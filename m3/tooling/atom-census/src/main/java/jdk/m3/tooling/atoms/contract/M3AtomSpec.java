/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional source anchor for an explicitly maintained atom.
 *
 * <p>The repository-wide census remains authoritative because Java annotations
 * cannot address initializer blocks, lambdas, native sources, build rules or
 * unmodified upstream files. This annotation supplies precise metadata where a
 * source owner chooses to carry it without altering runtime class files.</p>
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD,
        ElementType.PACKAGE, ElementType.MODULE, ElementType.RECORD_COMPONENT, ElementType.ANNOTATION_TYPE})
public @interface M3AtomSpec {
    /**
     * Returns the stable atom identifier.
     *
     * @return stable atom identifier
     */
    String id();

    /**
     * Returns the atom kind.
     *
     * @return closed atom kind
     */
    M3AtomKind kind();

    /**
     * Returns the stable owner or capability identifier.
     *
     * @return owner identifier
     */
    String owner();

    /**
     * Returns the contract document or machine-contract reference.
     *
     * @return contract reference
     */
    String contract();

    /**
     * Returns the closed pattern-catalogue identifier.
     *
     * @return pattern identifier
     */
    String pattern();

    /**
     * Returns whether the public or externally observable contract remains locked.
     *
     * @return {@code true} unless a separately evidenced change explicitly unlocks the contract
     */
    boolean publicContractLocked() default true;
}
