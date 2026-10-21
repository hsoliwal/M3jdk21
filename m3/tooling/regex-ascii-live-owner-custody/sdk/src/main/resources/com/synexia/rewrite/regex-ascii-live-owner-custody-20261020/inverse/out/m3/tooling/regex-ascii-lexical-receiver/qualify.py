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
    if fresh and ((SDK / "target").exists() or (ROOT / "receiver").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output, receiver classes and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
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
              "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
