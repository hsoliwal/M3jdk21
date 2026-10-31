// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/SpacesStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.style;

import org.openrewrite.java.JavaStyle;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

public final class SpacesStyle implements JavaStyle {
    private final BeforeParentheses beforeParentheses;
    private final AroundOperators aroundOperators;
    private final BeforeLeftBrace beforeLeftBrace;
    private final BeforeKeywords beforeKeywords;
    private final Within within;
    private final TernaryOperator ternaryOperator;
    private final TypeArguments typeArguments;
    private final Other other;
    private final TypeParameters typeParameters;


    public static final class BeforeParentheses {
        private final Boolean methodDeclaration;
        private final Boolean methodCall;
        private final Boolean ifParentheses;
        private final Boolean forParentheses;
        private final Boolean whileParentheses;
        private final Boolean switchParentheses;
        private final Boolean tryParentheses;
        private final Boolean catchParentheses;
        private final Boolean synchronizedParentheses;
        private final Boolean annotationParameters;

        public BeforeParentheses(final Boolean methodDeclaration, final Boolean methodCall, final Boolean ifParentheses, final Boolean forParentheses, final Boolean whileParentheses, final Boolean switchParentheses, final Boolean tryParentheses, final Boolean catchParentheses, final Boolean synchronizedParentheses, final Boolean annotationParameters) {
            this.methodDeclaration = methodDeclaration;
            this.methodCall = methodCall;
            this.ifParentheses = ifParentheses;
            this.forParentheses = forParentheses;
            this.whileParentheses = whileParentheses;
            this.switchParentheses = switchParentheses;
            this.tryParentheses = tryParentheses;
            this.catchParentheses = catchParentheses;
            this.synchronizedParentheses = synchronizedParentheses;
            this.annotationParameters = annotationParameters;
        }

        public Boolean getMethodDeclaration() {
            return this.methodDeclaration;
        }

        public Boolean getMethodCall() {
            return this.methodCall;
        }

        public Boolean getIfParentheses() {
            return this.ifParentheses;
        }

        public Boolean getForParentheses() {
            return this.forParentheses;
        }

        public Boolean getWhileParentheses() {
            return this.whileParentheses;
        }

        public Boolean getSwitchParentheses() {
            return this.switchParentheses;
        }

        public Boolean getTryParentheses() {
            return this.tryParentheses;
        }

        public Boolean getCatchParentheses() {
            return this.catchParentheses;
        }

        public Boolean getSynchronizedParentheses() {
            return this.synchronizedParentheses;
        }

