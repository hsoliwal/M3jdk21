// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: Synexia-to-M3 package/type adaptation.
// Modified 2026 by Hitesh Soliwal and contributors: re-entrant key/value callbacks (HASH_REENTRANT_GUARD).
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Objects;

/**
 * Weak-key open-addressed map. Each non-null key needs one WeakReference for GC;
 * values, hashes and occupancy remain separate arrays, with no key/value entries.
 * Values are strong and must not retain their keys if reclamation is desired.
 * Backing arrays are allocated on first insertion; reference-queue ownership is unchanged.
 */
public final class M3WeakHashMap<K, V> extends SlotMap<K, V> {
    @Override protected boolean cacheEntryFacades() { return false; }
    private static final Object NULL_KEY = new Object();
    private Object[] references = SlotArrays.EMPTY_OBJECTS, values = SlotArrays.EMPTY_OBJECTS;
    private int[] hashes = SlotArrays.EMPTY_INTS;
    private byte[] states = SlotArrays.EMPTY_BYTES;
    private final ReferenceQueue<Object> queue = new ReferenceQueue<>();
    private int size, occupied, modifications;
    /** Empty weak-key map supporting null keys and values. */
    public M3WeakHashMap() { }
    private static final class KeyReference extends WeakReference<Object> {
        private int slot;
        KeyReference(Object key, ReferenceQueue<Object> queue, int slot) { super(key, queue); this.slot = slot; }
    }
    private Object referent(int slot) {
        Object r = references[slot]; return r == NULL_KEY ? NULL_KEY : r == null ? null : ((KeyReference) r).get();
    }
    private void drain() {
        KeyReference ref;
        while ((ref = (KeyReference) queue.poll()) != null) {
            int s = ref.slot; if (s >= 0 && references[s] == ref) { erase(s, false); }
        }
    }
    @Override boolean live(int s) { return states[s] == 1 && referent(s) != null; }
    @Override int firstSlot() { drain(); return nextSlot(-1); }
    @Override int nextSlot(int s) {
        while (++s < states.length) { if (live(s)) { return s; } } return -1;
    }
    @Override int revision() { return modifications; }
    @Override int findSlot(Object key) {
        // Hash evaluation remains observable even before storage is allocated.
        drain(); return find(key, SlotArrays.hash(key));
    }
    //FLLDCIM: MRL - recipe HASH_REENTRANT_GUARD v1 (qualification/hash-reentrant-guard-20261107): re-entrant key/value callbacks
    private int find(Object key, int hash) { return find(key == null ? NULL_KEY : key, hash, 0); }
    private int find(Object target, int hash, int walks) {
        if (states.length == 0) { return -1; }
        int mask = states.length - 1, expected = modifications;
        for (int s = hash & mask; states[s] != 0; s = (s + 1) & mask) {
            if (states[s] == 1 && hashes[s] == hash) {
                boolean same = Objects.equals(target, referent(s));
                if (modifications != expected) { return rewalk(target, hash, walks); } // equals changed it
                if (same) { return s; }
            }
        }
        return -1;
    }
    private int rewalk(Object target, int hash, int walks) {
        if (walks == REWALKS) { throw new java.util.ConcurrentModificationException(); }
        return find(target, hash, walks + 1);
    }
    @Override K keyAt(int s) {
        Object key = referent(s);
        @SuppressWarnings("unchecked") K k = key == NULL_KEY ? null : (K) key; return k;
    }
    @Override V valueAt(int s) { return SlotArrays.get(values, s); }
    @Override V writeAt(int s, V value) { V old = valueAt(s); values[s] = value; return old; }
    @Override V eraseAt(int s) { forgetEntryFacade(keyAt(s)); return erase(s, true); }
    private V erase(int s, boolean structural) {
        if (states[s] != 1) { return null; }
        V old = valueAt(s);
        if (references[s] instanceof KeyReference ref) { ref.slot = -1; ref.clear(); }
        references[s] = null; values[s] = null; states[s] = 2; size--;
        if (structural) { modifications++; } return old;
    }
    @Override public int size() { drain(); return size; }
    @Override public V put(K key, V value) {
        for (int lookups = 0; ; lookups++) {
            int s = findSlot(key);
            if (s >= 0) { return writeAt(s, value); }
            int expected = modifications;
            int hash = SlotArrays.hash(key); // the second hash call is pinned (FootprintContractTest)
            if (modifications == expected) { insertNew(key, value, hash); return null; }
            if (lookups == REWALKS) { throw new java.util.ConcurrentModificationException(); }
        }
    }
    @Override long locate(Object key) {
        drain(); int hash = SlotArrays.hash(key);
        return (long) hash << 32 | find(key, hash) & 0xFFFFFFFFL;
    }
    @Override void insertAbsent(K key, V value, int hint, int expected) {
        if (modifications != expected) { throw new java.util.ConcurrentModificationException(); }
        insertNew(key, value, hint);
    }
    /** Inserts a key the walk just found absent, with its hash computed once; calls no user code. */
    private void insertNew(K key, V value, int hash) {
        if (states.length == 0) { rehash(16); }
        else if (occupied + 1 >= states.length - states.length / 3) {
            rehash(size + 1 >= states.length - states.length / 3 ? SlotArrays.doubled(states.length) : states.length);
        }
        int s = hash & (states.length - 1);
        while (states[s] == 1) { s = (s + 1) & (states.length - 1); }
        Object reference = key == null ? NULL_KEY : new KeyReference(key, queue, s);
        if (states[s] == 0) { occupied++; }
        references[s] = reference; values[s] = value; hashes[s] = hash; states[s] = 1;
        size++; modifications++;
    }
    private void rehash(int capacity) {
        Object[] r = new Object[capacity], v = new Object[capacity];
        int[] h = new int[capacity]; byte[] state = new byte[capacity]; int count = 0;
        for (int s = 0; s < states.length; s++) {
            if (!live(s)) { if (references[s] instanceof KeyReference ref) { ref.slot = -1; } continue; }
            int p = hashes[s] & (capacity - 1);
            while (state[p] != 0) { p = (p + 1) & (capacity - 1); }
            r[p] = references[s]; v[p] = values[s]; h[p] = hashes[s]; state[p] = 1; count++;
            if (r[p] instanceof KeyReference ref) { ref.slot = p; }
        }
        references = r; values = v; hashes = h; states = state; size = count; occupied = count;
    }
    @Override public void clear() {
        if (occupied == 0) { return; }
        for (Object r : references) { if (r instanceof KeyReference ref) { ref.slot = -1; ref.clear(); } }
        Arrays.fill(references, null); Arrays.fill(values, null); Arrays.fill(states, (byte) 0);
        size = 0; occupied = 0; modifications++; drain();
    }
    /** Drain references already enqueued by the JVM; does not request a GC. */
    public void expungeStaleKeys() { drain(); }
}
