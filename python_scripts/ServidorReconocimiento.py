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

app = Flask(__name__)

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


def verificar_rostro(ruta_imagen_capturada, ruta_foto_referencia):
    try:
        analisis = DeepFace.extract_faces(
            img_path=ruta_imagen_capturada,
            anti_spoofing=True,
            enforce_detection=True
        )
        if len(analisis) == 0:
            return {"success": False, "verified": False, "similarity": 0.0,
                    "error": "No se detectó ningún rostro"}

        es_real = analisis[0]["is_real"]
        if not es_real:
            return {"success": True, "verified": False, "similarity": 0.0,
                    "error": "Spoofing detectado"}

    except Exception as e:
        return {"success": False, "verified": False, "similarity": 0.0,
                "error": f"Error en validación facial: {str(e)}"}

    try:
        if not os.path.exists(ruta_foto_referencia):
            return {"success": False, "verified": False, "similarity": 0.0,
                    "error": "Foto de referencia no existe"}

        resultado = DeepFace.verify(
            img1_path=ruta_imagen_capturada,
            img2_path=ruta_foto_referencia,
            model_name="ArcFace",
            enforce_detection=False
        )

        distancia = resultado["distance"]
        umbral = resultado["threshold"]
        similarity = round((1 - distancia / umbral) * 100, 2)
        similarity = max(0.0, min(100.0, similarity))

        return {
            "success": True,
            "verified": resultado["verified"],
            "similarity": similarity
        }

    except Exception as e:
        return {"success": False, "verified": False, "similarity": 0.0,
                "error": str(e)}


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


@app.route("/filtro", methods=["POST"])
def filtro():
    data = request.get_json()
    directorio = data.get("directorio")
    if not directorio:
        return jsonify({"success": False, "error": "Falta el campo directorio"})
    resultado = seleccionar_mejor_foto(directorio)
    return jsonify(resultado)


@app.route("/verificar", methods=["POST"])
def verificar():
    data = request.get_json()
    ruta_referencia = data.get("rutaReferencia")
    imagen_base64 = data.get("imagenBase64")
    id_user = data.get("idUser")

    if not ruta_referencia or not imagen_base64 or not id_user:
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": "Faltan campos requeridos"})

    temp_path = None
    try:
        image_bytes = base64.b64decode(imagen_base64)
        BASE_DIR = os.path.dirname(os.path.abspath(__file__))
        temp_dir = os.path.join(BASE_DIR, "..", "temp")

        os.makedirs(temp_dir, exist_ok=True)
        temp_filename = f"srv_{id_user}_{uuid.uuid4().hex[:8]}.jpg"
        temp_path = os.path.join(temp_dir, temp_filename)

        with open(temp_path, "wb") as f:
            f.write(image_bytes)

        resultado = verificar_rostro(temp_path, ruta_referencia)
        return jsonify(resultado)

    except Exception as e:
        return jsonify({"success": False, "verified": False, "similarity": 0.0,
                        "error": str(e)})
    finally:
        if temp_path and os.path.exists(temp_path):
            os.remove(temp_path)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=False)