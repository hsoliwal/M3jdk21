# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Defensive tests for the canonical-mapping-only naming census."""
from __future__ import annotations

from hashlib import sha1
import importlib.util
import json
from pathlib import Path
<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
=======
import shutil
>>>>>>> f0d77f40b3f2fa78820aea517072e6d72f4d6ee8
import subprocess
import sys
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("name_census", HERE / "audit.py")
AUDIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(AUDIT)


def write(root: Path, path: str, value: str) -> None:
    dst = root / path
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(value, encoding="utf-8")


class TargetCensusTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.candidates = [
            dict(path="src/java.base/share/classes/jdk/internal/mindex/M3CI.java",
                 module="java.base", symbol="jdk.internal.mindex.M3CI"),
            dict(path="src/java.base/share/native/libjava/M3Index.c",
                 module="java.base", symbol="Java_jdk_internal_mindex_M3Index_copy0"),
        ]
        self.records = [dict(id="cap.one", status="implemented-unverified", targets=list(self.candidates)),
                        dict(id="cap.two", status="pending", targets=[self.candidates[0]])]
        write(self.root, self.candidates[0]["path"],
              "package jdk.internal.mindex; public final class M3CI {}\n")
        write(self.root, self.candidates[1]["path"], "// native symbol\n")
        self.save()

    def tearDown(self):
        self.tmp.cleanup()

    def save(self):
        text = json.dumps({"migration": {"records": self.records}})
        write(self.root, AUDIT.MAP, text)
        raw = (self.root / AUDIT.MAP).read_bytes()
        digest = sha1(b"blob " + str(len(raw)).encode("ascii") + b"\0" + raw).hexdigest()
        write(self.root, AUDIT.PLAN, json.dumps({"authority": AUDIT.MAP, "authority_blob": digest}))

    def test_present_and_shared_paths_are_explicit(self):
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(len(rows), 3)
        self.assertEqual([r["result"] for r in rows], ["PRESENT_NAME_MATCH", "NATIVE_PRESENT_UNVERIFIED", "PRESENT_NAME_MATCH"])
        self.assertEqual(rows[0]["path_refs"], "2")
        self.assertEqual(rows[2]["path_refs"], "2")
        self.assertEqual(AUDIT.as_tsv(rows), AUDIT.as_tsv(AUDIT.snapshot(self.root)))

    def test_pending_java_missing_is_not_a_pass(self):
        (self.root / self.candidates[0]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[0]["result"], "REVIEW_MISSING")
        self.assertEqual(rows[2]["result"], "PENDING_MISSING")

    def test_mismatched_package_is_flagged(self):
        write(self.root, self.candidates[0]["path"], "package wrong; class M3CI {}")
        self.assertEqual(AUDIT.snapshot(self.root)[0]["result"], "REVIEW_DECLARATION")

    def test_bad_java_path_is_flagged(self):
        self.records[0]["targets"][0] = dict(self.candidates[0], symbol="jdk.internal.mindex.M3Other")
        self.save()
        self.assertEqual(AUDIT.snapshot(self.root)[0]["result"], "REVIEW_PATH_SYMBOL")

    def test_absent_implemented_native_target_needs_review(self):
        (self.root / self.candidates[1]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[1]["result"], "REVIEW_NATIVE_MISSING")

    def test_absent_pending_native_target_is_not_accepted(self):
        self.records[0]["status"] = "pending"
        self.save()
        (self.root / self.candidates[1]["path"]).unlink()
        rows = AUDIT.snapshot(self.root)
        self.assertEqual(rows[1]["result"], "PENDING_NATIVE_MISSING")
        self.assertIn("target_path", AUDIT.as_tsv(rows).splitlines()[0])

    def test_map_drift_refused(self):
        write(self.root, AUDIT.MAP, "{}")
        with self.assertRaisesRegex(ValueError, "drift"):
            AUDIT.snapshot(self.root)

    def test_duplicate_mapping_id_refused(self):
        self.records.append(dict(self.records[0]))
        self.save()
        with self.assertRaisesRegex(ValueError, "duplicate"):
            AUDIT.snapshot(self.root)

    def test_escape_path_refused(self):
        self.records[0]["targets"][0]["path"] = "../../secret.java"
        self.save()
        with self.assertRaisesRegex(ValueError, "unsafe"):
            AUDIT.snapshot(self.root)

    def run_cli(self, script, *arguments):
        relative = Path("m3/tooling/name-contract/audit.py")
        destination = self.root / relative
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(HERE / "audit.py", destination)
        return subprocess.run([sys.executable, str(script), *arguments], cwd=self.root,
                              text=True, capture_output=True, check=False)

    def test_real_cli_accepts_relative_and_absolute_script_paths(self):
        relative = Path("m3/tooling/name-contract/audit.py")
        for script in (relative, self.root / relative):
            for arguments in ((), ("--strict",)):
                with self.subTest(script=script, arguments=arguments):
                    result = self.run_cli(script, *arguments)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertEqual(result.stdout, AUDIT.as_tsv(AUDIT.snapshot(self.root)))
                    self.assertIn("rows=3 distinct_paths=2 attention=0", result.stderr)

    def test_real_cli_preserves_strict_review_and_argument_refusal(self):
        relative = Path("m3/tooling/name-contract/audit.py")
        (self.root / self.candidates[1]["path"]).unlink()
        strict = self.run_cli(relative, "--strict")
        self.assertEqual(strict.returncode, 1, strict.stderr)
        self.assertIn("REVIEW_NATIVE_MISSING", strict.stdout)
        ordinary = self.run_cli(relative)
        self.assertEqual(ordinary.returncode, 0, ordinary.stderr)
        self.assertEqual(ordinary.stdout, strict.stdout)
        for arguments in (("--unknown",), ("--strict", "--strict")):
            invalid = self.run_cli(relative, *arguments)
            self.assertEqual(invalid.returncode, 2)
            self.assertEqual(invalid.stdout, "")
            self.assertIn("usage: audit.py [--strict]", invalid.stderr)


    def run_cli(self, *arguments: str, absolute: bool = False):
        relative = "m3/tooling/name-contract/audit.py"
        script = self.root / relative
        script.parent.mkdir(parents=True, exist_ok=True)
        script.write_bytes((HERE / "audit.py").read_bytes())
        return subprocess.run(
            [sys.executable, str(script) if absolute else relative, *arguments],
            cwd=self.root, capture_output=True, text=True, check=False)

    def test_cli_accepts_repository_relative_and_absolute_script_paths(self):
        for absolute in (False, True):
            for arguments in ((), ("--strict",)):
                with self.subTest(absolute=absolute, arguments=arguments):
                    result = self.run_cli(*arguments, absolute=absolute)
                    self.assertEqual(result.returncode, 0, result.stderr)
                    self.assertIn("PRESENT_NAME_MATCH", result.stdout)
                    self.assertIn("rows=3 distinct_paths=2 attention=0", result.stderr)

    def test_cli_strict_retains_review_failure(self):
        (self.root / self.candidates[1]["path"]).unlink()
        result = self.run_cli("--strict")
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("REVIEW_NATIVE_MISSING", result.stdout)
        self.assertIn("attention=1", result.stderr)

    def test_cli_without_strict_reports_review_without_failing(self):
        (self.root / self.candidates[1]["path"]).unlink()
        result = self.run_cli()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("REVIEW_NATIVE_MISSING", result.stdout)
        self.assertIn("attention=1", result.stderr)

    def test_cli_rejects_unknown_and_repeated_flags(self):
        for arguments in (("--unknown",), ("--strict", "--strict")):
            with self.subTest(arguments=arguments):
                result = self.run_cli(*arguments)
                self.assertEqual(result.returncode, 2)
                self.assertEqual(result.stdout, "")
                self.assertIn("usage: audit.py [--strict]", result.stderr)


