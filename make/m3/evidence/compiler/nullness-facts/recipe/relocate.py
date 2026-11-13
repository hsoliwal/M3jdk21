#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_NULLNESS_FACTS_PLUGIN v1 (recipe-first port, never by hand).

Copies every FILE, FIXTURE and SERVICE row of RELOCATION_MAP.tsv from a donor git tree (--donor-git DIR at
--donor-rev REV: the lineage commit at which every donor file sits at its live path with its sealed blob) into this
module. A FILE row (Java source) is rewritten only in (1) the leading license header lines, replaced by the M3JDK
header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped packages and types, through the
PACKAGE and TYPE rows, (4) the TYPE rows whose donor and target names differ, (5) whole comment lines, through the
PROSE rows (the product-name rules of make/m3/tooling/jdk-naming, so that jdk_naming.py is a fixed point over the output),
(6) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob URL at the donor
commit, and (7) CRLF line ends, normalized to LF (the repository stores LF; its .gitattributes is "* -text"). A
FIXTURE row gets (7) only; a SERVICE row (java.util.ServiceLoader descriptor) maps each line as a fully qualified type
name through the PACKAGE and TYPE rows, with (7). Every other byte of every file is kept.

PROVENANCE.tsv pins, per file: donor commit and blob, donor sha256, the blob of the same path at --develop-rev (ABSENT
when the develop tree no longer carries the file), the retained-variant path and blob of a RETAINED row (the custody
copy of the donor's pull request, read at --develop-rev), the target sha256, the eol normalization, every rewrite that
touched the body with its line count, and body_match: TRUE only when the bytes after the package line, minus the
anchor, equal the donor's LF-normalized bytes byte for byte (a TYPE rename or a PROSE rewrite makes it FALSE and the
rewrites column says which lines).

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package (java., javax., jdk., com.sun.), nor a mapped donor package, nor a
declared EXTERNAL package of the map, or a FILE without a top-level type.

