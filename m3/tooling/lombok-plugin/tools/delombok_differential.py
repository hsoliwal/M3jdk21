# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""delombok_differential.py <stage_dir> <java> <lombok.jar> <module_dir>... : DELOMBOK-DIFFERENTIAL v1 (CPU).

For each vendored module that keeps its Lombok sources in upstream-lombok/ and its committed expansion in
src/main/java/ (M3JDK rewrite-* modules, expanded with Lombok 1.18.30), re-expand upstream-lombok/ with the given
jar under the module's UPSTREAM_LOMBOK.config, using the committed expansion's format (no @SuppressWarnings, no
@lombok.Generated, java.lang simple names, no delombok comment), then compare file by file after the documented
committed edits are undone on both sides: the NOTICE first line, lombok imports, blank lines and bare @NonFinal
marker lines; comments are dropped (1.18.48 adds constructor Javadoc that 1.18.30 did not; no class-file effect). Prints one line per module and writes <stage_dir>/DIFFERENTIAL.tsv (module, file, status, first
differing line). This is the D0 baseline of TEST_PLAN.md: it measures how far the reference expander moved between
the version M3JDK committed and the version the plugin is specified against. Read-only on the modules."""
import os
import re
import shutil
import subprocess
import sys

FORMAT = ["-f", "suppressWarnings:skip", "-f", "generated:skip", "-f", "javaLangAsFQN:skip",
          "-f", "generateDelombokComment:skip"]
NOTICE = "// NOTICE: modified file."
LOMBOK_IMPORT = re.compile(r"^\s*import\s+(static\s+)?lombok\.")
NONFINAL_MARKER = re.compile(r"^\s*@(lombok\.experimental\.)?NonFinal\s*$")
TOKEN = re.compile(r'"(?:\\.|[^"\\\n])*"' + r"|'(?:\\.|[^'\\\n])*'" + r"|/\*.*?\*/|//[^\n]*", re.S)
UPSTREAM = "upstream-lombok"
COMMITTED = os.path.join("src", "main", "java")
CONFIG = "UPSTREAM_LOMBOK.config"
HEADER = "module\tfile\tstatus\tfirst_difference\n"


def normalized(path):
    with open(path, encoding="utf-8") as f:
        text = f.read()
    if text.startswith(NOTICE):
        text = text.split("\n", 1)[1]
    lines = TOKEN.sub(lambda m: m.group(0) if m.group(0)[0] in "\"'" else "", text).splitlines()
    return [l.rstrip() for l in lines if l.strip() and not LOMBOK_IMPORT.match(l) and not NONFINAL_MARKER.match(l)]


def first_difference(a, b):
    for i, (x, y) in enumerate(zip(a, b)):
        if x != y:
            return "%d: %s | %s" % (i + 1, x.strip()[:80], y.strip()[:80])
    return "length %d vs %d" % (len(a), len(b))


def module(stage, java, jar, mod):
    name = os.path.basename(os.path.normpath(mod))
    src, out = os.path.join(stage, name, "in"), os.path.join(stage, name, "out")
    shutil.rmtree(os.path.join(stage, name), ignore_errors=True)
    shutil.copytree(os.path.join(mod, UPSTREAM), src)
    shutil.copyfile(os.path.join(mod, CONFIG), os.path.join(src, "lombok.config"))
    p = subprocess.run([java, "-jar", jar, "delombok", "--encoding=UTF-8"] + FORMAT + ["-d", out, src],
                       capture_output=True, text=True)
    rows = []
    for root, _, files in os.walk(src):
        for f in sorted(x for x in files if x.endswith(".java")):
            rel = os.path.relpath(os.path.join(root, f), src)
            mine, theirs = os.path.join(out, rel), os.path.join(mod, COMMITTED, rel)
            if not os.path.exists(mine) or not os.path.exists(theirs):
                rows.append((name, rel.replace("\\", "/"), "MISSING", "expanded=%s committed=%s"
                             % (os.path.exists(mine), os.path.exists(theirs))))
                continue
            a, b = normalized(mine), normalized(theirs)
            rows.append((name, rel.replace("\\", "/"), "SAME" if a == b else "DIFF",
                         "-" if a == b else first_difference(a, b)))
    return p.returncode, len((p.stdout + p.stderr).splitlines()), sorted(rows)


def main(argv):
    stage, java, jar = argv[1:4]
    all_rows = []
    for mod in argv[4:]:
        code, diag_lines, rows = module(stage, java, jar, mod)
        same = sum(1 for r in rows if r[2] == "SAME")
        print("DELOMBOK-DIFFERENTIAL v1 %s exit=%d diag_lines=%d files=%d same=%d diff=%d missing=%d"
              % (os.path.basename(os.path.normpath(mod)), code, diag_lines, len(rows), same,
                 sum(1 for r in rows if r[2] == "DIFF"), sum(1 for r in rows if r[2] == "MISSING")))
        all_rows += rows
    with open(os.path.join(stage, "DIFFERENTIAL.tsv"), "w", encoding="utf-8", newline="\n") as f:
        f.write(HEADER)
        f.writelines("\t".join(r) + "\n" for r in all_rows)


if __name__ == "__main__":
    main(sys.argv)
