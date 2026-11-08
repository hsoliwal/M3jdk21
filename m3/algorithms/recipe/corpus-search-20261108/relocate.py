#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_CORPUS_SEARCH v1 (recipe-first port, never by hand).

Lineage: derived from recipe/catalogue-kernels-20261107/relocate.py (RELOCATE_CATALOGUE_KERNELS v1) byte for byte
except RECIPE_DIR_REL, this docstring and the EDITS table (the declared, hash-pinned substitutions of this leaf: the
class comment of the multi-pattern kernel without its JNI paragraph, the JNI dispatch block of firstOffsets, the
direct-buffer mirror of the prepared automaton, and the four native method declarations, each a multi-line span that
no DROP row can name). Row kinds, drop law, anchor, provenance and the idempotency contract are unchanged.

The same recipe law as the lineage: rewrite only header, package, mapped imports, declared type and member names,
declared drops, declared edits and insertions and one anchor; every other byte is kept. The drops are the verdicts of
DECISION.tsv: a donor member that is a JNI lane (the module is pure Java; the donor's Java path is the exact
fallback), a proof harness, or the same named algorithm as another kept member is removed by one DROP_METHOD,
DROP_TYPE or DROP_LINE row naming its reason, never by editing a body.

Copies every FILE row of RELOCATION_MAP.tsv from a donor git tree (--donor-git DIR at --donor-rev REV) into the
module m3/algorithms, rewriting only (0) the EDIT rows of that donor file, (1) the leading license header line(s),
replaced by the M3JDK header, (2) the package line, through the PACKAGE rows, (3) import lines of mapped packages and
types, through the PACKAGE and TYPE rows, (4) the MEMBER and TYPE rows (whole words, on every line), (5) the
DROP_METHOD, DROP_TYPE and DROP_LINE rows, (6) one //FLLDCIM: anchor line inserted before the top-level type
declaration, naming the donor blob and commit, and (7) the INSERT_AFTER rows. Every other byte is kept (line endings
too). Idempotent: writes a file only when its bytes differ; a second run reports changed=0. --check writes nothing and
exits 1 when a run would change a file. Refuses (exit 2) an unmapped package, an import that is neither a JDK
package, nor a mapped donor package, nor a declared EXTERNAL package, a DROP row matching no line or more than one
declaration, an EDIT whose replaced bytes do not match its pin, or a file without a top-level type. The donor
repository and its paths live only in RELOCATION_MAP.tsv.

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
RECIPE_DIR_REL = "m3/algorithms/recipe/corpus-search-20261108"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "donor_url",
                      "target_path", "target_sha256", "type_renames", "import_rewrites", "dropped_lines",
                      "inserted_lines", "edits", "body_match", "body_match_edited", "recipe")
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
                     "DROP_TYPE": {}, "DROP_LINE": {}, "DROP_DUMP": {}, "SKIP": [], "NEC": [], "SUPERSEDED": [],
                     "OWNED": [], "INSERT_AFTER": {}, "EDIT": [], "MEMBER": {}, "DONOR_ONLY": []}
    for kind, donor, target, *_note in rows:
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind in ("SKIP", "NEC", "SUPERSEDED", "OWNED", "DONOR_ONLY"):
            mapping[kind].append((donor, target))
        elif kind in ("DROP_METHOD", "DROP_TYPE"):
            name, paren, params = donor.partition("(")
            owner, _dot, member = name.rpartition(".")
            mapping[kind].setdefault(owner, []).append(member + paren + params)
        elif kind in ("DROP_LINE", "DROP_DUMP"):
            mapping[kind].setdefault(donor, []).append(target)
        elif kind == "EDIT":
            mapping[kind].append((donor, target, _note[0].split()[0] if _note else ""))
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


