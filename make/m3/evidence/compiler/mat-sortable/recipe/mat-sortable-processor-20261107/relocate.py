#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_MAT_SORTABLE_PROCESSOR v1 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory,
m3/compiler/mat-sortable).

Java files (.java): rewrites only (1) the leading license header lines, replaced by the M3JDK header, (2) the
package line, through the PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE and TYPE
rows (an import of a nested member such as a.b.Type.Nested.MEMBER maps its longest mapped package prefix and the
type that follows it), (4) the TYPE rows whose donor and target names differ, as whole-word tokens, (5) the LITERAL
rows, declared string-literal tokens (a system property name) renamed as whole tokens, exactly the rename the JDK
naming rules would apply (make/m3/tooling/jdk-naming/NAMING_RULES.tsv) so that jdk_naming.py and this recipe share one
fixed point, (6) the DROP rows: a declared donor line (pinned by the sha256 of its bytes) removed from a named file,
refused unless it occurs exactly once, (7) the REPLACE rows: a declared contiguous block of donor lines (pinned by
the sha256 of its bytes without end-of-line characters, joined by one LF, and by its line count) replaced in a named
file by the declared replacement lines (an empty replacement, written '-', removes the block), refused unless the
block occurs exactly once, and (8) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming
the donor blob URL at the donor commit. Every other byte of the file is kept (CRLF too).

C files (.c, .h): rewrites only (1) the first line, a block-comment SPDX line, replaced by the M3JDK header inside
the same block comment, or a line-comment SPDX line (// SPDX-License-Identifier: ...), replaced by the two-line M3JDK
header in line comments (native extension, see below), (2) #include lines through the INCLUDE rows (the javac -h header of the donor class becomes
<jni.h>, as every JNI source of this module includes it), (3) the JNI rows, the Java_<mangled donor class>_ symbol
prefix of every exported function, as whole-token prefixes, (4) the TYPE and LITERAL rows as whole-word tokens, and
(5) one //FLLDCIM: anchor line inserted before the first #include. Every other byte is kept.

NEC rows name donor files of the family that are NOT relocated because their equivalence with an existing owner of
the module is not decided here (never two owners of one shape); OMIT rows name donor files left out by decision
(a timing harness, JUnit wrappers replaced by the module template). Both kinds are informational: the recipe pins
their donor blob in its run log (HELD lines) and copies nothing.

CMake files (FILE rows whose donor path ends in CMakeLists.txt; native extension): rewrites only (1) the leading
# SPDX / # Copyright comment lines, replaced by the M3JDK header as # comments, (2) the TYPE and LITERAL rows as
whole-word tokens, and (3) one #FLLDCIM: anchor line inserted right after the header. Every other byte is kept.

Native extension (native leaf of the family, 2026-11-06): the donor's JNI kernels carry their own CMakeLists.txt, which
is relocated as a FILE row into src/main/native, so the module's native build is the donor's build file with the
M3JDK header and no registration into another module's CMake file is written (the bitmatrix CMake registration block
and its writer stay removed). The extension is additive: the Java path, the block-comment C path, the version (v3) and
every anchor of the Java and C rows relocated before it are unchanged (relocate.py --check stays a fixed point).

