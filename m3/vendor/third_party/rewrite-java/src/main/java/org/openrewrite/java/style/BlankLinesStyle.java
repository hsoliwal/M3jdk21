// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/BlankLinesStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class BlankLinesStyle implements JavaStyle {
    private final KeepMaximum keepMaximum;
    private final Minimum minimum;


    public static final class KeepMaximum {
        private final Integer inDeclarations;
        private final Integer inCode;
        private final Integer beforeEndOfBlock;
        private final Integer betweenHeaderAndPackage;

        public KeepMaximum(final Integer inDeclarations, final Integer inCode, final Integer beforeEndOfBlock, final Integer betweenHeaderAndPackage) {
            this.inDeclarations = inDeclarations;
            this.inCode = inCode;
            this.beforeEndOfBlock = beforeEndOfBlock;
            this.betweenHeaderAndPackage = betweenHeaderAndPackage;
        }

        public Integer getInDeclarations() {
            return this.inDeclarations;
        }

        public Integer getInCode() {
            return this.inCode;
        }

        public Integer getBeforeEndOfBlock() {
            return this.beforeEndOfBlock;
        }

        public Integer getBetweenHeaderAndPackage() {
            return this.betweenHeaderAndPackage;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof BlankLinesStyle.KeepMaximum)) return false;
            final BlankLinesStyle.KeepMaximum other = (BlankLinesStyle.KeepMaximum) o;
            final Object this$inDeclarations = this.getInDeclarations();
            final Object other$inDeclarations = other.getInDeclarations();
            if (this$inDeclarations == null ? other$inDeclarations != null : !this$inDeclarations.equals(other$inDeclarations)) return false;
            final Object this$inCode = this.getInCode();
            final Object other$inCode = other.getInCode();
            if (this$inCode == null ? other$inCode != null : !this$inCode.equals(other$inCode)) return false;
            final Object this$beforeEndOfBlock = this.getBeforeEndOfBlock();
            final Object other$beforeEndOfBlock = other.getBeforeEndOfBlock();
            if (this$beforeEndOfBlock == null ? other$beforeEndOfBlock != null : !this$beforeEndOfBlock.equals(other$beforeEndOfBlock)) return false;
            final Object this$betweenHeaderAndPackage = this.getBetweenHeaderAndPackage();
            final Object other$betweenHeaderAndPackage = other.getBetweenHeaderAndPackage();
            if (this$betweenHeaderAndPackage == null ? other$betweenHeaderAndPackage != null : !this$betweenHeaderAndPackage.equals(other$betweenHeaderAndPackage)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $inDeclarations = this.getInDeclarations();
            result = result * PRIME + ($inDeclarations == null ? 43 : $inDeclarations.hashCode());
            final Object $inCode = this.getInCode();
            result = result * PRIME + ($inCode == null ? 43 : $inCode.hashCode());
            final Object $beforeEndOfBlock = this.getBeforeEndOfBlock();
            result = result * PRIME + ($beforeEndOfBlock == null ? 43 : $beforeEndOfBlock.hashCode());
            final Object $betweenHeaderAndPackage = this.getBetweenHeaderAndPackage();
            result = result * PRIME + ($betweenHeaderAndPackage == null ? 43 : $betweenHeaderAndPackage.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "BlankLinesStyle.KeepMaximum(inDeclarations=" + this.getInDeclarations() + ", inCode=" + this.getInCode() + ", beforeEndOfBlock=" + this.getBeforeEndOfBlock() + ", betweenHeaderAndPackage=" + this.getBetweenHeaderAndPackage() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.KeepMaximum withInDeclarations(final Integer inDeclarations) {
            return this.inDeclarations == inDeclarations ? this : new BlankLinesStyle.KeepMaximum(inDeclarations, this.inCode, this.beforeEndOfBlock, this.betweenHeaderAndPackage);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.KeepMaximum withInCode(final Integer inCode) {
            return this.inCode == inCode ? this : new BlankLinesStyle.KeepMaximum(this.inDeclarations, inCode, this.beforeEndOfBlock, this.betweenHeaderAndPackage);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.KeepMaximum withBeforeEndOfBlock(final Integer beforeEndOfBlock) {
            return this.beforeEndOfBlock == beforeEndOfBlock ? this : new BlankLinesStyle.KeepMaximum(this.inDeclarations, this.inCode, beforeEndOfBlock, this.betweenHeaderAndPackage);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.KeepMaximum withBetweenHeaderAndPackage(final Integer betweenHeaderAndPackage) {
            return this.betweenHeaderAndPackage == betweenHeaderAndPackage ? this : new BlankLinesStyle.KeepMaximum(this.inDeclarations, this.inCode, this.beforeEndOfBlock, betweenHeaderAndPackage);
        }
    }


    public static final class Minimum {
        private final Integer beforePackage;
        private final Integer afterPackage;
        private final Integer beforeImports;
        private final Integer afterImports;
        private final Integer aroundClass;
        private final Integer afterClassHeader;
        private final Integer beforeClassEnd;
        private final Integer afterAnonymousClassHeader;
        private final Integer aroundFieldInInterface;
        private final Integer aroundField;
        private final Integer aroundMethodInInterface;
        private final Integer aroundMethod;
        private final Integer beforeMethodBody;
        private final Integer aroundInitializer;

        public Minimum(final Integer beforePackage, final Integer afterPackage, final Integer beforeImports, final Integer afterImports, final Integer aroundClass, final Integer afterClassHeader, final Integer beforeClassEnd, final Integer afterAnonymousClassHeader, final Integer aroundFieldInInterface, final Integer aroundField, final Integer aroundMethodInInterface, final Integer aroundMethod, final Integer beforeMethodBody, final Integer aroundInitializer) {
            this.beforePackage = beforePackage;
            this.afterPackage = afterPackage;
            this.beforeImports = beforeImports;
            this.afterImports = afterImports;
            this.aroundClass = aroundClass;
            this.afterClassHeader = afterClassHeader;
            this.beforeClassEnd = beforeClassEnd;
            this.afterAnonymousClassHeader = afterAnonymousClassHeader;
            this.aroundFieldInInterface = aroundFieldInInterface;
            this.aroundField = aroundField;
            this.aroundMethodInInterface = aroundMethodInInterface;
            this.aroundMethod = aroundMethod;
            this.beforeMethodBody = beforeMethodBody;
            this.aroundInitializer = aroundInitializer;
        }

        public Integer getBeforePackage() {
            return this.beforePackage;
        }

        public Integer getAfterPackage() {
            return this.afterPackage;
        }

        public Integer getBeforeImports() {
            return this.beforeImports;
        }

        public Integer getAfterImports() {
            return this.afterImports;
        }

        public Integer getAroundClass() {
            return this.aroundClass;
        }

        public Integer getAfterClassHeader() {
            return this.afterClassHeader;
        }

        public Integer getBeforeClassEnd() {
            return this.beforeClassEnd;
        }

        public Integer getAfterAnonymousClassHeader() {
            return this.afterAnonymousClassHeader;
        }

        public Integer getAroundFieldInInterface() {
            return this.aroundFieldInInterface;
        }

        public Integer getAroundField() {
            return this.aroundField;
        }

        public Integer getAroundMethodInInterface() {
            return this.aroundMethodInInterface;
        }

        public Integer getAroundMethod() {
            return this.aroundMethod;
        }

        public Integer getBeforeMethodBody() {
            return this.beforeMethodBody;
        }

        public Integer getAroundInitializer() {
            return this.aroundInitializer;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof BlankLinesStyle.Minimum)) return false;
            final BlankLinesStyle.Minimum other = (BlankLinesStyle.Minimum) o;
            final Object this$beforePackage = this.getBeforePackage();
            final Object other$beforePackage = other.getBeforePackage();
            if (this$beforePackage == null ? other$beforePackage != null : !this$beforePackage.equals(other$beforePackage)) return false;
            final Object this$afterPackage = this.getAfterPackage();
            final Object other$afterPackage = other.getAfterPackage();
            if (this$afterPackage == null ? other$afterPackage != null : !this$afterPackage.equals(other$afterPackage)) return false;
            final Object this$beforeImports = this.getBeforeImports();
            final Object other$beforeImports = other.getBeforeImports();
            if (this$beforeImports == null ? other$beforeImports != null : !this$beforeImports.equals(other$beforeImports)) return false;
            final Object this$afterImports = this.getAfterImports();
            final Object other$afterImports = other.getAfterImports();
            if (this$afterImports == null ? other$afterImports != null : !this$afterImports.equals(other$afterImports)) return false;
            final Object this$aroundClass = this.getAroundClass();
            final Object other$aroundClass = other.getAroundClass();
            if (this$aroundClass == null ? other$aroundClass != null : !this$aroundClass.equals(other$aroundClass)) return false;
            final Object this$afterClassHeader = this.getAfterClassHeader();
            final Object other$afterClassHeader = other.getAfterClassHeader();
            if (this$afterClassHeader == null ? other$afterClassHeader != null : !this$afterClassHeader.equals(other$afterClassHeader)) return false;
            final Object this$beforeClassEnd = this.getBeforeClassEnd();
            final Object other$beforeClassEnd = other.getBeforeClassEnd();
            if (this$beforeClassEnd == null ? other$beforeClassEnd != null : !this$beforeClassEnd.equals(other$beforeClassEnd)) return false;
            final Object this$afterAnonymousClassHeader = this.getAfterAnonymousClassHeader();
            final Object other$afterAnonymousClassHeader = other.getAfterAnonymousClassHeader();
            if (this$afterAnonymousClassHeader == null ? other$afterAnonymousClassHeader != null : !this$afterAnonymousClassHeader.equals(other$afterAnonymousClassHeader)) return false;
            final Object this$aroundFieldInInterface = this.getAroundFieldInInterface();
            final Object other$aroundFieldInInterface = other.getAroundFieldInInterface();
            if (this$aroundFieldInInterface == null ? other$aroundFieldInInterface != null : !this$aroundFieldInInterface.equals(other$aroundFieldInInterface)) return false;
            final Object this$aroundField = this.getAroundField();
            final Object other$aroundField = other.getAroundField();
            if (this$aroundField == null ? other$aroundField != null : !this$aroundField.equals(other$aroundField)) return false;
            final Object this$aroundMethodInInterface = this.getAroundMethodInInterface();
            final Object other$aroundMethodInInterface = other.getAroundMethodInInterface();
            if (this$aroundMethodInInterface == null ? other$aroundMethodInInterface != null : !this$aroundMethodInInterface.equals(other$aroundMethodInInterface)) return false;
            final Object this$aroundMethod = this.getAroundMethod();
            final Object other$aroundMethod = other.getAroundMethod();
            if (this$aroundMethod == null ? other$aroundMethod != null : !this$aroundMethod.equals(other$aroundMethod)) return false;
            final Object this$beforeMethodBody = this.getBeforeMethodBody();
            final Object other$beforeMethodBody = other.getBeforeMethodBody();
            if (this$beforeMethodBody == null ? other$beforeMethodBody != null : !this$beforeMethodBody.equals(other$beforeMethodBody)) return false;
            final Object this$aroundInitializer = this.getAroundInitializer();
            final Object other$aroundInitializer = other.getAroundInitializer();
            if (this$aroundInitializer == null ? other$aroundInitializer != null : !this$aroundInitializer.equals(other$aroundInitializer)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $beforePackage = this.getBeforePackage();
            result = result * PRIME + ($beforePackage == null ? 43 : $beforePackage.hashCode());
            final Object $afterPackage = this.getAfterPackage();
            result = result * PRIME + ($afterPackage == null ? 43 : $afterPackage.hashCode());
            final Object $beforeImports = this.getBeforeImports();
            result = result * PRIME + ($beforeImports == null ? 43 : $beforeImports.hashCode());
            final Object $afterImports = this.getAfterImports();
            result = result * PRIME + ($afterImports == null ? 43 : $afterImports.hashCode());
            final Object $aroundClass = this.getAroundClass();
            result = result * PRIME + ($aroundClass == null ? 43 : $aroundClass.hashCode());
            final Object $afterClassHeader = this.getAfterClassHeader();
            result = result * PRIME + ($afterClassHeader == null ? 43 : $afterClassHeader.hashCode());
            final Object $beforeClassEnd = this.getBeforeClassEnd();
            result = result * PRIME + ($beforeClassEnd == null ? 43 : $beforeClassEnd.hashCode());
            final Object $afterAnonymousClassHeader = this.getAfterAnonymousClassHeader();
            result = result * PRIME + ($afterAnonymousClassHeader == null ? 43 : $afterAnonymousClassHeader.hashCode());
            final Object $aroundFieldInInterface = this.getAroundFieldInInterface();
            result = result * PRIME + ($aroundFieldInInterface == null ? 43 : $aroundFieldInInterface.hashCode());
            final Object $aroundField = this.getAroundField();
            result = result * PRIME + ($aroundField == null ? 43 : $aroundField.hashCode());
            final Object $aroundMethodInInterface = this.getAroundMethodInInterface();
            result = result * PRIME + ($aroundMethodInInterface == null ? 43 : $aroundMethodInInterface.hashCode());
            final Object $aroundMethod = this.getAroundMethod();
            result = result * PRIME + ($aroundMethod == null ? 43 : $aroundMethod.hashCode());
            final Object $beforeMethodBody = this.getBeforeMethodBody();
            result = result * PRIME + ($beforeMethodBody == null ? 43 : $beforeMethodBody.hashCode());
            final Object $aroundInitializer = this.getAroundInitializer();
            result = result * PRIME + ($aroundInitializer == null ? 43 : $aroundInitializer.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "BlankLinesStyle.Minimum(beforePackage=" + this.getBeforePackage() + ", afterPackage=" + this.getAfterPackage() + ", beforeImports=" + this.getBeforeImports() + ", afterImports=" + this.getAfterImports() + ", aroundClass=" + this.getAroundClass() + ", afterClassHeader=" + this.getAfterClassHeader() + ", beforeClassEnd=" + this.getBeforeClassEnd() + ", afterAnonymousClassHeader=" + this.getAfterAnonymousClassHeader() + ", aroundFieldInInterface=" + this.getAroundFieldInInterface() + ", aroundField=" + this.getAroundField() + ", aroundMethodInInterface=" + this.getAroundMethodInInterface() + ", aroundMethod=" + this.getAroundMethod() + ", beforeMethodBody=" + this.getBeforeMethodBody() + ", aroundInitializer=" + this.getAroundInitializer() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withBeforePackage(final Integer beforePackage) {
            return this.beforePackage == beforePackage ? this : new BlankLinesStyle.Minimum(beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAfterPackage(final Integer afterPackage) {
            return this.afterPackage == afterPackage ? this : new BlankLinesStyle.Minimum(this.beforePackage, afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withBeforeImports(final Integer beforeImports) {
            return this.beforeImports == beforeImports ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAfterImports(final Integer afterImports) {
            return this.afterImports == afterImports ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundClass(final Integer aroundClass) {
            return this.aroundClass == aroundClass ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAfterClassHeader(final Integer afterClassHeader) {
            return this.afterClassHeader == afterClassHeader ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withBeforeClassEnd(final Integer beforeClassEnd) {
            return this.beforeClassEnd == beforeClassEnd ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAfterAnonymousClassHeader(final Integer afterAnonymousClassHeader) {
            return this.afterAnonymousClassHeader == afterAnonymousClassHeader ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundFieldInInterface(final Integer aroundFieldInInterface) {
            return this.aroundFieldInInterface == aroundFieldInInterface ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundField(final Integer aroundField) {
            return this.aroundField == aroundField ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundMethodInInterface(final Integer aroundMethodInInterface) {
            return this.aroundMethodInInterface == aroundMethodInInterface ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundMethod(final Integer aroundMethod) {
            return this.aroundMethod == aroundMethod ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, aroundMethod, this.beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withBeforeMethodBody(final Integer beforeMethodBody) {
            return this.beforeMethodBody == beforeMethodBody ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, beforeMethodBody, this.aroundInitializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public BlankLinesStyle.Minimum withAroundInitializer(final Integer aroundInitializer) {
            return this.aroundInitializer == aroundInitializer ? this : new BlankLinesStyle.Minimum(this.beforePackage, this.afterPackage, this.beforeImports, this.afterImports, this.aroundClass, this.afterClassHeader, this.beforeClassEnd, this.afterAnonymousClassHeader, this.aroundFieldInInterface, this.aroundField, this.aroundMethodInInterface, this.aroundMethod, this.beforeMethodBody, aroundInitializer);
        }
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.blankLines(), this);
    }

    public BlankLinesStyle(final KeepMaximum keepMaximum, final Minimum minimum) {
        this.keepMaximum = keepMaximum;
        this.minimum = minimum;
    }

    public KeepMaximum getKeepMaximum() {
        return this.keepMaximum;
    }

    public Minimum getMinimum() {
        return this.minimum;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof BlankLinesStyle)) return false;
        final BlankLinesStyle other = (BlankLinesStyle) o;
        final Object this$keepMaximum = this.getKeepMaximum();
        final Object other$keepMaximum = other.getKeepMaximum();
        if (this$keepMaximum == null ? other$keepMaximum != null : !this$keepMaximum.equals(other$keepMaximum)) return false;
        final Object this$minimum = this.getMinimum();
        final Object other$minimum = other.getMinimum();
        if (this$minimum == null ? other$minimum != null : !this$minimum.equals(other$minimum)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $keepMaximum = this.getKeepMaximum();
        result = result * PRIME + ($keepMaximum == null ? 43 : $keepMaximum.hashCode());
        final Object $minimum = this.getMinimum();
        result = result * PRIME + ($minimum == null ? 43 : $minimum.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "BlankLinesStyle(keepMaximum=" + this.getKeepMaximum() + ", minimum=" + this.getMinimum() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BlankLinesStyle withKeepMaximum(final KeepMaximum keepMaximum) {
        return this.keepMaximum == keepMaximum ? this : new BlankLinesStyle(keepMaximum, this.minimum);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public BlankLinesStyle withMinimum(final Minimum minimum) {
        return this.minimum == minimum ? this : new BlankLinesStyle(this.keepMaximum, minimum);
    }
}
