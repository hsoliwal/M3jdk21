// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/UnnecessaryParenthesesStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class UnnecessaryParenthesesStyle implements JavaStyle {
    private final Boolean expr;
    private final Boolean ident;
    private final Boolean numDouble;
    private final Boolean numFloat;
    private final Boolean numInt;
    private final Boolean numLong;
    private final Boolean stringLiteral;
    private final Boolean literalNull;
    private final Boolean literalFalse;
    private final Boolean literalTrue;
    private final Boolean assign;
    private final Boolean bitAndAssign;
    private final Boolean bitOrAssign;
    private final Boolean bitShiftRightAssign;
    private final Boolean bitXorAssign;
    private final Boolean divAssign;
    private final Boolean minusAssign;
    private final Boolean modAssign;
    private final Boolean plusAssign;
    private final Boolean shiftLeftAssign;
    private final Boolean shiftRightAssign;
    private final Boolean starAssign;
    private final Boolean lambda;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.unnecessaryParentheses(), this);
    }

    public UnnecessaryParenthesesStyle(final Boolean expr, final Boolean ident, final Boolean numDouble, final Boolean numFloat, final Boolean numInt, final Boolean numLong, final Boolean stringLiteral, final Boolean literalNull, final Boolean literalFalse, final Boolean literalTrue, final Boolean assign, final Boolean bitAndAssign, final Boolean bitOrAssign, final Boolean bitShiftRightAssign, final Boolean bitXorAssign, final Boolean divAssign, final Boolean minusAssign, final Boolean modAssign, final Boolean plusAssign, final Boolean shiftLeftAssign, final Boolean shiftRightAssign, final Boolean starAssign, final Boolean lambda) {
        this.expr = expr;
        this.ident = ident;
        this.numDouble = numDouble;
        this.numFloat = numFloat;
        this.numInt = numInt;
        this.numLong = numLong;
        this.stringLiteral = stringLiteral;
        this.literalNull = literalNull;
        this.literalFalse = literalFalse;
        this.literalTrue = literalTrue;
        this.assign = assign;
        this.bitAndAssign = bitAndAssign;
        this.bitOrAssign = bitOrAssign;
        this.bitShiftRightAssign = bitShiftRightAssign;
        this.bitXorAssign = bitXorAssign;
        this.divAssign = divAssign;
        this.minusAssign = minusAssign;
        this.modAssign = modAssign;
        this.plusAssign = plusAssign;
        this.shiftLeftAssign = shiftLeftAssign;
        this.shiftRightAssign = shiftRightAssign;
        this.starAssign = starAssign;
        this.lambda = lambda;
    }

    public Boolean getExpr() {
        return this.expr;
    }

    public Boolean getIdent() {
        return this.ident;
    }

    public Boolean getNumDouble() {
        return this.numDouble;
    }

    public Boolean getNumFloat() {
        return this.numFloat;
    }

    public Boolean getNumInt() {
        return this.numInt;
    }

    public Boolean getNumLong() {
        return this.numLong;
    }

    public Boolean getStringLiteral() {
        return this.stringLiteral;
    }

    public Boolean getLiteralNull() {
        return this.literalNull;
    }

    public Boolean getLiteralFalse() {
        return this.literalFalse;
    }

    public Boolean getLiteralTrue() {
        return this.literalTrue;
    }

    public Boolean getAssign() {
        return this.assign;
    }

    public Boolean getBitAndAssign() {
        return this.bitAndAssign;
    }

    public Boolean getBitOrAssign() {
        return this.bitOrAssign;
    }

    public Boolean getBitShiftRightAssign() {
        return this.bitShiftRightAssign;
    }

    public Boolean getBitXorAssign() {
        return this.bitXorAssign;
    }

    public Boolean getDivAssign() {
        return this.divAssign;
    }

    public Boolean getMinusAssign() {
        return this.minusAssign;
    }

    public Boolean getModAssign() {
        return this.modAssign;
    }

    public Boolean getPlusAssign() {
        return this.plusAssign;
    }

    public Boolean getShiftLeftAssign() {
        return this.shiftLeftAssign;
    }

    public Boolean getShiftRightAssign() {
        return this.shiftRightAssign;
    }

    public Boolean getStarAssign() {
        return this.starAssign;
    }

    public Boolean getLambda() {
        return this.lambda;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof UnnecessaryParenthesesStyle)) return false;
        final UnnecessaryParenthesesStyle other = (UnnecessaryParenthesesStyle) o;
        final Object this$expr = this.getExpr();
        final Object other$expr = other.getExpr();
        if (this$expr == null ? other$expr != null : !this$expr.equals(other$expr)) return false;
        final Object this$ident = this.getIdent();
        final Object other$ident = other.getIdent();
        if (this$ident == null ? other$ident != null : !this$ident.equals(other$ident)) return false;
        final Object this$numDouble = this.getNumDouble();
        final Object other$numDouble = other.getNumDouble();
        if (this$numDouble == null ? other$numDouble != null : !this$numDouble.equals(other$numDouble)) return false;
        final Object this$numFloat = this.getNumFloat();
        final Object other$numFloat = other.getNumFloat();
        if (this$numFloat == null ? other$numFloat != null : !this$numFloat.equals(other$numFloat)) return false;
        final Object this$numInt = this.getNumInt();
        final Object other$numInt = other.getNumInt();
        if (this$numInt == null ? other$numInt != null : !this$numInt.equals(other$numInt)) return false;
        final Object this$numLong = this.getNumLong();
        final Object other$numLong = other.getNumLong();
        if (this$numLong == null ? other$numLong != null : !this$numLong.equals(other$numLong)) return false;
        final Object this$stringLiteral = this.getStringLiteral();
        final Object other$stringLiteral = other.getStringLiteral();
        if (this$stringLiteral == null ? other$stringLiteral != null : !this$stringLiteral.equals(other$stringLiteral)) return false;
        final Object this$literalNull = this.getLiteralNull();
        final Object other$literalNull = other.getLiteralNull();
        if (this$literalNull == null ? other$literalNull != null : !this$literalNull.equals(other$literalNull)) return false;
        final Object this$literalFalse = this.getLiteralFalse();
        final Object other$literalFalse = other.getLiteralFalse();
        if (this$literalFalse == null ? other$literalFalse != null : !this$literalFalse.equals(other$literalFalse)) return false;
        final Object this$literalTrue = this.getLiteralTrue();
        final Object other$literalTrue = other.getLiteralTrue();
        if (this$literalTrue == null ? other$literalTrue != null : !this$literalTrue.equals(other$literalTrue)) return false;
        final Object this$assign = this.getAssign();
        final Object other$assign = other.getAssign();
        if (this$assign == null ? other$assign != null : !this$assign.equals(other$assign)) return false;
        final Object this$bitAndAssign = this.getBitAndAssign();
        final Object other$bitAndAssign = other.getBitAndAssign();
        if (this$bitAndAssign == null ? other$bitAndAssign != null : !this$bitAndAssign.equals(other$bitAndAssign)) return false;
        final Object this$bitOrAssign = this.getBitOrAssign();
        final Object other$bitOrAssign = other.getBitOrAssign();
        if (this$bitOrAssign == null ? other$bitOrAssign != null : !this$bitOrAssign.equals(other$bitOrAssign)) return false;
        final Object this$bitShiftRightAssign = this.getBitShiftRightAssign();
        final Object other$bitShiftRightAssign = other.getBitShiftRightAssign();
        if (this$bitShiftRightAssign == null ? other$bitShiftRightAssign != null : !this$bitShiftRightAssign.equals(other$bitShiftRightAssign)) return false;
        final Object this$bitXorAssign = this.getBitXorAssign();
        final Object other$bitXorAssign = other.getBitXorAssign();
        if (this$bitXorAssign == null ? other$bitXorAssign != null : !this$bitXorAssign.equals(other$bitXorAssign)) return false;
        final Object this$divAssign = this.getDivAssign();
        final Object other$divAssign = other.getDivAssign();
        if (this$divAssign == null ? other$divAssign != null : !this$divAssign.equals(other$divAssign)) return false;
        final Object this$minusAssign = this.getMinusAssign();
        final Object other$minusAssign = other.getMinusAssign();
        if (this$minusAssign == null ? other$minusAssign != null : !this$minusAssign.equals(other$minusAssign)) return false;
        final Object this$modAssign = this.getModAssign();
        final Object other$modAssign = other.getModAssign();
        if (this$modAssign == null ? other$modAssign != null : !this$modAssign.equals(other$modAssign)) return false;
        final Object this$plusAssign = this.getPlusAssign();
        final Object other$plusAssign = other.getPlusAssign();
        if (this$plusAssign == null ? other$plusAssign != null : !this$plusAssign.equals(other$plusAssign)) return false;
        final Object this$shiftLeftAssign = this.getShiftLeftAssign();
        final Object other$shiftLeftAssign = other.getShiftLeftAssign();
        if (this$shiftLeftAssign == null ? other$shiftLeftAssign != null : !this$shiftLeftAssign.equals(other$shiftLeftAssign)) return false;
        final Object this$shiftRightAssign = this.getShiftRightAssign();
        final Object other$shiftRightAssign = other.getShiftRightAssign();
        if (this$shiftRightAssign == null ? other$shiftRightAssign != null : !this$shiftRightAssign.equals(other$shiftRightAssign)) return false;
        final Object this$starAssign = this.getStarAssign();
        final Object other$starAssign = other.getStarAssign();
        if (this$starAssign == null ? other$starAssign != null : !this$starAssign.equals(other$starAssign)) return false;
        final Object this$lambda = this.getLambda();
        final Object other$lambda = other.getLambda();
        if (this$lambda == null ? other$lambda != null : !this$lambda.equals(other$lambda)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $expr = this.getExpr();
        result = result * PRIME + ($expr == null ? 43 : $expr.hashCode());
        final Object $ident = this.getIdent();
        result = result * PRIME + ($ident == null ? 43 : $ident.hashCode());
        final Object $numDouble = this.getNumDouble();
        result = result * PRIME + ($numDouble == null ? 43 : $numDouble.hashCode());
        final Object $numFloat = this.getNumFloat();
        result = result * PRIME + ($numFloat == null ? 43 : $numFloat.hashCode());
        final Object $numInt = this.getNumInt();
        result = result * PRIME + ($numInt == null ? 43 : $numInt.hashCode());
        final Object $numLong = this.getNumLong();
        result = result * PRIME + ($numLong == null ? 43 : $numLong.hashCode());
        final Object $stringLiteral = this.getStringLiteral();
        result = result * PRIME + ($stringLiteral == null ? 43 : $stringLiteral.hashCode());
        final Object $literalNull = this.getLiteralNull();
        result = result * PRIME + ($literalNull == null ? 43 : $literalNull.hashCode());
        final Object $literalFalse = this.getLiteralFalse();
        result = result * PRIME + ($literalFalse == null ? 43 : $literalFalse.hashCode());
        final Object $literalTrue = this.getLiteralTrue();
        result = result * PRIME + ($literalTrue == null ? 43 : $literalTrue.hashCode());
        final Object $assign = this.getAssign();
        result = result * PRIME + ($assign == null ? 43 : $assign.hashCode());
        final Object $bitAndAssign = this.getBitAndAssign();
        result = result * PRIME + ($bitAndAssign == null ? 43 : $bitAndAssign.hashCode());
        final Object $bitOrAssign = this.getBitOrAssign();
        result = result * PRIME + ($bitOrAssign == null ? 43 : $bitOrAssign.hashCode());
        final Object $bitShiftRightAssign = this.getBitShiftRightAssign();
        result = result * PRIME + ($bitShiftRightAssign == null ? 43 : $bitShiftRightAssign.hashCode());
        final Object $bitXorAssign = this.getBitXorAssign();
        result = result * PRIME + ($bitXorAssign == null ? 43 : $bitXorAssign.hashCode());
        final Object $divAssign = this.getDivAssign();
        result = result * PRIME + ($divAssign == null ? 43 : $divAssign.hashCode());
        final Object $minusAssign = this.getMinusAssign();
        result = result * PRIME + ($minusAssign == null ? 43 : $minusAssign.hashCode());
        final Object $modAssign = this.getModAssign();
        result = result * PRIME + ($modAssign == null ? 43 : $modAssign.hashCode());
        final Object $plusAssign = this.getPlusAssign();
        result = result * PRIME + ($plusAssign == null ? 43 : $plusAssign.hashCode());
        final Object $shiftLeftAssign = this.getShiftLeftAssign();
        result = result * PRIME + ($shiftLeftAssign == null ? 43 : $shiftLeftAssign.hashCode());
        final Object $shiftRightAssign = this.getShiftRightAssign();
        result = result * PRIME + ($shiftRightAssign == null ? 43 : $shiftRightAssign.hashCode());
        final Object $starAssign = this.getStarAssign();
        result = result * PRIME + ($starAssign == null ? 43 : $starAssign.hashCode());
        final Object $lambda = this.getLambda();
        result = result * PRIME + ($lambda == null ? 43 : $lambda.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "UnnecessaryParenthesesStyle(expr=" + this.getExpr() + ", ident=" + this.getIdent() + ", numDouble=" + this.getNumDouble() + ", numFloat=" + this.getNumFloat() + ", numInt=" + this.getNumInt() + ", numLong=" + this.getNumLong() + ", stringLiteral=" + this.getStringLiteral() + ", literalNull=" + this.getLiteralNull() + ", literalFalse=" + this.getLiteralFalse() + ", literalTrue=" + this.getLiteralTrue() + ", assign=" + this.getAssign() + ", bitAndAssign=" + this.getBitAndAssign() + ", bitOrAssign=" + this.getBitOrAssign() + ", bitShiftRightAssign=" + this.getBitShiftRightAssign() + ", bitXorAssign=" + this.getBitXorAssign() + ", divAssign=" + this.getDivAssign() + ", minusAssign=" + this.getMinusAssign() + ", modAssign=" + this.getModAssign() + ", plusAssign=" + this.getPlusAssign() + ", shiftLeftAssign=" + this.getShiftLeftAssign() + ", shiftRightAssign=" + this.getShiftRightAssign() + ", starAssign=" + this.getStarAssign() + ", lambda=" + this.getLambda() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withExpr(final Boolean expr) {
        return this.expr == expr ? this : new UnnecessaryParenthesesStyle(expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withIdent(final Boolean ident) {
        return this.ident == ident ? this : new UnnecessaryParenthesesStyle(this.expr, ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withNumDouble(final Boolean numDouble) {
        return this.numDouble == numDouble ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withNumFloat(final Boolean numFloat) {
        return this.numFloat == numFloat ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withNumInt(final Boolean numInt) {
        return this.numInt == numInt ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withNumLong(final Boolean numLong) {
        return this.numLong == numLong ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withStringLiteral(final Boolean stringLiteral) {
        return this.stringLiteral == stringLiteral ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withLiteralNull(final Boolean literalNull) {
        return this.literalNull == literalNull ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withLiteralFalse(final Boolean literalFalse) {
        return this.literalFalse == literalFalse ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withLiteralTrue(final Boolean literalTrue) {
        return this.literalTrue == literalTrue ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withAssign(final Boolean assign) {
        return this.assign == assign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withBitAndAssign(final Boolean bitAndAssign) {
        return this.bitAndAssign == bitAndAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withBitOrAssign(final Boolean bitOrAssign) {
        return this.bitOrAssign == bitOrAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withBitShiftRightAssign(final Boolean bitShiftRightAssign) {
        return this.bitShiftRightAssign == bitShiftRightAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withBitXorAssign(final Boolean bitXorAssign) {
        return this.bitXorAssign == bitXorAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withDivAssign(final Boolean divAssign) {
        return this.divAssign == divAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withMinusAssign(final Boolean minusAssign) {
        return this.minusAssign == minusAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withModAssign(final Boolean modAssign) {
        return this.modAssign == modAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withPlusAssign(final Boolean plusAssign) {
        return this.plusAssign == plusAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withShiftLeftAssign(final Boolean shiftLeftAssign) {
        return this.shiftLeftAssign == shiftLeftAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, shiftLeftAssign, this.shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withShiftRightAssign(final Boolean shiftRightAssign) {
        return this.shiftRightAssign == shiftRightAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, shiftRightAssign, this.starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withStarAssign(final Boolean starAssign) {
        return this.starAssign == starAssign ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, starAssign, this.lambda);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public UnnecessaryParenthesesStyle withLambda(final Boolean lambda) {
        return this.lambda == lambda ? this : new UnnecessaryParenthesesStyle(this.expr, this.ident, this.numDouble, this.numFloat, this.numInt, this.numLong, this.stringLiteral, this.literalNull, this.literalFalse, this.literalTrue, this.assign, this.bitAndAssign, this.bitOrAssign, this.bitShiftRightAssign, this.bitXorAssign, this.divAssign, this.minusAssign, this.modAssign, this.plusAssign, this.shiftLeftAssign, this.shiftRightAssign, this.starAssign, lambda);
    }
}
