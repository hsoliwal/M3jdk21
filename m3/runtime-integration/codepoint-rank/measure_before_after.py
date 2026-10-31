#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compare exact before/after product Geometry sources on the same Java21 VM.

Benchmark measures warmed Geometry.offsetByCodePoints only. It is NOT a
full matched M3JDK21 image benchmark, JNI/GC check, nor JMH confidence interval.
"""
from hashlib import sha256
from pathlib import Path
from tempfile import TemporaryDirectory
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
TARGET = "src/java.base/share/classes/java/lang/M3StringCodePointPrecompute.java"
BASE_COMMIT = "970a34350ebdf6a3f1c5ff5acc8445d0f3515f55"
BASE_SHA = "50e1fe55cd5e8a53b72aa8a4772f826f712c7e3edb3239514f9504819a0b0257"
HEAD_SHA = "43f65f6e578dc5b3ad59046a52ea70bffae3bbff855c861bc2ffd24d1f4bf3d2"
BENCH = Path(__file__).with_name("CodePointRankBeforeAfterBench.java")
START = "    static final class Geometry {"
END = "    private static final class Entry {"

def run(command, **kwargs):
    result = subprocess.run(command, check=False, **kwargs)
    if result.returncode:
        sys.exit(result.returncode)
    return result

def owner_source_before():
    result = run(["git", "-C", str(ROOT), "show", f"{BASE_COMMIT}:{TARGET}"],
                 capture_output=True)
    return result.stdout

def java_source(blob, owner, expected):
    actual = sha256(blob).hexdigest()
    if actual != expected:
        sys.exit(f"M3_BA_SOURCE_DRIFT owner={owner} sha256={actual} expected={expected}")
    source = blob.decode("utf-8", "strict")
    if source.count(START) != 1 or source.count(END) != 1:
        sys.exit(f"M3_BA_SOURCE_EXTRACT_FAIL owner={owner}")
    start = source.index(START)
    end = source.index(END, start)
    geometry = source[start:end]
    if geometry.count("private static int lowerBound") != 1:
        sys.exit(f"M3_BA_SOURCE_EXTRACT_FAIL lowerBound={owner}")
    return "import java.util.Objects;\nfinal class " + owner + " {\n" + geometry + "}\n"

def main():
    baseline = owner_source_before()
    candidate = (ROOT / TARGET).read_bytes()
    before = java_source(baseline, "RankGeometryBaseline", BASE_SHA)
    after = java_source(candidate, "RankGeometryCandidate", HEAD_SHA)
    if before == after:
        sys.exit("M3_BA_SOURCE_EXTRACT_FAIL no changed methods")
    with TemporaryDirectory(prefix="m3-rank-before-after-") as directory:
        work = Path(directory)
        old = work / "RankGeometryBaseline.java"
        new = work / "RankGeometryCandidate.java"
        old.write_text(before, encoding="utf-8")
        new.write_text(after, encoding="utf-8")
        run(["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
             "-d", directory, str(old), str(new), str(BENCH)])
        run(["java", "-Xms256m", "-Xmx256m", "-cp", directory,
             "CodePointRankBeforeAfterBench"], env=os.environ.copy())
    print("M3_CODEPOINT_BEFORE_AFTER_SOURCE_PASS exact_old_and_new=true"
          " matched_jdk=false warm_geometry_only=true")

if __name__ == "__main__":
    main()
