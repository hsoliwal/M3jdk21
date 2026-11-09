#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_HYBRID_INT_SET v3 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/arrays).
A FILE row may name a hash-pinned postimage (.java.txt) of the donor: the newest complete lineage of one file can
live in a reviewed postimage that the donor's main path does not carry yet.

Java files (.java, or a hash-pinned .java.txt postimage): rewrites only (1) the leading license header lines,
replaced by the M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped packages
and types, through the PACKAGE and TYPE rows (an import of a nested member such as a.b.Type.Nested.MEMBER maps its
longest mapped package prefix and the type that follows it), (4) the TYPE rows whose donor and target names differ,
as whole-word tokens, (5) the LITERAL rows, declared string-literal tokens renamed as whole tokens, exactly the
rename the JDK naming rules would apply (m3/tooling/jdk-naming/NAMING_RULES.tsv) so that jdk_naming.py and this
recipe share one fixed point, (6) the DROP rows: a declared donor line (pinned by the sha256 of its bytes) removed
from a named file, refused unless it occurs exactly once, and (7) one //FLLDCIM: anchor line inserted before the
top-level type declaration, naming the donor blob URL at the donor commit. Every other byte is kept (CRLF too).

NEC rows name donor files or module owners whose equivalence with the relocated family is NOT decided here (never
two owners of one shape); OMIT rows name donor files left out by decision (a timing harness, an OpenRewrite recipe
test, a JNI kernel without a Java consumer). Both kinds are informational: the recipe pins the donor blob of a donor
path in its run log (HELD lines) and copies nothing; a module owner (module:<path>) is logged as it is.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row
and states body_match (the bytes after the header/package line, minus the anchor, equal the donor's),
body_match_renamed (they equal the donor's after the TYPE, LITERAL and DROP rows are applied to the donor bytes:
only the declared names and lines differ) and declared_drops (the DROP rows applied to the file).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a Java file without a top-level type, a DROP row
whose line is absent or ambiguous, a LITERAL row whose donor token is absent from every FILE row, or a non-Java file.

Derived from m3/arrays/recipe/bitmatrix-20261106/relocate.py v3 (RELOCATE_BIT_MATRIX, PR #2521): the C/JNI/INCLUDE
rows and the CMake registration are dropped (this leaf relocates no native file), hash-pinned .java.txt postimages
as FILE sources and module owners in NEC/OMIT rows are added; everything else unchanged.

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
VERSION = "v3"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/arrays/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_drops", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "DROP": [], "LITERAL": {}, "HELD": []}
    for kind, donor, target, *note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "DROP":
            mapping["DROP"].append((donor, target))
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, note[0] if note else ""))
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


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                  seen: dict) -> tuple[bytes, bool, bool, int]:
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
        line = rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen)
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
        return rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], {})

    renamed = b"".join(mapped(line) for line in donor_body)
    target_bytes = b"".join(target_body)
    # body_match compares the donor bytes after its package line before any DROP row (so a drop makes it FALSE).
    after_package = next((at for at, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    return b"".join(out), b"".join(lines[after_package + 1:]) == target_bytes, renamed == target_bytes, dropped


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, seen: dict) -> tuple[bytes, bool, bool, int]:
    if not donor_path.endswith((".java", ".java.txt")):
        raise Refused("not a Java file: %s (this recipe relocates no native file)" % donor_path)
    return relocate_java(data, donor_path, blob, commit, mapping, seen)


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
    rows, changed, seen = [], 0, {}
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed, dropped = relocate(data, donor_path, blob, commit, mapping, seen)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_drops=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(), dropped, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    for kind, donor_path, target, note in mapping["HELD"]:
        if donor_path.startswith("module:"):
            print("HELD %s %s (module owner, no donor blob) target=%s" % (kind, donor_path, target))
            continue
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s" % (kind, donor_path, blob[:12], target))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d held=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), len(mapping["HELD"]), changed, "CHANGED" if provenance_state else "UNCHANGED"))
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
