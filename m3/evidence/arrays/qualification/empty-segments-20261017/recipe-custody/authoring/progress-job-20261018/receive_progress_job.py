#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain exact SDK qualification and receive the additive task into an isolated tree."""

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess


DATASET = "m3jdk21-progress-job"
TASK = "m3-collections-contract-refinement-20261017"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--maven-log", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    owner = args.recipe_owner.resolve()
    crate = owner / "recipe-crates" / TASK
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    target = output / "receiving"
    target.mkdir()
    metadata = json.loads((crate / "datasets" / DATASET / "DATASET.json").read_text())
    observations = []
    for label, check in (("check", True), ("apply", False), ("fixed-point", False)):
        command = ["python", "-B", str(crate / "receive.py"), "--dataset", DATASET,
                   "--recipe-owner", str(owner), "--target", str(target)]
        if check:
            command.append("--check")
        result = subprocess.run(command, capture_output=True, text=True, timeout=90)
        (output / (label + ".stdout.log")).write_text(result.stdout)
        (output / (label + ".stderr.log")).write_text(result.stderr)
        if result.returncode != 0:
            raise ValueError("receiver failed: " + result.stderr)
        value = json.loads(result.stdout.split("M3_COLLECTIONS_RECEIVER_PASS ", 1)[1])
        expected = 0 if label == "fixed-point" else metadata["targets"]
        if value["changes"] != expected:
            raise ValueError("unexpected receiver changes")
        observations.append({"label": label, "command": command, "exit": result.returncode, "result": value})
    shutil.copytree(crate / "datasets" / DATASET, output / "qualified-dataset")
    shutil.copytree(crate / "target" / DATASET / "candidate", output / "sdk-results")
    shutil.copytree(crate / "target" / DATASET / "surefire-reports", output / "surefire-reports")
    shutil.copyfile(args.maven_log, output / "maven.log")
    actual = [{"path": p.relative_to(target).as_posix(), "sha256": sha(p), "bytes": p.stat().st_size}
              for p in sorted(target.rglob("*")) if p.is_file()]
    if len(actual) != metadata["targets"]:
        raise ValueError("unexpected receiving source count")
    receipt = {"schema": "M3_PROGRESS_JOB_RECEIVING_V1", "result": "PASS", "dataset": DATASET,
               "receiving": str(target), "observations": observations, "files": actual,
               "host_pom_sha256": sha(crate / "pom.xml"), "runtime_behavior": "SEPARATE_GATE",
               "full_image": "NOT_RUN", "new_api": "PENDING", "Object_universal_no_break": "HOLD_UNCHANGED",
               "phase": "STRING_UNCHANGED"}
    (output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS check/apply", metadata["targets"], "; replay 0; isolated receiving:", target)


if __name__ == "__main__":
    main()
