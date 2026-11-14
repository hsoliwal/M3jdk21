#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# retained-port-20261107 (m3/file) donor-vs-restored differential of the 12 restored observer classes.
# usage: differential.sh <head-classes-dir> <out-dir> [episodes=3000] [seed=0x52455450] [java-home]
# donor side: the 12 parent blobs (parent_blob column) of RESTORE.tsv (the pre-rename donor package, read from the object store with git cat-file) +
#             differential/ShadowDifferential.java with __PKG__=<donor package>
# head side:  differential/ShadowDifferential.java with __PKG__=jdk.m3.file against <head-classes-dir>
# Both sides run under the default JIT and -Xint; the four outputs must be byte-identical (sha256).
set -euo pipefail
DONOR_PKG="com.m3"".file"; DONOR_DIR="com/m3""/file"
HEAD_CLASSES=$1; OUT=$2; EPISODES=${3:-3000}; SEED=${4:-$((0x52455450))}
JAVA_HOME_DIR=${5:-${JAVA_HOME:-"C:/Program Files/Java/jdk-21"}}
JAVAC="$JAVA_HOME_DIR/bin/javac"; JAVA="$JAVA_HOME_DIR/bin/java"
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../../../.." && pwd)
TMPD=${TMPDIR_OVERRIDE:-$OUT/tmp}
rm -rf "$OUT"; mkdir -p "$OUT/donor/src/$DONOR_DIR" "$OUT/donor/classes" "$OUT/head/src/jdk/m3/file" "$OUT/head/classes" "$TMPD"
LAYERS="M3FileShadow M3FileCharShadow M3FileUtf8Shadow M3FileLineShadow M3FileTokenShadow M3FileCrcShadow M3FileDigestShadow M3FileTextShadow M3FileWcShadow M3FileStructHash M3Grammar M3Paragraphs"
n=0
for name in $LAYERS; do
  blob=$(awk -F'\t' -v t="m3/file/src/main/java/jdk/m3/file/$name.java" '$1==t{print $6}' "$HERE/RESTORE.tsv")
  [ -n "$blob" ] || { echo "no RESTORE.tsv row for $name"; exit 2; }
  (cd "$ROOT" && git cat-file blob "$blob") > "$OUT/donor/src/$DONOR_DIR/$name.java"
  grep -q '^package com\.m3\.file;$' "$OUT/donor/src/$DONOR_DIR/$name.java" || { echo "donor blob $blob is not in the donor package"; exit 2; }
  n=$((n+1))
done
echo "donor sources: $n"
sed "s/__PKG__/$DONOR_PKG/" "$HERE/differential/ShadowDifferential.java" > "$OUT/donor/src/$DONOR_DIR/ShadowDifferential.java"
sed 's/__PKG__/jdk.m3.file/' "$HERE/differential/ShadowDifferential.java" > "$OUT/head/src/jdk/m3/file/ShadowDifferential.java"
"$JAVAC" -J-Djava.io.tmpdir="$TMPD" -proc:none --release 21 -Xlint:all -Werror -d "$OUT/donor/classes" "$OUT/donor/src/$DONOR_DIR"/*.java
"$JAVAC" -J-Djava.io.tmpdir="$TMPD" -proc:none --release 21 -Xlint:all -Werror -cp "$HEAD_CLASSES" -d "$OUT/head/classes" "$OUT/head/src/jdk/m3/file/ShadowDifferential.java"
run() { # side main classpath mode
  local side=$1 main=$2 cp=$3 mode=$4
  if [ "$mode" = xint ]; then flag=-Xint; else flag=-XX:+UseSerialGC; fi
  "$JAVA" $flag -Djava.io.tmpdir="$TMPD" -cp "$cp" "$main" "$SEED" "$EPISODES" "$OUT/$side-$mode.txt"
  echo "$side-$mode $(sha256sum "$OUT/$side-$mode.txt" | cut -c1-64) lines=$(wc -l < "$OUT/$side-$mode.txt")"
}
run donor "$DONOR_PKG.ShadowDifferential" "$OUT/donor/classes" jit | tee "$OUT/summary.txt"
run donor "$DONOR_PKG.ShadowDifferential" "$OUT/donor/classes" xint | tee -a "$OUT/summary.txt"
run head jdk.m3.file.ShadowDifferential "$OUT/head/classes;$HEAD_CLASSES" jit | tee -a "$OUT/summary.txt"
run head jdk.m3.file.ShadowDifferential "$OUT/head/classes;$HEAD_CLASSES" xint | tee -a "$OUT/summary.txt"
distinct=$(awk '{print $2}' "$OUT/summary.txt" | sort -u | wc -l)
echo "episodes=$EPISODES seed=$SEED distinct_sha256=$distinct finished=$(grep -c 'failure=none' "$OUT/head-jit.txt") refused=$(grep '^F ' "$OUT/head-jit.txt" | grep -vc 'failure=none')" | tee -a "$OUT/summary.txt"
if [ "$distinct" = 1 ]; then echo "DIFFERENTIAL PASS" | tee -a "$OUT/summary.txt"; else echo "DIFFERENTIAL FAIL" | tee -a "$OUT/summary.txt"; exit 1; fi
