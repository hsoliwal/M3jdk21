// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.Trees;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/** Parse-only source coordinates used by sealed recipes, not a replacement repository inventory. */
final class JavaSourceUnit {
    final String source;
    final CompilationUnitTree unit;
    final SourcePositions positions;
    private JavaSourceUnit(String source, CompilationUnitTree unit, SourcePositions positions) {
        this.source=source; this.unit=unit; this.positions=positions;
    }
    static JavaSourceUnit parse(String source) {
        if (SealHash.utf8(source).length > SealedSources.MAX_FILE_BYTES) throw new IllegalArgumentException("JAVA_SOURCE_BUDGET");
        var compiler=ToolProvider.getSystemJavaCompiler();
        if (compiler==null) throw new IllegalStateException("FULL_JDK_REQUIRED");
        var diagnostics=new DiagnosticCollector<JavaFileObject>();
        try (var files=compiler.getStandardFileManager(diagnostics,Locale.ROOT,StandardCharsets.UTF_8)) {
            var task=(JavacTask)compiler.getTask(new StringWriter(),files,diagnostics,
                    List.of("--release","21","-proc:none"),null,List.of(new Source(source)));
            var parsed=task.parse().iterator();
            if (!parsed.hasNext()) throw new IllegalArgumentException("JAVA_PARSE_EMPTY");
            var unit=parsed.next();
            if (parsed.hasNext() || diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind()==Diagnostic.Kind.ERROR)) {
                throw new IllegalArgumentException("JAVA_PARSE_ERRORS");
            }
            return new JavaSourceUnit(source,unit,Trees.instance(task).getSourcePositions());
        } catch (IOException failure) { throw new IllegalStateException("JAVA_PARSE_IO",failure); }
    }
    int start(Tree tree) { return offset(positions.getStartPosition(unit,tree)); }
    int end(Tree tree) { return offset(positions.getEndPosition(unit,tree)); }
    String slice(Tree tree) { int start=start(tree),end=end(tree); if (end<start) throw new IllegalArgumentException("JAVA_SPAN"); return source.substring(start,end); }
    private int offset(long position) {
        if (position<0 || position>source.length()) throw new IllegalArgumentException("JAVA_SPAN");
        return (int)position;
    }
    private static final class Source extends SimpleJavaFileObject {
        private final String value;
        Source(String value) { super(URI.create("string:///SealedSource.java"),Kind.SOURCE);this.value=value; }
        @Override public CharSequence getCharContent(boolean ignore) { return value; }
    }
}
