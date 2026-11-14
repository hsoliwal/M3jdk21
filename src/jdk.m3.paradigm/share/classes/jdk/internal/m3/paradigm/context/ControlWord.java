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

/**
 * Primitive resident control word for AbstractContext.
 *
 * <p>Layout: 8 flag bits, 28 revision bits, 28 compact-identity bits.
 * This is runtime representation only; persisted/public enum and UID contracts
 * remain explicit boundary formats.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextControlWord.java}
 * blob {@code eaf3c63ec5d0c149f2961f9f4bf2c12b8a06de0b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
final class ControlWord {
    static final int FLAGS_BITS = 8;
    static final int REVISION_BITS = 28;
    static final int LOCAL_ID_BITS = 28;

    static final int REVISION_SHIFT = FLAGS_BITS;
    static final int LOCAL_ID_SHIFT = FLAGS_BITS + REVISION_BITS;

    static final long FLAGS_MASK = (1L << FLAGS_BITS) - 1L;
    static final long MAX_REVISION = (1L << REVISION_BITS) - 1L;
    static final long MAX_LOCAL_ID = (1L << LOCAL_ID_BITS) - 1L;

    private static final long REVISION_MASK =
            MAX_REVISION << REVISION_SHIFT;
    private static final long LOCAL_ID_MASK =
            MAX_LOCAL_ID << LOCAL_ID_SHIFT;

    private ControlWord() {
    }

    static long initial(long localId, long flags) {
        requireLocalId(localId);
        return (localId << LOCAL_ID_SHIFT)
                | (flags & FLAGS_MASK);
    }

    static long localId(long word) {
        return (word & LOCAL_ID_MASK) >>> LOCAL_ID_SHIFT;
    }

    static long revision(long word) {
        return (word & REVISION_MASK) >>> REVISION_SHIFT;
    }

    static long flags(long word) {
        return word & FLAGS_MASK;
    }

    static long withFlags(long word, long flags) {
        return (word & ~FLAGS_MASK)
                | (flags & FLAGS_MASK);
    }

    static long withPresence(
            long word,
            long flag,
            boolean enabled) {
        if ((flag & ~FLAGS_MASK) != 0L
                || Long.bitCount(flag) != 1) {
            throw new IllegalArgumentException(
                    "presence flag must be one control flag bit");
        }
        return enabled
                ? word | flag
                : word & ~flag;
    }

    static long advance(long word) {
        return advanceWithFlags(word, flags(word));
    }

    static long advanceWithFlags(
            long word,
            long flags) {
        long revision = revision(word);
        if (revision >= MAX_REVISION) {
            throw new ArithmeticException(
                    "Context revision exhausted");
        }
        return (word & LOCAL_ID_MASK)
                | ((revision + 1L) << REVISION_SHIFT)
                | (flags & FLAGS_MASK);
    }

    static long compatibilityWord(long word) {
        return (revision(word) << REVISION_SHIFT)
                | flags(word);
    }

    private static void requireLocalId(long localId) {
        if (localId < 0L || localId > MAX_LOCAL_ID) {
            throw new IllegalArgumentException(
                    "compact context id must be in [0,"
                            + MAX_LOCAL_ID + "]");
        }
    }
}
