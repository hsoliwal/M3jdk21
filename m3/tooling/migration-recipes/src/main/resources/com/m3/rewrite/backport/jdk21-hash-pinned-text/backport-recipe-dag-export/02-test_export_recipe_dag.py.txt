# SPDX-License-Identifier: GPL-2.0-only WITH Classpath-exception-2.0
import csv
import io
from pathlib import Path
import tempfile
import unittest

import export_recipe_dag as dag


def tsv(header, rows):
    stream = io.StringIO()
    writer = csv.writer(stream, delimiter="\t", lineterminator="\n")
    writer.writerow(header)
    writer.writerows(rows)
    return stream.getvalue()


class ExportRecipeDagTest(unittest.TestCase):
    def test_candidate_queue_builds_seven_node_chain_without_authority(self):
        passes = [
            dag.Pass(0, "inventory", False, "inventory"),
            dag.Pass(1, "compatibility-classify", False, "classify"),
            dag.Pass(2, "dependency-closure", False, "deps"),
            dag.Pass(3, "materialize", False, "materialize"),
            dag.Pass(4, "apply", True, "apply"),
            dag.Pass(5, "verify", False, "verify"),
            dag.Pass(6, "promote", False, "promote"),
        ]
        queue = [{
            "source_type": "JBS",
            "release": "27",
            "identity": "JDK-8364182",
            "title": "Security properties",
            "domain": "hotspot/svc",
            "disposition": "candidate-adapted",
            "current_pass": "5",
            "action": "VERIFY_HASH_PINNED_RECIPE",
            "required_scope": "LIBRARY_API",
            "risk": "ADMITTED_UNVERIFIED",
            "dependency_or_commit": "",
            "reason": "exact recipe exists",
        }]
        recipes = [
            dag.RecipeBinding(
                "m3-jdk8364182-security-properties",
                "com.m3.rewrite.backport.M3Jdk8364182BackportRecipe",
                "LIBRARY_API",
                "CANDIDATE_UNVERIFIED",
                "PENDING_CI",
            )
        ]

        rows = dag.export_rows(queue, passes, recipes)

        self.assertEqual(7, len(rows))
        self.assertEqual(list(map(str, range(7))), [row["pass_ordinal"] for row in rows])
        self.assertEqual("", rows[0]["dependency_node_ids"])
        for index in range(1, 7):
            self.assertEqual(rows[index - 1]["node_id"], rows[index]["dependency_node_ids"])

        apply = rows[4]
        self.assertEqual("true", apply["declared_source_changing"])
        self.assertEqual("false", apply["manifest_mutation_authority"])
        self.assertEqual("false", apply["promotion_authority"])
        self.assertEqual("RECIPE_DECLARED", apply["binding_status"])
        self.assertEqual("PENDING_CI", apply["proof_status"])
        self.assertEqual("false", apply["execution_ready"])
        self.assertEqual("CURRENT", rows[5]["state"])
        self.assertTrue(all(len(row["item_root"]) == 64 for row in rows))
        self.assertTrue(all(len(row["node_root"]) == 64 for row in rows))

    def test_verified_recipe_is_the_only_way_apply_can_be_execution_ready(self):
        queue = [{
            "source_type": "JBS",
            "release": "27",
            "identity": "JDK-9999999",
            "title": "Fixture",
            "domain": "tools",
            "disposition": "candidate",
            "current_pass": "4",
            "action": "APPLY_VERIFIED_RECIPE",
            "required_scope": "MODULE",
            "risk": "NORMAL",
            "dependency_or_commit": "deadbeef",
            "reason": "fixture",
        }]
        passes = [
            dag.Pass(0, "inventory", False, "x"),
            dag.Pass(1, "compatibility-classify", False, "x"),
            dag.Pass(2, "dependency-closure", False, "x"),
            dag.Pass(3, "materialize", False, "x"),
            dag.Pass(4, "apply", True, "x"),
            dag.Pass(5, "verify", False, "x"),
            dag.Pass(6, "promote", False, "x"),
        ]
        verified = [
            dag.RecipeBinding(
                "m3-jdk9999999-fixture",
                "com.m3.Fixture",
                "MODULE",
                "VERIFIED",
                "VERIFIED",
            )
        ]
        unverified = [
            dag.RecipeBinding(
                "m3-jdk9999999-fixture",
                "com.m3.Fixture",
                "MODULE",
                "CANDIDATE_UNVERIFIED",
                "PENDING_CI",
            )
        ]

        self.assertEqual(
            "true", dag.export_rows(queue, passes, verified)[4]["execution_ready"]
        )
        self.assertEqual(
            "false", dag.export_rows(queue, passes, unverified)[4]["execution_ready"]
        )
        self.assertEqual(
            "false", dag.export_rows(queue, passes, [])[4]["execution_ready"]
        )

    def test_rejected_superseded_and_hold_future_states_are_explicit(self):
        base = {
            "source_type": "JEP",
            "release": "27",
            "title": "Fixture",
            "domain": "language",
            "current_pass": "1",
            "action": "HOLD",
            "required_scope": "LIBRARY_API",
            "risk": "HOLD",
            "dependency_or_commit": "",
            "reason": "fixture",
        }
        passes = [
            dag.Pass(0, "inventory", False, "x"),
            dag.Pass(1, "compatibility-classify", False, "x"),
            dag.Pass(2, "dependency-closure", False, "x"),
            dag.Pass(3, "materialize", False, "x"),
            dag.Pass(4, "apply", True, "x"),
            dag.Pass(5, "verify", False, "x"),
            dag.Pass(6, "promote", False, "x"),
        ]
        cases = {
            "reject-language": "BLOCKED",
            "superseded": "REDIRECTED",
            "hold-preview": "HELD",
            "candidate": "FUTURE",
        }
        for index, (disposition, expected) in enumerate(cases.items()):
            queue = [dict(base, identity=f"JEP-{900 + index}", disposition=disposition)]
            rows = dag.export_rows(queue, passes, [])
            self.assertEqual(expected, rows[2]["state"], disposition)
            self.assertTrue(all(row["promotion_authority"] == "false" for row in rows))

    def test_invalid_scope_and_noncanonical_mutation_pass_fail_closed(self):
        passes = [
            dag.Pass(0, "inventory", False, "x"),
            dag.Pass(1, "compatibility-classify", False, "x"),
            dag.Pass(2, "dependency-closure", False, "x"),
            dag.Pass(3, "materialize", False, "x"),
            dag.Pass(4, "apply", True, "x"),
            dag.Pass(5, "verify", False, "x"),
            dag.Pass(6, "promote", False, "x"),
        ]
        queue = [{
            "source_type": "JEP",
            "release": "27",
            "identity": "JEP-999",
            "title": "Fixture",
            "domain": "library",
            "disposition": "candidate",
            "current_pass": "2",
            "action": "INVESTIGATE",
            "required_scope": "UNIVERSE",
            "risk": "NORMAL",
            "dependency_or_commit": "",
            "reason": "fixture",
        }]
        with self.assertRaises(ValueError):
            dag.export_rows(queue, passes, [])

        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp, "passes.tsv")
            path.write_text(
                tsv(
                    dag.PASS_FIELDS,
                    [
                        (0, "inventory", "false", "x"),
                        (1, "compatibility-classify", "false", "x"),
                        (2, "dependency-closure", "false", "x"),
                        (3, "materialize", "true", "x"),
                        (4, "apply", "true", "x"),
                        (5, "verify", "false", "x"),
                        (6, "promote", "false", "x"),
                    ],
                ),
                encoding="utf-8",
            )
            with self.assertRaises(ValueError):
                dag.read_passes(path)

    def test_render_and_root_are_deterministic_under_queue_reordering(self):
        passes = [
            dag.Pass(0, "inventory", False, "x"),
            dag.Pass(1, "compatibility-classify", False, "x"),
            dag.Pass(2, "dependency-closure", False, "x"),
            dag.Pass(3, "materialize", False, "x"),
            dag.Pass(4, "apply", True, "x"),
            dag.Pass(5, "verify", False, "x"),
            dag.Pass(6, "promote", False, "x"),
        ]
        def row(identity, release):
            return {
                "source_type": "JEP",
                "release": str(release),
                "identity": identity,
                "title": identity,
                "domain": "library",
                "disposition": "candidate",
                "current_pass": "2",
                "action": "INVESTIGATE",
                "required_scope": "MODULE",
                "risk": "NORMAL",
                "dependency_or_commit": "",
                "reason": "fixture",
            }

        first = dag.export_rows([row("JEP-2", 23), row("JEP-1", 22)], passes, [])
        second = dag.export_rows([row("JEP-1", 22), row("JEP-2", 23)], passes, [])

        self.assertEqual(dag.render(first), dag.render(second))
        self.assertEqual(dag.dag_root(first), dag.dag_root(second))

    def test_repository_queue_exports_every_item_as_one_seven_pass_chain(self):
        backports = Path(__file__).resolve().parent
        m3 = backports.parent
        queue = dag.read_tsv(backports / "BACKPORT_WORK_QUEUE.tsv", dag.QUEUE_FIELDS)
        passes = dag.read_passes(backports / "BACKPORT_PASSES.tsv")
        recipes = dag.recipe_bindings(m3 / "tooling/recipe-catalogue.tsv")
        rows = dag.export_rows(queue, passes, recipes)

        self.assertEqual(len(queue) * 7, len(rows))
        by_item = {}
        for row in rows:
            by_item.setdefault(row["item_key"], []).append(row)
            self.assertEqual("false", row["manifest_mutation_authority"])
            self.assertEqual("false", row["promotion_authority"])
        self.assertEqual(len(queue), len(by_item))

        for key, item_rows in by_item.items():
            self.assertEqual(7, len(item_rows), key)
            self.assertEqual(1, sum(row["state"] == "CURRENT" for row in item_rows), key)
            self.assertEqual(
                1,
                sum(row["declared_source_changing"] == "true" for row in item_rows),
                key,
            )
            apply = next(row for row in item_rows if row["pass_id"] == "apply")
            self.assertEqual("false", apply["execution_ready"], key)

        self.assertTrue(
            any(row["binding_status"] == "RECIPE_DECLARED" for row in rows),
            "expected current recipe catalogue to bind at least one queue item",
        )


if __name__ == "__main__":
    unittest.main()
