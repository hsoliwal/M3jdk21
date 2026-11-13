# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import tempfile
import unittest
from pathlib import Path

import compatibility_policy


class CompatibilityPolicyTest(unittest.TestCase):
    def test_checked_in_ledger_is_valid(self) -> None:
        entries = compatibility_policy.load()
        by_jep = {entry.jep: entry for entry in entries}

        self.assertEqual("COMPATIBLE_TOOLING", by_jep[458].classification)
        self.assertEqual("OPT_IN_SOURCE_TOOLING", by_jep[467].classification)
        self.assertEqual("OPT_IN_SE_API_EXTENSION", by_jep[485].classification)
        self.assertEqual("COMPATIBLE_TOOLING", by_jep[493].classification)
        self.assertEqual("REJECT_LANGUAGE_SPEC", by_jep[511].classification)
        self.assertEqual("REJECT_JVMS_LANGUAGE_SPEC", by_jep[401].classification)

        self.assertEqual((483,), by_jep[514].dependencies)
        self.assertEqual((483,), by_jep[515].dependencies)
        self.assertEqual((483, 514, 515), by_jep[516].dependencies)

        for entry in entries:
            if entry.classification.startswith("REJECT_"):
                self.assertEqual("NO", entry.default_java21)
            if entry.classification.startswith("OPT_IN_"):
                self.assertEqual("NO", entry.default_java21)

    def test_language_or_api_extension_cannot_be_marked_candidate_default(self) -> None:
        row = _row(
            jep="511",
            release="25",
            classification="REJECT_LANGUAGE_SPEC",
            default_java21="CANDIDATE",
        )
        with self.assertRaisesRegex(ValueError, "cannot be default Java21"):
            _load_rows([row])

        row = _row(
            jep="485",
            release="24",
            classification="OPT_IN_SE_API_EXTENSION",
            default_java21="OPT_IN",
        )
        with self.assertRaisesRegex(ValueError, "cannot be default Java21"):
            _load_rows([row])

    def test_rejected_feature_cannot_be_opt_in_default_marker(self) -> None:
        row = _row(
            jep="401",
            release="26",
            classification="REJECT_JVMS_LANGUAGE_SPEC",
            default_java21="OPT_IN",
        )
        with self.assertRaises(ValueError):
            _load_rows([row])

    def test_dependencies_must_be_present_and_not_from_later_release(self) -> None:
        missing = _row(
            jep="515",
            release="25",
            classification="COMPATIBLE_RUNTIME",
            default_java21="CANDIDATE",
            dependency="483",
        )
        with self.assertRaisesRegex(ValueError, "dependency 483 is absent"):
            _load_rows([missing])

        dependency = _row(
            jep="483",
            release="26",
            classification="COMPATIBLE_RUNTIME",
            default_java21="CANDIDATE",
        )
        dependent = _row(
            jep="515",
            release="25",
            classification="COMPATIBLE_RUNTIME",
            default_java21="CANDIDATE",
            dependency="483",
        )
        with self.assertRaisesRegex(ValueError, "depends on later-release"):
            _load_rows([dependency, dependent])

    def test_duplicate_bad_url_and_pre22_rows_fail_closed(self) -> None:
        row = _row(
            jep="458",
            release="22",
            classification="COMPATIBLE_TOOLING",
            default_java21="OPT_IN",
        )
        with self.assertRaisesRegex(ValueError, "duplicate JEP"):
            _load_rows([row, row])

        bad_url = dict(row)
        bad_url["evidence_url"] = "https://example.invalid/458"
        with self.assertRaisesRegex(ValueError, "non-canonical"):
            _load_rows([bad_url])

        pre22 = dict(row)
        pre22["release"] = "21"
        with self.assertRaisesRegex(ValueError, "pre-22"):
            _load_rows([pre22])

    def test_empty_and_invalid_fields_fail_closed(self) -> None:
        with self.assertRaisesRegex(ValueError, "empty"):
            _load_rows([])

        row = _row(
            jep="x",
            release="22",
            classification="COMPATIBLE_TOOLING",
            default_java21="OPT_IN",
        )
        with self.assertRaisesRegex(ValueError, "invalid jep"):
            _load_rows([row])

        row = _row(
            jep="458",
            release="22",
            classification="COMPATIBLE_TOOLING",
            default_java21="YES",
        )
        with self.assertRaisesRegex(ValueError, "invalid default_java21"):
            _load_rows([row])

        row = _row(
            jep="458",
            release="22",
            classification="COMPATIBLE_TOOLING",
            default_java21="NO",
        )
        with self.assertRaisesRegex(ValueError, "CANDIDATE or OPT_IN"):
            _load_rows([row])


def _row(
    *,
    jep: str,
    release: str,
    classification: str,
    default_java21: str,
    dependency: str = "",
) -> dict[str, str]:
    return {
        "jep": jep,
        "release": release,
        "title": "Test feature",
        "classification": classification,
        "default_java21": default_java21,
        "dependency": dependency,
        "owner_lane": "TEST",
        "evidence_url": f"https://openjdk.org/jeps/{jep}",
        "note": "test row",
    }


def _load_rows(rows: list[dict[str, str]]) -> list[compatibility_policy.Entry]:
    with tempfile.TemporaryDirectory() as directory:
        path = Path(directory) / "ledger.tsv"
        fieldnames = [
            "jep",
            "release",
            "title",
            "classification",
            "default_java21",
            "dependency",
            "owner_lane",
            "evidence_url",
            "note",
        ]
        with path.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.DictWriter(stream, fieldnames=fieldnames, delimiter="	")
            writer.writeheader()
            writer.writerows(rows)
        return compatibility_policy.load(path)


if __name__ == "__main__":
    unittest.main()
