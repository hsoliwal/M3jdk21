#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 4 of recipe pmem-accelerator-20261107: javap -p (signatures) and javap -p -c (bytecode) of the donor classes
# against the relocated classes, both compiled here by the same javac with the same flags (JAVA_HOME, --release 21
# -proc:none -Xlint:all -Werror, no -g): the owner classes are the module's target/classes (Maven's -g adds only the
# debug tables for them), the relocated self-test is compiled from the module's test source because Maven's -g
# compile of a main with many locals lays its loops out differently from the plain compile of the donor (the first
# runs showed this on PmemAcceleratorSelfTest: -p identical, -c different; javac 21.0.13 vs 21.0.9 added nothing).
# The chain closes with a -g compile of the relocated self-test compared byte for byte with Maven's class file. The donor sources are the files donor.sh extracted (<donor-dir>: the accelerator
# owner and its self-test are compiled as they are; the NumaTopology template is compiled with two stubs of its HOLD
# closure, CpuAffinity.allowedCpuIds and NativeBindings.cpuFeatures, written here). The donor javap text is normalized
# by the PACKAGE rows of RELOCATION_MAP.tsv (dotted and slashed), the TYPE rows whose names differ and the library
# name of the TEXT row. For the parser only the kept members are compared: the Node record whole and the parseCpuList
# javap -p -c section; discover and nativeFeatures are the DROP rows. Prints JAVAP PASS|FAIL with the counts.
# usage: javap_parity.sh <donor-dir of donor.sh> <scratch-dir>   env: JAVA_HOME (a JDK 21)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
DONOR_DIR=$1
SCRATCH=$2
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
OUT="$SCRATCH/javap"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) w() { cygpath -m "$1"; } ;; *) w() { printf '%s' "$1"; } ;; esac
rm -rf "$OUT"
# the donor packages come from the map (no donor spelling in this script: the naming recipe rewrites scripts)
accel_package=$(awk -F'\t' '$1=="PACKAGE" && $2 ~ /accelerator/ && $2 !~ /^com\.m3\./ {print $2; exit}' "$MAP")
numa_package=$(awk -F'\t' '$1=="PACKAGE" && $2 ~ /^com\.m3\./ {print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE"{print $3; exit}' "$MAP")
target_dir=$(echo "$target_package" | tr . /)
numa_dir="$OUT/numa/src/$(echo "$numa_package" | tr . /)"
mkdir -p "$OUT/donor/classes" "$numa_dir" "$OUT/numa/classes"
normalize="$OUT/normalize.sed"
awk -F'\t' '$1=="PACKAGE" {d=$2; s=$2; t=$3; ts=$3; gsub(/\./, "\\.", d); gsub(/\./, "/", s); gsub(/\./, "/", ts); print "s#" d "#" t "#g"; print "s#" s "#" ts "#g"}' "$MAP" > "$normalize"
awk -F'\t' '$1=="TYPE" && $2!=$3 {print "s/\\b" $2 "\\b/" $3 "/g"}' "$MAP" >> "$normalize"
awk -F'\t' '$1=="TEXT" {d=$2; t=$3; sub(/^System.loadLibrary\("/, "", d); sub(/"\)$/, "", d); sub(/^System.loadLibrary\("/, "", t); sub(/"\)$/, "", t); print "s/String " d "$/String " t "/"}' "$MAP" >> "$normalize"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -d "$(w "$OUT/donor/classes")" "$DONOR_DIR"/*Accelerator*.java
echo "DONOR accelerator classes compiled by $("$JAVA_BIN/javac" -version 2>&1)"
# the relocated self-test from source, plain and with -g (the -g one must equal Maven's class file byte for byte)
selftest_source=$(ls "$MODULE/src/test/java/$target_dir"/*AcceleratorSelfTest.java)
mkdir -p "$OUT/relocated/classes" "$OUT/relocated-g/classes"
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$(w "$MODULE/target/classes")" -d "$(w "$OUT/relocated/classes")" "$selftest_source"
"$JAVA_BIN/javac" -g --release 21 -proc:none -Xlint:all -Werror -cp "$(w "$MODULE/target/classes")" -d "$(w "$OUT/relocated-g/classes")" "$selftest_source"
selftest_name=$(basename "$selftest_source" .java)
if cmp -s "$OUT/relocated-g/classes/$target_dir/$selftest_name.class" "$MODULE/target/test-classes/$target_dir/$selftest_name.class"; then
    echo "MAVEN_CLASS $selftest_name: Maven's test class file == javac -g of the same source (byte-identical)"
else
    echo "JAVAP FAIL $selftest_name: Maven's class file differs from javac -g of its source"; exit 1
fi
signatures=0
bytecode=0
total=0
compare() { # <name> <donor class file> <relocated class file>
    local name=$1 donor=$2 target=$3
    total=$((total + 1))
    for mode in p c; do
        flags=-p; [ "$mode" = c ] && flags="-p -c"
        "$JAVA_BIN/javap" $flags "$(w "$donor")" | tr -d '\r' | sed -f "$normalize" > "$OUT/$name.donor.$mode.txt"
        "$JAVA_BIN/javap" $flags "$(w "$target")" | tr -d '\r' > "$OUT/$name.relocated.$mode.txt"
        if cmp -s "$OUT/$name.donor.$mode.txt" "$OUT/$name.relocated.$mode.txt"; then
            [ "$mode" = p ] && signatures=$((signatures + 1)) || bytecode=$((bytecode + 1))
        else
            echo "JAVAP DIFF $name -$mode"
            diff "$OUT/$name.donor.$mode.txt" "$OUT/$name.relocated.$mode.txt" | head -n 10
        fi
    done
}
while read -r class; do
    name=$(basename "$class" .class)
    target_name=$(printf '%s\n' "$name" | sed -f "$normalize")
    target="$MODULE/target/classes/$target_dir/$target_name.class"
    [ -f "$target" ] || target="$OUT/relocated/classes/$target_dir/$target_name.class"
    [ -f "$target" ] || { echo "JAVAP FAIL no relocated class for $name"; exit 1; }
    compare "$name" "$class" "$target"
done < <(find "$OUT/donor/classes" -name '*.class' | sort)
# the parser: donor template + stubs of the HOLD closure, in the donor's package
cp "$DONOR_DIR"/NumaTopology.java.template "$numa_dir/NumaTopology.java"
printf 'package %s;\nfinal class CpuAffinity { static int[] allowedCpuIds() { return new int[0]; } }\n' "$numa_package" > "$numa_dir/CpuAffinity.java"
printf 'package %s;\nfinal class NativeBindings { static String cpuFeatures() { return ""; } }\n' "$numa_package" > "$numa_dir/NativeBindings.java"
"$JAVA_BIN/javac" --release 21 -proc:none -d "$(w "$OUT/numa/classes")" "$numa_dir"/*.java
numa_classes="$OUT/numa/classes/$(echo "$numa_package" | tr . /)"
compare 'NumaTopology$Node' "$numa_classes/NumaTopology\$Node.class" "$MODULE/target/classes/$target_dir/M3NumaTopology\$Node.class"
section() { # <javap text> : the parseCpuList method and its Code section, constant-pool indices stripped (the donor
    # class holds two more methods, so its pool is numbered differently; the resolved names in the // comments stay)
    awk '/parseCpuList\(java.lang.String\);/ {on=1; print; next} on && (/^  [^ ]/ || /^}$/) {on=0} on {print}' "$1" \
        | sed -E 's/#[0-9]+(,[ ]+[0-9]+)?[ ]*//' | grep -v '^$'
}
total=$((total + 1))
"$JAVA_BIN/javap" -p -c "$(w "$numa_classes/NumaTopology.class")" | tr -d '\r' | sed -f "$normalize" > "$OUT/NumaTopology.donor.full.txt"
"$JAVA_BIN/javap" -p -c "$(w "$MODULE/target/classes/$target_dir/M3NumaTopology.class")" | tr -d '\r' > "$OUT/NumaTopology.relocated.full.txt"
section "$OUT/NumaTopology.donor.full.txt" > "$OUT/NumaTopology.donor.parseCpuList.txt"
section "$OUT/NumaTopology.relocated.full.txt" > "$OUT/NumaTopology.relocated.parseCpuList.txt"
[ -s "$OUT/NumaTopology.donor.parseCpuList.txt" ] || { echo "JAVAP FAIL empty parseCpuList section"; exit 1; }
if cmp -s "$OUT/NumaTopology.donor.parseCpuList.txt" "$OUT/NumaTopology.relocated.parseCpuList.txt"; then
    signatures=$((signatures + 1)); bytecode=$((bytecode + 1))
    echo "JAVAP parseCpuList identical ($(wc -l < "$OUT/NumaTopology.relocated.parseCpuList.txt") lines); members: relocated $(grep -cE '^  [^ ].*\(' "$OUT/NumaTopology.relocated.full.txt"), donor $(grep -cE '^  [^ ].*\(' "$OUT/NumaTopology.donor.full.txt") (discover and nativeFeatures dropped)"
else
    echo "JAVAP DIFF NumaTopology.parseCpuList"; diff "$OUT/NumaTopology.donor.parseCpuList.txt" "$OUT/NumaTopology.relocated.parseCpuList.txt" | head -n 10
fi
if [ "$signatures" -eq "$total" ] && [ "$bytecode" -eq "$total" ]; then
    echo "JAVAP PASS classes=$total signatures_identical=$signatures bytecode_identical=$bytecode (names normalized by the map)"
else
    echo "JAVAP FAIL classes=$total signatures_identical=$signatures bytecode_identical=$bytecode"
    exit 1
fi
