#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Compile actual collections owners and qualify their JNI plus java.util.Sorts parity.

No product source is changed. Output must be new or empty. The sorting patch is
an explicit input built by the sorting verifier; no fixture API is substituted.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time

LIBRARIES = {
    "m3_collections_mask": ["m3_index_mask_compactor.c", "m3_index_mask_window.c"],
    "m3_collections_pathmask": ["m3_index_path_masks.c"],
    "m3_collections_address": ["m3_long_address_kernel.c"],
    "m3rankbatch": ["m3_long_rank_batch_jni.c"],
    "m3_residency_state_mask": ["m3_residency_state_mask.c"],
    "m3signalorder": ["m3_signal_order_jni.c"],
    "m3_collections_span": ["m3_span_index_jni.c"],
    "m3_collections_statehist": ["m3_state_mask_histogram.c"],
    "m3bitlane": ["segmented_bit_lane.c"],
}
# Existing published standalone oracles retain their original assertions.
CONTRACTS = [
    ("M3MaskWindowSparseContract", ["native"], "M3MaskWindowSparseContract"),
    ("M3BudgetedMaskWindowContract", ["--native"], "BUDGETED_MASK_WINDOW"),
    ("M3IndexMaskWindowSelfTest", ["--native"], "M3_INDEX_MASK_WINDOW"),
    ("M3IndexMaskWindowRecoverySelfTest", ["--native"], "M3IndexMaskWindowRecoverySelfTest"),
    ("M3IndexMaskCompactorSelfTest", ["--native"], "M3IndexMaskCompactorSelfTest"),
    ("M3IndexPathMasksSelfTest", ["--native"], "M3IndexPathMasksSelfTest"),
    ("M3IndexPathMasksLongSelfTest", ["--native"], "M3IndexPathMasksLongSelfTest"),
    ("M3IndexPathMaskAliasSelfTest", ["--native"], "PATH_MASK_ALIAS_PASS"),
    ("M3IndexPathImpossibleStageSelfTest", ["--native"], "M3_IMPOSSIBLE_STAGE_MASK_PASS"),
    ("M3IndexPathZeroStagesSelfTest", ["--native"], "M3_ZERO_STAGES_PATH_MASK_PASS"),
    ("M3IndexPathZeroWindowSelfTest", ["--native"], "M3_ZERO_INDEX_WINDOW_PASS"),
    ("M3IndexPathMaskWindowSelfTest", ["--native"], "M3_PATH_MASK_WINDOW_PASS"),
    ("M3IndexPathRankNativeSelfTest", [], "M3IndexPathRank JNI parity"),
    ("M3LongIndexPathMasksSelfTest", [], "M3LongIndexPathMasksSelfTest"),
    ("M3MaskSpansContract", ["native"], "MASK_SPANS_PASS"),
    ("M3PathMaskAliasContract", ["native"], "M3_PATH_MASK_ALIAS"),
    ("M3OrderWindowProof", ["common", "jni"], "ORDER_WINDOW"),
    ("M3SignalOrderContract", ["native"], "M3SignalOrderContract"),
    ("M3SignalDimensionsNativeContract", [], "M3SignalDimensionsNativeContract"),
]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def symbols(text):
    return set(re.findall(r"\b(Java_[A-Za-z0-9_]+)\s*\(", text))


