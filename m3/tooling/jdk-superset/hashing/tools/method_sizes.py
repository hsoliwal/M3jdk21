# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""method_sizes.py <javap> <classes dir> <sources dir> <ATOMS.tsv.gz> <out tsv>

Bytecode size of every method of jdk.internal.m3.hash against the HotSpot 21 inlining limits. The size is the
Code attribute's code_length read from the class file; `javap -c -p` lists the same methods and its last
instruction offset is the cross-check (it must be below code_length). Each method is joined to its tree-sitter atom
(atomize_patternize.py ATOMS.tsv.gz) by the source lines of its LineNumberTable (classes compiled with -g); the line
span is recomputed from the atom's byte span, because the ATOMS line columns are not reliable under tree-sitter 0.26.
Writes one row per method, sorted by class then source line then name.
"""
import csv
import gzip
import os
import re
import struct
import subprocess
import sys

NL = "\n"
TAB = "\t"
PACKAGE = "jdk/internal/m3/hash"
MAX_INLINE_SIZE = 35          # -XX:MaxInlineSize
FREQ_INLINE_SIZE = 325        # -XX:FreqInlineSize
DESIRED_METHOD_LIMIT = 8000   # -XX:DesiredMethodLimit (C2 inlining budget per compilation)
HUGE_METHOD_LIMIT = 8000      # -XX:HugeMethodLimit (not compiled while DontCompileHugeMethods)
ACC_STATIC = 0x0008
ACC_PRIVATE = 0x0002
ACC_BRIDGE = 0x0040
ACC_SYNTHETIC = 0x1000
TWO_SLOT_TAGS = (5, 6)
FIXED_TAG_SIZES = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4,
                   19: 2, 20: 2}
UTF8_TAG = 1
HEADER = ("class", "method", "descriptor", "flags", "bytecode_bytes", "javap_last_offset", "source_file",
          "start_line", "end_line", "source_lines", "atom_id", "gt_MaxInlineSize_35", "gt_FreqInlineSize_325",
          "gt_HugeMethodLimit_8000")
JAVAP_METHOD = re.compile(r"^  (?:.*? )?([\w$.<>]+)\((.*)\)(?: throws .*)?;$")
JAVAP_CLINIT = "  static {};"
JAVAP_INSN = re.compile(r"^\s+(\d+): \w")


class Reader:
    def __init__(self, data):
        self.data, self.pos = data, 0

    def take(self, fmt):
        value = struct.unpack_from(fmt, self.data, self.pos)
        self.pos += struct.calcsize(fmt)
        return value[0] if len(value) == 1 else value

    def skip(self, n):
        self.pos += n


def constant_pool(r):
    count = r.take(">H")
    pool, i = [None] * count, 1
    while i < count:
        tag = r.take(">B")
        if tag == UTF8_TAG:
            n = r.take(">H")
            pool[i] = r.data[r.pos:r.pos + n].decode("utf-8", "surrogatepass")
            r.skip(n)
        else:
            r.skip(FIXED_TAG_SIZES[tag])
        i += 2 if tag in TWO_SLOT_TAGS else 1
    return pool


def attributes(r, pool):
    out = {}
    for _ in range(r.take(">H")):
        name, length = pool[r.take(">H")], r.take(">I")
        out[name] = r.data[r.pos:r.pos + length]
        r.skip(length)
    return out


def code_info(code):
    r = Reader(code)
    r.skip(4)  # max_stack, max_locals
    code_length = r.take(">I")
    r.skip(code_length)
    r.skip(8 * r.take(">H"))  # exception table
    return code_length, r


def line_numbers(r, pool):
    lines = []
    for name, body in attributes(r, pool).items():
        if name == "LineNumberTable":
            n = struct.unpack_from(">H", body, 0)[0]
            lines += [struct.unpack_from(">H", body, 2 + 4 * k + 2)[0] for k in range(n)]
    return lines


def parse_class(path):
    r = Reader(open(path, "rb").read())
    r.skip(8)  # magic, minor, major
    pool = constant_pool(r)
    r.skip(6)  # access, this, super
    r.skip(2 * r.take(">H"))  # interfaces
    for _ in range(r.take(">H")):  # fields
        r.skip(6)
        attributes(r, pool)
    methods = []
    for _ in range(r.take(">H")):
        flags, name, desc = r.take(">H"), pool[r.take(">H")], pool[r.take(">H")]
        attrs = attributes(r, pool)
        if "Code" in attrs:
            length, rest = code_info(attrs["Code"])
            methods.append((name, desc, flags, length, line_numbers(rest, pool)))
    source = attributes(r, pool).get("SourceFile")
    source_name = pool[struct.unpack(">H", source)[0]] if source else ""
    return methods, source_name


def javap_offsets(javap, path):
    """Last instruction offset per method name, in javap -c -p order (overloads keep their order)."""
    out = subprocess.run([javap, "-c", "-p", path], capture_output=True, text=True, check=True).stdout
    offsets, current = [], None
    for line in out.splitlines():
        m = JAVAP_METHOD.match(line)
        if m or line == JAVAP_CLINIT:
            name = "<clinit>" if not m else "<init>" if "." in m.group(1) else m.group(1)
            current = [name, -1]
            offsets.append(current)
            continue
        i = JAVAP_INSN.match(line)
        if i and current is not None:
            current[1] = int(i.group(1))
    return offsets


def atom_spans(atoms_gz, sources):
    spans = {}
    with gzip.open(atoms_gz, "rt", encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f, delimiter=TAB):
            if row["kind"] != "method":
                continue
            path = os.path.join(sources, row["path"])
            data = open(path, "rb").read()
            start, end = int(row["start_byte"]), int(row["end_byte"])
            first = data.count(b"\n", 0, start) + 1
            last = data.count(b"\n", 0, end) + 1
            spans.setdefault(os.path.basename(row["path"]), []).append((first, last, row["atom_id"]))
    return spans


def flag_names(flags):
    names = [n for bit, n in ((ACC_STATIC, "static"), (ACC_PRIVATE, "private"), (ACC_BRIDGE, "bridge"),
                              (ACC_SYNTHETIC, "synthetic")) if flags & bit]
    return "|".join(names) or "-"


def main():
    javap, classes, sources, atoms_gz, out = sys.argv[1:6]
    spans = atom_spans(atoms_gz, sources)
    rows = []
    root = os.path.join(classes, PACKAGE)
    for name in sorted(os.listdir(root)):
        if not name.endswith(".class"):
            continue
        path = os.path.join(root, name)
        methods, source = parse_class(path)
        cross = javap_offsets(javap, path)
        by_name = {}
        for m_name, last in cross:
            by_name.setdefault(m_name, []).append(last)
        seen = {}
        for m_name, desc, flags, length, lines in methods:
            k = seen.get(m_name, 0)
            seen[m_name] = k + 1
            lasts = by_name.get(m_name, [])
            last = lasts[k] if k < len(lasts) else -1
            if last >= length:
                raise SystemExit(f"{name} {m_name}{desc}: javap offset {last} >= code_length {length}")
            first_line, last_line, atom = (min(lines), max(lines), "") if lines else (0, 0, "")
            if lines:
                inside = [s for s in spans.get(source, []) if s[0] <= first_line and last_line <= s[1]]
                if inside:
                    best = min(inside, key=lambda s: s[1] - s[0])
                    first_line, last_line, atom = best
            rows.append((name[:-len(".class")], m_name, desc, flag_names(flags), str(length), str(last), source,
                         str(first_line), str(last_line), str(last_line - first_line + 1 if lines else 0), atom,
                         "yes" if length > MAX_INLINE_SIZE else "no", "yes" if length > FREQ_INLINE_SIZE else "no",
                         "yes" if length > HUGE_METHOD_LIMIT else "no"))
    rows.sort(key=lambda r: (r[0], int(r[7]), r[1], r[2]))
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(TAB.join(HEADER) + NL + "".join(TAB.join(r) + NL for r in rows))
    big = [r for r in rows if r[12] == "yes"]
    print(f"methods={len(rows)} gt_MaxInlineSize={sum(r[11] == 'yes' for r in rows)} "
          f"gt_FreqInlineSize={len(big)} gt_HugeMethodLimit={sum(r[13] == 'yes' for r in rows)}")
    for r in sorted(big, key=lambda r: -int(r[4])):
        print(f"  {r[0]}.{r[1]} {r[4]} bytes, lines {r[7]}-{r[8]}")


main()
