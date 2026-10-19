// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.CpuTools;
import com.m3.tools.cpu.CpuToolsCli;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import jdk.m3.io.M3File;
import jdk.m3.text.ExecutionResult;
import jdk.m3.text.Limits;
import jdk.m3.text.Node;
import jdk.m3.text.Plan;
import jdk.m3.text.Receipt;
import jdk.m3.text.TextFrame;

/** Composition, admission, replay, literal-search oracle and linked CLI integration. */
public final class M3CpuToolsTest {
    private static int checks;
    private M3CpuToolsTest() { }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-cpu-integration-");
        try {
            registryAndOperations();
            dagAndReplay();
            budgetsAndCancellation();
            fileAndLiteral(work);
            cli(work);
            System.out.println("M3_CPU_INTEGRATION_PASS checks=" + checks
                    + " storage=" + Boolean.getBoolean("m3.test.requireStorage"));
        } finally {
            try (var paths = Files.walk(work)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void registryAndOperations() {
        var session = CpuTools.session();
        var ids = session.registry().bindings().stream().map(b -> b.id()).toList();
        check(ids.size() == 22, "exact registry admission");
        check(ids.equals(ids.stream().sorted().distinct().toList()), "stable unique operation inventory");
        check(CpuTools.implementationSha256().matches("[0-9a-f]{64}"), "actual module-set seal");
        check(session.registry().sha256().equals(CpuTools.session().registry().sha256()), "repeatable registry");
        check(!session.registry().sha256().equals(CpuTools.session(new Limits(4, 4, 10, 1000), null)
                .registry().sha256()), "binding identity includes budgets");
        long modules = ModuleLayer.boot().modules().stream().map(Module::getName)
                .filter(n -> n.startsWith("com.m3.tools.") || n.startsWith("jdk.m3.")).count();
        check(modules == 20, "all twenty tool modules resolved");

        expectText("b\r\na\rb\n", List.of("text.identity"), "b\r\na\rb\n");
        expectText("b\na\nb\n", List.of("text.sort", "text.uniq"), "a\nb\n");
        expectText("b\na\nc\n", List.of("text.sort.reverse"), "c\nb\na\n");
        expectText("cafe\u0301 beta caf\u00e9\n", List.of("text.normalize.nfc", "text.words", "text.frequency"),
                "beta\t1\ncaf\u00e9\t2\n");
        expectText("\uff21\uff22\uff23\r\n", List.of("text.normalize.nfkc"), "ABC\r\n");
        expectText("a\ta\na\\b\na\ta\n", List.of("text.frequency"), "a\\ta\t2\na\\\\b\t1\n");
        expectText("A\ud83d\ude42B", List.of("text.ngram.2"), "A\\ud83d\t1\n\ud83d\ude42\t1\n\\ude42B\t1\n");
        expectText("A\ud83d\ude42B", List.of("text.ngram.3"), "A\ud83d\ude42\t1\n\ud83d\ude42B\t1\n");
        check(CpuTools.escape("\\uD800\ud800\t\r\n").equals("\\\\uD800\\ud800\\t\\r\\n"), "unambiguous escaped UTF-16 fields");
        expectText("abc", List.of("hash.sha256.utf8"),
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad\n");
        expectText("0\n127\n128\n16384\n9223372036854775807\n", List.of("codec.varint.encode"),
                "00\n7f\n8001\n808001\nffffffffffffffff7f\n");
        expectText("0\n127\n128\n16384\n9223372036854775807\n",
                List.of("codec.varint.encode", "codec.varint.decode"),
                "0\n127\n128\n16384\n9223372036854775807\n");
        refuseOperation("codec.varint.encode", "9223372036854775808\n");
        refuseOperation("codec.varint.encode", "-1\n");
        refuseOperation("codec.varint.decode", "8000\n");
        refuseOperation("codec.varint.decode", "ffffffffffffffffff01\n");
        expectText("4\n0\t2\n1\t2\n2\t3\n", List.of("graph.topology"), "0\n1\n2\n3\n");
        expectText("0\n", List.of("graph.topology"), "");
        refuseOperation("graph.topology", "2\n0\t1\n1\t0\n");
        refuseOperation("graph.topology", "2\n0\t2\n");
        refuseOperation("graph.topology", "2\n0 1\n");
        expectText("5\t0\n0\t1\t9\n0\t2\t2\n2\t1\t3\n1\t3\t1\n",
                List.of("graph.shortest-paths"), "0\t0\n1\t5\n2\t2\n3\t6\n4\t9223372036854775807\n");
        refuseOperation("graph.shortest-paths", "2\t0\n0\t1\t-1\n");
        refuseOperation("graph.shortest-paths", "2\t2\n");
        expectText("alpha beta\nbeta gamma\n", List.of("graph.tsort"), "alpha\nbeta\ngamma\n");
        expectText("1\n2\n3\n4\n", List.of("statistics.moments"),
                "count\t4\nmean\t2.5\npopulationVariance\t1.25\nmin\t1.0\nmax\t4.0\n");
        refuseOperation("statistics.moments", "NaN\n");
        refuseOperation("statistics.moments", "Infinity\n");
        expectText("name,age\r\nAda,36\r\n", List.of("structured.csv.first-column"), "name\nAda\n");
        expectText("name\tage\nAda\t36\n", List.of("structured.tsv.first-column"), "name\nAda\n");
        expectText("B=two\nA=one\n", List.of("structured.env"), "A\tone\nB\ttwo\n");
        expectText("x\ny\n", List.of("text.number"), "1\tx\n2\ty\n");
        expectText("a".repeat(81) + "\n", List.of("text.fold.80"), "a".repeat(80) + "\na\n");
    }

    private static void dagAndReplay() {
        var session = CpuTools.session();
        var identity = session.registry().require("text.identity");
        var reverse = session.registry().require("text.sort.reverse");
        var a = Node.of("a", identity);
        var b = Node.of("b", reverse);
        var c = Node.of("c", identity, "a", "b");
        var plan = new Plan(List.of(c, b, a), "c");
        var input = TextFrame.fromString("a\nb\n");
        var result = session.execute(plan, input);
        check(result.output().text().equals("a\nb\nb\na\n"), "ordered fan-in uses incumbent DAG");
        check(result.receipt().steps().size() == 3, "one receipt for each executed atom");
        check(session.replay(plan, input, result).equals(result), "exact re-execution");
        check(session.execute(new Plan(List.of(a, c, b), "c"), input).equals(result), "input node order has no semantic effect");
        expect(IllegalArgumentException.class, () -> session.replay(plan, TextFrame.fromString("b\na\n"), result));
        var receipt = result.receipt();
        var steps = new ArrayList<>(receipt.steps());
        java.util.Collections.reverse(steps);
        var forged = new ExecutionResult(result.output(), new Receipt(receipt.planSha256(), receipt.registrySha256(),
                receipt.policySha256(), receipt.inputSha256(), receipt.outputSha256(), steps));
        expect(IllegalStateException.class, () -> session.replay(plan, input, forged));
        var monitor = new CountMonitor();
        var checked = CpuTools.session(Limits.standard(), monitor);
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(
                new Node("a", identity.id(), "0".repeat(64), List.of())), "a"), input));
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(
                Node.of("a", identity, "b"), Node.of("b", identity, "a")), "a"), input));
        expect(IllegalArgumentException.class, () -> checked.execute(new Plan(List.of(Node.of("a", identity),
                Node.of("unused", identity)), "a"), input));
        check(monitor.messages == 0 && monitor.work == 0, "whole graph refusal precedes any atom invocation");
        expect(IllegalArgumentException.class, () -> session.pipeline(List.of("missing")));
        expect(IllegalArgumentException.class, () -> session.pipeline(List.of()));
    }

    private static void budgetsAndCancellation() {
        var input = TextFrame.fromString("one two three");
        var small = CpuTools.session(new Limits(4, 3, 2, 100), null);
        expect(IllegalArgumentException.class, () -> small.execute(small.pipeline(List.of("text.words")), input));
        var bytes = CpuTools.session(new Limits(4, 3, 5, 2), null);
        expect(IllegalArgumentException.class, () -> bytes.execute(bytes.pipeline(List.of("text.identity")), input));
        var edges = CpuTools.session(new Limits(4, 0, 5, 100), null);
        expect(IllegalArgumentException.class, () -> edges.execute(edges.pipeline(List.of("graph.topology")),
                TextFrame.fromString("2\n0\t1\n")));
        expect(IllegalArgumentException.class, () -> small.pipeline(List.of("text.identity", "text.identity",
                "text.identity", "text.identity", "text.identity")));
        var monitor = new CountMonitor();
        var session = CpuTools.session(Limits.standard(), monitor);
        var plan = session.pipeline(List.of("text.identity", "text.sort"));
        monitor.cancelled = true;
        expect(CancellationException.class, () -> session.execute(plan, input));
        check(monitor.messages == 0, "cancel before operation");
        monitor.cancelled = false;
        monitor.cancelAfterWork = true;
        expect(CancellationException.class, () -> session.execute(plan, input));
        check(monitor.work == 1 && monitor.messages == 1, "cancel between atoms without success receipt");
        check(monitor.finished == 0, "session never completes borrowed monitor");
    }

    private static void fileAndLiteral(Path work) throws Exception {
        Path path = work.resolve("source.txt");
        Files.writeString(path, "ababa\ud83d\ude42\r\n", StandardCharsets.UTF_8);
        M3File source = M3File.load(path);
        var session = CpuTools.session();
        var result = session.execute(List.of("text.identity"), source);
        check(CpuTools.sourceReceipt(source, result).matches("[0-9a-f]{64}"), "source-bound DAG receipt");
        check(CpuTools.sourceReceipt(source, result).equals(CpuTools.sourceReceipt(source, result)), "repeat source seal");
        Path copied = work.resolve("copied.txt");
        Files.copy(path, copied);
        check(!CpuTools.sourceReceipt(source, result).equals(CpuTools.sourceReceipt(M3File.load(copied), result)),
                "source receipt binds file location as well as bytes");
        Files.writeString(path, "changed on disk");
        check(session.execute(List.of("text.identity"), source).equals(result), "execution uses admitted snapshot without reopen");
        var changed = M3File.load(path);
        expect(IllegalArgumentException.class, () -> CpuTools.sourceReceipt(changed, result));
        var zeroLines = CpuTools.session(new Limits(4, 4, 0, 100), null);
        expect(IllegalArgumentException.class, () -> zeroLines.execute(List.of("text.identity"), source));
        Path mixed = work.resolve("mixed-endings.txt");
        Files.writeString(mixed, "a\r\nb\rc\n");
        M3File threeLines = M3File.load(mixed);
        var twoLines = CpuTools.session(new Limits(4, 4, 2, 100), null);
        expect(IllegalArgumentException.class, () -> twoLines.execute(List.of("text.identity"), threeLines));
        var hits = CpuTools.literal(source, "aba", 1);
        check(Arrays.equals(hits.positions(), new int[]{0}) && !hits.complete(), "overlapping capped prefix");
        int[] mutable = hits.positions();
        mutable[0] = 100;
        check(hits.positions()[0] == 0, "immutable match positions");
        check(CpuTools.verifyLiteral(source, "aba", 1, hits, null), "search re-execution receipt");
        check(!CpuTools.verifyLiteral(source, "aba", 2, hits, null), "cap is receipt-bound");
        check(!CpuTools.verifyLiteral(changed, "aba", 1, hits, null), "source is receipt-bound");
        expect(IllegalArgumentException.class, () -> CpuTools.literal(source, "", -1));
        expect(IllegalArgumentException.class, () -> CpuTools.literal(source, "", 1_000_001));
        CountMonitor cancelled = new CountMonitor();
        cancelled.cancelled = true;
        expect(CancellationException.class, () -> CpuTools.literal(source, "aba", 2, cancelled));
        cancelled.cancelled = false;
        cancelled.cancelAfterWork = true;
        expect(CancellationException.class, () -> CpuTools.literal(source, "aba", 2, cancelled));

        Random random = new Random(612721L);
        String[] alphabet = {"a", "b", "\ud83d\ude42", "\u03c0", "\r\n", "\u0000"};
        for (int round = 0; round < 240; round++) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(60); i > 0; i--) text.append(alphabet[random.nextInt(alphabet.length)]);
            String spelling = text.toString();
            Files.writeString(path, spelling, StandardCharsets.UTF_8);
            M3File file = M3File.load(path);
            String needle;
            if (round % 3 == 0) needle = "";
            else if (round % 3 == 1 && !spelling.isEmpty()) {
                int start = random.nextInt(spelling.length());
                needle = spelling.substring(start, Math.min(spelling.length(), start + random.nextInt(5) + 1));
            } else needle = "aba";
            List<Integer> oracle = new ArrayList<>();
            for (int i = 0; i <= spelling.length() - needle.length(); i++) {
                boolean matches = true;
                for (int j = 0; j < needle.length(); j++) {
                    if (spelling.charAt(i + j) != needle.charAt(j)) { matches = false; break; }
                }
                if (matches) oracle.add(i);
            }
            for (int cap : new int[]{0, 1, 3, 100}) {
                var actual = CpuTools.literal(file, needle, cap);
                check(Arrays.equals(actual.positions(), oracle.stream().limit(cap).mapToInt(Integer::intValue).toArray()),
                        "independent exhaustive UTF-16 literal oracle");
                check(actual.complete() == (oracle.size() <= cap), "exact truncation bit");
                check(CpuTools.verifyLiteral(file, needle, cap, actual, null), "random source receipt replay");
            }
        }
        requireStorage(source.text());
        requireStorage(result.output().text());
        requireStorage(String.join("|", source.text(), source.text()));
    }

    private static void cli(Path work) throws Exception {
        check(invoke("--help").code == 0, "CLI help");
        check(invoke().code == 2 && invoke("--missing").code == 2, "CLI usage failure");
        var first = invoke("--list");
        check(first.code == 0 && first.output.lines().count() == 22, "CLI exact operation listing");
        check(first.equals(invoke("--list")), "repeatable CLI listing");
        check(invoke("--self-test").equals(invoke("--self-test")), "repeatable CLI self-test");
        check(invoke("--self-test").code == 0, "CLI self-test succeeds");
        Path text = work.resolve("cli.txt");
        Files.writeString(text, "b\na\nb\n");
        var run = invoke("--run", "text.sort,text.uniq", text.toString());
        check(run.code == 0 && run.output.equals("a\nb\n") && run.error.contains("receipt\tdag="), "CLI M3File pipeline");
        check(run.equals(invoke("--run", "text.sort,text.uniq", text.toString())), "CLI source receipt repeat");
        var find = invoke("--find", "b", text.toString(), "1");
        check(find.code == 0 && find.output.equals("0\n") && find.error.contains("complete=false"), "CLI explicit search truncation");
        var invalid = invoke("--run", "missing", work.resolve("absent.txt").toString());
        check(invalid.code == 1 && invalid.error.contains("unknown binding"), "binding refusal precedes file read");
        Files.writeString(text, "1\n0\t1\n");
        var invalidGraph = invoke("--run", "graph.topology", text.toString());
        check(invalidGraph.code == 1 && invalidGraph.output.isEmpty()
                && invalidGraph.error.contains("CPU_TOOLS_REFUSED"), "invalid graph is a CLI refusal, not an uncaught exception");
        Path java = work.resolve("CliSource.java");
        Files.writeString(java, "class CliSource { int value; int sum(int a) { return a + value; } }\n");
        var ast = invoke("--ast", java.toString());
        check(ast.code == 0 && ast.output.contains("method\tsum\t1\tfalse")
                && ast.error.contains("receipt\tast=") && ast.error.contains("phase=PARSED"), "CLI real M3AST syntax inventory");
        check(ast.equals(invoke("--ast", java.toString())), "CLI AST source and fact receipt repeat");
        check(!Files.exists(work.resolve("CliSource.class")), "CLI parse emits no classes");
    }

    private record CliResult(int code, String output, String error) { }
    private static CliResult invoke(String... args) {
        var output = new ByteArrayOutputStream();
        var error = new ByteArrayOutputStream();
        int code;
        try (var out = new PrintStream(output, true, StandardCharsets.UTF_8);
             var err = new PrintStream(error, true, StandardCharsets.UTF_8)) {
            code = CpuToolsCli.run(args, out, err);
        }
        return new CliResult(code, output.toString(StandardCharsets.UTF_8), error.toString(StandardCharsets.UTF_8));
    }

    private static void requireStorage(String text) throws Exception {
        if (!Boolean.getBoolean("m3.test.requireStorage")) return;
        var field = String.class.getDeclaredField("m3");
        field.setAccessible(true);
        check(!text.isEmpty() && field.get(text) != null, "actual M3String storage, not only API presence");
    }

    private static void expectText(String input, List<String> operations, String expected) {
        var session = CpuTools.session();
        var plan = session.pipeline(operations);
        var frame = TextFrame.fromString(input);
        var result = session.execute(plan, frame);
        check(result.output().text().equals(expected), operations + " golden output; actual=" + CpuTools.escape(result.output().text()));
        check(session.replay(plan, frame, result).equals(result), "operator output and receipt replay " + operations);
    }

    private static void refuseOperation(String operation, String text) {
        var session = CpuTools.session();
        expect(IllegalArgumentException.class, () -> session.execute(session.pipeline(List.of(operation)), TextFrame.fromString(text)));
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static void expect(Class<? extends Throwable> type, Checked operation) {
        checks++;
        try { operation.run(); }
        catch (Throwable failure) {
            if (type.isInstance(failure)) return;
            throw new AssertionError("expected " + type.getName() + " but got " + failure, failure);
        }
        throw new AssertionError("expected " + type.getName());
    }

    private static final class CountMonitor implements ProgressMonitor {
        boolean cancelled;
        boolean cancelAfterWork;
        int messages;
        long work;
        int finished;
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void message(String ignored) { messages++; }
        @Override public void worked(long amount) { work += amount; if (cancelAfterWork) cancelled = true; }
        @Override public void done() { finished++; }
    }
}
