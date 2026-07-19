import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_USE_LEGACY_KERAS"] = "1"

import tf_keras as keras
import cv2
from deepface import DeepFace
from flask import Flask, request, jsonify
import base64
import uuid
import json
import sys
import re
import statistics

app = Flask(__name__)

STORAGE_BASE = os.environ.get("STORAGE_PATH")
ID_PATTERN = re.compile(r'^[a-zA-Z0-9\-]+$')

def id_user_valido(id_user):
    return bool(id_user) and bool(ID_PATTERN.match(id_user))


def ruta_photo_usuario(id_user):
    if not STORAGE_BASE:
        raise ValueError("STORAGE_PATH no está configurado en el servidor")
    if not id_user_valido(id_user):
        raise ValueError("idUser inválido")
    return os.path.realpath(os.path.join(STORAGE_BASE, "Photo", id_user))


def es_ruta_segura(ruta, base_permitida):
    ruta_real = os.path.realpath(ruta)
    base_real = os.path.realpath(base_permitida)
    return ruta_real == base_real or ruta_real.startswith(base_real + os.sep)

print("=== Cargando modelos de reconocimiento facial... ===")
try:
    DeepFace.build_model("ArcFace")
    print("=== Modelos cargados correctamente ===")
except Exception as e:
    print(f"=== Error cargando modelos: {e} ===")

def seleccionar_mejor_foto(directorio_fotos):
    if not os.path.exists(directorio_fotos):
        return {"success": False, "error": "El directorio de fotos no existe"}

    archivos = [
        f for f in os.listdir(directorio_fotos)
        if f.lower().endswith((".jpg", ".jpeg", ".png"))
    ]

    if len(archivos) == 0:
        return {"success": False, "error": "No hay imágenes para evaluar"}

    mejor_archivo = None
    mejor_puntaje_nitidez = -1.0

    for archivo in archivos:
        ruta_imagen = os.path.join(directorio_fotos, archivo)
        try:
            analisis = DeepFace.extract_faces(
                img_path=ruta_imagen,
                anti_spoofing=False,
                enforce_detection=True
            )
            if len(analisis) == 0:
                continue

            img = cv2.imread(ruta_imagen)
            gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            nitidez = cv2.Laplacian(gris, cv2.CV_64F).var()

            if nitidez > mejor_puntaje_nitidez:
                mejor_puntaje_nitidez = nitidez
                mejor_archivo = archivo

        except Exception:
            continue

    if mejor_archivo is None:
        return {"success": False, "error": "Ninguna foto contiene un rostro válido"}

    return {
        "success": True,
        "best_image": mejor_archivo,
        "sharpness_score": round(mejor_puntaje_nitidez, 2)
    }


def verificar_rostros_rafaga(rutas_capturas, ruta_foto_referencia):
    """Compara N capturas contra la referencia y decide por mediana robusta."""
    if not os.path.exists(ruta_foto_referencia):
        return {"success": False, "verified": False, "similarity": 0.0,
                "error": "Foto de referencia no existe"}

    similitudes = []
    distancias = []
    umbral_modelo = None
    algun_spoof = False
    rostros_validos = 0

    for ruta in rutas_capturas:
        try:
            # Anti-spoofing por cada frame
            analisis = DeepFace.extract_faces(
                img_path=ruta,
                detector_backend="retinaface",
                anti_spoofing=True,
                enforce_detection=True
            )
            if len(analisis) == 0:
                continue
            if not analisis[0]["is_real"]:
                algun_spoof = True
                continue

            resultado = DeepFace.verify(
                img1_path=ruta,
                img2_path=ruta_foto_referencia,
                model_name="ArcFace",
                detector_backend="retinaface",
                enforce_detection=True,
                align=True
            )

            distancia = resultado["distance"]
            umbral = resultado["threshold"]
            umbral_modelo = umbral
            sim = round((1 - distancia / umbral) * 100, 2)
            sim = max(0.0, min(100.0, sim))

            similitudes.append(sim)
            distancias.append(distancia)
            rostros_validos += 1

        except ValueError:
            # enforce_detection=True: no había rostro claro en este frame, se descarta
            continue
        except Exception:
            continue

    # Si ningún frame tuvo rostro válido
    if rostros_validos == 0:
        if algun_spoof:
            return {"success": True, "verified": False, "similarity": 0.0,
                    "error": "Se detectó una posible suplantación (foto de foto). Usa tu rostro real."}
        return {"success": True, "verified": False, "similarity": 0.0,
                "error": "No se detectó un rostro claro. Acércate y mira de frente a la cámara."}

    # Mediana robusta: un frame malo no arrastra la decisión
    similarity_final = round(statistics.median(similitudes), 2)
    distancia_mediana = round(statistics.median(distancias), 4)

    UMBRAL_ACEPTACION = 55.0  # calibrado con datos reales
    verificado = similarity_final >= UMBRAL_ACEPTACION

    return {
        "success": True,
        "verified": verificado,
        "similarity": similarity_final,
        "distance": distancia_mediana,
        "threshold": round(umbral_modelo, 4) if umbral_modelo else None,
        "framesEvaluados": rostros_validos
    }


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/filtro", methods=["POST"])
def filtro():
    data = request.get_json()
    directorio = data.get("directorio")
    id_user = data.get("idUser")

    if not directorio or not id_user:
        return jsonify({"success": False, "error": "Faltan campos requeridos"})

    try:
        carpeta_permitida = ruta_photo_usuario(id_user)
    except ValueError as e:
        return jsonify({"success": False, "error": str(e)})

    if not es_ruta_segura(directorio, carpeta_permitida):
        return jsonify({"success": False, "error": "Directorio no permitido"})

    resultado = seleccionar_mejor_foto(directorio)
    return jsonify(resultado)


@app.route("/verificar", methods=["POST"])
def verificar():
    data = request.get_json()
    ruta_referencia = data.get("rutaReferencia")
    imagenes_base64 = data.get("imagenesBase64")
    id_user = data.get("idUser")

    if not ruta_referencia or not imagenes_base64 or not isinstance(imagenes_base64, list) or not id_user:
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": "Se esperaba una lista de imágenes"})

    try:
        carpeta_permitida = ruta_photo_usuario(id_user)
    except ValueError as e:
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": str(e)})

    if not es_ruta_segura(ruta_referencia, carpeta_permitida):
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": "Ruta de referencia no permitida"})

    temp_paths = []
    try:
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))
        temp_dir = os.path.join(BASE_DIR, "..", "temp")
        os.makedirs(temp_dir, exist_ok=True)

        for img_b64 in imagenes_base64:
            image_bytes = base64.b64decode(img_b64)
            temp_filename = f"srv_{id_user}_{uuid.uuid4().hex[:8]}.jpg"
            temp_path = os.path.join(temp_dir, temp_filename)
            with open(temp_path, "wb") as f:
                f.write(image_bytes)
            temp_paths.append(temp_path)

        resultado = verificar_rostros_rafaga(temp_paths, ruta_referencia)
        return jsonify(resultado)

    except Exception as e:
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": str(e)})
    finally:
        for tp in temp_paths:
            if os.path.exists(tp):
                os.remove(tp)


if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=False, threaded=True, use_reloader=False)