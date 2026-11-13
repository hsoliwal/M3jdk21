// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-pattern-20261031/src/test/java/com/synexia/m3/qualification/M3OpenJdkIndexMaskJniProbeTest.java blob f47e1c223567; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
final class OpenJdkIndexMaskJniProbeTest {
    @Test
    void refusesAnUnqualifiedInvocationBeforeJNI() {
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkIndexMaskJniProbe.main(new String[]{}));
        assertThrows(IllegalArgumentException.class,
                () -> OpenJdkIndexMaskJniProbe.main(new String[]{"--bypass-native"}));
    }

    @Test
    void rejectsJavaOnlyBackendDespiteMatchingPrivateMethodNames() {
        assertThrows(IllegalStateException.class,
                () -> OpenJdkIndexMaskJniProbe.requireDeclaredNativeSymbols(JavaOnly.class));
    }

    @Test
    void rejectsMissingNativeAbiAndWrongReturnTypes() {
        assertThrows(NoSuchMethodException.class,
                () -> OpenJdkIndexMaskJniProbe.requireDeclaredNativeSymbols(Missing.class));
        assertThrows(IllegalStateException.class,
                () -> OpenJdkIndexMaskJniProbe.requireDeclaredNativeSymbols(WrongReturn.class));
    }

    @Test
    void acceptsDeclaredNativeSignatureButDoesNotClaimALoadedLibrary() {
        assertDoesNotThrow(
                () -> OpenJdkIndexMaskJniProbe.requireDeclaredNativeSymbols(
                        DeclaredNativeOnly.class));
    }

    @Test
    void unlinkedNativeDeclarationsCannotSatisfyDirectCExportWitness() throws Exception {
        var declared = DeclaredNativeOnly.class.getDeclaredMethod(
                "selectPositionsNative", byte[].class, int.class);
        if (!declared.trySetAccessible()) throw new AssertionError("test native fixture inaccessible");
        assertThrows(UnsatisfiedLinkError.class,
                () -> OpenJdkIndexMaskJniProbe.selectNative(declared, new byte[1], 1));
    }

    private static final class Missing { }

    private static final class JavaOnly {
        private static int positionsAbiNative() { return 1; }
        private static int[] selectPositionsNative(byte[] data, int mask) { return new int[0]; }
        private static int positionsMasksAbiNative() { return 1; }
        private static int[] selectPositionsMasksNative(byte[] data, int need, int exclude) {
            return new int[0];
        }
    }

    private static final class WrongReturn {
        private static native long positionsAbiNative();
        private static native int[] selectPositionsNative(byte[] data, int mask);
        private static native int positionsMasksAbiNative();
        private static native int[] selectPositionsMasksNative(byte[] data, int need, int exclude);
    }

    private static final class DeclaredNativeOnly {
        private static native int positionsAbiNative();
        private static native int[] selectPositionsNative(byte[] data, int mask);
        private static native int positionsMasksAbiNative();
        private static native int[] selectPositionsMasksNative(byte[] data, int need, int exclude);
    }
}
