#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact canonical-mapping workflow successor custody and named two-test evidence."""
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
SUITES = {"com.synexia.rewrite.M3CanonicalMappingHostedWorkflowRecipeTest":["canonicalMappingWorkflowEveryPartialStateAndOrder","canonicalMappingWorkflowRefusesDriftMissingAndDuplicateOwnership"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipe.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-canonical-mapping-hosted-qualification-20261017.yml","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/before/m3-first-release-admission.yml","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/out/m3-first-release-admission.yml","src/test/java/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipe.java":"adc9c59d8b9e28927b81489e93441054bbc98241","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/before/m3-first-release-admission.yml":"c8e9b91b4c61377eaaa27e603e2a970b920f19b7","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/manifest.tsv":"8e725208642000fabbf6277ac31a8d971782a2ca","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/out/m3-first-release-admission.yml":"189a9292a2150ef04df003ab07cb1febaed0f7f3","src/main/resources/META-INF/rewrite/m3-canonical-mapping-hosted-qualification-20261017.yml":"e792d521da8d112588fbf313a7413c0b972455f0","src/test/java/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipeTest.java":"047df9dd6d37d8ecc39e51905729d557d4a70b1f"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipe.java":"ab9f1e6ef3282bf35d11fab33e85b3c5cdb3a748589d5cf283b05d5b9d428450","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/before/m3-first-release-admission.yml":"689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/manifest.tsv":"6fbd0da9c8c770b75f2850b8e9c6313e4b1a77667997764fd4634f12a3a646a2","src/main/resources/com/synexia/rewrite/m3-canonical-mapping-hosted-qualification-20261017/out/m3-first-release-admission.yml":"88ca043886bf8fcc4c9cc5488ad23bdb17ac005187fe44e17794874dd626d18d","src/main/resources/META-INF/rewrite/m3-canonical-mapping-hosted-qualification-20261017.yml":"23ed4f90fa8e5dc53c989a86d2a5aba9e5ef267c5e0409270ebfae343574ba9f","src/test/java/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipeTest.java":"5a4c49fd99724b692f51d252b7edeb19938709a6b1d63cb5583a524058a7225c"}
RECEIVER_OUTPUTS = {".github/workflows/m3-first-release-admission.yml":"88ca043886bf8fcc4c9cc5488ad23bdb17ac005187fe44e17794874dd626d18d"}

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
    if pins["schema"] != "M3_CANONICAL_MAPPING_HOSTED_WORKFLOW_RECEIVING_SDK_INPUTS_V1":
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
            raise ValueError("receiving hosted compiler qualification workflow output drift: " + name)


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3CanonicalMappingHostedWorkflowRecipeTest":["canonicalMappingWorkflowEveryPartialStateAndOrder","canonicalMappingWorkflowRefusesDriftMissingAndDuplicateOwnership"]}
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
    print("M3_CANONICAL_MAPPING_HOSTED_WORKFLOW_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


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
            name = "src/test/java/com/synexia/rewrite/M3CanonicalMappingHostedWorkflowRecipeTest.java"
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
    print("M3_CANONICAL_MAPPING_HOSTED_WORKFLOW_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_CANONICAL_MAPPING_HOSTED_WORKFLOW_RECEIVING_SDK_INPUTS_EXACT")
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
    emitted = ROOT / "target/emitted/canonical-mapping-hosted-workflow"
    expected_files = set(RECEIVER_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("workflow recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256((emitted / name).read_bytes()).hexdigest() != digest:
            raise ValueError("workflow recipe output drift: " + name)
    receipt = {
        "schema": "M3_CANONICAL_MAPPING_HOSTED_WORKFLOW_RECEIVING_SDK_RECEIPT_V1",
        "scope": "ORIGINAL_PROVIDER_ONE_EXISTING_WORKFLOW_SUCCESSOR_OUTPUT",
        "predecessorWorkflowSha256": "689f6e970fedc702f8d31c36c840954bfdb0da3ca5e4b38de18dbb78a23fadb2",
        "expectedHostedSdkMethods": 14,
        "expectedHostedSupervisedCommands": 16,
        "wholeMigrationValidator": "NOT_RUN",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedWorkflowOutputs": RECEIVER_OUTPUTS,
        "policyOutputsAppliedByThisModule": False,
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
