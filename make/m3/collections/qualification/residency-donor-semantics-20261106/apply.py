#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe RESIDENCY_DONOR_ATOMS v3: the remaining residency atoms of crate m3-doop-reuse-20261030 on M3ObjectResidency.

v2 = qualification/residency-victim-progress-20261106/apply.py v1 (anchored "sub" edits, every anchor exactly once,
LF payloads re-joined with the preimage's line ending, every preimage verified before the first write, EDITS.tsv
fixed point) plus serial leaves: the first rule pins the git blob of its preimage (the P1 postimage b7a83aa92326),
each later rule starts from the previous rule's postimage. The target must equal the preimage or one of the
postimages, anything else refuses (exit 2) without writing. --leaf applies the rules up to that leaf (one commit per
group); --check writes nothing and exits 1 when a run would change a file. Decisions: DECISION.tsv. Donor:
com.synexia develop ca1aafc4d94f (the census G7 pin).

Leaf A (ADOPT, crate step m3-doop-callback-error-20261030, donor blob 45caaf707b47, as composed in the
publication-budget postimage 8966285b99e2): an Error thrown by the factory/read/decode/spill-delete callbacks of
ensureResident() (or by the L066 re-check between them) or by the encode/write callbacks of evict() restores the
slot's prior state before it propagates; master left CREATING/RESTORING/SWAPPING behind for good and the slot refused
every later call.
Leaf CD (ADOPT, the crate atom rebindUnborrowedProjection of the publication-budget postimage, under the owner's API
name reuseUnborrowedProjection; NEC: the two members were never declared equivalent, this rule replaces the owner
body): refuses without LAZY_CREATE, under OBSERVE_ONLY and for a pinned, active or borrowed destination; both slots
CREATING and the source resident, counted and active-pinned while the resetter runs; the budget is re-checked after
it (group D: master's detach-first order let a reentrant resetter publish over the budget, ReentrantBudgetWitness
residentBytes=20 at budget 16); runtime exceptions and errors keep their identity; a failure discards the tainted
shell and records a decision. Owner strings are kept (IllegalArgumentException and ResidencyException messages, the
success reason).
Leaf PS (v3, ADOPT, residency fold 20261107, crate m3-doop-reuse-20261030 enhancement step
m3-doop-pressure-sampling-20261030, received by M3JDK as 4e229782bf1 (PR #2157, postimage blob e8347cd7780d) and lost by
the retain merge 7c6ddb6b7aa): the four budget boundaries (admission, enforceBudget, both reuse budget checks) call
targetBudget(), which takes HeapSnapshot.current() only when PRESSURE_AWARE is set; fixed-budget owners no longer
build a snapshot they ignore. Pressure-aware owners sample at the same boundaries in the same order. Decision:
../residency-fold-20261107/DECISION.tsv.

usage: python -I apply.py [--root DIR] [--check] [--leaf A|CD|PS]
"""
from __future__ import annotations

import argparse
import hashlib
import subprocess
import sys
from pathlib import Path

RECIPE = "apply.py"
VERSION = "v3"
RECIPE_DIR = Path(__file__).resolve().parent
BASE_COMMIT = "d97c5603f21"  # the P1 head (PR #2610); preimage blob b7a83aa92326
DONOR_COMMIT = "ca1aafc4d94f9dd1ee288226dcca070cc60991c9"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "leaf", "crate", "donor_blobs", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
DOOP = "m3-doop-reuse-20261030"
ANCHOR_A = ("    //FLLDCIM: MRL - residency donor semantics P2 group A, crate " + DOOP
            + " (m3-doop-callback-error-20261030, donor blob 45caaf707b47): ")
ANCHOR_CD = ("    //FLLDCIM: MRL - residency donor semantics P2 groups C+D, crate " + DOOP
             + " (rebindUnborrowedProjection of m3-doop-publication-budget-20261030, donor blob 8966285b99e2): ")
ANCHOR_PS = ("    //FLLDCIM: MRL - residency fold leaf PS, crate " + DOOP
             + " (m3-doop-pressure-sampling-20261030, M3JDK 4e229782bf1 blob e8347cd7780d): ")
PRE = "b7a83aa92326eb7a06df9b7d7d06a458b725d472"


class Refused(Exception):
    pass


# leaf, path, preimage blob (None: the previous rule's postimage), crate, donor blobs, rule note, edits [(old, new)]
RULES = (
    ("A", SRC + "M3ObjectResidency.java", PRE, DOOP,
     "post=45caaf707b47636b565311a960bff90c60450895 composed=8966285b99e21840bed497e9e3d6434cb327ab68",
     "ADOPT m3-doop-callback-error-20261030: catch (Error) restores the prior slot state around the"
     " ensureResident callbacks (factory/read/decode, the L066 re-check, spill delete) and around evict's"
     " encode/write", [
         ('            T value;\n'
           '            if (before == State.SWAPPED) {\n'
           '                setState(slot, State.RESTORING);\n'
           '                byte[] bytes = spill.read(slot);\n'
           '                value = codec.decode(slot, bytes);\n'
           '                Objects.requireNonNull(value, "decoded projection");\n'
           '            } else {\n'
           '                if (factory == null) throw new IllegalStateException("no factory for absent slot");\n'
           '                setState(slot, State.CREATING);\n'
           '                value = Objects.requireNonNull(factory.create(slot), "created projection");\n'
           '            }\n'
           '            // Factory/read/decode callbacks may consume the originally admitted capacity.\n'
           '            if ((flags & OBSERVE_ONLY) == 0 && wouldExceed(estimatedBytes, target)) {\n'
           '                throw new BudgetExceededException(estimatedBytes, residentBytes, target);\n'
           '            }\n'
           '            if (before == State.SWAPPED) spill.delete(slot);\n'
           '            resident[slot] = value;\n',
          '            T value;\n'
           '            try {\n'
           '                if (before == State.SWAPPED) {\n'
           '                    setState(slot, State.RESTORING);\n'
           '                    byte[] bytes = spill.read(slot);\n'
           '                    value = codec.decode(slot, bytes);\n'
           '                    Objects.requireNonNull(value, "decoded projection");\n'
           '                } else {\n'
           '                    if (factory == null) throw new IllegalStateException("no factory for absent slot");\n'
           '                    setState(slot, State.CREATING);\n'
           '                    value = Objects.requireNonNull(factory.create(slot), "created projection");\n'
           '                }\n'
           '                // Factory/read/decode callbacks may consume the originally admitted capacity.\n'
           '                if ((flags & OBSERVE_ONLY) == 0 && wouldExceed(estimatedBytes, target)) {\n'
           '                    throw new BudgetExceededException(estimatedBytes, residentBytes, target);\n'
           '                }\n'
           '                if (before == State.SWAPPED) spill.delete(slot);\n'
           '            } catch (Error failure) {\n'
           '                // Callbacks have not published a resident value or changed its accounting.\n'
           '                setState(slot, before);\n'
           '                throw failure;\n'
           '            }\n'
           '            resident[slot] = value;\n'),
         ('                byte[] snapshot = Objects.requireNonNull(codec.encode(typed), "encoded snapshot");\n'
           '                spill.write(slot, snapshot);\n',
          '                try {\n'
           '                    byte[] snapshot = Objects.requireNonNull(codec.encode(typed), "encoded snapshot");\n'
           '                    spill.write(slot, snapshot);\n'
           '                } catch (Error failure) {\n'
           '                    // Encode/write failed before detachment: retain the original resident.\n'
           '                    setState(slot, before);\n'
           '                    throw failure;\n'
           '                }\n'),
         ('    private void ensureResident(int slot, long estimatedBytes) {\n',
          ANCHOR_A + 'an Error from a callback restores the prior state\n'
           '    private void ensureResident(int slot, long estimatedBytes) {\n'),
         ('    private void evict(int slot) {\n',
          ANCHOR_A + 'an Error from encode/write retains the resident\n'
           '    private void evict(int slot) {\n'),
     ]),
    ("CD", SRC + "M3ObjectResidency.java", None, DOOP,
     "post=8966285b99e21840bed497e9e3d6434cb327ab68 (rebindUnborrowedProjection, discardFailedRebind)",
     "ADOPT the crate rebind atom in reuseUnborrowedProjection: guards, CREATING source during the reset,"
     " budget re-check after it, exception identity, tainted-shell discard (owner API name and strings kept)", [
         ('     * <p>On reset failure, both slots become absent. The possibly mutated instance is\n'
           '     * dropped for ordinary GC rather than returned to the original owner.\n',
          '     * <p>Requires LAZY_CREATE (the source becomes ABSENT and must be recreatable from its parent\n'
           '     * state); refused, without side effects, under OBSERVE_ONLY and for a pinned, active or\n'
           '     * borrowed destination. While the resetter runs, both slots are CREATING and the source stays\n'
           '     * resident, counted and pinned; the budget is checked again after the resetter, so a\n'
           '     * reentrant resetter cannot spend the destination budget twice.\n'
           '     *\n'
           '     * <p>On reset failure, both slots become absent. The possibly mutated instance is\n'
           '     * dropped for ordinary GC rather than returned to the original owner. Runtime exceptions\n'
           '     * and errors of the resetter propagate unchanged; checked exceptions are wrapped in\n'
           '     * ResidencyException.\n'),
         ('        if (source == target\n'
           '                || (flags & (RECREATE | STRICT_NO_ESCAPE)) != (RECREATE | STRICT_NO_ESCAPE)\n'
           '                || (flags & SWAP_SECONDARY) != 0\n'
           '                || callerInstalled[source] || callerInstalled[target]\n'
           '                || active[source] != 0 || borrowers[source] != 0\n'
           '                || (states[source] & SLOT_PINNED) != 0\n'
           '                || stateAt(source) != State.RESIDENT || stateAt(target) != State.ABSENT) {\n'
           '            return false;\n'
           '        }\n'
           '        long targetBudget = targetBudget(HeapSnapshot.current());\n'
           '        long bytesWithoutSource = residentBytes - weights[source];\n'
           '        if (estimatedBytes > targetBudget\n'
           '                || bytesWithoutSource > targetBudget - estimatedBytes) {\n'
           '            return false;\n'
           '        }\n'
           '        @SuppressWarnings("unchecked") T projection = (T) resident[source];\n'
           '        if (projection == null) return false;\n'
           '        resident[source] = null;\n'
           '        unlinkLru(source);\n'
           '        residentBytes = bytesWithoutSource;\n'
           '        weights[source] = 0L;\n'
           '        setState(source, State.SWAPPING);\n'
           '        setState(target, State.CREATING);\n'
           '        try {\n'
           '            reset.reinitialize(projection, target);\n'
           '            resident[target] = projection;\n'
           '            weights[target] = estimatedBytes;\n'
           '            residentBytes = Math.addExact(residentBytes, estimatedBytes);\n'
           '            setState(target, State.RESIDENT);\n'
           '            touch(target);\n'
           '            decision(target, State.ABSENT, State.RESIDENT, "reused unborrowed projection");\n'
           '            return true;\n'
           '        } catch (Exception failure) {\n'
           '            throw new ResidencyException("projection reset failed for target slot " + target, failure);\n'
           '        } finally {\n'
           '            setState(source, State.ABSENT);\n'
           '            if (stateAt(target) != State.RESIDENT) {\n'
           '                resident[target] = null;\n'
           '                weights[target] = 0L;\n'
           '                setState(target, State.ABSENT);\n'
           '            }\n'
           '        }\n'
           '    }\n',
          '        if (source == target\n'
           '                || (flags & (LAZY_CREATE | RECREATE | STRICT_NO_ESCAPE))\n'
           '                        != (LAZY_CREATE | RECREATE | STRICT_NO_ESCAPE)\n'
           '                || (flags & (SWAP_SECONDARY | OBSERVE_ONLY)) != 0\n'
           '                || stateAt(source) != State.RESIDENT || stateAt(target) != State.ABSENT\n'
           '                || callerInstalled[source] || callerInstalled[target]\n'
           '                || (states[source] & SLOT_PINNED) != 0 || (states[target] & SLOT_PINNED) != 0\n'
           '                || active[source] != 0 || borrowers[source] != 0\n'
           '                || active[target] != 0 || borrowers[target] != 0) {\n'
           '            return false;\n'
           '        }\n'
           '        long oldBytes = weights[source];\n'
           '        long others = residentBytes - oldBytes;\n'
           '        long budget = targetBudget(HeapSnapshot.current());\n'
           '        if (estimatedBytes > budget || others > budget - estimatedBytes) return false;\n'
           '\n'
           '        @SuppressWarnings("unchecked") T projection = (T) resident[source];\n'
           '        active[source] = 1;\n'
           '        setState(source, State.CREATING);\n'
           '        setState(target, State.CREATING);\n'
           '        try {\n'
           '            reset.reinitialize(projection, target);\n'
           '            // The callback may reenter unrelated slots or change memory pressure.\n'
           '            others = residentBytes - oldBytes;\n'
           '            budget = targetBudget(HeapSnapshot.current());\n'
           '            if (estimatedBytes > budget || others > budget - estimatedBytes) {\n'
           '                throw new BudgetExceededException(estimatedBytes, others, budget);\n'
           '            }\n'
           '        } catch (Exception failure) {\n'
           '            discardFailedRebind(source, target);\n'
           '            if (failure instanceof RuntimeException runtime) throw runtime;\n'
           '            throw new ResidencyException("projection reset failed for target slot " + target, failure);\n'
           '        } catch (Error failure) {\n'
           '            discardFailedRebind(source, target);\n'
           '            throw failure;\n'
           '        }\n'
           '\n'
           '        active[source] = 0;\n'
           '        resident[source] = null;\n'
           '        unlinkLru(source);\n'
           '        residentBytes -= oldBytes;\n'
           '        weights[source] = 0L;\n'
           '        setState(source, State.ABSENT);\n'
           '        resident[target] = projection;\n'
           '        weights[target] = estimatedBytes;\n'
           '        residentBytes += estimatedBytes;\n'
           '        setState(target, State.RESIDENT);\n'
           '        touch(target);\n'
           '        decision(target, State.ABSENT, State.RESIDENT, "reused unborrowed projection");\n'
           '        return true;\n'
           '    }\n'
           '\n'
           '    /** Reset may have partially mutated its input: discard rather than expose it. */\n'
           '    private void discardFailedRebind(int source, int target) {\n'
           '        active[source] = 0;\n'
           '        resident[source] = null;\n'
           '        unlinkLru(source);\n'
           '        residentBytes -= weights[source];\n'
           '        weights[source] = 0L;\n'
           '        setState(source, State.ABSENT);\n'
           '        setState(target, State.ABSENT);\n'
           '        decision(source, State.RESIDENT, State.ABSENT, "failed reset, discard tainted shell");\n'
           '    }\n'),
         ('    public synchronized boolean reuseUnborrowedProjection(int source, int target,\n',
          ANCHOR_CD + 'the donor rebind semantics under the owner name\n'
           '    public synchronized boolean reuseUnborrowedProjection(int source, int target,\n'),
     ]),
    ("PS", SRC + "M3ObjectResidency.java", None, DOOP,
     "post=e8347cd7780dbeb5a95759ef734a6dbf942d9edd (M3JDK 4e229782bf1, PR #2157) canonical=com.synexia"
     " 00bdbea3ee2fe0a7fc8a991f39e5dad2a0e8d604",
     "ADOPT m3-doop-pressure-sampling-20261030: targetBudget() samples HeapSnapshot.current() only with"
     " PRESSURE_AWARE; the four budget boundaries call it without building an ignored snapshot", [
         ('        long budget = targetBudget(HeapSnapshot.current());\n',
          '        long budget = targetBudget();\n'),
         ('            budget = targetBudget(HeapSnapshot.current());\n',
          '            budget = targetBudget();\n'),
         ('        try {\n'
          '            long target = targetBudget(HeapSnapshot.current());\n',
          '        try {\n'
          '            long target = targetBudget();\n'),
         ('    private void enforceBudget(int requestedSlot, long incomingBytes) {\n'
          '        long target = targetBudget(HeapSnapshot.current());\n',
          '    private void enforceBudget(int requestedSlot, long incomingBytes) {\n'
          '        long target = targetBudget();\n'),
         ('    private long targetBudget(HeapSnapshot heap) {\n'
          '        long target = maxResidentBytes;\n'
          '        if ((flags & PRESSURE_AWARE) != 0) {\n',
          ANCHOR_PS + 'no heap snapshot without PRESSURE_AWARE\n'
          '    private long targetBudget() {\n'
          '        long target = maxResidentBytes;\n'
          '        if ((flags & PRESSURE_AWARE) != 0) {\n'
          '            HeapSnapshot heap = HeapSnapshot.current();\n'),
     ]),
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
        raise Refused("preimage blob %s is not in this repository (%s)" % (blob, failure)) from failure


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


def run(root: Path, check: bool, leaf: str | None) -> int:
    """Serial rules on one file: rule 1 pins a git blob, each later rule starts from the previous postimage."""
    rows, chain, target_rel, changed = [], [], None, 0
    for rleaf, rel, blob, crate, donors, note, edits in RULES:
        if blob is not None:
            target_rel = rel
            if not (root / rel).exists():
                raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
            on_disk = (root / rel).read_bytes()
            pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        else:
            pre = chain[-1]
        post = postimage(pre, edits, rel)
        chain.append(post)
        rows.append((rel, sha256(pre), sha256(post), rleaf, crate, donors, note))
    on_disk = (root / target_rel).read_bytes()
    states = [rows[0][1]] + [row[2] for row in rows]  # sha256 of the preimage, then of every postimage in order
    if sha256(on_disk) not in states:
        raise Refused("drifted preimage %s: on disk %s is neither the pinned preimage nor a rule postimage" % (
            target_rel, blob_sha(on_disk)))
    reached = states.index(sha256(on_disk))
    wanted = len(RULES) if leaf is None else [rule[0] for rule in RULES].index(leaf) + 1
    for i, rule in enumerate(RULES):
        state = "UNCHANGED" if i < reached else ("CHANGED" if i < wanted else "PENDING")
        print("EDIT %s %s %s post_blob=%s" % (rule[0], state, target_rel, blob_sha(chain[i])[:12]))
    if wanted > reached:
        changed = 1
        if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
            (root / target_rel).write_bytes(chain[wanted - 1])
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("RESIDENCY_DONOR_ATOMS recipe=%s %s base=%s donor=%s rules=%d applied=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, DONOR_COMMIT[:12], len(rows), max(reached, wanted), changed,
        "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[4]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    parser.add_argument("--leaf", choices=[rule[0] for rule in RULES], help="apply the rules up to this leaf only")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check, args.leaf)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
