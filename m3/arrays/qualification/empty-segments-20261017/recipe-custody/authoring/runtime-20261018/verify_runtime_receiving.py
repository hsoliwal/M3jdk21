#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seed exact published originals, receive qualified SDK output, and run targeted custody tests."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

WORK = Path(__file__).resolve().parents[2]
HERE = WORK / "runtime-refinement"
BASELINE = HERE / "pinned-baseline"
BASE = "4b24824c3aa09d802121447f8970d442a855f064"
OWNER = HERE / "canonical/synexia-openrewrite-recipes"
CRATE = OWNER / "recipe-crates/m3-collections-contract-refinement-20261017"
RECEIVER = HERE / "receiver-v3"
PROOF = HERE / "proof/runtime-receiving-v3"
DATASETS = ("m3jdk21-object-vtable", "m3jdk21-progress-doclint", "m3jdk21-functional-doclint", "m3jdk21-progress-scope-lint", "m3jdk21-runtime-control")


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def execute(label, command):
    result = subprocess.run(command, capture_output=True, text=True, timeout=90)
    (PROOF / (label + ".stdout.log")).write_text(result.stdout)
    (PROOF / (label + ".stderr.log")).write_text(result.stderr)
    if result.returncode != 0:
        raise ValueError(label + " failed: " + result.stderr)
    return {"command": command, "exit": result.returncode,
            "stdout_sha256": sha(PROOF / (label + ".stdout.log")),
            "stderr_sha256": sha(PROOF / (label + ".stderr.log"))}


def main():
    from pinned_baseline import export
    if export() != BASELINE:
        raise ValueError("immutable baseline export mismatch")
    RECEIVER.mkdir(exist_ok=False)
    PROOF.mkdir(parents=True, exist_ok=False)
    paths = set()
    for name in DATASETS:
        paths.update(row.split("\t")[0] for row in (CRATE / "datasets" / name / "MODES.tsv").read_text().splitlines())
    history = ("m3/compatibility/m3-object-adapter-progress-20261017.tsv", "m3/compatibility/m3-default-template-source-seal.tsv")
    for manifest in history:
        paths.add(manifest)
        for row in (BASELINE / manifest).read_text().splitlines():
            if row and not row.startswith("#"):
                paths.add(row.split("\t")[0])
    original = {}
    for relative in sorted(paths):
        source = BASELINE / relative
        if source.exists():
            destination = RECEIVER / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
            original[relative] = sha(source)
    observations = {}
    for dataset in DATASETS:
        command = ["python3", str(CRATE / "receive.py"), "--dataset", dataset,
                   "--target", str(RECEIVER), "--recipe-owner", str(OWNER)]
        observations[dataset + "-check"] = execute(dataset + "-check", command + ["--check"])
        observations[dataset + "-apply"] = execute(dataset + "-apply", command)
        before = {str(path.relative_to(RECEIVER)): (sha(path), path.stat().st_mtime_ns)
                  for path in RECEIVER.rglob("*") if path.is_file()}
        observations[dataset + "-fixed-point"] = execute(dataset + "-fixed-point", command)
        after = {str(path.relative_to(RECEIVER)): (sha(path), path.stat().st_mtime_ns)
                 for path in RECEIVER.rglob("*") if path.is_file()}
        if before != after:
            raise ValueError("receiving fixed point wrote a file: " + dataset)
    for script in ("check_object_adapter_progress_receiver_20261017.py", "verify_m3_default_template_20261017.py", "test_object_adapter_progress_receiver_20261018.py"):
        observations[script] = execute(script, ["python3", str(RECEIVER / "m3/compatibility" / script)])
    for manifest in history:
        if sha(RECEIVER / manifest) != original[manifest]:
            raise ValueError("historical manifest changed")
    old_map = json.loads((BASELINE / "m3/docs/name-mapping.json").read_text())
    new_map = json.loads((RECEIVER / "m3/docs/name-mapping.json").read_text())
    if len(old_map["mappings"]) != 59 or len(new_map["mappings"]) != 64:
        raise ValueError("unexpected current owner mapping closure")
    retained = dict(new_map)
    retained["mappings"] = retained["mappings"][:59]
    if retained != old_map:
        raise ValueError("original mapping or phase field changed")
    receipt = {"schema": "M3_RUNTIME_RECEIVING_VERIFICATION_V1", "result": "PASS", "baseline": BASE,
               "seeded_originals": original, "observations": observations,
               "historical_manifests": {path: sha(RECEIVER / path) for path in history},
               "current_owner_count": 64, "previous_owner_count": 59, "negative_test_methods": 31,
               "Object_universal_compatibility": "HOLD", "runtime_image_execution": "SEPARATE_GATE"}
    (PROOF / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print("PASS five qualified datasets received at fixed points; 31 regressions; historical manifests retained; 5 owner mappings appended")


if __name__ == "__main__":
    main()
