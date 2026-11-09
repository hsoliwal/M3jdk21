#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Fail-on-master and mutant gates of an m3/collections owner-edit recipe (stdlib + the JDK + the JUnit console).

Rows of ../MUTANTS.tsv (columns id, kind, target, jvm, tests, find, replace, note):
  MASTER  run the tests on the master classes (head test classes); expect at least one failure (the probe is new).
  HEAD    run the tests on the head classes; expect every test to pass.
  MUTANT  copy the head source of target (module-relative), apply the exact find/replace pairs (backslash-n = newline;
          each find must match exactly once; rows with the same id apply in order), compile that file alone against the
          head classes into an overlay that precedes them on the class path, run the tests; KILLED = a test fails.
jvm is jit or xint (the JUnit console JVM gets -Xint: allocation probes are then interpreter-exact). tests is a comma
list of simple class names in jdk.m3.collections. Exit 1 when a gate fails or a mutant survives.
usage: mutants.py --module DIR --head-classes DIR --head-test-classes DIR --master-classes DIR --console JAR
                  --deps CP --out DIR [--java-home DIR] [--tmp DIR]"""
from __future__ import annotations

import argparse
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
PKG = "jdk.m3.collections."


def read_tsv(path: Path) -> list:
    lines = [line for line in path.read_text(encoding="utf-8").splitlines() if line and not line.startswith("#")]
    header = lines[0].split("\t")
    return [dict(zip(header, line.split("\t"))) for line in lines[1:]]


def unescape(text: str) -> str:
    return text.replace("\\n", "\n").replace("\\t", "\t")


class Runner:
    def __init__(self, args):
        self.args, self.out = args, Path(args.out)
        self.java = Path(args.java_home) / "bin"
        self.tmp = "-Djava.io.tmpdir=" + args.tmp
        self.sep = ";" if sys.platform.startswith("win") else ":"
        self.deps = [d for d in args.deps.split(self.sep) if d]

    def javac(self, dest: Path, cp: list, source: Path) -> tuple[int, str]:
        shutil.rmtree(dest, ignore_errors=True)
        dest.mkdir(parents=True)
        done = subprocess.run([str(self.java / "javac"), "-J" + self.tmp, "--release", "21", "-proc:none",
                               "-implicit:none", "-nowarn", "-cp", self.sep.join(cp), "-d", str(dest), str(source)],
                              capture_output=True, text=True)
        return done.returncode, done.stdout + done.stderr

    def junit(self, label: str, cp: list, tests: list, jvm: str) -> tuple[int, int, list]:
        reports = self.out / ("reports-" + label)
        shutil.rmtree(reports, ignore_errors=True)
        select = [arg for name in tests for arg in ("--select-class", PKG + name)]
        flags = [self.tmp] + (["-Xint"] if jvm == "xint" else [])
        subprocess.run([str(self.java / "java"), *flags, "-jar", self.args.console, "execute", "--disable-banner",
                        "--details=none", "--reports-dir", str(reports), "--class-path", self.sep.join(cp), *select],
                       capture_output=True, text=True)
        run = failed = 0
        names = []
        for xml in reports.glob("*.xml"):
            for case in ET.parse(xml).getroot().iter("testcase"):
                run += 1
                if case.find("failure") is not None or case.find("error") is not None:
                    failed += 1
                    names.append(case.get("name", "?"))
        return run, failed, names

    def classpath(self, main: str, overlay: Path | None = None) -> list:
        head = [str(overlay)] if overlay else []
        return head + [self.args.head_test_classes, main] + self.deps

    def row(self, ident: str, rows: list) -> tuple[str, str]:
        first = rows[0]
        tests, jvm, kind = first["tests"].split(","), first["jvm"], first["kind"]
        if kind == "MASTER":
            run, failed, names = self.junit(ident, self.classpath(self.args.master_classes), tests, jvm)
            return ("PASS" if failed else "FAIL"), "master run=%d failed=%d %s" % (run, failed, names)
        if kind == "HEAD":
            run, failed, names = self.junit(ident, self.classpath(self.args.head_classes), tests, jvm)
            return ("PASS" if run and not failed else "FAIL"), "head run=%d failed=%d %s" % (run, failed, names)
        source = Path(self.args.module) / first["target"]
        text = source.read_text(encoding="utf-8")
        for r in rows:
            find, replace = unescape(r["find"]), unescape(r["replace"])
            if text.count(find) != 1:
                return "FAIL", "find matches %d times: %r" % (text.count(find), find[:60])
            text = text.replace(find, replace)
        work = self.out / ("mutant-" + ident)
        shutil.rmtree(work, ignore_errors=True)
        work.mkdir(parents=True)
        mutated = work / source.name
        mutated.write_text(text, encoding="utf-8", newline="")
        code, log = self.javac(work / "classes", [self.args.head_classes] + self.deps, mutated)
        if code:
            return "FAIL", "mutant does not compile: " + log.strip().splitlines()[0][:160]
        run, failed, names = self.junit(ident, self.classpath(self.args.head_classes, work / "classes"), tests, jvm)
        return ("KILLED" if failed else "SURVIVED"), "run=%d failed=%d %s" % (run, failed, names)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    for name in ("module", "head-classes", "head-test-classes", "master-classes", "console", "deps", "out"):
        parser.add_argument("--" + name, required=True)
    parser.add_argument("--java-home", default="C:/Program Files/Java/jdk-21")
    parser.add_argument("--tmp", default=".")
    args = parser.parse_args()
    runner = Runner(args)
    groups: dict = {}
    for r in read_tsv(HERE / "MUTANTS.tsv"):
        groups.setdefault(r["id"], []).append(r)
    bad = 0
    for ident, rows in groups.items():
        state, detail = runner.row(ident, rows)
        bad += state in ("FAIL", "SURVIVED")
        print("%s %s %s %s" % (ident, rows[0]["kind"], state, detail))
    print("MUTANTS rows=%d failed_or_survived=%d" % (len(groups), bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
