#!/usr/bin/env python3
# Copyright 2026 Hitesh Soliwal and contributors
# SPDX-License-Identifier: Apache-2.0
"""Recipe HASH_REENTRANT_GUARD v1: re-entrant key/value callbacks on the hash slot-map owners (S1/S2 of #2676).

Measured first (M3HashReentrantLookupSelfTest report mode, java.util twins as the reference column; DECISION.tsv):
the base M3 hash owners return or overwrite the mapping that reused the located slot when a key's equals clears the
map and re-inserts (get/remove/put/replace... WRONG), erase or move a released slot (remove(6), access-order get(6):
DAMAGED), let a value's equals do the same in remove(k,v)/replace(k,old,new) (S2), insert a duplicate key when
M3WeakHashMap.put's second hashCode call inserts it (DAMAGED), and lose a racing insert in putIfAbsent/
computeIfAbsent/compute/merge, whose absent path looked the key up a second time through put. The java.util twins
answer reads with the pre-change value (linearized before the callback's change) and are themselves damaged in many
mutating shapes.

The guard therefore never fails where a linearizable answer exists (not stricter than the JDK):
  - a lookup walk whose key equals changed the structure walks again from the start (the answer is the one after the
    callback's change); after M3SlotMap.REWALKS disturbed walks it throws ConcurrentModificationException;
  - remove(k,v) and replace(k,old,new) look the key up again when the value's equals changed the structure;
  - putIfAbsent/computeIfAbsent/compute/merge look the key up once (locate: slot + key hash) and either write the
    present key's slot or insert the absent key by its hash (insertAbsent) without calling key code again, the
    structure unchanged since the walk (their existing revision check covers the user function in between): the
    same callback sequence as java.util.HashMap's single-walk compute; the base looked an absent key up a second
    time through put, whose key callbacks could insert it meanwhile (a lost racing insert);
  - M3WeakHashMap.put keeps its second hashCode call (pinned by M3FootprintContractTest: callback count and a
    failure in that call leaving a usable empty owner) but looks the key up again when that call changed the
    structure (bounded); the base inserted without looking again and could duplicate the key.
Hash computation before a walk needs no check: the walk that follows sees the changed structure.

Engine of qualification/treemap-aba-guard-20261107/apply.py: anchored "sub" edits, every anchor exactly once, LF
payloads re-joined with the preimage's line ending; each rule names the git blob of its preimage at the base commit;
a target must equal the preimage (applied) or the postimage (no-op), anything else is a drifted preimage and the
recipe refuses (exit 2) without writing. Idempotent: a second run reports changed=0 and EDITS.tsv is a fixed point.
--check writes nothing and exits 1 when a run would change a file.

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
BASE_COMMIT = "784f2c20e580"
EDITS_NAME = "EDITS.tsv"
EDITS_COLUMNS = ("file", "before_sha256", "after_sha256", "behaviour", "rule")
SRC = "m3/collections/src/main/java/jdk/m3/collections/"
ANCHOR = ("//FLLDCIM: MRL - recipe HASH_REENTRANT_GUARD v1 (qualification/hash-reentrant-guard-20261107):"
          " re-entrant key/value callbacks")
MODIFIED = "// Modified 2026 by Hitesh Soliwal and contributors: re-entrant key/value callbacks (HASH_REENTRANT_GUARD).\n"

SLOTMAP_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and contributors: slot cursors read the revision before their first slot"
     " (TREEMAP_ABA_GUARD).\n",
     "// Modified 2026 by Hitesh Soliwal and contributors: slot cursors read the revision before their first slot"
     " (TREEMAP_ABA_GUARD).\n" + MODIFIED),
    ("    int firstCursorSlot() { return firstSlot(); }\n",
     "    int firstCursorSlot() { return firstSlot(); }\n"
     "    " + ANCHOR + "\n"
     "    /**\n"
     "     * Walks a key lookup repeats when the key's equals changed the structure under it, and\n"
     "     * lookups remove(key, value) and replace(key, old, new) repeat when the value's equals did,\n"
     "     * before the operation gives up with ConcurrentModificationException.\n"
     "     */\n"
     "    static final int REWALKS = 8;\n"
     "    /**\n"
     "     * One lookup for the read-modify-write operations: the key's slot (low 32 bits, -1 when\n"
     "     * absent) and an owner hint for insertAbsent (high 32 bits; the hash owners' key hash).\n"
     "     */\n"
     "    long locate(Object key) { return findSlot(key) & 0xFFFFFFFFL; }\n"
     "    /**\n"
     "     * Inserts a key locate found absent, the structure unchanged since (revision expected). Hash\n"
     "     * owners insert by the hint without calling key code again, as java.util.HashMap's compute\n"
     "     * does with its single walk; the default puts.\n"
     "     */\n"
     "    void insertAbsent(K key, V value, int hint, int expected) { put(key, value); }\n"),
    ("    @Override public boolean remove(Object key, Object value) {\n"
     "        int s = findSlot(key);\n"
     "        if (s < 0 || !Objects.equals(value, valueAt(s))) { return false; }\n"
     "        eraseAt(s); return true;\n"
     "    }\n",
     "    @Override public boolean remove(Object key, Object value) {\n"
     "        for (int lookups = 0; ; lookups++) {\n"
     "            int s = findSlot(key);\n"
     "            if (s < 0) { return false; }\n"
     "            int expected = revision(); boolean same = Objects.equals(value, valueAt(s));\n"
     "            if (revision() == expected) { if (same) { eraseAt(s); } return same; }\n"
     "            if (lookups == REWALKS) { throw new ConcurrentModificationException(); }\n"
     "        }\n"
     "    }\n"),
    ("        int s = findSlot(key);\n"
     "        if (s < 0) { return put(key, value); }\n"
     "        V old = valueAt(s);\n",
     "        long located = locate(key); int s = (int) located;\n"
     "        if (s < 0) { insertAbsent(key, value, (int) (located >>> 32), revision()); return null; }\n"
     "        V old = valueAt(s);\n"),
    ("    @Override public boolean replace(K key, V old, V value) {\n"
     "        int s = findSlot(key);\n"
     "        if (s < 0 || !Objects.equals(old, valueAt(s))) { return false; }\n"
     "        writeAt(s, value); accessed(s); return true;\n"
     "    }\n",
     "    @Override public boolean replace(K key, V old, V value) {\n"
     "        for (int lookups = 0; ; lookups++) {\n"
     "            int s = findSlot(key);\n"
     "            if (s < 0) { return false; }\n"
     "            int expected = revision(); boolean same = Objects.equals(old, valueAt(s));\n"
     "            if (revision() == expected) { if (same) { writeAt(s, value); accessed(s); } return same; }\n"
     "            if (lookups == REWALKS) { throw new ConcurrentModificationException(); }\n"
     "        }\n"
     "    }\n"),
    ("        int s = findSlot(key);\n"
     "        if (s >= 0 && valueAt(s) != null) { V v = valueAt(s); accessed(s); return v; }\n"
     "        int expected = revision(); V v = action.apply(key); check(expected);\n"
     "        if (v != null) { put(key, v); }\n"
     "        return v;\n",
     "        long located = locate(key); int s = (int) located;\n"
     "        if (s >= 0 && valueAt(s) != null) { V v = valueAt(s); accessed(s); return v; }\n"
     "        int expected = revision(); V v = action.apply(key); check(expected);\n"
     "        if (v == null) { return null; }\n"
     "        if (s >= 0) { writeAt(s, v); accessed(s); } else { insertAbsent(key, v, (int) (located >>> 32), expected); }\n"
     "        return v;\n"),
    ("        Objects.requireNonNull(action); int s = findSlot(key), expected = revision();\n"
     "        V v = action.apply(key, s < 0 ? null : valueAt(s)); check(expected);\n"
     "        if (v == null) { if (s >= 0) { eraseAt(s); } } else { put(key, v); }\n"
     "        return v;\n",
     "        Objects.requireNonNull(action); long located = locate(key); int s = (int) located, expected = revision();\n"
     "        V v = action.apply(key, s < 0 ? null : valueAt(s)); check(expected);\n"
     "        if (v == null) { if (s >= 0) { eraseAt(s); } }\n"
     "        else if (s >= 0) { writeAt(s, v); accessed(s); }\n"
     "        else { insertAbsent(key, v, (int) (located >>> 32), expected); }\n"
     "        return v;\n"),
    ("        int s = findSlot(key), expected = revision(); V old = s < 0 ? null : valueAt(s);\n"
     "        V v = old == null ? value : action.apply(old, value); check(expected);\n"
     "        if (v == null) { if (s >= 0) { eraseAt(s); } } else { put(key, v); }\n"
     "        return v;\n",
     "        long located = locate(key); int s = (int) located, expected = revision();\n"
     "        V old = s < 0 ? null : valueAt(s);\n"
     "        V v = old == null ? value : action.apply(old, value); check(expected);\n"
     "        if (v == null) { if (s >= 0) { eraseAt(s); } }\n"
     "        else if (s >= 0) { writeAt(s, v); accessed(s); }\n"
     "        else { insertAbsent(key, v, (int) (located >>> 32), expected); }\n"
     "        return v;\n"),
]

HASHMAP_EDITS = [
    ("// Modified 2026: DOOP array/index/mask HashMap storage port; no stored Entry or Node.\n",
     "// Modified 2026: DOOP array/index/mask HashMap storage port; no stored Entry or Node.\n" + MODIFIED),
    (" * tree-bin and serialization-shape equivalence are separate porting gates.\n",
     " * tree-bin and serialization-shape equivalence are separate porting gates.\n"
     " * A lookup whose key equals changes this map's structure walks again, so it never reads,\n"
     " * writes or erases a slot the change released or reused.\n"),
    ("    private int find(Object key, int hash) {\n"
     "        if (index.length == 0) return -1;\n"
     "        int mask = index.length - 1;\n"
     "        for (int p = hash & mask, ref; (ref = index[p]) != 0; p = (p + 1) & mask) {\n"
     "            if (ref > 0) {\n"
     "                int s = ref - 1;\n"
     "                if (hashes[s] == hash && Objects.equals(key, keys[s])) return s;\n"
     "            }\n"
     "        }\n"
     "        return -1;\n"
     "    }\n",
     "    " + ANCHOR + "\n"
     "    private int find(Object key, int hash) { return find(key, hash, 0); }\n"
     "\n"
     "    private int find(Object key, int hash, int walks) {\n"
     "        if (index.length == 0) return -1;\n"
     "        int mask = index.length - 1, expected = modifications;\n"
     "        for (int p = hash & mask, ref; (ref = index[p]) != 0; p = (p + 1) & mask) {\n"
     "            if (ref > 0) {\n"
     "                int s = ref - 1;\n"
     "                if (hashes[s] == hash) {\n"
     "                    boolean same = Objects.equals(key, keys[s]);\n"
     "                    if (modifications != expected) return rewalk(key, hash, walks); // equals changed the structure\n"
     "                    if (same) return s;\n"
     "                }\n"
     "            }\n"
     "        }\n"
     "        return -1;\n"
     "    }\n"
     "\n"
     "    private int rewalk(Object key, int hash, int walks) {\n"
     "        if (walks == REWALKS) throw new java.util.ConcurrentModificationException();\n"
     "        return find(key, hash, walks + 1);\n"
     "    }\n"),
    ("        int h = hash(key), existing = find(key, h);\n"
     "        if (existing >= 0) return writeAt(existing, value);\n"
     "\n"
     "        // All allocation happens before claiming a slot or publishing a mapping.\n",
     "        int h = hash(key), existing = find(key, h);\n"
     "        if (existing >= 0) return writeAt(existing, value);\n"
     "        insertNew(key, value, h);\n"
     "        return null;\n"
     "    }\n"
     "\n"
     "    @Override long locate(Object key) {\n"
     "        int h = hash(key);\n"
     "        return (long) h << 32 | find(key, h) & 0xFFFFFFFFL;\n"
     "    }\n"
     "\n"
     "    @Override void insertAbsent(K key, V value, int hint, int expected) {\n"
     "        if (modifications != expected) throw new java.util.ConcurrentModificationException();\n"
     "        insertNew(key, value, hint);\n"
     "    }\n"
     "\n"
     "    /** Inserts a key the walk just found absent; calls no user code. */\n"
     "    private void insertNew(K key, V value, int h) {\n"
     "        // All allocation happens before claiming a slot or publishing a mapping.\n"),
    ("        size++;\n"
     "        modifications++;\n"
     "        return null;\n"
     "    }\n",
     "        size++;\n"
     "        modifications++;\n"
     "    }\n"),
]

LINKED_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and contributors: lazy expected-key reservation.\n",
     "// Modified 2026 by Hitesh Soliwal and contributors: lazy expected-key reservation.\n" + MODIFIED),
    ("        if (s < 0) { table.insert(key, value, hash, false); return null; }\n"
     "        V old = writeAt(s, value); accessed(s); return old;\n"
     "    }\n",
     "        if (s < 0) { table.insert(key, value, hash, false); return null; }\n"
     "        V old = writeAt(s, value); accessed(s); return old;\n"
     "    }\n"
     "    @Override long locate(Object key) {\n"
     "        int hash = M3SlotArrays.hash(key);\n"
     "        return (long) hash << 32 | table.find(key, hash) & 0xFFFFFFFFL;\n"
     "    }\n"
     "    @Override void insertAbsent(K key, V value, int hint, int expected) {\n"
     "        if (table.modifications != expected) { throw new java.util.ConcurrentModificationException(); }\n"
     "        table.insert(key, value, hint, false);\n"
     "    }\n"),
    ("        int find(Object key, int hash) {\n"
     "            if (index.length == 0) { return -1; }\n"
     "            for (int p = hash & (index.length - 1); index[p] != 0; p = (p + 1) & (index.length - 1)) {\n"
     "                int s = index[p] - 1;\n"
     "                if (s >= 0 && hashes[s] == hash && Objects.equals(key, keys[s])) { return s; }\n"
     "            }\n"
     "            return -1;\n"
     "        }\n",
     "        " + ANCHOR + "\n"
     "        int find(Object key, int hash) { return find(key, hash, 0); }\n"
     "        private int find(Object key, int hash, int walks) {\n"
     "            if (index.length == 0) { return -1; }\n"
     "            int mask = index.length - 1, expected = modifications;\n"
     "            for (int p = hash & mask; index[p] != 0; p = (p + 1) & mask) {\n"
     "                int s = index[p] - 1;\n"
     "                if (s >= 0 && hashes[s] == hash) {\n"
     "                    boolean same = Objects.equals(key, keys[s]);\n"
     "                    if (modifications != expected) { return rewalk(key, hash, walks); } // equals changed it\n"
     "                    if (same) { return s; }\n"
     "                }\n"
     "            }\n"
     "            return -1;\n"
     "        }\n"
     "        private int rewalk(Object key, int hash, int walks) {\n"
     "            if (walks == M3SlotMap.REWALKS) { throw new java.util.ConcurrentModificationException(); }\n"
     "            return find(key, hash, walks + 1);\n"
     "        }\n"),
]

WEAK_EDITS = [
    ("// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type"
     " adaptation.\n",
     "// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type"
     " adaptation.\n" + MODIFIED),
    ("    @Override int findSlot(Object key) {\n"
     "        drain(); int hash = M3SlotArrays.hash(key), mask = states.length - 1;\n"
     "        // Hash evaluation above remains observable even before storage is allocated.\n"
     "        if (states.length == 0) { return -1; }\n"
     "        Object target = key == null ? NULL_KEY : key;\n"
     "        for (int s = hash & mask; states[s] != 0; s = (s + 1) & mask) {\n"
     "            if (states[s] == 1 && hashes[s] == hash && Objects.equals(target, referent(s))) { return s; }\n"
     "        }\n"
     "        return -1;\n"
     "    }\n",
     "    @Override int findSlot(Object key) {\n"
     "        // Hash evaluation remains observable even before storage is allocated.\n"
     "        drain(); return find(key, M3SlotArrays.hash(key));\n"
     "    }\n"
     "    " + ANCHOR + "\n"
     "    private int find(Object key, int hash) { return find(key == null ? NULL_KEY : key, hash, 0); }\n"
     "    private int find(Object target, int hash, int walks) {\n"
     "        if (states.length == 0) { return -1; }\n"
     "        int mask = states.length - 1, expected = modifications;\n"
     "        for (int s = hash & mask; states[s] != 0; s = (s + 1) & mask) {\n"
     "            if (states[s] == 1 && hashes[s] == hash) {\n"
     "                boolean same = Objects.equals(target, referent(s));\n"
     "                if (modifications != expected) { return rewalk(target, hash, walks); } // equals changed it\n"
     "                if (same) { return s; }\n"
     "            }\n"
     "        }\n"
     "        return -1;\n"
     "    }\n"
     "    private int rewalk(Object target, int hash, int walks) {\n"
     "        if (walks == REWALKS) { throw new java.util.ConcurrentModificationException(); }\n"
     "        return find(target, hash, walks + 1);\n"
     "    }\n"),
    ("    @Override public V put(K key, V value) {\n"
     "        int s = findSlot(key);\n"
     "        if (s >= 0) { return writeAt(s, value); }\n"
     "        if (states.length == 0) { rehash(16); }\n",
     "    @Override public V put(K key, V value) {\n"
     "        for (int lookups = 0; ; lookups++) {\n"
     "            int s = findSlot(key);\n"
     "            if (s >= 0) { return writeAt(s, value); }\n"
     "            int expected = modifications;\n"
     "            int hash = M3SlotArrays.hash(key); // the second hash call is pinned (M3FootprintContractTest)\n"
     "            if (modifications == expected) { insertNew(key, value, hash); return null; }\n"
     "            if (lookups == REWALKS) { throw new java.util.ConcurrentModificationException(); }\n"
     "        }\n"
     "    }\n"
     "    @Override long locate(Object key) {\n"
     "        drain(); int hash = M3SlotArrays.hash(key);\n"
     "        return (long) hash << 32 | find(key, hash) & 0xFFFFFFFFL;\n"
     "    }\n"
     "    @Override void insertAbsent(K key, V value, int hint, int expected) {\n"
     "        if (modifications != expected) { throw new java.util.ConcurrentModificationException(); }\n"
     "        insertNew(key, value, hint);\n"
     "    }\n"
     "    /** Inserts a key the walk just found absent, with its hash computed once; calls no user code. */\n"
     "    private void insertNew(K key, V value, int hash) {\n"
     "        if (states.length == 0) { rehash(16); }\n"),
    ("        int hash = M3SlotArrays.hash(key); s = hash & (states.length - 1);\n",
     "        int s = hash & (states.length - 1);\n"),
    ("        size++; modifications++; return null;\n"
     "    }\n"
     "    private void rehash(int capacity) {\n",
     "        size++; modifications++;\n"
     "    }\n"
     "    private void rehash(int capacity) {\n"),
]

# path, preimage blob (base commit), behaviour, rule note, edits
RULES = (
    (SRC + "M3SlotMap.java", "93350a6721a0333085bf287087db316cbd64f47c", "S1+S2",
     "REWALKS bound; locate/insertAbsent hooks; remove(k,v)/replace(k,old,new) look again after a value callback"
     " changed the structure; putIfAbsent/computeIfAbsent/compute/merge locate once, write a present key through its"
     " slot and insert an absent key through insertAbsent",
     SLOTMAP_EDITS),
    (SRC + "M3HashMap.java", "3e3929b98fe7ef46f24d705d18cad6684ab5954f", "S1",
     "find walks again after a key equals changed the structure; put = find + insertNew; locate/insertAbsent",
     HASHMAP_EDITS),
    (SRC + "M3LinkedHashMap.java", "8bc93a73fd02e2fa000d5899d74cc1917c8c2abd", "S1",
     "Table.find walks again after a key equals changed the structure; locate/insertAbsent",
     LINKED_EDITS),
    (SRC + "M3WeakHashMap.java", "2cfac9e572e199fcb8e5d6f529a5c1b0776449d6", "S1",
     "find walks again after a key equals changed the structure; put looks again when its pinned second hash"
     " call changed the structure (find + insertNew);"
     " locate/insertAbsent",
     WEAK_EDITS),
)


class Refused(Exception):
    pass


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


def run(root: Path, check: bool) -> int:
    rows, changed, writes = [], 0, []
    for rel, blob, behaviour, note, edits in RULES:
        target = root / rel
        if not target.exists():
            raise Refused("missing preimage %s (pinned blob %s)" % (rel, blob))
        on_disk = target.read_bytes()
        pre = on_disk if blob_sha(on_disk) == blob else git_blob(root, blob)
        post = postimage(pre, edits, rel)
        if blob_sha(on_disk) != blob and on_disk != post:
            raise Refused("drifted preimage %s: on disk %s, pinned blob %s" % (rel, blob_sha(on_disk), blob))
        state = "UNCHANGED" if on_disk == post else "CHANGED"
        if state == "CHANGED":
            changed += 1
            writes.append((target, post))
        rows.append((rel, sha256(pre), sha256(post), behaviour, note))
        print("EDIT %s %s %s" % (behaviour, state, rel))
    if not check:  # every preimage verified before the first write: a refusal leaves the tree untouched
        for target, post in writes:
            target.write_bytes(post)
    table = "\n".join(["\t".join(EDITS_COLUMNS)] + ["\t".join(row) for row in rows]) + "\n"
    edits_path = RECIPE_DIR / EDITS_NAME
    table_state = not edits_path.exists() or edits_path.read_bytes() != table.encode("utf-8")
    if table_state and not check:
        edits_path.write_bytes(table.encode("utf-8"))
    print("HASH_REENTRANT_GUARD recipe=%s %s base=%s rules=%d changed=%d edits=%s" % (
        RECIPE, VERSION, BASE_COMMIT, len(rows), changed, "CHANGED" if table_state else "UNCHANGED"))
    return 1 if check and (changed or table_state) else 0


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--root", default=str(RECIPE_DIR.parents[3]), help="repository root (default: four up)")
    parser.add_argument("--check", action="store_true", help="write nothing; exit 1 when a run would change a file")
    args = parser.parse_args(argv)
    try:
        return run(Path(args.root).resolve(), args.check)
    except Refused as refusal:
        print("REFUSED " + str(refusal), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
