# SPDX-License-Identifier: Apache-2.0
from __future__ import annotations

import csv
import hashlib
import unittest
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECEIVER = ROOT / "tooling" / "migration-recipes" / "receiver" / "synexia-recipe-mirror-v2"
SNAPSHOT = RECEIVER / "m3jdk21-recipe-tree-audit-v2.tsv"
SNAPSHOT_ROOT = RECEIVER / "m3jdk21-recipe-tree-audit-v2.root"
SOURCE_PROVENANCE = RECEIVER / "SOURCE_PROVENANCE.tsv"
VERIFIER_PROVENANCE = RECEIVER / "VERIFIER_PROVENANCE.tsv"
POM = ROOT / "tooling" / "migration-recipes" / "pom.xml"
WORKFLOW = ROOT.parent / ".github" / "workflows" / "m3-synexia-recipe-home.yml"

TARGET_REVISION = "80b8534ad1e45002185fb3eeab233f98d3a7c0bd"
SNAPSHOT_SOURCE_REVISION = "e0e323fabb016d95a21417f93d4b4262d6514356"
VERIFIER_SOURCE_REVISION = "9264e2aa35ca58c28fc40e361811949a50163fd6"
SNAPSHOT_CONTENT_ROOT = "f9554fad00e93eb4fc721b1a8a114b44c3e987d7394e656032b2550379ffb809"
VERIFIER_COORDINATE = (
    "com.synexia:synexia-m3jdk21-recipe-mirror-verifier:1.0.0-SNAPSHOT"
)