class WorkflowCoverageTest(unittest.TestCase):
    def test_sparse_checkout_covers_concrete_canonical_target_paths(self):
        root = AUDIT.ROOT
        workflow = root / ".github/workflows/m3jdk21-name-contract.yml"
        lines = workflow.read_text(encoding="utf-8").splitlines()
        starts = [i for i, line in enumerate(lines)
                  if line.strip() == "sparse-checkout: |"]
        self.assertEqual(len(starts), 1, "one explicit sparse-checkout pattern block")
        start = starts[0]
        indentation = len(lines[start]) - len(lines[start].lstrip())
        patterns = []
        for line in lines[start + 1:]:
            if not line.strip():
                continue
            if len(line) - len(line.lstrip()) <= indentation:
                break
            patterns.append(line.strip())
        self.assertTrue(patterns, "checkout must declare its source scope")
        records = json.loads((root / AUDIT.MAP).read_text(encoding="utf-8"))["migration"]["records"]
        targets = {target["path"] for record in records for target in record.get("targets", [])
                   if isinstance(target, dict) and target.get("path") and target.get("symbol")}
        # The workflow uses exact paths and explicit recursive owner subtrees.
        # Actual Git sparse-checkout behavior is also qualified in the recipe receipt.
        uncovered = sorted(path for path in targets
                           if not any(path == pattern or pattern.endswith("/**")
                                      and path.startswith(pattern[:-2]) for pattern in patterns))
        self.assertEqual(uncovered, [], "mapped source owners are absent from the checkout scope")


if __name__ == "__main__":
    unittest.main()
