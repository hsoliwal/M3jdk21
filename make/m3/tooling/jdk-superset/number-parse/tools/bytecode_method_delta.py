# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""bytecode_method_delta.py <javap> <pre_classes> <post_classes> <out.tsv> [<class>#<method>...]

Gate BYTECODE_METHOD_DELTA v1 (per-method sibling of BYTECODE_IDENTITY v1): 'javap -c -p' of every class file of
pre_classes and of the same path in post_classes, constant-pool indexes removed, split into fields and one block per
method (its declaration line plus its Code listing). Rows: every method whose block differs, appears or disappears,
and every field that appears or disappears; plus one summary row per class. The verdict is PASS only when the changed
methods are exactly the named ones (class is the path without .class, e.g. jdk/internal/math/M3FastDoubleMath) and
every added field is private static final: so an in-file change can prove that the rest of the class, and every other
class (here the decimal fast path), compiles to the same instructions. The TSV starts with the Synexia SPDX header
('#' lines)."""
import os
import re
import subprocess
import sys

CP_INDEX = re.compile(r"#\d+ *")  # the index and javap's comment padding, which follows the index width
FIELD = re.compile(r"^  (?:(?:public|protected|private|static|final|volatile|transient) )*[\w.$<>\[\], ?]+ [\w$]+;$")
DECLARATION = re.compile(r"^  \S.*\)(?: throws [\w.$, ]+)?;$|^  static \{\};$")
NAME = re.compile(r"([\w$<>]+)\(")
NONE = "-"  # an empty cell (no trailing tabs in the TSV)
TSV_HEADER = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0\n"
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others\n")
FIRST_ALLOWED_ARG = 5  # argv: program, javap, pre_classes, post_classes, out.tsv, then the allowed methods


def javap(tool, path):
    out = subprocess.run([tool, "-c", "-p", path], capture_output=True, text=True, check=True).stdout
    return [CP_INDEX.sub("# ", ln.rstrip()) for ln in out.splitlines()]


def split(lines):
    """({field line}, {method declaration: listing}) of one javap -c -p listing."""
    fields, methods, current = set(), {}, None
    for ln in lines:
        if not ln or ln.startswith(("Compiled from", "}")) or not ln.startswith(" "):
            continue
        if FIELD.match(ln):
            fields.add(ln.strip())
            current = None
        elif DECLARATION.match(ln):
            current = ln.strip()
            methods[current] = []
        elif current is not None:
            methods[current].append(ln)
    return fields, methods


def method_name(declaration):
    if declaration == "static {};":
        return "<clinit>"
    match = NAME.search(declaration)
    return match.group(1) if match else declaration


def main(argv):
    tool, pre, post, out_path = argv[1:FIRST_ALLOWED_ARG]
    allowed = set(argv[FIRST_ALLOWED_ARG:])
    rows, changed, bad_fields = [], set(), 0
    for root, _, names in sorted(os.walk(pre)):
        for name in sorted(n for n in names if n.endswith(".class")):
            rel = os.path.relpath(os.path.join(root, name), pre).replace(os.sep, "/")
            klass = rel[:-len(".class")]
            f0, m0 = split(javap(tool, os.path.join(pre, rel)))
            f1, m1 = split(javap(tool, os.path.join(post, rel)))
            same = 0
            for decl in sorted(set(m0) | set(m1)):
                state = ("ADDED" if decl not in m0 else "REMOVED" if decl not in m1 else
                         "EQUAL" if m0[decl] == m1[decl] else "CHANGED")
                if state == "EQUAL":
                    same += 1
                    continue
                key = klass + "#" + method_name(decl)
                changed.add(key)
                rows.append((klass, "method", decl, state, len(m0.get(decl, ())), len(m1.get(decl, ()))))
            for field in sorted(f1 - f0):
                ok = field.startswith("private static final ")
                bad_fields += not ok
                rows.append((klass, "field", field, "ADDED" if ok else "ADDED_NOT_PRIVATE_STATIC_FINAL", NONE, NONE))
            for field in sorted(f0 - f1):
                bad_fields += 1
                rows.append((klass, "field", field, "REMOVED", NONE, NONE))
            rows.append((klass, "class", NONE, "METHODS_EQUAL=%d/%d" % (same, len(set(m0) | set(m1))), NONE, NONE))
    verdict = "PASS" if changed == allowed and bad_fields == 0 else "FAIL"
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(TSV_HEADER)
        f.write("class\tkind\tmember\tstate\tpre_lines\tpost_lines\n")
        for r in rows:
            f.write("\t".join(str(x) for x in r) + "\n")
        f.write("#verdict\t%s\tchanged=%s\tallowed=%s\tbad_fields=%d\n"
                % (verdict, ",".join(sorted(changed)), ",".join(sorted(allowed)), bad_fields))
    sys.stdout.write("CHANGED\t%s\nVERDICT\t%s\n" % (",".join(sorted(changed)) or "-", verdict))
    return 0 if verdict == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
