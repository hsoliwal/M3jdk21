#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Assemble only the unchanged canonical validator's exact C0 artifact closure through immutable object reuse."""
from __future__ import annotations

import base64
import hashlib
import importlib.metadata
import json
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys

HERE = Path(__file__).resolve().parent
AUDIT = HERE.parent
ROOT = AUDIT.parent
CURRENT = "c0a14387009aefc7d62bd3268055d526e04f9074"
TREE = "491bbd80a51483d0710fc12c1bad3a037b385ba0"
EPOCH_SHA = "027e47d20c7fb854e028487ceef341c929877a6421dbb75f6433b5cd007fe5ee"
MAP = "m3/docs/name-mapping.json"
VALIDATOR = "m3/migration/migration.py"
SCHEMA = "m3/docs/name-mapping.schema.json"


def sha(body):
    return hashlib.sha256(body).hexdigest()


def git_object(kind, body):
    return hashlib.sha1(kind.encode() + b" " + str(len(body)).encode() + b"\0" + body).hexdigest()


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def safe(relative):
    require(isinstance(relative, str) and bool(relative) and "\\" not in relative
            and not relative.startswith("/") and ":" not in relative, "unsafe relative path")
    require(all(part not in ("", ".", "..", ".git") for part in relative.split("/")), "unsafe path component")
    require(not any(ord(char) < 32 for char in relative), "control in path")
    return PurePosixPath(relative)


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("xb") as stream:
        stream.write((json.dumps(value, indent=2, sort_keys=True) + "\n").encode())


def captured_json(path):
    response = json.loads(path.read_bytes())["response"]
    require(response.get("isError") is False, "failed root readback")
    return json.loads(response["structuredContent"]["content"])


def direct_tree(entries):
    require(len({row["path"] for row in entries}) == len(entries), "duplicate tree entry")
    body = bytearray()
    for row in sorted(entries, key=lambda entry: (entry["path"] + ("/" if entry["type"] == "tree" else "")).encode()):
        require("/" not in str(safe(row["path"])), "non-direct tree entry")
        body.extend(row["mode"].lstrip("0").encode() + b" " + row["path"].encode() + b"\0")
        body.extend(bytes.fromhex(row["sha"]))
    return git_object("tree", bytes(body))


def recursive_tree(tree):
    require(tree["truncated"] is False, "truncated current m3 tree")
    entries = tree["tree"]
    require(len({row["path"] for row in entries}) == len(entries), "duplicate recursive path")
    expected = {"": tree["sha"]}
    expected.update({row["path"]: row["sha"] for row in entries if row["type"] == "tree"})
    for directory, oid in expected.items():
        prefix = directory + "/" if directory else ""
        children = []
        for row in entries:
            if row["path"].startswith(prefix):
                relative = row["path"][len(prefix):]
                if relative and "/" not in relative:
                    children.append({**row, "path": relative})
        require(direct_tree(children) == oid, "current m3 tree reconstruction failed: " + directory)
    return len(expected)


def fixture_inventory(root):
    return [{"path": path.relative_to(root).as_posix(), "bytes": path.stat().st_size,
             "sha256": sha(path.read_bytes()), "git_blob_sha1": git_object("blob", path.read_bytes())}
            for path in sorted(root.rglob("*")) if path.is_file()]


