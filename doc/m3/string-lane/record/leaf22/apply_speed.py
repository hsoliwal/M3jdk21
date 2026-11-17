# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""apply_speed.py <search.old> <edge.old> <search.new> <edge.new>: STRING_FAMILY_SPEED v1 (owner, 2026-11-17: "that is
exactly the reason why string needs to be so efficient"). Rewrites the hot paths of the SEARCH and EDGE families without
changing a result:
  1. indexOfAny(char...), indexOfAnyBut(char...), containsWhitespace: a flat Latin-1 string is scanned on its bytes;
  2. indexOfIgnoreCase / lastIndexOfIgnoreCase (and so containsIgnoreCase, containsAnyIgnoreCase): when the search
     starts with an ASCII unit, a start is compared only where its unit can match the first one: ASCII units by ASCII
     case (on the bytes of a flat Latin-1 string), other units by regionMatches' case rule for one unit; a search that
     starts with another unit keeps the plain loop (a per-unit case rule there costs as much as the region compare);
  3. normalizeSpace returns this string when it is already normalized;
  4. stripAccents maps a string of Latin-1 units through a table built once from the full algorithm.
Every anchor must match exactly once; the recipe refuses otherwise."""
import io
import sys

SEARCH_OLD, EDGE_OLD, SEARCH_NEW, EDGE_NEW = sys.argv[1:5]


def read(path):
    with io.open(path, encoding="utf-8", newline="") as stream:
        return stream.read()


def write(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as stream:
        stream.write(text)


def replace(text, old, new, label):
    if text.count(old) != 1:
        sys.exit(f"REFUSE {label}: anchor found {text.count(old)} times")
    return text.replace(old, new)


search = read(SEARCH_OLD)
search = replace(search, """        boolean wide = hasWideUnit(chars);
        for (int index = 0; index < length; index++) {
            char unit = charAt(index);
            if (unit < 128) {
                if (inMask(unit, low, high)) return index;
            } else if (wide && anyMatches(chars, unit, index, length)) {
                return index;
            }
        }
        return -1;
    }
""", """        boolean wide = hasWideUnit(chars);
        if (m3() == null && isLatin1()) {
            byte[] bytes = value;
            for (int index = 0; index < length; index++) {
                char unit = (char) (bytes[index] & 0xFF);
                if (unit < 128 ? inMask(unit, low, high)
                        : wide && anyMatches(chars, unit, index, length)) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = 0; index < length; index++) {
            char unit = charAt(index);
            if (unit < 128) {
                if (inMask(unit, low, high)) return index;
            } else if (wide && anyMatches(chars, unit, index, length)) {
                return index;
            }
        }
        return -1;
    }
""", "indexOfAny Latin-1 scan")
search = replace(search, """        int[] wide = new String(chars).codePoints().filter(point -> point >= 128).toArray();
        for (int index = 0; index < length; ) {
""", """        int[] wide = new String(chars).codePoints().filter(point -> point >= 128).toArray();
        if (m3() == null && isLatin1()) {
            byte[] bytes = value;
            for (int index = 0; index < length; index++) {
                int point = bytes[index] & 0xFF;
                if (point < 128 ? !inMask((char) point, low, high) : !contains(wide, point)) {
                    return index;
                }
            }
            return -1;
        }
        for (int index = 0; index < length; ) {
""", "indexOfAnyBut Latin-1 scan")
search = replace(search, """    public boolean containsWhitespace() {
        int length = length();
        for (int index = 0; index < length; index++) {
""", """    public boolean containsWhitespace() {
        int length = length();
        if (m3() == null && isLatin1()) {
            byte[] bytes = value;
            for (int index = 0; index < length; index++) {
                if (Character.isWhitespace((char) (bytes[index] & 0xFF))) return true;
            }
            return false;
        }
        for (int index = 0; index < length; index++) {
""", "containsWhitespace Latin-1 scan")
search = replace(search, """        int end = length() - count + 1;
        for (int index = start; index < end; index++) {
            if (regionMatches(true, index, target, 0, count)) return index;
        }
        return -1;
    }
""", """        int end = length() - count + 1;
        char first = target.charAt(0);
        boolean ascii = first < 128;
        int index = ascii ? nextCaseCandidate(start, end, 1, first) : start;
        while (index < end) {
            if (regionMatches(true, index, target, 0, count)) return index;
            index = ascii ? nextCaseCandidate(index + 1, end, 1, first) : index + 1;
        }
        return -1;
    }
""", "indexOfIgnoreCase candidates")
search = replace(search, """        if (count == 0) return start;
        for (int index = start; index >= 0; index--) {
            if (regionMatches(true, index, target, 0, count)) return index;
        }
        return -1;
    }
""", """        if (count == 0) return start;
        char first = target.charAt(0);
        boolean ascii = first < 128;
        int index = ascii ? nextCaseCandidate(start, -1, -1, first) : start;
        while (index >= 0) {
            if (regionMatches(true, index, target, 0, count)) return index;
            index = ascii ? nextCaseCandidate(index - 1, -1, -1, first) : index - 1;
        }
        return -1;
    }

    /**
     * The first index from {@code from} towards {@code stop} (exclusive), moving by {@code step},
     * whose unit can start an ignoring-case region match of the ASCII unit {@code first}, else
     * {@code stop}. An ASCII unit can only by ASCII case; any other unit is judged by
     * {@link #sameUnitIgnoreCase(char, char)}.
     */
    private int nextCaseCandidate(int from, int stop, int step, char first) {
        char fold = asciiLower(first);
        if (m3() == null && isLatin1()) {
            byte[] bytes = value;
            for (int index = from; index != stop; index += step) {
                char unit = (char) (bytes[index] & 0xFF);
                if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                    return index;
                }
            }
            return stop;
        }
        for (int index = from; index != stop; index += step) {
            char unit = charAt(index);
            if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                return index;
            }
        }
        return stop;
    }

    /** {@code unit} with an ASCII upper-case letter turned into lower case. */
    private static char asciiLower(char unit) {
        return unit >= 'A' && unit <= 'Z' ? (char) (unit - 'A' + 'a') : unit;
    }

    /**
     * Whether two units can start an ignoring-case region match: {@code false} only when
     * {@link #regionMatches(boolean, int, String, int, int)} with {@code ignoreCase} refuses them
     * whatever units follow. A surrogate always passes; its comparison belongs to the region.
     */
    private static boolean sameUnitIgnoreCase(char a, char b) {
        if (a == b || Character.isSurrogate(a) || Character.isSurrogate(b)) return true;
        char upperA = Character.toUpperCase(a);
        char upperB = Character.toUpperCase(b);
        return upperA == upperB || Character.toLowerCase(upperA) == Character.toLowerCase(upperB);
    }
