// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Maven/JUnit integration for the same standalone exact two-phase collection corpus. */
final class PackedMultiMapFilterTest {
    @Test void preservedPrimitiveBehavior() throws Exception {
        PackedMultiMapFilterContract.main(new String[0]);
    }

    @Test void retainedStorageUsesPrimitiveLanesAndIndexesOnly() {
        boolean sawPrimitiveLane = false;
        for (Field field : PackedLongLongMultiMap.class.getDeclaredFields()) {
            Class<?> type = field.getType();
            assertFalse(Map.Entry.class.isAssignableFrom(type), "retained entry object: " + field);
            assertFalse(type == Long.class || type == Integer.class || type == Byte.class,
                    "retained boxed payload: " + field);
            if (type.isArray()) {
                assertTrue(type.getComponentType().isPrimitive(), "non-primitive entry lane: " + field);
                sawPrimitiveLane = true;
            }
        }
        assertTrue(sawPrimitiveLane, "expected primitive storage lanes");
    }
}
