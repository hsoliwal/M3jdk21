// SPDX-License-Identifier: Apache-2.0
package com.m3.ast;

import org.junit.jupiter.api.Test;

final class M3ASTHierarchyTest {
    @Test void verifiedHierarchyAllCollectionFamilies() {
        M3ASTHierarchyContract.main(new String[] { "2000" });
    }

    @Test void exactIdentifierRoots() throws Exception {
        M3ASTHierarchyEncodingTest.main(new String[0]);
    }
}
