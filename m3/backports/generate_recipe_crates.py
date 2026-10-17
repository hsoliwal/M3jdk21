#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# Modified 2026-10-17: route exact existing target source to owner review before donor intake.
# SPDX-License-Identifier: Apache-2.0
"""Generate candidate hash-pinned OpenRewrite crates from exact JDK21/donor file pairs.

The generator never admits compatibility. By default it preserves the historical Java-only
behavior and turns selected exact Java deltas into deterministic resources consumed by
M3Jdk21HashPinnedSnapshotRecipe. Native/JNI C/C++ deltas can be selected explicitly with
--include-native and are emitted through M3Jdk21HashPinnedTextSnapshotRecipe as source-sealed FILE
atoms; PlainText replay is byte custody, not a C/C++ semantic parser. The historical --include-text
mode remains a broader opt-in for strict UTF-8 non-Java deltas. With --target-ref, exact existing files are retained and nominated for owner review;
current M3 drift is held before candidate generation. Neither path existence nor Git
blob equality proves complete JEP implementation. Generated crates remain
CANDIDATE_UNVERIFIED until recipe JUnit, native/JDK build, jtreg/runtime and compatibility gates pass.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import importlib.util
import subprocess
import sys
from dataclasses import asdict, dataclass, replace
from pathlib import Path
from typing import Iterable, Sequence

CRATE_LIMIT = 256
RESOURCE_ROOT = Path(
    "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned"
)
TEXT_RESOURCE_ROOT = Path(
    "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text"
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
    kind: str = "JAVA"
    donor_ref: str = ""


@dataclass(frozen=True)
class TargetReuse:
    """Exact file custody and review routing, never JEP or transformation admission."""

    path: str
    kind: str
    current_commit: str
    current_status: str
    current_mode: str
    current_object_type: str
    current_git_blob: str
    current_sha256: str
    baseline_commit: str
    baseline_status: str
    baseline_mode: str
    baseline_object_type: str
    baseline_git_blob: str
    baseline_sha256: str
    donor_commit: str
    donor_status: str
    donor_mode: str
    donor_object_type: str
    donor_git_blob: str
    donor_sha256: str
    route: str
    next_action: str
    source_mutation_authority: str = "false"
    compatibility_authority: str = "false"
    feature_acceptance: str = "false"


@dataclass(frozen=True)
class TargetReusePlan:
    baseline_commit: str
    donor_commit: str
    target_commit: str
    donor_overrides: tuple[tuple[str, str], ...]
    rows: tuple[TargetReuse, ...]


def _commit(repo: Path, ref: str) -> str:
    """Resolve once, then read every tree/blob through the immutable commit identity."""
    if not ref or ref.startswith("-") or any(ord(char) < 33 for char in ref):
        raise ValueError("invalid source ref")
    value = DELTA._git(
        repo, "rev-parse", "--verify", "--end-of-options", f"{ref}^{{commit}}"
    ).strip()
    if len(value) not in {40, 64} or any(char not in "0123456789abcdef" for char in value):
        raise ValueError("Git did not return an immutable commit identity")
    return value


def _reuse_path(path: str) -> str:
    if (
        not path or len(path) > 4096 or path.startswith("/") or "\\" in path or ":" in path
        or any(ord(char) < 32 or ord(char) == 127 for char in path)
        or any(part in {"", ".", "..", ".git"} for part in path.split("/"))
    ):
        raise ValueError(f"noncanonical reuse path: {path!r}")
    return path


def _reuse_kind(path: str) -> str:
    if path.endswith(".java"):
        return "JAVA"
    return "NATIVE" if DELTA._native_source(path) else "RESOURCE"


def _java_a3_review(row: TargetReuse) -> bool:
    return (
        row.route == "ALREADY_PRESENT_ATOMIZE_PATTERNIZE"
        and row.kind == "JAVA"
        and row.path.startswith(("src/", "test/"))
    )


def plan_target_reuse(
    repo: Path,
    release: int,
    selected: set[str] | None,
    all_candidates: bool,
    *,
    target_ref: str,
    target_git: Path | None = None,
    include_text: bool = False,
    include_native: bool = False,
    donor_ref: str | None = None,
    donor_overrides: dict[str, str] | None = None,
) -> TargetReusePlan:
    """Compare the locked baseline, exact donor and current target before import.

    Equal PRESENT blobs with equal Git modes nominate the existing source owner for
    review. This is file-byte evidence only: dependency closure, adapted implementations,
    Java/JNI behavior and complete JEP implementation require separate proof.
    """
    if selected is None and not all_candidates:
        raise ValueError("select --paths-file or explicitly request --all-candidates")
    releases = dict(DELTA.DONORS)
    if release not in releases:
        raise ValueError(f"unsupported donor release: {release}")
    source = repo.resolve()
    target = (target_git if target_git is not None else repo).resolve()
    baseline_commit = _commit(source, DELTA.BASELINE[1])
    donor_commit = _commit(source, donor_ref or releases[release])
    target_commit = _commit(target, target_ref)
    overrides = dict(donor_overrides or {})
    if selected is not None and set(overrides) - selected:
        raise ValueError("donor-map paths outside selected set")
    pinned_overrides = {
        _reuse_path(path): _commit(source, ref)
        for path, ref in sorted(overrides.items())
    }

    # Reuse the existing full-tree/delta owner. Every comparison receives immutable refs.
    comparisons = {
        ref: {
            row.path: row
            for row in DELTA.compare_refs(source, release, baseline_commit, ref)
        }
        for ref in sorted({donor_commit, *pinned_overrides.values()})
    }
    baseline = DELTA._tree(source, baseline_commit)
    current = DELTA._tree(target, target_commit)
    donors = {ref: DELTA._tree(source, ref) for ref in comparisons}
    selected_paths = sorted(
        selected if selected is not None
        else set(baseline) | set(current) | set(donors[donor_commit]) | set(pinned_overrides)
    )
    if set(pinned_overrides) - set(comparisons[donor_commit]):
        raise ValueError("donor-map paths absent from baseline/default comparison")

    def sha(entry) -> str:
        if entry is None:
            return "ABSENT"
        if entry.object_type != "blob":
            return "NOT_BLOB"
        # Git blob identity already binds the bytes. Do not fetch a duplicate payload
        # merely to emit a second hash, especially from a filtered/promisor checkout.
        return "NOT_MATERIALIZED"

    rows: list[TargetReuse] = []
    for path in selected_paths:
        path = _reuse_path(path)
        chosen_ref = pinned_overrides.get(path, donor_commit)
        before, now, after = baseline.get(path), current.get(path), donors[chosen_ref].get(path)
        source_kind = _reuse_kind(path)
        enabled = source_kind == "JAVA" or include_text or (source_kind == "NATIVE" and include_native)
        regular = all(
            entry is None or (entry.object_type == "blob" and entry.mode in {"100644", "100755"})
            for entry in (now, after)
        )
        delta = comparisons[chosen_ref].get(path)
        if not enabled:
            route, action = "SOURCE_KIND_NOT_SELECTED", "SELECT_THE_EXPLICIT_SOURCE_LANE"
        elif not regular:
            route, action = "HOLD_UNSUPPORTED_FILE_MODE", "REVIEW_EXISTING_SOURCE_KIND_AND_MODE"
        elif after is None:
            route = "NO_DONOR_IMPLEMENTATION" if now is None else "HOLD_DONOR_PATH_ABSENT"
            action = "REVIEW_MISSING_DONOR_SOURCE_WITHOUT_REMOVING_TARGET"
        elif now == after:
            route = "ALREADY_PRESENT_ATOMIZE_PATTERNIZE"
            if source_kind == "JAVA" and path.startswith(("src/", "test/")):
                action = "REVIEW_EXISTING_A3_JAVA_MASTERY_AND_CONTRACT"
            elif source_kind == "NATIVE":
                action = "REVIEW_EXISTING_NATIVE_OWNER_AND_NATIVE_GATES"
            else:
                action = "REVIEW_EXISTING_RESOURCE_OR_SOURCE_OWNER"
        elif now == before and delta is not None and delta.status in {"ADDED", "MODIFIED"}:
            route, action = "MECHANICAL_FILE_REPLAY", "GENERATE_CANDIDATE_THEN_PROVE_COMPATIBILITY"
        else:
            route, action = "HOLD_CURRENT_TREE_DRIFT", "PRESERVE_TARGET_AND_RECONCILE_OWNING_RECIPE"
        rows.append(TargetReuse(
            path, source_kind,
            target_commit, "ABSENT" if now is None else "PRESENT",
            "" if now is None else now.mode, "" if now is None else now.object_type,
            "" if now is None else now.oid,
            sha(now),
            baseline_commit, "ABSENT" if before is None else "PRESENT",
            "" if before is None else before.mode, "" if before is None else before.object_type,
            "" if before is None else before.oid,
            sha(before),
            chosen_ref, "ABSENT" if after is None else "PRESENT",
            "" if after is None else after.mode, "" if after is None else after.object_type,
            "" if after is None else after.oid,
            sha(after), route, action,
        ))
    return TargetReusePlan(
        baseline_commit, donor_commit, target_commit,
        tuple(sorted(pinned_overrides.items())), tuple(rows),
    )


def write_target_reuse(plan: TargetReusePlan, out: Path) -> None:
    """Write full custody and Java-only A3 review input; neither is executable admission."""
    out.mkdir(parents=True, exist_ok=True)
    columns = tuple(TargetReuse.__dataclass_fields__)
    for name, rows in (
        ("TARGET_REUSE.tsv", plan.rows),
        ("A3_EXISTING_SOURCES.tsv", tuple(row for row in plan.rows if _java_a3_review(row))),
    ):
        with (out / name).open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=columns, delimiter="\t", lineterminator="\n")
            writer.writeheader()
            writer.writerows(asdict(row) for row in rows)


def _verify_reuse_candidates(plan: TargetReusePlan, values: Sequence[Candidate]) -> TargetReusePlan:
    by_path = {row.path: row for row in plan.rows}
    for candidate in values:
        row = by_path.get(candidate.path)
        if (
            row is None or row.route != "MECHANICAL_FILE_REPLAY"
            or candidate.donor_ref != row.donor_commit
            or row.current_git_blob != row.baseline_git_blob
            or row.current_mode != row.baseline_mode
            or row.current_object_type != row.baseline_object_type
            or candidate.after_sha256 != _sha256_text(candidate.after_text)
        ):
            raise ValueError(f"target reuse candidate custody drift: {candidate.path}")
        data = candidate.after_text.encode("utf-8")
        blob = f"blob {len(data)}\0".encode("ascii") + data
        digest = hashlib.sha1(blob) if len(row.donor_git_blob) == 40 else hashlib.sha256(blob)
        if digest.hexdigest() != row.donor_git_blob:
            raise ValueError(f"target reuse candidate blob drift: {candidate.path}")
        by_path[candidate.path] = replace(
            row, current_sha256=candidate.before_sha256,
            baseline_sha256=candidate.before_sha256, donor_sha256=candidate.after_sha256,
        )
    return replace(plan, rows=tuple(by_path[row.path] for row in plan.rows))




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
        raise ValueError(f"{label}: source is not UTF-8") from failure
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


def _donor_overrides(path_file: Path | None) -> dict[str, str]:
    """Read exact path -> approved donor-ref overrides.

    The map is custody only: it cannot add target paths and it does not decide compatibility.
    """
    if path_file is None:
        return {}
    with path_file.open(encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    if not rows or set(rows[0]) != {"path", "donor_ref"}:
        raise ValueError("donor map must have path and donor_ref columns")

    result: dict[str, str] = {}
    previous = ""
    for row in rows:
        path = row["path"].strip()
        ref = row["donor_ref"].strip()
        if (
            not path
            or len(ref) != 40
            or any(ch not in "0123456789abcdef" for ch in ref)
            or path.startswith("/")
            or "\\" in path
            or ".." in path.split("/")
            or path <= previous
            or path in result
        ):
            raise ValueError(f"noncanonical donor-map row: {row}")
        previous = path
        result[path] = ref
    return result


def candidates(
    repo: Path,
    release: int,
    selected: set[str] | None,
    all_candidates: bool,
    include_text: bool = False,
    include_native: bool = False,
    donor_ref: str | None = None,
    donor_overrides: dict[str, str] | None = None,
    baseline_ref: str | None = None,
) -> tuple[list[Candidate], list[tuple[str, str]]]:
    if selected is None and not all_candidates:
        raise ValueError("select --paths-file or explicitly request --all-candidates")

    rows = (
        DELTA.compare_refs(repo, release, baseline_ref or DELTA.BASELINE[1], donor_ref)
        if donor_ref
        else DELTA.compare_refs(repo, release, baseline_ref, dict(DELTA.DONORS)[release])
        if baseline_ref
        else DELTA.compare(repo, (release,))
    )
    by_path = {row.path: row for row in rows}
    if selected is not None:
        missing = sorted(selected - set(by_path))
        if missing:
            raise ValueError(f"selected paths absent from comparison: {missing[:5]}")

    overrides = dict(donor_overrides or {})
    if selected is not None:
        outside = sorted(set(overrides) - selected)
        if outside:
            raise ValueError(f"donor-map paths outside selected set: {outside[:5]}")
    missing_overrides = sorted(set(overrides) - set(by_path))
    if missing_overrides:
        raise ValueError(
            f"donor-map paths absent from baseline/default comparison: {missing_overrides[:5]}"
        )

    by_ref: dict[str, list[str]] = {}
    for override_path, override_ref in overrides.items():
        by_ref.setdefault(override_ref, []).append(override_path)
    for override_ref, override_paths in sorted(by_ref.items()):
        override_rows = {
            row.path: row
            for row in DELTA.compare_refs(
                repo, release, baseline_ref or DELTA.BASELINE[1], override_ref
            )
        }
        for override_path in override_paths:
            by_path[override_path] = override_rows[override_path]

    paths = sorted(selected if selected is not None else by_path)
    result: list[Candidate] = []
    exclusions: list[tuple[str, str]] = []
    for path in paths:
        row = by_path[path]
        if row.status == "SAME":
            continue
        if not row.java_source:
            if row.native_source:
                if not (include_native or include_text):
                    continue
            elif not include_text:
                continue
        if row.status == "REMOVED":
            exclusions.append((row.path, "TYPED_EXCLUSION_AUTOMATIC_REMOVAL"))
            continue
        if row.status not in {"MODIFIED", "ADDED"}:
            exclusions.append((row.path, "TYPED_EXCLUSION_UNKNOWN_STATUS"))
            continue

        if row.donor_mode != "100644" or (
            row.status == "MODIFIED" and row.baseline_mode != row.donor_mode
        ):
            exclusions.append(
                (
                    row.path,
                    "TYPED_EXCLUSION_FILE_MODE:"
                    f"{row.baseline_mode or 'ABSENT'}->{row.donor_mode or 'ABSENT'}",
                )
            )
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
                kind=(
                    "JAVA"
                    if row.java_source
                    else "NATIVE"
                    if row.native_source and include_native
                    else "TEXT"
                ),
                donor_ref=row.donor_ref,
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
    baseline_ref: str = "jdk-21+35",
    donor_ref: str | None = None,
) -> list[str]:
    if crate_size < 1 or crate_size > CRATE_LIMIT:
        raise ValueError(f"crate_size must be between 1 and {CRATE_LIMIT}")

    yaml_root = out / YAML_ROOT
    yaml_root.mkdir(parents=True, exist_ok=True)

    kinds = {candidate.kind for candidate in candidates_}
    if not kinds.issubset({"JAVA", "NATIVE", "TEXT"}):
        raise ValueError(f"unsupported candidate kind(s): {sorted(kinds)}")

    qualified_names = len(kinds) > 1 or "NATIVE" in kinds
    lanes: list[tuple[str, list[Candidate], Path, str, str]] = []
    java_candidates = [candidate for candidate in candidates_ if candidate.kind == "JAVA"]
    native_candidates = [candidate for candidate in candidates_ if candidate.kind == "NATIVE"]
    text_candidates = [candidate for candidate in candidates_ if candidate.kind == "TEXT"]
    if java_candidates:
        lanes.append(
            (
                "java",
                java_candidates,
                RESOURCE_ROOT,
                "com.m3.rewrite.backport.M3Jdk21HashPinnedSnapshotRecipe",
                ".java.txt",
            )
        )
    if native_candidates:
        lanes.append(
            (
                "native",
                native_candidates,
                TEXT_RESOURCE_ROOT,
                "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                ".txt",
            )
        )
    if text_candidates:
        lanes.append(
            (
                "text",
                text_candidates,
                TEXT_RESOURCE_ROOT,
                "com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe",
                ".txt",
            )
        )

    crate_names: list[str] = []
    crate_types: dict[str, str] = {}
    crate_rows: list[tuple[str, ...]] = []
    for lane_name, lane_candidates, resource_root_path, recipe_class, suffix in lanes:
        resource_root = out / resource_root_path
        resource_root.mkdir(parents=True, exist_ok=True)
        for crate_index, chunk in enumerate(_chunks(lane_candidates, crate_size), 1):
            if qualified_names:
                crate_name = f"jdk{release}-{lane_name}-{crate_index:04d}"
            else:
                crate_name = f"jdk{release}-{crate_index:04d}"
            crate_names.append(crate_name)
            crate_types[crate_name] = recipe_class
            crate_dir = resource_root / crate_name
            crate_dir.mkdir(parents=True, exist_ok=True)

            manifest_rows: list[tuple[str, str, str, str]] = []
            for resource_index, candidate in enumerate(chunk, 1):
                resource_name = f"{resource_index:04d}{suffix}"
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
                    baseline_ref,
                    ";".join(
                        sorted(
                            {
                                candidate.donor_ref
                                for candidate in chunk
                                if candidate.donor_ref
                            }
                        )
                    )
                    or donor_ref
                    or (
                        f"jdk-{release}+"
                        + {22: "36", 23: "37", 24: "36", 25: "36", 26: "35", 27: "35"}[release]
                    ),
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
                f"  - {crate_types[crate_name]}:",
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
    parser.add_argument(
        "--target-ref",
        help="optional current target ref; resolved to an immutable commit before exact reuse routing",
    )
    parser.add_argument(
        "--target-git", type=Path,
        help="current target Git repository for --target-ref; defaults to --repo",
    )
    parser.add_argument(
        "--donor-ref",
        help="exact donor commit/ref; defaults to the selected release GA tag",
    )
    parser.add_argument("--paths-file", type=Path)
    parser.add_argument(
        "--donor-map",
        type=Path,
        help=(
            "optional sorted TSV path->donor_ref overrides; paths must already be selected "
            "by the packet and every ref is verified by Git"
        ),
    )
    parser.add_argument("--all-candidates", action="store_true")
    parser.add_argument(
        "--include-text",
        action="store_true",
        help=(
            "legacy broad opt-in: emit strict UTF-8 non-Java candidates through "
            "M3Jdk21HashPinnedTextSnapshotRecipe; default remains Java-only"
        ),
    )
    parser.add_argument(
        "--include-native",
        action="store_true",
        help=(
            "emit C/C++/header/assembly deltas as explicit NATIVE source-sealed FILE atoms "
            "through M3Jdk21HashPinnedTextSnapshotRecipe"
        ),
    )
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
    donor_overrides = _donor_overrides(args.donor_map)
    if args.target_git is not None and args.target_ref is None:
        raise ValueError("--target-git requires --target-ref")
    if args.target_ref is not None:
        if out.exists() and (not out.is_dir() or any(out.iterdir())):
            raise ValueError("target reuse output must be a new or empty directory")
        plan = plan_target_reuse(
            repo, args.release, selected, args.all_candidates,
            target_ref=args.target_ref, target_git=args.target_git,
            include_text=args.include_text, include_native=args.include_native,
            donor_ref=args.donor_ref, donor_overrides=donor_overrides,
        )
        import_paths = {row.path for row in plan.rows if row.route == "MECHANICAL_FILE_REPLAY"}
        candidate_rows, exclusions = candidates(
            repo, args.release, import_paths, False,
            include_text=args.include_text, include_native=args.include_native,
            donor_ref=plan.donor_commit,
            donor_overrides={path: ref for path, ref in plan.donor_overrides if path in import_paths},
            baseline_ref=plan.baseline_commit,
        )
        plan = _verify_reuse_candidates(plan, candidate_rows)
        if candidate_rows:
            crates = materialize(
                out, args.release, candidate_rows, exclusions,
                crate_size=args.crate_size,
                baseline_ref=plan.baseline_commit, donor_ref=plan.donor_commit,
            )
        else:
            # Do not generate an empty runnable YAML recipe, or leave an old crate behind.
            out.mkdir(parents=True, exist_ok=True)
            (out / "CRATES.tsv").write_text(
                "crate_name\trelease\tbaseline_ref\tdonor_ref\ttarget_count\t"
                "first_path\tlast_path\tstatus\n", encoding="utf-8",
            )
            with (out / "EXCLUSIONS.tsv").open("w", encoding="utf-8", newline="") as handle:
                writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
                writer.writerow(("path", "reason"))
                writer.writerows(exclusions)
            crates = []
        write_target_reuse(plan, out)
        print(
            f"generated {len(crates)} candidate crate(s); "
            f"already_present={sum(row.route == 'ALREADY_PRESENT_ATOMIZE_PATTERNIZE' for row in plan.rows)} "
            f"java_a3_review={sum(_java_a3_review(row) for row in plan.rows)} "
            f"target={plan.target_commit}; no JEP/runtime acceptance"
        )
        return 0
    candidate_rows, exclusions = candidates(
        repo,
        args.release,
        selected,
        args.all_candidates,
        include_text=args.include_text,
        include_native=args.include_native,
        donor_ref=args.donor_ref,
        donor_overrides=donor_overrides,
    )
    if not candidate_rows:
        raise SystemExit("no donor candidates selected")
    crates = materialize(
        out,
        args.release,
        candidate_rows,
        exclusions,
        crate_size=args.crate_size,
        baseline_ref=DELTA.BASELINE[1],
        donor_ref=args.donor_ref,
    )
    print(
        f"generated {len(crates)} crate(s) at max {args.crate_size} target(s)/crate, "
        f"{len(candidate_rows)} candidate(s), "
        f"{len(exclusions)} typed exclusion(s)"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
