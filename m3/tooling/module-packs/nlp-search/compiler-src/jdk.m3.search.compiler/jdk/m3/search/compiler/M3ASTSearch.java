// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
package jdk.m3.search.compiler;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.M3AST;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.net.URI;
import java.util.concurrent.CancellationException;
import jdk.m3.search.M3File;
import jdk.m3.search.M3FileSourceFacts;
import jdk.m3.search.M3Search;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;

/**
 * Projects the real M3AST PARSED event into source-bound declaration coordinates.
 * No parser, compiler tree cache, model or index writer is created here. The caller
 * registers the consumer before compilation and owns the writer's commit/close policy.
 *
 * <p>The supplied context digest describes the caller's chosen source/index namespace.
 * This consumer indexes syntax, not attributed semantic facts. It does not claim that
 * a source digest or a caller label proves a complete classpath/processor environment.
 * A separate instance owns a separate phase-local projection identity.</p>
 */
public final class M3ASTSearch {
    public static final String DECLARATION_KIND = "_m3_declaration_kind";
    public static final String NAME = "_m3_name";
    public static final String START = "_m3_start_utf16";
    public static final String END = "_m3_end_utf16";
    public static final String JAVAC_SHA256 = "_m3_javac_utf16_sha256";

    private final String contextSha256;
    private final int maxNodes;
    private final Runnable checkpoint;
    private final Map<URI, M3File> files;
    private final M3AST.Projection<Snapshot> projection;

    public M3ASTSearch(String contextSha256, int maxNodes) {
        this(contextSha256, maxNodes, null);
    }

    public M3ASTSearch(String contextSha256, int maxNodes, Runnable checkpoint) {
        this(contextSha256, maxNodes, List.of(), checkpoint);
    }

    /** Preserve original file-byte evidence only for the identical frozen compiler input. */
    public M3ASTSearch(String contextSha256, int maxNodes,
                       Collection<M3File> frozenInputs, Runnable checkpoint) {
        // Reuse the canonical metadata validation rather than a second hash/schema validator.
        new M3FileSourceFacts("m3:///context", "0".repeat(64), 0, contextSha256,
                "jdk.m3.search.compiler", 1, "PARSED");
        if (maxNodes < 1) throw new IllegalArgumentException("maxNodes must be positive");
        this.contextSha256 = contextSha256;
        this.maxNodes = maxNodes;
        this.checkpoint = checkpoint;
        Map<URI, M3File> inputs = new HashMap<>();
        for (M3File input : List.copyOf(Objects.requireNonNull(frozenInputs, "frozenInputs"))) {
            if (inputs.putIfAbsent(input.toUri(), input) != null)
                throw new IllegalArgumentException("duplicate source URI: " + input.toUri());
        }
        files = Map.copyOf(inputs);
        projection = new M3AST.Projection<>("jdk.m3.search.compiler.declarations.v1", this::capture);
    }

    /** The existing compiler session memoizes this exact operation once per live event. */
    public Snapshot project(M3AST.Unit unit) throws Exception {
        Objects.requireNonNull(unit, "unit");
        if (unit.phase() != M3AST.Phase.PARSED)
            throw new IllegalArgumentException("declaration capture requires the PARSED event");
        return unit.project(projection);
    }

    /**
     * Replace one source's complete document group after bounded capture succeeds.
     * IndexWriter supplies its existing atomic updateDocuments semantics. Parse errors
     * remain javac diagnostics; indexed declarations are explicitly syntax observations.
     */
    public M3AST.Consumer consumer(IndexWriter writer) {
        Objects.requireNonNull(writer, "writer");
        return new M3AST.Consumer() {
            @Override public void parsed(M3AST.Unit unit) throws Exception {
                Snapshot snapshot = project(unit);
                check();
                writer.updateDocuments(M3Search.sourceTerm(snapshot.source(), contextSha256),
                        snapshot.documents());
            }
        };
    }

    private Snapshot capture(M3AST.Unit unit) throws Exception {
        check();
        M3AST.Source retained = unit.source();
        M3File source = files.get(retained.uri());
        if (source == null) source = M3File.of(retained.uri(), retained.text());
        else if (source.source() != retained.text())
            throw new IllegalArgumentException("frozen input differs from the actual parser source");
        M3FileSourceFacts facts = M3FileSourceFacts.capture(retained.uri().toASCIIString(),
                retained.text(), contextSha256, "jdk.m3.search.compiler", 1, "PARSED");
        var positions = unit.trees().getSourcePositions();
        var compilationUnit = unit.tree();
        var queue = new ArrayDeque<Tree>();
        queue.add(compilationUnit);
        int[] discovered = {1};
        // Each accept visits immediate children only. The explicit queue bounds traversal
        // and avoids a recursive walk through hostile, deeply nested source expressions.
        TreeScanner<Void, Void> children = new TreeScanner<>() {
            @Override public Void scan(Tree tree, Void unused) {
                if (tree != null) {
                    if (discovered[0] == maxNodes)
                        throw new IllegalArgumentException("AST node budget exceeded");
                    discovered[0]++;
                    queue.addLast(tree);
                }
                return null;
            }
        };
        Declarations declarations = new Declarations(Math.min(32, maxNodes));
        int visited = 0;
        while (!queue.isEmpty()) {
            check();
            Tree tree = queue.removeFirst();
            visited++;
            String name = declarationName(tree);
            if (name != null) {
                long start = positions.getStartPosition(compilationUnit, tree);
                long end = positions.getEndPosition(compilationUnit, tree);
                // Synthetic nodes without source positions cannot assert a source range.
                if (start >= 0 && end >= start && end <= retained.text().length())
                    declarations.add((int) start, (int) end, tree.getKind().name(), name);
            }
            tree.accept(children, null);
        }
        check();
        return declarations.finish(source, facts, retained.utf16Sha256(), visited);
    }