usage: python -I relocate.py --donor-git DIR --donor-rev REV --develop-rev REV [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v1"
MODULE_PATH = "m3/compiler/nullness-facts"
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "develop_commit",
                      "develop_blob", "retained_path", "retained_blob", "target_path", "target_sha256", "eol",
                      "rewrites", "body_match", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
# a whole comment line: PROSE rows apply only here, so string literals and code keep their exact text
COMMENT_LINE = re.compile(rb"^\s*(?://|/\*|\*|#|<!--)")
JDK_PREFIXES = ("java.", "javax.", "jdk.", "com.sun.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
ABSENT = "ABSENT"
NONE = "-"


class Refused(Exception):
    pass


def read_map(module: Path) -> dict:
    rows = [line.split("\t") for line in (module / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "FIXTURE": [], "SERVICE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {},
                     "PROSE": [], "RETAINED": {}}
    for kind, donor, target, *note in rows:
        if kind in ("FILE", "FIXTURE", "SERVICE"):
            mapping[kind].append((donor, target))
        elif kind == "PROSE":
            name = (note[0].split(":")[0].strip() if note else "PROSE")
            mapping["PROSE"].append((name, re.compile(donor.encode()), target.encode()))
        elif kind in ("PACKAGE", "TYPE", "EXTERNAL", "RETAINED"):
            mapping[kind][donor] = target
        else:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def blob_of(donor: Path, rev: str, path: str) -> str:
    """The blob sha of rev:path, or ABSENT when the tree at rev has no such path."""
    result = subprocess.run(["git", "-C", str(donor), "rev-parse", "--verify", "-q", rev + ":" + path],
                            capture_output=True)
    return result.stdout.decode().strip() if result.returncode == 0 else ABSENT


def normalize_eol(data: bytes) -> tuple[bytes, str]:
    if b"\r\n" in data:
        return data.replace(b"\r\n", b"\n"), "CRLF->LF"
    return data, "LF"


def split_eol(line: bytes) -> tuple[bytes, bytes]:
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def rename_types(line: bytes, types: dict, hits: Counter) -> bytes:
    for donor, target in types.items():
        if donor != target:
            line, count = re.subn(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
            if count:
                hits["TYPE %s->%s" % (donor, target)] += 1
    return line


def prose(line: bytes, rules: list, hits: Counter) -> bytes:
    if not COMMENT_LINE.match(line):
        return line
    for name, pattern, replacement in rules:
        line, count = pattern.subn(replacement, line)
        if count:
            hits["PROSE " + name] += 1
    return line


def map_fqn(fqn: str, mapping: dict, donor_path: str) -> str:
    owner, _dot, simple = fqn.rpartition(".")
    if owner in mapping["PACKAGE"]:
        return mapping["PACKAGE"][owner] + "." + mapping["TYPE"].get(simple, simple)
    if fqn.startswith(JDK_PREFIXES) or owner in mapping["EXTERNAL"]:
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(donor_path: str, blob: str, commit: str) -> bytes:
    return ("//FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s/recipe/%s %s"
            % (DONOR_BLOB_URL, commit, donor_path, blob[:12], MODULE_PATH, RECIPE, VERSION)).encode()


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, str, Counter, bool]:
    """The relocated bytes, the eol state, the rewrites that touched the body, and body_match."""
    data, eol = normalize_eol(data)
    lines = LINES.findall(data)
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    out = [text + b"\n" for text in HEADER]
    donor_body: list[bytes] = []
    target_body: list[bytes] = []
    hits: Counter = Counter()
    anchored = False
    in_body = False
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
            mapped = map_fqn(imported.group(2).decode(), mapping, donor_path)
            if mapped != imported.group(2).decode():
                hits["IMPORT"] += 1
            line = imported.group(1) + mapped.encode() + imported.group(3) + line_eol
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit) + line_eol)
            anchored = True
        line = prose(rename_types(line, mapping["TYPE"], hits), mapping["PROSE"], hits)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    return b"".join(out), eol, hits, b"".join(donor_body) == b"".join(target_body)


def relocate_service(data: bytes, donor_path: str, mapping: dict) -> tuple[bytes, str, Counter, bool]:
    data, eol = normalize_eol(data)
    out: list[bytes] = []
    hits: Counter = Counter()
    for line in LINES.findall(data):
        text, line_eol = split_eol(line)
        stripped = text.strip()
        if stripped and not stripped.startswith(b"#"):
            mapped = map_fqn(stripped.decode(), mapping, donor_path).encode()
            if mapped != stripped:
                hits["FQN"] += 1
            text = mapped
        out.append(text + line_eol)
    result = b"".join(out)
    return result, eol, hits, result == data


def relocate_fixture(data: bytes) -> tuple[bytes, str, Counter, bool]:
    normalized, eol = normalize_eol(data)
    return normalized, eol, Counter(), True


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def describe(hits: Counter) -> str:
    return "; ".join("%s x%d" % (name, count) for name, count in sorted(hits.items())) if hits else NONE


def run(module: Path, donor: Path, rev: str, develop_rev: str, check: bool) -> int:
    mapping = read_map(module)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    develop = git(donor, "rev-parse", develop_rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    work = [("FILE", pair) for pair in mapping["FILE"]] + [("FIXTURE", pair) for pair in mapping["FIXTURE"]] \
        + [("SERVICE", pair) for pair in mapping["SERVICE"]]
    for kind, (donor_path, target) in work:
        blob = blob_of(donor, rev, donor_path)
        if blob == ABSENT:
            raise Refused("%s is absent from the donor tree at %s" % (donor_path, commit))
        data = git(donor, "cat-file", "blob", blob)
        if kind == "FILE":
            relocated, eol, hits, body_match = relocate(data, donor_path, blob, commit, mapping)
        elif kind == "SERVICE":
            relocated, eol, hits, body_match = relocate_service(data, donor_path, mapping)
        else:
            relocated, eol, hits, body_match = relocate_fixture(data)
        retained_path = mapping["RETAINED"].get(donor_path, NONE)
        retained_blob = blob_of(donor, develop_rev, retained_path) if retained_path != NONE else NONE
        if retained_blob == ABSENT:
            raise Refused("retained variant %s is absent from %s" % (retained_path, develop))
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("%s %s body_match=%s eol=%s rewrites=[%s] %s" % (
            kind, "CHANGED" if state else "UNCHANGED", str(body_match).upper(), eol, describe(hits), target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), develop, blob_of(donor, develop_rev, donor_path),
                     retained_path, retained_blob, target, sha256(relocated), eol, describe(hits),
                     str(body_match).upper(), RECIPE + " " + VERSION))
    rows.sort(key=lambda row: row[9])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(module / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s develop_commit=%s files=%d changed=%d provenance=%s" % (
        RECIPE, VERSION, commit, develop, len(rows), changed, "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor lineage revision (the commit the blobs are read at)")
    parser.add_argument("--develop-rev", required=True,
                        help="donor develop revision (live blob state and retained variants are read at it)")
    parser.add_argument("--module", default=str(Path(__file__).resolve().parent.parent))
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), Path(args.donor_git), args.donor_rev, args.develop_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
