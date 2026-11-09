# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""jit_size.py <javap> <classes_dir> <ATOMS.tsv.gz> <src_root> <out.tsv> [<class-prefix>...]

Gate JIT_SIZE v1 (CPU, read-only): the bytecode size of every method of the classes under classes_dir (optionally only
classes whose simple name starts with one of the prefixes), against the HotSpot 21 inlining and compile limits:
  MaxInlineSize      35 bytes  a callee this small is inlined even from a cold call site
  FreqInlineSize    325 bytes  a hot call site inlines a callee up to this size ("hot method too big" above it)
  HugeMethodLimit  8000 bytes  with DontCompileHugeMethods (default) a larger method is never compiled
Sizes come from 'javap -c -p -l': the offset of the last instruction plus its length (a method ends in a return,
athrow or goto). A minimal class-file reader gives code_length for the same method (class-file order, which javap
keeps); the two must agree (size_check). The source span comes from the tree-sitter L-Hash atomization
(atomize_patternize.py ATOMS.tsv.gz): the atom of the same owner and name whose span holds most of the method's
LineNumberTable lines. Lines are counted from the atom's byte span in the source file under src_root (the start_line
/ end_line columns of ATOMS.tsv.gz are tree-sitter points, which this binding reports wrongly for some leaves).
Columns: class member descriptor bytecode_bytes size_check first_line source_lines atom_id jit_class
jit_class: ABSTRACT | TRIVIAL_INLINE (<= 35) | HOT_INLINE (<= 325) | TOO_BIG_TO_INLINE (<= 8000) | HUGE (> 8000).
Exit 0 when every size_check is OK."""
import gzip
import os
import re
import struct
import subprocess
import sys

MAX_INLINE_SIZE = 35
FREQ_INLINE_SIZE = 325
HUGE_METHOD_LIMIT = 8000
TERMINAL_LENGTH = {"return": 1, "ireturn": 1, "lreturn": 1, "freturn": 1, "dreturn": 1, "areturn": 1,
                   "athrow": 1, "goto": 3, "goto_w": 5}
INSN = re.compile(r"^\s+(\d+): (\w+)")
LINE = re.compile(r"^\s+line (\d+): \d+$")
MEMBER = re.compile(r"^  (?!\s)(.*\)(?: throws [\w.$, ]+)?;|static \{\};)$")
CP_SKIP = {3: 4, 4: 4, 5: 8, 6: 8, 7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}
CP_WIDE = {5, 6}


def class_methods(path):
    """[(name, descriptor, code_length or None)] in class-file order."""
    data = open(path, "rb").read()
    pos, count = 10, struct.unpack_from(">H", data, 8)[0]
    utf8, i = {}, 1
    while i < count:
        tag = data[pos]
        if tag == 1:
            n = struct.unpack_from(">H", data, pos + 1)[0]
            utf8[i] = data[pos + 3:pos + 3 + n].decode("utf-8", "replace")
            pos += 3 + n
        else:
            pos += 1 + CP_SKIP[tag]
        i += 2 if tag in CP_WIDE else 1
    pos += 6
    pos += 2 + 2 * struct.unpack_from(">H", data, pos)[0]

    def members(pos, want_code):
        """(position after the member table, [(name, descriptor, code_length)]) of a field or method table."""
        count, out = struct.unpack_from(">H", data, pos)[0], []
        pos += 2
        for _ in range(count):
            _, name, desc, attrs = struct.unpack_from(">HHHH", data, pos)
            pos += 8
            code = None
            for _ in range(attrs):
                aname, alen = struct.unpack_from(">HI", data, pos)
                if want_code and utf8[aname] == "Code":
                    code = struct.unpack_from(">I", data, pos + 10)[0]  # after max_stack, max_locals
                pos += 6 + alen
            out.append((utf8[name], utf8[desc], code))
        return pos, out

    pos, _ = members(pos, False)  # fields
    return members(pos, True)[1]


def javap_methods(tool, path):
    """[(member signature, last offset, last opcode, LineNumberTable lines)] in javap order."""
    out = subprocess.run([tool, "-c", "-p", "-l", path], capture_output=True, text=True, check=True).stdout
    rows, cur = [], None
    for ln in out.splitlines():
        m = MEMBER.match(ln)
        if m:
            cur = [m.group(1), None, None, set()]
            rows.append(cur)
            continue
        if cur is None:
            continue
        i = INSN.match(ln)
        if i and not ln.strip().startswith("line"):
            cur[1], cur[2] = int(i.group(1)), i.group(2)
            continue
        line = LINE.match(ln)
        if line:
            cur[3].add(int(line.group(1)))
    return rows


def load_atoms(path, src_root):
    """{(owner, name): [(start_line, end_line, atom_id)]} of method/constructor atoms; lines from the byte span."""
    spans, sources = {}, {}
    with gzip.open(path, "rt", encoding="utf-8") as f:
        head = next(f).rstrip("\n").split("\t")
        col = {k: head.index(k) for k in ("kind", "owner", "name", "start_byte", "end_byte", "atom_id", "path")}
        for ln in f:
            r = ln.rstrip("\n").split("\t")
            if r[col["kind"]] in ("method", "constructor"):
                rel = r[col["path"]]
                if rel not in sources:
                    sources[rel] = open(os.path.join(src_root, rel), "rb").read()
                data, b0, b1 = sources[rel], int(r[col["start_byte"]]), int(r[col["end_byte"]])
                first = data.count(b"\n", 0, b0) + 1
                key = (r[col["owner"]], r[col["name"]])
                spans.setdefault(key, []).append((first, first + data.count(b"\n", b0, b1), r[col["atom_id"]]))
    return spans


def jit_class(size):
    if size is None:
        return "ABSTRACT"
    if size <= MAX_INLINE_SIZE:
        return "TRIVIAL_INLINE"
    if size <= FREQ_INLINE_SIZE:
        return "HOT_INLINE"
    return "TOO_BIG_TO_INLINE" if size <= HUGE_METHOD_LIMIT else "HUGE"


def member_name(sig, simple):
    if sig == "static {};":
        return "<clinit>"
    head = sig.split("(", 1)[0].split()[-1]
    return "<init>" if head.rsplit(".", 1)[-1] == simple else head


def main(argv):
    tool, classes, atoms_path, src_root, out_path = argv[1:6]
    prefixes = tuple(argv[6:])
    spans = load_atoms(atoms_path, src_root)
    rows, ok = [], True
    for root, _, names in sorted(os.walk(classes)):
        for name in sorted(n for n in names if n.endswith(".class")):
            simple = name[:-6]
            if prefixes and not simple.startswith(prefixes):
                continue
            path = os.path.join(root, name)
            cf, jp = class_methods(path), javap_methods(tool, path)
            if len(cf) != len(jp):
                raise SystemExit("method count differs for %s: class file %d, javap %d" % (path, len(cf), len(jp)))
            for (mname, desc, code), (sig, last, op, lines) in zip(cf, jp):
                first = min(lines) if lines else None
                if mname != member_name(sig, simple):
                    raise SystemExit("order mismatch in %s: %s vs %s" % (path, mname, sig))
                size = None if last is None else last + TERMINAL_LENGTH.get(op, 0)
                check = "OK" if (size is None and code is None) or (op in TERMINAL_LENGTH and size == code) else \
                    "MISMATCH(javap=%s,classfile=%s,last=%s)" % (size, code, op)
                ok &= check == "OK"
                # atom owners are dotted nesting paths; an anonymous class is <anonymous>
                parts = ["<anonymous>" if p.isdigit() else p for p in simple.split("$")]
                owner = parts[0] if len(parts) == 1 else ".".join(parts)
                # the atom holding most of the method's lines (a constructor also carries field-initializer lines)
                held = [(sum(s <= n <= e for n in lines), (s, e, a))
                        for s, e, a in spans.get((owner, mname if mname != "<init>" else parts[-1]), [])]
                atom = max(held)[1] if held and max(held)[0] > 0 else None
                rows.append((simple, sig, desc, "-" if code is None else str(code), check,
                             "-" if first is None else str(first),
                             "-" if atom is None else str(atom[1] - atom[0] + 1), "-" if atom is None else atom[2],
                             jit_class(code)))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("class\tmember\tdescriptor\tbytecode_bytes\tsize_check\tfirst_line\tsource_lines\tatom_id\tjit_class\n")
        for r in rows:
            f.write("\t".join(r) + "\n")
    tally = {}
    for r in rows:
        tally[r[8]] = tally.get(r[8], 0) + 1
    sys.stdout.write("METHODS\t%d\t%s\tSIZE_CHECK\t%s\n" % (len(rows), " ".join("%s=%d" % kv for kv in sorted(tally.items())),
                                                          "OK" if ok else "MISMATCH"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
