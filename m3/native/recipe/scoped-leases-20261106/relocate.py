#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_SCOPED_LEASES v1 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/native),
rewriting only (1) the leading license header (// lines, # lines or one leading /* */ block of Copyright/SPDX lines),
replaced by the M3JDK header in the file's comment style, (2) the package line, through the PACKAGE rows, (3) import
lines of mapped packages and types, through the PACKAGE, TYPE and EXTERNAL rows (a static import resolves through
the package of its class), (4) the TYPE rows whose donor and target names differ, as whole-word tokens, (5) the
dotted donor package inside string literals and text blocks, through the PACKAGE rows, (6) the JNI rows (symbol and
javac -h header prefixes) and the TEXT rows (literal text) on every line, (7) one import per DEPENDENCY row whose
simple type a Java file names in code (comment lines excluded), inserted after the last import, and (8) one
//FLLDCIM: anchor line (#FLLDCIM: in a CMake file) before the top-level type of a Java file or right after the header
of a C or CMake file, naming the donor blob URL at the donor commit. Every other byte of the file is kept (CRLF too).
PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every file and
states body_match (the bytes after the package line, minus the anchor and the added imports, equal the donor's) and
body_match_renamed (they equal the donor's bytes with the TYPE, PACKAGE-literal, JNI and TEXT rows applied: only the
declared names differ).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), or a Java file without a top-level type.

Derived from m3/arrays/recipe/csr-index-20261106/relocate.py v1 (RELOCATE_CSR_INDEX) and
m3/compiler/lifetime-slots/recipe/relocate.py v1 (RELOCATE_LIFETIME_SLOT_PLANNER): DEPENDENCY, JNI and TEXT rows,
C and CMake file kinds, block-comment headers and static imports added for the Java + JNI lease family.

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
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/native/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "recipe")
COPYRIGHT = b"Copyright 2026 Hitesh Soliwal and contributors"
SPDX = b"SPDX-License-Identifier: Apache-2.0"
HEADERS = {"java": (b"// " + COPYRIGHT, b"// " + SPDX),
           "c": (b"/*", b" * " + COPYRIGHT, b" * " + SPDX, b" */"),
           "cmake": (b"# " + COPYRIGHT, b"# " + SPDX)}
