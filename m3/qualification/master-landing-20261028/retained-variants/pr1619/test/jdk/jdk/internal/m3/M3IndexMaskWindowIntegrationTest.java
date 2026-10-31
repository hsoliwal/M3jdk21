/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 * @test
 * @summary Verify mask-window backend capability, raw JNI validation and scalar parity
 * @modules java.base/jdk.internal.m3
 * @run main/othervm -Xint M3IndexMaskWindowIntegrationTest java
 * @run main/othervm -Xcheck:jni -Xint --add-opens=java.base/jdk.internal.m3=ALL-UNNAMED M3IndexMaskWindowIntegrationTest native
 * @run main/othervm -Xcheck:jni -Xmixed --add-opens=java.base/jdk.internal.m3=ALL-UNNAMED M3IndexMaskWindowIntegrationTest native
 */
import jdk.internal.m3.M3IndexMaskWindow;
import jdk.internal.m3.M3IndexMaskWindow.Backend;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Independent oracle for both scan entry points. The "unavailable" mode runs with
 * the owner patched into a JDK whose libjava lacks this ABI. The "image" mode
 * additionally requires an installed jrt owner and its own loaded libjava.
 */
public final class M3IndexMaskWindowIntegrationTest {
    private static final int GUARD = 0x5a19c0de;
    private static int checks;

    public static void main(String[] args) throws Exception {
        String mode = args.length == 0 ? "native" : args[0];
        if (mode.equals("unavailable")) {
            unavailable();
        } else {
            boolean nativeMode = mode.equals("native") || mode.equals("image");
            if (!nativeMode && !mode.equals("java")) {
                throw new IllegalArgumentException("mode: " + mode);
            }
            origin(mode.equals("image"));
            Backend backend = nativeMode ? Backend.NATIVE_REQUIRED : Backend.JAVA;
            if (nativeMode) {
                require(M3IndexMaskWindow.nativeAvailable(), "native ABI unavailable");
                rawValidation();
            }
            publicValidation(backend);
            scalarParity(backend);
        }
        System.out.println("MASK_WINDOW_INTEGRATION_PASS mode=" + mode + " checks=" + checks);
    }

