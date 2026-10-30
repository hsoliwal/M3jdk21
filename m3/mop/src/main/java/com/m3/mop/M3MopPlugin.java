// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * javac Plugin {@code -Xplugin:"M3Mop out=DIR [root=DIR] [registry=FILE] [register]"}: atomize and
 * patternize the compiler's own AST of every compiled source.
 *
 * <p>The projection is taken when PARSE finishes, before attribution rewrites trees (implicit
 * {@code super()} calls, elided annotation {@code value=}), so it is byte-identical to the parse-only
 * CLI. A unit is published only after its ANALYZE event, i.e. after javac attributed and flow
 * checked it; units that never reach ANALYZE are not published. Output is written once,
 * when COMPILATION finishes. The compiler tree is only read, never mutated.</p>
 */
//FLLDCIM: MOP — no second AST: atomize/patternize run inside javac on its own trees
public final class M3MopPlugin implements Plugin {
    private final Map<String, M3Atomizer.Unit> parsed = new TreeMap<>();
    private final Set<String> analyzed = new HashSet<>();
    private Map<String, String> options = Map.of();
    private SourcePositions positions;
    private boolean published;

    @Override
    public String getName() {
        return "M3Mop";
    }

    @Override
    public void init(JavacTask task, String... args) {
        Objects.requireNonNull(task, "task");
        if (positions != null) throw new IllegalStateException("plugin instance already initialized");
        options = options(args);
        positions = Trees.instance(task).getSourcePositions();
        task.addTaskListener(new TaskListener() {
            @Override
            public void finished(TaskEvent event) {
                M3MopPlugin.this.finished(event);
            }
        });
    }

    private static Map<String, String> options(String... args) {
        Map<String, String> parsed = Stream.of(args).collect(Collectors.toMap(arg -> arg.split("=", 2)[0],
                arg -> arg.contains("=") ? arg.split("=", 2)[1] : "true"));
        if (!parsed.containsKey("out") || !Set.of("out", "root", "registry", "register")
                .containsAll(parsed.keySet())) {
            throw new IllegalArgumentException("usage: -Xplugin:\"M3Mop out=DIR [root=DIR] [registry=FILE] [register]\"");
        }
        return parsed;
    }

    private void finished(TaskEvent event) {
        switch (event.getKind()) {
            case PARSE -> parsed(event);
            case ANALYZE -> analyzed.add(uri(event));
            case COMPILATION -> publish();
            default -> { }
        }
    }

    private void parsed(TaskEvent event) {
        Path root = Path.of(options.getOrDefault("root", "."));
        String path = M3Atomizer.logicalPath(root, event.getSourceFile());
        try {
            parsed.put(uri(event), M3Atomizer.atomize(path, event.getCompilationUnit(), positions));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private void publish() {
        if (published) return;
        published = true;
        List<M3Atomizer.Unit> units = parsed.entrySet().stream()
                .filter(entry -> analyzed.contains(entry.getKey())).map(Map.Entry::getValue).toList();
        Optional<Path> registry = Optional.ofNullable(options.get("registry")).map(Path::of);
        try {
            M3AtomizeRun.publish(units, new M3AtomizeRun.Options(Path.of(options.get("out")), registry,
                    options.containsKey("register"), "javac-plugin"));
        } catch (IOException failure) {
            throw new UncheckedIOException(failure);
        }
    }

    private static String uri(TaskEvent event) {
        return event.getSourceFile().toUri().toString();
    }
}
