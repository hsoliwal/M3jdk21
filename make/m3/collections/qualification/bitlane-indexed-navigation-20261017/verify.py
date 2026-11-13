#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only exact-source/Java21/C17 JNI receiver gate for Synexia bitmap atom.

Never edits product source or silently falls back from requested JNI. Canonical
source mutation remains the Maven/OpenRewrite recipe in com.synexia #12104.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[3]
PREFIX = Path("m3/collections/")
JAVA_OWNER = PREFIX / "src/main/java/jdk/m3/collections/M3BitLane28.java"
PROOF_OWNER = PREFIX / "src/test/java/jdk/m3/collections/M3BitLaneJumpProof.java"
NATIVE_OWNER = PREFIX / "src/test/native/m3_bitlane_jump_jni.c"
ADDRESS = PREFIX / "src/main/java/jdk/m3/collections/M3Address28.java"
EXPECTED = {
    JAVA_OWNER.as_posix(): (
        "45eb57846a3607ef68855c667e258c5c24b90e61e071becc19c9819cbd06dfcc",
        "536eb7ae1968ce17f4ee2cab3d0d6ef6df6865599605359c00fb3f0225be84f3",
        "dbd5574956db770311229d87c043896d52cb9b46", "PRODUCT_JAVA_OWNER"),
    PROOF_OWNER.as_posix(): (
        "ABSENT", "dcd1f4d3439f558c6c685d615a24d136f2696cdf41de4a403a48f7cd6b1b578b",
        "1d7fc1c94a9588830bc2334c7214f6cdd3befebc", "JAVA21_CONTRACT_ORACLE"),
    NATIVE_OWNER.as_posix(): (
        "ABSENT", "a93ff4e0262344f882434764d089949b9b5373d253890ed082e061c062aa77cf",
        "dad4302f54d8ad58c015050fd0ec06069d743b13", "JNI_TEST_ONLY"),
}
BEFORE = ("45eb57846a3607ef68855c667e258c5c24b90e61e071becc19c9819cbd06dfcc",
          "06efbf5ebba7481422e9166e1304374c7151fdc8")
ADDRESS_BLOB = "ae1e43c52351d205ebfc7e57ca5a230a49410399"

def sha256(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()

def git_blob(value: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(value)).encode("ascii") + b"\0" + value).hexdigest()

def read_file(path: Path) -> bytes:
    if path.is_symlink() or not path.is_file():
        raise ValueError(f"refused: missing or symlinked receipt/source: {path}")
    return path.read_bytes()

def assert_image(value: bytes, sha: str, blob: str, path: str) -> None:
    if sha256(value) != sha or git_blob(value) != blob:
        raise ValueError(f"refused: exact source-template drift: {path}")

def receipt(root: Path) -> dict[str, bytes]:
    document = read_file(HERE / "RECEIVING.tsv").decode("utf-8")
    rows = list(csv.DictReader(document.splitlines(), delimiter="\t"))
    if [row["path"] for row in rows] != list(EXPECTED):
        raise ValueError("refused: unexpected source receiver inventory/order")
    values = {}
    for row in rows:
        path = row["path"]
        expected = EXPECTED[path]
        if tuple(row[key] for key in
                 ("pre_sha256", "post_sha256", "m3_template_git_blob", "role")) != expected:
            raise ValueError("refused: receipt authority drift")
        value = read_file(root / path)
        assert_image(value, expected[1], expected[2], path)
        values[path] = value
    original = read_file(HERE / "before/M3BitLane28.java")
    assert_image(original, *BEFORE, "frozen-original")
    address = read_file(root / ADDRESS)
    if git_blob(address) != ADDRESS_BLOB:
        raise ValueError("refused: unqualified address-space owner drift")
    return values

def refusal_tests(images: dict[str, bytes]) -> None:
    for path, blob in images.items():
        sha, git, _ = EXPECTED[path][1:]
        for mutation in (blob + b"// unauthorized mutation\n", blob[:-1]):
            try:
                assert_image(mutation, sha, git, path)
            except ValueError:
                continue
            raise AssertionError("manifest failed to refuse mutation")
    if not images[JAVA_OWNER.as_posix()].count(b"return select(rankUnchecked(nextBlock));") == 1:
        raise AssertionError("next-jump logic not in frozen generated owner")
    if not images[JAVA_OWNER.as_posix()].count(b"select(preceding - 1);") == 1:
        raise AssertionError("previous-jump logic not in frozen generated owner")
    print("M3_SOURCE_RECEIPT_DRIFT_REFUSAL_PASS")

