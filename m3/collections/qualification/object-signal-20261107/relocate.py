#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_OBJECT_SIGNAL v1 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/collections),
rewriting only (1) the leading license header line(s), replaced by the M3JDK header, (2) the package line, through the
PACKAGE rows, (3) import lines of mapped packages and types (static imports included), through the PACKAGE and TYPE
rows, (4) the TYPE rows (donor top-level type names to their M3 names, whole words, on every line), (5) the
DROP_METHOD, DROP_LINE and REWRITE rows (whole method declarations with their doc comment and single lines the plan
removes, and declared regular-expression substitutions inside the lines of one donor file, each naming its reason),
(6) the relocate-only rows INSERT_AFTER (declared lines inserted after one matched line), DROP_BLOCK (a declared run
of consecutive lines removed, pinned by its line count and the sha256 of its bytes) and ADAPT (a declared substitution
that adapts the donor line to an existing M3 owner, applied before the import mapping so it may replace an import),
and (7) one //FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob and commit.
Every other byte is kept (line endings too). PROVENANCE.tsv pins the donor commit, blob, sha256 and URL, the target
sha256, the counts of renamed, mapped, rewritten, adapted, dropped and inserted lines, and body_match: read back from
disk, every line of the target is a donor line, a donor line with the declared ADAPT and REWRITE rows and type renames
applied, the mapped package line, a mapped import line, or one of the explained insertions (header, anchor,
INSERT_AFTER rows), and every missing donor line is a declared DROP row.

--donor-subset DIR writes the donor-side inputs of the differential gate instead: every FILE row at its donor path
under DIR, with the donor header, no anchor, identity type names, the SUBSET_PACKAGE rows for the package line and the
both-mode rows (DROP_METHOD, DROP_LINE, REWRITE) only. The relocate-only rows (INSERT_AFTER, DROP_BLOCK, ADAPT) are not
applied there, so the donor side keeps the donor's own lines (the JNI seam, the codec parameter) and the differential
measures each adaptation against the donor. A main file without both-mode rows comes out byte-identical to its donor
blob (SUBSET_VERBATIM=TRUE, refused otherwise); a main file with declared both-mode rows (members whose closure is a
HELD or SKIP row) reports SUBSET_VERBATIM=FALSE and its row count.

A SUBSET_ONLY row names a donor closure file (an import of a relocated member whose M3 owner already exists, or whose
own leaf is HELD): --donor-subset writes it verbatim so the donor side compiles; it is never relocated. HELD, OMIT, SKIP
and NEC rows are informational: HELD and OMIT rows name one donor path each and the run pins its blob
(HELD lines of the log); SKIP and NEC rows are free text. Idempotent: writes a file only when its bytes differ; a
second run reports changed=0. --check writes nothing and exits 1 when a run would change a file. Refuses (exit 2) an
unmapped package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package,
a DROP, REWRITE, ADAPT or INSERT_AFTER row matching no line (a DROP_METHOD, DROP_BLOCK or INSERT_AFTER row matching
more than one), a DROP_BLOCK whose line count or sha256 differs from its pin, or a file without a top-level type. The
donor repository and its paths live only in RELOCATION_MAP.tsv and PROVENANCE.tsv (the anchor names blob and commit,
so no source line carries a donor repository name: m3/tooling/jdk-naming rewrites synexia-* tokens).

Derived from m3/collections/recipe/immutable-pools-20261106/relocate.py v2 (RELOCATE_IMMUTABLE_POOLS, same donor
repository and module) by adding the relocate-only DROP_BLOCK and ADAPT rows, the doc comment of a DROP_METHOD, the
non-verbatim main subset with declared rows, the HELD and OMIT rows and the adapted_lines provenance column; every v2
rule is unchanged.

usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check] [--donor-subset DIR]
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
RECIPE_DIR_REL = "m3/collections/qualification/object-signal-20261107"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "donor_url",
                      "target_path", "target_sha256", "type_renames", "import_rewrites", "rewritten_lines",
                      "adapted_lines", "dropped_lines", "inserted_lines", "body_match", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^(?:// |/\* )(?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b.*$")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(static\s+)?)([\w.]+(?:\.\*)?)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
ANNOTATION_LINE = re.compile(rb"^\s*@\w")
ANCHOR_PREFIX = b"//FLLDCIM: RELOCATE"
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
ROW_KINDS = ("REPO", "FILE", "PACKAGE", "SUBSET_PACKAGE", "TYPE", "EXTERNAL", "DROP_METHOD", "DROP_LINE", "REWRITE",
             "INSERT_AFTER", "DROP_BLOCK", "ADAPT", "SKIP", "NEC", "HELD", "OMIT", "SUPERSEDED", "SUBSET_ONLY")
INFO_KINDS = ("FILE", "SKIP", "NEC", "HELD", "OMIT", "SUPERSEDED", "SUBSET_ONLY")
PATTERN_KINDS = ("DROP_LINE", "REWRITE", "INSERT_AFTER", "DROP_BLOCK", "ADAPT")


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {kind: {} for kind in ROW_KINDS}
    mapping.update({kind: [] for kind in INFO_KINDS})
    for kind, donor, target, *_note in rows:
        if kind not in ROW_KINDS:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
        if kind in INFO_KINDS:
            mapping[kind].append((donor, target))
        elif kind == "DROP_METHOD":
            owner, _dot, method = donor.rpartition(".")
            mapping[kind].setdefault(owner, []).append(method)
        elif kind in PATTERN_KINDS:
            owner, _hash, pattern = donor.partition("#")
            if not pattern:
                raise Refused("%s row needs <DonorType>#<regex>: %s" % (kind, donor))
            mapping[kind].setdefault(owner, []).append((pattern, target))
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


def map_import(fqn: str, static: bool, mapping: dict, donor_path: str) -> str:
    name = fqn[:-2] if fqn.endswith(".*") else fqn
    owner, _dot, simple = name.rpartition(".")
    if static and owner.rpartition(".")[0] in mapping["PACKAGE"]:
        package, _dot, type_name = owner.rpartition(".")
        return mapping["PACKAGE"][package] + "." + mapping["TYPE"].get(type_name, type_name) + "." + simple \
            + fqn[len(name):]
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
    """[first, stop) of the declaration of `method`: its doc comment and annotations, the signature, the closing brace
    at the signature's indentation and one trailing blank line. The return type must start with a word character, so
    a call statement `method();` never counts as a declaration."""
    declaration = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final|synchronized|default|abstract)\s+)*"
                             rb"(?:<[^>]*>\s+)?[A-Za-z_][\w.<>\[\], ?]*\s+" + method.encode() + rb"\s*\(")
    starts = [index for index, line in enumerate(lines) if declaration.match(split_eol(line)[0])]
    if len(starts) != 1:
        raise Refused("DROP_METHOD %s: %d declarations in %s" % (method, len(starts), donor_path))
    start = starts[0]
    indent = declaration.match(lines[start]).group(1)
    first = start
    while first > 0 and ANNOTATION_LINE.match(lines[first - 1]) and lines[first - 1].startswith(indent):
        first -= 1
    if first > 0 and split_eol(lines[first - 1])[0].strip().endswith(b"*/"):
        opening = first - 1
        while opening >= 0 and not split_eol(lines[opening])[0].strip().startswith(b"/**"):
            opening -= 1
        if opening >= 0 and split_eol(lines[opening])[0].startswith(indent + b"/**"):
            first = opening
    end = start
    while end < len(lines) and split_eol(lines[end])[0] != indent + b"}":
        end += 1
    if end == len(lines):
        raise Refused("DROP_METHOD %s: no closing brace at its indentation in %s" % (method, donor_path))
    stop = end + 1
    if stop < len(lines) and split_eol(lines[stop])[0].strip() == b"":
        stop += 1
    return first, stop


def strip_header(lines: list) -> tuple[list, list]:
    index = 0
    while index < len(lines) and HEADER_LINE.match(split_eol(lines[index])[0]):
        index += 1
    return lines[:index], lines[index:]


