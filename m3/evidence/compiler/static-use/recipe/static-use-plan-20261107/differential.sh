#!/usr/bin/env bash
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# Differential gate of recipe static-use-plan-20261107 (after mvn package of m3/compiler/static-use; JAVA_HOME = JDK 21).
# The donor closure (every Java FILE row of RELOCATION_MAP.tsv that is not a JUnit test: M3StaticUsePlan,
# M3StaticFactsProcessor, CompilerOwnershipProof) and M3StaticUsePlanDump (test scope, compiled a second time with only
# its package line replaced) are compiled from the donor git tree with javac --release 21 -g -proc:none -Xlint:all
# -Werror.
# (a) Donor and relocated dumps (18,432 facts against the reference model, loop-bound shapes, growth sampling,
#     annotation retention), default JIT and -Xint, must be byte-identical.
# (b) The donor JUnit 5 test, compiled from the donor tree, runs through junit-platform-console-standalone against the
#     donor class (the relocated twin runs in the module's test phase).
# (c) Mutant lane: one-line mutants of the relocated owners, compiled into the scratch and shadowing them, must be
#     refused: plan mutants by the dump's reference model, processor mutants by the relocated CompilerOwnershipProof.
# (d) javap -p of every class of both owners (nested and anonymous classes included), package names masked:
#     donor = relocated.
# (e) Processor oracle (leaf 2): M3StaticFactsProcessorOracle (test scope; it reaches the processor by class name) compiles
#     one fixture corpus through the donor processor and through the relocated processor; the emitted companion sources,
#     ledgers and javac diagnostics must be byte-identical after masking the two declared differences (the annotation
#     package and the SHA-256 of each fixture text that names it; the oracle checks every hash against its exact source).
# (f) The donor proof CompilerOwnershipProof (52 javac compilations through the processor) prints the same assertion and
#     compiler-invocation counts on both sides.
# usage: differential.sh <donor-git-dir> <donor-rev> <scratch-dir> <junit-platform-console-standalone jar>
set -euo pipefail
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=';'; winpath() { (cd "$1" && pwd -W); } ;; *) SEP=':'; winpath() { (cd "$1" && pwd); } ;; esac
RECIPE_DIR=$(winpath "$(dirname "$0")")
MODULE=$(winpath "$(dirname "$0")/../..")
DONOR_GIT=$1
REV=$2
CONSOLE=$4
rm -rf "$3/src" "$3/classes" "$3/test-src" "$3/test-classes" "$3/out" "$3/oracle-cp" "$3/work" "$3"/mutant-*
mkdir -p "$3/src" "$3/classes" "$3/test-src" "$3/test-classes" "$3/out" "$3/tmp" "$3/oracle-cp" "$3/work"
SCRATCH=$(winpath "$3")
JAVA_BIN="${JAVA_HOME:?JAVA_HOME must point at a JDK 21}/bin"
JAVA=("$JAVA_BIN/java" "-Djava.io.tmpdir=$SCRATCH/tmp")
MAP="$RECIPE_DIR/RELOCATION_MAP.tsv"
target_package=$(awk -F'\t' '$1=="PACKAGE" {print $3; exit}' "$MAP")
donor_package=$(awk -F'\t' '$1=="PACKAGE" {print $2; exit}' "$MAP")
target_dir=${target_package//.//}
donor_dir=${donor_package//.//}
donor_commit=$(git -C "$DONOR_GIT" rev-parse "$REV^{commit}")
RELOCATED_CP="$MODULE/target/classes$SEP$MODULE/target/test-classes"
mapfile -t donor_files < <(awk -F'\t' '$1=="FILE" && $2 ~ /\.java$/ {print $2}' "$MAP")
closure=(); tests=()
for path in "${donor_files[@]}"; do
    body=$(git -C "$DONOR_GIT" show "$REV:$path")
    case "$body" in
        *"org.junit"*) printf '%s\n' "$body" > "$SCRATCH/test-src/$(basename "$path")"; tests+=("$(basename "$path" .java)") ;;
        *) printf '%s\n' "$body" > "$SCRATCH/src/$(basename "$path")"; closure+=("$(basename "$path" .java)") ;;
    esac
