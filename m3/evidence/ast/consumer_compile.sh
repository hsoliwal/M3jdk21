#!/bin/bash
# oj.sh <tree> <deps> <out> : compile m3/eclipse/optional-jmod main sources (the jdk.m3.ast consumer) against the tree's
# m3/ast and the current core, arrays, collections and file sources (source path, -implicit:none); errors per file.
T=$1; D=$2; O=$3; rm -rf "$O"; mkdir -p "$O"
javac --source 21 --target 21 -proc:none -implicit:none \
  -sourcepath "$T/m3/ast/src/main/java:$D/m3/core/src:$D/src/jdk.m3.collections/share/classes:$D/m3/collections/src/main/java:$D/m3/file/src/main/java" \
  -d "$O" $(find "$T/m3/eclipse/optional-jmod/src/main/java" -name '*.java' ! -name module-info.java) > "$O.log" 2>&1
echo "optional-jmod javac rc=$? errors=$(grep -c 'error:' "$O.log") classes=$(find "$O" -name '*.class' | wc -l)"
grep 'error:' "$O.log" | sed -E 's#^.*/(jdk/[^:]+):[0-9]+: #\1: #' | sort | uniq -c | head -5
