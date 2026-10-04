# SPDX-License-Identifier: Apache-2.0
"""Real-file regressions for preflight, alias safety, replay and bounded failure."""
import csv
import hashlib
import os
import stat
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import source_convergence_gate as gate
import materialize_source_convergence as materializer


def sha(payload):
    return hashlib.sha256(payload).hexdigest()


class ConvergencePreflightTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.source, self.work, self.evidence = [
            self.root / name for name in ("source", "work", "evidence")
        ]
        self.header = ["path", "preSha256", "postSha256", "fixedPoint", "status", "candidate"]
        self.rows = []
        self.before, self.after = {}, {}
        for name in ("A", "B"):
            path = f"src/example/{name}.java"
            self.before[path] = f"class {name} {{}}\n".encode()
            self.after[path] = f"class {name} {{ /* normalized */ }}\n".encode()
            for tree, payload in ((self.source, self.before[path]),
                                  (self.work, self.before[path]),
                                  (self.evidence / "candidates", self.after[path])):
                dest = tree / path
                dest.parent.mkdir(parents=True, exist_ok=True)
                dest.write_bytes(payload)
            self.rows.append([path, sha(self.before[path]), sha(self.after[path]),
                              "true", "CONVERGED_CHANGED", "candidates/" + path])
        self.manifest = self.evidence / "SOURCE_CONVERGENCE.tsv"
        self.targets = self.root / "targets.txt"
        self.targets.write_text("\n".join(r[0] for r in self.rows) + "\n")
        self.save()

    def save(self):
        with self.manifest.open("w", newline="") as output:
            writer = csv.writer(output, delimiter="\t", lineterminator="\n")
            writer.writerow(self.header)
            writer.writerows(self.rows)

    def run_plan(self):
        return materializer.materialize(self.source, self.manifest, self.work)

    def assert_untouched(self):
        for path, content in self.before.items():
            self.assertEqual(content, (self.source / path).read_bytes())
            self.assertEqual(content, (self.work / path).read_bytes())
        self.assertFalse((self.work / "m3-normalized-baseline.root").exists())
        self.assertFalse(list(self.work.rglob(".m3-convergence-*")))

    def test_inconsistent_unchanged_hashes(self):
        self.rows[0][4:] = ["CONVERGED_UNCHANGED", ""]
        self.save()
        with self.assertRaisesRegex(ValueError, "inconsistent convergence hashes"):
            gate.verify(self.source, self.manifest, self.targets)
        with self.assertRaises(ValueError):
            self.run_plan()
        self.assert_untouched()

    def test_inconsistent_changed_hashes(self):
        self.rows[0][2] = self.rows[0][1]
        self.save()
        with self.assertRaises(ValueError):
            self.run_plan()
        self.assert_untouched()

    def test_inconsistent_candidate_presence(self):
        for status, post, candidate in [
            ("CONVERGED_CHANGED", self.rows[0][2], ""),
            ("CONVERGED_UNCHANGED", self.rows[0][1], self.rows[0][5]),
        ]:
            with self.subTest(status=status):
                self.rows[0][2], self.rows[0][4], self.rows[0][5] = post, status, candidate
                self.save()
                with self.assertRaisesRegex(ValueError, "inconsistent convergence candidate"):
                    self.run_plan()
        self.assert_untouched()

    def test_all_candidates_validated_before_writes(self):
        (self.evidence / self.rows[1][5]).write_text("corrupt\n")
        with self.assertRaisesRegex(ValueError, "candidate postimage drift"):
            self.run_plan()
        self.assert_untouched()

    def test_later_worktree_drift_preserves_earlier_file(self):
        first = self.work / self.rows[0][0]
        (self.work / self.rows[1][0]).write_text("drift\n")
        with self.assertRaisesRegex(ValueError, "worktree preimage drift"):
            self.run_plan()
        self.assertEqual(self.before[self.rows[0][0]], first.read_bytes())

    def test_hardlinked_target_preserves_original(self):
        path = self.rows[0][0]
        target, original = self.work / path, self.source / path
        target.unlink()
        os.link(original, target)
        self.run_plan()
        self.assertEqual(self.before[path], original.read_bytes())
        self.assertEqual(self.after[path], target.read_bytes())
        self.assertFalse(os.path.samefile(target, original))

    def test_symlink_target_refused_before_writes(self):
        target = self.work / self.rows[1][0]
        target.unlink()
        target.symlink_to(self.source / self.rows[1][0])
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.run_plan()
        self.assertEqual(self.before[self.rows[0][0]],
                         (self.work / self.rows[0][0]).read_bytes())

    def test_symlink_candidate_refused(self):
        candidate = self.evidence / self.rows[1][5]
        value = self.root / "outside"
        value.write_bytes(self.after[self.rows[1][0]])
        candidate.unlink()
        candidate.symlink_to(value)
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.run_plan()
        self.assert_untouched()

    def test_symlink_parent_refused(self):
        directory = self.work / "src/example"
        directory.rename(self.work / "relocated")
        directory.symlink_to(self.work / "relocated", target_is_directory=True)
        with self.assertRaisesRegex(ValueError, "symlink"):
            self.run_plan()

    def test_receipt_collision_refused_before_writes(self):
        (self.work / "m3-normalized-baseline.tsv").write_text("other task\n")
        with self.assertRaisesRegex(ValueError, "receipt drift"):
            self.run_plan()
        self.assert_untouched()

    def test_receipt_symlink_is_not_followed(self):
        outside = self.root / "outside"
        outside.write_text("protected\n")
        (self.work / "m3-normalized-baseline.tsv").symlink_to(outside)
        with self.assertRaisesRegex(ValueError, "receipt symlink"):
            self.run_plan()
        self.assertEqual("protected\n", outside.read_text())
        self.assert_untouched()

    def test_replay_has_same_root_receipts_and_file_identity(self):
        first = self.run_plan()
        snapshot = {p.relative_to(self.work): (p.read_bytes(), p.stat().st_ino, p.stat().st_mtime_ns)
                    for p in self.work.rglob("*") if p.is_file()}
        self.assertEqual(first, self.run_plan())
        self.assertEqual(snapshot, {p.relative_to(self.work): (p.read_bytes(), p.stat().st_ino,
                                                              p.stat().st_mtime_ns)
                                    for p in self.work.rglob("*") if p.is_file()})

    def test_valid_v1_roots_remain_exact(self):
        expected = sha(("M3_SOURCE_CONVERGENCE_GATE_V1\n" + "\n".join(
            "\x1f".join((r[0], r[1], r[2], r[4], "fixedPoint=true")) for r in self.rows
        ) + "\n").encode())
        self.assertEqual(expected, gate.verify(self.source, self.manifest, self.targets))
        normalized = sha(("M3_NORMALIZED_JDK21_BASELINE_V1\n" + "\n".join(
            "\x1f".join((r[0], r[1], r[2], r[4], r[2])) for r in self.rows
        ) + "\n").encode())
        self.assertEqual(normalized, self.run_plan())

    def test_unchanged_row_is_not_rewritten(self):
        path = self.rows[1][0]
        self.rows[1][2], self.rows[1][4], self.rows[1][5] = self.rows[1][1], "CONVERGED_UNCHANGED", ""
        self.save()
        inode = (self.work / path).stat().st_ino
        self.run_plan()
        self.assertEqual(inode, (self.work / path).stat().st_ino)

    def test_permissions_preserved(self):
        target = self.work / self.rows[0][0]
        target.chmod(0o640)
        self.run_plan()
        self.assertEqual(0o640, stat.S_IMODE(target.stat().st_mode))

    def test_empty_manifest_refused(self):
        self.rows = []
        self.save()
        with self.assertRaisesRegex(ValueError, "empty convergence manifest"):
            self.run_plan()
        self.assert_untouched()

    def test_duplicate_header_refused(self):
        self.header.append("path")
        for row in self.rows:
            row.append(row[0])
        self.save()
        with self.assertRaisesRegex(ValueError, "header mismatch"):
            self.run_plan()
        self.assert_untouched()

    def test_incomplete_and_extra_cells_refused(self):
        original = list(self.rows[0])
        for row in (original[:-1], original + ["extra"]):
            with self.subTest(width=len(row)):
                self.rows[0] = row
                self.save()
                with self.assertRaisesRegex(ValueError, "row width"):
                    self.run_plan()
        self.assert_untouched()

    def test_boolean_typos_refused(self):
        self.rows[0][3] = "truthy"
        self.save()
        with self.assertRaisesRegex(ValueError, "invalid fixedPoint"):
            self.run_plan()
        self.assert_untouched()

    def test_path_aliases_and_traversal_refused(self):
        for bad in ("src//A.java", "src/./A.java", "src/../A.java", "src/.git/A.java",
                    "src/a:\u0001A.java", "/src/A.java", " src/A.java", "src/A.java\n"):
            with self.subTest(path=bad):
                self.rows[0][0] = bad
                self.save()
                with self.assertRaises(ValueError):
                    self.run_plan()
        self.assert_untouched()

    def test_candidate_escape_and_absolute_path_refused(self):
        for bad in ("../outside.java", "/tmp/out.java", "candidates//A.java", "C:/A.java"):
            with self.subTest(path=bad):
                self.rows[0][5] = bad
                self.save()
                with self.assertRaises(ValueError):
                    self.run_plan()
        self.assert_untouched()

    def test_source_root_and_worktree_must_be_disjoint(self):
        for target in (self.source, self.source / "nested", self.source.parent):
            with self.subTest(target=target):
                with self.assertRaisesRegex(ValueError, "disjoint"):
                    materializer.materialize(self.source, self.manifest, target)
        self.assert_untouched()

    def test_copy_refuses_nested_and_occupied_destination(self):
        with self.assertRaisesRegex(ValueError, "disjoint"):
            materializer.copy_tree(self.source, self.source / "nested")
        self.assertFalse((self.source / "nested").exists())
        with self.assertRaisesRegex(ValueError, "already exists"):
            materializer.copy_tree(self.source, self.work)

    def test_staging_failure_does_not_apply_partial_batch(self):
        real = materializer._stage
        count = 0
        def fail_second(target, payload, mode):
            nonlocal count
            count += 1
            if count == 2:
                raise OSError("injected staging failure")
            return real(target, payload, mode)
        with patch.object(materializer, "_stage", side_effect=fail_second):
            with self.assertRaisesRegex(OSError, "injected"):
                self.run_plan()
        self.assert_untouched()

    def test_partial_replace_failure_is_resumable_without_success_marker(self):
        real = materializer.os.replace
        count = 0
        def fail_second(source, target):
            nonlocal count
            count += 1
            if count == 2:
                raise OSError("injected replacement failure")
            return real(source, target)
        with patch.object(materializer.os, "replace", side_effect=fail_second):
            with self.assertRaisesRegex(OSError, "injected"):
                self.run_plan()
        self.assertFalse((self.work / "m3-normalized-baseline.root").exists())
        self.assertFalse(list(self.work.rglob(".m3-convergence-*")))
        self.run_plan()
        for path, payload in self.after.items():
            self.assertEqual(payload, (self.work / path).read_bytes())
            self.assertEqual(self.before[path], (self.source / path).read_bytes())

    def test_input_drift_during_staging_is_detected_before_apply(self):
        real = materializer._stage
        def change_manifest(target, payload, mode):
            staged = real(target, payload, mode)
            self.rows[1][2] = self.rows[1][1]
            self.save()
            return staged
        with patch.object(materializer, "_stage", side_effect=change_manifest):
            with self.assertRaises(ValueError):
                self.run_plan()
        self.assert_untouched()

    def test_stale_success_marker_refused(self):
        self.run_plan()
        path = self.rows[0][0]
        (self.work / path).write_bytes(self.before[path])
        with self.assertRaisesRegex(ValueError, "success marker"):
            self.run_plan()
        self.assertEqual(self.before[path], (self.work / path).read_bytes())

    def test_unrelated_hold_does_not_certify_or_block_selected_subset(self):
        self.rows[1][3:5] = ["false", "HOLD"]
        self.save()
        self.targets.write_text(self.rows[0][0] + "\n")
        gate.verify(self.source, self.manifest, self.targets)
        with self.assertRaisesRegex(ValueError, "unresolved"):
            self.run_plan()

    def test_target_order_dedup_and_non_java_filter_preserved(self):
        self.targets.write_text("path\n# selected roots\nmake/a.gmk\n"
                                + self.rows[1][0] + "\n" + self.rows[0][0] + "\n"
                                + self.rows[0][0] + "\n")
        self.assertEqual(sorted(self.before), gate.load_targets(self.targets))
        gate.verify(self.source, self.manifest, self.targets)

    def test_command_line_entrypoints(self):
        script = Path(materializer.__file__)
        run = subprocess.run([sys.executable, str(script), str(self.source),
                              str(self.manifest), str(self.work)],
                             capture_output=True, text=True)
        self.assertEqual(0, run.returncode, run.stderr)
        self.assertIn("M3_NORMALIZED_BASELINE\tPASS\t", run.stdout)
        verify = subprocess.run([sys.executable, gate.__file__, str(self.source),
                                 str(self.manifest), str(self.targets)],
                                capture_output=True, text=True)
        self.assertEqual(0, verify.returncode, verify.stderr)
        self.assertIn("SOURCE_CONVERGENCE_GATE\tPASS\t", verify.stdout)


if __name__ == "__main__":
    unittest.main()
