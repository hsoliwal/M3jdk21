#!/bin/bash
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
# INTEGRATE_FAMILY v1 (2026-11-17): moves a workflow draft of a String family onto current origin/master and re-proves
# it there. usage: integrate_family.sh <draft-dir> <leaf-dir> <key> <donor-jars>
# Copies the draft's family text, tests, spec/skipped/provenance tables, NOTES.md and proof/; takes every other family
# text from origin/master's String.java (the texts of the previous leaf, verified once in it); writes String.head.java =
# master with the family text appended after the last landed family text; runs the draft's proof/run_proof.sh and
# proof/run_junit.sh; leaves proof.out and junit.out in <leaf-dir>.
set -u
draft=$1; leaf=$2; key=$3; donor=$4
s=/j/m3-scratch/string-converge-20261117
rm -rf "$leaf"; mkdir -p "$leaf/work"
cp -r "$draft/proof" "$leaf/"
cp "$draft/${key}_family.txt" "$draft/NOTES.md" "$leaf/"
cp "$draft"/String*FamilyTest.java "$draft"/String*DonorTest.java "$leaf/" 2>/dev/null
cp "$draft"/*.spec.tsv "$draft"/*.skipped.tsv "$leaf/" 2>/dev/null
cp "$draft"/*.provenance.tsv "$leaf/" 2>/dev/null || true
# data files the draft's proof reads (e.g. CaseFolding.txt) and its generator tools
for f in "$draft"/*.txt; do case "$(basename "$f")" in *_family.txt|*_base.txt|PROOF.txt|JUNIT.txt) ;; *) cp "$f" "$leaf/" ;; esac; done
[ -d "$draft/tools" ] && cp -r "$draft/tools" "$leaf/"
cd /c/m3-superset/conv/m3jdk21 && git fetch -q origin master
git show origin/master:src/java.base/share/classes/java/lang/String.java > "$leaf/String.master.java"
python -I - "$(cygpath -m "$leaf")" "$(cygpath -m "$s")" "$key" <<'PY'
import io, os, sys
leaf, s, key = sys.argv[1:4]
r = lambda p: io.open(p, encoding="utf-8", newline="").read()
head = r(leaf + "/String.master.java")
sources = {"difference_base": s + "/leaf27", "search_family": s + "/leaf27", "edge_family": s + "/leaf27",
           "class_family": s + "/leaf27", "compare_family": s + "/leaf27", "slice_family": s + "/leaf27",
           "build_family": s + "/leaf28", "split_family": s + "/leaf29", "replace_family": s + "/leaf30"}
last_end = 0
for name, d in sources.items():
    text = r(d + "/" + name + ".txt")
    assert head.count(text) == 1, name + " not once in master"
    io.open(leaf + "/" + name + ".txt", "w", encoding="utf-8", newline="").write(text)
    last_end = max(last_end, head.index(text) + len(text))
fam = r(leaf + "/" + key + "_family.txt")
io.open(leaf + "/String.head.java", "w", encoding="utf-8", newline="").write(head[:last_end] + fam + head[last_end:])
print("head = master + " + key + " after the last family text")
PY
bash "$leaf/proof/run_proof.sh" "$(cygpath -m "$leaf")" "$(cygpath -m "$leaf/work")" "$donor" > "$leaf/proof.out" 2>&1
echo "proof exit $?" >> "$leaf/proof.out"
cj=$(cygpath -m ~/.m2/repository/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar)
bash "$leaf/proof/run_junit.sh" "$(cygpath -m "$leaf")" "$(cygpath -m "$leaf/work")" "$cj" > "$leaf/junit.out" 2>&1
echo "junit exit $?" >> "$leaf/junit.out"
grep -E 'RESULT|lint|COMPILE|proof exit' "$leaf/proof.out" | sort | uniq -c
grep -E 'tests (successful|failed)|junit exit' "$leaf/junit.out" | sort | uniq -c
