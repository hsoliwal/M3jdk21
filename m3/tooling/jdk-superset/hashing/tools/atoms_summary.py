# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atoms_summary.py <ATOMS_SUMMARY.tsv> <variant>=<source label>=<atomize output dir>[,<repeated run dir>...] ...

Stage 1 (atomize) of the hashing JIT leaf, as a table: one row per variant atomized by
`atomize_patternize.py --phase atomize` (the tree-sitter L-Hash atomizer of com.synexia's converge tools), read from
the run's own PART.json, FILES.tsv and ATOMS.tsv.gz, then one row per source file and variant. Columns: java files,
parse errors, leaves, methods, types; worker faults (native faults of the parser binding in a worker interpreter; the
tool re-runs a faulting batch in halves, so every file is still in the table; '+'-joined over repeated runs of the
same input, e.g. 5+0+1); the sha256 of the uncompressed atom table (ATOMS.tsv; repeated runs must give the same
table whatever their faults, else the tool stops) and the tool hashes. A run directory is the --output of the
atomizer (it holds one <donor>/ subdirectory). The table starts with the SPDX lines ('#').
"""
import csv
import gzip
import hashlib
import json
import os
import sys

NL = "\n"
TAB = "\t"
SEP = "="
DIRS = ","
SPDX_LINES = ("# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0",
              "# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others")
HEADER = ("variant", "scope", "source", "java_files", "parse_errors", "leaves", "methods", "types", "worker_faults",
          "ATOMS.tsv_sha256", "tools")
SHORT = 16
ABSENT = "-"


def run_dir(out_dir):
    donors = [d for d in sorted(os.listdir(out_dir)) if os.path.isdir(os.path.join(out_dir, d))]
    if len(donors) != 1:
        raise SystemExit(f"{out_dir}: expected one donor directory, found {donors}")
    return os.path.join(out_dir, donors[0])


def part_of(d):
    with open(os.path.join(d, "PART.json"), encoding="utf-8") as f:
        return json.load(f)


def atoms_sha256(d):
    with gzip.open(os.path.join(d, "ATOMS.tsv.gz"), "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()


def rows_of(variant, source, out_dirs):
    dirs = [run_dir(o) for o in out_dirs]
    part, table = part_of(dirs[0]), atoms_sha256(dirs[0])
    for again in dirs[1:]:
        if atoms_sha256(again) != table:
            raise SystemExit(f"{again}: the repeated run gave another atom table than {dirs[0]}")
    tools = part["tools"]
    tool_note = (f"atomize_patternize.py sha256 {tools['projector_sha256'][:SHORT]}, {tools['lhash_module']} sha256 "
                 f"{tools['lhash_module_sha256'][:SHORT]}, tree_sitter {tools['tree_sitter']}, "
                 f"tree_sitter_language_pack {tools['tree_sitter_language_pack']}, python {tools['python']}")
    faults = "+".join(str(part_of(d)["worker_faults"]) for d in dirs)
    c = part["counts"]
    out = [(variant, "run", source, part["java_files"], part["parse_errors"], c["leaves"], c["methods"], c["types"],
            faults, table, tool_note)]
    with open(os.path.join(dirs[0], "FILES.tsv"), encoding="utf-8", newline="") as f:
        for r in csv.DictReader(f, delimiter=TAB):
            out.append((variant, "file", os.path.basename(r["path"]), 1, r["parse_error"], r["leaves"], r["methods"],
                        r["types"], ABSENT, ABSENT, ABSENT))
    return out


def main():
    out, specs = sys.argv[1], sys.argv[2:]
    if not specs:
        raise SystemExit(__doc__)
    runs, files = [], []
    for spec in specs:
        variant, source, out_dirs = spec.split(SEP, 2)
        rows = rows_of(variant, source, out_dirs.split(DIRS))
        runs.append(rows[0])
        files += rows[1:]
    order = {r[0]: k for k, r in enumerate(runs)}
    files.sort(key=lambda r: (r[2], order[r[0]]))
    with open(out, "w", encoding="utf-8", newline=NL) as w:
        w.write(NL.join(SPDX_LINES) + NL + TAB.join(HEADER) + NL)
        w.write("".join(TAB.join(str(c) for c in r) + NL for r in runs + files))
    for r in runs:
        print(f"{r[0]}: files={r[3]} parse_errors={r[4]} leaves={r[5]} methods={r[6]} types={r[7]} faults={r[8]}")


main()
