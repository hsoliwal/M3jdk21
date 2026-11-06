#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_HASH_FAMILY v3 (recipe-first port, never by hand).

Copies every FILE and MUTANT row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR
at --donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/indexdb),
rewriting only (1) the leading license header lines, replaced by the M3JDK header, (2) the package line, through the
PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE, TYPE and EXTERNAL rows (a static
import maps its owner type, then keeps its member), (4) the MEMBER rows (a qualified static member of a donor type
that is not ported, mapped onto its M3 owner as a whole-word token), (5) the TYPE rows whose donor and target names
differ, as whole-word tokens, a row with a scope applying to that target file only, (6) for a MUTANT row the SUBST
rows scoped to its target, each applied exactly once to the renamed line, and (7) one //FLLDCIM: anchor line
inserted before the top-level type declaration, naming the donor blob URL at the donor commit. Every other byte of
the file is kept (CRLF too). PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and
target sha256 of every file and states body_match (the bytes after the package line, minus the anchor, equal the
donor's), body_match_renamed (they equal the donor's after the MEMBER, TYPE and SUBST rows are applied to the donor
bytes: only names and the declared substitutions differ), the role (FILE, MUTANT or DONOR_DEP) and the substitution
count. DONOR_DEP rows pin donor files that are compiled only by differential.sh (never relocated). SKIP rows pin
donor files that are deliberately not relocated (role SKIP; the scope column carries the reason class), ORACLE rows
pin the donor's oracle scripts and evidence (role ORACLE), PROBE rows with scope donor pin donor files compiled only
for an NEC probe (role PROBE); NEC rows record a difference between a donor member and an M3 owner and are counted,
never applied (equivalence is never decided by the recipe). NAMING rows are exact text replacements scoped to one
FILE target, each applied exactly once after the renames: the rewrite m3/tooling/jdk-naming/jdk_naming.py would make
(the rule is named in the note), so that the recipe and the naming recipe share one fixed point; they count as
substitutions in the provenance.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package of the
map (port the closure: add the file or the dependency to the map), a file without a top-level type, a MEMBER owner
still named after the mapping (an unmapped member of an unported type), a relocated file that still names the type
of a SKIP row (a reference to a file that is not ported), a SUBST text not found exactly once, a MUTANT row without
a SUBST row, a SUBST row scoped to a FILE row, or an unknown row kind.

Derived from m3/indexdb/recipe/hamming-topk-20261106/relocate.py v2 (RELOCATE_HAMMING_TOPK, PR #2510, the module's
own lineage, itself derived from m3/arrays/recipe/csr-index-20261106/relocate.py v1): SKIP, ORACLE, PROBE and NEC
rows and the SKIP-type refusal added.

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
RECIPE_PATH = "m3/indexdb/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "role", "substitutions", "recipe")
KINDS = ("FILE", "MUTANT", "PACKAGE", "TYPE", "MEMBER", "SUBST", "EXTERNAL", "DONOR_DEP", "SKIP", "ORACLE", "PROBE",
         "NEC", "NAMING")
PINNED = ("DONOR_DEP", "SKIP", "ORACLE", "PROBE")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
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
        kind, donor, target, scope, *_note = line.split("\t")
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


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def rename_tokens(line: bytes, tokens: dict) -> bytes:
    for donor, target in tokens.items():
        if donor != target:
            line = re.sub(rb"\b" + re.escape(donor.encode()) + rb"\b", target.encode(), line)
    return line


def substitute(line: bytes, substitutions: list, counts: list) -> bytes:
    for position, (before, after) in enumerate(substitutions):
        hits = line.count(before.encode())
        if hits:
            counts[position] += hits
            line = line.replace(before.encode(), after.encode())
    return line


