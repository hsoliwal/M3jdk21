#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Validate curated best-of-breed capability joins against the canonical fork inventory."""

from __future__ import annotations

import argparse
import csv
import re
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

ALLOWED_STATUS = {
    "PENDING_COMPATIBILITY_PROOF",
    "PENDING_BRANCH_PROOF",
    "HOLD_MISSING_BACKEND",
}
PRIORITY = {"HIGH": 0, "MEDIUM": 1, "LOW_MEDIUM": 2, "LOW": 3}


@dataclass(frozen=True)
class Capability:
    capability_id: str
    fork_id: str
    evidence_commit: str
    capability: str
    plane: str
    priority: str
    status: str
    source_copy_authority: bool
    selected_for_distribution: bool
    join_keys: str
    next_proof: str


def _bool(value: str) -> bool:
    checked = value.strip().lower()
    if checked == "true":
        return True
    if checked == "false":
        return False
    raise ValueError("invalid boolean: " + value)


def read_fork_ids(path: Path) -> set[str]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    ids = {row["fork_id"].strip() for row in rows}
    if not ids or "" in ids:
        raise ValueError("invalid fork catalogue")
    return ids


def read_capabilities(path: Path, fork_ids: set[str]) -> list[Capability]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    result: list[Capability] = []
    seen: set[str] = set()
    for row in rows:
        capability_id = row["capability_id"].strip()
        fork_id = row["fork_id"].strip()
        commit = row["evidence_commit"].strip().lower()
        status = row["status"].strip()
        priority = row["priority"].strip()
        source_copy = _bool(row["source_copy_authority"])
        selected = _bool(row["selected_for_distribution"])
        if not capability_id or capability_id in seen:
            raise ValueError("duplicate/blank capability_id: " + repr(capability_id))
        if fork_id not in fork_ids:
            raise ValueError("unknown fork_id: " + fork_id)
        if not re.fullmatch(r"[0-9a-f]{40}", commit):
            raise ValueError("invalid evidence_commit: " + commit)
        if status not in ALLOWED_STATUS:
            raise ValueError("unsupported status: " + status)
        if priority not in PRIORITY:
            raise ValueError("unsupported priority: " + priority)
        if source_copy or selected:
            raise ValueError("capability join cannot grant source-copy/distribution authority")
        seen.add(capability_id)
        result.append(
            Capability(
                capability_id,
                fork_id,
                commit,
                row["capability"].strip(),
                row["plane"].strip(),
                priority,
                status,
                False,
                False,
                row["join_keys"].strip(),
                row["next_proof"].strip(),
            )
        )
    if not result:
        raise ValueError("empty fork capability catalogue")
    return result


def read_unique_changes(path: Path) -> set[tuple[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return {
            (row["fork_id"], row["commit"])
            for row in csv.DictReader(handle, delimiter="\t")
        }


def _git(repo: Path, *args: str) -> str:
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
    )
    if proc.returncode:
        raise ValueError("git " + " ".join(args) + ": " + proc.stderr.strip())
    return proc.stdout.strip()


def verify(
    repo: Path,
    capabilities: Sequence[Capability],
    unique_changes: set[tuple[str, str]],
) -> list[tuple[Capability, bool]]:
    result: list[tuple[Capability, bool]] = []
    for row in capabilities:
        remote = "refs/remotes/m3/" + row.fork_id
        merge_base = _git(repo, "merge-base", row.evidence_commit, remote)
        if merge_base != row.evidence_commit:
            raise ValueError(
                row.capability_id + ": evidence commit is not reachable from pinned fork"
            )
        unique = (row.fork_id, row.evidence_commit) in unique_changes
        if row.status == "PENDING_COMPATIBILITY_PROOF" and not unique:
            raise ValueError(
                row.capability_id + ": product candidate is not fork-unique evidence"
            )
        result.append((row, unique))
    return sorted(result, key=lambda item: (PRIORITY[item[0].priority], item[0].capability_id))


def write_tsv(rows: Sequence[tuple[Capability, bool]], out) -> None:
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow((
        "order", "capability_id", "fork_id", "evidence_commit", "capability", "plane",
        "priority", "status", "fork_unique_evidence", "source_copy_authority",
        "selected_for_distribution", "join_keys", "next_proof",
    ))
    for order, (row, unique) in enumerate(rows):
        writer.writerow((
            order, row.capability_id, row.fork_id, row.evidence_commit, row.capability,
            row.plane, row.priority, row.status, str(unique).lower(), "false", "false",
            row.join_keys, row.next_proof,
        ))


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--fork-catalog", type=Path, required=True)
    parser.add_argument("--capabilities", type=Path, required=True)
    parser.add_argument("--changes", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)
    capabilities = read_capabilities(args.capabilities, read_fork_ids(args.fork_catalog))
    rows = verify(args.repo, capabilities, read_unique_changes(args.changes))
    args.out.parent.mkdir(parents=True, exist_ok=True)
    with args.out.open("w", encoding="utf-8", newline="") as handle:
        write_tsv(rows, handle)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
