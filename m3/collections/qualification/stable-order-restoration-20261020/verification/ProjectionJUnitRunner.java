/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Executes existing source-owned JUnit entrypoints without changing their assertions. */
public final class ProjectionJUnitRunner {
    private ProjectionJUnitRunner() { }

    public static void main(String[] args) {
        var request = LauncherDiscoveryRequestBuilder.request();
        for (String name : args) request.selectors(DiscoverySelectors.selectClass(name));
        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary);
        launcher.execute(request.build());
        var result = summary.getSummary();
        result.printTo(new PrintWriter(System.out, true));
        result.printFailuresTo(new PrintWriter(System.err, true));
        if (result.getTestsFoundCount() != args.length || result.getTestsSucceededCount() != args.length
                || result.getTestsFailedCount() != 0 || result.getTestsSkippedCount() != 0
                || result.getTestsAbortedCount() != 0 || result.getContainersFailedCount() != 0) {
            throw new AssertionError("Every selected existing test must execute and pass");
        }
    }
}
