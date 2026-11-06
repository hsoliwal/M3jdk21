#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_CHUNK_DELTA v3 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/file),
rewriting only (1) the leading license header (the donor's `/* ... */` Apache block or its `// SPDX-...` lines),
replaced by the M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped packages
and types, through the PACKAGE, TYPE and EXTERNAL rows (a static import maps its owner type, then keeps its member),
(4) the DROP rows scoped to the target: an exact line removed, exactly once (the donor's unused lombok logger
annotation and import, and the import of a donor type that maps onto a JDK type), (5) the IMPORT rows scoped to the
target: one import line added in sorted position among the existing imports (a JDK type the mapped names need),
(6) the MEMBER rows (a qualified static member of an unported donor type, mapped onto its M3 owner as a whole-word
token), (7) the TYPE rows whose donor and target names differ, as whole-word tokens; the target may be a JDK type
(String) or a parameterized JDK type (Map<String, byte[]>); a row with a scope applies to that target file only,
(8) the SUBST rows scoped to the target, each exact text applied exactly once to the renamed line (the display sites
that call the donor hash type's toShortHex(), and the test's hash and store constructors), (9) the AFTER rows scoped
to the target: lines inserted once after the exact renamed line they name (the explicit public constructor the
module's -Xlint:all missing-explicit-ctor lint requires of a class in an exported package; `\\n` separates lines),
and (10) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob URL at the
donor commit. Every other byte of the body is kept. Line endings are written as LF, the form the repository stores (core.autocrlf
normalizes the CRLF donors on commit), and --check compares LF-normalized bytes, so a CRLF checkout is a fixed point.

COPY rows copy a donor file verbatim (LF-normalized, no rewriting) into the module: the third-party NOTICE and
LICENSE the donor carries and the donor's own README and NOTICE. DONOR_DEP rows pin donor files that are compiled
only by differential.sh (never relocated).

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 (raw donor bytes) and target sha256
(LF bytes) of every file and states body_match (the bytes after the package line, minus the anchor, equal the donor's
LF-normalized body), body_match_renamed (they equal the donor body after the DROP, IMPORT, AFTER, MEMBER, TYPE and
SUBST rows are applied: only the declared names, drops, imports, insertions and substitutions differ), the role (FILE,
COPY or DONOR_DEP), the counts of substitutions, drops, imports and inserts, and changed_lines (the lines of the donor file and of the target
file that the other one does not contain, by difflib, with the header, package, anchor and import lines included:
the complete size of the edit).

Idempotent: writes a file only when its LF bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a file without a top-level type, a MEMBER owner
still named after the mapping, a DROP, SUBST or AFTER text not found exactly once, an IMPORT already present or
without an import block to join, or an unknown row kind.

Derived from m3/indexdb/recipe/hamming-topk-20261106/relocate.py v2 (RELOCATE_HAMMING_TOPK): the `/* ... */` license
block header, DROP rows, IMPORT rows, AFTER rows, SUBST rows on FILE targets (v2 allowed them on MUTANT rows only),
COPY rows, LF normalization with LF-aware --check, and the drops, imports, inserts and changed_lines provenance
columns added; MUTANT rows are not needed by this leaf and were not carried.

usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import difflib
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v3"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "m3/file/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "role", "substitutions", "drops",
                      "imports", "inserts", "changed_lines", "recipe")
KINDS = ("FILE", "COPY", "PACKAGE", "TYPE", "MEMBER", "SUBST", "DROP", "IMPORT", "AFTER", "EXTERNAL", "DONOR_DEP")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
BLOCK_START = re.compile(rb"^/\*(?!\*)")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.*]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
JDK_PREFIXES = ("java.", "javax.", "jdk.", "com.sun.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    """Rows by kind as (donor, target, scope) triples; PACKAGE and EXTERNAL as {donor: target}. The header row
    kind<TAB>donor<TAB>target<TAB>scope<TAB>note and # comment lines are skipped."""
    mapping: dict = {kind: [] for kind in KINDS}
    for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines():
        if not line.strip() or line.startswith("#") or line.startswith("kind\t"):
            continue
        fields = line.split("\t")
        if len(fields) < 3:
            raise Refused("row with fewer than three columns in %s: %s" % (MAP_NAME, line))
        # A trailing empty scope or note column may be trimmed by an editor: pad, never guess.
        kind, donor, target, scope = (fields + ["", ""])[:4]
        if kind not in KINDS:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
        mapping[kind].append((donor, target, scope))
    mapping["PACKAGE"] = {donor: target for donor, target, _scope in mapping["PACKAGE"]}
    mapping["EXTERNAL"] = {donor: target for donor, target, _scope in mapping["EXTERNAL"]}
    return mapping


def tokens_for(mapping: dict, target: str) -> dict:
    """MEMBER rows first (a qualified member names its unported owner type), then the unscoped TYPE rows, then the
    TYPE rows scoped to this target file, which override."""
    tokens = {donor: name for donor, name, _scope in mapping["MEMBER"]}
    tokens.update({donor: name for donor, name, scope in mapping["TYPE"] if not scope})
    tokens.update({donor: name for donor, name, scope in mapping["TYPE"] if scope == target})
    return tokens


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def normalize(data: bytes) -> bytes:
    return data.replace(b"\r\n", b"\n")


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def rename_tokens(line: bytes, tokens: dict) -> bytes:
    for donor, target in tokens.items():
        if donor != target:
            line = re.sub(rb"\b" + re.escape(donor.encode()) + rb"\b", lambda _match: target.encode(), line)
    return line


def substitute(line: bytes, substitutions: list, counts: list) -> bytes:
    for position, (before, after) in enumerate(substitutions):
        hits = line.count(before.encode())
        if hits:
            counts[position] += hits
            line = line.replace(before.encode(), after.encode())
    return line


def dropped(text: bytes, drops: list, counts: list) -> bool:
    for position, drop in enumerate(drops):
        if text == drop.encode():
            counts[position] += 1
            return True
    return False


def inserted_after(line: bytes, afters: list, counts: list) -> list:
    """The line, followed by the AFTER rows' lines when the line's text names one (`\\n` separates lines)."""
    text, eol = split_eol(line)
    out = [line]
    for position, (key, insertion) in enumerate(afters):
        if text == key.encode():
            counts[position] += 1
            out.extend(part.encode() + (eol or b"\n") for part in insertion.split("\\n"))
    return out


def map_import(fqn: str, static: bool, mapping: dict, tokens: dict, donor_path: str) -> str:
    if static:
        owner_type, _dot, member = fqn.rpartition(".")
        return map_import(owner_type, False, mapping, tokens, donor_path) + "." + member
    owner, _dot, simple = fqn.rpartition(".")
    if owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + tokens.get(simple, simple)
    if fqn.startswith(JDK_PREFIXES) or owner in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file, a DROP row or an EXTERNAL "
                  "row to %s)" % (fqn, donor_path, MAP_NAME))


