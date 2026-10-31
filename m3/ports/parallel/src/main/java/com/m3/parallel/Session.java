/* SPDX-License-Identifier: Apache-2.0 */
package com.m3.parallel;

import com.m3.parallel.spi.DeviceProgram;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Device-side state of one {@link Plan}: the backend's {@link DeviceProgram}, the launch counter and the open flag.
 *
 * <p>Created only by {@link Plan.Builder#build()}. Every operation is serialized on this object. Equality is
 * identity.</p>
 */
public final class Session {
    private final Backend backend;
    private final KernelId kernel;
    private final Grid1D grid;
    private final List<Plan.Binding> bindings;
    private final DeviceProgram program;
    private long launches;
    private boolean open = true;

    Session(Backend backend, KernelId kernel, Grid1D grid, List<Plan.Binding> bindings) {
        this.backend = backend;
        this.kernel = kernel;
        this.grid = grid;
        this.bindings = List.copyOf(bindings);
        this.program = backend.prepare(kernel, grid,
                this.bindings.stream().mapToInt(binding -> binding.buffer().length()).toArray());
    }

    /**
     * Returns the number of completed launches.
     *
     * @return launch count
     */
    public synchronized long launches() {
        return launches;
    }

    /**
     * Reports whether device storage is still held.
     *
     * @return {@code true} until closed
     */
    public synchronized boolean isOpen() {
        return open;
    }

    boolean preparedFor(Backend backend, KernelId kernel, Grid1D grid, List<Plan.Binding> bindings) {
        return this.backend == backend && this.kernel.equals(kernel) && this.grid.equals(grid)
                && this.bindings.equals(bindings);
    }

    synchronized Result execute(Plan plan) {
        requireOpen();
        if (launches == 0) {
            uploadAll(Transfer.ONCE);
        }
        uploadAll(Transfer.EACH_LAUNCH);
        program.launch();
        launches++;
        return new Result(plan, launches);
    }

    synchronized void download(long launch, IntBuffer[] buffers) {
        requireCurrent(launch);
        int[] slots = slotsOf(buffers);
        IntStream.range(0, slots.length).forEach(i -> program.download(slots[i], buffers[i]));
    }

    synchronized void upload(long launch, IntBuffer buffer) {
        requireCurrent(launch);
        program.upload(slotOf(buffer), buffer);
    }

    synchronized void close() {
        if (open) {
            open = false;
            program.close();
        }
    }

    private void uploadAll(Transfer transfer) {
        IntStream.range(0, bindings.size())
                .filter(slot -> bindings.get(slot).transfer() == transfer)
                .forEach(slot -> program.upload(slot, bindings.get(slot).buffer()));
    }

    private void requireOpen() {
        if (!open) {
            throw new IllegalStateException("plan is closed");
        }
    }

    private void requireCurrent(long launch) {
        requireOpen();
        if (launch != launches) {
            throw new IllegalStateException("stale result: launch " + launch + " was superseded by launch " + launches);
        }
    }

    private int[] slotsOf(IntBuffer[] buffers) {
        return Arrays.stream(buffers).mapToInt(this::slotOf).toArray();
    }

    private int slotOf(IntBuffer buffer) {
        return IntStream.range(0, bindings.size())
                .filter(slot -> bindings.get(slot).buffer() == buffer)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("buffer is not bound to this plan: " + buffer));
    }
}
