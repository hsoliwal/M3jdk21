#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact current-owner regex relation source candidate SDK inputs and actual named three-test evidence."""
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
SUITES = {"jdk.internal.m3.rewrite.M3RegexRelationsCurrentOwnerRecipeTest":["allPartialStatesAndOrdersReachExactFixedPoint","eachOwnedInputRefusesDriftMissingAndDuplicateOwnership","currentOwnerMembersAndCustodyRemainExact"]}
REQUIRED_PATHS = ["pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinManifest.java","pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-regex-relations-current-owner-20261017.yml","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/manifest.tsv","src/test/java/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipeTest.java","FOSS_REUSE_DECISION.tsv","THIRD_PARTY_NOTICES.md","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringRegexDp.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringRegexDp.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3DotStarRelations.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3DotStarRelations.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.c","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.c","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringHierarchicalRegexTest.java","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringHierarchicalRegexTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinManifest.java":"78d5fb68f13872164973a8918cc7886c5f548bca","pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinRecipe.java":"2ec479b8609459a28fdfd8a7a7d63cc44e919f00","pinned-engine/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipe.java":"12558533f03ad408b570b71fa3da3489196cf41a","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f","src/main/resources/META-INF/rewrite/m3-regex-relations-current-owner-20261017.yml":"97e07b77e9358e03732b2dae2e3dd5f5035af668","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/manifest.tsv":"7b6e647d5d969a01cd9261708250e4fd17f0154e","src/test/java/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipeTest.java":"53861074f20a1ad6e1c0ca7cb988443c1085826a","FOSS_REUSE_DECISION.tsv":"8f2df6115956754bbb6ad97da5a83af3b4e1af7a","THIRD_PARTY_NOTICES.md":"e8f456a849993c5f4b5d8fec69ebc5bf90aeabe5","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringRegexDp.java":"0f99affbec7a0d995e0a18ac4cf7f2e815da6e3b","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringRegexDp.java":"3fe2940825734a508967b559664c74bffd673d58","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3DotStarRelations.java":"d70a152bdedc380ed8b28861ea94ef4c2adbea90","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3DotStarRelations.java":"d70a152bdedc380ed8b28861ea94ef4c2adbea90","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.java":"30359b57b7b553e78355d3672119c685051ec1da","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.java":"30359b57b7b553e78355d3672119c685051ec1da","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.c":"89b3127f0f262de3e00ba28a01079a659013dce2","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.c":"89b3127f0f262de3e00ba28a01079a659013dce2","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringHierarchicalRegexTest.java":"5d47787597630efc70a4e3b173f2e788dfb6aa3a","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringHierarchicalRegexTest.java":"5228849e52b921e24731a42b6e3074ba76ac0aba"}
REQUIRED_SHA256 = {"pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinManifest.java":"c874b5d4b5fe8886d53763ab91d050f431c4b7c09d3fe34d58680231ccb1cf7e","pinned-engine/jdk/internal/m3/rewrite/M3GpuToolJoinRecipe.java":"300a520ec9d273922d1edbdf5442e5faf825c47e8e3df7febf4baa2f39945401","pinned-engine/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipe.java":"78e0bc6f658791eaa0022ab382815934a0efc9fd73ee0e5d54fca7633a3aba5f","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e","src/main/resources/META-INF/rewrite/m3-regex-relations-current-owner-20261017.yml":"968d0625c70a9c42e1652ddd3ff3ab2be912ddcaa636f764d0994268e8925fe6","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/manifest.tsv":"c48ef38513d29e2077f592541b8fda394101825f523146ec1eb00189786d1ba6","src/test/java/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipeTest.java":"914b5e9c1a420f8eec8265628c147649f394b4b0201d251fe522a83536f95e7b","FOSS_REUSE_DECISION.tsv":"b30be2a01f7b24b71a6aa0cd1687cdfed9fad002c9cf0452ba8758b7213bba96","THIRD_PARTY_NOTICES.md":"9be905c3a3d3e20a38b0a92bde462be4803cecc83b6d9785fdf527581d25af9a","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringRegexDp.java":"ca679153b93b33214e48c3d7cce4414cf9622ff380ee9aea3334c9a680f37779","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringRegexDp.java":"471ffbf34b6046b5bc811783397e56bd57f94fadd1883680e39192ea08ce486e","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3DotStarRelations.java":"9d485a1771a30b4229acb5526ca90de5f2df4a03ee5ad8f5388bb128d2c38194","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3DotStarRelations.java":"9d485a1771a30b4229acb5526ca90de5f2df4a03ee5ad8f5388bb128d2c38194","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.java":"ae1de4b652b474c594d5fa6e8ec2d43aae720d166e885497217688c9fabae49d","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.java":"ae1de4b652b474c594d5fa6e8ec2d43aae720d166e885497217688c9fabae49d","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StateRelation.c":"0df1989292b23b1f185f222db61ad55d5e38b30d5b758436ca9d62b8de277bbd","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StateRelation.c":"0df1989292b23b1f185f222db61ad55d5e38b30d5b758436ca9d62b8de277bbd","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/before/M3StringHierarchicalRegexTest.java":"1653c23098529c2fc9f7e66e1bfb5c5c2c97adc67c422b0e67cfbd7b5fb121fd","src/main/resources/jdk/internal/m3/rewrite/m3-regex-relations-current-owner-20261017/out/M3StringHierarchicalRegexTest.java":"69c725cb7cfe13b0a02bd6419cba2566f9c6b6ac30823d336765cbcebabbe601"}
RECEIVER_OUTPUTS = {"src/java.base/share/classes/java/lang/M3StringRegexDp.java":"471ffbf34b6046b5bc811783397e56bd57f94fadd1883680e39192ea08ce486e","test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java":"69c725cb7cfe13b0a02bd6419cba2566f9c6b6ac30823d336765cbcebabbe601"}
RECEIVER_CUSTODY = {"src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java":"9d485a1771a30b4229acb5526ca90de5f2df4a03ee5ad8f5388bb128d2c38194","src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java":"ae1de4b652b474c594d5fa6e8ec2d43aae720d166e885497217688c9fabae49d","src/java.base/share/native/libjava/M3StateRelation.c":"0df1989292b23b1f185f222db61ad55d5e38b30d5b758436ca9d62b8de277bbd"}

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
    if pins["schema"] != "M3_REGEX_RELATIONS_CURRENT_OWNER_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    if pins["providers"] != {"openrewriteAdapter": "2ec479b8609459a28fdfd8a7a7d63cc44e919f00",
                             "manifestKernel": "78d5fb68f13872164973a8918cc7886c5f548bca"}:
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
            raise ValueError("receiving current-owner regex relation source candidate output drift: " + name)


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"jdk.internal.m3.rewrite.M3RegexRelationsCurrentOwnerRecipeTest":["allPartialStatesAndOrdersReachExactFixedPoint","eachOwnedInputRefusesDriftMissingAndDuplicateOwnership","currentOwnerMembersAndCustodyRemainExact"]}
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
    print("M3_REGEX_RELATIONS_CURRENT_OWNER_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


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
                "pinned-engine/jdk/internal/m3/rewrite/nested/Extra.java",
                "pinned-engine/other/Extra.java",
                "src/test/java/other/ExtraTest.java",
                "src/test/resources/jdk/internal/m3/rewrite/hash-pinned-java/"
                "m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt",
                "src/main/resources/jdk/internal/m3/rewrite/hash-pinned-java/"
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
            name = "src/test/java/jdk/internal/m3/rewrite/M3RegexRelationsCurrentOwnerRecipeTest.java"
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
    print("M3_REGEX_RELATIONS_CURRENT_OWNER_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_REGEX_RELATIONS_CURRENT_OWNER_RECEIVING_SDK_INPUTS_EXACT")
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
    emitted = ROOT / "target/emitted/regex-relations-current-owner"
    expected_files = set(RECEIVER_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("current-owner recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("current-owner recipe output drift: " + name)
    receipt = {
        "schema": "M3_REGEX_RELATIONS_CURRENT_OWNER_RECEIVING_SDK_RECEIPT_V1",
        "scope": "CURRENT_OWNER_SOURCE_ONLY_TWO_OUTPUTS_THREE_KEEP_INPUTS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "2ec479b8609459a28fdfd8a7a7d63cc44e919f00",
                      "manifestKernel": "78d5fb68f13872164973a8918cc7886c5f548bca"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedProductSourceOutputs": RECEIVER_OUTPUTS,
        "unchangedReceiverCustody": RECEIVER_CUSTODY,
        "fullJdkBuiltByThisTask": False,
        "jniRuntimeQualifiedByThisTask": False,
        "javaParseOnlyIsNotCompilation": True,
        "productTargetAppliedByThisModule": False,
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
