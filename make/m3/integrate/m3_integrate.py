#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
# FLLDCIM: MRL - M3_TO_JDK_LAYOUT v1: one owner for the m3/ -> OpenJDK layout integration (map, move, refs, residue)
"""M3_TO_JDK_LAYOUT v1: dissolve the top-level m3/ tree into the OpenJDK source layout.

  map      RULES.tsv + AREAS.tsv over `git ls-tree -r <rev>` -> MAPPING.tsv (one row per m3/ path), AMBIGUOUS.tsv,
           SUMMARY.tsv. A path no rule matches, two rows with one target, or a target that is already a tracked path
           outside the rows' own sources is a finding: the tool refuses (exit 2).
  move     the rows of one phase whose status is in --status -> `git update-index --index-info` lines over an index
           read from <rev>: pure moves keep blob id and mode, RETIRE rows are removed. Writes no working tree.
  refs     on <new rev> (the moved tree): every live reference to a moved path follows it.
             root form  m3/<p>, ./m3/<p>, $VAR/m3/<p>, ${VAR}/m3/<p>, }}/m3/<p>, github.com/.../(blob|tree)/master/m3/<p>,
                        and path components ("m3" / "x" / "y", "m3", "x") in Python/Java;
             relative   ../<p> and ./<p> tokens, pom <module>/<relativePath>/<directory>/<source>... and
                        ${project.basedir}/<p> resolve against the file's OLD directory; when they name a tracked path or
                        directory of the old tree they are re-relativised from the file's NEW directory;
             python     Path(__file__)...parents[N] / .parent chains whose ancestor moved by the shift are re-indexed;
             pom        a <parent> with Maven's implicit ../pom.xml gets an explicit <relativePath> when that moved.
           Nested mirrors (before/m3/..., overlay/m3/..., <sha>/m3/..., jdk/m3/... package paths) are not repository
           references and are left as they are. Writes the changed blobs as one commit on --ref (git fast-import).
  residue  root-form references to an old moved path that remain in live files -> RESIDUE.tsv (exit 2 when any).
  repin    digests reseal.py leaves AMBIGUOUS (an unchanged copy still has the old bytes) are carried where the record
           names the file within 4 lines (path-anchored); others are reported (REPIN.tsv).
Digest pins of changed bytes are carried by the existing owner make/m3/tooling/jdk-naming/reseal.py, driven by
reseal_moves.py with this tool's MAPPING instead of the naming plan.
usage: python3 -I m3_integrate.py map --repo R --rev REV --out DIR
       python3 -I m3_integrate.py move --mapping MAPPING.tsv --phase P2 [--status MOVE,AMBIGUOUS] --rev REV
       python3 -I m3_integrate.py refs --repo R --base REV --rev NEWREV --mapping MAPPING.tsv --phase P2 --ref REF
       python3 -I m3_integrate.py residue --repo R --rev NEWREV --mapping MAPPING.tsv --phase P2 --out FILE
"""
from __future__ import annotations

import argparse
import posixpath
import re
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path

HERE = Path(__file__).resolve().parent
CODE_PARTS = re.compile(r"^(pom\.xml|src/.*|test/.*|tests/.*|native/.*|include/.*|legal/.*|CMakeLists\.txt|LICENSE[^/]*"
                        r"|NOTICE[^/]*|\.gitignore)$")
MOVING = ("MOVE", "AMBIGUOUS")


# ---------------------------------------------------------------- git plumbing
def git(repo: str, *args: str, data: bytes | None = None) -> bytes:
    done = subprocess.run(["git", "-C", repo, *args], input=data, capture_output=True)
    if done.returncode != 0:
        raise SystemExit("REFUSED git " + " ".join(args[:3]) + ": " + done.stderr.decode(errors="replace").strip())
    return done.stdout


def tree_entries(repo: str, rev: str) -> list[tuple[str, str, str]]:
    """(path, mode, blob id) of every tracked file of rev."""
    out = []
    for rec in git(repo, "ls-tree", "-r", "-z", "--full-tree", rev).split(b"\0"):
        if rec:
            meta, path = rec.split(b"\t", 1)
            mode, _kind, sha = meta.decode().split(" ")
            out.append((path.decode("utf-8", "surrogateescape"), mode, sha))
    return out


def blobs(repo: str, shas: list[str]):
    """Yield the bytes of each blob id, in order (one cat-file --batch process)."""
    proc = subprocess.Popen(["git", "-C", repo, "cat-file", "--batch"], stdin=subprocess.PIPE, stdout=subprocess.PIPE)
    import threading

    def feed():
        for s in shas:
            proc.stdin.write(s.encode() + b"\n")
        proc.stdin.close()
    threading.Thread(target=feed, daemon=True).start()
    for s in shas:
        head = proc.stdout.readline().split()
        if len(head) < 3 or head[1] == b"missing":
            raise SystemExit(f"REFUSED blob {s} is not local (fetch it first)")
        data = proc.stdout.read(int(head[2]))
        proc.stdout.read(1)
        yield data
    proc.wait()


# ---------------------------------------------------------------- map
def read_tsv(path: Path) -> list[list[str]]:
    rows = []
    for raw in path.read_text(encoding="utf-8").splitlines():
        if raw.strip() and not raw.startswith("#"):
            rows.append(raw.split("\t"))
    return rows


def lib_name(raw: str) -> str:
    """relocated library directory -> module-name segment: commons-io-2.18.0 -> commonsio, HdrHistogram -> hdrhistogram."""
    base = re.sub(r"-[0-9][0-9A-Za-z.]*$", "", raw)
    return re.sub(r"[^a-z0-9]", "", base.lower())


