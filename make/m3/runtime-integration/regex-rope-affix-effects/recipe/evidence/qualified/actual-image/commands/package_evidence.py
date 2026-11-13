#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Retain a bounded, text-only copy of the final actual-image test evidence."""

import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def file_digest(path):
    value = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    workspace = args.workspace.resolve(strict=True)
    runtime = workspace / "regex-work/runtime"
    fixture = runtime / "fixture-final"
    image = runtime / "actual-image"
    reviews = workspace / "regex-work/review"
    output = args.output.resolve()
    if output.exists():
        raise ValueError("evidence output must be absent")
    binding = json.loads((fixture / "SDK_BINDING.json").read_text())
    for category, root in (
            ("jdk_image_sha256", Path(binding["jdk"])),
            ("harness_jars_sha256", Path(binding["harness"])),
            ("core_sha256", Path(binding["build_source"]))):
        for relative, expected in binding[category].items():
            if file_digest(root / relative) != expected:
                raise ValueError("final identity drift: " + str(root / relative))
    for relative, expected in binding["test_overlays_sha256"].items():
        if file_digest(fixture / "source" / relative) != expected:
            raise ValueError("executed SDK test drift: " + relative)

    copied = {}

    def retain(source, relative):
        source = source.resolve(strict=True)
        relative = Path(relative)
        if source.is_symlink() or not source.is_file():
            raise ValueError("only regular source evidence is admitted")
        if relative.is_absolute() or ".." in relative.parts:
            raise ValueError("unsafe destination")
        if str(relative) in copied:
            raise ValueError("duplicate evidence destination")
        if source.suffix.lower() in {".class", ".so", ".jar", ".zip", ".tar", ".jta"}:
            raise ValueError("binary evidence is not admitted")
        data = source.read_bytes()
        data.decode("utf-8")
        if b"\0" in data:
            raise ValueError("NUL byte in supposed text evidence")
        destination = output / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        copied[str(relative)] = (sha256(data), len(data), str(source))

    for name in ("SDK_BINDING.json", "PREREQUISITES.json", "SOURCE_INVENTORY.tsv",
                 "GROUP_PREREQUISITES.json"):
        retain(fixture / name, "identity/" + name)
    for source in sorted((fixture / "sdk").iterdir()):
        retain(source, "identity/sdk/" + source.name)
    retain(Path("/tmp/m3-toolchain/jtreg-7.3.1+1-receipt.json"),
           "identity/jtreg-publisher-receipt.json")
    retain(Path(binding["jdk"]) / "release", "identity/jdk-release.txt")
    for name in ("TEST.ROOT", "TEST.groups"):
        retain(fixture / "source/test/jdk" / name, "identity/test-root/" + name)
    for relative in binding["test_overlays_sha256"]:
        retain(fixture / "source" / relative, "identity/sdk-test-sources/" + Path(relative).name)
    for name in ("stage_prerequisites.py", "stage_group_prerequisites.py",
                 "prepare_sdk_fixture.py", "execute_jtreg.py", "package_evidence.py",
                 "M3RegexOptionalFactsProbe.java", "M3StringLegacyFixtureProbe.java",
                 "M3StringRegexMethodProbe.java", "group-prerequisites.json"):
        retain(runtime / name, "commands/" + name)
    retain(image / "native/NATIVE_BUILD.json", "native/NATIVE_BUILD.json")

    suite_results = {}
    for label in ("critical-final", "critical-final-interpreted", "retained-focused"):
        suite = image / label
        receipt = json.loads((suite / "RUN.json").read_text())
        retain(suite / "RUN.json", label + "/RUN.json")
        retain(suite / "jtreg.log", label + "/jtreg.log")
        retain(suite / "report/text/summary.txt", label + "/summary.txt")
        tests = {}
        for source in sorted((suite / "work").rglob("*.jtr")):
            text = source.read_text()
            statuses = re.findall(r"^test result: (.*)$", text, flags=re.M)
            if len(statuses) != 1:
                raise ValueError("expected exactly one jtreg final result")
            counters = re.findall(r"^M3_STRING_REGEX_(?:DP|PREPARED_BATCH)_PASS.*$", text,
                                  flags=re.M)
            tests[source.stem] = {"result": statuses[0], "reported_counters": counters}
            retain(source, label + "/jtr/" + source.name + ".log")
        suite_results[label] = {"status": receipt["status"],
                                "exit_code": receipt["exit_code"],
                                "seconds": receipt["seconds"], "tests": tests}
    if (suite_results["critical-final"]["status"] != "PASS"
            or len(suite_results["critical-final"]["tests"]) != 2
            or suite_results["critical-final-interpreted"]["status"] != "PASS"
            or len(suite_results["critical-final-interpreted"]["tests"]) != 1):
        raise ValueError("the required final actual-image runs did not all pass")
    retained = suite_results["retained-focused"]["tests"]
    if (len(retained) != 11 or sum(row["result"].startswith("Passed.")
                                   for row in retained.values()) != 7):
        raise ValueError("retained result inventory changed")

    for label in ("legacy-fixture-diagnosis", "optional-facts-precondition", "affix-methods"):
        for source in sorted((image / label).iterdir()):
            if source.is_file():
                retain(source, "diagnosis/" + label + "/" + source.name)
    for source in sorted((image / "critical").iterdir()):
        if source.is_file():
            retain(source, "failures/initial-sparse-harness/" + source.name)
    for label in ("amalgamation-timeout-20261017", "compile-inventory-20261017",
                  "jtreg-optional-facts-20261017", "jtreg-oracle-correction-20261017",
                  "jtreg-singleton-owner-20261017",
                  "jtreg-whole-template-qualification-20261017"):
        for source in sorted((reviews / label).iterdir()):
            if source.is_file():
                name = source.name + ".log" if source.suffix == ".jtr" else source.name
                retain(source, "refinements/" + label + "/" + name)
    retain(reviews / "process-group-timeout-proof.log",
           "refinements/process-group-timeout-proof.log")
    retain(reviews / "batch-review.md", "review/batch-review.md")
    contract = workspace / "regex-work/final-sdk-contract-v4/evidence"
    for source in sorted(contract.iterdir()):
        if source.is_file() and source.suffix in {".json", ".log"}:
            retain(source, "source-contract/" + source.name)
    contract_receipt = json.loads((contract / "SDK_RUNTIME_RECEIPT.json").read_text())
    if contract_receipt["status"] != "PASS_WITH_BASELINE_HOLD":
        raise ValueError("source contract status changed")
    if contract_receipt["manifest_sha256"] != binding["manifest_sha256"]:
        raise ValueError("runtime and source contract manifests differ")

    summary = {
        "schema": "M3_FOCUSED_ACTUAL_IMAGE_EVIDENCE_V1",
        "status": "FOCUSED_RUNTIME_PASS_WITH_UNCHANGED_LEGACY_HOLDS",
        "created_utc": datetime.now(timezone.utc).isoformat(),
        "baseline_revision": "3bbb4712fb20adba1405741db8457164b9f436f5",
        "sdk_generated_test_sha256": binding["test_overlays_sha256"],
        "manifest_sha256": binding["manifest_sha256"],
        "sdk_binding_sha256": file_digest(fixture / "SDK_BINDING.json"),
        "image_and_harness_rechecked_after_tests": True,
        "suites": suite_results,
        "source_contract_status": contract_receipt["status"],
        "source_contract_baseline_holds": contract_receipt["baseline_holds"],
        "legacy_fixture_diagnosis": "diagnosis/legacy-fixture-diagnosis/DIAGNOSIS.json",
        "legacy_tests_modified": False,
        "required_jtreg_version_modified": False,
        "product_core_modified_during_fixture_refinements": False,
        "full_string_stage_accepted": False,
        "complete_regex_suite": "NOT_RUN; bounded focused selection only",
        "cuda_device": "NOT_RUN",
        "product_throughput": "NOT_RUN",
        "binary_artifacts_included": False,
        "recording_scope": "Exact text receipts, executed test sources, source inventories, helpers and JTRs; no full fixture tree, classes, native libraries, image or harness jars.",
    }
    summary_bytes = (json.dumps(summary, indent=2) + "\n").encode("utf-8")
    (output / "RUNTIME_SUMMARY.json").write_bytes(summary_bytes)
    copied["RUNTIME_SUMMARY.json"] = (sha256(summary_bytes), len(summary_bytes),
                                      "DERIVED_FROM_RETAINED_EXACT_RECEIPTS")
    retain(runtime / "EVIDENCE_README.md", "README.md")
    rows = ["path\tsha256\tbytes\torigin\n"]
    rows.extend(relative + "\t" + value[0] + "\t" + str(value[1]) + "\t" + value[2] + "\n"
                for relative, value in sorted(copied.items()))
    inventory = "".join(rows).encode("utf-8")
    (output / "COPY_INVENTORY.tsv").write_bytes(inventory)
    # Verify the copy independently of the source mapping after all writes.
    actual_files = {str(path.relative_to(output)) for path in output.rglob("*") if path.is_file()}
    if actual_files != set(copied) | {"COPY_INVENTORY.tsv"}:
        raise ValueError("unexpected file in compact evidence")
    for relative, (wanted, count, origin) in copied.items():
        data = (output / relative).read_bytes()
        if sha256(data) != wanted or len(data) != count or b"\0" in data:
            raise ValueError("copy verification failed: " + relative)
        data.decode("utf-8")
    print(json.dumps({"output": str(output), "retained_files": len(copied),
                      "total_files_including_inventory": len(actual_files),
                      "bytes_including_inventory": sum(value[1] for value in copied.values()) + len(inventory),
                      "copy_inventory_sha256": sha256(inventory),
                      "runtime_summary_sha256": sha256(summary_bytes),
                      "status": summary["status"]}, indent=2))


if __name__ == "__main__":
    main()
