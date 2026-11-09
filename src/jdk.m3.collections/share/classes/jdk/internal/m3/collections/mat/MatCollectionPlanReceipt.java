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
package jdk.internal.m3.collections.mat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Deterministic evidence for one physical collection choice.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/MatCollectionPlanReceipt.java}
 * blob {@code 14a91758a0a544c2096b6370dba26d01cdd335e4} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public record MatCollectionPlanReceipt(
        String version,
        String requirements,
        String statistics,
        String implementation,
        long estimatedPrimitiveBytes,
        long actualPrimitiveBytes,
        String identity) {

    public static final String VERSION = "MAT-COLLECTION-PLAN/2";

    public MatCollectionPlanReceipt {
        version = Objects.requireNonNull(version, "version");
        requirements = Objects.requireNonNull(requirements, "requirements");
        statistics = Objects.requireNonNull(statistics, "statistics");
        implementation = Objects.requireNonNull(implementation, "implementation");
        identity = Objects.requireNonNull(identity, "identity");
        if (estimatedPrimitiveBytes < 0 || actualPrimitiveBytes < 0) {
            throw new IllegalArgumentException("negative payload");
        }
        if (!identity.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("invalid plan identity");
        }
    }

    public static MatCollectionPlanReceipt of(
            MatCollectionRequirements requirements,
            String statistics,
            String implementation,
            long estimatedPrimitiveBytes,
            long actualPrimitiveBytes) {
        Objects.requireNonNull(requirements, "requirements");
        String canonical = VERSION + "\n"
                + requirements.canonical() + "\n"
                + Objects.requireNonNull(statistics, "statistics") + "\n"
                + Objects.requireNonNull(implementation, "implementation") + "\n"
                + estimatedPrimitiveBytes + "\n"
                + actualPrimitiveBytes + "\n";
        return new MatCollectionPlanReceipt(
                VERSION,
                requirements.canonical(),
                statistics,
                implementation,
                estimatedPrimitiveBytes,
                actualPrimitiveBytes,
                sha256(canonical));
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
}
