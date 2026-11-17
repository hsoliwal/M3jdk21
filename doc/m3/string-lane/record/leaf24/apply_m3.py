# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""apply_m3.py <search.in> <edge.in> <search.out> <edge.out>: STRING_FAMILY_M3_PATHS v1 (owner, 2026-11-17: "M3String is
so important to this"). The SEARCH and EDGE families read flat strings on their bytes since #3218, but M3 storage still
went unit by unit through charAt. This recipe gives their hot methods an M3 path: prepared M3 facts answer without a
scan where they decide (unit signals, ASCII regex classes, strip bounds, ascii/latin1), and otherwise the text is read
in bulk windows of SCAN_WINDOW units through getChars, a surrogate pair never split between two windows. Results do not
change. Every anchor must match exactly once; the recipe refuses otherwise."""
import io
import sys

SEARCH_IN, EDGE_IN, SEARCH_OUT, EDGE_OUT = sys.argv[1:5]


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


search = read(SEARCH_IN)
search = replace(search, """        boolean wide = hasWideUnit(chars);
        if (m3() == null && isLatin1()) {
""", """        boolean wide = hasWideUnit(chars);
        M3String storage = m3();
        if (storage != null) {
            M3StringFacts facts = storage.factsIfPrepared();
            if (facts != null && noUnitMayOccur(facts, chars)) return -1;
            return windowIndexOfAny(chars, low, high, wide);
        }
        if (isLatin1()) {
""", "indexOfAny M3 path")
search = replace(search, """        int[] wide = new String(chars).codePoints().filter(point -> point >= 128).toArray();
        if (m3() == null && isLatin1()) {
""", """        int[] wide = new String(chars).codePoints().filter(point -> point >= 128).toArray();
        if (m3() != null) return windowIndexOfAnyBut(low, high, wide);
        if (isLatin1()) {
""", "indexOfAnyBut M3 path")
search = replace(search, """    public boolean containsWhitespace() {
        int length = length();
        if (m3() == null && isLatin1()) {
""", """    public boolean containsWhitespace() {
        int length = length();
        M3String storage = m3();
        if (storage != null) {
            M3StringFacts facts = storage.factsIfPrepared();
            if (facts != null && (facts.regexAnyClasses & M3StringFacts.REGEX_SPACE) != 0) {
                return true;
            }
            return windowContainsWhitespace(length);
        }
        if (isLatin1()) {
""", "containsWhitespace M3 path")
search = replace(search, """        char first = target.charAt(0);
        boolean ascii = first < 128;
        int index = ascii ? nextCaseCandidate(start, end, 1, first) : start;
""", """        char first = target.charAt(0);
        boolean ascii = first < 128;
        if (ascii && noCaseCandidate(first)) return -1;
        int index = ascii ? nextCaseCandidate(start, end, 1, first) : start;
""", "indexOfIgnoreCase facts gate")
search = replace(search, """        char first = target.charAt(0);
        boolean ascii = first < 128;
        int index = ascii ? nextCaseCandidate(start, -1, -1, first) : start;
""", """        char first = target.charAt(0);
        boolean ascii = first < 128;
        if (ascii && noCaseCandidate(first)) return -1;
        int index = ascii ? nextCaseCandidate(start, -1, -1, first) : start;
""", "lastIndexOfIgnoreCase facts gate")
search = replace(search, """    private int nextCaseCandidate(int from, int stop, int step, char first) {
        char fold = asciiLower(first);
        if (m3() == null && isLatin1()) {
""", """    private int nextCaseCandidate(int from, int stop, int step, char first) {
        char fold = asciiLower(first);
        if (m3() != null) return windowCaseCandidate(from, stop, step, first, fold);
        if (isLatin1()) {
""", "nextCaseCandidate M3 path")
search = replace(search, """    /** {@code unit} with an ASCII upper-case letter turned into lower case. */
""", """    /**
     * {@link #nextCaseCandidate(int, int, int, char)} over M3 storage: the units from
     * {@code from} towards {@code stop} are read in bulk windows in the direction of the scan.
     */
    private int windowCaseCandidate(int from, int stop, int step, char first, char fold) {
        char[] window = new char[Math.min(Math.abs(stop - from), SCAN_WINDOW)];
        for (int index = from; index != stop; ) {
            int count = Math.min(window.length, Math.abs(stop - index));
            int begin = step > 0 ? index : index - count + 1;
            getChars(begin, begin + count, window, 0);
            for (int k = 0; k < count; k++) {
                int at = step > 0 ? k : count - 1 - k;
                char unit = window[at];
                if (unit < 128 ? asciiLower(unit) == fold : sameUnitIgnoreCase(unit, first)) {
                    return begin + at;
                }
            }
            index += step * count;
        }
        return stop;
    }

    /**
     * Whether prepared M3 facts rule out every start of an ignoring-case match of the ASCII unit
     * {@code first}: in Latin-1 text only its ASCII lower- and upper-case forms can match it.
     */
    private boolean noCaseCandidate(char first) {
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts == null || !facts.latin1) return false;
        char lower = asciiLower(first);
        char upper = lower >= 'a' && lower <= 'z' ? (char) (lower - 'a' + 'A') : lower;
        return !facts.mayContainCodeUnit(lower) && !facts.mayContainCodeUnit(upper);
    }

    /** {@code unit} with an ASCII upper-case letter turned into lower case. */
""", "case-candidate window and facts helpers")
search = replace(search, """    private static boolean sameUnitIgnoreCase(char a, char b) {
        if (a == b || Character.isSurrogate(a) || Character.isSurrogate(b)) return true;
        char upperA = Character.toUpperCase(a);
        char upperB = Character.toUpperCase(b);
        return upperA == upperB || Character.toLowerCase(upperA) == Character.toLowerCase(upperB);
    }
""", """    private static boolean sameUnitIgnoreCase(char a, char b) {
        if (a == b || Character.isSurrogate(a) || Character.isSurrogate(b)) return true;
        char upperA = Character.toUpperCase(a);
        char upperB = Character.toUpperCase(b);
        return upperA == upperB || Character.toLowerCase(upperA) == Character.toLowerCase(upperB);
    }

    /** Whether prepared M3 facts rule out every unit of {@code units}. */
    private static boolean noUnitMayOccur(M3StringFacts facts, char[] units) {
        for (char unit : units) {
            if (facts.mayContainCodeUnit(unit)) return false;
        }
        return true;
    }

    /** {@link #indexOfAny(char...)} over M3 storage, read in bulk windows. */
    private int windowIndexOfAny(char[] chars, long low, long high, boolean wide) {
        int length = length();
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from; at++) {
                char unit = window[at];
                if (unit < 128 ? inMask(unit, low, high)
                        : wide && anyMatches(chars, unit, from + at, length)) {
                    return from + at;
                }
            }
        }
        return -1;
    }

    /**
     * {@link #indexOfAnyBut(char...)} over M3 storage: code points read in bulk windows, a
     * surrogate pair never split between two windows.
     */
    private int windowIndexOfAnyBut(long low, long high, int[] wide) {
        int length = length();
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; ) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            int count = to - from;
            if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;
            for (int at = 0; at < count; ) {
                int point = Character.codePointAt(window, at, count);
                if (point < 128 ? !inMask((char) point, low, high) : !contains(wide, point)) {
                    return from + at;
                }
                at += Character.charCount(point);
            }
            from += count;
        }
        return -1;
    }

    /** {@link #containsWhitespace()} over M3 storage, read in bulk windows. */
    private boolean windowContainsWhitespace(int length) {
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        for (int from = 0; from < length; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from; at++) {
                if (Character.isWhitespace(window[at])) return true;
            }
        }
        return false;
    }
""", "search window helpers")
write(SEARCH_OUT, search)

edge = read(EDGE_IN)
edge = replace(edge, """    private boolean spaceNormalized(int length) {
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
""", """    private boolean spaceNormalized(int length) {
        if (length == 0) return true;
        if (charAt(0) <= ' ' || charAt(length - 1) <= ' ') return false;
        M3String storage = m3();
        if (storage != null) {
            M3StringFacts facts = storage.factsIfPrepared();
            if (facts != null && (facts.stripStart != 0 || facts.stripEnd != length)) return false;
            return windowSpaceNormalized(length);
        }
        int previous = 1;
        for (int index = 0; index < length && previous >= 0; index++) {
            previous = spaceStep(charAt(index), previous);
        }
        return previous == 0;
    }

    /** {@link #spaceNormalized(int)} over M3 storage, read in bulk windows. */
    private boolean windowSpaceNormalized(int length) {
        char[] window = new char[Math.min(length, SCAN_WINDOW)];
        int previous = 1;
        for (int from = 0; from < length && previous >= 0; from += window.length) {
            int to = Math.min(length, from + window.length);
            getChars(from, to, window, 0);
            for (int at = 0; at < to - from && previous >= 0; at++) {
                previous = spaceStep(window[at], previous);
            }
        }
        return previous == 0;
    }

    /**
     * One unit of {@link #spaceNormalized(int)}: {@code 1} after a space, {@code 0} after another
     * unit, {@code -1} when the string is not normalized (a no-break space, other whitespace, or
     * a space after whitespace). {@code previous} is {@code 1} at the start.
     */
    private static int spaceStep(char unit, int previous) {
        if (unit == (char) 0x00A0) return -1;
        boolean space = Character.isWhitespace(unit);
        if (space && (unit != ' ' || previous == 1)) return -1;
        return space ? 1 : 0;
    }
""", "spaceNormalized M3 path")
edge = replace(edge, """    public String stripAccents() {
        int length = length();
        int index = 0;
""", """    public String stripAccents() {
        int length = length();
        M3String storage = m3();
        M3StringFacts facts = storage == null ? null : storage.factsIfPrepared();
        if (facts != null && facts.ascii) return this;
        int index = 0;
""", "stripAccents ascii facts")
edge = replace(edge, """        if (latin1Units(index, length)) {
            String[] table = Latin1Accents.TABLE;
            StringBuilder out = new StringBuilder(length).append(this, 0, index);
            for (int at = index; at < length; at++) {
                out.append(table[charAt(at)]);
            }
            return out.toString();
        }
""", """        if (facts != null ? facts.latin1 : latin1Units(index, length)) {
            String[] table = Latin1Accents.TABLE;
            StringBuilder out = new StringBuilder(length).append(this, 0, index);
            if (storage == null) {
                for (int at = index; at < length; at++) {
                    out.append(table[charAt(at)]);
                }
                return out.toString();
            }
            char[] window = new char[Math.min(length - index, SCAN_WINDOW)];
            for (int from = index; from < length; from += window.length) {
                int to = Math.min(length, from + window.length);
                getChars(from, to, window, 0);
                for (int at = 0; at < to - from; at++) {
                    out.append(table[window[at]]);
                }
            }
            return out.toString();
        }
""", "stripAccents table windows")
edge = replace(edge, """    private boolean latin1Units(int from, int length) {
        if (m3() == null && isLatin1()) return true;
        for (int index = from; index < length; index++) {
            if (charAt(index) > 0xFF) return false;
        }
        return true;
    }
""", """    private boolean latin1Units(int from, int length) {
        if (m3() == null) {
            if (isLatin1()) return true;
            for (int index = from; index < length; index++) {
                if (charAt(index) > 0xFF) return false;
            }
            return true;
        }
        char[] window = new char[Math.min(length - from, SCAN_WINDOW)];
        for (int start = from; start < length; start += window.length) {
            int to = Math.min(length, start + window.length);
            getChars(start, to, window, 0);
            for (int at = 0; at < to - start; at++) {
                if (window[at] > 0xFF) return false;
            }
        }
        return true;
    }
""", "latin1Units M3 windows")
write(EDGE_OUT, edge)
print("M3 path edits written")
