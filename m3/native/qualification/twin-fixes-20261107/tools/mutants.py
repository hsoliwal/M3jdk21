#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Test-fails-on-master and mutant gates of recipe qualification/twin-fixes-20261107 (stdlib only).

The twins and their relocated tests come from ../EDITS.tsv: a MERGE3 row names the twin source of its leaf, a FILE row
under src/test/java names the relocated test of its leaf. Every row of ../MUTANTS.tsv edits one copy of a twin
(source=head: the module file; source=master: the pinned preimage blob of the MERGE3 row, read with git cat-file) by
exact find/replace pairs (each find must match exactly once; rows with the same id apply in order). The edited file
alone is compiled against the head (or master) classes into an overlay directory that precedes them on the class path,
and the JUnit console launcher runs the relocated tests plus the module's declared test family (FAMILY row). A mutant
is KILLED when the relocated test of its leaf fails. Gate rows: MASTER (master classes, no edit; the relocated test of
each leaf is compiled and run; the expect column says FAIL when it must fail on master, RECORD when its verdict is
recorded only, NOCOMPILE when it must not compile), SHIM (an edited master copy, recorded), HEAD (no edit; every
selected test passes).
usage: mutants.py --module DIR --head-classes DIR --head-test-classes DIR --master-classes DIR --console JAR
                  --out DIR [--java-home DIR] [--tmp DIR] [--library-path DIR] [--leaf X]
                  (exit 1 when a gate fails or a mutant survives)"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent


def read_tsv(path: Path) -> list:
    lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = lines[0].split("\t")
    return [dict(zip(header, line.split("\t"))) for line in lines[1:]]


def unescape(text: str) -> str:
    return text.replace("\\n", "\n").replace("\\t", "\t")


def class_name(test_path: str) -> str:
    parts = Path(test_path).with_suffix("").parts
    return ".".join(parts[parts.index("java") + 1:])


class Runner:
    def __init__(self, args):
        self.args, self.out = args, Path(args.out)
        self.java = Path(args.java_home) / "bin"
        self.tmp = "-Djava.io.tmpdir=" + args.tmp
        self.sep = ";" if sys.platform.startswith("win") else ":"
        self.library = ["-Djava.library.path=" + args.library_path] if args.library_path else []

    def javac(self, dest: Path, cp: list, sources: list) -> tuple[int, str]:
        shutil.rmtree(dest, ignore_errors=True)
        dest.mkdir(parents=True)
        done = subprocess.run([str(self.java / "javac"), "-J" + self.tmp, "--release", "21", "-proc:none",
                               "-implicit:none", "-nowarn", "-cp", self.sep.join(cp), "-d", str(dest), *sources],
                              capture_output=True, text=True)
        return done.returncode, done.stdout + done.stderr

    def junit(self, label: str, cp: list, classes: list, with_library: list = ()) -> dict:
        """Runs the classes without a library path, and the with_library classes in a second JVM with it."""
        result: dict = {}
        plain = [name for name in classes if name not in with_library]
        for suffix, names, library in (("", plain, []), ("-lib", [n for n in classes if n in with_library], self.library)):
            if not names:
                continue
            reports = self.out / ("reports-" + label + suffix)
            shutil.rmtree(reports, ignore_errors=True)
            select = [arg for name in names for arg in ("--select-class", name)]
            subprocess.run([str(self.java / "java"), self.tmp, "-XX:-UsePerfData", *library, "-jar", self.args.console,
                            "execute", "--disable-banner", "--details=none", "--reports-dir", str(reports),
                            "--class-path", self.sep.join(cp), *select], capture_output=True, text=True)
            result.update({name: [0, 0] for name in names})
            for xml in reports.glob("*.xml"):
                for case in ET.parse(xml).getroot().iter("testcase"):
                    name = case.get("classname", "")
                    if name in result:
                        result[name][0] += 1
                        result[name][1] += any(case.find(k) is not None for k in ("failure", "error"))
        return result


