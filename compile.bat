@echo off
setlocal enabledelayedexpansion

REM =====================================
REM Variables pour les deux projets
REM =====================================
set "FRAMEWORK_PATH=."
set "SRC=%FRAMEWORK_PATH%\src"
set "BIN=%FRAMEWORK_PATH%\build"
set "LIB=D:\tomcat10\apache-tomcat-10.1.28\lib"
set "FRAMEWORK_JAR=%FRAMEWORK_PATH%\Framework.jar"
set "jakarta_JAR=%LIB%\servlet-api.jar"
set "TESTAPP_LIB=D:\ITU\Semestre 5\Techno-reseaux\Fram\TesteFramework\lib"

REM =====================================
REM COMPILATION DU PREMIER PROJET (compile.bat)
REM =====================================
echo.
echo =====================================
echo COMPILATION PROJET 1 (Framework original)
echo =====================================

set BUILD=build
if exist %BUILD% rmdir /S /Q %BUILD%
mkdir %BUILD%

set LIB_JAR=lib\servlet-api.jar

echo Compilation projet 1...
javac -parameters -cp %LIB_JAR% -d %BUILD% ^
src\core\Route.java ^
src\core\Router.java ^
src\core\FrontServlet.java ^
src\core\MyAnnotation.java ^
src\core\model\ModelView.java ^
src\core\annotations\Controller.java ^
src\core\annotations\MyURL.java ^
src\core\annotations\GetMapping.java ^
src\core\annotations\PostMapping.java ^
src\core\utils\MethodRoute.java ^
src\core\utils\AnnotationScanner.java ^
src\core\utils\ParametersHandler.java ^
src\core\annotations\Param.java ^
src\core\annotations\Json.java ^
src\core\annotations\FileUpload.java ^
src\core\model\UploadedFile.java ^
src\core\utils\UrlPattern.java
if %ERRORLEVEL% neq 0 (
    echo ❌ Erreur compilation projet 1
    pause
    exit /b
)

echo Creation JAR projet 1...
cd %BUILD%
jar cf ..\Framework.jar *
cd ..

echo ✅ Projet 1 compile avec succes!

REM =====================================
REM COMPILATION DU DEUXIEME PROJET (script.bat original)
REM =====================================
echo.
echo =====================================
echo COMPILATION PROJET 2 (Nouveau Framework)
echo =====================================

REM Nettoyer ancien bin
echo Nettoyage du dossier build...
if exist "%BIN%" rmdir /s /q "%BIN%"
mkdir "%BIN%"

REM Compilation dans le BON ORDRE avec dépendances
echo Compilation du modele...
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\model\ModelView.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\model\UploadedFile.java"

echo Compilation des annotations...
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\MyURL.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\Controller.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\GetMapping.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\Param.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\PostMapping.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\Json.java"
javac -cp "%jakarta_JAR%" -d "%BIN%" "%SRC%\core\annotations\FileUpload.java"


echo Compilation des classes core de base...
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\Route.java"
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\Router.java"
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\MyAnnotation.java"

echo Compilation des utilitaires...
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\utils\AnnotationScanner.java"
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\utils\MethodRoute.java"
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\utils\ParametersHandler.java"
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\utils\UrlPattern.java"
echo Compilation du servlet...
javac -cp "%jakarta_JAR%;%BIN%" -d "%BIN%" "%SRC%\core\FrontServlet.java"

REM Vérifier la compilation
if errorlevel 1 (
  echo ❌ Erreur de compilation projet 2!
  pause
  exit /b 1
)

echo ✅ Toutes les classes du projet 2 compilees avec succes!

REM =====================================
REM Créer le JAR du projet 2
REM =====================================
if exist "%FRAMEWORK_JAR%" del /q "%FRAMEWORK_JAR%"
echo Creation du JAR projet 2...
jar cvf "%FRAMEWORK_JAR%" -C "%BIN%" .

REM =====================================
REM Vérifier le contenu des JARs
REM =====================================
echo Contenu du JAR projet 1:
jar tf Framework.jar

echo.
echo Contenu du JAR projet 2:
jar tf "%FRAMEWORK_JAR%"

REM =====================================
REM Copier le JAR dans testApp/lib
REM =====================================
if not exist "%TESTAPP_LIB%" mkdir "%TESTAPP_LIB%"
copy /y "%FRAMEWORK_JAR%" "%TESTAPP_LIB%\"

echo.
echo ✅ Les deux projets compiles avec succes!
echo ✅ JARs generes et copies!
echo.

pause