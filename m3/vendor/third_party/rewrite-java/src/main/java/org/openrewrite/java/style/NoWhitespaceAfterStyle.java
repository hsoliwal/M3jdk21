// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/NoWhitespaceAfterStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class NoWhitespaceAfterStyle implements Style {
    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     */
    private final Boolean allowLineBreaks;
    /**
     * A type-cast. For example: {@code (String) itr.next()}
     */
    private final Boolean typecast;
    /**
     * A :: reference to a method or constructor.
     */
    private final Boolean methodRef;
    /**
     * An array declaration.
     */
    private final Boolean arrayDeclarator;
    /**
     * An @ annotation symbol.
     */
    private final Boolean annotation;
    /**
     * An array initialization.
     */
    private final Boolean arrayInitializer;
    /**
     * The array index operator.
     */
    private final Boolean indexOperation;
    /**
     * The . (dot) operator.
     */
    private final Boolean dot;
    /**
     * The ++ (prefix increment) operator.
     */
    private final Boolean inc;
    /**
     * The -- (prefix decrement) operator.
     */
    private final Boolean dec;
    /**
     * The ~ (bitwise complement) operator.
     */
    private final Boolean bnoc;
    /**
     * The ! (logical complement) operator.
     */
    private final Boolean lnot;
    /**
     * The - (unary minus) operator.
     */
    private final Boolean unaryPlus;
    /**
     * The + (unary plus) operator.
     */
    private final Boolean unaryMinus;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.noWhitespaceAfterStyle(), this);
    }

    public NoWhitespaceAfterStyle(final Boolean allowLineBreaks, final Boolean typecast, final Boolean methodRef, final Boolean arrayDeclarator, final Boolean annotation, final Boolean arrayInitializer, final Boolean indexOperation, final Boolean dot, final Boolean inc, final Boolean dec, final Boolean bnoc, final Boolean lnot, final Boolean unaryPlus, final Boolean unaryMinus) {
        this.allowLineBreaks = allowLineBreaks;
        this.typecast = typecast;
        this.methodRef = methodRef;
        this.arrayDeclarator = arrayDeclarator;
        this.annotation = annotation;
        this.arrayInitializer = arrayInitializer;
        this.indexOperation = indexOperation;
        this.dot = dot;
        this.inc = inc;
        this.dec = dec;
        this.bnoc = bnoc;
        this.lnot = lnot;
        this.unaryPlus = unaryPlus;
        this.unaryMinus = unaryMinus;
    }

    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     */
    public Boolean getAllowLineBreaks() {
        return this.allowLineBreaks;
    }

    /**
     * A type-cast. For example: {@code (String) itr.next()}
     */
    public Boolean getTypecast() {
        return this.typecast;
    }

    /**
     * A :: reference to a method or constructor.
     */
    public Boolean getMethodRef() {
        return this.methodRef;
    }

    /**
     * An array declaration.
     */
    public Boolean getArrayDeclarator() {
        return this.arrayDeclarator;
    }

    /**
     * An @ annotation symbol.
     */
    public Boolean getAnnotation() {
        return this.annotation;
    }

    /**
     * An array initialization.
     */
    public Boolean getArrayInitializer() {
        return this.arrayInitializer;
    }

    /**
     * The array index operator.
     */
    public Boolean getIndexOperation() {
        return this.indexOperation;
    }

    /**
     * The . (dot) operator.
     */
    public Boolean getDot() {
        return this.dot;
    }

    /**
     * The ++ (prefix increment) operator.
     */
    public Boolean getInc() {
        return this.inc;
    }

    /**
     * The -- (prefix decrement) operator.
     */
    public Boolean getDec() {
        return this.dec;
    }

    /**
     * The ~ (bitwise complement) operator.
     */
    public Boolean getBnoc() {
        return this.bnoc;
    }

    /**
     * The ! (logical complement) operator.
     */
    public Boolean getLnot() {
        return this.lnot;
    }

    /**
     * The - (unary minus) operator.
     */
    public Boolean getUnaryPlus() {
        return this.unaryPlus;
    }

    /**
     * The + (unary plus) operator.
     */
    public Boolean getUnaryMinus() {
        return this.unaryMinus;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof NoWhitespaceAfterStyle)) return false;
        final NoWhitespaceAfterStyle other = (NoWhitespaceAfterStyle) o;
        final Object this$allowLineBreaks = this.getAllowLineBreaks();
        final Object other$allowLineBreaks = other.getAllowLineBreaks();
        if (this$allowLineBreaks == null ? other$allowLineBreaks != null : !this$allowLineBreaks.equals(other$allowLineBreaks)) return false;
        final Object this$typecast = this.getTypecast();
        final Object other$typecast = other.getTypecast();
        if (this$typecast == null ? other$typecast != null : !this$typecast.equals(other$typecast)) return false;
        final Object this$methodRef = this.getMethodRef();
        final Object other$methodRef = other.getMethodRef();
        if (this$methodRef == null ? other$methodRef != null : !this$methodRef.equals(other$methodRef)) return false;
        final Object this$arrayDeclarator = this.getArrayDeclarator();
        final Object other$arrayDeclarator = other.getArrayDeclarator();
        if (this$arrayDeclarator == null ? other$arrayDeclarator != null : !this$arrayDeclarator.equals(other$arrayDeclarator)) return false;
        final Object this$annotation = this.getAnnotation();
        final Object other$annotation = other.getAnnotation();
        if (this$annotation == null ? other$annotation != null : !this$annotation.equals(other$annotation)) return false;
        final Object this$arrayInitializer = this.getArrayInitializer();
        final Object other$arrayInitializer = other.getArrayInitializer();
        if (this$arrayInitializer == null ? other$arrayInitializer != null : !this$arrayInitializer.equals(other$arrayInitializer)) return false;
        final Object this$indexOperation = this.getIndexOperation();
        final Object other$indexOperation = other.getIndexOperation();
        if (this$indexOperation == null ? other$indexOperation != null : !this$indexOperation.equals(other$indexOperation)) return false;
        final Object this$dot = this.getDot();
        final Object other$dot = other.getDot();
        if (this$dot == null ? other$dot != null : !this$dot.equals(other$dot)) return false;
        final Object this$inc = this.getInc();
        final Object other$inc = other.getInc();
        if (this$inc == null ? other$inc != null : !this$inc.equals(other$inc)) return false;
        final Object this$dec = this.getDec();
        final Object other$dec = other.getDec();
        if (this$dec == null ? other$dec != null : !this$dec.equals(other$dec)) return false;
        final Object this$bnoc = this.getBnoc();
        final Object other$bnoc = other.getBnoc();
        if (this$bnoc == null ? other$bnoc != null : !this$bnoc.equals(other$bnoc)) return false;
        final Object this$lnot = this.getLnot();
        final Object other$lnot = other.getLnot();
        if (this$lnot == null ? other$lnot != null : !this$lnot.equals(other$lnot)) return false;
        final Object this$unaryPlus = this.getUnaryPlus();
        final Object other$unaryPlus = other.getUnaryPlus();
        if (this$unaryPlus == null ? other$unaryPlus != null : !this$unaryPlus.equals(other$unaryPlus)) return false;
        final Object this$unaryMinus = this.getUnaryMinus();
        final Object other$unaryMinus = other.getUnaryMinus();
        if (this$unaryMinus == null ? other$unaryMinus != null : !this$unaryMinus.equals(other$unaryMinus)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $allowLineBreaks = this.getAllowLineBreaks();
        result = result * PRIME + ($allowLineBreaks == null ? 43 : $allowLineBreaks.hashCode());
        final Object $typecast = this.getTypecast();
        result = result * PRIME + ($typecast == null ? 43 : $typecast.hashCode());
        final Object $methodRef = this.getMethodRef();
        result = result * PRIME + ($methodRef == null ? 43 : $methodRef.hashCode());
        final Object $arrayDeclarator = this.getArrayDeclarator();
        result = result * PRIME + ($arrayDeclarator == null ? 43 : $arrayDeclarator.hashCode());
        final Object $annotation = this.getAnnotation();
        result = result * PRIME + ($annotation == null ? 43 : $annotation.hashCode());
        final Object $arrayInitializer = this.getArrayInitializer();
        result = result * PRIME + ($arrayInitializer == null ? 43 : $arrayInitializer.hashCode());
        final Object $indexOperation = this.getIndexOperation();
        result = result * PRIME + ($indexOperation == null ? 43 : $indexOperation.hashCode());
        final Object $dot = this.getDot();
        result = result * PRIME + ($dot == null ? 43 : $dot.hashCode());
        final Object $inc = this.getInc();
        result = result * PRIME + ($inc == null ? 43 : $inc.hashCode());
        final Object $dec = this.getDec();
        result = result * PRIME + ($dec == null ? 43 : $dec.hashCode());
        final Object $bnoc = this.getBnoc();
        result = result * PRIME + ($bnoc == null ? 43 : $bnoc.hashCode());
        final Object $lnot = this.getLnot();
        result = result * PRIME + ($lnot == null ? 43 : $lnot.hashCode());
        final Object $unaryPlus = this.getUnaryPlus();
        result = result * PRIME + ($unaryPlus == null ? 43 : $unaryPlus.hashCode());
        final Object $unaryMinus = this.getUnaryMinus();
        result = result * PRIME + ($unaryMinus == null ? 43 : $unaryMinus.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "NoWhitespaceAfterStyle(allowLineBreaks=" + this.getAllowLineBreaks() + ", typecast=" + this.getTypecast() + ", methodRef=" + this.getMethodRef() + ", arrayDeclarator=" + this.getArrayDeclarator() + ", annotation=" + this.getAnnotation() + ", arrayInitializer=" + this.getArrayInitializer() + ", indexOperation=" + this.getIndexOperation() + ", dot=" + this.getDot() + ", inc=" + this.getInc() + ", dec=" + this.getDec() + ", bnoc=" + this.getBnoc() + ", lnot=" + this.getLnot() + ", unaryPlus=" + this.getUnaryPlus() + ", unaryMinus=" + this.getUnaryMinus() + ")";
    }

    /**
     * Whether whitespace is allowed if the token is at a linebreak.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withAllowLineBreaks(final Boolean allowLineBreaks) {
        return this.allowLineBreaks == allowLineBreaks ? this : new NoWhitespaceAfterStyle(allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * A type-cast. For example: {@code (String) itr.next()}
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withTypecast(final Boolean typecast) {
        return this.typecast == typecast ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * A :: reference to a method or constructor.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withMethodRef(final Boolean methodRef) {
        return this.methodRef == methodRef ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * An array declaration.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withArrayDeclarator(final Boolean arrayDeclarator) {
        return this.arrayDeclarator == arrayDeclarator ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * An @ annotation symbol.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withAnnotation(final Boolean annotation) {
        return this.annotation == annotation ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * An array initialization.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withArrayInitializer(final Boolean arrayInitializer) {
        return this.arrayInitializer == arrayInitializer ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The array index operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withIndexOperation(final Boolean indexOperation) {
        return this.indexOperation == indexOperation ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The . (dot) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withDot(final Boolean dot) {
        return this.dot == dot ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The ++ (prefix increment) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withInc(final Boolean inc) {
        return this.inc == inc ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The -- (prefix decrement) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withDec(final Boolean dec) {
        return this.dec == dec ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, dec, this.bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The ~ (bitwise complement) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withBnoc(final Boolean bnoc) {
        return this.bnoc == bnoc ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, bnoc, this.lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The ! (logical complement) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withLnot(final Boolean lnot) {
        return this.lnot == lnot ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, lnot, this.unaryPlus, this.unaryMinus);
    }

    /**
     * The - (unary minus) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withUnaryPlus(final Boolean unaryPlus) {
        return this.unaryPlus == unaryPlus ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, unaryPlus, this.unaryMinus);
    }

    /**
     * The + (unary plus) operator.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public NoWhitespaceAfterStyle withUnaryMinus(final Boolean unaryMinus) {
        return this.unaryMinus == unaryMinus ? this : new NoWhitespaceAfterStyle(this.allowLineBreaks, this.typecast, this.methodRef, this.arrayDeclarator, this.annotation, this.arrayInitializer, this.indexOperation, this.dot, this.inc, this.dec, this.bnoc, this.lnot, this.unaryPlus, unaryMinus);
    }
}
