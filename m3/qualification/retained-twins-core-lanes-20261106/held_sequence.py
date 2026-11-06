# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""held_sequence.py <HELD_PRS.tsv> <LOSS_CANDIDATES.tsv> <MASTER_PATHS.txt> <TWIN_CONTENT.tsv> <PR_TITLES.tsv> <out.tsv>
HELD_PRS_SEQUENCE v1: the whole-PR-retained PRs (loss-guard HELD_PRS.tsv) ordered for one-PR-per-round promotion.

Per PR: held paths = the loss-guard HELD_PRS total (DELIBERATE loss candidates absent from master); retained paths =
every retained-variants/pr<N>/<rel> on master (the label's ledger files excluded); inherited paths = the DELIBERATE
rows citing the PR with no retained copy; promotion paths = retained + inherited (what one round has to handle);
live twins = retained <rel> that also exist live on master (every one is a content decision for the owner: the
TWIN_CONTENT rows among them are the ones with members the live file lacks); modules = the first two path components.
Order (deterministic): round 1.. = PRs with no live twin, fewest promotion paths first (pure additions: promote by
copy); then the PRs with live twins, fewest first (promote through RETAINED_AUDIT/RETAINED_3WAY); #2147 last, marked
DEDICATED_PASS (8,297 inherited paths: the vendored public-fork-recipe-custody crate, one pass of its own)."""
import sys
from collections import defaultdict

PREFIX = "m3/qualification/master-landing-20261028/retained-variants/"
LEDGER_FILES = {"SOURCE_SHA256.tsv", "README.md"}  # the retain ledgers at a label root, not PR paths


def module(path):
    parts = path.split("/")
    return "/".join(parts[:2]) if len(parts) > 2 else parts[0]


def main(argv):
    held, cand, master, twins, titles, out = argv[1:7]
    master_paths = set(l.rstrip("\n") for l in open(master, encoding="utf-8"))
    retained = defaultdict(set)
    for p in master_paths:
        if p.startswith(PREFIX):
            label, _, rel = p[len(PREFIX):].partition("/")
            if label.startswith("pr") and label[2:].isdigit() and rel not in LEDGER_FILES:
                retained[int(label[2:])].add(rel)
    inherited = defaultdict(set)
    for l in list(open(cand, encoding="utf-8"))[1:]:
        path, _m, _src, pr, _note, cls = l.rstrip("\n").split("\t")
        if cls == "DELIBERATE" and pr.split(",")[0].isdigit():
            inherited[int(pr.split(",")[0])].add(path)
    content_twins = defaultdict(set)
    for l in list(open(twins, encoding="utf-8"))[1:]:
        rel, label = l.rstrip("\n").split("\t")[:2]
        if label.startswith("pr") and label[2:].isdigit():
            content_twins[int(label[2:])].add(rel)
    title = {}
    for l in open(titles, encoding="utf-8"):
        f = l.rstrip("\n").split("\t")
        if len(f) >= 3 and f[0].isdigit():
            title[int(f[0])] = (f[1], f[2])
    rows = []
    for l in list(open(held, encoding="utf-8"))[1:]:
        pr, retain_merge, date, _rc, _inh, total = l.rstrip("\n").split("\t")
        pr = int(pr)
        ret, inh = retained[pr], inherited[pr] - retained[pr]
        live = sorted(r for r in ret if r in master_paths)
        mods = sorted({module(p) for p in ret | inh})
        rows.append((pr, retain_merge, date, int(total), len(ret), len(inh), len(live), len(content_twins[pr]),
                     ";".join(mods), title.get(pr, ("", "")), len(ret) + len(inh)))
    pure = sorted((r for r in rows if r[6] == 0 and r[0] != 2147), key=lambda r: (r[10], r[0]))
    twin = sorted((r for r in rows if r[6] > 0 and r[0] != 2147), key=lambda r: (r[10], r[0]))
    dedicated = [r for r in rows if r[0] == 2147]
    with open(out, "w", encoding="utf-8", newline="\n") as o:
        o.write("round\tphase\tpr\tstate\tretain_merge\tdate\tpromotion_paths\theld_paths\tretained_paths\t"
                "inherited_not_retained\tlive_twins\tcontent_twins\tmodules\ttitle\n")
        n = 0
        for phase, group in (("NO_LIVE_TWIN", pure), ("LIVE_TWINS", twin), ("DEDICATED_PASS", dedicated)):
            for r in group:
                n += 1
                o.write("\t".join((str(n), phase, str(r[0]), r[9][0], r[1], r[2], str(r[10]), str(r[3]), str(r[4]),
                                   str(r[5]), str(r[6]), str(r[7]), r[8], r[9][1])) + "\n")
    print("HELD_PRS_SEQUENCE prs=%d no_live_twin=%d live_twins=%d dedicated=%d paths=%d" % (
        len(rows), len(pure), len(twin), len(dedicated), sum(r[3] for r in rows)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
