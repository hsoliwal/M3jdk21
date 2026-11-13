#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Relocation recipe RELOCATE_XXHASH64_STREAMING v4 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the repository root four levels above this recipe directory),
and applies the OWNER rows (declared patches of files the module already owns).

Java files (.java): rewrites only (1) the leading license header (a // SPDX block or a /* ... */ license block), replaced
by the M3JDK header of the sibling java.base files, (2) the package line, through the PACKAGE rows, or dropped for an
UNNAMED row (the default package of the jtreg tests of test/jdk/jdk/internal/m3/hash), (3) import lines of mapped
packages and types, through the PACKAGE, TYPE and EXTERNAL rows, (4) the TYPE rows whose donor and target names differ,
as whole-word tokens, (5) the LITERAL rows (a declared string-literal token renamed exactly as NAMING_RULES.tsv would,
so jdk_naming.py and this recipe share one fixed point), (6) the TOKEN rows, file-scoped whole-token renames of a member
access path or a magic literal onto the owner the module already has (never on a whole comment line), (7) the DROP rows
(a donor line pinned by the sha256 of its bytes, refused unless it occurs exactly once), (8) one //FLLDCIM: anchor line
before the top-level type declaration, naming the donor blob URL at the donor commit, and (9) the INSERT rows: a
declared block file placed in the PREAMBLE (after the header, before the package or import lines: jtreg tags and the
imports of a default-package test) or at the TAIL (before the closing brace of the top-level type). Every other byte is
kept.

SPLIT (NESTED, LIFT, REPLACE rows): a FILE row with a NESTED row relocates the nested type, not the donor's top-level
type. The target file is the header, the PREAMBLE inserts, the package line, the donor import region, the nested type's
own doc comment, the anchor, the nested declaration de-indented one level with its static modifier removed, the LIFT
HEAD blocks (donor top-level members pinned by the sha256 of their bytes, copied as they are), the nested members
de-indented one level, the INSERT TAIL blocks, the LIFT TAIL blocks and the closing brace. REPLACE rows swap a donor
block (pinned by the sha256 of its exact bytes, refused unless it occurs exactly once in the lifted or nested slice) for
a declared block file: this is where the donor's own copy of the kernel arithmetic gives way to calls of the owner's
atoms (never two kernels). Replacement and inserted lines are never de-indented or anchored; TYPE, LITERAL and TOKEN
renames apply to every emitted body line alike. BRACE_INDENT: a donor line that is a lone `}` at column 0 (a formatting
artifact of the donor) is indented to its brace depth in the assembled type body (whitespace only, no token changes).

OWNER rows: target path, pre_sha256:post_sha256, patch file (a unified diff next to this script, git apply -p1 from the
module root). The file at post_sha256 is UNCHANGED; at pre_sha256 the patch is applied (git apply --check first) and the
result must hash to post_sha256; any other content is refused as owner drift. The patch is the one declared change of an
existing owner file (here: MRL helper extraction on M3XxHash so that its stripe round, lane merge and tail are
package-private atoms).

NEC rows name donor files of the family that are NOT relocated because their equivalence with an existing owner is not
decided here; OMIT rows name donor files left out by decision. Both are informational: the recipe pins their donor blob
in its run log (HELD lines) and copies nothing.

PROVENANCE.tsv (next to this script) pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row and
states body_match (the bytes after the header/package line, minus the anchor, equal the donor's: only possible without
any declared op), body_match_renamed (TRUE: the target body equals the donor body after the declared TYPE, LITERAL,
TOKEN, DROP and INSERT rows; SPLIT: the target was assembled from the declared nested, lifted, replaced and inserted
blocks) and declared_ops (every declared op with its count). OWNER rows are listed with their pre and post sha256.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package, a Java
file without a top-level type, a DROP, LIFT or REPLACE pin that is absent or ambiguous, a LITERAL or TOKEN row that
matches nothing, a NESTED type that is absent, a block file that is absent, and an OWNER file whose content is neither
the pre- nor the post-image.

Derived from m3/arrays/recipe/bitmatrix-20261106/relocate.py v3 (RELOCATE_BIT_MATRIX, PR #2521): the C / JNI / INCLUDE
path and the CMake registration are dropped (java.base, Java only); TOKEN, UNNAMED, NESTED, LIFT, REPLACE, INSERT and
OWNER rows, the block-comment license header and the sibling header form are added.

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
RECIPE_PATH = "m3/qualification/" + RECIPE_DIR.parent.name + "/recipe/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_ops", "recipe")
HEADER = (b"/*", b" * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0",
          b" * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others", b" */")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
LICENSE_WORDS = re.compile(rb"Copyright|SPDX|Licensed under")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
TOP_START = re.compile(rb"^(?:/\*\*|@\w|(?:public|final|abstract|sealed|non-sealed|strictfp|class|interface|enum|record)\b)")
NESTED_DECL = re.compile(rb"^(\s*)((?:(?:public|protected|private|final|abstract|sealed|non-sealed|strictfp)\s+)*)static\s+"
                         rb"((?:(?:final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+)(\w+)\b(.*)$")
COMMENT_LINE = re.compile(rb"^\s*(?://|/\*|\*)")
CLOSING_LINE = re.compile(rb"^}\s*$")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")
INDENT = b"    "
BLOCK_SUFFIX = ".java.txt"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "EXTERNAL": {}, "LITERAL": {}, "TOKEN": [], "DROP": [],
                     "UNNAMED": set(), "NESTED": {}, "LIFT": [], "REPLACE": [], "INSERT": [], "OWNER": [], "HELD": []}
    for kind, donor, target, *rest in rows:  # columns: kind, donor, target, arg, note
        extra = rest[0] if rest else ""
        if kind in HELD_KINDS:
            extra = rest[-1] if rest else ""
        if kind == "FILE":
            mapping["FILE"].append((donor, target))
        elif kind == "DROP":
            mapping["DROP"].append((donor, target))
        elif kind == "TOKEN":
            mapping["TOKEN"].append((donor, target, extra))
        elif kind == "UNNAMED":
            mapping["UNNAMED"].add(donor)
        elif kind == "NESTED":
            mapping["NESTED"][donor] = target
        elif kind in ("LIFT", "REPLACE", "INSERT"):
            mapping[kind].append((donor, target, extra))
        elif kind == "OWNER":
            mapping["OWNER"].append((donor, target, extra))
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, extra))
        else:
            mapping[kind][donor] = target
    return mapping


