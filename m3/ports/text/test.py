#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Compile and execute the explicit route independently on a stock JDK 21."""
import hashlib
import json
import os
import platform
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parent
repo = root.parents[2]
shared = root.parent / "indexstring/src/main/java"
jdk = Path(os.environ["M3_JDK"])
out = root / "build"
out.mkdir(exist_ok=True)
def run(argv):
    completed = subprocess.run(list(map(str, argv)), capture_output=True, text=True)
    print(completed.stdout, end="")
    print(completed.stderr, end="", file=sys.stderr)
    completed.check_returncode()
    return {"argv": list(map(str, argv)), "exit": completed.returncode,
            "stdout": completed.stdout, "stderr": completed.stderr}
sources = [*sorted(shared.rglob("*.java")), *sorted((root / "src").rglob("*.java")),
           root / "test/M3StringTest.java"]
records = [run([jdk / "bin/javac", "-J-Xmx256m", "-J-XX:ActiveProcessorCount=2",
                "--release", "21", "-Xlint:all", "-Werror", "-d", out, *sources])]
for flags in [[], ["-Xint"], ["-XX:-CompactStrings"], ["-Xbatch", "-XX:-TieredCompilation"]]:
    records.append(run([jdk / "bin/java", "-Xms32m", "-Xmx256m", "-XX:ActiveProcessorCount=2",
                        "-ea", *flags, "-cp", out, "M3StringTest"]))
receipt = {"route": "A-stock-JVM", "records": records,
           "os": platform.platform(), "jdk": run([jdk / "bin/java", "-version"]),
           "source_root": "repository-relative; shared indexstring owners and explicit facade",
           "source_hashes": {p.relative_to(repo).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in [*sources, Path(__file__).resolve()]}}
(out / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
