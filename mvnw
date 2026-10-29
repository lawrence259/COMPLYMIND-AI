#!/bin/sh
DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -x "$DIR/tools/apache-maven-3.9.6/bin/mvn" ]; then
  exec "$DIR/tools/apache-maven-3.9.6/bin/mvn" "$@"
elif command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
else
  echo "Maven not found. Please install Maven or check tools directory."
  exit 1
fi
