#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compile exact postimage Java owners against a minimal immutable-owner fixture.
Not a full JDK image/JNI qualification.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys
here = Path(__file__).resolve().parent
repo = here.parents[2]
paths = {"M3StringAffixPrecompute.java": "src/java.base/share/classes/java/lang/M3StringAffixPrecompute.java",
         "M3StringRegexRowPrecompute.java": "src/java.base/share/classes/java/lang/M3StringRegexRowPrecompute.java",
         "Strings.java": "src/java.base/share/classes/java/util/Strings.java",
         "M3StringAffixPrecomputeTest.java": "test/jdk/java/lang/String/M3StringAffixPrecomputeTest.java",
         "StringsContractTest.java": "test/jdk/java/util/Strings/StringsContractTest.java"}
def execute(*args):
    result = subprocess.run([str(a) for a in args], check=False)
    if result.returncode: sys.exit(result.returncode)
def load(name):
    return (repo / paths[name]).read_text()
with TemporaryDirectory(prefix="m3-affix-strings-proof-") as directory:
    tmp = Path(directory)
    package = tmp / "com/m3/affix"
    package.mkdir(parents=True)
    affix = load("M3StringAffixPrecompute.java").replace(
        "package java.lang;", "package com.m3.affix;")
    row = load("M3StringRegexRowPrecompute.java").replace(
        "package java.lang;", "package com.m3.affix;")
    owner_anchor = "    int size() { return values.length; }"
    plan_anchor = "        Shape shape() { return shape; }"
    if row.count(owner_anchor) != 1 or row.count(plan_anchor) != 1:
        sys.exit("M3_AFFIX_PROOF_FAIL drifted row owner")
    row = row.replace(owner_anchor,
        "    M3StringAffixPrecompute affixesForProof() { return affixes; }\n" + owner_anchor)
    row = row.replace(plan_anchor,
        "        boolean indexedForProof() { return affix != null && indexedCandidates() != null; }\n" + plan_anchor)
    (package / "M3StringAffixPrecompute.java").write_text(affix)
    (package / "M3StringRegexRowPrecompute.java").write_text(row)
    sources = sorted((here / "proof/com/m3/affix").glob("*.java"))
    execute("javac", "--release", "21", "-proc:none",
            "-Xlint:all,-serial", "-Werror", "-d", tmp,
            *sources, package / "M3StringAffixPrecompute.java",
            package / "M3StringRegexRowPrecompute.java")
    for main in ("AffixIndexProof", "RegexRowProof", "AffixRankBeforeAfter"):
        execute("java", "-Xmx512m", "-cp", tmp, "com.m3.affix." + main)
    utilpath = tmp / "java/util"
    utilpath.mkdir(parents=True)
    (utilpath / "Strings.java").write_text(load("Strings.java"))
    execute("javac", "--patch-module", "java.base=" + str(tmp),
            "-d", tmp, utilpath / "Strings.java")
    testing = tmp / "StringsContractTest.java"
    testing.write_text(load("StringsContractTest.java"))
    execute("javac", "--release", "21", "--patch-module", "java.base=" + str(tmp),
            "-d", tmp, testing)
    execute("java", "--patch-module", "java.base=" + str(tmp),
            "-cp", tmp, "StringsContractTest")
    jtreg = tmp / "M3StringAffixPrecomputeTest.java"
    jtreg.write_text(load("M3StringAffixPrecomputeTest.java"))
    execute("javac", "--release", "21", "-Xlint:all,-serial", "-Werror",
            "-d", tmp, jtreg)
print("M3_AFFIX_STRINGS_JAVA21_PROOF_PASS source_extracted=true full_jdk=false")
