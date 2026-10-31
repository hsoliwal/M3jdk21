/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/** Stable, language-neutral descriptor emitted by an atomizer. */
public interface M3AtomDescriptor {
    /**
     * Returns the stable capability/atom identity.
     *
     * @return stable capability/atom identity
     */
    String atomId();

    /**
     * Returns the normalized repository-relative owning source path.
     *
     * @return normalized repository-relative owning source path
     */
    String sourcePath();

    /**
     * Returns the closed atom kind.
     *
     * @return closed atom kind
     */
    M3AtomKind kind();

    /**
     * Returns the stable implementation or capability owner.
     *
     * @return stable implementation or capability owner
     */
    String owner();

    /**
     * Returns the language-specific semantic signature.
     *
     * @return language-specific semantic signature
     */
    String signature();

    /**
     * Returns the lowercase SHA-256 of the sealed owning source bytes or slice.
     *
     * @return lowercase SHA-256 of the sealed owning source bytes or slice
     */
    String sourceSha256();

    /**
     * Returns the closed pattern-catalogue identity.
     *
     * @return closed pattern-catalogue identity
     */
    String patternId();

    /**
     * Returns the machine or human contract reference.
     *
     * @return machine or human contract reference
     */
    String contractRef();

    /**
     * Returns whether the public or external contract remains locked.
     *
     * @return whether the public or external contract remains locked
     */
    boolean publicContractLocked();
}
