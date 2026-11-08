#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_ALGORITHM_GRAPH_TRIE v1 (recipe-first port, never by hand).

Lineage: derived from recipe/families-20261106/relocate.py (RELOCATE_ALGORITHM_FAMILIES v1) byte for byte except RECIPE_DIR_REL,
the OWNED row kind and the INSERT_AFTER row kind (both below). The same recipe law: rewrite only header, package, mapped
imports, declared type names, declared drops and insertions and one anchor; every other byte is kept.

Copies every FILE row of RELOCATION_MAP.tsv from a donor git tree (--donor-git DIR at --donor-rev REV) into the
module m3/algorithms, rewriting only (1) the leading license header line(s), replaced by the M3JDK header, (2) the
package line, through the PACKAGE rows, (3) import lines of mapped packages and types, through the PACKAGE and TYPE
rows, (4) the TYPE rows (donor top-level type names to their M3 names, whole words, on every line, string literals
of the catalog included), (5) the DROP_METHOD and DROP_LINE rows (whole method declarations and single lines the plan
removes, each naming its owner or reason), (6) one //FLLDCIM: anchor line inserted before the top-level type
declaration, naming the donor blob and commit, and (7) the INSERT_AFTER rows (declared lines, carried verbatim in the
map, inserted after one unique anchor line of the donor type; here the explicit constructor the exported package's
missing-explicit-ctor lint demands). Every other byte is kept (line endings too). PROVENANCE.tsv pins the
donor commit, blob, sha256 and URL, the target sha256, the counts of renamed, mapped, dropped and inserted lines, and
body_match: read back from disk, every line of the target is a donor line, a donor line with the declared type
renames, the mapped package line, a mapped import line, or one of the explained insertions (header, anchor, INSERT_AFTER block), and
every missing donor line is a declared DROP row.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. --check writes nothing and
exits 1 when a run would change a file. Refuses (exit 2) an unmapped package, an import that is neither a JDK
package, nor a mapped donor package, nor a declared EXTERNAL package, a DROP row matching no line or more than one
declaration, or a file without a top-level type. The donor repository and its paths live only in RELOCATION_MAP.tsv.
A SUPERSEDED row is a FILE row a later recipe owns (named in its note): listed, never written. An OWNED row is a donor
source an earlier recipe already relocated (its owner is named in the note): listed, never written, compiled on the
donor side of differential.sh only, so the donor closure (AlgorithmSupport, the progress monitor) builds there.

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
RECIPE_DIR_REL = "m3/algorithms/recipe/graph-trie-20261106"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "donor_url",
                      "target_path", "target_sha256", "type_renames", "import_rewrites", "dropped_lines",
                      "inserted_lines", "body_match", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^(?:// |/\* )(?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b.*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+(?:\.\*)?)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
ANNOTATION_LINE = re.compile(rb"^\s*@\w")
ANCHOR_PREFIX = b"//FLLDCIM: RELOCATE"
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"REPO": {}, "FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "DROP_METHOD": {},
                     "DROP_LINE": {}, "DROP_DUMP": {}, "SKIP": [], "NEC": [], "SUPERSEDED": [], "OWNED": [],
                     "INSERT_AFTER": {}}
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind in ("SKIP", "NEC", "SUPERSEDED", "OWNED"):
            mapping[kind].append((donor, target))
        elif kind == "DROP_METHOD":
            owner, _dot, method = donor.rpartition(".")
            mapping[kind].setdefault(owner, []).append(method)
        elif kind in ("DROP_LINE", "DROP_DUMP"):
            mapping[kind].setdefault(donor, []).append(target)
        elif kind == "INSERT_AFTER":
            block = [text.encode() for text in "\t".join(_note).split("\\n")]
            mapping[kind].setdefault(donor, []).append((target, block))
        else:
            mapping[kind][donor] = target
    if len(mapping["REPO"]) != 1:
        raise Refused("exactly one REPO row is required in " + MAP_NAME)
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


