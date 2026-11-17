# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""port_donor_tests.py <spec.tsv> <class-name> <out.java> <skipped.tsv> <donor-test.java>...

DONOR_TEST_PORT v1 (2026-11-17; owner: "add test cases too"). Ports the donor's OWN JUnit assertions for the operations
of one String family into a JDK JUnit class, mechanically (CPU only; no LLM, no hand edits):

  spec.tsv rows: <donor-op> TAB STATIC|INSTANCE TAB <java-op> TAB <null-rule>
    STATIC    StringUtils.op(a, b...) -> String.<java-op>(a, b...)
    INSTANCE  StringUtils.op(r, a...) -> s(r).<java-op>(a...); a null receiver cannot be ported (skipped, reason
              NULL_RECEIVER); s(CharSequence) is a helper returning toString()
    null-rule NPE_ARG: a literal null argument (or, for varargs, a lone null or (T[]) null) means the JDK method throws
              NullPointerException, so the assertion becomes assertThrows(NullPointerException.class, () -> call);
              SAME: null arguments keep the donor's answer
  Every statement of a donor @Test method that calls a spec op is ported when, after translation, it calls no other
  StringUtils method and uses only literals, the donor class's static final constants (copied) and JDK types; it is
  otherwise written to skipped.tsv with the reason. Output: one @Test per donor method that kept a statement.
"""
import re
import sys

SPEC, CLASS, OUT, SKIPPED = sys.argv[1:5]
DONORS = sys.argv[5:]
spec = {}
for line in open(SPEC, encoding="utf-8"):
    if line.strip() and not line.startswith("#"):
        op, mode, java, rule = line.rstrip("\n").split("\t")
        spec[op] = (mode, java, rule)


def skip_literal(text, i):
    """Index after the string or char literal starting at text[i]."""
    quote = text[i]
    if text.startswith('"""', i):
        return text.index('"""', i + 3) + 3
    i += 1
    while text[i] != quote:
        i += 2 if text[i] == "\\" else 1
    return i + 1


def split_top(text, sep):
    """Split at sep characters that are outside literals and brackets."""
    parts, depth, start, i = [], 0, 0, 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            i = skip_literal(text, i)
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
        elif c == sep and depth == 0:
            parts.append(text[start:i])
            start = i + 1
        i += 1
    parts.append(text[start:])
    return parts


def close_paren(text, i):
    """Index of the parenthesis closing the one at text[i]."""
    depth = 0
    while True:
        c = text[i]
        if c in "\"'":
            i = skip_literal(text, i)
            continue
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return i
        i += 1


NULL_ARRAY = re.compile(r"^\(\s*[\w.]+\s*\[\]\s*\)\s*null$")


def translate(stmt):
    """(new statement, reason) with reason None when ported."""
    out, i, throws = "", 0, False
    pattern = re.compile(r"\bStringUtils\.(\w+)\(")
    while True:
        m = pattern.search(stmt, i)
        if not m:
            out += stmt[i:]
            break
        op = m.group(1)
        if op not in spec:
            return None, "OTHER_STRINGUTILS:" + op
        mode, java, rule = spec[op]
        end = close_paren(stmt, m.end() - 1)
        args = [a.strip() for a in split_top(stmt[m.end():end], ",")] if stmt[m.end():end].strip() else []
        inner, reason = translate(", ".join(args)) if any("StringUtils." in a for a in args) else (", ".join(args), None)
        if reason:
            return None, reason
        args = [a.strip() for a in split_top(inner, ",")] if inner.strip() else []
        if mode == "INSTANCE":
            if not args or args[0] == "null" or re.match(r"^\(\s*(String|CharSequence)\s*\)\s*null$", args[0]):
                return None, "NULL_RECEIVER"
            rest = args[1:]
            call = f"s({args[0]}).{java}({', '.join(rest)})"
        else:
            rest = args
            call = f"String.{java}({', '.join(args)})"
        if rule == "NPE_ARG" and mode == "INSTANCE":
            varargs_null = len(rest) == 1 and (rest[0] == "null" or NULL_ARRAY.match(rest[0]))
            plain_null = any(a == "null" for a in rest) and len(rest) > 1 and op not in ("equalsAny", "equalsAnyIgnoreCase")
            throws = throws or varargs_null or plain_null
        out += stmt[i:m.start()] + call
        i = end + 1
    if throws:
        call = re.search(r"\b(?:String|s\([^;]*?\))\.\w+\(.*\)", out)
        m = re.match(r"\s*assert\w+\((.*)\)\s*$", out, re.S)
        if not m:
            return None, "NPE_NOT_IN_ASSERT"
        inner_args = split_top(m.group(1), ",")
        target = next((a.strip() for a in inner_args if re.search(r"\b(String|s\()", a)), None)
        if target is None:
            return None, "NPE_TARGET_NOT_FOUND"
        out = f"assertThrows(NullPointerException.class, () -> {target})"
    return out, None


