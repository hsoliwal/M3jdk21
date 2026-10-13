/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import com.m3.parallel.spi.DeviceProgram;

import java.util.Arrays;

/**
 * Device program of the CPU reference. "Device" storage is a separate set of off-heap buffers, so transfer policies
 * behave exactly as on a discrete device: host writes are invisible to the kernel until uploaded.
 */
final class ReferenceProgram implements DeviceProgram {
    private final JavaKernel kernel;
    private final int launchSize;
    private final IntBuffer[] device;

    ReferenceProgram(JavaKernel kernel, Grid1D grid, int[] bufferLengths) {
        this.kernel = kernel;
        this.launchSize = grid.launchSize();
        this.device = Arrays.stream(bufferLengths).mapToObj(IntBuffer::allocate).toArray(IntBuffer[]::new);
    }

    @Override
    public void upload(int slot, IntBuffer host) {
        device[slot].copyFrom(host);
    }

    @Override
    public void download(int slot, IntBuffer host) {
        host.copyFrom(device[slot]);
    }

    @Override
    public void launch() {
        IntBuffer[] buffers = device;
        JavaKernel body = kernel;
        ParallelFor.run(launchSize, index -> body.run(index, buffers));
    }

    @Override
    public void close() {
        // Off-heap storage is released by the garbage collector together with this program.
    }
}
