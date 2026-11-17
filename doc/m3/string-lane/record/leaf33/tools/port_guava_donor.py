# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_guava_donor.py <porter.py> <spec.tsv> <donor-src-dir> <work-dir> <out.java> <skipped.tsv>

ENCODE_GUAVA_DONOR_PORT v1 (2026-11-17). Ports the assertions of Guava 33.7.2's own tests whose donor API has exactly
the semantics of an ENCODE-family member, with the commons-lang3 porter (DONOR_TEST_PORT v1), which only reads
StringUtils.<op>( calls in @Test methods. Exact pairs: Utf8.encodedLength = utf8Length(); urlPathSegmentEscaper() =
encodePercent("!$&'()*+,;=:@"); urlFragmentEscaper() = encodePercent("!$&'()*+,;=:@/?") (proven on every code point
by EncodeFamilyProof). Not ported: urlFormParameterEscaper (java.net.URLEncoder's rule, not a member) and
PercentEscaperTest (its PercentEscaper("", ...) keeps only alphanumerics; every member keeps - . _ ~ too).
Mechanical steps, CPU only, on copies of the donor files (Utf8Test.java, UrlEscapersTest.java, UrlEscaperTesting.java):
  1. JUnit 3 'public void test' methods get '@Test'; Truth 'assertThat(A).isEqualTo(B);' becomes 'assertEquals(B, A);'
     (regex R-PORT-1 of REGEX_SPEC.tsv);
  2. the guava-testlib EscaperAsserts helpers are inlined with their documented meaning: assertEscaping(e, X, c) ->
     assertEquals(X, e.escape(String.valueOf(c))); assertUnescaped(e, c) -> assertEquals(String.valueOf(c),
     e.escape(String.valueOf(c))); assertUnicodeEscaping(e, X, h, l) -> assertEquals(X, e.escape(new String(new char[]
     {h, l}))); a try { e.escape(null); fail } catch (NullPointerException) block becomes assertThrows;
  3. Utf8Test: 'Utf8.encodedLength(' -> 'StringUtils.encodedLength('; the static imports of Character constants are
     written Character.<NAME>; the donor helpers newString(a, b) and testEncodedLengthFails(s, i) are inlined
     (new String(new char[] {a, b}); assertEquals("Unpaired surrogate at index " + i, assertThrows(...).getMessage()));
  4. UrlEscaperTesting (helpers, run for the path-segment and fragment escapers by UrlEscapersTest) is copied twice,
     UrlEscaperTesting_pathSegment and UrlEscaperTesting_fragment, its helpers made @Test; in those copies and in the
     path-segment and fragment test methods of UrlEscapersTest, 'e.escape(X)' -> 'StringUtils.encodePercent(X, safe)';
  5. runs the porter; then the class header names Guava, and lines over 100 columns are broken after a top-level '('
     or ','.
"""
import re
import subprocess
import sys

porter, spec, src, work, out, skipped = sys.argv[1:7]
PATH_SEGMENT = "!$&'()*+,;=:@"
FRAGMENT = "!$&'()*+,;=:@/?"


def close(text, i):
    """Index of the parenthesis or brace closing the one at text[i] (string and char literals skipped)."""
    opening, depth = text[i], 0
    closing = ")" if opening == "(" else "}"
    while True:
        c = text[i]
        if c in "\"'":
            j = i + 1
            while text[j] != c:
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c == opening:
            depth += 1
        elif c == closing:
            depth -= 1
            if depth == 0:
                return i
        i += 1


def args_of(text):
    """Top-level comma-separated arguments of an argument list text."""
    parts, depth, start, i = [], 0, 0, 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            j = i + 1
            while text[j] != c:
                j += 2 if text[j] == "\\" else 1
            i = j + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == "," and depth == 0:
            parts.append(text[start:i].strip())
            start = i + 1
        i += 1
    parts.append(text[start:].strip())
    return parts


def rewrite_calls(text, name, build):
    """Each call name(args) (name not preceded by '.' or a word character) replaced by build(args)."""
    pattern = re.compile(r"(?<![.\w])" + re.escape(name) + r"\(")
    while True:
        m = pattern.search(text)
        if not m:
            return text
        end = close(text, m.end() - 1)
        text = text[:m.start()] + build(args_of(text[m.end():end])) + text[end + 1:]


def common(text):
    text = re.sub(r"(?m)^(\s*)public void test", r"\1@Test public void test", text)
    text = re.sub(r"assertThat\((.+)\)\.isEqualTo\((.+)\);", r"assertEquals(\2, \1);", text)
    text = rewrite_calls(text, "assertEscaping",
                         lambda a: "assertEquals(%s, %s.escape(String.valueOf(%s)))" % (a[1], a[0], a[2]))
    text = rewrite_calls(text, "assertUnescaped",
                         lambda a: "assertEquals(String.valueOf(%s), %s.escape(String.valueOf(%s)))" % (a[1], a[0],
                                                                                                        a[1]))
    text = rewrite_calls(text, "assertUnicodeEscaping",
                         lambda a: "assertEquals(%s, %s.escape(new String(new char[] {%s, %s})))" % (a[1], a[0], a[2],
                                                                                                   a[3]))
    return re.sub(r"try \{\s*e\.escape\(\(String\) null\);\s*fail\([^;]*\);\s*\} catch \(NullPointerException x\) "
                  r"\{[^}]*\}", "assertThrows(NullPointerException.class, () -> e.escape((String) null));", text)


def escapes_to(text, safe):
    return rewrite_calls(text, "e.escape", lambda a: 'StringUtils.encodePercent(%s, "%s")' % (a[0], safe))


def per_method(text, mapping):
    """e.escape( rewritten inside each method named in mapping (method name -> safe set)."""
    for method, safe in mapping.items():
        m = re.search(r"void %s\(\)\s*\{" % method, text)
        end = close(text, m.end() - 1)
        text = text[:m.start()] + escapes_to(text[m.start():end + 1], safe) + text[end + 1:]
    return text


donors = []
utf8 = common(open(src + "/Utf8Test.java", encoding="utf-8").read())
utf8 = utf8.replace("Utf8.encodedLength(", "StringUtils.encodedLength(")
utf8 = re.sub(r"(?<![.\w])(MIN_HIGH_SURROGATE|MAX_HIGH_SURROGATE|MIN_LOW_SURROGATE|MAX_LOW_SURROGATE|"
              r"MIN_SUPPLEMENTARY_CODE_POINT|MAX_CODE_POINT)\b", r"Character.\1", utf8)
utf8 = re.sub(r"(?m)^(\s*)(?:private )?static String newString\(char\.\.\. chars\) \{", r"\1static String unused(char... chars) {",
              utf8)
utf8 = rewrite_calls(utf8, "newString", lambda a: "new String(new char[] {%s})" % ", ".join(a))
utf8 = re.sub(r"private static void testEncodedLengthFails\(", "private static void encodedLengthFails(", utf8)
utf8 = rewrite_calls(utf8, "testEncodedLengthFails",
                     lambda a: 'assertEquals("Unpaired surrogate at index " + %s, assertThrows('
                               'IllegalArgumentException.class, () -> StringUtils.encodedLength(%s)).getMessage())'
                               % (a[1], a[0]))
donors.append(("Utf8Test", utf8))
helpers = common(open(src + "/UrlEscaperTesting.java", encoding="utf-8").read())
helpers = re.sub(r"(?m)^(\s*)static void (\w+)\(UnicodeEscaper e\)", r"\1@Test static void \2(UnicodeEscaper e)", helpers)
donors.append(("UrlEscaperTesting_pathSegment", escapes_to(helpers, PATH_SEGMENT)))
donors.append(("UrlEscaperTesting_fragment", escapes_to(helpers, FRAGMENT)))
tests = common(open(src + "/UrlEscapersTest.java", encoding="utf-8").read())
donors.append(("UrlEscapersTest", per_method(tests, {"testUrlPathSegmentEscaper": PATH_SEGMENT,
                                                      "testUrlFragmentEscaper": FRAGMENT})))
paths = []
for name, text in donors:
    path = "%s/%s.java" % (work, name)
    open(path, "w", encoding="utf-8", newline="").write(text)
    paths.append(path)
subprocess.run([sys.executable, "-I", porter, spec, "StringEncodeFamilyDonorTest", out, skipped] + paths, check=True)
text = open(out, encoding="utf-8", newline="").read()
text = text.replace(" * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to\n"
                    " *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n",
                    " * @summary The assertions of Guava 33.7.2's own Utf8 and UrlEscapers tests for this\n"
                    " *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n"
                    " *          through port_guava_donor.py\n")
text = text.replace(" * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources;",
                    " * Ported mechanically from Guava 33.7.2 (Apache-2.0) test sources by port_guava_donor.py\n"
                    " * (Utf8.encodedLength and the path-segment and fragment escapers read as\n"
                    " * StringUtils.);")


def wrap(line):
    """The line broken after a top-level ',' (else '(' or '.') so that each piece fits 100 columns."""
    if len(line) <= 100:
        return [line]
    indent = len(line) - len(line.lstrip())
    comma, other, i, quote = -1, -1, 0, None
    while i < min(len(line), 99):
        c = line[i]
        if quote:
            if c == "\\":
                i += 2
                continue
            if c == quote:
                quote = None
        elif c in "\"'":
            quote = c
        elif c == ",":
            comma = i + 1
        elif c in "(.":
            other = i + 1
        i += 1
    cut = comma if comma > indent + 1 else other
    if cut <= indent + 1:
        return [line]
    rest = " " * (indent + 8 if indent >= 8 else 16) + line[cut:].lstrip()
    return [line[:cut].rstrip()] + wrap(rest)


lines = []
for line in text.split("\n"):
    lines += wrap(line)
open(out, "w", encoding="utf-8", newline="").write("\n".join(lines))
