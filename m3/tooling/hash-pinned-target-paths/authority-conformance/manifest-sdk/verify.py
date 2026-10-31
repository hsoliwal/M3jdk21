#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Thin source-seal and evidence adapter; reuses the unchanged owned Surefire validator."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import runpy
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parent
ADMISSION = "24bdaa2c54d7ff2da53f3edfcf068ddd4ffa70f7"
RESOURCE = "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020"
TARGET = "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv"
SUITE = "com.synexia.rewrite.M3ReleaseJepManifestOrderRecipeTest"
METHODS = ["bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
           "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
           "authenticStrictProviderReplaysElevenTargetsAndRejectsBothOrderFailures"]
COUNTS = {"schema": 1, "tests": 3, "successfulRuns": 16, "refusedRuns": 6,
          "emittedOutputs": 2, "authenticProviderRuns": 2,
          "authenticProviderRefusals": 2, "authenticProviderOutputs": 11,
          "sourceOnly": True, "runtimeApplied": False, "phaseAdvanced": False}
REQUIRED_PATHS = [
  "FOSS_REUSE_DECISION.tsv",
  "README.md",
  "THIRD_PARTY_NOTICES.md",
  "pinned-engine/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java",
  "pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java",
  "pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java",
  "pinned-engine/com/synexia/rewrite/M3ReleaseJepManifestOrderInverseRecipe.java",
  "pinned-engine/com/synexia/rewrite/M3ReleaseJepManifestOrderRecipe.java",
  "pom.xml",
  "src/main/resources/META-INF/rewrite/release-jep-manifest-order-20261020-inverse.yml",
  "src/main/resources/META-INF/rewrite/release-jep-manifest-order-20261020.yml",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/before/m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/inverse/manifest.tsv",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/inverse/manifest.tsv.license",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/inverse/out/m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/manifest.tsv",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/manifest.tsv.license",
  "src/main/resources/com/synexia/rewrite/release-jep-manifest-order-20261020/out/m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv",
  "src/test/java/com/synexia/rewrite/M3ReleaseJepManifestOrderRecipeTest.java",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/00-JEP_CATALOGUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/01-PROGRAM_STATUS_2026-10-06.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/04-program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/09-POST21_PRIORITY_COMPATIBILITY.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/10-JEP_RESIDUE_QUEUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-original-order/manifest.tsv",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/00-JEP_CATALOGUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/01-PROGRAM_STATUS_2026-10-06.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/02-README.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/03-RELEASE_JEP_AUTHORITY.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/04-program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/05-release_jep_authority.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/06-test_program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/07-test_release_jep_authority.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/08-verify.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/09-POST21_PRIORITY_COMPATIBILITY.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/10-JEP_RESIDUE_QUEUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-00-JEP_CATALOGUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-01-PROGRAM_STATUS_2026-10-06.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-02-README.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-04-program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-06-test_program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-08-verify.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-09-POST21_PRIORITY_COMPATIBILITY.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/before-10-JEP_RESIDUE_QUEUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/00-JEP_CATALOGUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/01-PROGRAM_STATUS_2026-10-06.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/02-README.md.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/04-program_status.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/05-release_jep_authority.py.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/09-POST21_PRIORITY_COMPATIBILITY.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/10-JEP_RESIDUE_QUEUE.tsv.txt",
  "src/test/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-row8-order/manifest.tsv",
  "src/test/resources/frozen/M3Jdk21HashPinnedTextSnapshotRecipeTest.java.txt",
  "src/test/resources/frozen/M3ReleaseJepAuthorityRepairRecipe.java.txt",
  "src/test/resources/frozen/M3ReleaseJepAuthorityRepairRecipeTest.java.txt",
  "support/owned_state_relation_verify.py",
  "verify.py"
]
FROZEN_GIT = {
  "pinned-engine/com/synexia/rewrite/M3GpuToolJoinRecipe.java": "1c1b1b568d369f688f6953161636bf36b484bf5a",
  "pinned-engine/com/synexia/rewrite/M3GpuToolJoinManifest.java": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
  "pinned-engine/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java": "4a38e39d0755462d40c2b22548b87abde938ea0b",
  "pom.xml": "d79867bcc09ad6329e12418e15907e54b8ab025f",
  "src/test/resources/frozen/M3ReleaseJepAuthorityRepairRecipeTest.java.txt": "dfdd797de55426f5e93d64c324ccccb0967c04de",
  "src/test/resources/frozen/M3Jdk21HashPinnedTextSnapshotRecipeTest.java.txt": "1223036479ba04515e21b6a6087d832e6f7f9394",
  "src/test/resources/frozen/M3ReleaseJepAuthorityRepairRecipe.java.txt": "8e40addc386c60a9430f06ebb05345a2be8a6598",
  "support/owned_state_relation_verify.py": "0c2507d78817184e9df3c367390f2ac02bc870cc"
}
BEFORE_SHA = "cc775d76fdcf53442c69ccd83558d498a53a6f0e78893bee851660612370f789"
AFTER_SHA = "ce113825c8e34a7379f65da06d9e203a84666d85051acc2fe1fec672d8374aa9"
PROVIDERS = {"openrewriteAdapter": "1c1b1b568d369f688f6953161636bf36b484bf5a",
             "manifestKernel": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
             "authenticStrictProvider": "4a38e39d0755462d40c2b22548b87abde938ea0b"}
