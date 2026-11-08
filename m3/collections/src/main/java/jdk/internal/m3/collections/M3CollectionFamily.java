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

import java.util.Objects;

/**
 * A semantic contract and its runtime type, shared by interchangeable providers.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/selection/CollectionFamily.java}
 * blob {@code 149e56e224a81dad1b2fe560ce82def0379514b5} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3CollectionFamily<T>(String id, Class<T> type) {
    public M3CollectionFamily {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(type, "type");
        if (!id.matches("[a-z][a-z0-9.-]*") || type.isPrimitive()) {
            throw new IllegalArgumentException("family requires a stable ID and reference type");
        }
    }
}
