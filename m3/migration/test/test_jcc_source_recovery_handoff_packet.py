# SPDX-License-Identifier: Apache-2.0
"""Prepared actual-plan proof for an additive, four-existing-input JCC successor.

Requires real recovery-source resources. Missing/unbound resources fail; none are generated.
Use M3_JCC_CANDIDATE_ROOT for staging, or install under m3/migration/test/.
Passing this tool-plane replay test grants no source export, JNI or JDK acceptance.
"""
from __future__ import annotations

import importlib.util
import json
import os
from pathlib import Path
import re
import sys
import tempfile
import unittest


RESOURCE_ROOT = Path(
    "m3/tooling/migration-recipes/src/main/resources/com/m3/rewrite/backport/"
    "jdk21-hash-pinned-text"
)
CRATE = RESOURCE_ROOT / "jcc-source-recovery-handoff-20261005"
PREVIOUS_CRATE = RESOURCE_ROOT / "jcc-source-final-handoff-20261005"
RECIPE_ID = "jcc-source-recovery-handoff-20261005/1"
PREVIOUS_RECIPE_ID = "jcc-source-final-handoff-20261005/1"
PREVIOUS_SEAL = "af65912789780e214f09644b38f542db5f7670b83ee1fada228bce08f8f9b30e"
PREVIOUS_SOURCE = "d1cf2d81ee74a4a837f78e0cd2e4ccae2b3d4906"
RECOVERY_BASELINE = "be92c62ece9023b5c33676716a1076d00e26120a"
DESTINATION_PREIMAGE_COMMIT = "0994ecd65e86600f417f4d8702838d8c2663af61"
REGISTRY = "m3/docs/name-mapping.json"
BINDINGS = "m3/migration/evidence/jcc-handoff-20261005/source-destination-bindings.json"
IMAGES = {
    REGISTRY: ("before00-name-mapping.json.txt", "after00-name-mapping.json.txt"),
    "m3/docs/jcc-source-handoff.md":
        ("before01-jcc-source-handoff.md.txt", "after01-jcc-source-handoff.md.txt"),
    BINDINGS:
        ("before02-source-destination-bindings.json.txt", "after02-source-destination-bindings.json.txt"),
    "m3/migration/evidence/jcc-handoff-20261005/root-coverage-obligations.tsv":
        ("before03-root-coverage-obligations.tsv.txt", "after03-root-coverage-obligations.tsv.txt"),
}
OUTPUT_MODES = dict(zip(sorted(IMAGES), (0o640, 0o600, 0o750, 0o644)))
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
JCC_IDS = {"synexia.jcc-recipe-laboratory", "synexia.jcc-java-jni-regression"}
UNRELATED = "unrelated/preserve.bin"
BINARY = b"JCC successor unrelated binary\x00\xff\n" + bytes(range(256))


