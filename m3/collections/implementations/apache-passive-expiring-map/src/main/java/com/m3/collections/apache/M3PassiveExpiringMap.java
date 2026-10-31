/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package com.m3.collections.apache;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import org.apache.commons.collections4.Get;
import org.apache.commons.collections4.IterableGet;
import org.apache.commons.collections4.IterableMap;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.Put;
import org.apache.commons.collections4.ResettableIterator;
import org.apache.commons.collections4.map.PassiveExpiringMap.ExpirationPolicy;

/**
 * Owned M3 body for the public contract of Apache Commons Collections 4.5.0
 * {@code org.apache.commons.collections4.map.PassiveExpiringMap}: a map whose entries expire once the clock reaches
 * the expiration time their policy gave them. Commons supplies only API interfaces and the {@code ExpirationPolicy}
 * type (Apache-2.0); no Commons implementation class, helper or body is called or copied.
 *
 * <p>Contract kept from the provider. An entry is expired when its expiration time {@code t} is not negative and
 * {@code now >= t}; a negative time never expires. {@link #get}, {@link #containsKey} and {@link #put} remove only
 * their own key when it has expired; {@link #size}, {@link #isEmpty}, {@link #containsValue}, {@link #entrySet},
 * {@link #keySet}, {@link #values}, {@link #mapIterator}, {@link #forEach} and {@link #replaceAll} remove every expired
 * entry first. {@link #remove}, {@link #equals}, {@link #hashCode}, {@link #toString} and every operation on a view
 * expire nothing. {@link #put} calls the policy once, with the key and the value, after its own key check. The
 * time-to-live constructors use the provider's constant rule: a negative time-to-live never expires, otherwise the
 * expiration time is {@code now + ttl}, or never when that sum would overflow. The clock is
 * {@link System#currentTimeMillis()}, read at the same points as the provider reads it.
 *
 * <p>Storage. Entries live in stable slots of parallel lanes ({@code Object[] keys}, {@code Object[] values},
 * {@code int[] hashes}, {@code long[] deadlines}, {@code int[] stamps}, {@code int[] gens}) with a {@code long[]
 * liveMask}; a power-of-two {@code long[] table} holds one cell per probe position (spread hash in the high half, slot +
 * 1 in the low half, 0 empty), probed linearly and cleared by backward shift, so probing and shifting compare hashes
 * without touching the slot lanes. Key hash codes pass through the same full-avalanche mix as the core primitive hash
 * owners, so keys with sequential hash codes do not pile into one probe run. Free slots are chained through the hash
 * lane. No Entry or Node object is stored per mapping;
 * entries handed out by views are flyweights over a slot and its generation, so like the provider's HashMap nodes they
 * stay attached through value and expiration-time replacement and detach once their mapping is removed, even when the
 * slot is later reused for the same key.
 *
 * <p>Expiry order. Every slot with an expiration time has one current record in an {@link M3ExpiryOrderIndex}: a FIFO
 * ring while expiration times arrive in order (the constant time-to-live case), a 4-ary min-heap otherwise. A
 * whole-map purge pops only the expired prefix, so {@link #size()} and {@link #isEmpty()} cost O(1) when nothing has
 * expired, against the provider's walk over every entry. A changed or removed slot changes its stamp; its old record is
 * popped at once when it is the first record (removal or refresh in expiry order), otherwise skipped when it reaches the
 * front, and the index is compacted when stale records outnumber current ones.
 *
 * <p>The remaining {@code Map} operations ({@code getOrDefault}, {@code putIfAbsent}, {@code remove(key, value)}, both
 * {@code replace} forms, the {@code compute} family and {@code merge}) are the {@code java.util.Map} defaults, as they
 * are for the provider, so they reach the same purge points and clock reads through {@link #get},
 * {@link #containsKey}, {@link #put} and {@link #remove}.
 *
 * <p>Deliberate differences from the provider: iteration follows slot order (the provider's HashMap order is
 * unspecified); the {@code Map} constructors copy the given mappings as never-expiring entries instead of decorating
 * the given map, so later changes are not shared and keys use {@code equals}/{@code hashCode}; the serialized form is
 * the owned format below and the clock is not serialized; {@link #toString} prints a map that contains itself as
 * {@code (this Map)} where the provider's delegate recurses; the class is final. Fail-fast iteration follows the
 * provider's HashMap rule: insertions, removals and {@code clear()} invalidate iterators, value replacement does not.
 *
 * @param <K> key type
 * @param <V> value type
 */
