// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
import com.m3.tools.cpu.M3FileCpuFacts;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.io.M3File;

/** Real file snapshot + shared, original javac callback; no second parser. */
public final class M3FileCpuFactsTest {
    private static int checks;
    private M3FileCpuFactsTest() { }

    private static void yes(boolean accepted, String message) {
        checks++;
        if (!accepted) throw new AssertionError(message);
    }

    private static void expect(Class<? extends Throwable> type, Runnable runnable) {
        checks++;
        try { runnable.run(); }
        catch (Throwable problem) {
            if (type.isInstance(problem)) return;
            throw new AssertionError("wrong exception type", problem);
        }
        throw new AssertionError("expected " + type.getSimpleName());
    }

    private static void textFacts(Path work) throws Exception {
        Path filePath = work.resolve("content.txt");
        String text = "aaaa\r\nab\ud83d\ude42ab\n";
        Files.writeString(filePath, text, StandardCharsets.UTF_8);
        M3File original = M3File.load(filePath);
        M3FileCpuFacts facts = M3FileCpuFacts.of(original);
        yes(facts.source() == original && facts.syntax().isEmpty(), "source is only owner");
        yes(facts.retainedPrimitiveBytes() == 0, "no mandatory secondary arrays");
        yes(facts.sha256().equals(M3FileCpuFacts.of(original).sha256()), "deterministic source receipt");

        M3FileCpuFacts.Search first = facts.search("aa", 2);
        yes(Arrays.equals(new int[] {0, 1}, first.positions()) && !first.complete(),
                "overlap truncation is explicit");
        yes(facts.search("aa", 2) == first, "same query shares original CpuTools evidence");
        yes(Arrays.equals(new int[] {0, 1, 2}, facts.search("aa", 3).positions())
                && facts.search("aa", 3).complete(), "full overlapping matches");

        yes(Arrays.equals(new int[] {0, 6, 13}, facts.lineStarts()), "CRLF LF one offset lane");
        yes(facts.lineCount() == 3 && facts.lineOf(5) == 0 && facts.lineOf(6) == 1
                && facts.lineOf(13) == 2, "UTF-16 line boundaries and EOF");
        int[] exposed = facts.lineStarts();
        exposed[0] = 1000;
        yes(facts.lineStarts()[0] == 0, "defensive line offsets");
        yes(Arrays.equals(new int[] {6, 10}, facts.search("ab", 5).positions()),
                "original UTF-16 source search");
        yes(Arrays.equals(new long[] {2L}, facts.search("ab", 5).lineMask()),
                "primitive line mask from existing hit positions");
        yes(Arrays.equals(new long[] {3L}, facts.search("\r\nab", 4).lineMask()),
                "cross-CRLF hit sets both affected source lines");
        Path lineOverlap = work.resolve("line-overlap.txt");
        Files.writeString(lineOverlap, "aa\naa\naa\n", StandardCharsets.UTF_8);
        M3FileCpuFacts.Search overlapping = M3FileCpuFacts.of(M3File.load(lineOverlap))
                .search("aa\naa", 10);
        yes(Arrays.equals(new int[] {0, 3}, overlapping.positions())
                && Arrays.equals(new long[] {7L}, overlapping.lineMask()),
                "coalesced multi-line hit intervals preserve every affected line");
        yes(Arrays.equals(new int[] {8}, facts.search("\ud83d\ude42ab", 4).positions()),
                "supplementary UTF-16 source coordinates");
        yes(facts.search("", 50).size() == original.length() + 1,
                "empty needle includes one EOF position");
        long[] mutated = facts.search("ab", 5).lineMask();
        mutated[0] = 0;
        yes(facts.search("ab", 5).lineMask()[0] == 2L, "defensive line mask");
        expect(IllegalArgumentException.class, () -> facts.search("a", -1));

        Files.writeString(filePath, "aaaa\r\nxy\ud83d\ude42ab\n", StandardCharsets.UTF_8);
        M3File next = M3File.load(filePath);
        var diff = facts.changedSpan(next);
        yes(diff.fromStart() == 6 && diff.fromEnd() == 8
                && diff.toStart() == 6 && diff.toEnd() == 8,
                "reuse original donor minimal changed span");
        yes(facts.source().text().equals(text), "original immutable M3File unaffected");
        yes(facts.retainedPrimitiveBytes() > 0, "lazy primitive indexes occupy bounded memory");
        facts.discardDerivedFacts();
        yes(facts.retainedPrimitiveBytes() == 0, "reclaim derived arrays and cached probes");
    }

