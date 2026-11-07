#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutation gate of recipe planner-cycle-fix-20261107: one-edit mutants of the patched collect() in the module's own
M3IndexPrecomputePlanner (a copy of src/main/java in the scratch directory, never the module) must each make
M3IndexPrecomputePlannerDump in strict mode abort (reference drift, stack overflow, or the heap/time bound of a
closure that never ends); the unmutated copy must pass. Exit 1 when a mutant survives.

usage: python -I mutants.py <scratch> <module target/test-classes> <JDK bin> <package> <tuple domain> [CASES=400]
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
MODULE = HERE.parents[1]
WORK, TEST_CLASSES, JAVA, PKG, DOMAIN = Path(sys.argv[1]), sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5]
CASES = sys.argv[6] if len(sys.argv) > 6 else "400"
TARGET = "M3IndexPrecomputePlanner.java"
SECONDS = 120
MUTANTS = [
    ("recursive-donor-closure", (HERE / "edits" / "E3.new.txt").read_text(encoding="utf-8"),
     (HERE / "edits" / "E3.old.txt").read_text(encoding="utf-8")),
    ("no-visited-check", "      if (already.contains(next) || result.contains(next)) continue;\n",
     "      if (already.contains(next)) continue;\n"),
    ("closure-through-chosen", "      if (already.contains(next) || result.contains(next)) continue;\n",
     "      if (result.contains(next)) continue;\n"),
    ("dependencies-not-followed", "        pending.push(dependency);\n", "        result.isEmpty();\n"),
]


def run(tree: Path, classes: Path) -> str:
    sources = [str(p) for p in tree.rglob("*.java")]
    subprocess.run([JAVA + "/javac", "--release", "21", "-encoding", "UTF-8", "-proc:none", "-nowarn", "-d", str(classes),
                    *sources], check=True, capture_output=True)
    try:
        done = subprocess.run([JAVA + "/java", "-Xss1m", "-Xmx256m", "-cp", str(classes) + os.pathsep + TEST_CLASSES,
                               PKG + ".M3IndexPrecomputePlannerDump", PKG, "M3", DOMAIN, CASES, "strict"],
                              capture_output=True, text=True, timeout=SECONDS)
    except subprocess.TimeoutExpired:
        return "KILLED by the %d s bound (closure never ends)" % SECONDS
    lines = done.stderr.strip().splitlines() or [""]
    failure = next((line for line in lines if "Error" in line or "Exception" in line), "")
    return failure[:160] if failure else "SURVIVED " + done.stdout.strip().splitlines()[-1][:120]


killed = 0
for name, old, new in MUTANTS:
    tree = WORK / name / "src"
    shutil.rmtree(WORK / name, ignore_errors=True)
    shutil.copytree(MODULE / "src" / "main" / "java", tree)
    target = next(tree.rglob(TARGET))
    text = target.read_text(encoding="utf-8")
    assert text.count(old) == 1, (name, text.count(old))
    target.write_text(text.replace(old, new), encoding="utf-8", newline="\n")
    verdict = run(tree, WORK / name / "classes")
    killed += not verdict.startswith("SURVIVED")
    print("MUTANT %s: %s" % (name, verdict))
unmutated = WORK / "unmutated"
shutil.rmtree(unmutated, ignore_errors=True)
shutil.copytree(MODULE / "src" / "main" / "java", unmutated / "src")
print("UNMUTATED: " + run(unmutated / "src", unmutated / "classes"))
print("MUTANTS killed=%d/%d" % (killed, len(MUTANTS)))
sys.exit(0 if killed == len(MUTANTS) else 1)
