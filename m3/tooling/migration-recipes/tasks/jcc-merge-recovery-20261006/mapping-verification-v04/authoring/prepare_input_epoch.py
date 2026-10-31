#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Freeze the exact additive 090758 receiving epoch without changing V03 evidence."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
AUDIT = HERE.parents[1]
ROOT = AUDIT.parent
COMMIT = "09075810b55229662329a158923287e8a7118f86"
TREE = "bba8647ea80d1cb4101394b4c0fc5a97f0b8eb52"
MAP_SHA = "9c96a0f6f7cc0f9b9d3e91fc3a42ef8e4eab212726fccd5e63e483f1acd7dfb1"
MAP_GIT = "466f7c14d3fbe852bf9e2d1dc3e410e8b38a9ee2"
MAP = "m3/docs/name-mapping.json"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    verifier_path = AUDIT / "candidate/mapping/verify_supplemental_inputs.py"
    spec = importlib.util.spec_from_file_location("existing_epoch_verifier", verifier_path)
    verifier = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(verifier)
    prior_root = AUDIT / "candidate/mapping-v03/input-epoch"
    prior_file = prior_root / "INPUT_EPOCH.json"
    assert sha(prior_file) == "027e47d20c7fb854e028487ceef341c929877a6421dbb75f6433b5cd007fe5ee"
    prior = json.loads(prior_file.read_bytes())
    captures = [ROOT / ("publication/M3_LATEST_" + name + ".json") for name in ["COMMIT", "ROOT", "M3", "GITHUB"]]
    commit, root, m3, github = [verifier.captured_json(path) for path in captures]
    assert commit["sha"] == COMMIT and commit["tree"]["sha"] == TREE
    assert root["sha"] == TREE and root["truncated"] is False
    assert verifier.direct_tree_id(root["tree"]) == TREE
    roots = {row["path"]: row for row in root["tree"]}
    assert roots["m3"]["sha"] == m3["sha"] and roots[".github"]["sha"] == github["sha"]
    reconstructed = {"m3": verifier.verify_recursive_tree(m3), ".github": verifier.verify_recursive_tree(github)}
    entries = {"m3/" + row["path"]: row for row in m3["tree"]}
    body_file = ROOT / "publication/M3_LATEST_NAME_MAPPING.body.json"
    body = body_file.read_bytes()
    assert sha(body_file) == MAP_SHA and verifier.git_object("blob", body) == MAP_GIT and len(body) == 308294
    body_capture = ROOT / "publication/M3_LATEST_NAME_MAPPING.json"
    envelope = json.loads(body_capture.read_bytes())
    assert not envelope["response"].get("isError")
    assert envelope["response"]["structuredContent"]["content"].encode() == body
    old = json.loads((prior_root / "current" / MAP).read_bytes())
    current = json.loads(body)
    assert len(current["migration"]["records"]) == 55 and current["migration"]["records"][:52] == old["migration"]["records"]
    assert len(current["mappings"]) == 34 and current["mappings"][:25] == old["mappings"]
    assert len(current["migration"]["gates"]) == 20 and current["migration"]["gates"] == old["migration"]["gates"]
    assert [row["id"] for row in current["migration"]["records"][52:]] == ["synexia.regex.mr", "synexia.regex.rxm", "synexia.regex.rxa"]
    added_keys = {"donor_artifact_policy", "porting_invariant", "family_name_mapping"}
    assert set(current) - set(old) == added_keys and set(old) <= set(current)
    for key in old:
        if key not in ["migration", "mappings"]:
            assert current[key] == old[key]
    assert set(current["migration"]) == set(old["migration"])
    for key in old["migration"]:
        if key != "records":
            assert current["migration"][key] == old["migration"][key]
    updated = copy.deepcopy(prior)
    input_bodies = []
    for group in ["current_operational_preimages", "guards"]:
        for row in updated[group]:
            data = body if row["path"] == MAP else (prior_root / row["file"]).read_bytes()
            identity = verifier.git_object("blob", data)
            if row["path"].startswith("src/"):
                path = ROOT / "publication/M3_LATEST_DESCRIPTOR_GUARD.json"
                captured = json.loads(path.read_bytes())
                assert captured["response"]["structuredContent"]["url"] == "https://api.github.com/repos/hsoliwal/M3jdk21/contents/" + row["path"] + "?ref=" + COMMIT
                assert not captured["response"].get("isError")
                assert captured["response"]["structuredContent"]["content"].encode() == data
                assert identity == row["git_blob"]
                captures.append(path)
            else:
                entry = entries[row["path"]]
                assert entry["sha"] == identity and entry["mode"] == "100644" and entry["type"] == "blob"
                assert entry["size"] == len(data)
            row.update({"commit": COMMIT, "git_blob": identity, "sha256": hashlib.sha256(data).hexdigest(),
                        "bytes": len(data), "membership": "Actual 090758 readback/reconstructed tree; exact-commit descriptor capture"})
            input_bodies.append((row["file"], data))
    updated["prior_record_update"] = updated.pop("current_record_update")
    updated.pop("other_51_record_nodes_unchanged")
    updated.update({"schema": "m3-jcc-current-mapping-input-epoch/4", "input_revision": 1,
                    "commit": COMMIT, "root_tree": TREE, "m3_tree": m3["sha"], "github_tree": github["sha"],
                    "reconstructed_tree_count": reconstructed,
                    "shape": {"ordered_records": 55, "non_jcc_records": 53, "top_level_mappings": 34, "gates": 20},
                    "current_ordered_ids": [row["id"] for row in current["migration"]["records"]],
                    "previous_input_epoch": {"path": str(prior_file), "sha256": sha(prior_file)},
                    "prior_52_complete_records_unchanged_and_ordered": True,
                    "prior_25_complete_mappings_unchanged_and_ordered": True,
                    "concurrent_additions": {"records": current["migration"]["records"][52:],
                                             "mappings": current["mappings"][25:],
                                             "top_level_nodes": {key: current[key] for key in sorted(added_keys)}},
                    "scope": "Exact additive 090758 current map. All qualified V03 outputs/evidence remain immutable history; no target runtime acceptance."})
    captures += [body_file, body_capture, prior_file, verifier_path]
    captures += [ROOT / ("publication/M3_LATEST_" + name) for name in ["GUARDS_AND_MAP_DELTA.json", "AGENTS.json", "AGENTS.md", "PORTING_INVARIANT.json", "PORTING_INVARIANT.md"]]
    updated["captured_evidence"] = [{"path": str(path), "sha256": sha(path)} for path in captures]
    output = HERE / "input-epoch"
    output.mkdir(exist_ok=False)
    for path, data in input_bodies:
        target = output / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    target = output / "INPUT_EPOCH.json"
    target.write_text(json.dumps(updated, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"input_epoch": str(target), "sha256": sha(target), "shape": updated["shape"]}))


if __name__ == "__main__":
    main()
