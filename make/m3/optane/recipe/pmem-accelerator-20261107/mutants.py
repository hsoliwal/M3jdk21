#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Gate 5 of recipe pmem-accelerator-20261107: mutants of the relocated CPU-list parser, each killed by the reference
model test M3NumaTopologyCpuListTest (the hand-derived donor table, the formatter round trip, the regex grammar fuzz).

Every MUTANT substitutes one anchored line of M3NumaTopology.java (the anchor occurs exactly once), compiles the mutant
with the test (javac --release 21 -proc:none -Xlint:all -Werror against the JUnit console launcher jar) into
<scratch>/mutants/<id>/ and runs the launcher on that test class; KILLED when a test fails, SURVIVED when all pass.
Prints one line per mutant and MUTANTS killed=k/n; exits 1 unless every mutant is killed.

usage: python -I mutants.py <scratch-dir>     env: JAVA_HOME (a JDK 21), JUNIT_CONSOLE (junit-platform-console-standalone-1.11.4.jar)
"""
from __future__ import annotations

import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
MODULE = RECIPE_DIR.parent.parent
PACKAGE = "jdk.internal.m3.optane"
OWNER = MODULE / "src/main/java" / PACKAGE.replace(".", "/") / "M3NumaTopology.java"
TEST = MODULE / "src/test/java" / PACKAGE.replace(".", "/") / "M3NumaTopologyCpuListTest.java"
TEST_CLASS = PACKAGE + ".M3NumaTopologyCpuListTest"
SEP = ";" if os.name == "nt" else ":"

# id, anchor (exactly once), replacement, the contract the mutant breaks
MUTANTS = (
    ("n1-inverted-range",
     '                if (digits == 0 || high < low) throw new IllegalArgumentException("invalid CPU range");',
     '                if (digits == 0) throw new IllegalArgumentException("invalid CPU range");',
     "2-1 must refuse"),
    ("n2-bound-65536",
     '                if (low > (65535 - digit) / 10) throw new IllegalArgumentException("CPU index exceeds 65535");',
     '                if (low > (65536 - digit) / 10) throw new IllegalArgumentException("CPU index exceeds 65535");',
     "65536 must refuse"),
    ("n3-trailing-separator",
     '            if (++index == n) throw new IllegalArgumentException("trailing CPU-list separator");',
     '            ++index;',
     "1, must refuse"),
    ("n4-no-plus",
     "            if (cpus.charAt(index) == '+') index++;\n            int digits = 0;",
     "            int digits = 0;",
     "+1,1-3 must parse (the original plus syntax)"),
    ("n5-any-separator",
     "            if (index < n && cpus.charAt(index) != ',') {",
     "            if (false) {",
     "1a and 0 - 2 must refuse"),
    ("n6-exclusive-high",
     "            out.set(low, high + 1);",
     "            out.set(low, high);",
     "0-3 has four CPUs"),
)


def java_bin(tool: str) -> str:
    home = os.environ.get("JAVA_HOME")
    if not home:
        raise SystemExit("JAVA_HOME must point at a JDK 21")
    return str(Path(home) / "bin" / tool)


def run_mutant(scratch: Path, console: str, identity: str, anchor: str, replacement: str) -> str:
    work = scratch / "mutants" / identity
    shutil.rmtree(work, ignore_errors=True)
    (work / "src").mkdir(parents=True)
    (work / "classes").mkdir()
    text = OWNER.read_text(encoding="utf-8")
    if text.count(anchor) != 1:
        raise SystemExit("%s: anchor occurs %d times" % (identity, text.count(anchor)))
    (work / "src" / OWNER.name).write_text(text.replace(anchor, replacement), encoding="utf-8", newline="\n")
    compiled = subprocess.run([java_bin("javac"), "--release", "21", "-proc:none", "-Xlint:all", "-Werror", "-cp", console,
                               "-d", str(work / "classes"), str(work / "src" / OWNER.name), str(TEST)],
                              capture_output=True, text=True)
    if compiled.returncode != 0:
        return "COMPILE_ERROR " + compiled.stderr.strip().splitlines()[0]
    ran = subprocess.run([java_bin("java"), "-XX:-UsePerfData", "-jar", console, "execute", "--class-path", str(work / "classes"),
                          "--select-class", TEST_CLASS, "--details=summary", "--disable-banner"],
                         capture_output=True, text=True, timeout=600)
    failed = re.search(r"(\d+) tests failed", ran.stdout)
    successful = re.search(r"(\d+) tests successful", ran.stdout)
    counts = "successful=%s failed=%s" % (successful.group(1) if successful else "?", failed.group(1) if failed else "?")
    if failed and int(failed.group(1)) > 0:
        return "KILLED " + counts
    return "SURVIVED " + counts


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__.splitlines()[-2])
        return 2
    console = os.environ.get("JUNIT_CONSOLE")
    if not console:
        raise SystemExit("JUNIT_CONSOLE must name junit-platform-console-standalone-1.11.4.jar")
    scratch = Path(argv[1])
    killed = 0
    for identity, anchor, replacement, contract in MUTANTS:
        verdict = run_mutant(scratch, console, identity, anchor, replacement)
        killed += verdict.startswith("KILLED")
        print("MUTANT %s %s (%s)" % (identity, verdict, contract))
    print("MUTANTS killed=%d/%d" % (killed, len(MUTANTS)))
    return 0 if killed == len(MUTANTS) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
