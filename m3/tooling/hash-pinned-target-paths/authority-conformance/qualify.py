#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Wrap the unchanged API qualifier and each source-sealed SDK's own evidence validators."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import runpy
import subprocess

HERE = Path(__file__).resolve().parent
PREFIX = "m3/tooling/hash-pinned-target-paths/authority-conformance/"
WORKFLOW = ".github/workflows/m3-hash-pinned-target-paths.yml"
ORIGINAL = HERE.parent / "qualify.py"
ORIGINAL_GIT = "8e1020b02531dc76ec5f59ac4284a2278393a1f5"
ORIGINAL_WORKFLOW_GIT = "2c87b2d062d3834e7144255c2db6b5f09d45ca18"
CONTRACTS = {
    "scope": {
        "sourceCommit": "723b74515f35d3192834dded9f163af671c5bac7",
        "sourceTree": "3e3a863f12be6cd1cc390579655f7e31a5766e50",
        "sdkFiles": 22, "verifyGitBlob": "ded464e1f4618d19d0ac76f78ab36707fc4aca7f",
        "baselineCounterexamples": 9,
        "suites": {"com.synexia.rewrite.M3CanonicalScopeBoundaryRecipeTest": [
            "bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
            "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
            "actualEmittedClassesPreserveAcceptedScopesAndFixCounterexamples"]},
        "emittedOwners": ["synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3ScopeInference.java"],
        "counters": {"tests": 3, "successfulRuns": 18, "refusedRuns": 16, "emittedOutputs": 2},
        "schema": "M3_CANONICAL_SCOPE_BOUNDARY_SDK_RECEIPT_V1",
    },
    "manifest": {
        "sourceCommit": "0b81b343363f74bbe78000143bff5e77e02696dc",
        "sourceTree": "eb0f89026b1618164a998f4dc9e36cf54bfa09ec",
        "sdkFiles": 59,
        "suites": {"com.synexia.rewrite.M3ReleaseJepManifestOrderRecipeTest": [
            "bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
            "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
            "authenticStrictProviderReplaysElevenTargetsAndRejectsBothOrderFailures"]},
        "emittedOwners": ["m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/release-jep-authority-repair-20261006/manifest.tsv"],
        "counters": {"tests": 3, "successfulRuns": 16, "refusedRuns": 6, "emittedOutputs": 2,
                     "authenticProviderRuns": 2, "authenticProviderRefusals": 2, "authenticProviderOutputs": 11},
        "schema": "M3_AUTHORITY_MANIFEST_ORDER_SDK_RECEIPT_V1",
    },
}
FORBIDDEN = ("originalReceivingMavenModelQualified", "canonicalModuleQualified",
             "fullMatchedJdkQualified", "windowsQualified", "performanceRetentionQualified",
             "productTargetApplied", "runtimeApplied", "phaseAdvanced", "promotionAuthority")


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def blob(raw):
    return hashlib.sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()


def regular(path):
    require(path.is_file() and not any(p.is_symlink() for p in [path, *path.parents]),
            "missing or redirected authority input: " + str(path))
    return path


require(blob(regular(ORIGINAL).read_bytes()) == ORIGINAL_GIT, "unchanged original API qualifier required")
API = runpy.run_path(str(ORIGINAL), run_name="m3_authority_owned_api")
RECEIVER, ROOT, ARTIFACTS = API["RECEIVER"], API["ROOT"], API["ARTIFACTS"]


def unique(pairs):
    result = {}
    for key, value in pairs:
        require(key not in result, "duplicate authority JSON key")
        result[key] = value
    return result


def read_json(path):
    return json.loads(regular(path).read_bytes(), object_pairs_hook=unique)


def digest(path):
    return hashlib.sha256(regular(path).read_bytes()).hexdigest()


def sdk_inputs(lane):
    folder = HERE / (lane + "-sdk")
    contract = CONTRACTS[lane]
    module = runpy.run_path(str(regular(folder / "verify.py")), run_name="m3_authority_" + lane)
    if "verifyGitBlob" in contract:
        require(blob(regular(folder / "verify.py").read_bytes()) == contract["verifyGitBlob"],
                "corrected source-qualified scope verifier required")
    pins, payload = module["inputs"]()
    require({module["SUITE"]: module["METHODS"]} == contract["suites"], "exact named SDK suite required")
    require(len(pins["files"]) + 1 == contract["sdkFiles"], "complete SDK pin closure required")
    return folder, module, pins, payload


