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
import java.util.UUID;

/**
 * Primitive 128-bit context identity helpers.
 *
 * <p>The canonical representation is two Java long words. Java has no unsigned
 * long declaration, but all 64 bits are preserved exactly. UUID/String forms
 * are compatibility and I/O materializations only.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextUidBits.java}
 * blob {@code 97800451b8bba28efe951175eade3f6920add468} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class UidBits {
    private UidBits() {}

    public static long high(UUID uid) {
        return Objects.requireNonNull(uid, "uid").getMostSignificantBits();
    }

    public static long low(UUID uid) {
        return Objects.requireNonNull(uid, "uid").getLeastSignificantBits();
    }

    public static UUID uuid(long high, long low) {
        return new UUID(high, low);
    }

    public static String text(long high, long low) {
        return uuid(high, low).toString();
    }

    public static long[] words(long high, long low) {
        return new long[] {high, low};
    }

    public static boolean equals(UUID uid, long high, long low) {
        return uid != null
                && uid.getMostSignificantBits() == high
                && uid.getLeastSignificantBits() == low;
    }
}
