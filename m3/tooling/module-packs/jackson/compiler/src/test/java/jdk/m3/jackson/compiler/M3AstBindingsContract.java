/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
package jdk.m3.jackson.compiler;

import com.sun.source.util.JavacTask;
import com.sun.source.util.M3AST;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import jdk.m3.jackson.M3File;
import jdk.m3.jackson.M3ObjectMapper;
import jdk.m3.jackson.compiler.fixture.AstRecord;

/** Uses the actual patched M3 javac session, with no substitute API implementation. */
public final class M3AstBindingsContract {
    private M3AstBindingsContract() { }
    public static void main(String[] args) throws Exception {
        String code = "package jdk.m3.jackson.compiler.fixture; "
            + "public record AstRecord(String name, int count) { "
            + "static final String DECOY = \"class Fake { int ignored; }\"; }";
        Path output = Files.createTempDirectory("m3-jackson-ast-");
        List<M3AstBindings.TypeShape> shapes = new ArrayList<>();
        List<M3AstBindings.TypeShape> shared = new ArrayList<>();
        M3AST.Summary summary;
        try {
            var compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) throw new AssertionError("real javac required");
            try (var files = compiler.getStandardFileManager(null, null, null)) {
                var input = new SimpleJavaFileObject(
                    URI.create("string:///jdk/m3/jackson/compiler/fixture/AstRecord.java"),
                    javax.tools.JavaFileObject.Kind.SOURCE) {
                    @Override public CharSequence getCharContent(boolean ignoreErrors) { return code; }
                };
                JavacTask task = (JavacTask) compiler.getTask(null, files, null,
                    List.of("-proc:none", "-d", output.toString()), null, List.of(input));
                try (M3AST session = M3AST.open(task)) {
                    session.addConsumer(new M3AstBindings(shapes::add));
                    session.addConsumer(new M3AST.Consumer() {
                        @Override public void analyzed(M3AST.Unit unit) throws Exception {
                            shared.add(unit.project(M3AstBindings.projection()));
                        }
                    });
                    if (!task.call()) throw new AssertionError("real compiler failure");
                    summary = session.summary();
                }
            }
            if (shapes.isEmpty() || shapes.size() != shared.size()) throw new AssertionError("no AST events");
            for (int i = 0; i < shapes.size(); i++) {
                if (shapes.get(i) != shared.get(i)) throw new AssertionError("projection was recomputed");
            }
            if (!summary.completed() || !summary.closed() || summary.consumerFailed()
                    || summary.compilerErrors() != 0 || summary.projectionHits() != shapes.size()
                    || summary.projectionComputations() != shapes.size()
                    || summary.capturedSources() != 1) {
                throw new AssertionError("incorrect compiler ownership counters: " + summary);
            }
            var mapper = new M3ObjectMapper();
            for (var shape : shapes) {
                String json = mapper.writeValueAsString(shape);
                equal(shape, mapper.readValue(M3File.of(json), M3AstBindings.TypeShape.class));
                if (shape.qualifiedName().contains("Fake")) throw new AssertionError("code-looking string parsed");
            }
            var shape = shapes.getFirst();
            equal(AstRecord.class.getCanonicalName(), shape.qualifiedName());
            equal(List.of("name", "count"), shape.recordComponents().stream()
                .map(M3AstBindings.Member::name).toList());
            var binding = M3AstBindings.prepare(shape, AstRecord.class, mapper);
            AstRecord item = new AstRecord("हिन्दी", 7);
            equal(item, binding.readValue(M3File.of(binding.writeValueAsString(item))));
            try {
                M3AstBindings.prepare(shape, String.class, mapper);
                throw new AssertionError("mismatched runtime type admitted");
            } catch (IllegalArgumentException expected) { }
            System.out.println("M3_AST_BINDINGS_PASS shapes=" + shapes.size()
                + " projections=" + summary.projectionComputations() + " hits=" + summary.projectionHits());
        } finally {
            try (var paths = Files.walk(output)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(expected + " != " + actual);
    }
}
