#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""L-Hash v1 Python oracle over the input stream of M3HashFamilyDump (lane L).

usage: python -I -B lhash_stream.py CPU_LHASH_PY INPUTS_TSV OUT_TSV [--text-mode] [--range=FROM:TO]

INPUTS_TSV rows are "index TAB base64(bytes)" (M3HashFamilyDump --inputs=). For every input the LOCKED reference
(cpu_lhash.py, loaded from the path given, read only) computes tokens() and hash_ids() over the bytes decoded as UTF-8
with errors="ignore" (the Java side decodes with CodingErrorAction.IGNORE), and writes the Java dump's L-line format:
L<index> TAB byteLength TAB nTokens TAB h1 TAB h2 TAB h3 TAB h4 TAB h5Count TAB h5Fold TAB similarityBits, hashes as
16 lower-case hex digits, similarity(current, previous) as the 16-hex IEEE-754 bits of the double (the previous of
input 0 is the empty source). --text-mode first applies the universal-newline translation of a text-mode open()
(CRLF -> LF, then lone CR -> LF), which is how the reference reads a file (oracle_lhash.py, the cpu-lhash skill).
--range=FROM:TO writes only the inputs FROM <= index < TO (the input before FROM is hashed for the similarity), so
differential.sh runs bounded shards in parallel and concatenates them. Prints one summary line.
"""
import base64
import hashlib
import importlib.util
import struct
import sys
from pathlib import Path


def load(path):
    spec = importlib.util.spec_from_file_location("cpu_lhash_reference", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main(argv):
    reference = load(argv[1])
    text_mode = "--text-mode" in argv[4:]
    bounds = [arg.split("=", 1)[1].split(":") for arg in argv[4:] if arg.startswith("--range=")]
    first, last = (int(bounds[0][0]), int(bounds[0][1])) if bounds else (0, 1 << 62)
    previous = reference.hash_ids(reference.tokens(""))
    count = 0
    with open(argv[2], encoding="ascii") as inputs, open(argv[3], "w", encoding="ascii", newline="\n") as out:
        for row in inputs:
            index, payload = row.rstrip("\n").split("\t")
            position = int(index)
            if position < first - 1:
                continue
            if position >= last:
                break
            data = base64.b64decode(payload)
            source = data.decode("utf-8", errors="ignore")
            if text_mode:
                source = source.replace("\r\n", "\n").replace("\r", "\n")
            current = reference.hash_ids(reference.tokens(source))
            if position < first:
                previous = current
                continue
            similarity = reference.similarity(current, previous)
            out.write("L%s\t%d\t%d\t%016x\t%016x\t%016x\t%016x\t%d\t%016x\t%s\n" % (
                index, len(data), current["n_tokens"], current["h1"], current["h2"], current["h3"], current["h4"],
                len(current["h5"]), reference._fnv(current["h5"]), struct.pack(">d", similarity).hex()))
            previous = current
            count += 1
    digest = hashlib.sha256(Path(argv[1]).read_bytes().replace(b"\r\n", b"\n")).hexdigest()
    print("LHASH_STREAM inputs=%d mode=%s reference_sha256_lf=%s" % (count, "text" if text_mode else "bytes", digest))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
