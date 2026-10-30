// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.mop;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Atomize pillar: typed atoms, hard limits, and javac Plugin == CLI byte identity. */
public final class M3AtomizeContract {
    private static int checks;

    private M3AtomizeContract() { }

    static void check(boolean condition, String reason) {
        checks++;
        if (!condition) throw new AssertionError(reason);
    }

    static String body(int statements) {
        return IntStream.range(0, statements).mapToObj(i -> "        total += " + i + ";\n")
                .collect(Collectors.joining());
    }

    static final String FIXTURE = "package demo;\n"
            + "import java.io.IOException;\n"
            + "import java.nio.file.Files;\n"
            + "import java.nio.file.Path;\n"
            + "public class Fixture {\n"
            + "    int a = 1, b = 2;\n"
            + "    private int state;\n"
            + "    @SuppressWarnings(\"unused\")\n"
            + "    public Fixture(int seed) { state = seed; }\n"
            + "    public int fifteen() {\n        int total = 0;\n" + body(11) + "        return total;\n    }\n"
            + "    public int sixteen() {\n        int total = 0;\n" + body(12) +"        return total;\n    }\n"
            + "    public long read(Path path) throws IOException {\n"
            + "        //FLLDCIM: MOP — anchor inside an atom\n"
            + "        if (path == null) throw new IllegalArgumentException(\"path\");\n"
            + "        return Files.size(path);\n    }\n"
            + "    protected void loop(int[] values) { for (int v : values) { this.state += v; } }\n"
            + "    int hoist() { return \"k\".length() + \"k\".length(); }\n"
            + "    public interface Api { int size(); }\n"
            + "    private static final class Hidden { public void open() { } }\n"
            + "    public record Point(int x, long y) { public Point { if (x < 0) throw new IllegalStateException(); } }\n"
            + "    public enum Mode { FAST, SLOW }\n"
            + "}\n";

    private static M3Atom atom(List<M3Atom> atoms, String name) {
        return atoms.stream().filter(a -> a.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("missing atom " + name));
    }

    private static void typedAtoms() throws Exception {
        M3Atomizer.Unit unit = M3Atomizer.parse("demo/Fixture.java", FIXTURE);
        List<M3Atom> atoms = unit.atoms();
        check(atom(atoms, "a,b").kind().equals("FIELD"), "comma field group is one atom");
        check(atom(atoms, "fifteen").limits().equals(M3Atom.NONE)
                && atom(atoms, "fifteen").limit(M3Atom.METHOD_15) == 0, "15-line body is within METHOD_15");
        check(atom(atoms, "sixteen").limit(M3Atom.METHOD_15) == 1, "16-line body violates METHOD_15");
        M3Atom read = atom(atoms, "read");
        check(read.inputs().equals("Path path") && read.output().equals("long"), "typed inputs/output");
        check(read.failure().equals("IOException|THROW"), "declared and thrown failure: " + read.failure());
        check(read.effects().contains("IO") && read.effects().contains("THROWS"), "IO and THROWS effects");
        check(read.anchors() == 1, "anchor inside atom counted");
        M3Atom loop = atom(atoms, "loop");
        check(loop.limit(M3Atom.LOOP) == 1 && loop.effects().contains("MUTATES"), "loop and member mutation");
        check(atom(atoms, "hoist").limit(M3Atom.CONST_HOIST) == 1, "repeated literal is CONST_HOIST");
        check(atom(atoms, "size").exposed(), "interface method is implicitly public");
        check(!atom(atoms, "open").exposed(), "public method of private class is not exposed");
        check(!atom(atoms, "hoist").exposed() && atom(atoms, "loop").exposed(), "package-private vs protected");
        check(atom(atoms, "Point").kind().equals("TYPE")
                && atom(atoms, "Point").contract().contains("components=int x;long y"), "record components in type contract");
        check(atom(atoms, "Mode").contract().contains("constants=FAST,SLOW"), "enum constant order in type contract");
        check(atom(atoms, "FAST").exposed(), "enum constants are exposed");
        atoms.forEach(a -> check(a.exactSha256().equals(M3MopReceipt.sha256(
                FIXTURE.substring(a.startChar(), a.endChar()))), "exact sha256 of span " + a.name()));
        check(IntStream.range(1, atoms.size()).allMatch(i -> atoms.get(i - 1).endChar() <= atoms.get(i).startChar()),
                "atoms do not overlap");
    }

