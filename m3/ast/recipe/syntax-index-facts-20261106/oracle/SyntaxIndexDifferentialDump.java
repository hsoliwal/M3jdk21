// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.SplittableRandom;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.ast.M3SyntaxIndex;
import jdk.m3.ast.M3SyntaxTree;

/**
 * Differential dump of jdk.m3.ast.M3SyntaxIndex over a real Java corpus (recipe syntax-index-facts-20261106): the
 * same driver is compiled against the master classes and against the edited classes, and the two dumps are compared.
 * Every file is parsed by jdk.compiler and projected into an M3SyntaxTree (UTF-8 byte spans; a node whose span is not
 * nested in its parent or overlaps its previous sibling, such as a type shared by two declarators, is skipped with its
 * subtree, the same on both sides). Records: FILE (nodes, root digest), KIND (every distinct kind and absent probes:
 * count and digest of nodesOfKind), FACTS and F (a seeded FactBuilder workload whose insertion order differs from node
 * order, with duplicate keys: every frozen row per node and first()), REFUSE (frozen builder and ordinal refusals),
 * CONCURRENT (eight cold readers on a fresh index) and TOTAL. Independent of the implementation, every nodesOfKind
 * answer is checked against a brute-force scan of tree.kind(i), repeated, and checked for caller-owned arrays.
 */
public final class SyntaxIndexDifferentialDump {
    private static final String[] PROVIDERS = {"javac", "m3", "clang"};
    private static final String[] PROPERTIES = {"type", "symbol", "diagnostic", "origin", "range"};
    private static final String[] PROBES = {"", "missing", "Aa", "BB", "identifier", "IDENTIFIER ", "class"};
    private static final AtomicLong BRUTE_MISMATCHES = new AtomicLong();
    private static long kindQueries, facts, factNodes, nodes;

    private SyntaxIndexDifferentialDump() { }

    private static final class Node {
        final String kind;
        final int start, end;
        final List<Node> children = new ArrayList<>();
        Node(String kind, int start, int end) { this.kind = kind; this.start = start; this.end = end; }
    }

