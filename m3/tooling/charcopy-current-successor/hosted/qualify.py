#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind source-only charcopy tooling and inspect actual named/historical proof receipts."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
TASK = HERE.parent
RECEIVER = TASK.parents[2]
SDK = TASK / "sdk"
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ARTIFACTS = ROOT / "artifacts"
PREFIX = "m3/tooling/charcopy-current-successor/"
WORKFLOW = ".github/workflows/m3-charcopy-current-successor.yml"
SUITE = "com.synexia.rewrite.M3CharcopyCurrentSuccessorRecipeTest"
METHODS = {"namedCurrentEightKeepInputsReplayAsExactFixedPoint",
           "everyDriftMissingDuplicateLegacyAndMixedInputRefuses",
           "exactInverseAndFullHistoricalPreimagesRetainTheirIdentity"}
OLD_METHODS = {
    "test_current_canonical_tree_is_sealed_superseding_fixed_point",
    "test_historical_transform_atoms_remain_forward_reverse_reversible",
    "test_each_unknown_drift_is_refused_without_partial_write",
    "test_symlink_target_is_refused",
    "test_git_blob_uses_the_repository_nul_framing",
    "test_manifest_superseded_hashes_are_current_exact_sources",
    "test_missing_or_drifted_replacement_owner_refuses_without_recreating_history",
}
ENGINES = {"M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
           "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
           "M3CharcopyCurrentSuccessorRecipe": "b50c9e1950ae73d8ddaa4b50c7e167c36869ab8a"}
FALSE_GATES = ("originalPrivateTyped3bdeeQualified", "targetJavaCompiled", "matchedJdkQualified",
               "jniQualified", "runtimeBehaviorQualified", "performanceRetentionQualified",
               "runtimeApplied", "phaseAdvanced", "allCiGreen")


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def regular(path):
    require(path.is_file() and all(not item.is_symlink() for item in [path, *path.parents]),
            "missing or redirected qualification input: " + str(path))
    return path


def relative(value):
    path = PurePosixPath(value)
    require(value and not path.is_absolute() and "\\" not in value and ":" not in value
            and "\0" not in value and all(part not in ("", ".", "..") for part in value.split("/")),
            "unsafe qualification path")
    return path


def digest(data):
    return hashlib.sha256(data).hexdigest()


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def identity(data):
    return {"bytes": len(data), "sha256": digest(data), "gitBlob": blob(data)}


def git(*arguments, binary=False):
    result = subprocess.check_output(["git", *arguments], cwd=RECEIVER,
                                     timeout=60, text=not binary)
    return result if binary else result.strip()


def write(name, value):
    require(ARTIFACTS.is_dir()
            and all(not item.is_symlink() for item in [ARTIFACTS, *ARTIFACTS.parents]),
            "missing or redirected artifact directory")
    destination = ARTIFACTS / name
    require(not destination.exists() and not destination.is_symlink(), "fresh receipt required")
    raw = (json.dumps(value, indent=2) + "\n").encode("utf-8")
    require(len(raw) <= 1024 ** 2, "bounded hosted receipt required")
    with destination.open("xb") as stream:
        stream.write(raw)


