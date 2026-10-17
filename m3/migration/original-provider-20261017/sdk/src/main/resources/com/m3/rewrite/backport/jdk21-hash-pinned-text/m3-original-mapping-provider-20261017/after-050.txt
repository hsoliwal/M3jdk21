// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

/** Two-field indexed declared-method view with an explicit java.lang.reflect.Method boundary. */
public final class M3Method {
    private final M3CI owner;
    private final int row;

    M3Method(M3CI owner, int row) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.row = Objects.checkIndex(row, owner.memberSize());
        if (owner.memberKind(row) != M3MemberKind.METHOD) {
            throw new IllegalArgumentException("member row is not a method");
        }
    }

    public M3CI owner() { return owner; }
    public int row() { return row; }
    public M3Class getDeclaringClass() { return owner.classAt(owner.memberDeclaringClassRow(row)); }
    public String name() { return owner.memberName(row); }
    public String getName() { return name().toString(); }
    public String descriptor() { return owner.memberDescriptor(row); }
    public int getModifiers() { return owner.memberModifiers(row); }
    public boolean isSynthetic() { return (owner.memberFlags(row) & M3MemberFlags.SYNTHETIC) != 0; }
    public boolean isBridge() { return (owner.memberFlags(row) & M3MemberFlags.BRIDGE) != 0; }
    public boolean isVarArgs() { return (owner.memberFlags(row) & M3MemberFlags.VARARGS) != 0; }
    public boolean isDefault() { return (owner.memberFlags(row) & M3MemberFlags.DEFAULT_METHOD) != 0; }
    public int annotationCount() { return owner.memberAnnotationCount(row); }
    public String annotationTypeNameAt(int ordinal) {
        return owner.memberAnnotationTypeName(row, ordinal);
    }
    public int exceptionCount() { return owner.memberExceptionCount(row); }
    public String exceptionTypeDescriptorAt(int ordinal) {
        return owner.memberExceptionTypeDescriptor(row, ordinal);
    }
    public long structuralHash64() { return owner.memberStructuralHash64(row); }

    public Optional<Method> tryMethod() {
        return owner.resolveReflectiveElement(row)
                .filter(Method.class::isInstance)
                .map(Method.class::cast);
    }

    public Method toMethod() {
        return tryMethod().orElseThrow(
                () -> new IllegalStateException("JVM Method boundary is not live for row " + row));
    }

    @Override public String toString() {
        return getDeclaringClass().getName() + "#" + getName() + descriptor();
    }
}
