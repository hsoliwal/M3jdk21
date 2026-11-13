# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""py_literals.py <file.py>...

PY_LITERALS v1 (CPU, read-only), the Python sibling of LITERAL_CONSTANTS v1's inventory: every numeric literal of a
Python tool other than 0 and 1 that is not part of a module-level UPPER_CASE constant definition (NAME = ..., or
NAME: type = ...; the definition may continue over following lines) is a magic number. Prints one row per file
(file, count) and the first rows of each, then 'PY_LITERALS PASS|FAIL'; exit 1 on any magic number."""
import io
import re
import sys
import tokenize

CONSTANT_START = re.compile(r"^[A-Z][A-Z0-9_]*\s*(?::[^=]+)?=")
EXEMPT = {"0", "1"}
SHOWN = 6
SNIPPET = 70
EXIT_FAIL = 1


def constant_lines(tokens):
    """Line numbers covered by module-level constant definitions (a definition ends at its NEWLINE token)."""
    covered, start = set(), None
    for tok in tokens:
        if start is None and tok.type == tokenize.NAME and tok.start[1] == 0 \
                and CONSTANT_START.match(tok.line):
            start = tok.start[0]
        if start is not None and tok.type == tokenize.NEWLINE:
            covered.update(range(start, tok.end[0] + 1))
            start = None
    return covered


def scan(path):
    with open(path, encoding="utf-8") as f:
        src = f.read()
    tokens = list(tokenize.generate_tokens(io.StringIO(src).readline))
    covered = constant_lines(tokens)
    return ["%d:%s:%s" % (t.start[0], t.string, t.line.strip()[:SNIPPET]) for t in tokens
            if t.type == tokenize.NUMBER and t.string not in EXEMPT and t.start[0] not in covered]


def main(argv):
    total = 0
    for path in argv[1:]:
        hits = scan(path)
        total += len(hits)
        sys.stdout.write("%s\t%d\n" % (path.replace("\\", "/").rsplit("/", 1)[-1], len(hits)))
        sys.stdout.write("".join("    %s\n" % h for h in hits[:SHOWN]))
    sys.stdout.write("PY_LITERALS\t%s\t%d\n" % ("PASS" if total == 0 else "FAIL", total))
    return 0 if total == 0 else EXIT_FAIL


if __name__ == "__main__":
    sys.exit(main(sys.argv))