def binding(fresh=False):
    head = git("rev-parse", "HEAD")
    require(head == os.environ["EXPECTED_SOURCE_SHA"] and len(head) == 40, "exact head required")
    binding_path = TASK / "source-binding.json"
    raw = regular(binding_path).read_bytes()
    require(blob(raw) == git("rev-parse", "HEAD:" + PREFIX + "source-binding.json"),
            "source binding differs from exact checkout")
    record = json.loads(raw)
    require(record["repository"] == "hsoliwal/com.synexia"
            and record["receiverRepository"] == "hsoliwal/M3jdk21"
            and record["receiverBaseCommit"] == "677437be295b23f7ccf7409be287008944d0e8b0"
            and record["runtimeApplied"] is False and record["phaseAdvanced"] is False,
            "binding identity or source-only scope drift")
    revision = record["sourceCommit"]
    require(len(revision) == 40 and all(c in "0123456789abcdef" for c in revision),
            "canonical source commit required")
    rows = record["files"]
    require(len(rows) == 24, "exact 24-file authoring copy required")
    seen = set()
    for row in rows:
        name = str(relative(row["targetPath"]))
        require(name not in seen and (name.startswith(PREFIX) or name == WORKFLOW),
                "source binding ownership drift")
        seen.add(name)
        actual = identity(regular(RECEIVER / name).read_bytes())
        require(all(actual[key] == row[key] for key in actual), "canonical source differs: " + name)
    raw_inputs = regular(TASK / "INPUTS.json").read_bytes()
    inputs = json.loads(raw_inputs)
    require(inputs["schema"] == 1 and inputs["capBytes"] == 512 * 1024 ** 2
            and inputs["reserveBytes"] == 8 * 1024 ** 3 and len(inputs["sourceFiles"]) == 20
            and len(inputs["receiverFiles"]) == 8, "source policy or closure drift")
    selected = {}
    for row in inputs["sourceFiles"]:
        name = str(relative(row["path"]))
        require(name not in selected, "duplicate source pin")
        actual = identity(regular(TASK / name).read_bytes())
        require(all(actual[key] == row[key] for key in actual), "canonical input drift: " + name)
        selected[name] = actual
    receiver = {}
    require(record["receiverFiles"] == inputs["receiverFiles"], "receiver pin binding differs")
    for row in inputs["receiverFiles"]:
        name = str(relative(row["path"]))
        require(name not in receiver, "duplicate receiver pin")
        actual = identity(regular(RECEIVER / name).read_bytes())
        require(all(actual[key] == row[key] for key in actual)
                and actual["gitBlob"] == git("rev-parse", "HEAD:" + name),
                "actual receiver differs: " + name)
        receiver[name] = actual
    for name in ("src/java.base/share/classes/java/lang/MIndexString.java",
                 "m3/runtime-integration/tests/MIndexStringInvariant.java"):
        require(not git("ls-tree", "-r", "--name-only", "HEAD", "--", name),
                "tracked legacy receiver path forbidden: " + name)
        path = RECEIVER / name
        require(not path.exists() and not path.is_symlink(), "legacy receiver path forbidden")
    expected = {PREFIX + name for name in selected} | {
        PREFIX + "INPUTS.json", PREFIX + "hosted/qualify.py", PREFIX + "hosted/README.md", WORKFLOW}
    require(seen == expected, "exact source copy set differs")
    tracked = set(git("ls-files", "-z", "--", PREFIX, WORKFLOW, binary=True).decode("utf-8").split("\0"))
    tracked.discard("")
    require(tracked == expected | {PREFIX + "source-binding.json"},
            "unexpected or missing tracked authoring input")
    require(os.environ["M3_RECEIVER_ROOT"] == str(RECEIVER)
            and os.environ["M3_QUALIFICATION_ROOT"] == str(ROOT), "explicit source/output routing required")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--",
                    PREFIX, WORKFLOW, *receiver], cwd=RECEIVER, check=True, timeout=60)
    if fresh:
        require(not (SDK / "target").exists() and not (ROOT / "charcopy-replay").exists()
                and not (ARTIFACTS / "charcopy-guard-sdk.json").exists()
                and not any((ROOT / "m2").rglob("*.jar")), "all proof outputs and isolated Maven repo must start fresh")
    return record, selected, receiver, raw_inputs


def referenced(reference):
    path = ROOT / str(relative(reference["path"]))
    raw = regular(path).read_bytes()
    require(len(raw) == reference["bytes"] and digest(raw) == reference["sha256"],
            "execution receipt link differs: " + str(path))
    return path, raw


def class_identity(path):
    raw = regular(path).read_bytes()
    require(len(raw) >= 8 and raw[:4] == b"\xca\xfe\xba\xbe"
            and int.from_bytes(raw[6:8], "big") == 65, "actual Java21 provider class required")
    return {"bytes": len(raw), "sha256": digest(raw), "major": 65}


