/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. This particular file is
 * subject to the "Classpath" exception as provided in the LICENSE file
 * that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package java.lang.reflect;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Utilities for class, type, annotation, and executable metadata.
 *
 * <p>Existing {@link Class}, {@link Type}, {@link AnnotatedElement}, and
 * {@link Executable} objects remain the metadata owners. These operations
 * do not load classes by name, initialize classes, invoke members, suppress
 * access checks, or retain a global class-loader cache. Reflected objects
 * supplied by the caller retain their access flags. Use the original
 * {@link Field}, {@link Method}, and {@link Constructor} APIs to access or
 * invoke members in the caller's own access context.
 *
 * <p>All returned streams are sequential. Null arguments cause
 * {@link NullPointerException}. Metadata-format and resolution exceptions
 * from the underlying reflection APIs are propagated unchanged. Operations
 * on user implementations of reflection interfaces execute those objects'
 * metadata accessor methods in the usual way.
 *
 * @since 21
 */
public final class Reflection {
    private Reflection() {
        throw new AssertionError("no instances");
    }

    /**
     * Returns the supplied class followed by successive superclasses,
     * ending with {@code Object} where applicable. Interfaces and primitive
     * classes produce only themselves. An array class is followed by
     * {@code Object}. The superclass chain is traversed lazily.
     *
     * @param type the first class in the hierarchy
     * @return the class and its superclass chain
     */
    public static Stream<Class<?>> hierarchy(Class<?> type) {
        Objects.requireNonNull(type, "type");
        return Stream.<Class<?>>iterate(type, Objects::nonNull, Class::getSuperclass);
    }

