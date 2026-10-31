#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exercise six package refusals using compact copies of qualified actual inputs.

Run only after actual source binding, Maven/installer PASS, and canonical-after PASS:
  python3 -B verify_package_refusals.py --execution ACTUAL_EXECUTION \
      --validator ACTUAL_CANONICAL_AFTER --output NEW_REFUSAL_EVIDENCE

No successful packaging control is run. Each case must fail at its intended guard
before creating an output directory. Source revisions used for stale/root probes
come from the real historical binding beforeimage, never invented publications.
Only required input files and three cache-proof documents are copied; no Maven
cache, generated classes, command logs, or validator workspace is copied.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys
import traceback

HERE = Path(__file__).resolve().parent
PACKAGER = HERE / "package_mapping.py"
EPOCH_SHA256 = "4fd3b3bd4fb09759d8bb02ca9ba45d0b2934625f24bcc4ae195f3e49394dff70"
BEFORE_BINDINGS_SHA256 = "cdb5d7f4a0746fe561f51fa9a13cbd9fa319f8196bfd3075ca2288be7f3d1126"
TASK = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006"
PROOF = TASK + "/mapping-verification-v04"
RESOURCE = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-merge-recovery-20261006-mapping-v04"
PATHS = {"m3/docs/name-mapping.json", "m3/docs/jcc-source-handoff.md",
         "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
         "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv"}
CACHE_PROOFS = ("VERIFIED_CACHE.json", "OFFLINE_DIAGNOSTIC_CACHE_V02.json", "RECEIVER_ASM93_ADDON.json")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(body):
    return hashlib.sha256(body).hexdigest()


def git_blob(body):
    return hashlib.sha1(b"blob " + str(len(body)).encode() + b"\0" + body).hexdigest()


def relative(value):
    require(isinstance(value, str) and bool(value) and "\\" not in value and ":" not in value, "unsafe path")
    path = PurePosixPath(value)
    require(not path.is_absolute() and path.as_posix() == value and all(part not in ("", ".", "..", ".git")
            for part in value.split("/")) and not any(ord(char) < 32 for char in value), "unsafe relative path")
    return path


def read(root, value):
    path = root / relative(value)
    require(path.is_file() and not path.is_symlink(), "missing or symlink input: " + str(path))
    require(path.resolve().is_relative_to(root.resolve()), "input escapes its root")
    return path.read_bytes()


def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def documents(root, names):
    return {name: json.loads(read(root, name)) for name in names}


def snapshot(root, excluded=None):
    """Observe all original files, including generated files, without copying them."""
    result = {}
    for path in sorted(root.rglob("*")):
        if excluded is not None and (path == excluded or path.is_relative_to(excluded)):
            continue
        require(not path.is_symlink(), "symlink in original input tree")
        if path.is_file():
            result[path.relative_to(root).as_posix()] = digest(path.read_bytes())
    return result


