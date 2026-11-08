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
package jdk.internal.m3.collections.mat;

import java.util.HexFormat;
import java.util.Objects;

/**
 * Portable value address: immutable pool-image digest + canonical type + dense value id.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatPortableRef.java}
 * blob {@code c4f97ba07322208eca4326c4c391397e9f54c1d9} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MatPortableRef(String typeId, String imageSha256, int id) {
    public MatPortableRef {
        typeId = Objects.requireNonNull(typeId, "typeId");
        imageSha256 = Objects.requireNonNull(imageSha256, "imageSha256");
        if (typeId.isBlank()) throw new IllegalArgumentException("blank typeId");
        if (imageSha256.length() != 64) throw new IllegalArgumentException("SHA-256 must be 64 hex characters");
        try { HexFormat.of().parseHex(imageSha256); }
        catch (IllegalArgumentException failure) { throw new IllegalArgumentException("invalid SHA-256 hex", failure); }
        if (id < 0) throw new IllegalArgumentException("negative id");
    }
}
