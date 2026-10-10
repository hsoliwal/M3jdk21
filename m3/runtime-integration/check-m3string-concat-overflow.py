#!/usr/bin/env python3
"""Source gate for M3 String concat logical-length overflow: JDK OOME is required."""
from __future__ import annotations

import pathlib
import re
import sys
import shlex

ROOT = pathlib.Path(__file__).resolve().parents[2]
POOL = ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java"
WORKFLOW = ROOT / ".github/workflows/mindex-string-backing.yml"

# Copyright 2026 Hitesh Soliwal and contributors.
# Synexia #10152 / M3JDK #668 coverage, composed with #670; newer invariants stay untouched.
REQUIRED_TESTS = frozenset({
    'test/jdk/java/lang/String/M3MappedStringBackingTest.java',
    'test/jdk/java/lang/String/MapGuardTest.java',
    'test/jdk/java/lang/String/MapFactsTest.java',
    'test/jdk/java/lang/String/MapUtfTest.java',
    'test/jdk/java/lang/String/M3StringFactsCompositionTest.java',
    'test/jdk/java/lang/String/M3StringCanonicalDagTest.java',
    'test/jdk/java/lang/String/M3StringStreamsTest.java',
    'test/jdk/java/lang/String/M3StringPrecomputeSearchTest.java',
    'test/jdk/java/lang/String/M3StringRegexReplacementTest.java',
    'test/jdk/java/lang/String/M3StringInternTest.java',
    'test/jdk/java/lang/String/M3StringCharArrayHistoryTest.java',
    'test/jdk/java/lang/String/M3StringConcatReferenceDagTest.java',
    'test/jdk/java/lang/String/M3StringJoinCarryTest.java',
    'test/jdk/java/lang/String/M3StringBinaryConcatTest.java',
    'test/jdk/java/lang/String/M3StringConcatLengthOverflowTest.java',
    'test/jdk/java/lang/String/M3StringCodePointGeometryTest.java',
    'test/jdk/java/lang/String/M3StringLiteralMatchesTest.java',
    'test/jdk/java/lang/String/M3StringBuilderRangeBulkTest.java',
    'test/jdk/java/lang/String/M3StringByteSourceNeedleTest.java',
    'test/jdk/java/lang/String/M3StringMixedLiteralReplaceTest.java',
    'test/jdk/java/lang/String/M3StringRegexSplitHistoryTest.java',
    'test/jdk/jdk/internal/mindex/M3TQFactsTest.java',
    'test/jdk/java/util/regex/M3RegexLiteralTQTest.java',
    'test/jdk/java/util/regex/M3RegexReuseTest.java',
    'test/jdk/java/lang/String/M3StringConcatTupleTest.java',
    'test/jdk/java/lang/String/M3StringFactsMixedConsumersTest.java',
    'test/jdk/java/lang/String/M3StringJniShadowLifetimeTest.java',
    'test/jdk/java/lang/String/M3StringLiteralSplitTest.java',
    'test/jdk/java/lang/String/M3StringOwnerRangeLifetimeTest.java',
    'test/jdk/java/lang/String/M3StringPositionInvariant6Test.java',
    'test/jdk/java/lang/String/M3StringRegexSplitCurrentTest.java',
    'test/jdk/java/lang/String/M3StringUtf16DifferentialTest.java',
    'test/jdk/java/lang/String/M3TQSparseContainmentTest.java',
    'test/jdk/java/lang/String/M3TQTest.java',
})

def verify(source: str) -> None:
    match = re.search(
        r"static M3String concat\(M3String left, M3String right\)\s*\{(.*?)\n    \}",
        source, flags=re.DOTALL)
    if match is None:
        raise ValueError("M3StringPool.concat owner missing")
    body = match.group(1)
    guard = (
        "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {"
        '\n            throw new OutOfMemoryError("Required length exceeds implementation limit");'
        "\n        }"
    )
    offset = body.find(guard)
    if offset < 0:
        raise ValueError("logical-length OOME overflow guard absent or incorrect")
    before = body.find("if (left.length() == 0) return right;")
    after = body.find("if (left.owner() == right.owner()")
    balanced = body.find("return concatBalanced(left, right);")
    if before < 0 or after < 0 or balanced < 0 or not before < offset < after < balanced:
        raise ValueError("overflow guard must precede both tuple and range composition")
    if "catch (ArithmeticException" in body:
        raise ValueError("ArithmeticException must not be used as runtime size authority")

