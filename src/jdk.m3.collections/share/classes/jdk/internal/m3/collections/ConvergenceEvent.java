/*******************************************************************************
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
package jdk.internal.m3.collections;

import java.time.Instant;

/**
 * Convergence Event - sealed interface for event sourcing.
 *
 * <p>
 * From ChatGPT Discrete Multi-Pass Convergence: "Events are history.
 * Projections are memory. Memory is ephemeral. History is permanent."
 * </p>
 *
 * <h2>Core Laws</h2>
 * <ol>
 * <li>Memory is not stored; memory is reached.</li>
 * <li>Every pass may add, drop, or consolidate.</li>
 * <li>Event log is the only source of truth.</li>
 * <li>The last projection IS the memory.</li>
 * <li>Recall biases computation; it never injects facts.</li>
 * </ol>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceChunk
 * @see ConvergenceEventLog
 * @see ConvergenceProjection
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceEvent.java}
 * blob {@code 5831d510c392623b2b5c62c661e25b0654afc00a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public sealed interface ConvergenceEvent
        permits ConvergenceEvent.AddEvent, ConvergenceEvent.DropEvent, ConvergenceEvent.ConsolidateEvent {

    /**
     * Event sequence number (monotonically increasing).
     *
     * @return sequence number
     */
    long seq();

    /**
     * Event timestamp.
     *
     * @return instant when event occurred
     */
    Instant time();

    // --- ADD EVENT - Adds new chunk to memory ---

    /**
     * Add Event - introduces a new chunk into memory.
     *
     * <p>
     * Generated when:
     * </p>
     * <ul>
     * <li>New content is encountered</li>
     * <li>Chunk passes threshold gating</li>
     * <li>No existing chunk matches closely enough</li>
     * </ul>
     *
     * @param seq
     *            sequence number
     * @param time
     *            event timestamp
     * @param chunk
     *            the chunk being added
     */
    record AddEvent(long seq, Instant time, ConvergenceChunk chunk) implements ConvergenceEvent {}

    // --- DROP EVENT - Removes decayed chunk from memory ---

    /**
     * Drop Event - removes a chunk from memory.
     *
     * <p>
     * Generated when:
     * </p>
     * <ul>
     * <li>Chunk score decays below threshold</li>
     * <li>Chunk is superseded by consolidation</li>
     * <li>Memory pressure forces eviction</li>
     * </ul>
     *
     * @param seq
     *            sequence number
     * @param time
     *            event timestamp
     * @param hash
     *            hash of the chunk being dropped
     */
    record DropEvent(long seq, Instant time, long hash) implements ConvergenceEvent {}

    // --- CONSOLIDATE EVENT - Merges/updates chunk ---

    /**
     * Consolidate Event - merges or updates an existing chunk.
     *
     * <p>
     * Generated when:
     * </p>
     * <ul>
     * <li>Similar chunk encountered (reinforcement)</li>
     * <li>Score adjustment needed (decay)</li>
     * <li>Content evolution detected</li>
     * </ul>
     *
     * @param seq
     *            sequence number
     * @param time
     *            event timestamp
     * @param fromHash
     *            hash of the chunk being replaced
     * @param to
     *            the new consolidated chunk
     */
    record ConsolidateEvent(long seq, Instant time, long fromHash, ConvergenceChunk to) implements ConvergenceEvent {}
}
