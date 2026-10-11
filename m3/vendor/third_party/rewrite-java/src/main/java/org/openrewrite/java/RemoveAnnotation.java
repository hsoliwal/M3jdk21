// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/RemoveAnnotation.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java;

import org.openrewrite.Option;
import org.openrewrite.Recipe;

public final class RemoveAnnotation extends Recipe {
    @Option(displayName = "Annotation pattern", description = "An annotation pattern, expressed as a method pattern.", example = "@java.lang.SuppressWarnings(\"deprecation\")")
    private final String annotationPattern;

    @Override
    public String getDisplayName() {
        return "Remove annotation";
    }

    @Override
    public String getDescription() {
        return "Remove matching annotations wherever they occur.";
    }

    @Override
    public RemoveAnnotationVisitor getVisitor() {
        return new RemoveAnnotationVisitor(new AnnotationMatcher(annotationPattern));
    }

    public RemoveAnnotation(final String annotationPattern) {
        this.annotationPattern = annotationPattern;
    }

    public String getAnnotationPattern() {
        return this.annotationPattern;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoveAnnotation(annotationPattern=" + this.getAnnotationPattern() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoveAnnotation)) return false;
        final RemoveAnnotation other = (RemoveAnnotation) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$annotationPattern = this.getAnnotationPattern();
        final Object other$annotationPattern = other.getAnnotationPattern();
        if (this$annotationPattern == null ? other$annotationPattern != null : !this$annotationPattern.equals(other$annotationPattern)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RemoveAnnotation;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $annotationPattern = this.getAnnotationPattern();
        result = result * PRIME + ($annotationPattern == null ? 43 : $annotationPattern.hashCode());
        return result;
    }
}
