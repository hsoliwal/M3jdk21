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

/**
 * Least signed-long priority first, with primitive generation-checked handles.
 * Duplicate payloads are separate occurrences; equal-priority order is unspecified.
 * Handles survive growth and updates, and become stale after removal or clear.
 * They are collection-local, not globally unique or unforgeable: never transfer
 * one to another queue. Invalid handle access throws IllegalStateException.
 * Built-ins are single-threaded and obey the factory's null and live-size policies.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/shapes/ShapeAddressablePriorityQueue.java}
 * blob {@code 805cdf6f49cae18b57b4de49c1f3c61d8590e884} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3ShapeAddressablePriorityQueue<E> extends M3ShapeContainer {
    /** Insert an occurrence and return its nonzero handle; full insertion throws without mutation. */
    long add(E value, long priority);
    /** Read one occurrence, rejecting an invalid or stale handle. */
    E get(long handle);
    /** Replace only its payload and return the previous payload. */
    E set(long handle, E value);
    /** Read a live occurrence's priority. */
    long priorityOf(long handle);
    /** Change a live priority and return the previous priority, retaining the handle. */
    long updatePriority(long handle, long priority);
    /**
     * Validate both arrays, equal lengths and every handle before changing priorities.
     * Duplicate handles use the last priority. Null arrays throw NullPointerException;
     * unequal lengths throw IllegalArgumentException. Do not concurrently modify lanes.
     */
    void updatePriorities(long[] handles, long[] priorities);
    /** Remove one occurrence, invalidate its handle and return its payload. */
    E remove(long handle);
    /** Test validity without throwing for malformed, removed or stale numeric handles. */
    boolean isLive(long handle);
    /** Handle of a least-priority occurrence; throws NoSuchElementException when empty. */
    long firstHandle();
    /** Least-priority payload; throws NoSuchElementException when empty. */
    E first();
    /** Least retained priority; throws NoSuchElementException when empty. */
    long firstPriority();
    /** Remove a least-priority occurrence; throws NoSuchElementException when empty. */
    E removeFirst();
    /**
     * Visit each live occurrence once in unspecified order. Reads and nested traversal
     * are allowed; every callback mutation is rejected, including clear and empty batches.
     * A callback exception leaves membership unchanged and releases the traversal guard.
     */
    void forEach(M3HandlePriorityConsumer<? super E> action);
    /**
     * Invalidate all live handles and clear all payload references, retaining capacity
     * and generation history. Generation exhaustion rejects the entire clear beforehand.
     */
    @Override void clear();
}