def area_rules(areas: list[list[str]]) -> list[tuple]:
    """P1 rules from AREAS.tsv: the code parts of each area, longest area first."""
    rules = []
    for area, module, status, reason in sorted(areas, key=lambda r: -len(r[0])):
        slug = area.replace("/", "-")
        rules.append((f"P1-{slug}", area, module, status, reason))
    return rules


def p1_target(area: str, module: str, rest: str) -> str:
    """Proposed home of a code part (informational for HOLD rows; the area's own move tool decides at move time)."""
    slug = area.replace("/", "-")
    tests = "jdk/internal/m3" if module == "java.base" else "jdk/m3"
    m = re.match(r"^src/main/(java|resources)/(.+)$", rest)
    if m and not rest.endswith("module-info.java"):
        return f"src/{module}/share/classes/{m.group(2)}"
    m = re.match(r"^src/test/(java|resources)/(.+)$", rest)
    if m:
        return f"test/jdk/{m.group(2)}"
    m = re.match(r"^(?:src/main/(?:native|c)/|native/|include/)(.+)$", rest)
    if m:
        return f"src/{module}/share/native/libm3{slug.replace('-', '')}/{m.group(1)}"
    m = re.match(r"^(?:test|tests)/(.+)$", rest)
    if m:
        return f"test/jdk/{tests}/{slug}/{m.group(1)}"
    m = re.match(r"^legal/(.+)$", rest)
    if m:
        return f"src/{module}/share/legal/{m.group(1)}"
    return f"make/m3/{area}/{rest}"  # Maven bridge: pom.xml, bridge module-info, LICENSE/NOTICE, CMakeLists


def classify(path: str, rules: list[list[str]], areas: list[tuple]) -> tuple[str, str, str, str, str]:
    """(target, rule id, phase, status, reason) of one m3/ path."""
    p4_p3 = [r for r in rules if r[4] in ("P4", "P3")]
    rest_rules = [r for r in rules if r[4] not in ("P4", "P3")]
    for rule in p4_p3:
        hit = apply_rule(path, rule)
        if hit:
            return hit
    for rid, area, module, status, reason in areas:
        prefix = f"m3/{area}/"
        if path.startswith(prefix):
            rest = path[len(prefix):]
            return p1_target(area, module, rest), rid, "P1", status, f"{module}: {reason}"
    for rule in rest_rules:
        hit = apply_rule(path, rule)
        if hit:
            return hit
    raise SystemExit(f"REFUSED no rule matches {path}")


def apply_rule(path: str, rule: list[str]):
    rid, kind, pattern, target, phase, status, reason = rule
    if kind == "prefix":
        if not path.startswith(pattern):
            return None
        rest = path[len(pattern):]
        new = target.replace("{rest}", rest)
        return new, rid, phase, status, reason
    m = re.match(pattern, path)
    if not m:
        return None
    new = target
    if "{lib}" in new or "{lib}" in reason:
        new = new.replace("{lib}", lib_name(m.group(1)))
        reason = reason.replace("{lib}", lib_name(m.group(1)))
    for i, g in enumerate(m.groups(), 1):
        new = new.replace("{%d}" % i, g or "")
    return new, rid, phase, status, reason


def cmd_map(args) -> int:
    rules = read_tsv(Path(args.rules))
    areas = area_rules(read_tsv(Path(args.areas)))
    entries = tree_entries(args.repo, args.rev)
    tracked = {p for p, _m, _s in entries}
    rows = []
    for path, mode, sha in entries:
        if path.startswith("m3/"):
            target, rid, phase, status, reason = classify(path, rules, areas)
            rows.append((path, target, rid, phase, status, mode, sha, reason))
    findings = []
    by_target = defaultdict(list)
    for row in rows:
        if row[1] != "RETIRE":
            by_target[row[1]].append(row[0])
    for target, sources in by_target.items():
        if len(sources) > 1:
            findings.append(f"COLLISION\t{target}\t" + " ".join(sources))
        if target in tracked and not target.startswith("m3/"):
            findings.append(f"EXISTS\t{target}\t{sources[0]}")
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    head = "old_path\tnew_path\trule\tphase\tstatus\tmode\tblob\treason\n"
    body = "".join("\t".join(r) + "\n" for r in rows)
    (out / "MAPPING.tsv").write_text(head + body, encoding="utf-8", newline="\n")
    amb = [r for r in rows if r[4] == "AMBIGUOUS"]
    (out / "AMBIGUOUS.tsv").write_text(head + "".join("\t".join(r) + "\n" for r in amb), encoding="utf-8", newline="\n")
    summary = Counter()
    for r in rows:
        dest = "RETIRE" if r[1] == "RETIRE" else "/".join(r[1].split("/")[:2])
        summary[(r[3], r[4], dest)] += 1
    lines = ["phase\tstatus\tdestination\trows"] + [f"{k[0]}\t{k[1]}\t{k[2]}\t{v}" for k, v in sorted(summary.items())]
    lines.append(f"TOTAL\t\t\t{len(rows)}")
    (out / "SUMMARY.tsv").write_text("\n".join(lines) + "\n", encoding="utf-8", newline="\n")
    for f in findings:
        print(f)
    print(f"M3_TO_JDK_LAYOUT map rev={args.rev} rows={len(rows)} ambiguous={len(amb)} findings={len(findings)}")
    return 2 if findings else 0


# ---------------------------------------------------------------- mapping helpers
def load_mapping(path: str, phases: set[str], statuses: set[str]) -> list[list[str]]:
    rows = []
    for raw in Path(path).read_text(encoding="utf-8").splitlines()[1:]:
        r = raw.split("\t")
        if r[3] in phases and r[4] in statuses:
            rows.append(r)
    return rows


