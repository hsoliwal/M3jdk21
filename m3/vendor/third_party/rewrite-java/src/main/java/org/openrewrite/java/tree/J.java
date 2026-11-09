// NOTICE: modified file. This is the Lombok 1.18.30 delombok expansion of the unchanged upstream file kept at upstream-lombok/org/openrewrite/java/tree/J.java, with the unused lombok imports and no-op @NonFinal markers delombok leaves removed; no other change.
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
package org.openrewrite.java.tree;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.LoathingOfOthers;
import org.openrewrite.internal.SelfLoathing;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.JavaTypeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.internal.TypesInUse;
import org.openrewrite.java.search.FindTypes;
import org.openrewrite.marker.Markers;
import java.beans.Transient;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public interface J extends Tree {
    @SuppressWarnings("unchecked")
    @Override
    default <R extends Tree, P> R accept(TreeVisitor<R, P> v, P p) {
        return (R) acceptJava(v.adapt(JavaVisitor.class), p);
    }

    @Override
    default <P> boolean isAcceptable(TreeVisitor<?, P> v, P p) {
        return v.isAdaptableTo(JavaVisitor.class);
    }

    @Nullable
    default <P> J acceptJava(JavaVisitor<P> v, P p) {
        return v.defaultValue(this, p);
    }

    <J2 extends J> J2 withPrefix(Space space);

    Space getPrefix();

    default List<Comment> getComments() {
        return getPrefix().getComments();
    }

    default <J2 extends J> J2 withComments(List<Comment> comments) {
        return withPrefix(getPrefix().withComments(comments));
    }

    /**
     * @return This tree, printed.
     * @deprecated This method doesn't print in a way that is
     * specialized for each language extension of the base Java model. Use {@link #print(Cursor)} instead.
     */
    @Deprecated
    default String print() {
        PrintOutputCapture<Integer> outputCapture = new PrintOutputCapture<>(0);
        new JavaPrinter<Integer>().visit(this, outputCapture);
        return outputCapture.getOut();
    }

    /**
     * @return This tree, printed.
     * @deprecated This method doesn't print in a way that is
     * specialized for each language extension of the base Java model. Use {@link #print(Cursor)} instead.
     */
    @Deprecated
    default String printTrimmed() {
        return StringUtils.trimIndent(print());
    }


    @SuppressWarnings("unchecked")
    final class AnnotatedType implements J, Expression, TypeTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<J.Annotation> annotations;
        private final TypeTree typeExpression;

        @Override
        public JavaType getType() {
            return typeExpression.getType();
        }

        @Override
        public AnnotatedType withType(@Nullable JavaType type) {
            return withTypeExpression(typeExpression.withType(type));
        }

        /**
         * @deprecated Use {@link org.openrewrite.java.service.AnnotationService#getAllAnnotations(J)} instead.
         */
        @Deprecated
        public List<Annotation> getAllAnnotations() {
            List<J.Annotation> allAnnotations = annotations;
            List<J.Annotation> moreAnnotations;
            if (typeExpression instanceof FieldAccess && !(moreAnnotations = ((FieldAccess) typeExpression).getName().getAnnotations()).isEmpty()) {
                if (allAnnotations.isEmpty()) {
                    allAnnotations = moreAnnotations;
                } else {
                    allAnnotations = new ArrayList<>(annotations);
                    allAnnotations.addAll(moreAnnotations);
                }
            }
            return allAnnotations;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAnnotatedType(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.AnnotatedType)) return false;
            final J.AnnotatedType other = (J.AnnotatedType) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public AnnotatedType(final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final TypeTree typeExpression) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.typeExpression = typeExpression;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }

        public TypeTree getTypeExpression() {
            return this.typeExpression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AnnotatedType withId(final UUID id) {
            return this.id == id ? this : new J.AnnotatedType(id, this.prefix, this.markers, this.annotations, this.typeExpression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AnnotatedType withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.AnnotatedType(this.id, prefix, this.markers, this.annotations, this.typeExpression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AnnotatedType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.AnnotatedType(this.id, this.prefix, markers, this.annotations, this.typeExpression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AnnotatedType withAnnotations(final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.AnnotatedType(this.id, this.prefix, this.markers, annotations, this.typeExpression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AnnotatedType withTypeExpression(final TypeTree typeExpression) {
            return this.typeExpression == typeExpression ? this : new J.AnnotatedType(this.id, this.prefix, this.markers, this.annotations, typeExpression);
        }
    }


    final class Annotation implements J, Expression {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final NameTree annotationType;

        public String getSimpleName() {
            if (annotationType instanceof Identifier) {
                return ((Identifier) annotationType).getSimpleName();
            } else if (annotationType instanceof J.FieldAccess) {
                return ((J.FieldAccess) annotationType).getSimpleName();
            } else {
                // allow for extending languages like Kotlin to supply a different representation
                return annotationType.printTrimmed();
            }
        }

        @Nullable
        private final JContainer<Expression> arguments;

        @Nullable
        public List<Expression> getArguments() {
            return arguments == null ? null : arguments.getElements();
        }

        public Annotation withArguments(@Nullable List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElementsNullable(this.arguments, arguments));
        }

        @Override
        public JavaType getType() {
            return annotationType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Annotation withType(@Nullable JavaType type) {
            return withAnnotationType(annotationType.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAnnotation(this, p);
        }

        @Transient
        public CoordinateBuilder.Annotation getCoordinates() {
            return new CoordinateBuilder.Annotation(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Annotation t;

            @Nullable
            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public Annotation withArguments(@Nullable JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new Annotation(t.id, t.prefix, t.markers, t.annotationType, arguments);
            }

            public Padding(final Annotation t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Annotation)) return false;
            final J.Annotation other = (J.Annotation) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Annotation(final UUID id, final Space prefix, final Markers markers, final NameTree annotationType, @Nullable final JContainer<Expression> arguments) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotationType = annotationType;
            this.arguments = arguments;
        }

        private Annotation(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final NameTree annotationType, @Nullable final JContainer<Expression> arguments) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotationType = annotationType;
            this.arguments = arguments;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Annotation withId(final UUID id) {
            return this.id == id ? this : new J.Annotation(this.padding, id, this.prefix, this.markers, this.annotationType, this.arguments);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Annotation withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Annotation(this.padding, this.id, prefix, this.markers, this.annotationType, this.arguments);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Annotation withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Annotation(this.padding, this.id, this.prefix, markers, this.annotationType, this.arguments);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Annotation withAnnotationType(final NameTree annotationType) {
            return this.annotationType == annotationType ? this : new J.Annotation(this.padding, this.id, this.prefix, this.markers, annotationType, this.arguments);
        }

        public NameTree getAnnotationType() {
            return this.annotationType;
        }
    }


    final class ArrayAccess implements J, Expression, TypedTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression indexed;
        private final ArrayDimension dimension;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayAccess(this, p);
        }

        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ArrayAccess)) return false;
            final J.ArrayAccess other = (J.ArrayAccess) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ArrayAccess(final UUID id, final Space prefix, final Markers markers, final Expression indexed, final ArrayDimension dimension, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.indexed = indexed;
            this.dimension = dimension;
            this.type = type;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Expression getIndexed() {
            return this.indexed;
        }

        public ArrayDimension getDimension() {
            return this.dimension;
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withId(final UUID id) {
            return this.id == id ? this : new J.ArrayAccess(id, this.prefix, this.markers, this.indexed, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ArrayAccess(this.id, prefix, this.markers, this.indexed, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ArrayAccess(this.id, this.prefix, markers, this.indexed, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withIndexed(final Expression indexed) {
            return this.indexed == indexed ? this : new J.ArrayAccess(this.id, this.prefix, this.markers, indexed, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withDimension(final ArrayDimension dimension) {
            return this.dimension == dimension ? this : new J.ArrayAccess(this.id, this.prefix, this.markers, this.indexed, dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayAccess withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.ArrayAccess(this.id, this.prefix, this.markers, this.indexed, this.dimension, type);
        }
    }


    final class ArrayType implements J, TypeTree, Expression {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final TypeTree elementType;
        @Nullable
        private final List<J.Annotation> annotations;
        // nullable for backwards compatibility only
        @Nullable
        private final JLeftPadded<Space> dimension;
        private final JavaType type;

        /**
         * For backwards compatibility with older LSTs.
         * Do not remove until we're confident older LSTs are no longer in use.
         */
        @Deprecated
        @JsonCreator
        static ArrayType create(UUID id, Space prefix, Markers markers, TypeTree elementType, @Nullable List<JRightPadded<Space>> dimensions,  // Do not remove or rename, required for backwards compatibility.
        @Nullable List<J.Annotation> annotations, @Nullable JLeftPadded<Space> dimension, @Nullable JavaType type) {
            if (dimensions != null) {
                // To create a consistent JavaType$Array from old Groovy and Java LSTs, we need to map the element type.
                // The JavaType from GroovyTypeMapping was a JavaType$Array, while the JavaType from JavaTypeMapping was a JavaType$Class.
                JavaType updated = elementType.getType();
                while (updated instanceof JavaType.Array) {
                    updated = ((JavaType.Array) updated).getElemType();
                }
                elementType = elementType.withType(updated);
                if (dimensions.isEmpty()) {
                    // varargs in Javadoc
                    type = new JavaType.Array(null, elementType.getType(), null);
                } else {
                    int dimensionCount = dimensions.size();
                    elementType = mapOldFormat(elementType, dimensions.subList(0, dimensionCount - 1));
                    type = new JavaType.Array(null, elementType.getType(), null);
                    dimension = JLeftPadded.build(dimensions.get(dimensionCount - 1).getAfter()).withBefore(dimensions.get(dimensionCount - 1).getElement());
                }
            }
            return new ArrayType(id, prefix, markers, elementType, annotations, dimension, type == null ? JavaType.Unknown.getInstance() : type);
        }

        private static TypeTree mapOldFormat(TypeTree elementType, List<JRightPadded<Space>> dimensions) {
            int count = dimensions.size();
            if (count == 0) {
                return elementType;
            }
            return mapOldFormat(new ArrayType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, elementType, null, JLeftPadded.build(dimensions.get(0).getAfter()).withBefore(dimensions.get(0).getElement()), new JavaType.Array(null, elementType.getType(), null)), dimensions.subList(1, count));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayType(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ArrayType)) return false;
            final J.ArrayType other = (J.ArrayType) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ArrayType(final UUID id, final Space prefix, final Markers markers, final TypeTree elementType, @Nullable final List<J.Annotation> annotations, @Nullable final JLeftPadded<Space> dimension, final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.elementType = elementType;
            this.annotations = annotations;
            this.dimension = dimension;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withId(final UUID id) {
            return this.id == id ? this : new J.ArrayType(id, this.prefix, this.markers, this.elementType, this.annotations, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ArrayType(this.id, prefix, this.markers, this.elementType, this.annotations, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ArrayType(this.id, this.prefix, markers, this.elementType, this.annotations, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withElementType(final TypeTree elementType) {
            return this.elementType == elementType ? this : new J.ArrayType(this.id, this.prefix, this.markers, elementType, this.annotations, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withAnnotations(@Nullable final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.ArrayType(this.id, this.prefix, this.markers, this.elementType, annotations, this.dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withDimension(@Nullable final JLeftPadded<Space> dimension) {
            return this.dimension == dimension ? this : new J.ArrayType(this.id, this.prefix, this.markers, this.elementType, this.annotations, dimension, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayType withType(final JavaType type) {
            return this.type == type ? this : new J.ArrayType(this.id, this.prefix, this.markers, this.elementType, this.annotations, this.dimension, type);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public TypeTree getElementType() {
            return this.elementType;
        }

        @Nullable
        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }

        @Nullable
        public JLeftPadded<Space> getDimension() {
            return this.dimension;
        }

        public JavaType getType() {
            return this.type;
        }
    }


    final class Assert implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression condition;
        @Nullable
        private final JLeftPadded<Expression> detail;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssert(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Assert)) return false;
            final J.Assert other = (J.Assert) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Assert(final UUID id, final Space prefix, final Markers markers, final Expression condition, @Nullable final JLeftPadded<Expression> detail) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.condition = condition;
            this.detail = detail;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Expression getCondition() {
            return this.condition;
        }

        @Nullable
        public JLeftPadded<Expression> getDetail() {
            return this.detail;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assert withId(final UUID id) {
            return this.id == id ? this : new J.Assert(id, this.prefix, this.markers, this.condition, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assert withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Assert(this.id, prefix, this.markers, this.condition, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assert withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Assert(this.id, this.prefix, markers, this.condition, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assert withCondition(final Expression condition) {
            return this.condition == condition ? this : new J.Assert(this.id, this.prefix, this.markers, condition, this.detail);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assert withDetail(@Nullable final JLeftPadded<Expression> detail) {
            return this.detail == detail ? this : new J.Assert(this.id, this.prefix, this.markers, this.condition, detail);
        }
    }


    final class Assignment implements J, Statement, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression variable;
        private final JLeftPadded<Expression> assignment;

        public Expression getAssignment() {
            return assignment.getElement();
        }

        public Assignment withAssignment(Expression assignment) {
            return getPadding().withAssignment(this.assignment.withElement(assignment));
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssignment(this, p);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Assignment t;

            public JLeftPadded<Expression> getAssignment() {
                return t.assignment;
            }

            public Assignment withAssignment(JLeftPadded<Expression> assignment) {
                return t.assignment == assignment ? t : new Assignment(t.id, t.prefix, t.markers, t.variable, assignment, t.type);
            }

            public Padding(final Assignment t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Assignment)) return false;
            final J.Assignment other = (J.Assignment) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Assignment(final UUID id, final Space prefix, final Markers markers, final Expression variable, final JLeftPadded<Expression> assignment, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.variable = variable;
            this.assignment = assignment;
            this.type = type;
        }

        private Assignment(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Expression variable, final JLeftPadded<Expression> assignment, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.variable = variable;
            this.assignment = assignment;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assignment withId(final UUID id) {
            return this.id == id ? this : new J.Assignment(this.padding, id, this.prefix, this.markers, this.variable, this.assignment, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assignment withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Assignment(this.padding, this.id, prefix, this.markers, this.variable, this.assignment, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assignment withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Assignment(this.padding, this.id, this.prefix, markers, this.variable, this.assignment, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assignment withVariable(final Expression variable) {
            return this.variable == variable ? this : new J.Assignment(this.padding, this.id, this.prefix, this.markers, variable, this.assignment, this.type);
        }

        public Expression getVariable() {
            return this.variable;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Assignment withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Assignment(this.padding, this.id, this.prefix, this.markers, this.variable, this.assignment, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class AssignmentOperation implements J, Statement, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression variable;
        private final JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public AssignmentOperation withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        private final Expression assignment;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitAssignmentOperation(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return singletonList(this);
        }


        public enum Type {
            Addition, BitAnd, BitOr, BitXor, Division, /**
             * Raises the left operand to the power of the right operand.
             * Unused in Java, used in Python
             */
            Exponentiation, /**
             * Division of the left operand by the right operand, rounding down to the nearest integer.
             * Unused in Java, used in Python.
             */
            FloorDivision, LeftShift, /**
             * Matrix multiplication of the left operand by the right operand.
             * Unused in Java, used in Python
             */
            MatrixMultiplication, Modulo, Multiplication, RightShift, Subtraction, UnsignedRightShift;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final AssignmentOperation t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public AssignmentOperation withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new AssignmentOperation(t.id, t.prefix, t.markers, t.variable, operator, t.assignment, t.type);
            }

            public Padding(final AssignmentOperation t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.AssignmentOperation)) return false;
            final J.AssignmentOperation other = (J.AssignmentOperation) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public AssignmentOperation(final UUID id, final Space prefix, final Markers markers, final Expression variable, final JLeftPadded<Type> operator, final Expression assignment, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.variable = variable;
            this.operator = operator;
            this.assignment = assignment;
            this.type = type;
        }

        private AssignmentOperation(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Expression variable, final JLeftPadded<Type> operator, final Expression assignment, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.variable = variable;
            this.operator = operator;
            this.assignment = assignment;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withId(final UUID id) {
            return this.id == id ? this : new J.AssignmentOperation(this.padding, id, this.prefix, this.markers, this.variable, this.operator, this.assignment, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.AssignmentOperation(this.padding, this.id, prefix, this.markers, this.variable, this.operator, this.assignment, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.AssignmentOperation(this.padding, this.id, this.prefix, markers, this.variable, this.operator, this.assignment, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withVariable(final Expression variable) {
            return this.variable == variable ? this : new J.AssignmentOperation(this.padding, this.id, this.prefix, this.markers, variable, this.operator, this.assignment, this.type);
        }

        public Expression getVariable() {
            return this.variable;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withAssignment(final Expression assignment) {
            return this.assignment == assignment ? this : new J.AssignmentOperation(this.padding, this.id, this.prefix, this.markers, this.variable, this.operator, assignment, this.type);
        }

        public Expression getAssignment() {
            return this.assignment;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.AssignmentOperation withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.AssignmentOperation(this.padding, this.id, this.prefix, this.markers, this.variable, this.operator, this.assignment, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class Binary implements J, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression left;
        private final JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Binary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        private final Expression right;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBinary(this, p);
        }

        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Transient
        @Override
        public List<J> getSideEffects() {
            List<J> sideEffects = new ArrayList<>(2);
            sideEffects.addAll(left.getSideEffects());
            sideEffects.addAll(right.getSideEffects());
            return sideEffects;
        }


        public enum Type {
            Addition, Subtraction, Multiplication, Division, Modulo, LessThan, GreaterThan, LessThanOrEqual, GreaterThanOrEqual, Equal, NotEqual, BitAnd, BitOr, BitXor, LeftShift, RightShift, UnsignedRightShift, Or, And;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Binary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Binary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Binary(t.id, t.prefix, t.markers, t.left, operator, t.right, t.type);
            }

            public Padding(final Binary t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Binary)) return false;
            final J.Binary other = (J.Binary) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Binary(final UUID id, final Space prefix, final Markers markers, final Expression left, final JLeftPadded<Type> operator, final Expression right, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.left = left;
            this.operator = operator;
            this.right = right;
            this.type = type;
        }

        private Binary(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Expression left, final JLeftPadded<Type> operator, final Expression right, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.left = left;
            this.operator = operator;
            this.right = right;
            this.type = type;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Expression getLeft() {
            return this.left;
        }

        public Expression getRight() {
            return this.right;
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }

        public void setPadding(@Nullable final WeakReference<Padding> padding) {
            this.padding = padding;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withId(final UUID id) {
            return this.id == id ? this : new J.Binary(this.padding, id, this.prefix, this.markers, this.left, this.operator, this.right, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Binary(this.padding, this.id, prefix, this.markers, this.left, this.operator, this.right, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Binary(this.padding, this.id, this.prefix, markers, this.left, this.operator, this.right, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withLeft(final Expression left) {
            return this.left == left ? this : new J.Binary(this.padding, this.id, this.prefix, this.markers, left, this.operator, this.right, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withRight(final Expression right) {
            return this.right == right ? this : new J.Binary(this.padding, this.id, this.prefix, this.markers, this.left, this.operator, right, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Binary withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Binary(this.padding, this.id, this.prefix, this.markers, this.left, this.operator, this.right, type);
        }
    }


    /**
     * A block of statements, enclosed in curly braces.
     * <p>
     * To create an empty block, use {@link #createEmptyBlock()}.
     */
    final class Block implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<Boolean> statik;

        public boolean isStatic() {
            return statik.getElement();
        }

        public Block withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElement(statik));
        }

        private final List<JRightPadded<Statement>> statements;

        public List<Statement> getStatements() {
            return JRightPadded.getElements(statements);
        }

        public Block withStatements(List<Statement> statements) {
            return getPadding().withStatements(JRightPadded.withElements(this.statements, statements));
        }

        private final Space end;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBlock(this, p);
        }

        @Transient
        public CoordinateBuilder.Block getCoordinates() {
            return new CoordinateBuilder.Block(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final Block t;

            public JRightPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Block withStatic(JRightPadded<Boolean> statik) {
                return t.statik == statik ? t : new Block(t.id, t.prefix, t.markers, statik, t.statements, t.end);
            }

            public List<JRightPadded<Statement>> getStatements() {
                return t.statements;
            }

            public Block withStatements(List<JRightPadded<Statement>> statements) {
                return t.statements == statements ? t : new Block(t.id, t.prefix, t.markers, t.statik, statements, t.end);
            }

            public Padding(final Block t) {
                this.t = t;
            }
        }

        public static J.Block createEmptyBlock() {
            return new J.Block(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(false), Collections.emptyList(), Space.EMPTY);
        }

        @SelfLoathing(name = "Jonathan Leitschuh")
        @LoathingOfOthers("Who didn\'t encode this in the model?!")
        @Incubating(since = "7.25.0")
        public static boolean isInitBlock(Cursor cursor) {
            if (!(cursor.getValue() instanceof J.Block)) {
                throw new IllegalArgumentException("Cursor must point to a J.Block!");
            }
            J.Block block = cursor.getValue();
            if (block.isStatic()) {
                return false;
            }
            J.Block parentBlock = null;
            Iterator<Object> path = cursor.getPath();
            if (path.hasNext()) {
                path.next(); // skip the first element, which is the block itself
            }
            while (path.hasNext()) {
                Object next = path.next();
                if (parentBlock != null && next instanceof J.Block) {
                    // If we find an outer block before a ClassDeclaration or NewClass, we're not in an initializer block.
                    return false;
                } else if (next instanceof J.Block) {
                    parentBlock = (J.Block) next;
                    if (!parentBlock.getStatements().contains(block)) {
                        return false;
                    }
                } else if (next instanceof J.ClassDeclaration) {
                    J.ClassDeclaration classDeclaration = (J.ClassDeclaration) next;
                    return classDeclaration.getBody() == parentBlock;
                } else if (next instanceof J.NewClass) {
                    J.NewClass newClass = (J.NewClass) next;
                    return newClass.getBody() == parentBlock;
                } else if (next instanceof J.Lambda) {
                    return false;
                }
            }
            return false;
        }

        /**
         * Determines if the passed cursor is an {@link J.Block} that is static or initializer block.
         *
         * @param cursor Must point to a {@link J.Block}
         * @return True if the cursor represents a static or initializer block, false otherwise.
         */
        @Incubating(since = "7.25.0")
        public static boolean isStaticOrInitBlock(Cursor cursor) {
            if (!(cursor.getValue() instanceof J.Block)) {
                throw new IllegalArgumentException("Cursor must point to a J.Block!");
            }
            J.Block block = cursor.getValue();
            return block.isStatic() || isInitBlock(cursor);
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Block(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", statik=" + this.statik + ", statements=" + this.getStatements() + ", end=" + this.getEnd() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Block)) return false;
            final J.Block other = (J.Block) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Block(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Boolean> statik, final List<JRightPadded<Statement>> statements, final Space end) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.statik = statik;
            this.statements = statements;
            this.end = end;
        }

        private Block(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Boolean> statik, final List<JRightPadded<Statement>> statements, final Space end) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.statik = statik;
            this.statements = statements;
            this.end = end;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Block withId(final UUID id) {
            return this.id == id ? this : new J.Block(this.padding, id, this.prefix, this.markers, this.statik, this.statements, this.end);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Block withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Block(this.padding, this.id, prefix, this.markers, this.statik, this.statements, this.end);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Block withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Block(this.padding, this.id, this.prefix, markers, this.statik, this.statements, this.end);
        }

        public Space getEnd() {
            return this.end;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Block withEnd(final Space end) {
            return this.end == end ? this : new J.Block(this.padding, this.id, this.prefix, this.markers, this.statik, this.statements, end);
        }
    }


    final class Break implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final J.Identifier label;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitBreak(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Break)) return false;
            final J.Break other = (J.Break) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Break(final UUID id, final Space prefix, final Markers markers, @Nullable final J.Identifier label) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.label = label;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public J.Identifier getLabel() {
            return this.label;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Break withId(final UUID id) {
            return this.id == id ? this : new J.Break(id, this.prefix, this.markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Break withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Break(this.id, prefix, this.markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Break withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Break(this.id, this.prefix, markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Break withLabel(@Nullable final J.Identifier label) {
            return this.label == label ? this : new J.Break(this.id, this.prefix, this.markers, label);
        }
    }


    final class Case implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Type type;

        public Type getType() {
            return type == null ? Type.Statement : type;
        }

        /**
         * @return The pattern of this case statement.
         * @deprecated Prior to Java 12, there could only be one pattern. Use {@link #getExpressions()} instead.
         */
        @Deprecated
        public Expression getPattern() {
            return getExpressions().get(0);
        }

        /**
         * @return A new Case instance with the assigned pattern.
         * @deprecated Prior to Java 12, there could only be one pattern. Use {@link #withExpressions(List)} instead.
         */
        @Deprecated
        public Case withPattern(@Nullable Expression pattern) {
            return withExpressions(ListUtils.mapFirst(getExpressions(), first -> pattern));
        }

        private final JContainer<Expression> expressions;

        public List<Expression> getExpressions() {
            return expressions.getElements();
        }

        public Case withExpressions(List<Expression> expressions) {
            return getPadding().withExpressions(requireNonNull(JContainer.withElementsNullable(this.expressions, expressions)));
        }

        /**
         * For case with kind {@link Type#Statement}, returns the statements labeled by the case.
         * This container will be empty for case with kind {@link Type#Rule}, but possess the prefix
         * before the arrow.
         */
        private final JContainer<Statement> statements;

        public List<Statement> getStatements() {
            return statements.getElements();
        }

        public Case withStatements(List<Statement> statements) {
            return getPadding().withStatements(JContainer.withElements(this.statements, statements));
        }

        /**
         * For case with kind {@link Type#Rule}, returns the statement or expression after the arrow.
         * Returns null for case with kind {@link Type#Statement}.
         */
        @Nullable
        private final JRightPadded<J> body;

        @Nullable
        public J getBody() {
            return body == null ? null : body.getElement();
        }

        public Case withBody(J body) {
            return getPadding().withBody(JRightPadded.withElement(this.body, body));
        }

        @JsonCreator
        public Case(UUID id, Space prefix, Markers markers, Type type, @Deprecated @Nullable Expression pattern, JContainer<Expression> expressions, JContainer<Statement> statements, @Nullable JRightPadded<J> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
            if (pattern != null) {
                this.expressions = requireNonNull(JContainer.withElementsNullable(null, singletonList(pattern)));
            } else {
                this.expressions = expressions;
            }
            this.statements = statements;
            this.body = body;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitCase(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public enum Type {
            Statement, Rule;
        }


        public static class Padding {
            private final Case t;

            @Nullable
            public JRightPadded<J> getBody() {
                return t.body;
            }

            public Case withBody(@Nullable JRightPadded<J> body) {
                return t.body == body ? t : new Case(t.id, t.prefix, t.markers, t.type, null, t.expressions, t.statements, body);
            }

            public JContainer<Statement> getStatements() {
                return t.statements;
            }

            public Case withStatements(JContainer<Statement> statements) {
                return t.statements == statements ? t : new Case(t.id, t.prefix, t.markers, t.type, null, t.expressions, statements, t.body);
            }

            public JContainer<Expression> getExpressions() {
                return t.expressions;
            }

            public Case withExpressions(JContainer<Expression> expressions) {
                return t.expressions == expressions ? t : new Case(t.id, t.prefix, t.markers, t.type, null, expressions, t.statements, t.body);
            }

            public Padding(final Case t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Case)) return false;
            final J.Case other = (J.Case) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Case(final UUID id, final Space prefix, final Markers markers, final Type type, final JContainer<Expression> expressions, final JContainer<Statement> statements, @Nullable final JRightPadded<J> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
            this.expressions = expressions;
            this.statements = statements;
            this.body = body;
        }

        private Case(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Type type, final JContainer<Expression> expressions, final JContainer<Statement> statements, @Nullable final JRightPadded<J> body) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
            this.expressions = expressions;
            this.statements = statements;
            this.body = body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Case withId(final UUID id) {
            return this.id == id ? this : new J.Case(this.padding, id, this.prefix, this.markers, this.type, this.expressions, this.statements, this.body);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Case withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Case(this.padding, this.id, prefix, this.markers, this.type, this.expressions, this.statements, this.body);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Case withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Case(this.padding, this.id, this.prefix, markers, this.type, this.expressions, this.statements, this.body);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Case withType(final Type type) {
            return this.type == type ? this : new J.Case(this.padding, this.id, this.prefix, this.markers, type, this.expressions, this.statements, this.body);
        }
    }


    final class ClassDeclaration implements J, Statement, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        @Nullable
        private transient WeakReference<Annotations> annotations;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<Annotation> leadingAnnotations;
        private final List<Modifier> modifiers;
        private final Kind kind;

        public Kind.Type getKind() {
            return kind.getType();
        }

        public ClassDeclaration withKind(Kind.Type type) {
            Kind k = getAnnotations().getKind();
            if (k.type == type) {
                return this;
            } else {
                return getAnnotations().withKind(k.withType(type));
            }
        }

        private final Identifier name;
        @Nullable
        private final JContainer<TypeParameter> typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public ClassDeclaration withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Nullable
        private final JContainer<Statement> primaryConstructor;

        @Nullable
        public List<Statement> getPrimaryConstructor() {
            return primaryConstructor == null ? null : primaryConstructor.getElements();
        }

        public ClassDeclaration withPrimaryConstructor(@Nullable List<Statement> primaryConstructor) {
            return getPadding().withPrimaryConstructor(JContainer.withElementsNullable(this.primaryConstructor, primaryConstructor));
        }

        @Nullable
        private final JLeftPadded<TypeTree> extendings;

        @Nullable
        public TypeTree getExtends() {
            return extendings == null ? null : extendings.getElement();
        }

        public ClassDeclaration withExtends(@Nullable TypeTree extendings) {
            return getPadding().withExtends(JLeftPadded.withElement(this.extendings, extendings));
        }

        @Nullable
        private final JContainer<TypeTree> implementings;

        @Nullable
        public List<TypeTree> getImplements() {
            return implementings == null ? null : implementings.getElements();
        }

        public ClassDeclaration withImplements(@Nullable List<TypeTree> implementings) {
            return getPadding().withImplements(JContainer.withElementsNullable(this.implementings, implementings));
        }

        @Nullable
        private final JContainer<TypeTree> permitting;

        @Nullable
        public List<TypeTree> getPermits() {
            return permitting == null ? null : permitting.getElements();
        }

        public ClassDeclaration withPermits(@Nullable List<TypeTree> permitting) {
            return getPadding().withPermits(JContainer.withElementsNullable(this.permitting, permitting));
        }

        private final Block body;
        @Nullable
        private final JavaType.FullyQualified type;

        @SuppressWarnings("unchecked")
        @Override
        public ClassDeclaration withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (type != null && !(type instanceof JavaType.FullyQualified)) {
                throw new IllegalArgumentException("A class can only be type attributed with a fully qualified type name");
            }
            return new ClassDeclaration(id, prefix, markers, leadingAnnotations, modifiers, kind, name, typeParameters, primaryConstructor, extendings, implementings, permitting, body, (JavaType.FullyQualified) type);
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitClassDeclaration(this, p);
        }

        // gather annotations from everywhere they may occur
        /**
         * @deprecated Use {@link org.openrewrite.java.service.AnnotationService#getAllAnnotations(J)} instead.
         */
        @Deprecated
        public List<J.Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (J.Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            allAnnotations.addAll(kind.getAnnotations());
            return allAnnotations;
        }

        @Override
        @Transient
        public CoordinateBuilder.ClassDeclaration getCoordinates() {
            return new CoordinateBuilder.ClassDeclaration(this);
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }


        public static final class Kind implements J {
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final List<Annotation> annotations;
            private final Type type;


            public enum Type {
                Class, Enum, Interface, Annotation, Record;
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.ClassDeclaration.Kind)) return false;
                final J.ClassDeclaration.Kind other = (J.ClassDeclaration.Kind) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Kind(final UUID id, final Space prefix, final Markers markers, final List<Annotation> annotations, final Type type) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.annotations = annotations;
                this.type = type;
            }

            public UUID getId() {
                return this.id;
            }

            public Space getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public List<Annotation> getAnnotations() {
                return this.annotations;
            }

            public Type getType() {
                return this.type;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.ClassDeclaration.Kind(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", annotations=" + this.getAnnotations() + ", type=" + this.getType() + ")";
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ClassDeclaration.Kind withId(final UUID id) {
                return this.id == id ? this : new J.ClassDeclaration.Kind(id, this.prefix, this.markers, this.annotations, this.type);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ClassDeclaration.Kind withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.ClassDeclaration.Kind(this.id, prefix, this.markers, this.annotations, this.type);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ClassDeclaration.Kind withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.ClassDeclaration.Kind(this.id, this.prefix, markers, this.annotations, this.type);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ClassDeclaration.Kind withAnnotations(final List<Annotation> annotations) {
                return this.annotations == annotations ? this : new J.ClassDeclaration.Kind(this.id, this.prefix, this.markers, annotations, this.type);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ClassDeclaration.Kind withType(final Type type) {
                return this.type == type ? this : new J.ClassDeclaration.Kind(this.id, this.prefix, this.markers, this.annotations, type);
            }
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final ClassDeclaration t;

            @Nullable
            public JContainer<Statement> getPrimaryConstructor() {
                return t.primaryConstructor;
            }

            public ClassDeclaration withPrimaryConstructor(@Nullable JContainer<Statement> primaryConstructor) {
                return t.primaryConstructor == primaryConstructor ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, primaryConstructor, t.extendings, t.implementings, t.permitting, t.body, t.type);
            }

            @Nullable
            public JLeftPadded<TypeTree> getExtends() {
                return t.extendings;
            }

            public ClassDeclaration withExtends(@Nullable JLeftPadded<TypeTree> extendings) {
                return t.extendings == extendings ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, extendings, t.implementings, t.permitting, t.body, t.type);
            }

            @Nullable
            public JContainer<TypeTree> getImplements() {
                return t.implementings;
            }

            public ClassDeclaration withImplements(@Nullable JContainer<TypeTree> implementings) {
                return t.implementings == implementings ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, implementings, t.permitting, t.body, t.type);
            }

            @Nullable
            public JContainer<TypeTree> getPermits() {
                return t.permitting;
            }

            public ClassDeclaration withPermits(@Nullable JContainer<TypeTree> permitting) {
                return t.permitting == permitting ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, t.implementings, permitting, t.body, t.type);
            }

            public Kind getKind() {
                return t.kind;
            }

            public ClassDeclaration withKind(Kind kind) {
                return t.kind == kind ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.permitting, t.body, t.type);
            }

            @Nullable
            public JContainer<TypeParameter> getTypeParameters() {
                return t.typeParameters;
            }

            public ClassDeclaration withTypeParameters(@Nullable JContainer<TypeParameter> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.kind, t.name, typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.permitting, t.body, t.type);
            }

            public Padding(final ClassDeclaration t) {
                this.t = t;
            }
        }

        public Annotations getAnnotations() {
            Annotations a;
            if (this.annotations == null) {
                a = new Annotations(this);
                this.annotations = new WeakReference<>(a);
            } else {
                a = this.annotations.get();
                if (a == null || a.t != this) {
                    a = new Annotations(this);
                    this.annotations = new WeakReference<>(a);
                }
            }
            return a;
        }


        public static class Annotations {
            private final ClassDeclaration t;

            public Kind getKind() {
                return t.kind;
            }

            public ClassDeclaration withKind(Kind kind) {
                return t.kind == kind ? t : new ClassDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, kind, t.name, t.typeParameters, t.primaryConstructor, t.extendings, t.implementings, t.permitting, t.body, t.type);
            }

            public Annotations(final ClassDeclaration t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ClassDeclaration)) return false;
            final J.ClassDeclaration other = (J.ClassDeclaration) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ClassDeclaration(final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, final Kind kind, final Identifier name, @Nullable final JContainer<TypeParameter> typeParameters, @Nullable final JContainer<Statement> primaryConstructor, @Nullable final JLeftPadded<TypeTree> extendings, @Nullable final JContainer<TypeTree> implementings, @Nullable final JContainer<TypeTree> permitting, final Block body, @Nullable final JavaType.FullyQualified type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.kind = kind;
            this.name = name;
            this.typeParameters = typeParameters;
            this.primaryConstructor = primaryConstructor;
            this.extendings = extendings;
            this.implementings = implementings;
            this.permitting = permitting;
            this.body = body;
            this.type = type;
        }

        private ClassDeclaration(@Nullable final WeakReference<Padding> padding, @Nullable final WeakReference<Annotations> annotations, final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, final Kind kind, final Identifier name, @Nullable final JContainer<TypeParameter> typeParameters, @Nullable final JContainer<Statement> primaryConstructor, @Nullable final JLeftPadded<TypeTree> extendings, @Nullable final JContainer<TypeTree> implementings, @Nullable final JContainer<TypeTree> permitting, final Block body, @Nullable final JavaType.FullyQualified type) {
            this.padding = padding;
            this.annotations = annotations;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.kind = kind;
            this.name = name;
            this.typeParameters = typeParameters;
            this.primaryConstructor = primaryConstructor;
            this.extendings = extendings;
            this.implementings = implementings;
            this.permitting = permitting;
            this.body = body;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withId(final UUID id) {
            return this.id == id ? this : new J.ClassDeclaration(this.padding, this.annotations, id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, prefix, this.markers, this.leadingAnnotations, this.modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, this.prefix, markers, this.leadingAnnotations, this.modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withLeadingAnnotations(final List<Annotation> leadingAnnotations) {
            return this.leadingAnnotations == leadingAnnotations ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, leadingAnnotations, this.modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public List<Annotation> getLeadingAnnotations() {
            return this.leadingAnnotations;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withModifiers(final List<Modifier> modifiers) {
            return this.modifiers == modifiers ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public List<Modifier> getModifiers() {
            return this.modifiers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withName(final Identifier name) {
            return this.name == name ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.kind, name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, this.body, this.type);
        }

        public Identifier getName() {
            return this.name;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ClassDeclaration withBody(final Block body) {
            return this.body == body ? this : new J.ClassDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.kind, this.name, this.typeParameters, this.primaryConstructor, this.extendings, this.implementings, this.permitting, body, this.type);
        }

        public Block getBody() {
            return this.body;
        }

        @Nullable
        public JavaType.FullyQualified getType() {
            return this.type;
        }
    }


    final class CompilationUnit implements J, JavaSourceFile, SourceFile {
        @Nullable
        private transient SoftReference<TypesInUse> typesInUse;
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Path sourcePath;
        @Nullable
        private final FileAttributes fileAttributes;
        // for backwards compatibility
        @Nullable
        private final String charsetName;
        private final boolean charsetBomMarked;
        @Nullable
        private final Checksum checksum;

        @Override
        public Charset getCharset() {
            return charsetName == null ? StandardCharsets.UTF_8 : Charset.forName(charsetName);
        }

        @SuppressWarnings("unchecked")
        @Override
        public SourceFile withCharset(Charset charset) {
            return withCharsetName(charset.name());
        }

        @Nullable
        private final JRightPadded<Package> packageDeclaration;

        @Nullable
        public Package getPackageDeclaration() {
            return packageDeclaration == null ? null : packageDeclaration.getElement();
        }

        public CompilationUnit withPackageDeclaration(Package packageDeclaration) {
            return getPadding().withPackageDeclaration(JRightPadded.withElement(this.packageDeclaration, packageDeclaration));
        }

        private final List<JRightPadded<Import>> imports;

        public List<Import> getImports() {
            return JRightPadded.getElements(imports);
        }

        public CompilationUnit withImports(List<Import> imports) {
            return getPadding().withImports(JRightPadded.withElements(this.imports, imports));
        }

        private final List<ClassDeclaration> classes;
        private final Space eof;

        @Transient
        @Override
        public long getWeight(Predicate<Object> uniqueIdentity) {
            AtomicInteger n = new AtomicInteger();
            
            //noinspection ConstantConditions
            new JavaVisitor<AtomicInteger>() {
                final JavaTypeVisitor<AtomicInteger> typeVisitor = new JavaTypeVisitor<AtomicInteger>() {
                    @Override
                    public JavaType visit(@Nullable JavaType javaType, AtomicInteger n) {
                        if (javaType != null && uniqueIdentity.test(javaType)) {
                            n.incrementAndGet();
                            return super.visit(javaType, n);
                        }
                        return javaType;
                    }
                };
                final JavadocVisitor<AtomicInteger> javadocVisitor = new JavadocVisitor<AtomicInteger>(this) {
                    @Override
                    @Nullable
                    public Javadoc visit(@Nullable Tree tree, AtomicInteger n) {
                        if (tree != null) {
                            n.incrementAndGet();
                        }
                        return super.visit(tree, n);
                    }
                };
                @Override
                @Nullable
                public J visit(@Nullable Tree tree, AtomicInteger n) {
                    if (tree != null) {
                        n.incrementAndGet();
                    }
                    return super.visit(tree, n);
                }
                @Override
                public JavaType visitType(@Nullable JavaType javaType, AtomicInteger n) {
                    return typeVisitor.visit(javaType, n);
                }
                @Override
                protected JavadocVisitor<AtomicInteger> getJavadocVisitor() {
                    return javadocVisitor;
                }
            }.visit(this, n);
            return n.get();
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitCompilationUnit(this, p);
        }

        public Set<NameTree> findType(String clazz) {
            return FindTypes.find(this, clazz);
        }

        @Override
        public <P> TreeVisitor<?, PrintOutputCapture<P>> printer(Cursor cursor) {
            return new JavaPrinter<>();
        }

        @Transient
        public TypesInUse getTypesInUse() {
            TypesInUse cache;
            if (this.typesInUse == null) {
                cache = TypesInUse.build(this);
                this.typesInUse = new SoftReference<>(cache);
            } else {
                cache = this.typesInUse.get();
                if (cache == null || cache.getCu() != this) {
                    cache = TypesInUse.build(this);
                    this.typesInUse = new SoftReference<>(cache);
                }
            }
            return cache;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding implements JavaSourceFile.Padding {
            private final CompilationUnit t;

            @Nullable
            public JRightPadded<Package> getPackageDeclaration() {
                return t.packageDeclaration;
            }

            public CompilationUnit withPackageDeclaration(@Nullable JRightPadded<Package> packageDeclaration) {
                return t.packageDeclaration == packageDeclaration ? t : new CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null, packageDeclaration, t.imports, t.classes, t.eof);
            }

            @Override
            public List<JRightPadded<Import>> getImports() {
                return t.imports;
            }

            @Override
            public CompilationUnit withImports(List<JRightPadded<Import>> imports) {
                return t.imports == imports ? t : new CompilationUnit(t.id, t.prefix, t.markers, t.sourcePath, t.fileAttributes, t.charsetName, t.charsetBomMarked, null, t.packageDeclaration, imports, t.classes, t.eof);
            }

            public Padding(final CompilationUnit t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.CompilationUnit)) return false;
            final J.CompilationUnit other = (J.CompilationUnit) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public CompilationUnit(final UUID id, final Space prefix, final Markers markers, final Path sourcePath, @Nullable final FileAttributes fileAttributes, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final Checksum checksum, @Nullable final JRightPadded<Package> packageDeclaration, final List<JRightPadded<Import>> imports, final List<ClassDeclaration> classes, final Space eof) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.sourcePath = sourcePath;
            this.fileAttributes = fileAttributes;
            this.charsetName = charsetName;
            this.charsetBomMarked = charsetBomMarked;
            this.checksum = checksum;
            this.packageDeclaration = packageDeclaration;
            this.imports = imports;
            this.classes = classes;
            this.eof = eof;
        }

        private CompilationUnit(@Nullable final SoftReference<TypesInUse> typesInUse, @Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Path sourcePath, @Nullable final FileAttributes fileAttributes, @Nullable final String charsetName, final boolean charsetBomMarked, @Nullable final Checksum checksum, @Nullable final JRightPadded<Package> packageDeclaration, final List<JRightPadded<Import>> imports, final List<ClassDeclaration> classes, final Space eof) {
            this.typesInUse = typesInUse;
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.sourcePath = sourcePath;
            this.fileAttributes = fileAttributes;
            this.charsetName = charsetName;
            this.charsetBomMarked = charsetBomMarked;
            this.checksum = checksum;
            this.packageDeclaration = packageDeclaration;
            this.imports = imports;
            this.classes = classes;
            this.eof = eof;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withId(final UUID id) {
            return this.id == id ? this : new J.CompilationUnit(this.typesInUse, this.padding, id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withSourcePath(final Path sourcePath) {
            return this.sourcePath == sourcePath ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        public Path getSourcePath() {
            return this.sourcePath;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withFileAttributes(@Nullable final FileAttributes fileAttributes) {
            return this.fileAttributes == fileAttributes ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        @Nullable
        public FileAttributes getFileAttributes() {
            return this.fileAttributes;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        private J.CompilationUnit withCharsetName(@Nullable final String charsetName) {
            return this.charsetName == charsetName ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withCharsetBomMarked(final boolean charsetBomMarked) {
            return this.charsetBomMarked == charsetBomMarked ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        public boolean isCharsetBomMarked() {
            return this.charsetBomMarked;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withChecksum(@Nullable final Checksum checksum) {
            return this.checksum == checksum ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, checksum, this.packageDeclaration, this.imports, this.classes, this.eof);
        }

        @Nullable
        public Checksum getChecksum() {
            return this.checksum;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withClasses(final List<ClassDeclaration> classes) {
            return this.classes == classes ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, classes, this.eof);
        }

        public List<ClassDeclaration> getClasses() {
            return this.classes;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.CompilationUnit withEof(final Space eof) {
            return this.eof == eof ? this : new J.CompilationUnit(this.typesInUse, this.padding, this.id, this.prefix, this.markers, this.sourcePath, this.fileAttributes, this.charsetName, this.charsetBomMarked, this.checksum, this.packageDeclaration, this.imports, this.classes, eof);
        }

        public Space getEof() {
            return this.eof;
        }
    }


    final class Continue implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final J.Identifier label;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitContinue(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Continue)) return false;
            final J.Continue other = (J.Continue) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Continue(final UUID id, final Space prefix, final Markers markers, @Nullable final J.Identifier label) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.label = label;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public J.Identifier getLabel() {
            return this.label;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Continue withId(final UUID id) {
            return this.id == id ? this : new J.Continue(id, this.prefix, this.markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Continue withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Continue(this.id, prefix, this.markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Continue withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Continue(this.id, this.prefix, markers, this.label);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Continue withLabel(@Nullable final J.Identifier label) {
            return this.label == label ? this : new J.Continue(this.id, this.prefix, this.markers, label);
        }
    }


    final class DoWhileLoop implements J, Loop {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public DoWhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        private final JLeftPadded<ControlParentheses<Expression>> whileCondition;

        public ControlParentheses<Expression> getWhileCondition() {
            return whileCondition.getElement();
        }

        public DoWhileLoop withWhileCondition(ControlParentheses<Expression> whileCondition) {
            return getPadding().withWhileCondition(this.whileCondition.withElement(whileCondition));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitDoWhileLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final DoWhileLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public DoWhileLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new DoWhileLoop(t.id, t.prefix, t.markers, body, t.whileCondition);
            }

            public JLeftPadded<ControlParentheses<Expression>> getWhileCondition() {
                return t.whileCondition;
            }

            public DoWhileLoop withWhileCondition(JLeftPadded<ControlParentheses<Expression>> whileCondition) {
                return t.whileCondition == whileCondition ? t : new DoWhileLoop(t.id, t.prefix, t.markers, t.body, whileCondition);
            }

            public Padding(final DoWhileLoop t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.DoWhileLoop(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", body=" + this.getBody() + ", whileCondition=" + this.getWhileCondition() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.DoWhileLoop)) return false;
            final J.DoWhileLoop other = (J.DoWhileLoop) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public DoWhileLoop(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Statement> body, final JLeftPadded<ControlParentheses<Expression>> whileCondition) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.body = body;
            this.whileCondition = whileCondition;
        }

        private DoWhileLoop(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Statement> body, final JLeftPadded<ControlParentheses<Expression>> whileCondition) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.body = body;
            this.whileCondition = whileCondition;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.DoWhileLoop withId(final UUID id) {
            return this.id == id ? this : new J.DoWhileLoop(this.padding, id, this.prefix, this.markers, this.body, this.whileCondition);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.DoWhileLoop withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.DoWhileLoop(this.padding, this.id, prefix, this.markers, this.body, this.whileCondition);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.DoWhileLoop withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.DoWhileLoop(this.padding, this.id, this.prefix, markers, this.body, this.whileCondition);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class Empty implements J, Statement, Expression, TypeTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Empty withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEmpty(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Empty)) return false;
            final J.Empty other = (J.Empty) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Empty(final UUID id, final Space prefix, final Markers markers) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Empty(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Empty withId(final UUID id) {
            return this.id == id ? this : new J.Empty(id, this.prefix, this.markers);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Empty withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Empty(this.id, prefix, this.markers);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Empty withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Empty(this.id, this.prefix, markers);
        }
    }


    final class EnumValue implements J {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<J.Annotation> annotations;
        private final Identifier name;
        @Nullable
        private final NewClass initializer;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEnumValue(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.EnumValue)) return false;
            final J.EnumValue other = (J.EnumValue) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public EnumValue(final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final Identifier name, @Nullable final NewClass initializer) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.name = name;
            this.initializer = initializer;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }

        public Identifier getName() {
            return this.name;
        }

        @Nullable
        public NewClass getInitializer() {
            return this.initializer;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withId(final UUID id) {
            return this.id == id ? this : new J.EnumValue(id, this.prefix, this.markers, this.annotations, this.name, this.initializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.EnumValue(this.id, prefix, this.markers, this.annotations, this.name, this.initializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.EnumValue(this.id, this.prefix, markers, this.annotations, this.name, this.initializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withAnnotations(final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.EnumValue(this.id, this.prefix, this.markers, annotations, this.name, this.initializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withName(final Identifier name) {
            return this.name == name ? this : new J.EnumValue(this.id, this.prefix, this.markers, this.annotations, name, this.initializer);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValue withInitializer(@Nullable final NewClass initializer) {
            return this.initializer == initializer ? this : new J.EnumValue(this.id, this.prefix, this.markers, this.annotations, this.name, initializer);
        }
    }


    final class EnumValueSet implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<JRightPadded<EnumValue>> enums;

        public List<EnumValue> getEnums() {
            return JRightPadded.getElements(enums);
        }

        public EnumValueSet withEnums(List<EnumValue> enums) {
            return getPadding().withEnums(JRightPadded.withElements(this.enums, enums));
        }

        private final boolean terminatedWithSemicolon;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitEnumValueSet(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final EnumValueSet t;

            public List<JRightPadded<EnumValue>> getEnums() {
                return t.enums;
            }

            public EnumValueSet withEnums(List<JRightPadded<EnumValue>> enums) {
                return t.enums == enums ? t : new EnumValueSet(t.id, t.prefix, t.markers, enums, t.terminatedWithSemicolon);
            }

            public Padding(final EnumValueSet t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.EnumValueSet(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", enums=" + this.getEnums() + ", terminatedWithSemicolon=" + this.isTerminatedWithSemicolon() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.EnumValueSet)) return false;
            final J.EnumValueSet other = (J.EnumValueSet) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public EnumValueSet(final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<EnumValue>> enums, final boolean terminatedWithSemicolon) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.enums = enums;
            this.terminatedWithSemicolon = terminatedWithSemicolon;
        }

        private EnumValueSet(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<EnumValue>> enums, final boolean terminatedWithSemicolon) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.enums = enums;
            this.terminatedWithSemicolon = terminatedWithSemicolon;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValueSet withId(final UUID id) {
            return this.id == id ? this : new J.EnumValueSet(this.padding, id, this.prefix, this.markers, this.enums, this.terminatedWithSemicolon);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValueSet withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.EnumValueSet(this.padding, this.id, prefix, this.markers, this.enums, this.terminatedWithSemicolon);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValueSet withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.EnumValueSet(this.padding, this.id, this.prefix, markers, this.enums, this.terminatedWithSemicolon);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.EnumValueSet withTerminatedWithSemicolon(final boolean terminatedWithSemicolon) {
            return this.terminatedWithSemicolon == terminatedWithSemicolon ? this : new J.EnumValueSet(this.padding, this.id, this.prefix, this.markers, this.enums, terminatedWithSemicolon);
        }

        public boolean isTerminatedWithSemicolon() {
            return this.terminatedWithSemicolon;
        }
    }


    final class FieldAccess implements J, TypeTree, Expression, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression target;
        private final JLeftPadded<Identifier> name;

        public Identifier getName() {
            return name.getElement();
        }

        public FieldAccess withName(Identifier name) {
            return getPadding().withName(this.name.withElement(name));
        }

        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitFieldAccess(this, p);
        }

        public String getSimpleName() {
            return name.getElement().getSimpleName();
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return target.getSideEffects();
        }

        /**
         * @return For expressions like {@code String.class}, this casts target expression to a {@link NameTree}.
         * If the field access is not a reference to a class type, returns null.
         */
        @Nullable
        public NameTree asClassReference() {
            if (target instanceof NameTree) {
                String fqn = null;
                if (type instanceof JavaType.FullyQualified) {
                    fqn = ((JavaType.FullyQualified) type).getFullyQualifiedName();
                }
                return "java.lang.Class".equals(fqn) ? (NameTree) target : null;
            }
            return null;
        }

        public boolean isFullyQualifiedClassReference(String className) {
            return isFullyQualifiedClassReference(this, className);
        }

        private boolean isFullyQualifiedClassReference(J.FieldAccess fieldAccess, String className) {
            if (!className.contains(".")) {
                return false;
            }
            if (!fieldAccess.getName().getSimpleName().equals(className.substring(className.lastIndexOf('.') + 1))) {
                return false;
            }
            if (fieldAccess.getTarget() instanceof J.FieldAccess) {
                return isFullyQualifiedClassReference((J.FieldAccess) fieldAccess.getTarget(), className.substring(0, className.lastIndexOf('.')));
            }
            if (fieldAccess.getTarget() instanceof Identifier) {
                return ((Identifier) fieldAccess.getTarget()).getSimpleName().equals(className.substring(0, className.lastIndexOf('.')));
            }
            return false;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final FieldAccess t;

            public JLeftPadded<Identifier> getName() {
                return t.name;
            }

            public FieldAccess withName(JLeftPadded<Identifier> name) {
                return t.name == name ? t : new FieldAccess(t.id, t.prefix, t.markers, t.target, name, t.type);
            }

            public Padding(final FieldAccess t) {
                this.t = t;
            }
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.FieldAccess)) return false;
            final J.FieldAccess other = (J.FieldAccess) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public FieldAccess(final UUID id, final Space prefix, final Markers markers, final Expression target, final JLeftPadded<Identifier> name, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.target = target;
            this.name = name;
            this.type = type;
        }

        private FieldAccess(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Expression target, final JLeftPadded<Identifier> name, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.target = target;
            this.name = name;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.FieldAccess withId(final UUID id) {
            return this.id == id ? this : new J.FieldAccess(this.padding, id, this.prefix, this.markers, this.target, this.name, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.FieldAccess withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.FieldAccess(this.padding, this.id, prefix, this.markers, this.target, this.name, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.FieldAccess withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.FieldAccess(this.padding, this.id, this.prefix, markers, this.target, this.name, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.FieldAccess withTarget(final Expression target) {
            return this.target == target ? this : new J.FieldAccess(this.padding, this.id, this.prefix, this.markers, target, this.name, this.type);
        }

        public Expression getTarget() {
            return this.target;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.FieldAccess withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.FieldAccess(this.padding, this.id, this.prefix, this.markers, this.target, this.name, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class ForEachLoop implements J, Loop {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Control control;
        private final JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public ForEachLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitForEachLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        public static final class Control implements J {
            @Nullable
            private transient WeakReference<Padding> padding;
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final JRightPadded<VariableDeclarations> variable;

            public VariableDeclarations getVariable() {
                return variable.getElement();
            }

            public Control withVariable(VariableDeclarations variable) {
                return getPadding().withVariable(this.variable.withElement(variable));
            }

            private final JRightPadded<Expression> iterable;

            public Expression getIterable() {
                return iterable.getElement();
            }

            public Control withIterable(Expression iterable) {
                return getPadding().withIterable(this.iterable.withElement(iterable));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitForEachControl(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @Override
            public String toString() {
                return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
            }


            public static class Padding {
                private final Control t;

                public JRightPadded<VariableDeclarations> getVariable() {
                    return t.variable;
                }

                public Control withVariable(JRightPadded<VariableDeclarations> variable) {
                    return t.variable == variable ? t : new Control(t.id, t.prefix, t.markers, variable, t.iterable);
                }

                public JRightPadded<Expression> getIterable() {
                    return t.iterable;
                }

                public Control withIterable(JRightPadded<Expression> iterable) {
                    return t.iterable == iterable ? t : new Control(t.id, t.prefix, t.markers, t.variable, iterable);
                }

                public Padding(final Control t) {
                    this.t = t;
                }
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.ForEachLoop.Control)) return false;
                final J.ForEachLoop.Control other = (J.ForEachLoop.Control) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Control(final UUID id, final Space prefix, final Markers markers, final JRightPadded<VariableDeclarations> variable, final JRightPadded<Expression> iterable) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.variable = variable;
                this.iterable = iterable;
            }

            private Control(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<VariableDeclarations> variable, final JRightPadded<Expression> iterable) {
                this.padding = padding;
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.variable = variable;
                this.iterable = iterable;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForEachLoop.Control withId(final UUID id) {
                return this.id == id ? this : new J.ForEachLoop.Control(this.padding, id, this.prefix, this.markers, this.variable, this.iterable);
            }

            public UUID getId() {
                return this.id;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForEachLoop.Control withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.ForEachLoop.Control(this.padding, this.id, prefix, this.markers, this.variable, this.iterable);
            }

            public Space getPrefix() {
                return this.prefix;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForEachLoop.Control withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.ForEachLoop.Control(this.padding, this.id, this.prefix, markers, this.variable, this.iterable);
            }

            public Markers getMarkers() {
                return this.markers;
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final ForEachLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForEachLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new ForEachLoop(t.id, t.prefix, t.markers, t.control, body);
            }

            public Padding(final ForEachLoop t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.ForEachLoop(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", control=" + this.getControl() + ", body=" + this.getBody() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ForEachLoop)) return false;
            final J.ForEachLoop other = (J.ForEachLoop) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ForEachLoop(final UUID id, final Space prefix, final Markers markers, final Control control, final JRightPadded<Statement> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.control = control;
            this.body = body;
        }

        private ForEachLoop(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Control control, final JRightPadded<Statement> body) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.control = control;
            this.body = body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForEachLoop withId(final UUID id) {
            return this.id == id ? this : new J.ForEachLoop(this.padding, id, this.prefix, this.markers, this.control, this.body);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForEachLoop withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ForEachLoop(this.padding, this.id, prefix, this.markers, this.control, this.body);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForEachLoop withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ForEachLoop(this.padding, this.id, this.prefix, markers, this.control, this.body);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForEachLoop withControl(final Control control) {
            return this.control == control ? this : new J.ForEachLoop(this.padding, this.id, this.prefix, this.markers, control, this.body);
        }

        public Control getControl() {
            return this.control;
        }
    }


    final class ForLoop implements J, Loop {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Control control;
        private final JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public ForLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitForLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        public static final class Control implements J {
            @Nullable
            private transient WeakReference<Padding> padding;
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final List<JRightPadded<Statement>> init;

            public List<Statement> getInit() {
                return JRightPadded.getElements(init);
            }

            public Control withInit(List<Statement> init) {
                return getPadding().withInit(JRightPadded.withElements(this.init, init));
            }

            private final JRightPadded<Expression> condition;

            public Expression getCondition() {
                return condition.getElement();
            }

            public Control withCondition(Expression condition) {
                return getPadding().withCondition(this.condition.withElement(condition));
            }

            private final List<JRightPadded<Statement>> update;

            public List<Statement> getUpdate() {
                return JRightPadded.getElements(update);
            }

            public Control withUpdate(List<Statement> update) {
                return getPadding().withUpdate(JRightPadded.withElements(this.update, update));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitForControl(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @Override
            public String toString() {
                return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
            }


            public static class Padding {
                private final Control t;

                public List<JRightPadded<Statement>> getInit() {
                    return t.init;
                }

                public ForLoop.Control withInit(List<JRightPadded<Statement>> init) {
                    return t.init == init ? t : new ForLoop.Control(t.id, t.prefix, t.markers, init, t.condition, t.update);
                }

                public JRightPadded<Expression> getCondition() {
                    return t.condition;
                }

                public ForLoop.Control withCondition(JRightPadded<Expression> condition) {
                    return t.condition == condition ? t : new ForLoop.Control(t.id, t.prefix, t.markers, t.init, condition, t.update);
                }

                public List<JRightPadded<Statement>> getUpdate() {
                    return t.update;
                }

                public ForLoop.Control withUpdate(List<JRightPadded<Statement>> update) {
                    return t.update == update ? t : new ForLoop.Control(t.id, t.prefix, t.markers, t.init, t.condition, update);
                }

                public Padding(final Control t) {
                    this.t = t;
                }
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.ForLoop.Control)) return false;
                final J.ForLoop.Control other = (J.ForLoop.Control) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Control(final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<Statement>> init, final JRightPadded<Expression> condition, final List<JRightPadded<Statement>> update) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.init = init;
                this.condition = condition;
                this.update = update;
            }

            private Control(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<Statement>> init, final JRightPadded<Expression> condition, final List<JRightPadded<Statement>> update) {
                this.padding = padding;
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.init = init;
                this.condition = condition;
                this.update = update;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForLoop.Control withId(final UUID id) {
                return this.id == id ? this : new J.ForLoop.Control(this.padding, id, this.prefix, this.markers, this.init, this.condition, this.update);
            }

            public UUID getId() {
                return this.id;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForLoop.Control withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.ForLoop.Control(this.padding, this.id, prefix, this.markers, this.init, this.condition, this.update);
            }

            public Space getPrefix() {
                return this.prefix;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.ForLoop.Control withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.ForLoop.Control(this.padding, this.id, this.prefix, markers, this.init, this.condition, this.update);
            }

            public Markers getMarkers() {
                return this.markers;
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final ForLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public ForLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new ForLoop(t.id, t.prefix, t.markers, t.control, body);
            }

            public Padding(final ForLoop t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.ForLoop(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", control=" + this.getControl() + ", body=" + this.getBody() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ForLoop)) return false;
            final J.ForLoop other = (J.ForLoop) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ForLoop(final UUID id, final Space prefix, final Markers markers, final Control control, final JRightPadded<Statement> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.control = control;
            this.body = body;
        }

        private ForLoop(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Control control, final JRightPadded<Statement> body) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.control = control;
            this.body = body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForLoop withId(final UUID id) {
            return this.id == id ? this : new J.ForLoop(this.padding, id, this.prefix, this.markers, this.control, this.body);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForLoop withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ForLoop(this.padding, this.id, prefix, this.markers, this.control, this.body);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForLoop withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ForLoop(this.padding, this.id, this.prefix, markers, this.control, this.body);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ForLoop withControl(final Control control) {
            return this.control == control ? this : new J.ForLoop(this.padding, this.id, this.prefix, this.markers, control, this.body);
        }

        public Control getControl() {
            return this.control;
        }
    }


    /**
     * Java does not allow for parenthesis around TypeTree in places like a type cast where a J.ControlParenthesis is
     * used. But other languages, like Kotlin, do.
     */
    final class ParenthesizedTypeTree implements J, TypeTree, Expression {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<J.Annotation> annotations;
        private final J.Parentheses<TypeTree> parenthesizedType;

        @Override
        @Nullable
        public JavaType getType() {
            return parenthesizedType.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public ParenthesizedTypeTree withType(@Nullable JavaType type) {
            return withParenthesizedType(parenthesizedType.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitParenthesizedTypeTree(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }

        public J.Parentheses<TypeTree> getParenthesizedType() {
            return this.parenthesizedType;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.ParenthesizedTypeTree(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", annotations=" + this.getAnnotations() + ", parenthesizedType=" + this.getParenthesizedType() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ParenthesizedTypeTree)) return false;
            final J.ParenthesizedTypeTree other = (J.ParenthesizedTypeTree) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public ParenthesizedTypeTree(final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final J.Parentheses<TypeTree> parenthesizedType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.parenthesizedType = parenthesizedType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParenthesizedTypeTree withId(final UUID id) {
            return this.id == id ? this : new J.ParenthesizedTypeTree(id, this.prefix, this.markers, this.annotations, this.parenthesizedType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParenthesizedTypeTree withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ParenthesizedTypeTree(this.id, prefix, this.markers, this.annotations, this.parenthesizedType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParenthesizedTypeTree withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ParenthesizedTypeTree(this.id, this.prefix, markers, this.annotations, this.parenthesizedType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParenthesizedTypeTree withAnnotations(final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.ParenthesizedTypeTree(this.id, this.prefix, this.markers, annotations, this.parenthesizedType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParenthesizedTypeTree withParenthesizedType(final J.Parentheses<TypeTree> parenthesizedType) {
            return this.parenthesizedType == parenthesizedType ? this : new J.ParenthesizedTypeTree(this.id, this.prefix, this.markers, this.annotations, parenthesizedType);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class Identifier implements J, TypeTree, Expression {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<J.Annotation> annotations;
        private final String simpleName;
        @Nullable
        private final JavaType type;
        @Nullable
        private final JavaType.Variable fieldType;

        /**
         * @deprecated Use {@link #Identifier(UUID, Space, Markers, List, String, JavaType, JavaType.Variable)} instead.
         */
        @Deprecated
        public Identifier(UUID id, Space prefix, Markers markers, String simpleName, @Nullable JavaType type, @Nullable JavaType.Variable fieldType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = emptyList();
            this.simpleName = simpleName;
            this.type = type;
            this.fieldType = fieldType;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitIdentifier(this, p);
        }

        @Transient
        public CoordinateBuilder.Identifier getCoordinates() {
            return new CoordinateBuilder.Identifier(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }

        public String getSimpleName() {
            return this.simpleName;
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }

        @Nullable
        public JavaType.Variable getFieldType() {
            return this.fieldType;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Identifier)) return false;
            final J.Identifier other = (J.Identifier) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Identifier(final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final String simpleName, @Nullable final JavaType type, @Nullable final JavaType.Variable fieldType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.simpleName = simpleName;
            this.type = type;
            this.fieldType = fieldType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withId(final UUID id) {
            return this.id == id ? this : new J.Identifier(id, this.prefix, this.markers, this.annotations, this.simpleName, this.type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Identifier(this.id, prefix, this.markers, this.annotations, this.simpleName, this.type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Identifier(this.id, this.prefix, markers, this.annotations, this.simpleName, this.type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withAnnotations(final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.Identifier(this.id, this.prefix, this.markers, annotations, this.simpleName, this.type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withSimpleName(final String simpleName) {
            return this.simpleName == simpleName ? this : new J.Identifier(this.id, this.prefix, this.markers, this.annotations, simpleName, this.type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Identifier(this.id, this.prefix, this.markers, this.annotations, this.simpleName, type, this.fieldType);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Identifier withFieldType(@Nullable final JavaType.Variable fieldType) {
            return this.fieldType == fieldType ? this : new J.Identifier(this.id, this.prefix, this.markers, this.annotations, this.simpleName, this.type, fieldType);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class If implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<Expression> ifCondition;
        private final JRightPadded<Statement> thenPart;

        public Statement getThenPart() {
            return thenPart.getElement();
        }

        public If withThenPart(Statement thenPart) {
            return getPadding().withThenPart(this.thenPart.withElement(thenPart));
        }

        @Nullable
        private final Else elsePart;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitIf(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        public static final class Else implements J {
            @Nullable
            private transient WeakReference<Padding> padding;
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final JRightPadded<Statement> body;

            public Statement getBody() {
                return body.getElement();
            }

            public Else withBody(Statement body) {
                return getPadding().withBody(this.body.withElement(body));
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitElse(this, p);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }


            public static class Padding {
                private final Else t;

                public JRightPadded<Statement> getBody() {
                    return t.body;
                }

                public Else withBody(JRightPadded<Statement> body) {
                    return t.body == body ? t : new Else(t.id, t.prefix, t.markers, body);
                }

                public Padding(final Else t) {
                    this.t = t;
                }
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.If.Else(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", body=" + this.getBody() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.If.Else)) return false;
                final J.If.Else other = (J.If.Else) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Else(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Statement> body) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.body = body;
            }

            private Else(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Statement> body) {
                this.padding = padding;
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.body = body;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.If.Else withId(final UUID id) {
                return this.id == id ? this : new J.If.Else(this.padding, id, this.prefix, this.markers, this.body);
            }

            public UUID getId() {
                return this.id;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.If.Else withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.If.Else(this.padding, this.id, prefix, this.markers, this.body);
            }

            public Space getPrefix() {
                return this.prefix;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.If.Else withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.If.Else(this.padding, this.id, this.prefix, markers, this.body);
            }

            public Markers getMarkers() {
                return this.markers;
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final If t;

            public JRightPadded<Statement> getThenPart() {
                return t.thenPart;
            }

            public If withThenPart(JRightPadded<Statement> thenPart) {
                return t.thenPart == thenPart ? t : new If(t.id, t.prefix, t.markers, t.ifCondition, thenPart, t.elsePart);
            }

            public Padding(final If t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.If(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", ifCondition=" + this.getIfCondition() + ", thenPart=" + this.getThenPart() + ", elsePart=" + this.getElsePart() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.If)) return false;
            final J.If other = (J.If) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public If(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> ifCondition, final JRightPadded<Statement> thenPart, @Nullable final Else elsePart) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.ifCondition = ifCondition;
            this.thenPart = thenPart;
            this.elsePart = elsePart;
        }

        private If(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> ifCondition, final JRightPadded<Statement> thenPart, @Nullable final Else elsePart) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.ifCondition = ifCondition;
            this.thenPart = thenPart;
            this.elsePart = elsePart;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.If withId(final UUID id) {
            return this.id == id ? this : new J.If(this.padding, id, this.prefix, this.markers, this.ifCondition, this.thenPart, this.elsePart);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.If withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.If(this.padding, this.id, prefix, this.markers, this.ifCondition, this.thenPart, this.elsePart);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.If withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.If(this.padding, this.id, this.prefix, markers, this.ifCondition, this.thenPart, this.elsePart);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.If withIfCondition(final ControlParentheses<Expression> ifCondition) {
            return this.ifCondition == ifCondition ? this : new J.If(this.padding, this.id, this.prefix, this.markers, ifCondition, this.thenPart, this.elsePart);
        }

        public ControlParentheses<Expression> getIfCondition() {
            return this.ifCondition;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.If withElsePart(@Nullable final Else elsePart) {
            return this.elsePart == elsePart ? this : new J.If(this.padding, this.id, this.prefix, this.markers, this.ifCondition, this.thenPart, elsePart);
        }

        @Nullable
        public Else getElsePart() {
            return this.elsePart;
        }
    }


    final class Import implements Statement, Comparable<Import> {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JLeftPadded<Boolean> statik;
        private final FieldAccess qualid;
        @Nullable
        private final JLeftPadded<J.Identifier> alias;

        public boolean isStatic() {
            return statik.getElement();
        }

        public Import withStatic(boolean statik) {
            return getPadding().withStatic(this.statik.withElement(statik));
        }

        @Nullable
        public J.Identifier getAlias() {
            if (alias == null) {
                return null;
            }
            return alias.getElement();
        }

        public J.Import withAlias(@Nullable J.Identifier alias) {
            if (this.alias == null) {
                if (alias == null) {
                    return this;
                }
                return new J.Import(null, id, prefix, markers, statik, qualid, JLeftPadded.build(alias).withBefore(Space.format(" ")));
            }
            if (alias == null) {
                return new J.Import(null, id, prefix, markers, statik, qualid, null);
            }
            return getPadding().withAlias(this.alias.withElement(alias));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitImport(this, p);
        }

        /**
         * The type name of a statically imported inner class is the outermost class.
         */
        public String getTypeName() {
            if (isStatic()) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(qualid.getType());
                // the compiler doesn't type attribute static imports of classes
                Expression target = qualid.getTarget();
                if (fq == null) {
                    String possibleInnerClassFqn = getTypeName(qualid);
                    String possibleInnerClassName = possibleInnerClassFqn.substring(possibleInnerClassFqn.lastIndexOf('$') + 1);
                    if ("*".equals(possibleInnerClassName)) {
                        return possibleInnerClassFqn.substring(0, possibleInnerClassFqn.lastIndexOf('$'));
                    }
                    while (possibleInnerClassName.indexOf('$') >= 0) {
                        possibleInnerClassName = possibleInnerClassName.substring(possibleInnerClassName.indexOf('$') + 1);
                    }
                    JavaType.Class owner = TypeUtils.asClass(target.getType());
                    if (owner != null && !(target.getType() instanceof JavaType.ShallowClass)) {
                        Iterator<JavaType.Method> visibleMethods = owner.getVisibleMethods();
                        while (visibleMethods.hasNext()) {
                            JavaType.Method method = visibleMethods.next();
                            if (method.getName().equals(possibleInnerClassName)) {
                                return possibleInnerClassFqn.substring(0, possibleInnerClassFqn.lastIndexOf('$'));
                            }
                        }
                        Iterator<JavaType.Variable> visibleMembers = owner.getVisibleMembers();
                        while (visibleMembers.hasNext()) {
                            JavaType.Variable member = visibleMembers.next();
                            if (member.getName().equals(possibleInnerClassName)) {
                                return possibleInnerClassFqn.substring(0, possibleInnerClassFqn.lastIndexOf('$'));
                            }
                        }
                        return possibleInnerClassFqn;
                    }
                }
                return target instanceof Identifier ? ((Identifier) target).getSimpleName() : getTypeName((FieldAccess) target);
            }
            return getTypeName(qualid);
        }

        private String getTypeName(J.FieldAccess type) {
            StringBuilder typeName = new StringBuilder();
            J.FieldAccess part = type;
            while (true) {
                String name = part.getSimpleName();
                if (part.getTarget() instanceof J.Identifier) {
                    typeName.insert(0, ((Identifier) part.getTarget()).getSimpleName() + "." + name);
                    break;
                } else if (part.getTarget() instanceof J.FieldAccess) {
                    part = (FieldAccess) part.getTarget();
                    String delim = Character.isUpperCase(part.getSimpleName().charAt(0)) ? "$" : ".";
                    typeName.insert(0, delim + name);
                } else {
                    break;
                }
            }
            return typeName.toString();
        }

        /**
         * Retrieve just the package from the import.
         * e.g.:
         * <code>
         * import org.foo.A;            == "org.foo"
         * import static org.foo.A.bar; == "org.foo"
         * import org.foo.*;            == "org.foo"
         * </code>
         */
        public String getPackageName() {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(qualid.getType());
            if (fq != null) {
                return fq.getPackageName();
            }
            String typeName = getTypeName();
            int lastDot = typeName.lastIndexOf('.');
            return lastDot < 0 ? "" : typeName.substring(0, lastDot);
        }

        public String getClassName() {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(qualid.getType());
            if (fq != null) {
                return fq.getClassName();
            }
            String typeName = getTypeName();
            int lastDot = typeName.lastIndexOf('.');
            return lastDot < 0 ? typeName : typeName.substring(lastDot + 1);
        }

        @Override
        public int compareTo(Import o) {
            String p1 = this.getPackageName();
            String p2 = o.getPackageName();
            String[] p1s = p1.split("\\.");
            String[] p2s = p2.split("\\.");
            for (int i = 0; i < p1s.length; i++) {
                String s = p1s[i];
                if (p2s.length < i + 1) {
                    return 1;
                }
                if (!s.equals(p2s[i])) {
                    return s.compareTo(p2s[i]);
                }
            }
            return p1s.length < p2s.length ? -1 : this.getQualid().getSimpleName().compareTo(o.getQualid().getSimpleName());
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final Import t;

            public JLeftPadded<Boolean> getStatic() {
                return t.statik;
            }

            public Import withStatic(JLeftPadded<Boolean> statik) {
                return t.statik == statik ? t : new Import(t.id, t.prefix, t.markers, statik, t.qualid, t.alias);
            }

            @Nullable
            public JLeftPadded<J.Identifier> getAlias() {
                return t.alias;
            }

            public Import withAlias(@Nullable JLeftPadded<J.Identifier> alias) {
                return t.alias == alias ? t : new Import(t.id, t.prefix, t.markers, t.statik, t.qualid, alias);
            }

            public Padding(final Import t) {
                this.t = t;
            }
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Import)) return false;
            final J.Import other = (J.Import) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Import(final UUID id, final Space prefix, final Markers markers, final JLeftPadded<Boolean> statik, final FieldAccess qualid, @Nullable final JLeftPadded<J.Identifier> alias) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.statik = statik;
            this.qualid = qualid;
            this.alias = alias;
        }

        private Import(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JLeftPadded<Boolean> statik, final FieldAccess qualid, @Nullable final JLeftPadded<J.Identifier> alias) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.statik = statik;
            this.qualid = qualid;
            this.alias = alias;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Import withId(final UUID id) {
            return this.id == id ? this : new J.Import(this.padding, id, this.prefix, this.markers, this.statik, this.qualid, this.alias);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Import withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Import(this.padding, this.id, prefix, this.markers, this.statik, this.qualid, this.alias);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Import withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Import(this.padding, this.id, this.prefix, markers, this.statik, this.qualid, this.alias);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Import withQualid(final FieldAccess qualid) {
            return this.qualid == qualid ? this : new J.Import(this.padding, this.id, this.prefix, this.markers, this.statik, qualid, this.alias);
        }

        public FieldAccess getQualid() {
            return this.qualid;
        }
    }


    final class InstanceOf implements J, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<Expression> expression;

        public Expression getExpression() {
            return expression.getElement();
        }

        public InstanceOf withExpression(Expression expression) {
            return getPadding().withExpr(this.expression.withElement(expression));
        }

        private final J clazz;
        @Nullable
        private final J pattern;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitInstanceOf(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return expression.getElement().getSideEffects();
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final InstanceOf t;

            public JRightPadded<Expression> getExpr() {
                return t.expression;
            }

            public InstanceOf withExpr(JRightPadded<Expression> expression) {
                return t.expression == expression ? t : new InstanceOf(t.id, t.prefix, t.markers, expression, t.clazz, t.pattern, t.type);
            }

            public Padding(final InstanceOf t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.InstanceOf)) return false;
            final J.InstanceOf other = (J.InstanceOf) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public InstanceOf(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> expression, final J clazz, @Nullable final J pattern, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.clazz = clazz;
            this.pattern = pattern;
            this.type = type;
        }

        private InstanceOf(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> expression, final J clazz, @Nullable final J pattern, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.clazz = clazz;
            this.pattern = pattern;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withId(final UUID id) {
            return this.id == id ? this : new J.InstanceOf(this.padding, id, this.prefix, this.markers, this.expression, this.clazz, this.pattern, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.InstanceOf(this.padding, this.id, prefix, this.markers, this.expression, this.clazz, this.pattern, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.InstanceOf(this.padding, this.id, this.prefix, markers, this.expression, this.clazz, this.pattern, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withClazz(final J clazz) {
            return this.clazz == clazz ? this : new J.InstanceOf(this.padding, this.id, this.prefix, this.markers, this.expression, clazz, this.pattern, this.type);
        }

        public J getClazz() {
            return this.clazz;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withPattern(@Nullable final J pattern) {
            return this.pattern == pattern ? this : new J.InstanceOf(this.padding, this.id, this.prefix, this.markers, this.expression, this.clazz, pattern, this.type);
        }

        @Nullable
        public J getPattern() {
            return this.pattern;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.InstanceOf withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.InstanceOf(this.padding, this.id, this.prefix, this.markers, this.expression, this.clazz, this.pattern, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class IntersectionType implements J, TypeTree, Expression {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JContainer<TypeTree> bounds;

        public List<TypeTree> getBounds() {
            return bounds.getElements();
        }

        public IntersectionType withBounds(List<TypeTree> bounds) {
            return getPadding().withBounds(JContainer.withElementsNullable(this.bounds, bounds));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitIntersectionType(this, p);
        }

        @SuppressWarnings("unchecked")
        @Override
        public IntersectionType withType(@Nullable JavaType type) {
            // cannot overwrite type directly, perform this operation on each bound separately
            return this;
        }

        @Override
        public JavaType getType() {
            return new JavaType.Intersection(bounds.getPadding().getElements().stream().filter(Objects::nonNull).map(b -> b.getElement().getType()).collect(toList()));
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final IntersectionType t;

            @Nullable
            public JContainer<TypeTree> getBounds() {
                return t.bounds;
            }

            public IntersectionType withBounds(@Nullable JContainer<TypeTree> bounds) {
                return t.bounds == bounds ? t : new IntersectionType(t.id, t.prefix, t.markers, bounds);
            }

            public Padding(final IntersectionType t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.IntersectionType)) return false;
            final J.IntersectionType other = (J.IntersectionType) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public IntersectionType(final UUID id, final Space prefix, final Markers markers, final JContainer<TypeTree> bounds) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.bounds = bounds;
        }

        private IntersectionType(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JContainer<TypeTree> bounds) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.bounds = bounds;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.IntersectionType withId(final UUID id) {
            return this.id == id ? this : new J.IntersectionType(this.padding, id, this.prefix, this.markers, this.bounds);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.IntersectionType withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.IntersectionType(this.padding, this.id, prefix, this.markers, this.bounds);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.IntersectionType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.IntersectionType(this.padding, this.id, this.prefix, markers, this.bounds);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class Label implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        /**
         * Right padded before the ':'
         */
        private final JRightPadded<Identifier> label;

        public Identifier getLabel() {
            return label.getElement();
        }

        public Label withLabel(Identifier label) {
            return getPadding().withLabel(this.label.withElement(label));
        }

        private final Statement statement;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLabel(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Label t;

            public JRightPadded<Identifier> getLabel() {
                return t.label;
            }

            public Label withLabel(JRightPadded<Identifier> label) {
                return t.label == label ? t : new Label(t.id, t.prefix, t.markers, label, t.statement);
            }

            public Padding(final Label t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Label)) return false;
            final J.Label other = (J.Label) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Label(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Identifier> label, final Statement statement) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.label = label;
            this.statement = statement;
        }

        private Label(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Identifier> label, final Statement statement) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.label = label;
            this.statement = statement;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Label withId(final UUID id) {
            return this.id == id ? this : new J.Label(this.padding, id, this.prefix, this.markers, this.label, this.statement);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Label withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Label(this.padding, this.id, prefix, this.markers, this.label, this.statement);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Label withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Label(this.padding, this.id, this.prefix, markers, this.label, this.statement);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Label withStatement(final Statement statement) {
            return this.statement == statement ? this : new J.Label(this.padding, this.id, this.prefix, this.markers, this.label, statement);
        }

        public Statement getStatement() {
            return this.statement;
        }
    }


    final class Lambda implements J, Statement, Expression, TypedTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Parameters parameters;
        private final Space arrow;
        private final J body;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLambda(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        public static final class Parameters implements J {
            @Nullable
            private transient WeakReference<Padding> padding;
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final boolean parenthesized;
            private final List<JRightPadded<J>> parameters;

            public List<J> getParameters() {
                return JRightPadded.getElements(parameters);
            }

            public Parameters withParameters(List<J> parameters) {
                return getPadding().withParams(JRightPadded.withElements(this.parameters, parameters));
            }

            @Transient
            public CoordinateBuilder.Lambda.Parameters getCoordinates() {
                return new CoordinateBuilder.Lambda.Parameters(this);
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }


            public static class Padding {
                private final Parameters t;

                public List<JRightPadded<J>> getParams() {
                    return t.parameters;
                }

                public Parameters withParams(List<JRightPadded<J>> parameters) {
                    return t.parameters == parameters ? t : new Parameters(t.id, t.prefix, t.markers, t.parenthesized, parameters);
                }

                public Padding(final Parameters t) {
                    this.t = t;
                }
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.Lambda.Parameters(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", parenthesized=" + this.isParenthesized() + ", parameters=" + this.getParameters() + ")";
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.Lambda.Parameters)) return false;
                final J.Lambda.Parameters other = (J.Lambda.Parameters) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Parameters(final UUID id, final Space prefix, final Markers markers, final boolean parenthesized, final List<JRightPadded<J>> parameters) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.parenthesized = parenthesized;
                this.parameters = parameters;
            }

            private Parameters(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final boolean parenthesized, final List<JRightPadded<J>> parameters) {
                this.padding = padding;
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.parenthesized = parenthesized;
                this.parameters = parameters;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Lambda.Parameters withId(final UUID id) {
                return this.id == id ? this : new J.Lambda.Parameters(this.padding, id, this.prefix, this.markers, this.parenthesized, this.parameters);
            }

            public UUID getId() {
                return this.id;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Lambda.Parameters withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.Lambda.Parameters(this.padding, this.id, prefix, this.markers, this.parenthesized, this.parameters);
            }

            public Space getPrefix() {
                return this.prefix;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Lambda.Parameters withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.Lambda.Parameters(this.padding, this.id, this.prefix, markers, this.parenthesized, this.parameters);
            }

            public Markers getMarkers() {
                return this.markers;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Lambda.Parameters withParenthesized(final boolean parenthesized) {
                return this.parenthesized == parenthesized ? this : new J.Lambda.Parameters(this.padding, this.id, this.prefix, this.markers, parenthesized, this.parameters);
            }

            public boolean isParenthesized() {
                return this.parenthesized;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Lambda)) return false;
            final J.Lambda other = (J.Lambda) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Lambda(final UUID id, final Space prefix, final Markers markers, final Parameters parameters, final Space arrow, final J body, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.parameters = parameters;
            this.arrow = arrow;
            this.body = body;
            this.type = type;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Parameters getParameters() {
            return this.parameters;
        }

        public Space getArrow() {
            return this.arrow;
        }

        public J getBody() {
            return this.body;
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Lambda(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", parameters=" + this.getParameters() + ", arrow=" + this.getArrow() + ", body=" + this.getBody() + ", type=" + this.getType() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withId(final UUID id) {
            return this.id == id ? this : new J.Lambda(id, this.prefix, this.markers, this.parameters, this.arrow, this.body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Lambda(this.id, prefix, this.markers, this.parameters, this.arrow, this.body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Lambda(this.id, this.prefix, markers, this.parameters, this.arrow, this.body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withParameters(final Parameters parameters) {
            return this.parameters == parameters ? this : new J.Lambda(this.id, this.prefix, this.markers, parameters, this.arrow, this.body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withArrow(final Space arrow) {
            return this.arrow == arrow ? this : new J.Lambda(this.id, this.prefix, this.markers, this.parameters, arrow, this.body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withBody(final J body) {
            return this.body == body ? this : new J.Lambda(this.id, this.prefix, this.markers, this.parameters, this.arrow, body, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Lambda withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Lambda(this.id, this.prefix, this.markers, this.parameters, this.arrow, this.body, type);
        }
    }


    final class Literal implements J, Expression, TypedTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final Object value;
        @Nullable
        private final String valueSource;
        @Nullable
        private final List<UnicodeEscape> unicodeEscapes;
        /**
         * Including String literals
         */
        private final JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Literal withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (type instanceof JavaType.Primitive) {
                return new Literal(id, prefix, markers, value, valueSource, unicodeEscapes, (JavaType.Primitive) type);
            }
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitLiteral(this, p);
        }

        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }


        /**
         * See <a href="https://docs.oracle.com/javase/specs/jls/se7/html/jls-3.html#jls-3.3">jls-3.3</a>.
         * <p>
         * Unmatched UTF-16 surrogate pairs (composed of two escape and code point pairs) are unserializable
         * by technologies like Jackson. So we separate and store the code point off and reconstruct
         * the escape sequence when printing later.
         * <p>
         * We only escape unicode characters that are part of UTF-16 surrogate pairs. Others are generally
         * treated well by tools like Jackson.
         */
        public static final class UnicodeEscape {
            private final int valueSourceIndex;
            private final String codePoint;

            public UnicodeEscape(final int valueSourceIndex, final String codePoint) {
                this.valueSourceIndex = valueSourceIndex;
                this.codePoint = codePoint;
            }

            public int getValueSourceIndex() {
                return this.valueSourceIndex;
            }

            public String getCodePoint() {
                return this.codePoint;
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.Literal.UnicodeEscape)) return false;
                final J.Literal.UnicodeEscape other = (J.Literal.UnicodeEscape) o;
                if (this.getValueSourceIndex() != other.getValueSourceIndex()) return false;
                final Object this$codePoint = this.getCodePoint();
                final Object other$codePoint = other.getCodePoint();
                if (this$codePoint == null ? other$codePoint != null : !this$codePoint.equals(other$codePoint)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                result = result * PRIME + this.getValueSourceIndex();
                final Object $codePoint = this.getCodePoint();
                result = result * PRIME + ($codePoint == null ? 43 : $codePoint.hashCode());
                return result;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.Literal.UnicodeEscape(valueSourceIndex=" + this.getValueSourceIndex() + ", codePoint=" + this.getCodePoint() + ")";
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Literal.UnicodeEscape withValueSourceIndex(final int valueSourceIndex) {
                return this.valueSourceIndex == valueSourceIndex ? this : new J.Literal.UnicodeEscape(valueSourceIndex, this.codePoint);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Literal.UnicodeEscape withCodePoint(final String codePoint) {
                return this.codePoint == codePoint ? this : new J.Literal.UnicodeEscape(this.valueSourceIndex, codePoint);
            }
        }

        /**
         * Checks if the given {@link Expression} is a {@link Literal} with the given value.
         *
         * @param maybeLiteral An expression that may be an {@link Literal}.
         * @param value        The value to compare against.
         * @return {@code true} if the given {@link Expression} is a {@link Literal} with the given value.
         */
        @Incubating(since = "7.25.0")
        public static boolean isLiteralValue(@Nullable Expression maybeLiteral, Object value) {
            if (maybeLiteral instanceof Literal) {
                Literal literal = (Literal) maybeLiteral;
                return literal.getValue() != null && literal.getValue().equals(value);
            }
            return false;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Literal)) return false;
            final J.Literal other = (J.Literal) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Literal(final UUID id, final Space prefix, final Markers markers, @Nullable final Object value, @Nullable final String valueSource, @Nullable final List<UnicodeEscape> unicodeEscapes, final JavaType.Primitive type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.value = value;
            this.valueSource = valueSource;
            this.unicodeEscapes = unicodeEscapes;
            this.type = type;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public Object getValue() {
            return this.value;
        }

        @Nullable
        public String getValueSource() {
            return this.valueSource;
        }

        @Nullable
        public List<UnicodeEscape> getUnicodeEscapes() {
            return this.unicodeEscapes;
        }

        /**
         * Including String literals
         */
        public JavaType.Primitive getType() {
            return this.type;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Literal(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", value=" + this.getValue() + ", valueSource=" + this.getValueSource() + ", unicodeEscapes=" + this.getUnicodeEscapes() + ", type=" + this.getType() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withId(final UUID id) {
            return this.id == id ? this : new J.Literal(id, this.prefix, this.markers, this.value, this.valueSource, this.unicodeEscapes, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Literal(this.id, prefix, this.markers, this.value, this.valueSource, this.unicodeEscapes, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Literal(this.id, this.prefix, markers, this.value, this.valueSource, this.unicodeEscapes, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withValue(@Nullable final Object value) {
            return this.value == value ? this : new J.Literal(this.id, this.prefix, this.markers, value, this.valueSource, this.unicodeEscapes, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withValueSource(@Nullable final String valueSource) {
            return this.valueSource == valueSource ? this : new J.Literal(this.id, this.prefix, this.markers, this.value, valueSource, this.unicodeEscapes, this.type);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Literal withUnicodeEscapes(@Nullable final List<UnicodeEscape> unicodeEscapes) {
            return this.unicodeEscapes == unicodeEscapes ? this : new J.Literal(this.id, this.prefix, this.markers, this.value, this.valueSource, unicodeEscapes, this.type);
        }
    }


    final class MemberReference implements J, Expression, TypedTree, MethodCall {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<Expression> containing;

        public Expression getContaining() {
            return containing.getElement();
        }

        public MemberReference withContaining(Expression containing) {
            //noinspection ConstantConditions
            return getPadding().withContaining(JRightPadded.withElement(this.containing, containing));
        }

        @Nullable
        private final JContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public MemberReference withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Override
        public List<Expression> getArguments() {
            return emptyList();
        }

        @Override
        public MemberReference withArguments(List<Expression> arguments) {
            return this;
        }

        private final JLeftPadded<Identifier> reference;

        public Identifier getReference() {
            return reference.getElement();
        }

        public MemberReference withReference(Identifier reference) {
            return getPadding().withReference(this.reference.withElement(reference));
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        /**
         * In the case of a method reference, this will be the type of the functional interface that
         * this method reference is supplying.
         */
        @Nullable
        private final JavaType type;
        /**
         * In the case of a method reference, this is the method type pointed to by {@link #reference}.
         */
        @Nullable
        private final JavaType.Method methodType;
        /**
         * In the case of a field reference, this is the field pointed to by {@link #reference}.
         */
        @Nullable
        private final JavaType.Variable variableType;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMemberReference(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final MemberReference t;

            public JRightPadded<Expression> getContaining() {
                return t.containing;
            }

            public MemberReference withContaining(JRightPadded<Expression> containing) {
                return t.containing == containing ? t : new MemberReference(t.id, t.prefix, t.markers, containing, t.typeParameters, t.reference, t.type, t.methodType, t.variableType);
            }

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MemberReference withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, typeParameters, t.reference, t.type, t.methodType, t.variableType);
            }

            public JLeftPadded<Identifier> getReference() {
                return t.reference;
            }

            public MemberReference withReference(JLeftPadded<Identifier> reference) {
                return t.reference == reference ? t : new MemberReference(t.id, t.prefix, t.markers, t.containing, t.typeParameters, reference, t.type, t.methodType, t.variableType);
            }

            public Padding(final MemberReference t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.MemberReference)) return false;
            final J.MemberReference other = (J.MemberReference) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public MemberReference(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> containing, @Nullable final JContainer<Expression> typeParameters, final JLeftPadded<Identifier> reference, @Nullable final JavaType type, @Nullable final JavaType.Method methodType, @Nullable final JavaType.Variable variableType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.containing = containing;
            this.typeParameters = typeParameters;
            this.reference = reference;
            this.type = type;
            this.methodType = methodType;
            this.variableType = variableType;
        }

        private MemberReference(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> containing, @Nullable final JContainer<Expression> typeParameters, final JLeftPadded<Identifier> reference, @Nullable final JavaType type, @Nullable final JavaType.Method methodType, @Nullable final JavaType.Variable variableType) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.containing = containing;
            this.typeParameters = typeParameters;
            this.reference = reference;
            this.type = type;
            this.methodType = methodType;
            this.variableType = variableType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withId(final UUID id) {
            return this.id == id ? this : new J.MemberReference(this.padding, id, this.prefix, this.markers, this.containing, this.typeParameters, this.reference, this.type, this.methodType, this.variableType);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.MemberReference(this.padding, this.id, prefix, this.markers, this.containing, this.typeParameters, this.reference, this.type, this.methodType, this.variableType);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.MemberReference(this.padding, this.id, this.prefix, markers, this.containing, this.typeParameters, this.reference, this.type, this.methodType, this.variableType);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * In the case of a method reference, this will be the type of the functional interface that
         * this method reference is supplying.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.MemberReference(this.padding, this.id, this.prefix, this.markers, this.containing, this.typeParameters, this.reference, type, this.methodType, this.variableType);
        }

        /**
         * In the case of a method reference, this will be the type of the functional interface that
         * this method reference is supplying.
         */
        @Nullable
        public JavaType getType() {
            return this.type;
        }

        /**
         * In the case of a method reference, this is the method type pointed to by {@link #reference}.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withMethodType(@Nullable final JavaType.Method methodType) {
            return this.methodType == methodType ? this : new J.MemberReference(this.padding, this.id, this.prefix, this.markers, this.containing, this.typeParameters, this.reference, this.type, methodType, this.variableType);
        }

        /**
         * In the case of a method reference, this is the method type pointed to by {@link #reference}.
         */
        @Nullable
        public JavaType.Method getMethodType() {
            return this.methodType;
        }

        /**
         * In the case of a field reference, this is the field pointed to by {@link #reference}.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MemberReference withVariableType(@Nullable final JavaType.Variable variableType) {
            return this.variableType == variableType ? this : new J.MemberReference(this.padding, this.id, this.prefix, this.markers, this.containing, this.typeParameters, this.reference, this.type, this.methodType, variableType);
        }

        /**
         * In the case of a field reference, this is the field pointed to by {@link #reference}.
         */
        @Nullable
        public JavaType.Variable getVariableType() {
            return this.variableType;
        }
    }


    final class MethodDeclaration implements J, Statement, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        @Nullable
        private transient WeakReference<Annotations> annotations;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<Annotation> leadingAnnotations;
        private final List<Modifier> modifiers;
        @Nullable
        private final TypeParameters typeParameters;

        @Nullable
        public List<TypeParameter> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getTypeParameters();
        }

        public MethodDeclaration withTypeParameters(@Nullable List<TypeParameter> typeParameters) {
            if (typeParameters == null) {
                if (this.getAnnotations().getTypeParameters() == null) {
                    return this;
                } else {
                    return this.getAnnotations().withTypeParameters(null);
                }
            } else {
                TypeParameters currentTypeParameters = this.getAnnotations().getTypeParameters();
                if (currentTypeParameters == null) {
                    return getAnnotations().withTypeParameters(new TypeParameters(Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, typeParameters.stream().map(JRightPadded::build).collect(toList())));
                } else {
                    return getAnnotations().withTypeParameters(currentTypeParameters.withTypeParameters(typeParameters));
                }
            }
        }

        /**
         * Null for constructor declarations.
         */
        @Nullable
        private final TypeTree returnTypeExpression;
        private final IdentifierWithAnnotations name;

        public Identifier getName() {
            return name.getIdentifier();
        }

        public MethodDeclaration withName(Identifier name) {
            return getAnnotations().withName(this.name.withIdentifier(name));
        }

        private final JContainer<Statement> parameters;

        public List<Statement> getParameters() {
            return parameters.getElements();
        }

        public MethodDeclaration withParameters(List<Statement> parameters) {
            return getPadding().withParameters(JContainer.withElements(this.parameters, parameters));
        }

        @Nullable
        private final JContainer<NameTree> throwz;

        @Nullable
        public List<NameTree> getThrows() {
            return throwz == null ? null : throwz.getElements();
        }

        public MethodDeclaration withThrows(@Nullable List<NameTree> throwz) {
            return getPadding().withThrows(JContainer.withElementsNullable(this.throwz, throwz));
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @Nullable
        private final Block body;
        /**
         * For default values on definitions of annotation parameters.
         */
        @Nullable
        private final JLeftPadded<Expression> defaultValue;

        @Nullable
        public Expression getDefaultValue() {
            return defaultValue == null ? null : defaultValue.getElement();
        }

        public MethodDeclaration withDefaultValue(@Nullable Expression defaultValue) {
            return getPadding().withDefaultValue(JLeftPadded.withElement(this.defaultValue, defaultValue));
        }

        @Nullable
        private final JavaType.Method methodType;

        public MethodDeclaration withMethodType(@Nullable JavaType.Method type) {
            if (type == this.methodType) {
                return this;
            }
            return new MethodDeclaration(id, prefix, markers, leadingAnnotations, modifiers, typeParameters, returnTypeExpression, name, parameters, throwz, body, defaultValue, type);
        }

        public JavaType getType() {
            return methodType == null ? null : methodType.getReturnType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public MethodDeclaration withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this method declaration, use withMethodType(..)");
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMethodDeclaration(this, p);
        }

        public boolean isAbstract() {
            return body == null;
        }

        public boolean isConstructor() {
            return getReturnTypeExpression() == null;
        }

        public String getSimpleName() {
            return name.getIdentifier().getSimpleName();
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        @Override
        @Transient
        public CoordinateBuilder.MethodDeclaration getCoordinates() {
            return new CoordinateBuilder.MethodDeclaration(this);
        }

        // gather annotations from everywhere they may occur
        /**
         * @deprecated Use {@link org.openrewrite.java.service.AnnotationService#getAllAnnotations(J)} instead.
         */
        @Deprecated
        public List<J.Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (J.Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            if (typeParameters != null) {
                allAnnotations.addAll(typeParameters.getAnnotations());
            }
            if (returnTypeExpression instanceof AnnotatedType) {
                allAnnotations.addAll(((AnnotatedType) returnTypeExpression).getAnnotations());
            }
            allAnnotations.addAll(name.getAnnotations());
            return allAnnotations;
        }

        @Override
        public String toString() {
            return "MethodDeclaration{" + (getMethodType() == null ? "unknown" : getMethodType()) + "}";
        }


        public static final class IdentifierWithAnnotations {
            private final Identifier identifier;
            private final List<Annotation> annotations;

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.MethodDeclaration.IdentifierWithAnnotations)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int result = 1;
                return result;
            }

            public IdentifierWithAnnotations(final Identifier identifier, final List<Annotation> annotations) {
                this.identifier = identifier;
                this.annotations = annotations;
            }

            public Identifier getIdentifier() {
                return this.identifier;
            }

            public List<Annotation> getAnnotations() {
                return this.annotations;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.MethodDeclaration.IdentifierWithAnnotations(identifier=" + this.getIdentifier() + ", annotations=" + this.getAnnotations() + ")";
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.MethodDeclaration.IdentifierWithAnnotations withIdentifier(final Identifier identifier) {
                return this.identifier == identifier ? this : new J.MethodDeclaration.IdentifierWithAnnotations(identifier, this.annotations);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.MethodDeclaration.IdentifierWithAnnotations withAnnotations(final List<Annotation> annotations) {
                return this.annotations == annotations ? this : new J.MethodDeclaration.IdentifierWithAnnotations(this.identifier, annotations);
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final MethodDeclaration t;

            public JContainer<Statement> getParameters() {
                return t.parameters;
            }

            public MethodDeclaration withParameters(JContainer<Statement> parameters) {
                return t.parameters == parameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, parameters, t.throwz, t.body, t.defaultValue, t.methodType);
            }

            @Nullable
            public JContainer<NameTree> getThrows() {
                return t.throwz;
            }

            public MethodDeclaration withThrows(@Nullable JContainer<NameTree> throwz) {
                return t.throwz == throwz ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, t.parameters, throwz, t.body, t.defaultValue, t.methodType);
            }

            @Nullable
            public JLeftPadded<Expression> getDefaultValue() {
                return t.defaultValue;
            }

            public MethodDeclaration withDefaultValue(@Nullable JLeftPadded<Expression> defaultValue) {
                return t.defaultValue == defaultValue ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, defaultValue, t.methodType);
            }

            @Nullable
            public TypeParameters getTypeParameters() {
                return t.typeParameters;
            }

            public MethodDeclaration withTypeParameters(@Nullable TypeParameters typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, t.defaultValue, t.methodType);
            }

            public Padding(final MethodDeclaration t) {
                this.t = t;
            }
        }

        public Annotations getAnnotations() {
            Annotations a;
            if (this.annotations == null) {
                a = new Annotations(this);
                this.annotations = new WeakReference<>(a);
            } else {
                a = this.annotations.get();
                if (a == null || a.t != this) {
                    a = new Annotations(this);
                    this.annotations = new WeakReference<>(a);
                }
            }
            return a;
        }


        public static class Annotations {
            private final MethodDeclaration t;

            @Nullable
            public TypeParameters getTypeParameters() {
                return t.typeParameters;
            }

            public MethodDeclaration withTypeParameters(@Nullable TypeParameters typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, typeParameters, t.returnTypeExpression, t.name, t.parameters, t.throwz, t.body, t.defaultValue, t.methodType);
            }

            public IdentifierWithAnnotations getName() {
                return t.name;
            }

            public MethodDeclaration withName(IdentifierWithAnnotations name) {
                return t.name == name ? t : new MethodDeclaration(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeParameters, t.returnTypeExpression, name, t.parameters, t.throwz, t.body, t.defaultValue, t.methodType);
            }

            public Annotations(final MethodDeclaration t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.MethodDeclaration)) return false;
            final J.MethodDeclaration other = (J.MethodDeclaration) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public MethodDeclaration(final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, @Nullable final TypeParameters typeParameters, @Nullable final TypeTree returnTypeExpression, final IdentifierWithAnnotations name, final JContainer<Statement> parameters, @Nullable final JContainer<NameTree> throwz, @Nullable final Block body, @Nullable final JLeftPadded<Expression> defaultValue, @Nullable final JavaType.Method methodType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.typeParameters = typeParameters;
            this.returnTypeExpression = returnTypeExpression;
            this.name = name;
            this.parameters = parameters;
            this.throwz = throwz;
            this.body = body;
            this.defaultValue = defaultValue;
            this.methodType = methodType;
        }

        private MethodDeclaration(@Nullable final WeakReference<Padding> padding, @Nullable final WeakReference<Annotations> annotations, final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, @Nullable final TypeParameters typeParameters, @Nullable final TypeTree returnTypeExpression, final IdentifierWithAnnotations name, final JContainer<Statement> parameters, @Nullable final JContainer<NameTree> throwz, @Nullable final Block body, @Nullable final JLeftPadded<Expression> defaultValue, @Nullable final JavaType.Method methodType) {
            this.padding = padding;
            this.annotations = annotations;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.typeParameters = typeParameters;
            this.returnTypeExpression = returnTypeExpression;
            this.name = name;
            this.parameters = parameters;
            this.throwz = throwz;
            this.body = body;
            this.defaultValue = defaultValue;
            this.methodType = methodType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withId(final UUID id) {
            return this.id == id ? this : new J.MethodDeclaration(this.padding, this.annotations, id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, this.prefix, markers, this.leadingAnnotations, this.modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withLeadingAnnotations(final List<Annotation> leadingAnnotations) {
            return this.leadingAnnotations == leadingAnnotations ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, leadingAnnotations, this.modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        public List<Annotation> getLeadingAnnotations() {
            return this.leadingAnnotations;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withModifiers(final List<Modifier> modifiers) {
            return this.modifiers == modifiers ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        public List<Modifier> getModifiers() {
            return this.modifiers;
        }

        /**
         * Null for constructor declarations.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withReturnTypeExpression(@Nullable final TypeTree returnTypeExpression) {
            return this.returnTypeExpression == returnTypeExpression ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeParameters, returnTypeExpression, this.name, this.parameters, this.throwz, this.body, this.defaultValue, this.methodType);
        }

        /**
         * Null for constructor declarations.
         */
        @Nullable
        public TypeTree getReturnTypeExpression() {
            return this.returnTypeExpression;
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodDeclaration withBody(@Nullable final Block body) {
            return this.body == body ? this : new J.MethodDeclaration(this.padding, this.annotations, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeParameters, this.returnTypeExpression, this.name, this.parameters, this.throwz, body, this.defaultValue, this.methodType);
        }

        /**
         * Null for abstract method declarations and interface method declarations.
         */
        @Nullable
        public Block getBody() {
            return this.body;
        }

        @Nullable
        public JavaType.Method getMethodType() {
            return this.methodType;
        }
    }


    final class MethodInvocation implements J, Statement, Expression, TypedTree, MethodCall {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        /**
         * Right padded before the '.'
         */
        @Nullable
        private final JRightPadded<Expression> select;

        @Nullable
        public Expression getSelect() {
            return select == null ? null : select.getElement();
        }

        public MethodInvocation withSelect(@Nullable Expression select) {
            return getPadding().withSelect(JRightPadded.withElement(this.select, select));
        }

        @Nullable
        private final JContainer<Expression> typeParameters;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        private final Identifier name;

        public MethodInvocation withName(J.Identifier name) {
            if (this.name == name) {
                return this;
            }
            JavaType.Method newType = null;
            if (this.methodType != null) {
                if (name.getType() instanceof JavaType.Method && name.getType() != this.methodType) {
                    newType = (JavaType.Method) name.getType();
                } else {
                    newType = this.methodType.getName().equals(name.getSimpleName()) ? this.methodType : this.methodType.withName(name.getSimpleName());
                }
            }
            return new MethodInvocation(id, prefix, markers, select, typeParameters, name.withType(newType), arguments, newType);
        }

        private final JContainer<Expression> arguments;

        @Override
        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        @Override
        public MethodInvocation withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @Nullable
        private final JavaType.Method methodType;

        @Override
        public MethodInvocation withMethodType(@Nullable JavaType.Method type) {
            if (type == this.methodType) {
                return this;
            }
            return new MethodInvocation(id, prefix, markers, select, typeParameters, name, arguments, type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MethodInvocation withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this method invocation, use withMethodType(..)");
        }

        public MethodInvocation withDeclaringType(JavaType.FullyQualified type) {
            if (this.methodType == null) {
                return this;
            } else {
                return withMethodType(this.methodType.withDeclaringType(type));
            }
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMethodInvocation(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.MethodInvocation getCoordinates() {
            return new CoordinateBuilder.MethodInvocation(this);
        }

        @Override
        @Nullable
        public JavaType getType() {
            return methodType == null ? null : methodType.getReturnType();
        }

        public String getSimpleName() {
            return name.getSimpleName();
        }

        @Transient
        @Override
        public List<J> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final MethodInvocation t;

            @Nullable
            public JRightPadded<Expression> getSelect() {
                return t.select;
            }

            public MethodInvocation withSelect(@Nullable JRightPadded<Expression> select) {
                return t.select == select ? t : new MethodInvocation(t.id, t.prefix, t.markers, select, t.typeParameters, t.name, t.arguments, t.methodType);
            }

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public MethodInvocation withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, typeParameters, t.name, t.arguments, t.methodType);
            }

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public MethodInvocation withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new MethodInvocation(t.id, t.prefix, t.markers, t.select, t.typeParameters, t.name, arguments, t.methodType);
            }

            public Padding(final MethodInvocation t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.MethodInvocation)) return false;
            final J.MethodInvocation other = (J.MethodInvocation) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public MethodInvocation(final UUID id, final Space prefix, final Markers markers, @Nullable final JRightPadded<Expression> select, @Nullable final JContainer<Expression> typeParameters, final Identifier name, final JContainer<Expression> arguments, @Nullable final JavaType.Method methodType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.select = select;
            this.typeParameters = typeParameters;
            this.name = name;
            this.arguments = arguments;
            this.methodType = methodType;
        }

        private MethodInvocation(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, @Nullable final JRightPadded<Expression> select, @Nullable final JContainer<Expression> typeParameters, final Identifier name, final JContainer<Expression> arguments, @Nullable final JavaType.Method methodType) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.select = select;
            this.typeParameters = typeParameters;
            this.name = name;
            this.arguments = arguments;
            this.methodType = methodType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodInvocation withId(final UUID id) {
            return this.id == id ? this : new J.MethodInvocation(this.padding, id, this.prefix, this.markers, this.select, this.typeParameters, this.name, this.arguments, this.methodType);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodInvocation withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.MethodInvocation(this.padding, this.id, prefix, this.markers, this.select, this.typeParameters, this.name, this.arguments, this.methodType);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodInvocation withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.MethodInvocation(this.padding, this.id, this.prefix, markers, this.select, this.typeParameters, this.name, this.arguments, this.methodType);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MethodInvocation withTypeParameters(@Nullable final JContainer<Expression> typeParameters) {
            return this.typeParameters == typeParameters ? this : new J.MethodInvocation(this.padding, this.id, this.prefix, this.markers, this.select, typeParameters, this.name, this.arguments, this.methodType);
        }

        public Identifier getName() {
            return this.name;
        }

        @Nullable
        public JavaType.Method getMethodType() {
            return this.methodType;
        }
    }


    final class Modifier implements J {
        public static boolean hasModifier(Collection<Modifier> modifiers, Modifier.Type modifier) {
            return modifiers.stream().anyMatch(m -> m.getType() == modifier);
        }

        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        /**
         * For languages other than Java the type will be Modifier.Type.LanguageExtension and its text will be this keyword.
         * For all keywords which appear in Java this will be null.
         */
        @Nullable
        private final String keyword;
        private final Type type;
        private final List<Annotation> annotations;

        /**
         * @deprecated Use {@link #Modifier(UUID, Space, Markers, String, Type, List)} instead.
         */
        @Deprecated
        public Modifier(UUID id, Space prefix, Markers markers, Type type, List<Annotation> annotations) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.keyword = null;
            this.type = type;
            this.annotations = annotations;
        }

        @Override
        public String toString() {
            return type.toString().toLowerCase();
        }


        /**
         * These types are sorted in order of their recommended appearance in a list of modifiers, as defined in the
         * <a href="https://rules.sonarsource.com/java/tag/convention/RSPEC-1124">JLS</a>.
         */
        public enum Type {
            Default, Public, Protected, Private, Abstract, Static, Final, Sealed, NonSealed, Transient, Volatile, Synchronized, Native, Strictfp, Async, Reified, Inline, LanguageExtension;
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Modifier)) return false;
            final J.Modifier other = (J.Modifier) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Modifier(final UUID id, final Space prefix, final Markers markers, @Nullable final String keyword, final Type type, final List<Annotation> annotations) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.keyword = keyword;
            this.type = type;
            this.annotations = annotations;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * For languages other than Java the type will be Modifier.Type.LanguageExtension and its text will be this keyword.
         * For all keywords which appear in Java this will be null.
         */
        @Nullable
        public String getKeyword() {
            return this.keyword;
        }

        public Type getType() {
            return this.type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withId(final UUID id) {
            return this.id == id ? this : new J.Modifier(id, this.prefix, this.markers, this.keyword, this.type, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Modifier(this.id, prefix, this.markers, this.keyword, this.type, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Modifier(this.id, this.prefix, markers, this.keyword, this.type, this.annotations);
        }

        /**
         * For languages other than Java the type will be Modifier.Type.LanguageExtension and its text will be this keyword.
         * For all keywords which appear in Java this will be null.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withKeyword(@Nullable final String keyword) {
            return this.keyword == keyword ? this : new J.Modifier(this.id, this.prefix, this.markers, keyword, this.type, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withType(final Type type) {
            return this.type == type ? this : new J.Modifier(this.id, this.prefix, this.markers, this.keyword, type, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Modifier withAnnotations(final List<Annotation> annotations) {
            return this.annotations == annotations ? this : new J.Modifier(this.id, this.prefix, this.markers, this.keyword, this.type, annotations);
        }

        public List<Annotation> getAnnotations() {
            return this.annotations;
        }
    }


    /**
     * For modifiers not seen in Java this is used in conjunction with "keyword"
     */
    final class MultiCatch implements J, TypeTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<JRightPadded<NameTree>> alternatives;

        public List<NameTree> getAlternatives() {
            return JRightPadded.getElements(alternatives);
        }

        public MultiCatch withAlternatives(List<NameTree> alternatives) {
            return getPadding().withAlternatives(JRightPadded.withElements(this.alternatives, alternatives));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitMultiCatch(this, p);
        }

        @SuppressWarnings("unchecked")
        @Override
        public MultiCatch withType(@Nullable JavaType type) {
            // cannot overwrite type directly, perform this operation on each alternative separately
            return this;
        }

        @Override
        public JavaType getType() {
            return new JavaType.MultiCatch(alternatives.stream().filter(Objects::nonNull).map(alt -> alt.getElement().getType()).collect(toList()));
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final MultiCatch t;

            public List<JRightPadded<NameTree>> getAlternatives() {
                return t.alternatives;
            }

            public MultiCatch withAlternatives(List<JRightPadded<NameTree>> alternatives) {
                return t.alternatives == alternatives ? t : new MultiCatch(t.id, t.prefix, t.markers, alternatives);
            }

            public Padding(final MultiCatch t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.MultiCatch)) return false;
            final J.MultiCatch other = (J.MultiCatch) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public MultiCatch(final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<NameTree>> alternatives) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.alternatives = alternatives;
        }

        private MultiCatch(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<JRightPadded<NameTree>> alternatives) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.alternatives = alternatives;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MultiCatch withId(final UUID id) {
            return this.id == id ? this : new J.MultiCatch(this.padding, id, this.prefix, this.markers, this.alternatives);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MultiCatch withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.MultiCatch(this.padding, this.id, prefix, this.markers, this.alternatives);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.MultiCatch withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.MultiCatch(this.padding, this.id, this.prefix, markers, this.alternatives);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class NewArray implements J, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final TypeTree typeExpression;
        private final List<ArrayDimension> dimensions;
        @Nullable
        private final JContainer<Expression> initializer;

        @Nullable
        public List<Expression> getInitializer() {
            return initializer == null ? null : initializer.getElements();
        }

        public NewArray withInitializer(List<Expression> initializer) {
            return getPadding().withInitializer(JContainer.withElementsNullable(this.initializer, initializer));
        }

        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitNewArray(this, p);
        }

        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final NewArray t;

            @Nullable
            public JContainer<Expression> getInitializer() {
                return t.initializer;
            }

            public NewArray withInitializer(@Nullable JContainer<Expression> initializer) {
                return t.initializer == initializer ? t : new NewArray(t.id, t.prefix, t.markers, t.typeExpression, t.dimensions, initializer, t.type);
            }

            public Padding(final NewArray t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.NewArray(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", typeExpression=" + this.getTypeExpression() + ", dimensions=" + this.getDimensions() + ", initializer=" + this.getInitializer() + ", type=" + this.getType() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.NewArray)) return false;
            final J.NewArray other = (J.NewArray) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public NewArray(final UUID id, final Space prefix, final Markers markers, @Nullable final TypeTree typeExpression, final List<ArrayDimension> dimensions, @Nullable final JContainer<Expression> initializer, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.typeExpression = typeExpression;
            this.dimensions = dimensions;
            this.initializer = initializer;
            this.type = type;
        }

        private NewArray(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, @Nullable final TypeTree typeExpression, final List<ArrayDimension> dimensions, @Nullable final JContainer<Expression> initializer, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.typeExpression = typeExpression;
            this.dimensions = dimensions;
            this.initializer = initializer;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withId(final UUID id) {
            return this.id == id ? this : new J.NewArray(this.padding, id, this.prefix, this.markers, this.typeExpression, this.dimensions, this.initializer, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.NewArray(this.padding, this.id, prefix, this.markers, this.typeExpression, this.dimensions, this.initializer, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.NewArray(this.padding, this.id, this.prefix, markers, this.typeExpression, this.dimensions, this.initializer, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withTypeExpression(@Nullable final TypeTree typeExpression) {
            return this.typeExpression == typeExpression ? this : new J.NewArray(this.padding, this.id, this.prefix, this.markers, typeExpression, this.dimensions, this.initializer, this.type);
        }

        @Nullable
        public TypeTree getTypeExpression() {
            return this.typeExpression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withDimensions(final List<ArrayDimension> dimensions) {
            return this.dimensions == dimensions ? this : new J.NewArray(this.padding, this.id, this.prefix, this.markers, this.typeExpression, dimensions, this.initializer, this.type);
        }

        public List<ArrayDimension> getDimensions() {
            return this.dimensions;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewArray withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.NewArray(this.padding, this.id, this.prefix, this.markers, this.typeExpression, this.dimensions, this.initializer, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class ArrayDimension implements J {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<Expression> index;

        public Expression getIndex() {
            return index.getElement();
        }

        public ArrayDimension withIndex(Expression index) {
            return getPadding().withIndex(this.index.withElement(index));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitArrayDimension(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final ArrayDimension t;

            public JRightPadded<Expression> getIndex() {
                return t.index;
            }

            public ArrayDimension withIndex(JRightPadded<Expression> index) {
                return t.index == index ? t : new ArrayDimension(t.id, t.prefix, t.markers, index);
            }

            public Padding(final ArrayDimension t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ArrayDimension)) return false;
            final J.ArrayDimension other = (J.ArrayDimension) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ArrayDimension(final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> index) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.index = index;
        }

        private ArrayDimension(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<Expression> index) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.index = index;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayDimension withId(final UUID id) {
            return this.id == id ? this : new J.ArrayDimension(this.padding, id, this.prefix, this.markers, this.index);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayDimension withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ArrayDimension(this.padding, this.id, prefix, this.markers, this.index);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ArrayDimension withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ArrayDimension(this.padding, this.id, this.prefix, markers, this.index);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class NewClass implements J, Statement, Expression, TypedTree, MethodCall {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        /**
         * For situations like <code>this.new A()</code>.
         * Right padded before the '.'
         */
        @Nullable
        private final JRightPadded<Expression> enclosing;

        @Nullable
        public Expression getEnclosing() {
            return enclosing == null ? null : enclosing.getElement();
        }

        public NewClass withEnclosing(Expression enclosing) {
            return getPadding().withEnclosing(JRightPadded.withElement(this.enclosing, enclosing));
        }

        private final Space nooh;

        public Space getNew() {
            return nooh;
        }

        public NewClass withNew(Space nooh) {
            if (nooh == this.nooh) {
                return this;
            }
            return new NewClass(id, prefix, markers, enclosing, nooh, clazz, arguments, body, constructorType);
        }

        @Nullable
        private final TypeTree clazz;
        private final JContainer<Expression> arguments;

        @Override
        public List<Expression> getArguments() {
            return arguments.getElements();
        }

        @Override
        public NewClass withArguments(List<Expression> arguments) {
            return getPadding().withArguments(JContainer.withElements(this.arguments, arguments));
        }

        @Nullable
        private final Block body;
        @Nullable
        private final JavaType.Method constructorType;

        @Nullable
        @Override
        public JavaType getType() {
            return constructorType == null ? null : constructorType.getReturnType();
        }

        /**
         * This is an alias for {@link J.NewClass#getConstructorType()}.
         *
         * @return The constructor type.
         */
        @Nullable
        public JavaType.Method getMethodType() {
            return getConstructorType();
        }

        /**
         * This is an alias for {@link J.NewClass#withConstructorType(JavaType.Method)}.
         *
         * @param methodType The constructor type.
         * @return An instance with the new constructor type.
         */
        @Override
        public NewClass withMethodType(@Nullable JavaType.Method methodType) {
            return withConstructorType(methodType);
        }

        @SuppressWarnings("unchecked")
        @Override
        public NewClass withType(@Nullable JavaType type) {
            throw new UnsupportedOperationException("To change the return type of this new class, use withConstructorType(..)");
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitNewClass(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return singletonList(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final NewClass t;

            @Nullable
            public JRightPadded<Expression> getEnclosing() {
                return t.enclosing;
            }

            public NewClass withEnclosing(@Nullable JRightPadded<Expression> enclosing) {
                return t.enclosing == enclosing ? t : new NewClass(t.id, t.prefix, t.markers, enclosing, t.nooh, t.clazz, t.arguments, t.body, t.constructorType);
            }

            public JContainer<Expression> getArguments() {
                return t.arguments;
            }

            public NewClass withArguments(JContainer<Expression> arguments) {
                return t.arguments == arguments ? t : new NewClass(t.id, t.prefix, t.markers, t.enclosing, t.nooh, t.clazz, arguments, t.body, t.constructorType);
            }

            public Padding(final NewClass t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.NewClass)) return false;
            final J.NewClass other = (J.NewClass) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public NewClass(final UUID id, final Space prefix, final Markers markers, @Nullable final JRightPadded<Expression> enclosing, final Space nooh, @Nullable final TypeTree clazz, final JContainer<Expression> arguments, @Nullable final Block body, @Nullable final JavaType.Method constructorType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.enclosing = enclosing;
            this.nooh = nooh;
            this.clazz = clazz;
            this.arguments = arguments;
            this.body = body;
            this.constructorType = constructorType;
        }

        private NewClass(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, @Nullable final JRightPadded<Expression> enclosing, final Space nooh, @Nullable final TypeTree clazz, final JContainer<Expression> arguments, @Nullable final Block body, @Nullable final JavaType.Method constructorType) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.enclosing = enclosing;
            this.nooh = nooh;
            this.clazz = clazz;
            this.arguments = arguments;
            this.body = body;
            this.constructorType = constructorType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withId(final UUID id) {
            return this.id == id ? this : new J.NewClass(this.padding, id, this.prefix, this.markers, this.enclosing, this.nooh, this.clazz, this.arguments, this.body, this.constructorType);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.NewClass(this.padding, this.id, prefix, this.markers, this.enclosing, this.nooh, this.clazz, this.arguments, this.body, this.constructorType);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.NewClass(this.padding, this.id, this.prefix, markers, this.enclosing, this.nooh, this.clazz, this.arguments, this.body, this.constructorType);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withClazz(@Nullable final TypeTree clazz) {
            return this.clazz == clazz ? this : new J.NewClass(this.padding, this.id, this.prefix, this.markers, this.enclosing, this.nooh, clazz, this.arguments, this.body, this.constructorType);
        }

        @Nullable
        public TypeTree getClazz() {
            return this.clazz;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withBody(@Nullable final Block body) {
            return this.body == body ? this : new J.NewClass(this.padding, this.id, this.prefix, this.markers, this.enclosing, this.nooh, this.clazz, this.arguments, body, this.constructorType);
        }

        @Nullable
        public Block getBody() {
            return this.body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NewClass withConstructorType(@Nullable final JavaType.Method constructorType) {
            return this.constructorType == constructorType ? this : new J.NewClass(this.padding, this.id, this.prefix, this.markers, this.enclosing, this.nooh, this.clazz, this.arguments, this.body, constructorType);
        }

        @Nullable
        public JavaType.Method getConstructorType() {
            return this.constructorType;
        }
    }


    @Incubating(since = "8.12.0")
    class NullableType implements J, TypeTree, Expression {
        @Nullable
        private transient WeakReference<J.NullableType.Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<J.Annotation> annotations;
        private final JRightPadded<TypeTree> typeTree;

        public TypeTree getTypeTree() {
            return typeTree.getElement();
        }

        @Override
        @Nullable
        public JavaType getType() {
            // TODO also support `nullable` in type attribution
            return typeTree.getElement().getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public NullableType withType(@Nullable JavaType type) {
            JRightPadded<TypeTree> rp = getPadding().getTypeTree();
            TypeTree tt = rp.getElement();
            tt = tt.withType(type);
            return getPadding().withTypeTree(rp.withElement(tt));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitNullableType(this, p);
        }

        @Override
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public J.NullableType.Padding getPadding() {
            J.NullableType.Padding p;
            if (this.padding == null) {
                p = new J.NullableType.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new J.NullableType.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final J.NullableType t;

            public JRightPadded<TypeTree> getTypeTree() {
                return t.typeTree;
            }

            public J.NullableType withTypeTree(JRightPadded<TypeTree> typeTree) {
                return t.typeTree == typeTree ? t : new J.NullableType(t.id, t.prefix, t.markers, t.annotations, typeTree);
            }

            public Padding(final J.NullableType t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.NullableType)) return false;
            final J.NullableType other = (J.NullableType) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof J.NullableType;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public NullableType(final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final JRightPadded<TypeTree> typeTree) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.typeTree = typeTree;
        }

        private NullableType(@Nullable final WeakReference<J.NullableType.Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<J.Annotation> annotations, final JRightPadded<TypeTree> typeTree) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.typeTree = typeTree;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withPadding(@Nullable final WeakReference<J.NullableType.Padding> padding) {
            return this.padding == padding ? this : new J.NullableType(padding, this.id, this.prefix, this.markers, this.annotations, this.typeTree);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withId(final UUID id) {
            return this.id == id ? this : new J.NullableType(this.padding, id, this.prefix, this.markers, this.annotations, this.typeTree);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.NullableType(this.padding, this.id, prefix, this.markers, this.annotations, this.typeTree);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.NullableType(this.padding, this.id, this.prefix, markers, this.annotations, this.typeTree);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withAnnotations(final List<J.Annotation> annotations) {
            return this.annotations == annotations ? this : new J.NullableType(this.padding, this.id, this.prefix, this.markers, annotations, this.typeTree);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.NullableType withTypeTree(final JRightPadded<TypeTree> typeTree) {
            return this.typeTree == typeTree ? this : new J.NullableType(this.padding, this.id, this.prefix, this.markers, this.annotations, typeTree);
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public List<J.Annotation> getAnnotations() {
            return this.annotations;
        }
    }


    final class Package implements Statement, J {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression expression;
        private final List<Annotation> annotations;

        public String getPackageName() {
            return expression.withPrefix(Space.EMPTY).print(new Cursor(null, new J.CompilationUnit(null, null, Tree.randomId(), Space.EMPTY, Markers.EMPTY, null, null, null, false, null, null, null, null, Space.EMPTY)));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitPackage(this, p);
        }

        @Transient
        public CoordinateBuilder.Package getCoordinates() {
            return new CoordinateBuilder.Package(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Package)) return false;
            final J.Package other = (J.Package) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Package(final UUID id, final Space prefix, final Markers markers, final Expression expression, final List<Annotation> annotations) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
            this.annotations = annotations;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Expression getExpression() {
            return this.expression;
        }

        public List<Annotation> getAnnotations() {
            return this.annotations;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Package(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", expression=" + this.getExpression() + ", annotations=" + this.getAnnotations() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Package withId(final UUID id) {
            return this.id == id ? this : new J.Package(id, this.prefix, this.markers, this.expression, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Package withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Package(this.id, prefix, this.markers, this.expression, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Package withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Package(this.id, this.prefix, markers, this.expression, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Package withExpression(final Expression expression) {
            return this.expression == expression ? this : new J.Package(this.id, this.prefix, this.markers, expression, this.annotations);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Package withAnnotations(final List<Annotation> annotations) {
            return this.annotations == annotations ? this : new J.Package(this.id, this.prefix, this.markers, this.expression, annotations);
        }
    }


    final class ParameterizedType implements J, TypeTree, Expression {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final NameTree clazz;
        @Nullable
        private final JContainer<Expression> typeParameters;
        @Nullable
        private final JavaType type;

        @Nullable
        public List<Expression> getTypeParameters() {
            return typeParameters == null ? null : typeParameters.getElements();
        }

        public ParameterizedType withTypeParameters(@Nullable List<Expression> typeParameters) {
            return getPadding().withTypeParameters(JContainer.withElementsNullable(this.typeParameters, typeParameters));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitParameterizedType(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final ParameterizedType t;

            @Nullable
            public JContainer<Expression> getTypeParameters() {
                return t.typeParameters;
            }

            public ParameterizedType withTypeParameters(@Nullable JContainer<Expression> typeParameters) {
                return t.typeParameters == typeParameters ? t : new ParameterizedType(t.id, t.prefix, t.markers, t.clazz, typeParameters, t.type);
            }

            public Padding(final ParameterizedType t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ParameterizedType)) return false;
            final J.ParameterizedType other = (J.ParameterizedType) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ParameterizedType(final UUID id, final Space prefix, final Markers markers, final NameTree clazz, @Nullable final JContainer<Expression> typeParameters, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.clazz = clazz;
            this.typeParameters = typeParameters;
            this.type = type;
        }

        private ParameterizedType(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final NameTree clazz, @Nullable final JContainer<Expression> typeParameters, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.clazz = clazz;
            this.typeParameters = typeParameters;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParameterizedType withId(final UUID id) {
            return this.id == id ? this : new J.ParameterizedType(this.padding, id, this.prefix, this.markers, this.clazz, this.typeParameters, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParameterizedType withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ParameterizedType(this.padding, this.id, prefix, this.markers, this.clazz, this.typeParameters, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParameterizedType withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ParameterizedType(this.padding, this.id, this.prefix, markers, this.clazz, this.typeParameters, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParameterizedType withClazz(final NameTree clazz) {
            return this.clazz == clazz ? this : new J.ParameterizedType(this.padding, this.id, this.prefix, this.markers, clazz, this.typeParameters, this.type);
        }

        public NameTree getClazz() {
            return this.clazz;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ParameterizedType withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.ParameterizedType(this.padding, this.id, this.prefix, this.markers, this.clazz, this.typeParameters, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    class Parentheses<J2 extends J> implements J, Expression {
        @Nullable
        private transient WeakReference<Padding<J2>> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElement();
        }

        public Parentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElement(tree));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitParentheses(this, p);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return tree.getElement() instanceof Expression ? ((Expression) tree.getElement()).getSideEffects() : emptyList();
        }

        @Override
        public JavaType getType() {
            return tree.getElement() instanceof Expression ? ((Expression) tree.getElement()).getType() : tree.getElement() instanceof NameTree ? ((NameTree) tree.getElement()).getType() : null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Parentheses<J2> withType(@Nullable JavaType type) {
            return tree.getElement() instanceof Expression ? withTree(((Expression) tree.getElement()).withType(type)) : tree.getElement() instanceof NameTree ? withTree(((NameTree) tree.getElement()).withType(type)) : this;
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding<J2> getPadding() {
            Padding<J2> p;
            if (this.padding == null) {
                p = new Padding<>(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding<>(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding<J2 extends J> {
            private final Parentheses<J2> t;

            public JRightPadded<J2> getTree() {
                return t.tree;
            }

            public Parentheses<J2> withTree(JRightPadded<J2> tree) {
                return t.tree == tree ? t : new Parentheses<>(t.id, t.prefix, t.markers, tree);
            }

            public Padding(final Parentheses<J2> t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Parentheses)) return false;
            final J.Parentheses<?> other = (J.Parentheses<?>) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
            return other instanceof J.Parentheses;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Parentheses(final UUID id, final Space prefix, final Markers markers, final JRightPadded<J2> tree) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.tree = tree;
        }

        private Parentheses(@Nullable final WeakReference<Padding<J2>> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<J2> tree) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.tree = tree;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Parentheses<J2> withId(final UUID id) {
            return this.id == id ? this : new J.Parentheses<J2>(this.padding, id, this.prefix, this.markers, this.tree);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Parentheses<J2> withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Parentheses<J2>(this.padding, this.id, prefix, this.markers, this.tree);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Parentheses<J2> withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Parentheses<J2>(this.padding, this.id, this.prefix, markers, this.tree);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class ControlParentheses<J2 extends J> implements J, Expression {
        @Nullable
        private transient WeakReference<Padding<J2>> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JRightPadded<J2> tree;

        public J2 getTree() {
            return tree.getElement();
        }

        public ControlParentheses<J2> withTree(J2 tree) {
            return getPadding().withTree(this.tree.withElement(tree));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitControlParentheses(this, p);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return tree.getElement() instanceof Expression ? ((Expression) tree.getElement()).getSideEffects() : emptyList();
        }

        @Override
        public JavaType getType() {
            J2 element = tree.getElement();
            if (element instanceof Expression) {
                return ((Expression) element).getType();
            }
            if (element instanceof NameTree) {
                return ((NameTree) element).getType();
            }
            if (element instanceof J.VariableDeclarations) {
                return ((VariableDeclarations) element).getType();
            }
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ControlParentheses<J2> withType(@Nullable JavaType type) {
            return tree.getElement() instanceof Expression ? withTree(((Expression) tree.getElement()).withType(type)) : tree.getElement() instanceof NameTree ? withTree(((NameTree) tree.getElement()).withType(type)) : this;
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        public Padding<J2> getPadding() {
            Padding<J2> p;
            if (this.padding == null) {
                p = new Padding<>(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding<>(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding<J2 extends J> {
            private final ControlParentheses<J2> t;

            public JRightPadded<J2> getTree() {
                return t.tree;
            }

            public ControlParentheses<J2> withTree(JRightPadded<J2> tree) {
                return t.tree == tree ? t : new ControlParentheses<>(t.id, t.prefix, t.markers, tree);
            }

            public Padding(final ControlParentheses<J2> t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.ControlParentheses)) return false;
            final J.ControlParentheses<?> other = (J.ControlParentheses<?>) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public ControlParentheses(final UUID id, final Space prefix, final Markers markers, final JRightPadded<J2> tree) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.tree = tree;
        }

        private ControlParentheses(@Nullable final WeakReference<Padding<J2>> padding, final UUID id, final Space prefix, final Markers markers, final JRightPadded<J2> tree) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.tree = tree;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ControlParentheses<J2> withId(final UUID id) {
            return this.id == id ? this : new J.ControlParentheses<J2>(this.padding, id, this.prefix, this.markers, this.tree);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ControlParentheses<J2> withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.ControlParentheses<J2>(this.padding, this.id, prefix, this.markers, this.tree);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.ControlParentheses<J2> withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.ControlParentheses<J2>(this.padding, this.id, this.prefix, markers, this.tree);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class Primitive implements J, TypeTree, Expression {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JavaType.Primitive type;

        @SuppressWarnings("unchecked")
        @Override
        public Primitive withType(@Nullable JavaType type) {
            if (type == this.type) {
                return this;
            }
            if (!(type instanceof JavaType.Primitive)) {
                throw new IllegalArgumentException("Cannot apply a non-primitive type to Primitive");
            }
            return new Primitive(id, prefix, markers, (JavaType.Primitive) type);
        }

        @Override
        @NonNull
        public JavaType.Primitive getType() {
            return type;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitPrimitive(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Primitive)) return false;
            final J.Primitive other = (J.Primitive) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Primitive(final UUID id, final Space prefix, final Markers markers, final JavaType.Primitive type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Primitive withId(final UUID id) {
            return this.id == id ? this : new J.Primitive(id, this.prefix, this.markers, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Primitive withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Primitive(this.id, prefix, this.markers, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Primitive withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Primitive(this.id, this.prefix, markers, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }
    }


    final class Return implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final Expression expression;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitReturn(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Return)) return false;
            final J.Return other = (J.Return) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Return(final UUID id, final Space prefix, final Markers markers, @Nullable final Expression expression) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.expression = expression;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        @Nullable
        public Expression getExpression() {
            return this.expression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Return withId(final UUID id) {
            return this.id == id ? this : new J.Return(id, this.prefix, this.markers, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Return withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Return(this.id, prefix, this.markers, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Return withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Return(this.id, this.prefix, markers, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Return withExpression(@Nullable final Expression expression) {
            return this.expression == expression ? this : new J.Return(this.id, this.prefix, this.markers, expression);
        }
    }


    final class Switch implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<Expression> selector;
        private final Block cases;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitSwitch(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Switch)) return false;
            final J.Switch other = (J.Switch) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Switch(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> selector, final Block cases) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.selector = selector;
            this.cases = cases;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public ControlParentheses<Expression> getSelector() {
            return this.selector;
        }

        public Block getCases() {
            return this.cases;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Switch(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", selector=" + this.getSelector() + ", cases=" + this.getCases() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Switch withId(final UUID id) {
            return this.id == id ? this : new J.Switch(id, this.prefix, this.markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Switch withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Switch(this.id, prefix, this.markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Switch withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Switch(this.id, this.prefix, markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Switch withSelector(final ControlParentheses<Expression> selector) {
            return this.selector == selector ? this : new J.Switch(this.id, this.prefix, this.markers, selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Switch withCases(final Block cases) {
            return this.cases == cases ? this : new J.Switch(this.id, this.prefix, this.markers, this.selector, cases);
        }
    }


    final class SwitchExpression implements J, Expression, TypedTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<Expression> selector;
        private final Block cases;

        @Override
        @Transient
        @Nullable
        public JavaType getType() {
            return new JavaVisitor<AtomicReference<JavaType>>() {
                @Override
                public J visitBlock(Block block, AtomicReference<JavaType> javaType) {
                    if (!block.getStatements().isEmpty()) {
                        Case caze = (Case) block.getStatements().get(0);
                        javaType.set(caze.getExpressions().get(0).getType());
                    }
                    return block;
                }
            }.reduce(this, new AtomicReference<>()).get();
        }

        @Override
        public <T extends J> T withType(@Nullable JavaType type) {
            // a switch expression's type is driven by its case statements
            //noinspection unchecked
            return (T) this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitSwitchExpression(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.SwitchExpression)) return false;
            final J.SwitchExpression other = (J.SwitchExpression) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public SwitchExpression(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> selector, final Block cases) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.selector = selector;
            this.cases = cases;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public ControlParentheses<Expression> getSelector() {
            return this.selector;
        }

        public Block getCases() {
            return this.cases;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.SwitchExpression(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", selector=" + this.getSelector() + ", cases=" + this.getCases() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.SwitchExpression withId(final UUID id) {
            return this.id == id ? this : new J.SwitchExpression(id, this.prefix, this.markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.SwitchExpression withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.SwitchExpression(this.id, prefix, this.markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.SwitchExpression withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.SwitchExpression(this.id, this.prefix, markers, this.selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.SwitchExpression withSelector(final ControlParentheses<Expression> selector) {
            return this.selector == selector ? this : new J.SwitchExpression(this.id, this.prefix, this.markers, selector, this.cases);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.SwitchExpression withCases(final Block cases) {
            return this.cases == cases ? this : new J.SwitchExpression(this.id, this.prefix, this.markers, this.selector, cases);
        }
    }


    final class Synchronized implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<Expression> lock;
        private final Block body;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitSynchronized(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Synchronized)) return false;
            final J.Synchronized other = (J.Synchronized) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Synchronized(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> lock, final Block body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.lock = lock;
            this.body = body;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public ControlParentheses<Expression> getLock() {
            return this.lock;
        }

        public Block getBody() {
            return this.body;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Synchronized(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", lock=" + this.getLock() + ", body=" + this.getBody() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Synchronized withId(final UUID id) {
            return this.id == id ? this : new J.Synchronized(id, this.prefix, this.markers, this.lock, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Synchronized withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Synchronized(this.id, prefix, this.markers, this.lock, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Synchronized withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Synchronized(this.id, this.prefix, markers, this.lock, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Synchronized withLock(final ControlParentheses<Expression> lock) {
            return this.lock == lock ? this : new J.Synchronized(this.id, this.prefix, this.markers, lock, this.body);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Synchronized withBody(final Block body) {
            return this.body == body ? this : new J.Synchronized(this.id, this.prefix, this.markers, this.lock, body);
        }
    }


    final class Ternary implements J, Expression, Statement, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression condition;
        private final JLeftPadded<Expression> truePart;

        public Expression getTruePart() {
            return truePart.getElement();
        }

        public Ternary withTruePart(Expression truePart) {
            return getPadding().withTruePart(this.truePart.withElement(truePart));
        }

        private final JLeftPadded<Expression> falsePart;

        public Expression getFalsePart() {
            return falsePart.getElement();
        }

        public Ternary withFalsePart(Expression falsePart) {
            return getPadding().withFalsePart(this.falsePart.withElement(falsePart));
        }

        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTernary(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Ternary t;

            public JLeftPadded<Expression> getTruePart() {
                return t.truePart;
            }

            public Ternary withTruePart(JLeftPadded<Expression> truePart) {
                return t.truePart == truePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, truePart, t.falsePart, t.type);
            }

            public JLeftPadded<Expression> getFalsePart() {
                return t.falsePart;
            }

            public Ternary withFalsePart(JLeftPadded<Expression> falsePart) {
                return t.falsePart == falsePart ? t : new Ternary(t.id, t.prefix, t.markers, t.condition, t.truePart, falsePart, t.type);
            }

            public Padding(final Ternary t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Ternary)) return false;
            final J.Ternary other = (J.Ternary) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Ternary(final UUID id, final Space prefix, final Markers markers, final Expression condition, final JLeftPadded<Expression> truePart, final JLeftPadded<Expression> falsePart, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.condition = condition;
            this.truePart = truePart;
            this.falsePart = falsePart;
            this.type = type;
        }

        private Ternary(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Expression condition, final JLeftPadded<Expression> truePart, final JLeftPadded<Expression> falsePart, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.condition = condition;
            this.truePart = truePart;
            this.falsePart = falsePart;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Ternary withId(final UUID id) {
            return this.id == id ? this : new J.Ternary(this.padding, id, this.prefix, this.markers, this.condition, this.truePart, this.falsePart, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Ternary withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Ternary(this.padding, this.id, prefix, this.markers, this.condition, this.truePart, this.falsePart, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Ternary withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Ternary(this.padding, this.id, this.prefix, markers, this.condition, this.truePart, this.falsePart, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Ternary withCondition(final Expression condition) {
            return this.condition == condition ? this : new J.Ternary(this.padding, this.id, this.prefix, this.markers, condition, this.truePart, this.falsePart, this.type);
        }

        public Expression getCondition() {
            return this.condition;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Ternary withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Ternary(this.padding, this.id, this.prefix, this.markers, this.condition, this.truePart, this.falsePart, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class Throw implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Expression exception;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitThrow(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Throw)) return false;
            final J.Throw other = (J.Throw) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Throw(final UUID id, final Space prefix, final Markers markers, final Expression exception) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.exception = exception;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Expression getException() {
            return this.exception;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Throw withId(final UUID id) {
            return this.id == id ? this : new J.Throw(id, this.prefix, this.markers, this.exception);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Throw withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Throw(this.id, prefix, this.markers, this.exception);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Throw withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Throw(this.id, this.prefix, markers, this.exception);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Throw withException(final Expression exception) {
            return this.exception == exception ? this : new J.Throw(this.id, this.prefix, this.markers, exception);
        }
    }


    final class Try implements J, Statement {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final JContainer<Resource> resources;

        @Nullable
        public List<Resource> getResources() {
            return resources == null ? null : resources.getElements();
        }

        public Try withResources(@Nullable List<Resource> resources) {
            return getPadding().withResources(JContainer.withElementsNullable(this.resources, resources));
        }

        private final Block body;
        private final List<Catch> catches;
        @Nullable
        private final JLeftPadded<Block> finallie;

        @Nullable
        public Block getFinally() {
            return finallie == null ? null : finallie.getElement();
        }

        public Try withFinally(@Nullable Block finallie) {
            return getPadding().withFinally(JLeftPadded.withElement(this.finallie, finallie));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTry(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }


        public static final class Resource implements J {
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final TypedTree variableDeclarations;
            /**
             * Only honored on the last resource in a collection of resources.
             */
            private final boolean terminatedWithSemicolon;

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitTryResource(this, p);
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.Try.Resource)) return false;
                final J.Try.Resource other = (J.Try.Resource) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Resource(final UUID id, final Space prefix, final Markers markers, final TypedTree variableDeclarations, final boolean terminatedWithSemicolon) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.variableDeclarations = variableDeclarations;
                this.terminatedWithSemicolon = terminatedWithSemicolon;
            }

            public UUID getId() {
                return this.id;
            }

            public Space getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public TypedTree getVariableDeclarations() {
                return this.variableDeclarations;
            }

            /**
             * Only honored on the last resource in a collection of resources.
             */
            public boolean isTerminatedWithSemicolon() {
                return this.terminatedWithSemicolon;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.Try.Resource(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", variableDeclarations=" + this.getVariableDeclarations() + ", terminatedWithSemicolon=" + this.isTerminatedWithSemicolon() + ")";
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Resource withId(final UUID id) {
                return this.id == id ? this : new J.Try.Resource(id, this.prefix, this.markers, this.variableDeclarations, this.terminatedWithSemicolon);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Resource withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.Try.Resource(this.id, prefix, this.markers, this.variableDeclarations, this.terminatedWithSemicolon);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Resource withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.Try.Resource(this.id, this.prefix, markers, this.variableDeclarations, this.terminatedWithSemicolon);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Resource withVariableDeclarations(final TypedTree variableDeclarations) {
                return this.variableDeclarations == variableDeclarations ? this : new J.Try.Resource(this.id, this.prefix, this.markers, variableDeclarations, this.terminatedWithSemicolon);
            }

            /**
             * Only honored on the last resource in a collection of resources.
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Resource withTerminatedWithSemicolon(final boolean terminatedWithSemicolon) {
                return this.terminatedWithSemicolon == terminatedWithSemicolon ? this : new J.Try.Resource(this.id, this.prefix, this.markers, this.variableDeclarations, terminatedWithSemicolon);
            }
        }


        public static final class Catch implements J {
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final ControlParentheses<VariableDeclarations> parameter;
            private final Block body;

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitCatch(this, p);
            }

            @Override
            public String toString() {
                return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.Try.Catch)) return false;
                final J.Try.Catch other = (J.Try.Catch) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Catch(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<VariableDeclarations> parameter, final Block body) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.parameter = parameter;
                this.body = body;
            }

            public UUID getId() {
                return this.id;
            }

            public Space getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public ControlParentheses<VariableDeclarations> getParameter() {
                return this.parameter;
            }

            public Block getBody() {
                return this.body;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Catch withId(final UUID id) {
                return this.id == id ? this : new J.Try.Catch(id, this.prefix, this.markers, this.parameter, this.body);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Catch withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.Try.Catch(this.id, prefix, this.markers, this.parameter, this.body);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Catch withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.Try.Catch(this.id, this.prefix, markers, this.parameter, this.body);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Catch withParameter(final ControlParentheses<VariableDeclarations> parameter) {
                return this.parameter == parameter ? this : new J.Try.Catch(this.id, this.prefix, this.markers, parameter, this.body);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Try.Catch withBody(final Block body) {
                return this.body == body ? this : new J.Try.Catch(this.id, this.prefix, this.markers, this.parameter, body);
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final Try t;

            @Nullable
            public JContainer<Resource> getResources() {
                return t.resources;
            }

            public Try withResources(@Nullable JContainer<Resource> resources) {
                return t.resources == resources ? t : new Try(t.id, t.prefix, t.markers, resources, t.body, t.catches, t.finallie);
            }

            @Nullable
            public JLeftPadded<Block> getFinally() {
                return t.finallie;
            }

            public Try withFinally(@Nullable JLeftPadded<Block> finallie) {
                return t.finallie == finallie ? t : new Try(t.id, t.prefix, t.markers, t.resources, t.body, t.catches, finallie);
            }

            public Padding(final Try t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Try(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", resources=" + this.getResources() + ", body=" + this.getBody() + ", catches=" + this.getCatches() + ", finallie=" + this.finallie + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Try)) return false;
            final J.Try other = (J.Try) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Try(final UUID id, final Space prefix, final Markers markers, @Nullable final JContainer<Resource> resources, final Block body, final List<Catch> catches, @Nullable final JLeftPadded<Block> finallie) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.resources = resources;
            this.body = body;
            this.catches = catches;
            this.finallie = finallie;
        }

        private Try(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, @Nullable final JContainer<Resource> resources, final Block body, final List<Catch> catches, @Nullable final JLeftPadded<Block> finallie) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.resources = resources;
            this.body = body;
            this.catches = catches;
            this.finallie = finallie;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Try withId(final UUID id) {
            return this.id == id ? this : new J.Try(this.padding, id, this.prefix, this.markers, this.resources, this.body, this.catches, this.finallie);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Try withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Try(this.padding, this.id, prefix, this.markers, this.resources, this.body, this.catches, this.finallie);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Try withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Try(this.padding, this.id, this.prefix, markers, this.resources, this.body, this.catches, this.finallie);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Try withBody(final Block body) {
            return this.body == body ? this : new J.Try(this.padding, this.id, this.prefix, this.markers, this.resources, body, this.catches, this.finallie);
        }

        public Block getBody() {
            return this.body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Try withCatches(final List<Catch> catches) {
            return this.catches == catches ? this : new J.Try(this.padding, this.id, this.prefix, this.markers, this.resources, this.body, catches, this.finallie);
        }

        public List<Catch> getCatches() {
            return this.catches;
        }
    }


    final class TypeCast implements J, Expression, TypedTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<TypeTree> clazz;
        private final Expression expression;

        @Override
        public JavaType getType() {
            return clazz.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypeCast withType(@Nullable JavaType type) {
            return withClazz(clazz.withType(type));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTypeCast(this, p);
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return expression.getSideEffects();
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.TypeCast)) return false;
            final J.TypeCast other = (J.TypeCast) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public TypeCast(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<TypeTree> clazz, final Expression expression) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.clazz = clazz;
            this.expression = expression;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public ControlParentheses<TypeTree> getClazz() {
            return this.clazz;
        }

        public Expression getExpression() {
            return this.expression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeCast withId(final UUID id) {
            return this.id == id ? this : new J.TypeCast(id, this.prefix, this.markers, this.clazz, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeCast withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.TypeCast(this.id, prefix, this.markers, this.clazz, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeCast withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.TypeCast(this.id, this.prefix, markers, this.clazz, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeCast withClazz(final ControlParentheses<TypeTree> clazz) {
            return this.clazz == clazz ? this : new J.TypeCast(this.id, this.prefix, this.markers, clazz, this.expression);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeCast withExpression(final Expression expression) {
            return this.expression == expression ? this : new J.TypeCast(this.id, this.prefix, this.markers, this.clazz, expression);
        }
    }


    final class TypeParameter implements J {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<Annotation> annotations;
        private final List<J.Modifier> modifiers;
        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        private final Expression name;
        @Nullable
        private final JContainer<TypeTree> bounds;

        @Nullable
        public List<TypeTree> getBounds() {
            return bounds == null ? null : bounds.getElements();
        }

        public TypeParameter withBounds(@Nullable List<TypeTree> bounds) {
            return getPadding().withBounds(JContainer.withElementsNullable(this.bounds, bounds));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitTypeParameter(this, p);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final TypeParameter t;

            @Nullable
            public JContainer<TypeTree> getBounds() {
                return t.bounds;
            }

            public TypeParameter withBounds(@Nullable JContainer<TypeTree> bounds) {
                return t.bounds == bounds ? t : new TypeParameter(t.id, t.prefix, t.markers, t.annotations, t.modifiers, t.name, bounds);
            }

            public Padding(final TypeParameter t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.TypeParameter(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", annotations=" + this.getAnnotations() + ", modifiers=" + this.getModifiers() + ", name=" + this.getName() + ", bounds=" + this.getBounds() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.TypeParameter)) return false;
            final J.TypeParameter other = (J.TypeParameter) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public TypeParameter(final UUID id, final Space prefix, final Markers markers, final List<Annotation> annotations, final List<J.Modifier> modifiers, final Expression name, @Nullable final JContainer<TypeTree> bounds) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.modifiers = modifiers;
            this.name = name;
            this.bounds = bounds;
        }

        private TypeParameter(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<Annotation> annotations, final List<J.Modifier> modifiers, final Expression name, @Nullable final JContainer<TypeTree> bounds) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.modifiers = modifiers;
            this.name = name;
            this.bounds = bounds;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withId(final UUID id) {
            return this.id == id ? this : new J.TypeParameter(this.padding, id, this.prefix, this.markers, this.annotations, this.modifiers, this.name, this.bounds);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.TypeParameter(this.padding, this.id, prefix, this.markers, this.annotations, this.modifiers, this.name, this.bounds);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.TypeParameter(this.padding, this.id, this.prefix, markers, this.annotations, this.modifiers, this.name, this.bounds);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withAnnotations(final List<Annotation> annotations) {
            return this.annotations == annotations ? this : new J.TypeParameter(this.padding, this.id, this.prefix, this.markers, annotations, this.modifiers, this.name, this.bounds);
        }

        public List<Annotation> getAnnotations() {
            return this.annotations;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withModifiers(final List<J.Modifier> modifiers) {
            return this.modifiers == modifiers ? this : new J.TypeParameter(this.padding, this.id, this.prefix, this.markers, this.annotations, modifiers, this.name, this.bounds);
        }

        public List<J.Modifier> getModifiers() {
            return this.modifiers;
        }

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameter withName(final Expression name) {
            return this.name == name ? this : new J.TypeParameter(this.padding, this.id, this.prefix, this.markers, this.annotations, this.modifiers, name, this.bounds);
        }

        /**
         * Will be either a {@link TypeTree} or {@link Wildcard}. Wildcards aren't possible in
         * every context where type parameters may be defined (e.g. not possible on new statements).
         */
        public Expression getName() {
            return this.name;
        }
    }


    final class TypeParameters implements J {
        @Nullable
        private transient WeakReference<TypeParameters.Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<Annotation> annotations;
        private final List<JRightPadded<TypeParameter>> typeParameters;

        public List<TypeParameter> getTypeParameters() {
            return JRightPadded.getElements(typeParameters);
        }

        public TypeParameters withTypeParameters(List<TypeParameter> typeParameters) {
            return getPadding().withTypeParameters(JRightPadded.withElements(this.typeParameters, typeParameters));
        }

        public TypeParameters.Padding getPadding() {
            TypeParameters.Padding p;
            if (this.padding == null) {
                p = new TypeParameters.Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new TypeParameters.Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final TypeParameters t;

            public List<JRightPadded<TypeParameter>> getTypeParameters() {
                return t.typeParameters;
            }

            public TypeParameters withTypeParameters(List<JRightPadded<TypeParameter>> typeParameters) {
                return t.typeParameters == typeParameters ? t : new TypeParameters(t.id, t.prefix, t.markers, t.annotations, typeParameters);
            }

            public Padding(final TypeParameters t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.TypeParameters)) return false;
            final J.TypeParameters other = (J.TypeParameters) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public TypeParameters(final UUID id, final Space prefix, final Markers markers, final List<Annotation> annotations, final List<JRightPadded<TypeParameter>> typeParameters) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.typeParameters = typeParameters;
        }

        private TypeParameters(@Nullable final WeakReference<TypeParameters.Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<Annotation> annotations, final List<JRightPadded<TypeParameter>> typeParameters) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.annotations = annotations;
            this.typeParameters = typeParameters;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameters withId(final UUID id) {
            return this.id == id ? this : new J.TypeParameters(this.padding, id, this.prefix, this.markers, this.annotations, this.typeParameters);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameters withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.TypeParameters(this.padding, this.id, prefix, this.markers, this.annotations, this.typeParameters);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameters withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.TypeParameters(this.padding, this.id, this.prefix, markers, this.annotations, this.typeParameters);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.TypeParameters withAnnotations(final List<Annotation> annotations) {
            return this.annotations == annotations ? this : new J.TypeParameters(this.padding, this.id, this.prefix, this.markers, annotations, this.typeParameters);
        }

        public List<Annotation> getAnnotations() {
            return this.annotations;
        }
    }


    final class Unary implements J, Statement, Expression, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final JLeftPadded<Type> operator;

        public Type getOperator() {
            return operator.getElement();
        }

        public Unary withOperator(Type operator) {
            return getPadding().withOperator(this.operator.withElement(operator));
        }

        private final Expression expression;
        @Nullable
        private final JavaType type;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitUnary(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Unary(this);
        }

        @Override
        @Transient
        public List<J> getSideEffects() {
            return getOperator().isModifying() ? singletonList(this) : expression.getSideEffects();
        }


        public enum Type {
            PreIncrement, PreDecrement, PostIncrement, PostDecrement, Positive, Negative, Complement, Not;

            public boolean isModifying() {
                switch (this) {
                case PreIncrement: 
                case PreDecrement: 
                case PostIncrement: 
                case PostDecrement: 
                    return true;
                default: 
                    return false;
                }
            }
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Unary t;

            public JLeftPadded<Type> getOperator() {
                return t.operator;
            }

            public Unary withOperator(JLeftPadded<Type> operator) {
                return t.operator == operator ? t : new Unary(t.id, t.prefix, t.markers, operator, t.expression, t.type);
            }

            public Padding(final Unary t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Unary)) return false;
            final J.Unary other = (J.Unary) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Unary(final UUID id, final Space prefix, final Markers markers, final JLeftPadded<Type> operator, final Expression expression, @Nullable final JavaType type) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.operator = operator;
            this.expression = expression;
            this.type = type;
        }

        private Unary(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final JLeftPadded<Type> operator, final Expression expression, @Nullable final JavaType type) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.operator = operator;
            this.expression = expression;
            this.type = type;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unary withId(final UUID id) {
            return this.id == id ? this : new J.Unary(this.padding, id, this.prefix, this.markers, this.operator, this.expression, this.type);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unary withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Unary(this.padding, this.id, prefix, this.markers, this.operator, this.expression, this.type);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unary withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Unary(this.padding, this.id, this.prefix, markers, this.operator, this.expression, this.type);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unary withExpression(final Expression expression) {
            return this.expression == expression ? this : new J.Unary(this.padding, this.id, this.prefix, this.markers, this.operator, expression, this.type);
        }

        public Expression getExpression() {
            return this.expression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unary withType(@Nullable final JavaType type) {
            return this.type == type ? this : new J.Unary(this.padding, this.id, this.prefix, this.markers, this.operator, this.expression, type);
        }

        @Nullable
        public JavaType getType() {
            return this.type;
        }
    }


    final class VariableDeclarations implements J, Statement, TypedTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final List<Annotation> leadingAnnotations;
        private final List<Modifier> modifiers;
        @Nullable
        private final TypeTree typeExpression;
        @Nullable
        private final Space varargs;
        // For backwards compatibility.
        /**
         * @deprecated Use {@link ArrayType} instead.
         */
        @SuppressWarnings("DeprecatedIsStillUsed")
        @Deprecated
        private final List<JLeftPadded<Space>> dimensionsBeforeName;
        private final List<JRightPadded<NamedVariable>> variables;

        public List<NamedVariable> getVariables() {
            return JRightPadded.getElements(variables);
        }

        public VariableDeclarations withVariables(List<NamedVariable> vars) {
            return getPadding().withVariables(JRightPadded.withElements(this.variables, vars));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitVariableDeclarations(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.VariableDeclarations getCoordinates() {
            return new CoordinateBuilder.VariableDeclarations(this);
        }

        // gather annotations from everywhere they may occur
        /**
         * @deprecated Use {@link org.openrewrite.java.service.AnnotationService#getAllAnnotations(J)} instead.
         */
        @Deprecated
        public List<J.Annotation> getAllAnnotations() {
            List<Annotation> allAnnotations = new ArrayList<>(leadingAnnotations);
            for (J.Modifier modifier : modifiers) {
                allAnnotations.addAll(modifier.getAnnotations());
            }
            if (typeExpression != null && typeExpression instanceof J.AnnotatedType) {
                allAnnotations.addAll(((J.AnnotatedType) typeExpression).getAnnotations());
            }
            return allAnnotations;
        }

        @Nullable
        public JavaType.FullyQualified getTypeAsFullyQualified() {
            return typeExpression == null ? null : TypeUtils.asFullyQualified(typeExpression.getType());
        }

        @Nullable
        @Override
        public JavaType getType() {
            return typeExpression == null ? null : typeExpression.getType();
        }

        @SuppressWarnings("unchecked")
        @Override
        public VariableDeclarations withType(@Nullable JavaType type) {
            return typeExpression == null ? this : withTypeExpression(typeExpression.withType(type));
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static final class NamedVariable implements J, NameTree {
            @Nullable
            private transient WeakReference<Padding> padding;
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final Identifier name;
            private final List<JLeftPadded<Space>> dimensionsAfterName;
            @Nullable
            private final JLeftPadded<Expression> initializer;

            @Nullable
            public Expression getInitializer() {
                return initializer == null ? null : initializer.getElement();
            }

            public NamedVariable withInitializer(@Nullable Expression initializer) {
                if (initializer == null) {
                    return this.initializer == null ? this : new NamedVariable(id, prefix, markers, name, dimensionsAfterName, null, variableType);
                }
                return getPadding().withInitializer(JLeftPadded.withElement(this.initializer, initializer));
            }

            @Nullable
            private final JavaType.Variable variableType;

            public JavaType getType() {
                return variableType != null ? variableType.getType() : null;
            }

            @SuppressWarnings("unchecked")
            @Override
            public NamedVariable withType(@Nullable JavaType type) {
                return variableType != null ? withVariableType(variableType.withType(type)) : this;
            }

            public String getSimpleName() {
                return name.getSimpleName();
            }

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitVariable(this, p);
            }

            public Cursor getDeclaringScope(Cursor cursor) {
                return cursor.dropParentUntil(it -> it instanceof J.Block || it instanceof J.Lambda || it instanceof J.MethodDeclaration || it == Cursor.ROOT_VALUE);
            }

            public boolean isField(Cursor cursor) {
                Cursor declaringScope = getDeclaringScope(cursor);
                return declaringScope.getValue() instanceof J.Block && declaringScope.getParentTreeCursor().getValue() instanceof J.ClassDeclaration;
            }

            public Padding getPadding() {
                Padding p;
                if (this.padding == null) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                } else {
                    p = this.padding.get();
                    if (p == null || p.t != this) {
                        p = new Padding(this);
                        this.padding = new WeakReference<>(p);
                    }
                }
                return p;
            }

            @Override
            public String toString() {
                return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
            }


            public static class Padding {
                private final NamedVariable t;

                @Nullable
                public JLeftPadded<Expression> getInitializer() {
                    return t.initializer;
                }

                public NamedVariable withInitializer(@Nullable JLeftPadded<Expression> initializer) {
                    return t.initializer == initializer ? t : new NamedVariable(t.id, t.prefix, t.markers, t.name, t.dimensionsAfterName, initializer, t.variableType);
                }

                public Padding(final NamedVariable t) {
                    this.t = t;
                }
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.VariableDeclarations.NamedVariable)) return false;
                final J.VariableDeclarations.NamedVariable other = (J.VariableDeclarations.NamedVariable) o;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public NamedVariable(final UUID id, final Space prefix, final Markers markers, final Identifier name, final List<JLeftPadded<Space>> dimensionsAfterName, @Nullable final JLeftPadded<Expression> initializer, @Nullable final JavaType.Variable variableType) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.name = name;
                this.dimensionsAfterName = dimensionsAfterName;
                this.initializer = initializer;
                this.variableType = variableType;
            }

            private NamedVariable(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final Identifier name, final List<JLeftPadded<Space>> dimensionsAfterName, @Nullable final JLeftPadded<Expression> initializer, @Nullable final JavaType.Variable variableType) {
                this.padding = padding;
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.name = name;
                this.dimensionsAfterName = dimensionsAfterName;
                this.initializer = initializer;
                this.variableType = variableType;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withId(final UUID id) {
                return this.id == id ? this : new J.VariableDeclarations.NamedVariable(this.padding, id, this.prefix, this.markers, this.name, this.dimensionsAfterName, this.initializer, this.variableType);
            }

            public UUID getId() {
                return this.id;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.VariableDeclarations.NamedVariable(this.padding, this.id, prefix, this.markers, this.name, this.dimensionsAfterName, this.initializer, this.variableType);
            }

            public Space getPrefix() {
                return this.prefix;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.VariableDeclarations.NamedVariable(this.padding, this.id, this.prefix, markers, this.name, this.dimensionsAfterName, this.initializer, this.variableType);
            }

            public Markers getMarkers() {
                return this.markers;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withName(final Identifier name) {
                return this.name == name ? this : new J.VariableDeclarations.NamedVariable(this.padding, this.id, this.prefix, this.markers, name, this.dimensionsAfterName, this.initializer, this.variableType);
            }

            public Identifier getName() {
                return this.name;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withDimensionsAfterName(final List<JLeftPadded<Space>> dimensionsAfterName) {
                return this.dimensionsAfterName == dimensionsAfterName ? this : new J.VariableDeclarations.NamedVariable(this.padding, this.id, this.prefix, this.markers, this.name, dimensionsAfterName, this.initializer, this.variableType);
            }

            public List<JLeftPadded<Space>> getDimensionsAfterName() {
                return this.dimensionsAfterName;
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.VariableDeclarations.NamedVariable withVariableType(@Nullable final JavaType.Variable variableType) {
                return this.variableType == variableType ? this : new J.VariableDeclarations.NamedVariable(this.padding, this.id, this.prefix, this.markers, this.name, this.dimensionsAfterName, this.initializer, variableType);
            }

            @Nullable
            public JavaType.Variable getVariableType() {
                return this.variableType;
            }
        }

        public boolean hasModifier(Modifier.Type modifier) {
            return Modifier.hasModifier(getModifiers(), modifier);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final VariableDeclarations t;

            public List<JRightPadded<NamedVariable>> getVariables() {
                return t.variables;
            }

            public VariableDeclarations withVariables(List<JRightPadded<NamedVariable>> variables) {
                return t.variables == variables ? t : new VariableDeclarations(t.id, t.prefix, t.markers, t.leadingAnnotations, t.modifiers, t.typeExpression, t.varargs, t.dimensionsBeforeName, variables);
            }

            public Padding(final VariableDeclarations t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.VariableDeclarations)) return false;
            final J.VariableDeclarations other = (J.VariableDeclarations) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public VariableDeclarations(final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, @Nullable final TypeTree typeExpression, @Nullable final Space varargs, final List<JLeftPadded<Space>> dimensionsBeforeName, final List<JRightPadded<NamedVariable>> variables) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.typeExpression = typeExpression;
            this.varargs = varargs;
            this.dimensionsBeforeName = dimensionsBeforeName;
            this.variables = variables;
        }

        private VariableDeclarations(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final List<Annotation> leadingAnnotations, final List<Modifier> modifiers, @Nullable final TypeTree typeExpression, @Nullable final Space varargs, final List<JLeftPadded<Space>> dimensionsBeforeName, final List<JRightPadded<NamedVariable>> variables) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.leadingAnnotations = leadingAnnotations;
            this.modifiers = modifiers;
            this.typeExpression = typeExpression;
            this.varargs = varargs;
            this.dimensionsBeforeName = dimensionsBeforeName;
            this.variables = variables;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withId(final UUID id) {
            return this.id == id ? this : new J.VariableDeclarations(this.padding, id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.VariableDeclarations(this.padding, this.id, prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, markers, this.leadingAnnotations, this.modifiers, this.typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withLeadingAnnotations(final List<Annotation> leadingAnnotations) {
            return this.leadingAnnotations == leadingAnnotations ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, this.markers, leadingAnnotations, this.modifiers, this.typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        public List<Annotation> getLeadingAnnotations() {
            return this.leadingAnnotations;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withModifiers(final List<Modifier> modifiers) {
            return this.modifiers == modifiers ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, this.markers, this.leadingAnnotations, modifiers, this.typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        public List<Modifier> getModifiers() {
            return this.modifiers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withTypeExpression(@Nullable final TypeTree typeExpression) {
            return this.typeExpression == typeExpression ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, typeExpression, this.varargs, this.dimensionsBeforeName, this.variables);
        }

        @Nullable
        public TypeTree getTypeExpression() {
            return this.typeExpression;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.VariableDeclarations withVarargs(@Nullable final Space varargs) {
            return this.varargs == varargs ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeExpression, varargs, this.dimensionsBeforeName, this.variables);
        }

        @Nullable
        public Space getVarargs() {
            return this.varargs;
        }

        /**
         * @deprecated Use {@link ArrayType} instead.
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        @Deprecated
        public J.VariableDeclarations withDimensionsBeforeName(final List<JLeftPadded<Space>> dimensionsBeforeName) {
            return this.dimensionsBeforeName == dimensionsBeforeName ? this : new J.VariableDeclarations(this.padding, this.id, this.prefix, this.markers, this.leadingAnnotations, this.modifiers, this.typeExpression, this.varargs, dimensionsBeforeName, this.variables);
        }

        /**
         * @deprecated Use {@link ArrayType} instead.
         */
        @Deprecated
        public List<JLeftPadded<Space>> getDimensionsBeforeName() {
            return this.dimensionsBeforeName;
        }
    }


    final class WhileLoop implements J, Loop {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final ControlParentheses<Expression> condition;
        private final JRightPadded<Statement> body;

        public Statement getBody() {
            return body.getElement();
        }

        @SuppressWarnings("unchecked")
        public WhileLoop withBody(Statement body) {
            return getPadding().withBody(this.body.withElement(body));
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitWhileLoop(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }


        public static class Padding {
            private final WhileLoop t;

            public JRightPadded<Statement> getBody() {
                return t.body;
            }

            public WhileLoop withBody(JRightPadded<Statement> body) {
                return t.body == body ? t : new WhileLoop(t.id, t.prefix, t.markers, t.condition, body);
            }

            public Padding(final WhileLoop t) {
                this.t = t;
            }
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.WhileLoop(padding=" + this.getPadding() + ", id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", condition=" + this.getCondition() + ", body=" + this.getBody() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.WhileLoop)) return false;
            final J.WhileLoop other = (J.WhileLoop) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public WhileLoop(final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> condition, final JRightPadded<Statement> body) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.condition = condition;
            this.body = body;
        }

        private WhileLoop(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, final ControlParentheses<Expression> condition, final JRightPadded<Statement> body) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.condition = condition;
            this.body = body;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.WhileLoop withId(final UUID id) {
            return this.id == id ? this : new J.WhileLoop(this.padding, id, this.prefix, this.markers, this.condition, this.body);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.WhileLoop withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.WhileLoop(this.padding, this.id, prefix, this.markers, this.condition, this.body);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.WhileLoop withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.WhileLoop(this.padding, this.id, this.prefix, markers, this.condition, this.body);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.WhileLoop withCondition(final ControlParentheses<Expression> condition) {
            return this.condition == condition ? this : new J.WhileLoop(this.padding, this.id, this.prefix, this.markers, condition, this.body);
        }

        public ControlParentheses<Expression> getCondition() {
            return this.condition;
        }
    }


    final class Wildcard implements J, Expression, TypeTree {
        @Nullable
        private transient WeakReference<Padding> padding;
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        @Nullable
        private final JLeftPadded<Bound> bound;

        @Nullable
        public Bound getBound() {
            return bound == null ? null : bound.getElement();
        }

        public Wildcard withBound(@Nullable Bound bound) {
            return getPadding().withBound(JLeftPadded.withElement(this.bound, bound));
        }

        @Nullable
        private final NameTree boundedType;

        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Wildcard withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitWildcard(this, p);
        }

        @Override
        @Transient
        public CoordinateBuilder.Expression getCoordinates() {
            return new CoordinateBuilder.Expression(this);
        }


        public enum Bound {
            Extends, Super;
        }

        public Padding getPadding() {
            Padding p;
            if (this.padding == null) {
                p = new Padding(this);
                this.padding = new WeakReference<>(p);
            } else {
                p = this.padding.get();
                if (p == null || p.t != this) {
                    p = new Padding(this);
                    this.padding = new WeakReference<>(p);
                }
            }
            return p;
        }

        @Override
        public String toString() {
            return withPrefix(Space.EMPTY).printTrimmed(new JavaPrinter<>());
        }


        public static class Padding {
            private final Wildcard t;

            @Nullable
            public JLeftPadded<Bound> getBound() {
                return t.bound;
            }

            public Wildcard withBound(@Nullable JLeftPadded<Bound> bound) {
                return t.bound == bound ? t : new Wildcard(t.id, t.prefix, t.markers, bound, t.boundedType);
            }

            public Padding(final Wildcard t) {
                this.t = t;
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Wildcard)) return false;
            final J.Wildcard other = (J.Wildcard) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Wildcard(final UUID id, final Space prefix, final Markers markers, @Nullable final JLeftPadded<Bound> bound, @Nullable final NameTree boundedType) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.bound = bound;
            this.boundedType = boundedType;
        }

        private Wildcard(@Nullable final WeakReference<Padding> padding, final UUID id, final Space prefix, final Markers markers, @Nullable final JLeftPadded<Bound> bound, @Nullable final NameTree boundedType) {
            this.padding = padding;
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.bound = bound;
            this.boundedType = boundedType;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Wildcard withId(final UUID id) {
            return this.id == id ? this : new J.Wildcard(this.padding, id, this.prefix, this.markers, this.bound, this.boundedType);
        }

        public UUID getId() {
            return this.id;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Wildcard withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Wildcard(this.padding, this.id, prefix, this.markers, this.bound, this.boundedType);
        }

        public Space getPrefix() {
            return this.prefix;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Wildcard withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Wildcard(this.padding, this.id, this.prefix, markers, this.bound, this.boundedType);
        }

        public Markers getMarkers() {
            return this.markers;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Wildcard withBoundedType(@Nullable final NameTree boundedType) {
            return this.boundedType == boundedType ? this : new J.Wildcard(this.padding, this.id, this.prefix, this.markers, this.bound, boundedType);
        }

        @Nullable
        public NameTree getBoundedType() {
            return this.boundedType;
        }
    }


    final class Yield implements J, Statement {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final boolean implicit;
        private final Expression value;

        @Override
        public CoordinateBuilder.Yield getCoordinates() {
            return new CoordinateBuilder.Yield(this);
        }

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitYield(this, p);
        }

        public Yield(final UUID id, final Space prefix, final Markers markers, final boolean implicit, final Expression value) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.implicit = implicit;
            this.value = value;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public boolean isImplicit() {
            return this.implicit;
        }

        public Expression getValue() {
            return this.value;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Yield(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", implicit=" + this.isImplicit() + ", value=" + this.getValue() + ")";
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Yield)) return false;
            final J.Yield other = (J.Yield) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Yield withId(final UUID id) {
            return this.id == id ? this : new J.Yield(id, this.prefix, this.markers, this.implicit, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Yield withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Yield(this.id, prefix, this.markers, this.implicit, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Yield withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Yield(this.id, this.prefix, markers, this.implicit, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Yield withImplicit(final boolean implicit) {
            return this.implicit == implicit ? this : new J.Yield(this.id, this.prefix, this.markers, implicit, this.value);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Yield withValue(final Expression value) {
            return this.value == value ? this : new J.Yield(this.id, this.prefix, this.markers, this.implicit, value);
        }
    }


    /**
     * A tree node that represents an unparsed element.
     */
    final class Unknown implements J, Statement, Expression, TypeTree, TypedTree, NameTree {
        private final UUID id;
        private final Space prefix;
        private final Markers markers;
        private final Source source;

        @Override
        public <P> J acceptJava(JavaVisitor<P> v, P p) {
            return v.visitUnknown(this, p);
        }

        @Nullable
        @Override
        public JavaType getType() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Unknown withType(@Nullable JavaType type) {
            return this;
        }

        @Override
        public CoordinateBuilder.Statement getCoordinates() {
            return new CoordinateBuilder.Statement(this);
        }

        /**
         * This class only exists to clean up the printed results from `SearchResult` markers.
         * Without the marker the comments will print before the LST prefix.
         */
        public static class Source implements J {
            private final UUID id;
            private final Space prefix;
            private final Markers markers;
            private final String text;

            @Override
            public <P> J acceptJava(JavaVisitor<P> v, P p) {
                return v.visitUnknownSource(this, p);
            }

            @Override
            public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
                if (o == this) return true;
                if (!(o instanceof J.Unknown.Source)) return false;
                final J.Unknown.Source other = (J.Unknown.Source) o;
                if (!other.canEqual((Object) this)) return false;
                final Object this$id = this.getId();
                final Object other$id = other.getId();
                if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
                return true;
            }

            protected boolean canEqual(@org.openrewrite.internal.lang.Nullable final Object other) {
                return other instanceof J.Unknown.Source;
            }

            @Override
            public int hashCode() {
                final int PRIME = 59;
                int result = 1;
                final Object $id = this.getId();
                result = result * PRIME + ($id == null ? 43 : $id.hashCode());
                return result;
            }

            public Source(final UUID id, final Space prefix, final Markers markers, final String text) {
                this.id = id;
                this.prefix = prefix;
                this.markers = markers;
                this.text = text;
            }

            public UUID getId() {
                return this.id;
            }

            public Space getPrefix() {
                return this.prefix;
            }

            public Markers getMarkers() {
                return this.markers;
            }

            public String getText() {
                return this.text;
            }

            @org.openrewrite.internal.lang.NonNull
            @Override
            public String toString() {
                return "J.Unknown.Source(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", text=" + this.getText() + ")";
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Unknown.Source withId(final UUID id) {
                return this.id == id ? this : new J.Unknown.Source(id, this.prefix, this.markers, this.text);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Unknown.Source withPrefix(final Space prefix) {
                return this.prefix == prefix ? this : new J.Unknown.Source(this.id, prefix, this.markers, this.text);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Unknown.Source withMarkers(final Markers markers) {
                return this.markers == markers ? this : new J.Unknown.Source(this.id, this.prefix, markers, this.text);
            }

            /**
             * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
             */
            @org.openrewrite.internal.lang.NonNull
            public J.Unknown.Source withText(final String text) {
                return this.text == text ? this : new J.Unknown.Source(this.id, this.prefix, this.markers, text);
            }
        }

        @Override
        public boolean equals(@org.openrewrite.internal.lang.Nullable final Object o) {
            if (o == this) return true;
            if (!(o instanceof J.Unknown)) return false;
            final J.Unknown other = (J.Unknown) o;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            return result;
        }

        public Unknown(final UUID id, final Space prefix, final Markers markers, final Source source) {
            this.id = id;
            this.prefix = prefix;
            this.markers = markers;
            this.source = source;
        }

        public UUID getId() {
            return this.id;
        }

        public Space getPrefix() {
            return this.prefix;
        }

        public Markers getMarkers() {
            return this.markers;
        }

        public Source getSource() {
            return this.source;
        }

        @org.openrewrite.internal.lang.NonNull
        @Override
        public String toString() {
            return "J.Unknown(id=" + this.getId() + ", prefix=" + this.getPrefix() + ", markers=" + this.getMarkers() + ", source=" + this.getSource() + ")";
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unknown withId(final UUID id) {
            return this.id == id ? this : new J.Unknown(id, this.prefix, this.markers, this.source);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unknown withPrefix(final Space prefix) {
            return this.prefix == prefix ? this : new J.Unknown(this.id, prefix, this.markers, this.source);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unknown withMarkers(final Markers markers) {
            return this.markers == markers ? this : new J.Unknown(this.id, this.prefix, markers, this.source);
        }

        /**
         * @return a clone of this object, except with this updated property (returns {@code this} if an identical value is passed).
         */
        @org.openrewrite.internal.lang.NonNull
        public J.Unknown withSource(final Source source) {
            return this.source == source ? this : new J.Unknown(this.id, this.prefix, this.markers, source);
        }
    }
}
