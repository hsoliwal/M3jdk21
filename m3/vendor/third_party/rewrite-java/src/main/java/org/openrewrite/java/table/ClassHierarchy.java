// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/table/ClassHierarchy.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;

public class ClassHierarchy extends DataTable<ClassHierarchy.Row> {
    public ClassHierarchy(Recipe recipe) {
        super(recipe, "Class hierarchy", "Record the classes");
    }


    public static final class Row {
        @Column(displayName = "Source Path", description = "The path to the source file which declares the subclass.")
        private final String sourcePath;
        @Column(displayName = "Class name", description = "The class or interface which extends or implements another class or interface.")
        private final String className;
        @Column(displayName = "Superclass", description = "The class extended by the subclass.")
        private final String superclass;
        @Nullable
        @Column(displayName = "Interfaces", description = "Comma-separated list of interfaces implemented by the class.")
        private final String interfaces;

        public Row(final String sourcePath, final String className, final String superclass, @Nullable final String interfaces) {
            this.sourcePath = sourcePath;
            this.className = className;
            this.superclass = superclass;
            this.interfaces = interfaces;
        }

        public String getSourcePath() {
            return this.sourcePath;
        }

        public String getClassName() {
            return this.className;
        }

        public String getSuperclass() {
            return this.superclass;
        }

        @Nullable
        public String getInterfaces() {
            return this.interfaces;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ClassHierarchy.Row)) return false;
            final ClassHierarchy.Row other = (ClassHierarchy.Row) o;
            final Object this$sourcePath = this.getSourcePath();
            final Object other$sourcePath = other.getSourcePath();
            if (this$sourcePath == null ? other$sourcePath != null : !this$sourcePath.equals(other$sourcePath)) return false;
            final Object this$className = this.getClassName();
            final Object other$className = other.getClassName();
            if (this$className == null ? other$className != null : !this$className.equals(other$className)) return false;
            final Object this$superclass = this.getSuperclass();
            final Object other$superclass = other.getSuperclass();
            if (this$superclass == null ? other$superclass != null : !this$superclass.equals(other$superclass)) return false;
            final Object this$interfaces = this.getInterfaces();
            final Object other$interfaces = other.getInterfaces();
            if (this$interfaces == null ? other$interfaces != null : !this$interfaces.equals(other$interfaces)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourcePath = this.getSourcePath();
            result = result * PRIME + ($sourcePath == null ? 43 : $sourcePath.hashCode());
            final Object $className = this.getClassName();
            result = result * PRIME + ($className == null ? 43 : $className.hashCode());
            final Object $superclass = this.getSuperclass();
            result = result * PRIME + ($superclass == null ? 43 : $superclass.hashCode());
            final Object $interfaces = this.getInterfaces();
            result = result * PRIME + ($interfaces == null ? 43 : $interfaces.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ClassHierarchy.Row(sourcePath=" + this.getSourcePath() + ", className=" + this.getClassName() + ", superclass=" + this.getSuperclass() + ", interfaces=" + this.getInterfaces() + ")";
        }
    }
}
