#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_CANONICAL_LITERALS v5 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV, which must resolve to the commit of the REPO row) into the module (--module DIR, default: the
grandparent of this recipe directory, make/m3/intern).

Java files only. Rewrites only (1) the leading license header (a block comment holding an SPDX line, or // header
lines), replaced by the M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped
packages and types, through the PACKAGE and TYPE rows (an import of a nested member maps its longest mapped package
prefix and the type that follows it), (4) the TYPE rows whose donor and target names differ, as whole-word tokens,
(5) the LITERAL rows, declared string-literal tokens renamed as whole tokens, (6) the DROP rows: a declared donor line
(pinned by the sha256 of its bytes) removed from a named file, refused unless it occurs exactly once, (7) the CTOR
rows: an explicit public no-argument constructor (one javadoc line and the declaration, indented one level deeper)
inserted after the declaration line of the named class, refused unless that declaration occurs exactly once and the
class declares no constructor, (8) the REWRITE rows: a declared donor text replaced by a declared target text in a
named file (the dependency adapter), refused unless the donor text occurs exactly the declared number of times, and
(9) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob URL at the donor
commit. Every other byte of the file is kept (CRLF too).

REWRITE rows: kind, donor path, donor text, target text, occurrences, reason. Both texts are TSV-escaped (\\n is the
file's end of line, \\t a tab, \\\\ a backslash); they apply in map order to the file after the DROP and CTOR rows and
before the line mapping, so the donor text is written in donor names and the target text in target names (the TYPE
rows still run over it and are idempotent on target names).

PIN rows name a token that must occur in the relocated file exactly as often as in the donor file (wire-format ids,
test data): a later map row or naming rule that touches them makes the recipe refuse. OMIT / NEC rows are
informational: the recipe pins their donor blob in its run log (HELD lines) and copies nothing. The REPO row names the
donor repository and the pinned relocation commit; the CLOSURE row names the commit whose tree compiles the donor with
its own dependencies (differential.sh): the recipe refuses when a FILE row's blob there differs from the REPO commit's.
CLOSURE_PATH rows (source roots of the CLOSURE tree, jars of the local Maven repository) are read by differential.sh.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row
and states body_match (the bytes after the package line, minus the anchor, equal the donor's), body_match_renamed
(they equal the donor's after the TYPE, LITERAL, import, DROP, CTOR and REWRITE rows are applied to the donor bytes:
only the declared names and texts differ), declared_drops, declared_ctors and declared_rewrites.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package (port
the closure: add the file or the dependency to the map), a Java file without a top-level type, a DROP row whose line
is absent or ambiguous, a LITERAL row whose donor token is absent from every FILE row, a CTOR row whose class is
absent, ambiguous or already declares a constructor, a REWRITE row whose occurrence count differs, a PIN row whose
count differs (or is zero), a --donor-rev other than the REPO commit, or a FILE blob that differs at the CLOSURE commit.

Derived from make/m3/intern/recipe/intern-space-20261106/relocate.py v4 (RELOCATE_INTERN_SPACE, PR #2549), itself derived
from m3/arrays/recipe/bitmatrix-20261106/relocate.py v3 (RELOCATE_BIT_MATRIX, PR #2515): the REPO, CLOSURE,
CLOSURE_PATH and REWRITE rows and the declared_rewrites provenance column added (the donor repository is read from the
REPO row, no longer a constant); everything else unchanged.

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
VERSION = "v5"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "make/m3/intern/recipe/" + RECIPE_DIR.name + "/" + RECIPE
GITHUB = "https://github.com/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_drops", "declared_ctors",
                      "declared_rewrites", "recipe")
ESCAPES = re.compile(r"\\(.)")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
BLOCK_START = re.compile(rb"^/\*")
BLOCK_END = re.compile(rb"\*/\s*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")
CTOR_DOC = b"/** Explicit constructor: the exported module package lint (missing-explicit-ctor) refuses the default one. */"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "DROP": [], "LITERAL": {}, "CTOR": [],
                     "PIN": [], "HELD": [], "REWRITE": [], "REPO": {}, "CLOSURE": {}, "CLOSURE_PATH": []}
    for kind, donor, target, *note in rows:
        if kind in ("FILE", "DROP", "CTOR", "PIN", "CLOSURE_PATH"):
            mapping[kind].append((donor, target))
        elif kind == "REWRITE":
            if len(note) < 2 or not note[1].isdigit():
                raise Refused("REWRITE row of %s needs: donor text, target text, occurrences, reason" % donor)
            mapping[kind].append((donor, unescape(target), unescape(note[0]), int(note[1])))
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, note[0] if note else ""))
        elif kind in mapping:
            mapping[kind][donor] = target
        else:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
    for single in ("REPO", "CLOSURE"):
        if len(mapping[single]) != 1:
            raise Refused("%s needs exactly one %s row" % (MAP_NAME, single))
    return mapping


