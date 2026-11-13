#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-20261031/verify.py blob bfa179e51bbe; recipe make/m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
"""Fail-closed Java21/C17/C++20/JNI HotSpot-mode differential smoke gate.

This test is a real JVM/native compiler probe, NOT a proof that arbitrary OpenJDK
source was atomized, patternized, compiled by C2, or accepted for product release.
"""
import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import tempfile

HERE = pathlib.Path(__file__).resolve().parent
SOURCES = ("AtomJitProbe.java", "m3jni.c", "m3jni.cpp")
SYMBOLS = (
    "Java_jdk_m3_tooling_atomjit_fixture_AtomJitProbe_sum",
    "Java_jdk_m3_tooling_atomjit_fixture_AtomJitProbe_cppFold",
)
MODES = (
    ("interpreted", ("-Xint",)),
    ("c1_eligible", ("-XX:+TieredCompilation", "-XX:TieredStopAtLevel=1", "-Xcomp")),
    ("c2_eligible", ("-XX:-TieredCompilation", "-Xcomp")),
    ("default_tiered", ()),
)
MAX_OUTPUT = 1_000_000


class Rejected(RuntimeError):
    pass


def sha(data):
    return hashlib.sha256(data).hexdigest()


def command(argv, *, timeout=90, cwd=None):
    try:
        process = subprocess.run([str(v) for v in argv], cwd=cwd,
                                 stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                                 timeout=timeout, check=False)
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise Rejected("command unavailable or timed out: " + str(argv[:2])) from exc
    if len(process.stdout) > MAX_OUTPUT or len(process.stderr) > MAX_OUTPUT:
        raise Rejected("bounded output exceeded: " + str(argv[:2]))
    return process


def required(command_name):
    executable = shutil.which(command_name)
    if not executable:
        raise Rejected("missing executable: " + command_name)
    return pathlib.Path(executable).resolve()


def seal_sources():
    manifest = (HERE / "SOURCE_SHA256.tsv").read_text(encoding="utf-8").splitlines()
    approved = {}
    for line in manifest:
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 2 or fields[0] in approved or fields[0] not in SOURCES:
            raise Rejected("invalid source custody manifest")
        approved[fields[0]] = fields[1]
    if set(approved) != set(SOURCES):
        raise Rejected("incomplete fixture source custody")
    for name in SOURCES:
        if not (len(approved[name]) == 64 and all(c in "0123456789abcdef" for c in approved[name])):
            raise Rejected("bad source hash: " + name)
        if sha((HERE / "fixtures" / name).read_bytes()) != approved[name]:
            raise Rejected("source drift; requalify the recipe first: " + name)
    return approved


def java_home_from(javac):
    home = pathlib.Path(javac).resolve().parent.parent
    os_inc = "darwin" if sys.platform == "darwin" else "win32" if sys.platform == "win32" else "linux"
    include = home / "include"
    if not (include / "jni.h").is_file() or not (include / os_inc / "jni_md.h").is_file():
        raise Rejected("JDK native JNI header prerequisites not available")
    return include, include / os_inc


def build_fixture(work, *, perturb_java=False):
    javac = required("javac")
    cc = required("cc")
    cxx = required("c++")
    javap = required("javap")
    nm = required("nm")
    include, platform_include = java_home_from(javac)
    classes = work / "classes"
    headers = work / "headers"
    classes.mkdir(parents=True)
    headers.mkdir(parents=True)
    original = (HERE / "fixtures/AtomJitProbe.java").read_text(encoding="utf-8")
    source = work / "AtomJitProbe.java"
    if perturb_java:
        original = original.replace('"M3_JAVA_JNI_ATOM_CHECKSUM="', '"M3_UNACCEPTED_DRIFT="')
    source.write_text(original, encoding="utf-8")
    compile_java = command((javac, "--release", "21", "-Xlint:all", "-Werror", "-d", classes,
                            "-h", headers, source))
    if compile_java.returncode:
        raise Rejected("javac -h failed: " + compile_java.stderr.decode(errors="replace")[:300])
    header_file = headers / "jdk_m3_tooling_atomjit_fixture_AtomJitProbe.h"
    header = header_file.read_text(encoding="utf-8")
    if any(symbol not in header for symbol in SYMBOLS):
        raise Rejected("javac -h changed native declaration/symbol")
    for filename, compiler, standard in (("m3jni.c", cc, "c17"), ("m3jni.cpp", cxx, "c++20")):
        obj = work / (filename + ".o")
        result = command((compiler, "-Wall", "-Wextra", "-Werror", "-O2", "-fPIC",
                          "-std=" + standard, "-I" + str(include), "-I" + str(platform_include),
                          "-c", HERE / "fixtures" / filename, "-o", obj))
        if result.returncode:
            raise Rejected(standard + " JNI compile failed: " + result.stderr.decode(errors="replace")[:300])
    lib = work / "libm3_atom_jni.so"
    result = command((cxx, "-shared", "-o", lib, work / "m3jni.c.o", work / "m3jni.cpp.o"))
    if result.returncode:
        raise Rejected("JNI native shared link failed")
    exported = command((nm, "-D", "--defined-only", lib))
    if exported.returncode or any(symbol.encode() not in exported.stdout for symbol in SYMBOLS):
        raise Rejected("native exports differ from javac -h")
    api = command((javap, "-classpath", classes, "-public", "jdk.m3.tooling.atomjit.fixture.AtomJitProbe"))
    if api.returncode:
        raise Rejected("public API could not be inspected")
    return {"classes": classes, "native": work, "api": sha(api.stdout),
            "jni_header": sha(header_file.read_bytes()),
            "symbols": [s for s in SYMBOLS]}


