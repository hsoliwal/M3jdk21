#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_POSTING_INDEX v2 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/arrays).

Java files (.java): rewrites only (1) the leading license header lines, replaced by the M3JDK header, (2) the
package line, through the PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE and TYPE
rows (an import of a nested member such as a.b.Type.Nested.MEMBER maps its longest mapped package prefix and the
type that follows it), (4) the TYPE rows whose donor and target names differ, as whole-word tokens, (5) the DROP
rows: a declared donor line (pinned by the sha256 of its bytes) removed from a named file, refused unless it occurs
exactly once, and (6) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor
blob URL at the donor commit. Every other byte of the file is kept (CRLF too).

C files (.c, .h): rewrites only (1) the first line, a block-comment SPDX line, replaced by the M3JDK header inside
the same block comment, (2) #include lines through the INCLUDE rows (the javac -h header of the donor class becomes
<jni.h>, as every JNI source of this module includes it), (3) the JNI rows, the Java_<mangled donor class>_ symbol
prefix of every exported function, as whole-token prefixes, (4) the TYPE rows as whole-word tokens, and (5) one
//FLLDCIM: anchor line inserted before the first #include. Every other byte is kept.

The native build registration is part of the recipe too: the CMake block REGISTRATION (below) is appended to
src/main/native/CMakeLists.txt once (idempotent, --check reports it), the one documented build-file addition.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every file and
states body_match (the bytes after the header/package line, minus the anchor, equal the donor's), body_match_renamed
(they equal the donor's after the TYPE, JNI, INCLUDE and DROP rows are applied to the donor bytes: only the declared
names and lines differ) and declared_drops (the DROP rows applied to the file).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a Java file without a top-level type, a C file
without an SPDX first line or #include, an #include of a mapped-package header without an INCLUDE row, a DROP row
whose line is absent or ambiguous, or a C file that still names a JNI symbol of an unmapped donor package.

Derived from m3/arrays/recipe/csr-index-20261106/relocate.py v1 (RELOCATE_CSR_INDEX, PR #2499): C/JNI/INCLUDE/DROP
rows, nested-member imports, CMake registration and declared_drops added.

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
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/arrays/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_drops", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
C_HEADER = (b"/* Copyright 2026 Hitesh Soliwal and contributors", b" * SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
C_HEADER_LINE = re.compile(rb"^/\* SPDX-License-Identifier: [\w.-]+\s*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
INCLUDE_LINE = re.compile(rb"^(#include\s+)([<\"][^>\"]+[>\"])(.*)$")
JNI_SYMBOL = re.compile(rb"\bJava_(\w+)")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
CMAKE_PATH = "src/main/native/CMakeLists.txt"
REGISTRATION = b"""
# Synexia CSR posting-list union/intersection/count kernels join the existing arrays JNI owner
# (recipe m3/arrays/recipe/posting-index-20261106): Java_jdk_m3_arrays_M3IntPostingIndex_* take one
# validated immutable query batch, use GetIntArrayElements with JNI_ABORT release only, and retain no pointer.
target_sources(m3_arrays PRIVATE
  m3_posting_merge.c)
"""


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "JNI": {}, "INCLUDE": {}, "DROP": []}
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "DROP":
            mapping["DROP"].append((donor, target))
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


def rename_jni(line: bytes, jni: dict) -> bytes:
    for donor, target in jni.items():
        line = re.sub(rb"(?<![\w])" + re.escape(donor.encode()) + rb"(?=\w)", target.encode(), line)
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


def anchor(donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def drops_for(donor_path: str, mapping: dict) -> set:
    return {line_sha for path, line_sha in mapping["DROP"] if path == donor_path}


def apply_drops(lines: list, donor_path: str, drops: set) -> tuple[list, int]:
    """Removes every declared line (sha256 of its bytes without the end of line); refuses unless each occurs once."""
    if not drops:
        return lines, 0
    kept, found = [], {sha: 0 for sha in drops}
    for line in lines:
        sha = hashlib.sha256(split_eol(line)[0]).hexdigest()
        if sha in found:
            found[sha] += 1
            continue
        kept.append(line)
    for sha, count in found.items():
        if count != 1:
            raise Refused("DROP row %s of %s matched %d lines, expected exactly 1" % (sha[:12], donor_path, count))
    return kept, len(drops)


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, bool, bool, int]:
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
    drops = drops_for(donor_path, mapping)
    body_lines, dropped = apply_drops(lines[index:], donor_path, drops)
    for line in body_lines:
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
            line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
                + imported.group(3) + eol
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit) + eol)
            anchored = True
        line = rename_types(line, mapping["TYPE"])
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    def mapped(line: bytes) -> bytes:
        text, eol = split_eol(line)
        imported = IMPORT_LINE.match(text)
        if imported:
            line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
                + imported.group(3) + eol
        return rename_types(line, mapping["TYPE"])

    renamed = b"".join(mapped(line) for line in donor_body)
    target_bytes = b"".join(target_body)
    # body_match compares the donor bytes after its package line before any DROP row (so a drop makes it FALSE).
    after_package = next((at for at, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    return b"".join(out), b"".join(lines[after_package + 1:]) == target_bytes, renamed == target_bytes, dropped


def map_include(spec: bytes, mapping: dict, donor_path: str) -> bytes:
    key = spec.decode()
    if key in mapping["INCLUDE"]:
        return mapping["INCLUDE"][key].encode()
    header = key.strip("<>\"")
    for donor_package in mapping["PACKAGE"]:
        if header.startswith(donor_package.replace(".", "_") + "_"):
            raise Refused("#include %s of a mapped-package header in %s without an INCLUDE row" % (key, donor_path))
    return spec


def relocate_c(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, bool, bool, int]:
    lines = LINES.findall(data)
    if not lines or not C_HEADER_LINE.match(split_eol(lines[0])[0]):
        raise Refused("C file %s does not start with a block-comment SPDX line" % donor_path)
    first_eol = split_eol(lines[0])[1] or b"\n"
    out = [text + first_eol for text in C_HEADER]
    donor_body, target_body = [], []
    anchored = False
    drops = drops_for(donor_path, mapping)
    body_lines, dropped = apply_drops(lines[1:], donor_path, drops)
    for line in body_lines:
        text, eol = split_eol(line)
        donor_body.append(line)
        included = INCLUDE_LINE.match(text)
        if included:
            if not anchored:
                out.append(anchor(donor_path, blob, commit) + eol)
                anchored = True
            line = included.group(1) + map_include(included.group(2), mapping, donor_path) + included.group(3) + eol
        line = rename_types(rename_jni(line, mapping["JNI"]), mapping["TYPE"])
        for symbol in JNI_SYMBOL.finditer(split_eol(line)[0]):
            owner = symbol.group(1).decode()
            if any(owner.startswith(package.replace(".", "_") + "_") for package in mapping["PACKAGE"]):
                raise Refused("JNI symbol %s of a mapped donor package in %s without a JNI row"
                              % (symbol.group(0).decode(), donor_path))
        out.append(line)
        target_body.append(line)
    if not anchored:
        raise Refused("no #include in %s" % donor_path)

    def renamed(line: bytes) -> bytes:
        text, eol = split_eol(line)
        included = INCLUDE_LINE.match(text)
        if included:
            line = included.group(1) + map_include(included.group(2), mapping, donor_path) + included.group(3) + eol
        return rename_types(rename_jni(line, mapping["JNI"]), mapping["TYPE"])

    donor_bytes, target_bytes = b"".join(donor_body), b"".join(target_body)
    return b"".join(out), b"".join(lines[1:]) == target_bytes, b"".join(renamed(l) for l in donor_body) == target_bytes, dropped


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, bool, bool, int]:
    if donor_path.endswith((".c", ".h")):
        return relocate_c(data, donor_path, blob, commit, mapping)
    return relocate_java(data, donor_path, blob, commit, mapping)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def register_native(module: Path, check: bool) -> bool:
    """Appends the CMake registration block once; the block is the one documented native-build addition."""
    cmake = module / CMAKE_PATH
    data = cmake.read_bytes()
    if REGISTRATION.strip() in data:
        return False
    if not check:
        cmake.write_bytes(data + (b"" if data.endswith(b"\n") else b"\n") + REGISTRATION)
    return True


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed, dropped = relocate(data, donor_path, blob, commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_drops=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(), dropped, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), RECIPE + " " + VERSION))
    registered = register_native(module, check)
    changed += registered
    print("BUILD %s %s (%s)" % ("CHANGED" if registered else "UNCHANGED", CMAKE_PATH, "registration appended" if registered else "registration present"))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/arrays)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
