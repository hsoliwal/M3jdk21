#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# Differential gate of recipe hash-xxhash64-streaming-20261106 (RELOCATE_XXHASH64_STREAMING v4).
# Donor side: the FILE rows of RELOCATION_MAP.tsv, read from the donor git tree at <donor-rev> byte for byte, compiled
# with javac --release 21 and the donor's own Lombok processor + SLF4J API (unchanged donor bytes), run on the STOCK JDK.
# Relocated side: the 17 jdk.internal.m3.hash sources of this worktree compiled with java.base's flags
# (-g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline) into a --patch-module java.base image, the relocated
# tests compiled against it and run with --patch-module java.base=<classes> --add-exports java.base/jdk.internal.m3.hash.
#   patch_compile   the java.base patch compiles with 0 warnings
#   contract        donor XXHash64StreamingTest (JUnit 5) on the donor; M3XxHash64StreamingTest on the relocated
#   proof           XXHash64StandaloneProof and M3XxHash64StreamingProof print the same PASS line
#   dump            M3XxHash64StreamingDump over the donor surface (stock JDK) and the relocated surface (patched
#                   java.base): byte-identical, 0 MISMATCH lines on both (streaming == one-shot in every case)
#   probe           the allocation probe on both sides (figures only, never a verdict)
# Prints one line per step and DIFFERENTIAL PASS|FAIL.
# usage: JAVA_HOME=<JDK 21> differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <maven-repo> [random-cases]
set -uo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
ROOT=$(winpath "$(dirname "$0")/../../../..")
DONOR_GIT=$1; REV=$2; M2=$4; CASES=${5:-10000}
mkdir -p "$3"
S=$(winpath "$3")
J="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
LOMBOK="$M2/org/projectlombok/lombok/1.18.42/lombok-1.18.42.jar"
SLF4J="$M2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
API="$M2/org/junit/jupiter/junit-jupiter-api/5.11.4/junit-jupiter-api-5.11.4.jar"
GUARDIAN="$M2/org/apiguardian/apiguardian-api/1.1.2/apiguardian-api-1.1.2.jar"
CS="$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar"
for jar in "$LOMBOK" "$SLF4J" "$API" "$GUARDIAN" "$CS"; do [ -f "$jar" ] || { echo "MISSING $jar (mvn dependency:get -Dmaven.repo.local=$M2)"; exit 2; }; done
rm -rf "$S/donor-src" "$S/donor-classes" "$S/psrc" "$S/patch" "$S/testc" "$S/out"
mkdir -p "$S/donor-src" "$S/donor-classes" "$S/psrc/jdk/internal/m3/hash" "$S/patch" "$S/testc" "$S/out"
status=PASS
step() { local name=$1 verdict=$2; shift 2; echo "STEP $name $verdict $*"; [ "$verdict" = PASS ] || [ "$verdict" = INFO ] || status=FAIL; }

commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" {print $2}' "$MAP")
for path in "${donor_files[@]}"; do
    dir="$S/donor-src/$(dirname "$path" | sed 's#.*/src/\(main\|test\)/java/##')"
    mkdir -p "$dir"
    git -C "$DONOR_GIT" show "$commit:$path" > "$dir/$(basename "$path")"
    echo "DONOR $(git -C "$DONOR_GIT" rev-parse "$commit:$path" | cut -c1-12) $path"
done
owner_fqn=$(dirname "${donor_files[0]}" | sed 's#.*/src/main/java/##; s#/#.#g').$(basename "${donor_files[0]}" .java)
test_fqn=$(dirname "${donor_files[1]}" | sed 's#.*/src/test/java/##; s#/#.#g').$(basename "${donor_files[1]}" .java)
proof_fqn=$(dirname "${donor_files[2]}" | sed 's#.*/src/test/java/##; s#/#.#g').$(basename "${donor_files[2]}" .java)
probe_fqn=$(dirname "${donor_files[3]}" | sed 's#.*/src/test/java/##; s#/#.#g').$(basename "${donor_files[3]}" .java)

