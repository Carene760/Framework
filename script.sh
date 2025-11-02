#!/bin/bash
# =====================================
# Variables
# =====================================
FRAMEWORK_PATH="../Framework"
SRC="$FRAMEWORK_PATH/src"
BIN="$FRAMEWORK_PATH/build"
LIB="/mnt/c/tomcat11/apache-tomcat-11.0.7/lib"
FRAMEWORK_JAR="$FRAMEWORK_PATH/framework.jar"
JAKARTA_JAR="$LIB/servlet-api.jar"
TESTAPP_LIB="/mnt/c/Users/user/Documents/S5/Mr Naina/Test_framework/WEB-INF/lib"

# =====================================
# Nettoyer ancien bin
# =====================================
rm -rf "$BIN"
mkdir -p "$BIN"

# =====================================
# Compiler les .java
# =====================================
find "$SRC" -name "*.java" > sources.txt
javac -classpath "$JAKARTA_JAR" -d "$BIN" @sources.txt
if [ $? -ne 0 ]; then
  echo "Erreur de compilation!"
  exit 1
fi

# =====================================
# Créer le JAR
# =====================================
rm -f "$FRAMEWORK_JAR"
jar cvf "$FRAMEWORK_JAR" -C "$BIN" .
if [ $? -ne 0 ]; then
  echo "Erreur lors de la création du JAR!"
  exit 1
fi

# =====================================
# Copier le JAR dans testApp/lib
# =====================================
mkdir -p "$TESTAPP_LIB"
cp -f "$FRAMEWORK_JAR" "$TESTAPP_LIB"
if [ $? -ne 0 ]; then
  echo "Erreur lors de la copie du JAR!"
  exit 1
fi

echo "framework.jar généré et copié dans $TESTAPP_LIB avec succès"
