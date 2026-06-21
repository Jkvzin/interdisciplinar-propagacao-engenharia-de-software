@echo off
title Calculadora de Link Budget GPON
echo ============================================
echo   Calculadora de Link Budget GPON
echo   Projeto Interdisciplinar
echo ============================================
echo.
echo Compilando o projeto...
echo.

cd /d "%~dp0"

if not exist out mkdir out

REM Compila todos os arquivos Java
dir /s /b src\main\java\*.java > sources.txt
javac -d out -cp out @sources.txt 2>NUL
del sources.txt

if %errorlevel% neq 0 (
    echo.
    echo [ERRO] Falha na compilacao. Verifique se o Java esta instalado.
    echo         Instale o JDK em: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo Compilacao OK! Abrindo a calculadora...
echo.

REM Abre a interface grafica
start javaw -cp out ui.CalculadoraGUI

echo.
echo A calculadora foi aberta em uma janela separada.
echo Se a janela nao apareceu, olhe na barra de tarefas.
echo.
pause