# donor: unchanged bytes, the donor's Lombok processor and SLF4J API
"$J/javac" --release 21 -encoding UTF-8 -processorpath "$LOMBOK" -cp "$LOMBOK$SEP$SLF4J$SEP$API$SEP$GUARDIAN" -d "$S/donor-classes" \
    $(find "$S/donor-src" -name '*.java') > "$S/out/donor-javac.log" 2>&1
rc=$?; step donor_compile "$([ $rc = 0 ] && echo PASS || echo FAIL)" "rc=$rc classes=$(find "$S/donor-classes" -name '*.class' | wc -l) log=$S/out/donor-javac.log"

# relocated: java.base patch with java.base's flags, then the tests against it
cp "$ROOT"/src/java.base/share/classes/jdk/internal/m3/hash/*.java "$S/psrc/jdk/internal/m3/hash/"
"$J/javac" -g -Xlint:all -Werror -Xlint:-this-escape -XDstringConcat=inline --patch-module "java.base=$S/psrc" -d "$S/patch" \
    $(find "$S/psrc" -name '*.java') > "$S/out/patch-javac.log" 2>&1
rc=$?; step patch_compile "$([ $rc = 0 ] && echo PASS || echo FAIL)" "rc=$rc sources=$(find "$S/psrc" -name '*.java' | wc -l) classes=$(find "$S/patch" -name '*.class' | wc -l) warnings=$(grep -c 'warning:' "$S/out/patch-javac.log") log=$S/out/patch-javac.log"
T="$ROOT/test/jdk/jdk/internal/m3/hash"
"$J/javac" -g -Xlint:all -Werror --patch-module "java.base=$S/patch" --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$API$SEP$GUARDIAN" \
    -d "$S/testc" "$T/M3XxHash64StreamingTest.java" "$T/M3XxHash64StreamingProof.java" "$T/M3XxHash64StreamingAllocationProbe.java" \
    "$T/M3XxHash64StreamingDump.java" > "$S/out/test-javac.log" 2>&1
rc=$?; step test_compile "$([ $rc = 0 ] && echo PASS || echo FAIL)" "rc=$rc warnings=$(grep -c 'warning:' "$S/out/test-javac.log") log=$S/out/test-javac.log"
RUN=(--patch-module "java.base=$S/patch" --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED)

# contract: the donor JUnit class on the donor, the relocated JUnit class on the relocated owner
"$J/java" -jar "$CS" execute -cp "$S/donor-classes$SEP$SLF4J" --select-class "$test_fqn" --details=summary --disable-banner > "$S/out/donor-junit.txt" 2>&1
d=$(grep -E 'tests (successful|failed)' "$S/out/donor-junit.txt" | tr -s ' ' | tr '\n' ' ')
"$J/java" "${RUN[@]}" -jar "$CS" execute -cp "$S/testc" --select-class M3XxHash64StreamingTest --details=summary --disable-banner > "$S/out/relocated-junit.txt" 2>&1
r=$(grep -E 'tests (successful|failed)' "$S/out/relocated-junit.txt" | tr -s ' ' | tr '\n' ' ')
case "$d|$r" in *"3 tests successful"*"0 tests failed"*"|"*"3 tests successful"*"0 tests failed"*) v=PASS ;; *) v=FAIL ;; esac
step contract $v "donor: $d relocated: $r"

# proof mains
"$J/java" -cp "$S/donor-classes$SEP$SLF4J" "$proof_fqn" > "$S/out/donor-proof.txt" 2>"$S/out/donor-proof.err"; pd=$?
"$J/java" "${RUN[@]}" -cp "$S/testc" M3XxHash64StreamingProof > "$S/out/relocated-proof.txt" 2>"$S/out/relocated-proof.err"; pr=$?
v=FAIL; [ $pd = 0 ] && [ $pr = 0 ] && cmp -s "$S/out/donor-proof.txt" "$S/out/relocated-proof.txt" && v=PASS
step proof $v "rc=$pd/$pr $(cat "$S/out/relocated-proof.txt")"

# dump: donor surface on the stock JDK (no patch), relocated surface on the patched java.base
start=$(date +%s)
"$J/java" -Xmx2g -cp "$S/donor-classes$SEP$S/testc$SEP$SLF4J" M3XxHash64StreamingDump "$owner_fqn" "$CASES" > "$S/out/donor-dump.txt" 2>"$S/out/donor-dump.err"; dd=$?
"$J/java" -Xmx2g "${RUN[@]}" -cp "$S/testc" M3XxHash64StreamingDump m3 "$CASES" > "$S/out/relocated-dump.txt" 2>"$S/out/relocated-dump.err"; dr=$?
ds=$(sha256sum "$S/out/donor-dump.txt" | cut -d' ' -f1); rs=$(sha256sum "$S/out/relocated-dump.txt" | cut -d' ' -f1)
mm=$(( $(grep -c '^MISMATCH' "$S/out/donor-dump.txt") + $(grep -c '^MISMATCH' "$S/out/relocated-dump.txt") ))
v=FAIL; [ $dd = 0 ] && [ $dr = 0 ] && [ $mm = 0 ] && cmp -s "$S/out/donor-dump.txt" "$S/out/relocated-dump.txt" && v=PASS
step dump $v "rc=$dd/$dr donor_sha256=$ds relocated_sha256=$rs bytes=$(wc -c < "$S/out/donor-dump.txt") lines=$(wc -l < "$S/out/donor-dump.txt") mismatches=$mm seconds=$(( $(date +%s) - start )) $(tail -1 "$S/out/relocated-dump.txt")"

# mutant: the relocated getValue with its odd buffered tail byte dropped must be refused by the dump (the gate is live)
mkdir -p "$S/mutant/psrc/jdk/internal/m3/hash" "$S/mutant/patch"
cp "$S"/psrc/jdk/internal/m3/hash/*.java "$S/mutant/psrc/jdk/internal/m3/hash/"
sed -i 's/return M3XxHash.tail(h64, buffer, STRIPE, 0L, bufferSize);/return M3XxHash.tail(h64, buffer, STRIPE, 0L, bufferSize \& ~1);/' "$S/mutant/psrc/jdk/internal/m3/hash/M3XxHash64Streaming.java"
"$J/javac" -XDstringConcat=inline --patch-module "java.base=$S/mutant/psrc" -d "$S/mutant/patch" $(find "$S/mutant/psrc" -name '*.java') > "$S/out/mutant-javac.log" 2>&1
"$J/java" --patch-module "java.base=$S/mutant/patch" --add-exports java.base/jdk.internal.m3.hash=ALL-UNNAMED -cp "$S/testc" M3XxHash64StreamingDump m3 0 > "$S/out/mutant-dump.txt" 2>&1; mrc=$?
mml=$(grep -c '^MISMATCH' "$S/out/mutant-dump.txt")
v=FAIL; [ $mrc = 1 ] && [ "$mml" -gt 0 ] && v=PASS
step mutant $v "rc=$mrc mismatch_lines=$mml (getValue tail length bufferSize & ~1)"

# allocation probe: figures only
"$J/java" -cp "$S/donor-classes$SEP$SLF4J" "$probe_fqn" > "$S/out/donor-probe.txt" 2>&1
"$J/java" "${RUN[@]}" -cp "$S/testc" M3XxHash64StreamingAllocationProbe > "$S/out/relocated-probe.txt" 2>&1
step probe INFO "donor: $(tail -1 "$S/out/donor-probe.txt") relocated: $(tail -1 "$S/out/relocated-probe.txt")"
echo "DIFFERENTIAL $status donor_commit=$commit"
[ "$status" = PASS ]