def verify_workflow(workflow: str) -> None:
    """Require the 34-test union in the executed command; allow future additive tests."""
    commands = re.findall(r'^\s+run: (make test TEST=.*)$', workflow, flags=re.MULTILINE)
    if len(commands) != 1:
        raise ValueError("expected one explicit matched-image jtreg command")
    tokens = shlex.split(commands[0])
    if len(tokens) != 3 or tokens[:2] != ["make", "test"] or not tokens[2].startswith("TEST="):
        raise ValueError("jtreg command changed its explicit make-test contract")
    selected = tokens[2][len("TEST="):].split()
    if len(selected) != len(set(selected)):
        raise ValueError("duplicate jtreg selectors")
    missing = REQUIRED_TESTS - set(selected)
    if missing:
        raise ValueError("lost jtreg selectors: " + ", ".join(sorted(missing)))
    for trigger in ("test/jdk/java/lang/String/M3*.java", "test/jdk/java/lang/String/libM3TQTest.cpp"):
        if not re.search(r"^\s+- ['\"]" + re.escape(trigger) + r"['\"]\s*$", workflow, re.MULTILINE):
            raise ValueError("lost source/support trigger: " + trigger)
    for check in ("check-m3string-invariants.py", "check-m3string-regex-split-current.py",
                  "check-m3string-concat-overflow.py --self-test"):
        command = "python3 m3/runtime-integration/" + check
        if len(re.findall(r'^\s+run: ' + re.escape(command) + r'\s*$', workflow, re.MULTILINE)) != 1:
            raise ValueError("lost or duplicate source gate: " + check)


def workflow_self_test(workflow: str) -> None:
    verify_workflow(workflow)
    command = next(line for line in workflow.splitlines() if line.strip().startswith("run: make test TEST="))
    # Every selected test must remain executed, even if its name survives elsewhere.
    mutants = [workflow.replace(command, command.replace(path, "")) + "\n# " + path
               for path in sorted(REQUIRED_TESTS)]
    sample = min(REQUIRED_TESTS)
    mutants.extend([
        workflow.replace(command, command[:-1] + " " + sample + '"'),
        workflow + "\n" + command + "\n",
        workflow.replace(command, command[:-1]),
    ])
    for index, candidate in enumerate(mutants):
        try:
            verify_workflow(candidate)
        except ValueError:
            continue
        raise AssertionError("workflow mutant admitted: " + str(index))

def self_test(source: str) -> None:
    verify(source)
    guard = (
        "if ((long) left.length() + right.length() > Integer.MAX_VALUE) {"
        '\n            throw new OutOfMemoryError("Required length exceeds implementation limit");'
        "\n        }"
    )
    invalid = [
        source.replace(guard, guard.replace("OutOfMemoryError", "ArithmeticException"), 1),
        source.replace(guard, guard.replace("Integer.MAX_VALUE", "Integer.MIN_VALUE"), 1),
        source.replace(guard, "", 1),
    ]
    for index, candidate in enumerate(invalid):
        try:
            verify(candidate)
        except ValueError:
            continue
        raise AssertionError("mutant admitted: " + str(index))

if __name__ == "__main__":
    try:
        text = POOL.read_text(encoding="utf-8")
        verify(text)
        workflow = WORKFLOW.read_text(encoding="utf-8")
        verify_workflow(workflow)
        if "--self-test" in sys.argv[1:]:
            self_test(text)
            workflow_self_test(workflow)
    except (OSError, ValueError, AssertionError) as error:
        raise SystemExit("M3_STRING_CONCAT_OVERFLOW_GUARD_FAIL|" + str(error)) from error
    print("M3_STRING_CONCAT_OVERFLOW_GUARD_PASS")
