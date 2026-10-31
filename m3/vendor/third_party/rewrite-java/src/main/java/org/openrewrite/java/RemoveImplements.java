// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/RemoveImplements.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.tree.TypeUtils.isOfClassType;

public final class RemoveImplements extends Recipe {
    private static final AnnotationMatcher OVERRIDE_MATCHER = new AnnotationMatcher("java.lang.Override");

    @Override
    public String getDisplayName() {
        return "Remove interface implementations";
    }

    @Override
    public String getDescription() {
        return "Removes `implements` clauses from classes implementing the specified interface. " + "Removes `@Overrides` annotations from methods which no longer override anything.";
    }

    @Option(displayName = "Interface Type", description = "The fully qualified name of the interface to remove.", example = "java.io.Serializable")
    private final String interfaceType;
    @Option(displayName = "Filter", description = "Only apply the interface removal to classes with fully qualified names that begin with this filter. " + "`null` or empty matches all classes.", example = "com.yourorg.", required = false)
    @Nullable
    private final String filter;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
                if (!(classDeclaration.getType() instanceof JavaType.Class) || classDeclaration.getImplements() == null) {
                    return super.visitClassDeclaration(classDeclaration, ctx);
                }
                JavaType.Class cdt = (JavaType.Class) classDeclaration.getType();
                if ((filter == null || cdt.getFullyQualifiedName().startsWith(filter)) && cdt.getInterfaces().stream().anyMatch(it -> isOfClassType(it, interfaceType))) {
                    return SearchResult.found(classDeclaration, "");
                }
                return super.visitClassDeclaration(classDeclaration, ctx);
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Nullable
            AnnotationService annotationService;
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                if (annotationService == null) {
                    annotationService = service(AnnotationService.class);
                }
                if (!(cd.getType() instanceof JavaType.Class) || cd.getImplements() == null) {
                    return super.visitClassDeclaration(cd, ctx);
                }
                JavaType.Class cdt = (JavaType.Class) cd.getType();
                if ((filter == null || cdt.getFullyQualifiedName().startsWith(filter)) && cdt.getInterfaces().stream().anyMatch(it -> isOfClassType(it, interfaceType))) {
                    cd = cd.withImplements(cd.getImplements().stream().filter(implement -> !isOfClassType(implement.getType(), interfaceType)).collect(toList()));
                    cdt = cdt.withInterfaces(cdt.getInterfaces().stream().filter(it -> !isOfClassType(it, interfaceType)).collect(toList()));
                    cd = cd.withType(cdt);
                    maybeRemoveImport(interfaceType);
                    getCursor().putMessage(cdt.getFullyQualifiedName(), cdt);
                }
                return super.visitClassDeclaration(cd, ctx);
            }
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration md, ExecutionContext ctx) {
                if (md.getMethodType() == null) {
                    return super.visitMethodDeclaration(md, ctx);
                }
                Object maybeClassType = getCursor().getNearestMessage(md.getMethodType().getDeclaringType().getFullyQualifiedName());
                if (!(maybeClassType instanceof JavaType.Class)) {
                    return super.visitMethodDeclaration(md, ctx);
                }
                JavaType.Class cdt = (JavaType.Class) maybeClassType;
                JavaType.Method mt = md.getMethodType().withDeclaringType(cdt);
                md = md.withMethodType(mt);
                if (md.getName().getType() != null) {
                    md = md.withName(md.getName().withType(mt));
                }
                updateCursor(md);
                assert annotationService != null;
                if (!annotationService.matches(getCursor(), OVERRIDE_MATCHER) || TypeUtils.isOverride(md.getMethodType())) {
                    return super.visitMethodDeclaration(md, ctx);
                }
                md = (J.MethodDeclaration) new RemoveAnnotation("@java.lang.Override").getVisitor().visitNonNull(md, ctx, getCursor().getParentOrThrow());
                return super.visitMethodDeclaration(md, ctx);
            }
        });
    }

    public RemoveImplements(final String interfaceType, @Nullable final String filter) {
        this.interfaceType = interfaceType;
        this.filter = filter;
    }

    public String getInterfaceType() {
        return this.interfaceType;
    }

    @Nullable
    public String getFilter() {
        return this.filter;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoveImplements(interfaceType=" + this.getInterfaceType() + ", filter=" + this.getFilter() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoveImplements)) return false;
        final RemoveImplements other = (RemoveImplements) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$interfaceType = this.getInterfaceType();
        final Object other$interfaceType = other.getInterfaceType();
        if (this$interfaceType == null ? other$interfaceType != null : !this$interfaceType.equals(other$interfaceType)) return false;
        final Object this$filter = this.getFilter();
        final Object other$filter = other.getFilter();
        if (this$filter == null ? other$filter != null : !this$filter.equals(other$filter)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RemoveImplements;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $interfaceType = this.getInterfaceType();
        result = result * PRIME + ($interfaceType == null ? 43 : $interfaceType.hashCode());
        final Object $filter = this.getFilter();
        result = result * PRIME + ($filter == null ? 43 : $filter.hashCode());
        return result;
    }
}
