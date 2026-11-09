# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""perf_variants.py <repo> <base commit> <out dir> <java bin dir> <oracle classes> <perf classes> <rounds> <variant>...

The decision runs of the hashing JIT leaf, reproducible from the repository. Each variant is a java.base patch built
from the leaf's committed sources and its base commit (git show), and for the diagnostic reads an anchored edit of
M3ByteArrayAccess (each anchor must occur the stated number of times, else the tool stops). Variants:
  base    the leaf's base commit (the absorption with its review)
  unsafe  base + the leaf's byte[] access (the absorption's alternative: reads through jdk.internal.misc.Unsafe,
          XXH3 not split; its record is PERF.tsv's alternative-internal-unsafe rows)
  split   base + the leaf's M3XXH3 (XXH3_SPLIT v1 alone; reads through ByteArrayLittleEndian)
  d1      split + the leaf's byte[] access, each short-name read behind its own Preconditions.checkIndex, the check
          ByteArrayLittleEndian's VarHandle makes (index in [0, length - width]), then jdk.internal.misc.Unsafe
  d2      split + the leaf's byte[] access: the same reads unchecked (no entry check yet)
  d3      d2 with a checked and an unchecked instance, a final field choosing per read (the kernels get the
          unchecked one)
  v2      the leaf without ACCESS_SHORT_NAMES v1 (M3Access, M3ByteBufferAccess, M3CharSequenceAccess of the base)
  after   the leaf