COMPILED_PROVIDERS = [
    "com/synexia/rewrite/M3GpuToolJoinRecipe",
    "com/synexia/rewrite/M3GpuToolJoinManifest",
    "com/synexia/rewrite/M3ReleaseJepManifestOrderRecipe",
    "com/synexia/rewrite/M3ReleaseJepManifestOrderInverseRecipe",
    "com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe",
]

def require(value, message):
    if not value:
        raise ValueError(message)

def sha(raw):
    return hashlib.sha256(raw).hexdigest()

def blob(raw):
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()

def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate JSON key")
        result[key] = value
    return result

def regular(path):
    require(path.is_file() and not any(p.is_symlink() for p in [path, *path.parents]),
            "regular nonredirected file required: " + str(path))
    return path

def validate_inputs(files):
    require(set(files) == set(REQUIRED_PATHS) | {"pins.json"}, "closed SDK source inventory")
    raw = files["pins.json"]
    pins = json.loads(raw, object_pairs_hook=unique)
    require(raw == (json.dumps(pins, indent=2) + "\n").encode("utf-8"), "canonical pins bytes required")
    require(set(pins) == {"schema", "admissionCommit", "sourceOnly", "providers", "files"}
            and pins["schema"] == "M3_AUTHORITY_MANIFEST_ORDER_SDK_INPUTS_V1"
            and pins["admissionCommit"] == ADMISSION and pins["sourceOnly"] is True
            and pins["providers"] == PROVIDERS, "SDK policy identity")
    rows = pins["files"]
    require(type(rows) is list and len(rows) == len(REQUIRED_PATHS)
            and [row["path"] for row in rows] == sorted(REQUIRED_PATHS), "exact ordered nonself pins")
    for row in rows:
        require(set(row) == {"path", "gitBlob", "bytes", "sha256"}, "pin fields")
        data = files[row["path"]]
        require(type(row["bytes"]) is int and len(data) == row["bytes"]
                and sha(data) == row["sha256"] and blob(data) == row["gitBlob"], "pin content drift")
    for path, expected in FROZEN_GIT.items():
        require(blob(files[path]) == expected, "frozen owned donor changed: " + path)
    before, after = files[RESOURCE + "/before/" + TARGET], files[RESOURCE + "/out/" + TARGET]
    require(sha(before) == BEFORE_SHA and sha(after) == AFTER_SHA, "manifest pre/post identity")
    original = before.decode("utf-8").splitlines(keepends=True)
    corrected = after.decode("utf-8").splitlines(keepends=True)
    require(len(original) == len(corrected) == 11
            and corrected == [original[i - 1] for i in (1,2,3,4,6,8,5,7,9,10,11)]
            and sorted(original) == sorted(corrected), "complete original-row permutation required")
    paths = [line.split("\t")[0] for line in corrected]
    require(all(a < b for a, b in zip(paths, paths[1:])), "strict ordinal path ordering")
    for inverse in (False, True):
        prefix = RESOURCE + ("/inverse" if inverse else "")
        expected = TARGET + "\t" + (AFTER_SHA if inverse else BEFORE_SHA) + "\t" \
            + (BEFORE_SHA if inverse else AFTER_SHA) + "\tout/" + TARGET + "\n"
        require(files[prefix + "/manifest.tsv"] == expected.encode("utf-8"), "exact directional one-owner manifest")
    require(files[RESOURCE + "/inverse/out/" + TARGET] == before, "inverse restores original complete manifest")
    return pins

