#!/bin/bash
set -e
echo "Cleaning previous build..."
rm -rf out public
mkdir -p out
echo "Compiling Java source..."
javac -d out src/Main.java
echo "Copying static files..."
mkdir -p public
cp src/*.html src/*.css src/*.js public/
echo "Build complete."