d1 and d3 rebuild the leaf's scratch diagnostics from their description (the scratch builds were not kept). The
patches are compiled with java.base's flags (-Xlint:all -Werror). Then for each round 1..<rounds> and each variant
in the order given (interleaved, so the host's load spreads over all of them) one JVM runs M3HashAtomsPerf for the
groups in M3_PERF_GROUPS (default bytes,xxh3-64,xxh3-128) with -Dm3.hash.perf.run=<label>-<variant>-r<round>
(label M3_PERF_LABEL, default repro),
appending its rows to <out dir>/PERF_ROWS.tsv (the harness's columns: medians and bests of interleaved A/B rounds,
system load). Progress goes to stdout, one line per JVM.
"""
import os
import subprocess
import sys
import time

PACKAGE = os.path.join("jdk", "internal", "m3", "hash")
SRC = "src/java.base/share/classes/jdk/internal/m3/hash"
LEAF, BASE = "leaf", "base"
ACCESS = "M3ByteArrayAccess.java"
FLAGS = ["-g", "-Xlint:all", "-Werror", "-Xlint:-this-escape", "-XDstringConcat=inline"]
RUN = ["--add-opens", "java.base/jdk.internal.m3.hash=ALL-UNNAMED", "--add-exports",
       "java.base/sun.nio.ch=ALL-UNNAMED"]
GROUPS = os.environ.get("M3_PERF_GROUPS", "bytes,xxh3-64,xxh3-128")
LABEL = os.environ.get("M3_PERF_LABEL", "repro")
TIMEOUT_S = 1800
IMPORT = ("import jdk.internal.util.ByteArrayLittleEndian;\n",
          "import jdk.internal.util.ByteArrayLittleEndian;\nimport jdk.internal.util.Preconditions;\n", 1)
HELPER = ("    private static long address(final long offset) {\n"
          "        return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;\n"
          "    }\n",
          "    private static long address(final long offset) {\n"
          "        return Unsafe.ARRAY_BYTE_BASE_OFFSET + offset;\n"
          "    }\n"
          "\n"
          "    // diagnostic d1/d3 (tools/perf_variants.py): ByteArrayLittleEndian's per-read check\n"
          "    private static long checked(final byte[] input, final long offset, final int width) {\n"
          "        return Preconditions.checkIndex((int) offset, input.length - width + 1,"
          " Preconditions.AIOOBE_FORMATTER);\n"
          "    }\n", 1)
READS = (("Long", "Long.BYTES", 1), ("Int", "Integer.BYTES", 2), ("Short", "Short.BYTES", 2))
D1_EDITS = [IMPORT, HELPER] + [
    (f"UNSAFE.get{w}Unaligned(input, address(offset), false)",
     f"UNSAFE.get{w}Unaligned(input, address(checked(input, offset, {width})), false)", n) for w, width, n in READS]
D3_EDITS = [IMPORT, HELPER,
            ("    static final M3ByteArrayAccess INSTANCE;\n",
             "    static final M3ByteArrayAccess INSTANCE;\n    static final M3ByteArrayAccess CHECKED_INSTANCE;\n", 1),
            ("        INSTANCE = new M3ByteArrayAccess();\n",
             "        INSTANCE = new M3ByteArrayAccess(false);\n        CHECKED_INSTANCE = new M3ByteArrayAccess(true);\n",
             1),
            ("    private M3ByteArrayAccess() {}\n",
             "    // diagnostic d3 (tools/perf_variants.py): a checked and an unchecked instance, a final field per read\n"
             "    private final boolean checkReads;\n\n"
             "    private M3ByteArrayAccess(final boolean checkReads) {\n"
             "        this.checkReads = checkReads;\n"
             "    }\n", 1)] + [
    (f"UNSAFE.get{w}Unaligned(input, address(offset), false)",
     f"UNSAFE.get{w}Unaligned(input, address(checkReads ? checked(input, offset, {width}) : offset), false)", n)
    for w, width, n in READS]
SPLIT_FILES = {"M3XXH3.java": LEAF}
# variant -> (default source, per-file sources, edits)
VARIANTS = {
    "base": (BASE, {}, []),
    "unsafe": (BASE, {ACCESS: LEAF}, []),
    "split": (BASE, SPLIT_FILES, []),
    "d1": (BASE, dict(SPLIT_FILES, **{ACCESS: LEAF}), D1_EDITS),
    "d2": (BASE, dict(SPLIT_FILES, **{ACCESS: LEAF}), []),
    "d3": (BASE, dict(SPLIT_FILES, **{ACCESS: LEAF}), D3_EDITS),
    "v2": (LEAF, {"M3Access.java": BASE, "M3ByteBufferAccess.java": BASE, "M3CharSequenceAccess.java": BASE}, []),
    "after": (LEAF, {}, []),
}


def source(repo, base, which, name):
    if which == LEAF:
        with open(os.path.join(repo, SRC, name), encoding="utf-8", newline="") as f:
            return f.read()
    return subprocess.run(["git", "-C", repo, "show", f"{base}:{SRC}/{name}"], capture_output=True, check=True,
                          text=True, encoding="utf-8").stdout


def materialize(repo, base, out, variant):
    default, files, edits = VARIANTS[variant]
    psrc = os.path.join(out, variant, "psrc")
    target = os.path.join(psrc, PACKAGE)
    os.makedirs(target, exist_ok=True)
    names = sorted(n for n in os.listdir(os.path.join(repo, SRC)) if n.endswith(".java"))
    for name in names:
        text = source(repo, base, files.get(name, default), name)
        for old, new, count in edits if name == ACCESS else []:
            if text.count(old) != count:
                raise SystemExit(f"{variant}: {old[:60]!r} occurs {text.count(old)} times in {name}, not {count}")
            text = text.replace(old, new)
        with open(os.path.join(target, name), "w", encoding="utf-8", newline="") as w:
            w.write(text)
    return psrc, [os.path.join(target, n) for n in names]


def build(repo, base, out, jbin, variant):
    psrc, sources = materialize(repo, base, out, variant)
    patch = os.path.join(out, variant, "patch")
    subprocess.run([os.path.join(jbin, "javac")] + FLAGS + ["--patch-module", "java.base=" + psrc, "-d", patch]
                   + sources, check=True)
    return patch


def main():
    repo, base, out, jbin, oracle, perfc, rounds = sys.argv[1:8]
    variants = sys.argv[8:]
    unknown = [v for v in variants if v not in VARIANTS]
    if unknown or not variants:
        raise SystemExit(f"unknown variants {unknown}; known: {', '.join(VARIANTS)}")
    patches = {v: build(repo, base, out, jbin, v) for v in variants}
    print(f"built {', '.join(variants)} into {out}", flush=True)
    rows = os.path.join(out, "PERF_ROWS.tsv")
    for r in range(1, int(rounds) + 1):
        for v in variants:
            name = f"{LABEL}-{v}-r{r}"
            t0 = time.time()
            with open(os.path.join(out, name + ".log"), "w", encoding="utf-8") as log:
                p = subprocess.run([os.path.join(jbin, "java"), "-Xmx512m", "--patch-module",
                                    "java.base=" + patches[v]] + RUN
                                   + ["-Dm3.hash.oracle=" + oracle, "-Dm3.hash.perf.only=" + GROUPS,
                                      "-Dm3.hash.perf.run=" + name, "-Dm3.hash.perf.out=" + rows, "-cp", perfc,
                                      "M3HashAtomsPerf"], stdout=log, stderr=subprocess.STDOUT, timeout=TIMEOUT_S)
            print(f"[{time.strftime('%H:%M:%S')}] {name}: exit {p.returncode} in {time.time() - t0:.0f} s", flush=True)
            if p.returncode:
                raise SystemExit(f"{name} failed, see {name}.log")


main()