def report():
    record, selected, receiver, raw_inputs = binding()
    ident = os.environ["M3_QUALIFICATION_ID"]
    execution_raw = regular(ARTIFACTS / "charcopy-execution.json").read_bytes()
    execution = json.loads(execution_raw)
    require(execution["schema"] == 1 and execution["qualificationId"] == ident
            and execution["state"] == "PASS_SOURCE_SUCCESSOR_CUSTODY"
            and execution["scope"] == "NAMED_SDK_CURRENT_ZERO_WRITE_AND_ISOLATED_HISTORICAL_PROGRAM_REPLAY",
            "fresh actual source-only proof required")
    require(execution["sourceFoundation"] == "50cb3dc489c3f95a9781e50549d2e1866390f057"
            and execution["receiverCommit"] == record["receiverBaseCommit"]
            and execution["currentRuntimeWrites"] == 0
            and execution["legacyScriptsUnchanged"] is True
            and execution["legacyProgramsAreSeparateExecutables"] is True
            and execution["historicalLicensesRelabeled"] is False, "source/historical custody scope drift")
    require(all(execution[key] is False for key in FALSE_GATES), "source proof exceeds acceptance")
    storage = execution["storage"]
    require(storage["capBytes"] == 512 * 1024 ** 2 and storage["reserveBytes"] == 8 * 1024 ** 3
            and storage["externalAggregateAndProcessGroupSupervisorRequired"] is True
            and storage["selfEnforcedAggregateClaim"] is False, "external storage/process fence required")
    _, custody_raw = referenced(execution["sourceCustody"])
    custody = json.loads(custody_raw)
    require(custody["schema"] == 1 and custody["qualificationId"] == ident and custody["unchanged"] is True
            and custody["runtimeWrites"] == 0 and custody["inputManifestSha256"] == digest(raw_inputs)
            and custody["canonicalBefore"] == selected and custody["canonicalAfter"] == selected
            and custody["receiverBefore"] == receiver and custody["receiverAfter"] == receiver,
            "complete before/after source identity differs")
    xml_path = SDK / ("target/surefire-reports/TEST-" + SUITE + ".xml")
    require(sorted((SDK / "target/surefire-reports").glob("TEST-*.xml")) == [xml_path],
            "one actual three-method SDK suite required")
    xml = regular(xml_path).read_bytes()
    suite = ET.fromstring(xml)
    counts = {name: int(suite.get(name, "-1")) for name in ("tests", "failures", "errors", "skipped")}
    cases = suite.findall("testcase")
    require(suite.tag == "testsuite" and suite.get("name") == SUITE
            and counts == {"tests": 3, "failures": 0, "errors": 0, "skipped": 0}
            and len(cases) == 3 and {row.get("name") for row in cases} == METHODS
            and all(row.get("classname") == SUITE
                    and all(child.tag in ("system-out", "system-err") for child in row) for row in cases),
            "all three named SDK methods must execute without skips")
    sdk = execution["sdk"]
    _, sdk_raw = referenced(sdk)
    sdk_receipt = json.loads(sdk_raw)
    require(sdk["reportRoot"] == "SDK" and sdk["report"] == xml_path.relative_to(SDK).as_posix()
            and sdk["reportSha256"] == digest(xml) and sdk["reportBytes"] == len(xml)
            and sdk_receipt["qualificationId"] == ident and sdk_receipt["state"] == "PASS_SOURCE_ONLY"
            and sdk_receipt["tests"] == 3 and sdk_receipt["successfulSdkRuns"] == 4
            and sdk_receipt["refusedSdkRuns"] == 37
            and sdk_receipt["runtimeApplied"] is False and sdk_receipt["phaseAdvanced"] is False,
            "actual SDK receipt linkage/counters differ")
    _, filesystem_raw = referenced(execution["filesystem"])
    filesystem = json.loads(filesystem_raw)
    require(filesystem["tests"] == 3 and filesystem["failures"] == 0 and filesystem["errors"] == 0
            and filesystem["skipped"] == 0 and len(filesystem["refusalCases"]) == 38
            and len(set(filesystem["refusalCases"])) == 38
            and filesystem["retainedFixtureDirectories"] is True
            and filesystem["scope"] == "REAL_FILESYSTEM_COPIES_NOT_RUNTIME_OR_RECIPE_OUTPUT",
            "actual filesystem suite/cases differ")
    history = execution["historical"]
    require(history["fullInverseByteExact"] is True and history["originalTests"] == 7
            and set(history["originalTestMethods"]) == OLD_METHODS
            and history["refusals"] == 17 and len(history["refusalCases"]) == 17
            and len(set(history["refusalCases"])) == 17, "full historical replay or original tests differ")
    delta = history["currentDelta"]
    require(delta["blocks"] == 2 and delta["blockBytes"] == 143 and delta["totalBytes"] == 286
            and delta["guardSha256"] == "01ff1152e9e3100536675add7ff80f0d551bb7de1ea18999bcae803ff6da5b74"
            and delta["recovered"] == {
                "bytes": 55181, "sha256": "cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728",
                "gitBlob": "35ed998540e044eed8cb7596e2e2c019ae369bd3"},
            "whole current-to-predecessor inverse differs")
    labels = ["legacy-before-check", "legacy-forward", "legacy-after-check",
              "legacy-forward-fixed-point", "legacy-inverse", "legacy-inverse-fixed-point",
              "superseded-check", "superseded-forward", "superseded-reverse",
              "legacy-original-seven-tests"] + ["legacy-refusal-" + str(i) for i in range(1, 18)]
    require([row["label"] for row in history["commands"]] == labels, "exact historical command sequence required")
    commands = []
    for row in history["commands"]:
        raw = regular(ROOT / str(relative(row["log"]))).read_bytes()
        require(len(raw) == row["bytes"] and digest(raw) == row["sha256"]
                and ((row["exitCode"] != 0) if row["label"].startswith("legacy-refusal-")
                     else (row["exitCode"] == 0)), "actual historical command log/exit differs")
        commands.append({"label": row["label"], "exitCode": row["exitCode"],
                         "log": row["log"], "bytes": len(raw), "sha256": digest(raw)})
    original = regular(ARTIFACTS / "legacy-original-seven-tests.log").read_text(encoding="utf-8")
    require("Ran 7 tests in " in original and original.rstrip().endswith("OK")
            and all(sum(line.startswith(name + " ") for line in original.splitlines()) == 1
                    for name in OLD_METHODS), "actual unchanged seven-test output required")
    for name in ("guard-sdk-maven.log", "root-maven.log"):
        require("[INFO] BUILD SUCCESS" in regular(ARTIFACTS / name).read_text(encoding="utf-8"),
                "actual Maven command success required")
    providers = {}
    for name, expected in ENGINES.items():
        owner = SDK / ("pinned-engine/com/synexia/rewrite/" + name + ".java")
        require(blob(regular(owner).read_bytes()) == expected, "provider source drift")
        providers[name] = class_identity(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
    write("qualification-receipt.json", {
        "schema": "M3_CHARCOPY_SUCCESSOR_HOSTED_QUALIFICATION_V1",
        "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "canonicalSourceCommit": record["sourceCommit"],
        "qualificationId": ident, "sourceBindingSha256": digest(regular(TASK / "source-binding.json").read_bytes()),
        "inputManifestSha256": digest(raw_inputs), "canonicalInputFiles": len(selected),
        "receiverInputFiles": len(receiver), "executionReceiptSha256": digest(execution_raw),
        "sourceCustodySha256": digest(custody_raw), "sdkReceiptSha256": digest(sdk_raw),
        "filesystemReceiptSha256": digest(filesystem_raw), "surefireXmlSha256": digest(xml),
        "suite": SUITE, "methods": sorted(METHODS), "counts": counts,
        "sdkSuccessfulRuns": 4, "sdkRefusedRuns": 37, "filesystemTests": 3, "filesystemRefusals": 38,
        "historicalOriginalTests": 7, "historicalRefusals": 17, "compiledProviderClasses": providers,
        "commands": commands, "currentSourceSuccessorCustodyQualified": True,
        "isolatedHistoricalProgramReplayQualified": True, "targetJavaCompiled": False,
        "originalPrivateTyped3bdeeQualified": False, "jniQualified": False,
        "matchedJdkQualified": False, "runtimeBehaviorQualified": False,
        "performanceRetentionQualified": False, "runtimeApplied": False,
        "phaseAdvanced": False, "allCiGreen": False, "externalToolchainBytesSealed": False})


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        record, selected, receiver, raw_inputs = binding(fresh=True)
        write("binding-receipt.json", {"schema": 1, "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"], "canonicalInputFiles": len(selected),
              "receiverInputFiles": len(receiver), "sourceBindingSha256": digest(regular(TASK / "source-binding.json").read_bytes()),
              "inputManifestSha256": digest(raw_inputs), "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
