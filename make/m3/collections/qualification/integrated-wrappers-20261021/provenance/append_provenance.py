#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0

"""Prepare an append-only candidate ledger from an explicit, content-bound row plan.

This does not replace the unchanged provenance CLI against committed base...head.
It never writes the canonical or original global ledger.
"""

from __future__ import annotations

import argparse
import csv
import hashlib
import io
import json
import sys
import types
from pathlib import Path, PurePosixPath


COLUMNS = (
    "path", "classification", "source_url", "source_revision", "source_path",
    "license_id", "notice_path", "disposition", "evidence",
)
PLAN_SCHEMA = "m3.integrated.collections.provenance.row-plan.v1"


def require(condition: bool, message: str) -> None:
    if not condition:
        raise ValueError(message)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def git_blob(data: bytes) -> str:
    return hashlib.sha1(b"blob " + str(len(data)).encode("ascii") + b"\0" + data).hexdigest()


def relative_path(value: str) -> str:
    require(isinstance(value, str) and bool(value), "missing repository-relative path")
    pure = PurePosixPath(value)
    require(not pure.is_absolute() and ".." not in pure.parts and "\\" not in value,
            f"unsafe repository path: {value!r}")
    require(pure.as_posix() == value and value != ".", f"non-canonical path: {value!r}")
    return value


def within(root: Path, relative: str) -> Path:
    path = (root / relative_path(relative)).resolve()
    require(path != root and root in path.parents, f"path escapes root: {relative}")
    return path


def local_argument(root: Path, argument: str) -> Path:
    path = Path(argument)
    return (path if path.is_absolute() else root / path).resolve()


def load_reference_checker(path: Path):
    # Compile the verified source unchanged; avoid writing __pycache__ into pinned inputs.
    name = "m3_integrated_collections_unchanged_provenance_reference"
    module = types.ModuleType(name)
    module.__file__ = str(path)
    sys.modules[name] = module
    exec(compile(path.read_bytes(), str(path), "exec"), module.__dict__)
    return module


def tsv(rows: list[dict[str, str]], header: bool) -> bytes:
    stream = io.StringIO(newline="")
    writer = csv.DictWriter(stream, fieldnames=COLUMNS, delimiter="\t", lineterminator="\n")
    if header:
        writer.writeheader()
    for row in rows:
        require(set(row) == set(COLUMNS), f"invalid row columns for {row.get('path')}")
        require(all(isinstance(value, str) and not any(c in value for c in "\t\r\n")
                    for value in row.values()), f"non-scalar or multiline provenance row: {row['path']}")
        writer.writerow(row)
    return stream.getvalue().encode("utf-8")


