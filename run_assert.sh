#!/bin/bash

echo "Compiling with javac (excluding AspectJ)..."
javac -d bin -sourcepath src $(find src -name "*.java" -not -path "src/neoe/aspect/*")

if [ $? -ne 0 ]; then
  echo "Compilation failed"
  exit 1
fi

echo "Running with assertions enabled..."
java -ea -cp bin neoe.ne.Main
