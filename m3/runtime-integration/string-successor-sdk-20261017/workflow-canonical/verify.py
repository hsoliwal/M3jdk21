#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact receiving SDK inputs and actual two-test Surefire evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SUITES = {"com.synexia.rewrite.M3StringMatchedBuildAdmissionRecipeTest":["everyPartialStateAndOrderPreservesBothPipelinesAndHoldsFullBuild","eachWorkflowRefusesDriftMissingAndDuplicateOwnership"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-string-matched-build-admission-20261017.yml","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/m3-first-release-admission.yml","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/mindex-string-backing.yml","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/m3-first-release-admission.yml","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/mindex-string-backing.yml","src/test/java/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipe.java":"0b81dc7b2b8682681a136d5dcffdb8a6526bd849","src/test/java/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipeTest.java":"f5d3131ebbec0dc35861c876f6acf60aa8a824b6","src/main/resources/META-INF/rewrite/m3-string-matched-build-admission-20261017.yml":"a4bb79181a8b5cc126ff8c65abfccdb5a524dec5","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/manifest.tsv":"e77d32745fd4fe6af4953ea9d34c6b1c4207f681","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/m3-first-release-admission.yml":"1f0924995cae52819e1bcf76a4d0b4385a287b96","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/m3-first-release-admission.yml":"40eab8605bbc2e323c6e93d4da4f3d35ee1a0c49","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/mindex-string-backing.yml":"793ea270938003a40f8d54f831b4d189e1f91f97","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/mindex-string-backing.yml":"2a92041b72e27aa38581c47bdd653e7c1e75ceab","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipe.java":"ffff0c1b6a4012821f7e5bab6b0e04d6a83a9725e208d9fd6c4f0b9f2a3731c5","src/test/java/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipeTest.java":"5ae5410f079446e6c2a9de92c1f696a098200006d7cc7889ff7654449e9dd0a1","src/main/resources/META-INF/rewrite/m3-string-matched-build-admission-20261017.yml":"e33d975adb1fbc8589ff4e2e0697192d5289a44b60fb12f8a30aa872e84075fe","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/manifest.tsv":"7995918657c3618afa6afb8000822059b390d087c52ecfaf888f9da458efb3d7","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/m3-first-release-admission.yml":"b44c1590ecbf1df53403ad810601c8fde071e556a149a26f4bec8bf7c8be06d6","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/m3-first-release-admission.yml":"1fff46ed2234fe430f1dbcbdb7f55c3e3f0d0e4c08f7ae6e44b951f3da0bd552","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/before/mindex-string-backing.yml":"ed0f0e4a9577f85a88860df2e226eb010a8bb7b793fcd8201764329c981d25da","src/main/resources/com/synexia/rewrite/m3-string-matched-build-admission-20261017/out/mindex-string-backing.yml":"dbcce64499d3b189985137ea0f6580d7fc983b343536abca5fd7d3b6853be0b8","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e"}
RECEIVER_OUTPUTS = {".github/workflows/m3-first-release-admission.yml":"1fff46ed2234fe430f1dbcbdb7f55c3e3f0d0e4c08f7ae6e44b951f3da0bd552",".github/workflows/mindex-string-backing.yml":"dbcce64499d3b189985137ea0f6580d7fc983b343536abca5fd7d3b6853be0b8"}

