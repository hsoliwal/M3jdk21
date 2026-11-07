// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import org.junit.jupiter.api.Test;
import jdk.internal.m3.collections.M3LongLowerBoundWindowContract;

final class M3LongLowerBoundWindowTest {
    @Test void javaAndOptionalNativeLowerBoundWindows() {
        M3LongLowerBoundWindowContract.contracts();
    }
}
