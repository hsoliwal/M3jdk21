// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/OperatorWrapStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.NullFields;
import org.openrewrite.style.Style;
import org.openrewrite.style.StyleHelper;

@NullFields
public final class OperatorWrapStyle implements Style {
    /**
     * Whether to add a newline for the token at the end of the line.
     */
    @NonNull
    private final WrapOption wrapOption;
    /**
     * The ? (conditional) operator.
     */
    private final Boolean question;
    /**
     * The : (colon) operator.
     */
    private final Boolean colon;
    /**
     * The == (equal) operator.
     */
    private final Boolean equal;
    /**
     * The != (not equal) operator.
     */
    private final Boolean notEqual;
    /**
     * The / (division) operator.
     */
    private final Boolean div;
    /**
     * The + (addition) operator.
     */
    private final Boolean plus;
    /**
     * The - (subtraction) operator.
     */
    private final Boolean minus;
    /**
     * The * (multiplication or wildcard) operator.
     */
    private final Boolean star;
    /**
     * The % (remainder) operator.
     */
    private final Boolean mod;
    /**
     * The >> (signed shift right) operator.
     */
    private final Boolean sr;
    /**
     * The >>> (unsigned shift right) operator.
     */
    private final Boolean bsr;
    /**
     * The >= (greater than or equal) operator.
     */
    private final Boolean ge;
    /**
     * The > (greater than) operator.
     */
    private final Boolean gt;
    /**
     * The << (shift left) operator.
     */
    private final Boolean sl;
    /**
     * The <= (less than or equal) operator.
     */
    private final Boolean le;
    /**
     * The < (less than) operator.
     */
    private final Boolean lt;
    /**
     * The ^ (bitwise exclusive OR) operator.
     */
    private final Boolean bxor;
    /**
     * The | (bitwise OR) operator.
     */
    private final Boolean bor;
    /**
     * The || (conditional OR) operator.
     */
    private final Boolean lor;
    /**
     * The & (bitwise AND) operator.
     */
    private final Boolean band;
    /**
     * The && (conditional AND) operator.
     */
    private final Boolean land;
    /**
     * The & symbol when used to extend a generic upper or lower bounds constrain or a type cast expression with an additional interface.
     */
    private final Boolean typeExtensionAnd;
    /**
     * The instanceof operator.
     */
    private final Boolean literalInstanceof;
    /**
     * A :: reference to a method or constructor without arguments.
     */
    private final Boolean methodRef;
    /**
     * The = (assignment) operator.
     */
    private final Boolean assign;
    /**
     * The += (addition assignment) operator.
     */
    private final Boolean plusAssign;
    /**
     * The -= (subtraction assignment) operator.
     */
    private final Boolean minusAssign;
    /**
     * The *= (multiplication assignment) operator.
     */
    private final Boolean starAssign;
    /**
     * The /= (division assignment) operator.
     */
    private final Boolean divAssign;
    /**
     * The %= (remainder assignment) operator.
     */
    private final Boolean modAssign;
    /**
     * The >>= (signed right shift assignment) operator.
     */
    private final Boolean srAssign;
    /**
     * The >>>= (unsigned right shift assignment) operator.
     */
    private final Boolean bsrAssign;
    /**
     * The <<= (left shift assignment) operator.
     */
    private final Boolean slAssign;
    /**
     * The &= (bitwise AND assignment) operator.
     */
    private final Boolean bandAssign;
    /**
     * The ^= (bitwise exclusive OR assignment) operator.
     */
    private final Boolean bxorAssign;
    /**
     * The |= (bitwise OR assignment) operator.
     */
    private final Boolean borAssign;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.operatorWrapStyle(), this);
    }


    public enum WrapOption {
        /**
         * Require that the token is at the end of the line.
         */
        EOL, 
        /**
         * Require that the token is on a new line.
         */
        NL;
    }

    public OperatorWrapStyle(@NonNull final WrapOption wrapOption, final Boolean question, final Boolean colon, final Boolean equal, final Boolean notEqual, final Boolean div, final Boolean plus, final Boolean minus, final Boolean star, final Boolean mod, final Boolean sr, final Boolean bsr, final Boolean ge, final Boolean gt, final Boolean sl, final Boolean le, final Boolean lt, final Boolean bxor, final Boolean bor, final Boolean lor, final Boolean band, final Boolean land, final Boolean typeExtensionAnd, final Boolean literalInstanceof, final Boolean methodRef, final Boolean assign, final Boolean plusAssign, final Boolean minusAssign, final Boolean starAssign, final Boolean divAssign, final Boolean modAssign, final Boolean srAssign, final Boolean bsrAssign, final Boolean slAssign, final Boolean bandAssign, final Boolean bxorAssign, final Boolean borAssign) {
        this.wrapOption = wrapOption;
        this.question = question;
        this.colon = colon;
        this.equal = equal;
        this.notEqual = notEqual;
        this.div = div;
        this.plus = plus;
        this.minus = minus;
        this.star = star;
        this.mod = mod;
        this.sr = sr;
        this.bsr = bsr;
        this.ge = ge;
        this.gt = gt;
        this.sl = sl;
        this.le = le;
        this.lt = lt;
        this.bxor = bxor;
        this.bor = bor;
        this.lor = lor;
        this.band = band;
        this.land = land;
        this.typeExtensionAnd = typeExtensionAnd;
        this.literalInstanceof = literalInstanceof;
        this.methodRef = methodRef;
        this.assign = assign;
        this.plusAssign = plusAssign;
        this.minusAssign = minusAssign;
        this.starAssign = starAssign;
        this.divAssign = divAssign;
        this.modAssign = modAssign;
        this.srAssign = srAssign;
        this.bsrAssign = bsrAssign;
        this.slAssign = slAssign;
        this.bandAssign = bandAssign;
        this.bxorAssign = bxorAssign;
        this.borAssign = borAssign;
    }

    /**
     * Whether to add a newline for the token at the end of the line.
     */
    @NonNull
    public WrapOption getWrapOption() {
        return this.wrapOption;
    }

    /**
     * The ? (conditional) operator.
     */
    public Boolean getQuestion() {
        return this.question;
    }

    /**
     * The : (colon) operator.
     */
    public Boolean getColon() {
        return this.colon;
    }

    /**
     * The == (equal) operator.
     */
    public Boolean getEqual() {
        return this.equal;
    }

    /**
     * The != (not equal) operator.
     */
    public Boolean getNotEqual() {
        return this.notEqual;
    }

    /**
     * The / (division) operator.
     */
    public Boolean getDiv() {
        return this.div;
    }

    /**
     * The + (addition) operator.
     */
    public Boolean getPlus() {
        return this.plus;
    }

    /**
     * The - (subtraction) operator.
     */
    public Boolean getMinus() {
        return this.minus;
    }

    /**
     * The * (multiplication or wildcard) operator.
     */
    public Boolean getStar() {
        return this.star;
    }

    /**
     * The % (remainder) operator.
     */
    public Boolean getMod() {
        return this.mod;
    }

    /**
     * The >> (signed shift right) operator.
     */
    public Boolean getSr() {
        return this.sr;
    }

    /**
     * The >>> (unsigned shift right) operator.
     */
    public Boolean getBsr() {
        return this.bsr;
    }

    /**
     * The >= (greater than or equal) operator.
     */
    public Boolean getGe() {
        return this.ge;
    }

    /**
     * The > (greater than) operator.
     */
    public Boolean getGt() {
        return this.gt;
    }

    /**
     * The << (shift left) operator.
     */
    public Boolean getSl() {
        return this.sl;
    }

    /**
     * The <= (less than or equal) operator.
     */
    public Boolean getLe() {
        return this.le;
    }

    /**
     * The < (less than) operator.
     */
    public Boolean getLt() {
        return this.lt;
    }

    /**
     * The ^ (bitwise exclusive OR) operator.
     */
    public Boolean getBxor() {
        return this.bxor;
    }

    /**
     * The | (bitwise OR) operator.
     */
    public Boolean getBor() {
        return this.bor;
    }

    /**
     * The || (conditional OR) operator.
     */
    public Boolean getLor() {
        return this.lor;
    }

    /**
     * The & (bitwise AND) operator.
     */
    public Boolean getBand() {
        return this.band;
    }

    /**
     * The && (conditional AND) operator.
     */
    public Boolean getLand() {
        return this.land;
    }

    /**
     * The & symbol when used to extend a generic upper or lower bounds constrain or a type cast expression with an additional interface.
     */
    public Boolean getTypeExtensionAnd() {
        return this.typeExtensionAnd;
    }

    /**
     * The instanceof operator.
     */
    public Boolean getLiteralInstanceof() {
        return this.literalInstanceof;
    }

    /**
     * A :: reference to a method or constructor without arguments.
     */
    public Boolean getMethodRef() {
        return this.methodRef;
    }

    /**
     * The = (assignment) operator.
     */
    public Boolean getAssign() {
        return this.assign;
    }

    /**
     * The += (addition assignment) operator.
     */
    public Boolean getPlusAssign() {
        return this.plusAssign;
    }

    /**
     * The -= (subtraction assignment) operator.
     */
    public Boolean getMinusAssign() {
        return this.minusAssign;
    }

    /**
     * The *= (multiplication assignment) operator.
     */
    public Boolean getStarAssign() {
        return this.starAssign;
    }

    /**
     * The /= (division assignment) operator.
     */
    public Boolean getDivAssign() {
        return this.divAssign;
    }

    /**
     * The %= (remainder assignment) operator.
     */
    public Boolean getModAssign() {
        return this.modAssign;
    }

    /**
     * The >>= (signed right shift assignment) operator.
     */
    public Boolean getSrAssign() {
        return this.srAssign;
    }

    /**
     * The >>>= (unsigned right shift assignment) operator.
     */
    public Boolean getBsrAssign() {
        return this.bsrAssign;
    }

    /**
     * The <<= (left shift assignment) operator.
     */
    public Boolean getSlAssign() {
        return this.slAssign;
    }

    /**
     * The &= (bitwise AND assignment) operator.
     */
    public Boolean getBandAssign() {
        return this.bandAssign;
    }

    /**
     * The ^= (bitwise exclusive OR assignment) operator.
     */
    public Boolean getBxorAssign() {
        return this.bxorAssign;
    }

    /**
     * The |= (bitwise OR assignment) operator.
     */
    public Boolean getBorAssign() {
        return this.borAssign;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof OperatorWrapStyle)) return false;
        final OperatorWrapStyle other = (OperatorWrapStyle) o;
        final Object this$question = this.getQuestion();
        final Object other$question = other.getQuestion();
        if (this$question == null ? other$question != null : !this$question.equals(other$question)) return false;
        final Object this$colon = this.getColon();
        final Object other$colon = other.getColon();
        if (this$colon == null ? other$colon != null : !this$colon.equals(other$colon)) return false;
        final Object this$equal = this.getEqual();
        final Object other$equal = other.getEqual();
        if (this$equal == null ? other$equal != null : !this$equal.equals(other$equal)) return false;
        final Object this$notEqual = this.getNotEqual();
        final Object other$notEqual = other.getNotEqual();
        if (this$notEqual == null ? other$notEqual != null : !this$notEqual.equals(other$notEqual)) return false;
        final Object this$div = this.getDiv();
        final Object other$div = other.getDiv();
        if (this$div == null ? other$div != null : !this$div.equals(other$div)) return false;
        final Object this$plus = this.getPlus();
        final Object other$plus = other.getPlus();
        if (this$plus == null ? other$plus != null : !this$plus.equals(other$plus)) return false;
        final Object this$minus = this.getMinus();
        final Object other$minus = other.getMinus();
        if (this$minus == null ? other$minus != null : !this$minus.equals(other$minus)) return false;
        final Object this$star = this.getStar();
        final Object other$star = other.getStar();
        if (this$star == null ? other$star != null : !this$star.equals(other$star)) return false;
        final Object this$mod = this.getMod();
        final Object other$mod = other.getMod();
        if (this$mod == null ? other$mod != null : !this$mod.equals(other$mod)) return false;
        final Object this$sr = this.getSr();
        final Object other$sr = other.getSr();
        if (this$sr == null ? other$sr != null : !this$sr.equals(other$sr)) return false;
        final Object this$bsr = this.getBsr();
        final Object other$bsr = other.getBsr();
        if (this$bsr == null ? other$bsr != null : !this$bsr.equals(other$bsr)) return false;
        final Object this$ge = this.getGe();
        final Object other$ge = other.getGe();
        if (this$ge == null ? other$ge != null : !this$ge.equals(other$ge)) return false;
        final Object this$gt = this.getGt();
        final Object other$gt = other.getGt();
        if (this$gt == null ? other$gt != null : !this$gt.equals(other$gt)) return false;
        final Object this$sl = this.getSl();
        final Object other$sl = other.getSl();
        if (this$sl == null ? other$sl != null : !this$sl.equals(other$sl)) return false;
        final Object this$le = this.getLe();
        final Object other$le = other.getLe();
        if (this$le == null ? other$le != null : !this$le.equals(other$le)) return false;
        final Object this$lt = this.getLt();
        final Object other$lt = other.getLt();
        if (this$lt == null ? other$lt != null : !this$lt.equals(other$lt)) return false;
        final Object this$bxor = this.getBxor();
        final Object other$bxor = other.getBxor();
        if (this$bxor == null ? other$bxor != null : !this$bxor.equals(other$bxor)) return false;
        final Object this$bor = this.getBor();
        final Object other$bor = other.getBor();
        if (this$bor == null ? other$bor != null : !this$bor.equals(other$bor)) return false;
        final Object this$lor = this.getLor();
        final Object other$lor = other.getLor();
        if (this$lor == null ? other$lor != null : !this$lor.equals(other$lor)) return false;
        final Object this$band = this.getBand();
        final Object other$band = other.getBand();
        if (this$band == null ? other$band != null : !this$band.equals(other$band)) return false;
        final Object this$land = this.getLand();
        final Object other$land = other.getLand();
        if (this$land == null ? other$land != null : !this$land.equals(other$land)) return false;
        final Object this$typeExtensionAnd = this.getTypeExtensionAnd();
        final Object other$typeExtensionAnd = other.getTypeExtensionAnd();
        if (this$typeExtensionAnd == null ? other$typeExtensionAnd != null : !this$typeExtensionAnd.equals(other$typeExtensionAnd)) return false;
        final Object this$literalInstanceof = this.getLiteralInstanceof();
        final Object other$literalInstanceof = other.getLiteralInstanceof();
        if (this$literalInstanceof == null ? other$literalInstanceof != null : !this$literalInstanceof.equals(other$literalInstanceof)) return false;
        final Object this$methodRef = this.getMethodRef();
        final Object other$methodRef = other.getMethodRef();
        if (this$methodRef == null ? other$methodRef != null : !this$methodRef.equals(other$methodRef)) return false;
        final Object this$assign = this.getAssign();
        final Object other$assign = other.getAssign();
        if (this$assign == null ? other$assign != null : !this$assign.equals(other$assign)) return false;
        final Object this$plusAssign = this.getPlusAssign();
        final Object other$plusAssign = other.getPlusAssign();
        if (this$plusAssign == null ? other$plusAssign != null : !this$plusAssign.equals(other$plusAssign)) return false;
        final Object this$minusAssign = this.getMinusAssign();
        final Object other$minusAssign = other.getMinusAssign();
        if (this$minusAssign == null ? other$minusAssign != null : !this$minusAssign.equals(other$minusAssign)) return false;
        final Object this$starAssign = this.getStarAssign();
        final Object other$starAssign = other.getStarAssign();
        if (this$starAssign == null ? other$starAssign != null : !this$starAssign.equals(other$starAssign)) return false;
        final Object this$divAssign = this.getDivAssign();
        final Object other$divAssign = other.getDivAssign();
        if (this$divAssign == null ? other$divAssign != null : !this$divAssign.equals(other$divAssign)) return false;
        final Object this$modAssign = this.getModAssign();
        final Object other$modAssign = other.getModAssign();
        if (this$modAssign == null ? other$modAssign != null : !this$modAssign.equals(other$modAssign)) return false;
        final Object this$srAssign = this.getSrAssign();
        final Object other$srAssign = other.getSrAssign();
        if (this$srAssign == null ? other$srAssign != null : !this$srAssign.equals(other$srAssign)) return false;
        final Object this$bsrAssign = this.getBsrAssign();
        final Object other$bsrAssign = other.getBsrAssign();
        if (this$bsrAssign == null ? other$bsrAssign != null : !this$bsrAssign.equals(other$bsrAssign)) return false;
        final Object this$slAssign = this.getSlAssign();
        final Object other$slAssign = other.getSlAssign();
        if (this$slAssign == null ? other$slAssign != null : !this$slAssign.equals(other$slAssign)) return false;
        final Object this$bandAssign = this.getBandAssign();
        final Object other$bandAssign = other.getBandAssign();
        if (this$bandAssign == null ? other$bandAssign != null : !this$bandAssign.equals(other$bandAssign)) return false;
        final Object this$bxorAssign = this.getBxorAssign();
        final Object other$bxorAssign = other.getBxorAssign();
        if (this$bxorAssign == null ? other$bxorAssign != null : !this$bxorAssign.equals(other$bxorAssign)) return false;
        final Object this$borAssign = this.getBorAssign();
        final Object other$borAssign = other.getBorAssign();
        if (this$borAssign == null ? other$borAssign != null : !this$borAssign.equals(other$borAssign)) return false;
        final Object this$wrapOption = this.getWrapOption();
        final Object other$wrapOption = other.getWrapOption();
        if (this$wrapOption == null ? other$wrapOption != null : !this$wrapOption.equals(other$wrapOption)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $question = this.getQuestion();
        result = result * PRIME + ($question == null ? 43 : $question.hashCode());
        final Object $colon = this.getColon();
        result = result * PRIME + ($colon == null ? 43 : $colon.hashCode());
        final Object $equal = this.getEqual();
        result = result * PRIME + ($equal == null ? 43 : $equal.hashCode());
        final Object $notEqual = this.getNotEqual();
        result = result * PRIME + ($notEqual == null ? 43 : $notEqual.hashCode());
        final Object $div = this.getDiv();
        result = result * PRIME + ($div == null ? 43 : $div.hashCode());
        final Object $plus = this.getPlus();
        result = result * PRIME + ($plus == null ? 43 : $plus.hashCode());
        final Object $minus = this.getMinus();
        result = result * PRIME + ($minus == null ? 43 : $minus.hashCode());
        final Object $star = this.getStar();
        result = result * PRIME + ($star == null ? 43 : $star.hashCode());
        final Object $mod = this.getMod();
        result = result * PRIME + ($mod == null ? 43 : $mod.hashCode());
        final Object $sr = this.getSr();
        result = result * PRIME + ($sr == null ? 43 : $sr.hashCode());
        final Object $bsr = this.getBsr();
        result = result * PRIME + ($bsr == null ? 43 : $bsr.hashCode());
        final Object $ge = this.getGe();
        result = result * PRIME + ($ge == null ? 43 : $ge.hashCode());
        final Object $gt = this.getGt();
        result = result * PRIME + ($gt == null ? 43 : $gt.hashCode());
        final Object $sl = this.getSl();
        result = result * PRIME + ($sl == null ? 43 : $sl.hashCode());
        final Object $le = this.getLe();
        result = result * PRIME + ($le == null ? 43 : $le.hashCode());
        final Object $lt = this.getLt();
        result = result * PRIME + ($lt == null ? 43 : $lt.hashCode());
        final Object $bxor = this.getBxor();
        result = result * PRIME + ($bxor == null ? 43 : $bxor.hashCode());
        final Object $bor = this.getBor();
        result = result * PRIME + ($bor == null ? 43 : $bor.hashCode());
        final Object $lor = this.getLor();
        result = result * PRIME + ($lor == null ? 43 : $lor.hashCode());
        final Object $band = this.getBand();
        result = result * PRIME + ($band == null ? 43 : $band.hashCode());
        final Object $land = this.getLand();
        result = result * PRIME + ($land == null ? 43 : $land.hashCode());
        final Object $typeExtensionAnd = this.getTypeExtensionAnd();
        result = result * PRIME + ($typeExtensionAnd == null ? 43 : $typeExtensionAnd.hashCode());
        final Object $literalInstanceof = this.getLiteralInstanceof();
        result = result * PRIME + ($literalInstanceof == null ? 43 : $literalInstanceof.hashCode());
        final Object $methodRef = this.getMethodRef();
        result = result * PRIME + ($methodRef == null ? 43 : $methodRef.hashCode());
        final Object $assign = this.getAssign();
        result = result * PRIME + ($assign == null ? 43 : $assign.hashCode());
        final Object $plusAssign = this.getPlusAssign();
        result = result * PRIME + ($plusAssign == null ? 43 : $plusAssign.hashCode());
        final Object $minusAssign = this.getMinusAssign();
        result = result * PRIME + ($minusAssign == null ? 43 : $minusAssign.hashCode());
        final Object $starAssign = this.getStarAssign();
        result = result * PRIME + ($starAssign == null ? 43 : $starAssign.hashCode());
        final Object $divAssign = this.getDivAssign();
        result = result * PRIME + ($divAssign == null ? 43 : $divAssign.hashCode());
        final Object $modAssign = this.getModAssign();
        result = result * PRIME + ($modAssign == null ? 43 : $modAssign.hashCode());
        final Object $srAssign = this.getSrAssign();
        result = result * PRIME + ($srAssign == null ? 43 : $srAssign.hashCode());
        final Object $bsrAssign = this.getBsrAssign();
        result = result * PRIME + ($bsrAssign == null ? 43 : $bsrAssign.hashCode());
        final Object $slAssign = this.getSlAssign();
        result = result * PRIME + ($slAssign == null ? 43 : $slAssign.hashCode());
        final Object $bandAssign = this.getBandAssign();
        result = result * PRIME + ($bandAssign == null ? 43 : $bandAssign.hashCode());
        final Object $bxorAssign = this.getBxorAssign();
        result = result * PRIME + ($bxorAssign == null ? 43 : $bxorAssign.hashCode());
        final Object $borAssign = this.getBorAssign();
        result = result * PRIME + ($borAssign == null ? 43 : $borAssign.hashCode());
        final Object $wrapOption = this.getWrapOption();
        result = result * PRIME + ($wrapOption == null ? 43 : $wrapOption.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "OperatorWrapStyle(wrapOption=" + this.getWrapOption() + ", question=" + this.getQuestion() + ", colon=" + this.getColon() + ", equal=" + this.getEqual() + ", notEqual=" + this.getNotEqual() + ", div=" + this.getDiv() + ", plus=" + this.getPlus() + ", minus=" + this.getMinus() + ", star=" + this.getStar() + ", mod=" + this.getMod() + ", sr=" + this.getSr() + ", bsr=" + this.getBsr() + ", ge=" + this.getGe() + ", gt=" + this.getGt() + ", sl=" + this.getSl() + ", le=" + this.getLe() + ", lt=" + this.getLt() + ", bxor=" + this.getBxor() + ", bor=" + this.getBor() + ", lor=" + this.getLor() + ", band=" + this.getBand() + ", land=" + this.getLand() + ", typeExtensionAnd=" + this.getTypeExtensionAnd() + ", literalInstanceof=" + this.getLiteralInstanceof() + ", methodRef=" + this.getMethodRef() + ", assign=" + this.getAssign() + ", plusAssign=" + this.getPlusAssign() + ", minusAssign=" + this.getMinusAssign() + ", starAssign=" + this.getStarAssign() + ", divAssign=" + this.getDivAssign() + ", modAssign=" + this.getModAssign() + ", srAssign=" + this.getSrAssign() + ", bsrAssign=" + this.getBsrAssign() + ", slAssign=" + this.getSlAssign() + ", bandAssign=" + this.getBandAssign() + ", bxorAssign=" + this.getBxorAssign() + ", borAssign=" + this.getBorAssign() + ")";
    }

    /**
     * Whether to add a newline for the token at the end of the line.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withWrapOption(@NonNull final WrapOption wrapOption) {
        return this.wrapOption == wrapOption ? this : new OperatorWrapStyle(wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The ? (conditional) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withQuestion(final Boolean question) {
        return this.question == question ? this : new OperatorWrapStyle(this.wrapOption, question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The : (colon) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withColon(final Boolean colon) {
        return this.colon == colon ? this : new OperatorWrapStyle(this.wrapOption, this.question, colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The == (equal) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withEqual(final Boolean equal) {
        return this.equal == equal ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The != (not equal) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withNotEqual(final Boolean notEqual) {
        return this.notEqual == notEqual ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The / (division) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withDiv(final Boolean div) {
        return this.div == div ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The + (addition) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withPlus(final Boolean plus) {
        return this.plus == plus ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The - (subtraction) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withMinus(final Boolean minus) {
        return this.minus == minus ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The * (multiplication or wildcard) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withStar(final Boolean star) {
        return this.star == star ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The % (remainder) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withMod(final Boolean mod) {
        return this.mod == mod ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The >> (signed shift right) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withSr(final Boolean sr) {
        return this.sr == sr ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The >>> (unsigned shift right) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBsr(final Boolean bsr) {
        return this.bsr == bsr ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The >= (greater than or equal) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withGe(final Boolean ge) {
        return this.ge == ge ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The > (greater than) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withGt(final Boolean gt) {
        return this.gt == gt ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The << (shift left) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withSl(final Boolean sl) {
        return this.sl == sl ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The <= (less than or equal) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withLe(final Boolean le) {
        return this.le == le ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The < (less than) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withLt(final Boolean lt) {
        return this.lt == lt ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The ^ (bitwise exclusive OR) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBxor(final Boolean bxor) {
        return this.bxor == bxor ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The | (bitwise OR) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBor(final Boolean bor) {
        return this.bor == bor ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The || (conditional OR) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withLor(final Boolean lor) {
        return this.lor == lor ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The & (bitwise AND) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBand(final Boolean band) {
        return this.band == band ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The && (conditional AND) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withLand(final Boolean land) {
        return this.land == land ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The & symbol when used to extend a generic upper or lower bounds constrain or a type cast expression with an additional interface.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withTypeExtensionAnd(final Boolean typeExtensionAnd) {
        return this.typeExtensionAnd == typeExtensionAnd ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The instanceof operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withLiteralInstanceof(final Boolean literalInstanceof) {
        return this.literalInstanceof == literalInstanceof ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * A :: reference to a method or constructor without arguments.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withMethodRef(final Boolean methodRef) {
        return this.methodRef == methodRef ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The = (assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withAssign(final Boolean assign) {
        return this.assign == assign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The += (addition assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withPlusAssign(final Boolean plusAssign) {
        return this.plusAssign == plusAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The -= (subtraction assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withMinusAssign(final Boolean minusAssign) {
        return this.minusAssign == minusAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The *= (multiplication assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withStarAssign(final Boolean starAssign) {
        return this.starAssign == starAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The /= (division assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withDivAssign(final Boolean divAssign) {
        return this.divAssign == divAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The %= (remainder assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withModAssign(final Boolean modAssign) {
        return this.modAssign == modAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The >>= (signed right shift assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withSrAssign(final Boolean srAssign) {
        return this.srAssign == srAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The >>>= (unsigned right shift assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBsrAssign(final Boolean bsrAssign) {
        return this.bsrAssign == bsrAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The <<= (left shift assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withSlAssign(final Boolean slAssign) {
        return this.slAssign == slAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, slAssign, this.bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The &= (bitwise AND assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBandAssign(final Boolean bandAssign) {
        return this.bandAssign == bandAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, bandAssign, this.bxorAssign, this.borAssign);
    }

    /**
     * The ^= (bitwise exclusive OR assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBxorAssign(final Boolean bxorAssign) {
        return this.bxorAssign == bxorAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, bxorAssign, this.borAssign);
    }

    /**
     * The |= (bitwise OR assignment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public OperatorWrapStyle withBorAssign(final Boolean borAssign) {
        return this.borAssign == borAssign ? this : new OperatorWrapStyle(this.wrapOption, this.question, this.colon, this.equal, this.notEqual, this.div, this.plus, this.minus, this.star, this.mod, this.sr, this.bsr, this.ge, this.gt, this.sl, this.le, this.lt, this.bxor, this.bor, this.lor, this.band, this.land, this.typeExtensionAnd, this.literalInstanceof, this.methodRef, this.assign, this.plusAssign, this.minusAssign, this.starAssign, this.divAssign, this.modAssign, this.srAssign, this.bsrAssign, this.slAssign, this.bandAssign, this.bxorAssign, borAssign);
    }
}
