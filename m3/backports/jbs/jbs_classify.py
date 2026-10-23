#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Pass 3 of the JBS scaffold: a provisional disposition per unclassified enhancement, by component family,
from the dispositions the programme already recorded for JEPs (JEP_CATALOGUE.tsv) and seeds
(UPSTREAM_CHANGE_SEEDS.tsv). Mechanical and reversible: the column is `provisional_disposition`, the
`disposition` column stays UNCLASSIFIED until the reading pass confirms a row.
Usage: jbs_classify.py JBS_ENHANCEMENTS_22_27.tsv JEP_CATALOGUE.tsv UPSTREAM_CHANGE_SEEDS.tsv > out.tsv"""
import csv, sys, collections, re
rows = list(csv.DictReader(open(sys.argv[1], encoding="utf-8", newline=""), delimiter="\t"))
# component family -> provisional disposition, from the programme's own rules (locked compiler/source semantics,
# JIT holds, high-risk runtime/native, additive tooling admitted)
RULES = [
    (r"^hotspot/compiler|^hotspot/jit|/jit\b", "hold-jit"),
    (r"^hotspot/gc", "hold-gc-matrix"),
    (r"^hotspot/runtime|^hotspot/jvmti|^hotspot/svc", "candidate-adapted-serviceability" ),
    (r"^tools/javac|^tools/javadoc|^specification", "hold-javac"),
    (r"^core-libs/java\.lang\.foreign|^core-libs/java\.lang\.invoke|^core-libs/java\.lang:class_loading|^core-libs/java\.lang:reflect", "candidate-high-risk"),
    (r"^core-libs/java\.util:collections|^core-libs/java\.util\.stream|^core-libs/java\.util\.concurrent", "candidate-collections-lane"),
    (r"^hotspot/jfr|^hotspot/", "hold-runtime"),            # reading pass 2026-10-23: every other HotSpot component is runtime-coupled
    (r"^tools/jpackage|^tools/jshell|^tools/jdeps", "admitted-additive-tooling"),
    (r"^core-libs/java\.lang|^core-libs/java\.util|^core-libs/java\.text|^core-libs/java\.time|^core-libs/java\.io|^core-libs/java\.nio|^core-libs/java\.net|^core-libs/xml|^xml", "candidate-adapted"),
    (r"^security-libs", "candidate-adapted-security"),
    (r"^client-libs", "reject-client-libs"),
    (r"^infrastructure|^core-svc/tools|^tools/launcher|^tools/jlink|^tools/jar", "admitted-additive-tooling"),
    (r"^core-svc", "candidate-adapted-serviceability"),
    (r"^docs", "admitted-docs"),
    (r"^performance", "candidate-measured"),
]
def provisional(component, sub, summary):
    key = component + ("/" + sub if sub and sub != "-" else "")
    s = summary.lower()
    if re.search(r"\b(language|syntax|preview|unnamed|pattern matching|records?|sealed|switch)\b", s) and component.startswith("tools"):
        return "reject-language"
    for rx, d in RULES:
        if re.search(rx, key): return d
    return "candidate-adapted"
out = sys.stdout
fields = [k for k in rows[0].keys() if k != "provisional_disposition"] + ["provisional_disposition"]   # re-runs read their own output
w = csv.DictWriter(out, fieldnames=fields, delimiter="\t", lineterminator="\n", quoting=csv.QUOTE_NONE, quotechar=None, escapechar=None)
w.writeheader(); c = collections.Counter()
for r in rows:
    r["provisional_disposition"] = "already-in-jdk21u" if r["disposition"] == "ALREADY_IN_JDK21U" else provisional(r["component"], r["subcomponent"], r["summary"])
    c[r["provisional_disposition"]] += 1; w.writerow(r)
print("\n".join(f"{k}\t{v}" for k, v in c.most_common()), file=sys.stderr)