# id -> (old, new): a declared substitution of donor bytes; the EDIT row of the map names the donor file and pins the
# sha256 prefix of the replaced bytes (the census2-a5-20261106 EDIT law, SUB kind only).
EDITS = {
    # the class comment of the kernel: the JNI-candidate paragraph of the donor is replaced by the pure-Java contract
    "E1_MPS_DOC": (" * <p>This owner exists because the competitive-problem catalogue already maps\n"
                   " * {@code AlgorithmShape.AHO_CORASICK} to a JNI-candidate multi-pattern kernel. Java remains the\n"
                   " * semantic oracle. The optional native backend is a differential acceleration lane only and does\n"
                   " * not define Unicode or regex semantics.</p>\n",
                   " * <p>Pure Java: the prepared automaton answers prefix and exact multiplicity queries over the pattern\n"
                   " * set and the first byte offset of every pattern in a haystack. Bytes are the alphabet; the kernel\n"
                   " * defines no Unicode or regex semantics.</p>\n"),
    # firstOffsets(Prepared, byte[], monitor): the JNI dispatch block is removed; the Java lane is the only lane
    "E2_MPS_FIRST_OFFSETS_JAVA_ONLY": ("        if (monitor == null\n"
                                       "                && ChallengeNativeSearch.nativeEnabled()\n"
                                       "                && text.length >= NATIVE_MIN_TEXT_BYTES\n"
                                       "                && stable.patternCount() >= NATIVE_MIN_PATTERNS) {\n"
                                       "            int[] result =\n"
                                       "                    nativeFirstOffsets(\n"
                                       "                            text,\n"
                                       "                            stable.flatPatterns,\n"
                                       "                            stable.patternOffsets,\n"
                                       "                            stable.patternLengths);\n"
                                       "            validateNativeResult(stable, text, result);\n"
                                       "            return result;\n"
                                       "        }\n"
                                       "        return firstOffsetsJava(stable, text, checked);\n",
                                       "        return firstOffsetsJava(stable, text, checked);\n"),
    # Prepared(...): the direct-buffer mirror of the flat patterns (the JNI lane's view) is not built
    "E3_MPS_PREPARED_DIRECT": ("            this.patternLengths = patternLengths;\n"
                               "\n"
                               "            ByteBuffer patternStorage = ByteBuffer.allocateDirect(flatPatterns.length);\n"
                               "            patternStorage.put(flatPatterns).flip();\n"
                               "            directPatterns = patternStorage.asReadOnlyBuffer();\n"
                               "\n"
                               "            ByteBuffer offsetStorage =\n"
                               "                    ByteBuffer.allocateDirect(Math.multiplyExact(patternOffsets.length, Integer.BYTES))\n"
                               "                            .order(ByteOrder.nativeOrder());\n"
                               "            IntBuffer offsetInts = offsetStorage.asIntBuffer();\n"
                               "            offsetInts.put(patternOffsets).flip();\n"
                               "            directOffsets = offsetInts.asReadOnlyBuffer();\n"
                               "\n"
                               "            ByteBuffer lengthStorage =\n"
                               "                    ByteBuffer.allocateDirect(Math.multiplyExact(patternLengths.length, Integer.BYTES))\n"
                               "                            .order(ByteOrder.nativeOrder());\n"
                               "            IntBuffer lengthInts = lengthStorage.asIntBuffer();\n"
                               "            lengthInts.put(patternLengths).flip();\n"
                               "            directLengths = lengthInts.asReadOnlyBuffer();\n"
                               "        }\n",
                               "            this.patternLengths = patternLengths;\n"
                               "        }\n"),
    # the four native method declarations (multi-line, so no DROP row can name them): the module is pure Java
    "E4_MPS_NATIVE_DECLS": ("\n"
                            "    private static native int[] nativeFirstOffsets(\n"
                            "            byte[] haystack,\n"
                            "            byte[] flatPatterns,\n"
                            "            int[] patternOffsets,\n"
                            "            int[] patternLengths);\n"
                            "\n"
                            "    private static native int[] nativeFirstOffsetsDirect(\n"
                            "            ByteBuffer haystack,\n"
                            "            ByteBuffer flatPatterns,\n"
                            "            IntBuffer patternOffsets,\n"
                            "            IntBuffer patternLengths,\n"
                            "            int patternCount);\n"
                            "\n"
                            "    private static native int nativePrefixCountDirect(\n"
                            "            ByteBuffer flatPatterns,\n"
                            "            IntBuffer patternOffsets,\n"
                            "            IntBuffer patternLengths,\n"
                            "            int patternCount,\n"
                            "            byte[] prefix);\n"
                            "\n"
                            "    private static native int nativeExactCountDirect(\n"
                            "            ByteBuffer flatPatterns,\n"
                            "            IntBuffer patternOffsets,\n"
                            "            IntBuffer patternLengths,\n"
                            "            int patternCount,\n"
                            "            byte[] pattern);\n"
                            "}",
                            "}"),
}


