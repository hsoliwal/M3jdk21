#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Lines with missed instructions or branches from a JaCoCo XML report, with their source text.
# Usage: coverage_missed.sh <jacoco.xml> <source root> [exceptions.tsv]
# Output TSV: file:line, missed instructions, missed branches, covered branches, source (trimmed).
# Lines listed in the exceptions TSV (first column file:line) are printed with status EXCEPTION.
set -euo pipefail
xml=$1; src=$2; exc=${3:-/dev/null}
tr '>' '\n' < "$xml" | awk -v src="$src" -v exc="$exc" '
  BEGIN { FS = "\""; while ((getline l < exc) > 0) { split(l, f, "\t"); ex[f[1]] = 1 } }
  /<package name=/ { pkg = $2 }
  /<sourcefile name=/ { file = pkg "/" $2 }
  /<line nr=/ {
    for (i = 1; i < NF; i += 2) { k = $i; gsub(/[ <]|line|=/, "", k); v[k] = $(i + 1) }
    if (v["mi"] > 0 || v["mb"] > 0) {
      path = src "/" file; n = v["nr"]; text = ""
      if (!(path in seen)) { seen[path] = 1; c = 0; while ((getline t < path) > 0) lines[path, ++c] = t; close(path) }
      text = lines[path, n]; gsub(/^[ \t]+/, "", text)
      key = file ":" n; sub(/^.*\//, "", key)
      print key "\t" v["mi"] "\t" v["mb"] "\t" v["cb"] "\t" ((key in ex) ? "EXCEPTION" : "OPEN") "\t" text
    }
  }'