def apply_drops(lines: list, donor_type: str, donor_path: str, mapping: dict) -> tuple[list, int]:
    """The both-mode drops: DROP_METHOD (with its doc comment) and DROP_LINE rows."""
    dropped = 0
    for method in mapping["DROP_METHOD"].get(donor_type, ()):
        first, stop = method_span(lines, method, donor_path)
        dropped += stop - first
        lines = lines[:first] + lines[stop:]
    for regex, _reason in mapping["DROP_LINE"].get(donor_type, ()):
        pattern = re.compile(regex.encode())
        kept = [line for line in lines if not pattern.match(split_eol(line)[0])]
        if len(kept) == len(lines):
            raise Refused("DROP_LINE %s matches no line of %s" % (regex, donor_path))
        dropped += len(lines) - len(kept)
        lines = kept
    return lines, dropped


def apply_blocks(lines: list, donor_type: str, donor_path: str, mapping: dict) -> tuple[list, int]:
    """The relocate-only DROP_BLOCK rows: '<count>:<sha256>' consecutive lines starting at the one line the regex
    matches; the bytes of the block must hash to the pin (a drifted donor is refused, never half-dropped)."""
    dropped = 0
    for regex, pin in mapping["DROP_BLOCK"].get(donor_type, ()):
        count_text, _colon, digest = pin.partition(":")
        pattern = re.compile(regex.encode())
        hits = [index for index, line in enumerate(lines) if pattern.match(split_eol(line)[0])]
        if len(hits) != 1:
            raise Refused("DROP_BLOCK %s matches %d lines of %s" % (regex, len(hits), donor_path))
        count = int(count_text)
        block = b"".join(lines[hits[0]:hits[0] + count])
        if hits[0] + count > len(lines) or hashlib.sha256(block).hexdigest() != digest:
            raise Refused("DROP_BLOCK %s of %s: %d lines do not hash to the pin %s" % (regex, donor_path, count,
                                                                                      digest[:12]))
        lines = lines[:hits[0]] + lines[hits[0] + count:]
        dropped += count
    return lines, dropped


def apply_inserts(lines: list, donor_type: str, donor_path: str, mapping: dict) -> tuple[list, int]:
    """The INSERT_AFTER rows of the donor file: each inserts its lines (\\n-separated in the map, donor names, the
    file's line ending) after the one line its regex matches."""
    inserted = 0
    for regex, text in mapping["INSERT_AFTER"].get(donor_type, ()):
        pattern = re.compile(regex.encode())
        hits = [index for index, line in enumerate(lines) if pattern.match(split_eol(line)[0])]
        if len(hits) != 1:
            raise Refused("INSERT_AFTER %s matches %d lines of %s" % (regex, len(hits), donor_path))
        eol = split_eol(lines[hits[0]])[1] or b"\n"
        extra = [part.encode() + eol for part in text.split("\\n")]
        lines = lines[:hits[0] + 1] + extra + lines[hits[0] + 1:]
        inserted += len(extra)
    return lines, inserted


def substitute(line: bytes, rows) -> bytes:
    text, eol = split_eol(line)
    for regex, replacement in rows:
        text = re.sub(regex.encode(), replacement.encode(), text)
    return text + eol


def rewrite_line(line: bytes, donor_type: str, mapping: dict) -> bytes:
    """The REWRITE rows of the donor file applied to the line's text (its line ending kept outside the match)."""
    return substitute(line, mapping["REWRITE"].get(donor_type, ()))


def adapt_line(line: bytes, donor_type: str, mapping: dict) -> bytes:
    """The relocate-only ADAPT rows of the donor file, applied before the import mapping and the REWRITE rows."""
    return substitute(line, mapping["ADAPT"].get(donor_type, ()))


