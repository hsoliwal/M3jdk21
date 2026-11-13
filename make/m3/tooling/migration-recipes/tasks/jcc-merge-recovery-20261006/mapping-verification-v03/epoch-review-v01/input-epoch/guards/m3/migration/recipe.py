#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed offline replay of a sealed, source-pinned OpenRewrite installation plan.

This is a replay/rollback verifier, not a substitute claim that Maven executed.
Plans contain immutable before/after images. Every path and image is validated
before any destination write. Workspaces require exclusive mutation ownership.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import tempfile
from typing import Any

SCHEMA = "m3.sealed-install/1"
LOCK = ".m3-migration.lock"
JOURNAL = ".m3-migration-journal.json"

class Refusal(ValueError):
    """An input, output, plan, or workspace does not match its explicit contract."""

def digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def canonical(value: Any) -> bytes:
    return (json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=True) + "\n").encode()

def checked_path(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or "\\" in relative or "\x00" in relative:
        raise Refusal("invalid relative path")
    name = PurePosixPath(relative)
    if name.is_absolute() or any(p in ("", ".", "..") or ":" in p for p in relative.split("/")):
        raise Refusal("unsafe path: " + relative)
    path = root
    for component in name.parts:
        path /= component
        if path.is_symlink():
            raise Refusal("symlink refused: " + relative)
    return path

def read_optional(path: Path) -> bytes | None:
    if path.exists() and not path.is_file():
        raise Refusal("not a regular file: " + str(path))
    return path.read_bytes() if path.exists() else None

def sealed_plan(plan_path: Path) -> tuple[dict, list[dict]]:
    def unique_pairs(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise Refusal("duplicate JSON key: " + key)
            result[key] = value
        return result
    plan = json.loads(plan_path.read_text(encoding="utf-8"), object_pairs_hook=unique_pairs)
    if plan.get("schema") != SCHEMA or not plan.get("recipe_id") or not plan.get("outputs"):
        raise Refusal("unsupported/empty plan")
    seal = plan.get("plan_sha256")
    body = {k: v for k, v in plan.items() if k != "plan_sha256"}
    if seal != digest(canonical(body)):
        raise Refusal("plan seal mismatch")
    names = set()
    rows = []
    for entry in plan["outputs"]:
        relative = entry["path"]
        checked_path(Path("."), relative)
        if relative in names or relative in (LOCK, JOURNAL):
            raise Refusal("duplicate/reserved output path")
        names.add(relative)
        row = {"path": relative}
        for state in ("before", "after"):
            image = entry.get(state)
            if image is None:
                row[state] = None
            else:
                image_path = checked_path(plan_path.parent.resolve(), image["resource"])
                content = image_path.read_bytes()
                if digest(content) != image["sha256"]:
                    raise Refusal(state + " resource drift: " + relative)
                row[state] = content
        if row["before"] == row["after"]:
            raise Refusal("no-op output in plan: " + relative)
        rows.append(row)
    guarded = set()
    for guard in plan.get("guards", []):
        checked_path(Path("."), guard["path"])
        if guard["path"] in names or guard["path"] in guarded:
            raise Refusal("duplicate/overlapping guard")
        expected = guard["sha256"]
        if len(expected) != 64 or any(c not in "0123456789abcdef" for c in expected):
            raise Refusal("invalid guard hash")
        guarded.add(guard["path"])
    return plan, rows

def observe_guards(root: Path, plan: dict) -> None:
    for guard in plan.get("guards", []):
        content = read_optional(checked_path(root, guard["path"]))
        if content is None or digest(content) != guard["sha256"]:
            raise Refusal("dependency guard drift: " + guard["path"])

def observe(root: Path, rows: list[dict]) -> list[str]:
    states = []
    for row in rows:
        data = read_optional(checked_path(root, row["path"]))
        if data == row["before"]:
            states.append("before")
        elif data == row["after"]:
            states.append("after")
        else:
            raise Refusal("destination drift: " + row["path"])
    return states

def atomic_write(path: Path, data: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    prior_mode = path.stat().st_mode & 0o777 if path.exists() else 0o644
    fd, temporary = tempfile.mkstemp(prefix=".m3-stage-", dir=path.parent)
    try:
        with os.fdopen(fd, "wb") as stream:
            stream.write(data)
            stream.flush()
            os.fsync(stream.fileno())
        os.chmod(temporary, prior_mode)
        os.replace(temporary, path)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)

def restore(root: Path, rows: list[dict], state: str) -> None:
    for row in rows:
        path = checked_path(root, row["path"])
        content = row[state]
        if content is None:
            if path.exists():
                path.unlink()
        elif read_optional(path) != content:
            atomic_write(path, content)

def execute(plan_path: Path, root: Path, mode: str = "check") -> dict:
    if mode not in ("check", "apply", "rollback", "recover"):
        raise Refusal("unknown mode")
    if root.is_symlink():
        raise Refusal("symlink workspace refused")
    root = root.resolve()
    if not root.is_dir():
        raise Refusal("workspace does not exist")
    plan, rows = sealed_plan(plan_path.resolve())
    lock = checked_path(root, LOCK)
    journal = checked_path(root, JOURNAL)
    seal = plan["plan_sha256"]
    observe_guards(root, plan)
    states = observe(root, rows)  # preflight also runs for recovery
    if mode == "check":
        if lock.exists() or journal.exists():
            raise Refusal("incomplete transaction; use explicit recovery")
        if len(set(states)) != 1:
            raise Refusal("mixed before/after state")
        return {"recipe": plan["recipe_id"], "plan_sha256": seal,
                "state": states[0], "files": len(rows), "writes": 0}
    if mode == "recover":
        if not lock.is_file():
            raise Refusal("recovery needs this plan's lock")
        if lock.read_text().strip() != seal:
            raise Refusal("foreign recovery lock")
        if not journal.exists():
            if len(set(states)) != 1:
                raise Refusal("mixed state without a recovery journal")
            lock.unlink()
            return {"recipe": plan["recipe_id"], "state": "recovered-lock-only", "files": len(rows)}
        recorded = json.loads(journal.read_text())
        if recorded.get("restore_state") not in ("before", "after"):
            raise Refusal("invalid recovery state")
        if recorded.get("plan_sha256") != seal or recorded.get("paths") != [r["path"] for r in rows]:
            raise Refusal("foreign recovery journal")
        restore(root, rows, recorded["restore_state"])
        journal.unlink()
        lock.unlink()
        return {"recipe": plan["recipe_id"], "state": "recovered", "files": len(rows)}
    if journal.exists():
        raise Refusal("unfinished journal; use explicit recovery")
    if len(set(states)) != 1:
        raise Refusal("mixed before/after state")
    destination = "after" if mode == "apply" else "before"
    if states[0] == destination:
        if lock.exists():
            raise Refusal("workspace is locked")
        return {"recipe": plan["recipe_id"], "plan_sha256": seal,
                "state": destination, "files": len(rows), "writes": 0}
    flags = os.O_CREAT | os.O_EXCL | os.O_WRONLY | getattr(os, "O_NOFOLLOW", 0)
    try:
        descriptor = os.open(lock, flags, 0o600)
    except FileExistsError as exc:
        raise Refusal("workspace is locked") from exc
    journal_written = False
    try:
        with os.fdopen(descriptor, "w") as stream:
            stream.write(seal + "\n")
        observe_guards(root, plan)
        if observe(root, rows) != states:
            raise Refusal("workspace changed during lock acquisition")
        atomic_write(journal, canonical({"plan_sha256": seal, "restore_state": states[0],
                                        "paths": [row["path"] for row in rows]}))
        journal_written = True
        try:
            restore(root, rows, destination)
        except BaseException:
            # Never overwrite unrelated concurrent changes during recovery.
            observe(root, rows)
            restore(root, rows, states[0])
            journal.unlink()
            journal_written = False
            raise
        if observe(root, rows) != [destination] * len(rows):
            raise Refusal("postimage publication was interrupted")
        journal.unlink()
        journal_written = False
    finally:
        if not journal_written and lock.exists():
            lock.unlink()
    return {"recipe": plan["recipe_id"], "plan_sha256": seal,
            "state": destination, "files": len(rows), "writes": len(rows)}

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("mode", choices=("check", "apply", "rollback", "recover"))
    parser.add_argument("--plan", required=True, type=Path)
    parser.add_argument("--root", required=True, type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(execute(args.plan, args.root, args.mode), sort_keys=True))
    except (Refusal, OSError, KeyError, TypeError, json.JSONDecodeError) as error:
        parser.exit(2, "REFUSED: " + str(error) + "\n")
