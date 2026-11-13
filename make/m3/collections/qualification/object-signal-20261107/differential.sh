#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe object-signal-20261107: the donor classes (relocate.py --donor-subset: verbatim main
# files except the declared both-mode rows of the projector factory, the oracle minus its declared rows, the two
# SUBSET_ONLY closure files; javac --release 21 -Xlint:all -Werror) against the relocated classes (target/classes and
# target/test-classes of m3/collections after mvn package).
# (a) oracle: the donor oracle main and the relocated one print byte-identical output.
# (b) dump: differential/gen_dump.py writes the dump driver for each side from one template; the donor dump and the
#     relocated dump, each under the default JIT and under -Xint, are four byte-identical files.
# (c) javap -p of every donor class (names mapped through the PACKAGE and TYPE rows) against its relocated class:
#     IDENTICAL, or DIFFERENT with the differing lines (the declared DROP / ADAPT rows only).
# Donor package and type names come from RELOCATION_MAP.tsv, so no file in the naming scope names a donor package.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [queries] [seed]   (JAVA_HOME = JDK 21)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
rm -rf "$3/donor-src" "$3/donor-classes" "$3/relocated-src" "$3/relocated-classes" "$3/javap"
mkdir -p "$3/donor-src" "$3/donor-classes" "$3/relocated-src" "$3/relocated-classes" "$3/javap"
SCRATCH=$(winpath "$3")
QUERIES=${4:-20000}
SEED=${5:-20261107}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA_OPTS="-Djava.io.tmpdir=${M3_TMPDIR:-$SCRATCH}"
PY=${PYTHON:-python}
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_oracle=$(awk -F'\t' '$1=="FILE" && $3 ~ /src\/test\/java\// {n=split($2, p, "/"); sub(/\.java$/, "", p[n]); print p[n]; exit}' "$MAP")
target_oracle=$(awk -F'\t' -v d="$donor_oracle" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP")
echo "DONOR $(git -C "$DONOR_GIT" rev-parse "$REV^{commit}") package=$donor_package oracle=$donor_oracle"

# donor side
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --donor-subset "$SCRATCH/donor-src" \
    > "$SCRATCH/subset.log"
grep -c "SUBSET_VERBATIM=TRUE" "$SCRATCH/subset.log" | sed 's/^/subset verbatim files: /'
grep "SUBSET_VERBATIM=FALSE" "$SCRATCH/subset.log" || true
"$PY" -I "$RECIPE_DIR/differential/gen_dump.py" "$MAP" "$RECIPE_DIR/differential/M3ObjectSignalDump.java.tmpl" donor \
    "$SCRATCH/donor-src" > /dev/null
find "$SCRATCH/donor-src" -name '*.java' | sort > "$SCRATCH/donor-sources.txt"
"$JAVA_BIN/javac" -J"$JAVA_OPTS" --release 21 -g -proc:none -Xlint:all -Werror -d "$SCRATCH/donor-classes" \
    @"$SCRATCH/donor-sources.txt"
echo "javac donor: $(wc -l < "$SCRATCH/donor-sources.txt") files"

# relocated side: the module's classes plus the generated dump driver
"$PY" -I "$RECIPE_DIR/differential/gen_dump.py" "$MAP" "$RECIPE_DIR/differential/M3ObjectSignalDump.java.tmpl" \
    relocated "$SCRATCH/relocated-src" > /dev/null
"$JAVA_BIN/javac" -J"$JAVA_OPTS" --release 21 -g -proc:none -Xlint:all -Werror -cp "$MODULE/target/classes" \
    -d "$SCRATCH/relocated-classes" "$SCRATCH/relocated-src/${target_package//.//}/M3ObjectSignalDump.java"
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes$SEP$SCRATCH/relocated-classes"

status=PASS
same() {
    local name=$1 left=$2 right=$3
    if cmp -s "$left" "$right"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$name $verdict sha256=$(sha256sum "$left" | cut -c1-16)/$(sha256sum "$right" | cut -c1-16) bytes=$(wc -c < "$left") lines=$(wc -l < "$left")"
}

# (a) the oracle
"$JAVA_BIN/java" "$JAVA_OPTS" -cp "$SCRATCH/donor-classes" "$donor_package.$donor_oracle" > "$SCRATCH/donor-oracle.txt"
"$JAVA_BIN/java" "$JAVA_OPTS" -cp "$RELOCATED_CP" "$target_package.$target_oracle" > "$SCRATCH/relocated-oracle.txt"
same oracle "$SCRATCH/donor-oracle.txt" "$SCRATCH/relocated-oracle.txt"
cat "$SCRATCH/relocated-oracle.txt"

# (b) the dump, default JIT and -Xint
for mode in jit xint; do
    flag=""; [ "$mode" = xint ] && flag="-Xint"
    "$JAVA_BIN/java" "$JAVA_OPTS" $flag -cp "$SCRATCH/donor-classes" "$donor_package.M3ObjectSignalDump" "$QUERIES" "$SEED" \
        > "$SCRATCH/donor-dump-$mode.txt"
    "$JAVA_BIN/java" "$JAVA_OPTS" $flag -cp "$RELOCATED_CP" "$target_package.M3ObjectSignalDump" "$QUERIES" "$SEED" \
        > "$SCRATCH/relocated-dump-$mode.txt"
    echo "heartbeat dump $mode done $(date +%H:%M:%S)"
done
same dump-jit "$SCRATCH/donor-dump-jit.txt" "$SCRATCH/relocated-dump-jit.txt"
same dump-xint "$SCRATCH/donor-dump-xint.txt" "$SCRATCH/relocated-dump-xint.txt"
same dump-jit-vs-xint "$SCRATCH/donor-dump-jit.txt" "$SCRATCH/relocated-dump-xint.txt"
tail -n 1 "$SCRATCH/relocated-dump-jit.txt"
echo "query lines: $(grep -c ' query ' "$SCRATCH/relocated-dump-jit.txt"), refusal lines: $(grep -c ' refuse ' "$SCRATCH/relocated-dump-jit.txt"), accepted refusals: $(grep -c ' refuse .* accepted$' "$SCRATCH/relocated-dump-jit.txt" || true)"

# (c) javap -p, donor names mapped through the PACKAGE and TYPE rows
sed_map="$SCRATCH/javap/names.sed"
: > "$sed_map"
awk -F'\t' '$1=="PACKAGE" {gsub(/\./, "\\.", $2); print "s/\\b" $2 "\\./" $3 "./g"}' "$MAP" >> "$sed_map"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$sed_map"
identical=0; different=0
for class_file in $(cd "$SCRATCH/donor-classes" && find . -name '*.class' | sort); do
    donor_class=${class_file#./}; donor_class=${donor_class%.class}; donor_class=${donor_class//\//.}
    case "$donor_class" in *M3ObjectSignalDump*|*SelfTest*) continue ;; esac
    [ "${donor_class%.*}" = "$donor_package" ] || continue
    target_class=$(echo "$donor_class" | sed -f "$sed_map")
    "$JAVA_BIN/javap" -J"$JAVA_OPTS" -p -cp "$SCRATCH/donor-classes" "$donor_class" | sed -f "$sed_map" \
        | grep -v '^Compiled from' > "$SCRATCH/javap/donor-${target_class##*.}.txt"
    "$JAVA_BIN/javap" -J"$JAVA_OPTS" -p -cp "$MODULE/target/classes" "$target_class" | grep -v '^Compiled from' \
        > "$SCRATCH/javap/relocated-${target_class##*.}.txt"
    if cmp -s "$SCRATCH/javap/donor-${target_class##*.}.txt" "$SCRATCH/javap/relocated-${target_class##*.}.txt"; then
        identical=$((identical + 1)); echo "javap IDENTICAL ${target_class##*.}"
    else
        different=$((different + 1)); echo "javap DIFFERENT ${target_class##*.}"
        diff "$SCRATCH/javap/donor-${target_class##*.}.txt" "$SCRATCH/javap/relocated-${target_class##*.}.txt" \
            | grep '^[<>]' | sed 's/^/    /' || true
    fi
done
echo "javap identical=$identical different=$different (DIFFERENT is admitted only for the declared DROP / ADAPT rows)"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
