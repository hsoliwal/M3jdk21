# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_patterns.py <JIT_SIZE.tsv> <JIT.tsv> <PATTERN_MATRIX.tsv> <out PATTERNS.tsv>

NUMBER-PARSE-PATTERN v1, atom level (CPU, read-only). Every method and constructor of the 13 M3 types (jit_size.py
table of the post-image) and the FloatingDecimal wiring gets one row:
  pattern_version type atom bytes source_lines jit_class tier type_role grammar target carrier atom_pattern residue
  owner test
type_role/grammar/target/carrier come from the type-level matrix (PATTERN_MATRIX.tsv; the wiring is WIRING).
atom_pattern is the first matching rule of the closed set below; an atom no rule matches is UNKNOWN_PATTERN (the
script exits 1). residue is typed: NONE, or ';'-joined tokens
  PACKED_RESULT        several results in one long (no allocation); fields decoded by packedIndex / (int) / sign
  INSTANCE_ATOM        not static: the atom ends in the abstract valueOf* of its instance
  COLD_PATH            the slow path; large or rarely run by design
  FIXED:<recipe>       behaviour corrected toward the JDK in this leaf
  DONOR_QUIRK:<what>   donor behaviour kept and pinned by its test
  LATENT_DEFECT:<what> donor defect no caller reaches (FFT_LATENT v1 in RECIPE_MEMORY.tsv)
