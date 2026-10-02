/* Copyright 2026 Hitesh Soliwal. SPDX-License-Identifier: Apache-2.0 */
package com.m3.openrewrite;

import java.io.ByteArrayOutputStream;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import javax.tools.ToolProvider;
import static com.m3.openrewrite.TypedLiteralAdmissionLowering.Status.*;

/** Runs actual Java21 attribution, lowering, compilation and original/transformed execution. */
public final class TypedLiteralAdmissionLoweringTest {
    private TypedLiteralAdmissionLoweringTest() { }
    private static long checks;
    private static void check(boolean value) { if (!value) throw new AssertionError("check " + checks); checks++; }
    private static String literal(String value) {
        StringBuilder output = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                default -> output.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
            }
        }
        return output.append('"').toString();
    }
    private static Object run(Path classes, String source, String name) throws Exception {
        Path temp = Files.createTempDirectory("m3-lowering-test-");
        try {
            Path file = temp.resolve(name + ".java"); Files.writeString(file, source, StandardCharsets.UTF_8);
            ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
            int code = ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics, "--release", "21",
                    "-proc:none", "-Xlint:all", "-Werror", "-classpath", classes.toString(), "-d", temp.toString(), file.toString());
            if (code != 0) throw new AssertionError(diagnostics.toString(StandardCharsets.UTF_8));
            try (URLClassLoader loader = new URLClassLoader(new java.net.URL[]{temp.toUri().toURL(), classes.toUri().toURL()},
                    TypedLiteralAdmissionLoweringTest.class.getClassLoader())) {
                return loader.loadClass(name).getMethod("run").invoke(null);
            }
        } finally {
            try (var paths = Files.walk(temp)) {
                for (Path file : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(file);
            }
        }
    }
    private static String program(String expression, String extras) {
        return "import com.m3.text.M3Text; public class Negative { "
                + "static final String A=\"a\", B=\"b\"; " + extras
                + " public static Object run() { return " + expression + "; } }";
    }
    private static void unchanged(Path classes, String expression, String extra) {
        String source = program(expression, extra);
        var result = TypedLiteralAdmissionLowering.lower("Negative.java", source, classes);
        if (result.status() != UNCHANGED) throw new AssertionError(result.reason() + ": " + expression);
        check(result.after().equals(source)); check(result.changes() == 0);
    }
    public static void main(String[] args) throws Exception {
        Path classes = Path.of(args[0]);
        check(TypedLiteralAdmissionLowering.ownerClassRoot(classes).equals(TypedLiteralAdmissionLowering.OWNER_CLASS_ROOT));
        String simple = "import com.m3.text.M3Text; public class Example { "
                + "static final String A=\"A\\ud83d\", B=\"\\ude00B\"; "
                + "public static String[] run() { M3Text a=M3Text.fromString(A+B); "
                + "M3Text b=M3Text.fromString(Example.A+(Example.B+\"\")); "
                + "if(a!=b) throw new AssertionError(); String untouched=A+B; "
                + "if(untouched!=(A+B)) throw new AssertionError(); "
                + "return new String[]{a.asString(),b.asString(),untouched}; }}";
        var first = TypedLiteralAdmissionLowering.lower("Example.java", simple, classes);
        if (first.status() != CHANGED) throw new AssertionError(first.reason());
        check(first.changes() == 2);
        check(Arrays.equals((String[]) run(classes, simple, "Example"), (String[]) run(classes, first.after(), "Example")));
        check(first.after().contains("String untouched=A+B;"));
        check(first.rollback(first.after()).equals(simple));
        try { first.rollback(first.after() + "\n"); throw new AssertionError("rollback overwrote drift"); }
        catch (IllegalArgumentException expected) { checks++; }
        var fixed = TypedLiteralAdmissionLowering.lower("Example.java", first.after(), classes);
        check(fixed.status() == UNCHANGED); check(fixed.after().equals(first.after()));
        unchanged(classes, "M3Text.fromString(A + f())", "static String f(){return B;}");
        unchanged(classes, "M3Text.fromString((String)null+B)", "");
        unchanged(classes, "M3Text.fromString(A /* preserve */ +B)", "");
        unchanged(classes, "get().fromString(A+B)", "static M3Text get(){return null;}");
        unchanged(classes, "M3Text.fromString(get().A+B)", "static Negative get(){return null;}");
        unchanged(classes, "String.valueOf(A+B)", "");
        unchanged(classes, "M3Text.fromString(X+B)", "static final String X=new String(\"x\");");
        unchanged(classes, "M3Text.fromString(" + String.join("+", java.util.Collections.nCopies(65, "A")) + ")", "");
        String unresolved = program("M3Text.fromString(A+missing())", "");
        var missing = TypedLiteralAdmissionLowering.lower("Negative.java", unresolved, classes);
        check(missing.status() == REFUSED); check(missing.after().equals(unresolved));
        check(TypedLiteralAdmissionLowering.lower("../Negative.java", unresolved, classes).status() == REFUSED);
        check(TypedLiteralAdmissionLowering.lower("Negative.java", simple, classes.resolve("absent")).status() == REFUSED);
        String shadow = "public class Shadow {static final String A=\"a\",B=\"b\";"
                + "static class M3Text {static String fromString(String s){return s;}}"
                + "public static String run(){return M3Text.fromString(A+B);}}";
        check(TypedLiteralAdmissionLowering.lower("Shadow.java", shadow, classes).status() == UNCHANGED);
        String effects = "import com.m3.text.M3Text; public class Effects { static String trace=\"\";"
                + "static String a(){trace+=\"a\";return \"1\";} static String b(){trace+=\"b\";return \"2\";}"
                + "public static String run(){String s=M3Text.fromString(a()+b()).asString();return trace+s;}}";
        var effectResult = TypedLiteralAdmissionLowering.lower("Effects.java", effects, classes);
        check(effectResult.status() == UNCHANGED); check(effectResult.after().equals(effects));
        check(run(classes, effects, "Effects").equals("ab12"));
        check(run(classes, effectResult.after(), "Effects").equals("ab12"));
        // Type-checked constant variables avoid relying on javac retaining literal-folding AST nodes.
        Random random = new Random(0x4d334c4f574552L);
        String[] expected = new String[256];
        StringBuilder fields = new StringBuilder("import com.m3.text.M3Text; public class Corpus {\n");
        StringBuilder calls = new StringBuilder("public static String[] run(){return new String[]{\n");
        for (int i = 0; i < expected.length; i++) {
            char[] a = new char[random.nextInt(12)], b = new char[random.nextInt(12)];
            for (int j = 0; j < a.length; j++) a[j] = (char) random.nextInt(65536);
            for (int j = 0; j < b.length; j++) b[j] = (char) random.nextInt(65536);
            if (i == 0) { a = new char[]{'A', '\ud83d'}; b = new char[]{'\ude00', 0, '\ud800'}; }
            expected[i] = new String(a) + new String(b);
            fields.append("static final String L").append(i).append('=').append(literal(new String(a))).append(';');
            fields.append("static final String R").append(i).append('=').append(literal(new String(b))).append(';');
            if (i != 0) calls.append(',');
            calls.append("M3Text.fromString(L").append(i).append("+R").append(i).append(").asString()\n");
        }
        String corpus = fields.append(calls).append("};}}\n").toString();
        var transformed = TypedLiteralAdmissionLowering.lower("Corpus.java", corpus, classes);
        if (transformed.status() != CHANGED) throw new AssertionError(transformed.reason());
        check(transformed.changes() == 256);
        String[] original = (String[]) run(classes, corpus, "Corpus");
        String[] lowered = (String[]) run(classes, transformed.after(), "Corpus");
        for (int i = 0; i < expected.length; i++) {
            check(original[i].equals(expected[i])); check(lowered[i].equals(expected[i]));
            check(original[i].hashCode() == lowered[i].hashCode());
        }
        check(transformed.rollback(transformed.after()).equals(corpus));
        check(TypedLiteralAdmissionLowering.lower("Corpus.java", transformed.after(), classes).status() == UNCHANGED);
        System.out.println("M3_TYPED_LOWERING_PASS checks=" + checks + " compiledDifferentialCases=256 positiveSites=258");
    }
}
