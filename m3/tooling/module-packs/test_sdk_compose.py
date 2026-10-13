# SPDX-License-Identifier: Apache-2.0
"""Pure refactor/safety tests for M3SDK JMOD+developer-pack composition.

Synthetic .jmod files are intentionally NOT accepted as native/runtime evidence.
"""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import sdk_compose as sdk


class CompositeSdkTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.home = self.root / "jdk"
        self.vendor = self.root / "vendor"
        (self.home / "bin").mkdir(parents=True)
        (self.home / "jmods").mkdir()
        (self.home / "release").write_text('JAVA_VERSION="21.0.12"\n')
        for name in ("java", "javac", "jar", "jmod", "jlink"):
            (self.home / "bin" / name).touch()
        self.vendor.mkdir()
        self.names = tuple(sorted(("re2j", "com.google.common", *(
            "vendor.lib%03d" % index for index in range(85)))))
        self.pack = self.root / "JMOD_PACK.tsv"
        self.pack.write_text("module\tdescriptor_source\tresult\n" + "".join(
            name + "\ttest-descriptor\tJMOD\n" for name in self.names
        ))
        for name in self.names:
            (self.vendor / (name + ".jmod")).write_bytes(
                ("SYNTHETIC_NOT_A_JMOD:" + name).encode("ascii")
            )

    def test_exact_87_input_identity_is_repeatable(self):
        rows, pack, source = sdk.inspect_vendor_inputs(
            self.home, self.vendor, self.pack)
        self.assertEqual(87, len(rows))
        self.assertEqual(self.pack.resolve(), pack)
        self.assertEqual(self.vendor.resolve(), source)
        self.assertEqual(sdk.vendor_root(rows), sdk.vendor_root(rows))
        self.assertRegex(sdk.vendor_root(rows), r"^[0-9a-f]{64}$")
        sdk.require_unchanged(rows)

    def test_mutated_source_refused_without_output(self):
        rows, _, _ = sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)
        (self.vendor / "re2j.jmod").write_bytes(b"CHANGED")
        with self.assertRaisesRegex(ValueError, "MUTATED"):
            sdk.require_unchanged(rows)

    def test_missing_extra_or_symlinked_input_is_refused(self):
        (self.vendor / "re2j.jmod").unlink()
        with self.assertRaisesRegex(ValueError, "SET_DRIFT"):
            sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)
        (self.vendor / "re2j.jmod").write_bytes(b"fixture")
        (self.vendor / "extra.jmod").write_bytes(b"fixture")
        with self.assertRaisesRegex(ValueError, "SET_DRIFT"):
            sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)
        (self.vendor / "extra.jmod").unlink()
        (self.vendor / "re2j.jmod").unlink()
        (self.vendor / "re2j.jmod").symlink_to(self.vendor / "com.google.common.jmod")
        with self.assertRaisesRegex(ValueError, "UNSAFE_JMOD"):
            sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)

    def test_conflicting_host_jdk_module_name_refused(self):
        (self.home / "jmods" / "re2j.jmod").write_bytes(b"collision")
        with self.assertRaisesRegex(ValueError, "COLLISION"):
            sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)

    def test_invalid_manifest_refused(self):
        self.pack.write_text(self.pack.read_text().replace(
            "re2j\ttest-descriptor\tJMOD",
            "re2j\ttest-descriptor\tJMOD_FAILED"))
        with self.assertRaisesRegex(ValueError, "FAILED"):
            sdk.inspect_vendor_inputs(self.home, self.vendor, self.pack)

    def test_occupied_and_in_product_tree_outputs_refused(self):
        candidate = self.root / "candidate"
        self.assertEqual(candidate, sdk.require_safe_output(
            candidate, self.home, self.vendor))
        candidate.mkdir()
        (candidate / "keep").write_text("preserved")
        with self.assertRaisesRegex(ValueError, "OCCUPIED"):
            sdk.require_safe_output(candidate, self.home, self.vendor)
        self.assertEqual("preserved", (candidate / "keep").read_text())
        with self.assertRaisesRegex(ValueError, "UNSAFE"):
            sdk.require_safe_output(
                self.home / "not-allowed", self.home, self.vendor)

    def test_uncontrolled_vm_options_refused_before_creating_output(self):
        out = self.root / "candidate"
        with patch.dict(sdk.os.environ, {"JAVA_TOOL_OPTIONS": "-Dunsafe=true"}):
            with self.assertRaisesRegex(ValueError, "UNCONTROLLED"):
                sdk.compose(self.home, self.vendor, self.pack, out)
        self.assertFalse(out.exists())

    def test_missing_vendor_input_fails_before_creating_output(self):
        (self.vendor / "re2j.jmod").unlink()
        out = self.root / "candidate"
        with self.assertRaisesRegex(ValueError, "SET_DRIFT"):
            sdk.compose(self.home, self.vendor, self.pack, out)
        self.assertFalse(out.exists())

    def test_stage_success_cannot_promote_product_claim(self):
        source = Path(sdk.__file__).read_text(encoding="utf-8")
        self.assertIn('"product_accepted": False', source)
        self.assertIn('LOCAL_SDK_IMAGE_RUNTIME_SMOKE_PROVEN', source)
        self.assertIn('java21_spec_compatibility', source)
        self.assertIn('build_pack.main(', source)
        self.assertIn('sdk_image_probe.prove(', source)


if __name__ == "__main__":
    unittest.main()
