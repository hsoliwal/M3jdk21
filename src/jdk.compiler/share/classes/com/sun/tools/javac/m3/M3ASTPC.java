/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * Copyright (c) 2000, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.tools.javac.m3;

/*
 * Modified 2026-10-21 by Synexia (Hitesh Soliwal and contributors): added
 * shared partial build facts and bounded receipt transport in this javac owner.
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.MultiTaskListener;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.main.Arguments;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.ClientCodeException;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Options;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashMap;
import javax.tools.JavaFileObject;
import java.util.Objects;
import java.util.UUID;
import javax.lang.model.element.TypeElement;

/** The existing javac task owns M3 capture; no global parser or AST cache. */
public final class M3ASTPC implements Plugin {
    private static final Context.Key<Session> KEY = new Context.Key<>();
    private static final M3AST.Projection<M3AST.BuildFacts> BUILD_FACTS =
            new M3AST.Projection<>("javac-build-facts-v1", M3ASTPC::captureBuildFacts);

    public M3ASTPC() { }

    @Override public String getName() { return "M3AST"; }

    @Override public void init(JavacTask task, String... args) {
        boolean retainSyntax = false;
        Path trace = null;
        Path image = null;
        Path slots = null;
        Path facts = null;
        UUID factsId = null;
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
            else if (arg.startsWith("facts64=") && facts == null)
                facts = factsPath(arg.substring("facts64=".length()));
            else if (arg.startsWith("factsid=") && factsId == null) {
                String text = arg.substring("factsid=".length());
                factsId = UUID.fromString(text);
                if (!factsId.toString().equals(text))
                    throw new IllegalArgumentException("M3AST factsid requires a canonical UUID");
            }
            else throw new IllegalArgumentException("unknown or duplicate M3AST option: " + arg);
        }
        if (image == null && (slots != null || fold))
            throw new IllegalArgumentException("M3AST slots/fold-expressions require image");
        if (trace != null && (sameOutput(trace, image) || sameOutput(trace, slots)))
            throw new IllegalArgumentException("M3AST trace must differ from image and slots");
        if ((facts == null) != (factsId == null))
            throw new IllegalArgumentException("M3AST facts64 and factsid must be selected together");
        if (facts != null && (sameOutput(facts, trace) || sameOutput(facts, image)
                || sameOutput(facts, slots)))
            throw new IllegalArgumentException("M3AST facts output must differ from trace, image and slots");
        Session session = (Session) open(task, retainSyntax);
        try {
            // Reuse javac's actual processor/plugin loader, including module paths.
            var loader = JavacProcessingEnvironment.instance(session.context)
                    .getServiceLoader(M3AST.Consumer.class);
            for (M3AST.Consumer consumer : loader) session.addConsumer(consumer);
            if (image != null)
                session.addConsumer(M3PrimitiveImageCompiler.consumer(image, slots, fold));
            if (trace != null) session.addConsumer(new Trace(trace));
            if (facts != null) {
                if (session.factsReceipt != null)
                    throw new IllegalArgumentException("M3AST facts output already selected for this task");
                FactsReceipt receipt = new FactsReceipt(facts, factsId);
                session.addConsumer(receipt);
                session.factsReceipt = receipt;
            }
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

    private static Path factsPath(String encoded) {
        if (encoded.isEmpty() || encoded.length() > 65_536)
            throw new IllegalArgumentException("invalid M3AST facts64 path length");
        byte[] bytes = Base64.getUrlDecoder().decode(encoded);
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(bytes).equals(encoded))
            throw new IllegalArgumentException("M3AST facts64 requires unpadded canonical base64url");
        try {
            String decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            Path path = Path.of(decoded).toAbsolutePath().normalize();
            if (decoded.isEmpty() || path.getFileName() == null)
                throw new IllegalArgumentException("M3AST facts64 requires an output file path");
            return path;
        } catch (CharacterCodingException failure) {
            throw new IllegalArgumentException("M3AST facts64 path is not UTF-8", failure);
        }
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

    /** Public API delegation; one existing event projection owns the detached result. */
    public static M3AST.BuildFacts buildFacts(M3AST.Unit unit) throws Exception {
        if (!(unit instanceof LiveUnit live))
            throw new IllegalArgumentException("foreign M3AST unit implementation");
        live.check();
        if (live.phase != M3AST.Phase.ANALYZED)
            throw new IllegalStateException("M3AST build facts require an ANALYZED event");
        return unit.project(BUILD_FACTS);
    }

    private static M3AST.BuildFacts captureBuildFacts(M3AST.Unit unit) throws IOException {
        LiveUnit live = (LiveUnit) unit;
        live.check();
        if (!(live.type instanceof ClassSymbol analyzed)
                || !(live.tree instanceof JCTree.JCCompilationUnit compilation))
            throw new IOException("M3AST build facts require an original analyzed class");
        JCTree.JCClassDecl declaration = analyzedDeclaration(live);
        if (declaration == null)
            throw new IOException("M3AST build facts require an analyzed top-level declaration");
        URI uri = compilation.sourcefile.toUri();
        Seal seal = live.session.seals.get(uri);
        if (seal == null)
            throw new IOException("M3AST parsed-source seal unavailable: " + uri);
        BuildCollector collector = new BuildCollector();
        collector.scan(declaration);
        String[] declared = collector.declared.sorted();
        String[] referenced = collector.referenced.sorted();
        int used = 0;
        for (String name : referenced) {
            if (Arrays.binarySearch(declared, name) < 0) referenced[used++] = name;
        }
        return new M3AST.BuildFacts(uri, seal.length(), seal.digest(), analyzed.flatName().toString(),
                declared, Arrays.copyOf(referenced, used), collector.unresolved);
    }

    private static JCTree.JCClassDecl analyzedDeclaration(LiveUnit live) {
        if (!(live.type instanceof ClassSymbol analyzed)
                || !(live.tree instanceof JCTree.JCCompilationUnit compilation)) return null;
        for (JCTree tree : compilation.defs) {
            if (tree instanceof JCTree.JCClassDecl declaration && declaration.sym == analyzed)
                return declaration;
        }
        return null;
    }

    /** Read already-attributed symbols directly: no lazy Trees attribution or second frontend. */
    private static final class BuildCollector extends TreeScanner {
        private final BuildNames declared = new BuildNames();
        private final BuildNames referenced = new BuildNames();
        private boolean unresolved;

        @Override public void scan(JCTree tree) {
            if (tree == null) return;
            Type type = tree.type;
            while (type != null && type.hasTag(TypeTag.ARRAY)) type = ((Type.ArrayType) type).elemtype;
            if (type != null) {
                if (type.hasTag(TypeTag.ERROR)) unresolved = true;
                else if (type.hasTag(TypeTag.CLASS) && type.tsym instanceof ClassSymbol owner)
                    referenceClass(owner);
            }
            super.scan(tree);
        }

        private void reference(Symbol symbol) {
            if (symbol == null) { unresolved = true; return; }
            for (Symbol owner = symbol; owner != null; owner = owner.owner) {
                if (owner instanceof ClassSymbol type) {
                    referenceClass(type);
                    return;
                }
            }
        }

        private void referenceClass(ClassSymbol type) {
            // Primitive types also have ClassSymbols; Array/Method/Bound are
            // javac-only classes owned by noSymbol, not binary class identities.
            if (type.type instanceof Type.ClassType && type.type.hasTag(TypeTag.CLASS)
                    && type.owner != null && type.owner.kind != Kinds.Kind.NIL)
                referenced.add(type.flatName().toString());
        }

        @Override public void visitClassDef(JCTree.JCClassDecl tree) {
            if (tree.sym == null) unresolved = true;
            else declared.add(tree.sym.flatName().toString());
            super.visitClassDef(tree);
        }

        @Override public void visitIdent(JCTree.JCIdent tree) {
            reference(tree.sym);
            super.visitIdent(tree);
        }

        @Override public void visitSelect(JCTree.JCFieldAccess tree) {
            reference(tree.sym);
            super.visitSelect(tree);
        }

        @Override public void visitReference(JCTree.JCMemberReference tree) {
            reference(tree.sym);
            super.visitReference(tree);
        }

        @Override public void visitNewClass(JCTree.JCNewClass tree) {
            reference(tree.constructor);
            super.visitNewClass(tree);
        }
    }

    /** Event-local string lanes; no retained per-reference graph or compiler objects. */
    private static final class BuildNames {
        private static final int MAX_NAMES = 131_072;
        private String[] entries = new String[16];
        private int count;

        void add(String name) {
            if (name.isEmpty()) return;
            int slot = slot(name, entries);
            if (entries[slot] != null) return;
            if (count == MAX_NAMES)
                throw new IllegalStateException("M3AST build fact name budget exceeded");
            if ((count + 1) * 2 >= entries.length) {
                String[] enlarged = new String[entries.length << 1];
                for (String value : entries) {
                    if (value != null) enlarged[slot(value, enlarged)] = value;
                }
                entries = enlarged;
                slot = slot(name, entries);
            }
            entries[slot] = name;
            count++;
        }

        private static int slot(String name, String[] entries) {
            int hash = name.hashCode();
            int mask = entries.length - 1;
            int slot = (hash ^ (hash >>> 16)) & mask;
            while (entries[slot] != null && !name.equals(entries[slot])) slot = (slot + 1) & mask;
            return slot;
        }

        String[] sorted() {
            String[] names = new String[count];
            int index = 0;
            for (String value : entries) if (value != null) names[index++] = value;
            Arrays.sort(names);
            return names;
        }
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
            Seal seal = new Seal(source.text().length(), source.utf16Sha256());
            Seal previous = session.seals.putIfAbsent(source.uri(), seal);
            if (previous != null && !previous.equals(seal))
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

    private record Seal(int length, String digest) { }

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
        private FactsReceipt factsReceipt;
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
                // All M3 consumers must finish before a current invocation can publish facts.
                if (factsReceipt != null) {
                    try { factsReceipt.publish(summary()); }
                    catch (Throwable failure) {
                        failed = true;
                        if (first == null) first = failure;
                        else if (failure != first) first.addSuppressed(failure);
                    }
                }
            } finally {
                if (factsReceipt != null) factsReceipt.clear();
                factsReceipt = null;
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
        private static final byte COMPUTING = 1;
        private static final byte COMPLETED = 2;
        private static final byte FAILED = 4;
        private Session session;
        private CompilationUnitTree tree;
        private TypeElement type;
        private final M3AST.Phase phase;
        private final Thread thread = Thread.currentThread();
        // IdentityHashMap's paired-slot probing, specialized to this event's state.
        // No null key, removal, entry object, failure wrapper, or global owner.
        private Object[] projections;
        private byte[] projectionStates;
        private int projectionCount;
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
                        M3AST.Source candidate = retained(uri, text);
                        if (!candidate.utf16Sha256().equals(seal.digest()))
                            throw new IOException("M3AST source changed since parsing: " + uri);
                        source = candidate;
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
            if (projections == null) {
                Object[] entries = new Object[16];
                byte[] states = new byte[8];
                projections = entries;
                projectionStates = states;
            }
            int slot = projectionSlot(projection, projections);
            if ((projectionStates[slot >>> 1] & COMPUTING) != 0)
                throw new IllegalStateException("recursive M3AST projection: " + projection.name());
            if (projections[slot] == null) {
                if (3L * (projectionCount + 1) > projections.length) {
                    growProjections();
                    slot = projectionSlot(projection, projections);
                }
                projections[slot] = projection;
                projectionStates[slot >>> 1] = COMPUTING;
                projectionCount++;
                session.computations++;
                Object result;
                byte state = COMPLETED;
                try {
                    result = projection.create(this);
                } catch (Throwable failure) {
                    result = failure;
                    state |= FAILED;
                }
                // Nested factories can grow and rehash this same event's arrays.
                slot = projectionSlot(projection, projections);
                projections[slot + 1] = result;
                projectionStates[slot >>> 1] = state;
            } else session.hits++;
            Object result = projections[slot + 1];
            if ((projectionStates[slot >>> 1] & FAILED) != 0) {
                if (result instanceof Error error) throw error;
                throw (Exception) result;
            }
            return (T) result;
        }

        /** Adapted from java.util.IdentityHashMap's identity hash and paired-slot probe. */
        private static int projectionSlot(Object key, Object[] entries) {
            int hash = System.identityHashCode(key);
            int mask = entries.length - 1;
            int slot = ((hash << 1) - (hash << 8)) & mask;
            while (entries[slot] != null && entries[slot] != key) slot = (slot + 2) & mask;
            return slot;
        }

        private void growProjections() {
            int length = projections.length;
            // As in IdentityHashMap, retain a vacant key slot at maximum capacity.
            if (length == 1 << 30) {
                if (projectionCount == (length >>> 1) - 1)
                    throw new IllegalStateException("Capacity exhausted.");
                return;
            }
            Object[] entries = new Object[length << 1];
            byte[] states = new byte[length];
            for (int old = 0; old < length; old += 2) {
                Object key = projections[old];
                if (key != null) {
                    int slot = projectionSlot(key, entries);
                    entries[slot] = key;
                    entries[slot + 1] = projections[old + 1];
                    states[slot >>> 1] = projectionStates[old >>> 1];
                }
            }
            projections = entries;
            projectionStates = states;
        }

        void release() {
            session = null;
            tree = null;
            type = null;
            source = null;
            sourceFailure = null;
            projections = null;
            projectionStates = null;
            projectionCount = 0;
        }
    }

