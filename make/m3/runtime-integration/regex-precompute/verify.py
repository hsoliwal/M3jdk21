#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Run the existing regex product contract on an actual M3JDK21 image.

This verifier compiles tests only. It neither transforms product sources nor loads
recipe code, patched classes, private fixtures, or another repository.
"""

import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import tempfile


TEST = "test/jdk/java/lang/String/M3StringRegexPrecomputeTest.java"
SOURCE_INPUTS = (
    "src/java.base/share/classes/java/lang/String.java",
    "src/java.base/share/classes/java/lang/M3String.java",
    "src/java.base/share/classes/java/lang/M3StringRegexDp.java",
    "src/java.base/share/native/libjava/M3StringRegexDp.c",
    TEST,
)
MODES = (
    ("storage", ("-XX:+UseM3StringStorage",), "required"),
    ("storage-no-compact", ("-XX:+UseM3StringStorage", "-XX:-CompactStrings"), "required"),
    ("storage-off", ("-XX:-UseM3StringStorage",), "off"),
)


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def source_hashes(root):
    result = {}
    for name in SOURCE_INPUTS:
        path = root / name
        if not path.is_file() or path.is_symlink():
            raise ValueError("missing or linked product source: " + name)
        result[name] = digest(path)
    return result


def image_hashes(home):
    suffix = ".exe" if os.name == "nt" else ""
    paths = ("release", "bin/java" + suffix, "bin/javac" + suffix, "lib/modules")
    result = {}
    for name in paths:
        path = home / name
        if not path.is_file():
            raise ValueError("a complete source-built M3JDK21 image is required: " + str(path))
        result[name] = digest(path)
    # The Java launcher, modules, libjava and VM are all part of this execution.
    native = ("bin/java.dll", "bin/server/jvm.dll") if os.name == "nt" else (
        ("lib/libjava.dylib", "lib/server/libjvm.dylib") if sys.platform == "darwin"
        else ("lib/libjava.so", "lib/server/libjvm.so"))
    for name in native:
        path = home / name
        if not path.is_file():
            raise ValueError("missing product native library: " + str(path))
        result[name] = digest(path)
    return result


def write_json(path, value):
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8", dir=path.parent,
                                         prefix=".receipt-", delete=False) as stream:
            temporary = Path(stream.name)
            json.dump(value, stream, indent=2, sort_keys=True)
            stream.write("\n")
        temporary.replace(path)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)


def run_command(label, command, root, run_dir, receipt, receipt_path, timeout):
    log = run_dir / (label + ".log")
    entry = {"label": label, "command": [str(arg) for arg in command],
             "state": "RUNNING", "log": str(log)}
    receipt["commands"].append(entry)
    write_json(receipt_path, receipt)
    try:
        with log.open("w", encoding="utf-8") as stream:
            environment = {key: value for key, value in os.environ.items() if key not in {
                "JDK_JAVA_OPTIONS", "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVAC_OPTIONS"}}
            process = subprocess.run(entry["command"], cwd=root, stdout=stream,
                                     stderr=subprocess.STDOUT, timeout=timeout, check=False,
                                     env=environment)
        entry.update(state="FINISHED", exit_code=process.returncode)
        entry["log_sha256"] = digest(log)
        write_json(receipt_path, receipt)
        if process.returncode:
            raise RuntimeError(label + " failed with exit " + str(process.returncode)
                               + "; inspect " + str(log))
        if log.stat().st_size > 4 * 1024 * 1024:
            raise RuntimeError(label + " produced an unexpectedly large log")
        return log.read_text(encoding="utf-8", errors="replace")
    except (OSError, subprocess.TimeoutExpired) as failure:
        entry.update(state="TIMED_OUT" if isinstance(failure, subprocess.TimeoutExpired)
                     else "FAILED_TO_START", exit_code=None, error=str(failure))
        if log.is_file():
            entry["log_sha256"] = digest(log)
        write_json(receipt_path, receipt)
        raise


def verify(root, java_home, output, timeout=180):
    root, java_home, output = root.resolve(), java_home.resolve(), output.resolve()
    if output == root or output in root.parents or output == java_home or output in java_home.parents:
        raise ValueError("output must not replace the source root or JDK image")
    if output == java_home or java_home in output.parents:
        raise ValueError("verification output must be outside the JDK image")
    output.mkdir(parents=True, exist_ok=True)
    receipt_path = output / "receipt.json"
    receipt = {
        "schema": "M3_REGEX_PRODUCT_EXECUTION_V1",
        "status": "STARTED",
        "started_utc": datetime.now(timezone.utc).isoformat(),
        "scope": "Existing M3StringRegexPrecomputeTest, direct execution of its three product modes",
        "source_root": str(root), "java_home": str(java_home), "commands": [],
        "jtreg_harness": "NOT_RUN", "full_jdk_build": "NOT_RUN",
        "source_image_correspondence": "REQUIRES_SEPARATE_EXACT_BUILD_RECEIPT",
        "recipe_execution": "SYNEXIA_SEPARATE_GATE",
        "receiving_phase_advanced": False, "release_admission": "NOT_GRANTED",
    }
    # Invalidate an older PASS before reading source files or launching any tool.
    write_json(receipt_path, receipt)
    try:
        inputs = source_hashes(root)
        image = image_hashes(java_home)
        receipt.update(source_sha256=inputs, image_sha256=image)
        run_dir = Path(tempfile.mkdtemp(prefix="run-", dir=output))
        receipt["run_directory"] = str(run_dir)
        write_json(receipt_path, receipt)
        suffix = ".exe" if os.name == "nt" else ""
        java, javac = java_home / ("bin/java" + suffix), java_home / ("bin/javac" + suffix)

        def run(label, command):
            return run_command(label, command, root, run_dir, receipt, receipt_path, timeout)

        version = run("java-version", [java, "-XshowSettings:properties", "-version"])
        if not re.search(r"(?m)^\s*java\.specification\.version\s*=\s*21\s*$", version):
            raise RuntimeError("the product contract requires a Java 21 runtime")
        compiler = run("javac-version", [javac, "-version"])
        if not re.search(r"(?m)^javac 21(?:[.\s+-]|$)", compiler):
            raise RuntimeError("the product contract requires a Java 21 compiler")
        classes = run_dir / "classes"
        classes.mkdir()
        run("compile-product-test", [javac, "--release", "21", "-proc:none", "-Xlint:all",
                                     "-Werror", "-d", classes, root / TEST])
        receipt["test_class_sha256"] = {
            path.relative_to(classes).as_posix(): digest(path)
            for path in sorted(classes.rglob("*.class"))}
        if not receipt["test_class_sha256"]:
            raise RuntimeError("compiler produced no product test classes")
        receipt["modes"] = {}
        for label, flags, argument in MODES:
            text = run(label, [java, "-ea", "-esa", "-Xcheck:jni",
                               "--add-opens=java.base/java.lang=ALL-UNNAMED",
                               "-XX:+UnlockExperimentalVMOptions", *flags,
                               "-cp", classes, "M3StringRegexPrecomputeTest", argument])
            expected = "M3_REGEX_PRODUCT_TEST_PASS|mode=" + argument
            if expected not in text.splitlines():
                raise RuntimeError("missing exact product success marker for " + label)
            receipt["modes"][label] = "PASSED"
            write_json(receipt_path, receipt)
        if source_hashes(root) != inputs:
            raise RuntimeError("product inputs changed during qualification")
        if image_hashes(java_home) != image:
            raise RuntimeError("product image changed during qualification")
        if {path.relative_to(classes).as_posix(): digest(path)
            for path in sorted(classes.rglob("*.class"))} != receipt["test_class_sha256"]:
            raise RuntimeError("compiled product test classes changed during qualification")
        receipt.update(status="PASSED", inputs_and_image_unchanged=True)
        print("M3_REGEX_PRODUCT_VERIFY_PASS|modes=3|release=SEPARATE_GATE")
        return receipt
    except BaseException as failure:
        receipt.update(status="FAILED", error={"type": type(failure).__name__, "message": str(failure)})
        raise
    finally:
        receipt["finished_utc"] = datetime.now(timezone.utc).isoformat()
        write_json(receipt_path, receipt)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", type=Path, required=True)
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--timeout", type=int, default=180, help="per-command seconds (default 180)")
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("timeout must be positive")
    try:
        verify(args.target, args.java_home, args.output, args.timeout)
    except Exception as failure:
        print(type(failure).__name__ + ": " + str(failure), file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
