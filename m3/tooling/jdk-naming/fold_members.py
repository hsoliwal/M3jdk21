#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""FOLD_MEMBERS v1: one owner per concept. Members of duplicate classes fold into the owner class; the duplicates go.

usage: fold_members.py <repo root> <map.tsv> [--check]
The map (tab separated; '#' comments) declares everything, the tool does nothing undeclared:
  OWNER  <path of the owner .java>
  FILE   <path of a donor .java in the tree>                 a duplicate class whose members may fold
  TAKE   <Donor>.<member>[(<types>)]                          fold this member (every overload when no selector)
  SKIP   <Donor>.<member>  <reason>                           not folded (the owner already has it)
  EDIT   <Donor>.<member>  <sha256 prefix of old>  <old>  <new>   declared substitution inside that folded body
  IMPORT <fqn>                                                import the owner needs for the folded bodies
  REFS   <regex>  <replacement>                               reference rewrite over main and test Java of the module
                                                              (owner package tree, folded bodies and the owner text included, anchors excluded), evidence segments excluded
  REMOVE <path>                                               donor file removed after its members are folded
  MOVE   <old test path>  <new test path>                     test file renamed (class name follows the file name)
Folded bodies are copied byte for byte from the donor file (Javadoc and annotations included), one //FLLDCIM: FOLD
anchor per member names the donor blob; identical private helpers fold once; members the owner already declares (same
name and parameter types) are PRESENT and skipped. Idempotent: a second run changes nothing; --check writes nothing and
exits 1 when a run would change a file. Refuses (exit 2) an EDIT whose old text does not match its pin exactly once."""
from __future__ import annotations

import hashlib
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(sys.argv[1]).resolve()
MAP = Path(sys.argv[2]).resolve()
CHECK = "--check" in sys.argv
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
DECL = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final|synchronized|default|abstract)\s+)*"
                  rb"(?:<[^>]*>\s+)?(?!(?:return|new|throw|else|case|yield)\b)\w[\w.<>\[\], ?]*\s+(\w+)\s*\(")
FIELD = re.compile(rb"^(\s+)(?:(?:public|private|protected|static|final)\s+)+[\w.<>\[\]]+\s+(\w+)\s*(?:=|;)")
EVIDENCE = re.compile(r"/(recipe-custody|custody|evidence|qualification|recipes?|retained-variants|proofs?|snapshots?|"
                      r"before|after|postimages?|preimages?|donors?|intakes?|fixtures?|golden|expected|samples?)/")
changed = []


def eol_split(line: bytes):
    for eol in (b"\r\n", b"\n"):
        if line.endswith(eol):
            return line[:-len(eol)], eol
    return line, b""


def param_types(lines, start):
    text = b""
    for line in lines[start:]:
        text += eol_split(line)[0] + b" "
        if b")" in text[text.index(b"("):]:
            break
    inner = text[text.index(b"(") + 1:].decode()
    depth, cut = 0, []
    for char in inner:
        depth += char == "<"
        depth -= char == ">"
        if char == ")" and depth == 0:
            break
        cut.append(char if depth == 0 or char in "<>" else "")
    params = [re.sub(r"<>", "", p).strip() for p in "".join(cut).split(",")]
    return ",".join(re.sub(r"^(?:final\s+|@\w+\s+)*", "", p).rpartition(" ")[0].replace(" ", "") for p in params if p)


def leading_doc(lines, first):
    while first > 0 and eol_split(lines[first - 1])[0].strip().startswith(b"@"):
        first -= 1
    if first > 0 and eol_split(lines[first - 1])[0].strip().endswith(b"*/"):
        start = first - 1
        while start >= 0 and not eol_split(lines[start])[0].lstrip().startswith(b"/**"):
            start -= 1
        if start >= 0:
            first = start
    return first


def members(lines):
    found, index = [], 0
    while index < len(lines):
        text = eol_split(lines[index])[0]
        method, field = DECL.match(text), FIELD.match(text)
        if method and method.group(1) == b"    ":
            first, end = leading_doc(lines, index), index
            while end < len(lines) and eol_split(lines[end])[0] != b"    }" and not (
                    end == index and text.rstrip().endswith(b"}") and text.count(b"{") == text.count(b"}")):
                end += 1
            stop = end + 1
            if stop < len(lines) and eol_split(lines[stop])[0].strip() == b"":
                stop += 1
            found.append({"kind": "method", "name": method.group(2).decode(), "selector": param_types(lines, index),
                          "first": first, "stop": stop})
            index = stop
        elif field and field.group(1) == b"    " and b"(" not in text:
            first, stop = leading_doc(lines, index), index + 1
            if stop < len(lines) and eol_split(lines[stop])[0].strip() == b"":
                stop += 1
            found.append({"kind": "field", "name": field.group(2).decode(), "selector": None, "first": first, "stop": stop})
            index = stop
        else:
            index += 1
    return found


def read_map():
    m = {"OWNER": None, "FILE": [], "TAKE": [], "SKIP": {}, "EDIT": [], "IMPORT": [], "REFS": [], "REMOVE": [], "MOVE": []}
    for raw in MAP.read_text(encoding="utf-8").splitlines():
        if not raw.strip() or raw.startswith("#") or raw.startswith("kind\t"):
            continue
        kind, *rest = raw.split("\t")
        if kind == "OWNER":
            m["OWNER"] = rest[0]
        elif kind in ("FILE", "TAKE", "IMPORT", "REMOVE"):
            m[kind].append(rest[0])
        elif kind == "SKIP":
            m["SKIP"][rest[0]] = rest[1] if len(rest) > 1 else ""
        elif kind == "EDIT":
            m["EDIT"].append((rest[0], rest[1], rest[2], rest[3]))
        elif kind == "REFS":
            m["REFS"].append((rest[0], rest[1]))
        elif kind == "MOVE":
            m["MOVE"].append((rest[0], rest[1]))
    return m


def put(path: Path, data: bytes):
    if path.exists() and path.read_bytes() == data:
        return
    changed.append(path.relative_to(ROOT).as_posix())
    if not CHECK:
        path.write_bytes(data)


def refs(m, text: str) -> str:
    """REFS rows over every line that is not a //FLLDCIM: anchor (anchors are append-only and never edited)."""
    def line(one: str) -> str:
        if "//FLLDCIM:" in one:
            return one
        for pattern, replacement in m["REFS"]:
            one = re.sub(pattern, replacement, one)
        return one
    return "".join(line(one) for one in text.splitlines(keepends=True))


