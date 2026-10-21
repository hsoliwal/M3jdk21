// SPDX-License-Identifier: Apache-2.0
package com.synexia.indexstring.ast;

import org.junit.jupiter.api.Test;

final class MIndexASTHierarchyTest {
    @Test void verifiedHierarchyAllCollectionFamilies() {
        MIndexASTHierarchyContract.main(new String[] { "2000" });
    }

    @Test void exactIdentifierRoots() throws Exception {
        MIndexASTHierarchyEncodingTest.main(new String[0]);
    }
}
