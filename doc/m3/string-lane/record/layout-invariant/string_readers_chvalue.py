# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""string_readers_chvalue.py <String.java in> <String.java out>: STRING_LAYOUT_READERS v1 (2026-11-17), stage 2 of
the String layout invariant (doc/m3/string-lane/STRING_LAYOUT_INVARIANT.md): the char readers copy out of chValue.
  getChars(int, int, char[], int): after the unchanged bounds checks, one System.arraycopy from chValue (M3-backed
      Strings no longer walk their owners per call, flat Strings no longer inflate).
  toCharArray(): chValue.clone().
chValue never leaves String: both readers hand out a copy. A String without chValue (made outside the invariant,
for example by Unsafe.allocateInstance or with -XX:+OptimizeStringConcat) keeps the previous paths.
Every anchor must match exactly once; the recipe refuses otherwise."""
import io
import sys

SRC, DST = sys.argv[1:3]


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit("REFUSE %s: anchor found %d times, expected %d" % (label, text.count(old), count))
    return text.replace(old, new)


with io.open(SRC, encoding="utf-8", newline="") as stream:
    s = stream.read()
s = replace(s, """    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        M3String storage = m3();
""", """    public void getChars(int srcBegin, int srcEnd, char[] dst, int dstBegin) {
        checkBoundsBeginEnd(srcBegin, srcEnd, length());
        checkBoundsOffCount(dstBegin, srcEnd - srcBegin, dst.length);
        char[] chars = chValue;
        if (chars != null) {
            System.arraycopy(chars, srcBegin, dst, dstBegin, srcEnd - srcBegin);
            return;
        }
        M3String storage = m3();
""", "getChars reads chValue")
s = replace(s, """    public char[] toCharArray() {
        M3String storage = m3();
""", """    public char[] toCharArray() {
        char[] chars = chValue;
        if (chars != null) return chars.clone();
        M3String storage = m3();
""", "toCharArray clones chValue")
with io.open(DST, "w", encoding="utf-8", newline="") as stream:
    stream.write(s)
print("STRING_LAYOUT_READERS v1 applied")
