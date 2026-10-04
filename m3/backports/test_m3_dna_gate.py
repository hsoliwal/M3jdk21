# SPDX-License-Identifier: Apache-2.0
import hashlib
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import m3_dna_gate as pair


def sha(payload: bytes) -> str:
    return hashlib.sha256(payload).hexdigest()


class SourcePairConvergenceGateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.control = root / "control"
        self.baseline = root / "baseline"
        self.donor = root / "donor"
        self.evidence = root / "evidence"
        self.evidence.mkdir(parents=True)
        self.path = "src/java.base/share/classes/p/A.java"
        self.targets = root / "targets.txt"
        self.targets.write_text(self.path + "\n", encoding="utf-8")

        baseline_bytes = b"package p; final class A { int x(){return 1;} }\n"
        donor_bytes = b"package p; final class A { int x(){return 2;} }\n"
        for tree, payload in ((self.baseline, baseline_bytes), (self.donor, donor_bytes)):
            target = tree / self.path
            target.parent.mkdir(parents=True)
            target.write_bytes(payload)

        self.baseline_manifest = self.evidence / "baseline.tsv"
        self.donor_manifest = self.evidence / "donor.tsv"
        self._manifest(self.baseline_manifest, baseline_bytes)
        self._manifest(self.donor_manifest, donor_bytes)

        self.baseline_identity = self.evidence / "baseline-image.tsv"
        self.donor_identity = self.evidence / "donor-image.tsv"
        self._identity(self.baseline_identity, "BASELINE", "jdk-21+35", "1" * 64)
        self._identity(self.donor_identity, "DONOR", "jdk-22+36", "2" * 64)

    def tearDown(self) -> None:
        self.temp.cleanup()

    def _manifest(self, path: Path, payload: bytes) -> None:
        digest = sha(payload)
        path.write_text(
            "path\tpreSha256\tpostSha256\tfixedPoint\tstatus\tcandidate\n"
            f"{self.path}\t{digest}\t{digest}\ttrue\tCONVERGED_UNCHANGED\t\n",
            encoding="utf-8",
        )

    def _identity(self, path: Path, role: str, revision: str, root: str) -> None:
        path.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"{role}\t{revision}\t{root}\t1\t0\t0\n",
            encoding="utf-8",
        )

    @patch("m3_dna_gate.release_donor_refs.verify_with_release_authority")
    def test_pair_root_binds_both_fixed_points_and_donor_revision(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}
        first = pair.verify(
            self.control,
            self.baseline,
            self.baseline_manifest,
            self.baseline_identity,
            self.donor,
            self.donor_manifest,
            self.donor_identity,
            22,
            self.targets,
        )
        second = pair.verify(
            self.control,
            self.baseline,
            self.baseline_manifest,
            self.baseline_identity,
            self.donor,
            self.donor_manifest,
            self.donor_identity,
            22,
            self.targets,
        )
        self.assertEqual(first, second)
        self.assertRegex(first, r"^[0-9a-f]{64}$")

    @patch("m3_dna_gate.release_donor_refs.verify_with_release_authority")
    def test_wrong_donor_revision_and_source_drift_fail_closed(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}
        self._identity(self.donor_identity, "DONOR", "jdk-23+37", "2" * 64)
        with self.assertRaisesRegex(ValueError, "revision mismatch"):
            pair.verify(
                self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
                self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
            )

        self._identity(self.donor_identity, "DONOR", "jdk-22+36", "2" * 64)
        (self.donor / self.path).write_text("drift\n", encoding="utf-8")
        with self.assertRaisesRegex(ValueError, "preimage drift"):
            pair.verify(
                self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
                self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
            )

    @patch("m3_dna_gate.release_donor_refs.verify_with_release_authority")
    def test_added_donor_file_is_admitted_when_baseline_is_truly_absent(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}
        added = "src/java.base/share/classes/p/Added.java"
        donor_bytes = b"package p; final class Added {}\n"
        donor_file = self.donor / added
        donor_file.parent.mkdir(parents=True, exist_ok=True)
        donor_file.write_bytes(donor_bytes)
        digest = sha(donor_bytes)
        with self.donor_manifest.open("a", encoding="utf-8") as handle:
            handle.write(
                f"{added}\t{digest}\t{digest}\ttrue\tCONVERGED_UNCHANGED\t\n"
            )
        self.targets.write_text(added + "\n", encoding="utf-8")

        root = pair.verify(
            self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
            self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
        )
        self.assertRegex(root, r"^[0-9a-f]{64}$")

        baseline_file = self.baseline / added
        baseline_file.parent.mkdir(parents=True, exist_ok=True)
        baseline_file.write_bytes(b"unexpected\n")
        with self.assertRaisesRegex(ValueError, "exists without SOURCE_CONVERGENCE row"):
            pair.verify(
                self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
                self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
            )

    @patch("m3_dna_gate.release_donor_refs.verify_with_release_authority")
    def test_hold_and_missing_target_fail_closed(self, authority) -> None:
        authority.return_value = {22: "jdk-22+36"}
        text = self.donor_manifest.read_text(encoding="utf-8")
        self.donor_manifest.write_text(
            text.replace(
                "true\tCONVERGED_UNCHANGED",
                "false\tHOLD",
            ),
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "FILE fixed point"):
            pair.verify(
                self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
                self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
            )

        self.targets.write_text(
            "src/java.base/share/classes/p/Missing.java\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "missing donor SOURCE_CONVERGENCE row"):
            pair.verify(
                self.control, self.baseline, self.baseline_manifest, self.baseline_identity,
                self.donor, self.donor_manifest, self.donor_identity, 22, self.targets
            )


if __name__ == "__main__":
    unittest.main()