def cmd_move(args) -> int:
    phases, statuses = set(args.phase.split(",")), set(args.status.split(","))
    out = sys.stdout.buffer
    n_move = n_ret = 0
    for r in load_mapping(args.mapping, phases, statuses):
        old, new, mode, sha = r[0], r[1], r[5], r[6]
        out.write(f"0 {'0' * 40}\t{old}\n".encode("utf-8", "surrogateescape"))
        if new == "RETIRE":
            n_ret += 1
        else:
            out.write(f"{mode} {sha}\t{new}\n".encode("utf-8", "surrogateescape"))
            n_move += 1
    print(f"M3_TO_JDK_LAYOUT move moves={n_move} retired={n_ret}", file=sys.stderr)
    return 0




# ---------------------------------------------------------------- refs: the path model of one phase
SOURCE_ROOT_DIR = re.compile(r"/src/(?:main|test)/(?:java|resources)/")


class PathMap:
    """Old tree -> new tree for one phase: exact file moves, and directories whose every file moved by one shift.
    Rows of other phases' RETIRE status do not split a directory (they leave the tree in their own PR). A directory
    that the phase splits follows its pom.xml when it has one (a Maven module directory and its target/ go where the
    pom goes); otherwise a reference to it is a finding."""

    def __init__(self, old_entries, rows, retired=()):
        self.files = {p for p, _m, _s in old_entries}
        self.fmap = {r[0]: r[1] for r in rows if r[1] != "RETIRE"}
        self.retired = set(retired)
        self.dirs: dict = {"": ""}
        self.staying: set = set()
        conflict = object()
        acc: dict = {}
        for path in self.files:
            if path in self.retired:
                continue
            new = self.fmap.get(path, path)
            parts = path.split("/")
            for k in range(1, len(parts)):
                d = "/".join(parts[:k])
                if new == path:
                    self.staying.add(d)
                rest = "/".join(parts[k:])
                cand = new[:-len(rest) - 1] if new.endswith("/" + rest) else conflict
                prev = acc.get(d, cand)
                acc[d] = cand if prev == cand else conflict
        for path in self.retired:
            parts = path.split("/")
            for k in range(1, len(parts)):
                acc.setdefault("/".join(parts[:k]), "/".join(parts[:k]))
        for d, v in acc.items():
            self.dirs[d] = self.pom_follow(d) if v is conflict else v

    def pom_follow(self, d: str):
        if "/" not in d:
            return None  # a top-level directory (m3) is a reactor of many modules, not one module
        pom = d + "/pom.xml"
        if pom in self.files and pom not in self.retired:
            return posixpath.dirname(self.fmap.get(pom, pom))
        return None

    def new_of(self, old: str):
        """New path of an old file or directory path (no trailing slash); for an untracked path the longest known
        ancestor decides; None when that ancestor is split by the phase and has no pom.xml to follow."""
        old = old.rstrip("/")
        if old in self.fmap:
            return self.fmap[old]
        if old in self.files:
            return old
        if old in self.dirs:
            return self.dirs[old]
        parts = old.split("/")
        for k in range(len(parts) - 1, 0, -1):
            d = "/".join(parts[:k])
            if d in self.dirs:
                if SOURCE_ROOT_DIR.search(d + "/"):
                    return old  # a class that does not exist under a source root: synthetic or stale, not a move
                n = self.dirs[d]
                return None if n is None else n + "/" + "/".join(parts[k:])
        return old

    def known(self, old: str) -> bool:
        old = old.rstrip("/")
        return old in self.files or old in self.dirs

    def stays(self, old: str) -> bool:
        """The old file or directory still has tracked content at its old path after the phase."""
        old = old.rstrip("/")
        return (old in self.files and old not in self.fmap and old not in self.retired) or old in self.staying


PATHCH = r"[A-Za-z0-9_.\-/*{}$@+~%]"
ROOT_FORM = re.compile(
    r"(?:(?<![A-Za-z0-9_.\-/\\$@~+%])(?P<dot>\./)?"
    r"|(?P<var>(?:\$[A-Za-z_][A-Za-z0-9_]*|\$\{[^}\s/]+\}|\}\}|\$\([^()\s]*\)|\{[A-Za-z_][A-Za-z0-9_.]*\}|%s)/)"
    r"|(?P<url>github\.com/hsoliwal/M3jdk21/(?:blob|tree|raw)/master/))"
    r"(?P<path>m3/" + PATHCH + r"*)")
COMPONENTS = re.compile(r"""(?P<q>["'])m3(?P=q)(?P<more>(?:\s*(?P<sep>[/,])\s*(?P<q2>["'])[^"'\n/\\]+(?P=q2))+)""")
COMP_ONE = re.compile(r"""\s*([/,])\s*(["'])([^"'\n/\\]+)\2""")
CALL_CTX = re.compile(r"(?:join|Path|Path\.of|Paths\.get|resolve|joinpath)\([^()]*$")
# a relative token starts a path (boundary) or follows a variable that names the file's own directory
SELF_DIR = ("${project.basedir}/", "${basedir}/", 'dirname "$0")/', 'dirname -- "$0")/', 'dirname "${BASH_SOURCE[0]}")/',
            'dirname -- "${BASH_SOURCE[0]}")/', 'dirname "$0")"/', "${CMAKE_CURRENT_SOURCE_DIR}/",
            "${CMAKE_CURRENT_LIST_DIR}/")
