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

import java.util.function.LongSupplier;

/**
 * Strategy interface for waiting on a sequence value.
 *
 * <p>
 * Wait strategies control how consumers wait for new data from producers.
 * Different strategies offer trade-offs between latency and CPU usage.
 * </p>
 *
 * <h2>Strategy Selection Guide</h2>
 * <table>
 * <caption>Wait strategy trade-offs</caption>
 * <tr>
 * <th>Strategy</th>
 * <th>CPU</th>
 * <th>Latency</th>
 * <th>Use Case</th>
 * </tr>
 * <tr>
 * <td>BusySpin</td>
 * <td>100%</td>
 * <td>&lt;1μs</td>
 * <td>Ultra-low latency</td>
 * </tr>
 * <tr>
 * <td>Yielding</td>
 * <td>~100%</td>
 * <td>1-10μs</td>
 * <td>High throughput</td>
 * </tr>
 * <tr>
 * <td>Sleeping</td>
 * <td>Low</td>
 * <td>1-100ms</td>
 * <td>Bursty traffic</td>
 * </tr>
 * <tr>
 * <td>Blocking</td>
 * <td>Low</td>
 * <td>1-10μs</td>
 * <td>General purpose</td>
 * </tr>
 * <tr>
 * <td>PhasedBackoff</td>
 * <td>Adaptive</td>
 * <td>Configurable</td>
 * <td>Tuned scenarios</td>
 * </tr>
 * </table>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 *
 * // Create wait strategy
 * IWaitStrategy strategy = WaitStrategies.sleeping(100, TimeUnit.MICROSECONDS);
 *
 * // Wait for sequence to reach target
 * long availableSequence = strategy.waitFor(targetSequence, () -> cursor.get(), () -> !running);
 * }</pre>
 *
 * <h2>Inspired by</h2>
 *
 * @author Hitesh Soliwal
 * @version 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/IWaitStrategy.java}
 * blob {@code ddd512cef58c7157b247090b926e8772c4eba29c} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.lazy batch 1 (eligible core contracts) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IWaitStrategy {

    /**
     * Wait for the given sequence to be available.
     *
     * @param sequence
     *            the sequence value to wait for
     * @param cursor
     *            supplier of the current cursor position
     * @param isInterrupted
     *            supplier that returns true if waiting should stop
     * @return the available sequence (may be higher than requested)
     * @throws InterruptedException
     *             if interrupted while waiting
     */
    long waitFor(long sequence, LongSupplier cursor, java.util.function.BooleanSupplier isInterrupted)
            throws InterruptedException;

    /**
     * Signal that a new value is available.
     *
     * <p>
     * Called by producers when new data is published. Blocking strategies use this
     * to wake up waiting consumers.
     * </p>
     */
    void signalAllWhenBlocking();
}
