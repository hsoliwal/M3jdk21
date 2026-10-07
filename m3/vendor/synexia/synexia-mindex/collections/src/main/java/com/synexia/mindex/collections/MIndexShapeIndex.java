// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex.collections;

import com.synexia.mindex.MIndexObjectShape;
import java.util.Arrays;
import java.util.Objects;

/**
 * Canonical primitive identity for repeated collection/object shapes.
 *
 * <p>Each member is stored as a pair of MIndexString name ID and optional type
 * ID. Both lanes use the global MIndexString content-ID universe; type ID
 * {@code -1} means unspecified. The MIndexObjectShape bridge uses MIndexString
 * IDs of Java type names as the type lane. The underlying composite
 * index uses
 * exact packed-lane confirmation, so a hash collision never establishes shape
 * identity.
 */
public final class MIndexShapeIndex {
    public static final int UNSPECIFIED_TYPE_ID = -1;
    private static final Object SHAPE_DOMAIN = new Object();

    private final MIndexCompositeIndex composites;

    public MIndexShapeIndex() {
        this(new MIndexCompositeIndex());
    }

    public MIndexShapeIndex(
            MIndexCompositeIndex composites) {
        this.composites = Objects.requireNonNull(
                composites, "composites");
    }

    public int intern(MIndexObjectShape<?> shape) {
        MIndexObjectShape<?> actual =
                Objects.requireNonNull(shape, "shape");
        int[] memberNameIds =
                new int[actual.slotCount()];
        int[] memberTypeIds =
                new int[actual.slotCount()];
        for (int ordinal = 0;
                ordinal < memberNameIds.length;
                ordinal++) {
            MIndexObjectShape.Slot slot =
                    actual.slot(ordinal);
            memberNameIds[ordinal] = slot.nameId();
            memberTypeIds[ordinal] =
                    MIndexSpaces.stringId(
                            slot.type().getName());
        }
        return intern(memberNameIds, memberTypeIds);
    }

    public int intern(int[] memberNameIds) {
        Objects.requireNonNull(
                memberNameIds, "memberNameIds");
        int[] typeIds = new int[memberNameIds.length];
        Arrays.fill(typeIds, UNSPECIFIED_TYPE_ID);
        return intern(memberNameIds, typeIds);
    }

    public int intern(
            int[] memberNameIds,
            int[] memberTypeIds) {
        Objects.requireNonNull(
                memberNameIds, "memberNameIds");
        Objects.requireNonNull(
                memberTypeIds, "memberTypeIds");
        if (memberNameIds.length != memberTypeIds.length) {
            throw new IllegalArgumentException(
                    "shape name/type lane length mismatch");
        }

        MIndexSpace<?> strings = MIndexSpaces.strings();
        int[] lane = new int[memberNameIds.length << 1];
        for (int ordinal = 0;
                ordinal < memberNameIds.length;
                ordinal++) {
            int nameId = memberNameIds[ordinal];
            int typeId = memberTypeIds[ordinal];
            if (nameId < 0) {
                throw new IllegalArgumentException(
                        "negative member name ID");
            }
            strings.requireId(nameId);
            if (typeId < UNSPECIFIED_TYPE_ID) {
                throw new IllegalArgumentException(
                        "invalid member type ID");
            }
            if (typeId >= 0) {
                strings.requireId(typeId);
            }
            for (int previous = 0;
                    previous < ordinal;
                    previous++) {
                if (memberNameIds[previous] == nameId) {
                    throw new IllegalArgumentException(
                            "duplicate member name ID: "
                                    + nameId);
                }
            }
            lane[ordinal << 1] = nameId;
            lane[(ordinal << 1) + 1] = typeId;
        }
        return composites.intern(
                MIndexCompositeIndex.KIND_SHAPE,
                MIndexSpaces.strings(),
                SHAPE_DOMAIN,
                lane);
    }

    public MIndexCompositeIndex compositeIndex() {
        return composites;
    }

    public int memberCount(int shapeId) {
        requireShape(shapeId);
        return composites.length(shapeId) >>> 1;
    }

    public int nameId(int shapeId, int ordinal) {
        return composites.valueAt(
                requireShape(shapeId),
                Objects.checkIndex(
                        ordinal, memberCount(shapeId)) << 1);
    }

    public int typeId(int shapeId, int ordinal) {
        return composites.valueAt(
                requireShape(shapeId),
                (Objects.checkIndex(
                        ordinal, memberCount(shapeId)) << 1) + 1);
    }

    public int ordinalOfNameId(
            int shapeId,
            int nameId) {
        int members = memberCount(shapeId);
        for (int ordinal = 0; ordinal < members; ordinal++) {
            if (nameId(shapeId, ordinal) == nameId) {
                return ordinal;
            }
        }
        return -1;
    }

    public int[] copyNameIds(int shapeId) {
        int count = memberCount(shapeId);
        int[] result = new int[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            result[ordinal] = nameId(shapeId, ordinal);
        }
        return result;
    }

    public int[] copyTypeIds(int shapeId) {
        int count = memberCount(shapeId);
        int[] result = new int[count];
        for (int ordinal = 0; ordinal < count; ordinal++) {
            result[ordinal] = typeId(shapeId, ordinal);
        }
        return result;
    }

    public int[] copyLane(int shapeId) {
        return composites.copyLane(requireShape(shapeId));
    }

    public long structuralHash64(int shapeId) {
        return composites.structuralHash64(
                requireShape(shapeId));
    }

    private int requireShape(int shapeId) {
        if (composites.kind(shapeId)
                != MIndexCompositeIndex.KIND_SHAPE
                || !composites.hasDomains(
                        shapeId,
                        MIndexSpaces.strings(),
                        SHAPE_DOMAIN)) {
            throw new IllegalArgumentException(
                    "composite is not an MIndexString shape: "
                            + shapeId);
        }
        return shapeId;
    }
}
