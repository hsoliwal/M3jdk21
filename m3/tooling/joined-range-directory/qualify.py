#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the sealed current-output recipe; source replay never grants product promotion."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import runpy
import subprocess

HERE = Path(__file__).resolve().parent
RECEIVER = HERE.parents[2]
SDK = HERE / "sdk"
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ARTIFACTS = ROOT / "artifacts"
PREFIX = "m3/tooling/joined-range-directory/"
WORKFLOW = ".github/workflows/m3-joined-range-directory.yml"
SUITE = "com.synexia.rewrite.M3JoinedRangeDirectoryRecipeTest"
METHODS = ["allTwoPartialStatesBothOrdersReachExactFixedPoint",
           "allTwoInverseStatesBothOrdersRecoverExactBefore",
           "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
}
FORBIDDEN = ("originalPrivateModelQualified", "stringDispatchQualified", "fullNativeTargetQualified",
             "windowsQualified", "fullMatchedJdkQualified", "performanceRetentionQualified", "phaseAdvanced")


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def regular(path):
    if not path.is_file() or any(item.is_symlink() for item in [path, *path.parents]):
        raise RuntimeError("missing or redirected qualification input: " + str(path))
    return path


def sha256(path):
    return hashlib.sha256(regular(path).read_bytes()).hexdigest()


def relative(value):
    path = PurePosixPath(value)
    if (not value or path.is_absolute() or "\\" in value or ":" in value or "\0" in value
            or any(part in ("", ".", "..") for part in value.split("/"))):
        raise RuntimeError("unsafe qualification source path")
    return path


def git(*arguments, binary=False):
    return subprocess.check_output(["git", *arguments], cwd=RECEIVER,
                                   timeout=60, text=not binary).strip()


def write(name, value):
    if (not ARTIFACTS.is_dir()
            or any(item.is_symlink() for item in [ARTIFACTS, *ARTIFACTS.parents])):
        raise RuntimeError("missing or redirected qualification artifact directory")
    destination = ARTIFACTS / name
    if destination.exists() or destination.is_symlink():
        raise RuntimeError("qualification receipt must start absent")
    destination.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


TARGET_PROVIDER = "m3/core/src/com/m3/text/JoinedM3StringPiece.java"
SOURCE_GIT = {
    "m3/core/src/com/m3/text/JoinedM3StringPiece.java": "cc3c84f5ea0230e2f57e3d3ae0f5d29b9241e597",
    "m3/core/src/com/m3/text/M3StringPiece.java": "99cb72623fa6bc2d5426553e04aaf0e5bfa993cc",
    "m3/core/src/com/m3/text/LocalM3StringPiece.java": "6b745c9472144ad6de01de4d06a533974e558fb1",
    "m3/core/src/com/m3/text/LocalM3Arena.java": "d82710cd1a49b54d92afcd74cdd903cd009766ba",
    "m3/core/src/com/m3/text/StorageIdentity.java": "7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a"
}
AFTER_GIT = {
    "m3/core/src/com/m3/text/JoinedM3StringPiece.java": "3edc23c22362b08af00f7332ddc445a0274a6dd4",
    "m3/core/src/com/m3/text/M3StringPiece.java": "99cb72623fa6bc2d5426553e04aaf0e5bfa993cc",
    "m3/core/src/com/m3/text/LocalM3StringPiece.java": "6b745c9472144ad6de01de4d06a533974e558fb1",
    "m3/core/src/com/m3/text/LocalM3Arena.java": "d82710cd1a49b54d92afcd74cdd903cd009766ba",
    "m3/core/src/com/m3/text/StorageIdentity.java": "7f0a0bb63d38a0a6efbe08f37249dd3e258eb35a"
}

