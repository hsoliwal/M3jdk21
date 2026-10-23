#!/usr/bin/env python3
"""Record unchanged existing text-owner gates in an isolated pinned checkout."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time

import yaml

WORK = Path(__file__).resolve().parent
ROOT = WORK / "receiver-source"
JDK = Path("/tmp/m3-build-tools/jdk-21.0.12.1+1")
BASE = "2779aba3f98f31c324be43f731be7914e1a05935"


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")


def inputs(paths: list[str]) -> list[dict[str, object]]:
    rows = []
    for relative in sorted(set(paths)):
        data = (ROOT / relative).read_bytes()
        blob = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
        expected = subprocess.check_output(
            ["git", "ls-files", "--stage", "--", relative], cwd=ROOT, text=True).split()
        if len(expected) < 2 or blob != expected[1]:
            raise AssertionError("Source differs from pinned index: " + relative)
        rows.append({"path": relative, "bytes": len(data), "sha256": digest(data),
                     "git_blob": blob})
    return rows


def run() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("owner", choices=["gvas", "rope", "bigstring"])
    args = parser.parse_args()
    out = WORK / "source-lock-correction" / (args.owner + "-owner-001")
    out.mkdir(parents=True, exist_ok=False)
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    if head != BASE:
        raise AssertionError("Unexpected checkout head: " + head)
    env = dict(os.environ, M3_JDK=str(JDK), JAVA_HOME=str(JDK),
               PATH=str(JDK / "bin") + os.pathsep + os.environ.get("PATH", ""))
    commands = []
    if args.owner == "gvas":
        paths = ["m3/verify-gvas-literal-precompute.py",
                 "m3/ports/compiletime/src/main/java/com/m3/compiletime/M3GvasLiteralImageWriter.java",
                 "m3/core/src/com/m3/text/M3FixedAddressMapping.java",
                 "m3/core/src/com/m3/text/M3GvasLiteralImage.java",
                 "m3/core/test/com/m3/text/M3GvasLiteralImageContractTest.java",
                 "m3/lexicon/native/m3_fixed_address_mapping_jni.c"]
        commands = [["python3", "m3/verify-gvas-literal-precompute.py"]]
    elif args.owner == "rope":
        paths = [str(p.relative_to(ROOT)) for p in (ROOT / "m3/core/src").rglob("*.java")]
        paths += ["m3/run-rope-proof.sh", "m3/core/test/M3RopeTextIndexTest.java"]
        commands = [["bash", "m3/run-rope-proof.sh"]]
    else:
        workflow = ".github/workflows/m3-bigstring-rope-summary-20261018.yml"
        paths = ["m3/core/src/com/m3/text/" + p + ".java" for p in
                 ("StorageIdentity", "M3StringPiece", "LocalM3StringPiece",
                  "JoinedM3StringPiece", "M3BigString")]
        paths += [workflow, "m3/core/test/com/m3/text/M3BigStringSelfTest.java"]
        steps = yaml.safe_load((ROOT / workflow).read_text())["jobs"]["java21-proof"]["steps"]
        runs = [step for step in steps if "run" in step]
        if [step["name"] for step in runs] != [
                "Verify exact recipe-generated postimages",
                "Compile against the actual M3 immutable text-piece owners",
                "Run deterministic Unicode, search and long-coordinate oracle"]:
            raise AssertionError("Owning workflow steps changed")
        temporary = out / "runner-temp"
        temporary.mkdir()
        github_env = out / "github-env"
        github_env.write_text("")
        env.update(RUNNER_TEMP=str(temporary), GITHUB_ENV=str(github_env))
        for index, step in enumerate(runs):
            script = out / ("workflow-step-%02d.sh" % (index + 1))
            script.write_text(step["run"])
            commands.append(["bash", str(script)])
    before = inputs(paths)
    write_json(out / "INPUTS.json", before)
    recipe = {"schema": "M3_NLP_RECEIVER_TEXT_OWNER_GATE_1", "owner": args.owner,
              "receiver_head": head, "receiver_root": str(ROOT), "commands": [],
              "jdk": str(JDK), "state": "RUNNING", "gate_inputs": before,
              "driver_sha256": digest(Path(__file__).read_bytes()),
              "scope": "Unchanged existing source owner tests on pinned receiving sources; no full M3JDK or HotSpot admission."}
    write_json(out / "RESULT.json", recipe)
    begin = time.monotonic()
    all_output = ""
    for index, command in enumerate(commands):
        log = out / ("command-%02d.log" % (index + 1))
        start = time.monotonic()
        with log.open("w") as handle:
            result = subprocess.run(command, cwd=ROOT, env=env, stdout=handle,
                                    stderr=subprocess.STDOUT, check=False)
        data = log.read_bytes()
        all_output += data.decode(errors="replace")
        recipe["commands"].append({"argv": command, "exit": result.returncode,
                                  "elapsed_seconds": round(time.monotonic() - start, 3),
                                  "log": str(log.relative_to(WORK)),
                                  "log_bytes": len(data), "log_sha256": digest(data)})
        write_json(out / "RESULT.json", recipe)
        if result.returncode:
            recipe["state"] = "FAIL"
            recipe["elapsed_seconds"] = round(time.monotonic() - begin, 3)
            write_json(out / "RESULT.json", recipe)
            raise SystemExit(result.returncode)
        if args.owner == "bigstring":
            for line in (out / "github-env").read_text().splitlines():
                key, value = line.split("=", 1)
                env[key] = value
    after = inputs(paths)
    write_json(out / "READBACK.json", after)
    if before != after:
        raise AssertionError("Gate changed source inputs")
    recipe["state"] = "PASS"
    recipe["elapsed_seconds"] = round(time.monotonic() - begin, 3)
    recipe["inputs_unchanged"] = True
    recipe["stdout"] = all_output
    if args.owner == "bigstring":
        classes = Path(env["M3_BIGSTRING_CLASSES"])
        recipe["classes"] = [
            {"path": str(p.relative_to(classes)), "bytes": p.stat().st_size,
             "sha256": digest(p.read_bytes())}
            for p in sorted(classes.rglob("*.class"))]
        recipe["classpath"] = str(classes)
    write_json(out / "RESULT.json", recipe)
    print(json.dumps({"owner": args.owner, "state": recipe["state"],
                      "elapsed_seconds": recipe["elapsed_seconds"],
                      "result": str(out / "RESULT.json"), "stdout": all_output}))


if __name__ == "__main__":
    run()
