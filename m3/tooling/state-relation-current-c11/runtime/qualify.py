#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Qualify the unchanged five-owner relation proof under an existing bounded supervisor."""
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

TASK = Path(__file__).resolve().parent
RESERVE, CAP, LOG_LIMIT = 8 * 1024 ** 3, 512 * 1024 ** 2, 1024 ** 2
OWNER = "src/java.base/share/classes/jdk/internal/mindex/M3StateRelation.java"
ADAPTER = "src/java.base/share/classes/jdk/internal/mindex/M3DotStarRelations.java"
NATIVE = "src/java.base/share/native/libjava/M3StateRelation.c"
HEADER = "src/java.base/share/native/libjava/m3_state_relation.h"
PROOF = "m3/compatibility/regex-relations-20261014/M3RelationPortProof.java"
PINNED = {
    OWNER: "ea904c0962fb976f43b45575f06f46ad198e29a2",
    ADAPTER: "780c7887aaa6b0892a76f4eee1dcb17086a60289",
    NATIVE: "610ca8bab47065a49b9b58814d6392a287f4fd8b",
    HEADER: "18a02e228129827da486edc5b316790af6135310",
    PROOF: "e9a6beee8916cf97d5d4aac2383e7ac2fa8843e0",
}
MAIN = "jdk.internal.mindex.M3RelationPortProof"
JAVA_MARKER = "M3_RELATION_PORT cases=71748 checks=74829 native=false PASS"
JNI_MARKER = "M3_RELATION_PORT cases=71748 checks=149658 native=true PASS"
REUSE_MARKER = "REUSE logicalUnits=2097152 reads=2 summaries=21 joins=20 hits=20"
JNI_EXPORTS = {
    "Java_jdk_internal_mindex_M3StateRelation_composeNative",
    "Java_jdk_internal_mindex_M3StateRelation_repeatNative",
}
CLASS_NAMES = {
    "M3StateRelation", "M3DotStarRelations", "M3DotStarRelations$Access",
    "M3DotStarRelations$Join", "M3DotStarRelations$Result", "M3DotStarRelations$Engine",
    "M3RelationPortProof", "M3RelationPortProof$Node",
    "M3RelationPortProof$View", "M3RelationPortProof$1",
}
EXPECTED_CLASSES = {"jdk/internal/mindex/" + name + ".class" for name in CLASS_NAMES}


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git_blob(data):
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def real_directory(path, label):
    require(path.is_dir() and path == path.resolve()
            and all(not part.is_symlink() for part in [path, *path.parents]),
            "REAL_CANONICAL_DIRECTORY_REQUIRED:" + label)


def safe_file(base, relative):
    value = PurePosixPath(relative)
    require(not value.is_absolute() and value.parts and "\\" not in relative
            and all(part not in ("", ".", "..") for part in value.parts),
            "UNSAFE_INPUT_PATH")
    path = base.joinpath(*value.parts)
    for size in range(len(value.parts)):
        parent = base.joinpath(*value.parts[:size])
        require(parent.is_dir() and not parent.is_symlink(), "INPUT_PARENT_UNSAFE:" + relative)
    require(path.is_file() and not path.is_symlink(), "INPUT_MISSING_OR_UNSAFE:" + relative)
    return path


def capacity(root):
    free = shutil.disk_usage(root).free
    require(free >= RESERVE, "STORAGE_RESERVE_BELOW_8_GIB")
    return free


def context(value):
    require(value and chr(36) + "{" not in value, "EXPLICIT_QUALIFICATION_ROOT_REQUIRED")
    root = Path(value)
    require(root.is_absolute(), "ABSOLUTE_OUTPUT_ROOT_REQUIRED")
    real_directory(root, "OUTPUT")
    source_value = os.environ.get("M3_SOURCE_ROOT", "")
    require(source_value and chr(36) + "{" not in source_value, "EXPLICIT_SOURCE_ROOT_REQUIRED")
    source = Path(source_value)
    require(source.is_absolute(), "ABSOLUTE_SOURCE_ROOT_REQUIRED")
    real_directory(source, "SOURCE")
    require(root != source and not root.is_relative_to(source)
            and not source.is_relative_to(root), "SOURCE_OUTPUT_ROOTS_MUST_BE_SEPARATE")
    for name in ("artifacts", "tmp", "home", "cache"):
        real_directory(root / name, name)
    require(os.environ.get("M3_QUALIFICATION_ROOT") == str(root), "ROOT_ENVIRONMENT_MISMATCH")
    ident = os.environ.get("M3_QUALIFICATION_ID", "")
    require(re.fullmatch(r"[A-Za-z0-9_.-]{16,128}", ident), "FRESH_QUALIFICATION_ID_REQUIRED")
    for name in ("TMPDIR", "TEMP", "TMP"):
        require(os.environ.get(name) == str(root / "tmp"), "EXPLICIT_TEMP_ROUTING_REQUIRED:" + name)
    require(os.environ.get("HOME") == str(root / "home"), "EXPLICIT_HOME_ROUTING_REQUIRED")
    require(os.environ.get("XDG_CACHE_HOME") == str(root / "cache"), "EXPLICIT_CACHE_ROUTING_REQUIRED")
    capacity(root)
    return root, source, ident


