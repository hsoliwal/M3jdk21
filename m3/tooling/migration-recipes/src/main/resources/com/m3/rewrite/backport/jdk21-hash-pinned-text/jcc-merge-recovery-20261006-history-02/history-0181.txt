"""Run the retained E4 owners against frozen actual resources and save exact receipts."""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import stat
import subprocess
import sys
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parent
ROOT = BASE / "overlay"
OUT = BASE / "verification-v3"
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-recovery-handoff-20261005"
PLAN = CRATE / "plan.json"
PYTHON = Path(os.environ.get("CODEX_PRIMARY_RUNTIME_PYTHON", "/opt/codex/runtimes/codex-primary-runtime/dependencies/python/bin/python3"))
JDK = Path("/workspace/scratch/877d9513c002/toolchains/jdk-21.0.2")
MAVEN = Path("/workspace/scratch/877d9513c002/toolchains/apache-maven-3.9.12/bin/mvn")
M2 = Path("/workspace/scratch/1c68df1bae79/javac-convergence-20261005/m2")
CONTEXT_ROOT = BASE.parents[2] / "recovery-baseline-be92"


def identity(path):
    content = path.read_bytes()
    return {"path": str(path), "bytes": len(content), "sha256": hashlib.sha256(content).hexdigest(),
            "git_blob_sha1": hashlib.sha1(f"blob {len(content)}\0".encode() + content).hexdigest(),
            "mode": oct(path.stat().st_mode & 0o777)}


def admit_declared_file(path, row):
    assert path.is_absolute() and not path.is_symlink()
    assert path.resolve(strict=True) == path, "Symlink or noncanonical input path: " + str(path)
    assert stat.S_ISREG(path.lstat().st_mode), "Input is not a regular file: " + str(path)
    with path.open("rb") as stream:
        before = os.fstat(stream.fileno())
        assert stat.S_ISREG(before.st_mode), "Input is not a regular file: " + str(path)
        content = stream.read()
        after = os.fstat(stream.fileno())
    fields = ("st_dev", "st_ino", "st_mode", "st_size", "st_mtime_ns", "st_ctime_ns")
    assert all(getattr(before, field) == getattr(after, field) for field in fields)
    assert path.resolve(strict=True) == path
    observed = path.lstat()
    assert stat.S_ISREG(observed.st_mode)
    assert all(getattr(after, field) == getattr(observed, field) for field in fields)
    actual = {"path": str(path), "bytes": len(content),
              "sha256": hashlib.sha256(content).hexdigest(),
              "git_blob_sha1": hashlib.sha1(f"blob {len(content)}\0".encode() + content).hexdigest(),
              "mode": oct(after.st_mode & 0o777)}
    assert all(actual[key] == row[key] for key in ("bytes", "sha256", "git_blob_sha1")), str(path)
    if "mode" in row:
        assert row["mode"] in ("100644", "100755")
        assert ("100755" if after.st_mode & 0o111 else "100644") == row["mode"], str(path)
    return actual


def admit_current_context(context):
    """Compare every declared input before retaining its same-read freeze identity."""
    rows = context["current_context"]
    assert len(rows) == context["current_context_count"] == 527
    admitted = {}
    repository_paths = set()
    for row in rows:
        name = row["path"]
        relative = PurePosixPath(name)
        assert name and "\\" not in name and not relative.is_absolute() and relative.as_posix() == name
        assert all(part not in ("", ".", "..") for part in relative.parts)
        assert name not in repository_paths
        repository_paths.add(name)
        assert row["repo"] == "hsoliwal/com.synexia"
        assert row["commit"] == "be92c62ece9023b5c33676716a1076d00e26120a"
        path = Path(row["local_path"])
        assert path == CONTEXT_ROOT / relative and path not in admitted
        assert "mode" in row
        admitted[path] = admit_declared_file(path, row)
    return admitted


def admit_current_receipts(context):
    receipts = context["receipts"]
    assert len(receipts) == 10
    prefix = PurePosixPath("m3/tooling/migration-recipes/tasks/jcc-source-recovery-handoff-20261005/source-current")
    admitted = {}
    for name, row in receipts.items():
        assert name and "\\" not in name and PurePosixPath(name).name == name
        assert name not in (".", "..") and row["path"] == str(prefix / name)
        path = ROOT / row["path"]
        assert path not in admitted
        admitted[path] = admit_declared_file(path, row)
    return admitted


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def now():
    return datetime.now(timezone.utc).isoformat()


