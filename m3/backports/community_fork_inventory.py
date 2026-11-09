#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Inventory fork-unique JDK21 changes against current openjdk/jdk21u.

This is evidence only. It never auto-admits compatibility, copies donor source,
or grants mutation/promotion authority. Git patch-equivalence filtering avoids
counting commits that are already present upstream under a different commit id.
"""

from __future__ import annotations

import argparse
import csv
import json
import re
import subprocess
import sys
from collections import Counter, defaultdict
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

JBS_RE = re.compile(r"\bJDK-\d{7}\b")
COMPAT_RE = re.compile(
    r"\b(remove|removal|deprecat|restrict|disable|obsolete|drop support|"
    r"behavior change|incompatible)\w*\b",
    re.IGNORECASE,
)
NATIVE_EXTENSIONS = (".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".s", ".asm")
TOOL_PREFIXES = (
    "src/jdk.jcmd/",
    "src/jdk.jconsole/",
    "src/jdk.jdeps/",
    "src/jdk.javadoc/",
    "src/jdk.jartool/",
    "src/jdk.jlink/",
    "src/jdk.jpackage/",
    "src/jdk.jshell/",
    "src/jdk.jfr/",
    "src/jdk.management",
    "src/jdk.attach/",
    "src/jdk.hotspot.agent/",
)
GRAMMAR_PREFIXES = (
    "src/jdk.compiler/share/classes/com/sun/tools/javac/parser/",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/comp/",
)
GRAMMAR_EXACT = {
    "src/jdk.compiler/share/classes/com/sun/tools/javac/code/Source.java",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/code/Preview.java",
    "src/jdk.compiler/share/classes/com/sun/tools/javac/parser/Tokens.java",
}


@dataclass(frozen=True)
class ForkSpec:
    fork_id: str
    repository: str
    ref: str
    pinned_head: str
    plane: str
    priority: str
    license_policy: str
    source_copy_authority: bool
    selected_for_distribution: bool
    notes: str


@dataclass(frozen=True)
class Relationship:
    fork_id: str
    repository: str
    ref: str
    pinned_head: str
    resolved_head: str
    merge_base: str
    common_ancestry: bool
    fork_unique_commits: int
    upstream_unique_commits: int
    source_copy_authority: bool
    selected_for_distribution: bool


@dataclass(frozen=True)
class ForkChange:
    fork_id: str
    order: int
    commit: str
    jbs_ids: tuple[str, ...]
    subject: str
    domain: str
    risk: str
    scope_floor: str
    recipe_strategy: str
    compatibility_state: str
    paths: tuple[str, ...]


def _git(repo: Path, *args: str, allow_failure: bool = False) -> str:
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="strict",
    )
    if proc.returncode and not allow_failure:
        raise RuntimeError(
            f"git {' '.join(args)} failed with {proc.returncode}: {proc.stderr.strip()}"
        )
    return proc.stdout if proc.returncode == 0 else ""


def _bool(value: str) -> bool:
    normalized = value.strip().lower()
    if normalized == "true":
        return True
    if normalized == "false":
        return False
    raise ValueError(f"invalid boolean: {value!r}")


def read_catalog(path: Path) -> list[ForkSpec]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError("empty community fork catalogue")

    result: list[ForkSpec] = []
    seen: set[str] = set()
    for row in rows:
        fork_id = row["fork_id"].strip()
        if not fork_id or fork_id in seen:
            raise ValueError(f"duplicate/blank fork_id: {fork_id!r}")
        seen.add(fork_id)
        pinned = row["pinned_head"].strip().lower()
        if not re.fullmatch(r"[0-9a-f]{40}", pinned):
            raise ValueError(f"invalid pinned_head for {fork_id}: {pinned}")
        source_copy = _bool(row["source_copy_authority"])
        selected = _bool(row["selected_for_distribution"])
        if source_copy or selected:
            raise ValueError(
                f"community fork catalogue may not pre-authorize source copy/distribution: {fork_id}"
            )
        result.append(
            ForkSpec(
                fork_id=fork_id,
                repository=row["repository"].strip(),
                ref=row["ref"].strip(),
                pinned_head=pinned,
                plane=row["plane"].strip(),
                priority=row["priority"].strip(),
                license_policy=row["license_policy"].strip(),
                source_copy_authority=source_copy,
                selected_for_distribution=selected,
                notes=row["notes"].strip(),
            )
        )
    if result[0].fork_id != "upstream21u":
        raise ValueError("first community fork row must be upstream21u")
    return result


def _ref(fork_id: str) -> str:
    return f"refs/remotes/m3/{fork_id}"


def _verify_repo(repo: Path) -> None:
    if not repo.is_dir():
        raise ValueError(f"not a directory: {repo}")
    if _git(repo, "rev-parse", "--is-inside-work-tree").strip() != "true":
        raise ValueError(f"not a Git work tree: {repo}")


def _resolve(repo: Path, fork_id: str) -> str:
    return _git(repo, "rev-parse", "--verify", f"{_ref(fork_id)}^{{commit}}").strip()


def _merge_base(repo: Path, left: str, right: str) -> str:
    return _git(repo, "merge-base", left, right, allow_failure=True).strip()


def _unique_commits(repo: Path, upstream: str, fork: str, right: bool) -> tuple[str, ...]:
    side = "--right-only" if right else "--left-only"
    output = _git(
        repo,
        "rev-list",
        "--reverse",
        "--cherry-pick",
        side,
        f"{upstream}...{fork}",
        allow_failure=True,
    )
    return tuple(line for line in output.splitlines() if line)


def _subject(repo: Path, commit: str) -> str:
    return _git(repo, "show", "-s", "--format=%s", commit).rstrip("\n")


def _paths(repo: Path, commit: str) -> tuple[str, ...]:
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


def _native(path: str) -> bool:
    return path.lower().endswith(NATIVE_EXTENSIONS)


def _grammar(path: str) -> bool:
    return path in GRAMMAR_EXACT or any(path.startswith(prefix) for prefix in GRAMMAR_PREFIXES)


def _domain(paths: Sequence[str]) -> str:
    if any(_grammar(path) for path in paths):
        return "javac-language-sensitive"
    if any(path.startswith("src/hotspot/") for path in paths):
        return "hotspot"
    if any(_native(path) for path in paths):
        return "native-or-jni"
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


def _module(path: str) -> str:
    parts = path.split("/")
    if path.startswith("src/") and len(parts) > 1:
        return parts[1]
    if path.startswith("test/") and len(parts) > 1:
        return f"<test:{parts[1]}>"
    if path.startswith("make/"):
        return "<build>"
    return parts[0] if parts else "<root>"


def _scope_floor(paths: Sequence[str]) -> str:
    if not paths:
        return "FILE"
    if len(paths) == 1:
        return "FILE"
    parents = {str(Path(path).parent).replace("\\", "/") for path in paths}
    if len(parents) == 1:
        return "PACKAGE"
    modules = {_module(path) for path in paths}
    if len(modules) == 1:
        return "MODULE"
    return "MULTI_MODULE"


def _recipe_strategy(paths: Sequence[str]) -> str:
    if not paths:
        return "NO_SOURCE_PATH_REVIEW"
    java = all(path.endswith(".java") for path in paths)
    native = any(_native(path) for path in paths)
    if java:
        return "OPENREWRITE_JAVA_REVIEW"
    if native and all(_native(path) or path.startswith("test/") for path in paths):
        return "SOURCE_SEALED_NATIVE_REVIEW"
    textish = all(
        not _native(path)
        and not path.lower().endswith((".class", ".jar", ".zip", ".png", ".jpg", ".gif"))
        for path in paths
    )
    if textish:
        return "VERBATIM_TEXT_REVIEW"
    return "COMPOSITE_OR_BINARY_REVIEW"


def _risk(subject: str, paths: Sequence[str]) -> str:
    if any(_grammar(path) for path in paths):
        return "CRITICAL"
    if COMPAT_RE.search(subject):
        return "HIGH"
    if any(path.startswith("src/hotspot/") for path in paths) or any(_native(path) for path in paths):
        return "HIGH"
    if any(path.startswith(TOOL_PREFIXES) for path in paths):
        return "MEDIUM"
    if paths and all(path.startswith(("make/", "test/", ".github/")) for path in paths):
        return "LOW"
    return "MEDIUM"


def inventory(repo: Path, specs: Sequence[ForkSpec]) -> tuple[list[Relationship], list[ForkChange]]:
    _verify_repo(repo)
    upstream_spec = specs[0]
    upstream_ref = _ref(upstream_spec.fork_id)
    upstream_head = _resolve(repo, upstream_spec.fork_id)
    if upstream_head != upstream_spec.pinned_head:
        raise ValueError(
            f"upstream pin drift: expected {upstream_spec.pinned_head}, found {upstream_head}"
        )

    relationships: list[Relationship] = []
    changes: list[ForkChange] = []

    for spec in specs[1:]:
        resolved = _resolve(repo, spec.fork_id)
        if resolved != spec.pinned_head:
            raise ValueError(
                f"{spec.fork_id} pin drift: expected {spec.pinned_head}, found {resolved}"
            )

        fork_ref = _ref(spec.fork_id)
        merge_base = _merge_base(repo, upstream_ref, fork_ref)
        common = bool(merge_base)
        if common:
            fork_unique = _unique_commits(repo, upstream_ref, fork_ref, True)
            upstream_unique = _unique_commits(repo, upstream_ref, fork_ref, False)
        else:
            fork_unique = ()
            upstream_unique = ()

        relationships.append(
            Relationship(
                fork_id=spec.fork_id,
                repository=spec.repository,
                ref=spec.ref,
                pinned_head=spec.pinned_head,
                resolved_head=resolved,
                merge_base=merge_base,
                common_ancestry=common,
                fork_unique_commits=len(fork_unique),
                upstream_unique_commits=len(upstream_unique),
                source_copy_authority=False,
                selected_for_distribution=False,
            )
        )

        for order, commit in enumerate(fork_unique):
            subject = _subject(repo, commit)
            paths = _paths(repo, commit)
            changes.append(
                ForkChange(
                    fork_id=spec.fork_id,
                    order=order,
                    commit=commit,
                    jbs_ids=tuple(dict.fromkeys(JBS_RE.findall(subject))),
                    subject=subject,
                    domain=_domain(paths),
                    risk=_risk(subject, paths),
                    scope_floor=_scope_floor(paths),
                    recipe_strategy=_recipe_strategy(paths),
                    compatibility_state="PENDING_COMPATIBILITY_PROOF",
                    paths=paths,
                )
            )

    changes.sort(key=lambda row: (row.fork_id, row.order, row.commit))
    return relationships, changes


def write_relationships(rows: Sequence[Relationship], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow((
        "fork_id", "repository", "ref", "pinned_head", "resolved_head", "merge_base",
        "common_ancestry", "fork_unique_commits", "upstream_unique_commits",
        "source_copy_authority", "selected_for_distribution",
    ))
    for row in rows:
        writer.writerow((
            row.fork_id,
            row.repository,
            row.ref,
            row.pinned_head,
            row.resolved_head,
            row.merge_base,
            str(row.common_ancestry).lower(),
            row.fork_unique_commits,
            row.upstream_unique_commits,
            "false",
            "false",
        ))


def write_changes(rows: Sequence[ForkChange], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow((
        "fork_id", "order", "commit", "jbs_ids", "subject", "domain", "risk",
        "scope_floor", "recipe_strategy", "compatibility_state", "paths",
    ))
    for row in rows:
        writer.writerow((
            row.fork_id,
            row.order,
            row.commit,
            ",".join(row.jbs_ids),
            row.subject,
            row.domain,
            row.risk,
            row.scope_floor,
            row.recipe_strategy,
            row.compatibility_state,
            ",".join(row.paths),
        ))


def write_path_candidates(rows: Sequence[ForkChange], out) -> None:
    grouped: dict[str, list[ForkChange]] = defaultdict(list)
    for row in rows:
        for path in row.paths:
            grouped[path].append(row)

    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow((
        "path", "forks", "commit_count", "domains", "highest_risk",
        "scope_floor", "recipe_strategies", "compatibility_state",
        "source_copy_authority", "selected_for_distribution",
    ))
    risk_rank = {"LOW": 0, "MEDIUM": 1, "HIGH": 2, "CRITICAL": 3}
    for path in sorted(grouped):
        members = grouped[path]
        highest = max((member.risk for member in members), key=risk_rank.__getitem__)
        scope = _scope_floor((path,))
        writer.writerow((
            path,
            ",".join(sorted({member.fork_id for member in members})),
            len({member.commit for member in members}),
            ",".join(sorted({member.domain for member in members})),
            highest,
            scope,
            ",".join(sorted({member.recipe_strategy for member in members})),
            "PENDING_COMPATIBILITY_PROOF",
            "false",
            "false",
        ))


def summary(specs: Sequence[ForkSpec], relationships: Sequence[Relationship], changes: Sequence[ForkChange]) -> dict[str, object]:
    return {
        "schema": 1,
        "baseline": specs[0].repository + "@" + specs[0].pinned_head,
        "forks": len(relationships),
        "fork_unique_commits": len(changes),
        "by_fork": dict(sorted(Counter(row.fork_id for row in changes).items())),
        "by_domain": dict(sorted(Counter(row.domain for row in changes).items())),
        "by_risk": dict(sorted(Counter(row.risk for row in changes).items())),
        "by_scope_floor": dict(sorted(Counter(row.scope_floor for row in changes).items())),
        "by_recipe_strategy": dict(sorted(Counter(row.recipe_strategy for row in changes).items())),
        "compatibility_state": "PENDING_COMPATIBILITY_PROOF",
        "source_copy_authority": False,
        "selected_for_distribution": False,
    }


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--catalog", type=Path, required=True)
    parser.add_argument("--out-dir", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    specs = read_catalog(args.catalog)
    relationships, changes = inventory(args.repo.resolve(), specs)

    out = args.out_dir.resolve()
    out.mkdir(parents=True, exist_ok=True)
    with (out / "FORK_RELATIONSHIPS.tsv").open("w", encoding="utf-8", newline="") as handle:
        write_relationships(relationships, handle)
    with (out / "FORK_UNIQUE_CHANGES.tsv").open("w", encoding="utf-8", newline="") as handle:
        write_changes(changes, handle)
    with (out / "FORK_PATH_CANDIDATES.tsv").open("w", encoding="utf-8", newline="") as handle:
        write_path_candidates(changes, handle)
    (out / "summary.json").write_text(
        json.dumps(summary(specs, relationships, changes), indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
