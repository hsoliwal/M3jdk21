#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def load():
    path = Path(__file__).with_name("commit_patch_atoms.py")
    spec = importlib.util.spec_from_file_location("m3_commit_patch_atoms", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class CommitPatchAtomsTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load()

    @staticmethod
    def git(repo: Path, *args: str) -> str:
        return subprocess.check_output(("git", "-C", str(repo), *args), text=True).strip()

    @staticmethod
    def write(root: Path, path: str, text: str) -> None:
        target = root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")

    def test_exact_context_add_delete_and_conflict_are_typed_without_target_mutation(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            donor = root / "donor"
            target = root / "target"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(donor)), check=True)
            self.git(donor, "config", "user.email", "fixture@example.invalid")
            self.git(donor, "config", "user.name", "Fixture")

            self.write(donor, "src/A.java", "class A { int v=1; }\n")
            self.write(donor, "src/Delete.java", "class Delete {}\n")
            self.git(donor, "add", ".")
            self.git(donor, "commit", "-q", "-m", "parent")
            parent = self.git(donor, "rev-parse", "HEAD")

            self.write(donor, "src/A.java", "class A { int v=2; }\n")
            self.write(donor, "src/New.java", "class New {}\n")
            (donor / "src/Delete.java").unlink()
            self.git(donor, "add", "-A")
            self.git(donor, "commit", "-q", "-m", "change")
            commit = self.git(donor, "rev-parse", "HEAD")

            self.write(target, "src/A.java", "class A { int v=1; }\n")
            self.write(target, "src/Delete.java", "class Delete {}\n")
            before = (target / "src/A.java").read_bytes()

            rows = self.mod.materialize(
                donor,
                target,
                parent,
                commit,
                ["src/A.java", "src/Delete.java", "src/New.java"],
                out,
            )
            by_path = {row["path"]: row for row in rows}
            self.assertEqual("EXACT_PARENT_REPLAY", by_path["src/A.java"]["disposition"])
            self.assertEqual("REVIEW_DELETE_EXACT", by_path["src/Delete.java"]["disposition"])
            self.assertEqual("ADD_ABSENT_TARGET", by_path["src/New.java"]["disposition"])
            self.assertEqual(before, (target / "src/A.java").read_bytes())
            self.assertTrue((out / "candidates/src/A.java").is_file())
            self.assertTrue((out / "candidates/src/New.java").is_file())
            self.assertFalse((out / "candidates/src/Delete.java").exists())

            # The exact same recipe run is an idempotent receipt replay.
            self.assertEqual(
                rows,
                self.mod.materialize(
                    donor, target, parent, commit,
                    ["src/A.java", "src/Delete.java", "src/New.java"], out,
                ),
            )
            receipt = out / "APPLICABILITY.tsv"
            receipt.write_text("drifted receipt\\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.mod.materialize(
                    donor, target, parent, commit,
                    ["src/A.java", "src/Delete.java", "src/New.java"], out,
                )
            self.assertEqual(before, (target / "src/A.java").read_bytes())

    def test_context_patch_can_apply_to_nonidentical_target_and_conflicts_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            donor = root / "donor"
            target = root / "target"
            subprocess.run(("git", "init", "-q", str(donor)), check=True)
            self.git(donor, "config", "user.email", "fixture@example.invalid")
            self.git(donor, "config", "user.name", "Fixture")
            self.write(donor, "src/A.java", "class A {\n  int v=1;\n}\n")
            self.git(donor, "add", ".")
            self.git(donor, "commit", "-q", "-m", "parent")
            parent = self.git(donor, "rev-parse", "HEAD")
            self.write(donor, "src/A.java", "class A {\n  int v=2;\n}\n")
            self.git(donor, "add", ".")
            self.git(donor, "commit", "-q", "-m", "change")
            commit = self.git(donor, "rev-parse", "HEAD")

            self.write(target, "src/A.java", "// retained target header\nclass A {\n  int v=1;\n}\n")
            unchanged_target = (target / "src/A.java").read_bytes()
            exact_patch = self.mod.patch_bytes(
                donor, parent, commit, self.mod.changes(donor, parent, commit)["src/A.java"]
            )
            # Strict Git patching does not apply across an extra target header.
            # A fallback may only use the real upstream parent, never guess.
            self.assertIsNone(self.mod.apply_patch(
                unchanged_target, "src/A.java", exact_patch))
            self.assertIsNone(self.mod.apply_patch(
                unchanged_target, "src/A.java", exact_patch, b"wrong donor parent\n"))
            rows = self.mod.materialize(donor, target, parent, commit, ["src/A.java"], root / "ok")
            self.assertEqual("CONTEXT_PATCH_APPLIED", rows[0]["disposition"])
            merged = (root / "ok/candidates/src/A.java").read_text()
            self.assertIn("// retained target header", merged)
            self.assertIn("int v=2;", merged)
            self.assertEqual(unchanged_target, (target / "src/A.java").read_bytes())

            self.write(target, "src/A.java", "class A { int entirelyDifferent=7; }\n")
            rows = self.mod.materialize(donor, target, parent, commit, ["src/A.java"], root / "bad")
            self.assertEqual("CONFLICT_CONTEXT", rows[0]["disposition"])
            self.assertFalse((root / "bad/candidates/src/A.java").exists())

    def test_nul_delimited_git_status_refuses_newline_path(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp) / "donor"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")
            self.write(repo, "src/A.java", "class A {}\\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "parent")
            parent = self.git(repo, "rev-parse", "HEAD")
            self.write(repo, "src/evil\nname.java", "class Evil {}\\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "change")
            commit = self.git(repo, "rev-parse", "HEAD")
            with self.assertRaises(ValueError):
                self.mod.changes(repo, parent, commit)

    def test_output_symlink_does_not_write_through_to_target(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            donor, target, out = root / "donor", root / "target", root / "out"
            subprocess.run(("git", "init", "-q", str(donor)), check=True)
            self.git(donor, "config", "user.email", "fixture@example.invalid")
            self.git(donor, "config", "user.name", "Fixture")
            self.write(donor, "src/A.java", "class A {}\\n")
            self.git(donor, "add", ".")
            self.git(donor, "commit", "-q", "-m", "parent")
            parent = self.git(donor, "rev-parse", "HEAD")
            self.write(donor, "src/New.java", "class New {}\\n")
            self.git(donor, "add", ".")
            self.git(donor, "commit", "-q", "-m", "change")
            commit = self.git(donor, "rev-parse", "HEAD")
            out.mkdir()
            target.mkdir()
            (out / "patches").symlink_to(target)
            with self.assertRaises(ValueError):
                self.mod.materialize(
                    donor, target, parent, commit, ["src/New.java"], out,
                )
            self.assertEqual([], list(target.iterdir()))

            # The direct CLI may not normalize away an untrusted output symlink.
            alias = root / "output-alias"
            alias.symlink_to(out)
            selected = root / "selected.txt"
            selected.write_text("src/New.java\n", encoding="utf-8")
            result = subprocess.run(
                (
                    sys.executable,
                    str(Path(__file__).with_name("commit_patch_atoms.py")),
                    "--donor-repo", str(donor),
                    "--target-root", str(target),
                    "--parent", parent,
                    "--commit", commit,
                    "--paths", str(selected),
                    "--out", str(alias),
                ),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=False,
            )
            self.assertNotEqual(0, result.returncode)
            self.assertIn("symlink", result.stderr)
            self.assertEqual([], list(target.iterdir()))

    def test_missing_selected_path_is_rejected(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp) / "repo"
            target = Path(temp) / "target"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")
            self.write(repo, "A.txt", "a\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "one")
            parent = self.git(repo, "rev-parse", "HEAD")
            self.write(repo, "A.txt", "b\n")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "two")
            commit = self.git(repo, "rev-parse", "HEAD")
            with self.assertRaises(ValueError):
                self.mod.materialize(
                    repo, target, parent, commit, ["missing.txt"], Path(temp) / "out"
                )


if __name__ == "__main__":
    unittest.main()
