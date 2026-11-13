#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# run_gates.sh: re-runs the CSV-CAPABILITY v1 gates; one PASS/FAIL line per gate, exit 1 on any FAIL.
# Needs the vendored donors checked out (m3/vendor/third_party/{univocity-parsers,commons-csv,jackson-dataformat-csv}).
# Env: OUT scratch directory (default ./out-csv-gates).
set -u
here=$(cd "$(dirname "$0")" && pwd)
lane=$(dirname "$here")
repo=$(git -C "$lane" rev-parse --show-toplevel)
out=${OUT:-$PWD/out-csv-gates}
v1_sha=78e3e80f1f2177e4e6459130a8eb1a93b9c87932ade2458be83a5ff6d7713537
fail=0
mkdir -p "$out"

report() {
    if [ "$2" = 0 ]; then echo "PASS $1"; else echo "FAIL $1"; fail=1; fi
}

[ "$(sha256sum "$lane/inputs/CSV_API_SUPERSET.v1.tsv" | cut -d' ' -f1)" = "$v1_sha" ]
report "v1_input: CSV_API_SUPERSET.v1.tsv is the #2215 API superset v1 (sha256 ${v1_sha:0:16})" $?

python "$here/csv_donor_docs.py" "$repo/m3/vendor/third_party" "$out/methods.tsv" "$out/features.tsv" \
    "$out/extra.tsv" > /dev/null \
    && cmp -s "$out/methods.tsv" "$lane/inputs/DONOR_METHODS.tsv" \
    && cmp -s "$out/features.tsv" "$lane/inputs/DONOR_FEATURES.tsv" \
    && cmp -s "$out/extra.tsv" "$lane/inputs/DONOR_EXTRA.tsv"
report "donor_docs: evidence tables re-derived byte-identical from the vendored sources" $?

status=0
for run in 1 2; do
    python "$here/csv_capabilities.py" "$lane/inputs/CSV_API_SUPERSET.v1.tsv" "$lane/inputs/DONOR_FEATURES.tsv" \
        "$lane/inputs/DONOR_EXTRA.tsv" "$lane/CSV_CAPABILITY_SYNONYMS.tsv" "$out/caps$run.tsv" > "$out/caps$run.log" \
        || status=1
done
report "capability_gate: every donor key mapped exactly once, no unknown key, no empty capability" $status
cmp -s "$out/caps1.tsv" "$out/caps2.tsv" && cmp -s "$out/caps1.tsv" "$lane/CSV_CAPABILITIES.tsv"
report "capability_fixed_point: two runs byte-identical and equal to CSV_CAPABILITIES.tsv" $?

missing=0
for cap in $(awk -F'\t' 'NR>1 {print $1}' "$lane/CSV_CAPABILITIES.tsv"); do
    grep -Fq "| \`$cap\` |" "$lane/INTERFACE.md" || { echo "  no home: $cap"; missing=1; }
done
report "interface_coverage: every capability has a row in INTERFACE.md" $missing

exit $fail
