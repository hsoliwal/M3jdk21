#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hex_defect_model.py - HEX_DEFECT_MODEL v1: the hexadecimal path of FastDoubleParser 1.0.90 (absorbed in PR #2215 as
M3AbstractJavaFloatingPointBitsFrom{CharSequence,ByteArray}.parseHexFloatLiteral + M3FastDoubleMath
.tryHexFloatToDoubleTruncated / M3FastFloatMath.tryHexFloatToFloatTruncated), replayed in exact integer arithmetic.

  model <bits.tsv>               replay every difference row (method, literal, fdp bits, jdk bits) of the jackson-fdp
                                 differential (M3FdpDelegateDifferential); exit 1 unless the model reproduces every fdp
                                 value and the exact referee (fractions.Fraction, round-half-even) every jdk value
  affected <bits.tsv> <out.tsv>  write the unique (kind, literal) pairs with the stock bits, the fdp bits and the defect:
                                 the corpus of M3NumberParseHexAtomsTest
  probe <literal> <kind>...      model one literal: the parse, the dropped bits and the verdict, as JSON
  corpus <literals.txt> <bits.tsv>
                                 completeness: model every hex literal of a corpus (one escaped literal per line, e.g.
                                 the 250,000 jackson-fdp literals) and compare the pairs it predicts to differ with the
                                 pairs the differential found; exit 1 on any predicted-only or found-only pair

Defects (atoms and lines of the pre-image bf16b786637):
  BIT63_DOUBLE_ROUNDING  M3FastDoubleMath:1000 / M3FastFloatMath:131 `(double) significand + (significand < 0 ? 2^64 : 0)`:
                         an unsigned significand >= 2^63 is first rounded as the signed value s - 2^64 (63-bit magnitude,
                         10 bits dropped for a double, 39 for a float), then once more after + 2^64 (11 / 40 bits): two
                         roundings; the first can land exactly on a halfway point that round-half-even then resolves the
                         wrong way
  TRUNCATED_NO_STICKY    parseHexFloatLiteral re-parse (digitCount > 16): the significand keeps 15 or 16 hex digits and
                         the dropped digits reach the kernel only as isSignificandTruncated, which selects the exponent
                         and nothing else: no sticky bit, so a kept halfway pattern with non-zero dropped digits rounds
                         to even instead of up
  EXPONENT_CLAMP         parseHexFloatLiteral exponent loop: the exponent number stops growing at MAX_EXPONENT_NUMBER
                         (1024) and the fraction-digit adjustment (-4 per digit) is added afterwards, so 0x.1p10240 lands
                         at 2^1020 instead of overflowing (the JDK: Infinity)
