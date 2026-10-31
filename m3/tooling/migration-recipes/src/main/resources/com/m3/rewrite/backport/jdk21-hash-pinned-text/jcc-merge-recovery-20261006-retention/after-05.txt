# Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch

from git_graph import GitGraph
from retention import inspect, write


class RetentionAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.home = Path(self.temp.name)
        self.repo = self.home / "repo"
        self.repo.mkdir()
        self.git("init", "-b", "main")
        self.git("config", "user.name", "M3 Fixture")
        self.git("config", "user.email", "fixture@example.invalid")

        (self.repo / "src").mkdir()
        (self.repo / "recipes").mkdir()
        (self.repo / "m3/history").mkdir(parents=True)
        self.a = b"class A {}\n"
        self.b = b"class B {}\n"
        (self.repo / "src/A.java").write_bytes(self.a)
        (self.repo / "src/B.java").write_bytes(self.b)
        manifest = (
            "src/A.java\tABSENT\t" + hashlib.sha256(self.a).hexdigest() + "\tA.txt\n"
            "src/B.java\tABSENT\t" + hashlib.sha256(self.b).hexdigest() + "\tB.txt\n"
        )
        (self.repo / "recipes/cap.manifest.tsv").write_text(manifest, encoding="utf-8")
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            "capability_id\tmanifest_path\n"
            "cap\trecipes/cap.manifest.tsv\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "retained baseline")
        self.baseline = self.git("rev-parse", "HEAD")

    def git(self, *args) -> str:
        return subprocess.check_output(
            ["git", "-C", str(self.repo), *args],
            stderr=subprocess.DEVNULL,
        ).decode().strip()

    def test_exact_retention_is_deterministic(self):
        first = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        second = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(first, second)
        self.assertEqual(1, first["capabilities"])
        self.assertEqual(2, first["targets"])
        self.assertEqual(2, first["retained_exact"])
        self.assertEqual(0, first["present_drifted_review"])
        self.assertEqual(0, first["missing"])

    def test_drift_is_review_and_missing_is_explicit(self):
        (self.repo / "src/A.java").write_text("class A { int x; }\n", encoding="utf-8")
        (self.repo / "src/B.java").unlink()
        self.git("add", "-A")
        self.git("commit", "-m", "drift and loss")
        head = self.git("rev-parse", "HEAD")

        result = inspect(
            self.repo,
            head,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(0, result["retained_exact"])
        self.assertEqual(1, result["present_drifted_review"])
        self.assertEqual(1, result["missing"])
        states = {row["path"]: row["state"] for row in result["rows"]}
        self.assertEqual("PRESENT_DRIFTED_REVIEW", states["src/A.java"])
        self.assertEqual("MISSING", states["src/B.java"])

    def test_manifest_is_read_from_exact_commit_not_worktree(self):
        (self.repo / "recipes/cap.manifest.tsv").write_text(
            "broken worktree\n", encoding="utf-8"
        )
        result = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        self.assertEqual(2, result["retained_exact"])

    def test_invalid_manifest_and_catalogue_fail_closed(self):
        (self.repo / "recipes/cap.manifest.tsv").write_text(
            "src/B.java\tABSENT\t" + hashlib.sha256(self.b).hexdigest() + "\tB.txt\n"
            "src/A.java\tABSENT\t" + hashlib.sha256(self.a).hexdigest() + "\tA.txt\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "unsorted manifest")
        head = self.git("rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "sorted"):
            inspect(self.repo, head, "m3/history/RETAINED_CAPABILITIES.tsv")

        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            "capability_id\tmanifest_path\n"
            "cap\t../escape.tsv\n",
            encoding="utf-8",
        )
        self.git("add", ".")
        self.git("commit", "-m", "unsafe catalogue")
        unsafe = self.git("rev-parse", "HEAD")
        with self.assertRaisesRegex(ValueError, "noncanonical"):
            inspect(self.repo, unsafe, "m3/history/RETAINED_CAPABILITIES.tsv")

    def test_write_is_deterministic_and_refuses_existing_output(self):
        result = inspect(
            self.repo,
            self.baseline,
            "m3/history/RETAINED_CAPABILITIES.tsv",
        )
        output = self.home / "out"
        write(result, output)
        self.assertTrue((output / "RETENTION.tsv").is_file())
        summary = json.loads((output / "SUMMARY.json").read_text(encoding="utf-8"))
        self.assertEqual(result["root"], summary["root"])
        self.assertEqual(
            result["root"],
            (output / "STATE_ROOT.sha256").read_text(encoding="ascii").strip(),
        )
        with self.assertRaisesRegex(ValueError, "already exist"):
            write(result, output)

    def checkpoint(self, message="candidate"):
        self.git("add", "-A")
        self.git("commit", "-m", message)
        return self.git("rev-parse", "HEAD")

    def manifest(self, path, targets):
        destination = self.repo / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_text("".join(
            target + "\tABSENT\t" + hashlib.sha256(content).hexdigest() + "\tfixture.txt\n"
            for target, content in sorted(targets.items())
        ), encoding="utf-8")

    def catalogue(self, rows):
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            "capability_id\tmanifest_path\n" + "".join(
                capability + "\t" + manifest + "\n" for capability, manifest in rows
            ), encoding="utf-8",
        )

    def baseline_audit(self, commit, baselines=None, include_parents=False):
        return inspect(self.repo, commit, "m3/history/RETAINED_CAPABILITIES.tsv",
                       baseline_commits=(self.baseline,) if baselines is None else baselines,
                       include_parents=include_parents)

    def kinds(self, result):
        return {conflict["kind"] for conflict in result["declaration_conflicts"]}

    def cli(self, commit, *extra):
        output = self.home / ("cli-" + str(len(list(self.home.glob("cli-*")))))
        process = subprocess.run(
            [sys.executable, "-B", str(Path(__file__).with_name("retention.py")),
             "--repo", str(self.repo), "--commit", commit, "--output", str(output),
             "--require-no-missing", *extra],
            capture_output=True, text=True, check=False,
        )
        return process, output

    def test_default_summary_root_and_outputs_remain_exact(self):
        rows = [
            {"capability_id": "cap", "manifest_path": "recipes/cap.manifest.tsv",
             "path": target, "expected_sha256": hashlib.sha256(content).hexdigest(),
             "actual_sha256": hashlib.sha256(content).hexdigest(), "state": "RETAINED_EXACT"}
            for target, content in [("src/A.java", self.a), ("src/B.java", self.b)]
        ]
        expected = {"commit": self.baseline, "capabilities": 1, "targets": 2,
                    "retained_exact": 2, "present_drifted_review": 0, "missing": 0, "rows": rows}
        expected["root"] = hashlib.sha256(
            json.dumps(expected, sort_keys=True, separators=(",", ":")).encode("utf-8")
        ).hexdigest()
        actual = inspect(self.repo, self.baseline, "m3/history/RETAINED_CAPABILITIES.tsv")
        self.assertEqual(expected, actual)
        self.assertEqual(actual, self.baseline_audit(self.baseline, (), False))
        left, right = self.home / "legacy", self.home / "extended-default"
        write(expected, left)
        write(actual, right)
        self.assertEqual({path.name: path.read_bytes() for path in left.iterdir()},
                         {path.name: path.read_bytes() for path in right.iterdir()})

    def test_dropped_capability_keeps_baseline_targets_and_fails_strict_cli(self):
        self.catalogue([])
        head = self.checkpoint("drop catalogue row")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["targets"])
        self.assertEqual(2, result["retained_exact"])
        self.assertEqual(0, result["missing"])
        self.assertIn("CAPABILITY_REMOVED", self.kinds(result))
        process, output = self.cli(head, "--baseline-commit", self.baseline)
        self.assertEqual(3, process.returncode, process.stderr)
        summary = json.loads((output / "SUMMARY.json").read_text())
        self.assertGreater(summary["declaration_conflict_count"], 0)

    def test_deleted_catalogue_keeps_baseline_targets(self):
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").unlink()
        head = self.checkpoint("drop catalogue")
        result = self.baseline_audit(head)
        self.assertEqual("ABSENT", result["current_catalogue_state"])
        self.assertEqual(2, result["targets"])
        self.assertIn("CURRENT_CATALOGUE_ABSENT", self.kinds(result))
        process, _ = self.cli(head, "--baseline-commit", self.baseline)
        self.assertEqual(3, process.returncode, process.stderr)

    def test_removed_manifest_keeps_baseline_targets(self):
        (self.repo / "recipes/cap.manifest.tsv").unlink()
        head = self.checkpoint("drop manifest")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["retained_exact"])
        self.assertIn("CURRENT_MANIFEST_ABSENT", self.kinds(result))
        self.assertIn("TARGET_DECLARATION_REMOVED", self.kinds(result))
        process, _ = self.cli(head, "--include-parents")
        self.assertEqual(3, process.returncode, process.stderr)

    def test_removed_manifest_row_keeps_its_obligation(self):
        self.manifest("recipes/cap.manifest.tsv", {"src/A.java": self.a})
        head = self.checkpoint("drop one target declaration")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["retained_exact"])
        removed = [row["path"] for row in result["declaration_conflicts"]
                   if row["kind"] == "TARGET_DECLARATION_REMOVED"]
        self.assertEqual(["src/B.java"], removed)

    def test_empty_manifest_keeps_all_baseline_obligations(self):
        (self.repo / "recipes/cap.manifest.tsv").write_text("# no current rows\n")
        head = self.checkpoint("drop all manifest rows")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["targets"])
        self.assertIn("CURRENT_MANIFEST_EMPTY", self.kinds(result))

    def test_moved_manifest_is_explicit_declaration_conflict(self):
        (self.repo / "recipes/cap.manifest.tsv").rename(self.repo / "recipes/moved.tsv")
        self.catalogue([("cap", "recipes/moved.tsv")])
        head = self.checkpoint("move manifest declaration")
        result = self.baseline_audit(head)
        self.assertIn("MANIFEST_PATH_CHANGED", self.kinds(result))
        self.assertEqual(2, result["distinct_target_paths"])
        self.assertEqual({"recipes/cap.manifest.tsv", "recipes/moved.tsv"},
                         {row["manifest_path"] for row in result["obligations"]})

    def test_deleted_target_directory_fails_strict_cli(self):
        shutil.rmtree(self.repo / "src")
        head = self.checkpoint("drop target directory")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["missing"])
        self.assertEqual(0, result["declaration_conflict_count"])
        process, _ = self.cli(head, "--include-parents")
        self.assertEqual(3, process.returncode, process.stderr)

    def test_rewriting_expected_hash_cannot_reseal_changed_target(self):
        changed = b"class A { int changed; }\n"
        (self.repo / "src/A.java").write_bytes(changed)
        self.manifest("recipes/cap.manifest.tsv", {"src/A.java": changed, "src/B.java": self.b})
        head = self.checkpoint("rewrite target and expected hash")
        result = self.baseline_audit(head)
        self.assertIn("EXPECTED_SHA256_CHANGED", self.kinds(result))
        self.assertIn("EXPECTED_SHA256_CONFLICT", self.kinds(result))
        a_rows = [row for row in result["rows"] if row["path"] == "src/A.java"]
        self.assertEqual({hashlib.sha256(self.a).hexdigest(), hashlib.sha256(changed).hexdigest()},
                         {row["expected_sha256"] for row in a_rows})
        self.assertEqual({"RETAINED_EXACT", "PRESENT_DRIFTED_REVIEW"}, {row["state"] for row in a_rows})
        process, _ = self.cli(head, "--baseline-commit", self.baseline)
        self.assertEqual(3, process.returncode, process.stderr)

    def test_target_drift_with_unchanged_declaration_remains_review(self):
        (self.repo / "src/A.java").write_bytes(b"class A { int changed; }\n")
        head = self.checkpoint("legitimate target evolution for review")
        result = self.baseline_audit(head)
        self.assertEqual(1, result["present_drifted_review"])
        self.assertEqual(0, result["missing"])
        self.assertEqual(0, result["declaration_conflict_count"])
        process, _ = self.cli(head, "--include-parents", "--baseline-commit", self.baseline)
        self.assertEqual(0, process.returncode, process.stderr)

    def test_conflicting_baselines_are_order_independent_without_a_winner(self):
        changed = b"class A { int changed; }\n"
        self.manifest("recipes/cap.manifest.tsv", {"src/A.java": changed, "src/B.java": self.b})
        other = self.checkpoint("independent conflicting expectation")
        first = self.baseline_audit(other, (self.baseline, other))
        second = self.baseline_audit(other, (other, self.baseline))
        self.assertEqual(first, second)
        self.assertIn("EXPECTED_SHA256_CONFLICT", self.kinds(first))
        self.assertEqual(2, len([row for row in first["obligations"] if row["path"] == "src/A.java"]))
        with self.assertRaisesRegex(ValueError, "duplicate explicit"):
            self.baseline_audit(other, (self.baseline, self.baseline))

    def test_conflicting_baseline_manifest_paths_are_explicit(self):
        (self.repo / "recipes/cap.manifest.tsv").rename(self.repo / "recipes/moved.tsv")
        self.catalogue([("cap", "recipes/moved.tsv")])
        other = self.checkpoint("alternative manifest owner")
        result = self.baseline_audit(other, (self.baseline, other))
        self.assertIn("BASELINE_MANIFEST_CONFLICT", self.kinds(result))

    def test_harmless_additional_capability_and_target_are_accepted(self):
        c, d = b"class C {}\n", b"class D {}\n"
        (self.repo / "src/C.java").write_bytes(c)
        (self.repo / "src/D.java").write_bytes(d)
        self.manifest("recipes/cap.manifest.tsv", {"src/A.java": self.a, "src/B.java": self.b, "src/C.java": c})
        self.manifest("recipes/extra.manifest.tsv", {"src/D.java": d})
        self.catalogue([("cap", "recipes/cap.manifest.tsv"), ("extra", "recipes/extra.manifest.tsv")])
        head = self.checkpoint("add compatible capability and target")
        result = self.baseline_audit(head)
        self.assertEqual(2, result["capabilities"])
        self.assertEqual(4, result["retained_exact"])
        self.assertEqual(0, result["declaration_conflict_count"])
        process, _ = self.cli(head, "--include-parents")
        self.assertEqual(0, process.returncode, process.stderr)

    def test_baseline_manifest_comes_from_its_own_commit(self):
        self.manifest("recipes/cap.manifest.tsv", {"src/A.java": self.a})
        head = self.checkpoint("candidate omits B")
        before = self.baseline_audit(head)
        (self.repo / "recipes/cap.manifest.tsv").write_text("uncommitted invalid worktree\n")
        self.assertEqual(before, self.baseline_audit(head))
        inherited = [row for row in before["obligations"] if row["path"] == "src/B.java"]
        self.assertEqual([self.baseline], inherited[0]["declared_by"])

    def test_every_octopus_parent_contributes_its_obligations(self):
        parent_ids = [self.baseline]
        for suffix in ("C", "D"):
            self.git("checkout", "-b", "side-" + suffix, self.baseline)
            content = ("class " + suffix + " {}\n").encode()
            path = "src/" + suffix + ".java"
            manifest = "recipes/" + suffix + ".manifest.tsv"
            (self.repo / path).write_bytes(content)
            self.manifest(manifest, {path: content})
            self.catalogue([("cap", "recipes/cap.manifest.tsv"), (suffix, manifest)])
            parent_ids.append(self.checkpoint("side " + suffix))
        tree = self.git("rev-parse", self.baseline + "^{tree}")
        arguments = ["commit-tree", tree, "-m", "octopus preserving only first tree"]
        for parent in parent_ids:
            arguments.extend(["-p", parent])
        merge = self.git(*arguments)
        result = self.baseline_audit(merge, (), True)
        self.assertEqual(sorted(parent_ids), result["direct_parent_commits"])
        self.assertEqual(3, len(result["baselines"]))
        self.assertEqual({"src/C.java", "src/D.java"},
                         {row["path"] for row in result["rows"] if row["state"] == "MISSING"})
        self.assertEqual(4, result["distinct_target_paths"])
        self.assertEqual(result, self.baseline_audit(merge, (), True))

    def test_implicit_parent_catalogue_absence_is_recorded_but_explicit_refuses(self):
        catalogue = self.repo / "m3/history/RETAINED_CAPABILITIES.tsv"
        original = catalogue.read_bytes()
        catalogue.unlink()
        parent = self.checkpoint("older history without a catalogue")
        catalogue.parent.mkdir(parents=True, exist_ok=True)
        catalogue.write_bytes(original)
        head = self.checkpoint("introduce catalogue")
        result = self.baseline_audit(head, (), True)
        self.assertEqual("ABSENT_IMPLICIT_PARENT", result["baselines"][0]["catalogue_state"])
        self.assertEqual(parent, result["baselines"][0]["commit"])
        self.assertEqual(0, result["baselines"][0]["targets"])
        self.assertEqual(0, result["declaration_conflict_count"])
        for parents in (False, True):
            with self.subTest(parents=parents), self.assertRaisesRegex(ValueError, "baseline.*catalogue missing"):
                self.baseline_audit(head, (parent,), parents)

    def test_explicit_pre_push_baseline_survives_intermediate_catalogue_deletion(self):
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").unlink()
        self.checkpoint("first pushed commit erases catalogue")
        shutil.rmtree(self.repo / "src")
        head = self.checkpoint("last pushed commit erases target directory")
        result = self.baseline_audit(head, (self.baseline,), True)
        self.assertEqual(2, result["missing"])
        self.assertEqual(2, len(result["baselines"]))
        self.assertIn("CAPABILITY_REMOVED", self.kinds(result))

    def test_invalid_existing_parent_catalogue_is_not_treated_as_absence(self):
        catalogue = self.repo / "m3/history/RETAINED_CAPABILITIES.tsv"
        original = catalogue.read_bytes()
        catalogue.write_text("bad\theader\n")
        parent = self.checkpoint("malformed baseline")
        catalogue.write_bytes(original)
        head = self.checkpoint("restore valid current declaration")
        with self.assertRaisesRegex(ValueError, "catalogue header"):
            self.baseline_audit(head, (), True)
        with self.assertRaisesRegex(ValueError, "catalogue header"):
            self.baseline_audit(head, (parent,))

    def test_missing_baseline_manifest_refuses_even_for_implicit_parent(self):
        manifest = self.repo / "recipes/cap.manifest.tsv"
        original = manifest.read_bytes()
        manifest.unlink()
        parent = self.checkpoint("baseline manifest loss")
        manifest.write_bytes(original)
        head = self.checkpoint("current manifest restoration")
        for baselines, parents in [((parent,), False), ((), True)]:
            with self.subTest(parents=parents), self.assertRaisesRegex(ValueError, "baseline retained manifest missing"):
                self.baseline_audit(head, baselines, parents)

    def test_unsafe_and_non_commit_baseline_ids_refuse(self):
        tree = self.git("rev-parse", self.baseline + "^{tree}")
        for value in ["HEAD", self.baseline[:12], self.baseline.upper(), "--all", "0" * 40,
                      self.baseline + "^", tree, None]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                self.baseline_audit(self.baseline, (value,))
        with self.assertRaisesRegex(ValueError, "immutable tuple"):
            self.baseline_audit(self.baseline, [self.baseline])
        with self.assertRaisesRegex(ValueError, "include_parents"):
            self.baseline_audit(self.baseline, (), 1)

    def test_unsafe_catalogue_and_manifest_paths_refuse_in_baseline_mode(self):
        for path in ["../escape", "/absolute", "m3/../escape", "m3//history/file", "m3/.git/file",
                     "m3\\history\\file", "m3/history/file\n", "m3/history/fi\tle", "C:/outside", " padded"]:
            with self.subTest(path=path), self.assertRaisesRegex(ValueError, "noncanonical"):
                inspect(self.repo, self.baseline, path, include_parents=True)
        self.catalogue([("cap", "../escape.tsv")])
        head = self.checkpoint("unsafe current manifest path")
        with self.assertRaisesRegex(ValueError, "noncanonical"):
            self.baseline_audit(head)

    def test_unsafe_target_and_duplicate_obligations_refuse(self):
        digest = hashlib.sha256(self.a).hexdigest()
        manifest = self.repo / "recipes/cap.manifest.tsv"
        manifest.write_text("../escape\tABSENT\t" + digest + "\tA.txt\n")
        head = self.checkpoint("unsafe target")
        with self.assertRaisesRegex(ValueError, "noncanonical"):
            self.baseline_audit(head)
        row = "src/A.java\tABSENT\t" + digest + "\tA.txt\n"
        manifest.write_text(row + row)
        head = self.checkpoint("duplicate manifest target")
        with self.assertRaisesRegex(ValueError, "sorted"):
            self.baseline_audit(head)
        manifest.write_text(row)
        self.catalogue([("cap", "recipes/cap.manifest.tsv"), ("cap", "recipes/cap.manifest.tsv")])
        head = self.checkpoint("duplicate capability")
        with self.assertRaisesRegex(ValueError, "duplicate"):
            self.baseline_audit(head)

    def test_opt_in_digest_parser_accepts_all_digits_and_refuses_whitespace(self):
        manifest = self.repo / "recipes/cap.manifest.tsv"
        manifest.write_text("src/A.java\tABSENT\t" + "0" * 64 + "\tA.txt\n")
        head = self.checkpoint("all digit SHA-256 declaration")
        result = self.baseline_audit(head)
        self.assertIn("0" * 64, {row["expected_sha256"] for row in result["rows"]})
        manifest.write_text("src/A.java\tABSENT\t" + "a" * 31 + "  " + "b" * 31 + "\tA.txt\n")
        head = self.checkpoint("whitespace inside digest")
        with self.assertRaisesRegex(ValueError, "invalid/sorted"):
            self.baseline_audit(head)

    def test_opt_in_symlink_target_and_declaration_refuse(self):
        target = self.repo / "src/A.java"
        target.unlink()
        os.symlink("B.java", target)
        head = self.checkpoint("symlink target")
        with self.assertRaisesRegex(ValueError, "not a regular blob"):
            self.baseline_audit(head)
        target.unlink()
        target.write_bytes(self.a)
        manifest = self.repo / "recipes/cap.manifest.tsv"
        manifest.rename(self.repo / "recipes/real.tsv")
        os.symlink("real.tsv", manifest)
        head = self.checkpoint("symlink declaration")
        with self.assertRaisesRegex(ValueError, "not a regular blob"):
            self.baseline_audit(head)

    def test_ref_movement_still_refuses_baseline_audit(self):
        with patch.object(GitGraph, "refs", side_effect=[b"initial refs", b"changed refs"]):
            with self.assertRaisesRegex(ValueError, "refs moved"):
                self.baseline_audit(self.baseline)

    def test_baseline_output_replay_retains_obligation_provenance(self):
        result = self.baseline_audit(self.baseline, (), True)
        first, second = self.home / "first", self.home / "second"
        write(result, first)
        write(result, second)
        self.assertEqual({path.name: path.read_bytes() for path in first.iterdir()},
                         {path.name: path.read_bytes() for path in second.iterdir()})
        summary = json.loads((first / "SUMMARY.json").read_text())
        self.assertEqual(result["obligations"], summary["obligations"])
        self.assertTrue(all(row["declared_by"] == [self.baseline] for row in summary["obligations"]))

    def test_invalid_explicit_cli_baseline_refuses_before_output(self):
        process, output = self.cli(self.baseline, "--baseline-commit", "HEAD")
        self.assertEqual(2, process.returncode, process.stderr)
        self.assertFalse(output.exists())

    def test_repeated_cli_baselines_preserve_all_exact_origins(self):
        (self.repo / "src/A.java").write_bytes(b"class A { int evolved; }\n")
        head = self.checkpoint("second immutable baseline")
        process, output = self.cli(head, "--baseline-commit", self.baseline,
                                   "--baseline-commit", head, "--include-parents")
        self.assertEqual(0, process.returncode, process.stderr)
        summary = json.loads((output / "SUMMARY.json").read_text())
        self.assertEqual(sorted([self.baseline, head]), summary["explicit_baseline_commits"])
        self.assertEqual([self.baseline], summary["direct_parent_commits"])
        self.assertTrue(all(row["declared_by"] == sorted([self.baseline, head])
                            for row in summary["obligations"]))

    def test_empty_or_unreadable_explicit_baseline_catalogue_refuses(self):
        catalogue = self.repo / "m3/history/RETAINED_CAPABILITIES.tsv"
        for data in [b"capability_id\tmanifest_path\n", b"\xff"]:
            catalogue.write_bytes(data)
            baseline = self.checkpoint("inadmissible baseline catalogue")
            self.catalogue([("cap", "recipes/cap.manifest.tsv")])
            head = self.checkpoint("valid candidate catalogue")
            with self.subTest(data=data), self.assertRaises((ValueError, UnicodeError)):
                self.baseline_audit(head, (baseline,))

    def test_conflicting_capabilities_cannot_hide_competing_hashes(self):
        self.manifest("recipes/other.tsv", {"src/A.java": b"other expected content\n"})
        self.catalogue([("cap", "recipes/cap.manifest.tsv"), ("other", "recipes/other.tsv")])
        head = self.checkpoint("competing capability expectation")
        result = self.baseline_audit(head)
        self.assertIn("EXPECTED_SHA256_CONFLICT", self.kinds(result))
        self.assertEqual({"cap", "other"},
                         {row["capability_id"] for row in result["rows"] if row["path"] == "src/A.java"})

    def test_malformed_quoted_catalogue_is_a_cli_refusal(self):
        (self.repo / "m3/history/RETAINED_CAPABILITIES.tsv").write_text(
            'capability_id\tmanifest_path\n"unterminated\tmanifest\n'
        )
        head = self.checkpoint("malformed quoted catalogue")
        process, output = self.cli(head, "--include-parents")
        self.assertEqual(2, process.returncode, process.stderr)
        self.assertIn("RETENTION_REFUSED", process.stderr)
        self.assertNotIn("Traceback", process.stderr)
        self.assertFalse(output.exists())


if __name__ == "__main__":
    unittest.main()
