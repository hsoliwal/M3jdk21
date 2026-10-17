#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Verify three exact historical-to-current recipe edges without rewriting receipts."""
import argparse
import hashlib
from pathlib import Path


TASK = "m3/collections/wrappers/deep-filter-list-20261017"
CURRENT = TASK + "/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3-deep-filter-list-20261017"
CURRENT_MANIFEST = "d4f4b345672f0e3f6d7e71f5dd23f6e011ed7f7e7d9e356caf67730e61573cad"
HISTORICAL_ROOT = "m3/collections/wrappers/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/"
EDGES = {
    "filtered-list": (
        HISTORICAL_ROOT + "m3jdk21-filtered-mat-receiver-20261017",
        "0d1ccc5298cab01e0717facd0d2054a98973fc8f0628b1bb504d2461be638fa3",
        "m3/collections/src/main/java/com/m3/collections/M3FilteredLists.java",
    ),
    "writable-map": (
        HISTORICAL_ROOT + "m3jdk21-writable-map-avl-20261017",
        "a9ae10da0ce772603996736b73c42d027e7a1b4b7879c105bb237fcd8c594e38",
        "m3/collections/src/main/java/com/m3/collections/M3FilteredMaps.java",
    ),
    "sorted-multiset": (
        "m3/collections/wrappers/index-counted-receiver-20261017/recipe/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-index-counted-20261017",
        "2aa484c379a428de4de13a77b722a06a8d889a0dc3248dbb930ebe14dadcd5bb",
        "m3/collections/src/main/java/com/m3/collections/M3FilteredSortedMultisets.java",
    ),
}


def sha(data):
    return hashlib.sha256(data).hexdigest()


def checked_file(root, relative):
    path = root / relative
    cursor = path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("symlinked recipe input: " + relative)
        if root not in cursor.parents:
            raise ValueError("recipe path escaped root: " + relative)
        cursor = cursor.parent
    if not path.is_file():
        raise ValueError("missing recipe input: " + relative)
    return path.read_bytes()


def manifest(root, directory, expected):
    data = checked_file(root, directory + "/manifest.tsv")
    if sha(data) != expected:
        raise ValueError("exact manifest drift: " + directory)
    rows = [line.split("\t") for line in data.decode("utf-8").splitlines()]
    if any(len(row) != 4 for row in rows):
        raise ValueError("manifest row width")
    paths = [row[0] for row in rows]
    if paths != sorted(set(paths)):
        raise ValueError("manifest duplicate or unsorted path")
    return {row[0]: row[1:] for row in rows}


def verify(root, edge):
    root = root.resolve(strict=True)
    current = manifest(root, CURRENT, CURRENT_MANIFEST)
    if len(current) != 7:
        raise ValueError("incomplete current payload")
    for path, (before, after, filename) in current.items():
        payload = checked_file(root, CURRENT + "/" + filename)
        if sha(payload) != after:
            raise ValueError("current template drift: " + path)
        if checked_file(root, path) != payload:
            raise ValueError("current output drift: " + path)
        if before != "ABSENT" and sha(checked_file(root, CURRENT + "/pre/" + filename)) != before:
            raise ValueError("current preimage drift: " + path)

    directory, historical_manifest, path = EDGES[edge]
    historical = manifest(root, directory, historical_manifest)
    _, old_after, old_filename = historical[path]
    new_before, new_after, new_filename = current[path]
    original = checked_file(root, directory + "/" + old_filename)
    if sha(original) != old_after:
        raise ValueError("historical template drift: " + path)
    if old_after != new_before:
        raise ValueError("recipe edge does not meet: " + path)
    if original != checked_file(root, CURRENT + "/pre/" + new_filename):
        raise ValueError("historical bytes do not equal current preimage: " + path)
    print("M3_DEEP_FILTER_SUCCESSOR_PASS edge=" + edge + " java_targets=7"
          + " historical=" + old_after + " current=" + new_after)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--edge", choices=tuple(EDGES), required=True)
    args = parser.parse_args()
    verify(args.root, args.edge)


if __name__ == "__main__":
    main()
