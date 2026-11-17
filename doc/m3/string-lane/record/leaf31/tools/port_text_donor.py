# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_text_donor.py <porter.py> <spec.tsv> <donor-src-dir> <work-dir> <out.java> <skipped.tsv>

CASE_TEXT_DONOR_PORT v1 (2026-11-17). Ports commons-text 1.15.0's WordUtilsTest and CaseUtilsTest with the
commons-lang3 porter (DONOR_TEST_PORT v1), which only reads StringUtils.<op>( calls and single local declarations.
Mechanical steps, CPU only:
  1. copies of the two donor tests with 'WordUtils.' and 'CaseUtils.' rewritten to 'StringUtils.';
     'ArrayUtils.EMPTY_CHAR_ARRAY' becomes 'new char[0]' (the port imports no commons class);
  2. a reassignment 'name = value;' of a local declared in the same test method ends that method and starts a new
     @Test method <method>_<n> declaring the local with the new value (the porter keeps only a local's first
     declaration); a local declared as null is inlined as '(char[]) null', so the porter's NPE_ARG rule applies;
  3. runs the porter; then the class header names commons-text, java.util.stream.IntStream is imported, and lines
     over 100 columns are broken after a top-level '(' or ','.
"""
import re
import subprocess
import sys

porter, spec, src, work, out, skipped = sys.argv[1:7]


def split_reassignments(text):
    """Each reassignment of a char[] local opens a new @Test method declaring it."""
    lines, result, method, count, local = text.split("\n"), [], None, 0, None
    for line in lines:
        m = re.match(r"\s*void (\w+)\(\)", line)
        if m:
            method, count, local = m.group(1), 0, None
        d = re.match(r"(\s*)char\[\] (\w+) = (.+);$", line)
        if d:
            local = d.group(2)
        r = re.match(r"(\s*)(\w+) = (.+);$", line)
        if r and local and r.group(2) == local:
            count += 1
            result += ["    }", "", "    @Test", "    void %s_%d() {" % (method, count),
                       "%schar[] %s = %s;" % (r.group(1), local, r.group(3))]
            continue
        result.append(line)
    return "\n".join(result)


def inline_null_locals(text):
    """In each method, a 'char[] x = null;' local is replaced by the literal '(char[]) null'."""
    parts = re.split(r"(\n    @Test\n)", text)
    for i, part in enumerate(parts):
        for name in re.findall(r"char\[\] (\w+) = null;", part):
            part = part.replace("char[] %s = null;" % name, "")
            part = re.sub(r"\b%s\b(?=\s*\))" % name, "(char[]) null", part)
        parts[i] = part
    return "".join(parts)


donors = []
for name in ("WordUtilsTest", "CaseUtilsTest"):
    text = open("%s/%s.java" % (src, name), encoding="utf-8").read()
    text = re.sub(r"\b(WordUtils|CaseUtils)\.", "StringUtils.", text)
    text = text.replace("ArrayUtils.EMPTY_CHAR_ARRAY", "new char[0]")
    text = inline_null_locals(split_reassignments(text))
    path = "%s/%s.java" % (work, name)
    open(path, "w", encoding="utf-8", newline="").write(text)
    donors.append(path)
subprocess.run([sys.executable, "-I", porter, spec, "StringCaseFamilyTextDonorTest", out, skipped] + donors,
               check=True)
text = open(out, encoding="utf-8", newline="").read()
text = text.replace(" * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to\n"
                    " *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n",
                    " * @summary The assertions of commons-text 1.15.0's own WordUtils and CaseUtils tests for\n"
                    " *          this family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT\n"
                    " *          v1) through port_text_donor.py\n")
text = text.replace(" * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources;",
                    " * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by\n"
                    " * port_text_donor.py (WordUtils. and CaseUtils. read as StringUtils.);")
text = text.replace("import java.util.Locale;\n", "import java.util.Locale;\nimport java.util.stream.IntStream;\n")


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
