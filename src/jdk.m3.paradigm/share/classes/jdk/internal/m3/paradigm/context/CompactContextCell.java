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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Objects;
import java.util.UUID;

/**
 * One-reference, opt-in context storage primitive.
 *
 * <p>The cell itself retains exactly one reference: the currently published snapshot.
 * Snapshot lifecycle is packed into the low two bits of one long together with the
 * revision. Identity is retained as UUID rather than an eagerly-created String.
 *
 * <p>The empty snapshot is shared. Each successful semantic transition publishes a
 * fresh snapshot, so an old expected token cannot become current again. CAS methods
 * make one attempt; retry policy belongs to the caller.
 *
 * @param <V> immutable-or-caller-owned payload type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/CompactContextCell.java}
 * blob {@code 1e49e3528110cd0d7d6f77d0b2cf9cf193515f8b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class CompactContextCell<V> {
    private static final int PHASE_BITS = 2;
    private static final long PHASE_MASK = (1L << PHASE_BITS) - 1L;
    private static final long MAX_REVISION = Long.MAX_VALUE >>> PHASE_BITS;

    public enum Phase {
        ACTIVE(0),
        FROZEN(1),
        DISPOSED(2);

        private final int code;

        Phase(int code) {
            this.code = code;
        }

        int code() {
            return code;
        }

        static Phase fromCode(long code) {
            return switch ((int) code) {
                case 0 -> ACTIVE;
                case 1 -> FROZEN;
                case 2 -> DISPOSED;
                default -> throw new IllegalStateException(
                        "Unknown compact context phase code: " + code);
            };
        }
    }

    /** Immutable publication envelope. No enum or identity String is retained. */
    public static final class Snapshot<V> {
        private final long stateWord;
        private final V value;
        private final UUID uid;

        private Snapshot(long revision, V value, UUID uid, Phase phase) {
            Objects.requireNonNull(phase, "phase");
            if (revision < 0L || revision > MAX_REVISION) {
                throw new IllegalArgumentException("revision outside packed range: " + revision);
            }
            this.stateWord = (revision << PHASE_BITS) | phase.code();
            this.value = value;
            this.uid = uid;
        }

        public long revision() {
            return stateWord >>> PHASE_BITS;
        }

        public V value() {
            return value;
        }

        /** Null until identity is initialized. String materialization is a cold boundary. */
        public String initializedId() {
            return uid == null ? null : uid.toString();
        }

        /** Null until identity is initialized; avoids String materialization. */
        public UUID initializedUid() {
            return uid;
        }

        public Phase phase() {
            return Phase.fromCode(stateWord & PHASE_MASK);
        }

        long stateWord() {
            return stateWord;
        }
    }

    private static final Snapshot<?> EMPTY =
            new Snapshot<>(0L, null, null, Phase.ACTIVE);
    private static final VarHandle STATE;

    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(
                    CompactContextCell.class, "state", Snapshot.class);
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    // Deliberately the only instance field in the context-cell shell.
    @SuppressWarnings("unused")
    private volatile Snapshot<V> state;

    @SuppressWarnings("unchecked")
    public CompactContextCell() {
        state = (Snapshot<V>) EMPTY;
    }

    public CompactContextCell(V initialValue) {
        this();
        if (initialValue != null) {
            state = new Snapshot<>(0L, initialValue, null, Phase.ACTIVE);
        }
    }

    /** Caller-supplied durable identity; UUID is retained without allocating its String form. */
    public CompactContextCell(UUID uid, V initialValue) {
        state = new Snapshot<>(
                0L,
                initialValue,
                Objects.requireNonNull(uid, "uid"),
                Phase.ACTIVE);
    }

    /** Exact durable rehydration without replaying mutations. */
    protected CompactContextCell(
            UUID uid,
            V value,
            long revision,
            Phase phase) {
        UUID checkedUid = Objects.requireNonNull(uid, "uid");
        Phase checkedPhase = Objects.requireNonNull(phase, "phase");
        state = new Snapshot<>(
                revision,
                checkedPhase == Phase.DISPOSED ? null : value,
                checkedUid,
                checkedPhase);
    }

    @SuppressWarnings("unchecked")
    public final Snapshot<V> snapshot() {
        return (Snapshot<V>) STATE.getAcquire(this);
    }

    public final V value() {
        return snapshot().value;
    }

    /**
     * Lazily establishes stable identity without constructing a String.
     *
     * <p>Identity initialization replaces the expected token but does not advance the
     * semantic revision.
     */
    public final UUID uid() {
        UUID candidate = null;
        for (;;) {
            Snapshot<V> before = snapshot();
            if (before.uid != null) {
                return before.uid;
            }
            if (candidate == null) {
                candidate = UUID.randomUUID();
            }
            Snapshot<V> after = new Snapshot<>(
                    before.revision(),
                    before.value,
                    candidate,
                    before.phase());
            if (STATE.compareAndSet(this, before, after)) {
                return candidate;
            }
        }
    }

    /** Compatibility String identity, materialized only at the API boundary. */
    public final String id() {
        return uid().toString();
    }

    public final boolean trySet(
            Snapshot<V> expected,
            V replacement) {
        Objects.requireNonNull(expected, "expected");
        if (expected.phase() != Phase.ACTIVE || snapshot() != expected) {
            return false;
        }
        Snapshot<V> after = new Snapshot<>(
                nextRevision(expected.revision()),
                replacement,
                expected.uid,
                Phase.ACTIVE);
        return STATE.compareAndSet(this, expected, after);
    }

    public final boolean tryFreeze(Snapshot<V> expected) {
        Objects.requireNonNull(expected, "expected");
        if (expected.phase() != Phase.ACTIVE || snapshot() != expected) {
            return false;
        }
        Snapshot<V> after = new Snapshot<>(
                nextRevision(expected.revision()),
                expected.value,
                expected.uid,
                Phase.FROZEN);
        return STATE.compareAndSet(this, expected, after);
    }

    /** Releases this cell's payload reference, not snapshots retained by readers. */
    public final boolean tryDispose(Snapshot<V> expected) {
        Objects.requireNonNull(expected, "expected");
        if (expected.phase() == Phase.DISPOSED || snapshot() != expected) {
            return false;
        }
        Snapshot<V> after = new Snapshot<>(
                nextRevision(expected.revision()),
                null,
                expected.uid,
                Phase.DISPOSED);
        return STATE.compareAndSet(this, expected, after);
    }

    private static long nextRevision(long revision) {
        if (revision >= MAX_REVISION) {
            throw new ArithmeticException("compact context revision exhausted");
        }
        return revision + 1L;
    }
}
