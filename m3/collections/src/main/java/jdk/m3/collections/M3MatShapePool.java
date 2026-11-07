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
 * Canonical ordered record shapes, with exact field names owned by one M3MatPool.
 *
 * <p>One shape stores each field name as an integer ID; row values belong to the
 * caller and are never confused with structural metadata. Field order is significant.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatShapePool.java}
 * blob {@code 53c53250ae9c44d5dfa97207ef32d30d788d93c5} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class M3MatShapePool {
    private final M3MatPool<String> names;
    private final M3MatInternedRows shapes;

    private M3MatShapePool(M3MatPool<String> names, M3MatInternedRows shapes) {
        this.names = names;
        this.shapes = shapes;
    }

    public static Builder builder(M3MatPool<String> names, M3MatLimits limits) {
        return new Builder(names, limits);
    }

    public int size() { return shapes.size(); }
    public int fieldCount(int shape) { return shapes.length(shape); }
    public String fieldName(int shape, int field) {
        return names.value(shapes.get(shape, field));
    }
    public int fieldId(int shape, int field) { return shapes.get(shape, field); }
    public long payloadBytes() { return shapes.payloadBytes(); }

    /** Returns the position or -1 for a field absent from an existing shape. */
    public int fieldIndex(int shape, String field) {
        Objects.requireNonNull(field, "field");
        int id = names.idOf(field);
        int length = shapes.length(shape);
        // The UTF-8 codec can replace malformed Java UTF-16. Never alias such a
        // field to a different spelling merely because its encoded bytes collide.
        if (id < 0 || !names.value(id).equals(field)) return -1;
        for (int index = 0; index < length; index++) {
            if (shapes.get(shape, index) == id) return index;
        }
        return -1;
    }

    public int idOf(String... fields) {
        return shapes.idOf(ids(names, fields));
    }

    private static int[] ids(M3MatPool<String> names, String[] fields) {
        Objects.requireNonNull(fields, "fields");
        int[] ids = new int[fields.length];
        for (int index = 0; index < fields.length; index++) {
            String field = Objects.requireNonNull(fields[index], "field");
            int id = names.idOf(field);
            if (id < 0 || !names.value(id).equals(field)) {
                throw new IllegalArgumentException("field not exactly represented by symbol pool: " + field);
            }
            for (int earlier = 0; earlier < index; earlier++) {
                if (ids[earlier] == id) throw new IllegalArgumentException("duplicate field: " + field);
            }
            ids[index] = id;
        }
        return ids;
    }

    public static final class Builder {
        private final M3MatPool<String> names;
        private final M3MatInternedRows.Builder shapes;

        private Builder(M3MatPool<String> names, M3MatLimits limits) {
            this.names = Objects.requireNonNull(names, "names");
            this.shapes = M3MatInternedRows.builder(limits);
        }

        public int intern(String... fields) {
            return shapes.intern(ids(names, fields));
        }

        public int size() { return shapes.size(); }
        public M3MatShapePool build() { return new M3MatShapePool(names, shapes.build()); }
    }
}
