#!/bin/bash
# Wrapper: master's jdk.internal.mindex siblings (minus the owner) seed the proof source dir,
# because the merged owner calls master's M3StateRelation.compose. Then run #1080 qualify.sh unchanged.
set -e
cd "$(dirname "$0")"
OUT=${1:?out}; TARGET=${2:-t_merged}
rm -rf "$OUT"; mkdir -p "$OUT/source/jdk/internal/mindex"
for f in ../gate/base/jdk/internal/mindex/*.java; do
  [ "$(basename $f)" = M3RegexDpPlan.java ] || cp "$f" "$OUT/source/jdk/internal/mindex/"
done
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
bash batch/qualify.sh --target "$TARGET" --output "$OUT"