        public Boolean getAnnotationParameters() {
            return this.annotationParameters;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.BeforeParentheses)) return false;
            final SpacesStyle.BeforeParentheses other = (SpacesStyle.BeforeParentheses) o;
            final Object this$methodDeclaration = this.getMethodDeclaration();
            final Object other$methodDeclaration = other.getMethodDeclaration();
            if (this$methodDeclaration == null ? other$methodDeclaration != null : !this$methodDeclaration.equals(other$methodDeclaration)) return false;
            final Object this$methodCall = this.getMethodCall();
            final Object other$methodCall = other.getMethodCall();
            if (this$methodCall == null ? other$methodCall != null : !this$methodCall.equals(other$methodCall)) return false;
            final Object this$ifParentheses = this.getIfParentheses();
            final Object other$ifParentheses = other.getIfParentheses();
            if (this$ifParentheses == null ? other$ifParentheses != null : !this$ifParentheses.equals(other$ifParentheses)) return false;
            final Object this$forParentheses = this.getForParentheses();
            final Object other$forParentheses = other.getForParentheses();
            if (this$forParentheses == null ? other$forParentheses != null : !this$forParentheses.equals(other$forParentheses)) return false;
            final Object this$whileParentheses = this.getWhileParentheses();
            final Object other$whileParentheses = other.getWhileParentheses();
            if (this$whileParentheses == null ? other$whileParentheses != null : !this$whileParentheses.equals(other$whileParentheses)) return false;
            final Object this$switchParentheses = this.getSwitchParentheses();
            final Object other$switchParentheses = other.getSwitchParentheses();
            if (this$switchParentheses == null ? other$switchParentheses != null : !this$switchParentheses.equals(other$switchParentheses)) return false;
            final Object this$tryParentheses = this.getTryParentheses();
            final Object other$tryParentheses = other.getTryParentheses();
            if (this$tryParentheses == null ? other$tryParentheses != null : !this$tryParentheses.equals(other$tryParentheses)) return false;
            final Object this$catchParentheses = this.getCatchParentheses();
            final Object other$catchParentheses = other.getCatchParentheses();
            if (this$catchParentheses == null ? other$catchParentheses != null : !this$catchParentheses.equals(other$catchParentheses)) return false;
            final Object this$synchronizedParentheses = this.getSynchronizedParentheses();
            final Object other$synchronizedParentheses = other.getSynchronizedParentheses();
            if (this$synchronizedParentheses == null ? other$synchronizedParentheses != null : !this$synchronizedParentheses.equals(other$synchronizedParentheses)) return false;
            final Object this$annotationParameters = this.getAnnotationParameters();
            final Object other$annotationParameters = other.getAnnotationParameters();
            if (this$annotationParameters == null ? other$annotationParameters != null : !this$annotationParameters.equals(other$annotationParameters)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $methodDeclaration = this.getMethodDeclaration();
            result = result * PRIME + ($methodDeclaration == null ? 43 : $methodDeclaration.hashCode());
            final Object $methodCall = this.getMethodCall();
            result = result * PRIME + ($methodCall == null ? 43 : $methodCall.hashCode());
            final Object $ifParentheses = this.getIfParentheses();
            result = result * PRIME + ($ifParentheses == null ? 43 : $ifParentheses.hashCode());
            final Object $forParentheses = this.getForParentheses();
            result = result * PRIME + ($forParentheses == null ? 43 : $forParentheses.hashCode());
            final Object $whileParentheses = this.getWhileParentheses();
            result = result * PRIME + ($whileParentheses == null ? 43 : $whileParentheses.hashCode());
            final Object $switchParentheses = this.getSwitchParentheses();
            result = result * PRIME + ($switchParentheses == null ? 43 : $switchParentheses.hashCode());
            final Object $tryParentheses = this.getTryParentheses();
            result = result * PRIME + ($tryParentheses == null ? 43 : $tryParentheses.hashCode());
            final Object $catchParentheses = this.getCatchParentheses();
            result = result * PRIME + ($catchParentheses == null ? 43 : $catchParentheses.hashCode());
            final Object $synchronizedParentheses = this.getSynchronizedParentheses();
            result = result * PRIME + ($synchronizedParentheses == null ? 43 : $synchronizedParentheses.hashCode());
            final Object $annotationParameters = this.getAnnotationParameters();
            result = result * PRIME + ($annotationParameters == null ? 43 : $annotationParameters.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.BeforeParentheses(methodDeclaration=" + this.getMethodDeclaration() + ", methodCall=" + this.getMethodCall() + ", ifParentheses=" + this.getIfParentheses() + ", forParentheses=" + this.getForParentheses() + ", whileParentheses=" + this.getWhileParentheses() + ", switchParentheses=" + this.getSwitchParentheses() + ", tryParentheses=" + this.getTryParentheses() + ", catchParentheses=" + this.getCatchParentheses() + ", synchronizedParentheses=" + this.getSynchronizedParentheses() + ", annotationParameters=" + this.getAnnotationParameters() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withMethodDeclaration(final Boolean methodDeclaration) {
            return this.methodDeclaration == methodDeclaration ? this : new SpacesStyle.BeforeParentheses(methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withMethodCall(final Boolean methodCall) {
            return this.methodCall == methodCall ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withIfParentheses(final Boolean ifParentheses) {
            return this.ifParentheses == ifParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withForParentheses(final Boolean forParentheses) {
            return this.forParentheses == forParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withWhileParentheses(final Boolean whileParentheses) {
            return this.whileParentheses == whileParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withSwitchParentheses(final Boolean switchParentheses) {
            return this.switchParentheses == switchParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withTryParentheses(final Boolean tryParentheses) {
            return this.tryParentheses == tryParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withCatchParentheses(final Boolean catchParentheses) {
            return this.catchParentheses == catchParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, catchParentheses, this.synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withSynchronizedParentheses(final Boolean synchronizedParentheses) {
            return this.synchronizedParentheses == synchronizedParentheses ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, synchronizedParentheses, this.annotationParameters);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeParentheses withAnnotationParameters(final Boolean annotationParameters) {
            return this.annotationParameters == annotationParameters ? this : new SpacesStyle.BeforeParentheses(this.methodDeclaration, this.methodCall, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, annotationParameters);
        }
    }


    public static final class AroundOperators {
        private final Boolean assignment;
        private final Boolean logical;
        private final Boolean equality;
        private final Boolean relational;
        private final Boolean bitwise;
        private final Boolean additive;
        private final Boolean multiplicative;
        private final Boolean shift;
        private final Boolean unary;
        private final Boolean lambdaArrow;
        private final Boolean methodReferenceDoubleColon;

        public AroundOperators(final Boolean assignment, final Boolean logical, final Boolean equality, final Boolean relational, final Boolean bitwise, final Boolean additive, final Boolean multiplicative, final Boolean shift, final Boolean unary, final Boolean lambdaArrow, final Boolean methodReferenceDoubleColon) {
            this.assignment = assignment;
            this.logical = logical;
            this.equality = equality;
            this.relational = relational;
            this.bitwise = bitwise;
            this.additive = additive;
            this.multiplicative = multiplicative;
            this.shift = shift;
            this.unary = unary;
            this.lambdaArrow = lambdaArrow;
            this.methodReferenceDoubleColon = methodReferenceDoubleColon;
        }

        public Boolean getAssignment() {
            return this.assignment;
        }

        public Boolean getLogical() {
            return this.logical;
        }

        public Boolean getEquality() {
            return this.equality;
        }

        public Boolean getRelational() {
            return this.relational;
        }

        public Boolean getBitwise() {
            return this.bitwise;
        }

        public Boolean getAdditive() {
            return this.additive;
        }

        public Boolean getMultiplicative() {
            return this.multiplicative;
        }

        public Boolean getShift() {
            return this.shift;
        }

        public Boolean getUnary() {
            return this.unary;
        }

        public Boolean getLambdaArrow() {
            return this.lambdaArrow;
        }

        public Boolean getMethodReferenceDoubleColon() {
            return this.methodReferenceDoubleColon;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.AroundOperators)) return false;
            final SpacesStyle.AroundOperators other = (SpacesStyle.AroundOperators) o;
            final Object this$assignment = this.getAssignment();
            final Object other$assignment = other.getAssignment();
            if (this$assignment == null ? other$assignment != null : !this$assignment.equals(other$assignment)) return false;
            final Object this$logical = this.getLogical();
            final Object other$logical = other.getLogical();
            if (this$logical == null ? other$logical != null : !this$logical.equals(other$logical)) return false;
            final Object this$equality = this.getEquality();
            final Object other$equality = other.getEquality();
            if (this$equality == null ? other$equality != null : !this$equality.equals(other$equality)) return false;
            final Object this$relational = this.getRelational();
            final Object other$relational = other.getRelational();
            if (this$relational == null ? other$relational != null : !this$relational.equals(other$relational)) return false;
            final Object this$bitwise = this.getBitwise();
            final Object other$bitwise = other.getBitwise();
            if (this$bitwise == null ? other$bitwise != null : !this$bitwise.equals(other$bitwise)) return false;
            final Object this$additive = this.getAdditive();
            final Object other$additive = other.getAdditive();
            if (this$additive == null ? other$additive != null : !this$additive.equals(other$additive)) return false;
            final Object this$multiplicative = this.getMultiplicative();
            final Object other$multiplicative = other.getMultiplicative();
            if (this$multiplicative == null ? other$multiplicative != null : !this$multiplicative.equals(other$multiplicative)) return false;
            final Object this$shift = this.getShift();
            final Object other$shift = other.getShift();
            if (this$shift == null ? other$shift != null : !this$shift.equals(other$shift)) return false;
            final Object this$unary = this.getUnary();
            final Object other$unary = other.getUnary();
            if (this$unary == null ? other$unary != null : !this$unary.equals(other$unary)) return false;
            final Object this$lambdaArrow = this.getLambdaArrow();
            final Object other$lambdaArrow = other.getLambdaArrow();
            if (this$lambdaArrow == null ? other$lambdaArrow != null : !this$lambdaArrow.equals(other$lambdaArrow)) return false;
            final Object this$methodReferenceDoubleColon = this.getMethodReferenceDoubleColon();
            final Object other$methodReferenceDoubleColon = other.getMethodReferenceDoubleColon();
            if (this$methodReferenceDoubleColon == null ? other$methodReferenceDoubleColon != null : !this$methodReferenceDoubleColon.equals(other$methodReferenceDoubleColon)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $assignment = this.getAssignment();
            result = result * PRIME + ($assignment == null ? 43 : $assignment.hashCode());
            final Object $logical = this.getLogical();
            result = result * PRIME + ($logical == null ? 43 : $logical.hashCode());
            final Object $equality = this.getEquality();
            result = result * PRIME + ($equality == null ? 43 : $equality.hashCode());
            final Object $relational = this.getRelational();
            result = result * PRIME + ($relational == null ? 43 : $relational.hashCode());
            final Object $bitwise = this.getBitwise();
            result = result * PRIME + ($bitwise == null ? 43 : $bitwise.hashCode());
            final Object $additive = this.getAdditive();
            result = result * PRIME + ($additive == null ? 43 : $additive.hashCode());
            final Object $multiplicative = this.getMultiplicative();
            result = result * PRIME + ($multiplicative == null ? 43 : $multiplicative.hashCode());
            final Object $shift = this.getShift();
            result = result * PRIME + ($shift == null ? 43 : $shift.hashCode());
            final Object $unary = this.getUnary();
            result = result * PRIME + ($unary == null ? 43 : $unary.hashCode());
            final Object $lambdaArrow = this.getLambdaArrow();
            result = result * PRIME + ($lambdaArrow == null ? 43 : $lambdaArrow.hashCode());
            final Object $methodReferenceDoubleColon = this.getMethodReferenceDoubleColon();
            result = result * PRIME + ($methodReferenceDoubleColon == null ? 43 : $methodReferenceDoubleColon.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.AroundOperators(assignment=" + this.getAssignment() + ", logical=" + this.getLogical() + ", equality=" + this.getEquality() + ", relational=" + this.getRelational() + ", bitwise=" + this.getBitwise() + ", additive=" + this.getAdditive() + ", multiplicative=" + this.getMultiplicative() + ", shift=" + this.getShift() + ", unary=" + this.getUnary() + ", lambdaArrow=" + this.getLambdaArrow() + ", methodReferenceDoubleColon=" + this.getMethodReferenceDoubleColon() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withAssignment(final Boolean assignment) {
            return this.assignment == assignment ? this : new SpacesStyle.AroundOperators(assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withLogical(final Boolean logical) {
            return this.logical == logical ? this : new SpacesStyle.AroundOperators(this.assignment, logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withEquality(final Boolean equality) {
            return this.equality == equality ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withRelational(final Boolean relational) {
            return this.relational == relational ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withBitwise(final Boolean bitwise) {
            return this.bitwise == bitwise ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withAdditive(final Boolean additive) {
            return this.additive == additive ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withMultiplicative(final Boolean multiplicative) {
            return this.multiplicative == multiplicative ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, multiplicative, this.shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withShift(final Boolean shift) {
            return this.shift == shift ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, shift, this.unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withUnary(final Boolean unary) {
            return this.unary == unary ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, unary, this.lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withLambdaArrow(final Boolean lambdaArrow) {
            return this.lambdaArrow == lambdaArrow ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, lambdaArrow, this.methodReferenceDoubleColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.AroundOperators withMethodReferenceDoubleColon(final Boolean methodReferenceDoubleColon) {
            return this.methodReferenceDoubleColon == methodReferenceDoubleColon ? this : new SpacesStyle.AroundOperators(this.assignment, this.logical, this.equality, this.relational, this.bitwise, this.additive, this.multiplicative, this.shift, this.unary, this.lambdaArrow, methodReferenceDoubleColon);
        }
    }


    public static final class BeforeLeftBrace {
        private final Boolean classLeftBrace;
        private final Boolean methodLeftBrace;
        private final Boolean ifLeftBrace;
        private final Boolean elseLeftBrace;
        private final Boolean forLeftBrace;
        private final Boolean whileLeftBrace;
        private final Boolean doLeftBrace;
        private final Boolean switchLeftBrace;
        private final Boolean tryLeftBrace;
        private final Boolean catchLeftBrace;
        private final Boolean finallyLeftBrace;
        private final Boolean synchronizedLeftBrace;
        private final Boolean arrayInitializerLeftBrace;
        private final Boolean annotationArrayInitializerLeftBrace;

        public BeforeLeftBrace(final Boolean classLeftBrace, final Boolean methodLeftBrace, final Boolean ifLeftBrace, final Boolean elseLeftBrace, final Boolean forLeftBrace, final Boolean whileLeftBrace, final Boolean doLeftBrace, final Boolean switchLeftBrace, final Boolean tryLeftBrace, final Boolean catchLeftBrace, final Boolean finallyLeftBrace, final Boolean synchronizedLeftBrace, final Boolean arrayInitializerLeftBrace, final Boolean annotationArrayInitializerLeftBrace) {
            this.classLeftBrace = classLeftBrace;
            this.methodLeftBrace = methodLeftBrace;
            this.ifLeftBrace = ifLeftBrace;
            this.elseLeftBrace = elseLeftBrace;
            this.forLeftBrace = forLeftBrace;
            this.whileLeftBrace = whileLeftBrace;
            this.doLeftBrace = doLeftBrace;
            this.switchLeftBrace = switchLeftBrace;
            this.tryLeftBrace = tryLeftBrace;
            this.catchLeftBrace = catchLeftBrace;
            this.finallyLeftBrace = finallyLeftBrace;
            this.synchronizedLeftBrace = synchronizedLeftBrace;
            this.arrayInitializerLeftBrace = arrayInitializerLeftBrace;
            this.annotationArrayInitializerLeftBrace = annotationArrayInitializerLeftBrace;
        }

        public Boolean getClassLeftBrace() {
            return this.classLeftBrace;
        }

        public Boolean getMethodLeftBrace() {
            return this.methodLeftBrace;
        }

        public Boolean getIfLeftBrace() {
            return this.ifLeftBrace;
        }

        public Boolean getElseLeftBrace() {
            return this.elseLeftBrace;
        }

        public Boolean getForLeftBrace() {
            return this.forLeftBrace;
        }

        public Boolean getWhileLeftBrace() {
            return this.whileLeftBrace;
        }

        public Boolean getDoLeftBrace() {
            return this.doLeftBrace;
        }

        public Boolean getSwitchLeftBrace() {
            return this.switchLeftBrace;
        }

        public Boolean getTryLeftBrace() {
            return this.tryLeftBrace;
        }

        public Boolean getCatchLeftBrace() {
            return this.catchLeftBrace;
        }

        public Boolean getFinallyLeftBrace() {
            return this.finallyLeftBrace;
        }

        public Boolean getSynchronizedLeftBrace() {
            return this.synchronizedLeftBrace;
        }

        public Boolean getArrayInitializerLeftBrace() {
            return this.arrayInitializerLeftBrace;
        }

        public Boolean getAnnotationArrayInitializerLeftBrace() {
            return this.annotationArrayInitializerLeftBrace;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.BeforeLeftBrace)) return false;
            final SpacesStyle.BeforeLeftBrace other = (SpacesStyle.BeforeLeftBrace) o;
            final Object this$classLeftBrace = this.getClassLeftBrace();
            final Object other$classLeftBrace = other.getClassLeftBrace();
            if (this$classLeftBrace == null ? other$classLeftBrace != null : !this$classLeftBrace.equals(other$classLeftBrace)) return false;
            final Object this$methodLeftBrace = this.getMethodLeftBrace();
            final Object other$methodLeftBrace = other.getMethodLeftBrace();
            if (this$methodLeftBrace == null ? other$methodLeftBrace != null : !this$methodLeftBrace.equals(other$methodLeftBrace)) return false;
            final Object this$ifLeftBrace = this.getIfLeftBrace();
            final Object other$ifLeftBrace = other.getIfLeftBrace();
            if (this$ifLeftBrace == null ? other$ifLeftBrace != null : !this$ifLeftBrace.equals(other$ifLeftBrace)) return false;
            final Object this$elseLeftBrace = this.getElseLeftBrace();
            final Object other$elseLeftBrace = other.getElseLeftBrace();
            if (this$elseLeftBrace == null ? other$elseLeftBrace != null : !this$elseLeftBrace.equals(other$elseLeftBrace)) return false;
            final Object this$forLeftBrace = this.getForLeftBrace();
            final Object other$forLeftBrace = other.getForLeftBrace();
            if (this$forLeftBrace == null ? other$forLeftBrace != null : !this$forLeftBrace.equals(other$forLeftBrace)) return false;
            final Object this$whileLeftBrace = this.getWhileLeftBrace();
            final Object other$whileLeftBrace = other.getWhileLeftBrace();
            if (this$whileLeftBrace == null ? other$whileLeftBrace != null : !this$whileLeftBrace.equals(other$whileLeftBrace)) return false;
            final Object this$doLeftBrace = this.getDoLeftBrace();
            final Object other$doLeftBrace = other.getDoLeftBrace();
            if (this$doLeftBrace == null ? other$doLeftBrace != null : !this$doLeftBrace.equals(other$doLeftBrace)) return false;
            final Object this$switchLeftBrace = this.getSwitchLeftBrace();
            final Object other$switchLeftBrace = other.getSwitchLeftBrace();
            if (this$switchLeftBrace == null ? other$switchLeftBrace != null : !this$switchLeftBrace.equals(other$switchLeftBrace)) return false;
            final Object this$tryLeftBrace = this.getTryLeftBrace();
            final Object other$tryLeftBrace = other.getTryLeftBrace();
            if (this$tryLeftBrace == null ? other$tryLeftBrace != null : !this$tryLeftBrace.equals(other$tryLeftBrace)) return false;
            final Object this$catchLeftBrace = this.getCatchLeftBrace();
            final Object other$catchLeftBrace = other.getCatchLeftBrace();
            if (this$catchLeftBrace == null ? other$catchLeftBrace != null : !this$catchLeftBrace.equals(other$catchLeftBrace)) return false;
            final Object this$finallyLeftBrace = this.getFinallyLeftBrace();
            final Object other$finallyLeftBrace = other.getFinallyLeftBrace();
            if (this$finallyLeftBrace == null ? other$finallyLeftBrace != null : !this$finallyLeftBrace.equals(other$finallyLeftBrace)) return false;
            final Object this$synchronizedLeftBrace = this.getSynchronizedLeftBrace();
            final Object other$synchronizedLeftBrace = other.getSynchronizedLeftBrace();
            if (this$synchronizedLeftBrace == null ? other$synchronizedLeftBrace != null : !this$synchronizedLeftBrace.equals(other$synchronizedLeftBrace)) return false;
            final Object this$arrayInitializerLeftBrace = this.getArrayInitializerLeftBrace();
            final Object other$arrayInitializerLeftBrace = other.getArrayInitializerLeftBrace();
            if (this$arrayInitializerLeftBrace == null ? other$arrayInitializerLeftBrace != null : !this$arrayInitializerLeftBrace.equals(other$arrayInitializerLeftBrace)) return false;
            final Object this$annotationArrayInitializerLeftBrace = this.getAnnotationArrayInitializerLeftBrace();
            final Object other$annotationArrayInitializerLeftBrace = other.getAnnotationArrayInitializerLeftBrace();
            if (this$annotationArrayInitializerLeftBrace == null ? other$annotationArrayInitializerLeftBrace != null : !this$annotationArrayInitializerLeftBrace.equals(other$annotationArrayInitializerLeftBrace)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $classLeftBrace = this.getClassLeftBrace();
            result = result * PRIME + ($classLeftBrace == null ? 43 : $classLeftBrace.hashCode());
            final Object $methodLeftBrace = this.getMethodLeftBrace();
            result = result * PRIME + ($methodLeftBrace == null ? 43 : $methodLeftBrace.hashCode());
            final Object $ifLeftBrace = this.getIfLeftBrace();
            result = result * PRIME + ($ifLeftBrace == null ? 43 : $ifLeftBrace.hashCode());
            final Object $elseLeftBrace = this.getElseLeftBrace();
            result = result * PRIME + ($elseLeftBrace == null ? 43 : $elseLeftBrace.hashCode());
            final Object $forLeftBrace = this.getForLeftBrace();
            result = result * PRIME + ($forLeftBrace == null ? 43 : $forLeftBrace.hashCode());
            final Object $whileLeftBrace = this.getWhileLeftBrace();
            result = result * PRIME + ($whileLeftBrace == null ? 43 : $whileLeftBrace.hashCode());
            final Object $doLeftBrace = this.getDoLeftBrace();
            result = result * PRIME + ($doLeftBrace == null ? 43 : $doLeftBrace.hashCode());
            final Object $switchLeftBrace = this.getSwitchLeftBrace();
            result = result * PRIME + ($switchLeftBrace == null ? 43 : $switchLeftBrace.hashCode());
            final Object $tryLeftBrace = this.getTryLeftBrace();
            result = result * PRIME + ($tryLeftBrace == null ? 43 : $tryLeftBrace.hashCode());
            final Object $catchLeftBrace = this.getCatchLeftBrace();
            result = result * PRIME + ($catchLeftBrace == null ? 43 : $catchLeftBrace.hashCode());
            final Object $finallyLeftBrace = this.getFinallyLeftBrace();
            result = result * PRIME + ($finallyLeftBrace == null ? 43 : $finallyLeftBrace.hashCode());
            final Object $synchronizedLeftBrace = this.getSynchronizedLeftBrace();
            result = result * PRIME + ($synchronizedLeftBrace == null ? 43 : $synchronizedLeftBrace.hashCode());
            final Object $arrayInitializerLeftBrace = this.getArrayInitializerLeftBrace();
            result = result * PRIME + ($arrayInitializerLeftBrace == null ? 43 : $arrayInitializerLeftBrace.hashCode());
            final Object $annotationArrayInitializerLeftBrace = this.getAnnotationArrayInitializerLeftBrace();
            result = result * PRIME + ($annotationArrayInitializerLeftBrace == null ? 43 : $annotationArrayInitializerLeftBrace.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.BeforeLeftBrace(classLeftBrace=" + this.getClassLeftBrace() + ", methodLeftBrace=" + this.getMethodLeftBrace() + ", ifLeftBrace=" + this.getIfLeftBrace() + ", elseLeftBrace=" + this.getElseLeftBrace() + ", forLeftBrace=" + this.getForLeftBrace() + ", whileLeftBrace=" + this.getWhileLeftBrace() + ", doLeftBrace=" + this.getDoLeftBrace() + ", switchLeftBrace=" + this.getSwitchLeftBrace() + ", tryLeftBrace=" + this.getTryLeftBrace() + ", catchLeftBrace=" + this.getCatchLeftBrace() + ", finallyLeftBrace=" + this.getFinallyLeftBrace() + ", synchronizedLeftBrace=" + this.getSynchronizedLeftBrace() + ", arrayInitializerLeftBrace=" + this.getArrayInitializerLeftBrace() + ", annotationArrayInitializerLeftBrace=" + this.getAnnotationArrayInitializerLeftBrace() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withClassLeftBrace(final Boolean classLeftBrace) {
            return this.classLeftBrace == classLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withMethodLeftBrace(final Boolean methodLeftBrace) {
            return this.methodLeftBrace == methodLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withIfLeftBrace(final Boolean ifLeftBrace) {
            return this.ifLeftBrace == ifLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withElseLeftBrace(final Boolean elseLeftBrace) {
            return this.elseLeftBrace == elseLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withForLeftBrace(final Boolean forLeftBrace) {
            return this.forLeftBrace == forLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withWhileLeftBrace(final Boolean whileLeftBrace) {
            return this.whileLeftBrace == whileLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withDoLeftBrace(final Boolean doLeftBrace) {
            return this.doLeftBrace == doLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withSwitchLeftBrace(final Boolean switchLeftBrace) {
            return this.switchLeftBrace == switchLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withTryLeftBrace(final Boolean tryLeftBrace) {
            return this.tryLeftBrace == tryLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withCatchLeftBrace(final Boolean catchLeftBrace) {
            return this.catchLeftBrace == catchLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withFinallyLeftBrace(final Boolean finallyLeftBrace) {
            return this.finallyLeftBrace == finallyLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withSynchronizedLeftBrace(final Boolean synchronizedLeftBrace) {
            return this.synchronizedLeftBrace == synchronizedLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, synchronizedLeftBrace, this.arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withArrayInitializerLeftBrace(final Boolean arrayInitializerLeftBrace) {
            return this.arrayInitializerLeftBrace == arrayInitializerLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, arrayInitializerLeftBrace, this.annotationArrayInitializerLeftBrace);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeLeftBrace withAnnotationArrayInitializerLeftBrace(final Boolean annotationArrayInitializerLeftBrace) {
            return this.annotationArrayInitializerLeftBrace == annotationArrayInitializerLeftBrace ? this : new SpacesStyle.BeforeLeftBrace(this.classLeftBrace, this.methodLeftBrace, this.ifLeftBrace, this.elseLeftBrace, this.forLeftBrace, this.whileLeftBrace, this.doLeftBrace, this.switchLeftBrace, this.tryLeftBrace, this.catchLeftBrace, this.finallyLeftBrace, this.synchronizedLeftBrace, this.arrayInitializerLeftBrace, annotationArrayInitializerLeftBrace);
        }
    }


    public static final class BeforeKeywords {
        private final Boolean elseKeyword;
        private final Boolean whileKeyword;
        private final Boolean catchKeyword;
        private final Boolean finallyKeyword;

        public BeforeKeywords(final Boolean elseKeyword, final Boolean whileKeyword, final Boolean catchKeyword, final Boolean finallyKeyword) {
            this.elseKeyword = elseKeyword;
            this.whileKeyword = whileKeyword;
            this.catchKeyword = catchKeyword;
            this.finallyKeyword = finallyKeyword;
        }

        public Boolean getElseKeyword() {
            return this.elseKeyword;
        }

        public Boolean getWhileKeyword() {
            return this.whileKeyword;
        }

        public Boolean getCatchKeyword() {
            return this.catchKeyword;
        }

        public Boolean getFinallyKeyword() {
            return this.finallyKeyword;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.BeforeKeywords)) return false;
            final SpacesStyle.BeforeKeywords other = (SpacesStyle.BeforeKeywords) o;
            final Object this$elseKeyword = this.getElseKeyword();
            final Object other$elseKeyword = other.getElseKeyword();
            if (this$elseKeyword == null ? other$elseKeyword != null : !this$elseKeyword.equals(other$elseKeyword)) return false;
            final Object this$whileKeyword = this.getWhileKeyword();
            final Object other$whileKeyword = other.getWhileKeyword();
            if (this$whileKeyword == null ? other$whileKeyword != null : !this$whileKeyword.equals(other$whileKeyword)) return false;
            final Object this$catchKeyword = this.getCatchKeyword();
            final Object other$catchKeyword = other.getCatchKeyword();
            if (this$catchKeyword == null ? other$catchKeyword != null : !this$catchKeyword.equals(other$catchKeyword)) return false;
            final Object this$finallyKeyword = this.getFinallyKeyword();
            final Object other$finallyKeyword = other.getFinallyKeyword();
            if (this$finallyKeyword == null ? other$finallyKeyword != null : !this$finallyKeyword.equals(other$finallyKeyword)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $elseKeyword = this.getElseKeyword();
            result = result * PRIME + ($elseKeyword == null ? 43 : $elseKeyword.hashCode());
            final Object $whileKeyword = this.getWhileKeyword();
            result = result * PRIME + ($whileKeyword == null ? 43 : $whileKeyword.hashCode());
            final Object $catchKeyword = this.getCatchKeyword();
            result = result * PRIME + ($catchKeyword == null ? 43 : $catchKeyword.hashCode());
            final Object $finallyKeyword = this.getFinallyKeyword();
            result = result * PRIME + ($finallyKeyword == null ? 43 : $finallyKeyword.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.BeforeKeywords(elseKeyword=" + this.getElseKeyword() + ", whileKeyword=" + this.getWhileKeyword() + ", catchKeyword=" + this.getCatchKeyword() + ", finallyKeyword=" + this.getFinallyKeyword() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeKeywords withElseKeyword(final Boolean elseKeyword) {
            return this.elseKeyword == elseKeyword ? this : new SpacesStyle.BeforeKeywords(elseKeyword, this.whileKeyword, this.catchKeyword, this.finallyKeyword);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeKeywords withWhileKeyword(final Boolean whileKeyword) {
            return this.whileKeyword == whileKeyword ? this : new SpacesStyle.BeforeKeywords(this.elseKeyword, whileKeyword, this.catchKeyword, this.finallyKeyword);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeKeywords withCatchKeyword(final Boolean catchKeyword) {
            return this.catchKeyword == catchKeyword ? this : new SpacesStyle.BeforeKeywords(this.elseKeyword, this.whileKeyword, catchKeyword, this.finallyKeyword);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.BeforeKeywords withFinallyKeyword(final Boolean finallyKeyword) {
            return this.finallyKeyword == finallyKeyword ? this : new SpacesStyle.BeforeKeywords(this.elseKeyword, this.whileKeyword, this.catchKeyword, finallyKeyword);
        }
    }


    public static final class Within {
        /**
         * Based on testing in IntelliJ, within codeBraces only affects whether or not a space is inserted
         * into empty interface and class braces, not functions, logical blocks, or lambda braces.
         */
        private final Boolean codeBraces;
        private final Boolean brackets;
        private final Boolean arrayInitializerBraces;
        private final Boolean emptyArrayInitializerBraces;
        private final Boolean groupingParentheses;
        private final Boolean methodDeclarationParentheses;
        private final Boolean emptyMethodDeclarationParentheses;
        private final Boolean methodCallParentheses;
        private final Boolean emptyMethodCallParentheses;
        private final Boolean ifParentheses;
        private final Boolean forParentheses;
        private final Boolean whileParentheses;
        private final Boolean switchParentheses;
        private final Boolean tryParentheses;
        private final Boolean catchParentheses;
        private final Boolean synchronizedParentheses;
        private final Boolean typeCastParentheses;
        private final Boolean annotationParentheses;
        private final Boolean angleBrackets;
        // Records are a java 14 preview feature (and still a preview language feature in java 15)
        // rewrite does not consult this style setting for now
        private final Boolean recordHeader;

        public Within(final Boolean codeBraces, final Boolean brackets, final Boolean arrayInitializerBraces, final Boolean emptyArrayInitializerBraces, final Boolean groupingParentheses, final Boolean methodDeclarationParentheses, final Boolean emptyMethodDeclarationParentheses, final Boolean methodCallParentheses, final Boolean emptyMethodCallParentheses, final Boolean ifParentheses, final Boolean forParentheses, final Boolean whileParentheses, final Boolean switchParentheses, final Boolean tryParentheses, final Boolean catchParentheses, final Boolean synchronizedParentheses, final Boolean typeCastParentheses, final Boolean annotationParentheses, final Boolean angleBrackets, final Boolean recordHeader) {
            this.codeBraces = codeBraces;
            this.brackets = brackets;
            this.arrayInitializerBraces = arrayInitializerBraces;
            this.emptyArrayInitializerBraces = emptyArrayInitializerBraces;
            this.groupingParentheses = groupingParentheses;
            this.methodDeclarationParentheses = methodDeclarationParentheses;
            this.emptyMethodDeclarationParentheses = emptyMethodDeclarationParentheses;
            this.methodCallParentheses = methodCallParentheses;
            this.emptyMethodCallParentheses = emptyMethodCallParentheses;
            this.ifParentheses = ifParentheses;
            this.forParentheses = forParentheses;
            this.whileParentheses = whileParentheses;
            this.switchParentheses = switchParentheses;
            this.tryParentheses = tryParentheses;
            this.catchParentheses = catchParentheses;
            this.synchronizedParentheses = synchronizedParentheses;
            this.typeCastParentheses = typeCastParentheses;
            this.annotationParentheses = annotationParentheses;
            this.angleBrackets = angleBrackets;
            this.recordHeader = recordHeader;
        }

        /**
         * Based on testing in IntelliJ, within codeBraces only affects whether or not a space is inserted
         * into empty interface and class braces, not functions, logical blocks, or lambda braces.
         */
        public Boolean getCodeBraces() {
            return this.codeBraces;
        }

        public Boolean getBrackets() {
            return this.brackets;
        }

        public Boolean getArrayInitializerBraces() {
            return this.arrayInitializerBraces;
        }

        public Boolean getEmptyArrayInitializerBraces() {
            return this.emptyArrayInitializerBraces;
        }

        public Boolean getGroupingParentheses() {
            return this.groupingParentheses;
        }

        public Boolean getMethodDeclarationParentheses() {
            return this.methodDeclarationParentheses;
        }

        public Boolean getEmptyMethodDeclarationParentheses() {
            return this.emptyMethodDeclarationParentheses;
        }

        public Boolean getMethodCallParentheses() {
            return this.methodCallParentheses;
        }

        public Boolean getEmptyMethodCallParentheses() {
            return this.emptyMethodCallParentheses;
        }

        public Boolean getIfParentheses() {
            return this.ifParentheses;
        }

        public Boolean getForParentheses() {
            return this.forParentheses;
        }

        public Boolean getWhileParentheses() {
            return this.whileParentheses;
        }

        public Boolean getSwitchParentheses() {
            return this.switchParentheses;
        }

        public Boolean getTryParentheses() {
            return this.tryParentheses;
        }

        public Boolean getCatchParentheses() {
            return this.catchParentheses;
        }

        public Boolean getSynchronizedParentheses() {
            return this.synchronizedParentheses;
        }

        public Boolean getTypeCastParentheses() {
            return this.typeCastParentheses;
        }

        public Boolean getAnnotationParentheses() {
            return this.annotationParentheses;
        }

        public Boolean getAngleBrackets() {
            return this.angleBrackets;
        }

        public Boolean getRecordHeader() {
            return this.recordHeader;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.Within)) return false;
            final SpacesStyle.Within other = (SpacesStyle.Within) o;
            final Object this$codeBraces = this.getCodeBraces();
            final Object other$codeBraces = other.getCodeBraces();
            if (this$codeBraces == null ? other$codeBraces != null : !this$codeBraces.equals(other$codeBraces)) return false;
            final Object this$brackets = this.getBrackets();
            final Object other$brackets = other.getBrackets();
            if (this$brackets == null ? other$brackets != null : !this$brackets.equals(other$brackets)) return false;
            final Object this$arrayInitializerBraces = this.getArrayInitializerBraces();
            final Object other$arrayInitializerBraces = other.getArrayInitializerBraces();
            if (this$arrayInitializerBraces == null ? other$arrayInitializerBraces != null : !this$arrayInitializerBraces.equals(other$arrayInitializerBraces)) return false;
            final Object this$emptyArrayInitializerBraces = this.getEmptyArrayInitializerBraces();
            final Object other$emptyArrayInitializerBraces = other.getEmptyArrayInitializerBraces();
            if (this$emptyArrayInitializerBraces == null ? other$emptyArrayInitializerBraces != null : !this$emptyArrayInitializerBraces.equals(other$emptyArrayInitializerBraces)) return false;
            final Object this$groupingParentheses = this.getGroupingParentheses();
            final Object other$groupingParentheses = other.getGroupingParentheses();
            if (this$groupingParentheses == null ? other$groupingParentheses != null : !this$groupingParentheses.equals(other$groupingParentheses)) return false;
            final Object this$methodDeclarationParentheses = this.getMethodDeclarationParentheses();
            final Object other$methodDeclarationParentheses = other.getMethodDeclarationParentheses();
            if (this$methodDeclarationParentheses == null ? other$methodDeclarationParentheses != null : !this$methodDeclarationParentheses.equals(other$methodDeclarationParentheses)) return false;
            final Object this$emptyMethodDeclarationParentheses = this.getEmptyMethodDeclarationParentheses();
            final Object other$emptyMethodDeclarationParentheses = other.getEmptyMethodDeclarationParentheses();
            if (this$emptyMethodDeclarationParentheses == null ? other$emptyMethodDeclarationParentheses != null : !this$emptyMethodDeclarationParentheses.equals(other$emptyMethodDeclarationParentheses)) return false;
            final Object this$methodCallParentheses = this.getMethodCallParentheses();
            final Object other$methodCallParentheses = other.getMethodCallParentheses();
            if (this$methodCallParentheses == null ? other$methodCallParentheses != null : !this$methodCallParentheses.equals(other$methodCallParentheses)) return false;
            final Object this$emptyMethodCallParentheses = this.getEmptyMethodCallParentheses();
            final Object other$emptyMethodCallParentheses = other.getEmptyMethodCallParentheses();
            if (this$emptyMethodCallParentheses == null ? other$emptyMethodCallParentheses != null : !this$emptyMethodCallParentheses.equals(other$emptyMethodCallParentheses)) return false;
            final Object this$ifParentheses = this.getIfParentheses();
            final Object other$ifParentheses = other.getIfParentheses();
            if (this$ifParentheses == null ? other$ifParentheses != null : !this$ifParentheses.equals(other$ifParentheses)) return false;
            final Object this$forParentheses = this.getForParentheses();
            final Object other$forParentheses = other.getForParentheses();
            if (this$forParentheses == null ? other$forParentheses != null : !this$forParentheses.equals(other$forParentheses)) return false;
            final Object this$whileParentheses = this.getWhileParentheses();
            final Object other$whileParentheses = other.getWhileParentheses();
            if (this$whileParentheses == null ? other$whileParentheses != null : !this$whileParentheses.equals(other$whileParentheses)) return false;
            final Object this$switchParentheses = this.getSwitchParentheses();
            final Object other$switchParentheses = other.getSwitchParentheses();
            if (this$switchParentheses == null ? other$switchParentheses != null : !this$switchParentheses.equals(other$switchParentheses)) return false;
            final Object this$tryParentheses = this.getTryParentheses();
            final Object other$tryParentheses = other.getTryParentheses();
            if (this$tryParentheses == null ? other$tryParentheses != null : !this$tryParentheses.equals(other$tryParentheses)) return false;
            final Object this$catchParentheses = this.getCatchParentheses();
            final Object other$catchParentheses = other.getCatchParentheses();
            if (this$catchParentheses == null ? other$catchParentheses != null : !this$catchParentheses.equals(other$catchParentheses)) return false;
            final Object this$synchronizedParentheses = this.getSynchronizedParentheses();
            final Object other$synchronizedParentheses = other.getSynchronizedParentheses();
            if (this$synchronizedParentheses == null ? other$synchronizedParentheses != null : !this$synchronizedParentheses.equals(other$synchronizedParentheses)) return false;
            final Object this$typeCastParentheses = this.getTypeCastParentheses();
            final Object other$typeCastParentheses = other.getTypeCastParentheses();
            if (this$typeCastParentheses == null ? other$typeCastParentheses != null : !this$typeCastParentheses.equals(other$typeCastParentheses)) return false;
            final Object this$annotationParentheses = this.getAnnotationParentheses();
            final Object other$annotationParentheses = other.getAnnotationParentheses();
            if (this$annotationParentheses == null ? other$annotationParentheses != null : !this$annotationParentheses.equals(other$annotationParentheses)) return false;
            final Object this$angleBrackets = this.getAngleBrackets();
            final Object other$angleBrackets = other.getAngleBrackets();
            if (this$angleBrackets == null ? other$angleBrackets != null : !this$angleBrackets.equals(other$angleBrackets)) return false;
            final Object this$recordHeader = this.getRecordHeader();
            final Object other$recordHeader = other.getRecordHeader();
            if (this$recordHeader == null ? other$recordHeader != null : !this$recordHeader.equals(other$recordHeader)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $codeBraces = this.getCodeBraces();
            result = result * PRIME + ($codeBraces == null ? 43 : $codeBraces.hashCode());
            final Object $brackets = this.getBrackets();
            result = result * PRIME + ($brackets == null ? 43 : $brackets.hashCode());
            final Object $arrayInitializerBraces = this.getArrayInitializerBraces();
            result = result * PRIME + ($arrayInitializerBraces == null ? 43 : $arrayInitializerBraces.hashCode());
            final Object $emptyArrayInitializerBraces = this.getEmptyArrayInitializerBraces();
            result = result * PRIME + ($emptyArrayInitializerBraces == null ? 43 : $emptyArrayInitializerBraces.hashCode());
            final Object $groupingParentheses = this.getGroupingParentheses();
            result = result * PRIME + ($groupingParentheses == null ? 43 : $groupingParentheses.hashCode());
            final Object $methodDeclarationParentheses = this.getMethodDeclarationParentheses();
            result = result * PRIME + ($methodDeclarationParentheses == null ? 43 : $methodDeclarationParentheses.hashCode());
            final Object $emptyMethodDeclarationParentheses = this.getEmptyMethodDeclarationParentheses();
            result = result * PRIME + ($emptyMethodDeclarationParentheses == null ? 43 : $emptyMethodDeclarationParentheses.hashCode());
            final Object $methodCallParentheses = this.getMethodCallParentheses();
            result = result * PRIME + ($methodCallParentheses == null ? 43 : $methodCallParentheses.hashCode());
            final Object $emptyMethodCallParentheses = this.getEmptyMethodCallParentheses();
            result = result * PRIME + ($emptyMethodCallParentheses == null ? 43 : $emptyMethodCallParentheses.hashCode());
            final Object $ifParentheses = this.getIfParentheses();
            result = result * PRIME + ($ifParentheses == null ? 43 : $ifParentheses.hashCode());
            final Object $forParentheses = this.getForParentheses();
            result = result * PRIME + ($forParentheses == null ? 43 : $forParentheses.hashCode());
            final Object $whileParentheses = this.getWhileParentheses();
            result = result * PRIME + ($whileParentheses == null ? 43 : $whileParentheses.hashCode());
            final Object $switchParentheses = this.getSwitchParentheses();
            result = result * PRIME + ($switchParentheses == null ? 43 : $switchParentheses.hashCode());
            final Object $tryParentheses = this.getTryParentheses();
            result = result * PRIME + ($tryParentheses == null ? 43 : $tryParentheses.hashCode());
            final Object $catchParentheses = this.getCatchParentheses();
            result = result * PRIME + ($catchParentheses == null ? 43 : $catchParentheses.hashCode());
            final Object $synchronizedParentheses = this.getSynchronizedParentheses();
            result = result * PRIME + ($synchronizedParentheses == null ? 43 : $synchronizedParentheses.hashCode());
            final Object $typeCastParentheses = this.getTypeCastParentheses();
            result = result * PRIME + ($typeCastParentheses == null ? 43 : $typeCastParentheses.hashCode());
            final Object $annotationParentheses = this.getAnnotationParentheses();
            result = result * PRIME + ($annotationParentheses == null ? 43 : $annotationParentheses.hashCode());
            final Object $angleBrackets = this.getAngleBrackets();
            result = result * PRIME + ($angleBrackets == null ? 43 : $angleBrackets.hashCode());
            final Object $recordHeader = this.getRecordHeader();
            result = result * PRIME + ($recordHeader == null ? 43 : $recordHeader.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.Within(codeBraces=" + this.getCodeBraces() + ", brackets=" + this.getBrackets() + ", arrayInitializerBraces=" + this.getArrayInitializerBraces() + ", emptyArrayInitializerBraces=" + this.getEmptyArrayInitializerBraces() + ", groupingParentheses=" + this.getGroupingParentheses() + ", methodDeclarationParentheses=" + this.getMethodDeclarationParentheses() + ", emptyMethodDeclarationParentheses=" + this.getEmptyMethodDeclarationParentheses() + ", methodCallParentheses=" + this.getMethodCallParentheses() + ", emptyMethodCallParentheses=" + this.getEmptyMethodCallParentheses() + ", ifParentheses=" + this.getIfParentheses() + ", forParentheses=" + this.getForParentheses() + ", whileParentheses=" + this.getWhileParentheses() + ", switchParentheses=" + this.getSwitchParentheses() + ", tryParentheses=" + this.getTryParentheses() + ", catchParentheses=" + this.getCatchParentheses() + ", synchronizedParentheses=" + this.getSynchronizedParentheses() + ", typeCastParentheses=" + this.getTypeCastParentheses() + ", annotationParentheses=" + this.getAnnotationParentheses() + ", angleBrackets=" + this.getAngleBrackets() + ", recordHeader=" + this.getRecordHeader() + ")";
        }

        /**
         * Based on testing in IntelliJ, within codeBraces only affects whether or not a space is inserted
         * into empty interface and class braces, not functions, logical blocks, or lambda braces.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withCodeBraces(final Boolean codeBraces) {
            return this.codeBraces == codeBraces ? this : new SpacesStyle.Within(codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withBrackets(final Boolean brackets) {
            return this.brackets == brackets ? this : new SpacesStyle.Within(this.codeBraces, brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withArrayInitializerBraces(final Boolean arrayInitializerBraces) {
            return this.arrayInitializerBraces == arrayInitializerBraces ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withEmptyArrayInitializerBraces(final Boolean emptyArrayInitializerBraces) {
            return this.emptyArrayInitializerBraces == emptyArrayInitializerBraces ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withGroupingParentheses(final Boolean groupingParentheses) {
            return this.groupingParentheses == groupingParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withMethodDeclarationParentheses(final Boolean methodDeclarationParentheses) {
            return this.methodDeclarationParentheses == methodDeclarationParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withEmptyMethodDeclarationParentheses(final Boolean emptyMethodDeclarationParentheses) {
            return this.emptyMethodDeclarationParentheses == emptyMethodDeclarationParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withMethodCallParentheses(final Boolean methodCallParentheses) {
            return this.methodCallParentheses == methodCallParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withEmptyMethodCallParentheses(final Boolean emptyMethodCallParentheses) {
            return this.emptyMethodCallParentheses == emptyMethodCallParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withIfParentheses(final Boolean ifParentheses) {
            return this.ifParentheses == ifParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withForParentheses(final Boolean forParentheses) {
            return this.forParentheses == forParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withWhileParentheses(final Boolean whileParentheses) {
            return this.whileParentheses == whileParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withSwitchParentheses(final Boolean switchParentheses) {
            return this.switchParentheses == switchParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withTryParentheses(final Boolean tryParentheses) {
            return this.tryParentheses == tryParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withCatchParentheses(final Boolean catchParentheses) {
            return this.catchParentheses == catchParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withSynchronizedParentheses(final Boolean synchronizedParentheses) {
            return this.synchronizedParentheses == synchronizedParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withTypeCastParentheses(final Boolean typeCastParentheses) {
            return this.typeCastParentheses == typeCastParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, typeCastParentheses, this.annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withAnnotationParentheses(final Boolean annotationParentheses) {
            return this.annotationParentheses == annotationParentheses ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, annotationParentheses, this.angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withAngleBrackets(final Boolean angleBrackets) {
            return this.angleBrackets == angleBrackets ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, angleBrackets, this.recordHeader);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Within withRecordHeader(final Boolean recordHeader) {
            return this.recordHeader == recordHeader ? this : new SpacesStyle.Within(this.codeBraces, this.brackets, this.arrayInitializerBraces, this.emptyArrayInitializerBraces, this.groupingParentheses, this.methodDeclarationParentheses, this.emptyMethodDeclarationParentheses, this.methodCallParentheses, this.emptyMethodCallParentheses, this.ifParentheses, this.forParentheses, this.whileParentheses, this.switchParentheses, this.tryParentheses, this.catchParentheses, this.synchronizedParentheses, this.typeCastParentheses, this.annotationParentheses, this.angleBrackets, recordHeader);
        }
    }


    public static final class TernaryOperator {
        private final Boolean beforeQuestionMark;
        private final Boolean afterQuestionMark;
        private final Boolean beforeColon;
        private final Boolean afterColon;

        public TernaryOperator(final Boolean beforeQuestionMark, final Boolean afterQuestionMark, final Boolean beforeColon, final Boolean afterColon) {
            this.beforeQuestionMark = beforeQuestionMark;
            this.afterQuestionMark = afterQuestionMark;
            this.beforeColon = beforeColon;
            this.afterColon = afterColon;
        }

        public Boolean getBeforeQuestionMark() {
            return this.beforeQuestionMark;
        }

        public Boolean getAfterQuestionMark() {
            return this.afterQuestionMark;
        }

        public Boolean getBeforeColon() {
            return this.beforeColon;
        }

        public Boolean getAfterColon() {
            return this.afterColon;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.TernaryOperator)) return false;
            final SpacesStyle.TernaryOperator other = (SpacesStyle.TernaryOperator) o;
            final Object this$beforeQuestionMark = this.getBeforeQuestionMark();
            final Object other$beforeQuestionMark = other.getBeforeQuestionMark();
            if (this$beforeQuestionMark == null ? other$beforeQuestionMark != null : !this$beforeQuestionMark.equals(other$beforeQuestionMark)) return false;
            final Object this$afterQuestionMark = this.getAfterQuestionMark();
            final Object other$afterQuestionMark = other.getAfterQuestionMark();
            if (this$afterQuestionMark == null ? other$afterQuestionMark != null : !this$afterQuestionMark.equals(other$afterQuestionMark)) return false;
            final Object this$beforeColon = this.getBeforeColon();
            final Object other$beforeColon = other.getBeforeColon();
            if (this$beforeColon == null ? other$beforeColon != null : !this$beforeColon.equals(other$beforeColon)) return false;
            final Object this$afterColon = this.getAfterColon();
            final Object other$afterColon = other.getAfterColon();
            if (this$afterColon == null ? other$afterColon != null : !this$afterColon.equals(other$afterColon)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $beforeQuestionMark = this.getBeforeQuestionMark();
            result = result * PRIME + ($beforeQuestionMark == null ? 43 : $beforeQuestionMark.hashCode());
            final Object $afterQuestionMark = this.getAfterQuestionMark();
            result = result * PRIME + ($afterQuestionMark == null ? 43 : $afterQuestionMark.hashCode());
            final Object $beforeColon = this.getBeforeColon();
            result = result * PRIME + ($beforeColon == null ? 43 : $beforeColon.hashCode());
            final Object $afterColon = this.getAfterColon();
            result = result * PRIME + ($afterColon == null ? 43 : $afterColon.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.TernaryOperator(beforeQuestionMark=" + this.getBeforeQuestionMark() + ", afterQuestionMark=" + this.getAfterQuestionMark() + ", beforeColon=" + this.getBeforeColon() + ", afterColon=" + this.getAfterColon() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TernaryOperator withBeforeQuestionMark(final Boolean beforeQuestionMark) {
            return this.beforeQuestionMark == beforeQuestionMark ? this : new SpacesStyle.TernaryOperator(beforeQuestionMark, this.afterQuestionMark, this.beforeColon, this.afterColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TernaryOperator withAfterQuestionMark(final Boolean afterQuestionMark) {
            return this.afterQuestionMark == afterQuestionMark ? this : new SpacesStyle.TernaryOperator(this.beforeQuestionMark, afterQuestionMark, this.beforeColon, this.afterColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TernaryOperator withBeforeColon(final Boolean beforeColon) {
            return this.beforeColon == beforeColon ? this : new SpacesStyle.TernaryOperator(this.beforeQuestionMark, this.afterQuestionMark, beforeColon, this.afterColon);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TernaryOperator withAfterColon(final Boolean afterColon) {
            return this.afterColon == afterColon ? this : new SpacesStyle.TernaryOperator(this.beforeQuestionMark, this.afterQuestionMark, this.beforeColon, afterColon);
        }
    }


    public static final class TypeArguments {
        private final Boolean afterComma;
        private final Boolean beforeOpeningAngleBracket;
        private final Boolean afterClosingAngleBracket;

        public TypeArguments(final Boolean afterComma, final Boolean beforeOpeningAngleBracket, final Boolean afterClosingAngleBracket) {
            this.afterComma = afterComma;
            this.beforeOpeningAngleBracket = beforeOpeningAngleBracket;
            this.afterClosingAngleBracket = afterClosingAngleBracket;
        }

        public Boolean getAfterComma() {
            return this.afterComma;
        }

        public Boolean getBeforeOpeningAngleBracket() {
            return this.beforeOpeningAngleBracket;
        }

        public Boolean getAfterClosingAngleBracket() {
            return this.afterClosingAngleBracket;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.TypeArguments)) return false;
            final SpacesStyle.TypeArguments other = (SpacesStyle.TypeArguments) o;
            final Object this$afterComma = this.getAfterComma();
            final Object other$afterComma = other.getAfterComma();
            if (this$afterComma == null ? other$afterComma != null : !this$afterComma.equals(other$afterComma)) return false;
            final Object this$beforeOpeningAngleBracket = this.getBeforeOpeningAngleBracket();
            final Object other$beforeOpeningAngleBracket = other.getBeforeOpeningAngleBracket();
            if (this$beforeOpeningAngleBracket == null ? other$beforeOpeningAngleBracket != null : !this$beforeOpeningAngleBracket.equals(other$beforeOpeningAngleBracket)) return false;
            final Object this$afterClosingAngleBracket = this.getAfterClosingAngleBracket();
            final Object other$afterClosingAngleBracket = other.getAfterClosingAngleBracket();
            if (this$afterClosingAngleBracket == null ? other$afterClosingAngleBracket != null : !this$afterClosingAngleBracket.equals(other$afterClosingAngleBracket)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $afterComma = this.getAfterComma();
            result = result * PRIME + ($afterComma == null ? 43 : $afterComma.hashCode());
            final Object $beforeOpeningAngleBracket = this.getBeforeOpeningAngleBracket();
            result = result * PRIME + ($beforeOpeningAngleBracket == null ? 43 : $beforeOpeningAngleBracket.hashCode());
            final Object $afterClosingAngleBracket = this.getAfterClosingAngleBracket();
            result = result * PRIME + ($afterClosingAngleBracket == null ? 43 : $afterClosingAngleBracket.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.TypeArguments(afterComma=" + this.getAfterComma() + ", beforeOpeningAngleBracket=" + this.getBeforeOpeningAngleBracket() + ", afterClosingAngleBracket=" + this.getAfterClosingAngleBracket() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TypeArguments withAfterComma(final Boolean afterComma) {
            return this.afterComma == afterComma ? this : new SpacesStyle.TypeArguments(afterComma, this.beforeOpeningAngleBracket, this.afterClosingAngleBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TypeArguments withBeforeOpeningAngleBracket(final Boolean beforeOpeningAngleBracket) {
            return this.beforeOpeningAngleBracket == beforeOpeningAngleBracket ? this : new SpacesStyle.TypeArguments(this.afterComma, beforeOpeningAngleBracket, this.afterClosingAngleBracket);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TypeArguments withAfterClosingAngleBracket(final Boolean afterClosingAngleBracket) {
            return this.afterClosingAngleBracket == afterClosingAngleBracket ? this : new SpacesStyle.TypeArguments(this.afterComma, this.beforeOpeningAngleBracket, afterClosingAngleBracket);
        }
    }


    public static final class Other {
        private final Boolean beforeComma;
        private final Boolean afterComma;
        private final Boolean beforeForSemicolon;
        private final Boolean afterForSemicolon;
        private final Boolean afterTypeCast;
        private final Boolean beforeColonInForEach;
        private final Boolean insideOneLineEnumBraces;

        public Other(final Boolean beforeComma, final Boolean afterComma, final Boolean beforeForSemicolon, final Boolean afterForSemicolon, final Boolean afterTypeCast, final Boolean beforeColonInForEach, final Boolean insideOneLineEnumBraces) {
            this.beforeComma = beforeComma;
            this.afterComma = afterComma;
            this.beforeForSemicolon = beforeForSemicolon;
            this.afterForSemicolon = afterForSemicolon;
            this.afterTypeCast = afterTypeCast;
            this.beforeColonInForEach = beforeColonInForEach;
            this.insideOneLineEnumBraces = insideOneLineEnumBraces;
        }

        public Boolean getBeforeComma() {
            return this.beforeComma;
        }

        public Boolean getAfterComma() {
            return this.afterComma;
        }

        public Boolean getBeforeForSemicolon() {
            return this.beforeForSemicolon;
        }

        public Boolean getAfterForSemicolon() {
            return this.afterForSemicolon;
        }

        public Boolean getAfterTypeCast() {
            return this.afterTypeCast;
        }

        public Boolean getBeforeColonInForEach() {
            return this.beforeColonInForEach;
        }

        public Boolean getInsideOneLineEnumBraces() {
            return this.insideOneLineEnumBraces;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.Other)) return false;
            final SpacesStyle.Other other = (SpacesStyle.Other) o;
            final Object this$beforeComma = this.getBeforeComma();
            final Object other$beforeComma = other.getBeforeComma();
            if (this$beforeComma == null ? other$beforeComma != null : !this$beforeComma.equals(other$beforeComma)) return false;
            final Object this$afterComma = this.getAfterComma();
            final Object other$afterComma = other.getAfterComma();
            if (this$afterComma == null ? other$afterComma != null : !this$afterComma.equals(other$afterComma)) return false;
            final Object this$beforeForSemicolon = this.getBeforeForSemicolon();
            final Object other$beforeForSemicolon = other.getBeforeForSemicolon();
            if (this$beforeForSemicolon == null ? other$beforeForSemicolon != null : !this$beforeForSemicolon.equals(other$beforeForSemicolon)) return false;
            final Object this$afterForSemicolon = this.getAfterForSemicolon();
            final Object other$afterForSemicolon = other.getAfterForSemicolon();
            if (this$afterForSemicolon == null ? other$afterForSemicolon != null : !this$afterForSemicolon.equals(other$afterForSemicolon)) return false;
            final Object this$afterTypeCast = this.getAfterTypeCast();
            final Object other$afterTypeCast = other.getAfterTypeCast();
            if (this$afterTypeCast == null ? other$afterTypeCast != null : !this$afterTypeCast.equals(other$afterTypeCast)) return false;
            final Object this$beforeColonInForEach = this.getBeforeColonInForEach();
            final Object other$beforeColonInForEach = other.getBeforeColonInForEach();
            if (this$beforeColonInForEach == null ? other$beforeColonInForEach != null : !this$beforeColonInForEach.equals(other$beforeColonInForEach)) return false;
            final Object this$insideOneLineEnumBraces = this.getInsideOneLineEnumBraces();
            final Object other$insideOneLineEnumBraces = other.getInsideOneLineEnumBraces();
            if (this$insideOneLineEnumBraces == null ? other$insideOneLineEnumBraces != null : !this$insideOneLineEnumBraces.equals(other$insideOneLineEnumBraces)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $beforeComma = this.getBeforeComma();
            result = result * PRIME + ($beforeComma == null ? 43 : $beforeComma.hashCode());
            final Object $afterComma = this.getAfterComma();
            result = result * PRIME + ($afterComma == null ? 43 : $afterComma.hashCode());
            final Object $beforeForSemicolon = this.getBeforeForSemicolon();
            result = result * PRIME + ($beforeForSemicolon == null ? 43 : $beforeForSemicolon.hashCode());
            final Object $afterForSemicolon = this.getAfterForSemicolon();
            result = result * PRIME + ($afterForSemicolon == null ? 43 : $afterForSemicolon.hashCode());
            final Object $afterTypeCast = this.getAfterTypeCast();
            result = result * PRIME + ($afterTypeCast == null ? 43 : $afterTypeCast.hashCode());
            final Object $beforeColonInForEach = this.getBeforeColonInForEach();
            result = result * PRIME + ($beforeColonInForEach == null ? 43 : $beforeColonInForEach.hashCode());
            final Object $insideOneLineEnumBraces = this.getInsideOneLineEnumBraces();
            result = result * PRIME + ($insideOneLineEnumBraces == null ? 43 : $insideOneLineEnumBraces.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.Other(beforeComma=" + this.getBeforeComma() + ", afterComma=" + this.getAfterComma() + ", beforeForSemicolon=" + this.getBeforeForSemicolon() + ", afterForSemicolon=" + this.getAfterForSemicolon() + ", afterTypeCast=" + this.getAfterTypeCast() + ", beforeColonInForEach=" + this.getBeforeColonInForEach() + ", insideOneLineEnumBraces=" + this.getInsideOneLineEnumBraces() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withBeforeComma(final Boolean beforeComma) {
            return this.beforeComma == beforeComma ? this : new SpacesStyle.Other(beforeComma, this.afterComma, this.beforeForSemicolon, this.afterForSemicolon, this.afterTypeCast, this.beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withAfterComma(final Boolean afterComma) {
            return this.afterComma == afterComma ? this : new SpacesStyle.Other(this.beforeComma, afterComma, this.beforeForSemicolon, this.afterForSemicolon, this.afterTypeCast, this.beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withBeforeForSemicolon(final Boolean beforeForSemicolon) {
            return this.beforeForSemicolon == beforeForSemicolon ? this : new SpacesStyle.Other(this.beforeComma, this.afterComma, beforeForSemicolon, this.afterForSemicolon, this.afterTypeCast, this.beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withAfterForSemicolon(final Boolean afterForSemicolon) {
            return this.afterForSemicolon == afterForSemicolon ? this : new SpacesStyle.Other(this.beforeComma, this.afterComma, this.beforeForSemicolon, afterForSemicolon, this.afterTypeCast, this.beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withAfterTypeCast(final Boolean afterTypeCast) {
            return this.afterTypeCast == afterTypeCast ? this : new SpacesStyle.Other(this.beforeComma, this.afterComma, this.beforeForSemicolon, this.afterForSemicolon, afterTypeCast, this.beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withBeforeColonInForEach(final Boolean beforeColonInForEach) {
            return this.beforeColonInForEach == beforeColonInForEach ? this : new SpacesStyle.Other(this.beforeComma, this.afterComma, this.beforeForSemicolon, this.afterForSemicolon, this.afterTypeCast, beforeColonInForEach, this.insideOneLineEnumBraces);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.Other withInsideOneLineEnumBraces(final Boolean insideOneLineEnumBraces) {
            return this.insideOneLineEnumBraces == insideOneLineEnumBraces ? this : new SpacesStyle.Other(this.beforeComma, this.afterComma, this.beforeForSemicolon, this.afterForSemicolon, this.afterTypeCast, this.beforeColonInForEach, insideOneLineEnumBraces);
        }
    }


    public static final class TypeParameters {
        private final Boolean beforeOpeningAngleBracket;
        private final Boolean aroundTypeBounds;

        public TypeParameters(final Boolean beforeOpeningAngleBracket, final Boolean aroundTypeBounds) {
            this.beforeOpeningAngleBracket = beforeOpeningAngleBracket;
            this.aroundTypeBounds = aroundTypeBounds;
        }

        public Boolean getBeforeOpeningAngleBracket() {
            return this.beforeOpeningAngleBracket;
        }

        public Boolean getAroundTypeBounds() {
            return this.aroundTypeBounds;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof SpacesStyle.TypeParameters)) return false;
            final SpacesStyle.TypeParameters other = (SpacesStyle.TypeParameters) o;
            final Object this$beforeOpeningAngleBracket = this.getBeforeOpeningAngleBracket();
            final Object other$beforeOpeningAngleBracket = other.getBeforeOpeningAngleBracket();
            if (this$beforeOpeningAngleBracket == null ? other$beforeOpeningAngleBracket != null : !this$beforeOpeningAngleBracket.equals(other$beforeOpeningAngleBracket)) return false;
            final Object this$aroundTypeBounds = this.getAroundTypeBounds();
            final Object other$aroundTypeBounds = other.getAroundTypeBounds();
            if (this$aroundTypeBounds == null ? other$aroundTypeBounds != null : !this$aroundTypeBounds.equals(other$aroundTypeBounds)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $beforeOpeningAngleBracket = this.getBeforeOpeningAngleBracket();
            result = result * PRIME + ($beforeOpeningAngleBracket == null ? 43 : $beforeOpeningAngleBracket.hashCode());
            final Object $aroundTypeBounds = this.getAroundTypeBounds();
            result = result * PRIME + ($aroundTypeBounds == null ? 43 : $aroundTypeBounds.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "SpacesStyle.TypeParameters(beforeOpeningAngleBracket=" + this.getBeforeOpeningAngleBracket() + ", aroundTypeBounds=" + this.getAroundTypeBounds() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TypeParameters withBeforeOpeningAngleBracket(final Boolean beforeOpeningAngleBracket) {
            return this.beforeOpeningAngleBracket == beforeOpeningAngleBracket ? this : new SpacesStyle.TypeParameters(beforeOpeningAngleBracket, this.aroundTypeBounds);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public SpacesStyle.TypeParameters withAroundTypeBounds(final Boolean aroundTypeBounds) {
            return this.aroundTypeBounds == aroundTypeBounds ? this : new SpacesStyle.TypeParameters(this.beforeOpeningAngleBracket, aroundTypeBounds);
        }
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.spaces(), this);
    }

    public SpacesStyle(final BeforeParentheses beforeParentheses, final AroundOperators aroundOperators, final BeforeLeftBrace beforeLeftBrace, final BeforeKeywords beforeKeywords, final Within within, final TernaryOperator ternaryOperator, final TypeArguments typeArguments, final Other other, final TypeParameters typeParameters) {
        this.beforeParentheses = beforeParentheses;
        this.aroundOperators = aroundOperators;
        this.beforeLeftBrace = beforeLeftBrace;
        this.beforeKeywords = beforeKeywords;
        this.within = within;
        this.ternaryOperator = ternaryOperator;
        this.typeArguments = typeArguments;
        this.other = other;
        this.typeParameters = typeParameters;
    }

    public BeforeParentheses getBeforeParentheses() {
        return this.beforeParentheses;
    }

    public AroundOperators getAroundOperators() {
        return this.aroundOperators;
    }

    public BeforeLeftBrace getBeforeLeftBrace() {
        return this.beforeLeftBrace;
    }

    public BeforeKeywords getBeforeKeywords() {
        return this.beforeKeywords;
    }

    public Within getWithin() {
        return this.within;
    }

    public TernaryOperator getTernaryOperator() {
        return this.ternaryOperator;
    }

    public TypeArguments getTypeArguments() {
        return this.typeArguments;
    }

    public Other getOther() {
        return this.other;
    }

    public TypeParameters getTypeParameters() {
        return this.typeParameters;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof SpacesStyle)) return false;
        final SpacesStyle other = (SpacesStyle) o;
        final Object this$beforeParentheses = this.getBeforeParentheses();
        final Object other$beforeParentheses = other.getBeforeParentheses();
        if (this$beforeParentheses == null ? other$beforeParentheses != null : !this$beforeParentheses.equals(other$beforeParentheses)) return false;
        final Object this$aroundOperators = this.getAroundOperators();
        final Object other$aroundOperators = other.getAroundOperators();
        if (this$aroundOperators == null ? other$aroundOperators != null : !this$aroundOperators.equals(other$aroundOperators)) return false;
        final Object this$beforeLeftBrace = this.getBeforeLeftBrace();
        final Object other$beforeLeftBrace = other.getBeforeLeftBrace();
        if (this$beforeLeftBrace == null ? other$beforeLeftBrace != null : !this$beforeLeftBrace.equals(other$beforeLeftBrace)) return false;
        final Object this$beforeKeywords = this.getBeforeKeywords();
        final Object other$beforeKeywords = other.getBeforeKeywords();
        if (this$beforeKeywords == null ? other$beforeKeywords != null : !this$beforeKeywords.equals(other$beforeKeywords)) return false;
        final Object this$within = this.getWithin();
        final Object other$within = other.getWithin();
        if (this$within == null ? other$within != null : !this$within.equals(other$within)) return false;
        final Object this$ternaryOperator = this.getTernaryOperator();
        final Object other$ternaryOperator = other.getTernaryOperator();
        if (this$ternaryOperator == null ? other$ternaryOperator != null : !this$ternaryOperator.equals(other$ternaryOperator)) return false;
        final Object this$typeArguments = this.getTypeArguments();
        final Object other$typeArguments = other.getTypeArguments();
        if (this$typeArguments == null ? other$typeArguments != null : !this$typeArguments.equals(other$typeArguments)) return false;
        final Object this$other = this.getOther();
        final Object other$other = other.getOther();
        if (this$other == null ? other$other != null : !this$other.equals(other$other)) return false;
        final Object this$typeParameters = this.getTypeParameters();
        final Object other$typeParameters = other.getTypeParameters();
        if (this$typeParameters == null ? other$typeParameters != null : !this$typeParameters.equals(other$typeParameters)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $beforeParentheses = this.getBeforeParentheses();
        result = result * PRIME + ($beforeParentheses == null ? 43 : $beforeParentheses.hashCode());
        final Object $aroundOperators = this.getAroundOperators();
        result = result * PRIME + ($aroundOperators == null ? 43 : $aroundOperators.hashCode());
        final Object $beforeLeftBrace = this.getBeforeLeftBrace();
        result = result * PRIME + ($beforeLeftBrace == null ? 43 : $beforeLeftBrace.hashCode());
        final Object $beforeKeywords = this.getBeforeKeywords();
        result = result * PRIME + ($beforeKeywords == null ? 43 : $beforeKeywords.hashCode());
        final Object $within = this.getWithin();
        result = result * PRIME + ($within == null ? 43 : $within.hashCode());
        final Object $ternaryOperator = this.getTernaryOperator();
        result = result * PRIME + ($ternaryOperator == null ? 43 : $ternaryOperator.hashCode());
        final Object $typeArguments = this.getTypeArguments();
        result = result * PRIME + ($typeArguments == null ? 43 : $typeArguments.hashCode());
        final Object $other = this.getOther();
        result = result * PRIME + ($other == null ? 43 : $other.hashCode());
        final Object $typeParameters = this.getTypeParameters();
        result = result * PRIME + ($typeParameters == null ? 43 : $typeParameters.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "SpacesStyle(beforeParentheses=" + this.getBeforeParentheses() + ", aroundOperators=" + this.getAroundOperators() + ", beforeLeftBrace=" + this.getBeforeLeftBrace() + ", beforeKeywords=" + this.getBeforeKeywords() + ", within=" + this.getWithin() + ", ternaryOperator=" + this.getTernaryOperator() + ", typeArguments=" + this.getTypeArguments() + ", other=" + this.getOther() + ", typeParameters=" + this.getTypeParameters() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withBeforeParentheses(final BeforeParentheses beforeParentheses) {
        return this.beforeParentheses == beforeParentheses ? this : new SpacesStyle(beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withAroundOperators(final AroundOperators aroundOperators) {
        return this.aroundOperators == aroundOperators ? this : new SpacesStyle(this.beforeParentheses, aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withBeforeLeftBrace(final BeforeLeftBrace beforeLeftBrace) {
        return this.beforeLeftBrace == beforeLeftBrace ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withBeforeKeywords(final BeforeKeywords beforeKeywords) {
        return this.beforeKeywords == beforeKeywords ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withWithin(final Within within) {
        return this.within == within ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, within, this.ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withTernaryOperator(final TernaryOperator ternaryOperator) {
        return this.ternaryOperator == ternaryOperator ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, ternaryOperator, this.typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withTypeArguments(final TypeArguments typeArguments) {
        return this.typeArguments == typeArguments ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, typeArguments, this.other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withOther(final Other other) {
        return this.other == other ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, other, this.typeParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public SpacesStyle withTypeParameters(final TypeParameters typeParameters) {
        return this.typeParameters == typeParameters ? this : new SpacesStyle(this.beforeParentheses, this.aroundOperators, this.beforeLeftBrace, this.beforeKeywords, this.within, this.ternaryOperator, this.typeArguments, this.other, typeParameters);
    }
}
