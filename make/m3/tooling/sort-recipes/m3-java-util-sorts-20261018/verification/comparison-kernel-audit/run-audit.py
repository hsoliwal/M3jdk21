#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Replay sealed comparison-kernel evidence with explicit audit-only Java fixtures."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def run(command, log):
    result = subprocess.run([str(part) for part in command], text=True,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log.write_text(result.stdout, encoding="utf-8")
    if result.returncode:
        raise SystemExit("Audit command failed; inspect " + str(log))
    return result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java-home", type=Path)
    parser.add_argument("--build-dir", type=Path)
    args = parser.parse_args()
    audit = Path(__file__).resolve().parent
    recorded = json.loads((audit / "recorded-audit.json").read_text(encoding="utf-8"))
    for relative, expected in recorded["file_sha256"].items():
        path = (audit / relative).resolve()
        if sha256(path) != expected:
            raise SystemExit("Recorded audit input drift: " + relative)
    java_home = args.java_home or (Path(os.environ["JAVA_HOME"])
                                  if "JAVA_HOME" in os.environ else None)
    java = java_home / "bin/java" if java_home else shutil.which("java")
    javac = java_home / "bin/javac" if java_home else shutil.which("javac")
    if not java or not javac:
        raise SystemExit("Java 21 JDK required; set --java-home or JAVA_HOME")
    build = args.build_dir.resolve() if args.build_dir else Path(
            tempfile.mkdtemp(prefix="m3-comparison-kernel-audit-"))
    if build.exists() and any(build.iterdir()):
        raise SystemExit("Use a new or empty build directory: " + str(build))
    build.mkdir(parents=True, exist_ok=True)
    version = run([javac, "-version"], build / "compiler-version.txt").strip()
    if not version.startswith("javac 21"):
        raise SystemExit("Java 21 compiler required; observed " + version)
    run([java, "-version"], build / "runtime-version.txt")
    source = build / "source"
    package = source / "java/util"
    package.mkdir(parents=True)
    classes = build / "classes"
    classes.mkdir()
    for fixture in sorted((audit / "fixtures/java/util").glob("*.java")):
        shutil.copyfile(fixture, package / fixture.name)
    crate = audit.parents[1]
    for name in ("M3ComparisonSorts", "M3TimSort"):
        shutil.copyfile(crate / "payload/java" / (name + ".java.txt"),
                        package / (name + ".java"))
    run([javac, "--release", "21", "--patch-module", "java.base=" + str(source),
         "-proc:none", "-d", classes, *sorted(package.glob("*.java"))],
        build / "compile.txt")
    result = run([java, "-Xmx512m", "--patch-module", "java.base=" + str(classes),
                  "java.util.KernelAudit"], build / "results.txt")
    expected = "AUDIT_ONLY total cases=" + str(recorded["expected_cases"]) + (
            " PASS; production API/monitor/recipe qualification not implied")
    if result.strip().splitlines()[-1] != expected:
        raise SystemExit("Unexpected audit summary; inspect " + str(build / "results.txt"))
    print(expected)
    print("Audit-only evidence retained in " + str(build))


if __name__ == "__main__":
    main()
