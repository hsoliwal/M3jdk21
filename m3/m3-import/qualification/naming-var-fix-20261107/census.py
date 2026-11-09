#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Census HALF_RENAMED_IDENTIFIER v1: Java methods that declare one spelling of the donor-root variable and use the
other (the defect NAMING_VAR_FIX repairs in m3/m3-import; README.md).

METHOD rows: for every Java blob of REV under PREFIX whose code names the bare identifier synexia, each method-like
body (the outermost block opened after a parameter list, with that list) is checked both ways: a declaration of one
name (field, parameter, local, enhanced-for or lambda variable) together with a simple-name use of the other name
that no declaration in the body, its parameters or the file's fields resolves. UNDECLARED rows: a simple-name
receiver m3 followed by a dot and a letter (the only form the rename wrote) in a file that declares no m3 at all.
Code only: literals and comments are blanked by apply.code_mask. A method call named like the variable and a member
access (a dot before it) are not uses. Blobs are read with git cat-file --batch under GIT_NO_LAZY_FETCH=1, so a
blobless clone never fetches; a missing blob is reported as MISSING, never guessed.
Output: HITS.tsv rows (kind, file, line, method, declared, used, use_lines).

usage: python -I census.py [--root DIR] [--rev REV] [--prefix P] [--out HITS.tsv]
"""
from __future__ import annotations

import argparse
import importlib.util
import os
import re
import subprocess
import sys
from pathlib import Path

CENSUS = "HALF_RENAMED_IDENTIFIER"
VERSION = "v1"
NAMES = ("synexia", "m3")
COLUMNS = ("kind", "file", "line", "method", "declared", "used", "use_lines")
# the only form the rename produced: SEGMENT_DOT wrote m3 where a dot and a letter followed the bare word
RECEIVER = re.compile(r"(?<![\w$.])m3\s*\.(?=[A-Za-z])")
RECEIVER_GREP = r"(^|[^.[:alnum:]_$])m3[[:space:]]*\.[[:alpha:]]"
sys.dont_write_bytecode = True  # loading apply.py must not leave __pycache__ in the checkout
_SPEC = importlib.util.spec_from_file_location("naming_var_fix_apply", Path(__file__).with_name("apply.py"))
APPLY = importlib.util.module_from_spec(_SPEC)
_SPEC.loader.exec_module(APPLY)
TYPE = r"[A-Za-z_$][\w$.]*(?:<[^;=(){}]*>)?(?:\[\])*"
NOT_A_TYPE = {"return", "throw", "case", "yield", "assert", "else", "new", "package", "import"}
IDENT = re.compile(r"[A-Za-z_$][\w$]*")
METHOD_NAME = re.compile(r"([A-Za-z_$][\w$]*)\s*$")
# the ")" closing a parameter list, then an optional throws clause, then the "{" (searched in a bounded window)
THROWS = re.compile(r"\)(?=\s*(?:throws\s+[\w$.,<>\s]{1,400}?)?\s*$)")
WINDOW = 512
CONTROL = {"if", "for", "while", "switch", "catch", "synchronized", "try"}
DIRECTIVES = ("package ", "import ", "module ", "open module ", "requires ", "exports ", "opens ", "uses ",
              "provides ")


def declarations(mask: str, name: str) -> list[int]:
    # typed: field, parameter, local, enhanced-for, record component, instanceof pattern (x instanceof T name && ..)
    typed = re.compile(r"(?<![\w$.])(%s)\s+%s\s*(?=[=;,):?&|])" % (TYPE, name))
    # another declarator of a declaration list (Path a = .., name = ..) and a lambda parameter
    more = re.compile(r",\s*%s\s*=(?!=)|(?<![\w$.])%s\s*->|[(,]\s*%s\s*(?=[,)][^;{}]*->)" % (name, name, name))
    found = [m.end() - len(name) for m in typed.finditer(mask) if m.group(1).split(".")[0] not in NOT_A_TYPE]
    return found + [m.start() for m in more.finditer(mask)]


def directive(mask: str, pos: int) -> bool:
    """A package, import or module-info directive line names packages and modules, not variables."""
    return mask[mask.rfind("\n", 0, pos) + 1:pos].lstrip().startswith(DIRECTIVES)


def uses(mask: str, name: str) -> list[int]:
    out = []
    for m in IDENT.finditer(mask):
        if m.group(0) != name:
            continue
        before = mask[max(0, m.start() - WINDOW):m.start()].rstrip()
        after = mask[m.end():m.end() + WINDOW].lstrip()
        if before.endswith(".") or after.startswith(("(", ".*")) or directive(mask, m.start()):
            continue
        out.append(m.start())
    return out


def matching(mask: str, open_at: int, pair: str) -> int:
    depth = 0
    for i in range(open_at, len(mask)):
        depth += (mask[i] == pair[0]) - (mask[i] == pair[1])
        if depth == 0:
            return i
    return len(mask) - 1


def method_bodies(mask: str) -> list[tuple[int, int, int, str]]:
    """(param_start, body_end, line, name) of every outermost block opened after a parameter list."""
    bodies, end = [], -1
    for brace in re.finditer(r"\{", mask):
        i = brace.start()
        if i <= end:
            continue
        head = THROWS.search(mask, max(0, i - WINDOW), i)
        if not head:
            continue
        close, depth, start = head.start(), 0, -1
        for k in range(close, -1, -1):
            depth += (mask[k] == ")") - (mask[k] == "(")
            if depth == 0:
                start = k
                break
        name = METHOD_NAME.search(mask[:start])
        if start < 0 or not name or name.group(1) in CONTROL:
            continue
        end = matching(mask, i, "{}")
        bodies.append((start, end, mask.count("\n", 0, start) + 1, name.group(1)))
    return bodies


def hits(path: str, text: str) -> list[tuple]:
    mask = APPLY.code_mask(text)
    if not any(m.group(0) == NAMES[0] for m in IDENT.finditer(mask)):
        return undeclared(path, mask, set())
    bodies = method_bodies(mask)
    inside = lambda pos: any(s <= pos <= e for s, e, _l, _n in bodies)
    rows = []
    for declared, used in (NAMES, NAMES[::-1]):
        fields = [p for p in declarations(mask, used) if not inside(p)]
        if fields:
            continue
        for start, end, line, name in bodies:
            if not any(start <= p <= end for p in declarations(mask, declared)):
                continue
            if any(start <= p <= end for p in declarations(mask, used)):
                continue
            lines = sorted({mask.count("\n", 0, p) + 1 for p in uses(mask, used) if start <= p <= end})
            if lines:
                rows.append(("METHOD", path, line, name, declared, used, ",".join(map(str, lines))))
    return rows + undeclared(path, mask, {int(n) for row in rows for n in row[6].split(",")})


def undeclared(path: str, mask: str, reported: set) -> list[tuple]:
    """UNDECLARED: a simple-name receiver m3 (m3.x) in a file that declares no m3 at all, outside METHOD rows; an
    inherited field of that name would be a false candidate, so these rows are candidates, not verdicts."""
    if declarations(mask, NAMES[1]):
        return []
    lines = sorted({mask.count("\n", 0, m.start()) + 1 for m in RECEIVER.finditer(mask)
                    if not directive(mask, m.start())} - reported)
    return [("UNDECLARED", path, lines[0], "-", "-", NAMES[1], ",".join(map(str, lines)))] if lines else []


def candidates(root: Path, rev: str, prefix: str, env: dict, *pattern: str) -> set:
    listed = subprocess.run(["git", "-C", str(root), "grep", "-l", "-z", *pattern, rev, "--", prefix + "*.java"],
                            env=env, capture_output=True).stdout.decode("utf-8", "surrogateescape")
    return {p.split(":", 1)[1] for p in listed.split("\0") if p}


def blobs(root: Path, rev: str, prefix: str):
    env = dict(os.environ, GIT_NO_LAZY_FETCH="1")
    paths = sorted(candidates(root, rev, prefix, env, "-w", NAMES[0])
                   | candidates(root, rev, prefix, env, "-E", RECEIVER_GREP))
    query = "".join("%s:%s\n" % (rev, p) for p in paths).encode("utf-8", "surrogateescape")
    out = subprocess.run(["git", "-C", str(root), "cat-file", "--batch"], input=query, env=env,
                         capture_output=True, check=True).stdout
    pos = 0
    for path in paths:
        header_end = out.index(b"\n", pos)
        header = out[pos:header_end].split()
        if header[-1] == b"missing":
            yield path, None
            pos = header_end + 1
            continue
        size = int(header[2])
        yield path, out[header_end + 1:header_end + 1 + size].decode("utf-8", "surrogateescape")
        pos = header_end + 1 + size + 1


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".")
    parser.add_argument("--rev", default="HEAD")
    parser.add_argument("--prefix", default="m3/")
    parser.add_argument("--out")
    args = parser.parse_args(argv)
    rows, scanned, missing = [], 0, 0
    for path, text in blobs(Path(args.root), args.rev, args.prefix):
        scanned += 1
        if text is None:
            missing += 1
            rows.append(("MISSING", path, 0, "-", "-", "-", "-"))
            continue
        rows += hits(path, text)
    table = "\n".join("\t".join(map(str, row)) for row in [COLUMNS] + rows) + "\n"
    if args.out:
        Path(args.out).write_bytes(table.encode("utf-8"))
    else:
        sys.stdout.write(table)
    files = len({row[1] for row in rows})
    print("%s %s rev=%s scanned=%d hits=%d files=%d missing=%d" % (CENSUS, VERSION, args.rev, scanned,
                                                                    len(rows) - missing, files, missing),
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
