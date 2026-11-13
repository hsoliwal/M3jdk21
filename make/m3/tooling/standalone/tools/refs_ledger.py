#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""REFS_LEDGER v2: classify every cross-repository finding of check_standalone.py into one ledger.

Inputs are two check_standalone.py TSV scans (columns kind, path, line, text), taken before and after a change,
and the ordered rule table (m3/tooling/standalone/*_RULES.tsv: order, leaf, kind, path, text, class, decision, action,
evidence; kind/path/text are Python regular expressions searched in the finding, the first matching rule wins).
Both scans are classified with the same rules. The ledger has one row per dependency finding (D_* kinds):
path, kind, decision, action, evidence, line, class, state, text. A "before" finding is RESOLVED when the "after"
scan holds fewer findings with the same kind, path and text (line numbers move when files are edited), otherwise
REMAINS. An "after" finding left over once the "before" findings with its kind, path and text are matched (in line
order) is NEW: a regression, or a finding whose file moved (the old path then reports RESOLVED).
COUNT rows follow: per dependency kind of either scan, the full-scope and leaf-scope (rules with leaf=1) counts of
the "before" scan and of the "after" scan.
Rows are sorted, so the same inputs always give the same bytes (re-runnable to a fixed point).
v1 -> v2 (review of #2236): v1 took the "after" counts from the REMAINS rows, so after-only findings, kinds that
only the "after" scan holds and findings at a moved path were neither counted nor listed.
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
STATE_NEW = "NEW"
BEFORE = "before"
AFTER = "after"
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


def finding_order(finding: dict[str, str]) -> tuple:
    return finding["path"], int(finding["line"] or 0), finding["kind"], finding["text"]


def row_order(row: tuple[str, ...]) -> tuple:
    return row[0], int(row[5] or 0), row[1], row[8]


def ledger_row(finding: dict[str, str], rule: dict, state: str) -> tuple[str, ...]:
    return (finding["path"], finding["kind"], rule["decision"], rule["action"], rule["evidence"],
            finding["line"], rule["class"], state, finding["text"])


def tally(counts: collections.Counter, rule: dict, kind: str, side: str) -> None:
    for scope in (FULL_SCOPE, LEAF_SCOPE) if rule["leaf"] == LEAF_FLAG else (FULL_SCOPE,):
        counts[(scope, kind, side)] += 1


def build(before: list[dict[str, str]], after: list[dict[str, str]], rules: list[dict]) -> list[tuple[str, ...]]:
    rows, counts = [], collections.Counter()
    unmatched_after = collections.Counter(key(f) for f in after)
    for finding in sorted(before, key=finding_order):
        rule = classify(finding, rules)
        state = STATE_REMAINS if unmatched_after[key(finding)] > 0 else STATE_RESOLVED
        unmatched_after[key(finding)] -= 1
        rows.append(ledger_row(finding, rule, state))
        tally(counts, rule, finding["kind"], BEFORE)
    unmatched_before = collections.Counter(key(f) for f in before)
    for finding in sorted(after, key=finding_order):
        rule = classify(finding, rules)
        tally(counts, rule, finding["kind"], AFTER)
        if unmatched_before[key(finding)] > 0:
            unmatched_before[key(finding)] -= 1
        else:
            rows.append(ledger_row(finding, rule, STATE_NEW))
    rows.sort(key=row_order)  # stable: a NEW row follows the before rows it ties with
    kinds = sorted({f["kind"] for f in before} | {f["kind"] for f in after})
    for scope in (FULL_SCOPE, LEAF_SCOPE):
        for kind in kinds:
            rows.append((scope, kind, COUNT_DECISION,
                         f"before={counts[(scope, kind, BEFORE)]} after={counts[(scope, kind, AFTER)]}",
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
