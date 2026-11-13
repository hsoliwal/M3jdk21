#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe NAMING_VAR_FIX v1: one spelling for the donor-root variable of the m3/m3-import tests.

Root cause (README.md): the JDK naming rename fe35ea98841 (recipe JDK_NAMING_RENAME) applied the text rule
SEGMENT_DOT of make/m3/tooling/jdk-naming/NAMING_RULES.tsv, meant for a dotted name segment, to Java code. A local or
record component named synexia was renamed where a dot followed it (a method call on it) and kept where it was
declared or passed as an argument, so the three test classes named the variable two ways and test-compile failed
with 24 errors "cannot find symbol: variable m3". jdk_naming.py has no rule for a bare identifier (the bare word names
the donor in prose) and is a fixed point on m3/m3-import, so it cannot finish the job; this recipe does, the M3 way:
every Java identifier token synexia in the selected files becomes m3 (declarations, uses, record components and their
accessors). String literals, text blocks, character literals and comments are data and keep their bytes.

Hash-pinned: EDITS.tsv names each file with its preimage blob and sha256 and its postimage sha256. A file equal to its
preimage is rewritten and must hash to the postimage; a file equal to its postimage is left alone (idempotent);
anything else is drift and the recipe refuses (exit 2) before writing anything. A file that already declares a
variable m3 is refused (the rename would merge two variables). --check recomputes every postimage from the pinned
preimage blob (git cat-file) and compares the checkout. --emit writes EDITS.tsv from preimages at BASE_COMMIT.
Bytes and line endings are kept (only identifier tokens change).

usage: python -I apply.py [--root DIR] [--check | --emit]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "NAMING_VAR_FIX"
VERSION = "v1"
RECIPE_DIR_REL = "m3/m3-import/qualification/naming-var-fix-20261107"
BASE_COMMIT = "aa4c65c3701f5bb3c36f1f6e3bb5292d2f566c83"
ROOT_CAUSE_COMMIT = "fe35ea98841157594d9aeac71c734b2476d07da4"
SCOPE = "m3/m3-import/src/"
OLD = "synexia"
NEW = "m3"
KIND = "RENAME_IDENTIFIER"
RULE = "Java identifier token %s -> %s outside literals and comments" % (OLD, NEW)
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "tokens", "rule")
IDENTIFIER = re.compile(r"[A-Za-z_$][A-Za-z0-9_$]*")
# a declaration of NEW: a type (qualified, generic, array) then the name, then = ; , ) or : (field, parameter, local)
DECLARES_NEW = re.compile(r"(?<![\w$.])[A-Za-z_$][\w$.]*(?:<[^;=(){}]*>)?(?:\[\])*\s+%s\s*(?=[=;,):])" % NEW)
NOT_A_TYPE = {"return", "throw", "case", "yield", "assert", "else", "new", "package", "import"}


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git(root: Path, *args: str, data: bytes | None = None) -> bytes:
    return subprocess.run(["git", "-C", str(root), *args], input=data, check=True, capture_output=True).stdout


def code_mask(text: str) -> str:
    """text with every string literal, text block, character literal and comment blanked (newlines kept), so a
    position in the mask is code exactly when it is not blank there and the same position in text is code."""
    out = list(text)
    i, n = 0, len(text)

    def blank(start: int, end: int) -> None:
        for k in range(start, min(end, n)):
            if out[k] not in "\r\n":
                out[k] = " "
    while i < n:
        if text.startswith("//", i):
            end = text.find("\n", i)
            end = n if end < 0 else end
        elif text.startswith("/*", i):
            end = text.find("*/", i + 2)
            end = n if end < 0 else end + 2
        elif text.startswith('"""', i):
            end = text.find('"""', i + 3)
            while end > 0 and text[end - 1] == "\\":
                end = text.find('"""', end + 1)
            end = n if end < 0 else end + 3
        elif text[i] in "\"'":
            end = i + 1
            while end < n and text[end] != text[i] and text[end] != "\n":
                end += 2 if text[end] == "\\" else 1
            end += 1
        else:
            i += 1
            continue
        blank(i, end)
        i = end
    return "".join(out)


