/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.sun.tools.javac.m3;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.HashMap;
import javax.tools.JavaFileObject;
import java.util.Objects;
import javax.lang.model.element.TypeElement;

/** The existing javac task owns M3 capture; no global parser or AST cache. */
public final class M3ASTPC implements Plugin {
    private static final Context.Key<Session> KEY = new Context.Key<>();

    public M3ASTPC() { }

    @Override public String getName() { return "M3AST"; }

    @Override public void init(JavacTask task, String... args) {
        boolean retainSyntax = false;
        Path trace = null;
        Path image = null;
        Path slots = null;
        boolean fold = false;
        for (String arg : args) {
            if (arg.equals("lossless") && !retainSyntax) retainSyntax = true;
            else if (arg.equals("fold-expressions") && !fold) fold = true;
            else if (arg.startsWith("trace=") && trace == null)
                trace = Path.of(arg.substring("trace=".length()));
            else if (arg.startsWith("image=") && image == null)
                image = Path.of(arg.substring("image=".length()));
            else if (arg.startsWith("slots=") && slots == null)
                slots = Path.of(arg.substring("slots=".length()));
            else throw new IllegalArgumentException("unknown or duplicate M3AST option: " + arg);
        }
        if (image == null && (slots != null || fold))
            throw new IllegalArgumentException("M3AST slots/fold-expressions require image");
        if (trace != null && (sameOutput(trace, image) || sameOutput(trace, slots)))
            throw new IllegalArgumentException("M3AST trace must differ from image and slots");
        Session session = (Session) open(task, retainSyntax);
        try {
            // Reuse javac's actual processor/plugin loader, including module paths.
            var loader = JavacProcessingEnvironment.instance(session.context)
                    .getServiceLoader(M3AST.Consumer.class);
            for (M3AST.Consumer consumer : loader) session.addConsumer(consumer);
            if (image != null)
                session.addConsumer(M3PrimitiveImageCompiler.consumer(image, slots, fold));
            if (trace != null) session.addConsumer(new Trace(trace));
        } catch (IOException failure) {
            session.abort(failure);
            throw new UncheckedIOException(failure);
        } catch (RuntimeException | Error failure) {
            session.abort(failure);
            throw failure;
        }
    }

    private static boolean sameOutput(Path left, Path right) {
        return right != null && left.toAbsolutePath().normalize()
                .equals(right.toAbsolutePath().normalize());
    }

    /**
     * Internal, version-bound bridge for converters using javac internals.
     * Public consumers use Unit.tree()/trees(); this requires an explicit
     * jdk.compiler export and the converter must use the same compiler loader.
     */
    public static Context contextOf(M3AST.Unit unit) {
        if (!(unit instanceof LiveUnit live))
            throw new IllegalArgumentException("foreign M3AST unit implementation");
        live.check();
        return live.session.context;
    }

    /** Whether syntax retention was enabled before this task started. */
    public static boolean retainsSyntax(M3AST.Unit unit) {
        if (!(unit instanceof LiveUnit live))
            throw new IllegalArgumentException("foreign M3AST unit implementation");
        live.check();
        return live.session.syntaxRetained;
    }

    /** Bind M3 source identity to the exact characters handed to javac's parser. */
    public static CharSequence retainSource(Context context, JavaFileObject file, CharSequence text) {
        Session session = context.get(KEY);
        if (session == null || session.closed || text == null) return text;
        try {
            M3AST.Source source = retained(file.toUri(), text.toString());
            Seal seal = new Seal(source);
            Seal previous = session.seals.putIfAbsent(source.uri(), seal);
            if (previous != null && !previous.sameSeal(seal))
                throw new IllegalStateException("M3AST source changed within compilation: " + source.uri());
            session.activeSource = source;
            session.captures++;
            return source.text();
        } catch (RuntimeException | Error failure) {
            // A COMPILATION event can follow this failure from javac's finally block.
            // Close before that event can authorize a consumer's output publication.
            session.abort(failure);
            throw failure;
        }
    }

