# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""ascii_escape.py <file.java>...: rewrites every non-ASCII character of Java sources as UTF-16 unicode escapes
(a supplementary character becomes its two surrogate escapes), in place; prints the count per file."""
import io
import sys

BACKSLASH = chr(92)
for path in sys.argv[1:]:
    with io.open(path, encoding="utf-8", newline="") as stream:
        text = stream.read()
    out, count = [], 0
    for ch in text:
        point = ord(ch)
        if point < 128:
            out.append(ch)
            continue
        count += 1
        units = [point] if point < 0x10000 else [0xD800 + ((point - 0x10000) >> 10),
                                                 0xDC00 + ((point - 0x10000) & 0x3FF)]
        out.extend(BACKSLASH + "u%04x" % unit for unit in units)
    with io.open(path, "w", encoding="utf-8", newline="") as stream:
        stream.write("".join(out))
    print(path, count)
