#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Check MERGELOSS_RECORDS v1: the mechanical facts behind DECISION.tsv (merge-loss audit leaves 60-74).

Read-only, git only (works in a sparse or blobless checkout: it reads the HEAD tree, not the worktree). For every
row: the origin commit and the dropping merge resolve, and the dropping merge is an ancestor of HEAD. For every cited
master commit: it is an ancestor of HEAD. For every content claim: a pattern is present in, or absent from, a file
(or every file under a directory) of HEAD. Prints one line per failed probe and a summary; exit 1 on any failure.

usage: python -I verify.py [--root DIR]
"""
from __future__ import annotations

import argparse
import csv
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent
TEXT = "m3/core/src/jdk/m3/text/"

# commits each decision cites as deliberate master evidence (must be ancestors of HEAD)
EVIDENCE_COMMITS = {
    "1032019bd51": "acronym records unadmitted (the lexicon export contract, today m3/lexicon/M3_EXPORT.md)",
    "3b2c672321c": "number family bound to M3NumberSpace without language_tag",
    "47a6ac24890": "generated number export envelope",
    "20866d855e0": "SI-unit precompute decoder (findSiUnitPrecompute)",
    "5761adde29f": "source revision and git blob digests verified",
    "bd4ccdb035d": "dictionary/frequency contract fixture constants",
    "2dd99a11711": "#533 IndexWordFacts/IndexWordSignal with the negative-fact refusal",
    "3128c6a216b": "dictionary and frequency manifest rows bound to receivers",
    "49acb578201": "view-free scalar lookup kept by the #2471 fold (leaf 67 context)",
    "8f88f9d65e2": "view-free scalar admission kept by the #2473 fold (leaf 69 context)",
}

# (leaf, path or directory/, pattern, expected present?)
PROBES = (
    ("60", TEXT, "acronymAt(", False), ("60", TEXT, "numberAt(", False), ("60", TEXT, "siUnitAt(", False),
    ("60", TEXT, "record AcronymIdentity", False), ("70", TEXT, "record NumberIdentity", False),
    ("68", TEXT, "record SiUnitIdentity", False),
    ("62", TEXT, "record AcronymPrecompute", False), ("63", TEXT, "record NumberPrecompute", False),
    ("61", TEXT + "M3NumberSpace.java", "precompute(int value, String languageTag)", False),
    ("61", TEXT + "M3NumberSpace.java", "public String sourceRecordId(int value)", True),
    ("61", "m3/lexicon/m3-number-target-map.tsv", "M3NumberSpace.INSTANCE.sourceRecordId(value)", True),
    ("61", "m3/lexicon/M3_EXPORT.md", "The source itself has no `language_tag` field", True),
    ("62", "m3/lexicon/M3_EXPORT.md", "records remain unadmitted until each source snapshot has an exact revision,", True),
    ("62", "m3/core/src/", "AcronymSidecarCatalog", False),
    ("64", TEXT, "sourceBlobSha", False), ("72", TEXT, "sourceBlobSha", False),
    ("64", "m3/lexicon/m3-word-source-revisions.tsv", "source_id\tsource_revision\tsource_blob_sha", True),
    ("64", "m3/core/test/M3DictionaryFrequencyContractTest.java", "FREQUENCY_BLOB_SHA.equals(", True),
    ("72", "m3/core/test/M3LexiconPrecomputeTest.java", "new SharedLexiconPrecomputeCatalog.WordIdentity(", True),
    ("68", TEXT + "SharedLexiconCatalog.java", "findSiUnitPrecompute(", True),
    ("68", TEXT + "SharedLexiconCatalog.java", "\"dictlang.si-units\".equals(mapping.sourceId())", True),
    ("68", "m3/core/test/M3SiUnitDecoderTest.java", "findSiUnitPrecompute(", True),
    ("73", TEXT, "requireNonNegativeLong", False),
    ("73", TEXT + "M3LexiconPrecompute.java", "\"negative dictionary fact\"", True),
)


def git(root: Path, *args: str) -> subprocess.CompletedProcess:
    return subprocess.run(["git", "-C", str(root), *args], capture_output=True, text=True, encoding="utf-8")


def ancestor(root: Path, commit: str) -> bool:
    return git(root, "merge-base", "--is-ancestor", commit, "HEAD").returncode == 0


def present(root: Path, path: str, pattern: str) -> bool:
    if path.endswith("/"):
        return git(root, "grep", "-q", "-F", "-e", pattern, "HEAD", "--", path).returncode == 0
    shown = git(root, "show", "HEAD:" + path)
    if shown.returncode != 0:
        raise SystemExit("missing file at HEAD: " + path)
    return pattern in shown.stdout


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    root = Path(parser.parse_args(argv).root).resolve()
    failures, checks = [], 0
    with open(RECIPE_DIR / "DECISION.tsv", encoding="utf-8", newline="") as handle:
        rows = list(csv.DictReader(handle, delimiter="\t"))
    for row in rows:
        origin, merge = row["origin"].split()[0], row["dropping_merge"].split()[0]
        for commit in (origin, merge):
            checks += 1
            if git(root, "cat-file", "-e", commit + "^{commit}").returncode != 0:
                failures.append("leaf %s: commit %s does not resolve" % (row["leaf"], commit))
        checks += 1
        if not ancestor(root, merge):
            failures.append("leaf %s: dropping merge %s is not an ancestor of HEAD" % (row["leaf"], merge))
    for commit, why in EVIDENCE_COMMITS.items():
        checks += 1
        if not ancestor(root, commit):
            failures.append("evidence %s (%s) is not an ancestor of HEAD" % (commit, why))
    for leaf, path, pattern, expected in PROBES:
        checks += 1
        if present(root, path, pattern) != expected:
            failures.append("leaf %s: %r %s in %s" % (leaf, pattern, "missing" if expected else "present", path))
    for failure in failures:
        print("FAIL " + failure)
    decided = sorted({row["decision"] for row in rows})
    print("MERGELOSS_RECORDS verify v1 rows=%d decisions=%s checks=%d failures=%d" % (
        len(rows), ",".join(decided), checks, len(failures)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
