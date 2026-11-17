# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal and others
"""mutate.py <draft-dir> <work-dir> <donor-jars> <mode> [<mutant-name>,...]

ESCAPE_MARKUP_MUTATION v1 (2026-11-17). Sensitivity check of the proof (CPU only, no regex): for each mutant (one
verbatim text replacement in escape_markup_family.txt), writes the mutated family into a scratch leaf, inserts the
eleven texts into the stock String.java as run_proof.sh does, compiles java.base, and runs EscapeMarkupFamilyProof
check in one mode (flat, m3-facts or m3-scan; compact strings on) on the oracle answers already in <work-dir>. A
mutant is killed when the run fails. Prints one line per mutant and deletes its class output.
"""
import shutil
import subprocess
import sys
import zipfile

draft, work, donor, mode = sys.argv[1:5]
ONLY = sys.argv[5].split(",") if len(sys.argv) > 5 else None
JH = "C:/Program Files/Java/jdk-21"
MUTANTS = [
    ("lenient partial rank", "if (cmp < -10 && (partial < 0 || rank < cmp)) {",
     "if (cmp < -10 && (partial < 0 || rank > cmp)) {"),
    ("Windows-1252 index", "MARKUP_C1.charAt(point - 0x80)", "MARKUP_C1.charAt(point - 0x7F)"),
    ("python 32 code points", "while (end < length && points < 32) {", "while (end < length && points < 33) {"),
    ("commons int overflow", "if (digit < 0 || number > Integer.MAX_VALUE) return -1;",
     "if (digit < 0 || number >= Integer.MAX_VALUE) return -1;"),
    ("facts gate units", "if (!markupKeeps(rule, unit) && facts.mayContainCodeUnit((char) unit)) return false;",
     "if (!markupKeeps(rule, unit) && unit == '&' && facts.mayContainCodeUnit((char) unit)) return false;"),
    ("window seam", "if (to < length && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;",
     "if (to < 0 && count > 1 && Character.isHighSurrogate(window[count - 1])) count--;"),
    ("xml level of U+0085", "return point >= 0xA0 || point == 0x85 ? 2 : 1;",
     "return point >= 0xA0 || point == 0x86 ? 2 : 1;"),
    ("strict upper-case X", "boolean hex = charAt(at + 2) == 'x' && at + 3 < length;",
     "boolean hex = (charAt(at + 2) | 0x20) == 'x' && at + 3 < length;"),
    ("html5 escape name", "escape[e++] = (long) first << 32 | (n - (legacy ? 2 : 1));",
     "escape[e++] = (long) first << 32 | (n - 1);"),
    ("commons html3 limit", "if (rule == UNMARKUP_HTML3 && point > 0xFF) point = -1;",
     "if (rule == UNMARKUP_HTML3 && point > 0x100) point = -1;"),
    ("lenient partial dropped", "return partial < 0 ? -1 : -2 - partial;", "return -1;"),
    ("python 30 code points", "while (end < length && points < 32) {", "while (end < length && points < 30) {"),
    ("commons html3 limit 0x2FF", "if (rule == UNMARKUP_HTML3 && point > 0xFF) point = -1;",
     "if (rule == UNMARKUP_HTML3 && point > 0x2FF) point = -1;"),
]
PIECES = ("difference_base.txt", "search_family.txt", "edge_family.txt", "class_family.txt", "compare_family.txt",
          "slice_family.txt", "build_family.txt", "split_family.txt", "replace_family.txt", "case_family.txt",
          "escape_markup_family.txt")
props = {"flat": [], "m3-facts": ["-Dm3.stub.force=true", "-Dm3.stub.facts=true"],
         "m3-scan": ["-Dm3.stub.force=true"]}[mode]


def read(path):
    return open(path, encoding="utf-8", newline="").read().replace("\r", "")


stock = zipfile.ZipFile(JH + "/lib/src.zip").read("java.base/java/lang/String.java").decode("utf-8")
stock = stock.replace("\r", "").split("\n")
anchor = [i for i, l in enumerate(stock) if l == "    public boolean contains(CharSequence s) {"][0]
start = max(i for i in range(anchor) if stock[i] == "    /**")
stub = read(draft + "/proof/M3StringStub.java.txt")
cut = stub.index("/** The fields and class rules of M3StringFacts")
family = read(draft + "/escape_markup_family.txt")
killed = 0
for name, old, new in MUTANTS:
    if ONLY is not None and name not in ONLY:
        continue
    if family.count(old) != 1:
        print("MUTANT_TEXT_NOT_ONCE\t" + name)
        sys.exit(2)
    texts = [read(draft + "/" + p) for p in PIECES[:-1]] + [family.replace(old, new)]
    body = "\n".join(t.strip("\n") for t in texts).split("\n")
    lines = stock[:start] + body + ["", "    M3String m3() { return M3String.FORCE ? new M3String(this) : null; }",
                                    ""] + stock[start:]
    gen, out, harness = work + "/mut/gen/java/lang", work + "/mut/classes", work + "/mut/harness"
    shutil.rmtree(work + "/mut", ignore_errors=True)
    for d in (gen, out, harness):
        subprocess.run(["mkdir", "-p", d], check=True)
    open(gen + "/String.java", "w", encoding="utf-8", newline="").write("\n".join(lines))
    open(gen + "/M3String.java", "w", encoding="utf-8", newline="").write(stub[:cut])
    open(gen + "/M3StringFacts.java", "w", encoding="utf-8", newline="").write("package java.lang;\n\n" + stub[cut:])
    javac = [JH + "/bin/javac", "-nowarn", "-XDignore.symbol.file", "-implicit:none", "-d", out, "--patch-module",
             "java.base=" + work + "/mut/gen", gen + "/String.java", gen + "/M3String.java", gen + "/M3StringFacts.java"]
    if subprocess.run(javac, capture_output=True).returncode != 0:
        print("COMPILE_FAIL\t" + name)
        continue
    subprocess.run([JH + "/bin/javac", "-nowarn", "-d", harness, "--patch-module", "java.base=" + out, "-cp", donor,
                    draft + "/proof/EscapeMarkupFamilyProof.java"], check=True, capture_output=True)
    run = subprocess.run([JH + "/bin/java"] + props + ["--patch-module", "java.base=" + out, "-cp",
                         harness + ";" + donor, "EscapeMarkupFamilyProof", "check", work],
                         capture_output=True, text=True)
    failed = [l.split("\t")[0] for l in run.stdout.split("\n") if l.count("\t") == 3 and l.split("\t")[2] != "0"]
    killed += run.returncode != 0
    print("%s\t%s\tfailing_checks=%d\t%s" % ("KILLED" if run.returncode != 0 else "SURVIVED", name, len(failed),
                                            "; ".join(failed[:3])))
shutil.rmtree(work + "/mut", ignore_errors=True)
print("mutants\t%d\tkilled\t%d" % (len(ONLY) if ONLY else len(MUTANTS), killed))
