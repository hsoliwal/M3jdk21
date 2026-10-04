#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run the canonical M3 FILE-convergence DAG against one pinned newer-JDK donor checkout.

This is orchestration only. It does not fetch donors, decide compatibility, mutate the donor tree,
or promote product source. The checkout must already be detached at the exact RELEASE_DONOR_REFS
authority revision.
"""

from __future__ import annotations

import argparse
import csv
import subprocess
from dataclasses import dataclass
from pathlib import Path
from typing import Callable, Sequence

import release_donor_refs


@dataclass(frozen=True)
class DonorRun:
    release: int
    revision: str
    donor_root: Path
    output: Path


def expected_ref(control_root: Path, release: int) -> str:
    refs = release_donor_refs.verify_with_release_authority(control_root)
    try:
        return refs[release]
    except KeyError as failure:
        raise ValueError(f"unsupported donor release: {release}") from failure


def git_head(donor_root: Path) -> str:
    process = subprocess.run(
        ("git", "-C", str(donor_root), "rev-parse", "HEAD"),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
    )
    if process.returncode:
        raise RuntimeError(process.stderr.strip())
    return process.stdout.strip()


def resolved_ref(donor_root: Path, revision: str) -> str:
    process = subprocess.run(
        ("git", "-C", str(donor_root), "rev-parse", f"{revision}^{{commit}}"),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
    )
    if process.returncode:
        raise RuntimeError(process.stderr.strip())
    return process.stdout.strip()


def verify_checkout(control_root: Path, donor_root: Path, release: int) -> DonorRun:
    control = control_root.resolve()
    donor = donor_root.resolve()
    if not (donor / "src").is_dir():
        raise ValueError(f"donor OpenJDK src directory missing: {donor}")
    revision = expected_ref(control, release)
    actual = git_head(donor)
    expected = resolved_ref(donor, revision)
    if actual != expected:
        raise ValueError(
            f"donor checkout drift for JDK {release}: HEAD {actual} != {revision} {expected}"
        )
    return DonorRun(release, revision, donor, Path())


def maven_command(
    control_root: Path,
    donor_root: Path,
    release: int,
    output: Path,
    threads: int = 4,
    maven: str = "mvn",
) -> tuple[str, ...]:
    if threads <= 0:
        raise ValueError("threads")
    checked = verify_checkout(control_root, donor_root, release)
    return (
        maven,
        "-B",
        "-ntp",
        "-f",
        str(control_root.resolve() / "m3/pom.xml"),
        "-pl",
        "tooling/migration-recipes",
        "-am",
        "-Pm3-jdk-donor-convergence",
        f"-Dm3.donor.root={checked.donor_root}",
        f"-Dm3.donor.revision={checked.revision}",
        f"-Dm3.donor.output={output.resolve()}",
        f"-Dm3.donor.threads={threads}",
        "-DskipTests=true",
        "-Djacoco.skip=true",
        "verify",
    )


def verify_identity(output: Path, revision: str) -> str:
    identity = output.resolve() / "SOURCE_CONVERGENCE.image.tsv"
    manifest = output.resolve() / "SOURCE_CONVERGENCE.tsv"
    if not identity.is_file() or not manifest.is_file():
        raise ValueError("donor convergence output missing manifest/identity")
    with identity.open("r", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if len(rows) != 1:
        raise ValueError("donor convergence identity row count")
    row = rows[0]
    if row.get("role") != "DONOR" or row.get("revision") != revision:
        raise ValueError("donor convergence identity mismatch")
    root = row.get("semanticRoot", "")
    if len(root) != 64 or any(ch not in "0123456789abcdef" for ch in root):
        raise ValueError("invalid donor convergence semantic root")
    return root


def run(
    control_root: Path,
    donor_root: Path,
    release: int,
    output: Path,
    threads: int = 4,
    runner: Callable[[Sequence[str]], None] | None = None,
) -> str:
    revision = expected_ref(control_root.resolve(), release)
    command = maven_command(
        control_root,
        donor_root,
        release,
        output,
        threads=threads,
    )
    execute = runner or _run
    execute(command)
    return verify_identity(output, revision)


def _run(command: Sequence[str]) -> None:
    subprocess.run(tuple(command), check=True)


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--control-root", type=Path, required=True)
    parser.add_argument("--donor-root", type=Path, required=True)
    parser.add_argument("--release", type=int, choices=range(22, 28), required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--threads", type=int, default=4)
    args = parser.parse_args(argv)

    root = run(
        args.control_root,
        args.donor_root,
        args.release,
        args.output,
        threads=args.threads,
    )
    print(f"M3_DONOR_CONVERGENCE\tPASS\tJDK{args.release}\t{root}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
