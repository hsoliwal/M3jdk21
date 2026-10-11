// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/DefaultComesLastStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class DefaultComesLastStyle implements Style {
    /**
     * Whether to allow the default label to be not last if its evaluation is shared with a case.
     */
    private final Boolean skipIfLastAndSharedWithCase;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.defaultComesLast(), this);
    }

    public DefaultComesLastStyle(final Boolean skipIfLastAndSharedWithCase) {
        this.skipIfLastAndSharedWithCase = skipIfLastAndSharedWithCase;
    }

    /**
     * Whether to allow the default label to be not last if its evaluation is shared with a case.
     */
    public Boolean getSkipIfLastAndSharedWithCase() {
        return this.skipIfLastAndSharedWithCase;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof DefaultComesLastStyle)) return false;
        final DefaultComesLastStyle other = (DefaultComesLastStyle) o;
        final Object this$skipIfLastAndSharedWithCase = this.getSkipIfLastAndSharedWithCase();
        final Object other$skipIfLastAndSharedWithCase = other.getSkipIfLastAndSharedWithCase();
        if (this$skipIfLastAndSharedWithCase == null ? other$skipIfLastAndSharedWithCase != null : !this$skipIfLastAndSharedWithCase.equals(other$skipIfLastAndSharedWithCase)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $skipIfLastAndSharedWithCase = this.getSkipIfLastAndSharedWithCase();
        result = result * PRIME + ($skipIfLastAndSharedWithCase == null ? 43 : $skipIfLastAndSharedWithCase.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "DefaultComesLastStyle(skipIfLastAndSharedWithCase=" + this.getSkipIfLastAndSharedWithCase() + ")";
    }

    /**
     * Whether to allow the default label to be not last if its evaluation is shared with a case.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public DefaultComesLastStyle withSkipIfLastAndSharedWithCase(final Boolean skipIfLastAndSharedWithCase) {
        return this.skipIfLastAndSharedWithCase == skipIfLastAndSharedWithCase ? this : new DefaultComesLastStyle(skipIfLastAndSharedWithCase);
    }
}
