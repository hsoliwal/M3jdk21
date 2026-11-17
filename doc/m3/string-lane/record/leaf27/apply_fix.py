# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""apply_fix.py <search.in> <edge.in> <search.out> <edge.out>: STRING_FAMILY_CONTRACT_FIX v1 (2026-11-17), found by
running commons-lang3 3.22.0's own tests (owner: "add test cases too"):
  1. SEARCH: a null char[] or array argument throws NullPointerException as documented, also on an empty string (the
     empty-receiver shortcut came first and skipped the argument); results are unchanged otherwise.
  2. EDGE: strip(String), stripStart(String), stripEnd(String) read this string as code points and look each code
     point up in the strip characters, as commons-lang3 3.22 does: a surrogate pair is removed only when the strip
     characters hold its code point (the old unit-by-unit loop cut the shared high half of another pair).
Every anchor must match exactly once; the recipe refuses otherwise."""
import io
import sys

SEARCH_IN, EDGE_IN, SEARCH_OUT, EDGE_OUT = sys.argv[1:5]


def read(path):
    with io.open(path, encoding="utf-8", newline="") as stream:
        return stream.read()


def write(path, text):
    with io.open(path, "w", encoding="utf-8", newline="") as stream:
        stream.write(text)


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit(f"REFUSE {label}: anchor found {text.count(old)} times")
    return text.replace(old, new)


search = read(SEARCH_IN)
search = replace(search, "        if (isEmpty() || searches.length == 0) return false;\n",
                 "        if (searches.length == 0 || isEmpty()) return false;\n", "containsAny/AnyIgnoreCase", 2)
search = replace(search, "        if (isEmpty() || prefixes.length == 0) return false;\n",
                 "        if (prefixes.length == 0 || isEmpty()) return false;\n", "startsWithAny")
search = replace(search, "        if (isEmpty() || suffixes.length == 0) return false;\n",
                 "        if (suffixes.length == 0 || isEmpty()) return false;\n", "endsWithAny")
search = replace(search, "        if (length == 0 || chars.length == 0) return -1;\n",
                 "        if (chars.length == 0 || length == 0) return -1;\n", "indexOfAny/indexOfAnyBut(char...)", 2)
search = replace(search, """    public boolean containsOnly(char... chars) {
        if (isEmpty()) return true;
""", """    public boolean containsOnly(char... chars) {
        java.util.Objects.requireNonNull(chars, "chars");
        if (isEmpty()) return true;
""", "containsOnly(char...)")
write(SEARCH_OUT, search)

edge = read(EDGE_IN)
for where in ("at its start and its end.\n", "at its start.\n", "at its end.\n"):
    edge = replace(edge, "     * Returns this string without the given characters " + where,
                   "     * Returns this string without the given characters " + where
                   + "     * This string is read as code points, and a code point is removed when {@code chars}\n"
                   + "     * contains it as {@link #indexOf(int)} finds it: a surrogate pair goes only when\n"
                   + "     * {@code chars} holds that pair.\n", "strip doc " + where.strip())
edge = replace(edge, """        if (length < 2) return "";
        boolean crlf = charAt(length - 1) == '\\n' && charAt(length - 2) == '\\r';
        return substring(0, crlf ? length - 2 : length - 1);
""", """        if (length < 2) return "";
        boolean pair = Character.isLowSurrogate(charAt(length - 1))
                && Character.isHighSurrogate(charAt(length - 2));
        boolean crlf = charAt(length - 1) == '\\n' && charAt(length - 2) == '\\r';
        return substring(0, pair || crlf ? length - 2 : length - 1);
""", "chop keeps surrogate pairs whole")
edge = replace(edge, "     * Returns this string without its last character; a final {@code \"\\r\\n\"} is removed as one.\n",
               "     * Returns this string without its last character; a final {@code \"\\r\\n\"} is removed as one,\n"
               "     * and so is a final surrogate pair.\n", "chop doc")
edge = replace(edge, """    public String stripStart(String chars) {
        int length = length();
        int start = 0;
        if (!chars.isEmpty()) {
            while (start < length && chars.indexOf(charAt(start)) >= 0) {
                start++;
            }
        }
        return substring(start);
    }
""", """    public String stripStart(String chars) {
        int length = length();
        int start = 0;
        if (!chars.isEmpty()) {
            while (start < length) {
                int point = codePointAt(start);
                if (chars.indexOf(point) < 0) break;
                start += Character.charCount(point);
            }
        }
        return substring(start);
    }
""", "stripStart by code point")
edge = replace(edge, """    public String stripEnd(String chars) {
        int end = length();
        if (!chars.isEmpty()) {
            while (end > 0 && chars.indexOf(charAt(end - 1)) >= 0) {
                end--;
            }
        }
        return substring(0, end);
    }
""", """    public String stripEnd(String chars) {
        int end = length();
        if (!chars.isEmpty()) {
            while (end > 0) {
                int point = codePointBefore(end);
                if (chars.indexOf(point) < 0) break;
                end -= Character.charCount(point);
            }
        }
        return substring(0, end);
    }
""", "stripEnd by code point")
write(EDGE_OUT, edge)
print("contract fixes written")
