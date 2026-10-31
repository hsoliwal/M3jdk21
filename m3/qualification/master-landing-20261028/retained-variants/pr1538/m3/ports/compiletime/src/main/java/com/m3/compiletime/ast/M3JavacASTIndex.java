// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
// Modified 2026-10-20: reuse exact frozen-input source seals and event-local fallback facts.
package com.m3.compiletime.ast;

import com.synexia.mindex.compiler.ast.MIndexJavaAST;
import com.synexia.mindex.compiler.ast.MIndexASTDocument;
import com.synexia.mindex.compiler.ast.MIndexASTOccurrence;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.ast.MIndexASTPool;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.net.URI;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import javax.lang.model.element.TypeElement;

/**
 * Lazy, isolated ingress from the actual javac invocation. No parser or compiler is created here.
 * Live Trees are borrowed only inside synchronous callbacks; persistent projections are immutable.
 */
public final class M3JavacASTIndex implements AutoCloseable {
    public enum Phase { PARSE, ANALYZE, GENERATE }

    @FunctionalInterface public interface SourceResolver {
        /** Return the exact immutable characters supplied to this unit's parser. */
        String source(CompilationUnitTree unit) throws IOException;
    }

    public record Statistics(long parseEvents, long analyzeEvents, long generateEvents,
            long syntaxCaptures, long consumerCalls, boolean closed) { }

    /** The original compiler objects are valid only during this callback on the compiler thread. */
    public final class View {
        private CompilationUnitTree unit;
        private final Phase phase;
        private String source;
        private String sourceSha256;
        private TypeElement type;
        private final Thread thread = Thread.currentThread();
        private M3AST syntax;

        private View(CompilationUnitTree unit, Phase phase, String source, TypeElement type) {
            this.unit = unit;
            this.phase = phase;
            this.source = source;
            this.type = type;
        }

        public Phase phase() { requireActive(); return phase; }
        public CompilationUnitTree unit() { requireActive(); return unit; }
        public Trees trees() { requireActive(); return Trees.instance(task); }
        public JavacTask task() { requireActive(); return task; }
        public TypeElement analyzedType() { requireActive(); return type; }
        public String source() { requireActive(); return source; }

        /** Snapshot the pre-attribution syntax once, and only when a consumer requests it. */
        public M3AST ast() throws IOException {
            requireActive();
            if (phase != Phase.PARSE) throw new IllegalStateException("syntax must be frozen at PARSE");
            if (syntax == null) {
                syntax = new M3AST(MIndexJavaAST.capture(unit.getSourceFile().toUri().toString(),
                        source, unit, trees().getSourcePositions(), new MIndexASTPool(limits), release));
                syntaxCaptures++;
            }
            return syntax;
        }

        /** Acquire the lazy syntax projection through the caller-owned bounded cache. */
        public M3ASTWorkspaceCache<M3AST>.Lease acquireSyntax(M3ASTWorkspaceCache<M3AST> cache, long id)
                throws IOException {
            requireActive();
            if (phase != Phase.PARSE) throw new IllegalStateException("syntax must be frozen at PARSE");
            return Objects.requireNonNull(cache, "cache").acquire(id, facts("javac-public-tree", 1), ignored -> ast());
        }

        /** The full analyzed type identity distinguishes separate ANALYZE events in one file. */
        public M3FileSourceFacts facts(String provider, int schema) {
            requireActive();
            String identity = type == null ? provider : provider + "/type=" + type.getQualifiedName();
            URI sourceUri = unit.getSourceFile().toUri();
            String sourceName = sourceUri.toString();
            if (sourceSha256 == null) {
                if (resolver instanceof FrozenInputs frozen) {
                    sourceSha256 = frozen.sourceSha256(sourceUri, source);
                }
                if (sourceSha256 == null) {
                    sourceSha256 = M3FileSourceFacts.sourceSha256ImmutableView(source);
                }
            }
            return new M3FileSourceFacts(sourceName, sourceSha256, source.length(),
                    context.sha256(), identity, schema, phase.name());
        }

        private void requireActive() {
            if (unit == null || Thread.currentThread() != thread || closed) {
                throw new IllegalStateException("compiler view is outside its callback lifetime");
            }
        }

        private void release() { unit = null; type = null; syntax = null; source = null; sourceSha256 = null; }
    }

    private JavacTask task;
    private final M3CompilationContext context;
    private final MIndexASTLimits limits;
    private final int release;
    private SourceResolver resolver;
    private final Map<CompilationUnitTree, String> sourceByUnit = new IdentityHashMap<>();
    private final Map<Phase, List<Consumer<View>>> consumers = new EnumMap<>(Phase.class);
    private final Set<String> consumerIds = new HashSet<>();
    private final TaskListener listener = new TaskListener() {
        @Override public void finished(TaskEvent event) {
            try { dispatch(event); }
            catch (RuntimeException | Error failure) { close(); throw failure; }
        }
    };
    private boolean hasConsumers;
    private boolean started;
    private boolean closed;
    private long parseEvents;
    private long analyzeEvents;
    private long generateEvents;
    private long syntaxCaptures;
    private long consumerCalls;