REL_FORM = re.compile(r"(?:(?<![A-Za-z0-9_.\-/\\$@~+%])" + "".join("|(?<=" + re.escape(v) + ")" for v in SELF_DIR)
                      + r")(?P<rel>\.\.(?:/\.\.)*(?:/" + PATHCH + r"*)?(?![.\w])|\./(?!m3/)" + PATHCH + r"+)")
POM_ELEM = re.compile(r"<(?P<tag>module|relativePath|directory|source|sourceDirectory|testSourceDirectory|file|exists|"
                      r"missing|basedir|workingDirectory|location|descriptor|configLocation)>(?P<val>[^<${}]+?)</(?P=tag)>")
POM_BASEDIR = re.compile(r"\$\{(?:project\.basedir|basedir)\}/(?P<val>" + PATHCH + r"+)")
PY_PARENTS = re.compile(r"(?P<head>Path\(__file__\)(?:\.resolve\(\)|\.absolute\(\))?)\.parents\[(?P<n>\d+)\]")
PY_CHAIN = re.compile(r"(?P<head>Path\(__file__\)(?:\.resolve\(\)|\.absolute\(\))?)(?P<chain>(?:\.parent)+)(?![A-Za-z_])")
PY_VAR_CHAIN = re.compile(r"^[ \t]*(?P<var>[A-Za-z_]\w*)\s*=\s*Path\(__file__\)(?:\.resolve\(\)|\.absolute\(\))?"
                          r"(?P<chain>(?:\.parent)+)[ \t]*(?:#.*)?$", re.M)
PY_VAR_INDEX = re.compile(r"^[ \t]*(?P<var>[A-Za-z_]\w*)\s*=\s*Path\(__file__\)(?:\.resolve\(\)|\.absolute\(\))?"
                          r"\.parents\[(?P<n>\d+)\][ \t]*(?:#.*)?$", re.M)
VAR_PARENTS = re.compile(r"(?<![\w.])(?P<var>[A-Za-z_]\w*)\.parents\[(?P<n>\d+)\]")
VAR_CHAIN = re.compile(r"(?<![\w.])(?P<var>[A-Za-z_]\w*)(?P<chain>(?:\.parent)+)(?![A-Za-z_])")
PARENT_BLOCK = re.compile(r"<parent>(?P<body>.*?)</parent>", re.S)
FIXTURE_SEG = {"before", "after", "templates", "fixtures", "fixture", "golden", "expected", "baseline", "candidate",
               "snapshot", "snapshots", "retained-variants"}
TRAIL = ".,;:)"


def split_trail(tok: str):
    """A path token keeps sentence punctuation outside the path."""
    core = tok
    # stop before a dot or slash: "..", "x/.." and "a..." are path syntax, not sentence punctuation
    while len(core) > 1 and core[-1] in TRAIL and core[-2] not in "./":
        core = core[:-1]
    return core, tok[len(core):]


