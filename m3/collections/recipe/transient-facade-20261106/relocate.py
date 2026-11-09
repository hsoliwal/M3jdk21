#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_TRANSIENT_FACADE v4 (recipe-first port, never by hand).

Relocates the rows of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at --donor-rev
REV) into the module (--module DIR, default: the grandparent of this recipe directory, m3/collections). Three row kinds
carry source:

MERGE <donor .java> <target .java>: member insertion. The donor's top-level type body (every line between its type
declaration line and its closing brace) is inserted into the existing target type before the target's closing brace,
between two anchor lines (//FLLDCIM: RELOCATE-MERGE begin ... / end ...). The donor header, package line, type
Javadoc and type declaration line are not copied (the target keeps its own); the donor's import lines are mapped
through the PACKAGE/TYPE rows and the ones the target lacks are inserted into the target's import block at their
sorted position. Inside the body only the declared rows change bytes: TYPE and MEMBER rows (whole-word renames),
DROP_LINE and DROP_METHOD rows (a dropped declaration takes the blank line that follows it), REWRITE rows (declared
regular-expression substitutions, each naming its reason) and one INSERT_BLOCK row (declared lines appended at the end
of the merged block: the lazy native-ABI probe in the host's own pattern). Every other byte of every donor body line is
kept, and every other byte of the target is kept.

APPEND <donor .c> <target .c>: the donor's body (every line after its SPDX header lines) is appended to the existing
target source between the same two anchor lines, with the INCLUDE rows (#include of the donor's javac -h header ->
<jni.h>), the JNI rows (the Java_<mangled donor class>_ symbol prefix, as a whole-token prefix), the TYPE rows, the
DROP/REWRITE rows and one INSERT_BLOCK row applied. The donor's own #include lines stay inside the block (include
guards make the repeats harmless).

FILE <donor .java> <target .java>: a new file (the oracles), as recipe v2: the M3JDK header, the mapped package line,
mapped imports (JDK packages and declared EXTERNAL packages pass), TYPE and MEMBER renames, DROP/REWRITE rows, and one
//FLLDCIM: RELOCATE anchor line before the top-level type declaration naming the donor blob and commit (never the
donor path or repository: those tokens are JDK-naming rename tokens and live in RELOCATION_MAP.tsv and PROVENANCE.tsv).

Idempotent: a target that already carries the anchored block has the block replaced by the recomputed one, and a file
is written only when its bytes differ (second run changed=0). --check writes nothing and exits 1 when a run would
change a file. Refuses (exit 2) an unmapped package, an import that is neither a JDK package, nor a mapped donor
package, nor a declared EXTERNAL package, a DROP, REWRITE or INSERT row matching no line (a DROP_METHOD matching more
than one declaration), a MERGE target without a type closing brace or import block, a C file whose header is not an
SPDX block comment, a C line still naming a JNI symbol of a mapped donor package, or a begin anchor without its end.

PROVENANCE.tsv (next to this script) pins, per row, the donor commit, blob, sha256 and URL, the target sha256, the
mode, the sha256 of the host (the target without the merged block and without the imports this recipe inserted, listed
as inserted_imports: equal to the BASE row pin when the host is the planned master file, base_match), the counts of
renamed, mapped, rewritten,
dropped and inserted lines, and body_match: read back from disk, the lines between the anchors (minus the INSERT_BLOCK
lines) are exactly the donor body lines after the declared rows, in order, and every missing donor line is a declared
drop (FILE rows: every line after the header, package and anchor is a donor line after the declared rows).

Derived from m3/collections/recipe/entryless-lane-kernel-20261106/relocate.py v3 (itself derived from
m3/arrays/recipe/posting-index-20261106/relocate.py v2 and m3/collections/recipe/immutable-pools-20261106/relocate.py v2:
MERGE/APPEND/FILE, MEMBER, DROP/REWRITE/INSERT_BLOCK, REPO/BASE rows, read-back body_match). New in v4: a donor header
may also be one leading block comment whose text lines are only Copyright / SPDX-License-Identifier /
SPDX-FileCopyrightText lines ("/*", " * ...", " */", or "/* Copyright ..." opening the block); it is replaced by the
M3JDK header exactly like the "// " header lines (header_lines counts it). The family relocated by this recipe uses
three header shapes; v3 kept a block header verbatim below the M3JDK header.

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
RECIPE_DIR_REL = "m3/collections/recipe/" + RECIPE_DIR.name
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "donor_url",
                      "target_path", "target_sha256", "mode", "host_sha256", "base_match", "inserted_imports",
                      "type_renames", "member_renames", "import_rewrites", "rewritten_lines", "dropped_lines",
                      "inserted_lines", "body_match", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
