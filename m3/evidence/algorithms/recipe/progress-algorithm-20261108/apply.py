#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Owner-edit recipe APPLY_PROGRESS_ALGORITHM_OWNER_EDITS v2 (hash-pinned; never by hand).

Applies the rows of EDITS.tsv to owner files of the module m3/algorithms that the relocation does not write. Each row
names a file (module-relative), an edit id, an INSERT_AFTER anchor (one exact, unique line of the owner) and the line
it inserts, and the row group of one file pins the sha256 of the whole file before (pre) and after (post) the group.
A file whose bytes hash to post is already edited (no-op); a file whose bytes hash to pre is edited, and the result
must hash to post; any other content is refused (exit 2): the owner changed since the pin, and the rows must be
re-derived, never forced. Line endings of the owner are kept. Idempotent: a second run reports changed=0; --check
writes nothing and exits 1 when a run would change a file.

Lineage: recipe/corpus-search-20261108/apply.py plus one row kind, REPLACE_LINE (v2): the anchor is one exact, unique
line of the owner and the row line replaces it (line ending kept); INSERT_AFTER is unchanged. Here the one edit makes
jdk.m3.algorithm.M3ProgressChecks public: the donor progress helper (ProgressMonitors.nonNull / checkCanceled) is
imported by 41 catalogue tooling classes bound for m3/tooling and by the five monitor-bound structures of the
collections lane, which live outside this package.

usage: python -I apply.py [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
EDITS_NAME = "EDITS.tsv"


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_edits() -> dict:
    """{file: {"pre": sha, "post": sha, "rows": [(id, anchor, line)]}} in row order."""
    files: dict = {}
    for raw in (RECIPE_DIR / EDITS_NAME).read_text(encoding="utf-8").splitlines():
        if not raw.strip() or raw.startswith("#") or raw.startswith("file\t"):
            continue
        path, edit_id, kind, anchor, line, pre, post, *_note = raw.split("\t")
        if kind not in ("INSERT_AFTER", "REPLACE_LINE"):
            raise Refused("unknown edit kind %s (%s)" % (kind, edit_id))
        group = files.setdefault(path, {"pre": pre, "post": post, "rows": []})
        if (group["pre"], group["post"]) != (pre, post):
            raise Refused("rows of %s pin different pre/post digests (%s)" % (path, edit_id))
        group["rows"].append((edit_id, kind, anchor, line))
    return files


def apply_rows(data: bytes, rows: list, path: str) -> bytes:
    lines = data.splitlines(keepends=True)
    for edit_id, kind, anchor, line in rows:
        hits = [index for index, text in enumerate(lines) if text.rstrip("\r\n".encode()).decode("utf-8") == anchor]
        if len(hits) != 1:
            raise Refused("%s: anchor occurs %d times (need 1) in %s" % (edit_id, len(hits), path))
        at = hits[0]
        eol = lines[at][len(lines[at].rstrip(b"\r\n")):] or b"\n"
        if kind == "REPLACE_LINE":
            lines[at] = line.encode("utf-8") + eol
        else:
            lines.insert(at + 1, line.encode("utf-8") + eol)
    return b"".join(lines)


def run(module: Path, check: bool) -> int:
    changed = 0
    for path, group in read_edits().items():
        target = module / path
        data = target.read_bytes()
        digest = sha256(data)
        if digest == group["post"]:
            print("EDIT UNCHANGED rows=%d %s" % (len(group["rows"]), path))
            continue
        if digest != group["pre"]:
            raise Refused("%s: sha256 %s is neither the pinned pre %s nor post %s" % (
                path, digest[:12], group["pre"][:12], group["post"][:12]))
        edited = apply_rows(data, group["rows"], path)
        if sha256(edited) != group["post"]:
            raise Refused("%s: edited sha256 %s != pinned post %s" % (path, sha256(edited)[:12], group["post"][:12]))
        if not check:
            target.write_bytes(edited)
        changed += 1
        print("EDIT CHANGED rows=%d %s" % (len(group["rows"]), path))
    print("APPLY recipe=apply.py v2 files=%d changed=%d" % (len(read_edits()), changed))
    return 1 if check and changed else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (m3/algorithms)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