def prototypes(header):
    return {name: " ".join((result + " (" + arguments + ")").split())
            for result, name, arguments in re.findall(
                r"JNIEXPORT\s+(\w+)\s+JNICALL\s+(Java_\w+)\s*\(([^;]+)\);",
                header, flags=re.S)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--target", required=True, type=Path)
    parser.add_argument("--sorts-patch", required=True, type=Path)
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--preimage-tree", required=True, type=Path)
    parser.add_argument("--parity-test", type=Path)
    parser.add_argument("--span-test", type=Path)
    parser.add_argument("--candidate", action="store_true",
                        help="Label qualification as candidate-only before canonical product replay")
    args = parser.parse_args()
    target, patch, out = args.target.resolve(), args.sorts_patch.resolve(), args.output.resolve()
    baseline = args.preimage_tree.resolve()
    module = target / "m3/collections"
    before_module = baseline / "m3/collections"
    java_home = args.java_home.resolve()
    parity = (args.parity_test or target / "m3/verification/sorting/src/test/java/com/m3/collections/M3SortsNativeParityContract.java").resolve()
    span = (args.span_test or module / "src/test/native/m3_mask_window_spans.c").resolve()
    if out.exists() and any(out.iterdir()):
        parser.error("output must be new or empty")
    required = [module / "src/main/java", before_module / "src/main/java", parity, span,
                patch / "java/util/Sorts.class", patch / "java/util/SortAlgorithm.class"]
    for path in required:
        if not path.exists():
            parser.error("required real input is missing: " + str(path))
    for tool in ("java", "javac"):
        if not (java_home / "bin" / tool).is_file():
            parser.error("missing Java tool: " + tool)
    gcc, nm = shutil.which("gcc"), shutil.which("nm")
    if not gcc or not nm:
        parser.error("gcc and nm are required")
    out.mkdir(parents=True, exist_ok=True)
    for folder in ("classes", "before-classes", "test-classes", "headers", "before-headers",
                   "native", "native-absent", "test-source/com/m3/collections"):
        (out / folder).mkdir(parents=True, exist_ok=True)
    environment = os.environ.copy()
    removed = []
    for key in ("JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS", "CLASSPATH",
                "LD_PRELOAD", "LD_LIBRARY_PATH"):
        if key in environment:
            removed.append(key)
            environment.pop(key)
    environment.update(UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1", LC_ALL="C", TZ="UTC")
    receipt = {"schema": 1, "status": "RUNNING", "candidate_only": args.candidate,
               "scope": "actual collections Java/native owners and actual java.base sorting patch",
               "target": str(target), "preimage_tree": str(baseline), "sorts_patch": str(patch),
               "removed_environment_keys": removed, "commands": [], "libraries": {},
               "limitations": ["Linux/JDK21 scoped gate; no full JDK image, jtreg, JCK or other platform gate",
                               "JNI transferred-element counts are not elapsed-time performance measurements",
                               "Existing JNI methods have no progress callback; no new cancellation promise",
                               "Owning-module Maven tests and canonical recipe replay are separate gates"]}

    def save():
        (out / "EXECUTION.json").write_text(json.dumps(receipt, indent=2) + "\n")

    def run(label, command, expected_code=0, expected_text=None):
        command = list(map(str, command))
        started = time.monotonic()
        result = subprocess.run(command, cwd=target, env=environment, capture_output=True,
                                text=True, timeout=300)
        stdout, stderr = out / (label + ".stdout.log"), out / (label + ".stderr.log")
        stdout.write_text(result.stdout)
        stderr.write_text(result.stderr)
        combined = result.stdout + result.stderr
        diagnostic = re.search(r"WARNING[^\n]*(?:JNI|native method)|FATAL ERROR in native method|"
                               r"runtime error:|UndefinedBehaviorSanitizer", combined, re.I) is not None
        okay = (result.returncode == expected_code and not diagnostic
                and (expected_text is None or expected_text in combined))
        receipt["commands"].append({"label": label, "argv": command,
            "returncode": result.returncode, "expected_returncode": expected_code,
            "expected_text": expected_text, "jni_or_ubsan_diagnostic": diagnostic,
            "seconds": round(time.monotonic() - started, 6),
            "stdout": stdout.name, "stdout_sha256": digest(stdout),
            "stderr": stderr.name, "stderr_sha256": digest(stderr),
            "status": "PASS" if okay else "FAIL"})
        save()
        print(label + ": " + ("PASS" if okay else "FAIL"), flush=True)
        if not okay:
            raise RuntimeError(label + " failed; inspect " + str(stdout) + " and " + str(stderr))
        return combined

    def compile_main(source_module, prefix, class_folder, header_folder):
        sources = sorted((source_module / "src/main/java").rglob("*.java"))
        run(prefix, [java_home / "bin/javac", "--release", "21", "-Xlint:all,-module", "-Werror",
                     "-d", out / class_folder, "-h", out / header_folder, *sources])
        return sources

    try:
        run("java-version", [java_home / "bin/java", "-version"])
        run("javac-version", [java_home / "bin/javac", "-version"])
        run("gcc-version", [gcc, "--version"])
        main_sources = sorted((module / "src/main/java").rglob("*.java"))
        before_sources = sorted((before_module / "src/main/java").rglob("*.java"))
        native_sources = [module / "src/main/native/collections" / name
                          for names in LIBRARIES.values() for name in names]
        test_sources = [module / "src/test/java/com/m3/collections" / (name + ".java")
                        for name, _, _ in CONTRACTS]
        all_inputs = list(dict.fromkeys(main_sources + before_sources + native_sources
                                       + test_sources + [parity, span]
                                       + sorted(patch.rglob("*.class"))))
        receipt["inputs"] = {str(path): digest(path) for path in all_inputs}
        receipt["main_java_sources"] = len(main_sources)
        compile_main(module, "actual-main-java21", "classes", "headers")
        compile_main(before_module, "baseline-main-java21", "before-classes", "before-headers")
        before_abi, after_abi = {}, {}
        for header in (out / "before-headers").glob("*.h"):
            before_abi.update(prototypes(header.read_text()))
        for header in (out / "headers").glob("*.h"):
            after_abi.update(prototypes(header.read_text()))
        for name, signature in before_abi.items():
            if after_abi.get(name) != signature:
                raise RuntimeError("existing JNI prototype removed or changed: " + name)
        receipt["jni_abi"] = {"retained_preimage": before_abi, "candidate": after_abi,
                              "additions": sorted(after_abi.keys() - before_abi.keys())}
        flags = [gcc, "-std=c17", "-O2", "-Wall", "-Wextra", "-Werror", "-fsanitize=undefined",
                 "-fno-sanitize-recover=all", "-fno-omit-frame-pointer",
                 "-I" + str(java_home / "include"), "-I" + str(java_home / "include/linux"),
                 "-I" + str(out / "headers")]
        for library, names in LIBRARIES.items():
            sources = [module / "src/main/native/collections" / name for name in names]
            expected = set().union(*(symbols(path.read_text()) for path in sources))
            includes = []
            for owner in sorted({name.removeprefix("Java_").rsplit("_", 1)[0] for name in expected}):
                includes += ["-include", out / "headers" / (owner + ".h")]
            destination = out / "native" / ("lib" + library + ".so")
            run(library + "-c17-ubsan", flags + ["-fPIC", "-shared", "-Wl,-z,defs"]
                + includes + sources + ["-o", destination])
            exported = run(library + "-exports", [nm, "-D", "--defined-only", destination])
            actual = set(re.findall(r"\bJava_[A-Za-z0-9_]+\b", exported))
            if expected != actual or not expected <= after_abi.keys():
                raise RuntimeError("native exports/headers disagree: " + library)
            receipt["libraries"][library] = {"sha256": digest(destination), "exports": sorted(actual)}
        # A filename ending in .txt is allowed only as an explicit candidate input.
        parity_copy = out / "test-source/com/m3/collections/M3SortsNativeParityContract.java"
        shutil.copyfile(parity, parity_copy)
        run("actual-contracts-java21", [java_home / "bin/javac", "--release", "21", "-Xlint:all,-module",
            "-Werror", "--patch-module", "java.base=" + str(patch), "-cp", out / "classes",
            "-d", out / "test-classes", *test_sources, parity_copy])
        span_copy = out / "m3_mask_window_spans.c"
        shutil.copyfile(span, span_copy)
        window_source = module / "src/main/native/collections/m3_index_mask_window.c"
        baseline_window = before_module / "src/main/native/collections/m3_index_mask_window.c"
        run("span-candidate-c17-ubsan", flags + [span_copy, window_source, "-o", out / "span-candidate"])
        run("span-candidate-proof", [out / "span-candidate"], expected_text=
            "JNI_SPANS_PASS single=1/128 dense_limit1=1/128 cross_chunk=130/300 faults=3")
        run("span-baseline-c17-ubsan", flags + [span_copy, baseline_window, "-o", out / "span-baseline"])
        run("span-baseline-counterexample", [out / "span-baseline"], expected_code=1,
            expected_text="span assertion failed")
        java = [java_home / "bin/java", "-ea", "-Xcheck:jni", "-Xmx768m", "-XX:ActiveProcessorCount=2",
                "--patch-module", "java.base=" + str(patch), "-Dm3.collections.signal.native=true",
                "-cp", str(out / "test-classes") + os.pathsep + str(out / "classes")]
        for name, arguments, expected in CONTRACTS:
            run(name + "-native", java + ["-Djava.library.path=" + str(out / "native"),
                "com.m3.collections." + name, *arguments], expected_text=expected)
        run("signal-sorts-native-parity", java + ["-Djava.library.path=" + str(out / "native"),
            "com.m3.collections.M3SortsNativeParityContract", "native"],
            expected_text="realJNI=true sorts=LSD_RADIX")
        run("signal-missing-native-refusal", java + ["-Djava.library.path=" + str(out / "native-absent"),
            "com.m3.collections.M3SortsNativeParityContract", "missing"], expected_text="mode=missing")
        run("pathmask-input-alias-missing-refusal", java + ["-Djava.library.path=" + str(out / "native-absent"),
            "com.m3.collections.M3PathMaskAliasContract", "missing"],
            expected_text="mode=missing")
        run("pathmask-missing-native-refusal", java + ["-Djava.library.path=" + str(out / "native-absent"),
            "com.m3.collections.M3IndexPathMaskAliasSelfTest", "--missing"],
            expected_text="backend=NATIVE_REQUIRED")
        for path, before in receipt["inputs"].items():
            if digest(Path(path)) != before:
                raise RuntimeError("input source changed during qualification: " + path)
        receipt["status"] = "PASS_CANDIDATE_SCOPE" if args.candidate else "PASS_RECEIVER_SCOPE"
        receipt["final_input_recheck"] = "PASS"
        save()
        print(receipt["status"] + " libraries=" + str(len(LIBRARIES))
              + " exports=" + str(sum(len(x["exports"]) for x in receipt["libraries"].values())), flush=True)
    except BaseException as error:
        receipt["status"] = "FAIL"
        receipt["failure"] = str(error)
        save()
        raise


if __name__ == "__main__":
    main()
