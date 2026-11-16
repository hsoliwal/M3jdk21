/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Select interchangeable engines before preparation and retain each plan's backend
 * @modules m3.parallel/jdk.m3.parallel m3.parallel/jdk.m3.parallel.spi
 * @run main/othervm BackendSwapTest
 */



import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.JavaKernel;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.Plan;
import jdk.m3.parallel.Transfer;
import jdk.m3.parallel.Verified;
import jdk.m3.parallel.spi.BackendProvider;
import jdk.m3.parallel.spi.DeviceProgram;
import jdk.m3.parallel.spi.Probe;

public class BackendSwapTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        // Control discovery before the first use of Backend.reference/select/of(String).
        Path directory = Files.createTempDirectory("parallel-provider-services");
        Path service = directory.resolve("META-INF/services/" + BackendProvider.class.getName());
        Files.createDirectories(service.getParent());
        Files.writeString(service, UnsupportedProvider.class.getName() + "\n"
                + SupportedProvider.class.getName() + "\n"
                + SupportedZProvider.class.getName() + "\n"
                + LowerThrowingProvider.class.getName() + "\n");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                     new java.net.URL[]{directory.toUri().toURL()}, original)) {
            Thread.currentThread().setContextClassLoader(loader);
            explicitProviderAdmission();
            kernelAwareSelection();
            canonicalKernelsAndSwapping();
            inFlightPlanRetainsEngine();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            Files.deleteIfExists(service);
            Files.deleteIfExists(service.getParent());
            Files.deleteIfExists(directory.resolve("META-INF"));
            Files.deleteIfExists(directory);
        }
        System.out.println("BackendSwapTest passed " + checks + " checks");
    }

    private static void explicitProviderAdmission() {
        CpuProvider provider = new CpuProvider("private-cpu", 1);
        Backend backend = Backend.fromProvider(provider);
        require(backend.id().equals("private-cpu"), "explicit provider id");
        require(!backend.isReference(), "third-party CPU does not become the reference");
        provider.capabilities.clear();
        require(backend.capabilities().equals(Set.of(Backend.Capability.CPU)),
                "capability snapshot");
        expect(UnsupportedOperationException.class,
                () -> backend.capabilities().add(Backend.Capability.GPU));
        expect(NullPointerException.class, () -> Backend.fromProvider(null));
        expect(IllegalArgumentException.class,
                () -> Backend.fromProvider(new CpuProvider("bad id", 1)));
        expect(IllegalArgumentException.class,
                () -> Backend.fromProvider(new CpuProvider("java", 1)));
        CpuProvider impostor = new CpuProvider("impostor", 1);
        impostor.capabilities.add(Backend.Capability.REFERENCE);
        expect(IllegalArgumentException.class, () -> Backend.fromProvider(impostor));
        CpuProvider unavailable = new CpuProvider("unavailable", 1) {
            @Override
            public Probe probe() {
                return Probe.unavailable("no runtime");
            }
        };
        expect(IllegalStateException.class, () -> Backend.fromProvider(unavailable));
        require(unavailable.preparations.get() == 0, "unavailable engine cannot prepare");
        expect(IllegalStateException.class,
                () -> Backend.fromProvider(new CpuProvider("broken", 1) {
                    @Override
                    public Probe probe() {
                        throw new UnsatisfiedLinkError("driver missing");
                    }
                }));
        expect(IllegalStateException.class,
                () -> Backend.fromProvider(new CpuProvider("null-probe", 1) {
                    @Override
                    public Probe probe() {
                        return null;
                    }
                }));
        expect(IllegalArgumentException.class, () -> Backend.of("private-cpu"));
    }

    private static void kernelAwareSelection() {
        KernelId add = JavaKernels.intAdd();
        require(Backend.select(Backend.Policy.BEST).id().equals("high-unsupported"),
                "legacy policy preserved");
        require(Backend.select(Backend.Policy.BEST, add).id().equals("a-supported"),
                "kernel filters preference");
        require(Backend.select(Backend.Policy.ACCELERATOR, add).id().equals("a-supported"),
                "stable id tie-break");
        require(LowerThrowingProvider.PROBES.get() == 0,
                "lower-ranked engine was not consulted after selection");
        require(Backend.select(Backend.Policy.REFERENCE, add) == Backend.reference(),
                "explicit reference selection");
        KernelId missing = KernelId.of("test.missing");
        for (Backend.Policy policy : Backend.Policy.values()) {
            expect(IllegalStateException.class, () -> Backend.select(policy, missing));
        }
        KernelId custom = JavaKernels.register(KernelId.of("test.custom"),
                (index, buffers) -> {
                    if (index < buffers[0].length()) buffers[0].set(index, index);
                });
        require(Backend.select(Backend.Policy.BEST, custom) == Backend.reference(),
                "custom kernel uses capable CPU");
        expect(IllegalStateException.class,
                () -> Backend.select(Backend.Policy.ACCELERATOR, custom));
        CpuProvider unsupported = new UnsupportedProvider();
        Backend handle = Backend.fromProvider(unsupported);
        expect(IllegalArgumentException.class,
                () -> addPlan(handle, IntBuffer.of(1), IntBuffer.of(2), IntBuffer.allocate(1)));
        require(unsupported.preparations.get() == 0,
                "unsupported kernel rejected before allocation");
        expect(NullPointerException.class, () -> Backend.select(null, add));
        expect(NullPointerException.class, () -> Backend.select(Backend.Policy.BEST, null));
    }

    private static void canonicalKernelsAndSwapping() {
        KernelId add = JavaKernels.intAdd();
        JavaKernel body = JavaKernels.lookup(add).orElseThrow();
        require(add.equals(JavaKernels.intAdd()) && body == JavaKernels.lookup(add).orElseThrow(),
                "stable canonical body");
        require(JavaKernels.intBitCount().equals(KernelId.of("m3.parallel.int-bitcount.v1")),
                "bitcount id retained");
        expect(IllegalStateException.class,
                () -> JavaKernels.register(add, (index, buffers) -> {
                }));
        IntBuffer malformed = IntBuffer.allocate(1);
        expect(IllegalArgumentException.class,
                () -> body.run(0, IntBuffer.of(1), IntBuffer.of(2, 3), malformed));
        require(malformed.get(0) == 0, "invalid shape cannot write an output");

        int[] left = {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 0, 7, -80, 37};
        int[] right = {1, -1, -1, 0, 9, 80, Integer.MAX_VALUE};
        int[] expected = new int[left.length];
        for (int i = 0; i < expected.length; i++) expected[i] = left[i] + right[i];
        CpuProvider first = new CpuProvider("first-cpu", 1);
        Backend selected = Backend.fromProvider(first);
        IntBuffer a = IntBuffer.of(left), b = IntBuffer.of(right),
                oldOutput = IntBuffer.allocate(left.length);
        try (Plan oldPlan = addPlan(selected, a, b, oldOutput)) {
            Verified.run(oldPlan);
            require(Arrays.equals(expected, oldOutput.toArray()),
                    "independent provider and reference parity");
            selected = Backend.reference();
            IntBuffer newOutput = IntBuffer.allocate(left.length);
            try (Plan newPlan = addPlan(selected,
                         IntBuffer.of(left), IntBuffer.of(right), newOutput)) {
                Verified.run(newPlan);
                require(Arrays.equals(expected, newOutput.toArray()),
                        "same kernel on selected CPU reference");
                require(oldPlan.backend().id().equals("first-cpu")
                        && newPlan.backend().isReference(),
                        "plans retain engines");
            }
            a.set(0, 99);
            oldPlan.execute().download(oldOutput);
            require(Arrays.equals(expected, oldOutput.toArray()),
                    "old plan retains device state and ONCE transfer");
        }
        require(first.preparations.get() == 1 && first.closes.get() == 1,
                "one program per plan, one close");

        int[] patterns = {0, 1, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x55555555, 0x80000001};
        int[] counts = Arrays.stream(patterns).map(Integer::bitCount).toArray();
        for (Backend backend : new Backend[]{
                Backend.reference(), Backend.fromProvider(new CpuProvider("bits-cpu", 1))
        }) {
            IntBuffer input = IntBuffer.of(patterns), output = IntBuffer.allocate(patterns.length);
            try (Plan plan = Plan.builder(backend).kernel(JavaKernels.intBitCount())
                         .grid(Grid1D.of(patterns.length, 4))
                         .bind(input, Transfer.ONCE).bind(output, Transfer.ON_DEMAND).build()) {
                Verified.run(plan);
                require(Arrays.equals(counts, output.toArray()),
                        "signed bitcount parity on " + backend.id());
                require(Arrays.equals(patterns, input.toArray()), "bitcount preserves inputs");
            }
        }
    }

    private static void inFlightPlanRetainsEngine() throws Exception {
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        CpuProvider provider = new CpuProvider("blocking-cpu", 1) {
            @Override
            protected void beforeLaunch() {
                entered.countDown();
                try {
                    if (!release.await(10, TimeUnit.SECONDS))
                        throw new AssertionError("release timeout");
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                }
            }
        };
        IntBuffer result = IntBuffer.allocate(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Plan oldPlan = addPlan(
                     Backend.fromProvider(provider), IntBuffer.of(5), IntBuffer.of(8), result)) {
            Future<?> running = executor.submit(() -> oldPlan.execute().download(result));
            require(entered.await(10, TimeUnit.SECONDS), "old engine entered launch");
            try {
                IntBuffer next = IntBuffer.allocate(1);
                try (Plan newPlan = addPlan(
                             Backend.reference(), IntBuffer.of(20), IntBuffer.of(22), next)) {
                    newPlan.execute().download(next);
                    require(next.get(0) == 42,
                            "new engine executes independently of in-flight plan");
                }
            } finally {
                release.countDown();
            }
            running.get(10, TimeUnit.SECONDS);
            require(result.get(0) == 13 && oldPlan.backend().id().equals("blocking-cpu"),
                    "in-flight backend retained");
        } finally {
            release.countDown();
            executor.shutdownNow();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS))
                throw new AssertionError("executor did not terminate");
        }
    }

    private static Plan addPlan(Backend backend, IntBuffer left, IntBuffer right, IntBuffer sum) {
        return Plan.builder(backend).kernel(JavaKernels.intAdd()).grid(Grid1D.of(sum.length(), 4))
                .bind(left, Transfer.ONCE).bind(right, Transfer.ONCE)
                .bind(sum, Transfer.ON_DEMAND).build();
    }

    public static class CpuProvider implements BackendProvider {
        final EnumSet<Backend.Capability> capabilities = EnumSet.of(Backend.Capability.CPU);
        final AtomicInteger preparations = new AtomicInteger(), closes = new AtomicInteger();
        private final String id;
        private final int score;

        CpuProvider(String id, int score) {
            this.id = id;
            this.score = score;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Set<Backend.Capability> capabilities() {
            return capabilities;
        }

        @Override
        public int score() {
            return score;
        }

        @Override
        public Probe probe() {
            return Probe.ready();
        }

        @Override
        public boolean supports(KernelId kernel) {
            return kernel.name().equals("m3.parallel.int-add.v1")
                    || kernel.name().equals("m3.parallel.int-bitcount.v1");
        }

        protected void beforeLaunch() {
        }

        @Override
        public DeviceProgram prepare(KernelId kernel, Grid1D grid, int[] lengths) {
            preparations.incrementAndGet();
            int[][] device = Arrays.stream(lengths).mapToObj(int[]::new).toArray(int[][]::new);
            return new DeviceProgram() {
                @Override
                public void upload(int slot, IntBuffer host) {
                    host.copyTo(device[slot]);
                }

                @Override
                public void download(int slot, IntBuffer host) {
                    host.copyFrom(device[slot]);
                }

                @Override
                public void launch() {
                    beforeLaunch();
                    for (int index = 0; index < grid.global(); index++) {
                        if (kernel.name().equals("m3.parallel.int-add.v1")) {
                            device[2][index] = device[0][index] + device[1][index];
                        } else {
                            int value = device[0][index], count = 0;
                            while (value != 0) {
                                value &= value - 1;
                                count++;
                            }
                            device[1][index] = count;
                        }
                    }
                }

                @Override
                public void close() {
                    closes.incrementAndGet();
                }
            };
        }
    }

    public static final class UnsupportedProvider extends CpuProvider {
        public UnsupportedProvider() {
            super("high-unsupported", 1000);
        }

        @Override
        public boolean supports(KernelId kernel) {
            return kernel.name().equals("test.other");
        }
    }

    public static final class SupportedProvider extends CpuProvider {
        public SupportedProvider() {
            super("a-supported", 100);
        }
    }

    public static final class SupportedZProvider extends CpuProvider {
        public SupportedZProvider() {
            super("z-supported", 100);
        }
    }

    public static final class LowerThrowingProvider extends CpuProvider {
        static final AtomicInteger PROBES = new AtomicInteger();

        public LowerThrowingProvider() {
            super("lower-broken", 50);
        }

        @Override
        public boolean supports(KernelId kernel) {
            if (kernel.name().equals("m3.parallel.int-add.v1")) {
                PROBES.incrementAndGet();
                throw new AssertionError("a lower-ranked engine was needlessly consulted");
            }
            return false;
        }
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }

    private static void expect(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try {
            action.run();
        }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName(), failure);
        }
        throw new AssertionError("missing " + type.getName());
    }
}
