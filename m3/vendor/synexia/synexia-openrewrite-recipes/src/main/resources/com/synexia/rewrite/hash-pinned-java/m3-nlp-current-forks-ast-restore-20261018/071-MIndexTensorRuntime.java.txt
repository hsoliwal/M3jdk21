// SPDX-License-Identifier: Apache-2.0
package com.synexia.mindex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Backend selector for MIndex tensor kernels.
 *
 * <p>Built-ins are {@code java} and {@code jni}. External providers (for example a LiteRT NPU
 * adapter) are discovered with {@link ServiceLoader}. Auto mode prefers JNI when the native MIndex
 * library is loaded, then Java; external hardware providers are selected explicitly by id so a
 * deployment never silently changes its accelerator semantics.</p>
 */
public final class MIndexTensorRuntime {
    public static final String BACKEND_PROPERTY = "synexia.mindex.tensor.backend";
    private static final MIndexTensorBackend JAVA = new JavaBackend();
    private static final MIndexTensorBackend JNI = new JniBackend();

    private final MIndexTensorBackend backend;

    private MIndexTensorRuntime(MIndexTensorBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
    }

    public static MIndexTensorRuntime system() {
        String requested =
                System.getProperty(BACKEND_PROPERTY, "auto").strip().toLowerCase(Locale.ROOT);
        if (requested.equals("auto")) {
            return new MIndexTensorRuntime(JNI.available() ? JNI : JAVA);
        }
        return forBackend(requested);
    }

    public static MIndexTensorRuntime forBackend(String backendId) {
        String wanted = normalizeId(backendId);
        if (wanted.equals(JAVA.id())) return new MIndexTensorRuntime(JAVA);
        if (wanted.equals(JNI.id())) {
            if (!JNI.available()) {
                throw new IllegalStateException("JNI tensor backend is unavailable: " + MIndexNativeOps.status());
            }
            return new MIndexTensorRuntime(JNI);
        }

        for (MIndexTensorBackend candidate : externalBackends()) {
            if (normalizeId(candidate.id()).equals(wanted)) {
                if (!candidate.available()) {
                    throw new IllegalStateException("Tensor backend is unavailable: " + wanted);
                }
                return new MIndexTensorRuntime(candidate);
            }
        }
        throw new IllegalArgumentException("Unknown MIndex tensor backend: " + backendId);
    }

    public String backendId() {
        return backend.id();
    }

    public int[] multiply(
            MIndexInt8MatmulPlan plan,
            byte[] leftRowMajor,
            int rows,
            int[] bias) {
        Objects.requireNonNull(plan, "plan");
        plan.validateExecution(leftRowMajor, rows, bias);
        return backend.multiplyInt8(plan, leftRowMajor, rows, bias);
    }

    public static List<String> availableBackendIds() {
        ArrayList<String> ids = new ArrayList<>();
        ids.add(JAVA.id());
        if (JNI.available()) ids.add(JNI.id());
        for (MIndexTensorBackend backend : externalBackends()) {
            if (backend.available()) ids.add(normalizeId(backend.id()));
        }
        return ids.stream().distinct().sorted().toList();
    }

    private static List<MIndexTensorBackend> externalBackends() {
        ArrayList<MIndexTensorBackend> result = new ArrayList<>();
        try {
            ServiceLoader.load(MIndexTensorBackend.class).forEach(result::add);
        } catch (ServiceConfigurationError invalidProvider) {
            throw new IllegalStateException("Invalid MIndex tensor backend provider", invalidProvider);
        }
        result.sort(Comparator.comparing(backend -> normalizeId(backend.id())));
        return List.copyOf(result);
    }

    private static String normalizeId(String value) {
        String id = Objects.requireNonNull(value, "backendId").strip().toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9][a-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException("Invalid MIndex tensor backend id: " + value);
        }
        return id;
    }

    private static final class JavaBackend implements MIndexTensorBackend {
        @Override
        public String id() {
            return "java";
        }

        @Override
        public boolean available() {
            return true;
        }

        @Override
        public int[] multiplyInt8(
                MIndexInt8MatmulPlan plan,
                byte[] left,
                int rows,
                int[] bias) {
            return plan.referenceMultiply(left, rows, bias);
        }
    }

    private static final class JniBackend implements MIndexTensorBackend {
        @Override
        public String id() {
            return "jni";
        }

        @Override
        public boolean available() {
            return MIndexNativeOps.available();
        }

        @Override
        public int[] multiplyInt8(
                MIndexInt8MatmulPlan plan,
                byte[] left,
                int rows,
                int[] bias) {
            return MIndexNativeOps.matmulInt8Packed(plan, left, rows, bias);
        }
    }
}
