# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""gen_markup_tables.py <entities.json> <oracle-dir> <template> <out-family-text>

ESCAPE_MARKUP_TABLES v1 (2026-11-17). CPU only, no regular expressions. Builds the two named-reference table texts of
the ESCAPE_MARKUP family and writes the family text: <template> with the lines '@HTML5_TABLE@' and '@HTML4_TABLE@'
replaced by Java string-literal lines.

Checks before writing (any failure stops with a message):
  1. entities.json is the WHATWG file of sha256 ENTITIES_SHA256 (2231 names, 106 without ';').
  2. CPython 3.12 html.entities.html5 holds exactly the entities.json names and characters.
  3. unbescape 1.1.8 HTML5_SYMBOLS (oracle_html5_sorted.tsv, dumped by OracleDump) holds the same names and code
     points, in the order of ORDER_KEY (';' below end of name, end of name below every other character).
  4. every name unbescape writes when escaping (oracle_html5_escape.tsv) ends with ';' and names that code point.
  5. unbescape's HTML4 escape map (oracle_html4_escape.tsv) equals commons-text 1.15.0 EntityArrays BASIC +
     ISO8859_1 + HTML40_EXTENDED escape maps, whose unescape maps are their inverses (oracle_commons_html4.tsv).
  6. parsing the emitted table texts back (the Java parser's rules, re-implemented here) gives the same tables.

Table text: entries in ORDER_KEY order of their names, separated by one space. An entry is one base-36 digit (the
number of leading characters shared with the previous entry's name), the rest of the name, a marker and the code
point in lower-case hex, then optionally '/' and a second code point. Markers: '=' plain, ':' the name unbescape
uses to escape that code point, '-' plain and also a name without ';' (legacy), '+' both. Every name ends with ';';
a legacy entry stands for two names, with and without ';', in that order.
"""
import hashlib
import html.entities
import json
import sys

ENTITIES_SHA256 = "d741d877ac77c4194c4ad526b5b4a19aef8dfe411ab840a466891cdbb9f362e6"
DIGITS36 = "0123456789abcdefghijklmnopqrstuvwxyz"
LITERAL_WIDTH = 80


def order_key(name):
    """unbescape HtmlEscapeSymbols.compare as a sort key: ';' sorts below end of name, end below the rest."""
    return [0 if c == ";" else ord(c) + 2 for c in name] + [1]


def fail(message):
    print("FAIL\t" + message)
    sys.exit(1)


def read_tsv(path):
    rows = []
    for line in open(path, encoding="utf-8").read().split("\n"):
        if line:
            rows.append(line.split("\t"))
    return rows


def emit(entries):
    """entries: (name-with-semicolon, marker, first, second-or-None) in order -> table text."""
    tokens, previous = [], ""
    for name, marker, first, second in entries:
        base = name[:-1]
        keep = 0
        while keep < min(len(base), len(previous), 35) and base[keep] == previous[keep]:
            keep += 1
        token = DIGITS36[keep] + base[keep:] + marker + format(first, "x")
        if second is not None:
            token += "/" + format(second, "x")
        tokens.append(token)
        previous = base
    return " ".join(tokens)


def parse(text):
    """The Java parser's rules: -> list of (name, first, second), list of escape (point, name)."""
    names, escapes, previous = [], [], ""
    for token in text.split(" "):
        keep = DIGITS36.index(token[0])
        at = 1
        while token[at].isalnum():
            at += 1
        base = previous[:keep] + token[1:at]
        marker = token[at]
        rest = token[at + 1:].split("/")
        first = int(rest[0], 16)
        second = int(rest[1], 16) if len(rest) > 1 else None
        names.append((base + ";", first, second))
        if marker in "-+":
            names.append((base, first, second))
        if marker in ":+":
            escapes.append((first, base + ";"))
        previous = base
    return names, escapes


def literal_lines(text, indent):
    """Java string-literal lines of at most LITERAL_WIDTH characters of text each, cut after a space."""
    pieces, start = [], 0
    while start < len(text):
        end = min(len(text), start + LITERAL_WIDTH)
        if end < len(text):
            cut = text.rfind(" ", start, end)
            end = cut + 1
        pieces.append(text[start:end])
        start = end
    lines = []
    for i, piece in enumerate(pieces):
        lines.append(indent + '"' + piece + '"' + (" +" if i + 1 < len(pieces) else ");"))
    return lines


def main():
    entities_path, oracle, template, out = sys.argv[1:5]
    raw = open(entities_path, "rb").read()
    digest = hashlib.sha256(raw).hexdigest()
    if digest != ENTITIES_SHA256:
        fail("entities.json sha256 " + digest)
    entities = json.loads(raw)
    table = {}
    for name, value in entities.items():
        if name[0] != "&":
            fail("name " + name)
        table[name[1:]] = (tuple(value["codepoints"]), value["characters"])
    for name, (points, chars) in table.items():
        if "".join(chr(p) for p in points) != chars:
            fail("characters of " + name)
    legacy = sorted(n for n in table if not n.endswith(";"))
    if len(table) != 2231 or len(legacy) != 106:
        fail("counts %d %d" % (len(table), len(legacy)))
    for n in legacy:
        if n + ";" not in table or table[n + ";"][0] != table[n][0]:
            fail("legacy " + n)
    python = {k: v for k, v in html.entities.html5.items()}
    if python != {k: v[1] for k, v in table.items()}:
        fail("python html.entities.html5 differs from entities.json")
    print("check\tentities.json sha256\t" + digest)
    print("check\tpython html.entities.html5 == entities.json\t%d" % len(python))
    sorted_rows = read_tsv(oracle + "/oracle_html5_sorted.tsv")
    unb = [(r[1][1:], tuple(int(x, 16) for x in r[2].split(" "))) for r in sorted_rows]
    if sorted(unb) != sorted((n, v[0]) for n, v in table.items()):
        fail("unbescape HTML5 table differs from entities.json")
    if [n for n, _ in unb] != sorted(table, key=order_key):
        fail("ORDER_KEY does not reproduce unbescape SORTED_NCRS")
    print("check\tunbescape HTML5_SYMBOLS == entities.json, ORDER_KEY order\t%d" % len(unb))
    preferred = {}
    for point, ref in read_tsv(oracle + "/oracle_html5_escape.tsv"):
        name = ref[1:]
        if not ref.startswith("&") or not name.endswith(";") or table.get(name, ((),))[0] != (int(point, 16),):
            fail("html5 escape " + point + " " + ref)
        preferred[name] = True
    print("check\tunbescape HTML5 escape names (end with ';', name their code point)\t%d" % len(preferred))
    html4 = {int(p, 16): ref for p, ref in read_tsv(oracle + "/oracle_html4_escape.tsv")}
    commons, inverse = {}, {}
    for kind, point, ref in read_tsv(oracle + "/oracle_commons_html4.tsv"):
        if kind in ("BASIC", "ISO8859_1", "HTML40_EXTENDED"):
            commons[int(point, 16)] = ref
        elif kind in ("BASIC_UNESCAPE", "ISO8859_1_UNESCAPE", "HTML40_EXTENDED_UNESCAPE"):
            inverse[ref] = int(point, 16)
        elif (kind, point, ref) not in (("APOS", "27", "&apos;"), ("APOS_UNESCAPE", "27", "&apos;")):
            fail("commons row " + kind)
    if html4 != commons or inverse != {v: k for k, v in commons.items()}:
        fail("unbescape HTML4 escape map differs from commons-text EntityArrays")
    print("check\tunbescape HTML4 escape map == commons-text BASIC+ISO8859_1+HTML40_EXTENDED\t%d" % len(html4))
    entries5 = []
    for name in sorted((n for n in table if n.endswith(";")), key=order_key):
        points = table[name][0]
        marker = ("-+" if name[:-1] in table else "=:")[1 if name in preferred else 0]
        entries5.append((name, marker, points[0], points[1] if len(points) > 1 else None))
    entries4 = [(ref[1:], ":", point, None) for point, ref in html4.items()]
    entries4.sort(key=lambda e: order_key(e[0]))
    text5, text4 = emit(entries5), emit(entries4)
    names5, escapes5 = parse(text5)
    if names5 != [(n, table[n][0][0], table[n][0][1] if len(table[n][0]) > 1 else None)
                  for n in sorted(table, key=order_key)]:
        fail("HTML5 table text does not parse back")
    if sorted(escapes5) != sorted((table[n][0][0], n) for n in preferred):
        fail("HTML5 escape names do not parse back")
    names4, escapes4 = parse(text4)
    if sorted(escapes4) != sorted((p, r[1:]) for p, r in html4.items()) or len(names4) != len(html4):
        fail("HTML4 table text does not parse back")
    print("check\ttable texts parse back\thtml5 %d chars, html4 %d chars" % (len(text5), len(text4)))
    lines = []
    for line in open(template, encoding="utf-8", newline="").read().replace("\r", "").split("\n"):
        stripped = line.strip()
        if stripped in ("@HTML5_TABLE@", "@HTML4_TABLE@"):
            indent = line[:len(line) - len(line.lstrip())]
            lines += literal_lines(text5 if stripped == "@HTML5_TABLE@" else text4, indent)
        else:
            lines.append(line)
    open(out, "w", encoding="ascii", newline="").write("\n".join(lines))
    print("wrote\t" + out + "\t%d lines" % len(lines))


main()
