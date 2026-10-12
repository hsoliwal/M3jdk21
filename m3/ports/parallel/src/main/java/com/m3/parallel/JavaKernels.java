/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Process-wide registry of {@link JavaKernel} bodies used by the CPU reference backend.
 *
 * <p>Registration is idempotent for the same kernel instance and refuses to replace a different instance, so a kernel
 * id has exactly one Java meaning per process.</p>
 */
public final class JavaKernels {
    private static final ConcurrentMap<KernelId, JavaKernel> KERNELS = new ConcurrentHashMap<>();

    private JavaKernels() {
    }

    /**
     * Registers {@code kernel} under {@code id}.
     *
     * @param id     kernel id
     * @param kernel kernel body
     * @return {@code id}
     * @throws IllegalStateException if a different kernel instance is already registered under {@code id}
     */
    public static KernelId register(KernelId id, JavaKernel kernel) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kernel, "kernel");
        JavaKernel previous = KERNELS.putIfAbsent(id, kernel);
        if (previous != null && previous != kernel) {
            throw new IllegalStateException("a different Java kernel is already registered for " + id);
        }
        return id;
    }

    /**
     * Looks up the Java body of a kernel.
     *
     * @param id kernel id
     * @return the registered body, or empty
     */
    public static Optional<JavaKernel> lookup(KernelId id) {
        return Optional.ofNullable(KERNELS.get(Objects.requireNonNull(id, "id")));
    }

    /**
     * Returns the registered ids in name order.
     *
     * @return an unmodifiable snapshot
     */
    public static List<KernelId> registered() {
        return KERNELS.keySet().stream().sorted(Comparator.comparing(KernelId::name)).toList();
    }
}