    private static void parseRefusal() throws Exception {
        checks++;
        try {
            M3Atomizer.parse("demo/Bad.java", "class Bad { void m( }");
        } catch (java.io.IOException expected) {
            if (!expected.getMessage().startsWith("M3_PARSE_ERROR")) throw new AssertionError(expected);
            return;
        }
        throw new AssertionError("unparsable source was atomized");
    }

    /** Discriminator: projecting at ANALYZE would add the implicit super() CALLS to the constructor. */
    private static void pluginEqualsCli(Path work) throws Exception {
        Path root = work.resolve("src");
        Path file = root.resolve("demo/Fixture.java");
        Files.createDirectories(file.getParent());
        Files.writeString(file, FIXTURE, StandardCharsets.UTF_8);
        Path cli = work.resolve("cli");
        M3AtomizeRun.publish(M3Atomizer.parse(root, List.of(file)),
                new M3AtomizeRun.Options(cli, Optional.empty(), false, "contract"));
        Path plugin = work.resolve("plugin");
        check(compileWithPlugin(work, root, file, "out=" + plugin), "javac with -Xplugin:M3Mop succeeds");
        List.of("FILES.tsv", "ATOMS.tsv", "PATTERNS.tsv", "MANIFEST.sha256").forEach(name -> check(
                same(cli.resolve(name), plugin.resolve(name)), "plugin and CLI byte-identical " + name));
        String constructor = Files.readAllLines(plugin.resolve("ATOMS.tsv")).stream()
                .filter(line -> line.split("\t")[4].equals("<init>"))
                .filter(line -> line.split("\t")[2].equals("demo.Fixture"))
                .findFirst().orElseThrow();
        check(!constructor.split("\t")[12].contains("CALLS"), "constructor projected before Attr adds super()");
        check(Files.readString(plugin.resolve("RECEIPT.jsonl")).startsWith("{\"schema\":\"M3-MOP-RECEIPT/1\",\"goal\":\"javac-plugin\""),
                "plugin writes a JSONL receipt");
        Path again = work.resolve("cli-again");
        M3AtomizeRun.publish(M3Atomizer.parse(root, List.of(file)),
                new M3AtomizeRun.Options(again, Optional.empty(), false, "contract"));
        check(same(cli.resolve("RECEIPT.jsonl"), again.resolve("RECEIPT.jsonl")), "receipt is byte-identical on rerun");
    }

    static boolean compileWithPlugin(Path work, Path root, Path file, String arguments) throws Exception {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager files = javac.getStandardFileManager(null, Locale.ROOT, StandardCharsets.UTF_8)) {
            List<String> options = List.of("-proc:none", "--release", "21", "-d", work.resolve("classes").toString(),
                    "-cp", System.getProperty("java.class.path"), "-Xplugin:M3Mop " + arguments + " root=" + root);
            return javac.getTask(null, files, null, options, null, files.getJavaFileObjects(file.toFile())).call();
        }
    }

    static boolean same(Path left, Path right) {
        try {
            return java.util.Arrays.equals(Files.readAllBytes(left), Files.readAllBytes(right));
        } catch (java.io.IOException missing) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        Path work = Files.createTempDirectory("m3-mop-atomize");
        typedAtoms();
        parseRefusal();
        pluginEqualsCli(work);
        System.out.println("M3_MOP_ATOMIZE_CONTRACT_PASS checks=" + checks);
    }
}