def unescape(text: str) -> str:
    """TSV text escapes of the REWRITE rows: \\n end of line (the file's own at apply time), \\t tab, \\\\ backslash."""
    def one(match: re.Match) -> str:
        escaped = {"n": "\n", "t": "\t", "\\": "\\"}.get(match.group(1))
        if escaped is None:
            raise Refused("unknown escape \\%s in a REWRITE row" % match.group(1))
        return escaped
    return ESCAPES.sub(one, text)


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
    return ("//FLLDCIM: RELOCATE - donor %s%s/blob/%s/%s blob %s; recipe %s %s"
            % (GITHUB, repo, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def header_end(lines: list) -> int:
    """Index of the first line after the leading license header (a block comment holding SPDX, or // lines)."""
    if lines and BLOCK_START.match(lines[0]):
        for at, line in enumerate(lines):
            if BLOCK_END.search(split_eol(line)[0]):
                block = b"".join(lines[:at + 1])
                return at + 1 if b"SPDX-License-Identifier" in block else 0
        return 0
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    return index


def apply_drops(lines: list, donor_path: str, mapping: dict) -> tuple[list, int]:
    """Removes every declared line (sha256 of its bytes without the end of line); refuses unless each occurs once."""
    drops = {line_sha for path, line_sha in mapping["DROP"] if path == donor_path}
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


def apply_ctors(lines: list, donor_path: str, mapping: dict) -> tuple[list, int]:
    """Inserts one explicit public no-argument constructor after the declaration line of every CTOR class."""
    names = [name for path, name in mapping["CTOR"] if path == donor_path]
    for name in names:
        declaration = re.compile(rb"^(\s*)(?:(?:public|protected|private|static|final|abstract|strictfp)\s+)*class\s+"
                                 + name.encode() + rb"\b[^{]*\{\s*$")
        existing = re.compile(rb"^\s*(?:(?:public|protected|private)\s+)?" + name.encode() + rb"\s*\(")
        hits = [at for at, line in enumerate(lines) if declaration.match(split_eol(line)[0])]
        if len(hits) != 1:
            raise Refused("CTOR row %s of %s matched %d declarations, expected exactly 1" % (name, donor_path, len(hits)))
        if any(existing.match(split_eol(line)[0]) for line in lines):
            raise Refused("CTOR row %s of %s: the class already declares a constructor" % (name, donor_path))
        text, eol = split_eol(lines[hits[0]])
        indent = declaration.match(text).group(1) + b"    "
        lines = lines[:hits[0] + 1] + [indent + CTOR_DOC + eol, indent + b"public " + name.encode() + b"() { }" + eol] \
            + lines[hits[0] + 1:]
    return lines, len(names)


def apply_rewrites(lines: list, donor_path: str, mapping: dict, eol: bytes, seen: dict) -> tuple[list, int]:
    """Replaces each declared donor text by its target text, in map order; refuses an undeclared occurrence count."""
    rows = [(old, new, count) for path, old, new, count in mapping["REWRITE"] if path == donor_path]
    if not rows:
        return lines, 0
    text = b"".join(lines)
    for old, new, count in rows:
        old_bytes, new_bytes = (part.encode("utf-8").replace(b"\n", eol) for part in (old, new))
        found = text.count(old_bytes)
        if not old_bytes or found != count:
            raise Refused("REWRITE row %r of %s matched %d times, declared %d" % (old, donor_path, found, count))
        text = text.replace(old_bytes, new_bytes)
        seen[(donor_path, old)] = found
    return LINES.findall(text), len(rows)


def mapped_line(line: bytes, mapping: dict, donor_path: str, seen: dict) -> bytes:
    text, eol = split_eol(line)
    imported = IMPORT_LINE.match(text)
    if imported:
        line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
            + imported.group(3) + eol
    return rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen)


def check_pins(donor: bytes, target: bytes, donor_path: str, mapping: dict) -> list:
    pins = []
    for path, token in mapping["PIN"]:
        if path != donor_path:
            continue
        before, after = donor.count(token.encode()), target.count(token.encode())
        if before == 0 or before != after:
            raise Refused("PIN row %r of %s: donor count %d, target count %d" % (token, donor_path, before, after))
        pins.append((token, after))
    return pins


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                  seen: dict) -> tuple[bytes, bool, bool, int, int, int]:
    lines = LINES.findall(data)
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    index = header_end(lines)
    out = [text + first_eol for text in HEADER]
    body_lines, dropped = apply_drops(lines[index:], donor_path, mapping)
    body_lines, ctors = apply_ctors(body_lines, donor_path, mapping)
    body_lines, rewrites = apply_rewrites(body_lines, donor_path, mapping, first_eol or b"\n", seen)
    repo = next(iter(mapping["REPO"]))
    donor_body: list = []
    target_body: list = []
    anchored = False
    in_body = False
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
        if not anchored and TYPE_LINE.match(text):
            out.append(anchor(repo, donor_path, blob, commit) + eol)
            anchored = True
        line = mapped_line(line, mapping, donor_path, seen)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    renamed = b"".join(mapped_line(line, mapping, donor_path, {}) for line in donor_body)
    target_bytes = b"".join(target_body)
    # body_match compares the donor bytes after its package line before any DROP / CTOR / REWRITE row (a row makes it
    # FALSE).
    after_package = next((at for at, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    return (b"".join(out), b"".join(lines[after_package + 1:]) == target_bytes, renamed == target_bytes, dropped, ctors,
            rewrites)


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
    (repo, pinned_commit), = mapping["REPO"].items()
    (_closure_repo, closure_rev), = mapping["CLOSURE"].items()
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    if commit != pinned_commit:
        raise Refused("--donor-rev %s resolves to %s, the REPO row pins %s" % (rev, commit, pinned_commit))
    closure = git(donor, "rev-parse", closure_rev + "^{commit}").decode().strip()
    rows, changed, seen, pinned, closure_equal = [], 0, {}, 0, 0
    for donor_path, target in mapping["FILE"]:
        if not donor_path.endswith(".java"):
            raise Refused("FILE row %s is not a Java file (this recipe has no native lane)" % donor_path)
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        closure_blob = git(donor, "rev-parse", closure + ":" + donor_path).decode().strip()
        if closure_blob != blob:
            raise Refused("FILE row %s: blob %s at the CLOSURE commit, %s at the REPO commit" % (
                donor_path, closure_blob[:12], blob[:12]))
        closure_equal += 1
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed, dropped, ctors, rewrites = relocate_java(
            data, donor_path, blob, commit, mapping, seen)
        for token, count in check_pins(data, relocated, donor_path, mapping):
            print("PIN %r count=%d %s" % (token, count, target))
            pinned += 1
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_drops=%d declared_ctors=%d declared_rewrites=%d %s"
              % ("CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(),
                 dropped, ctors, rewrites, target))
        rows.append((repo, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), str(ctors),
                     str(rewrites), RECIPE + " " + VERSION))
    for path, old, _new, count in mapping["REWRITE"]:
        if (path, old) not in seen:
            raise Refused("REWRITE row %r names no FILE row (%s)" % (old, path))
        print("REWRITE %s occurrences=%d %r" % (path.rsplit("/", 1)[-1], count, old[:60]))
    print("CLOSURE commit=%s files=%d blobs=EQUAL" % (closure, closure_equal))
    for donor_literal in mapping["LITERAL"]:
        count = seen.get(donor_literal, 0)
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    if pinned != len(mapping["PIN"]):
        raise Refused("%d PIN rows name no FILE row" % (len(mapping["PIN"]) - pinned))
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s" % (kind, donor_path, blob[:12], target))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d held=%d pins=%d rewrites=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), len(mapping["HELD"]), pinned, len(mapping["REWRITE"]), changed,
        "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (pin the commit sha, not FETCH_HEAD)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default make/m3/intern)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