    /**
     * Returns each interface implemented or extended by {@code type} once.
     * The supplied interface itself is excluded. Direct interfaces are
     * discovered in declaration order for the supplied class and then each
     * superclass; their superinterfaces are visited breadth-first in the
     * same order. Class identity, including the defining loader, determines
     * uniqueness. Primitive classes have no interfaces; array classes
     * expose their normal {@code Cloneable} and {@code Serializable}
     * interfaces. This method takes an eager metadata snapshot without
     * initializing classes.
     *
     * @param type the class or interface to inspect
     * @return a stream over the distinct interface snapshot
     */
    public static Stream<Class<?>> interfaces(Class<?> type) {
        Objects.requireNonNull(type, "type");
        ArrayDeque<Class<?>> pending = new ArrayDeque<>();
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            Collections.addAll(pending, current.getInterfaces());
        }
        LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
        while (!pending.isEmpty()) {
            Class<?> next = pending.removeFirst();
            if (result.add(next)) Collections.addAll(pending, next.getInterfaces());
        }
        return result.stream();
    }

    /**
     * Finds a concrete raw class for a class, parameterized type, or generic
     * array whose component has a concrete raw class. Type variables and
     * wildcards return an empty result: their bounds do not establish one
     * exact runtime type. A parameterized type must expose a {@code Class}
     * as its raw type. A recursive generic-array descriptor returns empty.
     * Array classes are obtained through {@link Class#arrayType()}.
     *
     * @param type the reflected type
     * @return the concrete raw class, or empty when it cannot be resolved
     * @throws UnsupportedOperationException if the resolved array component cannot
     *         form an array class, including {@code void} or excess dimensions
     */
    public static Optional<Class<?>> rawType(Type type) {
        Objects.requireNonNull(type, "type");
        Type current = type;
        int dimensions = 0;
        Set<Type> seen = null;
        while (current instanceof GenericArrayType array) {
            if (seen == null) seen = Collections.newSetFromMap(new IdentityHashMap<>());
            if (!seen.add(current)) return Optional.empty();
            if (++dimensions > 255) {
                throw new UnsupportedOperationException("more than 255 array dimensions");
            }
            current = Objects.requireNonNull(array.getGenericComponentType());
        }
        if (current instanceof ParameterizedType parameterized) {
            current = parameterized.getRawType();
        }
        if (!(current instanceof Class<?> raw)) return Optional.empty();
        for (int i = 0; i < dimensions; i++) raw = raw.arrayType();
        return Optional.of(raw);
    }

    /**
     * Returns the immediate component type for an array class or generic
     * array type; all other types yield an empty result. A generic component
     * is retained as a {@code Type}, including unresolved type variables.
     *
     * @param type the reflected type
     * @return the immediate component type, if {@code type} is an array
     */
    public static Optional<Type> componentType(Type type) {
        Objects.requireNonNull(type, "type");
        if (type instanceof Class<?> raw) return Optional.ofNullable(raw.getComponentType());
        if (type instanceof GenericArrayType array) {
            return Optional.of(Objects.requireNonNull(array.getGenericComponentType()));
        }
        return Optional.empty();
    }

    /**
     * Returns actual type arguments in their declared order for a
     * {@link ParameterizedType}; other types yield an empty stream. A raw
     * class does not acquire invented type arguments or bounds.
     *
     * @param type the reflected type
     * @return the actual type arguments
     */
    public static Stream<Type> typeArguments(Type type) {
        Objects.requireNonNull(type, "type");
        return type instanceof ParameterizedType parameterized
                ? Arrays.stream(parameterized.getActualTypeArguments()) : Stream.empty();
    }

    /**
     * Returns the declared upper bounds of a type variable or wildcard,
     * preserving the order and implicit {@code Object} bound of the
     * original reflection API. Other types yield an empty stream.
     *
     * @param type the reflected type
     * @return the upper bounds
     */
    public static Stream<Type> upperBounds(Type type) {
        Objects.requireNonNull(type, "type");
        if (type instanceof TypeVariable<?> variable) return Arrays.stream(variable.getBounds());
        if (type instanceof WildcardType wildcard) return Arrays.stream(wildcard.getUpperBounds());
        return Stream.empty();
    }

    /**
     * Returns the lower bounds of a wildcard in the order supplied by
     * {@link WildcardType#getLowerBounds()}; other types yield an empty stream.
     *
     * @param type the reflected type
     * @return the lower bounds
     */
    public static Stream<Type> lowerBounds(Type type) {
        Objects.requireNonNull(type, "type");
        return type instanceof WildcardType wildcard
                ? Arrays.stream(wildcard.getLowerBounds()) : Stream.empty();
    }

    /**
     * Streams annotations present on an element, including inheritance
     * where {@link AnnotatedElement#getAnnotations()} specifies it. The
     * underlying API's unspecified annotation order remains unspecified.
     * The annotation array is obtained immediately.
     *
     * @param element the annotated element
     * @return the annotations present on the element
     */
    public static Stream<Annotation> annotations(AnnotatedElement element) {
        return Arrays.stream(Objects.requireNonNull(element, "element").getAnnotations());
    }

    /**
     * Streams annotations associated with the element and requested type,
     * using {@link AnnotatedElement#getAnnotationsByType(Class)}. Repeatable
     * containers, inherited annotations, and order retain that API's rules.
     * The annotation array is obtained immediately.
     *
     * @param element the annotated element
     * @param annotationType the annotation class
     * @param <A> the annotation type
     * @return the associated annotations
     */
    public static <A extends Annotation> Stream<A> annotations(
            AnnotatedElement element, Class<A> annotationType) {
        Objects.requireNonNull(element, "element");
        Objects.requireNonNull(annotationType, "annotationType");
        return Arrays.stream(element.getAnnotationsByType(annotationType));
    }

    /**
     * Streams an executable's parameters in declaration order. Names,
     * synthetic and implicit flags, and annotations retain
     * {@link Executable#getParameters()} semantics. The supplied executable
     * is not invoked and its access flags are unchanged.
     *
     * @param executable the method or constructor
     * @return the executable parameters
     */
    public static Stream<Parameter> parameters(Executable executable) {
        return Arrays.stream(Objects.requireNonNull(executable, "executable").getParameters());
    }

    /**
     * Streams generic parameter types in the order supplied by
     * {@link Executable#getGenericParameterTypes()}, including its rules
     * for synthetic or implicit parameters.
     *
     * @param executable the method or constructor
     * @return the generic parameter types
     */
    public static Stream<Type> parameterTypes(Executable executable) {
        return Arrays.stream(Objects.requireNonNull(executable, "executable")
                .getGenericParameterTypes());
    }

    /**
     * Streams generic exception types in declaration order, using
     * {@link Executable#getGenericExceptionTypes()}.
     *
     * @param executable the method or constructor
     * @return the declared generic exception types
     */
    public static Stream<Type> exceptionTypes(Executable executable) {
        return Arrays.stream(Objects.requireNonNull(executable, "executable")
                .getGenericExceptionTypes());
    }
}
