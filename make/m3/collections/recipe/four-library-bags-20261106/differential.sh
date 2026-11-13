#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of the relocation: the donor classes (verbatim donor blobs, written by relocate.py --donor-subset,
# including the ALREADY closure file, compiled with javac --release 21 -Xlint:all -Werror) and the relocated classes
# (target/classes + target/test-classes of m3/collections) replay the same inputs.
# (a) The relocated oracle mains and their donor originals must print byte-identical output: the algebra proof at its
#     default budget, the failure proof in isolated JVMs (rehash under -Xmx32m, remove under -Xmx16m).
# (b) M3FourLibraryBagsDump (test scope, not a test) replays <ops> seeded random bag/map operations; the donor side
#     compiles the same source with the type names and the package line mapped back through the TYPE and PACKAGE rows
#     of RELOCATION_MAP.tsv. The dumps must be byte-identical with the JIT and under -Xint (<xint-ops> operations),
#     and the -Xint dump must equal a JIT dump of the same length.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests of both sides. No file in the naming scope names a donor
# package literally: donor names come from RELOCATION_MAP.tsv next to this script.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [ops] [seed] [xint-ops]   (JAVA_HOME = JDK 21;
#        run after mvn -DskipTests package, which compiles the test sources)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
rm -rf "$3/src" "$3/classes"
mkdir -p "$3/src" "$3/classes"
SCRATCH=$(winpath "$3")
OPS=${4:-200000}
SEED=${5:-20261106}
XOPS=${6:-40000}
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
PY=${PYTHON:-python}
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
echo "DONOR $donor_commit package=$donor_package"

# donor side: main files verbatim, oracles verbatim (no DROP/REWRITE rows in this map), the ALREADY closure file
"$PY" -I "$RECIPE_DIR/relocate.py" --donor-git "$DONOR_GIT" --donor-rev "$REV" --donor-subset "$SCRATCH/src" \
    > "$SCRATCH/subset.log"
grep -c "SUBSET_VERBATIM=TRUE" "$SCRATCH/subset.log" | sed 's/^/subset verbatim files: /'
tail -n 1 "$SCRATCH/subset.log"
# the dump driver, mapped back: TYPE rows target->donor (whole words), PACKAGE row target->donor
donor_dir="$SCRATCH/src/$(echo "$donor_package" | tr . /)"
sed_script="$SCRATCH/reverse.sed"
awk -F'\t' -v dp="$donor_package" -v tp="$target_package" '
    $1=="TYPE" {printf "s/\\<%s\\>/%s/g\n", $3, $2}
    END {gsub(/\./, "\\.", tp); printf "s/^package %s;$/package %s;/\n", tp, dp}' "$MAP" > "$sed_script"
sed -f "$sed_script" "$MODULE/src/test/java/$(echo "$target_package" | tr . /)/M3FourLibraryBagsDump.java" \
    > "$donor_dir/M3FourLibraryBagsDump.java"
find "$SCRATCH/src" -name '*.java' | sort > "$SCRATCH/sources.txt"
"$JAVA_BIN/javac" --release 21 -Xlint:all -Werror -d "$SCRATCH/classes" @"$SCRATCH/sources.txt"
echo "javac donor sources: $(wc -l < "$SCRATCH/sources.txt") files, classes in $SCRATCH/classes"
RCP="$MODULE/target/classes$SEP$MODULE/target/test-classes"

# (a) the oracles, donor original vs relocated
oracle() {  # <side-classpath> <package> <prefix> <out>
    "$JAVA_BIN/java" -cp "$1" "$2.${3}BagAlgebraProof" >> "$4"
    "$JAVA_BIN/java" -Xmx32m -cp "$1" "$2.${3}BagMapFailureProof" rehash >> "$4"
    "$JAVA_BIN/java" -Xmx16m -cp "$1" "$2.${3}BagMapFailureProof" remove >> "$4"
}
: > "$SCRATCH/donor-oracles.txt"; : > "$SCRATCH/relocated-oracles.txt"
oracle "$SCRATCH/classes" "$donor_package" "" "$SCRATCH/donor-oracles.txt"
oracle "$RCP" "$target_package" M3 "$SCRATCH/relocated-oracles.txt"

# (b) the dump: JIT, -Xint, and a JIT run of the -Xint length
"$JAVA_BIN/java" -cp "$SCRATCH/classes" "$donor_package.M3FourLibraryBagsDump" "$OPS" "$SEED" > "$SCRATCH/donor-dump.txt"
"$JAVA_BIN/java" -cp "$RCP" "$target_package.M3FourLibraryBagsDump" "$OPS" "$SEED" > "$SCRATCH/relocated-dump.txt"
"$JAVA_BIN/java" -Xint -cp "$SCRATCH/classes" "$donor_package.M3FourLibraryBagsDump" "$XOPS" "$SEED" > "$SCRATCH/donor-xint.txt"
"$JAVA_BIN/java" -Xint -cp "$RCP" "$target_package.M3FourLibraryBagsDump" "$XOPS" "$SEED" > "$SCRATCH/relocated-xint.txt"
"$JAVA_BIN/java" -cp "$RCP" "$target_package.M3FourLibraryBagsDump" "$XOPS" "$SEED" > "$SCRATCH/relocated-jitshort.txt"

status=PASS
compare() {  # <label> <file-a> <file-b>
    local a_sha b_sha verdict
    a_sha=$(sha256sum "$2" | cut -d' ' -f1); b_sha=$(sha256sum "$3" | cut -d' ' -f1)
    if cmp -s "$2" "$3"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 $verdict sha256=$a_sha/$b_sha bytes=$(wc -c < "$2") lines=$(wc -l < "$2")"
}
compare oracles "$SCRATCH/donor-oracles.txt" "$SCRATCH/relocated-oracles.txt"
compare dump "$SCRATCH/donor-dump.txt" "$SCRATCH/relocated-dump.txt"
compare dump-xint "$SCRATCH/donor-xint.txt" "$SCRATCH/relocated-xint.txt"
compare xint-vs-jit "$SCRATCH/relocated-xint.txt" "$SCRATCH/relocated-jitshort.txt"
tr '\n' ' ' < "$SCRATCH/relocated-oracles.txt"; echo
tail -n 1 "$SCRATCH/relocated-dump.txt" | cut -c1-160
echo "dump lines carrying a thrown exception: $(grep -c ' -> !' "$SCRATCH/relocated-dump.txt" || true)"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