def main():
    old = AUDIT / "current-map-validation"
    require(not (HERE / "fixture").exists(), "fixture already exists")
    old_packet = (old / "PACKET.json").read_bytes()
    require(sha(old_packet) == "ac86b7dfedc336871e8a6f79cb340e4717da031c3fea8e3769fc3b7524b98f50", "old frozen packet drift")
    captures = [ROOT / "publication" / ("M3_FINAL_CURRENT_" + name + ".json") for name in ["COMMIT", "ROOT", "M3"]]
    old_captures = [old / "root-evidence" / ("M3_FOLLOWUP_CURRENT_" + name + ".json") for name in ["COMMIT", "ROOT", "M3"]]
    commit, root, m3 = [captured_json(path) for path in captures]
    old_commit, old_root, old_m3 = [captured_json(path) for path in old_captures]
    require(commit["sha"] == CURRENT and commit["tree"]["sha"] == TREE, "current commit mismatch")
    require(old_commit["sha"] == "d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2" and old_commit["tree"]["sha"] == old_root["sha"], "old commit mismatch")
    for tree in [root, old_root]:
        require(tree["truncated"] is False and direct_tree(tree["tree"]) == tree["sha"], "root reconstruction mismatch")
    require(root["sha"] == TREE, "wrong current root")
    root_entries = {row["path"]: row for row in root["tree"]}
    old_root_entries = {row["path"]: row for row in old_root["tree"]}
    require(root_entries["m3"]["sha"] == m3["sha"] and old_root_entries["m3"]["sha"] == old_m3["sha"], "disconnected m3 trees")
    tree_count, old_tree_count = recursive_tree(m3), recursive_tree(old_m3)
    entries = {"m3/" + row["path"]: row for row in m3["tree"]}
    old_entries = {"m3/" + row["path"]: row for row in old_m3["tree"]}
    require(root_entries["src"] == old_root_entries["src"], "src subtree changed; individual paths need new readbacks")

    epoch_root = AUDIT / "candidate/mapping-v03/input-epoch"
    epoch_bytes = (epoch_root / "INPUT_EPOCH.json").read_bytes()
    require(sha(epoch_bytes) == EPOCH_SHA, "V03 epoch drift")
    epoch = json.loads(epoch_bytes)
    require(epoch["commit"] == CURRENT and epoch["root_tree"] == TREE and epoch["schema"] == "m3-jcc-current-mapping-input-epoch/3", "V03 epoch identity mismatch")
    map_bytes = (epoch_root / "current" / MAP).read_bytes()
    document = json.loads(map_bytes)
    requirements, tested = {}, {}
    for record in document["migration"]["records"]:
        if record["status"] in {"implemented-tested", "implemented-unverified"}:
            rows = [("target", row) for row in record["targets"]]
            if record["recipe"].get("path"):
                rows.append(("recipe", record["recipe"]))
            for role, row in rows:
                requirements.setdefault(row["path"], []).append({"record": record["id"], "status": record["status"], "role": role,
                    "expected_sha256": row["sha256"], "declared_git_blob_sha1": row.get("git_blob_sha1")})
        if record["status"] == "implemented-tested":
            for test in record["tests"]:
                tested.setdefault(test["receipt"], []).append({"record": record["id"], "test_id": test["id"]})
    old_requirements = json.loads((old / "REQUIREMENTS.json").read_bytes())
    require(set(requirements) == {row["path"] for row in old_requirements["required_target_recipe_paths"]} and len(requirements) == 30, "required target/recipe path scope changed")
    require(tested == {row["path"]: row["tests"] for row in old_requirements["tested_receipts"]}, "tested receipt scope changed")
    closure = json.loads((old / "RECEIPT_CLOSURE.json").read_bytes())
    required = set(requirements) | set(tested) | {row["path"] for row in closure["closure_paths"]}
    require(len(required) == 44, "artifact closure scope changed")
    bodies, readbacks = {}, []
    old_acquisition = json.loads((old / "ACQUISITION.json").read_bytes())
    for row in old_acquisition["readbacks"]:
        relative = row["path"]
        require(relative in required and relative not in bodies, "unexpected old acquisition")
        capture_path = old / row["capture"]
        require(sha(capture_path.read_bytes()) == row["capture_sha256"], "old capture changed")
        same = (entries[relative] == old_entries[relative]) if relative.startswith("m3/") else relative.startswith("src/")
        if same:
            destination = HERE / "calls/reused" / capture_path.name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(capture_path, destination)
            origin = old_commit["sha"]
        else:
            require(relative == "m3/tooling/tq/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-tq/manifest.tsv", "unreviewed changed artifact")
            destination = HERE / "calls/changed-tq-manifest.json"
            origin = CURRENT
        envelope = json.loads(destination.read_bytes())
        require(envelope["request"] == {"repository_full_name": "hsoliwal/M3jdk21", "path": relative, "ref": origin, "encoding": "base64"}, "capture request mismatch")
        response = envelope["response"]
        require(response.get("isError") is False, "failed capture")
        content = response["structuredContent"]
        require(content["encoding"] == "base64" and content["display_url"] == "https://github.com/hsoliwal/M3jdk21/blob/" + origin + "/" + relative, "capture response identity mismatch")
        body = base64.b64decode("".join(content["content"].split()), validate=True)
        identity = git_object("blob", body)
        require(identity == content["sha"], "capture Git identity mismatch")
        if relative.startswith("m3/"):
            entry = entries[relative]
            require(entry["type"] == "blob" and entry["sha"] == identity and entry["size"] == len(body), "current m3 tree/body mismatch")
            membership = "Reconstructed C0 m3 tree exact path/mode/Git blob identity" + (" equals reconstructed d1 m3 entry" if same else "; fresh exact-C0 contents capture")
        else:
            require(identity == row["git_blob_sha1"] and sha(body) == row["sha256"], "old src body identity changed")
            membership = "Exact d1 commit/path contents capture plus identical reconstructed d1/C0 src subtree " + root_entries["src"]["sha"]
        bodies[relative] = body
        readbacks.append({"path": relative, "bytes": len(body), "sha256": sha(body), "git_blob_sha1": identity,
                          "origin_commit": origin, "current_commit": CURRENT, "reused": same,
                          "capture": destination.relative_to(HERE).as_posix(), "capture_sha256": sha(destination.read_bytes()), "membership": membership})
    require(set(bodies) == required and sum(row["reused"] for row in readbacks) == 43, "incorrect acquisition scope")
    # Reconstruct the selected receipt closure from the exact current receipt body.
    closure_refs = {}
    for receipt_path, tests in tested.items():
        receipt = json.loads(bodies[receipt_path])
        for test in sorted(tests, key=lambda row: row["test_id"]):
            matches = [run for run in receipt["runs"] if run["id"] == test["test_id"]]
            require(len(matches) == 1, "selected receipt run mismatch")
            run = matches[0]
            for path, expected in run["inputs"].items():
                closure_refs.setdefault(path, []).append({"role": "receipt-input", "test_id": test["test_id"], "expected_sha256": expected})
            if run.get("stdout_path"):
                closure_refs.setdefault(run["stdout_path"], []).append({"role": "receipt-stdout", "test_id": test["test_id"], "expected_sha256": run["stdout_sha256"]})
    require(closure_refs == {row["path"]: row["references"] for row in closure["closure_paths"]}, "selected receipt closure drift")
    local_inputs = []
    for row in epoch["current_operational_preimages"] + [row for row in epoch["guards"] if row["path"] in (VALIDATOR, SCHEMA)]:
        relative = row["path"]
        body = (epoch_root / row["file"]).read_bytes()
        entry = entries[relative]
        require(sha(body) == row["sha256"] and git_object("blob", body) == row["git_blob"] == entry["sha"] and len(body) == row["bytes"] == entry["size"], "frozen C0 local input drift")
        require(relative not in bodies, "fixture overlap")
        bodies[relative] = body
        local_inputs.append({"path": relative, "sha256": sha(body), "git_blob_sha1": entry["sha"], "bytes": len(body), "source": str(epoch_root / row["file"])})
    require(len(bodies) == 50 and len(local_inputs) == 6, "final fixture scope drift")
    for canonical in [VALIDATOR, SCHEMA]:
        require(bodies[canonical] == (old / "fixture" / canonical).read_bytes(), "canonical validator/schema changed")
    comparisons = [{"path": path, **reference, "actual_sha256": sha(bodies[path]), "actual_git_blob_sha1": git_object("blob", bodies[path]), "sha256_matches_declared": sha(bodies[path]) == reference["expected_sha256"]}
                   for group in [requirements, closure_refs] for path, refs in sorted(group.items()) for reference in refs]
    fixture = HERE / "fixture"
    fixture.mkdir()
    for relative, body in sorted(bodies.items()):
        target = fixture / safe(relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(body)
    evidence = HERE / "root-evidence"
    evidence.mkdir()
    for path in captures + old_captures:
        shutil.copyfile(path, evidence / path.name)
    (evidence / "INPUT_EPOCH.json").write_bytes(epoch_bytes)
    requirements_document = {**old_requirements, "commit": CURRENT, "root_tree": TREE, "mapping_sha256": sha(map_bytes),
                             "required_receipt_closure_pending": False, "required_target_recipe_paths": [{"path": path, "references": refs} for path, refs in sorted(requirements.items())]}
    dump(HERE / "REQUIREMENTS.json", requirements_document)
    dump(HERE / "RECEIPT_CLOSURE.json", closure)
    acquisition = {"schema": "m3-current-map-validator-acquisition/3", "status": "EXACT_CURRENT_BODIES_VERIFIED", "commit": CURRENT, "root_tree": TREE,
                   "input_epoch_sha256": EPOCH_SHA, "acquired_artifact_paths": 44, "reused_artifacts": 43, "fresh_artifacts": 1,
                   "fixture_files": 50, "canonical_validator_and_schema_paths": 2, "frozen_current_mapping_images": 4,
                   "verified_m3_tree_objects": tree_count, "verified_old_m3_tree_objects": old_tree_count,
                   "identical_src_subtree": root_entries["src"], "old_packet_sha256": sha(old_packet),
                   "readbacks": readbacks, "frozen_inputs": local_inputs, "declared_hash_comparisons": comparisons,
                   "declared_hash_mismatches": [row for row in comparisons if not row["sha256_matches_declared"]],
                   "root_readback_files": [{"path": "root-evidence/" + path.name, "sha256": sha(path.read_bytes())} for path in captures + old_captures],
                   "scope": "Exact C0 current fixture. Old immutable captures reused only after exact path/blob or whole-src subtree proof. Historical d1 failures retained unchanged in prior packet. No metadata/code/test repairs."}
    dump(HERE / "ACQUISITION.json", acquisition)
    dump(HERE / "FIXTURE_MANIFEST.json", {"schema": "m3-current-map-validator-fixture/3", "commit": CURRENT, "root_tree": TREE, "files": fixture_inventory(fixture), "source_bound_afterimages": False})
    print(json.dumps({"status": acquisition["status"], "artifacts": 44, "reused": 43, "fresh": 1, "declared_hash_mismatches": len(acquisition["declared_hash_mismatches"])}))


if __name__ == "__main__":
    main()

