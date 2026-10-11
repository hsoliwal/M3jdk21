// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ChangeStaticFieldToMethod.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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

import org.apache.commons.lang3.StringUtils;
import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.search.UsesField;
import org.openrewrite.java.tree.*;

public final class ChangeStaticFieldToMethod extends Recipe {
    @Option(displayName = "Old class name", description = "The fully qualified name of the class containing the field to replace.", example = "java.util.Collections")
    private final String oldClassName;
    @Option(displayName = "Old field name", description = "The simple name of the static field to replace.", example = "EMPTY_LIST")
    private final String oldFieldName;
    @Option(displayName = "New class name", description = "The fully qualified name of the class containing the method to use. Leave empty to keep the same class.", example = "java.util.List", required = false)
    @Nullable
    private final String newClassName;
    @Option(displayName = "New field target", description = "An optional method target that can be used to specify a static field within the new class.", example = "OK_RESPONSE", required = false)
    @Nullable
    private final String newTarget;
    @Option(displayName = "New method name", description = "The simple name of the method to use. The method must be static and have no arguments.", example = "of")
    private final String newMethodName;

    @Override
    public String getInstanceNameSuffix() {
        String shortType = oldClassName.substring(oldClassName.lastIndexOf('.') + 1);
        return String.format("`%s#%s` to `%s`", shortType, oldFieldName, newMethodName);
    }

    @Override
    public String getDisplayName() {
        return "Change static field access to static method access";
    }

    @Override
    public String getDescription() {
        return "Migrate accesses to a static field to invocations of a static method.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesField<>(oldClassName, oldFieldName), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (TypeUtils.isOfClassType(classDecl.getType(), oldClassName)) {
                    // Don't modify the class that declares the static field being replaced
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
            @Override
            public J visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                if (getCursor().firstEnclosing(J.Import.class) == null && TypeUtils.isOfClassType(fieldAccess.getTarget().getType(), oldClassName) && fieldAccess.getSimpleName().equals(oldFieldName)) {
                    return useNewMethod(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }
            @Override
            public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                JavaType.Variable varType = ident.getFieldType();
                if (varType != null && TypeUtils.isOfClassType(varType.getOwner(), oldClassName) && varType.getName().equals(oldFieldName)) {
                    return useNewMethod(ident);
                }
                return ident;
            }
            private J useNewMethod(TypeTree tree) {
                String newClass = newClassName == null ? oldClassName : newClassName;
                maybeRemoveImport(oldClassName);
                maybeAddImport(newClass);
                Cursor statementCursor = getCursor().dropParentUntil(Statement.class::isInstance);
                Statement statement = statementCursor.getValue();
                J.Block block = makeNewMethod(newClass).apply(statementCursor, statement.getCoordinates().replace());
                J.MethodInvocation method = block.getStatements().get(0).withPrefix(tree.getPrefix());
                if (method.getMethodType() == null) {
                    throw new IllegalArgumentException("Error while changing a static field to a method. The generated template using a the new class [" + newClass + "] and the method [" + newMethodName + "] resulted in a null method type.");
                }
                if (tree.getType() != null) {
                    JavaType.Method mt = method.getMethodType().withReturnType(tree.getType());
                    method = method.withMethodType(mt);
                    if (method.getName().getType() != null) {
                        method = method.withName(method.getName().withType(mt));
                    }
                }
                return method;
            }
            @NonNull
            private JavaTemplate makeNewMethod(String newClass) {
                String packageName = StringUtils.substringBeforeLast(newClass, ".");
                String simpleClassName = StringUtils.substringAfterLast(newClass, ".");
                String methodInvocationTemplate = "{" + simpleClassName + (newTarget != null ? "." + newTarget + "." : ".") + newMethodName + "();}";
                @Language("java")
                String methodStub;
                if (newTarget == null) {
                    methodStub = "package " + packageName + ";" + " public class " + simpleClassName + " {" + " public static void " + newMethodName + "() { return null; }" + " }";
                } else {
                    methodStub = "package " + packageName + ";" + " public class Target {" + " public static void " + newMethodName + "() { return null; }" + " }" + " public class " + simpleClassName + " {public static Target " + newTarget + ";}";
                }
                return JavaTemplate.builder(methodInvocationTemplate).contextSensitive().javaParser(JavaParser.fromJavaVersion().dependsOn(methodStub)).imports(newClass).build();
            }
        });
    }

    public ChangeStaticFieldToMethod(final String oldClassName, final String oldFieldName, @Nullable final String newClassName, @Nullable final String newTarget, final String newMethodName) {
        this.oldClassName = oldClassName;
        this.oldFieldName = oldFieldName;
        this.newClassName = newClassName;
        this.newTarget = newTarget;
        this.newMethodName = newMethodName;
    }

    public String getOldClassName() {
        return this.oldClassName;
    }

    public String getOldFieldName() {
        return this.oldFieldName;
    }

    @Nullable
    public String getNewClassName() {
        return this.newClassName;
    }

    @Nullable
    public String getNewTarget() {
        return this.newTarget;
    }

    public String getNewMethodName() {
        return this.newMethodName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ChangeStaticFieldToMethod(oldClassName=" + this.getOldClassName() + ", oldFieldName=" + this.getOldFieldName() + ", newClassName=" + this.getNewClassName() + ", newTarget=" + this.getNewTarget() + ", newMethodName=" + this.getNewMethodName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ChangeStaticFieldToMethod)) return false;
        final ChangeStaticFieldToMethod other = (ChangeStaticFieldToMethod) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$oldClassName = this.getOldClassName();
        final Object other$oldClassName = other.getOldClassName();
        if (this$oldClassName == null ? other$oldClassName != null : !this$oldClassName.equals(other$oldClassName)) return false;
        final Object this$oldFieldName = this.getOldFieldName();
        final Object other$oldFieldName = other.getOldFieldName();
        if (this$oldFieldName == null ? other$oldFieldName != null : !this$oldFieldName.equals(other$oldFieldName)) return false;
        final Object this$newClassName = this.getNewClassName();
        final Object other$newClassName = other.getNewClassName();
        if (this$newClassName == null ? other$newClassName != null : !this$newClassName.equals(other$newClassName)) return false;
        final Object this$newTarget = this.getNewTarget();
        final Object other$newTarget = other.getNewTarget();
        if (this$newTarget == null ? other$newTarget != null : !this$newTarget.equals(other$newTarget)) return false;
        final Object this$newMethodName = this.getNewMethodName();
        final Object other$newMethodName = other.getNewMethodName();
        if (this$newMethodName == null ? other$newMethodName != null : !this$newMethodName.equals(other$newMethodName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof ChangeStaticFieldToMethod;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $oldClassName = this.getOldClassName();
        result = result * PRIME + ($oldClassName == null ? 43 : $oldClassName.hashCode());
        final Object $oldFieldName = this.getOldFieldName();
        result = result * PRIME + ($oldFieldName == null ? 43 : $oldFieldName.hashCode());
        final Object $newClassName = this.getNewClassName();
        result = result * PRIME + ($newClassName == null ? 43 : $newClassName.hashCode());
        final Object $newTarget = this.getNewTarget();
        result = result * PRIME + ($newTarget == null ? 43 : $newTarget.hashCode());
        final Object $newMethodName = this.getNewMethodName();
        result = result * PRIME + ($newMethodName == null ? 43 : $newMethodName.hashCode());
        return result;
    }
}