def add_imports(body: list, imports: list, target: str) -> list:
    """Insert each IMPORT row in sorted position among the existing non-static import lines of the body."""
    for fqn in imports:
        existing = []
        for position, line in enumerate(body):
            matched = IMPORT_LINE.match(split_eol(line)[0])
            if matched and b"static" not in matched.group(1):
                existing.append((position, matched.group(2)))
        if not existing:
            raise Refused("IMPORT %s: no import block to join in %s" % (fqn, target))
        if any(name == fqn.encode() for _position, name in existing):
            raise Refused("IMPORT %s is already present in %s" % (fqn, target))
        position = next((index for index, name in existing if name > fqn.encode()), existing[-1][0] + 1)
        body.insert(position, b"import " + fqn.encode() + b";\n")
    return body


def header_end(lines: list) -> int:
    """Index of the first line after the donor's license header: a leading `/* ... */` block that carries a copyright
    or license statement, then any `// SPDX-...` or `// Copyright` lines. A `// Modified` or third-party attribution
    line that follows is body and stays."""
    index = 0
    if lines and BLOCK_START.match(lines[0]):
        end = 0
        while end < len(lines) and b"*/" not in lines[end]:
            end += 1
        block = b"".join(lines[: end + 1])
        if end < len(lines) and (b"Copyright" in block or b"Licensed under" in block):
            index = end + 1
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    return index


