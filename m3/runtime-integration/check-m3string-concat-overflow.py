#!/usr/bin/env python3
"""Source gate for M3 String concat logical-length overflow: JDK OOME is required."""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
POOL = ROOT / "src/java.base/share/classes/java/lang/M3StringPool.java"

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
        if "--self-test" in sys.argv[1:]:
            self_test(text)
    except (OSError, ValueError, AssertionError) as error:
        raise SystemExit("M3_STRING_CONCAT_OVERFLOW_GUARD_FAIL|" + str(error)) from error
    print("M3_STRING_CONCAT_OVERFLOW_GUARD_PASS")
