#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_UTF16_EDIT_ORACLE v2 (recipe-first port, never by hand; v1 = recipe/utf16-atoms-20261106).

Copies every FILE row of RELOCATION_MAP.tsv from a donor git tree (--donor-git DIR at --donor-rev REV) into the
module (the directory two levels above this recipe), rewriting only (1) the leading license header lines, replaced
by the M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines: mapped through the PACKAGE,
TYPE and EXTERNAL rows, dropped from a split target when nothing it emits uses them, and extended by the IMPORT
rows of the target, (4) type names, through the QUALIFIED rows (Type.member references) and the TYPE rows, and
(5) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob URL at the
donor commit. A donor file whose target has MEMBER rows is SPLIT: the target receives the members (fields,
constructors, methods, in donor order, each member's bytes unchanged) that the MEMBER rows assign to it, under the
target's own type name; members without a MEMBER row are dropped (declared drops, listed in the anchor). A member
named by a BIND row (Type.member -> file next to this recipe) is received with the bytes of that file instead of the
donor's: the declared binding of a donor candidate to the master owner, recorded in the anchor and in PROVENANCE.tsv
(bound column), never hidden. Every other byte of every file is kept.

PROVENANCE.tsv pins donor commit, blob sha, donor sha256 and target sha256 of every target, the members it
received (ALL for a whole file), the bound members, body_match (TRUE when the bytes after the package line, minus
the anchor and the added imports, equal the donor's own member bytes up to the relocated type's own name; FALSE
whenever a member is bound) and type_renames (the number of QUALIFIED/TYPE token substitutions).

Idempotent: writes a file only when its bytes differ (CRLF and LF compare equal); a second run reports changed=0.
Refuses (exit 2) an unmapped package, an import that is neither a JDK package, nor a mapped donor package, nor under
a declared EXTERNAL package of the map, a file without a top-level type, a MEMBER or BIND row naming a member the
donor does not declare exactly once, a BIND row whose member no target receives, or a donor path named by several
FILE rows without MEMBER rows for each target.

usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v2"
RECIPE_DIR = "m3/ports/precompute/recipe/utf16-edit-oracle-20261107"
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "members", "bound", "body_match", "type_renames", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+)(static\s+)?([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+(\w+)")
ANNOTATION = re.compile(rb"@\w+(?:\([^)]*\))?")
IDENTIFIER = re.compile(rb"[A-Za-z_$][\w$]*")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
INIT = "<init>"
ALL = "ALL"
NONE = "-"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "QUALIFIED": {}, "EXTERNAL": [], "MEMBER": {},
                     "IMPORT": {}, "BIND": {}}
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "EXTERNAL":
            mapping["EXTERNAL"].append(donor)
        elif kind == "MEMBER":  # Type.member -> target simple type name
            donor_type, _dot, member = donor.rpartition(".")
            mapping["MEMBER"].setdefault((donor_type, target), []).append(member)
        elif kind == "IMPORT":  # target path -> import text (without the keyword and the semicolon)
            mapping["IMPORT"].setdefault(donor, []).append(target)
        elif kind in ("NEC", "OMIT", "CLOSED"):  # documentation rows: copy nothing
            continue
        elif kind in mapping:  # PACKAGE, TYPE, QUALIFIED, BIND (Type.member -> recipe-relative file)
            mapping[kind][donor] = target
        else:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def substitute(line: bytes, rules: dict, counts: list) -> bytes:
    """Apply donor -> target token rules, longest donor first; counts[0] accumulates the substitutions."""
    for donor, target in sorted(rules.items(), key=lambda item: -len(item[0])):
        if donor != target:
            line, n = re.subn(rb"(?<![\w$.])" + re.escape(donor.encode()) + rb"(?![\w$])", target.encode(), line)
            counts[0] += n
    return line


def map_import(fqn: str, mapping: dict, donor_path: str) -> str:
    owner, _dot, simple = fqn.rpartition(".")
    if owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + mapping["TYPE"].get(simple, simple)
    if fqn.startswith(JDK_PREFIXES) or any(fqn.startswith(ext + ".") for ext in mapping["EXTERNAL"]):
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(donor_path: str, blob: str, commit: str, members: str, dropped: list, bound: list) -> bytes:
    text = "//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s/%s %s" % (
        DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_DIR, RECIPE, VERSION)
    if members != ALL:
        text += "; split members " + members
    if dropped:
        text += "; dropped members " + ",".join(dropped)
    if bound:
        text += "; bound members " + ",".join("%s (%s)" % pair for pair in bound)
    return text.encode()


def mask(text: bytes, in_comment: bool) -> tuple[bytes, bool]:
    """The line with comments and string/char literals blanked, so braces and semicolons can be counted."""
    out = bytearray()
    index, size = 0, len(text)
    while index < size:
        if in_comment:
            end = text.find(b"*/", index)
            stop = size if end < 0 else end + 2
            out += b" " * (stop - index)
            index, in_comment = stop, end < 0
            continue
        if text.startswith(b"//", index):
            out += b" " * (size - index)
            break
        if text.startswith(b"/*", index):
            out += b"  "
            index, in_comment = index + 2, True
            continue
        quote = text[index:index + 1]
        if quote in (b'"', b"'"):
            end = index + 1
            while end < size and text[end:end + 1] != quote:
                end += 2 if text[end:end + 1] == b"\\" else 1
            stop = min(end + 1, size)
            out += b" " * (stop - index)
            index = stop
            continue
        out += quote
        index += 1
    return bytes(out), in_comment


def member_name(declaration: bytes, type_name: str) -> str:
    text = ANNOTATION.sub(b" ", declaration)
    stops = [text.find(token) for token in (b"(", b"=", b";", b"{")]
    head = text[:min(stop for stop in stops if stop >= 0)]
    identifiers = IDENTIFIER.findall(head)
    if not identifiers:
        raise Refused("member without a name: %r" % declaration[:60])
    name = identifiers[-1].decode()
    return INIT if name == type_name else name


def split_members(body: list[bytes], type_name: str) -> list[tuple[str, list[bytes], list[bytes]]]:
    """(name, leading blank lines, lines) of every member of a class body, in order, bytes untouched."""
    members: list = []
    blanks: list = []
    current: list = []
    declaration = b""
    depth, opened, in_comment, name = 0, False, False, None
    for line in body:
        text, _eol = split_eol(line)
        masked, in_comment = mask(text, in_comment)
        if name is None and not masked.strip():
            (current if text.strip() or current else blanks).append(line)
            continue
        current.append(line)
        depth += masked.count(b"{") - masked.count(b"}")
        opened = opened or b"{" in masked
        if name is None:
            declaration += masked + b" "
            if opened or b";" in masked:
                name = member_name(declaration, type_name)
            else:
                continue
        if (opened and depth == 0) or (not opened and b";" in masked):
            members.append((name, blanks, current))
            blanks, current, declaration = [], [], b""
            depth, opened, name = 0, False, None
    if current or depth:
        raise Refused("unterminated member in %s: %r" % (type_name, b"".join(current)[:60]))
    return members


class Donor:
    """A donor compilation unit split into header, package, imports, preamble (type Javadoc), type line, body."""

    def __init__(self, data: bytes, donor_path: str):
        lines = LINES.findall(data)
        self.eol = split_eol(lines[0])[1] if lines else b"\n"
        index = 0
        while index < len(lines) and HEADER_LINE.match(lines[index]):
            index += 1
        self.package = None
        self.imports: list = []
        self.preamble: list = []
        self.type_index = None
        for position in range(index, len(lines)):
            text, _eol = split_eol(lines[position])
            package = PACKAGE_LINE.match(text)
            if package and self.package is None:
                self.package = package.group(1).decode()
                continue
            if IMPORT_LINE.match(text):
                self.imports.append(lines[position])
                continue
            if TYPE_LINE.match(text):
                self.type_index = position
                break
            if text.strip():
                self.preamble.append(lines[position])
        if self.package is None:
            raise Refused("no package line in %s" % donor_path)
        if self.type_index is None:
            raise Refused("no top-level type in %s" % donor_path)
        self.type_name = TYPE_LINE.match(split_eol(lines[self.type_index])[0]).group(1).decode()
        self.type_line = lines[self.type_index]
        self.tail = lines[self.type_index + 1:]
        depth, in_comment = 0, False
        for position in range(self.type_index, len(lines)):
            masked, in_comment = mask(split_eol(lines[position])[0], in_comment)
            depth += masked.count(b"{") - masked.count(b"}")
            if depth == 0 and position > self.type_index:
                self.body = lines[self.type_index + 1:position]
                self.closing = lines[position:]
                break
        else:
            raise Refused("unbalanced braces in %s" % donor_path)
        if split_eol(self.closing[0])[0].strip() != b"}":
            raise Refused("the closing brace of %s shares its line with other tokens" % donor_path)


def uses(name: str, text: bytes) -> bool:
    return re.search(rb"(?<![\w$])" + re.escape(name.encode()) + rb"(?![\w$])", text) is not None


def binding_lines(recipe_dir: Path, relative: str, eol: bytes) -> list[bytes]:
    parts = (recipe_dir / relative).read_bytes().replace(b"\r\n", b"\n").split(b"\n")
    if parts and parts[-1] == b"":
        parts.pop()
    return [part + eol for part in parts]


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, target: str,
             wanted: list | None, recipe_dir: Path, used_bindings: set) -> tuple[bytes, bool, str, str, int]:
    """The relocated bytes, body_match, the members column, the bound column and the type-name substitutions."""
    donor = Donor(data, donor_path)
    if donor.package not in mapping["PACKAGE"]:
        raise Refused("unmapped package %s in %s" % (donor.package, donor_path))
    target_type = Path(target).stem
    own = {donor.type_name: target_type} if wanted is not None else {}
    counts = [0]
    out = [text + donor.eol for text in HEADER]
    out.append(b"package " + mapping["PACKAGE"][donor.package].encode() + b";" + donor.eol)
    out.append(donor.eol)
    dropped: list = []
    bound: list = []
    if wanted is None:
        members_text = ALL
        body_lines = donor.tail
        donor_body = b"".join(donor.tail)
        emitted_body = donor_body
    else:
        members = split_members(donor.body, donor.type_name)
        names = [name for name, _blanks, _lines in members]
        for name in wanted:
            if names.count(name) != 1:
                raise Refused("member %s.%s is declared %d times in %s (MEMBER rows need exactly one)"
                              % (donor.type_name, name, names.count(name), donor_path))
        chosen = [(name, blanks, lines) for name, blanks, lines in members if name in wanted]
        dropped = [name for name in names if name not in wanted]
        members_text = ",".join(name for name, _blanks, _lines in chosen)
        body_lines = []
        donor_lines = []
        for position, (name, blanks, lines) in enumerate(chosen):
            key = donor.type_name + "." + name
            donor_lines += (blanks if position else []) + lines
            if key in mapping["BIND"]:
                bound.append((name, mapping["BIND"][key]))
                used_bindings.add(key)
                lines = binding_lines(recipe_dir, mapping["BIND"][key], donor.eol)
            body_lines += (blanks if position else []) + lines
        donor_body = b"".join(donor_lines)
        emitted_body = b"".join(body_lines)
        body_lines = body_lines + donor.closing
    emitted_imports = []
    for line in donor.imports:
        text, eol = split_eol(line)
        imported = IMPORT_LINE.match(text)
        fqn = imported.group(3).decode()
        if wanted is not None and not uses(fqn.rpartition(".")[2], emitted_body):
            continue
        mapped = map_import(fqn, mapping, donor_path)
        emitted_imports.append(imported.group(1) + (imported.group(2) or b"") + mapped.encode() + imported.group(4)
                               + eol)
    added = [b"import " + text.encode() + b";" + donor.eol for text in mapping["IMPORT"].get(target, [])]
    if added or emitted_imports:
        out += added + emitted_imports + [donor.eol]
    out += donor.preamble
    out.append(anchor(donor_path, blob, commit, members_text, dropped, bound) + donor.eol)
    renamed_type_line = substitute(donor.type_line, own, [0])
    out.append(substitute(substitute(renamed_type_line, mapping["QUALIFIED"], counts), mapping["TYPE"], counts))
    target_body: list = []
    for line in body_lines:
        line = substitute(line, own, [0])
        line = substitute(substitute(line, mapping["QUALIFIED"], counts), mapping["TYPE"], counts)
        target_body.append(line)
    out += target_body
    expected = substitute(donor_body, own, [0])
    produced = b"".join(target_body[:len(target_body) - (len(donor.closing) if wanted is not None else 0)])
    bound_text = ",".join(name for name, _path in bound) or NONE
    return b"".join(out), produced == expected and not bound, members_text, bound_text, counts[0]


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists():
        existing = path.read_bytes()
        if existing == data or existing.replace(b"\r\n", b"\n") == data.replace(b"\r\n", b"\n"):
            return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(module: Path, donor: Path, rev: str, check: bool) -> int:
    recipe_dir = Path(__file__).resolve().parent
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    donor_paths = [donor_path for donor_path, _target in mapping["FILE"]]
    rows, changed = [], 0
    used_bindings: set = set()
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        donor_type = Path(donor_path).stem
        wanted = mapping["MEMBER"].get((donor_type, Path(target).stem))
        if not wanted and donor_paths.count(donor_path) > 1:
            raise Refused("split target %s has no MEMBER rows for %s" % (target, donor_type))
        relocated, body_match, members, bound, renames = relocate(
            data, donor_path, blob, commit, mapping, target, wanted or None, recipe_dir, used_bindings)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s type_renames=%d members=%s bound=%s %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), renames, members, bound, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated), members, bound,
                     str(body_match).upper(), str(renames), RECIPE + " " + VERSION))
    unused = sorted(set(mapping["BIND"]) - used_bindings)
    if unused:
        raise Refused("BIND rows whose member no target receives: " + ", ".join(unused))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE, VERSION, commit, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(Path(__file__).resolve().parent.parent.parent),
                        help="module root (default: two levels above this recipe)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
