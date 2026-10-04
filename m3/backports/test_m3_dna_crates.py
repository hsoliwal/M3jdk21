# SPDX-License-Identifier: Apache-2.0
import csv
import hashlib
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import m3_dna_crates as normalized


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


class GenerateNormalizedRecipeCratesTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.control = root / "control"
        self.baseline = root / "baseline"
        self.donor = root / "donor"
        self.baseline_evidence = root / "baseline-evidence"
        self.donor_evidence = root / "donor-evidence"
        self.out = root / "out"
        self.path = "src/java.base/share/classes/p/A.java"
        self.added = "src/java.base/share/classes/p/Added.java"
        self.targets = root / "targets.txt"
        self.targets.write_text(self.path + "\n" + self.added + "\n", encoding="utf-8")

        raw_baseline = "package p; final class A { int x(){return 1;} }\n"
        normalized_baseline = (
            "package p; final class A { int x(){ int m3$atom=1; return m3$atom;} }\n"
        )
        raw_donor = "package p; final class A { int x(){return 2;} }\n"
        normalized_donor = (
            "package p; final class A { int x(){ int m3$atom=2; return m3$atom;} }\n"
        )
        added_text = "package p; final class Added {}\n"

        self._write(self.baseline / self.path, raw_baseline)
        self._write(self.donor / self.path, raw_donor)
        self._write(self.donor / self.added, added_text)

        self.baseline_manifest = self.baseline_evidence / "SOURCE_CONVERGENCE.tsv"
        self.donor_manifest = self.donor_evidence / "SOURCE_CONVERGENCE.tsv"
        self._changed_manifest(
            self.baseline_manifest,
            self.path,
            raw_baseline,
            normalized_baseline,
        )
        self._changed_manifest(
            self.donor_manifest,
            self.path,
            raw_donor,
            normalized_donor,
        )
        with self.donor_manifest.open("a", encoding="utf-8") as handle:
            handle.write(
                f"{self.added}\t{sha(added_text)}\t{sha(added_text)}\t"
                "true\tCONVERGED_UNCHANGED\t\n"
            )

        self.baseline_identity = self.baseline_evidence / "SOURCE_CONVERGENCE.image.tsv"
        self.donor_identity = self.donor_evidence / "SOURCE_CONVERGENCE.image.tsv"
        self._identity(self.baseline_identity, "BASELINE", "jdk-21+35", "1" * 64, 1, 1)
        self._identity(self.donor_identity, "DONOR", "jdk-22+36", "2" * 64, 2, 1)

        self.normalized_baseline = normalized_baseline
        self.normalized_donor = normalized_donor
        self.added_text = added_text

    def tearDown(self) -> None:
        self.temp.cleanup()

    @staticmethod
    def _write(path: Path, text: str) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")

    def _changed_manifest(self, manifest: Path, path: str, before: str, after: str) -> None:
        candidate = manifest.parent / "candidates" / path
        self._write(candidate, after)
        manifest.parent.mkdir(parents=True, exist_ok=True)
        manifest.write_text(
            "path\tpreSha256\tpostSha256\tfixedPoint\tstatus\tcandidate\n"
            f"{path}\t{sha(before)}\t{sha(after)}\ttrue\tCONVERGED_CHANGED\t"
            f"candidates/{path}\n",
            encoding="utf-8",
        )

    @staticmethod
    def _identity(
        path: Path,
        role: str,
        revision: str,
        root: str,
        files: int,
        changed: int,
    ) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"{role}\t{revision}\t{root}\t{files}\t{changed}\t0\n",
            encoding="utf-8",
        )

    @patch("m3_dna_crates.release_donor_refs.verify_with_release_authority")
    def test_crates_use_normalized_postimages_not_raw_sources(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}

        crates, pair_root = normalized.generate(
            self.control,
            self.baseline,
            self.baseline_manifest,
            self.baseline_identity,
            self.donor,
            self.donor_manifest,
            self.donor_identity,
            22,
            self.targets,
            self.out,
            crate_size=1,
        )

        self.assertEqual(2, len(crates))
        self.assertRegex(pair_root, r"^[0-9a-f]{64}$")

        root = (
            self.out
            / "src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned"
        )
        manifests = sorted(root.glob("jdk22-*/manifest.tsv"))
        self.assertEqual(2, len(manifests))

        rows = []
        for manifest in manifests:
            with manifest.open("r", encoding="utf-8", newline="") as handle:
                row = next(csv.reader(handle, delimiter="\t"))
            rows.append((manifest.parent, row))
        by_path = {row[1][0]: row for row in rows}

        a_dir, a = by_path[self.path]
        self.assertEqual(sha(self.normalized_baseline), a[1])
        self.assertEqual(sha(self.normalized_donor), a[2])
        self.assertEqual(
            self.normalized_donor,
            (a_dir / a[3]).read_text(encoding="utf-8"),
        )

        added_dir, added = by_path[self.added]
        self.assertEqual("ABSENT", added[1])
        self.assertEqual(sha(self.added_text), added[2])
        self.assertEqual(
            self.added_text,
            (added_dir / added[3]).read_text(encoding="utf-8"),
        )

        with (self.out / "NORMALIZED_PAIR.tsv").open(
            "r", encoding="utf-8", newline=""
        ) as handle:
            pair = next(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(pair_root, pair["pairRoot"])
        self.assertEqual("jdk-22+36", pair["donorRef"])
        self.assertEqual("2", pair["candidateTargets"])

        bindings = (self.out / "CRATE_NORMALIZATION.tsv").read_text(encoding="utf-8")
        for crate in crates:
            self.assertIn(crate, bindings)
        self.assertIn(pair_root, bindings)

    @patch("m3_dna_crates.release_donor_refs.verify_with_release_authority")
    def test_hold_blocks_crate_generation(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}
        text = self.donor_manifest.read_text(encoding="utf-8")
        self.donor_manifest.write_text(
            text.replace(
                "true\tCONVERGED_CHANGED",
                "false\tHOLD",
                1,
            ),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "FILE fixed point"):
            normalized.generate(
                self.control,
                self.baseline,
                self.baseline_manifest,
                self.baseline_identity,
                self.donor,
                self.donor_manifest,
                self.donor_identity,
                22,
                self.targets,
                self.out,
            )


if __name__ == "__main__":
    unittest.main()