class Rewriter:
    def __init__(self, pm: PathMap):
        self.pm = pm
        self.fixture = False

    def map_root(self, token: str):
        core, trail = split_trail(token)
        slash = core.endswith("/")
        new = self.pm.new_of(core)
        if new is None:
            return None
        return new + ("/" if slash and not new.endswith("/") else "") + trail

    def rel(self, tok: str, base_old: str, base_new: str, where: list, bare: bool = False):
        core, trail = split_trail(tok)
        slash = core.endswith("/")
        target = posixpath.normpath(posixpath.join(base_old or ".", core))
        if target == ".":
            target = ""
        if target.startswith("..") or (target and not self.pm.known(target)):
            return None
        new_target = self.pm.new_of(target) if target else ""
        if new_target is None:
            where.append(("SPLIT", target))
            return None
        if new_target == target and base_old == base_new:
            return None
        out = posixpath.relpath(new_target or ".", base_new or ".")
        if core.startswith("./") and not out.startswith(".."):
            out = "./" + out
        if bare and out.startswith("./"):
            out = out[2:]
        if slash and not out.endswith("/"):
            out += "/"
        return None if out == core.rstrip("/") + ("/" if slash else "") else out + trail

    def text(self, path: str, new_path: str, text: str, where: list) -> str:
        if new_path.startswith(".github/") and new_path.endswith((".yml", ".yaml")):
            return self.workflow(path, new_path, text, where)
        return self.plain(path, new_path, text, where)

    WHOLE = {"m3", "m3/", "m3/**", "m3/*", "./m3", "./m3/"}
    LIST_HEAD = re.compile(r"^(\s*)(?:paths|paths-ignore):\s*$")
    SPARSE_HEAD = re.compile(r"^(\s*)sparse-checkout:\s*\|\s*$")
    ITEM = re.compile(r"^(?P<pre>\s*-\s*)(?P<q>['\"]?)(?P<val>[^'\"#]+?)(?P=q)(?P<post>\s*(?:#.*)?)$")
    SPARSE_ITEM = re.compile(r"^(?P<pre>\s*)(?P<q>)(?P<val>!?[^\s#]+)(?P<post>\s*)$")

    def workflow(self, path: str, new_path: str, text: str, where: list) -> str:
        """Workflow trigger filters and sparse-checkout lists are unions: an entry whose old path still holds content
        keeps its line and the moved path is added after it; m3/** adds make/m3/** and doc/m3/**."""
        out, mode, indent = [], None, -1
        for line in text.split("\n"):
            stripped = line.strip()
            ind = len(line) - len(line.lstrip())
            if mode and stripped and ind <= indent:
                mode = None
            head = self.LIST_HEAD.match(line) or self.SPARSE_HEAD.match(line)
            if head:
                mode, indent = ("sparse" if "sparse" in line else "paths"), len(head.group(1))
                out.append(line)
                continue
            item = (self.ITEM if mode == "paths" else self.SPARSE_ITEM).match(line) if mode else None
            val = item.group("val").strip() if item else ""
            neg = val.startswith("!")
            core = val[1:] if neg else val
            if item and core.startswith(("m3/", "./m3/")) or (item and core in self.WHOLE):
                rows = self.union(core, mode, where)
                if rows is not None:
                    pre, q, post = item.group("pre"), item.group("q"), item.group("post")
                    out.extend(f"{pre}{q}{'!' if neg else ''}{v}{q}{post}" for v in rows)
                    continue
            out.append(self.plain(path, new_path, line, where))
        return "\n".join(out)

    def union(self, core: str, mode: str, where: list):
        if core in self.WHOLE:
            tail = core[core.index("m3") + 2:]
            return [core, "make/m3" + tail, "doc/m3" + tail]
        lit = re.split(r"/[^/]*[*?\[{][^/]*", core.lstrip("./"), maxsplit=1)[0]
        new = self.map_root(core.lstrip("./"))
        if new is None:
            where.append(("SPLIT", core))
            return None
        if new == core.lstrip("./"):
            return None
        return [core, new] if self.pm.stays(lit) else [new]

    def plain(self, path: str, new_path: str, text: str, where: list) -> str:
        old_dir, new_dir = posixpath.dirname(path), posixpath.dirname(new_path)
        if self.fixture:
            # a fixture test keeps its synthetic paths; a path joined to a root derived from __file__ (root / "m3/x")
            # names this repository and follows the move
            roots = file_roots(text)
            if roots:
                joined = re.compile(r"(?<![\w.])(?:" + "|".join(map(re.escape, sorted(roots))) +
                                    r")\s*/\s*([\"'])(?P<path>m3/[^\"'\n]+)\1")

                def join_sub(m):
                    new = self.map_root(m.group("path"))
                    if new is None:
                        where.append(("SPLIT", m.group("path")))
                        return m.group(0)
                    return m.group(0)[: m.start("path") - m.start(0)] + new + m.group(0)[m.end("path") - m.start(0):]
                text = joined.sub(join_sub, text)
            return self.located(path, new_path, text, where, old_dir, new_dir)

        def root_sub(m):
            tok = m.group("path")
            new = self.map_root(tok)
            if new is None:
                where.append(("SPLIT", tok))
                return m.group(0)
            return m.group(0)[: m.start("path") - m.start(0)] + new

        text = ROOT_FORM.sub(root_sub, text)

        def comp_sub(m):
            if m.group("sep") == "," and not CALL_CTX.search(text[max(0, m.start() - 120): m.start()]):
                return m.group(0)
            found = COMP_ONE.findall(m.group("more"))
            comps, seps = [c[2] for c in found], {c[0] for c in found}
            if len(seps) != 1:
                return m.group(0)
            old = "/".join(["m3", *comps])
            if not self.pm.known(old):
                return m.group(0)
            new = self.pm.new_of(old)
            if new is None:
                where.append(("SPLIT", old))
                return m.group(0)
            if new == old:
                return m.group(0)
            q, sep = m.group("q"), seps.pop()
            joiner = f"{q} {sep} {q}" if sep == "/" else f"{q}, {q}"
            return q + joiner.join(new.split("/")) + q

        text = COMPONENTS.sub(comp_sub, text)
        return self.located(path, new_path, text, where, old_dir, new_dir)

    def located(self, path: str, new_path: str, text: str, where: list, old_dir: str, new_dir: str) -> str:
        """The forms that depend on the file's own location: relative paths, pom paths, __file__ parents."""

        def rel_sub(m):
            new = self.rel(m.group("rel"), old_dir, new_dir, where)
            return m.group(0) if new is None else new

        text = REL_FORM.sub(rel_sub, text)
        if new_path.endswith("pom.xml"):
            def elem_sub(m):
                val = m.group("val")
                if val.startswith("/") or "://" in val or not re.fullmatch(PATHCH + "+", val):
                    return m.group(0)
                if val.startswith("."):
                    return m.group(0)  # already handled by the relative form
                new = self.rel(val, old_dir, new_dir, where, bare=True)
                return m.group(0) if new is None else f"<{m.group('tag')}>{new}</{m.group('tag')}>"
            text = POM_ELEM.sub(elem_sub, text)

            def base_sub(m):
                val = m.group("val")
                if val.startswith("."):
                    return m.group(0)
                new = self.rel(val, old_dir, new_dir, where, bare=True)
                return m.group(0) if new is None else m.group(0)[: m.start("val") - m.start(0)] + new
            text = POM_BASEDIR.sub(base_sub, text)
            text = self.implicit_parent(text, old_dir, new_dir)
        if new_path.endswith(".py"):
            text = self.py_parents(text, path, new_path, where)
        return text

    def implicit_parent(self, text: str, old_dir: str, new_dir: str) -> str:
        m = PARENT_BLOCK.search(text)
        if not m or "<relativePath" in m.group("body") or not old_dir:
            return text
        old_parent = posixpath.normpath(posixpath.join(old_dir, "../pom.xml"))
        if old_parent.startswith("..") or old_parent not in self.pm.files:
            return text
        rel = posixpath.relpath(self.pm.new_of(old_parent), new_dir or ".")
        if rel == "../pom.xml":
            return text
        body = m.group("body")
        indent = re.search(r"\n([ \t]*)<artifactId>", body)
        pad = indent.group(1) if indent else "    "
        end = body.rstrip()
        new_body = end + f"\n{pad}<relativePath>{rel}</relativePath>" + body[len(end):]
        return text[: m.start("body")] + new_body + text[m.end("body"):]

    def py_parents(self, text: str, path: str, new_path: str, where: list) -> str:
        old_par = path.split("/")[:-1]
        new_par = new_path.split("/")[:-1]

        def ancestor(parts, n):
            return "/".join(parts[: len(parts) - n]) if n <= len(parts) else None

        def new_index(n):
            a = ancestor(old_par, n)
            if a is None:
                return None
            target = self.pm.new_of(a) if a else ""
            if target is None:
                where.append(("SPLIT", a + " (parents)"))
                return None
            for k in range(len(new_par) + 1):
                if ancestor(new_par, k) == target:
                    return k
            where.append(("PARENTS", f"{a} -> {target}"))
            return None

        def sub_idx(m):
            n = int(m.group("n"))
            k = new_index(n)
            return m.group(0) if k is None or k == n else f"{m.group('head')}.parents[{k}]"

        def sub_chain(m):
            n = m.group("chain").count(".parent") - 1
            k = new_index(n)
            return m.group(0) if k is None or k == n else m.group("head") + ".parent" * (k + 1)

        text = PY_PARENTS.sub(sub_idx, text)
        text = PY_CHAIN.sub(sub_chain, text)
        return self.py_var_parents(text, new_index)

    def py_var_parents(self, text: str, new_index) -> str:
        """HERE = Path(__file__).resolve().parent (or .parents[N]); ROOT = HERE.parents[M] / HERE.parent.parent: the
        variable is an ancestor of the file at a fixed index, so its own parents are re-indexed like the file's."""
        base = {}
        for m in PY_VAR_CHAIN.finditer(text):
            base[m.group("var")] = m.group("chain").count(".parent") - 1
        for m in PY_VAR_INDEX.finditer(text):
            base[m.group("var")] = int(m.group("n"))
        for var in list(base):  # a name also bound to something else (a temp root, an argument) is left alone
            binds = len(re.findall(r"(?m)^[ \t]*" + re.escape(var) + r"\s*=(?!=)", text))
            ties = len([m for m in PY_VAR_CHAIN.finditer(text) if m.group("var") == var])
            ties += len([m for m in PY_VAR_INDEX.finditer(text) if m.group("var") == var])
            if binds != ties:
                del base[var]
        if not base:
            return text

        def sub_idx(m):
            b = base.get(m.group("var"))
            if b is None:
                return m.group(0)
            n = int(m.group("n"))
            k = new_index(b + n + 1)
            return m.group(0) if k is None or k - b - 1 == n or k - b - 1 < 0 else f"{m.group('var')}.parents[{k - b - 1}]"

        def sub_chain(m):
            b = base.get(m.group("var"))
            if b is None:
                return m.group(0)
            c = m.group("chain").count(".parent")
            k = new_index(b + c)
            return m.group(0) if k is None or k - b == c or k - b < 1 else m.group("var") + ".parent" * (k - b)

        text = VAR_PARENTS.sub(sub_idx, text)
        return VAR_CHAIN.sub(sub_chain, text)


