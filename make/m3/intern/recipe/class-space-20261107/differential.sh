#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe class-space-20261107 (after mvn package of make/m3/intern; JAVA_HOME = JDK 21).
# (a) The donor closure (every Java FILE row of RELOCATION_MAP.tsv except the JUnit tests) is compiled from the donor
#     git tree with javac --release 21 -g -proc:none -Xlint:all -Werror (-g as Maven compiles the module: two oracles
#     use a compiled class file of the family as their fixture); each donor oracle main and its relocated twin must
#     print byte-identical output.
# (b) M3ClassSpaceDifferentialDump (test scope) is compiled a second time for the donor with only its package line
#     replaced; donor and relocated dumps, default JIT and -Xint, must be byte-identical (model stream, budget,
#     collisions, region / batch admission, archive round trip through a mapped file, corruptions, JDK class files).
# (c) JNI lane (when a C17 compiler is on PATH, else SKIP): javac -h headers of the donor and the relocated test-scope
#     declarations, the donor and the relocated C oracles compiled with -std=c17 -Wall -Wextra -Werror, and the three
#     oracle mains with their native lane under -Xcheck:jni: donor and relocated output byte-identical.
# (d) Failure-atomic growth: the donor's inject-growth-faults.py (read from the donor tree, applied to scratch copies
#     only) throws a synthetic OutOfMemoryError in lane growth and in bucket growth; 'after' instrumentation of the
#     donor head and of the relocated M3ClassSpace must print FAULT_REPLAY_PASS lanes / buckets (state, every lane width
#     and the retry ordinal unchanged), and 'before' instrumentation of the crate preimage (donor proof/preimage) must
#     reproduce its two recorded baseline defects (the probe refuses both).
# (e) Allocation probe: donor and relocated M3ArchiveAllocationProbe; atoms, archive bytes and bytes per freeze must be
#     equal (nanoseconds per freeze printed, never compared).
# (f) Mutant lane: one-line mutants of the relocated owners, compiled into the scratch and shadowing the relocated
#     class, must be refused by the dump or a relocated oracle (they are live): exact-byte comparison skipping the
#     first byte (visible only under the dump's forced-collision owner: SHA-256 hides it elsewhere), a wrong ordinal
#     for an in-batch duplicate, no canonical duplicate refusal in the archive, long / double taking one CP slot.
# (g) javap -p of every class of the three owners (nested included), package names masked: donor = relocated.
# The donor package comes from RELOCATION_MAP.tsv next to this script, so no file in the naming scope names it.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [dump operations, default 100000]
set -euo pipefail
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); }; JNI_OS=win32; LIB=dll ;;
    Darwin*) SEP=':'; winpath() { (cd "$1" && pwd); }; JNI_OS=darwin; LIB=dylib ;;
    *) SEP=':'; winpath() { (cd "$1" && pwd); }; JNI_OS=linux; LIB=so ;;
esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
OPERATIONS=${4:-100000}
rm -rf "$3/src" "$3/classes" "$3/out" "$3/jni" "$3/fault" "$3"/mutant-*
mkdir -p "$3/src" "$3/classes" "$3/out" "$3/tmp" "$3/jni" "$3/fault"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA=("$JAVA_BIN/java" "-Djava.io.tmpdir=$SCRATCH/tmp")
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_dir=${target_package//.//}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
donor_path() { awk -F'\t' -v t="$1" '$1=="FILE" && $3==t {print $2}' "$MAP"; }
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.java$/ {print $2}' "$MAP")
compiled=0
for path in "${donor_files[@]}"; do
    body=$(git -C "$DONOR_GIT" show "$REV:$path")
    case "$body" in *"org.junit"*) continue ;; esac
    printf '%s\n' "$body" > "$SCRATCH/src/$(basename "$path")"
    compiled=$((compiled + 1))
