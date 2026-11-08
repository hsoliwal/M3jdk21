#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe mat-sortable-processor-20261107 (after mvn package of m3/compiler/mat-sortable; JAVA_HOME = JDK 21).
# The donor closure (the processor FILE row, the held emitter OMIT row that the donor processor still calls, the five
# mat annotations of the donor annotation package (EXTERNAL row: package and donor source root) and the donor contract)
# is compiled from the donor git tree with javac --release 21 -g -proc:none -Xlint:all -Werror.
# (a) Processor oracle: M3MatSortableProcessorOracle (test scope; it reaches the processor by class name) runs one fixture
#     corpus of mat sortables through the donor processor (donor annotations on the class path) and through the relocated
#     processor (jdk.m3.collections annotations on the class path); the generated companion sources and javac diagnostics
#     must be byte-identical after the declared masking (DECISION.tsv S7: annotation package and prefix, the held
#     emitter's companions and diagnostics, the held stableSignalOrder block).
# (b) javap -p of every processor class (nested records included), package and type names masked and the one declared
#     dropped field removed: donor = relocated.
# (c) Mutant lane: one-line mutants of the relocated processor, compiled into the scratch and shadowing it, must be
#     refused by the relocated contract M3SortableProcessorContract.
# (d) Baseline: the donor contract run against the donor processor (expected RED at develop 56cbdc631259: javac's
#     processing lint, DECISION.tsv S8); printed, never a verdict of this gate.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> [m3-collections classes dir or jar]
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
rm -rf "$3/src" "$3/classes" "$3/out" "$3/oracle-cp" "$3/work" "$3"/mutant-*
mkdir -p "$3/src" "$3/classes" "$3/out" "$3/tmp" "$3/oracle-cp" "$3/work"
SCRATCH=$(winpath "$3")
COLLECTIONS=${4:-$MODULE/../../collections/target/classes}
[ -e "$COLLECTIONS" ] || { echo "m3-collections classes not found: $COLLECTIONS"; exit 1; }
# a JVM class path entry must be a native path (an MSYS /s/... form is not): normalize a directory or a jar
if [ -d "$COLLECTIONS" ]; then COLLECTIONS=$(winpath "$COLLECTIONS"); else COLLECTIONS="$(winpath "$(dirname "$COLLECTIONS")")/$(basename "$COLLECTIONS")"; fi
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA=("$JAVA_BIN/java" "-Djava.io.tmpdir=$SCRATCH/tmp")
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_dir=${target_package//.//}
donor_dir=${donor_package//.//}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes$SEP$COLLECTIONS"
mapfile -t donor_files < <(awk -F'\t' '($1=="FILE" || ($1=="OMIT" && $2 ~ /MatCapabilityEmitter\.java$/)) && $2 ~ /\.java$/ {print $2}' "$MAP")
annotations=(Sortable SortField SortSignal SortDistance SortDependency)
annotation_package=$(awk -F'\t' '$1=="EXTERNAL" {print $2; exit}' "$MAP")
annotation_root=$(awk -F'\t' '$1=="EXTERNAL" {print $3; exit}' "$MAP")
for name in "${annotations[@]}"; do
    git -C "$DONOR_GIT" show "$REV:$annotation_root/${annotation_package//.//}/$name.java" > "$SCRATCH/src/$name.java"
done
closure=()
for path in "${donor_files[@]}"; do
    git -C "$DONOR_GIT" show "$REV:$path" > "$SCRATCH/src/$(basename "$path")"; closure+=("$(basename "$path" .java)")
done
"$JAVA_BIN/javac" --release 21 -g -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
echo "DONOR $donor_commit closure=${closure[*]} annotations=${annotations[*]}"
status=PASS
verdict() { # label donor-file relocated-file
    local sha_d sha_r v=IDENTICAL
    sha_d=$(sha256sum "$2" | cut -d' ' -f1); sha_r=$(sha256sum "$3" | cut -d' ' -f1)
    cmp -s "$2" "$3" || { v=DIFFERENT; status=FAIL; }
    echo "$1 $v donor_sha256=$sha_d relocated_sha256=$sha_r bytes=$(wc -c < "$2")"
}
# (a) processor oracle over one fixture corpus: donor processor vs relocated processor
mkdir -p "$SCRATCH/oracle-cp/$target_dir"
cp "$MODULE/target/test-classes/$target_dir/M3MatSortableProcessorOracle.class" "$SCRATCH/oracle-cp/$target_dir/"
"${JAVA[@]}" -cp "$SCRATCH/oracle-cp$SEP$SCRATCH/classes" "$target_package.M3MatSortableProcessorOracle" \
    "$donor_package.MatSortableProcessor" "$SCRATCH/work/oracle-donor" > "$SCRATCH/out/donor-processor-oracle.txt"
"${JAVA[@]}" -cp "$SCRATCH/oracle-cp$SEP$MODULE/target/classes$SEP$COLLECTIONS" "$target_package.M3MatSortableProcessorOracle" \
    "$target_package.M3MatSortableProcessor" "$SCRATCH/work/oracle-relocated" > "$SCRATCH/out/relocated-processor-oracle.txt"
verdict processor-oracle "$SCRATCH/out/donor-processor-oracle.txt" "$SCRATCH/out/relocated-processor-oracle.txt"
grep -E "^(CORPUS|DIAG|END|CHECKS|RESULT)" "$SCRATCH/out/relocated-processor-oracle.txt" | tr -d '\r' | cut -c1-200
echo "processor-oracle generated=$(grep -c '^GENERATED' "$SCRATCH/out/relocated-processor-oracle.txt") diagnostics=$(grep -c '^DIAG' "$SCRATCH/out/relocated-processor-oracle.txt") corpora=$(grep -c '^CORPUS' "$SCRATCH/out/relocated-processor-oracle.txt")"
# (b) javap -p, package and type names masked, the declared dropped field removed from the donor listing
mask() { sed -e "s/$donor_package\.//g" -e "s/$target_package\.//g" -e 's/\bMatSortableProcessor\b/M3MatSortableProcessor/g' | tr -d '\r'; }
javap_ok=0; total=0
for file in "$SCRATCH/classes/$donor_dir"/MatSortableProcessor*.class; do
    cls=$(basename "$file" .class)
    total=$((total + 1))
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "$donor_package.$cls" | grep -v '^Compiled from' \
        | grep -v 'private final java.util.Set<java.lang.String> capabilityEmitted;' | mask > "$SCRATCH/out/javap-donor-$cls.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.M3${cls}" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-relocated-$cls.txt" 2>/dev/null || true
    if cmp -s "$SCRATCH/out/javap-donor-$cls.txt" "$SCRATCH/out/javap-relocated-$cls.txt"; then javap_ok=$((javap_ok + 1)); else echo "javap $cls DIFFERENT"; status=FAIL; fi
done
echo "javap classes=$total identical=$javap_ok (donor listing minus the declared dropped field capabilityEmitted)"
# (c) mutant lane: one-line mutants of the relocated processor must be refused by the relocated contract
mutant() { # label sed-expression
    local label=$1 expression=$2 file=M3MatSortableProcessor.java dir="$SCRATCH/mutant-$1"
    rm -rf "$dir"; mkdir -p "$dir/classes" "$dir/tmp"
    sed "$expression" "$MODULE/src/main/java/$target_dir/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/$target_dir/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -proc:none -cp "$MODULE/target/classes" -d "$dir/classes" "$dir/$file"
    if "$JAVA_BIN/java" "-Djava.io.tmpdir=$dir/tmp" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.M3SortableProcessorContract" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $label ACCEPTED"; status=FAIL
    elif grep -q "AssertionError" "$dir/err.txt"; then
        echo "mutant $label REFUSED: $(grep -m1 -oE "AssertionError.*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $label FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
mutant descending_sign 's/comparison = Integer.compare(0, comparison);/comparison = -comparison;/'
mutant signal_only_zero 's/if (columns.isEmpty() \&\& signal != null) {/if (false) {/'
mutant schema_budget_1280 's/if (members.size() > 128) throw/if (members.size() > 1280) throw/'
mutant duplicate_priority_admitted 's/if (!priorities.add(c.priority())) {/if (false) {/'
mutant declared_edge_dropped 's/if (!edges.add("declared-edge:" + owner + "->" + descriptor)) {/if (false) {/'
mutant nulls_last 's/w.write("        if (left == null) return -1;\\n");/w.write("        if (left == null) return 1;\\n");/'
# (d) baseline: the donor contract against the donor processor (declared RED at the donor head; informational)
if "${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.SortableProcessorContract" > "$SCRATCH/out/donor-contract.txt" 2>&1; then
    echo "donor-contract GREEN $(grep -E 'ASSERTIONS|RESULT' "$SCRATCH/out/donor-contract.txt" | tr -d '\r' | tr '\n' ' ')"
else
    echo "donor-contract BASELINE_RED $(grep -m1 -oE 'AssertionError.*' "$SCRATCH/out/donor-contract.txt" | tr -d '\r' | cut -c1-200)"
fi
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.M3SortableProcessorContract" > "$SCRATCH/out/relocated-contract.txt" 2>&1 \
    && echo "relocated-contract PASS $(tr -d '\r' < "$SCRATCH/out/relocated-contract.txt" | tr '\n' ' ')" \
    || { echo "relocated-contract FAIL"; status=FAIL; tail -5 "$SCRATCH/out/relocated-contract.txt"; }
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
