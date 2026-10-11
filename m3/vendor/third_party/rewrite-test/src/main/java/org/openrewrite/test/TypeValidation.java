// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/test/TypeValidation.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.test;

public class TypeValidation {
    private boolean classDeclarations;
    private boolean identifiers;
    private boolean methodDeclarations;
    private boolean variableDeclarations;
    private boolean methodInvocations;
    private boolean constructorInvocations;

    public static TypeValidation all() {
        return new TypeValidation();
    }

    public static TypeValidation none() {
        return new TypeValidation(false, false, false, false, false, false);
    }

    static TypeValidation before(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        TypeValidation typeValidation = testMethodSpec.getTypeValidation() != null ? testMethodSpec.getTypeValidation() : testClassSpec.getTypeValidation();
        return typeValidation != null ? typeValidation : new TypeValidation();
    }

    static TypeValidation after(RecipeSpec testMethodSpec, RecipeSpec testClassSpec) {
        TypeValidation typeValidation = testMethodSpec.getAfterTypeValidation() != null ? testMethodSpec.getAfterTypeValidation() : testClassSpec.getAfterTypeValidation();
        // after type validation defaults to before type validation; possibly this will become stricter in the future
        return typeValidation != null ? typeValidation : before(testMethodSpec, testClassSpec);
    }

    private static boolean $default$classDeclarations() {
        return true;
    }

    private static boolean $default$identifiers() {
        return true;
    }

    private static boolean $default$methodDeclarations() {
        return true;
    }

    private static boolean $default$variableDeclarations() {
        return true;
    }

    private static boolean $default$methodInvocations() {
        return true;
    }

    private static boolean $default$constructorInvocations() {
        return true;
    }


    public static class TypeValidationBuilder {
        private boolean classDeclarations$set;
        private boolean classDeclarations$value;
        private boolean identifiers$set;
        private boolean identifiers$value;
        private boolean methodDeclarations$set;
        private boolean methodDeclarations$value;
        private boolean variableDeclarations$set;
        private boolean variableDeclarations$value;
        private boolean methodInvocations$set;
        private boolean methodInvocations$value;
        private boolean constructorInvocations$set;
        private boolean constructorInvocations$value;