def owner_plan(original, post, actual):
    lanes = (original, post, actual)
    if any(not isinstance(lane, dict) or set(lane) != set(SOURCE_GIT) for lane in lanes):
        raise RuntimeError("complete exact owner sets required")
    for name in SOURCE_GIT:
        if any(not isinstance(lane[name], bytes) for lane in lanes):
            raise RuntimeError("owner inputs must be immutable raw bytes")
        if blob(original[name]) != SOURCE_GIT[name] or blob(post[name]) != AFTER_GIT[name]:
            raise RuntimeError("known frozen original and generated postimage required: " + name)
        if name != TARGET_PROVIDER and original[name] != post[name]:
            raise RuntimeError("four KEEP owners must remain byte identical")
        allowed = (original[name], post[name]) if name == TARGET_PROVIDER else (original[name],)
        if actual[name] not in allowed:
            raise RuntimeError("unknown current owner drift: " + name)
    return {name: {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)}
            for name, raw in actual.items()}


def self_test_owner_plan(original, post):
    cases = ["known-original-current", "known-generated-postimage-current"]
    owner_plan(original, post, original)
    owner_plan(original, post, post)
    refused = 0
    def reject(lanes, label):
        nonlocal refused
        try:
            owner_plan(*lanes)
        except RuntimeError:
            refused += 1
            cases.append(label)
        else:
            raise AssertionError("unknown owner plan admitted: " + label)
    for index, label in enumerate(("frozen-original", "frozen-postimage", "actual-current")):
        for name in SOURCE_GIT:
            lanes = [dict(original), dict(post), dict(original)]
            lanes[index][name] += b"\nunknown drift\n"
            reject(lanes, label + "-drift:" + name)
            lanes = [dict(original), dict(post), dict(original)]
            del lanes[index][name]
            reject(lanes, label + "-missing:" + name)
        lanes = [dict(original), dict(post), dict(original)]
        lanes[index]["unexpected-owner"] = b"unrelated"
        reject(lanes, label + "-extra")
        lanes = [dict(original), dict(post), dict(original)]
        lanes[index] = "not-an-owner-map"
        reject(lanes, label + "-wrong-type")
    if refused != 36 or len(cases) != 38:
        raise AssertionError("owner plan control inventory drift")
    return {"schema": "M3_JOINED_RANGE_KNOWN_OWNER_PLAN_CONTROLS_V1", "state": "PASS",
            "tests": 38, "successes": 2, "refused": refused, "cases": cases,
            "scope": "PURE_BYTE_CONSTRUCTOR_NO_SOURCE_WRITES"}