def matrix(before_java, after_java, before, after, *, timeout):
    results = []
    baseline = None
    for mode, flags in MODES:
        for side, java, fixture in (("before", before_java, before), ("after", after_java, after)):
            args = (str(java), *flags, "-Djava.library.path=" + str(fixture["native"]),
                    "-cp", str(fixture["classes"]), "jdk.m3.tooling.atomjit.fixture.AtomJitProbe")
            run = command(args, timeout=timeout)
            value = (run.returncode, run.stdout, run.stderr)
            if run.returncode:
                raise Rejected(side + " " + mode + " returned nonzero exit code "+str(run.returncode)
                               + " / " + run.stderr.decode(errors="replace")[:200])
            if baseline is None:
                baseline = value
            elif value != baseline:
                raise Rejected("JVM mode or before/after behavior drift: " + side + "/" + mode)
            results.append({"side": side, "mode": mode, "status": "PASS",
                            "stdout_sha256": sha(run.stdout), "stderr_sha256": sha(run.stderr),
                            "exit_code": run.returncode})
    return results


def verify(before_java, after_java, evidence, *, timeout, negative=False):
    approved = seal_sources()
    if not before_java or not after_java:
        raise Rejected("before/after JVM paths must be supplied")
    before_java = pathlib.Path(shutil.which(str(before_java)) or str(before_java)).resolve(strict=True)
    after_java = pathlib.Path(shutil.which(str(after_java)) or str(after_java)).resolve(strict=True)
    for label, jvm in (("before", before_java), ("after", after_java)):
        version = command((jvm, "-version"), timeout=timeout)
        if version.returncode or b"version" not in version.stderr + version.stdout:
            raise Rejected(label + " JVM identification failed")
    evidence = pathlib.Path(evidence)
    evidence.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="m3-jit-", dir=evidence) as scratch:
        root = pathlib.Path(scratch)
        before = build_fixture(root / "before")
        after = build_fixture(root / "after", perturb_java=negative)
        if before["api"] != after["api"]:
            raise Rejected("public/protected reflection/API root changed")
        if before["jni_header"] != after["jni_header"]:
            raise Rejected("JNI declaration header changed")
        results = matrix(before_java, after_java, before, after, timeout=timeout)
    core = {"schema": "M3_OPENJDK_JIT_JNI_DIFFERENTIAL_V1",
            "scope": "FIXTURE_SMOKE_NOT_FULL_OPENJDK_PROOF",
            "status": "PASS", "source_sha256": approved,
            "baseline_java_binary": str(before_java), "candidate_java_binary": str(after_java),
            "public_api_sha256": before["api"], "jni_header_sha256": before["jni_header"],
            "required_symbols": list(SYMBOLS), "mode_results": results,
            "native_gate": "JAVAC_H_C17_CXX20_LINK_NM_EXECUTED"}
    root = sha(json.dumps(core, sort_keys=True, separators=(",", ":")).encode("utf-8"))
    out = {**core, "receipt_sha256": root}
    (evidence / "receipt.json").write_text(json.dumps(out, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return root


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--before-java", default=shutil.which("java"))
    parser.add_argument("--after-java", default=shutil.which("java"))
    parser.add_argument("--evidence", required=True)
    parser.add_argument("--timeout-seconds", type=int, default=60)
    parser.add_argument("--negative-test", action="store_true", help="require failure for an injected probe drift")
    args = parser.parse_args()
    if args.timeout_seconds < 1 or args.timeout_seconds > 900:
        parser.error("timeout must be in [1,900]")
    try:
        root = verify(args.before_java, args.after_java, args.evidence,
                      timeout=args.timeout_seconds, negative=args.negative_test)
    except (Rejected, OSError, ValueError) as exc:
        if args.negative_test and isinstance(exc, Rejected) and "behavior drift" in str(exc):
            print("PASS: injected behavior drift refused")
            return 0
        print("REJECTED: " + str(exc), file=sys.stderr)
        return 1
    if args.negative_test:
        print("REJECTED: injected behavior drift was not detected", file=sys.stderr)
        return 1
    print("PASS: Java21/C17/C++20 JNI four-mode fixture gate root=" + root)
    return 0


if __name__ == "__main__":
    sys.exit(main())
