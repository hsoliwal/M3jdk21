#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed M3 MOP recipe learning-memory and PR source-change policy."""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
import sys
from dataclasses import dataclass
from io import StringIO
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TASK_ROOT = ROOT / "m3/tooling/migration-recipes/tasks"
RECIPE_ROOT = ROOT / "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite"
HEADER = [
    "seq",
    "recipe",
    "trigger",
    "observed",
    "correction",
    "guard",
    "lesson",
    "category",
    "donor",
    "status",
]
STATUSES = {"RECORDED", "VALIDATED", "ADMITTED", "REJECTED", "SUPERSEDED", "REVOKED"}
HEX = re.compile(r"[0-9a-f]{40,64}")
BASE_HEX = re.compile(r"[0-9a-f]{40}")
RECIPE_FQCN = re.compile(r"com\.m3\.rewrite(?:\.[A-Za-z_$][A-Za-z0-9_$]*)+")
PACKAGE = re.compile(r"(?m)^package\s+([A-Za-z_$][A-Za-z0-9_$.]*)\s*;")
CLASS = re.compile(r"(?m)\bclass\s+([A-Za-z_$][A-Za-z0-9_$]*Recipe)\b")
SOURCE_SUFFIXES = {".java", ".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".gmk", ".S", ".s"}
MAX_LEDGER_BYTES = 1024 * 1024


@dataclass(frozen=True)
class MemoryRow:
    seq: int
    recipe: str
    trigger: str
    observed: str
    correction: str
    guard: str
    lesson: str
    category: str
    donor: str
    status: str


def parse_memory(text: str, label: str) -> list[MemoryRow]:
    reader = csv.DictReader(StringIO(text), delimiter="\t")
    if reader.fieldnames != HEADER:
        raise ValueError(f"{label}: invalid RECIPE_MEMORY.tsv header: {reader.fieldnames}")
    rows: list[MemoryRow] = []
    expected = 1
    for physical, row in enumerate(reader, start=2):
        if None in row or any(row[field] is None or not row[field].strip() for field in HEADER):
            raise ValueError(f"{label}: blank/malformed field at row {physical}")
        try:
            seq = int(row["seq"])
        except ValueError as invalid:
            raise ValueError(f"{label}: invalid seq at row {physical}") from invalid
        if seq != expected:
            raise ValueError(
                f"{label}: non-contiguous seq at row {physical}: expected {expected}, got {seq}"
            )
        expected += 1
        recipe = row["recipe"]
        if not RECIPE_FQCN.fullmatch(recipe):
            raise ValueError(f"{label}: invalid recipe FQCN at row {physical}: {recipe}")
        status = row["status"]
        if status not in STATUSES:
            raise ValueError(f"{label}: invalid status at row {physical}: {status}")
        correction = row["correction"]
        if correction == "PENDING":
            if status != "RECORDED":
                raise ValueError(f"{label}: only RECORDED rows may have PENDING correction")
        elif not HEX.fullmatch(correction):
            raise ValueError(f"{label}: invalid correction revision at row {physical}")
        rows.append(
            MemoryRow(
                seq,
                recipe,
                row["trigger"],
                row["observed"],
                correction,
                row["guard"],
                row["lesson"],
                row["category"],
                row["donor"],
                status,
            )
        )
    if not rows:
        raise ValueError(f"{label}: empty recipe memory")
    return rows


def memory_files() -> list[Path]:
    if not TASK_ROOT.is_dir():
        return []
    return sorted(
        path
        for path in TASK_ROOT.rglob("RECIPE_MEMORY.tsv")
        if path.is_file() and not path.is_symlink()
    )


def validate_current() -> dict[str, list[MemoryRow]]:
    ledgers: dict[str, list[MemoryRow]] = {}
    for path in memory_files():
        if path.stat().st_size <= 0 or path.stat().st_size > MAX_LEDGER_BYTES:
            raise ValueError(f"invalid recipe memory size: {path.relative_to(ROOT)}")
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(ROOT).as_posix()
        ledgers[relative] = parse_memory(text, relative)
    return ledgers


def git(*args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        check=check,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )


def changed_paths(base: str) -> list[str]:
    if not BASE_HEX.fullmatch(base):
        raise ValueError("base must be a 40-character lowercase Git revision")
    return [
        line
        for line in git("diff", "--name-only", "--diff-filter=ACMRT", f"{base}...HEAD").stdout.splitlines()
        if line
    ]


def is_runtime_source(path: str) -> bool:
    suffix = Path(path).suffix
    if suffix not in SOURCE_SUFFIXES:
        return False
    if path.startswith("src/"):
        return True
    return (
        path.startswith("m3/")
        and "/src/main/" in path
        and not path.startswith("m3/tooling/migration-recipes/")
    )


def is_recipe_source(path: str) -> bool:
    return (
        path.startswith("m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/")
        and path.endswith("Recipe.java")
    )


def is_memory(path: str) -> bool:
    return (
        path.startswith("m3/tooling/migration-recipes/tasks/")
        and path.endswith("/RECIPE_MEMORY.tsv")
    )


def recipe_identity(path: str) -> str:
    source = (ROOT / path).read_text(encoding="utf-8")
    package = PACKAGE.search(source)
    clazz = CLASS.search(source)
    if package is None or clazz is None:
        raise ValueError(f"cannot resolve changed recipe identity: {path}")
    return package.group(1) + "." + clazz.group(1)


def base_text(base: str, path: str) -> str | None:
    probe = git("cat-file", "-e", f"{base}:{path}", check=False)
    if probe.returncode != 0:
        return None
    return git("show", f"{base}:{path}").stdout


def verify_append_only(base: str, path: str, current: list[MemoryRow]) -> None:
    previous_text = base_text(base, path)
    if previous_text is None:
        return
    previous = parse_memory(previous_text, f"{base}:{path}")
    if len(current) < len(previous):
        raise ValueError(f"{path}: recipe memory removed prior rows")
    if current[: len(previous)] != previous:
        raise ValueError(f"{path}: recipe memory rewrote prior rows")


def enforce_pr(base: str, ledgers: dict[str, list[MemoryRow]]) -> tuple[int, int, int]:
    paths = changed_paths(base)
    runtime = sorted(path for path in paths if is_runtime_source(path))
    recipes = sorted(path for path in paths if is_recipe_source(path))
    memories = sorted(path for path in paths if is_memory(path))

    if runtime and not recipes:
        raise ValueError(
            "runtime/native source changed without a changed Maven/OpenRewrite recipe owner:\n"
            + "\n".join(runtime)
        )
    if recipes and not memories:
        raise ValueError(
            "recipe source changed without append-only RECIPE_MEMORY.tsv update:\n"
            + "\n".join(recipes)
        )

    memory_recipes: set[str] = set()
    for path in memories:
        rows = ledgers.get(path)
        if rows is None:
            raise ValueError(f"changed recipe memory is missing/invalid: {path}")
        verify_append_only(base, path, rows)
        memory_recipes.update(row.recipe for row in rows)

    for path in recipes:
        identity = recipe_identity(path)
        if identity not in memory_recipes:
            raise ValueError(
                f"changed recipe {identity} has no row in changed RECIPE_MEMORY.tsv"
            )

    return len(runtime), len(recipes), len(memories)


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base")
    args = parser.parse_args(argv)

    try:
        ledgers = validate_current()
        runtime = recipes = memories = 0
        if args.base:
            runtime, recipes, memories = enforce_pr(args.base, ledgers)
    except (OSError, UnicodeError, ValueError, subprocess.CalledProcessError) as failure:
        print(f"M3_RECIPE_MEMORY_FAIL: {failure}", file=sys.stderr)
        return 1

    rows = sum(len(value) for value in ledgers.values())
    print(
        "M3_RECIPE_MEMORY_PASS "
        f"ledgers={len(ledgers)} rows={rows} "
        f"runtime_changes={runtime} recipe_changes={recipes} memory_changes={memories}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
