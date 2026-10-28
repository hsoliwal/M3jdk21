#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail closed on newly introduced noncanonical public JDK names and artifacts.

Existing product source is grandfathered, not relabeled or automatically renamed.
Canonical migration ownership remains m3/docs/name-mapping.json; Synexia keeps
recipe names, candidate inventories, and chronological evidence.
"""
from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
import re
import subprocess
import sys

SHA = re.compile(r"[0-9a-f]{40}\Z")
DATE = re.compile(r"(?:^|[-_.])20\d{6}(?:$|[-_.])")
VERSION = re.compile(r"(?:[-_.]v\d+|V\d+)(?=\.|$)", re.IGNORECASE)
TYPE = re.compile(r"\b(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)\b")
PACKAGE = re.compile(r"(?m)^\s*package\s+([a-zA-Z_$][\w$]*(?:\.[a-zA-Z_$][\w$]*)*)\s*;")
PUBLIC = re.compile(r"\bpublic\s+(?:(?:abstract|final|sealed|non-sealed)\s+)*"
                    r"(?:class|interface|enum|record)\s+([A-Za-z_$][\w$]*)\b")
JDK_SOURCE = re.compile(r"^src/[^/]+/(?:share|unix|linux|windows|macosx|bsd)/classes/(java|javax)/")
RECIPE_ROOTS = (
    "synexia-openrewrite-recipes/",
    "synexia-m3-recipe/",
    "m3/tooling/migration-recipes/",
)
RECIPE_DESCRIPTOR = re.compile(r"(?:^|/)META-INF/rewrite/[^/]+\.ya?ml$")
RECIPE_JAVA = re.compile(r"(?:^|/)(?:rewrite|recipes?)/[^/]*Recipe(?:Test)?\.java$")



# Passive source/evidence data is neither a public Java name nor an executable
# recipe. Strictly scope the historical path exception to existing data lanes.
DATA_RECEIPT_PREFIXES = ("m3/qualification/", "m3/synexia-import/current-full-borrow/")
DATA_RECEIPT_EXTENSIONS = (".json", ".tsv")


def passive_data_receipt(path: str) -> bool:
    return (path.startswith(DATA_RECEIPT_PREFIXES)
            and path.endswith(DATA_RECEIPT_EXTENSIONS))



JDK_PRODUCT_SOURCE = re.compile(
    r"^src/(?!m3[.])[^/]+/(?:share|unix|linux|windows|macosx|bsd)/classes/.+\.java$"
)
DONOR_REFERENCE = re.compile(
    r"(?<![A-Za-z0-9_$.])(?:com\.synexia|com\.m3|com\.google\.common|"
    r"org\.eclipse\.collections|org\.apache\.commons\.collections4|"
    r"org\.springframework)(?:\.[A-Za-z_$][\w$]*)+"
)


def code_without_literals(source: str) -> str:
    """Mask comments, Java strings, char literals and text blocks."""
    output = []
    state = "code"
    position = 0
    while position < len(source):
        if state == "code":
            for prefix, next_state in (("//", "line"), ("/*", "block"), ('"""', "text")):
                if source.startswith(prefix, position):
                    output.extend(" " * len(prefix))
                    position += len(prefix)
                    state = next_state
                    break
            else:
                character = source[position]
                if character in ('"', "'"):
                    output.append(" ")
                    position += 1
                    state = "string" if character == '"' else "char"
                else:
                    output.append(character)
                    position += 1
            continue
        if state == "line" and source[position] == "\n":
            output.append("\n")
            position += 1
            state = "code"
            continue
        if state == "block" and source.startswith("*/", position):
            output.extend("  ")
            position += 2
            state = "code"
            continue
        if state == "text" and source.startswith('"""', position):
            output.extend("   ")
            position += 3
            state = "code"
            continue
        if state in ("string", "char"):
            quote = '"' if state == "string" else "'"
            if source[position] == "\\" and position + 1 < len(source):
                output.extend("\n" if ch == "\n" else " "
                              for ch in source[position:position + 2])
                position += 2
                continue
            if source[position] == quote:
                output.append(" ")
                position += 1
                state = "code"
                continue
        output.append("\n" if source[position] == "\n" else " ")
        position += 1
    return "".join(output)


def donor_references(source: str) -> set[str]:
    """Lexical preflight only; attributed Java/JNI closure remains mandatory."""
    return set(DONOR_REFERENCE.findall(code_without_literals(source)))


@dataclass(frozen=True)
class Change:
    status: str
    path: str
    previous: str | None = None


def git(root: Path, *args: str) -> bytes:
    result = subprocess.run(["git", *args], cwd=root, stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE, check=False)
    if result.returncode:
        raise ValueError("git request refused: " + result.stderr.decode("utf-8", "replace").strip())
    return result.stdout


