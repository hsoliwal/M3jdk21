#!/usr/bin/env python
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0
"""Atom VETOED_JEP_INDEX: the JEPs OpenJDK closed as Withdrawn or Rejected (JBS JEP issues), joined with the JEP 0 index
(openjdk.org/jeps/0 snapshot), the openjdk/jdk history (commits whose subject names the JEP number) and the branch lists
of the OpenJDK project repos (git ls-remote: jdk-sandbox, valhalla, amber, loom, panama, leyden, babylon, lilliput, ...).
Branch matches are by JEP number in the branch name, or by >= 2 distinctive title words; they are candidates, not proof.
Usage: vetoed_jeps.py <data dir> <out tsv> [reasons tsv]"""
import csv, html, os, re, sys
D, OUT = sys.argv[1], sys.argv[2]
REASONS = {}
if len(sys.argv) > 3 and os.path.exists(sys.argv[3]):
    for r in csv.reader(open(sys.argv[3], encoding="utf-8", newline=""), delimiter="\t"): REASONS[r[0]] = r[1]
page = open(os.path.join(D, "peer/jep0.html"), encoding="utf-8").read()
idx = {}
for row in re.findall(r"<tr>(.*?)</tr>", page, re.S):
    n = re.search(r'<td class="jep">(\d+)</td>', row)
    if not n: continue
    st = re.search(r'title="Status: ([^"]+)"', row); rel = re.search(r'title="Release: ([^"]+)"', row)
    cl = re.search(r'class="cl">([^<]*)<', row); cr = re.search(r'class="cr">([^<]*)<', row)
    idx[n.group(1)] = (html.unescape(st.group(1)).replace(" ", "") if st else "", rel.group(1) if rel else "",
                       (cl.group(1) if cl else "") + "/" + (cr.group(1) if cr else ""))
subjects = [l.rstrip("\n").split("\t", 2) for l in open(os.path.join(D, "openjdk_all_subjects.tsv"), encoding="utf-8")]
branches = []
for f in sorted(os.listdir(os.path.join(D, "lsremote"))):
    for l in open(os.path.join(D, "lsremote", f), encoding="utf-8"):
        sha, ref = l.split(); branches.append((f[:-4], ref[len("refs/heads/"):], sha))
STOP = set("with from into that this java jdk api apis support new feature features preview incubator second third first "
           "fourth final remove deprecate deprecation update improve improved enhancements enhanced based more using".split())
words = lambda s: {w for w in re.findall(r"[a-z]{4,}", s.lower()) if w not in STOP}
BSTOP = {"mast", "test", "bran", "temp", "jdk"}
stem = lambda ws: {w[:5] for w in ws} - BSTOP
def bnorm(b): return stem(re.findall(r"[a-z]{4,}", b.lower().replace("_", "-")))
norm = lambda t: " ".join(re.sub(r"\([^)]*\)", " ", t).lower().split())
alljeps = {}
for r in csv.DictReader(open(os.path.join(D, "peer/jeps.tsv"), encoding="utf-8", newline=""), delimiter="	"):
    m = re.match(r"JEP (\d+):\s*(.*)", r["summary"])
    if m: alljeps[m.group(1)] = norm(m.group(2))
rows = []
for r in csv.DictReader(open(os.path.join(D, "peer/jeps.tsv"), encoding="utf-8", newline=""), delimiter="\t"):
    if r["resolution"] not in ("Withdrawn", "Rejected"): continue
    m = re.match(r"JEP (\d+):\s*(.*)", r["summary"]); n, title = (m.group(1), m.group(2)) if m else ("", r["summary"])
    st, rel, comp = idx.get(n, ("", "", ""))
    nt = norm(title); sib = sorted({k for k, v in alljeps.items() if v == nt} | ({n} if n else set()))
    rx = re.compile(r"\bJEP[ -]?(%s)\b" % "|".join(sib)) if sib else None
    phrase = nt if len(nt.split()) >= 2 else None
    commits = [f"{s[:11]}@{d}" for s, d, subj in subjects if (rx and rx.search(subj)) or (phrase and phrase in subj.lower())]
    tw = stem(words(title))
    def hit(b):
        bs = bnorm(b)
        return bool((n and re.search(r"(^|[^0-9])%s([^0-9]|$)" % n, b) and re.search(r"jep", b, re.I))
                    or len(tw & bs) >= 2 or (len(bs) == 1 and bs <= tw))
    br = [f"{repo}:{b}" for repo, b, _ in branches if hit(b)]
    rows.append([r["key"], n or "-", title, f'{r["status"]}/{r["resolution"]}', r["resolved"] or "-", comp or r["components"] or "-",
                 rel or r["fixVersions"] or "-", REASONS.get(r["key"], "PENDING: JBS REST and openjdk.org returned HTTP 403 on 2026-10-29"),
                 str(len(commits)), ("sibling JEPs " + "/".join(x for x in sib if x != n) + "; " if len(sib) > 1 else "") + (",".join(commits[:8]) or "-") or "-", ",".join(br[:8]) or "-"])
rows.sort(key=lambda r: int(r[1]) if r[1].isdigit() else 10 ** 6)
with open(OUT, "w", encoding="utf-8", newline="\n") as o:
    o.write("jbs_id\tjep\ttitle\tstate\tresolved\tcomponent\trelease\treason_text\tjdk_commits_naming_jep\tjdk_commit_sample\tproject_branch_candidates\n")
    for r in rows: o.write("\t".join(c.replace("\t", " ") for c in r) + "\n")
print("vetoed", len(rows), "with_commits", sum(r[8] != "0" for r in rows), "with_branches", sum(r[10] != "-" for r in rows))
