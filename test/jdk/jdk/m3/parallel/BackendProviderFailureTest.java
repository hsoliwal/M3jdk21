/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 */
/*
 * @test
 * @summary Isolate optional provider callback failures without changing explicit execution
 * @modules m3.parallel/jdk.m3.parallel m3.parallel/jdk.m3.parallel.spi
 * @build BackendSwapTest
 * @run main/othervm BackendProviderFailureTest id
 * @run main/othervm BackendProviderFailureTest capabilities
 * @run main/othervm BackendProviderFailureTest score
 * @run main/othervm BackendProviderFailureTest probe
 * @run main/othervm BackendProviderFailureTest supports-service
 * @run main/othervm BackendProviderFailureTest supports-runtime
 * @run main/othervm BackendProviderFailureTest supports-linkage
 * @run main/othervm BackendProviderFailureTest supports-assertion
 * @run main/othervm BackendProviderFailureTest reference-error
 */

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;
import jdk.m3.parallel.Backend;
import jdk.m3.parallel.Grid1D;
import jdk.m3.parallel.IntBuffer;
import jdk.m3.parallel.JavaKernels;
import jdk.m3.parallel.KernelId;
import jdk.m3.parallel.Plan;
import jdk.m3.parallel.Transfer;
import jdk.m3.parallel.Verified;
import jdk.m3.parallel.spi.BackendProvider;
import jdk.m3.parallel.spi.Probe;

/** Each jtreg action gets a fresh registry and discovers real providers through ServiceLoader. */
public class BackendProviderFailureTest {
    private static final Set<String> SCENARIOS = Set.of(
            "id", "capabilities", "score", "probe", "supports-service", "supports-runtime",
            "supports-linkage", "supports-assertion", "reference-error");
    private static final ServiceConfigurationError SERVICE_FAILURE =
            new ServiceConfigurationError("intentional provider callback failure");
    private static final IllegalStateException RUNTIME_FAILURE =
            new IllegalStateException("intentional provider runtime failure");
    private static final LinkageError LINKAGE_FAILURE =
            new LinkageError("intentional provider linkage failure");
    private static final AssertionError ASSERTION_FAILURE =
            new AssertionError("intentional provider assertion failure");
    private static final IllegalStateException REFERENCE_FAILURE =
            new IllegalStateException("intentional reference kernel failure");
    private static final List<String> SUPPORT_CALLS = new ArrayList<>();

