#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""M3JDK21 receiving check only; not a second canonical transformation recipe."""
from pathlib import Path
import hashlib
import subprocess
import tempfile
import argparse

ROOT = Path(__file__).resolve().parents[4]
OWNER = ROOT / "m3/collections/src/main/java/com/m3/collections/M3CollectionViews.java"
TEST = ROOT / "m3/collections/src/test/java/com/m3/collections/M3CopyOnWriteBulkContract.java"
SHA256_OWNER = "7fed24335d4b642909a91974fc6c742780c3df2543835b7c518328b95800ef82"
GIT_BLOB_TEST = "1b8681e883b72eea0801a0012bee825b284dfbbc"

def gitblob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()

def verify() -> None:
    if OWNER.is_symlink() or TEST.is_symlink() or not OWNER.is_file() or not TEST.is_file():
        raise ValueError("missing or symlinked M3 COW receiver sources")
    source = OWNER.read_bytes()
    test = TEST.read_bytes()
    if hashlib.sha256(source).hexdigest() != SHA256_OWNER:
        raise ValueError("target source SHA256 differs from canonical source recipe output")
    if gitblob(test) != GIT_BLOB_TEST:
        raise ValueError("target contract fixture differs from Synexia canonical input")
    if source.count(b"M3_COW_BULK_REMOVE_V1") != 1:
        raise ValueError("COW bulk atom marker missing or duplicated")
    with tempfile.TemporaryDirectory(prefix="m3jdk-cow-receiver-") as temp:
        cmd = ["javac", "--release", "21", "-proc:none", "-Xlint:all", "-Werror",
               "-d", temp, str(OWNER), str(TEST)]
        subprocess.run(cmd, check=True)
        run = subprocess.run(["java", "-ea", "-cp", temp,
                              "com.m3.collections.M3CopyOnWriteBulkContract"],
                             text=True, capture_output=True)
        if run.returncode != 0 or "M3_COW_BULK_CONTRACT_PASS checks=21632" not in run.stdout:
            raise RuntimeError("M3JDK21 Java21 receiver failed\n" + run.stdout + run.stderr)
        print(run.stdout.strip())
    print("M3JDK21_COW_RECEIVER_CHECK_PASS exact blob, strict Java21, no persistent test output")

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--verify", action="store_true")
    parser.parse_args()
    verify()
