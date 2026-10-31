/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.swing.viewport;

import java.awt.Dimension;
import java.awt.Point;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;

/** Actual Swing parent and listener lifetime acceptance; no native display required. */
public final class M3ViewportBindingLifecycleProof {
    private int checks;

    private M3ViewportBindingLifecycleProof() { }

    public static void main(String[] args) throws Exception {
        System.out.println("M3_VIEWPORT_BINDING_LIFECYCLE_PASS=" + verify());
    }

    static int verify() throws Exception {
        M3ViewportBindingLifecycleProof proof = new M3ViewportBindingLifecycleProof();
        if (SwingUtilities.isEventDispatchThread()) {
            proof.run();
        } else {
            SwingUtilities.invokeAndWait(proof::run);
        }
        return proof.checks;
    }

    private void run() {
        check(SwingUtilities.isEventDispatchThread(), "proof runs on EDT");
        reparentAndDetach();
        installBeforeParenting();
        visibleSyncAndDeferral();
    }

    private void reparentAndDetach() {
        JPanel view = new JPanel();
        JViewport first = new JViewport();
        JViewport second = new JViewport();
        first.setView(view);
        int[] targetCalls = {0};
        M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 3,
                (row, count, overscan) -> targetCalls[0]++);
        eq(1, binding.deferred(), "hidden install defers once");
        eq(0, binding.pushes(), "hidden install does not push");
        subscription(first, binding, 1, "installed current viewport");
        subscription(second, binding, 0, "unrelated viewport");
        eventDelivery(first, binding, true, "initial current viewport");

        second.setView(view);
        check(view.getParent() == second, "real reparent changed component parent");
        subscription(first, binding, 0, "former viewport after reparent");
        subscription(second, binding, 1, "new viewport after reparent");
        eventDelivery(first, binding, false, "former viewport after reparent");
        eventDelivery(second, binding, true, "current viewport after reparent");

        for (int move = 0; move < 32; move++) {
            JViewport previous = (JViewport) view.getParent();
            JViewport current = previous == first ? second : first;
            current.setView(view);
            subscription(previous, binding, 0, "former viewport on repeated move " + move);
            subscription(current, binding, 1, "current viewport on repeated move " + move);
            eventDelivery(previous, binding, false, "former host event on move " + move);
            eventDelivery(current, binding, true, "current host event on move " + move);
        }

        JViewport current = (JViewport) view.getParent();
        current.remove(view);
        check(view.getParent() == null, "real remove detaches component");
        subscription(first, binding, 0, "first viewport after remove");
        subscription(second, binding, 0, "second viewport after remove");
        eventDelivery(first, binding, false, "first viewport after remove");
        eventDelivery(second, binding, false, "second viewport after remove");
        eq(0, targetCalls[0], "hidden movement never requests rows");
        eq(0, binding.pushes(), "hidden movement never pushes");
    }

    private void installBeforeParenting() {
        JPanel view = new JPanel();
        JPanel ordinaryParent = new JPanel();
        JViewport viewport = new JViewport();
        int[] targetCalls = {0};
        M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 3,
                (row, count, overscan) -> targetCalls[0]++);
        eq(1, binding.deferred(), "unparented install defers once");
        ordinaryParent.add(view);
        check(view.getParent() == ordinaryParent, "ordinary container owns view");
        subscription(viewport, binding, 0, "ordinary parent has no viewport subscription");

        viewport.setView(view);
        subscription(viewport, binding, 1, "binding follows first viewport parent");
        eventDelivery(viewport, binding, true, "first viewport after delayed parenting");
        ordinaryParent.add(view);
        subscription(viewport, binding, 0, "move to ordinary parent releases viewport");
        eventDelivery(viewport, binding, false, "viewport after move to ordinary parent");

        viewport.setView(view);
        subscription(viewport, binding, 1, "reattach registers exactly once");
        int deferred = binding.deferred();
        binding.sync();
        eq(deferred + 1, binding.deferred(), "explicit hidden sync retains deferral");
        eq(0, targetCalls[0], "hidden reattachment never requests rows");
        viewport.remove(view);
        subscription(viewport, binding, 0, "final detach releases viewport");
    }

    private void visibleSyncAndDeferral() {
        JPanel root = new JPanel(null);
        JPanel view = new JPanel();
        JViewport viewport = new JViewport();
        root.setBounds(0, 0, 200, 200);
        viewport.setBounds(10, 10, 80, 25);
        view.setPreferredSize(new Dimension(80, 100));
        view.setSize(80, 100);
        viewport.setView(view);
        root.add(viewport);
        int[] last = {-1, -1, -1, 0};
        M3ViewportBinding binding = M3ViewportBinding.install(view, 10, 3,
                (row, count, overscan) -> {
                    check(SwingUtilities.isEventDispatchThread(), "target callback runs on EDT");
                    last[0] = row;
                    last[1] = count;
                    last[2] = overscan;
                    last[3]++;
                });
        try {
            // These are actual Swing lightweight peers, not an isShowing override.
            root.addNotify();
            check(view.isShowing(), "actual lightweight hierarchy is showing");
            check(last[3] > 0, "showing restores the pending viewport");
            eq(0, last[0], "initial first row");
            eq(4, last[1], "existing partial-row convention");
            eq(3, last[2], "overscan preserved");

            int calls = last[3];
            binding.sync();
            eq(calls + 1, last[3], "explicit visible sync invokes target once");
            viewport.setViewPosition(new Point(0, 15));
            eq(1, last[0], "scroll updates first row");
            eq(4, last[1], "scroll preserves visible-row convention");

            root.setVisible(false);
            check(!view.isShowing(), "actual hierarchy is hidden");
            calls = last[3];
            int deferred = binding.deferred();
            viewport.setViewPosition(new Point(0, 25));
            eq(calls, last[3], "hidden scroll does not invoke target");
            check(binding.deferred() > deferred, "hidden scroll records pending viewport");
            root.setVisible(true);
            eq(calls + 1, last[3], "showing flushes pending viewport once");
            eq(2, last[0], "showing uses latest scroll position");
            eq(4, last[1], "showing preserves visible-row convention");
            eq(3, last[2], "showing preserves overscan");
        } finally {
            root.removeNotify();
            viewport.remove(view);
        }
        subscription(viewport, binding, 0, "visible fixture final detach");
    }

    private void eventDelivery(JViewport viewport, M3ViewportBinding binding,
                               boolean subscribed, String label) {
        int[] events = {0};
        ChangeListener observer = event -> {
            check(event.getSource() == viewport, label + ": actual event source");
            check(SwingUtilities.isEventDispatchThread(), label + ": event EDT");
            events[0]++;
        };
        viewport.addChangeListener(observer);
        int deferred = binding.deferred();
        int pushes = binding.pushes();
        try {
            viewport.setExtentSize(new Dimension(viewport.getWidth() + 1, viewport.getHeight() + 1));
        } finally {
            viewport.removeChangeListener(observer);
        }
        check(events[0] > 0, label + ": real viewport emitted change");
        eq(subscribed ? events[0] : 0, binding.deferred() - deferred,
                label + ": hidden delivery follows current host only");
        eq(pushes, binding.pushes(), label + ": hidden delivery does not push");
    }

    private void subscription(JViewport viewport, M3ViewportBinding binding, int expected, String label) {
        int count = 0;
        for (ChangeListener listener : viewport.getChangeListeners()) {
            if (listener == binding) count++;
        }
        eq(expected, count, label + ": binding listener count");
    }

    private void eq(int expected, int actual, String label) {
        check(expected == actual, label + ": expected " + expected + ", got " + actual);
    }

    private void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
