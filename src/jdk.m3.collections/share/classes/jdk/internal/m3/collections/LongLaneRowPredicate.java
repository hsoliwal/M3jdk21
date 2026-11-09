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

import jdk.internal.m3.collections.packed.PackedLongLaneStore;

/**
 * Primitive row predicate over a lane store.
 *
 * <p>The callback may read the store but must not mutate it. No row object is materialized.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/LongLaneRowPredicate.java}
 * blob {@code 2ad89415beade3860b3382ddfe2616b03e60f205} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections lane batch (v6, after U2): identifiers and package only; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface LongLaneRowPredicate {
    boolean test(PackedLongLaneStore store, int row);
}