"""
from __future__ import annotations

from collections import Counter
from fractions import Fraction
import json
import re
import struct
import sys

ESCAPE = re.compile(r"\\u([0-9a-fA-F]{4})")
# FloatingDecimal.HexFloatPattern.VALUE (after String.trim()): the JDK's hex literal language
JDK_HEX = re.compile(r"[-+]?0[xX](?:[0-9a-fA-F]+\.?|[0-9a-fA-F]*\.[0-9a-fA-F]+)[pP][-+]?[0-9]+[fFdD]?")
WHITESPACE_LIMIT = 0x20
HEX_VALUES = {c: int(c, 16) for c in "0123456789abcdefABCDEF"}
DECIMAL_DIGITS = "0123456789"
BITS_PER_HEX_DIGIT = 4
WORD_BITS = 64
WORD_MASK = (1 << WORD_BITS) - 1
SIGN_BIT = 1 << (WORD_BITS - 1)
MAX_EXACT_HEX_DIGITS = 16
MINIMAL_NINETEEN_DIGIT_INTEGER = 10 ** 18
MAX_EXPONENT_NUMBER = 1024
DECIMAL_RADIX = 10
# |binary exponent| beyond which a value overflows or underflows both formats (no 2^huge fractions)
EXPONENT_CLAMP = 2000
# kind: (precision incl. hidden bit, min normal exponent, max exponent)
FORMATS = {"double": (53, -1022, 1023), "float": (24, -126, 127)}
EXIT_MISMATCH = 1
EXIT_USAGE = 2
HEX_RADIX = 16
HEX_PREFIX_LENGTH = 2  # "0x"
BINARY_BASE = 2
HALF = Fraction(1, 2)
PARITY_MODULUS = 2
FORMAT_EMAX = 2  # index of the max exponent in a FORMATS tuple
SEEN_DEFECT = 2  # index of the defect in a seen tuple (jdk, fdp, defect)
BITS_HEX_WIDTH = {"double": 16, "float": 8}  # hex digits of a format's raw bits
MAX_REPORTED = 10
ARG_MODE = 1  # argv: program, mode, then its arguments
ARG_FIRST = 2
ARG_SECOND = 3
ARGC_ONE_ARG = 3  # argv length of a mode with one argument
ARGC_TWO_ARGS = 4
TSV_HEADER = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n")


def unescape(text: str) -> str:
    return ESCAPE.sub(lambda match: chr(int(match.group(1), HEX_RADIX)), text)


def strip(text: str) -> str:
    return text.strip("".join(chr(c) for c in range(WHITESPACE_LIMIT + 1)))


def parse(literal: str) -> dict:
    """The fields parseHexFloatLiteral computes, plus the exact (unclamped, untruncated) value."""
    text = strip(literal)
    i, negative = 0, False
    if text[i] in "+-":
        negative, i = text[i] == "-", i + 1
    if text[i] != "0" or text[i + 1] not in "xX":
        raise ValueError("not a hex literal: " + repr(literal))
    i += HEX_PREFIX_LENGTH
    start, significand, point = i, 0, -1
    while i < len(text) and (text[i] in HEX_VALUES or text[i] == "."):
        if text[i] == ".":
            point = i
        else:
            significand = ((significand << BITS_PER_HEX_DIGIT) | HEX_VALUES[text[i]]) & WORD_MASK
        i += 1
    end = i
    whole_and_fraction = text[start:end].replace(".", "")
    fraction_digits = 0 if point < 0 else end - point - 1
    if point < 0:
        digit_count, point, exponent = end - start, end, 0
    else:
        digit_count = end - start - 1
        exponent = min(point - end + 1, MAX_EXPONENT_NUMBER) * BITS_PER_HEX_DIGIT
    if text[i] not in "pP":
        raise ValueError("no binary exponent: " + repr(literal))
    i += 1
    exponent_negative = text[i] == "-"
    if text[i] in "+-":
        i += 1
    number, clamped, true_number = 0, False, 0
    while i < len(text) and text[i] in DECIMAL_DIGITS:
        digit = int(text[i])
        true_number = DECIMAL_RADIX * true_number + digit
        if number < MAX_EXPONENT_NUMBER:
            number = DECIMAL_RADIX * number + digit
        else:
            clamped = True
        i += 1
    if exponent_negative:
        number, true_number = -number, -true_number
    exponent += number
    truncated, truncated_exponent = False, 0
    if digit_count > MAX_EXACT_HEX_DIGITS:
        significand, skipped, j = 0, 0, start
        while j < end:
            if text[j] == ".":
                skipped += 1
            elif significand < MINIMAL_NINETEEN_DIGIT_INTEGER:
                significand = (significand << BITS_PER_HEX_DIGIT) | HEX_VALUES[text[j]]
            else:
                break
            j += 1
        truncated = j < end
        truncated_exponent = (point - j + skipped) * BITS_PER_HEX_DIGIT + number
    magnitude = int(whole_and_fraction, HEX_RADIX) if whole_and_fraction else 0
    return {"negative": negative, "significand": significand, "digit_count": digit_count,
            "significant_digits": len(whole_and_fraction.lstrip("0")), "exponent": exponent,
            "truncated": truncated, "truncated_exponent": truncated_exponent, "clamped": clamped,
            "exact": exact(magnitude, true_number - BITS_PER_HEX_DIGIT * fraction_digits)}


def exact(magnitude: int, power: int) -> Fraction | None:
    """magnitude * 2^power; None when it overflows every format, 0 when it underflows every format (no huge powers)."""
    if magnitude == 0:
        return Fraction(0)
    lead = magnitude.bit_length() + power
    if lead > EXPONENT_CLAMP:
        return None
    return Fraction(0) if lead < -EXPONENT_CLAMP else Fraction(magnitude) * Fraction(BINARY_BASE) ** power


def round_int(value: int, precision: int) -> int:
    """value rounded to `precision` significant bits, round-half-even (IEEE 754 roundTiesToEven, unbounded exponent)."""
    magnitude = abs(value)
    shift = magnitude.bit_length() - precision
    if shift <= 0:
        return value
    kept, rest = divmod(magnitude, 1 << shift)
    half = 1 << (shift - 1)
    if rest > half or (rest == half and kept & 1):
        kept += 1
    return (kept << shift) * (-1 if value < 0 else 1)


def correct(value: Fraction | None, kind: str) -> Fraction | None:
    """Round-half-even to the format with subnormals; None = overflow (infinity)."""
    precision, emin, emax = FORMATS[kind]
    if value is None or value == 0:
        return value
    exponent = value.numerator.bit_length() - value.denominator.bit_length()
    if Fraction(BINARY_BASE) ** exponent > value:
        exponent -= 1
    quantum = Fraction(BINARY_BASE) ** (max(exponent, emin) - (precision - 1))
    scaled = value / quantum
    kept = scaled.numerator // scaled.denominator
    rest = scaled - kept
    if rest > HALF or (rest == HALF and kept % PARITY_MODULUS):
        kept += 1
    rounded = kept * quantum
    return None if rounded >= Fraction(BINARY_BASE) ** (emax + 1) else rounded


def bits(value: Fraction | None, negative: bool, kind: str) -> int:
    magnitude = float("inf") if value is None else float(value)
    signed = -magnitude if negative else magnitude
    if kind == "double":
        return struct.unpack(">Q", struct.pack(">d", signed))[0]
    return struct.unpack(">I", struct.pack(">f", signed))[0]


def fdp_fast(fields: dict, kind: str) -> tuple[Fraction | None, bool] | None:
    """tryHex*Truncated of the pre-image: None = NaN (the caller falls back to the JDK); else (value, double-rounded)."""
    precision, emin, emax = FORMATS[kind]
    power = fields["truncated_exponent"] if fields["truncated"] else fields["exponent"]
    if not emin <= power <= emax:
        return None
    s = fields["significand"]
    twice = s & SIGN_BIT != 0
    cast = round_int(round_int(s - (1 << WORD_BITS), precision) + (1 << WORD_BITS), precision) if twice \
        else round_int(s, precision)
    value = Fraction(cast) * Fraction(BINARY_BASE) ** power
    return (None if value >= Fraction(BINARY_BASE) ** (emax + 1) else value), twice


def defect(fields: dict, kind: str) -> str:
    """Which defect makes the fast value differ from the correctly rounded one."""
    if fields["clamped"]:
        return "EXPONENT_CLAMP"
    precision = FORMATS[kind][0]
    s = fields["significand"]
    if s & SIGN_BIT == 0:
        return "TRUNCATED_NO_STICKY" if fields["truncated"] else "UNEXPLAINED"
    if not fields["truncated"]:
        return "BIT63_DOUBLE_ROUNDING"
    power, emax = fields["truncated_exponent"], FORMATS[kind][FORMAT_EMAX]
    single = Fraction(round_int(s, precision)) * Fraction(BINARY_BASE) ** power
    if single >= Fraction(BINARY_BASE) ** (emax + 1):
        single = None
    return "TRUNCATED_NO_STICKY" if single != correct(fields["exact"], kind) else "BIT63_DOUBLE_ROUNDING"


def replay(method: str, literal: str) -> dict:
    kind = "double" if "Double" in method else "float"
    fields = parse(unescape(literal))
    fast = fdp_fast(fields, kind)
    expected = bits(correct(fields["exact"], kind), fields["negative"], kind)
    model = expected if fast is None else bits(fast[0], fields["negative"], kind)
    return {"kind": kind, "fields": fields, "model": model, "correct": expected,
            "defect": defect(fields, kind) if model != expected else "NONE"}


def rows(path: str):
    for line in open(path, encoding="utf-8"):
        method, literal, fdp, jdk = line.rstrip("\n").split("\t")
        yield method, literal, int(fdp, HEX_RADIX), int(jdk, HEX_RADIX)


def model(path: str) -> int:
    tally, mismatches = Counter(), []
    for method, literal, fdp, jdk in rows(path):
        r = replay(method, literal)
        tally["rows"] += 1
        tally["model_reproduces_fdp"] += r["model"] == fdp
        tally["referee_equals_jdk"] += r["correct"] == jdk
        tally[r["kind"] + ":" + r["defect"]] += 1
        f = r["fields"]
        tally[r["kind"] + ":significant_hex_digits>=16"] += f["significant_digits"] >= MAX_EXACT_HEX_DIGITS
        if (r["model"] != fdp or r["correct"] != jdk) and len(mismatches) < MAX_REPORTED:
            mismatches.append([method, literal, hex(fdp), hex(jdk), hex(r["model"]), hex(r["correct"])])
    print(json.dumps({"file": path, "tally": dict(sorted(tally.items())), "mismatches": mismatches}, indent=1))
    ok = tally["model_reproduces_fdp"] == tally["rows"] == tally["referee_equals_jdk"]
    return 0 if ok else EXIT_MISMATCH


def affected(path: str, out: str) -> int:
    seen = {}
    for method, literal, fdp, jdk in rows(path):
        r = replay(method, literal)
        key = (r["kind"], literal)
        if key not in seen:
            seen[key] = (jdk, fdp, r["defect"])
    width = BITS_HEX_WIDTH
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write(TSV_HEADER)
        w.write("# M3 number-parse hex atoms: every (kind, literal) pair that FastDoubleParser 1.0.90 rounds differently from\n"
                "# stock JDK 21.0.9 in the jackson-fdp differential (250,000 literals, corpus sha256 81ba24f2..., seed 20261031;\n"
                "# difference list fdp-differential-stock.bits.tsv). Literal: \\uXXXX escapes for chars outside 0x20..0x7e and\n"
                "# the backslash. stock_bits: stock JDK 21.0.9 (equal to the exact round-half-even referee); fdp_bits: the\n"
                "# pre-fix value; defect: tools/hex_defect_model.py. Generated by tools/hex_defect_model.py affected.\n")
        w.write("kind\tliteral\tstock_bits\tfdp_bits\tdefect\n")
        for (kind, literal), (jdk, fdp, why) in sorted(seen.items()):
            w.write("%s\t%s\t%0*x\t%0*x\t%s\n" % (kind, literal, width[kind], jdk, width[kind], fdp, why))
    print(json.dumps({"pairs": len(seen), "by": dict(Counter(k + ":" + v[SEEN_DEFECT] for (k, _), v in seen.items()))}))
    return 0


def probe(literal: str, kinds: list[str]) -> int:
    for kind in kinds:
        r = replay("Double" if kind == "double" else "Float", literal)
        f = r["fields"]
        precision = FORMATS[kind][0]
        s = f["significand"]
        detail = {"literal": literal, "kind": kind, "significand": hex(s), "digit_count": f["digit_count"],
                  "truncated": f["truncated"], "exponent": f["exponent"], "clamped": f["clamped"],
                  "model_bits": hex(r["model"]), "correct_bits": hex(r["correct"]), "defect": r["defect"]}
        if s & SIGN_BIT:
            first = round_int(s - (1 << WORD_BITS), precision) + (1 << WORD_BITS)
            detail.update({"two_pow_64_minus_s": hex((1 << WORD_BITS) - s), "after_signed_cast": hex(first),
                           "after_plus_two_pow_64": hex(round_int(first, precision)),
                           "single_rounding": hex(round_int(s, precision))})
        print(json.dumps(detail))
    return 0


def corpus(literals: str, path: str) -> int:
    found = {("double" if "Double" in method else "float", literal) for method, literal, _, _ in rows(path)}
    predicted, tally = set(), Counter()
    for line in open(literals, encoding="utf-8"):
        literal = line.rstrip("\n")
        if not JDK_HEX.fullmatch(strip(unescape(literal))):
            continue
        tally["hex_literals"] += 1
        for kind in FORMATS:
            r = replay(kind.capitalize(), literal)
            tally[kind + ":" + r["defect"]] += 1
            if r["defect"] != "NONE":
                predicted.add((kind, literal))
    tally.update({"predicted": len(predicted), "found": len(found), "predicted_only": len(predicted - found),
                  "found_only": len(found - predicted)})
    print(json.dumps({"literals": literals, "tally": dict(sorted(tally.items())),
                      "predicted_only": sorted(predicted - found)[:MAX_REPORTED],
                      "found_only": sorted(found - predicted)[:MAX_REPORTED]},
                     indent=1))
    return 0 if predicted == found else EXIT_MISMATCH


def main(argv: list[str]) -> int:
    mode = argv[ARG_MODE] if len(argv) > ARG_MODE else ""
    if len(argv) >= ARGC_ONE_ARG and mode == "model":
        return model(argv[ARG_FIRST])
    if len(argv) == ARGC_TWO_ARGS and mode == "corpus":
        return corpus(argv[ARG_FIRST], argv[ARG_SECOND])
    if len(argv) == ARGC_TWO_ARGS and mode == "affected":
        return affected(argv[ARG_FIRST], argv[ARG_SECOND])
    if len(argv) >= ARGC_TWO_ARGS and mode == "probe":
        return probe(argv[ARG_FIRST], argv[ARG_SECOND:])
    sys.stderr.write(__doc__)
    return EXIT_USAGE


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
