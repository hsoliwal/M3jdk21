/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.parallel;

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
    private static final KernelId INT_ADD = KernelId.of("m3.parallel.int-add.v1");
    private static final KernelId INT_BITCOUNT = KernelId.of("m3.parallel.int-bitcount.v1");
    private static final JavaKernel INT_ADD_BODY = JavaKernels::addInts;
    private static final JavaKernel INT_BITCOUNT_BODY = JavaKernels::countBits;

    private JavaKernels() {
    }

    /**
     * Registers and returns the canonical integer-add kernel.
     *
     * <p>The three equally sized bindings are left input, right input and output. Each work-item writes one output
     * using Java's 32-bit wrapping addition. Padded work-items do nothing; input buffers are not modified.</p>
     *
     * @return kernel id {@code m3.parallel.int-add.v1}
     * @throws IllegalStateException if a different body already owns the id
     */
    public static KernelId intAdd() {
        return register(INT_ADD, INT_ADD_BODY);
    }

    /**
     * Registers and returns the canonical 32-bit population-count kernel.
     *
     * <p>The two equally sized bindings are input and output. Each work-item writes {@link Integer#bitCount(int)}
     * of its input, including negative two's-complement values. Padded work-items do nothing.</p>
     *
     * @return kernel id {@code m3.parallel.int-bitcount.v1}
     * @throws IllegalStateException if a different body already owns the id
     */
    public static KernelId intBitCount() {
        return register(INT_BITCOUNT, INT_BITCOUNT_BODY);
    }

    private static void addInts(int index, IntBuffer... buffers) {
        if (buffers.length != 3) {
            throw new IllegalArgumentException("integer-add kernel needs three lanes");
        }
        int length = buffers[2].length();
        if (buffers[0].length() != length || buffers[1].length() != length) {
            throw new IllegalArgumentException("integer-add lanes must have equal lengths");
        }
        if (index < length) {
            buffers[2].set(index, buffers[0].get(index) + buffers[1].get(index));
        }
    }

    private static void countBits(int index, IntBuffer... buffers) {
        if (buffers.length != 2) {
            throw new IllegalArgumentException("bitcount needs two int lanes");
        }
        int length = buffers[1].length();
        if (buffers[0].length() != length) {
            throw new IllegalArgumentException("bitcount lanes must have equal lengths");
        }
        if (index < length) {
            buffers[1].set(index, Integer.bitCount(buffers[0].get(index)));
        }
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
