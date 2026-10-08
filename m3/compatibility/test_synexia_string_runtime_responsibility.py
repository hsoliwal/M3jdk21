# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import re
import subprocess
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TABLE = ROOT / "compatibility" / "synexia-string-runtime-responsibility-20261008.tsv"
HEX40 = re.compile(r"^[0-9a-f]{40}$")

EXPECTED_ROLES = {
    "VALUE_HANDLE",
    "POOL_INTERNING",
    "TUPLE_COMPOSITION",
    "SCALAR_OWNER",
    "SCALAR_STORAGE",
    "CANONICAL_FACTS",
    "SEARCH_PLAN",
    "POSITION_MASK_SEARCH",
    "TRIGRAM_QUERY",
    "UTF16_SHADOW",
    "UTF8_SHADOW",
}


def git_blob(path: Path) -> str:
    data = path.read_bytes()
    return hashlib.sha1(f"blob {len(data)}\0".encode("ascii") + data).hexdigest()


class SynexiaStringRuntimeResponsibilityTest(unittest.TestCase):
    def test_exact_target_owners_match_source_declared_responsibility(self) -> None:
        with TABLE.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(len(EXPECTED_ROLES), len(rows))
        self.assertEqual(EXPECTED_ROLES, {row["source_role"] for row in rows})
        self.assertEqual(
            {"296323958b1019edd59b60b9c05cb148d024cfe5"},
            {row["source_snapshot_revision"] for row in rows},
        )
        self.assertEqual(
            {"c9b07049c57ecdf43175f5885e11998918416a00"},
            {row["target_revision"] for row in rows},
        )

        for row in rows:
            self.assertEqual("M3JDK_STRING_RUNTIME_RESPONSIBILITY_V1", row["schema"])
            self.assertEqual("hsoliwal/M3jdk21", row["target_repository"])
            self.assertRegex(row["source_git_blob"], HEX40)
            self.assertRegex(row["target_git_blob"], HEX40)
            self.assertEqual(
                "TARGET_OWNER_PRESENT_NOT_FAMILY_ACCEPTED",
                row["state"],
            )
            self.assertTrue(row["unresolved_gates"])
            target = ROOT.parent / row["target_path"]
            self.assertTrue(target.is_file(), row["target_path"])
            self.assertEqual(row["target_git_blob"], git_blob(target), row["target_path"])

    def test_license_boundary_is_explicit_per_target_owner(self) -> None:
        with TABLE.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        for row in rows:
            if row["mapping_kind"] == "SEPARATE_APACHE_COMPONENT":
                self.assertEqual("Apache-2.0", row["target_license_class"])
                self.assertEqual(
                    "src/java.base/share/classes/jdk/internal/mindex/M3TQ.java",
                    row["target_path"],
                )
            else:
                self.assertIn(
                    row["mapping_kind"],
                    {"TARGET_OWNED_ADAPTATION", "TARGET_NATIVE_ADAPTATION"},
                )
                self.assertEqual(
                    "OPENJDK_GPL2_CLASSPATH",
                    row["target_license_class"],
                )

    def test_target_revision_is_ancestor_but_does_not_grant_family_acceptance(self) -> None:
        with TABLE.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))
        revision = rows[0]["target_revision"]
        result = subprocess.run(
            ["git", "-C", str(ROOT.parent), "merge-base", "--is-ancestor", revision, "HEAD"],
            check=False,
        )
        self.assertEqual(0, result.returncode)
        self.assertTrue(all(row["state"].endswith("NOT_FAMILY_ACCEPTED") for row in rows))


if __name__ == "__main__":
    unittest.main()
