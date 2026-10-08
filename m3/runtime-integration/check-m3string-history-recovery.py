#!/usr/bin/env python3
"""Fail-closed, current-receiver MIndexString history parity gate."""
import csv
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
LEDGER = ROOT / "m3/docs/m3string-history-recovery.tsv"
EXPECTED = {
    "mindex.string.char-array-ingress",
    "mindex.string.char-array-ingress-recipe",
    "mindex.string.composition-fingerprint",
    "mindex.string.composition-fingerprint-maturation",
    "mindex.string.composite.range-retention",
    "mindex.string.mixed-owner-composition",
    "mindex.string.native-joined-handle",
    "mindex.string.os-shared-payload",
    "mindex.string.literal-regex-replacement",
    "mindex.string.regex-split-indexed",
    "mindex.string.literal-regex-matches",
    "mindex.string.compiler-concat-reference-dag",
    "mindex.string.codepoint-range-precompute",
    "mindex.string.codepoint-offset-precompute",
    "mindex.string.masked-view",
    "mindex.string.palindrome-facts",
    "mindex.string.suffix-decision",
    "mindex.string.sha256-boundary-aware",
    "mindex.string.sha256-range",
    "mindex.string.jdk-storage-prototype",
}
DISPOSITIONS = {
    "ALREADY_ABSORBED", "PARTIAL_ALREADY_ABSORBED", "PORT_SAFE_SUBSET",
    "ADAPT_INTERNAL_VIEW", "PRESERVE_DONOR_NO_STRING_CONSUMER",
    "EXTERNAL_M3_PLANE",
}
STATUSES = {
    "CURRENT_TREE_OBSERVED", "TESTED_BY_NEW_JTREG", "TOKEN_SIMHASH_EXTERNAL",
    "NO_JDK_STRING_CONSUMER", "PENDING_RUNTIME", "PENDING_CONCRETE_CONSUMER",
    "NO_PUBLIC_PHYSICAL_SHARING_API",
}

def need(condition, message):
    if not condition:
        raise SystemExit("M3_STRING_HISTORY_PARITY_FAIL|" + message)

def read(path):
    return (ROOT / path).read_text(encoding="utf-8")

with LEDGER.open(newline="", encoding="utf-8") as stream:
    rows = list(csv.reader(stream, delimiter="\t"))
need(bool(rows), "empty ledger")
need(rows[0] == ["capability_id", "donor_commit", "target_owner",
                 "disposition", "receiver_status", "proof_anchor", "semantic_rule"],
     "schema changed")
items = {}
for index, row in enumerate(rows[1:], 2):
    need(len(row) == 7, "bad column count on row " + str(index))
    identity, sha, owner, disposition, status, proof, rule = row
    need(identity not in items, "duplicate " + identity)
    need(bool(re.fullmatch(r"[0-9a-f]{40}", sha)), "unsealed donor " + identity)
    need(disposition in DISPOSITIONS, "invalid disposition " + identity)
    need(status in STATUSES, "invalid status " + identity)
    need(all((owner, proof, rule)), "missing evidence " + identity)
    items[identity] = row
need(set(items) == EXPECTED, "qualified history coverage drift")
for identity in ("mindex.string.regex-split-indexed",
                 "mindex.string.literal-regex-matches",
                 "mindex.string.compiler-concat-reference-dag",
                 "mindex.string.codepoint-range-precompute",
                 "mindex.string.codepoint-offset-precompute"):
    need(items[identity][4] == "PENDING_RUNTIME",
         "unqualified runtime promotion " + identity)

string = read("src/java.base/share/classes/java/lang/String.java")
m3 = read("src/java.base/share/classes/java/lang/M3String.java")
pool = read("src/java.base/share/classes/java/lang/M3StringPool.java")
tuple_ = read("src/java.base/share/classes/java/lang/M3StringTuple.java")
workflow = read(".github/workflows/mindex-string-backing.yml")

for marker, owner in [
    ("String(char[] value, int off, int len, Void sig)", string),
    ("maybeAdmit(value, off, len)", string),
    ("return storage.charShadow();", string),
    ("private volatile M3String m3;", string),
    ("static M3String admit(char[] source, int offset, int length)", m3),
    ("private final M3StringOwner owner;", m3),
    ("private final long value;", m3),
    ("boolean sameCoordinate(M3String other)", m3),
    ("boolean contentEquals(String other)", m3),
    ("static M3String internChars(char[] source, int offset, int length)", pool),
    ("M3StringAtom.localChars(source, offset, length, coder", pool),
    ("static M3String concat(M3String left, M3String right)", pool),
    ("concatBalanced(left, right)", pool),
    ("final M3String left;", tuple_),
    ("final M3String right;", tuple_),
    ("void getChars(int start, int end, char[] destination", tuple_),
    ("M3StringCharArrayHistoryTest.java", workflow),
    ("check-m3string-history-recovery.py", workflow),
]:
    need(marker in owner, "current implementation or proof missing: " + marker)

need(not (ROOT / "src/java.base/share/classes/java/lang/SubM3String.java").exists(),
     "obsolete donor wrapper copied")
need(not (ROOT / "src/java.base/share/classes/java/lang/M3IndexString.java").exists(),
     "competing canonical String owner")
need(items["mindex.string.composition-fingerprint"][4] == "TOKEN_SIMHASH_EXTERNAL",
     "candidate similarity promoted to equality authority")
print("M3_STRING_HISTORY_PARITY_PASS|capabilities=" + str(len(items)))
