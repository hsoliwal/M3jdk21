#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Deterministic domain/architecture classifier for the pinned JEP 491 path denominator."""

from __future__ import annotations

import argparse
import csv
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

ARCHES = ("aarch64", "arm", "ppc", "riscv", "s390", "x86", "zero")


@dataclass(frozen=True)
class Node:
    path: str
    domain: str
    architecture: str
    mutation_scope: str
    depends_on: str


def classify(path: str) -> Node:
    if not path or path.startswith("/") or "\\" in path or ".." in Path(path).parts:
        raise ValueError(f"canonical repository-relative path required: {path!r}")

    for arch in ARCHES:
        prefix = f"src/hotspot/cpu/{arch}/"
        if path.startswith(prefix):
            return Node(path, "HOTSPOT_CPU", arch, "MULTI_MODULE", "HOTSPOT_SHARED")

    if path.startswith("src/hotspot/share/prims/jvmti") or "/jvmti/" in path:
        return Node(path, "JVMTI", "shared", "MULTI_MODULE", "HOTSPOT_SHARED")
    if "/jfr/" in path or path.startswith("src/jdk.jfr/"):
        return Node(path, "JFR", "shared", "MULTI_MODULE", "HOTSPOT_SHARED")
    if path.startswith("src/jdk.hotspot.agent/") or "/serviceability/sa/" in path:
        return Node(path, "SA", "shared", "MULTI_MODULE", "HOTSPOT_SHARED")
    if path.startswith("src/hotspot/share/"):
        return Node(path, "HOTSPOT_SHARED", "shared", "MULTI_MODULE", "")
    if path.startswith("src/java.base/") and (
        "/native/" in path or path.endswith((".c", ".cc", ".cpp", ".h", ".hpp"))
    ):
        return Node(path, "JAVA_BASE_NATIVE", "shared", "MULTI_MODULE", "HOTSPOT_SHARED")
    if path.startswith("src/java.base/"):
        return Node(path, "JAVA_BASE", "shared", "MULTI_MODULE", "HOTSPOT_SHARED")

    if path.startswith("test/hotspot/"):
        return Node(path, "HOTSPOT_TEST", "test", "PROOF_ONLY", "HOTSPOT_SHARED")
    if path.startswith("test/jdk/"):
        return Node(path, "JDK_TEST", "test", "PROOF_ONLY", "JAVA_BASE")

    raise ValueError(f"unclassified J491 path: {path}")


def load_paths(path: Path) -> list[str]:
    rows = [
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if len(rows) != 246:
        raise ValueError(f"J491 denominator must be 246 paths, got {len(rows)}")
    if rows != sorted(rows):
        raise ValueError("J491 PATHS.txt must be sorted")
    if len(set(rows)) != len(rows):
        raise ValueError("J491 PATHS.txt contains duplicates")
    return rows


def materialize(paths: Iterable[str], out: Path) -> list[Node]:
    nodes = [classify(path) for path in paths]
    out.mkdir(parents=True, exist_ok=True)

    with (out / "ARCH_DAG.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("path", "domain", "architecture", "mutation_scope", "depends_on"))
        for node in nodes:
            writer.writerow(
                (node.path, node.domain, node.architecture, node.mutation_scope, node.depends_on)
            )

    domains = Counter(node.domain for node in nodes)
    arches = Counter(node.architecture for node in nodes)
    with (out / "DOMAIN_COUNTS.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("kind", "name", "count"))
        for name in sorted(domains):
            writer.writerow(("domain", name, domains[name]))
        for name in sorted(arches):
            writer.writerow(("architecture", name, arches[name]))

    if sum(domains.values()) != 246 or sum(arches.values()) != 246:
        raise AssertionError("J491 classification accounting drift")
    return nodes


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--paths", type=Path, required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    nodes = materialize(load_paths(args.paths), args.out)
    print(f"J491_CLASSIFIED={len(nodes)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
