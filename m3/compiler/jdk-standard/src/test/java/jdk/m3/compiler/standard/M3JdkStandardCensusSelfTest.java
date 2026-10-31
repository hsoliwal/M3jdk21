// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
package jdk.m3.compiler.standard;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Parse-only census tests with unresolved dependencies, exclusions, and syntax refusal. */
public final class M3JdkStandardCensusSelfTest {
    private M3JdkStandardCensusSelfTest() {
    }

    /**
     * Runs the census fixtures.
     *
     * @param args unused
     * @throws IOException if fixture creation or census execution fails
     */
    public static void main(String[] args) throws IOException {
        Path root = Files.createTempDirectory("m3jdk-standard-census");
        Path owned = root.resolve(
                "m3/demo/src/main/java/jdk/m3/demo/Helper.java");
        Path excluded = root.resolve(
                "m3/vendor/third_party/src/main/java/jdk/m3/vendor/Helper.java");
        write(owned, """
                package jdk.m3.demo;
                import missing.Unknown;
                public final class Helper {
                    public void doStuff(Unknown value) {
                    }
                }
                """);
        write(excluded, """
                package jdk.m3.vendor;
                public final class Helper {
                }
                """);

        List<Path> discovered = M3JdkStandardCensus.discoverFiles(root);
        check(discovered.equals(List.of(root.relativize(owned))),
                "unexpected discovery " + discovered);
        Path first = root.resolve("first.tsv");
        Path second = root.resolve("second.tsv");
        M3JdkStandardCensus.Result result = M3JdkStandardCensus.run(
                root, discovered, first, false);
        M3JdkStandardCensus.run(root, discovered, second, false);
        check(result.fileCount() == 1 && result.findingCount() == 2,
                "unexpected census counts " + result);
        check(Files.mismatch(first, second) == -1L, "census is not deterministic");
        String report = Files.readString(first, StandardCharsets.UTF_8);
        check(report.contains("type Helper: is a placeholder"), "missing type finding");
        check(report.contains("method doStuff: is a placeholder"),
                "missing method finding");

        Path syntaxRoot = Files.createTempDirectory("m3jdk-standard-syntax");
        Path broken = syntaxRoot.resolve(
                "m3/demo/src/main/java/jdk/m3/demo/M3Broken.java");
        write(broken, "package jdk.m3.demo; public final class M3Broken {\n");
        try {
            M3JdkStandardCensus.run(syntaxRoot, List.of(broken),
                    syntaxRoot.resolve("broken.tsv"), false);
            throw new AssertionError("syntax error was accepted");
        } catch (IOException expected) {
            check(expected.getMessage().contains("parse errors"),
                    "wrong syntax refusal " + expected.getMessage());
        }
        System.out.println("M3JDK_STANDARD_CENSUS_SELFTEST PASS");
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError("M3JDK_STANDARD_CENSUS_SELFTEST FAIL " + failure);
        }
    }
}
