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
 * Optional observer for every logical row touch, including already materialized rows.
 *
 * <p>A provider may implement this interface in addition to {@link PrimitiveRowProvider}.
 * The owning collection stores at most one observer reference; there is no per-element observer.
 * Observer callbacks share the provider reentry guard.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveTouchObserver.java}
 * blob {@code e3b6f862ab047295ba8ce712a449aa2577c5154f} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface PrimitiveTouchObserver {

    default void beforeTouch(long logicalId, short currentFlags) throws Exception {
    }

    default void afterTouch(
            long logicalId,
            short resultingFlags,
            boolean materializedNow) throws Exception {
    }

    default void onTouchFailure(
            long logicalId,
            short flags,
            Exception failure) {
    }
}
