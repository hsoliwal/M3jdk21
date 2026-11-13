# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""native_relocate.py <NATIVE_RELOCATE.tsv> <donor dir> <repo root>

NATIVE_SYMBOL_RELOCATE v1.1 (2026-11-09) copies donor JNI C into src/jdk.m3.collections/share/native/libm3collections and
renames only what the move requires. Each TSV row is: donor file (relative to the donor dir), destination (relative to
libm3collections), donor class binary name, M3 class binary name (nested classes with $), library, lineage. A donor class of `-` copies a file
that has no JNI names (a shared header) without renaming.

For each row:
- every `Java_<mangled donor class>_` prefix becomes `Java_<mangled M3 class>_`;
- `#include "<donor generated header>.h"` becomes the M3 generated header name (javac -h names nested classes with
  `_` for `$`, while symbols use `_00024`);
- one lineage comment line is inserted after the leading comment block.
Nothing else changes; the result is checked by compiling against the force-included M3 javac -h headers
(make/m3/collections). Rows sharing a destination are applied in order to the same file. Prints one line per file."""
import os
import re
import sys


def mangle(binary):
    return binary.replace("_", "_1").replace("$", "_00024").replace(".", "_")


def header(binary):
    return binary.replace(".", "_").replace("$", "_") + ".h"


tsv, donor, root = sys.argv[1:4]
dest_root = os.path.join(root, "src", "jdk.m3.collections", "share", "native", "libm3collections")
rows = [line.rstrip("\n").split("\t") for line in open(tsv, encoding="utf-8") if line.strip() and not line.startswith("donor")]
texts, lineage = {}, {}
for src, dst, old, new, _library, origin in rows:
    text = texts.get(dst) or open(os.path.join(donor, src), encoding="utf-8").read()
    if old == "-":  # v1.1: a shared header or helper with no JNI names is copied unchanged
        texts[dst] = text
        lineage.setdefault(dst, []).append(origin)
        continue
    before = len(re.findall(r"\bJava_" + re.escape(mangle(old)) + "_", text))
    text = re.sub(r"\bJava_" + re.escape(mangle(old)) + "_", "Java_" + mangle(new) + "_", text)
    text = text.replace('#include "' + header(old) + '"', '#include "' + header(new) + '"')
    texts[dst] = text
    lineage.setdefault(dst, []).append(f"{origin} ({old} -> {new}, {before} symbols)")
    if before == 0:
        raise SystemExit(f"NATIVE_SYMBOL_RELOCATE no Java_{mangle(old)}_ symbol in {src}")
for dst, text in texts.items():
    note = "/* Lineage: " + "; ".join(lineage[dst]) + ". JNI names relocated by NATIVE_SYMBOL_RELOCATE v1. */\n"
    end = text.find("*/") + 2 if text.lstrip().startswith("/*") else 0
    newline = text.find("\n", end)
    text = text[:newline + 1] + note + text[newline + 1:] if end else note + text
    path = os.path.join(dest_root, dst)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as w:
        w.write(text)
    print("NATIVE_SYMBOL_RELOCATE", dst, len(re.findall(r"\bJava_\w+", text)), "JNI names")
