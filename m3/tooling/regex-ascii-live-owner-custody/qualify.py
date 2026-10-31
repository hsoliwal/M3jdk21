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
PREFIX = "m3/tooling/regex-ascii-live-owner-custody/"
WORKFLOW = ".github/workflows/m3-regex-ascii-live-owner-custody.yml"
ORIGINAL_SDK = RECEIVER / "m3/tooling/regex-ascii-lexical-receiver/sdk"
ORIGINAL_PREFIX = "m3/tooling/regex-ascii-lexical-receiver/"
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
}
FORBIDDEN = ("originalPrivateModelQualified", "stringDispatchQualified", "jniRuntimeQualified",
             "windowsQualified", "fullMatchedJdkQualified", "performanceRetentionQualified",
             "productTargetApplied", "actualAppliedReceiverComponentQualified", "phaseAdvanced")


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


def identity(raw):
    return {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)}


def binding(fresh=False):
    head = git("rev-parse", "HEAD")
    if head != os.environ["EXPECTED_SOURCE_SHA"] or len(head) != 40:
        raise RuntimeError("checkout is not exact candidate head")
    data = regular(HERE / "source-binding.json").read_bytes()
    if blob(data) != git("rev-parse", "HEAD:" + PREFIX + "source-binding.json"):
        raise RuntimeError("source binding differs from exact checkout")
    record = json.loads(data)
    if (record["schema"] != "M3_LEXICAL_LIVE_OWNER_HELPER_SOURCE_BINDING_V1"
            or record["repository"] != "hsoliwal/com.synexia"
            or record["receiverRepository"] != "hsoliwal/M3jdk21"
            or record["runtimeApplied"] is not False or record["phaseAdvanced"] is not False
            or record["admissionCommit"] != "e2c397bdf9479a7bcae792ce2523b287273b9c45"
            or record["originalSdkTree"] != "089562441f095aef62f25338d1c148475e4e73a5"
            or record["helperSdkTree"] != "e790d6fdbfa18e3acccd9b451854f0412a5199f1"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if len(rows) != 82 or record["sdkFiles"] != 34 or record["originalSdkFiles"] != 46:
        raise RuntimeError("complete helper and unchanged original SDK binding required")
    seen = set()
    for row in rows:
        path = str(relative(row["targetPath"]))
        if path in seen or not (path.startswith(PREFIX) or path.startswith(ORIGINAL_PREFIX + "sdk/") or path == WORKFLOW):
            raise RuntimeError("source binding ownership drift")
        seen.add(path)
        raw = regular(RECEIVER / path).read_bytes()
        if identity(raw) != {key: row[key] for key in ("bytes", "sha256", "gitBlob")}:
            raise RuntimeError("canonical source input differs:" + path)
        if git("rev-parse", "HEAD:" + path) != blob(raw):
            raise RuntimeError("bound source differs from exact checkout:" + path)
    for prefix, count in ((PREFIX + "sdk/", 34), (ORIGINAL_PREFIX + "sdk/", 46)):
        tracked = set(git("ls-files", "-z", "--", prefix, binary=True).decode("utf-8").split("\0")) - {""}
        if len(tracked) != count or {name for name in seen if name.startswith(prefix)} != tracked:
            raise RuntimeError("exact complete SDK binding required")
    if not {PREFIX + "qualify.py", WORKFLOW} <= seen:
        raise RuntimeError("hosted authoring inputs must be bound")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--",
                    PREFIX, ORIGINAL_PREFIX, WORKFLOW], cwd=RECEIVER, check=True, timeout=60)
    helper = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_lexical_helper_sdk_binding")
    helper["inputs"]()
    original = runpy.run_path(str(regular(ORIGINAL_SDK / "verify.py")), run_name="m3_lexical_original_sdk_binding")
    original["inputs"]()
    fixture = SDK / helper["RESOURCE"] / "forward"
    helper_owners = {}
    for name in helper["TARGET_PATHS"]:
        before = regular(fixture / "before" / name).read_bytes()
        post = regular(fixture / "out" / name).read_bytes()
        actual = regular(RECEIVER / str(relative(name))).read_bytes()
        if blob(before) != helper["OWNER_BEFORE_GIT"][name] or blob(post) != helper["OWNER_AFTER_GIT"][name]:
            raise RuntimeError("original and proposed helper seals differ")
        if actual not in (before, post) or git("rev-parse", "HEAD:" + name) != blob(actual):
            raise RuntimeError("unknown helper source drift:" + name)
        helper_owners[name] = identity(actual)
    candidate = runpy.run_path(str(regular(fixture / "out" / (ORIGINAL_PREFIX + "qualify.py"))),
                              run_name="m3_lexical_candidate_owner_constructor_binding")
    product_fixture = ORIGINAL_SDK / original["RESOURCE"] / "forward"
    frozen = {name: regular(product_fixture / "before" / name).read_bytes() for name in candidate["SOURCE_GIT"]}
    post = {name: regular(product_fixture / "out" / name).read_bytes() for name in candidate["SOURCE_GIT"]}
    actual = {}
    for name in candidate["SOURCE_GIT"]:
        raw = regular(RECEIVER / str(relative(name))).read_bytes()
        if git("rev-parse", "HEAD:" + name) != blob(raw):
            raise RuntimeError("live product differs from exact checkout:" + name)
        actual[name] = raw
    record["observedCurrentOwners"] = candidate["owner_plan"](frozen, post, actual)
    record["ownerPlanControls"] = candidate["self_test_owner_plan"](frozen, post)
    record["receiverState"] = candidate["current_owner_state"](record["observedCurrentOwners"])
    record["observedHelperOwners"] = helper_owners
    if fresh and (any((module / "target").exists() for module in (SDK, ORIGINAL_SDK))
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("both SDK outputs and isolated Maven repository must start fresh")
    return record


def checked_sdk(label, root, *, helper):
    module = runpy.run_path(str(regular(root / "verify.py")), run_name="m3_lexical_" + label + "_report")
    pins, pins_raw = module["inputs"]()
    expected_suite = ("com.synexia.rewrite.M3RegexAsciiLiveOwnerCustodyRecipeTest" if helper
                      else "com.synexia.rewrite.M3RegexAsciiLexicalReceiverRecipeTest")
    expected_methods = (["allEightPartialStatesBothOrdersReachExactFixedPoint",
                         "allEightInverseStatesBothOrdersRecoverExactBefore",
                         "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"] if helper else
                        ["all8PartialStatesBothOrdersReachExactFixedPoint",
                         "all8InverseStatesBothOrdersRecoverExactBefore",
                         "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"])
    if module["SUITES"] != {expected_suite: expected_methods}:
        raise RuntimeError("exact named SDK method inventory required")
    xml_paths = sorted((root / "target/surefire-reports").glob("TEST-*.xml"))
    if len(xml_paths) != 1 or xml_paths[0].name != "TEST-" + expected_suite + ".xml":
        raise RuntimeError("one actual fresh suite required per SDK")
    xml = regular(xml_paths[0]).read_bytes()
    counts = module["validate_report"](xml, expected_suite, expected_methods)
    ident, head = os.environ["M3_QUALIFICATION_ID"], os.environ["EXPECTED_SOURCE_SHA"]
    counter_path = regular(root / ("target/authoring-recipe-test-counters.json" if helper else "target/recipe-test-counters.json"))
    counter = json.loads(counter_path.read_bytes())
    module["validate_counters"](counter, ident, head)
    module["validate_marker"](xml, ident)
    receipt_path = regular(root / ("target/authoring-sdk-receipt.json" if helper else "target/sdk-receipt.json"))
    receipt = json.loads(receipt_path.read_bytes())
    expected_schema = ("M3_REGEX_ASCII_LIVE_OWNER_CUSTODY_SDK_RECEIPT_V1" if helper
                       else "M3_REGEX_ASCII_LEXICAL_RECEIVER_SDK_RECEIPT_V1")
    expected_scope = ("SOURCE_ONLY_LEXICAL_LIVE_OWNER_CUSTODY_REPLAY" if helper
                      else "SOURCE_ONLY_REGEX_ASCII_LEXICAL_RECEIVER_RECIPE_REPLAY")
    if (receipt["schema"] != expected_schema or receipt["scope"] != expected_scope
            or receipt["qualificationId"] != ident or receipt["checkoutCommit"] != head
            or receipt["providers"] != pins["providers"] or receipt["sourceOnly"] is not True
            or receipt["inputPinsSha256"] != hashlib.sha256(pins_raw).hexdigest()
            or receipt["reports"][expected_suite] != {"counts": counts, "sha256": hashlib.sha256(xml).hexdigest()}
            or receipt["counterReceipt"]["sha256"] != sha256(counter_path)
            or receipt["counterReceipt"]["bytes"] != counter_path.stat().st_size
            or receipt["recipeJUnitQualified"] is not True or receipt["recipeReplayQualified"] is not True):
        raise RuntimeError("actual SDK report/counter identity differs")
    for flag in module["FALSE_GATES"]:
        if receipt[flag] is not False:
            raise RuntimeError("source-only SDK scope drift")
    if helper:
        if (receipt["tests"], receipt["successfulRuns"], receipt["refusedRuns"], receipt["emittedOutputs"]) != (3,64,30,3):
            raise RuntimeError("helper schedule inventory drift")
        expected_classes = ["M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                            "M3RegexAsciiLiveOwnerCustodyRecipe", "M3RegexAsciiLiveOwnerCustodyInverseRecipe"]
        emitted_root = root / "target/emitted"
    else:
        if (receipt["tests"], receipt["forwardApplies"], receipt["inverseApplies"], receipt["refusedRuns"]) != (3,32,32,48):
            raise RuntimeError("original schedule inventory drift")
        expected_classes = ["M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                            "M3RegexAsciiLexicalReceiverRecipe", "M3RegexAsciiLexicalReceiverInverseRecipe"]
        emitted_root = root / "target/emitted/regex-ascii-lexical-receiver-20261019"
    compiled = {}
    for name in expected_classes:
        raw = regular(root / ("target/classes/com/synexia/rewrite/" + name + ".class")).read_bytes()
        if len(raw) < 9 or raw[:4] != bytes.fromhex("cafebabe") or int.from_bytes(raw[6:8], "big") != 65:
            raise RuntimeError("actual Java21 provider classes required")
        compiled[name] = identity(raw)
        if helper:
            sealed = receipt["compiledProviders"][name]
            if (sealed["bytes"], sealed["sha256"], sealed["majorVersion"]) != (len(raw), compiled[name]["sha256"], 65):
                raise RuntimeError("helper provider class identity drift")
    paths = list(emitted_root.rglob("*")) if emitted_root.is_dir() else []
    outputs = module["RECEIVER_OUTPUTS"]
    if (emitted_root.is_symlink() or any(path.is_symlink() for path in paths)
            or {path.relative_to(emitted_root).as_posix() for path in paths if path.is_file()} != set(outputs)
            or receipt["emittedProductSourceOutputs"] != outputs):
        raise RuntimeError("exact authentic Maven emission set required")
    emissions = {}
    for key, expected_sha in outputs.items():
        raw = regular(emitted_root / str(relative(key))).read_bytes()
        if helper:
            counterpart = root / module["RESOURCE"] / "forward/out" / key
        else:
            direction, name = key.split("/", 1)
            counterpart = root / module["RESOURCE"] / direction / "out" / name
        if raw != regular(counterpart).read_bytes() or hashlib.sha256(raw).hexdigest() != expected_sha:
            raise RuntimeError("actual emitted body differs from recipe:" + key)
        emissions[key] = identity(raw)
    if len(emissions) != (3 if helper else 6):
        raise RuntimeError("three helper and six original emissions required")
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / (label + "-maven.log")).read_text(encoding="utf-8"):
        raise RuntimeError("both actual Maven SDK build successes required")
    return {"receiptSha256": sha256(receipt_path), "counterSha256": sha256(counter_path),
            "surefireXmlSha256": hashlib.sha256(xml).hexdigest(), "suite": expected_suite,
            "methods": expected_methods, "counts": counts, "emissions": emissions,
            "compiledProviders": compiled}


def report():
    record = binding()
    first = json.loads(regular(ARTIFACTS / "binding-receipt.json").read_bytes())
    for key in ("observedCurrentOwners", "observedHelperOwners", "ownerPlanControls"):
        if first[key] != record[key]:
            raise RuntimeError("owner state changed during qualification:" + key)
    original = checked_sdk("original-sdk", ORIGINAL_SDK, helper=False)
    helper = checked_sdk("sdk", SDK, helper=True)
    result = {"schema": "M3_LEXICAL_LIVE_OWNER_HELPER_HOSTED_QUALIFICATION_V1",
              "scope": "SOURCE_ONLY_ORIGINAL_AND_HELPER_MAVEN_REPLAY",
              "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"],
              "qualificationId": os.environ["M3_QUALIFICATION_ID"],
              "sourceBindingSha256": sha256(HERE / "source-binding.json"),
              "originalSdk": original, "helperSdk": helper,
              "originalSdkSuccessfulRuns": 64, "originalSdkRefusedRuns": 48,
              "helperSdkSuccessfulRuns": 64, "helperSdkRefusedRuns": 30,
              "receiverState": record["receiverState"], "receiverCurrentOwners": record["observedCurrentOwners"],
              "helperCurrentOwners": record["observedHelperOwners"], "ownerPlanControls": record["ownerPlanControls"],
              "sdkRecipeJUnitQualified": True, "knownOwnerSourceReplayQualified": True,
              "runtimeApplied": False, **{key: False for key in FORBIDDEN}}
    write("qualification-receipt.json", result)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    if parser.parse_args().operation == "binding":
        record = binding(fresh=True)
        write("binding-receipt.json", {"schema": 1, "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"],
              "sourceBindingSha256": sha256(HERE / "source-binding.json"),
              "boundFiles": len(record["files"]), "sdkFiles": 34, "originalSdkFiles": 46,
              "observedCurrentOwners": record["observedCurrentOwners"],
              "observedHelperOwners": record["observedHelperOwners"],
              "ownerPlanControls": record["ownerPlanControls"], "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
