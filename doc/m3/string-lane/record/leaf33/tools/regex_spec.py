# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""regex_spec.py <out.tsv>: writes REGEX_SPEC.tsv for the ENCODE draft and checks every row.

ENCODE_REGEX_SPEC v1 (2026-11-17). One row per regex used anywhere in the draft: the product method text (none), the
proof harness and scripts, the oracles, the donor-test tools (port_guava_donor.py and the shared port_donor_tests.py
it runs). Columns: id, pattern, flavour, language+version, source, licence, positive examples, negative examples
(JSON lists), oracle. Check: Python 're' rows (and the Java rows, literal patterns with the same meaning in both) are
run with re.search on each example (positive must match, negative must not); grep rows are run with the grep on PATH;
the shared porter's rows are also checked to occur verbatim in port_donor_tests.py. A TAB in a pattern is written \\t.
"""
import json
import re
import subprocess
import sys

B = chr(92)
PORTER = "J:/m3-scratch/donor-tests-20261117/port_donor_tests.py"
PY = "Python re (3.12)"
ROWS = []


def row(rid, pattern, flavour, lang, source, licence, pos, neg, oracle):
    ROWS.append([rid, pattern, flavour, lang, source, licence, pos, neg, oracle])


# product code
row("R-PRODUCT-0", "(none)", "-", "Java 21", "encode_family.txt", "Apache-2.0 OR EPL-2.0", [], [],
    "rg -c 'Pattern|matches\\(|split\\(|replaceAll' encode_family.txt = 0")
# proof harness and scripts
row("R-PROOF-1", B + "t", "java.util.regex (String.split)", "Java 21", "proof/EncodeFamilyProof.java rows()",
    "Apache-2.0 OR EPL-2.0", ["=a" + chr(9) + "hB"], ["=a hB"], "python re.search (same meaning for a literal TAB)")
row("R-PROOF-2", B + "n", "java.util.regex (String.split)", "Java 21", "proof/EncodeFamilyProof.java lines()",
    "Apache-2.0 OR EPL-2.0", ["0041" + chr(10)], ["0041"], "python re.search (same meaning for a literal LF)")
row("R-PROOF-3", B + "t0" + B + "t-$", "GNU grep BRE (literal TABs in the script)", "grep (Git for Windows)",
    "proof/run_proof.sh (grep -v: hide checks with 0 mismatches)", "Apache-2.0 OR EPL-2.0",
    ["rot13() vs python codecs rot13" + chr(9) + "109548" + chr(9) + "0" + chr(9) + "-"],
    ["decodePercent() vs python unquote(s)" + chr(9) + "112035" + chr(9) + "2970" + chr(9) + "x"], "grep")
row("R-PROOF-4", "Test run finished|containers|tests ", "GNU grep ERE", "grep (Git for Windows)",
    "proof/run_junit.sh (summary lines)", "Apache-2.0 OR EPL-2.0",
    ["Test run finished after 14247 ms", "[      1900 tests found           ]"], ["Thanks for using JUnit!"], "grep -E")
row("R-PROOF-5", "MethodSource|=> |JUnit Jupiter:", "GNU grep ERE", "grep (Git for Windows)",
    "proof/run_junit.sh (failure lines)", "Apache-2.0 OR EPL-2.0",
    ["  => org.opentest4j.AssertionFailedError: x", "MethodSource [className = 'A']"], ["[ 0 tests failed ]"],
    "grep -E")
row("R-ORACLE-1", "([" + B + "x00-" + B + "x7f]+)", PY, "CPython 3.12.6",
    "urllib/parse.py _asciire (inside the oracle urllib.parse.unquote; donor code, not draft code)", "PSF-2.0",
    ["%C3%A9", "a+b"], [chr(0xE9)], "python re.search")
# donor-test tool of this draft
P = "tools/port_guava_donor.py"
row("R-PORT-1", "assertThat" + B + "((.+)" + B + ")" + B + ".isEqualTo" + B + "((.+)" + B + ");", PY, "CPython 3.12.6",
    P + " common() (Truth to JUnit)", "Apache-2.0 OR EPL-2.0",
    ['assertThat(e.escape("")).isEqualTo("");'], ['assertEquals("", e.escape(""));'], "python re.search")
row("R-PORT-2", "(?m)^(" + B + "s*)public void test", PY, "CPython 3.12.6", P + " common() (JUnit 3 to @Test)",
    "Apache-2.0 OR EPL-2.0", ["  public void testEncodedLength_validStrings() {"],
    ["  private static void testEncodedLengthFails("], "python re.search")
row("R-PORT-3", "(?<![." + B + "w])assertEscaping" + B + "(", PY, "CPython 3.12.6",
    P + " rewrite_calls (also assertUnescaped, assertUnicodeEscaping, newString, testEncodedLengthFails, "
    "e" + B + ".escape)", "Apache-2.0 OR EPL-2.0", ["    assertEscaping(e, \"%00\", '" + B + "u0000');"],
    ["import static x.EscaperAsserts.assertEscaping;", "myassertEscaping(e)"], "python re.search")
row("R-PORT-4", "try " + B + "{" + B + "s*e" + B + ".escape" + B + "(" + B + "(String" + B + ") null" + B + ");" + B
    + "s*fail" + B + "([^;]*" + B + ");" + B + "s*" + B + "} catch " + B + "(NullPointerException x" + B + ") " + B
    + "{[^}]*" + B + "}", PY, "CPython 3.12.6", P + " common() (try/fail/catch to assertThrows)",
    "Apache-2.0 OR EPL-2.0",
    ['try {\n      e.escape((String) null);\n      fail("x");\n    } catch (NullPointerException x) {\n    }'],
    ['try { e.escape(""); } catch (NullPointerException x) {}'], "python re.search")
row("R-PORT-5", "(?<![." + B + "w])(MIN_HIGH_SURROGATE|MAX_HIGH_SURROGATE|MIN_LOW_SURROGATE|MAX_LOW_SURROGATE|"
    "MIN_SUPPLEMENTARY_CODE_POINT|MAX_CODE_POINT)" + B + "b", PY, "CPython 3.12.6",
    P + " (static imports of Character written out)", "Apache-2.0 OR EPL-2.0", ["newString(MIN_HIGH_SURROGATE)"],
    ["Character.MIN_HIGH_SURROGATE", "MIN_HIGH_SURROGATES"], "python re.search")
row("R-PORT-6", "(?m)^(" + B + "s*)(?:private )?static String newString" + B + "(char" + B + "." + B + "." + B
    + ". chars" + B + ") " + B + "{", PY, "CPython 3.12.6", P + " (donor helper renamed before inlining)",
    "Apache-2.0 OR EPL-2.0", ["  private static String newString(char... chars) {"],
    ["    newString(a, b);"], "python re.search")
row("R-PORT-7", "private static void testEncodedLengthFails" + B + "(", PY, "CPython 3.12.6",
    P + " (donor helper renamed before inlining)", "Apache-2.0 OR EPL-2.0",
    ["  private static void testEncodedLengthFails(String s, int i) {"], ["    testEncodedLengthFails(s, 0);"],
    "python re.search")
row("R-PORT-8", "(?m)^(" + B + "s*)static void (" + B + "w+)" + B + "(UnicodeEscaper e" + B + ")", PY,
    "CPython 3.12.6", P + " (UrlEscaperTesting helpers made @Test)", "Apache-2.0 OR EPL-2.0",
    ["  static void assertPathEscaper(UnicodeEscaper e) {"], ["  private UrlEscaperTesting() {}"],
    "python re.search")
row("R-PORT-9", "void testUrlPathSegmentEscaper" + B + "(" + B + ")" + B + "s*" + B + "{", PY, "CPython 3.12.6",
    P + " per_method (also testUrlFragmentEscaper)", "Apache-2.0 OR EPL-2.0",
    ["@Test public void testUrlPathSegmentEscaper() {"], ["public void testUrlFragmentEscaper() {"],
    "python re.search")
# the shared porter (DONOR_TEST_PORT v1), run unchanged
PORTER_ROWS = [
    ("^" + B + "(" + B + "s*[" + B + "w.]+" + B + "s*" + B + "[" + B + "]" + B + "s*" + B + ")" + B + "s*null$",
     ["(char[]) null"], ["null"]),
    (B + "bStringUtils" + B + ".(" + B + "w+)" + B + "(", ["StringUtils.encodedLength(x)"], ["Utf8.encodedLength(x)"]),
    ("^" + B + "(" + B + "s*(String|CharSequence)" + B + "s*" + B + ")" + B + "s*null$", ["(String) null"],
     ["(Object) null"]),
    ("^" + B + "(" + B + "s*[" + B + "w.<>" + B + "[" + B + "] ]+" + B + "s*" + B + ")" + B + "s*null$",
     ["(char[]) null"], ["\"null\""]),
    (B + "s*assert" + B + "w+" + B + "((.*)" + B + ")" + B + "s*$", ["assertEquals(1, x)"], ["fail()"]),
    (B + "b(String" + B + ".|asString" + B + "()", ["asString(\"a\").utf8Length()"], ["text.length()"]),
    ("@Test" + B + "b[^{;]*?void" + B + "s+(" + B + "w+)" + B + "s*" + B + "([^)]*" + B + ")" + B
     + "s*(?:throws[^{]*)?" + B + "{", ["@Test public void testA() {"], ["public void testA() {"]),
    ("^" + B + "s*static" + B + "s*" + B + "{", ["  static {"], ["  static int x;"]),
    ("^" + B + "s*(?:private |protected |public )?static final (?:String|char|int|char" + B + "[" + B
     + "]|String" + B + "[" + B + "]) (" + B + "w+)( = [^;]+)?;", ["  private static final String FOO = \"x\";"],
     ["  static String foo;"]),
    ("^(private |protected |public )?static final", ["private static final int X"], ["final int x"]),
    ("^" + B + "s*(private |protected |public )?", ["  private int x"], []),
    ("^StringUtils|Test$", ["Utf8Test"], ["UrlEscaperTesting_fragment"]),
    ("^(?:final" + B + "s+)?(String|char|int|char" + B + "[" + B + "]|String" + B + "[" + B + "])" + B + "s+("
     + B + "w+)" + B + "s*=", ["String s = \"a\""], ["s = \"a\""]),
    (B + "bassert(IllegalArgument|NullPointer)Exception" + B + "(", ["assertNullPointerException(() -> x)"],
     ["assertThrows(NullPointerException.class, x)"]),
    (B + "bStringUtils" + B + ".(EMPTY|SPACE|LF|CR)" + B + "b", ["StringUtils.EMPTY"], ["StringUtils.EMPTYISH"]),
    (B + "bSuppliers" + B + ".nul" + B + "(" + B + ")", ["Suppliers.nul()"], ["Suppliers.null()"]),
    (B + "b" + B + "w+Test" + B + ".(" + B + "w+)" + B + "b", ["Utf8Test.FOO"], ["Utf8.FOO"]),
    ("(?<![." + B + "w])[A-Z][A-Z0-9_]+" + B + "b", ["x + FOO_BAR"], ["Character.MIN_HIGH_SURROGATE"]),
    ("(?<![." + B + "w])([A-Za-z_]" + B + "w*)" + B + "b", ["asString"], [".x"]),
    ("(?<![." + B + "w])([a-z]" + B + "w*)" + B + "b(?!" + B + "s*" + B + "()", ["x + y"], ["f()"]),
    (B + "b[A-Z][A-Z0-9_]+" + B + "b|" + B + "bcp[0-9A-Fa-f]+" + B + "b", ["cp1F600", "FOO"], ["Foo"]),
    (B + "bnew" + B + "s+([A-Z]" + B + "w*)" + B + "s*" + B + "(", ["new String(x)"], ["new char[] {a}"]),
    (B + "n(" + B + "s*)((?:private |protected |public )?(?:static )?(?:final )?class (" + B + "w+)" + B + "b[^{]*"
     + B + "{)", ["\n  private static class Foo {"], ["class Foo {"]),
    ("^(private |protected |public )?(static )?", ["private static class A"], []),
    (B + B + "u[0-9a-fA-F]{0,3}$", [B + "u00"], [B + "u00E9"]),
]
for i, (pattern, pos, neg) in enumerate(PORTER_ROWS, 1):
    row("R-PORTER-%d" % i, pattern, PY, "CPython 3.12.6", "port_donor_tests.py (DONOR_TEST_PORT v1), run unchanged",
        "Apache-2.0 OR EPL-2.0", pos, neg, "python re.search; verbatim in port_donor_tests.py")


def check():
    porter = open(PORTER, encoding="utf-8").read()
    failures = []
    for r in ROWS:
        rid, pattern, flavour, _, source, _, pos, neg, oracle = r
        if pattern == "(none)":
            continue
        if flavour.startswith("GNU grep"):
            real = pattern.replace(B + "t", chr(9))
            flag = "-E" if "ERE" in flavour else "-G"
            for text, want in [(p, True) for p in pos] + [(n, False) for n in neg]:
                got = subprocess.run(["grep", "-q", flag, real], input=text.encode(), check=False).returncode == 0
                if got != want:
                    failures.append((rid, text))
            continue
        for text in pos:
            if not re.search(pattern, text):
                failures.append((rid, "positive", text))
        for text in neg:
            if re.search(pattern, text):
                failures.append((rid, "negative", text))
        fragment = pattern.split("(" + B + "w+)( = ")[0]
        if rid.startswith("R-PORTER") and fragment not in porter:
            failures.append((rid, "not verbatim in porter"))
    return failures


failures = check()
with open(sys.argv[1], "w", encoding="utf-8", newline="") as w:
    w.write("id\tpattern\tflavour\tlanguage_version\tsource\tlicence\tpositive_examples\tnegative_examples\toracle\n")
    for r in ROWS:
        cells = [c if isinstance(c, str) else json.dumps(c) for c in r]
        cells = [c.replace(chr(9), B + "t").replace(chr(10), B + "n") for c in cells]
        w.write("\t".join(cells) + "\n")
print("rows", len(ROWS), "failures", failures)
sys.exit(1 if failures else 0)
