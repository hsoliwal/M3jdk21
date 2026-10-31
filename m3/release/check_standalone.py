#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Standalone check: M3JDK21 builds, tests, gates and releases without any other repository.

User rule (2026-10-31): "M3jdk21 is standalone and self sufficient; atomization, patternization and using M3
everywhere ensure that; rename according to JDK naming standards." This scan counts, over the tracked files present
in the working tree, two families of findings:

* DEPENDENCY kinds (a build, test, CI or release step needs another repository) must reach zero for a release:
  D_CHECKOUT  a workflow checks out hsoliwal/com.synexia
  D_SECRET    a workflow reads a SYNEXIA_* secret
  D_MAVEN     a pom resolves a com.synexia group id
  D_REPO_REF  any other reference to hsoliwal/com.synexia in an executable or configuration file
* NAMING kinds (Synexia names inside the JDK, to be renamed to JDK naming: jdk.m3.* modules, jdk.internal.m3.* or
  jdk.m3.* packages) are a ratchet:
  N_PACKAGE   a Java package or import under com.synexia
  N_PATH      a tracked path containing "synexia"
  N_NAME      another Synexia identifier (Synexia*, SYNEXIA_*, synexia-*) in an executable or configuration file

Documentation and ledgers (Markdown, text, TSV, NOTICE/LICENSE, m3/docs, release audits) record lineage and
attribution, which the absorb-first rule keeps; they are not counted. The baseline (STANDALONE_BASELINE.tsv) pins
the count per kind: development mode fails when a kind grows; release mode fails unless every DEPENDENCY kind is 0.
--tree REV scans the files of a revision from their blobs instead of the working tree, so a sparse checkout scans
everything (nothing is unscanned) and a commit can be checked before it is checked out.
CLI: python -I check_standalone.py [--root DIR] [--tree REV] [--release] [--tsv OUT] [--write-baseline]
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import threading
from pathlib import Path

DEPENDENCY_KINDS = ("D_CHECKOUT", "D_SECRET", "D_MAVEN", "D_REPO_REF")
NAMING_KINDS = ("N_PACKAGE", "N_PATH", "N_NAME")
KINDS = DEPENDENCY_KINDS + NAMING_KINDS
BASELINE = "m3/release/STANDALONE_BASELINE.tsv"
# the files that define the patterns they look for or rewrite (this check and the JDK naming recipe crate)
SELF = {"m3/release/check_standalone.py", "m3/release/test_check_standalone.py", BASELINE}
SELF_PREFIX = "m3/tooling/jdk-naming/"


def is_self(path: str) -> bool:
    return path in SELF or path.startswith(SELF_PREFIX)
# The scope is fixed by path, not by what a checkout holds, so a sparse local run and a full CI run count the same
# files; a scoped file that is not checked out is reported as unscanned. Executable and configuration files only:
# JSON evidence, provenance and receipts are lineage (absorb-first keeps them), except the release contract.
SCOPED_SUFFIXES = {".yml", ".yaml", ".sh", ".py", ".mjs", ".js", ".properties", ".gradle", ".toml", ".cfg", ".ini"}
SCOPED_FILES = {"m3/release/release-contract.json"}
DOC_SUFFIXES = {".md", ".txt", ".tsv", ".rst", ".adoc", ".html", ".log", ".patch"}
DOC_NAMES = {"NOTICE", "LICENSE", "COPYRIGHT", "AUTHORS", "THIRD_PARTY_NOTICES"}
DOC_DIRS = ("m3/docs/", "m3/release/audit-", "doc/")
GROUP_ID_LINE = re.compile(r"<groupId>\s*com\.synexia[\w.]*\s*</groupId>")
RULES = (
    ("D_CHECKOUT", re.compile(r"repository:\s*['\"]?hsoliwal/com\.synexia\b")),
    ("D_SECRET", re.compile(r"secrets\.SYNEXIA_\w+")),
    ("D_REPO_REF", re.compile(r"hsoliwal/com\.synexia\b")),
    ("N_PACKAGE", re.compile(r"^\s*(?:package|import)\s+(?:static\s+)?com\.synexia\.")),
    ("N_NAME", re.compile(r"\bSynexia\w*|\bSYNEXIA_\w+|\bsynexia[-_]\w+|\bcom\.synexia\b")),
)
POM_NS = re.compile(r"^\{[^}]*\}")


