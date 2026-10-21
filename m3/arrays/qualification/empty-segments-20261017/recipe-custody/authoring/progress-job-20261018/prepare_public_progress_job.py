#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Prepare an archival-path successor after the original text recipe refuses .pom leaves."""

import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess


TASK = "m3-collections-contract-refinement-20261017"
DATASET = "m3jdk21-progress-job-custody"
PREFIX = "synexia-openrewrite-recipes/recipe-crates/" + TASK
PIN = "86528a90079b89a50b51ef13da08e7e70d468cb4"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return (json.dumps(value, indent=2) + "\n").encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("source-inventory", "recipe-owner", "baseline-root", "generic-builder", "task-root", "work-dir"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    crate = args.recipe_owner.resolve() / "recipe-crates" / TASK
    original_data = args.source_inventory.read_bytes()
    inventory = json.loads(original_data)
    aliases = []
    for entry in inventory["files"]:
        if sha(Path(entry["source"]).read_bytes()) != entry["sha256"]:
            raise ValueError("frozen original source inventory drift")
        if entry["path"].endswith(".pom"):
            old = entry["path"]
            entry["path"] += ".txt"
            entry["role"] += "; .txt is an archival path suffix, payload bytes unchanged"
            aliases.append({"before_path": old, "after_path": entry["path"], "sha256": entry["sha256"]})
    if len(aliases) != 4:
        raise ValueError("expected exactly four original Eclipse metadata suffix refusals")
    failures = [args.task_root / "public-custody-recipe-v1.log"]
    failures.extend(sorted((crate / "target" / DATASET / "surefire-reports").glob("*")))
    failures.extend(crate / "datasets" / DATASET / path for path in ("DATASET.json", "MODES.tsv", "ENGINE_EFFECTIVE.tsv"))
    failures.extend((crate / "datasets" / DATASET / "resources").rglob("manifest.tsv"))
    files = []
    for source in failures:
        if source.is_file():
            data = source.read_bytes()
            text = data.decode("utf-8", errors="strict")
            files.append({"source_path": str(source.resolve()), "sha256": sha(data), "bytes": len(data), "utf8": text})
    log = (args.task_root / "public-custody-recipe-v1.log").read_text()
    if "Tests run: 5, Failures: 0, Errors: 4, Skipped: 0" not in log or "invalid hash-pinned text manifest row" not in log:
        raise ValueError("expected actual original owner refusal evidence")
    failure_path = work / "public-custody-refusal-v1.json"
    failure_path.write_bytes(encoded({"schema": "M3_PROGRESS_JOB_CUSTODY_PATH_REFUSAL_V1",
        "result": "EXPECTED_OWNER_REFUSAL_REQUIRING_INPUT_CORRECTION", "sdk_tests": 5, "sdk_errors": 4,
        "sealer_receiver": "NOT_REACHED", "input_inventory_sha256": sha(original_data), "files": files,
        "repair": "Archive the four exact Eclipse Maven metadata bytes under .pom.txt; no text owner or path allowlist modification.",
        "product_dataset": "m3jdk21-progress-job remains unchanged and qualified; this failed outer custody was never received."}))
    catalogue_path = work / "public-custody-path-catalogue.json"
    catalogue_path.write_bytes(encoded({"schema": "M3_PROGRESS_JOB_ARTIFACT_PATH_REFINEMENT_V1",
        "problem_category": "recipe custody / archival metadata path admission",
        "owner": "Unchanged M3HashPinnedTextSnapshotRecipe.textPath",
        "existing_behavior": "Original recipe correctly refuses .pom target suffixes; generic source replay must retain that contract.",
        "selected_atom": "Map only the four read-only upstream Maven metadata archive names to .pom.txt, retaining byte hashes and licenses.",
        "rejected_alternative": "Widening the owner allowlist or bypassing SDK qualification would change a frozen recipe contract.",
        "aliases": aliases, "target_runtime_sources_changed": False, "phase": "STRING_UNCHANGED",
        "next_gate": "Fresh original five SDK/eight sealing/nine receiver qualification, then public-owner-only replay."}))

    def add(source, target, classification="GENERATED_EVIDENCE", role=None):
        data = source.read_bytes()
        record = {"source": str(source.resolve()), "path": PREFIX + "/" + target,
                  "sha256": sha(data), "size_bytes": len(data), "classification": classification,
                  "source_license": "Apache-2.0"}
        if role:
            record["role"] = role
        inventory["files"].append(record)

    add(Path(__file__).resolve(), "authoring/progress-job-20261018/prepare_public_progress_job.py",
        "FIRST_PARTY_PREPARATION_SOURCE", "Reusable input inventory path refinement; no generated output edits")
    add(Path(__file__).resolve().parent / "verify_public_progress_job.py",
        "authoring/progress-job-20261018/verify_public_progress_job.py", "FIRST_PARTY_PREPARATION_SOURCE",
        "Isolated exact public baseline, original receiving and fresh sdk-owner-only Maven validation")
    add(failure_path, "evidence/progress-job-20261018/public-custody-refusal-v1.json",
        role="Lossless actual failed SDK report and source manifests; no claimed eight/nine tests on failed pass")
    add(catalogue_path, "catalogue/progress-job-public-custody-20261018.json",
        role="Four-file archival path refinement, unchanged payload rights and selected original owner contract")
    inventory["schema"] = "M3_PROGRESS_JOB_CANONICAL_ARTIFACTS_V2"
    inventory["source_inventory_sha256"] = sha(original_data)
    inventory["path_aliases"] = aliases
    inventory["files"].sort(key=lambda entry: entry["path"])
    inventory["file_count"] = len(inventory["files"])
    inventory["size_bytes"] = sum(entry["size_bytes"] for entry in inventory["files"])
    prepared = work / "canonical-artifacts.json"
    prepared.write_bytes(encoded(inventory))
    command = ["python", "-B", str(args.generic_builder.resolve()), "--source-crate", str(crate),
        "--recipe-owner", str(args.recipe_owner.resolve()), "--public-root", str(args.baseline_root.resolve()),
        "--public-baseline-ref", PIN, "--canonical-baseline-ref", "8f8200375d8d403b58e855ee5db02f2e8fac33a8",
        "--dataset", "m3jdk21-progress-job", "--inventory", str(prepared), "--label", "progress-job-20261018",
        "--output", str(work / "candidate-input")]
    result = subprocess.run(command, capture_output=True, text=True, timeout=90)
    (work / "builder.stdout.log").write_text(result.stdout)
    (work / "builder.stderr.log").write_text(result.stderr)
    if result.returncode != 0:
        raise ValueError("unchanged generic input builder refused: " + result.stderr)
    spec = importlib.util.spec_from_file_location("canonical_prepare", crate / "prepare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    metadata = module.seal(crate, DATASET, "hsoliwal/M3jdk21", args.baseline_root.resolve(),
                           [work / "candidate-input"], PIN, args.recipe_owner.resolve())
    (work / "preparation-receipt.json").write_bytes(encoded({"schema": "M3_PROGRESS_JOB_PUBLIC_CUSTODY_PREPARATION_V2",
        "dataset": metadata, "inventory_sha256": sha(prepared.read_bytes()), "source_inventory_sha256": sha(original_data),
        "path_aliases": aliases, "product_sources_written": False, "qualification": "NEXT_GATE"}))
    print(json.dumps({"targets": metadata["targets"], "files": inventory["file_count"],
                      "inventory": str(prepared), "sha256": sha(prepared.read_bytes())}, sort_keys=True))


if __name__ == "__main__":
    main()
