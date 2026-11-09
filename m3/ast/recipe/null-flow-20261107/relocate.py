#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_CENSUS1_REMAINDER v4 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/<module>),
rewriting only (1) the leading license header (// lines, # lines, <!-- --> lines or one leading /* */ block of
Copyright/SPDX lines), replaced by the M3JDK header in the file's comment style, (2) the package line, through the
PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE, TYPE and EXTERNAL rows (a static
import resolves through the package of its class), (4) the TYPE rows whose donor and target names differ, as
whole-word tokens, (5) the dotted donor package inside string literals, text blocks and prose, through the PACKAGE
rows whose donor package has a dot (a one-word donor package such as test is a package line and nothing else),
(6) the JNI rows (symbol and javac -h header prefixes) and the TEXT rows (literal text) on every line, (7) one import
per DEPENDENCY row whose simple type a Java file names in code (comment lines excluded), inserted after the last
import, (8) the DROP_METHOD rows (<DonorType>.<method> -> reason): the whole declaration of that method (its
annotations, signature, body to the closing brace at the signature's indentation, or the signature line alone when
the method opens and closes on it, and one trailing blank line) is removed from the donor's lines before every other rule, and (9) one FLLDCIM anchor line (//FLLDCIM: in Java and C,
#FLLDCIM: in CMake and TSV, <!-- FLLDCIM: --> in Markdown) before the top-level type of a Java file or right after the
header of any other file, naming the donor blob URL at the donor commit. Every other byte of the file is kept (CRLF
too).
PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every file and
states body_match (the bytes after the package line, minus the anchor and the added imports, equal the donor's after
the declared drops), body_match_renamed (they equal those donor bytes with the TYPE, PACKAGE-literal, JNI and TEXT
rows applied: only the declared names differ), dropped_lines (the lines the DROP_METHOD rows removed), and
base_kind: DEVELOP when the file is the donor revision's blob, PR_HEAD_SUPERSET when a BLOB row of the map pins
another blob (blob@commit, refused unless that commit holds the blob at the path) with the donor revision's own blob
and the reason in base_note.

VERBATIM rows copy one donor file byte for byte (no header, anchor or rename: donor-run evidence such as logs, hash
tables and input manifests, whose statements name donor paths and digests that a rename would falsify) to a target
path under an evidence/ directory (the lineage scope of m3/tooling/jdk-naming, which never rewrites it); their
PROVENANCE row states body_match TRUE and base_note VERBATIM. MERGE rows record an edit of an existing module file
that a relocated class needs (path and reason; applied by the recipe's apply.py from EDITS.tsv); the recipe writes
nothing for them. SKIP and DROP rows document non-ports.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a DROP_METHOD row matching no declaration or more
than one, or a Java file without a top-level type.

Derived from m3/optane/recipe/pmem-gram-index-20261106/relocate.py v3 (RELOCATE_PMEM_GRAM_INDEX, itself from
pmem-region-core-20261106 v2 and m3/native/recipe/scoped-leases-20261106 v1): v4 adds the DROP_METHOD rows (the
method_span rule of m3/algorithms/recipe/families-20261106/relocate.py v1) with the dropped_lines column, and derives
RECIPE_PATH from the module directory so one recipe text serves m3/ast/recipe/null-flow-20261107 and
m3/optane/recipe/pmem-accelerator-20261107; every v3 rule is unchanged.

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
VERSION = "v4"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/" + RECIPE_DIR.parent.parent.name + "/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "dropped_lines", "recipe", "base_kind",
                      "base_note")
BASE_DEVELOP = "DEVELOP"
BASE_SUPERSET = "PR_HEAD_SUPERSET"
COPYRIGHT = b"Copyright 2026 Hitesh Soliwal and contributors"
SPDX = b"SPDX-License-Identifier: Apache-2.0"
HEADERS = {"java": (b"// " + COPYRIGHT, b"// " + SPDX),
           "c": (b"/*", b" * " + COPYRIGHT, b" * " + SPDX, b" */"),
           "cmake": (b"# " + COPYRIGHT, b"# " + SPDX),
           "tsv": (b"# " + COPYRIGHT, b"# " + SPDX),
           "md": (b"<!-- " + COPYRIGHT + b" -->", b"<!-- " + SPDX + b" -->")}
ANCHOR = {"java": (b"//", b""), "c": (b"//", b""), "cmake": (b"#", b""), "tsv": (b"#", b""),
          "md": (b"<!-- ", b" -->")}
