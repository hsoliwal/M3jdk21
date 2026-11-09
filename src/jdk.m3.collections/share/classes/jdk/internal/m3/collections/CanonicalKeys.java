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
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.Objects;

/**
 * Defensive immutable keys for primitive/object sequences used by canonical pools.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/canonical/CanonicalKeys.java}
 * blob {@code 1e8f9cdabe4444a2dec1be5ec9b2a9aadcd76e25} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CanonicalKeys {
    private CanonicalKeys() {
    }

    public static ByteKey bytes(byte[] values) {
        return new ByteKey(values);
    }

    public static IntKey ints(int[] values) {
        return new IntKey(values);
    }

    public static LongKey longs(long[] values) {
        return new LongKey(values);
    }

    public static ObjectKey objects(Object... values) {
        return new ObjectKey(values);
    }

    public static final class ByteKey {
        private final byte[] values;
        private final int hash;

        private ByteKey(byte[] values) {
            this.values = Objects.requireNonNull(values, "values").clone();
            this.hash = Arrays.hashCode(this.values);
        }

        public byte[] values() {
            return values.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ByteKey key && Arrays.equals(values, key.values);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final class IntKey {
        private final int[] values;
        private final int hash;

        private IntKey(int[] values) {
            this.values = Objects.requireNonNull(values, "values").clone();
            this.hash = Arrays.hashCode(this.values);
        }

        public int[] values() {
            return values.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof IntKey key && Arrays.equals(values, key.values);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    public static final class LongKey {
        private final long[] values;
        private final int hash;

        private LongKey(long[] values) {
            this.values = Objects.requireNonNull(values, "values").clone();
            this.hash = Arrays.hashCode(this.values);
        }

        public long[] values() {
            return values.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof LongKey key && Arrays.equals(values, key.values);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    /** Elements themselves must be immutable with stable equals/hashCode. */
    public static final class ObjectKey {
        private final Object[] values;
        private final int hash;

        private ObjectKey(Object[] values) {
            this.values = Objects.requireNonNull(values, "values").clone();
            for (Object value : this.values) {
                Objects.requireNonNull(value, "key element");
            }
            this.hash = Arrays.hashCode(this.values);
        }

        public Object[] values() {
            return values.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ObjectKey key && Arrays.equals(values, key.values);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
