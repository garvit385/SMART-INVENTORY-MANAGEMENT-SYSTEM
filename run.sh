#!/usr/bin/env sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
if ! command -v java >/dev/null 2>&1; then
  echo "Java 17 or later is required. Install a desktop JDK and try again." >&2
  exit 1
fi
if [ ! -f target/SmartStock.jar ]; then
  if ! command -v mvn >/dev/null 2>&1; then
    echo "Install Maven 3.8+ to build this source project, or download the runnable JAR." >&2
    exit 1
  fi
  mvn -B clean package
fi
exec java -jar target/SmartStock.jar "$@"
