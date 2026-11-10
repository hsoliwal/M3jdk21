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
import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Optional cooperative work budget. Child allocations are reserved, never minted or refunded.
 *
 * <p>A single atomic long packs cancellation in bit 63 and remaining units in
 * bits 0..62. Cancel and spend participate in the same atomic modification order;
 * a spend cannot overwrite cancellation. All values 0..Long.MAX_VALUE remain
 * representable. Successful spend/checkpoint operations allocate no internal
 * state objects. The injected clock and the JVM are outside that guarantee.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/GrailBudget.java}
 * blob {@code dc78e19f64dbbeb2cdec321b4242103f916d27c6} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class GrailBudget {
    public enum Reason { CANCELLED, DEADLINE, EXHAUSTED, INTERRUPTED }
    public static final class Stopped extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final Reason reason;
        private Stopped(Reason reason) { super(reason.name()); this.reason = reason; }
        public Reason reason() { return reason; }
    }
    private static final long CANCELLED = Long.MIN_VALUE;
    private static final long UNITS = Long.MAX_VALUE;
    private static final VarHandle REMAINING;
    static {
        try {
            REMAINING = MethodHandles.lookup().findVarHandle(GrailBudget.class, "remaining", long.class);
        } catch (ReflectiveOperationException ex) { throw new ExceptionInInitializerError(ex); }
    }
    private final GrailBudget parent;
    private final LongSupplier clock;
    private final long started;
    private final long timeout;
    private final int depth;
    private volatile long remaining;

    public GrailBudget(long units, Duration timeout) { this(units, timeout, System::nanoTime); }
    /** The injected clock must be monotonic and shared by all children. */
    public GrailBudget(long units, Duration timeout, LongSupplier clock) {
        this(null, units, nanos(timeout), Objects.requireNonNull(clock, "clock"));
    }
    private GrailBudget(GrailBudget parent, long units, long timeout, LongSupplier clock) {
        if (units < 0) throw new IllegalArgumentException("Negative work budget");
        this.parent = parent; this.clock = clock; this.started = clock.getAsLong();
        this.timeout = timeout; this.remaining = units; this.depth = parent == null ? 0 : parent.depth + 1;
    }
    public long remaining() { return current() & UNITS; }
    private long current() { return (long) REMAINING.getAcquire(this); }
    public void cancel() { REMAINING.getAndBitwiseOr(this, CANCELLED); }
    /** Does not clear interruption; cancellation/deadlines propagate from every ancestor. */
    public void checkpoint() {
        if (Thread.currentThread().isInterrupted()) throw new Stopped(Reason.INTERRUPTED);
        for (GrailBudget cursor = this; cursor != null; cursor = cursor.parent) {
            if (cursor.current() < 0) throw new Stopped(Reason.CANCELLED);
            if (clock.getAsLong() - cursor.started >= cursor.timeout) throw new Stopped(Reason.DEADLINE);
        }
    }
    public void spend(long units) {
        if (units <= 0) throw new IllegalArgumentException("Spend must be positive");
        for (;;) {
            checkpoint();
            long before = current();
            if (before < 0) throw new Stopped(Reason.CANCELLED);
            if (before < units) throw new Stopped(Reason.EXHAUSTED);
            if (REMAINING.compareAndSet(this, before, before - units)) return;
        }
    }
    /** Reservations are deliberately non-refundable; a cancelled child cannot double-spend. */
    public GrailBudget split(long units, Duration timeout) {
        long childTimeout = nanos(timeout);
        if (depth >= 63) throw new IllegalArgumentException("Budget nesting exceeds 64");
        spend(units);
        return new GrailBudget(this, units, childTimeout, clock);
    }
    static long nanos(Duration duration) {
        long value = Objects.requireNonNull(duration, "duration").toNanos();
        if (value < 0 || value > Long.MAX_VALUE / 2) throw new IllegalArgumentException("Invalid duration");
        return value;
    }
}
