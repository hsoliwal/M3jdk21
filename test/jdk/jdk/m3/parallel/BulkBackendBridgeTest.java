/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Exercise existing bulk adapters through interchangeable public engines and the
 * qualified JDK bridge
 * @modules java.base/jdk.internal.vm.parallel
 *          m3.parallel/jdk.m3.parallel
 *          m3.parallel.bulk.provider/jdk.m3.parallel.bulk
 * @build BackendSwapTest
 * @run main/othervm BulkBackendBridgeTest
 */



import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.bulk.M3ParallelBitCountProvider;
import jdk.m3.parallel.bulk.M3ParallelIntAddProvider;
import jdk.m3.parallel.spi.BackendProvider;

public class BulkBackendBridgeTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("bulk-provider-services");
        Path service = directory.resolve("META-INF/services/" + BackendProvider.class.getName());
        Files.createDirectories(service.getParent());
        Files.writeString(service, BackendSwapTest.UnsupportedProvider.class.getName() + "\n"
                + BackendSwapTest.SupportedProvider.class.getName() + "\n");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                     new java.net.URL[]{directory.toUri().toURL()}, original)) {
            Thread.currentThread().setContextClassLoader(loader);
            // Register the shared reference first: adapter class initialization must use that
            // same body.
            JavaKernels.intAdd();
            JavaKernels.intBitCount();
            M3ParallelIntAddProvider referenceAdd = M3ParallelIntAddProvider.reference();
            M3ParallelBitCountProvider referenceBits = M3ParallelBitCountProvider.reference();
            M3ParallelIntAddProvider otherAdd = M3ParallelIntAddProvider.requiredAccelerator();
            M3ParallelBitCountProvider otherBits = M3ParallelBitCountProvider.requiredAccelerator();
            if (!otherAdd.id().equals("m3-parallel-a-supported")
                    || !otherBits.id().equals("m3-bitcount-a-supported")) {
                throw new AssertionError(
                        "bridge selected an engine that does not support its kernel");
            }
            BulkScheduler scheduler = new BulkScheduler(
                    List.of(referenceAdd, referenceBits, otherAdd, otherBits));
            int[] left = {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 7};
            int[] right = {1, -1, -1, 9};
            int[] sums = new int[left.length];
            for (int i = 0; i < sums.length; i++) sums[i] = left[i] + right[i];
            execute(scheduler, referenceAdd.id(), new BulkTask(
                            M3ParallelIntAddProvider.OPERATION_ID, 4, 0,
                            List.of("left", "right"), List.of("sum")),
                    Map.of("left", left, "right", right), "sum", sums);
            execute(scheduler, otherAdd.id(), new BulkTask(
                            M3ParallelIntAddProvider.OPERATION_ID, 4, 2,
                            List.of("left", "right"), List.of("sum")),
                    Map.of("left", left, "right", right), "sum", sums);
            int[] counts = Arrays.stream(left).map(Integer::bitCount).toArray();
            execute(scheduler, referenceBits.id(), new BulkTask(
                    M3ParallelBitCountProvider.OPERATION_ID, 4, 0,
                    List.of("bits"), List.of("counts")), Map.of("bits", left), "counts", counts);
            execute(scheduler, otherBits.id(), new BulkTask(
                    M3ParallelBitCountProvider.OPERATION_ID, 4, 2,
                    List.of("bits"), List.of("counts")), Map.of("bits", left), "counts", counts);
            if (!Arrays.equals(left, new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 7})) {
                throw new AssertionError("bridge modified caller inputs");
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            Files.deleteIfExists(service);
            Files.deleteIfExists(service.getParent());
            Files.deleteIfExists(directory.resolve("META-INF"));
            Files.deleteIfExists(directory);
        }
        System.out.println("BulkBackendBridgeTest passed");
    }

    private static void execute(BulkScheduler scheduler, String provider, BulkTask task,
                                Map<String, Object> inputs, String output, int[] expected) {
        BulkExecution execution = scheduler.withProvider(provider).execute(task, inputs);
        if (!execution.provider().equals(provider)
                || !Arrays.equals(expected, (int[]) execution.output(output))) {
            throw new AssertionError("bridge output mismatch for " + provider);
        }
    }
}
