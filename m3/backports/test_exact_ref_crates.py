#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0

from __future__ import annotations

import csv
import importlib.util
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


def load(name: str):
    path = Path(__file__).with_name(name)
    spec = importlib.util.spec_from_file_location("m3_" + name.replace(".", "_"), path)
    if spec is None or spec.loader is None:
        raise RuntimeError(path)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class ExactRefCrateTest(unittest.TestCase):
    def setUp(self) -> None:
        self.delta = load("file_delta_inventory.py")
        self.crates = load("generate_recipe_crates.py")

    @staticmethod
    def git(repo: Path, *args: str) -> str:
        return subprocess.check_output(
            ("git", "-C", str(repo), *args), text=True
        ).strip()

    def test_exact_refs_are_compared_and_recorded_without_ga_aliasing(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            repo = root / "repo"
            out = root / "out"
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            self.git(repo, "config", "user.email", "fixture@example.invalid")
            self.git(repo, "config", "user.name", "Fixture")

            path = "src/hotspot/share/runtime/monitor.cpp"
            target = repo / path
            target.parent.mkdir(parents=True)
            target.write_text("int monitor() { return 21; }\n", encoding="utf-8")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "base")
            baseline = self.git(repo, "rev-parse", "HEAD")

            target.write_text("int monitor() { return 491; }\n", encoding="utf-8")
            self.git(repo, "add", ".")
            self.git(repo, "commit", "-q", "-m", "j491")
            donor = self.git(repo, "rev-parse", "HEAD")

            rows = self.delta.compare_refs(repo, 24, baseline, donor)
            changed = [row for row in rows if row.status != "SAME"]
            self.assertEqual(1, len(changed))
            self.assertEqual(baseline, changed[0].baseline_ref)
            self.assertEqual(donor, changed[0].donor_ref)

            candidates, exclusions = self.crates.candidates(
                repo,
                24,
                {path},
                False,
                include_native=True,
                baseline_ref=baseline,
                donor_ref=donor,
            )
            self.assertEqual([], exclusions)
            self.assertEqual([path], [candidate.path for candidate in candidates])

            self.crates.materialize(
                out,
                24,
                candidates,
                exclusions,
                crate_size=1,
                baseline_ref=baseline,
                donor_ref=donor,
            )
            with (out / "CRATES.tsv").open(encoding="utf-8", newline="") as handle:
                records = list(csv.DictReader(handle, delimiter="\t"))
            self.assertEqual(1, len(records))
            self.assertEqual(baseline, records[0]["baseline_ref"])
            self.assertEqual(donor, records[0]["donor_ref"])

    def test_exact_ref_arguments_fail_closed_when_only_one_is_supplied(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp)
            subprocess.run(("git", "init", "-q", str(repo)), check=True)
            with self.assertRaises(ValueError):
                self.crates.candidates(
                    repo,
                    24,
                    {"src/A.java"},
                    False,
                    baseline_ref="base",
                )


if __name__ == "__main__":
    unittest.main()
