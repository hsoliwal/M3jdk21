#!/usr/bin/env python3
"""dup_guard: find "one definition twice" artifacts left behind by 3-way merges.

Artifact class
--------------
A textual 3-way merge (git merge / rebase / fold) can keep BOTH sides of a hunk
when the same member was added independently on both parents.  The result
compiles-fails (javac: "method X is already defined", C: "redefinition of X")
or, for imports, silently carries a harmless duplicate line.

Scanner (CPU only, deterministic, no git required for --check)
--------------------------------------------------------------
  Java  : members directly inside a type body (brace depth 1 of the enclosing
          type; nested/anonymous/enum-constant bodies are separate scopes).
          Methods/constructors keyed by  name<type params>(parameter types),
          generics kept (type-parameter bounds are part of the erasure),
          parameter names/annotations/final dropped; ctor != method.
          Fields keyed by name.  Imports keyed by normalized import line.
          Duplicate top-level type names across files of one directory.
  C/C++ : *.c/*.h under **/native/** -- function DEFINITIONS (brace body at
          depth 0) whose name appears twice in the same file.  A preprocessor
          conditional between the two bodies marks FALSE_POSITIVE.

Verdicts
--------
  PURE_DUP        both member texts byte-identical (code_start..closing brace)
  DIVERGENT       same signature, texts differ -> NEC: owner decides; both
                  blob sha256 given
  FALSE_POSITIVE  parser-level: pp-conditional in C, low-confidence header

Usage
-----
  python dup_guard.py scan  --root <worktree> --out DUP_CANDIDATES.tsv [--blame]
  python dup_guard.py check --root <worktree> [--only <path>...]   # CI gate
      exit 1 on any PURE_DUP or DIVERGENT, 0 otherwise.
  python dup_guard.py file  <path>   # debug: list members of one file
"""
from __future__ import annotations

import argparse
import bisect
import hashlib
import os
import re
import subprocess
import sys
import time
from concurrent.futures import ProcessPoolExecutor
from dataclasses import dataclass, field
from typing import Iterable

SCAN_ROOTS = ("m3", "src")
EXCL_SEGS = {"target", "vendor", "third_party", "retained-variants"}
TEST_RES_RE = re.compile(r"(^|/)(main|test)/resources(/|$)")

MODIFIERS = {
    "public", "private", "protected", "static", "final", "abstract", "native",
    "synchronized", "transient", "volatile", "strictfp", "default", "sealed",
    "non-sealed",
}
CTRL_KW = {
    "if", "while", "for", "switch", "synchronized", "catch", "return", "new",
    "do", "try", "else", "case", "default", "throw", "assert", "super",
    "this", "instanceof",
}
C_KW = {
    "if", "while", "for", "switch", "return", "sizeof", "do", "else", "case",
    "struct", "union", "enum", "typedef", "defined", "__attribute__",
    "__declspec", "static_assert", "_Static_assert", "goto", "alignof",
}
IDENT_RE = re.compile(r"[A-Za-z_$][\w$]*")
TOK_RE = re.compile(r"[A-Za-z_$][\w$]*|\S")
TRAIL_IDENT_RE = re.compile(r"([A-Za-z_$][\w$]*)\s*$")
TRAIL_ARRAY_NAME_RE = re.compile(r"^(.*?)([A-Za-z_$][\w$]*)\s*((?:\[\s*\]\s*)*)$", re.S)
AFTER_PARENS_RE = re.compile(r"^\s*((throws|default)\b.*)?$", re.S)
PP_COND_RE = re.compile(r"^\s*#\s*(if|ifdef|ifndef|elif|else|endif)\b")


# ----------------------------------------------------------------------------
# text helpers
# ----------------------------------------------------------------------------
def read_text(path: str) -> str:
    with open(path, "rb") as fh:
        data = fh.read()
    return data.decode("utf-8", errors="surrogateescape")


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8", errors="surrogateescape")).hexdigest()


class LineIndex:
    def __init__(self, text: str):
        self.starts = [0]
        for m in re.finditer("\n", text):
            self.starts.append(m.end())
        self.text = text

    def line(self, off: int) -> int:
        return bisect.bisect_right(self.starts, off)

    def line_start(self, lineno: int) -> int:
        return self.starts[lineno - 1]

    def first_nonblank_line_after(self, off: int) -> int:
        """1-based line of the first non-blank line starting at/after `off`."""
        ln = self.line(off)
        # if `off` is not at the start of its line and the rest of that line is
        # non-blank, the member starts on this line
        rest = self.text[off:self.starts[ln] if ln < len(self.starts) else len(self.text)]
        if rest.strip():
            return ln
        ln += 1
        while ln <= len(self.starts):
            s = self.starts[ln - 1]
            e = self.starts[ln] if ln < len(self.starts) else len(self.text)
            if self.text[s:e].strip():
                return ln
            ln += 1
        return ln


