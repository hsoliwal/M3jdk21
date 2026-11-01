#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Exact original V1 replay plus pinned later Java/C JNI source custody."""
import argparse
import hashlib
from pathlib import Path
import re
import subprocess
import sys

HISTORICAL_SHA = "31cdce1db97fd1567b360ee843916f9b6cfa3ff3"
ORIGINAL_VERIFIER = "m3/collections/qualification/zero-mask-window-copy-20261017/verify.py"
ORIGINAL_VERIFIER_BLOB = "d40cbee01ed1351bb118e654bc4123ad17c2d29f"
MANIFEST = ("m3/collections/qualification/zero-mask-window-successor-20261031/"
            "SOURCE_SUCCESSOR_V2.tsv")
C = "m3/collections/src/main/native/collections/m3_index_mask_compactor.c"
JAVA = "m3/collections/src/test/java/jdk/m3/collections/M3IndexMaskCompactorSelfTest.java"
EXPECTED = (
    (C, "a549b92b38eaa337c0e95f32c46607cace9ae46d8240d37bd58a3a48e296990d",
     "dd0f0811904b0e53d12f4148b13d7abdc73b235f44f4956474c695a6c6e8f6e5",
     "8c2ff6e02369a2632bfdd3534d01259e9c86e21c",
     "adef01874abc60135771659652a8ac6c7c09e26c", "JNI_SOURCE"),
    (JAVA, "c81601d08eb3d7442d4fe952a9ba580da17bd3dbe74fb581daa04ebf2f575996",
     "5b3b47df54584b1ddf3aee72307cc645691b51c44a97d83522f70986bf1c7edc",
     "edc6698d7dc7fac990a39c3cc301951e062f2621",
     "10b14826f3cb76f180b558f1721a6e144af24137", "JAVA_ORACLE"),
)
JNI_SYMBOL = re.compile(rb"JNIEXPORT\s+\w+\s+JNICALL\s+(Java_[A-Za-z0-9_]+)\s*\(")
JAVA_METHOD = re.compile(rb"\b(?:public|private)\s+static\s+void\s+(\w+)\s*\(")

def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def receipt_rows(path: Path) -> tuple:
    if path.is_symlink() or not path.is_file():
        raise ValueError("missing or aliased successor receipt")
    raw = path.read_bytes()
    if b"\r" in raw or raw.decode("utf-8", "strict").encode("utf-8") != raw:
        raise ValueError("source receipt encoding drift")
    rows = tuple(tuple(line.split("\t")) for line in raw.decode().splitlines()
                 if line and not line.startswith("#"))
    if rows != EXPECTED:
        raise ValueError("wrong exact successor source corpus")
    return rows

def read_owned(root: Path, relative: str, digest: str, blob: str) -> bytes:
    path = root / relative
    if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(root):
        raise ValueError("missing or aliased exact source: " + relative)
    data = path.read_bytes()
    if hashlib.sha256(data).hexdigest() != digest or git_blob(data) != blob:
        raise ValueError("source SHA256/Git blob drift: " + relative)
    return data

def verify_history_and_current(product: Path, original: Path) -> tuple[str, ...]:
    product, original = product.resolve(strict=True), original.resolve(strict=True)
    if product == original:
        raise ValueError("historical and current source roots cannot alias")
    head = subprocess.check_output(
        ["git", "-C", str(original), "rev-parse", "HEAD"], text=True).strip()
    if head != HISTORICAL_SHA:
        raise ValueError("wrong original zero-mask source commit")
    for root in (original, product):
        file = root / ORIGINAL_VERIFIER
        if file.is_symlink() or not file.is_file() or git_blob(file.read_bytes()) != ORIGINAL_VERIFIER_BLOB:
            raise ValueError("immutable original V1 source verifier drift")
    subprocess.run([sys.executable, "-I", "-S", "-B", str(original / ORIGINAL_VERIFIER)],
                   cwd=original, check=True)
    receipt_rows(product / MANIFEST)
    result = []
    for path, before_sha, after_sha, before_blob, after_blob, kind in EXPECTED:
        previous = read_owned(original, path, before_sha, before_blob)
        current = read_owned(product, path, after_sha, after_blob)
        if previous == current:
            raise ValueError("phantom reviewed source successor")
        indexed = subprocess.check_output(
            ["git", "-C", str(product), "ls-files", "--stage", "--", path],
            text=True).strip()
        if indexed != "100644 " + after_blob + " 0\t" + path:
            raise ValueError("untracked or modified indexed source: " + path)
        if kind == "JNI_SOURCE":
            old_symbols, now_symbols = set(JNI_SYMBOL.findall(previous)), set(JNI_SYMBOL.findall(current))
            if not old_symbols or not old_symbols.issubset(now_symbols):
                raise ValueError("original JNI native ABI owner removed")
            result.append("JNI_EXPORTS=" + str(len(now_symbols)))
        else:
            old_methods, new_methods = set(JAVA_METHOD.findall(previous)), set(JAVA_METHOD.findall(current))
            if not old_methods or not old_methods.issubset(new_methods):
                raise ValueError("original Java regression oracle method lost")
            if b"PASS M3IndexMaskCompactorSelfTest" not in current:
                raise ValueError("old Java test witness lost")
            result.append("JAVA_ORACLE_METHODS=" + str(len(new_methods)))
    return tuple(result)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--original-checkout", type=Path, required=True)
    args = parser.parse_args()
    facts = verify_history_and_current(args.product, args.original_checkout)
    print("M3_ZERO_MASK_V2_SOURCE_REPLAY_PASS " + " ".join(facts)
          + " V1=EXECUTED current_java_c=SOURCE_SEALED")

if __name__ == "__main__":
    main()
