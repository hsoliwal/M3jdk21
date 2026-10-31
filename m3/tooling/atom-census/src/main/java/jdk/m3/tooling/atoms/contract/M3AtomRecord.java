/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.contract;

/**
 * Immutable row from {@code ATOMS.tsv}.
 *
 * @param atomId stable atom identity
 * @param sourcePath normalized repository-relative source path
 * @param kind closed atom kind
 * @param owner implementation or capability owner
 * @param signature language-specific semantic signature
 * @param sourceSha256 lowercase source SHA-256
 * @param patternId closed pattern identity
 * @param contractRef contract reference
 * @param publicContractLocked whether external behavior remains locked
 */
public record M3AtomRecord(
        String atomId,
        String sourcePath,
        M3AtomKind kind,
        String owner,
        String signature,
        String sourceSha256,
        String patternId,
        String contractRef,
        boolean publicContractLocked) implements M3AtomDescriptor {
}
