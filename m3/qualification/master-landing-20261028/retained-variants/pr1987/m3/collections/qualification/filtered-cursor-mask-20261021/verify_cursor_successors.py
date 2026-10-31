#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify immutable original cursor-mask receipt and exact reviewed successors.

This is a verification-only product result. Executable source-transformation recipes,
preimages, postimages and fixed-point tests remain exclusively in Synexia.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path, PurePosixPath
import re

ORIGINAL_MANIFEST = "m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_MANIFEST.tsv"
SUCCESSOR_MANIFEST = "m3/collections/qualification/filtered-cursor-mask-20261021/SOURCE_SUCCESSIONS.tsv"
ORIGINAL_BLOB = "2ffe4f870d6ca59156aa18acd345640b747dbcc6"
STATE = "SOURCE_IDENTITY_VERIFIED_RUNTIME_QUALIFICATION_PENDING"
HEX = re.compile(r"[0-9a-f]{64}\Z")
BLOB = re.compile(r"[0-9a-f]{40}\Z")

# Immutable first-party successor candidates, in current original JDK owners.
# Explicitly no blanket permission to rewrite old manifests or accept new hashes.
EXPECTED_SUCCESSORS = (
    (
        "m3/collections/src/main/java/com/m3/collections/M3FilteredDeques.java",
        "4b3e3b9009d69223358e90b1fd57e705644b2efbf1ed3e3fcefca52b4dcc4c8f",
        "5f119d62cead081d6e60c847b49c588447b131980a88b5b8a396453beb3a7382",
        "44d8c21fa93edfcdd5f8d98a05bf62f48ec263fa",
        "5fcc063d77ebf16efc23a4511d6fa66c00888b61",
        "hsoliwal/M3jdk21:original-first-party-deque-late-bound-spliterator",
        STATE,
        "Apache-2.0 OR EPL-2.0",
    ),
    (
        "m3/collections/src/main/java/com/m3/collections/M3FilteredViews.java",
        "02c8a766ad9acae90b2a3964a24b00e874a598687d90dfda3242a52b78a3a454",
        "e1a61906a898c5d1ee5ee4e38b365db8cb28f38026aba766160bd517a6b49fca",
        "445ea988d4df4d348db6566e18f0599bbd792d04",
        "25e1ba23e0269cd6226e9f0b76fd9a43c6bb1d59",
        "hsoliwal/M3jdk21:original-first-party-filtered-tree-range",
        STATE,
        "Apache-2.0 OR EPL-2.0",
    ),
    (
        "src/java.base/share/classes/java/util/CollectionViews.java",
        "ada66ee22610fc7ab54098e4a754e7e1140b5c1cc01af1341d9c2530769989a4",
        "e09db07fb113233e52a67960411608d8b18dc0eedfc5a7f861cdaecc7c1cc1e6",
        "ab1723f1f137249481023d5ea6a796bf7039b695",
        "5c39dc53d3bbd80eada25f74b960e22391eeecb6",
        "hsoliwal/M3jdk21:original-java-base-collectionviews-convergence",
        STATE,
        "Apache-2.0",
    ),
)


class SourceRefusal(ValueError):
    """Fail-closed lineage or byte-custody mismatch."""


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def regular_source(root: Path, relative: str) -> bytes:
    p = PurePosixPath(relative)
    if (not relative or p.is_absolute() or p.as_posix() != relative
            or "\\" in relative or any(part in (".", "..") for part in relative.split("/"))):
        raise SourceRefusal("unsafe relative path: " + relative)
    f = root.joinpath(*p.parts)
    if f.is_symlink() or not f.is_file() or not f.resolve().is_relative_to(root):
        raise SourceRefusal("missing or symlinked source: " + relative)
    return f.read_bytes()


def data_rows(content: bytes) -> list[tuple[str, ...]]:
    return [tuple(line.split("\t")) for line in content.decode("utf-8").splitlines()
            if line and not line.startswith("#")]


def sorted_unique(rows: list[tuple[str, ...]]) -> bool:
    return all(rows[i][0] < rows[i + 1][0] for i in range(len(rows) - 1))


def verify(
    root: Path,
    *,
    historical_blob: str = ORIGINAL_BLOB,
    successors: tuple[tuple[str, ...], ...] = EXPECTED_SUCCESSORS,
) -> tuple[int, int]:
    root = root.resolve(strict=True)
    source_manifest = regular_source(root, ORIGINAL_MANIFEST)
    if git_blob(source_manifest) != historical_blob:
        raise SourceRefusal("original eight-path source manifest was rewritten")
    original = data_rows(source_manifest)
    if (len(original) != 8 or not sorted_unique(original)
            or sum(row[1] != "ABSENT" for row in original) != 4):
        raise SourceRefusal("original source inventory cardinality/order changed")
    successor_rows = data_rows(regular_source(root, SUCCESSOR_MANIFEST))
    if tuple(successor_rows) != successors:
        raise SourceRefusal("successor origin, license, status or source digest is unreviewed")
    if len(successors) != 3 or not sorted_unique(successor_rows):
        raise SourceRefusal("unexpected extra or unordered successor")
    next_by_path = {row[0]: row for row in successors}
    if len(next_by_path) != 3:
        raise SourceRefusal("duplicate successor target")
    used = set()
    for row in original:
        if (len(row) != 5 or not row[0].endswith(".java")
                or (row[1] != "ABSENT" and not HEX.fullmatch(row[1]))
                or not HEX.fullmatch(row[2]) or not BLOB.fullmatch(row[3])
                or not re.fullmatch(r"[A-Za-z0-9_.-]+", row[4])):
            raise SourceRefusal("original source manifest row invalid: " + row[0])
        data = regular_source(root, row[0])
        current_sha, current_blob = sha256(data), git_blob(data)
        approved = next_by_path.get(row[0])
        if approved is None:
            if current_sha != row[2] or current_blob != row[3]:
                raise SourceRefusal("unchanged original Java source drift: " + row[0])
        else:
            path, old_sha, new_sha, old_blob, new_blob, _, state, license_id = approved
            if (old_sha != row[2] or old_blob != row[3]
                    or not HEX.fullmatch(new_sha) or not BLOB.fullmatch(new_blob)
                    or state != STATE
                    or license_id not in ("Apache-2.0", "Apache-2.0 OR EPL-2.0")
                    or current_sha != new_sha or current_blob != new_blob):
                raise SourceRefusal("unauthorized original-owner successor: " + path)
            used.add(path)
    if used != set(next_by_path):
        raise SourceRefusal("successor does not belong to original eight-path inventory")
    return len(original), len(used)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    args = parser.parse_args()
    try:
        original, successor = verify(args.root)
    except (SourceRefusal, OSError, UnicodeError) as failure:
        print("M3_FILTERED_CURSOR_SOURCE_REFUSED", str(failure))
        return 1
    print("M3_FILTERED_CURSOR_SOURCE_SUCCESSION_PASS", f"original={original}",
          f"successors={successor}", "release=HOLD")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
