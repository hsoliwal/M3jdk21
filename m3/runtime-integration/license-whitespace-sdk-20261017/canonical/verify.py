#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Seal exact policy inputs, opaque license bytes, and four real SDK reports."""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
import os
import re
import tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SUITES = {"com.synexia.rewrite.M3QualificationFixtureWhitespacePolicyRecipeTest":["allPartialStatesConvergeInBothOrdersToExactFixedPoint","driftMissingAndDuplicateTargetsRefuseIncludingKeptLicenses","realGitAdmitsOnlyExactCategoryAtFourLicensePaths(Path)","rawByteGuardsRejectTrimExtraMissingAndChangedLicense(Path)"]}
REQUIRED_PATHS = ["FOSS_REUSE_DECISION.tsv","THIRD_PARTY_NOTICES.md","licenses/ADDITIONAL_LICENSE_INFO","licenses/ASSEMBLY_EXCEPTION","licenses/GPL-2.0.txt","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-qualification-fixture-whitespace-policy-20261017.yml","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/ADDITIONAL_LICENSE_INFO","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/gitattributes.txt","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-full-borrow-string-phase.yml","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-synexia-full-borrow-receiver.yml","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/gitattributes.txt","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-full-borrow-string-phase.yml","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-synexia-full-borrow-receiver.yml","src/test/java/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"licenses/ADDITIONAL_LICENSE_INFO":"ff700cd09f6ea0cc497f68169fd5c210bdaa7c2b","licenses/ASSEMBLY_EXCEPTION":"429666664e9ff5029d51dc7f807e1ec9eff2dd9f","licenses/GPL-2.0.txt":"8b400c7ab81b7b18baff3f81d597f5e511883134","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipe.java":"4d97622792b31abf13d067234bedf58388610708","pom.xml":"0812a2209ca502bc6577fa8caeb567db1c70e782","src/main/resources/META-INF/rewrite/m3-qualification-fixture-whitespace-policy-20261017.yml":"e30544b25ab68895bcd85770dbaf3b58949b311a","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/manifest.tsv":"c742f5fd070b401cc3fe97f48f9b536e2ba8a073","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/ADDITIONAL_LICENSE_INFO":"ff700cd09f6ea0cc497f68169fd5c210bdaa7c2b","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/gitattributes.txt":"b7d308816cb46863d711049f0b71971feb0db4df","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-full-borrow-string-phase.yml":"af28fca94255a8ce486cf651cd538b6c0fc09e8c","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-synexia-full-borrow-receiver.yml":"210cf4f9e58ef398def50372c456aa45cbb48476","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/gitattributes.txt":"74e5869819659da9effa800ce457baff8fd3630d","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-full-borrow-string-phase.yml":"04bd1693d3be3d18c4d60678a5b3932bdd685c9e","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-synexia-full-borrow-receiver.yml":"9108a21a41a0f721883d12adf4ed316ce480326b","FOSS_REUSE_DECISION.tsv":"c6fb41803bd6fad0b00d87b223cf46bdafea7f76","THIRD_PARTY_NOTICES.md":"0f986852e76b2930493682840b5b65c338a4b324","src/test/java/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipeTest.java":"295ba16618abfcb074bdfc8b9add10bd2f6ba1e9"}
REQUIRED_SHA256 = {"licenses/ADDITIONAL_LICENSE_INFO":"a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916","licenses/ASSEMBLY_EXCEPTION":"75292f03bf23d3db7c985aecc191029b93883200721ed23ed34a2e601463df33","licenses/GPL-2.0.txt":"4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726","pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipe.java":"725ea030c8a1de09604e1659fe765cd40baa4bb4e6e570f200c30255d1fb3665","pom.xml":"a00ccd905424b5ca57b1b8202af8c75d95eefc76ea02bacdf9e3cd976b0d95bd","src/main/resources/META-INF/rewrite/m3-qualification-fixture-whitespace-policy-20261017.yml":"e0947c4258a9c6c0a13c2a2e6c5c0a23a3b8c2dd00cb64c3f8213548ed4e7d26","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/manifest.tsv":"4483702dea2abcc88c1c7ea85d00611944edf57512242c6bab4016171af36ec8","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/ADDITIONAL_LICENSE_INFO":"a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/gitattributes.txt":"7eabae637a18e909a606c108597779a30e82d9d2aba78948b228a0ac2a403d50","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-full-borrow-string-phase.yml":"0d226ef8a4373aeaee25553ebaeb66de02f869941ff063ba20a46d9f2b703ad5","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/out/m3-synexia-full-borrow-receiver.yml":"b8335a1f10795c1457dd7b9721da8f971a1207314c0341226b83b108ca663450","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/gitattributes.txt":"867a654d9bdbd56a2e8e3743d13dacdcb34a0734f568754758eead3f681e6ae2","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-full-borrow-string-phase.yml":"d1ed6b9aa47cc6de22637e405de7502f26a9fcd8b8c75b7b7f0fcbf18ff454f6","src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/m3-synexia-full-borrow-receiver.yml":"528333f9460a62476b239c72d50f189c438b4f11b9ca2d0d736af8be0f02485a","FOSS_REUSE_DECISION.tsv":"99ae7e7a8c1520cbb60c1adf173479066f6f9ca41cd6c14e98a6de08f749c0e1","THIRD_PARTY_NOTICES.md":"01cde61b20abe44c79b0a309c94e3317989eb545e038c699baa31ba269f55b52","src/test/java/com/synexia/rewrite/M3QualificationFixtureWhitespacePolicyRecipeTest.java":"c8bcdff1095667b410c23de1f1e3bfd4e84e07ea23967be424e8201bc30685aa"}
RECEIVER_OUTPUTS = {".gitattributes":"7eabae637a18e909a606c108597779a30e82d9d2aba78948b228a0ac2a403d50",".github/workflows/m3-full-borrow-string-phase.yml":"0d226ef8a4373aeaee25553ebaeb66de02f869941ff063ba20a46d9f2b703ad5",".github/workflows/m3-synexia-full-borrow-receiver.yml":"b8335a1f10795c1457dd7b9721da8f971a1207314c0341226b83b108ca663450"}
RECEIVER_LICENSE_SOURCES = {"m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/ADDITIONAL_LICENSE_INFO":"licenses/ADDITIONAL_LICENSE_INFO","m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/ASSEMBLY_EXCEPTION":"licenses/ASSEMBLY_EXCEPTION","m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/GPL-2.0.txt":"licenses/GPL-2.0.txt","m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses/OpenJDK-ADDITIONAL_LICENSE_INFO":"licenses/ADDITIONAL_LICENSE_INFO","m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses/OpenJDK-LICENSE":"licenses/GPL-2.0.txt","m3/runtime-integration/license-whitespace-sdk-20261017/canonical/licenses/ADDITIONAL_LICENSE_INFO":"licenses/ADDITIONAL_LICENSE_INFO","m3/runtime-integration/license-whitespace-sdk-20261017/canonical/src/main/resources/com/synexia/rewrite/m3-qualification-fixture-whitespace-policy-20261017/before/ADDITIONAL_LICENSE_INFO":"licenses/ADDITIONAL_LICENSE_INFO"}
RECEIVER_LICENSE_INVENTORIES = {"m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses":["m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/ADDITIONAL_LICENSE_INFO","m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/ASSEMBLY_EXCEPTION","m3/runtime-integration/string-successor-sdk-20261017/canonical/licenses/GPL-2.0.txt"],"m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses":["m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses/OpenJDK-ADDITIONAL_LICENSE_INFO","m3/runtime-integration/utf8-cursor-20261014/recipe/canonical/licenses/OpenJDK-LICENSE"]}

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



def exact_file(base: Path, name: str) -> bytes:
    path = base / name
    current = path
    while current != base:
        if current.is_symlink():
            raise ValueError("receiving symlink forbidden: " + name)
        current = current.parent
    if not path.is_file():
        raise ValueError("receiving source missing: " + name)
    if path.stat().st_size > 1024 * 1024:
        raise ValueError("receiving source byte budget: " + name)
    return path.read_bytes()


def inventory(base: Path, root: Path) -> set[str]:
    found = set()
    if base.is_symlink():
        raise ValueError("receiving inventory symlink")
    for path in base.rglob("*"):
        if path.is_symlink():
            raise ValueError("receiving inventory symlink")
        if path.is_file():
            found.add(path.relative_to(root).as_posix())
            if len(found) > 64:
                raise ValueError("receiving source inventory budget")
    return found


def inputs() -> tuple[dict, bytes]:
    pins_data = exact_file(ROOT, "pins.json")
    pins = json.loads(pins_data)
    if pins["schema"] != "M3_LICENSE_WHITESPACE_POLICY_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    rows = pins["files"]
    if len(rows) != len(REQUIRED_PATHS) or not 1 <= len(rows) <= 64:
        raise ValueError("receiving file budget or duplicate rows")
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
        if (not name or path.is_absolute() or ".." in path.parts or "\\" in name
                or ":" in name or path.as_posix() != name or name in seen):
            raise ValueError("invalid receiving path")
        seen.add(name)
        data = exact_file(ROOT, name)
        if hashlib.sha256(data).hexdigest() != row["sha256"]:
            raise ValueError("receiving source drift: " + name)
        if row.get("unchangedGitBlob") and git_blob(data) != row["unchangedGitBlob"]:
            raise ValueError("receiving Git blob drift: " + name)
    for directory in ("licenses", "pinned-engine", "src"):
        actual = inventory(ROOT / directory, ROOT)
        expected = {name for name in REQUIRED_PATHS if name.startswith(directory + "/")}
        if actual != expected:
            raise ValueError("unsealed receiving source inventory: " + directory)
    top_files = {path.name for path in ROOT.iterdir() if path.is_file()}
    expected_top = {name for name in REQUIRED_PATHS if "/" not in name} | {"pins.json"}
    if top_files != expected_top:
        raise ValueError("unsealed receiving root file")
    for path in ROOT.iterdir():
        if path.is_symlink():
            raise ValueError("receiving root symlink")
        if path.is_dir() and path.name not in {"licenses", "pinned-engine", "src", "target"}:
            raise ValueError("unsealed receiving root directory")
    return pins, pins_data


def receiver(repository: Path, pins: dict) -> None:
    del pins
    for name, expected in RECEIVER_OUTPUTS.items():
        if hashlib.sha256(exact_file(repository, name)).hexdigest() != expected:
            raise ValueError("receiving whitespace policy output drift: " + name)
    for name, source in RECEIVER_LICENSE_SOURCES.items():
        path = repository / name
        if not path.exists():
            raise ValueError("receiving immutable license missing: " + name)
        data = exact_file(repository, name)
        if (hashlib.sha256(data).hexdigest() != REQUIRED_SHA256[source]
                or git_blob(data) != REQUIRED_GIT_BLOBS[source]):
            raise ValueError("receiving immutable license drift: " + name)
    for name, expected in RECEIVER_LICENSE_INVENTORIES.items():
        if inventory(repository / name, repository) != set(expected):
            raise ValueError("receiving immutable license inventory drift: " + name)


def self_test(pins: dict, pins_data: bytes) -> int:
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
                "pinned-engine/other/Extra.txt",
                "src/test/java/other/ExtraTest.java",
                "src/test/resources/shadow.txt",
                "src/main/resources/extra.txt",
                "licenses/UNSEALED_LICENSE.txt",
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
            for name in REQUIRED_SHA256:
                data = (fixture / name).read_bytes()
                altered = data + b"\n"
                (fixture / name).write_bytes(altered)
                changed = json.loads(pins_data)
                row = next(row for row in changed["files"] if row["path"] == name)
                row["sha256"] = hashlib.sha256(altered).hexdigest()
                row["unchangedGitBlob"] = git_blob(altered)
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
                (fixture / name).write_bytes(data)
            (fixture / "pins.json").write_bytes(pins_data)
            for name in REQUIRED_PATHS:
                data = (fixture / name).read_bytes()
                (fixture / name).unlink()
                rejects()
                (fixture / name).write_bytes(data)
            changed = json.loads(pins_data)
            changed["files"].append(changed["files"][0].copy())
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            (fixture / "pins.json").write_bytes(pins_data)
            inputs()
            expected = (7 + 2 * len(REQUIRED_PATHS) + len(REQUIRED_GIT_BLOBS)
                        + 2 * len(REQUIRED_SHA256))
            if refused != expected:
                raise AssertionError("receiving closure refusal coverage drift")
        finally:
            ROOT = original
    print("M3_LICENSE_WHITESPACE_POLICY_CLOSURE_SELFTEST_PASS refused=" + str(refused))
    return refused


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3QualificationFixtureWhitespacePolicyRecipeTest":["allPartialStatesConvergeInBothOrdersToExactFixedPoint","driftMissingAndDuplicateTargetsRefuseIncludingKeptLicenses","realGitAdmitsOnlyExactCategoryAtFourLicensePaths(Path)","rawByteGuardsRejectTrimExtraMissingAndChangedLicense(Path)"]}
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
    print("M3_LICENSE_WHITESPACE_POLICY_SUREFIRE_SELFTEST_PASS refused=" + str(refused))



GIT_CASES = [
    [
        "baseline",
        2,
        112,
        0,
        0
    ],
    [
        "candidate",
        0,
        0,
        0,
        0
    ],
    [
        "unrelated-trailing",
        2,
        1,
        0,
        0
    ],
    [
        "nested-root-path-0",
        2,
        28,
        0,
        0
    ],
    [
        "suffixed-license-path-0",
        2,
        28,
        0,
        0
    ],
    [
        "nested-root-path-1",
        2,
        28,
        0,
        0
    ],
    [
        "suffixed-license-path-1",
        2,
        28,
        0,
        0
    ],
    [
        "nested-root-path-2",
        2,
        28,
        0,
        0
    ],
    [
        "suffixed-license-path-2",
        2,
        28,
        0,
        0
    ],
    [
        "nested-root-path-3",
        2,
        28,
        0,
        0
    ],
    [
        "suffixed-license-path-3",
        2,
        28,
        0,
        0
    ],
    [
        "space-before-tab-0",
        2,
        0,
        1,
        0
    ],
    [
        "blank-at-eof-0",
        2,
        0,
        0,
        1
    ],
    [
        "space-before-tab-1",
        2,
        0,
        1,
        0
    ],
    [
        "blank-at-eof-1",
        2,
        0,
        0,
        1
    ],
    [
        "space-before-tab-2",
        2,
        0,
        1,
        0
    ],
    [
        "blank-at-eof-2",
        2,
        0,
        0,
        1
    ],
    [
        "space-before-tab-3",
        2,
        0,
        1,
        0
    ],
    [
        "blank-at-eof-3",
        2,
        0,
        0,
        1
    ]
]
RAW_CASES = ["candidate","trim-0","extra-bytes-0","missing-0","changed-0","extra-file-0","trim-1","extra-bytes-1","missing-1","changed-1","extra-file-1","trim-2","extra-bytes-2","missing-2","changed-2","trim-3","extra-bytes-3","missing-3","changed-3"]


def validate_emitted() -> dict:
    emitted = ROOT / "target/emitted/license-whitespace"
    expected = {"policy/" + name for name in RECEIVER_OUTPUTS}
    expected |= {"logs/git-qualification.tsv", "logs/raw-byte-guards.tsv"}
    expected |= {"logs/git/" + name + ".log" for name, *_ in GIT_CASES}
    if inventory(emitted, emitted) != expected:
        raise ValueError("whitespace policy emitted inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        if hashlib.sha256(exact_file(emitted, "policy/" + name)).hexdigest() != digest:
            raise ValueError("whitespace policy emitted output drift: " + name)
    table = "case\texit\ttrailing\tspaceBeforeTab\tblankAtEof\n"
    table += "".join("\t".join(map(str, row)) + "\n" for row in GIT_CASES)
    if exact_file(emitted, "logs/git-qualification.tsv") != table.encode("ascii"):
        raise ValueError("real Git qualification table drift")
    licenses = [name for name in RECEIVER_LICENSE_SOURCES
                if name.endswith("ADDITIONAL_LICENSE_INFO")]
    paths = {
        "baseline": set(licenses), "candidate": set(),
        "unrelated-trailing": {"other/unrelated-trailing.txt"},
    }
    for index, path in enumerate(licenses):
        paths["nested-root-path-" + str(index)] = {"nested/" + path}
        paths["suffixed-license-path-" + str(index)] = {path + ".copy"}
        paths["space-before-tab-" + str(index)] = {path}
        paths["blank-at-eof-" + str(index)] = {path}
    phrases = (b"trailing whitespace.", b"space before tab in indent.", b"new blank line at EOF.")
    logs = {}
    for name, exit_code, trailing, tabs, eof in GIT_CASES:
        data = exact_file(emitted, "logs/git/" + name + ".log")
        if len(data) > 65536:
            raise ValueError("real Git diagnostic byte budget")
        diagnostic = re.findall(rb"^(.+):([0-9]+): (.+)$", data, re.MULTILINE)
        counts = Counter(message for _, _, message in diagnostic)
        if counts != Counter({message: count for message, count
                              in zip(phrases, (trailing, tabs, eof)) if count}):
            raise ValueError("real Git whitespace category drift: " + name)
        observed_paths = {path.decode("utf-8", errors="strict") for path, _, _ in diagnostic}
        if observed_paths != paths[name]:
            raise ValueError("real Git exact path discrimination drift: " + name)
        if name == "baseline" and Counter(path for path, _, _ in diagnostic) != {
                path.encode("utf-8"): 28 for path in licenses}:
            raise ValueError("real Git baseline must expose twenty-eight diagnostics per donor copy")
        if name == "candidate" and data != b"":
            raise ValueError("real Git candidate must have no output")
        if any(line and not line.startswith(b"+") and not re.fullmatch(
                rb".+:[0-9]+: .+", line) for line in data.splitlines()):
            raise ValueError("unexpected real Git diagnostic output: " + name)
        logs[name] = {"exitCode": exit_code, "diagnostics": len(diagnostic),
                      "sha256": hashlib.sha256(data).hexdigest()}
    raw_table = "case\texit\n"
    raw_table += "".join(name + ("\t0\n" if name == "candidate" else "\t1\n")
                         for name in RAW_CASES)
    if exact_file(emitted, "logs/raw-byte-guards.tsv") != raw_table.encode("ascii"):
        raise ValueError("independent raw-byte guard table drift")
    return {"gitCases": logs, "rawByteRefusals": 18,
            "policyOutputs": RECEIVER_OUTPUTS}


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
        self_test_reports()
        self_test(pins, pins_data)
    if not args.reports:
        print("M3_LICENSE_WHITESPACE_POLICY_SDK_INPUTS_EXACT")
        return
    directory = ROOT / "target/surefire-reports"
    expected = sorted(directory / ("TEST-" + name + ".xml") for name in SUITES)
    if sorted(directory.glob("TEST-*.xml")) != expected:
        raise ValueError("missing or unexpected SDK test reports")
    reports = {}
    for name, methods in SUITES.items():
        data = exact_file(directory, "TEST-" + name + ".xml")
        reports[name] = {"counts": validate_report(data, name, methods),
                         "sha256": hashlib.sha256(data).hexdigest()}
    emitted = validate_emitted()
    receipt = {
        "schema": "M3_LICENSE_WHITESPACE_POLICY_SDK_RECEIPT_V1",
        "scope": "ORIGINAL_PROVIDER_THREE_POLICY_OUTPUTS_FOUR_IMMUTABLE_LICENSE_OWNERS",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emitted": emitted,
        "namedDescriptorPartialStates": 8,
        "sourceOrders": 2,
        "recipeRefusals": 21,
        "expectedClosureSelfTestRefusals": 104,
        "expectedReportSelfTestRefusals": 32,
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
