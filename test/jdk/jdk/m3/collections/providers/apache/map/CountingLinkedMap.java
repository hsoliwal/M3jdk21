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
package jdk.m3.collections.apache.map;

import java.util.List;

/**
 * Lockstep subclass counting its {@code get(int)} and {@code size()} calls: a subclass keeps
 * {@code AbstractList}'s iterator, so both views must make the same calls.
 */
final class CountingLinkedMap extends LinkedMap<Object, Object> {

    private static final long serialVersionUID = 1L;

    /** The get(int) calls. */
    private int gets;

    /** The size() calls. */
    private int sizes;

    @Override
    public Object get(final int index) {
        gets++;
        return super.get(index);
    }

    @Override
    public int size() {
        sizes++;
        return super.size();
    }

    /**
     * Gets the counters.
     *
     * @return The get(int) and size() calls so far
     */
    List<Integer> counts() {
        return List.of(gets, sizes);
    }
}
