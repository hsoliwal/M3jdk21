/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.collections;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.function.Supplier;

/**
 * Merge-loss phase-2 leaf 50 oracle: drive every JNI export of the four leaf-50 C owners through a fixed,
 * seeded argument lattice and print one line per call (argument vector, return value or exception class and
 * message, post-call hashes of the caller arrays). Run once against libraries built from the incumbent sources
 * and once against the candidate; the two transcripts must be byte-identical.
 *
 * <p>usage: {@code java -Xcheck:jni -cp <classes> jdk.m3.collections.M3Mergeloss50JniThrowDifferential
 * <library dir> <transcript>}
 */
//FLLDCIM: MRL - mergeloss2 leaf 50: exception-behaviour differential for m3_jni_throw_by_name vs local helpers.
public final class M3Mergeloss50JniThrowDifferential {
    private static final int SAMPLES = 4000;
    private static final List<String> LIBRARIES = List.of("m3_collections_mask", "m3_collections_long_path_masks",
            "m3_collections_statehist", "m3bitlane");
    private static final int[] INTS = {-1, 0, 1, 2, 3, 7, 8, 255, 256, 300, 4096, 4097,
            Integer.MAX_VALUE, Integer.MIN_VALUE};

    private M3Mergeloss50JniThrowDifferential() {
    }

    public static void main(String[] args) throws IOException, ReflectiveOperationException {
        Path dir = Path.of(args[0]).toAbsolutePath();
        LIBRARIES.forEach(name -> System.load(dir.resolve(System.mapLibraryName(name)).toString()));
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(args[1]), StandardCharsets.UTF_8))) {
            for (Method method : targets()) {
                method.setAccessible(true);
                drive(method, out);
            }
        }
    }

    private static List<Method> targets() throws NoSuchMethodException {
        return List.of(
                M3IndexMaskWindow.class.getDeclaredMethod("scanNative", int[].class, byte[].class, int.class,
                        int.class, int.class, int[].class, int.class, int.class),
                M3IndexMaskWindow.class.getDeclaredMethod("scanBoundedNative", int[].class, byte[].class,
                        int.class, int.class, int.class, int.class, int[].class, int.class, int.class),
                M3LongIndexPathMasksJNI.class.getDeclaredMethod("selectNative", long[].class, int[].class,
                        int[][].class, int[].class, int[].class, int.class, int.class, int.class),
                M3LongIndexPathMasksJNI.class.getDeclaredMethod("scanWindowNative", long[].class, int[].class,
                        int[][].class, int[].class, int[].class, int.class, int.class, int.class, int.class,
                        long[].class, int.class, int.class),
                M3StateMaskHistogram.class.getDeclaredMethod("tallyNative", byte[].class, int.class, int.class,
                        int.class, int[].class),
                M3StateMaskHistogram.class.getDeclaredMethod("indexedAbiNative"),
                M3StateMaskHistogram.class.getDeclaredMethod("tallyIndexedNative", int[].class, byte[].class,
                        int.class, int.class, int.class, int[].class),
                M3BitLane28.class.getDeclaredMethod("cardinality0", long[][][].class, int.class, int.class));
    }

    private static void drive(Method method, PrintWriter out) {
        Class<?>[] types = method.getParameterTypes();
        SplittableRandom random = new SplittableRandom(method.getName().hashCode());
        int[] choice = new int[types.length];
        for (int sample = 0; sample < SAMPLES; sample++) {
            for (int p = 0; p < types.length; p++) {
                choice[p] = sample < 2 ? sample : random.nextInt(domain(types[p]).length);
            }
            Object[] values = new Object[types.length];
            for (int p = 0; p < types.length; p++) {
                values[p] = domain(types[p])[choice[p]].get();
            }
            out.println(method.getDeclaringClass().getSimpleName() + '.' + method.getName() + ' '
                    + Arrays.toString(choice) + ' ' + invoke(method, values) + ' ' + hashes(values));
        }
    }

    private static String invoke(Method method, Object[] values) {
        try {
            Object result = method.invoke(null, values);
            return "RETURN " + render(result);
        } catch (InvocationTargetException thrown) {
            Throwable cause = thrown.getCause();
            return "THROW " + cause.getClass().getName() + ": " + cause.getMessage();
        } catch (IllegalAccessException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private static String hashes(Object[] values) {
        StringBuilder text = new StringBuilder("H");
        for (Object value : values) {
            text.append(':').append(render(value).hashCode());
        }
        return text.toString();
    }

    private static String render(Object value) {
        if (value instanceof int[] ints) return Arrays.toString(ints);
        if (value instanceof long[] longs) return Arrays.toString(longs);
        if (value instanceof byte[] bytes) return Arrays.toString(bytes);
        if (value instanceof Object[] objects) return Arrays.deepToString(objects);
        return String.valueOf(value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Supplier<Object>[] domain(Class<?> type) {
        if (type == int.class) {
            return Arrays.stream(INTS).mapToObj(v -> (Supplier<Object>) () -> v).toArray(Supplier[]::new);
        }
        if (type == int[].class) {
            return new Supplier[] {() -> null, () -> new int[0], () -> new int[] {0, 1, 2, 3, 4, 5, 6, 7},
                    () -> new int[] {-1, 5, 2, 9}, () -> filledInts(300)};
        }
        if (type == byte[].class) {
            return new Supplier[] {() -> null, () -> new byte[0], () -> new byte[] {1, 3, 0, 7, -1, 2, 5, 1},
                    () -> filledBytes(300)};
        }
        if (type == long[].class) {
            return new Supplier[] {() -> null, () -> new long[0], () -> new long[] {0, 1, 2, 3, 4, 5, 6, 7},
                    () -> filledLongs(300)};
        }
        if (type == int[][].class) {
            return new Supplier[] {() -> null, () -> new int[0][], () -> new int[][] {{1, 2, 3, 0, 1, 2, 3, 0}},
                    () -> new int[][] {null}, () -> new int[][] {{1, 2}, {3, 4}}};
        }
        if (type == long[][][].class) {
            return new Supplier[] {() -> null, () -> new long[0][][],
                    () -> new long[M3Address28.REGION_COUNT][][], M3Mergeloss50JniThrowDifferential::oneRegion};
        }
        throw new AssertionError("no domain for " + type);
    }

    private static long[][][] oneRegion() {
        long[][][] regions = new long[M3Address28.REGION_COUNT][][];
        regions[0] = new long[][] {{-1L, 5L, 0L, 1L << 62}};
        return regions;
    }

    private static int[] filledInts(int length) {
        int[] values = new int[length];
        Arrays.setAll(values, i -> i * 7 % length);
        return values;
    }

    private static byte[] filledBytes(int length) {
        byte[] values = new byte[length];
        for (int i = 0; i < length; i++) values[i] = (byte) (i * 37);
        return values;
    }

    private static long[] filledLongs(int length) {
        long[] values = new long[length];
        Arrays.setAll(values, i -> i);
        return values;
    }
}
