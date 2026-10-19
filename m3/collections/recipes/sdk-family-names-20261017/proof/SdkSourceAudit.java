/* SPDX-License-Identifier: Apache-2.0 */
import com.sun.source.tree.*;
import com.sun.source.util.JavacTask;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;

/** Parse only; deliberately not advertised as donor-resolved compilation. */
public final class SdkSourceAudit {
    private record Unit(CompilationUnitTree tree, Map<String, String> imports, Map<String, MethodTree> methods) { }
    private static Unit unit(Path path) throws Exception {
        var compiler = ToolProvider.getSystemJavaCompiler();
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            var task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("--release", "21", "-proc:none"), null, manager.getJavaFileObjects(path));
            var trees = task.parse().iterator();
            CompilationUnitTree tree = trees.next();
            if (diagnostics.getDiagnostics().stream().anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR))
                throw new AssertionError(diagnostics.getDiagnostics());
            var imports = new HashMap<String, String>();
            for (ImportTree imp : tree.getImports()) {
                String value = imp.getQualifiedIdentifier().toString();
                imports.put(value.substring(value.lastIndexOf('.') + 1), value);
            }
            var methods = new LinkedHashMap<String, MethodTree>();
            for (Tree declaration : tree.getTypeDecls()) if (declaration instanceof ClassTree cls)
                for (Tree member : cls.getMembers()) if (member instanceof MethodTree method
                        && method.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.PUBLIC)
                        && method.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.STATIC))
                    if (methods.put(key(method, new Unit(tree, imports, methods)), method) != null) throw new AssertionError("duplicate factory descriptor");
            return new Unit(tree, imports, methods);
        }
    }
    private static String resolve(Tree type, Unit unit) {
        if (type instanceof ParameterizedTypeTree generic) return resolve(generic.getType(), unit);
        String value = type.toString();
        int dot = value.indexOf('.');
        String first = dot < 0 ? value : value.substring(0, dot);
        String imported = unit.imports.get(first);
        return imported == null ? value : imported + (dot < 0 ? "" : "$" + value.substring(dot + 1).replace('.', '$'));
    }
    private static String key(MethodTree method, Unit unit) {
        return method.getName() + "(" + String.join(";", method.getParameters().stream()
                .map(p -> resolve(p.getType(), unit)).toList()) + ")";
    }
    public static void main(String[] args) throws Exception {
        var before = unit(Path.of(args[0])); var after = unit(Path.of(args[1]));
        for (var entry : before.methods.entrySet()) {
            var actual = after.methods.get(entry.getKey());
            if (actual == null || !actual.toString().equals(entry.getValue().toString())) throw new AssertionError("old factory changed: " + entry.getKey());
        }
        var rows = Files.readAllLines(Path.of(args[2])); var seen = new HashSet<String>();
        for (String line : rows.subList(1, rows.size())) {
            String[] cells = line.split("\t", -1);
            if (cells.length != 8 || !seen.add(cells[1] + "(" + cells[2] + ")")) throw new AssertionError("invalid inventory");
            var method = after.methods.get(cells[1] + "(" + cells[2] + ")");
            if (method == null || !resolve(method.getReturnType(), after).equals(cells[3])) throw new AssertionError("return name " + cells[1]);
            var params = method.getParameters().stream().map(p -> resolve(p.getType(), after)).toList();
            if (!String.join(";", params).equals(cells[2])) throw new AssertionError("parameter names " + cells[1] + ": " + params);
        }
        if (!seen.equals(after.methods.keySet())) throw new AssertionError("unmapped public factory");
        for (int i = 3; i < args.length; i++) unit(Path.of(args[i]));
        System.out.println("SdkSourceAudit originalFactories=" + before.methods.size() + " unchanged; mapped=" + seen.size() + "; parseOnly=true");
    }
}
