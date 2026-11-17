# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <work>: ESCAPE_MARKUP CPython 3.12 oracle (CPU only, no regex of its own).

Reads <work>/escape_texts.txt and <work>/unescape_texts.txt (one text per line as UTF-16 units in upper-case hex,
written by EscapeMarkupFamilyProof corpus); a valid surrogate pair decodes to one code point, an unpaired surrogate
stays a code point of its own (surrogatepass). Writes the answers in the same form:
  ESCAPE_TRUE.txt      html.escape(s, True)  for each escape text
  ESCAPE_FALSE.txt     html.escape(s, False) for each escape text
  UNESCAPE.txt         html.unescape(s)      for each unescape text
  UNESCAPE_PLAIN.txt   html.unescape(s)      for each escape text
"""
import html
import sys

work = sys.argv[1]


def texts(name):
    data = open(work + "/" + name, encoding="ascii").read().split("\n")
    return [bytes.fromhex(line).decode("utf-16-be", "surrogatepass") for line in data[:-1]]


def write(name, answers):
    out = [a.encode("utf-16-be", "surrogatepass").hex().upper() for a in answers]
    open(work + "/" + name, "w", encoding="ascii", newline="").write("\n".join(out) + "\n")


escape = texts("escape_texts.txt")
unescape = texts("unescape_texts.txt")
write("ESCAPE_TRUE.txt", [html.escape(s, True) for s in escape])
write("ESCAPE_FALSE.txt", [html.escape(s, False) for s in escape])
write("UNESCAPE.txt", [html.unescape(s) for s in unescape])
write("UNESCAPE_PLAIN.txt", [html.unescape(s) for s in escape])
print("python_oracle\t%s\tescape %d\tunescape %d" % (sys.version.split()[0], len(escape), len(unescape)))