C_HEADER_LINE = re.compile(rb"^/\* (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b.*\*/\s*$")
BLOCK_HEADER_TEXT = re.compile(rb"^(?:Copyright|SPDX-License-Identifier|SPDX-FileCopyrightText)\b")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(static\s+)?)([\w.]+(?:\.\*)?)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
CLOSE_LINE = re.compile(rb"^}\s*$")
INCLUDE_LINE = re.compile(rb"^(#include\s+)([<\"][^>\"]+[>\"])(.*)$")
JNI_SYMBOL = re.compile(rb"\bJava_(\w+)")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
BEGIN = b"//FLLDCIM: RELOCATE-MERGE begin"
END = b"//FLLDCIM: RELOCATE-MERGE end"
ROW_KINDS = ("REPO", "BASE", "MERGE", "APPEND", "FILE", "PACKAGE", "TYPE", "MEMBER", "EXTERNAL", "JNI", "INCLUDE",
             "DROP_LINE", "DROP_METHOD", "REWRITE", "INSERT_BLOCK", "SKIP", "NEC")
STATEMENT_KEYWORDS = rb"(?!(?:return|throw|new|else|if|for|while|do|case|break|continue)\b)"


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {kind: ([] if kind in ("MERGE", "APPEND", "FILE", "SKIP", "NEC") else {}) for kind in ROW_KINDS}
    for kind in ("DROP_LINE", "DROP_METHOD", "REWRITE", "INSERT_BLOCK"):
        mapping[kind] = {}
    for kind, donor, target, *_note in rows:
        if kind not in ROW_KINDS:
            raise Refused("unknown row kind %s in %s" % (kind, MAP_NAME))
        if kind in ("MERGE", "APPEND", "FILE"):
            mapping[kind].append((donor, target))
        elif kind in ("SKIP", "NEC"):
            mapping[kind].append((donor, target))
        elif kind == "DROP_METHOD":
            donor_type, method = donor.rsplit(".", 1)
            mapping[kind].setdefault(donor_type, []).append(method)
        elif kind in ("DROP_LINE", "REWRITE"):
            donor_type, regex = donor.split("#", 1)
            mapping[kind].setdefault(donor_type, []).append((regex, target))
        elif kind == "INSERT_BLOCK":
            mapping[kind][donor] = target
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


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def unescape(text: str) -> str:
    return text.replace("\\n", "\n")


def rename_words(line: bytes, names: dict) -> tuple[bytes, int]:
    count = 0
    for donor in sorted(names, key=len, reverse=True):
        target = names[donor]
        if donor != target:
            line, n = re.subn(rb"\b" + donor.encode() + rb"\b", target.encode(), line)
            count += n
    return line, count


def rename_jni(line: bytes, jni: dict) -> tuple[bytes, int]:
    count = 0
    for donor, target in jni.items():
        line, n = re.subn(rb"(?<![\w])" + re.escape(donor.encode()) + rb"(?=\w)", target.encode(), line)
        count += n
    return line, count


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


def donor_type_of(donor_path: str) -> str:
    return Path(donor_path).stem


def method_span(lines: list, method: str, donor_path: str) -> tuple[int, int]:
    """[start, end) of one method or constructor declaration; the declaration pattern cannot match a call."""
    pattern = re.compile(rb"^(\s*)(?:(?:public|private|protected|static|final|native|synchronized|abstract)\s+)*"
                         + STATEMENT_KEYWORDS + rb"(?:[\w<>\[\],.?]+\s+)?" + method.encode() + rb"\s*\(")
    starts = [at for at, line in enumerate(lines) if pattern.match(split_eol(line)[0])]
    if len(starts) != 1:
        raise Refused("DROP_METHOD %s: %d declarations in %s" % (method, len(starts), donor_path))
    start = starts[0]
    text = split_eol(lines[start])[0]
    if text.rstrip().endswith((b"}", b";")):
        return start, start + 1
    indent = pattern.match(text).group(1)
    for end in range(start + 1, len(lines)):
        if split_eol(lines[end])[0] == indent + b"}":
            return start, end + 1
    raise Refused("DROP_METHOD %s: no closing brace at its indentation in %s" % (method, donor_path))


