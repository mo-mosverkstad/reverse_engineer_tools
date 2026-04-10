#!/bin/bash
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUT_DIR="$SCRIPT_DIR/out"

mkdir -p "$OUT_DIR"
javac -d "$OUT_DIR" "$SCRIPT_DIR"/src/main/java/uml4java/*.java
java -cp "$OUT_DIR" uml4java.Uml4Java "$@"
