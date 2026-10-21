#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact current joined-range directory SDK inputs and actual named source-only replay evidence."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import tempfile
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
RESOURCE = "src/main/resources/com/synexia/rewrite/joined-range-directory-20261020"
SUITE = "com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest"
METHODS = ["allTwoPartialStatesBothOrdersReachExactFixedPoint","allTwoInverseStatesBothOrdersRecoverExactBefore","eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]
SUITES = {SUITE: METHODS}
CHANGE_PATHS = ["m3/core/src/com/m3/text/JoinedM3StringPiece.java"]
KEEP_PATHS = ["m3/core/src/com/m3/text/M3StringPiece.java","m3/core/src/com/m3/text/LocalM3StringPiece.java","m3/core/src/com/m3/text/LocalM3Arena.java","m3/core/src/com/m3/text/StorageIdentity.java"]
TARGET_PATHS = CHANGE_PATHS + KEEP_PATHS
OWNER_BEFORE_GIT = {
    "m3/core/src/com/m3/text/JoinedM3StringPiece.java": "3edc23c22362b08af00f7332ddc445a0274a6dd4",
    "m3/core/src/com/m3/text/M3StringPiece.java": "99cb72623fa6bc2d5426553e04aaf0e5bfa993cc",
    "m3/core/src/com/m3/text/LocalM3StringPiece.java": "6b745c9472144ad6de01de4d06a533974e558fb1",
    "m3/core/src/com/m3/text/LocalM3Arena.java": "d82710cd1a49b54d92afcd74cdd903cd009766ba",
    "m3/core/src/com/m3/text/StorageIdentity.java": "7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a"
}
OWNER_AFTER_GIT = {
    "m3/core/src/com/m3/text/JoinedM3StringPiece.java": "4f88f794266375ad8174fa56e952c30d32689877",
    "m3/core/src/com/m3/text/M3StringPiece.java": "99cb72623fa6bc2d5426553e04aaf0e5bfa993cc",
    "m3/core/src/com/m3/text/LocalM3StringPiece.java": "6b745c9472144ad6de01de4d06a533974e558fb1",
    "m3/core/src/com/m3/text/LocalM3Arena.java": "d82710cd1a49b54d92afcd74cdd903cd009766ba",
    "m3/core/src/com/m3/text/StorageIdentity.java": "7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a"
}
DIRECTIONS = ("forward", "inverse")
RECEIVER_OUTPUTS: dict[str, str] = {}
RECEIVER_CUSTODY: dict[str, str] = {}
DESCRIPTOR = "src/main/resources/META-INF/rewrite/joined-range-directory-20261020.yml"
TEST = "src/test/java/com/synexia/rewrite/M3JoinedRangeDirectoryRecipeTest.java"
REQUIRED_PATHS = sorted([
    "pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java",
    "pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java",
    "pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryRecipe.java",
    "pinned-engine/com/synexia/rewrite/M3JoinedRangeDirectoryInverseRecipe.java",
    "pom.xml", DESCRIPTOR, TEST, "verify.py", "src/test/resources/com/synexia/rewrite/joined-range-directory-20261020/proof/M3JoinedFreshRangeProof.java.txt",
    "FOSS_REUSE_DECISION.tsv", "THIRD_PARTY_NOTICES.md", "README.md",
    *[RESOURCE + "/" + direction + "/manifest.tsv" for direction in DIRECTIONS],
    *[RESOURCE + "/" + direction + "/" + stage + "/" + name
      for direction in DIRECTIONS for stage in ("before", "out") for name in TARGET_PATHS],
])
ORIGINAL_GIT = {
    "pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
    "pom.xml": "d79867bcc09ad6329e12418e15907e54b8ab025f",
}
PROVIDERS = {"openrewriteAdapter": ORIGINAL_GIT["pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java"],
             "manifestKernel": ORIGINAL_GIT["pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java"]}
COMPILED_PROVIDERS = ("M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                      "M3JoinedRangeDirectoryRecipe", "M3JoinedRangeDirectoryInverseRecipe")
FALSE_GATES = ("targetJavaCompiled", "originalPrivateOwnerModuleQualified", "originalPrivateModelQualified",
               "receiverRuntimeQualified", "actualAppliedReceiverComponentQualified",
               "fullControlReactorQualified", "jniRuntimeQualified", "fullMatchedJdkQualified",
               "performanceRetentionQualified", "productTargetApplied", "runtimeApplied", "phaseAdvanced")


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def safe(name: str) -> str:
    path = PurePosixPath(name)
    require(bool(name) and not path.is_absolute() and "\\" not in name and ":" not in name
            and "\0" not in name and all(part not in ("", ".", "..") for part in name.split("/")),
            "invalid sealed SDK path")
    return name


def regular(path: Path) -> Path:
    require(path.is_file() and all(not item.is_symlink() for item in (path, *path.parents)),
            "missing or redirected SDK input: " + str(path))
    return path


def digest(raw: bytes) -> str:
    return hashlib.sha256(raw).hexdigest()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def source_inventory() -> set[str]:
    found = set()
    def refuse(error):
        raise ValueError("SDK source inventory failed") from error
    for folder, directories, files in os.walk(ROOT, followlinks=False, onerror=refuse):
        base = Path(folder)
        if base == ROOT:
            directories[:] = [name for name in directories if name != "target"]
        for name in directories:
            require(not (base / name).is_symlink(), "redirected SDK source directory")
        for name in files:
            path = regular(base / name)
            found.add(safe(path.relative_to(ROOT).as_posix()))
            require(len(found) <= 64, "SDK input inventory budget")
    return found




def manifests() -> tuple[dict[str, str], dict[str, str]]:
    admitted, bodies = {}, {}
    for direction in DIRECTIONS:
        root = ROOT / RESOURCE / direction
        rows = regular(root / "manifest.tsv").read_bytes().decode("utf-8").splitlines()
        require(len(rows) == 5, "five exact manifest rows required")
        current = {}
        for line in rows:
            cells = line.split("\t")
            require(len(cells) == 4, "four-cell manifest row required")
            name, before, after, payload = cells
            require(name in TARGET_PATHS and name not in current, "manifest target ownership drift")
            require(re.fullmatch(r"[0-9a-f]{64}", before) is not None
                    and re.fullmatch(r"[0-9a-f]{64}", after) is not None, "full source hashes required")
            old = regular(root / "before" / safe(name)).read_bytes()
            new = regular(root / "out" / safe(name)).read_bytes()
            require(digest(old) == before and digest(new) == after, "manifest fixture drift: " + name)
            old_git, new_git = ((OWNER_BEFORE_GIT, OWNER_AFTER_GIT) if direction == "forward"
                                else (OWNER_AFTER_GIT, OWNER_BEFORE_GIT))
            require(git_blob(old) == old_git[name] and git_blob(new) == new_git[name],
                    "authentic current fixture blob required: " + name)
            if name in KEEP_PATHS:
                require(before == after and old == new and payload == "KEEP", "unchanged owner drift")
            else:
                require(before != after and payload == "out/" + name, "one reviewed range-routing replacement required")
            current[name] = (before, after)
            bodies[(direction, "before", name)] = old
            bodies[(direction, "out", name)] = new
        require(set(current) == set(TARGET_PATHS), "complete five-owner closure required")
        admitted[direction] = current
    for name in TARGET_PATHS:
        require(admitted["forward"][name] == tuple(reversed(admitted["inverse"][name])),
                "forward/inverse hash contract differs")
        require(bodies[("forward", "before", name)] == bodies[("inverse", "out", name)]
                and bodies[("forward", "out", name)] == bodies[("inverse", "before", name)],
                "full inverse must recover exact original bytes")
    return ({direction + "/" + name: admitted[direction][name][1]
             for direction in DIRECTIONS for name in CHANGE_PATHS},
            {name: admitted["forward"][name][0] for name in KEEP_PATHS})

def inputs() -> tuple[dict, bytes]:
    pins_data = regular(ROOT / "pins.json").read_bytes()
    pins = json.loads(pins_data)
    require(pins["schema"] == "M3_JOINED_FRESH_OWNED_RANGE_SDK_INPUTS_V1"
            and pins["providers"] == PROVIDERS and pins["sourceOnly"] is True
            and pins["admissionCommit"] == "80253b14a14191ad826a5c7c7594100c7c3c588a",
            "sealed joined-range directory SDK identity drift")
    rows = pins["files"]
    require(len(rows) == len(REQUIRED_PATHS) == 34
            and {row["path"] for row in rows} == set(REQUIRED_PATHS),
            "complete closed joined-range directory SDK input set required")
    seen = set()
    for row in rows:
        name = safe(row["path"])
        require(name not in seen, "duplicate SDK input pin")
        seen.add(name)
        data = regular(ROOT / name).read_bytes()
        require(len(data) == row["bytes"] and digest(data) == row["sha256"]
                and git_blob(data) == row["gitBlob"], "sealed joined-range directory SDK source drift: " + name)
    require(source_inventory() == set(REQUIRED_PATHS) | {"pins.json"},
            "unsealed SDK source file")
    for name, expected in ORIGINAL_GIT.items():
        require(git_blob(regular(ROOT / name).read_bytes()) == expected,
                "unchanged qualified provider/POM required")
    outputs, custody = manifests()
    # Mutate stable objects: runpy callers retain the same exported dictionary instances.
    RECEIVER_OUTPUTS.clear()
    RECEIVER_OUTPUTS.update(outputs)
    RECEIVER_CUSTODY.clear()
    RECEIVER_CUSTODY.update(custody)
    return pins, pins_data


def validate_report(data: bytes, classname: str, methods: list[str]) -> dict[str, int]:
    root = ET.fromstring(data)
    require(root.tag == "testsuite" and root.get("name") == classname, "one exact Surefire testsuite required")
    counts = {key: int(root.get(key, "-1")) for key in ("tests", "failures", "errors", "skipped")}
    require(counts == {"tests": len(methods), "failures": 0, "errors": 0, "skipped": 0},
            "SDK suite did not execute all required gates successfully")
    cases = root.findall("testcase")
    require(len(cases) == len(methods) and {case.get("name") for case in cases} == set(methods),
            "SDK test inventory drift")
    for case in cases:
        require(case.get("classname") == classname, "SDK test class drift")
        require(all(case.find(tag) is None for tag in ("failure", "error", "skipped")),
                "SDK failure, error or skip present")
    return counts



def validate_counters(counter: dict, ident: str, head: str) -> None:
    require(counter["schema"] == 1 and counter["qualificationId"] == ident
            and counter["checkoutCommit"] == head and counter["tests"] == 3
            and counter["successfulRuns"] == 16 and counter["refusedRuns"] == 30
            and counter["emittedOutputs"] == 2
            and counter["sourceOnly"] is True and counter["runtimeApplied"] is False
            and counter["phaseAdvanced"] is False, "fresh actual recipe counter receipt required")


def validate_marker(xml: bytes, ident: str) -> None:
    marker = ("M3_JOINED_RANGE_DIRECTORY_RECIPE_COUNTERS successful=16"
              + " refused=30 outputs=2 qualificationId=" + ident)
    lines = [line for node in ET.fromstring(xml).iter("system-out")
             for line in (node.text or "").splitlines()
             if line.startswith("M3_JOINED_RANGE_DIRECTORY_RECIPE_COUNTERS")]
    require(lines == [marker], "actual named JUnit stdout counter marker required")

def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest":["allTwoPartialStatesBothOrdersReachExactFixedPoint","allTwoInverseStatesBothOrdersRecoverExactBefore","eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]}
    if SUITES != expected_suites:
        raise AssertionError("sealed SDK report inventory drift")
    refused = 0
    for classname, methods in expected_suites.items():
        def suite() -> ET.Element:
            root = ET.Element("testsuite", {
                "name": classname, "tests": str(len(methods)), "failures": "0", "errors": "0", "skipped": "0",
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
    print("M3_JOINED_RANGE_DIRECTORY_SUREFIRE_SELFTEST_PASS refused=" + str(refused))



def self_test_evidence() -> None:
    """Exercise real counter/marker validators; synthetic controls are not JUnit execution."""
    ident, head = "joined-range-selftest-qualification", "1" * 40
    baseline = {"schema": 1, "qualificationId": ident, "checkoutCommit": head, "tests": 3,
                "successfulRuns": 16, "refusedRuns": 30, "emittedOutputs": 2,
                "sourceOnly": True, "runtimeApplied": False, "phaseAdvanced": False}
    validate_counters(baseline, ident, head)
    refused = 0
    for key, wrong in (("schema", 2), ("qualificationId", ident + "-stale"),
                       ("checkoutCommit", "2" * 40), ("tests", 0), ("successfulRuns", 15),
                       ("refusedRuns", 29), ("emittedOutputs", 1),
                       ("sourceOnly", False), ("runtimeApplied", True), ("phaseAdvanced", True)):
        changed = dict(baseline)
        changed[key] = wrong
        try:
            validate_counters(changed, ident, head)
        except ValueError:
            refused += 1
        else:
            raise AssertionError("altered recipe counter admitted: " + key)
    line = ("M3_JOINED_RANGE_DIRECTORY_RECIPE_COUNTERS successful=16"
            + " refused=30 outputs=2 qualificationId=" + ident)
    root = ET.Element("testsuite")
    marker_node = ET.SubElement(root, "system-out")
    marker_node.text = line + "\n"
    validate_marker(ET.tostring(root), ident)
    for text in ("", line + "-stale\n", line + "\n" + line + "\n",
                 line.replace("refused=30", "refused=29")):
        changed = ET.Element("testsuite")
        changed_node = ET.SubElement(changed, "system-out")
        changed_node.text = text
        try:
            validate_marker(ET.tostring(changed), ident)
        except ValueError:
            refused += 1
        else:
            raise AssertionError("missing, stale, duplicate or altered counter marker admitted")
    require(refused == 14, "recipe evidence discriminator coverage drift")
    print("M3_JOINED_RANGE_DIRECTORY_EVIDENCE_SELFTEST_PASS refused=" + str(refused))


def self_test(pins: dict, pins_data: bytes) -> None:
    self_test_reports()
    self_test_evidence()
    global ROOT
    original = ROOT
    target = original / "target"
    target.mkdir(exist_ok=True)
    require(target.is_dir() and all(not path.is_symlink() for path in (target, *target.parents)),
            "owned SDK temporary root required")
    refused = 0
    with tempfile.TemporaryDirectory(dir=target) as directory:
        fixture = Path(directory)
        for row in pins["files"]:
            destination = fixture / row["path"]
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(regular(original / row["path"]).read_bytes())
        (fixture / "pins.json").write_bytes(pins_data)
        try:
            ROOT = fixture
            inputs()
            def rejects() -> None:
                nonlocal refused
                try:
                    inputs()
                except (ValueError, OSError):
                    refused += 1
                else:
                    raise AssertionError("unsealed SDK input admitted")
            for name in ("pinned-engine/nested/Extra.java", "src/test/java/ExtraTest.java",
                         "src/main/resources/extra.tsv", "src/test/resources/shadow.txt",
                         "Extra-root.txt"):
                destination = fixture / name
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_text("unsealed\n", encoding="utf-8")
                rejects()
                destination.unlink()
            for row in pins["files"]:
                changed = json.loads(pins_data)
                changed["files"] = [item for item in changed["files"] if item["path"] != row["path"]]
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            changed = json.loads(pins_data)
            changed["files"].append(dict(changed["files"][0]))
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            (fixture / "pins.json").write_bytes(pins_data)
            for row in pins["files"]:
                path = fixture / row["path"]
                before = path.read_bytes()
                path.write_bytes(before + b"\nunknown SDK drift\n")
                rejects()
                path.write_bytes(before)
            for key, bad in (("sha256", "0" * 64), ("gitBlob", "0" * 40), ("bytes", -1)):
                changed = json.loads(pins_data)
                changed["files"][0][key] = bad
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
            (fixture / "pins.json").write_bytes(pins_data)
            name = pins["files"][0]["path"]
            path = fixture / name
            before = path.read_bytes()
            path.unlink()
            path.symlink_to(original / name)
            rejects()
            path.unlink()
            path.write_bytes(before)
            inputs()
            require(refused == 5 + len(REQUIRED_PATHS) * 2 + 1 + 3 + 1,
                    "SDK closure refusal coverage drift")
        finally:
            ROOT = original
            inputs()
    print("M3_JOINED_RANGE_DIRECTORY_CLOSURE_SELFTEST_PASS refused=" + str(refused))



def reports(pins: dict, pins_data: bytes) -> None:
    ident = os.environ.get("M3_QUALIFICATION_ID", "")
    head = os.environ.get("EXPECTED_SOURCE_SHA", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", ident) is not None
            and re.fullmatch(r"[0-9a-f]{40}", head) is not None, "actual qualification context required")
    directory = ROOT / "target/surefire-reports"
    expected = sorted(directory / ("TEST-" + name + ".xml") for name in SUITES)
    require(sorted(directory.glob("TEST-*.xml")) == expected, "missing or unexpected actual SDK XML")
    actual_reports = {}
    for name, methods in SUITES.items():
        raw = regular(directory / ("TEST-" + name + ".xml")).read_bytes()
        actual_reports[name] = {"counts": validate_report(raw, name, methods), "sha256": digest(raw)}
        validate_marker(raw, ident)
    counter_path = regular(ROOT / "target/authoring-recipe-test-counters.json")
    counter_raw = counter_path.read_bytes()
    validate_counters(json.loads(counter_raw), ident, head)
    emitted = ROOT / "target/emitted"
    require(emitted.is_dir() and not emitted.is_symlink(), "actual recipe output directory required")
    paths = list(emitted.rglob("*"))
    require(all(not path.is_symlink() for path in paths)
            and {path.relative_to(emitted).as_posix() for path in paths if path.is_file()}
                == set(RECEIVER_OUTPUTS), "exactly two actual forward/inverse output files required")
    for key, expected_sha in RECEIVER_OUTPUTS.items():
        direction, name = key.split("/", 1)
        raw = regular(emitted / safe(key)).read_bytes()
        counterpart = regular(ROOT / RESOURCE / direction / "out" / safe(name)).read_bytes()
        require(digest(raw) == expected_sha and raw == counterpart, "actual recipe output drift: " + key)
    java_raw = regular(ROOT / "target/fresh-owned-java-execution.json").read_bytes()
    java = json.loads(java_raw)
    require(java == {"schema":"M3_JOINED_FRESH_OWNERSHIP_FIXTURE_V1","javaRelease":21,"compiledVariants":4,"semanticCohorts":2,"rejectedMutants":2,"qualifiedBeforeSha256":"10ddd082c142420c29985a85d8b2f77507fdbcde2552b8bfc154e658f3b2a9e3","afterSha256":"d69ef5cc0e4bd5d95808978b4a0d73982322230a623d897074840b58fab1993e","proofSummary":"JOINED_FRESH_RANGE_PASS fixtures=5 ranges=466 nested=466 freshResults=1864 invalidRanges=25 indexRefusals=3738 aliasMutations=3"}, "actual emitted ownership/aliasing/normalization and compiled-mutant proof required")
    compiled = {}
    for name in COMPILED_PROVIDERS:
        relative = "target/classes/com/synexia/rewrite/" + name + ".class"
        raw = regular(ROOT / relative).read_bytes()
        require(len(raw) > 8 and raw[:4] == bytes.fromhex("cafebabe")
                and int.from_bytes(raw[6:8], "big") == 65, "actual Java21 SDK provider class required")
        compiled[name] = {"path": relative, "bytes": len(raw), "sha256": digest(raw), "majorVersion": 65}
    receipt = {
        "schema": "M3_JOINED_FRESH_OWNED_RANGE_SDK_RECEIPT_V1",
        "scope": "SOURCE_ONLY_JOINED_FRESH_OWNED_RANGE_REPLAY",
        "qualificationId": ident, "checkoutCommit": head,
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": PROVIDERS, "compiledProviders": compiled,
        "inputPinsSha256": digest(pins_data), "reports": actual_reports,
        "emittedProductSourceOutputs": dict(RECEIVER_OUTPUTS),
        "unchangedReceiverCustody": dict(RECEIVER_CUSTODY),
        "counterReceipt": {"path": "target/authoring-recipe-test-counters.json",
                           "bytes": len(counter_raw), "sha256": digest(counter_raw)},
        "tests": 3, "successfulRuns": 16, "refusedRuns": 30, "emittedOutputs": 2,
        "sourceOnly": True, "recipeJUnitQualified": True, "recipeReplayQualified": True,
        "actualEmittedFreshOwnershipFixtureQualified": True,
        "freshOwnershipFixture": {"path": "target/fresh-owned-java-execution.json", "sha256": digest(java_raw), **java},
        **{key: False for key in FALSE_GATES},
    }
    output = ROOT / "target/authoring-sdk-receipt.json"
    require(not output.exists() and not output.is_symlink(), "fresh SDK receipt required")
    with output.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, sort_keys=True))
    print("M3_JOINED_RANGE_DIRECTORY_SDK_REPORTS_PASS")

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inputs", action="store_true")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--reports", action="store_true")
    args = parser.parse_args()
    pins, pins_data = inputs()
    if args.self_test:
        self_test(pins, pins_data)
    if args.reports:
        reports(pins, pins_data)
    else:
        print("M3_JOINED_RANGE_DIRECTORY_INPUTS_EXACT")


if __name__ == "__main__":
    main()
