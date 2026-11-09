/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Preserve the six absorbed graphics names, Java native declarations and geometry contracts
 * @modules jdk.m3.graphics
 * @run main/othervm M3GraphicsGeometryAtoms
 */

import java.lang.reflect.Modifier;
import java.util.Arrays;
import jdk.m3.graphics.M3Paint;
import jdk.m3.graphics.M3Path;
import jdk.m3.graphics.M3Region;
import jdk.m3.graphics.M3TextBlob;
import jdk.m3.graphics.M3RRect;
import jdk.m3.graphics.M3Rect;

/** Java source/API and geometry acceptance; no native rendering call is made. */
public final class M3GraphicsGeometryAtoms {
    private static int checks;

    public static void main(String[] args) {
        namesAndNativeDeclarationsWithoutInitialization();
        rectangularGeometry();
        factoriesAndFloatingPointEdges();
        roundedGeometryAndRadiiOwnership();
        System.out.println("M3GraphicsGeometryAtoms: " + checks
                + " checks passed; 144 Java native declarations inspected; native rendering NOT_RUN");
    }

    private static void namesAndNativeDeclarationsWithoutInitialization() {
        // Class literals and method inspection load types without invoking their static/native initialization.
        Class<?>[] owners = {
                M3Paint.class, M3Path.class, M3Region.class, M3TextBlob.class, M3RRect.class, M3Rect.class
        };
        String[] names = {"M3Paint", "M3Path", "M3Region", "M3TextBlob", "M3RRect", "M3Rect"};
        int[] nativeCounts = {40, 60, 28, 16, 0, 0};
        int total = 0;
        for (int i = 0; i < owners.length; i++) {
            Class<?> owner = owners[i];
            check(owner.getName().equals("jdk.m3.graphics." + names[i]), "canonical owner " + names[i]);
            check("jdk.m3.graphics".equals(owner.getModule().getName()), "integrated module " + names[i]);
            long observed = Arrays.stream(owner.getDeclaredMethods())
                    .filter(method -> Modifier.isNative(method.getModifiers())).count();
            check(observed == nativeCounts[i], "native declaration count " + names[i] + ": " + observed);
            total += nativeCounts[i];
            if (i < 4) {
                check(owner.getSuperclass().getName().equals("jdk.m3.graphics.impl.M3Managed"),
                        "managed native lifetime owner " + names[i]);
                check(AutoCloseable.class.isAssignableFrom(owner), "close contract " + names[i]);
            }
        }
        check(total == 144, "complete selected Java native surface");
        check(M3RRect.class.getSuperclass() == M3Rect.class, "rounded rectangle subtype contract");
    }

    private static void rectangularGeometry() {
        M3Rect rect = M3Rect.makeXYWH(10, 20, 30, 40);
        bounds(rect, 10, 20, 40, 60);
        exact(30, rect.getWidth(), "width");
        exact(40, rect.getHeight(), "height");
        check(rect.offset(0, 0) == rect, "zero offset retains identity");
        check(rect.scale(1, 1) == rect, "unit scale retains identity");
        check(rect.withLeft(10) == rect, "unchanged left retains identity");
        check(rect.withTop(20) == rect, "unchanged top retains identity");
        check(rect.withRight(40) == rect, "unchanged right retains identity");
        check(rect.withBottom(60) == rect, "unchanged bottom retains identity");
        bounds(rect.offset(1, -2), 11, 18, 41, 58);
        bounds(rect.scale(2, 3), 20, 60, 80, 180);
        bounds(rect, 10, 20, 40, 60);
        bounds(rect.withWidth(5), 10, 20, 15, 60);
        bounds(rect.withHeight(7), 10, 20, 40, 27);

        bounds(rect.intersect(M3Rect.makeLTRB(30, 50, 50, 80)), 30, 50, 40, 60);
        check(rect.intersect(M3Rect.makeLTRB(40, 20, 45, 60)) == null,
                "edge-touching rectangles have no positive-area intersection");
        check(rect.intersect(M3Rect.makeLTRB(-5, -5, -1, -1)) == null, "disjoint intersection");
        check(rect.contains(10, 20), "left/top edges are included");
        check(rect.contains(40, 60), "right/bottom edges are included");
        check(!rect.contains(Math.nextUp(40.0f), 60), "beyond right edge is excluded");
        check(!rect.isEmpty(), "positive dimensions are not empty");
        check(M3Rect.makeWH(0, 5).isEmpty(), "zero width is empty");
        check(M3Rect.makeWH(5, 0).isEmpty(), "zero height is empty");

        M3Rect equal = M3Rect.makeLTRB(10, 20, 40, 60);
        check(rect.equals(equal) && equal.equals(rect), "rectangle value equality");
        check(rect.hashCode() == equal.hashCode(), "equal rectangle hash");
        check(!rect.equals(null), "null inequality");
    }

