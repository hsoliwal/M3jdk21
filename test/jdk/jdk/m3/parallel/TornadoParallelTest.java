/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */



import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import jdk.internal.vm.parallel.BulkExecution;
import jdk.internal.vm.parallel.BulkScheduler;
import jdk.internal.vm.parallel.BulkTask;
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.Plan;
import jdk.m3.parallel.Result;
import jdk.m3.parallel.Transfer;
import jdk.m3.parallel.Verified;
import jdk.m3.parallel.bulk.M3ParallelBitCountProvider;
import jdk.m3.parallel.bulk.M3ParallelIntAddProvider;
import jdk.m3.parallel.spi.Probe;

/**
 * Explicit integration test for an image containing the optional parallel and
 * Tornado modules. This class is deliberately not an automatic jtreg test in a
 * default JDK image that does not contain those modules.
 *
 * <p>Run {@code TornadoParallelTest unavailable} with no usable Tornado device;
 * {@code TornadoParallelTest device [cpu|gpu]} with an actual device; or
 * {@code TornadoParallelTest image [cpu|gpu]} with an installed image. Device
 * modes require the real provider and never turn an unavailable device into a
 * passing skip. Preview, native-access, module resolution and the export of
 * {@code java.base/jdk.internal.vm.parallel} must be configured by the caller.</p>
 *
 * <p>For the cached runtime-option regression, run unavailable mode in a fresh
 * VM with {@code -Dtest.tornado.cached-option=bailout} or {@code virtual}. The
 * fixture enables and initializes the real runtime option, clears its system
 * property, and checks that the provider still refuses the cached option.</p>
 */
public final class TornadoParallelTest {
    private static final String BACKEND = "tornado";
    private static final KernelId ADD = KernelId.of(M3ParallelIntAddProvider.OPERATION_ID);
    private static final KernelId BITCOUNT = KernelId.of(M3ParallelBitCountProvider.OPERATION_ID);
    private static final int[] EDGE_BITS = {
            0, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, 1, -2,
            0x5555_5555, 0xaaaa_aaaa, 0x0f0f_0f0f, 0xf0f0_f0f0
    };
    private static long checks;

