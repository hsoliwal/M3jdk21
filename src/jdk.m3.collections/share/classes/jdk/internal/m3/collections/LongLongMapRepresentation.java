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
 * Physical primitive long-to-long map representations available to the compiler.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-algo/src/main/java/jdk/internal/m3/algorithms/collections/LongLongMapRepresentation.java}
 * blob {@code 51b059a5e304299c4c5105ccb454e85d52d487cb} at com.synexia develop {@code 5ab05fd0565303f8884b7deea8e309cea9cfef5f} by
 * RELOCATE_M3_OWNER v4 (algorithms collections batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public enum LongLongMapRepresentation {
    DENSE_SORTED(Order.SORTED),
    PAGED_SORTED(Order.SORTED),
    LINEAR_HASH(Order.UNORDERED),
    ROBIN_HOOD_HASH(Order.UNORDERED),
    SWISS_HASH(Order.UNORDERED),
    INSERTION_ORDERED(Order.INSERTION),
    ACCESS_ORDERED(Order.ACCESS);

    public enum Order {
        UNORDERED,
        SORTED,
        INSERTION,
        ACCESS
    }

    private final Order order;

    LongLongMapRepresentation(Order order) {
        this.order = order;
    }

    public Order order() {
        return order;
    }
}
