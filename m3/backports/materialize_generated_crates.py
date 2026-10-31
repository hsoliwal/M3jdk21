#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed executor for generated M3JDK21 hash-pinned recipe crates.

The generator remains the source of the Java/text recipe manifests. This operator performs exact
byte custody only after those manifests have been generated/proven. It does not classify
compatibility, infer equivalence, widen scope, process removals, or grant promotion authority.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
from dataclasses import dataclass
import os
from pathlib import Path
import sys


JAVA_ROOT = Path("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned")
TEXT_ROOT = Path("src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text")
ABSENT = "ABSENT"
BASELINE_REF = "jdk-21+35"
DONOR_REFS = {
    22: "jdk-22+36",
    23: "jdk-23+37",
    24: "jdk-24+36",
    25: "jdk-25+36",
    26: "jdk-26+35",
    27: "jdk-27+35",
}
CANDIDATE_STATUS = "CANDIDATE_UNVERIFIED"


@dataclass(frozen=True)
class Crate:
    name: str
    release: int
    target_count: int
    first_path: str
    last_path: str
    baseline_ref: str
    donor_ref: str
    status: str
    manifest: Path


@dataclass(frozen=True)
class Target:
    crate: str
    path: str
    before: str
    after: str
    resource: Path


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def canonical_relative(value: str) -> str:
    if (
        not value
        or len(value) > 4096
        or value.startswith(("/", "\\"))
        or "\\" in value
        or ":" in value
    ):
        raise ValueError(f"noncanonical relative path: {value!r}")
    parts = value.split("/")
    if any(part in ("", ".", "..", ".git") for part in parts):
        raise ValueError(f"noncanonical relative path: {value!r}")
    if any(any(ord(ch) < 32 or ord(ch) == 127 for ch in part) for part in parts):
        raise ValueError(f"control character in path: {value!r}")
    return "/".join(parts)


def hash_value(value: str) -> bool:
    return len(value) == 64 and all(ch in "0123456789abcdef" for ch in value)


def read_crates(generated: Path, require_one_target: bool) -> list[Crate]:
    index = generated / "CRATES.tsv"
    with index.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows:
        raise ValueError("generated crate index is empty")

    result: list[Crate] = []
    names: set[str] = set()
    for row in rows:
        name = canonical_relative(row["crate_name"])
        if "/" in name:
            raise ValueError(f"crate name must be a single path segment: {name}")
        if name in names:
            raise ValueError(f"duplicate crate: {name}")
        names.add(name)
        try:
            release = int(row["release"])
            target_count = int(row["target_count"])
            baseline_ref = row["baseline_ref"]
            donor_ref = row["donor_ref"]
            first_path = canonical_relative(row["first_path"])
            last_path = canonical_relative(row["last_path"])
            status = row["status"]
        except (KeyError, ValueError) as failure:
            raise ValueError(f"invalid crate metadata: {name}") from failure
        if release not in range(22, 28) or target_count < 1 or target_count > 256:
            raise ValueError(f"invalid crate bounds: {name}")
        if baseline_ref != BASELINE_REF:
            raise ValueError(f"unexpected crate baseline ref: {name}:{baseline_ref}")
        if donor_ref != DONOR_REFS[release]:
            raise ValueError(f"unexpected crate donor ref: {name}:{donor_ref}")
        if status != CANDIDATE_STATUS:
            raise ValueError(f"unexpected crate status: {name}:{status}")
        if first_path > last_path:
            raise ValueError(f"crate first/last path order invalid: {name}")
        if require_one_target and target_count != 1:
            raise ValueError(f"FILE lane requires one target per crate: {name}")

        candidates = [
            generated / JAVA_ROOT / name / "manifest.tsv",
            generated / TEXT_ROOT / name / "manifest.tsv",
        ]
        present = [path for path in candidates if path.is_file()]
        if len(present) != 1:
            raise ValueError(f"crate manifest ownership is ambiguous/missing: {name}")
        result.append(
            Crate(
                name,
                release,
                target_count,
                first_path,
                last_path,
                baseline_ref,
                donor_ref,
                status,
                present[0],
            )
        )

    return result


