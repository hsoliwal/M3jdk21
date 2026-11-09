#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
# Adapted 2026: require explicit immutable baseline and reuse an explicit existing recipe crate.
"""Seal final source/evidence custody against immutable Git preimages."""

import argparse
import hashlib
import importlib.util
import json
import re
from pathlib import Path
import subprocess
import sys

sys.dont_write_bytecode = True
DATASET = "m3-feature-final-image-custody"
PUBLIC = "m3/arrays/qualification/empty-segments-20261017/recipe-custody/"


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, required=True)
    parser.add_argument("--baseline", required=True)
    parser.add_argument("--crate", type=Path, required=True)
    parser.add_argument("--recipe-owner", type=Path, required=True)
    parser.add_argument("--candidate", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--dataset", default=DATASET)
    args = parser.parse_args()
    if not re.fullmatch(r"[0-9a-f]{40}", args.baseline):
        raise ValueError("baseline must be an immutable commit SHA")
    observed_base = subprocess.check_output(["git", "-C", str(args.repository.resolve()),
            "rev-parse", "--verify", args.baseline + "^{commit}"]).decode().strip()
    if observed_base != args.baseline:
        raise ValueError("baseline does not identify the requested commit")
    work = args.work_dir.resolve()
    work.mkdir(parents=True, exist_ok=False)
    baseline = work / "baseline"
    baseline.mkdir()
    candidate = args.candidate.resolve()
    paths = sorted(p for p in candidate.rglob("*") if p.is_file())
    if not paths or len(paths) > 256:
        raise ValueError("final evidence must respect the unchanged text target budget")
    records = []
    for source in paths:
        relative = source.relative_to(candidate).as_posix()
        if not relative.startswith(PUBLIC) or source.suffix == ".java":
            raise ValueError("final evidence targets must stay in the existing public custody owner")
        found = subprocess.run(["git", "-C", str(args.repository.resolve()), "show", args.baseline + ":" + relative],
                               capture_output=True)
        before = "ABSENT"
        blob = None
        if found.returncode == 0:
            data = found.stdout
            before = sha(data)
            blob = subprocess.check_output(["git", "-C", str(args.repository.resolve()), "rev-parse", args.baseline + ":" + relative]).decode().strip()
            observed = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
            if observed != blob:
                raise ValueError("immutable evidence baseline Git identity mismatch")
            destination = baseline / relative
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
        else:
            # ls-tree must independently establish absence; do not turn another
            # Git failure into an invented absent preimage.
            exists = subprocess.check_output(["git", "-C", str(args.repository.resolve()), "ls-tree", args.baseline, "--", relative])
            if exists:
                raise ValueError("Git source lookup failed for an existing baseline target")
        records.append({"path": relative, "before_sha256": before, "before_git_blob": blob,
                        "after_sha256": sha(source.read_bytes())})
    owner = args.recipe_owner.resolve()
    crate = args.crate.resolve()
    spec = importlib.util.spec_from_file_location("final_evidence_prepare", crate / "prepare.py")
    prepare = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(prepare)
    metadata = prepare.seal(crate, args.dataset, "hsoliwal/M3jdk21", baseline, [candidate], args.baseline, owner)
    receipt = {"schema": "M3_FEATURE_FINAL_PUBLIC_EVIDENCE_PREPARATION_V1", "baseline": args.baseline,
               "dataset": args.dataset, "metadata": metadata, "targets": records,
               "runtime_product_sources_changed": False, "recipe_engine_changes": [],
               "self_recursive_receiving_recipe_included": False,
               "existing_public_sdk_engines_and_tests": "REUSED_UNCHANGED",
               "publication_claim": "None: root owns later receiving and publication"}
    path = work / "preparation-receipt.json"
    path.write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({"dataset": args.dataset, "targets": len(records), "receipt": str(path)}))


if __name__ == "__main__":
    main()