PROTECT = ("make/m3/integrate/",)  # MAPPING.tsv and the ledgers name old paths and base blob ids on purpose


def load_protect(path: Path = HERE / "PROTECT.tsv") -> dict:
    """mode -> regexes over old or new paths: bytes (never rewritten) and fixture (only located forms rewritten)."""
    out: dict = {"bytes": [], "fixture": []}
    if path.is_file():
        for row in read_tsv(path):
            out[row[1]].append(re.compile(row[0]))
    return out


PROTECTED = load_protect()


def skip_content(path: str) -> bool:
    return (path.startswith(PROTECT) or any(seg in FIXTURE_SEG for seg in path.split("/")[:-1])
            or any(r.search(path) for r in PROTECTED["bytes"]))


ASSIGN = re.compile(r"(?m)^[ \t]*(?P<var>[A-Za-z_]\w*)[ \t]*=(?!=)[ \t]*(?P<expr>[^\n#]*?)[ \t]*(?:#.*)?$")
FILE_EXPR = re.compile(r"^Path\(__file__\)(?:\.resolve\(\)|\.absolute\(\))?(?:\.parents\[\d+\]|(?:\.parent)+)$")


def file_roots(text: str) -> set:
    """Names bound only to ancestors of __file__: HERE = Path(__file__).parent, ROOT = HERE.parents[2],
    root = Path(__file__).resolve().parents[3]. A name also bound to anything else (a temporary tree, an argument) is
    not a repository root."""
    binds: dict = {}
    for m in ASSIGN.finditer(text):
        binds.setdefault(m.group("var"), []).append(m.group("expr").strip())
    roots: set = set()
    while True:
        derived = re.compile(r"^(?:" + "|".join(map(re.escape, sorted(roots))) + r")(?:\.parents\[\d+\]|(?:\.parent)+)$") \
            if roots else None
        grown = {var for var, exprs in binds.items()
                 if all(FILE_EXPR.match(e) or (derived and derived.match(e)) for e in exprs)}
        if grown == roots:
            return roots
        roots = grown


def fixture_test(path: str, text: str = "") -> bool:
    """PROTECT.tsv fixture rows: tests whose m3/... literals are inputs of a recorded transformation."""
    return any(r.search(path) for r in PROTECTED["fixture"])


