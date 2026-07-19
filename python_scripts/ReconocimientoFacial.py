import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_USE_LEGACY_KERAS"] = "1"

import tf_keras as keras
import cv2
from deepface import DeepFace
import json
import sys


def verificar_usuario(ruta_imagen_capturada, ruta_foto_referencia):
    # FASE 1: ANTI-SPOOFING (igual que antes)
    try:
        analisis = DeepFace.extract_faces(
            img_path=ruta_imagen_capturada,
            anti_spoofing=True,
            enforce_detection=True
        )
        if len(analisis) == 0:
            return {"success": False, "verified": False, "error": "No se detectó ningún rostro"}
        
        es_real = analisis[0]["is_real"]
        if not es_real:
            return {"success": True, "verified": False, "error": "Spoofing detectado"}

    except Exception as e:
        return {"success": False, "verified": False, "error": f"Error en validación facial: {str(e)}"}

    # FASE 2: VERIFICACIÓN contra una sola foto
    try:
        if not os.path.exists(ruta_foto_referencia):
            return {"success": False, "verified": False, "error": "Foto de referencia no existe"}

        resultado = DeepFace.verify(
            img1_path=ruta_imagen_capturada,
            img2_path=ruta_foto_referencia,
            model_name="ArcFace",
            detector_backend="retinaface",
            enforce_detection=True,
            align=True
        )

        distancia = resultado["distance"]
        umbral = resultado["threshold"]
        similarity = round((1 - distancia / umbral) * 100, 2)
        similarity = max(0.0, min(100.0, similarity))

        if resultado["verified"]:
            return {"success": True, "verified": True, "similarity": similarity}
        else:
            return {"success": True, "verified": False, "similarity": similarity}

    except Exception as e:
        return {"success": False, "verified": False, "error": str(e)}


if __name__ == "__main__":
    try:
        if len(sys.argv) < 3:
            print(json.dumps({
                "success": False,
                "verified": False,
                "error": "Uso: python ReconocimientoFacial.py <imagen_capturada> <foto_referencia>"
            }))
            sys.exit(1)

        ruta_imagen = sys.argv[1]
        ruta_referencia = sys.argv[2]  # ahora es una foto, no un directorio
        resultado = verificar_usuario(ruta_imagen, ruta_referencia)
        print(json.dumps(resultado))

    except Exception as e:
        print(json.dumps({"success": False, "verified": False, "error": str(e)}))
        sys.exit(1)