def edited_source(group: list, module: Path, twin: str, blob: str) -> str:
    if group[0]["source"] == "head":
        text = (module / twin).read_text(encoding="utf-8")
    else:
        text = subprocess.run(["git", "-C", str(module), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout.decode("utf-8")
    for row in group:
        if row["find"] == "-":
            continue
        find, replace = unescape(row["find"]), unescape(row["replace"])
        if text.count(find) != 1:
            raise SystemExit("mutant %s: find matches %d times" % (row["id"], text.count(find)))
        text = text.replace(find, replace)
    return text


def detail_of(res: dict) -> str:
    return " ".join("%s=%d/%d" % (name.rsplit(".", 1)[-1], f, n) for name, (n, f) in res.items())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    for name in ("--module", "--head-classes", "--head-test-classes", "--master-classes", "--console", "--out"):
        parser.add_argument(name, required=True)
    parser.add_argument("--java-home", default="C:/Program Files/Java/jdk-21")
    parser.add_argument("--tmp", default=".")
    parser.add_argument("--library-path", default="", help="java.library.path of the LIBRARY leaf's JUnit runs (JNI fixture)")
    parser.add_argument("--extra-classpath", default="", help="dependency jars of the module (path separator joined)")
    parser.add_argument("--leaf", default="", help="only this leaf's rows")
    args = parser.parse_args()
    run, module, out = Runner(args), Path(args.module), Path(args.out)
    extra = [p for p in args.extra_classpath.split(run.sep) if p]
    args.head_classes = run.sep.join([args.head_classes, *extra])
    args.master_classes = run.sep.join([args.master_classes, *extra])
    out.mkdir(parents=True, exist_ok=True)
    edits = read_tsv(HERE / "EDITS.tsv")
    twins = {r["leaf"]: (r["target"], r["host_blob"]) for r in edits if r["mode"] == "MERGE3"}
    tests = {r["leaf"]: r["target"] for r in edits if r["mode"] == "FILE" and r["target"].startswith("src/test/java/")}
    leaves = [leaf for leaf in twins if not args.leaf or leaf == args.leaf]
    rows = read_tsv(HERE / "MUTANTS.tsv")
    family = [name for r in rows if r["kind"] == "FAMILY" for name in r["find"].split(",")]
    library_leaves = [r["leaf"] for r in rows if r["kind"] == "LIBRARY"]
    if library_leaves and not args.library_path:
        raise SystemExit("MUTANTS.tsv declares a LIBRARY leaf: pass --library-path (the built fixture library)")
    groups: dict = {}
    for row in rows:
        if row["kind"] not in ("FAMILY", "LIBRARY") and (not args.leaf or row["leaf"] in (args.leaf, "-")):
            groups.setdefault(row["id"], []).append(row)
    test_src = {leaf: str(module / tests[leaf]) for leaf in leaves}
    test_cls = {leaf: class_name(tests[leaf]) for leaf in leaves}
    with_library = [test_cls[leaf] for leaf in leaves if leaf in library_leaves]
    rc, log = run.javac(out / "tests-head", [args.head_classes, args.console], list(test_src.values()))
    if rc:
        raise SystemExit("relocated tests do not compile against head:\n" + log)
    lines, failed = [], 0
    for mid, group in groups.items():
        kind, leaf, source = group[0]["kind"], group[0]["leaf"], group[0]["source"]
        base = args.head_classes if source == "head" else args.master_classes
        if kind == "MASTER":  # unedited master classes: each relocated test compiled and run against them
            parts, ok = [], True
            for test_leaf in leaves:
                expect = next((r["expect"] for r in rows if r["id"] == mid and r["leaf"] == test_leaf), group[0]["expect"])
                rc_t, log_t = run.javac(out / ("tests-master-" + test_leaf), [base, args.console], [test_src[test_leaf]])
                if rc_t:
                    errors = sorted({line.split(": error: ", 1)[1] for line in log_t.splitlines() if ": error: " in line})
                    parts.append("%s=javac_exit_%d errors=%d (%s)" % (test_cls[test_leaf].rsplit(".", 1)[-1], rc_t,
                                                                      len(errors), "; ".join(errors)))
                    ok &= expect == "NOCOMPILE"
                    continue
                res = run.junit(mid + "-" + test_leaf, [str(out / ("tests-master-" + test_leaf)), base, args.console],
                                [test_cls[test_leaf]], with_library)
                n, f = res[test_cls[test_leaf]]
                parts.append("%s=%d/%d failed expect=%s" % (test_cls[test_leaf].rsplit(".", 1)[-1], f, n, expect))
                ok &= (f > 0 and n > 0) if expect == "FAIL" else (n > 0) if expect == "RECORD" else False
            failed += not ok
            lines.append("%s %s %s %s" % (mid, kind, " ".join(parts), "PASS" if ok else "FAIL"))
            continue
        twin, blob = twins[leaf] if leaf in twins else (None, None)
        if source == "head" and group[0]["find"] == "-":
            cp = [str(out / "tests-head"), args.head_test_classes, base, args.console]
        else:
            src_dir = out / ("src-" + mid)
            shutil.rmtree(src_dir, ignore_errors=True)
            target = src_dir / Path(twin).name
            src_dir.mkdir(parents=True)
            target.write_text(edited_source(group, module, twin, blob), encoding="utf-8", newline="\n")
            overlay = out / ("overlay-" + mid)
            rc, log = run.javac(overlay, [base], [str(target)])
            if rc:
                raise SystemExit("row %s does not compile:\n%s" % (mid, log))
            cp = [str(overlay), str(out / "tests-head"), args.head_test_classes, base, args.console]
        selected = [test_cls[leaf]] if kind == "SHIM" else [*test_cls.values(), *family]
        res = run.junit(mid, cp, selected, with_library)
        if kind == "HEAD":
            ok = all(f == 0 and n > 0 for n, f in res.values())
            verdict = "PASS" if ok else "FAIL"
        elif kind == "SHIM":
            ok, verdict = True, "RECORDED"
        else:
            ok = res[test_cls[leaf]][1] > 0
            verdict = "KILLED" if ok else "SURVIVED"
        failed += not ok
        lines.append("%s %s leaf=%s %s failed/total: %s" % (mid, kind, leaf, verdict, detail_of(res)))
    (out / "MUTANTS_RESULT.txt").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    print("\n".join(lines))
    print("MUTANTS rows=%d failed=%d" % (len(lines), failed))
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
