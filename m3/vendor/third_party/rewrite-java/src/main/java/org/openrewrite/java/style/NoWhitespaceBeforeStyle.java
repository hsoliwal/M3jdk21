// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/NoWhitespaceBeforeStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class NoWhitespaceBeforeStyle implements Style {
    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     */
    private final Boolean allowLineBreaks;
    /**
     * The . (dot) operator.
     */
    private final Boolean dot;
    /**
     * The , (comma) operator.
     */
    private final Boolean comma;
    /**
     * The statement terminator ({@code ;}).
     */
    private final Boolean semi;
    /**
     * A < symbol signifying the start of type arguments or type parameters.
     */
    private final Boolean genericStart;
    /**
     * A > symbol signifying the end of type arguments or type parameters.
     */
    private final Boolean genericEnd;
    /**
     * A :: reference to a method or constructor.
     */
    private final Boolean methodRef;
    /**
     * The ++ (postfix increment) operator.
     */
    private final Boolean postInc;
    /**
     * The -- (postfix decrement) operator.
     */
    private final Boolean postDec;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.noWhitespaceBeforeStyle(), this);
    }

    public NoWhitespaceBeforeStyle(final Boolean allowLineBreaks, final Boolean dot, final Boolean comma, final Boolean semi, final Boolean genericStart, final Boolean genericEnd, final Boolean methodRef, final Boolean postInc, final Boolean postDec) {
        this.allowLineBreaks = allowLineBreaks;
        this.dot = dot;
        this.comma = comma;
        this.semi = semi;
        this.genericStart = genericStart;
        this.genericEnd = genericEnd;
        this.methodRef = methodRef;
        this.postInc = postInc;
        this.postDec = postDec;
    }

    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     */
    public Boolean getAllowLineBreaks() {
        return this.allowLineBreaks;
    }

    /**
     * The . (dot) operator.
     */
    public Boolean getDot() {
        return this.dot;
    }

    /**
     * The , (comma) operator.
     */
    public Boolean getComma() {
        return this.comma;
    }

    /**
     * The statement terminator ({@code ;}).
     */
    public Boolean getSemi() {
        return this.semi;
    }

    /**
     * A < symbol signifying the start of type arguments or type parameters.
     */
    public Boolean getGenericStart() {
        return this.genericStart;
    }

    /**
     * A > symbol signifying the end of type arguments or type parameters.
     */
    public Boolean getGenericEnd() {
        return this.genericEnd;
    }

    /**
     * A :: reference to a method or constructor.
     */
    public Boolean getMethodRef() {
        return this.methodRef;
    }

    /**
     * The ++ (postfix increment) operator.
     */
    public Boolean getPostInc() {
        return this.postInc;
    }

    /**
     * The -- (postfix decrement) operator.
     */
    public Boolean getPostDec() {
        return this.postDec;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof NoWhitespaceBeforeStyle)) return false;
        final NoWhitespaceBeforeStyle other = (NoWhitespaceBeforeStyle) o;
        final Object this$allowLineBreaks = this.getAllowLineBreaks();
        final Object other$allowLineBreaks = other.getAllowLineBreaks();
        if (this$allowLineBreaks == null ? other$allowLineBreaks != null : !this$allowLineBreaks.equals(other$allowLineBreaks)) return false;
        final Object this$dot = this.getDot();
        final Object other$dot = other.getDot();
        if (this$dot == null ? other$dot != null : !this$dot.equals(other$dot)) return false;
        final Object this$comma = this.getComma();
        final Object other$comma = other.getComma();
        if (this$comma == null ? other$comma != null : !this$comma.equals(other$comma)) return false;
        final Object this$semi = this.getSemi();
        final Object other$semi = other.getSemi();
        if (this$semi == null ? other$semi != null : !this$semi.equals(other$semi)) return false;
        final Object this$genericStart = this.getGenericStart();
        final Object other$genericStart = other.getGenericStart();
        if (this$genericStart == null ? other$genericStart != null : !this$genericStart.equals(other$genericStart)) return false;
        final Object this$genericEnd = this.getGenericEnd();
        final Object other$genericEnd = other.getGenericEnd();
        if (this$genericEnd == null ? other$genericEnd != null : !this$genericEnd.equals(other$genericEnd)) return false;
        final Object this$methodRef = this.getMethodRef();
        final Object other$methodRef = other.getMethodRef();
        if (this$methodRef == null ? other$methodRef != null : !this$methodRef.equals(other$methodRef)) return false;
        final Object this$postInc = this.getPostInc();
        final Object other$postInc = other.getPostInc();
        if (this$postInc == null ? other$postInc != null : !this$postInc.equals(other$postInc)) return false;
        final Object this$postDec = this.getPostDec();
        final Object other$postDec = other.getPostDec();
        if (this$postDec == null ? other$postDec != null : !this$postDec.equals(other$postDec)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $allowLineBreaks = this.getAllowLineBreaks();
        result = result * PRIME + ($allowLineBreaks == null ? 43 : $allowLineBreaks.hashCode());
        final Object $dot = this.getDot();
        result = result * PRIME + ($dot == null ? 43 : $dot.hashCode());
        final Object $comma = this.getComma();
        result = result * PRIME + ($comma == null ? 43 : $comma.hashCode());
        final Object $semi = this.getSemi();
        result = result * PRIME + ($semi == null ? 43 : $semi.hashCode());
        final Object $genericStart = this.getGenericStart();
        result = result * PRIME + ($genericStart == null ? 43 : $genericStart.hashCode());
        final Object $genericEnd = this.getGenericEnd();
        result = result * PRIME + ($genericEnd == null ? 43 : $genericEnd.hashCode());
        final Object $methodRef = this.getMethodRef();
        result = result * PRIME + ($methodRef == null ? 43 : $methodRef.hashCode());
        final Object $postInc = this.getPostInc();
        result = result * PRIME + ($postInc == null ? 43 : $postInc.hashCode());
        final Object $postDec = this.getPostDec();
        result = result * PRIME + ($postDec == null ? 43 : $postDec.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "NoWhitespaceBeforeStyle(allowLineBreaks=" + this.getAllowLineBreaks() + ", dot=" + this.getDot() + ", comma=" + this.getComma() + ", semi=" + this.getSemi() + ", genericStart=" + this.getGenericStart() + ", genericEnd=" + this.getGenericEnd() + ", methodRef=" + this.getMethodRef() + ", postInc=" + this.getPostInc() + ", postDec=" + this.getPostDec() + ")";
    }

    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withAllowLineBreaks(final Boolean allowLineBreaks) {
        return this.allowLineBreaks == allowLineBreaks ? this : new NoWhitespaceBeforeStyle(allowLineBreaks, this.dot, this.comma, this.semi, this.genericStart, this.genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * The . (dot) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withDot(final Boolean dot) {
        return this.dot == dot ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, dot, this.comma, this.semi, this.genericStart, this.genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * The , (comma) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withComma(final Boolean comma) {
        return this.comma == comma ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, comma, this.semi, this.genericStart, this.genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * The statement terminator ({@code ;}).
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withSemi(final Boolean semi) {
        return this.semi == semi ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, semi, this.genericStart, this.genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * A < symbol signifying the start of type arguments or type parameters.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withGenericStart(final Boolean genericStart) {
        return this.genericStart == genericStart ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, this.semi, genericStart, this.genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * A > symbol signifying the end of type arguments or type parameters.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withGenericEnd(final Boolean genericEnd) {
        return this.genericEnd == genericEnd ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, this.semi, this.genericStart, genericEnd, this.methodRef, this.postInc, this.postDec);
    }

    /**
     * A :: reference to a method or constructor.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withMethodRef(final Boolean methodRef) {
        return this.methodRef == methodRef ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, this.semi, this.genericStart, this.genericEnd, methodRef, this.postInc, this.postDec);
    }

    /**
     * The ++ (postfix increment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withPostInc(final Boolean postInc) {
        return this.postInc == postInc ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, this.semi, this.genericStart, this.genericEnd, this.methodRef, postInc, this.postDec);
    }

    /**
     * The -- (postfix decrement) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceBeforeStyle withPostDec(final Boolean postDec) {
        return this.postDec == postDec ? this : new NoWhitespaceBeforeStyle(this.allowLineBreaks, this.dot, this.comma, this.semi, this.genericStart, this.genericEnd, this.methodRef, this.postInc, postDec);
    }
}
