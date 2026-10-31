#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Verify immutable 2026 Synexia handoff and exactly reviewed M3JDK21 successor.

This checks source byte custody, NOT runtime behavior or release qualification.
The historical five-row receipt must remain byte-identical to its merged Git blob.
"""
from __future__ import annotations

import argparse
import hashlib
from pathlib import Path, PurePosixPath
import re

HISTORY = "m3/docs/synexia-m3jdk21-eclipse-recipe-handoff-20261022.tsv"
SUCCESSIONS = "m3/docs/synexia-m3jdk21-source-successions.tsv"
HISTORY_BLOB = "4dfffd4cae11edf342b801ca3c007c43a6137979"
EVOLVED_OWNER = "m3/file/src/main/java/com/m3/file/M3ResourceLocks.java"
HISTORICAL_OWNER_SHA = "18d05ae54945d4129c46d62e26551cbc4fd200b31c185e020b9ee6240fdc9599"
CURRENT_OWNER_SHA = "eb0bf36625effaffa147be1f650590dfa08627a5f6f52d105b8fbb201d6b5718"
PRODUCT_PR = "hsoliwal/M3jdk21#1821"
CANONICAL_PR = "hsoliwal/com.synexia#12842"
STATE = "SOURCE_SUCCESSOR_RECEIVED_FULL_RUNTIME_QUALIFICATION_PENDING"
RIGHTS = "Apache-2.0"
HEX64 = re.compile(r"[0-9a-f]{64}\Z")


class HandoffRefusal(ValueError):
    """Malformed, missing or unexpected source custody."""


def checked_path(root: Path, relative: str) -> Path:
    path = PurePosixPath(relative)
    if (not relative or path.is_absolute() or "\\" in relative
            or any(part in (".", "..") for part in path.parts)
            or path.as_posix() != relative):
        raise HandoffRefusal("unsafe custody path: " + relative)
    candidate = root.joinpath(*path.parts)
    # A lexical path with an in-tree symlinked ancestor is still an alias:
    # checking only the final filename would admit a missing physical owner.
    # No source/custody path component may be a symlink.
    ancestor = root
    for part in path.parts:
        ancestor = ancestor / part
        if ancestor.is_symlink():
            raise HandoffRefusal("missing or symlinked custody input: " + relative)
    if (not candidate.is_file()
            or not candidate.resolve().is_relative_to(root)):
        raise HandoffRefusal("missing or symlinked custody input: " + relative)
    return candidate


def data_lines(source: str) -> list[str]:
    return [line for line in source.splitlines() if line and not line.startswith("#")]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def verify(root: Path) -> tuple[int, int]:
    root = root.resolve(strict=True)
    historical = checked_path(root, HISTORY).read_bytes()
    if git_blob_sha1(historical) != HISTORY_BLOB:
        raise HandoffRefusal("historical canonical handoff must be immutable")
    rows = data_lines(historical.decode("utf-8"))
    if len(rows) != 5:
        raise HandoffRefusal("historical handoff must contain exactly five Java paths")

    successor_lines = data_lines(checked_path(root, SUCCESSIONS).read_text(encoding="utf-8"))
    if len(successor_lines) != 1:
        raise HandoffRefusal("exactly one documented successor is currently admitted")
    successor = successor_lines[0].split("\t")
    if len(successor) != 7:
        raise HandoffRefusal("invalid successor record shape")
    path, old, new, product, canonical, state, rights = successor
    if ((path, old, new, product, canonical, state, rights) !=
            (EVOLVED_OWNER, HISTORICAL_OWNER_SHA, CURRENT_OWNER_SHA,
             PRODUCT_PR, CANONICAL_PR, STATE, RIGHTS)):
        raise HandoffRefusal("unreviewed successor origin, source digest or contract")

    last = ""
    saw_successor = False
    for line in rows:
        cols = line.split("\t")
        if len(cols) != 8:
            raise HandoffRefusal("invalid original handoff record shape")
        (relative, recipe, previous, original_sha, origin,
         review, admission, license_id) = cols
        if relative <= last or not relative.endswith(".java"):
            raise HandoffRefusal("unsorted or invalid historical Java path")
        last = relative
        if (not recipe.startswith("com.synexia.m3.")
                or (previous != "ABSENT" and not HEX64.fullmatch(previous))
                or not HEX64.fullmatch(original_sha)
                or license_id != RIGHTS or review != "hsoliwal/com.synexia#12951"
                or admission not in ("PENDING_RECIPE_AND_PRODUCT_CI", "QUALIFIED")):
            raise HandoffRefusal("invalid immutable first-party recipe custody")
        observed = sha256(checked_path(root, relative).read_bytes())
        if observed == original_sha:
            if relative == EVOLVED_OWNER:
                raise HandoffRefusal("recorded successor is not present at its current owner")
        elif (relative == path and original_sha == old and observed == new):
            saw_successor = True
        else:
            raise HandoffRefusal("unexpected Java product source drift: " + relative)
    if not saw_successor:
        raise HandoffRefusal("reviewed newer M3ResourceLocks owner was not found")
    return len(rows), 1


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    args = parser.parse_args()
    try:
        historical, successors = verify(args.root)
    except (HandoffRefusal, OSError, UnicodeError) as problem:
        print("M3_PRODUCT_SOURCE_RECIPE_HANDOFF_REFUSED", str(problem))
        return 1
    print("M3_PRODUCT_SOURCE_RECIPE_LINEAGE_PASS",
          f"historical={historical}", f"successors={successors}",
          "runtime_release=NOT_QUALIFIED")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
