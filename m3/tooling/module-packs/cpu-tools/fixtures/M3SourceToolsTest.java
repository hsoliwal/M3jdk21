// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.m3.tools.cpu.M3AstTools;
import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ProgressMonitor;
import java.util.concurrent.ProgressMonitors;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.io.M3File;

/** Independent byte/UTF-16 digest oracles and actual M3 compiler lifecycle tests. */
public final class M3SourceToolsTest {
    private static int checks;

    private M3SourceToolsTest() { }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-source-tools-fixtures-");
        try {
            fileContracts(work);
            sourceChangeContracts(work);
            astContracts(work);
            compilerLifetimeContracts(work);
            cancellationContracts(work);
            System.out.println("M3_SOURCE_TOOLS_PASS checks=" + checks
                    + " storage=" + Boolean.getBoolean("m3.test.requireStorage"));
        } finally {
            try (var paths = Files.walk(work)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void fileContracts(Path work) throws Exception {
        Path path = work.resolve("unicode.txt");
        String text = "\uFEFF\u03c0 A\ud83d\ude42\r\nline\rthree\n\u0000tail";
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Files.write(path, bytes);
        CountMonitor progress = new CountMonitor();
        M3File file = M3File.load(path, bytes.length, progress);
        check(file.text().equals(text), "strict decoder preserves BOM, NUL and line endings");
        check(file.path().equals(path.toAbsolutePath().normalize()) && file.uri().equals(path.toUri()), "path identity");
        check(file.text() == file.toString(), "one retained public String reference");
        check(file.byteLength() == bytes.length && file.length() == text.length(), "byte/character geometry");
        check(file.rawSha256().equals(sha(bytes)), "independent raw SHA oracle");
        check(file.utf16Sha256().equals(utf16(text)), "independent UTF-16 SHA oracle");
        check(!file.rawSha256().equals(file.utf16Sha256()), "byte and character seals are distinct");
        check(progress.begun == 1 && progress.finished == 1 && progress.worked == bytes.length, "one stream's byte progress");
        file.verifyUnchanged();
        for (int start = 0; start <= text.length(); start++) {
            for (int end = start; end <= text.length(); end++) {
                var range = file.range(start, end);
                check(range.source() == file && range.length() == end - start, "range retains original owner");
                for (int i = 0; i < range.length(); i++) check(range.charAt(i) == text.charAt(start + i), "shared range characters");
                check(range.utf16Sha256().equals(utf16(text.substring(start, end))), "range digest including split surrogates");
                check(range.subSequence(0, range.length()).source() == file, "nested range retains original owner");
            }
        }
        expect(IndexOutOfBoundsException.class, () -> file.range(-1, 0));
        expect(IndexOutOfBoundsException.class, () -> file.range(0, file.length() + 1));
        expect(IndexOutOfBoundsException.class, () -> file.range(1, 1).charAt(0));
        expect(IOException.class, () -> M3File.load(path, bytes.length - 1));
        expect(IllegalArgumentException.class, () -> M3File.load(path, -1));
        expect(IllegalArgumentException.class, () -> M3File.load(path, M3File.MAX_BYTES + 1));
        expect(IOException.class, () -> M3File.load(work));

        Path empty = work.resolve("empty.txt");
        Files.write(empty, new byte[0]);
        M3File zero = M3File.load(empty, 0);
        check(zero.length() == 0 && zero.byteLength() == 0, "empty zero-budget source");
        check(zero.rawSha256().equals(sha(new byte[0])) && zero.utf16Sha256().equals(utf16("")), "empty digests");
        for (byte[] invalid : List.of(new byte[] {(byte) 0xc0, (byte) 0xaf},
                new byte[] {(byte) 0xed, (byte) 0xa0, (byte) 0x80},
                new byte[] {(byte) 0xf0, (byte) 0x9f, (byte) 0x98})) {
            Files.write(work.resolve("invalid.txt"), invalid);
            expect(IOException.class, () -> M3File.load(work.resolve("invalid.txt")));
        }

        Random random = new Random(711204L);
        for (int round = 0; round < 24; round++) {
            StringBuilder generated = new StringBuilder("a".repeat(8189 + round));
            for (int i = 0; i < 80; i++) {
                int codePoint;
                do { codePoint = random.nextInt(0x110000); }
                while (codePoint >= 0xd800 && codePoint <= 0xdfff);
                generated.appendCodePoint(codePoint);
            }
            byte[] encoded = generated.toString().getBytes(StandardCharsets.UTF_8);
            Files.write(work.resolve("buffer-boundary.txt"), encoded);
            M3File observed = M3File.load(work.resolve("buffer-boundary.txt"));
            check(observed.text().contentEquals(generated), "UTF-8 decoder buffer seam");
            check(observed.rawSha256().equals(sha(encoded)), "random raw digest");
            check(observed.utf16Sha256().equals(utf16(generated)), "random UTF-16 digest");
        }

        for (var field : M3File.class.getDeclaredFields()) {
            check(field.getType() != byte[].class && field.getType() != char[].class, "no retained duplicate byte/char payload");
        }
        Path link = work.resolve("source-link.txt");
        Files.createSymbolicLink(link, path.getFileName());
        expect(IOException.class, () -> M3File.load(link));
        Path realDirectory = Files.createDirectory(work.resolve("real-directory"));
        Files.writeString(realDirectory.resolve("child.txt"), "child");
        Path directoryLink = work.resolve("directory-link");
        Files.createSymbolicLink(directoryLink, realDirectory.getFileName());
        expect(IOException.class, () -> M3File.load(directoryLink.resolve("child.txt")));
        checkStorage(file.text());
        checkStorage(String.join("|", file.text(), file.text()));
    }

    private static void sourceChangeContracts(Path work) throws Exception {
        Path path = work.resolve("changed.txt");
        Files.writeString(path, "original", StandardCharsets.UTF_8);
        M3File before = M3File.load(path);
        FileTime modified = Files.getLastModifiedTime(path);
        Files.writeString(path, "modified", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(path, modified);
        expect(IOException.class, before::verifyUnchanged);
        check(before.text().equals("original"), "snapshot remains immutable after external edit");
        Files.writeString(path, "x".repeat(30_000), StandardCharsets.UTF_8);
        FileTime initialTime = Files.getLastModifiedTime(path);
        ProgressMonitor changing = new ProgressMonitor() {
            private boolean changed;
            @Override public void worked(long ignored) {
                if (!changed) {
                    changed = true;
                    try {
                        Files.writeString(path, "y".repeat(30_000), StandardCharsets.UTF_8);
                        Files.setLastModifiedTime(path, FileTime.fromMillis(initialTime.toMillis() + 20_000));
                    } catch (IOException failure) { throw new java.io.UncheckedIOException(failure); }
                }
            }
        };
        expect(IOException.class, () -> M3File.load(path, changing));
    }

    private static void astContracts(Path work) throws Exception {
        Path source = work.resolve("Example.java");
        String code = "package demo;\r\nimport java.util.List;\r\nimport static java.lang.Math.max;\r\n"
                + "/* \ud83d\ude42 source coordinates */ public class Example<T> {\r\n"
                + "  private int value; public Example(int value) { this.value = value; }\r\n"
                + "  public int sum(int n) { String fake = \"class Bogus {}\"; return max(value,n); }\r\n"
                + "  String block = \"\"\"\nclass Shadow {}\n\"\"\";\r\n"
                + "  class Nested { void run() {} }\r\n}\r\n";
        Files.writeString(source, code, StandardCharsets.UTF_8);
        M3File file = M3File.load(source);
        CountMonitor progress = new CountMonitor();
        var all = M3AstTools.parse(List.of(file), progress);
        check(all.size() == 1, "one snapshot per compiler input");
        var snapshot = all.get(0);
        check(snapshot.phase() == M3AST.Phase.PARSED, "syntax facts are explicitly parse-phase");
        check(snapshot.source().uri().equals(file.uri()), "AST source URI");
        check(snapshot.source().rawSha256().orElseThrow().equals(file.rawSha256()), "exact file byte binding");
        check(snapshot.source().utf16Sha256().equals(file.utf16Sha256()), "exact compiler character binding");
        check(snapshot.visitedNodes() == snapshot.structureCount() + snapshot.unpositionedNodes(), "position accounting");
        check(snapshot.structureCount() > 25 && snapshot.retainedPrimitiveBytes() > 0, "structural primitive plane");
        for (int i = 0; i < snapshot.structureCount(); i++) {
            var fact = snapshot.structure(i);
            check(fact.span().start() >= 0 && fact.span().end() <= file.length(), "source-bound UTF-16 range");
            check(fact.parent() < i && fact.depth() <= M3AstTools.MAX_DEPTH, "parent precedes child");
        }
        check(snapshot.imports().size() == 2, "import inventory");
        check(snapshot.imports().get(0).qualifiedName().equals("java.util.List"), "qualified import name");
        check(!snapshot.imports().get(0).staticImport() && snapshot.imports().get(1).staticImport(), "static import distinction");
        check(snapshot.symbols().stream().anyMatch(s -> s.name().equals("Example") && s.kind().equals("CLASS")), "declared class");
        check(snapshot.symbols().stream().anyMatch(s -> s.name().equals("T") && s.kind().equals("TYPE_PARAMETER")), "type parameter");
        check(snapshot.symbols().stream().noneMatch(s -> s.name().equals("Bogus") || s.name().equals("Shadow")), "opaque strings and text blocks");
        check(snapshot.methods().size() == 3, "constructors and nested methods");
        check(snapshot.methods().stream().anyMatch(m -> m.constructor() && m.parameterCount() == 1), "constructor fact");
        var sum = snapshot.methods().stream().filter(m -> m.name().equals("sum")).findFirst().orElseThrow();
        check(sum.parameterCount() == 1 && sum.body().isPresent(), "method shape");
        check(file.range(sum.span().start(), sum.span().end()).toString().contains("int sum"), "method range is source text");
        check(progress.begun == 1 && progress.finished == 1 && progress.worked == snapshot.visitedNodes(), "compiler capture progress");
        expect(UnsupportedOperationException.class, () -> snapshot.methods().clear());
        expect(UnsupportedOperationException.class, () -> snapshot.structures().set(0, snapshot.structure(0)));
        expect(IllegalArgumentException.class, () -> M3AstTools.parse(List.of(file, file)));
        var replay = M3AstTools.parse(List.of(file)).get(0);
        check(snapshot.structures().equals(replay.structures()) && snapshot.symbols().equals(replay.symbols())
                && snapshot.methods().equals(replay.methods()), "deterministic detached syntax replay");
        check(snapshot.sha256().matches("[0-9a-f]{64}") && snapshot.sha256().equals(replay.sha256()),
                "actual fresh compiler parse reproduces source/inventory receipt");
        Files.writeString(source, code.replace("source coordinates", "source coordinateX"), StandardCharsets.UTF_8);
        var differentSource = M3AstTools.parse(List.of(M3File.load(source))).get(0);
        check(snapshot.structures().equals(differentSource.structures())
                && snapshot.symbols().equals(differentSource.symbols()), "same syntax shape at same URI after comment edit");
        check(!snapshot.sha256().equals(differentSource.sha256()), "receipt distinguishes exact source despite identical syntax facts");
        check(!Files.exists(work.resolve("Example.class")) && !Files.exists(work.resolve("demo")), "parse does not generate classes");
        Files.writeString(source, "broken current disk contents", StandardCharsets.UTF_8);
        var retained = M3AstTools.parse(List.of(file)).get(0);
        check(retained.source().equals(snapshot.source()), "compiler uses admitted snapshot without reopening file");
        check(retained.sha256().equals(snapshot.sha256()), "frozen source receipt survives unrelated disk change");
        expect(IOException.class, file::verifyUnchanged);
        Path invalid = work.resolve("Invalid.java");
        Files.writeString(invalid, "class Invalid { void broken( {", StandardCharsets.UTF_8);
        expect(IOException.class, () -> M3AstTools.parse(List.of(M3File.load(invalid))));
        Path deep = work.resolve("Deep.java");
        Files.writeString(deep, "class Deep { void f() {" + "{".repeat(M3AstTools.MAX_DEPTH + 8)
                + ";" + "}".repeat(M3AstTools.MAX_DEPTH + 8) + "} }", StandardCharsets.UTF_8);
        expect(IllegalArgumentException.class, () -> M3AstTools.parse(List.of(M3File.load(deep))));
        check(M3AstTools.parse(List.of()).isEmpty(), "empty source set");
    }

    private static void compilerLifetimeContracts(Path work) throws Exception {
        Path path = work.resolve("Lifetime.java");
        Files.writeString(path, "class Lifetime { int value; void method() {} }", StandardCharsets.UTF_8);
        M3File file = M3File.load(path);
        Files.writeString(path, "class Changed {}", StandardCharsets.UTF_8);
        M3File wrongGeneration = M3File.load(path);
        var compiler = ToolProvider.getSystemJavaCompiler();
        var snapshots = new ArrayList<M3AstTools.Snapshot>();
        M3AST.Unit[] expired = new M3AST.Unit[1];
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null,
                    List.of(input(file.uri(), file.text())));
            try (M3AST frontend = M3AST.open(task, true)) {
                frontend.addConsumer(M3AstTools.consumer(snapshots::add));
                frontend.addConsumer(new M3AST.Consumer() {
                    @Override public void parsed(M3AST.Unit unit) throws Exception {
                        expired[0] = unit;
                        expect(IllegalArgumentException.class,
                                () -> M3AstTools.capture(unit, wrongGeneration, ProgressMonitor.NONE));
                        var bound = M3AstTools.capture(unit, file, ProgressMonitor.NONE);
                        check(bound.source().rawSha256().orElseThrow().equals(file.rawSha256()), "exact source binding inside live callback");
                        var boundReplay = M3AstTools.capture(unit, file, ProgressMonitor.NONE);
                        check(bound.sha256().equals(boundReplay.sha256()), "repeated live capture receipt");
                        check(!bound.sha256().equals(snapshots.get(0).sha256()), "receipt distinguishes raw-byte binding presence");
                        check(M3AstTools.capture(unit).sha256().equals(snapshots.get(0).sha256()),
                                "two consumers of one actual compiler unit reproduce receipt");
                    }
                });
                task.parse();
                check(frontend.summary().parsedUnits() == 1, "existing task parsed once for multiple consumers");
            }
        }
        check(snapshots.size() == 1 && snapshots.get(0).source().rawSha256().isEmpty(), "unbound compiler source does not invent byte digest");
        expect(IllegalStateException.class, () -> expired[0].tree());
        check(!snapshots.get(0).methods().isEmpty(), "facts remain usable after compiler lifetime");
        for (var field : M3AstTools.Snapshot.class.getDeclaredFields()) {
            String name = field.getType().getName();
            check(!name.startsWith("com.sun.source.") || field.getType() == M3AST.Phase.class,
                    "snapshot retains no compiler tree, type or session");
        }

        String unpaired = "class Surrogate { /* " + (char) 0xd800 + " */ void ok() {} }";
        var unicode = new ArrayList<M3AstTools.Snapshot>();
        try (var manager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, manager, null, List.of("-proc:none"), null,
                    List.of(input(URI.create("string:///Surrogate.java"), unpaired)));
            try (M3AST frontend = M3AST.open(task, true)) {
                frontend.addConsumer(M3AstTools.consumer(unicode::add));
                task.parse();
            }
        }
        check(unicode.get(0).source().utf16Sha256().equals(utf16(unpaired)), "compiler seal preserves unpaired UTF-16 code units");
        check(unicode.get(0).sha256().matches("[0-9a-f]{64}"), "receipt accepts compiler source containing isolated surrogate");
    }

