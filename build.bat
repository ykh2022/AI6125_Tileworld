@echo off
set "JDK_DIR=C:\Program Files (x86)\Java\jdk-1.8"
set "JAVAC=%JDK_DIR%\bin\javac.exe"

echo Finding Java source files...
dir /s /B src\*.java > sources.txt

if not exist bin mkdir bin

echo Compiling Tileworld_rl...
"%JAVAC%" -d bin -cp "..\MASON_14.jar" @sources.txt

if %errorlevel% neq 0 (
    echo Compilation failed!
) else (
    echo Compilation successful!
)

del sources.txt
pause
