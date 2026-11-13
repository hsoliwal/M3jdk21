#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only frozen-input qualification of the public String recipe mirror."""

from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import signal
import stat
import subprocess
import time
import xml.etree.ElementTree as ET

TARGET = Path("/tmp/m3-publication/string")
MIRROR = TARGET / "m3/runtime-integration/regex-rope-affix-effects/recipe"
OUTPUT = Path("/tmp/m3-publication/string-mirror-qualification")
BOOT = Path("/tmp/m3-toolchain/jdk-21.0.12.1+1")
MAVEN = Path("/tmp/m3-toolchain/apache-maven-3.9.11/bin/mvn")
TESTS = {
    "exactReplayFixedPointAndInputOrder", "mixedPreimagesAndPostimages",
    "driftMissingDuplicateAndWrongRepresentationRefuse", "postScanChangesRefuse",
    "qualifiedSdkResultsAreRetained",
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def write_json(name, data):
    (OUTPUT / name).write_text(json.dumps(data, indent=2) + "\n")


def parse_rows(path, width):
    rows = [line.split("\t") for line in path.read_text().splitlines()
            if line and not line.startswith("#")]
    if any(len(row) != width for row in rows) or len({row[0] for row in rows}) != len(rows):
        raise ValueError("bad tabular input: " + str(path))
    return rows


def signature(path):
    if path.is_symlink() or not path.is_file():
        raise ValueError("expected regular admitted file: " + str(path))
    info = path.stat()
    data = path.read_bytes()
    return {"sha256": sha(data), "bytes": len(data),
            "mode": oct(stat.S_IMODE(info.st_mode)), "mtime_ns": info.st_mtime_ns}


def mirror_state():
    return {str(path.relative_to(MIRROR)): signature(path)
            for path in sorted(MIRROR.rglob("*")) if path.is_file()
            and path.relative_to(MIRROR).parts[0] != "target"}


def run(label, command, environment):
    started = datetime.now(timezone.utc).isoformat()
    start = time.monotonic()
    with (OUTPUT / (label + ".log")).open("w") as log:
        process = subprocess.Popen(command, cwd=MIRROR, env=environment,
                                   stdout=log, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        try:
            result = process.wait(timeout=300)
        except BaseException:
            for signum in (signal.SIGTERM, signal.SIGKILL):
                try:
                    os.killpg(process.pid, signum)
                except ProcessLookupError:
                    pass
                if signum == signal.SIGTERM:
                    try:
                        process.wait(timeout=5)
                    except subprocess.TimeoutExpired:
                        pass
            process.wait(timeout=5)
            raise
    receipt = {"command": command, "cwd": str(MIRROR), "started_utc": started,
               "seconds": round(time.monotonic() - start, 3), "exit_code": result,
               "log_sha256": sha((OUTPUT / (label + ".log")).read_bytes())}
    write_json(label + ".json", receipt)
    if result != 0:
        raise ValueError(label + " failed; original log and command are retained")
    return receipt


def main():
    if (MIRROR / "target").exists():
        raise ValueError("fresh public mirror must have no Maven target output")
    if (OUTPUT / "MIRROR_QUALIFICATION.json").exists():
        raise ValueError("qualification output already exists")
    manifests = {}
    for path in (MIRROR / "src/main/resources/com/synexia/rewrite").glob("hash-pinned-*/*/manifest.tsv"):
        for relative, before, after, resource in parse_rows(path, 4):
            if relative in manifests:
                raise ValueError("duplicate target")
            manifests[relative] = (before, after)
    if len(manifests) != 15:
        raise ValueError("expected all 15 targets")
    before_product = {path: signature(TARGET / path) for path in sorted(manifests)}
    if any(before_product[path]["sha256"] != after for path, (_, after) in manifests.items()):
        raise ValueError("frozen product must already have every final received postimage")
    before_mirror = mirror_state()
    write_json("PRODUCT_BEFORE.json", before_product)
    write_json("MIRROR_INPUTS_BEFORE.json", before_mirror)
    environment = dict(os.environ, JAVA_HOME=str(BOOT),
                       PATH=str(BOOT / "bin") + os.pathsep + os.environ.get("PATH", ""),
                       PYTHONDONTWRITEBYTECODE="1")
    maven_command = [str(MAVEN), "-o", "-B", "-ntp",
                     "-Dmaven.repo.local=/tmp/m3-maven-repository",
                     "-Drecipe.owner.root=" + str(MIRROR / "sdk-owner"), "test"]
    maven_receipt = run("maven", maven_command, environment)
    report = MIRROR / "target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
    xml_bytes = report.read_bytes()
    xml = ET.fromstring(xml_bytes)
    cases = xml.findall("./testcase")
    if len(cases) != 5 or {case.attrib["name"] for case in cases} != TESTS:
        raise ValueError("unexpected SDK test inventory")
    if any(case.find(tag) is not None for case in cases for tag in ("failure", "error", "skipped")):
        raise ValueError("SDK tests did not all pass")
    properties = {p.attrib["name"]: p.attrib.get("value", "")
                  for p in xml.findall("./properties/property")}
    if (properties.get("m3.recipe.owner.root") != str(MIRROR / "sdk-owner")
            or properties.get("java.specification.version") != "21"
            or properties.get("m3.recipe.name") != "com.synexia.m3jdk.RopeAffixPreparedEffects20261017"):
        raise ValueError("SDK did not use the exact bundled owner and recipe on Java21")
    (OUTPUT / "SDK_TESTS.xml").write_bytes(xml_bytes)
    for name in ("SDK_INPUTS.tsv", "SDK_RESULTS.tsv"):
        (OUTPUT / name).write_bytes((MIRROR / "target/candidate" / name).read_bytes())
    results = parse_rows(OUTPUT / "SDK_RESULTS.tsv", 3)
    if len(results) != 15 or {row[0] for row in results} != set(manifests):
        raise ValueError("wrong actual SDK output set")
    for relative, before, after in results:
        if ((before, after) != manifests[relative]
                or sha((MIRROR / "target/candidate" / relative).read_bytes()) != after):
            raise ValueError("actual SDK output differs from manifest")
    receiver = ["python3", str(MIRROR / "receive.py"), "--target", str(TARGET)]
    check = run("receive-check", receiver + ["--check"], environment)
    if (OUTPUT / "receive-check.log").read_text().strip() != "M3_SDK_RESULTS_RECEIVER_PASS mode=check targets=15 changes=0":
        raise ValueError("check did not report 15 targets and zero changes")
    if {path: signature(TARGET / path) for path in sorted(manifests)} != before_product:
        raise ValueError("product bytes, modes or mtimes changed during SDK/check")
    apply = run("receive-apply", receiver, environment)
    if (OUTPUT / "receive-apply.log").read_text().strip() != "M3_SDK_RESULTS_RECEIVER_PASS mode=apply targets=15 changes=0":
        raise ValueError("apply did not report 15 targets and zero changes")
    after_product = {path: signature(TARGET / path) for path in sorted(manifests)}
    after_mirror = mirror_state()
    write_json("PRODUCT_AFTER.json", after_product)
    write_json("MIRROR_INPUTS_AFTER.json", after_mirror)
    if after_product != before_product or after_mirror != before_mirror:
        raise ValueError("frozen input bytes, modes or mtimes changed")
    owner_inputs = {path: digest for path, digest in parse_rows(OUTPUT / "SDK_INPUTS.tsv", 2)
                    if path.startswith("owner/")}
    if len(owner_inputs) != 4:
        raise ValueError("expected exactly four bundled owner inputs")
    summary = {
        "schema": "M3_PUBLIC_STRING_MIRROR_QUALIFICATION_V1", "status": "PASS",
        "mirror": str(MIRROR), "target": str(TARGET),
        "java_home": str(BOOT), "offline_maven": True,
        "maven": maven_receipt, "sdk_tests": 5,
        "sdk_failures": 0, "sdk_errors": 0, "sdk_skipped": 0,
        "sdk_owner_root": str(MIRROR / "sdk-owner"), "sdk_owner_inputs": owner_inputs,
        "receiving": {"check": check, "apply": apply, "targets": 15, "changes": 0},
        "product_bytes_modes_mtimes_unchanged": True,
        "mirror_input_bytes_modes_mtimes_unchanged": True,
        "product_files_verified": len(before_product), "mirror_input_files_verified": len(before_mirror),
        "sdk_evidence_sha256": {name: sha((OUTPUT / name).read_bytes())
                                 for name in ("SDK_TESTS.xml", "SDK_INPUTS.tsv", "SDK_RESULTS.tsv")},
        "input_inventory_sha256": {name: sha((OUTPUT / name).read_bytes())
                                     for name in ("PRODUCT_BEFORE.json", "PRODUCT_AFTER.json",
                                                  "MIRROR_INPUTS_BEFORE.json", "MIRROR_INPUTS_AFTER.json")},
        "runtime_tests_rerun": False, "publication_performed": False,
        "full_string_stage_accepted": False,
    }
    write_json("MIRROR_QUALIFICATION.json", summary)
    print(json.dumps(summary, indent=2))
    print("MIRROR_QUALIFICATION_SHA256=" + sha((OUTPUT / "MIRROR_QUALIFICATION.json").read_bytes()))


if __name__ == "__main__":
    main()
