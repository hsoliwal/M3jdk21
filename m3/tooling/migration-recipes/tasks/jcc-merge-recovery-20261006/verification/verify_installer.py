#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Verify the existing sealed installer and retention owner in isolated Git fixtures."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import sys
import traceback

RESOURCE = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text"
CRATES = ["jcc-merge-recovery-20261006-history-01", "jcc-merge-recovery-20261006-history-02",
          "jcc-merge-recovery-20261006-retention"]
CATALOGUE = "m3/history/RETAINED_CAPABILITIES.tsv"


def digest(data):
    return hashlib.sha256(data).hexdigest()


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def json_file(path, value):
    write(path, (json.dumps(value, indent=2, sort_keys=True) + "\n").encode())


def tree(root):
    return {str(path.relative_to(root)): digest(path.read_bytes())
            for path in sorted(root.rglob("*")) if path.is_file() and ".git" not in path.relative_to(root).parts}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    repository = args.repository.resolve(strict=True)
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    start = datetime.now(timezone.utc).isoformat()
    commands, checks, refusals = [], [], []
    installer = load("jcc_existing_installer", repository / "m3/migration/recipe.py")
    plans = [repository / RESOURCE / name / "plan.json" for name in CRATES]
    loaded = [installer.sealed_plan(path) for path in plans]
    inputs = {str(path.relative_to(repository)): digest(path.read_bytes())
              for base in [repository / RESOURCE / name for name in CRATES]
              for path in base.rglob("*") if path.is_file()}
    for path in ["m3/migration/recipe.py", "m3/history/git_graph.py"]:
        inputs[path] = digest((repository / path).read_bytes())
    fixture = output / "installer-fixture"
    fixture.mkdir()
    for plan, rows in loaded:
        for guard in plan["guards"]:
            body = (repository / guard["path"]).read_bytes()
            assert digest(body) == guard["sha256"], guard["path"]
            write(fixture / guard["path"], body)
        for row in rows:
            if row["before"] is not None:
                write(fixture / row["path"], row["before"])
    write(fixture / "unrelated.txt", b"preserved sentinel\n")
    before = tree(fixture)

    def run(name, args, cwd):
        result = subprocess.run(args, cwd=cwd, capture_output=True, check=False)
        stdout, stderr = output / (name + ".stdout.log"), output / (name + ".stderr.log")
        stdout.write_bytes(result.stdout)
        stderr.write_bytes(result.stderr)
        commands.append({"name": name, "args": args, "cwd": str(cwd), "exit_code": result.returncode,
                         "stdout_sha256": digest(result.stdout), "stderr_sha256": digest(result.stderr)})
        return result

    def refuse(label, action):
        snapshot = tree(fixture)
        try:
            action()
        except installer.Refusal as failure:
            refusals.append({"case": label, "message": str(failure)})
        else:
            raise AssertionError("expected installer refusal: " + label)
        assert tree(fixture) == snapshot, "refusal mutated fixture: " + label

    status = "FAIL"
    try:
        assert [len(rows) for _, rows in loaded] == [256, 187, 6]
        assert sum(len(row["after"]) for _, rows in loaded[:2] for row in rows) == 29_889_572
        assert all(row["before"] is None for _, rows in loaded[:2] for row in rows)
        checks.append({"case": "sealed resource graph", "targets": 449, "historical_targets": 443})
        for plan_path, (_, rows) in zip(plans, loaded):
            assert installer.execute(plan_path, fixture, "check")["state"] == "before"
            first = installer.execute(plan_path, fixture, "apply")
            assert first["writes"] == len(rows)
            assert installer.execute(plan_path, fixture, "apply")["writes"] == 0
            assert installer.execute(plan_path, fixture, "check")["state"] == "after"
            for row in rows:
                assert (fixture / row["path"]).read_bytes() == row["after"]
        for plan_path, (_, rows) in reversed(list(zip(plans, loaded))):
            assert installer.execute(plan_path, fixture, "rollback")["writes"] == len(rows)
            assert installer.execute(plan_path, fixture, "rollback")["writes"] == 0
        assert tree(fixture) == before
        checks.append({"case": "all apply/fixed-point/check/rollback/fixed-point", "plans": 3, "targets": 449})
        for plan_path, (plan, rows) in zip(plans, loaded):
            selected = rows if len(rows) == 6 else [rows[0], rows[-1]]
            for row in selected:
                write(fixture / row["path"], b"foreign target bytes\n")
                refuse("destination drift " + row["path"], lambda: installer.execute(plan_path, fixture, "apply"))
                if row["before"] is None:
                    (fixture / row["path"]).unlink()
                else:
                    write(fixture / row["path"], row["before"])
            for guard in plan["guards"]:
                original = (fixture / guard["path"]).read_bytes()
                write(fixture / guard["path"], b"foreign guard bytes\n")
                refuse("guard drift " + guard["path"], lambda: installer.execute(plan_path, fixture, "apply"))
                write(fixture / guard["path"], original)
            row = rows[0]
            write(fixture / row["path"], row["after"])
            refuse("mixed state " + plan["recipe_id"], lambda: installer.execute(plan_path, fixture, "apply"))
            if row["before"] is None:
                (fixture / row["path"]).unlink()
            else:
                write(fixture / row["path"], row["before"])
        assert tree(fixture) == before
        checks.append({"case": "target/guard/mixed-state refusals are non-mutating", "refusals": len(refusals)})

        # This synthetic baseline uses exact existing JEP recipe afterimages. It does not
        # assert that the actual destination JDK still has those bodies or passes its gates.
        git_fixture = output / "retention-fixture"
        shutil.copytree(fixture, git_fixture)
        for line in (git_fixture / CATALOGUE).read_text().splitlines()[1:]:
            _, manifest = line.split("\t")
            body = (repository / manifest).read_bytes()
            write(git_fixture / manifest, body)
            for line in body.decode().splitlines():
                target, _, after_hash, resource = line.split("\t")
                after = (repository / Path(manifest).parent / resource).read_bytes()
                assert digest(after) == after_hash
                write(git_fixture / target, after)
        env = dict(__import__("os").environ, GIT_CONFIG_GLOBAL="/dev/null", GIT_CONFIG_NOSYSTEM="1")
        def git(*args):
            result = subprocess.run(["git", "-C", str(git_fixture), *args], env=env,
                                    check=True, capture_output=True)
            return result.stdout.decode().strip()
        git("init", "-q")
        git("config", "user.name", "JCC isolated verification")
        git("config", "user.email", "jcc-fixture@example.invalid")
        git("add", ".")
        git("commit", "-qm", "synthetic existing JEP retention baseline")
        baseline = git("rev-parse", "HEAD")
        for plan_path in plans:
            installer.execute(plan_path, git_fixture, "apply")
        git("add", ".")
        git("commit", "-qm", "synthetic restored JCC custody")
        candidate = git("rev-parse", "HEAD")
        sys.path.insert(0, str(git_fixture / "m3/history"))
        retention = load("jcc_materialized_retention", git_fixture / "m3/history/retention.py")
        exact = retention.inspect(git_fixture, candidate, CATALOGUE,
                                  baseline_commits=(baseline,), include_parents=True)
        assert exact["targets"] == 469 and exact["retained_exact"] == 469
        assert exact["missing"] == 0 and exact["declaration_conflict_count"] == 0
        assert exact == retention.inspect(git_fixture, candidate, CATALOGUE,
                                          baseline_commits=(baseline,), include_parents=True)
        json_file(output / "retention-exact.json", exact)
        checks.append({"case": "actual catalogue integration in immutable synthetic Git baseline",
                       "targets": 469, "existing_jep_targets": 26, "historical_jcc_targets": 443,
                       "fixture_baseline_commit": baseline, "fixture_candidate_commit": candidate,
                       "retention_root": exact["root"]})

        # Candidate declaration loss cannot erase the previous commit's 443 obligations.
        (git_fixture / CATALOGUE).write_bytes((fixture / CATALOGUE).read_bytes())
        for _, rows in loaded[:2]:
            for row in rows:
                (git_fixture / row["path"]).unlink()
        git("add", "-A")
        git("commit", "-qm", "synthetic dropped JCC history and declarations")
        dropped = git("rev-parse", "HEAD")
        lost = retention.inspect(git_fixture, dropped, CATALOGUE,
                                 baseline_commits=(candidate,), include_parents=True)
        assert lost["missing"] == 443 and lost["targets"] == 469
        assert {c["kind"] for c in lost["declaration_conflicts"]} == {"CAPABILITY_REMOVED"}
        assert lost["declaration_conflict_count"] == 2
        json_file(output / "retention-dropped.json", lost)
        cli = run("retention-loss-cli", [sys.executable, "-B", str(git_fixture / "m3/history/retention.py"),
            "--repo", str(git_fixture), "--commit", dropped, "--catalogue", CATALOGUE,
            "--output", str(output / "retention-cli-report"), "--baseline-commit", candidate,
            "--include-parents", "--require-no-missing"], git_fixture)
        assert cli.returncode == 3
        checks.append({"case": "both history declarations and all443 target paths removed",
                       "missing": 443, "declaration_conflicts": 2, "cli_exit_code": 3})

        # Restore the candidate fixture, then rewrite one hash and body together.
        git("restore", "--source", candidate, "--staged", "--worktree", ".")
        first = loaded[0][1][0]
        changed = first["after"] + b"\nsynthetic foreign reseal\n"
        write(git_fixture / first["path"], changed)
        manifest = git_fixture / RESOURCE / CRATES[0] / "manifest.tsv"
        lines = manifest.read_text().splitlines()
        cells = lines[0].split("\t")
        assert cells[0] == first["path"] and cells[2] == digest(first["after"])
        cells[2] = digest(changed)
        lines[0] = "\t".join(cells)
        manifest.write_text("\n".join(lines) + "\n")
        git("add", ".")
        git("commit", "-qm", "synthetic foreign hash reseal")
        resealed = git("rev-parse", "HEAD")
        rejected = retention.inspect(git_fixture, resealed, CATALOGUE, baseline_commits=(candidate,))
        assert rejected["targets"] == 470 and rejected["present_drifted_review"] == 1
        assert {"EXPECTED_SHA256_CHANGED", "EXPECTED_SHA256_CONFLICT"}.issubset(
                {c["kind"] for c in rejected["declaration_conflicts"]})
        json_file(output / "retention-resealed.json", rejected)
        checks.append({"case": "foreign body and expected hash reseal retains previous obligation",
                       "targets": 470, "drifted_old_expectations": 1,
                       "declaration_conflicts": rejected["declaration_conflict_count"]})
        assert all(digest((repository / path).read_bytes()) == sha for path, sha in inputs.items())
        status = "PASS"
    except BaseException:
        (output / "failure.txt").write_text(traceback.format_exc())
        raise
    finally:
        receipt = {"schema": "m3-jcc-recovery-installer-retention-verification/1", "status": status,
                   "started": start, "finished": datetime.now(timezone.utc).isoformat(),
                   "repository": str(repository), "checks": checks, "refusals": refusals,
                   "commands": commands, "input_hashes": inputs,
                   "scope": "Existing Python installer and materialized retention algorithm in isolated fixtures. Maven/OpenRewrite/JDK/runtime acceptance is separate."}
        json_file(output / "receipt.json", receipt)
        print(json.dumps({"status": status, "checks": len(checks), "refusals": len(refusals),
                          "receipt": str(output / "receipt.json")}))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
