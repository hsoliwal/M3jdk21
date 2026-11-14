#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Source-sealed current JUnit census; frozen donor APIs and unchanged executed XML."""
import argparse
import ast
import hashlib
import json
from pathlib import Path
import re
import types
import xml.etree.ElementTree as ET

HERE = Path(__file__).resolve().parent
DATA = {"CURRENT_187_METHOD_IDS.txt":"ce985659ee97d187d9cfcc93311cdc9e5942457b78fb04ef0ec8d9c9a17367c5","ORIGINAL_167_METHOD_IDS.txt":"1125263f0dd05e6db1ec5a510f7baafcea3129575a6668a8299f0530ba4aebf5","SOURCE_BINDINGS.tsv":"7782512185936c4c32e6bde64b22bf65d6a1abc981bd52ca0f8838ee6b52adea"}
APPROVED = "b123804aff3ac6930e420cfb82b5ce07040049fe"
PREFIX = "m3/collections/interop/qualification/"
V4 = PREFIX + "current-junit-census-20261026/verify_current_surefire.py"
XML = PREFIX + "runtime-junit-census-20261022/verify_surefire.py"
V5 = PREFIX + "current-junit-census-v5-20261031/verify_surefire_v5.py"
SOURCE = PREFIX + "current-source-successors-v4-20261031/verify_source_v4.py"
PINS = {V4: "f3995e0ff372fae238062de29dfa56ac08ab4a95",
        XML: "91a438127d48fac9b32104a1e98f1a3eb18bdc5d",
        V5: "b40def5f81537592ae00ac416ad12aa2f40b1ef0",
        SOURCE: "f405a14690a3ab181bb5c021ca27b09258c74e65"}
HISTORY = (
 ("runtime-junit-census-20261022/receipts/expected-00.tsv", "56bca22881cd0e8ad6b90c3b5582ddbd5ae0ef7d"),
 ("runtime-junit-census-20261022/receipts/expected-01.tsv", "e1e57850ac92df6e36743561044e134375aa128f"),
 ("runtime-junit-census-20261022/receipts/expected-02.tsv", "b3c2fd2ffd2c71543950e9f788ecce7e875a03e3"),
 ("runtime-junit-census-20261022/receipts/expected-03.tsv", "5a3e8a287f14898dfc3b6ba7da5f3e5443b87ad2"),
 ("current-junit-census-20261026/CURRENT_JUNIT_METHOD_SUCCESSION.tsv", "e8dac7d09e38caf446aaf2e6edbf20715bc073b0"))
LEGACY_SHA = "f6ffa76471ff77cb75072bece60a19faf185ce25f97998c095e886c265768efb"
LATER_SHA = "88b249f5c0f430ea240567abbe4e6fff977fab9b38f3d5d5cb462bad6530c2dd"
OLD = "com.m3.collections.interop."
NEW = "jdk.m3.collections.interop."

def git_blob(data: bytes) -> str:
    return hashlib.sha1(b'blob ' + str(len(data)).encode('ascii') + b'\0' + data).hexdigest()

def checked_file(root: Path, relative: str) -> bytes:
    """Reject parent aliases as well as the direct-file aliases rejected by V3."""
    relative_path = Path(relative)
    if (relative_path.is_absolute() or relative_path.as_posix() != relative
            or ".." in relative_path.parts or "\\" in relative):
        raise ValueError("unsafe source path: " + relative)
    cursor = root / relative_path
    while cursor != root:
        if cursor.is_symlink():
            raise ValueError("missing or aliased source: " + relative)
        cursor = cursor.parent
    path = root / relative_path
    if not path.is_file() or not path.resolve().is_relative_to(root):
        raise ValueError("missing or aliased source: " + relative)
    return path.read_bytes()

def source_module(name: str, file: Path, raw: bytes):
    module = types.ModuleType(name)
    module.__file__ = str(file)
    exec(compile(raw, module.__file__, "exec"), module.__dict__)
    return module

