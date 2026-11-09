// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class StableDeduplicatorTest {
    @Test
    void preservesFirstOccurrenceAndInputOrder() {
        record Row(String id, String value) {}

        List<Row> result = StableDeduplicator.distinctBy(
                List.of(
                        new Row("b", "first-b"),
                        new Row("a", "first-a"),
                        new Row("b", "second-b"),
                        new Row("c", "first-c"),
                        new Row("a", "second-a")),
                Row::id);

        assertEquals(
                List.of(
                        new Row("b", "first-b"),
                        new Row("a", "first-a"),
                        new Row("c", "first-c")),
                result);
    }

    @Test
    void reportsDuplicateKeysInFirstDuplicateOrder() {
        assertEquals(
                List.of("b", "a"),
                StableDeduplicator.duplicateKeys(
                        List.of("b1", "a1", "b2", "c1", "a2", "b3"),
                        value -> value.substring(0, 1)));
    }
}
