#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe LLM_RETAINED_PORT v1 (m3/llm): merge-loss restore of the Chrome LLM and summarization module of PR #2108.

PR #2108 (head 58bc0d2d297e, the second parent of the retain merge a92a5765d54a) added m3/llm as the module llm of
the com.m3.* namespace. A later fold of master dropped the whole module (S:/m3-scratch/eb-conv/folddrop/retained/
m3__llm.tsv: 44 production files, merge_kind RETAIN). This recipe writes every file of that module back from its git
blob at 58bc0d2d297e and relocates the exported package to jdk.m3.llm (the NAMING_RULES.tsv rules PKG_COM_M3 and
PKG_PATH_COM_M3 of m3/tooling/jdk-naming, applied to the same text scope as jdk_naming.py: .java, pom.xml, .mjs;
README.md is relocated too so the module's own documentation names the landed package), then applies the recorded
edits (EDITS below, every anchor exactly once: the one CONST_KEBAB rename jdk_naming.py plan still reported after the
package relocation, so that the naming tool is a fixed point on the written tree) and registers the module in
m3/pom.xml after nlp.

Hash pinned: RESTORE.tsv names, for every target, the donor blob (git object id at 58bc0d2d297e), the donor sha256 and
the target sha256. apply reads each blob with git cat-file (or --donor-dir, an export of the donor tree), refuses
(exit 2, before any write) when a donor blob's bytes do not hash to the recorded donor sha256, when a produced target
does not hash to the recorded target sha256, or when an existing target holds other bytes than the postimage (drift).
A target already equal to the postimage is a no-op, so a second run reports changed=0 (fixed point). --check writes
nothing and exits 1 when a run would change a file. --seal (author mode) recomputes the target hashes and rewrites
RESTORE.tsv and EDITS.tsv from the rules; it is how this recipe was produced and is not part of a verify.

usage: python -I apply.py [--root DIR] [--donor-dir DIR] [--check] [--seal]
"""
from __future__ import annotations

import argparse
import hashlib
import re
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v2"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "c7be9541dbad424dd5b9598121e830a76e10f6e6"
DONOR_COMMIT = "58bc0d2d297e"
RETAIN_MERGE = "a92a5765d54a"
DONOR_PR = "2108"
RESTORE_NAME = "RESTORE.tsv"
EDITS_NAME = "EDITS.tsv"
RESTORE_COLUMNS = ("target_path", "donor_path", "donor_blob", "donor_sha256", "target_sha256", "decision", "rule")
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "donor_blob", "rule")
LEAF = "retained-port-20261107"
ROOT_UP = 4  # m3/llm/recipe/<leaf>/apply.py -> repository root

MODULE_DIR = "m3/llm/"
# the donor package path is spelled in two parts so that jdk_naming.py (PKG_PATH_COM_M3) leaves this recipe alone
DONOR_PACKAGE_PATH = "com/m3/" + "llm/"
DONOR_PREFIX_MAIN = "m3/llm/src/main/java/" + DONOR_PACKAGE_PATH
DONOR_PREFIX_TEST = "m3/llm/src/test/java/" + DONOR_PACKAGE_PATH
TARGET_PREFIX_MAIN = "m3/llm/src/main/java/jdk/m3/llm/"
TARGET_PREFIX_TEST = "m3/llm/src/test/java/jdk/m3/llm/"
RELOCATE_SUFFIXES = (".java", ".mjs", ".md")
RELOCATE_NAMES = ("pom.xml",)
# NAMING_RULES.tsv PKG_COM_M3 / PKG_PATH_COM_M3 (m3/tooling/jdk-naming): exported M3 packages take the JDK module prefix;
# a Maven groupId span (<groupId>com.m3</groupId>) is not followed by a lowercase letter and stays.
PKG_RULE = (re.compile(r"(?:(?<![A-Za-z0-9])|(?<=\\[tnr]))com\.m3\.(?=[a-z])"), "jdk.m3.")
PATH_RULE = (re.compile(r"(?:(?<![A-Za-z0-9])|(?<=\\[tnr]))com/m3/(?=[a-z])"), "jdk/m3/")

# the m3/pom.xml registration: preimage blob at BASE_COMMIT, one anchored edit
REACTOR_POM = "m3/pom.xml"
REACTOR_POM_BLOB = "eadf233183d4134ba66c5ba30272cf474d5cec4a"
REACTOR_EDIT = ("    <module>nlp</module>\n", "    <module>nlp</module>\n    <module>llm</module>\n")

# ADAPT edits per target path: (old, new) applied after relocation, each anchor exactly once. Empty when every file
# compiles as restored (RESTORE_EXACT); DECISION.tsv names the decision per file.
LEGACY_SCHEMA_PREFIX = "SYNEXIA" + "-"  # two parts: the CONST_KEBAB rule must not rewrite this recipe
EDITS: dict[str, list[tuple[str, str]]] = {
    TARGET_PREFIX_MAIN + "chrome/ChromeMlTaskService.java": [
        ('        return Map.of("schema", "' + LEGACY_SCHEMA_PREFIX + 'CHROMEML-CAPABILITIES/1", "tasks", ',
         '        return Map.of("schema", "M3-CHROMEML-CAPABILITIES/1", "tasks", '),  # v2: no anchor comment, so the written
         # bytes equal master's retained variant retained-variants/pr2108/m3/llm (coordinator decision 2026-11-07)
    ],
}

# files of the donor module that this recipe does not write (DECISION.tsv HOLD rows)
HELD: tuple[str, ...] = ()


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def read_tsv(path: Path, columns: tuple[str, ...]) -> list[dict[str, str]]:
    rows = []
    with path.open(encoding="utf-8", newline="") as handle:
        header = None
        for line in handle:
            line = line.rstrip("\r\n")
            if not line or line.startswith("#"):
                continue
            if header is None:
                header = tuple(line.split("\t"))
                if header != columns:
                    raise Refused(f"{path.name}: columns {header} != {columns}")
                continue
            rows.append(dict(zip(columns, line.split("\t"))))
    return rows


def write_tsv(path: Path, columns: tuple[str, ...], rows: list[dict[str, str]], comment: str) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as handle:
        handle.write("# SPDX-License-Identifier: Apache-2.0\n# " + comment + "\n")
        handle.write("\t".join(columns) + "\n")
        for row in rows:
            handle.write("\t".join(row[c] for c in columns) + "\n")


def donor_bytes(root: Path, donor_dir: Path | None, donor_path: str, blob: str) -> bytes:
    if donor_dir is not None:
        return (donor_dir / donor_path).read_bytes()
    proc = subprocess.run(["git", "cat-file", "blob", blob], cwd=root, capture_output=True)
    if proc.returncode != 0:
        raise Refused(f"git cat-file blob {blob} ({donor_path}): {proc.stderr.decode('utf-8', 'replace').strip()}")
    return proc.stdout


def target_path_of(donor_path: str) -> str:
    if donor_path.startswith(DONOR_PREFIX_MAIN):
        return TARGET_PREFIX_MAIN + donor_path[len(DONOR_PREFIX_MAIN):]
    if donor_path.startswith(DONOR_PREFIX_TEST):
        return TARGET_PREFIX_TEST + donor_path[len(DONOR_PREFIX_TEST):]
    return donor_path


def relocates(target_path: str) -> bool:
    name = target_path.rsplit("/", 1)[-1]
    return name in RELOCATE_NAMES or name.endswith(RELOCATE_SUFFIXES)


def relocate(text: str) -> str:
    text = PKG_RULE[0].sub(PKG_RULE[1], text)
    return PATH_RULE[0].sub(PATH_RULE[1], text)


def apply_edits(target_path: str, text: str) -> str:
    for old, new in EDITS.get(target_path, ()):
        if text.count(old) != 1:
            raise Refused(f"{target_path}: anchor occurs {text.count(old)} times, expected once: {old[:60]!r}")
        text = text.replace(old, new, 1)
    return text


def produce(target_path: str, raw: bytes) -> bytes:
    if not relocates(target_path) and target_path not in EDITS:
        return raw
    text = raw.decode("utf-8")
    if relocates(target_path):
        text = relocate(text)
    text = apply_edits(target_path, text)
    return text.encode("utf-8")


def reactor_postimage(root: Path) -> tuple[bytes, bytes]:
    # the preimage is always the base blob (git cat-file), never the checkout: a sealed postimage computed from an
    # already registered m3/pom.xml would register the module twice (RECIPE_MEMORY REACTOR_PREIMAGE_FROM_BLOB)
    pre = donor_bytes(root, None, REACTOR_POM, REACTOR_POM_BLOB)
    text = pre.decode("utf-8")
    if text.count(REACTOR_EDIT[0]) != 1:
        raise Refused(f"{REACTOR_POM}: anchor occurs {text.count(REACTOR_EDIT[0])} times, expected once")
    return pre, text.replace(REACTOR_EDIT[0], REACTOR_EDIT[1], 1).encode("utf-8")


def plan(root: Path, donor_dir: Path | None, seal: bool) -> tuple[list[dict[str, str]], list[dict[str, str]], list[tuple[Path, bytes]]]:
    restore_path = RECIPE_DIR / RESTORE_NAME
    rows = read_tsv(restore_path, RESTORE_COLUMNS)
    edits: list[dict[str, str]] = []
    writes: list[tuple[Path, bytes]] = []
    seen = set()
    for row in rows:
        donor_path, blob = row["donor_path"], row["donor_blob"]
        if donor_path in HELD:
            raise Refused(f"{donor_path}: HELD and listed in {RESTORE_NAME}")
        target_path = row["target_path"]
        if target_path != target_path_of(donor_path):
            raise Refused(f"{donor_path}: target {target_path} != {target_path_of(donor_path)}")
        if target_path in seen:
            raise Refused(f"{target_path}: listed twice")
        seen.add(target_path)
        raw = donor_bytes(root, donor_dir, donor_path, blob)
        if sha256(raw) != row["donor_sha256"]:
            raise Refused(f"{donor_path}: donor blob {blob} sha256 {sha256(raw)} != {row['donor_sha256']}")
        out = produce(target_path, raw)
        if seal:
            row["target_sha256"] = sha256(out)
        elif sha256(out) != row["target_sha256"]:
            raise Refused(f"{target_path}: produced sha256 {sha256(out)} != recorded {row['target_sha256']}")
        rule = row["rule"]
        edits.append({"file": target_path, "before_sha256": row["donor_sha256"], "after_sha256": sha256(out),
                      "leaf": LEAF, "donor_blob": f"{DONOR_COMMIT} {donor_path} blob {blob}", "rule": rule})
        target = root / target_path
        if target.exists():
            current = target.read_bytes()
            if current != out:
                raise Refused(f"{target_path}: exists with sha256 {sha256(current)}, neither absent nor the postimage"
                              f" {sha256(out)} (drift)")
        else:
            writes.append((target, out))
    pre, post = reactor_postimage(root)
    current = (root / REACTOR_POM).read_bytes()
    if current == pre:
        writes.append((root / REACTOR_POM, post))
    elif current != post:
        raise Refused(f"{REACTOR_POM}: sha256 {sha256(current)} is neither the base blob {REACTOR_POM_BLOB} nor the"
                      f" postimage {sha256(post)} (drift)")
    edits.append({"file": REACTOR_POM, "before_sha256": sha256(pre), "after_sha256": sha256(post), "leaf": LEAF,
                  "donor_blob": f"{BASE_COMMIT[:12]} {REACTOR_POM} blob {REACTOR_POM_BLOB}",
                  "rule": "REGISTER <module>llm</module> after nlp"})
    return rows, edits, writes


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[ROOT_UP - 1]))
    parser.add_argument("--donor-dir", default=None, help="export of the donor tree instead of git cat-file")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--seal", action="store_true")
    args = parser.parse_args(argv)
    root = Path(args.root).resolve()
    donor_dir = Path(args.donor_dir).resolve() if args.donor_dir else None
    try:
        rows, edits, writes = plan(root, donor_dir, args.seal)
    except Refused as refused:
        print(f"{RECIPE} {VERSION} REFUSED: {refused}", file=sys.stderr)
        return 2
    if args.seal:
        write_tsv(RECIPE_DIR / RESTORE_NAME, RESTORE_COLUMNS, rows,
                  f"m3/llm restore of PR #{DONOR_PR} head {DONOR_COMMIT} (retain merge {RETAIN_MERGE}) onto"
                  f" {BASE_COMMIT[:12]}: donor blob and sha256, target sha256 after relocation and ADAPT edits")
        write_tsv(RECIPE_DIR / EDITS_NAME, EDITS_COLUMNS, edits,
                  f"before = donor blob sha256 at {DONOR_COMMIT} (m3/pom.xml: base {BASE_COMMIT[:12]}),"
                  f" after = written bytes; {RECIPE} {VERSION}")
    edits_path = RECIPE_DIR / EDITS_NAME
    recorded = read_tsv(edits_path, EDITS_COLUMNS) if edits_path.exists() else []
    edits_state = "UNCHANGED" if recorded == edits else "DRIFT"
    if edits_state == "DRIFT" and not args.seal:
        print(f"{RECIPE} {VERSION} REFUSED: {EDITS_NAME} does not match the rules (run --seal as the author)",
              file=sys.stderr)
        return 2
    if args.check:
        print(f"{RECIPE} {VERSION} check files={len(rows)} would_change={len(writes)} edits={edits_state}")
        return 1 if writes else 0
    for target, out in writes:
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(out)
    print(f"{RECIPE} {VERSION} files={len(rows)} changed={len(writes)} edits={edits_state}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
