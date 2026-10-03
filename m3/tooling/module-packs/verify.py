#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Offline Java contract checks; never mislabeled Maven/JUnit/JaCoCo execution."""
import argparse
import hashlib
import json
import subprocess
from pathlib import Path
from build_pack import java_home, command


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    home = java_home(args.jdk)
    output = args.out.absolute()
    if output.resolve().is_relative_to(home):
        raise ValueError("Output inside JAVA_HOME is forbidden")
    output.mkdir(parents=True, exist_ok=False)
    root = Path(__file__).resolve().parent
    files = sorted((root / "kernel/src/main/java").rglob("*.java"))
    checks = root / "kernel/src/test/java/com/m3/tools/modulepack/M3ModuleChecks.java"
    before = {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
              for path in [*files, checks]}
    classes, tests = output / "classes", output / "test-classes"
    classes.mkdir()
    tests.mkdir()
    command([home / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-d",
             classes, *files], output / "compile.log")
    command([home / "bin/javac", "--release", "21", "-Xlint:all", "-Werror", "-cp",
             classes, "-d", tests, checks], output / "test-compile.log")
    import os
    result = command([home / "bin/java", "--add-modules", "jdk.jlink", "-cp",
                      os.pathsep.join([str(classes), str(tests)]),
                      "com.m3.tools.modulepack.M3ModuleChecks", output / "fixtures"],
                     output / "checks.log")
    if "M3_MODULE_CHECKS_PASS" not in result:
        raise ValueError("Missing contract-check receipt")
    command([__import__("sys").executable, "-m", "unittest", "test_pack"],
            output / "python-tests.log") if Path.cwd() == root else command(
                [__import__("sys").executable, root / "test_pack.py"], output / "python-tests.log")
    after = {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
             for path in [*files, checks]}
    if before != after:
        raise ValueError("Source drift during verification")
    receipt = {"source_sha256": after, "jdk_release_sha256": hashlib.sha256(
        (home / "release").read_bytes()).hexdigest(), "java_contract_checks": result.strip(),
        "maven_junit_jacoco": "NOT_RUN", "openrewrite_tests": "NOT_RUN"}
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(result.strip())


if __name__ == "__main__":
    main()
