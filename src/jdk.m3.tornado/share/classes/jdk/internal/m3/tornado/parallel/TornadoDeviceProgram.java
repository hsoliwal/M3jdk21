/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.tornado.parallel;

import java.lang.foreign.Arena;
import java.util.Arrays;
import java.util.Objects;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.spi.DeviceProgram;
import uk.ac.manchester.tornado.api.GridScheduler;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.TornadoExecutionResult;
import uk.ac.manchester.tornado.api.WorkerGrid1D;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.exceptions.TornadoExecutionPlanException;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.arrays.TornadoNativeArray;

/** One persistent Tornado plan and its independently owned native staging buffers. */
final class TornadoDeviceProgram implements DeviceProgram {
    private static final String GRAPH = "m3parallel";
    private static final String TASK = "execute";

    private final Arena arena;
    private final IntArray[] arrays;
    private TornadoExecutionPlan plan;
    private TornadoExecutionResult lastResult;
    private boolean executionStarted;
    private boolean open = true;

    // Tornado inspects serializable method references to these internal kernels.
    // The references remain private to this program's task graph.
    @SuppressWarnings("serial")
    TornadoDeviceProgram(KernelId kernel, Grid1D grid, int[] lengths, TornadoDevice device) {
        validateShape(kernel, grid, lengths);
        Objects.requireNonNull(device, "device");
        long header = TornadoNativeArray.ARRAY_HEADER;
        if (header < Long.BYTES || header > Integer.MAX_VALUE || header % Integer.BYTES != 0) {
            throw new IllegalArgumentException(
                    "unsupported Tornado native-array header size: " + header);
        }

        Arena allocation = Arena.ofShared();
        IntArray[] storage = new IntArray[lengths.length];
        TornadoExecutionPlan created = null;
        try {
            for (int slot = 0; slot < storage.length; slot++) {
                long bytes = Math.addExact(
                        header, Math.multiplyExact((long) lengths[slot], Integer.BYTES));
                storage[slot] = IntArray.fromSegmentShallow(allocation.allocate(bytes, Long.BYTES));
            }
            // Arena storage starts at zero. Inputs uploaded before the first
            // launch replace that staging image. FIRST_EXECUTION initializes
            // every device slot once, including ON_DEMAND bindings whose host
            // contents must not be copied implicitly.
            TaskGraph graph = new TaskGraph(GRAPH)
                    .transferToDevice(DataTransferMode.FIRST_EXECUTION, (Object[]) storage);
            if (TornadoBackendProvider.INT_ADD.equals(kernel.name())) {
                graph.task(TASK, TornadoDeviceProgram::add,
                        storage[0], storage[1], storage[2], grid.global());
            } else {
                graph.task(TASK, TornadoDeviceProgram::bitcount,
                        storage[0], storage[1], grid.global());
            }
            graph.transferToHost(DataTransferMode.UNDER_DEMAND, (Object[]) storage);
            WorkerGrid1D workerGrid = new WorkerGrid1D(grid.launchSize());
            workerGrid.setLocalWork(grid.local(), 1, 1);
            GridScheduler scheduler = new GridScheduler(GRAPH + "." + TASK, workerGrid);
            created = new TornadoExecutionPlan(graph.snapshot());
            created = created.withDevice(device);
            created = created.withGridScheduler(scheduler);
            created = created.withoutPlanResultsHistory();
        } catch (RuntimeException | Error failure) {
            closeFailedConstruction(created, allocation, failure);
            throw failure;
        }
        arena = allocation;
        arrays = storage;
        plan = created;
    }

