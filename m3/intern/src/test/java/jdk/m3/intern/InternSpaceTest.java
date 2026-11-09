// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.intern;

import org.junit.jupiter.api.Test;

//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/fd9a233680b43197eb8c5745a729c342ca56c4e2/synexia-common/src/test/java/com/synexia/common/intern/M3InternSpaceTest.java blob 924c11a32bb6; recipe m3/intern/recipe/intern-space-20261106/relocate.py v4
final class InternSpaceTest {
    @Test
    void immutableInternSpaceCompatibilityAndArchiveReplay() throws Exception {
        InternSpaceProof.run();
    }
}