def git(repo: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True).stdout


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


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


def rename_tokens(line: bytes, tokens: list, donor_path: str, seen: dict) -> bytes:
    """Renames each TOKEN row of this file as a whole token, except on a whole comment line."""
    if COMMENT_LINE.match(line):
        return line
    for path, donor, target in tokens:
        if path != donor_path:
            continue
        pattern = rb"(?<![\w.])" + re.escape(donor.encode()) + rb"(?![\w.])"
        line, count = re.subn(pattern, target.encode(), line)
        seen[(path, donor)] = seen.get((path, donor), 0) + count
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


def apply_drops(lines: list, donor_path: str, mapping: dict) -> tuple[list, int]:
    """Removes every declared line (sha256 of its bytes without the end of line); refuses unless each occurs once."""
    drops = {line_sha for path, line_sha in mapping["DROP"] if path == donor_path}
    if not drops:
        return lines, 0
    kept, found = [], {sha: 0 for sha in drops}
    for line in lines:
        sha = sha256(split_eol(line)[0])
        if sha in found:
            found[sha] += 1
            continue
        kept.append(line)
    for sha, count in found.items():
        if count != 1:
            raise Refused("DROP row %s of %s matched %d lines, expected exactly 1" % (sha[:12], donor_path, count))
    return kept, len(drops)


def consume_header(lines: list) -> int:
    """Index of the first line after the license header: a run of // SPDX / Copyright lines, or one /* ... */ block
    whose text names a license."""
    index = 0
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    if index == 0 and lines and split_eol(lines[0])[0].startswith(b"/*") and not lines[0].startswith(b"/**"):
        end = next((at for at, line in enumerate(lines) if b"*/" in line), None)
        if end is not None and LICENSE_WORDS.search(b"".join(lines[:end + 1])):
            index = end + 1
    return index


def find_block(lines: list, block_sha: str, what: str, donor_path: str) -> tuple[int, int]:
    """The [start, end) of the one run of lines whose joined bytes hash to block_sha; refused unless exactly one."""
    hits = []
    for start in range(len(lines)):
        digest = hashlib.sha256()
        for end in range(start, len(lines)):
            digest.update(lines[end])
            if digest.hexdigest() == block_sha:
                hits.append((start, end + 1))
                break
    if len(hits) != 1:
        raise Refused("%s block %s of %s matched %d runs, expected exactly 1" % (what, block_sha[:12], donor_path, len(hits)))
    return hits[0]