owner is the atom that owns the behaviour: itself, or the single owner when JUnit proves atoms identical.
test is the dynamic JUnit test of the atom in M3NumberParseEveryAtomTest (its key)."""
import re
import sys

VERSION = "NUMBER-PARSE-PATTERN v1"
RULES = (  # (atom_pattern, regex over "Type.member(params)")
    ("CONSTRUCTOR", r"\.<init>\("),
    ("ENTRY", r"(\.parseFloatingPointLiteral\(|^FloatingDecimal\.parse(Double|Float)\()"),
    ("SIGNIFICAND_SCAN", r"\.parse(Dec|Hex)Float(ing)?(Point)?Literal\("),
    ("EXPONENT_SCAN", r"\.parseExponentNumber\("),
    ("LITERAL_TAIL", r"\.parseLiteralTail\("),
    ("PACKED_FIELD", r"\.packedIndex\("),
    ("WHITESPACE_SKIP", r"\.skipWhitespace\("),
    ("SPECIAL_VALUE", r"\.(parseNaNOrInfinity|nan|negativeInfinity|positiveInfinity)\("),
    ("TRUNCATED_REPARSE", r"\.valueOfTruncated"),
    ("VALUE_DISPATCH", r"\.valueOf(Float|Hex)Literal\("),
    ("SLOW_PATH_FALLBACK", r"\.jdkConverterValue\("),
    ("FAST_PATH_DECISION", r"\.try(DecFloatTo|DecTo|HexFloatTo)(Double|Float)(Truncated|WithFastAlgorithm)\("),
    ("EXACT_FAST_PATH", r"\.(is)?[cC]lingerFastPath\("),
    ("EISEL_LEMIRE", r"WithEiselLemire\("),
    ("UNSIGNED_CONVERSION", r"\.unsignedTo(Double|Float)\("),
    ("SCALB", r"\.fastScalb\("),
    ("WIDE_MULTIPLY", r"\.unsignedMultiplyHigh\("),
    ("FMA_PASSTHROUGH", r"M3FastDoubleSwar\.fma\("),
    ("SWAR_CLASSIFY", r"M3FastDoubleSwar\.(isDigit|isEightDigits|isEightZeroes)"),
    ("SWAR_PARSE", r"M3FastDoubleSwar\.tryToParse"),
    ("BYTE_VIEW", r"M3FastDoubleSwar\.(read|write)(Int|Long)(BE|LE)\("),
    ("CHAR_ACCESS", r"M3AbstractNumberParser\.charAt\("),
    ("CHAR_CLASS", r"M3AbstractNumberParser\.lookupHex\("),
    ("BOUNDS_CHECK", r"M3AbstractNumberParser\.checkBounds\("),
    ("POWER_TABLE", r"M3FastIntegerMath\.(computePowerOfTen|computeTenRaisedByNFloor16Recursive|createPowersOfTenFloor16Map|fillPowersOf10Floor16|fillPowersOfNFloor16Recursive|splitFloor16|estimateNumBits)\("),
    ("FFT_TRANSFORM", r"M3FftMultiplier\.(fft|ifft|fft3|fftMixedRadix|ifftMixedRadix)\("),
    ("FFT_ROOTS", r"M3FftMultiplier\.(calculateRootsOfUnity|getRootsOfUnity2|getRootsOfUnity3)\("),
    ("FFT_PACKING", r"M3FftMultiplier\.(toFftVector|fromFftVector|bitsPerFftPoint)\("),
    ("BIG_MULTIPLY", r"M3FftMultiplier\.(multiply|multiplyFft|square|squareFft)\("),
    ("COMPLEX_ARITHMETIC", r"M3FftMultiplier\$(ComplexVector|MutableComplex)\."),
)
RESIDUE = (  # (regex, token)
    (r"\.(parseExponentNumber|parseLiteralTail|packedIndex)\(", "PACKED_RESULT"),
    (r"\.valueOfTruncated", "INSTANCE_ATOM"),
    (r"\.(jdkConverterValue|parseNaNOrInfinity)\(", "COLD_PATH"),
    (r"\.(tryHexFloatTo(Double|Float)Truncated|unsignedTo(Double|Float))\(", "FIXED:HEX_ROUNDING v1"),
    (r"M3FastDoubleSwar\.(tryToParseEightHexDigits|tryToParseEightHexDigitsUtf16|tryToParseEightHexDigitsUtf8)\(", "FIXED:SWAR_HEX_BORROW v1"),
    (r"\.valueOfHexLiteral\(", "FIXED:HEX_ROUNDING v1"),
    (r"\.fillPowersOf10Floor16\(", "DONOR_QUIRK:key 0 holds 5"),
    (r"ComplexVector\.part\(", "DONOR_QUIRK:ignores the view offset"),
    (r"ComplexVector\.(applyWeights|applyInverseWeights|multiplyPointwise|squarePointwise)\(", "LATENT_DEFECT:overruns on a view"),
    (r"M3FftMultiplier\.(calculateRootsOfUnity|getRootsOfUnity3)\(", "LATENT_DEFECT:odd length 3 roots"),
    (r"M3FftMultiplier\.(fftMixedRadix|ifftMixedRadix)\(", "LATENT_DEFECT:length 12 wrong"),
    (r"M3FftMultiplier\.multiplyFft\(", "LATENT_DEFECT:17-23 byte operands wrong"),
    (r"M3FftMultiplier\.squareFft\(", "LATENT_DEFECT:8-9 bytes throw, 17-23 wrong"),
    (r"M3FastDoubleSwar\.fma\(", "DONOR_QUIRK:pass-through to Math.fma"),
)
OWNER = {  # atoms JUnit proves identical in behaviour -> the single owner
    "M3FastDoubleSwar.fma(double,double,double)": "java.lang.Math.fma(double,double,double)",
}
PARAM = re.compile(r"(?:[\w$]+\.)*([\w$]+)(?:<[^()]*>)?(\[\])*")


def key(cls, sig):
    """The test key Type.member(params) of a javap signature."""
    if sig == "static {};":
        return None
    head, params = sig.split("(", 1)
    name = head.split()[-1].rsplit(".", 1)[-1]
    name = "<init>" if name == cls else name
    plist = []
    depth, cur = 0, ""
    for ch in params.rsplit(")", 1)[0]:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            plist.append(cur.strip())
            cur = ""
        else:
            cur += ch
    if cur.strip():
        plist.append(cur.strip())
    simple = []
    for p in plist:
        m = PARAM.fullmatch(p.replace(" ", ""))
        base = m.group(1).rsplit("$", 1)[-1] if m else p
        simple.append(base + "[]" * p.count("[]"))
    return "%s.%s(%s)" % (cls, name, ",".join(simple))


def main(argv):
    size_path, jit_path, matrix_path, out_path = argv[1:5]
    matrix = {}
    for n, ln in enumerate(open(matrix_path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n:
            matrix["M3" + f[0]] = f[1:5]
    tiers = {}
    for n, ln in enumerate(open(jit_path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n:
            tiers[f[0]] = f[4]
    rows, unknown = [], []
    for n, ln in enumerate(open(size_path, encoding="utf-8")):
        f = ln.rstrip("\n").split("\t")
        if n == 0 or not (f[0].startswith("M3") or f[0] == "FloatingDecimal"):
            continue
        k = key(f[0], f[1])
        if k is None or (f[0] == "FloatingDecimal" and not re.search(r"\.parse(Double|Float)\(String\)$", k)):
            continue
        top = f[0].split("$")[0]
        axes = matrix.get(top, ["WIRING" if top == "FloatingDecimal" else "-", "-", "-", "-"])
        pattern = next((p for p, rx in RULES if re.search(rx, k)), "UNKNOWN_PATTERN")
        if pattern == "UNKNOWN_PATTERN":
            unknown.append(k)
        residue = ";".join(t for rx, t in RESIDUE if re.search(rx, k)) or "NONE"
        member = k.split(".", 1)[1].split("(", 1)[0]
        tier = tiers.get("%s::%s" % (f[0], member), "-")
        rows.append((VERSION, f[0], k, f[3], f[6], f[8], tier, *axes, pattern, residue, OWNER.get(k, k), k))
    with open(out_path, "w", encoding="utf-8", newline="\n") as w:
        w.write("pattern_version\ttype\tatom\tbytes\tsource_lines\tjit_class\ttier\ttype_role\tgrammar\ttarget\tcarrier"
                "\tatom_pattern\tresidue\towner\ttest\n")
        for r in rows:
            w.write("\t".join(r) + "\n")
    counts = {}
    for r in rows:
        counts[r[11]] = counts.get(r[11], 0) + 1
    sys.stdout.write("ATOMS\t%d\tPATTERNS\t%d\tUNKNOWN\t%d\t%s\n" % (len(rows), len(counts), len(unknown),
                                                                   " ".join(unknown)))
    return 0 if not unknown else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
