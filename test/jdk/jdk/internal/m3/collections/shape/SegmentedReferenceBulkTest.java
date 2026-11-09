/* Copyright 2026 Hitesh Soliwal <hsoliwal@gmail.com>
 * SPDX-License-Identifier: Apache-2.0 */
package jdk.internal.m3.collections.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Random;
import org.junit.jupiter.api.Test;

final class SegmentedReferenceBulkTest {
    @Test void referenceMemmovePreservesNullsAndIdentity() {
        Object[] expected = new Object[14000];
        SegmentedObjectLane28<Object> lane = new SegmentedObjectLane28<>();
        for (int i = 0; i < expected.length; i += 29) { expected[i] = new Object(); lane.set(i, expected[i]); }
        Random random = new Random(17);
        for (int pass = 0; pass < 400; pass++) {
            int n = random.nextInt(7000);
            int from = random.nextInt(expected.length - n + 1);
            int to = random.nextInt(expected.length - n + 1);
            System.arraycopy(expected, from, expected, to, n);
            lane.copyWithin(from, to, n);
            for (int i = 0; i < expected.length; i++) assertSame(expected[i], lane.get(i));
        }
    }

    @Test void positionalSequenceRetainsItsGuardAndOrderingContract() {
        var required = new CollectionRequirements(CollectionRequirements.Kind.SEQUENCE,
                CollectionRequirements.Order.POSITION, CollectionRequirements.Equality.IDENTITY,
                CollectionRequirements.Concurrency.SINGLE_THREAD, true, 20000);
        SegmentedShapeSequence<Object> sequence = new SegmentedShapeSequence<>(required);
        ArrayList<Object> oracle = new ArrayList<>();
        for (int i = 0; i < 9000; i++) { Object value = i % 7 == 0 ? null : new Object(); sequence.add(value); oracle.add(value); }
        Random random = new Random(42);
        for (int pass = 0; pass < 500; pass++) {
            if (pass % 2 == 0) {
                int position = random.nextInt(oracle.size() + 1); Object value = new Object();
                sequence.insert(position, value); oracle.add(position, value);
            } else {
                int position = random.nextInt(oracle.size());
                assertSame(oracle.remove(position), sequence.removeAt(position));
            }
        }
        assertEquals(oracle.size(), sequence.size());
        for (int i = 0; i < oracle.size(); i++) assertSame(oracle.get(i), sequence.get(i));
        assertThrows(IllegalStateException.class, () -> sequence.forEach(value -> sequence.removeAt(0)));
        assertSame(oracle.remove(0), sequence.removeAt(0));
        assertThrows(IndexOutOfBoundsException.class, () -> sequence.insert(-1, new Object()));
    }
}
