from deepface import DeepFace
import os
import json
import sys


def verificar_usuario(ruta_imagen_capturada, directorio_usuario):
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

        # Recorremos fotos del usuario
        for archivo in imagenes:

            ruta_referencia = os.path.join(
                directorio_usuario,
                archivo
            )

            try:

                resultado = DeepFace.verify(
                    img1_path=ruta_imagen_capturada,
                    img2_path=ruta_referencia,
                    model_name="ArcFace",
                    enforce_detection=False
                )

                # Coincidencia encontrada
                if resultado["verified"]:
                    return {
                        "success": True,
                        "verified": True
                    }

            except Exception:
                continue

        # Ninguna coincidencia
        return {
            "success": True,
            "verified": False
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