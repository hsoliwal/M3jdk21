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

import jdk.internal.m3.paradigm.annotation.ISpec;
import jdk.internal.m3.paradigm.annotation.Model;
import jdk.internal.m3.paradigm.annotation.Stability;

/**
 * Baton - Single-writer enforcement primitive.
 *
 * <p>From Grand Java Patterns Proof - Concurrency Section:
 * <pre>
 * &#64;SingleWriter -> SINGLE_WRITER discipline -> Baton/SWQ
 *
 * "If a user needs to understand concurrency primitives, AnOP has failed."
 * </pre>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * final class Baton implements IBaton {
 *     private final AtomicBoolean held = new AtomicBoolean(false);
 *
 *     public void enter() {
 *         AdaptiveWait.await(() -> held.compareAndSet(false, true));
 *     }
 *
 *     public void exit() {
 *         held.set(false);
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage in Generated Code</h2>
 * <pre>{@code
 * &#64;SingleWriter
 * void write(Data data);
 *
 * // Generated wrapper:
 * void write(Data data) {
 *     baton.enter();
 *     try {
 *         write_impl(data);
 *     } finally {
 *         baton.exit();
 *     }
 * }
 * }</pre>
 *
 * <h2>Properties</h2>
 * <ul>
 *   <li>Lock-free acquisition via CAS</li>
 *   <li>Adaptive wait strategy</li>
 *   <li>No thread blocking on fast path</li>
 *   <li>Progress guarantee via park promotion</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - Concurrency Section
 */
@ISpec(
    value = "Baton - lock-free single-writer enforcement via CAS with adaptive wait",
    constraints = {
        "Lock-free acquisition via compareAndSet",
        "Adaptive wait strategy (spin -> yield -> park)",
        "Progress guarantee via park promotion"
    },
    invariants = {
        "enter() blocks until baton acquired",
        "tryEnter() returns immediately",
        "exit() releases baton atomically"
    },
    examples = {
        "baton.enter(); try { ... } finally { baton.exit(); }",
        "if (baton.tryEnter()) { ... }",
        "baton.isHeld()"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/concurrency/IBaton.java}
 * blob {@code 7c4a9c3fc46f2dad8891a9f6e0277aff9be73dbb} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IBaton {

    /** Acquire the baton. Blocks adaptively until acquired. */
    void enter();

    /**
     * Try to acquire the baton without blocking.
     *
     * @return true if acquired, false otherwise
     */
    boolean tryEnter();

    /** Release the baton. */
    void exit();

    /**
     * Check if baton is currently held.
     *
     * @return true if held
     */
    boolean isHeld();
}
