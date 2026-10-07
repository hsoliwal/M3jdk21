#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Compare actual four-file recipe results with sealed installer behavior.

The caller supplies a staged repository and actual OpenRewrite Result directory.
All mutations occur in a newly created, isolated fixture. Source/runtime admission
is not inferred from these metadata application, rollback, and refusal checks.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import traceback

CRATE = "jcc-merge-recovery-20261006-mapping-v04"
RESOURCE = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/" + CRATE
TASK = "m3/tooling/migration-recipes/tasks/jcc-merge-recovery-20261006"
INSTALLER = "m3/migration/recipe.py"
INSTALLER_SHA256 = "453d976a5f29526432be138b357d3f52f868fcc40a4ee697d969cc0836359628"
TARGETS = {"m3/docs/name-mapping.json", "m3/docs/jcc-source-handoff.md",
           "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
           "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv"}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def tree(root):
    return {str(path.relative_to(root)): digest(path.read_bytes())
            for path in sorted(root.rglob("*")) if path.is_file()}


def write(path, body):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(body)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--materialized", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    repository = args.repository.resolve(strict=True)
    materialized = args.materialized.resolve(strict=True)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    started = datetime.now(timezone.utc).isoformat()
    status = "FAIL"
    checks, commands, refusals = [], [], []
    inputs = {}
    try:
        installer_path = repository / INSTALLER
        assert digest(installer_path.read_bytes()) == INSTALLER_SHA256
        spec = importlib.util.spec_from_file_location("existing_m3_sealed_installer", installer_path)
        installer = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(installer)
        plan_path = repository / RESOURCE / "plan.json"
        plan, rows = installer.sealed_plan(plan_path)
        assert len(rows) == 4 and {row["path"] for row in rows} == TARGETS
        assert len(plan["guards"]) == 7 and all(row["before"] is not None for row in rows)
        publication_path = repository / TASK / "source-publication-input-v04.json"
        publication = json.loads(publication_path.read_bytes())
        assert publication["fixture"] is False and publication["publication_state"] == "ACTUAL_GITHUB_READBACK_VERIFIED"
        assert plan["source_commit"] == publication["commit"]
        assert publication["source_export_admitted"] is False and publication["destination_gates_passed"] is False
        epoch_path = repository / TASK / "mapping-input-epoch-v04.json"
        epoch = json.loads(epoch_path.read_bytes())
        assert epoch["commit"] == plan["destination_base"]
        assert epoch["shape"] == {"ordered_records": 55, "non_jcc_records": 53, "top_level_mappings": 34, "gates": 20}
        actual_files = tree(materialized)
        assert set(actual_files) == TARGETS
        for row in rows:
            actual = (materialized / row["path"]).read_bytes()
            assert actual == row["after"], "actual recipe result differs from sealed afterimage: " + row["path"]
        inputs = {str(path.relative_to(repository)): digest(path.read_bytes())
                  for path in (repository / RESOURCE).rglob("*") if path.is_file()}
        for path in [installer_path, publication_path, epoch_path]:
            inputs[str(path.relative_to(repository))] = digest(path.read_bytes())
        fixture = output / "fixture"
        fixture.mkdir()
        repository_states = []
        for row in rows:
            observed = (repository / row["path"]).read_bytes()
            assert observed in (row["before"], row["after"]), "repository target drift: " + row["path"]
            repository_states.append("before" if observed == row["before"] else "after")
            inputs[row["path"]] = digest(observed)
            write(fixture / row["path"], row["before"])
        assert len(set(repository_states)) == 1, "repository has mixed pre/post images"
        for guard in plan["guards"]:
            body = (repository / guard["path"]).read_bytes()
            assert digest(body) == guard["sha256"]
            inputs[guard["path"]] = digest(body)
            write(fixture / guard["path"], body)
        write(fixture / "unrelated-sentinel.txt", b"unrelated fixture body remains unchanged\n")
        before_tree = tree(fixture)
        checks.append({"case": "actual recipe result equals all four sealed afterimages", "outputs": 4,
                       "source_commit": publication["commit"], "destination_input": epoch["commit"],
                       "repository_state": repository_states[0], "fixture_start": "exact sealed beforeimages"})

        def cli(label, mode):
            command = [sys.executable, "-B", str(installer_path), mode, "--root", str(fixture), "--plan", str(plan_path)]
            result = subprocess.run(command, cwd=repository, capture_output=True, check=False)
            (output / (label + ".stdout.log")).write_bytes(result.stdout)
            (output / (label + ".stderr.log")).write_bytes(result.stderr)
            commands.append({"case": label, "command": command, "exit_code": result.returncode,
                             "stdout_sha256": digest(result.stdout), "stderr_sha256": digest(result.stderr)})
            assert result.returncode == 0, label
            return json.loads(result.stdout)

        def refuse(label, mode):
            snapshot = tree(fixture)
            try:
                installer.execute(plan_path, fixture, mode)
            except installer.Refusal as failure:
                refusals.append({"case": label, "mode": mode, "message": str(failure)})
            else:
                raise AssertionError("expected refusal: " + label)
            assert tree(fixture) == snapshot, "refusal mutated caller state: " + label

        assert cli("01-check-before", "check")["state"] == "before"
        assert cli("02-apply", "apply")["writes"] == 4
        assert cli("03-apply-fixed-point", "apply")["writes"] == 0
        assert cli("04-check-after", "check")["state"] == "after"
        for row in rows:
            assert (fixture / row["path"]).read_bytes() == (materialized / row["path"]).read_bytes()
            write(fixture / row["path"], b"foreign bytes in after state\n")
            refuse("foreign afterimage " + row["path"], "rollback")
            write(fixture / row["path"], row["after"])
        assert cli("05-rollback", "rollback")["writes"] == 4
        assert cli("06-rollback-fixed-point", "rollback")["writes"] == 0
        assert tree(fixture) == before_tree
        checks.append({"case": "CLI apply/check/fixed-point/rollback restores exact input", "outputs": 4})
        for row in rows:
            write(fixture / row["path"], b"foreign bytes in before state\n")
            refuse("foreign beforeimage " + row["path"], "apply")
            (fixture / row["path"]).unlink()
            refuse("missing beforeimage " + row["path"], "apply")
            write(fixture / row["path"], row["before"])
        for guard in plan["guards"]:
            path = fixture / guard["path"]
            original = path.read_bytes()
            write(path, b"foreign canonical guard\n")
            refuse("foreign guard " + guard["path"], "apply")
            path.unlink()
            refuse("missing guard " + guard["path"], "apply")
            write(path, original)
        write(fixture / rows[0]["path"], rows[0]["after"])
        refuse("mixed before/after state", "apply")
        refuse("mixed before/after state", "rollback")
        write(fixture / rows[0]["path"], rows[0]["before"])
        assert tree(fixture) == before_tree
        assert len(refusals) == 28
        checks.append({"case": "all target/guard/mixed-state refusals preserve caller bytes", "refusals": len(refusals)})
        assert all(digest((repository / path).read_bytes()) == expected for path, expected in inputs.items())
        assert tree(materialized) == actual_files
        checks.append({"case": "staged inputs and actual recipe results unchanged", "input_files": len(inputs), "outputs": 4})
        status = "PASS"
    except BaseException:
        (output / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        receipt = {"schema": "m3-jcc-mapping-installer-verification/2", "status": status,
                   "started": started, "finished": datetime.now(timezone.utc).isoformat(),
                   "repository": str(repository), "materialized": str(materialized),
                   "checks": checks, "commands": commands, "refusals": refusals, "input_hashes": inputs,
                   "scope": "Four metadata outputs only; actual recipe bytes agree with unchanged sealed installer. Source/export/JDK/JNI/native/platform gates retain their own status."}
        (output / "receipt.json").write_text(json.dumps(receipt, indent=2, sort_keys=True) + "\n")
    print(json.dumps({"status": status, "checks": len(checks), "refusals": len(refusals), "receipt": str(output / "receipt.json")}))


if __name__ == "__main__":
    main()
