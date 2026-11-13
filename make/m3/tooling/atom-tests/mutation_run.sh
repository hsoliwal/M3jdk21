#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
#
# mutation_run.sh <worktree> <out_dir> <MUTANTS.tsv> <MUTATION.tsv> : ATOM-TEST MUTATION RUN v1 (no JDK build).
# Reproduces the discriminator evidence from the repository: every row of MUTANTS.tsv (id, owner file under PKG_DIR,
# exact old text on one line, replacement, test class, description; every field non-empty, since the tab-separated
# fields are read with IFS; @n and @uXXXX are the Java escapes \n and \uXXXX as in mutate_once.py) is applied once
# with mutate_once.py, the named test class runs through run_atom_tests.sh, and the owner file is restored with
# git checkout (also when a step fails). A mutant is KILLED when the class has a failing or erroring test, SURVIVED when
# it passes, NOT_APPLIED when the old text is not found exactly once. Writes MUTATION.tsv: mutant, owner, mutation,
# test_class, tests_failed, tests_total, verdict; one progress line per mutant. Exit 1 unless every mutant is KILLED.
set -uo pipefail
WT=${1:?worktree}; OUT=${2:?out_dir}; MUTANTS=${3:?MUTANTS.tsv}; RESULT=${4:?MUTATION.tsv}
PKG_DIR=${PKG_DIR:-"src/java.base/share/classes/jdk/internal/mindex"}
T="$WT/make/m3/tooling/atom-tests"
# Git Bash would rewrite mutant texts that start with '/' or '//' as paths before python sees them
export MSYS_NO_PATHCONV=1
mkdir -p "$OUT"
printf 'mutant\towner\tmutation\ttest_class\ttests_failed\ttests_total\tverdict\n' > "$RESULT.tmp"
status=0
while IFS=$'\t' read -r id file old new cls desc; do
  { [ -z "$id" ] || [ "$id" = "id" ]; } && continue
  owner="$WT/$PKG_DIR/$file"
  restore() { git -C "$WT" checkout -- "$PKG_DIR/$file"; }
  if ! python "$T/mutate_once.py" "$owner" "$old" "$new" > /dev/null; then
    restore
    printf '%s\t%s\t%s\t%s\t-\t-\tNOT_APPLIED\n' "$id" "${file%.java}" "$desc" "$cls" >> "$RESULT.tmp"
    echo "MUTANT $id NOT_APPLIED"; status=1; continue
  fi
  bash "$T/run_atom_tests.sh" "$WT" "$OUT/$id" "$cls" > "$OUT/$id.log" 2>&1
  restore
  counts=$(python - "$OUT/$id/reports/TEST-junit-jupiter.xml" "$cls" <<'PY'
import sys
import xml.etree.ElementTree as ET
try:
    cases = [c for c in ET.parse(sys.argv[1]).getroot().iter("testcase") if c.get("classname") == sys.argv[2]]
except (OSError, ET.ParseError):
    cases = []
failed = sum(1 for c in cases if c.findall("failure") or c.findall("error"))
print(failed, len(cases))
PY
)
  read -r failed total <<< "$counts"
  if [ "${total:-0}" -eq 0 ]; then verdict=ERROR_NO_TESTS; elif [ "$failed" -gt 0 ]; then verdict=KILLED; else verdict=SURVIVED; fi
  [ "$verdict" = KILLED ] || status=1
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\n' "$id" "${file%.java}" "$desc" "$cls" "$failed" "$total" "$verdict" >> "$RESULT.tmp"
  echo "MUTANT $id $verdict failed=$failed total=$total"
done < "$MUTANTS"
mv "$RESULT.tmp" "$RESULT"
echo "MUTATION mutants=$(($(wc -l < "$RESULT") - 1)) killed=$(awk -F'\t' 'NR>1 && $7=="KILLED"' "$RESULT" | wc -l)"
exit "$status"
