# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""value_site_guards.py <String.java> <out.tsv>

VALUE_SITE_GUARDS v1 (2026-11-16). CPU only, lexical. For every `value()` call site in java.lang.String it reports
the enclosing method, the receiver of the call (this or another String) and the M3 guard that stands in front of
the site, so the sites that can still flatten an M3-backed String can be listed. Classes:
  FLAT_TERNARY   the site is the null arm of a `storage != null ? ... : ...` or `storage == null ? ... : ...`
  FLAT_RETURN    an `if (<guard> != null) { ... return ...; }` on one guard variable closed before the site
  MIXED_GUARD    the closed guard tests two variables with && (both sides M3); a mixed pair still reaches the site
  OTHER_RECEIVER the call is `<x>.value()` on another String; its own guard is not visible lexically
  NO_GUARD       no M3 guard variable is tested in the method before the site
  ACCESSOR       the declaration of value() itself
Every row is a proposal for a reader with the source open; it is not a proof of reachability."""
import re
import sys

METHOD = re.compile(r"^    (?:public|private|protected|static|final|native|synchronized|[A-Za-z<])[^;=]*\(")
NOT_METHOD = re.compile(r"^    (?:return|throw|if|for|while|else|new)\b")
NAME = re.compile(r"([A-Za-z0-9_$]+)\s*\($")
SITE = re.compile(r"(?:([A-Za-z0-9_]+)\.)?value\(\)")
GUARD_VAR = re.compile(r"M3String\s+(\w+)\s*=\s*(?:(\w+)\.)?m3(?:\(\))?\s*;")
IF_GUARD = re.compile(r"^(\s*)(?:\} else )?if \((.*)\) \{\s*$")


def method_name(line):
    head = line.split("(", 1)[0] + "("
    found = NAME.search(head)
    return found.group(1) if found else "?"


def methods(lines):
    starts = [i for i, line in enumerate(lines) if METHOD.match(line) and not NOT_METHOD.match(line)]
    return [(start, end) for start, end in zip(starts, starts[1:] + [len(lines)])]


def closed_guards(lines, start, site):
    """Guard blocks `if (cond) {` that test an M3 variable against null and close with a return before the site."""
    found = []
    for i in range(start, site):
        match = IF_GUARD.match(lines[i])
        if not match or "!= null" not in match.group(2):
            continue
        close = next((j for j in range(i + 1, site) if lines[j].rstrip() == match.group(1) + "}"), None)
        body = " ".join(lines[i + 1:close]) if close else ""
        if close and ("return" in lines[close - 1] or body.rstrip().endswith(";") and "return" in body):
            found.append((i, match.group(2)))
    return found


def classify(lines, start, site, guards):
    line = lines[site]
    receiver = SITE.search(line).group(1)
    if re.match(r"^    (?:final )?byte\[\] value\(\)", line):
        return "ACCESSOR", "this", ""
    if receiver and receiver != "this":
        return "OTHER_RECEIVER", receiver, ""
    window = " ".join(lines[max(start, site - 2):site + 1])
    if re.search(r"\b(\w+) (?:!=|==) null\s*\?", window) and any(g in window for g in guards):
        return "FLAT_TERNARY", "this", ""
    closed = [(i, cond) for i, cond in closed_guards(lines, start, site) if any(g in cond for g in guards)]
    if not closed:
        return "NO_GUARD", "this", ""
    index, cond = closed[-1]
    return ("MIXED_GUARD" if "&&" in cond else "FLAT_RETURN"), "this", f"{index + 1}: if ({cond})"


def main(argv):
    lines = open(argv[1], encoding="utf-8", newline="").read().split("\n")
    rows = []
    for start, end in methods(lines):
        guards = {m.group(1) for line in lines[start:end] for m in GUARD_VAR.finditer(line)}
        for site in range(start, end):
            if SITE.search(lines[site]) and not lines[site].lstrip().startswith(("*", "//")):
                kind, receiver, guard = classify(lines, start, site, guards)
                rows.append((method_name(lines[start]), str(site + 1), receiver, kind, guard))
    with open(argv[2], "w", encoding="utf-8", newline="") as out:
        out.write("string_method\tline\treceiver\tclass\tguard\n")
        out.writelines("\t".join(row) + "\n" for row in rows)
    counts = {}
    for row in rows:
        counts[row[3]] = counts.get(row[3], 0) + 1
    print(len(rows), sorted(counts.items(), key=lambda item: -item[1]))


if __name__ == "__main__":
    main(sys.argv)
