# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""pattern_matrix.py <atoms_dir> <fdp_image> <out.tsv> : NUMBER-PARSE-PATTERN v1 over FastDoubleParser (CPU, read-only).
Each type is placed on the closed axes the library is generated along:
  role    FACADE (public XParser) | GRAMMAR_TEMPLATE (Abstract...From<carrier>) | INSTANCE (X...From<carrier>) |
          BIG_TEMPLATE (AbstractBig*Parser) | KERNEL (Fast*Math, FastDoubleSwar, FftMultiplier, BigSignificand,
          ParseDigitsTask*) | BASE (AbstractNumberParser, AbstractFloatValueParser) | RESIDUE (@IAntiPattern)
  grammar JAVA | JSON | -      target DOUBLE | FLOAT | BIG_DECIMAL | BIG_INTEGER | FLOATING_POINT | -
  carrier CHAR_SEQUENCE | CHAR_ARRAY | BYTE_ARRAY | -
plus the per-file leaf count and structural-class membership from ATOMS.tsv.gz, and JDK_NEED: which cells java.base
uses to serve Double.parseDouble / Float.parseFloat (Java grammar; a String's LATIN1 bytes or its chars)."""
import gzip
import os
import re
import sys

atoms, image, out = sys.argv[1:4]
leaves = {}
with gzip.open(os.path.join(atoms, "fdp", "ATOMS.tsv.gz"), "rt", encoding="utf-8") as f:
    head = next(f).rstrip("\n").split("\t")
    pi = head.index("path")
    for line in f:
        p = line.split("\t")[pi]
        leaves[os.path.basename(p)[:-5]] = leaves.get(os.path.basename(p)[:-5], 0) + 1
CARRIER = (("FromCharSequence", "CHAR_SEQUENCE"), ("FromCharArray", "CHAR_ARRAY"), ("FromByteArray", "BYTE_ARRAY"),
           ("CharSequence", "CHAR_SEQUENCE"), ("CharArray", "CHAR_ARRAY"), ("ByteArray", "BYTE_ARRAY"))
rows = []
for f in sorted(os.listdir(os.path.join(image, "ch", "randelshofer", "fastdoubleparser"))):
    if not f.endswith(".java") or f == "package-info.java":
        continue
    n = f[:-5]
    carrier = next((c for k, c in CARRIER if n.endswith(k)), "-")
    grammar = "JSON" if n.startswith(("Json", "AbstractJson")) else "JAVA" if "Java" in n else "-"
    target = ("BIG_DECIMAL" if "BigDecimal" in n else "BIG_INTEGER" if "BigInteger" in n else
              "FLOAT" if re.search(r"Float(Bits|Parser)", n) else "DOUBLE" if re.search(r"Double(Bits|Parser)", n) else
              "FLOATING_POINT" if "FloatingPoint" in n else "-")
    if n.endswith("Parser") and not n.startswith("Abstract"):
        role = "FACADE"
    elif n.startswith("AbstractBig"):
        role = "BIG_TEMPLATE"
    elif n.startswith("Abstract") and carrier != "-":
        role = "GRAMMAR_TEMPLATE"
    elif n in ("AbstractNumberParser", "AbstractFloatValueParser"):
        role = "BASE"
    elif carrier != "-":
        role = "INSTANCE"
    elif n.startswith(("Fast", "Fft", "BigSignificand", "ParseDigitsTask")):
        role = "KERNEL"
    else:
        role = "RESIDUE"
    need = "-"
    if grammar == "JAVA" and target in ("DOUBLE", "FLOAT", "FLOATING_POINT") and carrier in ("CHAR_SEQUENCE", "BYTE_ARRAY"):
        need = "YES"
    elif role in ("BASE",) or n in ("FastDoubleMath", "FastFloatMath", "FastDoubleSwar", "FastIntegerMath"):
        need = "YES"
    elif target in ("BIG_DECIMAL", "BIG_INTEGER") or n.startswith(("Fft", "BigSignificand", "ParseDigitsTask")):
        need = "LATER (BigDecimal/BigInteger leaf)"
    elif grammar == "JSON":
        need = "NO (JSON grammar: the lexer validates; jackson routes to Double.parseDouble)"
    elif carrier == "CHAR_ARRAY":
        need = "NO (no char[] entry in java.base parse APIs)"
    rows.append((n, role, grammar, target, carrier, str(leaves.get(n, 0)), need))
with open(out, "w", encoding="utf-8", newline="\n") as w:
    w.write("type\trole\tgrammar\ttarget\tcarrier\tleaves\tjdk_need\n" + "".join("\t".join(r) + "\n" for r in rows))
from collections import Counter
print("roles", dict(Counter(r[1] for r in rows)))
print("jdk_need", dict(Counter(r[6].split(" ")[0] for r in rows)))
