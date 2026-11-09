#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe PRECOMPUTE_JOINED_LANES v1: replay four stacked Synexia M3FS precompute recipes on jdk.m3.fs.

Receiver: m3/fs/src/main/java/jdk/m3/fs/M3FsPrecomputeBundle.java (module jdk.m3fs) and, for the memchr atom,
m3/fs/native/src/m3_fs_jni.c. The Synexia owner (indexstring fs/M3FsPrecomputeBundle.java) and this copy diverged,
so every Synexia hunk was re-sealed against the M3JDK file: EDITS.tsv pins, per stage and file, the sha256 of the
M3JDK preimage and of the postimage this recipe must produce; stages/<recipe>.edits holds the anchored edits (the
Synexia hunk, or the mapped hunk when the surrounding M3JDK code differs; DECISIONS.tsv records which).

Stages run in EDITS.tsv order (the Synexia stacking order): a file equal to a row's preimage is edited and the result
must equal the row's postimage; a file equal to the postimage, or to a later row's pre/postimage of the same file, is
already applied (no-op); anything else is a drifted preimage and the recipe refuses (exit 2) without writing. Every
OLD anchor must occur exactly once. --through stops after the named recipe (one commit per recipe), --replay rebuilds
every postimage in memory from the pinned base blobs (git cat-file) and checks the chain without touching files
(--emit writes each stage image, cumulative per file, for the differentials under qualification/).
Idempotent: a second run reports changed=0.

usage: python -I apply.py [--root DIR] [--check] [--through RECIPE] [--replay [--emit DIR]]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "recipe", "donor_hunk", "rule")
# Base blobs (M3JDK master cf0429ac1d4) of every receiver file; --replay starts from these bytes.
BASE_BLOBS = {
    "m3/fs/src/main/java/jdk/m3/fs/M3FsPrecomputeBundle.java": "73453af87a7998036d030f7b68bce0442d9a20a2",
    "m3/fs/native/src/m3_fs_jni.c": "540373e866a754250e8922e021daf33ce6d7e1b5",
}
MARK = "@@@ "


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def load_rows() -> list[dict]:
    lines = (RECIPE_DIR / EDITS_NAME).read_text(encoding="utf-8").splitlines()
    body = [line for line in lines if line and not line.startswith("#")]
    if not body or tuple(body[0].split("\t")) != EDITS_COLUMNS:
        raise Refused("EDITS.tsv header must be " + "\t".join(EDITS_COLUMNS))
    rows = [dict(zip(EDITS_COLUMNS, line.split("\t"))) for line in body[1:]]
    last: dict[str, str] = {}
    for row in rows:
        if row["file"] not in BASE_BLOBS:
            raise Refused("file outside the receiver set: " + row["file"])
        if row["file"] in last and last[row["file"]] != row["before_sha256"]:
            raise Refused("broken stacking chain at %s %s" % (row["recipe"], row["file"]))
        last[row["file"]] = row["after_sha256"]
    return rows


def load_stage(recipe: str) -> dict[str, list[tuple[str, str, str]]]:
    """file -> [(hunk header, old, new)] from stages/<recipe>.edits."""
    text = (RECIPE_DIR / "stages" / (recipe + ".edits")).read_text(encoding="utf-8")
    edits: dict[str, list[tuple[str, str, str]]] = {}
    current, header, mode, old, new = None, "", None, [], []
    for line in text.split("\n"):
        if line.startswith(MARK):
            word, _, rest = line[len(MARK):].partition(" ")
            if word == "FILE":
                current = rest.strip()
            elif word == "HUNK":
                header, mode, old, new = rest.strip(), None, [], []
            elif word in ("OLD", "NEW"):
                mode = word
            elif word == "END":
                if current is None or mode != "NEW":
                    raise Refused("malformed hunk in %s: %s" % (recipe, header))
                edits.setdefault(current, []).append((header, "\n".join(old) + "\n", "\n".join(new) + "\n"))
                mode = None
            else:
                raise Refused("unknown marker in %s: %s" % (recipe, line))
        elif mode == "OLD":
            old.append(line)
        elif mode == "NEW":
            new.append(line)
    return edits


