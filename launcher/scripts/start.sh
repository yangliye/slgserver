#!/bin/bash

SERVER_HOME="$(cd "$(dirname "$0")" && pwd)"
MAIN_CLASS="com.muyi.core.bootstrap.Bootstrap"

# JVM 参数（可根据实际情况调整）
JAVA_OPTS="-Xms512m -Xmx2g"
JAVA_OPTS="$JAVA_OPTS -XX:+UseZGC"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -XX:HeapDumpPath=${SERVER_HOME}/logs"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"

cd "$SERVER_HOME"

echo "========================================"
echo "  SLG Server"
echo "  Home: ${SERVER_HOME}"
echo "========================================"

exec java $JAVA_OPTS -cp "lib/*" $MAIN_CLASS --config=serverconfig/server.yaml "$@"
