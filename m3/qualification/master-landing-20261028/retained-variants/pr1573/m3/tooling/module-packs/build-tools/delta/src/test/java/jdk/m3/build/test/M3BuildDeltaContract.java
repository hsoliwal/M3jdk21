// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.build.test;

import com.m3.compiletime.ast.M3FileSourceFacts;
import com.m3.file.M3File.Fingerprint;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.parser.M3File;

/** Real javac fixtures for change evidence; this test implements no incremental compiler. */
public final class M3BuildDeltaContract {
    private static int checks, attempts, successes, parses, sourceReads;

    private M3BuildDeltaContract() {}

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 1) throw new IllegalArgumentException("one proof-output directory required");
        Path output = Files.createDirectories(Path.of(arguments[0]));
        bodyAndUnchangedBuild(output);
        inlinedConstantRequiresConsumer(output.resolve("constant"));
        removedNestedOutput(output.resolve("nested"));
        failedCompilation(output.resolve("failed"));
        contextOrder();
        require(parses >= 10 && sourceReads >= 10, "real parse/source-read observations");
        System.out.println("M3_BUILD_DELTA_PASS checks=" + checks + " compileAttempts=" + attempts
                + " successfulCompiles=" + successes + " parseEvents=" + parses + " sourceReads=" + sourceReads
                + " incrementalCompilationImplemented=false");
    }

    private static void bodyAndUnchangedBuild(Path output) throws Exception {
        M3File before = source("Example", "public class Example { public int value() { return 1; } }");
        M3File same = source("Example", new String(before.text()));
        Path first = output.resolve("body");
        Path repeated = output.resolve("body-repeat");
        require(compile(first, before), "initial body compile");
        require(compile(repeated, same), "equal rerun compile");
        Fingerprint oldClass = fingerprint(first.resolve("Example.class"));
        require(oldClass.equals(fingerprint(repeated.resolve("Example.class"))), "equal rerun class SHA-256");
        require(!same.changesFrom(before).charactersChanged(), "equal immutable source snapshots");
        require(facts(same, List.of()).changesFrom(facts(before, List.of())) == 0, "equal source facts");
        M3File after = source("Example", before.text().replace("return 1", "return 2"));
        require(after.changesFrom(before).charactersChanged(), "body edit source delta");
        require(facts(after, List.of()).changesFrom(facts(before, List.of()))
                == M3FileSourceFacts.SOURCE_CONTENT_CHANGED, "body edit fact mask");
        require(compile(first, after), "changed body compile");
        require(!oldClass.equals(fingerprint(first.resolve("Example.class"))), "body edit changes class bytes");
        Files.delete(repeated.resolve("Example.class"));
        requireMissingFingerprint(repeated.resolve("Example.class"));
    }

    private static void inlinedConstantRequiresConsumer(Path output) throws Exception {
        M3File api = source("Api", "public class Api { public static final int VALUE = 1; }");
        M3File client = source("Client", "public class Client { public static int value() { return Api.VALUE; } }");
        require(compile(output, api, client), "initial constant and consumer compile");
        Fingerprint previousApi = fingerprint(output.resolve("Api.class"));
        Fingerprint previousClient = fingerprint(output.resolve("Client.class"));
        require(clientValue(output) == 1, "initial inlined constant");
        M3File changedApi = source("Api", api.text().replace("VALUE = 1", "VALUE = 2"));
        require(changedApi.changesFrom(api).charactersChanged(), "constant edit source delta");
        require(facts(changedApi, List.of()).changesFrom(facts(api, List.of()))
                == M3FileSourceFacts.SOURCE_CONTENT_CHANGED, "constant edit fact mask");
        require(compile(output, changedApi), "producer-only compile");
        Fingerprint nextApi = fingerprint(output.resolve("Api.class"));
        require(!previousApi.equals(nextApi), "constant edit changes producer class");
        require(previousClient.equals(fingerprint(output.resolve("Client.class"))), "consumer remains unchanged");
        require(clientValue(output) == 1, "stale consumer still returns previous inlined constant");
        require(facts(client, List.of(nextApi.sha256())).changesFrom(facts(client, List.of(previousApi.sha256())))
                == M3FileSourceFacts.COMPILATION_CONTEXT_CHANGED, "same consumer source has changed dependency context");
        require(compile(output, client), "explicit dependent recompilation");
        require(!previousClient.equals(fingerprint(output.resolve("Client.class"))), "consumer class changed");
        require(clientValue(output) == 2, "consumer sees new constant after recompilation");
    }

    private static void removedNestedOutput(Path output) throws Exception {
        M3File before = source("Outer", "public class Outer { public static class Nested {} }");
        M3File after = source("Outer", "public class Outer {}");
        require(compile(output, before), "initial nested class compile");
        Path nested = output.resolve("Outer$Nested.class");
        Fingerprint previous = fingerprint(nested);
        require(after.changesFrom(before).charactersChanged(), "nested declaration removed");
        require(compile(output, after), "recompile owner without nested declaration");
        require(previous.equals(fingerprint(nested)), "javac leaves previously generated nested class behind");
        Files.delete(nested); // Explicit fixture reconciliation; the candidate APIs do not delete outputs.
        requireMissingFingerprint(nested);
    }

    private static void failedCompilation(Path output) throws Exception {
        int previouslySuccessful = successes;
        M3File broken = source("Broken", "public class Broken { public int value() { return ; } }");
        require(!compile(output, broken), "invalid source returns false");
        require(successes == previouslySuccessful, "failed task does not record a successful compilation");
        requireMissingFingerprint(output.resolve("Broken.class"));
    }

    private static void contextOrder() {
        M3File unchanged = source("Ordered", "public class Ordered {}");
        M3FileSourceFacts previous = facts(unchanged, List.of("classpath", "a.jar", "b.jar"));
        M3FileSourceFacts current = facts(unchanged, List.of("classpath", "b.jar", "a.jar"));
        require(!unchanged.changesFrom(unchanged).charactersChanged(), "context case has identical source");
        require(current.changesFrom(previous) == M3FileSourceFacts.COMPILATION_CONTEXT_CHANGED,
                "ordered classpath change is retained");
    }

    private static boolean compile(Path output, M3File... snapshots) throws IOException {
        Files.createDirectories(output);
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new AssertionError("full JDK required");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        List<JavaFileObject> inputs = new ArrayList<>();
        for (M3File snapshot : snapshots) inputs.add(new SnapshotSource(snapshot));
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            // These deterministic fixture flags do not change Maven/Gradle production configuration.
            var options = List.of("--release", "21", "-proc:none", "-g:none",
                    "-classpath", output.toString(), "-d", output.toString());
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics, options, null, inputs);
            task.setTaskListener(new TaskListener() {
                @Override public void finished(TaskEvent event) {
                    if (event.getKind() == TaskEvent.Kind.PARSE) parses++;
                }
            });
            attempts++;
            boolean success = task.call();
            if (success) successes++;
            return success;
        }
    }

    private static final class SnapshotSource extends SimpleJavaFileObject {
        private final M3File snapshot;
        SnapshotSource(M3File snapshot) { super(snapshot.uri(), Kind.SOURCE); this.snapshot = snapshot; }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            sourceReads++;
            return snapshot; // The compiler receives the exact immutable source owner, without a disk reread.
        }
    }

    private static M3File source(String name, String text) {
        return M3File.of(URI.create("string:///" + name + ".java"), text);
    }

    private static M3FileSourceFacts facts(M3File snapshot, List<String> context) {
        return M3FileSourceFacts.capture(snapshot.uri().toString(), snapshot,
                M3FileSourceFacts.contextSha256(context), "javac-fixture", 1, "PARSE");
    }

    private static Fingerprint fingerprint(Path path) throws IOException {
        return com.m3.file.M3File.of(path).fingerprint(1_048_576);
    }

    private static int clientValue(Path output) throws Exception {
        try (var loader = new URLClassLoader(new java.net.URL[] {output.toUri().toURL()}, null)) {
            return (int) loader.loadClass("Client").getMethod("value").invoke(null);
        }
    }

    private static void requireMissingFingerprint(Path path) throws IOException {
        try {
            fingerprint(path);
            throw new AssertionError("missing output was accepted: " + path);
        } catch (java.nio.file.NoSuchFileException expected) {
            require(!Files.exists(path), "missing output cannot provide valid fingerprint");
        }
    }

    private static void require(boolean condition, String description) {
        checks++;
        if (!condition) throw new AssertionError(description);
    }
}
