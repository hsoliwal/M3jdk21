# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""scenario_shapes.py : SCENARIO SHAPES v1 (CPU) - census and extraction of repeated SCENARIO row shapes.

  census  <spec_dir> <template_dir> <out_tsv>
      Lists every shape shared by three or more SCENARIO rows (rendered body = the SCENARIO template applied to the
      row). Two shape kinds, both mechanical:
        BODY       the whole body is equal after the row's atom name becomes ${atom} and at most MAX_TOKEN_HOLES single
                   identifier/literal tokens vary (token skeleton of the generator's Java lexing);
        ASSERTION  one line holding a JUnit assertion is equal after its first argument becomes ${expected}, an
                   optional third argument becomes ${label} and the atom name becomes ${atom}; the lines before it
                   are the row's setup, the lines after it its extra checks.
      Writes kind, shape, rows, members, skeleton; prints the totals.
  extract <spec_dir> <template_dir> <PATTERN>...
      Converts SCENARIO rows into the named parametrised patterns, tried in the given order. A pattern is applied
      only when three or more rows match it. A row matches when the pattern template, read as a regular expression
      (${setup} and ${extra} take whole lines, ${atom} must be the row's atom, other arguments one line of text),
      captures arguments whose rendering is byte-identical to the row's SCENARIO rendering; the converted spec row
      is checked again through the generator's renderer before the spec file is rewritten. Prints, per pattern,
      the matched rows and the SCENARIO row count before and after.
Never edits generated files: the specs are the recipe input; regenerate with gen_atom_tests.py."""
import os
import re
import sys

import gen_atom_tests as gen

MAX_TOKEN_HOLES = 2
MIN_ROWS = 3
SCENARIO = "SCENARIO"
LINE_ARGS = ("setup", "extra")
TOKEN = re.compile(r'''
  (?P<ws>\s+)
| (?P<comment>//[^\n]*|/\*.*?\*/)
| (?P<string>"(?:\\.|[^"\\\n])*")
| (?P<char>'(?:\\.|[^'\\\n])+')
| (?P<number>0[xX][0-9a-fA-F_]+[lL]?|0[bB][01_]+[lL]?|(?:\d[\d_]*\.\d*|\.\d+)(?:[eE][+-]?\d+)?[fFdD]?|\d[\d_]*[lLfFdD]?)
| (?P<ident>\$\{\w+\}|[A-Za-z_$][\w$]*)
| (?P<op>>>>=|<<=|>>=|>>>|\.\.\.|->|::|\+\+|--|&&|\|\||[=!<>]=|[-+*/%&|^]=|.)
''', re.S | re.X)
KEYWORDS = frozenset("""abstract assert boolean break byte case catch char class continue default do double else enum
extends final finally float for if implements instanceof int interface long new private protected public return short
static super switch synchronized this throw throws try void volatile while var true false null""".split())
HOLE_KINDS = ("ident", "number", "string", "char")


def rendered(row, templates, spec):
    return gen.render_body(row, templates, spec).strip("\n")


def scenario_rows(spec_dir, templates):
    for spec in sorted(os.listdir(spec_dir)):
        if spec.endswith(".atoms"):
            header, rows = gen.parse_spec(os.path.join(spec_dir, spec))
            for row in rows:
                if row["pattern"] == SCENARIO:
                    yield spec, row, rendered(row, templates, spec)


def tokens(text):
    return [(m.lastgroup, m.group()) for m in TOKEN.finditer(text) if m.lastgroup not in ("ws", "comment")]


def atomized(text, atom):
    return re.sub(r"(?<![\w$])%s(?![\w$])" % re.escape(atom), "${atom}", text)


def label(spec, row):
    return "%s.%s#%d" % (row["owner"], row["atom"], row["n"])


# ---- census -------------------------------------------------------------------------------------------------------
def body_shapes(rows):
    groups = {}
    for spec, row, body in rows:
        toks = tokens(atomized(body, row["atom"]))
        key = tuple("H" if k in HOLE_KINDS and v not in KEYWORDS and v != "${atom}" else v for k, v in toks)
        groups.setdefault(key, []).append((spec, row, toks))
    shapes = []
    for key, members in groups.items():
        pending = list(members)
        while len(pending) >= MIN_ROWS:
            cluster = [pending[0]]
            for other in pending[1:]:
                trial = cluster + [other]
                holes = sum(1 for i in range(len(key)) if len({m[2][i][1] for m in trial}) > 1)
                if holes <= MAX_TOKEN_HOLES:
                    cluster.append(other)
            if len(cluster) >= MIN_ROWS:
                varying = [i for i in range(len(key)) if len({m[2][i][1] for m in cluster}) > 1]
                skeleton = " ".join("<%d>" % varying.index(i) if i in varying else v
                                    for i, (k, v) in enumerate(cluster[0][2]))
                shapes.append(("BODY", cluster, skeleton))
            pending = [m for m in pending if m not in cluster]
    return shapes


def split_args(text, open_at):
    """Top-level argument spans of the call whose '(' is at open_at, and the offset after its ')'."""
    masked = gen.mask(text)
    depth, start, spans = 0, open_at + 1, []
    for i in range(open_at, len(masked)):
        c = masked[i]
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                spans.append((start, i))
                return spans, i + 1
        elif c == "," and depth == 1:
            spans.append((start, i))
            start = i + 1
    return spans, len(text)


ASSERT_CALL = re.compile(r"(?<![\w$.])(assert[A-Z]\w*)\s*\(")


def assertion_skeleton(line, atom):
    """The line with the first assertion's first argument as ${expected} and its third as ${label}, or None."""
    m = ASSERT_CALL.search(gen.mask(line))
    if not m or m.group(1) not in gen.JUNIT_ASSERTIONS:
        return None
    spans, end = split_args(line, m.end() - 1)
    if len(spans) < 2 or end > len(line):
        return None
    if len(spans) > 3:
        return None
    out = atomized(line[:spans[0][0]] + "${expected}" + line[spans[0][1]:spans[1][1]] + "${label}"
                   + line[spans[-1][1]:], atom)
    dot = out.find(".${atom}(")
    if dot > 0:
        start = receiver_start(out, dot)
        if start < dot:
            out = out[:start] + "${receiver}" + out[dot:]
    return out


def receiver_start(text, dot):
    """Start of the postfix expression (names, calls, indexing, member access) that ends just before text[dot]."""
    i = dot
    while i > 0:
        c = text[i - 1]
        if c in ")]":
            opener, depth, j = "(" if c == ")" else "[", 0, i - 1
            while j >= 0:
                depth += (text[j] == c) - (text[j] == opener)
                if depth == 0:
                    break
                j -= 1
            i = max(j, 0)
            continue
        if c.isalnum() or c in "_$":
            while i > 0 and (text[i - 1].isalnum() or text[i - 1] in "_$"):
                i -= 1
            if i > 0 and text[i - 1] == ".":
                i -= 1
                continue
        break
    return i


def assertion_shapes(rows):
    groups = {}
    for spec, row, body in rows:
        for line in body.split("\n"):
            if line[:1].isspace():
                continue
            skeleton = assertion_skeleton(line, row["atom"])
            if skeleton and "${atom}" in skeleton:
                groups.setdefault(skeleton, []).append((spec, row, None))
                break
    return [("ASSERTION", members, key) for key, members in groups.items() if len(members) >= MIN_ROWS]


def census(spec_dir, template_dir, out_tsv):
    templates = gen.load_templates(template_dir)
    rows = list(scenario_rows(spec_dir, templates))
    shapes = body_shapes(rows) + assertion_shapes(rows)
    with open(out_tsv, "w", encoding="utf-8", newline="\n") as w:
        w.write("kind\tshape\trows\tmembers\tskeleton\n")
        for n, (kind, members, skeleton) in enumerate(sorted(shapes, key=lambda s: (s[0], -len(s[1]), s[2])), 1):
            w.write("%s\tS%02d\t%d\t%s\t%s\n" % (kind, n, len(members), ",".join(label(s, r) for s, r, _ in members),
                                               skeleton.replace("\t", " ").replace("\n", " \\n ")))
    for kind in ("BODY", "ASSERTION"):
        picked = [s for s in shapes if s[0] == kind]
        print("census %s shapes=%d rows=%d" % (kind, len(picked), sum(len(s[1]) for s in picked)))
    print("census scenario_rows=%d" % len(rows))


# ---- extract ------------------------------------------------------------------------------------------------------
NEWLINE = re.escape("\n")
ARG_DEPTH = 4
LABEL_ARG = "label"
STRING_RE = r'"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])+\''


def nested(depth):
    """Text inside brackets (commas allowed, no line break), at most depth further bracket levels."""
    plain = r'[^\n()\[\]{}"\']'
    if depth == 0:
        return r'(?:%s|%s)*' % (STRING_RE, plain)
    inner = nested(depth - 1)
    return r'(?:%s|%s|\(%s\)|\[%s\]|\{%s\})*' % (STRING_RE, plain, inner, inner, inner)


def argument(depth=ARG_DEPTH):
    """One argument expression: one line, brackets balanced, no comma outside brackets."""
    plain = r'[^\n()\[\]{}"\',]'
    inner = nested(depth - 1)
    return r'(?:%s|%s|\(%s\)|\[%s\]|\{%s\})+?' % (STRING_RE, plain, inner, inner, inner)


ARGUMENT = argument()


def template_regex(template):
    """Regex of a pattern template (atom and owner already substituted): ${setup} takes the whole lines before the
    shaped text, ${extra} the whole lines after it (each optional together with its line break), ${label} an
    optional ', <argument>', any other argument one argument expression; a repeated argument repeats its value."""
    parts = re.split(r"(\$\{\w+\})", template.strip("\n"))
    pattern, names = "", []
    for i, part in enumerate(parts):
        m = re.fullmatch(r"\$\{(\w+)\}", part)
        if not m:
            pattern += re.escape(part)
            continue
        name = m.group(1)
        if name == "setup":
            pattern += r"(?P<setup>(?:[^\n]*\n)*?)"
            if i + 1 < len(parts) and parts[i + 1].startswith("\n"):
                parts[i + 1] = parts[i + 1][1:]
        elif name == "extra":
            if pattern.endswith(NEWLINE):
                pattern = pattern[:-len(NEWLINE)]
            pattern += r"(?:\n(?P<extra>[\s\S]*))?"
        elif name in names:
            pattern += "(?P=%s)" % name
        elif name == LABEL_ARG:
            pattern += r"(?P<%s>(?:, %s)?)" % (name, ARGUMENT)
        else:
            pattern += r"(?P<%s>%s)" % (name, ARGUMENT)
        names.append(name)
    return re.compile(pattern + r"\Z")


def match_row(row, body, template, optional, templates, spec, pattern):
    shaped = template.replace("${atom}", row["atom"]).replace("${owner}", row["owner"])
    m = template_regex(shaped).match(body)
    if not m:
        return None
    args = {}
    for name, value in m.groupdict().items():
        value = (value or "").rstrip("\n") if name in LINE_ARGS else (value or "")
        if value == "" and name not in optional:
            return None
        if value != "":
            args[name] = value
    candidate = dict(row, pattern=pattern, args=args)
    try:
        return candidate if rendered(candidate, templates, spec) == body else None
    except SystemExit:
        return None


def spec_block(row):
    out = ["@atom %s.%s %d %s %s" % (row["owner"], row["atom"], row["n"], row["pattern"], row["oracle"])]
    for name, value in row["args"].items():
        out += ["%s <<<" % name] + value.split("\n") + [">>>"] if "\n" in value else ["%s = %s" % (name, value)]
    return out


def rewrite_spec(path, conversions):
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    starts = [i for i, l in enumerate(lines) if gen.ATOM.match(l)]
    out, cursor = [], 0
    for idx, start in enumerate(starts):
        end = starts[idx + 1] if idx + 1 < len(starts) else len(lines)
        while end > start + 1 and (not lines[end - 1].strip() or lines[end - 1].startswith("//")):
            end -= 1
        key = start + 1
        if key in conversions:
            out += lines[cursor:start] + spec_block(conversions[key])
            cursor = end
    out += lines[cursor:]
    with open(path, "w", encoding="utf-8", newline="\n") as w:
        w.write("\n".join(out))


def extract(spec_dir, template_dir, patterns):
    templates = gen.load_templates(template_dir)
    rows = list(scenario_rows(spec_dir, templates))
    before = len(rows)
    taken, per_spec = set(), {}
    for pattern in patterns:
        template, optional = templates[pattern]
        hits = []
        for spec, row, body in rows:
            if (spec, row["line"]) in taken:
                continue
            converted = match_row(row, body, template, optional, templates, spec, pattern)
            if converted:
                hits.append((spec, row, converted))
        status = "APPLIED" if len(hits) >= MIN_ROWS else "SKIPPED_BELOW_%d" % MIN_ROWS
        print("extract %s rows=%d %s: %s" % (pattern, len(hits), status, ",".join(label(s, r) for s, r, _ in hits)))
        if len(hits) < MIN_ROWS:
            continue
        for spec, row, converted in hits:
            taken.add((spec, row["line"]))
            per_spec.setdefault(spec, {})[row["line"]] = converted
    for spec, conversions in per_spec.items():
        rewrite_spec(os.path.join(spec_dir, spec), conversions)
    print("extract scenario_rows before=%d after=%d templated=%d" % (before, before - len(taken), len(taken)))


def main():
    mode = sys.argv[1]
    if mode == "census":
        census(*sys.argv[2:5])
    elif mode == "extract":
        extract(sys.argv[2], sys.argv[3], sys.argv[4:])
    else:
        raise SystemExit(__doc__)


if __name__ == "__main__":
    main()
