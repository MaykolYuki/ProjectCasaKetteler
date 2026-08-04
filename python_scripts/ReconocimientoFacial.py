import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_USE_LEGACY_KERAS"] = "1"

import tf_keras as keras
import cv2
from deepface import DeepFace
import json
import sys
import time


def verificar_usuario(ruta_imagen_capturada, ruta_foto_referencia, detector="retinaface"):
    # FASE 1: ANTI-SPOOFING
    try:
        t_spoof = time.time()
        analisis = DeepFace.extract_faces(
            img_path=ruta_imagen_capturada,
            detector_backend=detector,
            anti_spoofing=True,
            enforce_detection=True
        )
        spoof_seconds = round(time.time() - t_spoof, 2)
        print(f"[TIMING] anti-spoofing ({detector}): {spoof_seconds}s", file=sys.stderr, flush=True)

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

        t_verify = time.time()
        resultado = DeepFace.verify(
            img1_path=ruta_imagen_capturada,
            img2_path=ruta_foto_referencia,
            model_name="ArcFace",
            detector_backend=detector,
            enforce_detection=True,
            align=True
        )
        verify_seconds = round(time.time() - t_verify, 2)
        print(f"[TIMING] verify ({detector}): {verify_seconds}s", file=sys.stderr, flush=True)

        distancia = resultado["distance"]
        umbral = resultado["threshold"]
        similarity = round((1 - distancia / umbral) * 100, 2)
        similarity = max(0.0, min(100.0, similarity))

        return {
            "success": True,
            "verified": resultado["verified"],
            "similarity": similarity,
            "detector": detector,
            "spoof_seconds": spoof_seconds,
            "verify_seconds": verify_seconds
        }

    except Exception as e:
        return {"success": False, "verified": False, "error": str(e)}


if __name__ == "__main__":
    try:
        if len(sys.argv) < 3:
            print(json.dumps({
                "success": False,
                "verified": False,
                "error": "Uso: python ReconocimientoFacial.py <imagen_capturada> <foto_referencia> [detector]"
            }))
            sys.exit(1)

        ruta_imagen = sys.argv[1]
        ruta_referencia = sys.argv[2]  # ahora es una foto, no un directorio
        # Detector opcional: retinaface (default), ssd, mtcnn, opencv...
        detector = sys.argv[3] if len(sys.argv) > 3 else "retinaface"

        t_total = time.time()
        resultado = verificar_usuario(ruta_imagen, ruta_referencia, detector)
        print(f"[TIMING] TOTAL ({detector}): {round(time.time() - t_total, 2)}s", file=sys.stderr, flush=True)
        print(json.dumps(resultado))

    except Exception as e:
        print(json.dumps({"success": False, "verified": False, "error": str(e)}))
        sys.exit(1)