def mask_java(text: str) -> str:
    """Blank comments and literal contents (keep quotes), preserve offsets/newlines."""
    out = list(text)
    n = len(text)
    i = 0
    while i < n:
        c = text[i]
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            j = n if j < 0 else j
            for k in range(i, j):
                out[k] = " "
            i = j
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            for k in range(i, j):
                if text[k] != "\n":
                    out[k] = " "
            i = j
        elif c == '"':
            if text.startswith('"""', i):
                j = text.find('"""', i + 3)
                while j > 0 and text[j - 1] == "\\":
                    j = text.find('"""', j + 1)
                j = n if j < 0 else j + 3
                for k in range(i + 1, j - 1):
                    if text[k] != "\n":
                        out[k] = " "
                i = j
            else:
                j = i + 1
                while j < n and text[j] != '"' and text[j] != "\n":
                    if text[j] == "\\":
                        out[j] = " "
                        j += 1
                        if j < n and text[j] != "\n":
                            out[j] = " "
                    else:
                        out[j] = " "
                    j += 1
                i = j + 1
        elif c == "'":
            j = i + 1
            while j < n and text[j] != "'" and text[j] != "\n":
                if text[j] == "\\":
                    out[j] = " "
                    j += 1
                    if j < n and text[j] != "\n":
                        out[j] = " "
                else:
                    out[j] = " "
                j += 1
            i = j + 1
        else:
            i += 1
    return "".join(out)


def mask_c(text: str) -> tuple[str, list[int]]:
    """Blank comments/literals and preprocessor lines; return (masked, pp_cond_lines)."""
    out = list(text)
    n = len(text)
    pp_lines: list[int] = []
    i = 0
    line_no = 1
    at_line_start = True
    while i < n:
        c = text[i]
        if c == "\n":
            line_no += 1
            at_line_start = True
            i += 1
            continue
        if at_line_start and c in " \t":
            i += 1
            continue
        if at_line_start and c == "#":
            # preprocessor line (with continuations)
            j = i
            seg_start_line = line_no
            while j < n:
                k = text.find("\n", j)
                k = n if k < 0 else k
                if k > 0 and text[k - 1] == "\\" and k < n:
                    j = k + 1
                    line_no += 1
                    continue
                j = k
                break
            directive = text[i:j]
            if PP_COND_RE.match(directive):
                pp_lines.append(seg_start_line)
            # blank it (also blank comments inside)
            for k in range(i, j):
                if text[k] != "\n":
                    out[k] = " "
            i = j
            at_line_start = True
            continue
        at_line_start = False
        if c == "/" and i + 1 < n and text[i + 1] == "/":
            j = text.find("\n", i)
            j = n if j < 0 else j
            for k in range(i, j):
                out[k] = " "
            i = j
        elif c == "/" and i + 1 < n and text[i + 1] == "*":
            j = text.find("*/", i + 2)
            j = n if j < 0 else j + 2
            for k in range(i, j):
                if text[k] != "\n":
                    out[k] = " "
                else:
                    line_no += 1
            i = j
        elif c == '"' or c == "'":
            q = c
            j = i + 1
            while j < n and text[j] != q and text[j] != "\n":
                if text[j] == "\\":
                    out[j] = " "
                    j += 1
                    if j < n and text[j] != "\n":
                        out[j] = " "
                else:
                    out[j] = " "
                j += 1
            i = j + 1
        else:
            i += 1
    return "".join(out), pp_lines


def strip_annotations(h: str) -> str:
    """Remove @Ident[(...)] (balanced parens), keep @interface."""
    out = []
    i = 0
    n = len(h)
    while i < n:
        if h[i] == "@":
            m = re.match(r"@\s*([A-Za-z_$][\w$]*(?:\s*\.\s*[A-Za-z_$][\w$]*)*)", h[i:])
            if not m or m.group(1) == "interface":
                out.append(h[i])
                i += 1
                continue
            j = i + m.end()
            k = j
            while k < n and h[k] in " \t\r\n":
                k += 1
            if k < n and h[k] == "(":
                depth = 0
                while k < n:
                    if h[k] == "(":
                        depth += 1
                    elif h[k] == ")":
                        depth -= 1
                        if depth == 0:
                            k += 1
                            break
                    k += 1
                j = k
            out.append(" ")
            i = j
        else:
            out.append(h[i])
            i += 1
    return "".join(out)


def top_positions(s: str) -> Iterable[int]:
    """Indices of characters at bracket depth 0.  Tracks ()[]{} and generic
    <...> (a '<' directly after an identifier/'>'/'?' or followed by an
    identifier/'?'/'>' is generic); '->' never closes an angle."""
    depth = 0
    angle = 0
    n = len(s)
    for i, ch in enumerate(s):
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        elif ch == "<":
            k = i - 1
            while k >= 0 and s[k] in " \t\r\n":
                k -= 1
            prev = s[k] if k >= 0 else " "
            j = i + 1
            while j < n and s[j] in " \t\r\n":
                j += 1
            nxt = s[j] if j < n else " "
            if (prev.isalnum() or prev in "_$>?") and (nxt.isalnum() or nxt in "_$?>"):
                angle += 1
        elif ch == ">":
            prev = s[i - 1] if i else " "
            if prev != "-" and angle > 0:
                angle -= 1
        if depth == 0 and angle == 0:
            yield i


