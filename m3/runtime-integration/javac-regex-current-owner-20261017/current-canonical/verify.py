#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify sealed current javac owner recipe inputs and actual named two-test SDK evidence."""
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
SUITES = {"com.synexia.rewrite.M3JavacConstantRegexCurrentOwnerQualificationRecipeTest":["allAdditionStatesAndOrdersReachExactFixedPoint","ownershipRefusesDriftMissingDuplicateAndPreservesUnrelated"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-javac-regex-current-owner-qualification-20261017.yml","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/manifest.tsv","FOSS_REUSE_DECISION.tsv","THIRD_PARTY_NOTICES.md","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlan.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFacts.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexCompilerASTPrecompute.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/pom.xml","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlanContract.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFactsContract.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/pom.xml","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify.sh","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/MIndexJdkConstantRegexCurrentOwnerContract.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify_source_seals.py","src/test/java/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipe.java":"e2f35fe4ffac96c83ce66a7000f0c2eb6f17beee","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f","src/main/resources/META-INF/rewrite/m3-javac-regex-current-owner-qualification-20261017.yml":"ad1239d7c9c64f1aed19fbc00a3a7a0b1ad6f4ce","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/manifest.tsv":"a533ad8561cea4e855f1699300c625450235cf1d","FOSS_REUSE_DECISION.tsv":"470e496ece2023a6b639c54610f2ac670717315b","THIRD_PARTY_NOTICES.md":"2feacb1e4b9cb3f2adf3e57b8dd669afc2289c08","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlan.java":"d45fd92bc212a02e7415e31a558a6fb941e608bd","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFacts.java":"0003406b4d62499e919a9e5a1be0853906225158","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexCompilerASTPrecompute.java":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/pom.xml":"ae122d96a2363c4f0dc914ebfb9a06081b140217","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlanContract.java":"92d77554ea1a8a242bc7a4b891a6af6a56bb4431","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFactsContract.java":"901db786b0ad9306f2750524255b8bb8046f05e4","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/pom.xml":"dd95ca0abeb4c7981cf3aaad7a8ec657be6fa2b1","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify.sh":"edefccb1828cddc85c58d71b50326d7ef0d7bb61","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/MIndexJdkConstantRegexCurrentOwnerContract.java":"ad2ac03772af9919be8a8a782b8308666cf1a27a","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify_source_seals.py":"bd13d0cdf6cf73fcdcd7912a98241b835fe46380","src/test/java/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.java":"b73357d101dce797ffdd6bcb57fbdf4072d3226b"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipe.java":"8a96477bc8b912c6816c0a93bc3c5dbfec214fcd45ad6c9418d8708855c36f0e","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e","src/main/resources/META-INF/rewrite/m3-javac-regex-current-owner-qualification-20261017.yml":"5c0237c92dd43634b63bcbf0b56ba5f76e3dc9d5a22f17fa684e878f3940f505","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/manifest.tsv":"da0085d694476cf0780a2a25fa293b372dd9ed887db4ce0552fac47116ea9cd7","FOSS_REUSE_DECISION.tsv":"ce6cca6a4ff64f25451fdc372c95270085926bfee6b6b7a488593a642180c0b1","THIRD_PARTY_NOTICES.md":"9b0f0c3af723e293bc990d01c0b8fd4f209e17b0b5c19506bde38c2deb544f2f","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlan.java":"940efc1c846bd869d542f2104c70b1c972cf89361133082850a6b0e888b9af87","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFacts.java":"85e36a76dc85b75fda2d3e8d4577a73d586c33ce26ef04367c9236cda62425dd","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexCompilerASTPrecompute.java":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/pom.xml":"cb8d9fb8e14df44afac0ad164b3a0ff7439fd1d3a64c46389a0da489af1f3b26","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJdkConstantRegexPlanContract.java":"cdd2d4704c647adbd5e6888a4228355a53b6dd7dc400d0e86fc04673e760fddf","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/inputs/MIndexJavaConstantStringFactsContract.java":"12d064b6e11a9ca3c670340890a22e699125ded1e051a18407f085adf53c76f2","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/pom.xml":"52428721ed0386f00311d4cbbc5deb96947220ce55e2698e13170bf173656c47","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify.sh":"50c697e1821bd1b358d326fdcb9fb3d13caa340af18875c9ca52e89ec92c7e1e","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/MIndexJdkConstantRegexCurrentOwnerContract.java":"1b87be56a58604ce1a5bb1f6ecb448771c269a8de21c78eecb81cb8e71cdbf29","src/main/resources/com/synexia/rewrite/m3-javac-regex-current-owner-qualification-20261017/out/verify_source_seals.py":"7fa6d59139bc533438358ce64f535de53ecfe750b58800a2e2e28e6e0c5677cf","src/test/java/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.java":"44d203cb6f96a92f1d14a80a1cc87b80cbb5eeb86049d90cb355c636b59edb81"}
RECEIVER_OUTPUTS = {"synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/pom.xml":"52428721ed0386f00311d4cbbc5deb96947220ce55e2698e13170bf173656c47","synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/verify.sh":"50c697e1821bd1b358d326fdcb9fb3d13caa340af18875c9ca52e89ec92c7e1e","synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexCurrentOwnerContract.java":"1b87be56a58604ce1a5bb1f6ecb448771c269a8de21c78eecb81cb8e71cdbf29","synexia-openrewrite-recipes/crates/m3-javac-regex-current-owner-qualification-20261017/verify_source_seals.py":"7fa6d59139bc533438358ce64f535de53ecfe750b58800a2e2e28e6e0c5677cf"}
RECEIVER_CUSTODY = {"synexia-indexstring/src/main/java/com/synexia/indexstring/MIndexJdkConstantRegexPlan.java":"940efc1c846bd869d542f2104c70b1c972cf89361133082850a6b0e888b9af87","synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFacts.java":"85e36a76dc85b75fda2d3e8d4577a73d586c33ce26ef04367c9236cda62425dd","synexia-mindex/compiler/src/main/java/com/synexia/mindex/compiler/MIndexCompilerASTPrecompute.java":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","synexia-indexstring/pom.xml":"cb8d9fb8e14df44afac0ad164b3a0ff7439fd1d3a64c46389a0da489af1f3b26","synexia-indexstring/src/test/java/com/synexia/indexstring/MIndexJdkConstantRegexPlanContract.java":"cdd2d4704c647adbd5e6888a4228355a53b6dd7dc400d0e86fc04673e760fddf","synexia-mindex/compiler/src/test/java/com/synexia/mindex/compiler/MIndexJavaConstantStringFactsContract.java":"12d064b6e11a9ca3c670340890a22e699125ded1e051a18407f085adf53c76f2"}

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
    if pins["schema"] != "M3_JAVAC_REGEX_CURRENT_OWNER_QUALIFICATION_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    if pins["providers"] != {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                             "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"}:
        raise ValueError("receiving provider metadata drift")
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
    for name, expected in {**RECEIVER_OUTPUTS, **RECEIVER_CUSTODY}.items():
        path = repository / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("receiving current javac owner qualification source candidate output drift: " + name)


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3JavacConstantRegexCurrentOwnerQualificationRecipeTest":["allAdditionStatesAndOrdersReachExactFixedPoint","ownershipRefusesDriftMissingDuplicateAndPreservesUnrelated"]}
    if SUITES != expected_suites:
        raise AssertionError("sealed SDK report inventory drift")
    refused = 0
    for classname, methods in expected_suites.items():
        def suite() -> ET.Element:
            root = ET.Element("testsuite", {
                "tests": str(len(methods)), "failures": "0", "errors": "0", "skipped": "0",
            })
            for method in methods:
                ET.SubElement(root, "testcase", {"name": method, "classname": classname})
            return root

        def reject(root: ET.Element) -> None:
            nonlocal refused
            try:
                validate_report(ET.tostring(root, encoding="utf-8"), classname, methods)
            except ValueError:
                refused += 1
            else:
                raise AssertionError("malformed SDK report admitted")

        baseline = suite()
        if validate_report(ET.tostring(baseline, encoding="utf-8"), classname, methods) != {
                "tests": len(methods), "failures": 0, "errors": 0, "skipped": 0}:
            raise AssertionError("exact provider SDK report refused")
        for index, method in enumerate(methods):
            if method.endswith("(Path)"):
                bare = method[:-6]
                alternatives = (
                    bare, bare + "()", bare + "(String)", bare + "(java.nio.file.Path)",
                    bare + "(Path, Path)", bare + "(Path[])", method + "[1]",
                )
            else:
                alternatives = (method + "()", method + "(Path)", method + "[1]")
            for wrong_name in alternatives:
                changed = suite()
                changed.findall("testcase")[index].set("name", wrong_name)
                reject(changed)
        changed = suite()
        changed.findall("testcase")[-1].set("name", methods[0])
        reject(changed)
        changed = suite()
        changed.remove(changed.findall("testcase")[-1])
        reject(changed)
        changed = suite()
        ET.SubElement(changed, "testcase", {"name": "unreviewed", "classname": classname})
        reject(changed)
        changed = suite()
        changed.findall("testcase")[0].set("classname", classname + "Unreviewed")
        reject(changed)
        for tag in ("failure", "error", "skipped"):
            changed = suite()
            ET.SubElement(changed.findall("testcase")[0], tag)
            reject(changed)
        for field in ("tests", "failures", "errors", "skipped"):
            changed = suite()
            changed.set(field, str(len(methods) + 1) if field == "tests" else "1")
            reject(changed)
        changed = suite()
        changed.tag = "testsuites"
        reject(changed)
    expected_refusals = sum(
        sum(7 if method.endswith("(Path)") else 3 for method in methods) + 12
        for methods in expected_suites.values())
    if refused != expected_refusals:
        raise AssertionError("SDK report refusal coverage drift")
    print("M3_JAVAC_REGEX_CURRENT_OWNER_QUALIFICATION_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


def self_test(pins: dict, pins_data: bytes) -> None:
    self_test_reports()
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
            name = "src/test/java/com/synexia/rewrite/M3JavacConstantRegexCurrentOwnerQualificationRecipeTest.java"
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
    print("M3_JAVAC_REGEX_CURRENT_OWNER_QUALIFICATION_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inputs", action="store_true", help="verify sealed inputs (always performed)")
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
        print("M3_JAVAC_REGEX_CURRENT_OWNER_QUALIFICATION_RECEIVING_SDK_INPUTS_EXACT")
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
    emitted = ROOT / "target/emitted/javac-regex-current-owner-qualification"
    expected_files = set(RECEIVER_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("current-owner recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("current-owner recipe output drift: " + name)
    receipt = {
        "schema": "M3_JAVAC_REGEX_CURRENT_OWNER_QUALIFICATION_RECEIVING_SDK_RECEIPT_V1",
        "scope": "CURRENT_JAVAC_OWNER_QUALIFICATION_SOURCE_ONLY_FOUR_ADD_OUTPUTS_SIX_KEEP_INPUTS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "generatedQualificationOutputs": RECEIVER_OUTPUTS,
        "unchangedReceiverCustody": RECEIVER_CUSTODY,
        "fullJdkBuiltByThisTask": False,
        "jniRuntimeQualifiedByThisTask": False,
        "sdkQualifiesTransformationOnly": True,
        "standaloneMavenActivationExecutedByThisTask": False,
        "newSuccessorContractExecutedByThisTask": False,
        "scannerContractExecutedByThisTask": False,
        "oldPlanContractCompiledByFourSourceCommand": False,
        "sourceDescribedNewCharSequenceComparisons": 10800,
        "sourceDescribedNewAdditionalChecks": 11552,
        "sourceDescriptionsAreNotExecutedCounts": True,
        "realMIndexBridgeQualifiedByThisTask": False,
        "productionSourceChangedByThisModule": False,
        "productTargetAppliedByThisModule": False,
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
