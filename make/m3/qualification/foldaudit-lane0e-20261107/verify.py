# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""CPU re-check of the lane-0e fold-audit decisions (record-only; nothing is edited).

Every decision in DECISION.tsv rests on a fact about the owner revision that this script re-derives with git
read-only commands and prints as one TSV row per check: check, state (PASS/FAIL), detail. Exit status 1 when any
check fails, so a drifted master is refused instead of silently re-cited.

    python -I verify.py --root <clone> [--rev origin/master]
"""
from __future__ import annotations

import argparse
import subprocess
import sys

# Cited merges whose tree equals their first parent's tree: the PR heads they bind contributed no content.
ANCESTRY_ONLY_MERGES = (
    ("0890555a47c2dbf5f109eaaba99b006d81baeb6f", "#1060/#1080/#1081"),
    ("8061d9bd8527ca6afaf1b320b3d9547456a70ad0", "heads 481-560 (#513, #546, #564)"),
    ("ddf4bb64dbac79648dda21150f3a088070d8a77c", "heads 561-640 (#568)"),
    ("1ef8401d4e6aac519291a0b103b13e97fc407451", "heads 641-720 (#626, #627, #650)"),
)
# Paths that must be absent at the owner revision for the HOLD rows to stay live.
ABSENT_PATHS = (
    "src/jdk.m3.logging",
    "m3/runtime-integration/regex-rope-affix-effects",
    "m3/runtime-integration/regex-mandatory-affixes",
    "m3/runtime-integration/regex-plan-amalgamation",
    "src/java.base/share/classes/java/lang/MIndexString.java",
)
# (path, blob) pairs the PRESENT_EVOLVED rows cite.
PINNED_BLOBS = (
    ("src/jdk.m3.graphics/share/classes/jdk/m3/graphics/impl/M3Cleanable.java",
     "51523cfd1caa2f5690d55e88abc2552ee80fad85"),
)
# (path, token, expected present) for the member-level facts.
TOKENS = (
    ("src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java", "prepareBatch", False),
    ("src/java.base/share/classes/jdk/internal/mindex/M3RegexDpPlan.java", "BatchBackend", False),
    ("src/java.base/share/classes/java/lang/M3StringRegexDp.java", "rejectsByPrecompute", False),
    ("src/java.base/share/classes/java/lang/M3StringRegexDp.java", "canonicalize", False),
    ("src/hotspot/share/classfile/javaClasses.inline.hpp", "java_lang_MIndexString", False),
    ("src/hotspot/share/classfile/javaClasses.inline.hpp", "java_lang_M3String::char_at", True),
    ("src/java.base/share/classes/java/lang/String.java", "private volatile M3String m3;", True),
    ("m3/runtime-integration/export-m3-lexicon.py", "def load_family_sidecars", False),
    ("m3/runtime-integration/tests/test_m3_export.py", "def run_export", True),
    ("m3/runtime-integration/tests/test_m3_export.py", "def test_typed_family_sidecars_round_trip_and_reject_partial", False),
    ("m3/runtime-integration/tests/test_m3_export.py", "def test_composite_owner_shapes_and_overloaded_field_types_are_admitted", False),
    ("m3/runtime-integration/tests/test_m3_export.py", "def test_directed_related_lexeme_families_round_trip_and_require_input", False),
    ("make/conf/module-loader-map.conf", "jdk.m3.logging", False),
)


def git(root: str, *args: str) -> str:
    proc = subprocess.run(["git", "-C", root, *args], capture_output=True, text=True, encoding="utf-8",
                          errors="replace")
    return proc.stdout.strip() if proc.returncode == 0 else ""


def git_ok(root: str, *args: str) -> bool:
    return subprocess.run(["git", "-C", root, *args], capture_output=True).returncode == 0


def row(check: str, ok: bool, detail: str) -> bool:
    print(f"{check}\t{'PASS' if ok else 'FAIL'}\t{detail}")
    return ok


def check_ancestry_only(root: str, rev: str) -> list[bool]:
    out = []
    for sha, label in ANCESTRY_ONLY_MERGES:
        reachable = git_ok(root, "merge-base", "--is-ancestor", sha, rev)
        tree = git(root, "rev-parse", sha + "^{tree}")
        parent_tree = git(root, "rev-parse", sha + "^1^{tree}")
        out.append(row(f"ancestry_only:{sha[:12]}", bool(tree) and tree == parent_tree and reachable,
                       f"{label}; tree={tree[:12]} parent1_tree={parent_tree[:12]}"))
    return out


def check_absent_paths(root: str, rev: str) -> list[bool]:
    return [row(f"absent:{path}", git(root, "ls-tree", "--name-only", rev, "--", path) == "", f"ls-tree {rev}")
            for path in ABSENT_PATHS]


def check_blobs(root: str, rev: str) -> list[bool]:
    return [row(f"blob:{path}", git(root, "rev-parse", f"{rev}:{path}") == blob, f"expected {blob[:12]}")
            for path, blob in PINNED_BLOBS]


def check_tokens(root: str, rev: str) -> list[bool]:
    out = []
    for path, token, expected in TOKENS:
        text = git(root, "cat-file", "-p", f"{rev}:{path}")
        present = token in text
        out.append(row(f"token:{path}:{token}", present == expected and bool(text),
                       f"expected_present={expected} found={present} bytes={len(text)}"))
    return out


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=".")
    parser.add_argument("--rev", default="origin/master")
    args = parser.parse_args(argv)
    print(f"owner_rev\tINFO\t{args.rev}={git(args.root, 'rev-parse', args.rev)}")
    results = (check_ancestry_only(args.root, args.rev) + check_absent_paths(args.root, args.rev)
               + check_blobs(args.root, args.rev) + check_tokens(args.root, args.rev))
    print(f"summary\t{'PASS' if all(results) else 'FAIL'}\t{sum(results)}/{len(results)} checks passed")
    return 0 if all(results) else 1


if __name__ == "__main__":
    sys.exit(main())