def binding(fresh=False):
    # The original helper validates its complete 31-file closure and unchanged seven-owner plan.
    original = API["binding"]()
    head = API["git"]("rev-parse", "HEAD")
    require(head == os.environ["EXPECTED_SOURCE_SHA"] and re.fullmatch("[0-9a-f]{40}", head),
            "exact candidate checkout required")
    path = regular(HERE / "source-binding.json")
    require(blob(path.read_bytes()) == API["git"]("rev-parse", "HEAD:" + PREFIX + "source-binding.json"),
            "authority binding differs from exact checkout")
    record = read_json(path)
    identity = {
        "schema": "M3_RECIPE_AUTHORITY_SOURCE_BINDING_V1",
        "repository": "hsoliwal/com.synexia", "receiverRepository": "hsoliwal/M3jdk21",
        "admissionCommit": "24bdaa2c54d7ff2da53f3edfcf068ddd4ffa70f7",
        "controlPlaneAdmission": "f6c99310cc01eaa3eacb496e12cd549836da3b4c",
        "censusAdmission": "400db8f4e351a1dd385dda734706ae518bbd1ebe",
        "rawJavaProofAdmission": "0a262d810b8d7cc9cb28ce64b715f1c0190aa051",
        "originalQualifierGitBlob": ORIGINAL_GIT, "originalWorkflowGitBlob": ORIGINAL_WORKFLOW_GIT,
        "artifactMembers": 46, "maxArtifactMembers": 64, "artifactCapBytes": 8388608,
        "runtimeApplied": False, "phaseAdvanced": False,
    }
    require(all(type(record.get(k)) is type(v) and record[k] == v for k, v in identity.items()),
            "authority binding identity, admission or budget drift")
    require(re.fullmatch("[0-9a-f]{40}", record["sourceCommit"]) is not None
            and record["sourceCommit"] == original["sourceCommit"], "shared canonical source commit required")
    rows, seen = record["files"], set()
    require(type(rows) is list and len(rows) == 83, "81 SDK files, controller and workflow must be sealed")
    for row in rows:
        require(set(row) == {"targetPath", "sourcePath", "gitBlob", "bytes", "sha256"},
                "complete exact source-binding tuple required")
        name = str(API["relative"](row["targetPath"]))
        API["relative"](row["sourcePath"])
        require(name not in seen and (name.startswith(PREFIX) or name == WORKFLOW),
                "child source-binding ownership drift")
        seen.add(name)
        raw = regular(RECEIVER / name).read_bytes()
        require(type(row["bytes"]) is int and len(raw) == row["bytes"]
                and blob(raw) == row["gitBlob"] and hashlib.sha256(raw).hexdigest() == row["sha256"]
                and blob(raw) == API["git"]("rev-parse", "HEAD:" + name),
                "sealed authority source differs: " + name)
    require(set(record["sdkClosures"]) == set(CONTRACTS), "exact two SDK lanes required")
    complete = {PREFIX + "qualify.py", WORKFLOW}
    for lane, contract in CONTRACTS.items():
        folder, module, pins, payload = sdk_inputs(lane)
        closure = record["sdkClosures"][lane]
        require(closure["lane"] == lane and all(closure[k] == contract[k] for k in (
            "sourceCommit", "sourceTree", "sdkFiles", "suites", "emittedOwners", "counters")),
            "source-qualified SDK contract differs: " + lane)
        prefix = PREFIX + lane + "-sdk/"
        tracked = set(API["git"]("ls-files", "-z", "--", prefix, binary=True).decode("utf-8").split("\0")) - {""}
        expected = {prefix + row["path"] for row in pins["files"]} | {prefix + "pins.json"}
        require(tracked == expected and len(tracked) == contract["sdkFiles"]
                and API["git"]("rev-parse", "HEAD:" + prefix[:-1]) == contract["sourceTree"],
                "complete exact SDK tree required: " + lane)
        raw = regular(folder / "pins.json").read_bytes()
        require(closure["inputPins"] == {"path": lane + "-sdk/pins.json", "gitBlob": blob(raw),
                "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}, "exact SDK pins tuple required")
        complete.update(expected)
        if fresh:
            require(not (folder / "target").exists() and not (folder / "target").is_symlink(),
                    "new SDK outputs must start absent")
    require(seen == complete, "no omitted or additional child binding inputs")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--", PREFIX, WORKFLOW],
                   cwd=RECEIVER, check=True, timeout=60)
    if fresh:
        require(not any((ROOT / "m2").rglob("*.jar")), "shared isolated Maven cache must start fresh")
    return record


def metadata(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)}


