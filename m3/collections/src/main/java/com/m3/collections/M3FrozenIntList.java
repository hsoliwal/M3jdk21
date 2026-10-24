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
package com.m3.collections;

/**
 * M3 five-library superset scaffold {@code M3FrozenIntList}: API group {@code FROZEN_INT_LIST} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>JDK List.of/Set.of/Map.of: JEP269 immutable; UOE mutate; null-hostile; dup-reject [license GPLv2+CPE; NEW_OWNER (LS1); priority P1; matrix owner PROPOSED:M3FrozenLongList/Set/Map]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind INT; lanes {@code int[] valueLane}; offset scheme ordinal slot (dense 0..size); state-mask bits FROZEN; stored nodes NONE; API entry objects TRANSIENT_FACADE(Map.Entry) (transient facades over slots, never the stored representation); matrix note: frozen sorted/dense lanes; ordinal offsets; build-time copy / FROZEN=1 word; occupancy implicit ALL.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3FrozenIntList {
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    /** Storage lane {@code valueLane}: dynamic primitive/reference array lane. */
    private int[] valueLane = new int[0];
    /** Per-slot state words (FROZEN); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3FrozenIntList() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3FrozenIntList|FROZEN_INT_LIST|INT|FROZEN|int[] valueLane";
    }

    /** JDK List.copyOf (int lane) (scaffold: not yet received). */
    public static M3FrozenIntList copyOf(int[] source) {
        throw new M3NotYetReceived("M3FrozenIntList.copyOf");
    }

    /** List.size (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3FrozenIntList.size");
    }

    /** List.get (primitive) (scaffold: not yet received). */
    public int get(int index) {
        throw new M3NotYetReceived("M3FrozenIntList.get");
    }

    /** List.indexOf (scaffold: not yet received). */
    public int indexOf(int value) {
        throw new M3NotYetReceived("M3FrozenIntList.indexOf");
    }

    /** List.toArray (defensive copy) (scaffold: not yet received). */
    public int[] toArray() {
        throw new M3NotYetReceived("M3FrozenIntList.toArray");
    }

    /** Iterable.forEach (scaffold: not yet received). */
    public void forEach(java.util.function.IntConsumer action) {
        throw new M3NotYetReceived("M3FrozenIntList.forEach");
    }

    /** List.iterator (scaffold: not yet received). */
    public java.util.PrimitiveIterator.OfInt iterator() {
        throw new M3NotYetReceived("M3FrozenIntList.iterator");
    }

    /** UOE contract once received (JEP269) (scaffold: not yet received). */
    public boolean add(int value) {
        throw new M3NotYetReceived("M3FrozenIntList.add");
    }
}
