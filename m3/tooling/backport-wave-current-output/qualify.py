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
PREFIX = "m3/tooling/backport-wave-current-output/"
WORKFLOW = ".github/workflows/m3-backport-wave-current-output.yml"
SUITE = "com.synexia.rewrite.M3BackportWaveCurrentOutputRecipeTest"
METHODS = ["bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
           "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
           "exactCurrentWorkflowPinsPreserveHistoricalReceipt"]
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
}
FORBIDDEN = ("originalPrivateModelQualified", "stringDispatchQualified", "fullNativeTargetQualified",
             "windowsQualified", "fullMatchedJdkQualified", "performanceRetentionQualified",
             "productTargetApplied", "phaseAdvanced")


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


TARGET_WORKFLOW = ".github/workflows/m3-jdk21-backport-wave-admission.yml"
SOURCE_GIT = {
    "m3/backports/test_compatibility_policy.py": "cd7b5a28ce3ec1f5a4475a145b40a3074b1a5c21",
    "m3/backports/compatibility_policy.py": "89673d65b2ad42fc694d809d5c6f4d0eab7adbd8",
    "m3/backports/POST21_PRIORITY_COMPATIBILITY.tsv": "db7579c96d7dfc9c0c70fb4b28773cf89bad76ca",
    "m3/backports/JEP_CATALOGUE.tsv": "a0ef9aa95da17a19c3df8ab8aded355fad4f4598",
    "m3/backports/RELEASE_JEP_AUTHORITY.tsv": "745a26772b8050c28dd93d05c724bfa9c2ad5e18",
    "m3/backports/test_backport_execution_waves.py": "7a3b961caa4302a2ebd1675360e29e6b1df9697b",
    "m3/backports/backport_execution_waves.py": "cc2a6ceb90b56d3b05d1307e705586c8d7cf1b40",
    "m3/backports/BACKPORT_EXECUTION_WAVES.md": "428d775ca476b187dceb26a012401b11b975bbbb",
    "m3/compatibility/synexia-wave-admission-pin-20261014.tsv": "60c11d68f482efadeadbbe6f58bfac9ec9aa0090",
    ".github/workflows/m3-jdk21-backport-wave-admission.yml": "9ba94e55b4c8f468fb0ff5f3cd7c530caa6fdff2"
}
AFTER_GIT = {**SOURCE_GIT, TARGET_WORKFLOW: "60ab068a3ebe7bab7c568afc405035f1fd15ffd3"}

def owner_plan(original, post, actual):
    lanes = (original, post, actual)
    if any(not isinstance(lane, dict) or set(lane) != set(SOURCE_GIT) for lane in lanes):
        raise RuntimeError("complete exact owner sets required")
    for name in SOURCE_GIT:
        if any(not isinstance(lane[name], bytes) for lane in lanes):
            raise RuntimeError("owner inputs must be immutable raw bytes")
        if blob(original[name]) != SOURCE_GIT[name] or blob(post[name]) != AFTER_GIT[name]:
            raise RuntimeError("known frozen original and generated postimage required: " + name)
        if name != TARGET_WORKFLOW and original[name] != post[name]:
            raise RuntimeError("nine KEEP owners must remain byte identical")
        allowed = (original[name], post[name]) if name == TARGET_WORKFLOW else (original[name],)
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
    if refused != 66 or len(cases) != 68:
        raise AssertionError("owner plan control inventory drift")
    return {"schema": "M3_WAVE_KNOWN_OWNER_PLAN_CONTROLS_V1", "state": "PASS",
            "tests": 68, "successes": 2, "refused": refused, "cases": cases,
            "scope": "PURE_BYTE_CONSTRUCTOR_NO_SOURCE_WRITES"}


