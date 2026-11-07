/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import org.junit.jupiter.api.Test;

/** Maven/JUnit binding for the dependency-free repository file census proof. */
final class M3RepositoryFileCensusTest {
    @Test
    void repositoryFileCensusIsCommitSealedAndDeterministic() throws Exception {
        M3RepositoryFileCensusSelfTest.run();
    }
}
