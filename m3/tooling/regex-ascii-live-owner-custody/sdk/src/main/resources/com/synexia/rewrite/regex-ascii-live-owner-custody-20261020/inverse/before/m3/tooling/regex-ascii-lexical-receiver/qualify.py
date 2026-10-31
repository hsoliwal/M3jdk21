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
PREFIX = "m3/tooling/regex-ascii-lexical-receiver/"
WORKFLOW = ".github/workflows/m3-regex-ascii-lexical-receiver.yml"
SUITE = "com.synexia.rewrite.M3RegexAsciiLexicalReceiverRecipeTest"
METHODS = ["all8PartialStatesBothOrdersReachExactFixedPoint",
           "all8InverseStatesBothOrdersRecoverExactBefore",
           "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership"]
COMPILER = "src/java.base/share/classes/jdk/internal/mindex/M3RegExpCompiler.java"
TEST = "test/jdk/java/util/regex/M3AutomatonDifferentialTest.java"
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
}
FORBIDDEN = ("originalPrivateModelQualified", "jniRuntimeQualified", "fullMatchedJdkQualified",
             "performanceRetentionQualified", "productTargetApplied", "phaseAdvanced")


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


SOURCE_GIT = {
    "m3/docs/name-mapping.json": "a7008bc04536586218da76e823bf570d50cec526",
    "src/java.base/share/classes/jdk/internal/mindex/M3Automaton.java": "7755e02e9da1546240afb96078a86e766566ff59",
    "src/java.base/share/classes/jdk/internal/mindex/M3AutomatonOps.java": "58cdad630918df11d186312b706bcea03ab52ffe",
    "src/java.base/share/classes/jdk/internal/mindex/M3ChunkCursor.java": "30df0c03381ba2348899d1a842a13045466ef500",
    "src/java.base/share/classes/jdk/internal/mindex/M3NotYetReceived.java": "ac96ac14ced11cf51983e555b7ff7898e2b0a4e9",
    "src/java.base/share/classes/jdk/internal/mindex/M3RegExpCompiler.java": "3cd380eca7cc903ea7fb8f80cccae1894a851ac2",
    "src/java.base/share/classes/jdk/internal/mindex/M3RunTable.java": "8363e554ec77a65587a18ab468bdabd967e5f10d",
    "test/jdk/java/util/regex/M3AutomatonDifferentialTest.java": "687dff70a6278355847a287d7dc3792867471b58"
}
AFTER_GIT = {
    "m3/docs/name-mapping.json": "b97a5f233cd993fbaf7bb3c297bf1821c6f4b8d7",
    "src/java.base/share/classes/jdk/internal/mindex/M3Automaton.java": "7755e02e9da1546240afb96078a86e766566ff59",
    "src/java.base/share/classes/jdk/internal/mindex/M3AutomatonOps.java": "58cdad630918df11d186312b706bcea03ab52ffe",
    "src/java.base/share/classes/jdk/internal/mindex/M3ChunkCursor.java": "30df0c03381ba2348899d1a842a13045466ef500",
    "src/java.base/share/classes/jdk/internal/mindex/M3NotYetReceived.java": "ac96ac14ced11cf51983e555b7ff7898e2b0a4e9",
    "src/java.base/share/classes/jdk/internal/mindex/M3RegExpCompiler.java": "e51d3910df949e63229b100bc81716bc3f2742d6",
    "src/java.base/share/classes/jdk/internal/mindex/M3RunTable.java": "8363e554ec77a65587a18ab468bdabd967e5f10d",
    "test/jdk/java/util/regex/M3AutomatonDifferentialTest.java": "3d95edee3c14c0a2a142217da695e77ed69e8026"
}
CHANGED_PATHS = tuple(name for name in SOURCE_GIT if SOURCE_GIT[name] != AFTER_GIT[name])

