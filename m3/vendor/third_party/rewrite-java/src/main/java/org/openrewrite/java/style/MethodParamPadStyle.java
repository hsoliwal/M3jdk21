// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/MethodParamPadStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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

public final class MethodParamPadStyle implements Style {
    /**
     * Whether to add spacing between the method declarations (or method invocations) identifier and the start of the left parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code method (a, b);}
     * <p>
     * When false:
     * <p>
     * {@code method(a, b);}
     */
    private final Boolean space;
    /**
     * Whether to allow a linebreak between the method declaration (or method invocation) identifier and the start of the left parenthesis.
     * Note this does not add a linebreak if one is missing. It only controls whether a linebreak is permitted.
     * <p>
     * When true:
     * <pre>{@code method // acceptable
     *      (a, b);
     * }</pre>
     * <p>
     * When false, the linebreak would be removed, becoming:
     * <p>
     * {@code method(a, b);}
     * <p>
     */
    private final Boolean allowLineBreaks;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.methodParamPadStyle(), this);
    }

    public MethodParamPadStyle(final Boolean space, final Boolean allowLineBreaks) {
        this.space = space;
        this.allowLineBreaks = allowLineBreaks;
    }

    /**
     * Whether to add spacing between the method declarations (or method invocations) identifier and the start of the left parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code method (a, b);}
     * <p>
     * When false:
     * <p>
     * {@code method(a, b);}
     */
    public Boolean getSpace() {
        return this.space;
    }

    /**
     * Whether to allow a linebreak between the method declaration (or method invocation) identifier and the start of the left parenthesis.
     * Note this does not add a linebreak if one is missing. It only controls whether a linebreak is permitted.
     * <p>
     * When true:
     * <pre>{@code method // acceptable
     *      (a, b);
     * }</pre>
     * <p>
     * When false, the linebreak would be removed, becoming:
     * <p>
     * {@code method(a, b);}
     * <p>
     */
    public Boolean getAllowLineBreaks() {
        return this.allowLineBreaks;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof MethodParamPadStyle)) return false;
        final MethodParamPadStyle other = (MethodParamPadStyle) o;
        final Object this$space = this.getSpace();
        final Object other$space = other.getSpace();
        if (this$space == null ? other$space != null : !this$space.equals(other$space)) return false;
        final Object this$allowLineBreaks = this.getAllowLineBreaks();
        final Object other$allowLineBreaks = other.getAllowLineBreaks();
        if (this$allowLineBreaks == null ? other$allowLineBreaks != null : !this$allowLineBreaks.equals(other$allowLineBreaks)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $space = this.getSpace();
        result = result * PRIME + ($space == null ? 43 : $space.hashCode());
        final Object $allowLineBreaks = this.getAllowLineBreaks();
        result = result * PRIME + ($allowLineBreaks == null ? 43 : $allowLineBreaks.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "MethodParamPadStyle(space=" + this.getSpace() + ", allowLineBreaks=" + this.getAllowLineBreaks() + ")";
    }

    /**
     * Whether to add spacing between the method declarations (or method invocations) identifier and the start of the left parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code method (a, b);}
     * <p>
     * When false:
     * <p>
     * {@code method(a, b);}
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public MethodParamPadStyle withSpace(final Boolean space) {
        return this.space == space ? this : new MethodParamPadStyle(space, this.allowLineBreaks);
    }

    /**
     * Whether to allow a linebreak between the method declaration (or method invocation) identifier and the start of the left parenthesis.
     * Note this does not add a linebreak if one is missing. It only controls whether a linebreak is permitted.
     * <p>
     * When true:
     * <pre>{@code method // acceptable
     *      (a, b);
     * }</pre>
     * <p>
     * When false, the linebreak would be removed, becoming:
     * <p>
     * {@code method(a, b);}
     * <p>
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public MethodParamPadStyle withAllowLineBreaks(final Boolean allowLineBreaks) {
        return this.allowLineBreaks == allowLineBreaks ? this : new MethodParamPadStyle(this.space, allowLineBreaks);
    }
}
