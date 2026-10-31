// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/search/FindTypeMappings.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.search;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.table.TypeMappings;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypedTree;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import static java.util.Collections.emptyList;

public final class FindTypeMappings extends ScanningRecipe<Map<FindTypeMappings.TypeAssociation, Integer>> {
    private final transient TypeMappings typeMappingsPerSource = new TypeMappings(this);

    @Override
    public String getDisplayName() {
        return "Find type mappings";
    }

    @Override
    public String getDescription() {
        return "Study the frequency of `J` types and their `JavaType` type attribution.";
    }

    @Override
    public Map<TypeAssociation, Integer> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<TypeAssociation, Integer> acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            @Nullable
            public JavaType visitType(@Nullable JavaType javaType, ExecutionContext ctx) {
                Cursor cursor = getCursor();
                acc.compute(new TypeAssociation(cursor.firstEnclosingOrThrow(JavaSourceFile.class).getClass(), cursor.getValue().getClass(), javaType == null ? null : javaType.getClass(), javaType == null ? cursor.getPathAsStream().filter(t -> t instanceof TypedTree && ((TypedTree) t).getType() != null).findFirst().map(Object::getClass).orElse(null) : null), (k, v) -> v == null ? 1 : v + 1);
                return javaType;
            }
        };
    }

    @Override
    public Collection<? extends SourceFile> generate(Map<TypeAssociation, Integer> acc, ExecutionContext ctx) {
        acc.forEach((assoc, count) -> {
            String j = assoc.getJ().getName();
            Class<?> nearJ = assoc.getNearestNonNullJ();
            typeMappingsPerSource.insertRow(ctx, new TypeMappings.Row(assoc.getCompilationUnit().getEnclosingClass().getSimpleName(), j.substring(j.lastIndexOf('.') + 1), assoc.getJavaType() == null ? "null" : assoc.getJavaType().getSimpleName(), count, nearJ == null ? null : nearJ.getName().substring(nearJ.getName().lastIndexOf('.') + 1)));
        });
        return emptyList();
    }


    public static final class TypeAssociation {
        private final Class<? extends JavaSourceFile> compilationUnit;
        private final Class<?> j;
        @Nullable
        private final Class<? extends JavaType> javaType;
        /**
         * When {@link #j} is null, this is the nearest non-null {@link J} type
         * in the cursor stack.
         */
        @Nullable
        private final Class<?> nearestNonNullJ;

        public TypeAssociation(final Class<? extends JavaSourceFile> compilationUnit, final Class<?> j, @Nullable final Class<? extends JavaType> javaType, @Nullable final Class<?> nearestNonNullJ) {
            this.compilationUnit = compilationUnit;
            this.j = j;
            this.javaType = javaType;
            this.nearestNonNullJ = nearestNonNullJ;
        }

        public Class<? extends JavaSourceFile> getCompilationUnit() {
            return this.compilationUnit;
        }

        public Class<?> getJ() {
            return this.j;
        }

        @Nullable
        public Class<? extends JavaType> getJavaType() {
            return this.javaType;
        }

        @Nullable
        public Class<?> getNearestNonNullJ() {
            return this.nearestNonNullJ;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof FindTypeMappings.TypeAssociation)) return false;
            final FindTypeMappings.TypeAssociation other = (FindTypeMappings.TypeAssociation) o;
            final Object this$compilationUnit = this.getCompilationUnit();
            final Object other$compilationUnit = other.getCompilationUnit();
            if (this$compilationUnit == null ? other$compilationUnit != null : !this$compilationUnit.equals(other$compilationUnit)) return false;
            final Object this$j = this.getJ();
            final Object other$j = other.getJ();
            if (this$j == null ? other$j != null : !this$j.equals(other$j)) return false;
            final Object this$javaType = this.getJavaType();
            final Object other$javaType = other.getJavaType();
            if (this$javaType == null ? other$javaType != null : !this$javaType.equals(other$javaType)) return false;
            final Object this$nearestNonNullJ = this.getNearestNonNullJ();
            final Object other$nearestNonNullJ = other.getNearestNonNullJ();
            if (this$nearestNonNullJ == null ? other$nearestNonNullJ != null : !this$nearestNonNullJ.equals(other$nearestNonNullJ)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $compilationUnit = this.getCompilationUnit();
            result = result * PRIME + ($compilationUnit == null ? 43 : $compilationUnit.hashCode());
            final Object $j = this.getJ();
            result = result * PRIME + ($j == null ? 43 : $j.hashCode());
            final Object $javaType = this.getJavaType();
            result = result * PRIME + ($javaType == null ? 43 : $javaType.hashCode());
            final Object $nearestNonNullJ = this.getNearestNonNullJ();
            result = result * PRIME + ($nearestNonNullJ == null ? 43 : $nearestNonNullJ.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "FindTypeMappings.TypeAssociation(compilationUnit=" + this.getCompilationUnit() + ", j=" + this.getJ() + ", javaType=" + this.getJavaType() + ", nearestNonNullJ=" + this.getNearestNonNullJ() + ")";
        }
    }

    public FindTypeMappings() {
    }

    public TypeMappings getTypeMappingsPerSource() {
        return this.typeMappingsPerSource;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FindTypeMappings(typeMappingsPerSource=" + this.getTypeMappingsPerSource() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FindTypeMappings)) return false;
        final FindTypeMappings other = (FindTypeMappings) o;
        if (!other.canEqual((Object) this)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof FindTypeMappings;
    }

    @Override
    public int hashCode() {
        final int result = 1;
        return result;
    }
}
