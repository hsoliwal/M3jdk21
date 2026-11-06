#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Replay the original V4 negative controls on *actual* V4-approved sources.

After source V4, current V5 and all 173 actual Surefire methods pass, project
the original 167 executed XML cases without editing test results. No synthetic
JUnit green or modification of the original source/receipt/verifier is allowed.
"""
import argparse
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

import verify_surefire_v5 as v5

ORIGINAL_TEST_DIR = ("m3/collections/interop/qualification/"
                     "current-junit-census-20261026")
REPORTS_REL = "m3/collections/interop/target/surefire-reports"
NEW_CLASSES = frozenset(
    "com.m3.collections.interop." + Path(row[0]).stem
    for row in v5.source_gate.EXPECTED
)


def additional_ids() -> set[str]:
    return {
        "com.m3.collections.interop." + Path(row[0]).stem + "#" + method
        for row in v5.source_gate.EXPECTED for method in row[3].split(",")
    }


def project_executed_xml(reports: Path, destination: Path,
                         added: set[str]) -> tuple[int, set[str]]:
    """Copy unmodified historical-suite XML, rejecting any alien or mixed case."""
    if destination.exists() or destination.is_symlink():
        raise ValueError("historical projection target already exists")
    if reports.is_symlink() or not reports.is_dir():
        raise ValueError("missing or aliased executed JUnit reports")
    files = sorted(reports.glob("TEST-*.xml"))
    if len(files) != 39:
        raise ValueError("not the complete executed 39-suite corpus")
    destination.mkdir(parents=True)
    omitted = set()
    copied = 0
    for file in files:
        if file.is_symlink() or not file.is_file():
            raise ValueError("aliased JUnit source report")
        suite = ET.parse(file).getroot()
        if suite.tag != "testsuite":
            raise ValueError("non-Surefire JUnit XML")
        cases = suite.findall("testcase")
        if len(cases) != int(suite.get("tests", "-1")):
            raise ValueError("incomplete Surefire suite")
        ids = {case.get("classname", "") + "#" + case.get("name", "")
               for case in cases}
        if len(ids) != len(cases) or not ids:
            raise ValueError("empty or duplicated JUnit case identities")
        if ids & added:
            if ids - added or len({case.get("classname") for case in cases}) != 1:
                raise ValueError("mixed new and historical JUnit identities")
            clazz = cases[0].get("classname")
            if clazz not in NEW_CLASSES or file.name != "TEST-" + clazz + ".xml":
                raise ValueError("unrecognized new provider test report")
            if omitted & ids:
                raise ValueError("duplicate provider atom report")
            omitted.update(ids)
            continue
        if ids & omitted:
            raise ValueError("duplicate executed JUnit identities")
        shutil.copyfile(file, destination / file.name)
        copied += 1
    if copied != 37 or omitted != added:
        raise ValueError("historical projection does not contain exactly 167 methods")
    return copied, omitted


def replay(product: Path, approved: Path, before: Path,
           historical: Path, reports: Path) -> None:
    product, approved, before, historical, reports = (
        path.resolve(strict=True)
        for path in (product, approved, before, historical, reports))
    expected = v5.expected_methods(product, approved, before, historical)
    if len(expected) != 173 or v5.verify_reports(approved, reports, expected) != 39:
        raise ValueError("current actual JUnit V5 corpus not admitted")
    addition = additional_ids()
    if len(addition) != 6 or not addition.issubset(expected):
        raise ValueError("unapproved new JUnit atom identities")
    old = v5.load_exact(approved, v5.JUNIT_V4, v5.JUNIT_V4_BLOB,
                        "_m3_approved_junit_v4")
    prior = old.expected_methods(approved)
    if len(prior) != 167 or prior & addition or prior | addition != expected:
        raise ValueError("original 167-method and new 6-method evidence diverged")

    temporary_reports = approved / REPORTS_REL
    if temporary_reports.exists() or temporary_reports.is_symlink():
        raise ValueError("refuse to overwrite historical reports")
    try:
        suites, omitted = project_executed_xml(reports, temporary_reports, addition)
        if len(omitted) != 6 or old.verify_reports(temporary_reports, prior) != suites:
            raise ValueError("historical real-output V4 verification failed")
        result = subprocess.run(
            [sys.executable, "-m", "unittest", "discover",
             "-s", str(approved / ORIGINAL_TEST_DIR),
             "-p", "test_current_surefire.py", "-v"],
            cwd=approved, check=False)
        if result.returncode != 0:
            raise ValueError("historical V4 negative-control execution failed")
    finally:
        if temporary_reports.is_dir() and not temporary_reports.is_symlink():
            shutil.rmtree(temporary_reports)
    print("M3_ORIGINAL_V4_JUNIT_REPLAY_PASS methods=167 suites=37 "
          "new_junit_methods=6 original_negative_controls=EXECUTED")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--product", type=Path, required=True)
    parser.add_argument("--approved-product", type=Path, required=True)
    parser.add_argument("--before-product", type=Path, required=True)
    parser.add_argument("--historical-product", type=Path, required=True)
    parser.add_argument("--reports", type=Path, required=True)
    options = parser.parse_args()
    replay(options.product, options.approved_product, options.before_product,
           options.historical_product, options.reports)


if __name__ == "__main__":
    main()
