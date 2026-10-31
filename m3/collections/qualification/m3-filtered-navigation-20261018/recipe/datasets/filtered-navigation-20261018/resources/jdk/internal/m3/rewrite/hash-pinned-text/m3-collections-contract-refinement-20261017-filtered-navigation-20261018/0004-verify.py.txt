#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run the current collection core and the declared regression closure with real Java/JNI."""

import argparse
import hashlib
import json
import os
from pathlib import Path
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[5])
    parser.add_argument("--output", type=Path)
    parser.add_argument("--maven", default="mvn")
    parser.add_argument("--maven-repository", type=Path)
    parser.add_argument("--offline", action="store_true")
    args = parser.parse_args()
    root = args.repository.resolve()
    here = Path(__file__).resolve().parent
    output = (args.output or here / "target").resolve()
    if output.exists() and (not output.is_dir() or any(output.iterdir())):
        raise SystemExit("Use a fresh empty output directory; earlier classes or reports must not be reused.")
    output.mkdir(parents=True, exist_ok=True)
    java_home = Path(os.environ["JAVA_HOME"])
    java = str(java_home / "bin/java")
    native = output / "native-lib"
    missing = output / "empty-lib"
    native.mkdir(exist_ok=True)
    missing.mkdir(exist_ok=True)
    if any(missing.iterdir()):
        raise SystemExit("The missing-library directory must be empty.")
    records = []

    def run(name, command):
        log = output / (name + ".log")
        started = time.monotonic()
        with log.open("w", encoding="utf-8") as stream:
            stream.write(shlex.join(command) + "\n")
            stream.flush()
            completed = subprocess.run(command, cwd=root, stdout=stream, stderr=subprocess.STDOUT)
        records.append({"name": name, "command": command, "exit_code": completed.returncode,
                        "seconds": round(time.monotonic() - started, 3), "log": log.name})
        print(name + ": " + ("PASS" if completed.returncode == 0 else "FAIL"), flush=True)
        if completed.returncode:
            print(log.read_text(encoding="utf-8")[-12000:], flush=True)
            raise SystemExit(completed.returncode)

    namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
    pom = ET.parse(here / "pom.xml").getroot()
    test_paths = [root / "m3/collections/src/test/java" / item.text
                  for item in pom.findall(".//m:testInclude", namespace)]
    main_paths = sorted((root / "m3/collections/src/main/java").rglob("*.java"))
    c_root = root / "m3/collections/src/main/native/collections"
    c_paths = [c_root / name for name in ("m3_index_path_masks.c", "m3_index_mask_compactor.c",
                                        "m3_index_mask_window.c")]
    inputs = main_paths + test_paths + c_paths + [here / "pom.xml", Path(__file__).resolve()]

    def seals():
        return {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
                for path in sorted(inputs)}

    before = seals()
    (output / "INPUTS.tsv").write_text("".join(path + "\t" + digest + "\n"
                                             for path, digest in before.items()), encoding="utf-8")
    run("java-version", [java, "-version"])
    common_c = [os.environ.get("CC", "gcc"), "-std=c17", "-Wall", "-Wextra", "-Werror",
                "-pedantic", "-O2", "-fPIC", "-shared", "-I" + str(java_home / "include"),
                "-I" + str(java_home / "include/linux")]
    run("compile-pathmask", common_c + ["-o", str(native / "libm3_collections_pathmask.so"), str(c_paths[0])])
    run("compile-mask", common_c + ["-o", str(native / "libm3_collections_mask.so"),
                                   str(c_paths[1]), str(c_paths[2])])
    totals = {}
    for mode, library in (("java", missing), ("native", native)):
        build = output / mode
        command = [args.maven, "-B", "-ntp", "-f", str(here / "pom.xml"),
                   "-Dm3.target=" + str(root), "-Dm3.runtime.output=" + str(build),
                   "-Dm3.native.path=" + str(library)]
        if args.offline:
            command.append("-o")
        if args.maven_repository:
            command.append("-Dmaven.repo.local=" + str(args.maven_repository.resolve()))
        run(mode + "-junit", command + ["test"])
        suites = [ET.parse(path).getroot() for path in (build / "surefire-reports").glob("TEST-*.xml")]
        totals[mode] = {key: sum(int(suite.attrib.get(key, "0")) for suite in suites)
                        for key in ("tests", "failures", "errors", "skipped")}
        if not suites or totals[mode]["tests"] < 45 or any(totals[mode][key] for key in ("failures", "errors", "skipped")):
            raise SystemExit("The complete declared JUnit closure must execute without failures or skips.")
        classpath = os.pathsep.join(str(build / item) for item in ("classes", "test-classes"))
        launcher = [java, "-ea", "-Xcheck:jni", "-Xmx512m", "-Djava.library.path=" + str(library),
                    "-cp", classpath]
        suite_args = [
            ("M3IndexMaskCompactorSelfTest", ["--native"] if mode == "native" else []),
            ("M3IndexMaskWindowSelfTest", ["--native"] if mode == "native" else []),
            ("M3IndexMaskWindowRecoverySelfTest", ["--native"] if mode == "native" else []),
            ("M3IndexPathMasksSelfTest", ["--native"] if mode == "native" else []),
            ("M3IndexPathMaskWindowSelfTest", ["--native"] if mode == "native" else []),
            ("M3BudgetedMaskWindowContract", ["--native"] if mode == "native" else []),
            ("M3MaskWindowSparseContract", ["native"] if mode == "native" else ["missing"]),
            ("M3NativeIndexRangeSelfTest", ["native"] if mode == "native" else ["none"]),
            ("M3ZeroMaskArrayFastPathProof", []),
            ("M3IndexPathMasksAliasContract", ["--native"] if mode == "native" else []),
        ]
        if mode == "native":
            suite_args += [("M3IndexPathRankNativeSelfTest", []), ("M3MaskNativeParityProof", []),
                           ("M3MaskDenseSelectionProof", [])]
        else:
            suite_args += [("M3IndexPathRankSelfTest", []), ("M3LongIndexPathMasksSelfTest", []),
                           ("M3BudgetedMaskWindowContract", ["--missing"])]
        for ordinal, (name, arguments) in enumerate(suite_args):
            run(mode + "-" + str(ordinal) + "-" + name, launcher + ["com.m3.collections." + name] + arguments)
        if mode == "java":
            run("small-stack-wrapper-depth", launcher[:1] + ["-Xss256k"] + launcher[1:]
                + ["com.m3.collections.M3FilteredNavigationReconciliationContract", "depth"])
    if seals() != before:
        raise SystemExit("An input changed during runtime qualification.")
    result = {"scope": "focused_runtime_closure; independent owning-module and whole-JDK gates remain",
              "production_java_units": len(main_paths), "test_source_units": len(test_paths),
              "compiled_native_sources": len(c_paths), "junit": totals, "runs": records,
              "inputs_unchanged": True, "native_required_executed": True}
    (output / "RESULTS.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print("M3_FILTERED_NAVIGATION_RUNTIME_PASS " + json.dumps({"junit": totals, "inputs_unchanged": True}), flush=True)


if __name__ == "__main__":
    main()
