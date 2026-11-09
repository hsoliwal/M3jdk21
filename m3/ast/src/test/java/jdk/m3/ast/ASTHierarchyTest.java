// SPDX-License-Identifier: Apache-2.0
package jdk.m3.ast;

import org.junit.jupiter.api.Test;

final class ASTHierarchyTest {
    @Test void verifiedHierarchyAllCollectionFamilies() {
        ASTHierarchyContract.main(new String[] { "2000" });
    }

    @Test void exactIdentifierRoots() throws Exception {
        ASTHierarchyEncodingTest.main(new String[0]);
    }
}
