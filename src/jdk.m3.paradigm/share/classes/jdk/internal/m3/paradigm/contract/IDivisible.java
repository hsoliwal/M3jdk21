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

/** Optional division alias; callers opt in only when division has established semantics. */
public interface IDivisible<T> {
    T divide(T other);

    /** One virtual dispatch to the existing division hook, with no invented fallback. */
    default T div(T other) {
        return divide(other);
    }
}
