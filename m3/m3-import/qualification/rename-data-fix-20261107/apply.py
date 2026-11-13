#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe RENAME_DATA_FIX v1: the m3/m3-import data and the renamed Java read the same names again.

The JDK naming rename fe35ea98841 (recipe JDK_NAMING_RENAME) renamed the Java of m3/m3-import but kept its TSV data
(lineage is out of the rename's scope) and missed two literal shapes (a path segment that ends the literal, a dotted
segment before a wildcard or the end). README.md holds the per-file decisions and their evidence.

STEPS.tsv is the whole recipe, applied per file in table order:
  LITERAL    file old new count   exact substring replacement; the preimage must hold old exactly count times
  NAMING     file HEADER|- cols   the naming law itself (jdk_naming.rewrite_line and NAMING_RULES.tsv, both read from
                                  BASE_COMMIT so a later rule cannot change the result) on the
                                  header line (HEADER) and on the listed tab columns of every data row (ALL: whole
                                  rows); comment lines (#) and every other column keep their bytes
  SEED_ROOT  file old new         recompute the "# root" line of an M3_DELIVERY_EXPORT_V1 manifest over its rows;
                                  the old line must hold old and the result must equal new
Hash-pinned: EDITS.tsv names every file with its preimage blob and sha256 at BASE_COMMIT and its postimage sha256.
A file at its preimage is rewritten and must hash to its postimage; a file at its postimage is left alone
(idempotent); anything else is drift and nothing is written (exit 2). --check recomputes every postimage from the
pinned preimage blob (git cat-file) and compares the checkout. --emit writes EDITS.tsv from BASE_COMMIT.

usage: python -I apply.py [--root DIR] [--check | --emit]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
import types
from pathlib import Path

RECIPE = "RENAME_DATA_FIX"
VERSION = "v1"
BASE_COMMIT = "dc44fdbc244bc55402ed097010496e571a258a7e"
NAMING_TOOL = "make/m3/tooling/jdk-naming/jdk_naming.py"
NAMING_RULES = "make/m3/tooling/jdk-naming/NAMING_RULES.tsv"
STEPS_NAME = "STEPS.tsv"
EDITS_NAME = "EDITS.tsv"
STEP_COLUMNS = ("kind", "file", "old", "new", "count", "reason")
EDITS_COLUMNS = ("file", "before_blob", "before_sha256", "after_sha256", "steps")
FRAME = "M3_DELIVERY_EXPORT_V1"
ROOT_LINE = "# root\t"
MANIFEST_CELLS = 8


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git(root: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True).stdout


class _Rules:
    """The rule table as jdk_naming.load_rules reads it (read_text), taken from a blob instead of the checkout."""

    def __init__(self, text: str):
        self.text = text

    def read_text(self, encoding: str = "utf-8") -> str:
        return self.text


def naming_law(root: Path):
    """jdk_naming.py and NAMING_RULES.tsv exactly as at BASE_COMMIT: a later rule (SEGMENT_DOT_END) must not change
    what this pinned recipe writes, so its postimages stay reproducible."""
    module = types.ModuleType("jdk_naming_at_base")
    module.__file__ = str(root / NAMING_TOOL)
    source = git(root, "show", "%s:%s" % (BASE_COMMIT, NAMING_TOOL)).decode("utf-8")
    exec(compile(source, NAMING_TOOL, "exec"), module.__dict__)
    rules = git(root, "show", "%s:%s" % (BASE_COMMIT, NAMING_RULES)).decode("utf-8")
    (module.TEXT_RULES, module.PROSE_RULES, module.KEEP_LINE, module.KEEP_SPAN,
     module.KEEP_PATH) = module.load_rules(_Rules(rules))
    return module


def read_table(path: Path, columns: tuple) -> list[dict]:
    lines = path.read_text(encoding="utf-8").splitlines()
    if tuple(lines[0].split("\t")) != columns:
        raise Refused("%s header drift" % path.name)
    return [dict(zip(columns, line.split("\t"))) for line in lines[1:] if line]


def literal(text: str, step: dict) -> str:
    found = text.count(step["old"])
    if found != int(step["count"]):
        raise Refused("%s: %r found %d times, expected %s" % (step["file"], step["old"], found, step["count"]))
    return text.replace(step["old"], step["new"])


def naming(text: str, step: dict, law) -> str:
    columns = None if step["new"] == "ALL" else [int(c) for c in step["new"].split(",")]
    lines = text.split("\n")
    for index, line in enumerate(lines):
        if not line or line.startswith("#"):
            continue
        if index == 0:
            lines[0] = law.rewrite_line(line, {}) if step["old"] == "HEADER" else line
        elif columns is None:
            lines[index] = law.rewrite_line(line, {})
        else:
            cells = line.split("\t")
            for column in columns:
                cells[column] = law.rewrite_line(cells[column], {})
            lines[index] = "\t".join(cells)
    return "\n".join(lines)


def frame(digest, value: str) -> None:
    data = value.encode("utf-8")
    digest.update(len(data).to_bytes(4, "big"))
    digest.update(data)


def delivery_root(rows: list[list[str]]) -> str:
    """M3ImportManifest.root: revision, target id, then each entry sorted by target path."""
    digest = hashlib.sha256()
    for value in (FRAME, rows[0][0].lower(), rows[0][1]):
        frame(digest, value)
    for row in sorted(rows, key=lambda cells: cells[4]):
        for value in (row[2], row[3], row[4], row[5].lower(), row[6], row[7]):
            frame(digest, value)
    return digest.hexdigest()


def seed_root(text: str, step: dict) -> str:
    lines = text.split("\n")
    rows = [line.split("\t") for line in lines[1:] if line and not line.startswith("#")]
    if any(len(row) != MANIFEST_CELLS for row in rows):
        raise Refused("%s: manifest row width" % step["file"])
    marks = [i for i, line in enumerate(lines) if line.startswith(ROOT_LINE)]
    if len(marks) != 1 or lines[marks[0]] != ROOT_LINE + step["old"]:
        raise Refused("%s: root line drift" % step["file"])
    computed = delivery_root(rows)
    if computed != step["new"]:
        raise Refused("%s: computed root %s, recipe pins %s" % (step["file"], computed, step["new"]))
    lines[marks[0]] = ROOT_LINE + computed
    return "\n".join(lines)


def transform(data: bytes, steps: list[dict], law) -> bytes:
    text = data.decode("utf-8", "surrogateescape")
    for step in steps:
        if step["kind"] == "LITERAL":
            text = literal(text, step)
        elif step["kind"] == "NAMING":
            text = naming(text, step, law)
        elif step["kind"] == "SEED_ROOT":
            text = seed_root(text, step)
        else:
            raise Refused("unknown step kind %s" % step["kind"])
    return text.encode("utf-8", "surrogateescape")


def by_file(steps: list[dict]) -> dict:
    files: dict = {}
    for step in steps:
        files.setdefault(step["file"], []).append(step)
    return files


def emit(root: Path, recipe_dir: Path, files: dict, law) -> int:
    rows = ["\t".join(EDITS_COLUMNS)]
    for path, steps in files.items():
        blob = git(root, "rev-parse", "%s:%s" % (BASE_COMMIT, path)).decode().strip()
        before = git(root, "cat-file", "blob", blob)
        after = transform(before, steps, law)
        rows.append("\t".join((path, blob, sha256(before), sha256(after), str(len(steps)))))
    (recipe_dir / EDITS_NAME).write_bytes(("\n".join(rows) + "\n").encode("utf-8"))
    print("%s %s EMIT base=%s files=%d" % (RECIPE, VERSION, BASE_COMMIT[:11], len(rows) - 1))
    return 0


def plan(root: Path, edits: list[dict], files: dict, law) -> list:
    work = []
    for row in edits:
        data = (root / row["file"]).read_bytes()
        digest = sha256(data)
        if digest == row["after_sha256"]:
            work.append((row, None))
            continue
        if digest != row["before_sha256"]:
            raise Refused("drift %s sha256=%s" % (row["file"], digest))
        after = transform(data, files[row["file"]], law)
        if sha256(after) != row["after_sha256"]:
            raise Refused("postimage drift %s" % row["file"])
        work.append((row, after))
    return work


def check(root: Path, edits: list[dict], files: dict, law) -> int:
    bad = 0
    for row in edits:
        before = git(root, "cat-file", "blob", row["before_blob"])
        after = transform(before, files[row["file"]], law)
        ok = (sha256(before) == row["before_sha256"] and sha256(after) == row["after_sha256"]
              and sha256((root / row["file"]).read_bytes()) == row["after_sha256"])
        bad += not ok
        print("%s\t%s" % ("CHECK_OK" if ok else "CHECK_FAIL", row["file"]))
    print("%s %s CHECK files=%d failed=%d" % (RECIPE, VERSION, len(edits), bad))
    return 0 if bad == 0 else 2


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--emit", action="store_true")
    args = parser.parse_args(argv)
    root, recipe_dir = Path(args.root).resolve(), Path(__file__).resolve().parent
    try:
        law = naming_law(root)
        files = by_file(read_table(recipe_dir / STEPS_NAME, STEP_COLUMNS))
        if args.emit:
            return emit(root, recipe_dir, files, law)
        edits = read_table(recipe_dir / EDITS_NAME, EDITS_COLUMNS)
        if sorted(row["file"] for row in edits) != sorted(files):
            raise Refused("EDITS.tsv and STEPS.tsv name different files")
        if args.check:
            return check(root, edits, files, law)
        work = plan(root, edits, files, law)
        for row, after in work:
            if after is not None:
                (root / row["file"]).write_bytes(after)
            print("%s\t%s" % ("UNCHANGED" if after is None else "CHANGED", row["file"]))
        changed = sum(after is not None for _row, after in work)
        print("%s %s APPLY files=%d changed=%d" % (RECIPE, VERSION, len(work), changed))
        return 0
    except Refused as refusal:
        print("REFUSED: %s" % refusal)
        return 2


if __name__ == "__main__":
    sys.exit(main())
