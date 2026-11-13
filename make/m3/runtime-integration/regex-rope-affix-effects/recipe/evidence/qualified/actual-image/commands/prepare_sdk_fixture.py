#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind the isolated jtreg fixture and built image to actual passing SDK test outputs."""

import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

SDK_TESTS = {
    "exactReplayFixedPointAndInputOrder", "mixedPreimagesAndPostimages",
    "driftMissingDuplicateAndWrongRepresentationRefuse", "postScanChangesRefuse",
    "qualifiedSdkResultsAreRetained",
}
CORE = (
    "src/java.base/share/classes/java/lang/M3StringRegexDp.java",
    "src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java",
    "src/java.base/share/native/libjava/M3RegexDpPlan.c",
)
TESTS = {
    "test/jdk/java/lang/String/M3StringRegexDpTest.java":
        "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a",
    "test/jdk/java/lang/String/M3StringRegexPreparedBatchTest.java":
        "465e1002df44eb9453b3d6c45670af4b9a9de90e7fc746d9b82a2c85423be0ef",
}
MANIFESTS = {
    "java": "d05609ee1f486274c7e17380fe977b51050a1a3e1c2e5cdebe4f5df019db4903",
    "text": "654f67f4ccfc86811d707c23d84514f08eb3dd4638e76df333816623c79b08cc",
}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def file_digest(path):
    value = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ("crate", "fixture", "build-source", "jdk", "harness"):
        parser.add_argument("--" + name, type=Path, required=True)
    args = parser.parse_args()
    crate, fixture, build_source, jdk, harness = (
        getattr(args, name).resolve(strict=True)
        for name in ("crate", "fixture", "build_source", "jdk", "harness"))
    receipt_path = fixture / "SDK_BINDING.json"
    if receipt_path.exists():
        raise ValueError("SDK fixture was already bound")
    candidate = crate / "target/candidate"
    report = crate / "target/surefire-reports/TEST-com.synexia.rewrite.M3TaskRecipeTest.xml"
    report_bytes = report.read_bytes()
    xml = ET.fromstring(report_bytes)
    tests = xml.findall("./testcase")
    if ({test.attrib["name"] for test in tests} != SDK_TESTS or len(tests) != 5
            or any(test.find(kind) is not None for test in tests
                   for kind in ("failure", "error", "skipped"))):
        raise ValueError("the exact five SDK tests have not all passed")
    properties = {item.attrib["name"]: item.attrib.get("value", "")
                  for item in xml.findall("./properties/property")}
    if properties.get("java.specification.version") != "21":
        raise ValueError("SDK qualification did not use Java21")
    if properties.get("m3.recipe.name") != "com.synexia.m3jdk.RopeAffixPreparedEffects20261017":
        raise ValueError("unexpected SDK recipe identity")
    sdk_results = (candidate / "SDK_RESULTS.tsv").read_bytes()
    if not sdk_results.startswith(b"# OpenRewrite SDK Results; no template-copy materialization\n"):
        raise ValueError("unexpected SDK Results header")
    rows = [row.split("\t") for row in sdk_results.decode("utf-8").splitlines()
            if row and not row.startswith("#")]
    if len(rows) != 15 or any(len(row) != 3 for row in rows):
        raise ValueError("incomplete SDK Results")
    outputs = {}
    for path, before, after in rows:
        rel = Path(path)
        if rel.is_absolute() or ".." in rel.parts or path in outputs:
            raise ValueError("unexpected SDK output path")
        data = (candidate / path).read_bytes()
        if digest(data) != after:
            raise ValueError("SDK output drift: " + path)
        outputs[path] = (data, before, after)
    sdk_inputs = (candidate / "SDK_INPUTS.tsv").read_bytes()
    inputs = dict(row.split("\t") for row in sdk_inputs.decode("utf-8").splitlines()
                  if row and not row.startswith("#"))
    for kind, expected in MANIFESTS.items():
        path = ("src/main/resources/com/synexia/rewrite/hash-pinned-" + kind
                + "/m3-rope-affix-effects-20261017/manifest.tsv")
        if digest((crate / path).read_bytes()) != expected or inputs.get("crate/" + path) != expected:
            raise ValueError("SDK did not bind the exact corrected manifest")
    for path in CORE:
        if file_digest(build_source / path) != outputs[path][2]:
            raise ValueError("built image source differs from SDK core: " + path)
    for path, expected in TESTS.items():
        if outputs[path][2] != expected:
            raise ValueError("unexpected corrected SDK test")
        staged = fixture / "source" / path
        before = outputs[path][1]
        if before == "ABSENT":
            if staged.exists():
                raise ValueError("new fixture test must be absent before SDK overlay")
        elif not staged.is_file() or file_digest(staged) != before:
            raise ValueError("fixture test preimage drift")
    image_hashes = {path: file_digest(jdk / path) for path in (
        "bin/java", "bin/javac", "lib/modules", "lib/libjava.so", "lib/server/libjvm.so", "release")}
    harness_hashes = {str(path.relative_to(harness)): file_digest(path)
                      for path in sorted((harness / "lib").glob("*.jar"))}
    if (report.read_bytes() != report_bytes
            or (candidate / "SDK_RESULTS.tsv").read_bytes() != sdk_results
            or (candidate / "SDK_INPUTS.tsv").read_bytes() != sdk_inputs):
        raise ValueError("SDK evidence changed while the fixture was admitted")
    evidence = fixture / "sdk"
    evidence.mkdir()
    (evidence / report.name).write_bytes(report_bytes)
    (evidence / "SDK_RESULTS.tsv").write_bytes(sdk_results)
    (evidence / "SDK_INPUTS.tsv").write_bytes(sdk_inputs)
    for path in TESTS:
        destination = fixture / "source" / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(outputs[path][0])
    receipt = {
        "status": "EXACT_SDK_TESTS_AND_BUILT_CORE_BOUND",
        "build_source": str(build_source), "jdk": str(jdk), "harness": str(harness),
        "recipe_crate": str(crate), "jdk_image_sha256": image_hashes,
        "harness_jars_sha256": harness_hashes, "core_sha256": {
            path: outputs[path][2] for path in CORE},
        "test_overlays_sha256": TESTS, "manifest_sha256": MANIFESTS,
        "sdk_report_sha256": digest(report_bytes), "sdk_results_sha256": digest(sdk_results),
        "sdk_inputs_sha256": digest(sdk_inputs),
        "jdk_build_note": "Complete images target built from the supplied source; frozen Java/C bytes equal SDK outputs.",
        "runtime_tests": "NOT_RUN",
    }
    receipt_path.write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, indent=2))


if __name__ == "__main__":
    main()
