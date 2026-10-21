// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.util.Objects;

/** Named primitive class-kind flags; values are computed once at snapshot admission. */
public final class M3ClassFlags {
    public static final int INTERFACE = 1;
    public static final int ENUM = 1 << 1;
    public static final int ANNOTATION = 1 << 2;
    public static final int RECORD = 1 << 3;
    public static final int ARRAY = 1 << 4;
    public static final int PRIMITIVE = 1 << 5;
    public static final int SEALED = 1 << 6;
    public static final int HIDDEN = 1 << 7;
    public static final int SYNTHETIC = 1 << 8;
    public static final int ANONYMOUS = 1 << 9;
    public static final int LOCAL = 1 << 10;
    public static final int MEMBER = 1 << 11;

    private M3ClassFlags() {
    }

    public static int of(Class<?> type) {
        Class<?> checked = Objects.requireNonNull(type, "type");
        int flags = 0;
        if (checked.isInterface()) flags |= INTERFACE;
        if (checked.isEnum()) flags |= ENUM;
        if (checked.isAnnotation()) flags |= ANNOTATION;
        if (checked.isRecord()) flags |= RECORD;
        if (checked.isArray()) flags |= ARRAY;
        if (checked.isPrimitive()) flags |= PRIMITIVE;
        if (checked.isSealed()) flags |= SEALED;
        if (checked.isHidden()) flags |= HIDDEN;
        if (checked.isSynthetic()) flags |= SYNTHETIC;
        if (checked.isAnonymousClass()) flags |= ANONYMOUS;
        if (checked.isLocalClass()) flags |= LOCAL;
        if (checked.isMemberClass()) flags |= MEMBER;
        return flags;
    }
}
