#!/usr/bin/env python3
"""Source-bound common JNI qualification, using only the Python standard library.

Linux execution is supported. Apple and old-kernel branches are syscall models.
Supply --baseline for an exact before/after differential; omit it for current-head
CI qualification. Output must be outside the checkout or ignored by Git.
"""
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0

import argparse
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shlex
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
OWNER = Path("src/java.base/share/native/libjava/M3StringPool.c")
HELPER = OWNER.parent / "m3_jni_util.h"
CONTRACTS = (
    (OWNER, "src/java.base/share/classes/java/lang/M3StringPool.java",
     "java.lang.M3StringPool", ("mapLexicon0", "unmapLexicon0")),
    (OWNER.parent / "M3StringRegexDp.c", "src/java.base/share/classes/java/lang/M3StringRegexDp.java",
     "java.lang.M3StringRegexDp", ("advance0",)),
    (OWNER.parent / "M3RegexDpPlan.c", "src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java",
     "jdk.internal.mindex.M3RegexDpPlan", ("compose0", "composeStructured0", "repeat0")),
    (OWNER.parent / "M3RX7.c", "src/java.base/share/classes/jdk/internal/mindex/M3RX7.java",
     "jdk.internal.mindex.M3RX7", ("buildPacked0",)),
    (OWNER.parent / "M3BitLane28.c", "src/java.base/share/classes/jdk/internal/mindex/M3BitLane28.java",
     "jdk.internal.mindex.M3BitLane28", ("cardinality0",)),
    (OWNER.parent / "M3StateRelation.c", "src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java",
     "jdk.internal.mindex.M3StateRelation", ("composeNative", "repeatNative")),
    (Path("m3/collections/src/main/native/collections/m3_index_mask_window.c"),
     "m3/collections/src/main/java/com/m3/collections/M3IndexMaskWindow.java",
     "com.m3.collections.M3IndexMaskWindow", ("scanNative", "scanBoundedNative")),
    (Path("m3/collections/src/main/native/collections/m3_index_path_masks.c"),
     "m3/collections/src/main/java/com/m3/collections/M3IndexPathMasks.java",
     "com.m3.collections.M3IndexPathMasks", ("selectNative",)),
    (Path("m3/collections/src/main/native/collections/m3_long_index_path_masks_jni.c"),
     "m3/collections/src/main/java/com/m3/collections/M3LongIndexPathMasksJNI.java",
     "com.m3.collections.M3LongIndexPathMasksJNI", ("selectNative", "scanWindowNative")),
    (Path("m3/collections/src/main/native/collections/m3_state_mask_histogram.c"),
     "m3/collections/src/main/java/com/m3/collections/M3StateMaskHistogram.java",
     "com.m3.collections.M3StateMaskHistogram", ("tallyNative",)),
    (Path("m3/collections/src/main/native/collections/m3_index_mask_compactor.c"),
     "m3/collections/src/main/java/com/m3/collections/M3IndexMaskCompactor.java",
     "com.m3.collections.M3IndexMaskCompactor", ("selectNative", "rangeAbiNative", "selectRangeNative",
         "positionsAbiNative", "selectPositionsNative", "positionsMasksAbiNative", "selectPositionsMasksNative",
         "selectWindowNative", "selectLongWindowNative")),
    (Path("m3/collections/src/main/native/collections/segmented_bit_lane.c"),
     "m3/collections/src/main/java/com/m3/collections/M3BitLane28.java",
     "com.m3.collections.M3BitLane28", ("cardinality0",)),
    (Path("m3/collections/src/main/native/collections/m3_residency_state_mask.c"),
     "m3/collections/src/main/java/com/m3/collections/M3ResidencyStateMaskIndex.java",
     "com.m3.collections.M3ResidencyStateMaskIndex", ("select0",)),
)
DEPENDENCIES = (OWNER.parent / "m3_state_relation.h",)
FIXTURES = ("run_native_tests.py", "m3_string_pool_model.c", "m3_jni_util_contract.c",
            "m3_string_pool_jni_smoke.c", "M3StringPoolNativeTest.java")


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def without_comments_and_literals(text):
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/'
    return re.sub(pattern, lambda m: "\n" * m.group().count("\n") + " ", text, flags=re.S)


