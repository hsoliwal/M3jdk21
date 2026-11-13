#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Relocation recipe RELOCATE_ATOM_JIT_GATE v4 (recipe-first port, never by hand).

Copies every FILE row of RELOCATION_MAP.tsv (next to this script) from a donor git tree (--donor-git DIR at
--donor-rev REV) into the module (--module DIR, default: the grandparent of this recipe directory,
make/m3/tooling/atom-jit-gate).

Every file first takes its REWRITE rows: a declared adapter change, rewrites/<name>.before replaced by
rewrites/<name>.after on the donor bytes, refused unless the before bytes occur exactly the declared number of times
(<name> or <name>=<count>, default 1). Only adapter changes are REWRITE rows; everything else is a rename row.

Java files (.java, and donor .java.txt snapshots): rewrites only (1) the leading license header (// lines or one
/* ... */ block that names Copyright or SPDX), replaced by the M3JDK header, (2) the package line, through the PACKAGE
rows, (3) import lines of mapped packages and types, through the PACKAGE and TYPE rows (an import of a nested member
maps its longest mapped package prefix and the type that follows it), (4) the TYPE rows whose donor and target names
differ, and the SCOPED_TYPE rows of that one file (donor column <donor path>#<Type>), as whole-word tokens, (5) the
LITERAL rows, declared string-literal tokens renamed as whole tokens, (6) the DROP rows: a declared donor line
(pinned by the sha256 of its bytes) removed from a named file, refused unless it occurs exactly once, and (7) one
//FLLDCIM: anchor line inserted before the top-level type declaration, naming the donor blob URL at the donor commit.

C and C++ files (.c, .h, .cpp): rewrites only (1) the first line, a block-comment or line-comment SPDX line, replaced
by the M3JDK header in the same comment style, (2) #include lines through the INCLUDE rows, (3) the JNI rows (symbol
or javac -h header-name prefixes, as whole-token prefixes), (4) the TYPE and LITERAL rows, and (5) one //FLLDCIM:
anchor line inserted before the first #include.

Python files (.py): rewrites only (1) the license comment lines after an optional #! line, replaced by the M3JDK
header, (2) the TYPE, LITERAL and JNI rows, and (3) one # FLLDCIM: anchor line after the header.

Last stage of every file: the JDK naming rules (make/m3/tooling/jdk-naming/jdk_naming.py rewrite_text, the one owner of
NAMING_RULES.tsv) are applied to the relocated text, so this recipe and jdk_naming.py share one fixed point by
construction; the run log and PROVENANCE.tsv count each naming rewrite (comment-line PROSE renames such as the
product name, constant renames such as SYNEXIA_ -> M3_ inside a validated schema token).

NAMING rows record the schema-relevant renames of that stage (donor token -> target token); the recipe checks each
donor token occurs in a donor FILE, the target token in a relocated file, and the donor token in no relocated file.

SEAL rows regenerate a donor source-custody manifest (name TAB sha256 per line) for the relocated files of the
manifest's sibling fixtures/ directory, in the donor manifest's order and with the same name set (refused otherwise).
NEC rows name donor files whose equivalence with an existing M3JDK owner is NOT decided (never two owners of one
shape); OMIT rows name donor files left out by decision. Both are informational: their donor blob is pinned in the
run log (HELD lines) and nothing is copied.

The module registration is part of the recipe too: REGISTRATION (below) is the module's GitHub workflow, written to
.github/workflows/<name> of the repository once (idempotent, --check reports it), the way make/m3/tooling/atom-census is
registered (a standalone pom.xml with its own workflow, not a module of the make/m3/pom.xml reactor).