def read_targets(generated: Path, crates: list[Crate]) -> list[Target]:
    result: list[Target] = []
    owners: set[str] = set()

    for crate in crates:
        crate_dir = crate.manifest.parent
        rows: list[Target] = []
        previous = ""
        for raw in crate.manifest.read_text(encoding="utf-8").splitlines():
            if not raw or raw.startswith("#"):
                continue
            cells = raw.split("\t")
            if len(cells) != 4:
                raise ValueError(f"invalid manifest row in {crate.name}: {raw!r}")
            path, before, after, resource_name = cells
            path = canonical_relative(path)
            resource_name = canonical_relative(resource_name)
            if "/" in resource_name:
                raise ValueError(f"crate resource must be local: {resource_name}")
            if path <= previous:
                raise ValueError(f"crate targets are not strictly sorted: {crate.name}")
            previous = path
            if before != ABSENT and not hash_value(before):
                raise ValueError(f"invalid preimage hash: {crate.name}:{path}")
            if not hash_value(after):
                raise ValueError(f"invalid postimage hash: {crate.name}:{path}")
            resource = crate_dir / resource_name
            if not resource.is_file() or resource.is_symlink():
                raise ValueError(f"missing/unsafe crate payload: {crate.name}:{resource_name}")
            if sha256(resource.read_bytes()) != after:
                raise ValueError(f"crate payload hash drift: {crate.name}:{path}")
            if path in owners:
                raise ValueError(f"target owned by multiple generated crates: {path}")
            owners.add(path)
            rows.append(Target(crate.name, path, before, after, resource))
        if len(rows) != crate.target_count:
            raise ValueError(
                f"crate target_count mismatch: {crate.name}: "
                f"{crate.target_count} != {len(rows)}"
            )
        if rows[0].path != crate.first_path or rows[-1].path != crate.last_path:
            raise ValueError(
                f"crate first/last path metadata drift: {crate.name}: "
                f"{crate.first_path}..{crate.last_path} != "
                f"{rows[0].path}..{rows[-1].path}"
            )
        result.extend(rows)

    return sorted(result, key=lambda target: target.path)


def target_state(repo: Path, target: Target) -> tuple[str, str]:
    path = repo / target.path
    if path.is_symlink():
        raise ValueError(f"symlink target forbidden: {target.path}")
    if not path.exists():
        return ABSENT, ABSENT
    if not path.is_file():
        raise ValueError(f"non-file target forbidden: {target.path}")
    digest = sha256(path.read_bytes())
    if digest == target.after:
        return "AFTER", digest
    if target.before != ABSENT and digest == target.before:
        return "BEFORE", digest
    raise ValueError(f"GENERATED_CRATE_PREIMAGE_DRIFT:{target.path}:{digest}")


def destination(repo: Path, relative: str) -> Path:
    root = repo.resolve()
    path = (repo / relative).resolve(strict=False)
    try:
        path.relative_to(root)
    except ValueError as failure:
        raise ValueError(f"target escapes repository: {relative}") from failure
    return path


def run(
    repo: Path,
    generated: Path,
    mode: str,
    require_one_target: bool,
    receipt: Path | None,
) -> list[Target]:
    repo = repo.resolve()
    generated = generated.resolve()
    crates = read_crates(generated, require_one_target)
    targets = read_targets(generated, crates)
    input_rows: list[tuple[Target, str, str]] = []

    for target in targets:
        state, observed = target_state(repo, target)
        input_rows.append((target, state, observed))

        if mode == "check":
            if state == ABSENT and target.before != ABSENT:
                raise ValueError(f"required preimage missing: {target.path}")
            continue

        if mode == "verify-post":
            if state != "AFTER":
                raise ValueError(f"postimage missing: {target.path}:{state}")
            continue

        if state == "AFTER":
            continue
        if state == ABSENT and target.before != ABSENT:
            raise ValueError(f"required preimage missing: {target.path}")
        if state == "BEFORE" and target.before == ABSENT:
            raise ValueError(f"unexpected preimage for addition: {target.path}")

        out = destination(repo, target.path)
        out.parent.mkdir(parents=True, exist_ok=True)
        temp = out.with_name(out.name + ".m3-generated.tmp")
        if temp.exists() or temp.is_symlink():
            temp.unlink()
        temp.write_bytes(target.resource.read_bytes())
        os.replace(temp, out)
        actual = sha256(out.read_bytes())
        if actual != target.after:
            raise ValueError(f"generated crate postimage drift: {target.path}:{actual}")

    if receipt is not None:
        receipt.parent.mkdir(parents=True, exist_ok=True)
        body = [
            "crate\tpath\tinput_state\tinput_sha256\tpost_sha256"
        ]
        for target, state, observed in input_rows:
            post = target.after if mode == "apply" or state == "AFTER" else ""
            body.append(
                f"{target.crate}\t{target.path}\t{state}\t{observed}\t{post}"
            )
        receipt.write_text("\n".join(body) + "\n", encoding="utf-8")

    return targets


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--generated", type=Path, required=True)
    parser.add_argument("--mode", choices=("check", "apply", "verify-post"), default="check")
    parser.add_argument("--require-one-target-per-crate", action="store_true")
    parser.add_argument("--receipt", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        targets = run(
            args.root,
            args.generated,
            args.mode,
            args.require_one_target_per_crate,
            args.receipt,
        )
    except (OSError, ValueError) as failure:
        print(f"generated crate materialization refused: {failure}", file=sys.stderr)
        return 2
    print(f"generated crate {args.mode}: {len(targets)} exact target(s) accepted")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
