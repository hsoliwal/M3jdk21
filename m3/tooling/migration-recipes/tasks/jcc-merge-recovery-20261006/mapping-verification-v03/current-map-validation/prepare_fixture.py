#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Assemble only the unchanged canonical validator's exact d1b artifact closure."""
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
CURRENT = "d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2"
TREE = "0812573f02908d8078d13f5f49346865cdf1c0e0"
EPOCH_SHA = "fb413e58be72aaffa1e91149479ac159c6d4b961101eed4ab10f1c99df0b6711"
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
    fixture = HERE / "fixture"
    require(not fixture.exists(), "fixture already exists")
    requirements = json.loads((HERE / "REQUIREMENTS.json").read_bytes())
    closure = json.loads((HERE / "RECEIPT_CLOSURE.json").read_bytes())
    required = {row["path"] for row in requirements["required_target_recipe_paths"]}
    required.update(row["path"] for row in requirements["tested_receipts"])
    required.update(row["path"] for row in closure["closure_paths"])
    require(len(required) == 44, "artifact closure exceeded the reviewed 44 paths")

    names = ["M3_FOLLOWUP_CURRENT_COMMIT.json", "M3_FOLLOWUP_CURRENT_ROOT.json", "M3_FOLLOWUP_CURRENT_M3.json"]
    captures = [ROOT / "publication" / name for name in names]
    commit, root, m3 = [captured_json(path) for path in captures]
    require(commit["sha"] == CURRENT and commit["tree"]["sha"] == TREE, "immutable commit readback changed")
    require(root["sha"] == TREE and root["truncated"] is False and direct_tree(root["tree"]) == TREE,
            "current root identity mismatch")
    root_entries = {row["path"]: row for row in root["tree"]}
    require(root_entries["m3"]["sha"] == m3["sha"], "m3 tree disconnected from current root")
    tree_count = recursive_tree(m3)
    m3_entries = {"m3/" + row["path"]: row for row in m3["tree"]}

    bodies, readbacks = {}, []
    for path in sorted((HERE / "calls").glob("*.json")):
        envelope = json.loads(path.read_bytes())
        request = envelope["request"]
        relative = request["path"]
        safe(relative)
        require(relative in required and relative not in bodies, "unexpected/duplicate acquired artifact")
        require(request == {"repository_full_name": "hsoliwal/M3jdk21", "path": relative,
                            "ref": CURRENT, "encoding": "base64"}, "mixed/truncated acquisition request")
        response = envelope["response"]
        require(response.get("isError") is False, "failed immutable read: " + relative)
        content = response["structuredContent"]
        require(content["encoding"] == "base64", "unsupported response encoding")
        body = base64.b64decode("".join(content["content"].split()), validate=True)
        identity = git_object("blob", body)
        require(identity == content["sha"], "Git blob identity mismatch: " + relative)
        expected_url = "https://github.com/hsoliwal/M3jdk21/blob/" + CURRENT + "/" + relative
        require(content["display_url"] == expected_url, "returned immutable path/ref mismatch")
        membership = "GitHub contents response at the exact immutable commit/path"
        mode = None
        if relative.startswith("m3/"):
            entry = m3_entries[relative]
            require(entry["type"] == "blob" and entry["sha"] == identity and entry["size"] == len(body),
                    "body differs from reconstructed current m3 tree: " + relative)
            membership += "; independently reconstructed m3 tree connected to current root"
            mode = entry["mode"]
        bodies[relative] = body
        readbacks.append({"path": relative, "bytes": len(body), "sha256": sha(body), "git_blob_sha1": identity,
                          "mode_from_tree": mode, "capture": str(path.relative_to(HERE)),
                          "capture_sha256": sha(path.read_bytes()), "membership": membership})
    require(set(bodies) == required, "missing required acquisition")

    epoch_root = AUDIT / "candidate/mapping-v02/input-epoch"
    epoch_bytes = (epoch_root / "INPUT_EPOCH.json").read_bytes()
    require(sha(epoch_bytes) == EPOCH_SHA, "frozen receiving epoch changed")
    epoch = json.loads(epoch_bytes)
    require(epoch["commit"] == CURRENT and epoch["root_tree"] == TREE, "wrong frozen receiving epoch")
    local_inputs = []
    for row in epoch["current_operational_preimages"] + [row for row in epoch["guards"] if row["path"] in (VALIDATOR, SCHEMA)]:
        relative = row["path"]
        body = (epoch_root / row["file"]).read_bytes()
        entry = m3_entries[relative]
        require(sha(body) == row["sha256"] and git_object("blob", body) == row["git_blob"] == entry["sha"]
                and len(body) == row["bytes"] == entry["size"], "frozen current body drift: " + relative)
        require(relative not in bodies, "unexpected fixture overlap")
        bodies[relative] = body
        local_inputs.append({"path": relative, "source": str(epoch_root / row["file"]), "sha256": sha(body),
                             "git_blob_sha1": entry["sha"], "bytes": len(body),
                             "membership": "Frozen epoch body matches fully reconstructed immutable m3 tree"})
    require(len(local_inputs) == 6 and len(bodies) == 50, "unexpected final fixture scope")

    mismatches = []
    comparisons = []
    for row in requirements["required_target_recipe_paths"] + closure["closure_paths"]:
        for reference in row["references"]:
            actual = sha(bodies[row["path"]])
            comparison = {"path": row["path"], **reference, "actual_sha256": actual,
                          "actual_git_blob_sha1": git_object("blob", bodies[row["path"]]),
                          "sha256_matches_declared": actual == reference["expected_sha256"]}
            comparisons.append(comparison)
            if not comparison["sha256_matches_declared"]:
                mismatches.append(comparison)

    # Current-body mismatches are retained evidence, not a reason to substitute old bytes.
    fixture.mkdir()
    for relative, body in sorted(bodies.items()):
        target = fixture / safe(relative)
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as stream:
            stream.write(body)
    provenance = HERE / "root-evidence"
    provenance.mkdir()
    for path in captures:
        shutil.copyfile(path, provenance / path.name)
    (provenance / "INPUT_EPOCH.json").write_bytes(epoch_bytes)
    acquisition = {"schema": "m3-current-map-validator-acquisition/1", "status": "EXACT_CURRENT_BODIES_VERIFIED",
                   "repository": "hsoliwal/M3jdk21", "commit": CURRENT, "root_tree": TREE,
                   "acquired_artifact_paths": 44, "canonical_validator_and_schema_paths": 2,
                   "frozen_current_mapping_images": 4, "fixture_files": 50,
                   "verified_m3_tree_objects": tree_count, "input_epoch_sha256": EPOCH_SHA,
                   "readbacks": readbacks, "frozen_inputs": local_inputs,
                   "root_readback_files": [{"path": "root-evidence/" + path.name, "sha256": sha(path.read_bytes())} for path in captures],
                   "declared_hash_comparisons": comparisons, "declared_hash_mismatches": mismatches,
                   "scope": "Exact immutable bytes acquired for canonical validation only. Mismatched mapping/receipt claims were not repaired. No historical commands executed."}
    dump(HERE / "ACQUISITION.json", acquisition)
    inventory = fixture_inventory(fixture)
    dump(HERE / "FIXTURE_MANIFEST.json", {"schema": "m3-current-map-validator-fixture/1", "commit": CURRENT,
                                         "root_tree": TREE, "files": inventory, "source_bound_afterimages": False})

    execution = HERE / "execution/baseline-v01"
    execution.mkdir(parents=True)
    command = [sys.executable, "-B", str(fixture / VALIDATOR), "validate", str(fixture)]
    result = subprocess.run(command, cwd=fixture, capture_output=True, check=False)
    (execution / "stdout.log").write_bytes(result.stdout)
    (execution / "stderr.log").write_bytes(result.stderr)
    require(fixture_inventory(fixture) == inventory, "canonical validator unexpectedly mutated fixture")
    try:
        jsonschema_version = importlib.metadata.version("jsonschema")
    except importlib.metadata.PackageNotFoundError:
        jsonschema_version = None
    execution_receipt = {"schema": "m3-current-map-canonical-validation/1",
                         "status": "PASS" if result.returncode == 0 else "BASELINE_VALIDATOR_FAILED",
                         "command": command, "exit_code": result.returncode, "commit": CURRENT, "root_tree": TREE,
                         "validator_sha256": sha(bodies[VALIDATOR]), "schema_sha256": sha(bodies[SCHEMA]),
                         "mapping_sha256": sha(bodies[MAP]), "fixture_manifest_sha256": sha((HERE / "FIXTURE_MANIFEST.json").read_bytes()),
                         "stdout_sha256": sha(result.stdout), "stderr_sha256": sha(result.stderr),
                         "diagnostics": result.stderr.decode().splitlines(), "fixture_unchanged": True,
                         "environment": {"python": sys.version, "jsonschema": jsonschema_version},
                         "scope": "Unchanged canonical validator, exact current d1b mapping and required current artifacts. No map/status/source/test changes; no historical receipt commands rerun."}
    dump(execution / "receipt.json", execution_receipt)
    print(json.dumps({"status": execution_receipt["status"], "exit_code": result.returncode,
                      "fixture_files": len(inventory), "acquired_paths": len(readbacks),
                      "declared_hash_mismatches": len(mismatches), "diagnostics": execution_receipt["diagnostics"]}))
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
