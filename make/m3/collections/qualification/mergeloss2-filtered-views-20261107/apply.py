#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe MERGELOSS2_FILTERED_VIEWS v1: the RESTORE rows of merge-loss audit phase 2 (filtered views family).

Merge-loss audit phase 2 (S:/m3-scratch/eb-conv/mergeloss2, CLEAN_0E.tsv rows 1 4 9 10 11 13 16 17 19 27 35 42 43 51
52 56 57 61 63 64 65 66; 508 lost lines). DECISION.tsv classifies every row; this recipe applies the four rows with
positive evidence (the origin commit's own test fails on master 33541216b2cd and passes on the restored owner):
  leaf 42 (origin 793988c0c6a, retain merge 082b54fa4ab7 #1368): M3FilteredViews deep descending iteration;
  leaf 27 (origin 01952e935de, retain merge 1633da209622 #1619): M3FilteredViews sorted-owner membership (Java part);
  leaf 43 (origin d1b9aa9f35c, retain merge 92805fd23aed #1616): M3FilteredLists reverse provider probe + empty add;
  leaf 17 (origin 3538a25a194, retain merge 6d0e9f5461e6 #1522): M3KeyedParallelismPlan fast-admission failure check;
plus the origin tests of those rows (CREATE from the retained-variant blobs master carries under
m3/qualification/master-landing-20261028/retained-variants/, already JDK-named) and one reference-model test.
Derived from make/m3/collections/qualification/census-small-batch-20261106/apply.py v1 (anchored "sub" edits, every anchor
exactly once, LF payloads re-joined with the preimage's line ending) with the rule chaining and CREATE kind of
make/m3/collections/qualification/mergeloss-01-15-20261106/apply.py: rules on one path chain (the second starts from the
first's postimage). Every EDIT rule names the git blob sha of its preimage on master 33541216b2cd; a target must equal
the preimage (the rules are applied) or the final postimage (already applied, no-op); anything else is drift and the
recipe refuses (exit 2) without writing. A CREATE target must be absent or equal its postimage; a CREATE with an
empty donor writes the payload of the recipe's own oracle/ file (pinned by sha256). Idempotent: a second run reports
changed=0 and EDITS.tsv is a fixed point. --check writes nothing and exits 1 when a run would change a file. Each
rule records its hunk-scoped 3-way (base = origin^, theirs = origin, both JDK-named; ours = master; git merge-file).

usage: python -I apply.py [--root DIR] [--check]
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
BASE_COMMIT = "33541216b2cd84780f2163f3ffcd3563d9751bfe"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "origin", "dropping_merge", "three_way",
                 "donor_blobs", "rule")
MAIN = "m3/collections/src/main/java/jdk/m3/collections/"
TEST = "m3/collections/src/test/java/jdk/m3/collections/"
DIR = "qualification/mergeloss2-filtered-views-20261107"


def anchor(leaf: str, origin: str, pr: str, merge: str, what: str, indent: str = "    ") -> str:
    return ("%s//FLLDCIM: MRL - mergeloss2 leaf %s: %s restored from origin %s (PR %s), dropped by retain merge %s;"
            " recipe %s\n" % (indent, leaf, what, origin, pr, merge, DIR))


A42 = anchor("42", "793988c0c6a", "#1368", "082b54fa4ab7", "deep descending iteration", "        ")
A27 = anchor("27", "01952e935de", "#1619", "1633da209622", "sorted-owner membership")
A43 = anchor("43", "d1b9aa9f35c", "#1616", "92805fd23aed", "reverse provider probe and empty insertion",
             "            ")
A17 = anchor("17", "3538a25a194", "#1522", "6d0e9f5461e6", "fast-admission failure check")
RV = "m3/qualification/master-landing-20261028/retained-variants/"


class Refused(Exception):
    pass


# leaf, kind, path, preimage blob (EDIT) / donor blob (CREATE, "" = oracle file), origin, merge, 3-way, donor blobs,
# rule note, edits [(old, new)]
RULES = (
    ("42", "EDIT", MAIN + "M3FilteredViews.java", "3d4bef11d752e5e4b33ebc322d7342436a8802fa", "793988c0c6a",
     "082b54fa4ab7", "base=8f6fb1d25c7f theirs=40bde7b1fea3 ours=3d4bef11d752 conflicts=1",
     "origin post=40bde7b1fea3 pre=8f6fb1d25c7f",
     "ADD (3-way conflict: master wrapped the one-layer return in the owner-epoch check of #2641 b8fb8956aaac): the"
     " origin's private-chain peel verbatim; the one-layer branch keeps master's ownerChecked return; the peeled"
     " chain iterates through CollectionView, whose depth-sized plan carries the same owner check", [
         ("        @Override public Iterator<E> descendingIterator() {\n"
          "            M3TreeMap<?, ?> owner = sortedOwner(nav);\n"
          "            int expected = revisionOf(owner);\n"
          "            return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));\n"
          "        }\n",
          A42 +
          "        @Override public Iterator<E> descendingIterator() {\n"
          "            if (!(nav instanceof NavigableSetView<?>)) {\n"
          "                M3TreeMap<?, ?> owner = sortedOwner(nav);\n"
          "                int expected = revisionOf(owner);\n"
          "                return ownerChecked(owner, expected, filtered(nav.descendingSet().iterator(), predicate));\n"
          "            }\n"
          "            // Reuse the ascending iterator's depth-sized predicate plan, opening\n"
          "            // the original descending owner only after the private chain is peeled.\n"
          "            ArrayList<Predicate<? super E>> filters = new ArrayList<>();\n"
          "            NavigableSetView<E> layer = this;\n"
          "            NavigableSet<E> backing;\n"
          "            while (true) {\n"
          "                filters.add(layer.predicate);\n"
          "                backing = layer.nav;\n"
          "                if (!(backing instanceof NavigableSetView<?> nested)) break;\n"
          "                @SuppressWarnings(\"unchecked\")\n"
          "                NavigableSetView<E> inner = (NavigableSetView<E>) nested;\n"
          "                layer = inner;\n"
          "            }\n"
          "            Collection<E> descending = backing.descendingSet();\n"
          "            for (int index = filters.size() - 1; index >= 0; index--) {\n"
          "                descending = new CollectionView<>(descending, filters.get(index));\n"
          "            }\n"
          "            return descending.iterator();\n"
          "        }\n"),
     ]),
    ("27", "EDIT", MAIN + "M3FilteredViews.java", "3d4bef11d752e5e4b33ebc322d7342436a8802fa", "01952e935de",
     "1633da209622", "base=60c5f46aa03d theirs=d301fa0a6f6b ours=3d4bef11d752 conflicts=0 (does not compile:"
     " sortedOwner(Collection) overloads #2641's sortedOwner(Object))",
     "origin post=d301fa0a6f6b(JDK-named 2dac3e40eb72) pre=60c5f46aa03d",
     "MAPPED: the clean 3-way adds sortedOwner(Collection<E>), which the call sites of #2641's sortedOwner(Object)"
     " (M3TreeMap owner of a NavigableSet) would resolve to; the helper is restored as sortedMembershipOwner, body"
     " verbatim; visibleMember gains the origin's sorted branch verbatim (doc line merged); the C half"
     " (m3_admit_chunk) is a HANDOFF", [
         ("    /** Membership has already succeeded; preserve the original representative probe order. */\n"
          "    private static <E> boolean visibleMember(Collection<E> source,\n"
          "            Predicate<? super E> predicate, Object value) {\n",
          A27 +
          "    /**\n"
          "     * Resolve only our own private, identity-preserving view layers. The backing\n"
          "     * SortedSet owns comparator equivalence; equality-based alias scans do not.\n"
          "     * This is iterative to avoid recursive wrapper-depth growth.\n"
          "     */\n"
          "    @SuppressWarnings(\"unchecked\")\n"
          "    private static <E> SortedSet<E> sortedMembershipOwner(Collection<E> source) {\n"
          "        Collection<E> owner = source;\n"
          "        while (true) {\n"
          "            if (owner instanceof SortedSet<?> sorted) return (SortedSet<E>) sorted;\n"
          "            if (owner.getClass() == SetView.class) {\n"
          "                owner = ((SetView<E>) owner).view.source;\n"
          "            } else if (owner.getClass() == CollectionView.class) {\n"
          "                owner = ((CollectionView<E>) owner).source;\n"
          "            } else {\n"
          "                return null;\n"
          "            }\n"
          "        }\n"
          "    }\n\n"
          "    /**\n"
          "     * Membership has already succeeded: a sorted owner (directly or under private view layers)\n"
          "     * evaluates its retained representative; any other source keeps the original representative\n"
          "     * probe order.\n"
          "     */\n"
          "    private static <E> boolean visibleMember(Collection<E> source,\n"
          "            Predicate<? super E> predicate, Object value) {\n"
          "        SortedSet<E> sorted = sortedMembershipOwner(source);\n"
          "        if (sorted != null) {\n"
          "            @SuppressWarnings(\"unchecked\") E alias = (E) value;\n"
          "            if (sorted instanceof NavigableSet<?> nav) {\n"
          "                // ceiling uses the stored representative without a new tail-range view.\n"
          "                @SuppressWarnings(\"unchecked\") NavigableSet<E> navigable = (NavigableSet<E>) nav;\n"
          "                return predicate.test(navigable.ceiling(alias));\n"
          "            }\n"
          "            return predicate.test(sorted.tailSet(alias).first());\n"
          "        }\n"),
     ]),
    ("43", "EDIT", MAIN + "M3FilteredLists.java", "f50fa2459f66bfbdc30273052e070d43d1846e21", "d1b9aa9f35c",
     "92805fd23aed", "base=10c4d16cc9cd theirs=2e51db70400b ours=f50fa2459f66 conflicts=3",
     "origin post=2e51db70400b pre=10c4d16cc9cd",
     "ADD (3 conflicts: #2609 78956e8a9f5 census P2-043 MAPPED the older void probe of dataset"
     " m3jdk21-filtered-list-mutation-20261018 onto the owner's state mask): the origin's boolean probe (reverse"
     " direction, 'delivered element disappeared') and empty-provider insertion verbatim, 'failed = true' mapped to"
     " the owner's 'state |= FAILED'; the P2-043 anchor and set/remove calls kept; import added", [
         ("import java.util.Comparator;\n",
          "import java.util.Comparator;\nimport java.util.ConcurrentModificationException;\n"),
         ("            /** Validate the original cursor without changing its physical position.\n",
          A43 + "            /** Validate the original cursor without changing its physical position.\n"),
         ("             * unmodifiable/snapshot iterator. next() also checks an exhausted\n"
          "             * fail-fast cursor; hasNext() alone need not detect invalidation.\n"
          "             */\n"
          "            private void checkForComodification() {\n"
          "                try {\n"
          "                    try {\n"
          "                        iterator.next();\n"
          "                    } catch (NoSuchElementException exhausted) {\n"
          "                        return;\n"
          "                    }\n"
          "                    iterator.previous();\n"
          "                } catch (RuntimeException | Error failure) {\n",
          "             * unmodifiable/snapshot iterator. next() also checks an exhausted\n"
          "             * fail-fast cursor; hasNext() alone need not detect invalidation.\n"
          "             * A bounded provider can refuse next() at its end before checking\n"
          "             * structural generation, so probe the reverse direction as well.\n"
          "             * Return true only when neither direction has an element.\n"
          "             */\n"
          "            private boolean checkForComodification() {\n"
          "                try {\n"
          "                    try {\n"
          "                        iterator.next();\n"
          "                    } catch (NoSuchElementException exhausted) {\n"
          "                        try {\n"
          "                            iterator.previous();\n"
          "                        } catch (NoSuchElementException empty) {\n"
          "                            if (lastPhysical >= 0) {\n"
          "                                throw new ConcurrentModificationException(\"delivered element disappeared\");\n"
          "                            }\n"
          "                            return true;\n"
          "                        }\n"
          "                        iterator.next();\n"
          "                        return false;\n"
          "                    }\n"
          "                    iterator.previous();\n"
          "                    return false;\n"
          "                } catch (RuntimeException | Error failure) {\n"),
         ("                requireAccepted(value);\n"
          "                checkForComodification();\n"
          "                int position = iterator.nextIndex();\n"
          "                backing.add(position, value);\n",
          "                requireAccepted(value);\n"
          "                boolean empty = checkForComodification();\n"
          "                int position = iterator.nextIndex();\n"
          "                if (empty) {\n"
          "                    // Empty bounded iterators may bypass generation checks in both\n"
          "                    // traversal directions. Let their own insertion validate and write.\n"
          "                    // Snapshot iterators can forbid writes on an editable backing list.\n"
          "                    try {\n"
          "                        iterator.add(value);\n"
          "                    } catch (UnsupportedOperationException unsupported) {\n"
          "                        backing.add(position, value);\n"
          "                    } catch (ConcurrentModificationException invalidated) {\n"
          "                        state |= FAILED;\n"
          "                        throw invalidated;\n"
          "                    }\n"
          "                } else {\n"
          "                    backing.add(position, value);\n"
          "                }\n"),
     ]),
    ("17", "EDIT", MAIN + "M3KeyedParallelismPlan.java", "e355a6af7825e2fd6e36e5a0a24794ecd3b7bd92", "3538a25a194",
     "6d0e9f5461e6", "base=f48930e2c0b8 theirs=e800b5e3f3a1 ours=e355a6af7825 conflicts=0",
     "origin post=e800b5e3f3a1 pre=f48930e2c0b8",
     "APPLICABLE: the clean 3-way equals this edit plus the anchor (fast admission re-checks the published lane"
     " failure); the row's other files: M3FilteredViews/M3LinkedList/M3PrimitiveFilteredSequences PRESENT_EVOLVED,"
     " M3IndexedListSnapshot and m3_index_mask_compactor.c HANDOFF (DECISION.tsv)", [
         ("        if (queue.offer(value)) return;\n",
          A17 +
          "        if (queue.offer(value)) {\n"
          "            checkFailure(laneFailure.get());\n"
          "            return;\n"
          "        }\n"),
     ]),
    ("42", "CREATE", TEST + "M3FilteredDescendingDepthContract.java", "af3ffd244faa3131a48276f3bcd359e64846b679",
     "793988c0c6a", "082b54fa4ab7", "-", "retained pr1368 af3ffd244faa (origin cc8304d9335b)",
     "CREATE from the origin's own contract (absent on master; retained variant pr1368, JDK-named): verbatim plus"
     " one anchor line", [
         ("package jdk.m3.collections;\n",
          anchor("42", "793988c0c6a", "#1368", "082b54fa4ab7", "origin contract", "") + "package jdk.m3.collections;\n"),
     ]),
    ("42", "CREATE", TEST + "M3FilteredDescendingDepthTest.java", "9ad6d13ffd3a14be65a4d6a1ecc05fe749d70fc8",
     "793988c0c6a", "082b54fa4ab7", "-", "retained pr1368 9ad6d13ffd3a (origin bf82b669b59a)",
     "CREATE from the origin's own JUnit wrapper; fails on master (StackOverflowError at depth 100000)", [
         ("package jdk.m3.collections;\n",
          anchor("42", "793988c0c6a", "#1368", "082b54fa4ab7", "origin test", "") + "package jdk.m3.collections;\n"),
     ]),
    ("27", "CREATE", TEST + "M3FilteredSortedRepresentativeContract.java", "5064a7542d4abaa90404c44d2ed17822a4bc9de2",
     "01952e935de", "1633da209622", "-", "retained pr1619 5064a7542d4a (origin 3765db24ac1b)",
     "CREATE from the origin's own contract (absent on master; retained variant pr1619, JDK-named)", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin contract", "") + "package jdk.m3.collections;\n"),
     ]),
    ("27", "CREATE", TEST + "M3FilteredSortedRepresentativeTest.java", "1b054016835744c475d8363385dc7fe7c80a4314",
     "01952e935de", "1633da209622", "-", "retained pr1619 1b0540168357 (origin 1f5f9c4b3597)",
     "CREATE from the origin's own JUnit wrapper; fails on master ('retained comparator class lost')", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin test", "") + "package jdk.m3.collections;\n"),
     ]),
    ("27", "CREATE", TEST + "M3StringTreeConsumerContract.java", "5ec10599477a45fd66fe1702cf69192b9ef4463e",
     "01952e935de", "1633da209622", "-", "retained pr1619 5ec10599477a (origin 5993bc5cbef8)",
     "CREATE from the origin's own contract (absent on master; retained variant pr1619, JDK-named)", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin contract", "") + "package jdk.m3.collections;\n"),
     ]),
    ("27", "CREATE", TEST + "M3StringTreeConsumerTest.java", "a2757498e2dcb2944b99f41782cac5cbd4b23f19",
     "01952e935de", "1633da209622", "-", "retained pr1619 a2757498e2dc (origin 73cf017c0251)",
     "CREATE from the origin's own JUnit wrapper; fails on master ('actual M3TreeSet comparator alias lost by"
     " generic filter')", [
         ("package jdk.m3.collections;\n",
          anchor("27", "01952e935de", "#1619", "1633da209622", "origin test", "") + "package jdk.m3.collections;\n"),
     ]),
    ("43", "CREATE", TEST + "M3FilteredListProviderInvalidationTest.java", "20f57a394f6aa29e462f7b5f6a19c89cf1c34b7f",
     "d1b9aa9f35c", "92805fd23aed", "-", "retained pr1616 20f57a394f6a (origin 4e22f9c2044d)",
     "CREATE from the origin's own test (absent on master); 3 of 7 fail on master", [
         ("package jdk.m3.collections;\n",
          anchor("43", "d1b9aa9f35c", "#1616", "92805fd23aed", "origin test", "") + "package jdk.m3.collections;\n"),
     ]),
    ("17", "CREATE", TEST + "M3KeyedParallelismConvergenceTest.java", "a5b4a5c50ba03f23123425bc79b140b139936f95",
     "3538a25a194", "6d0e9f5461e6", "-", "retained pr1522 a5b4a5c50ba0 (origin 2756efa51e88)",
     "CREATE from the origin's own test (absent on master); publishedFailureInDrainingLaneStopsFastAdmission fails"
     " on master", [
         ("package jdk.m3.collections;\n",
          anchor("17", "3538a25a194", "#1522", "6d0e9f5461e6", "origin test", "") + "package jdk.m3.collections;\n"),
     ]),
    ("42+27+43", "CREATE", TEST + "M3MergelossFilteredViewsReferenceTest.java", "", "793988c0c6a+01952e935de+d1b9aa9f35c",
     "082b54fa4ab7+1633da209622+92805fd23aed", "-", "oracle/M3MergelossFilteredViewsReferenceTest.java",
     "CREATE reference model (this recipe): descendingIterator/last of filter chains over java.util.TreeMap"
     " navigableKeySet/subMap/headMap/tailMap/descendingMap key views and M3TreeSet views against the TreeMap view's"
     " own descending order and predicate tape; generic-filter contains against TreeSet membership + stored"
     " representative; M3FilteredLists cursor writes after the delivered element of a generation-free provider"
     " disappeared", []),
)


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def blob_sha(data: bytes) -> str:
    return hashlib.sha1(b"blob %d\0" % len(data) + data).hexdigest()


def eol_of(data: bytes) -> str:
    return "\r\n" if b"\r\n" in data else "\n"


def git_blob(root: Path, blob: str) -> bytes:
    try:
        return subprocess.run(["git", "-C", str(root), "cat-file", "blob", blob], check=True,
                              capture_output=True).stdout
    except (subprocess.CalledProcessError, OSError) as failure:
        raise Refused("pinned blob %s is not in this repository (%s)" % (blob, failure)) from failure


def postimage(pre: bytes, edits, path: str) -> bytes:
    eol = eol_of(pre)
    text = pre.decode("utf-8")
    for old, new in edits:
        old, new = old.replace("\n", eol), new.replace("\n", eol)
        count = text.count(old)
        if count != 1:
            raise Refused("anchor occurs %d times (need 1) in %s: %r" % (count, path, old[:60]))
        text = text.replace(old, new)
    return text.encode("utf-8")


ORACLE_SHA256 = {"oracle/M3MergelossFilteredViewsReferenceTest.java":
                 "386e77357d5fd9617fe98ab21fc520d0931bf827c09b57531aba8b39f88d58af"}


def donor(root: Path, blob: str, note: str) -> bytes:
    if blob:
        data = git_blob(root, blob)
        if blob_sha(data) != blob:
            raise Refused("donor blob %s does not hash to itself" % blob)
        return data
    data = (RECIPE_DIR / note).read_bytes()
    if sha256(data) != ORACLE_SHA256[note]:
        raise Refused("oracle payload %s drifted: sha256 %s" % (note, sha256(data)))
    return data


def chain(root: Path):
    """Pre/post image per rule; rules on one path chain (the second starts from the first's postimage)."""
    staged, plan = {}, []
    for leaf, kind, rel, blob, origin, merge, three, donors, note, edits in RULES:
        if kind == "CREATE":
            pre, before = donor(root, blob, donors), b""
        elif rel in staged:
            pre = before = staged[rel]
        else:
            pre = before = git_blob(root, blob)
            if blob_sha(pre) != blob:
                raise Refused("preimage blob %s does not hash to itself" % blob)
        post = postimage(pre, edits, rel)
        staged[rel] = post
        plan.append((leaf, kind, rel, before, post, origin, merge, three, donors, note))
    return plan, staged


def run(root: Path, check: bool) -> int:
    plan, finals = chain(root)
    firsts = {}
    for leaf, kind, rel, before, *_ in plan:
        firsts.setdefault(rel, (kind, before))
    writes, changed = [], 0
    for rel, final in finals.items():
        kind, first = firsts[rel]
        target = root / rel
        on_disk = target.read_bytes() if target.exists() else None
        if kind == "CREATE":
            if on_disk not in (None, final):
                raise Refused("CREATE target %s exists with other content (blob %s)" % (rel, blob_sha(on_disk)))
        elif on_disk is None:
            raise Refused("missing preimage %s" % rel)
        elif on_disk != final and on_disk != first:
            raise Refused("drifted preimage %s: on disk blob %s" % (rel, blob_sha(on_disk)))
        state = "UNCHANGED" if on_disk == final else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, final))
        print("FILE %s %s" % (state, rel))
    rows = [(rel, sha256(before) if before else "ABSENT", sha256(post), leaf, origin, merge, three, donors,
             kind + " " + note) for leaf, kind, rel, before, post, origin, merge, three, donors, note in plan]
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, data in writes:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("MERGELOSS2_FILTERED_VIEWS recipe=%s %s base=%s rules=%d files=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT[:12], len(rows), len(finals), changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