Mat-sortable family (this recipe): two Java files with '// SPDX' line headers, no native file, no CMake file; the C
and CMake paths are kept unchanged and unused. The REPLACE rows hold the members that depend on owners the mat lane
still holds (MatCapabilityEmitter, MatIntOrder): their lines are declared in RELOCATION_MAP.tsv, counted in
PROVENANCE.tsv (declared_replaces) and make body_match FALSE, as a DROP row does.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row
and states body_match (the bytes after the header/package line, minus the anchor, equal the donor's),
body_match_renamed (they equal the donor's after the TYPE, LITERAL, JNI, INCLUDE, DROP and REPLACE rows are applied
to the donor bytes: only the declared names and lines differ), declared_drops (the DROP rows applied to the file) and
declared_replaces (the REPLACE rows applied to the file).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a Java file without a top-level type, a C file
without an SPDX first line or #include, an #include of a mapped-package header without an INCLUDE row, a DROP row
whose line is absent or ambiguous, a REPLACE row whose block is absent or ambiguous, a LITERAL row whose donor token
is absent from every FILE row, or a C file that still names a JNI symbol of an unmapped donor package.

Derived from m3/compiler/static-use/recipe/static-use-plan-20261107/relocate.py v1 (RELOCATE_STATIC_USE_PLAN, #2661
and #2675) by a scripted exact-anchor derivation: the recipe name, recipe path and default module change and the
REPLACE row kind (with its PROVENANCE.tsv column declared_replaces) is added; the relocation of every FILE row, the
LITERAL / DROP / NEC / OMIT / JNI / INCLUDE rows, the C and CMake paths and the refusals are unchanged.

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
RECIPE_PATH = "m3/compiler/mat-sortable/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_drops", "declared_replaces",
                      "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
C_HEADER = (b"/* Copyright 2026 Hitesh Soliwal and contributors", b" * SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
C_HEADER_LINE = re.compile(rb"^(?:/\* |// )SPDX-License-Identifier: [\w.-]+\s*$")
CMAKE_HEADER_LINE = re.compile(rb"^# (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
CMAKE_HEADER = (b"# Copyright 2026 Hitesh Soliwal and contributors", b"# SPDX-License-Identifier: Apache-2.0")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
INCLUDE_LINE = re.compile(rb"^(#include\s+)([<\"][^>\"]+[>\"])(.*)$")
JNI_SYMBOL = re.compile(rb"\bJava_(\w+)")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "JNI": {}, "INCLUDE": {}, "DROP": [],
                     "REPLACE": [], "LITERAL": {}, "HELD": []}
    for kind, donor, target, *note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "DROP":
            mapping["DROP"].append((donor, target))
        elif kind == "REPLACE":
            mapping["REPLACE"].append((donor, target, note[0] if note else "-"))
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


def replaces_for(donor_path: str, mapping: dict) -> list:
    return [(spec, replacement) for path, spec, replacement in mapping["REPLACE"] if path == donor_path]


def apply_replaces(lines: list, donor_path: str, replaces: list) -> tuple[list, int]:
    """Replaces every declared block (sha256 of its lines without end of line, joined by LF, and its line count) by
    the declared lines ('-' removes the block); refuses unless each block occurs exactly once."""
    if not replaces:
        return lines, 0
    texts = [split_eol(line)[0] for line in lines]
    for spec, replacement in replaces:
        sha, count = spec.split(":")
        count = int(count)
        starts = [at for at in range(len(texts) - count + 1)
                  if hashlib.sha256(b"\n".join(texts[at:at + count])).hexdigest() == sha]
        if len(starts) != 1:
            raise Refused("REPLACE row %s of %s matched %d blocks, expected exactly 1" % (sha[:12], donor_path, len(starts)))
        eol = split_eol(lines[starts[0]])[1] or b"\n"
        new_lines = [] if replacement == "-" else [text.encode() + eol for text in replacement.split("\\n")]
        lines = lines[:starts[0]] + new_lines + lines[starts[0] + count:]
        texts = [split_eol(line)[0] for line in lines]
    return lines, len(replaces)


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                  seen: dict) -> tuple[bytes, bool, bool, int, int]:
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
    body_lines, replaced = apply_replaces(body_lines, donor_path, replaces_for(donor_path, mapping))
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
    # body_match compares the donor bytes after its package line before any DROP or REPLACE row (so a drop or a
    # replacement makes it FALSE).
    after_package = next((at for at, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    return b"".join(out), b"".join(lines[after_package + 1:]) == target_bytes, renamed == target_bytes, dropped, replaced


def map_include(spec: bytes, mapping: dict, donor_path: str) -> bytes:
    key = spec.decode()
    if key in mapping["INCLUDE"]:
        return mapping["INCLUDE"][key].encode()
    header = key.strip("<>\"")
    for donor_package in mapping["PACKAGE"]:
        if header.startswith(donor_package.replace(".", "_") + "_"):
            raise Refused("#include %s of a mapped-package header in %s without an INCLUDE row" % (key, donor_path))
    return spec


def relocate_c(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
               seen: dict) -> tuple[bytes, bool, bool, int, int]:
    lines = LINES.findall(data)
    if not lines or not C_HEADER_LINE.match(split_eol(lines[0])[0]):
        raise Refused("C file %s does not start with a block-comment SPDX line" % donor_path)
    first_eol = split_eol(lines[0])[1] or b"\n"
    out = [text + first_eol for text in (HEADER if lines[0].startswith(b"//") else C_HEADER)]
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
        line = rename_literals(rename_types(rename_jni(line, mapping["JNI"]), mapping["TYPE"]), mapping["LITERAL"], seen)
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
        return rename_literals(rename_types(rename_jni(line, mapping["JNI"]), mapping["TYPE"]), mapping["LITERAL"], {})

    donor_bytes, target_bytes = b"".join(donor_body), b"".join(target_body)
    return b"".join(out), b"".join(lines[1:]) == target_bytes, b"".join(renamed(l) for l in donor_body) == target_bytes, dropped, 0


def relocate_cmake(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                   seen: dict) -> tuple[bytes, bool, bool, int, int]:
    lines = LINES.findall(data)
    index = 0
    while index < len(lines) and CMAKE_HEADER_LINE.match(lines[index]):
        index += 1
    if index == 0:
        raise Refused("CMake file %s does not start with a # SPDX line" % donor_path)
    eol = split_eol(lines[0])[1] or b"\n"
    out = [text + eol for text in CMAKE_HEADER] + [b"#" + anchor(donor_path, blob, commit)[2:] + eol]
    donor_body = lines[index:]
    target_body = [rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen) for line in donor_body]
    renamed = [rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], {}) for line in donor_body]
    target_bytes = b"".join(target_body)
    return b"".join(out) + target_bytes, b"".join(donor_body) == target_bytes, b"".join(renamed) == target_bytes, 0, 0


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, seen: dict) -> tuple[bytes, bool, bool, int, int]:
    if donor_path.endswith("CMakeLists.txt"):
        return relocate_cmake(data, donor_path, blob, commit, mapping, seen)
    if donor_path.endswith((".c", ".h")):
        return relocate_c(data, donor_path, blob, commit, mapping, seen)
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
        relocated, body_match, body_match_renamed, dropped, replaced = relocate(data, donor_path, blob, commit, mapping, seen)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_drops=%d declared_replaces=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(), dropped,
            replaced, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), str(replaced),
                     RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s %s" % (kind, donor_path, blob[:12], target, note))
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
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/compiler/mat-sortable)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