def module_at(root, relative):
    raw = checked_file(root, relative)
    if git_blob(raw) != PINS[relative]:
        raise ValueError("frozen executable authority drift: " + relative)
    return source_module("_m3_census_" + PINS[relative], root / relative, raw)


def flaky_leaf(product):
    raw = checked_file(product, V5)
    if git_blob(raw) != PINS[V5]:
        raise ValueError("frozen V5 authority drift")
    nodes = [node for node in ast.parse(raw).body
             if isinstance(node, ast.FunctionDef) and node.name == "reject_flaky"]
    if len(nodes) != 1:
        raise ValueError("missing frozen flaky-result leaf")
    scope = {"Path": Path, "ET": ET}
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(product / V5), "exec"), scope)
    return scope["reject_flaky"]


def read_data(name):
    raw = checked_file(HERE, name)
    if hashlib.sha256(raw).hexdigest() != DATA[name]:
        raise ValueError("fixed census data drift: " + name)
    return raw


def ids(name, prefix, count):
    rows = read_data(name).decode("utf-8").splitlines()
    if (rows != sorted(set(rows)) or len(rows) != count
            or any(not re.fullmatch(re.escape(prefix) + r"[A-Za-z_$][\w$]*#[A-Za-z_$][\w$]*", row)
                   for row in rows)):
        raise ValueError("fixed method identity shape/count drift")
    return set(rows)


def digest_ids(values):
    return hashlib.sha256(("\n".join(sorted(values)) + "\n").encode()).hexdigest()


def sources(product, current):
    rows = [line.split("\t") for line in read_data("SOURCE_BINDINGS.tsv").decode().splitlines()
            if line and not line.startswith("#")]
    paths = [row[0] for row in rows]
    if len(rows) != 52 or any(len(row) != 6 for row in rows) or len(set(paths)) != 52:
        raise ValueError("fixed source owner scope drift")
    directory = product / "m3/collections/interop/src/test/java/jdk/m3/collections/interop"
    found = set()
    for path in directory.rglob("*.java"):
        relative = path.relative_to(product).as_posix()
        checked_file(product, relative)
        found.add(relative)
    if found != set(paths):
        raise ValueError("unrecorded or missing current JUnit source owner")
    expected_counts = {}
    later = set()
    for path, sha, blob, count, disposition, attribution in rows:
        if not path.startswith("m3/collections/interop/src/test/java/jdk/m3/collections/interop/") or not path.endswith(".java"):
            raise ValueError("out-of-scope source owner")
        raw = checked_file(product, path)
        if hashlib.sha256(raw).hexdigest() != sha or git_blob(raw) != blob:
            raise ValueError("current JUnit source seal drift: " + path)
        owner = NEW + Path(path).stem
        expected_counts[owner] = int(count)
        if disposition == "LATER_QUALIFIED_ADDITION":
            later.add(owner)
    actual_counts = {owner: sum(value.startswith(owner + "#") for value in current)
                     for owner in expected_counts}
    if actual_counts != expected_counts or sum(actual_counts.values()) != 187:
        raise ValueError("source/method inventory drift")
    if later != {NEW + "M3BagInteropTest", NEW + "M3FiveProviderIntegrationTest"}:
        raise ValueError("later source succession scope drift")
    return later


class ReportsView:
    """Only expose original report Paths; never copy or rewrite XML."""
    def __init__(self, files):
        self.files = tuple(files)

    def glob(self, pattern):
        if pattern != "TEST-*.xml":
            raise ValueError("unapproved report selection")
        return self.files


def report_snapshot(files):
    return tuple((str(path), hashlib.sha256(path.read_bytes()).hexdigest()) for path in files)


