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
 * Concurrency Primitives Specification - Lockless AnOP via JDK Concurrency.
 *
 * <h2>CORE PRINCIPLE</h2>
 * <blockquote>
 * "If a user needs to understand concurrency primitives, AnOP has failed."
 * <br><br>
 * Users write annotations.
 * Framework handles correctness.
 * Surface is lockless. Underneath is adaptive and correct.
 * </blockquote>
 *
 * <h2>ANNOTATION TO INVARIANT MAPPING</h2>
 * <pre>
 * Annotation       | Invariant           | JDK Implementation
 * -----------------+---------------------+----------------------------
 * &#64;SingleWriter    | SINGLE_WRITER       | Baton (AtomicBoolean)
 * &#64;Serialized      | TOTAL_ORDERING      | Sequencer (ticket/serving)
 * &#64;Bounded(n)      | BOUNDED_CAPACITY    | Semaphore
 * &#64;Exclusive       | MUTUAL_EXCLUSION    | ThinMutex (CAS)
 * &#64;Observable      | ATOMIC_PUBLICATION  | AtomicReference publish
 * &#64;Cancellable     | COOPERATIVE_STOP    | AtomicBoolean flag
 * </pre>
 *
 * <h2>JDK TOOLBOX TIERS</h2>
 * <pre>
 * Tier 0 (Always prefer):
 * - AtomicBoolean
 * - AtomicInteger
 * - AtomicLong
 * - AtomicReference
 * - VarHandle
 * - Thread.onSpinWait()
 *
 * Tier 1 (For accumulation/queues):
 * - LongAdder
 * - CAS-based queues
 *
 * Tier 2 (For coordination):
 * - Semaphore
 * - CountDownLatch
 * - Phaser
 *
 * Tier 3 (Last resort only):
 * - ReentrantLock (non-fair, short-held)
 * - StampedLock
 * </pre>
 *
 * <h2>ADAPTIVE WAIT STRATEGY</h2>
 * <pre>{@code
 * final class AdaptiveWait {
 *     static void await(BooleanSupplier condition) {
 *         int spins = 0;
 *         while (!condition.getAsBoolean()) {
 *             if (spins++ &lt; 64) Thread.onSpinWait();
 *             else if (spins &lt; 128) Thread.yield();
 *             else LockSupport.parkNanos(1L);
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h2>BATON (Single-Writer)</h2>
 * <pre>{@code
 * final class Baton {
 *     private final AtomicBoolean held = new AtomicBoolean(false);
 *
 *     void enter() {
 *         AdaptiveWait.await(() -> held.compareAndSet(false, true));
 *     }
 *
 *     void exit() {
 *         held.set(false);
 *     }
 * }
 * }</pre>
 *
 * <h2>SEQUENCER (Total Ordering)</h2>
 * <pre>{@code
 * final class Sequencer {
 *     private final AtomicLong ticket = new AtomicLong(0L);
 *     private final AtomicLong serving = new AtomicLong(0L);
 *
 *     long enter() {
 *         long myTicket = ticket.getAndIncrement();
 *         AdaptiveWait.await(() -> serving.get() == myTicket);
 *         return myTicket;
 *     }
 *
 *     void exit(long myTicket) {
 *         serving.incrementAndGet();
 *     }
 * }
 * }</pre>
 *
 * <h2>CAPACITY GATE (Bounded)</h2>
 * <pre>{@code
 * final class CapacityGate {
 *     private final Semaphore permits;
 *
 *     CapacityGate(int max) {
 *         this.permits = new Semaphore(max, false);
 *     }
 *
 *     boolean tryEnter() {
 *         return permits.tryAcquire();
 *     }
 *
 *     void exit() {
 *         permits.release();
 *     }
 * }
 * }</pre>
 *
 * <h2>CANCELLATION FLAG</h2>
 * <pre>{@code
 * final class CancellationFlag {
 *     private final AtomicBoolean cancelled = new AtomicBoolean(false);
 *
 *     boolean isCancelled() {
 *         return cancelled.get();
 *     }
 *
 *     void cancel() {
 *         cancelled.set(true);
 *     }
 * }
 * }</pre>
 *
 * <h2>EXECUTION PLAN COMPOSITION</h2>
 * <pre>
 * Framework compiles ExecutionPlan internally:
 *
 * ExecutionPlan {
 *     writer = Baton
 *     ordering = Sequencer
 *     capacity = Semaphore
 *     waiting = AdaptiveWait
 *     cancellation = AtomicFlag
 *     observation = AtomicPublication
 * }
 * </pre>
 *
 * <h2>WAIT STRATEGY LADDER</h2>
 * <pre>
 * CAS -> SPIN -> YIELD -> PARK -> RESUME
 *
 * Promotion Rules:
 * - Spin is bounded (64 iterations)
 * - Yield follows spin (128 iterations)
 * - Park follows yield
 * - Resume is guaranteed
 * - Cancellation is always checked
 * </pre>
 *
 * <h2>REENTRANTLOCK RULES (LAST RESORT)</h2>
 * <pre>
 * Only use ReentrantLock when ALL conditions met:
 * - Critical section less than 100ns
 * - No IO inside
 * - No callbacks inside
 * - No user code inside
 * - No nesting
 *
 * If any violated -> use higher-level coordination.
 * </pre>
 *
 * <h2>FRAMEWORK RULEBOOK</h2>
 * <pre>
 * 1. Users never see concurrency
 * 2. Annotations select invariants, not mechanisms
 * 3. Fast path is always lock-free
 * 4. Blocking is allowed only off hot paths
 * 5. Execution is serialized per context
 * 6. Coordination beats contention
 * 7. Spin-wait is bounded
 * 8. Promotion paths always exist
 * 9. Progress > lock-free purity
 * 10. Specs exist, but ergonomics dominate
 * </pre>
 *
 * <h2>JIT OPTIMIZATION</h2>
 * <blockquote>
 * "The JIT can only optimize what it can see.
 *  Reflection hides intent.
 *  Generated code reveals it."
 * <br><br>
 * Compile-time AnOP doesn't just reduce overhead -
 * it unlocks optimizations that runtime AOP makes impossible.
 * </blockquote>
 *
 * @since Grand Java Patterns Proof - Concurrency Section
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/spec/ConcurrencySpec.java}
 * blob {@code 94d76539d49692ba74f8f91210da1b036616dc14} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConcurrencySpec {

    /** Default spin iterations before yield. */
    public static final int SPIN_ITERATIONS = 64;

    /** Default yield iterations before park. */
    public static final int YIELD_ITERATIONS = 128;

    /** Maximum critical section time for ReentrantLock (nanoseconds). */
    public static final long MAX_LOCK_CRITICAL_SECTION_NS = 100L;

    private ConcurrencySpec() {
        // Specification only - not instantiable
    }
}