def execute(args: list[str]) -> str:
    result = subprocess.run(args, text=True, stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, check=False)
    if result.returncode:
        raise RuntimeError(f"exit={result.returncode} command={args!r}\n{result.stdout}")
    return result.stdout.strip()

def compile_sources(root: Path, output: Path, original: bool) -> Path:
    target = output / "source/jdk/m3/collections"
    target.mkdir(parents=True)
    main = target / "M3BitLane28.java"
    main.write_bytes(read_file(HERE / "before/M3BitLane28.java")
                     if original else read_file(root / JAVA_OWNER))
    address = target / "M3Address28.java"
    address.write_bytes(read_file(root / ADDRESS))
    proof = target / "M3BitLaneJumpProof.java"
    proof.write_bytes(read_file(root / PROOF_OWNER))
    classes = output / "classes"
    classes.mkdir()
    javac = shutil.which("javac")
    if javac is None or not execute([javac, "-version"]).startswith("javac 21"):
        raise RuntimeError("JDK21 javac required, no fallback")
    print(execute([javac, "--release", "21", "-proc:none",
                   "-Xlint:all", "-Werror", "-h", str(output),
                   "-d", str(classes), str(address), str(main), str(proof)]))
    return classes

def test_java21(root: Path, native: bool) -> None:
    for command in ("javac", "java", "javap"):
        if shutil.which(command) is None:
            raise RuntimeError(f"missing Java21 tool {command}")
    with tempfile.TemporaryDirectory(prefix="m3-bitlane-jump-") as dirname:
        temp = Path(dirname)
        old = compile_sources(root, temp / "old", True)
        current = compile_sources(root, temp / "new", False)
        clazz = "jdk.m3.collections.M3BitLane28"
        before_api = execute(["javap", "-public", "-s", "-classpath", str(old), clazz])
        after_api = execute(["javap", "-public", "-s", "-classpath", str(current), clazz])
        if before_api != after_api:
            raise AssertionError("M3BitLane28 public Java API/ABI descriptor drift")
        main = "jdk.m3.collections.M3BitLaneJumpProof"
        for opts in ([], ["-Xint"]):
            output = execute(["java", "-ea", *opts, "-cp", str(current), main])
            if "M3_BITLANE_JUMP_PASS assertions=68012 native=false" not in output:
                raise AssertionError("unexpected Java21 count: " + output)
            print(output)
        if not native:
            print("M3_JAVA21_PRODUCT_CODE_AND_API_PASS")
            return
        if not sys.platform.startswith("linux"):
            raise RuntimeError("checked C17 JNI test currently requires Linux")
        cc = shutil.which("cc")
        if cc is None:
            raise RuntimeError("C compiler missing; native-required cannot fall back")
        jdk = Path(shutil.which("javac")).resolve().parent.parent
        headers = jdk / "include"
        if not (headers / "jni.h").is_file():
            raise RuntimeError("JDK21 JNI headers unavailable")
        library = temp / "libm3_bitlane_jump_jni.so"
        print(execute([cc, "-std=c17", "-O2", "-Wall", "-Wextra", "-Wconversion",
                       "-Wshadow", "-Werror", "-pedantic", "-fPIC", "-shared",
                       "-fsanitize=undefined", "-fno-sanitize-recover=undefined",
                       "-I", str(headers), "-I", str(headers / "linux"),
                       str(root / NATIVE_OWNER), "-o", str(library)]))
        for opts in ([], ["-Xint"]):
            output = execute(["java", "-ea", "-Xcheck:jni", *opts,
                              "-cp", str(current), main, str(library)])
            if "M3_BITLANE_JUMP_PASS assertions=68034 native=true" not in output:
                raise AssertionError("unexpected JNI count: " + output)
            print(output)
        print("M3_STRICT_C17_JNI_PRODUCT_CODE_PASS")

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--jni", action="store_true")
    args = parser.parse_args()
    root = args.root.resolve(strict=True)
    images = receipt(root)
    refusal_tests(images)
    test_java21(root, args.jni)

if __name__ == "__main__":
    main()
