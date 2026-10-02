#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal; SPDX-License-Identifier: Apache-2.0
"""Compile and execute the explicit route independently on a stock JDK 21."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

root = Path(__file__).resolve().parent
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
records = [run([jdk / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-d", out,
                *sorted((root / "src").rglob("*.java")), root / "test/M3StringTest.java"])]
for flags in [[], ["-Xint"], ["-XX:-CompactStrings"], ["-Xbatch", "-XX:-TieredCompilation"]]:
    records.append(run([jdk / "bin/java", "-ea", *flags, "-cp", out, "M3StringTest"]))
receipt = {"route": "A-stock-JVM", "records": records,
           "source_hashes": {str(p.relative_to(root)).replace('\\', '/'): hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in sorted((root / "src").rglob("*.java"))}}
(out / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
