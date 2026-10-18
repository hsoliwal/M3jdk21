#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Qualify emitted functional Javadocs against the exact eight-file progress successor."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def read(path):
    return [line.split("\t") for line in path.read_text().splitlines()
            if line and not line.startswith("#")]


def main():
    parser = argparse.ArgumentParser()
    for name in ("baseline", "functional", "progress", "jdk", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    progress_manifest = args.progress / "m3/compatibility/m3-progress-doclint-20261018.tsv"
    if sha(progress_manifest) != "7246c16a0e8ec742eba98dac97e24b7fc3947250ad5d2a22f72ac32dcddf0219":
        raise ValueError("progress successor custody mismatch")
    functional_manifest = args.functional / "m3/compatibility/m3-functional-progress-doclint-20261018.tsv"
    progress = read(progress_manifest)
    functional = read(functional_manifest)
    if len(progress) != 8 or len(functional) != 3 or len({row[0] for row in progress + functional}) != 11:
        raise ValueError("unexpected compiler source closure")
    inputs = {}
    for path, before, after, _ in progress:
        if sha(args.progress / path) != after:
            raise ValueError("progress source drift: " + path)
        inputs[path] = (args.progress / path, args.progress / path)
    for path, before, after, _ in functional:
        if sha(args.baseline / path) != before or sha(args.functional / path) != after:
            raise ValueError("functional source drift: " + path)
        inputs[path] = (args.baseline / path, args.functional / path)
    javac = str((args.jdk / "bin/javac").resolve())
    version = subprocess.check_output([javac, "-version"], text=True).strip()
    if not version.startswith("javac 21."):
        raise ValueError("requires Java 21 javac")
    observations = {}
    for label, index, lint, success in (("before-doclint", 0, "all/protected", False),
                                      ("before-bytecode", 0, "none", True),
                                      ("after-doclint", 1, "all/protected", True)):
        source = args.output / label / "source"
        classes = args.output / label / "classes"
        classes.mkdir(parents=True)
        for path, pair in inputs.items():
            destination = source / path
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(pair[index], destination)
        argv = [javac, "-g:none", "-proc:none", "-Werror", "-Xdoclint:" + lint,
                "--patch-module", "java.base=" + str((source / "src/java.base/share/classes").resolve()),
                "-d", str(classes.resolve())] + [str((source / p).resolve()) for p in sorted(inputs)]
        result = subprocess.run(argv, capture_output=True, text=True, timeout=90)
        (args.output / (label + ".stdout.log")).write_text(result.stdout)
        (args.output / (label + ".stderr.log")).write_text(result.stderr)
        observations[label] = {"command": argv, "exit": result.returncode,
                               "warning_lines": result.stderr.count("warning:")}
        if (result.returncode == 0) != success or (success and (result.stdout or result.stderr)):
            raise ValueError("unexpected compiler result: " + label + " " + result.stderr)
        if not success and (observations[label]["warning_lines"] != 7
                            or "warnings found and -Werror specified" not in result.stderr):
            raise ValueError("expected seven original functional doclint warnings")
    def tree(label):
        root = args.output / label / "classes"
        return {str(p.relative_to(root)): sha(p) for p in sorted(root.rglob("*.class"))}
    before, after = tree("before-bytecode"), tree("after-doclint")
    if len(before) != 14 or before != after:
        raise ValueError("comment-only edit changed executable class bytes")
    receipt = {"schema": "M3_FUNCTIONAL_DOCLINT_EQUIVALENCE_V1", "result": "PASS", "javac": version,
               "observations": observations, "identical_classes": after,
               "progress_manifest_sha256": sha(progress_manifest),
               "functional_manifest_sha256": sha(functional_manifest),
               "scope": "11 exact emitted progress/function source files; 3 newly documented functions",
               "reference_only": "before-bytecode disables doclint to obtain the known-warning reference classes",
               "Object_universal_compatibility": "HOLD", "full_image_build": "SEPARATE_GATE"}
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS three emitted functional owners; seven original warnings; eleven-source closure doclint zero; 14 identical class files")


if __name__ == "__main__":
    main()
