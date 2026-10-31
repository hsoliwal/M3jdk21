#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Test the exact M3JDK21 Geometry source in a separate stock Java21 runner.

This tests the generated *source implementation* but cannot substitute for a
matched native HotSpot build, String dispatch, JNI lifetime or jtreg.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
OWNER = ROOT / "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java"
TEST = Path(__file__).with_name("CodePointRankExtractedOracle.java")
source = OWNER.read_text(encoding="utf-8")
start_anchor = "    static final class Geometry {"
end_anchor = "    private static final class Entry {"
start = source.find(start_anchor)
end = source.find(end_anchor)
if start < 0 or end < start or source.count(start_anchor) != 1:
    sys.exit("M3_CODEPOINT_RANK_EXTRACT_FAIL unexpected source geometry structure")
snippet = source[start:end]
if snippet.count("private static int lowerBound") != 1:
    sys.exit("M3_CODEPOINT_RANK_EXTRACT_FAIL expected one lowerBound")
java = ("import java.util.Objects;\n"
        "final class RankGeometryUnderTest {\n" + snippet + "}\n")

def invoke(argv):
    completed = subprocess.run(argv, check=False)
    if completed.returncode != 0:
        sys.exit(completed.returncode)

with TemporaryDirectory(prefix="m3-codepoint-rank-") as directory:
    work = Path(directory)
    (work / "RankGeometryUnderTest.java").write_text(java, encoding="utf-8")
    invoke(["javac", "--release", "21", "-proc:none",
            "-Xlint:all", "-Werror", "-d", directory,
            str(work / "RankGeometryUnderTest.java"), str(TEST)])
    invoke(["java", "-ea", "-cp", directory, "CodePointRankExtractedOracle"])
