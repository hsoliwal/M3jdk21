// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindAnnotations.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;
import java.util.HashSet;
import java.util.Set;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

public final class FindAnnotations extends Recipe {
    /**
     * An annotation pattern, expressed as a method pattern.
     * See {@link AnnotationMatcher} for syntax.
     */
    @Option(displayName = "Annotation pattern", description = "An annotation pattern, expressed as a method pattern.", example = "@java.lang.SuppressWarnings(\"deprecation\")")
    private final String annotationPattern;
    @Option(displayName = "Match on meta annotations", description = "When enabled, matches on meta annotations of the annotation pattern.", required = false)
    @Nullable
    private final Boolean matchMetaAnnotations;

    @Override
    public String getDisplayName() {
        return "Find annotations";
    }

    @Override
    public String getDescription() {
        return "Find all annotations matching the annotation pattern.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationPattern, matchMetaAnnotations);
        return Preconditions.check(new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) requireNonNull(tree);
                    for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
                        if (annotationMatcher.matchesAnnotationOrMetaAnnotation(TypeUtils.asFullyQualified(type))) {
                            return SearchResult.found(cu);
                        }
                    }
                }
                return super.visit(tree, ctx);
            }
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (annotationMatcher.matches(annotation)) {
                    a = SearchResult.found(a);
                }
                return a;
            }
        }, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (annotationMatcher.matches(annotation)) {
                    a = SearchResult.found(a);
                }
                return a;
            }
        });
    }

    public static Set<J.Annotation> find(J j, String annotationPattern) {
        return find(j, annotationPattern, false);
    }

    public static Set<J.Annotation> find(J j, String annotationPattern, boolean matchMetaAnnotations) {
        return TreeVisitor.collect(new FindAnnotations(annotationPattern, matchMetaAnnotations).getVisitor(), j, new HashSet<>()).stream().filter(a -> a instanceof J.Annotation).map(a -> (J.Annotation) a).collect(toSet());
    }

    public FindAnnotations(final String annotationPattern, @Nullable final Boolean matchMetaAnnotations) {
        this.annotationPattern = annotationPattern;
        this.matchMetaAnnotations = matchMetaAnnotations;
    }

    /**
     * An annotation pattern, expressed as a method pattern.
     * See {@link AnnotationMatcher} for syntax.
     */
    public String getAnnotationPattern() {
        return this.annotationPattern;
    }

    @Nullable
    public Boolean getMatchMetaAnnotations() {
        return this.matchMetaAnnotations;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindAnnotations(annotationPattern=" + this.getAnnotationPattern() + ", matchMetaAnnotations=" + this.getMatchMetaAnnotations() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindAnnotations)) return false;
        final FindAnnotations other = (FindAnnotations) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchMetaAnnotations = this.getMatchMetaAnnotations();
        final Object other$matchMetaAnnotations = other.getMatchMetaAnnotations();
        if (this$matchMetaAnnotations == null ? other$matchMetaAnnotations != null : !this$matchMetaAnnotations.equals(other$matchMetaAnnotations)) return false;
        final Object this$annotationPattern = this.getAnnotationPattern();
        final Object other$annotationPattern = other.getAnnotationPattern();
        if (this$annotationPattern == null ? other$annotationPattern != null : !this$annotationPattern.equals(other$annotationPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindAnnotations;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchMetaAnnotations = this.getMatchMetaAnnotations();
        result = result * PRIME + ($matchMetaAnnotations == null ? 43 : $matchMetaAnnotations.hashCode());
        final Object $annotationPattern = this.getAnnotationPattern();
        result = result * PRIME + ($annotationPattern == null ? 43 : $annotationPattern.hashCode());
        return result;
    }
}
