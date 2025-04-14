#!/bin/bash

ASPECTJ_RT="src/lib/aspectjrt-1.9.23.jar"
ASPECTJ_TOOLS="src/lib/aspectjtools-1.9.23.jar"

echo "Compiling with AspectJ..."
java -cp "$ASPECTJ_TOOLS" org.aspectj.tools.ajc.Main \
  -source 8 -target 8 \
  -classpath "bin;$ASPECTJ_RT" \
  -d bin \
  -sourceroots src

if [ $? -ne 0 ]; then
  echo "Compilation failed"
  exit 1
fi

echo "Running the app..."
java -cp "bin;$ASPECTJ_RT" neoe.ne.Main
