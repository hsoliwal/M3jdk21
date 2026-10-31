// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/ExplicitInitializationStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class ExplicitInitializationStyle implements Style {
    private final Boolean onlyObjectReferences;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.explicitInitialization(), this);
    }

    public ExplicitInitializationStyle(final Boolean onlyObjectReferences) {
        this.onlyObjectReferences = onlyObjectReferences;
    }

    public Boolean getOnlyObjectReferences() {
        return this.onlyObjectReferences;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof ExplicitInitializationStyle)) return false;
        final ExplicitInitializationStyle other = (ExplicitInitializationStyle) o;
        final Object this$onlyObjectReferences = this.getOnlyObjectReferences();
        final Object other$onlyObjectReferences = other.getOnlyObjectReferences();
        if (this$onlyObjectReferences == null ? other$onlyObjectReferences != null : !this$onlyObjectReferences.equals(other$onlyObjectReferences)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $onlyObjectReferences = this.getOnlyObjectReferences();
        result = result * PRIME + ($onlyObjectReferences == null ? 43 : $onlyObjectReferences.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "ExplicitInitializationStyle(onlyObjectReferences=" + this.getOnlyObjectReferences() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public ExplicitInitializationStyle withOnlyObjectReferences(final Boolean onlyObjectReferences) {
        return this.onlyObjectReferences == onlyObjectReferences ? this : new ExplicitInitializationStyle(onlyObjectReferences);
    }
}
