#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Local warm-effect timing probe; it is not a product performance acceptance gate."""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import os
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--target", required=True, type=Path)
parser.add_argument("--crate", type=Path, default=Path(__file__).resolve().parents[1])
args = parser.parse_args()
root = args.target.resolve(strict=True)
crate = args.crate.resolve(strict=True)
jdk = Path(os.environ["JAVA_HOME"]).resolve(strict=True) / "bin"
# Admit current source binding and exact SDK output bytes before running the probe.
subprocess.run([sys.executable, str(crate / "receive.py"), "--crate", str(crate),
                "--target", str(root), "--check"], check=True)
source_path = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
resources = crate / "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-rope-affix-effects-20261017"
rows = [line.split("\t") for line in (resources / "manifest.tsv").read_text().splitlines()]
row = next(row for row in rows if row[0] == source_path)
before = (resources / "pre" / row[3]).read_bytes()
after = (crate / "target/candidate" / source_path).read_bytes()
if hashlib.sha256(before).hexdigest() != row[1] or hashlib.sha256(after).hexdigest() != row[2]:
    raise ValueError("probe source identity differs from qualified recipe")
spec = importlib.util.spec_from_file_location("source_support", root / "m3/runtime-integration/regex-plan-amalgamation/source_extracted_java21.py")
support = importlib.util.module_from_spec(spec)
spec.loader.exec_module(support)
with tempfile.TemporaryDirectory(prefix="m3-affix-probe-") as directory:
    work = Path(directory)
    for data, name in [(before, "RegexBefore"), (after, "RegexAfter")]:
        (work / (name + ".java")).write_text(support.rewrite_engine(data.decode("utf-8"), name))
    (work / "M3String.java").write_text(support.fixture_owner())
    # The byte-exact existing owner fixture intentionally groups package-private
    # owners in M3String.java; use the same fixture-only lint selection as its runner.
    subprocess.run([str(jdk / "javac"), "--release", "21", "-proc:none", "-Xlint:all,-auxiliaryclass", "-Werror",
                    "-d", directory, *map(str, work.glob("*.java")), str(Path(__file__).with_name("WarmProbe.java"))], check=True)
    subprocess.run([str(jdk / "java"), "-Xms256m", "-Xmx256m", "-cp", directory, "WarmProbe"], check=True)
