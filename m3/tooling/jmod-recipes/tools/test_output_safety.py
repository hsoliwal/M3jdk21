#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.
"""Executed refusal/no-effect and bounded release-view discriminators."""
import hashlib
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch
import zipfile

from jmod_image import RESERVE, flatten_release, guard_output


class OutputSafetyTest(unittest.TestCase):
    def setUp(self):
        self.owner = Path(tempfile.mkdtemp(prefix="jmod-safety-", dir=tempfile.gettempdir())).resolve()
        if sys.platform == "win32":
            assert self.owner.is_relative_to(Path("K:/codex-work/agent-temp").resolve())

    def snapshot(self):
        return {str(p.relative_to(self.owner)): hashlib.sha256(p.read_bytes()).hexdigest()
                for p in self.owner.rglob("*") if p.is_file()}

    def test_all_cli_owners_preserve_occupied_output_before_inputs_are_read(self):
        out = self.owner / "existing"
        out.mkdir()
        (out / "unique-proof.txt").write_text("keep me", encoding="utf-8")
        before = self.snapshot()
        cases = [("jmod_image.py", ["--modules", "absent", "--m2", "absent"]),
                 ("jmod_pack.py", ["--image-tsv", "absent", "--manifest", "absent", "--layer-root", "absent"]),
                 ("descriptor_gen.py", ["--image-tsv", "absent"])]
        for tool, arguments in cases:
            result = subprocess.run([sys.executable, "-B", str(Path(__file__).with_name(tool)), *arguments,
                                     "--out", str(out), "--output-root", str(self.owner),
                                     "--output-budget-bytes", "65536"], capture_output=True, text=True, timeout=15)
            self.assertNotEqual(0, result.returncode)
            self.assertIn("existing output is preserved", result.stderr)
            self.assertEqual(before, self.snapshot())

    def test_root_source_and_escape_are_refused_without_effects(self):
        for path in [self.owner, self.owner / ".." / "escape", Path("C:/m3-work-c/output")]:
            with self.assertRaises(ValueError):
                guard_output(path, self.owner, 65536)
        source = self.owner / "source"
        source.mkdir()
        (source / ".git").write_text("gitdir: protected", encoding="utf-8")
        before = self.snapshot()
        with self.assertRaises(ValueError):
            guard_output(source / "build", source, 65536)
        self.assertEqual(before, self.snapshot())

    def test_reserve_and_budget_fail_before_creation(self):
        before = self.snapshot()
        with patch("jmod_image.shutil.disk_usage", return_value=shutil._ntuple_diskusage(RESERVE, 0, RESERVE)):
            with self.assertRaises(ValueError):
                guard_output(self.owner / "out", self.owner, 65536)
        for budget in [0, -1, 513 * 1024**2]:
            with self.assertRaises(ValueError):
                guard_output(self.owner / "out", self.owner, budget)
        with self.assertRaises(ValueError):
            guard_output(self.owner / "out", self.owner, 65536, estimated=65537)
        self.assertEqual(before, self.snapshot())

    def test_archive_escape_refuses_even_after_a_valid_entry_without_partial_output(self):
        for i, unsafe in enumerate(["../escape", "C:/escape", "/escape", "a/../../escape", ".git/config"]):
            archive = self.owner / ("bad%d.jar" % i)
            with zipfile.ZipFile(archive, "w") as z:
                z.writestr("safe/A.class", b"safe first")
                z.writestr(unsafe, b"bad later")
            before = self.snapshot()
            with self.assertRaises(ValueError):
                flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
            self.assertEqual(before, self.snapshot())
            self.assertFalse((self.owner / "out").exists())

    def test_archive_size_refuses_before_creation(self):
        archive = self.owner / "large.jar"
        with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr("A.class", b"x" * 65537)
        with self.assertRaises(ValueError):
            flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
        self.assertFalse((self.owner / "out").exists())

    @unittest.skipUnless(sys.platform == "win32", "Windows junction discriminator")
    def test_windows_junction_owner_is_refused_without_writing(self):
        destination = self.owner / "destination"
        destination.mkdir()
        link = self.owner / "junction"
        result = subprocess.run(["cmd.exe", "/c", "mklink", "/J", str(link), str(destination)],
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stdout + result.stderr)
        with self.assertRaises(ValueError):
            guard_output(link / "out", link, 65536)
        self.assertFalse((destination / "out").exists())

    def test_small_files_are_charged_by_destination_allocation_not_payload_bytes(self):
        archive = self.owner / "small-files.jar"
        with zipfile.ZipFile(archive, "w") as z:
            for i in range(1024):
                z.writestr("A%d.class" % i, b"x")
        with self.assertRaises(ValueError):
            flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
        self.assertFalse((self.owner / "out").exists())

    def test_archive_symbolic_link_is_refused_before_any_write(self):
        archive = self.owner / "link.jar"
        info = zipfile.ZipInfo("link")
        info.create_system = 3
        info.external_attr = 0o120777 << 16
        with zipfile.ZipFile(archive, "w") as z:
            z.writestr("safe.class", b"safe")
            z.writestr(info, "../escape")
        with self.assertRaises(ValueError):
            flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
        self.assertFalse((self.owner / "out").exists())

    def test_duplicate_or_file_parent_entries_refuse_before_any_write(self):
        for i, entries in enumerate([[("A.class", b"first"), ("A.class", b"second")],
                                      [("dir", b"file"), ("dir/A.class", b"child")]]):
            archive = self.owner / ("conflict%d.jar" % i)
            with zipfile.ZipFile(archive, "w") as z:
                for name, payload in entries:
                    z.writestr(name, payload)
            before = self.snapshot()
            with self.assertRaises(ValueError):
                flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
            self.assertEqual(before, self.snapshot())
            self.assertFalse((self.owner / "out").exists())

    @unittest.skipUnless(sys.platform == "win32", "Windows policy branch discriminator")
    def test_windows_k_lane_policy_is_not_imposed_on_the_posix_branch(self):
        owner = Path("K:/codex-work").resolve()
        candidate = owner / (self.owner.name + "-not-created")
        with self.assertRaisesRegex(ValueError, "K:/codex-work/agent-temp"):
            guard_output(candidate, owner, 65536)
        with patch("jmod_image.os", SimpleNamespace(name="posix")):
            self.assertEqual(candidate, guard_output(candidate, owner, 65536))
        self.assertFalse(candidate.exists())

    def test_valid_plain_archive_uses_fresh_output_and_keeps_input_bytes(self):
        archive = self.owner / "plain.jar"
        with zipfile.ZipFile(archive, "w") as z:
            z.writestr("demo/A.class", b"small valid payload")
        source_hash = hashlib.sha256(archive.read_bytes()).hexdigest()
        self.assertFalse(flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536))
        self.assertEqual(b"small valid payload", (self.owner / "out/demo/A.class").read_bytes())
        with self.assertRaises(ValueError):
            flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536)
        self.assertEqual(source_hash, hashlib.sha256(archive.read_bytes()).hexdigest())

    def test_release_21_overlay_is_preserved_without_manifest_deletion(self):
        archive = self.owner / "good.jar"
        with zipfile.ZipFile(archive, "w") as z:
            z.writestr("demo/A.class", b"base")
            z.writestr("META-INF/MANIFEST.MF", b"Multi-Release: true\n")
            z.writestr("META-INF/versions/21/demo/A.class", b"release21")
            z.writestr("META-INF/versions/22/demo/A.class", b"release22")
        source_hash = hashlib.sha256(archive.read_bytes()).hexdigest()
        self.assertTrue(flatten_release(archive, self.owner / "out", output_root=self.owner, output_budget_bytes=65536))
        self.assertEqual(b"release21", (self.owner / "out/demo/A.class").read_bytes())
        self.assertFalse((self.owner / "out/META-INF/MANIFEST.MF").exists())
        self.assertEqual(source_hash, hashlib.sha256(archive.read_bytes()).hexdigest())


if __name__ == "__main__":
    unittest.main(verbosity=2)
