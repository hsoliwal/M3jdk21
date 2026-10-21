#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bind existing SDK sources and qualify actual recipe tests; no product application."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import runpy
import subprocess
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
RECEIVER = HERE.parents[2]
SDK = HERE / "sdk"
ROOT = Path(os.environ["M3_PROOF_ROOT"]).resolve()
ARTIFACTS = ROOT / "artifacts"
SUITE = "com.synexia.rewrite.M3RegexRelationsCurrentOwnerRecipeTest"
METHODS = ["allPartialStatesAndOrdersReachExactFixedPoint",
           "eachOwnedInputRefusesDriftMissingAndDuplicateOwnership",
           "currentOwnerMembersAndCustodyRemainExact"]
PRODUCT = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
BEFORE_SHA256 = "ca679153b93b33214e48c3d7cce4414cf9622ff380ee9aea3334c9a680f37779"
JTREG = "test/jdk/java/lang/String/M3StringHierarchicalRegexTest.java"
KEEP_PATHS = {"src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java",
              "src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java",
              "src/java.base/share/native/libjava/M3StateRelation.c"}
ENGINES = {
    "M3GpuToolJoinRecipe": "1c1b1b568d369f688f6953161636bf36b484bf5a",
    "M3GpuToolJoinManifest": "38df5f97bbeec8a8a8cfc77091d56bda925c07f2",
    "M3RegexRelationsCurrentOwnerRecipe": "c82f01cdf2c4524d86161ce3a2cdab68d5b5ad3b",
}


def blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def sha256(path):
    with regular(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def regular(path):
    if not path.is_file() or any(item.is_symlink() for item in [path, *path.parents]):
        raise RuntimeError("missing or redirected qualification input: " + str(path))
    return path


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
    binding_data = regular(HERE / "source-binding.json").read_bytes()
    if blob(binding_data) != git("rev-parse", "HEAD:m3/tooling/regex-relations-current-owner/source-binding.json"):
        raise RuntimeError("source binding itself differs from exact checkout")
    record = json.loads(binding_data)
    if (record["repository"] != "hsoliwal/com.synexia"
            or record["receiverRepository"] != "hsoliwal/M3jdk21"
            or record["runtimeApplied"] is not False or record["phaseAdvanced"] is not False):
        raise RuntimeError("source binding identity or scope drift")
    source_commit = record["sourceCommit"]
    if len(source_commit) != 40 or any(char not in "0123456789abcdef" for char in source_commit):
        raise RuntimeError("canonical source commit required")
    rows = record["files"]
    if not 23 <= len(rows) <= 32:
        raise RuntimeError("bounded complete SDK and hosted source binding required")
    sdk_relative = "m3/tooling/regex-relations-current-owner/sdk/"
    prefix = "m3/tooling/regex-relations-current-owner/"
    seen = set()
    for row in rows:
        path = str(relative(row["targetPath"]))
        if path in seen or not (path.startswith(prefix) or
                               path == ".github/workflows/m3-regex-relations-current-owner.yml"):
            raise RuntimeError("source binding ownership drift")
        seen.add(path)
        data = regular(RECEIVER / path).read_bytes()
        if blob(data) != row["gitBlob"] or len(data) != row["bytes"]:
            raise RuntimeError("canonical source input differs: " + path)
    tracked = set(git("ls-files", "-z", "--", sdk_relative, binary=True).decode("utf-8").split("\0"))
    tracked.discard("")
    if len(tracked) != 21 or {path for path in seen if path.startswith(sdk_relative)} != tracked:
        raise RuntimeError("exact complete 21-file SDK binding required")
    required = {prefix + "qualify.py", ".github/workflows/m3-regex-relations-current-owner.yml"}
    if not required <= seen:
        raise RuntimeError("hosted authoring inputs must be source-bound")
    subprocess.run(["git", "diff", "HEAD", "--exit-code", "--no-ext-diff", "--", prefix,
                    ".github/workflows/m3-regex-relations-current-owner.yml"],
                   cwd=RECEIVER, check=True, timeout=60)
    if fresh and ((SDK / "target").exists() or (ROOT / "jtreg-compile").exists()
                  or any((ROOT / "m2").rglob("*.jar"))):
        raise RuntimeError("SDK output and isolated Maven repository must start fresh")
    return record


def report():
    record = binding()
    # Existing SDK APIs own input, test-report and candidate-output validation.
    module = runpy.run_path(str(regular(SDK / "verify.py")), run_name="m3_regex_sdk_reuse")
    pins, pins_data = module["inputs"]()
    if module["SUITES"] != {SUITE: METHODS} or set(module["RECEIVER_OUTPUTS"]) != {PRODUCT, JTREG}:
        raise RuntimeError("actual existing SDK contract differs")
    receipt_path = regular(SDK / "target/sdk-receipt.json")
    receipt = json.loads(receipt_path.read_bytes())
    if receipt["scope"] != "CURRENT_OWNER_SOURCE_ONLY_TWO_OUTPUTS_THREE_KEEP_INPUTS":
        raise RuntimeError("SDK receipt scope drift")
    for name in ("fullJdkBuiltByThisTask", "jniRuntimeQualifiedByThisTask",
                 "productTargetAppliedByThisModule", "runtimeAppliedByThisTask", "phaseAdvanced"):
        if receipt[name] is not False:
            raise RuntimeError("SDK receipt exceeds source-only qualification")
    if (receipt["providers"] != pins["providers"]
            or receipt["inputPinsSha256"] != hashlib.sha256(pins_data).hexdigest()
            or receipt["emittedProductSourceOutputs"] != module["RECEIVER_OUTPUTS"]
            or receipt["unchangedReceiverCustody"] != module["RECEIVER_CUSTODY"]
            or set(receipt["unchangedReceiverCustody"]) != KEEP_PATHS):
        raise RuntimeError("SDK actual input or candidate receipt mismatch")
    reports = sorted((SDK / "target/surefire-reports").glob("TEST-*.xml"))
    if len(reports) != 1 or reports[0].name != "TEST-" + SUITE + ".xml":
        raise RuntimeError("one fresh actual three-method SDK suite required")
    xml = regular(reports[0]).read_bytes()
    counts = module["validate_report"](xml, SUITE, METHODS)
    parsed = ET.fromstring(xml)
    if parsed.get("name") != SUITE or receipt["reports"][SUITE]["counts"] != counts:
        raise RuntimeError("actual suite identity differs from SDK receipt")
    if receipt["reports"][SUITE]["sha256"] != hashlib.sha256(xml).hexdigest():
        raise RuntimeError("actual Surefire XML differs from SDK receipt")
    providers = []
    for name, expected in ENGINES.items():
        source = regular(SDK / ("pinned-engine/com/synexia/rewrite/" + name + ".java"))
        if blob(source.read_bytes()) != expected:
            raise RuntimeError("original provider source was replaced")
        compiled = regular(SDK / ("target/classes/com/synexia/rewrite/" + name + ".class"))
        data = compiled.read_bytes()
        if len(data) < 8 or data[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(data[6:8], "big") != 65:
            raise RuntimeError("actual provider Java21 class file required")
        providers.append({"class": "com.synexia.rewrite." + name, "sourceGitBlob": expected,
                          "classBytes": len(data), "classSha256": hashlib.sha256(data).hexdigest(),
                          "classMajor": 65})
    fixture_root = SDK / "src/main/resources/com/synexia/rewrite/m3-regex-relations-current-owner-20261017"
    before = regular(fixture_root / "before/M3StringRegexDp.java")
    if before.stat().st_size != 12888 or sha256(before) != BEFORE_SHA256:
        raise RuntimeError("current immutable BEFORE owner differs")
    emitted_root = SDK / "target/emitted/regex-relations-current-owner"
    actual = {path.relative_to(emitted_root).as_posix()
              for path in emitted_root.rglob("*") if path.is_file()}
    if actual != {PRODUCT, JTREG}:
        raise RuntimeError("two exact current-owner source outputs required")
    candidates = []
    for path, digest in module["RECEIVER_OUTPUTS"].items():
        emitted = regular(emitted_root / path)
        counterpart = regular(fixture_root / "out" / PurePosixPath(path).name)
        if sha256(emitted) != digest or emitted.read_bytes() != counterpart.read_bytes():
            raise RuntimeError("actual emitted source differs from pinned recipe output")
        candidates.append({"path": path, "bytes": emitted.stat().st_size, "sha256": digest,
                           "verifiedInJob": True, "exported": False})
    compiled_test = regular(ROOT / "jtreg-compile/M3StringHierarchicalRegexTest.class")
    test_class = compiled_test.read_bytes()
    if len(test_class) < 8 or test_class[:4] != b"\xca\xfe\xba\xbe" or int.from_bytes(test_class[6:8], "big") != 65:
        raise RuntimeError("actual stock-JDK compile-only jtreg class required")
    inputs_log = regular(ARTIFACTS / "sdk-inputs.log").read_text(encoding="utf-8")
    maven_log = regular(ARTIFACTS / "sdk-maven.log").read_text(encoding="utf-8")
    if ("M3_REGEX_RELATIONS_CURRENT_OWNER_SUREFIRE_SELFTEST_PASS" not in inputs_log
            or "M3_REGEX_RELATIONS_CURRENT_OWNER_RECEIVING_CLOSURE_SELFTEST_PASS" not in inputs_log
            or "[INFO] BUILD SUCCESS" not in maven_log):
        raise RuntimeError("actual existing SDK and Maven command evidence required")
    write("qualification-receipt.json", {
        "schema": "M3_REGEX_RELATIONS_HOSTED_SDK_QUALIFICATION_V1",
        "checkoutCommit": os.environ["EXPECTED_SOURCE_SHA"],
        "canonicalSourceCommit": record["sourceCommit"],
        "ciReportedSha": receipt["ciReportedSha"],
        "sourceBindingSha256": sha256(HERE / "source-binding.json"),
        "inputPinsSha256": hashlib.sha256(pins_data).hexdigest(),
        "sdkReceiptSha256": sha256(receipt_path),
        "surefireXmlSha256": hashlib.sha256(xml).hexdigest(),
        "suite": SUITE, "methods": METHODS, "counts": counts,
        "compiledProviderClasses": providers,
        "currentDpBefore": {"bytes": 12888, "sha256": BEFORE_SHA256},
        "sourceOutputs": candidates,
        "jtregSourceCompiledOnly": {"classBytes": len(test_class),
             "classSha256": hashlib.sha256(test_class).hexdigest(), "classMajor": 65,
             "compilerLogSha256": sha256(ARTIFACTS / "jtreg-compile.log"),
             "executed": False, "matchedJdkExecutionQualified": False},
        "unchangedKeepInputs": receipt["unchangedReceiverCustody"],
        "sdkRecipeJUnitQualified": True, "dpProductSourceRecipeReplayQualified": True,
        "dpProductJavaCompilationQualified": False,
        "originalPrivateModelQualified": False, "runtimeRegexQualified": False,
        "jniRuntimeQualified": False, "fullMatchedJdkQualified": False,
        "performanceRetentionQualified": False, "productTargetApplied": False,
        "phaseAdvanced": False,
    })


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
              "boundFiles": len(record["files"]), "sdkFiles": 21,
              "runtimeApplied": False, "phaseAdvanced": False})
    else:
        report()