def changes(root: Path, base: str, head: str) -> list[Change]:
    if not SHA.fullmatch(base) or not SHA.fullmatch(head):
        raise ValueError("base and head require exact 40-character commit IDs")
    parts = git(root, "diff", "--no-ext-diff", "--name-status", "-z",
                "--find-renames", "--diff-filter=ACMRT", base + "..." + head).split(b"\0")
    if parts[-1] != b"":
        raise ValueError("incomplete NUL-delimited git diff")
    rows = []
    i = 0
    while i < len(parts) - 1:
        status = parts[i].decode("ascii")
        i += 1
        if not re.fullmatch(r"[ACMRT](?:\d{1,3})?", status):
            raise ValueError("unexpected diff operation")
        previous = None
        if status.startswith(("R", "C")):
            previous = parts[i].decode("utf-8")
            i += 1
        name = parts[i].decode("utf-8")
        i += 1
        path = PurePosixPath(name)
        if path.is_absolute() or ".." in path.parts or "\\" in name or not name:
            raise ValueError("unsafe changed path")
        rows.append(Change(status, name, previous))
    return rows


def problems(change: Change, text: str | None = None,
             baseline: str | None = None) -> list[str]:
    path = change.path
    added = change.status.startswith(("A", "C", "R"))
    # Legacy public-repository recipe sources are custody, not a second
    # ongoing convergence workspace. Refuse their modification as well.
    if path.startswith(RECIPE_ROOTS):
        return ["Maven/OpenRewrite recipe sources belong exclusively in Synexia"]
    found = []
    if text is not None and JDK_PRODUCT_SOURCE.fullmatch(path):
        prior = donor_references(baseline) if baseline is not None else set()
        for symbol in sorted(donor_references(text) - prior):
            found.append("new donor namespace reference in JDK source: " + symbol)
    if not added:
        return found  # retain original contracts; refuse newly introduced donor references
    if RECIPE_DESCRIPTOR.search(path) or RECIPE_JAVA.search(path):
        found.append("new transformation recipe source/descriptor belongs in Synexia")
    parts = PurePosixPath(path).parts
    name = parts[-1]
    if not passive_data_receipt(path) and any(DATE.search(seg) or VERSION.search(seg) for seg in parts):
        found.append("unstable date/version identifier")
    if path.startswith("m3/docs/name-mapping.d/"):
        found.append("new parallel mapping fragment; update canonical name-mapping.json instead")
    matched_jdk = JDK_SOURCE.match(path)
    if matched_jdk is not None and name.endswith(".java"):
        if name.startswith("M3"):
            found.append("new public java.* M3 owner; implement existing JDK API in place")
        if text is not None:
            declared = PACKAGE.search(text)
            expected = ".".join(path[matched_jdk.start(1):].split("/")[:-1])
            if declared is None or declared.group(1) != expected:
                found.append("declared package differs from Java module path")
            direct_type = name[:-5]
            if direct_type not in ("module-info", "package-info") and direct_type not in TYPE.findall(text):
                found.append("Java filename does not match a declared type")
            if any(t.startswith("M3") for t in PUBLIC.findall(text)):
                found.append("new M3-prefixed public JDK type")
    if path.startswith("test/jdk/") and name.endswith(".java") and text is not None:
        declared = name[:-5]
        if declared not in TYPE.findall(text):
            found.append("jtreg file and type name mismatch")
    return found


def check(root: Path, base: str, head: str) -> tuple[list[Change], list[tuple[str, str]]]:
    delta = changes(root, base, head)
    violations = []
    for change in delta:
        source = None
        baseline = None
        product_java = JDK_PRODUCT_SOURCE.fullmatch(change.path) is not None
        if change.path.endswith(".java") and (
                product_java or change.status.startswith(("A", "C", "R"))):
            source = git(root, "show", head + ":" + change.path).decode("utf-8")
        if product_java and change.status.startswith(("M", "C", "R")):
            ancestor = git(root, "merge-base", base, head).decode("ascii").strip()
            old_path = change.previous or change.path
            baseline = git(root, "show", ancestor + ":" + old_path).decode("utf-8")
        for reason in problems(change, source, baseline):
            violations.append((change.path, reason))
    return delta, sorted(violations)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", required=True)
    parser.add_argument("--head", required=True)
    parser.add_argument("--repo", default=".", type=Path)
    args = parser.parse_args(argv)
    try:
        delta, failures = check(args.repo, args.base, args.head)
    except (ValueError, UnicodeError, OSError) as exc:
        print("PUBLIC_NAMES_REFUSED: " + str(exc), file=sys.stderr)
        return 2
    for path, reason in failures:
        print(path + "\t" + reason)
    print(f"PUBLIC_NAMES files={len(delta)} violations={len(failures)}")
    return 1 if failures else 0


if __name__ == "__main__":
    raise SystemExit(main())
