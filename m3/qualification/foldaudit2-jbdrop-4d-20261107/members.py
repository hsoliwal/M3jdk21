"""members.py <java-file> : list the member declarations of a Java source (types at any depth, methods, fields,
constructors) with their enclosing type path, line, and a body hash, by a brace-depth scan over code with comments
and literals masked. Used to diff two versions of a file structurally (which members one side lacks)."""
import hashlib
import re
import sys
from pathlib import Path

MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])*\'', re.S)


def masked(text):
    return MASK.sub(lambda m: re.sub(r"[^\n]", " ", m.group(0)), text)


DECL = re.compile(r"(?:(?:public|protected|private|static|final|abstract|synchronized|native|sealed|non-sealed|default)\s+)*"
                  r"(?:(class|interface|enum|record)\s+(\w+)|[\w<>\[\],.? ]+?\s+(\w+)\s*\(|[\w<>\[\],.? ]+?\s+(\w+)\s*(?:=|;))")


def members(text):
    code = masked(text)
    out, stack, depth, i, stmt_start = [], [], 0, 0, 0
    while i < len(code):
        c = code[i]
        if c == "{":
            head = code[stmt_start:i].strip()
            m = DECL.search(head.split("\n")[-1] if head else "") if head else None
            name = kind = None
            if m:
                if m.group(1):
                    kind, name = m.group(1), m.group(2)
                elif m.group(3) and not re.search(r"\b(if|for|while|switch|catch|synchronized|return|new)\b\s*\(?$",
                                                    head.split("\n")[-1][:m.start(3)]):
                    kind, name = "method", m.group(3)
            end = block_end(code, i)
            if name and (not stack or stack[-1][0] in ("class", "interface", "enum", "record")):
                path = ".".join(s[1] for s in stack if s[1])
                sig = " ".join(head.split())[-160:]
                out.append((path, kind, name, text.count("\n", 0, i) + 1, sig,
                            hashlib.sha1(text[stmt_start:end].encode()).hexdigest()[:12], stmt_start, end))
            stack.append((kind or "block", name if kind in ("class", "interface", "enum", "record") else None, end))
            depth += 1
            stmt_start = i + 1
        elif c == "}":
            if stack:
                stack.pop()
            depth -= 1
            stmt_start = i + 1
        elif c == ";":
            if stack and stack[-1][0] in ("class", "interface", "enum", "record"):
                head = code[stmt_start:i].strip()
                m = re.search(r"(\w+)\s*(?:=[^;]*)?$", " ".join(head.split()))
                if head and m and "(" not in head.split("=")[0]:
                    path = ".".join(s[1] for s in stack if s[1])
                    out.append((path, "field", m.group(1), text.count("\n", 0, i) + 1, " ".join(head.split())[:160],
                                hashlib.sha1(text[stmt_start:i + 1].encode()).hexdigest()[:12], stmt_start, i + 1))
            stmt_start = i + 1
        i += 1
    return out


def block_end(code, i):
    depth = 0
    for k in range(i, len(code)):
        depth += (code[k] == "{") - (code[k] == "}")
        if depth == 0:
            return k + 1
    return len(code)


if __name__ == "__main__":
    for path, kind, name, line, sig, h, s, e in members(Path(sys.argv[1]).read_text(encoding="utf-8")):
        print(f"{line}\t{path}\t{kind}\t{name}\t{h}\t{sig}")
