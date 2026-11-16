# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""absorb_catalogue.py <donor.java> <donor-label> <String.java> <out.tsv>

ABSORB_CATALOGUE v1 (2026-11-16). CPU only, lexical. One row per public static method name of a donor utility class:
overload count, source lines, how often its bodies flatten a String (toCharArray) or read it per unit (charAt), whether
java.lang.String already has a public method of that name, and a family assigned from the name. The family and the
proposed route are naming rules, so every row is a proposal for a reader with the source open:
  DELEGATE   java.lang.String has a public method of the same name: the library can call it, nothing to absorb
  ABSORB     no such String method: the algorithm needs an owner in java.base (clean-room, with the library as the
             oracle in tests); a public entry point needs the owner's admission
"""
import collections
import re
import sys

donor_path, label, string_path, out_path = sys.argv[1:5]
DECL = re.compile(r"^ {2,4}public static (?:<[^>]+> )?[\w.<>\[\], ?]+? (\w+)\(")
STRING_DECL = re.compile(r"^    public (?:static )?(?:<[^>]+> )?[\w.<>\[\], ?]+? (\w+)\(")
FAMILIES = [
    ("CLASS_TEST", r"^is(Alpha|Numeric|Whitespace|AsciiPrintable|AllLowerCase|AllUpperCase|MixedCase)"),
    ("EMPTY_BLANK", r"^(is|isNot|isAny|isNone|isAll|first|default|getIf)\w*(Empty|Blank)|^defaultString|^length$"),
    ("SEARCH", r"^(contains|indexOf|lastIndexOf|ordinalIndexOf|lastOrdinalIndexOf|countMatches|startsWith|endsWith)"),
    ("EDGE", r"^(strip|trim|chomp|chop|deleteWhitespace|normalizeSpace)"),
    ("SLICE", r"^(substring|left$|right$|mid$|truncate|abbreviate|remove|overlay|unwrap)"),
    ("SPLIT_JOIN", r"^(split|join)"),
    ("REPLACE", r"^replace"),
    ("CASE", r"^(capitalize|uncapitalize|swapCase|upperCase|lowerCase|toRootLowerCase|toRootUpperCase)"),
    ("COMPARE", r"^(equals|compare|getCommonPrefix|indexOfDifference|difference)"),
    ("DISTANCE", r"^(getLevenshteinDistance|getJaroWinklerDistance|getFuzzyDistance)"),
    ("BUILD", r"^(repeat|leftPad|rightPad|center|wrap|appendIfMissing|prependIfMissing|rotate|reverse)"),
]


def family(name):
    return next((fam for fam, rx in FAMILIES if re.search(rx, name)), "OTHER")


def read(path):
    return open(path, encoding="utf-8", newline="").read().replace("\r", "").split("\n")


string_names = {m.group(1) for line in read(string_path) if (m := STRING_DECL.match(line))}
lines = read(donor_path)
starts = [(i, m.group(1)) for i, line in enumerate(lines) if (m := DECL.match(line))]
stats = collections.defaultdict(lambda: [0, 0, 0, 0])
for (begin, name), (end, _) in zip(starts, starts[1:] + [(len(lines), "")]):
    body = "\n".join(lines[begin:end])
    row = stats[name]
    row[0] += 1
    row[1] += end - begin
    row[2] += body.count(".toCharArray()")
    row[3] += body.count(".charAt(")
rows = sorted((family(name), name, *values, "yes" if name in string_names else "no",
               "DELEGATE" if name in string_names else "ABSORB") for name, values in stats.items())
with open(out_path, "w", encoding="utf-8", newline="") as out:
    out.write("donor\tfamily\toperation\toverloads\tsource_lines\ttoCharArray\tcharAt\tsame_name_in_String\tproposed_route\n")
    out.writelines(label + "\t" + "\t".join(str(v) for v in row) + "\n" for row in rows)
by_route = collections.Counter(row[-1] for row in rows)
by_family = collections.Counter((row[0], row[-1]) for row in rows)
print(label, "operations", len(rows), dict(by_route))
print(" ".join(f"{fam}:{route[0]}={n}" for (fam, route), n in sorted(by_family.items())))