""", "lastIndexOfIgnoreCase candidates")
write(SEARCH_NEW, search)

edge = read(EDGE_OLD)
edge = replace(edge, """    public String normalizeSpace() {
        int length = length();
        StringBuilder out = new StringBuilder(length);
""", """    public String normalizeSpace() {
        int length = length();
        if (spaceNormalized(length)) return this;
        StringBuilder out = new StringBuilder(length);
""", "normalizeSpace unchanged check")
edge = replace(edge, """    /**
     * Returns this string without diacritical marks: the compatibility decomposition
""", """    /**
     * Whether normalizeSpace leaves this string as it is: no no-break space, whitespace only as
     * single spaces between other characters, and no character at most U+0020 at either end.
     */
    private boolean spaceNormalized(int length) {
        boolean previousSpace = true;
        for (int index = 0; index < length; index++) {
            char unit = charAt(index);
            if (unit == (char) 0x00A0) return false;
            boolean space = Character.isWhitespace(unit);
            if (space && (unit != ' ' || previousSpace)) return false;
            previousSpace = space;
        }
        return length == 0 || !previousSpace && charAt(0) > ' ' && charAt(length - 1) > ' ';
    }

    /**
     * Returns this string without diacritical marks: the compatibility decomposition
""", "spaceNormalized helper")
edge = replace(edge, """        if (index == length) return this;
        String decomposed = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFKD);
""", """        if (index == length) return this;
        if (latin1Units(index, length)) {
            String[] table = Latin1Accents.TABLE;
            StringBuilder out = new StringBuilder(length).append(this, 0, index);
            for (int at = index; at < length; at++) {
                out.append(table[charAt(at)]);
            }
            return out.toString();
        }
        return stripAccentsFull();
    }

    /** Whether every unit from {@code from} up to {@code length} is at most U+00FF. */
    private boolean latin1Units(int from, int length) {
        if (m3() == null && isLatin1()) return true;
        for (int index = from; index < length; index++) {
            if (charAt(index) > 0xFF) return false;
        }
        return true;
    }

    /**
     * stripAccents of each Latin-1 unit alone, built once from the full algorithm. A string of
     * Latin-1 units decomposes unit by unit into base characters and marks of U+0300..U+036F, and
     * no two of the remaining base characters compose, so its result is the units' results joined.
     */
    private static final class Latin1Accents {
        static final String[] TABLE = new String[256];
        static {
            for (int unit = 0; unit < 256; unit++) {
                TABLE[unit] = String.valueOf((char) unit).stripAccentsFull();
            }
        }
    }

    /** stripAccents of any string: NFKD, marks U+0300..U+036F removed, NFC, stroke letters. */
    private String stripAccentsFull() {
        String decomposed = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFKD);
""", "stripAccents Latin-1 table")
write(EDGE_NEW, edge)
print("speed edits written")