def binding(fresh=False):
    head = git("rev-parse", "HEAD")
    if head != os.environ["EXPECTED_SOURCE_SHA"] or len(head) != 40:
        raise RuntimeError("checkout is not exact candidate head")
    data = regular(HERE / "source-binding.json").read_bytes()
    if blob(data) != git("rev-parse", "HEAD:" + PREFIX + "source-binding.json"):
        raise RuntimeError("source binding differs from exact checkout")
    record = json.loads(data)
    if (record["repository"] != "hsoliwal/com.synexia"
            or record["receiverRepository"] != "hsoliwal/M3jdk21"
            or record["runtimeApplied"] is not False or record["phaseAdvanced"] is not False
            or record["admissionCommit"] != "c095d7dd5c6acc2994cf469f12bfb13cfdbf3737"
            or record["hostAdmissionCommit"] != "d22c2e0fb7ba7e5a39a3088aa1e29b633f46756c"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if len(rows) != 38 or record['sdkFiles'] != 36:
        raise RuntimeError("bounded complete SDK and hosted source binding required")
    seen = set()
    for row in rows:
        path = str(relative(row["targetPath"]))
        if path in seen or not (path.startswith(PREFIX) or path == WORKFLOW):
            raise RuntimeError("source binding ownership drift")
        seen.add(path)
        content = regular(RECEIVER / path).read_bytes()
        if blob(content) != row["gitBlob"] or len(content) != row["bytes"]:
            raise RuntimeError("canonical source input differs: " + path)
    sdk_prefix = PREFIX + "sdk/"
    tracked = set(git("ls-files", "-z", "--", sdk_prefix, binary=True).decode("utf-8").split("\0"))
    tracked.discard("")
    if len(tracked) != record["sdkFiles"] or {p for p in seen if p.startswith(sdk_prefix)} != tracked:
        raise RuntimeError("exact complete SDK binding required")
    if not {PREFIX + "qualify.py", WORKFLOW} <= seen:
        raise RuntimeError("hosted authoring inputs must be source-bound")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--", PREFIX, WORKFLOW],
                   cwd=RECEIVER, check=True, timeout=60)
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_wave_binding_reuse")
    module["inputs"]()
    fixture_root = SDK / module["RESOURCE"]
    original = {name: regular(fixture_root / "before" / name).read_bytes() for name in SOURCE_GIT}
    post = {name: regular(fixture_root / "out" / name).read_bytes() for name in SOURCE_GIT}
    owners = record["receiverOriginalOwners"]
    if len(owners) != 10 or {row["path"] for row in owners} != set(SOURCE_GIT):
        raise RuntimeError("ten unique original receiving owners required")
    for row in owners:
        raw = original[row["path"]]
        if row["gitBlob"] != blob(raw) or row["bytes"] != len(raw) or row["sha256"] != hashlib.sha256(raw).hexdigest():
            raise RuntimeError("original tuple must match sealed recipe fixtures")
    actual = {}
    for name in SOURCE_GIT:
        raw = regular(RECEIVER / str(relative(name))).read_bytes()
        if git("rev-parse", "HEAD:" + name) != blob(raw):
            raise RuntimeError("actual receiving owner differs from exact checkout: " + name)
        actual[name] = raw
    record["observedCurrentOwners"] = owner_plan(original, post, actual)
    record["ownerPlanControls"] = self_test_owner_plan(original, post)

    if fresh and ((SDK / "target").exists() or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver classes and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    first = json.loads(regular(ARTIFACTS / "binding-receipt.json").read_bytes())
    if first["observedCurrentOwners"] != record["observedCurrentOwners"]:
        raise RuntimeError("current source changed between binding and report")
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_wave_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or module["RECEIVER_OUTPUTS"] != {
            TARGET_WORKFLOW: "b64ac1da918bd1b5fd5dd56adc16aeb9705a854cfd262e0793fe79428ac450d6"}:
        raise RuntimeError("exact three-method one-workflow recipe contract required")
    expected_custody = {row["path"]: row["sha256"] for row in record["receiverOriginalOwners"]
                        if row["path"] != TARGET_WORKFLOW}
    if module["RECEIVER_CUSTODY"] != expected_custody or len(expected_custody) != 9:
        raise RuntimeError("all nine immutable owning inputs required")
    sdk_path = regular(SDK / "target/sdk-receipt.json")
    sdk = json.loads(sdk_path.read_bytes())
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1 or reports[0].name != "TEST-" + SUITE + ".xml":
        raise RuntimeError("one fresh complete SDK suite required")
    xml = regular(reports[0]).read_bytes()
    counts = module["validate_report"](xml, SUITE, METHODS)
    ident, head = os.environ["M3_QUALIFICATION_ID"], os.environ["EXPECTED_SOURCE_SHA"]
    counter = regular(SDK / "target/recipe-test-counters.json")
    module["validate_counters"](json.loads(counter.read_bytes()), ident, head)
    module["validate_marker"](xml, ident)
    if (sdk["schema"] != "M3_BACKPORT_WAVE_CURRENT_OUTPUT_SDK_RECEIPT_V1"
            or sdk["scope"] != "SOURCE_ONLY_BACKPORT_WAVE_CURRENT_OUTPUT_CONSUMER"
            or sdk["qualificationId"] != ident or sdk["checkoutCommit"] != head
            or sdk["providers"] != pins["providers"]
            or sdk["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or sdk["reports"][SUITE]["counts"] != counts
            or sdk["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest()
            or sdk["unchangedReceiverCustody"] != expected_custody
            or sdk["counterReceipt"]["sha256"] != sha256(counter)
            or (sdk["tests"],sdk["successfulRuns"],sdk["refusedRuns"],sdk["emittedOutputs"]) != (3,16,60,2)
            or sdk["recipeJUnitQualified"] is not True or sdk["recipeReplayQualified"] is not True):
        raise RuntimeError("actual SDK receipt/counter/source identity differs")
    for flag in module["FALSE_GATES"]:
        if sdk[flag] is not False:
            raise RuntimeError("source-only SDK receipt exceeds its boundary")
    providers = {}
    expected_providers = ("M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                          "M3BackportWaveCurrentOutputRecipe", "M3BackportWaveCurrentOutputInverseRecipe")
    if tuple(module["COMPILED_PROVIDERS"]) != expected_providers:
        raise RuntimeError("exact four Java21 source providers required")
    for name in expected_providers:
        path = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        raw = path.read_bytes()
        item = sdk["compiledProviders"][name]
        if (len(raw) <= 8 or raw[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(raw[6:8], "big") != 65
                or item["bytes"] != len(raw) or item["sha256"] != hashlib.sha256(raw).hexdigest()):
            raise RuntimeError("compiled provider identity drift")
        providers[name] = item
    emissions = {}
    for folder, receipt_key, stage in (("emitted", "emittedProductSourceOutputs", "out"),
                                        ("inverse-emitted", "inverseSourceOutputs", "before")):
        output = SDK / "target" / folder
        paths = list(output.rglob("*")) if output.is_dir() else []
        if (output.is_symlink() or any(path.is_symlink() for path in paths)
                or [path.relative_to(output).as_posix() for path in paths if path.is_file()] != [TARGET_WORKFLOW]
                or set(sdk[receipt_key]) != {TARGET_WORKFLOW}):
            raise RuntimeError("one exact authentic directional workflow emission required")
        raw = regular(output / TARGET_WORKFLOW).read_bytes()
        expected_raw = regular(SDK / module["RESOURCE"] / stage / TARGET_WORKFLOW).read_bytes()
        item = sdk[receipt_key][TARGET_WORKFLOW]
        if (raw != expected_raw or item["bytes"] != len(raw)
                or item["sha256"] != hashlib.sha256(raw).hexdigest() or item["gitBlob"] != blob(raw)):
            raise RuntimeError("actual emitted workflow differs from recipe postimage")
        emissions[folder] = item
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8"):
        raise RuntimeError("actual Maven SDK build success required")
    result = {
        "schema": "M3_BACKPORT_WAVE_CURRENT_OUTPUT_HOSTED_QUALIFICATION_V1",
        "scope": "SOURCE_ONLY_SEALED_WORKFLOW_CONSUMER_RECIPE",
        "checkoutCommit": head, "canonicalSourceCommit": record["sourceCommit"],
        "qualificationId": ident, "sourceBindingSha256": sha256(HERE / "source-binding.json"),
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256": sha256(sdk_path), "surefireXmlSha256": hashlib.sha256(xml).hexdigest(),
        "suite": SUITE, "methods": METHODS, "counts": counts,
        "successfulSdkRuns": 16, "refusedSdkRuns": 60, "emittedProductFiles": 2,
        "compiledProviders": providers, "unchangedReceiverCustody": expected_custody,
        "emissions": emissions, "receiverCurrentOwners": record["observedCurrentOwners"],
        "ownerPlanControls": record["ownerPlanControls"],
        "sdkRecipeJUnitQualified": True, "knownOwnerSourceReplayQualified": True,
        "actualWorkflowState": ("APPLIED" if record["observedCurrentOwners"][TARGET_WORKFLOW]["gitBlob"]
                                == AFTER_GIT[TARGET_WORKFLOW] else "ORIGINAL"),
        "workflowConsumerIntegrationQualified": False, "runtimeApplied": False,
    }
    result.update({key: False for key in FORBIDDEN})
    write("qualification-receipt.json", result)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        record = binding(fresh=True)
        write("binding-receipt.json", {"schema": 1,
              "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"],
              "sourceBindingSha256": sha256(HERE / "source-binding.json"),
              "boundFiles": len(record["files"]), "sdkFiles": record["sdkFiles"],
              "observedCurrentOwners": record["observedCurrentOwners"],
              "ownerPlanControls": record["ownerPlanControls"],
              "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
