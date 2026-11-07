#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe static-use-plan-20261107 (after mvn package of m3/compiler/static-use; JAVA_HOME = JDK 21).
# (a) The donor M3StaticUsePlan (FILE row, donor git tree) and M3StaticUsePlanDump (test scope, compiled a second time
#     with only its package line replaced) are compiled with javac --release 21 -g -proc:none -Xlint:all -Werror; donor
#     and relocated dumps (18,432 facts against the reference model, loop-bound shapes, growth sampling, annotation
#     retention), default JIT and -Xint, must be byte-identical.
# (b) The donor JUnit 5 test, compiled from the donor tree, runs through junit-platform-console-standalone against the
#     donor class (the relocated twin runs in the module's test phase).
# (c) Mutant lane: one-line mutants of the relocated owner, compiled into the scratch and shadowing it, must be refused
#     by the dump's reference model (they are live).
# (d) javap -p of the owner and its eleven nested types, package names masked: donor = relocated.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-platform-console-standalone jar>
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CONSOLE=$4
rm -rf "$3/src" "$3/classes" "$3/test-src" "$3/test-classes" "$3/out" "$3"/mutant-*
mkdir -p "$3/src" "$3/classes" "$3/test-src" "$3/test-classes" "$3/out" "$3/tmp"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA=("$JAVA_BIN/java" "-Djava.io.tmpdir=$SCRATCH/tmp")
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_dir=${target_package//.//}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
main_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/main\// {print $2}' "$MAP")
test_path=$(awk -F'\t' '$1=="FILE" && $3 ~ /^src\/test\// {print $2}' "$MAP")
git -C "$DONOR_GIT" show "$REV:$main_path" > "$SCRATCH/src/M3StaticUsePlan.java"
dump_src="$MODULE/src/test/java/$target_dir/M3StaticUsePlanDump.java"
[ "$(grep -c "^package $target_package;\$" "$dump_src")" = 1 ] || { echo "dump package line not found exactly once"; exit 1; }
sed "s/^package $target_package;\$/package $donor_package;/" "$dump_src" > "$SCRATCH/src/M3StaticUsePlanDump.java"
"$JAVA_BIN/javac" --release 21 -g -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
echo "DONOR $donor_commit owner=$main_path (+ dump driver)"
status=PASS
verdict() { # label donor-file relocated-file
    local sha_d sha_r v=IDENTICAL
    sha_d=$(sha256sum "$2" | cut -d' ' -f1); sha_r=$(sha256sum "$3" | cut -d' ' -f1)
    cmp -s "$2" "$3" || { v=DIFFERENT; status=FAIL; }
    echo "$1 $v donor_sha256=$sha_d relocated_sha256=$sha_r bytes=$(wc -c < "$2")"
}
# (a) dump, default JIT and -Xint
for mode in default xint; do
    flags=(); [ "$mode" = xint ] && flags=(-Xint)
    "${JAVA[@]}" "${flags[@]}" -cp "$SCRATCH/classes" "$donor_package.M3StaticUsePlanDump" > "$SCRATCH/out/donor-dump-$mode.txt"
    "${JAVA[@]}" "${flags[@]}" -cp "$RELOCATED_CP" "$target_package.M3StaticUsePlanDump" > "$SCRATCH/out/relocated-dump-$mode.txt"
    verdict "dump-$mode" "$SCRATCH/out/donor-dump-$mode.txt" "$SCRATCH/out/relocated-dump-$mode.txt"
done
verdict dump-default-vs-xint "$SCRATCH/out/relocated-dump-default.txt" "$SCRATCH/out/relocated-dump-xint.txt"
tr -d '\r' < "$SCRATCH/out/relocated-dump-default.txt" | cut -c1-240
# (b) donor JUnit test through the console launcher
git -C "$DONOR_GIT" show "$REV:$test_path" > "$SCRATCH/test-src/M3StaticUsePlanTest.java"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$SCRATCH/classes$SEP$CONSOLE" -d "$SCRATCH/test-classes" \
    "$SCRATCH/test-src/M3StaticUsePlanTest.java"
if "${JAVA[@]}" -jar "$CONSOLE" execute --disable-banner --details=summary \
        --class-path "$SCRATCH/classes$SEP$SCRATCH/test-classes" --select-class "$donor_package.M3StaticUsePlanTest" \
        > "$SCRATCH/out/donor-junit.txt" 2>&1; then
    echo "donor-junit PASS $(grep -E 'tests (successful|failed)' "$SCRATCH/out/donor-junit.txt" | tr -s ' []' ' ' | tr -d '\r' | tr '\n' ' ')"
else
    echo "donor-junit FAIL"; status=FAIL; tail -20 "$SCRATCH/out/donor-junit.txt"
fi
# (c) mutant lane
mutant() { # label sed-expression expected-text
    local label=$1 dir="$SCRATCH/mutant-$1" file=M3StaticUsePlan.java
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$2" "$MODULE/src/main/java/$target_dir/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/$target_dir/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -proc:none -d "$dir/classes" "$dir/$file"
    if "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.M3StaticUsePlanDump" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $label ACCEPTED"; status=FAIL
    elif grep -q "$3" "$dir/err.txt"; then
        echo "mutant $label REFUSED: $(grep -m1 -oE "AssertionError.*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $label FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
mutant published_not_materialized 's/if (facts.needsConcreteJavaObject() || facts.escape() == Escape.PUBLISHED) {/if (facts.needsConcreteJavaObject()) {/' "model disagreement"
mutant exclusive_bound_floor 's/: (distance + magnitude - 1) \/ magnitude;/: distance \/ magnitude;/' "model disagreement"
mutant growth_sample_ge 's/return required > physicalCapacity;/return required >= physicalCapacity;/' "model disagreement"
# (d) javap -p, package names masked
mask() { sed -e "s/$donor_package\.//g" -e "s/$target_package\.//g" | tr -d '\r'; }
javap_ok=0; total=0
for cls in M3StaticUsePlan 'M3StaticUsePlan$Escape' 'M3StaticUsePlan$Effect' 'M3StaticUsePlan$Decision' 'M3StaticUsePlan$Facts' \
        'M3StaticUsePlan$Plan' 'M3StaticUsePlan$Analyze' 'M3StaticUsePlan$CompilerSummary' 'M3StaticUsePlan$Relation' \
        'M3StaticUsePlan$IntLoop' 'M3StaticUsePlan$ReferenceMode' 'M3StaticUsePlan$1'; do
    [ -f "$SCRATCH/classes/${donor_package//.//}/$cls.class" ] || continue
    total=$((total + 1))
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "$donor_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-donor-$cls.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-relocated-$cls.txt"
    if cmp -s "$SCRATCH/out/javap-donor-$cls.txt" "$SCRATCH/out/javap-relocated-$cls.txt"; then javap_ok=$((javap_ok + 1)); else echo "javap $cls DIFFERENT"; status=FAIL; fi
done
echo "javap classes=$total identical=$javap_ok"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
