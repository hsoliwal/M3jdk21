// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/EmptyBlockStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
public final class EmptyBlockStyle implements Style {
    @NonNull
    private final BlockPolicy blockPolicy;
    private final Boolean instanceInit;
    private final Boolean literalCatch;
    private final Boolean literalDo;
    private final Boolean literalElse;
    private final Boolean literalFinally;
    private final Boolean literalFor;
    private final Boolean literalIf;
    private final Boolean literalSwitch;
    private final Boolean literalSynchronized;
    private final Boolean literalTry;
    private final Boolean literalWhile;
    private final Boolean staticInit;


    public enum BlockPolicy {
        /**
         * Require that there is some text in the block. For example:
         *     catch (Exception ex) {
         *         // This is a bad coding practice
         *     }
         */
        TEXT, /**
         * Require that there is a statement in the block. For example:
         *     finally {
         *         lock.release();
         *     }
         */
        STATEMENT;
    }

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.emptyBlock(), this);
    }

    public EmptyBlockStyle(@NonNull final BlockPolicy blockPolicy, final Boolean instanceInit, final Boolean literalCatch, final Boolean literalDo, final Boolean literalElse, final Boolean literalFinally, final Boolean literalFor, final Boolean literalIf, final Boolean literalSwitch, final Boolean literalSynchronized, final Boolean literalTry, final Boolean literalWhile, final Boolean staticInit) {
        this.blockPolicy = blockPolicy;
        this.instanceInit = instanceInit;
        this.literalCatch = literalCatch;
        this.literalDo = literalDo;
        this.literalElse = literalElse;
        this.literalFinally = literalFinally;
        this.literalFor = literalFor;
        this.literalIf = literalIf;
        this.literalSwitch = literalSwitch;
        this.literalSynchronized = literalSynchronized;
        this.literalTry = literalTry;
        this.literalWhile = literalWhile;
        this.staticInit = staticInit;
    }

    @NonNull
    public BlockPolicy getBlockPolicy() {
        return this.blockPolicy;
    }

    public Boolean getInstanceInit() {
        return this.instanceInit;
    }

    public Boolean getLiteralCatch() {
        return this.literalCatch;
    }

    public Boolean getLiteralDo() {
        return this.literalDo;
    }

    public Boolean getLiteralElse() {
        return this.literalElse;
    }

    public Boolean getLiteralFinally() {
        return this.literalFinally;
    }

    public Boolean getLiteralFor() {
        return this.literalFor;
    }

    public Boolean getLiteralIf() {
        return this.literalIf;
    }

    public Boolean getLiteralSwitch() {
        return this.literalSwitch;
    }

    public Boolean getLiteralSynchronized() {
        return this.literalSynchronized;
    }

    public Boolean getLiteralTry() {
        return this.literalTry;
    }

    public Boolean getLiteralWhile() {
        return this.literalWhile;
    }

    public Boolean getStaticInit() {
        return this.staticInit;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof EmptyBlockStyle)) return false;
        final EmptyBlockStyle other = (EmptyBlockStyle) o;
        final Object this$instanceInit = this.getInstanceInit();
        final Object other$instanceInit = other.getInstanceInit();
        if (this$instanceInit == null ? other$instanceInit != null : !this$instanceInit.equals(other$instanceInit)) return false;
        final Object this$literalCatch = this.getLiteralCatch();
        final Object other$literalCatch = other.getLiteralCatch();
        if (this$literalCatch == null ? other$literalCatch != null : !this$literalCatch.equals(other$literalCatch)) return false;
        final Object this$literalDo = this.getLiteralDo();
        final Object other$literalDo = other.getLiteralDo();
        if (this$literalDo == null ? other$literalDo != null : !this$literalDo.equals(other$literalDo)) return false;
        final Object this$literalElse = this.getLiteralElse();
        final Object other$literalElse = other.getLiteralElse();
        if (this$literalElse == null ? other$literalElse != null : !this$literalElse.equals(other$literalElse)) return false;
        final Object this$literalFinally = this.getLiteralFinally();
        final Object other$literalFinally = other.getLiteralFinally();
        if (this$literalFinally == null ? other$literalFinally != null : !this$literalFinally.equals(other$literalFinally)) return false;
        final Object this$literalFor = this.getLiteralFor();
        final Object other$literalFor = other.getLiteralFor();
        if (this$literalFor == null ? other$literalFor != null : !this$literalFor.equals(other$literalFor)) return false;
        final Object this$literalIf = this.getLiteralIf();
        final Object other$literalIf = other.getLiteralIf();
        if (this$literalIf == null ? other$literalIf != null : !this$literalIf.equals(other$literalIf)) return false;
        final Object this$literalSwitch = this.getLiteralSwitch();
        final Object other$literalSwitch = other.getLiteralSwitch();
        if (this$literalSwitch == null ? other$literalSwitch != null : !this$literalSwitch.equals(other$literalSwitch)) return false;
        final Object this$literalSynchronized = this.getLiteralSynchronized();
        final Object other$literalSynchronized = other.getLiteralSynchronized();
        if (this$literalSynchronized == null ? other$literalSynchronized != null : !this$literalSynchronized.equals(other$literalSynchronized)) return false;
        final Object this$literalTry = this.getLiteralTry();
        final Object other$literalTry = other.getLiteralTry();
        if (this$literalTry == null ? other$literalTry != null : !this$literalTry.equals(other$literalTry)) return false;
        final Object this$literalWhile = this.getLiteralWhile();
        final Object other$literalWhile = other.getLiteralWhile();
        if (this$literalWhile == null ? other$literalWhile != null : !this$literalWhile.equals(other$literalWhile)) return false;
        final Object this$staticInit = this.getStaticInit();
        final Object other$staticInit = other.getStaticInit();
        if (this$staticInit == null ? other$staticInit != null : !this$staticInit.equals(other$staticInit)) return false;
        final Object this$blockPolicy = this.getBlockPolicy();
        final Object other$blockPolicy = other.getBlockPolicy();
        if (this$blockPolicy == null ? other$blockPolicy != null : !this$blockPolicy.equals(other$blockPolicy)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $instanceInit = this.getInstanceInit();
        result = result * PRIME + ($instanceInit == null ? 43 : $instanceInit.hashCode());
        final Object $literalCatch = this.getLiteralCatch();
        result = result * PRIME + ($literalCatch == null ? 43 : $literalCatch.hashCode());
        final Object $literalDo = this.getLiteralDo();
        result = result * PRIME + ($literalDo == null ? 43 : $literalDo.hashCode());
        final Object $literalElse = this.getLiteralElse();
        result = result * PRIME + ($literalElse == null ? 43 : $literalElse.hashCode());
        final Object $literalFinally = this.getLiteralFinally();
        result = result * PRIME + ($literalFinally == null ? 43 : $literalFinally.hashCode());
        final Object $literalFor = this.getLiteralFor();
        result = result * PRIME + ($literalFor == null ? 43 : $literalFor.hashCode());
        final Object $literalIf = this.getLiteralIf();
        result = result * PRIME + ($literalIf == null ? 43 : $literalIf.hashCode());
        final Object $literalSwitch = this.getLiteralSwitch();
        result = result * PRIME + ($literalSwitch == null ? 43 : $literalSwitch.hashCode());
        final Object $literalSynchronized = this.getLiteralSynchronized();
        result = result * PRIME + ($literalSynchronized == null ? 43 : $literalSynchronized.hashCode());
        final Object $literalTry = this.getLiteralTry();
        result = result * PRIME + ($literalTry == null ? 43 : $literalTry.hashCode());
        final Object $literalWhile = this.getLiteralWhile();
        result = result * PRIME + ($literalWhile == null ? 43 : $literalWhile.hashCode());
        final Object $staticInit = this.getStaticInit();
        result = result * PRIME + ($staticInit == null ? 43 : $staticInit.hashCode());
        final Object $blockPolicy = this.getBlockPolicy();
        result = result * PRIME + ($blockPolicy == null ? 43 : $blockPolicy.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "EmptyBlockStyle(blockPolicy=" + this.getBlockPolicy() + ", instanceInit=" + this.getInstanceInit() + ", literalCatch=" + this.getLiteralCatch() + ", literalDo=" + this.getLiteralDo() + ", literalElse=" + this.getLiteralElse() + ", literalFinally=" + this.getLiteralFinally() + ", literalFor=" + this.getLiteralFor() + ", literalIf=" + this.getLiteralIf() + ", literalSwitch=" + this.getLiteralSwitch() + ", literalSynchronized=" + this.getLiteralSynchronized() + ", literalTry=" + this.getLiteralTry() + ", literalWhile=" + this.getLiteralWhile() + ", staticInit=" + this.getStaticInit() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withBlockPolicy(@NonNull final BlockPolicy blockPolicy) {
        return this.blockPolicy == blockPolicy ? this : new EmptyBlockStyle(blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withInstanceInit(final Boolean instanceInit) {
        return this.instanceInit == instanceInit ? this : new EmptyBlockStyle(this.blockPolicy, instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralCatch(final Boolean literalCatch) {
        return this.literalCatch == literalCatch ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralDo(final Boolean literalDo) {
        return this.literalDo == literalDo ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralElse(final Boolean literalElse) {
        return this.literalElse == literalElse ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralFinally(final Boolean literalFinally) {
        return this.literalFinally == literalFinally ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralFor(final Boolean literalFor) {
        return this.literalFor == literalFor ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralIf(final Boolean literalIf) {
        return this.literalIf == literalIf ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralSwitch(final Boolean literalSwitch) {
        return this.literalSwitch == literalSwitch ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralSynchronized(final Boolean literalSynchronized) {
        return this.literalSynchronized == literalSynchronized ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, literalSynchronized, this.literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralTry(final Boolean literalTry) {
        return this.literalTry == literalTry ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, literalTry, this.literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withLiteralWhile(final Boolean literalWhile) {
        return this.literalWhile == literalWhile ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, literalWhile, this.staticInit);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EmptyBlockStyle withStaticInit(final Boolean staticInit) {
        return this.staticInit == staticInit ? this : new EmptyBlockStyle(this.blockPolicy, this.instanceInit, this.literalCatch, this.literalDo, this.literalElse, this.literalFinally, this.literalFor, this.literalIf, this.literalSwitch, this.literalSynchronized, this.literalTry, this.literalWhile, staticInit);
    }
}
