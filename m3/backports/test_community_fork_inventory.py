#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import os
import subprocess
import tempfile
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("community_fork_inventory.py")
SPEC = importlib.util.spec_from_file_location("community_fork_inventory", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(mod)


def git(repo: Path, *args: str, env: dict[str, str] | None = None) -> str:
    merged = os.environ.copy()
    merged.update(
        {
            "GIT_AUTHOR_NAME": "M3 Test",
            "GIT_AUTHOR_EMAIL": "m3@example.invalid",
            "GIT_COMMITTER_NAME": "M3 Test",
            "GIT_COMMITTER_EMAIL": "m3@example.invalid",
        }
    )
    if env:
        merged.update(env)
    proc = subprocess.run(
        ("git", "-C", str(repo), *args),
        check=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        env=merged,
    )
    return proc.stdout.strip()


class CommunityForkInventoryTest(unittest.TestCase):
    def setUp(self) -> None:
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        git(self.repo, "init", "-q")
        git(self.repo, "checkout", "-q", "-b", "base")
        (self.repo / "common.txt").write_text("base\n", encoding="utf-8")
        git(self.repo, "add", "common.txt")
        git(self.repo, "commit", "-q", "-m", "base")
        self.base = git(self.repo, "rev-parse", "HEAD")

    def tearDown(self) -> None:
        self.temp.cleanup()

    def _commit(self, branch: str, path: str, text: str, message: str, timestamp: str) -> str:
        git(self.repo, "checkout", "-q", branch)
        target = self.repo / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text, encoding="utf-8")
        git(self.repo, "add", path)
        git(
            self.repo,
            "commit",
            "-q",
            "-m",
            message,
            env={
                "GIT_AUTHOR_DATE": timestamp,
                "GIT_COMMITTER_DATE": timestamp,
            },
        )
        return git(self.repo, "rev-parse", "HEAD")

    def _catalog(self, upstream: str, fork: str) -> Path:
        path = self.root / "catalog.tsv"
        path.write_text(
            "fork_id\trepository\tref\tpinned_head\tplane\tpriority\tlicense_policy\t"
            "source_copy_authority\tselected_for_distribution\tnotes\n"
            f"upstream21u\topenjdk/jdk21u\tmaster\t{upstream}\tBASELINE\tBASELINE\tREVIEW\tfalse\tfalse\tbase\n"
            f"fork-a\texample/fork\tproduct\t{fork}\tPRODUCT\tHIGH\tREVIEW\tfalse\tfalse\tfork\n",
            encoding="utf-8",
        )
        return path

    def _remote_ref(self, name: str, commit: str) -> None:
        git(self.repo, "update-ref", f"refs/remotes/m3/{name}", commit)

    def test_cherry_equivalent_patch_is_filtered_but_unique_change_remains(self) -> None:
        git(self.repo, "checkout", "-q", "-b", "upstream", self.base)
        upstream = self._commit(
            "upstream",
            "common.txt",
            "base\nshared\n",
            "JDK-8000001 shared patch",
            "2026-01-01T00:00:00+00:00",
        )

        git(self.repo, "checkout", "-q", "-b", "fork", self.base)
        self._commit(
            "fork",
            "common.txt",
            "base\nshared\n",
            "vendor spelling for shared patch",
            "2026-01-02T00:00:00+00:00",
        )
        fork = self._commit(
            "fork",
            "src/java.base/share/classes/example/ForkFeature.java",
            "package example; final class ForkFeature {}\n",
            "JDK-8999999 fork-only feature",
            "2026-01-03T00:00:00+00:00",
        )

        self._remote_ref("upstream21u", upstream)
        self._remote_ref("fork-a", fork)
        specs = mod.read_catalog(self._catalog(upstream, fork))
        relationships, changes = mod.inventory(self.repo, specs)

        self.assertEqual(1, len(relationships))
        self.assertTrue(relationships[0].common_ancestry)
        self.assertEqual(1, relationships[0].fork_unique_commits)
        self.assertEqual(0, relationships[0].upstream_unique_commits)

        self.assertEqual(1, len(changes))
        change = changes[0]
        self.assertEqual(("JDK-8999999",), change.jbs_ids)
        self.assertEqual("OPENREWRITE_JAVA_REVIEW", change.recipe_strategy)
        self.assertEqual("FILE", change.scope_floor)
        self.assertEqual("core-libs", change.domain)
        self.assertEqual("PENDING_COMPATIBILITY_PROOF", change.compatibility_state)

    def test_catalogue_refuses_pre_authorized_source_copy_or_distribution(self) -> None:
        path = self.root / "bad.tsv"
        path.write_text(
            "fork_id\trepository\tref\tpinned_head\tplane\tpriority\tlicense_policy\t"
            "source_copy_authority\tselected_for_distribution\tnotes\n"
            + "upstream21u\topenjdk/jdk21u\tmaster\t"
            + "0" * 40
            + "\tBASELINE\tBASELINE\tREVIEW\tfalse\tfalse\tbase\n"
            + "bad\texample/bad\tmain\t"
            + "1" * 40
            + "\tPRODUCT\tHIGH\tREVIEW\ttrue\tfalse\tbad\n",
            encoding="utf-8",
        )
        with self.assertRaises(ValueError):
            mod.read_catalog(path)

    def test_scope_recipe_risk_and_domain_are_evidence_only(self) -> None:
        java = ("src/java.base/share/classes/java/lang/Foo.java",)
        self.assertEqual("FILE", mod._scope_floor(java))
        self.assertEqual("OPENREWRITE_JAVA_REVIEW", mod._recipe_strategy(java))
        self.assertEqual("core-libs", mod._domain(java))

        package = (
            "src/java.base/share/classes/java/lang/Foo.java",
            "src/java.base/share/classes/java/lang/Bar.java",
        )
        self.assertEqual("PACKAGE", mod._scope_floor(package))

        module = (
            "src/java.base/share/classes/java/lang/Foo.java",
            "src/java.base/share/classes/java/util/Bar.java",
        )
        self.assertEqual("MODULE", mod._scope_floor(module))

        multi = (
            "src/java.base/share/classes/java/lang/Foo.java",
            "src/java.logging/share/classes/java/util/logging/Logger.java",
        )
        self.assertEqual("MULTI_MODULE", mod._scope_floor(multi))

        native = ("src/hotspot/share/runtime/vendor_feature.cpp",)
        self.assertEqual("SOURCE_SEALED_NATIVE_REVIEW", mod._recipe_strategy(native))
        self.assertEqual("hotspot", mod._domain(native))
        self.assertEqual("HIGH", mod._risk("vendor optimization", native))

        grammar = (
            "src/jdk.compiler/share/classes/com/sun/tools/javac/parser/JavacParser.java",
        )
        self.assertEqual("javac-language-sensitive", mod._domain(grammar))
        self.assertEqual("CRITICAL", mod._risk("parser feature", grammar))

    def test_output_writers_keep_all_authority_false(self) -> None:
        relationship = mod.Relationship(
            "fork-a",
            "example/fork",
            "main",
            "1" * 40,
            "1" * 40,
            "0" * 40,
            True,
            2,
            3,
            False,
            False,
        )
        change = mod.ForkChange(
            "fork-a",
            0,
            "2" * 40,
            (),
            "tooling improvement",
            "tools",
            "MEDIUM",
            "FILE",
            "OPENREWRITE_JAVA_REVIEW",
            "PENDING_COMPATIBILITY_PROOF",
            ("src/jdk.jcmd/share/classes/example/Tool.java",),
        )

        relationships = self.root / "relationships.tsv"
        changes = self.root / "changes.tsv"
        paths = self.root / "paths.tsv"
        with relationships.open("w", encoding="utf-8", newline="") as handle:
            mod.write_relationships([relationship], handle)
        with changes.open("w", encoding="utf-8", newline="") as handle:
            mod.write_changes([change], handle)
        with paths.open("w", encoding="utf-8", newline="") as handle:
            mod.write_path_candidates([change], handle)

        with relationships.open("r", encoding="utf-8", newline="") as handle:
            row = next(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual("false", row["source_copy_authority"])
        self.assertEqual("false", row["selected_for_distribution"])

        with paths.open("r", encoding="utf-8", newline="") as handle:
            row = next(csv.DictReader(handle, delimiter="\t"))
        self.assertEqual("PENDING_COMPATIBILITY_PROOF", row["compatibility_state"])
        self.assertEqual("false", row["source_copy_authority"])
        self.assertEqual("false", row["selected_for_distribution"])


if __name__ == "__main__":
    unittest.main()
