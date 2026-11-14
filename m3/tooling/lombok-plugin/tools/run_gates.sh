#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# run_gates.sh: re-runs the T0 gates of TEST_PLAN.md; one PASS/FAIL line per gate, exit 1 on any FAIL.
# Env: LOMBOK_JAR  lombok-1.18.48.jar (required; sha1 6858f13541bab505384f07053c5a7b539bbfd3e3)
#      JDK21_HOME  JDK 21 or later (default C:/Program Files/Java/jdk-21; JAVA_HOME is not used: it may name JDK 17)
#      OUT         scratch directory (default ./out-lombok-gates); never on a nearly full drive
#      COM_SYNEXIA optional com.synexia checkout added to the census (files on disk, ripgrep: M3_RG or rg on PATH)
#      PREFETCH=1  fetch the m3/**/*.java blobs in one batch first (blobless clones; not on a shared clone)
#      SKIP_CENSUS=1 print SKIP for census_fixed_point instead of running it: it runs git grep over m3/ at HEAD,
#                  which a shared blobless clone must not do; use only when no m3/**/*.java blob changed
set -u
here=$(cd "$(dirname "$0")" && pwd)
lane=$(dirname "$here")
repo=$(git -C "$lane" rev-parse --show-toplevel)
jdk=${JDK21_HOME:-"C:/Program Files/Java/jdk-21"}
java="$jdk/bin/java"
javap="$jdk/bin/javap"
jar=${LOMBOK_JAR:?set LOMBOK_JAR to lombok-1.18.48.jar}
out=${OUT:-$PWD/out-lombok-gates}
config="$repo/m3/vendor/third_party/rewrite-core/UPSTREAM_LOMBOK.config"
vendor="$repo/m3/vendor/third_party"
expected_diff="rewrite-java	org/openrewrite/java/tree/J.java
rewrite-test	org/openrewrite/test/RewriteTest.java"
fail=0
mkdir -p "$out"
"$java" -version 2>&1 | head -1 | grep -Eq '"(2[1-9]|[3-9][0-9])' || { echo "FAIL jdk: $java is not JDK 21+"; exit 1; }

report() {
    if [ "$2" = 0 ]; then echo "PASS $1"; else echo "FAIL $1"; fail=1; fi
}

if [ "${PREFETCH:-0}" = 1 ]; then
    git -C "$repo" ls-tree -r HEAD m3 | awk '$2=="blob" && $4 ~ /\.java$/ {print $3}' | sort -u \
        | git -C "$repo" -c fetch.negotiationAlgorithm=noop fetch -q origin --no-tags --no-write-fetch-head \
              --recurse-submodules=no --filter=blob:none --stdin
fi

python "$here/lombok_api.py" "$jar" "$javap" "$out/api.tsv" "$out/keys.tsv" > /dev/null \
    && cmp -s "$out/api.tsv" "$lane/LOMBOK_API.tsv" && cmp -s "$out/keys.tsv" "$lane/LOMBOK_CONFIG_KEYS.tsv"
report "lombok_api: LOMBOK-API v1 equals the committed tables" $?

fixture="$here/fixture/CensusFixture.java"
command -v cygpath > /dev/null && fixture=$(cygpath -m "$fixture")
printf 'origin\trev\tmodule\tvariant\tpath\tsha\tstaged\nfixture\t-\tfixture\t-\ttools/fixture/CensusFixture.java\t-\t%s\n' \
    "$fixture" > "$out/fixture_manifest.tsv"
"$java" "$here/LombokCensus.java" "$out/fixture_manifest.tsv" "$lane/LOMBOK_API.tsv" "$out/fixture.tsv" > /dev/null \
    && cmp -s "$out/fixture.tsv" "$here/fixture/EXPECTED_OCCURRENCES.tsv"
report "census_discriminator: fixture rows equal EXPECTED_OCCURRENCES.tsv" $?

if [ "${SKIP_CENSUS:-0}" = 1 ]; then
    echo "SKIP census_fixed_point: SKIP_CENSUS=1"
    echo "SKIP class_combinations: SKIP_CENSUS=1 (it reads the census stage)"
else
    origins=("git:m3jdk=$repo@HEAD@m3/*.java")
    [ -n "${COM_SYNEXIA:-}" ] && origins+=("fs:com.synexia=$COM_SYNEXIA")
    status=0
    for run in 1 2; do
        rm -rf "$out/stage$run"
        python "$here/lombok_census.py" "$out/stage$run" "$out/census$run" "$java" "$lane/LOMBOK_API.tsv" \
            "$lane/LOMBOK_CONFIG_KEYS.tsv" "${origins[@]}" > /dev/null || status=1
    done
    [ "$status" = 0 ] && cmp -s "$out/census1/USAGE_CENSUS.tsv" "$out/census2/USAGE_CENSUS.tsv"
    report "census_fixed_point: two runs byte-identical" $?
    for run in 1 2; do
        python "$here/lombok_combinations.py" "$out/stage$run/OCCURRENCES.tsv" "$out/combinations$run.tsv" \
            > /dev/null || status=1
    done
    [ "$status" = 0 ] && cmp -s "$out/combinations1.tsv" "$out/combinations2.tsv" \
        && { [ -z "${COM_SYNEXIA:-}" ] || cmp -s "$out/combinations1.tsv" "$lane/CLASS_COMBINATIONS.tsv"; }
    report "class_combinations: both stages project alike; with COM_SYNEXIA equal to CLASS_COMBINATIONS.tsv" $?
fi

status=0
for run in 1 2; do
    mkdir -p "$out/patterns$run"
    python "$here/lombok_patterns.py" "$out/pat$run" "$out/patterns$run" "$java" "$jar" "$config" \
        "$lane/USAGE_CENSUS.tsv" "$lane/CLASS_COMBINATIONS.tsv" > /dev/null || status=1
done
report "pattern_coverage: every census annotation group and class combination has a case" $status
cmp -s "$out/patterns1/PATTERNS.tsv" "$out/patterns2/PATTERNS.tsv" && cmp -s "$out/patterns1/PATTERNS.tsv" "$lane/PATTERNS.tsv"
report "pattern_fixed_point: two runs byte-identical and equal to PATTERNS.tsv" $?

python "$here/delombok_differential.py" "$out/d0" "$java" "$jar" "$vendor/rewrite-core" "$vendor/rewrite-java" \
    "$vendor/rewrite-java-21" "$vendor/rewrite-test" "$vendor/rewrite-xml" "$vendor/rewrite-yaml" > /dev/null
same=$(awk -F'\t' '$3=="SAME"' "$out/d0/DIFFERENTIAL.tsv" | wc -l)
diffs=$(awk -F'\t' '$3!="SAME" && NR>1 {print $1"\t"$2}' "$out/d0/DIFFERENTIAL.tsv")
[ "$same" -eq 267 ] && [ "$diffs" = "$expected_diff" ]
report "d0_differential: 267/269 equal, the 2 known differences only" $?

exit $fail
