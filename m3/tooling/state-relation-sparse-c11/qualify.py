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
PREFIX = "m3/tooling/state-relation-sparse-c11/"
WORKFLOW = ".github/workflows/m3-state-relation-sparse-c11.yml"
SUITE = "com.synexia.rewrite.M3StateRelationSparseC11RecipeTest"
METHODS = ["bothPartialStatesRoundTripAndReachFixedPointsInBothOrders",
           "everyOwnerRefusesDriftMissingAndDuplicateWithoutMutation",
           "historicalCRefusesBothDirectionsWithoutMutation"]
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


TARGET_C = "src/java.base/share/native/libjava/M3StateRelation.c"
SOURCE_GIT = {
    "src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java": "ea904c0962fb976f43b45575f06f46ad198e29a2",
    "src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java": "780c7887aaa6b0892a76f4eee1dcb17086a60289",
    "src/java.base/share/native/libjava/M3StateRelation.c": "610ca8bab47065a49b9b58814d6392a287f4fd8b",
    "src/java.base/share/native/libjava/m3_state_relation.h": "18a02e228129827da486edc5b316790af6135310",
    "m3/compatibility/regex-relations-20261014/M3RelationPortProof.java": "e9a6beee8916cf97d5d4aac2383e7ac2fa8843e0"
}
AFTER_GIT = {**SOURCE_GIT, TARGET_C: "abb751e6056e34f25d0f09660d575f362610359e"}