done
# (b) the dump, second compile for the donor: only the package line differs (refused unless it occurs exactly once)
dump_src="$MODULE/src/test/java/$target_dir/M3ClassSpaceDifferentialDump.java"
[ "$(grep -c "^package $target_package;\$" "$dump_src")" = 1 ] || { echo "dump package line not found exactly once"; exit 1; }
sed "s/^package $target_package;\$/package $donor_package;/" "$dump_src" > "$SCRATCH/src/M3ClassSpaceDifferentialDump.java"
echo "DONOR $donor_commit closure=$compiled (+ dump driver; JUnit tests skipped)"
"$JAVA_BIN/javac" --release 21 -g -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
status=PASS
verdict() { # label donor-file relocated-file
    local sha_d sha_r v=IDENTICAL
    sha_d=$(sha256sum "$2" | cut -d' ' -f1); sha_r=$(sha256sum "$3" | cut -d' ' -f1)
    cmp -s "$2" "$3" || { v=DIFFERENT; status=FAIL; }
    echo "$1 $v donor_sha256=$sha_d relocated_sha256=$sha_r bytes=$(wc -c < "$2") last=$(tail -1 "$3" | tr -d '\r' | cut -c1-150)"
}
# (a) oracle mains
for oracle in M3ClassSpaceSelfTest M3ClassFileAtomsSelfTest M3ClassSpaceRegionSelfTest M3ClassSpaceAtomicBatchSelfTest \
        M3ClassSpaceArchiveSelfTest M3ClassSpacePinnedMappedSelfTest M3ClassSpacePinnedSnapshotScopedTest \
        M3ArchiveCopyToSelfTest M3ArchiveFreezeParitySelfTest; do
    "${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.$oracle" > "$SCRATCH/out/donor-$oracle.txt"
    "${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.$oracle" > "$SCRATCH/out/relocated-$oracle.txt"
    verdict "$oracle" "$SCRATCH/out/donor-$oracle.txt" "$SCRATCH/out/relocated-$oracle.txt"
done
# (b) dump, default JIT and -Xint
for mode in default xint; do
    flags=(); [ "$mode" = xint ] && flags=(-Xint)
    "${JAVA[@]}" "${flags[@]}" -cp "$SCRATCH/classes" "$donor_package.M3ClassSpaceDifferentialDump" "$OPERATIONS" > "$SCRATCH/out/donor-dump-$mode.txt"
    "${JAVA[@]}" "${flags[@]}" -cp "$RELOCATED_CP" "$target_package.M3ClassSpaceDifferentialDump" "$OPERATIONS" > "$SCRATCH/out/relocated-dump-$mode.txt"
    verdict "dump-$mode" "$SCRATCH/out/donor-dump-$mode.txt" "$SCRATCH/out/relocated-dump-$mode.txt"
done
verdict dump-default-vs-xint "$SCRATCH/out/relocated-dump-default.txt" "$SCRATCH/out/relocated-dump-xint.txt"
grep -E "^(serial|budget|collisions|regions|archive|classfiles) .*(operations|images|batches|roundtrip|symbols)=|^corruptions=|^CHECKS |^RESULT " \
    "$SCRATCH/out/relocated-dump-default.txt" | grep -v " op=" | cut -c1-260
# (c) JNI lane
CC_BIN=$(command -v cc || command -v gcc || command -v clang || true)
if [ -z "$CC_BIN" ]; then
    echo "JNI SKIP no C compiler on PATH"