def validate_report(data: bytes, classname: str, methods: list[str]) -> dict[str, int]:
    root = ET.fromstring(data)
    if root.tag != "testsuite":
        raise ValueError("expected one Surefire testsuite")
    counts = {key: int(root.get(key, "-1"))
              for key in ("tests", "failures", "errors", "skipped")}
    if counts != {"tests": len(methods), "failures": 0, "errors": 0, "skipped": 0}:
        raise ValueError("SDK suite did not execute all required gates successfully")
    cases = root.findall("testcase")
    if len(cases) != len(methods) or {case.get("name") for case in cases} != set(methods):
        raise ValueError("SDK test inventory drift")
    for case in cases:
        if case.get("classname") != classname:
            raise ValueError("SDK test class drift")
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            raise ValueError("SDK test failure, error or skip present")
    return counts


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def inputs() -> tuple[dict, bytes]:
    pins_data = (ROOT / "pins.json").read_bytes()
    pins = json.loads(pins_data)
    if pins["schema"] != "M3_STRING_MATCHED_BUILD_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    rows = pins["files"]
    if not 1 <= len(rows) <= 64:
        raise ValueError("receiving file budget")
    if {row["path"] for row in rows} != set(REQUIRED_PATHS):
        raise ValueError("required receiving input inventory drift")
    for name, expected in REQUIRED_SHA256.items():
        matches = [row for row in rows if row["path"] == name]
        if len(matches) != 1 or matches[0].get("sha256") != expected:
            raise ValueError("required receiving digest drift: " + name)
    for name, blob in REQUIRED_GIT_BLOBS.items():
        matches = [row for row in rows if row["path"] == name]
        if len(matches) != 1 or matches[0].get("unchangedGitBlob") != blob:
            raise ValueError("required receiving identity drift: " + name)
    seen = set()
    for row in rows:
        name = row["path"]
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name or name in seen:
            raise ValueError("invalid receiving path")
        seen.add(name)
        data = (ROOT / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != row["sha256"]:
            raise ValueError("receiving source drift: " + name)
        if row.get("unchangedGitBlob") and git_blob(data) != row["unchangedGitBlob"]:
            raise ValueError("receiving Git blob drift: " + name)
    def inventory(base: Path, pattern: str) -> set[str]:
        found = set()
        for path in base.rglob(pattern):
            if path.is_file():
                found.add(path.relative_to(ROOT).as_posix())
                if len(found) > 64:
                    raise ValueError("receiving source inventory budget")
        return found
    engine = inventory(ROOT / "pinned-engine", "*.java")
    tests = inventory(ROOT / "src/test/java", "*.java")
    if engine != {name for name in REQUIRED_PATHS if name.startswith("pinned-engine/")}:
        raise ValueError("unsealed receiving provider")
    if tests != {name for name in REQUIRED_PATHS if name.startswith("src/test/java/")}:
        raise ValueError("receiving SDK test scope drift")
    resources = inventory(ROOT / "src/main/resources", "*")
    if resources != {name for name in REQUIRED_PATHS if name.startswith("src/main/resources/")}:
        raise ValueError("unsealed receiving resource")
    if inventory(ROOT / "src/test/resources", "*"):
        raise ValueError("test resources can shadow receiving fixtures")
    return pins, pins_data


def receiver(repository: Path, pins: dict) -> None:
    for name, expected in RECEIVER_OUTPUTS.items():
        path = repository / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("receiving String tooling output drift: " + name)


def self_test(pins: dict, pins_data: bytes) -> None:
    global ROOT
    original = ROOT
    (original / "target").mkdir(exist_ok=True)
    refused = 0
    with tempfile.TemporaryDirectory(dir=original / "target") as directory:
        fixture = Path(directory)
        for row in pins["files"]:
            path = fixture / row["path"]
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes((original / row["path"]).read_bytes())
        (fixture / "pins.json").write_bytes(pins_data)
        try:
            ROOT = fixture
            inputs()
            def rejects() -> None:
                nonlocal refused
                try:
                    inputs()
                except ValueError:
                    refused += 1
                else:
                    raise AssertionError("unsealed receiving input admitted")
            for name in (
                "pinned-engine/com/synexia/rewrite/nested/Extra.java",
                "pinned-engine/other/Extra.java",
                "src/test/java/other/ExtraTest.java",
                "src/test/resources/com/synexia/rewrite/hash-pinned-java/"
                "m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt",
                "src/main/resources/com/synexia/rewrite/hash-pinned-java/"
                "m3jdk21-generic-filtered-purpose-20261017/parse-context.tsv",
            ):
                path = fixture / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("unsealed", encoding="utf-8")
                rejects()
                path.unlink()
            for name in REQUIRED_PATHS:
                changed = json.loads(pins_data)
                changed["files"] = [row for row in changed["files"] if row["path"] != name]
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            for name in REQUIRED_GIT_BLOBS:
                changed = json.loads(pins_data)
                next(row for row in changed["files"] if row["path"] == name)[
                    "unchangedGitBlob"] = "0" * 40
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            for name in REQUIRED_SHA256:
                changed = json.loads(pins_data)
                next(row for row in changed["files"] if row["path"] == name)["sha256"] = "0" * 64
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            name = "src/test/java/com/synexia/rewrite/M3StringMatchedBuildAdmissionRecipeTest.java"
            data = (fixture / name).read_bytes()
            altered = data + b"// changed test\n"
            (fixture / name).write_bytes(altered)
            changed = json.loads(pins_data)
            row = next(row for row in changed["files"] if row["path"] == name)
            row["sha256"] = hashlib.sha256(altered).hexdigest()
            row["unchangedGitBlob"] = git_blob(altered)
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            (fixture / name).write_bytes(data)
            (fixture / "pins.json").write_bytes(pins_data)
            inputs()
            assert refused == 5 + len(REQUIRED_PATHS) + len(REQUIRED_GIT_BLOBS) + len(REQUIRED_SHA256) + 1
        finally:
            ROOT = original
    print("M3_STRING_MATCHED_BUILD_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--reports", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--repository", type=Path)
    args = parser.parse_args()
    pins, pins_data = inputs()
    if args.repository is not None:
        receiver(args.repository.resolve(), pins)
    if args.self_test:
        self_test(pins, pins_data)
    if not args.reports:
        print("M3_STRING_MATCHED_BUILD_RECEIVING_SDK_INPUTS_EXACT")
        return
    directory = ROOT / "target/surefire-reports"
    expected = sorted(directory / ("TEST-" + name + ".xml") for name in SUITES)
    if sorted(directory.glob("TEST-*.xml")) != expected:
        raise ValueError("missing or unexpected SDK test reports")
    reports = {}
    for name, methods in SUITES.items():
        data = (directory / ("TEST-" + name + ".xml")).read_bytes()
        reports[name] = {"counts": validate_report(data, name, methods),
                         "sha256": hashlib.sha256(data).hexdigest()}
    emitted = ROOT / "target/emitted/matched-build-admission"
    expected_files = set(RECEIVER_OUTPUTS) | {"matched-build-not-admitted.log"}
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("workflow recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("workflow recipe output drift: " + name)
    hold_log = (emitted / "matched-build-not-admitted.log").read_bytes()
    if hold_log.decode("utf-8", errors="strict") != "MATCHED_JDK_OUTPUT_NOT_ADMITTED\nNo measured larger-output authorization is recorded for configure, build, images, resources and jtreg.\nThe bounded 512 MiB SDK reservation does not admit a complete JDK build.\n":
        raise ValueError("isolated full-build hold probe log drift")
    receipt = {
        "schema": "M3_STRING_MATCHED_BUILD_RECEIVING_SDK_RECEIPT_V1",
        "scope": "ORIGINAL_PROVIDER_TWO_EXISTING_WORKFLOW_OUTPUTS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedWorkflowOutputs": RECEIVER_OUTPUTS,
        "isolatedHoldProbe": {"expectedExitCode": 1, "verifiedBySdk": True,
                             "exactLogSha256": hashlib.sha256(hold_log).hexdigest()},
        "fullJdkOutputAdmission": "NOT_ADMITTED",
        "matchedJdkJtreg": "NOT_RUN",
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