def fast_import(repo: str, rev: str, ref: str, message: str, files: dict, modes: dict) -> str:
    ident = git(repo, "var", "GIT_AUTHOR_IDENT").decode().strip()
    cident = git(repo, "var", "GIT_COMMITTER_IDENT").decode().strip()
    parent = git(repo, "rev-parse", rev + "^{commit}").decode().strip()
    msg = message.encode()
    out = [f"commit {ref}\nauthor {ident}\ncommitter {cident}\n".encode(), b"data %d\n" % len(msg), msg,
           f"\nfrom {parent}\n".encode()]
    for path, data in sorted(files.items()):
        q = path.encode("utf-8", "surrogateescape").replace(b"\\", b"\\\\").replace(b'"', b'\\"')
        out.append(f"M {modes.get(path, '100644')} inline ".encode() + b'"' + q + b'"\n')
        out.append(b"data %d\n" % len(data) + data + b"\n")
    out.append(b"done\n")
    git(repo, "fast-import", "--quiet", "--done", "--force", data=b"".join(out))
    return git(repo, "rev-parse", ref).decode().strip()


def cmd_refs(args) -> int:
    rows = load_mapping(args.mapping, set(args.phase.split(",")), set(args.status.split(",")))
    every = load_mapping(args.mapping, {"P1", "P2", "P3", "P4"},
                         {"MOVE", "AMBIGUOUS", "RETIRE", "HOLD_P1", "HOLD_APPROVAL"})
    retired = {r[0] for r in every if r[4] == "RETIRE"} - {r[0] for r in rows}
    pm = PathMap(tree_entries(args.repo, args.base), rows, retired)
    back = {v: k for k, v in pm.fmap.items()}
    new_entries = tree_entries(args.repo, args.rev)
    modes = {p: m for p, m, _s in new_entries}
    todo = [(p, s) for p, _m, s in new_entries if back.get(p, p) not in retired and not skip_content(back.get(p, p))]
    rw, findings, changed = Rewriter(pm), [], {}
    for i, data in enumerate(blobs(args.repo, [s for _p, s in todo])):
        if i % 10000 == 0:
            print(f"refs {i}/{len(todo)}", file=sys.stderr, flush=True)
        path_new = todo[i][0]
        if b"\0" in data[:8192] or len(data) > (32 << 20):
            continue
        path_old = back.get(path_new, path_new)
        moved = path_old != path_new
        if b"m3" not in data and not (moved and (b"./" in data or b"parent" in data or b".." in data)):
            continue
        text = data.decode("utf-8", "surrogateescape")
        where: list = []
        rw.fixture = fixture_test(path_old, text)
        new = rw.text(path_old, path_new, text, where)
        findings.extend(f"{kind}\t{path_new}\t{tok}" for kind, tok in where)
        if new != text:
            changed[path_new] = new.encode("utf-8", "surrogateescape")
    Path(args.findings).write_text("kind\tfile\ttoken\n" + "\n".join(sorted(set(findings))) + "\n",
                                   encoding="utf-8", newline="\n")
    print(f"M3_TO_JDK_LAYOUT refs scanned={len(todo)} changed={len(changed)} findings={len(set(findings))}")
    if args.list:
        Path(args.list).write_text("\n".join(sorted(changed)) + "\n", encoding="utf-8", newline="\n")
    if args.ref and changed:
        print(f"COMMIT\t{args.ref}\t{fast_import(args.repo, args.rev, args.ref, args.message, changed, modes)}")
    return 0


def cmd_residue(args) -> int:
    """Root-form references to an old moved path (file or directory) still in live files of rev."""
    rows = load_mapping(args.mapping, set(args.phase.split(",")), set(args.status.split(",")))
    gone = {r[0] for r in rows}
    moved_dirs = set()
    for r in rows:
        parts = r[0].split("/")
        moved_dirs.update("/".join(parts[:k]) for k in range(2, len(parts)))
    every = tree_entries(args.repo, args.rev)
    staying = set()
    for p, _m, _s in every:
        if p not in gone:
            parts = p.split("/")
            staying.update("/".join(parts[:k]) for k in range(1, len(parts)))
    entries = [(p, s) for p, _m, s in every if not skip_content(p) and p not in gone]
    hits = []
    for i, data in enumerate(blobs(args.repo, [s for _p, s in entries])):
        if b"m3/" not in data or b"\0" in data[:8192]:
            continue
        path = entries[i][0]
        text = data.decode("utf-8", "surrogateescape")
        if fixture_test(path, text):
            continue
        for n, line in enumerate(text.split("\n"), 1):
            for m in ROOT_FORM.finditer(line):
                core = split_trail(m.group("path"))[0].rstrip("/")
                if core in gone or (core in moved_dirs and core not in staying):
                    hits.append(f"{path}:{n}\t{core}\t{line.strip()[:160]}")
    Path(args.out).write_text("location\told_path\tline\n" + "\n".join(hits) + "\n", encoding="utf-8", newline="\n")
    print(f"M3_TO_JDK_LAYOUT residue rev={args.rev} hits={len(hits)}")
    return 2 if hits else 0


# ---------------------------------------------------------------- repin: digests reseal leaves ambiguous
PATHISH = re.compile(r"[A-Za-z0-9_.$+@~-]+(?:/[A-Za-z0-9_.$+@~-]+)+")
HEXTOK = re.compile(r"(?<![0-9A-Za-z])(?:[0-9a-fA-F]{64}|[0-9a-fA-F]{40})(?![0-9A-Za-z])")
WINDOW = 4


