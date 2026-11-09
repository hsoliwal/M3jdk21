// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/NeedBracesStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

public final class NeedBracesStyle implements Style {
    /**
     * Allow single-line statements without braces.
     */
    private final Boolean allowSingleLineStatement;
    /**
     * Allow loops with empty bodies.
     */
    private final Boolean allowEmptyLoopBody;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.needBracesStyle(), this);
    }

    public NeedBracesStyle(final Boolean allowSingleLineStatement, final Boolean allowEmptyLoopBody) {
        this.allowSingleLineStatement = allowSingleLineStatement;
        this.allowEmptyLoopBody = allowEmptyLoopBody;
    }

    /**
     * Allow single-line statements without braces.
     */
    public Boolean getAllowSingleLineStatement() {
        return this.allowSingleLineStatement;
    }

    /**
     * Allow loops with empty bodies.
     */
    public Boolean getAllowEmptyLoopBody() {
        return this.allowEmptyLoopBody;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof NeedBracesStyle)) return false;
        final NeedBracesStyle other = (NeedBracesStyle) o;
        final Object this$allowSingleLineStatement = this.getAllowSingleLineStatement();
        final Object other$allowSingleLineStatement = other.getAllowSingleLineStatement();
        if (this$allowSingleLineStatement == null ? other$allowSingleLineStatement != null : !this$allowSingleLineStatement.equals(other$allowSingleLineStatement)) return false;
        final Object this$allowEmptyLoopBody = this.getAllowEmptyLoopBody();
        final Object other$allowEmptyLoopBody = other.getAllowEmptyLoopBody();
        if (this$allowEmptyLoopBody == null ? other$allowEmptyLoopBody != null : !this$allowEmptyLoopBody.equals(other$allowEmptyLoopBody)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $allowSingleLineStatement = this.getAllowSingleLineStatement();
        result = result * PRIME + ($allowSingleLineStatement == null ? 43 : $allowSingleLineStatement.hashCode());
        final Object $allowEmptyLoopBody = this.getAllowEmptyLoopBody();
        result = result * PRIME + ($allowEmptyLoopBody == null ? 43 : $allowEmptyLoopBody.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "NeedBracesStyle(allowSingleLineStatement=" + this.getAllowSingleLineStatement() + ", allowEmptyLoopBody=" + this.getAllowEmptyLoopBody() + ")";
    }

    /**
     * Allow single-line statements without braces.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NeedBracesStyle withAllowSingleLineStatement(final Boolean allowSingleLineStatement) {
        return this.allowSingleLineStatement == allowSingleLineStatement ? this : new NeedBracesStyle(allowSingleLineStatement, this.allowEmptyLoopBody);
    }

    /**
     * Allow loops with empty bodies.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NeedBracesStyle withAllowEmptyLoopBody(final Boolean allowEmptyLoopBody) {
        return this.allowEmptyLoopBody == allowEmptyLoopBody ? this : new NeedBracesStyle(this.allowSingleLineStatement, allowEmptyLoopBody);
    }
}