//FLLDCIM: DOOP - slot lanes + live mask + probe table; entries are boundary flyweights
//FLLDCIM: CONTRACT - Commons 4.5.0 PassiveExpiringMap purge points and clock reads, verified by differential tests
public final class M3PassiveExpiringMap<K, V>
        implements Serializable,
                Map<K, V>,
                Get<K, V>,
                IterableGet<K, V>,
                IterableMap<K, V>,
                Put<K, V> {
    @Serial
    private static final long serialVersionUID = 0L;
    private static final int MAX_ARRAY = Integer.MAX_VALUE - 8;
    private static final long NEVER = -1L;
    private static final int COMPACT_FLOOR = 64;
    private static final Object[] NO_OBJECTS = {};
    private static final int[] NO_INTS = {};
    private static final long[] NO_LONGS = {};
    /** One empty probe cell shared by every map that has never held an entry; it is never written. */
    private static final long[] NO_TABLE = {0L};
    private static final long SLOT_BITS = 0xFFFF_FFFFL;
    private static final LongSupplier SYSTEM_CLOCK = System::currentTimeMillis;

    /** Per-entry policy, or null for the constant time-to-live {@link #timeToLiveMillis}. */
    private final ExpirationPolicy<K, V> policy;
    /** Constant time-to-live in milliseconds, used when {@link #policy} is null; negative never expires. */
    private final long timeToLiveMillis;

    private transient LongSupplier clock;
    private transient Object[] keys;
    private transient Object[] values;
    /** Spread hash of a live slot; the next free slot (or -1) of a free slot. */
    private transient int[] hashes;
    /** Expiration time of a live slot; negative never expires. */
    private transient long[] deadlines;
    /** Generation of a slot's expiry record; changed when the deadline changes or the slot is freed. */
    private transient int[] stamps;
    /** Generation of a slot's mapping; changed when the slot is freed or the map is cleared (entry detachment). */
    private transient int[] gens;
    private transient long[] liveMask;
    /** Probe cells: spread hash in the high 32 bits, slot + 1 in the low 32 bits, 0 for an empty cell. */
    private transient long[] table;
    private transient int size;
    private transient int slotHigh;
    private transient int freeHead;
    /** Live slots with a non-negative expiration time, i.e. current records in {@link #order}. */
    private transient int indexed;
    private transient int modCount;
    private transient M3ExpiryOrderIndex order;
    private transient Set<Map.Entry<K, V>> entrySetView;
    private transient Set<K> keySetView;
    private transient Collection<V> valuesView;

    /** Creates a map whose entries never expire. */
    public M3PassiveExpiringMap() {
        this(null, NEVER, Map.of(), SYSTEM_CLOCK);
    }

    /**
     * Creates a map whose entries never expire, holding a copy of {@code map}.
     *
     * @throws NullPointerException if {@code map} is null
     */
    public M3PassiveExpiringMap(Map<K, V> map) {
        this(NEVER, map);
    }

    /** Creates a map with a constant time-to-live in milliseconds; negative never expires, zero always expires. */
    public M3PassiveExpiringMap(long timeToLiveMillis) {
        this(null, timeToLiveMillis, Map.of(), SYSTEM_CLOCK);
    }

    /**
     * Creates a map with a constant time-to-live in milliseconds, holding a copy of {@code map} whose entries never
     * expire unless they are replaced.
     *
     * @throws NullPointerException if {@code map} is null
     */
    public M3PassiveExpiringMap(long timeToLiveMillis, Map<K, V> map) {
        this(null, timeToLiveMillis, Objects.requireNonNull(map, "map"), SYSTEM_CLOCK);
    }

    /**
     * Creates a map with a constant time-to-live in the given unit.
     *
     * @throws NullPointerException if {@code timeUnit} is null
     */
    public M3PassiveExpiringMap(long timeToLive, TimeUnit timeUnit) {
        this(toMillis(timeToLive, timeUnit));
    }

    /**
     * Creates a map with a constant time-to-live in the given unit, holding a copy of {@code map} whose entries
     * never expire unless they are replaced.
     *
     * @throws NullPointerException if {@code timeUnit} or {@code map} is null
     */
    public M3PassiveExpiringMap(long timeToLive, TimeUnit timeUnit, Map<K, V> map) {
        this(toMillis(timeToLive, timeUnit), map);
    }

    /**
     * Creates a map whose expiration times come from {@code expiringPolicy}.
     *
     * @throws NullPointerException if {@code expiringPolicy} is null
     */
    public M3PassiveExpiringMap(ExpirationPolicy<K, V> expiringPolicy) {
        this(Objects.requireNonNull(expiringPolicy, "expiringPolicy"), 0L, Map.of(), SYSTEM_CLOCK);
    }

    /**
     * Creates a map whose expiration times come from {@code expiringPolicy}, holding a copy of {@code map} whose
     * entries never expire unless they are replaced.
     *
     * @throws NullPointerException if {@code map} or {@code expiringPolicy} is null (checked in that order)
     */
    public M3PassiveExpiringMap(ExpirationPolicy<K, V> expiringPolicy, Map<K, V> map) {
        this(policyAfterMap(expiringPolicy, map), 0L, map, SYSTEM_CLOCK);
    }

    /** Main constructor; tests and benchmarks inject {@code clock}, the public constructors pass the system clock. */
    M3PassiveExpiringMap(ExpirationPolicy<K, V> policy, long timeToLiveMillis, Map<? extends K, ? extends V> initial,
            LongSupplier clock) {
        this.policy = policy;
        this.timeToLiveMillis = timeToLiveMillis;
        this.clock = Objects.requireNonNull(clock, "clock");
        initLanes();
        for (Map.Entry<? extends K, ? extends V> e : initial.entrySet()) {
            putNever(e.getKey(), e.getValue());
        }
    }

    /** The provider's time-unit conversion: {@code TimeUnit.MILLISECONDS.convert}, saturating. */
    static long toMillis(long timeToLive, TimeUnit timeUnit) {
        return TimeUnit.MILLISECONDS.convert(timeToLive, Objects.requireNonNull(timeUnit, "timeUnit"));
    }

    private static <K, V> ExpirationPolicy<K, V> policyAfterMap(ExpirationPolicy<K, V> policy, Map<K, V> map) {
        Objects.requireNonNull(map, "map");
        return Objects.requireNonNull(policy, "expiringPolicy");
    }

    // ------------------------------------------------------------------ Map API

    @Override
    public V get(Object key) {
        int s = liveSlot(key, now());
        return s < 0 ? null : valueAt(s);
    }

    @Override
    public boolean containsKey(Object key) {
        return liveSlot(key, now()) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        purge(now());
        return slotOfValue(value) >= 0;
    }

    //FLLDCIM: HPCPOP - hot path: one probe, lane writes and an index append; no allocation
    @Override
    public V put(K key, V value) {
        int mods = modCount;
        long now = now();
        int h = spread(key);
        int p = find(key, h);
        if (p >= 0 && expired(slotAt(p), now)) {
            removeAt(p, slotAt(p));
        }
        long deadline = expirationTime(key, value);
        if (mods != modCount) {
            p = find(key, h);
        }
        if (p >= 0) {
            return refresh(slotAt(p), value, deadline);
        }
        if (growthNeeded()) {
            growTable();
            p = find(key, h);
        }
        insert(p, key, h, value, deadline);
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Map.Entry<? extends K, ? extends V> e : map.entrySet()) {
            put(e.getKey(), e.getValue());
        }
    }

    /** Removes {@code key} without an expiry check and returns its value, as the provider does. */
    @Override
    public V remove(Object key) {
        int p = find(key, spread(key));
        if (p < 0) {
            return null;
        }
        int s = slotAt(p);
        V old = valueAt(s);
        removeAt(p, s);
        return old;
    }

    @Override
    public void clear() {
        modCount++;
        if (slotHigh > 0) {
            Arrays.fill(keys, 0, slotHigh, null);
            Arrays.fill(values, 0, slotHigh, null);
            Arrays.fill(liveMask, 0, (slotHigh + 63) >>> 6, 0L);
            Arrays.fill(table, 0L);
            for (int s = 0; s < slotHigh; s++) {
                gens[s]++;
            }
        }
        size = 0;
        slotHigh = 0;
        freeHead = -1;
        indexed = 0;
        order.clear();
    }

    @Override
    public int size() {
        purge(now());
        return size;
    }

    @Override
    public boolean isEmpty() {
        purge(now());
        return size == 0;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        purge(now());
        Set<Map.Entry<K, V>> v = entrySetView;
        return v != null ? v : (entrySetView = new EntryView());
    }

    @Override
    public Set<K> keySet() {
        purge(now());
        Set<K> v = keySetView;
        return v != null ? v : (keySetView = new KeyView());
    }

    @Override
    public Collection<V> values() {
        purge(now());
        Collection<V> v = valuesView;
        return v != null ? v : (valuesView = new ValueView());
    }

    @Override
    public MapIterator<K, V> mapIterator() {
        purge(now());
        return new SlotMapIterator();
    }

    /** Purges like {@link #entrySet()}, then visits every mapping; fails fast like the provider's entry iterator. */
    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        purge(now());
        int expected = modCount;
        for (int next = nextLive(0); next >= 0;) {
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
            int s = next;
            next = nextLive(s + 1);
            action.accept(keyAt(s), valueAt(s));
        }
    }

    /**
     * Purges like {@link #entrySet()}, then replaces every value in place; expiration times are unchanged. As with the
     * provider's entry-set walk, a replacement whose mapping the function removed is lost.
     */
    @Override
    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        purge(now());
        int expected = modCount;
        for (int next = nextLive(0); next >= 0;) {
            if (modCount != expected) {
                throw new ConcurrentModificationException();
            }
            int s = next;
            next = nextLive(s + 1);
            int gen = gens[s];
            V replacement = function.apply(keyAt(s), valueAt(s));
            if (live(s) && gens[s] == gen) {
                values[s] = replacement;
            }
        }
    }

    /** Map equality over the current mappings; expires nothing here and calls {@code o.size()} first. */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Map<?, ?> m) || m.size() != size) {
            return false;
        }
        try {
            for (int s = nextLive(0); s >= 0; s = nextLive(s + 1)) {
                Object key = keys[s];
                Object value = values[s];
                if (value == null ? m.get(key) != null || !m.containsKey(key) : !value.equals(m.get(key))) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int s = nextLive(0); s >= 0; s = nextLive(s + 1)) {
            h += Objects.hashCode(keys[s]) ^ Objects.hashCode(values[s]);
        }
        return h;
    }

    @Override
    public String toString() {
        int s = nextLive(0);
        if (s < 0) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder().append('{');
        for (;;) {
            Object k = keys[s];
            Object v = values[s];
            sb.append(k == this ? "(this Map)" : k).append('=').append(v == this ? "(this Map)" : v);
            s = nextLive(s + 1);
            if (s < 0) {
                return sb.append('}').toString();
            }
            sb.append(',').append(' ');
        }
    }

    // ------------------------------------------------------------------ expiry

    private long now() {
        return clock.getAsLong();
    }

    /** Policy time, or the provider's constant rule, which reads the clock again only for a non-negative ttl. */
    private long expirationTime(K key, V value) {
        if (policy != null) {
            return policy.expirationTime(key, value);
        }
        long ttl = timeToLiveMillis;
        if (ttl < 0L) {
            return NEVER;
        }
        long now = now();
        return now > Long.MAX_VALUE - ttl ? NEVER : now + ttl;
    }

    private boolean expired(int s, long now) {
        long d = deadlines[s];
        return d >= 0L && now >= d;
    }

    /** Slot of {@code key}, or -1; an expired mapping of the key is removed first. */
    private int liveSlot(Object key, long now) {
        int p = find(key, spread(key));
        if (p < 0) {
            return -1;
        }
        int s = slotAt(p);
        if (!expired(s, now)) {
            return s;
        }
        removeAt(p, s);
        return -1;
    }

    //FLLDCIM: ALG - EXPIRY_ORDER_INDEX (C20): pop only the expired prefix; O(expired + stale + 1) per call
    private void purge(long now) {
        M3ExpiryOrderIndex q = order;
        while (q.size() != 0) {
            int s = q.firstSlot();
            if (stamps[s] != q.firstStamp()) {
                q.removeFirst();
            } else if (q.firstDue() > now) {
                return;
            } else {
                removeAt(positionOf(s), s);
            }
        }
    }

    /** Appends the slot's current record; stale records are dropped first once they outnumber current ones. */
    private void record(long deadline, int s) {
        M3ExpiryOrderIndex q = order;
        if (q.size() >= COMPACT_FLOOR && q.size() > 2L * indexed) {
            q.retain(stamps);
        }
        q.add(deadline, s, stamps[s]);
    }

    /**
     * Pops the first index record when it is the current record of slot {@code s}, which must have one (a live slot with
     * a non-negative expiration time): removals and refreshes in expiry order then leave no stale record behind.
     */
    //FLLDCIM: ALG - EXPIRY_ORDER_INDEX (C20): eager pop of the front record keeps FIFO churn free of stale records
    private void dropFrontRecord(int s) {
        M3ExpiryOrderIndex q = order;
        if (q.firstSlot() == s && q.firstStamp() == stamps[s]) {
            q.removeFirst();
        }
    }

    private V refresh(int s, V value, long deadline) {
        V old = valueAt(s);
        values[s] = value;
        long before = deadlines[s];
        if (before != deadline) {
            if (before >= 0L) {
                indexed--;
                dropFrontRecord(s);
            }
            stamps[s]++;
            deadlines[s] = deadline;
            if (deadline >= 0L) {
                indexed++;
                record(deadline, s);
            }
        }
        return old;
    }

    // ------------------------------------------------------------------ lanes

    private void initLanes() {
        keys = NO_OBJECTS;
        values = NO_OBJECTS;
        hashes = NO_INTS;
        deadlines = NO_LONGS;
        stamps = NO_INTS;
        gens = NO_INTS;
        liveMask = NO_LONGS;
        table = NO_TABLE;
        size = 0;
        slotHigh = 0;
        freeHead = -1;
        indexed = 0;
        order = new M3ExpiryOrderIndex();
    }

    /**
     * Full-avalanche mix of the key's hash code (the 32-bit mix of the core M3IntHashSet/M3IntIntHashMap owners): with
     * linear probing a weak spread lets sequential hash codes fill one long probe run, and every backward-shift deletion
     * then walks that whole run.
     */
    //FLLDCIM: HPCPOP - one mix per key operation keeps probe runs short under sequential hash codes
    private static int spread(Object key) {
        int x = key == null ? 0 : key.hashCode();
        x ^= x >>> 16;
        x *= 0x7feb352d;
        x ^= x >>> 15;
        x *= 0x846ca68b;
        x ^= x >>> 16;
        return x;
    }

    /** Table position of {@code key}, or {@code -1 - p} for the empty position p that ends its probe run. */
    private int find(Object key, int h) {
        long[] t = table;
        int mask = t.length - 1;
        for (int p = h & mask;; p = (p + 1) & mask) {
            long c = t[p];
            if (c == 0L) {
                return -1 - p;
            }
            Object k;
            if ((int) (c >>> 32) == h && ((k = keys[(int) c - 1]) == key || key != null && key.equals(k))) {
                return p;
            }
        }
    }

    /** Table position of live slot {@code s}; compares slot numbers only, so no key code runs. */
    private int positionOf(int s) {
        long[] t = table;
        int mask = t.length - 1;
        int p = hashes[s] & mask;
        while ((int) t[p] != s + 1) {
            p = (p + 1) & mask;
        }
        return p;
    }

    /** Slot named by the occupied table cell at position {@code p}. */
    private int slotAt(int p) {
        return (int) table[p] - 1;
    }

    /** Table cell for slot {@code s} with spread hash {@code h}: hash in the high half, slot + 1 in the low half. */
    private static long cell(int h, int s) {
        return (long) h << 32 | (s + 1) & SLOT_BITS;
    }

    private boolean live(int s) {
        return s < slotHigh && (liveMask[s >>> 6] & 1L << s) != 0L;
    }

    /** First live slot at or after {@code from}, or -1. */
    private int nextLive(int from) {
        if (from >= slotHigh) {
            return -1;
        }
        int w = from >>> 6;
        long word = liveMask[w] & -1L << from;
        int words = (slotHigh + 63) >>> 6;
        while (word == 0L) {
            if (++w == words) {
                return -1;
            }
            word = liveMask[w];
        }
        return (w << 6) + Long.numberOfTrailingZeros(word);
    }

    private int slotOfValue(Object value) {
        for (int s = nextLive(0); s >= 0; s = nextLive(s + 1)) {
            Object v = values[s];
            if (v == value || value != null && value.equals(v)) {
                return s;
            }
        }
        return -1;
    }

    private boolean growthNeeded() {
        return (size + 1L) * 3 > table.length * 2L;
    }

    /** Rehashes from the old cells, which carry their hashes, so no slot lane is read. */
    private void growTable() {
        int capacity = tableFor(size + 1);
        long[] t = new long[capacity];
        int mask = capacity - 1;
        for (long c : table) {
            if (c != 0L) {
                int p = (int) (c >>> 32) & mask;
                while (t[p] != 0L) {
                    p = (p + 1) & mask;
                }
                t[p] = c;
            }
        }
        table = t;
    }

    /** Smallest power-of-two table keeping {@code entries} at a load of at most 2/3. */
    static int tableFor(int entries) {
        long needed = Math.max(4L, entries * 3L / 2 + 1);
        if (needed > 1 << 30) {
            throw new OutOfMemoryError("M3PassiveExpiringMap index capacity exhausted");
        }
        return Integer.highestOneBit((int) needed - 1) << 1;
    }

    /** Next slot-lane length: 1.5x, at least 8, at most {@code Integer.MAX_VALUE - 8}. */
    static int grownLanes(int old) {
        if (old >= MAX_ARRAY) {
            throw new OutOfMemoryError("M3PassiveExpiringMap capacity exhausted");
        }
        return (int) Math.min(MAX_ARRAY, Math.max(8L, (long) old + (old >> 1) + 1L));
    }

    private int takeSlot() {
        int s = freeHead;
        if (s >= 0) {
            freeHead = hashes[s];
            return s;
        }
        if (slotHigh == keys.length) {
            int n = grownLanes(keys.length);
            keys = Arrays.copyOf(keys, n);
            values = Arrays.copyOf(values, n);
            hashes = Arrays.copyOf(hashes, n);
            deadlines = Arrays.copyOf(deadlines, n);
            stamps = Arrays.copyOf(stamps, n);
            gens = Arrays.copyOf(gens, n);
            liveMask = Arrays.copyOf(liveMask, (n + 63) >>> 6);
        }
        return slotHigh++;
    }

    /** Fills the empty position encoded by {@code p} (from {@link #find} on the current table) with a new slot. */
    private void insert(int p, Object key, int h, Object value, long deadline) {
        int s = takeSlot();
        table[-1 - p] = cell(h, s);
        keys[s] = key;
        values[s] = value;
        hashes[s] = h;
        deadlines[s] = deadline;
        liveMask[s >>> 6] |= 1L << s;
        size++;
        modCount++;
        if (deadline >= 0L) {
            indexed++;
            record(deadline, s);
        }
    }

    /** A mapping of the constructor's copy: the provider never expires the decorated map's own entries. */
    private void putNever(Object key, Object value) {
        int h = spread(key);
        int p = find(key, h);
        if (p >= 0) {
            values[slotAt(p)] = value;
            return;
        }
        if (growthNeeded()) {
            growTable();
            p = find(key, h);
        }
        insert(p, key, h, value, NEVER);
    }

    /** Removes live slot {@code s} at table position {@code p}: backward-shift deletion on the cell hashes. */
    private void removeAt(int p, int s) {
        long[] t = table;
        int mask = t.length - 1;
        for (int q = (p + 1) & mask;; q = (q + 1) & mask) {
            long c = t[q];
            if (c == 0L) {
                break;
            }
            if (((q - ((int) (c >>> 32) & mask)) & mask) >= ((q - p) & mask)) {
                t[p] = c;
                p = q;
            }
        }
        t[p] = 0L;
        if (deadlines[s] >= 0L) {
            indexed--;
            dropFrontRecord(s);
        }
        stamps[s]++;
        gens[s]++;
        keys[s] = null;
        values[s] = null;
        liveMask[s >>> 6] &= ~(1L << s);
        hashes[s] = freeHead;
        freeHead = s;
        size--;
        modCount++;
    }

    @SuppressWarnings("unchecked")
    private K keyAt(int s) {
        return (K) keys[s];
    }

    @SuppressWarnings("unchecked")
    private V valueAt(int s) {
        return (V) values[s];
    }

    /**
     * Checks lane, table, free-list and expiry-index consistency; throws {@link IllegalStateException} on the first
     * violation. Package-private for tests.
     */
    void verifyInvariants() {
        int live = 0;
        int withDeadline = 0;
        for (int s = 0; s < slotHigh; s++) {
            if (live(s)) {
                live++;
                withDeadline += deadlines[s] >= 0L ? 1 : 0;
                same(spread(keys[s]), hashes[s], "hash lane of slot " + s);
                same(s + 1, tableEntryOf(s), "probe table entry of slot " + s);
            }
        }
        same(live, size, "live slots");
        same(withDeadline, indexed, "expiring slots");
        int used = 0;
        for (long c : table) {
            used += c != 0L ? 1 : 0;
        }
        same(size, used, "table entries");
        below(size * 3L, table.length * 2L + 1, "table load");
        int free = 0;
        for (int s = freeHead; s >= 0; s = hashes[s]) {
            same(false, live(s), "live slot " + s + " on the free list");
            below(free++, slotHigh, "free list length");
        }
        same(slotHigh - size, free, "free slots");
        verifyOrder();
    }

    /**
     * {@code s + 1} when a table cell names slot {@code s} with the slot's hash, else 0; bounded by the table length.
     */
    private int tableEntryOf(int s) {
        long[] t = table;
        int mask = t.length - 1;
        long want = cell(hashes[s], s);
        int p = hashes[s] & mask;
        for (int i = 0; i < t.length; i++, p = (p + 1) & mask) {
            if (t[p] == want) {
                return s + 1;
            }
        }
        return 0;
    }

    private void verifyOrder() {
        M3ExpiryOrderIndex q = order;
        q.verify();
        boolean[] seen = new boolean[slotHigh];
        int current = 0;
        for (int r = 0; r < q.size(); r++) {
            int s = q.slotAt(r);
            if (stamps[s] == q.stampAt(r)) {
                same(true, live(s), "current record of free slot " + s);
                same(false, seen[s], "second current record of slot " + s);
                same(deadlines[s], q.dueAt(r), "record deadline of slot " + s);
                seen[s] = true;
                current++;
            }
        }
        same(indexed, current, "current records");
    }

    /** Whether the expiry index is in heap mode; package-private for tests. */
    boolean orderIsHeap() {
        return order.isHeap();
    }

    /** Number of records in the expiry index, current and stale; package-private for tests. */
    int orderRecords() {
        return order.size();
    }

    private static void same(long expected, long actual, String what) {
        if (expected != actual) {
            throw new IllegalStateException(what + ": expected " + expected + " but was " + actual);
        }
    }

    private static void same(boolean expected, boolean actual, String what) {
        if (expected != actual) {
            throw new IllegalStateException(what + ": expected " + expected);
        }
    }

    private static void below(long value, long bound, String what) {
        if (value >= bound) {
            throw new IllegalStateException(what + ": " + value + " is not below " + bound);
        }
    }

    // ------------------------------------------------------------------ serialization

    /**
     * Owned serial form: the policy and the constant time-to-live (default fields), the mapping count, then key,
     * value and expiration time per mapping, including expired mappings not yet removed.
     */
    @Serial
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeInt(size);
        for (int s = nextLive(0); s >= 0; s = nextLive(s + 1)) {
            out.writeObject(keys[s]);
            out.writeObject(values[s]);
            out.writeLong(deadlines[s]);
        }
    }

    @Serial
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        int n = in.readInt();
        if (n < 0) {
            throw new InvalidObjectException("Invalid size " + n);
        }
        clock = SYSTEM_CLOCK;
        initLanes();
        for (int i = 0; i < n; i++) {
            Object key = in.readObject();
            Object value = in.readObject();
            long deadline = in.readLong();
            int h = spread(key);
            int p = find(key, h);
            if (p >= 0) {
                throw new InvalidObjectException("Duplicate key in stream");
            }
            if (growthNeeded()) {
                growTable();
                p = find(key, h);
            }
            insert(p, key, h, value, deadline);
        }
    }

    // ------------------------------------------------------------------ views

    /** Fail-fast slot cursor with the provider's HashMap rules: next slot found on advance, CME before NSEE. */
    private abstract class SlotCursor {
        private int next = nextLive(0);
        private int current = -1;
        private int expectedModCount = modCount;

        public final boolean hasNext() {
            return next >= 0;
        }

        final int advance() {
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            int s = next;
            if (s < 0) {
                throw new NoSuchElementException();
            }
            current = s;
            next = nextLive(s + 1);
            return s;
        }

        public final void remove() {
            int s = current;
            if (s < 0) {
                throw new IllegalStateException();
            }
            if (modCount != expectedModCount) {
                throw new ConcurrentModificationException();
            }
            current = -1;
            removeAt(positionOf(s), s);
            expectedModCount = modCount;
        }
    }

    private final class KeyCursor extends SlotCursor implements Iterator<K> {
        @Override
        public K next() {
            return keyAt(advance());
        }
    }

    private final class ValueCursor extends SlotCursor implements Iterator<V> {
        @Override
        public V next() {
            return valueAt(advance());
        }
    }

    private final class EntryCursor extends SlotCursor implements Iterator<Map.Entry<K, V>> {
        @Override
        public Map.Entry<K, V> next() {
            return new SlotEntry(advance());
        }
    }

    /**
     * Entry flyweight over a slot and its generation. While the mapping it was created for is still in the map it reads
     * and writes the value lane; once that mapping is removed it keeps the last value it saw, like a detached HashMap
     * node, even if the slot is reused for the same key.
     */
    private final class SlotEntry implements Map.Entry<K, V> {
        private final int slot;
        private final int gen;
        private final K key;
        private V value;

        SlotEntry(int s) {
            slot = s;
            gen = gens[s];
            key = keyAt(s);
            value = valueAt(s);
        }

        private boolean attached() {
            return live(slot) && gens[slot] == gen;
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            if (attached()) {
                value = valueAt(slot);
            }
            return value;
        }

        @Override
        public V setValue(V v) {
            V old = getValue();
            value = v;
            if (attached()) {
                values[slot] = v;
            }
            return old;
        }

        @Override
        public boolean equals(Object o) {
            return o == this
                    || o instanceof Map.Entry<?, ?> e && Objects.equals(key, e.getKey())
                            && Objects.equals(getValue(), e.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(getValue());
        }

        @Override
        public String toString() {
            return key + "=" + getValue();
        }
    }

    /** Runs {@code action} on every live slot, then fails if the map changed structurally (HashMap view rule). */
    private void forEachSlot(SlotAction action) {
        if (size == 0) {
            return;
        }
        int expected = modCount;
        for (int s = nextLive(0); s >= 0; s = nextLive(s + 1)) {
            action.visit(s);
        }
        if (modCount != expected) {
            throw new ConcurrentModificationException();
        }
    }

    @FunctionalInterface
    private interface SlotAction {
        void visit(int slot);
    }

    private final class EntryView extends AbstractSet<Map.Entry<K, V>> {
        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            M3PassiveExpiringMap.this.clear();
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new EntryCursor();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            Object key = e.getKey();
            int p = find(key, spread(key));
            if (p < 0) {
                return false;
            }
            int s = slotAt(p);
            return Objects.equals(keys[s], e.getKey()) && Objects.equals(values[s], e.getValue());
        }

        @Override
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry<?, ?> e)) {
                return false;
            }
            Object key = e.getKey();
            Object value = e.getValue();
            int p = find(key, spread(key));
            if (p < 0) {
                return false;
            }
            int s = slotAt(p);
            Object v = values[s];
            if (v != value && (value == null || !value.equals(v))) {
                return false;
            }
            removeAt(p, s);
            return true;
        }

        @Override
        public void forEach(Consumer<? super Map.Entry<K, V>> action) {
            Objects.requireNonNull(action);
            forEachSlot(s -> action.accept(new SlotEntry(s)));
        }
    }

    private final class KeyView extends AbstractSet<K> {
        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            M3PassiveExpiringMap.this.clear();
        }

        @Override
        public Iterator<K> iterator() {
            return new KeyCursor();
        }

        @Override
        public boolean contains(Object o) {
            return find(o, spread(o)) >= 0;
        }

        @Override
        public boolean remove(Object o) {
            int p = find(o, spread(o));
            if (p < 0) {
                return false;
            }
            removeAt(p, slotAt(p));
            return true;
        }

        @Override
        public void forEach(Consumer<? super K> action) {
            Objects.requireNonNull(action);
            forEachSlot(s -> action.accept(keyAt(s)));
        }
    }

    private final class ValueView extends AbstractCollection<V> {
        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            M3PassiveExpiringMap.this.clear();
        }

        @Override
        public Iterator<V> iterator() {
            return new ValueCursor();
        }

        @Override
        public boolean contains(Object o) {
            return slotOfValue(o) >= 0;
        }

        @Override
        public void forEach(Consumer<? super V> action) {
            Objects.requireNonNull(action);
            forEachSlot(s -> action.accept(valueAt(s)));
        }
    }

    /**
     * The provider's entry-set adapter over a slot cursor: {@code reset()} restarts the cursor and keeps the current
     * entry, {@code remove()} clears it, and the entry accessors throw {@link IllegalStateException} without one.
     */
    private final class SlotMapIterator implements MapIterator<K, V>, ResettableIterator<K> {
        private KeyCursor cursor = new KeyCursor();
        private boolean hasEntry;
        private int entrySlot;
        private int entryGen;
        private K entryKey;
        private V entryValue;

        @Override
        public boolean hasNext() {
            return cursor.hasNext();
        }

        @Override
        public K next() {
            int s = cursor.advance();
            hasEntry = true;
            entrySlot = s;
            entryGen = gens[s];
            entryKey = keyAt(s);
            entryValue = valueAt(s);
            return entryKey;
        }

        @Override
        public void remove() {
            cursor.remove();
            hasEntry = false;
            entryKey = null;
            entryValue = null;
        }

        @Override
        public void reset() {
            cursor = new KeyCursor();
        }

        @Override
        public K getKey() {
            requireEntry();
            return entryKey;
        }

        @Override
        public V getValue() {
            requireEntry();
            if (attached()) {
                entryValue = valueAt(entrySlot);
            }
            return entryValue;
        }

        @Override
        public V setValue(V value) {
            V old = getValue();
            entryValue = value;
            if (attached()) {
                values[entrySlot] = value;
            }
            return old;
        }

        private boolean attached() {
            return live(entrySlot) && gens[entrySlot] == entryGen;
        }

        private void requireEntry() {
            if (!hasEntry) {
                throw new IllegalStateException();
            }
        }
    }
}