    public M3JavacASTIndex(JavacTask task, M3CompilationContext context,
            SourceResolver resolver, MIndexASTLimits limits, int release) {
        this.task = Objects.requireNonNull(task, "task");
        this.context = Objects.requireNonNull(context, "context");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.limits = Objects.requireNonNull(limits, "limits");
        if (release < 7 || release > Runtime.version().feature()) {
            throw new IllegalArgumentException("unsupported source release");
        }
        this.release = release;
        for (Phase phase : Phase.values()) consumers.put(phase, new ArrayList<>());
        task.addTaskListener(listener);
    }

    /**
     * Stock-JDK route bound to inputs frozen before task creation. Javac wraps client objects;
     * URI membership and the exact immutable String instance prove the delegated input identity.
     */
    public static SourceResolver frozenInputs(Collection<M3File> inputs) {
        Map<URI, M3File> files = new HashMap<>();
        for (M3File file : List.copyOf(inputs)) {
            if (files.putIfAbsent(file.toUri(), file) != null) {
                throw new IllegalArgumentException("duplicate frozen source URI");
            }
        }
        return new FrozenInputs(Map.copyOf(files));
    }

    /** Same immutable input map as the original resolver; no second source owner or cache. */
    private static final class FrozenInputs implements SourceResolver {
        private final Map<URI, M3File> files;

        private FrozenInputs(Map<URI, M3File> files) { this.files = files; }

        @Override public String source(CompilationUnitTree unit) throws IOException {
            M3File file = files.get(unit.getSourceFile().toUri());
            if (file == null || unit.getSourceFile().getCharContent(false) != file.source()) {
                throw new IOException("source was not the admitted immutable parser input");
            }
            return file.source();
        }

        private String sourceSha256(URI uri, String source) {
            M3File file = files.get(uri);
            // A public resolver or mutable source-file identity cannot supply a trusted hash.
            // A mismatch uses the existing captured-character fallback instead of a foreign seal.
            return file != null && file.source() == source ? file.sourceSha256() : null;
        }
    }

    public M3JavacASTIndex on(Phase phase, String consumerId, Consumer<View> consumer) {
        if (closed || started) throw new IllegalStateException("register consumers before compilation");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(consumer, "consumer");
        if (consumerId == null || consumerId.isBlank() || !consumerIds.add(phase + "/" + consumerId)) {
            throw new IllegalArgumentException("empty or duplicate consumer identity");
        }
        consumers.get(phase).add(consumer);
        hasConsumers = true;
        return this;
    }

    public M3CompilationContext context() { return context; }
    public Statistics statistics() {
        return new Statistics(parseEvents, analyzeEvents, generateEvents, syntaxCaptures, consumerCalls, closed);
    }

    private void dispatch(TaskEvent event) {
        if (closed) return;
        if (event.getKind() == TaskEvent.Kind.COMPILATION) { close(); return; }
        Phase phase = switch (event.getKind()) {
            case PARSE -> Phase.PARSE;
            case ANALYZE -> Phase.ANALYZE;
            case GENERATE -> Phase.GENERATE;
            default -> null;
        };
        CompilationUnitTree unit = event.getCompilationUnit();
        if (phase == null || unit == null) return;
        started = true;
        if (phase == Phase.PARSE) {
            parseEvents++;
            if (!hasConsumers) return;
            // All consumers share this same immutable source object and compiler generation.
            try {
                String source = Objects.requireNonNull(resolver.source(unit), "resolved source");
                if (source.length() > limits.sourceChars()) throw new IOException("source exceeds AST budget");
                sourceByUnit.put(unit, source);
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        } else if (phase == Phase.ANALYZE) { analyzeEvents++; }
        else { generateEvents++; }
        if (!hasConsumers) return;
        String source = sourceByUnit.get(unit);
        if (source == null) throw new IllegalStateException("unit was not observed at PARSE");
        View view = new View(unit, phase, source, event.getTypeElement());
        try {
            for (Consumer<View> consumer : List.copyOf(consumers.get(phase))) {
                consumer.accept(view);
                consumerCalls++;
            }
        } finally { view.release(); }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        task.removeTaskListener(listener);
        sourceByUnit.clear();
        consumers.values().forEach(List::clear);
        consumerIds.clear();
        hasConsumers = false;
        resolver = null;
        task = null;
    }
}
