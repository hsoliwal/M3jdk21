/* SPDX-License-Identifier: Apache-2.0 */
package jdk.m3.parallel;

import jdk.m3.parallel.spi.BackendProvider;
import jdk.m3.parallel.spi.DeviceProgram;
import jdk.m3.parallel.spi.Probe;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Handle of an execution backend.
 *
 * <p>The pure-Java CPU reference ({@value #REFERENCE_ID}) is always present. Other backends are
 * discovered once per
 * process through {@link java.util.ServiceLoader} and {@link BackendProvider}; a provider whose
 * probe fails, throws, or
 * whose id is invalid, reserved or ambiguous is recorded as unavailable and never selected.
 * Discovered backends have
 * one instance per id. {@link #fromProvider(BackendProvider)} creates an independent, explicitly
 * supplied backend
 * without changing process-wide discovery. Backends are compared by identity.</p>
 */
public final class Backend {
    /** Id of the built-in pure-Java CPU reference backend. */
    public static final String REFERENCE_ID = "java";

    private static final Pattern PROVIDER_ID = Pattern.compile("[a-z0-9][a-z0-9._-]{0,63}");

    /** Backend selection policy. */
    public enum Policy {
        /** Always the pure-Java CPU reference. */
        REFERENCE,
        /** The available backend with the highest score; the CPU reference wins ties. */
        BEST,
        /**
         * The available non-reference backend with the highest score; fails when none is
         * available.
         */
        ACCELERATOR
    }

    /** Static backend capabilities. */
    public enum Capability {
        /** The parity oracle: the pure-Java CPU reference. */
        REFERENCE,
        /** Executes on host CPU cores. */
        CPU,
        /** Executes on a GPU or other accelerator device. */
        GPU,
        /** Executes native code through JNI. */
        NATIVE,
        /** Executes work-items on more than one thread. */
        MULTI_THREADED
    }

    private final String id;
    private final BackendProvider provider;
    private final Set<Capability> capabilities;
    private final int score;

    Backend(String id, BackendProvider provider, Set<Capability> capabilities, int score) {
        this.id = Objects.requireNonNull(id, "id");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.capabilities = Collections.unmodifiableSet(capabilities.isEmpty()
                ? EnumSet.noneOf(Capability.class) : EnumSet.copyOf(capabilities));
        this.score = score;
    }

    /**
     * Returns the pure-Java CPU reference backend.
     *
     * @return the reference backend
     */
    public static Backend reference() {
        return BackendRegistry.REFERENCE;
    }

    /**
     * Returns the available backend with the given id.
     *
     * @param id backend id, for example {@value #REFERENCE_ID}, {@code "cpu"}, {@code "opencl"}
     * or {@code "cuda"}
     * @return the backend
     * @throws IllegalArgumentException if no provider declares {@code id}
     * @throws IllegalStateException    if the provider exists but is unavailable
     */
    public static Backend of(String id) {
        return BackendRegistry.byId(Objects.requireNonNull(id, "id"));
    }

    /**
     * Creates an independent backend from an explicitly supplied engine provider.
     *
     * <p>The provider's id, capabilities and score are snapshotted, and its runtime must be
     * available. This method
     * neither installs nor replaces a process-wide provider. Choose the returned backend when
     * building a new
     * {@link Plan}; already prepared plans retain their original backend and device storage. The
     * caller owns any
     * provider-wide resources; each plan owns and closes only its prepared device program.</p>
     *
     * @param provider stateless provider factory
     * @return a backend for this provider
     * @throws NullPointerException if the provider or its capabilities are null
     * @throws IllegalArgumentException if the id is invalid or reserved, or the provider claims
     * the reference role
     * @throws IllegalStateException if the runtime is unavailable or its probe fails
     */
    public static Backend fromProvider(BackendProvider provider) {
        Objects.requireNonNull(provider, "provider");
        String id = provider.id();
        if (!validProviderId(id)) {
            throw new IllegalArgumentException("invalid backend id: " + id);
        }
        if (REFERENCE_ID.equals(id)) {
            throw new IllegalArgumentException(
                    "backend id " + id + " is reserved for the CPU reference");
        }
        Set<Capability> capabilities = Set.copyOf(provider.capabilities());
        if (capabilities.contains(Capability.REFERENCE)) {
            throw new IllegalArgumentException(
                    "the reference capability is reserved for the CPU reference");
        }
        int score = provider.score();
        Probe probe;
        try {
            probe = Objects.requireNonNull(provider.probe(), "probe");
        } catch (RuntimeException | LinkageError failure) {
            throw new IllegalStateException("backend " + id + " probe failed", failure);
        }
        if (!probe.available()) {
            throw new IllegalStateException("backend " + id + " is unavailable: " + probe.reason());
        }
        return new Backend(id, provider, capabilities, score);
    }

    static boolean validProviderId(String id) {
        return id != null && PROVIDER_ID.matcher(id).matches();
    }

    /**
     * Selects a backend by policy.
     *
     * @param policy selection policy
     * @return the selected backend
     * @throws IllegalStateException if {@link Policy#ACCELERATOR} finds no available accelerator
     */
    public static Backend select(Policy policy) {
        return BackendRegistry.select(Objects.requireNonNull(policy, "policy"));
    }

    /**
     * Selects an available backend that supports the requested kernel.
     *
     * <p>Preference and tie-breaking are identical to {@link #select(Policy)}, after excluding
     * unsupported kernels.
     * Selection happens before plan preparation. A later preparation or execution failure is
     * reported to the caller;
     * it never retries the operation on another backend.</p>
     *
     * @param policy selection policy
     * @param kernel required kernel
     * @return the selected backend
     * @throws IllegalStateException if no backend allowed by the policy supports the kernel
     */
    public static Backend select(Policy policy, KernelId kernel) {
        return BackendRegistry.select(Objects.requireNonNull(policy, "policy"),
                Objects.requireNonNull(kernel, "kernel"));
    }

    /**
     * Returns the probe result of every discovered provider, keyed by id (or by a diagnostic key
     * for providers that
     * could not be loaded or named), in key order.
     *
     * @return an unmodifiable sorted map
     */
    public static Map<String, Probe> probeAll() {
        return BackendRegistry.probes();
    }

    /**
     * Returns the backend id.
     *
     * @return the id
     */
    public String id() {
        return id;
    }

    /**
     * Returns the static capabilities.
     *
     * @return an unmodifiable set
     */
    public Set<Capability> capabilities() {
        return capabilities;
    }

    /**
     * Returns the selection score.
     *
     * @return score; the CPU reference scores {@code 0}
     */
    public int score() {
        return score;
    }

    /**
     * Reports whether this is the pure-Java CPU reference.
     *
     * @return {@code true} for the reference backend
     */
    public boolean isReference() {
        return provider instanceof ReferenceProvider;
    }

    /**
     * Reports whether the backend can execute {@code kernel}.
     *
     * @param kernel kernel id
     * @return {@code true} when a plan for {@code kernel} can be built on this backend
     */
    public boolean supports(KernelId kernel) {
        return provider.supports(Objects.requireNonNull(kernel, "kernel"));
    }

    DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] bufferLengths) {
        return Objects.requireNonNull(
                provider.prepare(kernel, grid, bufferLengths.clone()), "device program");
    }

    @Override
    public String toString() {
        return "Backend[" + id + "]";
    }
}
