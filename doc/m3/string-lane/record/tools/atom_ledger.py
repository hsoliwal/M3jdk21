# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""atom_ledger.py <head-String.java> <provenance.tsv> <out-dir> [<atom-hashes.tsv>] -- <family>=<text-file> ...

STRING_ATOM_LEDGER v1 (2026-11-17; owner: "you must atomize and patternize everything"). CPU only. Every method (and
holder class) of the given family method texts is one atom:
  family, member, kind (public/private, static), signature, line range in the head String.java, raw SHA-256 of the
  member text (its doc comment included), donor provenance (provenance.tsv: member name -> donor API; members not
  listed are M3JDK helpers), the patterns its body matches (PATTERNS below, versioned; none -> UNKNOWN_PATTERN), and,
  when an atomize-kit ATOM_HASHES.tsv of the head file is given, the atom/logic/structure hashes of that symbol.
Writes ATOMS.tsv and PATTERNS.tsv (the catalogue with the number of atoms per pattern)."""
import hashlib
import io
import re
import sys

PATTERNS = [
    ("P-NULLSAFE-STATIC", 1, "static member answering null arguments before delegating", r"\bstatic\b[^{]*\{[\s\S]*?== null"),
    ("P-LATIN1-BYTE-SCAN", 1, "flat Latin-1 storage read on its value bytes", r"isLatin1\(\)[\s\S]*?(bytes\[|value\b)"),
    ("P-WINDOW-SCAN", 1, "UTF-16/M3 storage read in bulk windows of SCAN_WINDOW units via getChars", r"SCAN_WINDOW[\s\S]*?getChars\("),
    ("P-M3-FACTS-GATE", 1, "prepared M3StringFacts answer without reading the text", r"factsIfPrepared\(\)|M3StringFacts facts"),
    ("P-SLICE-RESULT", 1, "result is a range of a string (an M3 slice, no copy, on M3 storage)", r"return[^;]*\bsubstring\("),
    ("P-SAME-STRING-RESULT", 1, "unchanged result returns this string (no allocation)", r"return this;"),
    ("P-CASE-UNIT-RULE", 1, "regionMatches' one-unit case rule: equal, same upper case, or same lower case of it",
     r"toUpperCase\([\s\S]*?toLowerCase\("),
    ("P-SURROGATE-GUARD", 1, "never splits a surrogate pair (moves back onto or around a pair)", r"is(High|Low)Surrogate"),
    ("P-TABLE-HOLDER", 1, "table built once in a private static holder class from the full algorithm",
     r"private static final class \w+ \{[\s\S]*?static final"),
    ("P-CODE-POINT-WALK", 1, "text walked as code points (Character.charCount/codePointAt)", r"charCount\(|codePointAt\("),
    ("P-DELEGATE", 1, "one-statement member delegating to another member", r"^\s*\{\s*return [^;]+;\s*\}\s*$"),
    ("P-NORMALIZER", 1, "Unicode normalization through java.text.Normalizer", r"java\.text\.Normalizer"),
    ("P-ASCII-FOLD", 1, "ASCII-only case folding (asciiLower)", r"asciiLower\("),
    ("P-NULL-SKIP-VARARGS", 1, "loop over a varargs/array argument that skips or answers null elements",
     r"for \((?:final )?(?:CharSequence|String|char) \w+ : \w+\)[\s\S]*?(== null|!= null)"),
    ("P-ASCII-MASK", 1, "character set as a two-word ASCII bit mask plus a list of wider units",
     r"asciiMask\(|inMask\("),
    ("P-INDEXOF-HOP", 1, "occurrences found by indexOf/lastIndexOf hops", r"(indexOf|lastIndexOf)\([^;]*[+-] *1|for \([^;]*indexOf\("),
    ("P-REGION-MATCH", 1, "comparison through regionMatches (case rule included)", r"regionMatches\("),
    ("P-CHAR-PREDICATE-SCAN", 1, "units or code points tested with a Character predicate",
     r"Character\.is(Whitespace|Letter|Digit|LetterOrDigit|LowerCase|UpperCase)\("),
    ("P-BUILDER-RESULT", 1, "result assembled in a StringBuilder", r"new StringBuilder\("),
    ("P-FLAT-MISMATCH", 1, "flat arrays compared with the vectorized ArraysSupport.mismatch", r"ArraysSupport\.mismatch"),
    ("P-EDGE-SCAN", 1, "scan from one or both ends of the text (strip/trim/chomp shape)",
     r"while \(\w+ < \w+ && |while \(\w+ > \w+ && |charAt\(length - 1\)|length\(\) - 1"),
    ("P-UNIT-LIST-SCAN", 1, "linear scan of a small unit or code point list (character sets, columns)",
     r"for \((?:char|int) \w+ : \w+\)|for \(int k = 1; k < \w+\.length"),
    ("P-UNIT-MAP", 1, "fixed unit-to-unit mapping as a switch", r"switch \(unit\)"),
    ("P-NULL-SAFE-PREDICATE", 1, "static null-safe predicate or default over a value",
     r"\bstatic\b[^{]*\{[\s\S]*?(== null|isNullOrEmpty|blank\()"),
]


def members(text):
    """(name, kind, signature, member text with its doc comment) of each top-level member of a method text."""
    out = []
    for m in re.finditer(r"^    (?=\S)((?:public |private |protected )?(?:static )?(?:final )?(?:synchronized )?"
                         r"(?:class \w+|[\w<>\[\], .?]+? (\w+)\s*\([^)]*\)[^{;]*))\s*\{", text, re.M):
        head = m.group(1)
        name = m.group(2) or re.search(r"class (\w+)", head).group(1)
        start = m.start()
        doc = text.rfind("    /**", 0, start)
        if doc >= 0 and text[doc:start].count("*/") == 1 and text[text.index("*/", doc) + 2:start].strip() == "":
            start = doc
        depth, i = 0, text.index("{", m.end() - 1)
        while True:
            c = text[i]
            if c == '"':
                i += 1
                while text[i] != '"':
                    i += 2 if text[i] == "\\" else 1
            elif c == "'":
                i += 3 if text[i + 1] != "\\" else 4
                continue
            elif c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        kind = ("static " if " static " in " " + head else "") + (
            "public" if head.startswith("public") else "private" if head.startswith("private") else "package")
        out.append((name, kind.strip(), " ".join(head.split()), text[start:i + 1], text[m.end() - 1:i + 1]))
    return out


def main():
    args = sys.argv[1:]
    split = args.index("--")
    head_file, prov_file, out = args[0], args[1], args[2]
    hashes_file = args[3] if split > 3 else None
    head = io.open(head_file, encoding="utf-8", newline="").read()
    prov = {}
    for line in io.open(prov_file, encoding="utf-8"):
        if line.strip() and not line.startswith("#"):
            name, donor = line.rstrip("\n").split("\t")[:2]
            prov[name] = donor
    planes = {}
    if hashes_file:
        for line in io.open(hashes_file, encoding="utf-8").read().split("\n")[1:]:
            f = line.split("\t")
            if len(f) > 5 and f[1] in ("METHOD", "IMPLEMENTATION", "TYPE"):
                planes.setdefault(f[2], (f[3], f[4], f[5]))
    counts = {p[0]: 0 for p in PATTERNS}
    rows = []
    for spec in args[split + 1:]:
        family, path = spec.split("=", 1)
        text = io.open(path, encoding="utf-8", newline="").read()
        base = head.index(text) if text in head else -1
        for name, kind, signature, member, body in members(text):
            offset = head.index(member, base) if base >= 0 and member in head[base:] else head.find(member)
            first = head.count("\n", 0, offset) + 1 if offset >= 0 else 0
            last = first + member.count("\n") if offset >= 0 else 0
            matched = [p[0] for p in PATTERNS if re.search(p[3], member if p[0] != "P-DELEGATE" else body, re.M)]
            for p in matched:
                counts[p] += 1
            a, l, s = planes.get(name, ("-", "-", "-"))
            rows.append([family, name, kind, signature, f"{first}-{last}",
                         hashlib.sha256(member.encode("utf-8")).hexdigest(), prov.get(name, "M3JDK helper"),
                         ",".join(matched) or "UNKNOWN_PATTERN", a, l, s])
    with io.open(out + "/ATOMS.tsv", "w", encoding="utf-8", newline="") as w:
        w.write("family\tmember\tkind\tsignature\thead_lines\tsha256\tprovenance\tpatterns\tatom_sha256\t"
                "logic_sha256\tstructure_sha256\n")
        for r in rows:
            w.write("\t".join(r) + "\n")
    with io.open(out + "/PATTERNS.tsv", "w", encoding="utf-8", newline="") as w:
        w.write("pattern\tversion\tmeaning\tatoms\n")
        for pid, version, meaning, _ in PATTERNS:
            w.write(f"{pid}\tv{version}\t{meaning}\t{counts[pid]}\n")
        w.write(f"UNKNOWN_PATTERN\t-\tmatched no pattern\t{sum(1 for r in rows if r[7] == 'UNKNOWN_PATTERN')}\n")
    print(f"atoms {len(rows)}; unknown pattern {sum(1 for r in rows if r[7] == 'UNKNOWN_PATTERN')}")


main()
