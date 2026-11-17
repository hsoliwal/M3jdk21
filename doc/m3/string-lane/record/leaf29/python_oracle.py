# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <corpus.bin> <answers.bin.gz>: CPython 3.12 as the oracle of partition, rpartition, rsplit and
splitLines of the SPLIT family.

corpus.bin (written by SplitFamilyProof --write-corpus, big-endian): int separator count, then each separator as int
unit count + UTF-16 units; int maxsplit count, then each maxsplit as int; int text count, then each text as int unit
count + UTF-16 units. Each unit sequence is decoded as UTF-16 with surrogatepass, so a surrogate pair becomes one code
point and a lone surrogate stays a code point of its own, as Python holds the string.

answers.bin.gz (gzip, big-endian), per text in corpus order: for each separator partition then rpartition; for each
separator and each maxsplit rsplit(sep, maxsplit); then splitlines(False) and splitlines(True). Each answer is an int
part count, or -1 when Python raises ValueError, then each part as int unit count + UTF-16 units (surrogatepass)."""
import gzip
import struct
import sys


def read_units(data, pos):
    (count,) = struct.unpack_from(">i", data, pos)
    pos += 4
    return data[pos:pos + 2 * count].decode("utf-16-be", "surrogatepass"), pos + 2 * count


def read_int(data, pos):
    return struct.unpack_from(">i", data, pos)[0], pos + 4


def write_answer(out, call):
    try:
        parts = call()
    except ValueError:
        out.write(struct.pack(">i", -1))
        return
    out.write(struct.pack(">i", len(parts)))
    for part in parts:
        units = part.encode("utf-16-be", "surrogatepass")
        out.write(struct.pack(">i", len(units) // 2))
        out.write(units)


def main():
    data = open(sys.argv[1], "rb").read()
    pos = 0
    count, pos = read_int(data, pos)
    separators = []
    for _ in range(count):
        sep, pos = read_units(data, pos)
        separators.append(sep)
    count, pos = read_int(data, pos)
    maxsplits = []
    for _ in range(count):
        value, pos = read_int(data, pos)
        maxsplits.append(value)
    count, pos = read_int(data, pos)
    answers = 0
    with gzip.open(sys.argv[2], "wb", compresslevel=6) as out:
        for _ in range(count):
            text, pos = read_units(data, pos)
            for sep in separators:
                write_answer(out, lambda: text.partition(sep))
                write_answer(out, lambda: text.rpartition(sep))
            for sep in separators:
                for maxsplit in maxsplits:
                    write_answer(out, lambda: text.rsplit(sep, maxsplit))
            write_answer(out, lambda: text.splitlines(False))
            write_answer(out, lambda: text.splitlines(True))
            answers += 2 * len(separators) + len(separators) * len(maxsplits) + 2
    print("python\t" + sys.version.split()[0] + "\ttexts\t" + str(count) + "\tanswers\t" + str(answers))


main()
