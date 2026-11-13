/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
// Adapted from the existing bounded Apache circular-FIFO proof launcher.
import java.io.PrintWriter;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

public final class CollectionTestLauncher {
    private CollectionTestLauncher() { }
    public static void main(String[] args) {
        if (args.length != 2) throw new IllegalArgumentException("selector and expected count required");
        DiscoverySelector selector = args[0].contains("#")
                ? DiscoverySelectors.selectMethod(args[0]) : DiscoverySelectors.selectClass(args[0]);
        long expected = Long.parseLong(args[1]);
        var listener = new SummaryGeneratingListener();
        var request = LauncherDiscoveryRequestBuilder.request().selectors(selector).build();
        LauncherFactory.create().execute(request, listener);
        var writer = new PrintWriter(System.out, true);
        var summary = listener.getSummary();
        summary.printTo(writer);
        summary.printFailuresTo(writer);
        System.out.printf("RUNTIME_TESTS found=%d passed=%d failed=%d aborted=%d skipped=%d%n",
                summary.getTestsFoundCount(), summary.getTestsSucceededCount(), summary.getTestsFailedCount(),
                summary.getTestsAbortedCount(), summary.getTestsSkippedCount());
        if (expected <= 0 || summary.getTestsFoundCount() != expected
                || summary.getTestsSucceededCount() != expected || summary.getTestsFailedCount() != 0
                || summary.getTestsAbortedCount() != 0 || summary.getTestsSkippedCount() != 0) {
            throw new AssertionError("actual JUnit execution did not satisfy exact count/zero-failure contract");
        }
    }
}
