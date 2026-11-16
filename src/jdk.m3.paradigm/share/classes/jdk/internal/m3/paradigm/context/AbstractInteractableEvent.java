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

import static java.util.Objects.requireNonNull;

import java.time.Instant;

/**
 * Template Method base for {@link IInteractableEvent}.
 *
 * <p>
 * Provides payload storage and timestamp with lifecycle hooks.
 *
 * @param <T>
 *            payload type
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/event/AbstractInteractableEvent.java}
 * blob {@code 3dceb86b58ef7cb3a181a8be933e4e90cc9ba320} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch 2 (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public abstract class AbstractInteractableEvent<T> implements IInteractableEvent<T> {
    private static final PlaceholderLogger log = PlaceholderLogger.of(AbstractInteractableEvent.class);


    private final T eventPayload;
    private final Instant eventTimestamp;

    protected AbstractInteractableEvent(T payload) {
        this(payload, Instant.now());
    }

    protected AbstractInteractableEvent(T payload, Instant timestamp) {
        this.eventPayload = requireNonNull(payload, "payload");
        this.eventTimestamp = requireNonNull(timestamp, "timestamp");
    }

    // --- TEMPLATE METHODS ---

    @Override
    public final T payload() {
        onBeforePayload();
        T result = doPayload();
        onAfterPayload(result);
        return result;
    }

    @Override
    public Instant timestamp() {
        return eventTimestamp;
    }

    // --- doXxx() - SUBCLASS MAY OVERRIDE ---

    protected T doPayload() {
        return eventPayload;
    }

    // --- CALLBACK HOOKS - onXxx() ---

    protected void onBeforePayload() {}

    protected void onAfterPayload(T result) {}
}