    private static String scenario;
    private static BrokenProvider broken;
    private static HealthyProvider healthy;

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !SCENARIOS.contains(args[0])) {
            throw new IllegalArgumentException("one known scenario is required");
        }
        scenario = args[0];
        Path directory = Files.createTempDirectory("parallel-callback-services");
        Path service = directory.resolve("META-INF/services/" + BackendProvider.class.getName());
        Files.createDirectories(service.getParent());
        // Deliberately disagree with rank and identifier order.
        Files.writeString(service, HealthyZProvider.class.getName() + "\n"
                + LowerProvider.class.getName() + "\n"
                + HealthyProvider.class.getName() + "\n"
                + UnsupportedProvider.class.getName() + "\n"
                + BrokenProvider.class.getName() + "\n");
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader loader = new URLClassLoader(
                new URL[]{directory.toUri().toURL()}, original)) {
            Thread.currentThread().setContextClassLoader(loader);
            KernelId add = JavaKernels.intAdd();
            Backend reference;
            try {
                reference = Backend.reference();
            } catch (ServiceConfigurationError failure) {
                // Keep the baseline's permanent class-initialization failure observable.
                try {
                    Backend.reference();
                } catch (NoClassDefFoundError poisoned) {
                    System.err.println("REGISTRY_POISONED " + scenario);
                }
                throw failure;
            }
            require(reference.isReference(), "registry lost its reference");
            if (scenario.equals("supports-assertion")) {
                assertionPropagates(add);
            } else if (scenario.equals("reference-error")) {
                referenceFailurePropagates(reference);
            } else {
                selectAndExecute(add, reference);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(original);
            Files.deleteIfExists(service);
            Files.deleteIfExists(service.getParent());
            Files.deleteIfExists(directory.resolve("META-INF"));
            Files.deleteIfExists(directory);
        }
        System.out.println("BackendProviderFailureTest passed " + scenario);
    }

    private static void selectAndExecute(KernelId add, Backend reference) {
        boolean supportsFailure = scenario.startsWith("supports-");
        if (!supportsFailure) {
            Map<String, Probe> probes = Backend.probeAll();
            String diagnostic = scenario.equals("id")
                    ? "invalid:" + BrokenProvider.class.getName() : "broken";
            Probe probe = probes.get(diagnostic);
            require(probe != null && !probe.available()
                    && probe.reason().contains(ServiceConfigurationError.class.getName()),
                    "metadata failure was not recorded: " + diagnostic);
            expectType(scenario.equals("id") ? IllegalArgumentException.class
                    : IllegalStateException.class, () -> Backend.of("broken"));
        }
        require(Backend.of("a-healthy").id().equals("a-healthy"), "healthy provider lost");
        require(Backend.select(Backend.Policy.BEST).id().equals(
                supportsFailure ? "broken" : "unsupported"), "legacy ranking changed");
        require(SUPPORT_CALLS.isEmpty(), "legacy selection consulted supports");
        require(Backend.select(Backend.Policy.REFERENCE, add) == reference,
                "reference policy changed");
        require(SUPPORT_CALLS.isEmpty(), "reference selection consulted optional providers");

        List<String> expected = supportsFailure
                ? List.of("broken", "unsupported", "a-healthy")
                : List.of("unsupported", "a-healthy");
        Backend selected = null;
        for (Backend.Policy policy : List.of(Backend.Policy.BEST, Backend.Policy.ACCELERATOR)) {
            SUPPORT_CALLS.clear();
            selected = Backend.select(policy, add);
            require(selected.id().equals("a-healthy"), "healthy identifier tie-break changed");
            require(SUPPORT_CALLS.equals(expected),
                    "callbacks were not lazy and ordered: " + SUPPORT_CALLS);
        }
        require(broken.preparations.get() == 0 && healthy.preparations.get() == 0,
                "selection prepared a device program");
        runHealthyPlan(selected);
        require(healthy.preparations.get() == 1 && healthy.closes.get() == 1,
                "healthy plan did not own and close exactly one program");

        if (supportsFailure) {
            Backend explicit = Backend.of("broken");
            Throwable failure = selectedFailure();
            expectSame(failure, () -> explicit.supports(add));
            expectSame(failure, () -> addPlan(explicit,
                    IntBuffer.of(1), IntBuffer.of(2), IntBuffer.allocate(1)));
            require(broken.preparations.get() == 0, "explicit failure allocated a program");
            require(healthy.preparations.get() == 1,
                    "explicit failure fell back to the healthy engine");
        }
    }

    private static void runHealthyPlan(Backend backend) {
        int[] left = {Integer.MAX_VALUE, Integer.MIN_VALUE, -1, 7, 0};
        int[] right = {1, -1, -1, 9, 0};
        int[] expected = {Integer.MIN_VALUE, Integer.MAX_VALUE, -2, 16, 0};
        IntBuffer output = IntBuffer.allocate(left.length);
        try (Plan plan = addPlan(backend, IntBuffer.of(left), IntBuffer.of(right), output)) {
            Verified receipt = Verified.run(plan);
            require(receipt.backend().equals("a-healthy"), "execution changed its backend");
            require(Arrays.equals(expected, output.toArray()), "healthy arithmetic mismatch");
        }
    }

    private static void assertionPropagates(KernelId add) {
        expectSame(ASSERTION_FAILURE, () -> Backend.select(Backend.Policy.BEST, add));
        require(SUPPORT_CALLS.equals(List.of("broken")),
                "selection continued after an AssertionError");
        require(healthy.preparations.get() == 0, "assertion failure executed another engine");
    }

    private static void referenceFailurePropagates(Backend reference) {
        KernelId kernel = JavaKernels.register(KernelId.of("test.reference-callback-failure"),
                (index, buffers) -> { throw REFERENCE_FAILURE; });
        require(Backend.select(Backend.Policy.REFERENCE, kernel) == reference,
                "reference policy did not retain the reference");
        require(SUPPORT_CALLS.isEmpty(), "reference policy consulted optional supports");
        try (Plan plan = Plan.builder(reference).kernel(kernel).grid(Grid1D.of(1, 1))
                .bind(IntBuffer.allocate(1), Transfer.ON_DEMAND).build()) {
            expectType(IllegalStateException.class, plan::execute);
            require(plan.session().launches() == 0, "failed reference launch was counted");
        }
        require(healthy.preparations.get() == 0, "reference failure used another engine");
    }

    private static Plan addPlan(Backend backend, IntBuffer left, IntBuffer right, IntBuffer sum) {
        return Plan.builder(backend).kernel(JavaKernels.intAdd()).grid(Grid1D.of(sum.length(), 4))
                .bind(left, Transfer.ONCE).bind(right, Transfer.ONCE)
                .bind(sum, Transfer.ON_DEMAND).build();
    }

    private static Throwable selectedFailure() {
        return switch (scenario) {
            case "supports-runtime" -> RUNTIME_FAILURE;
            case "supports-linkage" -> LINKAGE_FAILURE;
            case "supports-assertion", "reference-error" -> ASSERTION_FAILURE;
            default -> SERVICE_FAILURE;
        };
    }

    private static void failAt(String callback) {
        if (scenario.equals(callback) || (callback.equals("supports")
                && (scenario.startsWith("supports-") || scenario.equals("reference-error")))) {
            Throwable failure = selectedFailure();
            if (failure instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw (Error) failure;
        }
    }

    public static final class BrokenProvider extends BackendSwapTest.CpuProvider {
        public BrokenProvider() {
            super("broken", 1000);
            broken = this;
        }

        @Override
        public String id() {
            failAt("id");
            return super.id();
        }

        @Override
        public Set<Backend.Capability> capabilities() {
            failAt("capabilities");
            return super.capabilities();
        }

        @Override
        public int score() {
            failAt("score");
            return super.score();
        }

        @Override
        public Probe probe() {
            failAt("probe");
            return super.probe();
        }

        @Override
        public boolean supports(KernelId kernel) {
            SUPPORT_CALLS.add("broken");
            failAt("supports");
            return super.supports(kernel);
        }
    }

    public static final class UnsupportedProvider extends BackendSwapTest.CpuProvider {
        public UnsupportedProvider() {
            super("unsupported", 800);
        }

        @Override
        public boolean supports(KernelId kernel) {
            SUPPORT_CALLS.add("unsupported");
            return false;
        }
    }

    public static final class HealthyProvider extends BackendSwapTest.CpuProvider {
        public HealthyProvider() {
            super("a-healthy", 500);
            healthy = this;
        }

        @Override
        public boolean supports(KernelId kernel) {
            SUPPORT_CALLS.add("a-healthy");
            return super.supports(kernel);
        }
    }

    public static final class HealthyZProvider extends BackendSwapTest.CpuProvider {
        public HealthyZProvider() {
            super("z-healthy", 500);
        }

        @Override
        public boolean supports(KernelId kernel) {
            throw new AssertionError("equal-score later identifier was consulted");
        }
    }

    public static final class LowerProvider extends BackendSwapTest.CpuProvider {
        public LowerProvider() {
            super("lower", 100);
        }

        @Override
        public boolean supports(KernelId kernel) {
            throw new AssertionError("lower-ranked provider was consulted");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectSame(Throwable expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (failure == expected) {
                return;
            }
            throw new AssertionError("exception identity changed", failure);
        }
        throw new AssertionError("missing " + expected.getClass().getName());
    }

    private static void expectType(Class<? extends Throwable> expected, Runnable action) {
        try {
            action.run();
        } catch (Throwable failure) {
            if (expected.isInstance(failure)) {
                return;
            }
            throw new AssertionError("unexpected exception type", failure);
        }
        throw new AssertionError("missing " + expected.getName());
    }
}
