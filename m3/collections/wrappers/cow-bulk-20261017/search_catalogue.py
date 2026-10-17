#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Deterministic problem-category search of the read-only donor comparison ledger."""
import argparse
import csv
from pathlib import Path

LEDGER = Path(__file__).resolve().with_name("CATALOGUE.tsv")

def search(query: str) -> list[dict[str, str]]:
    with LEDGER.open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        rows = list(reader)
    tokens = [token.casefold() for token in query.split() if token]
    return sorted(
        (row for row in rows if all(
            token in " ".join(row.values()).casefold() for token in tokens)),
        key=lambda row: (row["problem_category"], row["operation"], row["candidate"]),
    )

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("query", nargs="*", help="terms across category, donor and contract")
    args = parser.parse_args()
    for row in search(" ".join(args.query)):
        print("\t".join((row["problem_category"], row["operation"],
                         row["disposition"], row["candidate"], row["provenance"])))

if __name__ == "__main__":
    main()
