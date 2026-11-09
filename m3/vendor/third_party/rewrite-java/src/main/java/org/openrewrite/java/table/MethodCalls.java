// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/table/MethodCalls.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.table;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

@JsonIgnoreType
public class MethodCalls extends DataTable<MethodCalls.Row> {
    public MethodCalls(Recipe recipe) {
        super(recipe, "Method calls", "The text of matching method invocations.");
    }


    public static final class Row {
        @Column(displayName = "Source file", description = "The source file that the method call occurred in.")
        private final String sourceFile;
        @Column(displayName = "Method call", description = "The text of the method call.")
        private final String method;
        @Column(displayName = "Class name", description = "The class name of the method call.")
        private final String className;
        @Column(displayName = "Method name", description = "The method name of the method call.")
        private final String methodName;
        @Column(displayName = "Argument types", description = "The argument types of the method call.")
        private final String argumentTypes;

        public Row(final String sourceFile, final String method, final String className, final String methodName, final String argumentTypes) {
            this.sourceFile = sourceFile;
            this.method = method;
            this.className = className;
            this.methodName = methodName;
            this.argumentTypes = argumentTypes;
        }

        public String getSourceFile() {
            return this.sourceFile;
        }

        public String getMethod() {
            return this.method;
        }

        public String getClassName() {
            return this.className;
        }

        public String getMethodName() {
            return this.methodName;
        }

        public String getArgumentTypes() {
            return this.argumentTypes;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof MethodCalls.Row)) return false;
            final MethodCalls.Row other = (MethodCalls.Row) o;
            final Object this$sourceFile = this.getSourceFile();
            final Object other$sourceFile = other.getSourceFile();
            if (this$sourceFile == null ? other$sourceFile != null : !this$sourceFile.equals(other$sourceFile)) return false;
            final Object this$method = this.getMethod();
            final Object other$method = other.getMethod();
            if (this$method == null ? other$method != null : !this$method.equals(other$method)) return false;
            final Object this$className = this.getClassName();
            final Object other$className = other.getClassName();
            if (this$className == null ? other$className != null : !this$className.equals(other$className)) return false;
            final Object this$methodName = this.getMethodName();
            final Object other$methodName = other.getMethodName();
            if (this$methodName == null ? other$methodName != null : !this$methodName.equals(other$methodName)) return false;
            final Object this$argumentTypes = this.getArgumentTypes();
            final Object other$argumentTypes = other.getArgumentTypes();
            if (this$argumentTypes == null ? other$argumentTypes != null : !this$argumentTypes.equals(other$argumentTypes)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $sourceFile = this.getSourceFile();
            result = result * PRIME + ($sourceFile == null ? 43 : $sourceFile.hashCode());
            final Object $method = this.getMethod();
            result = result * PRIME + ($method == null ? 43 : $method.hashCode());
            final Object $className = this.getClassName();
            result = result * PRIME + ($className == null ? 43 : $className.hashCode());
            final Object $methodName = this.getMethodName();
            result = result * PRIME + ($methodName == null ? 43 : $methodName.hashCode());
            final Object $argumentTypes = this.getArgumentTypes();
            result = result * PRIME + ($argumentTypes == null ? 43 : $argumentTypes.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "MethodCalls.Row(sourceFile=" + this.getSourceFile() + ", method=" + this.getMethod() + ", className=" + this.getClassName() + ", methodName=" + this.getMethodName() + ", argumentTypes=" + this.getArgumentTypes() + ")";
        }
    }
}
