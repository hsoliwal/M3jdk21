// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/TypecastParenPadStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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

public final class TypecastParenPadStyle implements Style {
    /**
     * Whether to add spacing between the typecast identifier and both the left and right parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code ( int ) m;}
     * <p>
     * When false:
     * <p>
     * {@code (int) m;}
     */
    private final Boolean space;

    @Override
    public Style applyDefaults() {
        return StyleHelper.merge(Checkstyle.typecastParenPadStyle(), this);
    }

    public TypecastParenPadStyle(final Boolean space) {
        this.space = space;
    }

    /**
     * Whether to add spacing between the typecast identifier and both the left and right parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code ( int ) m;}
     * <p>
     * When false:
     * <p>
     * {@code (int) m;}
     */
    public Boolean getSpace() {
        return this.space;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof TypecastParenPadStyle)) return false;
        final TypecastParenPadStyle other = (TypecastParenPadStyle) o;
        final Object this$space = this.getSpace();
        final Object other$space = other.getSpace();
        if (this$space == null ? other$space != null : !this$space.equals(other$space)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $space = this.getSpace();
        result = result * PRIME + ($space == null ? 43 : $space.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "TypecastParenPadStyle(space=" + this.getSpace() + ")";
    }

    /**
     * Whether to add spacing between the typecast identifier and both the left and right parenthesis.
     * <p>
     * When true:
     * <p>
     * {@code ( int ) m;}
     * <p>
     * When false:
     * <p>
     * {@code (int) m;}
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public TypecastParenPadStyle withSpace(final Boolean space) {
        return this.space == space ? this : new TypecastParenPadStyle(space);
    }
}
