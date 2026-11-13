// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Target-specific source gate; compilation, VM layout and jtreg remain separate gates. */
final class M3StringLayout {
    private static final int MAX_BYTES = 4 * 1024 * 1024;

    private M3StringLayout() { }

    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("expected M3String.java path");
            byte[] bytes;
            try (var input = Files.newInputStream(Path.of(args[0]))) {
                bytes = input.readNBytes(MAX_BYTES + 1);
            }
            if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("source byte budget");
            String source = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
            check(source);
            System.out.println("M3_STRING_LAYOUT_OK");
        } catch (IOException | IllegalArgumentException | IllegalStateException failure) {
            System.err.println("M3_STRING_LAYOUT_FAIL: " + failure.getMessage());
            System.exit(1);
        }
    }

    static void check(String source) throws IOException {
        var compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler unavailable");
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var input = new SimpleJavaFileObject(URI.create("string:///M3String.java"),
                JavaFileObject.Kind.SOURCE) {
            @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        // Parse only: never load processors, resolve target dependencies or execute target code.
        try (var files = compiler.getStandardFileManager(diagnostics, Locale.ROOT, StandardCharsets.UTF_8)) {
            var task = (JavacTask) compiler.getTask(null, files, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, List.of(input));
            List<CompilationUnitTree> units = new ArrayList<>();
            task.parse().forEach(units::add);
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR)) {
                throw new IllegalStateException("invalid Java 21 syntax");
            }
            if (units.size() != 1 || units.getFirst().getPackageName() == null
                    || !units.getFirst().getPackageName().toString().equals("java.lang")) {
                throw new IllegalStateException("expected java.lang compilation unit");
            }
            var matches = units.getFirst().getTypeDecls().stream()
                    .filter(t -> t instanceof ClassTree c && c.getSimpleName().contentEquals("M3String"))
                    .map(t -> (ClassTree) t).toList();
            if (matches.size() != 1) throw new IllegalStateException("expected one top-level M3String");
            ClassTree type = matches.getFirst();
            if (type.getKind() != Tree.Kind.CLASS
                    || !type.getModifiers().getFlags().equals(Set.of(Modifier.FINAL))
                    || type.getExtendsClause() != null || !type.getTypeParameters().isEmpty()) {
                throw new IllegalStateException("M3String class shape changed");
            }
            List<String> fields = new ArrayList<>();
            // Direct declarations only. Nested/local/anonymous class fields have other owners.
            for (Tree member : type.getMembers()) {
                if (!(member instanceof VariableTree field)) continue;
                Set<Modifier> flags = field.getModifiers().getFlags();
                if (flags.contains(Modifier.STATIC)) continue;
                if (!flags.equals(Set.of(Modifier.PRIVATE, Modifier.FINAL)) || field.getInitializer() != null) {
                    throw new IllegalStateException("instance field modifiers/initializer changed: " + field.getName());
                }
                fields.add(field.getType() + " " + field.getName());
            }
            if (!fields.equals(List.of("M3StringOwner owner", "long value"))) {
                throw new IllegalStateException("M3String instance fields changed: " + fields);
            }
        }
    }
}
