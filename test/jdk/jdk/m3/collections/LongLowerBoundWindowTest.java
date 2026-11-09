// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.LongLowerBoundWindowContract;

final class LongLowerBoundWindowTest {
    @Test void javaAndOptionalNativeLowerBoundWindows() {
        LongLowerBoundWindowContract.contracts();
    }
}
