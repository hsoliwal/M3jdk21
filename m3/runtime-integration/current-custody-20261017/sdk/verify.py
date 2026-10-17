#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify four generated custody owners, immutable historical inputs and exact named SDK proof."""
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
SUITES = {"com.synexia.rewrite.M3RuntimeCurrentCustodyRecipeTest":["runtimeCurrentCustodyEveryPartialStateAndOrder","runtimeCurrentCustodyRefusesDriftMissingAndDuplicateOwnership"]}
RUNTIME_TESTS = {"RecipeTest":["test_manifest_is_upstream_to_master_v2","test_current_tree_is_exact_superseded_fixed_point","test_every_current_target_matches_after_or_exact_superseded","test_patch_bytes_remain_content_addressed","test_each_unknown_drift_is_refused_without_partial_write","test_modified_utf8_owner_fanin_matches_canonical_donor","test_modified_utf8_partial_and_unreviewed_sources_are_refused","test_symlink_target_is_refused"],"CurrentCustodyTest":["test_exact_current_72_all_modes","test_historical_patch_66_round_trip","test_all_64_successor_masks","test_all_16_modified_utf8_masks","test_each_72_drift_missing_symlink_no_partial_writes","test_cohort_shape_path_and_hash_refusals","test_fixture_source_custody_guards","test_legacy_does_not_read_auxiliaries_or_fixtures","test_cli_does_not_read_fixture_data"]}
REQUIRED_PATHS = ["pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java","pinned-engine/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipe.java","pom.xml","src/main/resources/META-INF/rewrite/m3-runtime-current-custody-20261017.yml","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/apply.py","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/manifest.json","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/runtime.patch","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/test_recipe.py","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.cpp","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.hpp","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.inline.hpp","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3String.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringFacts.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringPool.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/String.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/M3PatternQuery.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/Pattern.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/legacy-admitted/src/java.base/share/classes/java/lang/M3String.java","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ADDITIONAL_LICENSE_INFO","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ASSEMBLY_EXCEPTION","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/LICENSE","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/manifest.tsv","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/apply.py","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/current-cohort-20261017.json","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/m3-runtime-current-custody.yml","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/test_recipe.py","src/test/java/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipeTest.java","verify.py"]
REQUIRED_GIT_BLOBS = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"38df5f97bbeec8a8a8cfc77091d56bda925c07f2","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"1c1b1b568d369f688f6953161636bf36b484bf5a","pinned-engine/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipe.java":"b243bf67f4318c481e63e472dd9445821605c91e","pom.xml":"d79867bcc09ad6329e12418e15907e54b8ab025f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/apply.py":"6f04034d19b92ed55deba47b870c1d850a734a52","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/manifest.json":"ebe4a76f7c11f86f1c1cef953940216f6793d321","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/runtime.patch":"a5abe528c303f689953731b1ba164e6e6bf53745","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/test_recipe.py":"e90bad7e46a39ec5c9b4c86f774a77116007ed0b","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.cpp":"a766d1f62716bf96f99bc275afd7e57e12ce9de7","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.hpp":"9052154399d82a077bbec99f92b9b4521e19ed2f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.inline.hpp":"0810b8c905e57745b9f84443b67c3b3fdd494bc4","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3String.java":"479d1f8145697d270af17f5e2d2f1c85f426fb33","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringFacts.java":"763a76fb55a52576ca1a3edc48fcc214663f799e","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringPool.java":"d97d0dd8f2c26599086ad817ed5d521b29593d2d","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java":"c45ae8696da18b15b7b9ee0267cc05aec1ae6c92","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/String.java":"dd4033194bc27c640948b59bcc9085d4f357c74c","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/M3PatternQuery.java":"7a13107628460134548a02095ca5985437a112bb","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/Pattern.java":"ee1c1951a7498c191810bc1901fff48972cddd53","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/legacy-admitted/src/java.base/share/classes/java/lang/M3String.java":"70d1d03c109126fdda36c54da21b484bbe368fd9","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ADDITIONAL_LICENSE_INFO":"ff700cd09f6ea0cc497f68169fd5c210bdaa7c2b","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ASSEMBLY_EXCEPTION":"429666664e9ff5029d51dc7f807e1ec9eff2dd9f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/LICENSE":"8b400c7ab81b7b18baff3f81d597f5e511883134","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/manifest.tsv":"795a72ce3a8060c0adb872030639802cdad11139","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/apply.py":"69006c0a5f66c9e4a057957300117b90643bb3f1","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/current-cohort-20261017.json":"629d1208f340ca87faab5c353eaebb8caeabe602","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/m3-runtime-current-custody.yml":"298fb1da85879368475f18fe128c46f477de1857","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/test_recipe.py":"e4a68d3dd6c1a4568ce341f4166f8e394d98c22e","src/main/resources/META-INF/rewrite/m3-runtime-current-custody-20261017.yml":"6ec42d564153a4d449a582572e35a3605e9ee23a","src/test/java/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipeTest.java":"e9b899ecc90278998248ee94934dbee3599f1835"}
REQUIRED_SHA256 = {"pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java":"56bd61be29eae76b3f468519677424450448751d94ca032956b330d7b3dd1999","pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java":"ee90a2b8689699745553c2f4d2d07121b91777f4e9a671fdb33ba8edea0c14ec","pinned-engine/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipe.java":"ba840deff9e82910992753e0e12400cb3c7354765e801d5fc63c61d19e2e88c4","pom.xml":"f0ab99c3dafb38e82f8e5fd4c6d22db07831808c251c4622f292114c83e6673e","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/apply.py":"d1e98695bbb175acd6661f1d6f2ad2eb9dc90c5a67cc18da280b4c20aee771a5","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/manifest.json":"12a749485fcd401d8fba311e5b8911f30e10988a63a14ebcbd11edac3fbee643","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/runtime.patch":"bfc58c6eb3c7540808839e937a0f64788d65eb64097d10407ecccf899560d0eb","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/before/test_recipe.py":"46d731debe97ab67d455078e493ce92f75c40481c2727868baa79ef6a7f8bad8","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.cpp":"10816a3adb62ae3dd5b7fdefb79a43947637fb90924298ace56c8320ea4d60cb","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.hpp":"36a6ae73f908a42bd565ea8caca830f7de63e423092c92141f0fd4110dc75df3","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/hotspot/share/classfile/javaClasses.inline.hpp":"18edfbc5b4ae95130b6fcfe19fd9927abd0e4cc5504129d9a20b025b10f0b7a1","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3String.java":"06e8c6ba9ebe01493022d20a442a5902209b553938068cd312140fddbb9d356b","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringFacts.java":"c834e63534db855f06e99bbfdf45caf0ecf36b88f5760bfe7fdaa8bcc355509f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringPool.java":"c0e3b63830c3540e39e51ecc7522e1294f92baa85ef7e3963f2c21d3762fa35b","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java":"ce6ae17859adb67fefe5fe127db102f0327a99ed89796a1194d4caaf075c46b4","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/lang/String.java":"c49e422dfe0e6b83a2d51155f0d36b5223719dfefd211a96313705bc3ee253ca","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/M3PatternQuery.java":"5712e094926b90f3be5356dea999e9d5dc2d13e0f1fb5082e943f8c0a2d5575f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/historical-after/src/java.base/share/classes/java/util/regex/Pattern.java":"d2828f7a76ae172204abe56cdf4e9df4b5fc0133bb65936b4e82aa9f7b9d105f","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/legacy-admitted/src/java.base/share/classes/java/lang/M3String.java":"24ac1f87c1b2022b08d695ae6b116e9376dad59ed0ffb86f52da0003dd3e036d","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ADDITIONAL_LICENSE_INFO":"a69bce275ba7a3570af6579cb0f55682cd75fedfcd49e0e8e9022270c447c916","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/ASSEMBLY_EXCEPTION":"75292f03bf23d3db7c985aecc191029b93883200721ed23ed34a2e601463df33","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/licenses/M3jdk21/LICENSE":"4b9abebc4338048a7c2dc184e9f800deb349366bdf28eb23c2677a77b4c87726","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/manifest.tsv":"26bd5136ac1e0216b891cd2537e0d809c60e7536169b564fd63d0e04cfe79f09","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/apply.py":"c25a6b4271d4c7e8c881adcd2a1500d06940ec58ab582d0ab4160a3436dfd2ad","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/current-cohort-20261017.json":"a2481edf200087aa9efb0d2825ef95a083124bf32a8182359b04c56b2d022bb3","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/m3-runtime-current-custody.yml":"fa76297daada18a5b638fb3dbb117cc5b1d36dcc3ac1f7611e39accfd85d845c","src/main/resources/com/synexia/rewrite/m3-runtime-current-custody-20261017/out/test_recipe.py":"a8f86801f9343b9b61ce6159f3f978065caebf25f5f8d588b1fda4c66025872d","src/main/resources/META-INF/rewrite/m3-runtime-current-custody-20261017.yml":"9a00058b1405152dde75c97836539b549b2b9772cfe8c42a4cc1d6440261c6fe","src/test/java/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipeTest.java":"af03be984576d471b72cc2cd8ef4c141a8eed394a87df4e28ac21ce35022065d"}
RECEIVER_OUTPUTS = {"m3/runtime-integration/recipe/apply.py":"c25a6b4271d4c7e8c881adcd2a1500d06940ec58ab582d0ab4160a3436dfd2ad","m3/runtime-integration/recipe/test_recipe.py":"a8f86801f9343b9b61ce6159f3f978065caebf25f5f8d588b1fda4c66025872d","m3/runtime-integration/recipe/current-cohort-20261017.json":"a2481edf200087aa9efb0d2825ef95a083124bf32a8182359b04c56b2d022bb3",".github/workflows/m3-runtime-current-custody.yml":"fa76297daada18a5b638fb3dbb117cc5b1d36dcc3ac1f7611e39accfd85d845c"}
KEEP_OUTPUTS = {"m3/runtime-integration/recipe/manifest.json":"12a749485fcd401d8fba311e5b8911f30e10988a63a14ebcbd11edac3fbee643","m3/runtime-integration/recipe/runtime.patch":"bfc58c6eb3c7540808839e937a0f64788d65eb64097d10407ecccf899560d0eb"}

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
    if pins["schema"] != "M3_RUNTIME_CURRENT_CUSTODY_RECEIVING_SDK_INPUTS_V1":
        raise ValueError("unknown receiving input schema")
    if pins["providers"] != {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                             "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"}:
        raise ValueError("receiving provider metadata drift")
    if pins.get("runtimeTests") != RUNTIME_TESTS or pins.get("expectedRuntimeTestMethods") != 17:
        raise ValueError("named runtime test inventory drift")
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
        data = safe_file(ROOT, name)
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


