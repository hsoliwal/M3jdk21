#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Gate 5 of recipe null-flow-20261107: mutants of the three relocated owners, each killed by the donor oracles.

Every MUTANT substitutes one anchored line (or a declared tuple of anchored lines) of one relocated owner (each anchor
occurs exactly once), compiles the three
owners of that mutant with the three *Probe oracles (javac --release 21 -proc:none -Xlint:all -Werror) into
<scratch>/mutants/<id>/ and runs the three probe mains; a mutant is KILLED when a probe exits non-zero (its
AssertionError) and SURVIVED when all three print PASS. Prints one line per mutant and MUTANTS killed=k/n; exits 1
unless every mutant is killed. The mutants are chosen for the contracts the reference table test
(M3NullFlowReferenceTableTest) states: the lane clear of the join, the UNKNOWN clause of assume, the two-bit slot
read, change detection of mergeFrom, the FORGET transfer, the entry copy of the solver and the identity (not equality)
of the symbol table.

usage: python -I mutants.py <scratch-dir>     env: JAVA_HOME (a JDK 21)
"""
from __future__ import annotations

import os
import shutil
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
MODULE = RECIPE_DIR.parent.parent
PACKAGE = "jdk.internal.m3.ast"
MAIN = MODULE / "src/main/java" / PACKAGE.replace(".", "/")
TEST = MODULE / "src/test/java" / PACKAGE.replace(".", "/")
OWNERS = ("M3NullFlowFacts.java", "M3NullFlowVariableIds.java", "M3NullFlowWorklist.java")
PROBES = ("M3NullFlowFactsProbe", "M3NullFlowWorklistProbe", "M3NullFlowVariableIdsProbe")
TIMEOUT_SECONDS = 300

# id, owner file, anchor (exactly once) and replacement (or a tuple of anchor/replacement pairs), the contract broken
MUTANTS = (
    ("m1-join-or", "M3NullFlowFacts.java",
     "            words[i] = a & ~(differingLanes | (differingLanes << 1));",
     "            words[i] = a | b;",
     "join table: NULL join NONNULL must forget (the mutant yields the invalid state 3)"),
    ("m2-assume-unknown", "M3NullFlowFacts.java",
     "        if (prior != UNKNOWN && prior != requiredState) {",
     "        if (prior != requiredState) {",
     "assumption table: an UNKNOWN fact accepts either assumption"),
    ("m3-state-one-bit", "M3NullFlowFacts.java",
     "        return (int) ((words[variable >>> 5] >>> shift) & 3L);",
     "        return (int) ((words[variable >>> 5] >>> shift) & 1L);",
     "two-bit slot read: NONNULL (10) must read back as 2"),
    ("m4-merge-no-change", "M3NullFlowFacts.java",
     "            changed |= merged != a;",
     "            changed |= false;",
     "merge table: a forgotten fact is a change (the solver would stop propagating)"),
    ("m5-forget-noop", "M3NullFlowWorklist.java",
     "            case FORGET -> candidate.set(slot, M3NullFlowFacts.UNKNOWN);",
     "            case FORGET -> { }",
     "FORGET transfer must clear the slot"),
    ("m6-entry-alias", "M3NullFlowWorklist.java",
     "        in[entryNode] = entry.copy();",
     "        in[entryNode] = entry;",
     "the entry frame is never modified by solve"),
    ("m7-symbol-equality", "M3NullFlowVariableIds.java",
     ("        int hash = System.identityHashCode(symbol);",
      "        int hash = symbol.hashCode();",
      "        while (array[slot] != null && array[slot] != symbol) {",
      "        while (array[slot] != null && !array[slot].equals(symbol)) {"),
     None,
     "symbol identity, not equality: two equal strings are two variables (the equals probe alone is equivalent in"
     " practice because the slot hash is the identity hash; the mutant must also hash by equality)"),
    ("m8-intern-after-freeze", "M3NullFlowVariableIds.java",
     '        if (frozen) throw new IllegalStateException("method symbol table frozen");',
     "        // frozen check removed",
     "a frozen table refuses intern"),
)


def java_bin(tool: str) -> str:
    home = os.environ.get("JAVA_HOME")
    if not home:
        raise SystemExit("JAVA_HOME must point at a JDK 21")
    return str(Path(home) / "bin" / tool)


def run_mutant(scratch: Path, identity: str, owner: str, anchor: str, replacement: str) -> str:
    work = scratch / "mutants" / identity
    shutil.rmtree(work, ignore_errors=True)
    (work / "src").mkdir(parents=True)
    (work / "classes").mkdir()
    for name in OWNERS:
        text = (MAIN / name).read_text(encoding="utf-8")
        if name == owner:
            pairs = anchor if isinstance(anchor, tuple) else (anchor, replacement)
            for old, new in zip(pairs[0::2], pairs[1::2]):
                if text.count(old) != 1:
                    raise SystemExit("%s: anchor occurs %d times in %s" % (identity, text.count(old), name))
                text = text.replace(old, new)
        (work / "src" / name).write_text(text, encoding="utf-8", newline="\n")
    sources = [str(work / "src" / name) for name in OWNERS] + [str(TEST / (probe + ".java")) for probe in PROBES]
    compiled = subprocess.run([java_bin("javac"), "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                               "-d", str(work / "classes")] + sources, capture_output=True, text=True)
    if compiled.returncode != 0:
        return "COMPILE_ERROR " + compiled.stderr.strip().splitlines()[0]
    verdicts = []
    for probe in PROBES:
        try:
            ran = subprocess.run([java_bin("java"), "-XX:-UsePerfData", "-cp", str(work / "classes"), PACKAGE + "." + probe],
                                 capture_output=True, text=True, timeout=TIMEOUT_SECONDS)
        except subprocess.TimeoutExpired:
            verdicts.append(probe + "=TIMEOUT")
            continue
        verdicts.append(probe + ("=PASS" if ran.returncode == 0 else "=FAIL"))
    killed = any(not verdict.endswith("=PASS") for verdict in verdicts)
    return ("KILLED " if killed else "SURVIVED ") + " ".join(verdicts)


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__.splitlines()[-2])
        return 2
    scratch = Path(argv[1])
    killed = 0
    for identity, owner, anchor, replacement, contract in MUTANTS:
        verdict = run_mutant(scratch, identity, owner, anchor, replacement)
        killed += verdict.startswith("KILLED")
        print("MUTANT %s %s %s (%s)" % (identity, owner, verdict, contract))
    print("MUTANTS killed=%d/%d" % (killed, len(MUTANTS)))
    return 0 if killed == len(MUTANTS) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
