/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary M3 swing-viewport leaf 1: JUnit suite of the arithmetic VariableHeightLayoutCache (seeded
 *          differential against the stock cache plus targeted edge cases); JaCoCo line and branch target 99%.
 * @modules java.desktop/sun.swing.tree
 * @build M3TreeLayoutCacheDifferential M3JTreeViewportDifferential
 * @run junit/othervm -Djava.awt.headless=true -XX:-OmitStackTraceInFastThrow M3VariableHeightLayoutCacheTest
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** JUnit entry points; the edge cases are compared against the stock cache through {@link Pair}. */
public class M3VariableHeightLayoutCacheTest {

    @Test
    void layoutCacheDifferential() throws Exception {
        String summary = M3TreeLayoutCacheDifferential.runSeeds(1, 80, 300);
        assertTrue(summary.contains(" failed 0 "), summary);
    }

    @Test
    void jtreeDifferential() throws Exception {
        String summary = M3JTreeViewportDifferential.runSeeds(1, 12, 150);
        assertTrue(summary.contains(" failed 0 "), summary);
    }

    @Test
    void enabledFollowsTheSystemProperty() throws Exception {
        boolean enabled = Class.forName("sun.swing.tree.M3VariableHeightLayoutCache").getField("ENABLED")
                .getBoolean(null);
        assertEquals(Boolean.getBoolean("m3.swing.viewport"), enabled);
    }
}
