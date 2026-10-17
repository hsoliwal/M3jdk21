#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Maven-bound, source-sealed JDK21 receiver of canonical Synexia COW bulk atom.

This is an exact-source Java snapshot recipe, not an OpenRewrite SDK pass. The
canonical behavioral donor is hsoliwal/com.synexia PR #11831. No JDK source,
parent branch or private Synexia owner is modified by this recipe.
"""
from __future__ import annotations

import argparse
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
SOURCE = Path("m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java")
TEST = Path("m3/collections/src/test/java/com/m3/collections/M3CowBulkReceiverProof.java")
PRE = "115f16592a41ce78a63b4892904cb4fe934f5ee45974ffb3305ec39a7c0b9f6e"
POST = "5d2a4c9197ac3391d13d5a25c0f77a12261255bbab076e9e454e89df2dcffc66"
PROOF = "af51caf6ddc8030a2b52c64c323835981890fa6dbc39e68188b96b8e696bea6a"
NATIVE = "82c12d4442b44f2131acb42317d0c2a894e154f143aab7e898c3a672c25ac7f1"

IMPORT = "import java.util.Spliterator;\n"
IMPORT_ADDITION = ("import java.util.concurrent.CopyOnWriteArrayList;\n"
                   "import java.util.concurrent.CopyOnWriteArraySet;\n")
METHOD = ("        private boolean removeMatching(Predicate<? super E> condition) {\n"
          "            boolean changed = false;\n")
REPLACEMENT = ("        private boolean removeMatching(Predicate<? super E> condition) {\n"
               "            // M3_COW_BULK_REMOVE_V1: delegate to JDK copy-on-write owner's supported removeIf.\n"
               "            if (backing instanceof CopyOnWriteArrayList<?> || backing instanceof CopyOnWriteArraySet<?>) {\n"
               "                return backing.removeIf(element -> visible(element) && condition.test(element));\n"
               "            }\n"
               "            boolean changed = false;\n")

def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def read_pinned(path: Path, expected: str) -> bytes:
    if path.is_symlink() or not path.is_file():
        raise ValueError(f"refused: missing or symlinked {path}")
    data = path.read_bytes()
    if digest(data) != expected:
        raise ValueError(f"refused: SHA256 seal mismatch: {path}")
    return data

def render(before: bytes) -> bytes:
    if digest(before) != PRE:
        raise ValueError("refused: source preimage drift")
    text = before.decode("utf-8", errors="strict")
    if text.count(IMPORT) != 1 or text.count(METHOD) != 1:
        raise ValueError("refused: nonunique source anchors")
    if "M3_COW_BULK_REMOVE_V1" in text or "import java.util.concurrent.CopyOnWriteArrayList;" in text:
        raise ValueError("refused: duplicate composition")
    output = text.replace(IMPORT, IMPORT + IMPORT_ADDITION, 1).replace(
        METHOD, REPLACEMENT, 1).encode("utf-8")
    if digest(output) != POST:
        raise ValueError("refused: generated output drift")
    return output

def inspect_fixture() -> tuple[bytes, bytes, bytes, bytes]:
    before = read_pinned(HERE / "before/M3CollectionViews.java", PRE)
    java = read_pinned(HERE / "proof/M3CowBulkReceiverProof.java", PROOF)
    native = read_pinned(HERE / "proof/m3_cow_bulk_probe.c", NATIVE)
    after = render(before)
    for altered in (before + b"\n// source drift\n",
                    before.replace(IMPORT.encode(), b"import java.util.Spliterator;\n"
                                                    b"import java.util.Spliterator;\n", 1),
                    before.replace(METHOD.encode(), b"        // altered owner\n", 1)):
        try:
            render(altered)
        except ValueError:
            continue
        raise AssertionError("negative source admission succeeded")
    if after.count(b"M3_COW_BULK_REMOVE_V1") != 1 or render(before) != after:
        raise AssertionError("nondeterministic candidate image")
    print("RECIPE_SOURCE_SEAL_AND_REFUSAL_PASS")
    return before, after, java, native

def run(cmd: list[str], cwd: Path | None = None) -> str:
    output = subprocess.run(cmd, cwd=cwd, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, check=False)
    if output.returncode != 0:
        raise RuntimeError(f"command failed {cmd!r}:\n{output.stdout}")
    return output.stdout.strip()

def compile_one(source: bytes, java: bytes, target: Path, javac: str) -> Path:
    root = target / "source/com/m3/collections"
    root.mkdir(parents=True)
    main = root / "M3CollectionViews.java"
    fixture = root / "M3CowBulkReceiverProof.java"
    main.write_bytes(source)
    fixture.write_bytes(java)
    classes = target / "classes"
    classes.mkdir()
    include = target / "jni-headers"
    include.mkdir()
    print(run([javac, "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
               "-h", str(include), "-d", str(classes), str(main), str(fixture)]))
    return classes

def exercise(pre: bytes, post: bytes, java: bytes, native: bytes,
             native_required: bool) -> None:
    javac = shutil.which("javac")
    java_cmd = shutil.which("java")
    javap = shutil.which("javap")
    if not javac or not java_cmd or not javap:
        raise RuntimeError("Java 21 javac/java/javap is mandatory")
    release = run([javac, "-version"])
    if not release.startswith("javac 21"):
        raise RuntimeError("Java 21 required, found " + release)
    with tempfile.TemporaryDirectory(prefix="m3-cow-receiver-") as scratch:
        base = Path(scratch)
        red = compile_one(pre, java, base / "red", javac)
        green = compile_one(post, java, base / "green", javac)
        cls = "com.m3.collections.M3CollectionViews"
        red_api = run([javap, "-public", "-s", "-classpath", str(red), cls])
        green_api = run([javap, "-public", "-s", "-classpath", str(green), cls])
        if red_api != green_api:
            raise AssertionError("public JDK module API drift")
        main = "com.m3.collections.M3CowBulkReceiverProof"
        print(run([java_cmd, "-ea", "-Xint", "-cp", str(red), main, "--red"]))
        print(run([java_cmd, "-ea", "-cp", str(green), main]))
        print(run([java_cmd, "-ea", "-Xint", "-cp", str(green), main]))
        if native_required:
            if sys.platform != "linux":
                raise RuntimeError("checked native proof currently requires Linux")
            cc = shutil.which("cc")
            if cc is None:
                raise RuntimeError("C17 compiler required for JNI parity")
            # Resolve real javac binary; Java 21 JNI headers must match Java runtime.
            home = Path(javac).resolve().parent.parent
            include = home / "include"
            if not (include / "jni.h").is_file():
                raise RuntimeError("JDK 21 JNI headers not found")
            unit = base / "m3_cow_bulk_probe.c"
            unit.write_bytes(native)
            lib = base / "libm3_cow_bulk_probe.so"
            print(run([cc, "-std=c17", "-O2", "-Wall", "-Wextra", "-Werror",
                       "-Wconversion", "-Wpedantic", "-fPIC", "-shared",
                       "-fsanitize=undefined", "-fno-sanitize-recover=undefined",
                       "-I", str(include), "-I", str(include / "linux"),
                       str(unit), "-o", str(lib)]))
            for mode in ([], ["-Xint"]):
                print(run([java_cmd, "-ea", "-Xcheck:jni", *mode, "-cp",
                           str(green), main, str(lib)]))
            print("C17_JNI_CHECKED_PARITY_PASS")
        print("JAVA21_DESCRIPTOR_PARITY_AND_RECEIVER_PASS")

def write_atomic(path: Path, value: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temp = tempfile.mkstemp(prefix=".m3-cow-", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as output:
            output.write(value)
        os.replace(temp, path)
    finally:
        if os.path.exists(temp):
            os.unlink(temp)

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("mode", choices=("self-test", "verify", "apply"))
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--jni", action="store_true", help="require compiled C17 JNI parity")
    args = parser.parse_args()
    before, after, java, native = inspect_fixture()
    if args.mode == "self-test":
        return
    root = args.root.resolve(strict=True)
    target = root / SOURCE
    test = root / TEST
    if target.is_symlink() or not target.is_file() or test.is_symlink():
        raise ValueError("refused: missing/unsafe owner or symlink")
    installed = target.read_bytes()
    if installed not in (before, after):
        raise ValueError("refused: target preimage was changed by another branch")
    if test.exists() and test.read_bytes() != java:
        raise ValueError("refused: existing proof file differs from tested template")
    if args.mode == "verify" and (installed != after or not test.exists()):
        raise ValueError("refused: recipe output not installed")
    # Generate, compile and test in isolation BEFORE any target write.
    exercise(before, after, java, native, args.jni)
    if args.mode == "apply":
        if installed == before:
            write_atomic(target, after)
        if not test.exists():
            write_atomic(test, java)
    if target.read_bytes() != after or test.read_bytes() != java:
        raise AssertionError("source delivery/image fixed point")
    print("M3_COW_BULK_RECEIVER_FIXED_POINT source_sha256=" + POST)

if __name__ == "__main__":
    main()
