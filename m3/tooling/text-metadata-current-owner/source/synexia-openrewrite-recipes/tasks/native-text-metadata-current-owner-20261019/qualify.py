#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Compose source seals and unchanged Java/C11 JNI proof under the caller's existing supervisor."""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

TASK = Path(__file__).resolve().parent
SOURCE = TASK.parents[2]
SDK = TASK / "guard-sdk"
CURRENT = SDK / "src/main/resources/com/synexia/rewrite/text-metadata-current-owner-20261019"
OLD = SOURCE / "synexia-openrewrite-recipes/tasks/composite-summary-20261012"
RESERVE, CAP, LOG_LIMIT = 8 * 1024 ** 3, 512 * 1024 ** 2, 1024 ** 2
TEST_CLASS = "com.synexia.rewrite.M3TextMetadataCurrentOwnerRecipeTest"
METHODS = {"namedCurrentTupleReplaysAsExactFixedPointInBothOrders",
           "historicalRecipeAndArchivesRetainTheirOriginalAdmissionBoundary",
           "refusesUnknownMixedMissingAndDuplicateOwnersWithoutMutation"}
JAVA = SOURCE / "synexia-mindex/runtime/src/main/java/com/synexia/mindex/TextMetadata.java"
C = SOURCE / "synexia-mindex/native-jni/src/mindex_text_metadata_jni.c"
PROOF = OLD / "proof/TextMetadataProof.java"


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def safe_file(base, relative):
    value = PurePosixPath(relative)
    require(not value.is_absolute() and value.parts
            and all(part not in ("", ".", "..") for part in value.parts), "UNSAFE_INPUT_PATH")
    path = base.joinpath(*value.parts)
    parents = [base, *[base.joinpath(*value.parts[:n]) for n in range(1, len(value.parts))]]
    require(all(p.is_dir() and not p.is_symlink() for p in parents), "INPUT_PARENT_UNSAFE:" + relative)
    require(path.is_file() and not path.is_symlink(), "INPUT_MISSING_OR_UNSAFE:" + relative)
    return path


def capacity(root):
    free = shutil.disk_usage(root).free
    require(free >= RESERVE, "STORAGE_RESERVE_BELOW_8_GIB")
    return free