class SynexiaRecipeMirrorReceiverV2Test(unittest.TestCase):
    def test_received_snapshot_root_and_ownership_split_are_exact(self) -> None:
        payload = SNAPSHOT.read_bytes()
        self.assertEqual(SNAPSHOT_CONTENT_ROOT, hashlib.sha256(payload).hexdigest())
        self.assertEqual(SNAPSHOT_CONTENT_ROOT, SNAPSHOT_ROOT.read_text().strip())

        with SNAPSHOT.open(encoding="utf-8", newline="") as handle:
            rows = list(csv.DictReader(handle, delimiter="\t"))

        self.assertEqual(47, len(rows))
        self.assertEqual({TARGET_REVISION}, {row["target_revision"] for row in rows})
        self.assertEqual({"hsoliwal/M3jdk21"}, {row["target_repository"] for row in rows})
        self.assertEqual(47, len({row["class"] for row in rows}))
        self.assertEqual(47, len({row["path"] for row in rows}))
        self.assertEqual(
            Counter(
                {
                    "SYNEXIA_CANONICAL_RESIDUE": 21,
                    "TARGET_ADAPTER_ONLY": 2,
                    "JDK_TARGET_SPECIFIC": 24,
                }
            ),
            Counter(row["disposition"] for row in rows),
        )

        for row in rows:
            self.assertRegex(row["git_blob"], r"^[0-9a-f]{40}$")
            if row["disposition"] == "SYNEXIA_CANONICAL_RESIDUE":
                self.assertEqual("FROZEN_NO_LOCAL_EVOLUTION", row["local_evolution_policy"])
                self.assertTrue(row["canonical_synexia_owner"].startswith("com.synexia"))
            elif row["disposition"] == "TARGET_ADAPTER_ONLY":
                self.assertEqual("THIN_RECEIVER_ONLY", row["local_evolution_policy"])
                self.assertTrue(row["canonical_synexia_owner"].startswith("com.synexia"))
            else:
                self.assertEqual("TARGET_LOCAL_ALLOWED", row["local_evolution_policy"])

    def test_handoff_and_portable_verifier_provenance_are_pinned(self) -> None:
        with SOURCE_PROVENANCE.open(encoding="utf-8", newline="") as handle:
            source_rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(2, len(source_rows))
        for row in source_rows:
            self.assertEqual("hsoliwal/com.synexia", row["source_repository"])
            self.assertEqual(SNAPSHOT_SOURCE_REVISION, row["source_revision"])
            self.assertEqual("Apache-2.0", row["license"])
            self.assertEqual("PROOF_BOUND_HANDOFF", row["disposition"])
            self.assertEqual(SNAPSHOT_CONTENT_ROOT, row["content_identity"])
            self.assertRegex(row["source_git_blob"], r"^[0-9a-f]{40}$")

        with VERIFIER_PROVENANCE.open(encoding="utf-8", newline="") as handle:
            verifier_rows = list(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual(1, len(verifier_rows))
        verifier = verifier_rows[0]
        self.assertEqual("hsoliwal/com.synexia", verifier["source_repository"])
        self.assertEqual(VERIFIER_SOURCE_REVISION, verifier["source_revision"])
        self.assertEqual(VERIFIER_COORDINATE, verifier["maven_coordinate"])
        self.assertEqual("Apache-2.0", verifier["license"])
        self.assertEqual("DEPENDENCY_ONLY_CANONICAL_SYNEXIA", verifier["disposition"])
        self.assertRegex(verifier["source_git_blob"], r"^[0-9a-f]{40}$")

    def test_maven_profile_depends_only_on_slim_synexia_verifier(self) -> None:
        tree = ET.parse(POM)
        ns = {"m": "http://maven.apache.org/POM/4.0.0"}
        root = tree.getroot()
        profiles = root.findall("m:profiles/m:profile", ns)
        profile = next(
            p
            for p in profiles
            if p.find("m:id", ns) is not None
            and p.find("m:id", ns).text == "m3-synexia-recipe-mirror-v2"
        )
        dependency = profile.find("m:dependencies/m:dependency", ns)
        self.assertIsNotNone(dependency)
        self.assertEqual("com.synexia", dependency.find("m:groupId", ns).text)
        self.assertEqual(
            "synexia-m3jdk21-recipe-mirror-verifier",
            dependency.find("m:artifactId", ns).text,
        )
        self.assertEqual("${m3.synexia.version}", dependency.find("m:version", ns).text)

        execution = profile.find(
            "m:build/m:plugins/m:plugin/m:executions/m:execution", ns
        )
        self.assertIsNotNone(execution)
        self.assertEqual(
            "verify-proof-bound-synexia-recipe-mirror",
            execution.find("m:id", ns).text,
        )
        self.assertEqual(
            "com.synexia.rewrite.M3Jdk21RecipeMirrorSnapshotCli",
            execution.find("m:configuration/m:mainClass", ns).text,
        )
        arguments = [
            arg.text for arg in execution.findall("m:configuration/m:arguments/m:argument", ns)
        ]
        self.assertEqual(TARGET_REVISION, arguments[-1])

    def test_target_contains_no_copy_of_synexia_verifier_implementation(self) -> None:
        source_root = (
            ROOT
            / "tooling"
            / "migration-recipes"
            / "src"
            / "main"
            / "java"
        )
        forbidden = {
            "M3Jdk21RecipeMirrorSnapshot.java",
            "M3Jdk21RecipeMirrorSnapshotCli.java",
        }
        actual = {path.name for path in source_root.rglob("*.java")}
        self.assertTrue(forbidden.isdisjoint(actual))

    def test_workflow_builds_exact_synexia_source_then_invokes_receiver_profile(self) -> None:
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("repository: hsoliwal/com.synexia", workflow)
        self.assertIn(f"ref: {VERIFIER_SOURCE_REVISION}", workflow)
        self.assertIn("pom-m3jdk21-recipe-mirror-verifier.xml", workflow)
        self.assertIn("-P m3-synexia-recipe-mirror-v2", workflow)
        self.assertIn(
            "java@verify-proof-bound-synexia-recipe-mirror",
            workflow,
        )


if __name__ == "__main__":
    unittest.main()
