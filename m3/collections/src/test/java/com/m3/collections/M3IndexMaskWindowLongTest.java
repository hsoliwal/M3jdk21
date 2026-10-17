/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.collections;

import org.junit.jupiter.api.Test;

/** Java-only Maven/JUnit coverage; actual JNI is verified by the dedicated native receipt. */
final class M3IndexMaskWindowLongTest {
    @Test void intAndLongBoundedWindows() {
        M3IndexMaskWindowLongContract.main(new String[]{"--java"});
    }
}
