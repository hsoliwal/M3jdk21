#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Read-only, exact Git-tree coverage proof for the original 298-source M3 collection root."""
import csv
import re
import subprocess
from pathlib import Path

REVISION = "8c8dd70e659b99c23a2d0e7daeb2adadf95429c2"
PREFIX = "m3/collections/src/main/java/com/m3/collections/"
INVENTORY = Path("m3/collections/algorithm-catalogue/all-collections-20261029/SOURCE_INVENTORY.tsv")
FIELDS = ("path", "git_blob_sha1", "size_bytes", "name_family",
          "algorithm_review_cue", "open_pr_numbers", "inventory_status")


def git(*args: str) -> str:
    return subprocess.check_output(("git", *args), text=True, encoding="utf-8")


def main() -> None:
    with INVENTORY.open(encoding="utf-8", newline="") as fh:
        lines = (line for line in fh if not line.startswith("#"))
        reader = csv.DictReader(lines, delimiter="\t")
        if tuple(reader.fieldnames or ()) != FIELDS:
            raise SystemExit("invalid inventory header")
        entries = list(reader)

    actual = {}
    for line in git("ls-tree", "-r", "-l", REVISION, "--", PREFIX).splitlines():
        attrs, path = line.split("\t", 1)
        mode, kind, sha, size = attrs.split()
        if path.endswith(".java"):
            if mode != "100644" or kind != "blob":
                raise SystemExit("unexpected file mode or object kind: " + path)
            actual[path] = (sha, int(size))

    listed = [row["path"] for row in entries]
    if len(entries) != 298 or listed != sorted(set(listed)):
        raise SystemExit("file count, sort order or uniqueness mismatch")
    if set(listed) != set(actual):
        raise SystemExit("frozen inventory does not cover exact source tree")
    for row in entries:
        p = row["path"]
        if not p.startswith(PREFIX) or not re.fullmatch(r"[a-f0-9]{40}", row["git_blob_sha1"]):
            raise SystemExit("bad path or source blob: " + p)
        if (row["git_blob_sha1"], int(row["size_bytes"])) != actual[p]:
            raise SystemExit("source blob or byte length mismatch: " + p)
        if not row["name_family"] or not row["algorithm_review_cue"]:
            raise SystemExit("missing review cue: " + p)
    print(f"PASS: all {len(entries)} Java source paths pinned to {REVISION}")


if __name__ == "__main__":
    main()