def owner_plan(original, post, actual):
    lanes = (original, post, actual)
    if any(not isinstance(lane, dict) or set(lane) != set(SOURCE_GIT) for lane in lanes):
        raise RuntimeError("complete exact owner sets required")
    for name in SOURCE_GIT:
        if any(not isinstance(lane[name], bytes) for lane in lanes):
            raise RuntimeError("owner inputs must be immutable raw bytes")
        if blob(original[name]) != SOURCE_GIT[name] or blob(post[name]) != AFTER_GIT[name]:
            raise RuntimeError("known frozen original and generated postimage required: " + name)
        if name not in CHANGED_PATHS and original[name] != post[name]:
            raise RuntimeError("five KEEP owners must remain byte identical")
        allowed = (original[name], post[name]) if name in CHANGED_PATHS else (original[name],)
        if actual[name] not in allowed:
            raise RuntimeError("unknown current owner drift: " + name)
    return {name: {"bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)}
            for name, raw in actual.items()}


def self_test_owner_plan(original, post):
    cases = []
    if len(CHANGED_PATHS) != 3:
        raise RuntimeError("three exact lexical replacements required")
    for state in range(8):
        actual = dict(original)
        for bit, name in enumerate(CHANGED_PATHS):
            if state & (1 << bit):
                actual[name] = post[name]
        owner_plan(original, post, actual)
        cases.append("known-current-state:" + str(state))
    refused = 0
    def reject(lanes, label):
        nonlocal refused
        try:
            owner_plan(*lanes)
        except RuntimeError:
            refused += 1
            cases.append(label)
        else:
            raise AssertionError("unknown owner tuple admitted: " + label)
    for index, label in enumerate(("frozen-original", "known-postimage", "actual-current")):
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
    if refused != 54 or len(cases) != 62:
        raise AssertionError("complete eight-state owner control inventory drift")
    return {"schema": "M3_LEXICAL_LIVE_OWNER_CONSTRUCTOR_CONTROLS_V1", "state": "PASS",
            "tests": 62, "successes": 8, "refused": refused, "cases": cases,
            "scope": "PURE_BYTE_CONSTRUCTOR_NO_SOURCE_WRITES"}


def current_owner_state(metadata):
    count = sum(metadata[name]["gitBlob"] == AFTER_GIT[name] for name in CHANGED_PATHS)
    return "ORIGINAL" if count == 0 else "APPLIED" if count == 3 else "PARTIAL_KNOWN_STATE"


def live_owner_custody():
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_lexical_live_owner_sdk")
    module["inputs"]()
    fixture = SDK / module["RESOURCE"] / "forward"
    original = {name: regular(fixture / "before" / name).read_bytes() for name in SOURCE_GIT}
    post = {name: regular(fixture / "out" / name).read_bytes() for name in SOURCE_GIT}
    actual = {}
    for name in SOURCE_GIT:
        raw = regular(RECEIVER / str(relative(name))).read_bytes()
        if git("rev-parse", "HEAD:" + name) != blob(raw):
            raise RuntimeError("live receiving file differs from checkout: " + name)
        actual[name] = raw
    result = owner_plan(original, post, actual)
    if sum(len(value) for value in original.values()) != 578555:
        raise RuntimeError("frozen original owner byte census drift")
    if sum(len(value) for value in post.values()) != 594441:
        raise RuntimeError("known complete postimage byte census drift")
    return result, self_test_owner_plan(original, post)


def cohort_sources(current):
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_lexical_cohort_source_plan")
    module["inputs"]()
    fixture = SDK / module["RESOURCE"] / "forward"
    emitted = SDK / "target/emitted/regex-ascii-lexical-receiver-20261019/forward"
    for name in CHANGED_PATHS:
        raw = regular(emitted / name).read_bytes()
        if blob(raw) != AFTER_GIT[name] or raw != regular(fixture / "out" / name).read_bytes():
            raise RuntimeError("authentic same-execution Maven postimage required: " + name)
    dependencies = [RECEIVER / name for name in SOURCE_GIT if name not in CHANGED_PATHS]
    compiler = RECEIVER / COMPILER if current[COMPILER]["gitBlob"] == AFTER_GIT[COMPILER] else emitted / COMPILER
    test = RECEIVER / TEST if current[TEST]["gitBlob"] == AFTER_GIT[TEST] else emitted / TEST
    return {
        "baseline": {"compiler": fixture / "before" / COMPILER, "test": fixture / "before" / TEST,
                     "dependencies": dependencies},
        "negative": {"compiler": fixture / "before" / COMPILER, "test": test, "dependencies": dependencies},
        "corrected": {"compiler": compiler, "test": test, "dependencies": dependencies},
    }


def cohort_source_identities(plans):
    result = {}
    for cohort, plan in plans.items():
        rows = []
        for role, paths in (("dependency", plan["dependencies"]), ("compiler", [plan["compiler"]]),
                            ("test", [plan["test"]])):
            for path in paths:
                raw = regular(path).read_bytes()
                if path.is_relative_to(SDK / "target/emitted"):
                    origin = "ACTUAL_MAVEN_EMISSION"
                elif path.is_relative_to(SDK):
                    origin = "FROZEN_ORIGINAL_SDK_FIXTURE"
                elif path.is_relative_to(RECEIVER):
                    origin = "ACTUAL_RECEIVER"
                else:
                    raise RuntimeError("undeclared cohort source origin")
                rows.append({"role": role, "sourcePath": path.relative_to(RECEIVER).as_posix(),
                             "origin": origin, "bytes": len(raw),
                             "sha256": hashlib.sha256(raw).hexdigest(), "gitBlob": blob(raw)})
        if len(rows) != 7 or sum(row["role"] == "dependency" for row in rows) != 5:
            raise RuntimeError("seven authentic cohort sources required")
        result[cohort] = rows
    return result


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
            or record["admissionCommit"] != "812544033b50937bd7d976cca5f7cd09b5003b93"):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if not 48 <= len(rows) <= 64:
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
    record["receiverCurrentOwners"], record["ownerPlanControls"] = live_owner_custody()
    record["actualProductState"] = current_owner_state(record["receiverCurrentOwners"])
    if fresh and ((SDK / "target").exists() or (ROOT / "receiver").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver classes and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    first = json.loads(regular(ARTIFACTS / "binding-receipt.json").read_bytes())
    if first["receiverCurrentOwners"] != record["receiverCurrentOwners"]:
        raise RuntimeError("live receiver changed between initial and final checks")
    source_path = regular(ARTIFACTS / "receiver-source-custody.json")
    custody = json.loads(source_path.read_bytes())
    expected_cohorts = cohort_source_identities(cohort_sources(record["receiverCurrentOwners"]))
    if (custody["before"] != record["receiverCurrentOwners"]
            or custody["after"] != record["receiverCurrentOwners"]
            or custody["cohortSources"] != expected_cohorts
            or custody["cohortSourcesAfter"] != expected_cohorts or custody["unchanged"] is not True):
        raise RuntimeError("complete live and frozen cohort source custody required")
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_lexical_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or len(module["RECEIVER_OUTPUTS"]) != 6:
        raise RuntimeError("actual existing SDK contract differs")
    sdk_receipt = regular(SDK / "target/sdk-receipt.json")
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1 or reports[0].name != "TEST-" + SUITE + ".xml":
        raise RuntimeError("one fresh actual three-method SDK suite required")
    xml = regular(reports[0]).read_bytes()
    counts = module["validate_report"](xml, SUITE, METHODS)
    value = json.loads(sdk_receipt.read_bytes())
    if (value["scope"] != "SOURCE_ONLY_REGEX_ASCII_LEXICAL_RECEIVER_RECIPE_REPLAY"
            or value["checkoutCommit"] != os.environ["EXPECTED_SOURCE_SHA"]
            or value["qualificationId"] != os.environ["M3_QUALIFICATION_ID"]
            or value["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or value["providers"] != pins["providers"]
            or value["emittedProductSourceOutputs"] != module["RECEIVER_OUTPUTS"]
            or value["unchangedReceiverCustody"] != module["RECEIVER_CUSTODY"]
            or value["reports"][SUITE]["counts"] != counts
            or value["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest()):
        raise RuntimeError("actual SDK receipt identity or evidence mismatch")
    for key in ("targetJavaCompiled", "originalPrivateOwnerModuleQualified",
                "receiverRuntimeQualified", "jniRuntimeQualified", "fullMatchedJdkQualified",
                "performanceRetentionQualified", "productTargetApplied", "runtimeApplied", "phaseAdvanced"):
        if value[key] is not False:
            raise RuntimeError("SDK source replay receipt exceeds its scope")
    counters = regular(SDK / "target/recipe-test-counters.json")
    if value["counterReceipt"]["sha256"] != sha256(counters):
        raise RuntimeError("actual counter receipt digest differs")
    if (value["tests"], value["forwardApplies"], value["inverseApplies"], value["refusedRuns"]) != (3,32,32,48):
        raise RuntimeError("actual recipe counter inventory differs")
    providers = []
    for name, expected in ENGINES.items():
        source = regular(SDK / ("pinned-engine/com/synexia/rewrite/" + name + ".java"))
        if blob(source.read_bytes()) != expected:
            raise RuntimeError("original provider source replaced")
        compiled = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        data = compiled.read_bytes()
        if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != 65:
            raise RuntimeError("actual provider Java21 class required")
        providers.append({"class": name, "sourceGitBlob": expected, "classBytes": len(data),
                          "classSha256": hashlib.sha256(data).hexdigest(), "classMajor": 65})
    emitted_root = SDK / "target/emitted/regex-ascii-lexical-receiver-20261019"
    actual = {path.relative_to(emitted_root).as_posix()
              for path in emitted_root.rglob("*") if path.is_file()}
    if actual != set(module["RECEIVER_OUTPUTS"]):
        raise RuntimeError("six exact recipe-emitted source outputs required")
    candidates = []
    for path, digest in module["RECEIVER_OUTPUTS"].items():
        emitted = regular(emitted_root / relative(path))
        if sha256(emitted) != digest:
            raise RuntimeError("actual emitted source differs from sealed recipe output")
        candidates.append({"path": path, "bytes": emitted.stat().st_size, "sha256": digest})
    receiver = ROOT / "receiver"
    class_records = {}
    for cohort in ("baseline", "negative", "corrected"):
        files = sorted((receiver / cohort).rglob("*.class"))
        if len(files) < 7:
            raise RuntimeError("actual authentic receiver closure compilation required")
        for path in files:
            data = regular(path).read_bytes()
            if data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != 65:
                raise RuntimeError("receiver class must be Java21")
        class_records[cohort] = [{"path": p.relative_to(receiver / cohort).as_posix(),
                                 "bytes": p.stat().st_size, "sha256": sha256(p)} for p in files]
    baseline = regular(ARTIFACTS / "receiver-baseline.log").read_text(encoding="utf-8")
    negative = regular(ARTIFACTS / "receiver-negative.log").read_text(encoding="utf-8")
    corrected = regular(ARTIFACTS / "receiver-corrected.log").read_text(encoding="utf-8")
    for text in (baseline, corrected):
        if "PASS M3AutomatonDifferentialTest" not in text:
            raise RuntimeError("actual complete owning differential success marker required")
    if "lexicalValid=16 lexicalInvalid=32 lexicalFallback=11" not in corrected:
        raise RuntimeError("actual new lexical discriminator inventory required")
    if "INVALID_SYNTAX_ADMITTED: \\x４１" not in negative:
        raise RuntimeError("original compiler must reproduce the specific Unicode-hex bug")
    maven = regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8")
    if "[INFO] BUILD SUCCESS" not in maven:
        raise RuntimeError("actual SDK Maven success required")
    value = {
        "schema": "M3_REGEX_ASCII_LEXICAL_RECEIVER_HOSTED_QUALIFICATION_V1",
        "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
        "canonicalSourceCommit": record["sourceCommit"],
        "sourceBindingSha256": sha256(HERE / "source-binding.json"),
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256": sha256(sdk_receipt),
        "surefireXmlSha256": hashlib.sha256(xml).hexdigest(),
        "suite": SUITE, "methods": METHODS, "counts": counts,
        "compiledProviderClasses": providers, "emittedSourceOutputs": candidates,
        "unchangedReceiverCustody": module["RECEIVER_CUSTODY"],
        "receiverClassFiles": class_records,
        "receiverBaselineLogSha256": sha256(ARTIFACTS / "receiver-baseline.log"),
        "receiverNegativeLogSha256": sha256(ARTIFACTS / "receiver-negative.log"),
        "receiverCorrectedLogSha256": sha256(ARTIFACTS / "receiver-corrected.log"),
        "sdkRecipeJUnitQualified": True, "receiverDifferentialOnStockJava21Qualified": True,
        "negativeControlReproduced": True, "receiverPayloadRecipeReplayQualified": True,
        "originalPrivateOwningTestsExecuted": False, "runtimeApplied": False,
        "receiverCurrentOwners": record["receiverCurrentOwners"],
        "ownerPlanControls": record["ownerPlanControls"],
        "actualProductState": record["actualProductState"],
        "receiverSourceCustodySha256": sha256(source_path),
        "cohortSourceIdentities": expected_cohorts,
        "actualAppliedReceiverComponentQualified": record["actualProductState"] == "APPLIED",
    }
    value.update({key: False for key in FORBIDDEN})
    write("qualification-receipt.json", value)


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
              "receiverCurrentOwners": record["receiverCurrentOwners"],
              "actualProductState": record["actualProductState"],
              "ownerPlanControls": record["ownerPlanControls"],
              "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
