/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel.spi;

import com.m3.parallel.Backend;
import com.m3.parallel.Grid1D;
import com.m3.parallel.KernelId;

import java.util.Set;

/**
 * Service-provider interface of a backend, discovered with {@link java.util.ServiceLoader}.
 *
 * <p>A provider is a stateless factory. {@link #probe()} must never crash the caller: a missing driver or runtime is
 * reported as {@link Probe#unavailable(String)}. Ids are lower-case ({@code [a-z0-9][a-z0-9._-]{0,63}}); the id
 * {@value com.m3.parallel.Backend#REFERENCE_ID} is reserved for the built-in CPU reference.</p>
 */
public interface BackendProvider {

    /**
     * Returns the stable backend id, for example {@code "cpu"}, {@code "opencl"} or {@code "cuda"}.
     *
     * @return the id
     */
    String id();

    /**
     * Returns the static capabilities of this backend.
     *
     * @return capability set
     */
    Set<Backend.Capability> capabilities();

    /**
     * Returns the preference used by {@link Backend.Policy#BEST}; higher wins. The CPU reference scores {@code 0}.
     *
     * @return score
     */
    int score();

    /**
     * Probes the runtime. Must not throw; any thrown exception is treated as unavailability.
     *
     * @return availability and reason
     */
    Probe probe();

    /**
     * Reports whether the backend has an executable kernel for {@code kernel}.
     *
     * @param kernel kernel id
     * @return {@code true} when {@link #prepare} accepts it
     */
    boolean supports(KernelId kernel);

    /**
     * Allocates zero-filled device storage for each binding and loads the kernel.
     *
     * @param kernel        kernel id
     * @param grid          launch grid
     * @param bufferLengths element count of each binding, in binding order
     * @return a device program owned by one plan
     */
    DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] bufferLengths);
}
