#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Recipe JBCOLL_COLLECTIONVIEWS v1: census leaf L001 (java.base java/util/CollectionViews.java).

Derived from m3/qualification/progress-monitor-unify-20261106/unify.py (hash-pinned preimage, anchors that must
occur exactly once, drift refusal, idempotent plan). The census (S:/m3-scratch/eb-conv/census, rows of crates
collectionviews-current-owner and m3-collectionviews-onepass-cow-20261023) found 7 crate postimage lines absent from
the live owner. Each becomes one rule with a verdict:

  APPLY              the rule's census text replaces the owner text in the tree
  SUPERSEDED         the owner replaced the census text later on purpose (commits named in the rule); never applied
  FORMAT_EQUIVALENT  same token stream or the same constant; never applied (proof: javac -g:none class bytes equal)

No rule of v1 is APPLY: the live owner already holds every semantic line of both crates (README.md). The recipe
still runs every rule on each invocation: the pinned preimage blob must be on disk (or the postimage, which equals it
when nothing is APPLY), every owner anchor must occur exactly once, every census text must be absent from the owner,
and with --donor-git every census text must occur in the pinned crate postimage blob(s). EDITS.tsv (rule, path,
verdict, crates, before blob, before sha256, after sha256, owner text, census text, evidence) is regenerated and is a
fixed point; a second run prints changed=0 plan=UNCHANGED.

--whatif OUT writes the owner with the census text of the selected rules (--rules R1,R2 or all) to OUT, which must lie
outside the repository: the differential compiles that copy as the counterfactual "census hunk applied" side.

usage: python -I apply.py [--root DIR] [--check] [--donor-git DIR --donor-rev REV] [--whatif OUT [--rules LIST]]
exit: 0 ok; 1 --check found a change; 2 refused (drift, anchor count, census text in owner, donor mismatch)
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v1"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "cf0429ac1d49c99371258e4de943d0c74e150711"
TARGET = "src/java.base/share/classes/java/util/CollectionViews.java"
PRE_BLOB = "4a1fcb4ea517ad7dbb78631e819077dc97993833"
PLAN_NAME = "EDITS.tsv"
PLAN_COLUMNS = ("rule", "path", "verdict", "crates", "before_blob", "before_sha256", "after_sha256",
                "owner_text", "census_text", "evidence")
DONOR_TSV = "DONOR.tsv"   # crate, donor path, pinned git blob (a TSV ledger: the JDK naming recipe keeps donor paths)
CROWN = "collectionviews-current-owner"
CCOW = "m3-collectionviews-onepass-cow-20261023"
OWNER_DISPATCH = ("eb55f07a82c (2026-10-27 'receive CollectionViews.java convergence output') and b9533511bdb "
                  "(2026-10-28 PR #2055) replaced the getClass() dispatch that 29d78c3564c (2026-10-23) had landed from "
                  "crate " + CCOW + "; the owner keeps the indexed-removal fallback, so CopyOnWrite subclasses keep "
                  "atomic bulk removal; the crates' product baseline b032048eece5 (2026-10-23) predates both")

# rule, verdict, crates, owner text, census text, evidence. Texts are whole lines (LF), anchored exactly once.
RULES = (
    ("R1", "SUPERSEDED", CROWN + "," + CCOW,
     "            if (backing instanceof java.util.concurrent.CopyOnWriteArrayList<?>\n",
     "            if (backing.getClass() == java.util.concurrent.CopyOnWriteArrayList.class\n",
     OWNER_DISPATCH),
    ("R2", "SUPERSEDED", CROWN + "," + CCOW,
     "                    || backing instanceof java.util.concurrent.CopyOnWriteArraySet<?>) {\n",
     "                    || backing.getClass() == java.util.concurrent.CopyOnWriteArraySet.class) {\n",
     OWNER_DISPATCH),
    ("R3", "FORMAT_EQUIVALENT", CROWN,
     "                    Spliterator.DISTINCT | Spliterator.ORDERED | Spliterator.SORTED,\n",
     "                    Spliterator.DISTINCT | Spliterator.SORTED | Spliterator.ORDERED,\n",
     "operand order of a constant | expression (0x15 either way)"),
    ("R4", "FORMAT_EQUIVALENT", CROWN,
     "            return prefix == null ? null\n"
     "                    : new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator);\n",
     "            return prefix == null ? null : new SelectionSpliterator<>(prefix, predicate, guaranteed, ownerComparator);\n",
     "line wrap only (owner wraps at 100 columns)"),
    ("R5", "FORMAT_EQUIVALENT", CROWN,
     "            return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE))\n"
     "                    | guaranteed;\n",
     "            return (source.characteristics() & (ORDERED | DISTINCT | SORTED | NONNULL | IMMUTABLE)) | guaranteed;\n",
     "line wrap only (owner wraps at 100 columns)"),
)
# the comment lines the crates carry next to R1/R2 (census-trivial; carried by --whatif so the copy is the crate text)
WHATIF_COMMENTS = (
    ("            // Snapshot iterators refuse removal even for CopyOnWrite subclasses.\n"
     "            // Ask the existing owner to perform its atomic bulk mutation.\n",
     "            // These exact owners publish bulk changes without mutating snapshot iterators.\n"
     "            // Subclasses retain their existing iterator dispatch and override contracts.\n"),)


class Refused(Exception):
    pass


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def git(repo: Path, *args: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(repo), *args], check=True, capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("git %s failed in %s (%s)" % (" ".join(args), repo, failure)) from failure


