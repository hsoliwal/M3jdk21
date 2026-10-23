#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Qualify the existing M3File delta owners; this does not execute a build-tool planner."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess


OWNERS = (
    "m3/file/src/main/java/com/m3/file/M3File.java",
    "m3/ports/compiletime/src/main/java/com/m3/compiletime/ast/M3FileSourceFacts.java",
    "m3/tooling/module-packs/parser/src/main/java/jdk/m3/parser/M3File.java",
)
TESTS = (
    ("m3/file/src/test/java/com/m3/file/M3FileFingerprintContract.java",
     "com.m3.file.M3FileFingerprintContract", "M3_FILE_FINGERPRINT_PASS"),
    ("m3/ports/compiletime/src/test/java/com/m3/compiletime/ast/M3FileSourceFactsDeltaContract.java",
     "com.m3.compiletime.ast.M3FileSourceFactsDeltaContract", "M3FileSourceFactsDeltaContract PASS"),
    ("m3/tooling/module-packs/parser/src/test/java/jdk/m3/parser/test/M3FileDeltaContract.java",
     "jdk.m3.parser.test.M3FileDeltaContract", "M3_FILE_DELTA_PASS"),
    ("m3/tooling/module-packs/build-tools/delta/src/test/java/jdk/m3/build/test/M3BuildDeltaContract.java",
     "jdk.m3.build.test.M3BuildDeltaContract", "M3_BUILD_DELTA_PASS"),
)
PUBLIC_TYPES = (
    "com.m3.file.M3File", "com.m3.file.M3File$Metadata",
    "com.m3.compiletime.ast.M3FileSourceFacts", "jdk.m3.parser.M3File",
)


def run(command, output, name):
    result = subprocess.run([str(arg) for arg in command], capture_output=True, text=True)
    text = result.stdout + result.stderr
    (output / (name + ".log")).write_text(text, encoding="utf-8")
    if result.returncode:
        raise RuntimeError(f"{name} failed with exit {result.returncode}; see {output / (name + '.log')}")
    return text


def public_declarations(text):
    return {line.strip() for line in text.splitlines()
            if line.strip().startswith(("public ", "protected "))}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo-root", type=Path, required=True)
    parser.add_argument("--jdk-home", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--baseline-ref", required=True)
    args = parser.parse_args()
    root, jdk, output = args.repo_root.resolve(), args.jdk_home.resolve(), args.out.resolve()
    if output.exists() and any(output.iterdir()):
        raise ValueError("qualification output must be new or empty")
    output.mkdir(parents=True, exist_ok=True)
    version = run([jdk / "bin/javac", "-version"], output, "compiler-version")
    if not version.startswith("javac 21."):
        raise ValueError("this qualification requires an actual Java 21 compiler")
    runtime = run([jdk / "bin/java", "-version"], output, "runtime-version")
    baseline = run(["git", "-C", root, "rev-parse", "--verify", args.baseline_ref + "^{commit}"],
                   output, "baseline-ref").strip()
    baseline_source = output / "baseline-source"
    for relative in OWNERS:
        result = subprocess.run(["git", "-C", str(root), "show", baseline + ":" + relative],
                                capture_output=True, check=True)
        path = baseline_source / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(result.stdout)
    baseline_classes, classes = output / "baseline-classes", output / "classes"
    baseline_classes.mkdir()
    classes.mkdir()
    sources = [*OWNERS, *(row[0] for row in TESTS)]
    qualified_inputs = [*sources, "m3/tooling/module-packs/build-tools/delta/verify.py"]
    candidate_source = output / "candidate-source"
    source_hashes = {}
    for relative in qualified_inputs:
        data = (root / relative).read_bytes()
        path = candidate_source / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        source_hashes[relative] = hashlib.sha256(data).hexdigest()
    flags = ["--release", "21", "-Xlint:all", "-Werror"]
    run([jdk / "bin/javac", *flags, "-d", baseline_classes,
         *(baseline_source / relative for relative in OWNERS)], output, "baseline-compile")
    run([jdk / "bin/javac", *flags, "-d", classes,
         *(candidate_source / relative for relative in sources)], output, "candidate-compile")
    api = []
    for index, name in enumerate(PUBLIC_TYPES):
        before = run([jdk / "bin/javap", "-public", "-classpath", baseline_classes, name],
                     output, f"api-{index}-before")
        after = run([jdk / "bin/javap", "-public", "-classpath", classes, name],
                    output, f"api-{index}-after")
        removed = public_declarations(before) - public_declarations(after)
        if removed:
            raise ValueError(f"existing public declarations removed from {name}: {sorted(removed)}")
        api.append({"class": name, "baseline_declarations_preserved": True})
    tests = []
    for index, (_, name, marker) in enumerate(TESTS):
        arguments = [output / "build-fixtures"] if name == "jdk.m3.build.test.M3BuildDeltaContract" else []
        text = run([jdk / "bin/java", "-ea", "-cp", classes, name, *arguments], output, f"test-{index}")
        if marker not in text:
            raise ValueError("successful exit without required contract marker: " + name)
        tests.append({"class": name, "result": text.strip()})
    for relative in qualified_inputs:
        if (root / relative).read_bytes() != (candidate_source / relative).read_bytes():
            raise ValueError("qualified source changed during verification: " + relative)
    receipt = {
        "schema": "M3_FILE_BUILD_DELTA_QUALIFICATION_V1", "result": "PASS",
        "baseline": baseline, "compiler": version.strip(), "runtime": runtime.strip(),
        "source_sha256": source_hashes, "public_api": api, "tests": tests,
        "scope": "Exact source/file/context delta APIs and real javac fixtures",
        "pending": ["Maven/Gradle task-planner integration", "cross-build persisted facts",
                    "source-built M3JDK/HotSpot and full fork suites", "comparative resource measurements"],
    }
    (output / "RESULT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"result": "PASS", "contract_programs": len(tests),
                      "public_types": len(api), "receipt": str(output / "RESULT.json")}, sort_keys=True))


if __name__ == "__main__":
    main()
