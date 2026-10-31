# SPDX-License-Identifier: Apache-2.0
"""Actual JCC packet proof using the retained sealed installer, with isolated destinations.

Run from the repository test path, or set M3_JCC_CANDIDATE_ROOT for a staged candidate.
Passing this tooling test grants no source-export or JDK/product acceptance.
"""
from __future__ import annotations

import importlib.util
import os
from pathlib import Path
import sys
import tempfile
import unittest


CRATE = Path(
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
    "jdk21-hash-pinned-text/jcc-handoff-20261005"
)
REGISTRY = "m3/docs/name-mapping.json"
OUTPUTS = {
    REGISTRY,
    "m3/docs/jcc-source-handoff.md",
    "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json",
    "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv",
}
GUARDS = {
    "m3/docs/name-mapping.schema.json",
    "m3/migration/migration.py",
    "m3/migration/recipe.py",
    "m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/"
    "M3Jdk21HashPinnedTextSnapshotRecipe.java",
    "m3/tooling/migration-recipes/pom.xml",
    "src/java.base/share/classes/jdk/internal/mindex/M3Descriptor.java",
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
    "jdk21-hash-pinned/jdk22-descriptor/manifest.tsv",
}
UNRELATED = "unrelated/preserve.bin"
BINARY = b"JCC unrelated binary\x00\xff\n" + bytes(range(256))


class JccHandoffPacketTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        override = os.environ.get("M3_JCC_CANDIDATE_ROOT")
        cls.candidate = Path(override).resolve() if override else Path(__file__).resolve().parents[3]
        owner = cls.candidate / "m3/migration/recipe.py"
        # The supplied candidate is read-only, including Python's import cache behavior.
        sys.dont_write_bytecode = True
        spec = importlib.util.spec_from_file_location("jcc_retained_sealed_installer", owner)
        if spec is None or spec.loader is None:
            raise AssertionError("Cannot load retained installer: " + str(owner))
        cls.recipe = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.recipe)
        source_plan = cls.candidate / CRATE / "plan.json"
        cls.plan, cls.rows = cls.recipe.sealed_plan(source_plan)
        if cls.plan["recipe_id"] != "jcc-handoff-20261005/2":
            raise AssertionError("Wrong actual packet recipe id")
        if len(cls.rows) != 4 or {row["path"] for row in cls.rows} != OUTPUTS:
            raise AssertionError("The actual packet must retain its exact four-output scope")
        guards = cls.plan.get("guards", [])
        if len(guards) != 7 or {guard["path"] for guard in guards} != GUARDS:
            raise AssertionError("The actual packet must retain its exact seven dependency guards")
        if [row["path"] for row in cls.rows if row["before"] is not None] != [REGISTRY]:
            raise AssertionError("The registry is the sole required preimage")
        cls.frozen_inputs = {source_plan: source_plan.read_bytes(), owner: owner.read_bytes()}
        cls.guard_images = {}
        for guard in guards:
            path = cls.candidate / guard["path"]
            content = path.read_bytes()
            if cls.recipe.digest(content) != guard["sha256"]:
                raise AssertionError("Actual candidate guard drift: " + guard["path"])
            cls.guard_images[guard["path"]] = content
            cls.frozen_inputs[path] = content
        cls.packet_temp = tempfile.TemporaryDirectory(prefix="jcc-packet-resources-")
        cls.addClassCleanup(cls.packet_temp.cleanup)
        cls.packet = Path(cls.packet_temp.name)
        (cls.packet / "plan.json").write_bytes(cls.frozen_inputs[source_plan])
        for entry in cls.plan["outputs"]:
            for state in ("before", "after"):
                image = entry[state]
                if image is not None:
                    source = source_plan.parent / image["resource"]
                    content = source.read_bytes()
                    cls.frozen_inputs[source] = content
                    (cls.packet / image["resource"]).write_bytes(content)
        cls.plan_path = cls.packet / "plan.json"
        copied_plan, copied_rows = cls.recipe.sealed_plan(cls.plan_path)
        if copied_plan != cls.plan or copied_rows != cls.rows:
            raise AssertionError("Exact packet copy changed its sealed contents")
        manifest = source_plan.parent / "manifest.tsv"
        cls.frozen_inputs[manifest] = manifest.read_bytes()
        manifest_rows = [line.split("\t") for line in manifest.read_text(encoding="utf-8").splitlines()
                         if line and not line.startswith("#")]
        expected = [[entry["path"],
                     entry["before"]["sha256"] if entry["before"] is not None else "ABSENT",
                     entry["after"]["sha256"], entry["after"]["resource"]]
                    for entry in cls.plan["outputs"]]
        if manifest_rows != expected:
            raise AssertionError("Actual OpenRewrite manifest and sealed installer outputs disagree")

    @classmethod
    def tearDownClass(cls):
        for path, content in cls.frozen_inputs.items():
            if path.read_bytes() != content:
                raise AssertionError("Supplied candidate input changed during packet tests: " + str(path))

    def workspace(self):
        temporary = tempfile.TemporaryDirectory(prefix="jcc-packet-workspace-")
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        for relative, content in self.guard_images.items():
            self.write(root, relative, content)
        for row in self.rows:
            if row["before"] is not None:
                self.write(root, row["path"], row["before"])
        self.write(root, UNRELATED, BINARY)
        return root

    @staticmethod
    def write(root, relative, content):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)

    @staticmethod
    def snapshot(root):
        result = {}
        for path in root.rglob("*"):
            key = path.relative_to(root).as_posix()
            mode = path.stat().st_mode & 0o777
            result[key] = ("directory", mode) if path.is_dir() else ("file", mode, path.read_bytes())
        return result

    def assert_state(self, root, state):
        expected_paths = set(GUARDS) | {UNRELATED}
        for row in self.rows:
            path = root / row["path"]
            if row[state] is None:
                self.assertFalse(path.exists(), row["path"])
            else:
                self.assertEqual(row[state], path.read_bytes(), row["path"])
                expected_paths.add(row["path"])
        for path, content in self.guard_images.items():
            self.assertEqual(content, (root / path).read_bytes(), path)
        self.assertEqual(BINARY, (root / UNRELATED).read_bytes())
        self.assertEqual(expected_paths, {p.relative_to(root).as_posix()
                                          for p in root.rglob("*") if p.is_file()})
        self.assertFalse((root / self.recipe.LOCK).exists())
        self.assertFalse((root / self.recipe.JOURNAL).exists())

    def assert_refuses_without_writes(self, root, reason):
        for mode in ("check", "apply", "rollback"):
            before = self.snapshot(root)
            with self.assertRaises(self.recipe.Refusal) as failure:
                self.recipe.execute(self.plan_path, root, mode)
            self.assertIn(reason, str(failure.exception), mode)
            self.assertEqual(before, self.snapshot(root), mode + " must leave files, modes and directories exact")
            self.assertEqual(BINARY, (root / UNRELATED).read_bytes())

    def test_actual_packet_check_apply_idempotent_rollback_and_replay(self):
        root = self.workspace()
        before = self.snapshot(root)
        checked = self.recipe.execute(self.plan_path, root, "check")
        self.assertEqual(self.plan["plan_sha256"], checked["plan_sha256"])
        self.assertEqual(("before", 4, 0), (checked["state"], checked["files"], checked["writes"]))
        self.assertEqual(before, self.snapshot(root))
        for mode, state, writes in (("rollback", "before", 0), ("apply", "after", 4),
                                    ("apply", "after", 0), ("check", "after", 0),
                                    ("rollback", "before", 4), ("rollback", "before", 0),
                                    ("apply", "after", 4)):
            with self.subTest(mode=mode, state=state, writes=writes):
                result = self.recipe.execute(self.plan_path, root, mode)
                self.assertEqual(self.plan["plan_sha256"], result["plan_sha256"])
                self.assertEqual((state, 4, writes), (result["state"], result["files"], result["writes"]))
                self.assert_state(root, state)

    def test_each_actual_guard_missing_or_drifted_refuses_atomically(self):
        for guard in self.plan["guards"]:
            for missing in (True, False):
                with self.subTest(guard=guard["path"], missing=missing):
                    root = self.workspace()
                    path = root / guard["path"]
                    path.unlink() if missing else path.write_bytes(path.read_bytes() + b"\nforeign guard edit\n")
                    self.assert_refuses_without_writes(root, "dependency guard drift: " + guard["path"])

    def test_actual_required_preimage_missing_or_drifted_refuses_atomically(self):
        for missing in (True, False):
            with self.subTest(missing=missing):
                root = self.workspace()
                path = root / REGISTRY
                path.unlink() if missing else path.write_bytes(b"foreign registry preimage\n")
                self.assert_refuses_without_writes(root, "destination drift: " + REGISTRY)

    def test_each_actual_absent_output_occupied_refuses_atomically(self):
        for row in self.rows:
            if row["before"] is None:
                with self.subTest(path=row["path"]):
                    root = self.workspace()
                    self.write(root, row["path"], b"occupied outside this plan\x00\xff")
                    self.assert_refuses_without_writes(root, "destination drift: " + row["path"])

    def test_each_single_postimage_mixed_with_preimages_refuses_atomically(self):
        for row in self.rows:
            with self.subTest(path=row["path"]):
                root = self.workspace()
                self.write(root, row["path"], row["after"])
                self.assert_refuses_without_writes(root, "mixed before/after state")

    def test_each_foreign_postimage_edit_refuses_rollback_and_replay_atomically(self):
        for row in self.rows:
            with self.subTest(path=row["path"]):
                root = self.workspace()
                self.recipe.execute(self.plan_path, root, "apply")
                self.assert_state(root, "after")
                self.write(root, row["path"], row["after"] + b"\nforeign postimage edit\n")
                self.assert_refuses_without_writes(root, "destination drift: " + row["path"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
