#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""JUnit V5: preserve actual original 167-method V4, admit six source-sealed atom tests.

Executed Surefire reports are authoritative. No test can be skipped, renamed,
silently flaked, duplicated, or replaced by an invented method receipt.
"""
import argparse
import importlib.util
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "current-source-successors-v4-20261031"))
import verify_source_v4 as source_gate

JUNIT_V4 = ("m3/collections/interop/qualification/"
            "current-junit-census-20261026/verify_current_surefire.py")
JUNIT_V4_BLOB = "f3995e0ff372fae238062de29dfa56ac08ab4a95"
HISTORICAL = ("m3/collections/interop/qualification/"
              "runtime-junit-census-20261022/verify_surefire.py")
HISTORICAL_BLOB = "0f8f6c50854d53672c3a07d26473b8457d439cbf"


def load_exact(approved: Path, relative: str, blob: str, module_name: str):
    raw = source_gate.v3.checked_bytes(approved, relative)
    if source_gate.v3.git_blob(raw) != blob:
        raise ValueError("historical JUnit verification authority drift: " + relative)
    file = approved / relative
    spec = importlib.util.spec_from_file_location(module_name, file)
    if spec is None or spec.loader is None:
        raise ValueError("missing historical executable JUnit verifier")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def expected_methods(product: Path, approved: Path,
                     before: Path, historical: Path) -> set[str]:
    added = source_gate.prove(product, approved, before, historical)
    approved, product = approved.resolve(strict=True), product.resolve(strict=True)
    old = load_exact(approved, JUNIT_V4, JUNIT_V4_BLOB, "_m3_original_v4_surefire")
    if source_gate.v3.checked_bytes(product, JUNIT_V4) != source_gate.v3.checked_bytes(approved, JUNIT_V4):
        raise ValueError("original V4 JUnit verifier was modified")
    baseline = old.expected_methods(approved)
    if len(baseline) != 167 or len(added) != 6 or baseline & added:
        raise ValueError("wrong original or added dedicated JUnit atom inventory")
    expected = baseline | added
    if len(expected) != 173:
        raise ValueError("incomplete V5 JUnit test method inventory")
    return expected


def reject_flaky(reports: Path):
    for file in sorted(reports.glob("TEST-*.xml")):
        if file.is_symlink():
            raise ValueError("aliased Surefire report")
        root = ET.parse(file).getroot()
        for case in root.findall("testcase"):
            if any(part.tag in ("flakyFailure", "flakyError", "rerunFailure", "rerunError")
                   for part in case):
                raise ValueError("hidden flaky/rerun JUnit failure: " + file.name)


def verify_reports(approved: Path, reports: Path, expected: set[str]) -> int:
    old = load_exact(approved.resolve(strict=True), HISTORICAL, HISTORICAL_BLOB,
                     "_m3_original_surefire")
    suites = old.verify_reports(reports, expected)
    if suites != 39:
        raise ValueError("expected 39 executed JUnit suites, found " + str(suites))
    reject_flaky(reports)
    return suites


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--approved-product", type=Path, required=True)
    parser.add_argument("--before-product", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    parser.add_argument("--reports", type=Path, required=True)
    args = parser.parse_args()
    expected = expected_methods(args.product, args.approved_product,
                                args.before_product, args.historical_product)
    suites = verify_reports(args.approved_product, args.reports, expected)
    print("M3_INTEROP_JUNIT_V5_PASS sources=50 methods=173 suites=" + str(suites)
          + " original=167 added=6 native=MANDATORY")


if __name__ == "__main__":
    main()