def once(text: str, anchor: str, what: str) -> None:
    count = text.count(anchor)
    if count != 1:
        raise Refused("%s occurs %d times (need 1): %r" % (what, count, anchor[:70]))


def replace(text: str, rules) -> str:
    for rule, _verdict, _crates, owner, census, _evidence in rules:
        once(text, owner, rule + " owner text")
        text = text.replace(owner, census)
    return text


def crate_blobs() -> dict:
    rows = [line.split("\t") for line in (RECIPE_DIR / DONOR_TSV).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith(("#", "crate\t"))]
    return {crate: (path, blob) for crate, path, blob in rows}


def check_donor(donor: Path, rev: str) -> int:
    texts = {}
    for crate, (path, blob) in crate_blobs().items():
        found = git(donor, "rev-parse", rev + ":" + path).decode().strip()
        if found != blob:
            raise Refused("crate %s postimage at %s is blob %s, pinned %s" % (crate, rev[:12], found, blob))
        texts[crate] = git(donor, "cat-file", "blob", blob).decode("utf-8").replace("\r\n", "\n")
    for rule, _verdict, crates, _owner, census, _evidence in RULES:
        for crate in crates.split(","):
            once(texts[crate], census, rule + " census text in crate " + crate)
    return len(texts)


def run(root: Path, check: bool, donor: Path | None, rev: str | None) -> int:
    target = root / TARGET
    on_disk = target.read_bytes()
    if blob_sha(on_disk) == PRE_BLOB:
        pre = on_disk
    else:
        pre = git(root, "cat-file", "blob", PRE_BLOB)
    text = pre.decode("utf-8")
    applied = [rule for rule in RULES if rule[1] == "APPLY"]
    for rule, verdict, _crates, owner, census, _evidence in RULES:
        once(text, owner, rule + " owner text")
        if census in text:
            raise Refused(rule + " census text already in the owner preimage")
        if verdict not in ("APPLY", "SUPERSEDED", "FORMAT_EQUIVALENT"):
            raise Refused(rule + " unknown verdict " + verdict)
    post = replace(text, applied).encode("utf-8")
    if on_disk not in (pre, post):
        raise Refused("drifted preimage %s: on disk blob %s, pinned blob %s" % (TARGET, blob_sha(on_disk), PRE_BLOB))
    donors = check_donor(donor, rev) if donor else 0
    state = "UNCHANGED" if on_disk == post else "CHANGED"
    if state == "CHANGED" and not check:
        target.write_bytes(post)
    rows = []
    for rule, verdict, crates, owner, census, evidence in RULES:
        print("%s %s %s" % (rule, verdict, "APPLIED" if verdict == "APPLY" else "NOT_APPLIED"))
        rows.append((rule, TARGET, verdict, crates, PRE_BLOB, sha256(pre), sha256(post),
                     owner.rstrip("\n").replace("\n", "\\n").strip(), census.rstrip("\n").replace("\n", "\\n").strip(),
                     evidence))
    plan = "\n".join(["\t".join(PLAN_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    plan_path = RECIPE_DIR / PLAN_NAME
    plan_state = not plan_path.exists() or plan_path.read_bytes() != plan.encode("utf-8")
    if plan_state and not check:
        plan_path.write_bytes(plan.encode("utf-8"))
    changed = int(state == "CHANGED")
    print("JBCOLL recipe=%s %s base=%s target=%s rules=%d apply=%d superseded=%d format_equivalent=%d donor_blobs=%d "
          "changed=%d plan=%s" % (RECIPE, VERSION, BASE_COMMIT[:12], TARGET, len(RULES), len(applied),
                                  sum(r[1] == "SUPERSEDED" for r in RULES),
                                  sum(r[1] == "FORMAT_EQUIVALENT" for r in RULES), donors, changed,
                                  "CHANGED" if plan_state else "UNCHANGED"))
    return 1 if check and (changed or plan_state) else 0


def whatif(root: Path, out: Path, selected: str) -> int:
    if out.resolve().is_relative_to(root.resolve()):
        raise Refused("--whatif output must lie outside the repository: " + str(out))
    pre = git(root, "cat-file", "blob", PRE_BLOB).decode("utf-8")
    names = {r[0] for r in RULES} if selected == "all" else set(selected.split(","))
    rules = [r for r in RULES if r[0] in names]
    if len(rules) != len(names):
        raise Refused("unknown rule in --rules " + selected)
    text = replace(pre, rules)
    if names & {"R1", "R2"}:
        for owner, census in WHATIF_COMMENTS:
            once(text, owner, "whatif comment")
            text = text.replace(owner, census)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(text.encode("utf-8"))
    print("JBCOLL whatif rules=%s out=%s sha256=%s" % (",".join(sorted(names)), out, sha256(text.encode("utf-8"))))
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[2]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--donor-git", help="com.synexia clone holding the crate postimage blobs")
    parser.add_argument("--donor-rev", help="com.synexia revision of the census pin")
    parser.add_argument("--whatif", help="write the counterfactual owner (census text of --rules) to this path")
    parser.add_argument("--rules", default="all", help="rules for --whatif: all or a comma list (R1,R2)")
    args = parser.parse_args(argv)
    try:
        root = Path(args.root).resolve()
        if args.whatif:
            return whatif(root, Path(args.whatif), args.rules)
        if bool(args.donor_git) != bool(args.donor_rev):
            raise Refused("--donor-git and --donor-rev go together")
        return run(root, args.check, Path(args.donor_git) if args.donor_git else None, args.donor_rev)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
