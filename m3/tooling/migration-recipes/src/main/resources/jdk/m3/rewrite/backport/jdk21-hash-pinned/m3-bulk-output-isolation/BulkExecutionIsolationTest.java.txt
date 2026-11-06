/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 */
/*
 * @test
 * @summary Isolate primitive result arrays from provider and consumer mutation
 * @modules java.base/jdk.internal.vm.parallel
 * @run main BulkExecutionIsolationTest
 */
import java.util.HashMap;
import java.util.Map;
import jdk.internal.vm.parallel.BulkExecution;

public class BulkExecutionIsolationTest {
    private static final String ROOT = "0".repeat(64);
    private static int checks;

    public static void main(String[] args) {
        boolean[] booleans = {true};
        byte[] bytes = {(byte) 0x80};
        short[] shorts = {-32768};
        char[] chars = {'\ud800'};
        int[] ints = {Integer.MIN_VALUE};
        long[] longs = {Long.MIN_VALUE};
        float[] floats = {Float.NaN};
        double[] doubles = {Double.NEGATIVE_INFINITY};
        HashMap<String, Object> source = new HashMap<>();
        source.put("boolean", booleans);
        source.put("byte", bytes);
        source.put("short", shorts);
        source.put("char", chars);
        source.put("int", ints);
        source.put("long", longs);
        source.put("float", floats);
        source.put("double", doubles);
        source.put("scalar", "unchanged");

        BulkExecution receipt = new BulkExecution(source, "cpu", ROOT);
        booleans[0] = false;
        bytes[0] = 0;
        shorts[0] = 0;
        chars[0] = 'x';
        ints[0] = 0;
        longs[0] = 0;
        floats[0] = 0;
        doubles[0] = 0;
        source.put("later", "unrelated");

        Map<String, Object> observed = receipt.outputs();
        check(((boolean[]) observed.get("boolean"))[0]);
        check(((byte[]) observed.get("byte"))[0] == (byte) 0x80);
        check(((short[]) observed.get("short"))[0] == -32768);
        check(((char[]) observed.get("char"))[0] == '\ud800');
        check(((int[]) observed.get("int"))[0] == Integer.MIN_VALUE);
        check(((long[]) observed.get("long"))[0] == Long.MIN_VALUE);
        check(Float.isNaN(((float[]) observed.get("float"))[0]));
        check(((double[]) observed.get("double"))[0] == Double.NEGATIVE_INFINITY);
        check(observed.size() == 9);
        check("unchanged".equals(observed.get("scalar")));
        check(ROOT.equals(receipt.receiptRoot()));

        ((boolean[]) observed.get("boolean"))[0] = false;
        ((byte[]) observed.get("byte"))[0] = 0;
        ((short[]) observed.get("short"))[0] = 0;
        ((char[]) observed.get("char"))[0] = 'x';
        ((int[]) observed.get("int"))[0] = 0;
        ((long[]) observed.get("long"))[0] = 0;
        ((float[]) observed.get("float"))[0] = 0;
        ((double[]) observed.get("double"))[0] = 0;

        Map<String, Object> repeated = receipt.outputs();
        check(((boolean[]) repeated.get("boolean"))[0]);
        check(((byte[]) repeated.get("byte"))[0] == (byte) 0x80);
        check(((short[]) repeated.get("short"))[0] == -32768);
        check(((char[]) repeated.get("char"))[0] == '\ud800');
        check(((int[]) repeated.get("int"))[0] == Integer.MIN_VALUE);
        check(((long[]) repeated.get("long"))[0] == Long.MIN_VALUE);
        check(Float.isNaN(((float[]) repeated.get("float"))[0]));
        check(((double[]) repeated.get("double"))[0] == Double.NEGATIVE_INFINITY);
        check(repeated.get("int") != observed.get("int"));
        check(repeated.get("int") != ints);
        check("cpu".equals(receipt.provider()));
        expect(UnsupportedOperationException.class, () -> repeated.put("new", new int[0]));
        expect(NullPointerException.class, () -> new BulkExecution(Map.of("x", new int[1]), null, ROOT));
        expect(IllegalArgumentException.class, () -> new BulkExecution(Map.of("x", new int[1]), "cpu", "bad"));
        System.out.println("PASS checks=" + checks);
    }

    private static void check(boolean ok) { checks++; if (!ok) throw new AssertionError("check " + checks); }
    private static void expect(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable failure) {
            check(type.isInstance(failure)); return;
        }
        throw new AssertionError("expected " + type.getName());
    }
}
