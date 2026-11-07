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
package jdk.m3.collections;

/**
 * Callback that materializes one logical primitive row into caller-owned scratch storage.
 *
 * <p>The provider must fill every lane and must not retain {@code target}.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/PrimitiveRowProvider.java}
 * blob {@code 286d5a82f2bb6fbc344dac613f817c24ac325ffe} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
@FunctionalInterface
public interface M3PrimitiveRowProvider {

    void load(long rowId, long[] target) throws Exception;

    default void beforeLoad(long rowId, short currentFlags) {
    }

    default void afterLoad(long rowId, short loadedFlags) {
    }

    default void onLoadFailure(long rowId, Exception failure) {
    }
}