    private static final M3SyntaxTree.ForeignNode<Node> ADAPTER = new M3SyntaxTree.ForeignNode<>() {
        @Override public String kind(Node n) { return n.kind; }
        @Override public String field(Node n) { return ""; }
        @Override public int startByte(Node n) { return n.start; }
        @Override public int endByte(Node n) { return n.end; }
        @Override public int flags(Node n) { return M3SyntaxTree.NAMED; }
        @Override public Iterable<Node> children(Node n) { return n.children; }
    };

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[1]);
        List<String> paths = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder(1 << 20);
        M3SyntaxTree largest = null;
        for (String path : paths) {
            if (path.isBlank()) continue;
            M3SyntaxTree tree = project(root.resolve(path));
            dumpFile(out, path, tree);
            if (largest == null || tree.size() > largest.size()) largest = tree;
        }
        if (largest != null) concurrent(out, largest);
        out.append("TOTAL files=").append(paths.stream().filter(p -> !p.isBlank()).count())
                .append(" nodes=").append(nodes).append(" kind_queries=").append(kindQueries)
                .append(" brute_mismatches=").append(BRUTE_MISMATCHES.get())
                .append(" facts=").append(facts).append(" fact_nodes=").append(factNodes).append('\n');
        System.out.print(out);
    }

    private static M3SyntaxTree project(Path file) throws IOException {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
        int[] byteAt = new int[source.length() + 1];
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            int width = c < 0x80 ? 1 : c < 0x800 ? 2 : Character.isSurrogate(c) ? 2 : 3;
            byteAt[i + 1] = byteAt[i] + width;
        }
        if (byteAt[source.length()] != bytes.length) throw new IllegalStateException("UTF-8 offsets of " + file);
        JavaFileObject object = new SimpleJavaFileObject(URI.create("string:///" + file.getFileName()),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return source; }
        };
        JavacTask javac = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(
                null, null, diagnostic -> { }, List.of("-proc:none"), null, List.of(object));
        CompilationUnitTree unit = javac.parse().iterator().next();
        SourcePositions positions = Trees.instance(javac).getSourcePositions();
        ArrayDeque<Node> parents = new ArrayDeque<>();
        Node[] top = {null};
        new TreeScanner<Void, Void>() {
            @Override public Void scan(Tree original, Void unused) {
                if (original == null) return null;
                long start = positions.getStartPosition(unit, original);
                long end = positions.getEndPosition(unit, original);
                if (start < 0 || end < start || end > source.length()) return super.scan(original, unused);
                Node mapped = new Node(original.getKind().name(), byteAt[(int) start], byteAt[(int) end]);
                Node parent = parents.peek();
                if (parent != null && !fits(parent, mapped)) return null;
                if (parent == null) top[0] = mapped;
                else parent.children.add(mapped);
                parents.push(mapped);
                super.scan(original, unused);
                parents.pop();
                return null;
            }
        }.scan(unit, null);
        if (top[0] == null) top[0] = new Node("EMPTY", 0, bytes.length);
        Node whole = new Node("SOURCE_FILE", 0, bytes.length); // the root spans the file from byte zero
        if (fits(whole, top[0])) whole.children.add(top[0]);
        return M3SyntaxTree.project("jdk.compiler/21", bytes, whole, ADAPTER);
    }

    private static boolean fits(Node parent, Node child) {
        int previousEnd = parent.children.isEmpty() ? parent.start : parent.children.get(parent.children.size() - 1).end;
        return child.start >= previousEnd && child.end <= parent.end && child.start <= child.end;
    }

    private static void dumpFile(StringBuilder out, String path, M3SyntaxTree tree) {
        M3SyntaxIndex index = new M3SyntaxIndex(tree);
        nodes += tree.size();
        out.append("FILE ").append(path).append(" nodes=").append(tree.size())
                .append(" root=").append(index.rootSha256()).append('\n');
        TreeSet<String> queries = new TreeSet<>(Arrays.asList(PROBES));
        for (int i = 0; i < tree.size(); i++) queries.add(tree.kind(i));
        for (String kind : queries) {
            int[] expected = brute(tree, kind);
            int[] first = index.nodesOfKind(kind);
            int[] second = index.nodesOfKind(kind);
            if (first.length > 0) first[0] = -9; // a caller-owned array must not reach the index
            int[] third = index.nodesOfKind(kind);
            if (!Arrays.equals(expected, second) || !Arrays.equals(expected, third)) {
                BRUTE_MISMATCHES.incrementAndGet();
                out.append("BRUTE_MISMATCH ").append(path).append(" kind=").append(kind).append('\n');
            }
            kindQueries++;
            out.append("KIND ").append(path).append(" kind=").append(kind).append(" count=").append(second.length)
                    .append(" nodes=").append(digest(second)).append('\n');
        }
        dumpFacts(out, path, tree);
    }

    private static int[] brute(M3SyntaxTree tree, String kind) {
        int[] all = new int[tree.size()];
        int count = 0;
        for (int i = 0; i < tree.size(); i++) if (kind.equals(tree.kind(i))) all[count++] = i;
        return Arrays.copyOf(all, count);
    }

    private static void dumpFacts(StringBuilder out, String path, M3SyntaxTree tree) {
        SplittableRandom random = new SplittableRandom(path.hashCode() * 0x9E3779B97F4A7C15L);
        int n = tree.size();
        int[] order = new int[n];
        for (int i = 0; i < n; i++) order[i] = i;
        for (int i = n - 1; i > 0; i--) {
            int j = random.nextInt(i + 1), t = order[i];
            order[i] = order[j]; order[j] = t;
        }
        M3SyntaxIndex.FactBuilder builder = M3SyntaxIndex.facts(tree);
        for (int node : order) {
            int k = random.nextInt(4);
            for (int j = 0; j < k; j++) {
                builder.add(node, PROVIDERS[random.nextInt(PROVIDERS.length)],
                        PROPERTIES[random.nextInt(PROPERTIES.length)],
                        tree.kind(node) + "@" + tree.startByte(node) + "#" + random.nextInt(7));
            }
        }
        for (int i = 0; i < n / 3; i++) builder.add(random.nextInt(n), "javac", "type", "dup" + random.nextInt(3));
        M3SyntaxIndex.Facts frozen = builder.freeze();
        frozen.requireSource(tree);
        facts += frozen.size();
        out.append("FACTS ").append(path).append(" size=").append(frozen.size()).append('\n');
        for (int node = 0; node < n; node++) {
            int count = frozen.count(node);
            if (count == 0) continue;
            factNodes++;
            out.append("F ").append(path).append(' ').append(node).append(" count=").append(count).append(' ');
            for (int i = 0; i < count; i++) {
                out.append(frozen.key(node, i)).append('=').append(frozen.value(node, i)).append(i + 1 < count ? "|" : "");
            }
            out.append(" first=").append(frozen.first(node, "javac:type")).append('\n');
        }
        out.append("REFUSE ").append(path)
                .append(" add=").append(refusal(() -> builder.add(0, "javac", "type", "late")))
                .append(" freeze=").append(refusal(builder::freeze))
                .append(" ordinal=").append(refusal(() -> frozen.key(0, frozen.count(0))))
                .append(" node=").append(refusal(() -> frozen.count(n))).append('\n');
    }

    private static String refusal(Runnable call) {
        try { call.run(); return "none"; }
        catch (RuntimeException failure) { return failure.getClass().getSimpleName(); }
    }

    private static void concurrent(StringBuilder out, M3SyntaxTree tree) throws InterruptedException {
        M3SyntaxIndex cold = new M3SyntaxIndex(tree);
        String[] kinds = new TreeSet<>(Arrays.asList(PROBES)).toArray(String[]::new);
        TreeSet<String> all = new TreeSet<>(Arrays.asList(kinds));
        for (int i = 0; i < tree.size(); i++) all.add(tree.kind(i));
        String[] queries = all.toArray(String[]::new);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicLong mismatches = new AtomicLong();
        Thread[] threads = new Thread[8];
        for (int t = 0; t < threads.length; t++) {
            int id = t;
            threads[t] = Thread.ofPlatform().start(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 400; i++) {
                        String query = queries[(id * 97 + i) % queries.length];
                        int[] now = cold.nodesOfKind(query);
                        if (!Arrays.equals(brute(tree, query), now)) mismatches.incrementAndGet();
                        if (now.length > 0) now[0] = -77;
                    }
                } catch (Throwable failure) { error.compareAndSet(null, failure); }
            });
        }
        start.countDown();
        for (Thread thread : threads) thread.join();
        BRUTE_MISMATCHES.addAndGet(mismatches.get());
        out.append("CONCURRENT nodes=").append(tree.size()).append(" threads=8 queries=3200 mismatches=")
                .append(mismatches.get()).append(" error=").append(error.get() == null ? "none" : error.get())
                .append('\n');
    }

    private static String digest(int[] values) {
        MessageDigest hash;
        try { hash = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
        for (int v : values) {
            hash.update((byte) (v >>> 24)); hash.update((byte) (v >>> 16));
            hash.update((byte) (v >>> 8)); hash.update((byte) v);
        }
        return HexFormat.of().formatHex(hash.digest(), 0, 8);
    }
}