HEADER_LINE = re.compile(rb"^\s*(?://|/\*|\*|#|<!--)\s*(?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
BLOCK_OPEN = re.compile(rb"^/\*\s*$")
BLOCK_CLOSE = re.compile(rb"^\s*\*/\s*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(static\s+)?)([\w.*]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
COMMENT_LINE = re.compile(rb"^\s*(?://|/\*|\*)")
ANNOTATION_LINE = re.compile(rb"^\s*@\w")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
ROW_KINDS = ("PACKAGE", "TYPE", "DEPENDENCY", "EXTERNAL", "JNI", "TEXT")
SKIP_KINDS = ("SKIP", "DROP", "MERGE")
BLOB_KIND = "BLOB"
VERBATIM_KIND = "VERBATIM"
DROP_METHOD_KIND = "DROP_METHOD"
LINEAGE_DIR = "/evidence/"
VERBATIM_NOTE = "VERBATIM: exact donor bytes (lineage evidence; no header, anchor or rename)"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], BLOB_KIND: {}, VERBATIM_KIND: [], DROP_METHOD_KIND: {}}
    mapping.update({kind: {} for kind in ROW_KINDS})
    for kind, donor, target, *note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == VERBATIM_KIND:
            mapping[VERBATIM_KIND].append((donor, target))
        elif kind == BLOB_KIND:
            blob, _at, commit = target.partition("@")
            mapping[BLOB_KIND][donor] = (blob, commit, note[0] if note else "")
        elif kind == DROP_METHOD_KIND:
            owner, _dot, method = donor.rpartition(".")
            mapping[DROP_METHOD_KIND].setdefault(owner, []).append(method)
        elif kind in SKIP_KINDS:
            continue  # documented non-ports and MERGE rows (a reviewed edit of an existing module file): nothing to relocate
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
    if target.endswith(".tsv"):
        return "tsv"
    if target.endswith(".md"):
        return "md"
    raise Refused("unknown file kind of %s (java, c, h, CMake, tsv or md only)" % target)


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def skip_header(lines: list[bytes]) -> int:
    """Index of the first line after the donor's license header (//, # or <!-- --> lines, one /* */ block)."""
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


def donor_type(donor_path: str) -> str:
    """The donor's top-level type name: the file name up to its first dot (X.java, X.java.template, X.java.txt)."""
    return Path(donor_path).name.split(".")[0]


def method_span(lines: list[bytes], method: str, donor_path: str) -> tuple[int, int]:
    """[first, stop) of the declaration of `method`: its annotations, the signature, the closing brace at the
    signature's indentation and one trailing blank line."""
    declaration = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final|synchronized|default|abstract)\s+)*"
                             rb"(?:<[^>]*>\s+)?[\w.<>\[\], ?]+\s+" + method.encode() + rb"\s*\(")
    starts = [index for index, line in enumerate(lines) if declaration.match(split_eol(line)[0])]
    if len(starts) != 1:
        raise Refused("DROP_METHOD %s: %d declarations in %s" % (method, len(starts), donor_path))
    start = starts[0]
    indent = declaration.match(lines[start]).group(1)
    first = start
    while first > 0 and ANNOTATION_LINE.match(lines[first - 1]) and lines[first - 1].startswith(indent):
        first -= 1
    end = start
    signature = split_eol(lines[start])[0]
    if signature.count(b"{") and signature.count(b"{") == signature.count(b"}") and signature.endswith(b"}"):
        pass  # a one-line method: the declaration closes on its own line
    else:
        while end < len(lines) and split_eol(lines[end])[0] != indent + b"}":
            end += 1
    if end == len(lines):
        raise Refused("DROP_METHOD %s: no closing brace at its indentation in %s" % (method, donor_path))
    stop = end + 1
    if stop < len(lines) and split_eol(lines[stop])[0].strip() == b"":
        stop += 1
    return first, stop


def apply_drops(lines: list[bytes], donor_path: str, mapping: dict) -> tuple[list[bytes], int]:
    """The donor's header-free lines minus every DROP_METHOD declaration of its type, and the count removed."""
    dropped = 0
    for method in mapping[DROP_METHOD_KIND].get(donor_type(donor_path), ()):
        first, stop = method_span(lines, method, donor_path)
        dropped += stop - first
        lines = lines[:first] + lines[stop:]
    return lines, dropped