def prepare(args: argparse.Namespace) -> dict:
    root = Path(args.task_root).resolve()
    canonical = (root / "canonical").resolve()
    input_syn = (root / "input/syn").resolve()
    plan_path = local_argument(root, args.plan)
    delta_path = local_argument(root, args.delta)
    output = local_argument(root, args.output_dir)
    allowed_output = (root / "design/provenance").resolve()
    require(output != allowed_output and allowed_output in output.parents,
            "output must be a fresh subdirectory of task-root/design/provenance")
    require(not output.exists(), f"occupied output directory: {output}")
    require(canonical.is_dir() and input_syn.is_dir(), "missing canonical or input/syn root")

    plan_bytes = plan_path.read_bytes()
    delta_bytes = delta_path.read_bytes()
    plan = json.loads(plan_bytes)
    delta = json.loads(delta_bytes)
    require(plan.get("schema") == PLAN_SCHEMA, "unsupported row-plan schema")
    require(isinstance(delta, list), "delta must use existing FILE_MANIFEST.json list format")
    require(plan.get("publication_branch") == "aix/integrated-collections-refinement-20261021",
            "publication branch drift requires an explicit reviewed plan update")

    original_path = within(input_syn, plan["original_ledger_path"])
    policy_path = within(input_syn, plan["policy_path"])
    checker_path = within(input_syn, plan["checker_path"])
    original = original_path.read_bytes()
    require(original.endswith(b"\n"), "original ledger lacks terminal LF; refuse byte rewrite")
    for path, expected in (
        (original_path, plan["original_ledger_sha256"]),
        (policy_path, plan["policy_sha256"]),
        (checker_path, plan["checker_sha256"]),
    ):
        require(sha256(path.read_bytes()) == expected, f"pinned input drift: {path}")

    gate = load_reference_checker(checker_path)
    policy = gate.load_policy(policy_path)
    existing = gate.load_ledger(original_path, set(policy["allowed_classifications"]))
    entries = {}
    for entry in plan["required_entries"]:
        path = relative_path(entry["path"])
        require(path not in entries, f"duplicate planned path: {path}")
        require(entry["row"]["path"] == path, f"row path mismatch: {path}")
        require(entry["expected_status"] in {"A", "M"}, f"unreviewed plan action: {path}")
        require(entry["content_binding"] in {"EXACT_REVIEWED_CONTENT", "FINAL_DELTA_AUTHORED_METADATA"},
                f"unknown content binding: {path}")
        require("target" not in PurePosixPath(path).parts, f"build output planned: {path}")
        entries[path] = entry

    excluded = {row["path"]: row for row in plan["excluded_unchanged_owners"]}
    for path, exclusion in excluded.items():
        current = within(canonical, path).read_bytes()
        require(sha256(current) == exclusion["sha256"], f"unchanged engine drift: {path}")
        require(current == within(input_syn, path).read_bytes(), f"engine differs from pinned input: {path}")

    actual = {}
    for item in delta:
        require(isinstance(item, dict), "non-object delta entry")
        repo = item.get("repo")
        require(repo in {"syn", "m3"}, f"unknown delta repository: {repo!r}")
        if repo != "syn":
            continue
        path = relative_path(item.get("path"))
        require(path != plan["original_ledger_path"],
                "supply pre-ledger delta; the candidate global ledger is this builder's output")
        require(path not in actual, f"duplicate delta path: {path}")
        require(path not in excluded, f"unchanged shared engine must not enter delta: {path}")
        require("target" not in PurePosixPath(path).parts, f"Maven build output in delta: {path}")
        require(path in entries, f"unplanned path requires an explicit reviewed provenance row: {path}")
        actual[path] = item
    require(set(actual) == set(entries),
            "missing planned delta paths: " + ", ".join(sorted(set(entries) - set(actual))))

    append_rows = []
    reuse_paths = []
    final_rows = dict(existing)
    content_receipts = []
    for path in sorted(actual):
        item, entry = actual[path], entries[path]
        current_path = within(canonical, path)
        current = current_path.read_bytes()
        current_hash = sha256(current)
        require(current_hash == item.get("sha256"), f"manifest content drift: {path}")
        if "bytes" in item:
            require(item["bytes"] == len(current), f"manifest byte count drift: {path}")
        if "git_blob" in item:
            require(item["git_blob"] == git_blob(current), f"manifest Git blob drift: {path}")
        if "source" in item:
            source = local_argument(root, item["source"])
            require(source == root or root in source.parents, f"manifest source outside task: {path}")
            require(source.read_bytes() == current, f"manifest source differs from canonical file: {path}")
        status = entry["expected_status"]
        if "status" in item:
            require(item["status"] == status, f"unexpected delta action: {path}")
        if status == "A":
            require(item.get("before_sha256") is None, f"added path has an occupied preimage: {path}")
            require(not within(input_syn, path).exists(), f"added path exists in pinned input: {path}")
            require(path not in existing, f"added path already has a ledger row: {path}")
        else:
            require(entry.get("reuse_existing_row") is True, f"unplanned existing-file modification: {path}")
            before = within(input_syn, path).read_bytes()
            require(sha256(before) == entry.get("before_sha256") == item.get("before_sha256"),
                    f"modified preimage drift: {path}")
            require(current.startswith(before), f"registry original-byte prefix changed: {path}")

        row = dict(entry["row"])
        if entry["content_binding"] == "EXACT_REVIEWED_CONTENT":
            require(current_hash == entry.get("expected_sha256"), f"reviewed source/template drift: {path}")
        else:
            require(row["classification"] in {"FIRST_PARTY", "FIRST_PARTY_NO_HEADER"},
                    f"payload cannot use mutable metadata binding: {path}")
            require(row["classification"] == "FIRST_PARTY_NO_HEADER" or path.endswith(".md"),
                    f"only authored prose or opaque metadata may use final-delta binding: {path}")
            row["source_revision"] = "sha256:" + current_hash
            row["evidence"] = row["evidence"].replace(
                "sha256=" + entry["observed_sha256"], "sha256=" + current_hash, 1)
        if entry.get("reuse_existing_row"):
            require(existing.get(path) == row, f"existing row would be rewritten: {path}")
            reuse_paths.append(path)
        else:
            require(path not in final_rows, f"duplicate final ledger path: {path}")
            append_rows.append(row)
            final_rows[path] = row
        violations = gate.validate_ledger_row(path, row, row["classification"], policy)
        require(not violations, f"unchanged checker rejected row for {path}: {violations}")
        content_receipts.append({
            "path": path, "status": status, "sha256": current_hash,
            "git_blob": git_blob(current), "bytes": len(current),
            "classification": row["classification"], "disposition": row["disposition"],
            "row_action": "REUSE_EXISTING_EXACT" if entry.get("reuse_existing_row") else "APPEND",
        })

    violations = []
    for path in sorted(actual):
        change = gate.Change(entries[path]["expected_status"], path)
        violations.extend(gate.validate_change(change, canonical, policy, final_rows))
    require(not violations, "unchanged checker preparation rejected files: " + repr(violations))

    appended = tsv(append_rows, header=False)
    candidate = original + appended
    append_file = tsv(append_rows, header=True)
    receipt = {
        "schema": "m3.integrated.collections.provenance.append-receipt.v1",
        "status": "PREPARED_AND_UNCHANGED_CHECKER_FUNCTIONS_VALIDATED_NOT_COMMITTED_CLI_GATE",
        "syn_base_revision": plan["syn_base_revision"],
        "publication_branch": plan["publication_branch"],
        "plan_sha256": sha256(plan_bytes), "delta_manifest_sha256": sha256(delta_bytes),
        "checker_sha256": plan["checker_sha256"], "policy_sha256": plan["policy_sha256"],
        "original_ledger_sha256": sha256(original),
        "candidate_ledger_sha256": sha256(candidate), "candidate_ledger_git_blob": git_blob(candidate),
        "preserved_original_prefix_bytes": len(original), "appended_bytes": len(appended),
        "appended_rows": len(append_rows), "reused_existing_rows": reuse_paths,
        "append_tsv_sha256": sha256(append_file), "preparation_violations": [],
        "files": content_receipts,
        "remaining_gate": "Run unchanged checker CLI against the actual committed Syn base...head including this candidate ledger, then all applicable repository admission gates.",
    }
    # All source/policy/delta validation is complete before any output is created.
    output.mkdir(parents=False)
    (output / "M3_FILE_PROVENANCE.tsv").write_bytes(candidate)
    (output / "PROVENANCE_APPEND.tsv").write_bytes(append_file)
    (output / "RECEIPT.json").write_text(json.dumps(receipt, indent=2) + "\n", encoding="utf-8")
    require(original_path.read_bytes() == original, "original ledger changed concurrently")
    return {"output_dir": str(output), "candidate_ledger_sha256": receipt["candidate_ledger_sha256"],
            "appended_rows": len(append_rows), "reused_rows": len(reuse_paths),
            "status": receipt["status"]}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--task-root", default=".")
    parser.add_argument("--plan", default="design/provenance/ROW_PLAN.json")
    parser.add_argument("--delta", required=True)
    parser.add_argument("--output-dir", required=True)
    args = parser.parse_args()
    try:
        print(json.dumps(prepare(args), indent=2))
        return 0
    except (ValueError, KeyError, TypeError, OSError, json.JSONDecodeError) as exc:
        print(json.dumps({"status": "REFUSED", "error": str(exc)}), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
