@echo off
setlocal enabledelayedexpansion

REM =====================================
REM Variables
REM =====================================
set "FRAMEWORK_PATH=..\Framework"
set "SRC=%FRAMEWORK_PATH%\src\main\java"
set "BIN=%FRAMEWORK_PATH%\build"
set "LIB=C:\tomcat11\apache-tomcat-11.0.7\lib"
set "FRAMEWORK_JAR=%FRAMEWORK_PATH%\framework.jar"
set "JAKARTA_JAR=%LIB%\servlet-api.jar" 
set "TESTAPP_LIB=E:\HP\Documents\S5\Mr Naina\Test_framework\src\main\webapp\WEB-INF\lib"

REM =====================================
REM Nettoyer ancien bin
REM =====================================
echo Nettoyage du dossier build...
if exist "%BIN%" rmdir /s /q "%BIN%"
mkdir "%BIN%"

REM =====================================
REM Compilation manuelle dans le bon ordre
REM =====================================
echo Compilation des annotations...
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\Url.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\Controller.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\Param.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\HttpMethod.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\RequestMapping.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\GetMapping.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\PostMapping.java"
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\annotation\Json.java"

echo Compilation du modele...
javac -cp "%JAKARTA_JAR%" -d "%BIN%" "%SRC%\com\framework\model\ModelView.java"

echo Compilation des utilitaires...
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\MethodRoute.java"
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\RouteEntry.java"
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\ObjectBinder.java"
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\JsonSerializer.java"
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\ParameterResolver.java" "%SRC%\com\framework\util\RouteMatcher.java"
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\util\ControllerScanner.java"

echo Compilation du servlet...
javac -cp "%JAKARTA_JAR%;%BIN%" -d "%BIN%" "%SRC%\com\framework\servlet\FrontServlet.java"

REM =====================================
REM Vérifier la compilation
REM =====================================
if errorlevel 1 (
  echo Erreur de compilation!
  pause
  exit /b 1
)

echo ✅ Toutes les classes compilees avec succes!

REM =====================================
REM Vérifier que toutes les classes sont compilées
REM =====================================
echo Verification des classes...
dir "%BIN%\com\framework" /s

REM =====================================
REM Créer le JAR
REM =====================================
if exist "%FRAMEWORK_JAR%" del /q "%FRAMEWORK_JAR%"
echo Creation du JAR...
jar cvf "%FRAMEWORK_JAR%" -C "%BIN%" .

REM =====================================
REM Vérifier le contenu du JAR
REM =====================================
echo Contenu du JAR:
jar tf "%FRAMEWORK_JAR%"

REM =====================================
REM Copier le JAR dans testApp/lib
REM =====================================
if not exist "%TESTAPP_LIB%" mkdir "%TESTAPP_LIB%"
copy /y "%FRAMEWORK_JAR%" "%TESTAPP_LIB%\"

echo.
echo ✅ framework.jar genere et copie avec succes!
echo.
