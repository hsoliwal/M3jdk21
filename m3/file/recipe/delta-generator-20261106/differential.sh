#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor delta generator (postimage), its delta type, hash type and store (the DONOR_DEP
# rows) and its 48 MiB probe, compiled from the donor git tree with javac --release 21 -Xlint:all -Werror, against the
# relocated classes (target/classes and target/test-classes of m3/file) over the same inputs, plus the develop
# main-path PREIMAGE (quadratic int matrices) as a third lane.
# (a) M3ChunkDeltaDump (differential/, compiled into scratch, reflection only) drives each generator over the proof's
#     16,129 exhaustive binary pairs and 300 seeded pairs, 100,000 seeded file-row pairs, 1,000 equal-root pairs,
#     1,000 generateWithSizes cases, the proof's exception cases and the 5000 x 5000 probe, printing every script,
#     distance, stats, apply() reconstruction and exception class with hashes as hex: donor, preimage and relocated
#     dumps must be byte-identical.
# (b) The 48 MiB probes: the relocated M3ChunkDeltaWorkspaceProbe and the donor DeltaWorkspaceProbe pass script and
#     distance under -Xmx48m; the same donor probe over the preimage must fail with OutOfMemoryError (the donor
#     README's negative control).
# (c) javap -public of the donor generator and delta type (and the delta's nested types), names mapped by sed from
#     the TYPE rows (hash type -> java.lang.String, store -> java.util.Map<java.lang.String, byte[]>), CR stripped
#     on both sides, equals the relocated classes'.
# The donor's lombok @Slf4j annotation and import are removed by sed before compiling (the DROP rows of the map; no
# donor file references the generated logger, which this script checks), so neither lombok nor slf4j is needed.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of every side. Donor package and type names come from
# RELOCATION_MAP.tsv next to this script, so no file in the naming scope names a donor package literally.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>   (JAVA_HOME = JDK 21; run after mvn package or
#        after javac of the module into target/classes and target/test-classes)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
mkdir -p "$3/src" "$3/pre-src" "$3/classes" "$3/pre-classes" "$3/probe-classes" "$3/dump-classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
hash_package=${donor_package%.*}
owner_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /Generator\.java$/ {print $2; exit}' "$MAP")
delta_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /M3ChunkDelta\.java$/ {print $2; exit}' "$MAP")
probe_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /Probe\.java$/ {print $2; exit}' "$MAP")
preimage_path=$(awk -F'\t' '$1=="DONOR_DEP" && $2 ~ /DeltaGenerator\.java$/ {print $2; exit}' "$MAP")
mapfile -t dep_paths < <(awk -F'\t' '$1=="DONOR_DEP" && $2 !~ /DeltaGenerator\.java$/ {print $2}' "$MAP")
donor_owner=$(basename "$owner_path" .java.txt)
donor_delta=$(basename "$delta_path" .java)
donor_probe=$(basename "$probe_path" .java)
donor_hash=$(awk -F'\t' '$1=="TYPE" && $3=="String" {print $2; exit}' "$MAP")
donor_store=$(awk -F'\t' '$1=="TYPE" && $3 ~ /^Map</ {print $2; exit}' "$MAP")
target_owner=$(awk -F'\t' -v d="$donor_owner" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
target_delta=$(awk -F'\t' -v d="$donor_delta" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
target_probe=$(awk -F'\t' -v d="$donor_probe" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
git -C "$DONOR_GIT" show "$REV:$owner_path" > "$SCRATCH/src/$donor_owner.java"
git -C "$DONOR_GIT" show "$REV:$delta_path" > "$SCRATCH/src/$donor_delta.java"
git -C "$DONOR_GIT" show "$REV:$probe_path" > "$SCRATCH/src/$donor_probe.java"
git -C "$DONOR_GIT" show "$REV:$preimage_path" > "$SCRATCH/pre-src/$donor_owner.java"
for path in "${dep_paths[@]}"; do git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"; done
# The DROP rows of the map, applied to the donor side: lombok's unused logger (no donor file references `log.`).
sed -i -e '/^import lombok\.extern\.slf4j\.Slf4j;\r\?$/d' -e '/^@Slf4j\r\?$/d' "$SCRATCH"/src/*.java "$SCRATCH"/pre-src/*.java
if grep -nE '^[[:space:]]*log\.' "$SCRATCH"/src/*.java "$SCRATCH"/pre-src/*.java; then
    echo "DIFFERENTIAL FAIL: a donor file references the lombok logger"; exit 1
fi
deps=(); for path in "${dep_paths[@]}"; do deps+=("$SCRATCH/src/$(basename "$path")"); done
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" "${deps[@]}" "$SCRATCH/src/$donor_delta.java" "$SCRATCH/src/$donor_owner.java" "$SCRATCH/src/$donor_probe.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/pre-classes" "${deps[@]}" "$SCRATCH/src/$donor_delta.java" "$SCRATCH/pre-src/$donor_owner.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -cp "$SCRATCH/pre-classes" -d "$SCRATCH/probe-classes" "$SCRATCH/src/$donor_probe.java"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/dump-classes" "$RECIPE_DIR/differential/M3ChunkDeltaDump.java"
echo "DONOR $donor_commit postimage=$owner_path preimage=$preimage_path deps=${dep_paths[*]}"
MAIN="$MODULE/target/classes"
TEST="$MODULE/target/test-classes"
DUMP="delta.differential.M3ChunkDeltaDump"
"$JAVA_BIN/java" -cp "$SCRATCH/classes$SEP$SCRATCH/dump-classes" "$DUMP" "$donor_package.$donor_owner" "$hash_package.$donor_hash" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$SCRATCH/pre-classes$SEP$SCRATCH/dump-classes" "$DUMP" "$donor_package.$donor_owner" "$hash_package.$donor_hash" > "$SCRATCH/preimage-dump.txt"
"$JAVA_BIN/java" -cp "$MAIN$SEP$SCRATCH/dump-classes" "$DUMP" "$target_package.$target_owner" java.lang.String > "$SCRATCH/relocated-dump.txt"
status=PASS
compare() {
    local left_sha right_sha verdict
    left_sha=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right_sha=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1/$2 $verdict ${1}_sha256=$left_sha ${2}_sha256=$right_sha bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
}
compare donor-dump relocated-dump
compare preimage-dump relocated-dump
probe() {
    local label=$1 cp=$2 main=$3 mode=$4 expect=$5 text
    if text=$("$JAVA_BIN/java" -Xmx48m -cp "$cp" "$main" "$mode" 2>&1); then
        if [ "$expect" = PASS ] && grep -q "^PASS $mode 5000x5000" <<<"$text"; then echo "probe-$label-$mode PASS: $(head -n 1 <<<"$text")"
        else echo "probe-$label-$mode UNEXPECTED: $(head -c 200 <<<"$text")"; status=FAIL; fi
    elif [ "$expect" = OOM ] && grep -q "OutOfMemoryError" <<<"$text"; then echo "probe-$label-$mode OutOfMemoryError (expected negative control)"
    else echo "probe-$label-$mode FAILED: $(head -c 200 <<<"$text")"; status=FAIL; fi
}
for mode in script distance; do
    probe relocated "$MAIN$SEP$TEST" "$target_package.$target_probe" "$mode" PASS
    probe donor "$SCRATCH/classes" "$donor_package.$donor_probe" "$mode" PASS
    probe preimage "$SCRATCH/pre-classes$SEP$SCRATCH/probe-classes" "$donor_package.$donor_probe" "$mode" OOM
done
nested=("" '$Operation' '$Operation$Type' '$Keep' '$Insert' '$Delete' '$Stats')
donor_classes=("$donor_package.$donor_owner"); relocated_classes=("$target_package.$target_owner")
for suffix in "${nested[@]}"; do donor_classes+=("$donor_package.$donor_delta$suffix"); relocated_classes+=("$target_package.$target_delta$suffix"); done
"$JAVA_BIN/javap" -public -cp "$SCRATCH/classes" "${donor_classes[@]}" | tr -d '\r' \
    | sed -e "s/$donor_package\.$donor_store\b/java.util.Map<java.lang.String, byte[]>/g" \
          -e "s/$hash_package\.$donor_hash\b/java.lang.String/g" \
          -e "s/$donor_package\.$donor_delta\b/$target_package.$target_delta/g" \
          -e "s/$donor_package\.$donor_owner\b/$target_package.$target_owner/g" \
          -e "s/\b$donor_delta\b/$target_delta/g" -e "s/\b$donor_owner\b/$target_owner/g" > "$SCRATCH/donor-javap.txt"
"$JAVA_BIN/javap" -public -cp "$MAIN" "${relocated_classes[@]}" | tr -d '\r' > "$SCRATCH/relocated-javap.txt"
compare donor-javap relocated-javap
tail -n 1 "$SCRATCH/relocated-dump.txt"
grep '^P 5000x5000' "$SCRATCH/relocated-dump.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
