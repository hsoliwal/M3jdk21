#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prove method-local source annotations preserve bytecode and the exact JDK lint gate."""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    for name in ("baseline", "candidate", "jdk", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=False)
    manifest = args.candidate / "m3/compatibility/m3-progress-scope-lint-20261018.tsv"
    rows = [line.split("\t") for line in manifest.read_text().splitlines() if line and not line.startswith("#")]
    if len(rows) != 4:
        raise ValueError("expected four annotation owners")
    changed = {}
    for path, before, after, _ in rows:
        if sha(args.baseline / path) != before or sha(args.candidate / path) != after:
            raise ValueError("source identity drift: " + path)
        changed[path] = args.candidate / path
    sources = {str(p.relative_to(args.baseline)): p for p in sorted(args.baseline.rglob("*.java"))}
    if len(sources) != 11:
        raise ValueError("expected eleven-source documented closure")
    javac = str((args.jdk / "bin/javac").resolve())
    version = subprocess.check_output([javac, "-version"], text=True).strip()
    if not version.startswith("javac 21."):
        raise ValueError("requires Java 21")
    observations = {}
    for label, after, fail_on_warning in (("before-full-lint", False, True),
                                         ("before-bytecode", False, False),
                                         ("after-full-lint", True, True)):
        source = args.output / label / "source"
        classes = args.output / label / "classes"
        classes.mkdir(parents=True)
        for relative, original in sources.items():
            destination = source / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(changed.get(relative, original) if after else original, destination)
        argv = [javac, "-g:none", "-proc:none", "-Xlint:all", "-Xlint:-this-escape",
                "-Xdoclint:all/protected", "-Xdoclint/package:java.*,javax.*"]
        if fail_on_warning:
            argv.append("-Werror")
        argv += ["--patch-module", "java.base=" + str((source / "src/java.base/share/classes").resolve()),
                 "-d", str(classes.resolve())] + [str((source / p).resolve()) for p in sorted(sources)]
        result = subprocess.run(argv, capture_output=True, text=True, timeout=90)
        (args.output / (label + ".stdout.log")).write_text(result.stdout)
        (args.output / (label + ".stderr.log")).write_text(result.stderr)
        observations[label] = {"command": argv, "exit": result.returncode,
                               "try_warning_lines": result.stderr.count("warning: [try]"),
                               "warning_lines": result.stderr.count("warning:")}
        success = label != "before-full-lint"
        if (result.returncode == 0) != success:
            raise ValueError("unexpected compiler result: " + label + ": " + result.stderr)
        if after and (result.stdout.strip() or result.stderr.strip()):
            raise ValueError("emitted sources still warn: " + result.stderr)
        if not after:
            warnings = observations[label]["warning_lines"]
            if warnings != observations[label]["try_warning_lines"]:
                raise ValueError("unexpected warning category: " + result.stderr)
            if fail_on_warning:
                # javac may stop after the first owner when -Werror is active.
                # The complete reference compile below must expose all five.
                if not 1 <= warnings <= 5 or "warnings found and -Werror specified" not in result.stderr:
                    raise ValueError("expected original try warning failure: " + result.stderr)
            elif warnings != 5:
                raise ValueError("expected exactly five complete-closure warnings: " + result.stderr)
    def tree(label):
        root = args.output / label / "classes"
        return {str(p.relative_to(root)): sha(p) for p in sorted(root.rglob("*.class"))}
    before, after = tree("before-bytecode"), tree("after-full-lint")
    if len(before) != 14 or before != after:
        raise ValueError("source annotations changed executable class bytes")
    receipt = {"schema": "M3_PROGRESS_SCOPE_LINT_EQUIVALENCE_V1", "result": "PASS", "javac": version,
               "manifest_sha256": sha(manifest), "observations": observations,
               "identical_classes": after, "scope": "five method-local SOURCE-retained annotations; four owners; eleven-source closure",
               "reference_only": "before-bytecode permits known warnings only to obtain reference class bytes; final gate keeps Werror",
               "Object_universal_compatibility": "HOLD", "full_image_build": "SEPARATE_GATE"}
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS five original try warnings; full JDK lint/doclint gate clean; 14 identical class files")


if __name__ == "__main__":
    main()
