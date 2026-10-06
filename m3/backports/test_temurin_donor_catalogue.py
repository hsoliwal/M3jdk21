#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


def rows(path: str) -> list[dict[str, str]]:
    with (ROOT / path).open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter="\t"))


class TemurinDonorCatalogueTest(unittest.TestCase):
    def test_adoptium_mirror_is_explicitly_non_unique_and_non_authoritative(self) -> None:
        forks = {row["fork_id"]: row for row in rows("m3/backports/COMMUNITY_FORKS.tsv")}
        upstream = forks["upstream21u"]
        adoptium = forks["adoptium-jdk21u"]

        self.assertEqual("openjdk/jdk21u", upstream["repository"])
        self.assertEqual("adoptium/jdk21u", adoptium["repository"])
        self.assertEqual(upstream["pinned_head"], adoptium["pinned_head"])
        self.assertEqual("MIRROR", adoptium["plane"])
        self.assertEqual("false", adoptium["source_copy_authority"])
        self.assertEqual("false", adoptium["selected_for_distribution"])
        self.assertIn("zero unique-source claim", adoptium["notes"])

    def test_tooling_donors_are_pinned_apache2_and_authority_false(self) -> None:
        tooling = {row["donor_id"]: row for row in rows(
            "m3/backports/COMMUNITY_TOOLING_DONORS.tsv"
        )}
        self.assertEqual({"temurin-build", "aqavit-tests"}, set(tooling))

        expected = {
            "temurin-build": (
                "adoptium/temurin-build",
                "cc31225e0aad72a5598d94e174cdd5c09e0d85a8",
                "BUILD_TOOLING",
            ),
            "aqavit-tests": (
                "adoptium/aqa-tests",
                "6772f0835e011b175b4c1ad3a708f35912b6d249",
                "TEST_AND_PERFORMANCE",
            ),
        }
        for donor, (repository, commit, plane) in expected.items():
            row = tooling[donor]
            self.assertEqual(repository, row["repository"])
            self.assertEqual("master", row["ref"])
            self.assertEqual(commit, row["pinned_head"])
            self.assertEqual(plane, row["plane"])
            self.assertEqual("Apache-2.0", row["license"])
            self.assertEqual(
                "d5dd862b1759bce588502bc0a7d4040e5fa9154e",
                row["license_blob"],
            )
            self.assertEqual("false", row["copy_authority"])
            self.assertEqual("false", row["selected_for_distribution"])
            self.assertEqual("ADAPT_OR_WRAP", row["integration_mode"])

    def test_capability_candidates_are_evidence_only_and_reference_known_donors(self) -> None:
        tooling = {row["donor_id"] for row in rows(
            "m3/backports/COMMUNITY_TOOLING_DONORS.tsv"
        )}
        capabilities = rows("m3/backports/TEMURIN_CAPABILITY_CANDIDATES.tsv")
        ids = [row["candidate_id"] for row in capabilities]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(8, len(capabilities))

        for row in capabilities:
            self.assertIn(row["donor_id"], tooling)
            self.assertEqual("PENDING_MECHANISM_REVIEW", row["status"])
            self.assertEqual("false", row["copy_authority"])
            self.assertEqual("false", row["promotion_authority"])
            self.assertTrue(row["mechanism"])
            self.assertTrue(row["m3_target"])
            self.assertTrue(row["next_proof"])


if __name__ == "__main__":
    unittest.main()