def anchor(donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def transform(lines: list, tokens: dict, substitutions: list, drops: list, imports: list, afters: list,
              counts: list, drop_counts: list, after_counts: list, target: str) -> list:
    """The body transformation shared by the target and by the body_match_renamed check: drops, renames,
    substitutions and AFTER insertions line by line, then the import insertions."""
    out = []
    for line in lines:
        text, _eol = split_eol(line)
        if dropped(text, drops, drop_counts):
            continue
        out.extend(inserted_after(substitute(rename_tokens(line, tokens), substitutions, counts), afters,
                                  after_counts))
    return add_imports(out, imports, target)


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
             target: str) -> tuple[bytes, bool, bool, int, int, int, int, int]:
    """The relocated LF bytes, body_match, body_match_renamed, and the counts of substitutions, drops, imports,
    inserts and changed lines (donor file vs target file, both LF)."""
    data = normalize(data)
    tokens = tokens_for(mapping, target)
    substitutions = [(before, after) for before, after, scope in mapping["SUBST"] if scope == target]
    drops = [text for text, _target, scope in mapping["DROP"] if scope == target]
    imports = [fqn for fqn, _target, scope in mapping["IMPORT"] if scope == target]
    afters = [(key, insertion) for key, insertion, scope in mapping["AFTER"] if scope == target]
    lines = LINES.findall(data)
    index = header_end(lines)
    pre: list = []
    package_line = None
    donor_body: list = []
    for line in lines[index:]:
        text, eol = split_eol(line)
        package = None if package_line is not None else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            package_line = b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + eol
        elif package_line is None:
            pre.append(line)
        else:
            donor_body.append(line)
    if package_line is None:
        raise Refused("no package line in %s" % donor_path)
    counts = [0] * len(substitutions)
    drop_counts = [0] * len(drops)
    after_counts = [0] * len(afters)
    target_body: list = []
    anchored = False
    for line in donor_body:
        text, eol = split_eol(line)
        if dropped(text, drops, drop_counts):
            continue
        imported = IMPORT_LINE.match(text)
        if imported:
            line = imported.group(1) + map_import(imported.group(2).decode(), b"static" in imported.group(1),
                                                  mapping, tokens, donor_path).encode() + imported.group(3) + eol
        elif not anchored and TYPE_LINE.match(text):
            target_body.append(anchor(donor_path, blob, commit) + eol)
            anchored = True
        target_body.extend(inserted_after(substitute(rename_tokens(line, tokens), substitutions, counts), afters,
                                          after_counts))
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    target_body = add_imports(target_body, imports, target)
    for position, hits in enumerate(counts):
        if hits != 1:
            raise Refused("SUBST text found %d times, not once, in %s: %s" % (hits, target, substitutions[position][0]))
    for position, hits in enumerate(drop_counts):
        if hits != 1:
            raise Refused("DROP text found %d times, not once, in %s: %s" % (hits, target, drops[position]))
    for position, hits in enumerate(after_counts):
        if hits != 1:
            raise Refused("AFTER line found %d times, not once, in %s: %s" % (hits, target, afters[position][0]))
    for owner in sorted({donor.split(".")[0] for donor, _name, _scope in mapping["MEMBER"]}):
        if re.search(rb"\b" + re.escape(owner.encode()) + rb"\b", b"".join(target_body)):
            raise Refused("unmapped member of the unported type %s in %s (add a MEMBER row)" % (owner, target))
    without_anchor = [line for line in target_body if not line.startswith(b"//FLLDCIM: RELOCATE")]
    renamed = transform(donor_body, tokens, substitutions, drops, imports, afters, [0] * len(substitutions),
                        [0] * len(drops), [0] * len(afters), target)
    relocated = b"".join([text + b"\n" for text in HEADER] + [rename_tokens(line, tokens) for line in pre]
                         + [package_line] + target_body)
    changed_lines = sum(1 for line in difflib.unified_diff(data.decode("utf-8").splitlines(),
                                                           relocated.decode("utf-8").splitlines(), n=0, lineterm="")
                        if (line.startswith("+") or line.startswith("-"))
                        and not line.startswith("+++") and not line.startswith("---"))
    return (relocated, b"".join(donor_body) == b"".join(without_anchor), b"".join(renamed) == b"".join(without_anchor),
            sum(counts), sum(drop_counts), len(imports), sum(after_counts), changed_lines)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and normalize(path.read_bytes()) == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target, _scope in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, body_match_renamed, applied, drops, imports, inserts, changed_lines = relocate(
            data, donor_path, blob, commit, mapping, target)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s substitutions=%d drops=%d imports=%d inserts=%d "
              "changed_lines=%d %s" % ("CHANGED" if state else "UNCHANGED", str(body_match).upper(),
                                       str(body_match_renamed).upper(), applied, drops, imports, inserts,
                                       changed_lines, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), "FILE", str(applied), str(drops),
                     str(imports), str(inserts), str(changed_lines), RECIPE + " " + VERSION))
    for donor_path, target, _scope in mapping["COPY"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        copied = normalize(data)
        state = write_if_changed(module / target, copied, check)
        changed += state
        print("COPY %s verbatim (LF) %s" % ("CHANGED" if state else "UNCHANGED", target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(copied), "TRUE", "TRUE",
                     "COPY", "0", "0", "0", "0", "0", RECIPE + " " + VERSION))
    for donor_path, _target, _scope in mapping["DONOR_DEP"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        print("DONOR_DEP blob=%s %s (compiled by differential.sh only, not relocated)" % (blob[:12], donor_path))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), "-", "-", "-", "-", "DONOR_DEP", "0", "0",
                     "0", "0", "0", RECIPE + " " + VERSION))
    rows.sort(key=lambda row: (("FILE", "COPY", "DONOR_DEP").index(row[9]), row[5], row[2]))
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/file)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
