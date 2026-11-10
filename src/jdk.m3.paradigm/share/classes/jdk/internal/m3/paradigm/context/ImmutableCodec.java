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
import java.util.function.Function;

/**
 * Explicit canonical representation for one immutable Java value type.
 *
 * <p>The codec is the proof obligation that pooling is behavior preserving.
 * Canonical bytes must be deterministic for the codec schema and must not
 * depend on object identity, process address, locale or wall clock state.
 *
 * @param <T> immutable value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextImmutableCodec.java}
 * blob {@code b0ec8518e4f14ddc1d1308ad13e7a71f6fced3d9} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface ImmutableCodec<T> {

    Class<T> type();

    String schema();

    byte[] canonicalBytes(T value);

    /**
     * Creates an explicit immutable codec.
     *
     * @param type value type
     * @param schema stable schema/version name
     * @param encoder canonical byte encoder
     * @param <T> value type
     * @return codec
     */
    static <T> ImmutableCodec<T> of(
            Class<T> type,
            String schema,
            Function<? super T, byte[]> encoder) {
        return new Basic<>(
                Objects.requireNonNull(type, "type"),
                requireSchema(schema),
                Objects.requireNonNull(encoder, "encoder"));
    }

    private static String requireSchema(String value) {
        String normalized =
                Objects.requireNonNull(value, "schema").strip();
        if (normalized.isEmpty()
                || normalized.length() > 128) {
            throw new IllegalArgumentException(
                    "schema must contain 1..128 characters");
        }
        return normalized;
    }

    /** Private immutable implementation returned by {@link #of}. */
    final class Basic<T> implements ImmutableCodec<T> {
        private final Class<T> type;
        private final String schema;
        private final Function<? super T, byte[]> encoder;

        private Basic(
                Class<T> type,
                String schema,
                Function<? super T, byte[]> encoder) {
            this.type = type;
            this.schema = schema;
            this.encoder = encoder;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public String schema() {
            return schema;
        }

        @Override
        public byte[] canonicalBytes(T value) {
            T checked =
                    type.cast(
                            Objects.requireNonNull(
                                    value,
                                    "value"));
            byte[] encoded =
                    Objects.requireNonNull(
                            encoder.apply(checked),
                            "canonical bytes");
            return encoded.clone();
        }

        @Override
        public String toString() {
            return "ContextImmutableCodec["
                    + type.getName()
                    + ","
                    + schema
                    + "]";
        }
    }
}
