#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Standalone scoped Java21 source gate for M3 CPU wave6, not installed JMOD proof."""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path

EXPECTED = {
    "M3WeightedGraphContract": 326_118,
    "M3SuffixNumericContract": 1_051_386,
}
CLASSES = ["M3ShortestPaths", "M3RollbackUnionFind", "M3SuffixArray", "M3FenwickLong"]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--pack", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    pack, output = args.pack.resolve(strict=True), args.output.absolute()
    if output.exists() or output.is_relative_to(pack):
        raise ValueError("fresh output directory outside source required")
    owner = pack / "src/jdk.m3.tools.cpu/jdk/m3/tools/cpu"
    fixtures = pack / "fixtures"
    source = [owner / (name + ".java") for name in CLASSES]
    source += [fixtures / (name + ".java") for name in EXPECTED]
    if not all(p.is_file() for p in source):
        raise ValueError("source closure incomplete")
    with tempfile.TemporaryDirectory(prefix="m3-wave6-javac-") as work:
        classes = Path(work) / "classes"
        classes.mkdir()
        compiled = subprocess.run(["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
                                   "-d", str(classes), *(str(p) for p in source)],
                                  capture_output=True, text=True, timeout=90)
        if compiled.returncode or compiled.stderr:
            raise RuntimeError("Java21 strict compilation failed: " + compiled.stderr)
        results = {}
        for mode in ("jit", "interpreted"):
            for name, count in EXPECTED.items():
                command = ["java", "-Xcheck:jni"]
                if mode == "interpreted":
                    command.append("-Xint")
                command += ["-cp", str(classes), name]
                run = subprocess.run(command, capture_output=True, text=True, timeout=180)
                fragment = "M3_WAVE6_GRAPH_PERSISTENCE" if name.startswith("M3Weighted") else "M3_WAVE6_TEXT_NUMERIC"
                expected = fragment + "_PASS checks=" + str(count)
                if run.returncode or run.stdout.strip() != expected or run.stderr.strip():
                    raise RuntimeError(mode + "/" + name + ": " + run.stdout + run.stderr)
                results[mode + "/" + name] = expected
    output.mkdir(parents=True)
    receipt = {"schema": "m3-cpu-wave6-standalone/1", "status": "STRICT_JAVA21_JIT_XINT_PASS",
               "checks_per_mode": sum(EXPECTED.values()), "executions": results,
               "jdk_image_jmod": "NOT_RUN", "openrewrite_sdk": "NOT_RUN",
               "complete_91_tool_families": False}
    (output / "REPORT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"status": receipt["status"], "checks_per_mode": receipt["checks_per_mode"]}))

if __name__ == "__main__":
    main()
