#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Preflight and safely overlay convergence candidates onto an isolated worktree.

Validation failures precede source writes. Each replacement is atomic, but the
whole worktree is not a filesystem transaction. Exclusive workspace ownership and
immutable input/evidence during execution are required.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import stat
import tempfile
from dataclasses import dataclass
from pathlib import Path

from source_convergence_gate import Row, load_manifest, regular_file, require_converged
import source_convergence_identity


def _hash(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


def load(path: Path) -> list[Row]:
    """Reuse the manifest Recognizer and reject every unresolved selected row."""
    rows = sorted(load_manifest(path).values(), key=lambda row: row.path)
    if not rows:
        raise ValueError("empty convergence manifest cannot materialize a baseline")
    for row in rows:
        try:
            require_converged(row)
        except ValueError as failure:
            raise ValueError(f"unresolved convergence row: {row.path}") from failure
    return rows


@dataclass(frozen=True)
class _Edit:
    """Per-file plan; payload is staged one file at a time, not retained for the tree."""
    row: Row
    canonical: Path
    target: Path
    candidate: Path | None
    current_hash: str
    mode: int

def _disjoint(source: Path, destination: Path) -> None:
    if source.is_relative_to(destination) or destination.is_relative_to(source):
        raise ValueError("normalized worktree must be disjoint from canonical source root")


def _plan(source: Path, manifest: Path, worktree: Path, rows: list[Row]) -> list[_Edit]:
    """Validate the complete write frontier before staging or replacing any target."""
    edits: list[_Edit] = []
    for row in rows:
        canonical = regular_file(source, row.path)
        target = regular_file(worktree, row.path)
        if _hash(canonical.read_bytes()) != row.pre_sha256:
            raise ValueError(f"canonical preimage drift: {row.path}")
        current = _hash(target.read_bytes())
        if current not in {row.pre_sha256, row.post_sha256}:
            raise ValueError(f"worktree preimage drift: {row.path}")
        candidate = None
        if row.status == "CONVERGED_CHANGED":
            candidate = regular_file(manifest.parent, row.candidate)
            if _hash(candidate.read_bytes()) != row.post_sha256:
                raise ValueError(f"candidate postimage drift: {row.path}")
        edits.append(_Edit(row, canonical, target, candidate, current,
                           stat.S_IMODE(target.stat().st_mode)))
    return edits


def _receipts(
    rows: list[Row],
    image_kind: str,
    identity: source_convergence_identity.ImageIdentity | None,
) -> tuple[str, dict[str, bytes], str]:
    """Preserve baseline V1 identity and add a revision-bound donor identity."""
    evidence = [
        "\x1f".join((r.path, r.pre_sha256, r.post_sha256, r.status, r.effective_sha256))
        for r in rows
    ]
    tsv = "path\tpreSha256\tpostSha256\tstatus\teffectiveSha256\n" + "\n".join(
        "\t".join((r.path, r.pre_sha256, r.post_sha256, r.status, r.effective_sha256))
        for r in rows
    ) + "\n"

    if image_kind == "baseline":
        root = _hash(("M3_NORMALIZED_JDK21_BASELINE_V1\n"
                      + "\n".join(evidence) + "\n").encode("utf-8"))
        marker = "m3-normalized-baseline.root"
        receipts = {
            "m3-normalized-baseline.tsv": tsv.encode("utf-8"),
            marker: (root + "\n").encode("utf-8"),
        }
        return root, receipts, marker

    if image_kind != "donor":
        raise ValueError(f"unsupported normalized image kind: {image_kind}")
    if identity is None:
        raise ValueError("donor materialization requires a typed source convergence identity")
    if identity.role != "DONOR":
        raise ValueError("donor materialization requires DONOR identity")

    header = "\x1f".join(
        ("M3_NORMALIZED_JDK_DONOR_V1", identity.revision, identity.semantic_root)
    )
    root = _hash((header + "\n" + "\n".join(evidence) + "\n").encode("utf-8"))
    marker = "m3-normalized-donor.root"
    image_tsv = (
        "role\trevision\tconvergenceSemanticRoot\tnormalizedRoot\tfiles\tchanged\tholds\n"
        f"{identity.role}\t{identity.revision}\t{identity.semantic_root}\t{root}\t"
        f"{identity.files}\t{identity.changed}\t{identity.holds}\n"
    )
    receipts = {
        "m3-normalized-donor.tsv": tsv.encode("utf-8"),
        marker: (root + "\n").encode("utf-8"),
        "m3-normalized-donor.image.tsv": image_tsv.encode("utf-8"),
    }
    return root, receipts, marker


def _admit_receipts(
    worktree: Path,
    receipts: dict[str, bytes],
    edits: list[_Edit],
    success_marker: str,
) -> None:
    """Never overwrite conflicting metadata or bless a stale existing success marker."""
    for name, payload in receipts.items():
        path = worktree / name
        if path.is_symlink():
            raise ValueError(f"receipt symlink: {name}")
        if path.exists() and (not path.is_file() or path.read_bytes() != payload):
            raise ValueError(f"receipt drift: {name}")
    if (worktree / success_marker).exists():
        if any(e.current_hash != e.row.post_sha256 for e in edits):
            raise ValueError("existing success marker does not describe current worktree")


def _stage(target: Path, payload: bytes, mode: int) -> Path:
    """Create a private sibling; never truncate an inode shared with the original."""
    temporary: Path | None = None
    try:
        with tempfile.NamedTemporaryFile(prefix=".m3-convergence-", dir=target.parent,
                                         delete=False) as handle:
            temporary = Path(handle.name)
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.chmod(temporary, mode)
        return temporary
    except BaseException:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
        raise


def materialize(
    source_root: Path,
    manifest: Path,
    worktree_root: Path,
    image_kind: str = "baseline",
    identity_path: Path | None = None,
) -> str:
    source_root = source_root.resolve()
    worktree_root = worktree_root.resolve()
    manifest = manifest.resolve()
    _disjoint(source_root, worktree_root)
    if not (worktree_root / "src").is_dir():
        raise ValueError("normalized worktree must already contain an OpenJDK src tree")

    kind = image_kind.strip().lower()
    identity = None
    if identity_path is not None:
        identity = source_convergence_identity.load(
            identity_path.resolve(), expected_role=kind.upper()
        )
    if kind == "donor" and identity is None:
        raise ValueError("donor materialization requires --identity")
    rows = load(manifest)
    edits = _plan(source_root, manifest, worktree_root, rows)
    root, receipts, success_marker = _receipts(rows, kind, identity)
    _admit_receipts(worktree_root, receipts, edits, success_marker)
    staged: list[tuple[Path, Path]] = []
    try:
        for edit in edits:
            if edit.current_hash == edit.row.post_sha256:
                continue
            if edit.candidate is None:
                raise ValueError(f"missing planned candidate: {edit.row.path}")
            payload = edit.candidate.read_bytes()
            if _hash(payload) != edit.row.post_sha256:
                raise ValueError(f"candidate changed during staging: {edit.row.path}")
            staged.append((edit.target, _stage(edit.target, payload, edit.mode)))

        # Staging is not promotion: recheck all inputs, aliases and targets first.
        if edits != _plan(source_root, manifest, worktree_root, rows):
            raise ValueError("worktree changed during staging")
        if rows != load(manifest):
            raise ValueError("convergence manifest changed during staging")
        _admit_receipts(worktree_root, receipts, edits, success_marker)
        for target, temporary in staged:
            os.replace(temporary, target)

        for edit in edits:
            if _hash(regular_file(source_root, edit.row.path).read_bytes()) != edit.row.pre_sha256:
                raise ValueError(f"canonical source changed during apply: {edit.row.path}")
            if _hash(regular_file(worktree_root, edit.row.path).read_bytes()) != edit.row.post_sha256:
                raise ValueError(f"normalized worktree drift: {edit.row.path}")

        # Publish .root last; a failure earlier cannot publish a completion marker.
        for name, payload in receipts.items():
            path = worktree_root / name
            if path.exists() and path.read_bytes() == payload:
                continue
            temporary = _stage(path, payload, 0o644)
            staged.append((path, temporary))
            os.replace(temporary, path)
        return root
    finally:
        for _, temporary in staged:
            temporary.unlink(missing_ok=True)


def copy_tree(source: Path, destination: Path) -> None:
    if destination.exists() or destination.is_symlink():
        raise ValueError(f"destination already exists: {destination}")
    source = source.resolve()
    destination = destination.resolve()
    _disjoint(source, destination)
    shutil.copytree(
        source,
        destination,
        symlinks=True,
        ignore=shutil.ignore_patterns(".git", "build", "target"),
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("source_root", type=Path)
    parser.add_argument("manifest", type=Path)
    parser.add_argument("worktree_root", type=Path)
    parser.add_argument("--copy", action="store_true",
                        help="copy source_root before overlay; otherwise use an isolated worktree")
    parser.add_argument(
        "--image-kind",
        choices=("baseline", "donor"),
        default="baseline",
        help="typed normalized image; baseline preserves the historical V1 receipt contract",
    )
    parser.add_argument(
        "--identity",
        type=Path,
        help="SOURCE_CONVERGENCE.image.tsv; required for donor images",
    )
    args = parser.parse_args()
    if args.copy:
        copy_tree(args.source_root, args.worktree_root)
    root = materialize(
        args.source_root,
        args.manifest,
        args.worktree_root,
        image_kind=args.image_kind,
        identity_path=args.identity,
    )
    label = "M3_NORMALIZED_BASELINE" if args.image_kind == "baseline" else "M3_NORMALIZED_DONOR"
    print(f"{label}\tPASS\t{root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