def map_import(fqn: str, mapping: dict, donor_path: str) -> str:
    name = fqn[:-2] if fqn.endswith(".*") else fqn
    owner, _dot, simple = name.rpartition(".")
    if owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + mapping["TYPE"].get(simple, simple) + fqn[len(name):]
    if fqn.startswith(JDK_PREFIXES) or owner in mapping["EXTERNAL"] or name in mapping["EXTERNAL"] \
            or owner.rpartition(".")[0] in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(blob: str, commit: str) -> bytes:
    return ("%s - donor blob %s at commit %s (repository, path, URL and digests: %s/%s); recipe %s/%s %s"
            % (ANCHOR_PREFIX.decode(), blob[:12], commit[:12], RECIPE_DIR_REL, PROVENANCE_NAME, RECIPE_DIR_REL,
               RECIPE, VERSION)).encode()


def method_span(lines: list, method: str, donor_path: str) -> tuple[int, int]:
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
    while end < len(lines) and split_eol(lines[end])[0] != indent + b"}":
        end += 1
    if end == len(lines):
        raise Refused("DROP_METHOD %s: no closing brace at its indentation in %s" % (method, donor_path))
    stop = end + 1
    if stop < len(lines) and split_eol(lines[stop])[0].strip() == b"":
        stop += 1
    return first, stop


def strip_header(lines: list) -> list:
    index = 0
    while index < len(lines) and HEADER_LINE.match(split_eol(lines[index])[0]):
        index += 1
    return lines[index:]


def apply_drops(lines: list, donor_type: str, donor_path: str, mapping: dict) -> tuple[list, int]:
    dropped = 0
    for method in mapping["DROP_METHOD"].get(donor_type, ()):
        first, stop = method_span(lines, method, donor_path)
        dropped += stop - first
        lines = lines[:first] + lines[stop:]
    for regex in mapping["DROP_LINE"].get(donor_type, ()):
        pattern = re.compile(regex.encode())
        kept = [line for line in lines if not pattern.match(split_eol(line)[0])]
        if len(kept) == len(lines):
            raise Refused("DROP_LINE %s matches no line of %s" % (regex, donor_path))
        dropped += len(lines) - len(kept)
        lines = kept
    return lines, dropped


def insertions(lines: list, donor_type: str, donor_path: str, mapping: dict) -> dict:
    """{index of the anchor line: block} of the INSERT_AFTER rows of `donor_type`; each regex must match one line."""
    found = {}
    for regex, block in mapping["INSERT_AFTER"].get(donor_type, ()):
        pattern = re.compile(regex.encode())
        hits = [index for index, line in enumerate(lines) if pattern.match(split_eol(line)[0])]
        if len(hits) != 1:
            raise Refused("INSERT_AFTER %s: %d anchor lines in %s" % (regex, len(hits), donor_path))
        found[hits[0]] = block
    return found


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, dict]:
    """The relocated bytes and the counts of renamed, mapped, dropped and inserted lines."""
    lines = LINES.findall(data)
    eol = split_eol(lines[0])[1] if lines else b"\n"
    body, dropped = apply_drops(strip_header(lines), Path(donor_path).stem, donor_path, mapping)
    inserts = insertions(body, Path(donor_path).stem, donor_path, mapping)
    out = [text + eol for text in HEADER]
    renames = imports = inserted = 0
    anchored = package_seen = False
    for index, line in enumerate(body):
        text, line_eol = split_eol(line)
        block = inserts.get(index)
        if block is not None:
            out.append(rename_types(line, mapping["TYPE"]))
            out.extend(text + eol for text in block)
            inserted += len(block)
            continue
        package = None if package_seen else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            out.append(b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + line_eol)
            package_seen = True
            continue
        imported = IMPORT_LINE.match(text)
        if imported:
            mapped = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
                + imported.group(3) + line_eol
            imports += mapped != line
            out.append(mapped)
            continue
        if not anchored and TYPE_LINE.match(text):
            out.append(anchor(blob, commit) + line_eol)
            anchored = True
        renamed = rename_types(line, mapping["TYPE"])
        renames += renamed != line
        out.append(renamed)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    return b"".join(out), {"type_renames": renames, "import_rewrites": imports, "dropped_lines": dropped,
                           "inserted_lines": inserted}


