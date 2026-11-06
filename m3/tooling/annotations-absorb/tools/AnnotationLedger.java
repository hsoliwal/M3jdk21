/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * The absorption ledger of the annotations convergence. Each vendored donor is compiled by javac
 * and its annotation types are read back by reflection, so the targets, retention and elements are
 * the compiler's, not a text scan's. The rows join MAP_RULES.tsv; a donor annotation without a rule
 * is ABSORB_NEXT, with a proposed owner under {@code jdk.m3.annotations}.
 *
 * <p>Usage: {@code java AnnotationLedger.java <donors dir> <MAP_RULES.tsv> <out dir>}. A donor is
 * a directory with {@code src/main/java} and {@code PROVENANCE.json}. Writes DONORS.tsv and
 * ANNOTATIONS.tsv; exit 0 when every MAP_RULES row names an annotation the donors declare.
 */
public final class AnnotationLedger {
    private static final Pattern VERSION = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LICENSE = Pattern.compile("\"license\"\\s*:\\s*\"([^\"]+)\"");
    private static final List<String> AREAS = List.of(
            "org.jspecify.annotations=", "org.checkerframework.=checker.",
            "org.jetbrains.annotations=jetbrains", "org.intellij.lang.annotations=intellij",
            "javax.annotation=jsr305", "com.google.errorprone.annotations=errorprone",
            "com.google.j2objc.annotations=j2objc");

    private AnnotationLedger() {
    }

    /** One donor annotation type as the compiler sees it. */
    record Row(String donor, String fqn, String targets, String retention, String elements,
            String meta) {
    }

    /**
     * Runs the ledger.
     *
     * @param args the donors directory, MAP_RULES.tsv and the output directory
     * @throws Exception if a donor does not compile or an output cannot be written
     */
    public static void main(String[] args) throws Exception {
        Path donors = Path.of(args[0]);
        Map<String, String[]> rules = rules(Path.of(args[1]));
        List<Path> dirs = Files.list(donors)
                .filter(d -> Files.isDirectory(d.resolve("src/main/java"))).sorted().toList();
        List<String> donorRows = new ArrayList<>(
                List.of("donor\tversion\tlicense\tjava_files\tannotation_types"));
        List<Row> rows = new ArrayList<>();
        for (Path d : dirs) {
            List<Row> found = annotations(d);
            rows.addAll(found);
            donorRows.add(String.join("\t", d.getFileName().toString(), field(d, VERSION),
                    field(d, LICENSE), String.valueOf(sources(d).size()),
                    String.valueOf(found.size())));
        }
        Map<String, Long> concept = rows.stream().collect(Collectors.groupingBy(
                r -> simple(r.fqn()), TreeMap::new, Collectors.mapping(Row::donor,
                        Collectors.collectingAndThen(Collectors.toSet(), s -> (long) s.size()))));
        List<String> out = new ArrayList<>(List.of("donor\tfqn\ttargets\tretention\telements\tmeta"
                + "\tconcept_donors\tdecision\trelation\tm3_owner"));
        rows.forEach(r -> out.add(line(r, concept.get(simple(r.fqn())), rules.get(r.fqn()))));
        Path o = Path.of(args[2]);
        Files.write(o.resolve("DONORS.tsv"), donorRows, StandardCharsets.UTF_8);
        Files.write(o.resolve("ANNOTATIONS.tsv"), out, StandardCharsets.UTF_8);
        List<String> unknown = rules.keySet().stream()
                .filter(f -> rows.stream().noneMatch(r -> r.fqn().equals(f))).toList();
        System.err.println("ANNOTATION_LEDGER donors=" + dirs.size() + " annotations=" + rows.size()
                + " rules=" + rules.size() + " unknown_rules=" + unknown);
        System.exit(unknown.isEmpty() ? 0 : 4);
    }

    private static String line(Row r, long donorsWithName, String[] rule) {
        String[] decision = rule != null ? rule
                : new String[] {"ABSORB_NEXT", "-", proposedOwner(r.fqn())};
        return String.join("\t", r.donor(), r.fqn(), r.targets(), r.retention(), r.elements(),
                r.meta(), String.valueOf(donorsWithName), decision[0], decision[1], decision[2]);
    }

