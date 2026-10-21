#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Preserve the archive-mode refusal and qualify a source-only explicit Git umask successor."""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys


TASK = "m3-collections-contract-refinement-20261017"
PREFIX = "synexia-openrewrite-recipes/recipe-crates/" + TASK
LABEL = "current-runtime-recovery-20261018"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return (json.dumps(value, indent=2) + "\n").encode()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("source-inventory", "recipe-owner", "repository", "generic-builder", "task-root", "output"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    work = args.output.resolve()
    work.mkdir(parents=True, exist_ok=False)
    source_inventory = args.source_inventory.read_bytes()
    inventory = json.loads(source_inventory)
    records = {entry["path"]: entry for entry in inventory["files"]}
    for entry in records.values():
        if sha(Path(entry["source"]).read_bytes()) != entry["sha256"]:
            raise ValueError("frozen source inventory drift")
    relative = PREFIX + "/authoring/" + LABEL + "/prepare_public_recovery.py"
    original = Path(records[relative]["source"])
    before = original.read_bytes()
    needle = b'["git", "-C", str(repository), "archive", pin, "--", *sorted(expected)]'
    replacement = b'["git", "-c", "tar.umask=0022", "-C", str(repository), "archive", pin, "--", *sorted(expected)]'
    if before.count(needle) != 1:
        raise ValueError("expected one exact original archive-command atom")
    common = ["--recipe-owner", str(args.recipe_owner.resolve()), "--repository", str(args.repository.resolve()),
              "--generic-builder", str(args.generic_builder.resolve())]
    failed_command = [sys.executable, "-B", str(original), "--source-inventory", str(args.source_inventory.resolve()),
                      *common, "--output", str(work / "retained-refusal")]
    failure = subprocess.run(failed_command, capture_output=True, timeout=180)
    if failure.returncode == 0 or b"archive bytes or mode differ from the exact Git tree" not in failure.stderr:
        raise ValueError("expected actual unchanged-helper archive-mode refusal")
    retained = []
    for name, data in (("prepare_public_recovery.py", before), ("stdout.log", failure.stdout), ("stderr.log", failure.stderr)):
        retained.append({"path": name, "sha256": sha(data), "size_bytes": len(data),
                         "source_license": "Apache-2.0", "utf8": data.decode("utf-8")})
    failure_path = work / "archive-mode-refusal-v1.json"
    failure_path.write_bytes(encoded({"schema": "M3_CURRENT_RUNTIME_RECOVERY_ARCHIVE_MODE_REFUSAL_V1",
        "status": "ACTUAL_PREPARATION_REFUSAL_NO_DATASET_EMISSION", "command": failed_command,
        "exit": failure.returncode, "source_inventory_sha256": sha(source_inventory), "files": retained,
        "cause": "git archive default tar.umask emits0664 for a100644 source; source Git blob bytes match but the explicit mode identity check refuses.",
        "selected_atom": "Pass git -c tar.umask=0022 to archive; retain the exact tree-mode and blob-identity checks.",
        "engine_changes": [], "product_sources_written": False, "Maven_gates": "NOT_REACHED_ON_REFUSAL"}))
    for item in json.loads(failure_path.read_text())["files"]:
        if sha(item["utf8"].encode("utf-8")) != item["sha256"]:
            raise ValueError("retained failure round trip changed")
    authored = work / "authored"
    authored.mkdir()
    successor = authored / original.name
    successor.write_bytes(before.replace(needle, replacement))
    verifier_name = PREFIX + "/authoring/" + LABEL + "/verify_public_recovery.py"
    verifier = Path(records[verifier_name]["source"])
    (authored / verifier.name).write_bytes(verifier.read_bytes())
    updated = dict(records[relative])
    updated.update(source=str(successor), sha256=sha(successor.read_bytes()), size_bytes=successor.stat().st_size,
                   source_origin="Serial new-source preparation correction; frozen original authorer retained in the refusal envelope.",
                   predecessor_sha256=sha(before))
    records[relative] = updated

    def add(source, relative_path, classification, role):
        path = PREFIX + "/" + relative_path
        if path in records:
            raise ValueError("new retry source overlaps an existing inventory target")
        data = source.read_bytes()
        records[path] = {"source": str(source.resolve()), "path": path, "sha256": sha(data), "size_bytes": len(data),
                         "classification": classification, "source_license": "Apache-2.0", "role": role}

    add(Path(__file__).resolve(), "authoring/" + LABEL + "/prepare_recovery_archive_retry.py",
        "FIRST_PARTY_PREPARATION_SOURCE", "Exact single-atom source successor; preserves actual refusal and original source")
    add(failure_path, "evidence/" + LABEL + "/archive-mode-refusal-v1.json",
        "GENERATED_EVIDENCE_WITH_PRESERVED_PAYLOAD_RIGHTS", "Lossless original Apache input helper and actual failed preflight")
    inventory.update(schema="M3_CURRENT_RUNTIME_RESTORATION_CANONICAL_ARTIFACTS_V2",
                     source_inventory_sha256=sha(source_inventory), files=[records[key] for key in sorted(records)],
                     file_count=len(records), size_bytes=sum(entry["size_bytes"] for entry in records.values()),
                     source_successor={"path": relative, "before_sha256": sha(before), "after_sha256": updated["sha256"],
                                       "scope": "One archive command explicitly selects tree-compatible tar permissions; mode/blob checks unchanged."})
    inventory_path = work / "canonical-artifacts.json"
    inventory_path.write_bytes(encoded(inventory))
    command = [sys.executable, "-B", str(successor), "--source-inventory", str(inventory_path), *common,
               "--output", str(work / "preparation")]
    result = subprocess.run(command, capture_output=True, timeout=240)
    (work / "preparation.stdout.log").write_bytes(result.stdout)
    (work / "preparation.stderr.log").write_bytes(result.stderr)
    if result.returncode:
        raise ValueError("source successor preparation failed: " + result.stderr.decode())
    print(json.dumps({"inventory": str(inventory_path), "sha256": sha(inventory_path.read_bytes()),
                      "files": len(records), "bytes": inventory["size_bytes"],
                      "preparation": json.loads(result.stdout)}, sort_keys=True))


if __name__ == "__main__":
    main()
