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

import java.util.Objects;

/**
 * One canonical codec for one immutable logical value type.
 *
 * @param <T> logical Java value type
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatCodec.java}
 * blob {@code 59aaf4a63b78e0e355b28455b7850d18846f6bd8} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public interface M3MatCodec<T> {
    /** Stable schema/type identity. */
    String typeId();

    /** Declared Java type accepted by this codec. */
    Class<T> javaType();

    /** Write one canonical self-delimiting value payload. */
    void encode(T value, M3MatSink sink);

    /** Read one value from a canonical payload. */
    T decode(M3MatSource source);

    default byte[] encode(T value) {
        M3MatSink sink = new M3MatSink();
        encode(Objects.requireNonNull(value, "value"), sink);
        return sink.toByteArray();
    }

    default T decode(byte[] bytes) {
        M3MatSource source = new M3MatSource(bytes);
        T value = decode(source);
        source.end();
        return value;
    }
}
