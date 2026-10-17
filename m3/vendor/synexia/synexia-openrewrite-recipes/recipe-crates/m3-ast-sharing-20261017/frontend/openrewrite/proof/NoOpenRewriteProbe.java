// SPDX-License-Identifier: Apache-2.0
import com.sun.source.util.JavacTask;
import com.synexia.mindex.ast.MIndexASTLimits;
import com.synexia.mindex.compiler.ast.M3CompilationContext;
import com.synexia.mindex.compiler.ast.M3File;
import com.synexia.mindex.compiler.ast.M3JavacASTIndex;
import java.net.URI;
import java.util.List;
import javax.tools.ToolProvider;

public final class NoOpenRewriteProbe {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("org.openrewrite.java.tree.J", false, ClassLoader.getSystemClassLoader());
            throw new AssertionError("OpenRewrite unexpectedly present");
        } catch (ClassNotFoundException expected) { }
        M3File source = M3File.of(URI.create("mem:///Plain.java"), "class Plain { int value = 3; }\n");
        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var manager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = List.of("-proc:none", "-d", args[0]);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, null, options, null, List.of(source));
            try (var index = new M3JavacASTIndex(task, M3CompilationContext.isolated(options),
                    unit -> source.source(), MIndexASTLimits.defaults(), 21)) {
                if (!task.call()) throw new AssertionError("plain compilation failed");
                if (index.statistics().parseEvents() != 1 || index.statistics().syntaxCaptures() != 0) {
                    throw new AssertionError("unexpected parser or eager syntax work");
                }
            }
        }
        System.out.println("OPTIONAL_OPENREWRITE_ABSENT_PASS parses=1 syntaxCaptures=0");
    }
}