def main():
    if not __debug__:
        raise RuntimeError('Optimized Python disables required receiving proof assertions')
    assert not OUT.exists(), "Use a distinct execution epoch; do not overwrite prior receipts"
    from author_e04_v3 import validate_inputs
    from publication_custody_v3 import bound_json, EXPECTATIONS_SHA256
    final_inputs = json.loads((BASE / "FINAL_SOURCE_INPUTS_V3.json").read_text())
    source, source_root, source_ref, inventory, source_accounting, context = validate_inputs(final_inputs)
    plan = json.loads(PLAN.read_text())
    assert plan["source_commit"] == source
    assert plan["target_commit"] == "0994ecd65e86600f417f4d8702838d8c2663af61"
    admitted_context = admit_current_context(context)
    admitted_receipts = admit_current_receipts(context)
    expectations = bound_json({"local_path": str(BASE / "custody-v3-proposal/LOCAL_EXPECTATIONS.json"),
                               "sha256": EXPECTATIONS_SHA256})
    fixture_rows = expectations["exact_content_addressed_invalid_utf8_exceptions"]
    assert len(fixture_rows) == 2
    admitted_external = dict(admitted_context)
    for row in fixture_rows:
        path = Path(row["local_path"])
        assert path not in admitted_external
        admitted_external[path] = admit_declared_file(path, row)
    mutated = {str(ROOT / row["path"]) for row in plan["outputs"]}
    fixed_paths = sorted(path for path in ROOT.rglob("*") if path.is_file() and "target" not in path.parts and str(path) not in mutated)
    assert set(admitted_receipts) <= set(fixed_paths)
    external = {Path(row["local_path"]) for group in ("sources", "proof_references") for row in inventory[group]}
    external.update(admitted_external)
    external.update(Path(final_inputs[key]["local_path"]) for key in ("publication_receipt", "source_inventory", "root_accounting", "handoff_document"))
    publication = json.loads(Path(final_inputs["publication_receipt"]["local_path"]).read_text())
    external.update(BASE / name for name in ("derive_source_handoff_v3.py", "author_e04_v3.py", "run_verification_v3.py", "publication_custody_v3.py", "custody-v3-proposal/LOCAL_EXPECTATIONS.json", "source-prepublication/DESIGNATED_COMPARISON.json"))
    external.update(Path(publication[key]) for key in ("manifest_path", "readback_path"))
    external.add(Path(publication["raw_fixture_custody_input"]["local_path"]))
    external.add(Path(publication["raw_fixture_custody"]["failed_readback_observation"]["local_path"]))
    inputs = {"frozen_at_utc": now(), "plan_sha256": plan["plan_sha256"],
              "fixed_inputs": [admitted_receipts[path] if path in admitted_receipts else identity(path) for path in fixed_paths],
              "external_source_and_proof_inputs": [admitted_external[path] if path in admitted_external else identity(path) for path in sorted(external)],
              "operational_preimages": [identity(ROOT / row["path"]) for row in plan["outputs"]]}
    OUT.mkdir()
    write(OUT / "inputs.json", inputs)
    env = {"PATH": str(JDK / "bin") + ":/usr/local/bin:/usr/bin:/bin", "JAVA_HOME": str(JDK),
           "LANG": "C.UTF-8", "LC_ALL": "C.UTF-8", "PYTHONDONTWRITEBYTECODE": "1",
           "PYTHONPATH": str(BASE.parent / "python-deps")}
    for key in ("HOME", "TMPDIR"):
        if key in os.environ:
            env[key] = os.environ[key]
    settings = OUT / "empty-maven-settings.xml"
    settings.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"/>\n')
    runs = []

    def run(name, command, expected=0):
        record = {"id": name, "command": [str(part) for part in command], "cwd": str(ROOT),
                  "started_utc": now(), "expected_exit_code": expected}
        stdout, stderr = OUT / (name + ".stdout.log"), OUT / (name + ".stderr.log")
        with stdout.open("wb") as out, stderr.open("wb") as err:
            result = subprocess.run(record["command"], cwd=ROOT, env=env, stdout=out, stderr=err, check=False)
        record.update(ended_utc=now(), exit_code=result.returncode,
                      stdout=identity(stdout), stderr=identity(stderr))
        runs.append(record)
        write(OUT / (name + ".json"), record)
        print(json.dumps({"id": name, "exit_code": result.returncode, "expected": expected}), flush=True)
        assert result.returncode == expected, "Unexpected result: " + name
        return stdout.read_text(), stderr.read_text()

    installer = ROOT / "m3/migration/recipe.py"
    installer_args = ["--plan", PLAN, "--root", ROOT]
    out, _ = run("01-check-E3-preimages", [PYTHON, installer, "check", *installer_args])
    assert json.loads(out)["state"] == "before"
    pom = ROOT / "m3/tooling/migration-recipes/verification/jcc-source-recovery-handoff/pom.xml"
    run("02-actual-text-recipe-maven", [MAVEN, "-B", "-ntp", "-o", "-s", settings,
                                       "-Dmaven.repo.local=" + str(M2), "-f", pom, "clean", "verify"])
    xml_path = pom.parent / "target/surefire-reports/TEST-com.m3.rewrite.backport.JccSourceRecoveryHandoffTest.xml"
    suite = ET.fromstring(xml_path.read_bytes())
    assert {key: suite.attrib[key] for key in ("tests", "failures", "errors", "skipped")} == {
        "tests": "12", "failures": "0", "errors": "0", "skipped": "0"}
    (OUT / "JccSourceRecoveryHandoffTest.xml").write_bytes(xml_path.read_bytes())
    run("03-actual-packet-contract", [PYTHON, "-B", ROOT / "m3/migration/test/test_jcc_source_recovery_handoff_packet.py"])
    for name, mode, state, writes in (
            ("04-apply", "apply", "after", 4),
            ("05-apply-fixed-point", "apply", "after", 0),
            ("06-rollback", "rollback", "before", 4),
            ("07-replay", "apply", "after", 4),
            ("08-check-final", "check", "after", 0)):
        output, _ = run(name, [PYTHON, installer, mode, *installer_args])
        measured = json.loads(output)
        assert (measured["state"], measured["files"], measured["writes"]) == (state, 4, writes)
    validator = ROOT / "m3/migration/migration.py"
    previous = CRATE / "before00-name-mapping.json.txt"
    output, _ = run("09-whole-map-validate", [PYTHON, validator, "validate", ROOT, "--previous", previous])
    assert output.strip() == "MIGRATION_MANIFEST_VALID completion=INCOMPLETE"
    output, _ = run("10-completion-remains-blocked", [PYTHON, validator, "complete", ROOT, "--previous", previous], 2)
    assert output.strip() == "MIGRATION_MANIFEST_VALID completion=INCOMPLETE"
    for row in inputs["fixed_inputs"] + inputs["external_source_and_proof_inputs"]:
        assert identity(Path(row["path"])) == row, "Frozen input changed: " + row["path"]
    for row in plan["outputs"]:
        assert (ROOT / row["path"]).read_bytes() == (CRATE / row["after"]["resource"]).read_bytes()
    receipt = {
        "scope": "Actual additive E4 metadata recipe/installer validation; no source-export, receiver-behavior rerun, full-module coverage or JDK/JNI acceptance.",
        "source_publication_commit": source, "source_publication_root": source_root, "source_publication_ref": source_ref,
        "source_publication_custody": inventory["source_publication"]["custody"],
        "destination_preimage_commit": "0994ecd65e86600f417f4d8702838d8c2663af61",
        "plan_sha256": plan["plan_sha256"], "runs": runs,
        "inputs": identity(OUT / "inputs.json"), "all_fixed_inputs_unchanged": True,
        "four_outputs_equal_sealed_afterimages": True,
        "java_suite": {key: suite.attrib[key] for key in ("name", "tests", "failures", "errors", "skipped", "time")},
        "java_xml": identity(OUT / "JccSourceRecoveryHandoffTest.xml"),
        "python_tests": 6, "python_refusal_checks": 132,
        "python_count_basis": "Successful six-test actual packet contract with required four outputs/seven guards: (8 preimage cases +14 guard cases +14 mixed cases +4 changed postimages +4 missing postimages) times three modes.",
        "source_input_commit": "be92c62ece9023b5c33676716a1076d00e26120a",
        "source_context_files": 527, "main_membership": 446, "parent_context_files": 109,
        "source_context_declared_identities_admitted_before_freeze": True,
        "source_context_receipt_identities_admitted_before_freeze": True,
        "source_qualification_transfer": False,
        "source_export_admitted": False, "owning_module_gate": "NOT_EXECUTED; preserved E2 acquisition/frontier and 99% gate remain",
        "receiver_behavior_rerun": False, "jdk_acceptance": False, "remote_writes": False,
        "environment_scope": "Explicit PATH/JAVA_HOME/UTF-8/Python dependency path; existing HOME/TMPDIR preserved if present. No implicit Java/Maven options inherited; empty explicit user settings and offline repository.",
        "toolchain": {"jdk": str(JDK), "maven": str(MAVEN), "offline_repository": str(M2), "python": str(PYTHON)},
    }
    write(OUT / "receipt.json", receipt)
    print(json.dumps({"result": "PASS_WITH_COMPLETION_BLOCKED", "java_tests": 12, "python_tests": 6,
                      "refusal_checks": 132, "receipt_sha256": identity(OUT / "receipt.json")["sha256"]}), flush=True)


if __name__ == "__main__":
    main()
