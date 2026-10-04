#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""M3 DNA FILE-atomic crate generator from normalized baseline/donor postimages.

Raw Git-tree deltas remain discovery evidence. This generator is the absorption lane: selected Java
paths must first pass the shared OpenRewrite FILE convergence DAG on both sides. Generated recipe
pre/post images therefore start from atomized/patternized/documented fixed-point source.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
from pathlib import Path
from typing import Sequence

import generate_recipe_crates
import release_donor_refs
import source_convergence_gate
import m3_dna_id
import m3_dna_gate


def _text(payload: bytes, label: str) -> str:
    try:
        value = payload.decode("utf-8", "strict")
    except UnicodeDecodeError as failure:
        raise ValueError(f"{label}: normalized Java source is not UTF-8") from failure
    if value.encode("utf-8") != payload:
        raise ValueError(f"{label}: normalized Java UTF-8 round-trip drift")
    return value


def _effective(
    root: Path,
    manifest: Path,
    row: source_convergence_gate.Row,
) -> tuple[str, str]:
    source_convergence_gate.require_converged(row)
    source = source_convergence_gate.regular_file(root, row.path)
    current = hashlib.sha256(source.read_bytes()).hexdigest()
    if current != row.pre_sha256:
        raise ValueError(
            f"normalized source preimage drift: {row.path}: {current} != {row.pre_sha256}"
        )

    if row.status == "CONVERGED_CHANGED":
        candidate = source_convergence_gate.regular_file(manifest.parent, row.candidate)
        payload = candidate.read_bytes()
    else:
        payload = source.read_bytes()
    actual = hashlib.sha256(payload).hexdigest()
    if actual != row.post_sha256:
        raise ValueError(
            f"normalized source postimage drift: {row.path}: {actual} != {row.post_sha256}"
        )
    return _text(payload, row.path), actual


def generate(
    control_root: Path,
    baseline_root: Path,
    baseline_manifest: Path,
    baseline_identity: Path,
    donor_root: Path,
    donor_manifest: Path,
    donor_identity: Path,
    release: int,
    targets: Path,
    out: Path,
    crate_size: int = 1,
) -> tuple[list[str], str]:
    control = control_root.resolve()
    refs = release_donor_refs.verify_with_release_authority(control)
    try:
        donor_ref = refs[release]
    except KeyError as failure:
        raise ValueError(f"unsupported donor release: {release}") from failure

    baseline_id = m3_dna_id.load(
        baseline_identity.resolve(),
        expected_role="BASELINE",
        expected_revision="jdk-21+35",
    )
    donor_id = m3_dna_id.load(
        donor_identity.resolve(),
        expected_role="DONOR",
        expected_revision=donor_ref,
    )
    pair_root = m3_dna_gate.verify(
        control,
        baseline_root,
        baseline_manifest,
        baseline_identity,
        donor_root,
        donor_manifest,
        donor_identity,
        release,
        targets,
    )

    selected = source_convergence_gate.load_targets(targets.resolve())
    baseline_rows = source_convergence_gate.load_manifest(baseline_manifest.resolve())
    donor_rows = source_convergence_gate.load_manifest(donor_manifest.resolve())

    candidates: list[generate_recipe_crates.Candidate] = []
    exclusions: list[tuple[str, str]] = []
    for path in selected:
        donor_row = donor_rows.get(path)
        if donor_row is None:
            raise ValueError(f"missing donor convergence row: {path}")
        after_text, after_hash = _effective(
            donor_root.resolve(), donor_manifest.resolve(), donor_row
        )

        baseline_row = baseline_rows.get(path)
        if baseline_row is None:
            baseline_path = baseline_root.resolve() / path
            if baseline_path.exists() or baseline_path.is_symlink():
                raise ValueError(
                    f"baseline source exists without convergence row: {path}"
                )
            before_hash = "ABSENT"
            status = "ADDED"
        else:
            _before_text, before_hash = _effective(
                baseline_root.resolve(),
                baseline_manifest.resolve(),
                baseline_row,
            )
            status = "MODIFIED"

        if before_hash == after_hash:
            exclusions.append((path, "NO_NORMALIZED_DELTA"))
            continue

        candidates.append(
            generate_recipe_crates.Candidate(
                path=path,
                status=status,
                before_sha256=before_hash,
                after_sha256=after_hash,
                after_text=after_text,
                kind="JAVA",
                donor_ref=donor_ref,
            )
        )

    if not candidates:
        raise ValueError("selected normalized targets contain no absorbable Java delta")

    out = out.resolve()
    crates = generate_recipe_crates.materialize(
        out,
        release,
        candidates,
        sorted(exclusions),
        crate_size=crate_size,
    )
    with (out / "NORMALIZED_PAIR.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="	", lineterminator="\n")
        writer.writerow(
            (
                "release",
                "baselineRevision",
                "baselineSemanticRoot",
                "donorRef",
                "donorSemanticRoot",
                "pairRoot",
                "selectedTargets",
                "candidateTargets",
                "excludedTargets",
            )
        )
        writer.writerow(
            (
                release,
                baseline_id.revision,
                baseline_id.semantic_root,
                donor_ref,
                donor_id.semantic_root,
                pair_root,
                len(selected),
                len(candidates),
                len(exclusions),
            )
        )

    with (out / "CRATE_NORMALIZATION.tsv").open(
        "w", encoding="utf-8", newline=""
    ) as handle:
        writer = csv.writer(handle, delimiter="	", lineterminator="\n")
        writer.writerow(("crate_name", "release", "donor_ref", "pair_root", "status"))
        for crate in crates:
            writer.writerow(
                (crate, release, donor_ref, pair_root, "CANDIDATE_UNVERIFIED")
            )
    return crates, pair_root


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
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument(
        "--crate-size",
        type=int,
        choices=range(1, generate_recipe_crates.CRATE_LIMIT + 1),
        default=1,
        help="default 1 preserves true FILE-scope recipe atoms",
    )
    args = parser.parse_args(argv)

    crates, root = generate(
        args.control_root,
        args.baseline_root,
        args.baseline_manifest,
        args.baseline_identity,
        args.donor_root,
        args.donor_manifest,
        args.donor_identity,
        args.release,
        args.targets,
        args.out,
        crate_size=args.crate_size,
    )
    print(
        f"M3_DNA_CRATES\tPASS\tJDK{args.release}\t"
        f"{len(crates)}\t{root}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
