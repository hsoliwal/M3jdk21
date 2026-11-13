// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.FilteredListMutationProof;

//FLLDCIM: RELOCATE - donor blob e024c1606276 at d5ad5090d87e; recipe make/m3/collections/qualification/census2-c2-20261106/relocate.py v1
final class FilteredListMutationTest {
    @Test void cursorMutationContract() {
        FilteredListMutationProof.main(new String[0]);
    }
}
