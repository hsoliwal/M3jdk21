"""Author reviewed successor recipe resources; retained recipe.py materializes outputs.

This work-only authoring script does not directly write the four operational files.
"""
from __future__ import annotations

import copy
import csv
import hashlib
import io
import json
from pathlib import Path

BASE = Path(__file__).resolve().parent
WORK = BASE.parents[1]
OVERLAY = BASE / "overlay"
SOURCE = "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906"
SOURCE_TREE = "6f9b6a48f8cefa7a643f222c7aa1f621497725e7"
SOURCE_REF = "refs/heads/aix/jcc-canonical-integration-20261005"
E2 = "df06cdee5a8526f573b3ad89622824d0b49e2f25"
E2_TREE = "e3e079ba36553bae517c3e53a5a834b24452a82e"
CRATE_NAME = "jcc-source-final-handoff-20261005"
PREFIX = Path("m3/tooling/migration-recipes")
CRATE = OVERLAY / PREFIX / "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text" / CRATE_NAME
E2_CRATE = CRATE.parent / "jcc-handoff-20261005"
TASK = PREFIX / "tasks" / CRATE_NAME
LAB = "synexia.jcc-recipe-laboratory"
JNI = "synexia.jcc-java-jni-regression"
IDS = (LAB, JNI)
MAPPING = "m3/docs/name-mapping.json"
BINDING = "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json"
COVERAGE = "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv"
DOCUMENT = "m3/docs/jcc-source-handoff.md"


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def data(path: Path):
    return json.loads(path.read_text())


def encoded(value) -> bytes:
    return (json.dumps(value, indent=2, ensure_ascii=False) + "\n").encode()


def identity(path: Path) -> dict:
    body = path.read_bytes()
    return {"bytes": len(body), "sha256": digest(body),
            "git_blob_sha1": hashlib.sha1(f"blob {len(body)}\0".encode() + body).hexdigest()}


def artifact(row: dict) -> dict:
    assert row["commit"] == SOURCE and row["tracking_ref"] == SOURCE_REF
    actual = identity(Path(row["local_path"]))
    for key in ("bytes", "sha256", "git_blob_sha1"):
        assert actual[key] == row[key], (row["path"], key)
    return {"repo": row["repo"], "commit": SOURCE, "module": row["module"],
            "path": row["path"], "symbol": row["symbol"], "signatures": [],
            "sha256": row["sha256"], "git_blob_sha1": row["git_blob_sha1"],
            "fingerprint": None, "revision_role": "pinned", "tracking_ref": SOURCE_REF}


def copy_evidence(source: Path, name: str) -> dict:
    relative = TASK / "source-final" / name
    destination = OVERLAY / relative
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(source.read_bytes())
    return {"path": str(relative), **identity(destination)}