    /** Verified immutable source may be reused without retaining its payload strongly. */
    private static final class Seal extends WeakReference<String> {
        private final int length;
        private final String digest;

        Seal(M3AST.Source source) {
            super(source.text());
            length = source.text().length();
            digest = source.utf16Sha256();
        }

        int length() { return length; }
        String digest() { return digest; }
        boolean sameSeal(Seal other) {
            return length == other.length && digest.equals(other.digest);
        }
    }

    private static M3AST.Source retained(URI uri, String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Encode exact code units, including lone surrogates, in a bounded buffer.
            byte[] block = new byte[Math.min(text.length(), 4096) * 2];
            for (int offset = 0; offset < text.length();) {
                int count = Math.min(text.length() - offset, block.length / 2);
                for (int index = 0; index < count; index++) {
                    char value = text.charAt(offset + index);
                    block[index * 2] = (byte) (value >>> 8);
                    block[index * 2 + 1] = (byte) value;
                }
                digest.update(block, 0, count * 2);
                offset += count;
            }
            return new M3AST.Source(uri, text, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException impossible) {
            throw new InternalError(impossible);
        }
    }

    /** Read plugin selection from the original task without creating compiler owners. */
    public static boolean isPluginSelected(JavacTask task) {
        Objects.requireNonNull(task, "task");
        if (!(task instanceof BasicJavacTask javac))
            throw new IllegalArgumentException("M3AST requires the active javac task");
        Context context = javac.getContext();
        if (context == null) throw new IllegalStateException("closed javac context");
        Arguments arguments = context.get(Arguments.argsKey);
        if (arguments == null) throw new IllegalStateException("javac arguments not initialized");
        for (var plugin : arguments.getPluginOpts()) {
            if ("M3AST".equals(plugin.head)) return true;
        }
        return false;
    }

    public static M3AST open(JavacTask task, boolean retainSyntax) {
        Objects.requireNonNull(task, "task");
        if (!(task instanceof BasicJavacTask javac))
            throw new IllegalArgumentException("M3AST requires the active javac task");
        Context context = javac.getContext();
        if (context == null) throw new IllegalStateException("closed javac context");
        Session session = context.get(KEY);
        if (session == null) {
            session = new Session(task, context);
            context.put(KEY, session);
        }
        if (session.closed) throw new IllegalStateException("M3AST session closed");
        if (retainSyntax) session.retainSyntax();
        return session;
    }

    private static final class Session implements M3AST, TaskListener {
        private JavacTask task;
        private Context context;
        private Trees trees;
        private final Object identity = new Object();
        private final ArrayList<M3AST.Consumer> consumers = new ArrayList<>();
        private long parsed, analyzed, captures, reads, computations, hits;
        private final HashMap<URI, Seal> seals = new HashMap<>();
        private M3AST.Source activeSource;
        private int errors;
        private boolean started, completed, closed, failed, syntaxRetained;

        Session(JavacTask task, Context context) {
            this.task = task;
            this.context = context;
            task.addTaskListener(this);
        }

        void retainSyntax() {
            if (started) throw new IllegalStateException("syntax policy requires an unstarted task");
            Options.instance(context).put("allowStringFolding", "false");
            var compiler = com.sun.tools.javac.main.JavaCompiler.instance(context);
            compiler.keepComments = true;
            compiler.genEndPos = true;
            syntaxRetained = true;
        }

        @Override public void addConsumer(M3AST.Consumer consumer) {
            Objects.requireNonNull(consumer, "consumer");
            if (closed || started) throw new IllegalStateException("register before compilation");
            for (M3AST.Consumer existing : consumers)
                if (existing == consumer) throw new IllegalArgumentException("duplicate consumer");
            consumers.add(consumer);
        }

        @Override public void started(TaskEvent event) { started = true; }