def body_match(target: bytes, data: bytes, donor_path: str, mapping: dict) -> bool:
    """Read back from disk: every target line is explained by the donor line at its position and the map."""
    target_lines = LINES.findall(target)
    if [split_eol(line)[0] for line in target_lines[:len(HEADER)]] != list(HEADER):
        return False
    anchors = [line for line in target_lines if split_eol(line)[0].startswith(ANCHOR_PREFIX)]
    kept = [line for line in target_lines[len(HEADER):] if not split_eol(line)[0].startswith(ANCHOR_PREFIX)]
    try:
        donor, _dropped = apply_drops(strip_header(LINES.findall(data)), Path(donor_path).stem, donor_path, mapping)
        inserts = insertions(donor, Path(donor_path).stem, donor_path, mapping)
    except Refused:
        return False
    kept = strip_insertions(kept, inserts)
    if kept is None or len(anchors) != 1 or len(kept) != len(donor):
        return False
    package_seen = False
    for target_line, donor_line in zip(kept, donor):
        if target_line == donor_line or target_line == rename_types(donor_line, mapping["TYPE"]):
            continue
        text, eol = split_eol(donor_line)
        package = None if package_seen else PACKAGE_LINE.match(text)
        if package:
            package_seen = True
            if target_line == b"package " + mapping["PACKAGE"].get(package.group(1).decode(), "").encode() + b";" + eol:
                continue
        imported = IMPORT_LINE.match(text)
        try:
            if imported and target_line == imported.group(1) + map_import(imported.group(2).decode(), mapping,
                                                                         donor_path).encode() + imported.group(3) + eol:
                continue
        except Refused:
            pass
        return False
    return True


def strip_insertions(kept: list, inserts: dict) -> list | None:
    """The target lines without the declared INSERT_AFTER blocks, each found verbatim right after its anchor line
    (the anchor's donor position, shifted by the blocks inserted before it); None when a block is missing."""
    out, cursor, shift = [], 0, 0
    for index in sorted(inserts):
        block = inserts[index]
        stop = index + shift + 1
        if stop > len(kept) or [split_eol(line)[0] for line in kept[stop:stop + len(block)]] != block:
            return None
        out.extend(kept[cursor:stop])
        cursor = stop + len(block)
        shift += len(block)
    out.extend(kept[cursor:])
    return out


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
    mapping = read_map(RECIPE_DIR)
    (repo, url), = mapping["REPO"].items()
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, counts = relocate(data, donor_path, blob, commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        on_disk = (module / target).read_bytes() if (module / target).exists() else relocated
        match = body_match(on_disk, data, donor_path, mapping)
        print("FILE %s body_match=%s renames=%d imports=%d drops=%d inserts=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(match).upper(), counts["type_renames"],
            counts["import_rewrites"], counts["dropped_lines"], counts["inserted_lines"], target))
        rows.append((repo, commit, donor_path, blob, sha256(data), url + "/blob/" + commit + "/" + donor_path,
                     target, sha256(on_disk), str(counts["type_renames"]), str(counts["import_rewrites"]),
                     str(counts["dropped_lines"]), str(counts["inserted_lines"]), str(match).upper(),
                     RECIPE + " " + VERSION))
    rows.sort(key=lambda row: row[6])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(RECIPE_DIR / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d body_match=%d/%d skips=%d nec=%d owned=%d provenance=%s"
          % (RECIPE, VERSION, commit, len(rows), changed, sum(row[12] == "TRUE" for row in rows), len(rows),
             len(mapping["SKIP"]), len(mapping["NEC"]), len(mapping["OWNED"]), "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (m3/algorithms)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
