// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import java.util.Objects;

/** Shared derived metadata for all nine built-in frozen collection families. */
public final class MIndexCollectionMetadata {
    /** Semantic ID lanes; queue priorities and table shape/header words are not ID occurrences. */
    public enum Role { ELEMENT, KEY, VALUE, SOURCE, RELATION, TARGET }

    private final MIndexCanonicalCollection source;
    private final MIndexIdIndex first;
    private final MIndexIdIndex second;
    private final MIndexIdIndex third;
    private final Role firstRole;
    private final MIndexRelations relations;

    MIndexCollectionMetadata(MIndexCanonicalCollection source) {
        this.source = Objects.requireNonNull(source, "source");
        MIndexIdIndex one;
        MIndexIdIndex two = null;
        MIndexIdIndex three = null;
        Role role = Role.ELEMENT;
        if (source instanceof MIndexFrozenList<?> value) {
            one = lane(value.space(), 0, 1, value.size());
        } else if (source instanceof MIndexFrozenSet<?> value) {
            one = lane(value.space(), 0, 1, value.size());
        } else if (source instanceof MIndexFrozenDeque<?> value) {
            one = lane(value.space(), 0, 1, value.size());
        } else if (source instanceof MIndexFrozenTuple<?> value) {
            one = lane(value.space(), 0, 1, value.size());
        } else if (source instanceof MIndexFrozenPriorityQueue<?> value) {
            one = lane(value.space(), 2, 3, value.size());
        } else if (source instanceof MIndexFrozenTable<?> value) {
            one = lane(value.space(), cellOffset(), 1, value.size());
        } else if (source instanceof MIndexFrozenMap<?, ?> value) {
            role = Role.KEY;
            one = lane(value.keySpace(), 0, 2, value.size());
            two = lane(value.valueSpace(), 1, 2, value.size());
        } else if (source instanceof MIndexFrozenMultiMap<?, ?> value) {
            role = Role.KEY;
            one = lane(value.keySpace(), 0, 2, value.size());
            two = lane(value.valueSpace(), 1, 2, value.size());
        } else if (source instanceof MIndexFrozenGraph<?, ?> value) {
            role = Role.SOURCE;
            one = lane(value.nodeSpace(), 0, 3, value.size());
            two = lane(value.relationSpace(), 1, 3, value.size());
            three = lane(value.nodeSpace(), 2, 3, value.size());
        } else {
            throw new IllegalArgumentException("unsupported frozen collection implementation");
        }
        first = one;
        second = two;
        third = three;
        firstRole = role;
        relations = second == null ? null : new MIndexRelations(first, third == null ? second : third);
    }

    public MIndexCompositeIndex compositeIndex() { return source.compositeIndex(); }
    public int canonicalId() { return source.canonicalId(); }
    public int kind() { return source.kind(); }
    public int size() { return source.size(); }
    public long structuralHash64() { return source.structuralHash64(); }
    public long signal64() { return source.signal64(); }

    public boolean hasRole(Role role) {
        Objects.requireNonNull(role, "role");
        return role == firstRole
                || firstRole == Role.KEY && role == Role.VALUE
                || firstRole == Role.SOURCE && (role == Role.RELATION || role == Role.TARGET);
    }

    /** Returns the shared prepared lane, with no new allocation. */
    public MIndexIdIndex column(Role role) {
        if (!hasRole(role)) { throw new IllegalArgumentException("role is absent from this shape"); }
        if (role == firstRole) { return first; }
        return role == Role.TARGET ? third : second;
    }

    /** Shared key-to-value or source-to-target incidence index. */
    public MIndexRelations relations() {
        if (relations == null) { throw new IllegalStateException("shape has no paired ID lanes"); }
        return relations;
    }

    /** Explicitly allocates a column index; logical positions are row ordinals. */
    public MIndexIdIndex tableColumn(int column) {
        MIndexFrozenTable<?> table = table();
        Objects.checkIndex(column, table.columns());
        return lane(table.space(), cellOffset() + column, table.columns(), table.rows());
    }

    /** Caller-owned derived indexes, separate from the bounded shared cache. */
    public MIndexRelations tableRelations(int fromColumn, int toColumn) {
        MIndexFrozenTable<?> table = table();
        Objects.checkIndex(fromColumn, table.columns());
        Objects.checkIndex(toColumn, table.columns());
        return new MIndexRelations(tableColumn(fromColumn), tableColumn(toColumn));
    }

    public long primitiveBytes() {
        return first.primitiveBytes() + (second == null ? 0 : second.primitiveBytes())
                + (third == null ? 0 : third.primitiveBytes());
    }

    static long estimatedPrimitiveBytes(MIndexCanonicalCollection source) {
        int lanes;
        if (source instanceof MIndexFrozenGraph<?, ?>) { lanes = 3; }
        else if (source instanceof MIndexFrozenMap<?, ?> || source instanceof MIndexFrozenMultiMap<?, ?>) {
            lanes = 2;
        } else if (source instanceof MIndexFrozenList<?> || source instanceof MIndexFrozenSet<?>
                || source instanceof MIndexFrozenDeque<?> || source instanceof MIndexFrozenTuple<?>
                || source instanceof MIndexFrozenTable<?> || source instanceof MIndexFrozenPriorityQueue<?>) {
            lanes = 1;
        } else { throw new IllegalArgumentException("unsupported frozen collection implementation"); }
        return Integer.BYTES * (3L * source.size() + 1) * lanes;
    }

    private MIndexIdIndex lane(MIndexSpace<?> space, int offset, int stride, int size) {
        return new MIndexIdIndex(compositeIndex(), canonicalId(), space, offset, stride, size);
    }

    private int cellOffset() { return compositeIndex().length(canonicalId()) - source.size(); }

    private MIndexFrozenTable<?> table() {
        if (source instanceof MIndexFrozenTable<?> table) { return table; }
        throw new IllegalStateException("shape is not a table");
    }
}
