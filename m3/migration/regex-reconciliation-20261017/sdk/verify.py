#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify target-native canonical JSON recipe custody and three actual named SDK tests."""
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
SUITES = {"com.m3.rewrite.backport.M3CanonicalRegexMappingReconciliationTest":["completeChangePreservesContractsAndReachesFixedPoint","serialStagesAndSealedRollbackRestoreOwnedStates","everyNamedOperationRejectsForeignMissingAndDuplicateOwners"]}
REQUIRED_PATHS = ["FOSS_REUSE_DECISION.tsv","THIRD_PARTY_NOTICES.md","pinned-engine/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-canonical-regex-reconciliation-20261017.yml","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/append-insertion.txt","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/historical-sparse-manifest.tsv","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/migration.py","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/name-mapping.schema.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/records.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/after.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/manifest.tsv","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/after.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/before.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/manifest.tsv","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/before.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/manifest.tsv","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/after.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/manifest.tsv","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/after.json","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/manifest.tsv","src/test/java/com/m3/rewrite/backport/M3CanonicalRegexMappingReconciliationTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"FOSS_REUSE_DECISION.tsv":"05cdea4750e4e53d2960e2d2661d0f642f51ae48","THIRD_PARTY_NOTICES.md":"e3399878d6c98621b788a8a72909071d867f89fe","pinned-engine/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java":"4a38e39d0755462d40c2b22548b87abde938ea0b","pom.xml":"1a004d03190d0d0d4d5a827870522cc22945b544","src/main/resources/META-INF/rewrite/m3-canonical-regex-reconciliation-20261017.yml":"0b93d77b45d8b54ca03d591b35158248ffc06445","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/append-insertion.txt":"6f4391c8752e3b6303872fdb572f3b3d3fad5df0","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/historical-sparse-manifest.tsv":"2fdaa30d27463bb9e9bff5064cbd4015571d3aa9","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/migration.py":"d976f4a5d7f8983ade85d5d2cf608e3f6b2885d7","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/name-mapping.schema.json":"40f032174baa2f1c19e3b02686723bc37f37122c","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/records.json":"614599dadccb048b2e8422b744e668b28a45b57b","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/after.json":"5b423f474830923ce816a528e172ac96757cb99a","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/manifest.tsv":"a4ffa10c9908f31dc7d3943f1ea924d9f7639ecf","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/after.json":"188b06b9e25edb3373825d59d12b97ad1d2d1dce","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/before.json":"a44460f982940623cf6ba1ca625caeeaa889f633","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/manifest.tsv":"30d96e76e4083039e0fd034701cc0e2302101690","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/before.json":"a44460f982940623cf6ba1ca625caeeaa889f633","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/manifest.tsv":"a0e5526fe7ef367b516f1c1e9f961dbd14aab33f","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/after.json":"188b06b9e25edb3373825d59d12b97ad1d2d1dce","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/manifest.tsv":"8486d22919e348ad343edb366c4dc978a4b3463b","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/after.json":"e504ecfb8991cf3f0f10406b2b3dd356ed70fa67","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/manifest.tsv":"262f6423b8c26c2ea3e77cffc6ff43159a79396a","src/test/java/com/m3/rewrite/backport/M3CanonicalRegexMappingReconciliationTest.java":"8d16e2a413ec2501a9d268946c86de303df92b68"}
REQUIRED_SHA256 = {"FOSS_REUSE_DECISION.tsv":"fc669ae108a0966d8d8dcc9db54a220ef406bf977e0ccb73025d37585dffcaf7","THIRD_PARTY_NOTICES.md":"775048bd90ffb6556f6cfe8e08456b901f64aee35b8159077031389bc891212f","pinned-engine/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java":"8133bcf7e9f750c714cdd42784bdf30b60f67dd957b2831cd54cb01c9a2cfc3a","pom.xml":"0594ebcd36985db4ff06d89ea7f1d6f56fc39cfa1b6f0f3b754b7189253685d6","src/main/resources/META-INF/rewrite/m3-canonical-regex-reconciliation-20261017.yml":"99f88fdedad45524bf17407631594e085863423667c0a612055b57998768fc6c","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/append-insertion.txt":"169e82b5c6c44637c427c6b3a171fe17e74598dc20394d405c43db5d5b6d9645","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/historical-sparse-manifest.tsv":"725a5dbccbfa4ff834586c21f9c495fffec985dd9d9033f86fc6e808ea421fa2","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/migration.py":"fd54e5fade1fc3050816265f2317967dd4176e7bb5e0bc55e6f620878026e88e","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/name-mapping.schema.json":"c29f34e5b5d858b60cddd43b2e7f273d9c01ad553cb0e3a5dcc568698646f70b","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/custody/records.json":"e8931d4ffac80339a5b3989c36eb0968ccce302333a9ee7d178fd66d425c470a","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/after.json":"daad1f0947f85d88179d6653fcffc5b66070c27f22bce9bf8853e076a45b60aa","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-append-20261017/manifest.tsv":"1af339ddeedd233f37f26727324e669a807961c2ce6f7f27d19419caa9b06452","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/after.json":"d9f17863e6325f7085967152ea725dfa9ab1b3b97ed21ee221a006e74fc10091","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/before.json":"fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-reconciliation-20261017/manifest.tsv":"6ac6d481178067aee78863331426c124c2360f2b4def5cff244aad39d8ed6ea6","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/before.json":"fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-regex-family-rollback-20261017/manifest.tsv":"f01fcd4c197075df6529abc3a9879454a7fc508751df1b7d580f4ad4dedc2093","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/after.json":"d9f17863e6325f7085967152ea725dfa9ab1b3b97ed21ee221a006e74fc10091","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-sparse-recipe-custody-20261017/manifest.tsv":"1ab4bab6c3228cf9887a038a349f394e30033f66559184c3afa0e48eacae167b","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/after.json":"d115f231d02264036402920be8427f50a05661cd9ea3dbf0794c67c0ab8e10ab","src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/m3-tq-target-lineage-20261017/manifest.tsv":"5435e84cd6a3f9af4c6525ee7019009c1c0dc89a36b52ad70d1e6665ca169e0d","src/test/java/com/m3/rewrite/backport/M3CanonicalRegexMappingReconciliationTest.java":"fc951a4f088cfa701263aa87b5f64ce980f2efd25a9af310364a2625a9899d0a"}
RECEIVER_OUTPUTS = {"m3/docs/name-mapping.json":"d9f17863e6325f7085967152ea725dfa9ab1b3b97ed21ee221a006e74fc10091"}
EMITTED_OUTPUTS = {"full/m3/docs/name-mapping.json":"d9f17863e6325f7085967152ea725dfa9ab1b3b97ed21ee221a006e74fc10091","append/m3/docs/name-mapping.json":"daad1f0947f85d88179d6653fcffc5b66070c27f22bce9bf8853e076a45b60aa","lineage/m3/docs/name-mapping.json":"d115f231d02264036402920be8427f50a05661cd9ea3dbf0794c67c0ab8e10ab","recipe-custody/m3/docs/name-mapping.json":"d9f17863e6325f7085967152ea725dfa9ab1b3b97ed21ee221a006e74fc10091","rollback/m3/docs/name-mapping.json":"fa9832d48ec7dc0334fcb3a02e1b665612ca3a9f3dfe13cd104ff3f2c42871ac"}
REPOSITORY_KEEP = {"m3/docs/name-mapping.schema.json":"c29f34e5b5d858b60cddd43b2e7f273d9c01ad553cb0e3a5dcc568698646f70b","m3/migration/migration.py":"fd54e5fade1fc3050816265f2317967dd4176e7bb5e0bc55e6f620878026e88e","src/java.base/share/classes/jdk/internal/mindex/M3TQ.java":"78aa154b8aa25134edd9a16c746e4fdc7bcd3984b8f76cc96ef81437247e08d4","src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java":"bbc2901ebe5b2de9951b672127bd47e9d13c70b5a39482fa0727703d0a2ff757","src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java":"3270f7889a8c94fa15c90e88491b7fbe483d5ad3053ab36d7e07a63c4c734caa","src/java.base/share/native/libjava/M3StateRelation.c":"b1bfd040a1454ac06ab3f96952d97c8c5a6509dcf55dbba9797b6517eea2ef16","src/java.base/share/native/libjava/m3_state_relation.h":"c948c0ac34180f1f9412a764c3c9fd8c59e11509467aa9e04f8825e903526885","src/java.base/share/classes/java/lang/M3StringRegexDp.java":"9a5d9511b996d780f26672e2f5dc6ccc0a2747fe33f0c5ef1a9c62629564a41e","test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java":"1653c23098529c2fc9f7e66e1bfb5c5c2c97adc67c422b0e67cfbd7b5fb121fd"}

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
    if pins["schema"] != "M3_CANONICAL_REGEX_MAPPING_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    if pins["providers"] != {"hashPinnedTextSnapshot": "4a38e39d0755462d40c2b22548b87abde938ea0b"}:
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
    for name, expected in {**REPOSITORY_KEEP, **RECEIVER_OUTPUTS}.items():
        path = repository / name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
            raise ValueError("canonical mapping or immutable receiving custody drift: " + name)


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.m3.rewrite.backport.M3CanonicalRegexMappingReconciliationTest":["completeChangePreservesContractsAndReachesFixedPoint","serialStagesAndSealedRollbackRestoreOwnedStates","everyNamedOperationRejectsForeignMissingAndDuplicateOwners"]}
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
    print("M3_CANONICAL_REGEX_MAPPING_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


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
                "pinned-engine/com/m3/rewrite/backport/nested/Extra.java",
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
            name = "src/test/java/com/m3/rewrite/backport/M3CanonicalRegexMappingReconciliationTest.java"
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
    print("M3_CANONICAL_REGEX_MAPPING_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_CANONICAL_REGEX_MAPPING_RECEIVING_SDK_INPUTS_EXACT")
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
    emitted = ROOT / "target/emitted/canonical-map"
    expected_files = set(EMITTED_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("canonical mapping recipe output inventory drift")
    for name, digest in EMITTED_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("canonical mapping recipe output drift: " + name)
    receipt = {
        "schema": "M3_CANONICAL_REGEX_MAPPING_RECEIVING_SDK_RECEIPT_V1",
        "scope": "TARGET_NATIVE_ADAPTER_METADATA_ONLY_THREE_SDK_TESTS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"hashPinnedTextSnapshot": "4a38e39d0755462d40c2b22548b87abde938ea0b"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedMappingOutputs": EMITTED_OUTPUTS,
        "wholeMigrationValidator": "NOT_RUN",
        "matchedJdkJtreg": "NOT_RUN",
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
