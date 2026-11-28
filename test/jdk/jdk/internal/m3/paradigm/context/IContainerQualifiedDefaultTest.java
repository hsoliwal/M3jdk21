// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/*
 * @test
 * @summary M3 ContextOS DI-004: reject unsupported qualifiers
 * @modules jdk.m3.paradigm/jdk.internal.m3.paradigm.context
 * @run junit/othervm jdk.m3.test.context.IContainerQualifiedDefaultTest
 */

package jdk.m3.test.context;

import jdk.internal.m3.paradigm.context.IContainer;
import jdk.internal.m3.paradigm.context.AbstractContainer;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** DI-004: a missing qualifier must never silently choose the unqualified service. */
class IContainerQualifiedDefaultTest {
    @Test
    void compileTimeContainerWithoutQualifiedOverrideMustRefuseQualifier() {
        IContainer generated = new IContainer() {
            @Override
            public <T> T get(Class<T> type) {
                if (type == String.class) return type.cast("unqualified");
                throw new IllegalArgumentException("No binding: " + type.getName());
            }
        };
        assertEquals("unqualified", generated.get(String.class));
        assertThrows(IllegalArgumentException.class,
                () -> generated.get(String.class, "blue"));
        assertThrows(IllegalArgumentException.class,
                () -> generated.get(String.class, "missing"));
    }

    @Test
    void explicitQualifiedOwnerPreservesSeparateBindings() {
        AbstractContainer container = new AbstractContainer() {};
        container.bind(String.class, () -> "base");
        container.bind(String.class, "blue", () -> "qualified");
        assertEquals("base", container.get(String.class));
        assertEquals("qualified", container.get(String.class, "blue"));
        assertThrows(IllegalArgumentException.class,
                () -> container.get(String.class, "missing"));
    }

    @Test
    void childDelegationRetainsExactParentQualifier() {
        AbstractContainer parent = new AbstractContainer() {};
        parent.bind(String.class, "blue", () -> "parent-blue");
        IContainer child = parent.createChild();
        assertEquals("parent-blue", child.get(String.class, "blue"));
        assertThrows(IllegalArgumentException.class,
                () -> child.get(String.class, "missing"));
    }
}
