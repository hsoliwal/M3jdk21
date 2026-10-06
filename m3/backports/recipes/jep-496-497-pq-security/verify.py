#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed verifier for the JEP496/497 Java21 dependency inventory."""

from __future__ import annotations

import csv
from pathlib import Path

EXPECTED = {
    "JDK-8318096": (0, 18, 18),
    "JDK-8340327": (10, 0, 10),
    "JEP-496": (8, 2, 10),
    "JEP-497": (8, 2, 10),
}
EXPECTED_COMMITS = {
    "JDK-8318096": "9123961aaa47aa58ec436640590d2cceedb8cbb1",
    "JDK-8340327": "3f53d571343792341481f4d15970cdc0bcd76a5e",
    "JEP-496": "13987b4244614d594dc8f94c288eddb6239a066f",
    "JEP-497": "8b98f958dc1afedc02b9d9c98089d6cb1ca3a5b7",
}


def read(path: Path) -> list[dict[str, str]]:
    with path.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise AssertionError(f"empty TSV: {path}")
    return rows


def verify(packet: Path) -> None:
    graph = read(packet / "DEPENDENCY_GRAPH.tsv")
    paths = read(packet / "PATHS.tsv")
    if [row["identity"] for row in graph] != list(EXPECTED):
        raise AssertionError("dependency identity/order drift")

    prior: set[str] = set()
    for order, row in enumerate(graph):
        identity = row["identity"]
        if int(row["order"]) != order:
            raise AssertionError(f"dependency order drift: {identity}")
        if row["upstream_commit"] != EXPECTED_COMMITS[identity]:
            raise AssertionError(f"upstream commit drift: {identity}")
        if not row["upstream_ref"].startswith("jdk-") or not row["upstream_ref"].endswith("+36"):
            raise AssertionError(f"unexpected composition ref: {identity}")
        dependency = row["depends_on"]
        if dependency and dependency not in prior:
            raise AssertionError(f"dependency not topologically prior: {identity}->{dependency}")
        prior.add(identity)

    grouped: dict[str, list[dict[str, str]]] = {identity: [] for identity in EXPECTED}
    seen_paths: set[tuple[str, str]] = set()
    for row in paths:
        identity = row["identity"]
        if identity not in grouped:
            raise AssertionError(f"unknown path identity: {identity}")
        key = (identity, row["path"])
        if key in seen_paths:
            raise AssertionError(f"duplicate path row: {identity}:{row['path']}")
        seen_paths.add(key)
        grouped[identity].append(row)

    for row in graph:
        identity = row["identity"]
        values = grouped[identity]
        excluded = sum(1 for value in values if value["m3_action"].startswith("EXCLUDE"))
        accepted = len(values) - excluded
        expected_accepted, expected_excluded, expected_total = EXPECTED[identity]
        if (accepted, excluded, len(values)) != (
            expected_accepted,
            expected_excluded,
            expected_total,
        ):
            raise AssertionError(
                f"path count drift for {identity}: "
                f"accepted={accepted} excluded={excluded} total={len(values)}"
            )
        if int(row["accepted_targets"]) != accepted:
            raise AssertionError(f"accepted_targets drift: {identity}")
        if int(row["excluded_targets"]) != excluded:
            raise AssertionError(f"excluded_targets drift: {identity}")

    named_parameter = [
        row
        for row in paths
        if row["path"].endswith("/java/security/spec/NamedParameterSpec.java")
    ]
    if {row["identity"] for row in named_parameter} != {"JEP-496", "JEP-497"}:
        raise AssertionError("NamedParameterSpec exclusion ownership drift")
    if any(row["m3_action"] != "EXCLUDE_DEFAULT" for row in named_parameter):
        raise AssertionError("NamedParameterSpec must remain excluded from default Java21")

    shared = [
        row
        for row in paths
        if row["path"].endswith("/sun/security/util/KnownOIDs.java")
        or row["path"].endswith("/sun/security/provider/all/Deterministic.java")
    ]
    if {row["identity"] for row in shared} != {"JEP-496", "JEP-497"}:
        raise AssertionError("shared JDK24 GA composition paths are incomplete")


if __name__ == "__main__":
    verify(Path(__file__).resolve().parent)
    print("PASS: JEP496/497 PQ dependency and path inventory")
