// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Objects;

/** Dependency-free JVM descriptor encoder used at the reflection admission boundary. */
public final class M3Descriptor {
    private M3Descriptor() {
    }

    /**
     * Returns the VM-owned descriptor spelling, including non-nominal hidden classes.
     * This does not make a hidden type name resolvable or portable between VM sessions.
     */
    public static String type(Class<?> type) {
        return Objects.requireNonNull(type, "type").descriptorString();
    }

    public static String field(Field field) {
        return type(Objects.requireNonNull(field, "field").getType());
    }

    public static String method(Method method) {
        Method checked = Objects.requireNonNull(method, "method");
        return executable(checked.getParameterTypes(), checked.getReturnType());
    }

    public static String constructor(Constructor<?> constructor) {
        Constructor<?> checked = Objects.requireNonNull(constructor, "constructor");
        return executable(checked.getParameterTypes(), void.class);
    }

    public static String recordComponent(RecordComponent component) {
        return type(Objects.requireNonNull(component, "component").getType());
    }

    private static String executable(Class<?>[] parameters, Class<?> returnType) {
        StringBuilder descriptor = new StringBuilder(16 + parameters.length * 8);
        descriptor.append('(');
        for (Class<?> parameter : parameters) {
            descriptor.append(type(parameter));
        }
        return descriptor.append(')').append(type(returnType)).toString();
    }
}
