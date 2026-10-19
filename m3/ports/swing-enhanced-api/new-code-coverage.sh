#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
# Line and branch coverage of the new code only (NEW_CODE.txt) from a JaCoCo XML report.
# Usage: new-code-coverage.sh <cov.xml> <NEW_CODE.txt>
xml=$1; list=$2
sed 's/>/>\n/g' "$xml" | awk -v list="$list" '
  BEGIN {
    while ((getline l < list) > 0) { if (l ~ /^#/ || l == "") continue; split(l, p, " "); want[p[1] " " p[2]] = 1 }
  }
  /<class name=/ { match($0, /name="[^"]*"/); cls = substr($0, RSTART + 6, RLENGTH - 7); method = ""; next }
  /<method name=/ { match($0, /name="[^"]*"/); method = substr($0, RSTART + 6, RLENGTH - 7); next }
  /<\/method>/ { method = ""; next }
  /<counter type="(LINE|BRANCH)"/ && method != "" {
    outer = cls; sub(/\$.*/, "", outer)
    if (!((outer " *") in want) && !((cls " " method) in want)) next
    match($0, /type="[A-Z]*"/); t = substr($0, RSTART + 6, RLENGTH - 7)
    match($0, /missed="[0-9]*"/); m = substr($0, RSTART + 8, RLENGTH - 9)
    match($0, /covered="[0-9]*"/); c = substr($0, RSTART + 9, RLENGTH - 10)
    miss[t] += m; cov[t] += c
    if (t == "LINE" && m > 0) printf "missed %d line(s): %s.%s\n", m, cls, method
  }
  END {
    for (t in cov) printf "NEW CODE %s %d/%d (%.1f%%)\n", t, cov[t], cov[t] + miss[t], 100 * cov[t] / (cov[t] + miss[t])
  }'
