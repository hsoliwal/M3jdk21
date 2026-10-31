// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/EqualsAvoidsNullStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class EqualsAvoidsNullStyle implements Style {
    private final Boolean ignoreEqualsIgnoreCase;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.equalsAvoidsNull(), this);
    }

    public EqualsAvoidsNullStyle(final Boolean ignoreEqualsIgnoreCase) {
        this.ignoreEqualsIgnoreCase = ignoreEqualsIgnoreCase;
    }

    public Boolean getIgnoreEqualsIgnoreCase() {
        return this.ignoreEqualsIgnoreCase;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof EqualsAvoidsNullStyle)) return false;
        final EqualsAvoidsNullStyle other = (EqualsAvoidsNullStyle) o;
        final Object this$ignoreEqualsIgnoreCase = this.getIgnoreEqualsIgnoreCase();
        final Object other$ignoreEqualsIgnoreCase = other.getIgnoreEqualsIgnoreCase();
        if (this$ignoreEqualsIgnoreCase == null ? other$ignoreEqualsIgnoreCase != null : !this$ignoreEqualsIgnoreCase.equals(other$ignoreEqualsIgnoreCase)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ignoreEqualsIgnoreCase = this.getIgnoreEqualsIgnoreCase();
        result = result * PRIME + ($ignoreEqualsIgnoreCase == null ? 43 : $ignoreEqualsIgnoreCase.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "EqualsAvoidsNullStyle(ignoreEqualsIgnoreCase=" + this.getIgnoreEqualsIgnoreCase() + ")";
    }

    /**
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public EqualsAvoidsNullStyle withIgnoreEqualsIgnoreCase(final Boolean ignoreEqualsIgnoreCase) {
        return this.ignoreEqualsIgnoreCase == ignoreEqualsIgnoreCase ? this : new EqualsAvoidsNullStyle(ignoreEqualsIgnoreCase);
    }
}