        @Override public void finished(TaskEvent event) {
            if (closed) return;
            switch (event.getKind()) {
                case PARSE -> {
                    parsed++;
                    try { dispatch(event, M3AST.Phase.PARSED); }
                    finally { activeSource = null; }
                }
                case ANALYZE -> { analyzed++; dispatch(event, M3AST.Phase.ANALYZED); }
                case COMPILATION -> {
                    completed = true;
                    try { close(); }
                    catch (Throwable failure) { throw new ClientCodeException(failure); }
                }
                default -> { }
            }
        }

        private void dispatch(TaskEvent event, M3AST.Phase phase) {
            if (consumers.isEmpty()) return;
            if (trees == null) trees = Trees.instance(task);
            LiveUnit unit = new LiveUnit(this, event.getCompilationUnit(),
                    event.getTypeElement(), phase);
            try {
                for (int index = 0; index < consumers.size() && !closed; index++) {
                    M3AST.Consumer consumer = consumers.get(index);
                    if (phase == M3AST.Phase.PARSED) consumer.parsed(unit);
                    else consumer.analyzed(unit);
                }
            } catch (Throwable failure) {
                abort(failure);
                // Preserve javac's normal user TaskListener exception path.
                // This owner lives in a trusted compiler package, so its own
                // callback is not wrapped by ClientCodeWrapper automatically.
                throw new ClientCodeException(failure);
            } finally {
                unit.release();
            }
        }

        @Override public M3AST.Summary summary() {
            int currentErrors = context == null ? errors : Log.instance(context).nerrors;
            return new M3AST.Summary(parsed, analyzed, captures, reads, computations, hits,
                    currentErrors, completed, closed, failed);
        }

        void abort(Throwable original) {
            failed = true;
            try { close(); }
            catch (Throwable cleanup) { if (cleanup != original) original.addSuppressed(cleanup); }
        }

        @Override public void close() {
            if (closed) return;
            closed = true;
            errors = Log.instance(context).nerrors;
            // JavacTaskImpl.generate() can release its context before an
            // explicit client close. The retained owner is still valid here.
            MultiTaskListener.instance(context).remove(this);
            // The context may outlive this handle; the handle must not retain it.
            task = null;
            trees = null;
            context = null;
            seals.clear();
            activeSource = null;
            Throwable first = null;
            try {
                for (M3AST.Consumer consumer : consumers) {
                    try { consumer.finished(summary()); }
                    catch (Throwable failure) {
                        failed = true;
                        if (first == null) first = failure;
                        else if (failure != first) first.addSuppressed(failure);
                    }
                }
            } finally {
                consumers.clear();
            }
            if (first != null) throw propagate(first);
        }
    }

    private static RuntimeException propagate(Throwable failure) {
        if (failure instanceof Error error) throw error;
        if (failure instanceof RuntimeException runtime) return runtime;
        if (failure instanceof IOException io) return new UncheckedIOException(io);
        return new IllegalStateException("M3AST consumer failed", failure);
    }

    private static final class LiveUnit implements M3AST.Unit {
        private static final Object COMPUTING = new Object();
        private static final Object NULL = new Object();
        private record Failure(Throwable cause) { }
        private Session session;
        private CompilationUnitTree tree;
        private TypeElement type;
        private final M3AST.Phase phase;
        private final Thread thread = Thread.currentThread();
        private IdentityHashMap<M3AST.Projection<?>, Object> projections;
        private M3AST.Source source;
        private IOException sourceFailure;

        LiveUnit(Session session, CompilationUnitTree tree, TypeElement type, M3AST.Phase phase) {
            this.session = session;
            this.tree = Objects.requireNonNull(tree, "event compilation unit");
            this.type = type;
            this.phase = phase;
        }

        private void check() {
            if (session == null || session.closed || Thread.currentThread() != thread)
                throw new IllegalStateException("M3AST view is valid only during its callback");
        }

        @Override public M3AST.Phase phase() { check(); return phase; }
        @Override public CompilationUnitTree tree() { check(); return tree; }
        @Override public Trees trees() { check(); return session.trees; }
        @Override public TypeElement analyzedType() { check(); return type; }
        @Override public Object contextIdentity() { check(); return session.identity; }

