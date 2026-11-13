# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""literal_census.py [--strict] [--gate] [--tsv OUT] <file-or-dir>... : LITERAL CENSUS v1 for Java (CPU, deterministic).

Counts the numeric literals that stand outside a named constant ("no magic numbers" in generated tests). Comments,
string, text-block and char literals are masked first. A numeric literal is NAMED when it sits in
  - the initializer of a field declared both static and final (array initializers included, any class depth), or
  - the argument list of an enum constant (the constant is the name).
Every other numeric literal is counted: method, constructor, initializer and lambda bodies, case labels, local
variables and instance fields. The values -1, 0, 1 and 2 are trivial (the checkstyle MagicNumber defaults, compared
by value: 0L, 1.0 and 0x2 are trivial, -2 is not). --strict also counts a literal that is an argument of a method or
constructor call inside a static final initializer (STATIC_CALL: Integer.getInteger("p", 30) names the field, not the
default). Context column: BLOCK (code), FIELD (non-constant field), STATIC_CALL (strict only).
--gate exits 1 when anything is counted. Importable: scan(text, strict) -> [(line, literal, value, context)]."""
import os
import re
import sys

TOKEN = re.compile(r'''
 (?P<ws>\s+)
|(?P<comment>//[^\n]*|/\*.*?\*/)
|(?P<textblock>"""(?:\\.|[^\\])*?""")
|(?P<string>"(?:\\.|[^"\\\n])*")
|(?P<char>'(?:\\.|[^'\\\n])+')
|(?P<number>(?:0[xX](?:[0-9a-fA-F_]*\.?[0-9a-fA-F_]+|[0-9a-fA-F_]+\.?)[pP][+-]?\d+[fFdD]?
             |0[xX][0-9a-fA-F_]+[lL]?
             |0[bB][01_]+[lL]?
             |(?:\d[\d_]*\.[\d_]*|\.\d[\d_]*)(?:[eE][+-]?\d+)?[fFdD]?
             |\d[\d_]*(?:[eE][+-]?\d+[fFdD]?|[fFdD]|[lL])?))
|(?P<ident>[A-Za-z_$][A-Za-z0-9_$]*)
|(?P<op>.)
''', re.S | re.X)
TRIVIAL = (-1, 0, 1, 2)
TYPE_WORDS = ("class", "interface", "enum", "record")
CONTROL_WORDS = ("if", "for", "while", "switch", "catch", "synchronized", "return", "new", "throw", "case")
UNARY_AFTER = {"=", "(", ",", "{", "[", "?", ":", "return", "case", "->", "+", "-", "*", "/", "%", "<", ">", "&",
               "|", "^", "!", "~", "yield"}


class Scope:
    """One brace level: CLASS (type body), ENUM (enum body, constants open), BLOCK (code) or INIT (initializer)."""

    def __init__(self, kind, constant=False):
        self.kind = kind
        self.member = []        # CLASS/ENUM: tokens of the member being declared
        self.parens = []        # open parentheses of the current member or initializer: True = call
        self.constant = constant


def tokens(text):
    out, line = [], 1
    for m in TOKEN.finditer(text):
        kind, value = m.lastgroup, m.group()
        if kind not in ("ws", "comment"):
            out.append((kind, value, line))
        line += value.count("\n")
    return out


def value_of(literal):
    s = literal.replace("_", "")
    low = s.lower()
    if low.startswith("0x") and "p" in low:
        return float.fromhex(s.rstrip("fFdD"))
    if low.startswith("0x"):
        return int(s.rstrip("lL"), 16)
    if low.startswith("0b"):
        return int(s.rstrip("lL")[2:], 2)
    if any(c in low for c in ".e") or low[-1] in "fd":
        return float(s.rstrip("fFdD"))
    s = s.rstrip("lL")
    return int(s, 8) if len(s) > 1 and s.startswith("0") else int(s)


def declares_type(seq):
    """True when the tokens declare a class, interface, enum or record (the keyword followed by a name)."""
    return any(v in TYPE_WORDS and i + 1 < len(seq) and seq[i + 1][0] == "ident" for i, (_, v, _) in enumerate(seq))


def anonymous_body(toks, i):
    """True when toks[i] == '{' opens an anonymous class body: new Type<...>(...) {."""
    if i == 0 or toks[i - 1][1] != ")":
        return False
    depth, j = 0, i - 1
    while j >= 0:
        depth += {")": 1, "(": -1}.get(toks[j][1], 0)
        if depth == 0:
            break
        j -= 1
    k = j - 1
    while k >= 0 and (toks[k][0] == "ident" or toks[k][1] in (".", "<", ">", ",", "?", "[", "]")):
        if toks[k][1] == "new":
            return True
        k -= 1
    return False


def is_call(toks, i):
    """True when the '(' at toks[i] opens an argument list (method or constructor call, or a declaration)."""
    if i == 0:
        return False
    kind, val, _ = toks[i - 1]
    return (kind == "ident" and val not in CONTROL_WORDS) or val in (">", "]")


def assigned(member):
    depth = 0
    for _, v, _ in member:
        depth += {"(": 1, ")": -1}.get(v, 0)
        if v == "=" and depth == 0:
            return True
    return False


def scan(text, strict=False):
    """[(line, literal, value, context)] for every non-trivial numeric literal outside a named constant."""
    toks = tokens(text)
    scopes = [Scope("CLASS")]
    found = []
    for i, (kind, val, line) in enumerate(toks):
        s = scopes[-1]
        if s.kind in ("INIT", "BLOCK"):
            if val == "{":
                if s.kind == "INIT":
                    scopes.append(Scope("INIT", s.constant))
                    continue
                start = i - 1
                while start >= 0 and toks[start][1] not in (";", "{", "}"):
                    start -= 1
                statement = toks[start + 1:i]
                if anonymous_body(toks, i) or declares_type(statement):
                    enum = declares_type(statement) and any(v == "enum" for _, v, _ in statement)
                    scopes.append(Scope("ENUM" if enum else "CLASS"))
                else:
                    scopes.append(Scope("BLOCK"))
            elif val == "}":
                scopes.pop()
                if scopes and scopes[-1].kind in ("CLASS", "ENUM") and s.kind != "INIT":
                    scopes[-1].member = []
            elif val == "(":
                s.parens.append(is_call(toks, i))
            elif val == ")" and s.parens:
                s.parens.pop()
            elif kind == "number":
                if s.kind == "BLOCK":
                    note(found, toks, i, "BLOCK")
                elif not s.constant:
                    note(found, toks, i, "FIELD")
                elif strict and any(s.parens):
                    note(found, toks, i, "STATIC_CALL")
            continue
        # CLASS or ENUM body: members, or enum constants while ENUM is still open
        if val == "(":
            s.parens.append(is_call(toks, i))
        elif val == ")" and s.parens:
            s.parens.pop()
        depth0 = not s.parens
        if s.kind == "ENUM":
            if val == ";" and depth0:
                s.kind = "CLASS"
                s.member = []
            elif val == "{" and depth0:
                scopes.append(Scope("CLASS"))
            elif val == "}" and depth0:
                scopes.pop()
                if scopes and scopes[-1].kind in ("CLASS", "ENUM"):
                    scopes[-1].member = []
            continue
        if val == ";" and depth0:
            s.member = []
            continue
        if val == "}" and depth0:
            scopes.pop()
            if scopes and scopes[-1].kind in ("CLASS", "ENUM"):
                scopes[-1].member = []
            continue
        if val == "{" and depth0:
            words = [v for k, v, _ in s.member if k == "ident"]
            if declares_type(s.member) and not assigned(s.member):
                scopes.append(Scope("ENUM" if "enum" in words else "CLASS"))
                s.member = []
            elif assigned(s.member):
                scopes.append(Scope("INIT", "static" in words and "final" in words))
            else:
                scopes.append(Scope("BLOCK"))
                s.member = []
            continue
        s.member.append((kind, val, line))
        if kind == "number":
            words = [v for k, v, _ in s.member if k == "ident"]
            constant = assigned(s.member) and "static" in words and "final" in words
            if not constant:
                note(found, toks, i, "FIELD")
            elif strict and any(s.parens):
                note(found, toks, i, "STATIC_CALL")
    return found


def note(found, toks, i, context):
    literal = toks[i][1]
    value = value_of(literal)
    negative = i > 0 and toks[i - 1][1] == "-" and (i < 2 or toks[i - 2][1] in UNARY_AFTER)
    if negative:
        value = -value
    if value in TRIVIAL:
        return
    found.append((toks[i][2], ("-" if negative else "") + literal, value, context))


def java_files(paths):
    for p in paths:
        if os.path.isdir(p):
            for dirpath, dirs, names in os.walk(p):
                dirs.sort()
                for n in sorted(names):
                    if n.endswith(".java"):
                        yield os.path.join(dirpath, n)
        else:
            yield p


def main(argv):
    gate, strict = "--gate" in argv, "--strict" in argv
    tsv = argv[argv.index("--tsv") + 1] if "--tsv" in argv else None
    paths = [a for i, a in enumerate(argv) if not a.startswith("--") and (i == 0 or argv[i - 1] != "--tsv")]
    rows, files = [], 0
    for path in java_files(paths):
        files += 1
        with open(path, encoding="utf-8") as f:
            for line, literal, _, context in scan(f.read(), strict):
                rows.append((path.replace(os.sep, "/"), str(line), literal, context))
    if tsv:
        with open(tsv, "w", encoding="utf-8", newline="\n") as w:
            w.write("file\tline\tliteral\tcontext\n")
            w.writelines("\t".join(r) + "\n" for r in rows)
    per_file = {}
    for r in rows:
        per_file[r[0]] = per_file.get(r[0], 0) + 1
    for path, n in sorted(per_file.items()):
        print(f"{n}\t{path}")
    print(f"literal_census v1{' strict' if strict else ''} files={files} counted={len(rows)} "
          f"files_with_literals={len(per_file)}")
    return 1 if gate and rows else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
