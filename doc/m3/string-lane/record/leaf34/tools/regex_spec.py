# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""regex_spec.py <out-tsv>

ESCAPE_MARKUP_REGEX_SPEC v1 (2026-11-17). Writes REGEX_SPEC.tsv: one row per regular expression the ESCAPE_MARKUP
draft uses anywhere (product code: none; proof and tools: the donor-test porter and pruner it runs, and the CPython
html.unescape rule its oracle runs and String.unescapeHtml5Python implements without a regex). Before writing, each
row's examples are checked with the regex engine the source uses (Python 3.12 re, same flags, same call): every
positive example must match, every negative one must not. Example cells separate examples with ' ;; ' and write TAB,
LF and backslash as \\t, \\n and \\\\. Exit 1 when an example disagrees.
"""
import re
import sys

PORTER = "J:/m3-scratch/donor-tests-20261117/port_donor_tests.py"
PRUNER = "J:/m3-scratch/donor-tests-20261117/prune_uncompilable.py"
SYN = "Apache-2.0 OR EPL-2.0 (Synexia)"
B = chr(92)
NL = chr(10)
# id, pattern, flags, call, source, licence, positives, negatives
ROWS = [
    ("R01", r"&(#[0-9]+;?|#[xX][0-9a-fA-F]+;?|[^\t\n\f <&#;]{1,32};?)", 0, "search",
     "CPython 3.12.6 Lib/html/__init__.py _charref (donor rule of html.unescape; the oracle runs it; "
     "String.unescapeHtml5Python implements it without a regex)", "PSF-2.0",
     ["&amp;", "&#65", "&#x41;", "&notit", "a&co;b"], ["& amp;", "&#;", "&#x;", "&;", "&<"]),
    ("R02", r"^\(\s*[\w.]+\s*\[\]\s*\)\s*null$", 0, "match", PORTER + ":79 NULL_ARRAY", SYN,
     ["(String[]) null", "(char []) null"], ["null", "(String) null"]),
    ("R03", r"\bStringUtils\.(\w+)\(", 0, "search", PORTER + ":85 and :298 (op call)", SYN,
     ["StringUtils.escapeHtml4(x)", "a(StringUtils.unescapeXml(\"&amp;\"))"],
     ["StringUtils.ESCAPE_HTML4.translate(x)", "MyStringUtils.x("]),
    ("R04", r"^\(\s*(String|CharSequence)\s*\)\s*null$", 0, "match", PORTER + ":102 (null receiver)", SYN,
     ["(String) null", "(CharSequence)null"], ["(Object) null", "null"]),
    ("R05", r"^\(\s*[\w.<>\[\] ]+\s*\)\s*null$", 0, "match", PORTER + ":110 (cast null argument)", SYN,
     ["(char[]) null", "(List<String>) null"], ["null", "(x) nul"]),
    ("R06", r"\s*assert\w+\((.*)\)\s*$", re.S, "match", PORTER + ":124 (assertion wrapper)", SYN,
     ["assertEquals(a, b)", " assertTrue(x) "], ["foo(a)", "assertEquals(a"]),
    ("R07", r"\b(String\.|asString\()", 0, "search", PORTER + ":128 (NPE target)", SYN,
     ["asString(\"a\").x()", "String.join(a)"], ["MyString.x", "toString()"]),
    ("R08", r"@Test\b[^{;]*?void\s+(\w+)\s*\([^)]*\)\s*(?:throws[^{]*)?\{", 0, "search",
     PORTER + ":160 (@Test method head)", SYN,
     ["@Test" + NL + "    void testA() {", "@Test void t() throws Exception {"], ["@Test;", "void t() {"]),
    ("R09", r"^\s*static\s*\{", re.M, "search", PORTER + ":186 (static block)", SYN,
     ["    static {", "x" + NL + "static{"], ["static final int X;", "  nonstatic {"]),
    ("R10", r"^\s*(?:private |protected |public )?static final (?:String|char|int|char\[\]|String\[\]) "
            r"(\w+)( = [^;]+)?;", re.M, "search", PORTER + ":200 (donor constant)", SYN,
     ["    private static final String FOO = \"foo\";", "static final int N;"],
     ["private final String FOO = \"x\";", "static final long N = 1;"]),
    ("R11", r"^(private |protected |public )?static final", 0, "search", PORTER + ":204", SYN,
     ["private static final String A", "static final int B"], ["final static int C", " static final"]),
    ("R12", r"^\s*(private |protected |public )?", 0, "search", PORTER + ":238 and :455 (modifier strip)", SYN,
     ["private x", "y"], []),
    ("R13", r"^StringUtils|Test$", 0, "search", PORTER + ":242 (donor short name)", SYN,
     ["StringUtilsTest", "CaseTest"], ["MyUtils", "Tests"]),
    ("R14", r"^(?:final\s+)?(String|char|int|char\[\]|String\[\])\s+(\w+)\s*=", 0, "match",
     PORTER + ":287 LOCAL_DECL", SYN, ["final String message = \"a\"", "char[] d = {}"], ["String s;", "long n = 1"]),
    ("R15", r"\bassert(IllegalArgument|NullPointer)Exception\(", 0, "search", PORTER + ":302", SYN,
     ["assertNullPointerException(() -> x)", "assertIllegalArgumentException("],
     ["assertThrows(", "assertNullPointer("]),
    ("R16", r"\bStringUtils\.(EMPTY|SPACE|LF|CR)\b", 0, "search", PORTER + ":304", SYN,
     ["StringUtils.EMPTY", "f(StringUtils.LF)"], ["StringUtils.EMPTY_ARRAY", "StringUtils.CRLF"]),
    ("R17", r"\bSuppliers\.nul\(\)", 0, "search", PORTER + ":306", SYN, ["Suppliers.nul()"], ["Suppliers.null()"]),
    ("R18", r"\b\w+Test\.(\w+)\b", 0, "search", PORTER + ":307 (donor-class constant)", SYN,
     ["StringUtilsTest.FOO", "ATest.x"], ["Test.x", "Tests.x"]),
    ("R19", r"(?<![.\w])[A-Z][A-Z0-9_]+\b", 0, "search", PORTER + ":311 (constant names)", SYN,
     ["FOO", "x + BAR_2"], ["Foo", "a.FOO", "A"]),
    ("R20", r"(?<![.\w])([A-Za-z_]\w*)\b", 0, "search", PORTER + ":313 (identifiers)", SYN,
     ["abc", "x.y"], ["123", ".abc"]),
    ("R21", r"(?<![.\w])([a-z]\w*)\b(?!\s*\()", 0, "search", PORTER + ":316 (lower-case names)", SYN,
     ["message", "a + b"], ["foo()", "X.y", "Abc"]),
    ("R22", r"\b[A-Z][A-Z0-9_]+\b|\bcp[0-9A-Fa-f]+\b", 0, "search", PORTER + ":332 (used constants)", SYN,
     ["FOO", "cp1F"], ["Foo", "cpx"]),
    ("R23", r"\bnew\s+([A-Z]\w*)\s*\(", 0, "search", PORTER + ":341 (nested class use)", SYN,
     ["new Foo(", "new  Bar ()"], ["new int[3]", "renew Foo("]),
    ("R24", r"\n(\s*)((?:private |protected |public )?(?:static )?(?:final )?class (\w+)\b[^{]*\{)", 0, "search",
     PORTER + ":345 (nested class)", SYN,
     [NL + "    private static class Foo {", NL + "class Bar extends X {"],
     ["class Foo {", NL + "    interface Foo {"]),
    ("R25", r"^(private |protected |public )?(static )?", 0, "search", PORTER + ":363 (nested class modifiers)", SYN,
     ["private static class A", "class B"], []),
    ("R26", r"\\u[0-9a-fA-F]{0,3}$", 0, "search", PORTER + ":383 (cut not inside a Unicode escape)", SYN,
     ["abc" + B + "u12", B + "u"], [B + "u1234", "abc"]),
    ("R27", r"^(.*?):(\d+): error: (.*)$", 0, "match", PRUNER + ":38 (javac error line, BLLSE)", SYN,
     ["A.java:12: error: cannot find symbol"], ["A.java:12: warning: x", "error: x"]),
    ("R28", r"\n    @Test\n    void \w+\(\) \{\n    \}\n", 0, "search", PRUNER + ":58 (emptied test method)", SYN,
     [NL + "    @Test" + NL + "    void t() {" + NL + "    }" + NL],
     [NL + "    @Test" + NL + "    void t() {" + NL + "        x();" + NL + "    }" + NL]),
]


def cell(examples):
    return " ;; ".join(e.replace(B, B + B).replace("\t", B + "t").replace(NL, B + "n") for e in examples) or "-"


def main():
    out = ["id\tpattern\tflavour\tlanguage_version\tsource\tlicence\tpositive_examples\tnegative_examples\toracle"]
    bad = 0
    for rid, pattern, flags, call, source, licence, positives, negatives in ROWS:
        regex = re.compile(pattern, flags)
        test = regex.match if call == "match" else regex.search
        for e in positives:
            bad += test(e) is None
        for e in negatives:
            bad += test(e) is not None
        flag_names = "+".join(n for n, f in (("MULTILINE", re.M), ("DOTALL", re.S)) if flags & f) or "none"
        out.append("\t".join([rid, pattern, "python re (re.%s, flags %s)" % (call, flag_names), "Python 3.12.6",
                              source, licence, cell(positives),
                              cell(negatives) if negatives else "- (matches the empty prefix of every string)",
                              "tools/regex_spec.py: Python 3.12.6 re.%s on every example" % call]))
    if bad:
        print("REGEX_SPEC examples disagree: %d" % bad)
        sys.exit(1)
    open(sys.argv[1], "w", encoding="utf-8", newline="").write("\n".join(out) + "\n")
    print("regex_spec\trows\t%d\texamples verified" % len(ROWS))


main()
