"""Run the retained E3 owners against frozen actual resources and save exact receipts."""
from __future__ import annotations

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parent
ROOT = BASE / "overlay"
OUT = BASE / "verification"
CRATE = ROOT / "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/jcc-source-final-handoff-20261005"
PLAN = CRATE / "plan.json"
PYTHON = Path(os.environ.get("CODEX_PRIMARY_RUNTIME_PYTHON", "/opt/codex/runtimes/codex-primary-runtime/dependencies/python/bin/python3"))
JDK = Path("/workspace/scratch/877d9513c002/toolchains/jdk-21.0.2")
MAVEN = Path("/workspace/scratch/877d9513c002/toolchains/apache-maven-3.9.12/bin/mvn")
M2 = Path("/workspace/scratch/1c68df1bae79/javac-convergence-20261005/m2")


def identity(path):
    content = path.read_bytes()
    return {"path": str(path), "bytes": len(content), "sha256": hashlib.sha256(content).hexdigest(),
            "git_blob_sha1": hashlib.sha1(f"blob {len(content)}\0".encode() + content).hexdigest(),
            "mode": oct(path.stat().st_mode & 0o777)}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n")


def now():
    return datetime.now(timezone.utc).isoformat()


def main():
    assert not OUT.exists(), "Use a distinct execution epoch; do not overwrite prior receipts"
    OUT.mkdir()
    plan = json.loads(PLAN.read_text())
    mutated = {str(ROOT / row["path"]) for row in plan["outputs"]}
    fixed_paths = sorted(path for path in ROOT.rglob("*") if path.is_file() and "target" not in path.parts and str(path) not in mutated)
    inventory = json.loads((BASE / "source-binding-review/SOURCE_FIELDS_AND_PROOFS.json").read_text())
    external = {Path(row["local_path"]) for group in ("production_changes", "retained_mapped_sources", "retained_contract_sources", "proof_references") for row in inventory[group]}
    inputs = {"frozen_at_utc": now(), "plan_sha256": plan["plan_sha256"],
              "fixed_inputs": [identity(path) for path in fixed_paths],
              "external_source_and_proof_inputs": [identity(path) for path in sorted(external)],
              "operational_preimages": [identity(ROOT / row["path"]) for row in plan["outputs"]]}
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
    out, _ = run("01-check-E2-preimages", [PYTHON, installer, "check", *installer_args])
    assert json.loads(out)["state"] == "before"
    pom = ROOT / "m3/tooling/migration-recipes/verification/jcc-source-final-handoff/pom.xml"
    run("02-actual-text-recipe-maven", [MAVEN, "-B", "-ntp", "-o", "-s", settings,
                                       "-Dmaven.repo.local=" + str(M2), "-f", pom, "clean", "verify"])
    xml_path = pom.parent / "target/surefire-reports/TEST-com.m3.rewrite.backport.JccSourceFinalHandoffTest.xml"
    suite = ET.fromstring(xml_path.read_bytes())
    assert {key: suite.attrib[key] for key in ("tests", "failures", "errors", "skipped")} == {
        "tests": "11", "failures": "0", "errors": "0", "skipped": "0"}
    (OUT / "JccSourceFinalHandoffTest.xml").write_bytes(xml_path.read_bytes())
    run("03-actual-packet-contract", [PYTHON, "-B", ROOT / "m3/migration/test/test_jcc_source_final_handoff_packet.py"])
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
        "scope": "Actual additive E3 metadata recipe/installer validation; no source-export, receiver-behavior rerun, full-module coverage or JDK/JNI acceptance.",
        "source_publication_commit": "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906",
        "destination_preimage_commit": "df06cdee5a8526f573b3ad89622824d0b49e2f25",
        "plan_sha256": plan["plan_sha256"], "runs": runs,
        "inputs": identity(OUT / "inputs.json"), "all_fixed_inputs_unchanged": True,
        "four_outputs_equal_sealed_afterimages": True,
        "java_suite": {key: suite.attrib[key] for key in ("name", "tests", "failures", "errors", "skipped", "time")},
        "java_xml": identity(OUT / "JccSourceFinalHandoffTest.xml"),
        "python_tests": 6, "python_refusal_checks": 132,
        "python_count_basis": "Successful six-test actual packet contract with required four outputs/seven guards: (8 preimage cases +14 guard cases +14 mixed cases +4 changed postimages +4 missing postimages) times three modes.",
        "source_export_admitted": False, "owning_module_gate": "NOT_EXECUTED; preserved E2 acquisition/frontier and 99% gate remain",
        "receiver_behavior_rerun": False, "jdk_acceptance": False, "remote_writes": False,
        "environment_scope": "Explicit PATH/JAVA_HOME/UTF-8/Python dependency path; existing HOME/TMPDIR preserved if present. No implicit Java/Maven options inherited; empty explicit user settings and offline repository.",
        "toolchain": {"jdk": str(JDK), "maven": str(MAVEN), "offline_repository": str(M2), "python": str(PYTHON)},
    }
    write(OUT / "receipt.json", receipt)
    print(json.dumps({"result": "PASS_WITH_COMPLETION_BLOCKED", "java_tests": 11, "python_tests": 6,
                      "refusal_checks": 132, "receipt_sha256": identity(OUT / "receipt.json")["sha256"]}), flush=True)


if __name__ == "__main__":
    main()