    private static void factoriesAndFloatingPointEdges() {
        rejects(() -> M3Rect.makeLTRB(2, 0, 1, 1), "reversed horizontal factory bounds");
        rejects(() -> M3Rect.makeLTRB(0, 2, 1, 1), "reversed vertical factory bounds");
        rejects(() -> M3Rect.makeWH(-1, 1), "negative width");
        rejects(() -> M3Rect.makeXYWH(0, 0, 1, -1), "negative height");
        // The public constructor is deliberately less restrictive than the checked factories.
        M3Rect inverted = new M3Rect(3, 4, 1, 2);
        exact(-2, inverted.getWidth(), "constructor retains signed width");
        exact(-2, inverted.getHeight(), "constructor retains signed height");
        M3Rect positiveZero = new M3Rect(0.0f, 0, 1, 1);
        M3Rect negativeZero = new M3Rect(-0.0f, 0, 1, 1);
        check(!positiveZero.equals(negativeZero), "value equality distinguishes signed zero");
        check(positiveZero.withLeft(-0.0f) == positiveZero,
                "wither retains the incumbent floating-point == identity contract");
        M3Rect nanA = new M3Rect(Float.NaN, 0, 1, 1);
        M3Rect nanB = new M3Rect(Float.NaN, 0, 1, 1);
        check(nanA.equals(nanB) && nanA.hashCode() == nanB.hashCode(), "NaN equality/hash contract");
        check(!nanA.contains(0, 0), "NaN bound contains no point");
    }

    private static void roundedGeometryAndRadiiOwnership() {
        float[] radii = {1, 2, 3, 4};
        M3RRect rounded = M3RRect.makeComplexLTRB(2, 4, 10, 12, radii);
        check(rounded._radii == radii, "public complex factory retains the supplied radii array");
        check(rounded.offset(0, 0) == rounded, "rounded zero-offset identity");
        check(rounded.scale(1, 1) == rounded, "rounded unit-scale identity");
        M3RRect moved = rounded.offset(5, 7);
        bounds(moved, 7, 11, 15, 19);
        check(moved._radii == radii, "offset retains the radii alias");
        radii[0] = 9;
        exact(9, rounded._radii[0], "caller mutation remains visible");
        exact(9, moved._radii[0], "shared radii remain visible through offset result");

        float[][] inputs = {{1}, {1, 2}, {1, 2, 3, 4}, {1, 2, 3, 4, 5, 6, 7, 8}};
        float[][] anisotropic = {{2, 3}, {2, 6}, {2, 3, 4, 6, 6, 9, 8, 12}, {2, 6, 6, 12, 10, 18, 14, 24}};
        for (int i = 0; i < inputs.length; i++) {
            M3RRect original = new M3RRect(2, 4, 10, 12, inputs[i]);
            M3RRect scaled = original.scale(2, 3);
            bounds(scaled, 4, 12, 20, 36);
            check(Arrays.equals(anisotropic[i], scaled._radii), "anisotropic radius shape " + inputs[i].length);
            check(scaled._radii != inputs[i], "nontrivial scale owns independent radii " + inputs[i].length);
            M3RRect uniform = original.scale(2);
            bounds(uniform, 4, 8, 20, 24);
            check(uniform._radii.length == inputs[i].length, "uniform radius shape " + inputs[i].length);
            for (int j = 0; j < inputs[i].length; j++) exact(inputs[i][j] * 2, uniform._radii[j], "uniform radius");
        }

        M3Rect plain = M3Rect.makeLTRB(0, 0, 10, 10);
        M3RRect round = plain.withRadii(2);
        check(!plain.equals(round) && !round.equals(plain), "subtype-aware symmetric equality");
        check(round.equals(plain.withRadii(2)), "rounded value equality");
        check(round.hashCode() == plain.withRadii(2).hashCode(), "rounded value hash");
        check(plain.inflate(1) instanceof M3RRect, "positive rectangular inflation produces rounded geometry");
        M3Rect contracted = M3RRect.makeLTRB(0, 0, 10, 10, 0).inflate(-1);
        check(contracted.getClass() == M3Rect.class, "negative radii collapse to a plain rectangle");
        bounds(contracted, 1, 1, 9, 9);
    }

    private static void bounds(M3Rect rect, float left, float top, float right, float bottom) {
        check(rect != null, "non-null rectangle");
        exact(left, rect.getLeft(), "left");
        exact(top, rect.getTop(), "top");
        exact(right, rect.getRight(), "right");
        exact(bottom, rect.getBottom(), "bottom");
    }

    private static void exact(float expected, float actual, String label) {
        check(Float.floatToIntBits(expected) == Float.floatToIntBits(actual),
                label + ": expected " + expected + ", got " + actual);
    }

    private static void rejects(Runnable action, String label) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException: " + label);
    }

    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
}
