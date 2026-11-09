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
package jdk.m3.collections;

import java.util.Arrays;

/**
 * Clears only the live half-open logical span of a primitive long ring.
 *
 * <p>Owners guarantee that all slots outside [head, head + size) modulo
 * capacity are already zero. This helper never allocates, examines payloads,
 * or changes ring cursors. It replaces full-capacity clearing only where
 * that owner invariant has been independently proved.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-primitives/src/main/java/jdk/internal/m3/primitives/RingLiveClear.java}
 * blob {@code 57c7430d5842f736dfc56995954020538a1d4359} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v3 (identifiers only, global M3 owner map; bytecode equal to the source modulo names).
 */
final class RingLiveClear {
    private RingLiveClear() { }

    static void clear(long[] lane, int head, int size) {
        if (size == 0) return;
        int first = Math.min(size, lane.length - head);
        Arrays.fill(lane, head, head + first, 0L);
        if (first < size) Arrays.fill(lane, 0, size - first, 0L);
    }
}
