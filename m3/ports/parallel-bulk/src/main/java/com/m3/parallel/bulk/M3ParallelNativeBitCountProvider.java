/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 */
package com.m3.parallel.bulk;

import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkExecutorProvider;
import jdk.internal.vm.parallel.BulkTask;

/**
 * Required C11/JNI CPU provider for the reviewed signed-32-bit population-count operation.
 *
 * <p>A native request never silently falls back to Java. Every native result is verified
 * against independent {@link Integer#bitCount(int)} over one defensive input snapshot.
 * This is a CPU-native provider, not a GPU backend or a speedup assertion.</p>
 */
public final class M3ParallelNativeBitCountProvider implements BulkExecutorProvider {
    public static final String OPERATION_ID = "m3.parallel.int-bitcount.v1";
    public static final String LIBRARY_PROPERTY = "m3.parallel.bitcount.native.path";
    public static final String INPUT = "bits";
    public static final String OUTPUT = "counts";
    public static final int MAX_ITEMS = 1 << 18;
    private static final String PROVIDER_ID = "m3-bitcount-jni-cpu";
    private static final String RECEIPT_DOMAIN = "M3_NATIVE_BITCOUNT_VERIFIED_V1";
    private static String loadedPath;

    public M3ParallelNativeBitCountProvider() {
        String configured = System.getProperty(LIBRARY_PROPERTY, "").strip();
        if (configured.isEmpty()) {
            throw new UnsatisfiedLinkError("required JNI bitcount library path is not configured");
        }
        Path absolute = Path.of(configured).toAbsolutePath().normalize();
        synchronized (M3ParallelNativeBitCountProvider.class) {
            if (loadedPath == null) {
                System.load(absolute.toString());
                if (nativeAbiVersion() != 1) {
                    throw new UnsatisfiedLinkError("JNI bitcount ABI version mismatch");
                }
                loadedPath = absolute.toString();
            } else if (!loadedPath.equals(absolute.toString())) {
                throw new IllegalStateException("JNI bitcount library changed within one class loader");
            }
        }
    }

    @Override public String id() { return PROVIDER_ID; }
    @Override public int priority() { return 30; }

    @Override
    public boolean supports(BulkTask task) {
        return task != null
                && OPERATION_ID.equals(task.operationId())
                && task.workItems() >= 1 && task.workItems() <= MAX_ITEMS
                && task.localWork() == 0
                && task.inputs().equals(List.of(INPUT))
                && task.outputs().equals(List.of(OUTPUT));
    }

    @Override
    public BulkExecution execute(BulkTask task, Map<String, Object> values) {
        if (!supports(task)) throw new IllegalArgumentException("unadmitted native bitcount task");
        Objects.requireNonNull(values, "values");
        if (!values.keySet().equals(Set.of(INPUT)) || !(values.get(INPUT) instanceof int[] raw)) {
            throw new IllegalArgumentException("native bitcount input schema");
        }
        int size = Math.toIntExact(task.workItems());
        if (raw.length != size) throw new IllegalArgumentException("native bitcount input length");
        int[] input = raw.clone();
        int[] actual = nativeBitCount(input);
        if (actual == null || actual.length != size) {
            throw new IllegalStateException("native bitcount output shape mismatch");
        }
        for (int index = 0; index < size; index++) {
            if (actual[index] != Integer.bitCount(input[index])) {
                throw new IllegalStateException("native bitcount mismatch at index " + index);
            }
        }
        return new BulkExecution(Map.of(OUTPUT, actual), id(), receipt(input, actual));
    }

    private static String receipt(int[] input, int[] result) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new ExceptionInInitializerError(impossible);
        }
        byte[] domain = RECEIPT_DOMAIN.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, domain.length);
        digest.update(domain);
        updateInt(digest, input.length);
        updateValues(digest, input);
        updateValues(digest, result);
        return java.util.HexFormat.of().formatHex(digest.digest());
    }

    private static void updateValues(MessageDigest digest, int[] values) {
        byte[] buffer = new byte[4096];
        int position = 0;
        for (int value : values) {
            buffer[position++] = (byte) (value >>> 24);
            buffer[position++] = (byte) (value >>> 16);
            buffer[position++] = (byte) (value >>> 8);
            buffer[position++] = (byte) value;
            if (position == buffer.length) {
                digest.update(buffer);
                position = 0;
            }
        }
        if (position != 0) digest.update(buffer, 0, position);
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update((byte) (value >>> 24));
        digest.update((byte) (value >>> 16));
        digest.update((byte) (value >>> 8));
        digest.update((byte) value);
    }

    private static native int nativeAbiVersion();
    private static native int[] nativeBitCount(int[] input);
}
