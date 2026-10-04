# SPDX-License-Identifier: Apache-2.0
import hashlib
import tempfile
import unittest
from pathlib import Path

import materialize_source_convergence as materializer


def sha(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


class MaterializeSourceConvergenceTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.source = root / "source"
        self.worktree = root / "worktree"
        self.output = root / "evidence"
        self.path = "src/java.base/share/classes/example/A.java"
        self.stable_path = "src/java.base/share/classes/example/B.java"

        before = b"package example; final class A {}\n"
        after = b"package example; final class A { /* M3 */ }\n"
        stable = b"package example; final class B {}\n"
        for tree in (self.source, self.worktree):
            (tree / self.path).parent.mkdir(parents=True)
            (tree / self.path).write_bytes(before)
            (tree / self.stable_path).write_bytes(stable)

        candidate = self.output / f"candidates/{self.path}"
        candidate.parent.mkdir(parents=True)
        candidate.write_bytes(after)
        self.manifest = self.output / "SOURCE_CONVERGENCE.tsv"
        self.manifest.write_text(
            "path\tpreSha256\tpostSha256\tatomizationChanged\t"
            "patternizationChanged\tdocumentationChanged\tfixedPoint\t"
            "status\tmessage\tcandidate\n"
            f"{self.path}\t{sha(before)}\t{sha(after)}\ttrue\ttrue\ttrue\t"
            f"true\tCONVERGED_CHANGED\t\tcandidates/{self.path}\n"
            f"{self.stable_path}\t{sha(stable)}\t{sha(stable)}\tfalse\tfalse\tfalse\t"
            "true\tCONVERGED_UNCHANGED\t\t\n",
            encoding="utf-8",
        )
        self.before = before
        self.after = after
        self.stable = stable

    def tearDown(self) -> None:
        self.temp.cleanup()

    def test_overlay_changes_only_detached_worktree_and_writes_root(self) -> None:
        root = materializer.materialize(self.source, self.manifest, self.worktree)

        self.assertRegex(root, r"^[0-9a-f]{64}$")
        self.assertEqual(self.before, (self.source / self.path).read_bytes())
        self.assertEqual(self.after, (self.worktree / self.path).read_bytes())
        self.assertEqual(self.stable, (self.worktree / self.stable_path).read_bytes())
        self.assertEqual(
            root,
            (self.worktree / "m3-normalized-baseline.root")
            .read_text(encoding="utf-8")
            .strip(),
        )
        receipt = (self.worktree / "m3-normalized-baseline.tsv").read_text(encoding="utf-8")
        self.assertIn(self.path, receipt)
        self.assertIn(sha(self.after), receipt)

    def test_canonical_drift_fails(self) -> None:
        (self.source / self.path).write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "canonical preimage drift"):
            materializer.materialize(self.source, self.manifest, self.worktree)

    def test_worktree_drift_fails(self) -> None:
        (self.worktree / self.path).write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "worktree preimage drift"):
            materializer.materialize(self.source, self.manifest, self.worktree)

    def test_candidate_drift_fails(self) -> None:
        candidate = self.output / f"candidates/{self.path}"
        candidate.write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "candidate postimage drift"):
            materializer.materialize(self.source, self.manifest, self.worktree)

    def test_hold_row_fails_closed(self) -> None:
        text = self.manifest.read_text(encoding="utf-8")
        self.manifest.write_text(
            text.replace(
                "true\tCONVERGED_CHANGED",
                "false\tHOLD",
                1,
            ),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "unresolved convergence row"):
            materializer.materialize(self.source, self.manifest, self.worktree)

    def _identity(self, role: str, revision: str, semantic_root: str | None = None) -> Path:
        identity = self.output / f"{role.lower()}-{revision.replace('/', '_')}.tsv"
        identity.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"{role}\t{revision}\t{semantic_root or ('1' * 64)}\t2\t1\t0\n",
            encoding="utf-8",
        )
        return identity

    def test_donor_requires_typed_identity_and_uses_separate_receipts(self) -> None:
        with self.assertRaisesRegex(ValueError, "requires --identity"):
            materializer.materialize(
                self.source,
                self.manifest,
                self.worktree,
                image_kind="donor",
            )

        identity = self._identity("DONOR", "jdk-22+36")
        root = materializer.materialize(
            self.source,
            self.manifest,
            self.worktree,
            image_kind="donor",
            identity_path=identity,
        )

        self.assertRegex(root, r"^[0-9a-f]{64}$")
        self.assertFalse((self.worktree / "m3-normalized-baseline.root").exists())
        self.assertEqual(
            root,
            (self.worktree / "m3-normalized-donor.root")
            .read_text(encoding="utf-8")
            .strip(),
        )
        image = (self.worktree / "m3-normalized-donor.image.tsv").read_text(
            encoding="utf-8"
        )
        self.assertIn("DONOR\tjdk-22+36\t", image)
        self.assertIn(root, image)

    def test_donor_revision_and_semantic_root_bind_normalized_identity(self) -> None:
        first_worktree = Path(self.temp.name) / "donor-a"
        second_worktree = Path(self.temp.name) / "donor-b"
        materializer.copy_tree(self.source, first_worktree)
        materializer.copy_tree(self.source, second_worktree)

        first = materializer.materialize(
            self.source,
            self.manifest,
            first_worktree,
            image_kind="donor",
            identity_path=self._identity("DONOR", "jdk-22+36", "2" * 64),
        )
        second = materializer.materialize(
            self.source,
            self.manifest,
            second_worktree,
            image_kind="donor",
            identity_path=self._identity("DONOR", "jdk-23+37", "3" * 64),
        )
        self.assertNotEqual(first, second)

    def test_donor_refuses_baseline_identity(self) -> None:
        with self.assertRaisesRegex(ValueError, "role mismatch"):
            materializer.materialize(
                self.source,
                self.manifest,
                self.worktree,
                image_kind="donor",
                identity_path=self._identity("BASELINE", "jdk-21+35"),
            )

    def test_copy_mode_excludes_git_build_and_target(self) -> None:
        source = Path(self.temp.name) / "copy-source"
        destination = Path(self.temp.name) / "copy-target"
        (source / "src").mkdir(parents=True)
        (source / "src/A.java").write_text("final class A {}\n", encoding="utf-8")
        (source / ".git").mkdir()
        (source / ".git/config").write_text("x", encoding="utf-8")
        (source / "build").mkdir()
        (source / "build/x").write_text("x", encoding="utf-8")
        (source / "target").mkdir()
        (source / "target/x").write_text("x", encoding="utf-8")

        materializer.copy_tree(source, destination)

        self.assertTrue((destination / "src/A.java").is_file())
        self.assertFalse((destination / ".git").exists())
        self.assertFalse((destination / "build").exists())
        self.assertFalse((destination / "target").exists())


if __name__ == "__main__":
    unittest.main()
