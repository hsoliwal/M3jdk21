#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind a private sealed recipe and its authentic receiver differential; never apply targets."""
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
PREFIX = "m3/tooling/backport-contract-current-owner/"
WORKFLOW = ".github/workflows/m3-backport-contract-current-owner.yml"
SUITE = "com.synexia.rewrite.M3BackportContractCurrentOwnerRecipeTest"
METHODS = ["allEightPartialStatesBothOrdersReachExactFixedPoint","allEightInverseStatesBothOrdersRecoverExactBefore","eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
}
FORBIDDEN = ("originalPrivateModelQualified", "stringDispatchQualified", "fullNativeTargetQualified",
             "windowsQualified", "fullMatchedJdkQualified", "performanceRetentionQualified",
             "productTargetApplied", "phaseAdvanced", "fullControlReactorQualified")


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
            or record["admissionCommit"] != "ab8633cc645487bc69bcd6919f328a0e1b5e40a9"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if not 28 <= len(rows) <= 60:
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
    owners = record["receiverOriginalOwners"]
    if len(owners) != 8 or len({row["path"] for row in owners}) != 8:
        raise RuntimeError("eight authentic receiving owners required")
    for row in owners:
        path = str(relative(row["path"]))
        data = regular(RECEIVER / path).read_bytes()
        if (blob(data) != row["gitBlob"] or len(data) != row["bytes"]
                or hashlib.sha256(data).hexdigest() != row["sha256"]
                or git("rev-parse", "HEAD:" + path) != row["gitBlob"]):
            raise RuntimeError("actual receiving owner differs from custody: " + path)
    if fresh and ((SDK / "target").exists() or (ROOT / "backport-contract-python").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver fixtures and isolated Maven repository must start fresh")
    return record



def report():
    record = binding()
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_backport_contract_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or len(module["RECEIVER_OUTPUTS"]) != 3:
        raise RuntimeError("exact three-method eight-owner recipe contract required")
    expected_custody = {row["path"]: row["sha256"] for row in record["receiverOriginalOwners"]}
    expected_outputs = dict(module["RECEIVER_OUTPUTS"])
    if module["RECEIVER_CUSTODY"] != expected_custody:
        raise RuntimeError("SDK custody does not match all eight receiving owners")
    sdk_path = regular(SDK / "target/sdk-receipt.json")
    sdk = json.loads(sdk_path.read_bytes())
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1 or reports[0].name != "TEST-" + SUITE + ".xml":
        raise RuntimeError("one fresh actual SDK suite required")
    xml = regular(reports[0]).read_bytes()
    counts = module["validate_report"](xml, SUITE, METHODS)
    ident, head = os.environ["M3_QUALIFICATION_ID"], os.environ["EXPECTED_SOURCE_SHA"]
    counter_path = regular(SDK / "target/recipe-test-counters.json")
    module["validate_counters"](json.loads(counter_path.read_bytes()), ident, head)
    module["validate_marker"](xml, ident)
    if (sdk["schema"] != "M3_BACKPORT_CONTRACT_CURRENT_OWNER_SDK_RECEIPT_V1"
            or sdk["scope"] != "SOURCE_ONLY_BACKPORT_CONTRACT_CURRENT_OWNER_REPLAY"
            or sdk["qualificationId"] != ident or sdk["checkoutCommit"] != head
            or sdk["providers"] != pins["providers"]
            or sdk["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or sdk["reports"][SUITE]["counts"] != counts
            or sdk["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest()
            or sdk["unchangedReceiverCustody"] != expected_custody
            or sdk["emittedProductSourceOutputs"] != expected_outputs
            or sdk["counterReceipt"]["sha256"] != sha256(counter_path)
            or (sdk["tests"],sdk["successfulRuns"],sdk["refusedRuns"],sdk["emittedOutputs"]) != (3,64,48,3)
            or sdk["recipeJUnitQualified"] is not True or sdk["recipeReplayQualified"] is not True):
        raise RuntimeError("actual SDK receipt/counter/source identity differs")
    for flag in module["FALSE_GATES"]:
        if sdk[flag] is not False:
            raise RuntimeError("source-only SDK receipt exceeds its boundary")
    providers = {}
    for name in ("M3GpuToolJoinRecipe","M3GpuToolJoinManifest",
                 "M3BackportContractCurrentOwnerRecipe","M3BackportContractCurrentOwnerInverseRecipe"):
        path = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        data = path.read_bytes()
        if data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != 65:
            raise RuntimeError("real major65 SDK provider required")
        item = sdk["compiledProviders"][name]
        if item["bytes"] != len(data) or item["sha256"] != hashlib.sha256(data).hexdigest():
            raise RuntimeError("SDK compiled provider changed")
        providers[name] = item
    emitted = SDK / "target/emitted"
    paths = list(emitted.rglob("*"))
    if emitted.is_symlink() or any(path.is_symlink() for path in paths):
        raise RuntimeError("redirected recipe outputs")
    if {path.relative_to(emitted).as_posix() for path in paths if path.is_file()} != set(expected_outputs):
        raise RuntimeError("exactly three actual emitted files required")
    output_rows = {}
    for name, digest in expected_outputs.items():
        path = regular(emitted / str(relative(name)))
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != digest:
            raise RuntimeError("actual emitted product bytes differ: " + name)
        output_rows[name] = {"path":"target/emitted/" + name, "bytes":len(data),
                             "sha256":digest, "gitBlob":blob(data)}
    driver = runpy.run_path(str(regular(HERE / "runtime/qualify.py")), run_name="m3_backport_contract_runtime_reuse")
    metadata, actual_sources, input_hash = driver["inputs"](RECEIVER)
    if set(actual_sources) != set(expected_custody):
        raise RuntimeError("runtime input closure must be exactly eight real owners")
    execution_path = regular(ARTIFACTS / "backport-contract-execution.json")
    execution = json.loads(execution_path.read_bytes())
    if (execution["schema"] != "M3_BACKPORT_CONTRACT_CURRENT_OWNER_EXECUTION_V1"
            or execution["scope"] != "EXACT_EIGHT_OWNER_BACKPORT_CONTRACT_DIFFERENTIAL"
            or execution["state"] != "PASS" or execution["qualificationId"] != ident
            or execution["receiverHead"] != head
            or execution["inputManifestSha256"] != input_hash
            or execution["sourceBase"] != "9a164c31b38c7013c179a3ff38b0bb60c199c7fd"
            or (execution["baselineTests"],execution["candidateTests"],execution["syntheticValidatorTests"]) != (21,21,3)):
        raise RuntimeError("current backport proof identity or scope differs")
    for key in ("baselineFailuresReproduced","candidateAllOwningTestsExecuted",
                "childApprovalReversionRefused","syntheticClassifierControlsPassed","sourceOwnersUnchanged"):
        if execution[key] is not True:
            raise RuntimeError("complete baseline/candidate/refusal proof required")
    driver["validate_execution"](execution, ROOT, RECEIVER)
    custody_ref = execution["sourceCustody"]
    custody_path = regular(ROOT / str(relative(custody_ref["path"])))
    custody_raw = custody_path.read_bytes()
    if (len(custody_raw) != custody_ref["bytes"]
            or hashlib.sha256(custody_raw).hexdigest() != custody_ref["sha256"]):
        raise RuntimeError("original and isolated receiver custody receipt differs")
    labels = ["backport-baseline","backport-candidate",
              "backport-child-approval-refusal","backport-synthetic-validator"]
    commands = execution["commands"]
    if [row["label"] for row in commands] != labels:
        raise RuntimeError("all four actual Python command records required")
    for row, expected_exit in zip(commands, (1,0,1,0), strict=True):
        raw = regular(ROOT / str(relative(row["log"]))).read_bytes()
        if (len(raw) != row["bytes"] or hashlib.sha256(raw).hexdigest() != row["sha256"]
                or row["exitCode"] != expected_exit
                or row["argv"][1:4] != ["-I","-S","-B"]):
            raise RuntimeError("actual Python command exit/log/isolation differs")
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8"):
        raise RuntimeError("actual Maven SDK build success required")
    result = {
        "schema":"M3_BACKPORT_CONTRACT_CURRENT_OWNER_HOSTED_QUALIFICATION_V1",
        "checkoutCommit":head, "canonicalSourceCommit":record["sourceCommit"],
        "qualificationId":ident, "sourceBindingSha256":sha256(HERE / "source-binding.json"),
        "inputPinsSha256":hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256":sha256(sdk_path), "surefireXmlSha256":hashlib.sha256(xml).hexdigest(),
        "suite":SUITE, "methods":METHODS, "counts":counts,
        "successfulSdkRuns":64, "refusedSdkRuns":48, "emittedProductFiles":3,
        "compiledProviders":providers, "unchangedReceiverCustody":expected_custody,
        "emittedProductSourceOutputs":output_rows,
        "runtimeInputManifestSha256":input_hash,
        "runtimeExecutionReceiptSha256":sha256(execution_path),
        "sourceCustodySha256":sha256(custody_path),
        "baselineTests":21, "candidateTests":21, "syntheticValidatorTests":3,
        "baselineFailuresReproduced":True, "childApprovalReversionRefused":True,
        "sdkRecipeJUnitQualified":True, "currentBackportContractsQualified":True,
        "runtimeApplied":False,
    }
    result.update({key:False for key in FORBIDDEN})
    write("qualification-receipt.json", result)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        record = binding(fresh=True)
        write("binding-receipt.json", {"schema":1,
              "checkoutCommit":os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit":record["sourceCommit"],
              "sourceBindingSha256":sha256(HERE / "source-binding.json"),
              "boundFiles":len(record["files"]), "sdkFiles":record["sdkFiles"],
              "runtimeApplied":False, "phaseAdvanced":False})
    else:
        report()