def split_top(s: str, sep: str = ",") -> list[str]:
    parts, last = [], 0
    for i in top_positions(s):
        if s[i] == sep:
            parts.append(s[last:i])
            last = i + 1
    parts.append(s[last:])
    return parts


def has_top_eq(s: str) -> bool:
    return first_top_eq(s) >= 0


def first_top_eq(s: str) -> int:
    for i in top_positions(s):
        if s[i] == "=":
            prev = s[i - 1] if i else ""
            nxt = s[i + 1] if i + 1 < len(s) else ""
            if prev in "=!<>" or nxt == "=":
                continue
            return i
    return -1


def match_open_paren(s: str, close: int) -> int:
    depth = 0
    for i in range(close, -1, -1):
        if s[i] == ")":
            depth += 1
        elif s[i] == "(":
            depth -= 1
            if depth == 0:
                return i
    return -1


def norm_params(p: str) -> str:
    p = strip_annotations(p)
    out = []
    for part in split_top(p, ","):
        part = re.sub(r"\bfinal\b", " ", part).strip()
        if not part:
            continue
        m = TRAIL_ARRAY_NAME_RE.match(part)
        if m and m.group(1).strip():
            typ = m.group(1) + m.group(3)
        else:
            typ = part
        out.append(re.sub(r"\s+", "", typ))
    return ",".join(out)


def type_decl(h: str) -> tuple[str, str] | None:
    """Return (kind, name) if the header declares a named type."""
    toks = TOK_RE.findall(h)
    for i, t in enumerate(toks):
        if t in ("class", "interface", "enum", "record") and i + 1 < len(toks):
            prev = toks[i - 1] if i else None
            nxt = toks[i + 1]
            if prev == "." or not IDENT_RE.fullmatch(nxt):
                continue
            if t == "record":
                nxt2 = toks[i + 2] if i + 2 < len(toks) else ""
                if nxt2 not in ("(", "<"):
                    continue
                if prev is not None and prev not in MODIFIERS and prev not in (")", ";", "}", "{"):
                    continue
            if t == "interface" and prev == "@":
                return "@interface", nxt
            return t, nxt
    return None


def enum_const_header(hs: str) -> bool:
    """`A`, `A(args)`, `, B(args)` or `A, B, C(args)` -- the last item owns the body."""
    pieces = [p.strip() for p in split_top(hs, ",")]
    pieces = [p for p in pieces if p]
    if not pieces:
        return False
    return all(re.fullmatch(r"[A-Za-z_$][\w$]*(\s*\(.*\))?", p, re.S) for p in pieces)


def paren_balance(s: str) -> int:
    return s.count("(") - s.count(")")


def anon_class_suffix(h: str) -> bool:
    s = h.rstrip()
    if not s.endswith(")"):
        return False
    o = match_open_paren(s, len(s) - 1)
    if o < 0:
        return False
    before = s[:o].rstrip()
    if before.endswith(">"):
        depth = 0
        k = len(before) - 1
        while k >= 0:
            if before[k] == ">":
                depth += 1
            elif before[k] == "<":
                depth -= 1
                if depth == 0:
                    break
            k -= 1
        before = before[:k].rstrip()
    m = re.search(r"([A-Za-z_$][\w$]*(?:\s*\.\s*[A-Za-z_$][\w$]*)*)$", before)
    if not m:
        return False
    before = before[:m.start()].rstrip()
    return bool(re.search(r"(^|[^\w$])new$", before))


@dataclass
class Scope:
    kind: str                 # FILE | TYPE | METHOD | BLOCK | INIT | INITIALIZER
    name: str = ""
    type_kind: str = ""       # class/interface/enum/record/@interface/anon
    open_off: int = -1
    seg_start: int = 0        # header start (restored on INIT close)
    member_start: int = 0
    code_start: int = -1
    name_off: int = -1
    sig: str = ""
    member_kind: str = ""
    consts_done: bool = False
    members: dict = field(default_factory=dict)
    path: str = ""            # scope path for reporting


@dataclass
class Member:
    kind: str       # method | ctor | field | import | type
    sig: str
    scope: str
    line: int       # line of name token
    span_start: int # first non-blank line after previous boundary (incl. javadoc)
    span_end: int   # line of closing char
    text: str       # code_start .. closing char (byte exact)
    confidence: str = "high"
    code_off: int = -1
    end_off: int = -1


