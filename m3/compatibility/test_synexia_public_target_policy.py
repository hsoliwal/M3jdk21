# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

import check_synexia_public_target as policy


class SynexiaPublicTargetPolicyTest(unittest.TestCase):
    def test_checked_in_policy_is_exact(self) -> None:
        rows = policy.load_policy()
        self.assertEqual(6, len(rows))
        self.assertIn(
            (
                "SYNEXIA_FIRST_PARTY",
                "Apache-2.0",
                "M3_TOOLING",
                "COPY_PRESERVE_LICENSE",
                "Apache-2.0",
            ),
            rows,
        )
        self.assertIn(
            (
                "SYNEXIA_FIRST_PARTY",
                "Apache-2.0",
                "M3_RECIPE",
                "COPY_PRESERVE_LICENSE",
                "Apache-2.0",
            ),
            rows,
        )

    def test_checked_in_apache_handoff_pin_is_qualification_only(self) -> None:
        pin = policy.load_apache_handoff_pin()
        self.assertEqual("hsoliwal/com.synexia", pin["source_repository"])
        self.assertEqual("9642", pin["source_pr"])
        self.assertEqual("Apache-2.0", pin["source_license"])
        self.assertEqual("PENDING_SYNEXIA_MERGE", pin["delivery_state"])
        self.assertEqual("QUALIFICATION_INPUT_ONLY", pin["target_role"])
        self.assertEqual("false", pin["automatic_application"])
        self.assertEqual("false", pin["target_relicense_authority"])
        self.assertEqual(
            "GPL-2.0-only WITH Classpath-exception-2.0",
            pin["openjdk_retained_license"],
        )

    def test_unmerged_apache_handoff_pin_cannot_enable_application_or_relicense(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            checked = policy.APACHE_HANDOFF_PIN.read_text(encoding="utf-8")

            automatic = root / "automatic.tsv"
            automatic.write_text(
                checked.replace("automatic_application\tfalse", "automatic_application\ttrue"),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "pin drift|automatic"):
                policy.load_apache_handoff_pin(automatic)

            relicense = root / "relicense.tsv"
            relicense.write_text(
                checked.replace("target_relicense_authority\tfalse", "target_relicense_authority\ttrue"),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "pin drift|relicense"):
                policy.load_apache_handoff_pin(relicense)

    def test_valid_first_party_packet_is_admitted(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            packet = Path(directory) / "packet.tsv"
            packet.write_text(_packet("Apache-2.0"), encoding="utf-8")
            self.assertEqual(1, policy.validate_packet(packet))

    def test_non_apache_or_missing_policy_refuses(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bad_license = root / "bad-license.tsv"
            bad_license.write_text(_packet("MIT"), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "non-Apache"):
                policy.validate_packet(bad_license)

            no_policy = root / "no-policy.tsv"
            no_policy.write_text(
                _packet("Apache-2.0").replace(
                    "@license-policy\tSYNEXIA_FIRST_PARTY_APACHE2_V1\n", ""
                ),
                encoding="utf-8",
            )
            with self.assertRaisesRegex(ValueError, "first-party Apache"):
                policy.validate_packet(no_policy)

    def test_duplicate_target_and_bad_seal_refuse(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            duplicate = root / "duplicate.tsv"
            body = _packet("Apache-2.0")
            row = body.splitlines()[-1]
            duplicate.write_text(body + row + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate target"):
                policy.validate_packet(duplicate)

            bad_sha = root / "bad-sha.tsv"
            bad_sha.write_text(body.replace("a" * 64, "z" * 64, 1), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "source seal"):
                policy.validate_packet(bad_sha)


def _packet(license_id: str) -> str:
    sha = "a" * 64
    contract = "b" * 64
    gate = "c" * 64
    return (
        "# SYNEXIA_M3_HANDOFF_V1\n"
        "@crate\tsynexia-example-v1\n"
        "@source-revision\t0123456789abcdef0123456789abcdef01234567\n"
        "@license-policy\tSYNEXIA_FIRST_PARTY_APACHE2_V1\n"
        f"ROW\tJAVA\tcap.example\tsrc/A.java\t{sha}\tm3/ports/example/A.java\tABSENT\t{sha}"
        f"\t0000-A.java.txt\t{license_id}\trecipe.example\t{contract}\t{gate}\n"
    )


if __name__ == "__main__":
    unittest.main()
