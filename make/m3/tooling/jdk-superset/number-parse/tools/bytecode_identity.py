# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""bytecode_identity.py <javap> <pre_classes> <post_classes> <out.tsv>

Gate BYTECODE_IDENTITY v1 for LITERAL_CONSTANTS: a hoisted literal is a constant variable, so javac folds it back and
every method body must stay instruction-for-instruction identical. For each class file of pre_classes (same relative
path in post_classes): 'javap -c -p' of both, constant-pool indexes (#n) removed, then
  methods  the instruction listing of every method (signature + Code) must be equal
  fields   pre fields must all remain; post may add only 'private static final' fields (the named constants)
One row per class: class methods_equal fields_added fields_removed verdict. Exit 0 only if every class is IDENTICAL."""
import os
import re
import subprocess
import sys

CP_INDEX = re.compile(r"#\d+ *")  # the index and javap's comment padding, which follows the index width
FIELD = re.compile(r"^  (?:(?:public|protected|private|static|final|volatile|transient) )*[\w.$<>\[\], ?]+ [\w$]+;$")


def javap(tool, path):
    out = subprocess.run([tool, "-c", "-p", path], capture_output=True, text=True, check=True).stdout
    return [CP_INDEX.sub("# ", ln.rstrip()) for ln in out.splitlines()]


def split(lines):
    """(field lines, method listing lines) of one javap -c -p listing; blank separator lines are layout, not code."""
    fields, methods = [], []
    for ln in lines:
        if ln:
            (fields if FIELD.match(ln) else methods).append(ln)
    return fields, methods


def main(argv):
    tool, pre, post, out_path = argv[1:5]
    rows, ok = [], True
    for root, _, names in sorted(os.walk(pre)):
        for name in sorted(n for n in names if n.endswith(".class")):
            rel = os.path.relpath(os.path.join(root, name), pre).replace(os.sep, "/")
            f0, m0 = split(javap(tool, os.path.join(pre, rel)))
            f1, m1 = split(javap(tool, os.path.join(post, rel)))
            added = [f for f in f1 if f not in f0]
            removed = [f for f in f0 if f not in f1]
            bad_added = [f for f in added if not f.lstrip().startswith("private static final ")]
            same = m0 == m1
            verdict = "IDENTICAL" if same and not removed and not bad_added else "DIFFERENT"
            ok &= verdict == "IDENTICAL"
            rows.append((rel, "yes" if same else "no", len(added), len(removed), verdict))
    with open(out_path, "w", encoding="utf-8", newline="\n") as f:
        f.write("class\tmethods_equal\tfields_added\tfields_removed\tverdict\n")
        for r in rows:
            f.write("\t".join(str(x) for x in r) + "\n")
    sys.stdout.write("CLASSES\t%d\tIDENTICAL\t%d\n" % (len(rows), sum(r[4] == "IDENTICAL" for r in rows)))
    return 0 if ok and rows else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
