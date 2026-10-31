#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Exact-arithmetic referee for raw-bits differences: which side rounds each Java literal correctly?

  python verify_rounding.py <bits.tsv> [<bits.tsv> ...]

Reads the difference lists written by M3FdpDelegateDifferential (method, literal, upstream bits, overlay bits),
evaluates each literal exactly over the Java FloatingPointLiteral grammar (fractions.Fraction) and rounds it to
binary64 or binary32, round-half-even with subnormals and overflow. Prints one JSON summary per file: how many
differences each side rounds correctly, and the literal shapes involved. Exit 1 when the overlay (java.base) side
is wrong for any difference.
"""
from __future__ import annotations

from collections import Counter
from fractions import Fraction
import json
from pathlib import Path
import re
import struct
import sys

ESCAPE = re.compile(r"\\u([0-9a-f]{4})")
HEX = re.compile(r"([+-]?)0[xX]([0-9a-fA-F]*)(?:\.([0-9a-fA-F]*))?[pP]([+-]?[0-9]+)[fFdD]?\Z")
DECIMAL = re.compile(r"([+-]?)([0-9]*)(?:\.([0-9]*))?(?:[eE]([+-]?[0-9]+))?[fFdD]?\Z")
WHITESPACE_LIMIT = " "
HEX_DIGIT_BITS = 4
DECIMAL_BASE = 10
BINARY_BASE = 2
HALF = Fraction(1, 2)
EXPONENT_CLAMP = 2000
FORMATS = {"double": (53, -1022, 1023), "float": (24, -126, 127)}
EXIT_OVERLAY_WRONG = 1


def unescape(text: str) -> str:
    return ESCAPE.sub(lambda match: chr(int(match.group(1), 16)), text)


def exact(literal: str) -> tuple[int, Fraction | None, str]:
    """(sign, exact magnitude or None for out-of-range, shape) of a Java literal accepted by both sides."""
    text = literal.strip("".join(chr(c) for c in range(ord(WHITESPACE_LIMIT) + 1)))
    match = HEX.match(text)
    if match:
        whole, fraction, exponent = match.group(2), match.group(3) or "", int(match.group(4))
        digits = (whole + fraction).lstrip("0")
        scale = exponent - HEX_DIGIT_BITS * len(fraction)
        magnitude = int(whole + fraction, 16) if whole + fraction else 0
        lead = magnitude.bit_length() + scale
        shape = f"hex sig_hex_digits={len(digits)}"
        base, power = BINARY_BASE, scale
    else:
        match = DECIMAL.match(text)
        if not match:
            raise ValueError("not a Java literal: " + repr(literal))
        whole, fraction, exponent = match.group(2), match.group(3) or "", int(match.group(4) or 0)
        digits = (whole + fraction).lstrip("0")
        magnitude = int(whole + fraction) if whole + fraction else 0
        power = exponent - len(fraction)
        lead = len(str(magnitude)) + power
        shape = f"decimal sig_digits={len(digits)}"
        base = DECIMAL_BASE
    sign = -1 if match.group(1) == "-" else 1
    if magnitude == 0:
        return sign, Fraction(0), shape
    if abs(lead) > EXPONENT_CLAMP:
        return sign, None if lead > 0 else Fraction(0), shape
    return sign, Fraction(magnitude) * Fraction(base) ** power, shape


def round_to(value: Fraction | None, sign: int, kind: str) -> int:
    """Round-half-even to binary64/binary32; returns the raw bit pattern."""
    precision, emin, emax = FORMATS[kind]
    if value is None or value == 0:
        magnitude = float("inf") if value is None else 0.0
    else:
        exponent = value.numerator.bit_length() - value.denominator.bit_length()
        if Fraction(BINARY_BASE) ** exponent > value:
            exponent -= 1
        quantum = Fraction(BINARY_BASE) ** (max(exponent, emin) - (precision - 1))
        scaled = value / quantum
        integer = scaled.numerator // scaled.denominator
        rest = scaled - integer
        if rest > HALF or (rest == HALF and integer % 2):
            integer += 1
        rounded = integer * quantum
        limit = Fraction(BINARY_BASE) ** (emax + 1)
        magnitude = float("inf") if rounded >= limit else float(rounded)
    signed = -magnitude if sign < 0 else magnitude
    if kind == "double":
        return struct.unpack(">Q", struct.pack(">d", signed))[0]
    return struct.unpack(">I", struct.pack(">f", signed))[0]


def referee(path: Path) -> dict:
    tally, shapes, wrong = Counter(), Counter(), []
    for line in path.read_text(encoding="utf-8").splitlines():
        method, literal, upstream, overlay = line.split("\t")
        kind = "double" if "parseDouble" in method else "float"
        sign, value, shape = exact(unescape(literal))
        correct = round_to(value, sign, kind)
        verdict = ("both" if int(upstream, 16) == int(overlay, 16) == correct else
                   "overlay" if int(overlay, 16) == correct else
                   "upstream" if int(upstream, 16) == correct else "neither")
        tally[kind + ":" + verdict] += 1
        shapes[kind + ":" + shape.split()[0]] += 1
        if verdict != "overlay" and len(wrong) < 10:
            wrong.append([method, literal, upstream, overlay, hex(correct)])
    return {"file": path.name, "differences": sum(tally.values()), "correctly_rounded_by": dict(sorted(tally.items())),
            "shapes": dict(sorted(shapes.items())), "overlay_not_correct_examples": wrong}


def main(argv: list[str]) -> int:
    results = [referee(Path(name)) for name in argv[1:]]
    print(json.dumps(results, indent=1, sort_keys=True))
    overlay_wrong = any(key.split(":")[1] != "overlay" for r in results for key in r["correctly_rounded_by"])
    return EXIT_OVERLAY_WRONG if overlay_wrong else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
