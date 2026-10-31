// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeFieldName.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JLeftPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public final class ChangeFieldName<P> extends JavaIsoVisitor<P> {
    private final String classType;
    private final String hasName;
    private final String toName;

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, p);
        J.ClassDeclaration enclosingClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (enclosingClass == null) {
            return v;
        }
        if (variable.isField(getCursor()) && matchesClass(enclosingClass.getType()) && variable.getSimpleName().equals(hasName)) {
            if (v.getVariableType() != null) {
                v = v.withVariableType(v.getVariableType().withName(toName));
            }
        }
        if (variable.getPadding().getInitializer() != null) {
            v = v.getPadding().withInitializer(visitLeftPadded(variable.getPadding().getInitializer(), JLeftPadded.Location.VARIABLE_INITIALIZER, p));
        }
        return v;
    }

    @Override
    public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, P p) {
        J.FieldAccess f = super.visitFieldAccess(fieldAccess, p);
        if (matchesClass(fieldAccess.getTarget().getType()) && fieldAccess.getSimpleName().equals(hasName)) {
            f = f.getPadding().withName(f.getPadding().getName().withElement(f.getPadding().getName().getElement().withSimpleName(toName)));
        }
        return f;
    }

    @Override
    public J.Identifier visitIdentifier(J.Identifier ident, P p) {
        J.Identifier i = super.visitIdentifier(ident, p);
        if (i.getFieldType() != null) {
            JavaType.Variable varType = i.getFieldType();
            if (varType.getName().equals(hasName) && TypeUtils.isOfClassType(varType.getOwner(), classType)) {
                if (varType.getOwner() instanceof JavaType.Method) {
                    return i;
                }
                return i.withSimpleName(toName).withFieldType(varType.withName(toName));
            }
        }
        return i;
    }

    private boolean matchesClass(@Nullable JavaType test) {
        JavaType.FullyQualified testClassType = TypeUtils.asFullyQualified(test);
        return testClassType != null && testClassType.getFullyQualifiedName().equals(classType);
    }

    public ChangeFieldName(final String classType, final String hasName, final String toName) {
        this.classType = classType;
        this.hasName = hasName;
        this.toName = toName;
    }

    public String getClassType() {
        return this.classType;
    }

    public String getHasName() {
        return this.hasName;
    }

    public String getToName() {
        return this.toName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeFieldName(classType=" + this.getClassType() + ", hasName=" + this.getHasName() + ", toName=" + this.getToName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeFieldName)) return false;
        final ChangeFieldName<?> other = (ChangeFieldName<?>) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$classType = this.getClassType();
        final Object other$classType = other.getClassType();
        if (this$classType == null ? other$classType != null : !this$classType.equals(other$classType)) return false;
        final Object this$hasName = this.getHasName();
        final Object other$hasName = other.getHasName();
        if (this$hasName == null ? other$hasName != null : !this$hasName.equals(other$hasName)) return false;
        final Object this$toName = this.getToName();
        final Object other$toName = other.getToName();
        if (this$toName == null ? other$toName != null : !this$toName.equals(other$toName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeFieldName;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $classType = this.getClassType();
        result = result * PRIME + ($classType == null ? 43 : $classType.hashCode());
        final Object $hasName = this.getHasName();
        result = result * PRIME + ($hasName == null ? 43 : $hasName.hashCode());
        final Object $toName = this.getToName();
        result = result * PRIME + ($toName == null ? 43 : $toName.hashCode());
        return result;
    }
}
