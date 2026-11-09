/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * GPLv2 with the Classpath exception.
 */

/*
 * @test
 * @summary Establish exact javac constant signals without executing application code
 * @modules jdk.compiler
 * @run main M3ComputeConstantFactsTest
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class M3ComputeConstantFactsTest {
    private static int checks;
    private static final String SOURCE = """
        import java.util.regex.Pattern;
        class ConstantFixture {
            static final String PREFIX = "^";
            static final String REGEX = PREFIX + "[A-Z]{2}[0-9]{3}$";
            static final int FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            static final String LITERAL = "ab" + "cd";
            static final int MASK = (1 << 3) | (1 << 6);
            static final String CODE_LOOKING = "Pattern.compile(\\\"[\\\");";
            static final String REPLACEMENT = "$1";
            static final String NOT_CONSTANT = new String("abcd");
            static final int[] MUTABLE_TABLE = {1, 2, 3};
            static final String ENVIRONMENT = System.getProperty("m3compute.test.forbidden");
            static final String USER_CALL = doNotExecute();
            static { doNotExecute(); }
            static String doNotExecute() { throw new AssertionError("must not execute application code"); }
        }
        """;

    private static void check(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }

    private static Map<String, Object> extract() throws Exception {
        JavaCompiler compiler = Objects.requireNonNull(ToolProvider.getSystemJavaCompiler(), "JDK required");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaFileObject source = new SimpleJavaFileObject(
                URI.create("string:///ConstantFixture.java"), JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) { return SOURCE; }
        };
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, List.of(source));
            Iterable<? extends CompilationUnitTree> units = task.parse();
            task.analyze();
            check(diagnostics.getDiagnostics().stream().noneMatch(d -> d.getKind() == Diagnostic.Kind.ERROR),
                    "fixture attribution: " + diagnostics.getDiagnostics());
            Trees trees = Trees.instance(task);
            Map<String, Object> facts = new LinkedHashMap<>();
            new TreePathScanner<Void, Void>() {
                @Override public Void visitVariable(VariableTree node, Void unused) {
                    Element element = trees.getElement(getCurrentPath());
                    if (element instanceof VariableElement variable) {
                        // javac, not source-regex matching or executing field initializers, is authority.
                        facts.put(variable.getSimpleName().toString(), variable.getConstantValue());
                    }
                    return super.visitVariable(node, unused);
                }
            }.scan(units, null);
            return facts;
        }
    }

    public static void main(String[] args) throws Exception {
        Map<String, Object> facts = extract();
        check(facts.equals(extract()), "deterministic extraction");
        check("^[A-Z]{2}[0-9]{3}$".equals(facts.get("REGEX")), "constant regex composition");
        check(Integer.valueOf(66).equals(facts.get("FLAGS")), "compiler-resolved regex flags");
        check("abcd".equals(facts.get("LITERAL")), "constant folding");
        check(Integer.valueOf(72).equals(facts.get("MASK")), "bit-mask folding");
        check("Pattern.compile(\"[\");".equals(facts.get("CODE_LOOKING")), "source-looking data stays opaque");
        check("$1".equals(facts.get("REPLACEMENT")), "replacement remains data, not expanded early");
        for (String name : List.of("NOT_CONSTANT", "MUTABLE_TABLE", "ENVIRONMENT", "USER_CALL")) {
            check(facts.containsKey(name) && facts.get(name) == null, "not compile-time constant: " + name);
        }
        // Demonstrate the admitted fixed-input half. This is an oracle, not a runtime cache/image loader.
        String regex = (String) facts.get("REGEX");
        int flags = (Integer) facts.get("FLAGS");
        Pattern prepared = Pattern.compile(regex, flags);
        for (String input : List.of("AB123", "ab123", "AB12", "ABC123", "\u0000AB123", "AB123\n")) {
            check(prepared.matcher(input).matches() == Pattern.compile(regex, flags).matcher(input).matches(),
                    "prepared-pattern parity");
        }
        check(!Pattern.compile(regex, 0).matcher("ab123").matches(), "flags participate in identity");
        System.out.println("M3_COMPUTE_CONSTANT_FACTS_PASS checks=" + checks
                + " constants=" + facts.values().stream().filter(Objects::nonNull).count()
                + " nonconstants=" + facts.values().stream().filter(Objects::isNull).count());
    }
}
