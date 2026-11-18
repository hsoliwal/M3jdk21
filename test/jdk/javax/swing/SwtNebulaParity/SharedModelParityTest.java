/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Swing web widgets retain the shared Nebula paint and state contracts
 * @modules java.desktop/jdk.m3.swing.web.model
 * @run main/othervm -Djava.awt.headless=true SharedModelParityTest
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jdk.m3.swing.web.model.M3WebModel;
import jdk.m3.swing.web.model.M3WebPaint;
import jdk.m3.swing.web.model.M3WebStates;

public class SharedModelParityTest {
    private static int checks;
    private static int failures;

    public static void main(String[] args) {
        run("visible paint and focus", SharedModelParityTest::visiblePaint);
        run("hidden and empty paint", SharedModelParityTest::paintAdmission);
        run("hidden item focus", SharedModelParityTest::hiddenFocus);
        run("stale focused item", SharedModelParityTest::staleFocus);
        run("stale hover item", SharedModelParityTest::staleHover);
        run("changed state layout order", SharedModelParityTest::stateLayout);
        run("unchanged layout bits", SharedModelParityTest::unchangedLayoutBits);
        run("text release and frame reuse", SharedModelParityTest::textRelease);
        run("overflowing clip endpoints", SharedModelParityTest::clipEndpoints);
        run("large tile hit testing", SharedModelParityTest::largeTileHitTesting);
        run("clip differential", SharedModelParityTest::clipDifferential);
        System.out.println("shared-model checks=" + checks + " failed-scenarios=" + failures);
        if (failures != 0) {
            throw new AssertionError(failures + " shared model scenarios failed");
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
        } catch (Throwable failure) {
            failures++;
            System.err.println(name + ": " + failure);
        }
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static Probe probe() {
        Probe model = new Probe();
        model.add("first");
        model.add("second");
        model.layout(160, 96);
        model.takeDamage(new int[4]);
        model.layoutCalls = 0;
        model.damageCalls.clear();
        return model;
    }

    private static void visiblePaint() {
        Probe model = probe();
        M3WebPaint paint = new M3WebPaint();
        model.paint(paint);
        check(model.emits == 1 && paint.count() == 1, "visible model emits its body");
        int before = paint.hash();
        model.paint(paint);
        check(paint.hash() == before, "repeated visible paint is identical");
        model.setFocus(1);
        model.setState(-1, M3WebStates.FOCUS, true);
        model.paint(paint);
        check(paint.count() == 2, "visible focused item retains its outline");
        check(paint.get(1, M3WebPaint.F_OP) == M3WebPaint.ROUND_LINE, "outline opcode");
    }

    private static void paintAdmission() {
        Probe model = probe();
        M3WebPaint paint = new M3WebPaint();
        model.paint(paint);
        model.setState(-1, M3WebStates.HIDDEN, true);
        int emits = model.emits;
        model.paint(paint);
        check(paint.count() == 0 && model.emits == emits, "hidden model clears records without invoking painter");
        model.setState(-1, M3WebStates.HIDDEN, false);
        model.layout(0, 96);
        model.paint(paint);
        check(paint.count() == 0 && model.emits == emits, "zero width clears records without invoking painter");
        model.layout(160, 0);
        model.paint(paint);
        check(paint.count() == 0 && model.emits == emits, "zero height clears records without invoking painter");
        model.layout(160, 96);
        model.paint(paint);
        check(paint.count() == 1 && model.emits == emits + 1, "showing restores body paint");
    }

    private static void hiddenFocus() {
        Probe model = probe();
        M3WebPaint paint = new M3WebPaint();
        model.setFocus(1);
        model.setState(-1, M3WebStates.FOCUS, true);
        model.setState(1, M3WebStates.HIDDEN, true);
        model.paint(paint);
        check(paint.count() == 1, "hidden item has no focus outline");
        check(model.focus() == 1, "hiding does not rewrite focus identity");
    }

    private static void staleFocus() {
        Probe model = probe();
        model.seedStaleFocus();
        M3WebPaint paint = new M3WebPaint();
        model.setState(-1, M3WebStates.FOCUS, true);
        model.paint(paint);
        check(paint.count() == 1, "subclass stale focus cannot read an inactive item");
        model.setFocus(0);
        check(model.damageCalls.equals(List.of(0)), "focus change damages only active item");
    }

    private static void staleHover() {
        Probe model = probe();
        model.seedStaleHover();
        model.pointer(M3WebStates.P_EXIT, -1, -1, 0);
        check(model.snapshot()[6] == -1, "pointer exit clears stale subclass hover");
    }

    private static void stateLayout() {
        Probe model = probe();
        List<Integer> heights = new ArrayList<>();
        List<Integer> masks = new ArrayList<>();
        model.setDelta((item, mask) -> {
            heights.add(model.itemRect(item, 3));
            masks.add(mask);
        });
        model.setState(0, M3WebStates.OPEN, true);
        check(heights.equals(List.of(40)), "OPEN publishes new geometry before state delta");
        check(masks.equals(List.of(M3WebModel.D_STATE)), "OPEN retains single state delta");
        model.setState(0, M3WebStates.HIDDEN, true);
        check(heights.equals(List.of(40, 0)), "HIDDEN publishes new geometry before state delta");
        check(model.layoutCalls == 2, "layout only runs for changed geometry bits");
    }

    private static void unchangedLayoutBits() {
        Probe model = probe();
        model.setState(0, M3WebStates.OPEN, true);
        int layoutCalls = model.layoutCalls;
        model.setState(0, M3WebStates.OPEN | M3WebStates.SELECTED, true);
        check(model.layoutCalls == layoutCalls, "unchanged OPEN in a changed mask does not relayout");
        model.setState(0, M3WebStates.SELECTED, true);
        check(model.layoutCalls == layoutCalls, "unchanged state remains a no-op");
    }

    private static void textRelease() {
        M3WebPaint paint = new M3WebPaint();
        for (int i = 0; i < 40; i++) {
            paint.text(i, i, 20, 10, 0, 12, "frame-one-" + i);
        }
        paint.reset();
        check(paint.count() == 0, "reset clears records");
        for (int i = 0; i < 40; i++) {
            check(paint.text(i) == null, "reset releases text " + i);
        }
        paint.text(2, 3, 4, 5, 0x123456, 12, "next");
        int hash = paint.hash();
        paint.reset();
        paint.text(2, 3, 4, 5, 0x123456, 12, "next");
        check(paint.hash() == hash, "reused frame record hash matches fresh replay");
    }

    private static void clipEndpoints() {
        M3WebPaint paint = new M3WebPaint();
        paint.rect(Integer.MAX_VALUE - 10, 5, 20, 10, 0);
        check(paint.intersects(0, Integer.MAX_VALUE - 5, 5, 20, 10), "positive endpoint overflow intersects");
        paint.reset();
        paint.rect(5, Integer.MAX_VALUE - 10, 10, 20, 0);
        check(paint.intersects(0, 5, Integer.MAX_VALUE - 5, 10, 20), "vertical endpoint overflow intersects");
    }

    private static void clipDifferential() {
        int[] coordinates = {Integer.MIN_VALUE, -21, 0, 9, Integer.MAX_VALUE - 10, Integer.MAX_VALUE};
        int[] extents = {0, 1, 12, Integer.MAX_VALUE};
        M3WebPaint paint = new M3WebPaint();
        for (int x : coordinates) {
            for (int w : extents) {
                for (int cx : coordinates) {
                    for (int cw : extents) {
                        paint.reset();
                        paint.rect(x, 3, w, 7, 0);
                        boolean expected = (long) x <= (long) cx + cw && (long) x + w >= cx;
                        check(paint.intersects(0, cx, 3, cw, 7) == expected,
                                "long endpoint oracle " + Arrays.toString(new int[] {x, w, cx, cw}));
                    }
                }
            }
        }
    }

    private static void largeTileHitTesting() {
        Probe model = probe();
        model.largeTile();
        check(model.hitTest(15, 100) == 0, "positive-origin tall tile remains hittable");
        check(model.hitTest(10, 20) == 0, "top and left edges are included");
        check(model.hitTest(40, 100) == -1, "right edge remains excluded");
        check(model.hitTest(15, 19) == -1, "point above tile remains outside");
        check(model.hitTest(15, Integer.MAX_VALUE) == 0, "large bottom coordinate does not wrap");
        int[] origins = {Integer.MIN_VALUE, -12, 0, 7, Integer.MAX_VALUE - 9, Integer.MAX_VALUE};
        int[] extents = {-1, 0, 1, Integer.MAX_VALUE};
        for (int origin : origins) {
            for (int extent : extents) {
                for (int point : origins) {
                    model.tile(origin, 11, extent, 20);
                    boolean inside = point >= origin && point < (long) origin + extent;
                    check((model.hitTest(point, 12) == 0) == inside, "horizontal half-open hit oracle");
                    model.tile(11, origin, 20, extent);
                    check((model.hitTest(12, point) == 0) == inside, "vertical half-open hit oracle");
                }
            }
        }
    }

    private static final class Probe extends M3WebModel {
        int emits;
        int layoutCalls;
        final List<Integer> damageCalls = new ArrayList<>();

        Probe() {
            super("parity-probe", M3WebStates.role("list"), 0, M3WebStates.RECT, 0);
        }

        void seedStaleFocus() { focus = count + 1; }
        void seedStaleHover() { hover = count + 1; }
        void largeTile() {
            tile(10, 20, 30, Integer.MAX_VALUE);
        }
        void tile(int x, int y, int width, int height) {
            setRect(0, x, y, width, height);
            setRect(1, 0, 0, 0, 0);
        }

        @Override
        public void load(String sample) {
            clear();
            add(sample);
        }

        @Override
        public int aspects() { return A_R | A_G | A_D | A_P; }

        @Override
        protected void doLayout() {
            layoutCalls++;
            for (int i = 0; i < count; i++) {
                setRect(i, 4, 4 + i * 20, 80,
                        (flags[i] & M3WebStates.HIDDEN) != 0 ? 0 : (flags[i] & M3WebStates.OPEN) != 0 ? 40 : 12);
            }
        }

        @Override
        protected void damageItem(int item) {
            damageCalls.add(item);
            super.damageItem(item);
        }

        @Override
        protected void emit(M3WebPaint paint) {
            emits++;
            paint.rect(0, 0, width, height, 0x123456);
        }
    }
}