    private TornadoParallelTest() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2
                || !Set.of("unavailable", "device", "image").contains(args[0])
                || args.length == 2 && !Set.of("cpu", "gpu").contains(args[1])) {
            throw new IllegalArgumentException(
                    "usage: TornadoParallelTest unavailable|device|image [cpu|gpu]");
        }
        if ("unavailable".equals(args[0])) {
            unavailable(cacheRejectedOption());
        } else {
            Backend backend = device(args.length == 2 ? args[1] : null);
            if ("image".equals(args[0])) {
                imageIdentity();
            }
            bulkOperations(backend, true);
            mixedTransfers(backend);
            zeroAndExplicitTransfers(backend);
            verifiedReceipts(backend);
            rejectedPlans(backend);
        }
        System.out.println("PASS TornadoParallelTest mode=" + args[0] + " checks=" + checks);
    }

    private static String cacheRejectedOption() throws ReflectiveOperationException {
        String option = System.getProperty("test.tornado.cached-option");
        if (option == null) {
            return null;
        }
        String property;
        String field;
        switch (option) {
            case "bailout" -> {
                property = "tornado.recover.bailout";
                field = "RECOVER_BAILOUT";
            }
            case "virtual" -> {
                property = "tornado.virtual.device";
                field = "VIRTUAL_DEVICE_ENABLED";
            }
            default -> throw new IllegalArgumentException(
                    "unknown test.tornado.cached-option: " + option);
        }
        System.setProperty(property, "true");
        try {
            Class<?> options = Class.forName(
                    "uk.ac.manchester.tornado.runtime.common.TornadoOptions");
            require(options.getField(field).getBoolean(null),
                    "real Tornado runtime option must be cached true");
        } finally {
            System.setProperty(property, "false");
        }
        return "cached " + property + "=true";
    }

    private static void unavailable(String expectedRefusal) {
        Probe probe = Backend.probeAll().get(BACKEND);
        require(probe != null, "Tornado provider must be discovered even when unavailable");
        require(!probe.available(), "unavailable mode found an available Tornado provider");
        require(!probe.reason().isBlank(), "unavailability reason must be recorded");
        if (expectedRefusal != null) {
            require(probe.reason().contains(expectedRefusal),
                    "cached option refusal must precede device probing: " + probe.reason());
        }
        require(Backend.reference().isReference(), "reference backend must remain usable");
        require(Backend.select(Backend.Policy.REFERENCE) == Backend.reference(),
                "reference selection");
        expect(IllegalStateException.class, () -> Backend.of(BACKEND),
                "unavailable backend selection");
        expect(IllegalStateException.class, () -> Backend.select(Backend.Policy.ACCELERATOR),
                "required accelerator must reject missing device");
        expect(IllegalStateException.class, M3ParallelIntAddProvider::requiredAccelerator,
                "required add provider must reject missing device");
        expect(IllegalStateException.class, M3ParallelBitCountProvider::requiredAccelerator,
                "required bitcount provider must reject missing device");
        bulkOperations(Backend.reference(), false);
        System.out.println("Tornado unavailable: " + probe.reason());
    }

    private static Backend device(String expectedType) {
        Probe probe = Backend.probeAll().get(BACKEND);
        require(probe != null && probe.available(), "real Tornado device required: " + probe);
        Backend backend = Backend.of(BACKEND);
        require(!backend.isReference(), "Tornado device must not be the Java reference");
        require(Backend.select(Backend.Policy.ACCELERATOR) == backend,
                "required accelerator selection");
        require(backend.supports(ADD) && backend.supports(BITCOUNT),
                "both existing kernels must be supported");
        if ("cpu".equals(expectedType)) {
            require(backend.capabilities().contains(Backend.Capability.CPU),
                    "CPU capability must be reported");
            require(!backend.capabilities().contains(Backend.Capability.GPU),
                    "CPU device must not claim GPU");
        } else if ("gpu".equals(expectedType)) {
            require(backend.capabilities().contains(Backend.Capability.GPU),
                    "GPU capability must be reported");
            require(!backend.capabilities().contains(Backend.Capability.CPU),
                    "GPU device must not claim CPU");
        }
        System.out.println("Tornado device capabilities: " + backend.capabilities());
        return backend;
    }

    private static void bulkOperations(Backend backend, boolean actualDevice) {
        M3ParallelIntAddProvider add = actualDevice
                ? M3ParallelIntAddProvider.requiredAccelerator()
                : M3ParallelIntAddProvider.reference();
        M3ParallelBitCountProvider bitcount = actualDevice
                ? M3ParallelBitCountProvider.requiredAccelerator()
                : M3ParallelBitCountProvider.reference();
        BulkScheduler scheduler = new BulkScheduler(List.of(add, bitcount));
        require(scheduler.providerIds().containsAll(List.of(add.id(), bitcount.id())),
                "bulk providers registered");
        Random random = new Random(0x544f524e41444f4cL);
        int[] lengths = actualDevice ? new int[]{1, 7, 33, 129, 257} : new int[]{17};
        for (int length : lengths) {
            int local = actualDevice ? Math.min(32, Integer.highestOneBit(length)) : 0;
            int[] left = sample(random, length);
            int[] right = sample(random, length);
            left[0] = Integer.MAX_VALUE;
            right[0] = 1;
            if (length > 1) {
                left[1] = Integer.MIN_VALUE;
                right[1] = -1;
            }
            int[] leftBefore = left.clone();
            int[] rightBefore = right.clone();
            BulkTask addTask = addTask(length, local);
            require(scheduler.select(addTask) == add, "scheduler must select the add provider");
            BulkExecution added = scheduler.execute(addTask, Map.of("left", left, "right", right));
            checkEnvelope(added, "m3-parallel-" + backend.id(), "sum");
            int[] expected = add(leftBefore, rightBefore);
            equal((int[]) added.output("sum"), expected, "bulk addition including wraparound");
            equal(left, leftBefore, "bulk add left input remains unchanged");
            equal(right, rightBefore, "bulk add right input remains unchanged");
            int[] exposed = (int[]) added.output("sum");
            exposed[0] ^= -1;
            equal((int[]) added.output("sum"), expected, "bulk result keeps its own output image");

            int[] bits = sample(random, length);
            int[] bitsBefore = bits.clone();
            BulkTask countTask = countTask(length, local);
            require(scheduler.select(countTask) == bitcount,
                    "scheduler must select the bitcount provider");
            BulkExecution counted = scheduler.execute(countTask, Map.of("bits", bits));
            checkEnvelope(counted, "m3-bitcount-" + backend.id(), "counts");
            equal((int[]) counted.output("counts"), bitcount(bitsBefore),
                    "bulk bitcount signed patterns");
            equal(bits, bitsBefore, "bulk bitcount input remains unchanged");
        }
        int local = actualDevice ? 1 : 0;
        expect(IllegalArgumentException.class,
                () -> scheduler.execute(addTask(2, local),
                        Map.of("left", new int[1], "right", new int[2])),
                "bulk input length mismatch");
        expect(IllegalArgumentException.class,
                () -> scheduler.execute(countTask(1, local), Map.of("bits", new long[1])),
                "bulk input primitive type");
        expect(IllegalArgumentException.class,
                () -> scheduler.execute(countTask(1, local),
                        Map.of("bits", new int[1], "extra", new int[1])),
                "bulk unexpected input");
        expect(IllegalStateException.class,
                () -> scheduler.execute(new BulkTask("unsupported.kernel", 1, local,
                        List.of("bits"), List.of("counts")), Map.of("bits", new int[1])),
                "scheduler must reject an unsupported operation");
    }

    private static void mixedTransfers(Backend backend) {
        int length = 33;
        Random random = new Random(0x4d49584544L);
        int[] originalLeft = sample(random, length);
        int[] originalRight = sample(random, length);
        IntBuffer left = IntBuffer.of(originalLeft);
        IntBuffer right = IntBuffer.of(originalRight);
        IntBuffer sum = filled(length, 0x6bad_beef);
        Plan plan = Plan.builder(backend).kernel(ADD).grid(Grid1D.of(length, 8))
                .bind(left, Transfer.ONCE).bind(right, Transfer.EACH_LAUNCH)
                .bind(sum, Transfer.ON_DEMAND).build();
        Result current = null;
        try {
            Result first = plan.execute();
            current = first;
            require(first.plan() == plan && first.launch() == 1, "first result identity");
            equal(sum.toArray(), constant(length, 0x6bad_beef), "download must be explicit");
            require(first.download(sum) == first, "download returns the current result");
            equal(sum.toArray(), add(originalLeft, originalRight), "first mixed transfer launch");

            left.copyFrom(constant(length, 77));
            int[] nextRight = sample(random, length);
            right.copyFrom(nextRight);
            Result second = plan.execute();
            current = second;
            require(second.launch() == 2 && plan.session().launches() == 2,
                    "second launch counter");
            expect(IllegalStateException.class, () -> first.download(sum), "stale result download");
            expect(IllegalStateException.class, () -> first.upload(left), "stale result upload");
            second.download(sum);
            equal(sum.toArray(), add(originalLeft, nextRight),
                    "ONCE isolated; EACH_LAUNCH refreshed");

            IntBuffer unbound = IntBuffer.allocate(length);
            sum.copyFrom(constant(length, 123));
            expect(IllegalArgumentException.class, () -> second.download(sum, unbound),
                    "unbound download");
            equal(sum.toArray(), constant(length, 123),
                    "invalid download must not partially transfer");
            expect(IllegalArgumentException.class, () -> second.upload(unbound), "unbound upload");
            int[] patchedLeft = sample(random, length);
            left.copyFrom(patchedLeft);
            require(second.upload(left) == second, "explicit upload returns the current result");
            left.copyFrom(constant(length, 999));
            second.download(left);
            equal(left.toArray(), patchedLeft,
                    "explicit upload snapshots before later host mutation");
            require(plan.session().launches() == 2, "explicit transfers must not launch a kernel");

            int[] finalRight = sample(random, length);
            right.copyFrom(finalRight);
            current = plan.execute().downloadAll();
            equal(sum.toArray(), add(patchedLeft, finalRight),
                    "explicit input refresh and repeated automatic upload");
            equal(left.toArray(), patchedLeft, "downloadAll preserves device left");
            equal(right.toArray(), finalRight, "downloadAll preserves device right");
            require(current.launch() == 3, "third launch counter");
        } finally {
            plan.close();
        }
        plan.close();
        require(!plan.isOpen() && !plan.session().isOpen(),
                "closed plan state and idempotent close");
        expect(IllegalStateException.class, plan::execute, "closed plan execute");
        Result closedResult = current;
        require(closedResult != null, "completed result retained for closed-state check");
        expect(IllegalStateException.class, () -> closedResult.download(sum),
                "closed result download");
        expect(IllegalStateException.class, () -> closedResult.upload(left),
                "closed result upload");
    }

    private static void zeroAndExplicitTransfers(Backend backend) {
        int length = 37;
        IntBuffer bits = filled(length, -1);
        IntBuffer counts = filled(length, -777);
        try (Plan plan = Plan.builder(backend).kernel(BITCOUNT).grid(Grid1D.of(length, 8))
                .bind(bits, Transfer.ON_DEMAND).bind(counts, Transfer.ON_DEMAND).build()) {
            Result first = plan.execute();
            equal(bits.toArray(), constant(length, -1), "unuploaded input host image stays intact");
            equal(counts.toArray(), constant(length, -777), "output download stays explicit");
            first.downloadAll();
            equal(bits.toArray(), new int[length], "never-uploaded input storage starts at zero");
            equal(counts.toArray(), new int[length], "zero input population counts");
            bits.copyFrom(constant(length, -1));
            first.upload(bits);
            bits.copyFrom(new int[length]);
            Result second = plan.execute().download(counts);
            equal(counts.toArray(), constant(length, 32), "explicitly uploaded negative patterns");
            bits.copyFrom(constant(length, Integer.MIN_VALUE));
            plan.execute().downloadAll();
            equal(bits.toArray(), constant(length, -1),
                    "ON_DEMAND ignores unuploaded host mutation");
            equal(counts.toArray(), constant(length, 32),
                    "resident input stays available on repeated launch");
            expect(IllegalStateException.class, second::downloadAll, "superseded ON_DEMAND result");
        }
    }

    private static void verifiedReceipts(Backend backend) {
        Random random = new Random(0x564552494659L);
        for (KernelId kernel : List.of(ADD, BITCOUNT)) {
            int length = 17;
            int[] source = sample(random, length);
            int[] other = sample(random, length);
            IntBuffer first = IntBuffer.of(source);
            IntBuffer second = IntBuffer.of(other);
            IntBuffer output = filled(length, -333);
            Grid1D grid = Grid1D.of(length, 8);
            Plan.Builder builder = Plan.builder(backend).kernel(kernel).grid(grid)
                    .bind(first, Transfer.ONCE);
            if (kernel.equals(ADD)) {
                builder.bind(second, Transfer.ONCE);
            }
            builder.bind(output, Transfer.ON_DEMAND);
            try (Plan plan = builder.build()) {
                Verified receipt = Verified.run(plan, 2);
                require(receipt.backend().equals(BACKEND),
                        "receipt identifies actual Tornado backend");
                require(receipt.kernel().equals(kernel) && receipt.grid().equals(grid),
                        "receipt kernel and grid");
                require(receipt.launches() == 2 && plan.session().launches() == 2,
                        "receipt launch count");
                hash(receipt.inputRoot(), "receipt input root");
                hash(receipt.outputRoot(), "receipt output root");
                hash(receipt.root(), "receipt root");
                equal(first.toArray(), source, "verified source binding remains unchanged");
                if (kernel.equals(ADD)) {
                    equal(second.toArray(), other, "verified second binding remains unchanged");
                }
                equal(output.toArray(), kernel.equals(ADD) ? add(source, other) : bitcount(source),
                        "verified complete output image");
                expect(IllegalStateException.class, () -> Verified.run(plan),
                        "verification requires a fresh plan");
                String changedRoot = (receipt.root().charAt(0) == '0' ? "1" : "0")
                        + receipt.root().substring(1);
                expect(IllegalArgumentException.class,
                        () -> new Verified(receipt.backend(), receipt.kernel(), receipt.grid(),
                                receipt.launches(),
                                receipt.inputRoot(), receipt.outputRoot(), changedRoot),
                        "receipt must reject a changed content root");
            }
        }
    }

    private static void rejectedPlans(Backend backend) {
        KernelId unsupported = KernelId.of("m3.parallel.unreviewed.v1");
        require(!backend.supports(unsupported), "unknown kernel must not be advertised");
        expect(IllegalArgumentException.class,
                () -> Plan.builder(backend).kernel(unsupported).grid(Grid1D.of(3, 2))
                        .bind(IntBuffer.allocate(3), Transfer.ONCE).build(),
                "unsupported kernel prepare");
        expect(IllegalArgumentException.class,
                () -> Plan.builder(backend).kernel(ADD).grid(Grid1D.of(3, 2))
                        .bind(IntBuffer.allocate(3), Transfer.ONCE)
                        .bind(IntBuffer.allocate(3), Transfer.ON_DEMAND).build(),
                "add binding count");
        expect(IllegalArgumentException.class,
                () -> Plan.builder(backend).kernel(BITCOUNT).grid(Grid1D.of(3, 2))
                        .bind(IntBuffer.allocate(4), Transfer.ONCE)
                        .bind(IntBuffer.allocate(3), Transfer.ON_DEMAND).build(),
                "binding length versus grid");
        expect(IllegalArgumentException.class,
                () -> Plan.builder(backend).kernel(BITCOUNT)
                        .grid(Grid1D.of(M3ParallelBitCountProvider.MAX_ITEMS + 1, 1))
                        .bind(IntBuffer.allocate(1), Transfer.ONCE)
                        .bind(IntBuffer.allocate(1), Transfer.ON_DEMAND).build(),
                "reviewed work bound");
    }

    private static void imageIdentity() throws ClassNotFoundException {
        inImage(Backend.class, "m3.parallel");
        inImage(Plan.class, "m3.parallel");
        inImage(M3ParallelIntAddProvider.class, "m3.parallel.bulk.provider");
        inImage(M3ParallelBitCountProvider.class, "m3.parallel.bulk.provider");
        inImage(BulkScheduler.class, "java.base");
        inImage(Class.forName("jdk.internal.m3.tornado.parallel.TornadoBackendProvider"),
                "jdk.m3.tornado");
        inImage(Class.forName("uk.ac.manchester.tornado.api.TornadoExecutionPlan"), "tornado.api");
        inImage(Class.forName("uk.ac.manchester.tornado.runtime.TornadoCoreRuntime"),
                "tornado.runtime");
    }

    private static void inImage(Class<?> type, String module) {
        require(module.equals(type.getModule().getName()),
                type.getName() + " must belong to " + module);
        URL resource = type.getResource('/' + type.getName().replace('.', '/') + ".class");
        require(resource != null && "jrt".equals(resource.getProtocol()),
                type.getName() + " must load from jrt");
        require(resource.getPath().startsWith('/' + module + '/'),
                type.getName() + " jrt module identity");
    }

    private static BulkTask addTask(int length, int local) {
        return new BulkTask(M3ParallelIntAddProvider.OPERATION_ID, length, local,
                List.of("left", "right"), List.of("sum"));
    }

    private static BulkTask countTask(int length, int local) {
        return new BulkTask(M3ParallelBitCountProvider.OPERATION_ID, length, local,
                List.of("bits"), List.of("counts"));
    }

    private static void checkEnvelope(BulkExecution execution, String provider, String output) {
        require(execution.provider().equals(provider), "bulk receipt provider identity");
        require(execution.outputNames().equals(Set.of(output)), "complete bulk output schema");
        hash(execution.receiptRoot(), "bulk receipt root");
    }

    private static int[] sample(Random random, int length) {
        int[] result = random.ints(length).toArray();
        System.arraycopy(EDGE_BITS, 0, result, 0, Math.min(length, EDGE_BITS.length));
        return result;
    }

    private static int[] add(int[] left, int[] right) {
        int[] result = new int[left.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = left[i] + right[i];
        }
        return result;
    }

    private static int[] bitcount(int[] input) {
        int[] result = new int[input.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = Integer.bitCount(input[i]);
        }
        return result;
    }

    private static int[] constant(int length, int value) {
        int[] result = new int[length];
        Arrays.fill(result, value);
        return result;
    }

    private static IntBuffer filled(int length, int value) {
        return IntBuffer.of(constant(length, value));
    }

    private static void equal(int[] actual, int[] expected, String message) {
        require(actual.length == expected.length, message + " length");
        for (int i = 0; i < expected.length; i++) {
            require(actual[i] == expected[i], message + " at " + i
                    + ": expected=" + expected[i] + ", actual=" + actual[i]);
        }
    }

    private static void hash(String value, String message) {
        require(value != null && value.matches("[0-9a-f]{64}"), message);
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action,
                               String message) {
        try {
            action.run();
        } catch (RuntimeException actual) {
            require(type.isInstance(actual),
                    message + ": expected " + type.getName() + ", got " + actual);
            return;
        }
        throw new AssertionError(message + ": expected " + type.getName());
    }
}
