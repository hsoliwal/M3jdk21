# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jdk_self_call.py <dir> : JDK_SELF_CALL v1. A donor absorbed INTO java.base must not call the public JDK API it now
implements: FastDoubleParser's slow paths call Double.parseDouble / Float.parseFloat, which (once FloatingDecimal routes
through the absorbed parser) recurse until StackOverflowError. In code regions only (comments and strings masked),
Double.parseDouble(<arg>) -> FloatingDecimal.readJavaFormatString(<arg>).doubleValue() and Float.parseFloat(<arg>) ->
FloatingDecimal.readJavaFormatString(<arg>).floatValue(), with <arg> taken by balanced parentheses (may span lines).
In place, deterministic; prints counts; exits 1 if a call is left."""
import os
import re
import sys

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)
CALLS = (("Double.parseDouble(", "doubleValue"), ("Float.parseFloat(", "floatValue"))


def masked(s):
    return MASK.sub(lambda m: "".join(c if c == "\n" else " " for c in m.group(0)), s)


def rewrite(s):
    n = 0
    for call, conv in CALLS:
        while True:
            m = masked(s)
            hit = re.search(r"(?<![\w.])" + re.escape(call), m)
            if not hit:
                break
            i, depth = hit.end(), 1
            while depth:
                depth += (m[i] == "(") - (m[i] == ")")
                i += 1
            arg = s[hit.end():i - 1]
            s = s[:hit.start()] + "FloatingDecimal.readJavaFormatString(" + arg + ")." + conv + "()" + s[i:]
            n += 1
    return s, n


def main():
    root, total, left = sys.argv[1], 0, 0
    for f in sorted(os.listdir(root)):
        if not f.endswith(".java") or f == "FloatingDecimal.java":
            continue
        p = os.path.join(root, f)
        s = open(p, encoding="utf-8", newline="").read()
        new, n = rewrite(s)
        if n:
            open(p, "w", encoding="utf-8", newline="").write(new)
            print(f"{f}: {n}")
            total += n
        left += len(re.findall(r"(?<![\w.])(?:Double\.parseDouble|Float\.parseFloat)\(", masked(new)))
    print(f"JDK_SELF_CALL rewritten={total} left={left}")
    sys.exit(1 if left else 0)


main()
