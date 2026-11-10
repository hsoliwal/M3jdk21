/* *****************************************************************************
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
 * Barrier for coordinating sequence-based access to a shared buffer.
 *
 * <p>
 * A sequence barrier allows consumers to wait for a specific sequence to become
 * available, providing coordination between producers and consumers in
 * lock-free data structures.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * ISequenceBarrier barrier = SequenceBarrier.create(cursor, waitStrategy);
 *
 * // Consumer: wait for sequence to be available
 * long availableSequence = barrier.waitFor(nextSequence);
 *
 * // Process all available sequences
 * while (nextSequence <= availableSequence) {
 * 	event = buffer.get(nextSequence);
 * 	process(event);
 * 	nextSequence++;
 * }
 *
 * // Signal shutdown
 * barrier.alert();
 * }</pre>
 *
 * <h2>Inspired by</h2>
 *
 * @author Hitesh Soliwal
 * @version 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/ISequenceBarrier.java}
 * blob {@code b200aa38aeaef5b6b5a4ef5a76c31c90b55cbd1c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.lazy batch 1 (eligible core contracts) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ISequenceBarrier {

    /**
     * Wait for the given sequence to be available.
     *
     * @param sequence
     *            the sequence value to wait for
     * @return the available sequence (may be higher than requested)
     * @throws AlertException
     *             if barrier has been alerted (shutdown)
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    long waitFor(long sequence) throws AlertException, InterruptedException;

    /**
     * Gets the current cursor position.
     *
     * @return current cursor sequence
     */
    long getCursor();

    /**
     * Checks if an alert has been raised.
     *
     * @return true if alerted
     */
    boolean isAlerted();

    /**
     * Raises an alert to signal shutdown.
     *
     * <p>
     * Once alerted, all waiting threads will be interrupted and subsequent waitFor
     * calls will throw AlertException.
     * </p>
     */
    void alert();

    /**
     * Clears the alert status.
     *
     * <p>
     * Allows the barrier to be reused after shutdown.
     * </p>
     */
    void clearAlert();

    /**
     * Checks for alert and throws if alerted.
     *
     * @throws AlertException
     *             if barrier has been alerted
     */
    void checkAlert() throws AlertException;
}