def apply_drops(lines: list, donor_type: str, donor_path: str, mapping: dict) -> tuple[list, int]:
    dropped = 0
    for method in mapping["DROP_METHOD"].get(donor_type, ()):
        start, end = method_span(lines, method, donor_path)
        if end < len(lines) and split_eol(lines[end])[0].strip() == b"":
            end += 1
        dropped += end - start
        lines = lines[:start] + lines[end:]
    for regex, _reason in mapping["DROP_LINE"].get(donor_type, ()):
        compiled = re.compile(regex.encode())
        kept = [line for line in lines if not compiled.search(split_eol(line)[0])]
        if len(kept) == len(lines):
            raise Refused("DROP_LINE %s matches no line of %s" % (regex, donor_path))
        dropped += len(lines) - len(kept)
        lines = kept
    return lines, dropped


def check_rewrites(lines: list, donor_type: str, donor_path: str, mapping: dict) -> None:
    for regex, _replacement in mapping["REWRITE"].get(donor_type, ()):
        compiled = re.compile(regex.encode())
        if not any(compiled.search(split_eol(line)[0]) for line in lines):
            raise Refused("REWRITE %s matches no line of %s" % (regex, donor_path))


def transform_java_line(line: bytes, donor_type: str, mapping: dict, counts: dict) -> bytes:
    text, eol = split_eol(line)
    for regex, replacement in mapping["REWRITE"].get(donor_type, ()):
        text, n = re.subn(regex.encode(), replacement.encode(), text)
        counts["rewritten"] += 1 if n else 0
    text, n = rename_words(text, mapping["MEMBER"])
    counts["members"] += n
    text, n = rename_words(text, mapping["TYPE"])
    counts["types"] += n
    return text + eol


def transform_c_line(line: bytes, donor_type: str, donor_path: str, mapping: dict, counts: dict) -> bytes:
    text, eol = split_eol(line)
    included = INCLUDE_LINE.match(text)
    if included:
        spec = included.group(2).decode()
        if spec in mapping["INCLUDE"]:
            text = included.group(1) + mapping["INCLUDE"][spec].encode() + included.group(3)
            counts["imports"] += 1
        else:
            header = spec.strip("<>\"")
            for donor_package in mapping["PACKAGE"]:
                if header.startswith(donor_package.replace(".", "_") + "_"):
                    raise Refused("#include %s of a mapped-package header in %s without an INCLUDE row" % (spec, donor_path))
    for regex, replacement in mapping["REWRITE"].get(donor_type, ()):
        text, n = re.subn(regex.encode(), replacement.encode(), text)
        counts["rewritten"] += 1 if n else 0
    text, n = rename_jni(text, mapping["JNI"])
    counts["members"] += n
    text, n = rename_words(text, mapping["TYPE"])
    counts["types"] += n
    for symbol in JNI_SYMBOL.finditer(text):
        owner = symbol.group(1).decode()
        if any(owner.startswith(package.replace(".", "_") + "_") for package in mapping["PACKAGE"]):
            raise Refused("JNI symbol %s of a mapped donor package in %s without a JNI row" % (symbol.group(0).decode(), donor_path))
    return text + eol


def insert_block_lines(donor_type: str, mapping: dict, eol: bytes) -> list:
    text = mapping["INSERT_BLOCK"].get(donor_type)
    if text is None:
        return []
    return [part.encode() + eol for part in unescape(text).split("\n")]


def anchor_begin(donor_path: str, blob: str, commit: str, indent: bytes) -> bytes:
    """Names blob and commit only: the donor path lives in PROVENANCE.tsv (its tokens are JDK-naming rename tokens)."""
    del donor_path
    return indent + BEGIN + (" - donor blob %s at %s; recipe %s/%s %s"
                             % (blob[:12], commit[:12], RECIPE_DIR_REL, RECIPE, VERSION)).encode()


def anchor_end(donor_path: str, blob: str, indent: bytes) -> bytes:
    del donor_path
    return indent + END + (" - donor blob %s" % blob[:12]).encode()


def anchor_file(donor_path: str, blob: str, commit: str) -> bytes:
    del donor_path
    return ("//FLLDCIM: RELOCATE - donor blob %s at %s; recipe %s/%s %s"
            % (blob[:12], commit[:12], RECIPE_DIR_REL, RECIPE, VERSION)).encode()