else
    INC=("-I$JAVA_HOME/include" "-I$JAVA_HOME/include/$JNI_OS")
    for side in donor relocated; do
        mkdir -p "$SCRATCH/jni/$side/h" "$SCRATCH/jni/$side/cls"
        if [ "$side" = donor ]; then
            decls=("$SCRATCH/src/M3ClassSpaceNative.java" "$SCRATCH/src/M3ClassSpaceArchiveNative.java")
            c_class=$(donor_path src/test/native/m3_class_space_native.c); c_archive=$(donor_path src/test/native/m3_classspace_archive_native.c)
            git -C "$DONOR_GIT" show "$REV:$c_class" > "$SCRATCH/jni/$side/m3_class_space_native.c"
            git -C "$DONOR_GIT" show "$REV:$c_archive" > "$SCRATCH/jni/$side/m3_classspace_archive_native.c"
        else
            decls=("$MODULE/src/test/java/$target_dir/M3ClassSpaceNative.java" "$MODULE/src/test/java/$target_dir/M3ClassSpaceArchiveNative.java")
            cp "$MODULE/src/test/native/m3_class_space_native.c" "$MODULE/src/test/native/m3_classspace_archive_native.c" "$SCRATCH/jni/$side/"
        fi
        "$JAVA_BIN/javac" --release 21 -proc:none -h "$SCRATCH/jni/$side/h" -d "$SCRATCH/jni/$side/cls" "${decls[@]}"
        for c in m3_class_space_native m3_classspace_archive_native; do
            "$CC_BIN" -std=c17 -Wall -Wextra -Werror -shared -fPIC "${INC[@]}" "-I$SCRATCH/jni/$side/h" \
                "$SCRATCH/jni/$side/$c.c" -o "$SCRATCH/jni/$side/$c.$LIB"
        done
    done
    echo "JNI compiler=$("$CC_BIN" --version | head -1 | tr -d '\r') headers donor=$(ls "$SCRATCH/jni/donor/h" | tr '\n' ' ')relocated=$(ls "$SCRATCH/jni/relocated/h" | tr '\n' ' ')"
    run_jni() { # side package classpath
        local lib="$SCRATCH/jni/$1"
        "${JAVA[@]}" -Xcheck:jni -cp "$3" "$2.M3ClassSpaceSelfTest" "$lib/m3_class_space_native.$LIB" > "$SCRATCH/out/$1-jni-classspace.txt" 2>&1
        "${JAVA[@]}" -Xcheck:jni -cp "$3" "$2.M3ClassFileAtomsSelfTest" "$lib/m3_class_space_native.$LIB" > "$SCRATCH/out/$1-jni-atoms.txt" 2>&1
        "${JAVA[@]}" -Xcheck:jni "-Dm3.archive.native.library=$lib/m3_classspace_archive_native.$LIB" -cp "$3" \
            "$2.M3ClassSpaceArchiveSelfTest" > "$SCRATCH/out/$1-jni-archive.txt" 2>&1
    }
    run_jni donor "$donor_package" "$SCRATCH/classes"
    run_jni relocated "$target_package" "$RELOCATED_CP"
    for lane in classspace atoms archive; do
        verdict "jni-$lane" "$SCRATCH/out/donor-jni-$lane.txt" "$SCRATCH/out/relocated-jni-$lane.txt"
        grep -q "JNI PASS\|M3_CLASSSPACE_ARCHIVE_PASS" "$SCRATCH/out/relocated-jni-$lane.txt" || { echo "jni-$lane no PASS line"; status=FAIL; }
    done
