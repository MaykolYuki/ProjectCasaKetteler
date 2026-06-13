# -----------------------------------------------------------------
# SILENCIAR ADVERTENCIAS DE TENSORFLOW (DEBE SER LO PRIMERO)
# -----------------------------------------------------------------
import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_USE_LEGACY_KERAS"] = "1"
# -----------------------------------------------------------------

import tf_keras as keras
import cv2
from deepface import DeepFace
import json
import sys


def verificar_usuario(ruta_imagen_capturada, directorio_usuario):
    # -----------------------------
    # FASE 1: ANTI-SPOOFING
    # -----------------------------
    try:
        analisis = DeepFace.extract_faces(
            img_path=ruta_imagen_capturada,
            anti_spoofing=True,
            enforce_detection=True
        )

        if len(analisis) == 0:
            return {
                "success": False,
                "verified": False,
                "error": "No se detectó ningún rostro"
            }

        es_real = analisis[0]["is_real"]

        if not es_real:
            return {
                "success": True,
                "verified": False,
                "error": "Spoofing detectado"
            }

    except Exception as e:
        return {
            "success": False,
            "verified": False,
            "error": f"Error en validación facial: {str(e)}"
        }

    # -----------------------------
    # FASE 2: VERIFICACIÓN
    # -----------------------------
    try:
        if not os.path.exists(directorio_usuario):
            return {
                "success": False,
                "verified": False,
                "error": "Directorio del usuario no existe"
            }

        archivos = os.listdir(directorio_usuario)

        imagenes = [
            archivo for archivo in archivos
            if archivo.lower().endswith((".jpg", ".jpeg", ".png"))
        ]

        if len(imagenes) == 0:
            return {
                "success": False,
                "verified": False,
                "error": "El usuario no tiene imágenes registradas"
            }

        mejor_similitud = 0.0

        for archivo in imagenes:
            ruta_referencia = os.path.join(directorio_usuario, archivo)
            try:
                resultado = DeepFace.verify(
                    img1_path=ruta_imagen_capturada,
                    img2_path=ruta_referencia,
                    model_name="ArcFace",
                    enforce_detection=False
                )

                distancia = resultado["distance"]
                umbral = resultado["threshold"]
                similarity = round((1 - distancia / umbral) * 100, 2)
                similarity = max(0.0, min(100.0, similarity))

                if similarity > mejor_similitud:
                    mejor_similitud = similarity

                if resultado["verified"]:
                    return {
                        "success": True,
                        "verified": True,
                        "similarity": similarity
                    }

            except Exception:
                continue

        return {
            "success": True,
            "verified": False,
            "similarity": mejor_similitud
        }

    except Exception as e:
        return {
            "success": False,
            "verified": False,
            "error": str(e)
        }


# -----------------------------------
# PUNTO DE ENTRADA
# -----------------------------------
if __name__ == "__main__":
    try:
        if len(sys.argv) < 3:
            print(json.dumps({
                "success": False,
                "verified": False,
                "error": (
                    "Uso correcto: "
                    "python ReconocimientoFacial.py "
                    "<imagen_capturada> "
                    "<directorio_usuario>"
                )
            }))
            sys.exit(1)

        ruta_imagen = sys.argv[1]
        directorio_usuario = sys.argv[2]

        resultado = verificar_usuario(
            ruta_imagen,
            directorio_usuario
        )

        print(json.dumps(resultado))

    except Exception as e:
        print(json.dumps({
            "success": False,
            "verified": False,
            "error": str(e)
        }))
        sys.exit(1)