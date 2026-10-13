/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import com.m3.parallel.spi.BackendProvider;
import com.m3.parallel.spi.DeviceProgram;
import com.m3.parallel.spi.Probe;

import java.util.EnumSet;
import java.util.Set;

/** Built-in provider of the pure-Java CPU reference backend: executes {@link JavaKernels} entries. */
final class ReferenceProvider implements BackendProvider {

    @Override
    public String id() {
        return Backend.REFERENCE_ID;
    }

    @Override
    public Set<Backend.Capability> capabilities() {
        return EnumSet.of(Backend.Capability.REFERENCE, Backend.Capability.CPU, Backend.Capability.MULTI_THREADED);
    }

    @Override
    public int score() {
        return 0;
    }

    @Override
    public Probe probe() {
        return Probe.ready();
    }

    @Override
    public boolean supports(KernelId kernel) {
        return JavaKernels.lookup(kernel).isPresent();
    }

    @Override
    public DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] bufferLengths) {
        JavaKernel body = JavaKernels.lookup(kernel)
                .orElseThrow(() -> new IllegalArgumentException("no Java kernel registered for " + kernel));
        return new ReferenceProgram(body, grid, bufferLengths);
    }
}
