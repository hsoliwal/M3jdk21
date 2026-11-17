# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""donor_upstream_fixes.py <String.java in> <String.java out>: DONOR_UPSTREAM_FIXES v1 (2026-11-17).
Ports the donors' own fixes made after the releases the families were proven against (owner: "check upstream for
fixes and new code"):
  1. commons-lang master 1a3afb09d5 "Fix premature overflow in StringUtils.repeat with separators":
     repeat(String separator, int count) rejects only a result longer than Integer.MAX_VALUE, counted exactly as
     length * count + separator.length * (count - 1) (3.22.0 rejected (length + separator.length) * count).
  2. commons-lang master a5ff3be60e "Fix StringUtils capacity and length overflows" (unwrap part):
     unwrap(String) compares wrapToken.length() > length() / 2; 2 * wrapToken.length() overflowed for a token of
     2^30 or more chars, and unwrap(token, token) then threw instead of returning the string.
  3. commons-text master 63f2013412 "Use locale-independent lowercasing in case conversion helpers":
     capitalizeFully(), capitalizeFully(char...) and toCamelCase(boolean, char...) lower-case with Locale.ROOT, so
     a Turkish default locale no longer leaks a dotless i into the result.
Every anchor must match the stated number of times; the recipe refuses otherwise."""
import io
import sys

SRC, DST = sys.argv[1:3]


def replace(text, old, new, label, count=1):
    if text.count(old) != count:
        sys.exit("REFUSE %s: anchor found %d times, expected %d" % (label, text.count(old), count))
    return text.replace(old, new)


with io.open(SRC, encoding="utf-8", newline="") as stream:
    s = stream.read()

s = replace(s, """     * @throws  IllegalArgumentException if {@code count} is greater than one and {@code count}
     *          times the length of this string and the separator together is greater than
     *          {@link Integer#MAX_VALUE}.
""", """     * @throws  IllegalArgumentException if {@code count} is greater than one and the result,
     *          {@code count} times this string and {@code count - 1} times the separator, would be
     *          longer than {@link Integer#MAX_VALUE}.
""", "repeat doc")
s = replace(s, """        if (((long) length() + separator.length()) * count > Integer.MAX_VALUE) {
""", """        if ((long) length() * count + (long) separator.length() * (count - 1)
                > Integer.MAX_VALUE) {
""", "repeat exact length")
s = replace(s, """        if (isEmpty() || wrapToken.isEmpty() || length() < 2 * wrapToken.length()) return this;
""", """        if (isEmpty() || wrapToken.isEmpty() || wrapToken.length() > length() / 2) return this;
""", "unwrap without overflow")
for doc, label in (("""     * Returns this string converted to lower case as {@link #toLowerCase()} does (the default
     * locale), then with the first character of each word changed to title case as
""", "capitalizeFully docs"),):
    s = replace(s, doc, """     * Returns this string converted to lower case in {@link java.util.Locale#ROOT} (as
     * {@link #toLowerCase(java.util.Locale)} does), then with the first character of each word
     * changed to title case as
""", label, 2)
s = replace(s, """     * Returns this string in camel case: converted to lower case as {@link #toLowerCase()} does
     * (the default locale), with the space and the given delimiters removed, and the first
""", """     * Returns this string in camel case: converted to lower case in
     * {@link java.util.Locale#ROOT}, with the space and the given delimiters removed, and the first
""", "toCamelCase doc")
s = replace(s, "        return toLowerCase().mapCodePoints(MAP_CAPITALIZE, ",
            "        return toLowerCase(java.util.Locale.ROOT).mapCodePoints(MAP_CAPITALIZE, ",
            "capitalizeFully Locale.ROOT", 2)
s = replace(s, "        return toLowerCase().mapCodePoints(MAP_CAMEL, delimiters, capitalizeFirstLetter);\n",
            "        return toLowerCase(java.util.Locale.ROOT).mapCodePoints(MAP_CAMEL, delimiters,\n"
            "                capitalizeFirstLetter);\n", "toCamelCase Locale.ROOT")

with io.open(DST, "w", encoding="utf-8", newline="") as stream:
    stream.write(s)
print("DONOR_UPSTREAM_FIXES v1 applied")