    private static void cancellationContracts(Path work) throws Exception {
        Path file = work.resolve("Cancel.java");
        Files.writeString(file, "class Cancel { int value; }", StandardCharsets.UTF_8);
        M3File source = M3File.load(file);
        ProgressMonitor cancelled = new ProgressMonitor() {
            @Override public boolean isCancelled() { return true; }
        };
        expect(CancellationException.class, () -> M3File.load(file, cancelled));
        expect(CancellationException.class, () -> source.range(0, 1, cancelled));
        expect(CancellationException.class, () -> source.range(0, 1).utf16Sha256(cancelled));
        expect(CancellationException.class, () -> source.verifyUnchanged(cancelled));
        expect(CancellationException.class, () -> M3AstTools.parse(List.of(source), cancelled));
        try (var scope = ProgressMonitors.bind(cancelled)) {
            check(scope != null, "bound default monitor");
            expect(CancellationException.class, () -> M3File.load(file));
            expect(CancellationException.class, () -> M3File.load(file, (ProgressMonitor) null));
            expect(CancellationException.class, () -> M3AstTools.parse(List.of(source)));
        }
        CountMonitor midParse = new CountMonitor() {
            @Override public boolean isCancelled() { return worked >= 3; }
        };
        expect(CancellationException.class, () -> M3AstTools.parse(List.of(source), midParse));
        check(midParse.finished == 1, "mid-compiler cancellation closes progress");
        Thread.currentThread().interrupt();
        try { expect(CancellationException.class, () -> M3File.load(file)); }
        finally { check(Thread.interrupted(), "interruption remains observable"); }
    }