def identifier_spans(text: str, name: str) -> list[tuple[int, int]]:
    """(start, end) of every code identifier token equal to name."""
    mask = code_mask(text)
    return [m.span() for m in IDENTIFIER.finditer(mask) if m.group(0) == name]


def declares(mask: str, pattern: re.Pattern) -> bool:
    return any(m.group(0).split()[0] not in NOT_A_TYPE for m in pattern.finditer(mask))


def transform(data: bytes, path: str) -> tuple[bytes, int]:
    text = data.decode("utf-8", "surrogateescape")
    spans = identifier_spans(text, OLD)
    if spans and declares(code_mask(text), DECLARES_NEW):
        raise Refused("%s already declares a variable %s" % (path, NEW))
    for start, end in reversed(spans):
        text = text[:start] + NEW + text[end:]
    return text.encode("utf-8", "surrogateescape"), len(spans)


def read_edits(recipe_dir: Path) -> list[dict]:
    lines = (recipe_dir / EDITS_NAME).read_text(encoding="utf-8").splitlines()
    if tuple(lines[0].split("\t")) != EDITS_COLUMNS:
        raise Refused("EDITS.tsv header drift")
    return [dict(zip(EDITS_COLUMNS, line.split("\t"))) for line in lines[1:] if line]


def emit(root: Path, recipe_dir: Path) -> int:
    listing = git(root, "ls-tree", "-r", "-z", BASE_COMMIT, "--", SCOPE).decode("utf-8").split("\0")
    rows = ["\t".join(EDITS_COLUMNS)]
    for entry in filter(None, listing):
        meta, path = entry.split("\t", 1)
        blob = meta.split()[2]
        if not path.endswith(".java"):
            continue
        before = git(root, "cat-file", "blob", blob)
        after, tokens = transform(before, path)
        if tokens:
            rows.append("\t".join((path, KIND, blob, sha256(before), sha256(after), str(tokens), RULE)))
    (recipe_dir / EDITS_NAME).write_bytes(("\n".join(rows) + "\n").encode("utf-8"))
    print("%s %s EMIT base=%s files=%d" % (RECIPE, VERSION, BASE_COMMIT[:11], len(rows) - 1))
    return 0


def plan(root: Path, edits: list[dict]) -> list[tuple[dict, bytes | None]]:
    """(row, postimage to write or None when already applied); refuses on any drift before anything is written."""
    work = []
    for row in edits:
        data = (root / row["file"]).read_bytes()
        digest = sha256(data)
        if digest == row["after_sha256"]:
            work.append((row, None))
            continue
        if digest != row["before_sha256"]:
            raise Refused("drift %s sha256=%s" % (row["file"], digest))
        after, tokens = transform(data, row["file"])
        if sha256(after) != row["after_sha256"] or str(tokens) != row["tokens"]:
            raise Refused("postimage drift %s" % row["file"])
        work.append((row, after))
    return work


def check(root: Path, edits: list[dict]) -> int:
    bad = 0
    for row in edits:
        before = git(root, "cat-file", "blob", row["before_blob"])
        after, tokens = transform(before, row["file"])
        ok = (sha256(before) == row["before_sha256"] and sha256(after) == row["after_sha256"]
              and str(tokens) == row["tokens"] and sha256((root / row["file"]).read_bytes()) == row["after_sha256"])
        bad += not ok
        print("%s\t%s\ttokens=%s" % ("CHECK_OK" if ok else "CHECK_FAIL", row["file"], tokens))
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
        if args.emit:
            return emit(root, recipe_dir)
        edits = read_edits(recipe_dir)
        if args.check:
            return check(root, edits)
        work = plan(root, edits)
        for row, after in work:
            if after is not None:
                (root / row["file"]).write_bytes(after)
            print("%s\t%s\ttokens=%s" % ("UNCHANGED" if after is None else "CHANGED", row["file"], row["tokens"]))
        changed = sum(after is not None for _row, after in work)
        print("%s %s APPLY files=%d changed=%d" % (RECIPE, VERSION, len(work), changed))
        return 0
    except Refused as refusal:
        print("REFUSED: %s" % refusal)
        return 2


if __name__ == "__main__":
    sys.exit(main())
