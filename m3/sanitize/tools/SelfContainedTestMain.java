/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
//FLLDCIM: IOP - self-contained gate test runner; JUnit Platform launcher compiled from m3/vendor/third_party sources

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

/**
 * Runs every JUnit Platform test under one test-classes root with the surefire default class-name includes and
 * writes one TSV summary line plus one line per failure to the result file.
 * Usage: {@code java -cp <repo-built classpath> SelfContainedTestMain <test-classes-dir> <result.tsv>}.
 * Exit status 0 when no test failed, 1 otherwise. No output on stdout: the gate reads the result file.
 */
public final class SelfContainedTestMain {

    /** Surefire 3 default includes: Test*, *Test, *Tests, *TestCase (nested classes included). */
    private static final String INCLUDES = "^(.*\\.)?(Test[^.]*|[^.]*Test|[^.]*Tests|[^.]*TestCase)(\\$.*)?$";

    private SelfContainedTestMain() {
    }

    public static void main(String[] args) throws IOException {
        TestExecutionSummary summary = run(Path.of(args[0]));
        Files.writeString(Path.of(args[1]), render(summary));
        System.exit(summary.getTotalFailureCount() == 0 ? 0 : 1);
    }

    static TestExecutionSummary run(Path root) {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClasspathRoots(Set.of(root)))
                .filters(ClassNameFilter.includeClassNamePatterns(INCLUDES))
                .build();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory.create().execute(request, listener);
        return listener.getSummary();
    }

    static String render(TestExecutionSummary s) {
        String head = String.join("\t", "found", Long.toString(s.getTestsFoundCount()), "started",
                Long.toString(s.getTestsStartedCount()), "succeeded", Long.toString(s.getTestsSucceededCount()),
                "failed", Long.toString(s.getTestsFailedCount()), "aborted", Long.toString(s.getTestsAbortedCount()),
                "skipped", Long.toString(s.getTestsSkippedCount()), "containers_failed",
                Long.toString(s.getContainersFailedCount()));
        String failures = s.getFailures().stream()
                .map(f -> "FAIL\t" + f.getTestIdentifier().getUniqueId() + "\t"
                        + String.valueOf(f.getException()).replace('\t', ' ').replace('\n', ' '))
                .collect(Collectors.joining("\n"));
        return head + "\n" + failures + (failures.isEmpty() ? "" : "\n");
    }
}
