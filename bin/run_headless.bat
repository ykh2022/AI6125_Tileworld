@echo off
set "JDK_DIR=C:\Program Files (x86)\Java\jdk-1.8"
set "JAVA=%JDK_DIR%\bin\java.exe"

echo Starting Tileworld_rl Headless...
"%JAVA%" -cp "bin;..\MASON_14.jar" tileworld.TileworldMain
pause
