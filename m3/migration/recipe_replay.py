#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Replay a reviewed exact-source recipe, preserving drift and partial-state refusals.

One file is promoted atomically at a time. The complete multi-file change is NOT a
filesystem transaction. A crash can leave a mixed state; normal replay refuses it.
Explicit --recover accepts only a mixture of the same sealed before/after states.
An exclusive cooperative lock protects this tool, not hostile external writers.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import stat
import tempfile
from typing import Any

HEX64 = re.compile(r"[0-9a-f]{64}\Z")
MAX_RESOURCE = 16 * 1024 * 1024


class Refusal(ValueError):
    """No automatic source or conflict-resolution authority exists."""


def sha(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def unique_object(pairs: list[tuple[str, Any]]) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for key, value in pairs:
        if key in result:
            raise Refusal(f"duplicate JSON key: {key}")
        result[key] = value
    return result


def read_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)


def safe_path(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative or "\\" in relative or "\0" in relative:
        raise Refusal("invalid relative path")
    pure = PurePosixPath(relative)
    if pure.is_absolute() or pure.as_posix() != relative or any(p in {".", ".."} for p in pure.parts):
        raise Refusal("noncanonical relative path")
    if ":" in pure.parts[0]:
        raise Refusal("drive-qualified path")
    current = root
    for i, part in enumerate(pure.parts):
        current = current / part
        try:
            mode = current.lstat().st_mode
        except FileNotFoundError:
            continue
        if stat.S_ISLNK(mode):
            raise Refusal(f"symlink refused: {relative}")
        if i < len(pure.parts) - 1 and not stat.S_ISDIR(mode):
            raise Refusal(f"nondirectory parent: {relative}")
        if i == len(pure.parts) - 1 and not stat.S_ISREG(mode):
            raise Refusal(f"nonregular target: {relative}")
    return current


def load_recipe(bundle: Path) -> tuple[str, dict[str, Any], list[dict[str, Any]]]:
    path = bundle / "recipe.json"
    data = path.read_bytes()
    doc = json.loads(data.decode("utf-8"), object_pairs_hook=unique_object)
    if not isinstance(doc, dict) or doc.get("schemaVersion") != 1:
        raise Refusal("unsupported recipe schema")
    if not isinstance(doc.get("id"), str) or not isinstance(doc.get("version"), str):
        raise Refusal("missing recipe identity")
    source_entries = doc.get("entries")
    if not isinstance(source_entries, list) or not source_entries:
        raise Refusal("empty or invalid transition list")
    seen: set[str] = set()
    entries = []
    for entry in source_entries:
        if not isinstance(entry, dict) or set(entry) != {"path", "before", "after", "preimage", "postimage"}:
            raise Refusal("invalid transition record")
        relative = entry["path"]
        # Syntax check also runs on the actual checkout during preflight.
        safe_path(bundle, relative)
        if not relative.startswith("m3/") or relative in seen:
            raise Refusal("duplicate or out-of-scope transition")
        seen.add(relative)
        loaded = dict(entry)
        for state, resource in (("before", "preimage"), ("after", "postimage")):
            digest, name = entry[state], entry[resource]
            if digest is None:
                if name is not None or state == "after":
                    raise Refusal("only an additive preimage may be absent")
                loaded[state + "Bytes"] = None
                continue
            if not isinstance(digest, str) or not HEX64.fullmatch(digest):
                raise Refusal("invalid content hash")
            target = safe_path(bundle, name)
            if not target.is_file() or target.stat().st_size > MAX_RESOURCE:
                raise Refusal("resource missing or exceeds bound")
            payload = target.read_bytes()
            if sha(payload) != digest:
                raise Refusal(f"resource drift: {name}")
            loaded[state + "Bytes"] = payload
        if entry["before"] == entry["after"]:
            raise Refusal("identity transition is not a mutation recipe")
        entries.append(loaded)
    return sha(data), doc, sorted(entries, key=lambda e: e["path"])


def read_state(path: Path) -> bytes | None:
    try:
        return path.read_bytes()
    except FileNotFoundError:
        return None


def root_hash(rows: list[dict[str, Any]], state: str) -> str:
    records = [{"path": row["path"], "sha256": row[state]} for row in rows]
    return sha(json.dumps(records, sort_keys=True, separators=(",", ":")).encode())


def atomic_write(path: Path, payload: bytes | None) -> None:
    if payload is None:
        path.unlink()
    else:
        path.parent.mkdir(parents=True, exist_ok=True)
        mode = stat.S_IMODE(path.stat().st_mode) if path.exists() else 0o644
        temporary: str | None = None
        try:
            with tempfile.NamedTemporaryFile(dir=path.parent, prefix=".m3-stage-", delete=False) as stream:
                temporary = stream.name
                stream.write(payload)
                stream.flush()
                os.fsync(stream.fileno())
            os.chmod(temporary, mode)
            os.replace(temporary, path)
            temporary = None
        finally:
            if temporary is not None:
                Path(temporary).unlink(missing_ok=True)
    if os.name == "posix":
        descriptor = os.open(path.parent, os.O_RDONLY | getattr(os, "O_DIRECTORY", 0))
        try:
            os.fsync(descriptor)
        finally:
            os.close(descriptor)


def replay(root: Path, bundle: Path, *, reverse: bool = False, check_only: bool = False,
           recover: bool = False, expected_recipe: str | None = None) -> dict[str, Any]:
    root = root.absolute()
    if root.is_symlink() or not root.is_dir():
        raise Refusal("checkout must be an existing nonsymlink directory")
    recipe_root, doc, entries = load_recipe(bundle)
    if expected_recipe is not None and recipe_root != expected_recipe:
        raise Refusal("recipe root differs from reviewed root")
    lock = root / ".m3-recipe-replay.lock"
    try:
        fd = os.open(lock, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
    except FileExistsError as error:
        raise Refusal("exclusive recipe lock exists; do not remove while another process may be running") from error
    try:
        with os.fdopen(fd, "w", encoding="ascii") as stream:
            stream.write(recipe_root + "\n")
        rows: list[dict[str, Any]] = []
        states: set[str] = set()
        for entry in entries:
            path = safe_path(root, entry["path"])
            current = read_state(path)
            digest = None if current is None else sha(current)
            if digest == entry["before"]:
                state = "before"
            elif digest == entry["after"]:
                state = "after"
            else:
                raise Refusal(f"source drift or missing owner: {entry['path']}")
            states.add(state)
            rows.append({"path": entry["path"], "before": entry["before"], "after": entry["after"],
                         "observed": digest, "state": state})
        if len(states) > 1 and not recover:
            raise Refusal("mixed installation: explicit sealed recovery is required")
        desired = "before" if reverse else "after"
        changed = [row for row in rows if row["state"] != desired]
        # Every path/resource has passed before the first source promotion.
        if not check_only:
            for row, entry in zip(rows, entries):
                if row["state"] == desired:
                    continue
                path = safe_path(root, row["path"])
                current = read_state(path)
                actual = None if current is None else sha(current)
                if actual != row["observed"]:
                    raise Refusal("concurrent source change; partial state must be reviewed")
                atomic_write(path, entry[desired + "Bytes"])
        return {
            "schemaVersion": 1, "recipe": doc["id"], "version": doc["version"],
            "recipeRoot": recipe_root, "direction": "rollback" if reverse else "apply",
            "status": "checked" if check_only else "fixed-point" if not changed else "applied",
            "recovery": recover, "changedPaths": [r["path"] for r in changed],
            "beforeRoot": root_hash(rows, "observed"),
            "afterRoot": root_hash(rows, "observed" if check_only else desired),
            "transitions": [{"path": r["path"], "before": r["before"], "after": r["after"]} for r in rows],
        }
    finally:
        lock.unlink(missing_ok=True)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, required=True)
    parser.add_argument("--bundle", type=Path, required=True)
    parser.add_argument("--recipe-sha256", required=True)
    parser.add_argument("--reverse", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--recover", action="store_true")
    options = parser.parse_args()
    try:
        receipt = replay(options.root, options.bundle, reverse=options.reverse,
                         check_only=options.check, recover=options.recover,
                         expected_recipe=options.recipe_sha256)
    except (Refusal, OSError, ValueError, TypeError) as error:
        parser.exit(2, "RECIPE_REFUSED " + str(error) + "\n")
    print(json.dumps(receipt, indent=2, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