class JccSourceRecoveryHandoffPacketTests(unittest.TestCase):
    @classmethod
    def require_file(cls, path, description):
        if not path.is_file() or path.is_symlink():
            raise AssertionError(
                "Required real " + description + " is absent or unsafe: " + str(path)
                + "; final binding is incomplete, not a skipped or passing proof"
            )
        content = path.read_bytes()
        cls.frozen_inputs[path] = content
        return content

    @classmethod
    def setUpClass(cls):
        override = os.environ.get("M3_JCC_CANDIDATE_ROOT")
        cls.candidate = Path(override).resolve() if override else Path(__file__).resolve().parents[3]
        cls.frozen_inputs = {}
        owner = cls.candidate / "m3/migration/recipe.py"
        cls.require_file(owner, "retained sealed installer")
        sys.dont_write_bytecode = True
        spec = importlib.util.spec_from_file_location("jcc_source_recovery_retained_installer", owner)
        if spec is None or spec.loader is None:
            raise AssertionError("Cannot load retained installer: " + str(owner))
        cls.recipe = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(cls.recipe)

        prior_path = cls.candidate / PREVIOUS_CRATE / "plan.json"
        cls.require_file(prior_path, "preserved published E3 plan")
        previous_plan, previous_rows = cls.recipe.sealed_plan(prior_path)
        if (previous_plan["recipe_id"] != PREVIOUS_RECIPE_ID
                or previous_plan["plan_sha256"] != PREVIOUS_SEAL):
            raise AssertionError("The published E3 plan must remain byte-bound to its original seal")
        if len(previous_rows) != 4 or {row["path"] for row in previous_rows} != set(IMAGES):
            raise AssertionError("Preserved E3 output scope changed")
        previous_after = {row["path"]: row["after"] for row in previous_rows}
        for entry in previous_plan["outputs"]:
            for state in ("before", "after"):
                if entry[state] is not None:
                    cls.require_file(prior_path.parent / entry[state]["resource"], "preserved E3 image")

        source_plan = cls.candidate / CRATE / "plan.json"
        cls.require_file(source_plan, "recovery-source successor plan")
        manifest = source_plan.parent / "manifest.tsv"
        manifest_bytes = cls.require_file(manifest, "recovery-source successor manifest")
        for names in IMAGES.values():
            for name in names:
                cls.require_file(source_plan.parent / name, "recovery-source successor image")
        cls.plan, cls.rows = cls.recipe.sealed_plan(source_plan)
        if cls.plan["recipe_id"] != RECIPE_ID:
            raise AssertionError("Wrong actual successor recipe id")
        source_commit = cls.plan.get("source_commit")
        if (not isinstance(source_commit, str) or not re.fullmatch(r"[0-9a-f]{40}", source_commit)
                or source_commit in (PREVIOUS_SOURCE, RECOVERY_BASELINE, "0" * 40)):
            raise AssertionError("Successor source_commit is not concretely bound to a new source epoch")
        if cls.plan.get("target_commit") != DESTINATION_PREIMAGE_COMMIT:
            raise AssertionError("Successor target_commit must bind the actual published E3 preimages")
        if len(cls.rows) != 4 or {row["path"] for row in cls.rows} != set(IMAGES):
            raise AssertionError("The successor must retain exactly four existing operational outputs")
        if [row["path"] for row in cls.rows] != sorted(IMAGES):
            raise AssertionError("Successor output order must match the sorted text manifest")
        for entry, row in zip(cls.plan["outputs"], cls.rows):
            if entry.get("before") is None or entry.get("after") is None:
                raise AssertionError("All four successor targets require existing before and after images")
            if (entry["before"]["resource"], entry["after"]["resource"]) != IMAGES[row["path"]]:
                raise AssertionError("Wrong successor image resource names: " + row["path"])
            if row["before"] != previous_after[row["path"]]:
                raise AssertionError("Successor preimage differs from published E3 output: " + row["path"])

        guards = cls.plan.get("guards", [])
        if len(guards) != 7 or {guard["path"] for guard in guards} != GUARDS:
            raise AssertionError("The successor must retain the exact seven dependency guards")
        if guards != previous_plan.get("guards"):
            raise AssertionError("Published E3 guard objects changed")
        cls.guard_images = {}
        for guard in guards:
            content = cls.require_file(cls.candidate / guard["path"], "unchanged E3 guard")
            if cls.recipe.digest(content) != guard["sha256"]:
                raise AssertionError("Actual candidate guard drift: " + guard["path"])
            cls.guard_images[guard["path"]] = content

        manifest_rows = [line.split("\t") for line in manifest_bytes.decode("utf-8").splitlines()
                         if line and not line.startswith("#")]
        expected = [[entry["path"], entry["before"]["sha256"],
                     entry["after"]["sha256"], entry["after"]["resource"]]
                    for entry in cls.plan["outputs"]]
        if manifest_rows != expected:
            raise AssertionError("Actual manifest must bind all four existing-input plan rows; ABSENT is invalid")

        after_images = {row["path"]: row["after"] for row in cls.rows}
        bindings = json.loads(after_images[BINDINGS])
        acceptance = bindings.get("acceptance", {})
        expected_acceptance = {key: False for key in (
            "source_export_admitted", "destination_materialized",
            "destination_gates_passed", "remote_read_back_delivered")}
        if (not isinstance(acceptance, dict) or set(acceptance) != set(expected_acceptance)
                or any(type(value) is not bool or value for value in acceptance.values())
                or bindings.get("source_commit") != source_commit):
            raise AssertionError("Successor binding must preserve blocked acceptance and its exact source epoch")
        records = json.loads(after_images[REGISTRY])["migration"]["records"]
        jcc = [row for row in records if row["id"] in JCC_IDS]
        if len(jcc) != 2 or {row["id"] for row in jcc} != JCC_IDS:
            raise AssertionError("Both existing JCC capability records must remain present")
        if any(row["status"] != "blocked" or row["tests"] != [] for row in jcc):
            raise AssertionError("Metadata rebinding must not promote source/receiving capability acceptance")

        temporary = tempfile.TemporaryDirectory(prefix="jcc-source-recovery-packet-")
        cls.addClassCleanup(temporary.cleanup)
        cls.packet = Path(temporary.name)
        (cls.packet / "plan.json").write_bytes(cls.frozen_inputs[source_plan])
        for names in IMAGES.values():
            for name in names:
                (cls.packet / name).write_bytes(cls.frozen_inputs[source_plan.parent / name])
        cls.plan_path = cls.packet / "plan.json"
        copied_plan, copied_rows = cls.recipe.sealed_plan(cls.plan_path)
        if copied_plan != cls.plan or copied_rows != cls.rows:
            raise AssertionError("Exact successor packet copy changed its sealed contents")

    @classmethod
    def tearDownClass(cls):
        for path, content in cls.frozen_inputs.items():
            if path.read_bytes() != content:
                raise AssertionError("Supplied packet input changed during tests: " + str(path))

    def workspace(self):
        temporary = tempfile.TemporaryDirectory(prefix="jcc-source-recovery-workspace-")
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        for path, content in self.guard_images.items():
            self.write(root, path, content)
            (root / path).chmod(0o640)
        for row in self.rows:
            self.write(root, row["path"], row["before"])
            (root / row["path"]).chmod(OUTPUT_MODES[row["path"]])
        self.write(root, UNRELATED, BINARY)
        (root / UNRELATED).chmod(0o600)
        return root

    @staticmethod
    def write(root, relative, content):
        path = root / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(content)

    @staticmethod
    def foreign_image(row, state):
        content = row[state] + b"\nforeign packet-contract edit\n"
        # Future real images are unknown during preparation. Never mistake the other
        # admitted image for a deliberately foreign negative control.
        if content in (row["before"], row["after"]):
            content += b"!"
        return content

    @staticmethod
    def snapshot(root):
        return {path.relative_to(root).as_posix():
                (("directory", path.stat().st_mode & 0o777) if path.is_dir()
                 else ("file", path.stat().st_mode & 0o777, path.read_bytes()))
                for path in root.rglob("*")}

    def assert_state(self, root, state):
        for row in self.rows:
            self.assertEqual(row[state], (root / row["path"]).read_bytes(), row["path"])
            self.assertEqual(OUTPUT_MODES[row["path"]], (root / row["path"]).stat().st_mode & 0o777)
        for path, content in self.guard_images.items():
            self.assertEqual(content, (root / path).read_bytes(), path)
            self.assertEqual(0o640, (root / path).stat().st_mode & 0o777)
        self.assertEqual(BINARY, (root / UNRELATED).read_bytes())
        self.assertEqual(0o600, (root / UNRELATED).stat().st_mode & 0o777)
        self.assertEqual(set(IMAGES) | GUARDS | {UNRELATED},
                         {p.relative_to(root).as_posix() for p in root.rglob("*") if p.is_file()})
        self.assertFalse((root / self.recipe.LOCK).exists())
        self.assertFalse((root / self.recipe.JOURNAL).exists())

    def assert_refuses_without_writes(self, root, reason):
        for mode in ("check", "apply", "rollback"):
            before = self.snapshot(root)
            with self.assertRaises(self.recipe.Refusal) as failure:
                self.recipe.execute(self.plan_path, root, mode)
            self.assertIn(reason, str(failure.exception), mode)
            self.assertEqual(before, self.snapshot(root), mode + " changed the refused workspace")
            self.assertEqual(BINARY, (root / UNRELATED).read_bytes())
            self.assertEqual(0o600, (root / UNRELATED).stat().st_mode & 0o777)

    def test_actual_existing_input_check_apply_fixed_point_rollback_and_replay(self):
        root = self.workspace()
        original = self.snapshot(root)
        for mode, state, writes in (("check", "before", 0), ("rollback", "before", 0),
                                    ("apply", "after", 4), ("check", "after", 0),
                                    ("apply", "after", 0), ("rollback", "before", 4),
                                    ("rollback", "before", 0), ("apply", "after", 4)):
            with self.subTest(mode=mode, state=state, writes=writes):
                before = self.snapshot(root)
                result = self.recipe.execute(self.plan_path, root, mode)
                self.assertEqual(self.plan["plan_sha256"], result["plan_sha256"])
                self.assertEqual((state, 4, writes), (result["state"], result["files"], result["writes"]))
                self.assert_state(root, state)
                if writes == 0:
                    self.assertEqual(before, self.snapshot(root), "Fixed point must preserve exact bytes and modes")
                if state == "before":
                    self.assertEqual(original, self.snapshot(root), "Rollback must restore all four exact preimages")

    def test_each_of_four_required_preimages_missing_or_drifted_refuses_atomically(self):
        for row in self.rows:
            for missing in (True, False):
                with self.subTest(path=row["path"], missing=missing):
                    root = self.workspace()
                    path = root / row["path"]
                    if missing:
                        path.unlink()
                    else:
                        path.write_bytes(self.foreign_image(row, "before"))
                    self.assert_refuses_without_writes(root, "destination drift: " + row["path"])

    def test_each_unchanged_guard_missing_or_drifted_refuses_atomically(self):
        for guard in self.plan["guards"]:
            for missing in (True, False):
                with self.subTest(guard=guard["path"], missing=missing):
                    root = self.workspace()
                    path = root / guard["path"]
                    if missing:
                        path.unlink()
                    else:
                        path.write_bytes(path.read_bytes() + b"\nforeign guard edit\n")
                    self.assert_refuses_without_writes(root, "dependency guard drift: " + guard["path"])

    def test_all_four_output_mixed_before_after_subsets_refuse_atomically(self):
        for after_mask in range(1, (1 << len(self.rows)) - 1):
            with self.subTest(after_mask=after_mask):
                root = self.workspace()
                for index, row in enumerate(self.rows):
                    if after_mask & (1 << index):
                        self.write(root, row["path"], row["after"])
                self.assert_refuses_without_writes(root, "mixed before/after state")

    def test_each_foreign_postimage_edit_refuses_rollback_and_replay_atomically(self):
        for row in self.rows:
            with self.subTest(path=row["path"]):
                root = self.workspace()
                self.recipe.execute(self.plan_path, root, "apply")
                self.assert_state(root, "after")
                self.write(root, row["path"], self.foreign_image(row, "after"))
                self.assert_refuses_without_writes(root, "destination drift: " + row["path"])

    def test_each_missing_existing_postimage_refuses_rollback_and_replay_atomically(self):
        for row in self.rows:
            with self.subTest(path=row["path"]):
                root = self.workspace()
                self.recipe.execute(self.plan_path, root, "apply")
                self.assert_state(root, "after")
                (root / row["path"]).unlink()
                self.assert_refuses_without_writes(root, "destination drift: " + row["path"])


if __name__ == "__main__":
    unittest.main(verbosity=2)
