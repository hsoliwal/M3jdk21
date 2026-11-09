// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindImports.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

public final class FindImports extends Recipe {
    @Option(displayName = "Type pattern", description = "A type pattern that is used to find matching field uses.", example = "org.springframework..*", required = false)
    @Nullable
    private final String typePattern;
    @Option(displayName = "Match inherited", description = "When enabled, find types that inherit from a deprecated type.", required = false)
    @Nullable
    private final Boolean matchInherited;

    @Override
    public String getDisplayName() {
        return "Find source files with imports";
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
        return "Locates source files that have imports matching the given type pattern, regardless of whether " + "that import is used in the code.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TypeMatcher typeMatcher = new TypeMatcher(typePattern, Boolean.TRUE.equals(matchInherited));
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Import visitImport(J.Import anImport, ExecutionContext ctx) {
                if (typeMatcher.matchesPackage(anImport.getTypeName())) {
                    return SearchResult.found(anImport);
                }
                return super.visitImport(anImport, ctx);
            }
        };
    }

    public FindImports(@Nullable final String typePattern, @Nullable final Boolean matchInherited) {
        this.typePattern = typePattern;
        this.matchInherited = matchInherited;
    }

    @Nullable
    public String getTypePattern() {
        return this.typePattern;
    }

    @Nullable
    public Boolean getMatchInherited() {
        return this.matchInherited;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindImports(typePattern=" + this.getTypePattern() + ", matchInherited=" + this.getMatchInherited() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindImports)) return false;
        final FindImports other = (FindImports) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchInherited = this.getMatchInherited();
        final Object other$matchInherited = other.getMatchInherited();
        if (this$matchInherited == null ? other$matchInherited != null : !this$matchInherited.equals(other$matchInherited)) return false;
        final Object this$typePattern = this.getTypePattern();
        final Object other$typePattern = other.getTypePattern();
        if (this$typePattern == null ? other$typePattern != null : !this$typePattern.equals(other$typePattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindImports;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchInherited = this.getMatchInherited();
        result = result * PRIME + ($matchInherited == null ? 43 : $matchInherited.hashCode());
        final Object $typePattern = this.getTypePattern();
        result = result * PRIME + ($typePattern == null ? 43 : $typePattern.hashCode());
        return result;
    }
}
