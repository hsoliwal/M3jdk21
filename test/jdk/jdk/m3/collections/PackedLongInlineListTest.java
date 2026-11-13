// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.InlineOwnerBehaviorProbe;

//FLLDCIM: TMOP - JUnit 5 wrapper template of InlineOwnerBehaviorProbe.main; recipe make/m3/collections/qualification/census2-t2-20261106/relocate.py v6
class PackedLongInlineListTest {
    @Test void proofMainPasses() throws Exception {
        InlineOwnerBehaviorProbe.main(new String[] {"650"});
    }
}