def strip_block(lines: list, target_path: str) -> tuple[list, list]:
    """The host (target without its anchored block) and the block lines found between the anchors."""
    begins = [at for at, line in enumerate(lines) if BEGIN in line]
    ends = [at for at, line in enumerate(lines) if END in line]
    if not begins and not ends:
        return lines, []
    if len(begins) != 1 or len(ends) != 1 or ends[0] < begins[0]:
        raise Refused("anchors of %s are not one begin followed by one end" % target_path)
    first = begins[0]
    if first > 0 and split_eol(lines[first - 1])[0].strip() == b"":
        first -= 1  # the blank line the recipe puts before its begin anchor belongs to the block
    return lines[:first] + lines[ends[0] + 1:], lines[begins[0] + 1:ends[0]]


def block_header_length(lines: list) -> int:
    """v4: the number of lines of a leading block comment made only of Copyright/SPDX lines (0 when absent)."""
    if not lines or not split_eol(lines[0])[0].startswith(b"/*"):
        return 0
    for at, line in enumerate(lines):
        text = split_eol(line)[0].strip()
        closes = text.endswith(b"*/")
        inner = text[2:] if at == 0 else text
        inner = inner[:-2] if closes else inner
        inner = inner.strip().lstrip(b"*").strip()
        if inner and not BLOCK_HEADER_TEXT.match(inner):
            return 0
        if closes:
            return at + 1
    return 0


def split_donor_java(lines: list, donor_path: str) -> dict:
    index = block_header_length(lines)
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    package_at = next((at for at in range(index, len(lines)) if PACKAGE_LINE.match(split_eol(lines[at])[0])), None)
    type_at = next((at for at in range(index, len(lines)) if TYPE_LINE.match(split_eol(lines[at])[0])), None)
    close_at = max((at for at, line in enumerate(lines) if CLOSE_LINE.match(split_eol(line)[0])), default=None)
    if package_at is None or type_at is None or close_at is None or close_at <= type_at:
        raise Refused("no package line, top-level type or closing brace in %s" % donor_path)
    imports = [lines[at] for at in range(package_at + 1, type_at) if IMPORT_LINE.match(split_eol(lines[at])[0])]
    preamble = type_at - package_at - 1 - len(imports)
    return {"package": PACKAGE_LINE.match(split_eol(lines[package_at])[0]).group(1).decode(), "imports": imports,
            "preamble": preamble, "type_at": type_at, "body": lines[type_at + 1:close_at], "close_at": close_at,
            "header_lines": index, "type_line": lines[type_at]}


def mapped_import_line(line: bytes, mapping: dict, donor_path: str, counts: dict) -> bytes:
    text, eol = split_eol(line)
    imported = IMPORT_LINE.match(text)
    fqn = imported.group(3).decode()
    mapped = map_import(fqn, mapping, donor_path)
    counts["imports"] += 1 if mapped != fqn else 0
    return imported.group(1) + mapped.encode() + imported.group(4) + eol


def host_import_block(lines: list, target_path: str) -> tuple[int, int]:
    """[first, last] indexes of the host's import lines."""
    imports = [at for at, line in enumerate(lines) if IMPORT_LINE.match(split_eol(line)[0])]
    if not imports:
        raise Refused("MERGE target %s has no import block" % target_path)
    return imports[0], imports[-1]


def merge_java(target: bytes, donor: bytes, donor_path: str, target_path: str, blob: str, commit: str,
               mapping: dict, inserted_before: list) -> tuple[bytes, dict]:
    counts = {"types": 0, "members": 0, "imports": 0, "rewritten": 0, "dropped": 0, "inserted": 0}
    donor_type = donor_type_of(donor_path)
    parts = split_donor_java(LINES.findall(donor), donor_path)
    if parts["package"] not in mapping["PACKAGE"]:
        raise Refused("unmapped package %s in %s" % (parts["package"], donor_path))
    body, counts["dropped"] = apply_drops(parts["body"], donor_type, donor_path, mapping)
    check_rewrites(body, donor_type, donor_path, mapping)
    body = [transform_java_line(line, donor_type, mapping, counts) for line in body]
    host_lines, _old = strip_block(LINES.findall(target), target_path)
    eol = split_eol(host_lines[0])[1] or b"\n"
    # import union: mapped donor imports the host lacks, inserted at their sorted position in the host's block
    first, last = host_import_block(host_lines, target_path)
    present = {split_eol(line)[0] for line in host_lines[first:last + 1]}
    wanted = [mapped_import_line(line, mapping, donor_path, counts) for line in parts["imports"]]
    added = []
    for line in wanted:
        text = split_eol(line)[0]
        if text in present:
            continue
        at = next((i for i in range(first, last + 1) if split_eol(host_lines[i])[0] > text), last + 1)
        host_lines.insert(at, text + eol)
        present.add(text)
        last += 1
        added.append(IMPORT_LINE.match(text).group(3).decode())
    counts["inserted"] += len(added)
    inserted_before[:] = added
    close_at = max((at for at, line in enumerate(host_lines) if CLOSE_LINE.match(split_eol(line)[0])), default=None)
    if close_at is None:
        raise Refused("MERGE target %s has no type closing brace" % target_path)
    extra = insert_block_lines(donor_type, mapping, eol)
    counts["inserted"] += len(extra) + 2
    block = [anchor_begin(donor_path, blob, commit, b"    ") + eol, *body, *extra, anchor_end(donor_path, blob, b"    ") + eol]
    before = host_lines[:close_at]
    if before and split_eol(before[-1])[0].strip() != b"":
        block.insert(0, eol)
        counts["inserted"] += 1
    out = before + block + host_lines[close_at:]
    expected = {"body": body, "extra": extra, "preamble": parts["preamble"]}
    return b"".join(out), {"counts": counts, "expected": expected}


