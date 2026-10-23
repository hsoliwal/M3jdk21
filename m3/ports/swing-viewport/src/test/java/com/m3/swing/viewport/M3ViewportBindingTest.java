/* SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.HierarchyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** Frozen EDT contracts for the existing binding's listener ownership. */
final class M3ViewportBindingTest {
    private static final class View extends JComponent {
        private static final long serialVersionUID = 1L;
        boolean showing;

        @Override public boolean isShowing() {
            return showing;
        }

        @Override public void computeVisibleRect(Rectangle destination) {
            destination.setBounds(0, 20, 100, 30);
        }
    }

    private static final class Viewport extends JViewport {
        private static final long serialVersionUID = 1L;

        void change() {
            fireStateChanged();
        }
    }

    private static void onEdt(Runnable contract) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            contract.run();
        });
    }

    private static long registrations(JViewport viewport, M3ViewportBinding binding) {
        return Arrays.stream(viewport.getChangeListeners())
                .filter(listener -> listener == binding).count();
    }

    private static void hierarchy(M3ViewportBinding binding, View view, long flags) {
        binding.hierarchyChanged(new HierarchyEvent(view, HierarchyEvent.HIERARCHY_CHANGED,
                view, view.getParent(), flags));
    }

    private static M3ViewportBinding install(Viewport viewport, View view, List<String> calls) {
        viewport.setView(view);
        return M3ViewportBinding.install(view, 10, 2,
                (first, visible, overscan) -> calls.add(first + ":" + visible + ":" + overscan));
    }

    @Test
    void hiddenDeferralAndShowingGeometryStayUnchanged() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport viewport = new Viewport();
            List<String> calls = new ArrayList<>();
            M3ViewportBinding binding = install(viewport, view, calls);
            assertEquals(1, registrations(viewport, binding));
            assertEquals(1, binding.deferred());
            assertEquals(0, binding.pushes());
            viewport.change();
            assertEquals(2, binding.deferred());
            assertTrue(calls.isEmpty());

            view.showing = true;
            hierarchy(binding, view, HierarchyEvent.SHOWING_CHANGED);
            assertEquals(List.of("2:4:2"), calls);
            assertEquals(1, binding.pushes());
            hierarchy(binding, view, HierarchyEvent.SHOWING_CHANGED);
            assertEquals(1, binding.pushes(), "no pending viewport is pushed twice");
            viewport.change();
            assertEquals(List.of("2:4:2", "2:4:2"), calls);
        });
    }

    @Test
    void sameParentHierarchyRefreshPreservesListenerOrder() throws Exception {
        onEdt(() -> {
            View view = new View();
            view.showing = true;
            Viewport viewport = new Viewport();
            viewport.setView(view);
            List<String> calls = new ArrayList<>();
            M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 2,
                    (first, visible, overscan) -> calls.add("binding"));
            viewport.addChangeListener(event -> calls.add("external"));
            calls.clear();
            viewport.change();
            assertEquals(List.of("external", "binding"), calls);
            calls.clear();

            hierarchy(binding, view, HierarchyEvent.PARENT_CHANGED);
            viewport.change();
            assertEquals(List.of("binding", "external"), calls,
                    "retain the original remove/add refresh ordering");
            assertEquals(1, registrations(viewport, binding));
        });
    }

    @Test
    void unrelatedHierarchyEventsDoNotReorderListeners() throws Exception {
        onEdt(() -> {
            View view = new View();
            view.showing = true;
            Viewport viewport = new Viewport();
            viewport.setView(view);
            List<String> calls = new ArrayList<>();
            M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 2,
                    (first, visible, overscan) -> calls.add("binding"));
            viewport.addChangeListener(event -> calls.add("external"));
            calls.clear();
            hierarchy(binding, view, HierarchyEvent.DISPLAYABILITY_CHANGED);
            viewport.change();
            assertEquals(List.of("external", "binding"), calls);
        });
    }

    @Test
    void reparentDetachesFormerViewport() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport former = new Viewport();
            Viewport current = new Viewport();
            M3ViewportBinding binding = install(former, view, new ArrayList<>());
            current.setView(view);
            assertEquals(0, registrations(former, binding), "former viewport retained binding");
            assertEquals(1, registrations(current, binding));
        });
    }

    @Test
    void formerViewportCannotDispatchAfterReparent() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport former = new Viewport();
            Viewport current = new Viewport();
            M3ViewportBinding binding = install(former, view, new ArrayList<>());
            current.setView(view);
            int deferred = binding.deferred();
            former.change();
            assertEquals(deferred, binding.deferred(), "former viewport still dispatches binding");
            current.change();
            assertEquals(deferred + 1, binding.deferred());
        });
    }

    @Test
    void removalDetachesListenerWithoutAddingDisposalApi() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport viewport = new Viewport();
            M3ViewportBinding binding = install(viewport, view, new ArrayList<>());
            viewport.setView(null);
            assertNull(view.getParent());
            assertEquals(0, registrations(viewport, binding), "detached view retained by viewport");
            int deferred = binding.deferred();
            viewport.change();
            assertEquals(deferred, binding.deferred());
        });
    }

    @Test
    void movingToOrdinaryParentDetachesViewport() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport viewport = new Viewport();
            M3ViewportBinding binding = install(viewport, view, new ArrayList<>());
            JPanel ordinaryParent = new JPanel();
            ordinaryParent.add(view);
            assertEquals(ordinaryParent, view.getParent());
            assertEquals(0, registrations(viewport, binding), "non-viewport parent left stale listener");
        });
    }

    @Test
    void initiallyUnparentedBindingFollowsLaterAttachment() throws Exception {
        onEdt(() -> {
            View view = new View();
            M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 2,
                    (first, visible, overscan) -> { });
            Viewport viewport = new Viewport();
            viewport.setView(view);
            assertEquals(1, registrations(viewport, binding));
            viewport.setView(null);
            assertEquals(0, registrations(viewport, binding), "later attachment was not released");
        });
    }

    @Test
    void repeatedMovesKeepExactlyOneCurrentRegistration() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport first = new Viewport();
            Viewport second = new Viewport();
            Viewport third = new Viewport();
            JPanel ordinaryParent = new JPanel();
            List<Viewport> viewports = List.of(first, second, third);
            M3ViewportBinding binding = install(first, view, new ArrayList<>());
            Container[] route = {second, first, third, ordinaryParent, second, null, first};
            for (int pass = 0; pass < 8; pass++) {
                for (Container parent : route) {
                    if (parent instanceof JViewport viewport) {
                        viewport.setView(view);
                    } else if (parent != null) {
                        parent.add(view);
                    } else if (view.getParent() != null) {
                        view.getParent().remove(view);
                    }
                    for (Viewport viewport : viewports) {
                        assertEquals(parent == viewport ? 1 : 0, registrations(viewport, binding),
                                "binding must belong only to its current direct viewport");
                    }
                }
            }
        });
    }

    @Test
    void rebindingRetainsUnrelatedViewportListeners() throws Exception {
        onEdt(() -> {
            View view = new View();
            Viewport former = new Viewport();
            Viewport current = new Viewport();
            List<String> calls = new ArrayList<>();
            former.addChangeListener(event -> calls.add("former"));
            current.addChangeListener(event -> calls.add("current"));
            M3ViewportBinding binding = install(former, view, new ArrayList<>());
            current.setView(view);
            calls.clear();
            former.change();
            current.change();
            assertEquals(List.of("former", "current"), calls);
            assertEquals(0, registrations(former, binding));
            assertEquals(1, registrations(current, binding));
        });
    }
}
