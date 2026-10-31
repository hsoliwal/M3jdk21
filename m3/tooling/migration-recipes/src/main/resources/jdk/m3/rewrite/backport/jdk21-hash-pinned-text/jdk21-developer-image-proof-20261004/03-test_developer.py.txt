# SPDX-License-Identifier: Apache-2.0
"""Unit/refusal proof; synthetic copies here are NOT JMOD/native compatibility proof."""
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import build_pack
import developer_tools
import verify_backport


class DeveloperPackTests(unittest.TestCase):
    def test_roots_are_explicit_unique_and_deterministic(self):
        roots = developer_tools.DEVELOPER_ROOTS
        self.assertEqual(tuple(sorted(set(roots))), roots)
        self.assertTrue({"jdk.compiler", "jdk.javadoc", "jdk.jlink", "jdk.jpackage"}.issubset(roots))

    def test_default_toolchain_inspection_is_still_callable(self):
        with tempfile.TemporaryDirectory() as directory:
            home = self.home(Path(directory))
            self.assertEqual(home, build_pack.java_home(home))

    def test_linux_gate_precedes_output(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            home = self.home(base)
            with patch.object(build_pack.platform, "system", return_value="Windows"):
                with self.assertRaisesRegex(ValueError, "Linux-only"):
                    build_pack.main(["--jdk", str(home), "--out", str(base / "output"),
                                     "--profile", "developer"])
            self.assertFalse((base / "output").exists())

    def test_existing_output_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            home = self.home(base)
            output = base / "occupied"
            output.mkdir()
            (output / "keep").write_text("keep")
            with self.assertRaises(FileExistsError):
                build_pack.main(["--jdk", str(home), "--out", str(output)])
            self.assertEqual("keep", (output / "keep").read_text())

    def test_jdk_output_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            home = self.home(Path(directory))
            with self.assertRaisesRegex(ValueError, "JAVA_HOME"):
                build_pack.main(["--jdk", str(home), "--out", str(home / "illegal")])
            self.assertFalse((home / "illegal").exists())

    def test_link_kit_copy_pins_every_file(self):
        with tempfile.TemporaryDirectory() as directory:
            home, output = self.copy_fixture(Path(directory))
            rows = developer_tools.copy_link_inputs(home, output)
            self.assertEqual(3, len(rows))
            self.assertEqual(sorted(rows), rows)
            for name, checksum in rows:
                self.assertEqual(checksum, build_pack.digest(output / "link-inputs" / name))
            with self.assertRaises(FileExistsError):
                developer_tools.copy_link_inputs(home, output)

    def test_incomplete_copy_is_not_admitted(self):
        with tempfile.TemporaryDirectory() as directory:
            home, output = self.copy_fixture(Path(directory))
            (home / "jmods/jdk.jlink.jmod").unlink()
            with self.assertRaisesRegex(ValueError, "Incomplete"):
                developer_tools.copy_link_inputs(home, output)
            self.assertFalse((output / "link-inputs/SHA256.tsv").exists())

    def test_copy_drift_is_refused(self):
        with tempfile.TemporaryDirectory() as directory:
            home, output = self.copy_fixture(Path(directory))
            with patch.object(developer_tools, "digest", side_effect=["a", "b"]):
                with self.assertRaisesRegex(ValueError, "changed during copy"):
                    developer_tools.copy_link_inputs(home, output)
            self.assertFalse((output / "link-inputs/SHA256.tsv").exists())

    def test_failed_tool_never_emits_success_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            with patch.object(developer_tools, "command", side_effect=RuntimeError("compiler failure")):
                with self.assertRaisesRegex(RuntimeError, "compiler failure"):
                    developer_tools.verify_developer_tools(base / "image", base, base / "jmods")
            self.assertFalse((base / "developer-proof/RECEIPT.json").exists())

    def test_selected_link_pins_match_inventory(self):
        home = Path("/selected-jdk")
        inventory = "module\tversion\tkind\tsha256\tpath\njava.base\t21\tJMOD\texact\t/selected-jdk/jmods/java.base.jmod\n"
        developer_tools.verify_link_pins(inventory, [("java.base.jmod", "exact")], home)
        with self.assertRaisesRegex(ValueError, "changed since inventory"):
            developer_tools.verify_link_pins(inventory, [("java.base.jmod", "drift")], home)

    def test_policy_cases_are_fresh_and_cover_precedence(self):
        cases = verify_backport.CASES
        self.assertEqual(14, len(cases))
        self.assertEqual(len(cases), len({row[0] for row in cases}))
        self.assertIn(("system-wins-invalid-security", "true", "invalid", "ALLOW"), cases)
        self.assertIn(("system-empty", "", None, "INVALID"), cases)

    def test_ambient_vm_options_refused_before_output(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            home = self.home(base)
            with patch.dict(verify_backport.os.environ, {"JAVA_TOOL_OPTIONS": "-Dunexpected=true"}):
                with self.assertRaisesRegex(ValueError, "Uncontrolled VM"):
                    verify_backport.main(["--jdk", str(home), "--repo", str(base),
                                          "--out", str(base / "proof")])
            self.assertFalse((base / "proof").exists())

    def test_product_source_drift_refused_before_output(self):
        with tempfile.TemporaryDirectory() as directory:
            base = Path(directory)
            home = self.home(base)
            product = base / verify_backport.SOURCE
            product.parent.mkdir(parents=True)
            product.write_text("not the pinned product")
            with patch.dict(verify_backport.os.environ, {}, clear=True):
                with self.assertRaisesRegex(ValueError, "PASSWORD_SOURCE_DRIFT"):
                    verify_backport.main(["--jdk", str(home), "--repo", str(base),
                                          "--out", str(base / "proof")])
            self.assertFalse((base / "proof").exists())

    @staticmethod
    def home(base):
        home = base / "jdk"
        (home / "bin").mkdir(parents=True)
        (home / "release").write_text('JAVA_VERSION="21.0.11"\n')
        for name in ("java", "javac", "jar", "jmod", "jlink"):
            (home / "bin" / name).touch()
        return home

    @staticmethod
    def copy_fixture(base):
        home, output = base / "jdk", base / "output"
        (home / "jmods").mkdir(parents=True)
        output.mkdir()
        for name in ("java.base", "jdk.compiler", "jdk.jlink"):
            (home / "jmods" / (name + ".jmod")).write_text("synthetic copy fixture " + name)
        return home, output


if __name__ == "__main__":
    unittest.main()
