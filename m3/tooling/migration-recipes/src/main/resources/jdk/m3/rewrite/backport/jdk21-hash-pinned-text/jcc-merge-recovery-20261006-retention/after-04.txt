# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Exact Git-tree retention audit for explicitly retained source-sealed capabilities."""
from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import sys

from git_graph import GitGraph, exact_id


def safe_path(value: str) -> str:
    if (
        not isinstance(value, str)
        or not value
        or value.startswith("/")
        or "\\" in value
        or "\0" in value
        or any(part in {"", ".", "..", ".git"} for part in value.split("/"))
    ):
        raise ValueError("noncanonical repository-relative path")
    return value


def tsv(data: bytes) -> list[dict[str, str]]:
    text = data.decode("utf-8")
    reader = csv.DictReader(io.StringIO(text), delimiter="\t")
    if reader.fieldnames is None:
        raise ValueError("TSV header required")
    return list(reader)


def blob(git: GitGraph, commit: str, path: str) -> bytes | None:
    encoded = safe_path(path).encode("utf-8")
    entry = git.entries(commit, {encoded}).get(encoded)
    if entry is None:
        return None
    mode, kind, oid = entry
    if kind != "blob" or mode == "160000":
        raise ValueError("retention target is not a regular blob: " + path)
    return git.run("cat-file", "blob", exact_id(oid))


def load_capabilities(git: GitGraph, commit: str, path: str) -> list[tuple[str, str]]:
    data = blob(git, commit, path)
    if data is None:
        raise ValueError("retained capability catalogue missing")
    rows = tsv(data)
    if rows and set(rows[0]) != {"capability_id", "manifest_path"}:
        raise ValueError("invalid retained capability catalogue header")
    result: list[tuple[str, str]] = []
    seen: set[str] = set()
    for row in rows:
        capability = row["capability_id"].strip()
        manifest = safe_path(row["manifest_path"].strip())
        if not capability or capability in seen:
            raise ValueError("duplicate/blank retained capability id")
        seen.add(capability)
        result.append((capability, manifest))
    if not result:
        raise ValueError("retained capability catalogue is empty")
    return result


def load_manifest(git: GitGraph, commit: str, path: str) -> list[tuple[str, str]]:
    data = blob(git, commit, path)
    if data is None:
        raise ValueError("retained manifest missing: " + path)
    rows: list[tuple[str, str]] = []
    previous = ""
    for raw in data.decode("utf-8").splitlines():
        if not raw or raw.startswith("#"):
            continue
        cells = raw.split("\t")
        if len(cells) != 4:
            raise ValueError("invalid source-sealed manifest row: " + path)
        target = safe_path(cells[0])
        expected = cells[2]
        if previous >= target or not expected.isascii() or not expected.islower() or len(expected) != 64:
            raise ValueError("invalid/sorted source-sealed manifest: " + path)
        try:
            bytes.fromhex(expected)
        except ValueError as failure:
            raise ValueError("invalid postimage SHA-256: " + target) from failure
        previous = target
        rows.append((target, expected))
    if not rows:
        raise ValueError("empty source-sealed manifest: " + path)
    return rows


def strict_path(value: str) -> str:
    """Canonical text paths for the opt-in declaration contract."""
    result = safe_path(value)
    if result != result.strip() or any(ord(char) < 32 or ord(char) == 127 for char in result):
        raise ValueError("noncanonical repository-relative path")
    if len(result) > 1 and result[0].isascii() and result[0].isalpha() and result[1] == ":":
        raise ValueError("noncanonical repository-relative path")
    result.encode("utf-8")
    return result


def sealed_blob(git: GitGraph, commit: str, path: str) -> bytes | None:
    encoded = strict_path(path).encode("utf-8")
    entry = git.entries(commit, {encoded}).get(encoded)
    if entry is None:
        return None
    mode, kind, oid = entry
    if kind != "blob" or mode not in {"100644", "100755"}:
        raise ValueError("retention declaration is not a regular blob: " + path)
    return git.run("cat-file", "blob", exact_id(oid))


