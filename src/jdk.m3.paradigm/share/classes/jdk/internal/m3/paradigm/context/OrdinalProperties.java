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

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only Map boundary over a schema-bound ordinal map; hot lookup does not allocate entries.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextOrdinalProperties.java}
 * blob {@code d85209ef6230e3e2739d6dfb706b75f2f4dda744} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class OrdinalProperties extends AbstractMap<String, Object> {
    private final OrdinalMap<Object> values;

    private OrdinalProperties(OrdinalMap<Object> values) {
        this.values = values;
    }

    public static OrdinalProperties empty(OrdinalSchema schema) {
        return new OrdinalProperties(OrdinalMap.empty(schema));
    }

    public OrdinalSchema schema() { return values.schema(); }
    public long presentBits() { return values.presentBits(); }

    public OrdinalProperties with(String key, Object value) {
        OrdinalMap<Object> next = values.with(key, value);
        return next == values ? this : new OrdinalProperties(next);
    }

    public OrdinalProperties without(String key) {
        OrdinalMap<Object> next = values.without(key);
        return next == values ? this : new OrdinalProperties(next);
    }

    @Override public int size() { return values.size(); }

    @Override
    public boolean containsKey(Object key) {
        return key instanceof String text && values.containsKey(text);
    }

    @Override
    public Object get(Object key) {
        return key instanceof String text ? values.get(text) : null;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return new AbstractSet<>() {
            @Override public int size() { return values.size(); }

            @Override
            public Iterator<Entry<String, Object>> iterator() {
                return new Iterator<>() {
                    private long remaining = values.presentBits();

                    @Override public boolean hasNext() { return remaining != 0L; }

                    @Override
                    public Entry<String, Object> next() {
                        if (remaining == 0L) throw new NoSuchElementException();
                        int ordinal = Long.numberOfTrailingZeros(remaining);
                        remaining &= remaining - 1L;
                        return Map.entry(
                                values.schema().name(ordinal),
                                Objects.requireNonNull(values.get(ordinal)));
                    }
                };
            }
        };
    }
}