def cmd_repin(args) -> int:
    """A digest that still names an unchanged copy somewhere (reseal: AMBIGUOUS) is carried only where the record
    names its file: a path within WINDOW lines of the digest whose base bytes have that digest. One candidate path is
    required; anything else is left and reported."""
    import hashlib
    amb = set()
    for raw in Path(args.ledger).read_text(encoding="utf-8").splitlines()[1:]:
        kind, before, *_ = raw.split("\t")
        if kind == "AMBIGUOUS":
            amb.add(before.lower())
    rows = load_mapping(args.mapping, set(args.phase.split(",")), set(args.status.split(",")))
    fmap = {r[0]: r[1] for r in rows if r[1] != "RETIRE"}
    back = {v: k for k, v in fmap.items()}
    old = tree_entries(args.repo, args.base)
    by_digest: dict = defaultdict(set)
    old_blob = {p: s for p, _m, s in old}
    for p, s in old_blob.items():
        if s in amb:
            by_digest[s].add(p)
    for i, data in enumerate(blobs(args.repo, [s for _p, _m, s in old])):
        h = hashlib.sha256(data).hexdigest()
        if h in amb:
            by_digest[h].add(old[i][0])
    new = tree_entries(args.repo, args.rev)
    modes = {p: m for p, m, _s in new}
    new_blob = {p: s for p, _m, s in new}
    need = {fmap.get(p, p) for ps in by_digest.values() for p in ps}
    new_sha = {}
    todo = [(p, new_blob[p]) for p in need if p in new_blob]
    for i, data in enumerate(blobs(args.repo, [s for _p, s in todo])):
        new_sha[todo[i][0]] = hashlib.sha256(data).hexdigest()
    entries = [(p, s) for p, _m, s in new if not skip_content(p)]
    changed, report = {}, []
    for i, data in enumerate(blobs(args.repo, [s for _p, s in entries])):
        if b"\0" in data[:8192] or not HEXTOK.search(data.decode("latin-1")):
            continue
        path = entries[i][0]
        text = data.decode("utf-8", "surrogateescape")
        if not any(t.lower() in amb for t in HEXTOK.findall(text)):
            continue
        lines = text.split("\n")
        for n, line in enumerate(lines):
            def sub(m):
                tok = m.group(0)
                d = tok.lower()
                if d not in amb:
                    return tok
                near = "\n".join(lines[max(0, n - WINDOW): n + WINDOW + 1])
                cands = set()
                for ptok in PATHISH.findall(near):
                    p = ptok.lstrip("./")
                    for oldp in (back.get(p, p), p):
                        if oldp in by_digest[d]:
                            cands.add(fmap.get(oldp, oldp))
                if len(cands) != 1:
                    report.append(f"LEFT\t{path}:{n + 1}\t{tok}\t{len(cands)}")
                    return tok
                target = cands.pop()
                if target not in new_blob:
                    report.append(f"GONE\t{path}:{n + 1}\t{tok}\t{target}")
                    return tok
                value = new_sha[target] if len(tok) == 64 else new_blob[target]
                value = value.upper() if tok != tok.lower() else value
                report.append(f"REPIN\t{path}:{n + 1}\t{tok}\t{value}\t{target}")
                return value
            lines[n] = HEXTOK.sub(sub, line)
        out = "\n".join(lines)
        if out != text:
            changed[path] = out.encode("utf-8", "surrogateescape")
    Path(args.out).write_text("kind\tlocation\tbefore\tafter_or_candidates\ttarget\n" + "\n".join(report) + "\n",
                              encoding="utf-8", newline="\n")
    kinds = Counter(r.split("\t", 1)[0] for r in report)
    print(f"M3_TO_JDK_LAYOUT repin ambiguous={len(amb)} files={len(changed)} " +
          " ".join(f"{k}={v}" for k, v in sorted(kinds.items())))
    if args.ref and changed:
        print(f"COMMIT\t{args.ref}\t{fast_import(args.repo, args.rev, args.ref, args.message, changed, modes)}")
    return 0


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    sub = ap.add_subparsers(dest="cmd", required=True)
    m = sub.add_parser("map")
    m.add_argument("--repo", default=".")
    m.add_argument("--rev", default="HEAD")
    m.add_argument("--rules", default=str(HERE / "RULES.tsv"))
    m.add_argument("--areas", default=str(HERE / "AREAS.tsv"))
    m.add_argument("--out", default=str(HERE))
    for name in ("move", "refs", "residue", "repin"):
        p = sub.add_parser(name)
        p.add_argument("--mapping", default=str(HERE / "MAPPING.tsv"))
        p.add_argument("--phase", required=True)
        p.add_argument("--status", default="MOVE,AMBIGUOUS")
        p.add_argument("--repo", default=".")
        p.add_argument("--rev", default="HEAD")
        if name == "refs":
            p.add_argument("--base", required=True)
            p.add_argument("--ref")
            p.add_argument("--findings", default="REFS_FINDINGS.tsv")
            p.add_argument("--list")
            p.add_argument("--message", default="M3_TO_JDK_LAYOUT: references follow the moved paths")
        if name == "residue":
            p.add_argument("--out", default="RESIDUE.tsv")
        if name == "repin":
            p.add_argument("--base", required=True)
            p.add_argument("--ledger", required=True, help="RESEAL_<phase>.tsv of reseal_moves.py")
            p.add_argument("--ref")
            p.add_argument("--out", default="REPIN.tsv")
            p.add_argument("--message", default="M3_TO_JDK_LAYOUT: carry ambiguous digests anchored by the path they name")
    a = ap.parse_args(argv)
    return {"map": cmd_map, "move": cmd_move, "refs": cmd_refs, "residue": cmd_residue, "repin": cmd_repin}[a.cmd](a)


if __name__ == "__main__":
    sys.exit(main())
