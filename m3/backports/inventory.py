#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Deterministically inventory upstream OpenJDK changes between GA tags.

This is an admission inventory, not a backport executor. It never mutates the
upstream checkout. Output is a TSV sorted by release and upstream commit order.
"""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

INTERVALS: tuple[tuple[int, str, str], ...] = (
    (22, "jdk-21+35", "jdk-22+36"),
    (23, "jdk-22+36", "jdk-23+37"),
    (24, "jdk-23+37", "jdk-24+36"),
    (25, "jdk-24+36", "jdk-25+36"),
    (26, "jdk-25+36", "jdk-26+35"),
    (27, "jdk-26+35", "jdk-27+35"),
)

JBS_RE = re.compile(r"\bJDK-\d{7}\b")
COMPAT_SIGNAL_RE = re.compile(
    r"\b(remove|removal|deprecat|restrict|disable|obsolete|drop support)\w*\b",
    re.IGNORECASE,
)

JAVAC_ROOT = "src/jdk.compiler/"
GRAMMAR_PREFIXES = (
    "src/jdk.compiler/share/classes/com/sun/tools/javac/parser/",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/comp/",
)
GRAMMAR_EXACT = {
    "src/jdk.compiler/share/classes/com/sun/tools/javac/code/Source.java",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/code/Preview.java",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/parser/Tokens.java",
}
TOOL_PREFIXES = (
    "src/jdk.jcmd/",
    "src/jdk.jconsole/",
    "src/jdk.jdeps/",
    "src/jdk.javadoc/",
    "src/jdk.jartool/",
    "src/jdk.jlink/",
    "src/jdk.jpackage/",
    "src/jdk.jshell/",
    "src/jdk.jstatd/",
    "src/jdk.jfr/",
    "src/jdk.management",
    "src/jdk.attach/",
    "src/jdk.hotspot.agent/",
)
SECURITY_MARKERS = (
    "/security/",
    "src/java.base/share/classes/java/security/",
    "src/java.base/share/classes/javax/crypto/",
    "src/java.base/share/classes/javax/net/ssl/",
    "src/jdk.crypto.",
)
HOTSPOT_COMPILER_PREFIXES = (
    "src/hotspot/share/c1/",
    "src/hotspot/share/c2/",
    "src/hotspot/share/opto/",
    "src/hotspot/share/compiler/",
    "src/hotspot/cpu/",
)


@dataclass(frozen=True)
class Change:
    release: int
    base_ref: str
    head_ref: str
    commit: str
    subject: str
    jbs_ids: tuple[str, ...]
    paths: tuple[str, ...]
    domain: str
    javac_touch: bool
    grammar_touch: bool
    hotspot_compiler_touch: bool
    compatibility_signal: bool
    disposition: str


def _git(repo: Path, *args: str) -> str:
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="strict",
    )
    if proc.returncode != 0:
        raise RuntimeError(
            f"git {' '.join(args)} failed with {proc.returncode}: {proc.stderr.strip()}"
        )
    return proc.stdout


def _verify_repo(repo: Path) -> None:
    if not repo.is_dir():
        raise ValueError(f"not a directory: {repo}")
    inside = _git(repo, "rev-parse", "--is-inside-work-tree").strip()
    if inside != "true":
        raise ValueError(f"not a Git work tree: {repo}")
    for _release, base_ref, head_ref in INTERVALS:
        _git(repo, "rev-parse", "--verify", f"{base_ref}^{{commit}}")
        _git(repo, "rev-parse", "--verify", f"{head_ref}^{{commit}}")


def _paths_for_commit(repo: Path, commit: str) -> tuple[str, ...]:
    output = _git(
        repo,
        "diff-tree",
        "--root",
        "--no-commit-id",
        "--name-only",
        "-r",
        "--no-renames",
        commit,
    )
    return tuple(line for line in output.splitlines() if line)


def _subject_for_commit(repo: Path, commit: str) -> str:
    return _git(repo, "show", "-s", "--format=%s", commit).rstrip("\n")


def _is_grammar_path(path: str) -> bool:
    return path in GRAMMAR_EXACT or any(path.startswith(p) for p in GRAMMAR_PREFIXES)


def _domain(paths: Sequence[str]) -> str:
    if any(path.startswith("src/hotspot/") for path in paths):
        return "hotspot"
    if any(any(marker in path for marker in SECURITY_MARKERS) for path in paths):
        return "security"
    if any(path.startswith(TOOL_PREFIXES) for path in paths):
        return "tools"
    if any(path.startswith("src/java.base/") for path in paths):
        return "core-libs"
    if any(path.startswith("src/") for path in paths):
        return "other-runtime-or-library"
    if any(path.startswith(("make/", "build/", ".github/")) for path in paths):
        return "build"
    if paths and all(path.startswith("test/") for path in paths):
        return "test"
    return "mixed-or-other"


def _classify(subject: str, paths: Sequence[str]) -> tuple[str, bool, bool, bool, bool]:
    javac_touch = any(path.startswith(JAVAC_ROOT) for path in paths)
    grammar_touch = any(_is_grammar_path(path) for path in paths)
    hotspot_compiler_touch = any(
        path.startswith(HOTSPOT_COMPILER_PREFIXES) for path in paths
    )
    compatibility_signal = bool(COMPAT_SIGNAL_RE.search(subject))

    if grammar_touch:
        disposition = "hold-language"
    elif javac_touch:
        disposition = "hold-javac"
    elif compatibility_signal:
        disposition = "hold-compat"
    elif hotspot_compiler_touch:
        disposition = "review-hotspot-compiler"
    elif any(path.startswith("src/hotspot/") for path in paths):
        disposition = "review-hotspot"
    elif any(path.startswith(TOOL_PREFIXES) for path in paths):
        disposition = "review-tool"
    else:
        disposition = "review"

    return (
        disposition,
        javac_touch,
        grammar_touch,
        hotspot_compiler_touch,
        compatibility_signal,
    )


def inventory(repo: Path, releases: Iterable[int]) -> list[Change]:
    selected = frozenset(releases)
    unknown = selected - {release for release, _base, _head in INTERVALS}
    if unknown:
        raise ValueError(f"unsupported release(s): {sorted(unknown)}")

    changes: list[Change] = []
    for release, base_ref, head_ref in INTERVALS:
        if release not in selected:
            continue
        commits = [
            line
            for line in _git(
                repo, "rev-list", "--reverse", f"{base_ref}..{head_ref}"
            ).splitlines()
            if line
        ]
        for commit in commits:
            subject = _subject_for_commit(repo, commit)
            paths = _paths_for_commit(repo, commit)
            jbs_ids = tuple(dict.fromkeys(JBS_RE.findall(subject)))
            (
                disposition,
                javac_touch,
                grammar_touch,
                hotspot_compiler_touch,
                compatibility_signal,
            ) = _classify(subject, paths)
            changes.append(
                Change(
                    release=release,
                    base_ref=base_ref,
                    head_ref=head_ref,
                    commit=commit,
                    subject=subject,
                    jbs_ids=jbs_ids,
                    paths=paths,
                    domain=_domain(paths),
                    javac_touch=javac_touch,
                    grammar_touch=grammar_touch,
                    hotspot_compiler_touch=hotspot_compiler_touch,
                    compatibility_signal=compatibility_signal,
                    disposition=disposition,
                )
            )
    return changes


def write_tsv(changes: Sequence[Change], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(
        (
            "release",
            "base_ref",
            "head_ref",
            "commit",
            "jbs_ids",
            "subject",
            "domain",
            "javac_touch",
            "grammar_touch",
            "hotspot_compiler_touch",
            "compatibility_signal",
            "disposition",
            "paths",
        )
    )
    for change in changes:
        writer.writerow(
            (
                change.release,
                change.base_ref,
                change.head_ref,
                change.commit,
                ",".join(change.jbs_ids),
                change.subject,
                change.domain,
                str(change.javac_touch).lower(),
                str(change.grammar_touch).lower(),
                str(change.hotspot_compiler_touch).lower(),
                str(change.compatibility_signal).lower(),
                change.disposition,
                ",".join(change.paths),
            )
        )


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--repo",
        type=Path,
        required=True,
        help="complete local openjdk/jdk checkout containing the pinned GA tags",
    )
    parser.add_argument(
        "--release",
        type=int,
        action="append",
        choices=range(22, 28),
        help="release to inventory; repeatable; default is 22 through 27",
    )
    parser.add_argument(
        "--out",
        type=Path,
        help="TSV destination; stdout when omitted",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    repo = args.repo.resolve()
    releases = tuple(args.release or range(22, 28))
    _verify_repo(repo)
    changes = inventory(repo, releases)

    if args.out is None:
        write_tsv(changes, sys.stdout)
    else:
        out = args.out.resolve()
        out.parent.mkdir(parents=True, exist_ok=True)
        with out.open("w", encoding="utf-8", newline="") as handle:
            write_tsv(changes, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
