#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
"""Verify every restored JMOD SDK source blob against its exact PR #788 Git identity.

This is source custody, not Java 21 compatibility, image runtime, or release promotion.
"""
import argparse
import hashlib
import json
import re
from pathlib import Path, PurePosixPath

HEADER = "path\tgit_blob_sha1\tsource_pr\tsource_commit"
SOURCE_COMMIT = "3c7f331a65e31fdbe4c198d79c7bfcb4c226cda2"
PREFIXES = ("m3/tooling/jmod-recipes/", "m3/vendor/third_party-m3/")

# The PR #788 receipt remains immutable. These two exact successors were emitted
# and qualified by the Synexia text/JNI recipe, then received in target PR #855.
# This is a finite source-custody transition, never permission to refresh hashes.
SUCCESSOR_COMMIT = "c49cca384837ebd9dc71b6141c71ab0bcb938d10"
SUCCESSORS = {
    "m3/tooling/jmod-recipes/src/main/java/com/m3/rewrite/jmod/M3ModuleDescriptorRecipe.java": (
        "83b7ecc23b00c2fe026de54c8fa03ce9000612d3",
        "302a06c5a79ecb6fd26c988dcf1fb9147e9198e1",
        "synexia-openrewrite-recipes/src/main/java/com/m3/rewrite/jmod/M3ModuleDescriptorRecipe.java",
    ),
    "m3/tooling/jmod-recipes/tools/jmod_pack.py": (
        "6824ea4419d769b71af846c5b6eee64f9a7454d1",
        "83b1483c6c5e6a39c519f82af603382914eed3e5",
        "synexia-openrewrite-recipes/crates/m3-text-jmods/tools/jmod_pack.py",
    ),
}


def rows_from_text(text, expected_count=52):
    lines = text.splitlines()
    if not lines or lines[0] != HEADER:
        raise ValueError("SDK_RECEIVER_INVALID_HEADER")
    seen, rows = set(), []
    for line in lines[1:]:
        cells = line.split("\t")
        if len(cells) != 4:
            raise ValueError("SDK_RECEIVER_INVALID_ROW")
        name, blob, pr, commit = cells
        parts = PurePosixPath(name).parts
        if (not name.startswith(PREFIXES)
                or name.startswith("/")
                or "\\" in name
                or any(part in ("", ".", "..") for part in parts)
                or not re.fullmatch(r"[0-9a-f]{40}", blob)
                or pr != "788" or commit != SOURCE_COMMIT):
            raise ValueError("SDK_RECEIVER_INVALID_IDENTITY")
        if name in seen:
            raise ValueError("SDK_RECEIVER_DUPLICATE_PATH")
        seen.add(name)
        rows.append((name, blob))
    if len(rows) != expected_count:
        raise ValueError("SDK_RECEIVER_DENOMINATOR_MISMATCH")
    return tuple(rows)


def git_blob_sha1(data):
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    return hashlib.sha1(header + data).hexdigest()


def verify(repo, ledger, expected_count=52):
    root = Path(repo).resolve(strict=True)
    rows = rows_from_text(Path(ledger).read_text(encoding="utf-8"), expected_count)
    proof = hashlib.sha256()
    successors = []
    for name, expected in rows:
        if name in SUCCESSORS:
            predecessor, successor, canonical_path = SUCCESSORS[name]
            if expected != predecessor:
                raise ValueError("SDK_RECEIVER_SUCCESSOR_LINEAGE_DRIFT:" + name)
            expected = successor
            successors.append({
                "path": name, "predecessor_git_blob": predecessor,
                "git_blob": successor, "canonical_repository": "hsoliwal/com.synexia",
                "canonical_commit": SUCCESSOR_COMMIT, "canonical_path": canonical_path,
            })
        source = root.joinpath(*PurePosixPath(name).parts)
        if (source.is_symlink() or not source.is_file()
                or not source.resolve(strict=True).is_relative_to(root)):
            raise ValueError("SDK_RECEIVER_MISSING_OR_UNSAFE_SOURCE:" + name)
        actual = git_blob_sha1(source.read_bytes())
        if actual != expected:
            raise ValueError("SDK_RECEIVER_BLOB_DRIFT:" + name)
        proof.update(name.encode("utf-8"))
        proof.update(b"\0")
        proof.update(actual.encode("ascii"))
        proof.update(b"\n")
    return {
        "schema": "M3SDK_JMOD_RECEIVER_V1",
        "status": "SOURCE_BLOBS_VERIFIED",
        "files": len(rows),
        "source_pr": 788,
        "source_commit": SOURCE_COMMIT,
        "qualified_source_successors": successors,
        "content_root_sha256": proof.hexdigest(),
        "jdk_runtime_verified": False,
        "java21_spec_compatible": "NOT_DETERMINED_BY_SOURCE_CUSTODY",
        "product_accepted": False,
    }


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path("."))
    parser.add_argument("--ledger", type=Path)
    args = parser.parse_args(argv)
    ledger = args.ledger or args.repo / "m3/tooling/jmod-recipes/RECEIVED_SDK_JMOD_BLOBS.tsv"
    result = verify(args.repo, ledger)
    print(json.dumps(result, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