        TypeValidationBuilder() {
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder classDeclarations(final boolean classDeclarations) {
            this.classDeclarations$value = classDeclarations;
            classDeclarations$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder identifiers(final boolean identifiers) {
            this.identifiers$value = identifiers;
            identifiers$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder methodDeclarations(final boolean methodDeclarations) {
            this.methodDeclarations$value = methodDeclarations;
            methodDeclarations$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder variableDeclarations(final boolean variableDeclarations) {
            this.variableDeclarations$value = variableDeclarations;
            variableDeclarations$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder methodInvocations(final boolean methodInvocations) {
            this.methodInvocations$value = methodInvocations;
            methodInvocations$set = true;
            return this;
        }

        /**
         * @return {@code this}.
         */
        @org.openrewrite.internal.lang.NonNull
        public TypeValidation.TypeValidationBuilder constructorInvocations(final boolean constructorInvocations) {
            this.constructorInvocations$value = constructorInvocations;
            constructorInvocations$set = true;
            return this;
        }

        @org.openrewrite.internal.lang.NonNull
        public TypeValidation build() {
            boolean classDeclarations$value = this.classDeclarations$value;
            if (!this.classDeclarations$set) classDeclarations$value = TypeValidation.$default$classDeclarations();
            boolean identifiers$value = this.identifiers$value;
            if (!this.identifiers$set) identifiers$value = TypeValidation.$default$identifiers();
            boolean methodDeclarations$value = this.methodDeclarations$value;
            if (!this.methodDeclarations$set) methodDeclarations$value = TypeValidation.$default$methodDeclarations();
            boolean variableDeclarations$value = this.variableDeclarations$value;
            if (!this.variableDeclarations$set) variableDeclarations$value = TypeValidation.$default$variableDeclarations();
            boolean methodInvocations$value = this.methodInvocations$value;
            if (!this.methodInvocations$set) methodInvocations$value = TypeValidation.$default$methodInvocations();
            boolean constructorInvocations$value = this.constructorInvocations$value;
            if (!this.constructorInvocations$set) constructorInvocations$value = TypeValidation.$default$constructorInvocations();
            return new TypeValidation(classDeclarations$value, identifiers$value, methodDeclarations$value, variableDeclarations$value, methodInvocations$value, constructorInvocations$value);
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "TypeValidation.TypeValidationBuilder(classDeclarations$value=" + this.classDeclarations$value + ", identifiers$value=" + this.identifiers$value + ", methodDeclarations$value=" + this.methodDeclarations$value + ", variableDeclarations$value=" + this.variableDeclarations$value + ", methodInvocations$value=" + this.methodInvocations$value + ", constructorInvocations$value=" + this.constructorInvocations$value + ")";
        }
    }

    @org.openrewrite.internal.lang.NonNull
    public static TypeValidation.TypeValidationBuilder builder() {
        return new TypeValidation.TypeValidationBuilder();
    }

    public boolean classDeclarations() {
        return this.classDeclarations;
    }

    public boolean identifiers() {
        return this.identifiers;
    }

    public boolean methodDeclarations() {
        return this.methodDeclarations;
    }

    public boolean variableDeclarations() {
        return this.variableDeclarations;
    }

    public boolean methodInvocations() {
        return this.methodInvocations;
    }

    public boolean constructorInvocations() {
        return this.constructorInvocations;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation classDeclarations(final boolean classDeclarations) {
        this.classDeclarations = classDeclarations;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation identifiers(final boolean identifiers) {
        this.identifiers = identifiers;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation methodDeclarations(final boolean methodDeclarations) {
        this.methodDeclarations = methodDeclarations;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation variableDeclarations(final boolean variableDeclarations) {
        this.variableDeclarations = variableDeclarations;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation methodInvocations(final boolean methodInvocations) {
        this.methodInvocations = methodInvocations;
        return this;
    }

    /**
     * @return {@code this}.
     */
    @org.openrewrite.internal.lang.NonNull
    public TypeValidation constructorInvocations(final boolean constructorInvocations) {
        this.constructorInvocations = constructorInvocations;
        return this;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TypeValidation)) return false;
        final TypeValidation other = (TypeValidation) o;
        if (!other.canEqual((Object) this)) return false;
        if (this.classDeclarations() != other.classDeclarations()) return false;
        if (this.identifiers() != other.identifiers()) return false;
        if (this.methodDeclarations() != other.methodDeclarations()) return false;
        if (this.variableDeclarations() != other.variableDeclarations()) return false;
        if (this.methodInvocations() != other.methodInvocations()) return false;
        if (this.constructorInvocations() != other.constructorInvocations()) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof TypeValidation;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + (this.classDeclarations() ? 79 : 97);
        result = result * PRIME + (this.identifiers() ? 79 : 97);
        result = result * PRIME + (this.methodDeclarations() ? 79 : 97);
        result = result * PRIME + (this.variableDeclarations() ? 79 : 97);
        result = result * PRIME + (this.methodInvocations() ? 79 : 97);
        result = result * PRIME + (this.constructorInvocations() ? 79 : 97);
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TypeValidation(classDeclarations=" + this.classDeclarations() + ", identifiers=" + this.identifiers() + ", methodDeclarations=" + this.methodDeclarations() + ", variableDeclarations=" + this.variableDeclarations() + ", methodInvocations=" + this.methodInvocations() + ", constructorInvocations=" + this.constructorInvocations() + ")";
    }

    public TypeValidation() {
        this.classDeclarations = TypeValidation.$default$classDeclarations();
        this.identifiers = TypeValidation.$default$identifiers();
        this.methodDeclarations = TypeValidation.$default$methodDeclarations();
        this.variableDeclarations = TypeValidation.$default$variableDeclarations();
        this.methodInvocations = TypeValidation.$default$methodInvocations();
        this.constructorInvocations = TypeValidation.$default$constructorInvocations();
    }

    public TypeValidation(final boolean classDeclarations, final boolean identifiers, final boolean methodDeclarations, final boolean variableDeclarations, final boolean methodInvocations, final boolean constructorInvocations) {
        this.classDeclarations = classDeclarations;
        this.identifiers = identifiers;
        this.methodDeclarations = methodDeclarations;
        this.variableDeclarations = variableDeclarations;
        this.methodInvocations = methodInvocations;
        this.constructorInvocations = constructorInvocations;
    }
}