def classify_java_file(path: str, text: str) -> tuple[list[Member], list[str]]:
    """Return (members, warnings)."""
    masked = mask_java(text)
    li = LineIndex(text)
    members: list[Member] = []
    warnings: list[str] = []
    stack = [Scope(kind="FILE", path="")]
    seg_start = 0
    member_start = 0
    anon_counter = 0
    n = len(masked)

    def first_code(off_from: int, off_to: int) -> int:
        m = re.compile(r"\S").search(masked, off_from, off_to)
        return m.start() if m else off_from

    def add_member(kind: str, sig: str, scope: Scope, name_off: int, code_off: int,
                   end_off: int, mstart: int, confidence: str = "high") -> None:
        members.append(Member(
            kind=kind, sig=sig, scope=scope.path, line=li.line(name_off),
            span_start=li.first_nonblank_line_after(mstart), span_end=li.line(end_off),
            text=text[code_off:end_off + 1], confidence=confidence,
            code_off=code_off, end_off=end_off))

    def parse_member_header(h_raw: str, parent: Scope, h_off: int):
        """Classify a '{'-header or ';'-statement at TYPE level.
        Returns (kind, sig, name_off, confidence) or None."""
        h = strip_annotations(h_raw)
        s = h.rstrip()
        if not s.strip():
            return None
        if not s.endswith(")"):
            # `throws`/`default` suffix
            lp = s.rfind(")")
            if lp < 0:
                return None
            if not AFTER_PARENS_RE.match(s[lp + 1:]):
                return None
            s = s[:lp + 1]
        o = match_open_paren(s, len(s) - 1)
        if o < 0:
            return None
        params = s[o + 1:len(s) - 1]
        before = s[:o].rstrip()
        m = TRAIL_IDENT_RE.search(before)
        if not m:
            return None
        name = m.group(1)
        pre = before[:m.start()].rstrip()
        if name in CTRL_KW:
            return None
        if re.search(r"(^|[^\w$])new$", pre):
            return None
        name_off = h_off + h.rfind(name, 0, m.end())
        pre_toks = TOK_RE.findall(re.sub(r"<[^<>]*(?:<[^<>]*>[^<>]*)*>", " ", pre))
        pre_core = [t for t in pre_toks if t not in MODIFIERS]
        # method type parameters take part in the erasure (bounds) -> part of the key
        tparams = ""
        pre_nomod = re.sub(r"\b(" + "|".join(sorted(MODIFIERS)) + r")\b", " ", pre).strip()
        if pre_nomod.startswith("<"):
            depth = 0
            for k, c2 in enumerate(pre_nomod):
                if c2 == "<":
                    depth += 1
                elif c2 == ">":
                    depth -= 1
                    if depth == 0:
                        tparams = re.sub(r"\s+", "", pre_nomod[:k + 1])
                        break
        sig = f"{name}{tparams}({norm_params(params)})"
        if not pre.strip():
            if name == parent.name:
                return ("ctor", sig, name_off, "high")
            return None
        if pre_core and pre_core[-1] in CTRL_KW:
            return None
        if not pre_core:
            if name == parent.name:
                return ("ctor", sig, name_off, "high")
            return ("method", sig, name_off, "low")
        last = pre_core[-1]
        conf = "high" if (IDENT_RE.fullmatch(last) or last in (">", "]")) else "low"
        return ("method", sig, name_off, conf)

    i = 0
    while i < n:
        ch = masked[i]
        if ch == "{":
            parent = stack[-1]
            h_raw = masked[seg_start:i]
            sc = Scope(kind="BLOCK", open_off=i, seg_start=seg_start, member_start=member_start)
            if parent.kind == "INIT":
                sc.kind = "INIT"
            elif parent.kind in ("TYPE", "FILE"):
                hs = strip_annotations(h_raw).rstrip()
                td = type_decl(h_raw)
                if parent.kind == "TYPE" and (has_top_eq(hs) or hs.endswith(("(", ",", "->", "="))
                                              or paren_balance(h_raw) > 0):
                    sc.kind = "INIT"
                elif td:
                    sc.kind = "TYPE"
                    sc.type_kind, sc.name = td
                    sc.path = (parent.path + "." if parent.path else "") + sc.name
                    sc.code_start = first_code(seg_start, i)
                    sc.name_off = seg_start + h_raw.find(td[1])
                elif parent.kind == "TYPE" and parent.type_kind == "enum" and not parent.consts_done \
                        and enum_const_header(hs):
                    anon_counter += 1
                    sc.kind = "TYPE"
                    sc.type_kind = "anon"
                    sc.name = ""
                    sc.path = parent.path + f"#enumconst{anon_counter}"
                elif parent.kind == "TYPE":
                    pm = parse_member_header(h_raw, parent, seg_start)
                    if pm:
                        sc.kind = "METHOD"
                        sc.member_kind, sc.sig, sc.name_off, sc.path = pm[0], pm[1], pm[2], pm[3]
                        sc.code_start = first_code(seg_start, i)
                    else:
                        toks = [t for t in TOK_RE.findall(hs)]
                        core = [t for t in toks if t not in MODIFIERS]
                        if not core:
                            sc.kind = "INITIALIZER"
                        elif parent.type_kind == "record" and core == [parent.name]:
                            sc.kind = "METHOD"
                            sc.member_kind, sc.sig = "ctor", f"{parent.name}(compact)"
                            sc.name_off = seg_start + h_raw.find(parent.name)
                            sc.path = "high"
                            sc.code_start = first_code(seg_start, i)
                        else:
                            sc.kind = "BLOCK"
                            warnings.append(f"{path}:{li.line(i)}: unclassified brace header in type body: {hs.strip()[:80]!r}")
                else:  # FILE level: module-info etc.
                    sc.kind = "BLOCK"
            else:  # inside BLOCK/METHOD/INITIALIZER
                td = type_decl(h_raw)
                if td:
                    anon_counter += 1
                    sc.kind = "TYPE"
                    sc.type_kind, sc.name = td
                    sc.path = parent_path(stack) + "$" + sc.name + f"#{anon_counter}"
                elif anon_class_suffix(h_raw):
                    anon_counter += 1
                    sc.kind = "TYPE"
                    sc.type_kind = "anon"
                    sc.path = parent_path(stack) + f"$anon{anon_counter}"
            if parent.kind == "TYPE" and sc.kind == "TYPE" and sc.type_kind != "anon":
                pass
            if parent.kind in ("TYPE", "FILE") and sc.kind == "TYPE" and sc.name and parent.kind == "FILE":
                # top-level type: record for cross-file dup detection
                members.append(Member(kind="type", sig=sc.name, scope="", line=li.line(sc.name_off),
                                      span_start=0, span_end=0, text=""))
            # anonymous class inside a TYPE-level field initializer: make a TYPE scope
            if sc.kind == "INIT" and anon_class_suffix(h_raw):
                anon_counter += 1
                sc.kind = "TYPE"
                sc.type_kind = "anon"
                sc.path = parent_path(stack) + f"$anon{anon_counter}"
                sc.seg_start = seg_start
                sc.kind = "TYPE"
                sc.member_kind = "INIT_ANON"  # restore seg_start on close
            stack.append(sc)
            if sc.kind == "INIT":
                pass  # keep seg_start (statement continues)
            else:
                seg_start = i + 1
                member_start = i + 1
            i += 1
        elif ch == "}":
            if len(stack) <= 1:
                warnings.append(f"{path}:{li.line(i)}: unbalanced '}}'")
                seg_start = i + 1
                member_start = i + 1
                i += 1
                continue
            sc = stack.pop()
            parent = stack[-1]
            if sc.kind == "METHOD" and parent.kind == "TYPE":
                add_member(sc.member_kind, sc.sig, parent, sc.name_off, sc.code_start, i,
                           sc.member_start, confidence=sc.path if sc.path in ("high", "low") else "high")
            if sc.kind == "INIT" or sc.member_kind == "INIT_ANON":
                seg_start = sc.seg_start      # statement continues to ';'
                member_start = sc.member_start
            else:
                seg_start = i + 1
                member_start = i + 1
            i += 1
        elif ch == ";":
            top = stack[-1]
            if top.kind == "FILE":
                h = masked[seg_start:i].strip()
                if h.startswith("import"):
                    norm = re.sub(r"\s+", " ", h)
                    norm = re.sub(r"\s*\.\s*", ".", norm)
                    code_off = first_code(seg_start, i)
                    members.append(Member(kind="import", sig=norm, scope="", line=li.line(code_off),
                                          span_start=li.first_nonblank_line_after(member_start),
                                          span_end=li.line(i), text=text[code_off:i + 1],
                                          code_off=code_off, end_off=i))
                seg_start = i + 1
                member_start = i + 1
                i += 1
                continue
            elif top.kind == "TYPE":
                h_raw = masked[seg_start:i]
                hs = strip_annotations(h_raw).strip()
                if top.type_kind == "enum" and not top.consts_done:
                    top.consts_done = True
                elif hs:
                    code_off = first_code(seg_start, i)
                    # method declaration without body?
                    pm = None
                    if "(" in hs and not has_top_eq(hs.split("(", 1)[0]):
                        pm = parse_member_header(h_raw, top, seg_start)
                    if pm:
                        add_member(pm[0], pm[1], top, pm[2], code_off, i, member_start, pm[3])
                    elif not ("(" in hs and not has_top_eq(hs)):
                        # field(s)
                        for piece in split_top(hs, ","):
                            cut = first_top_eq(piece)
                            decl = piece[:cut].rstrip() if cut >= 0 else piece.rstrip()
                            m = TRAIL_ARRAY_NAME_RE.match(decl.strip())
                            if not m:
                                continue
                            fname = m.group(2)
                            if fname in MODIFIERS or fname in CTRL_KW:
                                continue
                            name_off = seg_start + h_raw.find(fname)
                            add_member("field", fname, top, name_off, code_off, i, member_start)
                seg_start = i + 1
                member_start = i + 1
                i += 1
                continue
            if top.kind != "INIT":
                seg_start = i + 1
            i += 1
        else:
            i += 1
    if len(stack) != 1:
        warnings.append(f"{path}: unbalanced braces (depth {len(stack) - 1} at EOF)")
    return members, warnings


