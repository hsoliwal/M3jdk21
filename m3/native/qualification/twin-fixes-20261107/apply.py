#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe TWIN_FIXES v1: donor fixes ported onto their M3JDK twins by relocation + 3-way merge (no hand edit).

The twins of this module were relocated from the donor repository by one of the module's relocation recipes
(<module>/recipe/<name>/relocate.py with its RELOCATION_MAP.tsv). The donor then fixed the owners in merged PRs, each
a hash-pinned recipe crate with a pre snapshot, a post snapshot and a new test (PROVENANCE.tsv: repository, PR,
branch, merge commit, path, git blob, sha256 and URL of every donor byte read). This recipe ports each fix:

MERGE3 rows: the donor pre and post snapshot are relocated with the twin's own relocation recipe (its relocate.py
functions are imported from its file, not copied; the relocation recipe is found by the MERGE3 targets among the
FILE rows of the RELOCATION_MAP.tsv files under <module>/recipe). The RELOCATE anchor line of base and theirs is set
to the host's own anchor, because that line is recipe metadata, not donor content. Then git merge-file runs with
ours = the pinned M3JDK preimage (git blob), base = relocated donor pre, theirs = relocated donor post. Conflicts
refuse. HUNKS.tsv records every hunk: donor pre/post -> relocated base/theirs -> host ours/result and its 3-way class.
FILE rows relocate a new donor file (test, oracle with a package line, C fixture) like the relocation recipe's FILE
rows (M3JDK header, mapped package, mapped imports, one RELOCATE anchor naming this recipe); a target whose stem
differs from the donor stem renames that one type (JDK naming of the module). ORACLE rows relocate a default-package
donor oracle into the package of its target directory under oracle/. ALREADY rows prove that the host already carries
the relocated donor file: the donor post snapshot is relocated like a FILE row and must equal the pinned host preimage
once the //FLLDCIM: RELOCATE and //FLLDCIM: MRL anchor lines of both are dropped (recipe metadata, not donor content);
the host bytes are kept (a landed sibling port, e.g. a relocation refresh, is confirmed by diff, never re-applied).
ANCHOR rows insert one //FLLDCIM: MRL anchor above the (Javadoc of the) unique declaration anchor_decl matches; a
MERGE3 or ALREADY row may carry one anchor as well.

Pins: each chain of rows on one target starts at a pinned preimage (host git blob + sha256, or ABSENT) and every row
pins its postimage sha256; the donor bytes are pinned by PROVENANCE.tsv (git blob at the merge commit and sha256).
Every pin is verified before the first write; any mismatch refuses (exit 2) without writing. A target equal to the
final postimage is left alone, a target equal to an earlier state of its chain is advanced, anything else refuses
(drift). Idempotent: the second run reports changed=0. --check writes nothing and exits 1 when a run would change a
file. --seal fills the postimage pins written as "?" (first sealing of a new row) and rewrites EDITS.tsv; an unsealed
pin refuses otherwise. --leaf X stops each chain after leaf X.

usage: python -I apply.py --donor-git DIR [--root DIR] [--leaf X] [--tmp DIR] [--check] [--seal]
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
TAG = "TWIN_FIXES"
RECIPE_DIR = Path(__file__).resolve().parent
MODULE_DIR = RECIPE_DIR.parent.parent
MODULE_REL = "m3/" + MODULE_DIR.name
RECIPE_DIR_REL = MODULE_REL + "/qualification/" + RECIPE_DIR.name
EDITS_NAME, PROVENANCE_NAME, HUNKS_NAME = "EDITS.tsv", "PROVENANCE.tsv", "HUNKS.tsv"
HUNKS_COLUMNS = ("leaf", "target", "hunk", "donor_pre", "donor_post", "relocated_base", "relocated_theirs",
                 "host_ours", "host_result", "three_way", "first_line")
