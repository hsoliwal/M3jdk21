# SPDX-License-Identifier: Apache-2.0
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import normalize_release_donor as normalizer


class NormalizeReleaseDonorTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        root = Path(self.temp.name)
        self.control = root / "control"
        self.donor = root / "donor"
        self.output = root / "output"
        (self.control / "m3").mkdir(parents=True)
        (self.donor / "src").mkdir(parents=True)

    def tearDown(self) -> None:
        self.temp.cleanup()

    @patch("normalize_release_donor.verify_checkout")
    def test_maven_command_binds_explicit_donor_root_revision_and_profile(self, verify) -> None:
        verify.return_value = normalizer.DonorRun(
            22, "jdk-22+36", self.donor.resolve(), Path()
        )
        command = normalizer.maven_command(
            self.control, self.donor, 22, self.output, threads=7
        )
        self.assertIn("-Pm3-jdk-donor-convergence", command)
        self.assertIn(f"-Dm3.donor.root={self.donor.resolve()}", command)
        self.assertIn("-Dm3.donor.revision=jdk-22+36", command)
        self.assertIn(f"-Dm3.donor.output={self.output.resolve()}", command)
        self.assertIn("-Dm3.donor.threads=7", command)
        self.assertEqual("verify", command[-1])

    @patch("normalize_release_donor.verify_checkout")
    def test_maven_command_rejects_nonpositive_threads(self, verify) -> None:
        with self.assertRaisesRegex(ValueError, "threads"):
            normalizer.maven_command(
                self.control, self.donor, 22, self.output, threads=0
            )
        verify.assert_not_called()

    def test_verify_identity_requires_typed_exact_revision_and_manifest(self) -> None:
        self.output.mkdir()
        (self.output / "SOURCE_CONVERGENCE.tsv").write_text(
            "path\tpreSha256\tpostSha256\tfixedPoint\tstatus\tcandidate\n",
            encoding="utf-8",
        )
        identity = self.output / "SOURCE_CONVERGENCE.image.tsv"
        identity.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"DONOR\tjdk-22+36\t{'a' * 64}\t10\t2\t1\n",
            encoding="utf-8",
        )
        self.assertEqual("a" * 64, normalizer.verify_identity(self.output, "jdk-22+36"))

        identity.write_text(
            "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
            f"BASELINE\tjdk-22+36\t{'a' * 64}\t10\t2\t1\n",
            encoding="utf-8",
        )
        with self.assertRaisesRegex(ValueError, "identity mismatch"):
            normalizer.verify_identity(self.output, "jdk-22+36")

    @patch("normalize_release_donor.maven_command")
    @patch("normalize_release_donor.expected_ref")
    def test_run_executes_command_then_requires_identity(
        self, expected_ref, maven_command
    ) -> None:
        expected_ref.return_value = "f3701c80216900f3ded26f9de1befe43813be95c"
        maven_command.return_value = ("mvn", "verify")
        observed: list[tuple[str, ...]] = []

        def runner(command) -> None:
            observed.append(tuple(command))
            self.output.mkdir(parents=True)
            (self.output / "SOURCE_CONVERGENCE.tsv").write_text(
                "path\tpreSha256\tpostSha256\tfixedPoint\tstatus\tcandidate\n",
                encoding="utf-8",
            )
            (self.output / "SOURCE_CONVERGENCE.image.tsv").write_text(
                "role\trevision\tsemanticRoot\tfiles\tchanged\tholds\n"
                "DONOR\tf3701c80216900f3ded26f9de1befe43813be95c\t"
                + "b" * 64
                + "\t0\t0\t0\n",
                encoding="utf-8",
            )

        root = normalizer.run(
            self.control, self.donor, 27, self.output, runner=runner
        )
        self.assertEqual("b" * 64, root)
        self.assertEqual([("mvn", "verify")], observed)


if __name__ == "__main__":
    unittest.main()
