// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ReplaceConstantWithAnotherConstant.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public final class ReplaceConstantWithAnotherConstant extends Recipe {
    @Option(displayName = "Fully qualified name of the constant to replace", example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    private final String existingFullyQualifiedConstantName;
    @Option(displayName = "Fully qualified name of the constant to use in place of existing constant", example = "org.springframework.http.MediaType.APPLICATION_JSON_VALUE")
    private final String fullyQualifiedConstantName;

    @Override
    public String getDisplayName() {
        return "Replace constant with another constant";
    }

    @Override
    public String getDescription() {
        return "Replace a constant with another constant, adding/removing import on class if needed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.')), false), new ReplaceConstantWithAnotherConstantVisitor(existingFullyQualifiedConstantName, fullyQualifiedConstantName));
    }


    private static class ReplaceConstantWithAnotherConstantVisitor extends JavaVisitor<ExecutionContext> {
        private final String existingOwningType;
        private final String constantName;
        private final JavaType.FullyQualified newOwningType;
        private final String newConstantName;

        public ReplaceConstantWithAnotherConstantVisitor(String existingFullyQualifiedConstantName, String fullyQualifiedConstantName) {
            this.existingOwningType = existingFullyQualifiedConstantName.substring(0, existingFullyQualifiedConstantName.lastIndexOf('.'));
            this.constantName = existingFullyQualifiedConstantName.substring(existingFullyQualifiedConstantName.lastIndexOf('.') + 1);
            this.newOwningType = JavaType.ShallowClass.build(fullyQualifiedConstantName.substring(0, fullyQualifiedConstantName.lastIndexOf('.')));
            this.newConstantName = fullyQualifiedConstantName.substring(fullyQualifiedConstantName.lastIndexOf('.') + 1);
        }

        @Override
        public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
            JavaType.Variable fieldType = fieldAccess.getName().getFieldType();
            if (isConstant(fieldType)) {
                return replaceFieldAccess(fieldAccess, fieldType);
            }
            return super.visitFieldAccess(fieldAccess, ctx);
        }

        @Override
        public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
            JavaType.Variable fieldType = ident.getFieldType();
            if (isConstant(fieldType) && !isVariableDeclaration()) {
                return replaceFieldAccess(ident, fieldType);
            }
            return super.visitIdentifier(ident, ctx);
        }

        private J replaceFieldAccess(Expression expression, JavaType.Variable fieldType) {
            JavaType owner = fieldType.getOwner();
            while (owner instanceof JavaType.FullyQualified) {
                maybeRemoveImport(((JavaType.FullyQualified) owner).getFullyQualifiedName());
                owner = ((JavaType.FullyQualified) owner).getOwningClass();
            }
            if (expression instanceof J.Identifier) {
                maybeAddImport(newOwningType.getFullyQualifiedName(), newConstantName, false);
                J.Identifier identifier = (J.Identifier) expression;
                return identifier.withSimpleName(newConstantName).withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName));
            } else if (expression instanceof J.FieldAccess) {
                maybeAddImport(newOwningType.getFullyQualifiedName(), false);
                J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                Expression target = fieldAccess.getTarget();
                J.Identifier name = fieldAccess.getName();
                if (target instanceof J.Identifier) {
                    target = ((J.Identifier) target).withType(newOwningType).withSimpleName(newOwningType.getClassName());
                    name = name.withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName)).withSimpleName(newConstantName);
                } else {
                    target = (((J.FieldAccess) target).getName()).withType(newOwningType).withSimpleName(newOwningType.getClassName());
                    name = name.withFieldType(fieldType.withOwner(newOwningType).withName(newConstantName)).withSimpleName(newConstantName);
                }
                return fieldAccess.withTarget(target).withName(name);
            }
            return expression;
        }

        private boolean isConstant(@Nullable JavaType.Variable varType) {
            return varType != null && TypeUtils.isOfClassType(varType.getOwner(), existingOwningType) && varType.getName().equals(constantName);
        }

        private boolean isVariableDeclaration() {
            Cursor maybeVariable = getCursor().dropParentUntil(is -> is instanceof J.VariableDeclarations || is instanceof J.CompilationUnit);
            if (!(maybeVariable.getValue() instanceof J.VariableDeclarations)) {
                return false;
            }
            JavaType.Variable variableType = ((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getVariableType();
            if (variableType == null) {
                return true;
            }
            JavaType.FullyQualified ownerFqn = TypeUtils.asFullyQualified(variableType.getOwner());
            if (ownerFqn == null) {
                return true;
            }
            return constantName.equals(((J.VariableDeclarations) maybeVariable.getValue()).getVariables().get(0).getSimpleName()) && existingOwningType.equals(ownerFqn.getFullyQualifiedName());
        }
    }

    public ReplaceConstantWithAnotherConstant(final String existingFullyQualifiedConstantName, final String fullyQualifiedConstantName) {
        this.existingFullyQualifiedConstantName = existingFullyQualifiedConstantName;
        this.fullyQualifiedConstantName = fullyQualifiedConstantName;
    }

    public String getExistingFullyQualifiedConstantName() {
        return this.existingFullyQualifiedConstantName;
    }

    public String getFullyQualifiedConstantName() {
        return this.fullyQualifiedConstantName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ReplaceConstantWithAnotherConstant(existingFullyQualifiedConstantName=" + this.getExistingFullyQualifiedConstantName() + ", fullyQualifiedConstantName=" + this.getFullyQualifiedConstantName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ReplaceConstantWithAnotherConstant)) return false;
        final ReplaceConstantWithAnotherConstant other = (ReplaceConstantWithAnotherConstant) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$existingFullyQualifiedConstantName = this.getExistingFullyQualifiedConstantName();
        final Object other$existingFullyQualifiedConstantName = other.getExistingFullyQualifiedConstantName();
        if (this$existingFullyQualifiedConstantName == null ? other$existingFullyQualifiedConstantName != null : !this$existingFullyQualifiedConstantName.equals(other$existingFullyQualifiedConstantName)) return false;
        final Object this$fullyQualifiedConstantName = this.getFullyQualifiedConstantName();
        final Object other$fullyQualifiedConstantName = other.getFullyQualifiedConstantName();
        if (this$fullyQualifiedConstantName == null ? other$fullyQualifiedConstantName != null : !this$fullyQualifiedConstantName.equals(other$fullyQualifiedConstantName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ReplaceConstantWithAnotherConstant;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $existingFullyQualifiedConstantName = this.getExistingFullyQualifiedConstantName();
        result = result * PRIME + ($existingFullyQualifiedConstantName == null ? 43 : $existingFullyQualifiedConstantName.hashCode());
        final Object $fullyQualifiedConstantName = this.getFullyQualifiedConstantName();
        result = result * PRIME + ($fullyQualifiedConstantName == null ? 43 : $fullyQualifiedConstantName.hashCode());
        return result;
    }
}
