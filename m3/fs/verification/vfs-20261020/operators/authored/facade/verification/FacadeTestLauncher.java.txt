/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Launches the actual JUnit engine without changing the receiver's Maven configuration. */
public final class FacadeTestLauncher {
    private FacadeTestLauncher() {
    }

    public static void main(String[] args) {
        var builder = LauncherDiscoveryRequestBuilder.request();
        for (String name : args) {
            builder.selectors(DiscoverySelectors.selectClass(name));
        }
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(builder.build());
        var summary = listener.getSummary();
        PrintWriter writer = new PrintWriter(System.out, true);
        summary.printTo(writer);
        summary.printFailuresTo(writer);
        if (summary.getTestsFailedCount() > 0 || summary.getTestsSucceededCount() == 0) {
            System.exit(1);
        }
    }
}