fi
# (d) failure-atomic growth under injected OutOfMemoryError
INJECT=$(awk -F'\t' '$1=="OMIT" && $2 ~ /inject-growth-faults\.py$/ {print $2}' "$MAP")
PREIMAGE=${INJECT%/inject-growth-faults.py}/proof/preimage/M3ClassSpace.java
PROBE=$(donor_path "src/test/java/$target_dir/M3GrowthFaultProbe.java")
CLASS_SPACE=$(donor_path "src/main/java/$target_dir/M3ClassSpace.java")
git -C "$DONOR_GIT" show "$REV:$INJECT" > "$SCRATCH/fault/inject-growth-faults.py"
fault_case() { # label mode space-source probe-source package expectation(pass|defects)
    local dir="$SCRATCH/fault/$1" result=PASS
    mkdir -p "$dir/src" "$dir/classes"
    python -I "$SCRATCH/fault/inject-growth-faults.py" "$2" "$3" "$dir/src/M3ClassSpace.java"
    cp "$4" "$dir/src/M3GrowthFaultProbe.java"
    "$JAVA_BIN/javac" --release 21 -proc:none -d "$dir/classes" "$dir/src/M3ClassSpace.java" "$dir/src/M3GrowthFaultProbe.java"
    local lines=""
    for fault in lanes buckets; do
        if "${JAVA[@]}" -cp "$dir/classes" "$5.M3GrowthFaultProbe" "$fault" > "$dir/$fault.txt" 2>&1; then
            lines+="$fault=$(tr -d '\r' < "$dir/$fault.txt" | tail -1) "
            [ "$6" = pass ] || result=FAIL
        else
            lines+="$fault=REFUSED($(grep -m1 -oE 'AssertionError: .*' "$dir/$fault.txt" | tr -d '\r')) "
            [ "$6" = defects ] || result=FAIL
        fi
    done
    [ "$result" = PASS ] || status=FAIL
    echo "fault $1 $result $lines"
}
git -C "$DONOR_GIT" show "$REV:$CLASS_SPACE" > "$SCRATCH/fault/donor-head-M3ClassSpace.java"
git -C "$DONOR_GIT" show "$REV:$PREIMAGE" > "$SCRATCH/fault/donor-preimage-M3ClassSpace.java"
git -C "$DONOR_GIT" show "$REV:$PROBE" > "$SCRATCH/fault/donor-M3GrowthFaultProbe.java"
fault_case donor-preimage-before before "$SCRATCH/fault/donor-preimage-M3ClassSpace.java" "$SCRATCH/fault/donor-M3GrowthFaultProbe.java" "$donor_package" defects
fault_case donor-head-after after "$SCRATCH/fault/donor-head-M3ClassSpace.java" "$SCRATCH/fault/donor-M3GrowthFaultProbe.java" "$donor_package" pass
fault_case relocated-after after "$MODULE/src/main/java/$target_dir/M3ClassSpace.java" "$MODULE/src/test/java/$target_dir/M3GrowthFaultProbe.java" "$target_package" pass
# (e) allocation probe
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.M3ArchiveAllocationProbe" > "$SCRATCH/out/donor-probe.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3ArchiveAllocationProbe" > "$SCRATCH/out/relocated-probe.txt"
sed 's/ nanosPerFreeze=[0-9]*//' "$SCRATCH/out/donor-probe.txt" > "$SCRATCH/out/donor-probe-bytes.txt"
sed 's/ nanosPerFreeze=[0-9]*//' "$SCRATCH/out/relocated-probe.txt" > "$SCRATCH/out/relocated-probe-bytes.txt"
verdict M3ArchiveAllocationProbe_bytes "$SCRATCH/out/donor-probe-bytes.txt" "$SCRATCH/out/relocated-probe-bytes.txt"
echo "probe donor: $(tr -d '\r' < "$SCRATCH/out/donor-probe.txt")"
echo "probe relocated: $(tr -d '\r' < "$SCRATCH/out/relocated-probe.txt")"
# (f) mutant lane
mutant() { # label file sed-expression expected-text oracle [oracle args]
    local label=$1 dir="$SCRATCH/mutant-$1" file=$2 expression=$3 expected=$4 oracle=$5
    shift 5
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$expression" "$MODULE/src/main/java/$target_dir/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/$target_dir/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -proc:none -d "$dir/classes" -cp "$MODULE/target/classes" "$dir/$file"
    if "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.$oracle" "$@" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $label ACCEPTED"; status=FAIL
    elif grep -q "$expected" "$dir/err.txt"; then
        echo "mutant $label REFUSED: $(grep -m1 -oE "(AssertionError|IllegalArgumentException|IllegalStateException|IndexOutOfBoundsException).*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $label FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
mutant exact_bytes_skip_first M3ClassSpace.java '/private boolean equalsBytes(/,/for (int i = 0; i < length; i++)/s/int i = 0; i < length/int i = 1; i < length/' \
    "model disagreement" M3ClassSpaceDifferentialDump 20000
mutant batch_staged_id M3ClassSpace.java 's/ids\[cp\] = entries + staged;/ids[cp] = entries + additional;/' \
    "model disagreement" M3ClassSpaceDifferentialDump 20000
mutant canonical_duplicates M3ClassSpaceArchive.java 's/if (samePayload(order\[current\], order\[previous\])) {/if (false \&\& samePayload(order[current], order[previous])) {/' \
    "AssertionError" M3ClassSpaceArchiveSelfTest
mutant long_double_slot M3ClassFileAtoms.java 's/if (++i >= cpCount) {/if (i + 1 >= cpCount) {/' \
    "Exception" M3ClassSpaceDifferentialDump 20000
# (g) javap -p, package names masked
mask() { sed -e "s/$donor_package\.//g" -e "s/$target_package\.//g" -e "s#${donor_package//.//}/##g" -e "s#$target_dir/##g" | tr -d '\r'; }
javap_ok=0
for cls in M3ClassSpace 'M3ClassSpace$Binding' 'M3ClassSpace$DigestFunction' M3ClassSpaceArchive 'M3ClassSpaceArchive$View' \
        'M3ClassSpaceArchive$PinnedView' M3ClassFileAtoms 'M3ClassFileAtoms$Plan' 'M3ClassFileAtoms$Interned' 'M3ClassFileAtoms$Cursor'; do
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "$donor_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-donor-$cls.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-relocated-$cls.txt"
    if cmp -s "$SCRATCH/out/javap-donor-$cls.txt" "$SCRATCH/out/javap-relocated-$cls.txt"; then
        javap_ok=$((javap_ok + 1))
    else
        echo "javap $cls DIFFERENT"; status=FAIL
    fi
done
echo "javap classes=10 identical=$javap_ok"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
