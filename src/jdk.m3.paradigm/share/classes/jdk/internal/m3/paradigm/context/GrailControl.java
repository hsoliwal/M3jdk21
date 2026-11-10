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
 * Host-owned cooperative execution limits, shared across nested work. Charges are attempts,
 * not refunds: if an ancestor rejects a charge, descendant charges remain conservatively spent.
 * A callback must cooperate; this class cannot forcibly interrupt arbitrary Java code or I/O.
 *
 * <p>Cancellation and the full nonnegative long budget share one atomic word:
 * bit 63 is cancellation, bits 0..62 are remaining units. No per-charge state
 * object, atomic wrapper or listener is allocated. A successful local CAS is
 * the local debit linearization point; ancestor charging is deliberately not
 * one multiword transaction. Partial descendant charges are not refunded.
 * Clocks are host callbacks; JVM scheduling, GC and arbitrary callbacks/I/O are
 * outside this algorithmic nonblocking guarantee.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/grail/GrailControl.java}
 * blob {@code 68a9a92f8ec142bc7778a61b8cbe59ad9a89208a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class GrailControl {
    public enum Reason { CANCELLED, DEADLINE, EXHAUSTED, INTERRUPTED }
    public static final class Halted extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final Reason reason;
        private Halted(Reason reason) { super("Context execution halted: " + reason); this.reason = reason; }
        public Reason reason() { return reason; }
    }
    private static final long CANCELLED = Long.MIN_VALUE;
    private static final long UNITS = Long.MAX_VALUE;
    private static final VarHandle USAGE;
    static {
        try { USAGE = MethodHandles.lookup().findVarHandle(GrailControl.class, "usage", long.class); }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private final GrailControl parent;
    private final LongSupplier clock;
    private final long started, timeout;
    private final int depth;
    private volatile long usage;

    public GrailControl(long units, Duration timeout) { this(units, timeout, System::nanoTime); }
    /** The clock is monotonic and host-supplied, permitting deterministic deadline tests. */
    public GrailControl(long units, Duration timeout, LongSupplier clock) {
        this(null, units, nanos(Objects.requireNonNull(timeout)), Objects.requireNonNull(clock));
    }
    private GrailControl(GrailControl parent, long units, long timeout, LongSupplier clock) {
        if (units < 0) throw new IllegalArgumentException("Negative work limit");
        this.parent = parent;
        this.clock = clock;
        this.started = clock.getAsLong();
        this.timeout = timeout;
        this.depth = parent == null ? 0 : parent.depth + 1;
        if (depth > 32) throw new IllegalArgumentException("Control nesting exceeds 32");
        this.usage = units;
    }
    /** Compatibility entry point: no time deadline, effectively unbounded work, still cancellable. */
    public static GrailControl unlimited() { return new GrailControl(null, Long.MAX_VALUE, -1, System::nanoTime); }
    public GrailControl child(long units, Duration timeout) {
        checkpoint();
        if (units < 0 || units > remaining()) throw new IllegalArgumentException("Child work limit exceeds parent");
        return new GrailControl(this, units, nanos(Objects.requireNonNull(timeout)), clock);
    }
    public long remaining() { return current() & UNITS; }
    public void cancel() {
        // An atomic OR never loses a concurrent debit and cannot resurrect a cancelled scope.
        USAGE.getAndBitwiseOr(this, CANCELLED);
    }
    /** Checks this scope and every ancestor, without clearing the Java interrupt flag. */
    public void checkpoint() {
        if (Thread.currentThread().isInterrupted()) throw new Halted(Reason.INTERRUPTED);
        for (GrailControl scope = this; scope != null; scope = scope.parent) {
            if (scope.current() < 0) throw new Halted(Reason.CANCELLED);
            if (scope.timeout >= 0 && scope.clock.getAsLong() - scope.started >= scope.timeout)
                throw new Halted(Reason.DEADLINE);
        }
    }
    /** A successful charge fits every ancestor. Siblings cannot mint extra root work. */
    public void charge(long units) {
        if (units < 0) throw new IllegalArgumentException("Negative work charge");
        checkpoint();
        for (GrailControl scope = this; scope != null; scope = scope.parent) scope.debit(units);
        checkpoint();
    }
    private void debit(long units) {
        while (true) {
            long before = current();
            if (before < 0) throw new Halted(Reason.CANCELLED);
            if (units > before) throw new Halted(Reason.EXHAUSTED);
            // Bounds were checked before subtraction, so it cannot borrow into the flag bit.
            if (units == 0 || USAGE.compareAndSet(this, before, before - units)) return;
        }
    }
    private long current() { return (long) USAGE.getAcquire(this); }
    private static long nanos(Duration duration) {
        long value = duration.toNanos();
        if (value < 0 || value > Long.MAX_VALUE / 2) throw new IllegalArgumentException("Invalid deadline duration");
        return value;
    }
}
