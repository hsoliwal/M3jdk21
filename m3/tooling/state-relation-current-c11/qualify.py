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
PREFIX = "m3/tooling/state-relation-current-c11/"
WORKFLOW = ".github/workflows/m3-state-relation-current-c11.yml"
SUITE = "com.synexia.rewrite.M3StateRelationCurrentC11RecipeTest"
METHODS = ["exactFiveOwnersReplayAsFixedPointInBothOrders",
           "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
           "historicalCRefusesCurrentOwnership"]
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
            or record["admissionCommit"] != "baa99d4a2ab1dd71fe05706bae4e7444ade70e30"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if not 28 <= len(rows) <= 40:
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
    if len(owners) != 5 or len({row["path"] for row in owners}) != 5:
        raise RuntimeError("five authentic receiving owners required")
    for row in owners:
        path = str(relative(row["path"]))
        data = regular(RECEIVER / path).read_bytes()
        if (blob(data) != row["gitBlob"] or len(data) != row["bytes"]
                or hashlib.sha256(data).hexdigest() != row["sha256"]
                or git("rev-parse", "HEAD:" + path) != row["gitBlob"]):
            raise RuntimeError("actual receiving owner differs from custody: " + path)
    if fresh and ((SDK / "target").exists() or (ROOT / "state-relation-native").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver classes and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_state_relation_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or module["RECEIVER_OUTPUTS"] != {}:
        raise RuntimeError("exact three-method KEEP-only recipe contract required")
    expected_custody = {row["path"]: row["sha256"] for row in record["receiverOriginalOwners"]}
    if module["RECEIVER_CUSTODY"] != expected_custody:
        raise RuntimeError("SDK custody does not match all five authentic receiving owners")
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
    if (sdk["schema"] != "M3_STATE_RELATION_CURRENT_C11_SDK_RECEIPT_V1"
            or sdk["scope"] != "SOURCE_ONLY_STATE_RELATION_CURRENT_C11_CUSTODY"
            or sdk["qualificationId"] != ident or sdk["checkoutCommit"] != head
            or sdk["providers"] != pins["providers"]
            or sdk["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or sdk["reports"][SUITE]["counts"] != counts
            or sdk["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest()
            or sdk["unchangedReceiverCustody"] != expected_custody
            or sdk["emittedProductSourceOutputs"] != {}
            or sdk["counterReceipt"]["sha256"] != sha256(counter_path)
            or (sdk["tests"],sdk["successfulRuns"],sdk["refusedRuns"],sdk["emittedOutputs"]) != (3,4,16,0)
            or sdk["recipeJUnitQualified"] is not True or sdk["recipeReplayQualified"] is not True):
        raise RuntimeError("actual SDK receipt/counter/source identity differs")
    for flag in module["FALSE_GATES"]:
        if sdk[flag] is not False:
            raise RuntimeError("source-only SDK receipt exceeds its boundary")
    providers = {}
    for name in ("M3GpuToolJoinRecipe","M3GpuToolJoinManifest","M3StateRelationCurrentC11Recipe"):
        path = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        data = path.read_bytes()
        if data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != 65:
            raise RuntimeError("real major65 SDK provider required")
        item = sdk["compiledProviders"][name]
        if item["bytes"] != len(data) or item["sha256"] != hashlib.sha256(data).hexdigest():
            raise RuntimeError("SDK compiled provider changed")
        providers[name] = item
    emitted = SDK / "target/emitted"
    if emitted.is_symlink() or any(path.is_symlink() or path.is_file() for path in emitted.rglob("*")):
        raise RuntimeError("KEEP-only SDK must not emit product source")
    driver = runpy.run_path(str(regular(HERE / "runtime/qualify.py")), run_name="m3_state_relation_runtime_reuse")
    metadata, actual_sources, input_hash = driver["inputs"](RECEIVER)
    if set(actual_sources) != set(expected_custody):
        raise RuntimeError("runtime input closure must be exactly five real owners")
    execution_path = regular(ARTIFACTS / "state-relation-execution.json")
    execution = json.loads(execution_path.read_bytes())
    if (execution["schema"] != "M3_STATE_RELATION_CURRENT_C11_EXECUTION_V1"
            or execution["scope"] != "EXACT_FIVE_OWNER_STATE_RELATION_JAVA_C11_JNI_DIFFERENTIAL"
            or execution["state"] != "PASS" or execution["qualificationId"] != ident
            or execution["inputManifestSha256"] != input_hash
            or execution["sourceBase"] != metadata["sourceBase"]
            or execution["preCodeAdmission"] != metadata["preCodeAdmission"]
            or (execution["cases"],execution["javaChecks"],execution["nativeChecks"]) != (71748,74829,149658)):
        raise RuntimeError("current full numeric proof identity or scope differs")
    for key in ("fullExistingProofPreserved","javaClassPathExecuted","nativeExecuted",
                "explicitLibraryLoad","xcheckJni","missingLibraryRefused","currentAliasStagingCExecuted"):
        if execution[key] is not True:
            raise RuntimeError("actual classpath/C11/JNI proof required")
    if execution["nativeSkips"] != 0:
        raise RuntimeError("native checks may not be skipped")
    for key in ("modulePatchUsed","recipeSdkQualified","sourceBindingQualifiedByDriver",
                "externalToolchainBytesSealed","advance0Qualified","fullNativeLibraryQualified",
                "cmakeIntegrationQualified","privateSourceForkReactorQualified","matchedJdkQualified",
                "stringDispatchQualified","windowsQualified","fullJdkQualified","performanceQualified",
                "directCPartialOverlapQualified","runtimeApplied","phaseAdvanced"):
        if execution[key] is not False:
            raise RuntimeError("numeric proof receipt exceeds current scope")
    work = ROOT / "state-relation-native"
    if execution["classes"] != driver["classes"](work / "classes") or len(execution["classes"]) != 10:
        raise RuntimeError("actual ten-class inventory differs")
    header = regular(ROOT / str(relative(execution["generatedJniHeader"]["path"])))
    real_header = regular(work / "headers/jdk_internal_mindex_M3StateRelation.h")
    if (header.read_bytes() != real_header.read_bytes()
            or header.stat().st_size != execution["generatedJniHeader"]["bytes"]
            or sha256(header) != execution["generatedJniHeader"]["sha256"]):
        raise RuntimeError("actual generated JNI header differs")
    library = regular(work / "libm3relation.so")
    native = execution["nativeLibrary"]
    if (library.stat().st_size != native["bytes"] or sha256(library) != native["sha256"]
            or native["cStandard"] != "c11" or native["fatalUndefinedBehaviorSanitizer"] is not True
            or native["generatedHeaderForced"] is not True or native["exportedAsArtifact"] is not False
            or set(native["jniExports"]) != driver["JNI_EXPORTS"]):
        raise RuntimeError("complete current strict C11 library differs")
    custody_ref = execution["sourceCustody"]
    custody_path = regular(ROOT / str(relative(custody_ref["path"])))
    custody_raw = custody_path.read_bytes()
    custody = json.loads(custody_raw)
    if (len(custody_raw) != custody_ref["bytes"]
            or hashlib.sha256(custody_raw).hexdigest() != custody_ref["sha256"]
            or custody["qualificationId"] != ident or custody["inputManifestSha256"] != input_hash
            or custody["files"] != 5 or custody["sourceBytes"] != 28948
            or custody["before"] != actual_sources or custody["after"] != actual_sources
            or custody["unchanged"] is not True):
        raise RuntimeError("all original source custody must remain exact")
    labels = ["state-relation-java-compile","state-relation-java-proof","state-relation-c11-compile",
              "state-relation-native-symbols","state-relation-native-proof","state-relation-missing-library-refusal"]
    commands = execution["commands"]
    if [row["label"] for row in commands] != labels:
        raise RuntimeError("all six actual numeric command records required")
    for row in commands:
        raw = regular(ROOT / str(relative(row["log"]))).read_bytes()
        if (len(raw) != row["bytes"] or hashlib.sha256(raw).hexdigest() != row["sha256"]
                or row["exitCode"] != (1 if row["label"].endswith("refusal") else 0)):
            raise RuntimeError("actual numeric command exit/log custody differs")
        if row["label"] == "state-relation-java-proof":
            driver["proof_marker"](raw, driver["JAVA_MARKER"])
        if row["label"] == "state-relation-native-proof":
            driver["proof_marker"](raw, driver["JNI_MARKER"])
        if row["label"] == "state-relation-c11-compile":
            if not {"-std=c11","-Werror","-fsanitize=undefined","-fno-sanitize-recover=all","-include"} <= set(row["argv"]):
                raise RuntimeError("actual strict C11 sanitizer command required")
        if row["label"] == "state-relation-java-compile":
            if not {"--release","21","-proc:none","-Xlint:all","-Werror","-classpath","-sourcepath","-h"} <= set(row["argv"]):
                raise RuntimeError("strict authentic Java closure command required")
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8"):
        raise RuntimeError("actual Maven SDK build success required")
    result = {
        "schema": "M3_STATE_RELATION_CURRENT_C11_HOSTED_QUALIFICATION_V1",
        "checkoutCommit": head, "canonicalSourceCommit": record["sourceCommit"],
        "qualificationId": ident, "sourceBindingSha256": sha256(HERE / "source-binding.json"),
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256": sha256(sdk_path), "surefireXmlSha256": hashlib.sha256(xml).hexdigest(),
        "suite": SUITE, "methods": METHODS, "counts": counts,
        "successfulSdkRuns": 4, "refusedSdkRuns": 16, "emittedProductFiles": 0,
        "compiledProviders": providers, "unchangedReceiverCustody": expected_custody,
        "runtimeInputManifestSha256": input_hash, "runtimeExecutionReceiptSha256": sha256(execution_path),
        "sourceCustodySha256": sha256(custody_path),
        "generatedJniHeader": execution["generatedJniHeader"], "nativeLibrary": native,
        "actualCases": 71748, "actualJavaChecks": 74829, "actualNativeChecks": 149658, "nativeSkips": 0,
        "sdkRecipeJUnitQualified": True, "currentJavaC11JniQualified": True,
        "plainClasspathQualified": True, "missingLibraryRefused": True,
        "fullExistingProofPreserved": True, "runtimeApplied": False,
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
              "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