    /** The proposed owner of an annotation with no rule: its donor area in jdk.m3.annotations. */
    static String proposedOwner(String fqn) {
        return AREAS.stream().map(a -> a.split("=", -1)).filter(a -> fqn.startsWith(a[0]))
                .findFirst().map(a -> {
                    String rest = fqn.substring(a[0].length()).replace(".qual.", ".");
                    String area = (a[1] + rest).replaceAll("^\\.|\\.$", "");
                    return "jdk.m3.annotations." + area.replaceAll("\\.+", ".");
                }).orElse("jdk.m3.annotations.other." + simple(fqn));
    }

    private static Map<String, String[]> rules(Path tsv) throws IOException {
        return Files.readAllLines(tsv, StandardCharsets.UTF_8).stream()
                .filter(l -> !l.startsWith("#") && !l.startsWith("donor_fqn\t") && !l.isBlank())
                .map(l -> l.split("\t", -1))
                .collect(Collectors.toMap(c -> c[0], c -> new String[] {c[1], c[2], c[3]},
                        (a, b) -> a, TreeMap::new));
    }

    private static String field(Path donor, Pattern p) throws IOException {
        Path json = donor.resolve("PROVENANCE.json");
        Matcher m = p.matcher(Files.exists(json) ? Files.readString(json) : "");
        return m.find() ? m.group(1) : "-";
    }

    private static List<Path> sources(Path donor) throws IOException {
        try (Stream<Path> s = Files.walk(donor.resolve("src/main/java"))) {
            return s.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.getFileName().toString().equals("module-info.java"))
                    .sorted().toList();
        }
    }

    private static List<Row> annotations(Path donor) throws Exception {
        Path classes = Files.createTempDirectory("ann-ledger");
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<String> argv = new ArrayList<>(List.of("--release", "21", "-proc:none", "-nowarn",
                "-d", classes.toString()));
        sources(donor).forEach(f -> argv.add(f.toString()));
        if (javac.run(null, null, null, argv.toArray(String[]::new)) != 0) {
            throw new IllegalStateException("donor does not compile: " + donor);
        }
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classes.toUri().toURL()},
                ClassLoader.getPlatformClassLoader()); Stream<Path> s = Files.walk(classes)) {
            List<String> names = s.filter(f -> f.toString().endsWith(".class"))
                    .map(f -> classes.relativize(f).toString().replace('\\', '/'))
                    .map(f -> f.substring(0, f.length() - 6).replace('/', '.')).sorted().toList();
            List<Row> rows = new ArrayList<>();
            for (String n : names) {
                Class<?> c = Class.forName(n, false, loader);
                if (c.isAnnotation()) {
                    rows.add(row(donor.getFileName().toString(), c));
                }
            }
            return rows;
        }
    }

    private static Row row(String donor, Class<?> c) {
        Target t = c.getAnnotation(Target.class);
        String targets = t == null ? "DECLARATIONS(no @Target)" : Arrays.stream(t.value())
                .map(ElementType::name).sorted().collect(Collectors.joining(","));
        String retention = Optional.ofNullable(c.getAnnotation(Retention.class))
                .map(Retention::value).orElse(RetentionPolicy.CLASS).name();
        String elements = Arrays.stream(c.getDeclaredMethods()).filter(m -> !m.isSynthetic())
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .map(AnnotationLedger::element).collect(Collectors.joining(","));
        String meta = Arrays.stream(c.getAnnotations()).map(Annotation::annotationType)
                .filter(a -> !a.getPackageName().equals("java.lang.annotation"))
                .map(Class::getSimpleName).sorted().collect(Collectors.joining(","));
        return new Row(donor, c.getName().replace('$', '.'), targets, retention,
                elements.isEmpty() ? "-" : elements, meta.isEmpty() ? "-" : meta);
    }

    private static String element(Method m) {
        return m.getName() + ":" + m.getReturnType().getSimpleName()
                + (m.getDefaultValue() == null ? "" : "=default");
    }

    private static String simple(String fqn) {
        return fqn.substring(fqn.lastIndexOf('.') + 1);
    }
}
