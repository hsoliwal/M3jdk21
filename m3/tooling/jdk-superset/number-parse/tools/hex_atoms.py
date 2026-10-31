# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""hex_atoms.py <pre_src_dir> <post_src_dir> <out.tsv> [hex|dec]

HEX_ATOMS v1 / DEC_ATOMS v1 (CPU, read-only): the atoms of one literal path of java.base's number parser (PR #2215:
FastDoubleParser 1.0.90 absorbed as jdk.internal.math.M3*, wired into FloatingDecimal), each bound to its source range
and raw hash in the pre-image and the post-image. Atom set 'hex' (default): the hexadecimal path (HEX_ROUTE v1); atom
set 'dec': the decimal path (DEC_EXPONENT_CLAMP v1). A member's range runs from its declaration line to its closing
brace (Javadoc excluded); comments, strings and chars are masked before brace matching, so the range is exact. One row
per atom: atom role file member pre_lines pre_sha256 post_lines post_sha256 change. change = UNCHANGED | CHANGED.
The TSV starts with the Synexia SPDX header ('#' lines)."""
import hashlib
import os
import re
import sys

# (role, file, member name, regex the parameter list must match)
HEX_ATOMS = (
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
DEC_ATOMS = (
    ("ENTRY_DISPATCH", "FloatingDecimal.java", "parseDouble", r"String s"),
    ("ENTRY_DISPATCH", "FloatingDecimal.java", "parseFloat", r"String s"),
    ("GRAMMAR_DISPATCH", "M3AbstractJavaFloatingPointBitsFromCharSequence.java", "parseFloatingPointLiteral", r"CharSequence"),
    ("GRAMMAR_DISPATCH", "M3AbstractJavaFloatingPointBitsFromByteArray.java", "parseFloatingPointLiteral", r"byte\[\]"),
    ("DEC_GRAMMAR", "M3AbstractJavaFloatingPointBitsFromCharSequence.java", "parseDecFloatLiteral", r"CharSequence"),
    ("DEC_GRAMMAR", "M3AbstractJavaFloatingPointBitsFromByteArray.java", "parseDecFloatLiteral", r"byte\[\]"),
    ("DEC_SWAR", "M3FastDoubleSwar.java", "tryToParseFourDigits", r"byte\[\]"),
    ("DEC_VALUE", "M3JavaDoubleBitsFromCharSequence.java", "valueOfFloatLiteral", r"CharSequence"),
    ("DEC_VALUE", "M3JavaFloatBitsFromCharSequence.java", "valueOfFloatLiteral", r"CharSequence"),
    ("DEC_VALUE", "M3JavaDoubleBitsFromByteArray.java", "valueOfFloatLiteral", r"byte\[\]"),
    ("DEC_VALUE", "M3JavaFloatBitsFromByteArray.java", "valueOfFloatLiteral", r"byte\[\]"),
    ("DEC_KERNEL", "M3FastDoubleMath.java", "tryDecFloatToDoubleTruncated", r"long significand"),
    ("DEC_KERNEL", "M3FastFloatMath.java", "tryDecFloatToFloatTruncated", r"long significand"),
    ("DEC_FAST", "M3FastDoubleMath.java", "tryDecToDoubleWithFastAlgorithm", r"long significand"),
    ("DEC_FAST", "M3FastFloatMath.java", "tryDecToFloatWithFastAlgorithm", r"long significand"),
    ("JDK_ROUTE", "FloatingDecimal.java", "readJavaFormatString", r"String in"),
)
ATOM_SETS = {"hex": HEX_ATOMS, "dec": DEC_ATOMS}
DEFAULT_SET = "hex"
MASKED = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])+\'', re.S)
PACKAGE_DIR = os.path.join("jdk", "internal", "math")
JAVA_SUFFIX = ".java"
UNCHANGED, CHANGED = "UNCHANGED", "CHANGED"
TSV_HEADER = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n")
COLUMNS = ("atom", "role", "file", "member", "pre_lines", "pre_sha256", "post_lines", "post_sha256", "change")
REQUIRED_ARGS = 4  # program, pre, post, out
SET_ARG = 4


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


def bind(root, file, name, params):
    """(line range, sha256) of one member in one image."""
    with open(os.path.join(root, PACKAGE_DIR, file), encoding="utf-8") as f:
        first, last, digest = member(f.read(), name, params)
    return "%d-%d" % (first, last), digest


def main(argv):
    if len(argv) < REQUIRED_ARGS:
        raise SystemExit(__doc__)
    pre, post, out = argv[1:REQUIRED_ARGS]
    atoms = ATOM_SETS[argv[SET_ARG] if len(argv) > SET_ARG else DEFAULT_SET]
    rows = []
    for role, file, name, params in atoms:
        pre_lines, pre_sha = bind(pre, file, name, params)
        post_lines, post_sha = bind(post, file, name, params)
        rows.append({"atom": file[:-len(JAVA_SUFFIX)] + "#" + name + "(" + params.replace("\\", "") + ")",
                     "role": role, "file": file, "member": name, "pre_lines": pre_lines, "pre_sha256": pre_sha,
                     "post_lines": post_lines, "post_sha256": post_sha,
                     "change": UNCHANGED if pre_sha == post_sha else CHANGED})
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(TSV_HEADER)
        f.write("\t".join(COLUMNS) + "\n")
        f.write("".join("\t".join(r[c] for c in COLUMNS) + "\n" for r in rows))
    print("ATOMS\t%d\tCHANGED\t%s" % (len(rows), ",".join(r["atom"] for r in rows if r["change"] == CHANGED)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