def native_declarations(java_file, class_name, expected_methods):
    source = without_comments_and_literals(java_file.read_text())
    package, simple = class_name.rsplit(".", 1)
    if re.search(r"\bpackage\s+" + re.escape(package) + r"\s*;", source) is None:
        raise AssertionError(f"Unexpected package in {java_file}")
    if re.search(r"\bclass\s+" + re.escape(simple) + r"\b", source) is None:
        raise AssertionError(f"Unexpected owner class in {java_file}")
    pattern = (r"(?m)^[ \t]*((?:(?:public|private|protected|static|final|synchronized)\s+)*"
               r"native\s+[\w.$]+(?:\s*\[\s*\])*\s+(\w+)\s*\([^;{}]*\)"
               r"\s*(?:throws\s+[^;{}]+)?;)")
    found = [(match.group(2), " ".join(match.group(1).split()))
             for match in re.finditer(pattern, source)]
    if sorted(name for name, _ in found) != sorted(expected_methods):
        raise AssertionError(f"Native method set changed in {java_file}: {found}")
    if any(re.search(r"\bstatic\s+native\b", declaration) is None for _, declaration in found):
        raise AssertionError(f"Expected static native owner methods in {java_file}")
    return [declaration for _, declaration in sorted(found)]


def c_signatures(c_file, class_name, expected_methods):
    source = without_comments_and_literals(c_file.read_text())
    found = [" ".join(match.group().split()) for match in re.finditer(
        r"JNIEXPORT\s+\w+\s+JNICALL\s+Java_\w+\s*\([^)]*\)", source)]
    names = [re.search(r"\b(Java_\w+)\s*\(", declaration).group(1) for declaration in found]
    expected = ["Java_" + class_name.replace(".", "_") + "_" + name for name in expected_methods]
    if sorted(names) != sorted(expected):
        raise AssertionError(f"JNI symbol set changed in {c_file}: {names}")
    return [declaration for _, declaration in sorted(zip(names, found))]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--candidate", required=True, type=Path, help="M3JDK21 product source root")
    parser.add_argument("--baseline", type=Path, help="Optional exact before source root")
    parser.add_argument("--jdk", type=Path, default=os.environ.get("JAVA_HOME"), help="JDK root; defaults to JAVA_HOME")
    parser.add_argument("--out", required=True, type=Path, help="Dedicated output directory outside source or ignored by Git")
    parser.add_argument("--expected-head", default=os.environ.get("EXPECTED_HEAD"), help="Require this checked-out Git commit")
    parser.add_argument("--cc", default=os.environ.get("CC", "cc"))
    parser.add_argument("--cxx", default=os.environ.get("CXX", "c++"))
    args = parser.parse_args()
    if platform.system() != "Linux":
        parser.error("Real execution in this suite is Linux-only; Apple branch evidence is modeled.")
    if args.jdk is None:
        parser.error("Supply --jdk or JAVA_HOME.")
    candidate = args.candidate.resolve()
    baseline = args.baseline.resolve() if args.baseline else None
    jdk, out = args.jdk.resolve(), args.out.resolve()
    cc, cxx = shlex.split(args.cc), shlex.split(args.cxx)
    if not cc or not cxx or shutil.which(cc[0]) is None or shutil.which(cxx[0]) is None:
        parser.error("C17 and C++20 compilers are required.")
    for path in (jdk / "bin/javac", jdk / "bin/java", jdk / "include/jni.h",
                 jdk / "include/linux/jni_md.h", candidate / HELPER):
        if not path.is_file():
            parser.error(f"Required file not found: {path}")
    roots = [("candidate", candidate)] + ([("baseline", baseline)] if baseline else [])
    for _, root in roots:
        for c_path, java_path, _, _ in CONTRACTS:
            for path in (root / c_path, root / java_path):
                if not path.is_file():
                    parser.error(f"Required source owner not found: {path}")
        for dependency in DEPENDENCIES:
            if not (root / dependency).is_file():
                parser.error(f"Required native dependency not found: {root / dependency}")
    for root in [candidate, baseline, HERE]:
        if root is not None and (out == root or root.is_relative_to(out)):
            parser.error("Output cannot contain a source root.")
        if root is not None and out.is_relative_to(root):
            ignored = subprocess.run(["git", "-C", str(root), "check-ignore", "--quiet", str(out)],
                                     capture_output=True)
            if ignored.returncode != 0:
                parser.error("Output inside a source root must be ignored by Git; use a temporary directory.")
    out.mkdir(parents=True, exist_ok=True)
    marker = out / "output-owner.json"
    marker_value = {"schema": "m3-common-jni-test-output-v1"}
    if marker.exists():
        if json.loads(marker.read_text()) != marker_value:
            parser.error("Output directory belongs to another producer.")
    elif any(out.iterdir()):
        parser.error("Output directory must be empty or a previous output of this runner.")
    marker.write_text(json.dumps(marker_value, sort_keys=True) + "\n")
    work, logs = out / "work", out / "logs"
    for generated in (work, logs):
        if generated.exists():
            shutil.rmtree(generated)
        generated.mkdir()

    substitutions = sorted([(str(HERE), "$FIXTURES"), (str(candidate), "$CANDIDATE"),
                            (str(jdk), "$JDK"), (str(out), "$OUT")]
                           + ([(str(baseline), "$BASELINE")] if baseline else []),
                           key=lambda row: -len(row[0]))

    def normalized(value):
        for source, replacement in substitutions:
            value = value.replace(source, replacement)
        return value

    receipt = {
        "schema": "m3-common-jni-qualification-v1",
        "status": "RUNNING",
        "host": {"system": platform.system(), "machine": platform.machine()},
        "mode": "baseline-differential" if baseline else "current-only",
        "inputs": {}, "commands": [], "results": {}, "generated_outputs": {},
        "limitations": [
            "Apple source branching uses host Linux headers and modeled syscalls; no Apple SDK or kernel is executed.",
            "Missing-NOREPLACE modeling is not execution on an old Linux/Android kernel.",
            "MSVC_KEYWORD_MODEL forces _MSC_VER on the Linux host compiler; no MSVC compiler, Windows SDK or Windows runtime is executed.",
            "A stock checked JVM registers actual product C functions to a fixture class; this is not an installed source-built M3JDK21 image.",
            "Windows, Android and iOS execution and whole-runtime qualification remain separate gates.",
        ],
    }
    if not baseline:
        receipt["results"]["before-after-differential"] = "NOT_RUN: no baseline source tree supplied"

    source_inputs = {}
    for label, root in roots:
        for c_path, java_path, _, _ in CONTRACTS:
            source_inputs[f"{label}/{c_path.as_posix()}"] = root / c_path
            source_inputs[f"{label}/{java_path}"] = root / java_path
        for dependency in DEPENDENCIES:
            source_inputs[f"{label}/{dependency.as_posix()}"] = root / dependency
    source_inputs[f"candidate/{HELPER.as_posix()}"] = candidate / HELPER
    for header in ("include/jni.h", "include/linux/jni_md.h"):
        source_inputs[f"toolchain/{header}"] = jdk / header
    for name in FIXTURES:
        source_inputs[f"fixtures/{name}"] = HERE / name
    for key, path in source_inputs.items():
        receipt["inputs"][key] = digest(path)

    def run(label, command):
        command = [str(value) for value in command]
        result = subprocess.run(command, text=True, capture_output=True, timeout=90)
        (logs / f"{label}.stdout").write_text(normalized(result.stdout))
        (logs / f"{label}.stderr").write_text(normalized(result.stderr))
        receipt["commands"].append({"label": label, "argv": [normalized(value) for value in command],
                                    "exit_code": result.returncode})
        if result.returncode:
            raise RuntimeError(f"{label} failed ({result.returncode})\n{result.stdout}\n{result.stderr}")
        return result

    def finish(status, error=None):
        receipt["status"] = status
        if error is not None:
            receipt["error"] = normalized(str(error))
        for directory in (work, logs):
            for path in sorted(directory.rglob("*")):
                if path.is_file():
                    receipt["generated_outputs"][path.relative_to(out).as_posix()] = digest(path)
        (out / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")

    try:
        if args.expected_head:
            top = run("git-root", ["git", "-C", candidate, "rev-parse", "--show-toplevel"]).stdout.strip()
            if Path(top).resolve() != candidate:
                raise AssertionError("--candidate is not the expected checkout root")
            observed = run("git-head", ["git", "-C", candidate, "rev-parse", "HEAD"]).stdout.strip()
            if observed != args.expected_head:
                raise AssertionError(f"Expected head {args.expected_head}, observed {observed}")
            head_inputs = []
            for key, path in source_inputs.items():
                if key.startswith(("candidate/", "fixtures/")):
                    if not path.is_relative_to(candidate):
                        raise AssertionError("Exact-head verification requires fixture code inside the product checkout")
                    head_inputs.append(path.relative_to(candidate).as_posix())
            run("git-inputs-tracked", ["git", "-C", candidate, "ls-files", "--error-unmatch", "--", *head_inputs])
            run("git-inputs-at-head", ["git", "-C", candidate, "diff", "--exit-code", "HEAD", "--", *head_inputs])
            receipt["checked_out_head"] = observed
            receipt["head_inputs_unchanged"] = True
        for label, command in (("cc-version", [*cc, "--version"]), ("cxx-version", [*cxx, "--version"]),
                               ("java-version", [jdk / "bin/java", "-version"])):
            run(label, command)
        include_common = ["-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux")]
        prefix_maps = ["-ffile-prefix-map=" + source + "=" + label for source, label in substitutions]
        common = ["-O2", "-Wall", "-Wextra", "-Werror", *include_common, *prefix_maps]
        java_abis, c_abis, headers_by_root = {}, {}, {}
        for label, root in roots:
            abi_root = work / label / "abi-source"
            headers = work / label / "headers"
            classes = work / label / "abi-classes"
            objects = work / label / "objects"
            for directory in (abi_root, headers, classes, objects):
                directory.mkdir(parents=True, exist_ok=True)
            java_abis[label], c_abis[label] = {}, {}
            stubs = []
            for c_path, java_path, class_name, methods in CONTRACTS:
                declarations = native_declarations(root / java_path, class_name, methods)
                java_abis[label][class_name] = declarations
                c_abis[label][class_name] = c_signatures(root / c_path, class_name, methods)
                if label == "candidate":
                    includes = re.findall(r'^\s*#include\s+"([^"\n]*m3_jni_util\.h)"',
                                          (root / c_path).read_text(), flags=re.M)
                    if len(includes) != 1 or (root / c_path).parent.joinpath(includes[0]).resolve() != (candidate / HELPER).resolve():
                        raise AssertionError(f"Shared helper ownership differs in {c_path}")
                package, simple = class_name.rsplit(".", 1)
                stub = abi_root / (class_name.replace(".", "/") + ".java")
                stub.parent.mkdir(parents=True, exist_ok=True)
                stub.write_text(f"package {package};\nimport java.nio.ByteBuffer;\nimport java.io.IOException;\n"
                                + f"public final class {simple} {{\n    "
                                + "\n    ".join(declarations) + "\n}\n")
                stubs.append(stub)
            run("generate-headers-" + label, [jdk / "bin/javac", "--patch-module", f"java.base={abi_root}",
                                              "-h", headers, "-d", classes, *stubs])
            headers_by_root[label] = headers
            for c_path, _, class_name, _ in CONTRACTS:
                generated_header = headers / (class_name.replace(".", "_") + ".h")
                if not generated_header.is_file():
                    raise AssertionError(f"JNI header missing: {generated_header}")
                run("compile-owner-" + label + "-" + c_path.stem, [*cc, "-std=c17", *common, "-fPIC",
                    "-I" + str(headers), "-I" + str(root / OWNER.parent), "-include", generated_header,
                    "-c", root / c_path, "-o", objects / (c_path.stem + ".o")])
            receipt["results"][label + "-jni-abi"] = {
                "native_owners": len(CONTRACTS), "generated_from_actual_java_declarations": True,
                "c_symbols": c_abis[label], "java_declarations": java_abis[label],
            }
        if baseline:
            if c_abis["baseline"] != c_abis["candidate"] or java_abis["baseline"] != java_abis["candidate"]:
                raise AssertionError("Native/Java ABI declarations differ from baseline")
            receipt["results"]["native-owner-before-after-abi"] = "IDENTICAL"

        transcripts = {}
        model_modes = ([("baseline-linux", "baseline", baseline, [])] if baseline else []) + [
            ("candidate-linux", "candidate", candidate, []),
            ("candidate-linux-hint-model", "candidate", candidate, ["-DM3_MODEL_NO_NOREPLACE=1"]),
            ("candidate-apple-model", "candidate", candidate, ["-DM3_MODEL_APPLE=1"]),
        ]
        for label, source_label, root, definitions in model_modes:
            executable = work / label
            run("compile-" + label, [*cc, "-std=c17", *common,
                "-I" + str(headers_by_root[source_label]), "-I" + str(root / OWNER.parent),
                f'-DM3_POOL_SOURCE="{root / OWNER}"', *definitions,
                HERE / "m3_string_pool_model.c", "-o", executable])
            result = run("run-" + label, [executable])
            count = int(re.search(r"MODEL_CHECKS=(\d+)", result.stdout).group(1))
            receipt["results"][label] = {"checks": count, "kind": "deterministic-model"}
            transcripts[label] = result.stdout
        if baseline:
            if transcripts["baseline-linux"] != transcripts["candidate-linux"]:
                raise AssertionError("Linux before/after model outcomes or JNI cleanup differ")
            receipt["results"]["before-after-differential"] = "IDENTICAL 24-case Linux transcript"
        for language, compiler, standard in (("c", cc, "c17"), ("c++", cxx, "c++20")):
            label = "helper-" + language.replace("+", "p")
            executable = work / label
            run("compile-" + label, [*compiler, "-x", language, "-std=" + standard, *common,
                "-I" + str(candidate / OWNER.parent), HERE / "m3_jni_util_contract.c", "-o", executable])
            result = run("run-" + label, [executable])
            receipt["results"][label] = {"checks": int(re.search(r"HELPER_CHECKS=(\d+)", result.stdout).group(1)),
                                         "kind": "JNI-table-contract-model"}
        # Compile the actual header's Microsoft C keyword branch with the host
        # compiler. This is a macro/keyword model, not a Windows compiler test.
        label = "MSVC_KEYWORD_MODEL"
        model_command = [*cc, "-x", "c", "-std=c17", *common, "-D_MSC_VER=1930",
                         "-I" + str(candidate / OWNER.parent), HERE / "m3_jni_util_contract.c"]
        preprocessed = run("preprocess-" + label, [*model_command, "-E", "-P"])
        if re.search(r"static\s+__inline\s+void\s+m3_jni_throw_by_name\s*\(", preprocessed.stdout) is None:
            raise AssertionError("MSVC_KEYWORD_MODEL did not select the __inline declaration")
        executable = work / label
        run("compile-" + label, [*model_command, "-o", executable])
        result = run("run-" + label, [executable])
        receipt["results"][label] = {
            "checks": int(re.search(r"HELPER_CHECKS=(\d+)", result.stdout).group(1)),
            "kind": "MSVC_KEYWORD_MODEL", "selected_declaration": "static __inline void",
            "windows_build_or_runtime_proof": False,
        }
        jvm_classes = work / "jvm-classes"
        jvm_classes.mkdir()
        run("compile-jvm-fixture", [jdk / "bin/javac", "-d", jvm_classes, HERE / "M3StringPoolNativeTest.java"])
        for label, root in roots:
            library = work / f"libm3_pool_test_{label}.so"
            run("compile-jvm-" + label, [*cc, "-std=c17", *common, "-fPIC", "-shared",
                "-I" + str(headers_by_root[label]), "-I" + str(root / OWNER.parent),
                f'-DM3_POOL_SOURCE="{root / OWNER}"', HERE / "m3_string_pool_jni_smoke.c", "-o", library])
            result = run("jvm-" + label, [jdk / "bin/java", "-Xcheck:jni",
                "-Dm3.test.library=" + str(library), "-cp", jvm_classes, "M3StringPoolNativeTest"])
            if re.search(r"WARNING|FATAL ERROR|JNI DETECTED ERROR", result.stdout + result.stderr):
                raise AssertionError("Checked JNI reported a warning or error")
            receipt["results"]["jvm-linux-" + label] = {
                "checks": int(re.search(r"JVM_LINUX_CHECKS=(\d+)", result.stdout).group(1)),
                "kind": "actual-production-C-real-JVM-and-Linux-mmap", "jni_checking": True,
            }
        for key, path in source_inputs.items():
            if digest(path) != receipt["inputs"][key]:
                raise AssertionError(f"Source changed during verification: {path}")
        finish("PASS")
        print(json.dumps({"status": "PASS", "mode": receipt["mode"], "receipt": str(out / "receipt.json"),
                          "results": {key: value for key, value in receipt["results"].items()
                                      if not key.endswith("-jni-abi")}}, indent=2, sort_keys=True))
    except (RuntimeError, AssertionError, subprocess.TimeoutExpired, OSError, ValueError) as error:
        finish("FAIL", error)
        raise


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, AssertionError, subprocess.TimeoutExpired, OSError, ValueError) as failure:
        print(str(failure), file=sys.stderr)
        raise SystemExit(1)
