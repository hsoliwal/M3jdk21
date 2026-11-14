// SPDX-License-Identifier: Apache-2.0
// Relocated from com.synexia m3-fast-search/src/test/java/jdk/internal/m3/search/SearchEnginesTest.java (blob dafcef55a636) by M3_SEARCH_RELOCATE v2.
package jdk.m3.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class SearchEnginesTest {
    @Test
    void javaFallbackIsAlwaysAvailableWhenNativeIsDisabled() {
        System.clearProperty("m3.native.enabled");
        SearchEngine engine = SearchEngines.best();
        assertEquals("JAVA_ADAPTIVE", engine.name());
        assertFalse(new JniSearchEngine().available());
    }
}
