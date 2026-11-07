#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""One mutant per ADAPT class of recipe retained-port-20261107 (m3/llm), killed by the module's own tests.

The only ADAPT class is ChromeMlTaskService (the CONST_KEBAB schema id rename). Its mutant drops the surrogate guard
of plan(): a chunk may then end between a high and a low surrogate, and ChromeMlTaskServiceMigrationTest
(summaryAndKeyPointsRetainExactCoverageAndThreeWayReduction, PR #2108's own test) must fail on
assertEquals(7999, plan.getFirst().end()). The mutant is applied to a copy of m3/llm under --scratch (the worktree is
never modified), built with the same Maven settings as the recipe gates, and the surefire exit code decides:
non-zero = KILLED, zero = SURVIVED. MUTANTS.tsv is written next to this script.

usage: python -I mutants.py --module DIR --scratch DIR --settings FILE --repo DIR [--java-home DIR] [--tmp DIR]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import shutil
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
LEAF = "retained-port-20261107"
MUTANTS = (
    {
        "id": "M1",
        "file": "src/main/java/jdk/m3/llm/chrome/ChromeMlTaskService.java",
        "anchor": "            if (end < source.length() && Character.isHighSurrogate(source.charAt(end - 1))) end--;\n",
        "replacement": "            if (end < source.length() && Character.isHighSurrogate(source.charAt(end - 1))) end += 0;\n",
        "test": "ChromeMlTaskServiceMigrationTest",
        "note": "plan() surrogate guard removed: a segment may split a surrogate pair; the test expects end 7999",
    },
)
COLUMNS = ("mutant", "file", "anchor_sha256", "replacement_sha256", "test", "state", "evidence")


def sha(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def run_mutant(mutant: dict, module: Path, scratch: Path, settings: Path, repo: Path, java_home: str | None,
               tmp: Path) -> tuple[str, str]:
    copy = scratch / mutant["id"] / "m3" / "llm"
    if copy.exists():
        shutil.rmtree(copy)
    shutil.copytree(module, copy, ignore=shutil.ignore_patterns("target"))
    target = copy / mutant["file"]
    text = target.read_text(encoding="utf-8")
    if text.count(mutant["anchor"]) != 1:
        return "REFUSED", f"anchor occurs {text.count(mutant['anchor'])} times"
    target.write_text(text.replace(mutant["anchor"], mutant["replacement"], 1), encoding="utf-8", newline="\n")
    env = dict(os.environ)
    if java_home:
        env["JAVA_HOME"] = java_home
    env["MAVEN_OPTS"] = f"-Djava.io.tmpdir={tmp}"
    log = scratch / f"{mutant['id']}-mvn-test.log"
    mvn = shutil.which("mvn.cmd") or shutil.which("mvn") or "mvn"
    command = [mvn, "-B", "-ntp", "-T1", "-s", str(settings), f"-Dmaven.repo.local={repo}",
               f"-DargLine=-Djava.io.tmpdir={tmp}", f"-Dtest={mutant['test']}", "-Dsurefire.failIfNoSpecifiedTests=false",
               "-f", str(copy / "pom.xml"), "test"]
    with log.open("w", encoding="utf-8") as handle:
        proc = subprocess.run(command, cwd=copy, stdout=handle, stderr=subprocess.STDOUT, env=env)
    summary = ""
    for line in log.read_text(encoding="utf-8", errors="replace").splitlines():
        if "Tests run:" in line and "-- in" not in line:
            summary = line.strip()
    state = "KILLED" if proc.returncode != 0 else "SURVIVED"
    return state, f"mvn exit={proc.returncode}; {summary}; log {log}"


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--module", required=True)
    parser.add_argument("--scratch", required=True)
    parser.add_argument("--settings", required=True)
    parser.add_argument("--repo", required=True)
    parser.add_argument("--java-home", default=None)
    parser.add_argument("--tmp", default=None)
    args = parser.parse_args(argv)
    scratch = Path(args.scratch).resolve()
    scratch.mkdir(parents=True, exist_ok=True)
    tmp = Path(args.tmp).resolve() if args.tmp else scratch / "tmp"
    tmp.mkdir(parents=True, exist_ok=True)
    rows = []
    killed = 0
    for mutant in MUTANTS:
        state, evidence = run_mutant(mutant, Path(args.module).resolve(), scratch, Path(args.settings).resolve(),
                                     Path(args.repo).resolve(), args.java_home, tmp)
        killed += state == "KILLED"
        rows.append((mutant["id"], "m3/llm/" + mutant["file"], sha(mutant["anchor"]), sha(mutant["replacement"]),
                     mutant["test"], state, mutant["note"] + "; " + evidence))
        print(f"{mutant['id']} {state} {evidence}")
    with (RECIPE_DIR / "MUTANTS.tsv").open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# SPDX-License-Identifier: Apache-2.0\n")
        handle.write(f"# mutants of {LEAF}: one per ADAPT class, applied to a copy of m3/llm, killed by the module's own"
                     " surefire run (mutants.py)\n")
        handle.write("\t".join(COLUMNS) + "\n")
        for row in rows:
            handle.write("\t".join(row) + "\n")
    print(f"mutants={len(rows)} killed={killed}")
    return 0 if killed == len(rows) else 1


if __name__ == "__main__":
    sys.exit(main())
