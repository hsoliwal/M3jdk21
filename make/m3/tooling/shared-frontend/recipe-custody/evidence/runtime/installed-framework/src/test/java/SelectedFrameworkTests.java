import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

/** Qualification harness: runs the unchanged SDK-emitted tests against an installed pack. */
public final class SelectedFrameworkTests {
    private SelectedFrameworkTests() { }

    public static void main(String[] args) throws Exception {
        Path installed = Path.of(args[0]).toRealPath();
        for (String name : List.of("org.openrewrite.Recipe",
                "org.openrewrite.config.Environment", "org.openrewrite.java.JavaParser",
                "org.openrewrite.json.JsonParser", "org.openrewrite.yaml.YamlParser",
                "org.openrewrite.xml.XmlParser", "org.openrewrite.properties.PropertiesParser",
                "org.openrewrite.hcl.HclParser", "org.openrewrite.protobuf.ProtoParser",
                "org.openrewrite.groovy.GroovyParser", "org.openrewrite.gradle.GradleParser",
                "org.openrewrite.maven.MavenParser", "org.m3jdk.tooling.rewrite.M3RewriteTool",
                "org.m3jdk.tooling.rewrite.M3RewriteFrontend")) {
            URI origin = Class.forName(name).getProtectionDomain().getCodeSource().getLocation().toURI();
            Path file = Path.of(origin).toRealPath();
            if (!file.startsWith(installed)) {
                throw new AssertionError("Runtime class escaped installed bundle: " + name + " " + file);
            }
            System.out.println("INSTALLED_CLASS " + name + " " + file);
        }
        var summary = new SummaryGeneratingListener();
        var launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(summary, new TestExecutionListener() {
            @Override public void executionFinished(TestIdentifier identifier, TestExecutionResult result) {
                if (identifier.isTest()) {
                    System.out.println("FRAMEWORK_TEST " + identifier.getDisplayName() + " " + result.getStatus());
                }
            }
        });
        launcher.execute(LauncherDiscoveryRequestBuilder.request()
                .selectors(DiscoverySelectors.selectClass("org.m3jdk.tooling.rewrite.M3RewriteRuntimeTest"))
                .build());
        var results = summary.getSummary();
        results.printTo(new PrintWriter(System.out));
        results.printFailuresTo(new PrintWriter(System.out));
        System.out.println("M3_SELECTED_RUNTIME_TESTS found=" + results.getTestsFoundCount()
                + " succeeded=" + results.getTestsSucceededCount()
                + " failed=" + results.getTestsFailedCount()
                + " aborted=" + results.getTestsAbortedCount()
                + " skipped=" + results.getTestsSkippedCount());
        if (results.getTestsFoundCount() != 11 || results.getTestsSucceededCount() != 11
                || results.getTestsFailedCount() != 0 || results.getTestsAbortedCount() != 0
                || results.getTestsSkippedCount() != 0 || results.getContainersFailedCount() != 0) {
            throw new AssertionError("Installed runtime did not pass the complete eleven-test contract");
        }
    }
}
