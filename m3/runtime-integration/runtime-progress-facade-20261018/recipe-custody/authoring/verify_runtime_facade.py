#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify exact original-recipe Runtime outputs with a stock Java 21 toolchain.

This consumes original SDK qualification and writes only an isolated proof tree.
It does not receive a product or alter the original recipe implementation.
"""
from pathlib import Path
import argparse
import hashlib
import importlib.util
import json
import re
import shutil
import subprocess
import time


DATASET = "m3jdk21-runtime-facade"
RUNTIME = "src/java.base/share/classes/java/lang/Runtime.java"
TEST_ROOT = "test/jdk/java/lang/Runtime"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def class_tree(path):
    return {p.relative_to(path).as_posix(): sha(p) for p in sorted(path.rglob("*.class"))}


def api_members(text):
    return re.findall(r"(?m)^  (.+;)\n    descriptor: (.+)$", text)


def main():
    parser = argparse.ArgumentParser()
    for name in ("crate", "recipe-owner", "baseline", "source-receipt", "jdk", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    crate = args.crate.resolve()
    spec = importlib.util.spec_from_file_location("original_runtime_facade_receiver", crate / "receive.py")
    receiver = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(receiver)
    qualified = receiver.qualification(crate, DATASET, args.recipe_owner.resolve())
    candidate = qualified["candidate"]
    if set(qualified["outputs"]) != {
        RUNTIME, TEST_ROOT + "/M3RuntimeProgressFacade.java", TEST_ROOT + "/M3RuntimeLegacyClient.java",
        "m3/runtime-integration/runtime-progress-facade-20261018/CATALOGUE.tsv",
        "m3/runtime-integration/runtime-progress-facade-20261018/README.md",
        "m3/runtime-integration/runtime-progress-facade-20261018/RECEIVING-MAP.json",
    }:
        raise ValueError("unexpected Runtime facade recipe target set")
    closure = json.loads(args.source_receipt.read_text())
    if len(closure["files"]) != 5:
        raise ValueError("expected exactly Runtime plus four existing progress/executor owners")
    for row in closure["files"]:
        if sha(args.baseline / row["path"]) != row["sha256"]:
            raise ValueError("baseline dependency drift: " + row["path"])
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    bin_dir = args.jdk.resolve() / "bin"
    javac = str(bin_dir / "javac")
    java = str(bin_dir / "java")
    javap = str(bin_dir / "javap")
    observations = []
    receipt = {
        "schema": "M3_RUNTIME_FACADE_PATCH_AND_LEGACY_QUALIFICATION_V1", "status": "RUNNING",
        "sdk_inputs_sha256": sha(candidate / "SDK_INPUTS.tsv"),
        "sdk_results_sha256": sha(candidate / "SDK_RESULTS.tsv"),
        "source_receipt_sha256": sha(args.source_receipt),
        "source_closure": closure["files"], "commands": observations,
        "scope": "exact java.base patch on a stock Java21 VM; full custom image and ordered receiving promotion remain separate gates",
        "new_fields": 0, "jni_source_changes": 0, "performance_claim": "none",
    }

    def run(label, command, expect=0, marker=None):
        log = output / (label + ".log")
        started = time.time()
        with log.open("w") as stream:
            result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT, timeout=90)
        text = log.read_text()
        observations.append({"label": label, "command": command, "exit_code": result.returncode,
            "elapsed_seconds": time.time() - started, "log_sha256": sha(log)})
        if (result.returncode != 0) != (expect != 0):
            raise AssertionError("unexpected exit for " + label + ": " + text)
        if marker is not None and marker not in text:
            raise AssertionError("missing witness marker for " + label + ": " + text)
        return text

    try:
        version = run("javac-version", [javac, "-version"])
        if not version.startswith("javac 21."):
            raise ValueError("an unmodified Java21 compiler is required")
        stock_api = run("stock-runtime-api", [javap, "-p", "-s", "java.lang.Runtime"])
        if "progressMonitor(" in stock_api or "subProgressMonitor(" in stock_api:
            raise ValueError("stock Runtime already contains the candidate facade")
        classes = {}
        compile_flags = ["-g:none", "-proc:none", "-Xlint:all", "-Xlint:-this-escape", "-Werror",
            "-Xdoclint:all/protected", "-Xdoclint/package:java.*,javax.*"]
        for variant in ("baseline", "candidate"):
            source_root = output / variant / "source"
            classes[variant] = output / variant / "classes"
            classes[variant].mkdir(parents=True)
            sources = []
            for row in closure["files"]:
                relative = row["path"]
                source = candidate / relative if variant == "candidate" and relative == RUNTIME else args.baseline / relative
                target = source_root / relative
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(source, target)
                sources.append(str(target))
            compiled = run(variant + "-compile", [javac, *compile_flags, "--patch-module",
                "java.base=" + str(source_root / "src/java.base/share/classes"),
                "-d", str(classes[variant]), *sorted(sources)])
            if compiled.strip():
                raise AssertionError("strict build emitted diagnostics for " + variant)
            run(variant + "-runtime-api", [javap, "-p", "-s", str(classes[variant] / "java/lang/Runtime.class")])
        before_classes = class_tree(classes["baseline"])
        after_classes = class_tree(classes["candidate"])
        if set(before_classes) != set(after_classes):
            raise AssertionError("facade introduced an unexpected owner class")
        unchanged = {name: digest for name, digest in before_classes.items() if name != "java/lang/Runtime.class"}
        if unchanged != {name: digest for name, digest in after_classes.items() if name != "java/lang/Runtime.class"}:
            raise AssertionError("existing dependency or nested Runtime class bytecode changed")
        if before_classes["java/lang/Runtime.class"] == after_classes["java/lang/Runtime.class"]:
            raise AssertionError("candidate Runtime did not change")
        old_members = api_members((output / "baseline-runtime-api.log").read_text())
        new_members = api_members((output / "candidate-runtime-api.log").read_text())
        additions = [
            ("public java.util.concurrent.ProgressMonitor progressMonitor();", "()Ljava/util/concurrent/ProgressMonitor;"),
            ("public java.util.concurrent.ProgressMonitor subProgressMonitor(java.lang.String, long, long);", "(Ljava/lang/String;JJ)Ljava/util/concurrent/ProgressMonitor;"),
        ]
        if any(new_members.count(member) != 1 or member in old_members for member in additions):
            raise AssertionError("facade API descriptors differ from the reviewed methods")
        if [member for member in new_members if member not in additions] != old_members:
            raise AssertionError("an existing Runtime member descriptor changed")
        fields_before = [m for m in old_members if "(" not in m[0]]
        fields_after = [m for m in new_members if "(" not in m[0]]
        if fields_before != fields_after or not fields_before:
            raise AssertionError("Runtime field inventory changed or was not parsed")
        native_before = [m for m in old_members if " native " in m[0]]
        native_after = [m for m in new_members if " native " in m[0]]
        if native_before != native_after or len(native_before) != 5:
            raise AssertionError("Runtime native descriptor inventory changed")
        if ("private java.lang.Runtime();", "()V") not in old_members:
            raise AssertionError("Runtime constructor contract changed")
        receipt.update({"old_member_count": len(old_members), "new_api_members": additions,
            "unchanged_fields": fields_after, "unchanged_native_descriptors": native_after,
            "unchanged_compiled_dependencies": unchanged,
            "runtime_class_sizes": {v: (classes[v] / "java/lang/Runtime.class").stat().st_size for v in classes}})

        legacy = output / "legacy-frozen-classes"
        legacy.mkdir()
        legacy_source = candidate / TEST_ROOT / "M3RuntimeLegacyClient.java"
        test_flags = ["-g:none", "-proc:none", "-Xlint:all", "-Werror"]
        run("legacy-stock-compile", [javac, "--release", "21", *test_flags, "-d", str(legacy), str(legacy_source)])
        frozen = class_tree(legacy)
        if set(frozen) != {"M3RuntimeLegacyClient.class"}:
            raise AssertionError("unexpected legacy class corpus")
        bytecode = run("legacy-bytecode", [javap, "-c", "-s", str(legacy / "M3RuntimeLegacyClient.class")])
        for opcode, target in (("invokestatic", "java/lang/Runtime.getRuntime"),
                               ("invokevirtual", "java/lang/Runtime.availableProcessors"),
                               ("invokevirtual", "java/lang/Runtime.freeMemory")):
            if not any(opcode in line and target in line for line in bytecode.splitlines()):
                raise AssertionError("missing frozen legacy dispatch: " + target)
        run("legacy-stock-run", [java, "-Xmx256m", "-Xcheck:jni", "-cp", str(legacy),
            "M3RuntimeLegacyClient"], marker="M3_RUNTIME_LEGACY_CLIENT_PASS checks=12")
        run("legacy-baseline-run", [java, "-Xmx256m", "-Xshare:off", "-Xcheck:jni", "--patch-module",
            "java.base=" + str(classes["baseline"]), "-cp", str(legacy), "M3RuntimeLegacyClient"],
            marker="M3_RUNTIME_LEGACY_CLIENT_PASS checks=12")
        current_client = output / "recompiled-legacy-classes"
        current_client.mkdir()
        run("legacy-current-source-compile", [javac, *test_flags, "--patch-module",
            "java.base=" + str(classes["candidate"]), "-d", str(current_client), str(legacy_source)])
        if class_tree(current_client) != frozen:
            raise AssertionError("the unchanged legacy client source compiles to different bytecode")
        tests = output / "facade-test-classes"
        tests.mkdir()
        facade_test = candidate / TEST_ROOT / "M3RuntimeProgressFacade.java"
        run("facade-test-compile", [javac, *test_flags, "--patch-module",
            "java.base=" + str(classes["candidate"]), "-d", str(tests), str(facade_test)])
        modes = {"default": [], "interpreter": ["-Xint"],
            "c1": ["-XX:TieredStopAtLevel=1"], "c2": ["-XX:-TieredCompilation", "-Xbatch"]}
        counts = {}
        for mode, flags in modes.items():
            common = [java, "-Xmx256m", "-Xshare:off", "-Xcheck:jni", *flags,
                "--patch-module", "java.base=" + str(classes["candidate"])]
            run("legacy-candidate-" + mode, [*common, "-cp", str(legacy), "M3RuntimeLegacyClient"],
                marker="M3_RUNTIME_LEGACY_CLIENT_PASS checks=12")
            observed = run("facade-" + mode, [*common, "-cp", str(tests), "M3RuntimeProgressFacade"],
                marker="M3_RUNTIME_PROGRESS_FACADE_PASS checks=")
            matches = re.findall(r"M3_RUNTIME_PROGRESS_FACADE_PASS checks=(\d+)", observed)
            if len(matches) != 1:
                raise AssertionError("unexpected facade witness output")
            counts[mode] = int(matches[0])
        if len(set(counts.values())) != 1 or min(counts.values()) < 80:
            raise AssertionError("unexpected or inconsistent facade coverage")
        if class_tree(legacy) != frozen:
            raise AssertionError("legacy bytecode mutated during candidate observations")
        receipt["legacy_frozen_classes"] = frozen
        receipt["legacy_source_recompile_same_bytecode"] = True
        receipt["facade_checks_by_mode"] = counts
        negative = output / "negative-source"
        negative.mkdir()
        subclass = negative / "RuntimeSubclassMustRefuse.java"
        subclass.write_text("public class RuntimeSubclassMustRefuse extends Runtime { }\n")
        for label, patch in (("stock", []), ("candidate", ["--patch-module", "java.base=" + str(classes["candidate"])])):
            destination = output / ("negative-" + label)
            destination.mkdir()
            text = run("subclass-refusal-" + label, [javac, "-proc:none", "-XDrawDiagnostics", *patch,
                "-d", str(destination), str(subclass)], expect=1)
            if "compiler.err.report.access" not in text or "private" not in text or "Runtime" not in text:
                raise AssertionError("Runtime constructor access refusal changed")
        receipt["source_subclassing"] = "REFUSED_BY_PRIVATE_CONSTRUCTOR_IN_STOCK_AND_CANDIDATE"
        receipt["status"] = "PASS"
    except BaseException as failed:
        receipt["status"] = "FAIL"
        receipt["failure"] = type(failed).__name__ + ": " + str(failed)
        raise
    finally:
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"status": receipt["status"], "commands": len(observations),
        "new_methods": 2, "fields_added": 0, "legacy_frozen_classes": receipt["legacy_frozen_classes"],
        "facade_checks_by_mode": receipt["facade_checks_by_mode"]}))


if __name__ == "__main__":
    main()
