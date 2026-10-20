#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind the isolated original-path proof copy and inspect actual focused JNI receipts."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
RECEIVER = HERE.parents[2]
SOURCE = HERE / "source"
TASK_REL = "synexia-openrewrite-recipes/tasks/native-text-metadata-current-owner-20261019"
TASK = SOURCE / TASK_REL
SDK = TASK / "guard-sdk"
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ARTIFACTS = ROOT / "artifacts"
SUITE = "com.synexia.rewrite.M3TextMetadataCurrentOwnerRecipeTest"
METHODS = {"namedCurrentTupleReplaysAsExactFixedPointInBothOrders",
           "historicalRecipeAndArchivesRetainTheirOriginalAdmissionBoundary",
           "refusesUnknownMixedMissingAndDuplicateOwnersWithoutMutation"}
ENGINES = {"M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
           "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
           "M3CompositeSummaryRecipe": "8037c9d7370560981e600783945b39e855226162",
           "M3TextMetadataCurrentOwnerRecipe": "a44745ad0f4b42e55fe41dc1ea260c2a66c53240"}
COMMANDS = ["text-metadata-replay-compile", "text-metadata-current-replay",
            "text-metadata-historical-refusal", "text-metadata-java-compile",
            "text-metadata-java-proof", "text-metadata-c11-compile",
            "text-metadata-native-proof", "text-metadata-missing-library-refusal"]


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


def file_identity(path):
    data = regular(path).read_bytes()
    return {"bytes": len(data), "sha256": digest(data), "gitBlob": blob(data)}


def git(*arguments, binary=False):
    return subprocess.check_output(["git", *arguments], cwd=RECEIVER,
                                   timeout=60, text=not binary).strip()


def write(name, value):
    require(ARTIFACTS.is_dir()
            and all(not item.is_symlink() for item in [ARTIFACTS, *ARTIFACTS.parents]),
            "missing or redirected qualification artifact directory")
    destination = ARTIFACTS / name
    require(not destination.exists() and not destination.is_symlink(), "fresh receipt required")
    with destination.open("x", encoding="utf-8") as stream:
        stream.write(json.dumps(value, indent=2) + "\n")


def binding(fresh=False):
    head = git("rev-parse", "HEAD")
    require(head == os.environ["EXPECTED_SOURCE_SHA"] and len(head) == 40, "exact head required")
    raw = regular(HERE / "source-binding.json").read_bytes()
    require(blob(raw) == git("rev-parse", "HEAD:m3/tooling/text-metadata-current-owner/source-binding.json"),
            "source binding itself differs from exact checkout")
    record = json.loads(raw)
    require(record["repository"] == "hsoliwal/com.synexia"
            and record["receiverRepository"] == "hsoliwal/M3jdk21"
            and record["runtimeApplied"] is False and record["phaseAdvanced"] is False,
            "source binding identity or scope drift")
    revision = record["sourceCommit"]
    require(len(revision) == 40 and all(c in "0123456789abcdef" for c in revision),
            "canonical source commit required")
    prefix = "m3/tooling/text-metadata-current-owner/"
    source_prefix = prefix + "source/"
    rows = record["files"]
    require(61 <= len(rows) <= 72, "bounded complete original-path source binding required")
    seen = set()
    for row in rows:
        path = str(relative(row["targetPath"]))
        require(path not in seen and (path.startswith(prefix) or
                path == ".github/workflows/m3-text-metadata-current-owner.yml"),
                "source binding ownership drift")
        seen.add(path)
        data = regular(RECEIVER / path).read_bytes()
        require(blob(data) == row["gitBlob"] and len(data) == row["bytes"],
                "canonical source input differs: " + path)
    raw_inputs = regular(TASK / "INPUTS.json").read_bytes()
    inputs = json.loads(raw_inputs)
    require(inputs["schema"] == 1 and inputs["capBytes"] == 512 * 1024 ** 2
            and inputs["reserveBytes"] == 8 * 1024 ** 3 and len(inputs["files"]) == 58,
            "native source custody policy drift")
    selected = {}
    for row in inputs["files"]:
        name = str(relative(row["path"]))
        require(name not in selected, "duplicate native input pin")
        actual = file_identity(SOURCE / name)
        require(all(actual[key] == row[key] for key in actual), "native input pin drift: " + name)
        selected[name] = actual
    tracked = set(git("ls-files", "-z", "--", source_prefix, binary=True).decode("utf-8").split("\0"))
    tracked.discard("")
    expected = {source_prefix + path for path in selected} | {source_prefix + TASK_REL + "/INPUTS.json"}
    require(len(expected) == 59 and tracked == expected
            and {path for path in seen if path.startswith(source_prefix)} == expected,
            "exact complete 59-file original-path source copy required")
    require({prefix + "qualify.py", ".github/workflows/m3-text-metadata-current-owner.yml"} <= seen,
            "hosted authoring inputs must be source-bound")
    require(os.environ["M3_SOURCE_ROOT"] == str(SOURCE)
            and os.environ["M3_QUALIFICATION_ROOT"] == str(ROOT), "explicit mini-source routing required")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--", prefix,
                    ".github/workflows/m3-text-metadata-current-owner.yml"],
                   cwd=RECEIVER, check=True, timeout=60)
    if fresh:
        require(not (SDK / "target").exists() and not (ROOT / "text-metadata-native").exists()
                and not (ARTIFACTS / "text-metadata-guard-sdk.json").exists()
                and not any((ROOT / "m2").rglob("*.jar")), "proof outputs and isolated Maven repo must start fresh")
    return record, selected, raw_inputs


