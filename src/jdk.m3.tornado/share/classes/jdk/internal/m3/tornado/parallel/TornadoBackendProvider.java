/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.internal.m3.tornado.parallel;

import java.util.Objects;
import java.util.ServiceConfigurationError;
import java.util.Set;
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.spi.BackendProvider;
import jdk.m3.parallel.spi.DeviceProgram;
import jdk.m3.parallel.spi.Probe;
import uk.ac.manchester.tornado.api.TornadoRuntime;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.TornadoDeviceType;
import uk.ac.manchester.tornado.api.enums.TornadoVMBackendType;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;
import uk.ac.manchester.tornado.runtime.common.TornadoOptions;

/**
 * TornadoVM implementation of the existing parallel backend service.
 *
 * <p>The selected device is the runtime's observed default device. Discovery is
 * performed once per provider, so its capabilities, preference and probe describe
 * the same device even if another caller later changes Tornado's default. An
 * OpenCL CPU device advertises CPU capability; it does not advertise GPU or JNI
 * execution. The preference is a selection policy, not a performance claim.</p>
 *
 * <p>Only the existing bounded integer-add and integer-bitcount kernels are
 * admitted. A missing runtime, missing device, virtual device or enabled Java
 * bailout recovery makes this provider unavailable. There is no Java fallback
 * within this provider.</p>
 */
public final class TornadoBackendProvider implements BackendProvider {
    static final String INT_ADD = "m3.parallel.int-add.v1";
    static final String INT_BITCOUNT = "m3.parallel.int-bitcount.v1";
    static final int MAX_ITEMS = 1 << 18;

    private volatile Selection selection;

    /** Constructs the service without initializing the accelerator runtime. */
    public TornadoBackendProvider() {
    }

    @Override
    public String id() {
        return "tornado";
    }

    @Override
    public Set<Backend.Capability> capabilities() {
        return selection().capabilities();
    }

    @Override
    public int score() {
        return selection().score();
    }

    @Override
    public Probe probe() {
        return selection().probe();
    }

    @Override
    public boolean supports(KernelId kernel) {
        String name = Objects.requireNonNull(kernel, "kernel").name();
        return INT_ADD.equals(name) || INT_BITCOUNT.equals(name);
    }

    @Override
    public DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] bufferLengths) {
        Objects.requireNonNull(kernel, "kernel");
        Objects.requireNonNull(grid, "grid");
        int[] lengths = Objects.requireNonNull(bufferLengths, "bufferLengths").clone();
        TornadoDeviceProgram.validateShape(kernel, grid, lengths);

        Selection observed = selection();
        if (!observed.probe().available()) {
            throw new IllegalStateException(
                    "Tornado backend unavailable: " + observed.probe().reason());
        }
        requireDeviceExecution();
        long[] dimensions = observed.device().getDeviceMaxWorkgroupDimensions();
        if (dimensions != null && dimensions.length != 0
                && dimensions[0] > 0 && grid.local() > dimensions[0]) {
            throw new IllegalArgumentException("local work exceeds the selected device limit");
        }
        return new TornadoDeviceProgram(kernel, grid, lengths, observed.device());
    }

    private Selection selection() {
        Selection current = selection;
        if (current == null) {
            synchronized (this) {
                current = selection;
                if (current == null) {
                    current = discover();
                    selection = current;
                }
            }
        }
        return current;
    }

    private static Selection discover() {
        try {
            // Check the runtime's actual cached flags before initializing any
            // driver. Changing the original properties cannot hide them.
            requireDeviceExecution();
            TornadoRuntime runtime = Objects.requireNonNull(
                    TornadoRuntimeProvider.getTornadoRuntime(), "Tornado runtime");
            if (runtime.getNumBackends() == 0) {
                return Selection.unavailable("no Tornado accelerator backend is installed");
            }
            TornadoDevice device = Objects.requireNonNull(
                    runtime.getDefaultDevice(), "Tornado device");
            TornadoVMBackendType backend = Objects.requireNonNull(
                    device.getTornadoVMBackend(), "Tornado backend type");
            if (backend == TornadoVMBackendType.JAVA || backend == TornadoVMBackendType.VIRTUAL) {
                return Selection.unavailable(
                        "the selected Tornado backend is not an execution device: " + backend);
            }
            TornadoDeviceType type = Objects.requireNonNull(
                    device.getDeviceType(), "Tornado device type");
            return switch (type) {
                case CPU -> new Selection(
                        device, Set.of(Backend.Capability.CPU), 10, Probe.ready());
                case GPU -> new Selection(
                        device, Set.of(Backend.Capability.GPU), 100, Probe.ready());
                case ACCELERATOR -> new Selection(device, Set.of(), 50, Probe.ready());
                default -> Selection.unavailable("unqualified Tornado device type: " + type);
            };
        } catch (RuntimeException | LinkageError | ServiceConfigurationError failure) {
            return Selection.unavailable("Tornado runtime/device unavailable: "
                    + failure.getClass().getName() + ": " + failure.getMessage());
        }
    }

    static void requireDeviceExecution() {
        requireDisabledSystemMode("tornado.recover.bailout");
        requireDisabledSystemMode("tornado.virtual.device");
        if (TornadoOptions.RECOVER_BAILOUT) {
            throw new IllegalStateException("Tornado runtime cached tornado.recover.bailout=true");
        }
        if (TornadoOptions.VIRTUAL_DEVICE_ENABLED) {
            throw new IllegalStateException("Tornado runtime cached tornado.virtual.device=true");
        }
    }

    private static void requireDisabledSystemMode(String property) {
        if (Boolean.getBoolean(property)) {
            throw new IllegalStateException(
                    property + " must be false for the Tornado parallel backend");
        }
    }

    private record Selection(TornadoDevice device, Set<Backend.Capability> capabilities,
            int score, Probe probe) {
        static Selection unavailable(String reason) {
            return new Selection(null, Set.of(), 0, Probe.unavailable(reason));
        }
    }
}