def parent_path(stack: list[Scope]) -> str:
    for sc in reversed(stack):
        if sc.kind == "TYPE":
            return sc.path
    return ""


# ----------------------------------------------------------------------------
# C scanner
# ----------------------------------------------------------------------------
def classify_c_file(path: str, text: str) -> tuple[list[Member], list[str], list[int]]:
    masked, pp_lines = mask_c(text)
    li = LineIndex(text)
    members: list[Member] = []
    warnings: list[str] = []
    depth = 0
    seg_start = 0
    stack: list[tuple[int, int, str, int, int, int]] = []  # (open_off, seg_start, name, name_off, code_off, mstart)
    n = len(masked)
    i = 0
    while i < n:
        ch = masked[i]
        if ch == "{":
            name = ""
            name_off = code_off = -1
            if depth == 0:
                h = masked[seg_start:i]
                s = h.rstrip()
                if s.endswith(")") and not has_top_eq(s):
                    o = match_open_paren(s, len(s) - 1)
                    if o >= 0:
                        before = s[:o].rstrip()
                        # skip __attribute__((...)) tails
                        m = re.search(r"([A-Za-z_~][\w]*(?:\s*::\s*~?[A-Za-z_][\w]*)*)\s*$", before)
                        if m and m.group(1) not in C_KW and (before[:m.start()].strip() or "::" in m.group(1)):
                            qual = re.sub(r"\s+", "", m.group(1))
                            simple = qual.rsplit("::", 1)[-1]
                            name_off = seg_start + h.rfind(simple, 0, m.end())
                            if path.endswith(".c"):
                                name = simple
                            else:
                                name = f"{qual}({norm_params(s[o + 1:len(s) - 1])})"
                            mm = re.compile(r"\S").search(masked, seg_start, i)
                            code_off = mm.start() if mm else seg_start
            stack.append((i, seg_start, name, name_off, code_off, seg_start))
            depth += 1
            seg_start = i + 1
        elif ch == "}":
            if stack:
                open_off, sstart, name, name_off, code_off, mstart = stack.pop()
                depth -= 1
                if depth == 0 and name:
                    members.append(Member(kind="function", sig=name, scope="", line=li.line(name_off),
                                          span_start=li.first_nonblank_line_after(mstart),
                                          span_end=li.line(i), text=text[code_off:i + 1],
                                          code_off=code_off, end_off=i))
            else:
                warnings.append(f"{path}:{li.line(i)}: unbalanced '}}'")
                depth = 0
            seg_start = i + 1
        elif ch == ";":
            if depth == 0:
                seg_start = i + 1
            else:
                seg_start = i + 1
        i += 1
    if depth != 0:
        warnings.append(f"{path}: unbalanced braces (depth {depth} at EOF)")
    return members, warnings, pp_lines