def tracked(root: Path, rev: str | None = None) -> list[str]:
    command = ["ls-files", "-z"] if rev is None else ["ls-tree", "-r", "-z", "--name-only", "--full-tree", rev]
    out = subprocess.run(["git", "-C", str(root), *command], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8", "surrogateescape").split("\0") if p]


def tree_texts(root: Path, rev: str, paths: list[str]):
    """Yield (path, text) for each path of REV from one git cat-file --batch; a missing blob refuses (no lazy
    fetch: a partial clone must hold the blobs it is asked to check)."""
    proc = subprocess.Popen(["git", "-C", str(root), "cat-file", "--batch"], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, env=dict(os.environ, GIT_NO_LAZY_FETCH="1"))
    request = "".join(f"{rev}:{p}\n" for p in paths).encode("utf-8", "surrogateescape")
    feeder = threading.Thread(target=lambda: (proc.stdin.write(request), proc.stdin.close()))
    feeder.start()
    for path in paths:
        header = proc.stdout.readline().split()
        if len(header) != 3:
            proc.kill()
            raise SystemExit("REFUSED: blob not available locally: " + path)
        data = proc.stdout.read(int(header[2]))
        proc.stdout.read(1)
        yield path, data.decode("utf-8", errors="replace")
    feeder.join()
    proc.stdout.close()
    proc.wait()


def is_doc(path: str) -> bool:
    p = Path(path)
    return (p.suffix.lower() in DOC_SUFFIXES or p.name in DOC_NAMES or path.startswith(DOC_DIRS)
            or "/audit-" in path)


def is_pom(path: str) -> bool:
    name = Path(path).name
    return name == "pom.xml" or name.startswith("pom-") and name.endswith(".xml")


def in_scope(path: str) -> bool:
    if is_self(path) or is_doc(path):
        return False
    return (path in SCOPED_FILES or is_pom(path) or Path(path).suffix.lower() in SCOPED_SUFFIXES
            or path.startswith(".github/") and Path(path).suffix.lower() == ".json")


def pom_coordinates(text: str) -> tuple[tuple[str, str] | None, list[tuple[str, str, str]]]:
    """(the project's own groupId/artifactId, [(element, groupId, artifactId)] of its parent, dependencies, plugins)."""
    import xml.etree.ElementTree as ET
    try:
        project = ET.fromstring(text)
    except ET.ParseError:
        return None, []

    def child(element, name):
        for c in element:
            if POM_NS.sub("", c.tag) == name:
                return (c.text or "").strip()
        return ""
    refs = []
    for element in project.iter():
        tag = POM_NS.sub("", element.tag)
        if tag in ("parent", "dependency", "plugin", "extension"):
            refs.append((tag, child(element, "groupId"), child(element, "artifactId")))
    parent_group = next((g for t, g, _ in refs if t == "parent"), "")
    own = (child(project, "groupId") or parent_group, child(project, "artifactId"))
    return own, refs


def maven_findings(texts: dict) -> list[tuple[str, str, int, str]]:
    """D_MAVEN: a com.synexia parent, dependency, plugin or extension no module of this repository produces;
    N_NAME: one it produces, or a module of its own under a com.synexia groupId (a name to change, not a dependency).
    texts: pom path -> text."""
    parsed = {p: pom_coordinates(t) for p, t in texts.items()}
    produced = {own for own, _ in parsed.values() if own and own[1]}
    found = []
    for path, (own, refs) in parsed.items():
        lines = texts[path].splitlines()
        if own and own[0].startswith("com.synexia"):
            found.append(("N_NAME", path, 0, "module groupId " + ":".join(own)))
        for tag, group, artifact in refs:
            if not group.startswith("com.synexia") or tag == "parent" and (group, artifact) in produced:
                continue
            number = next((i for i, l in enumerate(lines, 1) if f"<artifactId>{artifact}</artifactId>" in l), 0)
            kind = "N_NAME" if (group, artifact) in produced else "D_MAVEN"
            found.append((kind, path, number, f"{tag} {group}:{artifact}"))
    return found


def scan_text(path: str, text: str) -> list[tuple[str, str, int, str]]:
    """Line findings of one scoped file: (kind, path, line, text); a line counts once, under its first matching kind.
    In a pom, com.synexia groupId lines are left to maven_findings."""
    found = [("N_PATH", path, 0, path)] if "synexia" in path.lower() else []
    pom = is_pom(path)
    for number, line in enumerate(text.splitlines(), 1):
        if pom and GROUP_ID_LINE.search(line):
            continue
        for kind, rule in RULES:
            if kind == "N_PACKAGE" and not path.endswith(".java"):
                continue
            if rule.search(line):
                found.append((kind, path, number, line.strip()[:200]))
                break
    return found


def worktree_texts(root: Path, paths: list[str]):
    for path in paths:
        try:
            yield path, (root / path).read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue


def scan(root: Path, rev: str | None = None) -> dict:
    """Scan the working tree, or with REV the blobs of that revision (every scoped file, checked out or not)."""
    root = Path(root).resolve()
    paths = tracked(root, rev)
    scoped = [p for p in paths if in_scope(p)]
    present = scoped if rev is not None else [p for p in scoped if (root / p).is_file()]
    texts = tree_texts(root, rev, present) if rev is not None else worktree_texts(root, present)
    findings, poms = [], {}
    for path, text in texts:
        findings += scan_text(path, text)
        if is_pom(path):
            poms[path] = text
    findings += maven_findings(poms)
    findings += [("N_PATH", p, 0, p) for p in paths if "synexia" in p.lower() and not in_scope(p) and not is_doc(p)
                 and not is_self(p)]
    counts = {kind: 0 for kind in KINDS}
    for kind, *_ in findings:
        counts[kind] += 1
    return {"counts": counts, "findings": findings, "scanned": len(present), "unscanned": len(scoped) - len(present)}


def read_baseline(root: Path, rev: str | None = None) -> dict | None:
    """The baseline of the working tree, or with REV the one committed in that revision (a tree scan compares with
    its own ratchet)."""
    if rev is None:
        path = Path(root) / BASELINE
        if not path.is_file():
            return None
        text = path.read_text(encoding="utf-8")
    else:
        shown = subprocess.run(["git", "-C", str(root), "show", f"{rev}:{BASELINE}"], capture_output=True)
        if shown.returncode != 0:
            return None
        text = shown.stdout.decode("utf-8")
    rows = [line.split("\t") for line in text.splitlines()[1:] if line.strip()]
    return {kind: int(count) for kind, count in rows}


def verdict(result: dict, baseline: dict | None, release: bool) -> tuple[str, str]:
    """PASS / BLOCK / HOLD with a detail line."""
    counts = result["counts"]
    dependency = sum(counts[k] for k in DEPENDENCY_KINDS)
    summary = " ".join(f"{k}={counts[k]}" for k in KINDS) + f" scanned={result['scanned']} unscanned={result['unscanned']}"
    if baseline is None:
        return "BLOCK", "no baseline " + BASELINE + "; " + summary
    grown = [f"{k} {baseline.get(k, 0)}->{counts[k]}" for k in KINDS if counts[k] > baseline.get(k, 0)]
    if grown:
        return "BLOCK", "grew past the baseline: " + ", ".join(grown) + "; " + summary
    if release and dependency:
        return "BLOCK", f"{dependency} dependencies on another repository remain; " + summary
    if release and result["unscanned"]:
        return "HOLD", "files not checked out were not scanned; " + summary
    naming = sum(counts[k] for k in NAMING_KINDS)
    if release and naming:
        return "HOLD", f"{naming} Synexia names remain (rename to JDK naming); " + summary
    return "PASS", summary


def write_tsv(result: dict, out: Path) -> None:
    rows = ["kind\tpath\tline\ttext"] + ["\t".join((k, p, str(n), t.replace("\t", " "))) for k, p, n, t in result["findings"]]
    out.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")


def write_baseline(root: Path, result: dict) -> None:
    rows = ["kind\tcount"] + [f"{k}\t{result['counts'][k]}" for k in KINDS]
    path = Path(root) / BASELINE
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".")
    parser.add_argument("--tree", help="scan the blobs of this revision instead of the working tree")
    parser.add_argument("--release", action="store_true")
    parser.add_argument("--tsv")
    parser.add_argument("--write-baseline", action="store_true")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    result = scan(root, args.tree)
    if args.tsv:
        write_tsv(result, Path(args.tsv))
    if args.write_baseline:
        write_baseline(root, result)
    status, detail = verdict(result, read_baseline(root, args.tree), args.release)
    print(f"standalone.no_external_repo_dependency\t{status}\t{detail}")
    return 0 if status == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
