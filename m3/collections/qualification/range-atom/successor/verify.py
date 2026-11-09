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
             "jdk/internal/m3/rewrite/hash-pinned-java/"
             "m3jdk21-long-range-array-flags-20261017/")
PRODUCTION = "m3/collections/src/main/java/jdk/m3/collections/"
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
NATIVE = "m3/collections/src/main/native/collections/"

ORIGINAL_TARGETS = {
    PRODUCTION + "M3LongRangeSet.java": (
        "11a1001d54bbd39d25102dab36b9ee7a9125713f8a1496598655f41f9c47652e",
        "M3LongRangeSet.java.txt"),
    TEST + "M3LongRangeSetSelfTest.java": (
        "420496cf602f3f08f8aef10a9ff73d1e104d7582a19be18d697f1898025595cb",
        "M3LongRangeSetSelfTest.java.txt"),
    TEST + "M3LongRangeSetTest.java": (
        "b57500ea01b78f1db7c5deb809758116ec27c99f23d108b04431c3fbdadf6b4d",
        "M3LongRangeSetTest.java.txt"),
}
OLD_PEERS = {
    PRODUCTION + "M3IndexPathMasks.java":
        ("be8838c2ad26aac24453e05a317d63016bca100b", "M3IndexPathMasks.java.txt"),
    PRODUCTION + "M3LongLongConsumer.java":
        ("8ce5558a5d660b7f8a206dde3b59d1b30c45aab4", "M3LongLongConsumer.java.txt"),
    NATIVE + "m3_index_path_masks.c":
        ("69aef883c09997bae30825d7ad8757bc2358bf69", "m3_index_path_masks.c.txt"),
}
GUARDED_OLD = {
    LEGACY + "verify.py": "37cafaecdf9ca96aed3087eef44d4250bac7ae76",
    LEGACY + "CATEGORY_REVIEW.tsv": "f1ed7acefae57e3baf76dd0d1b42dcda83db04c0",
    LEGACY + "RECEIPT.tsv": "18b18a9149c27a6d92b9548cc4315ca7f920b791",
    TEMPLATES + "manifest.tsv": "78ad6150a339f579d9361e67a60ca9d70ad88f6b",
}
CURRENT = {
    PRODUCTION + "M3LongRangeSet.java": (
        "72d57ce6b78845712336d7a8c44642ee13b327d85d95ca9a89e632fec23dd285",
        "ad7826be14ba09539e55125f52a6bea7a58ee362"),
    TEST + "M3LongRangeSetAtomTest.java": (
        "b2c6890df96006ef2038f44a4b1e27574dd00a25cebc497bbe5e2edb45037385",
        "29529c500602c6a03bdf4d5fc6562bbcab692b36"),
}
CURRENT_PEER_BLOBS = {
    PRODUCTION + "M3IndexPathMasks.java": "1adc99a3825acb8c60c31769199b3bd01ef0c25a",
    PRODUCTION + "M3LaneSearch.java": "f84bf3bd1cdd2105b6ba56b6dd887f9006eaaec1",
    PRODUCTION + "M3LongLongConsumer.java": "8ce5558a5d660b7f8a206dde3b59d1b30c45aab4",
    NATIVE + "m3_index_path_masks.c": "1bf825b0b55d898578092b0250196d95dbb93a90",
    TEST + "M3LongRangeSetSelfTest.java": "e9d18b07711ab54f8411697827921910b7c64b86",
    TEST + "M3LongRangeSetTest.java": "7864c4d62e9cb113a22aad31cef643e28461d96c",
}
BEFORE = ("9656b3a699f638dfe0a175a35546cc4d747f2e2e8caccfa8576f06d3929665cb",
          "0db5f522879f739c4c754608911f05620e713ba9")


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
