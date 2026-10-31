#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Per-atom V5 JUnit refusal controls, not synthetic substitute execution."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import xml.etree.ElementTree as ET
import verify_surefire_v5 as v5


class V5JUnitReceiptsTest(unittest.TestCase):
    def raw_historical(self):
        repo = Path(__file__).resolve().parents[5]
        file = repo / v5.HISTORICAL
        spec = importlib.util.spec_from_file_location("old_junit", file)
        assert spec and spec.loader
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        return module

    def temporary_suite(self, methods, *, bad=None):
        holder = tempfile.TemporaryDirectory()
        self.addCleanup(holder.cleanup)
        root = Path(holder.name)
        suite = ET.Element("testsuite", tests=str(len(methods)),
                           errors="0", failures="0", skipped="0")
        for key in methods:
            cls, name = key.split("#")
            case = ET.SubElement(suite, "testcase", classname=cls, name=name)
            if bad == "skip":
                ET.SubElement(case, "skipped")
            if bad == "flaky":
                ET.SubElement(case, "flakyFailure")
        ET.ElementTree(suite).write(root / "TEST-TwoAtomSample.xml")
        return root

    def test_six_exact_dedicated_atoms(self):
        self.assertEqual(2, len(v5.source_gate.EXPECTED))
        self.assertEqual(6, sum(len(row[3].split(","))
                                for row in v5.source_gate.EXPECTED))

    def test_original_junit_v4_and_v2_are_distinct_pinned_owners(self):
        self.assertNotEqual(v5.JUNIT_V4, v5.HISTORICAL)
        self.assertEqual(40, len(v5.JUNIT_V4_BLOB))
        self.assertEqual(40, len(v5.HISTORICAL_BLOB))

    def test_original_surefire_verifier_accepts_exact_test_identity(self):
        old = self.raw_historical()
        identities = {"jdk.m3.collections.interop.M3Flat3MapProviderTest#one",
                      "jdk.m3.collections.interop.M3FrozenLongSetProviderTest#two"}
        self.assertEqual(1, old.verify_reports(self.temporary_suite(identities), identities))

    def test_original_surefire_verifier_refuses_missing_test(self):
        old = self.raw_historical()
        ids = {"jdk.m3.collections.interop.A#one", "jdk.m3.collections.interop.B#two"}
        with self.assertRaisesRegex(ValueError, "JUnit execution diverged"):
            old.verify_reports(self.temporary_suite({next(iter(ids))}), ids)

    def test_original_surefire_verifier_refuses_skipped_test(self):
        old = self.raw_historical()
        ids = {"jdk.m3.collections.interop.A#one"}
        with self.assertRaisesRegex(ValueError, "failed or skipped"):
            old.verify_reports(self.temporary_suite(ids, bad="skip"), ids)

    def test_v5_rejects_flaky_or_rerun_test(self):
        ids = {"jdk.m3.collections.interop.A#one"}
        with self.assertRaisesRegex(ValueError, "hidden flaky"):
            v5.reject_flaky(self.temporary_suite(ids, bad="flaky"))

    def test_v5_rejects_aliased_report(self):
        holder = tempfile.TemporaryDirectory()
        self.addCleanup(holder.cleanup)
        root = Path(holder.name)
        source = self.temporary_suite({"jdk.m3.collections.interop.A#one"})
        (root / "TEST-Link.xml").symlink_to(next(source.glob("TEST-*.xml")))
        with self.assertRaisesRegex(ValueError, "aliased"):
            v5.reject_flaky(root)


if __name__ == "__main__":
    unittest.main(verbosity=2)
