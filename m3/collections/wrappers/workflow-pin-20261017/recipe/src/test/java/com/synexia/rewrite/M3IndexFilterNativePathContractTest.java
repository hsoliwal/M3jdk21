// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * The JNI native-source grant is exactly one reviewed product path, not a
 * generic new Maven-module native-source privilege. Source retention and
 * JNI correctness remain independently guarded by the manifest and C17 tests.
 */
final class M3IndexFilterNativePathContractTest {
    private static boolean admitted(String path) throws ReflectiveOperationException {
        Method method = M3HashPinnedTextSnapshotRecipe.class.getDeclaredMethod("textPath", String.class);
        method.setAccessible(true);
        try {
            return (Boolean) method.invoke(null, path);
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw failure;
        }
    }

    @Test void acceptsOriginalNativeRootsAndExactlyOneModuleJniOwner() throws Exception {
        assertTrue(admitted("src/main/native/hello.c"));
        assertTrue(admitted("m3/collections/src/main/native/collections/m3_int_filter_jni.c"));
    }

    @Test void refusesUnreviewedModuleNativeAndUnsafePaths() throws Exception {
        assertFalse(admitted("m3/collections/src/main/native/collections/another_filter.c"));
        assertFalse(admitted("m3/collections/src/main/native/collections/m3_int_filter_jni.h"));
        assertFalse(admitted("m3/collections/src/main/native/collections/../m3_int_filter_jni.c"));
        assertFalse(admitted("m3/collections/src/main/native/collections/m3_int_filter_jni.java"));
        assertFalse(admitted("m3/collections/src/test/native/collections/m3_int_filter_jni.c"));
        assertFalse(admitted("m3/other-module/src/main/native/m3_int_filter_jni.c"));
        assertFalse(admitted("../m3/collections/src/main/native/collections/m3_int_filter_jni.c"));
    }
}
