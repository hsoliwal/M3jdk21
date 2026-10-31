// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/RemoveAnnotationAttribute.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
/*
 * Copyright 2024 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RemoveAnnotationAttribute extends Recipe {
    @Option(displayName = "Annotation Type", description = "The fully qualified name of the annotation.", example = "org.junit.Test")
    private final String annotationType;
    @Option(displayName = "Attribute name", description = "The name of attribute to remove.", example = "timeout")
    private final String attributeName;

    @Override
    public String getDisplayName() {
        return "Remove annotation attribute";
    }

    @Override
    public String getInstanceNameSuffix() {
        String shortType = annotationType.substring(annotationType.lastIndexOf('.') + 1);
        return String.format("`@%s(%s)`", shortType, attributeName);
    }

    @Override
    public String getDescription() {
        return "Some annotations accept arguments. This recipe removes an existing attribute.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, false), new JavaIsoVisitor<ExecutionContext>() {
            private final AnnotationMatcher annotationMatcher = new AnnotationMatcher(annotationType);
            @Override
            public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                J.Annotation a = super.visitAnnotation(annotation, ctx);
                if (!annotationMatcher.matches(a)) {
                    return a;
                }
                AtomicBoolean didPassFirstAttribute = new AtomicBoolean(false);
                AtomicBoolean shouldTrimNextPrefix = new AtomicBoolean(false);
                return a.withArguments(ListUtils.map(a.getArguments(), arg -> {
                    try {
                        if (arg instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) arg;
                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                            if (attributeName.equals(variable.getSimpleName())) {
                                if (!didPassFirstAttribute.get()) {
                                    shouldTrimNextPrefix.set(true);
                                }
                                return null;
                            }
                        } else if (attributeName.equals("value")) {
                            if (!didPassFirstAttribute.get()) {
                                shouldTrimNextPrefix.set(true);
                            }
                            return null;
                        }
                        if (shouldTrimNextPrefix.get()) {
                            shouldTrimNextPrefix.set(false);
                            return arg.withPrefix(arg.getPrefix().withWhitespace(""));
                        }
                    } finally {
                        didPassFirstAttribute.set(true);
                    }
                    return arg;
                }));
            }
        });
    }

    public RemoveAnnotationAttribute(final String annotationType, final String attributeName) {
        this.annotationType = annotationType;
        this.attributeName = attributeName;
    }

    public String getAnnotationType() {
        return this.annotationType;
    }

    public String getAttributeName() {
        return this.attributeName;
    }

    @org.openrewrite.internal.lang.NonNull
    @Override
    public String toString() {
        return "RemoveAnnotationAttribute(annotationType=" + this.getAnnotationType() + ", attributeName=" + this.getAttributeName() + ")";
    }

    @Override
    public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
        if (o == this) return true;
        if (!(o instanceof RemoveAnnotationAttribute)) return false;
        final RemoveAnnotationAttribute other = (RemoveAnnotationAttribute) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$annotationType = this.getAnnotationType();
        final Object other$annotationType = other.getAnnotationType();
        if (this$annotationType == null ? other$annotationType != null : !this$annotationType.equals(other$annotationType)) return false;
        final Object this$attributeName = this.getAttributeName();
        final Object other$attributeName = other.getAttributeName();
        if (this$attributeName == null ? other$attributeName != null : !this$attributeName.equals(other$attributeName)) return false;
        return true;
    }

    protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
        return other instanceof RemoveAnnotationAttribute;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $annotationType = this.getAnnotationType();
        result = result * PRIME + ($annotationType == null ? 43 : $annotationType.hashCode());
        final Object $attributeName = this.getAttributeName();
        result = result * PRIME + ($attributeName == null ? 43 : $attributeName.hashCode());
        return result;
    }
}
