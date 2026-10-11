// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/TabsAndIndentsStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class TabsAndIndentsStyle implements JavaStyle {
    private final Boolean useTabCharacter;
    private final Integer tabSize;
    private final Integer indentSize;
    private final Integer continuationIndent;
    private final Boolean indentsRelativeToExpressionStart;
    private final MethodDeclarationParameters methodDeclarationParameters;


    public static final class MethodDeclarationParameters {
        private final Boolean alignWhenMultiple;

        public MethodDeclarationParameters(final Boolean alignWhenMultiple) {
            this.alignWhenMultiple = alignWhenMultiple;
        }

        public Boolean getAlignWhenMultiple() {
            return this.alignWhenMultiple;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof TabsAndIndentsStyle.MethodDeclarationParameters)) return false;
            final TabsAndIndentsStyle.MethodDeclarationParameters other = (TabsAndIndentsStyle.MethodDeclarationParameters) o;
            final Object this$alignWhenMultiple = this.getAlignWhenMultiple();
            final Object other$alignWhenMultiple = other.getAlignWhenMultiple();
            if (this$alignWhenMultiple == null ? other$alignWhenMultiple != null : !this$alignWhenMultiple.equals(other$alignWhenMultiple)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $alignWhenMultiple = this.getAlignWhenMultiple();
            result = result * PRIME + ($alignWhenMultiple == null ? 43 : $alignWhenMultiple.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "TabsAndIndentsStyle.MethodDeclarationParameters(alignWhenMultiple=" + this.getAlignWhenMultiple() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public TabsAndIndentsStyle.MethodDeclarationParameters withAlignWhenMultiple(final Boolean alignWhenMultiple) {
            return this.alignWhenMultiple == alignWhenMultiple ? this : new TabsAndIndentsStyle.MethodDeclarationParameters(alignWhenMultiple);
        }
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(IntelliJ.tabsAndIndents(), this);
    }

    public TabsAndIndentsStyle(final Boolean useTabCharacter, final Integer tabSize, final Integer indentSize, final Integer continuationIndent, final Boolean indentsRelativeToExpressionStart, final MethodDeclarationParameters methodDeclarationParameters) {
        this.useTabCharacter = useTabCharacter;
        this.tabSize = tabSize;
        this.indentSize = indentSize;
        this.continuationIndent = continuationIndent;
        this.indentsRelativeToExpressionStart = indentsRelativeToExpressionStart;
        this.methodDeclarationParameters = methodDeclarationParameters;
    }

    public Boolean getUseTabCharacter() {
        return this.useTabCharacter;
    }

    public Integer getTabSize() {
        return this.tabSize;
    }

    public Integer getIndentSize() {
        return this.indentSize;
    }

    public Integer getContinuationIndent() {
        return this.continuationIndent;
    }

    public Boolean getIndentsRelativeToExpressionStart() {
        return this.indentsRelativeToExpressionStart;
    }

    public MethodDeclarationParameters getMethodDeclarationParameters() {
        return this.methodDeclarationParameters;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TabsAndIndentsStyle)) return false;
        final TabsAndIndentsStyle other = (TabsAndIndentsStyle) o;
        final Object this$useTabCharacter = this.getUseTabCharacter();
        final Object other$useTabCharacter = other.getUseTabCharacter();
        if (this$useTabCharacter == null ? other$useTabCharacter != null : !this$useTabCharacter.equals(other$useTabCharacter)) return false;
        final Object this$tabSize = this.getTabSize();
        final Object other$tabSize = other.getTabSize();
        if (this$tabSize == null ? other$tabSize != null : !this$tabSize.equals(other$tabSize)) return false;
        final Object this$indentSize = this.getIndentSize();
        final Object other$indentSize = other.getIndentSize();
        if (this$indentSize == null ? other$indentSize != null : !this$indentSize.equals(other$indentSize)) return false;
        final Object this$continuationIndent = this.getContinuationIndent();
        final Object other$continuationIndent = other.getContinuationIndent();
        if (this$continuationIndent == null ? other$continuationIndent != null : !this$continuationIndent.equals(other$continuationIndent)) return false;
        final Object this$indentsRelativeToExpressionStart = this.getIndentsRelativeToExpressionStart();
        final Object other$indentsRelativeToExpressionStart = other.getIndentsRelativeToExpressionStart();
        if (this$indentsRelativeToExpressionStart == null ? other$indentsRelativeToExpressionStart != null : !this$indentsRelativeToExpressionStart.equals(other$indentsRelativeToExpressionStart)) return false;
        final Object this$methodDeclarationParameters = this.getMethodDeclarationParameters();
        final Object other$methodDeclarationParameters = other.getMethodDeclarationParameters();
        if (this$methodDeclarationParameters == null ? other$methodDeclarationParameters != null : !this$methodDeclarationParameters.equals(other$methodDeclarationParameters)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $useTabCharacter = this.getUseTabCharacter();
        result = result * PRIME + ($useTabCharacter == null ? 43 : $useTabCharacter.hashCode());
        final Object $tabSize = this.getTabSize();
        result = result * PRIME + ($tabSize == null ? 43 : $tabSize.hashCode());
        final Object $indentSize = this.getIndentSize();
        result = result * PRIME + ($indentSize == null ? 43 : $indentSize.hashCode());
        final Object $continuationIndent = this.getContinuationIndent();
        result = result * PRIME + ($continuationIndent == null ? 43 : $continuationIndent.hashCode());
        final Object $indentsRelativeToExpressionStart = this.getIndentsRelativeToExpressionStart();
        result = result * PRIME + ($indentsRelativeToExpressionStart == null ? 43 : $indentsRelativeToExpressionStart.hashCode());
        final Object $methodDeclarationParameters = this.getMethodDeclarationParameters();
        result = result * PRIME + ($methodDeclarationParameters == null ? 43 : $methodDeclarationParameters.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TabsAndIndentsStyle(useTabCharacter=" + this.getUseTabCharacter() + ", tabSize=" + this.getTabSize() + ", indentSize=" + this.getIndentSize() + ", continuationIndent=" + this.getContinuationIndent() + ", indentsRelativeToExpressionStart=" + this.getIndentsRelativeToExpressionStart() + ", methodDeclarationParameters=" + this.getMethodDeclarationParameters() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withUseTabCharacter(final Boolean useTabCharacter) {
        return this.useTabCharacter == useTabCharacter ? this : new TabsAndIndentsStyle(useTabCharacter, this.tabSize, this.indentSize, this.continuationIndent, this.indentsRelativeToExpressionStart, this.methodDeclarationParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withTabSize(final Integer tabSize) {
        return this.tabSize == tabSize ? this : new TabsAndIndentsStyle(this.useTabCharacter, tabSize, this.indentSize, this.continuationIndent, this.indentsRelativeToExpressionStart, this.methodDeclarationParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withIndentSize(final Integer indentSize) {
        return this.indentSize == indentSize ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, indentSize, this.continuationIndent, this.indentsRelativeToExpressionStart, this.methodDeclarationParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withContinuationIndent(final Integer continuationIndent) {
        return this.continuationIndent == continuationIndent ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, this.indentSize, continuationIndent, this.indentsRelativeToExpressionStart, this.methodDeclarationParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withIndentsRelativeToExpressionStart(final Boolean indentsRelativeToExpressionStart) {
        return this.indentsRelativeToExpressionStart == indentsRelativeToExpressionStart ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, this.indentSize, this.continuationIndent, indentsRelativeToExpressionStart, this.methodDeclarationParameters);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TabsAndIndentsStyle withMethodDeclarationParameters(final MethodDeclarationParameters methodDeclarationParameters) {
        return this.methodDeclarationParameters == methodDeclarationParameters ? this : new TabsAndIndentsStyle(this.useTabCharacter, this.tabSize, this.indentSize, this.continuationIndent, this.indentsRelativeToExpressionStart, methodDeclarationParameters);
    }
}
