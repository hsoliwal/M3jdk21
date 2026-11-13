#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""Recipe JBCOLL_ARRAYLIST_INLINE v1: census leaf L002 (java.base java/util/ArrayList.java + one new jtreg test).

Derived from make/m3/qualification/progress-monitor-unify-20261106/unify.py. Crate m3jdk21-java-base-arraylist-inline-
20261025 (census row UNAPPLIED, added_present=0/19; its preimage blob 885d891b0f81 IS the live owner blob, so the
3-way merge base = ours and the merge is the crate postimage). Two rules:

  EDIT  src/java.base/share/classes/java/util/ArrayList.java: three anchored substitutions inside
        removeIf(Predicate, int, int) (the crate hunk, generated from the pinned pre/post blobs). The result must be
        the crate postimage plus the one JDK naming rewrite NAMING (sha256 POST_SHA256), else the recipe refuses;
        --donor-git re-derives it from the crate blobs.
  NEW   test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java: the crate test, already jtreg-tagged
        (@test, @run main/othervm) with a plain main; written from the pinned donor blob (needs --donor-git once),
        afterwards only its sha256 is checked.

Hash-pinned: a target must equal the preimage (rule applied) or the postimage (already applied, no-op); anything else
is drift and the recipe refuses (exit 2) without writing. Every anchor must occur exactly once. PLAN.tsv (file, kind,
before blob, before sha256, after sha256, rule note) is a fixed point; a second run prints changed=0 plan=UNCHANGED.

--whatif OUT writes the owner postimage to OUT (outside the repository) without touching the tree.

usage: python -I apply.py [--root DIR] [--check] [--donor-git DIR --donor-rev REV] [--whatif OUT]
exit: 0 ok; 1 --check found a change; 2 refused
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
BASE_COMMIT = "af319ee82d00ff43fa0d4b161ab325df2863c199"
PLAN_NAME = "PLAN.tsv"
PLAN_COLUMNS = ("file", "kind", "before_blob", "before_sha256", "after_sha256", "rule")
DONOR_TSV = "DONOR.tsv"   # role, donor path, pinned git blob (a TSV ledger: the JDK naming recipe keeps donor paths)
OWNER = "src/java.base/share/classes/java/util/ArrayList.java"
PRE_BLOB = "885d891b0f817080824622d592691a27cf8ffa26"
CRATE_POST_SHA256 = "62d73bd98166651c82e55395bd1a6e7b68662ba2018bf0fa7fd418d0f973198a"
POST_SHA256 = "732324a2ad40629521e7253b57fde0715606fcfaebbb5caa030ed3614836835c"
# the JDK naming recipe (make/m3/tooling/jdk-naming, rule PROSE_PRODUCT_VERSION) puts a space between the product name and
# its version in the crate comment; the owner postimage is the crate postimage with exactly this rewrite (checked with
# --donor-git). The crate spelling is assembled so that the naming recipe does not rewrite this recipe's own anchor.
NAMING = (("            // M3JDK" + "21: inline the ephemeral", "            // M3JDK 21: inline the ephemeral"),)
TEST = "test/jdk/java/util/ArrayList/M3ArrayListInlineMaskTest.java"
TEST_SHA256 = "f4ed9e0a82a73254276b490ec969a1115442ce541d57fed1a7d4ea9b704f6980"

