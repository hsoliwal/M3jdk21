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
            rows = self.mod.materialize(donor, target, parent, commit, ["src/A.java"], root / "ok")
            self.assertEqual("CONTEXT_PATCH_APPLIED", rows[0]["disposition"])
            self.assertIn("int v=2;", (root / "ok/candidates/src/A.java").read_text())

            self.write(target, "src/A.java", "class A { int entirelyDifferent=7; }\n")
            rows = self.mod.materialize(donor, target, parent, commit, ["src/A.java"], root / "bad")
            self.assertEqual("CONFLICT_CONTEXT", rows[0]["disposition"])
            self.assertFalse((root / "bad/candidates/src/A.java").exists())

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
