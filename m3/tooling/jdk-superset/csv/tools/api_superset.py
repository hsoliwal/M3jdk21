# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""api_superset.py <image_root> <out.tsv> : API_SUPERSET v1 (CPU, regex-level, read-only). For each donor's
configuration surface (classes matched by CONFIG below), take every public setter-like method (set*/with*/enable*/
disable*/is*/get* and builder methods), normalize it to a capability key (drop the verb, lower-case, drop
"enabled"/"setting"), and emit the union: capability -> which donors provide it and under what names. This is the
capability superset the M3 API (jdk.m3 owner) must cover; every row cites the donor methods."""
import os
import re
import sys
from collections import defaultdict

CONFIG = {
    "univocity-parsers": re.compile(r"/(Csv|Tsv|FixedWidth)?(Parser|Writer|Common)?Settings\.java$|/(Csv|Tsv|FixedWidth)?Format\.java$|/CommonSettings\.java$"),
    "commons-csv": re.compile(r"/CSVFormat\.java$"),
    "jackson-dataformat-csv": re.compile(r"/CsvSchema\.java$|/CsvParser\.java$|/CsvGenerator\.java$|/CsvMapper\.java$"),
}
MASK = re.compile(r'//[^\n]*|/\*.*?\*/|"(?:\\.|[^"\\\n])*"', re.S)
METHOD = re.compile(r"\bpublic\s+(?:static\s+|final\s+|synchronized\s+)*(?:<[^>]+>\s*)?[\w.<>\[\], ?]+\s+(\w+)\s*\(([^)]*)\)")
VERB = re.compile(r"^(set|with|enable|disable|is|get|has|use|without)(?=[A-Z])")
SKIP = {"toString", "hashCode", "equals", "clone", "build", "builder", "copy", "main", "getClass"}


def key(name):
    k = VERB.sub("", name)
    k = re.sub(r"(Enabled|Setting|Settings)$", "", k)
    return k[:1].lower() + k[1:] if k else name


def main():
    root, out = sys.argv[1], sys.argv[2]
    caps = defaultdict(lambda: defaultdict(set))
    for donor, rx in CONFIG.items():
        base = os.path.join(root, donor)
        for dirpath, _, files in os.walk(base):
            for f in files:
                p = os.path.join(dirpath, f).replace(os.sep, "/")
                if not rx.search(p):
                    continue
                s = MASK.sub(" ", open(p, encoding="utf-8", errors="replace").read())
                cls = f[:-5]
                for m in METHOD.finditer(s):
                    name = m.group(1)
                    if name in SKIP or name == cls:
                        continue
                    caps[key(name)][donor].add(f"{cls}.{name}")
    donors = list(CONFIG)
    rows = []
    for k in sorted(caps, key=str.lower):
        have = [d for d in donors if caps[k].get(d)]
        rows.append([k, str(len(have))] + [";".join(sorted(caps[k].get(d, ()))) or "-" for d in donors])
    with open(out, "w", encoding="utf-8", newline="\n") as w:
        w.write("capability\tdonors\t" + "\t".join(donors) + "\n" + "".join("\t".join(r) + "\n" for r in rows))
    from collections import Counter
    c = Counter(r[1] for r in rows)
    print(f"capabilities={len(rows)} in_all_3={c.get('3', 0)} in_2={c.get('2', 0)} in_1={c.get('1', 0)}")
    for d in donors:
        print(d, "only:", sum(1 for r in rows if r[1] == "1" and r[2 + donors.index(d)] != "-"))


main()
