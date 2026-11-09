#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutation gate of recipe planner-family-20261107: six one-edit mutants of the renamed donor closure (the src-renamed
tree differential.sh leaves) must each make M3IndexPrecomputePlannerDump abort with a reference drift; the unmutated
tree must pass. Mutant trees live in the scratch directory only.

usage: python -I mutants.py <src-renamed> <scratch> <module target/test-classes> <JDK bin> <donor package> <tuple domain>
"""
import os
import shutil
import subprocess
import sys
from pathlib import Path

SRC = Path(sys.argv[1])          # verify-plan/src-renamed (donor package tree with the LITERAL rows applied)
WORK = Path(sys.argv[2])         # scratch for mutant trees
TEST_CLASSES = sys.argv[3]       # module target/test-classes (the dump)
JAVA = sys.argv[4]               # JDK bin directory
PKG, DOMAIN = sys.argv[5], sys.argv[6]
MUTANTS = [
    ("planner-tie-more-bytes", "IndexPrecomputePlanner.java",
     "if (bytes != bestBytes) return bytes < bestBytes;", "if (bytes != bestBytes) return bytes > bestBytes;"),
    ("greedy-negative-closure", "IndexPrecomputePlanner.java",
     "        if (closure.additionalScore <= 0) continue;\n", ""),
    ("no-missing-dependency-check", "IndexPrecomputePlanner.java",
     "          throw new IllegalArgumentException(\n              candidate.capability() + \" depends on missing \" + dependency);",
     "          continue;"),
    ("tuple-capacity-halved", "FrozenLongTupleIndex.java",
     "long required = Math.max(2L, (long) values.length * 2L);", "long required = Math.max(2L, (long) values.length);"),
    ("fallback-arity3-offset", "NativePrecomputeOps.java",
     "queries[offset], queries[offset + 1], queries[offset + 2], defaultValue);",
     "queries[offset], queries[offset + 1], queries[offset + 1], defaultValue);"),
    ("hash-length-prefix", "PrecomputeHashes.java",
     "digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());",
     "digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length + 1).array());"),
]


def run(tree: Path, classes: Path, cases: str) -> str:
    sources = [str(p) for p in tree.rglob("*.java")]
    subprocess.run([JAVA + "/javac", "--release", "21", "-encoding", "UTF-8", "-proc:none", "-nowarn", "-d", str(classes), *sources],
                   check=True, capture_output=True)
    done = subprocess.run([JAVA + "/java", "-Xss1m", "-cp", str(classes) + os.pathsep + TEST_CLASSES,
                           "jdk.m3.precompute.M3IndexPrecomputePlannerDump", PKG, "", DOMAIN, cases],
                          capture_output=True, text=True)
    lines = (done.stderr.strip().splitlines() or [""])
    drift = next((line for line in lines if "AssertionError" in line or "Exception" in line), "")
    return drift[:150] if drift else "SURVIVED " + done.stdout.strip().splitlines()[-1][:80]


killed = 0
for name, file, old, new in MUTANTS:
    tree = WORK / name / "src"
    shutil.rmtree(WORK / name, ignore_errors=True)
    shutil.copytree(SRC, tree)
    target = next(tree.rglob(file))
    text = target.read_text(encoding="utf-8")
    assert text.count(old) == 1, (name, text.count(old))
    target.write_text(text.replace(old, new), encoding="utf-8", newline="\n")
    verdict = run(tree, WORK / name / "classes", "400")
    killed += not verdict.startswith("SURVIVED")
    print("MUTANT %s: %s" % (name, verdict))
print("UNMUTATED: " + run(SRC, WORK / "unmutated-classes", "400"))
print("MUTANTS killed=%d/%d" % (killed, len(MUTANTS)))
sys.exit(0 if killed == len(MUTANTS) else 1)
