// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.fs;

import org.junit.jupiter.api.Test;

/** Owner-bound JUnit5 contract for original M3FS native-directory columns. */
final class M3DirectoryNameIndexTest {
    @Test void repeatedPrefixesPreserveOriginalNamesAndPhysicalRows() {
        M3DirectoryNameIndexContract.main(new String[0]);
    }
}
