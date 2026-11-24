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

/**
 * Lockstep subclass whose {@code get(int)} returns the key one place later, wrapping round: its
 * list view must iterate through the overridden {@code get(int)}, not the entry links.
 */
final class ShiftedLinkedMap extends LinkedMap<Object, Object> {

    private static final long serialVersionUID = 1L;

    @Override
    public Object get(final int index) {
        final int size = size();
        return super.get(size == 0 ? index : (index + 1) % size);
    }
}
