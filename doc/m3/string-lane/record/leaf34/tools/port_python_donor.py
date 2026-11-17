# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_python_donor.py <test_html.py> <class-name> <out.java>

ESCAPE_MARKUP_PYTHON_DONOR_PORT v1 (2026-11-17). Ports CPython 3.12's own Lib/test/test_html.py to a JDK JUnit class
for String.escapeHtml(boolean) and String.unescapeHtml5Python(), mechanically (CPU only, no regex): runs the donor
test module under unittest with html.escape and html.unescape wrapped so that every call is recorded with the test
method that made it; the run must pass (each recorded answer is then an answer the donor test asserted, directly or
through its check helpers). Each distinct (call, argument, answer) becomes an assertEquals in one @Test method per
donor test method, in first-call order. Literals are ASCII Java (TAB, LF and CR as escape sequences, every other
unit outside printing ASCII as a Unicode escape), split into pieces that fit 100 columns.
"""
import html
import importlib.util
import sys
import unittest

source, CLASS, OUT = sys.argv[1:4]
BACKSLASH = chr(92)
records = {}
current = ["?"]
plain_escape, plain_unescape = html.escape, html.unescape


def record(key):
    calls = records.setdefault(current[0], [])
    if key not in calls:
        calls.append(key)


def escape(s, quote=True):
    answer = plain_escape(s, quote)
    record(("escape", s, bool(quote), answer))
    return answer


def unescape(s):
    answer = plain_unescape(s)
    record(("unescape", s, None, answer))
    return answer


class Recorder(unittest.TextTestResult):
    def startTest(self, test):
        current[0] = test.id().split(".")[-1]
        super().startTest(test)


html.escape, html.unescape = escape, unescape
spec = importlib.util.spec_from_file_location("donor_test_html", source)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
suite = unittest.defaultTestLoader.loadTestsFromModule(module)
result = unittest.TextTestRunner(resultclass=Recorder, verbosity=0).run(suite)
if not result.wasSuccessful():
    sys.exit("donor test failed on this CPython")


def java_units(text):
    units = text.encode("utf-16-be", "surrogatepass")
    out = []
    for i in range(0, len(units), 2):
        u = units[i] << 8 | units[i + 1]
        c = chr(u)
        if c == '"' or c == BACKSLASH:
            out.append(BACKSLASH + c)
        elif u in (9, 10, 13):
            out.append(BACKSLASH + {9: "t", 10: "n", 13: "r"}[u])
        elif 32 <= u < 127:
            out.append(c)
        else:
            out.append(BACKSLASH + "u%04X" % u)
    return out


def literal(text, room):
    """Java expression for text: one literal, or pieces joined by ' +' over several lines."""
    units = java_units(text)
    pieces, piece = [], ""
    for unit in units:
        if len(piece) + len(unit) > room:
            pieces.append(piece)
            piece = ""
        piece += unit
    pieces.append(piece)
    return ['"' + p + '"' for p in pieces]


def statement(call, argument, quote, answer):
    receiver = literal(argument, 56)
    expected = literal(answer, 70)
    method = "escapeHtml(%s)" % ("true" if quote else "false") if call == "escape" else "unescapeHtml5Python()"
    lines = ["        assertEquals(" + " +\n                ".join(expected) + ","]
    lines.append("                (" + " +\n                ".join(receiver) + ")." + method + ");")
    return "\n".join(lines)


count = 0
with open(OUT, "w", encoding="ascii", newline="") as w:
    w.write("/*\n * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.\n * GPLv2 with the Classpath exception.\n"
            " *\n * @test\n * @summary The assertions of CPython 3.12's own test_html.py (html.escape, html.unescape),\n"
            " *          ported to java.lang.String by port_python_donor.py\n * @run junit %s\n */\n\n" % CLASS)
    w.write("import static org.junit.jupiter.api.Assertions.assertEquals;\n\nimport org.junit.jupiter.api.Test;\n\n")
    w.write("/**\n * Ported mechanically from CPython %s Lib/test/test_html.py (PSF-2.0): each html.escape and\n"
            " * html.unescape call the passing donor test made, with its answer.\n */\n" % sys.version.split()[0])
    w.write("public class %s {\n" % CLASS)
    for method, calls in records.items():
        w.write("\n    @Test\n    void htmlTests_%s() {\n" % method)
        for call in calls:
            w.write(statement(*call) + "\n")
            count += 1
        w.write("    }\n")
    w.write("}\n")
print("ported %d assertions in %d tests" % (count, len(records)))