def declaration_snapshot(
    git: GitGraph, commit: str, catalogue: str, *, current: bool, allow_absent: bool
) -> dict:
    """Read a catalogue and every manifest from their own exact Git commit."""
    data = sealed_blob(git, commit, catalogue)
    if data is None:
        if not allow_absent:
            raise ValueError("baseline retained capability catalogue missing: " + commit)
        return {"catalogue_state": "ABSENT", "catalogue_sha256": "ABSENT", "capabilities": {}}
    try:
        table = list(csv.reader(io.StringIO(data.decode("utf-8")), delimiter="\t", strict=True))
    except csv.Error as failure:
        raise ValueError("invalid retained capability catalogue TSV: " + commit) from failure
    if not table or table[0] != ["capability_id", "manifest_path"]:
        raise ValueError("invalid retained capability catalogue header: " + commit)
    capabilities = {}
    for cells in table[1:]:
        if len(cells) != 2:
            raise ValueError("invalid retained capability catalogue row: " + commit)
        capability, manifest = cells
        if (not capability or capability != capability.strip() or capability in capabilities
                or any(ord(char) < 32 or ord(char) == 127 for char in capability)):
            raise ValueError("duplicate/noncanonical retained capability id: " + commit)
        manifest = strict_path(manifest)
        manifest_data = sealed_blob(git, commit, manifest)
        if manifest_data is None and not current:
            raise ValueError("baseline retained manifest missing: " + commit + ":" + manifest)
        targets = {}
        previous = ""
        if manifest_data is not None:
            for raw in manifest_data.decode("utf-8").splitlines():
                if not raw or raw.startswith("#"):
                    continue
                row = raw.split("\t")
                if len(row) != 4:
                    raise ValueError("invalid source-sealed manifest row: " + manifest)
                target = strict_path(row[0])
                expected = row[2]
                if (target <= previous or len(expected) != 64
                        or any(char not in "0123456789abcdef" for char in expected)):
                    raise ValueError("invalid/sorted source-sealed manifest: " + manifest)
                previous = target
                targets[target] = expected
            if not targets and not current:
                raise ValueError("empty baseline source-sealed manifest: " + commit + ":" + manifest)
        capabilities[capability] = {
            "manifest_path": manifest,
            "manifest_state": "ABSENT" if manifest_data is None else "PRESENT" if targets else "EMPTY",
            "manifest_sha256": "ABSENT" if manifest_data is None else hashlib.sha256(manifest_data).hexdigest(),
            "targets": targets,
        }
    if not capabilities and not current:
        raise ValueError("baseline retained capability catalogue is empty: " + commit)
    return {
        "catalogue_state": "PRESENT" if capabilities else "EMPTY",
        "catalogue_sha256": hashlib.sha256(data).hexdigest(),
        "capabilities": capabilities,
    }