def apply_edits(data: bytes, donor_path: str, mapping: dict) -> tuple[bytes, int]:
    """Applies the EDIT rows of one donor file in map order; each replaced span must match its pinned sha256."""
    count = 0
    for path, edit_id, pinned in mapping["EDIT"]:
        if path != donor_path:
            continue
        if edit_id not in EDITS:
            raise Refused("EDIT row %s of %s names no declared edit" % (edit_id, donor_path))
        old, new = (text.encode("utf-8") for text in EDITS[edit_id])
        if data.count(old) != 1:
            raise Refused("EDIT %s: anchor occurs %d times (need 1) in %s" % (edit_id, data.count(old), donor_path))
        if len(pinned) < 12 or not sha256(old).startswith(pinned):
            raise Refused("EDIT %s: replaced bytes sha256 %s do not match the pinned %s" % (
                edit_id, sha256(old)[:12], pinned))
        data = data.replace(old, new)
        count += 1
    return data, count


def rename_types(line: bytes, types: dict, members: dict | None = None) -> bytes:
    for donor, target in (members or {}).items():
        line = re.sub(rb"\b" + re.escape(donor.encode()) + rb"\b", target.encode(), line)
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


def leading_doc(lines: list, first: int, indent: bytes) -> int:
    """The first line of the annotations and the one Javadoc block directly above `first` at `indent`."""
    while first > 0 and ANNOTATION_LINE.match(lines[first - 1]) and lines[first - 1].startswith(indent):
        first -= 1
    above = split_eol(lines[first - 1])[0] if first > 0 else b""
    if above.strip().endswith(b"*/") and above.startswith(indent):
        start = first - 1
        while start >= 0 and not split_eol(lines[start])[0].lstrip().startswith(b"/**"):
            if split_eol(lines[start])[0].lstrip().startswith(b"/*") or not lines[start].startswith(indent):
                return first
            start -= 1
        if start >= 0:
            first = start
    return first


def param_types(lines: list, start: int) -> str:
    """The comma-joined parameter types of the declaration starting at `start` (names, final and annotations dropped,
    generic arguments erased): the selector a DROP_METHOD row may carry for one overload."""
    text = b""
    for line in lines[start:]:
        text += split_eol(line)[0] + b" "
        if b")" in text[text.index(b"("):]:
            break
    inner = text[text.index(b"(") + 1:].decode()
    depth, cut = 0, []
    for char in inner:
        depth += char == "<"
        depth -= char == ">"
        if char == ")" and depth == 0:
            break
        cut.append(char if depth == 0 or char in "<>" else "")
    params = [re.sub(r"<>", "", part).strip() for part in "".join(cut).split(",")]
    types = [re.sub(r"^(?:final\s+|@\w+\s+)*", "", part).rpartition(" ")[0].replace(" ", "") for part in params
             if part]
    return ",".join(types)


