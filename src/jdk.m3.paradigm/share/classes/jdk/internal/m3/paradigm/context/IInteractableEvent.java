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

import java.time.Instant;

/**
 * Event produced through interaction - optional structured event type.
 *
 * <p>Use when you need structured events in interaction streams.
 * IInteractable returns raw Stream&lt;T&gt; by default.
 *
 * @param <T> The payload type
 * @since ContextOS 1.0
 */
@ISpec(
    value = "Optional structured event for interaction streams",
    constraints = {"Payload is the interaction result"},
    invariants = {"payload() returns non-null", "timestamp() returns non-null"},
    examples = {"IInteractableEvent.of(result)", "event.payload()"},
    model = Model.ModelType.HAIKU,
    stability = Stability.Level.STABLE
)
/**
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/event/IInteractableEvent.java}
 * blob {@code e6471fa214d0af5c5e93f2e9436b5e06ec07145c} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface IInteractableEvent<T> {

    /** Event payload. */
    T payload();

    /** Event timestamp. */
    default Instant timestamp() {
        return Instant.now();
    }

    /** Create event with payload. */
    static <T> IInteractableEvent<T> of(T payload) {
        return new SimpleEvent<>(payload, Instant.now());
    }

    /** Simple implementation. */
    record SimpleEvent<T>(T payload, Instant timestamp) implements IInteractableEvent<T> {}
}
