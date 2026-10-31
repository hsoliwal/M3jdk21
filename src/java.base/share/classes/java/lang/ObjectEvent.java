/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file).
 */
package java.lang;

/**
 * An immutable coalesced invalidation, with a lazy latest-value read.
 * Sequences order accepted publications at the receiving observation owner;
 * they do not order every arbitrary concurrent store or preserve old payloads.
 *
 * @since 21
 */
public final class ObjectEvent {
    /** A reference field changed by identity. */
    public static final int REFERENCE = 1;
    /** A primitive field changed by raw bits. */
    public static final int VALUE = 2;
    /** A producer or an explicitly followed child reported content change. */
    public static final int CONTENT = 4;
    /** All supported invalidation kinds. */
    public static final int ALL = REFERENCE | VALUE | CONTENT;

    private final Object source;
    private final M3Object.Field field;
    private final int mask;
    private final long firstSequence;
    private final long sequence;
    private final long generation;
    private final long originId;
    private final long originSequence;
    private final boolean coalesced;

    ObjectEvent(Object source, M3Object.Field field, int mask,
                long firstSequence, long sequence, long generation,
                long originId, long originSequence, boolean coalesced) {
        this.source = source;
        this.field = field;
        this.mask = mask;
        this.firstSequence = firstSequence;
        this.sequence = sequence;
        this.generation = generation;
        this.originId = originId;
        this.originSequence = originSequence;
        this.coalesced = coalesced;
    }

    /**
     * Returns the receiving payload owner, never an inaccessible descendant.
     *
     * @return the receiving payload owner
     */
    public Object source() {
        return source;
    }

    /**
     * Returns this subscriber's authorized field, or null for a root subscription.
     *
     * @return the authorized field, or null for a root subscription
     */
    public M3Object.Field field() {
        return field;
    }

    /**
     * Returns the subscribed invalidation bits included in this delivery.
     *
     * @return the subscribed invalidation bits included in this delivery
     */
    public int mask() {
        return mask;
    }

    /**
     * Returns the first included publication sequence at the receiving owner.
     *
     * @return the first included publication sequence at the receiving owner
     */
    public long firstSequence() {
        return firstSequence;
    }

    /**
     * Returns the last included publication sequence at the receiving owner.
     *
     * @return the last included publication sequence at the receiving owner
     */
    public long sequence() {
        return sequence;
    }

    /**
     * Returns the selected binding generation, or zero for a root event.
     *
     * @return the selected binding generation, or zero for a root event
     */
    public long generation() {
        return generation;
    }

    /**
     * Returns an opaque originating observation identity, or zero when multiple
     * originating owners were coalesced. It is not an address, object reference
     * or stable identifier across observation lifetimes or VMs.
     *
     * @return the originating observation identity, or zero for coalesced origins
     */
    public long originId() {
        return originId;
    }

    /**
     * Returns the last included originating sequence, or zero when originId is
     * zero. No historical originating object is retained by this envelope.
     *
     * @return the last originating sequence, or zero when the origin identity is unknown
     */
    public long originSequence() {
        return originSequence;
    }

    /**
     * Returns whether more than one publication was combined for this subscriber.
     *
     * @return whether more than one publication was combined for this subscriber
     */
    public boolean coalesced() {
        return coalesced;
    }

    /**
     * Reads the latest selected field through the subscriber's authority, or
     * returns source for a root event. The value can differ from its value at
     * publication or from another read of this same event.
     *
     * @return the latest selected field value, or the source for a root event
     */
    public Object currentValue() {
        return field == null ? source : field.get();
    }
}
