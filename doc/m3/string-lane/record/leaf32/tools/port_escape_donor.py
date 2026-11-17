# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_escape_donor.py <porter.py> <spec.tsv> <StringEscapeUtilsTest.java> <work-dir> <out.java> <skipped.tsv>

ESCAPE_CODE_DONOR_PORT v1 (2026-11-17). Ports commons-text 1.15.0's StringEscapeUtilsTest with the commons-lang3 porter
(DONOR_TEST_PORT v1), which only reads StringUtils.<op>( calls and single local declarations. Mechanical steps, CPU
only, no regular expressions:
  1. each spec op called as 'StringEscapeUtils.<op>(' or, statically imported, as a bare '<op>(' becomes
     'StringUtils.<op>(' (the other StringEscapeUtils members are left alone, so the porter ignores them);
  2. try { ... } catch (...) { ... } statements are removed: they test the translator objects with a null Writer,
     which have no String counterpart;
  3. the donor's assertion helpers are inlined: assertEscapeJava(expected, original[, message]) and
     assertUnescapeJava(...) become assertEquals(expected, StringUtils.escapeJava(original)) /
     StringUtils.unescapeJava(original); checkCsvEscapeWriter / checkCsvUnescapeWriter(expected, value) become
     assertEquals(expected, StringUtils.escapeCsv(value)) / unescapeCsv(value);
  4. runs the porter; then the class header names commons-text.
"""
import subprocess
import sys

porter, spec, donor, work, out, skipped = sys.argv[1:7]
OPS = [line.split("\t")[0] for line in open(spec, encoding="utf-8") if line.strip() and not line.startswith("#")]
HELPERS = {"assertEscapeJava": "escapeJava", "assertUnescapeJava": "unescapeJava",
           "checkCsvEscapeWriter": "escapeCsv", "checkCsvUnescapeWriter": "unescapeCsv"}


def skip_literal(text, i):
    """Index after the string or char literal starting at text[i]."""
    quote = text[i]
    i += 1
    while text[i] != quote:
        i += 2 if text[i] == "\\" else 1
    return i + 1


def matching(text, i, open_, close):
    """Index after the bracket closing the one at text[i], literals skipped."""
    depth = 0
    while True:
        c = text[i]
        if c in "\"'":
            i = skip_literal(text, i)
            continue
        depth += c == open_
        depth -= c == close
        i += 1
        if depth == 0:
            return i


def split_args(text):
    """Top-level comma-separated pieces of an argument list."""
    parts, depth, start, i = [], 0, 0, 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            i = skip_literal(text, i)
            continue
        depth += c in "([{"
        depth -= c in ")]}"
        if c == "," and depth == 0:
            parts.append(text[start:i].strip())
            start = i + 1
        i += 1
    parts.append(text[start:].strip())
    return parts


def is_word(c):
    return c.isalnum() or c == "_"


def rewrite(text):
    """One left-to-right pass: literals kept, spec calls qualified, try statements dropped, helpers inlined."""
    outp, i, n = [], 0, len(text)
    while i < n:
        c = text[i]
        if c in "\"'":
            j = skip_literal(text, i)
            outp.append(text[i:j])
            i = j
            continue
        if not is_word(c) or (i and (is_word(text[i - 1]) or text[i - 1] == ".")):
            outp.append(c)
            i += 1
            continue
        j = i
        while j < n and is_word(text[j]):
            j += 1
        word = text[i:j]
        k = j
        while k < n and text[k] == " ":
            k += 1
        if word == "StringEscapeUtils" and text.startswith(".", j):
            m = j + 1
            while m < n and is_word(text[m]):
                m += 1
            if text[j + 1:m] in OPS and text.startswith("(", m):
                outp.append("StringUtils." + text[j + 1:m])
                i = m
                continue
        if word in OPS and text.startswith("(", k):
            outp.append("StringUtils." + word)
            i = j
            continue
        if word == "try" and text.startswith("{", k):
            end = matching(text, k, "{", "}")
            while True:
                m = end
                while m < n and text[m] in " \r\n\t":
                    m += 1
                if not text.startswith("catch", m):
                    break
                m = matching(text, text.index("(", m), "(", ")")
                end = matching(text, text.index("{", m), "{", "}")
            i = end
            continue
        if word in HELPERS and text.startswith("(", k) and not text[:i].rstrip().endswith("void"):
            end = matching(text, k, "(", ")")
            args = split_args(text[k + 1:end - 1])
            inner = rewrite(args[1])
            outp.append("assertEquals(%s, StringUtils.%s(%s))" % (rewrite(args[0]), HELPERS[word], inner))
            i = end
            continue
        outp.append(word)
        i = j
    return "".join(outp)


source = open(donor, encoding="utf-8").read()
path = work + "/StringEscapeUtilsTest.java"
open(path, "w", encoding="utf-8", newline="").write(rewrite(source))
subprocess.run([sys.executable, "-I", porter, spec, "StringEscapeCodeFamilyDonorTest", out, skipped, path],
               check=True)
text = open(out, encoding="utf-8", newline="").read()
text = text.replace(" * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to\n"
                    " *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n",
                    " * @summary The assertions of commons-text 1.15.0's own StringEscapeUtils tests for this\n"
                    " *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n"
                    " *          through port_escape_donor.py\n")
text = text.replace(" * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources;",
                    " * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by\n"
                    " * port_escape_donor.py (StringEscapeUtils. read as StringUtils.);")
open(out, "w", encoding="utf-8", newline="").write(text)
