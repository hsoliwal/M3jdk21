#!/usr/bin/env python3
"""Run the existing full gate on the actual emitted receiver with exact input parity."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import time

from run_text_owner_gate import WORK, digest, write_json


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--receiver", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    receiver = args.receiver.resolve()
    out = args.out.resolve()
    receipts = out.with_name(out.name + "-receipts")
    receipts.mkdir(parents=True, exist_ok=False)
    if out.exists():
        raise AssertionError("Fresh output required")
    correction = WORK / "source-lock-correction"
    candidate_root = correction / "candidate-receiver"
    candidate_out = correction / "combined-candidate-001"
    prior = json.loads((correction / "combined-candidate-001-receipts/RESULT.json").read_text())
    if prior["state"] != "PASS":
        raise AssertionError("Candidate is not qualified")
    handoff = json.loads((correction / "HANDOFF.json").read_text())
    for row in handoff:
        if digest((receiver / row["target_path"]).read_bytes()) != row["after_sha256"]:
            raise AssertionError("Actual emitted target postimage mismatch: " + row["target_path"])
    previous_inputs = json.loads((correction / "combined-candidate-001-receipts/INPUTS.json").read_text())
    def remap(path: str) -> str:
        if path == str(candidate_root) or path.startswith(str(candidate_root) + os.sep):
            return str(receiver) + path[len(str(candidate_root)):]
        return path
    expected = {remap(path): facts for path, facts in previous_inputs.items()}
    expected[str(Path(__file__).resolve())] = {"bytes": Path(__file__).stat().st_size,
                                             "sha256": digest(Path(__file__).read_bytes())}
    def snapshot():
        return {value: {"bytes": Path(value).stat().st_size,
                        "sha256": digest(Path(value).read_bytes())}
                for value in sorted(expected)}
    before = snapshot()
    if before != expected:
        failures = [p for p in expected if expected[p] != before.get(p)]
        raise AssertionError("Actual receiver differs from qualified candidate inputs: " + repr(failures))
    write_json(receipts / "INPUTS.json", before)
    command = [remap(value) for value in prior["command"]]
    command[command.index("--out") + 1] = str(out)
    env = dict(os.environ, **prior["environment"])
    result = {"schema": "M3_NLP_COMBINED_ACTUAL_RECEIVER_GATE_1", "state": "RUNNING",
              "receiver": str(receiver), "receiver_historical_base": prior["receiver_base"],
              "emitted_target_seals": handoff, "command": command,
              "environment": prior["environment"], "input_count": len(before),
              "inputs_sha256": digest((receipts / "INPUTS.json").read_bytes()),
              "all_inputs_match_qualified_candidate": True,
              "source_lock_sha256": prior["source_lock_sha256"],
              "driver_sha256": prior["driver_sha256"], "driver_unchanged": True,
              "scope": "Actual emitted two-file target and complete unchanged current_forks.py Java21 source/JMOD/image gate.",
              "limits": ["Java21 staged companion pins are distinct from original donor-basis commits and Java25 full upstream lane.",
                         "External dependency archives are hash locked, not source built by this proof.",
                         "No full String/HotSpot/JDK family, platform, optional Faiss native or performance admission.",
                         "Original historical BigString template mismatch remains recorded."]}
    write_json(receipts / "RESULT.json", result)
    begin = time.monotonic()
    log = receipts / "command.log"
    with log.open("w") as handle:
        process = subprocess.run(command, cwd=receiver, env=env, stdout=handle,
                                 stderr=subprocess.STDOUT, check=False, timeout=600)
    after = snapshot()
    write_json(receipts / "READBACK.json", after)
    result.update(exit_code=process.returncode, elapsed_seconds=round(time.monotonic() - begin, 3),
                  inputs_unchanged=before == after,
                  readback_sha256=digest((receipts / "READBACK.json").read_bytes()),
                  log_bytes=log.stat().st_size, log_sha256=digest(log.read_bytes()))
    report = out / "BUILD_REPORT.json"
    if report.exists():
        value = json.loads(report.read_text())
        candidate_value = json.loads((candidate_out / "BUILD_REPORT.json").read_text())
        result.update(build_report_sha256=digest(report.read_bytes()),
                      module_count=len(value["module_jars"]), tests=value["tests"],
                      all_module_jar_hashes_equal_candidate=value["module_jars"] == candidate_value["module_jars"])
        owners = ("com.m3.text", "jdk.m3.compiletime", "jdk.m3.nlp.search")
        result["m3_owner_jar_hashes_equal_candidate"] = all(value["module_jars"][m] == candidate_value["module_jars"][m] for m in owners)
        artifact_paths = [*sorted((out / "jars").glob("*.jar")),
                          *sorted((out / "pack/jmods").glob("*.jmod")),
                          out / "pack/image/bin/java", out / "pack/image/lib/modules", out / "pack/image/release"]
        artifacts = [{"path": str(p.relative_to(out)), "bytes": p.stat().st_size,
                      "sha256": digest(p.read_bytes())} for p in artifact_paths]
        write_json(receipts / "INSTALLED_ARTIFACTS.json", artifacts)
        result["artifact_manifest_sha256"] = digest((receipts / "INSTALLED_ARTIFACTS.json").read_bytes())
    clean = {name: subprocess.check_output(["git", "status", "--porcelain"], cwd=WORK / "forks" / name, text=True) == ""
             for name in ("opennlp", "lucene")}
    result["forks_tracked_clean"] = clean
    result["state"] = "PASS" if process.returncode == 0 and before == after and all(clean.values()) and report.exists() and result.get("m3_owner_jar_hashes_equal_candidate") else "FAIL"
    write_json(receipts / "RESULT.json", result)
    print(json.dumps({k: v for k, v in result.items() if k not in ("command", "environment", "emitted_target_seals")}, indent=2))
    raise SystemExit(0 if result["state"] == "PASS" else 1)


if __name__ == "__main__":
    main()
