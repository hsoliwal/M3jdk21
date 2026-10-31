// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/WrappingAndBracesStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class WrappingAndBracesStyle implements JavaStyle {
    private final IfStatement ifStatement;

    public IfStatement getIfStatement() {
        //noinspection ConstantConditions
        return ifStatement == null ? new IfStatement(false) : ifStatement;
    }


    public static final class IfStatement {
        private final Boolean elseOnNewLine;

        public IfStatement(final Boolean elseOnNewLine) {
            this.elseOnNewLine = elseOnNewLine;
        }

        public Boolean getElseOnNewLine() {
            return this.elseOnNewLine;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof WrappingAndBracesStyle.IfStatement)) return false;
            final WrappingAndBracesStyle.IfStatement other = (WrappingAndBracesStyle.IfStatement) o;
            final Object this$elseOnNewLine = this.getElseOnNewLine();
            final Object other$elseOnNewLine = other.getElseOnNewLine();
            if (this$elseOnNewLine == null ? other$elseOnNewLine != null : !this$elseOnNewLine.equals(other$elseOnNewLine)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $elseOnNewLine = this.getElseOnNewLine();
            result = result * PRIME + ($elseOnNewLine == null ? 43 : $elseOnNewLine.hashCode());
            return result;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "WrappingAndBracesStyle.IfStatement(elseOnNewLine=" + this.getElseOnNewLine() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public WrappingAndBracesStyle.IfStatement withElseOnNewLine(final Boolean elseOnNewLine) {
            return this.elseOnNewLine == elseOnNewLine ? this : new WrappingAndBracesStyle.IfStatement(elseOnNewLine);
        }
    }

    public WrappingAndBracesStyle(final IfStatement ifStatement) {
        this.ifStatement = ifStatement;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof WrappingAndBracesStyle)) return false;
        final WrappingAndBracesStyle other = (WrappingAndBracesStyle) o;
        final Object this$ifStatement = this.getIfStatement();
        final Object other$ifStatement = other.getIfStatement();
        if (this$ifStatement == null ? other$ifStatement != null : !this$ifStatement.equals(other$ifStatement)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ifStatement = this.getIfStatement();
        result = result * PRIME + ($ifStatement == null ? 43 : $ifStatement.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "WrappingAndBracesStyle(ifStatement=" + this.getIfStatement() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public WrappingAndBracesStyle withIfStatement(final IfStatement ifStatement) {
        return this.ifStatement == ifStatement ? this : new WrappingAndBracesStyle(ifStatement);
    }
}
