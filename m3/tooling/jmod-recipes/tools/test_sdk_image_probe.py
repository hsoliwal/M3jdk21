# SPDX-License-Identifier: Apache-2.0
"""Pure and preflight refusal tests; not a substitute for linked-image runtime execution."""
import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import sdk_image_probe as sdk


def manifest(modules=("com.google.common", "re2j")):
    return "module\tdescriptor_source\tresult\n" + "".join(
        name + "\trecipe\tJMOD\n" for name in modules
    )


class SdkImageProbeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.image = self.root / "image"
        (self.image / "bin").mkdir(parents=True)
        (self.image / "release").write_text('JAVA_VERSION="21.0.12"\n')
        self.pack = self.root / "JMOD_PACK.tsv"
        self.pack.write_text(manifest())

    def test_manifest_deterministic_and_rejects_missing_or_failed_donors(self):
        parsed = sdk.read_pack(manifest())
        self.assertEqual(frozenset(("com.google.common", "re2j")), parsed)
        with self.assertRaisesRegex(ValueError, "INCOMPLETE"):
            sdk.read_pack(manifest(("re2j",)))
        with self.assertRaisesRegex(ValueError, "DUPLICATE"):
            sdk.read_pack(manifest(("re2j", "re2j")))
        with self.assertRaisesRegex(ValueError, "FAILED"):
            sdk.read_pack(manifest().replace("\tJMOD\n", "\tJMOD_FAILED\n", 1))

    def test_module_list_parser_refuses_duplicate_or_invalid_names(self):
        self.assertEqual(
            frozenset(("java.base", "re2j", "com.google.common")),
            sdk.parse_modules("java.base@21\nre2j\ncom.google.common\n"))
        with self.assertRaisesRegex(ValueError, "INVALID"):
            sdk.parse_modules("re2j\nre2j\n")
        with self.assertRaisesRegex(ValueError, "INVALID"):
            sdk.parse_modules("bad/name\n")

    def test_java21_identity_and_missing_dev_tools_fail_closed(self):
        self.assertEqual("21.0.12", sdk.java21_release(self.image))
        (self.image / "release").write_text('JAVA_VERSION="25.0.1"\n')
        with self.assertRaisesRegex(ValueError, "NOT_JAVA21"):
            sdk.java21_release(self.image)
        with self.assertRaisesRegex(ValueError, "TOOL_MISSING"):
            sdk.locate(self.image, "javac")

    def test_legacy_runtime_only_image_is_not_an_sdk(self):
        out = self.root / "candidate"
        with self.assertRaisesRegex(ValueError, "SDK_TOOL_MISSING"):
            sdk.prove(self.image, self.pack, out)
        self.assertFalse(out.exists())

    def test_uncontrolled_vm_flags_prevent_any_probe(self):
        out = self.root / "candidate"
        with patch.dict(sdk.os.environ, {"JAVA_TOOL_OPTIONS": "-Dtest=true"}):
            with self.assertRaisesRegex(ValueError, "UNCONTROLLED"):
                sdk.prove(self.image, self.pack, out)
        self.assertFalse(out.exists())

    def test_occupied_output_is_never_deleted(self):
        out = self.root / "candidate"
        out.mkdir()
        (out / "keep").write_text("unaltered")
        with self.assertRaisesRegex(ValueError, "OCCUPIED"):
            sdk.prove(self.image, self.pack, out)
        self.assertEqual("unaltered", (out / "keep").read_text())

    def test_smoke_does_not_grant_product_status(self):
        # Source-level guard: no passing test may promote a vendor smoke into jtreg admission.
        source = Path(sdk.__file__).read_text()
        self.assertIn('"product_accepted": False', source)
        self.assertIn('"openjdk_jtreg": "NOT_EXECUTED_BY_SMOKE"', source)
        self.assertEqual("M3_SDK_VENDOR_API_SMOKE_PASS", sdk.SMOKE)


if __name__ == "__main__":
    unittest.main()