def read_block(recipe_dir: Path, name: str) -> list:
    path = recipe_dir / name
    if not path.is_file() or not name.endswith(BLOCK_SUFFIX):
        raise Refused("block file %s is absent or not a %s file" % (name, BLOCK_SUFFIX))
    return LINES.findall(path.read_bytes())


def apply_replaces(tagged: list, donor_path: str, mapping: dict, recipe_dir: Path, counts: dict) -> list:
    """Swaps every REPLACE block of this file found in the slice (donor-origin lines only) for its block file."""
    for path, block_sha, block_file in mapping["REPLACE"]:
        if path != donor_path:
            continue
        donor_lines = [line for line, origin in tagged if origin]
        try:
            start, end = find_block(donor_lines, block_sha, "REPLACE", donor_path)
        except Refused:
            continue
        # map the donor-line indexes back onto the tagged list
        positions = [at for at, (_line, origin) in enumerate(tagged) if origin]
        first, last = positions[start], positions[end - 1]
        tagged = tagged[:first] + [(line, False) for line in read_block(recipe_dir, block_file)] + tagged[last + 1:]
        counts["REPLACE"] = counts.get("REPLACE", 0) + 1
    return tagged


def dedent(line: bytes) -> bytes:
    return line[len(INDENT):] if line.startswith(INDENT) else line


def nested_slice(rest: list, nested: str, donor_path: str) -> tuple[int, int, int]:
    """(doc start, declaration index, end exclusive) of the nested type in the lines after the import region."""
    decls = [at for at, line in enumerate(rest) if (m := NESTED_DECL.match(split_eol(line)[0])) and m.group(4).decode() == nested]
    if len(decls) != 1:
        raise Refused("NESTED type %s of %s matched %d declarations, expected exactly 1" % (nested, donor_path, len(decls)))
    decl = decls[0]
    doc = decl
    while doc > 0 and COMMENT_LINE.match(rest[doc - 1]) and split_eol(rest[doc - 1])[0].strip():
        doc -= 1
    depth, in_comment, end = 0, False, None
    for at in range(decl, len(rest)):
        text, in_comment = mask(split_eol(rest[at])[0], in_comment)
        depth += text.count(b"{") - text.count(b"}")
        if depth == 0 and text.count(b"}"):
            end = at + 1
            break
    if end is None:
        raise Refused("NESTED type %s of %s has no closing brace" % (nested, donor_path))
    return doc, decl, end


def mask(text: bytes, in_comment: bool) -> tuple[bytes, bool]:
    """The line with comments and string / char literals blanked (SFA: braces inside them never count)."""
    out, at = bytearray(), 0
    while at < len(text):
        if in_comment:
            close = text.find(b"*/", at)
            if close < 0:
                return bytes(out), True
            at, in_comment = close + 2, False
        elif text.startswith(b"//", at):
            break
        elif text.startswith(b"/*", at):
            at, in_comment = at + 2, True
        elif text[at] in b"\"'":
            quote = text[at]
            at += 1
            while at < len(text) and text[at] != quote:
                at += 2 if text[at] == 0x5C else 1
            at += 1
        else:
            out.append(text[at])
            at += 1
    return bytes(out), in_comment


def indent_lone_braces(tagged: list) -> list:
    """BRACE_INDENT: a donor line that is a lone `}` at column 0 (a formatting artifact of the donor, which closes a
    member or a loop at column 0) is indented to its brace depth in the assembled type body; no token changes."""
    out, depth, in_comment = [], 0, False
    for line, origin in tagged:
        text, eol = split_eol(line)
        masked, in_comment = mask(text, in_comment)
        if origin and text == b"}":
            line = INDENT * (depth - 1) + b"}" + eol
        depth += masked.count(b"{") - masked.count(b"}")
        out.append((line, origin))
    return out


def body_lines(tagged: list, donor_path: str, blob: str, commit: str, mapping: dict, seen: dict, token_seen: dict,
               anchored: list) -> list:
    """Renames every emitted line; inserts the anchor before the first top-level type declaration."""
    out = []
    for line, origin in tagged:
        text, eol = split_eol(line)
        imported = IMPORT_LINE.match(text)
        if imported:
            line = imported.group(1) + map_import(imported.group(2).decode(), mapping, donor_path).encode() \
                + imported.group(3) + eol
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor(donor_path, blob, commit) + eol)
            anchored.append(True)
        out.append(rename_tokens(rename_literals(rename_types(line, mapping["TYPE"]), mapping["LITERAL"], seen),
                                 mapping["TOKEN"], donor_path, token_seen))
    return out


