// SPDX-License-Identifier: Apache-2.0
// C6 DEPENDENCY-SOURCE-INLINE: donor-superset `commands` recipe helper for donors whose published
// sources jar carries a module descriptor or multi-release sources. Compiles every .java file below
// the source root except module-info.java and META-INF/versions/** in classpath mode with the given
// release, then packages classes plus every non-Java resource into donor.jar, exactly like the
// runner's own java recipe. Records what was excluded in compile-exclusions.tsv. Run via the JDK
// source-file launcher: java ModuleCompile.java <source> <output> <release> <encoding> [classpath-entry ...].
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;

public final class ModuleCompile {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]).toAbsolutePath();
        Path output = Path.of(args[1]).toAbsolutePath();
        String release = args[2];
        String encoding = args[3];
        StringBuilder cp = new StringBuilder();
        for (int i = 4; i < args.length; i++) {
            if (args[i].isBlank()) continue;
            if (cp.length() > 0) cp.append(java.io.File.pathSeparatorChar);
            cp.append(args[i]);
        }
        String classpath = cp.toString();
        List<Path> files = new ArrayList<>();
        List<String> excluded = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(source)) {
            for (Path p : walk.sorted().toList()) {
                if (!Files.isRegularFile(p) || !p.toString().endsWith(".java")) continue;
                String rel = source.relativize(p).toString().replace('\\', '/');
                int slash = rel.indexOf('/');
                if (slash > 0 && rel.substring(0, slash).contains(".") && Files.isRegularFile(source.resolve(rel.substring(0, slash)).resolve("module-info.java"))) {
                    String baseRel = rel.substring(slash + 1);
                    if (baseRel.endsWith("module-info.java") || Files.isRegularFile(source.resolve(baseRel))) { excluded.add(rel + "\tMODULE_SOURCE_VARIANT_DUPLICATE"); continue; }
                    excluded.add(rel + "\tMODULE_SOURCE_VARIANT_UNIQUE_INCLUDED");
                }
                if (rel.endsWith("module-info.java")) excluded.add(rel + "\tMODULE_DESCRIPTOR");
                else if (rel.startsWith("META-INF/versions/")) excluded.add(rel + "\tMULTI_RELEASE_SOURCE");
                else files.add(p);
            }
        }
        if (files.isEmpty()) throw new IOException("NO_JAVA_SOURCES");
        Path classes = Files.createDirectories(output.resolve("classes"));
        StringBuilder arguments = new StringBuilder();
        for (Path file : files)
            arguments.append('"').append(file.toString().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"\n");
        Path argFile = output.resolve("sources.args");
        Files.writeString(argFile, arguments.toString());
        Files.writeString(output.resolve("compile-exclusions.tsv"), "path\treason\n" + String.join("\n", excluded) + (excluded.isEmpty() ? "" : "\n"));
        List<String> argv = new ArrayList<>();
        if (release.startsWith("legacy:")) argv.addAll(List.of("-source", release.substring(7), "-target", release.substring(7), "-Xlint:-options"));
        else argv.addAll(List.of("--release", release));
        argv.addAll(List.of("-proc:none", "-encoding", encoding, "-d", classes.toString()));
        if (!classpath.isBlank()) { argv.add("-classpath"); argv.add(classpath); }
        argv.add("@" + argFile);
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        int rc;
        try (var log = Files.newOutputStream(output.resolve("compile.log"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            rc = javac.run(null, log, log, argv.toArray(String[]::new));
        }
        if (rc != 0) throw new IOException("JAVAC_EXIT_" + rc);
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(output.resolve("donor.jar"), StandardOpenOption.CREATE_NEW));
             Stream<Path> paths = Files.walk(classes)) {
            for (Path p : paths.filter(Files::isRegularFile).sorted().toList()) put(jar, classes.relativize(p).toString().replace('\\', '/'), p);
            try (Stream<Path> resources = Files.walk(source)) {
                for (Path p : resources.filter(Files::isRegularFile).sorted().toList()) {
                    if (p.toString().endsWith(".java")) continue;
                    String name = source.relativize(p).toString().replace('\\', '/');
                    if (name.startsWith("META-INF/") && (name.endsWith(".SF") || name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC"))) { excluded.add(name + "\tSIGNATURE_FILE_NOT_REPACKAGED"); continue; }
                    put(jar, name, p);
                }
            }
        }
        Files.writeString(output.resolve("compile-exclusions.tsv"), "path\treason\n" + String.join("\n", excluded) + (excluded.isEmpty() ? "" : "\n"));
        System.out.println("compiled " + files.size() + " excluded " + excluded.size());
    }

    private static void put(JarOutputStream jar, String name, Path file) throws IOException {
        JarEntry entry = new JarEntry(name);
        entry.setTime(0);
        jar.putNextEntry(entry);
        Files.copy(file, jar);
        jar.closeEntry();
    }
}
