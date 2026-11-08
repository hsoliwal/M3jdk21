// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.tooling.atomjit;

import java.lang.management.ManagementFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;

/** Actual M3IndexMaskCompactor Java-vs-required-native parity; no duplicate JNI backend. */
//FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/b65df53330e3d82f6b2d5509ded971f19883f4ca/synexia-openrewrite-recipes/crates/m3-openjdk-atom-jit-pattern-20261031/src/main/java/com/synexia/m3/qualification/M3OpenJdkIndexMaskJniProbe.java blob c4ef5be4a18d; recipe m3/tooling/atom-jit-gate/recipe/atom-jit-gate-20261106/relocate.py v4
public final class OpenJdkIndexMaskJniProbe {
    private static final int[] LENGTHS = {0, 1, 2, 63, 64, 65, 255, 256, 257, 1023, 1024};
    private static final int[] MASKS = {0, 1, 2, 3, 15, 63, 127, 128, 255};

    private OpenJdkIndexMaskJniProbe() { }

    public static void main(String[] args) throws Throwable {
        if (args.length != 1 || !"--run".equals(args[0])) {
            throw new IllegalArgumentException("Run within the source-built M3JDK21 with -Xcheck:jni: --run");
        }
        if (!ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xcheck:jni")) {
            throw new IllegalStateException("-Xcheck:jni required for native witness");
        }
        // Bind the native witness to exactly the same VM modules and HotSpot
        // implementation as the interpreted/C2 Java witness, not bin/java alone.
        String runtimeRoot = OpenJdkMaskJitProbe.currentRuntimeRoot();
        Class<?> compactor = Class.forName("jdk.m3.collections.M3IndexMaskCompactor");
        requireDeclaredNativeSymbols(compactor);
        Class<?> backend = Class.forName("jdk.m3.collections.M3IndexMaskCompactor$Backend");
        Object javaBackend = null, nativeBackend = null;
        for (Object candidate : backend.getEnumConstants()) {
            if (((Enum<?>) candidate).name().equals("JAVA")) javaBackend = candidate;
            if (((Enum<?>) candidate).name().equals("NATIVE_REQUIRED")) nativeBackend = candidate;
        }
        if (javaBackend == null || nativeBackend == null) throw new IllegalStateException("backend ABI drift");
        Method availability = compactor.getMethod("nativeAvailable");
        if (!Boolean.TRUE.equals(availability.invoke(null))) {
            throw new IllegalStateException("existing m3_collections_mask JNI library not loaded");
        }
        Method select = compactor.getMethod("selectPositions", byte[].class, int.class, backend);
        Method selectMasks = compactor.getMethod(
                "selectPositions", byte[].class, int.class, int.class, backend);
        // Execute the *private C JNI exports* as well as the public required-native
        // selector: a Java-only mock with unused native declarations must fail.
        Method directRequired = compactor.getDeclaredMethod(
                "selectPositionsNative", byte[].class, int.class);
        Method directMasks = compactor.getDeclaredMethod(
                "selectPositionsMasksNative", byte[].class, int.class, int.class);
        if (!directRequired.trySetAccessible() || !directMasks.trySetAccessible()) {
            throw new IllegalStateException("M3 JNI declarations inaccessible for direct proof");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int nativeExcludedRuns = 0;
        for (int run = 0; run < 8192; run++) {
            int length = LENGTHS[run % LENGTHS.length];
            byte[] states = new byte[length];
            for (int i = 0; i < length; i++) {
                int bits = (i * 73 + run * 19) ^ ((i * 17 + run) >>> 3);
                states[i] = (byte) bits;
            }
            byte[] original = states.clone();
            int mask = MASKS[run % MASKS.length];
            int[] javaResult = select(select, states, mask, javaBackend);
            int[] nativeResult = select(select, states, mask, nativeBackend);
            if (!Arrays.equals(javaResult, nativeResult)
                    || !Arrays.equals(javaResult, selectNative(directRequired, states, mask))) {
                throw new AssertionError("Java/public JNI/direct C result differs at run " + run);
            }
            int count = 0;
            for (int i = 0; i < length; i++) {
                if (((states[i] & 0xff) & mask) == mask) count++;
            }
            if (nativeResult.length != count) throw new AssertionError("unexpected native count");
            int j = 0;
            for (int i = 0; i < length; i++) {
                if (((states[i] & 0xff) & mask) == mask && nativeResult[j++] != i) {
                    throw new AssertionError("unexpected selected index");
                }
            }
            if (!Arrays.equals(states, original)) throw new AssertionError("native mutated input");
            // LC705/LC706 bit-mask category: independently compute two-mask semantics.
            // Select disjoint masks so the required-native implementation, rather than
            // the Java overlap short circuit, must execute the additive JNI ABI.
            int excluded = MASKS[(run * 17 + 3) % MASKS.length] & ~mask & 0xff;
            int[] javaMasked = selectMasks(selectMasks, states, mask, excluded, javaBackend);
            int[] nativeMasked = selectMasks(selectMasks, states, mask, excluded, nativeBackend);
            if (!Arrays.equals(javaMasked, nativeMasked)
                    || !Arrays.equals(javaMasked,
                            selectNative(directMasks, states, mask, excluded))) {
                throw new AssertionError("required/excluded Java/public JNI/direct C mismatch at " + run);
            }
            int selected = 0;
            for (int i = 0; i < length; i++) {
                int bits = states[i] & 0xff;
                if ((bits & mask) == mask && (bits & excluded) == 0) {
                    if (selected >= nativeMasked.length || nativeMasked[selected++] != i) {
                        throw new AssertionError("two-mask index or order mismatch at " + run);
                    }
                }
            }
            if (selected != nativeMasked.length || !Arrays.equals(states, original)) {
                throw new AssertionError("two-mask count/input mutation at " + run);
            }
            if (excluded != 0) nativeExcludedRuns++;
            if (run % 64 == 0 && nativeMasked.length > 0) {
                nativeMasked[0] = -1;
                if (!Arrays.equals(javaMasked,
                        selectMasks(selectMasks, states, mask, excluded, nativeBackend))) {
                    throw new AssertionError("two-mask native output aliased input/previous result");
                }
            }
            if (run % 64 == 0 && nativeResult.length > 0) {
                nativeResult[0] = -1;
                if (!Arrays.equals(javaResult, select(select, states, mask, nativeBackend))) {
                    throw new AssertionError("native output retained or aliased");
                }
            }
            digest.update(ByteBuffer.allocate(12).putInt(run).putInt(length).putInt(mask).array());
            for (int value : javaResult) digest.update(ByteBuffer.allocate(4).putInt(value).array());
            digest.update(ByteBuffer.allocate(4).putInt(excluded).array());
            for (int value : javaMasked) digest.update(ByteBuffer.allocate(4).putInt(value).array());
        }
        if (nativeExcludedRuns < 1000) {
            throw new AssertionError("insufficient required/excluded native ABI exercise");
        }
        if (!runtimeRoot.equals(OpenJdkMaskJitProbe.currentRuntimeRoot())) {
            throw new IllegalStateException("JNI target JDK image changed during parity test");
        }
        System.out.println("M3_JNI_RUNTIME_BINARY_SHA256=" + runtimeRoot);
        System.out.println("M3_JNI_RUNTIME_ROOT_VERSION=V2_LAUNCHER_HOTSPOT_MODULES_RELEASE");
        System.out.println("M3_JNI_TWO_MASK_NATIVE_RUNS=" + nativeExcludedRuns);
        System.out.println("M3_JNI_PARITY_SHA256=" + HexFormat.of().formatHex(digest.digest()));
        System.out.println("M3_JNI_BACKEND=NATIVE_REQUIRED");
        System.out.println("M3_JNI_STATUS=JAVA_NATIVE_PARITY_SMOKE_TARGET_GC_ABI_JTREG_PENDING");
    }

    /** Refuse a Java-only or incorrectly attributed backend before recording JNI evidence. */
    static void requireDeclaredNativeSymbols(Class<?> owner) throws NoSuchMethodException {
        requireNative(owner.getDeclaredMethod("positionsAbiNative"), int.class);
        requireNative(owner.getDeclaredMethod("selectPositionsNative",
                byte[].class, int.class), int[].class);
        requireNative(owner.getDeclaredMethod("positionsMasksAbiNative"), int.class);
        requireNative(owner.getDeclaredMethod("selectPositionsMasksNative",
                byte[].class, int.class, int.class), int[].class);
    }

    private static void requireNative(Method method, Class<?> returnType) {
        int flags = method.getModifiers();
        if (!Modifier.isPrivate(flags) || !Modifier.isStatic(flags)
                || !Modifier.isNative(flags) || method.getReturnType() != returnType) {
            throw new IllegalStateException("M3 native entrypoint declaration drift: "
                    + method.getName());
        }
    }

    static int[] selectNative(Method nativeMethod, Object... arguments)
            throws Throwable {
        try {
            return (int[]) nativeMethod.invoke(null, arguments);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static int[] select(Method select, byte[] states, int mask, Object backend)
            throws Throwable {
        try {
            return (int[]) select.invoke(null, states, mask, backend);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }

    private static int[] selectMasks(Method method, byte[] states, int required,
            int excluded, Object backend) throws Throwable {
        try {
            return (int[]) method.invoke(null, states, required, excluded, backend);
        } catch (InvocationTargetException failure) {
            throw failure.getCause();
        }
    }
}
