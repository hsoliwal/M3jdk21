#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
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
    for name, expected in rows:
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
