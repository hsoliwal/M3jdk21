# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import contextlib
import io
import tempfile
import unittest
from pathlib import Path

import check_m3_public_target as policy


class M3PublicTargetPolicyTest(unittest.TestCase):
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
        self.assertIn(
            (
                "SYNEXIA_FIRST_PARTY",
                "Apache-2.0",
                "JDK_SEPARATE_COMPONENT",
                "COPY_WITH_LEGAL_NOTICE",
                "Apache-2.0",
            ),
            rows,
        )

    def test_checked_in_apache_handoff_pin_is_qualification_only(self) -> None:
        pin = policy.load_apache_handoff_pin()
        self.assertEqual("hsoliwal/com.synexia", pin["source_repository"])
        self.assertEqual("9642", pin["source_pr"])
        self.assertEqual("Apache-2.0", pin["source_license"])
        self.assertEqual("synexia-m3-recipe", pin["canonical_owner"])
        self.assertEqual("Copyright 2026 Hitesh Soliwal and contributors", pin["copyright_notice"])
        self.assertEqual("SOURCE_MERGED_PROOF_PENDING", pin["delivery_state"])
        self.assertEqual("QUALIFICATION_INPUT_ONLY", pin["target_role"])
        self.assertEqual("false", pin["automatic_application"])
        self.assertEqual("false", pin["target_relicense_authority"])
        self.assertEqual(
            "GPL-2.0-only WITH Classpath-exception-2.0",
            pin["openjdk_retained_license"],
        )

    def test_merged_proof_pending_apache_handoff_pin_cannot_enable_application_or_relicense(self) -> None:
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

    def test_policy_preserves_source_attribution_and_license_boundaries(self) -> None:
        checked = policy.POLICY.read_text(encoding="utf-8")
        changes = (
            ("SYNEXIA_FIRST_PARTY", "M3_FIRST_PARTY"),
            ("SYNEXIA_FIRST_PARTY", "THIRD_PARTY"),
            ("Apache-2.0", "MIT"),
            ("COPY_WITH_LEGAL_NOTICE", "COPY_PRESERVE_LICENSE"),
            ("SEPARATE_ARTIFACT_REVIEW", "COPY_PRESERVE_LICENSE"),
            ("NO_SOURCE_COPY", "COPY_PRESERVE_LICENSE"),
            ("GPL-2.0-only WITH Classpath-exception-2.0", "Apache-2.0"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "policy.tsv"
            for before, after in changes:
                with self.subTest(before=before, after=after):
                    changed = checked.replace(before, after)
                    self.assertNotEqual(checked, changed)
                    path.write_text(changed, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "policy drift"):
                        policy.load_policy(path)

    def test_duplicate_policy_owner_refuses_even_with_another_action(self) -> None:
        checked = policy.POLICY.read_text(encoding="utf-8")
        first = checked.splitlines()[1]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "duplicate.tsv"
            for row in (first, first.replace("COPY_PRESERVE_LICENSE", "NO_SOURCE_COPY")):
                with self.subTest(row=row):
                    path.write_text(checked + row + "\n", encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "duplicate policy row"):
                        policy.load_policy(path)

    def test_policy_requires_complete_nonempty_rows_and_exact_header(self) -> None:
        checked = policy.POLICY.read_text(encoding="utf-8")
        cases = (
            ("empty", "", "empty"),
            ("header", checked.replace("source_class", "source_owner", 1), "header"),
            ("short-row", checked + "REFERENCE_ONLY\t*\n", "invalid policy row"),
            ("empty-cell", checked + "\t*\tANY\tNO_SOURCE_COPY\tN/A\n", "invalid policy row"),
            ("missing-row", "\n".join(checked.splitlines()[:-1]) + "\n", "policy drift"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "policy.tsv"
            for label, body, reason in cases:
                with self.subTest(label=label):
                    path.write_text(body, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        policy.load_policy(path)

    def test_handoff_pin_preserves_canonical_owner_and_historical_identity(self) -> None:
        checked = policy.APACHE_HANDOFF_PIN.read_text(encoding="utf-8")
        changes = (
            ("hsoliwal/com.synexia", "hsoliwal/M3jdk21"),
            ("synexia-m3-recipe", "m3-recipe"),
            ("Copyright 2026 Hitesh Soliwal and contributors", "Copyright target project"),
            ("25489c202af43863bda2ae23b67542c363e92450", "0" * 40),
            ("98274241cc6a0655e600d5fb5c58d70170aca0111770f95c14609041a1f1289a", "0" * 64),
            ("SOURCE_MERGED_PROOF_PENDING", "QUALIFIED"),
            ("QUALIFICATION_INPUT_ONLY", "RUNTIME_ADMITTED"),
            ("GPL-2.0-only WITH Classpath-exception-2.0", "Apache-2.0"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pin.tsv"
            for before, after in changes:
                with self.subTest(before=before):
                    changed = checked.replace(before, after)
                    self.assertNotEqual(checked, changed)
                    path.write_text(changed, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "pin drift"):
                        policy.load_apache_handoff_pin(path)

    def test_handoff_pin_requires_complete_nonempty_unique_fields(self) -> None:
        checked = policy.APACHE_HANDOFF_PIN.read_text(encoding="utf-8")
        cases = (
            ("empty", "", "empty"),
            ("header", checked.replace("field\tvalue", "key\tvalue", 1), "header"),
            ("short-row", checked + "extra\n", "invalid Apache handoff pin row"),
            ("empty-field", checked + "\tvalue\n", "invalid Apache handoff pin row"),
            ("empty-value", checked + "extra\t\n", "invalid Apache handoff pin row"),
            ("duplicate", checked + "canonical_owner\tsynexia-m3-recipe\n", "duplicate"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "pin.tsv"
            for label, body, reason in cases:
                with self.subTest(label=label):
                    path.write_text(body, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        policy.load_apache_handoff_pin(path)

    def test_packet_refuses_renamed_or_unknown_attribution_policy(self) -> None:
        body = _packet("Apache-2.0")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            for renamed in ("M3_FIRST_PARTY_APACHE2_V1", "THIRD_PARTY_APACHE2_V1", "SYNEXIA_FIRST_PARTY_APACHE2_V2"):
                with self.subTest(policy=renamed):
                    path.write_text(body.replace("SYNEXIA_FIRST_PARTY_APACHE2_V1", renamed), encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "first-party Apache"):
                        policy.validate_packet(path)

    def test_packet_requires_exact_version_and_unambiguous_header_and_rows(self) -> None:
        body = _packet("Apache-2.0")
        cases = (
            ("empty", "", "version"),
            ("version", body.replace("M3_HANDOFF_V1", "M3_HANDOFF_V2", 1), "version"),
            ("header", body.replace("@crate\tsynexia-example-v1", "@crate\tx\ty", 1), "header row"),
            ("duplicate-header", body + "@license-policy\tSYNEXIA_FIRST_PARTY_APACHE2_V1\n", "duplicate packet header"),
            ("row-marker", body.replace("ROW\tJAVA", "OTHER\tJAVA", 1), "packet row"),
            ("short-row", body + "ROW\tJAVA\n", "packet row"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            for label, changed, reason in cases:
                with self.subTest(label=label):
                    path.write_text(changed, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        policy.validate_packet(path)

    def test_packet_requires_immutable_source_revision_and_nonempty_payload(self) -> None:
        body = _packet("Apache-2.0")
        revision = "0123456789abcdef0123456789abcdef01234567"
        cases = (
            ("missing-revision", body.replace("@source-revision\t" + revision + "\n", ""), "source revision"),
            ("invalid-revision", body.replace(revision, "not-a-commit"), "source revision"),
            ("empty-payload", "\n".join(body.splitlines()[:-1]) + "\n", "empty Synexia handoff packet"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            for label, changed, reason in cases:
                with self.subTest(label=label):
                    path.write_text(changed, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        policy.validate_packet(path)

    def test_packet_source_and_target_preimage_seals_are_independent(self) -> None:
        body = _packet("Apache-2.0")
        cases = (
            ("repeated-source", body.replace("ABSENT\t" + "a" * 64, "ABSENT\t" + "b" * 64), "source seal"),
            ("unsealed-preimage", body.replace("ABSENT", "not-a-seal"), "target preimage"),
        )
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            for label, changed, reason in cases:
                with self.subTest(label=label):
                    path.write_text(changed, encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, reason):
                        policy.validate_packet(path)

    def test_packet_refuses_noncanonical_target_paths(self) -> None:
        body = _packet("Apache-2.0")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            for target in ("/absolute/A.java", "m3\\A.java", "m3//A.java", "m3/./A.java", "m3/../A.java", ".git/config", "m3/A.java/"):
                with self.subTest(target=target):
                    path.write_text(body.replace("m3/ports/example/A.java", target), encoding="utf-8")
                    with self.assertRaisesRegex(ValueError, "noncanonical target"):
                        policy.validate_packet(path)

    def test_packet_accepts_exact_preimages_and_multiple_distinct_targets(self) -> None:
        body = _packet("Apache-2.0").replace("ABSENT", "d" * 64)
        second = body.splitlines()[-1].replace("m3/ports/example/A.java", "m3/ports/example/B.java")
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "packet.tsv"
            path.write_text(body + "\n" + second + "\n", encoding="utf-8")
            self.assertEqual(2, policy.validate_packet(path))

    def test_cli_reports_policy_or_packet_scope_and_rejects_extra_arguments(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            packet = Path(directory) / "packet.tsv"
            packet.write_text(_packet("Apache-2.0"), encoding="utf-8")
            cases = (
                (["check_m3_public_target.py"], 0, "policy=checked apache_handoff=qualification-only", ""),
                (["check_m3_public_target.py", str(packet)], 0, "rows=1 license=Apache-2.0", ""),
                (["check_m3_public_target.py", str(packet), "extra"], 2, "", "usage:"),
            )
            for argv, status, message, error in cases:
                with self.subTest(argv=argv):
                    stdout, stderr = io.StringIO(), io.StringIO()
                    with contextlib.redirect_stdout(stdout), contextlib.redirect_stderr(stderr):
                        self.assertEqual(status, policy.main(argv))
                    self.assertIn(message, stdout.getvalue())
                    self.assertIn(error, stderr.getvalue())


def _packet(license_id: str) -> str:
    sha = "a" * 64
    contract = "b" * 64
    gate = "c" * 64
    return (
        "# M3_HANDOFF_V1\n"
        "@crate\tsynexia-example-v1\n"
        "@source-revision\t0123456789abcdef0123456789abcdef01234567\n"
        "@license-policy\tSYNEXIA_FIRST_PARTY_APACHE2_V1\n"
        f"ROW\tJAVA\tcap.example\tsrc/A.java\t{sha}\tm3/ports/example/A.java\tABSENT\t{sha}"
        f"\t0000-A.java.txt\t{license_id}\trecipe.example\t{contract}\t{gate}\n"
    )


if __name__ == "__main__":
    unittest.main()