def append_c(target: bytes, donor: bytes, donor_path: str, target_path: str, blob: str, commit: str,
             mapping: dict) -> tuple[bytes, dict]:
    counts = {"types": 0, "members": 0, "imports": 0, "rewritten": 0, "dropped": 0, "inserted": 0}
    donor_type = donor_type_of(donor_path)
    lines = LINES.findall(donor)
    index = 0
    while index < len(lines) and C_HEADER_LINE.match(split_eol(lines[index])[0]):
        index += 1
    if index == 0:
        raise Refused("C file %s does not start with a block-comment SPDX line" % donor_path)
    body, counts["dropped"] = apply_drops(lines[index:], donor_type, donor_path, mapping)
    check_rewrites(body, donor_type, donor_path, mapping)
    body = [transform_c_line(line, donor_type, donor_path, mapping, counts) for line in body]
    host_lines, _old = strip_block(LINES.findall(target), target_path)
    eol = split_eol(host_lines[0])[1] or b"\n"
    if not split_eol(host_lines[-1])[1]:
        host_lines[-1] = host_lines[-1] + eol
    while host_lines and split_eol(host_lines[-1])[0].strip() == b"":
        host_lines.pop()
    if body and not split_eol(body[-1])[1]:
        body[-1] = body[-1] + eol
    extra = insert_block_lines(donor_type, mapping, eol)
    block = [eol, anchor_begin(donor_path, blob, commit, b"") + eol, *body, *extra, anchor_end(donor_path, blob, b"") + eol]
    counts["inserted"] += len(extra) + 3
    return b"".join(host_lines + block), {"counts": counts, "expected": {"body": body, "extra": extra, "preamble": 0}}


def relocate_file(donor: bytes, donor_path: str, blob: str, commit: str, mapping: dict) -> tuple[bytes, dict]:
    counts = {"types": 0, "members": 0, "imports": 0, "rewritten": 0, "dropped": 0, "inserted": 0}
    donor_type = donor_type_of(donor_path)
    lines = LINES.findall(donor)
    parts = split_donor_java(lines, donor_path)
    if parts["package"] not in mapping["PACKAGE"]:
        raise Refused("unmapped package %s in %s" % (parts["package"], donor_path))
    eol = split_eol(lines[0])[1] or b"\n"
    rest, counts["dropped"] = apply_drops(lines[parts["header_lines"]:], donor_type, donor_path, mapping)
    check_rewrites(rest, donor_type, donor_path, mapping)
    out = [text + eol for text in HEADER]
    expected = []
    anchored = False
    for line in rest:
        text, line_eol = split_eol(line)
        if PACKAGE_LINE.match(text):
            out.append(b"package " + mapping["PACKAGE"][parts["package"]].encode() + b";" + line_eol)
            continue
        if IMPORT_LINE.match(text):
            line = mapped_import_line(line, mapping, donor_path, counts)
        elif not anchored and TYPE_LINE.match(text):
            out.append(anchor_file(donor_path, blob, commit) + line_eol)
            anchored = True
        line = transform_java_line(line, donor_type, mapping, counts)
        out.append(line)
        expected.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    counts["inserted"] = 1
    return b"".join(out), {"counts": counts, "expected": {"body": expected, "extra": [], "preamble": 0}}


