#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe DOOP_FACADE_FIXES v1: two donor fixes of the transient-facade store ported onto the M3JDK twin.

The twin jdk.m3.collections.M3TransientFacadeStore was relocated by recipe transient-facade-20261106 (6b2521d9f84)
and last fixed by the double-dispose twins 6a48d67ce59/2ea36542537 (#2591/#2601). The donor repository fixed two more
defects in two stacked draft PRs (PROVENANCE.tsv: repository, PR numbers, pinned heads, blobs, sha256):

Leaf A (factory reentrant duplicate admission): a reentrant factory can admit the logical id itself (a nested expose
or a retained withInternal) before it returns; the outer insert then throws "duplicate facade id" and the facade it
created was never admitted and never disposed (leak). A private insertCreated wraps insert at the two factory
creation-to-resident call sites (restore paths are unchanged); on failure it retires only an instance that is not
already resident (reference identity), keeps the primary exception and suppresses cleanup failures.
Leaf B (paired idle growth): ensureReusableCapacity published the grown Object[] before allocating the matching
long[]; a failed second allocation left freeFacades longer than freeIds and poisoned later idle returns. Both copies
are staged through the existing BackingCopier (ARRAY_COPY), validated, then published as a pair; a package-private
overload is the copy-fault seam.

Mechanics (no hand edit): every row of EDITS.tsv is derived from the donor's hash-pinned pre/post snapshots.
MERGE3 rows relocate the donor pre and post snapshot with the RELOCATION_MAP.tsv of recipe transient-facade-20261106
(its relocate.py functions are imported, not copied; the relocation anchor line of base and theirs is the host's own
RELOCATE anchor, because that line is not donor content), then git merge-file ours=host preimage (pinned git blob),
base=relocated donor pre, theirs=relocated donor post. Conflicts refuse. FILE rows relocate a new donor test like
the relocation recipe's FILE rows (M3JDK header, mapped package, TYPE rows, one RELOCATE anchor naming this recipe).
ORACLE rows relocate a default-package donor oracle into the package of its target directory under oracle/. A row
with an anchor_decl inserts one //FLLDCIM: MRL anchor above the (Javadoc of the) unique declaration it matches.
HUNKS.tsv records the per-hunk mapping donor pre/post -> relocated base/theirs -> host ours/result and the 3-way
class of each hunk.

Pins: each chain of rows on one target starts at a pinned preimage (host git blob + sha256 or ABSENT) and every row
pins its postimage sha256; the donor bytes are pinned by PROVENANCE.tsv (git blob at the pinned head and sha256).
Every pin is verified before the first write; any mismatch refuses (exit 2) without writing. A target equal to the
final postimage is left alone, a target equal to an earlier state of its chain is advanced, anything else refuses
(drift). Idempotent: the second run reports changed=0. --check writes nothing and exits 1 when a run would change a
file; --leaf A stops each chain after leaf A.

usage: python -I apply.py --donor-git DIR [--root DIR] [--leaf A|B] [--tmp DIR] [--check]
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import importlib.util
import re
import subprocess
import sys
import tempfile
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
MODULE_DIR = RECIPE_DIR.parent.parent
RECIPE_DIR_REL = "make/m3/collections/qualification/" + RECIPE_DIR.name
RELOCATION_RECIPE = MODULE_DIR / "recipe" / "transient-facade-20261106"
EDITS_NAME, PROVENANCE_NAME, HUNKS_NAME = "EDITS.tsv", "PROVENANCE.tsv", "HUNKS.tsv"
HUNKS_COLUMNS = ("leaf", "target", "hunk", "donor_pre", "donor_post", "relocated_base", "relocated_theirs",
                 "host_ours", "host_result", "three_way", "first_line")
RELOCATE_ANCHOR = re.compile(rb"^//FLLDCIM: RELOCATE - ")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
JAVADOC_OPEN = re.compile(rb"^\s*/\*\*")
ABSENT = "ABSENT"


class Refused(Exception):
    pass


def sha256(data: bytes | None) -> str:
    return ABSENT if data is None else hashlib.sha256(data).hexdigest()


def read_tsv(path: Path) -> list:
    lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = lines[0].split("\t")
    return [dict(zip(header, line.split("\t"))) for line in lines[1:]]


def load_relocate():
    """The relocation recipe's own functions (read_map, relocate_file, HEADER, ...), imported from its file."""
    spec = importlib.util.spec_from_file_location("relocate_transient_facade", RELOCATION_RECIPE / "relocate.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # the RELOCATE anchor of a file relocated here names this recipe, not relocate.py
    module.RECIPE_DIR_REL, module.RECIPE, module.VERSION = RECIPE_DIR_REL, RECIPE, VERSION
    return module


def git(cwd: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(cwd), *args], check=True, capture_output=True).stdout


def donor_entry(donor: Path, provenance: dict, key: str) -> tuple[bytes, dict]:
    row = provenance[key]
    blob = git(donor, "rev-parse", row["donor_head"] + ":" + row["donor_path"]).decode().strip()
    if blob != row["git_blob"]:
        raise Refused("donor drift: %s is blob %s at %s, pinned %s" % (key, blob, row["donor_head"][:12], row["git_blob"]))
    data = git(donor, "cat-file", "blob", blob)
    if sha256(data) != row["sha256"]:
        raise Refused("donor drift: %s sha256 %s, pinned %s" % (key, sha256(data), row["sha256"]))
    return data, row


def with_anchor(data: bytes, anchor: bytes) -> bytes:
    lines = LINES.findall(data)
    hits = [at for at, line in enumerate(lines) if RELOCATE_ANCHOR.match(line)]
    if len(hits) != 1:
        raise Refused("expected one RELOCATE anchor line, found %d" % len(hits))
    lines[hits[0]] = anchor
    return b"".join(lines)


def host_anchor(data: bytes) -> bytes:
    hits = [line for line in LINES.findall(data) if RELOCATE_ANCHOR.match(line)]
    if len(hits) != 1:
        raise Refused("host preimage has %d RELOCATE anchor lines, expected 1" % len(hits))
    return hits[0]


def merge3(ours: bytes, base: bytes, theirs: bytes, tmp: Path | None) -> tuple[bytes, int]:
    with tempfile.TemporaryDirectory(dir=tmp) as work:
        paths = []
        for name, data in (("ours", ours), ("base", base), ("theirs", theirs)):
            path = Path(work) / name
            path.write_bytes(data)
            paths.append(str(path))
        done = subprocess.run(["git", "merge-file", "-p", "--diff3", "-L", "ours", "-L", "base", "-L", "theirs",
                               *paths], capture_output=True)
    if done.returncode < 0 or done.returncode > 127:
        raise Refused("git merge-file failed: " + done.stderr.decode(errors="replace"))
    return done.stdout, done.returncode


def insert_anchor(data: bytes, decl: str, text: str) -> bytes:
    lines = LINES.findall(data)
    pattern = re.compile(decl.encode())
    hits = [at for at, line in enumerate(lines) if pattern.search(line)]
    if len(hits) != 1:
        raise Refused("anchor_decl %s matches %d lines" % (decl, len(hits)))
    at = hits[0]
    if at > 0 and lines[at - 1].strip().endswith(b"*/"):
        while at > 0 and not JAVADOC_OPEN.match(lines[at - 1]):
            at -= 1
        at -= 1
    indent = re.match(rb"^\s*", lines[hits[0]]).group(0)
    eol = b"\r\n" if lines[hits[0]].endswith(b"\r\n") else b"\n"
    return b"".join(lines[:at] + [indent + text.encode() + eol] + lines[at:])


def hunks(a: list, b: list) -> list:
    ops = difflib.SequenceMatcher(None, a, b, autojunk=False).get_opcodes()
    return [(i1, i2, j1, j2) for tag, i1, i2, j1, j2 in ops if tag != "equal"]


def span(start: int, end: int) -> str:
    return "%d,%d" % (start + 1, end - start)


def first_line(lines: list, start: int, end: int) -> str:
    text = next((line.strip() for line in lines[start:end] if line.strip()), b"")
    return text.decode("utf-8", errors="replace").replace("\t", " ")[:96] or "-"


def hunk_rows(row: dict, donor_pre: bytes, donor_post: bytes, base: bytes, theirs: bytes, ours: bytes,
              merged: bytes) -> list:
    sets = [hunks(LINES.findall(x), LINES.findall(y)) for x, y in
            ((donor_pre, donor_post), (base, theirs), (ours, merged))]
    ours_vs_base = hunks(LINES.findall(base), LINES.findall(ours))
    if not len(sets[0]) == len(sets[1]) == len(sets[2]):
        raise Refused("hunk counts differ donor=%d relocated=%d host=%d" % tuple(len(s) for s in sets))
    theirs_lines = LINES.findall(theirs)
    out = []
    for n, (d, r, h) in enumerate(zip(*sets), 1):
        overlap = [o for o in ours_vs_base if o[0] < r[1] + 1 and r[0] < o[1] + 1]
        kind = "CLEAN_THEIRS" if not overlap else "CLEAN_BOTH"
        out.append((row["leaf"], row["target"], str(n), span(d[0], d[1]), span(d[2], d[3]), span(r[0], r[1]),
                    span(r[2], r[3]), span(h[0], h[1]), span(h[2], h[3]), kind, first_line(theirs_lines, r[2], r[3])))
    summary = "ours_vs_base_hunks=%d theirs_hunks=%d conflicts=0" % (len(ours_vs_base), len(sets[1]))
    out.append((row["leaf"], row["target"], "3way", "-", "-", "-", "-", "-", "-", summary, "-"))
    return out


def relocate_oracle(reloc, data: bytes, target: str, blob: str, head: str) -> bytes:
    """A default-package donor oracle: M3JDK header, the package of the target directory, one RELOCATE anchor."""
    lines = LINES.findall(data)
    index = 0
    while index < len(lines) and reloc.HEADER_LINE.match(lines[index]):
        index += 1
    if any(reloc.PACKAGE_LINE.match(line) for line in lines):
        raise Refused("ORACLE row %s: donor declares a package (use a FILE row)" % target)
    parts = Path(target).parts
    package = ".".join(parts[parts.index("oracle") + 1:-1])
    eol = b"\n"
    out = [text + eol for text in reloc.HEADER] + [b"package " + package.encode() + b";" + eol, eol]
    anchored = False
    for line in lines[index:]:
        if not anchored and reloc.TYPE_LINE.match(line):
            out.append(reloc.anchor_file("", blob, head) + eol)
            anchored = True
        out.append(line)
    if not anchored:
        raise Refused("no top-level type in ORACLE row %s" % target)
    return b"".join(out)


def build_chain(rows: list, ctx: dict) -> tuple[list, list]:
    """All states of one target (preimage first) and its HUNKS rows; nothing is written here."""
    reloc, mapping, donor, provenance = ctx["reloc"], ctx["mapping"], ctx["donor"], ctx["provenance"]
    first = rows[0]
    if first["before_sha256"] == ABSENT:
        state = None
    else:
        state = git(ctx["root"], "cat-file", "blob", first["host_blob"])
    states, table = [state], []
    for row in rows:
        if ctx["leaf"] and row["leaf"] > ctx["leaf"]:
            break
        if sha256(state) != row["before_sha256"]:
            raise Refused("preimage pin of %s leaf %s: %s, pinned %s" % (row["target"], row["leaf"], sha256(state),
                                                                         row["before_sha256"]))
        if row["mode"] == "MERGE3":
            pre, pre_row = donor_entry(donor, provenance, row["donor_pre"])
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            src = provenance[row["donor_post"]]["donor_src"]
            anchor = host_anchor(state)
            base = with_anchor(reloc.relocate_file(pre, src, pre_row["git_blob"], pre_row["donor_head"], mapping)[0], anchor)
            theirs = with_anchor(reloc.relocate_file(post, src, post_row["git_blob"], post_row["donor_head"], mapping)[0], anchor)
            merged, conflicts = merge3(state, base, theirs, ctx["tmp"])
            if conflicts:
                raise Refused("3-way merge of %s leaf %s: %d conflicts" % (row["target"], row["leaf"], conflicts))
            table += hunk_rows(row, pre, post, base, theirs, state, merged)
            new = merged
        elif row["mode"] == "FILE":
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            src = provenance[row["donor_post"]]["donor_src"]
            new = reloc.relocate_file(post, src, post_row["git_blob"], post_row["donor_head"], mapping)[0]
        elif row["mode"] == "ORACLE":
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            new = relocate_oracle(reloc, post, row["target"], post_row["git_blob"], post_row["donor_head"])
        else:
            raise Refused("unknown mode %s" % row["mode"])
        if row["anchor_decl"] != "-":
            new = insert_anchor(new, row["anchor_decl"], row["anchor_text"])
        if sha256(new) != row["after_sha256"]:
            raise Refused("postimage pin of %s leaf %s: computed %s, pinned %s" % (row["target"], row["leaf"],
                                                                                   sha256(new), row["after_sha256"]))
        state = new
        states.append(new)
    return states, table


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(root: Path, donor: Path, leaf: str | None, tmp: Path | None, check: bool) -> int:
    reloc = load_relocate()
    provenance = {row["id"]: row for row in read_tsv(RECIPE_DIR / PROVENANCE_NAME)}
    for row in provenance.values():  # the production path at the same head names the relocated type and its blob
        src = provenance.get(row["id"].split(".")[0] + (".testsrc" if ".test" in row["id"] else ".src"))
        row["donor_src"] = src["donor_path"] if src else row["donor_path"]
    ctx = {"reloc": reloc, "mapping": reloc.read_map(RELOCATION_RECIPE), "donor": donor, "provenance": provenance,
           "root": root, "leaf": leaf, "tmp": tmp}
    targets: dict = {}
    for row in read_tsv(RECIPE_DIR / EDITS_NAME):
        targets.setdefault(row["target"], []).append(row)
    plans, table = [], []
    for target, rows in targets.items():
        states, part = build_chain(rows, ctx)
        table += part
        path = root / "m3/collections" / target
        current = path.read_bytes() if path.exists() else None
        final = states[-1]
        if current == final:
            plans.append((path, None, "UNCHANGED", target))
        elif any(current == state for state in states[:-1]):
            plans.append((path, final, "CHANGED", target))
        else:
            raise Refused("target drift: %s is %s, not a state of its chain" % (target, sha256(current)))
    changed = 0
    for path, data, state, target in plans:  # every pin was verified above; writes start here
        if data is not None:
            changed += write_if_changed(path, data, check)
        print("%s %s %s" % (state, sha256(data if data is not None else path.read_bytes()), target))
    hunks_text = "\t".join(HUNKS_COLUMNS) + "\n" + "".join("\t".join(r) + "\n" for r in table)
    hunk_state = leaf is None and write_if_changed(RECIPE_DIR / HUNKS_NAME, hunks_text.encode(), check)
    print("DOOP_FACADE_FIXES %s/%s %s leaf=%s targets=%d changed=%d hunks=%d hunks_tsv=%s" % (
        RECIPE_DIR_REL, RECIPE, VERSION, leaf or "ALL", len(plans), changed,
        sum(1 for r in table if r[2] != "3way"), "CHANGED" if hunk_state else "UNCHANGED"))
    return 1 if check and (changed or hunk_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory holding the pinned heads (blobless)")
    parser.add_argument("--root", default=str(MODULE_DIR.parent.parent), help="repository root (default: this tree)")
    parser.add_argument("--leaf", choices=("A", "B"), help="stop every chain after this leaf")
    parser.add_argument("--tmp", help="directory for the git merge-file inputs (default: the system temp)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root), Path(args.donor_git), args.leaf, Path(args.tmp) if args.tmp else None, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
