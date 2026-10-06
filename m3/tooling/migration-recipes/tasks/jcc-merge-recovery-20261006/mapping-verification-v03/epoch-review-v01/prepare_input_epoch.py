#!/usr/bin/env python3
"""Freeze the actual C0 receiving epoch; preserve V01/V02 inputs and failures."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
AUDIT = HERE.parents[1]
ROOT = AUDIT.parent
COMMIT = "c0a14387009aefc7d62bd3268055d526e04f9074"
TREE = "491bbd80a51483d0710fc12c1bad3a037b385ba0"
MAP_SHA = "335045274ea3ae01d576290b56704ec3409ef1bd27c8c220348815c5bbb711da"
MAP_GIT = "34253f1b73b9ba3531a86f778f00c5e95cd2fbf1"
MAP = "m3/docs/name-mapping.json"


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    verifier_path = AUDIT / "candidate/mapping/verify_supplemental_inputs.py"
    spec = importlib.util.spec_from_file_location("existing_epoch_verifier", verifier_path)
    verifier = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(verifier)
    prior_root = AUDIT / "candidate/mapping-v02/input-epoch"
    prior_file = prior_root / "INPUT_EPOCH.json"
    assert sha(prior_file) == "fb413e58be72aaffa1e91149479ac159c6d4b961101eed4ab10f1c99df0b6711"
    prior = json.loads(prior_file.read_bytes())
    capture_names = ["COMMIT", "ROOT", "M3", "GITHUB"]
    captures = [ROOT / ("publication/M3_FINAL_CURRENT_" + name + ".json") for name in capture_names]
    commit, root, m3, github = [verifier.captured_json(path) for path in captures]
    assert commit["sha"] == COMMIT and commit["tree"]["sha"] == TREE
    assert root["sha"] == TREE and root["truncated"] is False
    assert verifier.direct_tree_id(root["tree"]) == TREE
    root_entries = {row["path"]: row for row in root["tree"]}
    assert root_entries["m3"]["sha"] == m3["sha"]
    assert root_entries[".github"]["sha"] == github["sha"]
    reconstructed = {"m3": verifier.verify_recursive_tree(m3), ".github": verifier.verify_recursive_tree(github)}
    entries = {"m3/" + row["path"]: row for row in m3["tree"]}
    map_body_path = ROOT / "publication/M3_FINAL_CURRENT_NAME_MAPPING.body.json"
    map_body = map_body_path.read_bytes()
    assert hashlib.sha256(map_body).hexdigest() == MAP_SHA and verifier.git_object("blob", map_body) == MAP_GIT
    assert len(map_body) == 280877
    map_capture = ROOT / "publication/M3_FINAL_CURRENT_NAME_MAPPING.json"
    envelope = json.loads(map_capture.read_bytes())
    assert not envelope["response"].get("isError")
    assert envelope["response"]["structuredContent"]["content"].encode() == map_body
    old_map = json.loads((prior_root / "current" / MAP).read_bytes())
    current_map = json.loads(map_body)
    assert len(current_map["migration"]["records"]) == 52
    assert current_map["mappings"] == old_map["mappings"] and len(current_map["mappings"]) == 25
    assert current_map["migration"]["gates"] == old_map["migration"]["gates"]
    assert len(current_map["migration"]["gates"]) == 20
    old_records, new_records = [m["migration"]["records"] for m in (old_map, current_map)]
    assert [row["id"] for row in old_records] == [row["id"] for row in new_records]
    changed = [i for i, pair in enumerate(zip(old_records, new_records)) if pair[0] != pair[1]]
    assert changed == [46] and new_records[46]["id"] == "synexia.counterpart.MIndexRegexTrigramQuery"
    for key in current_map:
        if key != "migration":
            assert current_map[key] == old_map[key]
    for key in current_map["migration"]:
        if key != "records":
            assert current_map["migration"][key] == old_map["migration"][key]
    delta = ROOT / "publication/M3_FINAL_CURRENT_MAP_DELTA.json"
    assert sha(delta) == "6075d1ef1daf7afd2f386bd693d8d34d618dd9d6d233fb29e200cf2da40e22e5"
    assert len(json.loads(delta.read_bytes())["changes"]) == 17
    body_inputs = []
    updated = copy.deepcopy(prior)
    for key in ["current_operational_preimages", "guards"]:
        for row in updated[key]:
            data = map_body if row["path"] == MAP else (prior_root / row["file"]).read_bytes()
            identity = verifier.git_object("blob", data)
            if row["path"].startswith("src/"):
                capture = ROOT / "publication/M3_FINAL_CURRENT_DESCRIPTOR_GUARD.json"
                descriptor = json.loads(capture.read_bytes())
                assert COMMIT in json.dumps(descriptor["request"]) and row["path"] in json.dumps(descriptor["request"])
                assert not descriptor["response"].get("isError")
                assert descriptor["response"]["structuredContent"]["content"].encode() == data
                assert identity == row["git_blob"]
                captures.append(capture)
            else:
                entry = entries[row["path"]]
                assert entry["sha"] == identity and entry["mode"] == "100644" and entry["type"] == "blob"
                assert entry["size"] == len(data)
            row.update({"commit": COMMIT, "git_blob": identity, "sha256": hashlib.sha256(data).hexdigest(),
                        "bytes": len(data), "membership": "Actual C0 readback and reconstructed tree; exact-commit contents guard for descriptor"})
            body_inputs.append((row["file"], data))
    updated.update({"schema": "m3-jcc-current-mapping-input-epoch/3", "commit": COMMIT, "root_tree": TREE,
                    "m3_tree": m3["sha"], "github_tree": github["sha"], "reconstructed_tree_count": reconstructed,
                    "previous_input_epoch": {"path": str(prior_file), "sha256": sha(prior_file)},
                    "current_record_update": {"index": 46, "before": old_records[46], "after": new_records[46],
                                              "delta": str(delta), "delta_sha256": sha(delta)},
                    "other_51_record_nodes_unchanged": True,
                    "scope": "Versioned exact C0 mapping/guard inputs. V01/V02 and earlier validator failures retained. Actual source publication pending."})
    captures += [map_body_path, map_capture, delta, ROOT / "publication/M3_FINAL_CURRENT_ADMISSION.json", verifier_path]
    updated["captured_evidence"] = [{"path": str(path), "sha256": sha(path)} for path in captures]
    output = HERE / "input-epoch"
    output.mkdir(exist_ok=False)
    for relative, body in body_inputs:
        path = output / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(body)
    target = output / "INPUT_EPOCH.json"
    target.write_text(json.dumps(updated, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"input_epoch": str(target), "sha256": sha(target), "preimages": 4, "guards": 7, "shape": updated["shape"]}))


if __name__ == "__main__":
    main()
