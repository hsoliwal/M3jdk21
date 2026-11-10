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
 * Sequencer - Total ordering enforcement primitive.
 *
 * <p>From Grand Java Patterns Proof - Concurrency Section:
 * <pre>
 * &#64;Serialized -> TOTAL_ORDERING -> Sequencer (ticket/serving)
 *
 * Lock-free fair ordering via ticket/serving atomics.
 * </pre>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * final class Sequencer implements ISequencer {
 *     private final AtomicLong ticket = new AtomicLong(0L);
 *     private final AtomicLong serving = new AtomicLong(0L);
 *
 *     public long enter() {
 *         long myTicket = ticket.getAndIncrement();
 *         AdaptiveWait.await(() -> serving.get() == myTicket);
 *         return myTicket;
 *     }
 *
 *     public void exit(long myTicket) {
 *         serving.incrementAndGet();
 *     }
 * }
 * }</pre>
 *
 * <h2>Properties</h2>
 * <ul>
 *   <li>FIFO ordering guarantee</li>
 *   <li>Lock-free ticket acquisition</li>
 *   <li>Fair scheduling</li>
 *   <li>No starvation</li>
 * </ul>
 *
 * @since Grand Java Patterns Proof - Concurrency Section
 */
@ISpec(
    value = "Sequencer - lock-free FIFO ordering via ticket/serving atomics (no starvation)",
    constraints = {
        "FIFO ordering guarantee",
        "Lock-free ticket acquisition",
        "Fair scheduling - no starvation"
    },
    invariants = {
        "enter() returns ticket for exit()",
        "exit(ticket) advances serving",
        "currentServing() <= nextTicket()"
    },
    examples = {
        "long t = seq.enter(); try { ... } finally { seq.exit(t); }",
        "seq.currentServing()",
        "seq.nextTicket()"
    },
    model = Model.ModelType.OPUS,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/concurrency/ISequencer.java}
 * blob {@code b57bf436d490799bf17d92b405b366a55dbfc15f} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISequencer {

    /**
     * Enter the sequencer. Returns ticket for exit.
     *
     * @return ticket number for exit
     */
    long enter();

    /**
     * Exit the sequencer with ticket.
     *
     * @param ticket the ticket from enter()
     */
    void exit(long ticket);

    /**
     * Current serving number.
     *
     * @return current serving position
     */
    long currentServing();

    /**
     * Next ticket number.
     *
     * @return next ticket to be issued
     */
    long nextTicket();
}
