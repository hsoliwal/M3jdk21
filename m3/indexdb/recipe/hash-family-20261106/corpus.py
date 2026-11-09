#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Rebuilds the donor crate's L-Hash oracle corpus (gate 2: reproduce the crate figures).

usage: python -I corpus.py --donor-git DIR --out DIR --rev REV [--rev REV ...] [--github OWNER/REPO]

The ORACLE row of RELOCATION_MAP.tsv that ends in LHASH_ORACLE.tsv names the crate's oracle rows (path, sha256, ...);
it is read from the donor git at the first --rev. Every listed file is taken from the first --rev whose blob (as
stored, or with CRLF turned into LF) has the row's sha256; a file whose oracle-time version is older than every
local revision (shallow clone) is looked up through `gh api` on --github (commit list of the path, then contents at
each commit) until the sha256 matches. Writes the files under --out at their paths plus --out/corpus.txt (the oracle
order) and prints one summary line; exit 1 when a row stays unresolved.
"""
import argparse
import base64
import hashlib
import json
import subprocess
import sys
from pathlib import Path

RECIPE_DIR = Path(__file__).resolve().parent


def oracle_path():
    for line in (RECIPE_DIR / "RELOCATION_MAP.tsv").read_text(encoding="utf-8").splitlines():
        cells = line.split("\t")
        if cells[0] == "ORACLE" and cells[1].endswith("LHASH_ORACLE.tsv"):
            return cells[1]
    raise SystemExit("no LHASH_ORACLE.tsv ORACLE row in RELOCATION_MAP.tsv")


def show(git_dir, rev, path):
    done = subprocess.run(["git", "-C", git_dir, "-c", "gc.auto=0", "show", rev + ":" + path], capture_output=True)
    return done.stdout if done.returncode == 0 else None


def matching(data, want):
    for candidate in (data, data.replace(b"\r\n", b"\n")):
        if hashlib.sha256(candidate).hexdigest() == want:
            return candidate
    return None


def github(repo, path, want):
    listing = subprocess.run(["gh", "api", "repos/%s/commits?path=%s&per_page=30" % (repo, path)],
                             capture_output=True, timeout=120)
    for commit in json.loads(listing.stdout or b"[]") if listing.returncode == 0 else []:
        blob = subprocess.run(["gh", "api", "repos/%s/contents/%s?ref=%s" % (repo, path, commit["sha"])],
                              capture_output=True, timeout=120)
        if blob.returncode == 0:
            found = matching(base64.b64decode(json.loads(blob.stdout).get("content", "")), want)
            if found is not None:
                return commit["sha"], found
    return None, None


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--donor-git", required=True)
    parser.add_argument("--out", required=True)
    parser.add_argument("--rev", action="append", required=True)
    parser.add_argument("--github", default="")
    args = parser.parse_args(argv)
    rows = [line.split("\t") for line in show(args.donor_git, args.rev[0], oracle_path()).decode("utf-8").splitlines()[1:]]
    out = Path(args.out)
    counts = {"local": 0, "github": 0, "missing": 0}
    total = 0
    for cells in rows:
        path, want, found = cells[0], cells[1], None
        for rev in args.rev:
            data = show(args.donor_git, rev, path)
            found = matching(data, want) if data is not None else None
            if found is not None:
                counts["local"] += 1
                break
        if found is None and args.github:
            commit, found = github(args.github, path, want)
            if found is not None:
                counts["github"] += 1
                print("GITHUB %s %s" % (commit[:12], path))
        if found is None:
            counts["missing"] += 1
            print("MISSING " + path)
            continue
        target = out / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(found)
        total += len(found)
    (out / "corpus.txt").write_text("".join(cells[0] + "\n" for cells in rows), encoding="utf-8", newline="\n")
    print("CORPUS rows=%d local=%d github=%d missing=%d bytes=%d" % (len(rows), counts["local"], counts["github"],
                                                                    counts["missing"], total))
    return 1 if counts["missing"] else 0


if __name__ == "__main__":
    sys.exit(main())
