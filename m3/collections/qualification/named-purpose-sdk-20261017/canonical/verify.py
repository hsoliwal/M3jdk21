#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify exact receiving SDK inputs and actual eighteen-test Surefire evidence."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import tempfile
from pathlib import Path, PurePosixPath
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
SUITES = {"com.synexia.rewrite.M3Jdk21GenericPurposeCompositionRecipeTest":["sourceSealedBeforeAfterAndNoOpSecondPass","originalJavaOwnerAndAllOutputsHaveExactDigests","missingDriftedAndOccupiedSourcesRefuseWithoutGenerating","duplicateOrNonJavaSourceRefuses","unrelatedCompilationUnitIsNotRewritten","namedMavenRecipeResolvesIncumbentClassBackedEngine"],"com.synexia.rewrite.M3Jdk21GenericPurposeSealSuccessorRecipeTest":["historicalInputReplaysToApprovedSuccessorAndSecondPassIsNoop","oldInputAndAllThreePostimagesHavePinnedHashes","refusedMissingDriftedAndOccupiedTextSources","duplicateTextSourceIsNotSilentlyDeduplicated","registeredRecipeUsesTheExistingTextSourceEngine"],"com.synexia.rewrite.M3GenericPurposeRecipeOrderRepairTest":["nestedRecipeGeneratesExactCanonicalRepairsAndReachesFixedPoint","allEightPartialStatesConverge","driftMissingAndDuplicateTargetsRefuse","namedPurposeRecipeExecutesAllPartialStatesAndExactFixedPoint","namedSealRecipeExecutesAllPartialStatesAndExactFixedPoint"],"com.synexia.rewrite.M3NamedPurposeSdkBootstrapRecipeTest":["namedWorkflowReachesExactFixedPointAndPreservesExistingProof","workflowRejectsDriftMissingAndDuplicateTargets"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3EditScope.java","pinned-engine/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java","pinned-engine/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java","pinned-engine/com/synexia/rewrite/M3ScopeInference.java","pom.xml","src/main/resources/META-INF/rewrite/m3-generic-purpose-recipe-order-repair-20261017.yml","src/main/resources/META-INF/rewrite/m3jdk21-generic-filtered-purpose-20261017.yml","src/main/resources/META-INF/rewrite/m3jdk21-generic-purpose-seal-successor-20261017.yml","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/M3Jdk21GenericPurposeCompositionRecipeTest.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before-text-sdk.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredPurposeCompositionTest.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredWrapperSelfTest.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/pre/M3FilteredMaps.java.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/before.tsv.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/manifest.tsv.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/generic-filtered-purpose-postimages-20261017.tsv.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/pre/verify_generic_filtered_wrappers.py.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/test_verify_generic_filtered_successor.py.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/verify_generic_filtered_wrappers.py.txt","src/test/java/com/synexia/rewrite/M3GenericPurposeRecipeOrderRepairTest.java","src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeCompositionRecipeTest.java","src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java","verify.py","src/main/resources/META-INF/rewrite/m3-named-purpose-sdk-bootstrap-20261017.yml","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/before.yml.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/m3-generic-purpose-map-receiver-20261017.yml.txt","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/manifest.tsv","src/test/java/com/synexia/rewrite/M3NamedPurposeSdkBootstrapRecipeTest.java"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java":"2530c533003a8a17d0e782728d98a4dfebe13651","pinned-engine/com/synexia/rewrite/M3EditScope.java":"274360816db1e44a2f1796a3bc5cdeff6b0ce6db","pinned-engine/com/synexia/rewrite/M3ScopeInference.java":"f6fd8d4a558a6b9b0c78cf4d010e63e3b3e1a9da","pinned-engine/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java":"2db1257a40d21be06ba2a67da87aff0311183809","src/main/resources/META-INF/rewrite/m3jdk21-generic-filtered-purpose-20261017.yml":"18766663b3fbc2d33e44c4494a41c35ce0ee97cb","src/main/resources/META-INF/rewrite/m3jdk21-generic-purpose-seal-successor-20261017.yml":"9bbfd0b6f55d248fe3ff10b021a9cc0b09b5148f","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt":"42acdecc54133b545fe174230766e86092e37041","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredPurposeCompositionTest.java.txt":"c648a8f9cd7262c23fc044345619721dcb5479fa","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredWrapperSelfTest.java.txt":"85af59e2c8bc516de21b57f13e8104b107422bae","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/pre/M3FilteredMaps.java.txt":"8fec2f88d641d15e90ca2ad522749383c1926e59","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/generic-filtered-purpose-postimages-20261017.tsv.txt":"2282d1b4cd54261177de8ce1ae0616d7ecd2f84f","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/manifest.tsv":"5400c4ad65c86a1d4ad0c2184add5591753d53f4","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/pre/verify_generic_filtered_wrappers.py.txt":"07a2c7257afe8de57bea5feeab03fbe21fc6d106","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/test_verify_generic_filtered_successor.py.txt":"f10d9d6a7424fff7452ac4c204aa5fc3255a91ff","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/verify_generic_filtered_wrappers.py.txt":"be6bd8c01f56fcc5e6c0033bff77410573405522","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before.java.txt":"3472a1fe47c10089dc2a51ec45f7574aa60fedf4","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/before.tsv.txt":"1c5f84f650ef4522186ffafac6ba62d79294f7fe","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before-text-sdk.java.txt":"31f22bbe62f377a4a07aa09a1fa30d29e1307ef7","src/test/java/com/synexia/rewrite/M3GenericPurposeRecipeOrderRepairTest.java":"c8431ac70aac710caff1f850d9c5c59e13ba3f0e","src/main/resources/META-INF/rewrite/m3-named-purpose-sdk-bootstrap-20261017.yml":"0563713c120d48e7b20df9610ec8a540b9f31be7","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/before.yml.txt":"1a3ac1cbccf374af0b7079022dbb5c2368c3a189","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/m3-generic-purpose-map-receiver-20261017.yml.txt":"be38d7e13ef100d7a1a0e61ea4facdc9d138cc95","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/manifest.tsv":"7c3aa320a01ea952d342c798421fc293b43901dc","src/test/java/com/synexia/rewrite/M3NamedPurposeSdkBootstrapRecipeTest.java":"84ce218768c75cb986b7537a1a6b4eb62fe4b100"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3HashPinnedJavaSnapshotRecipe.java":"c4bbf3136d36608c80452226f0a59a365a065023b13c34b7ef6ca0dc2ed54e5c","pinned-engine/com/synexia/rewrite/M3EditScope.java":"962d37d62d740c3e8055c365a048ae7ee0479c26a9f6bbfe741e540d6c1134ab","pinned-engine/com/synexia/rewrite/M3ScopeInference.java":"17228a698f917bfabb46f8c4a20b50822820516fbc6e9ad71d52587a1918e730","pinned-engine/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java":"53abb7f4fcd9f2f3b4b6cdfce76d40ed313d0446c322cc088310e84ff23311bc","src/main/resources/META-INF/rewrite/m3jdk21-generic-filtered-purpose-20261017.yml":"613984f83cfd0ccd12f2b11bc8bb001f61523ceafb03a2624e697c0f787d1d64","src/main/resources/META-INF/rewrite/m3jdk21-generic-purpose-seal-successor-20261017.yml":"53130adaecdb65892402257072e3a88aedea3999a142ef68aee3af2b581a50ef","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredMaps.java.txt":"8e9ec760ea52dcd69b7d137a9b0efc88172dfd76df178219c1f0bd887200799e","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredPurposeCompositionTest.java.txt":"baf08471c33e0e75792c3beea04c4c0bab861b04cd3e5763072c8505e6640ac9","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/M3FilteredWrapperSelfTest.java.txt":"b9fabf7e916744765a8b67e629d19e9e6d1e85ba2ae7e2d67a7b3ecbdb878c9d","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/manifest.tsv":"33d730135f1dff30d158918499eb2fa16304ffe36509e481764731d1fdc7a66a","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/pre/M3FilteredMaps.java.txt":"1dff497079e768fda574a349cc521732d69cbacbecc19deba6888d909844f1cb","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/generic-filtered-purpose-postimages-20261017.tsv.txt":"c2e1a013529b2f7b2914dbdd1d78dfdabb7c60f56a7dcbcae8aa1005731850c1","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/manifest.tsv":"3df2107762a5f1c59648813bde23ed0652fa7602a60d0b3e2c8fa0d8466ac936","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/pre/verify_generic_filtered_wrappers.py.txt":"1f4d407a72adc6846d58b66d11d5f811f92dc788038c4dc480efd25541b4d37f","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/test_verify_generic_filtered_successor.py.txt":"085c496a5bf78ae6905d3b7d956c5a073bcc3460c4adac15e788c46bed0030e5","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3jdk21-generic-purpose-seal-successor-20261017/verify_generic_filtered_wrappers.py.txt":"291554b820f0efbec8f57809c32c5e1b5a408227c54231e463e8ba152cf0834c","src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeCompositionRecipeTest.java":"e27589ee4751518f79e8c2c492b8bd59e78fc486c78d1b98135e4276c0e34eb9","src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java":"da43cdb12c22ea089dd0e41ed9247d42ee3e99c80e500738b07ddbfe70fb1f8f","pom.xml":"e678cb266e41c771cb89ffc0fa08b59b1e4e52c7145ad06cd3838644b433cd19","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/M3Jdk21GenericPurposeCompositionRecipeTest.java.txt":"e27589ee4751518f79e8c2c492b8bd59e78fc486c78d1b98135e4276c0e34eb9","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before.java.txt":"91a269409d37e5e70276406880b451f05aa80beee0032a0e2f76c201426c60aa","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/manifest.tsv.txt":"33d730135f1dff30d158918499eb2fa16304ffe36509e481764731d1fdc7a66a","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/before.tsv.txt":"efe0b4c8d5386c748c6605401977122006537ef54f0eaafd4bffa710671067db","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/manifest.tsv":"4f0a8b85042ea0a7cc64379ef0a6ac51d5c280cb3dff525cabb978a9a63cbdb9","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-generic-purpose-manifest-order-repair-20261017/manifest.tsv":"3279736f109bce2608a2094febccd8d3ce60409460349d3ce3faed8f8e028cf1","src/main/resources/META-INF/rewrite/m3-generic-purpose-recipe-order-repair-20261017.yml":"e5dda896aa1691ad82ff160d9f3c2d72ab069f2b7317a042d2042b1ded4ddec7","src/test/java/com/synexia/rewrite/M3GenericPurposeRecipeOrderRepairTest.java":"6dba4567a3ac148684dd0282aa3a1597d1cc93338d59e0c2fae660823e682963","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java.txt":"da43cdb12c22ea089dd0e41ed9247d42ee3e99c80e500738b07ddbfe70fb1f8f","src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-generic-purpose-sdk-order-repair-20261017/before-text-sdk.java.txt":"46aeef720e427daa2dc969213489e7e0e8d37fd8dab16710599a356cb3a24e9c","src/main/resources/META-INF/rewrite/m3-named-purpose-sdk-bootstrap-20261017.yml":"b5dc7ceb3aece83ff5b90569af46985736e998a7ae2b3140bbd9b153e616f8c3","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/before.yml.txt":"8c0c5c9077cddbee219c9d3eb65ccac482a1f726adb5323f275d7fe38dd6ff49","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/m3-generic-purpose-map-receiver-20261017.yml.txt":"dd5d332d9f9c09a6500baa1b99ce7f76c5c7ca241103d82c42e13bec0bb4bf1c","src/main/resources/com/synexia/rewrite/hash-pinned-text/m3-named-purpose-sdk-bootstrap-20261017/manifest.tsv":"b32fa460d7f014465898b5ceaeef289ed747bd5890bcdbf9b7ef074360db81e0","src/test/java/com/synexia/rewrite/M3NamedPurposeSdkBootstrapRecipeTest.java":"cc3fc1c0ecb287d6be48e1df42ac0aca636e2fdcf728b70b4554aca278be26dd"}
RECEIVER_OUTPUTS = {"m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java":"8e9ec760ea52dcd69b7d137a9b0efc88172dfd76df178219c1f0bd887200799e","m3/collections/src/test/java/com/m3/collections/M3FilteredPurposeCompositionTest.java":"baf08471c33e0e75792c3beea04c4c0bab861b04cd3e5763072c8505e6640ac9","m3/collections/src/test/java/com/m3/collections/M3FilteredWrapperSelfTest.java":"b9fabf7e916744765a8b67e629d19e9e6d1e85ba2ae7e2d67a7b3ecbdb878c9d","m3/collections/qualification/generic-filtered-purpose-postimages-20261017.tsv":"c2e1a013529b2f7b2914dbdd1d78dfdabb7c60f56a7dcbcae8aa1005731850c1","m3/collections/qualification/test_verify_generic_filtered_successor.py":"085c496a5bf78ae6905d3b7d956c5a073bcc3460c4adac15e788c46bed0030e5","m3/collections/qualification/verify_generic_filtered_wrappers.py":"291554b820f0efbec8f57809c32c5e1b5a408227c54231e463e8ba152cf0834c"}


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
    if pins["schema"] != "M3_PURPOSE_RECEIVING_SDK_INPUTS_V1":
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
            raise ValueError("receiving wrapper output drift: " + name)


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
            name = "src/test/java/com/synexia/rewrite/M3GenericPurposeRecipeOrderRepairTest.java"
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
    print("M3_PURPOSE_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_PURPOSE_RECEIVING_SDK_INPUTS_EXACT")
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
    repairs = {"synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeCompositionRecipeTest.java":"e27589ee4751518f79e8c2c492b8bd59e78fc486c78d1b98135e4276c0e34eb9","synexia-openrewrite-recipes/src/test/java/com/synexia/rewrite/M3Jdk21GenericPurposeSealSuccessorRecipeTest.java":"da43cdb12c22ea089dd0e41ed9247d42ee3e99c80e500738b07ddbfe70fb1f8f","synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-generic-filtered-purpose-20261017/manifest.tsv":"33d730135f1dff30d158918499eb2fa16304ffe36509e481764731d1fdc7a66a"}
    emitted = ROOT / "target/repair-emitted"
    actual = sorted(p.relative_to(emitted).as_posix() for p in emitted.rglob("*") if p.is_file())
    if actual != sorted(repairs):
        raise ValueError("nested repair output inventory drift")
    for name, expected_digest in repairs.items():
        data = (emitted / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != expected_digest:
            raise ValueError("nested repair output drift: " + name)
    bootstrap_outputs = {
        ".github/workflows/m3-generic-purpose-map-receiver-20261017.yml":
            "dd5d332d9f9c09a6500baa1b99ce7f76c5c7ca241103d82c42e13bec0bb4bf1c",
    }
    bootstrap_emitted = ROOT / "target/bootstrap-emitted"
    actual_bootstrap = sorted(
        path.relative_to(bootstrap_emitted).as_posix()
        for path in bootstrap_emitted.rglob("*") if path.is_file())
    if actual_bootstrap != sorted(bootstrap_outputs):
        raise ValueError("named SDK bootstrap output inventory drift")
    for name, expected_digest in bootstrap_outputs.items():
        data = (bootstrap_emitted / name).read_bytes()
        if hashlib.sha256(data).hexdigest() != expected_digest:
            raise ValueError("named SDK bootstrap output drift: " + name)
    receipt = {
        "schema": "M3_PURPOSE_RECEIVING_SDK_RECEIPT_V1",
        "scope": "PUBLIC_JAVA_2530_TEXT_2DB_EXISTING_PURPOSE_NESTED_REPAIR_AND_NAMED_BOOTSTRAP_SDK",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"java": "2530c533003a8a17d0e782728d98a4dfebe13651",
                      "text": "2db1257a40d21be06ba2a67da87aff0311183809"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "emittedCanonicalRepairs": repairs,
        "emittedBootstrapWorkflow": bootstrap_outputs,
        "bootstrapNamedDescriptor": "EXECUTED",
        "originalNamedDescriptors": "EXECUTED",
        "nestedRepairNamedDescriptor": "EXECUTED",
        "canonicalProvider39c": "NOT_RUN",
        "matchedJdkJtreg": "NOT_RUN",
        "runtimeJavaAndJniByThisSdkModule": "NOT_RUN",
        "runtimeAppliedByThisTask": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
