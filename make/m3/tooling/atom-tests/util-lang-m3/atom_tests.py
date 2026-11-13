# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""atom_tests.py <repo_root> [--check] : template-first per-atom JUnit generator (CPU, deterministic).

Inputs (next to this file):
  templates/scaffold.java.tmpl   one test class: jtreg header, Access helper (privateLookupIn), ${tests}
  templates/common.java.tmpl     PATTERN/SUPPORT sections shared by every family (family sections win)
  templates/<family>.java.tmpl   sections '//@@ IMPORTS', '//@@ CONSTANTS', '//@@ HELPERS',
                                 '//@@ PATTERN <NAME> | uses=<SUPPORT,...> | oracle=<text>' and
                                 '//@@ SUPPORT <NAME> [| place=constants]' (emitted once per class that uses it,
                                 among the helpers, or among the constants with place=constants)
  specs/<family>.tsv             test_class, owner, atom, pattern, params (JSON object of ${...} values)
Each spec row binds one atom (method or constructor, named exactly as in PER_ATOM_TEST_CENSUS.tsv) to one
pattern; the pattern body is rendered with ${atom}, ${test} (Java method name, default = atom) and the
row params; a "uses" param adds SUPPORT sections for that row. A row with atom '*' and pattern CLASS carries
class-level params (summary, run_options, path). A row with pattern HOLD renders no test: its "reason" param
goes to the ledger, so an atom that cannot run on the test runtime is stated, not silently skipped.
The jtreg @modules line is derived from the rendered class: every package it reflects into (the owner and each
new Access("...")/Class.forName("...") target) is opened, every internal package it names in code is exported,
a package in both gets :+open. Generation fails on a boolean-literal template leftover (!true, && false,
if (false), ...) and on any numeric literal outside a named constant (literal_census.py --strict).
Outputs: test/jdk/... Java files (one per test_class) and TEST_PATTERNS.tsv (owner, atom, pattern, test, oracle).
--check only verifies that the generated files are byte-identical to what is on disk (fixed point)."""
import json
import os
import re
import string
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import literal_census  # noqa: E402  (make/m3/tooling/atom-tests/literal_census.py)

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
MODULE = "java.base"
REFLECTED = re.compile(r'new Access\("([\w.$]+)"\)|Class\.forName\("([\w.$]+)"')
INTERNAL_ROOT = ("jdk", "internal")
LEFTOVER = re.compile(r"!\s*(?:true|false)\b|(?:&&|\|\|)\s*(?:true|false)\b|\b(?:true|false)\s*(?:&&|\|\|)"
                      r"|[!=]=\s*(?:true|false)\b|\bif\s*\(\s*(?:true|false)\s*\)")
MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"""(?:\\.|[^\\])*?"""|"(?:\\.|[^"\\\n])*"|\'(?:\\.|[^\'\\\n])+\'', re.S)
MODULES_SLOT = "@@MODULES@@"


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
                                   "oracle": meta.get("oracle", ""), "place": meta.get("place", "helpers")}
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


def package_of(binary_name):
    return binary_name.split("$")[0].rsplit(".", 1)[0]


def internal_packages(java):
    """Packages under jdk.internal that the code names (imports and qualified type names, not strings)."""
    toks = literal_census.tokens(java)
    found = set()
    for i in range(len(toks) - 2):
        if (toks[i][1], toks[i + 1][1], toks[i + 2][1]) != ("jdk", ".", "internal") or \
                (i > 0 and toks[i - 1][1] == "."):
            continue
        parts, j = [], i
        while j < len(toks) and toks[j][0] == "ident":
            parts.append(toks[j][1])
            if j + 1 < len(toks) and toks[j + 1][1] == "." and toks[j][1][:1].islower():
                j += 2
            else:
                break
        if parts and parts[-1][:1].isupper():
            found.add(".".join(parts[:-1]))
    return found


def modules_line(owner, java):
    reflected = {package_of(owner)} | {package_of(a or b) for a, b in REFLECTED.findall(java)}
    compiled = internal_packages(java)
    flags = {}
    for p in reflected | compiled:
        flags[p] = ":+open" if p in reflected and p in compiled else (":open" if p in reflected else "")
    first = package_of(owner)
    ordered = [first] + sorted(p for p in flags if p != first)
    return " ".join(f"{MODULE}/{p}{flags[p]}" for p in ordered)


def render_class(scaffold, family, tmpl, test_class, rows):
    meta = next((r["params"] for r in rows if r["atom"] == "*"), {})
    if "modules" in meta:
        raise SystemExit(f"specs/{family}.tsv: {test_class} CLASS row sets 'modules'; the generator derives @modules")
    atoms = [r for r in rows if r["atom"] != "*"]
    owner = atoms[0]["owner"]
    package = owner.rsplit(".", 1)[0]
    tests, supports, patterns, holds = [], [], [], []
    for r in atoms:
        if r["pattern"] == "HOLD":
            holds.append((r, r["params"]["reason"]))
            continue
        pattern = tmpl["PATTERN"][r["pattern"]]
        values = dict(r["params"], atom=r["atom"], test=r["params"].get("test", r["atom"]))
        try:
            tests.append(string.Template(pattern["body"]).substitute(values))
        except KeyError as e:
            raise SystemExit(f"specs/{family}.tsv:{r['line']}: pattern {r['pattern']} needs param {e}")
        row_uses = [u for u in r["params"].get("uses", "").split(",") if u]
        for u in pattern["uses"] + row_uses:
            for dep in tmpl["SUPPORT"][u]["uses"] + [u]:
                if dep not in supports:
                    supports.append(dep)
        patterns.append((r, pattern, values["test"]))
    constants = tmpl["CONSTANTS"] + "".join("\n" + tmpl["SUPPORT"][u]["body"] for u in supports
                                            if tmpl["SUPPORT"][u]["place"] == "constants")
    helpers = tmpl["HELPERS"] + "".join("\n" + tmpl["SUPPORT"][u]["body"] for u in supports
                                        if tmpl["SUPPORT"][u]["place"] != "constants")
    summary = meta.get("summary", f"Per-atom contracts of {owner} (generated, template {family})")
    java = string.Template(scaffold).substitute(
        summary=summary, modules=MODULES_SLOT, test_class=test_class, owner=owner, family=family,
        run_options=meta.get("run_options", ""),
        imports=tmpl["IMPORTS"], constants=constants, tests="\n".join(tests), helpers=helpers)
    java = java.replace(MODULES_SLOT, modules_line(owner, java), 1)
    path = meta.get("path", PACKAGE_DIRS[package]) + "/" + test_class + ".java"
    java = "\n".join(line.rstrip() for line in java.split("\n"))
    return path, java, patterns, holds


def gates(path, java):
    """Leftover and literal gates on one rendered class: a list of violations (empty when clean)."""
    out = []
    code = MASK.sub('""', java)
    out += [f"{path}: template leftover '{m.group(0)}'" for m in LEFTOVER.finditer(code)]
    out += [f"{path}:{line}: literal {lit} outside a named constant ({ctx})"
            for line, lit, _, ctx in literal_census.scan(java, strict=True)]
    return out


def main():
    root = sys.argv[1]
    check = "--check" in sys.argv
    scaffold = open(os.path.join(HERE, "templates", "scaffold.java.tmpl"), encoding="utf-8").read()
    ledger, drift, violations, hold_count = [], [], [], 0
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
            path, java, patterns, holds = render_class(scaffold, family, tmpl, test_class,
                                                       [r for r in rows if r["test_class"] == test_class])
            violations += gates(path, java)
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
            ledger += [(r["owner"], r["atom"], "HOLD", "-", reason) for r, reason in holds]
            hold_count += len(holds)
    if violations:
        raise SystemExit("generation gates failed:\n  " + "\n  ".join(violations))
    if check:
        print("FIXED_POINT" if not drift else "DRIFT " + " ".join(drift))
        sys.exit(1 if drift else 0)
    with open(os.path.join(HERE, "TEST_PATTERNS.tsv"), "w", encoding="utf-8", newline="\n") as w:
        w.write("owner\tatom\tpattern\ttest\toracle\n")
        w.writelines("\t".join(row) + "\n" for row in ledger)
    tested = [row for row in ledger if row[2] != "HOLD"]
    print(f"atoms={len(tested)} hold={hold_count} classes={len({row[3].split('::')[0] for row in tested})} "
          f"patterns={len({row[2] for row in tested})} gates=leftover,literal(strict) violations=0")


main()
