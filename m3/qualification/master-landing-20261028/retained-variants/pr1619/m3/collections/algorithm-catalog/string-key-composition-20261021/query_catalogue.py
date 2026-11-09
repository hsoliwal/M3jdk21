#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Read-only filters over the finite String-key opportunity catalogue."""

import argparse
import csv
import json
import sys
from pathlib import Path

TABLES = {"decisions": "DONOR_DECISIONS.tsv", "owners": "OWNER_INVENTORY.tsv", "families": "SDK_FAMILY_INVENTORY.tsv"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--table", choices=TABLES, default="decisions")
    parser.add_argument("--where", action="append", default=[], metavar="FIELD=VALUE")
    parser.add_argument("--match", action="append", default=[], metavar="TEXT")
    parser.add_argument("--format", choices=("tsv", "json"), default="tsv")
    args = parser.parse_args()
    with (Path(__file__).resolve().parent / TABLES[args.table]).open(encoding="utf-8", newline="") as source:
        reader = csv.DictReader(source, delimiter="\t")
        fields = reader.fieldnames
        if not fields or len(fields) != len(set(fields)):
            parser.error("invalid or duplicate TSV header")
        exact = []
        for expression in args.where:
            key, separator, value = expression.partition("=")
            if not separator or key not in fields:
                parser.error("--where requires an existing field and '='; fields: " + ", ".join(fields))
            exact.append((key, value))
        results = []
        for row in reader:
            if None in row or any(value is None for value in row.values()):
                parser.error("malformed TSV row")
            if all(row[key] == value for key, value in exact) and all(term.casefold() in "\t".join(row.values()).casefold() for term in args.match):
                results.append(row)
    if args.format == "json":
        json.dump(results, sys.stdout, indent=2, ensure_ascii=True)
        sys.stdout.write("\n")
    else:
        writer = csv.DictWriter(sys.stdout, fieldnames=fields, delimiter="\t", lineterminator="\n")
        writer.writeheader()
        writer.writerows(results)


if __name__ == "__main__":
    main()