def class_identity(path):
    data = regular(path).read_bytes()
    require(len(data) >= 8 and data[:4] == b"\xca\xfe\xba\xbe"
            and int.from_bytes(data[6:8], "big") == 65, "actual Java21 class required")
    return {"bytes": len(data), "sha256": digest(data), "major": 65}


def report():
    record, selected, raw_inputs = binding()
    ident = os.environ["M3_QUALIFICATION_ID"]
    execution_raw = regular(ARTIFACTS / "text-metadata-execution.json").read_bytes()
    execution = json.loads(execution_raw)
    require(execution["schema"] == 1 and execution["qualificationId"] == ident
            and execution["state"] == "PASS_NATIVE"
            and execution["scope"] == "EXACT_TEXT_METADATA_JAVA_C11_JNI_DIFFERENTIAL",
            "fresh actual focused native proof required")
    require(execution["requiredNative"] is True and execution["nativeExecuted"] is True
            and execution["explicitLibraryLoad"] is True and execution["xcheckJni"] is True
            and execution["nativeSkips"] == 0 and execution["layouts"] == 76416
            and execution["javaChecks"] == 309986 and execution["nativeChecks"] == 386819,
            "actual Java and native proof counters or controls differ")
    for key in ("runtimeApplied", "phaseAdvanced", "fullNativeLibraryQualified",
                "cmakeIntegrationQualified", "compositeBehaviorQualified",
                "privateSourceForkReactorQualified", "fullJdkQualified", "windowsQualified", "performanceQualified"):
        require(execution[key] is False, "focused proof exceeds scope: " + key)
    require(execution["tools"]["externalToolchainBytesSealed"] is False,
            "external toolchain byte sealing remains separate")
    custody_ref = execution["sourceCustody"]
    custody_path = ROOT / str(relative(custody_ref["path"]))
    custody_raw = regular(custody_path).read_bytes()
    require(len(custody_raw) == custody_ref["bytes"] and digest(custody_raw) == custody_ref["sha256"],
            "actual source custody receipt differs")
    custody = json.loads(custody_raw)
    require(custody["qualificationId"] == ident and custody["unchanged"] is True
            and custody["before"] == selected and custody["after"] == selected
            and custody["inputManifestSha256"] == digest(raw_inputs)
            and custody["directProofSourceBytes"] == 17706 and custody["historicalArchivesChanged"] is False,
            "before and after original source custody differs")
    xml_path = SDK / ("target/surefire-reports/TEST-" + SUITE + ".xml")
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    require(reports == [xml_path], "one fresh three-method SDK suite required")
    xml = regular(xml_path).read_bytes()
    suite = ET.fromstring(xml)
    counts = {name: int(suite.get(name, "-1")) for name in ("tests", "failures", "errors", "skipped")}
    require(suite.tag == "testsuite" and suite.get("name") == SUITE
            and counts == {"tests": 3, "failures": 0, "errors": 0, "skipped": 0},
            "actual SDK suite failed, errored or skipped")
    cases = suite.findall("testcase")
    require(len(cases) == 3 and {row.get("name") for row in cases} == METHODS
            and all(row.get("classname") == SUITE
                    and all(child.tag in ("system-out", "system-err") for child in row) for row in cases),
            "all named SDK methods must execute")
    sdk = execution["sdk"]
    sdk_raw = regular(ARTIFACTS / "text-metadata-guard-sdk.json").read_bytes()
    sdk_receipt = json.loads(sdk_raw)
    require(sdk["reportRoot"] == "SOURCE" and sdk["report"] == xml_path.relative_to(SOURCE).as_posix()
            and sdk["reportSha256"] == digest(xml) and sdk["reportBytes"] == len(xml)
            and sdk["sha256"] == digest(sdk_raw) and sdk["bytes"] == len(sdk_raw),
            "actual SDK execution receipt linkage differs")
    require(sdk_receipt["qualificationId"] == ident and sdk_receipt["state"] == "PASS"
            and sdk_receipt["successfulSdkRuns"] == 8 and sdk_receipt["refusedSdkRuns"] == 26
            and sdk_receipt["tests"] == 3 and sdk_receipt["nativeExecuted"] is False
            and sdk_receipt["runtimeApplied"] is False and sdk_receipt["phaseAdvanced"] is False,
            "actual SDK scheduling receipt differs")
    provider_classes = {}
    for name, expected in ENGINES.items():
        owner = SDK / ("pinned-engine/com/synexia/rewrite/" + name + ".java")
        require(blob(regular(owner).read_bytes()) == expected, "provider source drift")
        provider_classes[name] = class_identity(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
    require([row["label"] for row in execution["commands"]] == COMMANDS, "exact native command sequence required")
    commands = []
    refusal_labels = {"text-metadata-historical-refusal", "text-metadata-missing-library-refusal"}
    for row in execution["commands"]:
        log = regular(ROOT / str(relative(row["log"]))).read_bytes()
        require(digest(log) == row["sha256"] and len(log) == row["bytes"]
                and (row["exitCode"] != 0 if row["label"] in refusal_labels else row["exitCode"] == 0),
                "actual native command log or exit differs")
        commands.append({"label": row["label"], "exitCode": row["exitCode"],
                         "log": row["log"], "bytes": len(log), "sha256": digest(log)})
    work = ROOT / "text-metadata-native"
    for row in execution["classes"]:
        actual = class_identity(work / "classes" / str(relative(row["path"])))
        require(all(actual[key] == row[key] for key in actual), "actual proof class differs after execution")
    header_ref = execution["generatedJniHeader"]
    header = regular(ROOT / str(relative(header_ref["path"]))).read_bytes()
    require(len(header) == header_ref["bytes"] and digest(header) == header_ref["sha256"]
            and header == regular(work / "headers/com_synexia_mindex_TextMetadata.h").read_bytes(),
            "actual generated JNI header differs")
    library = regular(work / "libm3_summary_proof.so").read_bytes()
    native = execution["nativeLibrary"]
    require(library[:4] == b"\x7fELF" and len(library) == native["bytes"]
            and digest(library) == native["sha256"] and native["generatedHeaderForced"] is True
            and native["cStandard"] == "c11" and native["fatalUndefinedBehaviorSanitizer"] is True
            and native["exportedAsArtifact"] is False, "actual native library receipt differs")
    for name in ("guard-sdk-maven.log", "native-maven.log"):
        require("[INFO] BUILD SUCCESS" in regular(ARTIFACTS / name).read_text(encoding="utf-8"),
                "actual Maven verify command success required")
    write("qualification-receipt.json", {
        "schema": "M3_TEXT_METADATA_HOSTED_QUALIFICATION_V1",
        "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"], "canonicalSourceCommit": record["sourceCommit"],
        "qualificationId": ident, "sourceBindingSha256": digest(regular(HERE / "source-binding.json").read_bytes()),
        "inputManifestSha256": digest(raw_inputs), "sourceInputFiles": len(selected),
        "actualExecutionReceiptSha256": digest(execution_raw), "sourceCustodySha256": digest(custody_raw),
        "sdkReceiptSha256": digest(sdk_raw), "surefireXmlSha256": digest(xml),
        "suite": SUITE, "methods": sorted(METHODS), "counts": counts,
        "sdkSuccessfulRuns": 8, "sdkRefusedRuns": 26, "compiledProviderClasses": provider_classes,
        "commands": commands, "focusedTextMetadataJavaC11JniQualified": True,
        "layouts": 76416, "javaChecks": 309986, "nativeChecks": 386819,
        "generatedJniHeader": header_ref, "nativeLibrary": native,
        "originalPrivateModelQualified": False, "productTargetApplied": False,
        "fullNativeLibraryQualified": False, "fullMatchedJdkQualified": False,
        "performanceRetentionQualified": False, "phaseAdvanced": False,
    })


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("operation", choices=("binding", "report"))
    operation = parser.parse_args().operation
    if operation == "binding":
        record, selected, raw_inputs = binding(fresh=True)
        write("binding-receipt.json", {"schema": 1, "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
              "canonicalSourceCommit": record["sourceCommit"], "sourceInputFiles": len(selected),
              "sourceBindingSha256": digest(regular(HERE / "source-binding.json").read_bytes()),
              "inputManifestSha256": digest(raw_inputs), "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
