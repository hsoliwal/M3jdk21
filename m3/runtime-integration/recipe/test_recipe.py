#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
# SPDX-License-Identifier: Apache-2.0
"""Test finite source custody and historical replay; no runtime/JDK/JNI proof."""
import hashlib
import importlib.util
import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
spec = importlib.util.spec_from_file_location("integration_recipe", HERE / "apply.py")
recipe = importlib.util.module_from_spec(spec)
spec.loader.exec_module(recipe)
manifest = json.loads((HERE / "manifest.json").read_bytes())
cohort_data = (HERE / recipe.CURRENT_COHORT_FILE).read_bytes()
if recipe.digest(cohort_data) != recipe.CURRENT_COHORT_SHA256:
    raise ValueError("current custody table seal drift")
cohort = json.loads(cohort_data)
CURRENT_FILES, AUXILIARIES = recipe.validate_current_cohort(cohort, manifest)
ALL_PATHS = tuple(CURRENT_FILES) + tuple(AUXILIARIES)
MODES = ({}, {"check": True}, {"reverse": True})


def fixture_bytes(row, repository=ROOT):
    """Read one sealed public-SDK fixture, with no fallback or root override."""
    if cohort["testResources"]["sdkRoot"] != recipe.CURRENT_SDK_ROOT:
        raise ValueError("fixture root drift")
    repository = repository.resolve()
    root = repository / recipe.CURRENT_SDK_ROOT
    relative = recipe._relative_path(row["resource"])
    expected = ("src/main/resources/com/synexia/rewrite/"
                "m3-runtime-current-custody-20261017/")
    if not row["resource"].startswith(expected):
        raise ValueError("fixture path drift: " + row["resource"])
    path = root.joinpath(*relative.parts)
    for parent in (path, *path.parents):
        if parent == repository:
            break
        if parent.is_symlink():
            raise ValueError("fixture symlink path: " + row["resource"])
    if root.resolve() not in path.resolve().parents or not path.is_file():
        raise ValueError("fixture missing: " + row["resource"])
    data = path.read_bytes()
    header = b"blob " + str(len(data)).encode("ascii") + b"\0"
    if (len(data) != row["bytes"] or recipe.digest(data) != row["sha256"]
            or hashlib.sha1(header + data).hexdigest() != row["gitBlob"]):
        raise ValueError("fixture source drift: " + row["resource"])
    return data


def write_path(target, name, data):
    path = target / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(data)


def seed_actual_current(target):
    for name, row in (*CURRENT_FILES.items(), *AUXILIARIES.items()):
        data = (ROOT / name).read_bytes()
        if recipe.digest(data) != row["sha256"] or len(data) != row["bytes"]:
            raise ValueError("source fixture baseline drift: " + name)
        write_path(target, name, data)


def seed_legacy_admitted(target):
    for name in manifest["files"]:
        write_path(target, name, (ROOT / name).read_bytes())
    for row in cohort["testResources"]["files"]:
        if row["targetPath"] in recipe.CURRENT_SUCCESSOR_PATHS:
            write_path(target, row["targetPath"], fixture_bytes(row))


def seed_historical_after(target):
    for name in manifest["files"]:
        write_path(target, name, (ROOT / name).read_bytes())
    for row in cohort["testResources"]["files"]:
        if row["role"] == "historical-after":
            write_path(target, row["targetPath"], fixture_bytes(row))


def hash_snapshot(target, names=ALL_PATHS):
    result = {}
    for name in names:
        path = target / name
        if path.is_symlink():
            result[name] = ("SYMLINK", str(path.readlink()))
        elif path.exists():
            result[name] = recipe.digest(path.read_bytes())
        else:
            result[name] = None
    return result


