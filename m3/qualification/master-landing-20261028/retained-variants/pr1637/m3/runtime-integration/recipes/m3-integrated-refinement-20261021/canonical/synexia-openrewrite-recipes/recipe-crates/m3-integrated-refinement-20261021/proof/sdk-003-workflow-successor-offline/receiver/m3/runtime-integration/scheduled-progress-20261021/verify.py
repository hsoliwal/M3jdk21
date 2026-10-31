#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify sealed scheduler sources, existing contracts and frozen stock-client binaries."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import traceback


PREFIX = "m3/runtime-integration/scheduled-progress-20261021"
OWNER = "src/java.base/share/classes/java/util/concurrent/ScheduledExecutorService.java"
NEW_TEST = "test/jdk/java/util/concurrent/ProgressJob/M3ScheduledProgress.java"
OLD_TEST = "test/jdk/java/util/concurrent/ProgressJob/M3ProgressJob.java"
CLIENT = PREFIX + "/src/test/java/LegacyScheduledClient.java"
API_FLAGS = ["-g:none", "-proc:none", "-Xlint:all", "-Xlint:-this-escape", "-Werror",
             "-Xdoclint:all/protected", "-Xdoclint/package:java.*,javax.*"]
TEST_FLAGS = ["-g:none", "-proc:none", "-Xlint:all", "-Werror"]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def class_hashes(root):
    return {path.relative_to(root).as_posix(): digest(path)
            for path in sorted(root.rglob("*.class"))}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)


