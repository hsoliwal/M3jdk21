#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compare emitted progress documentation with pinned originals and Java 21 output."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--baseline", type=Path, required=True)
    parser.add_argument("--sdk-candidate", type=Path, required=True)
    parser.add_argument("--jdk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    manifest = args.sdk_candidate / "m3/compatibility/m3-progress-doclint-20261018.tsv"
    sources = []
    for raw in manifest.read_text().splitlines():
        if raw and not raw.startswith("#"):
            path, before, after, _ = raw.split("\t")
            if sha(args.baseline / path) != before or sha(args.sdk_candidate / path) != after:
                raise ValueError("source mismatch: " + path)
            sources.append(path)
    if len(sources) != 8 or len(set(sources)) != 8:
        raise ValueError("expected eight documentation owners")
    javac = str((args.jdk / "bin/javac").resolve())
    version = subprocess.run([javac, "-version"], text=True, capture_output=True, check=True).stdout.strip()
    if not version.startswith("javac 21."):
        raise ValueError("requires Java 21 javac: " + version)
    observations = {}
    for label, base, doclint, expected_success in (
            ("before-doclint", args.baseline, "all/protected", False),
            ("after-all-visibility", args.sdk_candidate, "all", False),
            ("before-bytecode", args.baseline, "none", True),
            ("after-doclint", args.sdk_candidate, "all/protected", True)):
        inputs = args.output / label / "source"
        classes = args.output / label / "classes"
        for path in sources:
            destination = inputs / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(base / path, destination)
        classes.mkdir()
        argv = [javac, "-g:none", "-proc:none", "-Werror", "-Xmaxwarns", "1000",
                "-Xdoclint:" + doclint, "--patch-module",
                "java.base=" + str((inputs / "src/java.base/share/classes").resolve()),
                "-d", str(classes.resolve())] + [str((inputs / path).resolve()) for path in sources]
        result = subprocess.run(argv, text=True, capture_output=True, timeout=90)
        (args.output / (label + ".stdout.log")).write_text(result.stdout)
        (args.output / (label + ".stderr.log")).write_text(result.stderr)
        observations[label] = {"command": argv, "exit": result.returncode,
                               "warning_lines": result.stderr.count("warning:")}
        if (result.returncode == 0) != expected_success:
            raise ValueError("unexpected compiler result: " + label + ": " + result.stderr)
        if expected_success and (result.stderr.strip() or result.stdout.strip()):
            raise ValueError("unexpected compiler diagnostic: " + label)
        if not expected_success and ("warnings found and -Werror specified" not in result.stderr
                                           or observations[label]["warning_lines"] == 0):
            raise ValueError("expected documentation warning failure was not reproduced: " + label)
    def class_tree(label):
        root = args.output / label / "classes"
        return {str(path.relative_to(root)): sha(path) for path in sorted(root.rglob("*.class"))}
    before_classes, after_classes = class_tree("before-bytecode"), class_tree("after-doclint")
    if not before_classes or before_classes != after_classes:
        raise ValueError("documentation changed executable class bytes")
    receipt = {"schema": "M3_PROGRESS_DOCLINT_EQUIVALENCE_V1", "javac": version,
               "manifest_sha256": sha(manifest), "observations": observations,
               "identical_classes": before_classes,
               "result": "PASS", "scope": "eight exact progress source files; -g:none bytecode equality",
               "Object_universal_compatibility": "HOLD", "full_image_build": "SEPARATE_GATE",
               "build_doclint_scope": "make/modules/java.base/Java.gmk: all/protected; all categories retained",
               "all_visibility_scope": "NOT_QUALIFIED: private and anonymous constructor missing comments retained",
               "comparison_only": "before-bytecode disables doclint only to obtain the known-warning reference classes"}
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS eight emitted progress sources; original doclint failure retained; "
          + str(len(before_classes)) + " identical class files; original JDK public/protected doclint emits no warnings")


if __name__ == "__main__":
    main()
