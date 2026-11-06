# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""classify3.py: union of the two loss-candidate methods -> LOSS_CANDIDATES.tsv
(path, method, source_commit_or_blob, pr, note, class).

Dropper = where the path's lineage ended on master's history. Order of evidence:
  1. a first-parent deletion (DROPPERS_FP.txt: path in the first parent, absent in the commit), newest first:
       non-merge commit                        -> DELIBERATE (cite)
       merge, path in p1 and p2                -> RESTORE (both sides had it; the merge dropped it)
       merge, path in p1, not p2, in base      -> DELIBERATE (the PR side deleted it; the merge followed)
       merge, path in p1, not p2, not base     -> RESTORE (a superset-only addition the merge dropped)
  2. else the merges that left a second parent's copy out (DROPPERS_RAW_ALL.txt, -m), EARLIEST first:
       earliest is "Superset: retain PR #N"    -> DELIBERATE (RETAIN_MERGE_WHOLE_PR: the live tree stays, the PR's
                                                  files are kept under retained-variants/prN; promotion is a reviewed
                                                  follow-up by the landing's own statement)
       otherwise                               -> RESTORE from the latest excluder's second parent (newest blob)
  3. no deletion anywhere: a post-rename twin of a candidate path (JDK naming) -> DUPLICATE; else UNSURE
Earlier classes: RETAINED_COPY (nested copy under retained-variants), RENAMED (JDK-naming target on master),
MOVED (same blob elsewhere on master)."""
import re
import subprocess
import sys
from collections import Counter, defaultdict

sys.path.insert(0, "S:/m3-scratch/eb-conv/restore/naming")
import jdk_naming as jn  # noqa: E402

REPO = "C:/m3-superset/conv/m3jdk21"
RET = "m3/qualification/master-landing-20261028/retained-variants/"
LANDING = re.compile(r"^(Superset\b|superset 31|merge\(superset|merge\(m3 superset|m3\(superset|Merge M3jdk21 superset landing)")
WHOLE_PR = re.compile(r"^Superset: retain PR #(\d+)")
GROUPS = [l.strip() for l in open("GROUPS.txt", encoding="utf-8") if l.strip()]


def git(*a):
    return subprocess.run(["git", "-C", REPO, *a], capture_output=True, text=True, encoding="utf-8",
                          errors="replace").stdout


def tree(commit):
    out = {}
    for line in git("ls-tree", "-r", commit, "--", *GROUPS).split("\n"):
        if line:
            meta, _, path = line.partition("\t")
            out[path] = meta.split()[2]
    return out


def harvest(fname):
    drop, cur = defaultdict(list), None
    for line in open(fname, encoding="utf-8", errors="replace"):
        line = line.rstrip("\n")
        if line.startswith("\x01"):
            hdr, _, subj = line[1:].partition("|")
            f = hdr.split()
            cur = (f[0], f[1], tuple(f[2:]), subj)
        elif line and cur:
            if not drop[line] or drop[line][-1][0] != cur[0]:
                drop[line].append(cur)
    return drop


master = set(open("MASTER_PATHS.txt", encoding="utf-8").read().split("\n"))
blob_paths = defaultdict(list)
for line in open("MASTER_LS.tsv", encoding="utf-8"):
    b, _, p = line.rstrip("\n").partition("\t")
    blob_paths[b].append(p)
ret_blob = {}
for line in open("RETAINED_LS.tsv", encoding="utf-8"):
    b, _, p = line.rstrip("\n").partition("\t")
    ret_blob[p] = b
fp_drop = harvest("DROPPERS_FP.txt")
m_drop = harvest("DROPPERS_RAW_ALL.txt")
heads = defaultdict(list)
for line in list(open("LOSS_RAW.tsv", encoding="utf-8"))[1:]:
    p, pr, head, hdate, m, base, st, blob = line.rstrip("\n").split("\t")
    heads[p].append((hdate, pr, head, blob, st))
retained = defaultdict(list)
for line in list(open("RETAINED_ORPHANS.tsv", encoding="utf-8"))[1:]:
    rel, label, rp = line.rstrip("\n").split("\t")
    retained[rel].append((label, rp))
originals = {}
for p in heads:
    t = jn.target_path(p)
    if t != p:
        originals.setdefault(t, p)
rows = {}
pending = defaultdict(list)
for p in sorted(set(heads) | set(retained)):
    hs = sorted(heads.get(p, []), reverse=True)
    rs = retained.get(p, [])
    method = "both" if hs and rs else ("loss-guard-local" if hs else "retained-twin")
    pr = ",".join(sorted({h[1] for h in hs} | {r[0] for r in rs}))
    rblobs = {ret_blob.get(r[1]) for r in rs} - {None}
    src = ("commit " + hs[0][2] + " blob " + hs[0][3]) if hs else ("retained " + rs[0][1] + " blob " + next(iter(rblobs), "?"))
    base_row = [p, method, src, pr]
    if p.startswith(RET):
        rows[p] = base_row + ["nested retained copy", "RETAINED_COPY"]
        continue
    t = jn.target_path(p)
    if t != p and t in master:
        rows[p] = base_row + ["renamed to " + t + " (JDK naming 5d454c3be9b)", "RENAMED"]
        continue
    cand_blobs = {h[3] for h in hs} | rblobs
    hit = [(b, blob_paths[b][0]) for b in cand_blobs if b in blob_paths and not blob_paths[b][0].startswith(RET)]
    if hit:
        rows[p] = base_row + ["same blob on master at " + hit[0][1], "MOVED"]
        continue
    fps = fp_drop.get(p) or fp_drop.get(t) or []
    ms = m_drop.get(p) or m_drop.get(t) or []
    if fps:
        rows[p] = base_row
        pending[("fp", fps[0])].append(p)
    elif ms:
        earliest, latest = ms[-1], ms[0]
        w = WHOLE_PR.match(earliest[3])
        if w:
            kept = ("kept under retained-variants/pr" + w.group(1) if (rs or retained.get(t))
                    else "inherited from a stacked base, not retained")
            rows[p] = base_row + [f"RETAIN_MERGE_WHOLE_PR hold at {earliest[0][:12]} {earliest[1]} (#{w.group(1)}): live tree kept, "
                                  f"PR files {kept}; promotion is a reviewed follow-up (landing statement)", "DELIBERATE"]
        else:
            rows[p] = base_row
            pending[("m", latest)].append(p)
    elif p in originals:
        rows[p] = base_row + [f"post-rename twin of {originals[p]} (see that row)", "DUPLICATE"]
    else:
        rows[p] = base_row + ["no deletion commit found on master history", "UNSURE"]
print("pending droppers", len(pending), "paths", sum(len(v) for v in pending.values()), file=sys.stderr)
for i, ((kind, d), items) in enumerate(sorted(pending.items(), key=lambda kv: -len(kv[1]))):
    sha, date, parents, subj = d
    cite = f"{sha[:12]} {date} {subj[:90]}"
    print(f"heartbeat classify {i + 1}/{len(pending)} {sha[:12]} paths={len(items)}", file=sys.stderr)
    if len(parents) < 2:
        cls = "UNSURE" if LANDING.search(subj) else "DELIBERATE"
        for p in items:
            rows[p] += [f"deleted by non-merge commit {cite}", cls]
        continue
    p1, p2 = parents[0], parents[1]
    mb = git("merge-base", p1, p2).strip()
    t1, t2, tb = tree(p1), tree(p2), tree(mb) if mb else {}
    for p in items:
        in1, in2, inb = p in t1, p in t2, p in tb
        if in1 and in2:
            rows[p] += [f"dropped by merge {cite} | present in both parents", "RESTORE"]
            rows[p][2] = f"{p1[:12]}:{p} blob {t1[p]}" + ("" if t1[p] == t2[p] else f" (p2 blob {t2[p]})")
        elif in1 and inb:
            rows[p] += [f"dropped by merge {cite} | the PR side (p2 {p2[:12]}) deleted it", "DELIBERATE"]
        elif in1:
            rows[p] += [f"dropped by merge {cite} | superset-only addition, p2 never had it", "RESTORE"]
            rows[p][2] = f"{p1[:12]}:{p} blob {t1[p]}"
        elif in2 and not inb:
            rows[p] += [f"dropped by merge {cite} | the PR's addition was left out (RETAIN resolver)", "RESTORE"]
            rows[p][2] = f"{p2[:12]}:{p} blob {t2[p]}"
        elif in2:
            rows[p] += [f"dropped by merge {cite} | carried by the p2 line, never taken by p1 (no first-parent deletion)", "RESTORE"]
            rows[p][2] = f"{p2[:12]}:{p} blob {t2[p]}"
        else:
            rows[p] += [f"dropper {cite} holds it in no parent", "UNSURE"]
with open("LOSS_CANDIDATES.tsv", "w", encoding="utf-8", newline="\n") as o:
    o.write("path\tmethod\tsource_commit_or_blob\tpr\tnote\tclass\n")
    for p in sorted(rows):
        o.write("\t".join(rows[p]) + "\n")
print(Counter(r[5] for r in rows.values()))
