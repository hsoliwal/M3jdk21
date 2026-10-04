#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0
"""Generate candidate hash-pinned OpenRewrite crates from exact JDK21/donor file pairs.

The generator never admits compatibility. It turns selected exact Java deltas into deterministic
resources consumed by M3Jdk21HashPinnedSnapshotRecipe. Generated crates remain CANDIDATE_UNVERIFIED
until Java-21 parse, recipe JUnit, compile/test/runtime and compatibility gates pass.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable, Sequence

CRATE_LIMIT = 256
RESOURCE_ROOT = Path(
    "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned"
)
YAML_ROOT = Path("src/main/resources/META-INF/rewrite")


def _load_delta_module():
    path = Path(__file__).with_name("file_delta_inventory.py")
    spec = importlib.util.spec_from_file_location("m3_file_delta_inventory_for_crates", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


DELTA = _load_delta_module()


@dataclass(frozen=True)
class Candidate:
    path: str
    status: str
    before_sha256: str
    after_sha256: str
    after_text: str


def _git_bytes(repo: Path, ref: str, path: str) -> bytes:
    proc = subprocess.run(
        ("git", "-C", str(repo), "show", f"{ref}:{path}"),
        check=False,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if proc.returncode:
        raise RuntimeError(
            f"git show {ref}:{path} failed: "
            + proc.stderr.decode("utf-8", "replace").strip()
        )
    return proc.stdout


def _utf8(data: bytes, label: str) -> str:
    try:
        text = data.decode("utf-8", "strict")
    except UnicodeDecodeError as failure:
        raise ValueError(f"{label}: Java source is not UTF-8") from failure
    if text.encode("utf-8") != data:
        raise ValueError(f"{label}: UTF-8 round-trip drift")
    return text


def _sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def _selected_paths(path_file: Path | None) -> set[str] | None:
    if path_file is None:
        return None
    paths = {
        line.strip()
        for line in path_file.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    }
    if not paths:
        raise ValueError("paths file selected zero paths")
    return paths


def candidates(
    repo: Path,
    release: int,
    selected: set[str] | None,
    all_candidates: bool,
) -> tuple[list[Candidate], list[tuple[str, str]]]:
    if selected is None and not all_candidates:
        raise ValueError("select --paths-file or explicitly request --all-candidates")

    rows = DELTA.compare(repo, (release,))
    by_path = {row.path: row for row in rows}
    if selected is not None:
        missing = sorted(selected - set(by_path))
        if missing:
            raise ValueError(f"selected paths absent from comparison: {missing[:5]}")

    result: list[Candidate] = []
    exclusions: list[tuple[str, str]] = []
    for row in rows:
        if selected is not None and row.path not in selected:
            continue
        if not row.java_source or row.status == "SAME":
            continue
        if row.status == "REMOVED":
            exclusions.append((row.path, "TYPED_EXCLUSION_AUTOMATIC_REMOVAL"))
            continue
        if row.status not in {"MODIFIED", "ADDED"}:
            exclusions.append((row.path, "TYPED_EXCLUSION_UNKNOWN_STATUS"))
            continue

        try:
            after_text = _utf8(
                _git_bytes(repo, row.donor_ref, row.path),
                f"{row.donor_ref}:{row.path}",
            )
            if row.status == "ADDED":
                before_hash = "ABSENT"
            else:
                before_text = _utf8(
                    _git_bytes(repo, row.baseline_ref, row.path),
                    f"{row.baseline_ref}:{row.path}",
                )
                before_hash = _sha256_text(before_text)
            after_hash = _sha256_text(after_text)
        except ValueError as failure:
            exclusions.append((row.path, f"TYPED_EXCLUSION_ENCODING:{failure}"))
            continue

        if before_hash == after_hash:
            raise AssertionError(f"verbatim MODIFIED/ADDED row has no delta: {row.path}")
        result.append(
            Candidate(
                path=row.path,
                status=row.status,
                before_sha256=before_hash,
                after_sha256=after_hash,
                after_text=after_text,
            )
        )

    result.sort(key=lambda item: item.path)
    exclusions.sort()
    return result, exclusions


def _chunks(values: Sequence[Candidate], size: int) -> Iterable[Sequence[Candidate]]:
    for start in range(0, len(values), size):
        yield values[start : start + size]


def materialize(
    out: Path,
    release: int,
    candidates_: Sequence[Candidate],
    exclusions: Sequence[tuple[str, str]],
    crate_size: int = CRATE_LIMIT,
) -> list[str]:
    if crate_size < 1 or crate_size > CRATE_LIMIT:
        raise ValueError(f"crate_size must be between 1 and {CRATE_LIMIT}")

    resource_root = out / RESOURCE_ROOT
    yaml_root = out / YAML_ROOT
    resource_root.mkdir(parents=True, exist_ok=True)
    yaml_root.mkdir(parents=True, exist_ok=True)

    crate_names: list[str] = []
    crate_rows: list[tuple[str, ...]] = []
    for crate_index, chunk in enumerate(_chunks(candidates_, crate_size), 1):
        crate_name = f"jdk{release}-{crate_index:04d}"
        crate_names.append(crate_name)
        crate_dir = resource_root / crate_name
        crate_dir.mkdir(parents=True, exist_ok=True)

        manifest_rows: list[tuple[str, str, str, str]] = []
        for resource_index, candidate in enumerate(chunk, 1):
            resource_name = f"{resource_index:04d}.java.txt"
            (crate_dir / resource_name).write_text(
                candidate.after_text,
                encoding="utf-8",
                newline="",
            )
            manifest_rows.append(
                (
                    candidate.path,
                    candidate.before_sha256,
                    candidate.after_sha256,
                    resource_name,
                )
            )

        with (crate_dir / "manifest.tsv").open(
            "w", encoding="utf-8", newline=""
        ) as handle:
            writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
            writer.writerows(manifest_rows)

        crate_rows.append(
            (
                crate_name,
                str(release),
                "jdk-21+35",
                f"jdk-{release}+"
                + {22: "36", 23: "37", 24: "36", 25: "36", 26: "35", 27: "35"}[release],
                str(len(chunk)),
                chunk[0].path,
                chunk[-1].path,
                "CANDIDATE_UNVERIFIED",
            )
        )

    with (out / "CRATES.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(
            (
                "crate_name",
                "release",
                "baseline_ref",
                "donor_ref",
                "target_count",
                "first_path",
                "last_path",
                "status",
            )
        )
        writer.writerows(crate_rows)

    with (out / "EXCLUSIONS.tsv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
        writer.writerow(("path", "reason"))
        writer.writerows(exclusions)

    yaml = []
    for crate_name in crate_names:
        recipe_name = crate_name.replace("-", "_")
        yaml.extend(
            [
                "---",
                "type: specs.openrewrite.org/v1beta/recipe",
                f"name: com.m3.generated.{recipe_name}",
                f"displayName: M3JDK21 candidate crate {crate_name}",
                "description: Exact hash-pinned candidate; compatibility proof required before admission.",
                "recipeList:",
                "  - com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe:",
                f"      crateName: {crate_name}",
                "",
            ]
        )

    yaml.extend(
        [
            "---",
            "type: specs.openrewrite.org/v1beta/recipe",
            f"name: com.m3.generated.jdk{release}.CandidateBackports",
            f"displayName: M3JDK21 JDK {release} candidate backports",
            "description: Candidate-only composition of generated hash-pinned donor crates.",
            "recipeList:",
        ]
    )
    for crate_name in crate_names:
        recipe_name = crate_name.replace("-", "_")
        yaml.append(f"  - com.m3.generated.{recipe_name}")
    yaml.append("")

    (yaml_root / f"m3-jdk{release}-candidate-backports.yml").write_text(
        "\n".join(yaml),
        encoding="utf-8",
    )
    return crate_names


def _parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--release", type=int, choices=range(22, 28), required=True)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--paths-file", type=Path)
    parser.add_argument("--all-candidates", action="store_true")
    parser.add_argument(
        "--crate-size",
        type=int,
        default=CRATE_LIMIT,
        choices=range(1, CRATE_LIMIT + 1),
        help=(
            "maximum targets per generated crate; use 1 for true FILE-scope recipe atoms "
            "before explicit DAG promotion"
        ),
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str] | None = None) -> int:
    args = _parse_args(sys.argv[1:] if argv is None else argv)
    repo = args.repo.resolve()
    out = args.out.resolve()
    selected = _selected_paths(args.paths_file)
    candidate_rows, exclusions = candidates(
        repo,
        args.release,
        selected,
        args.all_candidates,
    )
    if not candidate_rows:
        raise SystemExit("no Java donor candidates selected")
    crates = materialize(
        out,
        args.release,
        candidate_rows,
        exclusions,
        crate_size=args.crate_size,
    )
    print(
        f"generated {len(crates)} crate(s) at max {args.crate_size} target(s)/crate, "
        f"{len(candidate_rows)} Java candidate(s), "
        f"{len(exclusions)} typed exclusion(s)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
