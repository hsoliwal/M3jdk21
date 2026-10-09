#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("backport_recipe_dag.py")
    spec = importlib.util.spec_from_file_location("m3_backport_recipe_dag", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class BackportRecipeDagTest(unittest.TestCase):
    def setUp(self) -> None:
        self.mod = load_module()

    @staticmethod
    def write_tsv(path: Path, fieldnames: list[str], rows: list[dict[str, str]]) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(
                handle, fieldnames=fieldnames, delimiter="\t", lineterminator="\n"
            )
            writer.writeheader()
            writer.writerows(rows)

    def fixture(self, root: Path) -> None:
        backports = root / "m3" / "backports"
        recipes = backports / "recipes"
        (recipes / "jep-423-region-pinning").mkdir(parents=True)
        (recipes / "jdk-9000001-tool").mkdir(parents=True)

        self.write_tsv(
            backports / "JEP_CATALOGUE.tsv",
            ["release", "jep", "title", "domain", "disposition", "reason", "superseded_by"],
            [
                {
                    "release": "22",
                    "jep": "423",
                    "title": "Region Pinning for G1",
                    "domain": "gc",
                    "disposition": "candidate",
                    "reason": "runtime candidate",
                    "superseded_by": "",
                },
                {
                    "release": "24",
                    "jep": "484",
                    "title": "Class-File API",
                    "domain": "library",
                    "disposition": "candidate",
                    "reason": "library candidate",
                    "superseded_by": "",
                },
                {
                    "release": "25",
                    "jep": "511",
                    "title": "Module Import Declarations",
                    "domain": "language",
                    "disposition": "reject-language",
                    "reason": "grammar change",
                    "superseded_by": "",
                },
                {
                    "release": "22",
                    "jep": "461",
                    "title": "Stream Gatherers Preview",
                    "domain": "library",
                    "disposition": "superseded",
                    "reason": "use final",
                    "superseded_by": "485",
                },
            ],
        )

        self.write_tsv(
            backports / "UPSTREAM_CHANGE_SEEDS.tsv",
            ["release", "jbs", "title", "component", "disposition", "reason", "upstream_commit"],
            [
                {
                    "release": "27",
                    "jbs": "JDK-9000001",
                    "title": "Tool packet",
                    "component": "core-svc/tools",
                    "disposition": "admitted",
                    "reason": "additive tool",
                    "upstream_commit": "a" * 40,
                },
                {
                    "release": "27",
                    "jbs": "JDK-9000002",
                    "title": "VM packet",
                    "component": "hotspot/runtime",
                    "disposition": "candidate-high-risk",
                    "reason": "runtime proof",
                    "upstream_commit": "",
                },
            ],
        )

        self.write_tsv(
            backports / "COMMUNITY_CAPABILITY_CANDIDATES.tsv",
            [
                "capability_id",
                "plane",
                "candidate",
                "evidence_type",
                "evidence_ref",
                "upstream_join_key",
                "packaging_candidate",
                "status",
                "selected_for_distribution",
                "next_proof",
            ],
            [
                {
                    "capability_id": "BPT-COMMUNITY",
                    "plane": "CORE_BACKPORT",
                    "candidate": "Community runtime idea",
                    "evidence_type": "FORK",
                    "evidence_ref": "F1",
                    "upstream_join_key": "UNKNOWN",
                    "packaging_candidate": "JDK_SOURCE_NATIVE",
                    "status": "PENDING_COMPATIBILITY_PROOF",
                    "selected_for_distribution": "false",
                    "next_proof": "pin donor and prove compatibility",
                }
            ],
        )

    def test_compiles_existing_authorities_into_deterministic_recipe_atoms(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.fixture(root)
            first = self.mod.compile_root(root)
            second = self.mod.compile_root(root)
            self.mod.validate(first)
            self.mod.validate(second)

            self.assertEqual(first, second)
            self.assertEqual(
                self.mod.canonical_root(first), self.mod.canonical_root(second)
            )

            j423 = [atom for atom in first if atom.work_id == "JEP-423"]
            self.assertTrue(j423)
            self.assertTrue(all(atom.state == "MATERIALIZED_RECIPE" for atom in j423))
            self.assertEqual(
                [
                    "INVENTORY",
                    "ATOMIZE",
                    "PATTERNIZE",
                    "DOCUMENT",
                    "RECIPE",
                    "COMPILE",
                    "TEST",
                    "RUNTIME_PARITY",
                    "FIXED_POINT",
                ],
                [atom.phase for atom in j423],
            )
            self.assertEqual("MULTI_MODULE", j423[0].scope)
            self.assertEqual("jep-423-region-pinning", j423[0].recipe_ref)

            j484 = [atom for atom in first if atom.work_id == "JEP-484"]
            self.assertTrue(all(atom.state == "AUTHOR_RECIPE" for atom in j484))
            self.assertEqual(
                [
                    "INVENTORY",
                    "ATOMIZE",
                    "PATTERNIZE",
                    "DOCUMENT",
                    "RECIPE",
                    "COMPILE",
                    "TEST",
                    "FIXED_POINT",
                ],
                [atom.phase for atom in j484],
            )
            self.assertNotIn("RUNTIME_PARITY", [atom.phase for atom in j484])
            self.assertIn(
                "canonical Synexia A3 FILE atomization",
                j484[1].reason,
            )
            self.assertIn(
                "pattern/IOP attribution",
                j484[2].reason,
            )

            rejected = [atom for atom in first if atom.work_id == "JEP-511"]
            self.assertEqual(1, len(rejected))
            self.assertEqual("REJECT", rejected[0].state)
            self.assertFalse(rejected[0].executable)

            superseded = [atom for atom in first if atom.work_id == "JEP-461"]
            self.assertEqual("SUPERSEDED", superseded[0].state)
            self.assertFalse(superseded[0].executable)

            tool = [atom for atom in first if atom.work_id == "JDK-9000001"]
            self.assertTrue(all(atom.state == "MATERIALIZED_RECIPE" for atom in tool))
            self.assertEqual("MODULE", tool[0].scope)

            vm = [atom for atom in first if atom.work_id == "JDK-9000002"]
            self.assertTrue(all(atom.state == "AUTHOR_RECIPE" for atom in vm))
            self.assertIn("RUNTIME_PARITY", [atom.phase for atom in vm])

            community = [atom for atom in first if atom.work_id == "BPT-COMMUNITY"]
            self.assertEqual(1, len(community))
            self.assertFalse(community[0].executable)
            self.assertEqual("COMMUNITY_REVIEW", community[0].proof_lane)

    def test_executable_work_items_emit_small_canonical_packet_tsvs(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            self.fixture(root)
            atoms = self.mod.compile_root(root)
            self.mod.validate(atoms)
            packet_dir = root / "target" / "packets"
            packets = self.mod.write_packets(atoms, packet_dir)
            index = root / "target" / "index.tsv"
            self.mod.write_packet_index(packets, index)

            packet_names = {path.name for _, _, path in packets}
            self.assertIn("jep-423.tsv", packet_names)
            self.assertIn("jep-484.tsv", packet_names)
            self.assertIn("jdk-9000001.tsv", packet_names)
            self.assertIn("jdk-9000002.tsv", packet_names)
            self.assertNotIn("jep-511.tsv", packet_names)
            self.assertNotIn("jep-461.tsv", packet_names)
            self.assertFalse(any(name.startswith("m3-bpt-community") for name in packet_names))

            j423 = (packet_dir / "jep-423.tsv").read_text(encoding="utf-8").splitlines()
            self.assertEqual(
                "packet_id\tatom_id\tscope\tscope_promotion_approved\twork_ref\tdepends_on",
                j423[0],
            )
            parsed = [line.split("\t") for line in j423[1:]]
            self.assertEqual(
                [
                    "inventory",
                    "atomize",
                    "patternize",
                    "document",
                    "recipe",
                    "compile",
                    "test",
                    "runtime-parity",
                    "fixed-point",
                ],
                [row[1] for row in parsed],
            )
            self.assertTrue(all(row[2] == "MULTI_MODULE" for row in parsed))
            self.assertTrue(all(row[3] == "true" for row in parsed))
            self.assertEqual("", parsed[0][5])
            self.assertEqual("inventory", parsed[1][5])
            self.assertEqual("atomize", parsed[2][5])
            self.assertEqual("patternize", parsed[3][5])
            self.assertEqual("document", parsed[4][5])
            self.assertEqual(
                "m3/backports/recipes/jep-423-region-pinning",
                parsed[4][4],
            )

            j484 = (packet_dir / "jep-484.tsv").read_text(encoding="utf-8").splitlines()
            recipe_row = [line.split("\t") for line in j484[1:] if "\trecipe\t" in line][0]
            self.assertEqual("AUTHOR_RECIPE:JEP-484", recipe_row[4])
            self.assertEqual("MODULE", recipe_row[2])
            self.assertEqual("true", recipe_row[3])

            index_text = index.read_text(encoding="utf-8")
            self.assertIn("JEP-423\tMATERIALIZED_RECIPE\tjep-423.tsv", index_text)
            self.assertIn("JEP-484\tAUTHOR_RECIPE\tjep-484.tsv", index_text)

    def test_current_repository_packets_are_recognized_without_duplicate_authoring(self) -> None:
        root = Path(__file__).resolve().parents[2]
        atoms = self.mod.compile_root(root)
        self.mod.validate(atoms)

        ordered_jep_work = []
        for atom in atoms:
            if atom.source_kind == "JEP" and atom.work_id not in ordered_jep_work:
                ordered_jep_work.append(atom.work_id)
        self.assertEqual(
            [
                "JEP-423",
                "JEP-458",
                "JEP-474",
                "JEP-484",
                "JEP-485",
                "JEP-493",
                "JEP-496",
                "JEP-497",
                "JEP-510",
                "JEP-523",
            ],
            ordered_jep_work[:10],
        )

        j423 = [atom for atom in atoms if atom.work_id == "JEP-423"]
        self.assertTrue(j423)
        self.assertTrue(
            all(atom.state.startswith("MATERIALIZED_RECIPE") for atom in j423)
        )

        j485 = [atom for atom in atoms if atom.work_id == "JEP-485"]
        self.assertTrue(j485)
        self.assertEqual("MATERIALIZED_RECIPE_SET", j485[0].state)
        self.assertIn("jep-485-gatherers", j485[0].recipe_ref)

        jdk8357439 = [atom for atom in atoms if atom.work_id == "JDK-8357439"]
        self.assertTrue(jdk8357439)
        self.assertTrue(
            all(atom.state.startswith("MATERIALIZED_RECIPE") for atom in jdk8357439)
        )

        rejected = [atom for atom in atoms if atom.work_id == "JEP-511"]
        self.assertEqual(1, len(rejected))
        self.assertEqual("REJECT", rejected[0].state)
        self.assertFalse(rejected[0].executable)

        payload = self.mod.summary(atoms)
        self.assertEqual("M3_BACKPORT_RECIPE_DAG_V1", payload["schema"])
        self.assertRegex(payload["root"], r"^[0-9a-f]{64}$")
        self.assertGreater(payload["work_items"], 80)
        self.assertGreater(payload["atoms"], payload["work_items"])


if __name__ == "__main__":
    unittest.main()