def inputs(source):
    real_directory(TASK, "TASK")
    metadata_file = safe_file(TASK, "INPUTS.json")
    raw = metadata_file.read_bytes()
    require(len(raw) < 32768, "INPUT_MANIFEST_TOO_LARGE")
    metadata = json.loads(raw)
    require(metadata["schema"] == 1 and metadata["capability"] == "state-relation-current-c11-20261019"
            and metadata["capBytes"] == CAP and metadata["reserveBytes"] == RESERVE,
            "INPUT_POLICY_DRIFT")
    rows = metadata["files"]
    require(len(rows) == 5 and {row["path"] for row in rows} == set(PINNED), "EXACT_FIVE_INPUTS_REQUIRED")
    require(metadata["expected"] == {
        "cases": 71748, "javaChecks": 74829, "nativeChecks": 149658,
        "javaMarker": JAVA_MARKER, "nativeMarker": JNI_MARKER, "reuseMarker": REUSE_MARKER,
    }, "COMPLETE_PROOF_EXPECTATION_DRIFT")
    result = {}
    for row in rows:
        require(row["gitBlob"] == PINNED[row["path"]] and row["license"] == "Apache-2.0",
                "CANONICAL_INPUT_IDENTITY_DRIFT:" + row["path"])
        path = safe_file(source, row["path"])
        require(path.stat().st_size == row["bytes"] and row["bytes"] < 131072,
                "SOURCE_LENGTH_DRIFT:" + row["path"])
        data = path.read_bytes()
        actual = {"bytes": len(data), "sha256": sha(data), "gitBlob": git_blob(data)}
        require(all(actual[key] == row[key] for key in actual), "SOURCE_PIN_DRIFT:" + row["path"])
        result[row["path"]] = actual
    require(sum(row["bytes"] for row in result.values()) == 28948, "FIVE_OWNER_CLOSURE_BYTES_DRIFT")
    return metadata, result, sha(raw)


def environment(root):
    child = dict(os.environ)
    child.update(HOME=str(root / "home"), TMPDIR=str(root / "tmp"),
                 TEMP=str(root / "tmp"), TMP=str(root / "tmp"),
                 XDG_CACHE_HOME=str(root / "cache"), PYTHONDONTWRITEBYTECODE="1",
                 UBSAN_OPTIONS="halt_on_error=1:print_stacktrace=1")
    # Hidden Java/GCC injection must not change the requested compiler or JVM command.
    for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS",
                "CLASSPATH", "CPATH", "C_INCLUDE_PATH", "LIBRARY_PATH", "GCC_EXEC_PREFIX"):
        require(not child.get(key), "UNDECLARED_TOOLCHAIN_OPTION:" + key)
    return child


def version_command(argv, env):
    completed = subprocess.run(argv, stdin=subprocess.DEVNULL, capture_output=True,
                               timeout=15, env=env, check=False, start_new_session=False)
    raw = completed.stdout + completed.stderr
    require(completed.returncode == 0 and len(raw) < 65536, "PROVISIONED_TOOLCHAIN_PROBE_FAILED")
    return raw.decode("utf-8", errors="strict")


