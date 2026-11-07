#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Gate 3 of recipe null-flow-20261107: the donor's own oracles (the three *Probe mains) compiled from the donor tree at
# the pin (every FILE row of RELOCATION_MAP.tsv, javac --release 21 -proc:none -Xlint:all -Werror) against the relocated
# classes (target/classes of the Maven main build + the relocated test sources compiled here), each run in the default
# JIT and in -Xint; the four stdouts (donor JIT, donor -Xint, relocated JIT, relocated -Xint) must be byte-identical.
# The probes print only their figures, so no name normalization is needed. The donor classes stay in <scratch>/donor
# for javap_parity.sh.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir>
# env:   JAVA_HOME (a JDK 21), JUNIT_CONSOLE (junit-platform-console-standalone-1.11.4.jar: the junit-jupiter API the
#        donor JUnit entries import)
set -euo pipefail
RECIPE_DIR=$(cd "$(dirname "$0")" && pwd)
MODULE=$(cd "$RECIPE_DIR/../.." && pwd)
DONOR=$1
REV=$2
SCRATCH=$3
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
CONSOLE="${JUNIT_CONSOLE:?JUNIT_CONSOLE must name junit-platform-console-standalone-1.11.4.jar}"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; w() { cygpath -m "$1"; } ;; *) SEP=':'; w() { printf '%s' "$1"; } ;; esac
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
rm -rf "$SCRATCH/donor" "$SCRATCH/relocated-test-classes"
mkdir -p "$SCRATCH/donor/src" "$SCRATCH/donor/classes" "$SCRATCH/relocated-test-classes"
commit=$(git -C "$DONOR" rev-parse "$REV^{commit}")
donor_package=$(awk -F'\t' '$1=="PACKAGE"{print $2; exit}' "$MAP")
target_package=$(awk -F'\t' '$1=="PACKAGE"{print $3; exit}' "$MAP")
while IFS=$'\t' read -r donor_path target _note; do
    git -C "$DONOR" cat-file blob "$commit:$donor_path" > "$SCRATCH/donor/src/$(basename "$donor_path")"
done < <(awk -F'\t' '$1=="FILE"{print $2 "\t" $3 "\t" $4}' "$MAP")
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$(w "$CONSOLE")" -d "$(w "$SCRATCH/donor/classes")" "$SCRATCH"/donor/src/*.java
echo "DONOR compiled $(ls "$SCRATCH"/donor/src/*.java | wc -l) files of $commit into $SCRATCH/donor/classes"
mapfile -t relocated_tests < <(find "$MODULE/src/test/java/$(echo "$target_package" | tr . /)" -name '*.java' | sort)
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$(w "$MODULE/target/classes")$SEP$(w "$CONSOLE")" \
    -d "$(w "$SCRATCH/relocated-test-classes")" "${relocated_tests[@]}"
PROBES="M3NullFlowFactsProbe M3NullFlowWorklistProbe M3NullFlowVariableIdsProbe"
run() { # <family> <mode-name> <classpath> <package> [jvm flags]
    local family=$1 mode=$2 cp=$3 package=$4
    shift 4
    : > "$SCRATCH/$family-$mode.txt"
    for probe in $PROBES; do
        "$JAVA_BIN/java" "$@" -XX:-UsePerfData -cp "$cp" "$package.$probe" >> "$SCRATCH/$family-$mode.txt"
    done
}
run donor jit "$(w "$SCRATCH/donor/classes")" "$donor_package"
run donor xint "$(w "$SCRATCH/donor/classes")" "$donor_package" -Xint
run relocated jit "$(w "$MODULE/target/classes")$SEP$(w "$SCRATCH/relocated-test-classes")" "$target_package"
run relocated xint "$(w "$MODULE/target/classes")$SEP$(w "$SCRATCH/relocated-test-classes")" "$target_package" -Xint
for pair in "donor-jit donor-xint" "donor-jit relocated-jit" "donor-xint relocated-xint"; do
    set -- $pair
    cmp "$SCRATCH/$1.txt" "$SCRATCH/$2.txt" || { echo "DIFFERENTIAL FAIL $1 vs $2"; exit 1; }
done
cat "$SCRATCH/donor-jit.txt"
echo "DIFFERENTIAL PASS donor=$commit figures identical in donor JIT, donor -Xint, relocated JIT, relocated -Xint; sha256=$(sha256sum "$SCRATCH/donor-jit.txt" | cut -c1-64)"
