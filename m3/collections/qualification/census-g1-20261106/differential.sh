#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential of qualification/census-g1-20261106: donor M3FilteredMaps (with the donor M3CollectionViews postimage it
# composes through, and the donor M3FilteredLists for the donor list proof) vs the relocated jdk.m3.collections.M3FilteredMaps (module target/classes, after mvn package),
# driven by differential/M3FilteredMapsDump.java.tmpl over seeded ops; plus the two relocated proofs on both sides.
# usage: differential.sh <donor git dir> <donor rev> <out dir> [ops-per-owner-kind] [seed]
# env:   JAVA_HOME = JDK 21. Donor paths are read from RELOCATION_MAP.tsv (FILE rows) and the donor SKIP row of
#        M3CollectionViews; nothing is checked out.
set -euo pipefail
DONOR=$1; REV=$2; OUT=$3; OPS=${4:-200000}; SEED=${5:-20261106}
HERE=$(cd "$(dirname "$0")" && (pwd -W 2>/dev/null || pwd))
MODULE=$(cd "$HERE/../.." && (pwd -W 2>/dev/null || pwd))
MAP="$HERE/RELOCATION_MAP.tsv"
J="${JAVA_HOME:?JAVA_HOME}/bin"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';' ;; *) SEP=':' ;; esac
rm -rf "$OUT"; mkdir -p "$OUT/donor-src" "$OUT/donor" "$OUT/relocated"
donor_path() { awk -F'\t' -v t="$1" '($1=="FILE" || $1=="SKIP") && index($2, "/" t ".java.txt") {print $2; exit}' "$MAP"; }
DONOR_PKG=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
for type in M3FilteredMaps M3CollectionViews M3FilteredLists M3DeepFilterCompositionProof M3DeepFilteredListsProof; do
  git -C "$DONOR" cat-file blob "$REV:$(donor_path $type)" > "$OUT/donor-src/$type.java"
done
"$J/javac" --release 21 -Xlint:all -Werror -proc:none -d "$OUT/donor" "$OUT"/donor-src/*.java
sed "s/@PKG@/$DONOR_PKG/" "$HERE/differential/M3FilteredMapsDump.java.tmpl" > "$OUT/donor-src/M3FilteredMapsDump.java"
mkdir -p "$OUT/relocated-src"
sed "s/@PKG@/jdk.m3.collections/" "$HERE/differential/M3FilteredMapsDump.java.tmpl" > "$OUT/relocated-src/M3FilteredMapsDump.java"
"$J/javac" --release 21 -Xlint:all -Werror -proc:none -d "$OUT/donor" -cp "$OUT/donor" "$OUT/donor-src/M3FilteredMapsDump.java"
"$J/javac" --release 21 -Xlint:all -Werror -proc:none -d "$OUT/relocated" -cp "$MODULE/target/classes" "$OUT/relocated-src/M3FilteredMapsDump.java"
"$J/java" -cp "$OUT/donor" M3FilteredMapsDump "$OPS" "$SEED" > "$OUT/donor.dump"
"$J/java" -cp "$OUT/relocated$SEP$MODULE/target/classes" M3FilteredMapsDump "$OPS" "$SEED" > "$OUT/relocated.dump"
for proof in M3DeepFilterCompositionProof M3DeepFilteredListsProof; do
  "$J/java" -Xss1m -cp "$OUT/donor" "$DONOR_PKG.$proof" > "$OUT/donor-$proof.out"
  "$J/java" -Xss1m -cp "$MODULE/target/classes$SEP$MODULE/target/test-classes" "jdk.m3.collections.$proof" > "$OUT/relocated-$proof.out"
done
cat "$OUT/donor.dump" "$OUT/relocated.dump"
for f in .dump -M3DeepFilterCompositionProof.out -M3DeepFilteredListsProof.out; do
  d=$(sha256sum < "$OUT/donor$f"); r=$(sha256sum < "$OUT/relocated$f")
  echo "${f#[-.]} donor=${d%% *} relocated=${r%% *} $([ "$d" = "$r" ] && echo IDENTICAL || echo DIFFERENT)"
done
if ! grep -q " mismatches=0 " "$OUT/donor.dump" || ! grep -q " mismatches=0 " "$OUT/relocated.dump" \n    || ! cmp -s "$OUT/donor.dump" "$OUT/relocated.dump"; then echo "DIFFERENTIAL FAIL"; exit 1; fi
echo "DIFFERENTIAL PASS ops=$((OPS * 3)) seed=$SEED"