# ----------------------------------------------------------------------------
# candidate construction
# ----------------------------------------------------------------------------
@dataclass
class Candidate:
    path: str
    kind: str
    sig: str
    scope: str
    line_a: int
    line_b: int
    span_a: str
    span_b: str
    identical: bool
    diff_class: str
    blob_a: str
    blob_b: str
    verdict: str
    note: str
    origin_a: str = ""
    origin_b: str = ""

    def row(self) -> list[str]:
        return [self.path, self.kind, self.sig, self.scope, str(self.line_a), str(self.line_b),
                self.span_a, self.span_b, "yes" if self.identical else "no", self.diff_class,
                self.blob_a, self.blob_b, self.origin_a, self.origin_b, self.verdict, self.note]


HEADER = ["path", "kind", "signature", "scope", "line_a", "line_b", "span_a", "span_b",
          "bodies_identical", "diff_class", "blob_a", "blob_b", "origin_a", "origin_b", "verdict", "note"]


def diff_class(a: str, b: str) -> str:
    if a == b:
        return "IDENT"
    if re.sub(r"\s+", "", a) == re.sub(r"\s+", "", b):
        return "WS_ONLY"
    ma, mb = mask_java(a), mask_java(b)
    if re.sub(r"\s+", "", ma) == re.sub(r"\s+", "", mb):
        return "COMMENT_ONLY"
    return "CODE"


def pair_candidates(path: str, members: list[Member], pp_lines: list[int] | None = None) -> list[Candidate]:
    cands: list[Candidate] = []
    seen: dict[tuple[str, str, str], Member] = {}
    for m in members:
        if m.kind == "type":
            continue
        key = (m.kind, m.scope, m.sig)   # ctor and method with the class name may coexist
        if key in seen:
            a = seen[key]
            dc = diff_class(a.text, m.text)
            identical = dc == "IDENT"
            note = ""
            if m.kind == "import":
                verdict = "PURE_DUP" if identical else "DIVERGENT"
                note = "harmless: identical single-type import"
            elif pp_lines is not None and any(a.span_end < ln < m.line for ln in pp_lines):
                verdict = "FALSE_POSITIVE"
                note = "pp-conditional between definitions"
            elif a.confidence == "low" or m.confidence == "low":
                verdict = "FALSE_POSITIVE"
                note = "low-confidence header parse"
            else:
                verdict = "PURE_DUP" if identical else "DIVERGENT"
            cands.append(Candidate(
                path=path, kind=m.kind, sig=m.sig, scope=m.scope, line_a=a.line, line_b=m.line,
                span_a=f"{a.span_start}-{a.span_end}", span_b=f"{m.span_start}-{m.span_end}",
                identical=identical, diff_class=dc, blob_a=sha256_text(a.text), blob_b=sha256_text(m.text),
                verdict=verdict, note=note))
            # keep the first as the anchor; a third copy pairs with the first too
        else:
            seen[key] = m
    return cands


