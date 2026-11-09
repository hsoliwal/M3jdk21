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
package jdk.internal.m3.collections;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
/**
 * Projection - ephemeral memory derived from events.
 *
 * <p>
 * From ChatGPT: "Memory = lim (Passₙ → ∞) Projection(Events₁..ₙ)"
 * </p>
 *
 * <h2>SPECIFICATION</h2>
 *
 * <h3>Deferred Evaluation</h3>
 * <p>
 * Projection uses extreme deferred evaluation:
 * </p>
 * <ul>
 * <li><b>Lazy snapshot</b> - Map copy only when requested</li>
 * <li><b>Lazy replay</b> - Events applied only when needed</li>
 * <li><b>No persistence</b> - Memory is never stored, only derived</li>
 * </ul>
 *
 * <h3>Memory Model</h3>
 *
 * <pre>{@code
 * Memory State = f(EventLog)
 *
 * apply(AddEvent)        → state.put(chunk.hash, chunk)
 * apply(DropEvent)       → state.remove(hash)
 * apply(ConsolidateEvent)→ state.remove(fromHash); state.put(to.hash, to)
 *
 * replay(log) = foldLeft(log.events, emptyState, apply)
 * }</pre>
 *
 * <h3>Invariants</h3>
 * <ul>
 * <li>Projection is derived, never authoritative</li>
 * <li>State can always be reconstructed from events</li>
 * <li>Modifications only through events (no direct mutation)</li>
 * </ul>
 *
 * @author Hitesh Soliwal (hsoliwal@gmail.com)
 * @see ConvergenceEventLog
 * @see ConvergenceEvent
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/convergence/ConvergenceProjection.java}
 * blob {@code 976f98c20262db8edddfa2bf7c01adf53a393fde} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public class ConvergenceProjection {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConvergenceProjection.class);

    /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
    public ConvergenceProjection() {
    }


    private final Map<Long, ConvergenceChunk> state = new ConcurrentHashMap<>();

    // Lazy snapshot - only materialized when requested
    private volatile Supplier<Map<Long, ConvergenceChunk>> lazySnapshot;

    /**
     * Applies an event to the projection.
     *
     * <p>
     * Pattern-matched switch (Java 21):
     * </p>
     * <ul>
     * <li>AddEvent → put chunk</li>
     * <li>DropEvent → remove by hash</li>
     * <li>ConsolidateEvent → remove old, put new</li>
     * </ul>
     *
     * @param event
     *            the event to apply
     */
    public void apply(ConvergenceEvent event) {
        invalidateSnapshot();
        switch (event) {
            case ConvergenceEvent.AddEvent a -> state.put(a.chunk().hash(), a.chunk());
            case ConvergenceEvent.DropEvent d -> state.remove(d.hash());
            case ConvergenceEvent.ConsolidateEvent c -> {
                state.remove(c.fromHash());
                state.put(c.to().hash(), c.to());
            }
        }
    }

    /**
     * Gets an immutable snapshot of current state.
     *
     * <p>
     * <b>DEFERRED:</b> Copy only made on first call after modification.
     * </p>
     *
     * @return immutable copy of state
     */
    public Map<Long, ConvergenceChunk> snapshot() {
        if (lazySnapshot == null) {
            synchronized (this) {
                if (lazySnapshot == null) {
                    final Map<Long, ConvergenceChunk> copy = Map.copyOf(state);
                    lazySnapshot = () -> copy;
                }
            }
        }
        return lazySnapshot.get();
    }

    /**
     * Gets current size (no copy needed).
     *
     * @return number of chunks in memory
     */
    public int size() {
        return state.size();
    }

    /**
     * Checks if memory is empty.
     *
     * @return true if no chunks
     */
    public boolean isEmpty() {
        return state.isEmpty();
    }

    /**
     * Checks if memory contains a chunk.
     *
     * @param hash
     *            chunk hash to check
     * @return true if present
     */
    public boolean contains(long hash) {
        return state.containsKey(hash);
    }

    /**
     * Gets a chunk by hash (nullable).
     *
     * @param hash
     *            chunk hash
     * @return chunk or null if not present
     */
    public ConvergenceChunk get(long hash) {
        return state.get(hash);
    }

    private void invalidateSnapshot() {
        lazySnapshot = null;
    }

    // --- STATIC FACTORY: REPLAY ---

    /**
     * Replays all events to rebuild projection.
     *
     * <p>
     * <b>This is the ONLY way to materialize memory from events.</b>
     * </p>
     *
     * <p>
     * Replay is:
     * </p>
     * <ul>
     * <li>Deterministic (same events → same state)</li>
     * <li>Idempotent (replay(replay(log)) = replay(log))</li>
     * <li>Complete (all events applied in order)</li>
     * </ul>
     *
     * @param log
     *            the event log to replay
     * @return new projection with all events applied
     */
    public static ConvergenceProjection replay(ConvergenceEventLog log) {
        ConvergenceProjection p = new ConvergenceProjection();
        log.all().forEach(p::apply);
        return p;
    }

    /**
     * Replays events incrementally from a starting projection.
     *
     * <p>
     * <b>DEFERRED:</b> Only processes new events.
     * </p>
     *
     * @param base
     *            starting projection
     * @param log
     *            event log
     * @param afterSeq
     *            only replay events after this sequence
     * @return projection with incremental events applied
     */
    public static ConvergenceProjection replayIncremental(
            ConvergenceProjection base, ConvergenceEventLog log, long afterSeq) {

        // Clone base state
        ConvergenceProjection p = new ConvergenceProjection();
        p.state.putAll(base.state);

        // Apply only new events
        log.after(afterSeq).forEach(p::apply);
        return p;
    }
}
