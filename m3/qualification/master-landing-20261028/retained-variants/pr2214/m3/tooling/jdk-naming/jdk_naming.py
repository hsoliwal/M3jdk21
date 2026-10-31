#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Rename Synexia names in M3JDK21 to JDK naming (recipe JDK_NAMING_RENAME, user decision 2026-10-31).

Scheme: modules and exported packages jdk.m3.<area>; internal packages jdk.internal.m3.<area>; com.synexia.* ->
jdk.internal.m3.*; Synexia* -> M3*; synexia-* -> m3-*; SYNEXIA_* -> M3_*. Legacy recipe code is renamed and kept
(absorb by rename), not deleted. Untouched: copyright, SPDX and license lines; references to the hsoliwal/com.synexia
repository (dependencies, removed by the standalone work, not renamed); documentation and lineage (Markdown, TSV
ledgers, provenance and evidence JSON, NOTICE/LICENSE, m3/docs, release audits).

plan: every tracked path gets its renamed path (refused on collision); every in-scope text file gets its rewrite
count per rule. apply: moves plus rewrites, only when the plan has no refusal and every in-scope file is checked out
(a file outside a sparse checkout has unknown content; pass 1 missed such content-only files: RENAME_COMPLETENESS).
--tree REV reads the files as blobs of REV instead of the checkout, and apply writes one commit to --ref through git
fast-import, so a sparse or partial checkout renames the whole tree. Bytes are kept as they are (CRLF too; pass 1
read text mode and dropped CRLF in 4 files). A second plan after apply finds nothing (fixed point).
CLI: python -I jdk_naming.py plan|apply [--root DIR] [--tree REV --ref REF [--message TEXT]] [--tsv OUT] [--only P]...
"""
from __future__ import annotations

import argparse
import os
import re
import subprocess
import sys
import threading
from pathlib import Path

# The rules live in NAMING_RULES.tsv (one table, read by this recipe and by the JUnit conformance test in
# conformance/). Ordered: the first matching rule rewrites a token; earlier rules are more specific. Paths are renamed
# by the same rules (renamed_path), so a reference in text and the file it names always agree (pass 1 had separate
# path rules that accepted _synexia_ after an underscore while the text rules needed \b: files moved, their callers
# did not; 17 workflows called m3/compatibility/check_synexia_*.py after the move: PATH_TEXT_AGREEMENT).
# No rule renames the bare word Synexia: it names the donor project in attribution and lineage prose ("Adapted from
# Synexia MIndexRegexTrigramQuery", "No Synexia runtime dependency", a GPL modification notice) and renaming it would
# make those statements false; identifiers never keep it. Copyright and SPDX lines are kept whole; dependencies are
# kept as spans and the rest of their line is renamed (renaming a dependency would hide it from the standalone check;
# a whole kept line once left <groupId>com.synexia</groupId><artifactId>m3-x</artifactId> pointing at a renamed
# module).
RULES_FILE = Path(__file__).with_name("NAMING_RULES.tsv")
# every path move of the tree-mode apply, so the old path of any file stays mapped to its new one (reseal.py maps
# the digests the same way in RENAME_RESEAL.tsv)
RENAME_MAP = "m3/tooling/jdk-naming/RENAME_MAP.tsv"


def load_rules(path: Path = RULES_FILE) -> tuple:
    """TEXT rules (every line), PROSE rules (whole comment lines only), and the keep patterns."""
    rows = [line.split("\t") for line in path.read_text(encoding="utf-8").splitlines()[1:] if line.strip()]
    rules = tuple(tuple((name, re.compile(pattern), replacement) for kind, name, pattern, replacement, _note in rows
                        if kind == wanted) for wanted in ("TEXT", "PROSE"))
    keeps = tuple(re.compile("|".join(pattern for k, _n, pattern, _r, _note in rows if k == kind))
                  for kind in ("KEEP_LINE", "KEEP_SPAN", "KEEP_PATH"))
    return (*rules, *keeps)


TEXT_RULES, PROSE_RULES, KEEP_LINE, KEEP_SPAN, KEEP_PATH = load_rules()
SPAN_TOKEN = re.compile("\x00(\\d+)\x00")
# a whole comment line: the PROSE rules apply only here, so string literals and data keep their exact text
COMMENT_LINE = re.compile(r"^\s*(?://|/\*|\*|#|<!--)")
# every rule's pattern contains one of these, so a line without them is never rewritten (skips most of 1.1 GB of text)
QUICK = re.compile(r"(?i)synexia|com[./_]{1,2}m3[./_]|m3([-_./])m3\1|m3[ _-]?jdk")
TEXT_SUFFIXES = {".java", ".yml", ".yaml", ".xml", ".sh", ".py", ".mjs", ".js", ".properties", ".gradle", ".toml",
                 ".cfg", ".ini", ".c", ".h", ".cpp", ".json", ".txt"}
DOC_SUFFIXES = {".md", ".tsv", ".rst", ".adoc", ".html", ".log", ".patch"}
DOC_NAMES = {"NOTICE", "LICENSE", "COPYRIGHT", "AUTHORS", "THIRD_PARTY_NOTICES"}
LINEAGE_HINTS = ("provenance", "evidence", "receipt", "manifest", "SOURCE_SHA256", "FINAL_", "readback")
# files that define Synexia patterns on purpose: this recipe and the standalone check (renaming them blinds the check)
SELF_PREFIX = "m3/tooling/jdk-naming/"
SELF = {"m3/release/check_standalone.py", "m3/release/test_check_standalone.py"}


def is_self(path: str) -> bool:
    return path.startswith(SELF_PREFIX) or path in SELF


def tracked(root: Path) -> list[str]:
    out = subprocess.run(["git", "-C", str(root), "ls-files", "-z"], check=True, capture_output=True).stdout
    return [p for p in out.decode("utf-8", "surrogateescape").split("\0") if p]


def is_lineage(path: str) -> bool:
    p = Path(path)
    suffixes = {"." + part for part in p.name.lower().split(".")[1:]}  # x.log.txt is a log
    return (suffixes & DOC_SUFFIXES or p.name in DOC_NAMES or path.startswith(("m3/docs/", "m3/release/audit-"))
            or "/evidence/" in path or "/receipts/" in path or "/surefire-reports/" in path
            or p.name.startswith("TEST-") and p.suffix.lower() == ".xml"
            or p.suffix.lower() == ".json" and any(h.lower() in path.lower() for h in LINEAGE_HINTS))


def renamed_path(path: str) -> str:
    return rewrite_line(path, {})


def rewrite_line(line: str, counts: dict) -> str:
    if KEEP_LINE.search(line):
        return line
    spans: list[str] = []

    def stash(match):
        spans.append(match.group(0))
        return "\x00%d\x00" % (len(spans) - 1)
    comment = COMMENT_LINE.match(line) is not None
    line = KEEP_SPAN.sub(stash, line)
    for name, rule, replacement in TEXT_RULES + (PROSE_RULES if comment else ()):
        line, n = rule.subn(replacement, line)
        if n:
            counts[name] = counts.get(name, 0) + n
    line = SPAN_TOKEN.sub(lambda m: spans[int(m.group(1))], line)
    return line


def rewrite_text(text: str) -> tuple[str, dict]:
    counts: dict = {}
    if not QUICK.search(text):
        return text, counts
    lines = text.split("\n")
    return "\n".join(rewrite_line(line, counts) if QUICK.search(line) else line for line in lines), counts


def in_scope(path: str, only: tuple[str, ...]) -> bool:
    if is_self(path) or is_lineage(path):
        return False
    if only and not path.startswith(only):
        return False
    return Path(path).suffix.lower() in TEXT_SUFFIXES or Path(path).name == "pom.xml"


def keeps_name(path: str) -> str:
    """The path with its directory renamed and its file name kept."""
    directory, _slash, name = path.rpartition("/")
    return (renamed_path(directory) + "/" + name) if directory else name


def target_path(path: str) -> str:
    """The renamed path; a legal or attribution file (KEEP_PATH) keeps its name and follows its renamed directory, so
    licenses/Synexia-LICENSE stays next to the files of its crate (KEEP_PATH once kept the whole path and split
    m3-openrewrite-recipes/ from its renamed siblings: LEGAL_FOLLOWS_DIRECTORY)."""
    return keeps_name(path) if KEEP_PATH.search(path) else renamed_path(path)


def plan_moves(paths: list[str], only: tuple[str, ...]) -> dict:
    """Every path move. Two paths whose renamed paths coincide (a delivery image holding both jdk.m3.x-jar.log.txt
    and jdk.m3.x-jar.log.txt) collide; a colliding lineage file follows its directory and keeps its name
    (KEEP_LINEAGE_NAME), or stays where it is when that name is taken too (KEEP_LINEAGE_PATH); anything else
    refuses. NamingScan.pathConformant mirrors this rule."""
    existing = set(paths)
    chosen = [p for p in paths if not (is_self(p) or only and not p.startswith(only))]
    full = {p: target_path(p) for p in chosen}
    claims: dict = {}
    for new in full.values():
        claims[new] = claims.get(new, 0) + 1
    moves, refusals, targets = [], [], set()
    for path in chosen:
        new = full[path]
        if new == path:
            continue
        if new in existing or claims[new] > 1:
            if not is_lineage(path):
                refusals.append(("COLLISION", path, new))
                continue
            new = keeps_name(path)
            if new == path or new in existing or new in targets:
                continue
        targets.add(new)
        moves.append((path, new))
    return {"moves": moves, "rewrites": [], "refusals": refusals, "absent": []}


def decoded(data: bytes) -> str:
    return data.decode("utf-8", "surrogateescape")


def encoded(text: str) -> bytes:
    return text.encode("utf-8", "surrogateescape")


def plan(root: Path, only: tuple[str, ...] = ()) -> dict:
    root = Path(root).resolve()
    paths = tracked(root)
    result = plan_moves(paths, only)
    for path in paths:
        if not in_scope(path, only):
            continue
        file = root / path
        if not file.is_file():
            result["absent"].append(path)  # unknown content: it may name Synexia under a path that does not
            continue
        text = decoded(file.read_bytes())
        new_text, counts = rewrite_text(text)
        if new_text != text:
            result["rewrites"].append((path, counts))
    return result


def git(root: Path, *args: str) -> str:
    return subprocess.run(["git", "-C", str(root), *args], check=True, capture_output=True).stdout.decode().strip()


def tree_entries(root: Path, rev: str) -> list[tuple[str, str, str]]:
    out = subprocess.run(["git", "-C", str(root), "ls-tree", "-r", "-z", "--full-tree", rev], check=True,
                         capture_output=True).stdout
    entries = []
    for record in decoded(out).split("\0"):
        if record:
            meta, path = record.split("\t", 1)
            mode, _kind, sha = meta.split()
            entries.append((path, mode, sha))
    return entries


def blobs(root: Path, shas: list[str]):
    """Yield the bytes of each blob in order from one git cat-file --batch; a missing blob refuses (no lazy fetch)."""
    proc = subprocess.Popen(["git", "-C", str(root), "cat-file", "--batch"], stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, env=dict(os.environ, GIT_NO_LAZY_FETCH="1"))
    request = "".join(sha + "\n" for sha in shas).encode("ascii")
    feeder = threading.Thread(target=lambda: (proc.stdin.write(request), proc.stdin.close()))
    feeder.start()
    for sha in shas:
        header = proc.stdout.readline().split()
        if len(header) != 3:
            proc.kill()
            raise SystemExit("REFUSED: blob not available locally: " + sha)
        data = proc.stdout.read(int(header[2]))
        proc.stdout.read(1)
        yield data
    feeder.join()
    proc.stdout.close()
    proc.wait()


def tree_rewrites(root: Path, entries: list, only: tuple[str, ...]):
    """Yield (path, counts, new bytes) for every in-scope blob of the tree that the rules change."""
    scoped = [(path, sha) for path, mode, sha in entries if mode != "160000" and in_scope(path, only)]
    for index, data in enumerate(blobs(root, [sha for _path, sha in scoped])):  # drained, so cat-file is reaped
        path = scoped[index][0]
        text = decoded(data)
        new_text, counts = rewrite_text(text)
        if new_text != text:
            yield path, counts, encoded(new_text)


def plan_tree(root: Path, rev: str, only: tuple[str, ...] = ()) -> dict:
    entries = tree_entries(Path(root), rev)
    result = plan_moves([path for path, _mode, _sha in entries], only)
    result["rewrites"] = [(path, counts) for path, counts, _data in tree_rewrites(Path(root), entries, only)]
    return result


def quoted(path: str) -> bytes:
    """A fast-import path in C-style quotes (always quoted: spaces, quotes and backslashes need no special case)."""
    return encoded('"' + path.replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"')


def apply_tree(root: Path, rev: str, ref: str, message: str, only: tuple[str, ...] = ()) -> str:
    """Write the renamed tree of REV as one commit on REF (parent REV) through git fast-import; return its id."""
    root = Path(root)
    entries = tree_entries(root, rev)
    result = plan_moves([path for path, _mode, _sha in entries], only)
    if result["refusals"]:
        raise SystemExit("REFUSED: " + "; ".join(f"{k} {a} -> {b}" for k, a, b in result["refusals"][:5]))
    parent = git(root, "rev-parse", rev + "^{commit}")
    proc = subprocess.Popen(["git", "-C", str(root), "fast-import", "--quiet", "--done"], stdin=subprocess.PIPE)
    out, marks = proc.stdin, {}
    for path, _counts, data in tree_rewrites(root, entries, only):
        marks[path] = len(marks) + 1
        out.write(b"blob\nmark :%d\ndata %d\n" % (marks[path], len(data)) + data + b"\n")
    idents = (git(root, "var", "GIT_AUTHOR_IDENT"), git(root, "var", "GIT_COMMITTER_IDENT"))
    out.write(encoded("commit %s\nauthor %s\ncommitter %s\n" % (ref, *idents)))
    out.write(b"data %d\n" % len(encoded(message)) + encoded(message) + b"\nfrom " + parent.encode() + b"\n")
    moved = dict(result["moves"])
    out.writelines(b"D " + quoted(path) + b"\n" for path in moved)
    for path, mode, sha in entries:
        if path in marks or path in moved:
            source = ":%d" % marks[path] if path in marks else sha
            out.write(("M %s %s " % (mode, source)).encode() + quoted(moved.get(path, path)) + b"\n")
    mapping = encoded("path_before\tpath_after\n" + "".join(f"{a}\t{b}\n" for a, b in result["moves"]))
    out.write(b"M 100644 inline " + quoted(RENAME_MAP) + b"\ndata %d\n" % len(mapping) + mapping + b"\n")
    out.write(b"done\n")
    out.close()
    if proc.wait():
        raise SystemExit("REFUSED: git fast-import failed")
    return git(root, "rev-parse", ref)


def apply(root: Path, result: dict) -> None:
    root = Path(root).resolve()
    if result["refusals"]:
        raise SystemExit("REFUSED: " + "; ".join(f"{k} {a} -> {b}" for k, a, b in result["refusals"][:5]))
    for path, _ in result["rewrites"]:
        file = root / path
        file.write_bytes(encoded(rewrite_text(decoded(file.read_bytes()))[0]))
    # moves on disk, then one git add of every old and new path (one git mv per file took minutes per thousand)
    for path, new in result["moves"]:
        target = root / new
        target.parent.mkdir(parents=True, exist_ok=True)
        if target.exists():
            raise SystemExit("REFUSED: move target exists: " + new)
        (root / path).replace(target)
    paths = [p for p, _ in result["rewrites"]] + [p for move in result["moves"] for p in move]
    if paths:
        listing = "\0".join(paths) + "\0"
        subprocess.run(["git", "-C", str(root), "add", "-A", "--sparse", "--pathspec-from-file=-", "--pathspec-file-nul"],
                       input=listing.encode("utf-8", "surrogateescape"), check=True)


def write_tsv(result: dict, out: Path) -> None:
    rows = ["op\tpath\ttarget_or_counts"]
    rows += [f"MOVE\t{a}\t{b}" for a, b in result["moves"]]
    rows += ["REWRITE\t%s\t%s" % (p, ",".join(f"{k}={v}" for k, v in sorted(c.items()))) for p, c in result["rewrites"]]
    rows += [f"REFUSED_{k}\t{a}\t{b}" for k, a, b in result["refusals"]]
    rows += [f"ABSENT\t{p}\tnot checked out" for p in result["absent"]]
    out.write_text("\n".join(rows) + "\n", encoding="utf-8", newline="\n")


def report(result: dict, tsv) -> None:
    if tsv:
        write_tsv(result, Path(tsv))
    totals: dict = {}
    for _, counts in result["rewrites"]:
        for k, v in counts.items():
            totals[k] = totals.get(k, 0) + v
    print("JDK_NAMING moves=%d rewrites=%d refusals=%d absent=%d %s" % (
        len(result["moves"]), len(result["rewrites"]), len(result["refusals"]), len(result["absent"]),
        " ".join(f"{k}={v}" for k, v in sorted(totals.items()))))


def main_tree(args, only: tuple[str, ...]) -> int:
    result = plan_tree(Path(args.root), args.tree, only)
    report(result, args.tsv)
    if args.mode == "plan":
        return 0 if not result["refusals"] else 1
    if not args.ref:
        raise SystemExit("REFUSED: --tree apply needs --ref")
    commit = apply_tree(Path(args.root), args.tree, args.ref, args.message, only)
    again = plan_tree(Path(args.root), commit, only)
    print("COMMIT %s %s\nFIXED_POINT moves=%d rewrites=%d" % (args.ref, commit, len(again["moves"]),
                                                              len(again["rewrites"])))
    return 0 if not again["moves"] and not again["rewrites"] else 1


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("mode", choices=("plan", "apply"))
    parser.add_argument("--root", default=".")
    parser.add_argument("--tsv")
    parser.add_argument("--only", action="append", default=[])
    parser.add_argument("--tree", help="read blobs of this revision instead of the checkout")
    parser.add_argument("--ref", help="with --tree: the ref apply writes its commit to")
    parser.add_argument("--message", default="JDK naming: rename Synexia names (recipe JDK_NAMING_RENAME)")
    args = parser.parse_args(argv)
    only = tuple(args.only)
    if args.tree:
        return main_tree(args, only)
    result = plan(Path(args.root), only)
    report(result, args.tsv)
    if args.mode == "apply":
        if result["absent"]:
            print("REFUSED: files to rename are not checked out; widen the checkout first")
            return 2
        apply(Path(args.root), result)
        again = plan(Path(args.root), tuple(args.only))
        print("FIXED_POINT moves=%d rewrites=%d" % (len(again["moves"]), len(again["rewrites"])))
        return 0 if not again["moves"] and not again["rewrites"] else 1
    return 0 if not result["refusals"] else 1


if __name__ == "__main__":
    sys.exit(main())