def binding(fresh=False):
    head = git("rev-parse", "HEAD")
    if head != os.environ["EXPECTED_SOURCE_SHA"] or len(head) != 40:
        raise RuntimeError("checkout is not exact candidate head")
    raw = regular(HERE / "source-binding.json").read_bytes()
    if blob(raw) != git("rev-parse", "HEAD:" + PREFIX + "source-binding.json"):
        raise RuntimeError("source binding differs from checkout")
    record = json.loads(raw)
    if (record["schema"] != "M3_JOINED_RANGE_DIRECTORY_SOURCE_BINDING_V1"
            or record["repository"] != "hsoliwal/com.synexia"
            or record["receiverRepository"] != "hsoliwal/M3jdk21"
            or record["admissionCommit"] != "ec4309cde9a5311347aab535b1cee4a2be442742"
            or record["runtimeApplied"] is not False or record["phaseAdvanced"] is not False
            or record["sdkFiles"] != 34 or len(record["files"]) != 39):
        raise RuntimeError("complete source binding identity/scope required")
    if len(record["sourceCommit"]) != 40 or any(c not in "0123456789abcdef" for c in record["sourceCommit"]):
        raise RuntimeError("canonical source commit required")
    seen = set()
    for row in record["files"]:
        path = str(relative(row["targetPath"]))
        if path in seen or not (path.startswith(PREFIX) or path == WORKFLOW):
            raise RuntimeError("bound input ownership differs")
        seen.add(path)
        body = regular(RECEIVER / path).read_bytes()
        if (blob(body) != row["gitBlob"] or len(body) != row["bytes"]
                or hashlib.sha256(body).hexdigest() != row["sha256"]
                or git("rev-parse", "HEAD:" + path) != blob(body)):
            raise RuntimeError("canonical source bytes differ:" + path)
    tracked = set(git("ls-files", "-z", "--", PREFIX + "sdk/", binary=True).decode("utf-8").split("\0")) - {""}
    if len(tracked) != 34 or tracked != {name for name in seen if name.startswith(PREFIX + "sdk/")}:
        raise RuntimeError("complete SDK source closure required")
    required = {PREFIX + "qualify.py", WORKFLOW, PREFIX + "runtime/qualify.py",
                PREFIX + "runtime/M3JoinedRangeProof.java", PREFIX + "runtime/BUILD_INPUTS.json"}
    if not required <= seen:
        raise RuntimeError("all authoring/runtime inputs must be source-bound")
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_joined_directory_source_binding")
    module["inputs"]()
    fixture = SDK / module["RESOURCE"] / "forward"
    original = {name: regular(fixture / "before" / name).read_bytes() for name in SOURCE_GIT}
    post = {name: regular(fixture / "out" / name).read_bytes() for name in SOURCE_GIT}
    actual = {name: regular(RECEIVER / str(relative(name))).read_bytes() for name in SOURCE_GIT}
    for name, body in actual.items():
        if git("rev-parse", "HEAD:" + name) != blob(body):
            raise RuntimeError("receiving owner differs from checkout:" + name)
    owners = record["receiverOriginalOwners"]
    if len(owners) != 5 or {row["path"] for row in owners} != set(SOURCE_GIT):
        raise RuntimeError("five unique original receiving owners required")
    for row in owners:
        body = original[row["path"]]
        if (row["gitBlob"], row["sha256"], row["bytes"]) != (
                blob(body), hashlib.sha256(body).hexdigest(), len(body)):
            raise RuntimeError("original owner metadata differs from frozen recipe input")
    record["observedCurrentOwners"] = owner_plan(original, post, actual)
    record["ownerPlanControls"] = self_test_owner_plan(original, post)
    metadata = json.loads(regular(HERE / "runtime/BUILD_INPUTS.json").read_bytes())
    rows = metadata["files"]
    if (metadata["schema"] != "M3_JOINED_RANGE_ORIGINAL_BUILD_INPUTS_V1"
            or metadata["commit"] != "73f59218dc4ef1a3ef79aad990e77273db2a2d60"
            or len(rows) != 58 or len({r["path"] for r in rows}) != 58):
        raise RuntimeError("complete original modular build source census required")
    observed = {}
    for row in rows:
        name = str(relative(row["path"]))
        body = regular(RECEIVER / name).read_bytes()
        item = {"bytes": len(body), "sha256": hashlib.sha256(body).hexdigest(), "gitBlob": blob(body)}
        if name != TARGET_PROVIDER and item != {key: row[key] for key in item}:
            raise RuntimeError("original build input drift:" + name)
        if name == TARGET_PROVIDER and body not in (original[name], post[name]):
            raise RuntimeError("unknown joined owner state")
        if git("rev-parse", "HEAD:" + name) != blob(body):
            raise RuntimeError("original build input differs from checkout:" + name)
        observed[name] = item
    for prefix, count in (("m3/core/src/", 27), ("m3/core/test/", 25)):
        tracked = set(git("ls-files", "-z", "--", prefix, binary=True).decode("utf-8").split("\0")) - {""}
        if len(tracked) != count or tracked != {name for name in observed if name.startswith(prefix)}:
            raise RuntimeError("complete original core source/test census required")
    record["observedBuildInputs"] = observed
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--",
                    PREFIX, WORKFLOW, *observed], cwd=RECEIVER, check=True, timeout=60)
    if fresh and ((SDK / "target").exists() or (ROOT / "joined-range-runtime").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK/runtime output and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    first = json.loads(regular(ARTIFACTS / "binding-receipt.json").read_bytes())
    for key in ("observedCurrentOwners", "observedBuildInputs", "ownerPlanControls"):
        if first[key] != record[key]:
            raise RuntimeError("source changed during execution:" + key)
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_joined_directory_sdk_report")
    pins, pins_raw = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS}:
        raise RuntimeError("exact named source recipe tests required")
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1 or reports[0].name != "TEST-" + SUITE + ".xml":
        raise RuntimeError("one actual complete source recipe suite required")
    xml = regular(reports[0]).read_bytes()
    counts = module["validate_report"](xml, SUITE, METHODS)
    ident, head = os.environ["M3_QUALIFICATION_ID"], os.environ["EXPECTED_SOURCE_SHA"]
    counter = regular(SDK / "target/authoring-recipe-test-counters.json")
    module["validate_counters"](json.loads(counter.read_bytes()), ident, head)
    module["validate_marker"](xml, ident)
    sdk_path = regular(SDK / "target/authoring-sdk-receipt.json")
    sdk = json.loads(sdk_path.read_bytes())
    custody = {row["path"]: row["sha256"] for row in record["receiverOriginalOwners"]
               if row["path"] != TARGET_PROVIDER}
    if (sdk["schema"] != "M3_JOINED_RANGE_DIRECTORY_SDK_RECEIPT_V1"
            or sdk["scope"] != "SOURCE_ONLY_JOINED_RANGE_DIRECTORY_REPLAY"
            or sdk["qualificationId"] != ident or sdk["checkoutCommit"] != head
            or sdk["inputPinsSha256"] != hashlib.sha256(pins_raw).hexdigest()
            or sdk["providers"] != pins["providers"] or sdk["unchangedReceiverCustody"] != custody
            or sdk["reports"][SUITE] != {"counts": counts, "sha256": hashlib.sha256(xml).hexdigest()}
            or sdk["counterReceipt"]["sha256"] != sha256(counter)
            or (sdk["tests"], sdk["successfulRuns"], sdk["refusedRuns"], sdk["emittedOutputs"]) != (3,16,30,2)
            or sdk["recipeJUnitQualified"] is not True or sdk["recipeReplayQualified"] is not True):
        raise RuntimeError("actual source recipe evidence differs")
    for flag in module["FALSE_GATES"]:
        if sdk[flag] is not False:
            raise RuntimeError("source recipe boundary differs")
    providers = ("M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                 "M3JoinedRangeDirectoryRecipe", "M3JoinedRangeDirectoryInverseRecipe")
    if tuple(module["COMPILED_PROVIDERS"]) != providers or set(sdk["compiledProviders"]) != set(providers):
        raise RuntimeError("four exact compiled source providers required")
    for name in providers:
        body = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class")).read_bytes()
        item = sdk["compiledProviders"][name]
        if (len(body) < 9 or body[:4] != bytes.fromhex("cafebabe")
                or int.from_bytes(body[6:8], "big") != 65
                or (item["bytes"], item["sha256"], item["majorVersion"]) != (
                    len(body), hashlib.sha256(body).hexdigest(), 65)):
            raise RuntimeError("actual compiled provider identity differs")
    if sdk["emittedProductSourceOutputs"] != module["RECEIVER_OUTPUTS"]:
        raise RuntimeError("exact directional emission receipt required")
    emitted = {}
    for direction, owner_map in (("forward", AFTER_GIT), ("inverse", SOURCE_GIT)):
        path = regular(SDK / "target/emitted" / direction / TARGET_PROVIDER)
        raw = path.read_bytes()
        if blob(raw) != owner_map[TARGET_PROVIDER] or raw != regular(
                SDK / module["RESOURCE"] / direction / "out" / TARGET_PROVIDER).read_bytes():
            raise RuntimeError("actual directional recipe output differs")
        emitted[direction] = {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)}
    runtime_root = ROOT / "joined-range-runtime"
    runtime_path = regular(runtime_root / "runtime-receipt.json")
    runtime = json.loads(runtime_path.read_bytes())
    if (runtime["schema"] != "M3_JOINED_RANGE_RUNTIME_RECEIPT_V1" or runtime["state"] != "PASS"
            or runtime["checkoutCommit"] != head or runtime["qualificationId"] != ident
            or runtime["targetBefore"] != emitted["inverse"] or runtime["targetAfter"] != emitted["forward"]
            or runtime["receiverOwner"] != record["observedCurrentOwners"][TARGET_PROVIDER]
            or runtime["coreModuleOriginalBuildQualified"] is not True
            or runtime["originalTestProgramsPerCohort"] != 16 or runtime["originalJvmModesPerCohort"] != 4
            or runtime["rangeCorpusQualified"] is not True or runtime["pairedAllocationTimingMeasured"] is not True
            or len(runtime["samples"]) != 150 or runtime["failures"]
            or runtime["changedCompiledClasses"] != ["com/m3/text/JoinedM3StringPiece.class"]):
        raise RuntimeError("actual complete core/range/workload evidence required")
    for flag in ("targetMutationByQualificationJob", "nativeCodeChanged", "fullMatchedJdkQualified",
                 "stringDispatchQualified", "performanceRetentionQualified", "phaseAdvanced"):
        if runtime[flag] is not False:
            raise RuntimeError("runtime scope exceeds measured component")
    if len(runtime["commands"]) != 22:
        raise RuntimeError("two original builds/two proof compiles/eight corpus/ten paired process runs required")
    for command in runtime["commands"]:
        if command["returncode"] != 0 or command["parentReaped"] is not True:
            raise RuntimeError("nonterminal inner command")
        raw = regular(runtime_root / str(relative(command["log"]))).read_bytes()
        expected = command["logIdentity"]
        if (len(raw), hashlib.sha256(raw).hexdigest(), blob(raw)) != (
                expected["bytes"], expected["sha256"], expected["gitBlob"]):
            raise RuntimeError("inner command raw log differs")
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8"):
        raise RuntimeError("actual source Maven success required")
    applied = record["observedCurrentOwners"][TARGET_PROVIDER]["gitBlob"] == AFTER_GIT[TARGET_PROVIDER]
    if runtime["physicalReceiverPostimageQualified"] != applied:
        raise RuntimeError("physical applied-state distinction differs")
    result = {"schema": "M3_JOINED_RANGE_DIRECTORY_HOSTED_QUALIFICATION_V1",
              "scope": "EXACT_RECIPE_OUTPUT_CORE_BUILD_RANGE_CONTRACT_AND_ALLOCATION_WORKLOAD",
              "checkoutCommit": head, "canonicalSourceCommit": record["sourceCommit"],
              "qualificationId": ident, "sourceBindingSha256": sha256(HERE / "source-binding.json"),
              "sdkReceiptSha256": sha256(sdk_path), "runtimeReceiptSha256": sha256(runtime_path),
              "suite": SUITE, "methods": METHODS, "counts": counts,
              "successfulSdkRuns": 16, "refusedSdkRuns": 30, "authenticDirectionalEmissions": emitted,
              "ownerPlanControls": record["ownerPlanControls"],
              "receiverCurrentOwners": record["observedCurrentOwners"],
              "originalBuildInputs": record["observedBuildInputs"],
              "sourceRecipeQualified": True, "originalCoreBuildQualifiedOnBothCohorts": True,
              "rangeCorpusQualified": True, "pairedAllocationTimingMeasured": True,
              "allocationSampleCount": 150, "physicalTargetState": "APPLIED" if applied else "ORIGINAL",
              "actualAppliedReceiverQualified": applied,
              "targetMutationByQualificationJob": False, **{key: False for key in FORBIDDEN}}
    write("qualification-receipt.json", result)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    if parser.parse_args().operation == "binding":
        record = binding(fresh=True)
        write("binding-receipt.json", {"schema": 1, "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"],
              "sourceBindingSha256": sha256(HERE / "source-binding.json"),
              "boundFiles": len(record["files"]), "sdkFiles": 34,
              "observedCurrentOwners": record["observedCurrentOwners"],
              "observedBuildInputs": record["observedBuildInputs"], "ownerPlanControls": record["ownerPlanControls"],
              "targetMutationByQualificationJob": False, "phaseAdvanced": False})
    else:
        report()