def toolchain(root):
    env = environment(root)
    require(sys.platform.startswith("linux"), "FOCUSED_PROOF_REQUIRES_LINUX")
    home = os.environ.get("JAVA_HOME", "")
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
        "-J-Djava.io.tmpdir=" + str(root / "tmp"), "-J-Duser.home=" + str(root / "home"), "-version"], env)
    require(re.search(r'(?:openjdk|java) version "21(?:[."]|$)', java_version), "HOST_JDK_21_REQUIRED")
    require(re.search(r"\bjavac 21(?:[.\s]|$)", javac_version), "JAVAC_21_REQUIRED")
    cc_value = os.environ.get("CC", "gcc")
    require(cc_value and not any(char.isspace() for char in cc_value),
            "CC_MUST_NAME_ONE_PROVISIONED_EXECUTABLE")
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
    nm_selected = shutil.which("nm")
    require(nm_selected, "PROVISIONED_NM_REQUIRED")
    nm = str(Path(nm_selected).resolve())
    result = {"java": str(java), "javac": str(javac), "javaVersion": java_version,
              "javacVersion": javac_version, "gcc": cc, "gccVersion": gcc_version,
              "gccTarget": machine, "ubsanRuntime": sanitizer, "nm": nm,
              "nmVersion": version_command([nm, "--version"], env),
              "hostJdk21Verified": True, "externalToolchainBytesSealed": False}
    return env, java, javac, jdk, cc, nm, java_options, result


def write(root, name, data):
    require(re.fullmatch(r"[a-z0-9-]+\.json", name), "FIXED_JSON_BASENAME_REQUIRED")
    real_directory(root / "artifacts", "ARTIFACTS")
    path = root / "artifacts" / name
    require(not path.exists() and not path.is_symlink(), "FRESH_ARTIFACT_REQUIRED:" + name)
    raw = (json.dumps(data, indent=2, sort_keys=True) + "\n").encode("utf-8")
    require(len(raw) <= LOG_LIMIT, "RECEIPT_TOO_LARGE")
    with path.open("xb") as stream:
        stream.write(raw)
    return {"path": "artifacts/" + name, "bytes": len(raw), "sha256": sha(raw)}


def command(root, source, label, argv, env, expected=0, timeout=120):
    capacity(root)
    real_directory(root / "artifacts", "ARTIFACTS")
    require(re.fullmatch(r"[a-z0-9-]+", label), "FIXED_LOG_BASENAME_REQUIRED")
    path = root / "artifacts" / (label + ".log")
    require(not path.exists() and not path.is_symlink(), "FRESH_COMMAND_LOG_REQUIRED:" + label)
    started = time.time()
    with path.open("xb") as log:
        process = subprocess.Popen(argv, cwd=source, stdin=subprocess.DEVNULL,
            stdout=log, stderr=subprocess.STDOUT, env=env, start_new_session=False)
        try:
            code = process.wait(timeout=timeout)
        except BaseException:
            if process.poll() is None:
                process.kill()
                process.wait(timeout=10)
            raise
    capacity(root)
    require(path.stat().st_size <= LOG_LIMIT, "COMMAND_LOG_EXCEEDS_1_MIB:" + label)
    raw = path.read_bytes()
    require(code == expected, "COMMAND_EXIT:" + label + ":" + str(code))
    return {"label": label, "argv": [str(item) for item in argv], "exitCode": code,
            "startedUnix": started, "finishedUnix": time.time(),
            "log": str(path.relative_to(root)), "bytes": len(raw), "sha256": sha(raw)}, raw


def classes(directory):
    found = {path.relative_to(directory).as_posix(): path for path in directory.rglob("*.class")}
    require(set(found) == EXPECTED_CLASSES, "EXACT_TEN_FOCUSED_CLASSES_REQUIRED")
    rows = []
    for name in sorted(found):
        path = found[name]
        require(path.is_file() and not path.is_symlink(), "CLASS_MUST_BE_REAL_FILE:" + name)
        raw = path.read_bytes()
        require(len(raw) >= 8 and raw[:4] == b"\xca\xfe\xba\xbe"
                and int.from_bytes(raw[6:8], "big") == 65, "JAVA_21_CLASS_REQUIRED:" + name)
        rows.append({"path": name, "bytes": len(raw), "sha256": sha(raw), "major": 65})
    return rows


def proof_marker(raw, expected):
    require(raw.decode("utf-8", errors="strict").splitlines() == [REUSE_MARKER, expected],
            "ACTUAL_COMPLETE_UNCHANGED_PROOF_OUTPUT_REQUIRED")


