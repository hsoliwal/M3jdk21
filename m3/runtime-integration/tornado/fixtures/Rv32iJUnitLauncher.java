/*
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Runs unchanged donor and target tests with the source-built JUnit engine. */
public final class Rv32iJUnitLauncher {
    private Rv32iJUnitLauncher() {
    }

    public static void main(String[] args) {
        if (args.length != 1 || !(args[0].equals("cpu") || args[0].equals("device"))) {
            throw new IllegalArgumentException("expected cpu or device mode");
        }
        boolean device = args[0].equals("device");
        if (Boolean.getBoolean("m3.gpu") != device) {
            throw new AssertionError("original-suite hardware switch does not match requested mode");
        }
        var request = LauncherDiscoveryRequestBuilder.request().selectors(
                DiscoverySelectors.selectClass("com.synexia.m3.cpugpu.CpuRv32iEngineTest"),
                DiscoverySelectors.selectClass("com.synexia.m3.cpugpu.Elf32RiscVLoaderTest"),
                DiscoverySelectors.selectClass("com.synexia.m3.cpugpu.Rv32iAssemblerTest"),
                DiscoverySelectors.selectClass("com.synexia.m3.cpugpu.Rv32iBulkExecutorTest"),
                DiscoverySelectors.selectClass("com.synexia.m3.cpugpu.TornadoRv32iDifferentialTest"),
                DiscoverySelectors.selectClass("com.m3.tornado.Rv32iBulkExecutorAdapterTest")).build();
        var listener = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);
        var summary = listener.getSummary();
        var writer = new PrintWriter(System.out, true);
        summary.printTo(writer);
        summary.printFailuresTo(writer);
        long successes = device ? 19 : 16;
        long aborts = device ? 0 : 2;
        long skips = device ? 0 : 1;
        if (summary.getTestsFoundCount() != 19 || summary.getTestsFailedCount() != 0
                || summary.getTestsSucceededCount() != successes || summary.getTestsAbortedCount() != aborts
                || summary.getTestsSkippedCount() != skips) {
            throw new AssertionError("expected 19 original tests in " + args[0] + " mode: " + successes
                    + " successes, " + aborts + " hardware assumptions, " + skips + " hardware skips");
        }
        if (device) {
            System.out.println("M3_RV32IM_ORIGINAL_JUNIT_DEVICE_PASS found=19 successful=19 aborted=0 skipped=0 failed=0");
        } else {
            System.out.println("M3_RV32IM_ORIGINAL_JUNIT_PASS found=19 successful=16 aborted=2 skipped=1 failed=0");
        }
    }
}
