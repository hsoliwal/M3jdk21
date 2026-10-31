#!/usr/bin/env python3
# Copyright (c) 2026 Hitesh Soliwal.
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import importlib.util
import io
import sys
import tempfile
import unittest
from pathlib import Path


def load_module():
    path = Path(__file__).with_name("compatibility_queue.py")
    spec = importlib.util.spec_from_file_location("m3_compatibility_queue", path)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load {path}")
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class CompatibilityQueueTest(unittest.TestCase):
    def setUp(self) -> None:
        self.q = load_module()

    def row(
        self,
        *,
        release: int,
        commit: str,
        subject: str,
        domain: str,
        disposition: str = "review",
        paths: str = "",
        javac: bool = False,
        grammar: bool = False,
        hotspot_compiler: bool = False,
        compat: bool = False,
        jbs: str = "",
    ) -> dict[str, str]:
        return {
            "release": str(release),
            "commit": commit,
            "jbs_ids": jbs,
            "subject": subject,
            "domain": domain,
            "javac_touch": str(javac).lower(),
            "grammar_touch": str(grammar).lower(),
            "hotspot_compiler_touch": str(hotspot_compiler).lower(),
            "compatibility_signal": str(compat).lower(),
            "disposition": disposition,
            "paths": paths,
        }

    def test_every_row_remains_pending_compatibility_proof(self) -> None:
        rows = [
            self.row(
                release=22,
                commit="a" * 40,
                subject="tool",
                domain="tools",
                paths="src/jdk.jcmd/share/classes/a/A.java",
            ),
            self.row(
                release=23,
                commit="b" * 40,
                subject="parser",
                domain="other-runtime-or-library",
                disposition="hold-language",
                paths="src/jdk.compiler/share/classes/com/sun/tools/javac/parser/JavacParser.java",
                javac=True,
                grammar=True,
            ),
            self.row(
                release=24,
                commit="c" * 40,
                subject="remove compatibility",
                domain="core-libs",
                disposition="hold-compat",
                paths="src/java.base/share/classes/java/lang/Foo.java",
                compat=True,
            ),
        ]
        items = self.q.queue(rows)
        self.assertEqual(3, len(items))
        self.assertTrue(
            all(item.compatibility_state == self.q.PENDING for item in items)
        )
        by_commit = {item.commit: item for item in items}
        self.assertEqual("TOOLING", by_commit["a" * 40].proof_lane)
        self.assertEqual("LANGUAGE_SPLIT_REVIEW", by_commit["b" * 40].proof_lane)
        self.assertEqual(
            "JAVA21_COMPATIBILITY_REVIEW", by_commit["c" * 40].proof_lane
        )

    def test_priority_is_deterministic_and_low_risk_build_tooling_runs_first(self) -> None:
        rows = [
            self.row(
                release=24,
                commit="d" * 40,
                subject="hotspot",
                domain="hotspot",
                paths="src/hotspot/share/runtime/foo.cpp",
            ),
            self.row(
                release=22,
                commit="b" * 40,
                subject="tool",
                domain="tools",
                paths="src/jdk.jlink/share/classes/jdk/tools/jlink/Foo.java",
            ),
            self.row(
                release=22,
                commit="a" * 40,
                subject="build",
                domain="build",
                paths="make/modules/java.base/Java.gmk",
            ),
            self.row(
                release=23,
                commit="c" * 40,
                subject="library",
                domain="core-libs",
                paths="src/java.base/share/classes/java/util/Foo.java",
            ),
        ]
        forward = self.q.queue(rows)
        reverse = self.q.queue(reversed(rows))

        self.assertEqual(
            [(item.priority, item.release, item.commit) for item in forward],
            [(item.priority, item.release, item.commit) for item in reverse],
        )
        self.assertEqual([10, 20, 35, 50], [item.priority for item in forward])
        self.assertEqual(list(range(4)), [item.order for item in forward])

    def test_scope_floor_and_recipe_strategy_are_physical_only(self) -> None:
        rows = [
            self.row(
                release=22,
                commit="a" * 40,
                subject="one java file",
                domain="tools",
                paths="src/jdk.jcmd/share/classes/jdk/internal/foo/A.java",
            ),
            self.row(
                release=22,
                commit="b" * 40,
                subject="same module multiple files",
                domain="core-libs",
                paths=(
                    "src/java.base/share/classes/java/util/A.java,"
                    "src/java.base/share/classes/java/util/B.java"
                ),
            ),
            self.row(
                release=22,
                commit="c" * 40,
                subject="multiple modules",
                domain="other-runtime-or-library",
                paths=(
                    "src/java.base/share/classes/java/util/A.java,"
                    "src/java.logging/share/classes/java/util/logging/B.java"
                ),
            ),
            self.row(
                release=22,
                commit="d" * 40,
                subject="mixed packet",
                domain="tools",
                paths=(
                    "src/jdk.jcmd/share/classes/jdk/internal/A.java,"
                    "src/jdk.jcmd/share/conf/jcmd"
                ),
            ),
        ]
        by_commit = {item.commit: item for item in self.q.queue(rows)}
        self.assertEqual("FILE", by_commit["a" * 40].scope_floor)
        self.assertEqual("MODULE", by_commit["b" * 40].scope_floor)
        self.assertEqual("MULTI_MODULE", by_commit["c" * 40].scope_floor)
        self.assertEqual(
            "OPENREWRITE_OR_HASH_PINNED_JAVA",
            by_commit["a" * 40].recipe_strategy,
        )
        self.assertEqual(
            "MIXED_PACKET_OPENREWRITE_PLUS_VERBATIM",
            by_commit["d" * 40].recipe_strategy,
        )

    def test_boolean_and_inventory_schema_fail_closed(self) -> None:
        bad = self.row(
            release=22,
            commit="a" * 40,
            subject="bad",
            domain="tools",
            paths="src/jdk.jcmd/share/classes/A.java",
        )
        bad["javac_touch"] = "maybe"
        with self.assertRaises(ValueError):
            self.q.queue([bad])

        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "inventory.tsv"
            path.write_text("release\tcommit\n22\t" + "a" * 40 + "\n", encoding="utf-8")
            with self.assertRaises(ValueError):
                self.q.read_inventory(path)

    def test_tsv_output_contains_all_mechanical_decision_fields(self) -> None:
        item = self.q.queue(
            [
                self.row(
                    release=22,
                    commit="a" * 40,
                    subject="JDK-8000000 tool",
                    domain="tools",
                    paths="src/jdk.jcmd/share/conf/bash-completion/jcmd",
                    jbs="JDK-8000000",
                )
            ]
        )[0]
        output = io.StringIO()
        self.q.write_tsv([item], output)
        text = output.getvalue()
        self.assertIn("proof_lane", text)
        self.assertIn("recipe_strategy", text)
        self.assertIn("compatibility_state", text)
        self.assertIn("PENDING_COMPATIBILITY_PROOF", text)
        self.assertIn("PROVE_TOOLING_BACKPORT", text)


if __name__ == "__main__":
    unittest.main()
