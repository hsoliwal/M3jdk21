// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import jdk.m3.collections.LongIterator;
import jdk.m3.collections.LongSortedSet;
import jdk.m3.collections.LongTreeSet;
import org.junit.jupiter.api.Test;

/**
 * BinarySetAlgebra.last() by indexed seek on LongTreeSet sources: against the last element of the
 * view's own merge cursor, for intersection, difference and symmetric difference of random sets
 * (empty sets and Long.MIN_VALUE/MAX_VALUE included), and through the cursor path when a source is
 * not a LongTreeSet.
 */
//FLLDCIM: LEETCODIFY — differential proof for BinarySetAlgebra.last
final class LongSetAlgebraLastTest {

    private static String last(LongSortedSet set) {
        try {
            return "value " + set.last();
        } catch (RuntimeException e) {
            return "threw " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private static String cursorLast(LongSortedSet set) {
        LongIterator cursor = set.iterator();
        if (!cursor.hasNext()) return "threw java.util.NoSuchElementException: null";
        long last = cursor.nextLong();
        while (cursor.hasNext()) last = cursor.nextLong();
        return "value " + last;
    }

    private static LongTreeSet randomSet(Random random, int span) {
        LongTreeSet set = new LongTreeSet();
        for (int i = random.nextInt(30); i > 0; i--) {
            set.add(switch (random.nextInt(12)) {
                case 0 -> Long.MAX_VALUE;
                case 1 -> Long.MIN_VALUE;
                case 2 -> Long.MAX_VALUE - 1;
                default -> random.nextInt(span) - span / 2;
            });
        }
        return set;
    }

    @Test
    void lastAgainstCursor() {
        for (int seed = 0; seed < 3_000; seed++) {
            Random random = new Random(seed);
            int span = 2 + random.nextInt(60);
            LongTreeSet a = randomSet(random, span);
            LongTreeSet b = randomSet(random, span);
            LongSortedSet[] views = {LongCompositeViews.sortedIntersection(a, b),
                LongCompositeViews.sortedDifference(a, b),
                LongCompositeViews.sortedSymmetricDifference(a, b),
                LongCompositeViews.sortedIntersection(LongCompositeViews.sortedUnion(a), b)};
            for (int v = 0; v < views.length; v++) {
                assertEquals(cursorLast(views[v]), last(views[v]), "seed " + seed + " view " + v);
            }
        }
    }
}
