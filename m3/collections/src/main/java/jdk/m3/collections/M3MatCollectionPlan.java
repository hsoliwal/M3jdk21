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
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionPlan.java}
 * blob {@code 8533a83f504de89abc2103cb1499033240dc6362} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record M3MatCollectionPlan(
        M3MatStorageKind kind, long primitivePayloadBytes, int size, long domainSpan, String detail) {
    public M3MatCollectionPlan {
        if (kind == null || detail == null) throw new NullPointerException();
        if (primitivePayloadBytes < 0 || size < 0 || domainSpan < 0) {
            throw new IllegalArgumentException("negative plan metric");
        }
    }
}