def main() -> int:
    m = read_map()
    owner_path = ROOT / m["OWNER"]
    owner = refs(m, owner_path.read_bytes().decode("utf-8")).encode("utf-8")
    owner_lines = LINES.findall(owner)
    present = {(x["name"], x["selector"]) for x in members(owner_lines)}
    folded, table, seen = [], [], {}
    for donor_rel in m["FILE"]:
        donor_path = ROOT / donor_rel
        if not donor_path.exists():
            continue  # already removed by an earlier run
        blob = subprocess.run(["git", "-C", str(ROOT), "hash-object", donor_rel], capture_output=True, text=True).stdout.strip()
        data = donor_path.read_bytes()
        lines = LINES.findall(data)
        donor_type = donor_path.stem
        for member in members(lines):
            key = donor_type + "." + member["name"]
            if member["name"] == donor_type:
                continue
            taken = any(t == key or t == key + "(" + (member["selector"] or "") + ")" for t in m["TAKE"])
            if key in m["SKIP"] or not taken:
                table.append((key, member["selector"], "SKIP" if key in m["SKIP"] else "NOT_TAKEN"))
                continue
            body = b"".join(lines[member["first"]:member["stop"]])
            for ekey, pin, old, new in m["EDIT"]:
                if ekey not in (key, key + "(" + (member["selector"] or "") + ")"):
                    continue
                o = old.encode()
                if body.count(o) != 1 or not hashlib.sha256(o).hexdigest().startswith(pin):
                    sys.exit(f"REFUSE EDIT {ekey}: old text occurs {body.count(o)} times or pin {pin} mismatches")
                body = body.replace(o, new.encode())
            body = refs(m, body.decode("utf-8")).encode("utf-8")
            digest = hashlib.sha256(re.sub(rb"\s+", b" ", body)).hexdigest()[:12]
            if (member["name"], member["selector"]) in present:
                table.append((key, member["selector"], "PRESENT"))
                continue
            if digest in seen:
                table.append((key, member["selector"], "DUPLICATE of " + seen[digest]))
                continue
            seen[digest] = key
            present.add((member["name"], member["selector"]))
            anchor = (f"    //FLLDCIM: FOLD - member {key} from donor blob {blob[:12]} ({donor_rel}); recipe "
                      f"m3/tooling/jdk-naming/fold_members.py v1, map {MAP.relative_to(ROOT).as_posix()}\n").encode()
            folded.append(anchor + body + (b"" if body.endswith(b"\n") else b"\n"))
            table.append((key, member["selector"], "FOLD"))
    if folded:
        close = max(i for i, line in enumerate(owner_lines) if eol_split(line)[0] == b"}")
        head = owner_lines[:close]
        for imp in m["IMPORT"]:
            line = f"import {imp};\n".encode()
            if line not in owner:
                last = max(i for i, l in enumerate(head) if eol_split(l)[0].startswith(b"import "))
                head = head[:last + 1] + [line] + head[last + 1:]
        put(owner_path, b"".join(head) + b"\n" + b"".join(folded) + b"".join(owner_lines[close:]))
    else:
        put(owner_path, b"".join(owner_lines))
    # references over the module's main and test Java (evidence segments excluded)
    module = owner_path
    while module.name != "src" and module.parent != module:
        module = module.parent
    module = module.parent
    for java in sorted(module.rglob("*.java")):
        rel = "/" + java.relative_to(ROOT).as_posix()
        if EVIDENCE.search(rel) or "/src/main/java/" not in rel and "/src/test/java/" not in rel:
            continue
        if java == owner_path or java.relative_to(ROOT).as_posix() in m["REMOVE"]:
            continue
        text = java.read_text(encoding="utf-8")
        new = refs(m, text)
        if new != text:
            put(java, new.encode("utf-8"))
    for old_rel, new_rel in m["MOVE"]:
        old_p, new_p = ROOT / old_rel, ROOT / new_rel
        if old_p.exists():
            text = old_p.read_text(encoding="utf-8").replace(old_p.stem, new_p.stem)
            changed.append(old_rel + " -> " + new_rel)
            if not CHECK:
                subprocess.run(["git", "-C", str(ROOT), "mv", old_rel, new_rel], check=True)
                new_p.write_text(text, encoding="utf-8", newline="\n")
    for rel in m["REMOVE"]:
        if (ROOT / rel).exists():
            changed.append("REMOVE " + rel)
            if not CHECK:
                subprocess.run(["git", "-C", str(ROOT), "rm", "-q", rel], check=True)
    for row in table:
        print("MEMBER\t" + "\t".join("" if c is None else str(c) for c in row))
    for c in changed:
        print("CHANGED\t" + c)
    print(f"FOLD_MEMBERS v1 owner={m['OWNER']} folded={sum(1 for r in table if r[2] == 'FOLD')} changed={len(changed)} check={CHECK}")
    return 1 if CHECK and changed else 0


if __name__ == "__main__":
    sys.exit(main())
