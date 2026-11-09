// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class BackportDagLoaderTest {

    @Test
    void canonicalResourceLoads() {
        assertEquals(12, BackportDagLoader.load("m3-backport-dag.tsv").size());
    }

    @Test
    void invalidAndMissingResourcesFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> BackportDagLoader.load(""));
        assertThrows(IllegalArgumentException.class, () -> BackportDagLoader.load("../escape.tsv"));
        assertThrows(IllegalArgumentException.class, () -> BackportDagLoader.load("missing.tsv"));
        assertThrows(IllegalArgumentException.class, () -> BackportDagLoader.load("bad-header.tsv"));
        assertThrows(IllegalArgumentException.class, () -> BackportDagLoader.load("bad-row.tsv"));
    }
}
