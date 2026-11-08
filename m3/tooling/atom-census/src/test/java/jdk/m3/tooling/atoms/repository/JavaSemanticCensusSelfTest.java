/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.atoms.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import jdk.m3.tooling.atoms.contract.AtomKind;

/** Dependency-free Java 21 executable proof for bounded Java/JNI-declaration parsing and committed-tree identity. */
public final class JavaSemanticCensusSelfTest {
    private static int checks;

    private JavaSemanticCensusSelfTest() {
    }

    /**
     * Runs the independent fixtures against a real temporary Git repository.
     *
     * @param args unused
     * @throws Exception on incorrect semantics or an unavailable runtime
     */
    public static void main(String[] args) throws Exception {
        verifySyntaxSpectrum();
        verifyNativeOverloads();
        verifyUnicodeAndIdentitySeparation();
        verifyBadSourceRefusal();
        verifyModules();
        verifyCommittedTreeAndFixedPoint();
        System.out.println("M3_JAVA_SEMANTIC_SELFTEST\tchecks=" + checks + "\tresult=PASS");
    }

    private static void verifySyntaxSpectrum() {
        String source = """
                package example;
                import java.util.function.Function;
                @Deprecated
                public class Spectrum<T> {
                  private int uninitialized;
                  private static int counter = 7;
                  { counter++; }
                  static { counter++; }
                  public Spectrum() { this.uninitialized = counter; }
                  public synchronized String run(String s) {
                    String decoy = "public native int fake(String value);";
                    String block = XXX
                      native void phantom();
                      XXX;
                    Function<Integer,Integer> f = x -> x+1;
                    Runnable nested = () -> { Runnable sub = () -> {}; sub.run(); };
                    Object anon = new Object() { public String toString() { return s; } };
                    class Local { void localMethod() { } }
                    return decoy + block + anon + new Local() + f.apply(1) + nested;
                  }
                  public static native long call(byte[] bytes, long off);
                  public static native long call(String text, long off);
                  abstract static class Abstract { abstract int read(); }
                  interface Worker { void work(); }
                }
                """.replace("XXX", "\"\"\"");
        // Text-block decoy content is intentionally not Java syntax in the resulting AST.
        JavaSemanticExtractor.ParseResult result = JavaSemanticExtractor.extract("example/Spectrum.java",
                source.getBytes(StandardCharsets.UTF_8));
        check(result.complete(), "valid Java AST parse, errors=" + result.status());
        Map<AtomKind, Integer> kinds = count(result.atoms());
        check(kinds.getOrDefault(AtomKind.JAVA_CONSTRUCTOR, 0) == 1, "constructor exactly once");
        check(kinds.getOrDefault(AtomKind.JAVA_STATIC_INITIALIZER, 0) == 1, "static init once");
        check(kinds.getOrDefault(AtomKind.JAVA_INSTANCE_INITIALIZER, 0) == 1, "instance init once");
        check(kinds.getOrDefault(AtomKind.JAVA_NATIVE_DECLARATION, 0) == 2, "exact two native methods");
        check(kinds.getOrDefault(AtomKind.JAVA_LAMBDA, 0) == 3, "nested and top-level lambdas");
        check(kinds.getOrDefault(AtomKind.JAVA_FIELD_INITIALIZER, 0) == 1, "field initializer");
        check(result.atoms().stream().anyMatch(a -> a.signature().contains("method:localMethod")),
                "nested local class method not lost");
        check(result.atoms().stream().anyMatch(a -> a.owner().contains("anonymous#")),
                "anonymous type owner retained");
        check(result.atoms().stream().noneMatch(a -> a.signature().contains("fake")
                || a.signature().contains("phantom")), "source text decoys must not create methods");
        check(result.atoms().stream().map(JavaSemanticExtractor.Atom::atomId).distinct().count()
                == result.atoms().size(), "unique atom identity");
        check(result.atoms().stream().allMatch(a -> a.endOffset() > a.startOffset()
                && a.sourceSha256().matches("[0-9a-f]{64}")), "sealed positive source slices");
        check(result.atoms().equals(JavaSemanticExtractor.extract("example/Spectrum.java",
                source.getBytes(StandardCharsets.UTF_8)).atoms()), "byte-identical extraction fixed point");
    }