def owner_plan(original, post, actual):
    lanes = (original, post, actual)
    if any(not isinstance(lane, dict) or set(lane) != set(SOURCE_GIT) for lane in lanes):
        raise RuntimeError("complete exact owner sets required")
    for name in SOURCE_GIT:
        if any(not isinstance(lane[name], bytes) for lane in lanes):
            raise RuntimeError("owner inputs must be immutable raw bytes")
        if blob(original[name]) != SOURCE_GIT[name] or blob(post[name]) != AFTER_GIT[name]:
            raise RuntimeError("known frozen original and generated postimage required: " + name)
        if name != TARGET_C and original[name] != post[name]:
            raise RuntimeError("four KEEP owners must remain byte identical")
        allowed = (original[name], post[name]) if name == TARGET_C else (original[name],)
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
    return {"schema": "M3_SPARSE_C11_KNOWN_OWNER_PLAN_CONTROLS_V1", "state": "PASS",
            "tests": 38, "successes": 2, "refused": refused, "cases": cases,
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
            or record["admissionCommit"] != "5d2d2c39120c7a779e2beb06f0af19cb7364e253"
            or record["hostAdmissionCommit"] != "73ec51da3a1e419a1bcb8816d0313dead8dbaf62"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if len(rows) != 31 or record['sdkFiles'] != 27:
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
    if not {PREFIX + "qualify.py", PREFIX + "runtime/qualify.py", PREFIX + "runtime/INPUTS.json", WORKFLOW} <= seen:
        raise RuntimeError("hosted authoring inputs must be source-bound")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--", PREFIX, WORKFLOW],
                   cwd=RECEIVER, check=True, timeout=60)
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_sparse_binding_reuse")
    module["inputs"]()
    fixture_root = SDK / module["RESOURCE"]
    original = {name: regular(fixture_root / "before" / name).read_bytes() for name in SOURCE_GIT}
    post = {name: regular(fixture_root / "out" / name).read_bytes() for name in SOURCE_GIT}
    owners = record["receiverOriginalOwners"]
    if len(owners) != 5 or {row["path"] for row in owners} != set(SOURCE_GIT):
        raise RuntimeError("five unique original receiving owners required")
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

    if fresh and ((SDK / "target").exists() or (ROOT / "state-relation-native").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver classes and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    first = json.loads(regular(ARTIFACTS / "binding-receipt.json").read_bytes())
    if first["observedCurrentOwners"] != record["observedCurrentOwners"]:
        raise RuntimeError("current source changed between binding and report")
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_sparse_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or module["RECEIVER_OUTPUTS"] != {
            TARGET_C: "5d40fe7c65d54e26c6964313186624675592703ea6e2e1cb2886840b2f15347e"}:
        raise RuntimeError("exact three-method one-C recipe contract required")
    expected_custody = {row["path"]: row["sha256"] for row in record["receiverOriginalOwners"]
                        if row["path"] != TARGET_C}
    if module["RECEIVER_CUSTODY"] != expected_custody or len(expected_custody) != 4:
        raise RuntimeError("all four immutable owning inputs required")
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
    if (sdk["schema"] != "M3_STATE_RELATION_SPARSE_C11_SDK_RECEIPT_V1"
            or sdk["scope"] != "SOURCE_ONLY_STATE_RELATION_SPARSE_C11_PROPOSAL"
            or sdk["qualificationId"] != ident or sdk["checkoutCommit"] != head
            or sdk["providers"] != pins["providers"]
            or sdk["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or sdk["reports"][SUITE]["counts"] != counts
            or sdk["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest()
            or sdk["unchangedReceiverCustody"] != expected_custody
            or sdk["counterReceipt"]["sha256"] != sha256(counter)
            or (sdk["tests"],sdk["successfulRuns"],sdk["refusedRuns"],sdk["emittedOutputs"]) != (3,16,32,2)
            or sdk["recipeJUnitQualified"] is not True or sdk["recipeReplayQualified"] is not True):
        raise RuntimeError("actual SDK receipt/counter/source identity differs")
    for flag in module["FALSE_GATES"]:
        if sdk[flag] is not False:
            raise RuntimeError("source-only SDK receipt exceeds its boundary")
    providers = {}
    expected_providers = ("M3GpuToolJoinRecipe", "M3GpuToolJoinManifest",
                          "M3StateRelationSparseC11Recipe", "M3StateRelationSparseC11InverseRecipe")
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
                or [path.relative_to(output).as_posix() for path in paths if path.is_file()] != [TARGET_C]
                or set(sdk[receipt_key]) != {TARGET_C}):
            raise RuntimeError("one exact authentic directional C emission required")
        raw = regular(output / TARGET_C).read_bytes()
        expected_raw = regular(SDK / module["RESOURCE"] / stage / TARGET_C).read_bytes()
        item = sdk[receipt_key][TARGET_C]
        if (raw != expected_raw or item["bytes"] != len(raw)
                or item["sha256"] != hashlib.sha256(raw).hexdigest() or item["gitBlob"] != blob(raw)):
            raise RuntimeError("actual emitted C differs from recipe postimage")
        emissions[folder] = item
    driver = runpy.run_path(str(regular(HERE / "runtime/qualify.py")), run_name="m3_state_relation_runtime_reuse")
    metadata, actual_sources, input_hash = driver["inputs"](RECEIVER)
    if actual_sources != record["observedCurrentOwners"] or set(actual_sources) != set(SOURCE_GIT):
        raise RuntimeError("runtime input closure must be exactly five real owners")
    execution_path = regular(ARTIFACTS / "state-relation-execution.json")
    execution = json.loads(execution_path.read_bytes())
    if (execution["schema"] != "M3_STATE_RELATION_SPARSE_C11_EXECUTION_V1"
            or execution["scope"] != "EXACT_EXISTING_PROOF_ORIGINAL_AND_MAVEN_EMITTED_SPARSE_PORTABLE_C11_JNI"
            or execution["state"] != "PASS" or execution["qualificationId"] != ident
            or execution["inputManifestSha256"] != input_hash
            or execution["sourceBase"] != metadata["sourceBase"]
            or execution["preCodeAdmission"] != metadata["preCodeAdmission"]
            or (execution["cases"],execution["javaChecks"],execution["nativeChecks"]) != (71748,74829,149658)):
        raise RuntimeError("current full numeric proof identity or scope differs")
    for key in ("fullExistingProofPreserved","javaClassPathExecuted","nativeExecuted",
                "explicitLibraryLoad","xcheckJni","missingLibraryRefused","baselineCExecuted",
                "actualMavenEmittedSparseCExecuted","forcedPortableCExecuted"):
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
    natives = execution["nativeLibraries"]
    if set(natives) != {"baseline", "sparse", "portable"}:
        raise RuntimeError("exact three native variants required")
    for variant, native in natives.items():
        library = regular(work / ("libm3relation-" + variant + ".so"))
        source_git = SOURCE_GIT[TARGET_C] if variant == "baseline" else AFTER_GIT[TARGET_C]
        if (native["path"] != str(library.relative_to(ROOT))
                or library.stat().st_size != native["bytes"] or sha256(library) != native["sha256"]
                or native["cStandard"] != "c11" or native["fatalUndefinedBehaviorSanitizer"] is not True
                or native["generatedHeaderForced"] is not True or native["exportedAsArtifact"] is not False
                or set(native["jniExports"]) != driver["JNI_EXPORTS"]
                or native["sourceGitBlob"] != source_git
                or native["forcedPortableScan"] is not (variant == "portable")
                or (native["cases"], native["nativeChecks"], native["nativeSkips"]) != (71748,149658,0)):
            raise RuntimeError("complete strict variant library differs: " + variant)
    custody_ref = execution["sourceCustody"]
    custody_path = regular(ROOT / str(relative(custody_ref["path"])))
    custody_raw = custody_path.read_bytes()
    custody = json.loads(custody_raw)
    if (len(custody_raw) != custody_ref["bytes"]
            or hashlib.sha256(custody_raw).hexdigest() != custody_ref["sha256"]
            or custody["qualificationId"] != ident or custody["inputManifestSha256"] != input_hash
            or custody["files"] != 5 or custody["sourceBytes"] != sum(row["bytes"] for row in actual_sources.values())
            or custody["frozenOriginalBytes"] != 28948
            or custody["before"] != actual_sources or custody["after"] != actual_sources
            or custody["unchanged"] is not True):
        raise RuntimeError("all original source custody must remain exact")
    labels = ["state-relation-java-compile", "state-relation-java-proof"]
    for variant in ("baseline", "sparse", "portable"):
        labels.extend("state-relation-" + variant + "-" + suffix
                      for suffix in ("c11-compile", "native-symbols", "native-proof"))
    labels.append("state-relation-missing-library-refusal")
    commands = execution["commands"]
    if [row["label"] for row in commands] != labels:
        raise RuntimeError("all twelve actual numeric command records required")
    for row in commands:
        raw = regular(ROOT / str(relative(row["log"]))).read_bytes()
        if (len(raw) != row["bytes"] or hashlib.sha256(raw).hexdigest() != row["sha256"]
                or row["exitCode"] != (1 if row["label"].endswith("refusal") else 0)):
            raise RuntimeError("actual numeric command exit/log custody differs")
        if row["label"] == "state-relation-java-proof":
            driver["proof_marker"](raw, driver["JAVA_MARKER"])
        if row["label"].endswith("-native-proof"):
            driver["proof_marker"](raw, driver["JNI_MARKER"])
        if row["label"].endswith("-c11-compile"):
            if not {"-std=c11","-Werror","-fsanitize=undefined","-fno-sanitize-recover=all","-include"} <= set(row["argv"]):
                raise RuntimeError("actual strict C11 sanitizer command required")
        if row["label"] == "state-relation-java-compile":
            if not {"--release","21","-proc:none","-Xlint:all","-Werror","-classpath","-sourcepath","-h"} <= set(row["argv"]):
                raise RuntimeError("strict authentic Java closure command required")
    for variant in ("baseline", "sparse", "portable"):
        row = next(row for row in commands if row["label"] == "state-relation-" + variant + "-c11-compile")
        forced = "-DM3_STATE_RELATION_FORCE_POSITIONAL_SCAN"
        if (forced in row["argv"]) != (variant == "portable"):
            raise RuntimeError("actual portable macro command required")
        native_source = SDK / ("src/main/resources/com/synexia/rewrite/state-relation-sparse-c11-20261020/before/"
                               + TARGET_C) if variant == "baseline" else SDK / "target/emitted" / TARGET_C
        if str(native_source) not in row["argv"]:
            raise RuntimeError("compile must consume actual declared original/emitted source")
        source_raw = regular(native_source).read_bytes()
        if (natives[variant]["sourceBytes"] != len(source_raw)
                or natives[variant]["sourceSha256"] != hashlib.sha256(source_raw).hexdigest()
                or natives[variant]["sourceGitBlob"] != blob(source_raw)):
            raise RuntimeError("compiled native source drift")
    if "[INFO] BUILD SUCCESS" not in regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8"):
        raise RuntimeError("actual Maven SDK build success required")
    result = {
        "schema": "M3_STATE_RELATION_SPARSE_C11_HOSTED_QUALIFICATION_V1",
        "scope": "SOURCE_RECIPE_AND_EXPLICIT_NUMERIC_THREE_VARIANT_C11_JNI",
        "checkoutCommit": head, "canonicalSourceCommit": record["sourceCommit"],
        "qualificationId": ident, "sourceBindingSha256": sha256(HERE / "source-binding.json"),
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256": sha256(sdk_path), "surefireXmlSha256": hashlib.sha256(xml).hexdigest(),
        "suite": SUITE, "methods": METHODS, "counts": counts,
        "successfulSdkRuns": 16, "refusedSdkRuns": 32, "emittedProductFiles": 2,
        "compiledProviders": providers, "unchangedReceiverCustody": expected_custody,
        "emissions": emissions, "receiverCurrentOwners": record["observedCurrentOwners"],
        "ownerPlanControls": record["ownerPlanControls"],
        "sdkRecipeJUnitQualified": True, "knownOwnerSourceReplayQualified": True,
        "actualCState": ("APPLIED" if record["observedCurrentOwners"][TARGET_C]["gitBlob"]
                                == AFTER_GIT[TARGET_C] else "ORIGINAL"),
        "productCIntegrationQualified": False, "runtimeApplied": False,
        "runtimeExecutionReceiptSha256": sha256(execution_path),
        "runtimeInputManifestSha256": input_hash, "sourceCustodySha256": sha256(custody_path),
        "generatedJniHeader": execution["generatedJniHeader"], "nativeLibraries": natives,
        "actualCasesPerVariant": 71748, "actualJavaChecks": 74829,
        "actualNativeChecksPerVariant": 149658, "nativeVariants": 3, "nativeSkips": 0,
        "sparseAndPortableC11JniQualified": True, "missingLibraryRefused": True,
        "fullExistingProofPreserved": True, "performanceQualified": False,
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
