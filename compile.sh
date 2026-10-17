#!/bin/sh
# Compiles every file in src/ into out/. Errors name the exact file and line.
cd "$(dirname "$0")" && rm -rf out && mkdir out && javac -d out src/*.java && echo "Compiled OK"
