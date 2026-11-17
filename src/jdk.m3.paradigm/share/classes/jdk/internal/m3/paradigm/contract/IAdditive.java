/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package jdk.internal.m3.paradigm.contract;

/** Opt-in addition alias; the pre-existing {@link #add(Object)} hook owns semantics. */
public interface IAdditive<T> {
    T add(T other);

    /** One virtual dispatch to {@link #add(Object)}, without changing Java's {@code +}. */
    default T plus(T other) {
        return add(other);
    }
}
