#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed original receipt replay, followed by the exact admitted Java/JNI successor.

Never changes the original 2026-10-17 verifier, manifests, sources or JNI symbols.
Runs that original verifier on its exact frozen Java/JNI inputs in a new temp tree.
Then checks every current production/test owner and the two-generation range source.
"""
from __future__ import annotations

import hashlib
from pathlib import Path, PurePosixPath
import subprocess
import sys
from tempfile import TemporaryDirectory

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[4]
LEGACY = "m3/collections/qualification/long-range-set-20261017/"
TEMPLATES = ("m3/tooling/migration-recipes/src/main/resources/"
             "com/synexia/rewrite/hash-pinned-java/"
             "m3jdk21-long-range-array-flags-20261017/")
PRODUCTION = "m3/collections/src/main/java/com/m3/collections/"
TEST = "m3/collections/src/test/java/com/m3/collections/"
NATIVE = "m3/collections/src/main/native/collections/"

ORIGINAL_TARGETS = {
    PRODUCTION + "M3LongRangeSet.java": (
        "f865b006888f1d4554b51cf69e7f4d6bac67458982f957f9151af0b4f09eaec8",
        "M3LongRangeSet.java.txt"),
    TEST + "M3LongRangeSetSelfTest.java": (
        "3b4ba4e2542ebbc607710f183f231d7c1bbe3e94ef4aa4fe6de49f0f9242ac29",
        "M3LongRangeSetSelfTest.java.txt"),
    TEST + "M3LongRangeSetTest.java": (
        "1768a8be759b46ae24678ce089cdf328ef6c83f335bd3f5582cda813819261a5",
        "M3LongRangeSetTest.java.txt"),
}
OLD_PEERS = {
    PRODUCTION + "M3IndexPathMasks.java":
        ("ef0fac5c1e71f8493baab365c8af64bb22cb6171", "M3IndexPathMasks.java.txt"),
    PRODUCTION + "M3LongLongConsumer.java":
        ("b1c01d1afacc76bd338179398ab42835e16992fa", "M3LongLongConsumer.java.txt"),
    NATIVE + "m3_index_path_masks.c":
        ("086b49293257fd17cc7fbd1ebbb98460aa677e35", "m3_index_path_masks.c.txt"),
}
GUARDED_OLD = {
    LEGACY + "verify.py": "13085fff756852fbe5cb6c1d26f373079f60d6a7",
    LEGACY + "CATEGORY_REVIEW.tsv": "f1ed7acefae57e3baf76dd0d1b42dcda83db04c0",
    LEGACY + "RECEIPT.tsv": "18b18a9149c27a6d92b9548cc4315ca7f920b791",
    TEMPLATES + "manifest.tsv": "9e749b13cc80f5885f697948fd61c112f65b4f10",
}
CURRENT = {
    PRODUCTION + "M3LongRangeSet.java": (
        "72c80e7748286181422405cbcc21420d5eb584637be49af7b1841e0bc8b97b98",
        "37f4f35415728a3d4bc54838b97153b05a973184"),
    TEST + "M3LongRangeSetAtomTest.java": (
        "cf436d406706f6ad252d360b86c595d0974b4b44ee8792a7f8684949d88511f0",
        "1df89940ecfab1baa6e94db62deb765cb02ae1d8"),
}
CURRENT_PEER_BLOBS = {
    PRODUCTION + "M3IndexPathMasks.java": "57c45d38af1529ff5ae7882e08606bb61b384f8c",
    PRODUCTION + "M3LaneSearch.java": "cf0395b89a6eb875a866982f8de390957c239406",
    PRODUCTION + "M3LongLongConsumer.java": "b1c01d1afacc76bd338179398ab42835e16992fa",
    NATIVE + "m3_index_path_masks.c": "a3466fbf6d9065a9b21cd061782e0f44867ae086",
    TEST + "M3LongRangeSetSelfTest.java": "f18a4d01b263a7e8471ecba2720e027b2a119abe",
    TEST + "M3LongRangeSetTest.java": "592004d8776f5122914a214566e33a84ad5cdfdf",
}
BEFORE = ("c1c07e23086cd4c49dca39a511d433209e8df0dd7449f8375a8aaaa7799d4596",
          "ca18bcda04ebf192acf8ae5388d433d2dbf83798")


def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob(data: bytes) -> str:
    prefix = ("blob " + str(len(data)) + "\0").encode("ascii")
    return hashlib.sha1(prefix + data).hexdigest()


def demand(data: bytes, expected_sha: str | None = None, expected_blob: str | None = None) -> None:
    if expected_sha is not None and digest(data) != expected_sha:
        raise ValueError("SHA256 mismatch: " + digest(data) + " expected " + expected_sha)
    if expected_blob is not None and blob(data) != expected_blob:
        raise ValueError("Git blob mismatch: " + blob(data) + " expected " + expected_blob)


def load(path: str, base: Path = ROOT) -> bytes:
    name = PurePosixPath(path)
    if name.is_absolute() or ".." in name.parts:
        raise ValueError("invalid source path: " + path)
    candidate = base.joinpath(*name.parts)
    if not candidate.is_file() or candidate.is_symlink():
        raise ValueError("missing or symlinked source: " + path)
    return candidate.read_bytes()


def stage(root: Path, path: str, data: bytes) -> None:
    target = root.joinpath(*PurePosixPath(path).parts)
    target.parent.mkdir(parents=True, exist_ok=True)
    with target.open("xb") as output:
        output.write(data)


def original_receipt_replay() -> None:
    # The unmodified historical receipt is checked, then actually executed.
    for path, old_blob in GUARDED_OLD.items():
        demand(load(path), expected_blob=old_blob)
    lines = load(TEMPLATES + "manifest.tsv").decode("utf-8").splitlines()
    manifest = []
    for line in lines:
        if line and not line.startswith("#"):
            fields = line.split("\t")
            if len(fields) != 4:
                raise ValueError("historical manifest field drift")
            manifest.append(tuple(fields))
    expected = [(path, "ABSENT", *ORIGINAL_TARGETS[path])
                for path in sorted(ORIGINAL_TARGETS)]
    if manifest != expected:
        raise ValueError("historical manifest targets drifted")

    with TemporaryDirectory(prefix="m3-original-range-replay-") as scratch:
        old_root = Path(scratch)
        for path, _, after, template in manifest:
            content = load(TEMPLATES + template)
            demand(content, expected_sha=after)
            stage(old_root, TEMPLATES + template, content)
            stage(old_root, path, content)
        stage(old_root, TEMPLATES + "manifest.tsv", load(TEMPLATES + "manifest.tsv"))
        for path, (old_blob, name) in OLD_PEERS.items():
            content = load("m3/collections/qualification/range-atom/successor/samples/" + name)
            demand(content, expected_blob=old_blob)
            stage(old_root, path, content)
        for path in (LEGACY + "verify.py", LEGACY + "CATEGORY_REVIEW.tsv", LEGACY + "RECEIPT.tsv"):
            stage(old_root, path, load(path))
        proof = subprocess.run(
            [sys.executable, "-S", str(old_root / LEGACY / "verify.py")],
            cwd=old_root, text=True, capture_output=True, check=False)
        if proof.returncode != 0 or "PASS M3_LONG_RANGE_RECEIVER targets=3" not in proof.stdout:
            raise ValueError("original immutable verifier refused: " + proof.stdout + proof.stderr)


def current_successor() -> None:
    before = load("m3/collections/qualification/range-atom/successor/samples/before-M3LongRangeSet.java.txt")
    demand(before, *BEFORE)
    for path, (sha, git_blob) in CURRENT.items():
        demand(load(path), sha, git_blob)
    for path, git_blob in CURRENT_PEER_BLOBS.items():
        demand(load(path), expected_blob=git_blob)
    if before == load(PRODUCTION + "M3LongRangeSet.java"):
        raise ValueError("successor was not applied")

    # Negative controls explicitly prove fail-closed digest guards without
    # modifying any production file or changing the original receipt.
    checked = 0
    for raw, sha, git_blob in [(before, *BEFORE)] + [
            (load(path), *pins) for path, pins in CURRENT.items()]:
        for mutant in (raw + b"\n// deliberate-source-drift\n", raw[:-1]):
            try:
                demand(mutant, sha, git_blob)
            except ValueError:
                checked += 1
            else:
                raise ValueError("mutated successor source unexpectedly admitted")
    if checked != 6:
        raise ValueError("negative control count drift")
    print("PASS M3_LONG_RANGE_SUCCESSOR original_receipt=1 preimage=1 "
          "postimage=1 junit=1 current_peers=" + str(len(CURRENT_PEER_BLOBS))
          + " drift_refusals=" + str(checked))


if __name__ == "__main__":
    try:
        original_receipt_replay()
        current_successor()
    except Exception as failure:
        print("REFUSE M3_LONG_RANGE_SUCCESSOR: " + str(failure), file=sys.stderr)
        sys.exit(1)
