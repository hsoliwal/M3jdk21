// SPDX-License-Identifier: Apache-2.0
package com.m3.compiletime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * CPU-only inventory of compile-time-known strings over JDK modules ({@code jrt:/}) and a jar
 * set, in one JVM. Per-unit rows are appended and flushed as each unit completes.
 *
 * <pre>
 * java --add-opens java.base/java.util.regex=ALL-UNNAMED -cp ... \
 *   com.m3.compiletime.M3CompileTimeStringInventory --out DIR [--jdk] [--jars LIST] [--regex-only]
 * </pre>
 */
public final class M3CompileTimeStringInventory {
    private final M3InventoryState state = new M3InventoryState();
    private final M3ClassScan scan;
    private final BufferedWriter rows;

    private M3CompileTimeStringInventory(BufferedWriter rows, boolean regexOnly) {
        this.rows = rows;
        this.scan = new M3ClassScan(state, regexOnly);
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(arg(args, "--out", "target/compiletime-inventory"));
        Files.createDirectories(out);
        boolean jdk = List.of(args).contains("--jdk");
        String jars = arg(args, "--jars", null);
        boolean regexOnly = List.of(args).contains("--regex-only");
        try (BufferedWriter w = Files.newBufferedWriter(out.resolve("COMPILETIME_STRING_INVENTORY.tsv"))) {
            w.write("corpus\tunit\tcategory\tmetric\tvalue\n");
            M3CompileTimeStringInventory run = new M3CompileTimeStringInventory(w, regexOnly);
            if (jdk) {
                run.scanJdk();
            }
            if (jars != null) {
                run.scanJars(Files.readAllLines(Path.of(jars)));
            }
            M3InventoryReport.write(run.state, out);
        }
        log("DONE " + out);
    }

    static String arg(String[] args, String name, String fallback) {
        for (int i = 0; i + 1 < args.length; i++) {
            if (args[i].equals(name)) {
                return args[i + 1];
            }
        }
        return fallback;
    }

    private void scanJdk() throws IOException {
        FileSystem jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        List<Path> modules;
        try (Stream<Path> s = Files.list(jrt.getPath("/modules"))) {
            modules = s.sorted().toList();
        }
        log("JDK " + System.getProperty("java.home") + " modules=" + modules.size());
        for (Path module : modules) {
            List<Path> classes;
            try (Stream<Path> s = Files.walk(module)) {
                classes = s.filter(p -> p.toString().endsWith(".class"))
                        .filter(p -> !p.getFileName().toString().equals("module-info.class"))
                        .sorted().toList();
            }
            state.beginUnit(0, module.getFileName().toString());
            for (Path c : classes) {
                classBytes(Files.readAllBytes(c), c.toString());
            }
            flushUnit();
        }
    }

    private void scanJars(List<String> jarList) throws IOException {
        List<String> jars = new ArrayList<>(jarList.stream().map(String::trim).filter(s -> !s.isEmpty()).toList());
        Collections.sort(jars);
        log("JARS " + jars.size());
        for (String jar : jars) {
            Path path = Path.of(jar);
            state.beginUnit(1, path.getFileName().toString());
            try (ZipFile zip = new ZipFile(path.toFile())) {
                List<? extends ZipEntry> entries = Collections.list(zip.entries()).stream()
                        .filter(e -> e.getName().endsWith(".class"))
                        .sorted((a, b) -> a.getName().compareTo(b.getName())).toList();
                for (ZipEntry e : entries) {
                    jarEntry(zip, e);
                }
            }
            flushUnit();
        }
    }

    private void jarEntry(ZipFile zip, ZipEntry e) throws IOException {
        if (e.getName().endsWith("module-info.class")) {
            state.inc("all", "module_info_skipped");
            return;
        }
        if (e.getName().startsWith("META-INF/versions/")) {
            state.inc("all", "multi_release_classes_skipped");
            return;
        }
        try (InputStream in = zip.getInputStream(e)) {
            classBytes(in.readAllBytes(), e.getName());
        }
    }

    private void classBytes(byte[] bytes, String name) {
        try {
            scan.scan(bytes);
        } catch (RuntimeException unreadable) {
            state.inc("all", "class_parse_failures");
            log("PARSE_FAIL " + state.unitName + " " + name + " " + unreadable);
        }
    }

    private void flushUnit() throws IOException {
        String corpus = M3InventoryState.CORPORA[state.corpus];
        for (Map.Entry<String, Long> e : new TreeMap<>(state.unit).entrySet()) {
            rows.write(corpus + '\t' + state.unitName + '\t' + e.getKey() + '\t' + e.getValue() + '\n');
        }
        rows.flush();
        log("UNIT " + corpus + " " + state.unitName + " classes=" + state.unit.getOrDefault("all\tclasses", 0L));
    }

    static void log(String line) {
        System.err.println(line);
    }
}
