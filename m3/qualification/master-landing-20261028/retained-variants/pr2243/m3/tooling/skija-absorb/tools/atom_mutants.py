# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_mutants.py <repo> <work_dir> <m3_classes_dir> <test_classes_dir> <oracle_jars> <junit_console_jar>

Mutation check of the jdk.m3.graphics atom tests (CPU only, deterministic). Each mutant is one source edit of an
absorbed class (MUTANTS below: the edit must match exactly once, or the given number of times, all replaced). The
tool copies the class source into <work_dir>/mutants/<id>/src, applies the edit, compiles that one file against the
module classes into <work_dir>/mutants/<id>/classes, and runs the generated JUnit tests with the mutant classes first
on the class path, with identity hashes pinned as the jtreg @run lines do, then again without (result_unpinned: there
a hashCode with an identity-hashed part compares by partition only). A mutant is KILLED when at least one atom test
reports a mismatching case (its ATOM row) and the run fails. Writes m3/tooling/skija-absorb/leaf1/ATOM_MUTANTS.tsv and
prints one line per mutant; the exit status follows the pinned results (1 when a mutant survives or does not apply).
javac/java come from $JDK_BIN, else $JAVA_HOME/bin, else PATH.
"""
import os
import shutil
import subprocess
import sys

SRC = "src/jdk.m3.graphics/share/classes"
LEDGER = "m3/tooling/skija-absorb/leaf1/ATOM_MUTANTS.tsv"
PINNED = ["-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=2"]
MAX_SHOWN = 160
TAB = "\t"
NL = "\n"
ORIGIN_2243 = "#2243"
ORIGIN_REVIEW = "#2243 review"
MUTANTS = (
    ("MUT1_rect_width", ORIGIN_2243, "jdk/m3/graphics/M3Rect.java",
     "        return _right - _left;", "        return _left - _right;", "types.Rect"),
    ("MUT2_colortype_bpp", ORIGIN_2243, "jdk/m3/graphics/M3ColorType.java",
     "            case RGBA_F32:           return 16;", "            case RGBA_F32:           return 8;",
     "skija.ColorType"),
    ("MUT3_blendmode_order", ORIGIN_2243, "jdk/m3/graphics/M3BlendMode.java",
     "    CLEAR,\n\n    /** r = s */\n    SRC,", "    SRC,\n\n    /** r = s */\n    CLEAR,", "skija.BlendMode"),
    ("MUT4_color4f_alias", ORIGIN_2243, "jdk/m3/graphics/M3Color4f.java",
     "        if (_a == 1.0F) return this;", "        // mutant: no shortcut", "skija.Color4f", 2),
    ("REV1_rect_makexywh_checks", ORIGIN_REVIEW, "jdk/m3/graphics/M3Rect.java",
     '        if (w < 0) throw new IllegalArgumentException("Rect::makeXYWH expected w >= 0, got: " + w);\n'
     '        if (h < 0) throw new IllegalArgumentException("Rect::makeXYWH expected h >= 0, got: " + h);',
     '        if (h < 0) throw new IllegalArgumentException("Rect::makeXYWH expected h >= 0, got: " + h);\n'
     '        if (w < 0) throw new IllegalArgumentException("Rect::makeXYWH expected w >= 0, got: " + w);',
     "types.Rect"),
    ("REV2_rect_equals_bottom", ORIGIN_REVIEW, "jdk/m3/graphics/M3Rect.java",
     "        if (java.lang.Float.compare(this.getBottom(), other.getBottom()) != 0) return false;",
     "        // mutant: _bottom ignored by equals", "types.Rect"),
    ("REV3_colorinfo_equals_alphatype", ORIGIN_REVIEW, "jdk/m3/graphics/M3ColorInfo.java",
     "        if (this$_alphaType == null ? other$_alphaType != null : !this$_alphaType.equals(other$_alphaType)) "
     "return false;", "        // mutant: alphaType ignored by equals", "skija.ColorInfo"),
    ("REV4_colorinfo_hash_null_constant", ORIGIN_REVIEW, "jdk/m3/graphics/M3ColorInfo.java",
     "        result = result * PRIME + ($_colorSpace == null ? 43 : $_colorSpace.hashCode());",
     "        result = result * PRIME + ($_colorSpace == null ? 0 : $_colorSpace.hashCode());", "skija.ColorInfo"),
)


def tool(name):
    for env, sub in (("JDK_BIN", ""), ("JAVA_HOME", "bin")):
        if os.environ.get(env):
            return os.path.join(os.environ[env], sub, name)
    return name


def apply(repo, work, mid, rel, find, replace, count):
    text = open(os.path.join(repo, SRC, rel), encoding="utf-8").read()
    if text.count(find) != count:
        return None, "edit matches %d times" % text.count(find)
    src = os.path.join(work, "mutants", mid, "src", rel)
    os.makedirs(os.path.dirname(src), exist_ok=True)
    with open(src, "w", encoding="utf-8", newline=NL) as f:
        f.write(text.replace(find, replace))
    return src, "-"


def run(repo, work, m3_classes, tests, oracle, console, mutant):
    mid, origin, rel, find, replace, expected = mutant[:6]
    count = mutant[6] if len(mutant) > 6 else 1
    shutil.rmtree(os.path.join(work, "mutants", mid), ignore_errors=True)
    src, note = apply(repo, work, mid, rel, find, replace, count)
    if src is None:
        return (mid, origin, rel, expected, "NOT_APPLIED", "-", note, "-")
    classes = os.path.join(work, "mutants", mid, "classes")
    c = subprocess.run([tool("javac"), "--release", "21", "-proc:none", "-nowarn", "-encoding", "UTF-8", "-cp",
                        m3_classes, "-d", classes, src], capture_output=True, text=True)
    if c.returncode != 0:
        return (mid, origin, rel, expected, "NOT_COMPILED", "-", c.stderr.strip().splitlines()[0][:MAX_SHOWN], "-")
    cp = os.pathsep.join([classes, tests, m3_classes])
    pinned = junit(work, mid, cp, tests, oracle, console, PINNED, expected)
    unpinned = junit(work, mid, cp, tests, oracle, console, [], expected)
    return (mid, origin, rel, expected, pinned[0], pinned[1], pinned[2], unpinned[0])


def junit(work, mid, cp, tests, oracle, console, options, expected):
    """(KILLED or SURVIVED, failing atoms, first mismatch of the expected atom, else of the first failing atom)."""
    r = subprocess.run([tool("java")] + options + ["-Dm3.graphics.oracle=" + oracle, "-jar", console, "execute",
                        "--disable-banner", "--disable-ansi-colors", "--details=summary", "-cp", cp,
                        "--include-classname=.*", "--scan-class-path=" + tests], capture_output=True, text=True,
                       encoding="utf-8", errors="replace")
    name = "junit.log" if options else "junit_unpinned.log"
    with open(os.path.join(work, "mutants", mid, name), "w", encoding="utf-8", newline=NL) as f:
        f.write(r.stdout + r.stderr)
    lines = r.stdout.splitlines()
    atoms = [line.split(TAB) for line in lines if line.startswith("ATOM" + TAB)]
    failing = sorted(a[1] for a in atoms if int(a[4]) > 0)
    heads = [i for i, line in enumerate(lines) if "mismatching cases; first" in line and i + 1 < len(lines)]
    own = [i for i in heads if (" %s: " % expected) in lines[i]]
    first = lines[(own or heads)[0] + 1].strip() if heads else "-"
    killed = r.returncode != 0 and bool(failing)
    return "KILLED" if killed else "SURVIVED", ",".join(failing) or "-", first[:MAX_SHOWN].replace(TAB, " ")


def main(argv):
    if len(argv) != 7:
        sys.stderr.write(__doc__)
        return 2
    repo, work, m3_classes, tests, oracle, console = argv[1:]
    out = [run(repo, work, m3_classes, tests, oracle, console, m) for m in MUTANTS]
    with open(os.path.join(repo, LEDGER), "w", encoding="utf-8", newline=NL) as f:
        f.write(TAB.join(("mutant", "origin", "source", "expected_atom", "result", "failing_atoms", "first_failure",
                          "result_unpinned")) + NL)
        for row in out:
            f.write(TAB.join(row) + NL)
    for row in out:
        print("MUTANT%s%s%s%s%s%s%sunpinned=%s" % (TAB, row[0], TAB, row[4], TAB, row[5], TAB, row[7]))
    killed = sum(r[4] == "KILLED" for r in out)
    print("mutants=%d killed=%d killed_unpinned=%d" % (len(out), killed, sum(r[7] == "KILLED" for r in out)))
    return 0 if killed == len(out) else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
