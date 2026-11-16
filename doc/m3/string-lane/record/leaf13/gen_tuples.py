# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""gen_tuples.py <base M3StringTuple.java> <head M3StringTuple.java> <segments_block.txt> <out-dir>

Writes the two classes the proof compiles: M3StringTuple = stub header + the member text of the head source from the
first new constant to the end of the nested Segments class (checked to be the block file, verbatim), and
M3StringTupleBase = stub header + the charAt method of the base source. Only the headers are written here."""
import sys

base_path, head_path, block_path, out = sys.argv[1:5]
read = lambda p: open(p, encoding="utf-8", newline="").read().replace("\r", "")
head, base, block = read(head_path), read(base_path), read(block_path)
if block not in head:
    sys.exit("BLOCK_NOT_VERBATIM_IN_HEAD")
start = base.index("    @Override\n    char charAt(int index) {")
old = base[start:base.index("\n    }\n", start) + len("\n    }\n")]
HEADER = """import java.util.Objects;

final class %(name)s extends M3StringOwner {
    final M3String left;
    final M3String right;
    final int height;

    %(name)s(M3String left, M3String right) {
        super(left.length() + right.length());
        this.left = left;
        this.right = right;
        this.height = 1 + Math.max(childHeight(left), childHeight(right));
    }

    private static int childHeight(M3String value) {
        M3StringOwner owner = value.owner();
        return value.start() == 0 && value.length() == owner.length && owner instanceof %(name)s tuple
                ? tuple.height : 0;
    }

"""
CHAIN = """
    static M3StringTupleBase chain(int leaves) {
        M3String text = new M3String(new Leaf(new char[] {'q'}), 0, 1);
        M3StringTupleBase tuple = null;
        for (int index = 0; index < leaves; index++) {
            M3String next = new M3String(new Leaf(new char[] {(char) ('a' + index % 26), 'x'}), 0, 2);
            tuple = new M3StringTupleBase(text, next);
            text = new M3String(tuple, 0, tuple.length);
        }
        return tuple;
    }
"""
open(out + "/M3StringTuple.java", "w", encoding="utf-8", newline="").write(HEADER % {"name": "M3StringTuple"} + block + "}\n")
open(out + "/M3StringTupleBase.java", "w", encoding="utf-8", newline="").write(
    HEADER % {"name": "M3StringTupleBase"} + old + CHAIN + "}\n")
print("block_verbatim_in_head\t1")
print("base_charAt_lines\t" + str(old.count("\n")))