done
dump_src="$MODULE/src/test/java/$target_dir/M3StaticUsePlanDump.java"
[ "$(grep -c "^package $target_package;\$" "$dump_src")" = 1 ] || { echo "dump package line not found exactly once"; exit 1; }
sed "s/^package $target_package;\$/package $donor_package;/" "$dump_src" > "$SCRATCH/src/M3StaticUsePlanDump.java"
"$JAVA_BIN/javac" --release 21 -g -proc:none -Xlint:all -Werror -d "$SCRATCH/classes" "$SCRATCH"/src/*.java
echo "DONOR $donor_commit closure=${closure[*]} (+ dump driver) tests=${tests[*]}"
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
# (b) donor JUnit tests through the console launcher
"$JAVA_BIN/javac" --release 21 -proc:none -Xlint:all -Werror -cp "$SCRATCH/classes$SEP$CONSOLE" -d "$SCRATCH/test-classes" \
    "$SCRATCH"/test-src/*.java
selects=(); for t in "${tests[@]}"; do selects+=(--select-class "$donor_package.$t"); done
if "${JAVA[@]}" -jar "$CONSOLE" execute --disable-banner --details=summary \
        --class-path "$SCRATCH/classes$SEP$SCRATCH/test-classes" "${selects[@]}" > "$SCRATCH/out/donor-junit.txt" 2>&1; then
    echo "donor-junit PASS $(grep -E 'tests (successful|failed)' "$SCRATCH/out/donor-junit.txt" | tr -s ' []' ' ' | tr -d '\r' | tr '\n' ' ')"
else
    echo "donor-junit FAIL"; status=FAIL; tail -20 "$SCRATCH/out/donor-junit.txt"
fi
# (c) mutant lane
mutant() { # label file sed-expression expected-text oracle [oracle args]
    local label=$1 file=$2 expression=$3 expected=$4 oracle=$5 dir="$SCRATCH/mutant-$1"
    shift 5
    rm -rf "$dir"; mkdir -p "$dir/classes"
    sed "$expression" "$MODULE/src/main/java/$target_dir/$file" > "$dir/$file"
    if cmp -s "$dir/$file" "$MODULE/src/main/java/$target_dir/$file"; then echo "mutant $label NOT_APPLIED"; status=FAIL; return; fi
    "$JAVA_BIN/javac" --release 21 -proc:none -cp "$MODULE/target/classes" -d "$dir/classes" "$dir/$file"
    if "${JAVA[@]}" -cp "$dir/classes$SEP$RELOCATED_CP" "$target_package.$oracle" "$@" > "$dir/out.txt" 2> "$dir/err.txt"; then
        echo "mutant $label ACCEPTED"; status=FAIL
    elif grep -q "$expected" "$dir/err.txt"; then
        echo "mutant $label REFUSED: $(grep -m1 -oE "AssertionError.*" "$dir/err.txt" | tr -d '\r' | cut -c1-140)"
    else
        echo "mutant $label FAILED_OTHERWISE: $(head -c 300 "$dir/err.txt")"; status=FAIL
    fi
}
mutant published_not_materialized M3StaticUsePlan.java 's/if (facts.needsConcreteJavaObject() || facts.escape() == Escape.PUBLISHED) {/if (facts.needsConcreteJavaObject()) {/' "model disagreement" M3StaticUsePlanDump
mutant exclusive_bound_floor M3StaticUsePlan.java 's/: (distance + magnitude - 1) \/ magnitude;/: distance \/ magnitude;/' "model disagreement" M3StaticUsePlanDump
mutant growth_sample_ge M3StaticUsePlan.java 's/return required > physicalCapacity;/return required >= physicalCapacity;/' "model disagreement" M3StaticUsePlanDump
mkdir -p "$SCRATCH/work/mutants"
mutant paren_single_level M3StaticFactsProcessor.java 's/while (expression instanceof ParenthesizedTree parentheses) expression = parentheses.getExpression();/if (expression instanceof ParenthesizedTree parentheses) expression = parentheses.getExpression();/' \
    "AssertionError" CompilerOwnershipProof "$SCRATCH/work/mutants/paren"
mutant loop_budget_129 M3StaticFactsProcessor.java 's/if (loops.size() == 128) throw/if (loops.size() == 129) throw/' \
    "AssertionError" CompilerOwnershipProof "$SCRATCH/work/mutants/budget"
mutant mutated_param_bound M3StaticFactsProcessor.java '/parameters.get(id.getName().toString()) == TypeKind.INT$/{n;s/\&\& !mutated.contains(id.getName().toString())) {/\&\& true) {/}' \
    "AssertionError" CompilerOwnershipProof "$SCRATCH/work/mutants/param"
mutant source_hash_stripped M3StaticFactsProcessor.java 's/sourceHash = digest(source.toString());/sourceHash = digest(source.toString().strip());/' \
    "AssertionError" CompilerOwnershipProof "$SCRATCH/work/mutants/hash"
# (d) javap -p, package names masked
mask() { sed -e "s/$donor_package\.//g" -e "s/$target_package\.//g" | tr -d '\r'; }
javap_ok=0; total=0
for file in "$SCRATCH/classes/$donor_dir"/M3StaticUsePlan*.class "$SCRATCH/classes/$donor_dir"/M3StaticFactsProcessor*.class; do
    cls=$(basename "$file" .class)
    case "$cls" in M3StaticUsePlanDump*) continue ;; esac
    total=$((total + 1))
    "$JAVA_BIN/javap" -p -cp "$SCRATCH/classes" "$donor_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-donor-$cls.txt"
    "$JAVA_BIN/javap" -p -cp "$MODULE/target/classes" "$target_package.$cls" | grep -v '^Compiled from' | mask > "$SCRATCH/out/javap-relocated-$cls.txt" 2>/dev/null || true
    if cmp -s "$SCRATCH/out/javap-donor-$cls.txt" "$SCRATCH/out/javap-relocated-$cls.txt"; then javap_ok=$((javap_ok + 1)); else echo "javap $cls DIFFERENT"; status=FAIL; fi
done
echo "javap classes=$total identical=$javap_ok"
# (e) processor oracle over one fixture corpus: donor processor vs relocated processor
mkdir -p "$SCRATCH/oracle-cp/$target_dir"
cp "$MODULE/target/test-classes/$target_dir/M3StaticFactsProcessorOracle.class" "$SCRATCH/oracle-cp/$target_dir/"
"${JAVA[@]}" -cp "$SCRATCH/oracle-cp$SEP$SCRATCH/classes" "$target_package.M3StaticFactsProcessorOracle" \
    "$donor_package.M3StaticFactsProcessor" "$SCRATCH/work/oracle-donor" > "$SCRATCH/out/donor-processor-oracle.txt"
"${JAVA[@]}" -cp "$SCRATCH/oracle-cp$SEP$MODULE/target/classes" "$target_package.M3StaticFactsProcessorOracle" \
    "$target_package.M3StaticFactsProcessor" "$SCRATCH/work/oracle-relocated" > "$SCRATCH/out/relocated-processor-oracle.txt"
verdict processor-oracle "$SCRATCH/out/donor-processor-oracle.txt" "$SCRATCH/out/relocated-processor-oracle.txt"
grep -E "^(CORPUS|DIAG|LEDGER|CHECKS|RESULT)" "$SCRATCH/out/relocated-processor-oracle.txt" | tr -d '\r' | cut -c1-200
echo "processor-oracle generated=$(grep -c '^GENERATED' "$SCRATCH/out/relocated-processor-oracle.txt") diagnostics=$(grep -c '^DIAG' "$SCRATCH/out/relocated-processor-oracle.txt")"
# (f) donor proof on both sides
"${JAVA[@]}" -cp "$SCRATCH/classes" "$donor_package.CompilerOwnershipProof" "$SCRATCH/work/proof-donor" > "$SCRATCH/out/donor-proof.txt"
"${JAVA[@]}" -cp "$RELOCATED_CP" "$target_package.CompilerOwnershipProof" "$SCRATCH/work/proof-relocated" > "$SCRATCH/out/relocated-proof.txt"
verdict CompilerOwnershipProof "$SCRATCH/out/donor-proof.txt" "$SCRATCH/out/relocated-proof.txt"
tr -d '\r' < "$SCRATCH/out/relocated-proof.txt"
echo "DIFFERENTIAL $status"
[ "$status" = PASS ]
