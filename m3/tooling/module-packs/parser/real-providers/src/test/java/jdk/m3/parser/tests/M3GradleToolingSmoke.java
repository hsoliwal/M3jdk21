// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
package jdk.m3.parser.tests;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import jdk.m3.parser.M3ASTAuthoritativeModels;
import jdk.m3.parser.M3ASTIndex;
import jdk.m3.parser.M3ASTIngress;
import jdk.m3.parser.M3File;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.GradleProject;

/** Real Tooling API model from an actual Gradle daemon; never executes tasks. */
public final class M3GradleToolingSmoke {
    private M3GradleToolingSmoke() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one fresh project directory");
        Path project = Path.of(args[0]).toAbsolutePath().normalize();
        if (Files.exists(project)) throw new IllegalArgumentException("must be absent");
        Files.createDirectories(project.resolve("child"));
        String script = "plugins { id 'java' }\n";
        Files.writeString(project.resolve("settings.gradle"), "rootProject.name = 'm3root'\ninclude 'child'\n", StandardCharsets.UTF_8);
        Files.writeString(project.resolve("build.gradle"), script, StandardCharsets.UTF_8);
        Files.writeString(project.resolve("child/build.gradle"), "", StandardCharsets.UTF_8);
        try (ProjectConnection connection = GradleConnector.newConnector()
                .forProjectDirectory(project.toFile()).useGradleVersion("8.12").connect()) {
            GradleProject original = connection.getModel(GradleProject.class);
            if (original.getChildren().size() != 1) throw new AssertionError("Gradle original child project");
            M3ASTIndex index = M3ASTAuthoritativeModels.gradleTooling(
                    M3File.of(URI.create("memory:/build.gradle"), script), original,
                    new M3ASTIngress.Limits(2_000_000, 100_000, 256));
            int projects = 0;
            int tasks = 0;
            for (int i = 0; i < index.nodeCount(); i++) {
                if ("GRADLE_TOOLING_PROJECT".equals(index.kind(i))) projects++;
                if ("GRADLE_TOOLING_TASK".equals(index.kind(i))) tasks++;
                if (index.hasExactRange(i)) throw new AssertionError("Gradle resolved model has no script source span");
            }
            if (projects != 2 || tasks < 1) {
                throw new AssertionError("real Gradle Tooling model mismatch: projects=" + projects + ", tasks=" + tasks);
            }
            System.out.println("M3_AST_REAL_GRADLE_TOOLING_PASS projects=" + projects
                    + " tasks=" + tasks + " nodes=" + index.nodeCount());
        }
    }
}