def context(value):
    require(value and chr(36) + "{" not in value, "EXPLICIT_QUALIFICATION_ROOT_REQUIRED")
    root = Path(value)
    require(root.is_absolute() and root.exists() and root == root.resolve(),
            "CANONICAL_EXISTING_QUALIFICATION_ROOT_REQUIRED")
    require(not root.is_symlink(), "QUALIFICATION_ROOT_SYMLINK")
    require(os.environ.get("M3_SOURCE_ROOT") == str(SOURCE), "EXPLICIT_SOURCE_ROOT_MISMATCH")
    require(SOURCE.is_dir() and SOURCE == SOURCE.resolve()
            and all(not p.is_symlink() for p in [SOURCE, *SOURCE.parents]), "SOURCE_ROOT_UNSAFE")
    require(root != SOURCE and not root.is_relative_to(SOURCE), "OUTPUT_ROOT_IS_SOURCE")
    for name in ("artifacts", "tmp", "home", "cache"):
        path = root / name
        require(path.is_dir() and not path.is_symlink(), "PROVISIONED_OUTPUT_DIRECTORY_REQUIRED:" + name)
    require(os.environ.get("M3_QUALIFICATION_ROOT") == str(root), "ROOT_ENVIRONMENT_MISMATCH")
    ident = os.environ.get("M3_QUALIFICATION_ID", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", ident), "FRESH_QUALIFICATION_ID_REQUIRED")
    for name in ("TMPDIR", "TEMP", "TMP"):
        require(os.environ.get(name) == str(root / "tmp"), "EXPLICIT_TEMP_ROUTING_REQUIRED:" + name)
    require(os.environ.get("HOME") == str(root / "home"), "EXPLICIT_HOME_ROUTING_REQUIRED")
    capacity(root)
    return root, ident


def inputs():
    metadata = json.loads((TASK / "INPUTS.json").read_text(encoding="utf-8"))
    require(metadata["schema"] == 1 and metadata["capBytes"] == CAP
            and metadata["reserveBytes"] == RESERVE, "INPUT_POLICY_DRIFT")
    rows = metadata["files"]
    require(len({row["path"] for row in rows}) == len(rows), "DUPLICATE_INPUT_PIN")
    result = {}
    for row in rows:
        data = safe_file(SOURCE, row["path"]).read_bytes()
        actual = {"bytes": len(data), "sha256": sha(data), "gitBlob": git_blob(data)}
        require(all(actual[key] == row[key] for key in actual), "SOURCE_PIN_DRIFT:" + row["path"])
        result[row["path"]] = actual
    require(sum(path.stat().st_size for path in (JAVA, C, PROOF)) == 17706,
            "UNCHANGED_DIRECT_CLOSURE_MUST_BE_17706_BYTES")
    return metadata, result


def read_receipt(root, ident):
    path = root / "artifacts/text-metadata-guard-sdk.json"
    require(path.is_file() and not path.is_symlink(), "FRESH_ACTUAL_SDK_RECEIPT_REQUIRED")
    raw = path.read_bytes()
    require(len(raw) < 16384, "SDK_RECEIPT_TOO_LARGE")
    receipt = json.loads(raw)
    require(receipt["schema"] == 1 and receipt["state"] == "PASS"
            and receipt["qualificationId"] == ident
            and receipt["scope"] == "ACTUAL_NAMED_OPENREWRITE_SOURCE_CUSTODY_ONLY"
            and receipt["successfulSdkRuns"] == 8 and receipt["refusedSdkRuns"] == 26
            and receipt["tests"] == 3, "SDK_RECEIPT_SCOPE_MISMATCH")
    require(receipt["nativeExecuted"] is False and receipt["runtimeApplied"] is False
            and receipt["phaseAdvanced"] is False, "SDK_RECEIPT_MUST_NOT_PROMOTE_RUNTIME")
    require(receipt["currentManifestSha256"] == sha((CURRENT / "manifest.tsv").read_bytes()),
            "SDK_CURRENT_MANIFEST_MISMATCH")
    require(receipt["historicalManifestSha256"] == sha((OLD / "templates/manifest.tsv").read_bytes()),
            "SDK_HISTORICAL_MANIFEST_MISMATCH")
    descriptor = SDK / "src/main/resources/META-INF/rewrite/text-metadata-current-owner.yml"
    require(receipt["descriptorSha256"] == sha(descriptor.read_bytes()), "SDK_DESCRIPTOR_MISMATCH")
    report = SDK / ("target/surefire-reports/TEST-" + TEST_CLASS + ".xml")
    require(report.is_file() and not report.is_symlink(), "ACTUAL_SUREFIRE_REPORT_REQUIRED")
    report_bytes = report.read_bytes()
    require(len(report_bytes) < LOG_LIMIT, "SUREFIRE_REPORT_TOO_LARGE")
    suite = ET.fromstring(report_bytes)
    require(suite.tag == "testsuite" and suite.attrib.get("name") == TEST_CLASS,
            "WRONG_SUREFIRE_SUITE")
    for name, expected in (("tests", 3), ("failures", 0), ("errors", 0), ("skipped", 0)):
        require(int(suite.attrib[name]) == expected, "SUREFIRE_COUNT_MISMATCH:" + name)
    cases = suite.findall("testcase")
    require(len(cases) == 3 and {case.attrib["name"] for case in cases} == METHODS,
            "ALL_THREE_NAMED_METHODS_MUST_RUN")
    require(all(case.attrib["classname"] == TEST_CLASS
                and all(child.tag in {"system-out", "system-err"} for child in case)
                for case in cases), "SUREFIRE_FAILURE_SKIP_OR_EXTRA_CASE_DATA")
    return {"path": str(path.relative_to(root)), "bytes": len(raw), "sha256": sha(raw),
            "reportRoot": "SOURCE", "report": str(report.relative_to(SOURCE)), "reportBytes": len(report_bytes),
            "reportSha256": sha(report_bytes), "methods": sorted(METHODS),
            "tests": 3, "failures": 0, "errors": 0, "skipped": 0}


def environment(root):
    child = dict(os.environ)
    child.update(HOME=str(root / "home"), TMPDIR=str(root / "tmp"),
                 TEMP=str(root / "tmp"), TMP=str(root / "tmp"),
                 XDG_CACHE_HOME=str(root / "cache"), PYTHONDONTWRITEBYTECODE="1",
                 UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1")
    return child


def version_command(argv, env):
    completed = subprocess.run(argv, stdin=subprocess.DEVNULL, capture_output=True,
                               timeout=15, env=env, check=False)
    raw = completed.stdout + completed.stderr
    require(completed.returncode == 0 and len(raw) < 65536, "PROVISIONED_TOOLCHAIN_PROBE_FAILED")
    return raw.decode("utf-8", errors="strict")


def toolchain(root, native):
    env = environment(root)
    require(sys.platform.startswith("linux"), "FOCUSED_PROOF_REQUIRES_LINUX")
    home = os.environ.get("JAVA_HOME")
    require(home, "PROVISIONED_JAVA_HOME_REQUIRED_NO_FALLBACK")
    jdk = Path(home).resolve()
    java, javac = jdk / "bin/java", jdk / "bin/javac"
    require(java.is_file() and javac.is_file(), "PROVISIONED_JDK_REQUIRED")
    for name, actual in (("java", java), ("javac", javac)):
        selected = shutil.which(name)
        require(selected and Path(selected).resolve() == actual.resolve(), "PATH_JDK_MISMATCH:" + name)
    java_options = ["-XX:+PerfDisableSharedMem", "-Djava.io.tmpdir=" + str(root / "tmp"),
                    "-Duser.home=" + str(root / "home")]
    java_version = version_command([str(java), *java_options, "-version"], env)
    javac_version = version_command([str(javac), "-J-XX:+PerfDisableSharedMem",
                                     "-J-Djava.io.tmpdir=" + str(root / "tmp"),
                                     "-J-Duser.home=" + str(root / "home"), "-version"], env)
    require(re.search(r'(?:openjdk|java) version "21(?:[."]|$)', java_version),
            "NATIVE_JDK_21_HOST_REQUIRED")
    require(re.search(r"\bjavac 21(?:[.\s]|$)", javac_version), "JAVAC_21_REQUIRED")
    result = {"java": str(java), "javac": str(javac), "javaVersion": java_version,
              "javacVersion": javac_version, "hostJdk21Verified": True,
              "externalToolchainBytesSealed": False}
    cc = None
    if native:
        cc_value = os.environ.get("CC", "gcc")
        require(not any(c.isspace() for c in cc_value), "CC_MUST_NAME_ONE_PROVISIONED_EXECUTABLE")
        selected = shutil.which(cc_value)
        require(selected, "PROVISIONED_GCC_REQUIRED_NO_DOWNLOAD_FALLBACK")
        cc = str(Path(selected).resolve())
        gcc_version = version_command([cc, "--version"], env)
        require("gcc" in gcc_version.splitlines()[0].lower(), "GCC_REQUIRED")
        machine = version_command([cc, "-dumpmachine"], env).strip()
        require("linux" in machine, "LINUX_GCC_TARGET_REQUIRED")
        sanitizer = version_command([cc, "-print-file-name=libubsan.so"], env).strip()
        require(Path(sanitizer).is_absolute() and Path(sanitizer).is_file(),
                "PROVISIONED_UBSAN_RUNTIME_REQUIRED")
        require((jdk / "include/jni.h").is_file() and (jdk / "include/linux/jni_md.h").is_file(),
                "PROVISIONED_JDK_JNI_HEADERS_REQUIRED")
        result.update(gcc=cc, gccVersion=gcc_version, gccTarget=machine, ubsanRuntime=sanitizer)
    return env, java, javac, jdk, cc, java_options, result


def write(root, name, data):
    path = root / "artifacts" / name
    require(not path.exists() and not path.is_symlink(), "FRESH_ARTIFACT_REQUIRED:" + name)
    raw = (json.dumps(data, indent=2, sort_keys=True) + "\n").encode("utf-8")
    require(len(raw) <= LOG_LIMIT, "RECEIPT_TOO_LARGE")
    with path.open("xb") as stream:
        stream.write(raw)
    return {"path": "artifacts/" + name, "bytes": len(raw), "sha256": sha(raw)}


def command(root, label, argv, env, expected=0, timeout=90):
    capacity(root)
    path = root / "artifacts" / (label + ".log")
    require(not path.exists() and not path.is_symlink(), "FRESH_COMMAND_LOG_REQUIRED:" + label)
    started = time.time()
    with path.open("xb") as log:
        process = subprocess.Popen(argv, cwd=SOURCE, stdin=subprocess.DEVNULL,
                                   stdout=log, stderr=subprocess.STDOUT, env=env,
                                   start_new_session=False)
        try:
            code = process.wait(timeout=timeout)
        except BaseException:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=10)
            raise
    capacity(root)
    raw = path.read_bytes()
    require(len(raw) <= LOG_LIMIT, "COMMAND_LOG_EXCEEDS_1_MIB:" + label)
    require((code == 0) if expected == 0 else (code != 0), "COMMAND_EXIT:" + label + ":" + str(code))
    return {"label": label, "argv": [str(x) for x in argv], "exitCode": code,
            "startedUnix": started, "finishedUnix": time.time(),
            "log": str(path.relative_to(root)), "bytes": len(raw), "sha256": sha(raw)}, raw


def classes(directory, expected):
    actual = {path.relative_to(directory).as_posix(): path for path in directory.rglob("*.class")}
    require(set(actual) == set(expected), "EXACT_FOCUSED_CLASS_INVENTORY_REQUIRED")
    rows = []
    for name in sorted(actual):
        raw = actual[name].read_bytes()
        require(raw[:4] == b"\xca\xfe\xba\xbe" and int.from_bytes(raw[6:8], "big") == 65,
                "JAVA_21_CLASS_REQUIRED:" + name)
        rows.append({"path": name, "bytes": len(raw), "sha256": sha(raw), "major": 65})
    return rows


def execute(root, ident, native):
    metadata, before = inputs()
    sdk = read_receipt(root, ident)
    env, java, javac, jdk, cc, java_options, tools = toolchain(root, native)
    work = root / "text-metadata-native"
    require(not work.exists() and not work.is_symlink(), "FRESH_NATIVE_OUTPUT_ROOT_REQUIRED")
    replay_classes, java_classes, headers = [work / name for name in ("replay-classes", "classes", "headers")]
    for directory in (replay_classes, java_classes, headers):
        directory.mkdir(parents=True, exist_ok=False)
    commands = []
    manifest_owner = SOURCE / "synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3GpuToolJoinManifest.java"
    replay_owner = SOURCE / "synexia-openrewrite-recipes/tasks/recipe-qualification/src/main/java/com/synexia/rewrite/Replay.java"
    javac_options = ["-J-XX:+PerfDisableSharedMem", "-J-Djava.io.tmpdir=" + str(root / "tmp"),
                     "-J-Duser.home=" + str(root / "home"), "--release", "21", "-Xlint:all", "-Werror"]
    row, _ = command(root, "text-metadata-replay-compile",
                     [str(javac), *javac_options, "-d", str(replay_classes),
                      str(manifest_owner), str(replay_owner)], env)
    commands.append(row)
    replay = [str(java), *java_options, "-cp", str(replay_classes), "com.synexia.rewrite.Replay"]
    row, raw = command(root, "text-metadata-current-replay",
                       [*replay, "--check", str(SOURCE), str(CURRENT)], env)
    require(b"changes=0 applied=false" in raw, "CURRENT_KEEP_REPLAY_MUST_BE_ZERO_WRITE")
    commands.append(row)
    row, raw = command(root, "text-metadata-historical-refusal",
                       [*replay, "--check", str(SOURCE), str(OLD / "templates")], env, expected=1)
    require(b"source drift: synexia-mindex/native-jni/CMakeLists.txt" in raw,
            "HISTORICAL_CURRENT_TUPLE_REFUSAL_REQUIRED")
    commands.append(row)
    row, _ = command(root, "text-metadata-java-compile",
                     [str(javac), *javac_options, "-h", str(headers),
                      "-d", str(java_classes), str(JAVA), str(PROOF)], env)
    commands.append(row)
    inventory = classes(java_classes, {
        "com/synexia/mindex/TextMetadata.class", "com/synexia/mindex/TextMetadata$Summary.class",
        "com/synexia/mindex/TextMetadataProof.class", "com/synexia/mindex/TextMetadataProof$1.class",
    })
    generated = headers / "com_synexia_mindex_TextMetadata.h"
    require(generated.is_file() and not generated.is_symlink(), "ACTUAL_GENERATED_JNI_HEADER_REQUIRED")
    generated_raw = generated.read_bytes()
    require(b"Java_com_synexia_mindex_TextMetadata_nativeConcat" in generated_raw
            and b"(JNIEnv *, jclass, jintArray)" in generated_raw, "JNI_HEADER_SIGNATURE_MISMATCH")
    header_path = root / "artifacts/text-metadata-generated-jni.h"
    with header_path.open("xb") as stream:
        stream.write(generated_raw)
    run_java = [str(java), *java_options, "-Xcheck:jni", "-cp", str(java_classes),
                "com.synexia.mindex.TextMetadataProof"]
    row, raw = command(root, "text-metadata-java-proof", run_java, env)
    require(raw.decode("utf-8").splitlines().count(
        "TEXT_METADATA_PASS native=false layouts=76416 checks=309986") == 1,
        "ACTUAL_COMPLETE_JAVA_PROOF_MARKER_REQUIRED")
    commands.append(row)
    library_receipt = None
    if native:
        library = work / "libm3_summary_proof.so"
        row, _ = command(root, "text-metadata-c11-compile",
                         [cc, "-std=c11", "-Wall", "-Wextra", "-Wpedantic", "-Werror",
                          "-Wconversion", "-Wsign-conversion", "-fPIC", "-shared",
                          "-fsanitize=undefined", "-fno-sanitize-recover=all",
                          "-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux"),
                          "-I" + str(headers), "-include", generated.name, str(C), "-o", str(library)], env)
        commands.append(row)
        require(library.is_file() and not library.is_symlink(), "ACTUAL_JNI_LIBRARY_REQUIRED")
        binary = library.read_bytes()
        require(binary[:4] == b"\x7fELF", "LINUX_SHARED_LIBRARY_REQUIRED")
        library_receipt = {"bytes": len(binary), "sha256": sha(binary), "generatedHeaderForced": True,
                           "cStandard": "c11", "fatalUndefinedBehaviorSanitizer": True,
                           "exportedAsArtifact": False}
        row, raw = command(root, "text-metadata-native-proof", [*run_java, str(library)], env)
        require(raw.decode("utf-8").splitlines().count(
            "TEXT_METADATA_PASS native=true layouts=76416 checks=386819") == 1,
            "REQUIRED_NATIVE_COMPLETE_PROOF_MARKER_MISSING")
        commands.append(row)
        missing = work / "must-not-exist.so"
        require(not missing.exists(), "MISSING_LIBRARY_CONTROL_OCCUPIED")
        row, raw = command(root, "text-metadata-missing-library-refusal",
                           [*run_java, str(missing)], env, expected=1)
        require(b"UnsatisfiedLinkError" in raw and b"TEXT_METADATA_PASS" not in raw,
                "MISSING_NATIVE_MUST_FAIL_WITHOUT_FALLBACK")
        commands.append(row)
        require(library.read_bytes() == binary, "JNI_LIBRARY_CHANGED_AFTER_EXECUTION")
    _, after = inputs()
    require(before == after, "SOURCE_OR_ARCHIVE_CHANGED")
    require(inventory == classes(java_classes, {row["path"] for row in inventory}),
            "CLASSES_CHANGED_AFTER_EXECUTION")
    custody = write(root, "text-metadata-source-custody.json",
                    {"schema": 1, "qualificationId": ident, "before": before, "after": after,
                     "unchanged": True, "inputManifestSha256": sha((TASK / "INPUTS.json").read_bytes()),
                     "directProofSourceBytes": 17706, "historicalArchivesChanged": False})
    receipt = {
        "schema": 1, "qualificationId": ident, "state": "PASS_NATIVE" if native else "JAVA_ONLY",
        "scope": "EXACT_TEXT_METADATA_JAVA_C11_JNI_DIFFERENTIAL" if native else "JAVA_ONLY",
        "sdk": sdk, "sourceCustody": custody, "commands": commands, "tools": tools,
        "classes": inventory, "generatedJniHeader": {"path": str(header_path.relative_to(root)),
            "bytes": len(generated_raw), "sha256": sha(generated_raw)}, "nativeLibrary": library_receipt,
        "layouts": 76416, "javaChecks": 309986, "nativeChecks": 386819 if native else 0,
        "requiredNative": native, "nativeExecuted": native, "nativeSkips": 0,
        "explicitLibraryLoad": native, "xcheckJni": True,
        "storage": {"capBytes": CAP, "reserveBytes": RESERVE,
                    "externalAggregateAndProcessSupervisorRequired": True,
                    "selfEnforcedAggregateClaim": False, "finalFreeBytes": capacity(root)},
        "sourceBase": metadata["sourceBase"], "sourceBindingAndExternalByteSealing": "SEPARATE",
        "runtimeApplied": False, "phaseAdvanced": False, "fullNativeLibraryQualified": False,
        "cmakeIntegrationQualified": False, "compositeBehaviorQualified": False,
        "privateSourceForkReactorQualified": False, "fullJdkQualified": False,
        "windowsQualified": False, "performanceQualified": False,
    }
    output = write(root, "text-metadata-execution.json", receipt)
    print(json.dumps({"state": receipt["state"], "nativeExecuted": native, "receipt": output}, sort_keys=True))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-root", required=True)
    parser.add_argument("--inputs-only", action="store_true")
    parser.add_argument("--jni", action="store_true")
    args = parser.parse_args()
    require(not (args.inputs_only and args.jni), "INPUTS_ONLY_CANNOT_CLAIM_NATIVE")
    root, ident = context(args.output_root)
    if args.inputs_only:
        _, found = inputs()
        require(not (root / "artifacts/text-metadata-guard-sdk.json").exists(),
                "SDK_RECEIPT_ALREADY_EXISTS_NEW_QUALIFICATION_REQUIRED")
        print(json.dumps({"state": "INPUTS_ONLY", "files": len(found), "qualificationId": ident,
                          "executedSdk": False, "nativeExecuted": False}))
    else:
        execute(root, ident, args.jni)


if __name__ == "__main__":
    main()
