#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate gate (c) evidence: which FastDoubleParser classes load while Jackson parses, per build.

  python class_load_trace.py --jdk <jdk21> --run <run_gates.py output dir> [--count N]

Runs the Jackson differential with -Xlog:class+load for the upstream and the overlay build of a run_gates.py output
directory and writes <run>/classload.json: per build the harness exit code, its report header (numbers, documents,
corpus sha256, java, jar) and the FastDoubleParser classes in load order. PASS when both harness runs exit 0, the
upstream build loads FastDoubleParser bit parsers (JavaDoubleBitsFrom*, FastDoubleMath, ...) and the overlay build
loads none; exit 1 otherwise.
"""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

GATE = "c_class_load_trace"
OUTPUT = "classload.json"
PACKAGE = "ch.randelshofer.fastdoubleparser."
LOADED = re.compile(re.escape(PACKAGE) + r"([A-Za-z0-9_$]+)")
BIT_PARSERS = re.compile(r"BitsFrom|FastDoubleMath|FastFloatMath")
HARNESS = "M3JacksonFastDoubleDifferential"
VARIANTS = ("upstream", "overlay")
DEFAULT_COUNT = 2000
STATUS_PASS = "PASS"
STATUS_FAIL = "FAIL"
EXIT_TRACE_FAIL = 1


def report_header(report: Path) -> dict[str, str]:
    """The harness report's first line (#M3JacksonFastDoubleDifferential key=value ...) as a dict."""
    lines = report.read_text(encoding="utf-8").splitlines() if report.is_file() else []
    fields = lines[0].split("\t")[1:] if lines else []
    return dict(field.split("=", 1) for field in fields if "=" in field)


def trace(jdk: Path, run: Path, variant: str, count: int) -> dict:
    classpath = os.pathsep.join(str(p) for p in (run / "harness-jackson", run / ("jackson-" + variant),
                                                run / f"fdp-{variant}.jar"))
    report = run / f"classload-{variant}.tsv"
    result = subprocess.run([str(jdk / "bin/java"), "-Xlog:class+load=info", "-cp", classpath, HARNESS, str(count),
                             str(report)], capture_output=True, text=True, check=False)
    header = report_header(report)
    report.unlink(missing_ok=True)
    names = []
    for name in LOADED.findall(result.stdout):
        if name not in names:
            names.append(name)
    return {"exit": result.returncode, "report": header, "classes": names}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT)
    args = parser.parse_args(argv)
    builds = {variant: trace(args.jdk, args.run, variant, args.count) for variant in VARIANTS}
    bit_parsers = {variant: [name for name in build["classes"] if BIT_PARSERS.search(name)]
                   for variant, build in builds.items()}
    passed = (all(build["exit"] == 0 for build in builds.values()) and bool(bit_parsers["upstream"])
              and not bit_parsers["overlay"])
    record = {"gate": GATE, "count": args.count, "builds": builds, "bit_parsers": bit_parsers,
              "status": STATUS_PASS if passed else STATUS_FAIL}
    (args.run / OUTPUT).write_text(json.dumps(record, indent=1, sort_keys=True) + "\n", encoding="utf-8",
                                   newline="\n")
    for variant, build in builds.items():
        print(f"{variant} build loads {len(build['classes'])} FDP classes: " + " ".join(build["classes"]))
    print(json.dumps({"gate": GATE, "status": record["status"]}))
    return 0 if passed else EXIT_TRACE_FAIL


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
