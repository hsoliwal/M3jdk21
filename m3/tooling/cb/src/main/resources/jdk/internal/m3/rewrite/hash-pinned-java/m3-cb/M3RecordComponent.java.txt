// SPDX-License-Identifier: Apache-2.0
package jdk.internal.mindex;

import java.lang.reflect.RecordComponent;
import java.util.Objects;
import java.util.Optional;

/** Two-field indexed record-component view with an explicit RecordComponent boundary. */
public final class M3RecordComponent {
    private final M3CI owner;
    private final int row;

    M3RecordComponent(M3CI owner, int row) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.row = Objects.checkIndex(row, owner.memberSize());
        if (owner.memberKind(row) != M3MemberKind.RECORD_COMPONENT) {
            throw new IllegalArgumentException("member row is not a record component");
        }
    }

    public M3CI owner() { return owner; }
    public int row() { return row; }
    public M3Class getDeclaringRecord() { return owner.classAt(owner.memberDeclaringClassRow(row)); }
    public String name() { return owner.memberName(row); }
    public String getName() { return name().toString(); }
    public String descriptor() { return owner.memberDescriptor(row); }
    public int annotationCount() { return owner.memberAnnotationCount(row); }
    public String annotationTypeNameAt(int ordinal) {
        return owner.memberAnnotationTypeName(row, ordinal);
    }
    public long structuralHash64() { return owner.memberStructuralHash64(row); }

    public Optional<RecordComponent> tryRecordComponent() {
        return owner.resolveReflectiveElement(row)
                .filter(RecordComponent.class::isInstance)
                .map(RecordComponent.class::cast);
    }

    public RecordComponent toRecordComponent() {
        return tryRecordComponent().orElseThrow(
                () -> new IllegalStateException(
                        "JVM RecordComponent boundary is not live for row " + row));
    }

    @Override public String toString() { return getDeclaringRecord().getName() + "#" + getName(); }
}
