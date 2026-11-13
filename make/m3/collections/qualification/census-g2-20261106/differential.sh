#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differentials of qualification/census-g2-20261106 (run after mvn package of m3/collections):
#  1. before/after of the edited owners: the pinned preimages of M3CowSet and M3CowLongList (git blobs named in
#     EDIT_RULES.tsv, compiled separately) vs the module classes, same seeded ops (differential/M3CowDump.java.tmpl);
#     every differing line is classified: RECEIVED when the scaffold answered M3NotYetReceived, else UNCLASSIFIED.
#  2. donor/relocated: the donor postimages (donor package: RELOCATION_MAP.tsv PACKAGE row) vs the module classes: dumps byte-identical,
#     both compared with the JDK CopyOnWriteArraySet / CopyOnWriteArrayList<Long> model (0 mismatches), and the crate's
#     M3CowSetDifferential main on both sides (identical output).
# usage: differential.sh <donor git dir> <donor rev> <out dir> [ops] [seed]      env: JAVA_HOME = JDK 21
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3; OPS=${4:-200000}; SEED=${5:-20261106}
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
ROOT=$(cd "$HERE/../../../.." && (pwd -W 2>/dev/null || pwd))
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
JC=("$J/javac" --release 21 -Xlint:all,-module -Werror -proc:none)
rm -rf "$OUT"; mkdir -p "$OUT"/{before-src,before,donor-src,donor,after}
DONOR_PKG=$(awk -F'	' '$1=="PACKAGE" {print $2; exit}' "$HERE/RELOCATION_MAP.tsv")
rules() { python -I -c "import json,sys;[print(r[1],r[2],json.loads(r[4])['pre'],json.loads(r[4])['post']) for r in (l.split(chr(9)) for l in open(sys.argv[1],encoding='utf-8').read().splitlines()) if r[0]=='DONOR']" "$HERE/EDIT_RULES.tsv" | tr -d '\r'; }
while read -r path blob pre post; do
  git -C "$ROOT" cat-file blob "$blob" > "$OUT/before-src/$(basename "$path")"
  git -C "$DONOR" cat-file blob "$REV:$post" > "$OUT/donor-src/$(basename "$path")"
done < <(rules)
git -C "$DONOR" cat-file blob "$REV:$(awk -F'\t' '$1=="FILE" && $3 ~ /M3CowSetDifferential.java$/ {print $2}' "$HERE/RELOCATION_MAP.tsv")" > "$OUT/donor-src/M3CowSetDifferential.java"
"${JC[@]}" -d "$OUT/before" -cp "$MODULE/target/classes" "$OUT"/before-src/*.java
"${JC[@]}" -d "$OUT/donor" "$OUT"/donor-src/*.java
sed "s/@PKG@/jdk.m3.collections/" "$HERE/differential/M3CowDump.java.tmpl" > "$OUT/before-src/M3CowDump.java.txt"
mkdir -p "$OUT/driver-m3" "$OUT/driver-donor"
cp "$OUT/before-src/M3CowDump.java.txt" "$OUT/driver-m3/M3CowDump.java"
sed "s/@PKG@/$DONOR_PKG/" "$HERE/differential/M3CowDump.java.tmpl" > "$OUT/driver-donor/M3CowDump.java"
"${JC[@]}" -d "$OUT/after" -cp "$MODULE/target/classes" "$OUT/driver-m3/M3CowDump.java"
"${JC[@]}" -d "$OUT/donor" -cp "$OUT/donor" "$OUT/driver-donor/M3CowDump.java"
"$J/java" -cp "$OUT/after$SEP$OUT/before$SEP$MODULE/target/classes" M3CowDump "$OPS" "$SEED" > "$OUT/before.dump" 2> "$OUT/before.err"
"$J/java" -cp "$OUT/after$SEP$MODULE/target/classes" M3CowDump "$OPS" "$SEED" model > "$OUT/after.dump" 2> "$OUT/after.err"
"$J/java" -cp "$OUT/donor" M3CowDump "$OPS" "$SEED" model > "$OUT/donor.dump" 2> "$OUT/donor.err"
"$J/java" -cp "$OUT/donor" "$DONOR_PKG.M3CowSetDifferential" > "$OUT/donor-M3CowSetDifferential.out"
"$J/java" -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" jdk.m3.collections.M3CowSetDifferential > "$OUT/relocated-M3CowSetDifferential.out"
python -I - "$OUT/before.dump" "$OUT/after.dump" > "$OUT/classify.txt" <<'PY'
import sys
before = open(sys.argv[1], encoding="utf-8").read().splitlines()
after = open(sys.argv[2], encoding="utf-8").read().splitlines()
assert len(before) == len(after), (len(before), len(after))
same = received = unclassified = 0
for b, a in zip(before, after):
    tb, rb = b.split(" -> ", 1); ta, ra = a.split(" -> ", 1)
    assert tb == ta, (tb, ta)
    if rb == ra: same += 1
    elif rb == "EX:M3NotYetReceived": received += 1
    else:
        unclassified += 1
        print("UNCLASSIFIED", b, "|", a)
print("before_after lines=%d identical=%d received=%d unclassified=%d" % (len(after), same, received, unclassified))
PY
cat "$OUT/classify.txt" | tail -1; cat "$OUT/after.err" "$OUT/donor.err"
d=$(sha256sum < "$OUT/donor.dump"); a=$(sha256sum < "$OUT/after.dump")
echo "dump donor=${d%% *} relocated=${a%% *} $([ "$d" = "$a" ] && echo IDENTICAL || echo DIFFERENT)"
d2=$(sha256sum < "$OUT/donor-M3CowSetDifferential.out"); r2=$(sha256sum < "$OUT/relocated-M3CowSetDifferential.out")
echo "M3CowSetDifferential donor=${d2%% *} relocated=${r2%% *} $([ "$d2" = "$r2" ] && echo IDENTICAL || echo DIFFERENT): $(cat "$OUT/relocated-M3CowSetDifferential.out")"
if [ "$d" != "$a" ] || [ "$d2" != "$r2" ] || ! grep -q " unclassified=0$" "$OUT/classify.txt" \
    || ! grep -q " mismatches=0$" "$OUT/after.err" || ! grep -q " mismatches=0$" "$OUT/donor.err"; then
  echo "DIFFERENTIAL FAIL"; exit 1
fi
echo "DIFFERENTIAL PASS ops=$((OPS * 2)) seed=$SEED"
