# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""M3 PSE/CCPS recipe: deterministic observations; no source transformation."""
from __future__ import annotations

import argparse
from collections import defaultdict
import csv
import hashlib
import json
from pathlib import Path
import sys
import subprocess

from git_graph import GitGraph, exact_id, path_text, topological


class Artifacts:
    """Exclusive new output namespace; only an explicit final seal marks success."""

    def __init__(self, root: Path, budget: int):
        self.root, self.budget, self.used = root, budget, 0
        root.mkdir(parents=True, exist_ok=False)
        (root / "INCOMPLETE").write_text("No complete receipt has been published.\n")

    def put(self, name: str, value: bytes) -> None:
        if self.used + len(value) > self.budget:
            raise ValueError("artifact budget exceeded; output remains INCOMPLETE")
        target = self.root / name
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("xb") as stream:
            stream.write(value)
        self.used += len(value)

    def text(self, name: str, text: str) -> None:
        self.put(name, text.encode("utf-8"))

    def table(self, name: str, header, rows) -> None:
        import io
        buffer = io.StringIO(newline="")
        writer = csv.writer(buffer, delimiter="\t", lineterminator="\n")
        writer.writerow(header)
        writer.writerows(rows)
        self.text(name, buffer.getvalue())

    def seal(self) -> str:
        rows = []
        for path in sorted(self.root.rglob("*")):
            if path.is_file() and path.name != "INCOMPLETE":
                rows.append((path.relative_to(self.root).as_posix(), hashlib.sha256(path.read_bytes()).hexdigest()))
        self.table("OUTPUT_CONTRACT.tsv", ["path", "sha256"], rows)
        digest = hashlib.sha256((self.root / "OUTPUT_CONTRACT.tsv").read_bytes()).hexdigest()
        self.text("STATE_ROOT.sha256", digest + "  OUTPUT_CONTRACT.tsv\n")
        (self.root / "INCOMPLETE").unlink()
        return digest


def disposition(source, target) -> str:
    if source is None:
        return "TARGET_ONLY" if target else "ABSENT_BOTH"
    if target is None:
        return "ABSENT_AT_TARGET"
    return "EXACT_ENTRY_RETAINED" if source == target else "DIFFERENT_REQUIRES_REVIEW"


