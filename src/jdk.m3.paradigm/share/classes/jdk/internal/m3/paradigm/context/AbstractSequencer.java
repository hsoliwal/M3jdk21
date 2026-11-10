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

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.collections.TokenInterner;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * AbstractSequencer - Base implementation for ISequencer.
 *
 * <p>Provides lock-free FIFO ordering via ticket/serving atomics with template methods:
 * <ul>
 *   <li>AtomicLong for ticket issuance (getAndIncrement)</li>
 *   <li>AtomicLong for serving counter</li>
 *   <li>Adaptive wait strategy (spin → yield → park)</li>
 *   <li>No starvation guarantee via FIFO ordering</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doEnter(long)} - Custom enter logic after acquiring slot</li>
 *   <li>{@link #doExit(long)} - Custom exit logic before releasing</li>
 *   <li>{@link #onEnter(long)} - Callback when slot acquired</li>
 *   <li>{@link #onExit(long)} - Callback when slot released</li>
 *   <li>{@link #onWait(long, long)} - Callback during wait</li>
 * </ul>
 *
 * <h2>Algorithm</h2>
 * <pre>
 * enter():
 *   myTicket = ticket.getAndIncrement()      // Lock-free
 *   while (serving.get() != myTicket):       // Adaptive wait
 *     wait()
 *   return myTicket
 *
 * exit(myTicket):
 *   serving.incrementAndGet()                // Release slot
 * </pre>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/concurrency/AbstractSequencer.java}
 * blob {@code 6edb676f24f64287861b21b5f97d32ac87525076} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractSequencer implements ISequencer {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractSequencer.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String PREFIX_SEQUENCER = INTERNER.intern("Sequencer[serving=");
    private static final String NEXT_TICKET_EQ = INTERNER.intern(", nextTicket=");
    private static final String WAITING_EQ = INTERNER.intern(", waiting=");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");

    // --- CONSTANTS ---
    private static final int SPIN_LIMIT = 10;
    private static final int YIELD_LIMIT = 100;
    private static final long PARK_NANOS = 1000L;

    private final AtomicLong ticket = new AtomicLong(0L);
    private final AtomicLong serving = new AtomicLong(0L);

    /** Create a sequencer. */
    protected AbstractSequencer() {
        // Default constructor
    }

    @Override
    public long enter() {
        long myTicket = ticket.getAndIncrement();
        log.debug("Acquired ticket: {}", myTicket);
        int attempts = 0;
        // NFL: ticket spin-wait loop — concurrency sequencer — loop preserved per M3 §3
        while (serving.get() != myTicket) {
            attempts++;
            onWait(myTicket, attempts);
            adaptiveWait(attempts);
        }
        log.debug("Ticket {} granted after {} attempts", myTicket, attempts);
        doEnter(myTicket);
        onEnter(myTicket);
        return myTicket;
    }

    @Override
    public void exit(long myTicket) {
        log.debug("Releasing ticket: {}", myTicket);
        doExit(myTicket);
        serving.incrementAndGet();
        onExit(myTicket);
        log.debug("Ticket {} released, now serving: {}", myTicket, serving.get());
    }

    @Override
    public long currentServing() {
        return serving.get();
    }

    @Override
    public long nextTicket() {
        return ticket.get();
    }

    /**
     * Adaptive wait strategy.
     *
     * @param attempts the number of wait attempts
     */
    private void adaptiveWait(int attempts) {
        if (attempts <= SPIN_LIMIT) {
            Thread.onSpinWait();
        } else if (attempts <= YIELD_LIMIT) {
            Thread.yield();
        } else {
            LockSupport.parkNanos(PARK_NANOS);
        }
    }

    // --- Template Methods (Override for Custom Behavior) ---

    /**
     * Called after acquiring slot.
     *
     * @param ticketNumber the acquired ticket number
     */
    protected void doEnter(long ticketNumber) {
        // Default: no-op
    }

    /**
     * Called before releasing slot.
     *
     * @param ticketNumber the ticket being released
     */
    protected void doExit(long ticketNumber) {
        // Default: no-op
    }

    /**
     * Callback when slot is acquired.
     *
     * @param ticketNumber the acquired ticket number
     */
    protected void onEnter(long ticketNumber) {
        // Default: no-op
    }

    /**
     * Callback when slot is released.
     *
     * @param ticketNumber the released ticket number
     */
    protected void onExit(long ticketNumber) {
        // Default: no-op
    }

    /**
     * Callback during wait.
     *
     * @param myTicket the ticket being waited for
     * @param attempts the number of wait attempts
     */
    protected void onWait(long myTicket, long attempts) {
        // Default: no-op
    }

    // --- Accessors for Subclasses ---

    /**
     * Get the ticket counter.
     *
     * @return the AtomicLong ticket counter
     */
    protected AtomicLong getTicket() {
        return ticket;
    }

    /**
     * Get the serving counter.
     *
     * @return the AtomicLong serving counter
     */
    protected AtomicLong getServing() {
        return serving;
    }

    /**
     * Get the queue length (waiting count).
     *
     * @return number of waiting threads
     */
    protected long queueLength() {
        return ticket.get() - serving.get();
    }

    /**
     * Check if sequencer is idle (no waiters).
     *
     * @return true if no threads waiting
     */
    protected boolean isIdle() {
        return ticket.get() == serving.get();
    }

    /**
     * Get the spin limit.
     *
     * @return spin limit before yielding
     */
    protected int getSpinLimit() {
        return SPIN_LIMIT;
    }

    /**
     * Get the yield limit.
     *
     * @return yield limit before parking
     */
    protected int getYieldLimit() {
        return YIELD_LIMIT;
    }

    /**
     * Reset the sequencer (use with caution).
     * <p>
     * Only safe when no threads are waiting.
     */
    protected void reset() {
        ticket.set(0L);
        serving.set(0L);
    }

    @Override
    public String toString() {
        return PREFIX_SEQUENCER + serving.get() +
                NEXT_TICKET_EQ + ticket.get() +
                WAITING_EQ + queueLength() + BRACKET_CLOSE;
    }
}
