// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/table/ClasspathReport.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class ClasspathReport extends DataTable<ClasspathReport.Row> {
    public ClasspathReport(Recipe recipe) {
        super(recipe, "Classpath report", "Contains a report of the runtime classpath and any other jars found inside each classpath entry.");
    }


    public static final class Row {
        @Column(displayName = "Classpath entry URI", description = "The URI where a particular classpath entry can be found. " + "May point to a jar or a directory containing class files. " + "Local to the system running the recipe.")
        private final String uri;
        @Column(displayName = "Classpath entry resource", description = "Path within a classpath entry to a jar file it contains. Resources not ending in \"jar\" are ignored.")
        private final String resource;

        public Row(final String uri, final String resource) {
            this.uri = uri;
            this.resource = resource;
        }

        public String getUri() {
            return this.uri;
        }

        public String getResource() {
            return this.resource;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof ClasspathReport.Row)) return false;
            final ClasspathReport.Row other = (ClasspathReport.Row) o;
            final Object this$uri = this.getUri();
            final Object other$uri = other.getUri();
            if (this$uri == null ? other$uri != null : !this$uri.equals(other$uri)) return false;
            final Object this$resource = this.getResource();
            final Object other$resource = other.getResource();
            if (this$resource == null ? other$resource != null : !this$resource.equals(other$resource)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $uri = this.getUri();
            result = result * PRIME + ($uri == null ? 43 : $uri.hashCode());
            final Object $resource = this.getResource();
            result = result * PRIME + ($resource == null ? 43 : $resource.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "ClasspathReport.Row(uri=" + this.getUri() + ", resource=" + this.getResource() + ")";
        }
    }
}