    private static void astFacts(Path work) throws Exception {
        Path path = work.resolve("Sample.java");
        String source = "package demo; public class Sample { int result() { return 42; } }";
        Files.writeString(path, source, StandardCharsets.UTF_8);
        M3File file = M3File.load(path);
        var compiler = ToolProvider.getSystemJavaCompiler();
        var snapshots = new ArrayList<M3FileCpuFacts>();
        var diagnostics = new javax.tools.DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            JavaFileObject originalUnit = new SimpleJavaFileObject(file.uri(), JavaFileObject.Kind.SOURCE) {
                @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return file.text(); }
            };
            JavacTask originalTask = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "-source", "21"), null, List.of(originalUnit));
            try (M3AST handle = M3AST.open(originalTask, true)) {
                handle.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit live) throws Exception {
                        snapshots.add(M3FileCpuFacts.fromOriginalParse(file, live, null));
                    }
                });
                originalTask.parse(); // the one authoritative javac parse.
            }
        }
        yes(snapshots.size() == 1, "captured exactly once from original parsed event");
        M3FileCpuFacts facts = snapshots.get(0);
        yes(facts.source() == file && facts.syntax().isPresent(), "same file and original AST");
        yes(facts.visitedAstNodes() > 0, "original compiler structural facts");
        int count = facts.kindCount(Tree.Kind.CLASS);
        yes(count == 1 && facts.kindCount(Tree.Kind.CLASS) == count,
                "one lazily computed structural-kind histogram");
        yes(facts.retainedPrimitiveBytes() > 0, "only one small histogram retained");
        Path otherPath = work.resolve("Foreign.java");
        Files.writeString(otherPath, source, StandardCharsets.UTF_8);
        M3File other = M3File.load(otherPath);
        expect(IllegalArgumentException.class, () -> M3FileCpuFacts.of(other, facts.syntax().orElseThrow()));
        facts.discardDerivedFacts();
        yes(facts.retainedPrimitiveBytes() == 0, "retained original AST is not copied");
    }

    private static void concurrentFacts(Path work) throws Exception {
        Path path = work.resolve("parallel.txt");
        Files.writeString(path, "key\nkey\r\nkey", StandardCharsets.UTF_8);
        M3FileCpuFacts facts = M3FileCpuFacts.of(M3File.load(path));
        AtomicReference<Throwable> failure = new AtomicReference<>();
        var workers = new ArrayList<Thread>();
        for (int i = 0; i < 4; i++) {
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < 100; j++) {
                        M3FileCpuFacts.Search search = facts.search("key", 20);
                        if (!search.complete() || search.size() != 3
                                || !Arrays.equals(search.lineMask(), new long[] {7L})) {
                            throw new AssertionError("concurrent source index inconsistency");
                        }
                    }
                } catch (Throwable error) { failure.compareAndSet(null, error); }
            });
            workers.add(thread);
            thread.start();
        }
        for (Thread thread : workers) thread.join();
        yes(failure.get() == null, "thread-safe memoized source observations");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 0) throw new IllegalArgumentException("no arguments");
        Path work = Files.createTempDirectory("m3-cpu-file-facts-");
        try {
            textFacts(work);
            astFacts(work);
            concurrentFacts(work);
            System.out.println("M3_FILE_CPU_FACTS_PASS checks=" + checks);
        } finally {
            try (var walk = Files.walk(work)) {
                for (Path file : walk.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
}
