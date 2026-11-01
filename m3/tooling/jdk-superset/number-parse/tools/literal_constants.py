# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""literal_constants.py - recipe LITERAL_CONSTANTS v1 (user rule: no magic numbers, no floating strings).

  inventory <src_dir> <out.tsv> <file>...   every numeric, char and string literal of the code regions, one row each
                                            (a final table's elements collapse to one row): file line col kind type
                                            literal value class member disposition before after
  apply <src_dir> <names.tsv> <constants_out.tsv> <file>...
                                            every HOIST literal -> its ledger name; the constant is declared once at the
                                            top of its innermost named class (private static final, the literal's own
                                            type and spelling, so javac folds it back: method bytecode is unchanged);
                                            constants_out gets one row per replaced occurrence
  check <src_dir> <names.tsv> <file>...     apply without writing; prints the edit count (0 = fixed point)
  A file argument 'F.java:m1,m2' limits the run to those members of F (an edited region of a larger file).
  v1.1: apply and check take an optional first file argument --kinds=number,char that limits HOIST to those literal
  kinds (test and gate sources name their numbers and chars; their message strings stay inline). The TSVs this tool
  writes start with the Synexia SPDX header ('#' lines, which every reader of these ledgers skips).

Lexing is token-exact (SFA): comments, strings, text blocks and chars are whole tokens; only literal tokens are replaced,
everything else is copied byte for byte. A unary minus belongs to its literal (-1074 is one value).

Dispositions
  HOIST         a magic literal: needs a ledger name
  EXEMPT_UNIT   numeric value 0 or 1, unary minus included (-1): the user rule's loop / index / unit values
  CONSTANT_DEF  initializer of a final field: the literal is already the definition of a named constant
  TABLE_DATA    element of a final field's array initializer: data of a named table

Ledger (names.tsv, tab separated, '#' comments): class member literal name role
  member is 'm#k' (k-th occurrence of that literal spelling in member m, overloads counted together, source order),
  'm' or '*'; the most specific row wins. One name per role: occurrences with the same role share a name, and the same
  value in two roles gets two names. A name may be an existing static final field of the class with the same value
  (reused, not redeclared). A new name must not occur as an identifier anywhere in the input files (no shadowing, no
  hiding). Exit 3 and no write when a HOIST literal has no name or a name is unsafe."""
import math
import os
import re
import sys

TOKEN = re.compile(r'''
  (?P<ws>\s+)
| (?P<comment>//[^\n]*|/\*.*?\*/)
| (?P<text>"""(?:\\.|[^\\])*?""")
| (?P<string>"(?:\\.|[^"\\\n])*")
| (?P<char>'(?:\\.|[^'\\\n])+')
| (?P<number>0[xX](?:[0-9a-fA-F_]*\.[0-9a-fA-F_]*|[0-9a-fA-F_]+)[pP][+-]?[0-9_]+[fFdD]?
   |0[xX][0-9a-fA-F_]+[lL]?
   |0[bB][01_]+[lL]?
   |(?:[0-9][0-9_]*\.[0-9_]*|\.[0-9][0-9_]*)(?:[eE][+-]?[0-9_]+)?[fFdD]?
   |[0-9][0-9_]*[eE][+-]?[0-9_]+[fFdD]?
   |[0-9][0-9_]*[fFdDlL]?)
| (?P<ident>[A-Za-z_$][A-Za-z0-9_$]*)
| (?P<op>>>>=|<<=|>>=|>>>|\.\.\.|->|::|\+\+|--|&&|\|\||[=!<>]=|[-+*/%&|^]=|<<|>>|[{}()\[\];,.@=<>!~?:+\-*/&|^%])
''', re.S | re.X)
LITERAL_KINDS = ("number", "char", "string", "text")
TYPE_KEYWORDS = ("class", "interface", "enum", "record")
UNARY_AFTER = {"(", "[", ",", "=", "return", "case", "?", ":", "{", ";", "->", "throw", "assert", "&&", "||", "!",
               "==", "!=", "<", ">", "<=", ">=", "+", "-", "*", "/", "%", "&", "|", "^", "<<", ">>", ">>>", "~",
               "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="}
COLS = ("file", "line", "col", "kind", "type", "literal", "value", "class", "member", "disposition", "before", "after")
TSV_HEADER = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n")
KINDS_OPTION = "--kinds="
KINDS_SEPARATOR = ","


class Tok:
    __slots__ = ("kind", "text", "start", "end", "line", "col")

    def __init__(self, kind, text, start, end, line, col):
        self.kind, self.text, self.start, self.end, self.line, self.col = kind, text, start, end, line, col


def lex(src):
    toks, pos, line, line_start = [], 0, 1, 0
    while pos < len(src):
        m = TOKEN.match(src, pos)
        if not m:
            raise SystemExit("lex error at offset %d: %r" % (pos, src[pos:pos + 20]))
        kind, text = m.lastgroup, m.group(0)
        if kind not in ("ws", "comment"):
            toks.append(Tok(kind, text, pos, m.end(), line, pos - line_start + 1))
        nl = text.count("\n")
        if nl:
            line += nl
            line_start = pos + text.rindex("\n") + 1
        pos = m.end()
    return toks


def java_type(text):
    t = text.replace("_", "")
    low = t.lower()
    if low.startswith("0x"):
        if "p" in low:
            return "float" if low.endswith("f") else "double"
        return "long" if low.endswith("l") else "int"
    if low.startswith("0b"):
        return "long" if low.endswith("l") else "int"
    if low.endswith("l"):
        return "long"
    if low.endswith("f"):
        return "float"
    if low.endswith("d") or "." in low or "e" in low:
        return "double"
    return "int"


def numeric_value(text):
    t = text.replace("_", "")
    neg = t.startswith("-")
    t = t.lstrip("-")
    low = t.lower()
    if low.startswith("0x") and "p" in low:
        v = float.fromhex(low.rstrip("fd"))
    elif low.startswith("0x"):
        v = int(low[2:].rstrip("l"), 16)
    elif low.startswith("0b"):
        v = int(low[2:].rstrip("l"), 2)
    elif java_type(t) in ("float", "double"):
        v = float(low.rstrip("fd"))
    else:
        body = low.rstrip("l")
        v = int(body, 8) if len(body) > 1 and body.startswith("0") else int(body)
    return -v if neg else v


def skip_annotation(toks, j, end):
    """j at '@': index after the annotation (name, dotted parts, optional argument list)."""
    j += 1
    while j < end and (toks[j].kind == "ident" or toks[j].text == "."):
        j += 1
    if j < end and toks[j].text == "(":
        depth = 0
        while j < end:
            depth += (toks[j].text == "(") - (toks[j].text == ")")
            j += 1
            if depth == 0:
                break
    return j


def type_decl(toks, words):
    """Name of the type declared by the header words (token indexes), or None."""
    for n, k in enumerate(words[:-1]):
        nxt = toks[words[n + 1]]
        if toks[k].text in TYPE_KEYWORDS and (n == 0 or toks[words[n - 1]].text != ".") and nxt.kind == "ident" and \
                (n + 2 == len(words) or toks[words[n + 2]].text in ("{", "<", "(", "extends", "implements", "permits")):
            return nxt.text
    return None


def header_shape(toks, start, end):
    """Shape of a class-body member header toks[start:end]: (kind, name, final)."""
    j, words = start, []
    while j < end:
        if toks[j].text == "@" and j + 1 < end and toks[j + 1].text != "interface":
            j = skip_annotation(toks, j, end)
            continue
        words.append(j)
        j += 1
    texts = [toks[k].text for k in words]
    name = type_decl(toks, words)
    if name:
        return "CLASS", name, False
    if "=" in texts:
        eq = texts.index("=")
        return "FIELD", texts[eq - 1], "final" in texts[:eq]
    if "(" in texts:
        par = texts.index("(")
        return "METHOD", texts[par - 1] if par else "?", False
    return "INIT", "<clinit>" if "static" in texts else "<init>", False


def anonymous_class(toks, i):
    """toks[i] is '{' after ')': is it 'new T(...) {'?"""
    j, depth = i - 1, 0
    while j >= 0:
        depth += (toks[j].text == ")") - (toks[j].text == "(")
        if depth == 0:
            break
        j -= 1
    j -= 1
    while j >= 0 and (toks[j].kind == "ident" or toks[j].text in (".", "<", ">", ">>", ",", "?")):
        if toks[j].text == "new":
            return True
        j -= 1
    return False


class Frame:
    __slots__ = ("kind", "name", "final", "head")

    def __init__(self, kind, name="", final=False, head=0):
        self.kind, self.name, self.final, self.head = kind, name, final, head


def open_frame(toks, i, top):
    """The frame a '{' at toks[i] opens inside frame top."""
    prev = toks[i - 1].text if i else ""
    if top.kind == "CLASS":
        kind, name, final = header_shape(toks, top.head, i)
        if kind == "CLASS":
            return Frame("CLASS", name, head=i + 1)
        if kind == "FIELD" and prev == ")" and anonymous_class(toks, i):
            return Frame("ANON", name, head=i + 1)
        if kind == "FIELD" and prev != "->":
            return Frame("ARRAY", name, final, i + 1)
        return Frame("CODE", name, head=i + 1)
    if top.kind == "ARRAY" or prev in ("=", "]"):
        return Frame("ARRAY", top.name, top.final if top.kind == "ARRAY" else False, i + 1)
    if prev == ")" and anonymous_class(toks, i):
        return Frame("ANON", top.name, head=i + 1)
    words = [k for k in range(top.head, i)]
    name = type_decl(toks, words) if words else None
    return Frame("CLASS", name, head=i + 1) if name else Frame("CODE", top.name, head=i + 1)


def scan(toks, opens=None):
    """[(token index, innermost named class, member, context)] for every literal token; opens: class -> '{' index."""
    stack, out = [Frame("CLASS", "<file>", head=0)], []
    for i, t in enumerate(toks):
        top = stack[-1]
        if t.text == "{" and t.kind == "op":
            frame = open_frame(toks, i, top)
            if frame.kind == "CLASS" and opens is not None:
                opens[frame.name] = i
            stack.append(frame)
            continue
        if t.text == "}" and t.kind == "op":
            stack.pop()
            stack[-1].head = i + 1
            continue
        if t.text == ";" and t.kind == "op" and top.kind in ("CLASS", "CODE", "ANON"):
            top.head = i + 1
            continue
        if t.kind not in LITERAL_KINDS:
            continue
        cls = next(f.name for f in reversed(stack) if f.kind == "CLASS")
        if top.kind == "ARRAY":
            root_at = max(n for n, f in enumerate(stack) if f.kind != "ARRAY")
            root = stack[root_at]
            if root.kind == "CLASS":
                member, ctx = top.name, ("TABLE" if stack[root_at + 1].final else "CODE")
            else:
                member, ctx = next(f.name for f in reversed(stack) if f.kind in ("CODE", "ANON", "CLASS")), "CODE"
        elif top.kind == "CLASS":
            kind, name, final = header_shape(toks, top.head, i)
            member, ctx = name, ("FINAL_FIELD" if kind == "FIELD" and final else "CODE")
        else:
            member, ctx = top.name, "CODE"
        out.append((i, cls, member, ctx))
    return out


def literal_span(toks, i):
    """(first token index, literal text with unary minus) of literal token i."""
    if i >= 2 and toks[i - 1].text == "-" and toks[i].kind == "number" and \
            (toks[i - 2].text in UNARY_AFTER or toks[i - 2].kind == "ident" and toks[i - 2].text in ("return", "case")):
        return i - 1, "-" + toks[i].text
    if i == 1 and toks[0].text == "-":
        return 0, "-" + toks[i].text
    return i, toks[i].text


def java_value(typ, lit):
    """Comparable value of a literal in its Java type (two's complement for int/long, signed zero kept)."""
    if typ in ("char", "String"):
        return typ, lit
    v = numeric_value(lit)
    if typ == "int":
        v = (v + (1 << 31)) % (1 << 32) - (1 << 31)
    elif typ == "long":
        v = (v + (1 << 63)) % (1 << 64) - (1 << 63)
    else:
        v = float(v).hex()
    return typ, v


def literal_rows(path, rel, members=None):
    src = open(path, encoding="utf-8", newline="").read()
    toks = lex(src)
    rows, seen = [], {}
    for i, cls, member, ctx in scan(toks):
        if members is not None and member not in members:
            continue
        t = toks[i]
        first, lit = literal_span(toks, i)
        if t.kind == "number":
            typ, val = java_type(t.text), numeric_value(lit)
            value = repr(val)
        else:
            typ, val = ("char" if t.kind == "char" else "String"), None
            value = lit
        if ctx == "TABLE":
            disp = "TABLE_DATA"
        elif ctx == "FINAL_FIELD":
            disp = "CONSTANT_DEF"
        elif t.kind == "number" and val in (0, 1, -1) and not (val == 0 and math.copysign(1.0, val) < 0):
            disp = "EXEMPT_UNIT"
        else:
            disp = "HOIST"
        ordinal = seen[(cls, member, lit)] = seen.get((cls, member, lit), 0) + 1
        before = " ".join(x.text for x in toks[max(0, first - 4):first])
        after = " ".join(x.text for x in toks[i + 1:i + 5])
        rows.append(dict(file=rel, line=toks[first].line, col=toks[first].col, kind=t.kind, type=typ, literal=lit,
                         value=value, **{"class": cls}, member=member, disposition=disp, before=before, after=after,
                         _first=first, _last=i, _toks=toks, _src=src, _ordinal=ordinal))
    return rows


def load_ledger(path):
    """{(class, member or member#ordinal or *, literal): (name, role)}"""
    names = {}
    for ln in open(path, encoding="utf-8"):
        ln = ln.rstrip("\n")
        if not ln or ln.startswith("#") or ln.startswith("class\t"):
            continue
        cls, member, lit, name, role = (ln.split("\t") + [""] * 5)[:5]
        if (cls, member, lit) in names:
            raise SystemExit("duplicate ledger row: %s %s %s" % (cls, member, lit))
        names[(cls, member, lit)] = (name, role)
    return names


def ledger_name(names, r):
    for member in ("%s#%d" % (r["member"], r["_ordinal"]), r["member"], "*"):
        hit = names.get((r["class"], member, r["literal"]))
        if hit:
            return hit
    return None


def field_decls(toks):
    """Existing 'static final <type> NAME = <literal>;' declarations: {(class, NAME): (type, value)}."""
    found = {}
    for i, cls, member, ctx in scan(toks):
        if ctx == "FINAL_FIELD" and toks[i - 1].text in ("=", "-") and toks[i + 1].text == ";":
            first, lit = literal_span(toks, i)
            if toks[first - 1].text == "=" and toks[first - 2].text == member:
                typ = java_type(toks[i].text) if toks[i].kind == "number" else (
                    "char" if toks[i].kind == "char" else "String")
                found[(cls, member)] = java_value(typ, lit)
    return found


def class_bodies(toks):
    """{class name: index of its opening brace token}."""
    opens = {}
    scan(toks, opens)
    return opens


def plan(path, rel, names, used_idents, members=None, kinds=None):
    """(edits, declarations by class, ledger rows, problems) for one file; nothing is written.
    kinds: None for every HOIST literal, else the literal kinds (number, char, string, text) to hoist."""
    rows = literal_rows(path, rel, members)
    existing = field_decls(rows[0]["_toks"]) if rows else {}
    decls, edits, ledger, problems = {}, [], [], []
    for r in rows:
        if r["disposition"] != "HOIST" or (kinds is not None and r["kind"] not in kinds):
            continue
        hit = ledger_name(names, r)
        if not hit:
            problems.append("UNNAMED\t%s:%d\t%s\t%s#%d\t%s" % (rel, r["line"], r["class"], r["member"], r["_ordinal"],
                                                           r["literal"]))
            continue
        name, role = hit
        jv = java_value(r["type"], r["literal"])
        if (r["class"], name) in existing:
            if existing[(r["class"], name)] != jv:
                problems.append("VALUE_CLASH\t%s:%d\t%s\t%s" % (rel, r["line"], name, r["literal"]))
                continue
            state = "REUSED"
        else:
            prev = decls.get((r["class"], name))
            if prev and java_value(prev[1], prev[0]) != jv:
                problems.append("NAME_REUSED\t%s:%d\t%s\t%s/%s" % (rel, r["line"], name, prev[0], r["literal"]))
                continue
            if name in used_idents:
                problems.append("SHADOW_RISK\t%s:%d\t%s" % (rel, r["line"], name))
                continue
            if not prev:
                decls[(r["class"], name)] = (r["literal"], r["type"], role, len(decls))
            state = "DECLARED"
        toks = r["_toks"]
        edits.append((toks[r["_first"]].start, toks[r["_last"]].end, name))
        ledger.append((rel, r["line"], r["col"], r["class"], "%s#%d" % (r["member"], r["_ordinal"]), r["literal"],
                       r["type"], name, state, role))
    return rows, edits, decls, ledger, problems


def render(rows, edits, decls):
    """The rewritten source: literal tokens replaced, one constant block at the top of each class body."""
    toks, src = rows[0]["_toks"], rows[0]["_src"]
    opens = class_bodies(toks)
    nl = "\r\n" if "\r\n" in src else "\n"
    inserts = {}
    for (cls, name), (lit, typ, role, order) in sorted(decls.items(), key=lambda kv: kv[1][3]):
        inserts.setdefault(opens[cls], []).append((name, lit, typ, role))
    out, pos = [], 0
    pieces = [(toks[o].end, 0, o) for o in inserts] + [(s, 1, (e, n)) for s, e, n in edits]
    for at, what, data in sorted(pieces):
        out.append(src[pos:at])
        pos = at
        if what == 0:
            line_start = src.rfind("\n", 0, toks[data].start) + 1
            head = src[line_start:toks[data].start]
            indent = head[:len(head) - len(head.lstrip())] + " " * 4
            block = [nl + indent + "//FLLDCIM: M3 - LITERAL_CONSTANTS v1: this class's magic literals, named by role "
                                   "(user rule: no magic numbers, no floating strings)"]
            for name, lit, typ, role in inserts[data]:
                block.append(nl + indent + "/** %s */" % role + nl + indent +
                             "private static final %s %s = %s;" % (typ, name, lit))
            if not src.startswith(nl + nl, at):
                block.append(nl)
            out.append("".join(block))
        else:
            end, name = data
            out.append(name)
            pos = end
    out.append(src[pos:])
    return "".join(out)


def file_args(files):
    """'File.java' or 'File.java:member,member' (only those members are in scope)."""
    for arg in files:
        rel, _, mem = arg.partition(":")
        yield rel, (set(mem.split(",")) if mem else None)


LEDGER_COLS = ("file", "line", "col", "class", "member", "literal", "type", "name", "state", "role")


def main(argv):
    if len(argv) < 4:
        raise SystemExit(__doc__)
    cmd, src_dir, aux, rest = argv[1], argv[2], argv[3], argv[4:]
    if cmd == "inventory":
        with open(aux, "w", encoding="utf-8", newline="\n") as f:
            f.write(TSV_HEADER)
            f.write("\t".join(COLS) + "\n")
            for rel, members in file_args(rest):
                tables = {}
                for r in literal_rows(os.path.join(src_dir, rel), rel, members):
                    if r["disposition"] == "TABLE_DATA":
                        key = (r["class"], r["member"])
                        if key not in tables:
                            tables[key] = r
                            r["_count"] = 0
                        tables[key]["_count"] += 1
                        continue
                    f.write("\t".join(str(r[c]) for c in COLS) + "\n")
                for r in tables.values():
                    r = dict(r, literal="<%d elements>" % r["_count"], value="", before="", after="")
                    f.write("\t".join(str(r[c]) for c in COLS) + "\n")
        return 0
    out_ledger, kinds = None, None
    if cmd == "apply":
        out_ledger, rest = rest[0], rest[1:]
    if rest and rest[0].startswith(KINDS_OPTION):
        kinds, rest = set(rest[0][len(KINDS_OPTION):].split(KINDS_SEPARATOR)), rest[1:]
    names = load_ledger(aux)
    used = set()
    for rel, _ in file_args(rest):
        used |= {t.text for t in lex(open(os.path.join(src_dir, rel), encoding="utf-8", newline="").read())
                 if t.kind == "ident"}
    results, problems, ledger, total = {}, [], [], 0
    for rel, members in file_args(rest):
        rows, edits, decls, rows_ledger, bad = plan(os.path.join(src_dir, rel), rel, names, used, members, kinds)
        problems += bad
        ledger += rows_ledger
        if edits and not bad:
            results[rel] = render(rows, edits, decls)
            total += len(edits)
    if problems:
        sys.stdout.write("\n".join(problems) + "\n")
        return 3
    if cmd == "apply":
        for rel, text in results.items():
            with open(os.path.join(src_dir, rel), "w", encoding="utf-8", newline="") as f:
                f.write(text)
        with open(out_ledger, "w", encoding="utf-8", newline="\n") as f:
            f.write(TSV_HEADER)
            f.write("\t".join(LEDGER_COLS) + "\n")
            for row in ledger:
                f.write("\t".join(str(x) for x in row) + "\n")
    sys.stdout.write("EDITS\t%d\tFILES\t%d\n" % (total, len(results)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