def scan_one(args: tuple[str, str]) -> tuple[str, list[Candidate], list[str], list[tuple[str, int]]]:
    root, rel = args
    full = os.path.join(root, rel)
    try:
        text = read_text(full)
    except OSError as e:
        return rel, [], [f"{rel}: read error {e}"], []
    if rel.endswith(".java"):
        members, warnings = classify_java_file(rel, text)
        cands = pair_candidates(rel, members)
        types = [(m.sig, m.line) for m in members if m.kind == "type"]
        return rel, cands, warnings, types
    members, warnings, pp = classify_c_file(rel, text)
    return rel, pair_candidates(rel, members, pp), warnings, []


def iter_files(root: str, only: list[str] | None = None) -> list[str]:
    files: list[str] = []
    if only:
        for o in only:
            full = os.path.join(root, o)
            if os.path.isfile(full):
                files.append(o.replace("\\", "/"))
            elif os.path.isdir(full):
                files.extend(_walk(root, o.replace("\\", "/").rstrip("/")))
        return sorted(set(files))
    for top in SCAN_ROOTS:
        if os.path.isdir(os.path.join(root, top)):
            files.extend(_walk(root, top))
    return sorted(set(files))


def _walk(root: str, top: str) -> Iterable[str]:
    base = os.path.join(root, top)
    for dirpath, dirnames, filenames in os.walk(base):
        rel_dir = os.path.relpath(dirpath, root).replace("\\", "/")
        dirnames[:] = sorted(d for d in dirnames if d not in EXCL_SEGS and d != ".git")
        if TEST_RES_RE.search(rel_dir + "/"):
            dirnames[:] = []
            continue
        is_native = "/native/" in (rel_dir + "/")
        for fn in sorted(filenames):
            if fn.endswith(".java"):
                yield f"{rel_dir}/{fn}"
            elif is_native and (fn.endswith(".c") or fn.endswith(".h")):
                yield f"{rel_dir}/{fn}"


def blame_lines(root: str, rel: str, lines: list[int]) -> dict[int, str]:
    if not lines:
        return {}
    cmd = ["git", "-C", root, "blame", "--porcelain"]
    for ln in sorted(set(lines)):
        cmd += ["-L", f"{ln},{ln}"]
    cmd += ["--", rel]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, timeout=600, errors="replace")
    except (subprocess.TimeoutExpired, OSError) as e:
        return {ln: f"blame-error:{type(e).__name__}" for ln in lines}
    if out.returncode != 0:
        return {ln: "blame-error" for ln in lines}
    res: dict[int, str] = {}
    cur_sha = None
    cur_final = None
    times: dict[str, str] = {}
    for line in out.stdout.splitlines():
        m = re.match(r"^([0-9a-f]{40}) (\d+) (\d+)", line)
        if m:
            cur_sha, cur_final = m.group(1), int(m.group(3))
            res[cur_final] = cur_sha
        elif line.startswith("author-time ") and cur_sha:
            times[cur_sha] = time.strftime("%Y-%m-%d", time.gmtime(int(line.split()[1])))
    return {ln: f"{res[ln][:12]}@{times.get(res[ln], '?')}" if ln in res else "?" for ln in lines}


