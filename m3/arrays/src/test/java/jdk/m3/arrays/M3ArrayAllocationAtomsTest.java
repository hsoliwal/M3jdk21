// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.arrays;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Contract tests for each allocation atom in the original Java/JNI array owner. */
public final class M3ArrayAllocationAtomsTest {
    private record Kind(String suffix, Class<?> component, Object nonzero) {}
    private static final List<Kind> KINDS = List.of(
            new Kind("Objects", Object.class, new Object()),
            new Kind("Booleans", boolean.class, true),
            new Kind("Bytes", byte.class, (byte) -128),
            new Kind("Chars", char.class, '\uffff'),
            new Kind("Shorts", short.class, Short.MIN_VALUE),
            new Kind("Ints", int.class, Integer.MIN_VALUE),
            new Kind("Longs", long.class, Long.MIN_VALUE),
            new Kind("Floats", float.class, Float.NaN),
            new Kind("Doubles", double.class, Double.NaN));

    @BeforeAll static void requireActualLibrary() {
        String path = System.getProperty("m3.arrays.native.path");
        if (path == null || path.isBlank()) {
            assertFalse(Boolean.getBoolean("m3.arrays.native.required"), "required JNI path missing");
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "optional native profile is disabled");
        }
        assertNotNull(path, "allocation-atom tests require the actual JNI library");
        M3ArrayNative.load(Path.of(path));
        assertTrue(M3ArrayNative.isLoaded());
    }

    @Test void objects() throws Throwable { checkKind(KINDS.get(0)); }
    @Test void booleans() throws Throwable { checkKind(KINDS.get(1)); }
    @Test void bytes() throws Throwable { checkKind(KINDS.get(2)); }
    @Test void chars() throws Throwable { checkKind(KINDS.get(3)); }
    @Test void shorts() throws Throwable { checkKind(KINDS.get(4)); }
    @Test void ints() throws Throwable { checkKind(KINDS.get(5)); }
    @Test void longs() throws Throwable { checkKind(KINDS.get(6)); }
    @Test void floats() throws Throwable { checkKind(KINDS.get(7)); }
    @Test void doubles() throws Throwable { checkKind(KINDS.get(8)); }

    private static void checkKind(Kind kind) throws Throwable {
        Method allocate = publicAllocator(kind);
        assertEquals(Array.newInstance(kind.component(), 0).getClass(), allocate.getReturnType());
        Method nativeMethod = assertDoesNotThrow(() -> M3ArrayNative.class.getDeclaredMethod(
                "nativeAllocate" + kind.suffix(), int.class));
        assertTrue(Modifier.isNative(nativeMethod.getModifiers()));
        assertTrue(Modifier.isPrivate(nativeMethod.getModifiers()));
        nativeMethod.setAccessible(true);
        for (int length : new int[] {0, 1, 16, 1024, 4097}) {
            Object first = invoke(allocate, length);
            Object second = invoke(allocate, length);
            assertNotSame(first, second, "even empty arrays have independent identity");
            assertEquals(kind.component(), first.getClass().getComponentType());
            assertEquals(length, Array.getLength(first));
            assertEquals(length, Array.getLength(second));
            for (int i = 0; i < length; i++) {
                assertZero(first, i, kind);
                assertZero(second, i, kind);
            }
            if (length != 0) {
                Array.set(first, 0, kind.nonzero());
                Array.set(first, length - 1, kind.nonzero());
                assertEquals(kind.nonzero(), Array.get(first, 0));
                assertEquals(kind.nonzero(), Array.get(first, length - 1));
                assertZero(second, 0, kind);
                assertZero(second, length - 1, kind);
            }
        }
        for (int negative : new int[] {-1, Integer.MIN_VALUE}) {
            assertThrows(NegativeArraySizeException.class, () -> invoke(allocate, negative));
            assertThrows(NegativeArraySizeException.class, () -> invoke(nativeMethod, negative));
        }
    }

    private static void assertZero(Object array, int index, Kind kind) {
        Object value = Array.get(array, index);
        if (kind.component() == Object.class) assertNull(value);
        else if (kind.component() == boolean.class) assertEquals(false, value);
        else if (kind.component() == char.class) assertEquals('\0', value);
        else if (kind.component() == float.class) assertEquals(0, Float.floatToRawIntBits((Float) value));
        else if (kind.component() == double.class) assertEquals(0L, Double.doubleToRawLongBits((Double) value));
        else assertEquals(0L, ((Number) value).longValue());
    }

    private static Method publicAllocator(Kind kind) {
        return assertDoesNotThrow(() -> M3ArrayNative.class.getMethod(
                "allocate" + kind.suffix() + "Required", int.class),
                "missing primitive allocation atom: " + kind.suffix());
    }

    private static Object invoke(Method method, int length) throws Throwable {
        try { return method.invoke(null, length); }
        catch (InvocationTargetException failure) { throw failure.getCause(); }
    }

    @Test void unavailableLibraryAndNegativePrecedenceInFreshVm() throws Exception { runChild("missing"); }
    @Test void configuredLibraryInitializesConcurrentlyInFreshVm() throws Exception { runChild("concurrent"); }
    @Test void realNativeAllocationExceptionsInBoundedHeap() throws Exception { runChild("oom"); }
    @Test void silentNativeNullIsNeverPublished() throws Exception { runChild("silent-null"); }

    private static void runChild(String mode) throws Exception {
        Path log = Files.createTempFile("m3-array-atoms-", ".log");
        var args = new ArrayList<String>();
        args.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        args.add("-Xmx32m");
        args.add("-Xcheck:jni");
        if (!mode.equals("missing")) {
            Path library = Path.of(System.getProperty("m3.arrays.native.path"));
            if (mode.equals("silent-null")) library = library.getParent().resolve(
                    System.mapLibraryName("m3_array_allocation_null_fixture"));
            args.add("-Dm3.arrays.native.path=" + library);
        }
        args.add("-cp");
        args.add(System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")));
        args.add(M3ArrayAllocationAtomsTest.class.getName());
        args.add(mode);
        Process process = new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "fresh-VM probe timed out");
            String output = Files.readString(log);
            assertEquals(0, process.exitValue(), output);
            assertEquals("PASS " + mode + " 9 allocation atoms", output.strip());
        } finally {
            if (process.isAlive()) process.destroyForcibly();
            Files.deleteIfExists(log);
        }
    }

    public static void main(String[] args) throws Throwable {
        assertFalse(M3ArrayNative.isLoaded());
        switch (args[0]) {
            case "missing" -> {
                System.clearProperty("m3.arrays.native.path");
                for (Kind kind : KINDS) {
                    Method method = publicAllocator(kind);
                    assertThrows(NegativeArraySizeException.class, () -> invoke(method, -1));
                    assertFalse(M3ArrayNative.isLoaded());
                    assertThrows(UnsatisfiedLinkError.class, () -> invoke(method, 0));
                    assertFalse(M3ArrayNative.isLoaded());
                }
            }
            case "oom", "silent-null" -> {
                for (Kind kind : KINDS) {
                    Method method = publicAllocator(kind);
                    int length = args[0].equals("oom") ? Integer.MAX_VALUE : 17;
                    assertThrows(OutOfMemoryError.class, () -> invoke(method, length));
                }
            }
            case "concurrent" -> {
                try (var workers = Executors.newFixedThreadPool(4)) {
                    List<Callable<Object>> calls = new ArrayList<>();
                    for (int round = 0; round < 8; round++) for (Kind kind : KINDS) calls.add(() -> {
                        try { return invoke(publicAllocator(kind), 17); }
                        catch (Throwable failure) { throw new AssertionError(failure); }
                    });
                    var results = workers.invokeAll(calls);
                    var identities = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Object, Boolean>());
                    for (var result : results) {
                        Object array = result.get();
                        assertEquals(17, Array.getLength(array));
                        assertTrue(identities.add(array));
                    }
                }
                assertTrue(M3ArrayNative.isLoaded());
            }
            default -> throw new AssertionError(args[0]);
        }
        System.out.println("PASS " + args[0] + " 9 allocation atoms");
    }
}
