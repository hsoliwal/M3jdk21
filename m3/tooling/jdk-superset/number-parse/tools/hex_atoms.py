# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hex_atoms.py <pre_src_dir> <post_src_dir> <out.tsv>

HEX_ATOMS v1 (CPU, read-only): the atoms of the hexadecimal literal path of java.base's number parser (PR #2215:
FastDoubleParser 1.0.90 absorbed as jdk.internal.math.M3*, wired into FloatingDecimal), each bound to its source range
and raw hash in the pre-image and the post-image. A member's range runs from its declaration line to its closing brace
(Javadoc excluded); comments, strings and chars are masked before brace matching, so the range is exact. One row per
atom: atom role file member pre_lines pre_sha256 post_lines post_sha256 change. change = UNCHANGED | CHANGED."""
import hashlib
import os
import re
import sys

# (role, file, member name, regex the parameter list must match)
ATOMS = (
    ("ENTRY_DISPATCH", "FloatingDecimal.java", "parseDouble", r"String s"),
    ("ENTRY_DISPATCH", "FloatingDecimal.java", "parseFloat", r"String s"),
    ("GRAMMAR_DISPATCH", "M3AbstractJavaFloatingPointBitsFromCharSequence.java", "parseFloatingPointLiteral", r"CharSequence"),
    ("GRAMMAR_DISPATCH", "M3AbstractJavaFloatingPointBitsFromByteArray.java", "parseFloatingPointLiteral", r"byte\[\]"),
    ("HEX_GRAMMAR", "M3AbstractJavaFloatingPointBitsFromCharSequence.java", "parseHexFloatLiteral", r"CharSequence"),
    ("HEX_GRAMMAR", "M3AbstractJavaFloatingPointBitsFromByteArray.java", "parseHexFloatingPointLiteral", r"byte\[\]"),
    ("HEX_DIGIT", "M3AbstractNumberParser.java", "lookupHex", r"char ch"),
    ("HEX_DIGIT", "M3AbstractNumberParser.java", "lookupHex", r"byte ch"),
    ("HEX_SWAR", "M3FastDoubleSwar.java", "tryToParseEightHexDigits", r"CharSequence"),
    ("HEX_SWAR", "M3FastDoubleSwar.java", "tryToParseEightHexDigits", r"byte\[\]"),
    ("HEX_VALUE", "M3JavaDoubleBitsFromCharSequence.java", "valueOfHexLiteral", r"CharSequence"),
    ("HEX_VALUE", "M3JavaFloatBitsFromCharSequence.java", "valueOfHexLiteral", r"CharSequence"),
    ("HEX_VALUE", "M3JavaDoubleBitsFromByteArray.java", "valueOfHexLiteral", r"byte\[\]"),
    ("HEX_VALUE", "M3JavaFloatBitsFromByteArray.java", "valueOfHexLiteral", r"byte\[\]"),
    ("HEX_KERNEL", "M3FastDoubleMath.java", "tryHexFloatToDoubleTruncated", r"long significand"),
    ("HEX_KERNEL", "M3FastFloatMath.java", "tryHexFloatToFloatTruncated", r"long significand"),
    ("HEX_SCALE", "M3FastDoubleMath.java", "fastScalb", r"double number"),
    ("HEX_SCALE", "M3FastFloatMath.java", "fastScalb", r"float number"),
    ("JDK_ROUTE", "FloatingDecimal.java", "readJavaFormatString", r"String in"),
    ("JDK_ROUTE", "FloatingDecimal.java", "parseHexString", r"String s"),
)
MASKED = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])+\'', re.S)
PACKAGE_DIR = os.path.join("jdk", "internal", "math")


def mask(src):
    """Comments, strings and chars blanked (newlines kept), so offsets and line numbers stay valid."""
    return MASKED.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), src)


def member(src, name, params):
    masked = mask(src)
    head = re.compile(r"(?m)^[ \t]*(?:[\w<>\[\],.?]+[ \t]+)+" + re.escape(name) + r"\s*\(([^)]*)\)[^;{]*\{")
    for m in head.finditer(masked):
        if not re.search(params, m.group(1)):
            continue
        depth, i = 0, m.end() - 1
        while True:
            depth += {"{": 1, "}": -1}.get(masked[i], 0)
            if depth == 0:
                break
            i += 1
        start = m.start() + len(masked[m.start():m.end()]) - len(masked[m.start():m.end()].lstrip())
        text = src[start:i + 1]
        first = src.count("\n", 0, start) + 1
        return first, first + text.count("\n"), hashlib.sha256(text.encode("utf-8")).hexdigest()
    raise SystemExit("member not found: %s(%s)" % (name, params))


def main(argv):
    pre, post, out = argv[1:4]
    rows = []
    for role, file, name, params in ATOMS:
        cells = []
        for root in (pre, post):
            with open(os.path.join(root, PACKAGE_DIR, file), encoding="utf-8") as f:
                first, last, digest = member(f.read(), name, params)
            cells += ["%d-%d" % (first, last), digest]
        change = "UNCHANGED" if cells[1] == cells[3] else "CHANGED"
        rows.append((file[:-len(".java")] + "#" + name + "(" + params.replace("\\", "") + ")", role, file, name,
                     cells[0], cells[1], cells[2], cells[3], change))
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write("atom\trole\tfile\tmember\tpre_lines\tpre_sha256\tpost_lines\tpost_sha256\tchange\n")
        f.write("".join("\t".join(r) + "\n" for r in rows))
    print("ATOMS\t%d\tCHANGED\t%s" % (len(rows), ",".join(r[0] for r in rows if r[8] == "CHANGED")))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