def methods(text):
    result = {}
    signature = None
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("public ") and stripped.endswith(";"):
            signature = stripped
        elif stripped.startswith("descriptor:") and signature:
            name = signature[:signature.index("(")].split()[-1]
            descriptor = stripped.split(":", 1)[1].strip()
            result[name + descriptor] = signature
            signature = None
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("root", "baseline-root", "jdk-home", "out"):
        parser.add_argument("--" + name, type=Path, required=True)
    parser.add_argument("--candidate", type=Path,
                        help="Isolated authoring postimages; omit for actual emitted-target proof")
    args = parser.parse_args()
    args.root = args.root.resolve()
    args.baseline_root = args.baseline_root.resolve()
    args.jdk_home = args.jdk_home.resolve()
    args.out = args.out.resolve()
    candidate = args.candidate.resolve() if args.candidate else args.root
    args.out.mkdir(parents=True, exist_ok=False)
    receipt = {
        "schema": "M3_SCHEDULED_PROGRESS_RECEIVER_V1", "status": "RUNNING",
        "scope": "ISOLATED_AUTHORING_CANDIDATE" if args.candidate else "ACTUAL_EMITTED_TARGET",
        "root": str(args.root), "candidate": str(candidate),
        "baseline_root": str(args.baseline_root), "commands": [],
        "product_or_jdk_images_written": False,
        "full_source_built_image": "NOT_RUN_BOUNDED_JAVA_BASE_SOURCE_PATCH",
        "jtreg_harness": "NOT_RUN_TEST_MAINS_EXECUTED_DIRECTLY",
        "performance": "NOT_MEASURED_NO_SPEEDUP_CLAIM",
        "receiving_phase": "STRING_UNCHANGED",
        "eclipse_job_manager": "NOT_EXERCISED_BY_THIS_GATE"
    }

    def run(label, command, *, expected=0, quiet=False, timeout=90):
        argv = [str(value) for value in command]
        row = {"label": label, "command": argv, "expected_exit": expected}
        receipt["commands"].append(row)
        result = subprocess.run(argv, capture_output=True, timeout=timeout)
        out = args.out / (label + ".stdout.log")
        err = args.out / (label + ".stderr.log")
        out.write_bytes(result.stdout)
        err.write_bytes(result.stderr)
        row.update(exit=result.returncode, stdout_sha256=digest(out), stderr_sha256=digest(err))
        require((result.returncode == 0) == (expected == 0),
                label + " unexpected exit " + str(result.returncode) + "\n"
                + result.stdout.decode(errors="replace") + result.stderr.decode(errors="replace"))
        if quiet:
            require(not result.stdout and not result.stderr, label + " emitted diagnostic output")
        return result.stdout.decode(), result.stderr.decode()

    try:
        pin_path = candidate / PREFIX / "SOURCE_PIN.json"
        pins = json.loads(pin_path.read_text())
        receipt["source_pin_sha256"] = digest(pin_path)
        receipt["receiving_revision"] = pins["receiving_revision"]
        require(digest(args.baseline_root / OWNER) == pins["owner"]["before_sha256"],
                "baseline scheduler source drift")
        require(digest(candidate / OWNER) == pins["owner"]["after_sha256"],
                "candidate scheduler source drift")
        for entry in pins["dependencies"]:
            path = entry["path"]
            require(digest(args.root / path) == entry["sha256"], "target dependency drift: " + path)
            require(digest(args.baseline_root / path) == entry["sha256"], "baseline dependency drift: " + path)
        for entry in pins["tests"]:
            source = args.root / entry["path"] if entry["path"] == OLD_TEST else candidate / entry["path"]
            require(digest(source) == entry["sha256"], "contract test drift: " + entry["path"])
        receipt["inputs"] = {"owner": pins["owner"], "dependencies": pins["dependencies"],
                             "tests": pins["tests"]}
        java = args.jdk_home / "bin/java"
        javac = args.jdk_home / "bin/javac"
        javap = args.jdk_home / "bin/javap"
        stdout, stderr = run("compiler-version", [javac, "-version"])
        require((stdout + stderr).strip().startswith("javac 21"), "actual Java 21 compiler required")
        run("runtime-version", [java, "-version"])
        receipt["toolchain"] = {str(path): digest(path) for path in (java, javac, javap)}
        images = {}
        for label in ("baseline", "candidate"):
            sources = args.out / label / "source"
            classes = args.out / label / "classes"
            classes.mkdir(parents=True)
            selected = []
            for entry in pins["dependencies"]:
                path = entry["path"]
                source = args.baseline_root / path if label == "baseline" else args.root / path
                target = sources / path
                copy(source, target)
                selected.append(target)
            owner = sources / OWNER
            copy((args.baseline_root if label == "baseline" else candidate) / OWNER, owner)
            selected.append(owner)
            patch_source = sources / "src/java.base/share/classes"
            run(label + "-strict-api", [javac, *API_FLAGS, "--patch-module", "java.base=" + str(patch_source),
                                        "-d", classes, *selected], quiet=True)
            images[label] = classes
            receipt[label + "_classes"] = class_hashes(classes)
        api = {}
        for label, classes in images.items():
            stdout, _ = run(label + "-public-descriptors", [javap, "-public", "-s",
                              classes / "java/util/concurrent/ScheduledExecutorService.class"])
            api[label] = methods(stdout)
        require(len(api["baseline"]) == 4, "expected exactly four baseline scheduler declarations")
        for name, signature in api["baseline"].items():
            require(api["candidate"].get(name) == signature, "original descriptor/modifier changed: " + name)
        added = {name: signature for name, signature in api["candidate"].items()
                 if name not in api["baseline"]}
        expected = {
            "scheduleMonitored(Ljava/lang/Runnable;JLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/ProgressMonitor;)Ljava/util/concurrent/ScheduledFuture;",
            "scheduleMonitored(Ljava/util/concurrent/Callable;JLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/ProgressMonitor;)Ljava/util/concurrent/ScheduledFuture;",
            "scheduleAtFixedRateMonitored(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/ProgressMonitor;)Ljava/util/concurrent/ScheduledFuture;",
            "scheduleWithFixedDelayMonitored(Ljava/lang/Runnable;JJLjava/util/concurrent/TimeUnit;Ljava/util/concurrent/ProgressMonitor;)Ljava/util/concurrent/ScheduledFuture;"
        }
        require(set(added) == expected and all(" default " in sig for sig in added.values()),
                "unexpected additive API surface")
        receipt["api"] = {"original": api["baseline"], "added": added,
                          "classification": "FOUR_REQUESTED_ADDITIVE_DEFAULTS_EXISTING_DESCRIPTORS_UNCHANGED"}
        clients = args.out / "contract-clients"
        clients.mkdir()
        run("compile-existing-and-new-contracts", [javac, *TEST_FLAGS, "--patch-module",
            "java.base=" + str(images["candidate"]), "-d", clients, args.root / OLD_TEST,
            candidate / NEW_TEST], quiet=True)
        legacy = args.out / "stock-compiled-client"
        legacy.mkdir()
        run("compile-stock-client-once", [javac, *TEST_FLAGS, "-d", legacy, candidate / CLIENT], quiet=True)
        frozen = class_hashes(legacy)
        require(len(frozen) == 2, "unexpected stock client class closure")
        receipt["frozen_stock_client_classes"] = frozen
        missing_api = args.out / "RequestedApiMissing.java"
        missing_api.write_text("import java.util.concurrent.*;\nclass RequestedApiMissing {\n"
            "void run(ScheduledExecutorService s, Runnable r, Callable<Object> c, ProgressMonitor m) {\n"
            "s.scheduleMonitored(r, 0, TimeUnit.SECONDS, m);\n"
            "s.scheduleMonitored(c, 0, TimeUnit.SECONDS, m);\n"
            "s.scheduleAtFixedRateMonitored(r, 0, 1, TimeUnit.SECONDS, m);\n"
            "s.scheduleWithFixedDelayMonitored(r, 0, 1, TimeUnit.SECONDS, m);\n}\n}\n")
        _, stderr = run("baseline-missing-requested-api", [javac, *TEST_FLAGS, "--patch-module",
            "java.base=" + str(images["baseline"]), "-d", args.out / "missing-api", missing_api], expected=1)
        require("cannot find symbol" in stderr and "scheduleMonitored" in stderr
                and "scheduleAtFixedRateMonitored" in stderr and "scheduleWithFixedDelayMonitored" in stderr,
                "negative compiler witness failed for an unrelated reason")
        outputs = {}
        for mode, flags in (("mixed", []), ("interpreter", ["-Xint"])):
            stdout, _ = run(mode + "-stock-old-binary", [java, *flags, "-cp", legacy, "LegacyScheduledClient"])
            outputs[mode + "-stock-legacy"] = stdout
            for label, classes in images.items():
                prefix = [java, *flags, "--patch-module", "java.base=" + str(classes)]
                for main_class, arguments in (("M3ProgressJob", []), ("M3ScheduledProgress", [label])):
                    stdout, _ = run(mode + "-" + label + "-" + main_class,
                                    [*prefix, "-cp", clients, main_class, *arguments])
                    outputs[mode + "-" + label + "-" + main_class] = stdout
                stdout, _ = run(mode + "-" + label + "-old-binary",
                                [*prefix, "-cp", legacy, "LegacyScheduledClient"])
                require(stdout == outputs[mode + "-stock-legacy"], "old binary output changed")
                if label == "candidate":
                    stdout, _ = run(mode + "-old-implementer-new-defaults",
                                    [*prefix, "-cp", legacy, "LegacyScheduledClient", "new-defaults"])
                    require(stdout == outputs[mode + "-stock-legacy"], "old implementer dispatch changed")
            for main_class in ("M3ProgressJob", "M3ScheduledProgress"):
                require(outputs[mode + "-baseline-" + main_class]
                        == outputs[mode + "-candidate-" + main_class],
                        "baseline/candidate external contract observations differ: " + main_class)
        require(class_hashes(legacy) == frozen, "frozen stock-compiled client bytes changed")
        for entry in pins["dependencies"]:
            require(digest(args.root / entry["path"]) == entry["sha256"], "dependency changed during proof")
        require(digest(candidate / OWNER) == pins["owner"]["after_sha256"], "owner changed during proof")
        receipt["observations"] = outputs
        receipt["runtime_launches"] = sum("--patch-module" in row["command"] and
            row["command"][0] == str(java) or "stock-old-binary" in row["label"]
            for row in receipt["commands"])
        receipt["status"] = "PASS"
    except BaseException:
        receipt["status"] = "FAIL"
        receipt["failure"] = traceback.format_exc()
        raise
    finally:
        (args.out / "RESULT.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"status": receipt["status"], "scope": receipt["scope"],
                      "runtime_launches": receipt["runtime_launches"], "api_added": 4}, indent=2))


if __name__ == "__main__":
    main()