    /** Bounded, invocation-owned transport of immutable event facts, never a dependency store. */
    private static final class FactsReceipt implements M3AST.Consumer {
        private static final int MAX_TYPES = 65_536;
        private static final long MAX_TEXT = 8_388_608;
        private final Path output;
        private final UUID invocation;
        private M3AST.BuildFacts[] values = new M3AST.BuildFacts[16];
        private int count;
        private long retainedText;
        private long skipped;

        FactsReceipt(Path output, UUID invocation) {
            this.output = output;
            this.invocation = invocation;
        }

        @Override public void analyzed(M3AST.Unit unit) throws Exception {
            // Module/package-only events have no original top-level class declaration.
            if (analyzedDeclaration((LiveUnit) unit) == null) { skipped++; return; }
            if (count == MAX_TYPES) throw new IOException("M3AST facts type budget exceeded");
            M3AST.BuildFacts facts = M3AST.buildFacts(unit);
            long text = facts.sourceUri().toASCIIString().length() + 64L
                    + facts.analyzedBinaryName().length();
            for (String name : facts.declaredBinaryNames()) text += name.length() + 1L;
            for (String name : facts.referencedBinaryNames()) text += name.length() + 1L;
            if (text > MAX_TEXT - retainedText)
                throw new IOException("M3AST facts retained-text budget exceeded");
            if (count == values.length) values = Arrays.copyOf(values, values.length << 1);
            values[count++] = facts;
            retainedText += text;
        }

