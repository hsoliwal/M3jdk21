#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Stage the mapping test only from a verified actual20-owner binder result.

This renderer has no publication client and does not write the four operational images.
The original source packet remains the source-body/readback custody record. The test
resource is an explicitly named, separately hashed, body-free projection of that input.
"""
from __future__ import annotations

import argparse
import copy
import importlib.util
import json
from pathlib import Path
import re

HERE = Path(__file__).resolve().parent
TEST_PATH = "m3/tooling/migration-recipes/src/test/java/com/m3/rewrite/backport/JccMergeMappingRecoveryV03Test.java"
POM_PATH = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006/mapping-verification-v03/pom.xml"
YAML_PATH = "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-jcc-merge-mapping-recovery-v03.yml"
SUMMARY_PATH = ("m3/tooling/migration-recipes/src/test/resources/com/m3/rewrite/backport/"
                "jcc-merge-recovery-20261006-mapping-v03/publication-input-summary.json")
BEFORE_PINS = (
    "335045274ea3ae01d576290b56704ec3409ef1bd27c8c220348815c5bbb711da",
    "fff2e02f72c07f89c6f8bcabab3839878ff8628f0b31b04bb3632eb62be10337",
    "cdb5d7f4a0746fe561f51fa9a13cbd9fa319f8196bfd3075ca2288be7f3d1126",
    "cba3b3315df643f328c4acb2fd08b5194262dc7ad7b40557107a8c614355f55b",
)
PRIOR_MAP_PIN = "eb42442866c73c38027b481a84c0473bea422e4628d0794fa31fc61d9f1a818e"
PRIOR_BINDINGS_PIN = "f8b27e3582eb494af2c8dd47225976f974be6edc2f6c26313975064d48739497"
INPUT_EPOCH_PIN = "2a4878ce5eeeeeccfe216dae36e62faa3e5da06c7baa654e7a43ce3d823c5139"


def load_binder():
    specification = importlib.util.spec_from_file_location("mapping_template_binder", HERE.parent / "bind_mapping.py")
    if specification is None or specification.loader is None:
        raise ValueError("mapping binder is unavailable")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


def require_no_bodies(value):
    if isinstance(value, dict):
        if "content_base64" in value:
            raise ValueError("test summary must not contain source bodies")
        for child in value.values():
            require_no_bodies(child)
    elif isinstance(value, list):
        for child in value:
            require_no_bodies(child)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--audit-root", required=True, type=Path)
    parser.add_argument("--source-input", required=True, type=Path)
    parser.add_argument("--mapping-resources", required=True, type=Path,
                        help="Output directory from bind_mapping.py in actual publication mode")
    parser.add_argument("--output", required=True, type=Path,
                        help="New staging directory for four test/configuration artifacts only")
    args = parser.parse_args()
    binder = load_binder()
    audit = args.audit_root.resolve(strict=True)
    bundle = args.mapping_resources.resolve(strict=True)
    output = args.output.resolve()
    binder.require(not output.exists(), "test staging output already exists")
    epoch_root = audit / "candidate/mapping-v03/input-epoch"
    epoch_bytes = (epoch_root / "INPUT_EPOCH.json").read_bytes()
    binder.require(binder.INPUT_EPOCH_SHA256 == INPUT_EPOCH_PIN
                   and binder.digest(epoch_bytes) == INPUT_EPOCH_PIN, "current input epoch pin drift")
    epoch = json.loads(epoch_bytes)
    binder.require(epoch["schema"] == "m3-jcc-current-mapping-input-epoch/3"
                   and epoch["status"] == "EXACT_CURRENT_INPUTS_FROZEN"
                   and epoch["commit"] == binder.DESTINATION and epoch["root_tree"] == binder.DESTINATION_ROOT,
                   "current input epoch identity drift")
    binder.require(epoch["shape"] == {"ordered_records": 52, "non_jcc_records": 50, "top_level_mappings": 25, "gates": 20},
                   "current input epoch shape drift")
    current = {path: (epoch_root / "current" / path).read_bytes() for path in binder.PATHS}
    prior = {path: (audit / "files/prior" / path).read_bytes() for path in binder.PATHS}
    for index, path in enumerate(binder.PATHS):
        binder.require(binder.digest(current[path]) == BEFORE_PINS[index], "current input pin drift: " + path)
    binder.require(binder.digest(prior[binder.PATHS[0]]) == PRIOR_MAP_PIN, "historical map pin drift")
    binder.require(binder.digest(prior[binder.PATHS[2]]) == PRIOR_BINDINGS_PIN, "historical binding pin drift")
    for guard in epoch["guards"]:
        binder.require(binder.digest((epoch_root / guard["file"]).read_bytes()) == guard["sha256"], "current canonical guard drift")
    owner = (epoch_root / "guards" / binder.TEXT_OWNER).read_bytes()
    binder.require(binder.digest(owner) == binder.TEXT_OWNER_HASH, "retained text owner drift")
    packet = json.loads(args.source_input.read_bytes())
    # No fixture switch exists here. The binder rechecks all twenty source bodies,
    # the complete direct root/POM, readback identity, owner set, and blocked gates.
    artifacts, pom = binder.validate_source(packet, json.loads(prior[binder.PATHS[0]]))
    binder.require(packet["fixture"] is False, "an actual publication input is required")
    source_input = binder.pretty(packet)
    binder.require((bundle / "source-publication-input.json").read_bytes() == source_input,
                   "binder output source input differs from supplied actual packet")
    binder.require((bundle / "mapping-input-epoch.json").read_bytes() == epoch_bytes,
                   "binder output receiving input epoch differs from frozen current epoch")
    receipt = json.loads((bundle / "MAPPING_RESOURCES.json").read_bytes())
    binder.require(receipt["schema"] == "m3-jcc-current-mapping-resources/1"
                   and receipt["fixture"] is False
                   and receipt["status"] == "ACTUAL_SOURCE_BOUND_TEMPLATES_NOT_EXECUTED",
                   "mapping resources must come from actual publication mode")
    binder.require(receipt["source_commit"] == packet["commit"]
                   and receipt["source_root_tree"] == packet["root_tree"]
                   and receipt["destination_base"] == binder.DESTINATION
                   and receipt["destination_root_tree"] == binder.DESTINATION_ROOT
                   and receipt["input_epoch_sha256"] == INPUT_EPOCH_PIN
                   and receipt["crate"] == binder.CRATE
                   and receipt["source_input_sha256"] == binder.digest(source_input),
                   "mapping receipt identity drift")
    expected_images = binder.compose(current, prior, packet)
    base = bundle / "resources"
    expected_names = {"manifest.tsv", "plan.json"}
    expected_rows, expected_plan_rows = [], []
    for index, path in enumerate(binder.PATHS):
        before_name, after_name = f"before-{index:02d}.txt", f"after-{index:02d}.txt"
        expected_names.update((before_name, after_name))
        before, after = current[path], expected_images[path]
        binder.require((base / before_name).read_bytes() == before, "before resource drift: " + path)
        binder.require((base / after_name).read_bytes() == after, "after resource differs from actual input composition: " + path)
        expected_rows.append({"path": path, "before_sha256": binder.digest(before), "after_sha256": binder.digest(after),
                              "before_git_blob": binder.git_object("blob", before), "after_git_blob": binder.git_object("blob", after),
                              "before_bytes": len(before), "after_bytes": len(after),
                              "before_resource": before_name, "after_resource": after_name})
        expected_plan_rows.append({"path": path, "before": {"resource": before_name, "sha256": binder.digest(before)},
                                   "after": {"resource": after_name, "sha256": binder.digest(after)}})
    binder.require({path.name for path in base.iterdir()} == expected_names, "unexpected mapping resource set")
    binder.require(receipt["rows"] == expected_rows, "mapping row receipt drift")
    expected_manifest = "".join("\t".join([row["path"], row["before_sha256"], row["after_sha256"], row["after_resource"]]) + "\n"
                                for row in sorted(expected_rows, key=lambda item: item["path"])).encode()
    binder.require((base / "manifest.tsv").read_bytes() == expected_manifest, "manifest order/content drift")
    binder.require(receipt["manifest_sha256"] == binder.digest(expected_manifest), "manifest receipt drift")
    history = json.loads((audit / "candidate/HISTORY_RESOURCES.json").read_bytes())
    guard_plan = json.loads((audit / "candidate/overlay" / history["crates"][0]["plan_path"]).read_bytes())
    binder.require({row["path"]: row["sha256"] for row in guard_plan["guards"]}
                   == {row["path"]: row["sha256"] for row in epoch["guards"]}, "current epoch/plan guards differ")
    expected_plan = {"schema": "m3.sealed-install/1",
                     "recipe_id": "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/" + binder.CRATE,
                     "version": 1, "source_commit": packet["commit"], "destination_base": binder.DESTINATION,
                     "scope": "Exact current 20-owner mapping metadata; source and destination acceptance remain blocked",
                     "outputs": sorted(expected_plan_rows, key=lambda row: row["path"]), "guards": guard_plan["guards"]}
    expected_plan["plan_sha256"] = binder.digest(binder.canonical(expected_plan))
    plan_bytes = (base / "plan.json").read_bytes()
    binder.require(json.loads(plan_bytes) == expected_plan, "sealed plan differs from exact source-bound plan")
    binder.require(receipt["plan_sha256"] == expected_plan["plan_sha256"], "plan receipt drift")

    projected = ["fixture", "repository", "commit", "root_tree", "ref", "scope", "publication_state",
                 "publication_receipt_sha256", "commit_readback_sha256", "root_readback_sha256", "ref_readback_sha256",
                 "commit_readback", "root_readback", "ref_readback", "execution_reports",
                 "source_export_admitted", "destination_gates_passed"]
    summary = {"schema": "m3-jcc-source-publication-verification-summary/1",
               "source_input_schema": packet["schema"], "source_input_sha256": binder.digest(source_input),
               "receiving_input_epoch_sha256": INPUT_EPOCH_PIN, "receiving_commit": binder.DESTINATION,
               "receiving_root_tree": binder.DESTINATION_ROOT,
               **{key: copy.deepcopy(packet[key]) for key in projected}}
    summary["execution_summary"] = copy.deepcopy(packet.get("execution_summary", {}))
    summary["root_pom"] = {key: packet["root_pom"][key] for key in ["bytes", "sha256", "git_blob_sha1"]}
    summary["root_pom"]["module_declarations"] = binder.module_declarations(pom)
    artifact_keys = ["path", "repository", "commit", "module", "symbol", "capability_id", "bytes",
                     "sha256", "git_blob_sha1", "readback_verified", "readback_sha256"]
    summary["artifacts"] = [{key: row[key] for key in artifact_keys} for row in artifacts.values()]
    summary["source_components"] = binder.validate_build_components(packet)
    require_no_bodies(summary)
    summary_bytes = binder.pretty(summary)
    pins = {"ACTUAL_SOURCE_COMMIT": packet["commit"], "ACTUAL_SOURCE_ROOT_TREE": packet["root_tree"],
            "ACTUAL_SOURCE_INPUT_SHA256": binder.digest(source_input),
            "ACTUAL_PUBLICATION_SUMMARY_SHA256": binder.digest(summary_bytes),
            "ACTUAL_MAPPING_MANIFEST_SHA256": receipt["manifest_sha256"],
            "ACTUAL_MAPPING_PLAN_SHA256": expected_plan["plan_sha256"],
            "ACTUAL_MAPPING_PLAN_FILE_SHA256": binder.digest(plan_bytes)}
    pins.update({f"ACTUAL_AFTER_{index:02d}_SHA256": row["after_sha256"] for index, row in enumerate(expected_rows)})
    template = (HERE / "JccMergeMappingRecoveryV03Test.java.in").read_text()
    for key, value in pins.items():
        marker = "__UNRESOLVED_" + key + "__"
        binder.require(template.count(marker) == 1, "missing/duplicate template pin: " + key)
        binder.require(re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", value), "invalid template pin value")
        template = template.replace(marker, value)
    binder.require("__UNRESOLVED_" not in template, "unresolved template pin remains")
    staged = {TEST_PATH: template.encode(), POM_PATH: (HERE / "pom.xml").read_bytes(),
              YAML_PATH: (HERE / "m3-jcc-merge-mapping-recovery-v03.yml").read_bytes(), SUMMARY_PATH: summary_bytes}
    binder.require(not set(staged).intersection(binder.PATHS), "renderer must not write operational mapping images")
    result = {"schema": "m3-jcc-mapping-test-render/1", "status": "ACTUAL_SOURCE_BOUND_TEST_INPUTS_NOT_EXECUTED",
              "fixture": False, "source_commit": packet["commit"], "source_root_tree": packet["root_tree"],
              "source_input_sha256": binder.digest(source_input), "summary_sha256": binder.digest(summary_bytes),
              "receiving_input_epoch_sha256": INPUT_EPOCH_PIN, "receiving_commit": binder.DESTINATION,
              "receiving_root_tree": binder.DESTINATION_ROOT,
              "manifest_sha256": receipt["manifest_sha256"], "plan_sha256": expected_plan["plan_sha256"],
              "files": [{"path": path, "bytes": len(body), "sha256": binder.digest(body),
                         "git_blob_sha1": binder.git_object("blob", body)} for path, body in sorted(staged.items())],
              "template_inputs": {name: binder.digest((HERE / name).read_bytes()) for name in
                                  ["JccMergeMappingRecoveryV03Test.java.in", "pom.xml", "m3-jcc-merge-mapping-recovery-v03.yml", "render_template.py"]},
              "binder_sha256": binder.digest((HERE.parent / "bind_mapping.py").read_bytes()),
              "source_export_admitted": False, "destination_gates_passed": False,
              "scope": "Only test/configuration inputs staged. No JUnit execution, recipe output, or publication is asserted."}
    # Every validation above completes before the first staging write.
    output.mkdir(parents=True, exist_ok=False)
    for path, body in staged.items():
        destination = output / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(body)
    (output / "RENDERED_TEMPLATE_RECEIPT.json").write_bytes(binder.pretty(result))
    print(json.dumps({key: result[key] for key in ["status", "source_commit", "source_root_tree", "summary_sha256"]}))


if __name__ == "__main__":
    main()
