// SPDX-License-Identifier: Apache-2.0
package jdk.m3.precompute;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class EditDistancePlanTest {
    @Test
    void deterministicExamplesAndThresholds() {
        EditDistancePlan kitten = EditDistancePlan.compile("kitten");
        assertTrue(kitten.usesMyers());
        assertEquals(3, kitten.distance("sitting"));
        assertTrue(kitten.within("sitten", 1));
        assertFalse(kitten.within("sitting", 2));
        assertTrue(kitten.within("sitting", 3));

        EditDistancePlan empty = EditDistancePlan.compile("");
        assertEquals(3, empty.distance("abc"));
        assertTrue(empty.within("abc", 3));
        assertFalse(empty.within("abc", 2));
    }

    @Test
    void randomizedMyersAndDynamicPathsMatchIndependentDp() {
        Random random = new Random(0x4d593352L);
        char[] alphabet = {'a', 'b', 'c', '\u0100', '\ud800', '\udc00'};

        for (int sample = 0; sample < 1000; sample++) {
            int patternLength = random.nextInt(90);
            int sourceLength = random.nextInt(90);
            String pattern = randomString(random, alphabet, patternLength);
            String source = randomString(random, alphabet, sourceLength);
            int expected = exact(pattern, source);

            EditDistancePlan plan = EditDistancePlan.compile(pattern);
            assertEquals(expected, plan.distance(source), "sample=" + sample);
            for (int threshold : new int[] {0, 1, 2, 3, 5, 8, 16, expected}) {
                assertEquals(
                        expected <= threshold,
                        plan.within(source, threshold),
                        "sample=" + sample + " threshold=" + threshold);
            }
        }
    }

    @Test
    void workspaceOwnershipAndBudgetsFailClosed() {
        EditDistancePlan first = EditDistancePlan.compile("abcdef");
        EditDistancePlan second = EditDistancePlan.compile("abcdef");
        EditDistancePlan.Workspace workspace = first.workspace();
        assertThrows(
                IllegalArgumentException.class,
                () -> second.distance("abc", workspace, null));
        assertThrows(
                IllegalArgumentException.class,
                () -> EditDistancePlan.compile("x".repeat(1_048_577)));
        assertThrows(
                IllegalArgumentException.class,
                () -> first.distance("x".repeat(1_048_577)));
        EditDistancePlan large = EditDistancePlan.compile("x".repeat(20_000));
        assertThrows(
                IllegalArgumentException.class,
                () -> large.distance("y".repeat(20_000)));
    }

    private static String randomString(Random random, char[] alphabet, int length) {
        char[] value = new char[length];
        for (int index = 0; index < value.length; index++) {
            value[index] = alphabet[random.nextInt(alphabet.length)];
        }
        return new String(value);
    }

    private static int exact(String pattern, String source) {
        int[] previous = new int[pattern.length() + 1];
        int[] current = new int[pattern.length() + 1];
        for (int column = 0; column <= pattern.length(); column++) previous[column] = column;
        for (int row = 1; row <= source.length(); row++) {
            current[0] = row;
            for (int column = 1; column <= pattern.length(); column++) {
                current[column] =
                        Math.min(
                                Math.min(current[column - 1] + 1, previous[column] + 1),
                                previous[column - 1]
                                        + (pattern.charAt(column - 1) == source.charAt(row - 1) ? 0 : 1));
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[pattern.length()];
    }
}