def validate_sdk(lane):
    folder, module, pins, payload = sdk_inputs(lane)
    contract = CONTRACTS[lane]
    ident, head = os.environ["M3_QUALIFICATION_ID"], os.environ["EXPECTED_SOURCE_SHA"]
    receipt_path = regular(folder / "target/sdk-receipt.json")
    sdk = read_json(receipt_path)
    counter_path = regular(folder / "target/recipe-test-counters.json")
    counters = read_json(counter_path)
    module["validate_counters"](counters, ident, head)
    require(all(type(counters.get(k)) is int and counters[k] == v for k, v in contract["counters"].items()),
            "actual named-recipe counters differ")
    require(sdk["schema"] == contract["schema"] and sdk["qualificationId"] == ident
            and sdk["checkoutCommit"] == head and sdk["providers"] == pins["providers"]
            and sdk["inputPinsSha256"] == digest(folder / "pins.json")
            and sdk["recipeJUnitQualified"] is True, "fresh source SDK receipt identity required")
    report_folder = folder / "target/surefire-reports"
    require(sorted(p.name for p in report_folder.glob("TEST-*.xml")) ==
            sorted("TEST-" + suite + ".xml" for suite in contract["suites"]), "exact SDK XML inventory required")
    require(sorted(p.name for p in report_folder.glob("*.txt")) ==
            sorted(suite + ".txt" for suite in contract["suites"]), "exact SDK text report inventory required")
    validator = module if lane == "scope" else module["owned_report_validator"](payload)
    for suite, methods in contract["suites"].items():
        raw = regular(report_folder / ("TEST-" + suite + ".xml")).read_bytes()
        counts = validator["validate_report"](raw, suite, methods)
        module["validate_marker"](raw, ident)
        require(sdk["reports"][suite] == {"counts": counts, "sha256": hashlib.sha256(raw).hexdigest()},
                "SDK report projection differs")
        require(regular(report_folder / (suite + ".txt")).stat().st_size > 0, "actual Surefire text report required")
    require(set(sdk["reports"]) == set(contract["suites"]), "receipt report inventory drift")
    expected_names = module["COMPILED_PROVIDERS"]
    require(set(sdk["compiledProviders"]) == set(expected_names), "compiled provider inventory differs")
    for name in expected_names:
        binary_name = "com/synexia/rewrite/" + name if lane == "scope" else name
        raw = regular(folder / ("target/classes/" + binary_name + ".class")).read_bytes()
        item = sdk["compiledProviders"][name]
        expected = {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest()}
        if lane == "scope":
            expected.update(path="target/classes/" + binary_name + ".class", majorVersion=65)
        else:
            expected["major"] = 65
        require(len(raw) > 8 and raw[:4] == bytes.fromhex("cafebabe")
                and int.from_bytes(raw[6:8], "big") == 65 and item == expected
                and all(type(item[k]) is type(v) for k, v in expected.items()),
                "actual Java21 compiled SDK provider and exact receipt projection required")
    emissions = {}
    for direction, stage in (("emitted", "out"), ("inverse-emitted", "before")):
        output = folder / "target" / direction
        require(output.is_dir() and not output.is_symlink(), "fresh directional SDK output required")
        paths = list(output.rglob("*"))
        require(not any(p.is_symlink() for p in paths)
                and sorted(p.relative_to(output).as_posix() for p in paths if p.is_file()) ==
                sorted(contract["emittedOwners"]), "exact directional SDK emission inventory required")
        values = sdk["emittedProductSourceOutputs" if direction == "emitted" else "inverseSourceOutputs"] if lane == "scope" else sdk["emittedOutputs"]
        require(set(values) == (set(contract["emittedOwners"]) if lane == "scope" else {"emitted", "inverse-emitted"}),
                "directional SDK receipt inventory differs")
        for owner in contract["emittedOwners"]:
            raw = regular(output / owner).read_bytes()
            require(raw == regular(folder / module["RESOURCE"] / stage / owner).read_bytes(),
                    "emitted SDK owner differs from sealed directional fixture")
            item = values[owner] if lane == "scope" else values[direction]
            expected = {"path": "target/" + direction + "/" + owner, **metadata(raw)}
            require(item == expected, "directional receipt projection differs")
            emissions[direction + "/" + owner] = expected
    if lane == "scope":
        require(sdk["scope"] == "SOURCE_ONLY_CANONICAL_SCOPE_BOUNDARY_PROPOSAL"
                and sdk["sourceOnly"] is True and sdk["recipeReplayQualified"] is True
                and all(sdk[k] == v and type(sdk[k]) is int for k, v in contract["counters"].items())
                and sdk["unchangedReceiverCustody"] == module["RECEIVER_CUSTODY"]
                and sdk["counterReceipt"] == {"path": "target/recipe-test-counters.json",
                    "bytes": counter_path.stat().st_size, "sha256": digest(counter_path)},
                "scope SDK custody or counters differ")
        for key in module["FALSE_GATES"]:
            require(sdk[key] is False, "scope SDK exceeds source boundary")
        proof_path = regular(folder / "target/scope-java-execution.json")
        java = read_json(proof_path)
        expected = {"schema": "M3_SCOPE_JAVA_EXECUTION_V1", "compiledVariants": 2,
                    "baselineCounterexamples": contract["baselineCounterexamples"], "acceptedScopeCases": 12, "javaRelease": 21,
                    "beforeSha256": module["C_BEFORE_SHA"], "afterSha256": module["C_AFTER_SHA"]}
        require(java == expected and all(type(java[k]) is type(v) for k, v in expected.items())
                and sdk["scopeJavaExecution"] == {"path": "target/scope-java-execution.json",
                    "sha256": digest(proof_path), **expected}, "raw actual JavaCompiler proof and projection required")
        extra = {"scopeJavaExecutionSha256": digest(proof_path), "scopeJavaExecution": java}
    else:
        module["validate_counters"](sdk["counters"], ident, head)
        require(sdk["state"] == "PASS_SOURCE_RECIPE_ONLY" and sdk["counters"] == counters
                and sdk["counterReceiptSha256"] == digest(counter_path)
                and sdk["authenticStrictProviderFixtureQualified"] is True, "authentic strict provider fixture proof required")
        for key in ("originalReceivingMavenModelQualified", "scopeRegistryQualified", "publicTargetApplied",
                    "fullJdkQualified", "runtimeApplied", "phaseAdvanced", "promotionAuthority"):
            require(sdk[key] is False, "manifest SDK exceeds source boundary")
        extra = {}
    require("[INFO] BUILD SUCCESS" in regular(ARTIFACTS / (lane + "-maven.log")).read_text(encoding="utf-8"),
            "actual successful Maven SDK execution required")
    return {"sourceCommit": contract["sourceCommit"], "sourceTree": contract["sourceTree"],
            "inputPinsSha256": digest(folder / "pins.json"), "sdkReceiptSha256": digest(receipt_path),
            "counterReceiptSha256": digest(counter_path), "counters": counters,
            "reports": sdk["reports"], "emissions": emissions, **extra}