def member_span(lines: list, member: str, donor_path: str, kind: str) -> tuple[int, int]:
    """[first, stop) of the declaration of `member` (a method, optionally `name(type,type)` for one overload, or a
    nested type): its leading Javadoc and annotations, the declaration, the closing brace at the declaration's
    indentation and one trailing blank line."""
    name, paren, selector = member.partition("(")
    if kind == "DROP_TYPE":
        declaration = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final|abstract|sealed)\s+)*"
                                 rb"(?:class|record|interface|enum)\s+" + name.encode() + rb"\b")
    else:
        declaration = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final|synchronized|default|abstract)"
                                 rb"\s+)*(?:<[^>]*>\s+)?(?!(?:return|new|throw|else|case|yield)\b)\w[\w.<>\[\], ?]*\s+"
                                 + name.encode() + rb"\s*\(")
    starts = [index for index, line in enumerate(lines) if declaration.match(split_eol(line)[0])]
    if paren:
        starts = [index for index in starts if param_types(lines, index) == selector.rstrip(")")]
    if len(starts) != 1:
        raise Refused("%s %s: %d declarations in %s" % (kind, member, len(starts), donor_path))
    start = starts[0]
    indent = declaration.match(lines[start]).group(1)
    first = leading_doc(lines, start, indent)
    end = start
    while end < len(lines) and split_eol(lines[end])[0] != indent + b"}" \
            and not (end == start and split_eol(lines[end])[0].rstrip().endswith(b"}")
                     and lines[end].count(b"{") == lines[end].count(b"}")):
        end += 1
    if end == len(lines):
        raise Refused("%s %s: no closing brace at its indentation in %s" % (kind, member, donor_path))
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
    for kind in ("DROP_TYPE", "DROP_METHOD"):
        for member in mapping[kind].get(donor_type, ()):
            first, stop = member_span(lines, member, donor_path, kind)
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
            out.append(rename_types(line, mapping["TYPE"], mapping["MEMBER"]))
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
        renamed = rename_types(line, mapping["TYPE"], mapping["MEMBER"])
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
        if target_line == donor_line or target_line == rename_types(donor_line, mapping["TYPE"], mapping["MEMBER"]):
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
        edited, edit_count = apply_edits(data, donor_path, mapping)
        relocated, counts = relocate(edited, donor_path, blob, commit, mapping)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        on_disk = (module / target).read_bytes() if (module / target).exists() else relocated
        match = body_match(on_disk, data, donor_path, mapping)
        match_edited = body_match(on_disk, edited, donor_path, mapping)
        print("FILE %s body_match=%s body_match_edited=%s renames=%d imports=%d drops=%d inserts=%d edits=%d %s" % (
            "CHANGED" if state else "UNCHANGED", str(match).upper(), str(match_edited).upper(), counts["type_renames"],
            counts["import_rewrites"], counts["dropped_lines"], counts["inserted_lines"], edit_count, target))
        rows.append((repo, commit, donor_path, blob, sha256(data), url + "/blob/" + commit + "/" + donor_path,
                     target, sha256(on_disk), str(counts["type_renames"]), str(counts["import_rewrites"]),
                     str(counts["dropped_lines"]), str(counts["inserted_lines"]), str(edit_count),
                     str(match).upper(), str(match_edited).upper(), RECIPE + " " + VERSION))
    rows.sort(key=lambda row: row[6])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(RECIPE_DIR / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d changed=%d body_match=%d/%d body_match_edited=%d/%d edits=%d"
          " skips=%d nec=%d owned=%d donor_only=%d provenance=%s"
          % (RECIPE, VERSION, commit, len(rows), changed, sum(row[13] == "TRUE" for row in rows), len(rows),
             sum(row[14] == "TRUE" for row in rows), len(rows), sum(int(row[12]) for row in rows),
             len(mapping["SKIP"]), len(mapping["NEC"]), len(mapping["OWNED"]), len(mapping["DONOR_ONLY"]),
             "CHANGED" if provenance_state else "UNCHANGED"))
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
