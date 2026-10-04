#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Require selected Java backport targets to be normalized on baseline and donor images."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
from typing import Sequence

import release_donor_refs
import source_convergence_gate
import source_convergence_identity


def _effective(
    root: Path,
    manifest: Path,
    row: source_convergence_gate.Row,
) -> str:
    source_convergence_gate.require_converged(row)
    source = source_convergence_gate.regular_file(root, row.path)
    current = hashlib.sha256(source.read_bytes()).hexdigest()
    if current != row.pre_sha256:
        raise ValueError(
            f"Java target preimage drift: {row.path}: {current} != {row.pre_sha256}"
        )
    if row.status == "CONVERGED_CHANGED":
        candidate = source_convergence_gate.regular_file(manifest.parent, row.candidate)
        actual = hashlib.sha256(candidate.read_bytes()).hexdigest()
        if actual != row.post_sha256:
            raise ValueError(
                f"candidate postimage drift: {row.path}: {actual} != {row.post_sha256}"
            )
    return row.post_sha256


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

    baseline_root = baseline_root.resolve()
    donor_root = donor_root.resolve()
    baseline_manifest = baseline_manifest.resolve()
    donor_manifest = donor_manifest.resolve()
    baseline_rows = source_convergence_gate.load_manifest(baseline_manifest)
    donor_rows = source_convergence_gate.load_manifest(donor_manifest)
    selected = source_convergence_gate.load_targets(targets.resolve())
    if not selected:
        raise ValueError("normalized pair gate selected zero Java targets")

    evidence: list[str] = []
    for target in selected:
        donor_row = donor_rows.get(target)
        if donor_row is None:
            raise ValueError(f"missing donor SOURCE_CONVERGENCE row: {target}")
        donor_effective = _effective(donor_root, donor_manifest, donor_row)

        baseline_row = baseline_rows.get(target)
        if baseline_row is None:
            baseline_path = baseline_root / target
            if baseline_path.exists() or baseline_path.is_symlink():
                raise ValueError(
                    f"baseline target exists without SOURCE_CONVERGENCE row: {target}"
                )
            baseline_effective = "ABSENT"
        else:
            baseline_effective = _effective(
                baseline_root,
                baseline_manifest,
                baseline_row,
            )

        evidence.append(
            "".join(
                (
                    target,
                    baseline_effective,
                    donor_effective,
                    "ADDED" if baseline_effective == "ABSENT" else "PAIRED",
                )
            )
        )

    digest = hashlib.sha256()
    _frame(digest, "M3_NORMALIZED_SOURCE_PAIR_GATE_V2")
    _frame(digest, "jdk-21+35")
    _frame(digest, baseline_id.semantic_root)
    _frame(digest, str(release))
    _frame(digest, donor_ref)
    _frame(digest, donor_id.semantic_root)
    for row in evidence:
        _frame(digest, row)
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
