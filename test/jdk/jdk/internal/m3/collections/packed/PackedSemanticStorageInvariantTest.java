// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections.packed;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PackedSemanticStorageInvariantTest {
    @Test
    void retainedArraysArePrimitiveOnly() {
        assertPrimitiveArraysOnly(PackedSemanticState.class);
        assertPrimitiveArraysOnly(PackedRelationMatrix.class);
        assertPrimitiveArraysOnly(PackedRuleProgram.class);
        assertPrimitiveArraysOnly(PackedSemanticMachine.class);
    }

    @Test
    void semanticCoreRetainsNoCollectionsMapsOrBoxedScalars() {
        assertDenseFields(PackedSemanticState.class);
        assertDenseFields(PackedRelationMatrix.class);
        assertDenseFields(PackedRuleProgram.class);
        assertDenseFields(PackedSemanticMachine.class);
    }

    @Test
    void meaningNamesStayAtInterpretationBoundary() {
        assertTrue(PackedActionAdmissionUseCase.isDerivedFact(
                PackedActionAdmissionUseCase.EXECUTING));
        assertFalse(PackedActionAdmissionUseCase.isDerivedFact(
                PackedActionAdmissionUseCase.STARTED));
        assertTrue(PackedActionAdmissionUseCase.factName(
                PackedActionAdmissionUseCase.BLOCKED).equals("BLOCKED"));
        assertTrue(PackedActionAdmissionUseCase.scalarName(
                PackedActionAdmissionUseCase.RISK).equals("RISK"));
    }

    @Test
    void semanticCoreRetainsNoCollectionsOrMaps() {
        assertNoCollectionFields(PackedSemanticState.class);
        assertNoCollectionFields(PackedRelationMatrix.class);
        assertNoCollectionFields(PackedRuleProgram.class);
        assertNoCollectionFields(PackedSemanticMachine.class);
    }

    private static void assertPrimitiveArraysOnly(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            if (fieldType.isArray()) {
                assertTrue(
                        fieldType.getComponentType().isPrimitive(),
                        () -> type.getSimpleName() + "." + field.getName());
            }
        }
    }

    private static void assertDenseFields(Class<?> type) {
        assertNoCollectionFields(type);
    }

    private static void assertNoCollectionFields(Class<?> type) {
        for (Field field : type.getDeclaredFields()) {
            Class<?> fieldType = field.getType();
            assertFalse(
                    Collection.class.isAssignableFrom(fieldType),
                    () -> type.getSimpleName() + "." + field.getName());
            assertFalse(
                    Map.class.isAssignableFrom(fieldType),
                    () -> type.getSimpleName() + "." + field.getName());
            assertFalse(
                    fieldType == Long.class
                            || fieldType == Integer.class
                            || fieldType == Byte.class
                            || fieldType == Short.class
                            || fieldType == Float.class
                            || fieldType == Double.class
                            || fieldType == Boolean.class
                            || fieldType == Character.class,
                    () -> type.getSimpleName() + "." + field.getName() + " retains boxed scalar");
        }
    }
}
