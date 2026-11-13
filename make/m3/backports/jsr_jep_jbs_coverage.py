#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Fail-closed JSR/JEP/JBS release-denominator join for Java 21 M3JDK21.

Projection only. Existing release_jep_authority, jep_source_witness and
compatibility_queue own facts and policies. A JBS commit subject is NOT a
verified JBS issue, and no row proves that a backport was applied or tested.
"""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Mapping, Sequence

import compatibility_queue
import inventory as upstream_inventory
import jep_source_witness
import release_jep_authority

# Strict release-specification crosswalk for the existing JDK 22..27 authority.
# M3JDK21's locked baseline is JSR 396 / source 21 / classfile 65.
PLATFORM = (
    (22, 397, 66, 2384),
    (23, 398, 67, 2355),
    (24, 399, 68, 2562),
    (25, 400, 69, 2678),
    (26, 401, 70, 2611),
    (27, 402, 71, 2358),
)
PENDING = "PENDING_COMPATIBILITY_PROOF"
# Exact OpenJDK GA tag-to-commit identities verified against upstream refs/tags.
# Pinned source commit IDs, not user-provided names, are the history authority.
GA_TAG_COMMIT_PINS: tuple[tuple[str, str], ...] = (
    ("jdk-21+35", "890adb6410dab4606a4f26a942aed02fb2f55387"),
    ("jdk-22+36", "fe9f05023e5a916b21e2db72fa5b1e8368a2c07d"),
    ("jdk-23+37", "9ad2e63f176364b96a827af80055e7db4b61fc9a"),
    ("jdk-24+36", "6705a9255d28f351950e7fbca9d05e73942a4e27"),
    ("jdk-25+36", "6c48f4ed707bf0b15f9b6098de30db8aae6fa40f"),
    ("jdk-26+35", "4408cd2a07a14243a58cd9d30813302bfbe81133"),
    ("jdk-27+35", "815ff4dc327fe17f2433c7d115a5a503af10f3c4"),
)
INVENTORY_COLUMNS = (
    "release", "base_ref", "head_ref", "commit", "jbs_ids", "subject",
    "domain", "javac_touch", "grammar_touch", "hotspot_compiler_touch",
    "compatibility_signal", "disposition", "paths",
)
QUEUE_COLUMNS = (
    "order", "release", "commit", "jbs_ids", "subject", "domain",
    "inventory_disposition", "risk", "scope_floor", "proof_lane",
    "recipe_strategy", "priority", "compatibility_state", "next_action", "paths",
)
COVERAGE_COLUMNS = (
    "kind", "release", "authority_id", "upstream_commit", "disposition", "proof_state",
)
ISSUE_INDEX_COLUMNS = (
    "jbs_id", "release_list", "source_commit_count", "source_commit_shas", "proof_state",
)
ISSUE = re.compile(r"JDK-[0-9]{7}\Z")
SHA = re.compile(r"[0-9a-f]{40}\Z")


def load_tsv(path: Path, columns: tuple[str, ...], *, large: bool = False) -> list[dict[str, str]]:
    """Strict, bounded TSV input; restore process-global csv limit on all outcomes."""
    original_limit = csv.field_size_limit()
    try:
        if large:
            csv.field_size_limit(4 * 1024 * 1024)
        with path.open(encoding="utf-8", newline="") as stream:
            reader = csv.DictReader(stream, delimiter="\t", strict=True)
            if tuple(reader.fieldnames or ()) != columns:
                raise ValueError(f"unexpected TSV schema: {path.name}")
            rows = list(reader)
    finally:
        csv.field_size_limit(original_limit)
    if not rows or any(None in row or any(v is None for v in row.values()) for row in rows):
        raise ValueError(f"empty or incomplete TSV: {path.name}")
    return rows


def verify_inventory_queue(
    inventory_rows: Sequence[Mapping[str, str]],
    queue_rows: Sequence[Mapping[str, str]],
) -> int:
    """Recompute every queue field with the original M3JDK21 classifier.

    This binds the projection to the supplied inventory. Independent GA-tag
    commit-identity proof remains necessary before runtime promotion.
    """
    # Reuse the exact tag intervals and subject parser owned by the upstream scanner.
    # The new normalized OpenJDK prefix parser is selected automatically if admitted.
    intervals = {
        release: (base, head)
        for release, base, head in upstream_inventory.INTERVALS
    }
    parser = getattr(upstream_inventory, "_subject_jbs_ids", None)
    for index, row in enumerate(inventory_rows):
        ref_pair = intervals.get(int(row["release"]))
        if ref_pair is None or (row["base_ref"], row["head_ref"]) != ref_pair:
            raise ValueError(f"INVENTORY_GA_TAG_INTERVAL_DRIFT row={index}")
        expected_ids = (
            tuple(parser(row["subject"]))
            if parser is not None
            else tuple(dict.fromkeys(upstream_inventory.JBS_RE.findall(row["subject"])))
        )
        if row["jbs_ids"] != ",".join(expected_ids):
            raise ValueError(f"INVENTORY_SUBJECT_JBS_ID_DRIFT row={index}")
    replayed = compatibility_queue.queue(inventory_rows)
    if len(replayed) != len(queue_rows):
        raise ValueError("INVENTORY_QUEUE_CARDINALITY_DRIFT")
    for position, (expected, observed) in enumerate(zip(replayed, queue_rows, strict=True)):
        for field in QUEUE_COLUMNS:
            if observed[field] != str(getattr(expected, field)):
                raise ValueError(
                    f"INVENTORY_QUEUE_SOURCE_DRIFT row={position} field={field}"
                )
    return len(replayed)


def verify_actual_git_history(
    source_repo: Path,
    inventory_rows: Sequence[Mapping[str, str]],
    *,
    tag_pins: Sequence[tuple[str, str]] = GA_TAG_COMMIT_PINS,
) -> int:
    """Recreate all original inventory facts from exact pinned GA Git objects.

    Uses the incumbent OpenJDK inventory reader rather than approximating its
    parsing or attribution. Git checkout evidence is not Java21 runtime proof.
    """
    repo = source_repo.resolve(strict=True)
    if not repo.is_dir():
        raise ValueError("UPSTREAM_GIT_DIRECTORY_REQUIRED")
    intervals = upstream_inventory.INTERVALS
    tag_names = tuple(tag for tag, _digest in tag_pins)
    required = (intervals[0][1], *(head for _release, _base, head in intervals))
    if tag_names != required or len(tag_names) != len(set(tag_names)):
        raise ValueError("UPSTREAM_GA_TAG_SET_DRIFT")
    for tag, pinned_commit in tag_pins:
        observed = upstream_inventory._git(
            repo, "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}"
        ).strip()
        if observed != pinned_commit:
            raise ValueError(f"UPSTREAM_GA_TAG_COMMIT_DRIFT tag={tag}")
    releases = tuple(release for release, _base, _head in intervals)
    recomputed = upstream_inventory.inventory(repo, releases)
    if len(recomputed) != len(inventory_rows):
        raise ValueError("UPSTREAM_INVENTORY_CARDINALITY_DRIFT")
    for index, (expected, observed) in enumerate(
            zip(recomputed, inventory_rows, strict=True)):
        for field in INVENTORY_COLUMNS:
            value = getattr(expected, field)
            if field in ("jbs_ids", "paths"):
                canonical = ",".join(value)
            elif isinstance(value, bool):
                canonical = str(value).lower()
            else:
                canonical = str(value)
            if observed[field] != canonical:
                raise ValueError(
                    f"UPSTREAM_INVENTORY_GIT_DRIFT row={index} field={field}"
                )
    return len(recomputed)


def jbs_issue_index(queue_rows: Sequence[Mapping[str, str]]) -> list[dict[str, str]]:
    """Group every source-subject JBS candidate by exact commit; never mark it fixed.

    This is a projection of the already verified queue, not another JBS ID
    tokenizer or an independent JBS database. Unlinked commits stay unlinked.
    """
    associations: dict[str, set[tuple[int, str]]] = {}
    seen_commits: set[str] = set()
    for position, row in enumerate(queue_rows):
        release = int(row["release"])
        commit = row["commit"]
        if release not in range(22, 28) or not SHA.fullmatch(commit) or commit in seen_commits:
            raise ValueError(f"INVALID_ISSUE_SOURCE_COMMIT row={position}")
        seen_commits.add(commit)
        if row["compatibility_state"] != PENDING:
            raise ValueError(f"UNVERIFIED_ISSUE_SOURCE_PROMOTED row={position}")
        ids = row["jbs_ids"].split(",") if row["jbs_ids"] else []
        if len(ids) != len(set(ids)) or any(not ISSUE.fullmatch(s) for s in ids):
            raise ValueError(f"INVALID_ISSUE_SOURCE_IDS row={position}")
        for issue in ids:
            associations.setdefault(issue, set()).add((release, commit))
    return [
        {"jbs_id": issue,
         "release_list": ",".join(str(value) for value in sorted({r for r, _ in refs})),
         "source_commit_count": str(len(refs)),
         "source_commit_shas": ",".join(commit for _release, commit in sorted(refs)),
         "proof_state": PENDING}
        for issue, refs in sorted(associations.items())
    ]


def render_jbs_index_tsv(items: Sequence[Mapping[str, str]]) -> str:
    out = io.StringIO(newline="")
    writer = csv.DictWriter(out, fieldnames=ISSUE_INDEX_COLUMNS,
                            delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(items)
    return out.getvalue()


def require_complete_jbs_issue_parser() -> None:
    """Block full issue-index output until the original numeric parser is admitted.

    Do not duplicate the JBS parser: the canonical owner is M3JDK21
    inventory._subject_jbs_ids from the previously merged Synexia recipe.
    """
    parser = getattr(upstream_inventory, "_subject_jbs_ids", None)
    if not callable(parser):
        raise ValueError("JBS_ISSUE_INDEX_NUMERIC_PARSER_MISSING")
    controls = (
        ("8345678: fix", ("JDK-8345678",)),
        ("8345678, 8345679: two fixes", ("JDK-8345678", "JDK-8345679")),
        ("8320175: [BACKOUT] 8316533: regression", ("JDK-8320175", "JDK-8316533")),
        ("Merge 8345678 fix", ()),
    )
    for subject, expected in controls:
        if tuple(parser(subject)) != expected:
            raise ValueError("JBS_ISSUE_INDEX_NUMERIC_PARSER_INCOMPATIBLE")


def join(
    authorities: Mapping[int, Sequence[int]],
    catalogue: Sequence[Mapping[str, str]],
    seed_rows: Sequence[Mapping[str, str]],
    queue_rows: Sequence[Mapping[str, str]],
    *,
    platform: Sequence[tuple[int, int, int, int]] = PLATFORM,
) -> tuple[list[dict[str, str]], dict[str, object]]:
    """One receipt per JSR, JEP and upstream *commit*, never per imagined bug fix."""
    expected = {release: count for release, _jsr, _major, count in platform}
    if (not platform or tuple(sorted(expected)) != tuple(sorted(authorities))
            or tuple(release for release, *_ in platform) != tuple(sorted(expected))
            or len(platform) != len(expected)
            or len({jsr for _, jsr, _, _ in platform}) != len(platform)):
        raise ValueError("JSR_RELEASE_DENOMINATOR_MISMATCH")
    if any(major <= 65 or jsr <= 396 or count < 1 for _, jsr, major, count in platform):
        raise ValueError("JAVA21_PLATFORM_CONTRACT_INVALID")

    catalogue_keys = [(int(row["release"]), int(row["jep"])) for row in catalogue]
    expected_keys = [(release, jep) for release in sorted(authorities)
                     for jep in authorities[release]]
    if catalogue_keys != expected_keys or len(set(catalogue_keys)) != len(catalogue_keys):
        raise ValueError("JEP_AUTHORITY_CATALOGUE_DRIFT")
    if len({jep for _, jep in catalogue_keys}) != len(catalogue_keys):
        raise ValueError("JEP_ID_REUSED_ACROSS_RELEASES")
    if len(seed_rows) != len({row["jbs"] for row in seed_rows}):
        raise ValueError("DUPLICATE_JBS_SEED")
    for seed in seed_rows:
        if (int(seed["release"]) not in expected or not ISSUE.fullmatch(seed["jbs"])
                or seed.get("upstream_commit", "") and
                not SHA.fullmatch(seed["upstream_commit"])):
            raise ValueError("INVALID_JBS_SEED")

    if len(queue_rows) != sum(expected.values()):
        raise ValueError("INCOMPLETE_UPSTREAM_COMMIT_DENOMINATOR")
    commits: set[str] = set()
    counts: Counter[int] = Counter()
    issues: set[str] = set()
    unlinked = 0
    for index, row in enumerate(queue_rows):
        release = int(row["release"])
        commit = row["commit"]
        if (int(row["order"]) != index or release not in expected
                or not SHA.fullmatch(commit) or commit in commits):
            raise ValueError("DUPLICATE_OR_INVALID_JBS_COMMIT_ORDER")
        if row["compatibility_state"] != PENDING:
            raise ValueError("UNPROVEN_JBS_PROMOTED")
        ids = row["jbs_ids"].split(",") if row["jbs_ids"] else []
        if len(ids) != len(set(ids)) or any(not ISSUE.fullmatch(item) for item in ids):
            raise ValueError("INVALID_JBS_SUBJECT_CANDIDATE")
        commits.add(commit)
        counts[release] += 1
        issues.update(ids)
        unlinked += not bool(ids)
    if dict(counts) != expected:
        raise ValueError("UPSTREAM_RELEASE_COMMIT_COUNT_DRIFT")
    issue_rows = jbs_issue_index(queue_rows)
    if len(issue_rows) != len(issues):
        raise ValueError("JBS_SUBJECT_CANDIDATE_IDENTITY_DRIFT")

    items: list[dict[str, str]] = []
    for release, jsr, _major, _count in platform:
        items.append(dict(kind="JSR", release=str(release), authority_id=f"JSR-{jsr}",
                          upstream_commit="", disposition="SPECIFICATION_CROSSWALK",
                          proof_state="NOT_IMPLEMENTATION_EVIDENCE"))
    for row in catalogue:
        items.append(dict(kind="JEP", release=row["release"],
                          authority_id=f"JEP-{row['jep']}", upstream_commit="",
                          disposition=row["disposition"],
                          proof_state="NOT_IMPLEMENTATION_EVIDENCE"))
    for row in queue_rows:
        items.append(dict(kind="JBS_COMMIT", release=row["release"],
                          authority_id=row["jbs_ids"], upstream_commit=row["commit"],
                          disposition=row["inventory_disposition"], proof_state=PENDING))
    rendered = render_tsv(items)
    summary: dict[str, object] = {
        "schema": "M3_JSR_JEP_JBS_FULL_COMMIT_COVERAGE_V1",
        "java21_specification": "JSR-396",
        "java21_classfile_major": 65,
        "platform_jsr_count": len(platform),
        "released_jep_count": len(catalogue),
        "upstream_commit_count": len(queue_rows),
        "upstream_commit_counts_by_release": {str(k): counts[k] for k in sorted(expected)},
        "subject_candidate_distinct_jbs_ids": len(issues),
        "jbs_subject_issue_index_rows": len(issue_rows),
        "jbs_subject_issue_index_sha256": hashlib.sha256(
            render_jbs_index_tsv(issue_rows).encode("utf-8")
        ).hexdigest(),
        "unlinked_upstream_commits": unlinked,
        "seed_issue_count": len(seed_rows),
        "seed_ids_not_in_commit_subjects": sorted({s["jbs"] for s in seed_rows} - issues),
        "coverage_tsv_sha256": hashlib.sha256(rendered.encode("utf-8")).hexdigest(),
        "jsr_platforms_implemented": False,
        "all_jeps_implemented": False,
        "all_jbs_fixes_implemented": False,
        "java21_compatibility_proven": False,
        "promotion_authorized": False,
        "queue_recomputed_from_inventory": False,
        "upstream_git_history_authenticated": False,
    }
    return items, summary


def render_tsv(items: Sequence[Mapping[str, str]]) -> str:
    output = io.StringIO(newline="")
    writer = csv.DictWriter(output, fieldnames=COVERAGE_COLUMNS, delimiter="\t", lineterminator="\n")
    writer.writeheader()
    writer.writerows(items)
    return output.getvalue()


def work(root: Path, queue: Path, inventory: Path, upstream_repo: Path) -> tuple[list[dict[str, str]], dict[str, object], list[dict[str, str]]]:
    root = root.resolve(strict=True)
    authorities = release_jep_authority.verify_repository_authority(root)
    area = root / "make" / "m3" / "backports"
    catalogue = jep_source_witness.read_catalogue(area / "JEP_CATALOGUE.tsv")
    seeds = load_tsv(area / "UPSTREAM_CHANGE_SEEDS.tsv", (
        "release", "jbs", "title", "component", "disposition", "reason", "upstream_commit"))
    # Both inputs must be generated under the existing build-output fence.
    queue_path = jep_source_witness.checked_output_path(root, queue)
    inventory_path = jep_source_witness.checked_output_path(root, inventory)
    if queue_path == inventory_path:
        raise ValueError("INVENTORY_QUEUE_PATH_COLLISION")
    queue_rows = load_tsv(queue_path, QUEUE_COLUMNS, large=True)
    inventory_rows = load_tsv(inventory_path, INVENTORY_COLUMNS, large=True)
    verify_inventory_queue(inventory_rows, queue_rows)
    verified_commits = verify_actual_git_history(upstream_repo, inventory_rows)
    if verified_commits != len(queue_rows):
        raise ValueError("UPSTREAM_HISTORY_QUEUE_DENOMINATOR_DRIFT")
    records, summary = join(authorities, catalogue, seeds, queue_rows)
    summary["queue_recomputed_from_inventory"] = True
    summary["upstream_git_history_authenticated"] = True
    summary["jbs_subject_parser_state"] = (
        "NUMERIC_PARSER_PRESENT_REQUIRES_INDEX_GATE"
        if callable(getattr(upstream_inventory, "_subject_jbs_ids", None))
        else "EXPLICIT_JDK_ONLY_PARTIAL"
    )
    return records, summary, jbs_issue_index(queue_rows)


def ensure_distinct_output_paths(
    sources: Sequence[Path], targets: Sequence[Path],
) -> None:
    """Fail closed before replay if an output could overwrite input evidence."""
    if len(set(targets)) != len(targets):
        raise ValueError("OUTPUT_PATH_COLLISION")
    if set(sources).intersection(targets):
        raise ValueError("INPUT_OUTPUT_PATH_COLLISION")


def main(argv: Sequence[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True, type=Path)
    parser.add_argument("--queue", required=True, type=Path,
                        help="Complete generated compatibility queue, not the 13 seed issues")
    parser.add_argument("--inventory", required=True, type=Path,
                        help="Complete UPSTREAM_CHANGES.tsv from the same GA-tag run")
    parser.add_argument("--upstream-repo", required=True, type=Path,
                        help="Full local openjdk/jdk checkout with all seven pinned GA tags")
    parser.add_argument("--tsv-out", required=True, type=Path)
    parser.add_argument("--json-out", required=True, type=Path)
    parser.add_argument("--jbs-index-out", type=Path,
                        help="Optional separate one-row-per-JBS-subject-candidate TSV")
    args = parser.parse_args(argv)
    if args.jbs_index_out is not None:
        require_complete_jbs_issue_parser()
    root = args.root.resolve(strict=True)
    # Reject dangerous aliases *before* the expensive 14,948-commit Git replay.
    # Reuse the incumbent path-fence for both input and output identities.
    source_paths = (
        jep_source_witness.checked_output_path(root, args.queue),
        jep_source_witness.checked_output_path(root, args.inventory),
    )
    output_paths = [
        jep_source_witness.checked_output_path(root, args.tsv_out),
        jep_source_witness.checked_output_path(root, args.json_out),
    ]
    if args.jbs_index_out is not None:
        output_paths.append(
            jep_source_witness.checked_output_path(root, args.jbs_index_out)
        )
    ensure_distinct_output_paths(source_paths, output_paths)

    items, report, issue_rows = work(
        root, args.queue, args.inventory, args.upstream_repo
    )
    destinations = [
        (output_paths[0], render_tsv(items)),
        (output_paths[1], json.dumps(report, indent=2, sort_keys=True) + "\n"),
    ]
    if args.jbs_index_out is not None:
        destinations.append((output_paths[2], render_jbs_index_tsv(issue_rows)))
    for path, _content in destinations:
        path.parent.mkdir(parents=True, exist_ok=True)
    for path, content in destinations:
        path.write_text(content, encoding="utf-8", newline="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
