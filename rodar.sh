#!/bin/bash
# Calculadora de Link Budget GPON
# Script para Linux / Mac — execute com: ./rodar.sh

echo "============================================"
echo "  Calculadora de Link Budget GPON"
echo "  Projeto Interdisciplinar"
echo "============================================"
echo ""

cd "$(dirname "$0")"

echo "Compilando o projeto..."

mkdir -p out
javac -d out -cp out $(find src/main/java -name "*.java")

if [ $? -ne 0 ]; then
    echo ""
    echo "[ERRO] Falha na compilacao. Verifique se o JDK esta instalado."
    echo "       Ubuntu/Debian: sudo apt install default-jdk"
    echo "       Mac: brew install openjdk"
    echo "       Ou baixe em: https://adoptium.net/"
    exit 1
fi

echo "Compilacao OK! Abrindo a calculadora..."
echo ""

java -cp out ui.CalculadoraGUI
