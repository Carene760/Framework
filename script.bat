@echo off
setlocal enabledelayedexpansion

REM =====================================
REM Variables
REM =====================================
set "FRAMEWORK_PATH=..\Framework"
set "SRC=%FRAMEWORK_PATH%\src"
set "BIN=%FRAMEWORK_PATH%\build"
set "LIB=C:\tomcat11\apache-tomcat-11.0.7\lib"
set "FRAMEWORK_JAR=%FRAMEWORK_PATH%\framework.jar"
set "JAKARTA_JAR=%LIB%\servlet-api.jar"
set "TESTAPP_LIB=C:\Users\user\Documents\S5\Mr Naina\Test_framework\WEB-INF\lib"

REM =====================================
REM Nettoyer ancien bin
REM =====================================
if exist "%BIN%" rmdir /s /q "%BIN%"
mkdir "%BIN%"

REM =====================================
REM Compiler les .java - avec chemins relatifs
REM =====================================
REM Se déplacer dans le dossier src et lister les fichiers .java avec chemins relatifs
cd "%SRC%"
for /r %%i in (*.java) do (
    echo %%~fi >> ..\..\sources.tmp
)
cd ..\..

REM Convertir les chemins absolus en relatifs par rapport au dossier courant
set "CURRENT_DIR=%CD%"
(for /f "usebackq delims=" %%a in ("sources.tmp") do (
    set "abs_path=%%a"
    set "rel_path=!abs_path:%CURRENT_DIR%\=!"
    echo !rel_path!
)) > sources.txt

del sources.tmp 2>nul

javac -classpath "%JAKARTA_JAR%" -d "%BIN%" @"sources.txt"
if errorlevel 1 (
  echo Erreur de compilation!
  exit /b 1
)

REM =====================================
REM Créer le JAR
REM =====================================
if exist "%FRAMEWORK_JAR%" del /q "%FRAMEWORK_JAR%"
jar cvf "%FRAMEWORK_JAR%" -C "%BIN%" .
if errorlevel 1 (
  echo Erreur lors de la création du JAR!
  exit /b 1
)

REM =====================================
REM Copier le JAR dans testApp/lib
REM =====================================
if not exist "%TESTAPP_LIB%" mkdir "%TESTAPP_LIB%"
copy /y "%FRAMEWORK_JAR%" "%TESTAPP_LIB%\"
if errorlevel 1 (
  echo Erreur lors de la copie du JAR!
  exit /b 1
)

echo framework.jar généré et copié dans %TESTAPP_LIB% avec succès

endlocal