def map_import(fqn: str, static: bool, mapping: dict, tokens: dict, donor_path: str) -> str:
    if static:
        owner_type, _dot, member = fqn.rpartition(".")
        return map_import(owner_type, False, mapping, tokens, donor_path) + "." + member
    owner, _dot, simple = fqn.rpartition(".")
    if owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + tokens.get(simple, simple)
    if fqn.startswith(JDK_PREFIXES) or owner in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(donor_path: str, blob: str, commit: str, role: str) -> bytes:
    note = "; MUTANT: negative control of the oracle, SUBST rows of the map applied" if role == "MUTANT" else ""
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s%s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION, note)).encode()


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, target: str,
             role: str) -> tuple[bytes, bool, bool, int]:
    """The relocated bytes, body_match (bytes after the package line, minus the anchor, equal the donor's),
    body_match_renamed (they equal the donor's bytes with the MEMBER, TYPE and SUBST rows applied: only names and
    the declared substitutions differ) and the number of substitutions applied."""
    tokens = tokens_for(mapping, target)
    substitutions = [(before, after) for before, after, scope in mapping["SUBST"] if scope == target]
    if role == "MUTANT" and not substitutions:
        raise Refused("MUTANT row %s without a SUBST row" % target)
    if role == "FILE" and substitutions:
        raise Refused("SUBST row scoped to the FILE row %s (only a MUTANT row takes substitutions)" % target)
    substitutions += [(before, after) for before, after, scope in mapping["NAMING"] if scope == target]
    counts = [0] * len(substitutions)
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
                                                  mapping, tokens, donor_path).encode() + imported.group(3) + eol
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit, role) + eol)
            anchored = True
        line = substitute(rename_tokens(line, tokens), substitutions, counts)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    for position, hits in enumerate(counts):
        if hits != 1:
            raise Refused("SUBST text found %d times, not once, in %s: %s" % (hits, target, substitutions[position][0]))
    donor_bytes, target_bytes = b"".join(donor_body), b"".join(target_body)
    for owner in sorted({donor.split(".")[0] for donor, _name, _scope in mapping["MEMBER"]}):
        if re.search(rb"\b" + re.escape(owner.encode()) + rb"\b", target_bytes):
            raise Refused("unmapped member of the unported type %s in %s (add a MEMBER row)" % (owner, target))
    for skipped in sorted({Path(donor).stem for donor, _name, _scope in mapping["SKIP"] if donor.endswith(".java")}):
        if re.search(rb"" + re.escape(skipped.encode()) + rb"", target_bytes):
            raise Refused("relocated file %s still names the SKIP type %s (map it by a TYPE or MEMBER row)"
                          % (target, skipped))
    renamed = b"".join(substitute(rename_tokens(line, tokens), substitutions, [0] * len(substitutions))
                       for line in donor_body)
    return b"".join(out), donor_bytes == target_bytes, renamed == target_bytes, sum(counts)


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
    for role in ("FILE", "MUTANT"):
        for donor_path, target, _scope in mapping[role]:
            blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
            data = git(donor, "cat-file", "blob", blob)
            relocated, body_match, body_match_renamed, applied = relocate(data, donor_path, blob, commit, mapping,
                                                                          target, role)
            state = write_if_changed(module / target, relocated, check)
            changed += state
            print("%s %s body_match=%s body_match_renamed=%s substitutions=%d %s" % (
                role, "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(),
                applied, target))
            rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                         str(body_match).upper(), str(body_match_renamed).upper(), role, str(applied),
                         RECIPE + " " + VERSION))
    for role in PINNED:
        for donor_path, _target, scope in mapping[role]:
            if role == "PROBE" and scope != "donor":
                continue
            blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
            data = git(donor, "cat-file", "blob", blob)
            print("%s blob=%s %s (pinned, not relocated%s)" % (role, blob[:12], donor_path,
                                                              ": " + scope if role == "SKIP" else ""))
            rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), "-", "-", "-", "-", role, "0",
                         RECIPE + " " + VERSION))
    order = ("FILE", "MUTANT") + PINNED
    rows.sort(key=lambda row: (order.index(row[9]), row[5], row[2]))
    print("NEC rows=%d (recorded differences, never applied)" % len(mapping["NEC"]))
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/indexdb)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
