#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""REFS_LEDGER v1: classify every cross-repository finding of check_standalone.py into one ledger.

Inputs are two check_standalone.py TSV scans (columns kind, path, line, text), taken before and after a change,
and the ordered rule table (m3/tooling/standalone/*_RULES.tsv: order, leaf, kind, path, text, class, decision, action,
evidence; kind/path/text are Python regular expressions searched in the finding, the first matching rule wins).
The ledger has one row per dependency finding (D_* kinds) of the "before" scan:
path, kind, decision, action, evidence, line, class, state, text. A finding is RESOLVED when the "after" scan holds
fewer findings with the same kind, path and text (line numbers move when files are edited), otherwise REMAINS.
COUNT rows follow: per dependency kind, the full-scope and leaf-scope (rules with leaf=1) counts before and after.
Rows are sorted, so the same inputs always give the same bytes (re-runnable to a fixed point).
CLI: python -I refs_ledger.py --before B.tsv --after A.tsv --rules RULES.tsv --out LEDGER.tsv [--check]
--check writes nothing and exits 1 when the ledger on disk differs from the rebuilt one.
"""
from __future__ import annotations

import argparse
import collections
import csv
import re
import sys
from pathlib import Path

DEPENDENCY_PREFIX = "D_"
LEAF_FLAG = "1"
TAB = "\t"
NEWLINE = "\n"
STATE_RESOLVED = "RESOLVED"
STATE_REMAINS = "REMAINS"
COUNT_DECISION = "COUNT"
FULL_SCOPE = "(scope:full)"
LEAF_SCOPE = "(scope:leaf)"
LEDGER_HEADER = ("path", "kind", "decision", "action", "evidence", "line", "class", "state", "text")
RULE_FIELDS = ("order", "leaf", "kind", "path", "text", "class", "decision", "action", "evidence")
EXIT_OK = 0
EXIT_DIFFERS = 1


def read_tsv(path: Path) -> list[dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        return list(csv.DictReader(handle, delimiter=TAB, quoting=csv.QUOTE_NONE))


def dependency_findings(path: Path) -> list[dict[str, str]]:
    return [row for row in read_tsv(path) if row["kind"].startswith(DEPENDENCY_PREFIX)]


def load_rules(path: Path) -> list[dict]:
    rules = []
    for row in sorted(read_tsv(path), key=lambda r: int(r["order"])):
        if tuple(row) != RULE_FIELDS:
            raise ValueError(f"rule header drift: {tuple(row)}")
        rules.append({**row, "re": {k: re.compile(row[k]) for k in ("kind", "path", "text")}})
    return rules


def classify(finding: dict[str, str], rules: list[dict]) -> dict:
    for rule in rules:
        if all(rule["re"][k].search(finding[k]) for k in ("kind", "path", "text")):
            return rule
    raise ValueError(f"no rule for {finding['kind']} {finding['path']}:{finding['line']}")


def key(finding: dict[str, str]) -> tuple[str, str, str]:
    return finding["kind"], finding["path"], finding["text"]


def build(before: list[dict[str, str]], after: list[dict[str, str]], rules: list[dict]) -> list[tuple[str, ...]]:
    remaining = collections.Counter(key(f) for f in after)
    ordered = sorted(before, key=lambda f: (f["path"], int(f["line"] or 0), f["kind"], f["text"]))
    rows, counts = [], collections.Counter()
    for finding in ordered:
        rule = classify(finding, rules)
        state = STATE_REMAINS if remaining[key(finding)] > 0 else STATE_RESOLVED
        remaining[key(finding)] -= 1
        rows.append((finding["path"], finding["kind"], rule["decision"], rule["action"], rule["evidence"],
                     finding["line"], rule["class"], state, finding["text"]))
        scopes = (FULL_SCOPE, LEAF_SCOPE) if rule["leaf"] == LEAF_FLAG else (FULL_SCOPE,)
        for scope in scopes:
            counts[(scope, finding["kind"], "before")] += 1
            counts[(scope, finding["kind"], "after")] += state == STATE_REMAINS
    kinds = sorted({f["kind"] for f in before})
    for scope in (FULL_SCOPE, LEAF_SCOPE):
        for kind in kinds:
            rows.append((scope, kind, COUNT_DECISION,
                         f"before={counts[(scope, kind, 'before')]} after={counts[(scope, kind, 'after')]}",
                         "check_standalone.py scans (before, after)", "0", "COUNT", "", ""))
    return rows


def render(rows: list[tuple[str, ...]]) -> str:
    lines = [TAB.join(LEDGER_HEADER)] + [TAB.join(c.replace(TAB, " ") for c in row) for row in rows]
    return NEWLINE.join(lines) + NEWLINE


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    for name in ("--before", "--after", "--rules", "--out"):
        parser.add_argument(name, required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    text = render(build(dependency_findings(args.before), dependency_findings(args.after), load_rules(args.rules)))
    if args.check:
        same = args.out.is_file() and args.out.read_text(encoding="utf-8") == text
        print(f"refs_ledger.check\t{'PASS' if same else 'DIFFERS'}\t{args.out}")
        return EXIT_OK if same else EXIT_DIFFERS
    args.out.write_text(text, encoding="utf-8", newline=NEWLINE)
    print(f"refs_ledger.write\t{args.out}\trows={text.count(NEWLINE) - 1}")
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