def strip_comments(text):
    """The text without // and /* */ comments, string and char literals kept."""
    out, i = [], 0
    while i < len(text):
        c = text[i]
        if c in "\"'":
            j = skip_literal(text, i)
            out.append(text[i:j])
            i = j
        elif text.startswith("//", i):
            j = text.find("\n", i)
            i = len(text) if j < 0 else j
        elif text.startswith("/*", i):
            j = text.find("*/", i + 2)
            out.append(" ")
            i = len(text) if j < 0 else j + 2
        else:
            out.append(c)
            i += 1
    return "".join(out)


def methods(source):
    """(name, body) of each @Test method (source without comments)."""
    source = strip_comments(source)
    for m in re.finditer(r"@Test\b[^{;]*?void\s+(\w+)\s*\([^)]*\)\s*(?:throws[^{]*)?\{", source):
        start = m.end() - 1
        depth, i = 0, start
        while True:
            c = source[i]
            if c in "\"'":
                i = skip_literal(source, i)
                continue
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    break
            i += 1
        yield m.group(1), source[start + 1:i]


def constants(source):
    """Static final String/char/int (array) constants of a donor test class, by name."""
    return {m.group(1): m.group(0).strip() for m in re.finditer(
        r"^\s*(?:private |protected |public )?static final (?:String|char|int|char\[\]|String\[\]) (\w+) = [^;]+;",
        source, re.M)}


tests, skipped, used = [], [], {}
known = {}
for donor in DONORS:
    source = open(donor, encoding="utf-8").read()
    known.update(constants(source))
def mask_literals(text):
    """The text with every string and char literal replaced by an empty literal."""
    out, i = [], 0
    while i < len(text):
        if text[i] in "\"'":
            j = skip_literal(text, i)
            out.append('""')
            i = j
        else:
            out.append(text[i])
            i += 1
    return "".join(out)


LOCAL_DECL = re.compile(r"^(?:final\s+)?(String|char|int|char\[\]|String\[\])\s+(\w+)\s*=")
for donor in DONORS:
    source = open(donor, encoding="utf-8").read()
    for name, body in methods(source):
        kept, locals_ = [], {}
        for raw in split_top(body, ";"):
            stmt = " ".join(raw.split())
            decl = LOCAL_DECL.match(stmt)
            if decl and "StringUtils." not in stmt:
                locals_[decl.group(2)] = stmt + ";"
                continue
            if not stmt or not re.search(r"\bStringUtils\.(\w+)\(", stmt):
                continue
            if not any(f"StringUtils.{op}(" in stmt for op in spec):
                continue
            new, reason = translate(stmt)
            if reason is None:
                masked = mask_literals(new)
                idents = set(re.findall(r"\b[A-Z][A-Z0-9_]+\b", masked))
                missing = [x for x in idents if x not in known and x not in ("ROOT", "US", "ENGLISH")]
                lowers = set(re.findall(r"(?<![.\w])([a-z]\w*)\b(?!\s*\()", masked)) - {
                    "s", "null", "true", "false", "new", "assertEquals", "assertTrue", "assertFalse",
                    "assertNull", "assertThrows", "int", "char", "length", "class", "toUpperCase",
                    "toLowerCase"}
                undeclared = sorted(x for x in lowers if x not in locals_ and not x.startswith("assert"))
                if missing:
                    reason = "UNKNOWN_SYMBOL:" + ",".join(sorted(missing))
                elif undeclared:
                    reason = "LOCAL_STATE:" + ",".join(undeclared)
                else:
                    for x in sorted(lowers & set(locals_)):
                        if locals_[x] not in kept:
                            kept.append(locals_[x])
            if reason:
                skipped.append((donor.replace("\\", "/").split("/")[-1], name, reason, stmt[:200]))
                continue
            for x in re.findall(r"\b[A-Z][A-Z0-9_]+\b|\bcp[0-9A-Fa-f]+\b", new):
                if x in known:
                    used[x] = known[x]
            kept.append(new + ";")
        if kept:
            tests.append((donor.replace("\\", "/").split("/")[-1].replace(".java", ""), name, kept))

