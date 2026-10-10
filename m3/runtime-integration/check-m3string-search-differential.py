#!/usr/bin/env python3
"""Independent UTF-16 search proof for M3StringSearchPrecompute.

The receiver source is checked for bounded weak-owner plans, forward/reverse
KMP, adaptive BMH, and optional trigram facts. Search behavior is then tested
against a naive UTF-16 reference using an independent model. This does not
claim target image/JNI performance or substitute for String's exact oracle.
"""

from __future__ import annotations

from pathlib import Path
import random
import sys


ROOT = Path(__file__).resolve().parents[2]


def fail(message: str) -> None:
    raise SystemExit(f"M3_STRING_SEARCH_DIFFERENTIAL_FAIL|{message}")


def read(relative: str) -> str:
    path = ROOT / relative
    if not path.is_file():
        fail(f"missing={relative}")
    return path.read_text(encoding="utf-8")


def require(source: str, fragment: str, label: str) -> None:
    if fragment not in source:
        fail(f"source_contract={label}|missing={fragment}")


def prefix_table(pattern: tuple[int, ...]) -> tuple[int, ...]:
    table = [0] * len(pattern)
    for index in range(1, len(pattern)):
        matched = table[index - 1]
        while matched > 0 and pattern[index] != pattern[matched]:
            matched = table[matched - 1]
        if pattern[index] == pattern[matched]:
            matched += 1
        table[index] = matched
    return tuple(table)


def reverse_prefix_table(pattern: tuple[int, ...]) -> tuple[int, ...]:
    reverse = tuple(reversed(pattern))
    return prefix_table(reverse)


def skip_table(pattern: tuple[int, ...]) -> tuple[int, ...]:
    length = len(pattern)
    table = [max(1, length)] * 256
    for index in range(length - 1):
        table[pattern[index] & 255] = length - 1 - index
    return tuple(table)


def naive_index(source: tuple[int, ...], pattern: tuple[int, ...], start: int) -> int:
    start = max(0, start)
    if not pattern:
        return min(start, len(source))
    limit = len(source) - len(pattern)
    for candidate in range(start, limit + 1):
        if source[candidate:candidate + len(pattern)] == pattern:
            return candidate
    return -1


def naive_last(source: tuple[int, ...], pattern: tuple[int, ...], start: int) -> int:
    if not pattern:
        return min(max(start, 0), len(source))
    maximum = min(start, len(source) - len(pattern))
    for candidate in range(maximum, -1, -1):
        if source[candidate:candidate + len(pattern)] == pattern:
            return candidate
    return -1


def kmp(source: tuple[int, ...], pattern: tuple[int, ...],
        prefix: tuple[int, ...], start: int) -> int:
    matched = 0
    for index in range(max(0, start), len(source)):
        unit = source[index]
        while matched > 0 and unit != pattern[matched]:
            matched = prefix[matched - 1]
        if unit == pattern[matched]:
            matched += 1
        if matched == len(pattern):
            return index - len(pattern) + 1
    return -1


def adaptive_bmh(source: tuple[int, ...], pattern: tuple[int, ...],
                 prefix: tuple[int, ...], skips: tuple[int, ...],
                 start: int) -> int:
    maximum_start = len(source) - len(pattern)
    at = max(0, start)
    failed_work = 0
    while at <= maximum_start:
        index = len(pattern) - 1
        while index >= 0 and pattern[index] == source[at + index]:
            index -= 1
        if index < 0:
            return at

        shift = skips[source[at + len(pattern) - 1] & 255]
        if shift > maximum_start - at:
            return -1
        at += shift
        failed_work += len(pattern) - index
        if failed_work > len(pattern) + 2 * (at - start):
            return kmp(source, pattern, prefix, at)
    return -1


def planned_index(source: tuple[int, ...], pattern: tuple[int, ...], start: int) -> int:
    start = max(0, start)
    if not pattern:
        return min(start, len(source))
    if len(pattern) > len(source) - start:
        return -1
    if len(pattern) == 1:
        try:
            return source.index(pattern[0], start)
        except ValueError:
            return -1
    prefix = prefix_table(pattern)
    if len(pattern) >= 8:
        return adaptive_bmh(source, pattern, prefix, skip_table(pattern), start)
    return kmp(source, pattern, prefix, start)


