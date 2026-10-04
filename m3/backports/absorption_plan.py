#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Join M3 source convergence with complete post-21 feature/commit absorption evidence.

This is planning evidence only. It grants no source mutation, donor-copy, compatibility-admission
or promotion authority.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Mapping, Sequence

_CONVERGED = {"CONVERGED_CHANGED", "CONVERGED_UNCHANGED"}
_HEX = frozenset("0123456789abcdef")


@dataclass(frozen=True)
class Convergence:
    path: str
    fixed_point: bool
    status: str


@dataclass(frozen=True)
class PlanRow:
    ordinal: int
    row_type: str
    release: int
    identity: str
    source_ref: str
    domain: str
    disposition: str
    risk: str
    required_scope: str
    current_stage: str
    next_action: str
    java_path_count: int
    java_converged_count: int
    java_hold_count: int
    java_missing_count: int
    non_java_path_count: int
    baseline_state: str
    source_convergence_root: str
    mutation_authority: bool = False
    promotion_authority: bool = False


def _sha256_bytes(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def _sha(value: str, field: str) -> str:
    checked = value.strip().lower()
    if len(checked) != 64 or any(char not in _HEX for char in checked):
        raise ValueError(f"invalid {field}: {value!r}")
    return checked


def _strict_bool(value: str, field: str) -> bool:
    checked = value.strip().lower()
    if checked == "true":
        return True
    if checked == "false":
        return False
    raise ValueError(f"invalid {field}: {value!r}")


def _canonical_path(value: str) -> str:
    path = value.strip().replace("\\", "/")
    if not path or path.startswith("/") or "\x00" in path:
        raise ValueError(f"invalid repository path: {value!r}")
    parts = path.split("/")
    if any(part in {"", ".", ".."} for part in parts):
        raise ValueError(f"invalid repository path: {value!r}")
    return path


def _read_tsv(path: Path, required: set[str]) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty TSV: {path}")
    missing = required - set(rows[0])
    if missing:
        raise ValueError(f"{path.name} missing columns: {sorted(missing)}")
    return rows


def load_convergence(path: Path) -> tuple[dict[str, Convergence], str]:
    payload = path.read_bytes()
    rows = _read_tsv(
        path,
        {
            "path",
            "preSha256",
            "postSha256",
            "fixedPoint",
            "status",
            "candidate",
        },
    )
    result: dict[str, Convergence] = {}
    changed = 0
    holds = 0
    for raw in rows:
        target = _canonical_path(raw["path"])
        if not target.startswith("src/") or not target.endswith(".java"):
            raise ValueError(f"non-Java source convergence row: {target}")
        if target in result:
            raise ValueError(f"duplicate convergence row: {target}")
        _sha(raw["preSha256"], "preSha256")
        _sha(raw["postSha256"], "postSha256")
        fixed = _strict_bool(raw["fixedPoint"], "fixedPoint")
        status = raw["status"].strip()
        if status in _CONVERGED:
            if not fixed:
                raise ValueError(f"converged row is not fixed point: {target}")
            if status == "CONVERGED_CHANGED":
                changed += 1
                if not raw["candidate"].strip():
                    raise ValueError(f"changed row has no candidate: {target}")
            elif raw["candidate"].strip():
                raise ValueError(f"unchanged row has candidate: {target}")
        elif status == "HOLD":
            holds += 1
            if fixed:
                raise ValueError(f"HOLD row cannot be fixed point: {target}")
        else:
            raise ValueError(f"unknown convergence status {status!r}: {target}")
        result[target] = Convergence(target, fixed, status)

    semantic_root = _sha256_bytes(payload)
    summary = path.with_name("SOURCE_CONVERGENCE.summary.tsv")
    if summary.is_file():
        summary_rows = _read_tsv(
            summary, {"files", "changed", "holds", "semanticRoot"}
        )
        if len(summary_rows) != 1:
            raise ValueError("SOURCE_CONVERGENCE summary must contain one row")
        row = summary_rows[0]
        if (
            int(row["files"]) != len(result)
            or int(row["changed"]) != changed
            or int(row["holds"]) != holds
            or _sha(row["semanticRoot"], "semanticRoot") != semantic_root
        ):
            raise ValueError("SOURCE_CONVERGENCE summary drift")
    return result, semantic_root


def _feature_baseline(row: Mapping[str, str]) -> str:
    action = row["action"].strip()
    disposition = row["disposition"].strip()
    if disposition.startswith("reject-") or action.startswith("EXCLUDE_"):
        return "EXCLUDED_BY_JAVA21_CONTRACT"
    if disposition.startswith("superseded") or action == "REDIRECT_SUPERSEDED":
        return "SUPERSEDED_LINEAGE"
    return "AWAIT_FILE_ATOMS"


def feature_rows(
    path: Path, convergence_root: str, start: int = 0
) -> list[PlanRow]:
    rows = _read_tsv(
        path,
        {
            "source_type",
            "release",
            "identity",
            "title",
            "domain",
            "disposition",
            "current_pass",
            "action",
            "required_scope",
            "risk",
            "dependency_or_commit",
            "reason",
        },
    )
    seen: set[tuple[str, str]] = set()
    result: list[PlanRow] = []
    for offset, raw in enumerate(rows):
        key = (raw["source_type"].strip(), raw["identity"].strip())
        if key in seen:
            raise ValueError(f"duplicate feature queue identity: {key}")
        seen.add(key)
        result.append(
            PlanRow(
                ordinal=start + offset,
                row_type="FEATURE",
                release=int(raw["release"]),
                identity=raw["identity"].strip(),
                source_ref=raw["dependency_or_commit"].strip(),
                domain=raw["domain"].strip(),
                disposition=raw["disposition"].strip(),
                risk=raw["risk"].strip(),
                required_scope=raw["required_scope"].strip(),
                current_stage=f"PASS_{int(raw['current_pass'])}",
                next_action=raw["action"].strip(),
                java_path_count=0,
                java_converged_count=0,
                java_hold_count=0,
                java_missing_count=0,
                non_java_path_count=0,
                baseline_state=_feature_baseline(raw),
                source_convergence_root=convergence_root,
            )
        )
    return result


def _commit_baseline(
    paths: Sequence[str], convergence: Mapping[str, Convergence]
) -> tuple[int, int, int, int, int, str]:
    java_paths = [
        path
        for path in paths
        if path.startswith("src/") and path.endswith(".java")
    ]
    non_java = len(paths) - len(java_paths)
    converged = 0
    holds = 0
    missing = 0
    for path in java_paths:
        row = convergence.get(path)
        if row is None:
            missing += 1
        elif row.status in _CONVERGED and row.fixed_point:
            converged += 1
        else:
            holds += 1

    if not java_paths:
        state = "NO_JAVA_SOURCE"
    elif missing:
        state = "JAVA_BASELINE_MISSING"
    elif holds:
        state = "JAVA_BASELINE_HOLD"
    else:
        state = "JAVA_BASELINE_CONVERGED"

    return len(java_paths), converged, holds, missing, non_java, state


def commit_rows(
    path: Path,
    convergence: Mapping[str, Convergence],
    convergence_root: str,
    start: int,
) -> list[PlanRow]:
    rows = _read_tsv(
        path,
        {
            "order",
            "release",
            "commit",
            "jbs_ids",
            "subject",
            "domain",
            "inventory_disposition",
            "risk",
            "scope_floor",
            "proof_lane",
            "recipe_strategy",
            "priority",
            "compatibility_state",
            "next_action",
            "paths",
        },
    )
    seen: set[str] = set()
    prepared: list[tuple[int, dict[str, str]]] = []
    for raw in rows:
        commit = raw["commit"].strip().lower()
        if len(commit) != 40 or any(char not in _HEX for char in commit):
            raise ValueError(f"invalid upstream commit: {commit!r}")
        if commit in seen:
            raise ValueError(f"duplicate upstream commit: {commit}")
        seen.add(commit)
        prepared.append((int(raw["order"]), raw))
    prepared.sort(key=lambda item: item[0])
    if [order for order, _raw in prepared] != list(range(len(prepared))):
        raise ValueError("compatibility queue order is not contiguous")

    result: list[PlanRow] = []
    for offset, (_order, raw) in enumerate(prepared):
        paths = tuple(
            _canonical_path(value)
            for value in raw["paths"].split(",")
            if value.strip()
        )
        (
            java_count,
            converged,
            holds,
            missing,
            non_java,
            baseline_state,
        ) = _commit_baseline(paths, convergence)
        result.append(
            PlanRow(
                ordinal=start + offset,
                row_type="COMMIT",
                release=int(raw["release"]),
                identity=raw["commit"].strip().lower(),
                source_ref=raw["jbs_ids"].strip(),
                domain=raw["domain"].strip(),
                disposition=raw["inventory_disposition"].strip(),
                risk=raw["risk"].strip(),
                required_scope=raw["scope_floor"].strip(),
                current_stage=raw["compatibility_state"].strip(),
                next_action=raw["next_action"].strip(),
                java_path_count=java_count,
                java_converged_count=converged,
                java_hold_count=holds,
                java_missing_count=missing,
                non_java_path_count=non_java,
                baseline_state=baseline_state,
                source_convergence_root=convergence_root,
            )
        )
    return result


def build_plan(
    source_convergence: Path,
    feature_queue: Path,
    commit_queue: Path,
) -> tuple[list[PlanRow], str]:
    convergence, convergence_root = load_convergence(source_convergence)
    features = feature_rows(feature_queue, convergence_root)
    commits = commit_rows(
        commit_queue, convergence, convergence_root, start=len(features)
    )
    return features + commits, convergence_root


_FIELDS = (
    "ordinal",
    "row_type",
    "release",
    "identity",
    "source_ref",
    "domain",
    "disposition",
    "risk",
    "required_scope",
    "current_stage",
    "next_action",
    "java_path_count",
    "java_converged_count",
    "java_hold_count",
    "java_missing_count",
    "non_java_path_count",
    "baseline_state",
    "source_convergence_root",
    "mutation_authority",
    "promotion_authority",
)


def render(rows: Sequence[PlanRow]) -> str:
    from io import StringIO

    out = StringIO()
    writer = csv.writer(out, delimiter="\t", lineterminator="\n")
    writer.writerow(_FIELDS)
    for row in rows:
        writer.writerow(
            (
                row.ordinal,
                row.row_type,
                row.release,
                row.identity,
                row.source_ref,
                row.domain,
                row.disposition,
                row.risk,
                row.required_scope,
                row.current_stage,
                row.next_action,
                row.java_path_count,
                row.java_converged_count,
                row.java_hold_count,
                row.java_missing_count,
                row.non_java_path_count,
                row.baseline_state,
                row.source_convergence_root,
                str(row.mutation_authority).lower(),
                str(row.promotion_authority).lower(),
            )
        )
    return out.getvalue()


def write(
    rows: Sequence[PlanRow],
    convergence_root: str,
    output: Path,
    summary_output: Path,
    root_output: Path,
) -> str:
    payload = render(rows)
    semantic_root = _sha256_bytes(payload.encode("utf-8"))
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(payload, encoding="utf-8")

    by_type = Counter(row.row_type for row in rows)
    by_baseline = Counter(row.baseline_state for row in rows)
    summary = {
        "schema": 1,
        "rows": len(rows),
        "feature_rows": by_type.get("FEATURE", 0),
        "commit_rows": by_type.get("COMMIT", 0),
        "source_convergence_root": convergence_root,
        "semantic_root": semantic_root,
        "by_baseline_state": dict(sorted(by_baseline.items())),
        "mutation_authority": False,
        "promotion_authority": False,
    }
    summary_output.parent.mkdir(parents=True, exist_ok=True)
    summary_output.write_text(
        json.dumps(summary, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    root_output.parent.mkdir(parents=True, exist_ok=True)
    root_output.write_text(semantic_root + "\n", encoding="utf-8")
    return semantic_root


def _parse_args(argv: Sequence[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-convergence", type=Path, required=True)
    parser.add_argument("--feature-queue", type=Path, required=True)
    parser.add_argument("--commit-queue", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--summary-out", type=Path, required=True)
    parser.add_argument("--root-out", type=Path, required=True)
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(argv)
    rows, convergence_root = build_plan(
        args.source_convergence.resolve(),
        args.feature_queue.resolve(),
        args.commit_queue.resolve(),
    )
    root = write(
        rows,
        convergence_root,
        args.out.resolve(),
        args.summary_out.resolve(),
        args.root_out.resolve(),
    )
    print(
        "M3_JDK21_ABSORPTION_PLAN"
        f"\tPASS\trows={len(rows)}\troot={root}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