    private void check() {
        if (Thread.currentThread().isInterrupted())
            throw new CancellationException("M3 AST search interrupted");
        if (checkpoint != null) checkpoint.run();
    }

    private static String declarationName(Tree tree) {
        if (tree instanceof ClassTree declaration) return declaration.getSimpleName().toString();
        if (tree instanceof MethodTree declaration) return declaration.getName().toString();
        if (tree instanceof VariableTree declaration) return declaration.getName().toString();
        return null;
    }

    /** Primitive range lanes plus immutable names; never a retained compiler Tree/Element. */
    private static final class Declarations {
        private int[] starts, ends;
        private String[] kinds, names;
        private int size;

        Declarations(int capacity) {
            starts = new int[capacity]; ends = new int[capacity];
            kinds = new String[capacity]; names = new String[capacity];
        }

        void add(int start, int end, String kind, String name) {
            if (size == starts.length) {
                int capacity = Math.addExact(size, Math.max(1, size >>> 1));
                starts = Arrays.copyOf(starts, capacity); ends = Arrays.copyOf(ends, capacity);
                kinds = Arrays.copyOf(kinds, capacity); names = Arrays.copyOf(names, capacity);
            }
            starts[size] = start; ends[size] = end; kinds[size] = kind; names[size++] = name;
        }

        Snapshot finish(M3File source, M3FileSourceFacts facts, String javacDigest, int visited) {
            return new Snapshot(source, facts, javacDigest, visited, Arrays.copyOf(starts, size),
                    Arrays.copyOf(ends, size), Arrays.copyOf(kinds, size), Arrays.copyOf(names, size));
        }
    }

    /** Immutable syntax metadata survives the compiler callback; source text has one owner. */
    public static final class Snapshot {
        private final M3File source;
        private final M3FileSourceFacts facts;
        private final String javacDigest;
        private final int visitedNodes;
        private final int[] starts, ends;
        private final String[] kinds, names;

        private Snapshot(M3File source, M3FileSourceFacts facts, String javacDigest, int visitedNodes,
                         int[] starts, int[] ends, String[] kinds, String[] names) {
            this.source = source; this.facts = facts; this.javacDigest = javacDigest;
            this.visitedNodes = visitedNodes; this.starts = starts; this.ends = ends;
            this.kinds = kinds; this.names = names;
        }

        public M3File source() { return source; }
        public M3FileSourceFacts facts() { return facts; }
        /** Javac's raw UTF-16BE digest; distinct from the domain-separated file-source digest. */
        public String javacUtf16Sha256() { return javacDigest; }
        public int visitedNodes() { return visitedNodes; }
        public int size() { return starts.length; }
        public int start(int index) { return starts[Objects.checkIndex(index, size())]; }
        public int end(int index) { return ends[Objects.checkIndex(index, size())]; }
        public String kind(int index) { return kinds[Objects.checkIndex(index, size())]; }
        public String name(int index) { return names[Objects.checkIndex(index, size())]; }

        /** Fresh caller-owned documents; full source is analyzed once, declaration rows only metadata. */
        public Iterable<Document> documents() {
            return () -> new Iterator<>() {
                private int position = -1;
                @Override public boolean hasNext() { return position < size(); }
                @Override public Document next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    Document document;
                    if (position == -1) document = M3Search.document(source, facts.contextSha256());
                    else {
                        document = M3Search.metadata(facts, source.byteSha256(), "declaration");
                        document.add(new StringField(DECLARATION_KIND, kinds[position], Field.Store.YES));
                        document.add(new StringField(NAME, names[position], Field.Store.YES));
                        document.add(new IntPoint(START, starts[position]));
                        document.add(new StoredField(START, starts[position]));
                        document.add(new IntPoint(END, ends[position]));
                        document.add(new StoredField(END, ends[position]));
                    }
                    document.add(new StringField(JAVAC_SHA256, javacDigest, Field.Store.YES));
                    position++;
                    return document;
                }
            };
        }
    }
}
