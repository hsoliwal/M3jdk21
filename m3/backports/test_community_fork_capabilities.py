#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

import csv
import importlib.util
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

MODULE = Path(__file__).with_name("community_fork_capabilities.py")
SPEC = importlib.util.spec_from_file_location("community_fork_capabilities", MODULE)
assert SPEC is not None and SPEC.loader is not None
mod = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = mod
SPEC.loader.exec_module(mod)


def git(repo: Path, *args: str) -> str:
    env = os.environ.copy()
    env.update({
        "GIT_AUTHOR_NAME": "M3 Test",
        "GIT_AUTHOR_EMAIL": "m3@example.invalid",
        "GIT_COMMITTER_NAME": "M3 Test",
        "GIT_COMMITTER_EMAIL": "m3@example.invalid",
    })
    return subprocess.run(
        ("git", "-C", str(repo), *args),
        check=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE,
        text=True, encoding="utf-8", env=env,
    ).stdout.strip()


class ForkCapabilityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.repo = self.root / "repo"
        self.repo.mkdir()
        git(self.repo, "init", "-q")
        git(self.repo, "checkout", "-q", "-b", "base")
        (self.repo / "x").write_text("base\n")
        git(self.repo, "add", "x")
        git(self.repo, "commit", "-qm", "base")
        self.base = git(self.repo, "rev-parse", "HEAD")
        git(self.repo, "checkout", "-q", "-b", "fork")
        (self.repo / "x").write_text("base\nfork\n")
        git(self.repo, "commit", "-qam", "fork feature")
        self.feature = git(self.repo, "rev-parse", "HEAD")
        git(self.repo, "update-ref", "refs/remotes/m3/fork-a", self.feature)

    def tearDown(self):
        self.temp.cleanup()

    def capability(self, **overrides):
        values = {
            "capability_id": "CAP-X",
            "fork_id": "fork-a",
            "evidence_commit": self.feature,
            "capability": "feature",
            "plane": "CORE_BACKPORT",
            "priority": "HIGH",
            "status": "PENDING_COMPATIBILITY_PROOF",
            "source_copy_authority": "false",
            "selected_for_distribution": "false",
            "join_keys": "JDK-8000000",
            "next_proof": "prove",
        }
        values.update(overrides)
        return values

    def write_caps(self, rows):
        path = self.root / "caps.tsv"
        with path.open("w", encoding="utf-8", newline="") as handle:
            writer = csv.DictWriter(handle, fieldnames=list(rows[0]), delimiter="\t")
            writer.writeheader()
            writer.writerows(rows)
        return path

    def test_product_candidate_requires_reachable_unique_commit(self):
        rows = mod.read_capabilities(self.write_caps([self.capability()]), {"fork-a"})
        verified = mod.verify(self.repo, rows, {("fork-a", self.feature)})
        self.assertEqual(1, len(verified))
        self.assertTrue(verified[0][1])

    def test_product_candidate_without_unique_evidence_refuses(self):
        rows = mod.read_capabilities(self.write_caps([self.capability()]), {"fork-a"})
        with self.assertRaisesRegex(ValueError, "fork-unique"):
            mod.verify(self.repo, rows, set())

    def test_branch_proof_can_reference_reachable_non_unique_head(self):
        rows = mod.read_capabilities(
            self.write_caps([self.capability(status="PENDING_BRANCH_PROOF")]),
            {"fork-a"},
        )
        verified = mod.verify(self.repo, rows, set())
        self.assertFalse(verified[0][1])

    def test_unknown_fork_and_authority_refuse(self):
        with self.assertRaisesRegex(ValueError, "unknown fork"):
            mod.read_capabilities(
                self.write_caps([self.capability(fork_id="missing")]),
                {"fork-a"},
            )
        with self.assertRaisesRegex(ValueError, "authority"):
            mod.read_capabilities(
                self.write_caps([self.capability(source_copy_authority="true")]),
                {"fork-a"},
            )


if __name__ == "__main__":
    unittest.main()