    private static void verifyNativeOverloads() {
        String java = """
                package p; public class N {
                  public native void f(int x);
                  public native void f(long x);
                  public native int g(int[] x);
                  public void h() { String x = "native int pretend();"; }
                }
                """;
        var parsed = JavaSemanticExtractor.extract("p/N.java", java.getBytes(StandardCharsets.UTF_8));
        check(parsed.complete(), "Java native declarations parse without a JNI library");
        List<JavaSemanticExtractor.Atom> nativeAtoms = parsed.atoms().stream()
                .filter(a -> a.kind() == AtomKind.JAVA_NATIVE_DECLARATION).toList();
        check(nativeAtoms.size() == 3, "three actual native method declarations");
        check(nativeAtoms.stream().map(JavaSemanticExtractor.Atom::atomId).distinct().count() == 3,
                "overloads have distinct identities");
        check(nativeAtoms.stream().noneMatch(a -> a.signature().contains("pretend")),
                "quoted native fake not a declaration");
    }

    private static void verifyUnicodeAndIdentitySeparation() {
        String before = "package p; class Unicode { String greet() { return \"नमस्ते\"; } }\n";
        String after = "package p; class Unicode { String greet() { return \"नमस्कार\"; } }\n";
        var first = JavaSemanticExtractor.extract("p/Unicode.java",
                before.getBytes(StandardCharsets.UTF_8));
        var second = JavaSemanticExtractor.extract("p/Unicode.java",
                after.getBytes(StandardCharsets.UTF_8));
        var moved = JavaSemanticExtractor.extract("moved/Unicode.java",
                before.getBytes(StandardCharsets.UTF_8));
        check(first.complete() && second.complete() && moved.complete(),
                "Unicode source and moved path parse");
        check(JavaSemanticExtractor.extract("p/Unicode.JAVA",
                before.getBytes(StandardCharsets.UTF_8)).complete(),
                "source-family case normalization preserves Java file classification");
        var oldMethod = first.atoms().stream().filter(a -> a.kind() == AtomKind.JAVA_METHOD)
                .findFirst().orElseThrow();
        var newMethod = second.atoms().stream().filter(a -> a.kind() == AtomKind.JAVA_METHOD)
                .findFirst().orElseThrow();
        var movedMethod = moved.atoms().stream().filter(a -> a.kind() == AtomKind.JAVA_METHOD)
                .findFirst().orElseThrow();
        check(oldMethod.atomId().equals(newMethod.atomId()),
                "signature identity survives internal implementation edits");
        check(!oldMethod.sourceSha256().equals(newMethod.sourceSha256()),
                "implementation byte changes invalidate source-slice evidence");
        check(!oldMethod.atomId().equals(movedMethod.atomId()),
                "a move requires separately reviewed stable identity override");
        String crlf = before.replace("\n", "\r\n");
        var windows = JavaSemanticExtractor.extract("p/Unicode.java",
                crlf.getBytes(StandardCharsets.UTF_8));
        check(windows.complete() && windows.atoms().stream().anyMatch(
                atom -> atom.kind() == AtomKind.JAVA_METHOD), "CRLF source is parsed without normalization");
    }