        @Override public M3AST.Source source() throws IOException {
            check();
            if (sourceFailure != null) throw sourceFailure;
            if (source == null) {
                URI uri = tree.getSourceFile().toUri();
                try {
                    if (phase == M3AST.Phase.PARSED && session.activeSource != null
                            && uri.equals(session.activeSource.uri())) {
                        source = session.activeSource;
                    } else {
                        Seal seal = session.seals.get(uri);
                        if (seal == null) throw new IOException("M3AST parsed-source seal unavailable: " + uri);
                        session.reads++;
                        String text = tree.getSourceFile().getCharContent(false).toString();
                        if (text.length() != seal.length())
                            throw new IOException("M3AST source changed since parsing: " + uri);
                        String verified = seal.get();
                        if (text.equals(verified)) {
                            source = new M3AST.Source(uri, text, seal.digest());
                        } else {
                            M3AST.Source candidate = retained(uri, text);
                            if (!candidate.utf16Sha256().equals(seal.digest()))
                                throw new IOException("M3AST source changed since parsing: " + uri);
                            session.seals.put(uri, new Seal(candidate));
                            source = candidate;
                        }
                    }
                } catch (IOException failure) {
                    sourceFailure = failure;
                    throw failure;
                }
            }
            return source;
        }

        @Override @SuppressWarnings("unchecked")
        public <T> T project(M3AST.Projection<T> projection) throws Exception {
            check();
            Objects.requireNonNull(projection, "projection");
            if (projections == null) projections = new IdentityHashMap<>();
            Object result = projections.get(projection);
            if (result == COMPUTING) throw new IllegalStateException("recursive M3AST projection: " + projection.name());
            if (result == null) {
                projections.put(projection, COMPUTING);
                session.computations++;
                try {
                    T value = projection.create(this);
                    result = value == null ? NULL : value;
                } catch (Throwable failure) {
                    result = new Failure(failure);
                }
                projections.put(projection, result);
            } else session.hits++;
            if (result instanceof Failure failure) {
                if (failure.cause() instanceof Error error) throw error;
                throw (Exception) failure.cause();
            }
            return result == NULL ? null : (T) result;
        }

        void release() {
            session = null;
            tree = null;
            type = null;
            source = null;
            sourceFailure = null;
            if (projections != null) projections.clear();
            projections = null;
        }
    }

    private static final class Trace implements M3AST.Consumer {
        private final BufferedWriter writer;

        Trace(Path path) throws IOException {
            writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try {
                writer.write("# M3AST-1 actual javac events; no independent parse or attribution\n");
                writer.write("phase\tsource\ttype\n");
            } catch (IOException | RuntimeException | Error failure) {
                try { writer.close(); }
                catch (Throwable cleanup) { if (cleanup != failure) failure.addSuppressed(cleanup); }
                throw failure;
            }
        }

        private void event(M3AST.Unit unit) throws IOException {
            writer.write(unit.phase().name());
            writer.write('\t');
            writer.write(unit.tree().getSourceFile().toUri().toASCIIString());
            writer.write('\t');
            if (unit.analyzedType() != null)
                writer.write(unit.analyzedType().getQualifiedName().toString());
            writer.write('\n');
        }

        @Override public void parsed(M3AST.Unit unit) throws IOException { event(unit); }
        @Override public void analyzed(M3AST.Unit unit) throws IOException { event(unit); }
        @Override public void finished(M3AST.Summary summary) throws IOException {
            try {
                writer.write("# parsed=" + summary.parsedUnits() + " analyzed=" + summary.analyzedTypes()
                        + " captures=" + summary.capturedSources() + " extraSourceReads=" + summary.sourceReads() + " projections=" + summary.projectionComputations()
                        + " hits=" + summary.projectionHits() + " errors=" + summary.compilerErrors()
                        + " completed=" + summary.completed() + " consumerFailed=" + summary.consumerFailed() + "\n");
            } finally { writer.close(); }
        }
    }
}
