#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Mutant-refusal gate MUTANT_REFUSAL v1 of a tests-only convergence leaf (crate tests already relocated).

Two checks; the module itself is never written (its mutated owner sources are hashed before and after):
(1) parity, with --donor-git: every row of CRATE_PARITY.tsv (next to this script) names a crate file at a donor
    revision by git blob and the module file the owner recipe relocated it to by sha256; both must match, so the
    crate's test bytes at that revision are on the module byte for byte (names mapped by the owner recipe).
(2) mutants: the module (pom.xml, its other top-level files and src/) is copied to <scratch>/control and to
    <scratch>/<id> for every MUTANTS.tsv row (next to this script). A row replaces exactly one line of one owner
    source (the original line must occur exactly once) and runs `mvn -o test -Dtest=<classes>` on the copy.
    The control copy must pass with tests > 0 and no failure. A KILL row must compile, report at least one failing
    or erroring test case and name failure_contains in one of them; a SURVIVE row (a documented gap of the crate's
    tests, not a crate-guarded property) must pass.
Prints PARITY / CONTROL / MUTANT lines and MUTANT_REFUSAL PASS|FAIL; writes <scratch>/MUTANTS_RESULT.tsv.

usage: python -I mutants.py --scratch DIR [--donor-git DIR] [--module DIR] [--maven-repo DIR] [--mvn CMD] [--only ID]
"""
from __future__ import annotations

import argparse
import hashlib
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

TOOL = "mutants.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
PARITY_NAME = "CRATE_PARITY.tsv"
MUTANTS_NAME = "MUTANTS.tsv"
RESULT_NAME = "MUTANTS_RESULT.tsv"
RESULT_COLUMNS = ("id", "expect", "state", "tests", "failures", "errors", "skipped", "failing_classes",
                  "first_failure", "seconds")
CONTROL = "control"
KILL = "KILL"
SURVIVE = "SURVIVE"
MESSAGE_LIMIT = 300
LINES = re.compile(rb"[^\n]*\n|[^\n]+$")
EOL = re.compile(rb"\r?\n$")


def read_tsv(path: Path) -> list:
    rows, header = [], None
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("#"):
            continue
        cells = line.split("\t")
        if header is None:
            header = cells
            continue
        rows.append(dict(zip(header, cells)))
    return rows


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def parity(module: Path, donor: Path) -> bool:
    ok = True
    for row in read_tsv(RECIPE_DIR / PARITY_NAME):
        spec = row["donor_rev"] + ":" + row["donor_path"]
        blob = subprocess.run(["git", "-C", str(donor), "rev-parse", spec], check=True,
                              capture_output=True).stdout.decode().strip()
        target_sha = sha256_file(module / row["target_path"])
        good = blob == row["donor_blob"] and target_sha == row["target_sha256"]
        ok = ok and good
        print("PARITY %s donor_blob=%s target_sha256=%s %s" % ("MATCH" if good else "MISMATCH", blob,
              target_sha, row["target_path"]))
    return ok


def copy_module(module: Path, dest: Path) -> None:
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True)
    for entry in module.iterdir():
        if entry.is_file():
            shutil.copy2(entry, dest / entry.name)
    shutil.copytree(module / "src", dest / "src")


def apply_mutant(dest: Path, row: dict) -> None:
    path = dest / row["file"]
    original, mutant = row["original"].encode(), row["mutant"].encode()
    lines = LINES.findall(path.read_bytes())
    hits = [index for index, line in enumerate(lines) if EOL.sub(b"", line) == original]
    if len(hits) != 1:
        raise SystemExit("REFUSED: %s original line occurs %d times in %s" % (row["id"], len(hits), row["file"]))
    line = lines[hits[0]]
    lines[hits[0]] = mutant + line[len(EOL.sub(b"", line)):]
    path.write_bytes(b"".join(lines))


def run_tests(dest: Path, classes: str, mvn: str, repo: str) -> dict:
    command = [mvn, "-B", "-ntp", "-o", "-f", str(dest / "pom.xml"), "test", "-Dtest=" + classes,
               "-Dsurefire.failIfNoSpecifiedTests=false"]
    if repo:
        command.insert(1, "-Dmaven.repo.local=" + repo)
    started = time.time()
    with open(dest / "mvn.log", "wb") as log:
        subprocess.run(command, stdout=log, stderr=subprocess.STDOUT, check=False)
    result = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "failing": [], "messages": [],
              "seconds": "%.1f" % (time.time() - started)}
    reports = sorted((dest / "target" / "surefire-reports").glob("TEST-*.xml"))
    result["reports"] = len(reports)
    for report in reports:
        suite = ElementTree.parse(report).getroot()
        for key in ("tests", "failures", "errors", "skipped"):
            result[key] += int(suite.get(key, "0"))
        for case in suite.iter("testcase"):
            for kind in ("failure", "error"):
                for problem in case.iter(kind):
                    name = case.get("classname", "").rsplit(".", 1)[-1] + "." + case.get("name", "")
                    message = (problem.get("message") or problem.get("type") or "").splitlines()
                    result["failing"].append(case.get("classname", "").rsplit(".", 1)[-1])
                    result["messages"].append(name + ": " + (message[0] if message else ""))
    return result


def verdict(row: dict, result: dict) -> bool:
    broken = result["failures"] + result["errors"]
    if row["expect"] == KILL:
        return result["reports"] > 0 and broken > 0 and any(row["failure_contains"] in m for m in result["messages"])
    return result["reports"] > 0 and result["tests"] > 0 and broken == 0


def record(row_id: str, expect: str, state: str, result: dict) -> tuple:
    first = result["messages"][0][:MESSAGE_LIMIT].replace("\t", " ") if result["messages"] else "-"
    failing = ",".join(sorted(set(result["failing"]))) or "-"
    return (row_id, expect, state, str(result["tests"]), str(result["failures"]), str(result["errors"]),
            str(result["skipped"]), failing, first, result["seconds"])


def run(module: Path, scratch: Path, donor: str, mvn: str, repo: str, only: str) -> int:
    ok = parity(module, Path(donor)) if donor else True
    rows = [row for row in read_tsv(RECIPE_DIR / MUTANTS_NAME) if not only or row["id"] == only]
    owners = sorted({row["file"] for row in rows})
    before = {name: sha256_file(module / name) for name in owners}
    classes = ",".join(sorted({name for row in rows for name in row["test_classes"].split(",")}))
    copy_module(module, scratch / CONTROL)
    control = run_tests(scratch / CONTROL, classes, mvn, repo)
    control_ok = control["reports"] > 0 and control["tests"] > 0 and control["failures"] + control["errors"] == 0
    ok = ok and control_ok
    results = [record(CONTROL, "PASS", "PASS" if control_ok else "FAIL", control)]
    print("CONTROL %s tests=%d failures=%d errors=%d skipped=%d classes=%s seconds=%s" % (
        "PASS" if control_ok else "FAIL", control["tests"], control["failures"], control["errors"],
        control["skipped"], classes, control["seconds"]))
    for row in rows:
        dest = scratch / row["id"]
        copy_module(module, dest)
        apply_mutant(dest, row)
        result = run_tests(dest, row["test_classes"], mvn, repo)
        good = verdict(row, result)
        ok = ok and good
        state = ("KILLED" if result["failures"] + result["errors"] else "SURVIVED") if result["reports"] else "NO_REPORT"
        results.append(record(row["id"], row["expect"], state, result))
        print("MUTANT %s id=%s expect=%s state=%s tests=%d failures=%d errors=%d first=%s" % (
            "PASS" if good else "FAIL", row["id"], row["expect"], state, result["tests"], result["failures"],
            result["errors"], results[-1][8]))
    after = {name: sha256_file(module / name) for name in owners}
    untouched = before == after
    ok = ok and untouched
    table = "\n".join(["\t".join(RESULT_COLUMNS)] + ["\t".join(row) for row in results]) + "\n"
    (scratch / RESULT_NAME).write_text(table, encoding="utf-8", newline="\n")
    print("MUTANT_REFUSAL %s tool=%s %s mutants=%d module_untouched=%s result=%s" % (
        "PASS" if ok else "FAIL", TOOL, VERSION, len(rows), str(untouched).upper(), scratch / RESULT_NAME))
    return 0 if ok else 1


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--scratch", required=True, help="scratch directory for the module copies (created)")
    parser.add_argument("--donor-git", default="", help="donor git directory for the parity check (blobless is enough)")
    parser.add_argument("--module", default=str(RECIPE_DIR.parents[2]), help="module root (default: owner module)")
    parser.add_argument("--maven-repo", default=os.environ.get("MAVEN_REPO_LOCAL", ""), help="maven.repo.local")
    parser.add_argument("--mvn", default="mvn.cmd" if os.name == "nt" else "mvn", help="maven launcher")
    parser.add_argument("--only", default="", help="run one MUTANTS.tsv id (and the control)")
    args = parser.parse_args(argv)
    scratch = Path(args.scratch)
    scratch.mkdir(parents=True, exist_ok=True)
    return run(Path(args.module), scratch, args.donor_git, args.mvn, args.maven_repo, args.only)


if __name__ == "__main__":
    sys.exit(main())