def count_hits(lines: list, kind: str, donor_type: str, donor_path: str, mapping: dict) -> int:
    hits_total = 0
    for regex, _replacement in mapping[kind].get(donor_type, ()):
        hits = sum(1 for line in lines if re.search(regex.encode(), split_eol(line)[0]))
        if hits == 0:
            raise Refused("%s %s matches no line of %s" % (kind, regex, donor_path))
        hits_total += hits
    return hits_total


def prepare(data: bytes, donor_path: str, mapping: dict, subset: bool) -> tuple[list, list, dict]:
    """Header, body after the declared drops/inserts (relocate mode: blocks and inserts too) and the counts."""
    donor_type = Path(donor_path).stem
    header, body = strip_header(LINES.findall(data))
    body, dropped = apply_drops(body, donor_type, donor_path, mapping)
    blocks = inserted = adapted = 0
    if not subset:
        body, blocks = apply_blocks(body, donor_type, donor_path, mapping)
        body, inserted = apply_inserts(body, donor_type, donor_path, mapping)
        adapted = count_hits(body, "ADAPT", donor_type, donor_path, mapping)
    rewritten = count_hits(body, "REWRITE", donor_type, donor_path, mapping)
    return header, body, {"dropped_lines": dropped + blocks, "inserted_lines": inserted, "adapted_lines": adapted,
                          "rewritten_lines": rewritten, "both_mode_rows": dropped + rewritten}


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, subset: bool) -> tuple[bytes, dict]:
    """The relocated bytes (or the donor-subset bytes) and the counts of renamed, mapped, rewritten and dropped lines."""
    lines = LINES.findall(data)
    eol = split_eol(lines[0])[1] if lines else b"\n"
    donor_type = Path(donor_path).stem
    header, body, counts = prepare(data, donor_path, mapping, subset)
    packages = mapping["SUBSET_PACKAGE"] if subset else mapping["PACKAGE"]
    types = {} if subset else mapping["TYPE"]
    out = list(header) if subset else [text + eol for text in HEADER]
    renames = imports = 0
    anchored = package_seen = False
    for line in body:
        if not subset:
            line = adapt_line(line, donor_type, mapping)
        text, line_eol = split_eol(line)
        package = None if package_seen else PACKAGE_LINE.match(text)
        if package:
            donor_package = package.group(1).decode()
            if donor_package not in mapping["PACKAGE"]:
                raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
            out.append(b"package " + packages.get(donor_package, donor_package).encode() + b";" + line_eol)
            package_seen = True
            continue
        imported = IMPORT_LINE.match(text)
        if imported:
            fqn = map_import(imported.group(3).decode(), bool(imported.group(2)), mapping, donor_path)
            mapped = line if subset else imported.group(1) + fqn.encode() + imported.group(4) + line_eol
            imports += mapped != line
            out.append(mapped)
            continue
        if not subset and not anchored and TYPE_LINE.match(text):
            out.append(anchor(blob, commit) + line_eol)
            anchored = True
        renamed = rename_types(rewrite_line(line, donor_type, mapping), types)
        renames += renamed != rewrite_line(line, donor_type, mapping)
        out.append(renamed)
    if not subset and not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    counts.update({"type_renames": renames, "import_rewrites": imports})
    return b"".join(out), counts


