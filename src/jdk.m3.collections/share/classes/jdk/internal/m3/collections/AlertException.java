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

import java.io.Serial;

/**
 * Exception thrown when a sequence barrier has been alerted.
 *
 * <p>
 * This exception signals that the barrier has been shut down and consumers
 * should stop processing. It is a checked exception to ensure proper handling
 * of shutdown scenarios.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * try {
 * 	long sequence = barrier.waitFor(nextSequence);
 * 	processEvent(sequence);
 * } catch (AlertException e) {
 * 	// Shutdown signal received
 * 	cleanUp();
 * 	return;
 * }
 * }</pre>
 *
 * <h2>Inspired by</h2>
 *
 * @author Hitesh Soliwal
 * @version 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/lazy/AlertException.java}
 * blob {@code f526e46f042ec7840c37668ab13a7e3840284b57} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.lazy batch 1 (eligible core contracts) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class AlertException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Singleton instance (exception is just a signal, no stack trace needed). */
    public static final AlertException INSTANCE = new AlertException();

    private AlertException() {
        // Singleton
    }

    /**
     * Overridden to prevent stack trace generation for performance.
     *
     * <p>
     * This exception is used as a control flow mechanism, not for error reporting,
     * so stack traces are unnecessary overhead.
     * </p>
     *
     * @return this exception
     */
    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
