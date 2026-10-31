#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Refine the public baseline fixture's one directory guard through the same recipe."""

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
    original = args.source_inventory.read_bytes()
    inventory = json.loads(original)
    helper_path = PREFIX + "/authoring/progress-job-20261018/verify_public_progress_job.py"
    helper = next(entry for entry in inventory["files"] if entry["path"] == helper_path)
    for entry in inventory["files"]:
        if sha(Path(entry["source"]).read_bytes()) != entry["sha256"]:
            raise ValueError("frozen inventory input drift")
    before = Path(helper["source"]).read_bytes()
    needle = b'if relative.is_absolute() or ".." in relative.parts or not str(relative).startswith("m3/"):'
    replacement = (b'if relative.is_absolute() or ".." in relative.parts or not (\n'
                   b'                        str(relative).startswith("m3/") or (str(relative) == "m3" and member.isdir())):')
    if before.count(needle) != 1:
        raise ValueError("expected exact one directory guard")
    after = before.replace(needle, replacement)
    compile(after.decode("utf-8"), "verify_public_progress_job.py", "exec")
    post = work / "authored/verify_public_progress_job.py"
    post.parent.mkdir()
    post.write_bytes(after)
    helper["supersedes_sha256"] = helper["sha256"]
    helper["source"] = str(post)
    helper["sha256"] = sha(after)
    helper["size_bytes"] = len(after)
    helper["role"] += "; admits only the normal top-level m3 directory while retaining absolute/traversal/symlink refusal"
    failure = json.loads((args.task_root / "proof/public-custody-v2/receipt.json").read_text())
    if failure.get("result") != "FAIL" or failure.get("failure") != "unsafe baseline archive path" or failure.get("commands"):
        raise ValueError("expected pre-receiver fixture setup failure")
    retained = []
    inputs = [args.task_root / "public-custody-recipe-v2.log",
              args.task_root / "proof/public-custody-v2/receipt.json",
              crate / "target" / DATASET / "candidate/SDK_INPUTS.tsv",
              crate / "target" / DATASET / "candidate/SDK_RESULTS.tsv"]
    inputs.extend(sorted((crate / "target" / DATASET / "surefire-reports").glob("*")))
    inputs.extend(crate / "datasets" / DATASET / name for name in ("DATASET.json", "MODES.tsv", "ENGINE_EFFECTIVE.tsv"))
    inputs.extend((crate / "datasets" / DATASET / "resources").rglob("manifest.tsv"))
    for source in inputs:
        if source.is_file():
            data = source.read_bytes()
            retained.append({"path": str(source.resolve()), "sha256": sha(data), "bytes": len(data), "utf8": data.decode("utf-8")})
    proof = work / "public-driver-refusal-v2.json"
    proof.write_bytes(encoded({"schema": "M3_PROGRESS_JOB_PUBLIC_FIXTURE_DIRECTORY_REFINEMENT_V1",
        "result": "SOURCE_DRIVER_FIX_REQUIRED", "prior_custody_recipe": "PASS_ORIGINAL_5_8_9_TARGETS_156",
        "public_replay": "FAILED_DURING_BASELINE_SETUP_BEFORE_RECEIVING", "source_inventory_sha256": sha(original),
        "before_source": {"sha256": sha(before), "bytes": len(before), "utf8": before.decode("utf-8")},
        "after_source_sha256": sha(after), "retained_actual_evidence": retained,
        "category": "fixture / exact Git subtree archive setup", "selected_atom": "Admit m3 only when it is a directory; preserve every other guard.",
        "product_sources_changed": False, "recipe_owner_changed": False,
        "phase": "STRING_UNCHANGED", "Object_no_break": "HOLD_UNCHANGED"}))

    def add(source, target, classification):
        data = source.read_bytes()
        inventory["files"].append({"source": str(source.resolve()), "path": PREFIX + "/" + target,
            "sha256": sha(data), "size_bytes": len(data), "classification": classification, "source_license": "Apache-2.0"})

    add(Path(__file__).resolve(), "authoring/progress-job-20261018/prepare_public_progress_job_retry.py", "FIRST_PARTY_PREPARATION_SOURCE")
    add(proof, "evidence/progress-job-20261018/public-driver-refusal-v2.json", "GENERATED_EVIDENCE")
    inventory["schema"] = "M3_PROGRESS_JOB_CANONICAL_ARTIFACTS_V3"
    inventory["source_inventory_sha256"] = sha(original)
    inventory["fixture_driver_refinement"] = {"path": helper_path, "before_sha256": sha(before), "after_sha256": sha(after)}
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
        raise ValueError("generic builder refused: " + result.stderr)
    spec = importlib.util.spec_from_file_location("canonical_prepare", crate / "prepare.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    metadata = module.seal(crate, DATASET, "hsoliwal/M3jdk21", args.baseline_root.resolve(),
                           [work / "candidate-input"], PIN, args.recipe_owner.resolve())
    (work / "preparation-receipt.json").write_bytes(encoded({"schema": "M3_PROGRESS_JOB_PUBLIC_CUSTODY_PREPARATION_V3",
        "dataset": metadata, "inventory_sha256": sha(prepared.read_bytes()), "source_inventory_sha256": sha(original),
        "driver_before_sha256": sha(before), "driver_after_sha256": sha(after), "product_sources_written": False}))
    print(json.dumps({"targets": metadata["targets"], "files": inventory["file_count"],
                      "inventory": str(prepared), "sha256": sha(prepared.read_bytes())}, sort_keys=True))


if __name__ == "__main__":
    main()