def body_match(target: bytes, data: bytes, donor_path: str, mapping: dict) -> bool:
    """Read back from disk: every target line is explained by the donor line at its position and the map."""
    donor_type = Path(donor_path).stem
    target_lines = LINES.findall(target)
    if [split_eol(line)[0] for line in target_lines[:len(HEADER)]] != list(HEADER):
        return False
    anchors = [line for line in target_lines if split_eol(line)[0].startswith(ANCHOR_PREFIX)]
    kept = [line for line in target_lines[len(HEADER):] if not split_eol(line)[0].startswith(ANCHOR_PREFIX)]
    try:
        _header, donor, _counts = prepare(data, donor_path, mapping, False)
    except Refused:
        return False
    if len(anchors) != 1 or len(kept) != len(donor):
        return False
    package_seen = False
    for target_line, donor_line in zip(kept, donor):
        adapted = adapt_line(donor_line, donor_type, mapping)
        if target_line == donor_line \
                or target_line == rename_types(rewrite_line(adapted, donor_type, mapping), mapping["TYPE"]):
            continue
        text, eol = split_eol(adapted)
        package = None if package_seen else PACKAGE_LINE.match(text)
        if package:
            package_seen = True
            if target_line == b"package " + mapping["PACKAGE"].get(package.group(1).decode(), "").encode() + b";" + eol:
                continue
        imported = IMPORT_LINE.match(text)
        try:
            if imported and target_line == imported.group(1) + map_import(
                    imported.group(3).decode(), bool(imported.group(2)), mapping, donor_path).encode() \
                    + imported.group(4) + eol:
                continue
        except Refused:
            pass
        return False
    return True


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def run_subset(mapping: dict, donor: Path, rev: str, out: Path) -> int:
    """Donor-side inputs of the differential: main files verbatim unless they carry declared both-mode rows."""
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    for donor_path, target in mapping["FILE"] + mapping["SUPERSEDED"] + mapping["SUBSET_ONLY"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        subset, counts = relocate(data, donor_path, blob, commit, mapping, True)
        verbatim = subset == data
        if "/src/main/" in "/" + target and not verbatim and counts["both_mode_rows"] == 0:
            raise Refused("donor-subset of a main file is not verbatim and has no declared row: %s" % donor_path)
        state = write_if_changed(out / donor_path, subset, False)
        print("SUBSET %s SUBSET_VERBATIM=%s rewrites=%d drops=%d %s" % ("CHANGED" if state else "UNCHANGED",
              str(verbatim).upper(), counts["rewritten_lines"], counts["dropped_lines"], donor_path))
    print("DONOR_SUBSET donor_commit=%s files=%d dir=%s" % (commit, len(mapping["FILE"]) + len(mapping["SUPERSEDED"])
                                                           + len(mapping["SUBSET_ONLY"]), out))
    return 0


def run(module: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(RECIPE_DIR)
    (repo, url), = mapping["REPO"].items()
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed = [], 0
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, counts = relocate(data, donor_path, blob, commit, mapping, False)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        on_disk = (module / target).read_bytes() if (module / target).exists() else relocated
        match = body_match(on_disk, data, donor_path, mapping)
        print("FILE %s body_match=%s renames=%d imports=%d rewrites=%d adapts=%d drops=%d inserts=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(match).upper(), counts["type_renames"],
            counts["import_rewrites"], counts["rewritten_lines"], counts["adapted_lines"], counts["dropped_lines"],
            counts["inserted_lines"], target))
        rows.append((repo, commit, donor_path, blob, sha256(data), url + "/blob/" + commit + "/" + donor_path,
                     target, sha256(on_disk), str(counts["type_renames"]), str(counts["import_rewrites"]),
                     str(counts["rewritten_lines"]), str(counts["adapted_lines"]), str(counts["dropped_lines"]),
                     str(counts["inserted_lines"]), str(match).upper(), RECIPE + " " + VERSION))
    for kind in ("HELD", "OMIT"):
        for donor_path, reason in mapping[kind]:
            blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
            print("%s %s blob=%s %s" % (kind, donor_path, blob[:12], reason))
    rows.sort(key=lambda row: row[6])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(RECIPE_DIR / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d body_match=%d/%d held=%d omit=%d skips=%d nec=%d "
          "provenance=%s" % (RECIPE, VERSION, commit, len(rows), changed, sum(row[14] == "TRUE" for row in rows),
                             len(rows), len(mapping["HELD"]), len(mapping["OMIT"]), len(mapping["SKIP"]),
                             len(mapping["NEC"]), "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (m3/collections)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--donor-subset", metavar="DIR", help="write the donor-side differential inputs under DIR")
    args = parser.parse_args(argv)
    try:
        if args.donor_subset:
            return run_subset(read_map(RECIPE_DIR), Path(args.donor_git), args.donor_rev, Path(args.donor_subset))
        return run(Path(args.module), Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