def inputs():
    files = {}
    for path in ROOT.rglob("*"):
        rel = path.relative_to(ROOT)
        if rel.parts[0] == "target":
            continue
        require(not path.is_symlink(), "redirected SDK source")
        if path.is_file():
            require(path.stat().st_size <= 1048576, "bounded SDK source")
            files[rel.as_posix()] = regular(path).read_bytes()
    return validate_inputs(files), files

def owned_report_validator(files):
    path = "support/owned_state_relation_verify.py"
    require(blob(files[path]) == "0c2507d78817184e9df3c367390f2ac02bc870cc", "owned XML validator identity")
    return runpy.run_path(str(regular(ROOT / path)), run_name="m3_authority_manifest_owned_xml")

def validate_counters(counter, ident, head):
    expected = {**COUNTS, "qualificationId": ident, "checkoutCommit": head}
    require(type(counter) is dict and set(counter) == set(expected)
            and all(type(counter[k]) is type(v) and counter[k] == v for k, v in expected.items()),
            "actual named-recipe/authentic-provider counters required")

def marker(ident):
    return ("M3_AUTHORITY_MANIFEST_ORDER_RECIPE_COUNTERS successful=16 refused=6 outputs=2"
            + " authenticRuns=2 authenticRefusals=2 authenticOutputs=11 qualificationId=" + ident)

def validate_marker(raw, ident):
    lines = [line for node in ET.fromstring(raw).iter("system-out")
             for line in (node.text or "").splitlines()
             if line.startswith("M3_AUTHORITY_MANIFEST_ORDER_RECIPE_COUNTERS")]
    require(lines == [marker(ident)], "one exact actual counter marker")

def self_test(files, owned):
    # Synthetic rejection controls exercise the same validators; they are not JUnit execution.
    owned["self_test_reports"]()
    refused = 0
    for path in REQUIRED_PATHS:
        changed = dict(files)
        changed[path] += b"\nunknown drift\n"
        try:
            validate_inputs(changed)
        except (ValueError, KeyError):
            refused += 1
        else:
            raise AssertionError("source drift accepted: " + path)
    for changed in ({**files, "unexpected": b""},
                    {k: v for k, v in files.items() if k != "README.md"},
                    {**files, "pins.json": files["pins.json"] + b"\n"}):
        try:
            validate_inputs(changed)
        except (ValueError, KeyError):
            refused += 1
        else:
            raise AssertionError("closed-input control accepted")
    ident, head = "manifest-order-self-test-qualification", "1" * 40
    baseline = {**COUNTS, "qualificationId": ident, "checkoutCommit": head}
    validate_counters(baseline, ident, head)
    counter_refused = 0
    for key, value in baseline.items():
        changed = dict(baseline)
        changed[key] = not value if type(value) is bool else value + 1 if type(value) is int else value + "-stale"
        try:
            validate_counters(changed, ident, head)
        except ValueError:
            counter_refused += 1
        else:
            raise AssertionError("counter control accepted: " + key)
    root = ET.Element("testsuite", {"name": SUITE, "tests": "3", "failures": "0", "errors": "0", "skipped": "0"})
    for method in METHODS:
        ET.SubElement(root, "testcase", {"name": method, "classname": SUITE})
    ET.SubElement(root, "system-out").text = marker(ident) + "\n"
    raw = ET.tostring(root)
    owned["validate_report"](raw, SUITE, METHODS)
    validate_marker(raw, ident)
    for wrong in ("", marker(ident) + "-stale", marker(ident) + "\n" + marker(ident)):
        bad = ET.Element("testsuite")
        ET.SubElement(bad, "system-out").text = wrong
        try:
            validate_marker(ET.tostring(bad), ident)
        except ValueError:
            counter_refused += 1
        else:
            raise AssertionError("marker control accepted")
    require(refused == len(REQUIRED_PATHS) + 3 and counter_refused == len(baseline) + 3, "control census")
    print(json.dumps({"state": "SELF_TEST_PASS_ONLY", "sourceRefusals": refused,
                      "counterMarkerRefusals": counter_refused, "mavenExecuted": False}, sort_keys=True))