def safe_file(base: Path, name: str) -> bytes:
    """Read a canonical regular owner without following a link at any path component."""
    if not isinstance(name, str) or not name:
        raise ValueError("unsafe source-custody path: " + str(name))
    path = PurePosixPath(name)
    if (path.is_absolute() or path.as_posix() != name
            or ".." in path.parts or "\\" in name or ":" in name
            or any(part in {"", ".", ".git"} for part in path.parts)):
        raise ValueError("unsafe source-custody path: " + str(name))
    cursor = base
    if cursor.is_symlink():
        raise ValueError("redirected source-custody root")
    for part in path.parts:
        cursor = cursor / part
        if cursor.is_symlink():
            raise ValueError("source-custody symlink: " + name)
    if not cursor.is_file():
        raise ValueError("source-custody file missing: " + name)
    return cursor.read_bytes()


def receiver(repository: Path, pins: dict) -> None:
    for name, expected in {**RECEIVER_OUTPUTS, **KEEP_OUTPUTS}.items():
        data = safe_file(repository, name)
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError("receiving source-custody owner drift: " + name)
    cohort = json.loads(safe_file(repository,
        "m3/runtime-integration/recipe/current-cohort-20261017.json"))
    if (cohort["schema"] != "M3_RUNTIME_CURRENT_SOURCE_CUSTODY_V1"
            or cohort["scope"] != "EXACT_CURRENT_SOURCE_CUSTODY_ONLY"
            or cohort["sourceCustodyOnly"] is not True
            or any(cohort[flag] is not False for flag in (
                "runtimeQualified", "jniQualified", "jdkQualified",
                "performanceQualified", "phaseAdvanced"))):
        raise ValueError("receiving source-custody scope drift")
    legacy = json.loads(safe_file(repository, "m3/runtime-integration/recipe/manifest.json"))
    files, auxiliaries = cohort["files"], cohort["auxiliaries"]
    rows = files + auxiliaries
    if (len(files) != 66 or len(auxiliaries) != 6 or len(rows) != 72
            or len({row["path"] for row in rows}) != 72
            or {row["path"] for row in files} != set(legacy["files"])):
        raise ValueError("receiving complete 72-owner custody inventory drift")
    for row in rows:
        data = safe_file(repository, row["path"])
        if (len(data) != row["bytes"] or git_blob(data) != row["gitBlob"]
                or hashlib.sha256(data).hexdigest() != row["sha256"]):
            raise ValueError("receiving complete 72-owner source drift: " + row["path"])
    evidence = cohort["provenance"]["buildSourceEvidence"]
    if len(evidence) != 7 or len({row["path"] for row in evidence}) != 7:
        raise ValueError("receiving build-source evidence inventory drift")
    for row in evidence:
        data = safe_file(repository, row["path"])
        if (git_blob(data) != row["gitBlob"]
                or hashlib.sha256(data).hexdigest() != row["sha256"]):
            raise ValueError("receiving build-source evidence drift: " + row["path"])
    print("M3_RUNTIME_CURRENT_CUSTODY_RECEIVER_EXACT owners=72 generated=4 immutableInputs=2 buildEvidence=7")


