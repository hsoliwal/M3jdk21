/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.Objects;

/**
 * Immutable metadata describing one registered mapping edge.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/mapper/MappingDescriptor.java}
 * blob {@code 5b04d93801a4e53225e7d005d745f3f1f56f0f07} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MappingDescriptor(
        Class<?> sourceType,
        Class<?> targetType,
        String qualifier,
        MappingCardinality cardinality,
        int priority,
        boolean lossless,
        boolean reversible,
        String contract) {

    public MappingDescriptor {
        sourceType =
                Objects.requireNonNull(sourceType, "sourceType");
        targetType =
                Objects.requireNonNull(targetType, "targetType");
        qualifier = MappingKey.normalizeQualifier(qualifier);
        cardinality =
                Objects.requireNonNull(cardinality, "cardinality");
        contract = Objects.requireNonNullElse(contract, "");
    }

    public static MappingDescriptor oneToOne(
            Class<?> sourceType,
            Class<?> targetType) {
        return new MappingDescriptor(
                sourceType,
                targetType,
                "",
                MappingCardinality.ONE_TO_ONE,
                0,
                false,
                false,
                "");
    }

    public MappingDescriptor withQualifier(String next) {
        return new MappingDescriptor(
                sourceType,
                targetType,
                next,
                cardinality,
                priority,
                lossless,
                reversible,
                contract);
    }

    public MappingDescriptor withPriority(int next) {
        return new MappingDescriptor(
                sourceType,
                targetType,
                qualifier,
                cardinality,
                next,
                lossless,
                reversible,
                contract);
    }

    public MappingDescriptor withContract(String next) {
        return new MappingDescriptor(
                sourceType,
                targetType,
                qualifier,
                cardinality,
                priority,
                lossless,
                reversible,
                next);
    }

    public MappingDescriptor asLossless() {
        return new MappingDescriptor(
                sourceType,
                targetType,
                qualifier,
                cardinality,
                priority,
                true,
                reversible,
                contract);
    }

    public MappingDescriptor asReversible() {
        return new MappingDescriptor(
                sourceType,
                targetType,
                qualifier,
                cardinality,
                priority,
                lossless,
                true,
                contract);
    }

    public MappingKey key() {
        return new MappingKey(
                sourceType, targetType, qualifier);
    }

    public String stableId() {
        return sourceType.getName()
                + "->"
                + targetType.getName()
                + "#"
                + qualifier
                + ":"
                + cardinality.name()
                + ":"
                + priority
                + ":"
                + lossless
                + ":"
                + reversible
                + ":"
                + contract;
    }
}
