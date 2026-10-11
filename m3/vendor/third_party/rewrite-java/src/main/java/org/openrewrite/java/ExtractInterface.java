// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/ExtractInterface.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

public class ExtractInterface {

    public static final class CreateInterface extends JavaIsoVisitor<ExecutionContext> {
        private final String fullyQualifiedInterfaceName;

        @Override
        public J postVisit(J tree, ExecutionContext ctx) {
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile c = (JavaSourceFile) requireNonNull(tree);
                String pkg = c.getPackageDeclaration() == null ? "" : Arrays.stream(c.getPackageDeclaration().getExpression().printTrimmed(getCursor()).split("\\.")).map(subpackage -> "..").collect(Collectors.joining("/", "../", "/"));
                String interfacePkg = JavaType.ShallowClass.build(fullyQualifiedInterfaceName).getPackageName();
                if (!interfacePkg.isEmpty()) {
                    c = c.withPackageDeclaration(new J.Package(randomId(), c.getPackageDeclaration() == null ? Space.EMPTY : c.getPackageDeclaration().getPrefix(), Markers.EMPTY, TypeTree.build(interfacePkg).withPrefix(Space.format(" ")), c.getPackageDeclaration() == null ? emptyList() : c.getPackageDeclaration().getAnnotations()));
                }
                c = (JavaSourceFile) c.withSourcePath(c.getSourcePath().resolve(pkg + fullyQualifiedInterfaceName.replace('.', '/') + ".java").normalize());
                return c;
            }
            return super.postVisit(tree, ctx);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration c = classDecl;
            c = c.withKind(J.ClassDeclaration.Kind.Type.Interface);
            c = c.withBody(c.getBody().withStatements(ListUtils.map(c.getBody().getStatements(), s -> s instanceof J.MethodDeclaration ? s : null)));
            c = c.withName(c.getName().withSimpleName(fullyQualifiedInterfaceName.substring(fullyQualifiedInterfaceName.lastIndexOf('.') + 1)));
            c = c.withType(JavaType.ShallowClass.build(fullyQualifiedInterfaceName).withKind(JavaType.FullyQualified.Kind.Interface));
            return autoFormat(super.visitClassDeclaration(c, ctx), ctx);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
            if (method.hasModifier(J.Modifier.Type.Static) || !method.hasModifier(J.Modifier.Type.Public) || method.isConstructor()) {
                //noinspection ConstantConditions
                return null;
            }
            return method.withModifiers(ListUtils.map(method.getModifiers(), m -> m.getType() == J.Modifier.Type.Public || m.getType() == J.Modifier.Type.Final || m.getType() == J.Modifier.Type.Native ? null : m)).withBody(null);
        }

        public CreateInterface(final String fullyQualifiedInterfaceName) {
            this.fullyQualifiedInterfaceName = fullyQualifiedInterfaceName;
        }

        public String getFullyQualifiedInterfaceName() {
            return this.fullyQualifiedInterfaceName;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ExtractInterface.CreateInterface(fullyQualifiedInterfaceName=" + this.getFullyQualifiedInterfaceName() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ExtractInterface.CreateInterface)) return false;
            final ExtractInterface.CreateInterface other = (ExtractInterface.CreateInterface) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
            final Object other$fullyQualifiedInterfaceName = other.getFullyQualifiedInterfaceName();
            if (this$fullyQualifiedInterfaceName == null ? other$fullyQualifiedInterfaceName != null : !this$fullyQualifiedInterfaceName.equals(other$fullyQualifiedInterfaceName)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof ExtractInterface.CreateInterface;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
            result = result * PRIME + ($fullyQualifiedInterfaceName == null ? 43 : $fullyQualifiedInterfaceName.hashCode());
            return result;
        }
    }


    public static final class ImplementAndAddOverrideAnnotations extends JavaIsoVisitor<ExecutionContext> {
        private final String fullyQualifiedInterfaceName;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (classDecl.getType() != null && classDecl.getType().getFullyQualifiedName().equals(fullyQualifiedInterfaceName)) {
                return classDecl;
            }
            maybeAddImport(fullyQualifiedInterfaceName);
            JavaType.ShallowClass type = JavaType.ShallowClass.build(fullyQualifiedInterfaceName);
            J.Block body = classDecl.getBody();
            Cursor parent = getCursor().getParent();
            assert parent != null;
            J.ClassDeclaration implementing = (J.ClassDeclaration) new ImplementInterface<>(classDecl, type).visitNonNull(classDecl, ctx, parent);
            return (J.ClassDeclaration) new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                    if (getCursor().getParentTreeCursor().getValue() == body) {
                        try {
                            if (!method.hasModifier(J.Modifier.Type.Public) || method.hasModifier(J.Modifier.Type.Static) || method.isConstructor()) {
                                return method;
                            }
                            if (FindAnnotations.find(method, "@java.lang.Override").isEmpty()) {
                                return JavaTemplate.apply("@Override", getCursor(), method.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName, new RuleBasedCollator("< Override"))));
                            }
                            return method;
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    return super.visitMethodDeclaration(method, ctx);
                }
            }.visitNonNull(implementing, ctx, getCursor().getParentOrThrow());
        }

        public ImplementAndAddOverrideAnnotations(final String fullyQualifiedInterfaceName) {
            this.fullyQualifiedInterfaceName = fullyQualifiedInterfaceName;
        }

        public String getFullyQualifiedInterfaceName() {
            return this.fullyQualifiedInterfaceName;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ExtractInterface.ImplementAndAddOverrideAnnotations(fullyQualifiedInterfaceName=" + this.getFullyQualifiedInterfaceName() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ExtractInterface.ImplementAndAddOverrideAnnotations)) return false;
            final ExtractInterface.ImplementAndAddOverrideAnnotations other = (ExtractInterface.ImplementAndAddOverrideAnnotations) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
            final Object other$fullyQualifiedInterfaceName = other.getFullyQualifiedInterfaceName();
            if (this$fullyQualifiedInterfaceName == null ? other$fullyQualifiedInterfaceName != null : !this$fullyQualifiedInterfaceName.equals(other$fullyQualifiedInterfaceName)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof ExtractInterface.ImplementAndAddOverrideAnnotations;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $fullyQualifiedInterfaceName = this.getFullyQualifiedInterfaceName();
            result = result * PRIME + ($fullyQualifiedInterfaceName == null ? 43 : $fullyQualifiedInterfaceName.hashCode());
            return result;
        }
    }
}
