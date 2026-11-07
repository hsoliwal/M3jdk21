// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.compiler.staticuse;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;

/**
 * JUnit 5 admission of the relocated donor oracle CompilerOwnershipProof and of M3StaticFactsProcessorOracle over the
 * relocated processor (recipe static-use-plan-20261107, leaf 2; M3JDK-authored, test scope). Work directories are
 * created under java.io.tmpdir.
 */
final class M3StaticFactsProcessorTest {
    @Test
    void donorCompilerOwnershipProof() throws Exception {
        CompilerOwnershipProof.main(new String[] {Files.createTempDirectory("m3-compiler-ownership-proof-").toString()});
    }

    @Test
    void processorOracleCorpus() throws Exception {
        M3StaticFactsProcessorOracle.main(new String[] {M3StaticFactsProcessor.class.getName()});
    }
}