def planned_last(source: tuple[int, ...], pattern: tuple[int, ...], start: int) -> int:
    if not pattern:
        return min(max(start, 0), len(source))
    maximum_start = min(start, len(source) - len(pattern))
    if maximum_start < 0:
        return -1
    if len(pattern) == 1:
        for index in range(maximum_start, -1, -1):
            if source[index] == pattern[0]:
                return index
        return -1

    reverse_prefix = reverse_prefix_table(pattern)
    matched = 0
    scan_start = maximum_start + len(pattern) - 1
    for index in range(scan_start, -1, -1):
        unit = source[index]
        reverse_index = len(pattern) - 1 - matched
        while matched > 0 and unit != pattern[reverse_index]:
            matched = reverse_prefix[matched - 1]
            reverse_index = len(pattern) - 1 - matched
        if unit == pattern[reverse_index]:
            matched += 1
        if matched == len(pattern):
            return index
    return -1


def check_case(source: tuple[int, ...], pattern: tuple[int, ...],
               case_number: int) -> int:
    starts = (-2, 0, 1, len(source) // 2, len(source), len(source) + 2)
    queries = 0
    for start in starts:
        expected = naive_index(source, pattern, start)
        actual = planned_index(source, pattern, start)
        if actual != expected:
            fail(f"case={case_number}|index|start={start}|expected={expected}|actual={actual}")
        expected_last = naive_last(source, pattern, start)
        actual_last = planned_last(source, pattern, start)
        if actual_last != expected_last:
            fail(f"case={case_number}|last|start={start}|expected={expected_last}|actual={actual_last}")
        queries += 1
    return queries * 2


def main() -> None:
    search = read("src/java.base/share/classes/java/lang/M3StringSearchPrecompute.java")
    for fragment, label in [
        ("private static final int SLOTS = 256;", "bounded-pattern-cache"),
        ("private static final int MAX_PATTERN_UNITS = 8_192;", "pattern-budget"),
        ("private static final int SOURCE_SLOTS = 64;", "bounded-source-cache"),
        ("private static final int MAX_TRIGRAM_SOURCE_UNITS = 32_768;", "source-budget"),
        ("int[] prefix", "forward-kmp"),
        ("int[] reversePrefix", "reverse-kmp"),
        ("int[] skip256", "bmh-skip"),
        ("static int indexOf(", "forward-search"),
        ("static int lastIndexOf(", "reverse-search"),
        ("private static int adaptiveBmh(", "adaptive-bmh"),
        ("failedComparisonWork", "adversarial-work-budget"),
        ("return kmp(source, pattern, plan, at, endIndex);", "exact-kmp-fallback"),
        ("WeakReference<M3StringOwner>", "weak-owner"),
        ("M3TQ.Facts trigrams", "optional-trigram-facts"),
    ]:
        require(search, fragment, label)

    alphabet = (97, 98, 0xE9, 0x100, 0xD800, 0xDFFF, 0xD83D, 0xDE00)
    fixed = [
        ((), ()),
        ((97,), (97,)),
        ((97, 98, 97, 98, 97, 98), (97, 98, 97)),
        ((0xD800, 0xDFFF, 0xD83D, 0xDE00), (0xD800, 0xDFFF)),
        (tuple(97 for _ in range(96)), tuple(97 for _ in range(8))),
        (tuple(97 if index % 3 else 98 for index in range(257)),
         (97, 98, 97, 98, 97, 98, 97, 98)),
    ]

    rng = random.Random(20261010)
    cases = 0
    queries = 0
    for source, pattern in fixed:
        queries += check_case(source, pattern, cases)
        cases += 1

    for _ in range(512):
        source = tuple(rng.choice(alphabet) for _ in range(rng.randint(0, 384)))
        if source and rng.random() < 0.65:
            begin = rng.randrange(len(source))
            end = rng.randrange(begin, len(source) + 1)
            pattern = source[begin:end]
        else:
            pattern = tuple(rng.choice(alphabet) for _ in range(rng.randint(0, 24)))
        queries += check_case(source, pattern, cases)
        cases += 1

    print(
        "M3_STRING_SEARCH_DIFFERENTIAL_PASS"
        f"|cases={cases}|queries={queries}|utf16=true|"
        "forward=true|reverse=true|kmp=true|bmh=true|source_contract=true"
    )


if __name__ == "__main__":
    main()
