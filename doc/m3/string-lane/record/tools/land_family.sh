#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
#
# LAND_FAMILY v1 (2026-11-17): lands one proven String family as an M3jdk21 PR.
# usage: land_family.sh <leaf-dir> <leaf-name> <key> <Family> <branch> <title> <plan-row-file> <commit-msg-file>
# <leaf-dir> holds String.head.java (master + the family text), <key>_family.txt, the family's tests
# String<Family>FamilyTest.java and String<Family>FamilyDonorTest.java, <key>.spec.tsv, <key>.skipped.tsv,
# <key>.provenance.tsv (member -> donor), NOTES.md, proof/ (runner, harnesses, stub), work/ (proof outputs), proof.out
# and junit.out. Steps: atom ledger of every family text in the head (tools/atom_ledger.py, PROVENANCE.tsv + the
# family's rows), record folder doc/m3/string-lane/record/<leaf-name>, ABSORB_PLAN row (appended after the last Landed
# row; the previous "this PR" row is marked merged by the caller's plan-row file), commit on origin/master with the
# naming gates, push, PR.
set -eu
leaf=$1; name=$2; key=$3; fam=$4; branch=$5; title=$6; planrow=$7; msg=$8
tools=$(cd "$(dirname "$0")" && pwd)
repo=/c/m3-superset/conv/m3jdk21
e=$leaf/edit; r=$e/doc/m3/string-lane/record/$name
rm -rf "$e"; mkdir -p "$r" "$e/src/java.base/share/classes/java/lang" "$e/test/jdk/java/lang/String" "$e/doc/m3/string-lane/record/tools"
cp "$leaf/String.head.java" "$e/src/java.base/share/classes/java/lang/String.java"
cp "$leaf"/String*Test.java "$e/test/jdk/java/lang/String/"
cp "$leaf/${key}_family.txt" "$leaf"/*.spec.tsv "$leaf"/*.skipped.tsv "$leaf/NOTES.md" "$r/"
for f in "$leaf"/*.txt; do case "$(basename "$f")" in *_family.txt|*_base.txt) ;; *) cp "$f" "$r/" ;; esac; done
[ -d "$leaf/tools" ] && mkdir -p "$r/tools" && cp "$leaf"/tools/* "$r/tools/"
for f in "$leaf"/proof/*; do
  case "$f" in *.java) cp "$f" "$r/$(basename "$f").txt" ;; *) cp "$f" "$r/" ;; esac
done
cp "$leaf/proof.out" "$r/PROOF.txt"; cp "$leaf/junit.out" "$r/JUNIT.txt"
# provenance: tools/PROVENANCE.tsv + this family's rows (deduplicated by member)
python -I - "$tools/PROVENANCE.tsv" "$leaf/${key}.provenance.tsv" <<'EOF'
import sys
seen, rows = set(), []
for p in sys.argv[1:3]:
    for line in open(p, encoding="utf-8"):
        f = line.rstrip("\n").split("\t")
        if line.startswith("#") or len(f) < 2 or f[0] in seen:
            continue
        seen.add(f[0]); rows.append(line.rstrip("\n"))
open(sys.argv[1], "w", encoding="utf-8", newline="").write("# member\tdonor API (as proven by the family's oracle)\n" + "\n".join(rows) + "\n")
EOF
mkdir -p "$leaf/ledger"
specs=()
for t in difference_base search_family edge_family class_family compare_family slice_family build_family \
         split_family replace_family case_family; do
  [ -f "$leaf/$t.txt" ] && specs+=("$(echo "$t" | sed 's/_family//; s/_base//' | tr a-z A-Z)=$(cygpath -m "$leaf/$t.txt")")
done
python -I "$tools/atom_ledger.py" "$(cygpath -m "$leaf/String.head.java")" "$(cygpath -m "$tools/PROVENANCE.tsv")" \
  "$(cygpath -m "$leaf/ledger")" -- "${specs[@]}"
cp "$leaf/ledger/ATOMS.tsv" "$leaf/ledger/PATTERNS.tsv" "$r/"
cp "$tools/atom_ledger.py" "$tools/PROVENANCE.tsv" "$e/doc/m3/string-lane/record/tools/"
cd "$repo"
git fetch -q origin master
base=$(git rev-parse origin/master)
git show "$base:doc/m3/string-lane/ABSORB_PLAN.md" > "$e/doc/m3/string-lane/ABSORB_PLAN.md"
python -I - "$(cygpath -m "$e/doc/m3/string-lane/ABSORB_PLAN.md")" "$(cygpath -m "$planrow")" <<'EOF'
import sys
p, rowfile = sys.argv[1:3]
t = open(p, encoding="utf-8", newline="").read()
spec = open(rowfile, encoding="utf-8").read().split("\n---\n")
old, new = spec[0].strip("\n"), spec[1].strip("\n")
assert t.count(old) == 1, "plan anchor not found once"
t = t.replace(old, new)
open(p, "w", encoding="utf-8", newline="").write(t)
EOF
work=$leaf/gw; rm -rf "$work"; mkdir -p "$work"
bash /c/tmp/string-ff/build_and_gate.sh "$base" "$e" "$msg" "$work" | grep -v '^A	' | tail -4
head=$(cat "$work/HEAD_COMMIT")
git diff --stat "$base" "$head" | tail -1
timeout 300 git push -q origin "$head:refs/heads/$branch" 2>&1 | grep -v '^remote: *$' | tail -1 || true
tail -n +3 "$msg" > "$leaf/PR_BODY.md"
printf '\nNot run here: the JDK make build; anything inside an M3JDK VM (CI runs the JUnit tests with -XX:+UseM3StringStorage).\n' >> "$leaf/PR_BODY.md"
gh pr create -R hsoliwal/M3jdk21 --base master --head "$branch" --title "$title" --body-file "$(cygpath -m "$leaf/PR_BODY.md")" 2>&1 | tail -1
