#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Require selected Java backport targets to be normalized on both baseline and donor images."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
from typing import Sequence

import release_donor_refs
import source_convergence_gate
import source_convergence_identity


def verify(
    control_root: Path,
    baseline_root: Path,
    baseline_manifest: Path,
    baseline_identity: Path,
    donor_root: Path,
    donor_manifest: Path,
    donor_identity: Path,
    release: int,
    targets: Path,
) -> str:
    control = control_root.resolve()
    donor_ref = release_donor_refs.verify_with_release_authority(control).get(release)
    if donor_ref is None:
        raise ValueError(f"unsupported donor release: {release}")

    baseline_id = source_convergence_identity.load(
        baseline_identity.resolve(),
        expected_role="BASELINE",
        expected_revision="jdk-21+35",
    )
    donor_id = source_convergence_identity.load(
        donor_identity.resolve(),
        expected_role="DONOR",
        expected_revision=donor_ref,
    )

    baseline_gate = source_convergence_gate.verify(
        baseline_root.resolve(),
        baseline_manifest.resolve(),
        targets.resolve(),
    )
    donor_gate = source_convergence_gate.verify(
        donor_root.resolve(),
        donor_manifest.resolve(),
        targets.resolve(),
    )

    selected = source_convergence_gate.load_targets(targets.resolve())
    if not selected:
        raise ValueError("normalized pair gate selected zero Java targets")

    digest = hashlib.sha256()
    _frame(digest, "M3_NORMALIZED_SOURCE_PAIR_GATE_V1")
    _frame(digest, "jdk-21+35")
    _frame(digest, baseline_id.semantic_root)
    _frame(digest, baseline_gate)
    _frame(digest, str(release))
    _frame(digest, donor_ref)
    _frame(digest, donor_id.semantic_root)
    _frame(digest, donor_gate)
    for path in selected:
        _frame(digest, path)
    return digest.hexdigest()


def _frame(digest, value: str) -> None:
    data = value.encode("utf-8")
    digest.update(len(data).to_bytes(4, "big"))
    digest.update(data)


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--control-root", type=Path, required=True)
    parser.add_argument("--baseline-root", type=Path, required=True)
    parser.add_argument("--baseline-manifest", type=Path, required=True)
    parser.add_argument("--baseline-identity", type=Path, required=True)
    parser.add_argument("--donor-root", type=Path, required=True)
    parser.add_argument("--donor-manifest", type=Path, required=True)
    parser.add_argument("--donor-identity", type=Path, required=True)
    parser.add_argument("--release", type=int, choices=range(22, 28), required=True)
    parser.add_argument("--targets", type=Path, required=True)
    args = parser.parse_args(argv)

    root = verify(
        args.control_root,
        args.baseline_root,
        args.baseline_manifest,
        args.baseline_identity,
        args.donor_root,
        args.donor_manifest,
        args.donor_identity,
        args.release,
        args.targets,
    )
    print(f"SOURCE_PAIR_CONVERGENCE_GATE\tPASS\tJDK{args.release}\t{root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
