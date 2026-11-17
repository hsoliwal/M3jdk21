# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <dir>: CPython 3.12 as the oracle of String.zeroFill (str.zfill).
Reads corpus.bin (int32 count, then per text int32 units + UTF-16LE units) and params.txt (op, abs|rel, n, pad, texts)
written by BuildFamilyProof --write-corpus. Each text becomes a Python str by UTF-16 surrogatepass decoding (a pair is
one code point, a lone surrogate stays one), so len() counts code points. For every zfill row, in order, writes the
answer of text.zfill(width) for the first <texts> texts to zfill.ans, encoded the same way back to UTF-16LE units."""
import os
import struct
import sys


def read_corpus(path):
    data = open(path, "rb").read()
    count = struct.unpack_from("<i", data, 0)[0]
    offset, texts = 4, []
    for _ in range(count):
        units = struct.unpack_from("<i", data, offset)[0]
        offset += 4
        texts.append(data[offset:offset + 2 * units].decode("utf-16-le", "surrogatepass"))
        offset += 2 * units
    return texts


def main():
    folder = sys.argv[1]
    texts = read_corpus(os.path.join(folder, "corpus.bin"))
    out = bytearray()
    rows = 0
    for line in open(os.path.join(folder, "params.txt"), encoding="ascii"):
        op, kind, n, _pad, count = line.rstrip("\n").split("\t")
        if op != "zfill":
            continue
        rows += 1
        for text in texts[:int(count)]:
            width = int(n) + (len(text.encode("utf-16-le", "surrogatepass")) // 2 if kind == "rel" else 0)
            units = text.zfill(width).encode("utf-16-le", "surrogatepass")
            out += struct.pack("<i", len(units) // 2) + units
    open(os.path.join(folder, "zfill.ans"), "wb").write(out)
    print("python", sys.version.split()[0], "zfill rows", rows, "texts", len(texts))


main()
