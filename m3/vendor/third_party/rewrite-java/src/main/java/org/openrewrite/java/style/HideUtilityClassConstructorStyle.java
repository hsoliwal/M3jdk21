// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/style/HideUtilityClassConstructorStyle.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaStyle;
import java.util.Collection;

public final class HideUtilityClassConstructorStyle implements JavaStyle {
    /**
     * If any of the annotation signatures are present on the utility class, the visitor will ignore operating on the class.
     * These should be {@link AnnotationMatcher}-compatible, fully-qualified annotation signature strings.
     */
    private final Collection<String> ignoreIfAnnotatedBy;

    public HideUtilityClassConstructorStyle(final Collection<String> ignoreIfAnnotatedBy) {
        this.ignoreIfAnnotatedBy = ignoreIfAnnotatedBy;
    }

    /**
     * If any of the annotation signatures are present on the utility class, the visitor will ignore operating on the class.
     * These should be {@link AnnotationMatcher}-compatible, fully-qualified annotation signature strings.
     */
    public Collection<String> getIgnoreIfAnnotatedBy() {
        return this.ignoreIfAnnotatedBy;
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof HideUtilityClassConstructorStyle)) return false;
        final HideUtilityClassConstructorStyle other = (HideUtilityClassConstructorStyle) o;
        final Object this$ignoreIfAnnotatedBy = this.getIgnoreIfAnnotatedBy();
        final Object other$ignoreIfAnnotatedBy = other.getIgnoreIfAnnotatedBy();
        if (this$ignoreIfAnnotatedBy == null ? other$ignoreIfAnnotatedBy != null : !this$ignoreIfAnnotatedBy.equals(other$ignoreIfAnnotatedBy)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $ignoreIfAnnotatedBy = this.getIgnoreIfAnnotatedBy();
        result = result * PRIME + ($ignoreIfAnnotatedBy == null ? 43 : $ignoreIfAnnotatedBy.hashCode());
        return result;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "HideUtilityClassConstructorStyle(ignoreIfAnnotatedBy=" + this.getIgnoreIfAnnotatedBy() + ")";
    }

    /**
     * If any of the annotation signatures are present on the utility class, the visitor will ignore operating on the class.
     * These should be {@link AnnotationMatcher}-compatible, fully-qualified annotation signature strings.
     * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
     */
    @org.openrewrite.internal.lang.NonNull
    public HideUtilityClassConstructorStyle withIgnoreIfAnnotatedBy(final Collection<String> ignoreIfAnnotatedBy) {
        return this.ignoreIfAnnotatedBy == ignoreIfAnnotatedBy ? this : new HideUtilityClassConstructorStyle(ignoreIfAnnotatedBy);
    }
}
