#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run the unchanged canonical validator on the current fixture or actual four outputs.

No dependency installation, network request, test execution, or source repair occurs.
Canonical nonzero exit status remains nonzero, including inherited baseline failures.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import subprocess

HERE = Path(__file__).resolve().parent
CURRENT = "09075810b55229662329a158923287e8a7118f86"
CURRENT_ROOT = "bba8647ea80d1cb4101394b4c0fc5a97f0b8eb52"
INPUT_EPOCH_SHA = "4fd3b3bd4fb09759d8bb02ca9ba45d0b2934625f24bcc4ae195f3e49394dff70"
VALIDATOR = "m3/migration/migration.py"
SCHEMA = "m3/docs/name-mapping.schema.json"
PATHS = ("m3/docs/name-mapping.json", "m3/docs/jcc-source-handoff.md",
         "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
         "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def sha(body):
    return hashlib.sha256(body).hexdigest()


def inventory(root):
    require(root.is_dir() and not root.is_symlink(), "unsafe fixture root")
    result = {}
    for path in sorted(root.rglob("*")):
        require(not path.is_symlink(), "symlink in fixture")
        if path.is_file():
            result[path.relative_to(root).as_posix()] = sha(path.read_bytes())
    return result


def write_json(path, value):
    with path.open("x") as stream:
        json.dump(value, stream, indent=2, sort_keys=True)
        stream.write("\n")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--python", required=True, type=Path, help="Existing interpreter with the canonical jsonschema dependency")
    parser.add_argument("--output", required=True, type=Path, help="New execution evidence directory")
    parser.add_argument("--materialized", type=Path, help="Actual Maven Result directory with exactly four target files")
    parser.add_argument("--mapping-resources", type=Path, help="Actual-mode V04 binder output directory")
    args = parser.parse_args()
    require(bool(args.materialized) == bool(args.mapping_resources), "actual results and binder resources are required together")
    python = args.python.absolute()
    require(python.is_file(), "Python executable is missing")
    output = args.output.resolve()
    require(not output.exists(), "execution directory already exists")
    base = HERE / "fixture"
    require(not output.is_relative_to(base.resolve()), "execution evidence must stay outside the immutable baseline fixture")
    manifest_bytes = (HERE / "FIXTURE_MANIFEST.json").read_bytes()
    manifest = json.loads(manifest_bytes)
    require(manifest["commit"] == CURRENT and manifest["root_tree"] == CURRENT_ROOT, "wrong baseline epoch")
    expected = {row["path"]: row["sha256"] for row in manifest["files"]}
    require(len(expected) == 52 and inventory(base) == expected, "current fixture changed")
    require(expected[VALIDATOR] == "fd54e5fade1fc3050816265f2317967dd4176e7bb5e0bc55e6f620878026e88e"
            and expected[SCHEMA] == "c29f34e5b5d858b60cddd43b2e7f273d9c01ad553cb0e3a5dcc568698646f70b", "canonical owner/schema changed")
    require(expected[PATHS[0]] == "9c96a0f6f7cc0f9b9d3e91fc3a42ef8e4eab212726fccd5e63e483f1acd7dfb1", "current map changed")
    replacements, binding = {}, None
    if args.materialized:
        actual = args.materialized.resolve(strict=True)
        bundle = args.mapping_resources.resolve(strict=True)
        require(not output.is_relative_to(actual) and not output.is_relative_to(bundle),
                "execution evidence must stay outside the actual result and binder input directories")
        require(set(inventory(actual)) == set(PATHS), "actual Result directory must contain exactly the four target files")
        receipt_bytes = (bundle / "MAPPING_RESOURCES.json").read_bytes()
        receipt = json.loads(receipt_bytes)
        packet_bytes = (bundle / "source-publication-input.json").read_bytes()
        packet = json.loads(packet_bytes)
        require(receipt["schema"] == "m3-jcc-current-mapping-resources/1" and receipt["fixture"] is False
                and receipt["status"] == "ACTUAL_SOURCE_BOUND_TEMPLATES_NOT_EXECUTED", "actual-mode binder receipt required")
        require(receipt["crate"] == "jcc-merge-recovery-20261006-mapping-v04"
                and receipt["destination_base"] == CURRENT and receipt["destination_root_tree"] == CURRENT_ROOT
                and receipt["input_epoch_sha256"] == INPUT_EPOCH_SHA, "different receiving input epoch")
        require(packet["schema"] == "m3-jcc-source-publication-input/1" and packet["fixture"] is False
                and packet["publication_state"] == "ACTUAL_GITHUB_READBACK_VERIFIED"
                and packet["scope"] == "current-five-owner-repair-publication", "verified actual source packet required")
        require(packet["source_export_admitted"] is False and packet["destination_gates_passed"] is False,
                "source packet must retain blocked acceptance")
        require(receipt["source_commit"] == packet["commit"] and receipt["source_root_tree"] == packet["root_tree"]
                and receipt["source_input_sha256"] == sha(packet_bytes), "source packet/receipt mismatch")
        require(sha((bundle / "mapping-input-epoch.json").read_bytes()) == INPUT_EPOCH_SHA, "input epoch copy changed")
        require(len(receipt["rows"]) == 4 and {row["path"] for row in receipt["rows"]} == set(PATHS), "four exact binder output rows required")
        for row in receipt["rows"]:
            path = row["path"]
            body = (actual / path).read_bytes()
            require(row["before_sha256"] == expected[path] and sha(body) == row["after_sha256"], "actual Result image drift: " + path)
            replacements[path] = body
        binding = {"source_commit": packet["commit"], "source_root_tree": packet["root_tree"],
                   "source_input_sha256": sha(packet_bytes), "mapping_resources_sha256": sha(receipt_bytes),
                   "materialized_root": str(actual), "input_epoch_sha256": INPUT_EPOCH_SHA}

    # Runtime introspection is local and read-only. Its manifest records the selected
    # existing environment without installing or relabelling any package.
    probe = r'''
import hashlib,importlib.metadata,json,pathlib,sys
packages=[]
for name in ['jsonschema','attrs','referencing','rpds-py','jsonschema-specifications','typing-extensions']:
    try: d=importlib.metadata.distribution(name)
    except importlib.metadata.PackageNotFoundError:
        packages.append({'name':name,'available':False});continue
    files=[]
    for item in sorted(d.files or [],key=str):
        p=pathlib.Path(d.locate_file(item))
        if p.is_file() and p.suffix not in ('.pyc','.pyo'):
            b=p.read_bytes();files.append({'path':str(p),'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()})
    packages.append({'name':name,'version':d.version,'available':True,'files':files})
p=pathlib.Path(sys.executable).resolve()
print(json.dumps({'python':sys.version,'executable':sys.executable,'resolved_executable':str(p),'executable_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'packages':packages}))
'''
    runtime = subprocess.run([str(python), "-I", "-B", "-c", probe], capture_output=True, check=False)
    require(runtime.returncode == 0, "existing runtime introspection failed: " + runtime.stderr.decode())
    environment = json.loads(runtime.stdout)
    output.mkdir(parents=True)
    write_json(output / "runtime.json", environment)
    workspace = base
    if replacements:
        workspace = output / "workspace"
        shutil.copytree(base, workspace)
        for relative, body in replacements.items():
            (workspace / relative).write_bytes(body)
    before_run = inventory(workspace)
    command = [str(python), "-I", "-B", str(workspace / VALIDATOR), "validate", str(workspace)]
    if replacements:
        command += ["--previous", str(base / PATHS[0])]
    result = subprocess.run(command, cwd=workspace, capture_output=True, check=False)
    (output / "stdout.log").write_bytes(result.stdout)
    (output / "stderr.log").write_bytes(result.stderr)
    require(inventory(workspace) == before_run and inventory(base) == expected, "validator mutated a fixture")
    diagnostics = result.stderr.decode().splitlines()
    baseline_receipt = HERE / "execution/baseline-v01/receipt.json"
    baseline_diagnostics = json.loads(baseline_receipt.read_bytes())["diagnostics"] if replacements and baseline_receipt.is_file() else []
    missing_jsonschema = not next(row["available"] for row in environment["packages"] if row["name"] == "jsonschema")
    status = "PASS" if result.returncode == 0 else ("ENVIRONMENT_BLOCKED" if missing_jsonschema else "CANONICAL_VALIDATOR_FAILED")
    receipt = {"schema": "m3-current-map-canonical-validation/2", "status": status, "command": command,
               "exit_code": result.returncode, "commit": CURRENT, "root_tree": CURRENT_ROOT,
               "mode": "ACTUAL_FOUR_AFTERIMAGES" if replacements else "EXACT_CURRENT_BASELINE",
               "fixture_manifest_sha256": sha(manifest_bytes), "inputs": before_run,
               "validator_sha256": before_run[VALIDATOR], "schema_sha256": before_run[SCHEMA],
               "mapping_sha256": before_run[PATHS[0]], "runtime_manifest_sha256": sha((output / "runtime.json").read_bytes()),
               "stdout_sha256": sha(result.stdout), "stderr_sha256": sha(result.stderr),
               "diagnostics": diagnostics, "fixture_unchanged": True, "actual_source_binding": binding,
               "baseline_diagnostics": baseline_diagnostics,
               "new_diagnostics_relative_to_baseline": [line for line in diagnostics if line not in baseline_diagnostics] if replacements else None,
               "removed_baseline_diagnostics": [line for line in baseline_diagnostics if line not in diagnostics] if replacements else None,
               "source_export_admitted": False, "destination_gates_passed": False,
               "scope": "Unchanged canonical validator and schema. Current failures remain failures. No map/status/source/test changes or historical command reruns."}
    write_json(output / "receipt.json", receipt)
    print(json.dumps({"status": status, "exit_code": result.returncode, "mode": receipt["mode"],
                      "diagnostics": diagnostics, "receipt": str(output / "receipt.json")}))
    return result.returncode


if __name__ == "__main__":
    raise SystemExit(main())
