# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Exact Git-tree retention audit for explicitly retained source-sealed capabilities."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import sys

from git_graph import GitGraph, exact_id


def safe_path(value: str) -> str:
    if (
        not isinstance(value, str)
        or not value
        or value.startswith("/")
        or "\\" in value
        or "\0" in value
        or any(part in {"", ".", "..", ".git"} for part in value.split("/"))
    ):
        raise ValueError("noncanonical repository-relative path")
    return value


def tsv(data: bytes) -> list[dict[str, str]]:
    text = data.decode("utf-8")
    reader = csv.DictReader(io.StringIO(text), delimiter="\t")
    if reader.fieldnames is None:
        raise ValueError("TSV header required")
    return list(reader)


def blob(git: GitGraph, commit: str, path: str) -> bytes | None:
    encoded = safe_path(path).encode("utf-8")
    entry = git.entries(commit, {encoded}).get(encoded)
    if entry is None:
        return None
    mode, kind, oid = entry
    if kind != "blob" or mode == "160000":
        raise ValueError("retention target is not a regular blob: " + path)
    return git.run("cat-file", "blob", exact_id(oid))


def load_capabilities(git: GitGraph, commit: str, path: str) -> list[tuple[str, str]]:
    data = blob(git, commit, path)
    if data is None:
        raise ValueError("retained capability catalogue missing")
    rows = tsv(data)
    if rows and set(rows[0]) != {"capability_id", "manifest_path"}:
        raise ValueError("invalid retained capability catalogue header")
    result: list[tuple[str, str]] = []
    seen: set[str] = set()
    for row in rows:
        capability = row["capability_id"].strip()
        manifest = safe_path(row["manifest_path"].strip())
        if not capability or capability in seen:
            raise ValueError("duplicate/blank retained capability id")
        seen.add(capability)
        result.append((capability, manifest))
    if not result:
        raise ValueError("retained capability catalogue is empty")
    return result


def load_manifest(git: GitGraph, commit: str, path: str) -> list[tuple[str, str]]:
    data = blob(git, commit, path)
    if data is None:
        raise ValueError("retained manifest missing: " + path)
    rows: list[tuple[str, str]] = []
    previous = ""
    for raw in data.decode("utf-8").splitlines():
        if not raw or raw.startswith("#"):
            continue
        cells = raw.split("\t")
        if len(cells) != 4:
            raise ValueError("invalid source-sealed manifest row: " + path)
        target = safe_path(cells[0])
        expected = cells[2]
        if previous >= target or not expected.isascii() or not expected.islower() or len(expected) != 64:
            raise ValueError("invalid/sorted source-sealed manifest: " + path)
        try:
            bytes.fromhex(expected)
        except ValueError as failure:
            raise ValueError("invalid postimage SHA-256: " + target) from failure
        previous = target
        rows.append((target, expected))
    if not rows:
        raise ValueError("empty source-sealed manifest: " + path)
    return rows


def inspect(repo: Path, commit: str, catalogue: str) -> dict:
    git = GitGraph(repo)
    checked_commit = git.commit_id(commit)
    initial_refs = git.initial_refs
    rows = []
    capabilities = load_capabilities(git, checked_commit, catalogue)
    for capability, manifest in capabilities:
        for target, expected in load_manifest(git, checked_commit, manifest):
            content = blob(git, checked_commit, target)
            if content is None:
                actual = "ABSENT"
                state = "MISSING"
            else:
                actual = hashlib.sha256(content).hexdigest()
                state = "RETAINED_EXACT" if actual == expected else "PRESENT_DRIFTED_REVIEW"
            rows.append(
                {
                    "capability_id": capability,
                    "manifest_path": manifest,
                    "path": target,
                    "expected_sha256": expected,
                    "actual_sha256": actual,
                    "state": state,
                }
            )
    if git.refs() != initial_refs:
        raise ValueError("refs moved during retention audit")
    rows.sort(key=lambda row: (row["capability_id"], row["path"]))
    summary = {
        "commit": checked_commit,
        "capabilities": len(capabilities),
        "targets": len(rows),
        "retained_exact": sum(row["state"] == "RETAINED_EXACT" for row in rows),
        "present_drifted_review": sum(row["state"] == "PRESENT_DRIFTED_REVIEW" for row in rows),
        "missing": sum(row["state"] == "MISSING" for row in rows),
        "rows": rows,
    }
    root = hashlib.sha256(
        json.dumps(summary, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()
    summary["root"] = root
    return summary


def write(summary: dict, output: Path) -> None:
    if output.exists() or output.is_symlink():
        raise ValueError("retention output must not already exist")
    output.mkdir(parents=True, exist_ok=False)
    rows = summary["rows"]
    with (output / "RETENTION.tsv").open("x", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(
            stream,
            delimiter="\t",
            lineterminator="\n",
            fieldnames=[
                "capability_id",
                "manifest_path",
                "path",
                "expected_sha256",
                "actual_sha256",
                "state",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)
    public_summary = {key: value for key, value in summary.items() if key != "rows"}
    (output / "SUMMARY.json").write_text(
        json.dumps(public_summary, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    (output / "STATE_ROOT.sha256").write_text(summary["root"] + "\n", encoding="ascii")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument(
        "--catalogue",
        default="m3/history/RETAINED_CAPABILITIES.tsv",
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--require-no-missing", action="store_true")
    args = parser.parse_args()
    try:
        summary = inspect(args.repo, args.commit, args.catalogue)
        write(summary, args.output)
        print(json.dumps({key: value for key, value in summary.items() if key != "rows"}, sort_keys=True))
        if args.require_no_missing and summary["missing"]:
            print("RETENTION_REFUSED: retained targets are missing", file=sys.stderr)
            return 3
        return 0
    except (ValueError, OSError, UnicodeError) as failure:
        print("RETENTION_REFUSED: " + str(failure), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
