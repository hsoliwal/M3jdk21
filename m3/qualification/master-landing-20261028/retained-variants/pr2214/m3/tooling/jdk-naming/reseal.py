#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Reseal after the JDK naming rename (recipe JDK_NAMING_RESEAL).

The rename changes the bytes and paths of files that custody manifests, verifier constants and receipts pin by sha256,
git blob id or repository path (CI on the first rename: "source custody mismatch", "installed source mismatch",
"receiver source drift"). This stage carries every pin to the file's new value.

It is a graph pass, not rounds (rounds cost one full scan per level of a custody chain and the chains are deeper
than 8): one scan hashes every blob of the renamed tree into a digest index (a base digest of renamed bytes names the
renamed blob); a second scan records which blob pins which; the blobs that must change (a stale pin, a moved path, or
a pin on a blob that changes) are resealed in dependency order, so a receipt is resealed after the manifest it pins.
Consistent records cannot form a pin cycle (a record exists before its hash is pinned), so a cycle means a corrupted
record; it cannot be sealed by content hashes: its members are resealed with the values known so far and reported
(CYCLE). The first version of this stage carried pins in rounds and stopped at 8: hash-chained history resources
(each entry pins the previous one) are deeper than that (RECIPE_MEMORY: RESEAL_DEPTH).
RENAME_RESEAL.tsv records every carried value (kind before after), so an old digest in history stays traceable: the
ledger is the lineage, the manifests are live pins. Not carried, and reported instead of guessed: an old digest that
still names bytes in the tree while its renamed copy moved on (AMBIGUOUS), and composite digests over sets of files,
whose owners recompute them.
CLI: python -I reseal.py --base BASE --tree REV [--ref REF] [--message TEXT]
"""
from __future__ import annotations

import argparse
import hashlib
import importlib.util
import re
import subprocess
import sys
from collections import defaultdict
from pathlib import Path

SPEC = importlib.util.spec_from_file_location("jdk_naming", Path(__file__).with_name("jdk_naming.py"))
jn = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(jn)

LEDGER = "m3/tooling/jdk-naming/RENAME_RESEAL.tsv"
HEX = re.compile(r"(?<![0-9A-Za-z])(?:[0-9a-f]{64}|[0-9a-f]{40}|[0-9A-F]{64}|[0-9A-F]{40})(?![0-9A-Za-z])")
HEX_QUICK = re.compile(r"[0-9a-fA-F]{40}")
PATHISH = re.compile(r"[A-Za-z0-9_.$+@~-]+(?:/[A-Za-z0-9_.$+@~-]+)+")


def digests(data: bytes) -> tuple[str, str]:
    return hashlib.sha256(data).hexdigest(), hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


class Pins:
    """Path moves and base -> renamed digests of the rename; an old value with two different new values is
    ambiguous."""

    def __init__(self):
        self.map: dict = {}
        self.kind: dict = {}
        self.ambiguous: set = set()
        self.roots = None  # regex of the first segments of the moved paths, set by clean()

    def add(self, kind: str, old: str, new: str) -> None:
        if old == new or old in self.ambiguous:
            return
        if old in self.map and self.map[old] != new:
            self.ambiguous.add(old)
            del self.map[old]
            del self.kind[old]
            return
        self.map[old], self.kind[old] = new, kind

    def add_bytes(self, before: bytes, after: bytes) -> None:
        for kind, old, new in zip(("SHA256", "BLOB"), digests(before), digests(after)):
            self.add(kind, old, new)
            self.add(kind, old.upper(), new.upper())

    def clean(self) -> None:
        roots = sorted({old.split("/", 1)[0] for old, kind in self.kind.items() if kind == "PATH" and "/" in old})
        self.roots = re.compile(r"(?:^|[^A-Za-z0-9_.$+@~-])(?:" + "|".join(map(re.escape, roots)) + ")/") if roots else None


def carried(text: str, pins: Pins, tokens: dict | None = None) -> str:
    """The text with each digest token in TOKENS replaced and every moved repository path carried."""
    if tokens and HEX_QUICK.search(text):
        text = HEX.sub(lambda m: tokens.get(m.group(0), m.group(0)), text)
    if not pins.roots or not pins.roots.search(text):
        return text
    # a moved path token starts with one of the moved roots; only lines with such a root pay for the path regex
    return "\n".join(PATHISH.sub(lambda m: carried_path(m.group(0), pins), line) if pins.roots.search(line) else line
                     for line in text.split("\n"))


def carried_path(token: str, pins: Pins) -> str:
    start = 0
    while True:  # the token or a suffix of it after a slash ("./m3/x", "repo/m3/x") names a repository path
        new = pins.map.get(token[start:])
        if new is not None and pins.kind.get(token[start:]) == "PATH":
            return token[:start] + new
        start = token.find("/", start) + 1
        if start == 0:
            return token


def rename_pins(root: Path, base: str, rev: str) -> tuple[Pins, list, dict]:
    old = {p: s for p, m, s in jn.tree_entries(root, base) if m != "160000"}
    new = {p: s for p, m, s in jn.tree_entries(root, rev) if m != "160000"}
    moves = dict(jn.plan_moves(list(old), ())["moves"])
    pairs, unmapped = [], []
    for path, sha in old.items():
        target = moves.get(path, path)
        if target not in new:
            unmapped.append(path)
        elif target != path or new[target] != sha:
            pairs.append((path, target, sha, new[target]))
    pins = Pins()
    for path, target, _sha, _new_sha in pairs:
        pins.add("PATH", path, target)
    changed = [(sha, new_sha) for _p, _t, sha, new_sha in pairs if sha != new_sha]
    stream = jn.blobs(root, [sha for pair in changed for sha in pair])  # before, after, before, after ...
    for before in stream:
        pins.add_bytes(before, next(stream))
    pins.clean()
    return pins, unmapped, new


def digest_index(root: Path, unique: list, pins: Pins) -> tuple[dict, dict]:
    """digest (sha256 and blob id, both cases) -> the blob of the renamed tree it names. A base digest of renamed
    bytes names the renamed blob, unless the same digest still names a blob of the tree (AMBIGUOUS: kept)."""
    sha256 = {}
    for position, data in enumerate(jn.blobs(root, unique)):
        sha256[unique[position]] = hashlib.sha256(data).hexdigest()
    index = {}
    for blob, sha in sha256.items():
        for value in (sha, blob, sha.upper(), blob.upper()):
            index[value] = blob
    for old, new in pins.map.items():
        target = index.get(new) if pins.kind[old] != "PATH" else None
        if target is not None:
            if old in index and index[old] != target:
                pins.ambiguous.add(old)
            else:
                index[old] = target
    return index, sha256


def pin_graph(root: Path, unique: list, index: dict, sha256: dict, pins: Pins) -> tuple[dict, set]:
    """blob -> {token: pinned blob} for every text blob that pins another blob, and the seeds: blobs with a stale
    token (the token is not the pinned blob's current value) or a moved path."""
    refs, seeds = {}, set()
    for position, data in enumerate(jn.blobs(root, unique)):
        if b"\0" in data:
            continue
        blob, text = unique[position], jn.decoded(data)
        found = {}
        if HEX_QUICK.search(text):
            for token in HEX.findall(text):
                target = index.get(token)
                if target is not None and token not in pins.ambiguous:
                    found[token] = target
        if found:
            refs[blob] = found
            if any(token.lower() not in (sha256[target], target) for token, target in found.items()):
                seeds.add(blob)
        if pins.roots and pins.roots.search(text) and carried(text, pins) != text:
            seeds.add(blob)
    return refs, seeds


def must_change(refs: dict, seeds: set) -> tuple[set, dict]:
    """The seeds and every blob that pins, directly or through others, a blob that may change."""
    dependents = defaultdict(set)
    for blob, found in refs.items():
        for target in found.values():
            if target != blob:
                dependents[target].add(blob)
    nodes, frontier = set(seeds), list(seeds)
    while frontier:
        for dependent in dependents[frontier.pop()]:
            if dependent not in nodes:
                nodes.add(dependent)
                frontier.append(dependent)
    return nodes, dependents


def replacement(token: str, target: str, final: dict, sha256: dict) -> str:
    sha, blob = final.get(target, (sha256[target], target))
    value = sha if len(token) == 64 else blob
    return value.upper() if token != token.lower() else value


def resolve(root: Path, refs: dict, nodes: set, dependents: dict, sha256: dict, pins: Pins) -> dict:
    """Reseal the nodes in dependency order (Kahn): a blob is read once, after every node it pins is final. When
    only cycles remain, their members are resealed with the values known so far and reported."""
    pending = {b: len({t for t in refs.get(b, {}).values() if t in nodes and t != b}) for b in nodes}
    final, new_bytes, carried_values, cycles, layers = {}, {}, {}, set(), 0
    ready = sorted(b for b in nodes if pending[b] == 0)
    while len(final) < len(nodes):
        if not ready:
            ready = sorted(b for b in nodes if b not in final)
            cycles.update(ready)
        layers += 1
        batch, ready = ready, []
        for position, data in enumerate(jn.blobs(root, batch)):  # drained, so cat-file is reaped
            blob = batch[position]
            tokens = {t: replacement(t, target, final, sha256) for t, target in refs.get(blob, {}).items()}
            text = jn.decoded(data)
            new = carried(text, pins, tokens)
            if new != text:
                new_bytes[blob] = jn.encoded(new)
                final[blob] = digests(new_bytes[blob])
                carried_values.update({t: v for t, v in tokens.items() if t != v and t in text})
            else:
                final[blob] = (sha256[blob], blob)
            for dependent in dependents.get(blob, ()):
                if dependent in pending and dependent not in final:
                    pending[dependent] -= 1
                    if pending[dependent] == 0:
                        ready.append(dependent)
    return {"new_bytes": new_bytes, "carried": carried_values, "cycles": cycles, "layers": layers}


def reseal(root: Path, base: str, rev: str) -> dict:
    root = Path(root)
    pins, unmapped, entries = rename_pins(root, base, rev)
    paths_of = defaultdict(list)
    for path, blob in entries.items():
        if not jn.is_self(path):
            paths_of[blob].append(path)
    unique = sorted(paths_of)
    index, sha256 = digest_index(root, unique, pins)
    refs, seeds = pin_graph(root, unique, index, sha256, pins)
    nodes, dependents = must_change(refs, seeds)
    done = resolve(root, refs, nodes, dependents, sha256, pins)
    files = {path: data for blob, data in done["new_bytes"].items() for path in paths_of[blob]}
    cycle_paths = sorted(path for blob in done["cycles"] for path in paths_of[blob])
    return {"files": files, "pins": pins, "carried": done["carried"], "cycles": cycle_paths, "unmapped": unmapped,
            "layers": done["layers"], "nodes": len(nodes)}


def ledger(result: dict) -> bytes:
    pins = result["pins"]
    rows = ["kind\tbefore\tafter"]
    rows += sorted(f"PATH\t{o}\t{n}" for o, n in pins.map.items() if pins.kind[o] == "PATH")
    rows += sorted(("SHA256" if len(o) == 64 else "BLOB") + f"\t{o}\t{n}" for o, n in result["carried"].items())
    rows += sorted(f"AMBIGUOUS\t{o}\t-" for o in pins.ambiguous)
    rows += [f"CYCLE\t{path}\t-" for path in result["cycles"]]
    return jn.encoded("\n".join(rows) + "\n")


def commit(root: Path, rev: str, ref: str, message: str, files: dict) -> str:
    modes = {p: m for p, m, _s in jn.tree_entries(root, rev)}
    proc = subprocess.Popen(["git", "-C", str(root), "fast-import", "--quiet", "--done"], stdin=subprocess.PIPE)
    out = proc.stdin
    idents = (jn.git(root, "var", "GIT_AUTHOR_IDENT"), jn.git(root, "var", "GIT_COMMITTER_IDENT"))
    out.write(jn.encoded("commit %s\nauthor %s\ncommitter %s\n" % (ref, *idents)))
    out.write(b"data %d\n" % len(jn.encoded(message)) + jn.encoded(message) + b"\nfrom "
              + jn.git(root, "rev-parse", rev + "^{commit}").encode() + b"\n")
    for path, data in sorted(files.items()):
        out.write(("M %s inline " % modes.get(path, "100644")).encode() + jn.quoted(path) + b"\n")
        out.write(b"data %d\n" % len(data) + data + b"\n")
    out.write(b"done\n")
    out.close()
    if proc.wait():
        raise SystemExit("REFUSED: git fast-import failed")
    return jn.git(root, "rev-parse", ref)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".")
    parser.add_argument("--base", required=True, help="the tree before the rename")
    parser.add_argument("--tree", required=True, help="the renamed tree")
    parser.add_argument("--ref", help="write the resealed commit to this ref")
    parser.add_argument("--message", default="JDK naming: reseal pins after the rename (recipe JDK_NAMING_RESEAL)")
    args = parser.parse_args(argv)
    root = Path(args.root)
    result = reseal(root, args.base, args.tree)
    paths = sum(1 for kind in result["pins"].kind.values() if kind == "PATH")
    print("JDK_NAMING_RESEAL files=%d nodes=%d layers=%d carried=%d paths=%d ambiguous=%d cycles=%d unmapped=%d" % (
        len(result["files"]), result["nodes"], result["layers"], len(result["carried"]), paths,
        len(result["pins"].ambiguous), len(result["cycles"]), len(result["unmapped"])))
    if args.ref:
        files = dict(result["files"])
        files[LEDGER] = ledger(result)
        print("COMMIT %s %s" % (args.ref, commit(root, args.tree, args.ref, args.message, files)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
