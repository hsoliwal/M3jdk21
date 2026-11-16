/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Additional functional interfaces integrated into M3JDK 21 java.base.
 *
 * <p>The base shape policy covers zero, one, or two arguments, each a reference or one of the eight
 * Java primitives, with reference, primitive, or void results. Predicates represent boolean
 * results. Operators express equal-domain roles; comparators add ordering obligations. Higher
 * generic arities from three through sixteen provide reference-result functions, predicates, and
 * consumers.
 *
 * <p>Names preserve argument order. {@code ObjIntFunction<T,R>} takes an object then an int; {@code
 * IntObjFunction<T,R>} reverses those positions. Equal primitive pairs use {@code
 * IntBiFunction<R>}; differing pairs use {@code IntLongFunction<R>}. Primitive results use {@code
 * To}, for example {@code IntLongToDoubleFunction}. An index is an int or long argument, not a
 * separate callback hierarchy.
 *
 * <p>The original {@link java.util.function} package remains unchanged. New subpackages avoid
 * introducing competing simple names into existing wildcard imports. Clients that opt into these
 * packages should use explicit imports when simple names overlap.
 *
 * <p>Every interface has one non-generic abstract method and can target a lambda or method
 * reference. No interface implies purity, scheduling, serialization, synchronization, memoization,
 * resource ownership, or callback identity. Primitive positions avoid boxing in the functional
 * method signature; this is not a guarantee that a lambda never allocates.
 *
 * <p>Every checked interface declares an interface-level {@code E extends Throwable}. These
 * interfaces do not extend nonthrowing JDK counterparts. Calls and composition preserve the
 * original throwable, including interruption and Error objects. No implicit unchecked adapter or
 * exception wrapper is supplied. Combining distinct exception families requires selecting a common
 * declared throwable supertype.
 *
 * @since M3JDK 21
 */
package java.util.function.checked;