EDITS = (
        ("sub",
         (
             '            final int beg = i;\n'
             '            final long[] deathRow = nBits(end - beg);\n'
             '            deathRow[0] = 1L;   // set bit 0\n'
             '            for (i = beg + 1; i < end; i++)\n'
             '                if (filter.test(elementAt(es, i)))\n'
             '                    setBit(deathRow, i - beg);\n'),
         (
             '            final int beg = i;\n'
             '            // M3JDK 21: inline the ephemeral deletion decisions for up to two\n'
             '            // 64-bit words. Large ranges retain the original deathRow layout,\n'
             '            // compactor dispatch, and observable iteration/removal behavior.\n'
             '            final int span = end - beg;\n'
             '            final boolean inlineMask = span <= 2 * Long.SIZE;\n'
             '            final long[] deathRow = inlineMask ? null : nBits(span);\n'
             '            long deletionLow = 1L; // the first rejected element is ordinal 0\n'
             '            long deletionHigh = 0L;\n'
             '            if (!inlineMask) deathRow[0] = 1L;\n'
             '            for (i = beg + 1; i < end; i++) {\n'
             '                if (filter.test(elementAt(es, i))) {\n'
             '                    final int ordinal = i - beg;\n'
             '                    if (inlineMask) {\n'
             '                        if (ordinal < Long.SIZE) deletionLow |= 1L << ordinal;\n'
             '                        else deletionHigh |= 1L << (ordinal - Long.SIZE);\n'
             '                    } else {\n'
             '                        setBit(deathRow, ordinal);\n'
             '                    }\n'
             '                }\n'
             '            }\n')),
        ("sub",
         (
             '            int w = beg;\n'
             '            // M3JDK: use indexed deletion-mask runs only after predicate\n'
             '            // execution and the original fail-fast check have completed.\n'
             '            if (end - beg >= 512) {\n'),
         (
             '            int w = beg;\n'
             '            // Keep the large M3JDK compactor and its original bit layout.\n'
             '            if (!inlineMask && span >= 512) {\n')),
        ("sub",
         (
             '                w = M3CollectionMaskCompactor.compact(es, beg, end, deathRow);\n'),
         (
             '                w = M3CollectionMaskCompactor.compact(es, beg, end, deathRow);\n'
             '            } else if (inlineMask) {\n'
             '                for (i = beg; i < end; i++) {\n'
             '                    final int ordinal = i - beg;\n'
             '                    final long word = ordinal < Long.SIZE ? deletionLow : deletionHigh;\n'
             '                    if ((word & (1L << (ordinal & (Long.SIZE - 1)))) == 0)\n'
             '                        es[w++] = es[i];\n'
             '                }\n')),
)


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


def donor_rows() -> dict:
    rows = [line.split("\t") for line in (RECIPE_DIR / DONOR_TSV).read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith(("#", "role\t"))]
    return {role: (path, blob) for role, path, blob in rows}


def owner_post(pre: bytes) -> bytes:
    text = pre.decode("utf-8")
    for _mode, old, new in EDITS:
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, OWNER, old[:60]))
        text = text.replace(old, new)
    post = text.encode("utf-8")
    if sha256(post) != POST_SHA256:
        raise Refused("owner postimage sha256 %s, pinned %s (crate postimage + NAMING)" % (sha256(post), POST_SHA256))
    return post


def check_donor(donor: Path, rev: str, pre: bytes, post: bytes) -> bytes:
    rows = donor_rows()
    crate = {}
    for role in ("pre", "post"):
        path, blob = rows[role]
        found = git(donor, "rev-parse", rev + ":" + path).decode().strip()
        if found != blob:
            raise Refused("crate %s blob at %s is %s, pinned %s" % (role, rev[:12], found, blob))
        crate[role] = git(donor, "cat-file", "blob", blob)
    if crate["pre"] != pre:
        raise Refused("crate preimage is not the owner preimage blob " + PRE_BLOB)
    if sha256(crate["post"]) != CRATE_POST_SHA256:
        raise Refused("crate postimage sha256 %s, pinned %s" % (sha256(crate["post"]), CRATE_POST_SHA256))
    text = crate["post"].decode("utf-8")
    for old, new in NAMING:
        if text.count(old) != 1:
            raise Refused("NAMING anchor occurs %d times in the crate postimage: %r" % (text.count(old), old))
        text = text.replace(old, new)
    if text.encode("utf-8") != post:
        raise Refused("owner postimage is not crate postimage + NAMING")
    path, blob = rows["test"]
    found = git(donor, "rev-parse", rev + ":" + path).decode().strip()
    if found != blob:
        raise Refused("crate test blob at %s is %s, pinned %s" % (rev[:12], found, blob))
    test = git(donor, "cat-file", "blob", blob)
    if sha256(test) != TEST_SHA256:
        raise Refused("crate test sha256 %s, pinned %s" % (sha256(test), TEST_SHA256))
    return test


