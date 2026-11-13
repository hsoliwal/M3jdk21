# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""method_bytecode.py <javap> <pre_classes> <post_classes> <out_tsv> : METHOD BYTECODE IDENTITY v1 (CPU).

Per-method form of BYTECODE_IDENTITY v1 (make/m3/tooling/jdk-superset/hashing/tools/bytecode_identity.py, hashing lane):
a literal replaced by a constant variable (static final with a constant initializer) is folded back by javac, so the
method keeps its instructions. For every class file present in pre_classes and post_classes, 'javap -c -p' of both
with the constant-pool indexes (#n) removed is split into methods (declaration line + Code listing); each method is
IDENTICAL, CHANGED, ONLY_PRE or ONLY_POST. Writes class, method, status; prints the counts. Exit status 0 always: the
listing is evidence (which methods changed beyond constant folding), not a gate."""
import os
import re
import subprocess
import sys

CP_INDEX = re.compile(r"#\d+ *")
METHOD_HEAD = re.compile(r"^  \S.*\);$|^  static \{\};$|^  \S.*\) throws .*;$")


def javap(tool, path):
    out = subprocess.run([tool, "-c", "-p", path], capture_output=True, text=True, check=True).stdout
    return [CP_INDEX.sub("# ", ln.rstrip()) for ln in out.splitlines()]


def methods(lines):
    """declaration -> listing of every method of one javap -c -p output."""
    out, head = {}, None
    for ln in lines:
        if METHOD_HEAD.match(ln):
            head = ln.strip()
            out[head] = []
        elif head is not None and ln.startswith("    "):
            out[head].append(ln)
        elif ln and not ln.startswith(" "):
            head = None
    return out


def main(argv):
    tool, pre, post, out_path = argv[1:5]
    rows = []
    for root, _, names in sorted(os.walk(pre)):
        for name in sorted(n for n in names if n.endswith(".class")):
            rel = os.path.relpath(os.path.join(root, name), pre).replace(os.sep, "/")
            if not os.path.isfile(os.path.join(post, rel)):
                rows.append((rel, "*", "ONLY_PRE"))
                continue
            m0, m1 = methods(javap(tool, os.path.join(pre, rel))), methods(javap(tool, os.path.join(post, rel)))
            for head in sorted(set(m0) | set(m1)):
                status = ("ONLY_POST" if head not in m0 else "ONLY_PRE" if head not in m1
                          else "IDENTICAL" if m0[head] == m1[head] else "CHANGED")
                rows.append((rel, head, status))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("class\tmethod\tstatus\n")
        f.writelines("\t".join(r) + "\n" for r in rows)
    counts = {}
    for r in rows:
        counts[r[2]] = counts.get(r[2], 0) + 1
    print("method_bytecode methods=%d %s" % (len(rows), " ".join("%s=%d" % kv for kv in sorted(counts.items()))))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