    private static void unavailable() {
        List<String> failures = new ArrayList<>();
        if (M3IndexMaskWindow.nativeAvailable()) {
            failures.add("nativeAvailable accepted libjava without mask-window symbols");
        }
        checks++;
        int[] rows = {11, -7};
        byte[] states = {1, (byte) 128};
        int[] out = {GUARD, GUARD, GUARD};
        missing(failures, "scan zero output limit", () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, out, 1, 0, Backend.NATIVE_REQUIRED));
        missing(failures, "bounded zero source budget", () ->
                M3IndexMaskWindow.scanBounded(rows, states, 0, 0, 0, 0, out, 1, 1, Backend.NATIVE_REQUIRED));
        missing(failures, "bounded zero output limit", () ->
                M3IndexMaskWindow.scanBounded(rows, states, 0, 2, 0, 0, out, 1, 0, Backend.NATIVE_REQUIRED));
        missing(failures, "bounded terminal source", () ->
                M3IndexMaskWindow.scanBounded(rows, states, 2, Integer.MAX_VALUE, 0, 0, out, 1, 1, Backend.NATIVE_REQUIRED));
        missing(failures, "scan contradictory masks", () ->
                M3IndexMaskWindow.scan(rows, states, 0, 128, 128, out, 1, 1, Backend.NATIVE_REQUIRED));
        missing(failures, "scan nonempty source", () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, out, 1, 1, Backend.NATIVE_REQUIRED));
        missing(failures, "bounded nonempty source", () ->
                M3IndexMaskWindow.scanBounded(rows, states, 0, 1, 0, 0, out, 1, 1, Backend.NATIVE_REQUIRED));
        require(Arrays.equals(out, new int[] {GUARD, GUARD, GUARD}), "unavailable modified output");
        if (!failures.isEmpty()) {
            throw new AssertionError(String.join("\n", failures));
        }
    }

    private static void missing(List<String> failures, String label, Runnable call) {
        try {
            call.run();
            failures.add(label + " failed to refuse NATIVE_REQUIRED");
        } catch (UnsatisfiedLinkError expected) {
            // Explicit refusal; Java fallback is forbidden.
        }
        checks++;
    }

    private static void origin(boolean installed) throws Exception {
        Class<?> owner = M3IndexMaskWindow.class;
        require(owner.getClassLoader() == null, "owner must use bootstrap loader");
        require(owner.getModule().getName().equals("java.base"), "owner must belong to java.base");
        String resource = owner.getResource("M3IndexMaskWindow.class").toString();
        if (installed) {
            require(resource.equals("jrt:/java.base/jdk/internal/m3/M3IndexMaskWindow.class"),
                    "installed owner must originate in jrt: " + resource);
            if (System.getProperty("os.name").equals("Linux")) {
                Path expected = Path.of(System.getProperty("java.home"), "lib", "libjava.so").toRealPath();
                try (var maps = Files.lines(Path.of("/proc/self/maps"))) {
                    require(maps.anyMatch(line -> line.endsWith(" " + expected)),
                            "image libjava not loaded: " + expected);
                }
                System.out.println("MASK_WINDOW_LIBJAVA " + expected);
            }
        }
        System.out.println("MASK_WINDOW_OWNER module=" + owner.getModule().getName()
                + " loader=" + owner.getClassLoader() + " resource=" + resource);
    }

    private static void scalarParity(Backend backend) {
        int[] rows = new int[260];
        byte[] states = new byte[260];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = i % 5 == 0 ? Integer.MIN_VALUE : (i % 3 == 0 ? 17 : -i);
            states[i] = (byte) i;
        }
        // Every unsigned required/excluded combination, including overlap and high bits.
        for (int required = 0; required < 256; required++) {
            for (int excluded = 0; excluded < 256; excluded++) {
                int first = (required + excluded) % 4;
                int limit = (required * 17 + excluded) % 132;
                verify(rows, states, first, Integer.MAX_VALUE, required, excluded,
                        3, limit, false, backend);
                verify(rows, states, first, 257 - first, required, excluded,
                        5, limit, true, backend);
            }
        }
        // Deliberate sparse boundary cases cross the 128-slot JNI chunk and MAX_WINDOW.
        for (int length : new int[] {0, 1, 127, 128, 129, 255, 256, 257, 4095, 4096, 4097, 8201}) {
            int[] sparseRows = new int[length];
            byte[] sparseStates = new byte[length];
            for (int i = 0; i < length; i++) {
                sparseRows[i] = i % 7 == 0 ? 23 : -i;
                if (i % 128 == 0 || i % 128 == 127) sparseStates[i] = (byte) 128;
            }
            for (int first : new int[] {0, length / 2, length}) {
                for (int budget : new int[] {0, 1, 127, 128, 129, Integer.MAX_VALUE}) {
                    for (int limit : new int[] {0, 1, 127, 128, 129, M3IndexMaskWindow.MAX_WINDOW}) {
                        for (int required : new int[] {0, 128}) {
                            verify(sparseRows, sparseStates, first, budget, required, 0,
                                    4, limit, true, backend);
                        }
                    }
                }
                verify(sparseRows, sparseStates, first, Integer.MAX_VALUE, 128, 0,
                        2, M3IndexMaskWindow.MAX_WINDOW, false, backend);
            }
        }
        Random random = new Random(0x21_1557L);
        for (int iteration = 0; iteration < 4000; iteration++) {
            int length = random.nextInt(700);
            int[] randomRows = new int[length];
            byte[] randomStates = new byte[length];
            for (int i = 0; i < length; i++) {
                randomRows[i] = random.nextInt();
                randomStates[i] = (byte) random.nextInt(256);
            }
            int first = random.nextInt(length + 1);
            int required = random.nextInt(256), excluded = random.nextInt(256);
            int limit = random.nextInt(260), offset = iteration % 9;
            verify(randomRows, randomStates, first, random.nextInt(900), required, excluded,
                    offset, limit, true, backend);
            verify(randomRows, randomStates, first, Integer.MAX_VALUE, required, excluded,
                    offset, limit, false, backend);
        }
    }

    private static void verify(int[] rows, byte[] states, int first, int budget,
            int required, int excluded, int offset, int limit, boolean bounded, Backend backend) {
        int[] output = new int[offset + limit + 7];
        Arrays.fill(output, GUARD);
        int[] expected = output.clone();
        int[] oldRows = rows.clone();
        byte[] oldStates = states.clone();
        int end = bounded ? first + Math.min(budget, rows.length - first) : rows.length;
        int next = first, written = 0;
        while (next < end && written < limit) {
            int state = Byte.toUnsignedInt(states[next]);
            if ((state & required) == required && (state & excluded) == 0) {
                expected[offset + written++] = rows[next];
            }
            next++;
        }
        long expectedCursor = ((long) next << 32) | written;
        long actual = bounded
                ? M3IndexMaskWindow.scanBounded(rows, states, first, budget, required, excluded,
                        output, offset, limit, backend)
                : M3IndexMaskWindow.scan(rows, states, first, required, excluded,
                        output, offset, limit, backend);
        require(actual == expectedCursor && Arrays.equals(output, expected)
                        && Arrays.equals(rows, oldRows) && Arrays.equals(states, oldStates),
                "oracle bounded=" + bounded + " first=" + first + " budget=" + budget
                        + " required=" + required + " excluded=" + excluded + " limit=" + limit);
    }

    private static void publicValidation(Backend backend) {
        int[] rows = {1, 2, 3};
        byte[] states = {1, 2, 3};
        int[] output = {GUARD, GUARD, GUARD, GUARD};
        rejects(NullPointerException.class, () ->
                M3IndexMaskWindow.scan(null, states, 0, 0, 0, output, 0, 0, backend));
        rejects(NullPointerException.class, () ->
                M3IndexMaskWindow.scan(rows, null, 0, 0, 0, output, 0, 0, backend));
        rejects(NullPointerException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, null, 0, 0, backend));
        rejects(NullPointerException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, 0, 0, null));
        for (int invalid : new int[] {-1, 256, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            rejects(IllegalArgumentException.class, () ->
                    M3IndexMaskWindow.scan(rows, states, 0, invalid, 0, output, 0, 0, backend));
            rejects(IllegalArgumentException.class, () ->
                    M3IndexMaskWindow.scanBounded(rows, states, 0, 0, 0, invalid, output, 0, 0, backend));
        }
        rejects(IllegalArgumentException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, rows, 0, 0, backend));
        rejects(IllegalArgumentException.class, () ->
                M3IndexMaskWindow.scanBounded(rows, new byte[2], 0, 0, 0, 0, output, 0, 0, backend));
        rejects(IllegalArgumentException.class, () ->
                M3IndexMaskWindow.scanBounded(rows, states, 0, -1, 0, 0, output, 0, 0, backend));
        rejects(IllegalArgumentException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, 0, 4097, backend));
        rejects(IndexOutOfBoundsException.class, () ->
                M3IndexMaskWindow.scan(rows, states, -1, 0, 0, output, 0, 0, backend));
        rejects(IndexOutOfBoundsException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 4, 0, 0, output, 0, 0, backend));
        rejects(IndexOutOfBoundsException.class, () ->
                M3IndexMaskWindow.scan(rows, states, 0, 0, 0, output, Integer.MAX_VALUE, 1, backend));
        require(Arrays.equals(output, new int[] {GUARD, GUARD, GUARD, GUARD}),
                "public validation wrote output");
        require(Arrays.equals(rows, new int[] {1, 2, 3}), "public validation wrote source alias");
    }

    private static void rawValidation() throws Exception {
        Method scan = M3IndexMaskWindow.class.getDeclaredMethod("scanNative", int[].class,
                byte[].class, int.class, int.class, int.class, int[].class, int.class, int.class);
        Method bounded = M3IndexMaskWindow.class.getDeclaredMethod("scanBoundedNative", int[].class,
                byte[].class, int.class, int.class, int.class, int.class, int[].class, int.class, int.class);
        scan.setAccessible(true);
        bounded.setAccessible(true);
        for (Method method : new Method[] {scan, bounded}) {
            int[] rows = {17, -8, 17};
            byte[] states = {1, (byte) 128, (byte) 255};
            int[] output = {GUARD, GUARD, GUARD, GUARD};
            Object[] valid = method == scan
                    ? new Object[] {rows, states, 0, 0, 0, output, 1, 1}
                    : new Object[] {rows, states, 0, 3, 0, 0, output, 1, 1};
            int maskIndex = method == scan ? 3 : 4;
            int outIndex = maskIndex + 2;
            for (int position : new int[] {0, 1, outIndex}) rawReject(method, valid, position, null);
            rawReject(method, valid, 1, new byte[2]);
            rawReject(method, valid, outIndex, rows);
            for (int mask : new int[] {-1, 256, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
                rawReject(method, valid, maskIndex, mask);
                rawReject(method, valid, maskIndex + 1, mask);
            }
            for (int first : new int[] {-1, 4, Integer.MAX_VALUE}) rawReject(method, valid, 2, first);
            if (method == bounded) {
                for (int end : new int[] {-1, 4, Integer.MAX_VALUE}) rawReject(method, valid, 3, end);
                Object[] reversed = valid.clone();
                reversed[2] = 2;
                rawReject(method, reversed, 3, 1);
            }
            for (int offset : new int[] {-1, 4, Integer.MAX_VALUE}) {
                rawReject(method, valid, outIndex + 1, offset);
            }
            for (int limit : new int[] {-1, 4097, Integer.MAX_VALUE}) {
                rawReject(method, valid, outIndex + 2, limit);
            }
            require(Arrays.equals(rows, new int[] {17, -8, 17}), "raw validation wrote source alias");
            require(Arrays.equals(states, new byte[] {1, (byte) 128, (byte) 255}), "raw validation wrote states");
            require(Arrays.equals(output, new int[] {GUARD, GUARD, GUARD, GUARD}), "raw validation wrote output");
        }
    }

    private static void rawReject(Method method, Object[] valid, int index, Object value) throws Exception {
        Object[] invalid = valid.clone();
        invalid[index] = value;
        try {
            method.invoke(null, invalid);
            throw new AssertionError("raw JNI accepted " + method.getName() + " argument " + index);
        } catch (InvocationTargetException target) {
            require(target.getCause() instanceof IllegalArgumentException,
                    "raw JNI wrong exception: " + target.getCause());
        }
    }

    private static void rejects(Class<? extends Throwable> expected, Runnable call) {
        try {
            call.run();
        } catch (Throwable failure) {
            require(expected.isInstance(failure), "expected " + expected.getName() + ", got " + failure);
            return;
        }
        throw new AssertionError("missing " + expected.getName());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}

