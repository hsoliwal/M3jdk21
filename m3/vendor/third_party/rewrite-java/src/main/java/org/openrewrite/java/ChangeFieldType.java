// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeFieldType.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import static java.util.Collections.emptyList;

public class ChangeFieldType<P> extends JavaIsoVisitor<P> {
    private final String oldFullyQualifiedTypeName;
    private final JavaType.FullyQualified newFieldType;

    public ChangeFieldType(JavaType.FullyQualified oldFieldType, JavaType.FullyQualified newFieldType) {
        this.oldFullyQualifiedTypeName = oldFieldType.getFullyQualifiedName();
        this.newFieldType = newFieldType;
    }

    @Override
    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, P p) {
        JavaType.FullyQualified typeAsClass = multiVariable.getTypeAsFullyQualified();
        J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, p);
        if (typeAsClass != null && oldFullyQualifiedTypeName.equals(typeAsClass.getFullyQualifiedName())) {
            maybeAddImport(newFieldType);
            maybeRemoveImport(typeAsClass);
            mv = mv.withTypeExpression(mv.getTypeExpression() == null ? null : new J.Identifier(mv.getTypeExpression().getId(), mv.getTypeExpression().getPrefix(), Markers.EMPTY, emptyList(), newFieldType.getClassName(), newFieldType, null));
            mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                if (varType != null && !varType.equals(newFieldType)) {
                    return var.withType(newFieldType).withName(var.getName().withType(newFieldType));
                }
                return var;
            }));
        }
        return mv;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeFieldType)) return false;
        final ChangeFieldType<?> other = (ChangeFieldType<?>) o;
        if (!other.canEqual((Object) this)) return false;
        if (!super.equals(o)) return false;
        final Object this$oldFullyQualifiedTypeName = this.getOldFullyQualifiedTypeName();
        final Object other$oldFullyQualifiedTypeName = other.getOldFullyQualifiedTypeName();
        if (this$oldFullyQualifiedTypeName == null ? other$oldFullyQualifiedTypeName != null : !this$oldFullyQualifiedTypeName.equals(other$oldFullyQualifiedTypeName)) return false;
        final Object this$newFieldType = this.getNewFieldType();
        final Object other$newFieldType = other.getNewFieldType();
        if (this$newFieldType == null ? other$newFieldType != null : !this$newFieldType.equals(other$newFieldType)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeFieldType;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = super.hashCode();
        final Object $oldFullyQualifiedTypeName = this.getOldFullyQualifiedTypeName();
        result = result * PRIME + ($oldFullyQualifiedTypeName == null ? 43 : $oldFullyQualifiedTypeName.hashCode());
        final Object $newFieldType = this.getNewFieldType();
        result = result * PRIME + ($newFieldType == null ? 43 : $newFieldType.hashCode());
        return result;
    }

    public String getOldFullyQualifiedTypeName() {
        return this.oldFullyQualifiedTypeName;
    }

    public JavaType.FullyQualified getNewFieldType() {
        return this.newFieldType;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeFieldType(oldFullyQualifiedTypeName=" + this.getOldFullyQualifiedTypeName() + ", newFieldType=" + this.getNewFieldType() + ")";
    }
}
