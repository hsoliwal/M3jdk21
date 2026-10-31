// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/FallThroughStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class FallThroughStyle implements Style {
    /**
     * Control whether the last case group should be checked.
     */
    private final Boolean checkLastCaseGroup;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.fallThrough(), this);
    }

    public FallThroughStyle(final Boolean checkLastCaseGroup) {
        this.checkLastCaseGroup = checkLastCaseGroup;
    }

    /**
     * Control whether the last case group should be checked.
     */
    public Boolean getCheckLastCaseGroup() {
        return this.checkLastCaseGroup;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof FallThroughStyle)) return false;
        final FallThroughStyle other = (FallThroughStyle) o;
        final Object this$checkLastCaseGroup = this.getCheckLastCaseGroup();
        final Object other$checkLastCaseGroup = other.getCheckLastCaseGroup();
        if (this$checkLastCaseGroup == null ? other$checkLastCaseGroup != null : !this$checkLastCaseGroup.equals(other$checkLastCaseGroup)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $checkLastCaseGroup = this.getCheckLastCaseGroup();
        result = result * PRIME + ($checkLastCaseGroup == null ? 43 : $checkLastCaseGroup.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "FallThroughStyle(checkLastCaseGroup=" + this.getCheckLastCaseGroup() + ")";
    }

    /**
     * Control whether the last case group should be checked.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public FallThroughStyle withCheckLastCaseGroup(final Boolean checkLastCaseGroup) {
        return this.checkLastCaseGroup == checkLastCaseGroup ? this : new FallThroughStyle(checkLastCaseGroup);
    }
}
