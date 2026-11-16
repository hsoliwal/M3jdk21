# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""STRING_JOIN v1: names-only (UFI tier 1) join of string-related donor sources against M3JDK owners.

Inputs are path listings produced by `git ls-tree -r --name-only <ref>` (no blob reads).
Output rows are proposals only (NEC): a name match never decides equivalence.

usage: string_join.py <donor_string_main.txt> <m3jdk_master_java.txt> <out_dir>
"""
import collections
import re
import sys

NOISE_ROOTS = {
    ".superset", "chat-artifacts", "third_party", ".merge-preserved", ".m3", "docs", "donors",
    "scripts", "tools", "tmp", "experiments", "raw",
}
RECIPE_ROOTS = {"synexia-openrewrite-recipes", "synexia-m3-recipe", "m3-recipe-proof"}
PREFIXES = ("MIndex", "M3Index", "M3", "Index", "Synexia")
FAMILIES = (
    "MIndexString", "IndexString", "M3String", "Rope", "CharSeq", "Utf16", "Utf8", "Latin1",
    "Regex", "Lexicon", "Intern", "String",
)
TEST_PATH = re.compile(r"(^|/)(test|tests|jmh|bench)/")
SKIP_M3JDK = re.compile(r"/vendor/|/third_party")


def simple_name(path):
    return path.rsplit("/", 1)[-1][: -len(".java")]


def concept_key(name):
    changed = True
    while changed:
        changed = False
        for prefix in PREFIXES:
            rest = name[len(prefix):]
            if name.startswith(prefix) and rest[:1].isupper():
                name, changed = rest, True
    return name


def family(name):
    return next((f for f in FAMILIES if f in name), "OTHER")


def root_class(root):
    if root in NOISE_ROOTS:
        return "NOISE_COPY"
    if root in RECIPE_ROOTS:
        return "RECIPE_OWNER"
    return "LIVE_MODULE"


def read_lines(path):
    with open(path, encoding="utf-8", newline="") as handle:
        return [line.strip() for line in handle if line.strip()]


def owner_index(m3jdk_paths):
    index = collections.defaultdict(list)
    for path in m3jdk_paths:
        if TEST_PATH.search(path) or SKIP_M3JDK.search(path):
            continue
        index[concept_key(simple_name(path))].append(path)
    return index


def join_rows(donor_paths, index):
    for path in donor_paths:
        root = path.split("/", 1)[0]
        name = simple_name(path)
        key = concept_key(name)
        owners = index.get(key, [])
        in_base = [p for p in owners if p.startswith("src/java.base/")]
        status = "NAME_IN_JAVA_BASE" if in_base else ("NAME_IN_M3JDK" if owners else "NAME_ABSENT")
        yield (root_class(root), root, family(name), name, key, status, str(len(owners)),
               (in_base or owners or ["-"])[0], path)


def write_tsv(path, header, rows):
    with open(path, "w", encoding="utf-8", newline="") as handle:
        handle.write("\t".join(header) + "\n")
        handle.writelines("\t".join(row) + "\n" for row in rows)


def main(argv):
    donor_file, m3jdk_file, out_dir = argv[1:4]
    index = owner_index(read_lines(m3jdk_file))
    rows = sorted(join_rows(read_lines(donor_file), index))
    header = ("root_class", "donor_root", "family", "donor_type", "concept_key", "status",
              "m3jdk_name_matches", "first_m3jdk_path", "donor_path")
    write_tsv(out_dir + "/STRING_CONVERGE_JOIN.tsv", header, rows)
    live = [row for row in rows if row[0] == "LIVE_MODULE"]
    by_module = collections.Counter((row[1], row[5]) for row in live)
    modules = sorted({row[1] for row in live}, key=lambda m: -sum(by_module[(m, s)] for s in (
        "NAME_ABSENT", "NAME_IN_M3JDK", "NAME_IN_JAVA_BASE")))
    summary = [(m, str(by_module[(m, "NAME_IN_JAVA_BASE")]), str(by_module[(m, "NAME_IN_M3JDK")]),
                str(by_module[(m, "NAME_ABSENT")])) for m in modules]
    write_tsv(out_dir + "/STRING_CONVERGE_MODULES.tsv",
              ("donor_root", "name_in_java_base", "name_in_m3jdk", "name_absent"), summary)
    classes = collections.Counter(row[0] for row in rows)
    status = collections.Counter(row[5] for row in live)
    print("rows", len(rows), dict(classes))
    print("live", len(live), dict(status))
    for row in summary[:24]:
        print("\t".join(row))


if __name__ == "__main__":
    main(sys.argv)
