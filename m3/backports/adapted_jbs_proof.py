#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Verify the exact current-tree state for the merged adapted JBS proof wave."""

from __future__ import annotations

import argparse
import csv
import hashlib
import json
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Sequence

SCHEMA = "M3_ADAPTED_JBS_PROOF_WAVE_V1"
WAVE = Path("m3/backports/ADAPTED_JBS_PROOF_WAVE.tsv")


@dataclass(frozen=True)
class TargetReceipt:
    order: int
    work_id: str
    path: str
    role: str
    expected: str
    actual: str
    state: str


@dataclass(frozen=True)
class WorkReceipt:
    order: int
    work_id: str
    evidence_kind: str
    evidence_path: str
    recipe_test: str
    jtreg_tests: str
    linux_build: bool
    macos_build: bool
    smoke: str
    target_count: int
    target_root: str


def _rows(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError(f"empty TSV: {path}")
    return rows


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while True:
            chunk = handle.read(1024 * 1024)
            if not chunk:
                break
            digest.update(chunk)
    return digest.hexdigest()


def _truth(value: str) -> bool:
    value = value.strip().lower()
    if value == "true":
        return True
    if value == "false":
        return False
    raise ValueError(f"invalid boolean: {value!r}")


def _target_root(receipts: Sequence[TargetReceipt]) -> str:
    payload = "".join(
        "\u001f".join(
            (
                receipt.work_id,
                receipt.path,
                receipt.role,
                receipt.expected,
                receipt.actual,
                receipt.state,
            )
        )
        + "\u001e"
        for receipt in receipts
    )
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def _verify_adaptation(root: Path, order: int, work_id: str, path: Path) -> list[TargetReceipt]:
    receipts: list[TargetReceipt] = []
    for row in _rows(path):
        target = row["target_path"].strip()
        expected = row["target_sha256"].strip()
        if not expected or expected == "ABSENT":
            raise ValueError(f"{work_id}: invalid target_sha256 for {target}")
        candidates = [expected]
        superseded = row.get("superseded_target_sha256", "").strip()
        if superseded:
            candidates.append(superseded)
        file = root / target
        if not file.is_file():
            raise ValueError(f"{work_id}: target missing: {target}")
        actual = _sha256(file)
        if actual not in candidates:
            raise ValueError(
                f"{work_id}: target drift: {target}: {actual} not in {candidates}"
            )
        receipts.append(
            TargetReceipt(
                order=order,
                work_id=work_id,
                path=target,
                role=row.get("role", "").strip(),
                expected=expected,
                actual=actual,
                state="SUPERSEDED_APPROVED" if actual != expected else "TARGET_MATCH",
            )
        )
    return receipts


def _verify_hash_pinned_text(
    root: Path, order: int, work_id: str, path: Path
) -> list[TargetReceipt]:
    receipts: list[TargetReceipt] = []
    for row in _rows(path):
        target = row["path"].strip()
        expected = row["after"].strip()
        file = root / target
        if not file.is_file():
            raise ValueError(f"{work_id}: target missing: {target}")
        actual = _sha256(file)
        if actual != expected:
            raise ValueError(
                f"{work_id}: target drift: {target}: {actual} != {expected}"
            )
        receipts.append(
            TargetReceipt(
                order=order,
                work_id=work_id,
                path=target,
                role="hash-pinned-text",
                expected=expected,
                actual=actual,
                state="TARGET_MATCH",
            )
        )
    return receipts


def compile_root(root: Path) -> tuple[list[WorkReceipt], list[TargetReceipt]]:
    root = root.resolve()
    wave = _rows(root / WAVE)
    seeds = {row["jbs"]: row for row in _rows(root / "m3/backports/UPSTREAM_CHANGE_SEEDS.tsv")}
    seen: set[str] = set()
    work_receipts: list[WorkReceipt] = []
    targets: list[TargetReceipt] = []

    for expected_order, row in enumerate(wave):
        order = int(row["order"])
        work_id = row["work_id"].strip()
        if order != expected_order:
            raise ValueError("non-contiguous wave order")
        if work_id in seen:
            raise ValueError(f"duplicate wave work_id: {work_id}")
        seen.add(work_id)
        seed = seeds.get(work_id)
        if seed is None:
            raise ValueError(f"wave work_id not in UPSTREAM_CHANGE_SEEDS.tsv: {work_id}")
        disposition = seed["disposition"].strip().lower()
        if disposition not in {"admitted", "candidate-adapted"}:
            raise ValueError(f"wave work_id is not adapted/admitted: {work_id}: {disposition}")

        evidence_kind = row["evidence_kind"].strip()
        evidence_path = row["evidence_path"].strip()
        evidence = root / evidence_path
        if not evidence.is_file():
            raise ValueError(f"missing evidence: {evidence_path}")
        if evidence_kind == "ADAPTATION":
            current = _verify_adaptation(root, order, work_id, evidence)
        elif evidence_kind == "HASH_PINNED_TEXT":
            current = _verify_hash_pinned_text(root, order, work_id, evidence)
        else:
            raise ValueError(f"unknown evidence_kind: {evidence_kind}")

        recipe_test = row["recipe_test"].strip()
        if not recipe_test:
            raise ValueError(f"{work_id}: recipe_test required")
        tests = [value for value in row["jtreg_tests"].split(";") if value]
        for test in tests:
            if not (root / test).is_file():
                raise ValueError(f"{work_id}: jtreg test missing: {test}")
        smoke = row["smoke"].strip()
        if smoke and not (root / smoke).is_file():
            raise ValueError(f"{work_id}: smoke proof missing: {smoke}")

        targets.extend(current)
        work_receipts.append(
            WorkReceipt(
                order=order,
                work_id=work_id,
                evidence_kind=evidence_kind,
                evidence_path=evidence_path,
                recipe_test=recipe_test,
                jtreg_tests=";".join(tests),
                linux_build=_truth(row["linux_build"]),
                macos_build=_truth(row["macos_build"]),
                smoke=smoke,
                target_count=len(current),
                target_root=_target_root(current),
            )
        )

    return work_receipts, targets


def root_hash(
    work_receipts: Sequence[WorkReceipt], targets: Sequence[TargetReceipt]
) -> str:
    digest = hashlib.sha256()
    digest.update(SCHEMA.encode("utf-8"))
    for row in work_receipts:
        digest.update(repr(asdict(row)).encode("utf-8"))
    for row in targets:
        digest.update(repr(asdict(row)).encode("utf-8"))
    return digest.hexdigest()


def write_tsv(rows: Sequence[object], path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    if not rows:
        raise ValueError("empty receipt")
    first = asdict(rows[0])
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=list(first),
            delimiter="\t",
            lineterminator="\n",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow(asdict(row))


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(argv)

    work, targets = compile_root(args.root)
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    write_tsv(work, out / "work.tsv")
    write_tsv(targets, out / "targets.tsv")
    payload = {
        "schema": SCHEMA,
        "root": root_hash(work, targets),
        "work_items": len(work),
        "targets": len(targets),
        "linux_build_items": sum(row.linux_build for row in work),
        "macos_build_items": sum(row.macos_build for row in work),
        "product_write_authority": False,
        "promotion_authority": False,
        "compatibility_authority": False,
        "rows": [asdict(row) for row in work],
    }
    (out / "summary.json").write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    print(
        "M3_ADAPTED_JBS_PROOF_WAVE_OK "
        f"work_items={payload['work_items']} targets={payload['targets']} "
        f"root={payload['root']} productWriteAuthority=false promotionAuthority=false"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
