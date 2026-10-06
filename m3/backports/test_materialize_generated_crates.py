#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import importlib.util
from pathlib import Path
import sys
import tempfile
import unittest


MODULE_PATH = Path(__file__).with_name("materialize_generated_crates.py")
SPEC = importlib.util.spec_from_file_location("m3_generated_crate_materializer", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
materializer = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = materializer
SPEC.loader.exec_module(materializer)


class GeneratedCrateMaterializerTest(unittest.TestCase):
    def fixture(self) -> tuple[Path, Path, dict[str, bytes]]:
        root = Path(tempfile.mkdtemp(prefix="m3-generated-target-"))
        generated = Path(tempfile.mkdtemp(prefix="m3-generated-crates-"))

        java_name = "jdk24-java-0001"
        text_name = "jdk24-text-0001"
        java_path = "src/jdk.jlink/share/classes/demo/Foo.java"
        text_path = "make/demo.gmk"
        before_java = b"class Foo {}\n"
        after_java = b"class Foo { int value() { return 1; } }\n"
        after_text = b"DEMO := enabled\n"

        java_dir = generated / materializer.JAVA_ROOT / java_name
        text_dir = generated / materializer.TEXT_ROOT / text_name
        java_dir.mkdir(parents=True)
        text_dir.mkdir(parents=True)

        (java_dir / "0001.java.txt").write_bytes(after_java)
        (java_dir / "manifest.tsv").write_text(
            f"{java_path}\t{hashlib.sha256(before_java).hexdigest()}\t"
            f"{hashlib.sha256(after_java).hexdigest()}\t0001.java.txt\n",
            encoding="utf-8",
        )
        (text_dir / "0001.txt").write_bytes(after_text)
        (text_dir / "manifest.tsv").write_text(
            f"{text_path}\tABSENT\t{hashlib.sha256(after_text).hexdigest()}\t0001.txt\n",
            encoding="utf-8",
        )

        with (generated / "CRATES.tsv").open("w", encoding="utf-8", newline="") as handle:
            writer = csv.writer(handle, delimiter="\t", lineterminator="\n")
            writer.writerow(
                (
                    "crate_name",
                    "release",
                    "baseline_ref",
                    "donor_ref",
                    "target_count",
                    "first_path",
                    "last_path",
                    "status",
                )
            )
            writer.writerow((java_name, "24", "jdk-21+35", "jdk-24+36", "1", java_path, java_path, "CANDIDATE_UNVERIFIED"))
            writer.writerow((text_name, "24", "jdk-21+35", "jdk-24+36", "1", text_path, text_path, "CANDIDATE_UNVERIFIED"))

        java_target = root / java_path
        java_target.parent.mkdir(parents=True)
        java_target.write_bytes(before_java)
        return root, generated, {java_path: after_java, text_path: after_text}

    def test_java_and_text_crates_apply_and_replay_to_fixed_point(self) -> None:
        root, generated, expected = self.fixture()
        receipt = root / "receipt.tsv"

        targets = materializer.run(root, generated, "check", True, receipt)
        self.assertEqual(2, len(targets))
        self.assertFalse((root / "make/demo.gmk").exists())

        applied = materializer.run(root, generated, "apply", True, receipt)
        self.assertEqual(2, len(applied))
        for path, payload in expected.items():
            self.assertEqual(payload, (root / path).read_bytes())

        verified = materializer.run(root, generated, "verify-post", True, receipt)
        self.assertEqual(2, len(verified))

        # Exact postimage replay remains a no-op at the byte level.
        materializer.run(root, generated, "apply", True, receipt)
        for path, payload in expected.items():
            self.assertEqual(payload, (root / path).read_bytes())

    def test_preimage_drift_fails_closed(self) -> None:
        root, generated, _ = self.fixture()
        path = root / "src/jdk.jlink/share/classes/demo/Foo.java"
        path.write_text("class Drift {}\n", encoding="utf-8")

        with self.assertRaisesRegex(ValueError, "GENERATED_CRATE_PREIMAGE_DRIFT"):
            materializer.run(root, generated, "check", True, None)

    def test_file_lane_rejects_multi_target_crate(self) -> None:
        root, generated, _ = self.fixture()
        index = generated / "CRATES.tsv"
        text = index.read_text(encoding="utf-8")
        text = text.replace(
            "jdk24-java-0001\t24\tjdk-21+35\tjdk-24+36\t1\t",
            "jdk24-java-0001\t24\tjdk-21+35\tjdk-24+36\t2\t",
        )
        index.write_text(text, encoding="utf-8")

        with self.assertRaisesRegex(ValueError, "FILE lane requires one target per crate"):
            materializer.run(root, generated, "check", True, None)

    def test_duplicate_target_ownership_fails_closed(self) -> None:
        root, generated, _ = self.fixture()
        java_dir = generated / materializer.JAVA_ROOT / "jdk24-java-0001"
        text_dir = generated / materializer.TEXT_ROOT / "jdk24-text-0001"
        java_manifest = (java_dir / "manifest.tsv").read_text(encoding="utf-8").strip().split("\t")
        duplicate_payload = (java_dir / java_manifest[3]).read_bytes()
        (text_dir / "0001.txt").write_bytes(duplicate_payload)
        (text_dir / "manifest.tsv").write_text(
            f"{java_manifest[0]}\t{java_manifest[1]}\t{java_manifest[2]}\t0001.txt\n",
            encoding="utf-8",
        )

        with self.assertRaisesRegex(ValueError, "multiple generated crates"):
            materializer.run(root, generated, "check", True, None)

    def test_path_and_crate_ownership_fences(self) -> None:
        self.assertRaises(ValueError, materializer.canonical_relative, "../escape")
        self.assertRaises(ValueError, materializer.canonical_relative, "/absolute")
        self.assertRaises(ValueError, materializer.canonical_relative, "a\\b")

        root, generated, _ = self.fixture()
        index = generated / "CRATES.tsv"
        text = index.read_text(encoding="utf-8").replace(
            "jdk24-java-0001",
            "nested/jdk24-java-0001",
            1,
        )
        index.write_text(text, encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "single path segment"):
            materializer.run(root, generated, "check", False, None)


    def test_crate_index_provenance_and_bounds_are_reproved(self) -> None:
        mutations = (
            ("jdk-21+35", "jdk-20+36", "unexpected crate baseline ref"),
            ("jdk-24+36", "jdk-24+35", "unexpected crate donor ref"),
            ("CANDIDATE_UNVERIFIED", "VERIFIED", "unexpected crate status"),
            (
                "src/jdk.jlink/share/classes/demo/Foo.java\tsrc/jdk.jlink/share/classes/demo/Foo.java",
                "src/jdk.jlink/share/classes/demo/Z.java\tsrc/jdk.jlink/share/classes/demo/Foo.java",
                "first/last path order invalid",
            ),
        )
        for old, new, message in mutations:
            with self.subTest(message=message):
                root, generated, _ = self.fixture()
                index = generated / "CRATES.tsv"
                text = index.read_text(encoding="utf-8")
                self.assertIn(old, text)
                index.write_text(text.replace(old, new, 1), encoding="utf-8")
                with self.assertRaisesRegex(ValueError, message):
                    materializer.run(root, generated, "check", True, None)

    def test_crate_first_last_metadata_must_match_manifest(self) -> None:
        root, generated, _ = self.fixture()
        index = generated / "CRATES.tsv"
        rows = list(csv.DictReader(index.open(encoding="utf-8", newline=""), delimiter="\t"))
        rows[0]["first_path"] = "src/jdk.jlink/share/classes/demo/Bar.java"
        rows[0]["last_path"] = "src/jdk.jlink/share/classes/demo/Bar.java"
        with index.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(
                handle,
                fieldnames=(
                    "crate_name",
                    "release",
                    "baseline_ref",
                    "donor_ref",
                    "target_count",
                    "first_path",
                    "last_path",
                    "status",
                ),
                delimiter="\t",
                lineterminator="\n",
            )
            writer.writeheader()
            writer.writerows(rows)

        with self.assertRaisesRegex(ValueError, "first/last path metadata drift"):
            materializer.run(root, generated, "check", True, None)

    def test_ambiguous_manifest_ownership_and_payload_symlink_fail_closed(self) -> None:
        root, generated, _ = self.fixture()
        java_name = "jdk24-java-0001"
        java_manifest = generated / materializer.JAVA_ROOT / java_name / "manifest.tsv"
        text_owner = generated / materializer.TEXT_ROOT / java_name
        text_owner.mkdir(parents=True)
        (text_owner / "manifest.tsv").write_text(
            java_manifest.read_text(encoding="utf-8"),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "ownership is ambiguous"):
            materializer.run(root, generated, "check", True, None)

        root, generated, _ = self.fixture()
        java_dir = generated / materializer.JAVA_ROOT / java_name
        payload = java_dir / "0001.java.txt"
        original = payload.read_bytes()
        payload.unlink()
        target = generated / "payload-copy.java.txt"
        target.write_bytes(original)
        payload.symlink_to(target)
        with self.assertRaisesRegex(ValueError, "missing/unsafe crate payload"):
            materializer.run(root, generated, "check", True, None)


if __name__ == "__main__":
    unittest.main()