RELOCATE_ANCHOR = re.compile(rb"^//FLLDCIM: RELOCATE - ")
ANCHOR_LINE = re.compile(rb"^\s*//FLLDCIM: (?:RELOCATE|MRL)\b")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
JAVADOC_OPEN = re.compile(rb"^\s*/\*\*")
ABSENT, UNSEALED = "ABSENT", "?"


class Refused(Exception):
    pass


def sha256(data: bytes | None) -> str:
    return ABSENT if data is None else hashlib.sha256(data).hexdigest()


def read_tsv(path: Path) -> list:
    lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = lines[0].split("\t")
    return [dict(zip(header, line.split("\t"))) for line in lines[1:]]


def write_tsv_rows(path: Path, rows: list) -> None:
    """Rewrite the data lines of a TSV in place; comment lines and the header are kept byte for byte."""
    kept = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = kept[0].split("\t")
    out, at = [], 0
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#") or line == kept[0]:
            out.append(line)
            continue
        out.append("\t".join(rows[at][name] for name in header))
        at += 1
    path.write_text("\n".join(out) + "\n", encoding="utf-8", newline="\n")


def find_relocation_recipe(targets: set) -> Path:
    """The one relocation recipe of this module whose FILE rows produced every MERGE3 target."""
    hits = []
    for map_path in sorted((MODULE_DIR / "recipe").glob("*/RELOCATION_MAP.tsv")):
        rows = [line.split("\t") for line in map_path.read_text(encoding="utf-8").splitlines()
                if line and not line.startswith("#")]
        files = {row[2] for row in rows if len(row) > 2 and row[0] == "FILE"}
        if targets and targets <= files:
            hits.append(map_path.parent)
    if len(hits) != 1:
        raise Refused("relocation recipe of %s: %d candidates %s" % (sorted(targets), len(hits), [h.name for h in hits]))
    return hits[0]