def run_scan(root: str, only: list[str] | None, jobs: int, do_blame: bool, log=sys.stderr,
             blame_verdicts: set[str] | None = None):
    files = iter_files(root, only)
    log.write(f"[dup_guard] root={root} files={len(files)} jobs={jobs}\n")
    log.flush()
    cands: list[Candidate] = []
    warnings: list[str] = []
    types: dict[tuple[str, str], list[tuple[str, int]]] = {}
    t0 = time.time()
    done = 0
    tasks = [(root, f) for f in files]
    if jobs > 1 and len(files) > 200:
        ex = ProcessPoolExecutor(max_workers=jobs)
        it = ex.map(scan_one, tasks, chunksize=64)
    else:
        ex = None
        it = map(scan_one, tasks)
    for rel, c, w, ts in it:
        cands.extend(c)
        warnings.extend(w)
        for name, ln in ts:
            types.setdefault((os.path.dirname(rel), name), []).append((rel, ln))
        done += 1
        if done % 5000 == 0:
            log.write(f"[dup_guard] heartbeat {done}/{len(files)} files {time.time() - t0:.0f}s cands={len(cands)}\n")
            log.flush()
    if ex:
        ex.shutdown()
    for (d, name), locs in sorted(types.items()):
        if len(locs) > 1:
            locs = sorted(locs)
            for rel, ln in locs[1:]:
                cands.append(Candidate(path=rel, kind="type", sig=name, scope="", line_a=locs[0][1], line_b=ln,
                                       span_a=f"{locs[0][0]}", span_b=rel, identical=False, diff_class="CROSS_FILE",
                                       blob_a="", blob_b="", verdict="DIVERGENT",
                                       note=f"top-level type also in {locs[0][0]}"))
    cands.sort(key=lambda c: (c.path, c.line_a, c.line_b, c.kind, c.sig))
    if do_blame:
        by_path: dict[str, list[int]] = {}
        for c in cands:
            if c.kind == "type" or (blame_verdicts and c.verdict not in blame_verdicts):
                continue
            by_path.setdefault(c.path, []).extend([c.line_a, c.line_b])
        for k, (p, lines) in enumerate(by_path.items()):
            bl = blame_lines(root, p, lines)
            for c in cands:
                if c.path == p and c.kind != "type":
                    c.origin_a = bl.get(c.line_a, "?")
                    c.origin_b = bl.get(c.line_b, "?")
            if (k + 1) % 20 == 0:
                log.write(f"[dup_guard] heartbeat blame {k + 1}/{len(by_path)} files\n")
                log.flush()
    log.write(f"[dup_guard] done files={len(files)} candidates={len(cands)} warnings={len(warnings)} {time.time() - t0:.0f}s\n")
    return cands, warnings, len(files)


def write_tsv(path: str, cands: list[Candidate]) -> None:
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as fh:
        fh.write("\t".join(HEADER) + "\n")
        for c in cands:
            fh.write("\t".join(x.replace("\t", " ").replace("\n", " ") for x in c.row()) + "\n")


def summarize(cands: list[Candidate]) -> dict[str, int]:
    out: dict[str, int] = {}
    for c in cands:
        out[c.verdict] = out.get(c.verdict, 0) + 1
    return out


def main(argv: list[str]) -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = ap.add_subparsers(dest="cmd", required=True)
    for name in ("scan", "check"):
        p = sub.add_parser(name)
        p.add_argument("--root", default=".")
        p.add_argument("--only", nargs="*", default=None, help="paths (files/dirs) relative to root")
        p.add_argument("--jobs", type=int, default=max(1, (os.cpu_count() or 2) - 1))
        p.add_argument("--out", default=None, help="TSV output (scan default: DUP_CANDIDATES.tsv)")
        p.add_argument("--warnings", default=None, help="write parser warnings here")
        p.add_argument("--blame", action="store_true", help="fill origin_a/origin_b via git blame")
        p.add_argument("--blame-verdicts", default=None,
                       help="comma list of verdicts to blame (default all); e.g. PURE_DUP,DIVERGENT")
        p.add_argument("--allow", default=None, help="TSV of accepted rows (path\\tkind\\tsignature) to ignore in check")
    pf = sub.add_parser("file")
    pf.add_argument("path")
    a = ap.parse_args(argv)
    if a.cmd == "file":
        text = read_text(a.path)
        if a.path.endswith(".java"):
            ms, ws = classify_java_file(a.path, text)
        else:
            ms, ws, _ = classify_c_file(a.path, text)
        for m in ms:
            print(f"{m.line}\t{m.kind}\t{m.scope}\t{m.sig}\t{m.span_start}-{m.span_end}\t{m.confidence}")
        for w in ws:
            print("WARN", w)
        return 0
    root = os.path.abspath(a.root)
    bv = set(a.blame_verdicts.split(",")) if a.blame_verdicts else None
    cands, warnings, nfiles = run_scan(root, a.only, a.jobs, a.blame, blame_verdicts=bv)
    if a.warnings:
        with open(a.warnings, "w", encoding="utf-8", newline="") as fh:
            fh.write("\n".join(warnings) + ("\n" if warnings else ""))
    if a.cmd == "scan":
        out = a.out or "DUP_CANDIDATES.tsv"
        write_tsv(out, cands)
        print(f"files={nfiles} candidates={len(cands)} {summarize(cands)} -> {out}")
        return 0
    allow: set[tuple[str, str, str]] = set()
    if a.allow and os.path.isfile(a.allow):
        with open(a.allow, encoding="utf-8") as fh:
            for line in fh:
                parts = line.rstrip("\n").split("\t")
                if len(parts) >= 3 and parts[0] != "path":
                    allow.add((parts[0], parts[1], parts[2]))
    bad = [c for c in cands if c.verdict in ("PURE_DUP", "DIVERGENT") and (c.path, c.kind, c.sig) not in allow]
    if a.out:
        write_tsv(a.out, cands)
    for c in bad:
        print(f"{c.verdict}\t{c.path}\t{c.kind}\t{c.sig}\t{c.line_a}\t{c.line_b}\t{c.diff_class}")
    print(f"files={nfiles} candidates={len(cands)} {summarize(cands)} blocking={len(bad)}")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
