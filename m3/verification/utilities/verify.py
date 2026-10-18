#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed Java 21 utility receiver checks, using actual receiving files."""
from __future__ import annotations

import argparse
import csv
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time

DATASET = "m3jdk21-utils-20261018"
MANIFEST = "m3/verification/utilities/SOURCE_INPUTS.tsv"
RUNTIME = (
    "src/java.base/share/classes/java/io/IOUtils.java",
    "src/java.base/share/classes/java/lang/reflect/Reflections.java",
    "src/java.base/share/classes/java/util/Streams.java",
    "src/java.base/share/classes/java/util/Strings.java",
)
MAINS = ("ReflectionsContract", "StreamsIOUtilitiesContract", "UtilityFamilyCompositionContract")
TEST_ROOT = "m3/verification/utilities/src/test/java/"


def sha(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def regular(root: Path, relative: str) -> Path:
    part = Path(relative)
    if part.is_absolute() or ".." in part.parts or str(part) != relative:
        raise ValueError("noncanonical source path: " + relative)
    path = root / part
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("source symlink: " + relative)
        cursor = cursor.parent
    if not path.is_file():
        raise ValueError("missing source: " + relative)
    return path


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--java-home", type=Path, default=os.environ.get("JAVA_HOME"))
    parser.add_argument("--output-dir", type=Path)
    parser.add_argument("--recipe-crate", type=Path)
    parser.add_argument("--recipe-owner", type=Path)
    args = parser.parse_args()
    root = args.source_root.resolve(strict=True)
    if args.java_home is None:
        parser.error("--java-home or JAVA_HOME must identify a Java 21 JDK")
    java_home = args.java_home.resolve(strict=True)
    if bool(args.recipe_crate) != bool(args.recipe_owner):
        parser.error("--recipe-crate and --recipe-owner must be supplied together")
    release = (java_home / "release").read_text(encoding="utf-8")
    versions = [line.split("=", 1)[1].strip('"') for line in release.splitlines()
                if line.startswith("JAVA_VERSION=")]
    if len(versions) != 1 or versions[0].split(".")[0].split("-")[0] != "21":
        parser.error("the receiver proof requires Java 21")
    out = (args.output_dir or root / "m3/verification/utilities/out").resolve()
    out.mkdir(parents=True, exist_ok=True)
    if any(out.iterdir()):
        parser.error("--output-dir must be empty so previous outputs cannot satisfy a gate")
    logs = out / "logs"
    logs.mkdir()
    receipt = {"dataset": DATASET, "status": "RUNNING", "source_root": str(root),
               "java_version": versions[0], "java_home": str(java_home),
               "manifest_sha256": None, "sources": [], "commands": [],
               "scope": "Actual receiver source files patched into a Java 21 host; not a full M3JDK21 image or jtreg run."}

    def run(label: str, command: list[str], timeout: int = 180) -> str:
        started = time.monotonic()
        result = subprocess.run(command, cwd=root, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, timeout=timeout)
        log = logs / (label + ".log")
        log.write_text(result.stdout, encoding="utf-8")
        receipt["commands"].append({"label": label, "argv": command,
            "exit_code": result.returncode, "seconds": time.monotonic() - started,
            "log": str(log.relative_to(out)), "log_sha256": sha(log)})
        if result.returncode:
            raise RuntimeError(label + " failed; inspect " + str(log))
        print(label + ": PASS", flush=True)
        return result.stdout

    try:
        manifest = regular(root, MANIFEST)
        receipt["manifest_sha256"] = sha(manifest)
        with manifest.open(encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream, delimiter="\t")
            if reader.fieldnames != ["path", "sha256", "role"]:
                raise ValueError("unexpected source manifest schema")
            rows = list(reader)
        names = [row["path"] for row in rows]
        if names != sorted(set(names)):
            raise ValueError("source manifest must be sorted and unique")
        expected = set(RUNTIME) | {TEST_ROOT + name + ".java" for name in MAINS}
        if not expected.issubset(names):
            raise ValueError("source manifest does not cover every compiled runtime/test input")
        sealed = {row["path"]: row["sha256"] for row in rows}
        for row in rows:
            source = regular(root, row["path"])
            if sha(source) != row["sha256"]:
                raise ValueError("source drift: " + row["path"])
            receipt["sources"].append(row)
        run("java-version", [str(java_home / "bin/java"), "-XX:-UsePerfData", "-version"])
        if args.recipe_crate:
            crate = args.recipe_crate.resolve(strict=True)
            owner = args.recipe_owner.resolve(strict=True)
            check_output = run("actual-recipe-check", [sys.executable, str(crate / "receive.py"),
                "--crate", str(crate), "--recipe-owner", str(owner), "--dataset", DATASET,
                "--target", str(root), "--check"], 300)
            prefix = "M3_COLLECTIONS_RECEIVER_PASS "
            results = [json.loads(line[len(prefix):]) for line in check_output.splitlines()
                       if line.startswith(prefix)]
            if len(results) != 1:
                raise ValueError("missing or repeated actual receiver result")
            result = results[0]
            if (result.get("dataset") != DATASET or result.get("mode") != "check"
                    or result.get("actual_sdk_results") is not True
                    or type(result.get("changes")) is not int or result["changes"] != 0):
                raise ValueError("actual receiver is not at its qualified fixed point")
            receipt["recipe_check"] = "PASS"
            receipt["recipe_result"] = result
        else:
            receipt["recipe_check"] = "NOT_RUN; independent runtime gate only"
        run("catalogue", [sys.executable,
            str(regular(root, "m3/verification/utilities/catalogue.py")), "verify",
            "--source-root", str(root)])
        patch_source = out / "patch-sources"
        patch = out / "patch-classes"
        classes = out / "test-classes"
        patch.mkdir()
        classes.mkdir()
        compiled = []
        prefix = "src/java.base/share/classes/"
        for relative in RUNTIME:
            source = regular(root, relative)
            copied = patch_source / relative.removeprefix(prefix)
            copied.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, copied)
            if sha(copied) != sealed[relative]:
                raise ValueError("compile input copy drift: " + relative)
            compiled.append(str(copied))
        javac = str(java_home / "bin/javac")
        run("compile-runtime", [javac, "-Xlint:all", "-Werror", "--patch-module",
            "java.base=" + str(patch_source), "-d", str(patch)] + compiled)
        test_source = out / "test-sources"
        test_source.mkdir()
        tests = []
        for name in MAINS:
            relative = TEST_ROOT + name + ".java"
            copied = test_source / (name + ".java")
            shutil.copyfile(regular(root, relative), copied)
            if sha(copied) != sealed[relative]:
                raise ValueError("test compile input copy drift: " + relative)
            tests.append(str(copied))
        run("compile-contracts", [javac, "-Xlint:all", "-Werror", "--patch-module",
            "java.base=" + str(patch), "-d", str(classes)] + tests)
        for name in MAINS:
            run(name, [str(java_home / "bin/java"), "-XX:-UsePerfData", "-ea", "-Xcheck:jni",
                "--patch-module", "java.base=" + str(patch), "-cp", str(classes), name])
        for owner in ("java.util.Streams", "java.io.IOUtils", "java.lang.reflect.Reflections",
                      "java.util.Strings"):
            run("api-" + owner, [str(java_home / "bin/javap"), "-public", "-s",
                str(patch / (owner.replace(".", "/") + ".class"))])
        if sha(regular(root, MANIFEST)) != receipt["manifest_sha256"]:
            raise ValueError("source manifest changed during verification")
        for relative, digest in sealed.items():
            if sha(regular(root, relative)) != digest:
                raise ValueError("source changed during verification: " + relative)
        receipt["final_source_recheck"] = True
        receipt["compiled_inputs_match_manifest"] = True
        receipt["status"] = "PASS_RECEIVER_SCOPE" if args.recipe_crate else "PASS_PATCH_SCOPE"
    except Exception as failure:
        receipt["status"] = "FAIL"
        receipt["failure"] = str(failure)
        print(str(failure), file=sys.stderr)
    finally:
        (out / "EXECUTION.json").write_text(json.dumps(receipt, indent=2, sort_keys=True)
                                            + "\n", encoding="utf-8")
    print(receipt["status"], flush=True)
    return 0 if receipt["status"].startswith("PASS_") else 1


if __name__ == "__main__":
    raise SystemExit(main())
