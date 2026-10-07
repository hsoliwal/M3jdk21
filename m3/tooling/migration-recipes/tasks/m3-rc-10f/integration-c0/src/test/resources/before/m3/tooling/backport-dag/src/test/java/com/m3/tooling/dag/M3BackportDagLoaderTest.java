// SPDX-License-Identifier: Apache-2.0
package com.m3.tooling.dag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

final class M3BackportDagLoaderTest {

    @Test
    void canonicalResourceLoads() {
        assertEquals(12, M3BackportDagLoader.load("m3-backport-dag.tsv").size());
    }

    @Test
    void invalidAndMissingResourcesFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> M3BackportDagLoader.load(""));
        assertThrows(IllegalArgumentException.class, () -> M3BackportDagLoader.load("../escape.tsv"));
        assertThrows(IllegalArgumentException.class, () -> M3BackportDagLoader.load("missing.tsv"));
        assertThrows(IllegalArgumentException.class, () -> M3BackportDagLoader.load("bad-header.tsv"));
        assertThrows(IllegalArgumentException.class, () -> M3BackportDagLoader.load("bad-row.tsv"));
    }
}