def main() -> None:
    inventory_path = BASE / "source-binding-review/SOURCE_FIELDS_AND_PROOFS.json"
    assert digest(inventory_path.read_bytes()) == "b0a1ff2a675017bc4eda2620a031bb963d038c21e10ddacb8f57b1fc51f6188a"
    inventory = data(inventory_path)
    root_path = BASE / "source-binding-review/root-accounting/ROOT_ACCOUNTING.json"
    assert digest(root_path.read_bytes()) == "cfe76d41dda4220937086d951a126bf04dec0d282ba088b6de2a6bff33b6e14d"
    root = data(root_path)
    assert inventory["source_commit"] == root["final_commit"] == SOURCE
    assert inventory["source_root_tree"] == root["final_root_tree"] == SOURCE_TREE
    publication_path = WORK / "publication-current/SOURCE_IMPLEMENTATION_PUBLICATION_VERIFIED.json"
    assert digest(publication_path.read_bytes()) == "03ba5aa231929079fda7772dc6a798a687ba7486fa81ceacc0723407330f956a"
    publication = data(publication_path)
    assert publication["commit"] == SOURCE and publication["tree"] == SOURCE_TREE
    assert len(publication["tree_entries_verified"]) == 1016 and len(publication["preserved_files_verified"]) == 112
    assert all(row["verified"] is True for row in publication["tree_entries_verified"])
    assert publication["commit_parent_tree_and_ref_verified"] is True
    assert publication["full_reactor_export_jdk_acceptance"] is False
    transport_path = Path(publication["manifest_path"])
    assert digest(transport_path.read_bytes()) == publication["manifest_sha256"]
    transport_rows = {row["path"]: row for row in data(transport_path)["rows"]}
    remote_rows = {row["path"]: row for row in publication["tree_entries_verified"]}
    for proof in inventory["proof_references"]:
        actual = identity(Path(proof["local_path"]))
        advertised = transport_rows[proof["path"]]
        assert proof["commit"] == SOURCE
        assert actual["bytes"] == proof["bytes"] == advertised["bytes"]
        assert actual["sha256"] == proof["sha256"] == advertised["sha256"]
        assert actual["git_blob_sha1"] == proof["git_blob_sha1"] == advertised["git_blob"]
        assert remote_rows[proof["path"]]["git_blob"] == proof["git_blob_sha1"]
        assert remote_rows[proof["path"]]["bytes"] == proof["bytes"]

    evidence = {
        "source_fields": copy_evidence(inventory_path, "SOURCE_FIELDS_AND_PROOFS.json"),
        "source_review": copy_evidence(BASE / "source-binding-review/REVIEW.md", "SOURCE_REVIEW.md"),
        "root_accounting": copy_evidence(root_path, "ROOT_ACCOUNTING.json"),
        "publication_readback": copy_evidence(publication_path, "SOURCE_IMPLEMENTATION_PUBLICATION_VERIFIED.json"),
        "production_body_readback": copy_evidence(WORK / "publication-current/SOURCE_PRODUCTION_BODY_READBACK.json", "SOURCE_PRODUCTION_BODY_READBACK.json"),
        "pr_observation": copy_evidence(WORK / "publication-current/PR_STATE_AFTER_SOURCE_PUBLICATION.json", "PR_STATE_AFTER_SOURCE_PUBLICATION.json"),
        "source_ci_observation": copy_evidence(WORK / "publication-current/SOURCE_INITIAL_CI_OBSERVATION.json", "SOURCE_INITIAL_CI_OBSERVATION.json"),
        "master_preimage_observation": copy_evidence(WORK / "publication-current/M3_CURRENT_MASTER_PREIMAGES.json", "M3_CURRENT_MASTER_PREIMAGES.json"),
    }
    old_map = data(CRATE / "before00-name-mapping.json.txt")
    mapping = copy.deepcopy(old_map)
    rows = {row["id"]: row for row in mapping["migration"]["records"]}
    old_rows = {row["id"]: row for row in old_map["migration"]["records"]}
    assert len(rows) == 46 and len(mapping["migration"]["gates"]) == 20
    for record_id in IDS:
        row = rows[record_id]
        for old in row["sources"]:
            assert old not in row["lineage"]["previous_sources"]
            row["lineage"]["previous_sources"].append(copy.deepcopy(old))
        row["sources"] = []
    for source in inventory["retained_mapped_sources"]:
        old = source["previous_source_object"]
        assert old in old_rows[source["existing_record"]]["sources"]
        current = artifact(source)
        current["signatures"] = old["signatures"]
        current["fingerprint"] = old["fingerprint"]
        rows[source["existing_record"]]["sources"].append(current)
    for source in inventory["production_changes"]:
        rows[source["suggested_existing_record"]]["sources"].append(artifact(source))
    for source in inventory["retained_contract_sources"]:
        rows[LAB]["sources"].append(artifact(source))
    assert [len(rows[record_id]["sources"]) for record_id in IDS] == [11, 6]

    receiver = next(target for target in rows[LAB]["targets"]
                    if target["symbol"] == "com.m3.rewrite.atom.JccReceivingFixtureTest")
    assert receiver["commit"] is None and receiver["revision_role"] == "candidate"
    rows[LAB]["lineage"]["previous_targets"].append(copy.deepcopy(receiver))
    receiver["commit"] = E2
    receiver["revision_role"] = "pinned"
    for record_id in IDS:
        row = rows[record_id]
        assert row["status"] == "blocked" and row["tests"] == []
        row["sync"]["source_revision"] = SOURCE
        row["sync"]["target_revision"] = E2
        row["sync"]["pending"] = [
            "Complete the exact canonical static-analysis runtime closure and unchanged original upstream gate; final replay stopped at 46-upstream-java with exit 1 (67 tests: 66 passed, zero failures, one error).",
            "Execute the thirteen later declared stages, including required JNI/native build and Java/native parity; they did not run in the final source replay.",
            "Require the actual source-canon checkpoint, history/pattern audit and all eleven verification gates at the same eligible execution root before source export.",
            "Complete source-qualified destination applicability and whole owning-module 99% line/branch, image/runtime and applicable JNI gates; finite original receiver proof and artifact publication do not satisfy them.",
            "Reconcile source PR 9285 with moving develop separately; observed dirty mergeability does not rewrite the frozen source input or publication pins.",
            "Resolve or reobserve the prior receiving CI failures separately; merging PR 139 is publication history, not gate evidence."
        ]
        row["recipe"]["id"] = "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe/" + CRATE_NAME
        row["recipe"]["preconditions"] = [
            "All four existing E2 preimages at " + E2 + "; canonical map SHA256 " + digest((CRATE / "before00-name-mapping.json.txt").read_bytes()) + ".",
            "Use the unchanged retained text transformer and sealed-install/1 owner; preserve the historical E2 crate, seven guards and exact previous source/receiver artifact objects.",
            "Source publication " + SOURCE + " is verified; qualification remains blocked at the required upstream runtime gate and later JNI stages are unexecuted."
        ]
        row["observation"] = (
            "Final source publication d1cf2d81 and its nine exact production fixes are explicitly accounted. "
            "Four old source objects remain intact in lineage; source execution input 0b8dc32b and local seed are distinct from publication. "
            "See " + BINDING + "#source_execution and " + str(TASK) + "/source-final/. "
            "Source core 91 and parent 8 passed; upstream 67 includes one error and no failures. Later JNI gates did not run. "
            "These scoped source results, the independently authored receiver and committed artifact read-back grant no capability export or JDK acceptance."
        )
    rows[LAB]["reason"] = (
        "Blocked source-qualified JCC laboratory handoff, now bound to actual source publication d1cf2d81 and the published original receiver at df06cdee. "
        "Final source runtime closure, later JNI, source-canon export and destination full-module/JDK obligations remain incomplete."
    )
    rows[JNI]["reason"] = (
        "Java/regex/opaque-source/JNI receiving applicability remains blocked. Four catalogue/donor/planning repair owners are source obligations only; "
        "their committed fixes and finite Java tests do not materialize a destination JNI/regex implementation or qualify unrun JNI gates."
    )
    rows[LAB]["materialization"]["outputs"] = (
        "Reviewed ledger/documentation and the original receiver tooling fixture are published at df06cdee; this successor updates metadata only. "
        "No source-qualified JCC capability, JNI/regex adapter or JDK product output is materialized."
    )
    rows[LAB]["materialization"]["cached_facts"] = (
        "Original four-test receiver evidence and owner inputs remain exact; its candidate artifact is now bound to its actual prior publication. "
        "Source final replay receipts retain their original 0b8dc input epoch and required-runtime failure. No source PASS count is transferred."
    )
    rows[JNI]["materialization"]["cached_facts"] = (
        "Nine source production fixes and retained owners are bound to d1cf2d81 with original replay evidence. Catalogue ordering intentionally changes derived Review roots. "
        "Finite core/parent Java passes are separate from the one-error original upstream gate and unrun JNI stages."
    )

    old_coverage = list(csv.DictReader(io.StringIO((CRATE / "before03-root-coverage-obligations.tsv.txt").read_text()), delimiter="\t"))
    entries = {row["path"]: row for row in root["root_entries"]}
    assert len(entries) == len(old_coverage) == 343
    declarations = {}
    for declaration in root["maven_declarations"]:
        declarations.setdefault(declaration["path"].split("/")[0], []).append(declaration)
    partial = {}
    for record_id in IDS:
        for source in rows[record_id]["sources"]:
            partial.setdefault(source["path"].split("/")[0], set()).add(record_id)
    for row in old_coverage:
        entry = entries[row["root_path"]]
        assert (row["object_type"], row["mode"]) == (entry["type"], entry["mode"])
        row.update(source_commit=SOURCE, source_tree=SOURCE_TREE, object_id=entry["sha"],
                   partial_capability_records=";".join(sorted(partial.get(row["root_path"], set()))))
        observed = declarations.get(row["root_path"], [])
        assert row["root_pom_declarations"] == ";".join(sorted(item["path"] for item in observed))
        assert row["declaration_contexts"] == ";".join(item["context"] for item in sorted(observed, key=lambda item: item["path"]))
    buffer = io.StringIO(newline="")
    writer = csv.DictWriter(buffer, fieldnames=list(old_coverage[0]), delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(old_coverage)
    coverage_bytes = buffer.getvalue().encode()

    binding = data(CRATE / "before02-source-destination-bindings.json.txt")
    binding["source_commit"] = SOURCE
    binding["source_root_tree"] = SOURCE_TREE
    binding["destination_commit"] = E2
    binding["destination_root_tree"] = E2_TREE
    binding["canonical_map_preimage_sha256"] = digest((CRATE / "before00-name-mapping.json.txt").read_bytes())
    binding["source_acquisition_receipt_sha256"] = identity(inventory_path)["sha256"]
    binding["source_acquisition_scope"] = "17 exact source owner/contracts at the published feature commit, including nine actual production changes and eight unchanged owners/contracts; 503 selected original inputs compared. Not whole-repository semantic coverage or source export."
    binding["source_root_receipt_sha256"] = identity(root_path)["sha256"]
    binding["source_root_accounting"]["coverage_receipt_sha256"] = digest(coverage_bytes)
    binding["source_artifacts"] = [source for record_id in IDS for source in rows[record_id]["sources"]]
    fixture = binding["independent_receiving_fixture"]
    fixture["candidate_artifact"] = copy.deepcopy(receiver)
    fixture["classification"] = "Original destination tooling test with exact existing artifact read-back at df06cdee; no source implementation or source PASS receipt imported."
    fixture["qualification"] = "The original finite four-test proof and all its inputs/receipts remain unchanged. Its artifact is published and read back; no behavior rerun was needed for this metadata-only repin. Capability tests[] stay empty and source/full-module/JDK acceptance remains blocked."
    binding["prior_handoff"] = {
        "commit": E2, "root_tree": E2_TREE, "pull_request": "https://github.com/hsoliwal/M3jdk21/pull/139",
        "observed_state": "MERGED", "merge_commit": "8131f535300c005afd27443d0281ede11198522f",
        "old_source_commit": "0b8dc32b9e8a616b7b7141bbdd88722839dc64bc",
        "old_source_root": "3a270e543f9a547582ebda8ac8b69bfd134c41be",
        "preserved_crate": str(E2_CRATE.relative_to(OVERLAY)),
        "old_binding_resource": str((E2_CRATE / "02-source-destination-bindings.json.txt").relative_to(OVERLAY)),
        "scope": "Publication lineage only. All old canonical source and candidate receiver artifact objects remain in operational lineage; complete E2 recipe/evidence remains unchanged."
    }
    binding["source_publication"] = {
        "commit": SOURCE, "root_tree": SOURCE_TREE, "tracking_ref": SOURCE_REF,
        "parent": publication["parent"], "pull_request": "https://github.com/hsoliwal/com.synexia/pull/9285",
        "payload_files_read_back": 1016, "payload_bytes": 22836558, "preserved_files_read_back": 112,
        "production_bodies_directly_read_back": 9,
        "transport_manifest_sha256": publication["manifest_sha256"], "transport_manifest_itself_published": False,
        "source_gate_or_export_authority_granted": False,
        "receipts": evidence
    }
    binding["source_execution"] = {
        "input_remote_revision": inventory["frozen_replay_input_revision"],
        "input_local_seed": "72c958b9856a505f7da5eced4e58a6ee1c4dc0b9", "local_seed_is_remote_ancestry": False,
        "published_output_revision": SOURCE,
        "relationship": "Actual recipe-produced postimages and unchanged selected inputs were published/read back. These tests were executed from the frozen original input packet, not claimed as a fresh checkout test at the later publication commit.",
        "compiled_first_party_owners": 443,
        "compiler_flags": ["--release 21", "-proc:none", "-Xlint:all", "-Werror"],
        "original_inputs": 503, "changed_production_inputs": 9, "byte_identical_original_inputs": 494,
        "advertised_production_bindings": 12, "already_satisfied_production_bindings": 3,
        "separate_protected_fixture_write": "Four wildcard imports expanded; bodies/assertions unchanged.",
        **inventory["qualification"],
        "api_limit": "69 externally visible types across 12 owners compared to retained epoch-04 API capture, with narrower real preimage comparisons; not complete original-baseline binary compatibility. Catalogue ordering/root changes are intentional.",
        "composition": "16 logical positive A/P/AP/PA schedules and 32 replay executions; serialized receipts match. Persisted source observations are hashes, not complete transformed source/class maps.",
        "proofs": [{key: row[key] for key in ("path", "commit", "bytes", "sha256", "git_blob_sha1")}
                   | {"repo": "hsoliwal/com.synexia", "url": "https://github.com/hsoliwal/com.synexia/blob/" + SOURCE + "/" + row["path"]}
                   for row in inventory["proof_references"]],
        "production_changes": [{"source": artifact(row), "previous_sha256": row["previous"]["sha256"],
                                "previous_git_blob_sha1": row["previous"]["git_blob_sha1"],
                                "record": row["suggested_existing_record"],
                                "assignment_qualification": row["assignment_qualification"],
                                "recipe_result_provenance": row["recipe_result_provenance"]}
                               for row in inventory["production_changes"]],
    }

    assert binding["descriptor_reconciliation"] == data(CRATE / "before02-source-destination-bindings.json.txt")["descriptor_reconciliation"]
    assert all(value is False for value in binding["acceptance"].values())
    assert len(binding["source_artifacts"]) == 17
    for record_id in set(rows) - set(IDS):
        assert rows[record_id] == old_rows[record_id], record_id
    old_globals, new_globals = copy.deepcopy(old_map), copy.deepcopy(mapping)
    del old_globals["migration"]["records"], new_globals["migration"]["records"]
    assert old_globals == new_globals

    document = (BASE / "handoff-document.md").read_bytes()
    outputs = {
        MAPPING: ("before00-name-mapping.json.txt", "after00-name-mapping.json.txt", encoded(mapping)),
        DOCUMENT: ("before01-jcc-source-handoff.md.txt", "after01-jcc-source-handoff.md.txt", document),
        BINDING: ("before02-source-destination-bindings.json.txt", "after02-source-destination-bindings.json.txt", encoded(binding)),
        COVERAGE: ("before03-root-coverage-obligations.tsv.txt", "after03-root-coverage-obligations.tsv.txt", coverage_bytes),
    }
    plan = {"schema": "m3.sealed-install/1", "recipe_id": CRATE_NAME + "/1", "source_commit": SOURCE,
            "target_commit": E2, "scope": "Additive metadata successor from four existing E2 outputs; actual source publication bound with upstream/JNI/export/capability acceptance still blocked.",
            "outputs": [], "guards": copy.deepcopy(data(E2_CRATE / "plan.json")["guards"])}
    manifest_lines = []
    for path, (before_name, after_name, body) in sorted(outputs.items()):
        before = (CRATE / before_name).read_bytes()
        assert (OVERLAY / path).read_bytes() == before, "Operational outputs must still be E2 preimages"
        assert body != before
        (CRATE / after_name).write_bytes(body)
        before_hash, after_hash = digest(before), digest(body)
        plan["outputs"].append({"path": path, "before": {"resource": before_name, "sha256": before_hash},
                                "after": {"resource": after_name, "sha256": after_hash}})
        manifest_lines.append("\t".join((path, before_hash, after_hash, after_name)))
    canonical = (json.dumps(plan, sort_keys=True, separators=(",", ":"), ensure_ascii=True) + "\n").encode()
    plan["plan_sha256"] = digest(canonical)
    (CRATE / "manifest.tsv").write_text("\n".join(manifest_lines) + "\n")
    (CRATE / "plan.json").write_bytes(encoded(plan))
    (BASE / "AUTHORING_RESULT.json").write_bytes(encoded({
        "source_commit": SOURCE, "destination_preimage_commit": E2,
        "plan_sha256": plan["plan_sha256"], "map_after_sha256": digest(outputs[MAPPING][2]),
        "four_operational_files_still_E2": True, "source_artifacts": 17, "source_production_changes": 9,
        "records": 46, "unchanged_records": 44, "gates": 20, "source_export_admitted": False,
        "source_receipt_refs": evidence, "recipe_execution": "PENDING_AFTER_FREEZE"}))
    print(json.dumps({"plan_sha256": plan["plan_sha256"], "map_after_sha256": digest(outputs[MAPPING][2]),
                      "source_artifacts": 17, "operational_outputs_unchanged": True}))


if __name__ == "__main__":
    main()
