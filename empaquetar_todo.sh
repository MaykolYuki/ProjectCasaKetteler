#!/bin/bash

# Nombre del archivo de salida
OUTPUT_FILE="todo_el_codigo_proyecto.txt"

# Limpiar el archivo si ya existe
> "$OUTPUT_FILE"

echo "=== ESTRUCTURA SIMPLIFICADA DEL PROYECTO ===" >> "$OUTPUT_FILE"
if command -v tree &> /dev/null; then
    tree -I "target|.git|.idea|.settings|bin|venv_perfecto" >> "$OUTPUT_FILE"
else
    echo "Carpeta raíz: $(pwd)" >> "$OUTPUT_FILE"
fi
echo -e "\n=============================================\n" >> "$OUTPUT_FILE"

echo "--- BUSCANDO CÓDIGO JAVA Y CONFIGURACIONES ---"
# 1. Buscar archivos Java dentro de 'src' (evitamos XMLs gigantes de fuera)
find src -type f \( -name "*.java" -o -name "*.properties" -o -name "*.yml" -o -name "*.xml" \) \
! -path "*/target/*" 2>/dev/null | while read -r archivo; do

    echo "========================================" >> "$OUTPUT_FILE"
    echo "RUTA: $archivo" >> "$OUTPUT_FILE"
    echo "========================================" >> "$OUTPUT_FILE"
    cat "$archivo" >> "$OUTPUT_FILE"
    echo -e "\n\n" >> "$OUTPUT_FILE"
done

echo "--- BUSCANDO SCRIPTS DE PYTHON ---"
# 2. Buscar archivos .py en todo el proyecto, ignorando el entorno virtual
find . -type f -name "*.py" \
! -path "*/venv_perfecto/*" \
! -path "*/.git/*" | while read -r archivo; do

    echo "========================================" >> "$OUTPUT_FILE"
    echo "RUTA: $archivo" >> "$OUTPUT_FILE"
    echo "========================================" >> "$OUTPUT_FILE"
    cat "$archivo" >> "$OUTPUT_FILE"
    echo -e "\n\n" >> "$OUTPUT_FILE"
done

echo "¡Listo! Todo tu código Java y tus scripts de Python están en: $OUTPUT_FILE"
