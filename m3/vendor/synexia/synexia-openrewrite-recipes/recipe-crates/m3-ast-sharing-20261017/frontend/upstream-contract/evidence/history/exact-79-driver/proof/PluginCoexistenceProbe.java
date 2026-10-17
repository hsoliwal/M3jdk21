// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Separate finite probe; the pinned upstream contract test remains unmodified. */
public final class PluginCoexistenceProbe {
    private static int checks;
    private PluginCoexistenceProbe() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("one isolated output directory required");
        Path work = Path.of(args[0]).toAbsolutePath();
        Files.createDirectories(work);
        var providers = new HashMap<String, Integer>();
        for (Plugin plugin : ServiceLoader.load(Plugin.class)) providers.merge(plugin.getName(), 1, Integer::sum);
        check(providers.getOrDefault("M3AST", 0) == 1, "one builtin M3AST provider");
        check(providers.getOrDefault("M3ASTIndex", 0) == 1, "one external M3ASTIndex provider");
        Path trace = work.resolve("builtin.tsv");
        Path index = work.resolve("index");
        JavaFileObject source = new SimpleJavaFileObject(URI.create("string:///Coexist.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignored) {
                return new StringBuilder("class Coexist { static final int V=6*7; String s=\"a\"+\"b\"; } class Peer {}");
            }
        };
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        JavacTask task = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics,
                List.of("-proc:none", "-d", work.toString(), "-Xplugin:M3AST trace=" + trace,
                        "-Xplugin:M3ASTIndex out=" + index), null, List.of(source));
        AtomicInteger parses = new AtomicInteger();
        task.addTaskListener(new TaskListener() {
            @Override public void finished(TaskEvent event) {
                if (event.getKind() == TaskEvent.Kind.PARSE) parses.incrementAndGet();
            }
        });
        check(task.call(), "both plugins compile one real task: " + diagnostics.getDiagnostics());
        check(parses.get() == 1, "one real parse with both plugins");
        check(Files.isRegularFile(work.resolve("Coexist.class")) && Files.isRegularFile(work.resolve("Peer.class")),
                "both real top-level class outputs exist");
        String builtin = Files.readString(trace);
        check(builtin.contains("parsed=1 analyzed=2") && builtin.contains("completed=true") && builtin.contains("errors=0"),
                "builtin summary reports the actual task");
        List<Path> sessions;
        try (var paths = Files.list(index)) { sessions = paths.filter(Files::isDirectory).toList(); }
        check(sessions.size() == 1, "one external plugin session");
        Path session = sessions.getFirst();
        String receipt = Files.readString(session.resolve("receipt.json"));
        check(receipt.contains("\"parseEvents\": 1") && receipt.contains("\"analyzeEvents\": 2")
                && receipt.contains("\"syntaxCaptures\": 1") && receipt.contains("\"compilerErrors\": 0"),
                "external summary reports the same parse/type events");
        try (var paths = Files.list(session)) {
            check(paths.filter(path -> path.getFileName().toString().endsWith(".m3ast")).count() == 1,
                    "one immutable syntax export");
        }
        System.out.println("M3_AST_PLUGIN_COEXISTENCE checks=" + checks + " parses=" + parses.get());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
