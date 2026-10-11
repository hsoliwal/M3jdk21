// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindFields.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;
import java.util.HashSet;
import java.util.Set;

public final class FindFields extends Recipe {
    @Option(displayName = "Fully-qualified type name", description = "A fully-qualified Java type name, that is used to find matching fields.", example = "com.fasterxml.jackson.core.json.JsonWriteFeature")
    private final String fullyQualifiedTypeName;
    @Option(displayName = "Match inherited", description = "When enabled, find types that inherit from a deprecated type.", required = false)
    @Nullable
    private final Boolean matchInherited;
    @Option(displayName = "Field name", description = "The name of a field on the type.", example = "QUOTE_FIELD_NAMES")
    private final String fieldName;

    @Override
    public String getDisplayName() {
        return "Find fields";
    }

    @Override
    public String getInstanceNameSuffix() {
        return "on types `" + fullyQualifiedTypeName + "`";
    }

    @Override
    public String getDescription() {
        return "Find uses of a field.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesField<>(fullyQualifiedTypeName, fieldName), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext ctx) {
                JavaType.Variable varType = fieldAccess.getName().getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    return SearchResult.found(fieldAccess);
                }
                return super.visitFieldAccess(fieldAccess, ctx);
            }
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier i = super.visitIdentifier(identifier, ctx);
                JavaType.Variable varType = identifier.getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    i = SearchResult.found(i);
                }
                return i;
            }
            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, ExecutionContext ctx) {
                J.MemberReference m = super.visitMemberReference(memberRef, ctx);
                JavaType.Variable varType = memberRef.getVariableType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, Boolean.TRUE.equals(matchInherited)).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    m = m.withReference(SearchResult.found(m.getReference()));
                }
                return m;
            }
        });
    }

    public static Set<J> find(J j, String fullyQualifiedTypeName, String fieldName) {
        JavaVisitor<Set<J>> findVisitor = new JavaIsoVisitor<Set<J>>() {
            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, Set<J> vs) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, vs);
                JavaType.Variable varType = fieldAccess.getName().getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(f);
                }
                return f;
            }
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<J> vs) {
                J.Identifier i = super.visitIdentifier(identifier, vs);
                JavaType.Variable varType = identifier.getFieldType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(i);
                }
                return i;
            }
            @Override
            public J.MemberReference visitMemberReference(J.MemberReference memberRef, Set<J> vs) {
                J.MemberReference m = super.visitMemberReference(memberRef, vs);
                JavaType.Variable varType = memberRef.getVariableType();
                if (varType != null && new TypeMatcher(fullyQualifiedTypeName, true).matches(varType.getOwner()) && StringUtils.matchesGlob(varType.getName(), fieldName)) {
                    vs.add(m);
                }
                return m;
            }
        };
        Set<J> vs = new HashSet<>();
        findVisitor.visit(j, vs);
        return vs;
    }

    public FindFields(final String fullyQualifiedTypeName, @Nullable final Boolean matchInherited, final String fieldName) {
        this.fullyQualifiedTypeName = fullyQualifiedTypeName;
        this.matchInherited = matchInherited;
        this.fieldName = fieldName;
    }

    public String getFullyQualifiedTypeName() {
        return this.fullyQualifiedTypeName;
    }

    @Nullable
    public Boolean getMatchInherited() {
        return this.matchInherited;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindFields(fullyQualifiedTypeName=" + this.getFullyQualifiedTypeName() + ", matchInherited=" + this.getMatchInherited() + ", fieldName=" + this.getFieldName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindFields)) return false;
        final FindFields other = (FindFields) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$matchInherited = this.getMatchInherited();
        final Object other$matchInherited = other.getMatchInherited();
        if (this$matchInherited == null ? other$matchInherited != null : !this$matchInherited.equals(other$matchInherited)) return false;
        final Object this$fullyQualifiedTypeName = this.getFullyQualifiedTypeName();
        final Object other$fullyQualifiedTypeName = other.getFullyQualifiedTypeName();
        if (this$fullyQualifiedTypeName == null ? other$fullyQualifiedTypeName != null : !this$fullyQualifiedTypeName.equals(other$fullyQualifiedTypeName)) return false;
        final Object this$fieldName = this.getFieldName();
        final Object other$fieldName = other.getFieldName();
        if (this$fieldName == null ? other$fieldName != null : !this$fieldName.equals(other$fieldName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindFields;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $matchInherited = this.getMatchInherited();
        result = result * PRIME + ($matchInherited == null ? 43 : $matchInherited.hashCode());
        final Object $fullyQualifiedTypeName = this.getFullyQualifiedTypeName();
        result = result * PRIME + ($fullyQualifiedTypeName == null ? 43 : $fullyQualifiedTypeName.hashCode());
        final Object $fieldName = this.getFieldName();
        result = result * PRIME + ($fieldName == null ? 43 : $fieldName.hashCode());
        return result;
    }
}