def read_back(data: bytes, mode: str, expected: dict, target_path: str) -> tuple[bool, list]:
    """body_match from the bytes on disk, and the host lines (merged modes) for the host pin."""
    lines = LINES.findall(data)
    if mode == "FILE":
        body = [line for line in lines[len(HEADER):] if not line.startswith(b"//FLLDCIM: RELOCATE")
                and not PACKAGE_LINE.match(split_eol(line)[0])]
        return body == expected["body"], []
    host, block = strip_block(lines, target_path)
    return block == expected["body"] + expected["extra"], host


def recipe_import(line: bytes, recipe_imports: list) -> bool:
    """True for an import line of one of the (fully qualified) imports this recipe inserted into the host."""
    imported = IMPORT_LINE.match(split_eol(line)[0])
    return bool(imported) and imported.group(3).decode() in recipe_imports


def previous_imports(recipe_dir: Path) -> dict:
    """target_path -> the import lines an earlier run of this recipe inserted (its own PROVENANCE.tsv)."""
    path = recipe_dir / PROVENANCE_NAME
    if not path.exists():
        return {}
    rows = [line.split("	") for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]
    if not rows or "inserted_imports" not in rows[0]:
        return {}
    target_at, imports_at = rows[0].index("target_path"), rows[0].index("inserted_imports")
    return {row[target_at]: [part for part in row[imports_at].split(";") if part and part != "-"] for row in rows[1:]}


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
    repo = next(iter(mapping["REPO"]), "")
    url = mapping["REPO"].get(repo, "")
    rows, changed, matched = [], 0, 0
    earlier = previous_imports(recipe_dir)
    jobs = [("MERGE", d, t) for d, t in mapping["MERGE"]] + [("APPEND", d, t) for d, t in mapping["APPEND"]] \
        + [("FILE", d, t) for d, t in mapping["FILE"]]
    for mode, donor_path, target in jobs:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        path = module / target
        inserted_imports: list = []
        if mode == "MERGE":
            relocated, info = merge_java(path.read_bytes(), data, donor_path, target, blob, commit, mapping, inserted_imports)
        elif mode == "APPEND":
            relocated, info = append_c(path.read_bytes(), data, donor_path, target, blob, commit, mapping)
        else:
            relocated, info = relocate_file(data, donor_path, blob, commit, mapping)
        state = write_if_changed(path, relocated, check)
        changed += state
        on_disk = relocated if check and state else path.read_bytes()
        body_match, host = read_back(on_disk, mode, info["expected"], target)
        disk_sha = sha256(on_disk)
        recipe_imports = sorted(set(earlier.get(target, [])) | set(inserted_imports))
        counts = info["counts"]
        if mode == "FILE":
            host_sha, base_match = "-", "-"
        else:
            host_without = [line for line in host if not recipe_import(line, recipe_imports)] if mode == "MERGE" else host
            host_sha = sha256(b"".join(host_without))
            pin = mapping["BASE"].get(target)
            base_match = "-" if pin is None else str(pin == host_sha).upper()
            # structural count, stable across runs: recipe imports + blank + begin anchor + INSERT_BLOCK + end anchor
            counts["inserted"] = len(recipe_imports) + 3 + len(info["expected"]["extra"])
            counts["imports"] = len(recipe_imports) if mode == "MERGE" else counts["imports"]
        matched += body_match
        print("%s %s body_match=%s base_match=%s dropped=%d inserted=%d %s" % (
            mode, "CHANGED" if state else "UNCHANGED", str(body_match).upper(), base_match, counts["dropped"],
            counts["inserted"], target))
        rows.append((repo, commit, donor_path, blob, sha256(data), url + "/blob/" + commit + "/" + donor_path, target,
                     disk_sha, mode, host_sha, base_match, ";".join(recipe_imports) or "-", str(counts["types"]),
                     str(counts["members"]), str(counts["imports"]), str(counts["rewritten"]), str(counts["dropped"]),
                     str(counts["inserted"]), str(body_match).upper(), RECIPE + " " + VERSION))
    rows.sort(key=lambda row: (row[8], row[6]))
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s/%s %s donor_commit=%s rows=%d changed=%d body_match=%d/%d skips=%d nec=%d provenance=%s" % (
        RECIPE_DIR_REL, RECIPE, VERSION, commit, len(rows), changed, matched, len(rows), len(mapping["SKIP"]),
        len(mapping["NEC"]), "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (commit, FETCH_HEAD, branch)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent), help="module root (default m3/collections)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
