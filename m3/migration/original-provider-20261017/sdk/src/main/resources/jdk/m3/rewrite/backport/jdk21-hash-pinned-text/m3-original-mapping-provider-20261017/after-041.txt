// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.reflect.Constructor;
import java.util.Objects;
import java.util.Optional;

/** Two-field indexed declared-constructor view with an explicit Constructor boundary. */
public final class M3Constructor {
    private final M3CI owner;
    private final int row;

    M3Constructor(M3CI owner, int row) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.row = Objects.checkIndex(row, owner.memberSize());
        if (owner.memberKind(row) != M3MemberKind.CONSTRUCTOR) {
            throw new IllegalArgumentException("member row is not a constructor");
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
    public boolean isVarArgs() { return (owner.memberFlags(row) & M3MemberFlags.VARARGS) != 0; }
    public int annotationCount() { return owner.memberAnnotationCount(row); }
    public String annotationTypeNameAt(int ordinal) {
        return owner.memberAnnotationTypeName(row, ordinal);
    }
    public int exceptionCount() { return owner.memberExceptionCount(row); }
    public String exceptionTypeDescriptorAt(int ordinal) {
        return owner.memberExceptionTypeDescriptor(row, ordinal);
    }
    public long structuralHash64() { return owner.memberStructuralHash64(row); }

    public Optional<Constructor<?>> tryConstructor() {
        return owner.resolveReflectiveElement(row)
                .filter(Constructor.class::isInstance)
                .map(element -> (Constructor<?>) element);
    }

    public Constructor<?> toConstructor() {
        return tryConstructor().orElseThrow(
                () -> new IllegalStateException(
                        "JVM Constructor boundary is not live for row " + row));
    }

    @Override public String toString() { return getName() + descriptor(); }
}
