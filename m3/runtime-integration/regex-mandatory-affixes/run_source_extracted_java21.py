#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Seal and exercise actual complete regex classes on stock Java21.

The immutable owner is a fixture; its cursor and fact mixers are extracted
from the actual target. This is not matched M3JDK/HotSpot/JNI qualification.
"""
from pathlib import Path
from hashlib import sha256
from tempfile import TemporaryDirectory
import argparse
import ast
import importlib.util
import subprocess
import sys

sys.dont_write_bytecode = True

SOURCE = "src/java.base/share/classes/java/lang/M3StringRegexDp.java"
TEST = "test/jdk/java/lang/String/M3StringRegexDpTest.java"
SUPPORT = "m3/runtime-integration/regex-plan-amalgamation/source_extracted_java21.py"
BEFORE_SHA = "9536a6df2042fe047f02ef07834edca91ce7a863e5b3c1daafffe55fdad48e6e"
AFTER_SHA = "3029950078524419af52edbcbaaccab545bd5b1bed824ee80218250b8c748ef4"
TEST_SHA = "b9bbf14010da526e41d0e78945e728d5e9aa1cab8ccc732a815a9a14a253ee2a"
DEPENDENCIES = {
    "src/java.base/share/classes/java/lang/M3String.java":
        "cd1192e27c981da03aeae2385d3d54ad31d912f043cf8c7dfba18c7b9106b728",
    "src/java.base/share/classes/java/lang/M3StringFacts.java":
        "d18122b42e9e4bcb76fd3e807467a59160e012d7ac249efe9cc71e5f6b8faa22",
}
SUPPORT_FUNCTIONS = {
    "extract": "f619f22acee114782ef037ce38b14fb4d6eaee12ff7910e74e93b9996f8a4b44",
    "fixture_owner": "307ed0e3fa81a30191e5c4661535a2e0e87a8fe83f0c65dcb91391d905c3601e",
    "rewrite_engine": "f9c3821f23aab7e65221807041498b487d063941518b868382f28e802923e96e",
}


def checked(raw, expected, label):
    actual = sha256(raw).hexdigest()
    if actual != expected:
        raise SystemExit("M3_REGEX_AFFIX_SOURCE_FAIL " + label + " sha256=" + actual)
    return raw.decode("utf-8", "strict")


def once(source, before, after):
    if source.count(before) != 1:
        raise SystemExit("M3_REGEX_AFFIX_FIXTURE_FAIL instrumentation anchor")
    return source.replace(before, after)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, default=Path(__file__).resolve().parents[3])
    parser.add_argument("--candidate-root", type=Path)
    parser.add_argument("--java-home", type=Path)
    args = parser.parse_args()
    root = args.source_root.resolve()
    candidate = (args.candidate_root or root).resolve()
    here = Path(__file__).resolve().parent
    previous = checked((here / "M3StringRegexDpBefore.java.txt").read_bytes(),
                       BEFORE_SHA, "before")
    updated = checked((candidate / SOURCE).read_bytes(), AFTER_SHA, "after")
    checked((candidate / TEST).read_bytes(), TEST_SHA, "jtreg")
    for path, wanted in DEPENDENCIES.items():
        checked((root / path).read_bytes(), wanted, path)
    support_source = (root / SUPPORT).read_text(encoding="utf-8")
    observed = {}
    for node in ast.parse(support_source).body:
        if isinstance(node, ast.FunctionDef) and node.name in SUPPORT_FUNCTIONS:
            observed[node.name] = sha256(ast.get_source_segment(
                support_source, node).encode("utf-8")).hexdigest()
    if observed != SUPPORT_FUNCTIONS:
        raise SystemExit("M3_REGEX_AFFIX_SOURCE_FAIL source-extraction support drift")
    spec = importlib.util.spec_from_file_location("affix_source_support", root / SUPPORT)
    support = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(support)
    support.ROOT = root
    fixture = support.fixture_owner()
    # Only fixture dependencies are instrumented. The actual engine and cursor
    # bodies remain byte-exact apart from package/class-name adaptation.
    fixture = once(fixture, "final class M3StringAtom extends M3StringOwner {",
                   "final class M3StringAtom extends M3StringOwner {\n    static long reads;")
    fixture = once(fixture, "    @Override char charAt(int index) { return value[index]; }",
                   "    @Override char charAt(int index) { reads++; return value[index]; }")
    fixture = once(fixture, "final class M3String {",
                   "final class M3String {\n    static long wrapperReads;")
    fixture = once(fixture, "    char charAt(int index) {\n        Objects.checkIndex(index, length);",
                   "    char charAt(int index) {\n        wrapperReads++;\n        Objects.checkIndex(index, length);")
    java = str(args.java_home / "bin/java") if args.java_home else "java"
    javac = str(args.java_home / "bin/javac") if args.java_home else "javac"
    with TemporaryDirectory(prefix="m3-regex-affixes-") as directory:
        work = Path(directory)
        for filename, content in [
            ("RegexBefore.java", support.rewrite_engine(previous, "RegexBefore")),
            ("RegexAfter.java", support.rewrite_engine(updated, "RegexAfter")),
            ("M3String.java", fixture),
        ]:
            (work / filename).write_text(content, encoding="utf-8")
        subprocess.run([javac, "--release", "21", "-proc:none", "-Xlint:all,-auxiliaryclass",
                        "-Werror", "-d", directory, str(work / "RegexBefore.java"),
                        str(work / "RegexAfter.java"), str(work / "M3String.java"),
                        str(here / "src/test/java/RegexAffixBeforeAfter.java"), str(candidate / TEST)],
                       check=True)
        subprocess.run([java, "-Xms256m", "-Xmx256m", "-ea", "-cp", directory,
                        "RegexAffixBeforeAfter"], check=True)
    print("M3_REGEX_AFFIX_EXTRACTED_JAVA_PASS full_class=true cursor_exact=true"
          " mock_owner=true matched_m3jdk=false")


if __name__ == "__main__":
    main()
