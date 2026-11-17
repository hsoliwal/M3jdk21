# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""python_oracle.py <work-dir>: CPython 3.12 as the oracle of the ENCODE family.
Texts are UTF-16 code units in hex (big endian), one per line, as EncodeFamilyProof corpus writes them; a text becomes
a Python str with surrogatepass (a pair is one code point, a lone surrogate stays one) and back. A cell is '=' + an
ASCII result, 'h' + a result in hex units, a number, or '!' when Python raises. Writes:
PY_ENC.txt  per line of enc_texts.txt: quote(s, safe='') TAB quote_plus(s) TAB codecs.encode(s, 'rot13') TAB
            len(s.encode('utf-8'))
PY_SAFE.txt per safe set of safes.txt, per line of safe_texts.txt: quote(s, safe) TAB quote_plus(s, safe)
PY_DEC.txt  per line of dec_texts.txt: unquote(s) TAB unquote_plus(s)"""
import codecs
import os
import sys
from urllib.parse import quote, quote_plus, unquote, unquote_plus


def dec(h):
    return bytes.fromhex(h).decode("utf-16-be", "surrogatepass")


def enc(s):
    return s.encode("utf-16-be", "surrogatepass").hex().upper()


def lines(path):
    return open(path, encoding="ascii", newline="").read().split("\n")[:-1]


def ascii_cell(f):
    try:
        return "=" + f()
    except UnicodeEncodeError:
        return "!"


def length_cell(s):
    try:
        return str(len(s.encode("utf-8")))
    except UnicodeEncodeError:
        return "!"


def main():
    work = sys.argv[1]
    texts = [dec(h) for h in lines(os.path.join(work, "enc_texts.txt"))]
    with open(os.path.join(work, "PY_ENC.txt"), "w", encoding="ascii", newline="") as w:
        for s in texts:
            w.write("\t".join((ascii_cell(lambda: quote(s, safe="")), ascii_cell(lambda: quote_plus(s)),
                               "h" + enc(codecs.encode(s, "rot13")), length_cell(s))) + "\n")
    safes = [dec(h) for h in lines(os.path.join(work, "safes.txt"))]
    safe_texts = [dec(h) for h in lines(os.path.join(work, "safe_texts.txt"))]
    with open(os.path.join(work, "PY_SAFE.txt"), "w", encoding="ascii", newline="") as w:
        for safe in safes:
            for s in safe_texts:
                w.write(ascii_cell(lambda: quote(s, safe)) + "\t" + ascii_cell(lambda: quote_plus(s, safe)) + "\n")
    dec_texts = [dec(h) for h in lines(os.path.join(work, "dec_texts.txt"))]
    with open(os.path.join(work, "PY_DEC.txt"), "w", encoding="ascii", newline="") as w:
        for s in dec_texts:
            w.write("h" + enc(unquote(s)) + "\th" + enc(unquote_plus(s)) + "\n")
    print("python", sys.version.split()[0], "enc_texts", len(texts), "safes", len(safes), "safe_texts",
          len(safe_texts), "dec_texts", len(dec_texts))


main()
