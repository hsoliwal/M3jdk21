// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/test/AdHocRecipe.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.test;

import org.intellij.lang.annotations.Language;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

public final class AdHocRecipe extends Recipe {
    @Nullable
    @Language("markdown")
    private final String displayName;
    @Nullable
    private final String name;
    @Nullable
    private final Boolean causesAnotherCycle;
    private final transient Supplier<TreeVisitor<?, ExecutionContext>> getVisitor;
    @Nullable
    private final List<Maintainer> maintainers;
    @Nullable
    private final Integer maxCycles;

    public AdHocScanningRecipe withGenerator(Supplier<Collection<SourceFile>> generator) {
        return new AdHocScanningRecipe(displayName, name, causesAnotherCycle, getVisitor, generator, maintainers, maxCycles);
    }

    public String getDisplayName() {
        return StringUtils.isBlank(displayName) ? "Ad hoc recipe" : displayName;
    }

    @Override
    public String getDescription() {
        return "An ad hoc recipe used in RewriteTest.";
    }

    public String getName() {
        return StringUtils.isBlank(name) ? super.getName() : name;
    }

    @Override
    public boolean causesAnotherCycle() {
        return causesAnotherCycle == null ? super.causesAnotherCycle() : causesAnotherCycle;
    }

    @Override
    public int maxCycles() {
        return maxCycles == null ? super.maxCycles() : maxCycles;
    }

    public List<Maintainer> getMaintainers() {
        return maintainers == null ? Collections.emptyList() : maintainers;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return getVisitor.get();
    }

    public AdHocRecipe(@Nullable @Language("markdown") final String displayName, @Nullable final String name, @Nullable final Boolean causesAnotherCycle, final Supplier<TreeVisitor<?, ExecutionContext>> getVisitor, @Nullable final List<Maintainer> maintainers, @Nullable final Integer maxCycles) {
        this.displayName = displayName;
        this.name = name;
        this.causesAnotherCycle = causesAnotherCycle;
        this.getVisitor = getVisitor;
        this.maintainers = maintainers;
        this.maxCycles = maxCycles;
    }

    @Nullable
    public Boolean getCausesAnotherCycle() {
        return this.causesAnotherCycle;
    }

    public Supplier<TreeVisitor<?, ExecutionContext>> getGetVisitor() {
        return this.getVisitor;
    }

    @Nullable
    public Integer getMaxCycles() {
        return this.maxCycles;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "AdHocRecipe(displayName=" + this.getDisplayName() + ", name=" + this.getName() + ", causesAnotherCycle=" + this.getCausesAnotherCycle() + ", getVisitor=" + this.getGetVisitor() + ", maintainers=" + this.getMaintainers() + ", maxCycles=" + this.getMaxCycles() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof AdHocRecipe)) return false;
        final AdHocRecipe other = (AdHocRecipe) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$causesAnotherCycle = this.getCausesAnotherCycle();
        final Object other$causesAnotherCycle = other.getCausesAnotherCycle();
        if (this$causesAnotherCycle == null ? other$causesAnotherCycle != null : !this$causesAnotherCycle.equals(other$causesAnotherCycle)) return false;
        final Object this$maxCycles = this.getMaxCycles();
        final Object other$maxCycles = other.getMaxCycles();
        if (this$maxCycles == null ? other$maxCycles != null : !this$maxCycles.equals(other$maxCycles)) return false;
        final Object this$displayName = this.getDisplayName();
        final Object other$displayName = other.getDisplayName();
        if (this$displayName == null ? other$displayName != null : !this$displayName.equals(other$displayName)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$maintainers = this.getMaintainers();
        final Object other$maintainers = other.getMaintainers();
        if (this$maintainers == null ? other$maintainers != null : !this$maintainers.equals(other$maintainers)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof AdHocRecipe;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $causesAnotherCycle = this.getCausesAnotherCycle();
        result = result * PRIME + ($causesAnotherCycle == null ? 43 : $causesAnotherCycle.hashCode());
        final Object $maxCycles = this.getMaxCycles();
        result = result * PRIME + ($maxCycles == null ? 43 : $maxCycles.hashCode());
        final Object $displayName = this.getDisplayName();
        result = result * PRIME + ($displayName == null ? 43 : $displayName.hashCode());
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $maintainers = this.getMaintainers();
        result = result * PRIME + ($maintainers == null ? 43 : $maintainers.hashCode());
        return result;
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withDisplayName(@Nullable @Language("markdown") final String displayName) {
        return this.displayName == displayName ? this : new AdHocRecipe(displayName, this.name, this.causesAnotherCycle, this.getVisitor, this.maintainers, this.maxCycles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withName(@Nullable final String name) {
        return this.name == name ? this : new AdHocRecipe(this.displayName, name, this.causesAnotherCycle, this.getVisitor, this.maintainers, this.maxCycles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withCausesAnotherCycle(@Nullable final Boolean causesAnotherCycle) {
        return this.causesAnotherCycle == causesAnotherCycle ? this : new AdHocRecipe(this.displayName, this.name, causesAnotherCycle, this.getVisitor, this.maintainers, this.maxCycles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withGetVisitor(final Supplier<TreeVisitor<?, ExecutionContext>> getVisitor) {
        return this.getVisitor == getVisitor ? this : new AdHocRecipe(this.displayName, this.name, this.causesAnotherCycle, getVisitor, this.maintainers, this.maxCycles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withMaintainers(@Nullable final List<Maintainer> maintainers) {
        return this.maintainers == maintainers ? this : new AdHocRecipe(this.displayName, this.name, this.causesAnotherCycle, this.getVisitor, maintainers, this.maxCycles);
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public AdHocRecipe withMaxCycles(@Nullable final Integer maxCycles) {
        return this.maxCycles == maxCycles ? this : new AdHocRecipe(this.displayName, this.name, this.causesAnotherCycle, this.getVisitor, this.maintainers, maxCycles);
    }
}
