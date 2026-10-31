// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Objects;

/** Named primitive member flags retained beside the JDK modifier lane. */
public final class M3MemberFlags {
    public static final int SYNTHETIC = 1;
    public static final int BRIDGE = 1 << 1;
    public static final int VARARGS = 1 << 2;
    public static final int DEFAULT_METHOD = 1 << 3;
    public static final int ENUM_CONSTANT = 1 << 4;
    public static final int RECORD_COMPONENT = 1 << 5;

    private M3MemberFlags() {
    }

    public static int of(Field field) {
        Field checked = Objects.requireNonNull(field, "field");
        int flags = checked.isSynthetic() ? SYNTHETIC : 0;
        if (checked.isEnumConstant()) flags |= ENUM_CONSTANT;
        return flags;
    }

    public static int of(Method method) {
        Method checked = Objects.requireNonNull(method, "method");
        int flags = checked.isSynthetic() ? SYNTHETIC : 0;
        if (checked.isBridge()) flags |= BRIDGE;
        if (checked.isVarArgs()) flags |= VARARGS;
        if (checked.isDefault()) flags |= DEFAULT_METHOD;
        return flags;
    }

    public static int of(Constructor<?> constructor) {
        Constructor<?> checked = Objects.requireNonNull(constructor, "constructor");
        int flags = checked.isSynthetic() ? SYNTHETIC : 0;
        if (checked.isVarArgs()) flags |= VARARGS;
        return flags;
    }

    public static int of(RecordComponent component) {
        Objects.requireNonNull(component, "component");
        return RECORD_COMPONENT;
    }
}
