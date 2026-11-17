# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_markup_donor.py <porter.py> <prune.py> <spec.tsv> <donor-test.java> <work-dir> <out.java> <skipped.tsv>
                        <java-home> <patched-java-base> <junit-jar>

ESCAPE_MARKUP_DONOR_PORT v1 (2026-11-17). Ports commons-text 1.15.0's StringEscapeUtilsTest with the commons-lang3
porter (DONOR_TEST_PORT v1), which only reads StringUtils.<op>( calls. Mechanical steps, CPU only, no regex here:
  1. a copy of the donor test with 'StringEscapeUtils.' rewritten to 'StringUtils.' (a call of another
     StringEscapeUtils method then reads as another StringUtils op and is skipped by the porter), and each loop
     'for (final String[] element : HTML_ESCAPES)' unrolled into one @Test method <method>_row<n> per row of
     HTML_ESCAPES, the loop's 'final String x = element[k];' locals inlined as the row's literals (the porter reads
     neither String[][] constants nor loop variables; a null row value becomes a literal null receiver, which the
     porter skips as NULL_RECEIVER);
  2. runs the porter; the class header then names commons-text instead of commons-lang3;
  3. runs prune_uncompilable.py (DONOR_TEST_PRUNE v1) against the patched java.base: a ported statement that does not
     compile (a loop variable of the donor, a local the porter cannot see) goes to skipped.tsv as COMPILE:<message>.
"""
import subprocess
import sys

porter, prune, spec, donor, work, out, skipped, java_home, patched, junit = sys.argv[1:11]
LOOP = "for (final String[] element : HTML_ESCAPES) {"
NL = chr(10)


def literal_end(text, i):
    """Index after the string or char literal starting at text[i]."""
    quote, i = text[i], i + 1
    while text[i] != quote:
        i += 2 if text[i] == "\\" else 1
    return i + 1


def closing(text, i):
    """Index of the brace closing the one at text[i], literals skipped."""
    depth = 0
    while True:
        c = text[i]
        if c in "\"'":
            i = literal_end(text, i)
            continue
        depth += c == "{"
        depth -= c == "}"
        if depth == 0:
            return i
        i += 1


def split_top(text):
    """Comma-separated items of text outside literals and brackets."""
    items, depth, start, i = [], 0, 0, 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            i = literal_end(text, i)
            continue
        depth += c in "({["
        depth -= c in ")}]"
        if c == "," and depth == 0:
            items.append(text[start:i].strip())
            start = i + 1
        i += 1
    items.append(text[start:].strip())
    return [item for item in items if item]


def rename(text, mapping):
    """text with whole identifiers in mapping replaced, outside literals."""
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            j = literal_end(text, i)
            out.append(text[i:j])
            i = j
        elif c.isalpha() or c == "_":
            j = i
            while j < len(text) and (text[j].isalnum() or text[j] == "_"):
                j += 1
            word = text[i:j]
            out.append(mapping.get(word, word) if text[i - 1] != "." else word)
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def unroll(text):
    """Each HTML_ESCAPES loop unrolled into one @Test method per row (see step 1)."""
    table = text.index("{", text.index("HTML_ESCAPES = {"))
    rows = [split_top(row.strip()[1:-1]) for row in split_top(text[table + 1:closing(text, table)])]
    while LOOP in text:
        loop = text.index(LOOP)
        open_brace = loop + len(LOOP) - 1
        end = closing(text, open_brace)
        method_open = text.rindex("() {", 0, loop) + 3
        method_name = text[text.rindex("void ", 0, method_open) + 5:method_open - 3]
        body, locals_ = [], {}
        for line in text[open_brace + 1:end].split(NL):
            if "= element[" in line:
                name = line.split("=")[0].split()[-1]
                locals_[name] = int(line.split("element[")[1].split("]")[0])
            else:
                body.append(line)
        methods = []
        for n, row in enumerate(rows, 1):
            mapping = {name: row[k] for name, k in locals_.items()}
            lines = [rename(line, mapping) for line in body]
            head = NL + "    @Test" + NL + "    void %s_row%d() {" % (method_name, n)
            methods.append(head + NL.join(lines) + NL + "    }" + NL)
        text = text[:loop] + text[end + 1:]
        method_end = closing(text, method_open)
        text = text[:method_end + 1] + NL + "".join(methods) + text[method_end + 1:]
    return text


text = unroll(open(donor, encoding="utf-8").read().replace("StringEscapeUtils.", "StringUtils."))
copy = work + "/StringEscapeUtilsTest.java"
open(copy, "w", encoding="utf-8", newline="").write(text)
name = out.replace("\\", "/").split("/")[-1][:-5]
subprocess.run([sys.executable, "-I", porter, spec, name, out, skipped, copy], check=True)
java = open(out, encoding="utf-8", newline="").read()
java = java.replace(" * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to\n"
                    " *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n",
                    " * @summary The assertions of commons-text 1.15.0's own StringEscapeUtils tests for this\n"
                    " *          family, ported to java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n"
                    " *          through port_markup_donor.py\n")
java = java.replace(" * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources;",
                    " * Ported mechanically from commons-text 1.15.0 (Apache-2.0) test sources by\n"
                    " * port_markup_donor.py (StringEscapeUtils. read as StringUtils.);")
open(out, "w", encoding="utf-8", newline="").write(java)
classes = work + "/donor-classes"
command = [java_home + "/bin/javac", "-nowarn", "-d", classes, "--patch-module", "java.base=" + patched,
           "-cp", junit, out]
subprocess.run([sys.executable, "-I", prune, out, skipped, "--"] + command, check=True)
