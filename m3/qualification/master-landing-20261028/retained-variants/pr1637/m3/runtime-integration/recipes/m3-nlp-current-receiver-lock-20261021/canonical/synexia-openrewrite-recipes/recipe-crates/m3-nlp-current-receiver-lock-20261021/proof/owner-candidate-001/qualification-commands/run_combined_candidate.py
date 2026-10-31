#!/usr/bin/env python3
"""Run the unchanged full current-forks receiver against the isolated candidate."""
from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import sys
import time

from run_text_owner_gate import JDK, ROOT, WORK, digest, write_json


def main() -> None:
    correction = WORK / "source-lock-correction"
    root = correction / "candidate-receiver"
    out = correction / "combined-candidate-001"
    receipts = correction / "combined-candidate-001-receipts"
    receipts.mkdir(exist_ok=False)
    if out.exists():
        raise AssertionError("Fresh output required")
    for receipt in ("bigstring-owner-001", "gvas-owner-002", "builder-and-whole-rope-001"):
        if json.loads((correction / receipt / "RESULT.json").read_text())["state"] != "PASS":
            raise AssertionError("Missing prerequisite: " + receipt)
    for fork, expected in [("opennlp", "8b647d1d389736952e43fb46865c874747f7acde"),
                           ("lucene", "873b6167603c69101d626ddffd8d733340300fea")]:
        source = WORK / "forks" / fork
        if subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=source, text=True).strip() != expected:
            raise AssertionError("Fork pin mismatch")
        if subprocess.check_output(["git", "status", "--porcelain"], cwd=source, text=True):
            raise AssertionError("Fork source is not clean")
    driver = root / "m3/tooling/module-packs/nlp-search/current_forks.py"
    original_driver = ROOT / driver.relative_to(root)
    if driver.read_bytes() != original_driver.read_bytes():
        raise AssertionError("Combined source/build gate changed")
    recipe = ROOT / "m3/vendor/synexia/synexia-openrewrite-recipes"
    crate = recipe / "crates/m3-nlp-search/current-forks-20261018"
    inputs = set()
    for name in ("OPENNLP_OWNER_001_INPUTS.json", "LUCENE_OWNER_001_INPUTS.json"):
        prior = json.loads((WORK / "evidence" / name).read_text())
        for value, expected in prior.items():
            path = Path(value)
            if path.stat().st_size != expected["bytes"] or digest(path.read_bytes()) != expected["sha256"]:
                raise AssertionError("Previously qualified input changed: " + value)
            inputs.add(path)
    lock_path = driver.parent / "current-source-lock.json"
    lock = json.loads(lock_path.read_text())
    inputs.update(root / relative for entry in lock["modules"].values() for relative in entry["files"])
    inputs.update(p for p in driver.parent.rglob("*") if p.is_file() and "__pycache__" not in p.parts)
    inputs.update((root / "m3/tooling/jmod-recipes/tools").glob("*.py"))
    inputs.add(Path(__file__).resolve())
    def snapshot():
        return {str(path): {"bytes": path.stat().st_size, "sha256": digest(path.read_bytes())}
                for path in sorted(inputs)}
    before = snapshot()
    write_json(receipts / "INPUTS.json", before)
    command = [sys.executable, "-B", str(driver), "--receiver", str(root),
               "--jdk-home", str(JDK), "--out", str(out),
               "--opennlp-source", str(WORK / "forks/opennlp"),
               "--lucene-source", str(WORK / "forks/lucene"),
               "--recipe-runner", str(crate / "run_recipe.py"),
               "--recipe-resources", str(recipe / "src/main/resources"),
               "--opennlp-dependencies", str(WORK / "dependencies/opennlp"),
               "--lucene-dependencies", str(WORK / "dependencies/lucene")]
    env = dict(os.environ, JAVA_HOME=str(JDK),
               JAVA_TOOL_OPTIONS="-Xmx512m -XX:ActiveProcessorCount=2 -XX:-UsePerfData",
               PYTHONDONTWRITEBYTECODE="1")
    result = {"schema": "M3_NLP_COMBINED_CANDIDATE_GATE_1", "state": "RUNNING",
              "command": command, "environment": {key: env[key] for key in ("JAVA_HOME", "JAVA_TOOL_OPTIONS", "PYTHONDONTWRITEBYTECODE")},
              "receiver_base": "2779aba3f98f31c324be43f731be7914e1a05935",
              "source_lock_sha256": digest(lock_path.read_bytes()),
              "driver_sha256": digest(driver.read_bytes()), "driver_unchanged": True,
              "input_count": len(before), "inputs_sha256": digest((receipts / "INPUTS.json").read_bytes()),
              "scope": "Existing full Java21 combined source/JMOD/image gate, isolated two-file candidate; no product application yet.",
              "runtime_dependency_resolution": "Exact prequalified SHA-locked dependency directories; original source build gates invoke no Maven or network dependency fetch.",
              "historical_bigstring_template_mismatch": "Retained separately; this gate binds the actual received source pinned by its manifest/workflow, not the differing original canonical head template."}
    write_json(receipts / "RESULT.json", result)
    begin = time.monotonic()
    log = receipts / "command.log"
    with log.open("w") as handle:
        process = subprocess.run(command, cwd=root, env=env, stdout=handle,
                                 stderr=subprocess.STDOUT, check=False, timeout=600)
    after = snapshot()
    write_json(receipts / "READBACK.json", after)
    result.update(exit_code=process.returncode, elapsed_seconds=round(time.monotonic() - begin, 3),
                  inputs_unchanged=before == after, readback_sha256=digest((receipts / "READBACK.json").read_bytes()),
                  log_sha256=digest(log.read_bytes()), log_bytes=log.stat().st_size)
    report = out / "BUILD_REPORT.json"
    if report.exists():
        value = json.loads(report.read_text())
        result.update(build_report_sha256=digest(report.read_bytes()),
                      module_count=len(value["module_jars"]), tests=value["tests"])
    clean = {name: subprocess.check_output(["git", "status", "--porcelain"], cwd=WORK / "forks" / name, text=True) == ""
             for name in ("opennlp", "lucene")}
    result["forks_tracked_clean"] = clean
    result["state"] = "PASS" if process.returncode == 0 and before == after and all(clean.values()) and report.exists() else "FAIL"
    write_json(receipts / "RESULT.json", result)
    print(json.dumps({k: v for k, v in result.items() if k not in ("command", "environment")}, indent=2))
    raise SystemExit(0 if result["state"] == "PASS" else 1)


if __name__ == "__main__":
    main()
