// SPDX-License-Identifier: Apache-2.0
package com.m3.migration;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

/** Syntax/declaration inventory of every supplied Java/resource file, not an attribution engine. */
public final class SourceInventory {
  private SourceInventory() {}
  public static void main(String[] args) throws Exception {
    if (args.length != 2) throw new IllegalArgumentException("usage: <source-root> <new-report.json>");
    Path source = Path.of(args[0]).toRealPath(), output = Path.of(args[1]).toAbsolutePath().normalize();
    if (output.startsWith(source)) throw new IllegalArgumentException("report must be outside inventory root");
    Files.writeString(output, Json.write(scan(source)) + "\n", StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
  }
  static Map<String, Object> scan(Path root) throws Exception {
    var compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) throw new IllegalArgumentException("full JDK 21 required");
    List<Object> result = new ArrayList<>();
    try (var walk = Files.walk(root)) {
      for (Path path : walk.sorted().toList()) {
        Path relative = root.relativize(path);
        boolean ignored = false;
        for (Path component : relative) if (component.toString().equals(".git")) ignored = true;
        if (ignored) continue;
        if (Files.isSymbolicLink(path)) throw new IllegalArgumentException("symlink input requires explicit policy: " + relative);
        if (Files.isDirectory(path)) continue;
        if (Files.size(path) > 16L * 1024 * 1024) throw new IllegalArgumentException("file exceeds inventory budget: " + relative);
        List<Object> declarations = new ArrayList<>();
        List<String> imports = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        if (path.toString().endsWith(".java")) {
          var collector = new DiagnosticCollector<JavaFileObject>();
          try (var manager = compiler.getStandardFileManager(collector, java.util.Locale.ROOT, StandardCharsets.UTF_8)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, collector,
                List.of("--release", "21", "-proc:none"), null, manager.getJavaFileObjects(path));
            for (CompilationUnitTree unit : task.parse()) {
              String pkg = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
              unit.getImports().forEach(value -> imports.add(value.toString().strip()));
              new TreeScanner<Void, String>() {
                @Override public Void visitClass(ClassTree tree, String owner) {
                  String name = tree.getSimpleName().toString();
                  if (name.isEmpty()) return null; // Anonymous implementation bodies are not public declarations.
                  String symbol = owner.isEmpty() ? name : owner + "." + name;
                  declarations.add(Map.of("kind", tree.getKind().name(), "symbol", symbol,
                      "modifiers", tree.getModifiers().toString().strip()));
                  for (var member : tree.getMembers()) scan(member, symbol);
                  return null;
                }
                @Override public Void visitMethod(MethodTree tree, String owner) {
                  declarations.add(Map.of("kind", "METHOD", "owner", owner, "name", tree.getName().toString(),
                      "modifiers", tree.getModifiers().toString().strip(), "return_type", String.valueOf(tree.getReturnType()),
                      "parameters", tree.getParameters().stream().map(Object::toString).toList(),
                      "throws", tree.getThrows().stream().map(Object::toString).toList()));
                  return null;
                }
                @Override public Void visitVariable(VariableTree tree, String owner) {
                  declarations.add(Map.of("kind", "FIELD", "owner", owner, "name", tree.getName().toString(),
                      "type", String.valueOf(tree.getType()), "modifiers", tree.getModifiers().toString().strip()));
                  return null;
                }
              }.scan(unit, pkg);
            }
            for (Diagnostic<?> diagnostic : collector.getDiagnostics())
              diagnostics.add(diagnostic.getKind() + ":" + diagnostic.getLineNumber() + ":" + diagnostic.getMessage(java.util.Locale.ROOT));
          }
        }
        result.add(Map.of("path", relative.toString().replace('\\', '/'), "sha256", M3PortRecipe.hash(Files.readAllBytes(path)),
            "bytes", Files.size(path), "declarations", declarations, "imports_not_resolved_dependencies", imports,
            "diagnostics", diagnostics, "disposition", diagnostics.isEmpty() ? "discovered_requires_contract_review" : "blocked_syntax_diagnostics"));
      }
    }
    return Map.of("schema", 1, "scope", "all files under supplied root except .git; no repository-completeness inference", "files", result,
        "limitations", "Parse only, no type attribution or generated-code execution. Imports are evidence, not resolved dependency closure. Native ABI, reflection, module resolution and external resources require separate review.");
  }
}
