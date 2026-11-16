/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */

/*
 * @test
 * @summary Read-only composite interning: equal read-only composites share one InternShapes handle.
 * @modules java.base/jdk.internal.m3.intern
 * @run main InternShapesCompositeTest
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jdk.internal.m3.intern.InternShapes;
import jdk.internal.m3.intern.InternSpace;
import jdk.internal.m3.intern.Internable;

public class InternShapesCompositeTest {

    record Point(int x, String label) { }

    enum Colour { RED, GREEN }

    private static int checks;

    public static void main(String[] args) throws Exception {
        InternSpace space = new InternSpace();
        InternShapes shapes = new InternShapes(space);
        long scope = 1;

        // equal lists built differently share a handle; order matters for lists
        same(shapes.internList(scope, List.of(1, 2)), shapes.internList(scope, List.copyOf(List.of(1, 2))), "List.of vs List.copyOf");
        differ(shapes.internList(scope, List.of(1, 2)), shapes.internList(scope, List.of(2, 1)), "list order");
        // order does not matter for sets and maps
        same(shapes.internSet(scope, Set.of(1, 2, 3)), shapes.internSet(scope, Set.of(3, 1, 2)), "set order");
        same(shapes.internMap(scope, Map.of("a", 1, "b", 2)), shapes.internMap(scope, Map.of("b", 2, "a", 1)), "map order");
        differ(shapes.internMap(scope, Map.of("a", 1)), shapes.internMap(scope, Map.of("a", 2)), "map values");
        // families are domain-separated
        differ(shapes.internValue(scope, List.of(1, 2)), shapes.internValue(scope, Set.of(1, 2)), "list vs set");
        differ(shapes.internValue(scope, "x"), shapes.internValue(scope, List.of("x")), "string vs list");
        differ(shapes.internValue(scope, 1), shapes.internValue(scope, 1L), "Integer vs Long (equals is false)");
        // nested composites
        Object a = List.of(Map.of("k", Set.of(1, 2)), Map.of());
        Object b = List.of(Map.of("k", Set.of(2, 1)), Map.of());
        same(shapes.internValue(scope, a), shapes.internValue(scope, b), "nested equal values");
        // boxed floating point follows equals: NaN equals NaN, +0.0 differs from -0.0
        same(shapes.internValue(scope, Double.NaN), shapes.internValue(scope, 0.0 / 0.0), "NaN");
        differ(shapes.internValue(scope, 0.0), shapes.internValue(scope, -0.0), "+0.0 vs -0.0");
        // null, enums, records
        same(shapes.internValue(scope, null), shapes.internValue(scope, null), "null");
        same(shapes.internEnum(scope, Colour.RED), shapes.internValue(scope, Colour.RED), "enum");
        differ(shapes.internEnum(scope, Colour.RED), shapes.internEnum(scope, Colour.GREEN), "enum constants");
        same(shapes.internRecordValue(scope, new Point(1, "p")), shapes.internValue(scope, new Point(1, "p")), "record");
        differ(shapes.internRecordValue(scope, new Point(1, "p")), shapes.internRecordValue(scope, new Point(2, "p")), "record components");
        // Internable values intern themselves
        long marker = shapes.internValue(scope, List.of("marker"));
        Internable custom = s -> marker;
        same(shapes.internValue(scope, custom), marker, "Internable");
        // mutable composites are refused; read-only views go through a snapshot
        ArrayList<Integer> mutable = new ArrayList<>(List.of(4, 5));
        refused(() -> shapes.internValue(scope, mutable), "mutable ArrayList via internValue");
        refused(() -> shapes.internList(scope, mutable), "mutable ArrayList via internList");
        refused(() -> shapes.internList(scope, Collections.unmodifiableList(mutable)), "unmodifiable view via internList");
        same(shapes.internSnapshot(scope, Collections.unmodifiableList(mutable)), shapes.internList(scope, List.of(4, 5)), "snapshot of view");
        same(shapes.internSnapshot(scope, Collections.unmodifiableMap(new java.util.HashMap<>(Map.of("z", 9)))),
             shapes.internMap(scope, Map.of("z", 9)), "snapshot of map view");
        refused(() -> shapes.internValue(scope, new Object()), "plain Object");
        refused(() -> shapes.internValue(-1, List.of()), "negative scope");

        // frozen space: known values keep their handle, new values are refused; archive round trip keeps handles
        long known = shapes.internValue(scope, a);
        space.freeze();
        same(shapes.internValue(scope, b), known, "frozen space returns the existing handle");
        try {
            shapes.internValue(scope, List.of("never seen"));
            throw new AssertionError("frozen space accepted a new composite");
        } catch (IllegalStateException expected) {
            checks++;
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        space.writeTo(new DataOutputStream(bytes));
        InternSpace replay = InternSpace.readFrom(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        same(new InternShapes(replay).internValue(scope, b), known, "archive round trip");
        System.out.println("InternShapesCompositeTest: " + checks + " checks passed");
    }

    private static void same(long x, long y, String what) {
        if (x != y) throw new AssertionError(what + ": expected the same handle, got " + x + " and " + y);
        checks++;
    }

    private static void differ(long x, long y, String what) {
        if (x == y) throw new AssertionError(what + ": expected different handles, both " + x);
        checks++;
    }

    private static void refused(Runnable r, String what) {
        try {
            r.run();
        } catch (IllegalArgumentException expected) {
            checks++;
            return;
        }
        throw new AssertionError(what + ": was not refused");
    }
}