def postimage(pre: bytes, hunks: list[tuple[str, str, str]], label: str) -> bytes:
    if b"\r\n" in pre:
        raise Refused("CRLF preimage %s (receivers are LF in git)" % label)
    text = pre.decode("utf-8")
    for header, old, new in hunks:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s, hunk %s" % (count, label, header))
        text = text.replace(old, new)
    return text.encode("utf-8")


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("base blob %s is not in this repository (%s)" % (blob, failure)) from failure


def selected(rows: list[dict], through: str | None) -> list[dict]:
    if through is None:
        return rows
    names = [row["recipe"] for row in rows]
    if through not in names:
        raise Refused("unknown recipe for --through: " + through)
    last = max(index for index, name in enumerate(names) if name == through)
    return rows[:last + 1]


def replay(root: Path, rows: list[dict], emit: Path | None = None) -> int:
    images = {path: git_blob(root, blob) for path, blob in BASE_BLOBS.items()}
    if emit is not None:
        for path, data in images.items():
            (emit / "00-base").mkdir(parents=True, exist_ok=True)
            (emit / "00-base" / Path(path).name).write_bytes(data)
    for row in rows:
        pre = images[row["file"]]
        if sha256(pre) != row["before_sha256"]:
            raise Refused("replay: %s %s preimage %s != pinned %s" % (
                row["recipe"], row["file"], sha256(pre), row["before_sha256"]))
        post = postimage(pre, load_stage(row["recipe"]).get(row["file"], []), row["file"])
        if sha256(post) != row["after_sha256"]:
            raise Refused("replay: %s %s postimage %s != pinned %s" % (
                row["recipe"], row["file"], sha256(post), row["after_sha256"]))
        images[row["file"]] = post
        if emit is not None:
            (emit / row["recipe"]).mkdir(parents=True, exist_ok=True)
            for path, data in images.items():
                (emit / row["recipe"] / Path(path).name).write_bytes(data)
        print("REPLAY OK %s %s %s" % (row["recipe"], row["file"], row["after_sha256"][:16]))
    print("PRECOMPUTE_JOINED_LANES replay rows=%d chain=OK" % len(rows))
    return 0


def run(root: Path, check: bool, through: str | None) -> int:
    rows = load_rows()
    todo = selected(rows, through)
    changed = 0
    for row in todo:
        target = root / row["file"]
        on_disk = target.read_bytes()
        digest = sha256(on_disk)
        chain = [r for r in rows if r["file"] == row["file"]]
        later = chain[chain.index(row) + 1:]
        if digest == row["after_sha256"] or any(digest in (r["before_sha256"], r["after_sha256"]) for r in later):
            state = "UNCHANGED"
        elif digest == row["before_sha256"]:
            post = postimage(on_disk, load_stage(row["recipe"]).get(row["file"], []), row["file"])
            if sha256(post) != row["after_sha256"]:
                raise Refused("postimage drift %s %s: produced %s, pinned %s" % (
                    row["recipe"], row["file"], sha256(post), row["after_sha256"]))
            state = "CHANGED"
            if not check:
                target.write_bytes(post)
        else:
            raise Refused("drifted preimage %s for %s: on disk %s, pinned %s" % (
                row["file"], row["recipe"], digest, row["before_sha256"]))
        changed += state == "CHANGED"
        print("%s %s %s" % (row["recipe"], state, row["file"]))
    print("PRECOMPUTE_JOINED_LANES recipe=%s %s rows=%d/%d through=%s changed=%d" % (
        RECIPE, VERSION, len(todo), len(rows), through or "ALL", changed))
    return 1 if check and changed else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--through", help="apply the rows up to and including this recipe")
    parser.add_argument("--replay", action="store_true", help="rebuild the chain from the base blobs in memory")
    parser.add_argument("--emit", help="with --replay: write every stage image to DIR/<recipe>/<file name>")
    args = parser.parse_args(argv)
    try:
        root = Path(args.root).resolve()
        if args.replay:
            return replay(root, selected(load_rows(), args.through), Path(args.emit) if args.emit else None)
        return run(root, args.check, args.through)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