def inserts(donor_path: str, where: str, mapping: dict, recipe_dir: Path, counts: dict) -> list:
    out = []
    for path, position, block_file in mapping["INSERT"]:
        if path == donor_path and position == where:
            out.extend(read_block(recipe_dir, block_file))
            counts["INSERT"] = counts.get("INSERT", 0) + 1
    return out


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict, recipe_dir: Path, seen: dict,
                  token_seen: dict) -> tuple[bytes, bool, str, str]:
    lines = LINES.findall(data)
    eol = split_eol(lines[0])[1] if lines else b"\n"
    index = consume_header(lines)
    counts: dict = {}
    body, dropped = apply_drops(lines[index:], donor_path, mapping)
    if dropped:
        counts["DROP"] = dropped
    package_at = next((at for at, line in enumerate(body) if PACKAGE_LINE.match(split_eol(line)[0])), None)
    if package_at is None:
        raise Refused("no package line in %s" % donor_path)
    donor_package = PACKAGE_LINE.match(split_eol(body[package_at])[0]).group(1).decode()
    if donor_package not in mapping["PACKAGE"]:
        raise Refused("unmapped package %s in %s" % (donor_package, donor_path))
    out = [text + eol for text in HEADER] + [eol]
    preamble = inserts(donor_path, "PREAMBLE", mapping, recipe_dir, counts)
    if preamble:
        out.extend(preamble + [eol])
    if donor_path in mapping["UNNAMED"]:
        counts["UNNAMED"] = 1
    else:
        out.append(b"package " + mapping["PACKAGE"][donor_package].encode() + b";" + eol)
    after = body[package_at + 1:]
    if donor_path in mapping["UNNAMED"] and after and not after[0].strip():
        after = after[1:]  # the blank line after the dropped package line: the PREAMBLE block already ends with one
    region_end = next((at for at, line in enumerate(after) if TOP_START.match(split_eol(line)[0])), len(after))
    imports, rest = after[:region_end], after[region_end:]
    nested = mapping["NESTED"].get(donor_path)
    while nested and len(imports) > 1 and not imports[-1].strip() and not imports[-2].strip():
        imports.pop()  # a dropped import leaves a second blank line before the type: one blank line, as the donor had
    anchored: list = []
    out.extend(body_lines([(line, True) for line in imports], donor_path, blob, commit, mapping, seen, token_seen, anchored))
    nested = mapping["NESTED"].get(donor_path)
    if nested:
        counts["NESTED"] = 1
        doc, decl, end = nested_slice(rest, nested, donor_path)
        lifted = {"HEAD": [], "TAIL": []}
        for path, block_sha, where in mapping["LIFT"]:
            if path != donor_path:
                continue
            outside = rest[:doc] + rest[end:]
            start, stop = find_block(outside, block_sha, "LIFT", donor_path)
            block = apply_replaces([(line, True) for line in outside[start:stop]], donor_path, mapping, recipe_dir, counts)
            lifted[where].append(block)
            counts["LIFT"] = counts.get("LIFT", 0) + 1
        slice_ = apply_replaces([(line, True) for line in rest[doc:end]], donor_path, mapping, recipe_dir, counts)
        decl_at = next(at for at, (line, origin) in enumerate(slice_) if origin and NESTED_DECL.match(split_eol(line)[0]))
        m = NESTED_DECL.match(split_eol(slice_[decl_at][0])[0])
        declaration = m.group(2) + m.group(3) + m.group(4) + m.group(5) + split_eol(slice_[decl_at][0])[1]
        tagged = [(dedent(line), True) for line, _origin in slice_[:decl_at]] + [(declaration, True)]
        for block in lifted["HEAD"]:
            tagged.extend(block)
            tagged.append((eol, False))
        tagged.extend((dedent(line) if origin else line, origin) for line, origin in slice_[decl_at + 1:-1])
        tail = inserts(donor_path, "TAIL", mapping, recipe_dir, counts)
        if tail:
            tagged.append((eol, False))
            tagged.extend((line, False) for line in tail)
        for block in lifted["TAIL"]:
            tagged.append((eol, False))
            tagged.extend(block)
        tagged.append((dedent(slice_[-1][0]), True))
        tagged = indent_lone_braces(tagged)
        declared_replaces =sum(1 for path, _sha, _file in mapping["REPLACE"] if path == donor_path)
        if counts.get("REPLACE", 0) != declared_replaces:
            raise Refused("%d of %d REPLACE rows of %s matched a block" % (counts.get("REPLACE", 0), declared_replaces, donor_path))
        out.extend(body_lines(tagged, donor_path, blob, commit, mapping, seen, token_seen, anchored))
        renamed = "SPLIT"
    else:
        tagged = [(line, True) for line in rest]
        tail = inserts(donor_path, "TAIL", mapping, recipe_dir, counts)
        if tail:
            closing = max(at for at, (line, _origin) in enumerate(tagged) if CLOSING_LINE.match(split_eol(line)[0]))
            tagged = tagged[:closing] + [(eol, False)] + [(line, False) for line in tail] + tagged[closing:]
        emitted = body_lines(tagged, donor_path, blob, commit, mapping, seen, token_seen, anchored)
        out.extend(emitted)
        renamed = "TRUE"
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    for kind in ("TOKEN",):
        n = sum(1 for path, _d, _t in mapping[kind] if path == donor_path)
        if n:
            counts[kind] = n
    declared = ",".join("%s=%d" % (k, v) for k, v in sorted(counts.items())) or "-"
    # body_match (v3 semantics: the donor bytes after its package line equal the target body) is FALSE for every row here:
    # the anchor line is always inserted, and every FILE row of this map declares at least one op.
    body_match = not counts and b"".join(after) == b"".join(out[len(HEADER) + 2:])
    return b"".join(out), body_match, renamed, declared


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def apply_owner(module: Path, recipe_dir: Path, target: str, pins: str, patch: str, check: bool) -> tuple[str, str, str]:
    pre, post = pins.split(":")
    path = module / target
    if not path.is_file():
        raise Refused("OWNER file %s is absent from the module" % target)
    current = sha256(path.read_bytes())
    if current == post:
        return "UNCHANGED", pre, post
    if current != pre:
        raise Refused("OWNER file %s is at %s: neither the pre-image %s nor the post-image %s (owner drift)"
                      % (target, current[:12], pre[:12], post[:12]))
    patch_path = recipe_dir / patch
    if not patch_path.is_file():
        raise Refused("OWNER patch %s is absent" % patch)
    try:
        git(module, "apply", "--check", str(patch_path))
    except subprocess.CalledProcessError as failed:
        raise Refused("OWNER patch %s does not apply: %s" % (patch, failed.stderr.decode(errors="replace").strip()))
    if check:
        return "CHANGED", pre, post
    git(module, "apply", str(patch_path))
    applied = sha256(path.read_bytes())
    if applied != post:
        raise Refused("OWNER patch %s applied but %s hashes to %s, expected %s" % (patch, target, applied[:12], post[:12]))
    return "CHANGED", pre, post


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed, seen, token_seen = [], 0, {}, {}
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        relocated, body_match, renamed, declared = relocate_java(data, donor_path, blob, commit, mapping, recipe_dir, seen, token_seen)
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_ops=%s %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), renamed, declared, target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), renamed, declared, RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    for path, donor_token, target_token in mapping["TOKEN"]:
        count = token_seen.get((path, donor_token), 0)
        if count == 0:
            raise Refused("TOKEN row %s of %s matched no token" % (donor_token, path))
        print("TOKEN %s -> %s occurrences=%d %s" % (donor_token, target_token, count, path))
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s %s" % (kind, donor_path, blob[:12], target, note))
    for target, pins, patch in mapping["OWNER"]:
        state, pre, post = apply_owner(module, recipe_dir, target, pins, patch, check)
        changed += state == "CHANGED"
        print("OWNER %s pre=%s post=%s patch=%s %s" % (state, pre[:12], post[:12], patch, target))
        rows.append(("hsoliwal/M3jdk21", "-", target, "-", pre, target, post, "FALSE", "PATCH", "OWNER=" + patch, RECIPE + " " + VERSION))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d owners=%d held=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(mapping["FILE"]), len(mapping["OWNER"]), len(mapping["HELD"]), changed,
        "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit sha; never a moving FETCH_HEAD)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent.parent.parent), help="repository root")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
