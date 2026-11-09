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
 * Explicit fixed-width external-sort resource and ordering contract.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/collections/foss/ExternalSortOptions.java}
 * blob {@code 4648fed3f03eb05dec8a64ab239abb3674fecff0} at com.synexia develop {@code 5ab05fd05653} by
 * RELOCATE_M3_OWNER v4 (common.collections subpackages batch 1 (phase D, v5): identifiers and package only; bytecode equal to the source modulo names).
 */
public record ExternalSortOptions(int runRecords, int fanIn, int bufferBytes,
        long maximumInputRecords, long maximumRunBytes, long maximumArrayPayloadBytes,
        boolean unsigned, Selector selector) {
    public enum Selector { HEAP, TOURNAMENT, LOSER_TREE }
    public ExternalSortOptions {
        Objects.requireNonNull(selector, "selector");
        if (runRecords < 1 || runRecords > Integer.MAX_VALUE - 8 || fanIn < 2 || fanIn > 4096
                || bufferBytes < 256 || bufferBytes > 1048576 || maximumInputRecords < 0
                || maximumInputRecords > (Long.MAX_VALUE - 16) / 24 || maximumRunBytes < 0
                || maximumArrayPayloadBytes < payloadBound(runRecords, fanIn, bufferBytes))
            throw new IllegalArgumentException("external sort bounds");
    }
    /** Owned primitive arrays and configured buffers; excludes JDK internals, headers and RSS. */
    public long arrayPayloadBoundBytes() { return payloadBound(runRecords, fanIn, bufferBytes); }
    private static long payloadBound(int records, int fanIn, int buffer) {
        return Math.max(24L * records + 1024 + 2L * buffer,
                (fanIn + 1L) * buffer + 64L * fanIn + 256) + 65536;
    }
    String identity() {
        return runRecords + "," + fanIn + "," + bufferBytes + "," + maximumInputRecords
                + "," + maximumRunBytes + "," + maximumArrayPayloadBytes + "," + unsigned
                + "," + selector.name();
    }
}
