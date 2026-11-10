#!/usr/bin/env python3
"""Execute the frozen, focused CI recipe proof and retain actual process evidence."""
from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import xml.etree.ElementTree as ET


HERE = Path(__file__).resolve().parent
CANDIDATE = HERE.parent
WORK = HERE / "maven-workspace"
TASK = "m3/tooling/migration-recipes/tasks/jcc-ci-repair-20261006"
POM = WORK / TASK / "verification/pom.xml"
ROOT = CANDIDATE.parents[1]
JDK = ROOT / "toolchain/jdk-21+35"
MAVEN = ROOT / "toolchain/apache-maven-3.9.9/bin/mvn"


def utc() -> str:
    return dt.datetime.now(dt.timezone.utc).isoformat()


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def identity(path: Path) -> dict:
    data = path.read_bytes()
    return {"path": str(path), "bytes": len(data), "sha256": sha(data),
            "git_blob": hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()}


def write_new(path: Path, data: dict) -> None:
    with path.open("x", encoding="utf-8") as output:
        json.dump(data, output, indent=2)
        output.write("\n")


def check_inputs() -> dict:
    frozen = json.loads((CANDIDATE / "FROZEN_INPUTS.json").read_text())
    copied = json.loads((HERE / "02-maven-inputs.json").read_text())
    for row in frozen["files"]:
        path = CANDIDATE / row["path"]
        assert path.is_file() and not path.is_symlink(), path
        data = path.read_bytes()
        assert len(data) == row["bytes"] and sha(data) == row["sha256"], path
    for row in copied["copies"]:
        path = WORK / row["destination"]
        assert path.is_file() and not path.is_symlink(), path
        data = path.read_bytes()
        assert len(data) == row["bytes"] and sha(data) == row["sha256"], path
    return {"frozen_files_unchanged": len(frozen["files"]),
            "prepared_copies_unchanged": len(copied["copies"]),
            "frozen_inputs": identity(CANDIDATE / "FROZEN_INPUTS.json"),
            "prepared_inputs": identity(HERE / "02-maven-inputs.json")}