def qualify(product, approved, before, historical, reports):
    product, approved, before, historical = (
        root.resolve(strict=True) for root in (product, approved, before, historical))
    current = ids("CURRENT_187_METHOD_IDS.txt", NEW, 187)
    original = ids("ORIGINAL_167_METHOD_IDS.txt", OLD, 167)
    mapped = {NEW + value[len(OLD):] for value in original}
    later_classes = sources(product, current)
    for relative, pin in HISTORY:
        if git_blob(checked_file(approved, PREFIX + relative)) != pin:
            raise ValueError("original method-inventory provenance drift")
    original_gate = module_at(approved, V4)
    xml_gate = module_at(approved, XML)
    source_gate = module_at(product, SOURCE)
    provider = source_gate.prove(product, approved, before, historical)
    legacy = mapped | provider
    later = current - legacy
    if (len(mapped) != 167 or len(provider) != 6 or mapped & provider
            or len(legacy) != 173 or not legacy.issubset(current)
            or len(later) != 14 or digest_ids(legacy) != LEGACY_SHA
            or digest_ids(later) != LATER_SHA
            or {value.split("#")[0] for value in later} != later_classes):
        raise ValueError("namespace or exact 167/6/14 method partition drift")
    if reports.is_symlink() or not reports.is_dir():
        raise ValueError("missing or aliased report directory")
    files = tuple(sorted(reports.glob("TEST-*.xml")))
    if len(files) != 41 or any(path.is_symlink() or not path.is_file() for path in files):
        raise ValueError("complete 41-suite actual XML required")
    before_hashes = report_snapshot(files)
    if xml_gate.verify_reports(ReportsView(files), current) != 41:
        raise ValueError("full 187-method actual XML suite count drift")
    flaky_leaf(product)(ReportsView(files))
    groups = {"original": [], "provider": [], "later": []}
    sets = {"original": mapped, "provider": provider, "later": later}
    for file in files:
        suite = ET.parse(file).getroot()
        cases = suite.findall("testcase")
        identities = {case.get("classname", "") + "#" + case.get("name", "") for case in cases}
        classes = {case.get("classname", "") for case in cases}
        if len(classes) != 1 or not cases or file.name != "TEST-" + next(iter(classes)) + ".xml":
            raise ValueError("impure or renamed actual report")
        selected = [name for name, expected in sets.items() if identities.issubset(expected)]
        if len(selected) != 1:
            raise ValueError("mixed or unowned actual report")
        groups[selected[0]].append(file)
    if tuple(map(len, (groups["original"], groups["provider"], groups["later"]))) != (37, 2, 2):
        raise ValueError("original/provider/later report partition drift")
    if original_gate.verify_reports(approved, ReportsView(groups["original"]), mapped) != 37:
        raise ValueError("frozen original V4 API replay failed")
    if xml_gate.verify_reports(ReportsView(groups["original"] + groups["provider"]), legacy) != 39:
        raise ValueError("legacy 173-method execution failed")
    if xml_gate.verify_reports(ReportsView(groups["later"]), later) != 2:
        raise ValueError("later 14-method execution failed")
    if before_hashes != report_snapshot(files):
        raise ValueError("actual XML changed during read-only verification")
    for relative in (V4, XML):
        if git_blob(checked_file(approved, relative)) != PINS[relative]:
            raise ValueError("historical authority changed during replay")
    return {"sources": 52, "methods": 187, "suites": 41, "original": 167,
            "provider": 6, "legacy": 173, "later": 14,
            "originalV4ApiReplaySuites": 37, "historicalUnittest": "NOT_RUN",
            "rawReportSetSHA256": hashlib.sha256(json.dumps(before_hashes).encode()).hexdigest()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    for option in ("product", "approved-product", "before-product", "historical-product", "reports"):
        parser.add_argument("--" + option, type=Path, required=True)
    args = parser.parse_args()
    result = qualify(args.product, args.approved_product, args.before_product,
                     args.historical_product, args.reports)
    print("M3_INTEROP_JUNIT_V6_PASS " + json.dumps(result, sort_keys=True))


if __name__ == "__main__":
    main()
