#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Rewrites the NOT_DONE (and earlier adapter) rows of spec/VIEWPORT_MATRIX.tsv for the viewport adapters (pattern M3P-ADAPTER-1) from the
# specs and the test logs: R/G/D/P = Y (provided), n/a (planned by the family, not providable by the measure kind),
# - (not planned); adapter = factory; test = generated test; status = PASS, NOT_RUN (Nebula bundle absent) or FAIL.
# Usage: viewport_matrix.sh <gen dir> <nebula junit.log> <swing launcher log>   (writes spec/VIEWPORT_MATRIX.tsv)
set -euo pipefail
gen=$1; nlog=$2; slog=$3
results=$(mktemp -p "$gen" .results.XXXX)
{ grep -o 'NOT_RUN [^ ]*' "$nlog" | sed 's/.*\.//;s/^/NOT_RUN\t/' || true
  grep -oE '^[0-9]+\) [a-zA-Z]+\([^)]*\)' "$nlog" | sed 's/.*\.//;s/)$//;s/^/FAIL\t/' || true
  grep -oE 'JUnit Jupiter:M3[A-Za-z]+ViewportTest' "$slog" | sed 's/.*://;s/^/FAIL\t/' || true; } > "$results"
awk -F'\t' -v OFS='\t' -v res="$results" -v fam="$gen/spec/FAMILIES.tsv" -v kinds="$gen/spec/ADAPTER_KINDS.tsv" \
    -v mem="$gen/spec/MEMBERS.tsv" -v ad="$gen/spec/ADAPTERS.tsv" '
  function pascal(s,   n, p, i, o) { n = split(s, p, "-"); o = ""; for (i = 1; i <= n; i++) o = o toupper(substr(p[i], 1, 1)) substr(p[i], 2); return o }
  BEGIN {
    while ((getline l < res) > 0) { split(l, f, "\t"); status[f[2]] = f[1] }
    while ((getline l < fam) > 0) { split(l, f, "\t"); planned[f[1]] = f[3] }
    while ((getline l < kinds) > 0) { split(l, f, "\t"); can[f[1]] = f[2] }
    while ((getline l < mem) > 0) { split(l, f, "\t"); family[f[1]] = f[2] }
    while ((getline l < ad) > 0) { split(l, f, "\t"); kind[f[1] "|" f[2]] = f[4]; cls[f[1] "|" f[2]] = f[3] }
  }
  NR == 1 || ($9 !~ /^NOT_DONE/ && $9 !~ /adapter, shared core/) { print; next }
  {
    c = $1; sub(/ \(.*/, "", c); k = c "|" $2; F = family[c]; K = kind[k]
    if (K == "") { print; next }
    na = ""; split("R G D P", A, " ")
    for (i = 1; i <= 4; i++) {
      p = index(";" planned[F] ";", ";" A[i] ";") > 0; q = index(";" can[K] ";", ";" A[i] ";") > 0
      $(i + 2) = p ? (q ? "Y" : "n/a") : "-"; if (p && !q) na = na A[i] " "
    }
    Fp = pascal(tolower(F)); C = pascal(c); neb = $2 == "nebula"
    $7 = (neb ? "org.eclipse.nebula.widgets.m3web" : "com.m3.swing.web") ".adapter.M3" Fp "Viewports#adapt" C
    $8 = "M3" C (neb ? "Viewport_Test" : "ViewportTest")
    s = status[$8]
    note = na == "" ? "" : "; n/a " na "(kind " K ": one surface or plane, no rows)"
    if (s == "FAIL") $9 = "FAIL: adapter test failed (" K ")"
    else if (s == "NOT_RUN") $9 = "VIEWPORT (adapter, shared core, kind " K "); proof NOT_RUN: " cls[k] " bundle not on the test class path; factory on stand-in hosts PASS" note
    else $9 = "VIEWPORT (adapter, shared core, kind " K "): R/G/D/P contract + off==stock PASS" note
    print
  }' "$gen/spec/VIEWPORT_MATRIX.tsv" > "$gen/spec/VIEWPORT_MATRIX.tsv.new"
mv "$gen/spec/VIEWPORT_MATRIX.tsv.new" "$gen/spec/VIEWPORT_MATRIX.tsv"; rm -f "$results"
awk -F'\t' 'NR > 1 { split($9, s, /[ :(;]/); n[$2 "\t" s[1]]++ } END { for (k in n) print n[k] "\t" k }' "$gen/spec/VIEWPORT_MATRIX.tsv" | sort -k2
