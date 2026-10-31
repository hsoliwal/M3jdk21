#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""fdp-delegate gate (c) evidence: which FastDoubleParser classes load while Jackson parses, per build.

  python class_load_trace.py --jdk <jdk21> --run <run_gates.py output dir> [--count N]

Runs the Jackson differential with -Xlog:class+load for the upstream and the overlay build of a run_gates.py output
directory and writes <run>/classload.txt: one line per build listing the FastDoubleParser classes in load order.
The overlay build must load no FastDoubleParser bit parser (JavaDoubleBitsFrom*, FastDoubleMath, ...).
"""
from __future__ import annotations

import argparse
import os
from pathlib import Path
import re
import subprocess
import sys

PACKAGE = "ch.randelshofer.fastdoubleparser."
LOADED = re.compile(re.escape(PACKAGE) + r"([A-Za-z0-9_$]+)")
BIT_PARSERS = re.compile(r"BitsFrom|FastDoubleMath|FastFloatMath")
HARNESS = "M3JacksonFastDoubleDifferential"
VARIANTS = ("upstream", "overlay")
DEFAULT_COUNT = 2000
EXIT_BIT_PARSERS_IN_OVERLAY = 1


def trace(jdk: Path, run: Path, variant: str, count: int) -> list[str]:
    classpath = os.pathsep.join(str(p) for p in (run / "harness-jackson", run / ("jackson-" + variant),
                                                run / f"fdp-{variant}.jar"))
    report = run / f"classload-{variant}.tsv"
    result = subprocess.run([str(jdk / "bin/java"), "-Xlog:class+load=info", "-cp", classpath, HARNESS, str(count),
                             str(report)], capture_output=True, text=True, check=False)
    report.unlink(missing_ok=True)
    names = []
    for name in LOADED.findall(result.stdout):
        if name not in names:
            names.append(name)
    return names


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--count", type=int, default=DEFAULT_COUNT)
    args = parser.parse_args(argv)
    lines, traces = [], {}
    for variant in VARIANTS:
        traces[variant] = trace(args.jdk, args.run, variant, args.count)
        lines.append(f"{variant} build loads {len(traces[variant])} FDP classes: " + " ".join(traces[variant]))
    (args.run / "classload.txt").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print("\n".join(lines))
    return EXIT_BIT_PARSERS_IN_OVERLAY if any(BIT_PARSERS.search(n) for n in traces["overlay"]) else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
