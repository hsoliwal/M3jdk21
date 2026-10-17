#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# Copyright 2026 Hitesh Soliwal and contributors
"""Run the separate plugin coexistence probe in the qualified linked image."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import shutil
import subprocess

from artifact_admission import admit_patch

HERE = Path(__file__).resolve().parent


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--frontend-jar", type=Path, required=True)
    admission_group = parser.add_mutually_exclusive_group(required=True)
    admission_group.add_argument("--frontend-sha256", help="Explicit frontend digest used with the exact pinned compiler proof")
    admission_group.add_argument("--artifact-receipt", type=Path,
                                 help="The same source-qualified ARTIFACTS.json used for the linked compiler proof")
    parser.add_argument("--patch", type=Path, required=True)
    args = parser.parse_args()
    evidence = HERE / "evidence"
    qualification = json.loads((evidence / "QUALIFICATION.json").read_text())
    if qualification["status"] != "passed":
        raise ValueError("exact upstream linked-image contract must pass first")
    previous_admission = qualification.get("artifact_admission", {"mode": "exact-artifact-pin"})
    if args.artifact_receipt is not None:
        admission = admit_patch(args.patch, args.artifact_receipt)
        if previous_admission.get("mode") != "sealed-build-receipt" or admission != previous_admission:
            raise ValueError("coexistence requires the same sealed builder receipt used by the linked-image proof")
        expected_frontend = admission.get("plugin_sha256")
        if expected_frontend is None:
            raise ValueError("source-qualified builder receipt lacks its frontend artifact digest")
    else:
        if previous_admission.get("mode") != "exact-artifact-pin":
            raise ValueError("linked-image proof used a builder receipt; coexistence requires that same receipt")
        admission = {"mode": "explicit-frontend-digest", "plugin_sha256": args.frontend_sha256}
        expected_frontend = args.frontend_sha256
    if digest(args.frontend_jar) != expected_frontend:
        raise ValueError("frontend artifact differs from supplied qualified receipt")
    if digest(args.patch) != qualification["patch_sha256"]:
        raise ValueError("final patch differs from the linked-image compiler contract")
    proof_work = Path(qualification["proof_work_directory"])
    image = proof_work / "image"
    if digest(image / "lib/modules") != qualification["linked_image_modules_sha256"]:
        raise ValueError("linked image differs from qualified modules")
    output = proof_work / "coexistence"
    if output.exists():
        raise ValueError("isolated coexistence output already exists")
    output.mkdir()
    classes = output / "classes"
    classes.mkdir()
    source = HERE / "proof/PluginCoexistenceProbe.java"
    export = "jdk.compiler/com.sun.tools.javac.m3=ALL-UNNAMED"
    commands = [
        ("compile-plugin-coexistence", [str(image / "bin/javac"), "-J-Xmx256m", "-proc:none", "-Xlint:all", "-Werror",
                                       "--add-exports", export, "-d", str(classes), str(source)]),
        ("plugin-coexistence", [str(image / "bin/java"), "-Xmx512m", "--add-exports", export,
                                "-cp", str(classes) + ":" + str(args.frontend_jar.resolve()),
                                "PluginCoexistenceProbe", str(output / "work")]),
    ]
    receipt = {"status": "in-progress", "probe_source_sha256": digest(source),
               "frontend_jar_sha256": expected_frontend, "compiler_patch_sha256": qualification["patch_sha256"],
               "artifact_admission": admission, "proof_driver_sha256": digest(Path(__file__)),
               "linked_image_modules_sha256": qualification["linked_image_modules_sha256"],
               "upstream_test_was_modified": False, "commands": [], "product_output_directory_touched": False}
    try:
        for label, command in commands:
            log = evidence / (label + ".log")
            with log.open("w") as stream:
                result = subprocess.run(command, cwd=output, stdout=stream, stderr=subprocess.STDOUT, timeout=120)
            receipt["commands"].append({"label": label, "argv": command, "exit_code": result.returncode,
                                        "log_sha256": digest(log)})
            if result.returncode != 0:
                raise ValueError("coexistence proof failed: " + label)
        text = (evidence / "plugin-coexistence.log").read_text()
        matches = re.findall(r"^M3_AST_PLUGIN_COEXISTENCE checks=(\d+) parses=1$", text, re.M)
        if matches != ["9"]:
            raise ValueError("complete coexistence receipt absent")
        receipt["status"] = "passed"
        receipt["checks"] = 9
        receipt["actual_parses"] = 1
        receipt["plugins"] = ["M3AST", "M3ASTIndex"]
        shutil.copyfile(output / "work/builtin.tsv", evidence / "coexistence-builtin-trace.tsv")
        sessions = list((output / "work/index").iterdir())
        shutil.copyfile(sessions[0] / "receipt.json", evidence / "coexistence-index-receipt.json")
    except BaseException as failure:
        receipt["status"] = "failed"
        receipt["failure"] = str(failure)
        raise
    finally:
        (evidence / "COEXISTENCE.json").write_text(json.dumps(receipt, indent=2) + "\n")
        print(json.dumps({"status": receipt["status"], "checks": receipt.get("checks")}), flush=True)


if __name__ == "__main__":
    main()
