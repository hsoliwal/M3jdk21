#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Prevent a previously merged M3 recipe atom disappearing from the product tree.

Historical recipe images are checked byte-for-byte by Git blob identity. Evolving
runtime Java owners are checked for an admitted capability marker instead of a
permanent frozen digest. This is a preservation gate, NOT behavioral qualification.
"""
import argparse
import hashlib
from pathlib import Path
import re
import sys

MANIFEST = Path(__file__).with_name("merged-atoms-preservation-20261022.tsv")


def git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\x00" + data).hexdigest()


def verify(root: Path, manifest: Path) -> int:
    root = root.resolve(strict=True)
    count = 0
    previous = ""
    for line_number, line in enumerate(manifest.read_text(encoding="utf-8").splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if len(fields) != 4:
            raise AssertionError(f"invalid preservation manifest row {line_number}")
        pr, relative, check, expected = fields
        if not pr.isdecimal() or check not in ("PRESENT", "REGEX", "SHA1"):
            raise AssertionError(f"unknown preservation check at row {line_number}")
        candidate = Path(relative)
        if (candidate.is_absolute() or not relative or relative <= previous
                or any(part in ("..", ".") for part in candidate.parts)):
            raise AssertionError(f"unsorted/unsafe preservation path at row {line_number}")
        previous = relative
        path = root / candidate
        if path.is_symlink() or not path.is_file() or not path.resolve().is_relative_to(root):
            raise AssertionError(f"lost merged PR #{pr} file: {relative}")
        data = path.read_bytes()
        if check == "SHA1":
            if not re.fullmatch("[0-9a-f]{40}", expected) or git_blob_sha1(data) != expected:
                raise AssertionError(f"changed pinned historical recipe from PR #{pr}: {relative}")
        elif check == "REGEX":
            if re.search(expected, data.decode("utf-8"), flags=re.MULTILINE) is None:
                raise AssertionError(f"lost merged capability from PR #{pr}: {relative}")
        elif expected != "-":
            raise AssertionError(f"invalid PRESENT expectation at row {line_number}")
        count += 1
    if count < 20:
        raise AssertionError("merged preservation manifest unexpectedly incomplete")
    return count


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--manifest", type=Path, default=MANIFEST)
    args = parser.parse_args()
    try:
        checked = verify(args.root, args.manifest)
    except (OSError, ValueError, UnicodeError, AssertionError) as error:
        print("M3_MERGED_ATOM_PRESERVATION_FAIL", error, file=sys.stderr)
        raise SystemExit(1) from error
    print(f"M3_MERGED_ATOM_PRESERVATION_PASS checked={checked}")


if __name__ == "__main__":
    main()
