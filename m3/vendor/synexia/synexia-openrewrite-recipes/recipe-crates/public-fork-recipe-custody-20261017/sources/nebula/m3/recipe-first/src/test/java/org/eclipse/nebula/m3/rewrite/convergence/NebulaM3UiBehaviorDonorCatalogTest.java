// SPDX-License-Identifier: EPL-2.0
package org.eclipse.nebula.m3.rewrite.convergence;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class NebulaM3UiBehaviorDonorCatalogTest {
    @Test
    void catalogueIsBehaviorOnlyAndCoversViewportDistillationContracts() {
        assertFalse(NebulaM3UiBehaviorDonorCatalog.sourceCopyAuthority());
        assertFalse(NebulaM3UiBehaviorDonorCatalog.mutationAuthority());
        assertFalse(NebulaM3UiBehaviorDonorCatalog.promotionAuthority());
        assertEquals(4, NebulaM3UiBehaviorDonorCatalog.sources().size());
        assertEquals(64, NebulaM3UiBehaviorDonorCatalog.root().length());

        Set<String> obligations = new HashSet<>();
        NebulaM3UiBehaviorDonorCatalog.sources()
                .forEach(source -> obligations.addAll(source.obligations()));

        for (String required :
                new String[] {
                    "CANVAS_CLIPPED_PAINT",
                    "SCROLLED_COMPOSITE_LOGICAL_ORIGIN",
                    "JLAYEREDPANE_Z_ORDER",
                    "JTABLE_MODEL_RENDERER_EDITOR_HEADER_SORT_FILTER_SELECTION",
                    "JTREE_MODEL_RENDERER_EDITOR_SELECTION",
                    "AFFINE_TRANSFORM",
                    "GC_STATE_SCOPE",
                    "EVENT_DISPATCH_THREAD",
                    "MOUSE_MOTION_WHEEL_EVENTS",
                    "TREE_EXPAND_COLLAPSE_MODEL_SELECTION_EVENTS",
                    "DND_HIT_TESTING",
                    "ACCESSIBILITY"
                }) {
            assertTrue(obligations.contains(required), required);
        }
    }

    @Test
    void catalogueSerializationIsStableAndUnique() {
        Set<String> ids = new HashSet<>();
        Set<String> urls = new HashSet<>();
        NebulaM3UiBehaviorDonorCatalog.sources()
                .forEach(
                        source -> {
                            assertTrue(ids.add(source.id()));
                            assertTrue(urls.add(source.url()));
                            assertTrue(source.url().startsWith("https://www.java2s.com/"));
                        });
        assertEquals(
                NebulaM3UiBehaviorDonorCatalog.tsv(),
                NebulaM3UiBehaviorDonorCatalog.tsv(),
                "catalogue must be deterministic");
    }
}
