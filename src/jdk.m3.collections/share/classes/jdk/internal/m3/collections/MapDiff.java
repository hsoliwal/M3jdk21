/* *****************************************************************************
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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

/**
 * The difference of two maps: the keys only the left has, the keys only the right has, the keys both map to
 * equivalent values, and the keys both map to different values.
 *
 * <p>Every key is kept once in a key lane, grouped by kind, with its left and right value in two lanes beside it and
 * five offsets that bound the four groups. No entry object is stored; map views are built only when asked for. Inside
 * a group the keys keep the iteration order of the left map (of the right map for the keys only it has). Keys are
 * looked up in the maps themselves, so the maps decide key equality; values are compared with the given equivalence,
 * which is called once per shared key. Null keys and values are allowed when the maps allow them. One pass over each
 * map.</p>
 *
 * <p>Provider comparison: Guava {@code Maps.difference} and {@code MapDifference} (Apache-2.0): the same four groups
 * in the same order. Not a {@code java.util} replacement.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class MapDiff<K, V> {
    /** Kind: the key is only in the left map. */
    public static final int ONLY_LEFT = 0;
    /** Kind: the key is only in the right map. */
    public static final int ONLY_RIGHT = 1;
    /** Kind: both maps have the key with equivalent values. */
    public static final int SAME = 2;
    /** Kind: both maps have the key with different values. */
    public static final int DIFFERENT = 3;
    private static final int KINDS = 4;
    private static final byte SHARED_WITH_LEFT = -1;

    /** Storage lane {@code keyLane}: every key once, grouped by kind. */
    private final Object[] keyLane;
    /** Storage lane {@code leftLane}: the left value of each key; null for a key only the right map has. */
    private final Object[] leftLane;
    /** Storage lane {@code rightLane}: the right value of each key; null for a key only the left map has. */
    private final Object[] rightLane;
    /** Storage lane {@code boundLane}: the start of each kind's group, then the total. */
    private final int[] boundLane;

    private MapDiff(Object[] keys, Object[] lefts, Object[] rights, int[] bounds) {
        keyLane = keys;
        leftLane = lefts;
        rightLane = rights;
        boundLane = bounds;
    }

    /**
     * Compares two maps; values are compared with {@link Objects#equals(Object, Object)}.
     *
     * @param left the map before, or on the left
     * @param right the map after, or on the right
     * @param <K> the key type
     * @param <V> the value type
     * @return the difference; it does not change when the maps change later
     */
    public static <K, V> MapDiff<K, V> between(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right) {
        return between(left, right, Objects::equals);
    }

    /**
     * Compares two maps with an explicit value equivalence.
     *
     * @param left the map before, or on the left
     * @param right the map after, or on the right
     * @param equivalent whether two values of a shared key count as the same; called once per shared key
     * @param <K> the key type
     * @param <V> the value type
     * @return the difference; it does not change when the maps change later
     */
    public static <K, V> MapDiff<K, V> between(Map<? extends K, ? extends V> left, Map<? extends K, ? extends V> right,
            BiPredicate<? super V, ? super V> equivalent) {
        Objects.requireNonNull(equivalent, "equivalent");
        Object[] leftEntries = Objects.requireNonNull(left, "left").entrySet().toArray();
        Object[] rightEntries = Objects.requireNonNull(right, "right").entrySet().toArray();
        byte[] kinds = new byte[leftEntries.length + rightEntries.length];
        Object[] others = new Object[leftEntries.length];
        int[] counts = new int[KINDS];
        classifyLeft(leftEntries, right, equivalent, kinds, others, counts);
        classifyRight(rightEntries, left, kinds, leftEntries.length, counts);
        return laidOut(leftEntries, rightEntries, kinds, others, counts);
    }

    /** Returns whether the maps are equal: no key on one side only and no differing value. */
    public boolean areEqual() {
        return count(SAME) == keyLane.length;
    }

    /** Returns the number of keys of {@code kind}. */
    public int count(int kind) {
        Objects.checkIndex(kind, KINDS);
        return boundLane[kind + 1] - boundLane[kind];
    }

    /** Returns the key at {@code index} of the group of {@code kind}. */
    @SuppressWarnings("unchecked")
    public K key(int kind, int index) {
        return (K) keyLane[at(kind, index)];
    }

    /** Returns the left value of the key at {@code index} of the group of {@code kind}; null when only right. */
    @SuppressWarnings("unchecked")
    public V left(int kind, int index) {
        return (V) leftLane[at(kind, index)];
    }

    /** Returns the right value of the key at {@code index} of the group of {@code kind}; null when only left. */
    @SuppressWarnings("unchecked")
    public V right(int kind, int index) {
        return (V) rightLane[at(kind, index)];
    }

    /** Returns the entries only the left map has, as an unmodifiable map in the left map's order. */
    public Map<K, V> onlyLeft() {
        return view(ONLY_LEFT, leftLane);
    }

    /** Returns the entries only the right map has, as an unmodifiable map in the right map's order. */
    public Map<K, V> onlyRight() {
        return view(ONLY_RIGHT, rightLane);
    }

    /** Returns the entries both maps have with equivalent values (the left values), as an unmodifiable map. */
    public Map<K, V> common() {
        return view(SAME, leftLane);
    }

    /** Returns the left values of the keys whose values differ, as an unmodifiable map. */
    public Map<K, V> differingLeft() {
        return view(DIFFERENT, leftLane);
    }

    /** Returns the right values of the keys whose values differ, as an unmodifiable map. */
    public Map<K, V> differingRight() {
        return view(DIFFERENT, rightLane);
    }

    private int at(int kind, int index) {
        return boundLane[kind] + Objects.checkIndex(index, count(kind));
    }

    @SuppressWarnings("unchecked")
    private Map<K, V> view(int kind, Object[] valueLane) {
        Map<K, V> map = new LinkedHashMap<>();
        for (int at = boundLane[kind]; at < boundLane[kind + 1]; at++) {
            map.put((K) keyLane[at], (V) valueLane[at]);
        }
        return Collections.unmodifiableMap(map);
    }

    /** Decides the kind of every left entry and remembers the right value of a shared key. */
    @SuppressWarnings("unchecked")
    private static <V> void classifyLeft(Object[] entries, Map<?, ? extends V> right,
            BiPredicate<? super V, ? super V> equivalent, byte[] kinds, Object[] others, int[] counts) {
        for (int i = 0; i < entries.length; i++) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entries[i];
            V other = right.get(entry.getKey());
            int kind = ONLY_LEFT;
            if (other != null || right.containsKey(entry.getKey())) {
                kind = equivalent.test((V) entry.getValue(), other) ? SAME : DIFFERENT;
                others[i] = other;
            }
            kinds[i] = (byte) kind;
            counts[kind]++;
        }
    }

    /** Marks the right entries whose key the left map lacks; the others were handled from the left. */
    private static void classifyRight(Object[] entries, Map<?, ?> left, byte[] kinds, int offset, int[] counts) {
        for (int i = 0; i < entries.length; i++) {
            boolean only = !left.containsKey(((Map.Entry<?, ?>) entries[i]).getKey());
            kinds[offset + i] = only ? ONLY_RIGHT : SHARED_WITH_LEFT;
            if (only) {
                counts[ONLY_RIGHT]++;
            }
        }
    }

    /** Lays the classified entries out by kind: a counting sort that keeps the order inside each kind. */
    private static <K, V> MapDiff<K, V> laidOut(Object[] leftEntries, Object[] rightEntries, byte[] kinds,
            Object[] others, int[] counts) {
        int[] bounds = new int[KINDS + 1];
        for (int kind = 0; kind < KINDS; kind++) {
            bounds[kind + 1] = bounds[kind] + counts[kind];
        }
        Object[] keys = new Object[bounds[KINDS]];
        Object[] lefts = new Object[bounds[KINDS]];
        Object[] rights = new Object[bounds[KINDS]];
        int[] cursor = Arrays.copyOf(bounds, KINDS);
        for (int i = 0; i < leftEntries.length; i++) {
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) leftEntries[i];
            int at = cursor[kinds[i]]++;
            keys[at] = entry.getKey();
            lefts[at] = entry.getValue();
            rights[at] = others[i];
        }
        placeRight(rightEntries, kinds, leftEntries.length, keys, rights, cursor[ONLY_RIGHT]);
        return new MapDiff<>(keys, lefts, rights, bounds);
    }

    private static void placeRight(Object[] entries, byte[] kinds, int offset, Object[] keys, Object[] rights, int at) {
        for (int i = 0; i < entries.length; i++) {
            if (kinds[offset + i] == ONLY_RIGHT) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) entries[i];
                keys[at] = entry.getKey();
                rights[at++] = entry.getValue();
            }
        }
    }
}