        void publish(M3AST.Summary summary) throws IOException {
            if (!summary.completed() || !summary.closed() || summary.compilerErrors() != 0
                    || summary.consumerFailed() || count == 0) return;
            Arrays.sort(values, 0, count, Comparator
                    .comparing((M3AST.BuildFacts value) -> value.sourceUri().toASCIIString())
                    .thenComparing(M3AST.BuildFacts::analyzedBinaryName));
            for (int index = 1; index < count; index++) {
                M3AST.BuildFacts left = values[index - 1], right = values[index];
                if (left.sourceUri().equals(right.sourceUri())
                        && left.analyzedBinaryName().equals(right.analyzedBinaryName()))
                    throw new IOException("duplicate M3AST analyzed fact slice");
            }
            Path parent = output.getParent();
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, ".m3ast-facts-", ".tmp");
            try {
                try (ReceiptWriter writer = new ReceiptWriter(temporary)) {
                    writer.row(M3AST.BuildFacts.SCHEMA, "PARTIAL", invocation.toString());
                    writer.row("OUTPUT", encoded(output.toString()));
                    long declarations = 0, references = 0;
                    for (int index = 0; index < count; index++) {
                        M3AST.BuildFacts facts = values[index];
                        String slot = Integer.toString(index);
                        writer.row("TYPE", slot, encoded(facts.sourceUri().toASCIIString()),
                                Integer.toString(facts.sourceLength()), facts.sourceUtf16Sha256(),
                                encoded(facts.analyzedBinaryName()),
                                Boolean.toString(facts.hasUnresolvedSymbols()));
                        for (String name : facts.declaredBinaryNames()) {
                            writer.row("DECLARE", slot, encoded(name));
                            declarations++;
                        }
                        for (String name : facts.referencedBinaryNames()) {
                            writer.row("REFERENCE", slot, encoded(name));
                            references++;
                        }
                    }
                    writer.row("SUMMARY", Long.toString(summary.parsedUnits()),
                            Long.toString(summary.analyzedTypes()), Long.toString(summary.capturedSources()),
                            Long.toString(summary.sourceReads()), Long.toString(summary.projectionComputations()),
                            Long.toString(summary.projectionHits()), Integer.toString(summary.compilerErrors()),
                            Boolean.toString(summary.completed()), Boolean.toString(summary.closed()),
                            Boolean.toString(summary.consumerFailed()), Long.toString(skipped));
                    writer.row("END", Integer.toString(count), Long.toString(declarations),
                            Long.toString(references));
                }
                Files.move(temporary, output, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }

        void clear() {
            values = null;
            count = 0;
            retainedText = 0;
        }

        private static String encoded(String value) {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** All columns are ASCII: decimal, boolean, schema names or base64url UTF-8. */
    private static final class ReceiptWriter implements AutoCloseable {
        private static final long MAX_BYTES = 67_108_864;
        private final BufferedWriter writer;
        private long bytes;

        ReceiptWriter(Path file) throws IOException {
            writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        void row(String... values) throws IOException {
            long length = values.length;
            for (String value : values) length += value.length();
            if (length > MAX_BYTES - bytes)
                throw new IOException("M3AST facts output byte budget exceeded");
            for (int index = 0; index < values.length; index++) {
                if (index != 0) writer.write('\t');
                writer.write(values[index]);
            }
            writer.write('\n');
            bytes += length;
        }

        @Override public void close() throws IOException { writer.close(); }
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
