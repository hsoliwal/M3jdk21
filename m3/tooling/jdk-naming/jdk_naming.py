#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Rename Synexia names in M3JDK21 to JDK naming (recipe JDK_NAMING_RENAME, user decision 2026-10-31).

Scheme: modules and exported packages jdk.m3.<area>; internal packages jdk.internal.m3.<area>; com.synexia.* ->
jdk.internal.m3.*; Synexia* -> M3*; synexia-* -> m3-*; SYNEXIA_* -> M3_*. Legacy recipe code is renamed and kept
(absorb by rename), not deleted. Untouched: copyright, SPDX and license lines; references to the hsoliwal/com.synexia
repository (dependencies, removed by the standalone work, not renamed); documentation and lineage (Markdown, TSV
ledgers, provenance and evidence JSON, NOTICE/LICENSE, m3/docs, release audits).

plan: every tracked path gets its renamed path (refused on collision); every in-scope text file present gets its
rewrite count per rule. apply: git mv plus rewrites, only when the plan has no refusal and every file to change is
checked out. A second plan after apply finds nothing (fixed point).
CLI: python -I jdk_naming.py plan|apply [--root DIR] [--tsv OUT] [--only PREFIX]...
"""
from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

# ordered: the first matching rule rewrites a token; earlier rules are more specific
TEXT_RULES = (
    ("PKG_M3", re.compile(r"\bcom\.synexia\.m3\b"), "jdk.internal.m3"),
    ("PKG", re.compile(r"\bcom\.synexia\.(?=[a-z])"), "jdk.internal.m3."),
    ("PKG_PATH_M3", re.compile(r"\bcom/synexia/m3/"), "jdk/internal/m3/"),
    ("PKG_PATH", re.compile(r"\bcom/synexia/(?=[a-z])"), "jdk/internal/m3/"),
    ("TYPE_M3", re.compile(r"\bM3Synexia(?=[A-Z0-9_])"), "M3"),
    ("TYPE", re.compile(r"\bSynexia(?=[A-Z0-9_])"), "M3"),
    ("CONST", re.compile(r"\bSYNEXIA_(?=[A-Z0-9])"), "M3_"),
    ("KEBAB", re.compile(r"\bsynexia-(?=[a-z0-9])"), "m3-"),
    ("SNAKE", re.compile(r"\bsynexia_(?=[a-z0-9])"), "m3_"),
    # second pass (user, 2026-10-31, the rule restated after the first rename): the rest of the Synexia names and
    # the M3 packages to JDK naming
    ("CONST_KEBAB", re.compile(r"\bSYNEXIA-(?=[A-Z0-9])"), "M3-"),
    ("SEGMENT", re.compile(r"(?<=/)synexia(?=/)"), "m3"),
    ("WORD", re.compile(r"\bSynexia\b"), "M3"),
    ("PKG_COM_M3", re.compile(r"\bcom\.m3\.(?=[a-z])"), "jdk.m3."),
    ("PKG_PATH_COM_M3", re.compile(r"\bcom/m3/(?=[a-z])"), "jdk/m3/"),
)
PATH_RULES = (
    (re.compile(r"(^|/)com/synexia/m3/"), r"\1jdk/internal/m3/"),
    (re.compile(r"(^|/)com/synexia/"), r"\1jdk/internal/m3/"),
    (re.compile(r"M3Synexia(?=[A-Z0-9_])"), "M3"),
    (re.compile(r"Synexia(?=[A-Z0-9_])"), "M3"),
    (re.compile(r"(^|[/_.-])synexia-"), r"\1m3-"),
    (re.compile(r"(^|[/_.-])synexia_"), r"\1m3_"),
    (re.compile(r"(^|/)synexia(?=/)"), r"\1m3"),
    (re.compile(r"(^|[/_.-])SYNEXIA_(?=[A-Z0-9])"), r"\1M3_"),
    (re.compile(r"(^|[/_.-])SYNEXIA-(?=[A-Z0-9])"), r"\1M3-"),
    (re.compile(r"(^|/)com/m3/"), r"\1jdk/m3/"),
)
# copyright and SPDX lines are kept whole; dependencies are kept as spans and the rest of their line is renamed
# (renaming a dependency would hide it from the standalone check): links to the Synexia repository, SYNEXIA_* secrets,
# and Maven groupIds (mapped in a later phase, not as Java packages). A whole kept line left a one-line
# <dependency><groupId>com.synexia</groupId><artifactId>synexia-x</artifactId> pointing at a renamed module.
KEEP_LINE = re.compile(r"Copyright|SPDX-")
# also kept as spans: Maven groupIds under com.m3 (coordinates, not packages) and the attribution file names
KEEP_SPAN = re.compile(r"<groupId>\s*com\.(?:synexia|m3)[\w.]*\s*</groupId>|secrets\.SYNEXIA_\w+"
                       r"|\bSynexia-(?:NOTICE|LICENSE|RIGHTS|AUTHORS)\b|M3-SYNEXIA-[A-Z]+(?:\.md)?"
                       r"|(?:https?://)?(?:www\.)?(?:github\.com|raw\.githubusercontent\.com|api\.github\.com/repos)"
                       r"/hsoliwal/com\.synexia[\w./#?=&%~:+-]*|hsoliwal/com\.synexia\b")
SPAN_TOKEN = re.compile("\x00(\\d+)\x00")
# legal and attribution files keep their names (M3-SYNEXIA-NOTICE.md is a required release-boundary file)
KEEP_PATH = re.compile(r"(?i)synexia-(?:notice|license|rights|authors)")
TEXT_SUFFIXES = {".java", ".yml", ".yaml", ".xml", ".sh", ".py", ".mjs", ".js", ".properties", ".gradle", ".toml",
                 ".cfg", ".ini", ".c", ".h", ".cpp", ".json", ".txt"}
DOC_SUFFIXES = {".md", ".tsv", ".rst", ".adoc", ".html", ".log", ".patch"}
DOC_NAMES = {"NOTICE", "LICENSE", "COPYRIGHT", "AUTHORS", "THIRD_PARTY_NOTICES"}
LINEAGE_HINTS = ("provenance", "evidence", "receipt", "manifest", "SOURCE_SHA256", "FINAL_", "readback")
# files that define Synexia patterns on purpose: this recipe and the standalone check (renaming them blinds the check)
SELF = {"m3/tooling/jdk-naming/jdk_naming.py", "m3/tooling/jdk-naming/test_jdk_naming.py",
        "m3/release/check_standalone.py", "m3/release/test_check_standalone.py"}


def tracked(root: Path) -> list[str]:
    out = subprocess.run(["git", "-C", str(root), "ls-files", "-z"], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8", "surrogateescape").split("\0") if p]


def is_lineage(path: str) -> bool:
    p = Path(path)
    return (p.suffix.lower() in DOC_SUFFIXES or p.name in DOC_NAMES or path.startswith(("m3/docs/", "m3/release/audit-"))
            or "/evidence/" in path or "/receipts/" in path or "/surefire-reports/" in path
            or p.name.startswith("TEST-") and p.suffix.lower() == ".xml"
            or p.suffix.lower() == ".json" and any(h.lower() in path.lower() for h in LINEAGE_HINTS))


def renamed_path(path: str) -> str:
    for rule, replacement in PATH_RULES:
        path = rule.sub(replacement, path)
    return path


def rewrite_line(line: str, counts: dict) -> str:
    if KEEP_LINE.search(line):
        return line
    spans: list[str] = []

    def stash(match):
        spans.append(match.group(0))
        return "\x00%d\x00" % (len(spans) - 1)
    line = KEEP_SPAN.sub(stash, line)
    for name, rule, replacement in TEXT_RULES:
        line, n = rule.subn(replacement, line)
        if n:
            counts[name] = counts.get(name, 0) + n
    line = SPAN_TOKEN.sub(lambda m: spans[int(m.group(1))], line)
    return line


def rewrite_text(text: str) -> tuple[str, dict]:
    counts: dict = {}
    lines = text.split("\n")
    return "\n".join(rewrite_line(line, counts) for line in lines), counts


def in_scope(path: str, only: tuple[str, ...]) -> bool:
    if path in SELF or is_lineage(path):
        return False
    if only and not path.startswith(only):
        return False
    return Path(path).suffix.lower() in TEXT_SUFFIXES or Path(path).name == "pom.xml"


def plan(root: Path, only: tuple[str, ...] = ()) -> dict:
    root = Path(root).resolve()
    paths = tracked(root)
    existing = set(paths)
    moves, refusals, rewrites, absent = [], [], [], []
    targets: dict = {}
    for path in paths:
        if path in SELF or only and not path.startswith(only) or KEEP_PATH.search(path):
            continue
        new = renamed_path(path)
        if new != path:
            if new in existing or new in targets:
                if is_lineage(path):
                    continue  # KEEP_LINEAGE_PATH: lineage stays where its records say it is
                refusals.append(("COLLISION", path, new))
            targets[new] = path
            moves.append((path, new))
    for path in paths:
        if not in_scope(path, only):
            continue
        file = root / path
        if not file.is_file():
            if renamed_path(path) != path or "ynexia" in path or "YNEXIA" in path:
                absent.append(path)
            continue
        text = file.read_text(encoding="utf-8", errors="surrogateescape")
        new_text, counts = rewrite_text(text)
        if new_text != text:
            rewrites.append((path, counts))
    return {"moves": moves, "rewrites": rewrites, "refusals": refusals, "absent": absent}


def apply(root: Path, result: dict) -> None:
    root = Path(root).resolve()
    if result["refusals"]:
        raise SystemExit("REFUSED: " + "; ".join(f"{k} {a} -> {b}" for k, a, b in result["refusals"][:5]))
    for path, _ in result["rewrites"]:
        file = root / path
        text = file.read_text(encoding="utf-8", errors="surrogateescape")
        file.write_text(rewrite_text(text)[0], encoding="utf-8", errors="surrogateescape", newline="")
    # moves on disk, then one git add of every old and new path (one git mv per file took minutes per thousand)
    for path, new in result["moves"]:
        target = root / new
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            raise SystemExit("REFUSED: move target exists: " + new)
        (root / path).replace(target)
    paths = [p for p, _ in result["rewrites"]] + [p for move in result["moves"] for p in move]
    if paths:
        listing = "\0".join(paths) + "\0"
        subprocess.run(["git", "-C", str(root), "add", "-A", "--sparse", "--pathspec-from-file=-", "--pathspec-file-nul"],
                       input=listing.encode("utf-8", "surrogateescape"), check=True)


def write_tsv(result: dict, out: Path) -> None:
    rows = ["op\tpath\ttarget_or_counts"]
    rows += [f"MOVE\t{a}\t{b}" for a, b in result["moves"]]
    rows += ["REWRITE\t%s\t%s" % (p, ",".join(f"{k}={v}" for k, v in sorted(c.items()))) for p, c in result["rewrites"]]
    rows += [f"REFUSED_{k}\t{a}\t{b}" for k, a, b in result["refusals"]]
    rows += [f"ABSENT\t{p}\tnot checked out" for p in result["absent"]]
    out.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("mode", choices=("plan", "apply"))
    parser.add_argument("--root", default=".")
    parser.add_argument("--tsv")
    parser.add_argument("--only", action="append", default=[])
    args = parser.parse_args(argv)
    result = plan(Path(args.root), tuple(args.only))
    if args.tsv:
        write_tsv(result, Path(args.tsv))
    totals: dict = {}
    for _, counts in result["rewrites"]:
        for k, v in counts.items():
            totals[k] = totals.get(k, 0) + v
    print("JDK_NAMING moves=%d rewrites=%d refusals=%d absent=%d %s" % (
        len(result["moves"]), len(result["rewrites"]), len(result["refusals"]), len(result["absent"]),
        " ".join(f"{k}={v}" for k, v in sorted(totals.items()))))
    if args.mode == "apply":
        if result["absent"]:
            print("REFUSED: files to rename are not checked out; widen the checkout first")
            return 2
        apply(Path(args.root), result)
        again = plan(Path(args.root), tuple(args.only))
        print("FIXED_POINT moves=%d rewrites=%d" % (len(again["moves"]), len(again["rewrites"])))
        return 0 if not again["moves"] and not again["rewrites"] else 1
    return 0 if not result["refusals"] else 1


if __name__ == "__main__":
    sys.exit(main())