def admit_actual(execution, validator):
    names = ["receipt.json", "INPUT_SEAL.json", "MATERIALIZED_OUTPUTS.json",
             "bound-inputs/MAPPING_RESOURCES.json", "bound-inputs/source-publication-input.json",
             "bound-inputs/mapping-input-epoch.json", "rendered-tests/RENDERED_TEMPLATE_RECEIPT.json"]
    docs = documents(execution, names)
    receipt, seal, results = [docs[name] for name in names[:3]]
    resources, source, epoch, rendered = [docs[name] for name in names[3:]]
    canonical = json.loads(read(validator, "receipt.json"))
    require(receipt["schema"] == "m3-jcc-current-mapping-execution/2" and receipt["status"] == "PASS", "actual execution PASS required")
    require(source["schema"] == "m3-jcc-source-publication-input/1" and source["fixture"] is False
            and source["publication_state"] == "ACTUAL_GITHUB_READBACK_VERIFIED", "actual source publication required")
    require(source["source_export_admitted"] is False and source["destination_gates_passed"] is False,
            "source acceptance must remain blocked")
    require(len(source["artifacts"]) == 20 and len(source["source_components"]) == 1, "source owner/component set changed")
    require(resources["schema"] == "m3-jcc-current-mapping-resources/1" and resources["fixture"] is False
            and resources["status"] == "ACTUAL_SOURCE_BOUND_TEMPLATES_NOT_EXECUTED", "actual binder receipt required")
    require(resources["source_commit"] == receipt["source_commit"] == seal["source_commit"] == source["commit"]
            and resources["source_root_tree"] == receipt["source_root_tree"] == seal["source_root_tree"] == source["root_tree"], "mixed source identity")
    require(resources["input_epoch_sha256"] == seal["receiving_input_epoch_sha256"] == EPOCH_SHA256
            == digest(read(execution, "bound-inputs/mapping-input-epoch.json")), "wrong receiving epoch")
    require(resources["destination_base"] == receipt["destination_input"] == epoch["commit"]
            and resources["destination_root_tree"] == receipt["destination_root"] == epoch["root_tree"], "mixed destination identity")
    source_hash = digest(read(execution, "bound-inputs/source-publication-input.json"))
    require(source_hash == resources["source_input_sha256"] == receipt["source_input_sha256"] == seal["source_input_sha256"], "source packet bytes differ")
    sealed = {row["path"]: row for row in seal["files"]}
    require(len(sealed) == len(seal["files"]), "duplicate sealed input")
    for path, row in sealed.items():
        body = read(execution, "repository/" + str(relative(path)))
        require(len(body) == row["bytes"] and digest(body) == row["sha256"] and git_blob(body) == row["git_blob"], "original sealed input drift: " + path)
    require(results["status"] == "PASS" and results["source_commit"] == source["commit"]
            and results["destination_input"] == resources["destination_base"], "actual materialized receipt required")
    result_rows = {row["path"]: row for row in results["files"]}
    require(len(results["files"]) == 4 and set(result_rows) == PATHS
            and len(resources["rows"]) == 4 and {row["path"] for row in resources["rows"]} == PATHS, "four unique actual outputs required")
    require(set(snapshot(execution / "materialized")) == PATHS, "actual Result directory has extra/missing files")
    require(canonical["status"] == "PASS" and canonical["exit_code"] == 0
            and canonical["mode"] == "ACTUAL_FOUR_AFTERIMAGES" and not canonical["diagnostics"], "canonical actual-after PASS required")
    require(canonical["commit"] == resources["destination_base"] and canonical["root_tree"] == resources["destination_root_tree"], "canonical receiving epoch differs")
    binding = canonical["actual_source_binding"]
    require(binding["source_commit"] == source["commit"] and binding["source_root_tree"] == source["root_tree"]
            and binding["source_input_sha256"] == source_hash
            and binding["mapping_resources_sha256"] == digest(read(execution, "bound-inputs/MAPPING_RESOURCES.json"))
            and binding["input_epoch_sha256"] == EPOCH_SHA256
            and Path(binding["materialized_root"]).resolve() == execution / "materialized", "canonical proof not bound to selected execution")
    for row in resources["rows"]:
        path = row["path"]
        body = read(execution, "materialized/" + path)
        require(digest(body) == row["after_sha256"] == result_rows[path]["sha256"] == canonical["inputs"][path]
                and git_blob(body) == row["after_git_blob"] == result_rows[path]["git_blob"]
                and len(body) == row["after_bytes"] == result_rows[path]["bytes"], "actual output drift: " + path)
        require(sealed[path]["sha256"] == row["before_sha256"] and sealed[path]["git_blob"] == row["before_git_blob"], "beforeimage drift")
    selected = set(names)
    selected.update("repository/" + path for path in sealed)
    selected.update("materialized/" + path for path in PATHS)
    resource_names = set(snapshot(execution / "bound-inputs/resources"))
    require(len(resource_names) == 10 and {RESOURCE + "/" + name for name in resource_names}
            == {path for path in sealed if path.startswith(RESOURCE + "/")}, "ten sealed crate resources required")
    for name in resource_names:
        require(digest(read(execution, "bound-inputs/resources/" + name)) == sealed[RESOURCE + "/" + name]["sha256"], "replay resource drift")
        selected.add("bound-inputs/resources/" + name)
    require(len(rendered["files"]) == 4 and len({row["path"] for row in rendered["files"]}) == 4, "four unique rendered artifacts required")
    for row in rendered["files"]:
        path = row["path"]
        require(path in sealed and digest(read(execution, "rendered-tests/" + path)) == row["sha256"] == sealed[path]["sha256"], "rendered input drift")
        selected.add("rendered-tests/" + path)
    for name, key in zip(CACHE_PROOFS, ["verified_cache_sha256", "cache_origin_sha256", "asm93_addon_receipt_sha256"]):
        require(digest(read(execution, "cache-proof/" + name)) == receipt[key], "cache proof drift")
        selected.add("cache-proof/" + name)
    for name in ["source-publication-input.json", "mapping-input-epoch.json"]:
        require(digest(read(execution, "bound-inputs/" + name)) == sealed[TASK + "/" + name.replace(".json", "-v04.json")]["sha256"], "bound document differs from seal")
    require(digest(read(execution, "bound-inputs/MAPPING_RESOURCES.json")) == sealed[PROOF + "/MAPPING_RESOURCES.json"]["sha256"], "resource receipt differs from seal")
    historical_bytes = read(execution, "bound-inputs/resources/before-02.txt")
    require(digest(historical_bytes) == BEFORE_BINDINGS_SHA256, "historical binding beforeimage drift")
    historical = json.loads(historical_bytes)
    require(re.fullmatch(r"[0-9a-f]{40}", historical["source_commit"]) is not None
            and re.fullmatch(r"[0-9a-f]{40}", historical["source_root_tree"]) is not None
            and historical["source_commit"] != source["commit"] and historical["source_root_tree"] != source["root_tree"], "distinct actual historical source identity required")
    return sorted(selected), source, historical


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execution", required=True, type=Path)
    parser.add_argument("--validator", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    execution, validator = args.execution.resolve(strict=True), args.validator.resolve(strict=True)
    output = args.output.resolve()
    require(execution.is_dir() and validator.is_dir() and not output.exists(), "existing actual inputs and new output required")
    require(not output.is_relative_to(validator), "evidence must stay outside original validator inputs")
    if output.is_relative_to(execution):
        require(output.parent == execution and output.name == "packaging-refusals",
                "only the new direct execution/packaging-refusals evidence directory may be nested in execution")
    require(sys.flags.optimize == 0, "refusal harness requires assertions enabled")
    selected, source, historical = admit_actual(execution, validator)
    original = {"execution": snapshot(execution), "validator": snapshot(validator)}
    excluded_evidence = output if output.is_relative_to(execution) else None
    packager_bytes = PACKAGER.read_bytes()
    output.mkdir(parents=True)
    started = datetime.now(timezone.utc).isoformat()
    status, cases = "FAIL", []
    copied_bytes = sum(len(read(execution, path)) for path in selected) + len(read(validator, "receipt.json"))
    dump(output / "INPUTS.json", {"schema": "m3-jcc-package-refusal-inputs/1", "source_commit": source["commit"],
         "source_root_tree": source["root_tree"], "input_epoch_sha256": EPOCH_SHA256,
         "packager_sha256": digest(packager_bytes), "original_execution": str(execution), "original_validator": str(validator),
         "selected_execution_files": [{"path": path, "sha256": original["execution"][path]} for path in selected],
         "selected_validator_files": [{"path": "receipt.json", "sha256": original["validator"]["receipt.json"]}],
         "copied_files_per_case": len(selected) + 1, "copied_bytes_per_case": copied_bytes,
         "new_evidence_subtree_excluded_from_original_comparison": str(excluded_evidence) if excluded_evidence else None,
         "original_file_hashes": original, "maven_cache_copied": False, "successful_package_control": False})
    specifications = [
        ("01-stale-validator-source", 'assert binding["source_commit"] == source["commit"]'),
        ("02-changed-actual-output", 'assert sha(body) == row["after_sha256"]'),
        ("03-drifted-replay-resource", 'assert sha(body) == sealed[path]["sha256"]'),
        ("04-duplicate-operational-row", 'assert len(resources["rows"]) == 4'),
        ("05-mismatched-source-root", 'assert resources["source_root_tree"] == receipt["source_root_tree"]'),
        ("06-missing-cache-provenance", 'cache_proof / "VERIFIED_CACHE.json"'),
    ]
    try:
        for name, guard in specifications:
            case = output / "cases" / name
            # The packager's existing workspace exclusion keeps deliberately
            # damaged negative-case copies out of any later evidence delivery.
            copied_execution, copied_validator = case / "workspace/execution", case / "workspace/validator"
            for path in selected:
                target = copied_execution / relative(path)
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(read(execution, path))
            copied_validator.mkdir(parents=True)
            canonical = json.loads(read(validator, "receipt.json"))
            relocation = {"field": "actual_source_binding.materialized_root",
                          "from": canonical["actual_source_binding"]["materialized_root"],
                          "to": str(copied_execution / "materialized"),
                          "scope": "Harness-only relocation to the exact isolated actual Result copies"}
            canonical["actual_source_binding"]["materialized_root"] = str(copied_execution / "materialized")
            dump(copied_validator / "receipt.json", canonical)
            dump(case / "HARNESS_RELOCATION.json", relocation)
            mutation = {}
            if name == "01-stale-validator-source":
                canonical["actual_source_binding"]["source_commit"] = historical["source_commit"]
                dump(copied_validator / "receipt.json", canonical)
                mutation = {"field": "actual_source_binding.source_commit", "from": source["commit"],
                            "to": historical["source_commit"], "identity_origin": "Exact actual historical before-02.txt"}
            elif name == "02-changed-actual-output":
                path = "m3/docs/name-mapping.json"
                target = copied_execution / "materialized" / path
                target.write_bytes(target.read_bytes() + b"\nPACKAGE_REFUSAL_PROBE\n")
                mutation = {"path": "materialized/" + path, "kind": "append bytes to isolated actual Result copy"}
            elif name == "03-drifted-replay-resource":
                target = copied_execution / "bound-inputs/resources/after-00.txt"
                target.write_bytes(target.read_bytes() + b"\nPACKAGE_REFUSAL_PROBE\n")
                mutation = {"path": "bound-inputs/resources/after-00.txt", "kind": "append bytes to isolated replay resource"}
            elif name == "04-duplicate-operational-row":
                target = copied_execution / "bound-inputs/MAPPING_RESOURCES.json"
                resource_receipt = json.loads(target.read_bytes())
                resource_receipt["rows"].append(dict(resource_receipt["rows"][0]))
                dump(target, resource_receipt)
                mutation = {"path": "bound-inputs/MAPPING_RESOURCES.json", "kind": "duplicate one existing actual row"}
            elif name == "05-mismatched-source-root":
                target = copied_execution / "bound-inputs/MAPPING_RESOURCES.json"
                resource_receipt = json.loads(target.read_bytes())
                resource_receipt["source_root_tree"] = historical["source_root_tree"]
                dump(target, resource_receipt)
                mutation = {"field": "source_root_tree", "from": source["root_tree"],
                            "to": historical["source_root_tree"], "identity_origin": "Exact actual historical before-02.txt"}
            elif name == "06-missing-cache-provenance":
                (copied_execution / "cache-proof/VERIFIED_CACHE.json").unlink()
                mutation = {"path": "cache-proof/VERIFIED_CACHE.json", "kind": "remove isolated proof copy"}
            attempt = case / "forbidden-output"
            dump(case / "mutation.json", mutation)
            snapshot_before = {"execution": snapshot(copied_execution), "validator": snapshot(copied_validator)}
            command = [sys.executable, "-I", "-B", str(PACKAGER), "--execution", str(copied_execution),
                       "--validator", str(copied_validator), "--output", str(attempt)]
            result = subprocess.run(command, cwd=HERE, capture_output=True, check=False)
            (case / "stdout.log").write_bytes(result.stdout)
            (case / "stderr.log").write_bytes(result.stderr)
            unchanged = snapshot_before == {"execution": snapshot(copied_execution), "validator": snapshot(copied_validator)}
            diagnostic = result.stderr.decode(errors="replace")
            matched_guard = guard in diagnostic
            expected_exception = "FileNotFoundError" if name == "06-missing-cache-provenance" else "AssertionError"
            final_line = next((line for line in reversed(diagnostic.splitlines()) if line.strip()), "")
            matched_exception = final_line == expected_exception or final_line.startswith(expected_exception + ":")
            passed = result.returncode != 0 and not attempt.exists() and unchanged and matched_guard and matched_exception
            row = {"case": name, "status": "PASS" if passed else "FAIL", "command": command,
                   "exit_code": result.returncode, "output_created": attempt.exists(), "input_copies_unchanged": unchanged,
                   "expected_guard": guard, "intended_guard_observed": matched_guard, "mutation": mutation,
                   "expected_exception": expected_exception, "intended_exception_observed": matched_exception,
                   "harness_relocation": relocation,
                   "stdout_sha256": digest(result.stdout), "stderr_sha256": digest(result.stderr)}
            dump(case / "receipt.json", row)
            cases.append(row)
            require(passed, "refusal did not reach its intended guard without writes: " + name)
        require(len(cases) == 6, "six refusal cases required")
        status = "PASS"
    except BaseException:
        (output / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        originals_unchanged = original == {"execution": snapshot(execution, excluded_evidence), "validator": snapshot(validator)}
        packager_unchanged = PACKAGER.read_bytes() == packager_bytes
        if not originals_unchanged or not packager_unchanged:
            status = "FAIL"
        receipt = {"schema": "m3-jcc-package-refusal-verification/1", "status": status, "started": started,
                   "finished": datetime.now(timezone.utc).isoformat(), "source_commit": source["commit"],
                   "source_root_tree": source["root_tree"], "input_epoch_sha256": EPOCH_SHA256,
                   "packager_sha256": digest(packager_bytes), "harness_sha256": digest(Path(__file__).read_bytes()),
                   "original_inputs_unchanged": originals_unchanged, "packager_unchanged": packager_unchanged,
                   "new_evidence_subtree_excluded_from_original_comparison": str(excluded_evidence) if excluded_evidence else None,
                   "refusals": cases, "successful_package_control": False, "maven_cache_copied": False,
                   "source_export_admitted": False, "destination_gates_passed": False,
                   "scope": "Six isolated negative packaging checks over qualified actual inputs. No successful package/publication control and no invented source publication identities."}
        dump(output / "receipt.json", receipt)
    require(status == "PASS", "original input or packager changed")
    print(json.dumps({"status": status, "refusals": len(cases), "original_inputs_unchanged": True,
                      "output_directories_created_by_packager": 0, "receipt": str(output / "receipt.json")}))


if __name__ == "__main__":
    main()
