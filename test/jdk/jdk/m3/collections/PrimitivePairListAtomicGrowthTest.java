// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Tests every primitive kind pairing without per-entry object allocations. */
final class PrimitivePairListAtomicGrowthTest {
    @Test
    void failedSecondArrayCopyPreservesEveryOneOfSixtyFourPrimitivePairShapes() {
        for (PrimitiveKind left : PrimitiveKind.values()) {
            for (PrimitiveKind right : PrimitiveKind.values()) {
                PrimitivePairList pairs = new PrimitivePairList(left, right, 1);
                pairs.addBits(1, 2);
                Object oldLeft = pairs.leftBackingArray();
                Object oldRight = pairs.rightBackingArray();
                long oldPayload = pairs.payloadBytes();
                AtomicInteger calls = new AtomicInteger();
                assertThrows(OutOfMemoryError.class, () -> pairs.ensure(2, (kind, source, capacity) -> {
                    if (calls.incrementAndGet() == 2) throw new OutOfMemoryError("second lane");
                    return kind.copyOf(source, capacity);
                }));
                assertEquals(2, calls.get());
                assertEquals(1, pairs.capacity());
                assertEquals(1, pairs.size());
                assertEquals(oldPayload, pairs.payloadBytes());
                assertSame(oldLeft, pairs.leftBackingArray());
                assertSame(oldRight, pairs.rightBackingArray());
                assertEquals(left.storageBits(1), pairs.leftBits(0));
                assertEquals(right.storageBits(2), pairs.rightBits(0));
                pairs.addBits(3, 4);
                assertEquals(2, pairs.size());
                assertEquals(left.storageBits(3), pairs.leftBits(1));
                assertEquals(right.storageBits(4), pairs.rightBits(1));
            }
        }
    }

    @Test
    void wrongArrayTypeNullAndWrongLengthAreRejectedBeforePublication() {
        PrimitivePairList pairs = new PrimitivePairList(PrimitiveKind.LONG, PrimitiveKind.DOUBLE, 1);
        pairs.addBits(9, Double.doubleToRawLongBits(-0.0));
        Object left = pairs.leftBackingArray();
        Object right = pairs.rightBackingArray();
        assertThrows(IllegalStateException.class, () -> pairs.ensure(2,
                (kind, source, capacity) -> new int[capacity]));
        assertThrows(IllegalStateException.class, () -> pairs.ensure(2,
                (kind, source, capacity) -> kind.copyOf(source, capacity - 1)));
        assertThrows(NullPointerException.class, () -> pairs.ensure(2,
                (kind, source, capacity) -> null));
        assertSame(left, pairs.leftBackingArray());
        assertSame(right, pairs.rightBackingArray());
        assertEquals(Double.doubleToRawLongBits(-0.0), pairs.rightBits(0));
    }

    @Test
    void capacityAlreadySufficientAvoidsCallbackAndExistingIdentityRemainsStable() {
        PrimitivePairList pairs = new PrimitivePairList(PrimitiveKind.INT, PrimitiveKind.FLOAT, 8);
        pairs.addBits(42, Float.floatToRawIntBits(1.5f));
        Object left = pairs.leftBackingArray();
        pairs.ensure(8, (kind, source, capacity) -> {
            throw new AssertionError("unexpected copy");
        });
        assertSame(left, pairs.leftBackingArray());
        assertEquals(8, pairs.capacity());
    }
}
