#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# tree-lanes-port-20261107 (m3/file): retained-vs-ported differential of the five tree-lane leaves over a seeded tree corpus.
# usage: differential.sh <head-classes-dir> <out-dir> [episodes=200] [seed=0x54524545] [java-home]
# donor side: the retained PR #1619 module snapshot (every src/main/java file of
#             m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file, read from the object store by the
#             tree of the base commit recorded in RESTORE.tsv's '# retained_tree=' comment) compiled whole, the pre-adaptation leaves
#             among them; + differential/TreeLeafDifferential.java
# head side:  differential/TreeLeafDifferential.java against <head-classes-dir> (today's module with the ported leaves)
# Both sides run under the default JIT and -Xint; the four outputs must be byte-identical (sha256).
set -euo pipefail
HEAD_CLASSES=$1; OUT=$2; EPISODES=${3:-200}; SEED=${4:-$((0x54524545))}
JAVA_HOME_DIR=${5:-${JAVA_HOME:-"C:/Program Files/Java/jdk-21"}}
JAVAC="$JAVA_HOME_DIR/bin/javac"; JAVA="$JAVA_HOME_DIR/bin/java"
HERE=$(cd "$(dirname "$0")" && pwd)
ROOT=$(cd "$HERE/../../../.." && pwd)
TMPD=${TMPDIR_OVERRIDE:-$OUT/tmp}
RETAINED="m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java"
TREE=$(grep '^# retained_tree=' "$HERE/RESTORE.tsv" | head -1 | cut -d= -f2)
[ -n "$TREE" ] || { echo "RESTORE.tsv has no '# retained_tree=' comment"; exit 2; }
rm -rf "$OUT"; mkdir -p "$OUT/donor/src" "$OUT/donor/classes" "$OUT/head/src/jdk/m3/file" "$OUT/head/classes" "$TMPD"
(cd "$ROOT" && git archive "$TREE" "$RETAINED") | tar -x -C "$OUT/donor/src"
n=$(find "$OUT/donor/src" -name '*.java' | wc -l)
echo "donor sources: $n (retained PR #1619 module snapshot at $TREE)"
for name in M3FileFlag M3FileKind M3FileNames M3FileScanSpec M3FileWordCount; do
  blob=$(awk -F'\t' -v t="m3/file/src/main/java/jdk/m3/file/$name.java" '$1==t{print $4}' "$HERE/RESTORE.tsv")
  [ -n "$blob" ] || { echo "no RESTORE.tsv row for $name"; exit 2; }
  actual=$(cd "$ROOT" && git hash-object "$OUT/donor/src/$RETAINED/jdk/m3/file/$name.java")
  [ "$actual" = "$blob" ] || { echo "donor $name is $actual, RESTORE.tsv blob $blob"; exit 2; }
done
cp "$HERE/differential/TreeLeafDifferential.java" "$OUT/donor/src/$RETAINED/jdk/m3/file/"
cp "$HERE/differential/TreeLeafDifferential.java" "$OUT/head/src/jdk/m3/file/"
"$JAVAC" -J-Djava.io.tmpdir="$TMPD" -proc:none --release 21 -Xlint:all -Werror -d "$OUT/donor/classes" $(find "$OUT/donor/src" -name '*.java')
"$JAVAC" -J-Djava.io.tmpdir="$TMPD" -proc:none --release 21 -Xlint:all -Werror -cp "$HEAD_CLASSES" -d "$OUT/head/classes" "$OUT/head/src/jdk/m3/file/TreeLeafDifferential.java"
run() { # side classpath mode
  local side=$1 cp=$2 mode=$3
  if [ "$mode" = xint ]; then flag=-Xint; else flag=-XX:+UseSerialGC; fi
  "$JAVA" $flag -Djava.io.tmpdir="$TMPD" -cp "$cp" jdk.m3.file.TreeLeafDifferential "$SEED" "$EPISODES" "$OUT/$side-$mode.txt" "$TMPD/corpus-$side-$mode"
  echo "$side-$mode $(sha256sum "$OUT/$side-$mode.txt" | cut -c1-64) lines=$(wc -l < "$OUT/$side-$mode.txt")"
}
run donor "$OUT/donor/classes" jit | tee "$OUT/summary.txt"
run donor "$OUT/donor/classes" xint | tee -a "$OUT/summary.txt"
run head "$OUT/head/classes;$HEAD_CLASSES" jit | tee -a "$OUT/summary.txt"
run head "$OUT/head/classes;$HEAD_CLASSES" xint | tee -a "$OUT/summary.txt"
distinct=$(awk '{print $2}' "$OUT/summary.txt" | sort -u | wc -l)
echo "episodes=$EPISODES seed=$SEED distinct_sha256=$distinct rows=$(grep -c '^T' "$OUT/head-jit.txt") finished=$(grep -c 'failure=none' "$OUT/head-jit.txt") mismatches=$(grep -c MISMATCH "$OUT/head-jit.txt" || true)" | tee -a "$OUT/summary.txt"
if [ "$distinct" = 1 ]; then echo "DIFFERENTIAL PASS" | tee -a "$OUT/summary.txt"; else echo "DIFFERENTIAL FAIL" | tee -a "$OUT/summary.txt"; exit 1; fi
