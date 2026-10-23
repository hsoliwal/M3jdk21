/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.tornado;

import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoBackend;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.TornadoExecutionResult;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.enums.ProfilerMode;
import uk.ac.manchester.tornado.api.runtime.TornadoRuntimeProvider;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;

/** Installed-image identity and a required-device integer-kernel parity probe. */
public final class M3TornadoImageProbe {
    private record Selected(int backendIndex, int deviceIndex, TornadoDevice device) {
    }

    private M3TornadoImageProbe() {
    }

    public static void main(String[] args) throws Exception {
        if ((args.length != 2 && args.length != 3) || !List.of("identity", "kernel", "no-device").contains(args[0])) {
            throw new IllegalArgumentException("expected identity|kernel|no-device, backend and optional gpu");
        }
        boolean requireGpu = args.length == 3;
        if (requireGpu && !args[2].equals("gpu")) {
            throw new IllegalArgumentException("only gpu device constraint is supported");
        }
        identity();
        if (args[0].equals("identity")) {
            System.out.println("M3_TORNADO_IMAGE_IDENTITY_PASS");
            return;
        }
        if (args[0].equals("no-device")) {
            // A deterministic empty-catalog fixture exercises the exact selection
            // refusal used below; it is not evidence of absent host hardware.
            select(List.of(), args[1]);
            throw new AssertionError("empty device catalog unexpectedly admitted");
        }
        var runtime = TornadoRuntimeProvider.getTornadoRuntime();
        List<Selected> devices = new ArrayList<>();
        for (int backendIndex = 0; backendIndex < runtime.getNumBackends(); backendIndex++) {
            TornadoBackend backend = runtime.getBackend(backendIndex);
            if (!backend.getBackendType().name().equalsIgnoreCase(args[1])) {
                continue;
            }
            for (int deviceIndex = 0; deviceIndex < backend.getNumDevices(); deviceIndex++) {
                TornadoDevice device = backend.getDevice(deviceIndex);
                if (!requireGpu || device.getDeviceType().name().equals("GPU")) {
                    devices.add(new Selected(backendIndex, deviceIndex, device));
                }
            }
        }
        Selected selected = select(devices, args[1]);
        execute(selected);
    }

    private static void identity() throws ClassNotFoundException {
        for (String name : List.of("tornado.api", "tornado.runtime", "tornado.annotation",
                "tornado.drivers.common", "tornado.graal", "jdk.m3.tornado")) {
            if (ModuleLayer.boot().findModule(name).isEmpty()) {
                throw new AssertionError("module not installed: " + name);
            }
        }
        for (Class<?> type : List.of(TaskGraph.class, Rv32iBulkExecutorAdapter.class,
                Class.forName("uk.ac.manchester.tornado.runtime.TornadoCoreRuntime", false,
                        ClassLoader.getSystemClassLoader()))) {
            URL resource = type.getResource(type.getSimpleName() + ".class");
            if (resource == null || !resource.getProtocol().equals("jrt")) {
                throw new AssertionError("class did not load from product image: " + type + " " + resource);
            }
        }
    }

    private static Selected select(List<Selected> devices, String backend) {
        if (devices.isEmpty()) {
            System.out.println("M3_TORNADO_NO_DEVICE backend=" + backend);
            System.exit(3);
            throw new AssertionError("process did not refuse absent device");
        }
        Selected result = devices.get(0);
        if (!result.device().getTornadoVMBackend().name().equalsIgnoreCase(backend)) {
            throw new AssertionError("selected device is from a different backend");
        }
        return result;
    }

    public static void integerKernel(IntArray left, IntArray right, IntArray output) {
        for (@Parallel int i = 0; i < output.getSize(); i++) {
            output.set(i, left.get(i) * 31 + (right.get(i) ^ 0x5a5a) + i);
        }
    }

    private static void execute(Selected selected) throws Exception {
        int size = 257;
        IntArray left = new IntArray(size);
        IntArray right = new IntArray(size);
        IntArray output = new IntArray(size);
        int[] expected = new int[size];
        for (int i = 0; i < size; i++) {
            int l = i * 17 - 1400;
            int r = i * 7 + 53;
            left.set(i, l);
            right.set(i, r);
            output.set(i, Integer.MIN_VALUE);
            expected[i] = l * 31 + (r ^ 0x5a5a) + i;
        }
        TaskGraph graph = new TaskGraph("m3image")
                .transferToDevice(DataTransferMode.FIRST_EXECUTION, left, right)
                .task("integer", M3TornadoImageProbe::integerKernel, left, right, output)
                .transferToHost(DataTransferMode.EVERY_EXECUTION, output);
        try (TornadoExecutionPlan plan = new TornadoExecutionPlan(graph.snapshot())
                .withDevice(selected.device()).withProfiler(ProfilerMode.SILENT)) {
            TornadoExecutionResult result = plan.execute();
            ByteBuffer values = ByteBuffer.allocate(size * Integer.BYTES);
            for (int i = 0; i < size; i++) {
                if (output.get(i) != expected[i]) {
                    throw new AssertionError("kernel mismatch at " + i + ": " + output.get(i) + " != " + expected[i]);
                }
                values.putInt(output.get(i));
            }
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(values.array()));
            var profiler = result.getProfilerResult();
            String evidence = "{\"elements\":" + size
                    + ",\"backend\":" + quote(selected.device().getTornadoVMBackend().name().toLowerCase(Locale.ROOT))
                    + ",\"device_type\":" + quote(selected.device().getDeviceType().name())
                    + ",\"device_name\":" + quote(selected.device().getDeviceName())
                    + ",\"backend_index\":" + selected.backendIndex()
                    + ",\"device_index\":" + selected.deviceIndex()
                    + ",\"output_sha256\":" + quote(digest)
                    + ",\"copy_in_bytes\":" + profiler.getTotalBytesCopyIn()
                    + ",\"copy_out_bytes\":" + profiler.getTotalBytesCopyOut()
                    + ",\"kernel_nanoseconds\":" + profiler.getDeviceKernelTime() + "}";
            System.out.println("M3_TORNADO_KERNEL_PASS " + evidence);
        }
    }

    private static String quote(String value) {
        StringBuilder result = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\\' || ch == '"') {
                result.append('\\').append(ch);
            } else if (ch < 32) {
                result.append(String.format(Locale.ROOT, "\\u%04x", (int) ch));
            } else {
                result.append(ch);
            }
        }
        return result.append('"').toString();
    }
}
