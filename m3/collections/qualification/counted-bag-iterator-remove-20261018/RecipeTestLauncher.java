/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
import java.io.PrintWriter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
public final class RecipeTestLauncher {
    private RecipeTestLauncher() { }
    public static void main(String[] args) {
        var summary = new SummaryGeneratingListener();
        var request = LauncherDiscoveryRequestBuilder.request()
            .selectors(DiscoverySelectors.selectClass("com.synexia.rewrite.M3CountedBagIteratorRemoveRecipeTest")).build();
        LauncherFactory.create().execute(request, summary);
        var writer = new PrintWriter(System.out, true);
        summary.getSummary().printTo(writer);
        summary.getSummary().printFailuresTo(writer);
        if (summary.getSummary().getTestsFoundCount() != 2 || summary.getSummary().getTestsSucceededCount() != 2
                || summary.getSummary().getTestsFailedCount() != 0) throw new AssertionError("receiving test admission failed");
    }
}
