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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

/**
 * AbstractBaton - Base implementation for IBaton.
 *
 * <p>Provides lock-free single-writer enforcement with template method pattern:
 * <ul>
 *   <li>AtomicBoolean for lock-free CAS acquisition</li>
 *   <li>Adaptive wait strategy (spin → yield → park)</li>
 *   <li>Progress guarantee via park promotion</li>
 * </ul>
 *
 * <h2>Template Method Pattern</h2>
 * <p>Subclasses can override:
 * <ul>
 *   <li>{@link #doEnter()} - Custom enter logic (called after acquiring)</li>
 *   <li>{@link #doTryEnter()} - Custom try-enter logic (called after successful CAS)</li>
 *   <li>{@link #doExit()} - Custom exit logic (called before releasing)</li>
 *   <li>{@link #onEnter()} - Callback when baton acquired</li>
 *   <li>{@link #onExit()} - Callback when baton released</li>
 *   <li>{@link #onSpinWait(int)} - Callback during spin wait</li>
 * </ul>
 *
 * <h2>Adaptive Wait Strategy</h2>
 * <pre>
 * Phase 1 (0-10 attempts):   Thread.onSpinWait()  - CPU hint
 * Phase 2 (11-100 attempts): Thread.yield()      - Cooperate with scheduler
 * Phase 3 (101+ attempts):   LockSupport.parkNanos(1000) - Short park
 * </pre>
 *
 * @since ContextOS 1.1
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/concurrency/AbstractBaton.java}
 * blob {@code ef8133a936f29c889cab23349a2fe160d95dcae3} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractBaton implements IBaton {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractBaton.class);


    // --- INTERNED STRING CONSTANTS ---
    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    private static final String PREFIX_BATON = INTERNER.intern("Baton[held=");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");

    // --- CONSTANTS ---
    private static final int SPIN_LIMIT = 10;
    private static final int YIELD_LIMIT = 100;
    private static final long PARK_NANOS = 1000L;

    private final AtomicBoolean held = new AtomicBoolean(false);

    /** Create a baton. */
    protected AbstractBaton() {
        // Default constructor
    }

    @Override
    public void enter() {
        log.debug("Attempting to acquire baton");
        int attempts = 0;
        // NFL: CAS spin-wait loop — baton acquisition — loop preserved per M3 §3
        while (!held.compareAndSet(false, true)) {
            attempts++;
            onSpinWait(attempts);
            adaptiveWait(attempts);
        }
        log.debug("Baton acquired after {} attempts", attempts);
        doEnter();
        onEnter();
    }

    @Override
    public boolean tryEnter() {
        log.debug("Trying to acquire baton non-blocking");
        if (held.compareAndSet(false, true)) {
            log.debug("Baton acquired successfully");
            doTryEnter();
            onEnter();
            return true;
        }
        log.debug("Baton acquisition failed (already held)");
        return false;
    }

    @Override
    public void exit() {
        log.debug("Releasing baton");
        doExit();
        held.set(false);
        onExit();
        log.debug("Baton released");
    }

    @Override
    public boolean isHeld() {
        return held.get();
    }

    /**
     * Adaptive wait strategy.
     *
     * @param attempts the number of failed attempts
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

    /** Called after baton is acquired via blocking enter. */
    protected void doEnter() {
        // Default: no-op
    }

    /** Called after baton is acquired via tryEnter. */
    protected void doTryEnter() {
        // Default: no-op
    }

    /** Called before baton is released. */
    protected void doExit() {
        // Default: no-op
    }

    /** Callback when baton is acquired. */
    protected void onEnter() {
        // Default: no-op
    }

    /** Callback when baton is released. */
    protected void onExit() {
        // Default: no-op
    }

    /**
     * Callback during spin wait.
     *
     * @param attempts the number of attempts so far
     */
    protected void onSpinWait(int attempts) {
        // Default: no-op
    }

    // --- Accessors for Subclasses ---

    /**
     * Get the internal held state directly.
     *
     * @return the AtomicBoolean
     */
    protected AtomicBoolean getHeld() {
        return held;
    }

    /** Force release (use with caution). */
    protected void forceRelease() {
        held.set(false);
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

    @Override
    public String toString() {
        return PREFIX_BATON + held.get() + BRACKET_CLOSE;
    }
}
