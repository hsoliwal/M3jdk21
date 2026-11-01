/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors
 */

import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/**
 * Target-native, compiler-parsed structural guard for the existing replacement routes.
 * This is deliberately not type attribution, runtime equivalence, or a jtreg substitute.
 * Comments, literals, other overloads and nested classes cannot stand in for these methods.
 */
public final class M3StringReplacementRoutes {
    private M3StringReplacementRoutes() {}

    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("expected String.java path");
            verify(Path.of(args[0]));
            System.out.println("M3_STRING_REPLACEMENT_ROUTES_OK");
        } catch (IOException | RuntimeException failure) {
            System.err.println("M3_STRING_REPLACEMENT_ROUTES_FAIL: " + failure.getMessage());
            System.exit(1);
        }
    }

    static void verify(Path source) throws IOException {
        if (!Files.isRegularFile(source) || Files.size(source) > 8L * 1024 * 1024) {
            throw new IllegalArgumentException("missing or oversized String source");
        }
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler unavailable");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT,
                                                         StandardCharsets.UTF_8)) {
            var inputs = files.getJavaFileObjectsFromPaths(List.of(source));
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, inputs);
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            if (units.size() != 1 || diagnostics.getDiagnostics().stream()
                    .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalArgumentException("Java syntax refused");
            }
            CompilationUnitTree unit = units.getFirst();
            require("java.lang".equals(String.valueOf(unit.getPackageName())), "wrong package");
            List<ClassTree> owners = unit.getTypeDecls().stream()
                    .filter(t -> t instanceof ClassTree c && c.getSimpleName().contentEquals("String"))
                    .map(t -> (ClassTree) t).toList();
            require(owners.size() == 1, "java.lang.String owner missing or duplicated");
            ClassTree owner = owners.getFirst();
            literal(method(owner, "replace", List.of("CharSequence", "CharSequence"), Modifier.PUBLIC));
            helper(method(owner, "m3Replace", List.of("M3String", "String", "M3String", "String", "M3String"), Modifier.PRIVATE));
            regex(method(owner, "replaceFirst", List.of("String", "String"), Modifier.PUBLIC), true);
            regex(method(owner, "replaceAll", List.of("String", "String"), Modifier.PUBLIC), false);
        }
    }

    private static MethodTree method(ClassTree owner, String name, List<String> types,
                                     Modifier visibility) {
        List<MethodTree> matches = owner.getMembers().stream()
                .filter(t -> t instanceof MethodTree m && m.getName().contentEquals(name)
                        && m.getParameters().stream().map(p -> p.getType().toString()).toList().equals(types))
                .map(t -> (MethodTree) t).toList();
        require(matches.size() == 1, "missing or duplicate method: " + name + types);
        MethodTree method = matches.getFirst();
        require(method.getBody() != null && method.getModifiers().getFlags().contains(visibility)
                && !method.getModifiers().getFlags().contains(Modifier.STATIC)
                && "String".equals(String.valueOf(method.getReturnType())), "method contract: " + name);
        return method;
    }

    private static void literal(MethodTree method) {
        var statements = method.getBody().getStatements();
        require(statements.size() == 6, "literal entry statement geometry");
        expect(statements.get(0), "String trgtStr = target.toString();", "target conversion");
        expect(statements.get(1), "String replStr = replacement.toString();", "replacement conversion");
        expect(statements.get(2), "int thisLen = length();", "source length");
        expect(statements.get(3), "int trgtLen = trgtStr.length();", "target length");
        expect(statements.get(4), "int replLen = replStr.length();", "replacement length");
        IfTree split = branch(statements.get(5), "(trgtLen > 0)", "nonempty target split");
        var nonempty = block(split.getThenStatement(), "nonempty target");
        require(nonempty.size() == 11, "nonempty statement geometry");
        expect(nonempty.get(0), "if (trgtLen == 1 && replLen == 1) { return replace(trgtStr.charAt(0), replStr.charAt(0)); }", "unit replacement");
        expect(nonempty.get(1), "M3String storage = m3();", "source owner");
        expect(nonempty.get(2), "M3String targetM3 = trgtStr.m3();", "target owner");
        expect(nonempty.get(3), "M3String replacementM3 = replStr.m3();", "replacement owner");
        IfTree dispatch = branch(nonempty.get(4), "(storage != null || targetM3 != null || replacementM3 != null)", "mixed admission");
        var dispatchBody = block(dispatch.getThenStatement(), "public dispatch");
        require(dispatchBody.size() == 2 && dispatch.getElseStatement() == null, "public dispatch geometry");
        expect(dispatchBody.get(0), "String replaced = m3Replace(storage, trgtStr, targetM3, replStr, replacementM3);", "public helper dispatch");
        expect(dispatchBody.get(1), "if (replaced != null) return replaced;", "literal refusal fallback");
        expect(nonempty.get(10), "return this;", "unmatched flat identity");

        var empty = block(split.getElseStatement(), "empty target");
        require(empty.size() == 9, "empty target statement geometry");
        IfTree admission = branch(empty.get(4), "(storage != null || replacementM3 != null)", "empty target admission");
        var admitted = block(admission.getThenStatement(), "empty owner admission");
        require(admitted.size() == 3, "empty admission statement geometry");
        expect(admitted.get(0), "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);", "empty source owner");
        expect(admitted.get(1), "if (replacementM3 == null) replacementM3 = M3String.canonicalize(replStr);", "empty replacement owner");
        expect(admitted.get(2), "if (sourceM3 != null && replacementM3 != null) { return new String(sourceM3.replaceEmptyTarget(replacementM3)); }", "empty canonical dispatch");
        expect(empty.get(8), "return sb.toString();", "empty compatibility fallback");
    }

    private static void helper(MethodTree method) {
        var statements = method.getBody().getStatements();
        String[] expected = {
            "int found = indexOf(trgtStr);",
            "if (found < 0) return this;",
            "M3String sourceM3 = storage != null ? storage : M3String.canonicalize(this);",
            "if (sourceM3 == null) return null;",
            "M3String replacement = replacementM3 != null ? replacementM3 : M3String.canonicalize(replStr);",
            "if (replacement == null) return null;",
            "return new String(targetM3 != null ? sourceM3.replaceAt(targetM3, replacement, found) : sourceM3.replaceFlatTarget(this, trgtStr, replacement, found));"
        };
        require(statements.size() == expected.length, "helper statement geometry");
        for (int index = 0; index < expected.length; index++) {
            expect(statements.get(index), expected[index], "helper route " + index);
        }
    }

    private static void regex(MethodTree method, boolean firstOnly) {
        var statements = method.getBody().getStatements();
        require(statements.size() == 3, "regex entry geometry: " + method.getName());
        expect(statements.get(0), "M3String storage = m3();", "regex source owner");
        IfTree guard = branch(statements.get(1), "(storage != null && M3String.isLiteralRegexReplacement(regex, replacement))", "regex subset guard");
        var body = block(guard.getThenStatement(), "regex admitted route");
        require(body.size() == 2 && guard.getElseStatement() == null, "regex guard geometry");
        expect(body.get(0), "String replaced = storage.replaceLiteralRegex(this, regex, replacement, " + firstOnly + ");", "regex literal route");
        expect(body.get(1), "if (replaced != null) return replaced;", "regex refusal fallback");
        expect(statements.get(2), "return Pattern.compile(regex).matcher(this)." + method.getName() + "(replacement);", "stock regex fallback");
    }

    private static IfTree branch(StatementTree statement, String condition, String label) {
        require(statement instanceof IfTree, label + " is not an if statement");
        IfTree branch = (IfTree) statement;
        expect(branch.getCondition(), condition, label);
        return branch;
    }

    private static List<? extends StatementTree> block(StatementTree statement, String label) {
        require(statement instanceof BlockTree, label + " is not a block");
        return ((BlockTree) statement).getStatements();
    }

    private static void expect(Tree actual, String expected, String label) {
        // Compare compiler-rendered trees, never raw Java substrings. No expected fragment
        // contains literals; a string containing code therefore cannot satisfy a route.
        String wanted = compact(expected);
        if (actual != null && actual.getKind() == Tree.Kind.VARIABLE && wanted.endsWith(";")) {
            wanted = wanted.substring(0, wanted.length() - 1);
        }
        require(actual != null && compact(actual.toString()).equals(wanted), label);
    }

    private static String compact(String source) {
        return source.replaceAll("\\s+", "");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
