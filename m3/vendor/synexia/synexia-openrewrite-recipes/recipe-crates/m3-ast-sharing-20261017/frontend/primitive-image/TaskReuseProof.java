// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.m3.M3ASTPC;
import com.sun.tools.javac.m3.M3PrimitiveImageCompiler;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.compiler.ast.M3AST;
import com.synexia.mindex.compiler.ast.M3CompilationContext;
import com.synexia.mindex.compiler.ast.M3JavacASTIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Actual primitive-image consumer and AST consumers share its one compiler task. */
public final class TaskReuseProof {
    private TaskReuseProof() { }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]);
        int checks = 0;
        for (boolean fold : new boolean[]{false, true}) {
            String name = fold ? "fold" : "legacy";
            List<String> command = new ArrayList<>(List.of("--out", root.resolve(name + ".m3prim").toString(),
                    "--slots", root.resolve(name + ".tsv").toString()));
            if (fold) command.add("--fold-expressions");
            command.add(root.resolve("Constants.java").toString());
            Map<CompilationUnitTree, String> source = new IdentityHashMap<>();
            M3JavacASTIndex[] index = new M3JavacASTIndex[1];
            AutoCloseable[] observer = new AutoCloseable[1];
            M3AST[] first = new M3AST[1];
            int[] attached = {0};
            try {
                M3PrimitiveImageCompiler.run(command.toArray(String[]::new), task -> {
                    attached[0]++;
                    observer[0] = M3ASTPC.observe(task, (unit, text) -> source.put(unit, text.toString()));
                    index[0] = new M3JavacASTIndex(task, M3CompilationContext.isolated(List.of()),
                            source::get, MIndexASTLimits.defaults(), 21);
                    index[0].on(M3JavacASTIndex.Phase.PARSE, "first", view -> {
                        try { first[0] = view.ast(); }
                        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    });
                    index[0].on(M3JavacASTIndex.Phase.PARSE, "second", view -> {
                        try { require(first[0] == view.ast(), "consumers received different syntax projections"); }
                        catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
                    });
                });
                require(attached[0] == 1, "more than one compiler attachment");
                require(index[0].statistics().parseEvents() == 1, "more than one source parse");
                require(index[0].statistics().analyzeEvents() == 1, "unexpected attribution events");
                require(index[0].statistics().syntaxCaptures() == 1, "syntax was not lazy/shared");
                require(index[0].statistics().consumerCalls() == 2, "consumer call count");
                require(Files.readString(root.resolve("Constants.java")).equals(first[0].source()), "source changed");
                require(Arrays.equals(Files.readAllBytes(root.resolve("baseline-" + name + ".m3prim")),
                        Files.readAllBytes(root.resolve(name + ".m3prim"))), "primitive image changed");
                require(Files.readString(root.resolve("baseline-" + name + ".tsv"))
                        .equals(Files.readString(root.resolve(name + ".tsv"))), "primitive sidecar changed");
                checks += 9;
            } finally {
                if (index[0] != null) index[0].close();
                if (observer[0] != null) observer[0].close();
                source.clear();
            }
        }
        System.out.println("M3_PRIMITIVE_SHARED_TASK checks=" + checks
                + " modes=legacy,fold tasksPerMode=1 parsePerMode=1 syntaxPerMode=1 imageAndSidecarEqual=true");
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
