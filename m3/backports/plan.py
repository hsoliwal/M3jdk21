#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Generate the deterministic M3 JDK21 backport work queue."""

from __future__ import annotations

import argparse
import csv
from dataclasses import dataclass
from io import StringIO
from pathlib import Path
from typing import Iterable, Sequence


@dataclass(frozen=True)
class WorkItem:
    source_type: str
    release: int
    identity: str
    title: str
    domain: str
    disposition: str
    current_pass: int
    action: str
    required_scope: str
    risk: str
    dependency_or_commit: str
    reason: str


HEADER = (
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
)


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


def required_scope(domain: str) -> str:
    value = domain.lower()
    if any(token in value for token in ("language", "removal", "compatibility")):
        return "LIBRARY_API"
    if any(
        token in value
        for token in (
            "native",
            "hotspot",
            "gc",
            "runtime-aot",
            "vm-gc",
            "library-runtime-native",
        )
    ):
        return "MULTI_MODULE"
    return "MODULE"


def classify(disposition: str) -> tuple[int, str, str]:
    if disposition == "reject-language":
        return (1, "EXCLUDE_LANGUAGE", "INCOMPATIBLE")
    if disposition == "reject-compat":
        return (1, "EXCLUDE_COMPAT", "INCOMPATIBLE")
    if disposition.startswith("superseded"):
        return (1, "REDIRECT_SUPERSEDED", "SUPERSEDED")
    if disposition.startswith("hold-"):
        return (2, "HOLD_DEPENDENCY_OR_POLICY", "HOLD")
    if disposition == "candidate-high-risk":
        return (2, "INVESTIGATE_DEPENDENCY_CLOSURE", "HIGH")
    if disposition == "candidate":
        return (2, "INVESTIGATE_DEPENDENCY_CLOSURE", "NORMAL")
    if disposition == "admitted":
        return (5, "VERIFY_MATERIALIZED", "ADMITTED_UNVERIFIED")
    return (1, "REVIEW", "UNKNOWN")


def build(root: Path) -> list[WorkItem]:
    base = root / "m3/backports"
    result: list[WorkItem] = []

    for row in read_tsv(base / "JEP_CATALOGUE.tsv"):
        current_pass, action, risk = classify(row["disposition"])
        result.append(
            WorkItem(
                source_type="JEP",
                release=int(row["release"]),
                identity=f"JEP-{row['jep']}",
                title=row["title"],
                domain=row["domain"],
                disposition=row["disposition"],
                current_pass=current_pass,
                action=action,
                required_scope=required_scope(row["domain"]),
                risk=risk,
                dependency_or_commit=row["superseded_by"],
                reason=row["reason"],
            )
        )

    for row in read_tsv(base / "UPSTREAM_CHANGE_SEEDS.tsv"):
        current_pass, action, risk = classify(row["disposition"])
        result.append(
            WorkItem(
                source_type="JBS",
                release=int(row["release"]),
                identity=row["jbs"],
                title=row["title"],
                domain=row["component"],
                disposition=row["disposition"],
                current_pass=current_pass,
                action=action,
                required_scope=required_scope(row["component"]),
                risk=risk,
                dependency_or_commit=row["upstream_commit"],
                reason=row["reason"],
            )
        )

    return sorted(
        result,
        key=lambda item: (item.release, item.source_type, item.identity),
    )


def render(items: Iterable[WorkItem]) -> str:
    output = StringIO()
    writer = csv.writer(output, delimiter="\t", lineterminator="\n")
    writer.writerow(HEADER)
    for item in items:
        writer.writerow(
            (
                item.source_type,
                item.release,
                item.identity,
                item.title,
                item.domain,
                item.disposition,
                item.current_pass,
                item.action,
                item.required_scope,
                item.risk,
                item.dependency_or_commit,
                item.reason,
            )
        )
    return output.getvalue()


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parents[2],
        help="M3Jdk21 repository root",
    )
    parser.add_argument(
        "--out",
        type=Path,
        help="destination; stdout when omitted",
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args([] if argv is None else argv)
    root = args.root.resolve()
    text = render(build(root))
    if args.out is None:
        print(text, end="")
    else:
        destination = args.out.resolve()
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text(text, encoding="utf-8", newline="")
    return 0


if __name__ == "__main__":
    import sys

    raise SystemExit(main(sys.argv[1:]))
