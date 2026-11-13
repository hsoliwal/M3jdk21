#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-20261031/test_verify.py blob b81703e17619; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
"""Small independent custody/refusal tests; real JVM/JNI execution is in verify.py."""
import pathlib
import shutil
import tempfile
import unittest
from unittest.mock import patch

import verify


class GateCustodyTests(unittest.TestCase):
    def test_pinned_manifest_valid_and_complete(self):
        self.assertEqual(set(verify.SOURCES), set(verify.seal_sources()))

    def test_drift_is_rejected_before_native_build_or_runtime(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            (root / "fixtures").mkdir()
            shutil.copy2(verify.HERE / "SOURCE_SHA256.tsv", root)
            for name in verify.SOURCES:
                shutil.copy2(verify.HERE / "fixtures" / name, root / "fixtures" / name)
            with (root / "fixtures" / "AtomJitProbe.java").open("a") as output:
                output.write("// tampered without a new recipe qualification\n")
            with patch.object(verify, "HERE", root):
                with self.assertRaisesRegex(verify.Rejected, "source drift"):
                    verify.seal_sources()

    def test_missing_native_source_manifest_refused(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = pathlib.Path(tmp)
            (root / "fixtures").mkdir()
            shutil.copytree(verify.HERE / "fixtures", root / "fixtures", dirs_exist_ok=True)
            manifest = (verify.HERE / "SOURCE_SHA256.tsv").read_text()
            (root / "SOURCE_SHA256.tsv").write_text("\n".join(manifest.splitlines()[:-1]) + "\n")
            with patch.object(verify, "HERE", root):
                with self.assertRaisesRegex(verify.Rejected, "incomplete"):
                    verify.seal_sources()

    def test_expected_distinct_interpreter_c1_c2_and_tiered_modes(self):
        self.assertEqual(["interpreted", "c1_eligible", "c2_eligible", "default_tiered"],
                         [entry[0] for entry in verify.MODES])
        self.assertEqual(4, len(set(entry[0] for entry in verify.MODES)))


if __name__ == "__main__":
    unittest.main()
