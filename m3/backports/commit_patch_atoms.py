#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Materialize exact upstream commit deltas as target-side FILE patch candidates.

This tool never mutates the target tree. It writes one upstream patch per selected path and,
where mechanically applicable, a candidate postimage plus a hash ledger. Renames/deletions remain
explicit review operations; conflicts are typed rather than guessed.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import subprocess
import tempfile
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

from patch_atom_io import relative_git_path, safe_output_root, target_blob, write_exact


@dataclass(frozen=True)
class Change:
    status: str
    path: str
    previous_path: str = ""


def git(repo: Path, *args: str, data: bytes | None = None) -> bytes:
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        input=data,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if proc.returncode:
        raise RuntimeError(
            f"git {' '.join(args)} failed: " + proc.stderr.decode("utf-8", "replace").strip()
        )
    return proc.stdout


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def changes(repo: Path, parent: str, commit: str) -> dict[str, Change]:
    """Read NUL-delimited Git status; quoting/newlines cannot alias another FILE atom."""
    raw = git(repo, "diff", "--name-status", "-z", "-M", parent, commit)
    if raw and not raw.endswith(b"\0"):
        raise ValueError("unterminated Git name-status stream")
    tokens = raw.decode("utf-8", "strict").split("\0")[:-1] if raw else []
    result: dict[str, Change] = {}
    index = 0
    while index < len(tokens):
        status = tokens[index]
        index += 1
        if not status:
            raise ValueError("empty Git change status")
        kind = status[0]
        needed = 2 if kind in ("R", "C") else 1
        if index + needed > len(tokens):
            raise ValueError("truncated Git change status")
        if needed == 2:
            previous = relative_git_path(tokens[index])
            path = relative_git_path(tokens[index + 1])
            index += 2
            change = Change(status, path, previous)
        else:
            path = relative_git_path(tokens[index])
            index += 1
            change = Change(status, path)
        if path in result:
            raise ValueError(f"duplicate changed path: {path}")
        result[path] = change
    return result


def read_paths(path: Path) -> list[str]:
    rows = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if rows != sorted(rows) or len(set(rows)) != len(rows):
        raise ValueError("paths must be sorted and unique")
    return [relative_git_path(row) for row in rows]


def object_bytes(repo: Path, ref: str, path: str) -> bytes:
    return git(repo, "show", f"{ref}:{path}")


def patch_bytes(repo: Path, parent: str, commit: str, change: Change) -> bytes:
    paths = [change.previous_path, change.path] if change.previous_path else [change.path]
    return git(repo, "diff", "--binary", parent, commit, "--", *paths)


def apply_patch(current: bytes, path: str, patch: bytes) -> bytes | None:
    with tempfile.TemporaryDirectory() as temp:
        root = Path(temp)
        subprocess.run(("git", "init", "-q", str(root)), check=True)
        subprocess.run(("git", "-C", str(root), "config", "user.email", "fixture@example.invalid"), check=True)
        subprocess.run(("git", "-C", str(root), "config", "user.name", "M3 Patch Atom"), check=True)
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(current)
        subprocess.run(("git", "-C", str(root), "add", "--", path), check=True)
        subprocess.run(("git", "-C", str(root), "commit", "-q", "-m", "target preimage"), check=True)

        check = subprocess.run(
            ("git", "-C", str(root), "apply", "--check", "--whitespace=nowarn", "-"),
            input=patch,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if check.returncode:
            return None
        applied = subprocess.run(
            ("git", "-C", str(root), "apply", "--whitespace=nowarn", "-"),
            input=patch,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=False,
        )
        if applied.returncode:
            raise RuntimeError(applied.stderr.decode("utf-8", "replace"))
        return target.read_bytes()


def materialize(
    donor_repo: Path,
    target_root: Path,
    parent: str,
    commit: str,
    selected_paths: Iterable[str],
    out: Path,
) -> list[dict[str, str]]:
    selected = [relative_git_path(path) for path in selected_paths]
    if selected != sorted(set(selected)) or not selected:
        raise ValueError("selected paths must be nonempty, sorted, unique")
    all_changes = changes(donor_repo, parent, commit)
    missing = sorted(set(selected) - set(all_changes))
    if missing:
        raise ValueError(f"selected paths absent from commit delta: {missing[:5]}")

    out = safe_output_root(out, donor_repo, target_root)

    rows: list[dict[str, str]] = []
    for ordinal, path in enumerate(selected, 1):
        change = all_changes[path]
        patch = patch_bytes(donor_repo, parent, commit, change)
        patch_name = f"{ordinal:04d}.patch"
        write_exact(out, f"patches/{patch_name}", patch)

        kind = change.status[0]
        target_exists, target_bytes = target_blob(target_root, path)
        before_hash = sha256(target_bytes) if target_exists else "ABSENT"
        candidate: bytes | None = None
        disposition = ""
        note = ""

        if kind == "M":
            upstream_before = object_bytes(donor_repo, parent, path)
            upstream_after = object_bytes(donor_repo, commit, path)
            if not target_exists:
                disposition = "CONFLICT_MISSING_TARGET"
            elif target_bytes == upstream_before:
                candidate = upstream_after
                disposition = "EXACT_PARENT_REPLAY"
            else:
                candidate = apply_patch(target_bytes, path, patch)
                disposition = "CONTEXT_PATCH_APPLIED" if candidate is not None else "CONFLICT_CONTEXT"
        elif kind == "A":
            upstream_after = object_bytes(donor_repo, commit, path)
            if not target_exists:
                candidate = upstream_after
                disposition = "ADD_ABSENT_TARGET"
            elif target_bytes == upstream_after:
                candidate = target_bytes
                disposition = "ALREADY_CONVERGED"
            else:
                disposition = "CONFLICT_OCCUPIED_ADD"
        elif kind == "D":
            upstream_before = object_bytes(donor_repo, parent, path)
            if not target_exists:
                disposition = "ALREADY_REMOVED"
            elif target_bytes == upstream_before:
                disposition = "REVIEW_DELETE_EXACT"
            else:
                disposition = "CONFLICT_DELETE_DRIFT"
        elif kind == "R":
            disposition = "REVIEW_RENAME"
            note = change.previous_path
        else:
            disposition = "REVIEW_UNSUPPORTED_STATUS"

        after_hash = ""
        if candidate is not None:
            write_exact(out, f"candidates/{path}", candidate)
            after_hash = sha256(candidate)

        rows.append(
            {
                "path": path,
                "upstream_status": change.status,
                "previous_path": change.previous_path,
                "target_before_sha256": before_hash,
                "candidate_after_sha256": after_hash,
                "disposition": disposition,
                "patch": f"patches/{patch_name}",
                "note": note,
            }
        )

    fields = [
        "path",
        "upstream_status",
        "previous_path",
        "target_before_sha256",
        "candidate_after_sha256",
        "disposition",
        "patch",
        "note",
    ]
    handle = io.StringIO(newline="")
    writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(rows)
    write_exact(out, "APPLICABILITY.tsv", handle.getvalue().encode("utf-8"))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--donor-repo", type=Path, required=True)
    parser.add_argument("--target-root", type=Path, required=True)
    parser.add_argument("--parent", required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument("--paths", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    rows = materialize(
        args.donor_repo.resolve(),
        args.target_root.resolve(),
        args.parent,
        args.commit,
        read_paths(args.paths),
        args.out.resolve(),
    )
    print(f"M3_PATCH_ATOMS={len(rows)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
