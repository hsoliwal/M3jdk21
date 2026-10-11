// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindDeprecatedClasses.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import java.util.Iterator;

public final class FindDeprecatedClasses extends Recipe {
    private static final AnnotationMatcher DEPRECATED_MATCHER = new AnnotationMatcher("java.lang.Deprecated");
    @Option(displayName = "Type pattern", description = "A type pattern that is used to find matching classes.", example = "org.springframework..*", required = false)
    @Nullable
    private final String typePattern;
    @Option(displayName = "Match inherited", description = "When enabled, find types that inherit from a deprecated type.", required = false)
    @Nullable
    private final Boolean matchInherited;
    @Option(displayName = "Ignore deprecated scopes", description = "When a deprecated type is used in a deprecated method or class, ignore it.", required = false)
    @Nullable
    private final Boolean ignoreDeprecatedScopes;

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated classes";
    }

    @Override
    public String getInstanceNameSuffix() {
        if (typePattern != null) {
            return "matching `" + typePattern + "`";
        }
        return super.getInstanceNameSuffix();
    }

    @Override
    public String getDescription() {
        return "Find uses of deprecated classes, optionally ignoring those classes that are inside deprecated scopes.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TypeMatcher typeMatcher = typePattern == null ? null : new TypeMatcher(typePattern, Boolean.TRUE.equals(matchInherited));
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                for (JavaType javaType : cu.getTypesInUse().getTypesInUse()) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(javaType);
                    if (fqn != null && (typeMatcher == null || typeMatcher.matches(fqn))) {
                        for (JavaType.FullyQualified annotation : fqn.getAnnotations()) {
                            if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                return SearchResult.found(cu);
                            }
                        }
                    }
                }
                return cu;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public <N extends NameTree> N visitTypeName(N nameTree, ExecutionContext ctx) {
                if (getCursor().firstEnclosing(J.Import.class) == null) {
                    JavaType.FullyQualified fqn = TypeUtils.asFullyQualified(nameTree.getType());
                    if (fqn != null && (typeMatcher == null || typeMatcher.matches(fqn))) {
                        for (JavaType.FullyQualified annotation : fqn.getAnnotations()) {
                            if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                if (Boolean.TRUE.equals(ignoreDeprecatedScopes)) {
                                    Iterator<Cursor> cursorPath = getCursor().getPathAsCursors();
                                    while (cursorPath.hasNext()) {
                                        Cursor ancestor = cursorPath.next();
                                        if (ancestor.getValue() instanceof J.MethodDeclaration && isDeprecated(ancestor)) {
                                            return nameTree;
                                        }
                                        if (ancestor.getValue() instanceof J.ClassDeclaration && isDeprecated(ancestor)) {
                                            return nameTree;
                                        }
                                    }
                                }
                                return SearchResult.found(nameTree);
                            }
                        }
                    }
                }
                return nameTree;
            }
            private boolean isDeprecated(Cursor cursor) {
                return service(AnnotationService.class).matches(cursor, DEPRECATED_MATCHER);
            }
        });
    }

    public FindDeprecatedClasses(@Nullable final String typePattern, @Nullable final Boolean matchInherited, @Nullable final Boolean ignoreDeprecatedScopes) {
        this.typePattern = typePattern;
        this.matchInherited = matchInherited;
        this.ignoreDeprecatedScopes = ignoreDeprecatedScopes;
    }

    @Nullable
    public String getTypePattern() {
        return this.typePattern;
    }

    @Nullable
    public Boolean getMatchInherited() {
        return this.matchInherited;
    }

    @Nullable
    public Boolean getIgnoreDeprecatedScopes() {
        return this.ignoreDeprecatedScopes;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindDeprecatedClasses(typePattern=" + this.getTypePattern() + ", matchInherited=" + this.getMatchInherited() + ", ignoreDeprecatedScopes=" + this.getIgnoreDeprecatedScopes() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindDeprecatedClasses)) return false;
        final FindDeprecatedClasses other = (FindDeprecatedClasses) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchInherited = this.getMatchInherited();
        final Object other$matchInherited = other.getMatchInherited();
        if (this$matchInherited == null ? other$matchInherited != null : !this$matchInherited.equals(other$matchInherited)) return false;
        final Object this$ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        final Object other$ignoreDeprecatedScopes = other.getIgnoreDeprecatedScopes();
        if (this$ignoreDeprecatedScopes == null ? other$ignoreDeprecatedScopes != null : !this$ignoreDeprecatedScopes.equals(other$ignoreDeprecatedScopes)) return false;
        final Object this$typePattern = this.getTypePattern();
        final Object other$typePattern = other.getTypePattern();
        if (this$typePattern == null ? other$typePattern != null : !this$typePattern.equals(other$typePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindDeprecatedClasses;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchInherited = this.getMatchInherited();
        result = result * PRIME + ($matchInherited == null ? 43 : $matchInherited.hashCode());
        final Object $ignoreDeprecatedScopes = this.getIgnoreDeprecatedScopes();
        result = result * PRIME + ($ignoreDeprecatedScopes == null ? 43 : $ignoreDeprecatedScopes.hashCode());
        final Object $typePattern = this.getTypePattern();
        result = result * PRIME + ($typePattern == null ? 43 : $typePattern.hashCode());
        return result;
    }
}
