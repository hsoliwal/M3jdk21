/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

/**
 * Borrowed coalesced invalidation view, valid only inside its listener callback.
 *
 * <p>One view is reused by the runtime. Thread/lifetime checks diagnose ordinary
 * misuse; they cannot distinguish an escaped alias during a later callback.
 * Never retain this view. A snapshot retains metadata, not object payload state.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/event/ObjectEvent.java}
 * blob {@code b24af003ab27936c589f4062f4dd0df6707b56e5} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ObjectEvent {
    private final Thread dispatcher;
    private boolean active;
    private long handle;
    private long referenceMask;
    private long contentMask;
    private long sequence;
    private AsyncObjectEvents.FieldLayout layout;

    ObjectEvent(Thread dispatcher) {
        this.dispatcher = dispatcher;
    }

    void begin(long handle, long referenceMask, long contentMask, long sequence,
               AsyncObjectEvents.FieldLayout layout) {
        this.handle = handle;
        this.referenceMask = referenceMask;
        this.contentMask = contentMask;
        this.sequence = sequence;
        this.layout = layout;
        active = true;
    }

    void end() {
        active = false;
        layout = null;
    }

    private void check() {
        if (Thread.currentThread() != dispatcher || !active) {
            throw new IllegalStateException("ObjectEvent is borrowed only for its callback; retain snapshot() instead");
        }
    }

    public long handle() { check(); return handle; }
    public long referenceMask() { check(); return referenceMask; }
    public long contentMask() { check(); return contentMask; }
    public long changedMask() { check(); return referenceMask | contentMask; }
    /** Monotonic sequence of affected waves for this registration, not a payload version. */
    public long sequence() { check(); return sequence; }
    public AsyncObjectEvents.FieldLayout layout() { check(); return layout; }

    public Snapshot snapshot() {
        check();
        return new Snapshot(handle, referenceMask, contentMask, sequence, layout);
    }

    /** Immutable event metadata. Reading the object later can observe a newer payload. */
    public record Snapshot(long handle, long referenceMask, long contentMask,
                           long sequence, AsyncObjectEvents.FieldLayout layout) {
        public long changedMask() { return referenceMask | contentMask; }
    }
}
