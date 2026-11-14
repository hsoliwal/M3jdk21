#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of the relocation: the donor classes and the relocated classes (target/classes and target/test-classes of the
# module) replay the same inputs.
# (a) Dump: the main-scope FILE rows of RELOCATION_MAP.tsv are compiled from the donor git tree with javac --release 21
#     -Xlint:all -Werror; the reflective dump driver M3LintStorageDump (test scope) replays CASES seeded values through
#     the donor width enum and lint class and through the relocated ones on the default JIT, then XINT_CASES values
#     under -Xint; each pair of dumps must be byte-identical.
# (b) Crate test: the test-scope FILE row runs on both sides through the JUnit console launcher, the donor test against
#     its donor closure (every FILE and NEC .java donor row of the map, then every imported donor type, fetched from
#     the donor tree until no new file is needed; NEC rows module:<path> name M3JDK owners and are skipped; javac
#     --release 21 -Xlint:all -Werror) and the relocated test against the module build. The result trees, with the donor package and type names mapped by the PACKAGE and TYPE rows and the
#     elapsed-time line removed, must be identical.
# Prints DIFFERENTIAL PASS|FAIL with the sha256 digests. Donor package and type names come from the map.
# Derived from m3/arrays/recipe/int-ring-buffer-20261106/differential.sh: one dump serves two owners (the width enum
# and the lint class), and the crate test is a JUnit class, not an oracle main, so it runs through the console launcher.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-platform-console-standalone.jar>
#        [CASES=20000] [XINT_CASES=2000]   (JAVA_HOME = JDK 21; run after mvn package)
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CONSOLE=$4
CASES=${5:-20000}
XINT_CASES=${6:-2000}
mkdir -p "$3/donor-classes" "$3/donor-src" "$3/donor-test-classes"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
package_path=${donor_package//.//}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
type_of() { awk -F'\t' -v d="$1" '$1=="TYPE" && $2==d {print $3; exit}' "$MAP"; }
fetch() { # fetch <donor path> <scratch-relative destination>
    mkdir -p "$(dirname "$SCRATCH/$2")"
    git -C "$DONOR_GIT" show "$REV:$1" > "$SCRATCH/$2"
    echo "DONOR $donor_commit $1 blob=$(git -C "$DONOR_GIT" rev-parse "$REV:$1" | cut -c1-12)"
}
rm -rf "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/donor-test-classes"
mkdir -p "$SCRATCH/donor-src" "$SCRATCH/donor-classes" "$SCRATCH/donor-test-classes"
# (a) main-scope owners and the dump
mains=()
while IFS=$'\t' read -r kind donor target _note; do
    [ "$kind" = FILE ] || continue
    case "$target" in src/main/java/*) ;; *) continue ;; esac
    fetch "$donor" "donor-src/main/$package_path/$(basename "$donor")"
    mains+=("$SCRATCH/donor-src/main/$package_path/$(basename "$donor")")
done < "$MAP"
"$JAVA_BIN/javac" -J-Djava.io.tmpdir="$SCRATCH" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror \
    -d "$SCRATCH/donor-classes" "${mains[@]}"
width_donor=$(basename "${mains[0]}" .java)
lint_donor=$(basename "${mains[1]}" .java)
RELOCATED="$MODULE/target/classes$SEP$MODULE/target/test-classes"
DUMP="$target_package.M3LintStorageDump"
status=PASS
compare() {
    local left right verdict
    left=$(sha256sum "$SCRATCH/$1.txt" | cut -d' ' -f1)
    right=$(sha256sum "$SCRATCH/$2.txt" | cut -d' ' -f1)
    if cmp -s "$SCRATCH/$1.txt" "$SCRATCH/$2.txt"; then verdict=IDENTICAL; else verdict=DIFFERENT; status=FAIL; fi
    echo "$1 vs $2 $verdict sha256=$left/$right bytes=$(wc -c < "$SCRATCH/$1.txt") lines=$(wc -l < "$SCRATCH/$1.txt")"
}
dump() { # dump <name> <class path> <width class> <lint class> <cases> [jvm flag]
    "$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" ${6:-} -cp "$2" "$DUMP" "$3" "$4" "$5" > "$SCRATCH/$1.txt"
}
DONOR_CP="$SCRATCH/donor-classes$SEP$MODULE/target/test-classes"
dump donor-dump "$DONOR_CP" "$donor_package.$width_donor" "$donor_package.$lint_donor" "$CASES"
dump relocated-dump "$RELOCATED" "$target_package.$(type_of "$width_donor")" "$target_package.$(type_of "$lint_donor")" "$CASES"
compare donor-dump relocated-dump
dump donor-dump-xint "$DONOR_CP" "$donor_package.$width_donor" "$donor_package.$lint_donor" "$XINT_CASES" -Xint
dump relocated-dump-xint "$RELOCATED" "$target_package.$(type_of "$width_donor")" "$target_package.$(type_of "$lint_donor")" "$XINT_CASES" -Xint
compare donor-dump-xint relocated-dump-xint
tail -n 1 "$SCRATCH/relocated-dump.txt"
tail -n 1 "$SCRATCH/relocated-dump-xint.txt"
# (b) the crate test on both sides
test_donor=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/test\/java\// {print $2; exit}' "$MAP")
test_type=$(basename "$test_donor" .java)
donor_main_root=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\/java\// {print $2; exit}' "$MAP")
donor_main_root=${donor_main_root%/"$package_path"/*}
donor_test_root=${test_donor%/"$package_path"/*}
awk -F'\t' '($1=="FILE" || $1=="NEC") && $2 ~ /\.java$/ && $2 !~ /^module:/ {print $2}' "$MAP" | while read -r path; do
    case "$path" in "$donor_test_root"/*) scope=test ;; *) scope=main ;; esac
    fetch "$path" "donor-src/$scope/$package_path/$(basename "$path")" > /dev/null
done
# An imported donor type outside the family package brings its whole package directory (same-package references are
# not imports); the family package itself is limited to the FILE and NEC rows above.
while :; do
    added=0
    while read -r dir; do
        [ "$dir" = "$package_path" ] && continue
        [ -d "$SCRATCH/donor-src/main/$dir" ] && continue
        git -C "$DONOR_GIT" cat-file -e "$REV:$donor_main_root/$dir" 2>/dev/null || continue
        while read -r path; do
            fetch "$path" "donor-src/main/${path#"$donor_main_root"/}" > /dev/null
        done < <(git -C "$DONOR_GIT" ls-tree --name-only "$REV" "$donor_main_root/$dir/" | grep '\.java$')
        added=$((added + 1))
    done < <(find "$SCRATCH/donor-src" -name '*.java' -exec cat {} + \
        | sed -nE 's/^import ([a-z][A-Za-z0-9_.]*)\.[A-Z][A-Za-z0-9_]*;.*/\1/p' | grep -vE '^(java|javax|jdk|org\.junit)\.' \
        | tr . / | sort -u)
    [ "$added" -eq 0 ] && break
done
echo "DONOR_TEST_CLOSURE files=$(find "$SCRATCH/donor-src" -name '*.java' | wc -l) root=$donor_main_root"
"$JAVA_BIN/javac" -J-Djava.io.tmpdir="$SCRATCH" --release 21 -encoding UTF-8 -proc:none -Xlint:all -Werror -implicit:class \
    -cp "$CONSOLE" -sourcepath "$SCRATCH/donor-src/main$SEP$SCRATCH/donor-src/test" -d "$SCRATCH/donor-test-classes" \
    "$SCRATCH/donor-src/test/$package_path/$test_type.java"
normalize="$SCRATCH/junit-normalize.sed"
awk -F'\t' '$1=="PACKAGE" && $2!=$3 {gsub(/\./, "\\.", $2); print "s/" $2 "/" $3 "/g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
junit() { # junit <name> <class path> <class>
    "$JAVA_BIN/java" -Djava.io.tmpdir="$SCRATCH" -jar "$CONSOLE" execute --disable-banner --disable-ansi-colors \
        --details=tree --details-theme=ascii -cp "$2" --select-class "$3" 2>&1 | tr -d '\r' \
        | grep -vE 'Test run finished after|^stty: ' | sed -f "$normalize" > "$SCRATCH/$1.txt" || true
}
junit donor-junit "$SCRATCH/donor-test-classes" "$donor_package.$test_type"
junit relocated-junit "$RELOCATED" "$target_package.$(type_of "$test_type")"
compare donor-junit relocated-junit
grep -E 'tests (successful|failed)' "$SCRATCH/relocated-junit.txt" | tr -s ' ' | tr '\n' ' '; echo
grep -q '0 tests failed' "$SCRATCH/relocated-junit.txt" || status=FAIL
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
