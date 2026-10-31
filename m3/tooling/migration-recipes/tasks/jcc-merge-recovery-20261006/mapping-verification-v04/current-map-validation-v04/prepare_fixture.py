#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Assemble only the unchanged canonical validator's exact 090 artifact closure through immutable object reuse."""
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
CURRENT = "09075810b55229662329a158923287e8a7118f86"
TREE = "bba8647ea80d1cb4101394b4c0fc5a97f0b8eb52"
EPOCH_SHA = "4fd3b3bd4fb09759d8bb02ca9ba45d0b2934625f24bcc4ae195f3e49394dff70"
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
    old = AUDIT / "current-map-validation-v03"
    require(not (HERE / "fixture").exists(), "fixture already exists")
    old_packet = (old / "PACKET.json").read_bytes()
    require(sha(old_packet) == "6e2ab166b48662dbf216c9791597d7779163236a0dc2e4ce76c2963bb022882c", "V03 frozen packet drift")
    old_sealed = {row["path"]: row for row in json.loads(old_packet)["files"]}
    captures = [ROOT / "publication" / ("M3_LATEST_" + name + ".json") for name in ["COMMIT", "ROOT", "M3"]]
    old_captures = [old / "root-evidence" / ("M3_FINAL_CURRENT_" + name + ".json") for name in ["COMMIT", "ROOT", "M3"]]
    commit, root, m3 = [captured_json(path) for path in captures]
    old_commit, old_root, old_m3 = [captured_json(path) for path in old_captures]
    require(commit["sha"] == CURRENT and commit["tree"]["sha"] == TREE, "latest commit/root mismatch")
    require(old_commit["sha"] == "c0a14387009aefc7d62bd3268055d526e04f9074" and old_commit["tree"]["sha"] == old_root["sha"], "C0 commit/root mismatch")
    historical_root_path = old / "root-evidence/M3_FOLLOWUP_CURRENT_ROOT.json"
    historical_root = captured_json(historical_root_path)
    for tree in [root, old_root, historical_root]:
        require(tree["truncated"] is False and direct_tree(tree["tree"]) == tree["sha"], "root tree reconstruction failed")
    require(root["sha"] == TREE, "latest root identity mismatch")
    roots = [{row["path"]: row for row in tree["tree"]} for tree in [root, old_root, historical_root]]
    require(roots[0]["m3"]["sha"] == m3["sha"] and roots[1]["m3"]["sha"] == old_m3["sha"], "disconnected m3 roots")
    require(roots[1]["src"] == roots[2]["src"], "C0-to-d1 source capture ancestry drift")
    tree_count, old_tree_count = recursive_tree(m3), recursive_tree(old_m3)
    entries = {"m3/" + row["path"]: row for row in m3["tree"]}
    old_entries = {"m3/" + row["path"]: row for row in old_m3["tree"]}

    # Reconstruct every acquired direct tree, then walk both roots to the exact
    # unchanged subtrees that contain the earlier required Java/native bodies.
    direct = {tree["sha"]: tree for tree in [root, old_root, historical_root]}
    tree_captures, file_captures = [], {}
    for path in sorted((HERE / "acquisition/captures").glob("*.json")):
        envelope = json.loads(path.read_bytes())
        request = envelope["request"]
        if "path" in request:
            file_captures[request["path"]] = path
            continue
        tree = captured_json(path)
        if "tree" not in tree:
            continue
        require(tree["truncated"] is False and direct_tree(tree["tree"]) == tree["sha"], "acquired direct tree identity mismatch")
        if tree["sha"] in direct:
            require(tree["tree"] == direct[tree["sha"]]["tree"], "inconsistent direct tree capture")
        direct[tree["sha"]] = tree
        tree_captures.append({"path": path.relative_to(HERE).as_posix(), "sha256": sha(path.read_bytes()), "tree": tree["sha"]})

    def walk(root_id, path):
        oid = root_id
        for part in path.split("/"):
            require(oid in direct, "missing required ancestry tree: " + oid)
            row = next((row for row in direct[oid]["tree"] if row["path"] == part), None)
            require(row is not None and row["type"] == "tree", "missing required ancestry component: " + part)
            oid = row["sha"]
        return oid

    prefixes = ["src/java.base/share/classes/jdk/internal/mindex", "src/java.base/share/native/libjava"]
    equal_subtrees = []
    for prefix in prefixes:
        previous, current = walk(old_root["sha"], prefix), walk(TREE, prefix)
        require(previous == current, "required source subtree changed: " + prefix)
        equal_subtrees.append({"path": prefix, "old_tree": previous, "current_tree": current})

    epoch_root = AUDIT / "candidate/mapping-v04/input-epoch"
    epoch_bytes = (epoch_root / "INPUT_EPOCH.json").read_bytes()
    require(sha(epoch_bytes) == EPOCH_SHA, "V04 epoch drift")
    epoch = json.loads(epoch_bytes)
    require(epoch["schema"] == "m3-jcc-current-mapping-input-epoch/4" and epoch["commit"] == CURRENT and epoch["root_tree"] == TREE, "V04 epoch identity mismatch")
    map_bytes = (epoch_root / "current" / MAP).read_bytes()
    document = json.loads(map_bytes)
    old_map = json.loads((old / "fixture" / MAP).read_bytes())
    require(document["migration"]["records"][:52] == old_map["migration"]["records"] and len(document["migration"]["records"]) == 55,
            "all prior52complete records must remain ordered and unchanged")
    require(document["mappings"][:25] == old_map["mappings"] and len(document["mappings"]) == 34, "all prior25mapping nodes must remain ordered and unchanged")
    require(set(document) - set(old_map) == {"donor_artifact_policy", "porting_invariant", "family_name_mapping"}, "unexpected new global map fields")
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
    old_required_paths = {row["path"] for row in old_requirements["required_target_recipe_paths"]}
    new_paths = {"src/java.base/share/classes/java/util/regex/Matcher.java",
                 "m3/tooling/mr/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned/jdk22-m3-mr/manifest.tsv"}
    require(set(requirements) == old_required_paths | new_paths and len(requirements) == 32, "canonical artifact scope changed unexpectedly")
    require(tested == {row["path"]: row["tests"] for row in old_requirements["tested_receipts"]}, "tested receipt scope changed")
    closure = json.loads((old / "RECEIPT_CLOSURE.json").read_bytes())
    required = set(requirements) | set(tested) | {row["path"] for row in closure["closure_paths"]}
    require(len(required) == 46, "artifact closure must contain exactly46paths")
    bodies, readbacks = {}, []
    old_acquisition = json.loads((old / "ACQUISITION.json").read_bytes())
    jobs = [(row["path"], old / row["capture"], row) for row in old_acquisition["readbacks"]]
    jobs.extend((path, file_captures[path], None) for path in sorted(new_paths))
    for relative, path, prior in jobs:
        require(relative in required and relative not in bodies, "unexpected acquisition path")
        envelope = json.loads(path.read_bytes())
        origin = prior["origin_commit"] if prior else CURRENT
        require(envelope["request"] == {"repository_full_name": "hsoliwal/M3jdk21", "path": relative, "ref": origin, "encoding": "base64"}, "capture request identity mismatch")
        require(envelope["response"].get("isError") is False, "failed exact immutable read")
        content = envelope["response"]["structuredContent"]
        require(content["encoding"] == "base64" and content["display_url"] == "https://github.com/hsoliwal/M3jdk21/blob/" + origin + "/" + relative, "capture returned wrong path/ref")
        body = base64.b64decode("".join(content["content"].split()), validate=True)
        identity = git_object("blob", body)
        require(identity == content["sha"], "capture body Git identity mismatch")
        membership = "Exact latest immutable commit/path contents readback"
        destination = path
        if prior:
            require(sha(path.read_bytes()) == prior["capture_sha256"] == old_sealed[path.relative_to(old).as_posix()]["sha256"], "frozen earlier capture drift")
            require(identity == prior["git_blob_sha1"] and sha(body) == prior["sha256"] and len(body) == prior["bytes"], "earlier body identity drift")
            destination = HERE / "calls/reused" / path.name
            destination.parent.mkdir(parents=True, exist_ok=True)
            require(not destination.exists(), "duplicate reused capture filename")
            shutil.copyfile(path, destination)
            if relative.startswith("m3/"):
                require(entries[relative] == old_entries[relative], "earlier required m3 entry changed")
                membership = "Exact C0/latest path-mode-size-Git identity in independently reconstructed m3 trees"
            else:
                require(any(relative.startswith(prefix + "/") for prefix in prefixes), "unproven source capture reuse")
                membership = "Exact original commit/path capture; d1/C0 src identity and reconstructed C0/latest ancestry to identical required source subtree"
        if relative.startswith("m3/"):
            entry = entries[relative]
            require(entry["type"] == "blob" and entry["sha"] == identity and entry["size"] == len(body), "latest m3 tree/body mismatch")
        bodies[relative] = body
        readbacks.append({"path": relative, "bytes": len(body), "sha256": sha(body), "git_blob_sha1": identity,
                          "origin_commit": origin, "current_commit": CURRENT, "reused": prior is not None,
                          "capture": destination.relative_to(HERE).as_posix(), "capture_sha256": sha(destination.read_bytes()), "membership": membership})
    require(set(bodies) == required and sum(row["reused"] for row in readbacks) == 44, "missing/extra body acquisition")
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
        require(sha(body) == row["sha256"] and git_object("blob", body) == row["git_blob"] == entry["sha"] and len(body) == row["bytes"] == entry["size"], "frozen latest local input drift")
        require(relative not in bodies, "fixture overlap")
        bodies[relative] = body
        local_inputs.append({"path": relative, "sha256": sha(body), "git_blob_sha1": entry["sha"], "bytes": len(body), "source": str(epoch_root / row["file"])})
    require(len(bodies) == 52 and len(local_inputs) == 6, "final fixture scope drift")
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
    for path in captures + old_captures + [historical_root_path]:
        shutil.copyfile(path, evidence / path.name)
    (evidence / "INPUT_EPOCH.json").write_bytes(epoch_bytes)
    prior_evidence = HERE / "prior-evidence"
    prior_evidence.mkdir()
    for name in ["PACKET.json", "ACQUISITION.json", "FIXTURE_MANIFEST.json"]:
        shutil.copyfile(old / name, prior_evidence / name)
    dump(HERE / "REQUIREMENTS.json", {**old_requirements, "commit": CURRENT, "root_tree": TREE, "mapping_sha256": sha(map_bytes),
         "target_recipe_unique_paths": 32, "required_receipt_closure_pending": False,
         "required_target_recipe_paths": [{"path": path, "references": refs} for path, refs in sorted(requirements.items())]})
    dump(HERE / "RECEIPT_CLOSURE.json", closure)
    acquisition = {"schema": "m3-current-map-validator-acquisition/4", "status": "EXACT_CURRENT_BODIES_VERIFIED", "commit": CURRENT, "root_tree": TREE,
                   "input_epoch_sha256": EPOCH_SHA, "acquired_artifact_paths": 46, "reused_artifacts": 44, "fresh_artifacts": 2,
                   "fixture_files": 52, "canonical_validator_and_schema_paths": 2, "frozen_current_mapping_images": 4,
                   "verified_m3_tree_objects": tree_count, "verified_old_m3_tree_objects": old_tree_count,
                   "equal_required_source_subtrees": equal_subtrees, "verified_source_ancestry_captures": tree_captures,
                   "old_packet_sha256": sha(old_packet), "readbacks": readbacks, "frozen_inputs": local_inputs,
                   "declared_hash_comparisons": comparisons, "declared_hash_mismatches": [row for row in comparisons if not row["sha256_matches_declared"]],
                   "mapping_preservation": {"old52complete_records_ordered_unchanged": True, "old25mapping_nodes_ordered_unchanged": True,
                       "current_records": 55, "current_non_jcc_records": 53, "current_mappings": 34, "current_gates": 20,
                       "new_global_keys": ["donor_artifact_policy", "family_name_mapping", "porting_invariant"]},
                   "root_readback_files": [{"path": "root-evidence/" + path.name, "sha256": sha(path.read_bytes())} for path in captures + old_captures + [historical_root_path]],
                   "scope": "Exact admitted090 baseline only. Earlier artifacts reused only after immutable path/blob/subtree proof; two new MR checked bodies captured. Prior failures and V03 actual results remain unchanged. No metadata/code/test repair or historical command execution."}
    dump(HERE / "ACQUISITION.json", acquisition)
    dump(HERE / "FIXTURE_MANIFEST.json", {"schema": "m3-current-map-validator-fixture/4", "commit": CURRENT, "root_tree": TREE, "files": fixture_inventory(fixture), "source_bound_afterimages": False})
    print(json.dumps({"status": acquisition["status"], "artifacts": 46, "reused": 44, "fresh": 2, "fixture_files": 52,
                      "declared_hash_mismatches": len(acquisition["declared_hash_mismatches"])}))


if __name__ == "__main__":
    main()

