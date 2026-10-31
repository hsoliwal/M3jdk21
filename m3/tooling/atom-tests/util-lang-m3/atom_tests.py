# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_tests.py <repo_root> [--check] : template-first per-atom JUnit generator (CPU, deterministic).

Inputs (next to this file):
  templates/scaffold.java.tmpl   one test class: jtreg header, Access helper (privateLookupIn), ${tests}
  templates/common.java.tmpl     PATTERN/SUPPORT sections shared by every family (family sections win)
  templates/<family>.java.tmpl   sections '//@@ IMPORTS', '//@@ CONSTANTS', '//@@ HELPERS',
                                 '//@@ PATTERN <NAME> | uses=<SUPPORT,...> | oracle=<text>' and
                                 '//@@ SUPPORT <NAME>' (pattern-private helpers emitted once per class)
  specs/<family>.tsv             test_class, owner, atom, pattern, params (JSON object of ${...} values)
Each spec row binds one atom (method or constructor, named exactly as in PER_ATOM_TEST_CENSUS.tsv) to one
pattern; the pattern body is rendered with ${atom}, ${test} (Java method name, default = atom) and the
row params. A row with atom '*' and pattern CLASS carries class-level params (summary, modules, path).
Outputs: test/jdk/... Java files (one per test_class) and TEST_PATTERNS.tsv (atom, pattern, test, oracle).
--check only verifies that the generated files are byte-identical to what is on disk (fixed point)."""
import json
import os
import re
import string
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SECTION = re.compile(r"^//@@ (IMPORTS|CONSTANTS|HELPERS|PATTERN|SUPPORT)\b(.*)$", re.M)
PACKAGE_DIRS = {
    "java.util": "test/jdk/java/util/M3Atoms",
    "java.util.regex": "test/jdk/java/util/regex/M3Atoms",
    "java.lang": "test/jdk/java/lang/M3Atoms",
    "jdk.internal.m3": "test/jdk/jdk/internal/m3/atoms",
    "jdk.internal.m3.io": "test/jdk/jdk/internal/m3/atoms",
    "jdk.internal.util": "test/jdk/jdk/internal/util/M3Atoms",
    "jdk.internal.misc": "test/jdk/jdk/internal/util/M3Atoms",
}


def parse_template(path):
    text = open(path, encoding="utf-8").read()
    marks = list(SECTION.finditer(text))
    out = {"IMPORTS": "", "CONSTANTS": "", "HELPERS": "", "PATTERN": {}, "SUPPORT": {}}
    for i, m in enumerate(marks):
        body = text[m.end() + 1: marks[i + 1].start() if i + 1 < len(marks) else len(text)].rstrip("\n")
        kind, rest = m.group(1), m.group(2).strip()
        if kind in ("PATTERN", "SUPPORT"):
            parts = [p.strip() for p in rest.split(" | ")]
            meta = dict(p.split("=", 1) for p in parts[1:])
            out[kind][parts[0]] = {"body": body, "uses": [u for u in meta.get("uses", "").split(",") if u],
                                   "oracle": meta.get("oracle", "")}
        else:
            out[kind] = body
    return out


def read_spec(path):
    rows = []
    with open(path, encoding="utf-8") as f:
        header = next(f).rstrip("\n").split("\t")
        for n, line in enumerate(f, 2):
            if not line.strip() or line.startswith("#"):
                continue
            row = dict(zip(header, line.rstrip("\n").split("\t")))
            row["params"] = json.loads(row.get("params") or "{}")
            row["line"] = n
            rows.append(row)
    return rows


def render_class(scaffold, family, tmpl, test_class, rows):
    meta = next((r["params"] for r in rows if r["atom"] == "*"), {})
    atoms = [r for r in rows if r["atom"] != "*"]
    owner = atoms[0]["owner"]
    package = owner.rsplit(".", 1)[0]
    tests, supports, patterns = [], [], []
    for r in atoms:
        pattern = tmpl["PATTERN"][r["pattern"]]
        values = dict(r["params"], atom=r["atom"], test=r["params"].get("test", r["atom"]))
        try:
            tests.append(string.Template(pattern["body"]).substitute(values))
        except KeyError as e:
            raise SystemExit(f"specs/{family}.tsv:{r['line']}: pattern {r['pattern']} needs param {e}")
        supports += [u for u in pattern["uses"] if u not in supports]
        patterns.append((r, pattern, values["test"]))
    helpers = tmpl["HELPERS"] + "".join("\n" + tmpl["SUPPORT"][u]["body"] for u in supports)
    summary = meta.get("summary", f"Per-atom contracts of {owner} (generated, template {family})")
    modules = meta.get("modules", f"java.base/{package}:open")
    java = string.Template(scaffold).substitute(
        summary=summary, modules=modules, test_class=test_class, owner=owner, family=family,
        imports=tmpl["IMPORTS"], constants=tmpl["CONSTANTS"], tests="\n".join(tests), helpers=helpers)
    path = meta.get("path", PACKAGE_DIRS[package]) + "/" + test_class + ".java"
    java = "\n".join(line.rstrip() for line in java.split("\n"))
    return path, java, patterns


def main():
    root = sys.argv[1]
    check = "--check" in sys.argv
    scaffold = open(os.path.join(HERE, "templates", "scaffold.java.tmpl"), encoding="utf-8").read()
    ledger, drift = [], []
    for spec in sorted(os.listdir(os.path.join(HERE, "specs"))):
        if not spec.endswith(".tsv"):
            continue
        family = spec[:-len(".tsv")]
        tmpl = parse_template(os.path.join(HERE, "templates", family + ".java.tmpl"))
        common = parse_template(os.path.join(HERE, "templates", "common.java.tmpl"))
        tmpl["PATTERN"] = {**common["PATTERN"], **tmpl["PATTERN"]}
        tmpl["SUPPORT"] = {**common["SUPPORT"], **tmpl["SUPPORT"]}
        rows = read_spec(os.path.join(HERE, "specs", spec))
        for test_class in dict.fromkeys(r["test_class"] for r in rows):
            path, java, patterns = render_class(scaffold, family, tmpl, test_class,
                                                [r for r in rows if r["test_class"] == test_class])
            target = os.path.join(root, path)
            if check:
                current = open(target, encoding="utf-8").read() if os.path.exists(target) else None
                if current != java:
                    drift.append(path)
            else:
                os.makedirs(os.path.dirname(target), exist_ok=True)
                with open(target, "w", encoding="utf-8", newline="\n") as w:
                    w.write(java)
            ledger += [(r["owner"], r["atom"], r["pattern"], f"{path}::{test}", p["oracle"])
                       for r, p, test in patterns]
    if check:
        print("FIXED_POINT" if not drift else "DRIFT " + " ".join(drift))
        sys.exit(1 if drift else 0)
    with open(os.path.join(HERE, "TEST_PATTERNS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("owner\tatom\tpattern\ttest\toracle\n")
        w.writelines("\t".join(row) + "\n" for row in ledger)
    print(f"atoms={len(ledger)} classes={len({row[3].split('::')[0] for row in ledger})} "
          f"patterns={len({row[2] for row in ledger})}")


main()
