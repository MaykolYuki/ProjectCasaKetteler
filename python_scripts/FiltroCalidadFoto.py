# -----------------------------------------------------------------
# SILENCIAR ADVERTENCIAS DE TENSORFLOW (DEBE SER LO PRIMERO)
# -----------------------------------------------------------------
import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_USE_LEGACY_KERAS"] = "1"
# -----------------------------------------------------------------

import cv2
from deepface import DeepFace
import json
import sys

def seleccionar_mejor_foto(directorio_fotos):
    """
    Analiza todas las fotos de un directorio y devuelve el nombre de la que 
    tiene el rostro más nítido y real.
    """
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
            # Sin anti-spoofing para fotos de referencia
            analisis = DeepFace.extract_faces(
                img_path=ruta_imagen,
                anti_spoofing=False,  # <- CAMBIAR
                enforce_detection=True
            )
            
            if len(analisis) == 0:
                continue

            # Quitar la verificación de is_real
            # if not analisis[0].get("is_real", True):
            #     continue

            # Medir nitidez
            img = cv2.imread(ruta_imagen)
            gris = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
            nitidez = cv2.Laplacian(gris, cv2.CV_64F).var()

            if nitidez > mejor_puntaje_nitidez:
                mejor_puntaje_nitidez = nitidez
                mejor_archivo = archivo

        except Exception:
            continue

    # Si terminamos el ciclo y no hay un mejor archivo, ninguna foto servía
    if mejor_archivo is None:
        return {
            "success": False,
            "error": "Ninguna de las fotos contiene un rostro humano válido o real"
        }

    # Retornamos el ganador
    return {
        "success": True,
        "best_image": mejor_archivo,
        "sharpness_score": round(mejor_puntaje_nitidez, 2)
    }

# -----------------------------------
# PUNTO DE ENTRADA
# -----------------------------------
if __name__ == "__main__":
    try:
        # Esperamos que Java nos envíe la ruta de la carpeta donde están las fotos a evaluar
        if len(sys.argv) < 2:
            print(json.dumps({
                "success": False, 
                "error": "Uso: python FiltroCalidadFoto.py <directorio_con_fotos>"
            }))
            sys.exit(1)

        directorio_evaluacion = sys.argv[1]
        resultado = seleccionar_mejor_foto(directorio_evaluacion)
        
        # Imprimir en formato JSON para que Java lo lea fácilmente
        print(json.dumps(resultado))

    except Exception as e:
        print(json.dumps({
            "success": False, 
            "error": str(e)
        }))
        sys.exit(1)