def self_test_reports() -> None:
    """Prove exact provider-derived names without stripping parameter signatures."""
    # Independent literal oracle matches the sealed Java methods and pinned provider.
    expected_suites = {"com.synexia.rewrite.M3RuntimeCurrentCustodyRecipeTest":["runtimeCurrentCustodyEveryPartialStateAndOrder","runtimeCurrentCustodyRefusesDriftMissingAndDuplicateOwnership"]}
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
    print("M3_RUNTIME_CURRENT_CUSTODY_SUREFIRE_SELFTEST_PASS refused=" + str(refused))


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
                "src/test/resources/com/synexia/rewrite/unsealed/"
                "m3-runtime-current-custody-20261017/Extra.txt",
                "src/main/resources/com/synexia/rewrite/unsealed/"
                "m3-runtime-current-custody-20261017/extra-context.tsv",
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
            for classname, methods in RUNTIME_TESTS.items():
                changed = json.loads(pins_data)
                del changed["runtimeTests"][classname]
                (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                rejects()
                for index, method in enumerate(methods):
                    changed = json.loads(pins_data)
                    changed["runtimeTests"][classname][index] = method + "_unreviewed"
                    (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
                    rejects()
            changed = json.loads(pins_data)
            changed["runtimeTests"]["ForeignTest"] = ["test_unreviewed"]
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            changed = json.loads(pins_data)
            changed["expectedRuntimeTestMethods"] = 0
            (fixture / "pins.json").write_text(json.dumps(changed), encoding="utf-8")
            rejects()
            name = "src/test/java/com/synexia/rewrite/M3RuntimeCurrentCustodyRecipeTest.java"
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
            assert refused == (5 + len(REQUIRED_PATHS) + len(REQUIRED_GIT_BLOBS)
                               + len(REQUIRED_SHA256) + 1 + len(RUNTIME_TESTS)
                               + sum(len(methods) for methods in RUNTIME_TESTS.values()) + 2)
        finally:
            ROOT = original
    print("M3_RUNTIME_CURRENT_CUSTODY_RECEIVING_CLOSURE_SELFTEST_PASS refused=" + str(refused))


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
        print("M3_RUNTIME_CURRENT_CUSTODY_RECEIVING_SDK_INPUTS_EXACT")
        return
    directory = ROOT / "target/surefire-reports"
    expected = sorted(directory / ("TEST-" + name + ".xml") for name in SUITES)
    if sorted(directory.glob("TEST-*.xml")) != expected:
        raise ValueError("missing or unexpected SDK test reports")
    reports = {}
    for name, methods in SUITES.items():
        data = safe_file(ROOT, "target/surefire-reports/TEST-" + name + ".xml")
        reports[name] = {"counts": validate_report(data, name, methods),
                         "sha256": hashlib.sha256(data).hexdigest()}
    emitted = ROOT / "target/emitted/runtime-current-custody"
    expected_files = set(RECEIVER_OUTPUTS)
    actual_files = {path.relative_to(emitted).as_posix()
                    for path in emitted.rglob("*") if path.is_file()}
    if actual_files != expected_files:
        raise ValueError("runtime-custody recipe output inventory drift")
    for name, digest in RECEIVER_OUTPUTS.items():
        emitted_data = safe_file(emitted, name)
        if hashlib.sha256(emitted_data).hexdigest() != digest:
            raise ValueError("runtime-custody recipe output drift: " + name)
        if args.repository is not None and safe_file(args.repository.resolve(), name) != emitted_data:
            raise ValueError("actual receiving owner differs from Maven-emitted output: " + name)
    receipt = {
        "schema": "M3_RUNTIME_CURRENT_CUSTODY_RECEIVING_SDK_RECEIPT_V1",
        "scope": "SOURCE_CUSTODY_RECIPE_OUTPUTS_ONLY",
        "ciReportedSha": os.environ.get("GITHUB_SHA", "UNKNOWN"),
        "providers": {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
                      "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2"},
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "reports": reports,
        "sdkMethods": 2,
        "expectedOwnedStates": 16,
        "expectedSourceOrdersPerState": 2,
        "expectedBasicOwnershipRefusals": 16,
        "emittedOutputs": RECEIVER_OUTPUTS,
        "immutableHistoricalInputs": KEEP_OUTPUTS,
        "receivingActualOutputsCompared": args.repository is not None,
        "sourceCustodyPaths": 72,
        "buildSourceEvidenceFiles": 7,
        "expectedRuntimeTestMethods": 17,
        "runtimeTests": RUNTIME_TESTS,
        "runtimeTestsExecutedByThisModule": False,
        "matchedJdkJtreg": "NOT_RUN",
        "jniExecution": "NOT_RUN",
        "performanceAndRetention": "NOT_RUN",
        "runtimeApplied": False,
        "phaseAdvanced": False,
    }
    output = ROOT / "target/sdk-receipt.json"
    output.write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(receipt, sort_keys=True))



if __name__ == "__main__":
    main()
