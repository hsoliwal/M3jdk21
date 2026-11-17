# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""string_layout_lazy.py <String.java in> <String.java out>: STRING_LAYOUT_LAZY v1 (2026-11-17), owner decision 51
("or use const", "lazily filled"): chValue becomes a const (write-once, @Stable) char[] filled once on the first
whole-string char demand, published with a store-store fence, never handed to a caller; value stays final and eager.
  - the field loses `final` (javadoc: lazily filled, const);
  - the 17 constructor assignments `this.chValue = layoutChars(...)` go; String(String) still shares the original's
    array (null or filled);
  - constChars(): fills chValue once (layoutChars over value/coder/m3), storeStoreFence, then publishes;
  - toCharArray() returns constChars().clone(); getChars(...) fills chValue only when the whole string is asked for,
    otherwise it copies from chValue when present and keeps the previous paths when not (a small range of a large
    string never allocates the whole char[]).
Every anchor must match the stated number of times; the recipe refuses otherwise."""
import io
import re
import sys

SRC, DST = sys.argv[1:3]


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit("REFUSE %s: anchor found %d times, expected %d" % (label, text.count(old), count))
    return text.replace(old, new)


with io.open(SRC, encoding="utf-8", newline="") as stream:
    s = stream.read()

s = replace(s, """    /**
     * The chars of this string: Java 21 compatibility is the interned chars.
     *
     * <p>A JNI-ready {@code char[]} of {@link #length()} UTF-16 units, filled when the String is
     * made (layout invariant, see {@link #value}) and protected internally: it is never handed to
     * a caller, which only ever receives a copy, and never written after construction.</p>
     */
    @Stable
    private final char[] chValue;
""", """    /**
     * The chars of this string: Java 21 compatibility is the interned chars.
     *
     * <p>A JNI-ready {@code char[]} of {@link #length()} UTF-16 units, filled lazily (owner
     * decision 51): it is {@code null} until the first whole-string char demand ({@link
     * #toCharArray()}, {@link #getChars(int, int, char[], int)} of the whole string, JNI), then
     * written once with the chars of {@link #value} and never again: a const array. It is
     * protected internally: never handed to a caller, which only ever receives a copy.</p>
     */
    @Stable
    private char[] chValue;
""", "chValue field: const, lazily filled")

s, removed = re.subn(r"\n +this\.chValue = layoutChars\(this\.value, this\.coder, this\.m3\);", "", s)
if removed != 17:
    sys.exit("REFUSE constructor assignments: %d removed, expected 17" % removed)

s = replace(s, """    private static char[] layoutChars(byte[] bytes, byte coder, M3String storage) {
""", """    private static char[] layoutChars(byte[] bytes, byte coder, M3String storage) {
        //FLLDCIM: LAYOUT - decision 51: called once per String by chars(), never at construction
""", "layoutChars anchor note")
s = replace(s, """    M3String m3() {
        return m3;
    }
""", """    M3String m3() {
        return m3;
    }

    /**
     * The const chars of this string (layout invariant, decision 51): fills {@link #chValue} once
     * with the chars of {@link #value} on the first call, behind a store-store fence so that no
     * thread sees the reference before the filled contents, and returns it. Callers copy out of
     * it; it never leaves this class.
     */
    private char[] constChars() {
        char[] chars = chValue;
        if (chars == null) {
            chars = layoutChars(value, coder, m3());
            jdk.internal.misc.Unsafe.getUnsafe().storeStoreFence();
            chValue = chars;
        }
        return chars;
    }
""", "constChars() lazy filler")

s = replace(s, """        char[] chars = chValue;
        if (chars != null) {
            System.arraycopy(chars, srcBegin, dst, dstBegin, srcEnd - srcBegin);
            return;
        }
""", """        char[] chars = srcBegin == 0 && srcEnd == length() ? constChars() : chValue;
        if (chars != null) {
            System.arraycopy(chars, srcBegin, dst, dstBegin, srcEnd - srcBegin);
            return;
        }
""", "getChars fills on a whole-string demand")
s = replace(s, """    public char[] toCharArray() {
        char[] chars = chValue;
        if (chars != null) return chars.clone();
        M3String storage = m3();
        if (storage != null) {
            return storage.charShadow();
        }
        byte[] currentValue = value();
        return isLatin1() ? StringLatin1.toChars(currentValue)
                          : StringUTF16.toChars(currentValue);
    }
""", """    public char[] toCharArray() {
        return constChars().clone();
    }
""", "toCharArray returns a copy of the const chars")
with io.open(DST, "w", encoding="utf-8", newline="") as stream:
    stream.write(s)
print("STRING_LAYOUT_LAZY v1 applied: %d constructor fills removed" % removed)
