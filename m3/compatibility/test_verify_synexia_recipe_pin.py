# SPDX-License-Identifier: Apache-2.0
"""JUnit-equivalent Python regression tests for external Synexia checkout pins."""

from __future__ import annotations

import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from m3.compatibility.verify_synexia_recipe_pin import (
    FIELDS,
    PIN,
    _blob_from_tree,
    _path,
    read_pin,
    verify,
)


class SynexiaRecipeCheckoutPinTest(unittest.TestCase):
    def test_committed_pin_is_single_source_of_checkout_authority(self) -> None:
        pin = read_pin()
        self.assertEqual("hsoliwal/com.synexia", pin["canonical_repository"])
        self.assertEqual("Apache-2.0", pin["license"])
        self.assertEqual(40, len(pin["canonical_revision"]))
        for path_column, blob_column in FIELDS:
            self.assertTrue(pin[path_column].startswith(("docs/", "synexia-")))
            self.assertEqual(40, len(pin[blob_column]))

        workflow = (
            PIN.parents[2] / ".github" / "workflows" / "m3-synexia-recipe-home.yml"
        ).read_text(encoding="utf-8")
        self.assertIn(f"ref: {pin['canonical_revision']}", workflow)

    def test_verifier_accepts_exact_revision_and_both_git_blob_pins(self) -> None:
        pin = read_pin()
        responses = [pin["canonical_revision"]] + [
            f"100644 blob {pin[blob_column]}\t{pin[path_column]}"
            for path_column, blob_column in FIELDS
        ]
        with patch("m3.compatibility.verify_synexia_recipe_pin._git", side_effect=responses) as run_git:
            self.assertEqual(pin["canonical_revision"], verify(Path("/dummy")))
            self.assertEqual(3, run_git.call_count)

    def test_revision_or_either_blob_mismatch_is_rejected(self) -> None:
        pin = read_pin()
        responses = [pin["canonical_revision"]] + [
            f"100644 blob {pin[blob_column]}\t{pin[path_column]}"
            for path_column, blob_column in FIELDS
        ]
        with patch("m3.compatibility.verify_synexia_recipe_pin._git", side_effect=["f" * 40]):
            with self.assertRaisesRegex(ValueError, "revision mismatch"):
                verify(Path("/dummy"))

        for invalid_index in (1, 2):
            invalid = list(responses)
            _, blob_column = FIELDS[invalid_index - 1]
            invalid[invalid_index] = invalid[invalid_index].replace(pin[blob_column], "0" * 40)
            with self.subTest(invalid_index=invalid_index):
                with patch("m3.compatibility.verify_synexia_recipe_pin._git", side_effect=invalid):
                    with self.assertRaisesRegex(ValueError, "blob mismatch"):
                        verify(Path("/dummy"))

    def test_duplicate_unsafe_or_malformed_pin_is_rejected(self) -> None:
        original = PIN.read_text(encoding="utf-8")
        with tempfile.TemporaryDirectory() as temp:
            location = Path(temp) / "pin.tsv"
            location.write_text(original + original.splitlines()[-1] + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "exactly one"):
                read_pin(location)
            location.write_text(original.replace("Apache-2.0", "UNVERIFIED"), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "invalid canonical"):
                read_pin(location)

        for path in ("../a", "/absolute", "a//b", "a\\b", "a/./b", "a/../b", ""):
            with self.subTest(path=path):
                with self.assertRaisesRegex(ValueError, "unsafe"):
                    _path(path)

        with self.assertRaisesRegex(ValueError, "exactly one"):
            _blob_from_tree("", "x/file")
        with self.assertRaisesRegex(ValueError, "invalid pinned Git tree"):
            _blob_from_tree("100644 tree " + "a" * 40 + "\tx/file", "x/file")


if __name__ == "__main__":
    unittest.main()
