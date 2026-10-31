#!/usr/bin/env python3
"""Verify the explicit-constructor atom and the unchanged whole-module rope gate."""
from __future__ import annotations

import json
import os
from pathlib import Path
import subprocess
import time

from run_text_owner_gate import JDK, ROOT, WORK, digest, inputs, write_json


def main() -> None:
    base = WORK / "source-lock-correction"
    candidate = base / "candidate-receiver"
    out = base / "builder-and-whole-rope-001"
    out.mkdir(exist_ok=False)
    owner = "m3/core/src/com/m3/text/SharedLexiconPrecomputeCatalog.java"
    dependency = "m3/core/src/com/m3/text/M3LexiconPrecompute.java"
    test = "m3/core/test/M3LexiconPrecomputeTest.java"
    sources = [dependency, owner, test]
    baseline_inputs = inputs(sources + ["m3/run-rope-proof.sh", "m3/core/test/M3RopeTextIndexTest.java"])
    core_paths = sorted(str(p.relative_to(candidate)) for p in (candidate / "m3/core/src").rglob("*.java"))
    candidate_paths = sorted(set(core_paths + sources + ["m3/run-rope-proof.sh", "m3/core/test/M3RopeTextIndexTest.java"]))
    def snapshot():
        return [{"path": p, "bytes": (candidate / p).stat().st_size,
                 "sha256": digest((candidate / p).read_bytes())} for p in candidate_paths]
    before = snapshot()
    write_json(out / "BASELINE_INPUTS.json", baseline_inputs)
    write_json(out / "CANDIDATE_INPUTS.json", before)
    result = {"schema": "M3_NLP_BUILDER_CONSTRUCTOR_QUALIFICATION_1", "state": "RUNNING",
              "commands": [], "scope": "Only explicitly declares the existing public no-arg Builder constructor; original whole-module lint gate retained.",
              "driver_sha256": digest(Path(__file__).read_bytes()),
              "source_closure": len(core_paths)}
    env = dict(os.environ, M3_JDK=str(JDK), JAVA_HOME=str(JDK))
    begin = time.monotonic()
    def command(label, argv, root):
        log = out / (label + ".log")
        start = time.monotonic()
        with log.open("w") as handle:
            process = subprocess.run(argv, cwd=root, env=env, stdout=handle,
                                     stderr=subprocess.STDOUT, check=False)
        data = log.read_bytes()
        result["commands"].append({"label": label, "argv": argv, "cwd": str(root),
                                  "exit": process.returncode,
                                  "elapsed_seconds": round(time.monotonic() - start, 3),
                                  "log": str(log.relative_to(WORK)),
                                  "log_bytes": len(data), "log_sha256": digest(data)})
        write_json(out / "RESULT.json", result)
        if process.returncode:
            result["state"] = "FAIL"
            write_json(out / "RESULT.json", result)
            raise SystemExit(process.returncode)
        return data
    class_dirs = {}
    for mode, source_root in [("baseline", ROOT), ("candidate", candidate)]:
        classes = out / (mode + "-classes")
        classes.mkdir()
        class_dirs[mode] = classes
        command(mode + "-strict-compile", [str(JDK / "bin/javac"), "--release", "21", "-Xlint:all", "-Werror", "-d", str(classes), *sources], source_root)
        command(mode + "-owning-test", [str(JDK / "bin/java"), "-ea", "-esa", "-cp", str(classes), "M3LexiconPrecomputeTest"], source_root)
    classes = sorted(str(p.relative_to(class_dirs["baseline"]))[:-6].replace("/", ".")
                     for p in class_dirs["baseline"].rglob("SharedLexiconPrecomputeCatalog*.class"))
    before_api = command("baseline-public-api", [str(JDK / "bin/javap"), "-public", "-s", "-classpath", str(class_dirs["baseline"]), *classes], ROOT)
    after_api = command("candidate-public-api", [str(JDK / "bin/javap"), "-public", "-s", "-classpath", str(class_dirs["candidate"]), *classes], candidate)
    if before_api != after_api:
        raise AssertionError("Public API descriptors changed")
    constructor = "com.m3.text.SharedLexiconPrecomputeCatalog$Builder"
    before_code = command("baseline-builder-bytecode", [str(JDK / "bin/javap"), "-c", "-p", "-s", "-classpath", str(class_dirs["baseline"]), constructor], ROOT)
    after_code = command("candidate-builder-bytecode", [str(JDK / "bin/javap"), "-c", "-p", "-s", "-classpath", str(class_dirs["candidate"]), constructor], candidate)
    if before_code != after_code:
        raise AssertionError("Builder executable bytecode changed")
    command("original-whole-module-rope", ["bash", "m3/run-rope-proof.sh"], candidate)
    after = snapshot()
    write_json(out / "CANDIDATE_READBACK.json", after)
    if before != after:
        raise AssertionError("Candidate source inputs changed")
    if inputs(sources + ["m3/run-rope-proof.sh", "m3/core/test/M3RopeTextIndexTest.java"]) != baseline_inputs:
        raise AssertionError("Baseline source inputs changed")
    class_rows = [{"path": str(p.relative_to(class_dirs["candidate"])),
                   "bytes": p.stat().st_size, "sha256": digest(p.read_bytes())}
                  for p in sorted(class_dirs["candidate"].rglob("*.class"))]
    result.update(state="PASS", public_api_equal=True, public_class_count=len(classes),
                  builder_all_executable_bytecode_equal=True, inputs_unchanged=True,
                  classpath=str(class_dirs["candidate"]), classes=class_rows,
                  elapsed_seconds=round(time.monotonic() - begin, 3),
                  declared_change="Implicit public no-arg constructor becomes an explicit public no-arg constructor; debug source line tables can differ.")
    write_json(out / "RESULT.json", result)
    print(json.dumps({k: result[k] for k in ("state", "public_class_count", "public_api_equal", "builder_all_executable_bytecode_equal", "elapsed_seconds", "classpath")}))


if __name__ == "__main__":
    main()