def invoke(name: str, command: list[str], environment: dict[str, str], cwd: Path) -> dict:
    log = HERE / (name + ".log")
    record = {"schema": "m3-jcc-ci-process/1", "argv": command, "cwd": str(cwd),
              "java_home": environment["JAVA_HOME"], "start_utc": utc()}
    with log.open("xb") as output:
        process = subprocess.run(command, cwd=cwd, env=environment, stdout=output, stderr=subprocess.STDOUT)
    record.update({"finish_utc": utc(), "exit_code": process.returncode, "output": identity(log)})
    write_new(HERE / (name + ".json"), record)
    print(json.dumps({"stage": name, "exit_code": process.returncode, "log": str(log)}), flush=True)
    return record


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--cache", type=Path, required=True)
    parser.add_argument("--cache-receipt", type=Path, required=True)
    args = parser.parse_args()
    cache, cache_receipt = args.cache.resolve(), args.cache_receipt.resolve()
    assert cache.is_dir() and cache_receipt.is_file()
    materialized = HERE / "materialized-01"
    compiled = HERE / "repaired-test-classes"
    assert not materialized.exists() and not compiled.exists()
    assert not (POM.parent / "target").exists()
    inputs = check_inputs()
    environment = os.environ.copy()
    environment["JAVA_HOME"] = str(JDK)
    environment["PATH"] = str(JDK / "bin") + os.pathsep + environment.get("PATH", "")
    command = [str(MAVEN), "-o", "-B", "-ntp", "-Dmaven.repo.local=" + str(cache),
               "-Dm3.ci.materialized=" + str(materialized), "-f", str(POM), "test"]
    request = {"schema": "m3-jcc-ci-maven-execution-request/1", "start_utc": utc(),
               "inputs": inputs, "toolchain_receipt": identity(ROOT / "toolchain/TOOLCHAIN.json"),
               "cache": str(cache), "cache_custody": identity(cache_receipt),
               "argv": command, "materialized_destination": str(materialized),
               "scope": "Finite diagnostic using an exact frozen artifact cache; native source custody and broad admission remain separate. Focused frozen two-owner/eight-method lane; no whole-module or JDK build claim."}
    write_new(HERE / "04-maven-request.json", request)
    result = invoke("05-maven", command, environment, WORK)
    if result["exit_code"] != 0:
        write_new(HERE / "JAVA_EXECUTION_FAILURE.json", {"status": "MAVEN_FAILED", "process": result,
                                                        "inputs_after": check_inputs()})
        return result["exit_code"] or 1
    report_path = POM.parent / "target/surefire-reports/TEST-com.m3.rewrite.backport.JccCiRepairTest.xml"
    report = ET.parse(report_path).getroot()
    assert int(report.attrib["tests"]) == 8
    assert all(int(report.attrib.get(k, "0")) == 0 for k in ("failures", "errors", "skipped"))
    props = {p.attrib["name"]: p.attrib["value"] for p in report.findall("./properties/property")}
    classpath = props.get("surefire.test.class.path", props.get("java.class.path"))
    assert classpath
    classpath_entries = [Path(p) for p in classpath.split(os.pathsep)]
    assert all(p.exists() for p in classpath_entries), classpath_entries
    expected = json.loads((CANDIDATE / "AUTHORING_RESULT.json").read_text())["outputs"]
    actual_files = sorted(str(p.relative_to(materialized)) for p in materialized.rglob("*") if p.is_file())
    assert actual_files == sorted(row["path"] for row in expected)
    outputs = []
    for row in expected:
        path = materialized / row["path"]
        actual = identity(path)
        assert actual["sha256"] == row["after"]["sha256"] and actual["bytes"] == row["after_bytes"]
        assert actual["git_blob"] == row["after_git_blob"]
        installer_path = HERE / "installer/installer-workspace" / row["path"]
        assert path.read_bytes() == installer_path.read_bytes(), installer_path
        outputs.append({"repository_path": row["path"], **actual,
                        "origin": "ACTUAL_OPENREWRITE_RESULT_FROM_JccCiRepairTest",
                        "matches_actual_installer_output": identity(installer_path)})
    javac_sources = [str(materialized / row["path"]) for row in expected if row["path"].endswith(".java")]
    assert len(javac_sources) == 2
    compiled.mkdir()
    javac_command = [str(JDK / "bin/javac"), "--release", "21", "-proc:none", "-encoding", "UTF-8",
                     "-classpath", classpath, "-d", str(compiled), *javac_sources]
    javac_result = invoke("06-repaired-test-javac", javac_command, environment, WORK)
    if javac_result["exit_code"] != 0:
        write_new(HERE / "JAVA_EXECUTION_FAILURE.json", {"status": "REPAIRED_TEST_COMPILATION_FAILED",
                                                        "process": javac_result,
                                                        "inputs_after": check_inputs()})
        return javac_result["exit_code"] or 1
    classes = [identity(p) for p in sorted(compiled.rglob("*.class"))]
    assert {Path(p["path"]).name for p in classes} == {
        "M3A3RegexMemoryLabDeliveryRecipeTest.class", "M3A3RegexMemoryWorkflowRecipeTest.class"}
    receipt = {"schema": "m3-jcc-ci-java-execution/1", "status": "FOCUSED_JAVA_PASS",
               "finish_utc": utc(), "tested_baseline_commit": "b71ee5bf88398fb80961c13db03e8c675fb445ce",
               "maven_process": result, "junit_report": identity(report_path),
               "junit_tests": [{"class": t.attrib["classname"], "name": t.attrib["name"]}
                               for t in report.findall("testcase")],
               "test_count": 8, "failures": 0, "errors": 0, "skipped": 0,
               "actual_recipe_output_count": 3, "fixed_point_change_count": 0,
               "python_installer_receipt": identity(HERE / "installer/receipt.json"),
               "outputs": outputs, "javac_process": javac_result,
               "compiled_repaired_whole_test_classes": classes,
               "surefire_classpath_property": "surefire.test.class.path" if "surefire.test.class.path" in props else "java.class.path",
               "surefire_classpath": [{"path": str(p), **(identity(p) if p.is_file() else {"kind": "directory"})}
                                       for p in classpath_entries],
               "inputs_after": check_inputs(), "cache_custody": identity(cache_receipt),
               "scope": "Actual finite diagnostic recipe/JUnit execution and compilation of both complete repaired test classes. Their original four test methods are preserved but are not executed by this focused lane. Foundation environment wiring has only the separately labelled mocked control. Native source custody, broad admission, whole-module, CI-host, GCC acquisition and rebuilt-JDK gates remain separate."}
    write_new(HERE / "JAVA_EXECUTION.json", receipt)
    print(json.dumps({"status": receipt["status"], "tests": 8, "outputs": 3, "javac_classes": len(classes)}), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