def inspect_baselines(
    git: GitGraph, commit: str, catalogue: str, baseline_commits: tuple[str, ...], include_parents: bool
) -> dict:
    catalogue = strict_path(catalogue)
    explicit = {git.commit_id(value) for value in baseline_commits}
    if len(explicit) != len(baseline_commits):
        raise ValueError("duplicate explicit retention baseline")
    parents = set()
    if include_parents:
        parents = {
            git.commit_id(value)
            for value in git.run("show", "-s", "--format=%P", commit).decode("ascii").split()
        }
    baselines = {}
    for baseline in sorted(explicit | parents):
        baselines[baseline] = declaration_snapshot(
            git, baseline, catalogue, current=False, allow_absent=baseline not in explicit
        )
    current = declaration_snapshot(git, commit, catalogue, current=True, allow_absent=True)
    conflicts = []
    if current["catalogue_state"] != "PRESENT":
        conflicts.append({"kind": "CURRENT_CATALOGUE_" + current["catalogue_state"], "path": catalogue})
    current_capabilities = current["capabilities"]
    for capability, declaration in sorted(current_capabilities.items()):
        if declaration["manifest_state"] != "PRESENT":
            conflicts.append({
                "kind": "CURRENT_MANIFEST_" + declaration["manifest_state"],
                "capability_id": capability, "manifest_path": declaration["manifest_path"],
            })
    for baseline, snapshot in sorted(baselines.items()):
        for capability, obligation in sorted(snapshot["capabilities"].items()):
            declaration = current_capabilities.get(capability)
            source = {"baseline_commit": baseline, "capability_id": capability,
                      "manifest_path": obligation["manifest_path"]}
            if declaration is None:
                conflicts.append(dict(source, kind="CAPABILITY_REMOVED"))
                continue
            if declaration["manifest_path"] != obligation["manifest_path"]:
                conflicts.append(dict(source, kind="MANIFEST_PATH_CHANGED",
                                      current_manifest_path=declaration["manifest_path"]))
            for target, expected in sorted(obligation["targets"].items()):
                actual_declaration = declaration["targets"].get(target)
                if actual_declaration is None:
                    conflicts.append(dict(source, kind="TARGET_DECLARATION_REMOVED", path=target,
                                          expected_sha256=expected))
                elif actual_declaration != expected:
                    conflicts.append(dict(source, kind="EXPECTED_SHA256_CHANGED", path=target,
                                          expected_sha256=expected, current_expected_sha256=actual_declaration))

    # Preserve every distinct obligation, including incompatible hashes. No baseline wins.
    obligations = {}
    baseline_manifests = {}
    for origin, snapshot in [(commit, current), *sorted(baselines.items())]:
        for capability, declaration in sorted(snapshot["capabilities"].items()):
            manifest = declaration["manifest_path"]
            if origin in baselines:
                baseline_manifests.setdefault(capability, {}).setdefault(manifest, set()).add(origin)
            for target, expected in sorted(declaration["targets"].items()):
                obligations.setdefault((capability, manifest, target, expected), set()).add(origin)
    for capability, manifests in sorted(baseline_manifests.items()):
        if len(manifests) > 1:
            conflicts.append({
                "kind": "BASELINE_MANIFEST_CONFLICT", "capability_id": capability,
                "declarations": [{"manifest_path": path, "commits": sorted(commits)}
                                 for path, commits in sorted(manifests.items())],
            })
    obligation_rows = [
        {"capability_id": capability, "manifest_path": manifest, "path": path,
         "expected_sha256": expected, "declared_by": sorted(origins)}
        for (capability, manifest, path, expected), origins in sorted(obligations.items())
    ]
    by_path = {}
    for obligation in obligation_rows:
        by_path.setdefault(obligation["path"], []).append(obligation)
    for path, declarations in sorted(by_path.items()):
        if len({declaration["expected_sha256"] for declaration in declarations}) > 1:
            conflicts.append({"kind": "EXPECTED_SHA256_CONFLICT", "path": path,
                              "declarations": declarations})

    # One target-tree read, and one body hash per Git blob, across all baseline origins.
    entries = git.entries(commit, {path.encode("utf-8") for path in by_path})
    actual_hashes = {}
    for path in sorted(by_path):
        entry = entries.get(path.encode("utf-8"))
        if entry is None:
            actual_hashes[path] = "ABSENT"
        else:
            mode, kind, oid = entry
            if kind != "blob" or mode not in {"100644", "100755"}:
                raise ValueError("retention target is not a regular blob: " + path)
            actual_hashes[path] = git.blob_digest(exact_id(oid), mode)[0]
    rows = []
    for capability, manifest, path, expected in sorted(obligations, key=lambda key: (key[0], key[2], key[1], key[3])):
        actual = actual_hashes[path]
        state = "MISSING" if actual == "ABSENT" else "RETAINED_EXACT" if actual == expected else "PRESENT_DRIFTED_REVIEW"
        rows.append({"capability_id": capability, "manifest_path": manifest, "path": path,
                     "expected_sha256": expected, "actual_sha256": actual, "state": state})
    if git.refs() != git.initial_refs:
        raise ValueError("refs moved during retention audit")
    conflicts.sort(key=lambda value: json.dumps(value, sort_keys=True, separators=(",", ":")))
    summary = {
        "schema": "m3-retained-capability-baselines/1", "commit": commit, "catalogue": catalogue,
        "include_parents": include_parents, "explicit_baseline_commits": sorted(explicit),
        "direct_parent_commits": sorted(parents),
        "baselines": [
            {"commit": baseline, "explicit": baseline in explicit, "direct_parent": baseline in parents,
             "catalogue_state": "ABSENT_IMPLICIT_PARENT" if snapshot["catalogue_state"] == "ABSENT"
                                else snapshot["catalogue_state"],
             "catalogue_sha256": snapshot["catalogue_sha256"],
             "capabilities": len(snapshot["capabilities"]),
             "targets": sum(len(item["targets"]) for item in snapshot["capabilities"].values())}
            for baseline, snapshot in sorted(baselines.items())
        ],
        "current_catalogue_state": current["catalogue_state"],
        "current_catalogue_sha256": current["catalogue_sha256"],
        "current_capabilities": len(current_capabilities),
        "capabilities": len(set(current_capabilities).union(
            *(set(snapshot["capabilities"]) for snapshot in baselines.values())
        )), "targets": len(rows),
        "distinct_target_paths": len(by_path),
        "retained_exact": sum(row["state"] == "RETAINED_EXACT" for row in rows),
        "present_drifted_review": sum(row["state"] == "PRESENT_DRIFTED_REVIEW" for row in rows),
        "missing": sum(row["state"] == "MISSING" for row in rows), "rows": rows,
        "obligations": obligation_rows, "declaration_conflicts": conflicts,
        "declaration_conflict_count": len(conflicts),
    }
    summary["root"] = hashlib.sha256(
        json.dumps(summary, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()
    return summary


def inspect(
    repo: Path, commit: str, catalogue: str, *, baseline_commits: tuple[str, ...] = (), include_parents: bool = False
) -> dict:
    if not isinstance(baseline_commits, tuple) or not isinstance(include_parents, bool):
        raise ValueError("baseline_commits must be an immutable tuple and include_parents a bool")
    git = GitGraph(repo)
    checked_commit = git.commit_id(commit)
    if baseline_commits or include_parents:
        return inspect_baselines(git, checked_commit, catalogue, baseline_commits, include_parents)
    initial_refs = git.initial_refs
    rows = []
    capabilities = load_capabilities(git, checked_commit, catalogue)
    for capability, manifest in capabilities:
        for target, expected in load_manifest(git, checked_commit, manifest):
            content = blob(git, checked_commit, target)
            if content is None:
                actual = "ABSENT"
                state = "MISSING"
            else:
                actual = hashlib.sha256(content).hexdigest()
                state = "RETAINED_EXACT" if actual == expected else "PRESENT_DRIFTED_REVIEW"
            rows.append(
                {
                    "capability_id": capability,
                    "manifest_path": manifest,
                    "path": target,
                    "expected_sha256": expected,
                    "actual_sha256": actual,
                    "state": state,
                }
            )
    if git.refs() != initial_refs:
        raise ValueError("refs moved during retention audit")
    rows.sort(key=lambda row: (row["capability_id"], row["path"]))
    summary = {
        "commit": checked_commit,
        "capabilities": len(capabilities),
        "targets": len(rows),
        "retained_exact": sum(row["state"] == "RETAINED_EXACT" for row in rows),
        "present_drifted_review": sum(row["state"] == "PRESENT_DRIFTED_REVIEW" for row in rows),
        "missing": sum(row["state"] == "MISSING" for row in rows),
        "rows": rows,
    }
    root = hashlib.sha256(
        json.dumps(summary, sort_keys=True, separators=(",", ":")).encode("utf-8")
    ).hexdigest()
    summary["root"] = root
    return summary


def write(summary: dict, output: Path) -> None:
    if output.exists() or output.is_symlink():
        raise ValueError("retention output must not already exist")
    output.mkdir(parents=True, exist_ok=False)
    rows = summary["rows"]
    with (output / "RETENTION.tsv").open("x", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(
            stream,
            delimiter="\t",
            lineterminator="\n",
            fieldnames=[
                "capability_id",
                "manifest_path",
                "path",
                "expected_sha256",
                "actual_sha256",
                "state",
            ],
        )
        writer.writeheader()
        writer.writerows(rows)
    public_summary = {key: value for key, value in summary.items() if key != "rows"}
    (output / "SUMMARY.json").write_text(
        json.dumps(public_summary, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )
    (output / "STATE_ROOT.sha256").write_text(summary["root"] + "\n", encoding="ascii")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--commit", required=True)
    parser.add_argument(
        "--catalogue",
        default="m3/history/RETAINED_CAPABILITIES.tsv",
    )
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--require-no-missing", action="store_true")
    parser.add_argument("--baseline-commit", action="append", default=[],
                        help="immutable full Git commit ID; repeat for independent baseline obligations")
    parser.add_argument("--include-parents", action="store_true",
                        help="retain obligations from every direct parent of the checked commit")
    args = parser.parse_args()
    try:
        summary = inspect(args.repo, args.commit, args.catalogue,
                          baseline_commits=tuple(args.baseline_commit), include_parents=args.include_parents)
        write(summary, args.output)
        print(json.dumps({key: value for key, value in summary.items() if key != "rows"}, sort_keys=True))
        if args.require_no_missing and summary["missing"]:
            print("RETENTION_REFUSED: retained targets are missing", file=sys.stderr)
            return 3
        if args.require_no_missing and summary.get("declaration_conflict_count", 0):
            print("RETENTION_REFUSED: retained baseline declarations conflict", file=sys.stderr)
            return 3
        return 0
    except (ValueError, OSError, UnicodeError) as failure:
        print("RETENTION_REFUSED: " + str(failure), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