ANCHOR_PREFIX = {"java": b"//", "c": b"//", "cmake": b"#"}
HEADER_LINE = re.compile(rb"^\s*(?://|/\*|\*|#)\s*(?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
BLOCK_OPEN = re.compile(rb"^/\*\s*$")
BLOCK_CLOSE = re.compile(rb"^\s*\*/\s*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(static\s+)?)([\w.*]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
COMMENT_LINE = re.compile(rb"^\s*(?://|/\*|\*)")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
ROW_KINDS = ("PACKAGE", "TYPE", "DEPENDENCY", "EXTERNAL", "JNI", "TEXT")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": []}
    mapping.update({kind: {} for kind in ROW_KINDS})
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        else:
            mapping[kind][donor] = target
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def file_kind(target: str) -> str:
    if target.endswith(".java"):
        return "java"
    if target.endswith((".c", ".h")):
        return "c"
    if target.endswith("CMakeLists.txt") or target.endswith(".cmake"):
        return "cmake"
    raise Refused("unknown file kind of %s (java, c, h or CMake only)" % target)


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def skip_header(lines: list[bytes]) -> int:
    """Index of the first line after the donor's license header (// or # lines, /* */ one-liners, one block)."""
    index = 0
    if lines and BLOCK_OPEN.match(split_eol(lines[0])[0]):
        end = 1
        while end < len(lines) and HEADER_LINE.match(lines[end]):
            end += 1
        if end < len(lines) and end > 1 and BLOCK_CLOSE.match(split_eol(lines[end])[0]):
            index = end + 1
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    return index


def rename_names(line: bytes, mapping: dict) -> bytes:
    """The TYPE rows (whole words), the PACKAGE rows as dotted literals, then the JNI and TEXT rows, in map order."""
    for donor, target in mapping["TYPE"].items():
        if donor != target:
            line = re.sub(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
    for donor, target in mapping["PACKAGE"].items():
        line = re.sub(rb"(?<![\w.])" + re.escape(donor.encode()) + rb"\.", target.encode() + b".", line)
    for kind in ("JNI", "TEXT"):
        for donor, target in mapping[kind].items():
            line = line.replace(donor.encode(), target.encode())
    return line


def map_import(fqn: str, static: bool, mapping: dict, donor_path: str) -> str:
    owner, _dot, simple = fqn.rpartition(".")
    if static:
        package, _dot, type_name = owner.rpartition(".")
        if package in mapping["PACKAGE"]:
            return mapping["PACKAGE"][package] + "." + mapping["TYPE"].get(type_name, type_name) + "." + simple
        if owner.startswith(JDK_PREFIXES) or package in mapping["EXTERNAL"]:
            return fqn
    elif owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + mapping["TYPE"].get(simple, simple)
    elif fqn.startswith(JDK_PREFIXES) or owner in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(kind: str, donor_path: str, blob: str, commit: str) -> bytes:
    return ANCHOR_PREFIX[kind] + ("FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
                                  % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def dependency_imports(body: list[bytes], mapping: dict) -> list[bytes]:
    """One import per DEPENDENCY row whose simple type a code line (not a comment line) of the body names."""
    code = b"\n".join(line for line in body if not COMMENT_LINE.match(line))
    return [b"import " + fqn.encode() + b";" for simple, fqn in mapping["DEPENDENCY"].items()
            if re.search(rb"\b" + simple.encode() + rb"\b", code)]


def relocate_java(lines: list[bytes], index: int, eol: bytes, donor_path: str, blob: str, commit: str,
                  mapping: dict) -> tuple[list[bytes], list[bytes], list[bytes], list[bytes]]:
    """Header-free Java lines -> (output lines, donor body, target body minus anchor/added imports, added imports)."""
    out: list[bytes] = []
    donor_body: list[bytes] = []
    target_body: list[bytes] = []
    anchored = in_body = False
    last_import = -1
    for line in lines[index:]:
        text, line_eol = split_eol(line)
        package = None if in_body else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            out.append(b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + line_eol)
            in_body = True
            continue
        if in_body:
            donor_body.append(line)
        imported = IMPORT_LINE.match(text)
        if imported:
            fqn = map_import(imported.group(3).decode(), imported.group(2) is not None, mapping, donor_path)
            line = imported.group(1) + fqn.encode() + imported.group(4) + line_eol
            last_import = len(out)
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor("java", donor_path, blob, commit) + line_eol)
            anchored = True
        line = rename_names(line, mapping)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    added = [imp + eol for imp in dependency_imports(donor_body, mapping)]
    if added:
        at = last_import + 1 if last_import >= 0 else 1
        out[at:at] = added
    return out, donor_body, target_body, added


def relocate_text(lines: list[bytes], index: int, eol: bytes, kind: str, donor_path: str, blob: str, commit: str,
                  mapping: dict) -> tuple[list[bytes], list[bytes], list[bytes]]:
    """Header-free C or CMake lines -> (output lines with the anchor first, donor body, target body minus anchor)."""
    donor_body = lines[index:]
    target_body = [rename_names(line, mapping) for line in donor_body]
    return [anchor(kind, donor_path, blob, commit) + eol] + target_body, donor_body, target_body


def relocate(data: bytes, donor_path: str, target_path: str, blob: str, commit: str,
             mapping: dict) -> tuple[bytes, bool, bool]:
    """The relocated bytes, body_match and body_match_renamed (see the module docstring)."""
    kind = file_kind(target_path)
    lines = LINES.findall(data)
    eol = split_eol(lines[0])[1] if lines else b"\n"
    index = skip_header(lines)
    out = [text + eol for text in HEADERS[kind]]
    if kind == "java":
        body_out, donor_body, target_body, _added = relocate_java(lines, index, eol, donor_path, blob, commit, mapping)
    else:
        body_out, donor_body, target_body = relocate_text(lines, index, eol, kind, donor_path, blob, commit, mapping)
    out.extend(body_out)
    donor_bytes, target_bytes = b"".join(donor_body), b"".join(target_body)
    renamed = b"".join(rename_names(line, mapping) for line in donor_body)
    return b"".join(out), donor_bytes == target_bytes, renamed == target_bytes


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed = relocate(data, donor_path, target, blob, commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s %s" % ("CHANGED" if state else "UNCHANGED",
              str(body_match).upper(), str(body_match_renamed).upper(), target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), RECIPE + " " + VERSION))
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
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/native)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