def run(root: Path, check: bool, donor: Path | None, rev: str | None) -> int:
    rows, changed = [], 0
    owner = root / OWNER
    on_disk = owner.read_bytes()
    pre = on_disk if blob_sha(on_disk) == PRE_BLOB else git(root, "cat-file", "blob", PRE_BLOB)
    post = owner_post(pre)
    if on_disk not in (pre, post):
        raise Refused("drifted preimage %s: on disk blob %s, pinned blob %s" % (OWNER, blob_sha(on_disk), PRE_BLOB))
    test_bytes = check_donor(donor, rev, pre, post) if donor else None
    state = "UNCHANGED" if on_disk == post else "CHANGED"
    if state == "CHANGED" and not check:
        owner.write_bytes(post)
    changed += state == "CHANGED"
    print("EDIT %s %s" % (state, OWNER))
    rows.append((OWNER, "EDIT", PRE_BLOB, sha256(pre), sha256(post),
                 "crate hunk, 3 anchored substitutions in removeIf(Predicate, int, int); result = crate postimage (sha256 62d73bd98166) + JDK naming PROSE_PRODUCT_VERSION"))
    test = root / TEST
    if test.exists():
        if sha256(test.read_bytes()) != TEST_SHA256:
            raise Refused("drifted %s: sha256 %s, pinned %s" % (TEST, sha256(test.read_bytes()), TEST_SHA256))
        tstate = "UNCHANGED"
    elif test_bytes is None:
        raise Refused("%s is absent: pass --donor-git/--donor-rev to write it" % TEST)
    else:
        tstate = "CHANGED"
        if not check:
            test.parent.mkdir(parents=True, exist_ok=True)
            test.write_bytes(test_bytes)
    changed += tstate == "CHANGED"
    print("NEW %s %s" % (tstate, TEST))
    rows.append((TEST, "NEW", "-", "-", TEST_SHA256, "crate test, jtreg @run main/othervm + plain main (no package)"))
    plan = "\n".join(["\t".join(PLAN_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    plan_path = RECIPE_DIR / PLAN_NAME
    plan_state = not plan_path.exists() or plan_path.read_bytes() != plan.encode("utf-8")
    if plan_state and not check:
        plan_path.write_bytes(plan.encode("utf-8"))
    print("JBCOLL recipe=%s %s base=%s rules=2 edits=%d changed=%d donor=%s plan=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(EDITS), changed, "VERIFIED" if donor else "-",
        "CHANGED" if plan_state else "UNCHANGED"))
    return 1 if check and (changed or plan_state) else 0


def whatif(root: Path, out: Path) -> int:
    if out.resolve().is_relative_to(root.resolve()):
        raise Refused("--whatif output must lie outside the repository: " + str(out))
    post = owner_post(git(root, "cat-file", "blob", PRE_BLOB))
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_bytes(post)
    print("JBCOLL whatif out=%s sha256=%s" % (out, sha256(post)))
    return 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: three up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--donor-git", help="com.synexia clone holding the crate blobs")
    parser.add_argument("--donor-rev", help="com.synexia revision of the census pin")
    parser.add_argument("--whatif", help="write the owner postimage to this path (outside the repository)")
    args = parser.parse_args(argv)
    try:
        root = Path(args.root).resolve()
        if args.whatif:
            return whatif(root, Path(args.whatif))
        if bool(args.donor_git) != bool(args.donor_rev):
            raise Refused("--donor-git and --donor-rev go together")
        return run(root, args.check, Path(args.donor_git) if args.donor_git else None, args.donor_rev)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
