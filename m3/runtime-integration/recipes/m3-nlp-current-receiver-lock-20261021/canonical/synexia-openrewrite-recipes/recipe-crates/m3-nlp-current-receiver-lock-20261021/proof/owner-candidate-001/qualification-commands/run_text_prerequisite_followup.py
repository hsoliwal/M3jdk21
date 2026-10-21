#!/usr/bin/env python3
"""Preserve environmental retry and a finite unchanged rope fixture qualification."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import time

from run_text_owner_gate import BASE, JDK, ROOT, WORK, digest, inputs, write_json


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("owner", choices=["gvas", "rope"])
    args = parser.parse_args()
    out = WORK / "source-lock-correction" / (args.owner + "-owner-002")
    out.mkdir(parents=True, exist_ok=False)
    env = dict(os.environ, M3_JDK=str(JDK), JAVA_HOME=str(JDK))
    if args.owner == "gvas":
        previous = json.loads((out.parent / "gvas-owner-001/RESULT.json").read_text())
        paths = [row["path"] for row in previous["gate_inputs"]]
        commands = [["python3", "m3/verify-gvas-literal-precompute.py"]]
        scope = "Identical original gate after sandbox child /proc visibility refusal; runs with required process visibility."
    else:
        paths = ["m3/core/src/com/m3/text/" + name + ".java" for name in
                 ("StorageIdentity", "M3StringPiece", "LocalM3StringPiece",
                  "JoinedM3StringPiece", "LocalM3Arena", "M3RopeTextIndex")]
        paths += ["m3/core/test/M3RopeTextIndexTest.java"]
        classes = out / "classes"
        classes.mkdir()
        commands = [[str(JDK / "bin/javac"), "--release", "21", "-Xlint:all", "-Werror",
                     "-d", str(classes), *paths]]
        for flags in ([], ["-Xint"], ["-XX:-CompactStrings"], ["-Xbatch", "-XX:-TieredCompilation"]):
            commands.append([str(JDK / "bin/java"), "-ea", "-esa", *flags,
                             "-cp", str(classes), "M3RopeTextIndexTest"])
        scope = ("Unchanged existing rope fixture and exact six-source dependency closure, strict Java21 compilation and all four original JVM modes. "
                 "Original whole-module run-rope-proof.sh remains refused by unrelated SharedLexiconPrecomputeCatalog.Builder missing-explicit-ctor lint; this finite proof does not claim that gate passed.")
    before = inputs(paths)
    write_json(out / "INPUTS.json", before)
    result = {"schema": "M3_NLP_RECEIVER_TEXT_OWNER_FOLLOWUP_1", "owner": args.owner,
              "receiver_head": BASE, "scope": scope, "state": "RUNNING", "commands": [],
              "gate_inputs": before, "driver_sha256": digest(Path(__file__).read_bytes()),
              "shared_driver_sha256": digest((WORK / "run_text_owner_gate.py").read_bytes())}
    write_json(out / "RESULT.json", result)
    begin = time.monotonic()
    stdout = ""
    for index, command in enumerate(commands):
        log = out / ("command-%02d.log" % (index + 1))
        start = time.monotonic()
        with log.open("w") as handle:
            process = subprocess.run(command, cwd=ROOT, env=env, stdout=handle,
                                     stderr=subprocess.STDOUT, check=False)
        data = log.read_bytes()
        stdout += data.decode(errors="replace")
        result["commands"].append({"argv": command, "exit": process.returncode,
                                  "elapsed_seconds": round(time.monotonic() - start, 3),
                                  "log": str(log.relative_to(WORK)),
                                  "log_bytes": len(data), "log_sha256": digest(data)})
        write_json(out / "RESULT.json", result)
        if process.returncode:
            result["state"] = "FAIL"
            result["elapsed_seconds"] = round(time.monotonic() - begin, 3)
            write_json(out / "RESULT.json", result)
            raise SystemExit(process.returncode)
    after = inputs(paths)
    write_json(out / "READBACK.json", after)
    if before != after:
        raise AssertionError("Owner source inputs changed")
    result.update(state="PASS", inputs_unchanged=True, stdout=stdout,
                  elapsed_seconds=round(time.monotonic() - begin, 3))
    if args.owner == "rope":
        result["classes"] = [{"path": str(p.relative_to(classes)), "bytes": p.stat().st_size,
                              "sha256": digest(p.read_bytes())}
                             for p in sorted(classes.rglob("*.class"))]
        result["classpath"] = str(classes)
    write_json(out / "RESULT.json", result)
    print(json.dumps({"owner": args.owner, "state": result["state"], "stdout": stdout,
                      "elapsed_seconds": result["elapsed_seconds"], "result": str(out / "RESULT.json")}))


if __name__ == "__main__":
    main()