class RecipeTest(unittest.TestCase):
    def seed_current(self, target):
        # This original suite means the admitted historical superseded state.
        # Later six-owner successors are exercised separately below.
        seed_legacy_admitted(target)

    def snapshot(self, target):
        return {
            name: (target / name).read_bytes() if (target / name).exists() else None
            for name in manifest["files"]
        }

    def test_manifest_is_upstream_to_master_v2(self):
        self.assertEqual("M3_RUNTIME_INTEGRATION_RECIPE_V2", manifest["schema"])
        self.assertEqual(manifest["base_commit"], manifest["upstream_commit"])
        self.assertEqual(66, len(manifest["files"]))
        for name, hashes in manifest["files"].items():
            self.assertIsNotNone(hashes["after"], name)
            superseded = hashes.get("superseded", [])
            self.assertEqual(len(superseded), len(set(superseded)), name)
            if hashes["before"] is None:
                self.assertTrue(superseded, name)
            else:
                self.assertNotIn(hashes["before"], superseded, name)
                self.assertNotIn(hashes["after"], superseded, name)

    def test_current_tree_is_exact_superseded_fixed_point(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            before = self.snapshot(target)
            self.assertEqual("superseded", recipe.apply(target, check=True))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("superseded", recipe.apply(target))
            self.assertEqual(before, self.snapshot(target))
            self.assertEqual("superseded", recipe.apply(target, reverse=True))
            self.assertEqual(before, self.snapshot(target))

    def test_every_current_target_matches_after_or_exact_superseded(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            self.assertEqual("superseded", recipe.apply(target, check=True))
            for name, hashes in manifest["files"].items():
                source = target / name
                actual = recipe.digest(source.read_bytes()) if source.exists() else None
                allowed = {hashes["after"], *hashes.get("superseded", [])}
                self.assertIn(actual, allowed, name)
                if hashes["before"] is None:
                    self.assertIn(actual, hashes.get("superseded", []), name)

    def test_patch_bytes_remain_content_addressed(self):
        patch = HERE / "runtime.patch"
        self.assertEqual(manifest["patch_sha256"], recipe.digest(patch.read_bytes()))
        self.assertNotIn(b"\r", patch.read_bytes())

    def test_each_unknown_drift_is_refused_without_partial_write(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            self.assertEqual("superseded", recipe.apply(target, check=True))
            for name in manifest["files"]:
                path = target / name
                original = path.read_bytes() if path.exists() else None
                if original is None:
                    path.parent.mkdir(parents=True, exist_ok=True)
                    path.write_bytes(b"drift")
                else:
                    path.write_bytes(original + b"\n")
                before = self.snapshot(target)
                with self.assertRaisesRegex(ValueError, "source drift") as refused:
                    recipe.apply(target)
                self.assertIn(name, str(refused.exception))
                self.assertEqual(before, self.snapshot(target))
                if original is None:
                    path.unlink()
                else:
                    path.write_bytes(original)

    def test_modified_utf8_owner_fanin_matches_canonical_donor(self):
        expected = {
            "src/hotspot/share/classfile/javaClasses.cpp",
            "src/hotspot/share/classfile/javaClasses.hpp",
            "src/hotspot/share/classfile/javaClasses.inline.hpp",
            "src/java.base/share/classes/java/lang/M3StringFacts.java",
        }
        self.assertEqual(expected, set(recipe.MUTF8_FANIN))
        observed = {}
        for path, (prior, legacy, current) in recipe.MUTF8_FANIN.items():
            row = manifest["files"][path]
            self.assertEqual(prior, row["after"])
            self.assertEqual([*legacy, current], row["superseded"])
            observed[path] = recipe.digest((ROOT / path).read_bytes())
            self.assertEqual(current, observed[path], path)
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, observed))

    def test_modified_utf8_partial_and_unreviewed_sources_are_refused(self):
        current = {path: new for path, (_, _, new) in recipe.MUTF8_FANIN.items()}
        historic = {
            path: legacy[-1] if legacy else prior
            for path, (prior, legacy, _) in recipe.MUTF8_FANIN.items()
        }
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, current))
        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, historic))
        for path in current:
            with self.subTest(path=path, stage="partial"):
                partial = dict(current)
                partial[path] = historic[path]
                with self.assertRaisesRegex(ValueError, "partial modified UTF-8"):
                    recipe.verify_mutf8_fanin(manifest, partial)
            with self.subTest(path=path, stage="unknown"):
                damaged = dict(current)
                damaged[path] = "0" * 64
                with self.assertRaisesRegex(ValueError, "modified UTF-8 source drift"):
                    recipe.verify_mutf8_fanin(manifest, damaged)
            with self.subTest(path=path, stage="receipt"):
                damaged = json.loads(json.dumps(manifest))
                damaged["files"][path]["superseded"].pop()
                with self.assertRaisesRegex(ValueError, "modified UTF-8 recipe seal drift"):
                    recipe.verify_mutf8_fanin(damaged, current)

    def test_symlink_target_is_refused(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            self.seed_current(target)
            self.assertEqual("superseded", recipe.apply(target, check=True))
            name = next(iter(manifest["files"]))
            path = target / name
            path.unlink()
            path.symlink_to(ROOT / name)
            with self.assertRaisesRegex(ValueError, "symlink path"):
                recipe.apply(target)


class CurrentCustodyTest(unittest.TestCase):
    def assert_state(self, target, wanted):
        before = hash_snapshot(target)
        for options in MODES:
            self.assertEqual(wanted, recipe.apply(target, **options))
            self.assertEqual(before, hash_snapshot(target))

    def assert_refused(self, target, text, culprit=None):
        before = hash_snapshot(target)
        for options in MODES:
            with self.assertRaisesRegex(ValueError, re.escape(text)) as refused:
                recipe.apply(target, **options)
            if culprit is not None:
                self.assertIn(culprit, str(refused.exception))
            self.assertEqual(before, hash_snapshot(target))

    def assert_historical_hashes(self, target, state):
        expected = {name: row[state] for name, row in manifest["files"].items()}
        self.assertEqual(expected, hash_snapshot(target, manifest["files"]))

    def test_exact_current_72_all_modes(self):
        self.assertEqual(66, len(CURRENT_FILES))
        self.assertEqual(6, len(AUXILIARIES))
        self.assertEqual(72, len(set(ALL_PATHS)))
        self.assertTrue(cohort["sourceCustodyOnly"])
        for flag in ("runtimeQualified", "jniQualified", "jdkQualified",
                     "performanceQualified", "phaseAdvanced"):
            self.assertIs(False, cohort[flag])
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            self.assert_state(target, "current-custody")

    def test_historical_patch_66_round_trip(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_historical_after(target)
            self.assert_historical_hashes(target, "after")
            self.assertEqual("after", recipe.apply(target, check=True))
            # Real immutable binary patch replay: no subprocess mocking.
            for reverse, expected in ((True, "before"), (False, "after"),
                                      (True, "before"), (False, "after")):
                self.assertEqual(expected, recipe.apply(target, reverse=reverse))
                self.assert_historical_hashes(target, expected)
                self.assertEqual(expected, recipe.apply(target, check=True))
            for name in AUXILIARIES:
                self.assertFalse((target / name).exists(), name)

    def test_all_64_successor_masks(self):
        names = sorted(recipe.CURRENT_SUCCESSOR_PATHS)
        rows = {row["targetPath"]: row for row in cohort["testResources"]["files"]
                if row["role"] == "historical-after"}
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            self.assert_state(target, "current-custody")
            current = {name: (target / name).read_bytes() for name in names}
            historic = {name: fixture_bytes(rows[name]) for name in names}
            for mask in range(64):
                with self.subTest(mask=mask):
                    for bit, name in enumerate(names):
                        write_path(target, name,
                                   current[name] if mask & (1 << bit) else historic[name])
                    if mask == 63:
                        self.assert_state(target, "current-custody")
                    elif mask == 0:
                        self.assert_state(target, "superseded")
                    else:
                        culprit = next(name for name in CURRENT_FILES
                                       if name in names and
                                       recipe.digest((target / name).read_bytes()) !=
                                       CURRENT_FILES[name]["sha256"])
                        self.assert_refused(target, "partial current runtime custody cohort",
                                            culprit)
            for name in names:
                write_path(target, name, current[name])
            self.assert_state(target, "current-custody")

    def test_all_16_modified_utf8_masks(self):
        names = sorted(recipe.MUTF8_FANIN)
        rows = {row["targetPath"]: row for row in cohort["testResources"]["files"]
                if row["role"] == "historical-after"}
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            self.assert_state(target, "current-custody")
            current = {name: (target / name).read_bytes() for name in names}
            historic = {name: fixture_bytes(rows[name]) for name in names}
            for mask in range(16):
                with self.subTest(mask=mask):
                    for bit, name in enumerate(names):
                        write_path(target, name,
                                   current[name] if mask & (1 << bit) else historic[name])
                    observed = {name: recipe.digest((target / name).read_bytes())
                                for name in names}
                    if mask == 15:
                        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, observed))
                        self.assert_state(target, "current-custody")
                    elif mask == 0:
                        self.assertIsNone(recipe.verify_mutf8_fanin(manifest, observed))
                        culprit = next(name for name in CURRENT_FILES if name in names)
                        self.assert_refused(target, "partial current runtime custody cohort",
                                            culprit)
                    else:
                        with self.assertRaisesRegex(ValueError, "partial modified UTF-8"):
                            recipe.verify_mutf8_fanin(manifest, observed)
                        self.assert_refused(target, "partial modified UTF-8")
            for name in names:
                write_path(target, name, current[name])
            self.assert_state(target, "current-custody")

    def test_each_72_drift_missing_symlink_no_partial_writes(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            for name in ALL_PATHS:
                path = target / name
                original = path.read_bytes()
                for mutation in ("drift", "missing", "symlink"):
                    with self.subTest(path=name, mutation=mutation):
                        self.assert_state(target, "current-custody")
                        try:
                            if mutation == "drift":
                                path.write_bytes(original + b"\n")
                            else:
                                path.unlink()
                                if mutation == "symlink":
                                    path.symlink_to(ROOT / name)
                            if mutation == "symlink":
                                expected = "symlink path"
                            elif name in AUXILIARIES:
                                expected = "current custody auxiliary drift"
                            elif mutation == "missing":
                                expected = "current custody missing source"
                            else:
                                expected = "source drift"
                            self.assert_refused(target, expected, name)
                        finally:
                            if path.is_symlink():
                                path.unlink()
                            path.write_bytes(original)
            self.assert_state(target, "current-custody")

    def test_cohort_shape_path_and_hash_refusals(self):
        self.assertEqual((CURRENT_FILES, AUXILIARIES),
                         recipe.validate_current_cohort(cohort, manifest))
        mutations = (
            ("scope", lambda c: c.__setitem__("runtimeQualified", True), "scope drift"),
            ("source-count", lambda c: c["files"].pop(), "source inventory drift"),
            ("duplicate", lambda c: c["files"].__setitem__(1, dict(c["files"][0])),
             "source identity drift"),
            ("traversal", lambda c: c["files"][0].__setitem__("path", "../escape"),
             "unsafe custody path"),
            ("absolute", lambda c: c["files"][0].__setitem__("path", "/escape"),
             "unsafe custody path"),
            ("backslash", lambda c: c["files"][0].__setitem__("path", "src\\escape"),
             "unsafe custody path"),
            ("hash", lambda c: c["files"][0].__setitem__("sha256", "z" * 64),
             "source identity drift"),
            ("git", lambda c: c["files"][0].__setitem__("gitBlob", "z" * 40),
             "source identity drift"),
            ("size", lambda c: c["files"][0].__setitem__("bytes", True),
             "source identity drift"),
            ("successors", lambda c: c["successorPaths"].pop(), "successor set drift"),
            ("auxiliaries", lambda c: c["auxiliaries"].pop(), "auxiliary inventory drift"),
            ("fixture-root", lambda c: c["testResources"].__setitem__("sdkRoot", "other"),
             "public fixture root drift"),
            ("fixture-traversal", lambda c: c["testResources"]["files"][0].__setitem__(
                "resource", "../escape"), "unsafe custody path"),
            ("fixture-duplicate", lambda c: c["testResources"]["files"].__setitem__(
                1, dict(c["testResources"]["files"][0])), "fixture identity drift"),
        )
        for label, mutate, expected in mutations:
            with self.subTest(case=label):
                damaged = json.loads(cohort_data)
                mutate(damaged)
                with self.assertRaisesRegex(ValueError, re.escape(expected)):
                    recipe.validate_current_cohort(damaged, manifest)
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            self.assert_state(target, "current-custody")
            # Redirect only the applier's artifact directory to actual copied bytes.
            control = target / "control"
            control.mkdir()
            for name in ("manifest.json", "runtime.patch", recipe.CURRENT_COHORT_FILE):
                (control / name).write_bytes((HERE / name).read_bytes())
            with patch.object(recipe, "HERE", control):
                self.assert_state(target, "current-custody")
                table = control / recipe.CURRENT_COHORT_FILE
                original = table.read_bytes()
                table.write_bytes(original + b"\n")
                self.assert_refused(target, "current custody table seal drift")
                table.unlink()
                self.assert_refused(target, "current custody table missing or redirected")
                table.symlink_to(HERE / recipe.CURRENT_COHORT_FILE)
                self.assert_refused(target, "current custody table missing or redirected")

    def test_fixture_source_custody_guards(self):
        self.assertEqual(11, len(cohort["testResources"]["files"]))
        self.assertEqual(954180, sum(row["bytes"] for row in
                                     cohort["testResources"]["files"]))
        for row in cohort["testResources"]["files"]:
            data = fixture_bytes(row)
            self.assertEqual(row["sha256"], recipe.digest(data))
        row = cohort["testResources"]["files"][0]
        with tempfile.TemporaryDirectory() as folder:
            repository = Path(folder)
            path = repository / recipe.CURRENT_SDK_ROOT / row["resource"]
            path.parent.mkdir(parents=True)
            path.write_bytes(fixture_bytes(row))
            self.assertEqual(row["bytes"], len(fixture_bytes(row, repository)))
            path.write_bytes(path.read_bytes() + b"\n")
            with self.assertRaisesRegex(ValueError, "fixture source drift"):
                fixture_bytes(row, repository)
            path.unlink()
            with self.assertRaisesRegex(ValueError, "fixture missing"):
                fixture_bytes(row, repository)
            path.symlink_to(ROOT / recipe.CURRENT_SDK_ROOT / row["resource"])
            with self.assertRaisesRegex(ValueError, "fixture symlink path"):
                fixture_bytes(row, repository)
            for resource in ("../escape", "/escape", "src\\escape",
                             "src/test/resources/shadow"):
                damaged = dict(row, resource=resource)
                with self.assertRaisesRegex(ValueError, "unsafe custody path|fixture path drift"):
                    fixture_bytes(damaged, repository)

    def test_legacy_does_not_read_auxiliaries_or_fixtures(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_legacy_admitted(target)
            for name in AUXILIARIES:
                self.assertFalse((target / name).exists(), name)
            original_read = Path.read_bytes

            def guarded_read(path):
                if "src/main/resources/" in path.as_posix():
                    raise AssertionError("runtime CLI attempted to read fixture data")
                if path.as_posix() in {(target / name).as_posix() for name in AUXILIARIES}:
                    raise AssertionError("legacy branch attempted later auxiliary data")
                return original_read(path)

            with patch.object(Path, "read_bytes", guarded_read):
                self.assert_state(target, "superseded")

    def test_cli_does_not_read_fixture_data(self):
        with tempfile.TemporaryDirectory() as folder:
            target = Path(folder)
            seed_actual_current(target)
            self.assert_state(target, "current-custody")
            # This target has no SDK resources. Actual CLI still verifies source72.
            self.assertFalse((target / recipe.CURRENT_SDK_ROOT).exists())
            before = hash_snapshot(target)
            for flags in ([], ["--check"], ["--reverse"]):
                completed = subprocess.run(
                    ["python3", "-I", "-S", "-B", str(HERE / "apply.py"),
                     "--target", str(target), *flags], check=True, text=True,
                    capture_output=True)
                self.assertIn("MINDEX_INTEGRATION_RECIPE_PASS state=current-custody",
                              completed.stdout)
                self.assertIn("scope=SOURCE_ONLY owners=72 runtimeQualified=false",
                              completed.stdout)
                self.assertEqual(before, hash_snapshot(target))


if __name__ == "__main__":
    unittest.main()
