#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_SCOPED_NGRAMS v1 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv from a donor git tree (--donor-git DIR at --donor-rev REV) into this
module, rewriting only (1) the leading license header lines, replaced by the M3JDK header, (2) the package line,
through the PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE and TYPE rows, (4) the
TYPE rows whose donor and target names differ, and (5) one //FLLDCIM: anchor line inserted before the top-level type
declaration, naming the donor blob URL at the donor commit. Every other byte of the file is kept (CRLF too).
PROVENANCE.tsv pins donor commit, blob sha, donor sha256 and target sha256 of every file and states body_match:
TRUE when the bytes after the package line (minus the anchor) are identical to the donor's, TYPES when they are
identical after the declared TYPE renames (the only edit this recipe makes inside a body), FALSE otherwise.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package (java., javax., jdk., com.sun.), nor a mapped donor package, nor a
declared EXTERNAL package of the map (port the closure: add the file or the dependency to the map), or a file
without a top-level type.

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
VERSION = "v1"
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.", "com.sun.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")


class Refused(Exception):
    pass


def read_map(module: Path) -> dict:
    rows = [line.split("\t") for line in (module / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}}
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        else:
            mapping[kind][donor] = target
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


def map_import(fqn: str, static: bool, mapping: dict, donor_path: str) -> str:
    """A type import names package.Type (or package.*); a static import names package.Type.member (or .*)."""
    package = (fqn.rpartition(".")[0] if static else fqn).rpartition(".")[0]
    type_name, _dot, member = fqn[len(package) + 1:].partition(".")
    if package in mapping["PACKAGE"]:
        return mapping["PACKAGE"][package] + "." + mapping["TYPE"].get(type_name, type_name) + _dot + member
    if fqn.startswith(JDK_PREFIXES) or package in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe m3/nlp/recipe/%s %s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE, VERSION)).encode()


def body_match(donor_body: list[bytes], target_body: list[bytes], types: dict) -> str:
    """TRUE: bytes identical; TYPES: identical after the declared TYPE renames; FALSE otherwise."""
    donor, target = b"".join(donor_body), b"".join(target_body)
    if donor == target:
        return "TRUE"
    if b"".join(rename_types(line, types) for line in donor_body) == target:
        return "TYPES"
    return "FALSE"


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, str]:
    """The relocated bytes and body_match (bytes after the package line, minus the anchor, against the donor's)."""
    lines = LINES.findall(data)
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    out = [text + first_eol for text in HEADER]
    donor_body: list[bytes] = []
    target_body: list[bytes] = []
    anchored = False
    in_body = False
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
        imported = IMPORT_LINE.match(text)
        if imported:
            line = imported.group(1) + map_import(imported.group(2).decode(), b"static" in imported.group(1),
                                                  mapping, donor_path).encode() + imported.group(3) + eol
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit) + eol)
            anchored = True
        line = rename_types(line, mapping["TYPE"])
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    return b"".join(out), body_match(donor_body, target_body, mapping["TYPE"])


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(module: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(module)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, match = relocate(data, donor_path, blob, commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s %s" % ("CHANGED" if state else "UNCHANGED", match, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated), match,
                     RECIPE + " " + VERSION))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(module / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE, VERSION, commit, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(Path(__file__).resolve().parent.parent))
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