PROVENANCE.tsv pins donor commit, blob sha, donor sha256 and target sha256 of every FILE row and states body_match
(the bytes after the header/package line, minus the anchor, equal the donor's), body_match_renamed (they equal the
donor's after the REWRITE, TYPE, SCOPED_TYPE, LITERAL, JNI, INCLUDE, DROP rows and the naming stage), declared_drops,
declared_rewrites and naming_rewrites.

Idempotent: writes a file only when its bytes differ; a second run reports changed=0. Refuses (exit 2) an unmapped
package, an import that is neither a JDK package, nor a mapped donor package, nor a declared EXTERNAL package, a Java
file without a top-level type, a C file without an SPDX first line or #include, a REWRITE whose before bytes occur
other than the declared number of times, a DROP row whose line is absent or ambiguous, a LITERAL row whose donor
token is absent from every FILE row, or a SEAL row whose names differ from the relocated fixtures.

Derived from m3/arrays/recipe/bitmatrix-20261106/relocate.py v3 (RELOCATE_BIT_MATRIX): REWRITE, SCOPED_TYPE and SEAL
rows, Python and line-comment C/C++ handlers, block-comment Java headers, the naming stage and a workflow
registration instead of the CMake block added; everything else unchanged.

usage: python -I relocate.py --donor-git DIR --donor-rev REV [--module DIR] [--check]
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "relocate.py"
VERSION = "v4"
RECIPE_DIR = Path(__file__).resolve().parent
RECIPE_PATH = "make/m3/tooling/atom-jit-gate/recipe/" + RECIPE_DIR.name + "/" + RECIPE
DONOR_REPO = "hsoliwal/com.synexia"
DONOR_BLOB_URL = "https://github.com/" + DONOR_REPO + "/blob/"
MAP_NAME = "RELOCATION_MAP.tsv"
PROVENANCE_NAME = "PROVENANCE.tsv"
REWRITES = "rewrites"
PROVENANCE_COLUMNS = ("donor_repo", "donor_commit", "donor_path", "donor_blob", "donor_sha256", "target_path",
                      "target_sha256", "body_match", "body_match_renamed", "declared_drops", "declared_rewrites",
                      "naming_rewrites", "recipe")
HEADER = (b"// Copyright 2026 Hitesh Soliwal and contributors", b"// SPDX-License-Identifier: Apache-2.0")
C_HEADER = (b"/* Copyright 2026 Hitesh Soliwal and contributors", b" * SPDX-License-Identifier: Apache-2.0")
PY_HEADER = (b"# Copyright 2026 Hitesh Soliwal and contributors", b"# SPDX-License-Identifier: Apache-2.0")
HEADER_LINE = re.compile(rb"^// (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
PY_HEADER_LINE = re.compile(rb"^# (?:SPDX-License-Identifier|SPDX-FileCopyrightText|Copyright)\b")
C_HEADER_LINE = re.compile(rb"^/\* SPDX-License-Identifier: [\w.-]+\s*$")
C_LINE_HEADER_LINE = re.compile(rb"^// SPDX-License-Identifier: [\w.-]+\s*$")
BLOCK_START = re.compile(rb"^/\*")
BLOCK_END = re.compile(rb"\*/\s*$")
LICENSE_WORD = re.compile(rb"Copyright|SPDX-")
PACKAGE_LINE = re.compile(rb"^package\s+([\w.]+)\s*;")
IMPORT_LINE = re.compile(rb"^(import\s+(?:static\s+)?)([\w.]+)(\s*;.*)$")
TYPE_LINE = re.compile(rb"^(?:(?:public|final|abstract|sealed|non-sealed|strictfp)\s+)*(?:class|interface|enum|record)\s+\w+")
INCLUDE_LINE = re.compile(rb"^(#include\s+)([<\"][^>\"]+[>\"])(.*)$")
JNI_SYMBOL = re.compile(rb"\bJava_(\w+)")
JDK_PREFIXES = ("java.", "javax.", "jdk.")
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
HELD_KINDS = ("NEC", "OMIT")
NAMING_TOOL = "make/m3/tooling/jdk-naming/jdk_naming.py"
WORKFLOW_PATH = ".github/workflows/m3-tooling-atom-jit-gate-20261106.yml"
REGISTRATION = b"""# SPDX-License-Identifier: Apache-2.0
# Registration of make/m3/tooling/atom-jit-gate (written by make/m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py,
# the way make/m3/tooling/atom-census is registered: a standalone pom.xml with its own workflow).
name: M3JDK21 per-atom JIT retest gate (tooling)
on:
  pull_request:
    paths:
      - 'make/m3/tooling/atom-jit-gate/**'
      - 'make/m3/tooling/atom-census/**'
      - '.github/workflows/m3-tooling-atom-jit-gate-20261106.yml'
  push:
    paths:
      - 'make/m3/tooling/atom-jit-gate/**'
      - 'make/m3/tooling/atom-census/**'
      - '.github/workflows/m3-tooling-atom-jit-gate-20261106.yml'
permissions:
  contents: read
jobs:
  atom-jit-gate:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@11d5960a326750d5838078e36cf38b85af677262
        with:
          persist-credentials: false
      - uses: actions/setup-java@cf277c60eb25467037889841efdb72551f06f6c3
        with:
          distribution: temurin
          java-version: '21'
      - name: Install the atom census the gate atomizes with
        run: mvn -B -ntp -q -f make/m3/tooling/atom-census/pom.xml -DskipTests install
      - name: Package the gate (-Xlint:all -Werror) and run its JUnit tests
        run: mvn -B -ntp -f make/m3/tooling/atom-jit-gate/pom.xml package
      - name: Fixture gate (javac -h, C17/C++20 JNI, nm exports, four JVM modes, negative control)
        shell: bash
        run: |
          set -euo pipefail
          gate=make/m3/tooling/atom-jit-gate/fixture-gate
          python3 -I "$gate/verify.py" --evidence "$RUNNER_TEMP/atom-jit-fixture" --timeout-seconds 120
          python3 -I "$gate/verify.py" --evidence "$RUNNER_TEMP/atom-jit-negative" --timeout-seconds 120 --negative-test
          (cd "$gate" && python3 -m unittest -q test_verify)
      - name: Explicit product hold
        run: echo 'TOOLING_CI_DOES_NOT_PROVE_FULL_JDK21_JTREG_IMAGE_C1_C2_JNI_ABI_CROSS_PLATFORM'
"""


class Refused(Exception):
    pass


def read_map(recipe_dir: Path) -> dict:
    rows = [line.split("\t") for line in (recipe_dir / MAP_NAME).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#") and not line.startswith("kind\t")]
    mapping: dict = {"FILE": [], "PACKAGE": {}, "TYPE": {}, "SCOPED_TYPE": {}, "EXTERNAL": {}, "JNI": {},
                     "INCLUDE": {}, "DROP": [], "LITERAL": {}, "REWRITE": [], "SEAL": [], "NAMING": [], "HELD": []}
    for kind, donor, target, *note in rows:
        if kind in ("FILE", "DROP", "REWRITE", "SEAL", "NAMING"):
            mapping[kind].append((donor, target))
        elif kind == "SCOPED_TYPE":
            path, _hash, simple = donor.rpartition("#")
            mapping["SCOPED_TYPE"].setdefault(path, {})[simple] = target
        elif kind in HELD_KINDS:
            mapping["HELD"].append((kind, donor, target, note[0] if note else ""))
        else:
            mapping[kind][donor] = target
    return mapping


def git(donor: Path, *args: str) -> bytes:
    return subprocess.run(["git", "-C", str(donor), *args], check=True, capture_output=True).stdout


def naming_tool(module: Path):
    """The JDK naming recipe of the repository (one owner of NAMING_RULES.tsv), found upward from the module."""
    for root in (module.resolve(), *module.resolve().parents):
        tool = root / NAMING_TOOL
        if tool.is_file():
            spec = importlib.util.spec_from_file_location("jdk_naming", tool)
            loaded = importlib.util.module_from_spec(spec)
            spec.loader.exec_module(loaded)
            return root, loaded
    raise Refused("naming tool %s not found above %s" % (NAMING_TOOL, module))


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


def rename_jni(line: bytes, jni: dict) -> bytes:
    for donor, target in jni.items():
        line = re.sub(rb"(?<![\w])" + re.escape(donor.encode()) + rb"(?=\w)", target.encode(), line)
    return line


def file_types(mapping: dict, donor_path: str) -> dict:
    return {**mapping["TYPE"], **mapping["SCOPED_TYPE"].get(donor_path, {})}


def map_import(fqn: str, mapping: dict, types: dict, donor_path: str) -> str:
    parts = fqn.split(".")
    for cut in range(len(parts) - 1, 0, -1):
        owner = ".".join(parts[:cut])
        if owner in mapping["PACKAGE"]:
            simple = parts[cut]
            return ".".join([mapping["PACKAGE"][owner], types.get(simple, simple), *parts[cut + 1:]])
        if owner in mapping["EXTERNAL"]:
            return fqn
    if fqn.startswith(JDK_PREFIXES):
        return fqn
    raise Refused("import of an unmapped type %s in %s (port the closure: add the file or an EXTERNAL row to %s)"
                  % (fqn, donor_path, MAP_NAME))


def anchor(prefix: bytes, donor_path: str, blob: str, commit: str) -> bytes:
    return prefix + ("FLLDCIM: RELOCATE - donor %s%s/%s blob %s; recipe %s %s"
                     % (DONOR_BLOB_URL, commit, donor_path, blob[:12], RECIPE_PATH, VERSION)).encode()


def drops_for(donor_path: str, mapping: dict) -> set:
    return {line_sha for path, line_sha in mapping["DROP"] if path == donor_path}


def apply_drops(lines: list, donor_path: str, drops: set) -> tuple[list, int]:
    """Removes every declared line (sha256 of its bytes without the end of line); refuses unless each occurs once."""
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


def apply_rewrites(data: bytes, donor_path: str, mapping: dict, recipe_dir: Path) -> tuple[bytes, int]:
    """Declared adapter changes on the donor bytes: before -> after, exactly the declared number of occurrences."""
    applied = 0
    for path, spec in mapping["REWRITE"]:
        if path != donor_path:
            continue
        name, _eq, count = spec.partition("=")
        expected = int(count) if count else 1
        before = (recipe_dir / REWRITES / (name + ".before")).read_bytes()
        after = (recipe_dir / REWRITES / (name + ".after")).read_bytes()
        found = data.count(before)
        if not before or found != expected:
            raise Refused("REWRITE %s of %s matched %d times, expected %d" % (name, donor_path, found, expected))
        data = data.replace(before, after)
        applied += found
    return data, applied


def relocate_java(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                  seen: dict) -> tuple[bytes, bytes, bytes, int]:
    lines = LINES.findall(data)
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    index = 0
    if lines and BLOCK_START.match(lines[0]):
        end = next((at for at, line in enumerate(lines) if BLOCK_END.search(split_eol(line)[0])), -1)
        if end >= 0 and any(LICENSE_WORD.search(line) for line in lines[:end + 1]):
            index = end + 1
    while index < len(lines) and HEADER_LINE.match(lines[index]):
        index += 1
    types = file_types(mapping, donor_path)
    out = [text + first_eol for text in HEADER]
    target_body: list = []
    anchored = False
    in_body = False
    drops = drops_for(donor_path, mapping)
    body_lines, dropped = apply_drops(lines[index:], donor_path, drops)

    def mapped(line: bytes, counts: dict) -> bytes:
        text, eol = split_eol(line)
        imported = IMPORT_LINE.match(text)
        if imported:
            line = imported.group(1) + map_import(imported.group(2).decode(), mapping, types, donor_path).encode() \
                + imported.group(3) + eol
        return rename_literals(rename_types(line, types), mapping["LITERAL"], counts)

    donor_body: list = []
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
        if not anchored and TYPE_LINE.match(text):
            out.append(anchor(b"//", donor_path, blob, commit) + eol)
            anchored = True
        line = mapped(line, seen)
        out.append(line)
        if in_body:
            target_body.append(line)
    if not anchored:
        raise Refused("no top-level type in %s" % donor_path)
    after_package = next((at for at, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
    raw_body = b"".join(lines[after_package + 1:])
    renamed = b"".join(mapped(line, {}) for line in body_lines[next(
        (at for at, line in enumerate(body_lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(body_lines)) + 1:])
    return b"".join(out), raw_body, renamed, dropped


def map_include(spec: bytes, mapping: dict, donor_path: str) -> bytes:
    key = spec.decode()
    if key in mapping["INCLUDE"]:
        return mapping["INCLUDE"][key].encode()
    header = key.strip("<>\"")
    for donor_package in mapping["PACKAGE"]:
        if header.startswith(donor_package.replace(".", "_") + "_"):
            raise Refused("#include %s of a mapped-package header in %s without an INCLUDE row" % (key, donor_path))
    return spec


def relocate_c(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
               seen: dict) -> tuple[bytes, bytes, bytes, int]:
    lines = LINES.findall(data)
    first = split_eol(lines[0])[0] if lines else b""
    if C_HEADER_LINE.match(first):
        header = C_HEADER
    elif C_LINE_HEADER_LINE.match(first):
        header = HEADER
    else:
        raise Refused("C file %s does not start with an SPDX comment line" % donor_path)
    first_eol = split_eol(lines[0])[1] or b"\n"
    types = file_types(mapping, donor_path)
    out = [text + first_eol for text in header]
    target_body = []
    anchored = False
    drops = drops_for(donor_path, mapping)
    body_lines, dropped = apply_drops(lines[1:], donor_path, drops)

    def mapped(line: bytes, counts: dict) -> bytes:
        text, eol = split_eol(line)
        included = INCLUDE_LINE.match(text)
        if included:
            line = included.group(1) + map_include(included.group(2), mapping, donor_path) + included.group(3) + eol
        return rename_literals(rename_types(rename_jni(line, mapping["JNI"]), types), mapping["LITERAL"], counts)

    for line in body_lines:
        text, eol = split_eol(line)
        if not anchored and INCLUDE_LINE.match(text):
            out.append(anchor(b"//", donor_path, blob, commit) + eol)
            anchored = True
        line = mapped(line, seen)
        for symbol in JNI_SYMBOL.finditer(split_eol(line)[0]):
            owner = symbol.group(1).decode()
            if any(owner.startswith(package.replace(".", "_") + "_") for package in mapping["PACKAGE"]):
                raise Refused("JNI symbol %s of a mapped donor package in %s without a JNI row"
                              % (symbol.group(0).decode(), donor_path))
        out.append(line)
        target_body.append(line)
    if not anchored:
        raise Refused("no #include in %s" % donor_path)
    return b"".join(out), b"".join(lines[1:]), b"".join(mapped(line, {}) for line in body_lines), dropped


def relocate_py(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
                seen: dict) -> tuple[bytes, bytes, bytes, int]:
    lines = LINES.findall(data)
    index = 1 if lines and lines[0].startswith(b"#!") else 0
    out = lines[:index]
    first_eol = split_eol(lines[0])[1] if lines else b"\n"
    start = index
    while index < len(lines) and PY_HEADER_LINE.match(lines[index]):
        index += 1
    if index == start:
        raise Refused("Python file %s has no license comment line" % donor_path)
    types = file_types(mapping, donor_path)
    out += [text + first_eol for text in PY_HEADER] + [anchor(b"# ", donor_path, blob, commit) + first_eol]
    body_lines, dropped = apply_drops(lines[index:], donor_path, drops_for(donor_path, mapping))

    def mapped(line: bytes, counts: dict) -> bytes:
        return rename_literals(rename_types(rename_jni(line, mapping["JNI"]), types), mapping["LITERAL"], counts)

    out += [mapped(line, seen) for line in body_lines]
    return b"".join(out), b"".join(lines[index:]), b"".join(mapped(line, {}) for line in body_lines), dropped


def relocate(data: bytes, donor_path: str, blob: str, commit: str, mapping: dict,
             seen: dict) -> tuple[bytes, bytes, bytes, int]:
    if donor_path.endswith((".c", ".h", ".cpp")):
        return relocate_c(data, donor_path, blob, commit, mapping, seen)
    if donor_path.endswith(".py"):
        return relocate_py(data, donor_path, blob, commit, mapping, seen)
    if donor_path.endswith((".java", ".java.txt")):
        return relocate_java(data, donor_path, blob, commit, mapping, seen)
    raise Refused("no handler for %s" % donor_path)


def body_of(relocated: bytes, donor_path: str) -> bytes:
    """The relocated bytes after the header (and package line), without the anchor line: the comparable body."""
    lines = [line for line in LINES.findall(relocated) if b"FLLDCIM: RELOCATE - donor " not in line]
    if donor_path.endswith((".java", ".java.txt")):
        at = next((i for i, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
        return b"".join(lines[at + 1:])
    skip = 1 if lines and lines[0].startswith(b"#!") else 0
    return b"".join(lines[skip + 2:])


def donor_body(data: bytes, donor_path: str) -> bytes:
    """The donor bytes after its header (and package line): what body_match compares, before any declared row."""
    lines = LINES.findall(data)
    if donor_path.endswith((".java", ".java.txt")):
        at = next((i for i, line in enumerate(lines) if PACKAGE_LINE.match(split_eol(line)[0])), len(lines))
        return b"".join(lines[at + 1:])
    if donor_path.endswith(".py"):
        at = 1 if lines and lines[0].startswith(b"#!") else 0
        while at < len(lines) and PY_HEADER_LINE.match(lines[at]):
            at += 1
        return b"".join(lines[at:])
    return b"".join(lines[1:])


def name(module: Path, tool, data: bytes) -> tuple[bytes, dict]:
    text = tool.decoded(data)
    renamed, counts = tool.rewrite_text(text)
    return tool.encoded(renamed), counts


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def write_if_changed(path: Path, data: bytes, check: bool) -> bool:
    if path.exists() and path.read_bytes() == data:
        return False
    if not check:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
    return True


def seal(module: Path, donor: Path, rev: str, mapping: dict, relocated: dict, check: bool) -> int:
    """Regenerates each SEAL manifest from the relocated bytes of its fixtures/, in the donor manifest's order."""
    changed = 0
    for donor_manifest, target_manifest in mapping["SEAL"]:
        names = [line.split("\t")[0] for line in git(donor, "show", rev + ":" + donor_manifest).decode().splitlines()
                 if line and not line.startswith("#")]
        fixtures = str(Path(target_manifest).parent / "fixtures").replace("\\", "/") + "/"
        sealed = {Path(target).name: data for target, data in relocated.items() if target.startswith(fixtures)}
        if sorted(names) != sorted(sealed):
            raise Refused("SEAL %s names %s, relocated fixtures %s" % (target_manifest, names, sorted(sealed)))
        manifest = "".join("%s\t%s\n" % (fixture, sha256(sealed[fixture])) for fixture in names).encode()
        state = write_if_changed(module / target_manifest, manifest, check)
        changed += state
        print("SEAL %s %s fixtures=%d" % ("CHANGED" if state else "UNCHANGED", target_manifest, len(names)))
    return changed


def run(module: Path, recipe_dir: Path, donor: Path, rev: str, check: bool) -> int:
    mapping = read_map(recipe_dir)
    root, tool = naming_tool(module)
    commit = git(donor, "rev-parse", rev + "^{commit}").decode().strip()
    rows, changed, seen, relocated_files, donor_files = [], 0, {}, {}, {}
    for donor_path, target in mapping["FILE"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        data = git(donor, "cat-file", "blob", blob)
        donor_files[donor_path] = data
        rewritten, rewrites = apply_rewrites(data, donor_path, mapping, recipe_dir)
        relocated, _rewritten_body, renamed_body, dropped = relocate(rewritten, donor_path, blob, commit, mapping, seen)
        relocated, counts = name(module, tool, relocated)
        renamed_body, _ = name(module, tool, renamed_body)
        target_body = body_of(relocated, donor_path)
        body_match, body_match_renamed = donor_body(data, donor_path) == target_body, renamed_body == target_body
        naming = sum(counts.values())
        relocated_files[target] = relocated
        state = write_if_changed(module / target, relocated, check)
        changed += state
        print("FILE %s body_match=%s body_match_renamed=%s declared_drops=%d declared_rewrites=%d naming=%s %s" % (
            "CHANGED" if state else "UNCHANGED", str(body_match).upper(), str(body_match_renamed).upper(), dropped,
            rewrites, ",".join("%s:%d" % item for item in sorted(counts.items())) or "0", target))
        rows.append((DONOR_REPO, commit, donor_path, blob, sha256(data), target, sha256(relocated),
                     str(body_match).upper(), str(body_match_renamed).upper(), str(dropped), str(rewrites),
                     str(naming), RECIPE + " " + VERSION))
    for donor_literal, count in seen.items():
        if count == 0:
            raise Refused("LITERAL row %s matched no token in any FILE row" % donor_literal)
        print("LITERAL %s -> %s occurrences=%d" % (donor_literal, mapping["LITERAL"][donor_literal], count))
    changed += seal(module, donor, rev, mapping, relocated_files, check)
    for donor_token, target_token in mapping["NAMING"]:
        before = sum(donor_token.encode() in data for data in donor_files.values())
        after = sum(target_token.encode() in data for data in relocated_files.values())
        left = sum(donor_token.encode() in data for data in relocated_files.values())
        if not before or not after or left:
            raise Refused("NAMING row %s -> %s: donor files %d, relocated files %d, left unrenamed %d"
                          % (donor_token, target_token, before, after, left))
        print("NAMING %s -> %s donor_files=%d relocated_files=%d" % (donor_token, target_token, before, after))
    for kind, donor_path, target, note in mapping["HELD"]:
        blob = git(donor, "rev-parse", rev + ":" + donor_path).decode().strip()
        print("HELD %s %s blob=%s target=%s %s" % (kind, donor_path, blob[:12], target, note[:120]))
    registered = write_if_changed(root / WORKFLOW_PATH, REGISTRATION, check)
    changed += registered
    print("BUILD %s %s (%s)" % ("CHANGED" if registered else "UNCHANGED", WORKFLOW_PATH,
                                "registration written" if registered else "registration present"))
    rows.sort(key=lambda row: row[5])
    provenance = "\n".join(["\t".join(PROVENANCE_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    provenance_state = write_if_changed(recipe_dir / PROVENANCE_NAME, provenance.encode(), check)
    print("RELOCATE recipe=%s %s donor_commit=%s files=%d held=%d changed=%d provenance=%s" % (
        RECIPE_PATH, VERSION, commit, len(rows), len(mapping["HELD"]), changed,
        "CHANGED" if provenance_state else "UNCHANGED"))
    return 1 if check and (changed or provenance_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True, help="donor git directory (a blobless clone is enough)")
    parser.add_argument("--donor-rev", required=True, help="donor revision (pin the commit sha, not FETCH_HEAD)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parent.parent),
                        help="module root (default make/m3/tooling/atom-jit-gate)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.module), RECIPE_DIR, Path(args.donor_git), args.donor_rev, args.check)
    except Refused as refused:
        print("REFUSED: " + str(refused))
        return 2


if __name__ == "__main__":
    sys.exit(main())
