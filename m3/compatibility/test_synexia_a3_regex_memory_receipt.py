# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECEIPT = ROOT / "compatibility" / "synexia-a3-regex-memory-recipe-receipt-20261008.tsv"


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


class SynexiaA3RegexMemoryReceiptTest(unittest.TestCase):
    def test_receipt_pins_canonical_synexia_packet_and_identical_manifest(self) -> None:
        with RECEIPT.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(1, len(rows))
        row = rows[0]
        self.assertEqual("M3JDK21_SYNEXIA_A3_REGEX_MEMORY_RECEIPT_V1", row["schema"])
        self.assertEqual("hsoliwal/com.synexia", row["canonical_repository"])
        self.assertEqual("9861", row["canonical_pr"])
        self.assertEqual(
            "69704fe7b6f455042c6c90f959d9af4fa48ce9e9",
            row["canonical_recipe_revision"],
        )
        self.assertEqual(
            "com.synexia.rewrite.M3A3RegexMemoryLab",
            row["canonical_recipe"],
        )
        self.assertEqual("FROZEN_THIN_RECEIVER", row["target_state"])

        for field in (
            "canonical_recipe_git_blob",
            "canonical_manifest_git_blob",
            "target_descriptor_git_blob",
            "target_manifest_git_blob",
        ):
            self.assertRegex(row[field], r"^[0-9a-f]{40}$")

        descriptor = ROOT.parent / row["target_descriptor_path"]
        manifest = ROOT.parent / row["target_manifest_path"]
        self.assertTrue(descriptor.is_file())
        self.assertTrue(manifest.is_file())
        self.assertEqual(row["target_descriptor_git_blob"], git_blob(descriptor))
        self.assertEqual(row["target_manifest_git_blob"], git_blob(manifest))
        self.assertEqual(
            row["canonical_manifest_git_blob"],
            row["target_manifest_git_blob"],
            "target and canonical manifests must be byte-identical",
        )

        descriptor_text = descriptor.read_text(encoding="utf-8")
        self.assertIn("com.synexia.rewrite.M3A3RegexMemoryLab", descriptor_text)
        self.assertIn("69704fe7b6f455042c6c90f959d9af4fa48ce9e9", descriptor_text)
        self.assertIn("M3HashPinnedJavaSnapshotRecipe", descriptor_text)
        self.assertIn("crateName: a3-regex-memory-lab", descriptor_text)

    def test_adjacent_descriptors_remain_target_adapters(self) -> None:
        workflow = (
            ROOT.parent
            / "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/"
            / "m3-a3-regex-memory-workflow.yml"
        ).read_text(encoding="utf-8")
        self.assertIn("Target-only replay", workflow)
        self.assertIn("not portable recipe authority", workflow)

        jni = (
            ROOT.parent
            / "m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/"
            / "m3-jni-newstring-admission.yml"
        ).read_text(encoding="utf-8")
        self.assertIn("M3JDK product adapter", jni)
        self.assertIn("OpenJDK String bootstrap/JNI ABI integration", jni)


if __name__ == "__main__":
    unittest.main()
