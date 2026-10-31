#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Author four reviewed mapping images from an explicit, verified source-publication packet.

This is task authoring input validation, not a publication client or runtime acceptance owner.
Product materialization remains with M3Jdk21HashPinnedTextSnapshotRecipe and recipe.py.
"""
from __future__ import annotations

import argparse
import base64
import copy
import csv
import hashlib
import io
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

SOURCE_REPO = "hsoliwal/com.synexia"
DESTINATION = "c0a14387009aefc7d62bd3268055d526e04f9074"
DESTINATION_ROOT = "491bbd80a51483d0710fc12c1bad3a037b385ba0"
SOURCE_HISTORY = "becb01f90bda8302b25592c3705fc35c7a250676"
RECEIVER_HISTORY = "946fd922c1d878e3f32309c83d132057a7ef8451"
CRATE = "jcc-merge-recovery-20261006-mapping-v03"
LAB = "synexia.jcc-recipe-laboratory"
JNI = "synexia.jcc-java-jni-regression"
IDS = (LAB, JNI)
IOP = "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3IopPatternMechanicalPasses.java"
PATHS = (
    "m3/docs/name-mapping.json",
    "m3/docs/jcc-source-handoff.md",
    "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
    "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv",
)
RESOURCE_ROOT = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text"
TASK_ROOT = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006"
TEXT_OWNER = "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java"
TEXT_OWNER_HASH = "8015207ce502d22955f7f77697177e5939fa1e25be7cd74be89c639335ae48e6"
OID = re.compile(r"[0-9a-f]{40}\Z")
SHA = re.compile(r"[0-9a-f]{64}\Z")
INPUT_EPOCH_SHA256 = "027e47d20c7fb854e028487ceef341c929877a6421dbb75f6433b5cd007fe5ee"
CURRENT_INPUT_HASHES = {
    PATHS[0]: "335045274ea3ae01d576290b56704ec3409ef1bd27c8c220348815c5bbb711da",
    PATHS[1]: "fff2e02f72c07f89c6f8bcabab3839878ff8628f0b31b04bb3632eb62be10337",
    PATHS[2]: "cdb5d7f4a0746fe561f51fa9a13cbd9fa319f8196bfd3075ca2288be7f3d1126",
    PATHS[3]: "cba3b3315df643f328c4acb2fd08b5194262dc7ad7b40557107a8c614355f55b",
}


class Refusal(ValueError):
    pass


def require(condition, message):
    if not condition:
        raise Refusal(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def git_object(kind, data):
    return hashlib.sha1(kind.encode() + b" " + str(len(data)).encode() + b"\0" + data).hexdigest()


def canonical(value):
    return (json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True) + "\n").encode()


def pretty(value):
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode()


def unique(values):
    result, seen = [], set()
    for value in values:
        key = canonical(value)
        if key not in seen:
            result.append(copy.deepcopy(value))
            seen.add(key)
    return result


def safe_path(value):
    require(isinstance(value, str) and bool(value) and "\\" not in value and not value.startswith("/"), "unsafe repository path")
    require(all(part and part not in (".", "..", ".git") and ":" not in part for part in value.split("/")), "unsafe repository path")
    require(not any(ord(char) < 32 or ord(char) == 127 for char in value), "control character in repository path")
    return value


def tree_hash(entries):
    seen = set()
    for row in entries:
        require(row["path"] not in seen and "/" not in safe_path(row["path"]), "duplicate/non-direct root entry")
        seen.add(row["path"])
        require((row["type"], row["mode"]) in {("tree", "040000"), ("blob", "100644"),
                ("blob", "100755"), ("blob", "120000"), ("commit", "160000")}, "unsupported Git root entry")
        require(OID.fullmatch(row["sha"]) is not None, "invalid root object ID")
    order = sorted(entries, key=lambda row: row["path"].encode() + (b"/" if row["type"] == "tree" else b""))
    body = b"".join(("40000" if row["mode"] == "040000" else row["mode"]).encode() + b" "
                    + row["path"].encode() + b"\0" + bytes.fromhex(row["sha"]) for row in order)
    return git_object("tree", body)


def decode_body(item):
    try:
        body = base64.b64decode(item["content_base64"], validate=True)
    except (KeyError, TypeError, ValueError) as error:
        raise Refusal("missing/invalid source body") from error
    require(len(body) == item["bytes"], "source body size drift")
    require(digest(body) == item["sha256"] and git_object("blob", body) == item["git_blob_sha1"], "source body identity drift")
    return body


def validate_source(packet, prior_map, *, allow_fixture=False):
    require(packet.get("schema") == "m3-jcc-source-publication-input/1", "unsupported source input schema")
    require(type(packet.get("fixture")) is bool, "fixture marker required")
    fixture = packet["fixture"]
    require(not fixture or allow_fixture, "fixture inputs are unpublishable")
    require(packet.get("repository") == SOURCE_REPO, "unexpected donor repository")
    for field in ("commit", "root_tree"):
        require(isinstance(packet.get(field), str) and OID.fullmatch(packet[field]), "invalid source " + field)
        require(packet[field] != "0" * 40, "unbound source " + field)
    require(packet["commit"] != SOURCE_HISTORY, "history-only source publication cannot bind current repairs")
    require(packet.get("scope") == "current-five-owner-repair-publication", "current repair publication scope required")
    require(packet.get("publication_state") == ("SYNTHETIC_FIXTURE" if fixture else "ACTUAL_GITHUB_READBACK_VERIFIED"), "source publication not verified")
    require(isinstance(packet.get("ref"), str) and packet["ref"].startswith("refs/heads/"), "explicit source ref required")
    for key in ("publication_receipt_sha256", "commit_readback_sha256", "root_readback_sha256", "ref_readback_sha256"):
        require(isinstance(packet.get(key), str) and SHA.fullmatch(packet[key]), "missing source readback receipt: " + key)
    require(packet.get("commit_readback", {}).get("sha") == packet["commit"], "source commit readback mismatch")
    require(packet["commit_readback"].get("tree", {}).get("sha") == packet["root_tree"], "source commit/tree readback mismatch")
    require(packet.get("ref_readback", {}).get("ref") == packet["ref"], "source ref name mismatch")
    require(packet["ref_readback"].get("object", {}).get("sha") == packet["commit"], "source ref object mismatch")
    root = packet.get("root_readback", {})
    require(root.get("sha") == packet["root_tree"] and root.get("truncated") is False, "incomplete source root readback")
    entries = root.get("tree")
    require(isinstance(entries, list) and len(entries) > 0, "empty source root")
    require(tree_hash(entries) == packet["root_tree"], "source root tree hash mismatch")
    root_pom = decode_body(packet["root_pom"])
    pom_entry = next((row for row in entries if row["path"] == "pom.xml"), None)
    require(pom_entry is not None and pom_entry["type"] == "blob" and pom_entry["sha"] == packet["root_pom"]["git_blob_sha1"], "root POM not bound to source root")
    prior = {row["id"]: row for row in prior_map["migration"]["records"] if row["id"] in IDS}
    expected = {source["path"]: (record_id, source) for record_id, record in prior.items() for source in record["sources"]}
    require(len(expected) == 19, "expected prior19-owner source set")
    expected[IOP] = (LAB, {"path": IOP, "symbol": "com.synexia.rewrite.M3IopPatternMechanicalPasses", "module": "synexia-openrewrite-recipes"})
    artifacts = packet.get("artifacts")
    require(isinstance(artifacts, list) and len(artifacts) == 20, "exact current 20-owner source set required")
    actual = {}
    for row in artifacts:
        path = safe_path(row["path"])
        require(path in expected and path not in actual, "unexpected/duplicate source owner")
        capability, old = expected[path]
        require(row.get("capability_id") == capability and row.get("module") == old["module"]
                and row.get("symbol") == old["symbol"], "source owner/capability mismatch")
        require(row.get("commit") == packet["commit"] and row.get("repository") == SOURCE_REPO, "mixed source revisions")
        decode_body(row)
        require(row.get("readback_verified") is True, "source body readback required")
        require(isinstance(row.get("readback_sha256"), str) and SHA.fullmatch(row["readback_sha256"]), "source readback receipt missing")
        actual[path] = row
    require(set(actual) == set(expected), "missing source owner")
    reports = packet.get("execution_reports")
    require(isinstance(reports, list) and reports, "current execution reports required")
    for report in reports:
        safe_path(report["path"])
        require(SHA.fullmatch(report["sha256"]) and isinstance(report.get("scope"), str) and bool(report["scope"]), "invalid execution report identity")
    require(packet.get("source_export_admitted") is False and packet.get("destination_gates_passed") is False,
            "this metadata recovery cannot promote export or destination gates")
    validate_build_components(packet)
    return actual, root_pom


def validate_build_components(packet):
    components = packet.get("source_components")
    require(isinstance(components, list) and len(components) == 1, "exact module-POM source component required")
    row = components[0]
    require(row.get("kind") == "module-pom-compile-edge"
            and row.get("path") == "synexia-openrewrite-recipes/pom.xml"
            and row.get("module") == "synexia-openrewrite-recipes", "source build component owner mismatch")
    require(row.get("repository") == SOURCE_REPO and row.get("commit") == packet["commit"], "mixed source build revision")
    decode_body(row)
    require(row.get("sha256") == "7846d9447016ab714a4d1c82d7f3814efa7a94049cb99bb59ba09f6f6d8c7a80"
            and row.get("git_blob_sha1") == "1a1c0cc8017b9ce9eadd6eba7e2ead3400a2266f"
            and row.get("bytes") == 203000, "source module-POM qualified output drift")
    require(row.get("readback_verified") is True and isinstance(row.get("readback_sha256"), str)
            and SHA.fullmatch(row["readback_sha256"]), "source build component readback missing")
    return [{key: row[key] for key in ["kind", "repository", "commit", "path", "module", "bytes",
             "sha256", "git_blob_sha1", "readback_verified", "readback_sha256"]}]


def module_declarations(pom):
    try:
        root = ET.fromstring(pom)
    except ET.ParseError as error:
        raise Refusal("invalid root POM") from error
    result = []
    def visit(node, context):
        tag = node.tag.rsplit("}", 1)[-1]
        if tag == "profile":
            identifier = next((child.text or "" for child in node if child.tag.rsplit("}", 1)[-1] == "id"), "")
            context = "profile:" + identifier.strip()
        if tag == "modules":
            for child in node:
                if child.tag.rsplit("}", 1)[-1] == "module":
                    result.append({"path": safe_path((child.text or "").strip()), "context": context})
        for child in node:
            visit(child, context)
    visit(root, "project")
    return result


def root_coverage(packet, pom, artifacts):
    declarations = module_declarations(pom)
    columns = ["source_commit", "source_tree", "root_path", "object_type", "mode", "object_id",
               "root_pom_declarations", "declaration_contexts", "source_known", "source_tested",
               "mapping_reviewed", "coverage_obligation", "partial_capability_records", "export_admitted",
               "destination_materialized", "destination_gates", "read_back_delivered"]
    output = io.StringIO()
    writer = csv.writer(output, delimiter="\t", lineterminator="\n")
    writer.writerow(columns)
    for entry in sorted(packet["root_readback"]["tree"], key=lambda row: row["path"]):
        root = entry["path"]
        nominated = [row for row in declarations if row["path"].split("/", 1)[0] == root]
        related = sorted({row["capability_id"] for row in artifacts.values() if row["path"].split("/", 1)[0] == root})
        writer.writerow([packet["commit"], packet["root_tree"], root, entry["type"], entry["mode"], entry["sha"],
                         ";".join(row["path"] for row in nominated), ";".join(row["context"] for row in nominated),
                         "ROOT_OBJECT_BOUND", "NOT_ESTABLISHED_FOR_ROOT_UNIT", "UNREVIEWED_ROOT_UNIT",
                         "family.entire-source-closure", ";".join(related), "false", "false", "NOT_RUN_FOR_ROOT_UNIT", "false"])
    modules = {row["path"] for row in declarations}
    direct = {path for path in modules if "/" not in path}
    nested = {path for path in modules if "/" in path}
    nested_roots = {path.split("/", 1)[0] for path in nested}
    entries = packet["root_readback"]["tree"]
    receipt = {"root_entries": len(entries), "root_trees": sum(row["type"] == "tree" for row in entries),
               "root_blobs": sum(row["type"] == "blob" for row in entries), "root_gitlinks": sum(row["type"] == "commit" for row in entries),
               "root_pom_module_declarations": len(declarations), "distinct_module_paths": len(modules),
               "direct_root_modules": len(direct), "nested_module_paths": len(nested),
               "root_trees_with_nested_modules": len(nested_roots), "nested_containers_also_direct_modules": len(nested_roots & direct),
               "distinct_root_trees_referenced": len({path.split("/", 1)[0] for path in modules}),
               "root_pom_sha256": packet["root_pom"]["sha256"], "root_pom_git_blob": packet["root_pom"]["git_blob_sha1"],
               "whole_repository_file_count": None, "semantic_dependency_closure_complete": False,
               "coverage_receipt_path": PATHS[3], "coverage_receipt_sha256": digest(output.getvalue().encode())}
    return output.getvalue().encode(), receipt


def compose(current_files, prior_files, packet, *, allow_fixture=False):
    require(set(current_files) == set(PATHS), "exact four current mapping inputs required")
    for path in PATHS:
        require(digest(current_files[path]) == CURRENT_INPUT_HASHES[path], "frozen current mapping input drift: " + path)
    current_map = json.loads(current_files[PATHS[0]])
    prior_map = json.loads(prior_files[PATHS[0]])
    artifacts, pom = validate_source(packet, prior_map, allow_fixture=allow_fixture)
    original_records = current_map["migration"]["records"]
    require(len(original_records) == 52 and len({r["id"] for r in original_records}) == 52, "exact 52-record current map required")
    require(len(current_map["mappings"]) == 25 and len(current_map["migration"]["gates"]) == 20, "current mapping/gate shape drift")
    require(all(any(row["id"] == identity for row in original_records) for identity in IDS), "JCC mapping missing")
    prior_records = {row["id"]: row for row in prior_map["migration"]["records"] if row["id"] in IDS}
    result = copy.deepcopy(current_map)
    current_sources = []
    for record in result["migration"]["records"]:
        if record["id"] not in IDS:
            continue
        old = next(row for row in original_records if row["id"] == record["id"])
        earlier = prior_records[record["id"]]
        source_rows = []
        # Historical source ordering is preserved; the newly relevant IOP plan owner is appended.
        paths = [row["path"] for row in earlier["sources"]]
        if record["id"] == LAB:
            paths.append(IOP)
        for path in paths:
            row = artifacts[path]
            previous = next((item for item in earlier["sources"] if item["path"] == path), None)
            source = {"repo": SOURCE_REPO, "commit": packet["commit"], "module": row["module"],
                      "path": path, "symbol": row["symbol"], "signatures": previous["signatures"] if previous else [],
                      "sha256": row["sha256"], "git_blob_sha1": row["git_blob_sha1"], "fingerprint": None,
                      "revision_role": "pinned", "tracking_ref": packet["ref"]}
            source_rows.append(source)
        record["sources"] = source_rows
        current_sources.extend(source_rows)
        record["lineage"]["previous_sources"] = unique(old["lineage"]["previous_sources"] + old["sources"]
                                                       + earlier["lineage"]["previous_sources"] + earlier["sources"])
        record["lineage"]["previous_targets"] = unique(old["lineage"]["previous_targets"] + old["targets"]
                                                       + earlier["lineage"]["previous_targets"] + earlier["targets"])
        record["lineage"]["supersedes"] = unique(old["lineage"]["supersedes"] + earlier["lineage"]["supersedes"])
        record["status"] = "blocked"
        record["source_state"] = "partial"
        record["tests"] = []
        record["reason"] = ("Current 20-owner source publication is bound after exact readback; historical source and receiving work is retained. "
                             "Source export, whole owning-module coverage and applicable M3JDK21 runtime/JNI/platform gates remain unadmitted.")
        record["recipe"] = {"id": "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/" + CRATE,
                            "version": "0.1.0-SNAPSHOT", "path": TEXT_OWNER, "sha256": TEXT_OWNER_HASH,
                            "preconditions": ["Four exact reviewed current mapping preimages at " + DESTINATION + ".",
                                "Preserve all 52 ordered IDs, 50 non-JCC records, all 25 current top-level mapping objects, precompute ownership and 20 existing gates.",
                                "Require actual verified current source publication; history-only and fixture publication identities refuse."],
                            "rollback": "Existing recipe.py sealed rollback restores exact receipt-owned preimages and refuses foreign edits."}
        record["sync"]["source_revision"] = packet["commit"]
        record["sync"]["target_revision"] = DESTINATION
        record["sync"]["pending"] = [
            "Complete the existing source-canon checkpoint, same-root verification gates, offline dependency/source closure and whole-repository review before source export.",
            "Carry only source-qualified applicable behavior into existing M3 owners and execute unchanged owning-module coverage plus applicable image/runtime/JNI/platform gates.",
            "Verify the actual resulting merge tree and its required CI, retaining every historical custody obligation; commit ancestry alone does not deliver files."]
        record["observation"] = ("Current source " + packet["commit"] + " / root " + packet["root_tree"]
            + " binds 14 laboratory and 6 Java/JNI owners. Historical 0b/d1/0c53 and donor 4990 results retain their original epochs. "
            + "The 443 restored receiving files and retention checks establish artifact custody only. Current source execution receipts are separately linked in "
            + PATHS[2] + "; no rebuilt-JDK acceptance or Synexia runtime dependency is introduced.")
    require([row["id"] for row in result["migration"]["records"]] == [row["id"] for row in original_records], "record order changed")
    require([row for row in result["migration"]["records"] if row["id"] not in IDS]
            == [row for row in original_records if row["id"] not in IDS], "non-JCC records changed")
    for key in current_map:
        if key != "migration":
            require(result[key] == current_map[key], "current global field changed: " + key)
    for key in current_map["migration"]:
        if key != "records":
            require(result["migration"][key] == current_map["migration"][key], "current migration field changed: " + key)
    coverage, root_accounting = root_coverage(packet, pom, artifacts)
    bindings = json.loads(current_files[PATHS[2]])
    historical_binding = json.loads(prior_files[PATHS[2]])
    bindings["source_commit"] = packet["commit"]
    bindings["source_root_tree"] = packet["root_tree"]
    bindings["destination_commit"] = DESTINATION
    bindings["destination_root_tree"] = DESTINATION_ROOT
    bindings["canonical_map_preimage_sha256"] = digest(current_files[PATHS[0]])
    bindings["source_acquisition_receipt_sha256"] = digest(pretty(packet))
    bindings["source_acquisition_scope"] = "Exactly 20 current designated source bodies and the complete direct source root/POM are bound. This is not whole-repository semantic or export admission."
    bindings["source_root_receipt_sha256"] = packet["root_readback_sha256"]
    bindings["source_root_accounting"] = root_accounting
    bindings["source_artifacts"] = current_sources
    bindings["source_build_components"] = validate_build_components(packet)
    bindings["acceptance"] = {key: False for key in bindings["acceptance"]}
    bindings["source_publication"] = {key: copy.deepcopy(packet[key]) for key in ["commit", "root_tree", "ref", "publication_state", "publication_receipt_sha256", "scope"]}
    bindings["source_publication"]["source_gate_or_export_authority_granted"] = False
    bindings["source_execution"] = {"reports": packet["execution_reports"], "summary": packet.get("execution_summary", {}),
                                     "source_export_admitted": False, "destination_gates_passed": False,
                                     "scope": "Current reported executions retain their own exact input and runtime closure. Historical proof is not relabelled."}
    bindings["merge_recovery"] = {
        "current_input": DESTINATION, "current_input_root": DESTINATION_ROOT,
        "current_input_epoch": TASK_ROOT + "/mapping-input-epoch.json",
        "intermediate_reviewed_input": {"commit": "d1b9162cd568108f4c8d82f6b6a03cccfdb91bd2", "root_tree": "0812573f02908d8078d13f5f49346865cdf1c0e0", "input_epoch_sha256": "fb413e58be72aaffa1e91149479ac159c6d4b961101eed4ab10f1c99df0b6711", "scope": "Prior 52-record fixture and inherited TQ validation failures retained; current TQ record is preserved from exact C0 input."}, "current_input_epoch_sha256": INPUT_EPOCH_SHA256,
        "earlier_reviewed_input": {"commit": "87590cb96fb0e2dc0f88fae8e01957f7179cd255", "ordered_records": 47, "non_jcc_records": 45, "top_level_mappings": 17, "gates": 20, "scope": "Earlier fixture and refused map input retained as history; current preimages come from the separately frozen C0 epoch."},
        "source_history_publication": SOURCE_HISTORY, "source_history_pr": 9427,
        "receiving_history_publication": RECEIVER_HISTORY, "receiving_history_pr": 189,
        "historical_receiving_source": "0483f79ae51dd578c2e4fec1b8431bd51b59b469",
        "restored_historical_files": 443, "retained_capabilities_scope": "historical recipe/test/evidence custody, not runtime capability promotion",
        "current_record_ids": [row["id"] for row in original_records], "non_jcc_records_preserved": 50,
        "source_publication_input": TASK_ROOT + "/source-publication-input.json",
        "historical_epochs": [
            {"scope": "Current input's earlier JCC mapping, preserved verbatim in recipe beforeimages", "source_commit": "0b8dc32b9e8a616b7b7141bbdd88722839dc64bc",
             "binding_sha256": digest(current_files[PATHS[2]]), "records": [row for row in original_records if row["id"] in IDS]},
            {"scope": "Prior0483 receiving handoff and all0b/d1/0c53 lineage; historical execution, publication and pending obligations retain their original scopes",
             "source_commit": "0c53b4f1dfe66c7eff05c8047b8fc681d26fecb8", "binding_sha256": digest(prior_files[PATHS[2]]),
             "binding_resource": RESOURCE_ROOT + "/jcc-source-recovery-handoff-20261005/after02-source-destination-bindings.json.txt",
             "records": [row for row in prior_map["migration"]["records"] if row["id"] in IDS],
             "source_execution": historical_binding["source_execution"], "source_publication": historical_binding["source_publication"]}],
        "donor_catalogue_history": {"source_commit": "4990edbf4ba4afaa14e0ba2d7613331c31e21e9e",
            "receiving_path": "m3/tooling/migration-recipes/tasks/jcc-source-recovery-handoff-20261005/donor-catalogue",
            "scope": "Previously published donor catalogue and provenance; no private challenge corpus or automatic qualification"}}
    fixture_note = "**UNPUBLISHABLE SYNTHETIC FIXTURE OUTPUT.**\n\n" if packet["fixture"] else ""
    introduction = fixture_note + "# Current JCC receiving recovery, 2026-10-06\n\n" + (
        "The canonical mapping now binds current source `" + packet["commit"] + "`, root `" + packet["root_tree"]
        + "`, from `" + packet["ref"] + "`. The 14 laboratory and 6 Java/JNI source owners have explicit body, Git blob and SHA-256 identities. "
        "All 52 mapping IDs retain their order; 50 other records, all 25 current top-level mapping objects, 20 gates and M3-owned precompute policy are preserved.\n\n"
        "The 443 recovered receiving artifacts retain their historical 0b/d1/0c53 and donor 4990 evidence. They are registered as historical custody in the existing retention audit. "
        "The previous source publication and receiver history are preserved; current execution reports appear separately in [source-destination-bindings.json](../migration/evidence/jcc-handoff-20261005/source-destination-bindings.json).\n\n"
        "Both JCC capability records remain blocked. Source export, whole-module coverage, actual merge/CI and applicable M3JDK21 image/runtime/JNI/native/platform/performance gates retain their existing requirements. "
        "M3JDK21 owns the runtime; Synexia supplies donor code and evidence. Recipe replay and historical custody do not establish a rebuilt-JDK pass.\n\n"
        "The current exact destination input is `" + DESTINATION + "`, root `" + DESTINATION_ROOT + "`. "
        "The separately qualified Synexia recipe-module POM compile edge is bound as source build provenance; it adds no M3JDK21 runtime dependency. The four current operational files are updated only by the existing hash-pinned text recipe and sealed installer. "
        "Its current source publication input is immutable, late-bound and refuses fixture or history-only identities.\n\n"
        "## Earlier notes, preserved as historical context\n\n")
    return {PATHS[0]: pretty(result), PATHS[1]: introduction.encode() + current_files[PATHS[1]],
            PATHS[2]: pretty(bindings), PATHS[3]: coverage}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--audit-root", required=True, type=Path)
    parser.add_argument("--source-input", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--fixture-only", action="store_true")
    args = parser.parse_args()
    audit = args.audit_root.resolve(strict=True)
    epoch_root = audit / "candidate/mapping-v03/input-epoch"
    epoch_bytes = (epoch_root / "INPUT_EPOCH.json").read_bytes()
    require(digest(epoch_bytes) == INPUT_EPOCH_SHA256, "current receiving input epoch drift")
    before_audit = json.loads(epoch_bytes)
    require(before_audit["commit"] == DESTINATION and before_audit["root_tree"] == DESTINATION_ROOT, "current receiving revision drift")
    current = {path: (epoch_root / "current" / path).read_bytes() for path in PATHS}
    prior = {path: (audit / "files/prior" / path).read_bytes() for path in PATHS}
    for row in before_audit["current_operational_preimages"]:
        require(digest(current[row["path"]]) == row["sha256"], "current operational preimage drift")
    packet = json.loads(args.source_input.read_text())
    require(packet["fixture"] == args.fixture_only, "fixture flag/input mismatch")
    after = compose(current, prior, packet, allow_fixture=args.fixture_only)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    base = output / "resources"
    base.mkdir()
    rows, plan_rows = [], []
    for index, path in enumerate(PATHS):
        before_name, after_name = f"before-{index:02d}.txt", f"after-{index:02d}.txt"
        (base / before_name).write_bytes(current[path])
        (base / after_name).write_bytes(after[path])
        rows.append({"path": path, "before_sha256": digest(current[path]), "after_sha256": digest(after[path]),
                     "before_git_blob": git_object("blob", current[path]), "after_git_blob": git_object("blob", after[path]),
                     "before_bytes": len(current[path]), "after_bytes": len(after[path]), "before_resource": before_name, "after_resource": after_name})
        plan_rows.append({"path": path, "before": {"resource": before_name, "sha256": digest(current[path])},
                          "after": {"resource": after_name, "sha256": digest(after[path])}})
    lines = sorted(rows, key=lambda row: row["path"])
    (base / "manifest.tsv").write_text("".join("\t".join([r["path"], r["before_sha256"], r["after_sha256"], r["after_resource"]]) + "\n" for r in lines))
    history = json.loads((audit / "candidate/HISTORY_RESOURCES.json").read_text())
    guard_plan = json.loads((audit / "candidate/overlay" / history["crates"][0]["plan_path"]).read_text())
    require({row["path"]: row["sha256"] for row in guard_plan["guards"]} ==
            {row["path"]: row["sha256"] for row in before_audit["guards"]}, "canonical guards drifted from frozen current epoch")
    for guard in before_audit["guards"]:
        require(digest((epoch_root / guard["file"]).read_bytes()) == guard["sha256"], "frozen canonical guard body drift")
    plan = {"schema": "m3.sealed-install/1", "recipe_id": "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/" + CRATE,
            "version": 1, "source_commit": packet["commit"], "destination_base": DESTINATION,
            "scope": "FIXTURE_ONLY_UNPUBLISHABLE" if args.fixture_only else "Exact current 20-owner mapping metadata; source and destination acceptance remain blocked",
            "outputs": sorted(plan_rows, key=lambda row: row["path"]), "guards": guard_plan["guards"]}
    plan["plan_sha256"] = digest(canonical(plan))
    (base / "plan.json").write_bytes(pretty(plan))
    (output / "source-publication-input.json").write_bytes(pretty(packet))
    (output / "mapping-input-epoch.json").write_bytes(epoch_bytes)
    result = {"schema": "m3-jcc-current-mapping-resources/1", "status": "FIXTURE_ONLY_UNPUBLISHABLE" if args.fixture_only else "ACTUAL_SOURCE_BOUND_TEMPLATES_NOT_EXECUTED",
              "fixture": args.fixture_only, "source_commit": packet["commit"], "source_root_tree": packet["root_tree"], "destination_base": DESTINATION,
              "crate": CRATE, "rows": rows, "plan_sha256": plan["plan_sha256"],
              "manifest_sha256": digest((base / "manifest.tsv").read_bytes()), "source_input_sha256": digest(pretty(packet)), "input_epoch_sha256": INPUT_EPOCH_SHA256, "destination_root_tree": DESTINATION_ROOT,
              "preserved": {"ordered_records": 52, "non_jcc_records": 50, "gates": 20, "top_level_mappings": 25},
              "bound_owners": {LAB: 14, JNI: 6}, "source_export_admitted": False, "destination_gates_passed": False}
    (output / "MAPPING_RESOURCES.json").write_bytes(pretty(result))
    print(json.dumps({key: result[key] for key in ["status", "source_commit", "manifest_sha256", "plan_sha256", "preserved", "bound_owners"]}))


if __name__ == "__main__":
    main()