    static void validateShape(KernelId kernel, Grid1D grid, int[] lengths) {
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(grid, "grid");
        Objects.requireNonNull(lengths, "bufferLengths");
        int count = switch (kernel.name()) {
            case TornadoBackendProvider.INT_ADD -> 3;
            case TornadoBackendProvider.INT_BITCOUNT -> 2;
            default -> throw new IllegalArgumentException(
                    "unsupported Tornado parallel kernel: " + kernel);
        };
        if (grid.global() > TornadoBackendProvider.MAX_ITEMS
                || grid.local() > TornadoBackendProvider.MAX_ITEMS) {
            throw new IllegalArgumentException(
                    "Tornado integer kernel exceeds its reviewed work bound");
        }
        if (lengths.length != count) {
            throw new IllegalArgumentException("kernel requires " + count + " buffer bindings");
        }
        for (int length : lengths) {
            // The reviewed oracles guard the output length. Binding every lane
            // to the logical size keeps padded work-items outside every array
            // and gives the device and Java reference exactly the same domain.
            if (length != grid.global()) {
                throw new IllegalArgumentException("each buffer length must equal grid.global");
            }
        }
    }

    @Override
    public void upload(int slot, IntBuffer host) {
        IntArray array = binding(slot, host);
        TornadoBackendProvider.requireDeviceExecution();
        for (int index = 0; index < host.length(); index++) {
            array.set(index, host.get(index));
        }
        if (executionStarted) {
            // This is the fork's explicit, synchronous upload operation. It
            // refreshes only the selected binding and does not run the kernel.
            plan.transferToDevice(array);
        }
    }

    @Override
    public void download(int slot, IntBuffer host) {
        IntArray array = binding(slot, host);
        if (lastResult == null) {
            throw new IllegalStateException(
                    "a completed Tornado launch is required before download");
        }
        lastResult.transferToHost(array);
        for (int index = 0; index < host.length(); index++) {
            host.set(index, array.get(index));
        }
    }

    @Override
    public void launch() {
        requireOpen();
        TornadoBackendProvider.requireDeviceExecution();
        // After any attempted execution, retry uploads must be explicit: the
        // runtime may have completed its first copy-in before a kernel failed.
        executionStarted = true;
        lastResult = null;
        lastResult = Objects.requireNonNull(plan.execute(), "Tornado execution result");
    }

    @Override
    public void close() {
        if (!open) {
            return;
        }
        open = false;
        try (arena) {
            // Device buffers are released before the host segments they refer to.
            plan.close();
        } catch (TornadoExecutionPlanException failure) {
            throw new IllegalStateException("cannot release Tornado device buffers", failure);
        } finally {
            lastResult = null;
            plan = null;
            Arrays.fill(arrays, null);
        }
    }

    private IntArray binding(int slot, IntBuffer host) {
        requireOpen();
        Objects.checkIndex(slot, arrays.length);
        Objects.requireNonNull(host, "host");
        IntArray array = arrays[slot];
        if (host.length() != array.getSize()) {
            throw new IllegalArgumentException("host length differs from device binding");
        }
        return array;
    }

    private void requireOpen() {
        if (!open) {
            throw new IllegalStateException("Tornado device program is closed");
        }
    }

    private static void closeFailedConstruction(TornadoExecutionPlan created, Arena allocation,
                                                Throwable failure) {
        if (created != null) {
            try {
                created.close();
            } catch (Exception | Error cleanup) {
                failure.addSuppressed(cleanup);
            }
        }
        try {
            allocation.close();
        } catch (RuntimeException | Error cleanup) {
            failure.addSuppressed(cleanup);
        }
    }

    // The annotated loop bound is the logical size, not the rounded launch
    // size. Tornado lowers the loop to device work-items; ordinary Java can
    // execute the same source for independent no-device kernel tests.
    static void add(IntArray left, IntArray right, IntArray sum, int size) {
        for (@Parallel int index = 0; index < size; index++) {
            sum.set(index, left.get(index) + right.get(index));
        }
    }

    static void bitcount(IntArray bits, IntArray counts, int size) {
        for (@Parallel int index = 0; index < size; index++) {
            counts.set(index, Integer.bitCount(bits.get(index)));
        }
    }
}
