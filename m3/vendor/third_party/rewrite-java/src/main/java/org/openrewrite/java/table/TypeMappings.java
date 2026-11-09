// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/table/TypeMappings.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.java.tree.J;

@JsonIgnoreType
public class TypeMappings extends DataTable<TypeMappings.Row> {
    public TypeMappings(Recipe recipe) {
        super(recipe, "Type mapping", "The types mapped to `J` trees.");
    }


    public static final class Row {
        @Column(displayName = "Compilation unit class name", description = "The root compilation unit class name containing the mapping.")
        private final String compilationUnitName;
        @Column(displayName = "Tree class name", description = "The simple class name of the `J` element.")
        private final String treeName;
        @Column(displayName = "Java type class name", description = "The simple class name of the `JavaType`.")
        private final String typeName;
        @Column(displayName = "Count", description = "The number of times this tree and type pair occurred in a repository.")
        private final Integer count;
        /**
         * When {@link #typeName} is null, this is the nearest non-null {@link J} type
         * in the cursor stack.
         */
        @Column(displayName = "Nearest non-null tree class name", description = "The simple class name of the nearest non-null `J` element when " + "`typeName` is null.")
        private final String nearestNonNullTreeName;

        public Row(final String compilationUnitName, final String treeName, final String typeName, final Integer count, final String nearestNonNullTreeName) {
            this.compilationUnitName = compilationUnitName;
            this.treeName = treeName;
            this.typeName = typeName;
            this.count = count;
            this.nearestNonNullTreeName = nearestNonNullTreeName;
        }

        public String getCompilationUnitName() {
            return this.compilationUnitName;
        }

        public String getTreeName() {
            return this.treeName;
        }

        public String getTypeName() {
            return this.typeName;
        }

        public Integer getCount() {
            return this.count;
        }

        public String getNearestNonNullTreeName() {
            return this.nearestNonNullTreeName;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof TypeMappings.Row)) return false;
            final TypeMappings.Row other = (TypeMappings.Row) o;
            final Object this$count = this.getCount();
            final Object other$count = other.getCount();
            if (this$count == null ? other$count != null : !this$count.equals(other$count)) return false;
            final Object this$compilationUnitName = this.getCompilationUnitName();
            final Object other$compilationUnitName = other.getCompilationUnitName();
            if (this$compilationUnitName == null ? other$compilationUnitName != null : !this$compilationUnitName.equals(other$compilationUnitName)) return false;
            final Object this$treeName = this.getTreeName();
            final Object other$treeName = other.getTreeName();
            if (this$treeName == null ? other$treeName != null : !this$treeName.equals(other$treeName)) return false;
            final Object this$typeName = this.getTypeName();
            final Object other$typeName = other.getTypeName();
            if (this$typeName == null ? other$typeName != null : !this$typeName.equals(other$typeName)) return false;
            final Object this$nearestNonNullTreeName = this.getNearestNonNullTreeName();
            final Object other$nearestNonNullTreeName = other.getNearestNonNullTreeName();
            if (this$nearestNonNullTreeName == null ? other$nearestNonNullTreeName != null : !this$nearestNonNullTreeName.equals(other$nearestNonNullTreeName)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $count = this.getCount();
            result = result * PRIME + ($count == null ? 43 : $count.hashCode());
            final Object $compilationUnitName = this.getCompilationUnitName();
            result = result * PRIME + ($compilationUnitName == null ? 43 : $compilationUnitName.hashCode());
            final Object $treeName = this.getTreeName();
            result = result * PRIME + ($treeName == null ? 43 : $treeName.hashCode());
            final Object $typeName = this.getTypeName();
            result = result * PRIME + ($typeName == null ? 43 : $typeName.hashCode());
            final Object $nearestNonNullTreeName = this.getNearestNonNullTreeName();
            result = result * PRIME + ($nearestNonNullTreeName == null ? 43 : $nearestNonNullTreeName.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "TypeMappings.Row(compilationUnitName=" + this.getCompilationUnitName() + ", treeName=" + this.getTreeName() + ", typeName=" + this.getTypeName() + ", count=" + this.getCount() + ", nearestNonNullTreeName=" + this.getNearestNonNullTreeName() + ")";
        }
    }
}
