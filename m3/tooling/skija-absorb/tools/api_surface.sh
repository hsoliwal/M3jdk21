#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
# api_surface.sh <classes_dir> <out.txt> [sed-script]: javap -public over every class of a classes dir, one sorted
# line per member (owner-qualified), optionally renamed by a sed script so two surfaces compare after a rename.
set -euo pipefail
dir="$1"; out="$2"; ren="${3:-}"
J="${JDK_BIN:-/c/Program Files/Java/jdk-21/bin}"
list=$(mktemp)
(cd "$dir" && find . -name '*.class' | sed 's#^\./##; s#\.class$##; s#/#.#g' | sort) > "$list"
(cd "$dir" && xargs -a "$list" -n 200 "$J/javap" -public -cp . 2>/dev/null) | tr -d '\r' | awk '
  /^Compiled from/ {next}
  /^(public |protected )?.*(class|interface|enum|record) [^ ]+/ && /\{$/ {owner=$0; sub(/ \{$/,"",owner); print owner; next}
  /^\}/ {next}
  {sub(/^ +/,""); print owner " :: " $0}' | { if [ -n "$ren" ]; then sed -f "$ren"; else cat; fi; } | sort -u > "$out"
rm -f "$list"
wc -l < "$out"
