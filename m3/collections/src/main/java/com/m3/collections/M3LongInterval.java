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
 * M3 five-library superset scaffold {@code M3LongInterval}: API group {@code LONG_INTERVAL} over arrays and state masks.
 *
 * <p>Provider lineage (LIBRARY_API_MATRIX.tsv, run 20261017-synexia-to-public/LIB-SUPERSET):
 * <ul>
 *   <li>ECLIPSE Interval/IntInterval/LongInterval: arithmetic progression as immutable list; O(1) storage [license EPL-2.0|EDL-1.0; NEW_OWNER (LS7; trivial owner, pure offset arithmetic) (adjacent #1171/#1184 = long interval SETS, not EC arithmetic Interval); priority P2; matrix owner PROPOSED:M3LongInterval (from,to,step closed form)]</li>
 * </ul>
 *
 * <p>DOOP mapping: lane kind LONG; lanes {@code long from;long to;long step}; offset scheme no lanes: index -&gt; from + index*step; state-mask bits FROZEN; stored nodes NONE; API entry objects NONE (transient facades over slots, never the stored representation); matrix note: NO lanes: from/step/count scalars; index-&gt;value arithmetic / FROZEN inherent.
 *
 * <p>Status: SCAFFOLD. Every public atom throws {@link M3NotYetReceived} until this owner is received through a hash-pinned Synexia recipe with its own
 * provider oracle (never inferred from another provider). Not a {@code java.util} replacement.
 */
public final class M3LongInterval {
    /** State-mask bit {@code FROZEN} (per-slot state word, bit 0). */
    static final long STATE_FROZEN = 1L << 0;

    /** Storage lane {@code from}: scalar lane. */
    private long from;
    /** Storage lane {@code to}: scalar lane. */
    private long to;
    /** Storage lane {@code step}: scalar lane. */
    private long step;
    /** Per-slot state words (FROZEN); one bit set per slot and state. */
    private long[] stateMaskWords = new long[0];
    /** Live slot count. */
    private int size;

    /** Creates an empty scaffold owner; lanes grow by copy once received. */
    public M3LongInterval() {
    }

    /** Reports the declared state-mask bits and lanes for ledgers; never allocates per element. */
    public static String scaffoldDescriptor() {
        return "M3LongInterval|LONG_INTERVAL|LONG|FROZEN|long from;long to;long step";
    }

    /** LongInterval.fromTo (scaffold: not yet received). */
    public static M3LongInterval fromTo(long from, long to) {
        throw new M3NotYetReceived("M3LongInterval.fromTo");
    }

    /** LongInterval.fromToBy (scaffold: not yet received). */
    public static M3LongInterval fromToBy(long from, long to, long step) {
        throw new M3NotYetReceived("M3LongInterval.fromToBy");
    }

    /** LongInterval.zeroTo (scaffold: not yet received). */
    public static M3LongInterval zeroTo(long to) {
        throw new M3NotYetReceived("M3LongInterval.zeroTo");
    }

    /** LongInterval.getFirst (scaffold: not yet received). */
    public long getFirst() {
        throw new M3NotYetReceived("M3LongInterval.getFirst");
    }

    /** LongInterval.getLast (scaffold: not yet received). */
    public long getLast() {
        throw new M3NotYetReceived("M3LongInterval.getLast");
    }

    /** LongInterval.get (scaffold: not yet received). */
    public long get(int index) {
        throw new M3NotYetReceived("M3LongInterval.get");
    }

    /** LongInterval.size (scaffold: not yet received). */
    public int size() {
        throw new M3NotYetReceived("M3LongInterval.size");
    }

    /** LongInterval.contains (scaffold: not yet received). */
    public boolean contains(long value) {
        throw new M3NotYetReceived("M3LongInterval.contains");
    }

    /** LongInterval.indexOf (scaffold: not yet received). */
    public int indexOf(long value) {
        throw new M3NotYetReceived("M3LongInterval.indexOf");
    }

    /** LongInterval.reverseThis (scaffold: not yet received). */
    public M3LongInterval reverseThis() {
        throw new M3NotYetReceived("M3LongInterval.reverseThis");
    }

    /** LongInterval.toArray (scaffold: not yet received). */
    public long[] toArray() {
        throw new M3NotYetReceived("M3LongInterval.toArray");
    }

    /** LongInterval.forEach (scaffold: not yet received). */
    public void forEach(java.util.function.LongConsumer action) {
        throw new M3NotYetReceived("M3LongInterval.forEach");
    }
}
