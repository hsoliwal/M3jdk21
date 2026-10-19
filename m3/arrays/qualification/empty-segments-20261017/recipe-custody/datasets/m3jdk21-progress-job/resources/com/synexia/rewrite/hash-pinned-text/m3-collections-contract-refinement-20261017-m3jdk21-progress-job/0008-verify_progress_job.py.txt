#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify received ProgressJob bytes without modifying either JDK image or product tree."""

import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess
import traceback


PREFIX = "m3/runtime-integration/progress-job-20261018"
OWNER = "src/java.base/share/classes/java/util/concurrent/ProgressJob.java"
JDK_TEST = "test/jdk/java/util/concurrent/ProgressJob/M3ProgressJob.java"
JDK_FLAGS = ["-g:none", "-proc:none", "-Xlint:all", "-Xlint:-this-escape", "-Werror",
             "-Xdoclint:all/protected", "-Xdoclint/package:java.*,javax.*"]
CLIENT_FLAGS = ["-g:none", "-proc:none", "-Xlint:all", "-Werror"]


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def class_tree(root):
    return {p.relative_to(root).as_posix(): sha(p) for p in sorted(root.rglob("*.class"))}


def copy(source, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("baseline-root", "candidate", "stock-jdk", "m3-jdk", "eclipse-dir", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    args.output = args.output.resolve()
    args.output.mkdir(parents=True, exist_ok=False)
    observations = []
    receipt = {"schema": "M3_PROGRESS_JOB_BEHAVIOR_PROOF_V1", "result": "RUNNING",
               "product_sources_written": False, "jdk_images_modified": False,
               "full_image_with_new_api": "NOT_RUN_PATCHED_IMAGE_ONLY",
               "public_api_admission": "PENDING_FULL_IMAGE_API_REVIEW_AND_ORDERED_PHASE",
               "Object_universal_compatibility": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED",
               "commands": observations}

    def run(label, command, expected_success=True, quiet_success=False, timeout=120):
        result = subprocess.run([str(value) for value in command], capture_output=True, text=True, timeout=timeout)
        stdout = args.output / (label + ".stdout.log")
        stderr = args.output / (label + ".stderr.log")
        stdout.write_text(result.stdout)
        stderr.write_text(result.stderr)
        observations.append({"label": label, "command": [str(value) for value in command],
                             "exit": result.returncode, "expected_success": expected_success,
                             "stdout_sha256": sha(stdout), "stderr_sha256": sha(stderr)})
        if (result.returncode == 0) != expected_success:
            raise ValueError("unexpected result: " + label + "\n" + result.stdout + result.stderr)
        if quiet_success and (result.stdout.strip() or result.stderr.strip()):
            raise ValueError("strict compile emitted output: " + label + "\n" + result.stdout + result.stderr)
        return result.stdout, result.stderr

    try:
        review_path = args.candidate / PREFIX / "DONOR-REVIEW.json"
        review = json.loads(review_path.read_text())
        if sha(args.candidate / OWNER) != review["product_source_sha256"]:
            raise ValueError("received ProgressJob differs from reviewed owner")
        receipt["review_sha256"] = sha(review_path)
        receipt["source_sha256"] = review["product_source_sha256"]
        sources = args.output / "stock-source"
        source_paths = []
        for entry in review["current_owner_dependencies"]:
            source = args.baseline_root / entry["path"]
            if sha(source) != entry["sha256"]:
                raise ValueError("existing owner drift: " + entry["path"])
            target = sources / entry["path"]
            copy(source, target)
            source_paths.append(target)
        if len(source_paths) != 7:
            raise ValueError("expected exact seven-source dependency closure")
        owner_copy = sources / OWNER
        copy(args.candidate / OWNER, owner_copy)
        source_paths.append(owner_copy)
        receipt["existing_dependencies"] = review["current_owner_dependencies"]
        artifacts = review["eclipse_release_inputs"]["files"]
        for entry in artifacts:
            if sha(args.eclipse_dir / entry["file"]) != entry["sha256"]:
                raise ValueError("external artifact drift: " + entry["file"])
        jars = [args.eclipse_dir / "org.eclipse.equinox.common-3.21.0.jar",
                args.eclipse_dir / "org.eclipse.osgi-3.24.300.jar"]
        eclipse_classpath = ":".join(str(path.resolve()) for path in jars)
        receipt["external_eclipse_jars"] = [{"path": path.name, "sha256": sha(path)} for path in jars]
        stock_java = (args.stock_jdk / "bin/java").resolve()
        stock_javac = (args.stock_jdk / "bin/javac").resolve()
        image_java = (args.m3_jdk / "bin/java").resolve()
        image_javac = (args.m3_jdk / "bin/javac").resolve()
        for label, executable in (("stock-javac-version", stock_javac), ("image-javac-version", image_javac)):
            stdout, stderr = run(label, [executable, "-version"])
            if not (stdout + stderr).strip().startswith("javac 21"):
                raise ValueError("requires actual Java 21 compiler")
        run("stock-java-version", [stock_java, "-version"])
        run("image-java-version", [image_java, "-version"])
        receipt["toolchain_sha256"] = {str(path): sha(path) for path in (stock_java, stock_javac, image_java, image_javac)}
        stock_classes = args.output / "stock-patch-classes"
        image_classes = args.output / "image-new-class"
        stock_classes.mkdir()
        image_classes.mkdir()
        patch_source = (sources / "src/java.base/share/classes").resolve()
        run("stock-strict-api", [stock_javac, *JDK_FLAGS, "--patch-module", "java.base=" + str(patch_source),
                                  "-d", stock_classes, *source_paths], quiet_success=True)
        image_source = args.output / "image-new-source/java/util/concurrent/ProgressJob.java"
        copy(args.candidate / OWNER, image_source)
        run("image-strict-api", [image_javac, *JDK_FLAGS, "--patch-module", "java.base=" + str(image_source.parents[3]),
                                  "-d", image_classes, image_source], quiet_success=True)
        new_class = "java/util/concurrent/ProgressJob.class"
        if class_tree(image_classes) != {new_class: sha(stock_classes / new_class)}:
            raise ValueError("new class differs between stock closure and existing M3 image compiles")
        receipt["new_class_sha256"] = sha(image_classes / new_class)
        receipt["stock_patch_classes"] = class_tree(stock_classes)
        run("new-class-bytecode", [args.stock_jdk / "bin/javap", "-p", "-c", "-s", "-verbose", image_classes / new_class])
        clients = args.output / "new-client-classes"
        clients.mkdir()
        run("strict-core-test", [stock_javac, *CLIENT_FLAGS, "--patch-module", "java.base=" + str(stock_classes),
                                 "-d", clients, args.candidate / JDK_TEST], quiet_success=True)
        eclipse_source = args.candidate / PREFIX / "src/test/java/EclipseStatusIdentity.java"
        run("strict-eclipse-test", [stock_javac, *CLIENT_FLAGS, "--patch-module", "java.base=" + str(stock_classes),
                                    "-classpath", eclipse_classpath, "-d", clients, eclipse_source], quiet_success=True)
        old_source = args.candidate / PREFIX / "src/test/java/OldCallableClient.java"
        old_classes = args.output / "frozen-stock-client"
        old_classes.mkdir()
        run("stock-old-client-compile", [stock_javac, *CLIENT_FLAGS, "-d", old_classes, old_source], quiet_success=True)
        frozen_classes = class_tree(old_classes)
        if len(frozen_classes) != 3:
            raise ValueError("unexpected frozen old client closure")
        old_new_classes = args.output / "recompiled-old-client"
        old_new_classes.mkdir()
        run("patched-old-client-source", [stock_javac, *CLIENT_FLAGS, "--patch-module", "java.base=" + str(stock_classes),
                                           "-d", old_new_classes, old_source], quiet_success=True)
        if class_tree(old_new_classes) != frozen_classes:
            raise ValueError("finite old client produces different bytes with added API")
        negative = args.output / "negative-source/ProgressJobRunnableConflict.java"
        copy(args.candidate / PREFIX / "src/test/resources/ProgressJobRunnableConflict.java.txt", negative)
        negative_classes = args.output / "negative-classes"
        negative_classes.mkdir()
        stdout, stderr = run("new-runnable-subclass-conflict", [stock_javac, *CLIENT_FLAGS, "-XDrawDiagnostics",
                                "--patch-module", "java.base=" + str(stock_classes), "-d", negative_classes, negative],
                               expected_success=False)
        keys = re.findall(r"compiler\.err\.[a-zA-Z0-9_.]+", stderr)
        expected_keys = {"compiler.err.override.incompatible.ret", "compiler.err.override.weaker.access"}
        if not expected_keys.issubset(keys):
            raise ValueError("both new Runnable return/access conflicts must be observed: " + stderr)
        if any("cant.resolve" in key for key in keys):
            raise ValueError("negative witness failed due to unavailable API")
        receipt["new_runnable_subclass"] = {"result": "EXPECTED_CONFLICT", "diagnostic_keys": keys,
                                            "old_dual_clients": "SEPARATE_FINITE_POSITIVE_WITNESS"}
        stdout, _ = run("stock-old-client-run", [stock_java, "-Xmx256m", "-ea", "-classpath", old_classes, "OldCallableClient"])
        if "PASS old Callable" not in stdout:
            raise ValueError("stock old client did not execute")
        counts = {}
        for label, java, patch in (("stock-patched", stock_java, stock_classes), ("m3-image-patched", image_java, image_classes)):
            runtime = [java, "-Xmx256m", "-ea", "-Xcheck:jni", "--patch-module", "java.base=" + str(patch)]
            stdout, _ = run(label + "-old-binary", [*runtime, "-classpath", old_classes, "OldCallableClient"])
            if "PASS old Callable" not in stdout:
                raise ValueError("frozen old binary did not execute")
            stdout, _ = run(label + "-core", [*runtime, "-classpath", clients, "M3ProgressJob"])
            match = re.search(r"PASS ProgressJob checks=(\d+)", stdout)
            if match is None:
                raise ValueError("core witness did not execute")
            core_count = int(match.group(1))
            stdout, _ = run(label + "-eclipse", [*runtime, "-classpath", str(clients) + ":" + eclipse_classpath, "EclipseStatusIdentity"])
            match = re.search(r"PASS real Eclipse IStatus/Status/MultiStatus checks=(\d+)", stdout)
            if match is None:
                raise ValueError("actual Eclipse status witness did not execute")
            counts[label] = {"core_checks": core_count, "real_eclipse_checks": int(match.group(1))}
        if counts["stock-patched"] != counts["m3-image-patched"]:
            raise ValueError("runtime witness counts differ")
        if class_tree(old_classes) != frozen_classes:
            raise ValueError("frozen old client class bytes changed during execution")
        receipt["frozen_stock_client_classes"] = frozen_classes
        receipt["new_client_classes"] = class_tree(clients)
        receipt["checks"] = counts
        receipt["synexia_result_runtime"] = "NOT_RUN: exact IJobResult/AbstractJob/IAdaptable source review only; no stubbed substitute or whole context closure claim"
        receipt["result"] = "PASS"
    except Exception as error:
        receipt["result"] = "FAIL"
        receipt["failure"] = str(error)
        (args.output / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS", json.dumps(receipt["checks"], sort_keys=True), "; new API pending; full new image not run; existing HOLD unchanged")


if __name__ == "__main__":
    main()
