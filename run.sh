#!/usr/bin/env bash
set -euo pipefail

# Fix para diálogos do PIN (evita crash com /snap/core20)
export XDG_RUNTIME_DIR="/run/user/$(id -u)"
export LD_PRELOAD="/lib/x86_64-linux-gnu/libpthread.so.0"
export LD_LIBRARY_PATH="/lib/x86_64-linux-gnu:/usr/lib/x86_64-linux-gnu"

# Garantir JNI libs do PTEID (mantém o que já tens no pom)
export JAVA_TOOL_OPTIONS="-Djava.library.path=/usr/local/lib:/usr/local/lib/pteid_jni"

# Executar JavaFX app
mvn -q -DskipTests javafx:run
