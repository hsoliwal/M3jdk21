// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3 ContextOS DI-004: delegate qualified lookup through default children
 * @modules jdk.m3.paradigm/jdk.internal.m3.paradigm.context:open
 * @compile --patch-module jdk.m3.paradigm=${test.src} --add-reads jdk.m3.paradigm=ALL-UNNAMED IContainerQualifiedChildTest.java
 * @run junit/othervm --patch-module jdk.m3.paradigm=${test.classes} --add-reads jdk.m3.paradigm=ALL-UNNAMED jdk.internal.m3.paradigm.context.IContainerQualifiedChildTest
 */

package jdk.internal.m3.paradigm.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/** DI-004 continuation: default child preserves qualified parent lookup. */
class IContainerQualifiedChildTest {
    private static IContainer parent() {
        return new IContainer() {
            @Override public <T> T get(Class<T> type) {
                if (type == String.class) return type.cast("parent-base");
                throw new IllegalArgumentException("missing unqualified: " + type);
            }
            @Override public <T> T get(Class<T> type, String qualifier) {
                if (type == String.class && "east".equals(qualifier)) {
                    return type.cast("parent-east");
                }
                throw new IllegalArgumentException("missing qualifier: " + qualifier);
            }
        };
    }

    @Test
    void generatedChildDelegatesExactParentQualifier() {
        IContainer child = parent().createChild();
        assertEquals("parent-base", child.get(String.class));
        assertEquals("parent-east", child.get(String.class, "east"));
        assertThrows(IllegalArgumentException.class,
                () -> child.get(String.class, "west"));
    }

    @Test
    void locallyBoundUnqualifiedDoesNotShadowQualifiedParent() {
        IContainer child = parent().createChild();
        child.bind(String.class, () -> "child-base");
        assertEquals("child-base", child.get(String.class));
        assertEquals("parent-east", child.get(String.class, "east"));
    }

    @Test
    void nestedGeneratedChildrenPropagateQualifier() {
        IContainer grandchild = parent().createChild().createChild();
        assertEquals("parent-east", grandchild.get(String.class, "east"));
        assertThrows(IllegalArgumentException.class,
                () -> grandchild.get(String.class, "missing"));
    }

    @Test
    void generatedParentWithoutQualifiedOverrideStillRefuses() {
        IContainer parent = new IContainer() {
            @Override public <T> T get(Class<T> type) {
                return type.cast("parent-base");
            }
        };
        IContainer child = parent.createChild();
        assertEquals("parent-base", child.get(String.class));
        assertThrows(IllegalArgumentException.class,
                () -> child.get(String.class, "east"));
    }
}
