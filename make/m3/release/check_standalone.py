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
attribution, which the absorb-first rule keeps; they are not counted. LINEAGE (2026-11-10) is data that names the donor
inside a scoped file; it is listed in the TSV and summary under its own kind and never counted as a dependency or name:
  L_ARCHIVE     every finding of a file under m3/qualification/<run>/.../retained-variants/ (the archive of
                retained_source_queue.py: copies kept as history; no workflow, build or test runs them)
  L_PROVENANCE  a D_REPO_REF line whose every donor reference is
                (a) in a Python file with no REMOTE_ACCESS anywhere in it: a comment, a docstring, or the donor identity
                    (DONOR_IDENTITY: the repository, its URL, #PR, or a blob/tree/commit/pull link) assigned to, keyed
                    by, compared with or asserted against a provenance field (PROVENANCE_FIELD: *repo, *repository,
                    *workspace, *url, *pr; never an *owner* field, which is ownership policy and stays counted);
                (b) in a pom: inside <description> or an XML comment;
                (c) in a YAML file outside any .github/ directory with no REMOTE_ACCESS anywhere in it: a provenance
                    key holding only a donor identity, or the prose of an OpenRewrite recipe (description:,
                    displayName:);
                (d) elsewhere: a comparison field == / != donor (LINEAGE_COMPARISON) on a line with no REMOTE_ACCESS.
                Any other use of the same string (an argument, a URL to fetch, a command line) stays D_REPO_REF.
  L_SEALED      a D_REPO_REF line of a file listed with its Git blob in STANDALONE_LINEAGE.tsv: sealed or hash-pinned
                lineage (expected receipt rows, re-seal and relocation scripts, offline verifiers of recorded donor
                requests) whose bytes other evidence pins, so the donor names cannot be edited out without breaking a
                seal. The listing holds only while the file is byte-identical to its pinned blob and has no
                NETWORK_CAPABILITY anywhere in it; an edited, unlisted or network-capable file counts as before.
POLICY (2026-11-10) is a guard that only asserts the recipe-ownership policy of AGENTS.md (recipes are authored and
stored in the donor; this repository receives postimages): it names the donor without needing it to build, test, gate
or release. It is listed in the TSV and summary under its own kind and never counted as a dependency:
  P_POLICY      a D_REPO_REF line whose exact text is listed for its file in STANDALONE_POLICY.tsv, in a file with no
                NETWORK_CAPABILITY and no other counted donor reference; a file that fetches, checks out or builds
                from the donor counts every line, and an edited line counts again.
D_CHECKOUT, D_SECRET and D_MAVEN are never lineage outside the archive, and never policy. The baseline (STANDALONE_BASELINE.tsv) pins
the count per kind: development mode fails when a kind grows; release mode fails unless every DEPENDENCY kind is 0.
Only the exact structured canonical Maven receipt owner in release-contract.json is passive metadata; other
references in that contract and all executable checkout/build references remain scanned.
--tree REV scans the files of a revision from their blobs instead of the working tree, so a sparse checkout scans
everything (nothing is unscanned) and a commit can be checked before it is checked out.
CLI: python -I check_standalone.py [--root DIR] [--tree REV] [--release] [--tsv OUT] [--write-baseline]
"""
from __future__ import annotations

import argparse
import ast
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import threading
import tokenize
from pathlib import Path

DEPENDENCY_KINDS = ("D_CHECKOUT", "D_SECRET", "D_MAVEN", "D_REPO_REF")
NAMING_KINDS = ("N_PACKAGE", "N_PATH", "N_NAME")
KINDS = DEPENDENCY_KINDS + NAMING_KINDS
LINEAGE_KINDS = ("L_ARCHIVE", "L_PROVENANCE", "L_SEALED")
# the donor repository, the one identity this check looks for (m3/tooling/standalone/LINEAGE_SOURCES.tsv: lineage only)
DONOR = "hsoliwal/com.synexia"
BASELINE = "m3/release/STANDALONE_BASELINE.tsv"
# Sealed lineage files (L_SEALED): path, pinned Git blob, reason; '#' lines before the header are notes.
LINEAGE_FILES = "m3/release/STANDALONE_LINEAGE.tsv"
LINEAGE_FILES_HEADER = "path\tblob\treason"
OID = re.compile(r"[0-9a-f]{40}\Z")
# Recipe-ownership policy guards (P_POLICY): path, the exact stripped line, reason; '#' lines before the header are notes.
POLICY_KINDS = ("P_POLICY",)
POLICY_ROWS = "m3/release/STANDALONE_POLICY.tsv"
POLICY_ROWS_HEADER = "path\tline\treason"
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
DONOR_REF = re.compile(re.escape(DONOR) + r"\b")
RULES = (
    ("D_CHECKOUT", re.compile(r"repository:\s*['\"]?" + DONOR_REF.pattern)),
    ("D_SECRET", re.compile(r"secrets\.SYNEXIA_\w+")),
    ("D_REPO_REF", DONOR_REF),
    ("N_PACKAGE", re.compile(r"^\s*(?:package|import)\s+(?:static\s+)?com\.synexia\.")),
    ("N_NAME", re.compile(r"\bSynexia\w*|\bSYNEXIA_\w+|\bsynexia[-_]\w+|\bcom\.synexia\b")),
)
POM_NS = re.compile(r"^\{[^}]*\}")
PASSIVE_MAVEN_OWNER = re.compile(
    r'"M3_MAVEN_CONTROL_REACTOR"\s*:\s*"(?P<owner>hsoliwal/com\.synexia)"')
# Lineage rules (module docstring). The archive is the one retained_source_queue.py writes, at any run directory.
LINEAGE_ARCHIVE = re.compile(r"^m3/qualification/(?:[^/]+/)+retained-variants/")
# Any of these anywhere in a Python file means it can reach another repository: none of its references are lineage.
REMOTE_ACCESS = re.compile(
    r"\bgit\s+(?:clone|fetch|pull|push|ls-remote|submodule|remote\s+add)\b"
    r"|[\"'](?:clone|fetch|pull|push|ls-remote|submodule)[\"']"
    r"|[\"']gh[\"']|\bgh\s+(?:api|repo|pr|release|run|workflow|auth)\b"
    r"|\burlopen\b|\burllib\.request\b|\brequests\.\w+\(|\bhttp\.client\b|\bhttpx\b|\bwget\b|\bcurl\b"
    r"|api\.github\.com|raw\.githubusercontent\.com|codeload\.github\.com|\bgit\+https?:"
    r"|github\.com/[\w.-]+/[\w.-]+\.git\b")
# A listed file is sealed lineage only if it cannot reach a network or another repository by itself: no network module,
# no urlopen, and none of the REMOTE_ACCESS commands. A host name inside a literal (a recorded request URL compared with
# an expected value) is not a capability; it is what an offline verifier of recorded requests holds.
NETWORK_CAPABILITY = re.compile(
    r"^\s*(?:import|from)\s+(?:urllib\.request|urllib3|requests|http\.client|httpx|aiohttp|socket|pycurl|ftplib)\b"
    r"|^\s*from\s+urllib\s+import\s+[^\n]*\brequest\b|\burlopen\b|\burllib\.request\b"
    r"|\bgit\s+(?:clone|fetch|pull|push|ls-remote|submodule|remote\s+add)\b"
    r"|[\"'](?:clone|fetch|pull|push|ls-remote|submodule)[\"']"
    r"|[\"']gh[\"']|\bgh\s+(?:api|repo|pr|release|run|workflow|auth)\b|\bwget\b|\bcurl\b|\bgit\+https?:"
    r"|github\.com/[\w.-]+/[\w.-]+\.git\b", re.M)
# Owner fields are policy (who may change a recipe), not provenance: a literal under one stays counted.
OWNER_FIELD = re.compile(r"(?i)owner")
# The donor named as an identity: the repository, its web URL, a pull request, or a browse link to a blob/tree/commit.
DONOR_IDENTITY = re.compile(r"(?:https://github\.com/)?" + re.escape(DONOR)
                            + r"(?:#\d+|/(?:blob|tree|commit|pull)/.*)?\Z", re.S)
# A field or name that records provenance: repo, repository, workspace, url or pr, optionally prefixed (DONOR_REPO,
# source_repository, canonical_repository, html_url). Owner fields are policy, not provenance: they stay counted.
PROVENANCE_FIELD = re.compile(r"(?i)(?:[a-z0-9]+_)*(?:repo|repository|workspace|url|pr)\Z")
ASSERT_CALLS = frozenset({"assertEqual", "assertNotEqual", "assertIn", "assertNotIn"})
LINEAGE_COMPARISON = re.compile(
    r"""(?:\[\s*(["'])(?:\w+_)?(?:repo|repository)\1\s*\]|\.get\(\s*(["'])(?:\w+_)?(?:repo|repository)\2\s*\))"""
    r"""\s*(?:==|!=)\s*(["'])""" + re.escape(DONOR) + r"""\3""")
MARKUP_LINEAGE = re.compile(r"<description>.*?</description>|<!--.*?-->", re.S)
# YAML outside any .github/ directory (c): a provenance key whose whole value is a donor identity, or the prose of an
# OpenRewrite recipe (type specs.openrewrite.org/...) under description: or displayName:, inline or as a block scalar.
YAML_PROVENANCE = re.compile(r"""^\s*(?:-\s+)?(?P<key>[\w-]+):\s+"""
                             r"""(?:"(?P<dq>[^"]*)"|'(?P<sq>[^']*)'|(?P<plain>[^\s"'#]\S*))(?:\s+#.*)?\s*$""")
YAML_RECIPE = re.compile(r"^type:\s*['\"]?specs\.openrewrite\.org/", re.M)
YAML_PROSE = re.compile(r"^(?P<indent>\s*)(?:-\s+)?(?:description|displayName):(?P<rest>.*)$")


def dependency_scan_text(path: str, text: str) -> str:
    """Mask only the canonical recipe receipt's structured owner value.

    A receipt owner is provenance, not a checkout or runtime dependency. The
    surrounding contract remains scanned, including commands on the same line.
    Malformed/duplicate-key JSON or ambiguous occurrences get no exemption.
    This classifies metadata; it does not validate or execute a receipt.
    """
    if path != "m3/release/release-contract.json":
        return text

    def unique_fields(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError("duplicate contract key")
            result[key] = value
        return result

    def reject_constant(value):
        raise ValueError("non-finite JSON constant: " + value)

    try:
        contract = json.loads(text, object_pairs_hook=unique_fields,
                              parse_constant=reject_constant)
        if not isinstance(contract, dict) or contract.get("schema") != "M3_RELEASE_CONTRACT_V1":
            return text
        evidence = contract.get("execution_evidence")
        if not isinstance(evidence, dict) or evidence.get("schema") != "M3_RELEASE_EVIDENCE_V1":
            return text
        owners = evidence.get("gate_owners")
        if not isinstance(owners, dict) or owners.get("M3_MAVEN_CONTROL_REACTOR") != "hsoliwal/com.synexia":
            return text
        pending, owner_count = [contract], 0
        while pending:
            value = pending.pop()
            if isinstance(value, dict):
                owner_count += value.get("M3_MAVEN_CONTROL_REACTOR") == "hsoliwal/com.synexia"
                pending.extend(value.values())
            elif isinstance(value, list):
                pending.extend(value)
        if owner_count != 1:
            return text
        matches = list(PASSIVE_MAVEN_OWNER.finditer(text))
        if len(matches) != 1:
            return text
        start, end = matches[0].span("owner")
        return text[:start] + " " * (end - start) + text[end:]
    except (ValueError, RecursionError):
        return text


def is_archive(path: str) -> bool:
    return bool(LINEAGE_ARCHIVE.match(path))


def provenance_field(node: ast.AST) -> bool:
    """A name, attribute, subscript key or .get(key) whose name is a PROVENANCE_FIELD."""
    if isinstance(node, ast.Name):
        name = node.id
    elif isinstance(node, ast.Attribute):
        name = node.attr
    elif isinstance(node, ast.Subscript) and isinstance(node.slice, ast.Constant):
        name = node.slice.value
    elif (isinstance(node, ast.Call) and isinstance(node.func, ast.Attribute) and node.func.attr == "get"
          and node.args and isinstance(node.args[0], ast.Constant)):
        name = node.args[0].value
    else:
        return False
    return isinstance(name, str) and bool(PROVENANCE_FIELD.match(name)) and not OWNER_FIELD.search(name)


def donor_identity(node: ast.AST) -> bool:
    """The literal names the donor only as an identity (an f-string by its leading text)."""
    if isinstance(node, ast.JoinedStr):
        rest = [v for v in node.values[1:] if isinstance(v, ast.Constant) and DONOR_REF.search(str(v.value))]
        node = node.values[0] if node.values and not rest else None
    return isinstance(node, ast.Constant) and isinstance(node.value, str) and bool(DONOR_IDENTITY.match(node.value))


def lineage_string(node: ast.AST, parents: dict) -> bool:
    """A docstring, or a donor identity bound to, keyed by or compared with a provenance field. Concatenation (+) and,
    inside a comparison, a tuple/list/set of candidates are looked through; any other use is not lineage."""
    child, parent = node, parents.get(node)
    while (isinstance(parent, ast.BinOp) and isinstance(parent.op, ast.Add)
           or isinstance(parent, (ast.Tuple, ast.List, ast.Set)) and isinstance(parents.get(parent), ast.Compare)):
        child, parent = parent, parents.get(parent)
    if isinstance(parent, ast.Expr):
        return child is node and isinstance(node, ast.Constant)
    if not donor_identity(node):
        return False
    if isinstance(parent, ast.Assign):
        return parent.value is child and all(provenance_field(t) for t in parent.targets)
    if isinstance(parent, ast.AnnAssign):
        return parent.value is child and provenance_field(parent.target)
    if isinstance(parent, ast.Dict):
        keys = [k for k, v in zip(parent.keys, parent.values) if v is child]
        return len(keys) == 1 and isinstance(keys[0], ast.Constant) and provenance_field(ast.Name(id=str(keys[0].value)))
    if isinstance(parent, ast.keyword):
        return parent.arg is not None and provenance_field(ast.Name(id=parent.arg))
    if isinstance(parent, ast.Compare):
        operands = [parent.left, *parent.comparators]
        return (all(isinstance(op, (ast.Eq, ast.NotEq, ast.In, ast.NotIn)) for op in parent.ops)
                and any(provenance_field(o) for o in operands if o is not child))
    if isinstance(parent, ast.Call) and isinstance(parent.func, ast.Attribute) and parent.func.attr in ASSERT_CALLS:
        return child in parent.args and any(provenance_field(a) for a in parent.args if a is not child)
    return False


def python_lineage_lines(text: str) -> set[int]:
    """Lines of a Python file whose every donor reference is a comment or a lineage_string. A file that can reach
    another repository (REMOTE_ACCESS anywhere in it) or that does not parse has none."""
    lines = text.split("\n")
    if REMOTE_ACCESS.search(text) or len(text.splitlines()) != len(io.StringIO(text).readlines()):
        return set()
    try:
        tree = ast.parse(text)
        tokens = list(tokenize.generate_tokens(io.StringIO(text).readline))
    except (SyntaxError, ValueError, tokenize.TokenError, RecursionError):
        return set()
    covered: dict[int, int] = {}
    refused: set[int] = set()
    for token in tokens:
        if token.type == tokenize.COMMENT:
            covered[token.start[0]] = covered.get(token.start[0], 0) + len(DONOR_REF.findall(token.string))
    parents = {child: node for node in ast.walk(tree) for child in ast.iter_child_nodes(node)}
    for node in ast.walk(tree):
        literal = isinstance(node, ast.JoinedStr) or isinstance(node, ast.Constant) and isinstance(node.value, str)
        if not literal or isinstance(parents.get(node), ast.JoinedStr):
            continue
        lineage = lineage_string(node, parents)
        for number in range(node.lineno, node.end_lineno + 1):
            raw = lines[number - 1].encode("utf-8")
            start = node.col_offset if number == node.lineno else 0
            end = node.end_col_offset if number == node.end_lineno else len(raw)
            hits = len(DONOR_REF.findall(raw[start:end].decode("utf-8", errors="replace")))
            if hits and lineage:
                covered[number] = covered.get(number, 0) + hits
            elif hits:
                refused.add(number)
    return {n for n, hits in covered.items()
            if n not in refused and hits == len(DONOR_REF.findall(lines[n - 1]))}


def markup_lineage_lines(text: str) -> set[int]:
    """Lines of a pom whose every donor reference is inside <description> or an XML comment."""
    spans = [m.span() for m in MARKUP_LINEAGE.finditer(text)]
    inside: dict[int, bool] = {}
    for match in DONOR_REF.finditer(text):
        number = text.count("\n", 0, match.start()) + 1
        inside[number] = inside.get(number, True) and any(a <= match.start() and match.end() <= b for a, b in spans)
    return {n for n, ok in inside.items() if ok}


def yaml_lineage_lines(text: str) -> set[int]:
    """Lines of a YAML file (outside any .github/ directory) with no REMOTE_ACCESS whose donor reference is a provenance key holding
    only a donor identity, or (in an OpenRewrite recipe) prose under description: or displayName:."""
    if REMOTE_ACCESS.search(text):
        return set()
    recipe, found, block = bool(YAML_RECIPE.search(text)), set(), None
    for number, line in enumerate(text.splitlines(), 1):
        indent = len(line) - len(line.lstrip())
        if block is not None and line.strip() and indent <= block:
            block = None
        prose = YAML_PROSE.match(line) if recipe else None
        if prose and prose.group("rest").strip()[:1] in (">", "|"):
            block = len(prose.group("indent"))
        if not DONOR_REF.search(line):
            continue
        field = YAML_PROVENANCE.match(line)
        if (block is not None and not prose or prose and block is None
                or field and provenance_field(ast.Name(id=field.group("key").replace("-", "_")))
                and DONOR_IDENTITY.match(next(v for v in field.group("dq", "sq", "plain") if v is not None))):
            found.add(number)
    return found


def lineage_lines(path: str, text: str) -> set[int]:
    """Line numbers (1-based, as scan_text counts) whose D_REPO_REF finding is L_PROVENANCE."""
    if path.endswith(".py"):
        return python_lineage_lines(text)
    if is_pom(path):
        return markup_lineage_lines(text)
    if Path(path).suffix.lower() in (".yml", ".yaml") and ".github/" not in "/" + path:
        found = yaml_lineage_lines(text)
    else:
        found = set()
    for number, line in enumerate(text.splitlines(), 1):
        if (DONOR_REF.search(line) and not REMOTE_ACCESS.search(line)
                and len(DONOR_REF.findall(line)) == len(LINEAGE_COMPARISON.findall(line))):
            found.add(number)
    return found


def tracked(root: Path, rev: str | None = None) -> list[str]:
    command = ["ls-files", "-z"] if rev is None else ["ls-tree", "-r", "-z", "--name-only", "--full-tree", rev]
    out = subprocess.run(["git", "-C", str(root), *command], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8", "surrogateescape").split("\0") if p]


def tree_texts(root: Path, rev: str, paths: list[str]):
    """Yield (path, text, blob) for each path of REV from one git cat-file --batch; a missing blob refuses (no lazy
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
        yield path, data.decode("utf-8", errors="replace"), header[0].decode("ascii")
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
    original_lines = text.splitlines()
    for number, line in enumerate(dependency_scan_text(path, text).splitlines(), 1):
        if pom and GROUP_ID_LINE.search(line):
            continue
        for kind, rule in RULES:
            if kind == "N_PACKAGE" and not path.endswith(".java"):
                continue
            if rule.search(line):
                found.append((kind, path, number, original_lines[number - 1].strip()[:200]))
                break
    if any(kind == "D_REPO_REF" for kind, *_ in found):
        lineage = lineage_lines(path, text)
        found = [("L_PROVENANCE", p, n, kind + " " + t) if kind == "D_REPO_REF" and n in lineage else (kind, p, n, t)
                 for kind, p, n, t in found]
    return found


def as_archive(findings: list[tuple[str, str, int, str]]) -> list[tuple[str, str, int, str]]:
    """L_ARCHIVE: every finding of an archived copy, its own kind kept in the text."""
    return [("L_ARCHIVE", p, n, t if k == "L_PROVENANCE" else k + " " + t) for k, p, n, t in findings]


def as_sealed(findings: list[tuple[str, str, int, str]]) -> list[tuple[str, str, int, str]]:
    """L_SEALED: the D_REPO_REF findings of a sealed lineage file; every other kind is kept as it is."""
    return [("L_SEALED", p, n, k + " " + t) if k == "D_REPO_REF" else (k, p, n, t) for k, p, n, t in findings]


def git_blob_id(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def read_exemption_rows(root: Path, rev: str | None, name: str, header: str) -> list[list[str]]:
    """The three-column rows of an exemption list in the working tree, or with REV the one committed in that revision;
    none when it is absent. An exemption list is never read loosely: a malformed one refuses (ValueError)."""
    if rev is None:
        path = Path(root) / name
        if not path.is_file():
            return []
        data = path.read_bytes()
    else:
        shown = subprocess.run(["git", "-C", str(root), "show", f"{rev}:{name}"], capture_output=True)
        if shown.returncode != 0:
            return []
        data = shown.stdout
    lines = data.decode("utf-8").split("\n")
    while lines and lines[0].startswith("#"):
        lines.pop(0)
    if not lines or lines[0] != header or lines[-1] != "":
        raise ValueError(f"{name} malformed: header or final newline")
    rows = []
    for number, line in enumerate(lines[1:-1], 2):
        fields = line.split("\t")
        if (len(fields) != 3 or not in_scope(fields[0]) or is_archive(fields[0]) or not fields[2].strip()
                or fields[:2] in [r[:2] for r in rows]):
            raise ValueError(f"{name} malformed: row {number} after the notes")
        rows.append(fields)
    return rows


def read_lineage_files(root: Path, rev: str | None = None) -> dict[str, str]:
    """{path: pinned blob} of STANDALONE_LINEAGE.tsv (one row per path)."""
    pinned: dict[str, str] = {}
    for number, (path, blob, _reason) in enumerate(read_exemption_rows(root, rev, LINEAGE_FILES, LINEAGE_FILES_HEADER), 2):
        if path in pinned or not OID.match(blob):
            raise ValueError(f"{LINEAGE_FILES} malformed: row {number} after the notes")
        pinned[path] = blob
    return pinned


def read_policy_rows(root: Path, rev: str | None = None) -> dict[str, set[str]]:
    """{path: {exact stripped line}} of STANDALONE_POLICY.tsv; every listed line names the donor."""
    listed: dict[str, set[str]] = {}
    for number, (path, line, _reason) in enumerate(read_exemption_rows(root, rev, POLICY_ROWS, POLICY_ROWS_HEADER), 2):
        if line != line.strip() or not DONOR_REF.search(line):
            raise ValueError(f"{POLICY_ROWS} malformed: row {number} after the notes")
        listed.setdefault(path, set()).add(line)
    return listed


def as_policy(text: str, listed: set[str], findings: list[tuple[str, str, int, str]]):
    """(findings with the listed D_REPO_REF lines as P_POLICY, listed lines not found). Nothing is policy in a file
    with NETWORK_CAPABILITY or with a counted donor reference that is not listed (an argument, a checkout path)."""
    lines = text.splitlines()
    exact = {n: lines[n - 1].strip() for k, _p, n, _t in findings if k == "D_REPO_REF"}
    if NETWORK_CAPABILITY.search(text) or any(line not in listed for line in exact.values()):
        return findings, sorted(listed)
    found = [("P_POLICY", p, n, k + " " + t) if k == "D_REPO_REF" else (k, p, n, t) for k, p, n, t in findings]
    return found, sorted(listed - set(exact.values()))


def is_sealed(root: Path, path: str, text: str, blob: str | None, pinned: str) -> bool:
    """The file is byte-identical to its pinned blob (a working-tree file also as checked out with CRLF) and has no
    NETWORK_CAPABILITY."""
    if NETWORK_CAPABILITY.search(text):
        return False
    if blob is None:
        data = (root / path).read_bytes()
        return pinned in (git_blob_id(data), git_blob_id(data.replace(b"\r\n", b"\n")))
    return pinned == blob


def worktree_texts(root: Path, paths: list[str]):
    for path in paths:
        try:
            yield path, (root / path).read_text(encoding="utf-8", errors="replace"), None
        except OSError:
            continue


def scan(root: Path, rev: str | None = None) -> dict:
    """Scan the working tree, or with REV the blobs of that revision (every scoped file, checked out or not)."""
    root = Path(root).resolve()
    paths = tracked(root, rev)
    scoped = [p for p in paths if in_scope(p)]
    present = scoped if rev is not None else [p for p in scoped if (root / p).is_file()]
    texts = tree_texts(root, rev, present) if rev is not None else worktree_texts(root, present)
    sealed_files = read_lineage_files(root, rev)
    policy_rows = read_policy_rows(root, rev)
    findings, poms, archived_poms, sealed = [], {}, {}, set()
    unmatched = {path: sorted(lines) for path, lines in policy_rows.items()}
    for path, text, blob in texts:
        found = scan_text(path, text)
        if path in sealed_files and is_sealed(root, path, text, blob, sealed_files[path]):
            found = as_sealed(found)
            sealed.add(path)
        if path in policy_rows:
            found, unmatched[path] = as_policy(text, policy_rows[path], found)
        findings += as_archive(found) if is_archive(path) else found
        if is_pom(path):
            (archived_poms if is_archive(path) else poms)[path] = text
    findings += maven_findings(poms)  # an archived pom is no reactor module: it produces nothing a live pom resolves
    findings += as_archive(maven_findings(archived_poms))
    unscoped = [("N_PATH", p, 0, p) for p in paths if "synexia" in p.lower() and not in_scope(p) and not is_doc(p)
                and not is_self(p)]
    findings += [f for f in unscoped if not is_archive(f[1])] + as_archive([f for f in unscoped if is_archive(f[1])])
    counts = {kind: 0 for kind in KINDS + LINEAGE_KINDS + POLICY_KINDS}
    for kind, *_ in findings:
        counts[kind] += 1
    lineage = {kind: counts.pop(kind) for kind in LINEAGE_KINDS}
    policy = {kind: counts.pop(kind) for kind in POLICY_KINDS}
    # listed files that are not sealed here (edited, missing, not checked out or network-capable): their lines count
    stale = sorted(set(sealed_files) - sealed)
    not_policy = [f"{path}: {line}" for path, lines in sorted(unmatched.items()) for line in lines]
    return {"counts": counts, "lineage": lineage, "policy": policy, "findings": findings, "scanned": len(present),
            "unscanned": len(scoped) - len(present), "lineage_not_sealed": stale, "policy_not_matched": not_policy}


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
    summary += "".join(f" {k}={v}" for k, v in result.get("lineage", {}).items())
    summary += "".join(f" {k}={v}" for k, v in result.get("policy", {}).items())
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
    for path in result["lineage_not_sealed"]:
        print(f"standalone.lineage_not_sealed\t{path}\tcounted: not byte-identical to its pinned blob, absent or "
              "network-capable", file=sys.stderr)
    for row in result["policy_not_matched"]:
        print(f"standalone.policy_not_matched\t{row}\tcounted or absent: line edited, file absent or "
              "network-capable, or another donor reference in the file is counted", file=sys.stderr)
    return 0 if status == "PASS" else 1


if __name__ == "__main__":
    sys.exit(main())