def rename_names(line: bytes, mapping: dict) -> bytes:
    """The TYPE rows (whole words), the dotted PACKAGE rows as dotted literals, then the JNI and TEXT rows, in map
    order."""
    for donor, target in mapping["TYPE"].items():
        if donor != target:
            line = re.sub(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
    for donor, target in mapping["PACKAGE"].items():
        if "." in donor:
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
    prefix, suffix = ANCHOR[kind]
    return prefix + ("FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
                     % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode() + suffix


def dependency_imports(body: list[bytes], mapping: dict) -> list[bytes]:
    """One import per DEPENDENCY row whose simple type a code line (not a comment line) of the body names."""
    code = b"\n".join(line for line in body if not COMMENT_LINE.match(line))
    return [b"import " + fqn.encode() + b";" for simple, fqn in mapping["DEPENDENCY"].items()
            if re.search(rb"\b" + simple.encode() + rb"\b", code)]


def relocate_java(lines: list[bytes], eol: bytes, donor_path: str, blob: str, commit: str,
                  mapping: dict) -> tuple[list[bytes], list[bytes], list[bytes], list[bytes]]:
    """Header-free, drop-applied Java lines -> (output lines, donor body, target body minus anchor/added imports,
    added imports)."""
    out: list[bytes] = []
    donor_body: list[bytes] = []
    target_body: list[bytes] = []
    anchored = in_body = False
    last_import = -1
    for line in lines:
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


def relocate_text(lines: list[bytes], eol: bytes, kind: str, donor_path: str, blob: str, commit: str,
                  mapping: dict) -> tuple[list[bytes], list[bytes], list[bytes]]:
    """Header-free C, CMake, TSV or Markdown lines -> (output lines with the anchor first, donor body, target body
    minus anchor)."""
    donor_body = lines
    target_body = [rename_names(line, mapping) for line in donor_body]
    return [anchor(kind, donor_path, blob, commit) + eol] + target_body, donor_body, target_body


def relocate(data: bytes, donor_path: str, target_path: str, blob: str, commit: str,
             mapping: dict) -> tuple[bytes, bool, bool, int]:
    """The relocated bytes, body_match, body_match_renamed and dropped_lines (see the module docstring)."""
    kind = file_kind(target_path)
    lines = LINES.findall(data)
    eol = split_eol(lines[0])[1] if lines else b"\n"
    body_lines, dropped = apply_drops(lines[skip_header(lines):], donor_path, mapping) if kind == "java" \
        else (lines[skip_header(lines):], 0)
    out = [text + eol for text in HEADERS[kind]]
    if kind == "java":
        body_out, donor_body, target_body, _added = relocate_java(body_lines, eol, donor_path, blob, commit, mapping)
    else:
        body_out, donor_body, target_body = relocate_text(body_lines, eol, kind, donor_path, blob, commit, mapping)
    out.extend(body_out)
    donor_bytes, target_bytes = b"".join(donor_body), b"".join(target_body)
    renamed = b"".join(rename_names(line, mapping) for line in donor_body)
    return b"".join(out), donor_bytes == target_bytes, renamed == target_bytes, dropped


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def source_of(donor: Path, rev: str, commit: str, donor_path: str, mapping: dict) -> tuple[str, str, str, str]:
    """(blob, commit of the anchor, base_kind, base_note) of one FILE row: the donor revision's blob, or, for a BLOB
    row, the pinned blob of the named commit (refused when that commit does not hold the blob at the path) with the
    donor revision's own blob recorded as the superseded base."""
    develop_blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
    if donor_path not in mapping[BLOB_KIND]:
        return develop_blob, commit, BASE_DEVELOP, "-"
    blob, pin_commit, note = mapping[BLOB_KIND][donor_path]
    pin_commit = git(donor, "rev-parse", pin_commit + "^{commit}").decode().strip()
    at_pin = git(donor, "rev-parse", pin_commit + ":" + donor_path).decode().strip()
    if at_pin != blob:
        raise Refused("BLOB row of %s: %s holds blob %s at that path, not %s" % (donor_path, pin_commit[:12], at_pin[:12], blob[:12]))
    return blob, pin_commit, BASE_SUPERSET, "develop %s blob %s superseded by %s blob %s: %s" % (
        commit[:12], develop_blob[:12], pin_commit[:12], blob[:12], note)


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob, file_commit, base_kind, base_note = source_of(donor, rev, commit, donor_path, mapping)
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed, dropped = relocate(data, donor_path, target, blob, file_commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s dropped=%d base=%s %s" % ("CHANGED" if state else "UNCHANGED",
              str(body_match).upper(), str(body_match_renamed).upper(), dropped, base_kind, target))
        rows.append((DONOR_REPO, file_commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), RECIPE + " " + VERSION,
                     base_kind, base_note))
    for donor_path, target in mapping[VERBATIM_KIND]:
        if LINEAGE_DIR not in "/" + target:
            raise Refused("VERBATIM target %s is not under an evidence/ directory (lineage scope)" % target)
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        state = write_if_changed(module / target, data, check)
        changed += state
        print("VERBATIM %s %s" % ("CHANGED" if state else "UNCHANGED", target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(data), "TRUE", "TRUE", "0",
                     RECIPE + " " + VERSION, BASE_DEVELOP, VERBATIM_NOTE))
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
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/<module>)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
