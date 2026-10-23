#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact receiving SDK inputs and actual six-test Surefire evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
CLASS = "com.synexia.rewrite.M3SingleByteCursorRecipeTest"
METHODS = {
    "exactTargetAndPreimageRemainSealed",
    "sdkAppliesExactOutputThenHasNoChanges",
    "refusesMissingDuplicateAndDriftBeforeGeneration",
    "identicalTextIsNotPermissionToSkipTheJavaParser",
    "unrelatedJavaOwnerIsNotModified",
    "namedRecipeActivatesTheSealedCrate",
}


REQUIRED_PATHS = [
  "licenses/OpenJDK-ADDITIONAL_LICENSE_INFO",
  "licenses/OpenJDK-LICENSE",
  "pinned-engine/com/synexia/rewrite/M3EditScope.java",
  "pinned-engine/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java",
  "pinned-engine/com/synexia/rewrite/M3ScopeInference.java",
  "pom.xml",
  "src/main/resources/META-INF/rewrite/m3jdk21-single-byte-cursor-20261023.yml",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3String.before.java.txt",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3String.java.txt",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3StringSingleByteCursorTest.java.txt",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/manifest.tsv",
  "src/test/java/com/synexia/rewrite/M3SingleByteCursorRecipeTest.java",
  "verify.py"
]
REQUIRED_GIT_BLOBS = {
  "pinned-engine/com/synexia/rewrite/M3EditScope.java": "274360816db1e44a2f1796a3bc5cdeff6b0ce6db",
  "pinned-engine/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java": "ef56d2e7bf7dbe6503740c606c0d53e877ebd2ab",
  "pinned-engine/com/synexia/rewrite/M3ScopeInference.java": "4eb9b7224a22f3ad02111ab8cd731acfc53dfbe0",
  "src/main/resources/META-INF/rewrite/m3jdk21-single-byte-cursor-20261023.yml": "938c8b9a5c3d0f990296aa49dce34d7ad0409c38",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3String.before.java.txt": "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3String.java.txt": "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/M3StringSingleByteCursorTest.java.txt": "07e183224e75515f207679942e851bc2be0c9013",
  "src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-single-byte-cursor-20261023/manifest.tsv": "83becfeaf96aef1d21ecff16121ac0b053499224",
  "src/test/java/com/synexia/rewrite/M3SingleByteCursorRecipeTest.java": "152fedf92e8d6d907f9bb207a4c7c6a25d4f6167"
}


def validate_report(data: bytes) -> dict[str, int]:
    root = ET.fromstring(data)
    if root.tag != "testsuite":
        raise ValueError("expected one Surefire testsuite")
    counts = {key: int(root.get(key, "-1"))
              for key in ("tests", "failures", "errors", "skipped")}
    if counts != {"tests": 6, "failures": 0, "errors": 0, "skipped": 0}:
        raise ValueError("SDK suite did not execute all six gates successfully")
    cases = root.findall("testcase")
    if len(cases) != 6 or {case.get("name") for case in cases} != METHODS:
        raise ValueError("SDK test inventory drift")
    for case in cases:
        if case.get("classname") != CLASS:
            raise ValueError("SDK test class drift")
        if any(case.find(tag) is not None for tag in ("failure", "error", "skipped")):
            raise ValueError("SDK test failure, error or skip present")
    return counts


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def inputs() -> tuple[dict, bytes]:
    pins_data = (ROOT / "pins.json").read_bytes()
    pins = json.loads(pins_data)
    if pins["schema"] != "M3_SINGLE_BYTE_CANONICAL_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    rows = pins["files"]
    if not 1 <= len(rows) <= 64:
        raise ValueError("receiving file budget")
    if {row["path"] for row in rows} != set(REQUIRED_PATHS):
        raise ValueError("required receiving input inventory drift")
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
    if tests != {"src/test/java/com/synexia/rewrite/M3SingleByteCursorRecipeTest.java"}:
        raise ValueError("receiving SDK test scope drift")
    resources = inventory(ROOT / "src/main/resources", "*")
    if resources != {name for name in REQUIRED_PATHS if name.startswith("src/main/resources/")}:
        raise ValueError("unsealed receiving resource")
    if inventory(ROOT / "src/test/resources", "*"):
        raise ValueError("test resources can shadow receiving fixtures")
    return pins, pins_data


def receiver(repository: Path, pins: dict) -> None:
    expected = {
        "src/java.base/share/classes/java/lang/M3String.java": "8a57ca4f085e4e8f1f1d9f2ec0fdb2fc5888597e",
        "test/jdk/java/lang/String/M3StringSingleByteCursorTest.java": "07e183224e75515f207679942e851bc2be0c9013",
    }
    for name, blob in expected.items():
        if git_blob((repository / name).read_bytes()) != blob:
            raise ValueError("candidate target source drift: " + name)


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
                "m3jdk21-single-byte-cursor-20261023/M3String.java.txt",
                "src/main/resources/com/synexia/rewrite/hash-pinned-java/"
                "m3jdk21-single-byte-cursor-20261023/parse-context.tsv",
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
            name = "src/test/java/com/synexia/rewrite/M3SingleByteCursorRecipeTest.java"
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
            assert refused == 5 + len(REQUIRED_PATHS) + len(REQUIRED_GIT_BLOBS) + 1
        finally:
            ROOT = original
    print("M3_SINGLE_BYTE_CANONICAL_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_SINGLE_BYTE_CANONICAL_SDK_INPUTS_EXACT")
        return
    directory = ROOT / "target/surefire-reports"
    expected = directory / ("TEST-" + CLASS + ".xml")
    if sorted(directory.glob("TEST-*.xml")) != [expected]:
        raise ValueError("missing or unexpected SDK test reports")
    report_data = expected.read_bytes()
    counts = validate_report(report_data)
    receipt = {
        "schema": "M3_SINGLE_BYTE_CANONICAL_SDK_RECEIPT_V1",
        "scope": "CANONICAL_EF56_SIX_SDK_TESTS_ONLY",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providerGitBlob": "ef56d2e7bf7dbe6503740c606c0d53e877ebd2ab",
        "testGitBlob": "152fedf92e8d6d907f9bb207a4c7c6a25d4f6167",
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reportSha256": hashlib.sha256(report_data).hexdigest(),
        "tests": counts,
        "canonicalProviderEf56": "QUALIFIED_BY_THIS_SDK_ONLY",
        "yamlDescriptorActivation": "QUALIFIED_BY_SDK_TEST",
        "matchedJdkJtreg": "NOT_RUN",
        "javaRuntimeAndJni": "NOT_RUN",
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))


if __name__ == "__main__":
    main()
