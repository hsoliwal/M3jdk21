#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_COOCCURRENCE_PPMI v3 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR) into the
module (--module DIR, default: the grandparent of this recipe directory, m3/nlp), at the donor commit pinned by the
REPO row of the map (--donor-rev REV may repeat it; a REV that resolves to another commit is refused, so one pin
serves the recipe, the differential and the provenance).

A FILE row's source may carry a .java, .java.txt (a hash-pinned postimage of a donor recipe crate) or .java.template
(a donor recipe template) name: the bytes are Java source either way, the target name decides, and no template marker
is interpreted (the donor templates carry none; a marker would make body_match_renamed FALSE and is refused by the
compiler). The recipe rewrites only (1) the leading license header lines (// SPDX-*, // Copyright), replaced by the
M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped packages and types, through
the PACKAGE, TYPE and EXTERNAL rows (an import of a.b.Type.member maps its longest mapped package prefix and the type
that follows it; JDK packages java., javax., jdk., com.sun. pass), (4) the TYPE rows whose donor and target names
differ, as whole-word tokens, (5) the LITERAL rows, declared string-literal tokens (a system property, a library
name) renamed as whole tokens, exactly the rename the JDK naming rules would apply (make/m3/tooling/jdk-naming/
NAMING_RULES.tsv) so that jdk_naming.py and this recipe share one fixed point, and (6) one //FLLDCIM: anchor line
inserted before the top-level type declaration, naming the donor blob URL at the donor commit. Every other byte of
the file is kept (CRLF too).

SKIP rows name donor files of the leaf that are NOT relocated in this pass by decision (a native kernel left to a
follow-up leaf, with its target symbol and path in the note); NEC rows name donor files whose equivalence with an
existing owner is not decided here (never two owners of one shape). Both are informational: the recipe pins their
donor blob in its run log (HELD lines) and copies nothing.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row
and states body_match (the bytes after the package line, minus the anchor, equal the donor's) and body_match_renamed
(they equal the donor's after the TYPE, LITERAL and import rows are applied to the donor bytes: only the declared
names differ, method bodies are the donor's).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) a REV that is
not the pinned commit, an unmapped package, an import that is neither a JDK package, nor a mapped donor package, nor a
declared EXTERNAL package (port the closure: add the file or the dependency to the map), a file without a top-level
type, or a LITERAL row whose donor token is absent from every FILE row.

A PACKAGE row whose donor is "-" names the default package: a donor file without a package line (an oracle compiled
next to the donor class in the donor's proof scripts) receives "package <target>;" and one empty line after the
header; its first line after the header starts the body, so body_match_renamed still compares every donor byte.

Same code as m3/nlp/recipe/backoff-word-ngrams-20261106/relocate.py v3 (PR #2544); only this docstring's name differs.
Java files only: the C selector of this donor family is a SKIP row for the follow-up JNI leaf, whose recipe adds
the C path (INCLUDE and JNI rows) of m3/arrays/recipe/bitmatrix-20261106/relocate.py v3.

Derived from m3/nlp/recipe/scoped-ngram-perplexity-20261106/relocate.py v2 (RELOCATE_NGRAM_PERPLEXITY, PR #2537:
REPO pin, LITERAL, SKIP and NEC rows, body_match_renamed, longest-prefix import mapping), which derives from
m3/nlp/recipe/relocate.py v1 (PR #2506) and m3/arrays/recipe/bitmatrix-20261106/relocate.py v3; v3 adds the
default-package PACKAGE row.

usage: python -I relocate.py --donor-git DIR [--donor-rev REV] [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v3"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/nlp/recipe/" + RECIPE_DIR.name + "/" + RECIPE
BLOB_URL = "https://github.com/%s/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.", "com.sun.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("SKIP", "NEC")
DEFAULT_PACKAGE = "-"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"REPO": None, "FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "LITERAL": {}, "HELD": []}
    for kind, donor, target, *note in rows:
        if kind == "REPO":
            mapping["REPO"] = (donor, target)
        elif kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, note[0] if note else ""))
        else:
            mapping[kind][donor] = target
    if mapping["REPO"] is None:
        raise Refused("no REPO row (donor repository slug and pinned commit) in " + MAP_NAME)
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def rename_types(line: bytes, types: dict) -> bytes:
    for donor, target in types.items():
        if donor != target:
            line = re.sub(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
    return line


def rename_literals(line: bytes, literals: dict, seen: dict) -> bytes:
    """Renames each declared literal token (regex-escaped, not preceded or followed by a word or dot character)."""
    for donor, target in literals.items():
        pattern = rb"(?<![\w.])" + re.escape(donor.encode()) + rb"(?![\w.])"
        line, count = re.subn(pattern, target.encode(), line)
        seen[donor] = seen.get(donor, 0) + count
    return line


def map_import(fqn: str, mapping: dict, donor_path: str) -> str:
    parts = fqn.split(".")
    for cut in range(len(parts) - 1, 0, -1):
        owner = ".".join(parts[:cut])
        if owner in mapping["PACKAGE"]:
            simple = parts[cut]
            return ".".join([mapping["PACKAGE"][owner], mapping["TYPE"].get(simple, simple), *parts[cut + 1:]])
        if owner in mapping["EXTERNAL"]:
            return fqn
    if fqn.startswith(JDK_PREFIXES):
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(repo: str, donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
            % (BLOB_URL % repo, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def mapped_line(line: bytes, mapping: dict, donor_path: str, seen: dict) -> bytes:
    text, eol = split_eol(line)
    imported = IMPORT_LINE.match(text)
    if imported:
        line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
            + imported.group(3) + eol
    return rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen)


def relocate(data: bytes, repo: str, donor_path: str, blob: str, commit: str, mapping: dict,
             seen: dict) -> tuple[bytes, bool, bool]:
    """The relocated bytes, body_match and body_match_renamed (bytes after the package line, minus the anchor)."""
    lines = LINES.findall(data)
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    out = [text + first_eol for text in HEADER]
    donor_body: list = []
    target_body: list = []
    anchored = False
    in_body = False
    if index < len(lines) and not PACKAGE_LINE.match(split_eol(lines[index])[0])             and not any(PACKAGE_LINE.match(split_eol(line)[0]) for line in lines[index:]):
        if DEFAULT_PACKAGE not in mapping["PACKAGE"]:
            raise Refused("default-package file %s without a PACKAGE row for %s" % (donor_path, DEFAULT_PACKAGE))
        eol = split_eol(lines[index])[1] or first_eol
        out.append(b"package " + mapping["PACKAGE"][DEFAULT_PACKAGE].encode() + b";" + eol + eol)
        in_body = True
    for line in lines[index:]:
        text, eol = split_eol(line)
        package = None if in_body else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            out.append(b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + eol)
            in_body = True
            continue
        if in_body:
            donor_body.append(line)
        if not anchored and not IMPORT_LINE.match(text) and TYPE_LINE.match(text):
            out.append(anchor(repo, donor_path, blob, commit) + eol)
            anchored = True
        line = mapped_line(line, mapping, donor_path, seen)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    target_bytes = b"".join(target_body)
    renamed = b"".join(mapped_line(line, mapping, donor_path, {}) for line in donor_body)
    return b"".join(out), b"".join(donor_body) == target_bytes, renamed == target_bytes


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def pinned_commit(donor: Path, mapping: dict, rev: str | None) -> str:
    repo, pin = mapping["REPO"]
    commit = git(donor, "rev-parse", pin + "^{commit}").decode().strip()
    if commit != pin:
        raise Refused("REPO row of %s pins %s, which %s does not resolve to itself" % (MAP_NAME, pin, donor))
    if rev is not None:
        given = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
        if given != pin:
            raise Refused("--donor-rev %s resolves to %s, the REPO row of %s pins %s" % (rev, given, MAP_NAME, pin))
    return commit


def run(module: Path, recipe_dir: Path, donor: Path, rev: str | None, check: bool) -> int:
    mapping = read_map(recipe_dir)
    repo = mapping["REPO"][0]
    commit = pinned_commit(donor, mapping, rev)
    rows, changed, seen = [], 0, {}
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", commit + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed = relocate(data, repo, donor_path, blob, commit, mapping, seen)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(), target))
        rows.append((repo, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", commit + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s %s" % (kind, donor_path, blob[:12], target, note[:120]))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d held=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), len(mapping["HELD"]), changed,
        "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", default=None, help="donor revision; must resolve to the REPO row's commit")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/nlp)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
