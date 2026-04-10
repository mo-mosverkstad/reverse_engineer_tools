#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$SCRIPT_DIR/out"

mkdir -p "$OUT_DIR"
find "$SCRIPT_DIR/src/main/java" -name '*.java' | xargs javac -d "$OUT_DIR"
java -cp "$OUT_DIR" uml4java.Uml4Java "$@"