def nested_classes(sources, statements):
    """Source text of the donor's nested classes that the ported statements construct (new X(...))."""
    wanted = set(re.findall(r"\bnew\s+([A-Z]\w*)\s*\(", " ".join(statements)))
    found = {}
    for source in sources:
        text = strip_comments(source)
        for m in re.finditer(r"\n(\s*)((?:private |protected |public )?(?:static )?(?:final )?class (\w+)\b[^{]*\{)",
                             text):
            name = m.group(3)
            if name not in wanted or name in found:
                continue
            start = m.start(2)
            depth, i = 0, text.index("{", start)
            while True:
                c = text[i]
                if c in "\"'":
                    i = skip_literal(text, i)
                    continue
                depth += c == "{"
                depth -= c == "}"
                if depth == 0:
                    break
                i += 1
            body = text[start:i + 1]
            body = re.sub(r"^(private |protected |public )?(static )?", "private static ", body)
            found[name] = "\n".join("    " + line.strip("\r") if line.strip() else ""
                                    for line in body.split("\n"))
    return [found[n] for n in sorted(found)]


def wrap(stmt, indent=8, cont=16, width=100):
    """The statement as lines of at most width columns, broken after commas outside literals."""
    if indent + len(stmt) <= width:
        return [" " * indent + stmt]
    cuts, depth, i = [], 0, 0
    while i < len(stmt):
        c = stmt[i]
        if c in "\"'":
            i = skip_literal(stmt, i)
            continue
        depth += c in "([{"
        depth -= c in ")]}"
        if c == "," and depth >= 1:
            cuts.append(i + 1)
        i += 1
    lines, start, pad = [], 0, indent
    while indent + len(stmt[start:]) > width or (lines and pad + len(stmt[start:].lstrip()) > width):
        limit = start + width - pad
        fit = [c for c in cuts if start < c <= limit]
        if not fit:
            break
        lines.append(" " * pad + stmt[start:fit[-1]].strip())
        start, pad = fit[-1], cont
        if pad + len(stmt[start:].strip()) <= width:
            break
    lines.append(" " * pad + stmt[start:].strip())
    return lines


sources = [open(d, encoding="utf-8").read() for d in DONORS]
nested = nested_classes(sources, [s for _, _, kept in tests for s in kept])
with open(OUT, "w", encoding="utf-8", newline="") as w:
    w.write("/*\n * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.\n * GPLv2 with the Classpath exception.\n *\n")
    w.write(f" * @test\n * @summary The assertions of commons-lang3 3.22.0's own tests for this family, ported to\n"
            f" *          java.lang.String by port_donor_tests.py (DONOR_TEST_PORT v1)\n * @run junit {CLASS}\n */\n\n")
    w.write("import static org.junit.jupiter.api.Assertions.*;\n\nimport java.util.Locale;\nimport org.junit.jupiter.api.Test;\n\n")
    w.write(f"/**\n * Ported mechanically from commons-lang3 3.22.0 (Apache-2.0) test sources; skipped statements and\n"
            f" * reasons are listed in the string-lane record.\n */\npublic class {CLASS} {{\n")
    for decl in sorted(set(used.values())):
        w.write("    " + re.sub(r"^\s*(private |protected |public )?", "private ", decl) + "\n")
    w.write("\n    private static String s(CharSequence text) {\n        return text.toString();\n    }\n")
    for cls in nested:
        w.write("\n" + cls + "\n")
    for donor, name, kept in tests:
        w.write(f"\n    @Test\n    void {donor[:1].lower() + donor[1:]}_{name}() {{\n")
        for stmt in kept:
            w.write("\n".join(wrap(stmt)) + "\n")
        w.write("    }\n")
    w.write("}\n")
with open(SKIPPED, "w", encoding="utf-8", newline="") as w:
    w.write("donor_file\tmethod\treason\tstatement\n")
    for row in skipped:
        w.write("\t".join(row) + "\n")
print(f"ported {sum(len(k) for _, _, k in tests)} statements in {len(tests)} tests; skipped {len(skipped)}")