def execute(root, source, ident):
    metadata, before, manifest_hash = inputs(source)
    env, java, javac, jdk, cc, nm, java_options, tools = toolchain(root)
    require(not (root / "artifacts/state-relation-execution.json").exists(),
            "FRESH_EXECUTION_RECEIPT_REQUIRED")
    work = root / "state-relation-native"
    require(not work.exists() and not work.is_symlink(), "FRESH_NATIVE_OUTPUT_ROOT_REQUIRED")
    class_root, headers = work / "classes", work / "headers"
    for directory in (class_root, headers):
        directory.mkdir(parents=True, exist_ok=False)
    commands = []
    javac_options = ["-J-XX:+PerfDisableSharedMem", "-J-Djava.io.tmpdir=" + str(root / "tmp"),
                    "-J-Duser.home=" + str(root / "home"), "--release", "21",
                    "-proc:none", "-Xlint:all", "-Werror", "-classpath", "", "-sourcepath", ""]
    row, raw = command(root, source, "state-relation-java-compile",
        [str(javac), *javac_options, "-h", str(headers), "-d", str(class_root),
         str(source / OWNER), str(source / ADAPTER), str(source / PROOF)], env)
    require(not raw, "STRICT_JAVA_COMPILE_MUST_HAVE_NO_DIAGNOSTICS")
    commands.append(row)
    inventory = classes(class_root)
    generated = headers / "jdk_internal_mindex_M3StateRelation.h"
    require({path.name for path in headers.iterdir()} == {generated.name}
            and generated.is_file() and not generated.is_symlink(), "ONE_REAL_GENERATED_JNI_HEADER_REQUIRED")
    generated_raw = generated.read_bytes()
    exports = set(re.findall(rb"JNICALL\s+(Java_[A-Za-z0-9_]+)\s*\(", generated_raw))
    require(exports == {name.encode("ascii") for name in JNI_EXPORTS}
            and b"(JNIEnv *, jclass, jlongArray, jlongArray);" in generated_raw
            and b"(JNIEnv *, jclass, jlongArray, jlong);" in generated_raw,
            "ACTUAL_JAVAC_JNI_SIGNATURES_REQUIRED")
    real_directory(root / "artifacts", "ARTIFACTS")
    header_copy = root / "artifacts/state-relation-generated-jni.h"
    with header_copy.open("xb") as stream:
        stream.write(generated_raw)
    java_command = [str(java), *java_options, "-Xmx32m", "-Xcheck:jni",
                    "-cp", str(class_root), MAIN]
    row, raw = command(root, source, "state-relation-java-proof", java_command, env)
    proof_marker(raw, JAVA_MARKER)
    commands.append(row)
    library = work / "libm3relation.so"
    row, raw = command(root, source, "state-relation-c11-compile",
        [cc, "-std=c11", "-Wall", "-Wextra", "-Wpedantic", "-Werror",
         "-Wconversion", "-Wsign-conversion", "-fPIC", "-shared",
         "-fsanitize=undefined", "-fno-sanitize-recover=all",
         "-I" + str(jdk / "include"), "-I" + str(jdk / "include/linux"),
         "-I" + str((source / HEADER).parent), "-I" + str(headers),
         "-include", str(generated), str(source / NATIVE), "-o", str(library)], env)
    require(not raw, "STRICT_C11_COMPILE_MUST_HAVE_NO_DIAGNOSTICS")
    commands.append(row)
    require(library.is_file() and not library.is_symlink() and library.stat().st_size < LOG_LIMIT,
            "BOUNDED_REAL_JNI_LIBRARY_REQUIRED")
    binary = library.read_bytes()
    require(binary[:4] == b"\x7fELF", "ACTUAL_LINUX_SHARED_LIBRARY_REQUIRED")
    row, raw = command(root, source, "state-relation-native-symbols",
                       [nm, "-D", "--defined-only", str(library)], env)
    symbols = {line.split()[-1] for line in raw.decode("utf-8").splitlines() if line.split()}
    require({name for name in symbols if name.startswith("Java_")} == JNI_EXPORTS
            and {"m3_state_relation_compose", "m3_state_relation_repeat"} <= symbols,
            "EXACT_JNI_EXPORTS_AND_REAL_KERNEL_SYMBOLS_REQUIRED")
    commands.append(row)
    row, raw = command(root, source, "state-relation-native-proof",
                       [*java_command, str(library)], env)
    proof_marker(raw, JNI_MARKER)
    commands.append(row)
    missing = work / "must-not-exist.so"
    require(not missing.exists() and not missing.is_symlink(), "MISSING_LIBRARY_CONTROL_OCCUPIED")
    row, raw = command(root, source, "state-relation-missing-library-refusal",
                       [*java_command, str(missing)], env, expected=1)
    require(b"java.lang.UnsatisfiedLinkError" in raw and str(missing).encode("utf-8") in raw
            and b"M3_RELATION_PORT" not in raw and b"REUSE logicalUnits" not in raw,
            "MISSING_NATIVE_MUST_REFUSE_BEFORE_PROOF_WITHOUT_FALLBACK")
    commands.append(row)
    _, after, final_manifest_hash = inputs(source)
    require(before == after and manifest_hash == final_manifest_hash, "SOURCE_OR_MANIFEST_CHANGED")
    require(inventory == classes(class_root), "CLASSES_CHANGED_AFTER_EXECUTION")
    require(library.read_bytes() == binary and generated.read_bytes() == generated_raw
            and header_copy.read_bytes() == generated_raw, "NATIVE_OR_GENERATED_HEADER_CHANGED")
    for item in commands:
        raw = safe_file(root, item["log"]).read_bytes()
        require(len(raw) == item["bytes"] and sha(raw) == item["sha256"], "COMMAND_LOG_CHANGED")
    custody = write(root, "state-relation-source-custody.json", {
        "schema": 1, "qualificationId": ident, "inputManifestSha256": manifest_hash,
        "files": 5, "sourceBytes": 28948, "before": before, "after": after, "unchanged": True,
    })
    receipt = {
        "schema": "M3_STATE_RELATION_CURRENT_C11_EXECUTION_V1", "state": "PASS",
        "qualificationId": ident, "scope": "EXACT_FIVE_OWNER_STATE_RELATION_JAVA_C11_JNI_DIFFERENTIAL",
        "sourceBase": metadata["sourceBase"], "preCodeAdmission": metadata["preCodeAdmission"],
        "inputManifestSha256": manifest_hash, "sourceCustody": custody,
        "commands": commands, "tools": tools, "classes": inventory,
        "generatedJniHeader": {"path": str(header_copy.relative_to(root)),
            "bytes": len(generated_raw), "sha256": sha(generated_raw)},
        "nativeLibrary": {"bytes": len(binary), "sha256": sha(binary), "exportedAsArtifact": False,
            "cStandard": "c11", "fatalUndefinedBehaviorSanitizer": True,
            "generatedHeaderForced": True, "jniExports": sorted(JNI_EXPORTS)},
        "cases": 71748, "javaChecks": 74829, "nativeChecks": 149658,
        "fullExistingProofPreserved": True, "javaClassPathExecuted": True,
        "nativeExecuted": True, "nativeSkips": 0, "explicitLibraryLoad": True,
        "xcheckJni": True, "missingLibraryRefused": True,
        "currentAliasStagingCExecuted": True, "modulePatchUsed": False,
        "recipeSdkQualified": False, "recipeSdkQualification": "SEPARATE_PREREQUISITE",
        "storage": {"capBytes": CAP, "reserveBytes": RESERVE,
            "externalAggregateAndProcessSupervisorRequired": True,
            "selfEnforcedAggregateClaim": False, "finalFreeBytes": capacity(root)},
        "sourceBindingQualifiedByDriver": False, "externalToolchainBytesSealed": False,
        "advance0Qualified": False, "fullNativeLibraryQualified": False,
        "cmakeIntegrationQualified": False, "privateSourceForkReactorQualified": False,
        "matchedJdkQualified": False, "stringDispatchQualified": False,
        "windowsQualified": False, "fullJdkQualified": False, "performanceQualified": False,
        "directCPartialOverlapQualified": False, "runtimeApplied": False, "phaseAdvanced": False,
    }
    output = write(root, "state-relation-execution.json", receipt)
    print(json.dumps({"state": "PASS", "nativeExecuted": True, "receipt": output}, sort_keys=True))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output-root", required=True)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--inputs-only", action="store_true")
    mode.add_argument("--run", action="store_true")
    args = parser.parse_args()
    root, source, ident = context(args.output_root)
    if args.inputs_only:
        _, found, manifest_hash = inputs(source)
        print(json.dumps({"state": "INPUTS_ONLY", "qualificationId": ident, "files": len(found),
            "inputManifestSha256": manifest_hash, "nativeExecuted": False}, sort_keys=True))
    else:
        execute(root, source, ident)


if __name__ == "__main__":
    main()