def reports(pins, files, owned):
    ident, head = os.environ.get("M3_QUALIFICATION_ID", ""), os.environ.get("EXPECTED_SOURCE_SHA", "")
    require(re.fullmatch("[A-Za-z0-9_.-]{16,128}", ident)
            and re.fullmatch("[0-9a-f]{40}", head), "fresh execution identities")
    reports_found = sorted((ROOT / "target/surefire-reports").glob("TEST-*.xml"))
    require([p.name for p in reports_found] == ["TEST-" + SUITE + ".xml"], "one exact fresh JUnit suite")
    xml = regular(reports_found[0]).read_bytes()
    counts = owned["validate_report"](xml, SUITE, METHODS)
    counter_raw = regular(ROOT / "target/recipe-test-counters.json").read_bytes()
    counters = json.loads(counter_raw, object_pairs_hook=unique)
    validate_counters(counters, ident, head)
    validate_marker(xml, ident)
    emitted = {}
    for directory, stage in (("emitted", "out"), ("inverse-emitted", "before")):
        folder = ROOT / "target" / directory
        actual = [p.relative_to(folder).as_posix() for p in folder.rglob("*") if p.is_file()]
        require(actual == [TARGET], "one authentic directional emission")
        raw = regular(folder / TARGET).read_bytes()
        require(raw == files[RESOURCE + "/" + stage + "/" + TARGET], "authentic emitted postimage")
        emitted[directory] = {"path": "target/" + directory + "/" + TARGET,
                              "bytes": len(raw), "sha256": sha(raw), "gitBlob": blob(raw)}
    compiled = {}
    for name in COMPILED_PROVIDERS:
        raw = regular(ROOT / ("target/classes/" + name + ".class")).read_bytes()
        require(len(raw) >= 8 and raw[:4] == b"\xca\xfe\xba\xbe" and int.from_bytes(raw[6:8], "big") == 65,
                "actual Java21 source-built provider")
        compiled[name] = {"bytes": len(raw), "sha256": sha(raw), "major": 65}
    result = {"schema": "M3_AUTHORITY_MANIFEST_ORDER_SDK_RECEIPT_V1",
              "state": "PASS_SOURCE_RECIPE_ONLY", "qualificationId": ident, "checkoutCommit": head,
              "providers": pins["providers"], "inputPinsSha256": sha(files["pins.json"]),
              "reports": {SUITE: {"sha256": sha(xml), "counts": counts}},
              "counterReceiptSha256": sha(counter_raw), "counters": counters,
              "compiledProviders": compiled, "emittedOutputs": emitted,
              "recipeJUnitQualified": True, "authenticStrictProviderFixtureQualified": True,
              "originalReceivingMavenModelQualified": False, "scopeRegistryQualified": False,
              "publicTargetApplied": False, "fullJdkQualified": False, "runtimeApplied": False,
              "phaseAdvanced": False, "promotionAuthority": False}
    destination = ROOT / "target/sdk-receipt.json"
    require(not destination.exists() and not destination.is_symlink(), "fresh receipt")
    with destination.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(result, indent=2) + "\n")
    print(json.dumps({"state": result["state"], "receipt": str(destination)}, sort_keys=True))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--inputs", action="store_true")
    parser.add_argument("--reports", action="store_true")
    args = parser.parse_args()
    pins, files = inputs()
    owned = owned_report_validator(files)
    if args.self_test:
        self_test(files, owned)
    if args.reports:
        reports(pins, files, owned)
    if args.inputs and not args.self_test and not args.reports:
        print(json.dumps({"state": "INPUTS_ONLY", "files": len(files), "mavenExecuted": False}, sort_keys=True))

if __name__ == "__main__":
    main()

