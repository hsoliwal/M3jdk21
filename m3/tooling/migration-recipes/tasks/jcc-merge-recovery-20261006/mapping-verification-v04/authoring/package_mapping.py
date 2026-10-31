#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Package only qualified actual mapping outputs and their finite evidence."""
import argparse
import hashlib
import json
from pathlib import Path

HERE = Path(__file__).resolve().parent
AUDIT = HERE.parents[1]
TASK = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006"
PROOF = TASK + "/mapping-verification-v04"
RESOURCE = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-merge-recovery-20261006-mapping-v04"
PATHS = {"m3/docs/name-mapping.json", "m3/docs/jcc-source-handoff.md",
         "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
         "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv"}


def sha(body):
    return hashlib.sha256(body).hexdigest()


def git_blob(body):
    return hashlib.sha1(b"blob " + str(len(body)).encode() + b"\0" + body).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execution", required=True, type=Path)
    parser.add_argument("--validator", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    execution = args.execution.resolve(strict=True)
    validator = args.validator.resolve(strict=True)
    output = args.output.resolve()
    assert not output.exists()
    receipt = json.loads((execution / "receipt.json").read_bytes())
    assert receipt["status"] == "PASS"
    current_validator = json.loads((validator / "receipt.json").read_bytes())
    assert current_validator["status"] == "PASS" and current_validator["exit_code"] == 0
    assert current_validator["mode"] == "ACTUAL_FOUR_AFTERIMAGES" and not current_validator["diagnostics"]
    baseline = AUDIT / "current-map-validation-v04/execution/baseline-v01/receipt.json"
    baseline_receipt = json.loads(baseline.read_bytes())
    assert baseline_receipt["status"] == "PASS" and baseline_receipt["exit_code"] == 0
    bundle = execution / "bound-inputs"
    resources = json.loads((bundle / "MAPPING_RESOURCES.json").read_bytes())
    source = json.loads((bundle / "source-publication-input.json").read_bytes())
    assert not source["fixture"] and source["publication_state"] == "ACTUAL_GITHUB_READBACK_VERIFIED"
    assert resources["schema"] == "m3-jcc-current-mapping-resources/1" and resources["fixture"] is False
    assert resources["status"] == "ACTUAL_SOURCE_BOUND_TEMPLATES_NOT_EXECUTED"
    assert resources["source_commit"] == receipt["source_commit"] == source["commit"]
    assert resources["source_root_tree"] == receipt["source_root_tree"] == source["root_tree"]
    assert len(source["artifacts"]) == 20 and len(source["source_components"]) == 1
    assert len(resources["rows"]) == 4 and {row["path"] for row in resources["rows"]} == PATHS
    assert resources["input_epoch_sha256"] == "4fd3b3bd4fb09759d8bb02ca9ba45d0b2934625f24bcc4ae195f3e49394dff70"
    seal = json.loads((execution / "INPUT_SEAL.json").read_bytes())
    assert seal["source_commit"] == source["commit"] and seal["source_root_tree"] == source["root_tree"]
    assert seal["source_input_sha256"] == receipt["source_input_sha256"] == sha((bundle / "source-publication-input.json").read_bytes())
    assert seal["receiving_input_epoch_sha256"] == resources["input_epoch_sha256"]
    sealed = {row["path"]: row for row in seal["files"]}
    assert len(sealed) == len(seal["files"])
    for path, row in sealed.items():
        body = (execution / "repository" / path).read_bytes()
        assert len(body) == row["bytes"] and sha(body) == row["sha256"] and git_blob(body) == row["git_blob"]
    materialized = json.loads((execution / "MATERIALIZED_OUTPUTS.json").read_bytes())
    assert materialized["status"] == "PASS" and materialized["source_commit"] == source["commit"]
    assert materialized["destination_input"] == resources["destination_base"]
    result_rows = {row["path"]: row for row in materialized["files"]}
    assert len(materialized["files"]) == 4 and set(result_rows) == PATHS
    cache_proof = execution / "cache-proof"
    assert sha((cache_proof / "VERIFIED_CACHE.json").read_bytes()) == receipt["verified_cache_sha256"]
    assert sha((cache_proof / "OFFLINE_DIAGNOSTIC_CACHE_V02.json").read_bytes()) == receipt["cache_origin_sha256"]
    assert sha((cache_proof / "RECEIVER_ASM93_ADDON.json").read_bytes()) == receipt["asm93_addon_receipt_sha256"]
    assert current_validator["commit"] == baseline_receipt["commit"] == resources["destination_base"]
    assert current_validator["root_tree"] == baseline_receipt["root_tree"] == resources["destination_root_tree"]
    binding = current_validator["actual_source_binding"]
    assert binding["source_commit"] == source["commit"] and binding["source_root_tree"] == source["root_tree"]
    assert binding["source_input_sha256"] == sha((bundle / "source-publication-input.json").read_bytes())
    assert binding["mapping_resources_sha256"] == sha((bundle / "MAPPING_RESOURCES.json").read_bytes())
    assert binding["input_epoch_sha256"] == resources["input_epoch_sha256"]
    assert binding["input_epoch_sha256"] == sha((bundle / "mapping-input-epoch.json").read_bytes())
    assert Path(binding["materialized_root"]).resolve() == (execution / "materialized").resolve()
    additions = {}
    modifications = {}

    def add(path, origin):
        assert path not in additions and path not in modifications and path not in PATHS, path
        origin = Path(origin)
        assert origin.is_file() and not origin.is_symlink(), origin
        if path in sealed:
            body = origin.read_bytes()
            assert sha(body) == sealed[path]["sha256"] and len(body) == sealed[path]["bytes"]
        additions[path] = origin

    def add_tree(prefix, root, allowed=None):
        root = Path(root)
        for file in sorted(root.rglob("*")):
            if file.is_file() and not any(part in {"__pycache__", "target", "classes", "workspace", "fixture-output", "forbidden-output"}
                                          for part in file.relative_to(root).parts):
                relative = file.relative_to(root).as_posix()
                if allowed is None or allowed(relative):
                    add(prefix + "/" + relative, file)

    for row in resources["rows"]:
        assert row["path"] not in modifications
        origin = execution / "materialized" / row["path"]
        body = origin.read_bytes()
        assert sha(body) == row["after_sha256"] and git_blob(body) == row["after_git_blob"]
        assert len(body) == row["after_bytes"] == result_rows[row["path"]]["bytes"]
        assert result_rows[row["path"]]["sha256"] == row["after_sha256"]
        assert result_rows[row["path"]]["git_blob"] == row["after_git_blob"]
        assert sealed[row["path"]]["sha256"] == row["before_sha256"]
        assert sealed[row["path"]]["git_blob"] == row["before_git_blob"]
        assert current_validator["inputs"][row["path"]] == row["after_sha256"]
        modifications[row["path"]] = (origin, row["before_git_blob"])
    resource_paths = {RESOURCE + "/" + path.relative_to(bundle / "resources").as_posix()
                      for path in (bundle / "resources").rglob("*") if path.is_file()}
    assert resource_paths == {path for path in sealed if path.startswith(RESOURCE + "/")} and len(resource_paths) == 10
    add_tree(RESOURCE, bundle / "resources")
    render = execution / "rendered-tests"
    rendered = json.loads((render / "RENDERED_TEMPLATE_RECEIPT.json").read_bytes())
    assert len(rendered["files"]) == 4 and len({row["path"] for row in rendered["files"]}) == 4
    for row in rendered["files"]:
        assert row["path"] in sealed
        assert sha((render / row["path"]).read_bytes()) == row["sha256"]
        add(row["path"], render / row["path"])
    for name in ["source-publication-input.json", "mapping-input-epoch.json"]:
        add(TASK + "/" + name.replace(".json", "-v04.json"), bundle / name)
    prior_manifest = AUDIT / "publication-mapping-v03/PUBLICATION_MANIFEST.json"
    assert sha(prior_manifest.read_bytes()) == "2f816b0f0606257ca0c1746a0a30d54b96562a4b28ba0351429c006c6baf4d92"
    add(PROOF + "/prior-qualification/PUBLICATION_MANIFEST_V03.json", prior_manifest)
    source_packet_root = Path(receipt["source_input"]).parent
    publication_receipt_file = source_packet_root / "SOURCE_PUBLICATION_RECEIPT.json"
    assert sha(publication_receipt_file.read_bytes()) == source["publication_receipt_sha256"]
    publication_receipt = json.loads(publication_receipt_file.read_bytes())
    assert publication_receipt["commit"] == source["commit"] and publication_receipt["root_tree"] == source["root_tree"]
    add(PROOF + "/source-publication-custody/SOURCE_PUBLICATION_RECEIPT.json", publication_receipt_file)
    add(PROOF + "/source-publication-custody/PREFLIGHT.json", source_packet_root / "PREFLIGHT.json")
    source_captures = list(publication_receipt["captures"].values()) + publication_receipt["proofs"]
    source_captures += [row["readback"] for row in publication_receipt["owner_readbacks"]]
    source_captures += [publication_receipt["root_pom_readback"], publication_receipt["module_pom_readback"]]
    for proof in publication_receipt["proofs"]:
        proof_body = Path(proof["path"]).read_bytes()
        assert sha(proof_body) == proof["sha256"]
        value = json.loads(proof_body)
        if isinstance(value.get("captures"), list):
            source_captures += value["captures"]
    source_objects = {}
    for row in source_captures:
        origin = Path(row["path"])
        body = origin.read_bytes()
        assert sha(body) == row["sha256"] and len(body) == row["bytes"]
        if row["sha256"] not in source_objects:
            target = PROOF + "/source-publication-custody/objects/" + row["sha256"] + ".json"
            add(target, origin)
            source_objects[row["sha256"]] = {"original_path": row["path"], "repository_path": target,
                                               "bytes": row["bytes"], "sha256": row["sha256"]}
    for name in ["MAPPING_RESOURCES.json", "source-publication-input.json", "mapping-input-epoch.json"]:
        add(PROOF + "/execution/binding-inputs/" + name, bundle / name)
    add(PROOF + "/MAPPING_RESOURCES.json", bundle / "MAPPING_RESOURCES.json")
    add(PROOF + "/verify_installer.py", HERE / "verify_installer.py")
    add(PROOF + "/RENDERED_TEMPLATE_RECEIPT.json", render / "RENDERED_TEMPLATE_RECEIPT.json")
    for name in ["bind_mapping.py", "test_bind_mapping.py", "verify_fixture.py", "execute_mapping.py", "prepare_input_epoch.py", "package_mapping.py", "verify_package_refusals.py"]:
        add(PROOF + "/authoring/" + name, HERE / name)
    add_tree(PROOF + "/receiving-input", HERE / "input-epoch")
    for version in ["mapping-v04"]:
        base = AUDIT / "candidate" / version
        fixture_proof = "verification-v01"
        for filename in ["receipt.json", "01-unit-tests.stdout.log", "01-unit-tests.stderr.log",
                         "03-publication-mode-refusal.stdout.log", "03-publication-mode-refusal.stderr.log"]:
            add(PROOF + "/authoring-proof/" + version + "/" + filename, base / fixture_proof / filename)
        template = base / "java-template"
        packet = json.loads((template / "TEMPLATE_PACKET.json").read_bytes())
        add(PROOF + "/template-history/" + version + "/TEMPLATE_PACKET.json", template / "TEMPLATE_PACKET.json")
        for row in packet["files"]:
            path = template / row["path"]
            assert sha(path.read_bytes()) == row["sha256"]
            add(PROOF + "/template-history/" + version + "/" + row["path"], path)
    add_tree(PROOF + "/epoch-preflight-v01", HERE / "epoch-preflight-v01")
    for name in ["receipt.json", "INPUT_SEAL.json", "MATERIALIZED_OUTPUTS.json"]:
        add(PROOF + "/execution/" + name, execution / name)
    for folder in ["01-source-binding", "02-render-tests", "03-maven", "04-installer", "cache-proof"]:
        add_tree(PROOF + "/execution/" + folder, execution / folder)
    add_tree(PROOF + "/execution/installer", execution / "installer", lambda path: not path.startswith("fixture/"))
    add_tree(PROOF + "/execution/canonical-after", validator)
    for version in ["current-map-validation-v04"]:
        root = AUDIT / version
        packet = json.loads((root / "PACKET.json").read_bytes())
        add(PROOF + "/" + version + "/PACKET.json", root / "PACKET.json")
        # Preserve complete bounded baseline custody, including historical failures.
        add_tree(PROOF + "/" + version, root, lambda path: path != "PACKET.json")
    for path in ["MAPPING_FOLLOWUP_CURRENT_ADMISSION_FAILURE_V01.json", "MAPPING_FOLLOWUP_CURRENT_ADMISSION_V02.json",
                 "SOURCE_SUPPLEMENTAL_BODIES.json"]:
        add(PROOF + "/admission-history/" + path, AUDIT / path)
    packaging_proof = json.loads((execution / "packaging-refusals/receipt.json").read_bytes())
    assert packaging_proof["status"] == "PASS" and len(packaging_proof["refusals"]) == 6
    assert packaging_proof["source_commit"] == source["commit"] and packaging_proof["source_root_tree"] == source["root_tree"]
    assert packaging_proof["input_epoch_sha256"] == resources["input_epoch_sha256"]
    assert packaging_proof["packager_sha256"] == sha(Path(__file__).read_bytes())
    assert packaging_proof["harness_sha256"] == sha((HERE / "verify_package_refusals.py").read_bytes())
    assert packaging_proof["original_inputs_unchanged"] and packaging_proof["packager_unchanged"]
    assert all(row["status"] == "PASS" and row["exit_code"] != 0 and not row["output_created"]
               and row["input_copies_unchanged"] and row["intended_guard_observed"]
               and row["intended_exception_observed"] for row in packaging_proof["refusals"])
    add_tree(PROOF + "/execution/packaging-refusals", execution / "packaging-refusals")
    output.mkdir()
    source_index = output / "SOURCE_CUSTODY_INDEX.json"
    source_index.write_text(json.dumps({"schema": "m3-jcc-source-custody-path-index/1", "source_commit": source["commit"],
                                      "publication_receipt_sha256": source["publication_receipt_sha256"],
                                      "objects": list(source_objects.values())}, indent=2, sort_keys=True) + "\n")
    add(PROOF + "/source-publication-custody/INDEX.json", source_index)
    readme = output / "DELIVERY.md"
    readme.write_text(
        "# Current JCC mapping delivery\n\n"
        f"Source `{source['commit']}` / root `{source['root_tree']}` supplies exactly 20 designated Java owners "
        "(14 laboratory, six Java/JNI) and one separately qualified recipe-module POM compile edge. "
        f"Receiving input `{resources['destination_base']}` / root `{resources['destination_root_tree']}` is pinned.\n\n"
        "The unchanged `M3Jdk21HashPinnedTextSnapshotRecipe` produced all four operational afterimages. "
        "The package preserves 55 ordered mapping IDs, 53 complete non-JCC records, all 34 current top-level mapping objects, "
        "20 gates, the external TQ record update, and all concurrent regex records and policy/name-map nodes. Both JCC records remain blocked. Source build provenance creates no "
        "M3JDK21, HotSpot, java.base, or JNI dependency on Synexia.\n\n"
        "## Executed gates\n\n"
        "- 13 focused Java 21/OpenRewrite 8.17.1/JUnit 5.10.2 methods passed offline, including exact four-output replay, "
        "fresh fixed point, named recipe/serialization, identity/provenance/checksum, drift/missing/duplicate/scan-swap refusals, "
        "full current-node preservation, historical lineage, source components, and complete direct-root accounting.\n"
        "- The unchanged sealed installer agrees with the four actual recipe results; CLI apply/check/rollback/fixed-point passed, "
        "and all 28 target/guard/mixed-state refusals preserved caller bytes.\n"
        "- Six isolated publication-package refusals rejected stale validator identity, changed actual output, drifted replay resource, "
        "duplicate operational rows, mismatched source root, and missing cache proof without publishing or mutating original inputs.\n"
        "- The unchanged canonical migration validator passed on both the exact current baseline and the four actual afterimages "
        "with the prior map supplied. Both runs retain `completion=INCOMPLETE`. Historical d1 TQ failures and C0 V03 qualification remain in their original epochs.\n\n"
        "These are finite metadata and fixture gates. They do not establish whole-source export, semantic closure, rebuilt-JDK behavior, "
        "or native/platform/performance acceptance. All existing product gates retain their requirements.\n\n"
        "## Replay\n\n"
        "Run the focused `pom.xml` in this directory with `-Dm3.mapping.materialized=/absolute/new/result-directory verify`. "
        "The property writes only four actual Result bodies and refuses existing output files. The canonical validation runner is "
        "`current-map-validation-v04/run_validator.py`; provide an existing interpreter with jsonschema, those four results, "
        "`execution/binding-inputs` as `--mapping-resources`, and a fresh evidence directory. No helper installs dependencies.\n\n"
        "Replay also requires the two immutable historical oracle resources from the separately delivered history/retention component "
        "(`jcc-merge-recovery-20261006-history-01/history-0014.txt` and `history-0016.txt`). The final delivery includes that component. "
        "The execution cache proof verifies every file against the exact diagnostic snapshot and ASM 9.3 addition; broader native custody remains separate.\n\n"
        "Prepared templates, fixture refusal receipts, earlier failed baselines, the V03 provenance-metadata correction, and its completed C0 execution remain "
        "separate evidence. Synthetic binder afterimages are excluded. The four modified paths in the publication manifest identify "
        "their actual Maven Result origins and exact expected Git preimages.\n")
    add(PROOF + "/README.md", readme)
    overlay = output / "overlay"
    overlay.mkdir(parents=True)
    rows = []
    for path in sorted(set(additions) | set(modifications)):
        if path in modifications:
            origin, before = modifications[path]
            operation = "modify"
        else:
            origin, before, operation = additions[path], None, "add"
        body = origin.read_bytes()
        destination = overlay / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open("xb") as stream:
            stream.write(body)
        rows.append({"repository_path": path, "local_path": str(destination), "mode": "100644",
                     "bytes": len(body), "sha256": sha(body), "git_blob": git_blob(body),
                     "expected_before_git_blob": before, "operation": operation,
                     "origin_path": str(origin) if operation == "modify" else None})
    result = {"schema": "m3-jcc-current-mapping-publication/4", "status": "QUALIFIED_ACTUAL_RECIPE_OUTPUTS",
              "repository": "hsoliwal/M3jdk21", "destination_base": resources["destination_base"],
              "destination_root": resources["destination_root_tree"], "overlay": str(overlay),
              "source_commit": source["commit"], "source_root_tree": source["root_tree"],
              "source_java_owners": 20, "source_build_components": 1,
              "operational_modifications": 4, "additions": len(additions), "files": rows,
              "tests": {"junit": 13, "installer_refusals": 28, "packaging_refusals": 6, "canonical_baseline_exit": 0, "canonical_after_exit": 0},
              "source_export_admitted": False, "destination_gates_passed": False,
              "scope": "Exact current mapping metadata from actual OpenRewrite results, separately qualified source module POM provenance, and preserved bounded evidence. No rebuilt-JDK/runtime/native/export admission."}
    target = output / "PUBLICATION_MANIFEST.json"
    target.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"manifest": str(target), "sha256": sha(target.read_bytes()), "files": len(rows),
                      "modifications": 4, "additions": len(additions), "bytes": sum(row["bytes"] for row in rows)}))


if __name__ == "__main__":
    main()
