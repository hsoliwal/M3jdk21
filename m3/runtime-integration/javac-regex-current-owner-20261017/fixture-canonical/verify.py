#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact exact compiler-owner fixture recovery SDK inputs and actual named five-test evidence."""
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
SUITES = {"com.synexia.rewrite.M3JavacRegexOwnerFixtureRecoveryRecipeTest":["fixtureRecoveryAddsExactOwnerAndReachesFixedPoint","fixtureRecoveryRefusesAllForeignAndAmbiguousInputs"],"com.synexia.rewrite.M3JavacConstantRegexPrecomputeRecipeTest":["exactCompilerOwnerAllowsSevenAdditionsAndOneStableReplay","missingOwnerDriftDuplicateAndForeignCandidatesRefuseWholeBatch","unrelatedSourceNeverReceivesMutation"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipe.java","pinned-engine/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-javac-regex-owner-fixture-recovery-20261017.yml","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/MIndexCompilerASTPrecompute.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/MIndexCompilerASTPrecompute.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/M3JavacConstantRegexPrecomputeRecipeTest.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/M3JavacConstantRegexPrecomputeRecipeTest.java","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/verify_source_seals.py","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/verify_source_seals.py","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/compiler-owner-before.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/00-MIndexJdkConstantRegexPlan.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/01-MIndexJdkConstantRegexPlanContract.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/02-MIndexJdkConstantRegexPlanTest.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/03-MIndexJavaConstantStringFacts.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/04-MIndexJavaConstantStringFactsContract.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/05-MIndexJavaConstantStringFactsTest.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/06-M3JavacConstantRegexPrecomputeRecipe.java.txt","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt","src/test/java/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipeTest.java","src/test/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipe.java":"7c49bc0d1472f48386ed52476840079c4a488bde","pinned-engine/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipe.java":"ae71aa9c15f91413dfa0efc4ec9d0562b2083a69","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f","src/main/resources/META-INF/rewrite/m3-javac-regex-owner-fixture-recovery-20261017.yml":"3a67fc4fd61c6da1043b8dd0cd0a1e2606dfaa43","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/MIndexCompilerASTPrecompute.java":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/MIndexCompilerASTPrecompute.java":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/M3JavacConstantRegexPrecomputeRecipeTest.java":"2fd1927532cf27818a44024199256aec776b1df4","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/M3JavacConstantRegexPrecomputeRecipeTest.java":"2fd1927532cf27818a44024199256aec776b1df4","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/manifest.tsv":"2ea8bbbaf7a93decb09f1bf4ecc756f2101892b8","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/manifest.tsv":"2ea8bbbaf7a93decb09f1bf4ecc756f2101892b8","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/verify_source_seals.py":"c8fafe24441a138e246068b4efac5c791bf359d0","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/verify_source_seals.py":"c8fafe24441a138e246068b4efac5c791bf359d0","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/compiler-owner-before.java.txt":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/manifest.tsv":"5282767a150e40b7f789196d14d2345e099feb4e","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/00-MIndexJdkConstantRegexPlan.java.txt":"aef73f8b743dcc193e528f69748443a1810a1f1c","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/01-MIndexJdkConstantRegexPlanContract.java.txt":"92d77554ea1a8a242bc7a4b891a6af6a56bb4431","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/02-MIndexJdkConstantRegexPlanTest.java.txt":"70512a3c53fa5b791147086d0e491475e6caf3b6","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/03-MIndexJavaConstantStringFacts.java.txt":"0003406b4d62499e919a9e5a1be0853906225158","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/04-MIndexJavaConstantStringFactsContract.java.txt":"901db786b0ad9306f2750524255b8bb8046f05e4","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/05-MIndexJavaConstantStringFactsTest.java.txt":"3fe69b3c580f8a2544b4a096e9b34d18cea32122","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/06-M3JavacConstantRegexPrecomputeRecipe.java.txt":"7c49bc0d1472f48386ed52476840079c4a488bde","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/manifest.tsv":"2ea8bbbaf7a93decb09f1bf4ecc756f2101892b8","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt":"1ac623b8fa822f4f8ce9ed9c3bb8c9a5194184b6","src/test/java/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipeTest.java":"3dfe38f59be2dcc30071cfceddd264afc50dff0e","src/test/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipeTest.java":"2fd1927532cf27818a44024199256aec776b1df4"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipe.java":"076be555dba3719f342b9259695c24cea887d66b7ce72827c74a361ed4680cb1","pinned-engine/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipe.java":"5dae6fd43b6b356b032de69616f4d9562680c26b3c22847152150c1a6c693972","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e","src/main/resources/META-INF/rewrite/m3-javac-regex-owner-fixture-recovery-20261017.yml":"75b56891c033f8f0c5034de695f7520a29dbb2e7352eb52c07491f8fb2ef5988","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/MIndexCompilerASTPrecompute.java":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/MIndexCompilerASTPrecompute.java":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/M3JavacConstantRegexPrecomputeRecipeTest.java":"3e19e63b38fb97b1964c050d422c6494a7acc3b46cda11b0da1cb3e09ddefb79","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/M3JavacConstantRegexPrecomputeRecipeTest.java":"3e19e63b38fb97b1964c050d422c6494a7acc3b46cda11b0da1cb3e09ddefb79","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/manifest.tsv":"c623d462fa245abc1d3b964a908dac5bd7a2968703e507c64e2da587af4a49bc","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/manifest.tsv":"c623d462fa245abc1d3b964a908dac5bd7a2968703e507c64e2da587af4a49bc","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/before/verify_source_seals.py":"e4b4e7a2c4efec34661bca472c61ee164a75432d0781eccc0c1037e36f793b3e","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/verify_source_seals.py":"e4b4e7a2c4efec34661bca472c61ee164a75432d0781eccc0c1037e36f793b3e","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/out/compiler-owner-before.java.txt":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","src/main/resources/com/synexia/rewrite/m3-javac-regex-owner-fixture-recovery-20261017/manifest.tsv":"f5c254057374cb429c8a6a80bffdb41977255d0362956bb2521ccfe76ddcda9f","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/00-MIndexJdkConstantRegexPlan.java.txt":"c5c0569b5e7d1d5a0abbf76e2188c14024318a34ae2bc05c13567f7a3e24b4d2","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/01-MIndexJdkConstantRegexPlanContract.java.txt":"cdd2d4704c647adbd5e6888a4228355a53b6dd7dc400d0e86fc04673e760fddf","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/02-MIndexJdkConstantRegexPlanTest.java.txt":"7b0842c62366ce830efd9a252f16a67f4d42550a1d6ec63b4b0a883065c93f4b","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/03-MIndexJavaConstantStringFacts.java.txt":"85e36a76dc85b75fda2d3e8d4577a73d586c33ce26ef04367c9236cda62425dd","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/04-MIndexJavaConstantStringFactsContract.java.txt":"12d064b6e11a9ca3c670340890a22e699125ded1e051a18407f085adf53c76f2","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/05-MIndexJavaConstantStringFactsTest.java.txt":"c0fe48f169df924c5f05200613c4770a7a5bffe8b99c24916bf2a19099d88144","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/06-M3JavacConstantRegexPrecomputeRecipe.java.txt":"076be555dba3719f342b9259695c24cea887d66b7ce72827c74a361ed4680cb1","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/manifest.tsv":"c623d462fa245abc1d3b964a908dac5bd7a2968703e507c64e2da587af4a49bc","src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834","src/test/java/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipeTest.java":"189bb7996c5de9c630729db5b5c32b83728b00255224e74cbee3908bfeb7cfb0","src/test/java/com/synexia/rewrite/M3JavacConstantRegexPrecomputeRecipeTest.java":"3e19e63b38fb97b1964c050d422c6494a7acc3b46cda11b0da1cb3e09ddefb79"}
RECEIVER_OUTPUTS = {"synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/m3-javac-constant-regex/compiler-owner-before.java.txt":"74dd92f6a378763e040dc868a267f06b6c9af035d26fbfad0ac1224c3438a834"}

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
    if pins["schema"] != "M3_JAVAC_REGEX_OWNER_FIXTURE_RECOVERY_RECEIVING_SDK_INPUTS_V1":
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
    for name, expected in RECEIVER_OUTPUTS.items():
        path = repository / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("receiving exact compiler-owner fixture recovery output drift: " + name)


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3JavacRegexOwnerFixtureRecoveryRecipeTest":["fixtureRecoveryAddsExactOwnerAndReachesFixedPoint","fixtureRecoveryRefusesAllForeignAndAmbiguousInputs"],"com.synexia.rewrite.M3JavacConstantRegexPrecomputeRecipeTest":["exactCompilerOwnerAllowsSevenAdditionsAndOneStableReplay","missingOwnerDriftDuplicateAndForeignCandidatesRefuseWholeBatch","unrelatedSourceNeverReceivesMutation"]}
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
    print("M3_JAVAC_REGEX_OWNER_FIXTURE_RECOVERY_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


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
            name = "src/test/java/com/synexia/rewrite/M3JavacRegexOwnerFixtureRecoveryRecipeTest.java"
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
    print("M3_JAVAC_REGEX_OWNER_FIXTURE_RECOVERY_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_JAVAC_REGEX_OWNER_FIXTURE_RECOVERY_RECEIVING_SDK_INPUTS_EXACT")
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
    emitted = ROOT / "target/emitted/javac-regex-owner-fixture-recovery"
    expected_files = set(RECEIVER_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("fixture recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("fixture recipe output drift: " + name)
    receipt = {
        "schema": "M3_JAVAC_REGEX_OWNER_FIXTURE_RECOVERY_RECEIVING_SDK_RECEIPT_V1",
        "scope": "ORIGINAL_PROVIDER_ONE_ADDED_COMPILER_FIXTURE_AND_THREE_UNCHANGED_ORIGINAL_JUNIT_TESTS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedFixtureOutputs": RECEIVER_OUTPUTS,
        "policyOutputsAppliedByThisModule": False,
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
