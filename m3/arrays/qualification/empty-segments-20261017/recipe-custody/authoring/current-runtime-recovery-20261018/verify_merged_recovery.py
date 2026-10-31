#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain fresh qualification and verify original SDK receiving in an isolated tree."""

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys

from prepare_merged_recovery import CATALOGUE, CURRENT, DATASET, DELIVERED, METADATA, TASK


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("recipe-owner", "preparation", "maven-log", "contract-proof", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    owner, preparation, output = args.recipe_owner.resolve(), args.preparation.resolve(), args.output.resolve()
    crate = owner / "recipe-crates" / TASK
    if "[INFO] BUILD SUCCESS" not in args.maven_log.read_text():
        raise ValueError("successful original Maven qualification is required")
    admission = json.loads((preparation / "candidate-input" / METADATA / "ADMISSION.json").read_text())
    if admission["baseline"] != CURRENT or admission["earlier_delivery"] != DELIVERED:
        raise ValueError("wrong source admission epoch")
    output.mkdir(parents=True, exist_ok=False)
    target = output / "receiving"
    shutil.copytree(preparation / "baseline", target)
    observations = []

    def run(label, command):
        result = subprocess.run(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=120)
        (output / (label + ".stdout.log")).write_bytes(result.stdout)
        (output / (label + ".stderr.log")).write_bytes(result.stderr)
        event = {"label": label, "command": command, "exit": result.returncode,
                 "stdout_sha256": hashlib.sha256(result.stdout).hexdigest(),
                 "stderr_sha256": hashlib.sha256(result.stderr).hexdigest()}
        observations.append(event)
        if result.returncode:
            (output / "FAILED_COMMANDS.json").write_text(json.dumps(observations, indent=2) + "\n")
            raise ValueError("fresh verification command failed: " + label)
        return result, event

    run("preparation-contract", [sys.executable, "-B", "-m", "unittest", "discover", "-s",
                                  str(Path(__file__).resolve().parent), "-p", "test_merged_recovery.py", "-v"])
    for label, check in (("check", True), ("apply", False), ("fixed-point", False)):
        command = [sys.executable, "-B", str(crate / "receive.py"), "--dataset", DATASET,
                   "--recipe-owner", str(owner), "--target", str(target)]
        if check:
            command.append("--check")
        result, event = run(label, command)
        value = json.loads(result.stdout.decode().split("M3_COLLECTIONS_RECEIVER_PASS ", 1)[1])
        if value["targets"] != 31 or value["changes"] != (0 if label == "fixed-point" else 31):
            raise ValueError("unexpected current-baseline receiving effect")
        event["result"] = value

    expected = preparation / "candidate-input"
    paths = sorted(p.relative_to(expected).as_posix() for p in expected.rglob("*") if p.is_file())
    if len(paths) != 31:
        raise ValueError("candidate target set changed")
    actual_paths = sorted(p.relative_to(target).as_posix() for p in target.rglob("*") if p.is_file())
    if actual_paths != paths:
        raise ValueError("receiving path set changed")
    for path in paths:
        if (target / path).read_bytes() != (expected / path).read_bytes():
            raise ValueError("SDK output differs from admitted source bytes: " + path)
    current_catalogue = (preparation / "baseline" / CATALOGUE).read_bytes()
    if not (target / CATALOGUE).read_bytes().startswith(current_catalogue):
        raise ValueError("current catalogue prefix was not preserved")
    production = []
    for row in admission["targets"]:
        if row["path"].startswith("src/"):
            if sha(target / row["path"]) != row["delivered_sha256"]:
                raise ValueError("restored production source differs from earlier delivery")
            production.append({"path": row["path"], "sha256": row["delivered_sha256"]})
    if len(production) != 14:
        raise ValueError("production restoration scope changed")
    shutil.copytree(crate / "datasets" / DATASET, output / "qualified-dataset")
    shutil.copytree(crate / "target" / DATASET / "candidate", output / "sdk-results")
    shutil.copytree(crate / "target" / DATASET / "surefire-reports", output / "surefire-reports")
    shutil.copytree(args.contract_proof, output / "original-contract-proof")
    shutil.copyfile(args.maven_log, output / "maven.log")
    contract = json.loads((output / "original-contract-proof/evidence/RECEIVER_RECEIPT.json").read_text())
    if contract["status"] != "PASS" or contract["dataset"] != DATASET or contract["targets"] != 31:
        raise ValueError("original receiver contract gate did not pass")
    files = [{"path": path, "sha256": sha(target / path), "size_bytes": (target / path).stat().st_size}
             for path in paths]
    receipt = {"schema": "M3_CURRENT_RUNTIME_RESTORATION_RECEIVING_V1", "result": "PASS",
               "baseline": CURRENT, "earlier_delivery": DELIVERED, "dataset": DATASET,
               "receiving": str(target), "observations": observations, "files": files,
               "preparation_contract_cases": 9, "original_sdk_cases": 5,
               "original_sealing_cases": 8, "original_receiver_cases": 9,
               "production_postimages": production, "current_catalogue_prefix_preserved": True,
               "all_31_sdk_outputs_equal_admitted_inputs": True,
               "host_pom_sha256": sha(crate / "pom.xml"),
               "OpenJDK_image": "SEPARATE_CURRENT_BUILD_INPUT_IDENTITY_OR_NEW_IMAGE_GATE",
               "Object_no_break": "HOLD_UNCHANGED", "phase": "STRING_UNCHANGED",
               "parent_receiving_worktree_modified": False}
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"result": "PASS", "dataset": DATASET, "check": 31, "apply": 31,
                      "replay": 0, "receipt": str(output / "RECEIPT.json"),
                      "receipt_sha256": sha(output / "RECEIPT.json")}, sort_keys=True))


if __name__ == "__main__":
    main()
