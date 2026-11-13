#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compile and execute actual M3 owners/cursor on a stock Java 21 VM.

The real target String.java is read only for compiler symbol resolution. With
-implicit:none, its class is never emitted or patched into the stock VM. All
explicit production sources are unchanged actual M3String*.java owners. No
substitute owner, extracted cursor, or rewritten runtime source is generated.

This bounded source/owner proof does not establish public String dispatch,
native String admission, JNI compatibility shadows, or a matched HotSpot build.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

SOURCE_PREFIX = Path("src/java.base/share/classes")
REQUIRED = {"M3String.java", "M3StringOwner.java", "M3StringAtom.java",
            "M3StringTuple.java", "M3StringFacts.java"}
LOADED = ["java.lang.M3String", "java.lang.M3StringOwner", "java.lang.M3StringAtom",
          "java.lang.M3StringTuple", "java.lang.M3String$CodeUnitCursor"]


def digest(path: Path) -> dict[str, str | int]:
    content = path.read_bytes()
    header = f"blob {len(content)}\0".encode("ascii")
    return {"path": str(path), "bytes": len(content),
            "sha256": hashlib.sha256(content).hexdigest(),
            "git_blob": hashlib.sha1(header + content).hexdigest()}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--symbol-root", type=Path,
                        help="Optional exact baseline dependency source tree")
    parser.add_argument("--test-source", type=Path,
                        help="Explicit shared test fixture, for a baseline without the new test")
    parser.add_argument("--java-home", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--expected-m3-blob")
    parser.add_argument("--benchmark", action="store_true")
    parser.add_argument("--skip-interpreter", action="store_true")
    arguments = parser.parse_args()
    here = Path(__file__).resolve().parent
    test_source = (arguments.test_source.resolve() if arguments.test_source else
            arguments.source_root.resolve() / "test/jdk/java/lang/String/M3StringPathCursorTest.java")
    if not test_source.is_file():
        parser.error("the exact M3StringPathCursorTest.java fixture is required; "
                     "use --test-source for a baseline without the new test")
    roots = [arguments.source_root.resolve() / SOURCE_PREFIX]
    if arguments.symbol_root:
        roots.append(arguments.symbol_root.resolve() / SOURCE_PREFIX)
    source_by_name: dict[str, Path] = {}
    for root in reversed(roots):
        for path in sorted((root / "java/lang").glob("M3String*.java")):
            source_by_name[path.name] = path
    if not REQUIRED <= source_by_name.keys():
        parser.error(f"missing actual source owners: {sorted(REQUIRED - source_by_name.keys())}")
    symbols = next((root / "java/lang/String.java" for root in roots
                    if (root / "java/lang/String.java").is_file()), None)
    if symbols is None:
        parser.error("actual target String.java is required for compiler symbols")
    seal = digest(source_by_name["M3String.java"])
    if arguments.expected_m3_blob and seal["git_blob"] != arguments.expected_m3_blob:
        parser.error(f"M3String source drift: {seal['git_blob']}")
    output = arguments.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    classes = output / "classes"
    bench_classes = output / "bench-classes"
    classes.mkdir()
    bench_classes.mkdir()
    report: dict[str, object] = {
        "schema": "M3_ACTUAL_OWNER_CURSOR_V1",
        "runner": digest(Path(__file__).resolve()),
        "actual_sources": [digest(source_by_name[name]) for name in sorted(source_by_name)],
        "compiler_only_string_symbols": digest(symbols),
        "test": digest(test_source),
        "benchmark": digest(here / "M3StringCursorBenchmark.java"),
        "stock_vm": True,
        "simulated_owners": False,
        "extracted_runtime_methods": False,
        "modified_string_class_loaded": False,
        "string_admission_tested": False,
        "hotspot_jni_integration_tested": False,
        "commands": [],
        "status": "STARTED",
    }
    javac = str(arguments.java_home.resolve() / "bin/javac")
    java = str(arguments.java_home.resolve() / "bin/java")

    def invoke(name: str, command: list[str]) -> None:
        log = output / (name + ".log")
        with log.open("w", encoding="utf-8") as stream:
            completed = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT,
                                       check=False, text=True)
        report["commands"].append({"name": name, "argv": command,
                                   "returncode": completed.returncode, "log": str(log)})
        print(f"{name}: returncode={completed.returncode} log={log}", flush=True)
        if completed.returncode:
            print(log.read_text(encoding="utf-8")[-16000:], file=sys.stderr)
            raise RuntimeError(f"{name} failed with {completed.returncode}")

    try:
        invoke("java-version", [java, "-version"])
        invoke("compile-actual-owners", [javac, "--release", "21", "-proc:none",
                "-implicit:none", "--patch-module",
                "java.base=" + os.pathsep.join(map(str, roots)), "-d", str(classes),
                *[str(source_by_name[name]) for name in sorted(source_by_name)]])
        production_classes = sorted(path.relative_to(classes).as_posix()
                                    for path in classes.rglob("*.class"))
        if not production_classes or any(not path.startswith("java/lang/M3String")
                                         for path in production_classes):
            raise RuntimeError("unexpected platform replacement in runtime patch")
        report["production_class_files"] = production_classes
        invoke("compile-test", [javac, "--release", "21", "-proc:none", "-implicit:none",
                "-Xlint:all", "-Werror", "--patch-module",
                "java.base=" + os.pathsep.join((str(classes), str(test_source.parent))),
                "-d", str(classes), str(test_source)])
        trace = output / "actual-class-load.log"
        invoke("test-default", [java, "-Xms32m", "-Xmx128m", "-ea",
                "-Xlog:class+load=info:file=" + str(trace), "--patch-module",
                "java.base=" + str(classes), "java.lang.M3StringPathCursorTest"])
        trace_lines = trace.read_text(encoding="utf-8").splitlines()
        loaded_lines = []
        for name in LOADED:
            matches = [line for line in trace_lines if (name + " source: ") in line]
            if len(matches) != 1 or str(classes) not in matches[0]:
                raise RuntimeError(f"actual class load proof failed: {name}")
            loaded_lines.extend(matches)
        report["actual_owner_load_evidence"] = loaded_lines
        if not arguments.skip_interpreter:
            invoke("test-interpreter", [java, "-Xms32m", "-Xmx128m", "-Xint", "-ea",
                    "--patch-module", "java.base=" + str(classes),
                    "java.lang.M3StringPathCursorTest"])
        if arguments.benchmark:
            invoke("compile-benchmark", [javac, "--release", "21", "-proc:none", "-implicit:none",
                    "-Xlint:all", "-Werror", "--patch-module", "java.base=" + str(classes),
                    "-d", str(bench_classes), str(here / "M3StringCursorBenchmark.java")])
            command = [java, "-Xms128m", "-Xmx128m", "-ea", "--patch-module",
                       "java.base=" + str(classes), "-cp", str(bench_classes),
                       "M3StringCursorBenchmark"]
            timer = Path("/usr/bin/time")
            if timer.is_file():
                command = [str(timer), "-v", "-o", str(output / "benchmark-process-memory.txt"),
                           *command]
            invoke("benchmark", command)
        report["status"] = "PASS"
    except (OSError, RuntimeError) as failure:
        report["status"] = "FAIL"
        report["failure"] = str(failure)
    finally:
        (output / "receipt.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print("M3_ACTUAL_OWNER_CURSOR_" + str(report["status"]), flush=True)
    return 0 if report["status"] == "PASS" else 1


if __name__ == "__main__":
    raise SystemExit(main())