    private static SimpleJavaFileObject input(URI uri, String text) {
        return new SimpleJavaFileObject(uri, JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return text; }
        };
    }

    private static void checkStorage(String text) throws Exception {
        if (!Boolean.getBoolean("m3.test.requireStorage")) return;
        check(!text.isEmpty(), "storage witness must be nonempty");
        var field = String.class.getDeclaredField("m3");
        field.setAccessible(true);
        check(field.get(text) != null, "file and joined Strings have actual canonical M3 storage");
    }

    private static String sha(byte[] input) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input));
    }

    private static String utf16(CharSequence text) throws Exception {
        ByteBuffer units = ByteBuffer.allocate(text.length() * Character.BYTES);
        for (int i = 0; i < text.length(); i++) units.putChar(text.charAt(i));
        return sha(units.array());
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    @FunctionalInterface private interface Checked { void run() throws Exception; }

    private static void expect(Class<? extends Throwable> expected, Checked operation) throws Exception {
        try { operation.run(); }
        catch (Throwable failure) {
            check(expected.isInstance(failure), "expected " + expected.getSimpleName() + ", got " + failure);
            return;
        }
        throw new AssertionError("expected " + expected.getSimpleName());
    }

    private static class CountMonitor implements ProgressMonitor {
        int begun, finished;
        long worked;
        @Override public void begin(String task, long totalWork) { begun++; }
        @Override public void worked(long amount) { worked += amount; }
        @Override public void done() { finished++; }
    }
}
