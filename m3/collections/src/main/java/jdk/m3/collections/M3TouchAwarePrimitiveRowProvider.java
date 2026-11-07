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
 * Compatibility specialization for providers that historically exposed only a before-touch hook.
 *
 * <p>The complete observation contract now lives in {@link M3PrimitiveTouchObserver}. Existing
 * implementations remain source-compatible: they still implement {@link #beforeTouch(long, short)}
 * while after/failure callbacks inherit default no-op behavior.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/TouchAwarePrimitiveRowProvider.java}
 * blob {@code 463979f8961f1b1585fa1577a6c6637f432d0144} at com.synexia develop {@code fd9a233680b43197eb8c5745a729c342ca56c4e2} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
public interface M3TouchAwarePrimitiveRowProvider
        extends M3PrimitiveRowProvider, M3PrimitiveTouchObserver {

    /**
     * Called immediately before an external touch is resolved.
     *
     * <p>If this callback throws, no collection state is mutated by the touch.
     */
    @Override
    void beforeTouch(long rowId, short currentFlags) throws Exception;
}