def report():
    record = binding()
    first = read_json(ARTIFACTS / "authority-binding-receipt.json")
    require(first["checkoutCommit"] == os.environ["EXPECTED_SOURCE_SHA"]
            and first["sourceBindingSha256"] == digest(HERE / "source-binding.json")
            and first["canonicalSourceCommit"] == record["sourceCommit"], "authority source changed after initial binding")
    result = {"schema": "M3_RECIPE_AUTHORITY_HOSTED_QUALIFICATION_V1",
              "scope": "SOURCE_ONLY_TWO_RECIPE_SDK_CONFORMANCE",
              "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "canonicalSourceCommit": record["sourceCommit"],
              "qualificationId": os.environ["M3_QUALIFICATION_ID"],
              "sourceBindingSha256": digest(HERE / "source-binding.json"),
              "originalQualificationReceiptSha256": digest(ARTIFACTS / "qualification-receipt.json"),
              "sdkClosures": {lane: validate_sdk(lane) for lane in CONTRACTS},
              "artifactMembers": record["artifactMembers"],
              "recipeSdksQualified": True, "sourceOnly": True,
              **{key: False for key in FORBIDDEN}}
    API["write"]("authority-qualification-receipt.json", result)


def artifact_sources():
    """Flat namespace aliases for exact new inputs, reports and bidirectional proof members."""
    result = [(HERE / "source-binding.json", "authority-source-binding.json")]
    for lane, contract in CONTRACTS.items():
        folder = HERE / (lane + "-sdk")
        for relative, alias in (("pins.json", "pins.json"), ("target/sdk-receipt.json", "sdk-receipt.json"),
                                ("target/recipe-test-counters.json", "recipe-test-counters.json")):
            result.append((folder / relative, lane + "-" + alias))
        for suite in contract["suites"]:
            for name in ("TEST-" + suite + ".xml", suite + ".txt"):
                result.append((folder / "target/surefire-reports" / name, lane + "-" + name))
        for index, owner in enumerate(contract["emittedOwners"]):
            for direction in ("emitted", "inverse-emitted"):
                result.append((folder / "target" / direction / owner,
                               lane + "-" + direction + "-" + str(index) + "-" + Path(owner).name))
    result.append((HERE / "scope-sdk/target/scope-java-execution.json", "scope-java-execution.json"))
    require(len(result) == 16, "complete namespaced artifact census changed")
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        record = binding(fresh=True)
        API["write"]("authority-binding-receipt.json", {
            "schema": "M3_RECIPE_AUTHORITY_BINDING_RECEIPT_V1",
            "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "canonicalSourceCommit": record["sourceCommit"],
            "sourceBindingSha256": digest(HERE / "source-binding.json"),
            "sdkClosures": record["sdkClosures"], "allThreeSdkInputsValidatedBeforeMaven": True,
            "sourceOnly": True, "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
