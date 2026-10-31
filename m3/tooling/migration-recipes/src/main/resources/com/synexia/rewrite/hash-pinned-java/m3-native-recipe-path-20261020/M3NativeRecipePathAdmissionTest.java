/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
package com.synexia.rewrite;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Admission proves native PlainText paths cannot widen Java LST targets. */
public final class M3NativeRecipePathAdmissionTest {
    @Test void nativeOwnersOnly() throws Exception {
        Method method = M3HashPinnedJavaSnapshotRecipe.class
                .getDeclaredMethod("nativePath", String.class);
        method.setAccessible(true);
        assertTrue((boolean) method.invoke(null,
                "m3/collections/src/main/native/collections/m3_index_path_masks.c"));
        assertTrue((boolean) method.invoke(null, "src/java.base/share/native/libjava/owner.c"));
        assertFalse((boolean) method.invoke(null, "m3/collections/src/main/java/Owner.java"));
        assertFalse((boolean) method.invoke(null, "m3/collections/src/main/native/../secrets.c"));
        assertFalse((boolean) method.invoke(null, "m3/collections/src/main/native\\secrets.c"));
        assertFalse((boolean) method.invoke(null, "README.md"));
    }
    @Test void nativeCrateCanBeLoaded() {
        var recipe = new M3HashPinnedJavaSnapshotRecipe("m3-native-pathmask-stagecache-20261020", false);
        assertNotNull(recipe.getInitialValue(new org.openrewrite.InMemoryExecutionContext(
                throwable -> { throw new IllegalStateException(throwable); })));
    }
}