def load_relocate(recipe_dir: Path):
    """The relocation recipe's own functions (read_map, relocate/relocate_file, headers, anchors), imported from its file."""
    spec = importlib.util.spec_from_file_location("relocate_" + recipe_dir.name.replace("-", "_"), recipe_dir / "relocate.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    # the RELOCATE anchor of a file relocated here names this recipe, not relocate.py
    for name, value in (("RECIPE_DIR_REL", RECIPE_DIR_REL), ("RECIPE", RECIPE), ("VERSION", VERSION),
                        ("RECIPE_PATH", RECIPE_DIR_REL + "/" + RECIPE)):
        if hasattr(module, name):
            setattr(module, name, value)
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


def strip_anchors(data: bytes) -> bytes:
    """The file without its //FLLDCIM: RELOCATE and //FLLDCIM: MRL lines (recipe metadata, not donor content)."""
    return b"".join(line for line in LINES.findall(data) if not ANCHOR_LINE.match(line))


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


def mapping_for(mapping: dict, donor_src: str, target: str) -> dict:
    """The relocation map, plus one TYPE row when a .java target stem differs from its donor stem."""
    donor_stem, target_stem = Path(donor_src).stem, Path(target).stem
    if not target.endswith(".java") or donor_stem == target_stem:
        return mapping
    return dict(mapping, TYPE={**mapping["TYPE"], donor_stem: target_stem})


def relocate_bytes(reloc, data: bytes, donor_src: str, target: str, blob: str, head: str, mapping: dict) -> bytes:
    """One relocated file through the relocation recipe's own FILE-row function (either API shape)."""
    if hasattr(reloc, "relocate_file"):  # RELOCATE_TRANSIENT_FACADE v4 and siblings
        return reloc.relocate_file(data, donor_src, blob, head, mapping)[0]
    return reloc.relocate(data, donor_src, target, blob, head, mapping)[0]  # RELOCATE_SCOPED_LEASES v1 and siblings


def relocate_oracle(reloc, data: bytes, donor_src: str, target: str, blob: str, head: str) -> bytes:
    """A default-package donor oracle: M3JDK header, the package of the target directory, one RELOCATE anchor."""
    lines = LINES.findall(data)
    index = 0
    while index < len(lines) and reloc.HEADER_LINE.match(lines[index]):
        index += 1
    if any(reloc.PACKAGE_LINE.match(line) for line in lines):
        raise Refused("ORACLE row %s: donor declares a package (use a FILE row)" % target)
    parts = Path(target).parts
    package = ".".join(parts[parts.index("oracle") + 1:-1])
    header = getattr(reloc, "HEADER", None) or reloc.HEADERS["java"]
    anchor = reloc.anchor_file("", blob, head) if hasattr(reloc, "anchor_file") else reloc.anchor("java", donor_src, blob, head)
    eol = b"\n"
    out = [text + eol for text in header] + [b"package " + package.encode() + b";" + eol, eol]
    anchored = False
    for line in lines[index:]:
        if not anchored and reloc.TYPE_LINE.match(line):
            out.append(anchor + eol)
            anchored = True
        out.append(line)
    if not anchored:
        raise Refused("no top-level type in ORACLE row %s" % target)
    return b"".join(out)


def build_chain(rows: list, ctx: dict) -> tuple[list, list]:
    """All states of one target (preimage first) and its HUNKS rows; nothing is written here."""
    reloc, mapping, donor, provenance = ctx["reloc"], ctx["mapping"], ctx["donor"], ctx["provenance"]
    first = rows[0]
    state = None if first["before_sha256"] == ABSENT else git(ctx["root"], "cat-file", "blob", first["host_blob"])
    states, table = [state], []
    for row in rows:
        if ctx["leaf"] and row["leaf"] > ctx["leaf"]:
            break
        if row["before_sha256"] == UNSEALED:  # a chained row sealed with its predecessor's postimage
            if not ctx["seal"]:
                raise Refused("unsealed preimage pin of %s leaf %s: chain state %s (run --seal)" % (row["target"], row["leaf"], sha256(state)))
            row["before_sha256"] = sha256(state)
            ctx["sealed"] += 1
        if sha256(state) != row["before_sha256"]:
            raise Refused("preimage pin of %s leaf %s: %s, pinned %s" % (row["target"], row["leaf"], sha256(state),
                                                                         row["before_sha256"]))
        if row["mode"] == "MERGE3":
            pre, pre_row = donor_entry(donor, provenance, row["donor_pre"])
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            src = post_row["donor_src"]
            anchor = host_anchor(state)
            base = with_anchor(relocate_bytes(reloc, pre, src, row["target"], pre_row["git_blob"], pre_row["donor_head"], mapping), anchor)
            theirs = with_anchor(relocate_bytes(reloc, post, src, row["target"], post_row["git_blob"], post_row["donor_head"], mapping), anchor)
            merged, conflicts = merge3(state, base, theirs, ctx["tmp"])
            if conflicts:
                raise Refused("3-way merge of %s leaf %s: %d conflicts" % (row["target"], row["leaf"], conflicts))
            table += hunk_rows(row, pre, post, base, theirs, state, merged)
            new = merged
        elif row["mode"] == "FILE":
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            src = post_row["donor_src"]
            new = relocate_bytes(reloc, post, src, row["target"], post_row["git_blob"], post_row["donor_head"],
                                 mapping_for(mapping, src, row["target"]))
        elif row["mode"] == "ORACLE":
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            new = relocate_oracle(reloc, post, post_row["donor_src"], row["target"], post_row["git_blob"], post_row["donor_head"])
        elif row["mode"] == "ALREADY":
            pre, pre_row = donor_entry(donor, provenance, row["donor_pre"]) if row["donor_pre"] != ABSENT else (None, None)
            post, post_row = donor_entry(donor, provenance, row["donor_post"])
            src = post_row["donor_src"]
            theirs = relocate_bytes(reloc, post, src, row["target"], post_row["git_blob"], post_row["donor_head"],
                                    mapping_for(mapping, src, row["target"]))
            if state is None or strip_anchors(theirs) != strip_anchors(state):
                raise Refused("ALREADY row %s leaf %s: the host is not the relocated donor file (anchor lines aside)"
                              % (row["target"], row["leaf"]))
            fix_hunks = "-"
            if pre is not None:
                base = relocate_bytes(reloc, pre, src, row["target"], pre_row["git_blob"], pre_row["donor_head"], mapping)
                fix_hunks = str(len(hunks(LINES.findall(strip_anchors(base)), LINES.findall(strip_anchors(theirs)))))
            table.append((row["leaf"], row["target"], "already", "-", "-", "-", "-", "-", "-",
                          "host_vs_relocated_post_hunks=0 (anchor lines dropped) fix_hunks=" + fix_hunks, "-"))
            new = state
        elif row["mode"] == "ANCHOR":
            new = state
        else:
            raise Refused("unknown mode %s" % row["mode"])
        if row["anchor_decl"] != "-":
            new = insert_anchor(new, row["anchor_decl"], row["anchor_text"])
        if row["after_sha256"] == UNSEALED:
            if not ctx["seal"]:
                raise Refused("unsealed postimage pin of %s leaf %s: computed %s (run --seal)" % (row["target"], row["leaf"], sha256(new)))
            row["after_sha256"] = sha256(new)
            ctx["sealed"] += 1
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


def run(root: Path, donor: Path, leaf: str | None, tmp: Path | None, check: bool, seal: bool) -> int:
    provenance = {row["id"]: row for row in read_tsv(RECIPE_DIR / PROVENANCE_NAME)}
    for row in provenance.values():  # a snapshot row (pre/post/test) is relocated under its production path at the
        owner, _dot, kind = row["id"].partition(".")  # same head (same blob); every other row under its own path
        src = provenance.get(owner + (".testsrc" if kind == "test" else ".src")) if kind in ("pre", "post", "test") else None
        row["donor_src"] = src["donor_path"] if src else row["donor_path"]
    edits = read_tsv(RECIPE_DIR / EDITS_NAME)
    relocation = find_relocation_recipe({row["target"] for row in edits if row["mode"] == "MERGE3"
                                         or (row["mode"] == "ALREADY" and row["target"].startswith("src/main/"))})
    reloc = load_relocate(relocation)
    ctx = {"reloc": reloc, "mapping": reloc.read_map(relocation), "donor": donor, "provenance": provenance,
           "root": root, "leaf": leaf, "tmp": tmp, "seal": seal, "sealed": 0}
    targets: dict = {}
    for row in edits:
        targets.setdefault(row["target"], []).append(row)
    plans, table = [], []
    for target, rows in targets.items():
        states, part = build_chain(rows, ctx)
        table += part
        path = root / MODULE_REL / target
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
    if ctx["sealed"] and not check:
        write_tsv_rows(RECIPE_DIR / EDITS_NAME, edits)
    hunks_text = "\t".join(HUNKS_COLUMNS) + "\n" + "".join("\t".join(r) + "\n" for r in table)
    hunk_state = leaf is None and write_if_changed(RECIPE_DIR / HUNKS_NAME, hunks_text.encode(), check)
    print("%s %s/%s %s relocation=%s leaf=%s targets=%d changed=%d sealed=%d hunks=%d hunks_tsv=%s" % (
        TAG, RECIPE_DIR_REL, RECIPE, VERSION, relocation.name, leaf or "ALL", len(plans), changed, ctx["sealed"],
        sum(1 for r in table if r[2] != "3way"), "CHANGED" if hunk_state else "UNCHANGED"))
    return 1 if check and (changed or hunk_state or ctx["sealed"]) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory holding the pinned merge commits (blobless)")
    parser.add_argument("--root", default=str(MODULE_DIR.parent.parent), help="repository root (default: this tree)")
    parser.add_argument("--leaf", help="stop every chain after this leaf")
    parser.add_argument("--tmp", help="directory for the git merge-file inputs (default: the system temp)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--seal", action="store_true", help="fill the postimage pins written as ? and rewrite EDITS.tsv")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root), Path(args.donor_git), args.leaf, Path(args.tmp) if args.tmp else None, args.check,
                   args.seal)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