    private static void verifyBadSourceRefusal() {
        var invalid = JavaSemanticExtractor.extract("p/Broken.java",
                "public class Broken { void nope( { }".getBytes(StandardCharsets.UTF_8));
        check(!invalid.complete() && invalid.atoms().isEmpty(), "invalid grammar explicitly refused");
        var invalidUtf8 = JavaSemanticExtractor.extract("p/Bad.java", new byte[]{(byte) 0xc3, (byte) 0x28});
        check(!invalidUtf8.complete() && "INVALID_UTF8".equals(invalidUtf8.status()),
                "malformed UTF-8 explicitly refused");
        try {
            JavaSemanticExtractor.extract("../escape.java", new byte[0]);
            throw new AssertionError("traversal accepted");
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }

    private static void verifyModules() {
        var module = JavaSemanticExtractor.extract("src/m/module-info.java",
                "module m { requires java.logging; exports p; }".getBytes(StandardCharsets.UTF_8));
        check(module.complete(), "module descriptor parses");
        check(module.atoms().stream().anyMatch(a -> a.signature().contains("module:m")), "module atom");
        check(module.atoms().stream().filter(a -> a.signature().contains("module-directive")).count() == 2,
                "module directives individually represented");
        var record = JavaSemanticExtractor.extract("p/P.java", """
                package p; record P(String s, int x) {
                  P { if (x < 0) throw new IllegalArgumentException(); }
                  int value() { return x; }
                }
                """.getBytes(StandardCharsets.UTF_8));
        check(record.complete(), "record compact constructor parses");
        check(record.atoms().stream().anyMatch(a -> a.kind() == AtomKind.JAVA_CONSTRUCTOR),
                "record compact constructor retained");
        check(record.atoms().stream().filter(a -> a.signature().contains("::field:")).count() == 2,
                "record components become explicit declaration atoms");
    }

    private static void verifyCommittedTreeAndFixedPoint() throws Exception {
        Path root = Files.createTempDirectory("m3-java-atom-git-");
        try {
            git(root, "init", "--quiet", "--initial-branch=main");
            Path src = root.resolve("src/p/X.java");
            Files.createDirectories(src.getParent());
            Files.writeString(src, "package p; class X { native void n(int x); void m() {} }\n");
            Files.writeString(root.resolve("src/p/NotJava.txt"), "class Phantom { void notReal() {} }\n");
            git(root, "add", "--", ".");
            git(root, "-c", "user.name=M3", "-c", "user.email=m3@example.invalid", "commit", "-qm", "fixture");
            Path left = Files.createDirectory(root.resolve("left"));
            Path right = Files.createDirectory(root.resolve("right"));
            var a = JavaSemanticCensus.generate(root, "HEAD", left);
            git(root, "rev-parse", "HEAD");
            var b = JavaSemanticCensus.generate(root, a.commitId(), right);
            check(a.javaFiles() == 1 && a.parsedFiles() == 1 && a.blockedFiles() == 0,
                    "one exact committed Java file parsed");
            check(a.nativeDeclarations() == 1 && a.atoms() >= 3, "Java JNI declaration inventoried");
            check(a.treeId().equals(b.treeId()) && a.commitId().equals(b.commitId()),
                    "revision alias resolves to immutable identity");
            for (String name : List.of("JAVA_ATOMS.tsv", "JAVA_EVIDENCE.tsv",
                    "JAVA_PARSE_STATUS.tsv", "JAVA_SEMANTIC_RECEIPT.tsv")) {
                check(Files.mismatch(left.resolve(name), right.resolve(name)) == -1,
                        "four exact fixed-point output bytes " + name);
            }
            String atoms = Files.readString(a.atomsPath());
            String evidence = Files.readString(a.evidencePath());
            check(atoms.contains("JAVA_NATIVE_DECLARATION")
                    && evidence.lines().filter(row -> row.contains("\tJIT_TESTED\t"))
                            .allMatch(row -> row.contains("\tNOT_RUN\t")),
                    "JIT evidence is present only as unexecuted where required");
            check(evidence.contains("\tABI_VERIFIED\tNOT_RUN\t-\t-"), "native ABI is pending");
            check(evidence.contains("\tINVENTORIED\tPASS\t")
                    && evidence.contains("\tSOURCE_IDENTITY\tPASS\t"), "limited identity evidence is PASS");
            check(evidence.contains("\tCORRECTNESS_TESTED\tNOT_RUN\t-\t-"),
                    "correctness unexecuted, no false success");
            String receipt = Files.readString(a.receiptPath());
            check(receipt.contains("admission\tHOLD") && receipt.contains("java_complete\ttrue"),
                    "parse-only completeness cannot be admission");
            // The working tree can diverge after the commit. The readback must remain identical.
            Files.writeString(src, "SILENT WORKING TREE DRIFT SHOULD NEVER ENTER THE COMMITTED CENSUS");
            Path third = Files.createDirectory(root.resolve("third"));
            var c = JavaSemanticCensus.generate(root, "HEAD", third);
            check(c.atoms() == a.atoms()
                    && Files.mismatch(a.atomsPath(), c.atomsPath()) == -1,
                    "working tree drift cannot enter exact Git source census");
            // A later committed invalid Java file must be visible as a blocked source, not omitted silently.
            Files.writeString(src, "package p; class X { void invalid( { }\n");
            git(root, "add", "--", "src/p/X.java");
            git(root, "-c", "user.name=M3", "-c", "user.email=m3@example.invalid", "commit", "-qm", "broken");
            Path fourth = Files.createDirectory(root.resolve("fourth"));
            var d = JavaSemanticCensus.generate(root, "HEAD", fourth);
            check(d.blockedFiles() == 1 && d.atoms() == 0, "invalid committed Java recorded not invented");
            check(Files.readString(d.statusPath()).contains("\tBLOCKED\t0\t0\tJAVAC_PARSE_ERROR"),
                    "parse errors retain explicit blocker state");
            check(Files.readString(d.receiptPath()).contains("java_complete\tfalse"),
                    "parse failure blocks Java-completeness claim");
        } finally {
            // The fixture owns this private temporary tree. No product/workspace files are removed.
            removeFixture(root);
        }
    }

    private static Map<AtomKind, Integer> count(List<JavaSemanticExtractor.Atom> atoms) {
        Map<AtomKind, Integer> counts = new EnumMap<>(AtomKind.class);
        atoms.forEach(a -> counts.merge(a.kind(), 1, Integer::sum));
        return counts;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static void git(Path root, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(root.toString());
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = process.waitFor();
        if (exit != 0) {
            throw new IOException("Git fixture failed: " + String.join(" ", command) + " / " + output);
        }
    }

    private static void removeFixture(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            for (Path p : stream.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
