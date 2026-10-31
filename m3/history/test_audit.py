# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
"""Real local Git fixtures; never substitute these passes for JDK verification."""
from __future__ import annotations

import csv
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

from audit import disposition, inspect
from git_graph import GitGraph, exact_id, path_text, raw_changes, topological


class HistoryContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        self.repo = self.home / "source"
        self.repo.mkdir()
        self.git("init", "-b", "main")
        self.git("config", "user.name", "M3 Fixture")
        self.git("config", "user.email", "fixture@example.invalid")
        (self.repo / "original.txt").write_text("upstream\n")
        self.git("add", ".")
        self.git("commit", "-m", "upstream")
        self.upstream = self.git("rev-parse", "HEAD")
        self.git("checkout", "-b", "feature")
        (self.repo / "runtime.java").write_text("class RuntimeOwner {}\n")
        (self.repo / "evidence.json").write_text('{"status":"historical"}\n')
        self.odd = 'spaces tab\tand\nnewlines-λ.java'
        (self.repo / self.odd).write_text("same content\n")
        (self.repo / "duplicate.java").write_text("same content\n")
        (self.repo / "binary.dat").write_bytes(b"\0\xff\0\x81")
        self.git("add", ".")
        self.git("commit", "-m", "built candidate")
        self.candidate = self.git("rev-parse", "HEAD")
        self.git("checkout", "main")
        (self.repo / "newer.txt").write_text("new work\n")
        self.git("add", ".")
        self.git("commit", "-m", "current content")
        self.first = self.git("rev-parse", "HEAD")
        tree = self.git("rev-parse", "HEAD^{tree}")
        self.target = self.git("commit-tree", tree, "-p", self.first, "-p", self.candidate,
                               input=b"ancestry-only merge\n")
        self.git("update-ref", "refs/heads/main", self.target)
        self.git("reset", "--hard", self.target)  # Fixture setup only; reader has no writes.
        self.snapshot = {"schema": 1, "repository": "fixture/history", "upstream": self.upstream,
                         "candidate": self.candidate, "target": self.target, "target_tree": tree,
                         "refs": {"main": self.target, "feature": self.candidate}, "pr_numbers": [1],
                         "max_commits": 100, "max_command_bytes": 1048576,
                         "max_artifact_bytes": 10485760, "workers": 1}

    def git(self, *args, input=None) -> str:
        return subprocess.check_output(["git", "-C", str(self.repo), *args], input=input,
                                       stderr=subprocess.DEVNULL).decode().strip()

    def audit(self, name="report", snapshot=None, prs=None):
        path = self.home / name
        result = inspect(self.repo, snapshot or self.snapshot, path, prs)
        return path, result

    def rows(self, path, name):
        with (path / name).open(newline="") as stream:
            return list(csv.DictReader(stream, delimiter="\t"))

    def test_ancestry_is_not_content(self):
        path, result = self.audit()
        self.assertTrue(result["candidate_is_target_ancestor"])
        self.assertEqual(1, result["ancestry_only_merge_signals"])
        self.assertEqual(5, result["candidate_paths_absent_at_target"])
        self.assertEqual("NOT_RUN", result["jdk_build"])
        signals = self.rows(path, "MERGES.tsv")
        self.assertEqual("true", signals[0]["same_first_parent_tree"])
        self.assertIn(self.candidate, signals[0]["different_side_trees"])

    def test_all_parent_edges_and_forward_reverse(self):
        path, _ = self.audit()
        forward = self.rows(path, "COMMITS_FORWARD.tsv")
        reverse = self.rows(path, "COMMITS_REVERSE.tsv")
        self.assertEqual(forward, list(reversed(reverse)))
        edges = self.rows(path, "PARENT_EDGES.tsv")
        merge_edges = [e for e in edges if e["child"] == self.target]
        self.assertEqual(["1", "2"], [e["parent_ordinal"] for e in merge_edges])
        self.assertEqual("0", merge_edges[0]["changed_paths"])
        self.assertGreater(int(merge_edges[1]["changed_paths"]), 0)
        positions = {r["commit"]: i for i, r in enumerate(forward)}
        for e in edges:
            if e["parent"] in positions:
                self.assertLess(positions[e["parent"]], positions[e["child"]])

    def test_odd_paths_binary_patches_and_hashes(self):
        path, _ = self.audit()
        rows = self.rows(path, "CHANGES.tsv")
        self.assertTrue(any(json.loads(r["path_json"]) == self.odd for r in rows))
        self.assertTrue(any(b"GIT binary patch" in p.read_bytes() for p in (path / "patches").iterdir()))
        for p in (path / "patches").iterdir():
            self.assertEqual(p.stem, hashlib.sha256(p.read_bytes()).hexdigest())
        self.assertEqual(2, len(self.rows(path, "CLUSTERS_EXACT.tsv")))

    def test_repeat_is_deterministic_and_source_unchanged(self):
        refs = self.git("show-ref")
        index = (self.repo / ".git/index").read_bytes()
        files = {p.relative_to(self.repo): p.read_bytes() for p in self.repo.iterdir() if p.is_file()}
        a, first = self.audit("a")
        b, second = self.audit("b")
        self.assertEqual(first, second)
        self.assertEqual((a / "OUTPUT_CONTRACT.tsv").read_bytes(), (b / "OUTPUT_CONTRACT.tsv").read_bytes())
        self.assertEqual(refs, self.git("show-ref"))
        self.assertEqual(index, (self.repo / ".git/index").read_bytes())
        self.assertEqual(files, {p.relative_to(self.repo): p.read_bytes() for p in self.repo.iterdir() if p.is_file()})
        self.assertFalse((a / "INCOMPLETE").exists())
        for row in self.rows(a, "OUTPUT_CONTRACT.tsv"):
            self.assertEqual(row["sha256"], hashlib.sha256((a / row["path"]).read_bytes()).hexdigest())

    def test_determinism_across_process_hash_seeds(self):
        import sys
        config = self.home / "snapshot.json"
        config.write_text(json.dumps(self.snapshot))
        summaries = []
        for seed in ("1", "987"):
            output = subprocess.check_output([
                sys.executable, "-B", str(Path(__file__).with_name("audit.py")),
                "--repo", str(self.repo), "--snapshot", str(config),
                "--output", str(self.home / ("seed-" + seed))],
                env=dict(os.environ, PYTHONHASHSEED=seed))
            summaries.append(json.loads(output))
        self.assertEqual(summaries[0]["state_root"], summaries[1]["state_root"])

    def test_refuses_existing_output(self):
        self.audit()
        with self.assertRaisesRegex(ValueError, "already exist"):
            self.audit()

    def test_refuses_output_in_worktree(self):
        with self.assertRaisesRegex(ValueError, "outside"):
            inspect(self.repo, self.snapshot, self.repo / "report")
        self.assertFalse((self.repo / "report").exists())

    def test_refuses_output_in_git_admin(self):
        with self.assertRaisesRegex(ValueError, "outside"):
            inspect(self.repo, self.snapshot, self.repo / ".git/report")

    def test_refuses_symlink_output(self):
        (self.home / "link").symlink_to(self.repo, target_is_directory=True)
        with self.assertRaises(ValueError):
            inspect(self.repo, self.snapshot, self.home / "link/report")

    def test_refuses_shallow_repository(self):
        shallow = self.home / "shallow"
        subprocess.run(["git", "clone", "--depth", "1", self.repo.as_uri(), str(shallow)],
                       check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        with self.assertRaisesRegex(ValueError, "shallow"):
            inspect(shallow, self.snapshot, self.home / "report")

    def test_refuses_bad_id_and_missing_commit(self):
        for value in ("HEAD", "--all", self.candidate[:12], "F" * 40, "0" * 40):
            changed = dict(self.snapshot, candidate=value)
            with self.assertRaises(ValueError):
                self.audit("invalid-" + str(len(value)), changed)

    def test_refuses_bad_tree(self):
        with self.assertRaisesRegex(ValueError, "tree pin"):
            self.audit(snapshot=dict(self.snapshot, target_tree=self.candidate))

    def test_refuses_budget_and_leaves_incomplete(self):
        with self.assertRaisesRegex(ValueError, "artifact budget"):
            self.audit(snapshot=dict(self.snapshot, max_artifact_bytes=100))
        self.assertTrue((self.home / "report/INCOMPLETE").exists())
        self.assertFalse((self.home / "report/STATE_ROOT.sha256").exists())

    def test_refuses_commit_limit(self):
        with self.assertRaisesRegex(ValueError, "commit count"):
            self.audit(snapshot=dict(self.snapshot, max_commits=1))
        self.assertFalse((self.home / "report").exists())

    def test_pr_witness_separate_from_proof(self):
        witness = {"repository": "fixture/history", "pulls": [{"number": 1, "state": "closed",
                    "base_ref": "feature-stack", "head_sha": self.candidate,
                    "merge_commit_sha": self.target, "merged_at": "2026-10-02T00:00:00Z"}]}
        path, result = self.audit(prs=witness)
        self.assertEqual(0, result["pr_heads_outside_snapshot"])
        self.assertEqual("feature-stack", self.rows(path, "PULL_REQUESTS.tsv")[0]["base_ref"])
        self.assertEqual("NOT_RUN", result["jdk_tests"])

    def test_pr_head_outside_snapshot_is_visible(self):
        witness = {"repository": "fixture/history", "pulls": [{"number": 1, "state": "open",
                    "base_ref": "main", "head_sha": "f" * 40}]}
        _, result = self.audit(prs=witness)
        self.assertEqual(1, result["pr_heads_outside_snapshot"])

    def test_refuses_incomplete_pr_witness(self):
        with self.assertRaisesRegex(ValueError, "PR witness"):
            self.audit(prs={"repository": "fixture/history", "pulls": []})
        self.assertTrue((self.home / "report/INCOMPLETE").exists())

    def test_octopus_parent_is_not_dropped(self):
        third = self.git("commit-tree", self.git("rev-parse", self.candidate + "^{tree}"),
                         "-p", self.upstream, input=b"independent third parent\n")
        target = self.git("commit-tree", self.snapshot["target_tree"], "-p", self.first,
                          "-p", self.candidate, "-p", third, input=b"octopus\n")
        changed = dict(self.snapshot, target=target, refs={"main": target, "third": third})
        path, _ = self.audit(snapshot=changed)
        rows = [e for e in self.rows(path, "PARENT_EDGES.tsv") if e["child"] == target]
        self.assertEqual(["1", "2", "3"], [r["parent_ordinal"] for r in rows])

    def test_real_content_merge_not_flagged_ancestry_only(self):
        target = self.git("commit-tree", self.git("rev-parse", self.candidate + "^{tree}"),
                          "-p", self.first, "-p", self.candidate, input=b"take feature content\n")
        changed = dict(self.snapshot, target=target, target_tree=self.git("rev-parse", target + "^{tree}"),
                       refs={"main": target, "feature": self.candidate})
        _, result = self.audit(snapshot=changed)
        self.assertEqual(0, result["candidate_paths_absent_at_target"])
        self.assertEqual(0, result["ancestry_only_merge_signals"])

    def test_deleted_then_reintroduced_is_preserved_in_edges(self):
        self.git("checkout", "feature")
        self.git("rm", "runtime.java")
        self.git("commit", "-m", "delete")
        (self.repo / "runtime.java").write_text("class NewRuntimeOwner {}\n")
        self.git("add", ".")
        self.git("commit", "-m", "reintroduce")
        changed = dict(self.snapshot, refs={"main": self.target, "feature": self.git("rev-parse", "HEAD")})
        path, _ = self.audit(snapshot=changed)
        rows = [r for r in self.rows(path, "CHANGES.tsv") if json.loads(r["path_json"]) == "runtime.java"]
        self.assertIn("D", [r["status"] for r in rows])
        self.assertGreaterEqual([r["status"] for r in rows].count("A"), 2)

    def test_refuses_unrelated_history(self):
        unrelated = self.git("commit-tree", self.snapshot["target_tree"], input=b"orphan\n")
        with self.assertRaisesRegex(ValueError, "outside the declared"):
            self.audit(snapshot=dict(self.snapshot, refs={"unrelated": unrelated}))

    def test_detects_ref_movement(self):
        from unittest.mock import patch
        real = GitGraph.refs
        count = 0
        def moved(graph):
            nonlocal count
            count += 1
            return real(graph) + (b"changed" if count > 1 else b"")
        with patch.object(GitGraph, "refs", moved), self.assertRaisesRegex(ValueError, "refs moved"):
            self.audit()
        self.assertTrue((self.home / "report/INCOMPLETE").exists())


class PureContractTest(unittest.TestCase):
    def test_topology_tie_breaks_and_cycle_refusal(self):
        self.assertEqual(["a", "b", "z"], topological({"z": ["a", "b"], "b": [], "a": []}))
        with self.assertRaises(ValueError):
            topological({"a": ["b"], "b": ["a"]})

    def test_path_bytes_roundtrip(self):
        value = b"bad-\xff-name\n\t"
        self.assertEqual(value, json.loads(path_text(value)).encode("utf-8", "surrogateescape"))

    def test_raw_diff_refuses_truncated_or_rename(self):
        for value in (b"bad", b"header\0", b":100644 100644 " + b"1"*40 + b" " + b"2"*40 + b" R100\0path\0"):
            with self.assertRaises(ValueError):
                raw_changes(value)
        self.assertEqual([], raw_changes(b""))

    def test_exact_entry_is_not_semantic_equivalence(self):
        a, b = ("100644", "blob", "a"), ("100755", "blob", "a")
        self.assertEqual("DIFFERENT_REQUIRES_REVIEW", disposition(a, b))
        self.assertEqual("EXACT_ENTRY_RETAINED", disposition(a, a))
        self.assertEqual("ABSENT_AT_TARGET", disposition(a, None))
        self.assertEqual("ABSENT_BOTH", disposition(None, None))

    def test_oid_widths(self):
        self.assertEqual("1"*40, exact_id("1"*40))
        self.assertEqual("1"*64, exact_id("1"*64))
        with self.assertRaises(ValueError):
            exact_id("1"*41)


class CollectorContractTest(unittest.TestCase):
    def test_pagination_and_selected_ids(self):
        from unittest.mock import patch
        from collect_prs import collect
        def pr(n):
            return {"number": n, "title": "fixture", "state": "closed", "head": {"sha": "a" * 40},
                    "base": {"ref": "stack"}, "updated_at": "2026-10-02", "html_url": "https://github.com/example/repo/pull/1"}
        pages = [json.dumps([pr(n) for n in range(1, 101)]).encode(), json.dumps([pr(101)]).encode()]
        class Response:
            def __enter__(self): return self
            def __exit__(self, *args): pass
            def read(self, limit): return pages.pop(0)
        class Opener:
            def open(self, request, timeout): return Response()
        with patch("collect_prs.urllib.request.build_opener", return_value=Opener()):
            result = collect({"repository": "example/repo", "pr_numbers": [1, 101]})
        self.assertEqual([1, 101], [p["number"] for p in result["pulls"]])
        self.assertEqual([], pages)

    def test_missing_metadata_fails_closed(self):
        from unittest.mock import patch, MagicMock
        from collect_prs import collect
        response = MagicMock()
        response.__enter__.return_value.read.return_value = b"[]"
        opener = MagicMock()
        opener.open.return_value = response
        with patch("collect_prs.urllib.request.build_opener", return_value=opener):
            with self.assertRaisesRegex(ValueError, "incomplete"):
                collect({"repository": "example/repo", "pr_numbers": [1]})

    def test_bad_repository_refused_before_network(self):
        from collect_prs import collect
        with self.assertRaisesRegex(ValueError, "repository"):
            collect({"repository": "example/repo?token=bad", "pr_numbers": [1]})

    def test_redirect_never_leaks_authorization(self):
        import urllib.request
        from collect_prs import SameHostRedirect
        request = urllib.request.Request("https://api.github.com/repos/example/repo", headers={"Authorization": "test"})
        for url in ("https://example.invalid/", "http://api.github.com/repos/example/repo"):
            with self.assertRaisesRegex(ValueError, "redirect"):
                SameHostRedirect().redirect_request(request, None, 302, "redirect", {}, url)


if __name__ == "__main__":
    unittest.main()