def inspect(repo: Path, snapshot: dict, output: Path, prs: dict | None = None) -> dict:
    """Produce review-only ledgers for the complete frozen, upstream-bounded DAG."""
    if snapshot.get("schema") != 1 or snapshot.get("workers") != 1:
        raise ValueError("schema 1 and a single worker are required")
    refs = snapshot["refs"]
    if not isinstance(refs, dict) or not refs or len(refs) > 1000:
        raise ValueError("invalid or unbounded ref snapshot")
    for key in ("max_commits", "max_command_bytes", "max_artifact_bytes"):
        if not isinstance(snapshot[key], int) or snapshot[key] <= 0:
            raise ValueError("positive resource budgets are required")
    g = GitGraph(repo, snapshot["max_command_bytes"])
    output = output.absolute()
    if output.exists() or output.is_symlink():
        raise ValueError("output must not already exist")
    resolved = output.resolve()
    if resolved.is_relative_to(g.root) or resolved.is_relative_to(g.common):
        raise ValueError("output must be outside the worktree and Git administration")
    if any(parent.is_symlink() for parent in (output, *output.parents)):
        raise ValueError("symlink output path refused")
    upstream, candidate, target = (g.commit_id(snapshot[k]) for k in ("upstream", "candidate", "target"))
    tips = sorted({upstream, candidate, target, *(g.commit_id(s) for s in refs.values())})
    if any(not g.ancestor(upstream, tip) for tip in tips):
        raise ValueError("tip outside the declared upstream history")
    if snapshot.get("target_tree") and g.metadata(target)[0] != exact_id(snapshot["target_tree"]):
        raise ValueError("target tree pin mismatch")
    parents = g.graph(upstream, tips, snapshot["max_commits"])
    ordered = topological(parents)
    artifacts = Artifacts(output, snapshot["max_artifact_bytes"])
    metadata = {s: g.metadata(s) for s in sorted(set(parents) | {p for ps in parents.values() for p in ps})}
    artifacts.text("SNAPSHOT.json", json.dumps(snapshot, indent=2, sort_keys=True) + "\n")
    rows = [(s, metadata[s][0], metadata[s][1], " ".join(parents[s]), metadata[s][2]) for s in ordered]
    header = ["commit", "tree", "committer_epoch", "parents", "subject_json"]
    artifacts.table("COMMITS_FORWARD.tsv", header, rows)
    artifacts.table("COMMITS_REVERSE.tsv", header, reversed(rows))
    edges, changes, merges, touched, blob_paths = [], [], [], set(), defaultdict(set)
    patch_hashes = set()
    for child in ordered:
        ps = parents[child]
        if len(ps) > 1:
            same = metadata[child][0] == metadata[ps[0]][0]
            different = [p for p in ps[1:] if metadata[p][0] != metadata[child][0]]
            merges.append((child, ps[0], len(ps), str(same).lower(), " ".join(different),
                           "REVIEW_FIRST_PARENT_TREE_REUSE" if same and different else "REVIEW_MERGE_CONTENT"))
        for ordinal, parent in enumerate(ps, 1):
            delta = g.changes(parent, child)
            patch = g.patch(parent, child) if delta else b""
            patch_hash = hashlib.sha256(patch).hexdigest()
            if patch_hash not in patch_hashes:
                artifacts.put("patches/" + patch_hash + ".patch", patch)
                patch_hashes.add(patch_hash)
            edges.append((parent, child, ordinal, len(delta), patch_hash))
            for oldmode, newmode, old, new, status, path in delta:
                touched.add(path)
                oldhash, _ = g.blob_digest(old, oldmode)
                newhash, _ = g.blob_digest(new, newmode)
                for oid, mode in ((old, oldmode), (new, newmode)):
                    if mode not in {"000000", "160000"}:
                        blob_paths[oid].add(path)
                changes.append((parent, child, ordinal, status, path_text(path), oldmode, newmode,
                                old, new, oldhash, newhash))
    artifacts.table("PARENT_EDGES.tsv", ["parent", "child", "parent_ordinal", "changed_paths", "patch_sha256"], edges)
    artifacts.table("CHANGES.tsv", ["parent", "child", "parent_ordinal", "status", "path_json",
                                    "old_mode", "new_mode", "old_oid", "new_oid", "old_sha256", "new_sha256"], changes)
    artifacts.table("MERGES.tsv", ["commit", "first_parent", "parent_count", "same_first_parent_tree",
                                   "different_side_trees", "disposition"], merges)
    before = g.entries(candidate, touched)
    after = g.entries(target, touched)
    original = g.entries(upstream, touched)
    retention, inventory, queue = [], [], []
    for path in sorted(touched):
        a, b = before.get(path), after.get(path)
        state = disposition(a, b)
        retention.append((path_text(path), a[2] if a else "ABSENT", b[2] if b else "ABSENT", state))
        digest, size = g.blob_digest(b[2], b[0]) if b else ("ABSENT", 0)
        inventory.append((path_text(path), path_text(path.rsplit(b"/", 1)[0]), "NOT_SEMANTICALLY_PARSED",
                          b[2] if b else "ABSENT", digest, size, "GIT_TREE_HAS_NO_FILE_MTIME"))
        if a and state != "EXACT_ENTRY_RETAINED":
            queue.append(("P0" if path.startswith(b"src/") else "P1", path_text(path), state,
                          "REVIEW_ONLY: confirm full contract and exact diff; never automatically restore"))
    artifacts.table("CANDIDATE_RETENTION.tsv", ["path_json", "candidate_oid", "target_oid", "disposition"], retention)
    artifacts.table("INVENTORY.tsv", ["path_json", "parent_path_json", "symbol_summary_status",
                                      "git_object_id", "sha256", "bytes", "mtime_status"], inventory)
    branch_rows = []
    entry_cache = {target: after, upstream: original, candidate: before}
    for name, tip in sorted(refs.items()):
        if tip not in entry_cache:
            entry_cache[tip] = g.entries(tip, touched)
        for path, entry in sorted(entry_cache[tip].items()):
            if entry != original.get(path):
                branch_rows.append((name, tip, path_text(path), entry[2],
                                    after[path][2] if path in after else "ABSENT",
                                    disposition(entry, after.get(path))))
    artifacts.table("BRANCH_RETENTION.tsv", ["ref", "tip", "path_json", "tip_oid", "target_oid", "disposition"], branch_rows)
    pr_rows = []
    if prs is not None:
        expected = set(snapshot.get("pr_numbers", []))
        found = [p["number"] for p in prs["pulls"]]
        if set(found) != expected or len(found) != len(expected) or prs["repository"] != snapshot["repository"]:
            raise ValueError("PR witness has missing, extra or duplicate records")
        for pr in sorted(prs["pulls"], key=lambda p: p["number"]):
            head = exact_id(pr["head_sha"])
            known = head in metadata or head in tips
            reachable = any(g.ancestor(head, tip) for tip in tips) if known else False
            pr_rows.append((pr["number"], pr["state"], pr["base_ref"], head,
                            pr.get("merge_commit_sha") or "NONE", pr.get("merged_at") or "NOT_MERGED",
                            "HEAD_IN_SNAPSHOT" if reachable else "HEAD_OUTSIDE_SNAPSHOT_REVIEW"))
        artifacts.text("PR_WITNESS.json", json.dumps(prs, indent=2, sort_keys=True) + "\n")
    artifacts.table("PULL_REQUESTS.tsv", ["number", "state", "base_ref", "head_sha", "merge_commit_sha",
                                          "merged_at", "snapshot_coverage"], pr_rows)
    exact_clusters = [(oid, g.cache[oid][0], path_text(path), "These files may be similar. Please confirm via hash or diff.")
                      for oid, paths in sorted(blob_paths.items()) if len(paths) > 1 for path in sorted(paths)]
    artifacts.table("CLUSTERS_EXACT.tsv", ["git_blob", "sha256", "historical_path_json", "proposal"], exact_clusters)
    artifacts.table("CLUSTERS_NORMALIZED.tsv", ["status", "reason"],
                    [("NOT_RUN", "No normalized or semantic equivalence recognizer was executed")])
    artifacts.table("ACTION_QUEUE.tsv", ["priority", "path_json", "signal", "action"], sorted(queue))
    summary = {"commits": len(ordered), "parent_edges": len(edges), "path_change_records": len(changes),
               "fork_touched_paths": len(touched), "frozen_refs": len(refs), "unique_tips": len(tips),
               "ancestry_only_merge_signals": sum(r[-1] == "REVIEW_FIRST_PARENT_TREE_REUSE" for r in merges),
               "candidate_paths_absent_at_target": sum(r[-1] == "ABSENT_AT_TARGET" for r in retention),
               "candidate_paths_different_at_target": sum(r[-1] == "DIFFERENT_REQUIRES_REVIEW" for r in retention),
               "candidate_is_target_ancestor": g.ancestor(candidate, target),
               "semantic_review": "NOT_COMPLETE", "jdk_build": "NOT_RUN", "jdk_tests": "NOT_RUN",
               "pr_metadata": "CAPTURED_SEPARATE_WITNESS" if prs else "NOT_COLLECTED",
               "pr_heads_outside_snapshot": sum(r[-1] != "HEAD_IN_SNAPSHOT" for r in pr_rows),
               "nested_gitlinks": "NOT_RECURSED", "normalization": "NOT_RUN"}
    if g.refs() != g.initial_refs:
        raise ValueError("repository refs moved during audit; output remains INCOMPLETE")
    artifacts.table("SUMMARY.tsv", ["metric", "value"], sorted(summary.items()))
    artifacts.table("STATUS.tsv", ["stage", "status"], [("frozen_graph_traversal", "COMPLETE"),
                    ("parent_patch_inventory", "COMPLETE"), ("semantic_review", "OPEN"), ("runtime_promotion", "FORBIDDEN")])
    artifacts.table("RUN_CONTEXT.tsv", ["key", "value"], [("mode", "dry-run"), ("workers", 1),
                    ("upstream", upstream), ("candidate", candidate), ("target", target),
                    ("git", g.run("--version").decode().strip()), ("python", sys.version.split()[0])])
    artifacts.table("VERIFY_CONTRACT.tsv", ["gate", "result"], [("snapshot_and_graph", "PASS"),
                    ("all_parent_diffs", "PASS"), ("refs_unchanged", "PASS"),
                    ("JDK_compile", "NOT_RUN"), ("JDK_tests", "NOT_RUN"), ("runtime", "NOT_RUN")])
    artifacts.table("PROVENANCE.tsv", ["source", "sha256"], [(p.name, hashlib.sha256(p.read_bytes()).hexdigest())
                    for p in sorted(Path(__file__).parent.glob("*.py"))])
    artifacts.text("COMMANDS.json", json.dumps(g.commands, indent=2) + "\n")
    artifacts.text("FINAL_REPORT.md", "# Frozen history audit\n\n" +
                   "```json\n" + json.dumps(summary, indent=2, sort_keys=True) + "\n```\n\n" +
                   "Both traversal directions and every parent edge were inventoried within the frozen upstream-bounded graph.\n"
                   "Source files, index and refs were not intentionally modified. No merge or restoration was performed.\n"
                   "Patch/blob identity is not semantic equivalence. Review obligations and historical failures remain open.\n"
                   "No JDK compilation, jtreg, JCK, runtime acceptance or application benchmark was executed by this audit.\n"
                   "PR metadata is a separately captured witness; only heads inside the frozen graph count as covered.\n"
                   "Submodule repositories and normalized/semantic clusters are outside this run.\n")
    summary["state_root"] = artifacts.seal()
    return summary


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--snapshot", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--prs", type=Path)
    args = parser.parse_args()
    try:
        snapshot = json.loads(args.snapshot.read_text(encoding="utf-8"))
        prs = json.loads(args.prs.read_text(encoding="utf-8")) if args.prs else None
        print(json.dumps(inspect(args.repo, snapshot, args.output, prs), sort_keys=True))
        return 0
    except (ValueError, KeyError, OSError, TypeError, subprocess.TimeoutExpired) as failure:
        print("AUDIT_REFUSED: " + str(failure), file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
